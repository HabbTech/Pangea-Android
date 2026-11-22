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
package org.meshtastic.feature.messaging.pangea

import androidx.compose.runtime.staticCompositionLocalOf

/** What a chat row or bubble shows for a Pangea user: their photo, else their emoji on their colour. */
class AvatarVisual(val photo: ByteArray?, val emoji: String, val color: Int?) {
    val hasCustomAvatar: Boolean
        get() = photo != null || emoji.isNotEmpty()
}

/**
 * Looks up a Pangea avatar by conversation key ("8!a1b2c3d4" for a private chat). Provided by the Chats screen, so the
 * row composables don't need the profile store threaded through every layer; null everywhere else.
 */
@Suppress("CompositionLocalAllowlist")
val LocalPangeaAvatars = staticCompositionLocalOf<(String) -> AvatarVisual?> { { null } }

/** The node number of a private chat's key, or null for a channel. */
fun dmNodeNum(contactKey: String): Long? {
    val bang = contactKey.indexOf('!')
    if (bang < 0) return null
    return contactKey.substring(bang + 1).toLongOrNull(radix = HEX)
}

private const val HEX = 16
