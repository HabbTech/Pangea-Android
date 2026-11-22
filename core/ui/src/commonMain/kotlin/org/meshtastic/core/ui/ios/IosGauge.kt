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

package org.meshtastic.core.ui.ios

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val GAUGE_START = 135f
private const val GAUGE_SWEEP = 270f

/**
 * A battery level as the iPhone app's node detail draws it (SwiftUI's circular `Gauge`): an open ring shaded red to
 * green, a dot at the level, the percentage in the middle over a small battery, and [voltage] under the ring.
 */
@Composable
fun IosBatteryGauge(level: Int, voltage: String?, modifier: Modifier = Modifier) {
    val c = Ios.colors
    val fraction = level.coerceIn(0, 100) / 100f
    val ring =
        Brush.sweepGradient(
            0f to c.systemGreen,
            0.125f to c.systemGreen,
            0.375f to c.systemRed,
            0.5f to c.systemOrange,
            0.75f to c.systemYellow,
            1f to c.systemGreen,
        )
    val marker = c.label
    val glyph = c.label
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(56.dp)) {
                val stroke = 5.5.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(
                    brush = ring,
                    startAngle = GAUGE_START,
                    sweepAngle = GAUGE_SWEEP,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                // The level: a ring-wide dot with a dark edge, as SwiftUI marks a gauge's value.
                val angle = (GAUGE_START + GAUGE_SWEEP * fraction) * PI.toFloat() / 180f
                val r = arcSize.width / 2
                val dot = Offset(center.x + r * cos(angle), center.y + r * sin(angle))
                drawCircle(Color.White, radius = stroke * 0.62f, center = dot)
                drawCircle(marker, radius = stroke * 0.62f, center = dot, style = Stroke(width = 1.4.dp.toPx()))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$level%", style = Ios.type.headline, color = c.label)
                Canvas(Modifier.padding(top = 1.dp).size(width = 14.dp, height = 7.dp).clearAndSetSemantics {}) {
                    val body = Size(size.width - 2.dp.toPx(), size.height)
                    drawRoundRect(
                        glyph,
                        size = body,
                        cornerRadius = CornerRadius(1.5.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                    drawRoundRect(
                        glyph,
                        topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                        size = Size((body.width - 3.dp.toPx()) * fraction, body.height - 3.dp.toPx()),
                        cornerRadius = CornerRadius(0.5.dp.toPx()),
                    )
                    drawRect(
                        glyph,
                        topLeft = Offset(body.width + 0.5.dp.toPx(), size.height * 0.3f),
                        size = Size(1.dp.toPx(), size.height * 0.4f),
                    )
                }
            }
        }
        if (voltage != null) {
            Text(
                voltage,
                style = Ios.type.subheadline,
                color = c.secondaryLabel,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
