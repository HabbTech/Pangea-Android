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
@file:Suppress("LoopWithTooManyJumpStatements")

package org.meshtastic.core.model.pangea

/** The emoji a Pangea profile or chat icon can carry, grouped as the picker shows them. */
object EmojiCatalog {
    data class Emoji(val emoji: String, val name: String, val variants: List<String>)

    data class Group(val name: String, val emoji: List<Emoji>)

    /** The quick picks the profile editor shows first. Part of [contains], as on iOS. */
    val suggestedEmoji: List<String> =
        listOf("😀", "😎", "🤠", "🥾", "🏕️", "⛰️", "🌲", "🔥", "🐻", "🦊", "🐺", "🦅", "🚙", "📡", "🎣", "⭐️")

    val groups: List<Group> by lazy { parse(emojiCatalogChunks.joinToString("\n")) }

    /** Every emoji the editor can produce: the full list, every skin tone, and the quick picks. */
    private val all: Set<String> by lazy {
        buildSet {
            groups.forEach { group ->
                group.emoji.forEach { item ->
                    add(item.emoji)
                    addAll(item.variants)
                }
            }
            addAll(suggestedEmoji)
        }
    }

    /** Whether this is exactly one of the emoji someone can pick, so it draws as one emoji. */
    fun contains(text: String): Boolean = text in all

    private const val VARIATION_SELECTOR = '️'
    private const val KEYCAP = '⃣'

    internal fun parse(raw: String): List<Group> {
        val groups = mutableListOf<Group>()
        var name = ""
        var items = mutableListOf<Emoji>()
        for (line in raw.split("\n")) {
            if (line.isEmpty()) continue
            // "#️⃣" starts with "#" too; Swift compares whole characters, so only a bare "#" opens a group.
            if (line.startsWith("#") && line.getOrNull(1) != VARIATION_SELECTOR && line.getOrNull(1) != KEYCAP) {
                if (items.isNotEmpty()) groups += Group(name, items)
                name = line.drop(1)
                items = mutableListOf()
                continue
            }
            val parts = line.split(";")
            if (parts.size < 2) continue
            val variants = if (parts.size > 2) parts[2].split(" ").filter { it.isNotEmpty() } else emptyList()
            items += Emoji(parts[0], parts[1], variants)
        }
        if (items.isNotEmpty()) groups += Group(name, items)
        return groups
    }
}
