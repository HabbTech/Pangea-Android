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
package org.meshtastic.feature.node.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.MeasurementSystem
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.TelemetryType
import org.meshtastic.core.navigation.MapRoute
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.actions
import org.meshtastic.core.resources.add_favorite
import org.meshtastic.core.resources.air_quality_metrics_log
import org.meshtastic.core.resources.device_metrics_log
import org.meshtastic.core.resources.env_metrics_log
import org.meshtastic.core.resources.host_metrics_log
import org.meshtastic.core.resources.ignore
import org.meshtastic.core.resources.logs
import org.meshtastic.core.resources.message
import org.meshtastic.core.resources.mute_notifications
import org.meshtastic.core.resources.neighbor_info
import org.meshtastic.core.resources.open_compass
import org.meshtastic.core.resources.pangea_delete_node
import org.meshtastic.core.resources.pangea_device_metrics_log
import org.meshtastic.core.resources.pangea_environment_metrics_log
import org.meshtastic.core.resources.pangea_estimate_coverage
import org.meshtastic.core.resources.pangea_exchange_positions
import org.meshtastic.core.resources.pangea_exchange_user_info
import org.meshtastic.core.resources.pangea_host_metrics_log
import org.meshtastic.core.resources.pangea_local_stats_log
import org.meshtastic.core.resources.pangea_neighbor_info_log
import org.meshtastic.core.resources.pangea_pax_log
import org.meshtastic.core.resources.pangea_position_log
import org.meshtastic.core.resources.pangea_power_metrics_log
import org.meshtastic.core.resources.pangea_remove_from_ignored
import org.meshtastic.core.resources.pangea_request_local_stats
import org.meshtastic.core.resources.pangea_request_telemetry
import org.meshtastic.core.resources.pangea_trace_route
import org.meshtastic.core.resources.pangea_trace_route_log
import org.meshtastic.core.resources.pax_metrics_log
import org.meshtastic.core.resources.power_metrics_log
import org.meshtastic.core.resources.remove_favorite
import org.meshtastic.core.resources.share_contact
import org.meshtastic.core.resources.unmute
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Refresh
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosAccessory
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.util.LocalSitePlannerAvailable
import org.meshtastic.feature.node.component.COOL_DOWN_TIME_MS
import org.meshtastic.feature.node.component.NodeMenuAction
import org.meshtastic.feature.node.component.REQUEST_NEIGHBORS_COOL_DOWN_TIME_MS
import org.meshtastic.feature.node.model.LogsType
import org.meshtastic.feature.node.model.NodeDetailAction
import org.meshtastic.feature.node.model.showsDirectMessageAction

/** One log the node may have: its iOS name and symbol, and the log screen it opens. */
private class LogLink(val title: StringResource, val icon: ImageVector, val type: LogsType)

private val iosLogs by lazy {
    listOf(
        LogLink(Res.string.pangea_device_metrics_log, IosIcons.FlipPhone, LogsType.DEVICE),
        LogLink(Res.string.pangea_position_log, IosIcons.MapPinEllipse, LogsType.POSITIONS),
        LogLink(Res.string.pangea_environment_metrics_log, IosIcons.CloudSunRain, LogsType.ENVIRONMENT),
        LogLink(Res.string.air_quality_metrics_log, IosIcons.AqiMedium, LogsType.AIR_QUALITY),
        LogLink(Res.string.pangea_trace_route_log, IosIcons.Signpost, LogsType.TRACEROUTE),
        LogLink(Res.string.pangea_power_metrics_log, IosIcons.Bolt, LogsType.POWER),
        LogLink(Res.string.pangea_local_stats_log, IosIcons.ChartBar, LogsType.SIGNAL),
    )
}

/** Logs Android keeps that the iPhone app has no screen for; listed after iOS's own, and only once there is data. */
private val androidLogs by lazy {
    listOf(
        LogLink(Res.string.pangea_neighbor_info_log, IosIcons.People, LogsType.NEIGHBOR_INFO),
        LogLink(Res.string.pangea_host_metrics_log, IosIcons.Cpu, LogsType.HOST),
        LogLink(Res.string.pangea_pax_log, IosIcons.FigureWalk, LogsType.PAX),
    )
}

/**
 * The node's logs as the iPhone app lists them: every log iOS has, greyed out until the node has some, with Estimate
 * Coverage after the position log where the site planner is available.
 */
@Composable
internal fun NodeLogsSection(node: Node, availableLogs: Set<LogsType>, onAction: (NodeDetailAction) -> Unit) {
    val c = Ios.colors
    IosSection(header = stringResource(Res.string.logs)) {
        iosLogs.forEach { link ->
            val has = link.type in availableLogs
            IosRow(
                title = stringResource(link.title),
                icon = link.icon,
                iconTint = if (has) c.tint else c.tint.copy(alpha = DISABLED_ICON_ALPHA),
                titleColor = if (has) c.label else c.secondaryLabel,
                accessory = IosAccessory.Chevron,
                onClick =
                if (has) {
                    { onAction(NodeDetailAction.Navigate(link.type.routeFactory(node.num))) }
                } else {
                    null
                },
            )
            if (link.type == LogsType.POSITIONS && has && LocalSitePlannerAvailable.current) {
                IosRow(
                    title = stringResource(Res.string.pangea_estimate_coverage),
                    icon = IosIcons.Antenna,
                    titleColor = c.tint,
                    onClick = { onAction(NodeDetailAction.Navigate(MapRoute.Map(sitePlannerNodeNum = node.num))) },
                )
            }
        }
        androidLogs
            .filter { it.type in availableLogs }
            .forEach { link ->
                IosRow(
                    title = stringResource(link.title),
                    icon = link.icon,
                    accessory = IosAccessory.Chevron,
                    onClick = { onAction(NodeDetailAction.Navigate(link.type.routeFactory(node.num))) },
                )
            }
    }
}

/**
 * The node's actions in the iPhone app's order: mute, share, favourite, message, then the requests, compass, ignore and
 * delete. Our own radio gets only the first three.
 */
@Suppress("LongMethod")
@Composable
internal fun NodeActionsSection(
    node: Node,
    isLocal: Boolean,
    hasConversation: Boolean,
    lastTracerouteTime: Long?,
    displayUnits: MeasurementSystem,
    onAction: (NodeDetailAction) -> Unit,
) {
    val c = Ios.colors
    fun menu(action: NodeMenuAction) = onAction(NodeDetailAction.HandleNodeMenuAction(action))
    IosSection(header = stringResource(Res.string.actions)) {
        if (node.capabilities.canMuteNode) {
            ActionRow(
                if (node.isMuted) Res.string.unmute else Res.string.mute_notifications,
                if (node.isMuted) IosIcons.BellSlash else IosIcons.Bell,
            ) {
                menu(NodeMenuAction.Mute(node))
            }
        }
        ActionRow(Res.string.share_contact, IosIcons.QrCode) { onAction(NodeDetailAction.ShareContact) }
        if (!isLocal) {
            IosRow(
                title = stringResource(if (node.isFavorite) Res.string.remove_favorite else Res.string.add_favorite),
                icon = if (node.isFavorite) IosIcons.StarFill else IosIcons.Star,
                iconTint = if (node.isFavorite) c.systemYellow else c.tint,
                titleColor = c.tint,
                onClick = { menu(NodeMenuAction.Favorite(node)) },
            )
            if (node.showsDirectMessageAction(hasConversation)) {
                ActionRow(Res.string.message, IosIcons.MessageOutline) { menu(NodeMenuAction.DirectMessage(node)) }
            }
            ActionRow(Res.string.pangea_exchange_positions, IosIcons.ArrowsCirclepath) {
                menu(NodeMenuAction.RequestPosition(node))
            }
            ActionRow(Res.string.pangea_request_local_stats, IosIcons.ChartBar) {
                menu(NodeMenuAction.RequestTelemetry(node, TelemetryType.LOCAL_STATS))
            }
            ActionRow(Res.string.pangea_exchange_user_info, IosIcons.PeopleGear) {
                menu(NodeMenuAction.RequestUserInfo(node))
            }
            val coolingDown = lastTracerouteTime != null && nowMillis - lastTracerouteTime < COOL_DOWN_TIME_MS
            ActionRow(Res.string.pangea_trace_route, IosIcons.Signpost, enabled = !coolingDown) {
                menu(NodeMenuAction.TraceRoute(node))
            }
            if (node.validPosition != null) {
                ActionRow(Res.string.open_compass, IosIcons.LocationCircle) {
                    onAction(NodeDetailAction.OpenCompass(node, displayUnits))
                }
            }
            ActionRow(
                if (node.isIgnored) Res.string.pangea_remove_from_ignored else Res.string.ignore,
                IosIcons.MinusCircle,
            ) {
                menu(NodeMenuAction.Ignore(node))
            }
            IosRow(
                title = stringResource(Res.string.pangea_delete_node),
                icon = IosIcons.Trash,
                iconTint = c.systemRed,
                titleColor = c.systemRed,
                onClick = { menu(NodeMenuAction.Remove(node)) },
            )
        }
    }
}

/** A tinted action row; greyed and inert while [enabled] is false. */
@Composable
private fun ActionRow(title: StringResource, icon: ImageVector, enabled: Boolean = true, onClick: () -> Unit) {
    val c = Ios.colors
    IosRow(
        title = stringResource(title),
        icon = icon,
        iconTint = if (enabled) c.tint else c.tint.copy(alpha = DISABLED_ICON_ALPHA),
        titleColor = if (enabled) c.tint else c.secondaryLabel,
        onClick = if (enabled) onClick else null,
    )
}

/** One telemetry Android can ask a node for; the iPhone app has no buttons for these. */
private class TelemetryRequest(val title: StringResource, val icon: ImageVector, val action: (Node) -> NodeMenuAction)

private val telemetryRequests by lazy {
    listOf(
        TelemetryRequest(Res.string.device_metrics_log, IosIcons.FlipPhone) {
            NodeMenuAction.RequestTelemetry(it, TelemetryType.DEVICE)
        },
        TelemetryRequest(Res.string.env_metrics_log, IosIcons.CloudSunRain) {
            NodeMenuAction.RequestTelemetry(it, TelemetryType.ENVIRONMENT)
        },
        TelemetryRequest(Res.string.air_quality_metrics_log, IosIcons.AqiMedium) {
            NodeMenuAction.RequestTelemetry(it, TelemetryType.AIR_QUALITY)
        },
        TelemetryRequest(Res.string.power_metrics_log, IosIcons.Bolt) {
            NodeMenuAction.RequestTelemetry(it, TelemetryType.POWER)
        },
        TelemetryRequest(Res.string.host_metrics_log, IosIcons.Cpu) {
            NodeMenuAction.RequestTelemetry(it, TelemetryType.HOST)
        },
        TelemetryRequest(Res.string.pax_metrics_log, IosIcons.FigureWalk) {
            NodeMenuAction.RequestTelemetry(it, TelemetryType.PAX)
        },
    )
}

/**
 * Android's on-demand telemetry requests, kept below iOS's sections so nothing Android offered is lost: a row per
 * metric with a refresh symbol, and neighbour info where the node can answer it.
 */
@Composable
internal fun NodeTelemetryRequestsSection(
    node: Node,
    lastRequestNeighborsTime: Long?,
    onAction: (NodeDetailAction) -> Unit,
) {
    val c = Ios.colors
    val refresh =
        IosAccessory.Custom {
            androidx.compose.material3.Icon(MeshtasticIcons.Refresh, contentDescription = null, tint = c.tint)
        }
    IosSection(header = stringResource(Res.string.pangea_request_telemetry)) {
        telemetryRequests.forEach { request ->
            IosRow(
                title = stringResource(request.title),
                icon = request.icon,
                accessory = refresh,
                onClick = { onAction(NodeDetailAction.HandleNodeMenuAction(request.action(node))) },
            )
        }
        if (node.capabilities.canRequestNeighborInfo) {
            val coolingDown =
                lastRequestNeighborsTime != null &&
                    nowMillis - lastRequestNeighborsTime < REQUEST_NEIGHBORS_COOL_DOWN_TIME_MS
            IosRow(
                title = stringResource(Res.string.neighbor_info),
                icon = IosIcons.People,
                titleColor = if (coolingDown) c.secondaryLabel else c.label,
                accessory = refresh,
                onClick =
                if (coolingDown) {
                    null
                } else {
                    { onAction(NodeDetailAction.HandleNodeMenuAction(NodeMenuAction.RequestNeighborInfo(node))) }
                },
            )
        }
    }
}

private const val DISABLED_ICON_ALPHA = 0.35f
