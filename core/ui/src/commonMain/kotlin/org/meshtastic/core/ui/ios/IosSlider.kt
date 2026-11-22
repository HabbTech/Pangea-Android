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
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * iOS's slider: a thin grey track filled with the tint up to a white capsule thumb, and, when the value moves in
 * [steps], a grey dot under the track at each stop, as SwiftUI draws `Slider(value:in:step:)`.
 *
 * Built on Material's [Slider] for its gestures, keyboard and accessibility; only the drawing is iOS's.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IosSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val c = Ios.colors
    val interaction = remember { MutableInteractionSource() }
    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = interaction,
        thumb = {
            Box(
                Modifier.size(width = ThumbWidth, height = ThumbHeight)
                    // iOS lifts the white thumb off a white card with a clear shadow.
                    .shadow(4.dp, CircleShape, ambientColor = ThumbShadow, spotColor = ThumbShadow)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        },
        track = { state ->
            val fraction = ((state.value - valueRange.start) / span).coerceIn(0f, 1f)
            val active = if (enabled) c.tint else c.systemGray4
            Canvas(Modifier.fillMaxWidth().height(TrackHeight)) {
                // The thumb's centre travels between half a thumb in from either end; the fill and the stops follow
                // it there.
                val inset = ThumbWidth.toPx() / 2
                val radius = CornerRadius(size.height / 2, size.height / 2)
                drawRoundRect(c.secondaryFill, cornerRadius = radius)
                val filled = inset + (size.width - 2 * inset) * fraction
                drawRoundRect(active, size = Size(filled, size.height), cornerRadius = radius)
                if (steps > 0) {
                    // The stops sit under the track.
                    val y = size.height + TickGap.toPx()
                    val stops = steps + 1
                    for (i in 0..stops) {
                        val x = inset + (size.width - 2 * inset) * i / stops
                        drawCircle(c.systemGray4, radius = TickRadius.toPx(), center = Offset(x, y))
                    }
                }
            }
        },
    )
}

private val ThumbWidth = 38.dp
private val ThumbHeight = 24.dp
private val TrackHeight = 4.dp
private val TickGap = 9.dp
private val TickRadius = 1.5.dp
private val ThumbShadow = Color.Black.copy(alpha = 0.35f)
