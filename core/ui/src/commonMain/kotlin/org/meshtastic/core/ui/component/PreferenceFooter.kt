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
package org.meshtastic.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.meshtastic.core.ui.ios.IosButton
import org.meshtastic.core.ui.ios.IosButtonStyle

/** Discard and Save under a settings form: iOS capsule buttons, Save filled with the tint. */
@Composable
fun PreferenceFooter(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    positiveEnabled: Boolean = true,
    negativeText: String? = null,
    onNegativeClicked: () -> Unit = {},
    positiveText: String? = null,
    onPositiveClicked: () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (negativeText != null) {
            IosButton(
                text = negativeText,
                onClick = onNegativeClicked,
                style = IosButtonStyle.Tinted,
                enabled = enabled,
                large = true,
                modifier = Modifier.weight(1f),
            )
        }
        if (positiveText != null) {
            IosButton(
                text = positiveText,
                onClick = onPositiveClicked,
                enabled = enabled && positiveEnabled,
                large = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
