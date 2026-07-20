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
package org.meshtastic.core.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Canary for the Pangea brand palette - an INTENTIONAL divergence from upstream Meshtastic green.
 *
 * If this test fails after an upstream merge or an AI-agent session, the Pangea palette was clobbered: restore the
 * values below, do NOT update the expectations. Authority: design/PANGEA-BRAND.md.
 */
class PangeaPaletteTest {

    @Test
    fun primaryRolesAreDarkenedFossil() {
        assertEquals(Color(0xFFC4430D), primaryLight, "light primary must be darkened Fossil (AA 4.68)")
        assertEquals(Color(0xFFFF9F6B), primaryDark, "dark primary must be light Fossil (AA 8.48)")
    }

    @Test
    fun containerRolesAreFossilTones() {
        assertEquals(Color(0xFFFFDBCB), primaryContainerLight)
        assertEquals(Color(0xFF3B0900), onPrimaryContainerLight)
        assertEquals(Color(0xFF7A2E04), primaryContainerDark)
        assertEquals(Color(0xFFFFDBCB), onPrimaryContainerDark)
    }

    @Test
    fun brandMarkIsFossil() {
        assertEquals(Color(0xFFF05511), PangeaFossil, "brand mark is FOSSIL (F-0-5-5-1-1)")
    }
}
