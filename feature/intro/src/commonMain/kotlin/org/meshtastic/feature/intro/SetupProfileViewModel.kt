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
package org.meshtastic.feature.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.common.util.currentRegionCode
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.pangea.RadioAutoConfig
import org.meshtastic.core.repository.ConnectionStateProvider
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.PangeaProfileStore
import org.meshtastic.core.repository.PangeaProfileSync
import org.meshtastic.core.repository.PangeaRadioSetup
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.proto.Config.LoRaConfig.RegionCode
import kotlin.time.Duration.Companion.seconds

/**
 * The setup name page (Pangea-Apple SetupProfileView): saves the user's name to their profile and the radio, and the
 * LoRa region when the radio has none, then waits for the radio to restart with them.
 */
@KoinViewModel
class SetupProfileViewModel(
    private val store: PangeaProfileStore,
    private val sync: PangeaProfileSync,
    private val radioSetup: PangeaRadioSetup,
    private val nodeRepository: NodeRepository,
    private val connection: ConnectionStateProvider,
    radioConfigRepository: RadioConfigRepository,
) : ViewModel() {
    enum class Phase {
        EDITING,
        SAVING,
        RESTARTING,
        DONE,
        FAILED,
    }

    private val _phase = MutableStateFlow(Phase.EDITING)
    val phase: StateFlow<Phase> = _phase.asStateFlow()

    /** True while the radio has no LoRa region, so it can't send anything until one is chosen. */
    val needsRegion: StateFlow<Boolean> =
        radioConfigRepository.localConfigFlow
            .map { it.lora?.region == RegionCode.UNSET }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), false)

    /** The region picked from the phone's country, when it maps to one with confidence. */
    val suggestedRegion: RegionCode? = RadioAutoConfig.suggestedRegion(currentRegionCode())

    /** The name to start from: the profile's, else the radio's unless it's still the factory "Meshtastic 1a2b". */
    fun startingName(): String = store.me.value.displayName.ifEmpty {
        nodeRepository.ourNodeInfo.value
            ?.user
            ?.long_name
            ?.takeUnless { RadioAutoConfig.hasDefaultName(it) }
            .orEmpty()
    }

    fun save(name: String, region: RegionCode?) {
        if (_phase.value == Phase.SAVING || _phase.value == Phase.RESTARTING) return
        _phase.value = Phase.SAVING
        viewModelScope.launch {
            val clean = RadioAutoConfig.clampedLongName(name)
            val before = store.me.value
            // Saved here first, so nothing typed is lost if the radio doesn't take it.
            store.updateMe(clean, before.emoji, before.color)
            val renamed = radioSetup.applyName(clean)
            val regionSet = region?.let { radioSetup.applyRegion(it) } ?: Result.success(false)
            if (renamed.isFailure || regionSet.isFailure) {
                _phase.value = Phase.FAILED
                return@launch
            }
            if (renamed.getOrDefault(false) || regionSet.getOrDefault(false)) {
                _phase.value = Phase.RESTARTING
                // The radio usually comes back well within this; don't trap the user if it doesn't.
                withTimeoutOrNull(RESTART_WAIT) {
                    connection.connectionState
                        .dropWhile { it is ConnectionState.Connected }
                        .first { it is ConnectionState.Connected }
                }
            }
            // My card goes out once the radio is back, so the new name reaches the people I talk to.
            if (store.me.value != before) sync.pushUpdatedCard()
            _phase.value = Phase.DONE
        }
    }

    private companion object {
        const val STOP_TIMEOUT = 5_000L
        val RESTART_WAIT = 45.seconds
    }
}
