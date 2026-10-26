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
package org.meshtastic.feature.settings.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosCardShape
import org.meshtastic.core.ui.ios.LocalIosSectionRows

/**
 * A settings section, as on the Pangea iPhone app: a grey [title] (none when blank) over a white rounded card. Rows in
 * it draw iOS's hairlines between them ([LocalIosSectionRows]); the card covers the one above its first row.
 */
@Composable
fun ExpressiveSection(
    title: String,
    modifier: Modifier = Modifier,
    titleColor: Color = Ios.colors.secondaryLabel,
    content: @Composable ColumnScope.() -> Unit,
) {
    val card = Ios.colors.groupedRow
    Column(modifier = modifier) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp).fillMaxWidth(),
                style = Ios.type.headline,
                color = titleColor,
            )
        }
        CompositionLocalProvider(LocalIosSectionRows provides true) {
            Column(
                modifier =
                Modifier.fillMaxWidth().clip(IosCardShape).background(card).drawWithContent {
                    drawContent()
                    drawRect(card, size = Size(size.width, 1.dp.toPx()))
                },
                content = content,
            )
        }
    }
}
