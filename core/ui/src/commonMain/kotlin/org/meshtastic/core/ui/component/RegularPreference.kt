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
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package org.meshtastic.core.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.iosPressable
import org.meshtastic.core.ui.theme.AppTheme

@Composable
fun RegularPreference(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    summary: String? = null,
    trailingIcon: ImageVector? = null,
    dropdownMenu: @Composable () -> Unit = {},
) {
    RegularPreference(
        title = title,
        subtitle = AnnotatedString(text = subtitle),
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        summary = summary,
        trailingIcon = trailingIcon,
        dropdownMenu = dropdownMenu,
    )
}

@Composable
fun RegularPreference(
    title: String,
    subtitle: AnnotatedString,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    summary: String? = null,
    trailingIcon: ImageVector? = null,
    dropdownMenu: @Composable () -> Unit = {},
) {
    val c = Ios.colors
    PreferenceRow(
        title = title,
        summary = summary,
        enabled = enabled,
        modifier = modifier.iosPressable(enabled = enabled, onClick = onClick),
        trailing = {
            Text(text = subtitle, style = Ios.type.body, color = c.secondaryLabel)
            if (trailingIcon != null) {
                Box {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = c.tertiaryLabel,
                        modifier = Modifier.padding(start = 6.dp).size(20.dp),
                    )
                    dropdownMenu()
                }
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
fun RegularPreferencePreview() {
    AppTheme { RegularPreference(title = "Advanced settings", subtitle = "Text2", onClick = {}) }
}
