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
package org.meshtastic.feature.settings.radio.component

import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import kotlin.test.Test
import kotlin.test.assertEquals

class AdminKeySlotsTest {
    private val a = "a".repeat(32).encodeUtf8()
    private val b = "b".repeat(32).encodeUtf8()
    private val c = "c".repeat(32).encodeUtf8()

    @Test
    fun `a key typed in an existing slot replaces it`() {
        assertEquals(listOf(a, c), withAdminKey(listOf(a, b), slot = 1, key = c))
    }

    @Test
    fun `a key typed past the end is appended rather than leaving a gap`() {
        assertEquals(listOf(a, c), withAdminKey(listOf(a), slot = 2, key = c))
    }

    @Test
    fun `clearing a slot drops it and closes the gap`() {
        assertEquals(listOf(a, c), withAdminKey(listOf(a, b, c), slot = 1, key = ByteString.EMPTY))
    }

    @Test
    fun `clearing an empty slot changes nothing`() {
        assertEquals(listOf(a), withAdminKey(listOf(a), slot = 2, key = ByteString.EMPTY))
    }
}
