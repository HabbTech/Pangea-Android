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
package org.meshtastic.core.model.pangea

import org.meshtastic.proto.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Vectors from Pangea-Apple PangeaCodeTests.swift (short names, pairing code). */
class RadioAutoConfigTest {
    @Test
    fun shortNamesSkipInitialsThatDontFit() {
        assertEquals("MJK", RadioAutoConfig.shortName("Mary Jo Kim"))
        assertEquals("A", RadioAutoConfig.shortName("🇺🇸 Andy"))
        assertEquals("ANDY", RadioAutoConfig.shortName("🇺🇸Andy"))
        assertEquals("ME", RadioAutoConfig.shortName("🇺🇸"))
        assertEquals("SAM", RadioAutoConfig.shortName("Sam"))
        assertEquals("AD", RadioAutoConfig.shortName("Andrew Davis"))
    }

    @Test
    fun pairingCodeIsTurnedOffOnceSettingsAreKnown() {
        val random = Config.BluetoothConfig.PairingMode.RANDOM_PIN
        val fixed = Config.BluetoothConfig.PairingMode.FIXED_PIN
        val none = Config.BluetoothConfig.PairingMode.NO_PIN
        assertTrue(RadioAutoConfig.needsCodeTurnedOff(random, expertMode = false))
        assertTrue(RadioAutoConfig.needsCodeTurnedOff(fixed, expertMode = false))
        assertFalse(RadioAutoConfig.needsCodeTurnedOff(none, expertMode = false))
        assertFalse(RadioAutoConfig.needsCodeTurnedOff(null, expertMode = false))
        assertFalse(RadioAutoConfig.needsCodeTurnedOff(fixed, expertMode = true))
    }

    @Test
    fun regionsAndDefaultNames() {
        assertEquals(Config.LoRaConfig.RegionCode.US, RadioAutoConfig.suggestedRegion("us"))
        assertEquals(Config.LoRaConfig.RegionCode.EU_868, RadioAutoConfig.suggestedRegion("DE"))
        assertNull(RadioAutoConfig.suggestedRegion("ZZ"))
        assertTrue(RadioAutoConfig.hasDefaultName("Meshtastic 1a2b"))
        assertTrue(RadioAutoConfig.hasDefaultName("PANGEA:ID:A1B2"))
        assertFalse(RadioAutoConfig.hasDefaultName("Sam's Radio"))
        assertEquals("A1B2", RadioAutoConfig.pangeaSerial("pangea:id:A1B2"))
    }
}
