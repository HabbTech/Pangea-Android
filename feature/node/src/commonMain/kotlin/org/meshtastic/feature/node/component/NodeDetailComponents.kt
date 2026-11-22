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
package org.meshtastic.feature.node.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.a11y_label_value
import org.meshtastic.core.resources.copy
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosCardShape
import org.meshtastic.core.ui.ios.LocalIosSectionRows
import org.meshtastic.core.ui.util.createClipEntry

/** A section of a node's details, as on the Pangea iPhone app: a grey header over a white rounded card of rows. */
@Composable
internal fun SectionCard(
    title: StringResource,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val card = Ios.colors.groupedRow
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(title),
            style = Ios.type.headline,
            color = Ios.colors.secondaryLabel,
            modifier =
            Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp).semantics {
                heading()
            }, // Proper navigation for screen reader users
        )
        CompositionLocalProvider(LocalIosSectionRows provides true) {
            Column(
                modifier =
                Modifier.fillMaxWidth().clip(IosCardShape).background(card).drawWithContent {
                    drawContent()
                    // Hides the first row's hairline, which iOS doesn't draw.
                    drawRect(card, size = Size(size.width, 1.dp.toPx()))
                },
                content = content,
            )
        }
    }
}

@Composable
internal fun InfoItem(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle = Ios.type.body,
    valueColor: Color = Ios.colors.secondaryLabel,
    iconTint: Color = Ios.colors.tint,
    iconSize: Dp = 22.dp,
    onClick: (() -> Unit)? = null,
) = InfoItem(
    label = label,
    value = value,
    modifier = modifier,
    valueStyle = valueStyle,
    valueColor = valueColor,
    onClick = onClick,
) {
    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(iconSize), tint = iconTint)
}

/** [InfoItem] with the leading glyph as a slot, for a composite icon that is not a single [ImageVector]. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun InfoItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueStyle: TextStyle = Ios.type.body,
    valueColor: Color = Ios.colors.secondaryLabel,
    onClick: (() -> Unit)? = null,
    icon: @Composable () -> Unit,
) {
    val clipboard: Clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val copyLabel = stringResource(Res.string.copy)
    val contentDescriptionText = stringResource(Res.string.a11y_label_value, label, value)

    // iOS's detail row: icon, label, and the value at the trailing edge (wrapping there when long).
    val separator = Ios.colors.separator
    val drawSeparator = LocalIosSectionRows.current
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp) // Minimum touch target height
            .combinedClickable(
                onLongClick = { coroutineScope.launch { clipboard.setClipEntry(createClipEntry(value, label)) } },
                onLongClickLabel = copyLabel, // Clear intent for accessibility
                onClick = onClick ?: {},
                role = Role.Button,
            )
            .drawBehind {
                if (drawSeparator) {
                    val start = ROW_TEXT_START.toPx()
                    drawRect(separator, Offset(start, 0f), Size(size.width - start - 16.dp.toPx(), 0.5.dp.toPx()))
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {
                // Screen readers read as a unified data unit
                contentDescription = contentDescriptionText
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(ROW_TEXT_START - 16.dp), contentAlignment = Alignment.CenterStart) { icon() }
        Text(text = label, style = Ios.type.body, color = Ios.colors.label)
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = valueStyle,
            color = valueColor,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Where a detail row's label starts: 16 margin, the icon, and iOS's gap. */
private val ROW_TEXT_START = 52.dp

/** Between rows outside a [SectionCard]. Inside one, the rows draw iOS's hairlines themselves. */
@Composable
internal fun SectionDivider(modifier: Modifier = Modifier) {
    if (LocalIosSectionRows.current) return
    HorizontalDivider(modifier = modifier.padding(horizontal = 20.dp), color = Ios.colors.separator.copy(alpha = 0.5f))
}
