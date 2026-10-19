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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Groups a top bar's buttons in one glass capsule, as iOS 26 does ("↕ ⛓ CAPH"). Draws nothing when the [content] turns
 * out empty, so screens without actions don't get an empty pill.
 */
@Composable
fun IosActionCapsule(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val chrome = Ios.colors.chrome
    Layout(
        content = content,
        modifier =
        modifier.drawBehind {
            if (size.width > 0f) {
                drawRoundRect(chrome, cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2))
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val contentWidth = placeables.sumOf { it.width }
        if (contentWidth == 0) return@Layout layout(0, 0) {}
        val pad = 4.dp.roundToPx()
        val height = maxOf(placeables.maxOf { it.height }, 44.dp.roundToPx())
        layout(contentWidth + 2 * pad, height) {
            var x = pad
            placeables.forEach {
                it.placeRelative(x, (height - it.height) / 2)
                x += it.width
            }
        }
    }
}

/**
 * An iOS segmented control: a grey track with the chosen segment on a raised white pill. [options] are the labels; each
 * segment reports itself as a selectable tab for accessibility.
 */
@Composable
fun IosSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Ios.colors
    val track = if (c.isDark) Color(0xFF1C1C1E) else Color(0xFFEEEEF0)
    val pill = if (c.isDark) Color(0xFF636366) else Color.White
    Row(
        modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(10.dp)).background(track).padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                Modifier.weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (selected) {
                            Modifier.shadow(2.dp, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .background(pill)
                        } else {
                            Modifier
                        },
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab,
                    ) {
                        onSelect(index)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style =
                    Ios.type.footnote.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal),
                    color = c.label,
                    maxLines = 1,
                )
            }
        }
    }
}
