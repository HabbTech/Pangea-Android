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
@file:Suppress("MagicNumber")

package org.meshtastic.core.model.pangea

import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString

/**
 * Byte-level helpers shared by every Pangea profile message. Everything rides on PRIVATE_APP, which other Meshtastic
 * apps ignore. Must stay byte-for-byte compatible with Pangea-Apple's `PangeaWire` (ProfileCard.swift).
 */
object PangeaWire {
    val cardMagic: ByteString = "PGP2".encodeUtf8()
    val requestMagic: ByteString = "PGQ3".encodeUtf8()
    val manifestMagic: ByteString = "PGM1".encodeUtf8()
    val chunkMagic: ByteString = "PGC2".encodeUtf8()
    val cardRequestMagic: ByteString = "PGK1".encodeUtf8()

    const val TAG_BYTES = 8

    /** First four bytes of SHA-256 as a big-endian unsigned value, never 0 (0 means "no photo"). */
    fun hash(data: ByteString): UInt {
        val digest = data.sha256()
        val value =
            (digest[0].toUInt() and 0xFFu shl 24) or
                (digest[1].toUInt() and 0xFFu shl 16) or
                (digest[2].toUInt() and 0xFFu shl 8) or
                (digest[3].toUInt() and 0xFFu)
        return if (value == 0u) 1u else value
    }

    /** First eight bytes of SHA-256: what pieces and manifest parts are checked against. */
    fun tag(data: ByteString): ByteString = data.sha256().substring(0, TAG_BYTES)

    fun bytes(value: UInt): ByteString = byteArrayOf(
        (value shr 24).toByte(),
        (value shr 16 and 0xFFu).toByte(),
        (value shr 8 and 0xFFu).toByte(),
        (value and 0xFFu).toByte(),
    )
        .toByteString()

    internal fun concat(vararg parts: ByteString): ByteString {
        val buffer = Buffer()
        parts.forEach { buffer.write(it) }
        return buffer.readByteString()
    }

    /** Sequential reader over a payload; every read returns null once the data runs out. */
    class Reader(private val data: ByteString) {
        private var offset = 0

        fun byte(): UByte? {
            if (offset >= data.size) return null
            return data[offset++].toUByte()
        }

        fun take(count: Int): ByteString? {
            if (count < 0 || offset + count > data.size) return null
            val out = data.substring(offset, offset + count)
            offset += count
            return out
        }

        fun uint32(): UInt? {
            val bytes = take(4) ?: return null
            var value = 0u
            for (i in 0 until 4) value = (value shl 8) or (bytes[i].toUInt() and 0xFFu)
            return value
        }

        fun rest(): ByteString {
            val out = if (offset >= data.size) ByteString.EMPTY else data.substring(offset)
            offset = data.size
            return out
        }
    }
}

/** The longest prefix of whole code points that fits in [limit] UTF-8 bytes (grapheme-safe enough for names). */
fun String.clampedUtf8(limit: Int): String {
    if (encodeUtf8().size <= limit) return this
    var used = 0
    var end = 0
    var index = 0
    while (index < length) {
        val clusterEnd = graphemeEnd(this, index)
        val size = substring(index, clusterEnd).encodeUtf8().size
        if (used + size > limit) break
        used += size
        end = clusterEnd
        index = clusterEnd
    }
    return substring(0, end)
}

/**
 * End of the user-perceived character starting at [start]: a base code point plus any following combining marks,
 * variation selectors, emoji modifiers, tag characters and ZWJ-joined code points. Swift slices by Character (extended
 * grapheme cluster); this approximation agrees for every emoji the profile editor offers.
 */
internal fun graphemeEnd(text: String, start: Int): Int {
    var index = start + CodePoints.charCountAt(text, start)
    while (index < text.length) {
        val cp = CodePoints.codePointAt(text, index)
        val joinsPrevious = isCombining(cp) || cp == ZWJ || isPreviousZwj(text, index)
        if (!joinsPrevious) break
        index += CodePoints.charCountAt(text, index)
    }
    return index
}

private const val ZWJ = 0x200D

private fun isPreviousZwj(text: String, index: Int): Boolean {
    if (index == 0) return false
    val prev = CodePoints.codePointBefore(text, index)
    return prev == ZWJ
}

private fun isCombining(cp: Int): Boolean = cp in 0x0300..0x036F ||
    cp in 0x1AB0..0x1AFF ||
    cp in 0x1DC0..0x1DFF ||
    cp in 0x20D0..0x20FF ||
    cp in 0xFE00..0xFE0F ||
    cp in 0xFE20..0xFE2F ||
    cp in 0x1F3FB..0x1F3FF ||
    cp in 0xE0020..0xE007F ||
    cp in 0xE0100..0xE01EF ||
    UnicodeProps.isMark(cp)

/** Minimal code-point helpers usable from commonMain (Kotlin's Char API is UTF-16 only). */
internal object CodePoints {
    fun charCountAt(text: String, index: Int): Int =
        if (text[index].isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()) 2 else 1

    fun codePointAt(text: String, index: Int): Int {
        val high = text[index]
        if (high.isHighSurrogate() && index + 1 < text.length) {
            val low = text[index + 1]
            if (low.isLowSurrogate()) return toCodePoint(high, low)
        }
        return high.code
    }

    fun codePointBefore(text: String, index: Int): Int {
        val low = text[index - 1]
        if (low.isLowSurrogate() && index - 2 >= 0) {
            val high = text[index - 2]
            if (high.isHighSurrogate()) return toCodePoint(high, low)
        }
        return low.code
    }

    fun toCodePoint(high: Char, low: Char): Int = ((high.code - 0xD800) shl 10) + (low.code - 0xDC00) + 0x10000

    fun codePoints(text: String): List<Int> {
        val out = ArrayList<Int>(text.length)
        var i = 0
        while (i < text.length) {
            out += codePointAt(text, i)
            i += charCountAt(text, i)
        }
        return out
    }

    fun fromCodePoints(codePoints: List<Int>): String {
        val sb = StringBuilder(codePoints.size)
        codePoints.forEach { appendCodePoint(sb, it) }
        return sb.toString()
    }

    fun appendCodePoint(sb: StringBuilder, cp: Int) {
        if (cp < 0x10000) {
            sb.append(cp.toChar())
        } else {
            val v = cp - 0x10000
            sb.append((0xD800 + (v shr 10)).toChar())
            sb.append((0xDC00 + (v and 0x3FF)).toChar())
        }
    }

    fun utf8Width(cp: Int): Int = when {
        cp < 0x80 -> 1
        cp < 0x800 -> 2
        cp < 0x10000 -> 3
        else -> 4
    }
}
