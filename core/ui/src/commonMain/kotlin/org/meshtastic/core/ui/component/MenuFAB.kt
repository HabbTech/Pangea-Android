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

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.close
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosGlassIconButton
import org.meshtastic.core.ui.ios.IosIcons

/**
 * A floating button with a menu of actions, drawn as the Pangea iPhone app draws its floating controls: a round glass
 * button with the symbol in the tint, opening a menu of [items] (iOS's context menu) above it.
 */
@Composable
fun MenuFAB(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    items: List<MenuFABItem>,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    testTag: String? = null,
) {
    val stateAwareDescription = if (expanded) stringResource(Res.string.close) else contentDescription
    Box(modifier = modifier) {
        IosGlassIconButton(
            icon = IosIcons.ShareUp,
            contentDescription = stateAwareDescription,
            onClick = { onExpandedChange(!expanded) },
            tint = Ios.colors.tint,
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier,
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            items.forEach { item ->
                DropdownMenuItem(
                    modifier = if (item.testTag != null) Modifier.testTag(item.testTag) else Modifier,
                    onClick = {
                        item.onClick()
                        onExpandedChange(false)
                    },
                    leadingIcon = { Icon(item.icon, contentDescription = null) },
                    text = { Text(item.label) },
                )
            }
        }
    }
}

data class MenuFABItem(val label: String, val icon: ImageVector, val onClick: () -> Unit, val testTag: String? = null)
