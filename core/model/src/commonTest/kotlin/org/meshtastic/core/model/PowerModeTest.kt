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
package org.meshtastic.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PowerModeTest {

    @Test
    fun mostAggressivePicksHigherRankForAllPairs() {
        val expected =
            mapOf(
                (PowerMode.STANDARD to PowerMode.STANDARD) to PowerMode.STANDARD,
                (PowerMode.STANDARD to PowerMode.TRAIL) to PowerMode.TRAIL,
                (PowerMode.STANDARD to PowerMode.EXPEDITION) to PowerMode.EXPEDITION,
                (PowerMode.TRAIL to PowerMode.STANDARD) to PowerMode.TRAIL,
                (PowerMode.TRAIL to PowerMode.TRAIL) to PowerMode.TRAIL,
                (PowerMode.TRAIL to PowerMode.EXPEDITION) to PowerMode.EXPEDITION,
                (PowerMode.EXPEDITION to PowerMode.STANDARD) to PowerMode.EXPEDITION,
                (PowerMode.EXPEDITION to PowerMode.TRAIL) to PowerMode.EXPEDITION,
                (PowerMode.EXPEDITION to PowerMode.EXPEDITION) to PowerMode.EXPEDITION,
            )
        for ((pair, result) in expected) {
            assertEquals(result, PowerMode.mostAggressive(pair.first, pair.second), "mostAggressive$pair")
        }
    }

    @Test
    fun gatesMatchTierDefinitionsExhaustively() {
        // One row per mode; a new enum entry must be classified here before this test compiles/passes.
        for (mode in PowerMode.entries) {
            when (mode) {
                PowerMode.STANDARD -> {
                    assertTrue(mode.allowsContinuousLocation)
                    assertTrue(mode.allowsWeather)
                    assertTrue(mode.allowsLiveUpdates)
                    assertTrue(mode.allowsBackgroundAutoConnect)
                    assertTrue(mode.allowsWidgetRefresh)
                    assertEquals(0, mode.locationIntervalFloorSecs)
                }
                PowerMode.TRAIL -> {
                    assertTrue(mode.allowsContinuousLocation)
                    assertTrue(mode.allowsWeather)
                    assertFalse(mode.allowsLiveUpdates)
                    assertTrue(mode.allowsBackgroundAutoConnect)
                    assertTrue(mode.allowsWidgetRefresh)
                    assertEquals(300, mode.locationIntervalFloorSecs)
                }
                PowerMode.EXPEDITION -> {
                    assertFalse(mode.allowsContinuousLocation)
                    assertFalse(mode.allowsWeather)
                    assertFalse(mode.allowsLiveUpdates)
                    assertFalse(mode.allowsBackgroundAutoConnect)
                    assertFalse(mode.allowsWidgetRefresh)
                    assertEquals(Int.MAX_VALUE, mode.locationIntervalFloorSecs)
                }
            }
        }
    }

    @Test
    fun fromNameParsesPersistedValuesAndFallsBack() {
        for (mode in PowerMode.entries) {
            assertEquals(mode, PowerMode.fromName(mode.name))
        }
        assertEquals(PowerMode.STANDARD, PowerMode.fromName(null))
        assertEquals(PowerMode.STANDARD, PowerMode.fromName("bogus"))
        assertEquals(PowerMode.STANDARD, PowerMode.fromName("standard"), "parsing is case-sensitive by design")
    }
}
