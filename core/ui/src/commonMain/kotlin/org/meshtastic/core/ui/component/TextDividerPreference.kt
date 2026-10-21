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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosCard
import org.meshtastic.core.ui.theme.AppTheme

@Composable
fun TextDividerPreference(
    title: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingIcon: ImageVector? = null,
) {
    TextDividerPreference(
        title = AnnotatedString(text = title),
        enabled = enabled,
        modifier = modifier,
        trailingIcon = trailingIcon,
    )
}

@Composable
fun TextDividerPreference(
    title: AnnotatedString,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingIcon: ImageVector? = null,
) {
    IosCard(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(all = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = Ios.type.body,
                color =
                if (!enabled) {
                    Ios.colors.label.copy(alpha = 0.38f)
                } else {
                    Color.Unspecified
                },
            )
            if (trailingIcon != null) {
                Icon(trailingIcon, "trailingIcon", modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.End))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TextDividerPreferencePreview() {
    AppTheme { TextDividerPreference(title = "Advanced settings") }
}
