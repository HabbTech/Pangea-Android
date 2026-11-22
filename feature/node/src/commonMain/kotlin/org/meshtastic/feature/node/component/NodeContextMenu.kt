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
package org.meshtastic.feature.node.component

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.Node
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.add_favorite
import org.meshtastic.core.resources.ignore
import org.meshtastic.core.resources.message
import org.meshtastic.core.resources.mute_notifications
import org.meshtastic.core.resources.remove
import org.meshtastic.core.resources.remove_favorite
import org.meshtastic.core.resources.remove_ignored
import org.meshtastic.core.resources.trace_route
import org.meshtastic.core.resources.unmute
import org.meshtastic.core.resources.update_status
import org.meshtastic.core.ui.ios.IosContextMenuOffset
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosMenu
import org.meshtastic.core.ui.ios.IosMenuDivider
import org.meshtastic.core.ui.ios.IosMenuItem

/**
 * Shared context menu for node actions (favorite, mute, message, trace route, ignore, remove).
 *
 * Used by both Android and Desktop adaptive node list screens.
 */
@Composable
fun NodeContextMenu(
    expanded: Boolean,
    node: Node,
    onFavorite: () -> Unit,
    onMute: () -> Unit,
    onMessage: () -> Unit,
    onTraceRoute: () -> Unit,
    onIgnore: () -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    IosMenu(expanded = expanded, onDismissRequest = onDismiss, offset = IosContextMenuOffset) {
        FavoriteMenuItem(node, onFavorite, onDismiss)
        if (node.capabilities.canMuteNode) {
            MuteMenuItem(node, onMute, onDismiss)
        }
        MessageMenuItem(node, onMessage, onDismiss)
        TraceRouteMenuItem(node, onTraceRoute, onDismiss)
        IosMenuDivider()
        IgnoreMenuItem(node, onIgnore, onDismiss)
        RemoveMenuItem(node, onRemove, onDismiss)
    }
}

/** A menu of its own: favoriting, muting, messaging or removing yourself is meaningless (design#115). */
@Composable
fun LocalNodeContextMenu(expanded: Boolean, onUpdateStatus: () -> Unit, onDismiss: () -> Unit) {
    IosMenu(expanded = expanded, onDismissRequest = onDismiss, offset = IosContextMenuOffset) {
        IosMenuItem(
            onClick = {
                onUpdateStatus()
                onDismiss()
            },
            icon = IosIcons.TextAlignLeft,
            text = stringResource(Res.string.update_status),
        )
    }
}

@Composable
private fun FavoriteMenuItem(node: Node, onFavorite: () -> Unit, onDismiss: () -> Unit) {
    val isFavorite = node.isFavorite
    IosMenuItem(
        onClick = {
            onFavorite()
            onDismiss()
        },
        enabled = !node.isIgnored,
        icon = if (isFavorite) IosIcons.StarFill else IosIcons.Star,
        text = stringResource(if (isFavorite) Res.string.remove_favorite else Res.string.add_favorite),
    )
}

@Composable
private fun IgnoreMenuItem(node: Node, onIgnore: () -> Unit, onDismiss: () -> Unit) {
    val isIgnored = node.isIgnored
    IosMenuItem(
        onClick = {
            onIgnore()
            onDismiss()
        },
        icon = IosIcons.MinusCircle,
        destructive = true,
        text = stringResource(if (isIgnored) Res.string.remove_ignored else Res.string.ignore),
    )
}

@Composable
private fun MuteMenuItem(node: Node, onMute: () -> Unit, onDismiss: () -> Unit) {
    val isMuted = node.isMuted
    IosMenuItem(
        onClick = {
            onMute()
            onDismiss()
        },
        icon = if (isMuted) IosIcons.Bell else IosIcons.BellSlash,
        text = stringResource(if (isMuted) Res.string.unmute else Res.string.mute_notifications),
    )
}

@Composable
private fun MessageMenuItem(node: Node, onMessage: () -> Unit, onDismiss: () -> Unit) {
    IosMenuItem(
        onClick = {
            onMessage()
            onDismiss()
        },
        enabled = !node.isIgnored,
        icon = IosIcons.Bubble,
        text = stringResource(Res.string.message),
    )
}

@Composable
private fun TraceRouteMenuItem(node: Node, onTraceRoute: () -> Unit, onDismiss: () -> Unit) {
    IosMenuItem(
        onClick = {
            onTraceRoute()
            onDismiss()
        },
        enabled = !node.isIgnored,
        icon = IosIcons.PointsConnected,
        text = stringResource(Res.string.trace_route),
    )
}

@Composable
private fun RemoveMenuItem(node: Node, onRemove: () -> Unit, onDismiss: () -> Unit) {
    IosMenuItem(
        onClick = {
            onRemove()
            onDismiss()
        },
        enabled = !node.isIgnored,
        icon = IosIcons.Trash,
        destructive = true,
        text = stringResource(Res.string.remove),
    )
}
