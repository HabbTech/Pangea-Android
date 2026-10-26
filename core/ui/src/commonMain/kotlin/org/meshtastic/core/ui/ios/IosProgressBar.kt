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
package org.meshtastic.core.ui.ios

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * iOS's linear `ProgressView(value:)`: a thin capsule track in the system fill, filled from the leading end in the tint
 * up to [progress] (0 to 1).
 */
@Composable
fun IosProgressBar(progress: () -> Float, modifier: Modifier = Modifier, color: Color = Ios.colors.tint) {
    val track = Ios.colors.secondaryFill
    val value = progress().coerceIn(0f, 1f)
    Canvas(
        modifier.fillMaxWidth().height(4.dp).semantics { progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f) },
    ) {
        val radius = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(color = track, cornerRadius = radius)
        if (value > 0f) {
            drawRoundRect(color = color, size = Size(size.width * value, size.height), cornerRadius = radius)
        }
    }
}
