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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.meshtastic.core.ui.icon.Android
import org.meshtastic.core.ui.icon.ChevronRight
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosSwitch
import org.meshtastic.core.ui.ios.LocalIosSectionRows
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.core.ui.util.createClipEntry

/**
 * A list item with an optional [leadingIcon], headline [text], optional [supportingText], and optional [trailingIcon].
 */
@Composable
fun ListItem(
    text: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    textColor: Color = Color.Unspecified,
    supportingTextColor: Color = Color.Unspecified,
    copyable: Boolean = false,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    leadingIconTint: Color = Color.Unspecified,
    trailingIcon: ImageVector? = MeshtasticIcons.ChevronRight,
    trailingIconTint: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
) {
    val clipboard: Clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()

    BasicListItem(
        text = text,
        modifier = modifier,
        textColor = textColor,
        supportingText = supportingText,
        supportingTextColor = supportingTextColor,
        enabled = enabled,
        leadingIcon = leadingIcon,
        leadingIconTint = leadingIconTint,
        trailingContent = trailingIcon.icon(trailingIconTint),
        onClick = onClick,
        onLongClick =
        if (!supportingText.isNullOrBlank() && copyable) {
            { coroutineScope.launch { clipboard.setClipEntry(createClipEntry(supportingText)) } }
        } else {
            null
        },
    )
}

/** A toggleable switch list item. */
@Composable
fun SwitchListItem(
    checked: Boolean,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = Color.Unspecified,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    leadingIconTint: Color = Color.Unspecified,
) {
    BasicListItem(
        text = text,
        modifier = modifier,
        textColor = textColor,
        enabled = enabled,
        leadingIcon = leadingIcon,
        leadingIconTint = leadingIconTint,
        trailingContent = { IosSwitch(checked = checked, onCheckedChange = null, enabled = enabled) },
        onClick = onClick,
    )
}

/**
 * The foundational list item, drawn like a row of the Pangea iPhone app's grouped lists: an orange [leadingIcon]
 * (optional), the headline [text], the [trailingContent] (a grey chevron by default in [ListItem]), and the
 * [supportingText] under the icon and title in a small caption, as iOS places "Expert Mode". Inside a settings section
 * ([LocalIosSectionRows]) it draws the hairline above itself.
 *
 * @see [LinkedCoordinatesItem] for example usage
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BasicListItem(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.Unspecified,
    supportingText: String? = null,
    supportingTextColor: Color = Color.Unspecified,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    leadingIconTint: Color = Color.Unspecified,
    trailingContent: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val c = Ios.colors
    val separator = c.separator
    val drawSeparator = LocalIosSectionRows.current
    val textStart = if (leadingIcon != null) ICON_TEXT_START else 16.dp
    val clickable =
        if (onClick != null) {
            Modifier.combinedClickable(enabled = enabled, onClick = onClick, onLongClick = onLongClick)
        } else {
            Modifier
        }
    Column(
        modifier
            .fillMaxWidth()
            .then(clickable)
            .drawBehind {
                if (drawSeparator) {
                    val start = textStart.toPx()
                    drawRect(separator, Offset(start, 0f), Size(size.width - start - 16.dp.toPx(), 0.5.dp.toPx()))
                }
            }
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (leadingIconTint == Color.Unspecified) c.tint else leadingIconTint,
                    modifier = Modifier.size(26.dp),
                )
                Spacer(Modifier.width(ICON_TEXT_START - 16.dp - 26.dp))
            }
            Text(
                text = text,
                style = Ios.type.body,
                color = if (textColor == Color.Unspecified) c.label else textColor,
                modifier = Modifier.weight(1f),
            )
            trailingContent?.invoke()
        }
        if (!supportingText.isNullOrBlank()) {
            Text(
                text = supportingText,
                style = Ios.type.caption1,
                color = if (supportingTextColor == Color.Unspecified) c.secondaryLabel else supportingTextColor,
            )
        }
    }
}

/** Where a row's text starts when it has an icon: 16 margin, a 26 dp icon, then iOS's gap. */
private val ICON_TEXT_START = 60.dp

/** How a row looks while it can't be used. */
private const val DISABLED_ALPHA = 0.45f

@Composable
fun ImageVector?.icon(tint: Color = Color.Unspecified): @Composable (() -> Unit)? = this?.let {
    {
        val resolvedTint = if (tint == Color.Unspecified) Ios.colors.tertiaryLabel else tint
        Icon(imageVector = it, contentDescription = null, modifier = Modifier.size(20.dp), tint = resolvedTint)
    }
}

@Preview(showBackground = true)
@Composable
fun ListItemPreview() {
    AppTheme { ListItem(text = "Text", leadingIcon = MeshtasticIcons.Android, enabled = true) {} }
}

@Preview(showBackground = true)
@Composable
fun ListItemDisabledPreview() {
    AppTheme { ListItem(text = "Text", leadingIcon = MeshtasticIcons.Android, enabled = false) {} }
}

@Preview(showBackground = true)
@Composable
fun SwitchListItemPreview() {
    AppTheme { SwitchListItem(text = "Text", leadingIcon = MeshtasticIcons.Android, checked = true, onClick = {}) }
}

@Preview(showBackground = true)
@Composable
private fun ListItemPreviewSupportingText() {
    AppTheme {
        ListItem(text = "Text 1", leadingIcon = MeshtasticIcons.Android, supportingText = "Text2", trailingIcon = null)
    }
}
