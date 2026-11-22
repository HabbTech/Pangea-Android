/*
 * Copyright (c) 2026 Meshtastic LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.meshtastic.core.data.pangea

import co.touchlab.kermit.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import org.koin.core.annotation.Single
import org.meshtastic.core.common.di.ServiceScope
import org.meshtastic.core.common.util.currentRegionCode
import org.meshtastic.core.database.getDatabaseDirectory
import org.meshtastic.core.database.getFileSystem
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.pangea.RadioAutoConfig
import org.meshtastic.core.repository.ConnectionStateProvider
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.PangeaRadioSetup
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.core.repository.RadioController
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.proto.Config
import kotlin.time.Duration.Companion.seconds

@Single(binds = [PangeaRadioSetup::class])
class PangeaRadioSetupImpl(
    private val radioController: RadioController,
    private val nodeRepository: NodeRepository,
    private val radioConfigRepository: RadioConfigRepository,
    private val uiPrefs: UiPrefs,
    connection: ConnectionStateProvider,
    private val scope: ServiceScope,
) : PangeaRadioSetup {
    private val fileSystem: FileSystem = getFileSystem()
    private val attemptsFile: Path = getDatabaseDirectory().parent!! / "PangeaProfiles" / "pairing-code-attempts.json"

    init {
        connection.connectionState
            .filter { it == ConnectionState.Connected }
            .onEach {
                scope.launch {
                    fillInRegion()
                    turnOffPairingCode()
                }
            }
            .launchIn(scope)
    }

    override suspend fun applyName(name: String): Result<Boolean> = runCatching {
        val me = nodeRepository.ourNodeInfo.value ?: error("No connected radio")
        val longName = RadioAutoConfig.clampedLongName(name)
        if (longName.isEmpty() || longName == me.user.long_name) return@runCatching false
        val owner =
            me.user
                .newBuilder()
                .also {
                    it.long_name = longName
                    it.short_name = RadioAutoConfig.shortName(longName)
                }
                .build()
        radioController.editLocalSettings { setOwner(owner) }
        Logger.i { "🧭 [Setup] Saved owner name" }
        true
    }

    override suspend fun applyRegion(region: Config.LoRaConfig.RegionCode): Result<Boolean> = runCatching {
        if (region == Config.LoRaConfig.RegionCode.UNSET) return@runCatching false
        val current = radioConfigRepository.localConfigFlow.first().lora
        if (current?.region == region) return@runCatching false
        // Keep whatever else is on the radio; only fill in what's required to transmit.
        val lora =
            (
                current
                    ?: Config.LoRaConfig.Builder()
                        .also {
                            it.use_preset = true
                            it.hop_limit = DEFAULT_HOPS
                        }
                        .build()
                )
                .newBuilder()
                .also {
                    it.region = region
                    it.tx_enabled = true
                }
                .build()
        radioController.editLocalSettings { setConfig(Config.Builder().also { it.lora = lora }.build()) }
        Logger.i { "🧭 [Setup] Set region to $region" }
        true
    }

    /**
     * A radio fresh out of the box has no LoRa region and can't transmit. Outside Expert Mode, fill it in from the
     * phone's country when that maps to one region with confidence (Pangea-Apple RadioAutoConfig.suggestedRegion); the
     * radio restarts to apply it. Expert Mode leaves it to the user.
     */
    @Suppress("ReturnCount")
    private suspend fun fillInRegion() {
        delay(SETTLE)
        if (uiPrefs.expertModeEnabled.value) return
        val region = radioConfigRepository.localConfigFlow.first().lora?.region ?: return
        if (region != Config.LoRaConfig.RegionCode.UNSET) return
        val suggested = RadioAutoConfig.suggestedRegion(currentRegionCode()) ?: return
        applyRegion(suggested).onFailure { Logger.e(it) { "🧭 [Setup] Couldn't fill in the region" } }
    }

    /**
     * The phone has to enter a code the first time it pairs, which a radio without a screen can't show. Once paired,
     * the radio is set to pair without one, so reconnecting or pairing another phone never asks. The radio restarts to
     * apply it. Sent at most [RadioAutoConfig.MAX_PAIRING_CODE_ATTEMPTS] times per radio, ever.
     */
    @Suppress("ReturnCount")
    private suspend fun turnOffPairingCode() {
        // Settle first: the Bluetooth config arrives during the handshake.
        delay(SETTLE)
        val me = nodeRepository.ourNodeInfo.value ?: return
        val bluetooth = radioConfigRepository.localConfigFlow.first().bluetooth ?: return
        if (!RadioAutoConfig.needsCodeTurnedOff(bluetooth.mode, uiPrefs.expertModeEnabled.value)) return
        val attempts = readAttempts()
        val tried = attempts[me.num.toString()] ?: 0
        if (!RadioAutoConfig.mayTryTurningOffCode(tried)) {
            Logger.i { "🧭 [Setup] Radio kept its pairing code after $tried tries; leaving it" }
            return
        }
        // Counted before sending: the radio may take the change and restart before the call returns.
        writeAttempts(attempts + (me.num.toString() to tried + 1))
        val config =
            bluetooth
                .newBuilder()
                .also {
                    it.enabled = true
                    it.mode = Config.BluetoothConfig.PairingMode.NO_PIN
                }
                .build()
        runCatching {
            radioController.editLocalSettings { setConfig(Config.Builder().also { it.bluetooth = config }.build()) }
        }
            .onSuccess { Logger.i { "🧭 [Setup] Turned off the pairing code" } }
            .onFailure { Logger.e(it) { "🧭 [Setup] Couldn't turn off the pairing code" } }
    }

    private fun readAttempts(): Map<String, Int> = runCatching {
        if (!fileSystem.exists(attemptsFile)) return@runCatching emptyMap()
        Json.decodeFromString<Map<String, Int>>(fileSystem.read(attemptsFile) { readUtf8() })
    }
        .getOrDefault(emptyMap())

    private fun writeAttempts(attempts: Map<String, Int>) {
        runCatching {
            fileSystem.createDirectories(attemptsFile.parent!!)
            fileSystem.write(attemptsFile) { writeUtf8(Json.encodeToString(attempts)) }
        }
    }

    private companion object {
        const val DEFAULT_HOPS = 3
        val SETTLE = 5.seconds
    }
}
