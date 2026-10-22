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
@file:Suppress("ReturnCount")

package org.meshtastic.core.model.pangea

import okio.ByteString.Companion.encodeUtf8
import org.meshtastic.proto.Config

/**
 * What a freshly paired radio still needs before it can message, decided without the user having to understand it
 * (Pangea-Apple RadioAutoConfig.swift).
 */
object RadioAutoConfig {
    /** Radios sold by Pangea advertise as `PANGEA:ID:<serial>` and ship pre-configured. */
    const val PANGEA_NAME_PREFIX = "PANGEA:ID:"

    /** The firmware's limits for owner names, in bytes. */
    const val MAX_LONG_NAME_BYTES = 36
    const val MAX_SHORT_NAME_BYTES = 4

    /** How many times one radio is ever sent "pair without a code": each one restarts it. */
    const val MAX_PAIRING_CODE_ATTEMPTS = 2

    fun isPangeaRadio(advertisedName: String?): Boolean =
        advertisedName?.uppercase()?.startsWith(PANGEA_NAME_PREFIX) == true

    /** The serial after the prefix, for display ("Pangea Radio · A1B2"). */
    fun pangeaSerial(advertisedName: String?): String? {
        if (!isPangeaRadio(advertisedName)) return null
        return advertisedName!!.drop(PANGEA_NAME_PREFIX.length).ifEmpty { null }
    }

    /** EU 868 MHz band: EU/EEA members plus neighbours that follow ETSI rules. */
    private val euCountries =
        setOf(
            "AT",
            "BE",
            "BG",
            "HR",
            "CY",
            "CZ",
            "DK",
            "EE",
            "FI",
            "FR",
            "DE",
            "GR",
            "HU",
            "IE",
            "IT",
            "LV",
            "LT",
            "LU",
            "MT",
            "NL",
            "PL",
            "PT",
            "RO",
            "SK",
            "SI",
            "ES",
            "SE",
            "GB",
            "NO",
            "IS",
            "LI",
            "CH",
            "RS",
            "ME",
            "MK",
            "AL",
            "BA",
            "MD",
            "TR",
            "ZA",
        )

    /**
     * Best guess at the legal LoRa region for where the phone is set up. Null when the country isn't one we can map
     * confidently, so the UI asks instead.
     */
    @Suppress("CyclomaticComplexMethod")
    fun suggestedRegion(countryCode: String?): Config.LoRaConfig.RegionCode? {
        val code = countryCode?.uppercase() ?: return null
        if (code in euCountries) return Config.LoRaConfig.RegionCode.EU_868
        return when (code) {
            "US",
            "CA",
            "MX",
            "PR",
            "GU",
            "VI",
            "AS",
            "MP",
            -> Config.LoRaConfig.RegionCode.US

            "AU",
            "NZ",
            "BR",
            "CL",
            "AR",
            "CO",
            "PE",
            "UY",
            "CR",
            "PA",
            -> Config.LoRaConfig.RegionCode.ANZ

            "CN" -> Config.LoRaConfig.RegionCode.CN

            "JP" -> Config.LoRaConfig.RegionCode.JP

            "KR" -> Config.LoRaConfig.RegionCode.KR

            "TW" -> Config.LoRaConfig.RegionCode.TW

            "RU" -> Config.LoRaConfig.RegionCode.RU

            "IN" -> Config.LoRaConfig.RegionCode.IN

            "TH" -> Config.LoRaConfig.RegionCode.TH

            // UA 868 was folded into EU 868 (firmware #10994).
            "UA" -> Config.LoRaConfig.RegionCode.EU_868

            "MY" -> Config.LoRaConfig.RegionCode.MY_919

            "SG" -> Config.LoRaConfig.RegionCode.SG_923

            "PH" -> Config.LoRaConfig.RegionCode.PH_915

            "KZ" -> Config.LoRaConfig.RegionCode.KZ_863

            else -> null
        }
    }

    /** Stock firmware names radios "Meshtastic 1a2b"; Pangea radios use the serial. */
    fun hasDefaultName(longName: String): Boolean =
        longName.isEmpty() || longName.startsWith("Meshtastic ") || longName.uppercase().startsWith(PANGEA_NAME_PREFIX)

    /**
     * "Andrew Davis" -> "AD", "Sam" -> "SAM", "Mary Jo Kim" -> "MJK". Max 4 bytes, as the firmware requires. Initials
     * too big to fit (a flag is 8 bytes) are skipped rather than ending the name early, then plain letters from the
     * name are tried before falling back to "ME".
     */
    fun shortName(longName: String): String {
        val name = longName.cleanedName
        val words = name.split(' ').filter { it.isNotEmpty() }
        var candidate: String
        if (words.size > 1) {
            candidate = ""
            for (word in words) {
                val initial = word.substring(0, graphemeEnd(word, 0)).uppercase()
                if (candidate.byteCount() + initial.byteCount() <= MAX_SHORT_NAME_BYTES) candidate += initial
            }
        } else {
            candidate = name.firstGraphemes(MAX_SHORT_NAME_BYTES).uppercase().clampedUtf8(MAX_SHORT_NAME_BYTES)
        }
        if (candidate.isEmpty()) {
            candidate = name.filter { it.code < ASCII_LIMIT && it.isLetter() }.take(MAX_SHORT_NAME_BYTES).uppercase()
        }
        return candidate.ifEmpty { "ME" }
    }

    /** Without hidden characters, spaces made plain, cut to the firmware's 36 bytes. */
    fun clampedLongName(name: String): String = name.cleanedName(MAX_LONG_NAME_BYTES)

    /**
     * Whether a radio paired with these Bluetooth settings should be switched to pairing without a code. Only once its
     * settings are known; Expert Mode leaves the pairing mode to the user.
     */
    fun needsCodeTurnedOff(mode: Config.BluetoothConfig.PairingMode?, expertMode: Boolean): Boolean {
        if (mode == null || expertMode) return false
        return mode != Config.BluetoothConfig.PairingMode.NO_PIN
    }

    fun mayTryTurningOffCode(attempts: Int): Boolean = attempts < MAX_PAIRING_CODE_ATTEMPTS

    private const val ASCII_LIMIT = 0x80

    private fun String.byteCount(): Int = encodeUtf8().size

    private fun String.firstGraphemes(count: Int): String {
        var end = 0
        repeat(count) { if (end < length) end = graphemeEnd(this, end) }
        return substring(0, end)
    }
}
