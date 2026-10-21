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
package org.meshtastic.core.ui.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

/**
 * An iOS 26 menu: the rounded glass panel a toolbar button's pull-down or a long press opens, its items a row each with
 * the symbol on the leading side. Fill it with [IosMenuItem]s and [IosMenuDivider]s. A long-press menu on a full-width
 * row passes [IosContextMenuOffset], so it sits in from the screen's edge as iOS's does.
 */
@Composable
fun IosMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.widthIn(min = MENU_MIN_WIDTH),
        offset = offset,
        shape = RoundedCornerShape(MENU_CORNER),
        // Opaque: a translucent panel lets its own shadow show through as a grey box. In Dark Mode iOS lifts a menu
        // to the elevated grey, lighter than the page under it.
        containerColor = if (Ios.colors.isDark) DARK_MENU else Ios.colors.chrome.copy(alpha = 1f),
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
        content = content,
    )
}

/**
 * One choice in an [IosMenu]. [checked] puts iOS's checkmark in the symbol's place, and [checkable] keeps that column
 * when unchecked, so a picker's labels line up; [destructive] draws it in red, as `Button(role: .destructive)` does.
 */
@Composable
fun IosMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    checked: Boolean = false,
    checkable: Boolean = false,
    destructive: Boolean = false,
    enabled: Boolean = true,
) {
    val color = if (destructive) Ios.colors.systemRed else Ios.colors.label
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val symbol = if (checked) IosIcons.Checkmark else icon
        if (symbol != null || checkable) {
            Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                if (symbol != null) {
                    Icon(symbol, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
        }
        Text(text = text, style = Ios.type.body, color = color, modifier = Modifier.padding(vertical = 11.dp))
    }
}

/** The hairline between groups of an [IosMenu]'s items. */
@Composable
fun IosMenuDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 4.dp)
            .height(0.5.dp)
            .background(Ios.colors.separator),
    )
}

/** Where a row's long-press menu opens: in from the leading edge, as iOS's context menus are. */
val IosContextMenuOffset = DpOffset(16.dp, 0.dp)

private val MENU_MIN_WIDTH = 220.dp
private val DARK_MENU = Color(0xFF2C2C2E)
private val MENU_CORNER = 24.dp
private const val DISABLED_ALPHA = 0.4f
