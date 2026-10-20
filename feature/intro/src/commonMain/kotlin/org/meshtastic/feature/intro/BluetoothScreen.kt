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
import org.meshtastic.core.resources.bluetooth_permission_blocked_notice
import org.meshtastic.core.resources.bluetooth_permission_denied_notice
import org.meshtastic.core.resources.pangea_bluetooth_desc
import org.meshtastic.core.resources.pangea_bluetooth_title
import org.meshtastic.core.resources.pangea_bt_background_desc
import org.meshtastic.core.resources.pangea_bt_background_title
import org.meshtastic.core.resources.pangea_bt_nodes_desc
import org.meshtastic.core.resources.pangea_bt_nodes_title
import org.meshtastic.core.resources.pangea_configure_bluetooth
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.core.ui.util.PermissionStatus

/**
 * Bluetooth Connectivity, as on the iPhone app, with one button that asks for Android's "Nearby devices" permissions.
 * Skipped below Android 12, where Bluetooth scanning rides on the location permission the page before asked for.
 */
@Composable
internal fun BluetoothScreen(status: PermissionStatus, onPrimaryAction: () -> Unit, onOpenSettings: () -> Unit) {
    PermissionScreenLayout(
        headlineRes = Res.string.pangea_bluetooth_title,
        descriptionRes = Res.string.pangea_bluetooth_desc,
        features =
        listOf(
            FeatureUIData(
                icon = IosIcons.BluetoothRune,
                titleRes = Res.string.pangea_bt_nodes_title,
                subtitleRes = Res.string.pangea_bt_nodes_desc,
                capsule = true,
            ),
            FeatureUIData(
                icon = IosIcons.PersonDotted,
                titleRes = Res.string.pangea_bt_background_title,
                subtitleRes = Res.string.pangea_bt_background_desc,
            ),
        ),
        status = status,
        deniedNoticeRes = Res.string.bluetooth_permission_denied_notice,
        blockedNoticeRes = Res.string.bluetooth_permission_blocked_notice,
        configureButtonTextRes = Res.string.pangea_configure_bluetooth,
        onPrimaryAction = onPrimaryAction,
        onOpenSettings = onOpenSettings,
    )
}

@PreviewLightDark
@Composable
private fun BluetoothScreenPreview() {
    AppTheme {
        Surface { BluetoothScreen(status = PermissionStatus.NOT_REQUESTED, onPrimaryAction = {}, onOpenSettings = {}) }
    }
}

@PreviewLightDark
@Composable
private fun BluetoothScreenBlockedPreview() {
    AppTheme {
        Surface {
            BluetoothScreen(status = PermissionStatus.PERMANENTLY_DENIED, onPrimaryAction = {}, onOpenSettings = {})
        }
    }
}
