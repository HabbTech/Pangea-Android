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
@file:Suppress("MagicNumber", "ReturnCount", "TooManyFunctions")

package org.meshtastic.core.model.pangea

import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString
import org.meshtastic.proto.SharedContact
import org.meshtastic.proto.User

/**
 * A person's @code is their radio's node number written in Crockford base32 with a check character, e.g. "@4KQ2-F9MX".
 * Short enough to say out loud, forgiving of O/0 and I/1 mix-ups, and a typo is caught instead of reaching a stranger.
 * Links and QR codes carry the code plus the full contact (name + public key), so scanning one connects instantly
 * without a round trip over the mesh.
 *
 * Byte-for-byte compatible with Pangea-Apple `PangeaCode.swift`: changing the alphabet or check digit would change
 * everyone's code.
 */
object PangeaCode {
    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    /** 7 × 5 bits covers a 32-bit node number. */
    private const val DATA_LENGTH = 7
    private const val BITS = 5
    private const val DIGIT_MASK = 31L
    private const val HALF = 4
    private const val NODE_ID_LENGTH = 9

    const val SCHEME = "pangea"
    const val CONTACT_HOST = "dm"

    /** The most a code, node ID or link can take up. Anything longer is refused before any other work. */
    private const val MAX_INPUT_LENGTH = 2048

    /** Sentence punctuation, quotes and brackets a code is often pasted with. */
    private const val EDGE_PUNCTUATION = ".,;:!?\"'“”‘’()[]<>"

    private const val UINT32_MAX = 0xFFFFFFFFL
    private const val FIRST_USABLE = 3L

    /** "@4KQ2-F9MX" */
    fun code(nodeNum: Long): String {
        val raw = rawCode(nodeNum and UINT32_MAX)
        return "@" + raw.take(HALF) + "-" + raw.takeLast(HALF)
    }

    /** Node number for a typed or pasted code, or null if it isn't a valid code. */
    @Suppress("ReturnCount")
    fun nodeNum(text: String): Long? {
        val cleanedText = clean(text) ?: return null
        val cleaned = normalize(cleanedText)
        if (cleaned.length != DATA_LENGTH + 1) return null
        val digits = cleaned.map { ALPHABET.indexOf(it).takeIf { d -> d >= 0 } ?: return null }
        var value = 0L
        for (digit in digits.take(DATA_LENGTH)) value = (value shl BITS) or digit.toLong()
        if (value > UINT32_MAX) return null
        if (checkDigit(digits.take(DATA_LENGTH)) != digits[DATA_LENGTH]) return null
        if (!isUsable(value)) return null
        return value
    }

    private fun rawCode(nodeNum: Long): String {
        val digits = IntArray(DATA_LENGTH)
        var value = nodeNum
        for (index in DATA_LENGTH - 1 downTo 0) {
            digits[index] = (value and DIGIT_MASK).toInt()
            value = value shr BITS
        }
        val check = checkDigit(digits.toList())
        return (digits.toList() + check).map { ALPHABET[it] }.joinToString("")
    }

    /**
     * Luhn mod 32: catches any single wrong character, and every swap of two neighbouring characters except "0" with
     * "Z". Changing it would change everyone's code, so that one gap stays.
     */
    private fun checkDigit(digits: List<Int>): Int {
        val base = ALPHABET.length
        var factor = 2
        var sum = 0
        for (digit in digits.reversed()) {
            val addend = factor * digit
            sum += addend / base + addend % base
            factor = if (factor == 2) 1 else 2
        }
        return (base - sum % base) % base
    }

    /** Uppercases, drops "@", dashes and spaces, and maps look-alike letters. */
    private fun normalize(text: String): String = buildString {
        for (cp in CodePoints.codePoints(text.uppercase())) {
            if (UnicodeProps.isWhitespace(cp) || UnicodeProps.category(cp) == CharCategory.FORMAT) continue
            when (cp) {
                '@'.code,
                '-'.code,
                '_'.code,
                in 0x2010..0x2015,
                0x2212,
                -> Unit

                'O'.code -> append('0')

                'I'.code,
                'L'.code,
                -> append('1')

                else -> CodePoints.appendCodePoint(this, cp)
            }
        }
    }

    /**
     * What a paste carries around a code: fullwidth forms fold to ASCII, invisible format characters go, and so do
     * whitespace, punctuation and quotes at either end, except a leading "!", which is the node ID form. Null when it's
     * too long to be a code at all.
     */
    private fun clean(input: String): String? {
        if (input.encodeToByteArray().size > MAX_INPUT_LENGTH) return null
        val scalars =
            CodePoints.codePoints(input).map(::foldCompatibility).filter {
                UnicodeProps.category(it) != CharCategory.FORMAT
            }
        fun isEdge(cp: Int) = UnicodeProps.isWhitespace(cp) || (cp < 0x10000 && cp.toChar() in EDGE_PUNCTUATION)
        var start = 0
        var end = scalars.size
        while (start < end && scalars[start] != '!'.code && isEdge(scalars[start])) start++
        while (end > start && isEdge(scalars[end - 1])) end--
        return CodePoints.fromCodePoints(scalars.subList(start, end))
    }

    /** The part of NFKC a pasted code needs: fullwidth ASCII and the ideographic space. */
    @Suppress("MagicNumber")
    private fun foldCompatibility(cp: Int): Int = when (cp) {
        in 0xFF01..0xFF5E -> cp - 0xFEE0
        0x3000 -> 0x20
        else -> cp
    }

    /** Long enough to be a whole code (or link / node ID), so "doesn't look right" isn't shown mid-typing. */
    fun looksComplete(input: String): Boolean {
        val text = clean(input) ?: return true
        if ("://" in text || text.startsWith("!")) return text.length >= NODE_ID_LENGTH
        return normalize(text).length >= DATA_LENGTH + 1
    }

    /** 0 and broadcast aren't people; 1–3 are reserved by the firmware. */
    private fun isUsable(num: Long): Boolean = num > FIRST_USABLE && num != UINT32_MAX

    /** Everything we learned from whatever the user typed, pasted or scanned. */
    data class Invite(
        val nodeNum: Long,
        /** Present when the invite came from a link or QR code. */
        val contact: SharedContact? = null,
    ) {
        val code: String
            get() = code(nodeNum)

        val displayName: String?
            get() = contact?.user?.long_name?.takeIf { it.isNotEmpty() }
    }

    /** Accepts "@4KQ2-F9MX", "4kq2f9mx", "!a1b2c3d4", pangea:// links and Meshtastic contact links. */
    fun invite(input: String): Invite? {
        val text = clean(input)?.takeIf { it.isNotEmpty() } ?: return null
        if (SCHEME_REGEX.containsMatchIn(text)) return inviteFromUrl(text)
        if (text.startsWith("!")) {
            val num = text.drop(1).toLongOrNull(radix = 16)
            if (num != null && num <= UINT32_MAX && isUsable(num)) return Invite(num)
        }
        return nodeNum(text)?.let { Invite(it) }
    }

    @Suppress("ReturnCount")
    fun inviteFromUrl(url: String): Invite? {
        val parsed = SimpleUrl.parse(url) ?: return null
        if (parsed.scheme.equals(SCHEME, ignoreCase = true) && parsed.host.equals(CONTACT_HOST, ignoreCase = true)) {
            val code = parsed.pathSegments.firstOrNull().orEmpty()
            val contact = parsed.fragment?.let(::decodeContact)
            if (contact != null && isUsable(contact.node_num.toLong() and UINT32_MAX)) {
                val contactNum = contact.node_num.toLong() and UINT32_MAX
                // The code shown in the link has to be the person it opens.
                if (code.isNotEmpty() && nodeNum(code) != contactNum) return null
                return Invite(contactNum, contact)
            }
            return nodeNum(code)?.let { Invite(it) }
        }
        if (url.lowercase().contains("meshtastic.org/v/#")) {
            val contact = decodeContact(url.substringAfterLast("#")) ?: return null
            val contactNum = contact.node_num.toLong() and UINT32_MAX
            if (isUsable(contactNum)) return Invite(contactNum, contact)
        }
        return null
    }

    /** pangea://dm/4KQ2F9MX#<SharedContact> */
    fun link(nodeNum: Long, user: User): String {
        val contact =
            SharedContact.Builder()
                .also {
                    it.node_num = (nodeNum and UINT32_MAX).toInt()
                    it.user = user
                }
                .build()
        val data = SharedContact.ADAPTER.encode(contact).toByteString().base64Url().trimEnd('=')
        val raw = rawCode(nodeNum and UINT32_MAX)
        return "$SCHEME://$CONTACT_HOST/$raw#$data"
    }

    private fun decodeContact(fragment: String): SharedContact? {
        val bytes = fragment.base64UrlToBase64().decodeBase64() ?: return null
        return runCatching { SharedContact.ADAPTER.decode(bytes) }.getOrNull()
    }

    private val SCHEME_REGEX = Regex("^[A-Za-z][A-Za-z0-9+.-]*:")
}

/** "-_" to "+/" and padding put back, as Pangea-Apple's `base64urlToBase64`. */
internal fun String.base64UrlToBase64(): String {
    val base64 = replace('-', '+').replace('_', '/')
    val pad = (4 - base64.length % 4) % 4
    return base64 + "=".repeat(pad)
}

/** Just enough URL parsing for pangea:// and Meshtastic links (scheme, host, path segments, fragment). */
internal data class SimpleUrl(
    val scheme: String,
    val host: String,
    val pathSegments: List<String>,
    val fragment: String?,
) {
    companion object {
        fun parse(url: String): SimpleUrl? {
            val schemeEnd = url.indexOf(':').takeIf { it > 0 } ?: return null
            val scheme = url.substring(0, schemeEnd)
            var rest = url.substring(schemeEnd + 1)
            val fragment = rest.substringAfter('#', "").takeIf { '#' in rest }
            rest = rest.substringBefore('#').substringBefore('?')
            if (!rest.startsWith("//")) {
                return SimpleUrl(scheme, "", rest.split('/').filter { it.isNotEmpty() }, fragment)
            }
            rest = rest.removePrefix("//")
            val host = rest.substringBefore('/')
            val path = rest.substringAfter('/', "")
            return SimpleUrl(scheme, host, path.split('/').filter { it.isNotEmpty() }, fragment)
        }
    }
}
