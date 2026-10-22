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

import okio.ByteString.Companion.toByteString
import org.meshtastic.proto.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Vectors shared with Pangea-Apple PangeaCodeTests.swift, so both apps give everyone the same code. */
class PangeaCodeTest {
    private fun user(name: String, key: okio.ByteString): User = User.Builder()
        .also {
            it.long_name = name
            it.public_key = key
        }
        .build()

    @Test
    fun roundTrips() {
        for (num in listOf(4L, 0x1A2B3C4DL, 0xDEADBEEFL, 0xFFFFFFFFL - 1)) {
            assertEquals(num, PangeaCode.nodeNum(PangeaCode.code(num)))
        }
    }

    @Test
    fun matchesTheIosVector() {
        assertEquals("@256T-ARZ6", PangeaCode.code(0x8A6D2B1FL))
    }

    @Test
    fun formatIsFourDashFour() {
        val code = PangeaCode.code(0x1A2B3C4DL)
        assertTrue(code.startsWith("@"))
        assertEquals(10, code.length)
        assertEquals('-', code[5])
    }

    @Test
    fun lenientInput() {
        val code = PangeaCode.code(0x1A2B3C4DL)
        val bare = code.replace("@", "").replace("-", "")
        assertEquals(0x1A2B3C4DL, PangeaCode.nodeNum(bare.lowercase()))
        assertEquals(0x1A2B3C4DL, PangeaCode.nodeNum(" $code "))
        val lookalike = bare.replace("0", "O").replace("1", "l")
        assertEquals(0x1A2B3C4DL, PangeaCode.nodeNum(lookalike))
    }

    @Test
    fun invisibleAndTypographicCharactersAreIgnored() {
        val raw = PangeaCode.code(0x1A2B3C4DL).replace("@", "").replace("-", "")
        val first = raw.take(4)
        val last = raw.takeLast(4)
        val pastes =
            listOf(
                "@$first‑$last",
                "@$first–$last",
                " @$first-$last ",
                "‎@$first​-$last‬",
                "$first‍$last",
                "$first\t$last\n",
            )
        pastes.forEach { assertEquals(0x1A2B3C4DL, PangeaCode.nodeNum(it), it) }
        assertNull(PangeaCode.nodeNum("$first$last́"))
    }

    @Test
    fun pastedPunctuationAndWidthFormsAreAccepted() {
        val num = 0x8A6D2B1FL
        val code = PangeaCode.code(num)
        val fullwidth = code.map { (it.code + 0xFEE0).toChar() }.joinToString("")
        val pastes =
            listOf(
                "$code.",
                "“$code”",
                "\"$code\"",
                "($code)",
                "$code!",
                fullwidth,
                "!8a6d​2b1f",
                "‎!8a6d2b1f",
                "“!8a6d2b1f”.",
                "!８ａ６ｄ２ｂ１ｆ",
            )
        pastes.forEach {
            assertEquals(num, PangeaCode.invite(it)?.nodeNum, it)
            assertTrue(PangeaCode.looksComplete(it), it)
        }
        assertNull(PangeaCode.invite("$codé"))
        assertNull(PangeaCode.invite("@256T-ARZ7."))
        assertNull(PangeaCode.invite("＠256T-ARZ7"))
    }

    @Test
    fun everySingleTypoIsRejected() {
        val alphabet = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
        val raw = PangeaCode.code(0x1A2B3C4DL).filter { it != '@' && it != '-' }
        for (position in raw.indices) {
            for (replacement in alphabet) {
                if (replacement == raw[position]) continue
                val typo = raw.substring(0, position) + replacement + raw.substring(position + 1)
                assertNull(PangeaCode.nodeNum(typo), "typo at $position -> $replacement")
            }
        }
    }

    @Test
    fun hugePastesAreRefused() {
        val huge = " ".repeat(100_000) + PangeaCode.code(0x8A6D2B1FL)
        assertNull(PangeaCode.invite(huge))
        assertNull(PangeaCode.nodeNum(huge))
        assertTrue(PangeaCode.looksComplete(huge))
    }

    @Test
    fun codeOnlyLooksWrongOnceComplete() {
        assertFalse(PangeaCode.looksComplete("@4KQ2-F9"))
        assertFalse(PangeaCode.looksComplete("@4KQ2-F9M"))
        assertTrue(PangeaCode.looksComplete("@4KQ2-F9MX"))
        assertTrue(PangeaCode.looksComplete("4kq2f9mx"))
        assertFalse(PangeaCode.looksComplete("!a1b2"))
        assertTrue(PangeaCode.looksComplete("!a1b2c3d4"))
    }

    @Test
    fun rejectsReservedBroadcastAndWrongLength() {
        assertNull(PangeaCode.nodeNum(PangeaCode.code(0)))
        assertNull(PangeaCode.nodeNum(PangeaCode.code(0xFFFFFFFFL)))
        assertNull(PangeaCode.nodeNum("@4KQ2"))
        assertNull(PangeaCode.nodeNum(""))
        assertEquals(0x1A2B3C4DL, PangeaCode.invite("!1a2b3c4d")?.nodeNum)
    }

    @Test
    fun linkCarriesContact() {
        val user = user("Sam Rivera", ByteArray(32) { 7 }.toByteString())
        val link = PangeaCode.link(0x1A2B3C4DL, user)
        assertTrue(link.startsWith("pangea://dm/"))
        val invite = assertNotNull(PangeaCode.invite(link))
        assertEquals(0x1A2B3C4DL, invite.nodeNum)
        assertEquals("Sam Rivera", invite.displayName)
        assertEquals(user.public_key, invite.contact?.user?.public_key)
    }

    @Test
    fun linkWithoutContactFallsBackToCode() {
        val code = PangeaCode.code(0x1A2B3C4DL).filter { it != '@' && it != '-' }
        val invite = assertNotNull(PangeaCode.invite("pangea://dm/$code"))
        assertEquals(0x1A2B3C4DL, invite.nodeNum)
        assertNull(invite.contact)
    }

    @Test
    fun linkWhoseCodeIsSomeoneElseIsRefused() {
        val user = user("Sam", ByteArray(32) { 1 }.toByteString())
        val real = PangeaCode.link(0x1A2B3C4DL, user)
        val fragment = real.substringAfter('#')
        val otherCode = PangeaCode.code(0xDEADBEEFL).filter { it != '@' && it != '-' }
        assertNull(PangeaCode.invite("pangea://dm/$otherCode#$fragment"))
        assertEquals(0x1A2B3C4DL, PangeaCode.invite("pangea://dm/#$fragment")?.nodeNum)
    }

    @Test
    fun ignoresUnrelatedLinks() {
        assertNull(PangeaCode.invite("https://example.com/dm/ABC"))
        assertNull(PangeaCode.invite("hello there"))
    }
}
