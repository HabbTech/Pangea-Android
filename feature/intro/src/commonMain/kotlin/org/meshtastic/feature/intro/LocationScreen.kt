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
package org.meshtastic.feature.intro

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.configure_location_permissions
import org.meshtastic.core.resources.distance_filters
import org.meshtastic.core.resources.distance_filters_description
import org.meshtastic.core.resources.distance_measurements
import org.meshtastic.core.resources.distance_measurements_description
import org.meshtastic.core.resources.location_permission_blocked_notice
import org.meshtastic.core.resources.location_permission_denied_notice
import org.meshtastic.core.resources.mesh_map_location
import org.meshtastic.core.resources.mesh_map_location_description
import org.meshtastic.core.resources.pangea_continuous_location_desc
import org.meshtastic.core.resources.pangea_continuous_location_title
import org.meshtastic.core.resources.phone_location
import org.meshtastic.core.resources.phone_location_description
import org.meshtastic.core.resources.share_location
import org.meshtastic.core.resources.share_location_description
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.core.ui.util.PermissionStatus

/**
 * Phone Location, as on the iPhone app: what the phone's location is used for, with one button that asks for it. iOS
 * also has two switches here (share location, background activity); Android keeps location sharing per radio, set once
 * one is paired, and shares in the background from its service, so they are left out.
 */
@Composable
internal fun LocationScreen(status: PermissionStatus, onPrimaryAction: () -> Unit, onOpenSettings: () -> Unit) {
    val c = Ios.colors
    PermissionScreenLayout(
        headlineRes = Res.string.phone_location,
        descriptionRes = Res.string.phone_location_description,
        features =
        listOf(
            FeatureUIData(
                icon = IosIcons.Location,
                titleRes = Res.string.share_location,
                subtitleRes = Res.string.share_location_description,
                tint = c.systemBlue,
            ),
            FeatureUIData(
                icon = IosIcons.LocationFill,
                titleRes = Res.string.pangea_continuous_location_title,
                subtitleRes = Res.string.pangea_continuous_location_desc,
                tint = c.systemBlue,
            ),
            FeatureUIData(
                icon = IosIcons.Measurement,
                titleRes = Res.string.distance_measurements,
                subtitleRes = Res.string.distance_measurements_description,
            ),
            FeatureUIData(
                icon = IosIcons.FilterCircle,
                titleRes = Res.string.distance_filters,
                subtitleRes = Res.string.distance_filters_description,
            ),
            FeatureUIData(
                icon = IosIcons.MapPin,
                titleRes = Res.string.mesh_map_location,
                subtitleRes = Res.string.mesh_map_location_description,
                tint = c.systemRed,
            ),
        ),
        status = status,
        deniedNoticeRes = Res.string.location_permission_denied_notice,
        blockedNoticeRes = Res.string.location_permission_blocked_notice,
        configureButtonTextRes = Res.string.configure_location_permissions,
        onPrimaryAction = onPrimaryAction,
        onOpenSettings = onOpenSettings,
    )
}

@PreviewLightDark
@Composable
private fun LocationScreenPreview() {
    AppTheme {
        Surface { LocationScreen(status = PermissionStatus.NOT_REQUESTED, onPrimaryAction = {}, onOpenSettings = {}) }
    }
}

@PreviewLightDark
@Composable
private fun LocationScreenDeniedPreview() {
    AppTheme {
        Surface {
            LocationScreen(status = PermissionStatus.DENIED_CAN_RETRY, onPrimaryAction = {}, onOpenSettings = {})
        }
    }
}
