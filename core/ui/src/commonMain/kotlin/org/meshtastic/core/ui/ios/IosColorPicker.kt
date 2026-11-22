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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.close
import org.meshtastic.core.resources.pangea_color_blue
import org.meshtastic.core.resources.pangea_color_green
import org.meshtastic.core.resources.pangea_color_grid
import org.meshtastic.core.resources.pangea_color_red
import org.meshtastic.core.resources.pangea_color_sliders
import org.meshtastic.core.resources.pangea_color_spectrum
import org.meshtastic.core.resources.pangea_colors
import kotlin.math.roundToInt

/**
 * iOS's colour picker sheet as `ColorPicker(supportsOpacity: false)` opens it: Grid, Spectrum and Sliders pages above
 * the chosen colour, at the medium detent. Each pick goes straight to [onColorChange], as iOS's binding does; the ✕
 * closes the sheet.
 */
@Composable
fun IosColorPickerSheet(
    color: Color,
    onColorChange: (Color) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Ios.colors
    val title = stringResource(Res.string.pangea_colors)
    var page by rememberSaveable { mutableIntStateOf(0) }
    IosSheet(
        title = title,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        heightFraction = 0.66f,
        header = {
            Box(Modifier.fillMaxWidth().height(74.dp).padding(horizontal = 16.dp)) {
                Text(title, style = Ios.type.headline, color = c.label, modifier = Modifier.align(Alignment.Center))
                IosGlassIconButton(
                    icon = IosIcons.Xmark,
                    contentDescription = stringResource(Res.string.close),
                    onClick = onDismissRequest,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        },
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            IosSegmentedControl(
                options =
                listOf(
                    stringResource(Res.string.pangea_color_grid),
                    stringResource(Res.string.pangea_color_spectrum),
                    stringResource(Res.string.pangea_color_sliders),
                ),
                selectedIndex = page,
                onSelect = { page = it },
            )
            Spacer(Modifier.height(18.dp))
            when (page) {
                0 -> ColorGrid(selected = color, onPick = onColorChange)
                1 -> ColorSpectrum(onPick = onColorChange)
                else -> ColorSliders(selected = color, onPick = onColorChange)
            }
            Spacer(Modifier.height(22.dp))
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(color))
        }
    }
}

/** The main row of iOS's grid, one colour per column; the rows above darken it and the rows below lighten it. */
private val GridHues =
    listOf(
        0xFF00A1D8,
        0xFF0061FE,
        0xFF4D22B2,
        0xFF982ABC,
        0xFFB92D5D,
        0xFFFF4015,
        0xFFFF6A00,
        0xFFFFAB01,
        0xFFFCC700,
        0xFFFEFB41,
        0xFFD9EC37,
        0xFF76BB40,
    )
        .map { Color(it) }

/** iOS's 12 × 10 grid: a grey ramp from white to black, then nine shades of each hue, darkest first. */
private val GridColors: List<List<Color>> =
    listOf(List(12) { lerp(Color.White, Color.Black, it / 11f) }) +
        (1..9).map { row ->
            val step = row - 5
            GridHues.map { hue ->
                if (step < 0) lerp(hue, Color.Black, -step * 0.19f) else lerp(hue, Color.White, step * 0.2f)
            }
        }

@Composable
private fun ColorGrid(selected: Color, onPick: (Color) -> Unit) {
    val chosen = selected.toArgb()
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))) {
        GridColors.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { swatch ->
                    Box(
                        Modifier.weight(1f)
                            .aspectRatio(1f)
                            .background(swatch)
                            .then(
                                if (swatch.toArgb() == chosen) {
                                    Modifier.border(3.dp, Color.White, RoundedCornerShape(4.dp))
                                } else {
                                    Modifier
                                },
                            )
                            .clickable { onPick(swatch) },
                    )
                }
            }
        }
    }
}

/** The colour at a point of the spectrum: hue across, white at the top through the pure hue to black at the bottom. */
private fun spectrumColor(x: Float, y: Float): Color {
    val hue = Color.hsv(x.coerceIn(0f, 1f) * 359f, 1f, 1f)
    val down = y.coerceIn(0f, 1f)
    return if (down < 0.5f) lerp(Color.White, hue, down * 2f) else lerp(hue, Color.Black, (down - 0.5f) * 2f)
}

@Composable
private fun ColorSpectrum(onPick: (Color) -> Unit) {
    val pick by rememberUpdatedState(onPick)
    var width by remember { mutableIntStateOf(1) }
    var height by remember { mutableIntStateOf(1) }
    val at = { point: Offset -> pick(spectrumColor(point.x / width, point.y / height)) }
    Box(
        Modifier.fillMaxWidth()
            .aspectRatio(1.2f)
            .clip(RoundedCornerShape(8.dp))
            .onSizeChanged {
                width = it.width.coerceAtLeast(1)
                height = it.height.coerceAtLeast(1)
            }
            .background(Brush.horizontalGradient((0..6).map { Color.hsv(it * 59.8f, 1f, 1f) }))
            .background(Brush.verticalGradient(listOf(Color.White, Color.Transparent, Color.Black)))
            .pointerInput(Unit) { detectTapGestures { at(it) } }
            .pointerInput(Unit) { detectDragGestures(onDragStart = { at(it) }) { change, _ -> at(change.position) } },
    )
}

@Composable
private fun ColorSliders(selected: Color, onPick: (Color) -> Unit) {
    val argb = selected.toArgb()
    val red = (argb shr 16) and 0xFF
    val green = (argb shr 8) and 0xFF
    val blue = argb and 0xFF
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ChannelSlider(
            label = stringResource(Res.string.pangea_color_red),
            value = red,
            from = Color(0, green, blue),
            to = Color(255, green, blue),
        ) {
            onPick(Color(it, green, blue))
        }
        ChannelSlider(
            label = stringResource(Res.string.pangea_color_green),
            value = green,
            from = Color(red, 0, blue),
            to = Color(red, 255, blue),
        ) {
            onPick(Color(red, it, blue))
        }
        ChannelSlider(
            label = stringResource(Res.string.pangea_color_blue),
            value = blue,
            from = Color(red, green, 0),
            to = Color(red, green, 255),
        ) {
            onPick(Color(red, green, it))
        }
    }
}

/** One channel of the Sliders page: its name, a capsule shaded across the channel's range, and the 0–255 value. */
@Composable
private fun ChannelSlider(label: String, value: Int, from: Color, to: Color, onValueChange: (Int) -> Unit) {
    val c = Ios.colors
    val update by rememberUpdatedState(onValueChange)
    var width by remember { mutableIntStateOf(0) }
    val thumb = 28.dp
    Column {
        Text(label.uppercase(), style = Ios.type.footnote, color = c.secondaryLabel)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.weight(1f)
                    .height(thumb)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(from, to)))
                    .onSizeChanged { width = it.width }
                    .pointerInput(Unit) {
                        val thumbPx = thumb.toPx()
                        val toValue = { x: Float ->
                            val travel = (width - thumbPx).coerceAtLeast(1f)
                            update((((x - thumbPx / 2f) / travel).coerceIn(0f, 1f) * 255f).roundToInt())
                        }
                        detectTapGestures { toValue(it.x) }
                    }
                    .pointerInput(Unit) {
                        val thumbPx = thumb.toPx()
                        val toValue = { x: Float ->
                            val travel = (width - thumbPx).coerceAtLeast(1f)
                            update((((x - thumbPx / 2f) / travel).coerceIn(0f, 1f) * 255f).roundToInt())
                        }
                        detectHorizontalDragGestures(onDragStart = { toValue(it.x) }) { change, _ ->
                            toValue(change.position.x)
                        }
                    }
                    .semantics {
                        contentDescription = label
                        progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), 0f..255f, steps = 254)
                        setProgress { target ->
                            update(target.roundToInt().coerceIn(0, 255))
                            true
                        }
                    },
            ) {
                Box(
                    Modifier.offset {
                        val travel = (width - thumb.roundToPx()).coerceAtLeast(0)
                        IntOffset((travel * value / 255f).roundToInt(), 0)
                    }
                        .size(thumb)
                        .border(3.dp, Color.White, CircleShape),
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier.width(60.dp).height(34.dp).clip(RoundedCornerShape(8.dp)).background(c.fieldFill),
                contentAlignment = Alignment.Center,
            ) {
                Text(value.toString(), style = Ios.type.body, color = c.label)
            }
        }
    }
}
