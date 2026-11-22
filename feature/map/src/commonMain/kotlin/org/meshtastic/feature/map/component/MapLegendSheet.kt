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

package org.meshtastic.feature.map.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.pangea_legend_nodes_section
import org.meshtastic.core.resources.pangea_legend_offline
import org.meshtastic.core.resources.pangea_legend_offline_badge
import org.meshtastic.core.resources.pangea_legend_offline_text
import org.meshtastic.core.resources.pangea_legend_online
import org.meshtastic.core.resources.pangea_legend_online_badge
import org.meshtastic.core.resources.pangea_legend_online_text
import org.meshtastic.core.resources.pangea_legend_precision
import org.meshtastic.core.resources.pangea_legend_precision_section
import org.meshtastic.core.resources.pangea_legend_precision_text
import org.meshtastic.core.resources.pangea_legend_route_end
import org.meshtastic.core.resources.pangea_legend_route_line
import org.meshtastic.core.resources.pangea_legend_route_line_text
import org.meshtastic.core.resources.pangea_legend_route_start
import org.meshtastic.core.resources.pangea_legend_routes_section
import org.meshtastic.core.resources.pangea_legend_waypoint
import org.meshtastic.core.resources.pangea_legend_waypoint_text
import org.meshtastic.core.resources.pangea_legend_waypoints_section
import org.meshtastic.core.resources.pangea_map_legend
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.ios.IosSheet
import org.meshtastic.core.ui.ios.iosRowSeparator

/**
 * The iPhone app's Map Legend (`MapLegend`): what each mark on the mesh map means, a symbol beside a name and a line of
 * explanation, grouped as Nodes, Waypoints, Position Precision and Routes.
 *
 * iOS's legend also has a detection-sensor node, trace routes and the convex hull; this map draws none of those, so the
 * legend leaves them out rather than explain marks nobody will find.
 */
@Composable
fun MapLegendSheet(onDismissRequest: () -> Unit) {
    IosSheet(
        title = stringResource(Res.string.pangea_map_legend),
        onDismissRequest = onDismissRequest,
        grouped = true,
        grabber = true,
    ) {
        MapLegendContent()
    }
}

/** The sheet's body, apart from the sheet so it can be shown without a window to host a modal. */
@Composable
internal fun MapLegendContent(modifier: Modifier = Modifier) {
    val c = Ios.colors
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        IosSection(header = stringResource(Res.string.pangea_legend_nodes_section)) {
            LegendRow(Res.string.pangea_legend_online, Res.string.pangea_legend_online_text) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(c.systemGreen.copy(alpha = 0.3f)))
                BadgeCircle(stringResource(Res.string.pangea_legend_online_badge), c.systemGreen)
            }
            LegendRow(Res.string.pangea_legend_offline, Res.string.pangea_legend_offline_text) {
                BadgeCircle(stringResource(Res.string.pangea_legend_offline_badge), c.systemGray)
            }
        }
        IosSection(header = stringResource(Res.string.pangea_legend_waypoints_section)) {
            LegendRow(Res.string.pangea_legend_waypoint, Res.string.pangea_legend_waypoint_text) {
                BadgeCircle("📍", c.systemOrange)
            }
        }
        IosSection(header = stringResource(Res.string.pangea_legend_precision_section)) {
            LegendRow(Res.string.pangea_legend_precision, Res.string.pangea_legend_precision_text) {
                Box(
                    Modifier.size(36.dp)
                        .clip(CircleShape)
                        .background(c.systemBlue.copy(alpha = 0.25f))
                        .border(1.dp, Color.White, CircleShape),
                )
                Dot(c.systemBlue, 8.dp, ring = false)
            }
        }
        IosSection(header = stringResource(Res.string.pangea_legend_routes_section)) {
            LegendRow(Res.string.pangea_legend_route_start, null) { Dot(c.systemGreen, 15.dp) }
            LegendRow(Res.string.pangea_legend_route_end, null) { Dot(Color.Black, 15.dp) }
            LegendRow(Res.string.pangea_legend_route_line, Res.string.pangea_legend_route_line_text) {
                RouteLine(c.systemBlue)
            }
        }
    }
}

/** One legend entry: its [symbol] in a 40 dp box, the name, and the explanation under it when there is one. */
@Composable
private fun LegendRow(title: StringResource, subtitle: StringResource?, symbol: @Composable () -> Unit) {
    val name = stringResource(title)
    val text = subtitle?.let { stringResource(it) }
    Row(
        Modifier.iosRowSeparator(start = TEXT_START)
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) { contentDescription = listOfNotNull(name, text).joinToString(". ") },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) { symbol() }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).clearAndSetSemantics {}, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(name, style = Ios.type.subheadline.copy(fontWeight = FontWeight.SemiBold), color = Ios.colors.label)
            if (text != null) Text(text, style = Ios.type.caption1, color = Ios.colors.secondaryLabel)
        }
    }
}

/** A node's circle as the map draws it, with a short word inside. */
@Composable
private fun BadgeCircle(text: String, color: Color) {
    Box(Modifier.size(28.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Text(text, style = Ios.type.caption2.copy(fontWeight = FontWeight.SemiBold), color = Color.White, maxLines = 1)
    }
}

/** A filled dot, ringed in white like a route's end marks. */
@Composable
private fun Dot(color: Color, size: Dp, ring: Boolean = true) {
    Box(
        Modifier.size(size)
            .clip(CircleShape)
            .background(color)
            .then(if (ring) Modifier.border(2.dp, Color.White, CircleShape) else Modifier),
    )
}

/** A sample of a route's line: 3 pt wide with round ends. */
@Composable
private fun RouteLine(color: Color) {
    Canvas(Modifier.size(40.dp)) {
        val y = size.height / 2
        val inset = 4.dp.toPx()
        drawLine(
            color = color,
            start = Offset(inset, y),
            end = Offset(size.width - inset, y),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

/** Where a legend row's words start, which is where its separator starts too: 16 + 40 + 12. */
private val TEXT_START = 68.dp
