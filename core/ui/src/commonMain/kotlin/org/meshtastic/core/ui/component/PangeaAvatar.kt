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
package org.meshtastic.core.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.decodeToImageBitmap

/** A photo's bytes decoded once per composition key, or null when they aren't an image this platform can draw. */
@Composable
fun rememberAvatarBitmap(bytes: ByteArray?): ImageBitmap? =
    remember(bytes?.contentHashCode()) { bytes?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() } }

/**
 * Photo, then emoji, then initials, the same way across every Pangea screen.
 *
 * @param fallbackColor the circle behind initials (the node's colour) when the person picked none.
 */
@Composable
fun PangeaAvatar(
    photo: ImageBitmap?,
    emoji: String,
    colorIndex: Int?,
    initials: String,
    fallbackColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
) {
    val shape = CircleShape
    val background = colorIndex?.let(AvatarPalette::color) ?: fallbackColor
    when {
        photo != null ->
            Image(
                bitmap = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier.size(size).clip(shape),
            )

        emoji.isNotEmpty() ->
            Box(modifier.size(size).clip(shape).background(background), contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = (size.value * EMOJI_SCALE).sp)
            }

        else ->
            Box(modifier.size(size).clip(shape).background(background), contentAlignment = Alignment.Center) {
                Text(
                    initials.ifEmpty { "?" },
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (size.value * INITIALS_SCALE).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
    }
}

private const val EMOJI_SCALE = 0.5f
private const val INITIALS_SCALE = 0.34f
