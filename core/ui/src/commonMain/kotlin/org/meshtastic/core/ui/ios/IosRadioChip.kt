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
@file:Suppress("MagicNumber", "MatchingDeclarationName")

package org.meshtastic.core.ui.ios

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import org.meshtastic.core.model.MeshActivity

/**
 * What the Expert Mode radio chip shows: the connected radio's [shortName], whether it is [connected], and the
 * [activity] that flashes its send and receive lights.
 */
data class IosRadioChipState(val shortName: String, val connected: Boolean, val activity: Flow<MeshActivity>)

/** Set by the app shell in Expert Mode only, so screens show the chip there and nowhere in standard mode. */
@Suppress("CompositionLocalAllowlist")
val LocalIosRadioChip = staticCompositionLocalOf<IosRadioChipState?> { null }

/**
 * The radio chip at the top right of the iPhone app's Expert Mode screens (ConnectedDevice): the up and down activity
 * lights, a green link badge and the radio's short name, or a red broken link while no radio is connected. iOS's tap
 * pings the radio; Android's [onClick] opens our own node.
 */
@Composable
fun IosRadioChip(state: IosRadioChipState, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val c = Ios.colors
    var sent by remember { mutableIntStateOf(0) }
    var received by remember { mutableIntStateOf(0) }
    LaunchedEffect(state.activity) {
        state.activity.collect {
            when (it) {
                MeshActivity.Send -> sent++
                MeshActivity.Receive -> received++
            }
        }
    }
    Row(
        modifier
            .height(44.dp)
            .shadow(12.dp, CircleShape, ambientColor = c.chromeShadow, spotColor = c.chromeShadow)
            .clip(CircleShape)
            .background(c.chrome)
            .then(if (onClick != null) Modifier.iosPressable(onClick = onClick) else Modifier)
            .padding(start = 14.dp, end = if (state.connected) 16.dp else 12.dp)
            .semantics { contentDescription = state.shortName },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            ActivityLight(IosIcons.ArrowUpThin, flash = sent, color = c.systemGreen)
            ActivityLight(IosIcons.ArrowDownThin, flash = received, color = c.systemRed)
        }
        Spacer(Modifier.width(10.dp))
        if (state.connected) {
            Box(
                Modifier.size(22.dp).clip(CircleShape).background(c.systemGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IosIcons.Link, contentDescription = null, tint = c.systemGreen, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(state.shortName, style = Ios.type.callout, color = c.secondaryLabel, maxLines = 1)
        } else {
            Icon(
                IosIcons.Link,
                contentDescription = null,
                tint = c.systemRed,
                modifier =
                Modifier.size(20.dp).drawWithContent {
                    drawContent()
                    drawLine(
                        c.systemRed,
                        start = Offset(size.width * 0.12f, size.height * 0.12f),
                        end = Offset(size.width * 0.88f, size.height * 0.88f),
                        strokeWidth = 1.6.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                },
            )
        }
    }
}

/** One activity light: the arrow in the tint and a small ring that fills with [color] for a moment on each [flash]. */
@Composable
private fun ActivityLight(icon: ImageVector, flash: Int, color: Color) {
    val c = Ios.colors
    val brightness = remember { Animatable(0f) }
    LaunchedEffect(flash) {
        if (flash > 0) {
            brightness.snapTo(1f)
            brightness.animateTo(0f, tween(300))
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = c.tint, modifier = Modifier.size(9.dp))
        Spacer(Modifier.width(2.dp))
        Box(
            Modifier.size(7.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = brightness.value))
                .border(0.5.dp, c.label, CircleShape),
        )
    }
}

/** The chip, where the app shell provides one (Expert Mode); nothing otherwise. */
@Composable
fun IosRadioChipIfExpert(modifier: Modifier = Modifier) {
    LocalIosRadioChip.current?.let { IosRadioChip(it, modifier) }
}
