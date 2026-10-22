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
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.app_notifications
import org.meshtastic.core.resources.configure_notification_permissions
import org.meshtastic.core.resources.critical_alerts
import org.meshtastic.core.resources.incoming_messages
import org.meshtastic.core.resources.low_battery
import org.meshtastic.core.resources.notification_permission_blocked_notice
import org.meshtastic.core.resources.notification_permission_denied_notice
import org.meshtastic.core.resources.notifications_for_low_battery_alerts
import org.meshtastic.core.resources.pangea_critical_alerts_desc
import org.meshtastic.core.resources.pangea_notify_messages_desc
import org.meshtastic.core.resources.pangea_send_notifications
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.core.ui.util.PermissionStatus

/**
 * App Notifications, as on the iPhone app: "Send Notifications" (incoming messages, low battery) and "Critical Alerts",
 * with one button that asks for the permission. (iOS's Siri & CarPlay row has no Android counterpart.)
 */
@Composable
internal fun NotificationsScreen(status: PermissionStatus, onPrimaryAction: () -> Unit, onOpenSettings: () -> Unit) {
    val c = Ios.colors
    PermissionScreenLayout(
        headlineRes = Res.string.app_notifications,
        descriptionRes = null,
        features =
        listOf(
            FeatureUIData(
                icon = IosIcons.MessageOutline,
                titleRes = Res.string.incoming_messages,
                subtitleRes = Res.string.pangea_notify_messages_desc,
                tint = c.systemGreen,
            ),
            FeatureUIData(
                icon = IosIcons.Battery25,
                titleRes = Res.string.low_battery,
                subtitleRes = Res.string.notifications_for_low_battery_alerts,
            ),
        ),
        status = status,
        deniedNoticeRes = Res.string.notification_permission_denied_notice,
        blockedNoticeRes = Res.string.notification_permission_blocked_notice,
        configureButtonTextRes = Res.string.configure_notification_permissions,
        onPrimaryAction = onPrimaryAction,
        onOpenSettings = onOpenSettings,
        header = { SetupSectionTitle(stringResource(Res.string.pangea_send_notifications)) },
        additionalContent = {
            SetupSectionTitle(stringResource(Res.string.critical_alerts))
            FeatureRow(
                FeatureUIData(
                    icon = IosIcons.WarningFill,
                    subtitleRes = Res.string.pangea_critical_alerts_desc,
                    tint = c.systemYellow,
                ),
            )
        },
    )
}

@PreviewLightDark
@Composable
private fun NotificationsScreenPreview() {
    AppTheme {
        Surface {
            NotificationsScreen(status = PermissionStatus.NOT_REQUESTED, onPrimaryAction = {}, onOpenSettings = {})
        }
    }
}
