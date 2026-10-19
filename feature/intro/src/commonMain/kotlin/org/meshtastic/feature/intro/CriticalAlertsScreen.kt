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
import org.meshtastic.core.resources.skip
import org.meshtastic.core.ui.theme.AppTheme

/**
 * Screen for explaining and guiding the user to configure critical alert settings. This screen is part of the app
 * introduction flow and appears after the general notification permission screen.
 *
 * @param onSkip Callback invoked if the user chooses to skip configuring critical alerts.
 * @param onConfigure Callback invoked when the user proceeds to configure critical alerts.
 */
@Composable
internal fun CriticalAlertsScreen(onSkip: () -> Unit, onConfigure: () -> Unit) {
    IosSetupPage(
        title = stringResource(Res.string.critical_alerts),
        description = stringResource(Res.string.critical_alerts_dnd_request_text),
        primaryLabel = stringResource(Res.string.configure_critical_alerts),
        onPrimary = onConfigure,
        skipLabel = stringResource(Res.string.skip),
        onSkip = onSkip,
    )
}

@PreviewLightDark
@Composable
private fun CriticalAlertsScreenPreview() {
    AppTheme { Surface { CriticalAlertsScreen(onSkip = {}, onConfigure = {}) } }
}
