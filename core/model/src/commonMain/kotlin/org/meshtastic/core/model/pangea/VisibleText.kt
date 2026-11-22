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
@file:Suppress("MagicNumber", "TooManyFunctions")

package org.meshtastic.core.model.pangea

// One rule for every name that's shown: people's names (theirs and mine), short names, group names and emoji
// (ported from Pangea-Apple ProfileCard.swift). A character is hidden when Unicode says it draws nothing (control and
// format characters, line and paragraph separators, private-use and unassigned code points, anything
// default-ignorable such as zero-width spaces, soft hyphens, variation selectors, tags and the Hangul fillers) or it's
// a blank that looks like a gap (U+2800 braille blank, U+FFFC). A few are kept where visible text needs them:
// - a zero-width joiner between two emoji, or between two letters or marks of a non-Latin script
// - a zero-width non-joiner between two such letters or marks
// - an emoji or text variation selector right after an emoji
// - an ideographic variation selector right after an ideograph
// - the tag characters of a subdivision flag
// A combining mark at the very start, or right after a space, goes too.

/** Without hidden characters. Spaces and line breaks are left as they are. */
val String.withoutHiddenCharacters: String
    get() = CodePoints.fromCodePoints(VisibleText.scalars(this, plainSpaces = false))

/** Hidden characters that can make it look like another name, or cut it short on the radio. */
val String.hasHiddenCharacters: Boolean
    get() =
        VisibleText.scalars(this, plainSpaces = false, dropStrayMarks = false).size != CodePoints.codePoints(this).size

/** Without hidden characters, every kind of space or line break made one plain space, and trimmed. Idempotent. */
val String.cleanedName: String
    get() = CodePoints.fromCodePoints(VisibleText.scalars(this, plainSpaces = true)).trim(' ')

/** A name I typed for myself, as [cleanedName], cut to fit in whole characters. "" when nothing visible is left. */
fun String.cleanedName(fittingBytes: Int): String = cutName(fittingBytes).first

/**
 * [cleanedName] cut to [bytes], and whether it's "" only because not even the first visible character fits, as opposed
 * to there being nothing.
 */
fun String.cutName(bytes: Int): Pair<String, Boolean> {
    val start = CodePoints.fromCodePoints(VisibleText.scalars(this, plainSpaces = true, stopPast = bytes)).trim(' ')
    val cleaned = start.clampedUtf8(bytes).cleanedName
    if (cleaned.isBlankName) return "" to !start.isBlankName
    return cleaned to false
}

/** Nothing to see: no letter, number, punctuation or symbol. */
val String.isBlankName: Boolean
    get() {
        var index = 0
        while (index < length) {
            val end = graphemeEnd(this, index)
            val base = CodePoints.codePointAt(this, index)
            if (!UnicodeProps.isHiddenByDefault(base) && UnicodeProps.isVisibleGlyphCategory(base)) return false
            index = end
        }
        return true
    }

internal object VisibleText {
    /** The visible code points of [text]. See the file comment for the rules. */
    @Suppress("CyclomaticComplexMethod", "LongMethod", "NestedBlockDepth", "LoopWithTooManyJumpStatements")
    fun scalars(text: String, plainSpaces: Boolean, dropStrayMarks: Boolean = true, stopPast: Int? = null): List<Int> {
        val scalars = CodePoints.codePoints(text)
        val out = ArrayList<Int>(scalars.size)
        var bytes = 0
        fun keep(cp: Int) {
            out += cp
            bytes += CodePoints.utf8Width(cp)
        }
        var i = 0
        while (i < scalars.size) {
            if (stopPast != null && bytes - (if (out.firstOrNull() == SPACE) 1 else 0) > stopPast) break
            val scalar = scalars[i]
            val after = i + 1
            if (UnicodeProps.isWhitespace(scalar)) {
                if (!plainSpaces) {
                    keep(scalar)
                } else if (out.lastOrNull() != SPACE) {
                    keep(SPACE)
                }
                i = after
                continue
            }
            if (!UnicodeProps.isHiddenByDefault(scalar)) {
                val lastIsSpaceOrNone = out.lastOrNull()?.let { UnicodeProps.isWhitespace(it) } ?: true
                if (!dropStrayMarks || !UnicodeProps.isMark(scalar) || !lastIsSpaceOrNone) keep(scalar)
                i = after
                continue
            }
            val previous = out.lastOrNull()
            val next =
                if (after < scalars.size && !UnicodeProps.isHiddenByDefault(scalars[after])) scalars[after] else null
            when (scalar) {
                0x200D -> {
                    if (previous != null && next != null) {
                        val emojiJoin =
                            UnicodeProps.isEmojiPart(previous) && UnicodeProps.isEmoji(next) && next >= ASCII_LIMIT
                        val scriptJoin =
                            UnicodeProps.isScriptLetterOrMark(previous) && UnicodeProps.isScriptLetterOrMark(next)
                        if (emojiJoin || scriptJoin) keep(scalar)
                    }
                }

                0x200C -> {
                    if (
                        previous != null &&
                        next != null &&
                        UnicodeProps.isScriptLetterOrMark(previous) &&
                        UnicodeProps.isScriptLetterOrMark(next)
                    ) {
                        keep(scalar)
                    }
                }

                0xFE0E,
                0xFE0F,
                -> if (previous != null && UnicodeProps.isEmoji(previous)) keep(scalar)

                in 0xFE00..0xFE0D,
                in 0xE0100..0xE01EF,
                -> if (previous != null && UnicodeProps.isIdeographic(previous)) keep(scalar)

                in 0xE0020..0xE007F -> {
                    // The run of tag characters is kept whole if it's a subdivision flag, else dropped whole.
                    var end = i
                    while (end < scalars.size && scalars[end] in 0xE0020..0xE007F && scalars[end] != CANCEL_TAG) end++
                    val closed = end < scalars.size && scalars[end] == CANCEL_TAG
                    if (closed && end > i && previous == BLACK_FLAG) {
                        for (k in i..end) keep(scalars[k])
                    }
                    i = if (closed) end + 1 else end
                    continue
                }

                else -> Unit
            }
            i = after
        }
        return out
    }

    private const val SPACE = 0x20
    private const val ASCII_LIMIT = 0x80
    private const val CANCEL_TAG = 0xE007F
    private const val BLACK_FLAG = 0x1F3F4
}

/**
 * The Unicode properties the name rules need, for any code point. Kotlin common only classifies UTF-16 units, so BMP
 * code points use [Char.category] and supplementary planes use the ranges that matter for names and emoji.
 */
@Suppress("MagicNumber")
internal object UnicodeProps {
    fun category(cp: Int): CharCategory? {
        if (cp < 0x10000) return cp.toChar().category
        // Narrow ranges first: `when` takes the first branch that matches.
        return when (cp) {
            in 0x1F3FB..0x1F3FF -> CharCategory.MODIFIER_SYMBOL
            in 0x1F000..0x1FAFF -> CharCategory.OTHER_SYMBOL
            in 0x1D173..0x1D17A -> CharCategory.FORMAT
            in 0x1BCA0..0x1BCA3 -> CharCategory.FORMAT
            in 0x1D400..0x1D7FF -> CharCategory.UPPERCASE_LETTER
            in 0x1D100..0x1D1FF -> CharCategory.OTHER_SYMBOL
            in 0xE0000..0xE0FFF -> if (cp in 0xE0100..0xE01EF) CharCategory.NON_SPACING_MARK else CharCategory.FORMAT
            in 0xF0000..0x10FFFF -> CharCategory.PRIVATE_USE
            in 0x20000..0x3FFFF -> CharCategory.OTHER_LETTER
            in 0x10000..0x1BFFF -> CharCategory.OTHER_LETTER
            else -> CharCategory.UNASSIGNED
        }
    }

    fun isMark(cp: Int): Boolean = when (category(cp)) {
        CharCategory.NON_SPACING_MARK,
        CharCategory.COMBINING_SPACING_MARK,
        CharCategory.ENCLOSING_MARK,
        -> true

        else -> false
    }

    fun isLetter(cp: Int): Boolean = when (category(cp)) {
        CharCategory.UPPERCASE_LETTER,
        CharCategory.LOWERCASE_LETTER,
        CharCategory.TITLECASE_LETTER,
        CharCategory.MODIFIER_LETTER,
        CharCategory.OTHER_LETTER,
        -> true

        else -> false
    }

    fun isVisibleGlyphCategory(cp: Int): Boolean = when (category(cp)) {
        CharCategory.UPPERCASE_LETTER,
        CharCategory.LOWERCASE_LETTER,
        CharCategory.TITLECASE_LETTER,
        CharCategory.MODIFIER_LETTER,
        CharCategory.OTHER_LETTER,
        CharCategory.DECIMAL_DIGIT_NUMBER,
        CharCategory.LETTER_NUMBER,
        CharCategory.OTHER_NUMBER,
        CharCategory.CONNECTOR_PUNCTUATION,
        CharCategory.DASH_PUNCTUATION,
        CharCategory.START_PUNCTUATION,
        CharCategory.END_PUNCTUATION,
        CharCategory.INITIAL_QUOTE_PUNCTUATION,
        CharCategory.FINAL_QUOTE_PUNCTUATION,
        CharCategory.OTHER_PUNCTUATION,
        CharCategory.MATH_SYMBOL,
        CharCategory.CURRENCY_SYMBOL,
        CharCategory.MODIFIER_SYMBOL,
        CharCategory.OTHER_SYMBOL,
        -> true

        else -> false
    }

    fun isWhitespace(cp: Int): Boolean = cp in 0x09..0x0D ||
        cp == 0x20 ||
        cp == 0x85 ||
        cp == 0xA0 ||
        cp == 0x1680 ||
        cp in 0x2000..0x200A ||
        cp == 0x2028 ||
        cp == 0x2029 ||
        cp == 0x202F ||
        cp == 0x205F ||
        cp == 0x3000

    fun isDefaultIgnorable(cp: Int): Boolean = cp == 0xAD ||
        cp == 0x34F ||
        cp == 0x61C ||
        cp in 0x115F..0x1160 ||
        cp in 0x17B4..0x17B5 ||
        cp in 0x180B..0x180F ||
        cp in 0x200B..0x200F ||
        cp in 0x202A..0x202E ||
        cp in 0x2060..0x206F ||
        cp == 0x3164 ||
        cp in 0xFE00..0xFE0F ||
        cp == 0xFEFF ||
        cp == 0xFFA0 ||
        cp in 0xFFF0..0xFFF8 ||
        cp in 0x1BCA0..0x1BCA3 ||
        cp in 0x1D173..0x1D17A ||
        cp in 0xE0000..0xE0FFF

    fun isHiddenByDefault(cp: Int): Boolean {
        val hiddenCategory =
            when (category(cp)) {
                CharCategory.CONTROL,
                CharCategory.FORMAT,
                CharCategory.LINE_SEPARATOR,
                CharCategory.PARAGRAPH_SEPARATOR,
                CharCategory.PRIVATE_USE,
                CharCategory.UNASSIGNED,
                CharCategory.SURROGATE,
                -> true

                else -> false
            }
        return hiddenCategory || isDefaultIgnorable(cp) || cp == 0x2800 || cp == 0xFFFC
    }

    /** Unicode `Emoji` property (approximate by block, exact for everything the profile editor offers). */
    fun isEmoji(cp: Int): Boolean = cp == 0x23 ||
        cp == 0x2A ||
        cp in 0x30..0x39 ||
        cp == 0xA9 ||
        cp == 0xAE ||
        cp == 0x203C ||
        cp == 0x2049 ||
        cp == 0x2122 ||
        cp == 0x2139 ||
        cp in 0x2194..0x2199 ||
        cp in 0x21A9..0x21AA ||
        cp in 0x231A..0x231B ||
        cp == 0x2328 ||
        cp == 0x23CF ||
        cp in 0x23E9..0x23F3 ||
        cp in 0x23F8..0x23FA ||
        cp == 0x24C2 ||
        cp in 0x25AA..0x25AB ||
        cp == 0x25B6 ||
        cp == 0x25C0 ||
        cp in 0x25FB..0x25FE ||
        cp in 0x2600..0x27BF ||
        cp in 0x2934..0x2935 ||
        cp in 0x2B05..0x2B07 ||
        cp in 0x2B1B..0x2B1C ||
        cp == 0x2B50 ||
        cp == 0x2B55 ||
        cp == 0x3030 ||
        cp == 0x303D ||
        cp == 0x3297 ||
        cp == 0x3299 ||
        cp in 0x1F000..0x1FAFF

    fun isEmojiModifier(cp: Int): Boolean = cp in 0x1F3FB..0x1F3FF

    fun isEmojiPart(cp: Int): Boolean =
        isEmoji(cp) || isEmojiModifier(cp) || cp == 0xFE0F || cp == 0x20E3 || cp in 0xE0020..0xE007F

    fun isIdeographic(cp: Int): Boolean = cp == 0x3006 ||
        cp == 0x3007 ||
        cp in 0x3021..0x3029 ||
        cp in 0x3038..0x303A ||
        cp in 0x3400..0x4DBF ||
        cp in 0x4E00..0x9FFF ||
        cp in 0xF900..0xFAFF ||
        cp in 0x17000..0x18AFF ||
        cp in 0x1B170..0x1B2FF ||
        cp in 0x20000..0x3FFFF

    /** A letter or mark outside ASCII: the scripts that need joiners and non-joiners. */
    fun isScriptLetterOrMark(cp: Int): Boolean = cp >= 0x80 && (isLetter(cp) || isMark(cp))
}
