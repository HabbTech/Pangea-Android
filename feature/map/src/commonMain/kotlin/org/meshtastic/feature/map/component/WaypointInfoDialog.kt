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
package org.meshtastic.feature.map.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.common.util.MeasurementSystem
import org.meshtastic.core.common.util.NumberFormatter
import org.meshtastic.core.model.util.PUSHPIN_CODE_POINT
import org.meshtastic.core.model.util.toCodePointString
import org.meshtastic.core.model.util.toDistanceString
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.delete
import org.meshtastic.core.resources.edit
import org.meshtastic.core.resources.expires
import org.meshtastic.core.resources.geofence
import org.meshtastic.core.resources.geofence_alerts_opt_in
import org.meshtastic.core.resources.geofence_alerts_opt_in_desc
import org.meshtastic.core.resources.geofence_radius
import org.meshtastic.core.resources.pangea_open_in_maps
import org.meshtastic.core.resources.pangea_waypoint_area
import org.meshtastic.core.resources.pangea_waypoint_location
import org.meshtastic.core.resources.pangea_waypoint_timestamps
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosAccessory
import org.meshtastic.core.ui.ios.IosGlassIconButton
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosInset
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.ios.IosSheet
import org.meshtastic.core.ui.util.rememberOpenMap
import org.meshtastic.proto.Waypoint

/**
 * Read-only view of a geofence waypoint the local device did NOT create. Editing/re-broadcasting someone else's
 * geofence is not offered here; the one interactive control is the receiver-local opt-in that decides whether THIS
 * device raises crossing notifications for it (foreign geofences are silent by default — see
 * [org.meshtastic.core.data.manager .GeofenceMonitor]). Reached for both locked and unlocked foreign geofences, so the
 * locked case gets a view at all. Unlocked foreign geofences are still editable — [onEdit], when non-null, opens the
 * full editor. [onDeleteForMe] opens the delete alert; dropping our local copy needs no mesh-wide permission, so it is
 * offered even when the geofence is locked to its creator.
 *
 * Drawn as the iPhone app's waypoint sheet: the name as the title with edit and trash at the top right, then the emoji
 * and name, the description, the location, the geofence, the local notification opt-in and the expiry.
 */
@Composable
fun WaypointInfoDialog(
    waypoint: Waypoint,
    displayUnits: MeasurementSystem,
    alertsEnabled: Boolean,
    onToggleAlerts: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDeleteForMe: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    // waypoint.icon is untrusted input; toCodePointString substitutes a fallback rather than throwing.
    val emoji = if (waypoint.icon == 0) PUSHPIN_CODE_POINT.toCodePointString() else waypoint.icon.toCodePointString()
    val title = waypoint.name.takeIf { it.isNotBlank() } ?: stringResource(Res.string.geofence)

    IosSheet(
        title = title,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        grouped = true,
        grabber = true,
        header = { WaypointInfoBar(title = title, onEdit = onEdit, onDeleteForMe = onDeleteForMe) },
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column {
                WaypointTitle(emoji = emoji, name = title)
                if (waypoint.description.isNotBlank()) {
                    IosSection { IosRow(title = waypoint.description, icon = IosIcons.DocText) }
                }
            }
            WaypointLocationSection(waypoint = waypoint, name = title)
            if (waypoint.geofence_radius > 0 || waypoint.bounding_box != null) {
                IosSection(header = stringResource(Res.string.geofence)) {
                    if (waypoint.geofence_radius > 0) {
                        IosRow(
                            title = stringResource(Res.string.geofence_radius),
                            value = waypoint.geofence_radius.toDistanceString(displayUnits),
                        )
                    }
                    if (waypoint.bounding_box != null) {
                        IosRow(title = stringResource(Res.string.pangea_waypoint_area))
                    }
                }
            }
            // The one interactive control: whether this device alerts on crossings of a geofence it didn't create.
            IosSection(footer = stringResource(Res.string.geofence_alerts_opt_in_desc)) {
                IosRow(
                    title = stringResource(Res.string.geofence_alerts_opt_in),
                    icon = IosIcons.Bell,
                    accessory = IosAccessory.Switch(checked = alertsEnabled, onCheckedChange = onToggleAlerts),
                )
            }
            if (waypoint.expire.isExpirySet()) {
                IosSection(header = stringResource(Res.string.pangea_waypoint_timestamps)) {
                    IosRow(
                        title = stringResource(Res.string.expires),
                        icon = IosIcons.Clock,
                        value = DateFormatter.formatDateTimeShort(waypoint.expire * MILLIS_PER_SECOND),
                    )
                }
            }
        }
    }
}

/** The sheet's bar: the waypoint's name in the middle; edit and trash, when offered, at the right. */
@Composable
private fun WaypointInfoBar(title: String, onEdit: (() -> Unit)?, onDeleteForMe: (() -> Unit)?) {
    Box(Modifier.fillMaxWidth().height(74.dp).padding(horizontal = IosInset)) {
        Text(title, style = Ios.type.headline, color = Ios.colors.label, modifier = Modifier.align(Alignment.Center))
        Row(Modifier.align(Alignment.CenterEnd), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (onEdit != null) {
                IosGlassIconButton(
                    IosIcons.Compose,
                    contentDescription = stringResource(Res.string.edit),
                    onClick = onEdit,
                    tint = Ios.colors.tint,
                )
            }
            // iOS's trash opens a menu of the ways to delete; here it opens the delete alert, which lists them.
            if (onDeleteForMe != null) {
                IosGlassIconButton(
                    IosIcons.Trash,
                    contentDescription = stringResource(Res.string.delete),
                    onClick = onDeleteForMe,
                    tint = Ios.colors.tint,
                )
            }
        }
    }
}

/** The emoji in an orange circle beside the name in bold, above the first card, as iOS heads the sheet. */
@Composable
private fun WaypointTitle(emoji: String, name: String) {
    Row(
        Modifier.fillMaxWidth().padding(start = IosInset + 4.dp, end = IosInset, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(Ios.colors.systemOrange),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 18.sp)
        }
        Text(name, style = Ios.type.headline, color = Ios.colors.label)
    }
}

/** Where the waypoint is: its coordinates, then iOS's Open in Maps, which hands them to the phone's map app. */
@Composable
private fun WaypointLocationSection(waypoint: Waypoint, name: String) {
    val openMap = rememberOpenMap()
    IosSection(header = stringResource(Res.string.pangea_waypoint_location)) {
        IosRow(
            title =
            NumberFormatter.format((waypoint.latitude_i ?: 0) * COORDINATE_SCALE, COORDINATE_DECIMALS) +
                ", " +
                NumberFormatter.format((waypoint.longitude_i ?: 0) * COORDINATE_SCALE, COORDINATE_DECIMALS),
            titleColor = Ios.colors.secondaryLabel,
            icon = IosIcons.MapPin,
        )
        // iOS's Open in Maps: the phone's own map app, at the waypoint with its name.
        IosRow(
            title = stringResource(Res.string.pangea_open_in_maps),
            titleColor = Ios.colors.tint,
            icon = IosIcons.MapPinEllipse,
            onClick = {
                openMap(
                    (waypoint.latitude_i ?: 0) * COORDINATE_SCALE,
                    (waypoint.longitude_i ?: 0) * COORDINATE_SCALE,
                    name,
                )
            },
        )
    }
}

/** Waypoint coordinates are stored as integers in units of 1e-7 degrees. */
private const val COORDINATE_SCALE = 1e-7

/** iOS shows six decimals, about 10 cm. */
private const val COORDINATE_DECIMALS = 6

private const val MILLIS_PER_SECOND = 1000L
