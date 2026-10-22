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
package org.meshtastic.feature.wifiprovision.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.wifi_provision_sending_credentials
import org.meshtastic.core.resources.wifi_provision_status_applied
import org.meshtastic.core.resources.wifi_provision_status_failed
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosActivityIndicator
import org.meshtastic.core.ui.ios.IosCardShape
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.feature.wifiprovision.WifiProvisionUiState.ProvisionStatus

/**
 * Inline status card matching the web flasher's colored status feedback, drawn as an iOS card: a soft wash of green or
 * red behind the words when it worked or failed, grey with the spinner while the credentials go.
 */
@Composable
internal fun ProvisionStatusCard(provisionStatus: ProvisionStatus, isProvisioning: Boolean) {
    val colors = statusCardColors(provisionStatus, isProvisioning)
    Row(
        modifier =
        Modifier.fillMaxWidth()
            .clip(IosCardShape)
            .background(colors.first)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatusIcon(provisionStatus = provisionStatus, isProvisioning = isProvisioning, tint = colors.second)
        Text(text = statusText(provisionStatus, isProvisioning), style = Ios.type.headline, color = colors.second)
    }
}

/** Resolve container + content color pair for the provision status card. */
@Composable
private fun statusCardColors(provisionStatus: ProvisionStatus, isProvisioning: Boolean): Pair<Color, Color> {
    val c = Ios.colors
    return when {
        isProvisioning -> c.groupedRow to c.label
        provisionStatus == ProvisionStatus.Success -> c.systemGreen.copy(alpha = WASH) to c.systemGreen
        provisionStatus == ProvisionStatus.Failed -> c.systemRed.copy(alpha = WASH) to c.systemRed
        else -> c.groupedRow to c.label
    }
}

@Composable
private fun StatusIcon(provisionStatus: ProvisionStatus, isProvisioning: Boolean, tint: Color) {
    when {
        isProvisioning -> IosActivityIndicator(size = 20.dp, color = Ios.colors.secondaryLabel)

        provisionStatus == ProvisionStatus.Success ->
            Icon(IosIcons.CheckCircleFill, contentDescription = null, modifier = Modifier.size(22.dp), tint = tint)

        provisionStatus == ProvisionStatus.Failed ->
            Icon(IosIcons.WarningFill, contentDescription = null, modifier = Modifier.size(22.dp), tint = tint)
    }
}

@Composable
private fun statusText(provisionStatus: ProvisionStatus, isProvisioning: Boolean): String = when {
    isProvisioning -> stringResource(Res.string.wifi_provision_sending_credentials)
    provisionStatus == ProvisionStatus.Success -> stringResource(Res.string.wifi_provision_status_applied)
    provisionStatus == ProvisionStatus.Failed -> stringResource(Res.string.wifi_provision_status_failed)
    else -> ""
}

/** iOS's tinted wash behind status text. */
private const val WASH = 0.15f
