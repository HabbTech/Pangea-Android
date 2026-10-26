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
package org.meshtastic.core.ui.util

import androidx.compose.runtime.Composable

/**
 * Lets the user pick a photo and hands back a square avatar small enough to send over the radio (at most
 * [AVATAR_MAX_BYTES], as Pangea-Apple's PhotoCompressor), or null when nothing usable was picked.
 */
@Composable expect fun rememberAvatarPhotoPicker(onPick: (ByteArray?) -> Unit): () -> Unit

/** 256 px, in up to ~70 radio packets: a few minutes to download, once, when asked. */
const val AVATAR_MAX_BYTES = 12_000

/** Sizes tried, largest first, until the photo fits. */
val AVATAR_SIZES = listOf(256, 224, 192, 160, 128, 96)

/** JPEG qualities tried at each size. */
val AVATAR_QUALITIES = listOf(50, 40, 30)
