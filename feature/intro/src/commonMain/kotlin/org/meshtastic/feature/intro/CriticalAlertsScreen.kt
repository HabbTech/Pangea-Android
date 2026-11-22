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
import org.meshtastic.core.resources.configure_critical_alerts
import org.meshtastic.core.resources.critical_alerts
import org.meshtastic.core.resources.critical_alerts_dnd_request_text
import org.meshtastic.core.resources.pangea_critical_alerts_desc
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.theme.AppTheme

/**
 * Critical Alerts: Android's extra step after notifications are allowed, which opens the alert channel's settings so
 * critical messages can sound through Do Not Disturb (iOS asks for this with the notification permission itself). Laid
 * out like the other setup pages, with one button.
 *
 * @param onConfigure Opens the settings and moves on.
 */
@Composable
internal fun CriticalAlertsScreen(onConfigure: () -> Unit) {
    IosSetupPage(
        title = stringResource(Res.string.critical_alerts),
        description = stringResource(Res.string.critical_alerts_dnd_request_text),
        largeTitle = true,
        primaryLabel = stringResource(Res.string.configure_critical_alerts),
        onPrimary = onConfigure,
    ) {
        FeatureRow(
            FeatureUIData(
                icon = IosIcons.WarningFill,
                subtitleRes = Res.string.pangea_critical_alerts_desc,
                tint = Ios.colors.systemYellow,
            ),
        )
    }
}

@PreviewLightDark
@Composable
private fun CriticalAlertsScreenPreview() {
    AppTheme { Surface { CriticalAlertsScreen(onConfigure = {}) } }
}
