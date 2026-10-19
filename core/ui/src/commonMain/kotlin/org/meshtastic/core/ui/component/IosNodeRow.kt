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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.common.util.MeasurementSystem
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.util.toDistanceString
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.node_list_click_label
import org.meshtastic.core.resources.node_list_long_click_label
import org.meshtastic.core.resources.pangea_battery_charging
import org.meshtastic.core.resources.pangea_hops_away
import org.meshtastic.core.resources.pangea_radio_connected
import org.meshtastic.core.resources.pangea_role_value
import org.meshtastic.core.resources.unknown_username
import org.meshtastic.core.ui.icon.Favorite
import org.meshtastic.core.ui.icon.HopCount
import org.meshtastic.core.ui.icon.Lock
import org.meshtastic.core.ui.icon.LockOpen
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.util.LocalModemPreset

/**
 * A radio in Expert Mode's Nodes list, drawn like the Pangea iPhone app's: its short name on its colour with the
 * battery under it, then a line each for the name (with its key lock), "Connected" for ours, when it was last heard,
 * its role and how many hops away, a star for favourites and a chevron.
 */
@Suppress("LongMethod", "LongParameterList")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IosNodeRow(
    thisNode: Node?,
    thatNode: Node,
    distanceUnits: MeasurementSystem,
    connectionState: ConnectionState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
) {
    val c = Ios.colors
    val isThisNode = thisNode?.num == thatNode.num
    val name = thatNode.user.long_name.ifEmpty { stringResource(Res.string.unknown_username) }
    val roleLabel = stringResource(thatNode.user.role.label)
    val distance =
        remember(thisNode, thatNode, distanceUnits) {
            thisNode?.distance(thatNode)?.takeIf { it > 0 }?.toDistanceString(distanceUnits)
        }
    val strings = rememberNodeDescriptionStrings()
    val modemPreset = LocalModemPreset.current
    val description =
        remember(thatNode, distance, strings, modemPreset, roleLabel) {
            buildNodeDescription(
                name = name,
                isOnline = thatNode.isOnline,
                isFavorite = thatNode.isFavorite,
                lastHeard = thatNode.lastHeard,
                role = roleLabel,
                hopsAway = thatNode.hopsAway,
                batteryLevel = thatNode.batteryLevel,
                distance = distance,
                snr = thatNode.snrOrNull,
                viaMqtt = thatNode.viaMqtt,
                strings = strings,
                modemPreset = modemPreset,
                isUnknownUser = thatNode.isUnknownUser,
            )
        }
    Column(
        modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = description
                role = Role.Button
            }
            .combinedClickable(
                onClickLabel = stringResource(Res.string.node_list_click_label),
                onClick = onClick,
                onLongClickLabel = stringResource(Res.string.node_list_long_click_label),
                onLongClick = onLongClick,
            ),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(AVATAR_COLUMN), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(AVATAR).clip(CircleShape).background(Color(thatNode.colors.second)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(thatNode.user.short_name, style = Ios.type.title2, color = Color(thatNode.colors.first))
                }
                thatNode.batteryLevel
                    ?.takeIf { it > 0 }
                    ?.let { level ->
                        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            BatteryGlyph(level)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (level > FULL_BATTERY) {
                                    stringResource(Res.string.pangea_battery_charging)
                                } else {
                                    "$level%"
                                },
                                style = Ios.type.footnote,
                                color = c.secondaryLabel,
                            )
                        }
                    }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Line(
                    icon = if (thatNode.hasPKC) MeshtasticIcons.Lock else MeshtasticIcons.LockOpen,
                    tint = if (thatNode.hasPKC) c.systemGreen else c.systemYellow,
                    text = name,
                    color = c.label,
                )
                if (isThisNode && connectionState is ConnectionState.Connected) {
                    Line(IosIcons.Antenna, c.systemGreen, stringResource(Res.string.pangea_radio_connected))
                }
                if (thatNode.lastHeard > 0) {
                    Line(
                        icon = IosIcons.CheckCircle,
                        tint = if (thatNode.isOnline) c.systemGreen else c.tertiaryLabel,
                        text = DateFormatter.formatDateTimeShort(thatNode.lastHeard * MILLIS),
                    )
                }
                Line(IosIcons.Radio, c.label, stringResource(Res.string.pangea_role_value, roleLabel))
                if (!isThisNode && thatNode.hopsAway > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LineIcon(MeshtasticIcons.HopCount, c.label)
                        Text(
                            stringResource(Res.string.pangea_hops_away),
                            style = Ios.type.body,
                            color = c.secondaryLabel,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            thatNode.hopsAway.toString(),
                            style = Ios.type.body,
                            color = c.label,
                            modifier =
                            Modifier.border(1.5.dp, c.label, RoundedCornerShape(5.dp)).padding(horizontal = 7.dp),
                        )
                    }
                }
            }
            Column(Modifier.height(AVATAR), horizontalAlignment = Alignment.End) {
                if (thatNode.isFavorite) {
                    Icon(MeshtasticIcons.Favorite, null, tint = c.systemYellow, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.weight(1f))
                Icon(IosIcons.ChevronRight, null, tint = c.tertiaryLabel, modifier = Modifier.size(14.dp))
                Spacer(Modifier.weight(1f))
            }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(0.5.dp).background(c.separator))
    }
}

@Composable
private fun Line(icon: ImageVector, tint: Color, text: String, color: Color = Ios.colors.secondaryLabel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LineIcon(icon, tint)
        Text(text, style = Ios.type.body, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LineIcon(icon: ImageVector, tint: Color) {
    Box(Modifier.width(34.dp), contentAlignment = Alignment.CenterStart) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** iOS's battery symbol: an outline with a nub, filled to [level] (green when on power, red when low). */
@Composable
private fun BatteryGlyph(level: Int) {
    val c = Ios.colors
    val fill =
        when {
            level > FULL_BATTERY -> c.systemGreen
            level <= LOW_BATTERY -> c.systemRed
            else -> c.label
        }
    val outline = c.secondaryLabel
    Canvas(Modifier.size(width = 26.dp, height = 13.dp)) {
        val stroke = 1.2.dp.toPx()
        val nub = 2.dp.toPx()
        val body = Size(size.width - nub - stroke, size.height - stroke)
        drawRoundRect(outline, Offset(stroke / 2, stroke / 2), body, CornerRadius(3.dp.toPx()), style = Stroke(stroke))
        drawRoundRect(
            outline,
            Offset(size.width - nub, size.height * (1 - NUB_HEIGHT) / 2),
            Size(nub, size.height * NUB_HEIGHT),
            CornerRadius(1.dp.toPx()),
        )
        val inset = 2.4.dp.toPx()
        val fraction = level.coerceAtMost(FULL_BATTERY) / FULL_BATTERY.toFloat()
        drawRoundRect(
            fill,
            Offset(inset, inset),
            Size((body.width - 2 * inset + stroke) * fraction, size.height - 2 * inset),
            CornerRadius(1.5.dp.toPx()),
        )
    }
}

private val AVATAR = 72.dp
private val AVATAR_COLUMN = 80.dp
private const val FULL_BATTERY = 100
private const val LOW_BATTERY = 20
private const val MILLIS = 1000L

/** The battery's nub, as a share of its height. */
private const val NUB_HEIGHT = 0.36f
