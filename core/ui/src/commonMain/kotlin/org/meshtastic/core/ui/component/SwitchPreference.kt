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

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosActivityIndicator
import org.meshtastic.core.ui.ios.IosSwitch
import org.meshtastic.core.ui.theme.AppTheme

/**
 * A setting that's on or off, as on the Pangea iPhone app: the title with an iOS switch beside it and the explanation
 * under both. The whole row toggles.
 */
@Composable
fun SwitchPreference(
    modifier: Modifier = Modifier,
    title: String,
    summary: String = "",
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    padding: PaddingValues? = null,
    containerColor: Color? = null,
    loading: Boolean = false,
) {
    PreferenceRow(
        title = title,
        summary = summary,
        enabled = enabled,
        modifier =
        modifier
            .then(padding?.let { Modifier.padding(it) } ?: Modifier)
            .then(containerColor?.let { Modifier.background(it) } ?: Modifier)
            .toggleable(
                value = checked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled && !loading,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
        trailing = {
            if (loading) {
                IosActivityIndicator(color = Ios.colors.secondaryLabel)
            } else {
                IosSwitch(checked = checked, onCheckedChange = null, enabled = enabled)
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
fun SwitchPreferencePreview() {
    AppTheme {
        SwitchPreference(
            title = "Setting",
            summary = "What it does",
            checked = true,
            enabled = true,
            onCheckedChange = {},
        )
    }
}
