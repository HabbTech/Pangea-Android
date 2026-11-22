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
package org.meshtastic.feature.settings.pangea

import kotlin.test.Test
import kotlin.test.assertEquals

class RouteTextTest {

    @Test
    fun theRecordingTimerReadsAsSwiftUIsDoes() {
        assertEquals("0:00", elapsed(0))
        assertEquals("0:05", elapsed(5_999))
        assertEquals("12:34", elapsed((12 * 60 + 34) * 1_000L))
        assertEquals("1:02:03", elapsed((3_600 + 2 * 60 + 3) * 1_000L))
        assertEquals("0:00", elapsed(-1_000))
    }

    @Test
    fun aRouteNameIsCutToAHundredBytesACharacterAtATime() {
        assertEquals("short", "short".withinBytes(100))
        assertEquals("a".repeat(100), "a".repeat(120).withinBytes(100))
        // "é" is two bytes: 51 of them would be 102, so one goes.
        assertEquals("é".repeat(50), "é".repeat(51).withinBytes(100))
    }
}
