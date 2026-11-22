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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.pangea_decrease
import org.meshtastic.core.resources.pangea_increase

/**
 * iOS's `Stepper`: − and + in a grey capsule split by a hairline. A side greys out at its end of [range], so the value
 * never leaves it.
 */
@Composable
fun IosStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = Ios.colors
    Row(
        modifier.size(width = 92.dp, height = 32.dp).clip(CircleShape).background(c.fieldFill),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperSide(
            icon = IosIcons.Minus,
            description = stringResource(Res.string.pangea_decrease),
            enabled = enabled && value > range.first,
        ) {
            onValueChange(value - 1)
        }
        Box(Modifier.width(1.dp).height(20.dp).background(c.separator))
        StepperSide(
            icon = IosIcons.Plus,
            description = stringResource(Res.string.pangea_increase),
            enabled = enabled && value < range.last,
        ) {
            onValueChange(value + 1)
        }
    }
}

@Composable
private fun RowScope.StepperSide(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    val c = Ios.colors
    Box(
        Modifier.weight(1f).fillMaxHeight().iosPressable(enabled = enabled, onClick = onClick).semantics {
            contentDescription = description
        },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (enabled) c.label else c.tertiaryLabel,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** The rainbow ring iOS draws around a colour well, hue running anticlockwise from red at the right. */
private val ColorWellRing = Brush.sweepGradient((0..12).map { Color.hsv(((12 - it) % 12) * 30f, 0.8f, 0.95f) })

/** iOS's `ColorPicker` well: [color] inside a rainbow ring. Tapping it is how the colour is changed. */
@Composable
fun IosColorWell(color: Color, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = Ios.colors
    Box(
        modifier
            .size(28.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(CircleShape)
            .background(ColorWellRing)
            .iosPressable(enabled = enabled, onClick = onClick)
            .padding(3.dp)
            .clip(CircleShape)
            .background(c.groupedRow)
            .padding(1.dp)
            .clip(CircleShape)
            .background(color),
    )
}
