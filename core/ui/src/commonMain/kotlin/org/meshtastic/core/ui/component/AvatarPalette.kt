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

import androidx.compose.ui.graphics.Color

/** The colours a Pangea profile can pick, in wire order (Pangea-Apple AvatarPalette). */
object AvatarPalette {
    @Suppress("MagicNumber")
    val colors: List<Color> =
        listOf(
            Color(0.94f, 0.33f, 0.05f), // Pangea orange
            Color(0.98f, 0.62f, 0.10f),
            Color(0.20f, 0.70f, 0.40f),
            Color(0.10f, 0.65f, 0.70f),
            Color(0.20f, 0.48f, 0.95f),
            Color(0.45f, 0.35f, 0.90f),
            Color(0.85f, 0.30f, 0.60f),
            Color(0.45f, 0.47f, 0.52f),
        )

    fun color(index: Int): Color = colors[((index % colors.size) + colors.size) % colors.size]
}
