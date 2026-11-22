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

class ReadableEnumNameTest {

    @Suppress("EnumNaming", "EnumEntryNameCase")
    private enum class Sample {
        RANDOM_PIN,
        UNSET,
        CODEC2_700,
        EU_868,
        US,
        TRAILING_,
    }

    @Test
    fun wordsAreCapitalisedAndSpaced() {
        assertEquals("Random Pin", Sample.RANDOM_PIN.readableName())
        assertEquals("Unset", Sample.UNSET.readableName())
    }

    @Test
    fun digitsStayWithTheirWord() {
        assertEquals("Codec2 700", Sample.CODEC2_700.readableName())
    }

    @Test
    fun twoLetterPartsStayCapitals() {
        assertEquals("EU 868", Sample.EU_868.readableName())
        assertEquals("US", Sample.US.readableName())
    }

    @Test
    fun emptyPartsAreDropped() {
        assertEquals("Trailing", Sample.TRAILING_.readableName())
    }
}
