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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosCardShape
import org.meshtastic.core.ui.theme.AppTheme

/**
 * A section of a settings form, as on the Pangea iPhone app (inset-grouped `Form`): a grey [title] over a white rounded
 * card holding the rows, and an optional grey [footer] under it.
 */
@Composable
fun TitledCard(
    title: String?,
    modifier: Modifier = Modifier,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier) {
        title?.let {
            Text(
                text = it,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp).fillMaxWidth(),
                style = Ios.type.headline,
                color = Ios.colors.secondaryLabel,
            )
        }
        Column(Modifier.fillMaxWidth().clip(IosCardShape).background(Ios.colors.groupedRow), content = content)
        footer?.let {
            Text(
                text = it,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp).fillMaxWidth(),
                style = Ios.type.footnote,
                color = Ios.colors.secondaryLabel,
            )
        }
    }
}

@PreviewLightDark
@Composable
fun TitledCardPreview() {
    AppTheme { Surface { TitledCard(title = "Title") { Box(modifier = Modifier.fillMaxWidth().height(100.dp)) {} } } }
}
