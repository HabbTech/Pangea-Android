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
package org.meshtastic.feature.connections.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.meshtastic.core.ui.ios.IosButton
import org.meshtastic.core.ui.ios.IosButtonStyle

/**
 * Shared "icon + label" button used throughout the Connections screen, so every scan / add / empty-state affordance
 * renders identically: an [IosButton] with the symbol before the words, which name the action for screen readers.
 */
@Composable
fun ConnectionActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    style: ConnectionActionButtonStyle = ConnectionActionButtonStyle.Filled,
    enabled: Boolean = true,
) {
    // iOS's buttons: filled, tinted (tonal and outlined), or plain words.
    IosButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        style =
        when (style) {
            ConnectionActionButtonStyle.Filled -> IosButtonStyle.Filled

            ConnectionActionButtonStyle.Tonal,
            ConnectionActionButtonStyle.Outlined,
            -> IosButtonStyle.Tinted

            ConnectionActionButtonStyle.Text -> IosButtonStyle.Plain
        },
        enabled = enabled,
        icon = icon,
    )
}
