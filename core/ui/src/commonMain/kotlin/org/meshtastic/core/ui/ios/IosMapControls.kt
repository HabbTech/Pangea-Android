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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.orient_north
import org.meshtastic.core.resources.pangea_map_2d
import org.meshtastic.core.resources.pangea_map_3d
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * MapKit's `MapPitchToggle`: a round glass button reading **3D** in the tint, or **2D** once the map is tilted, which
 * switches between the two.
 */
@Composable
fun IosMapPitchToggle(pitched: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(if (pitched) Res.string.pangea_map_2d else Res.string.pangea_map_3d)
    IosRoundMapControl(onClick = onClick, contentDescription = label, modifier = modifier) {
        Text(label, style = Ios.type.headline, color = Ios.colors.tint)
    }
}

/**
 * MapKit's `MapCompass`: a round glass dial with ticks, **N** and a red north marker, turned so the marker points to
 * the map's north. Tapping it turns the map north up.
 */
@Composable
fun IosMapCompass(bearing: Float, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ios.colors
    val label = stringResource(Res.string.orient_north)
    IosRoundMapControl(onClick = onClick, contentDescription = label, modifier = modifier) {
        Box(Modifier.fillMaxSize().rotate(-bearing), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2
                val rim = radius - 4.dp.toPx()
                for (step in 1 until TICKS) {
                    val major = step % MAJOR_EVERY == 0
                    val angle = step * 2 * PI / TICKS - PI / 2
                    val inner = rim - (if (major) 5.dp else 3.dp).toPx()
                    drawLine(
                        color = c.secondaryLabel,
                        start = center + Offset((cos(angle) * inner).toFloat(), (sin(angle) * inner).toFloat()),
                        end = center + Offset((cos(angle) * rim).toFloat(), (sin(angle) * rim).toFloat()),
                        strokeWidth = (if (major) 1.5.dp else 1.dp).toPx(),
                        cap = StrokeCap.Round,
                    )
                }
                val tip = 2.5.dp.toPx()
                val base = 8.5.dp.toPx()
                val half = 3.5.dp.toPx()
                val north =
                    Path().apply {
                        moveTo(center.x, tip)
                        lineTo(center.x + half, base)
                        lineTo(center.x - half, base)
                        close()
                    }
                drawPath(north, c.systemRed)
            }
            Text("N", style = Ios.type.headline, color = c.label)
        }
    }
}

/** The 44 dp round glass disc MapKit draws its map controls on. */
@Composable
private fun IosRoundMapControl(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val c = Ios.colors
    Box(
        modifier
            .size(44.dp)
            .shadow(12.dp, CircleShape, ambientColor = c.chromeShadow, spotColor = c.chromeShadow)
            .clip(CircleShape)
            .background(c.chrome)
            .iosPressable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Tick marks round the compass dial; every [MAJOR_EVERY]th, a quarter of the way round, is a longer one. */
private const val TICKS = 12
private const val MAJOR_EVERY = 3
