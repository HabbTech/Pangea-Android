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
@file:Suppress("MagicNumber")

package org.meshtastic.feature.messaging.pangea

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.navigate_back
import org.meshtastic.core.resources.pangea_everyone_bell
import org.meshtastic.core.resources.pangea_find_in_conversation
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosGlassIconButton
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosInset
import org.meshtastic.core.ui.ios.IosSearchField
import org.meshtastic.core.ui.ios.IosTopBar
import org.meshtastic.core.ui.ios.iosPressable

/**
 * The top of a chat, as on the Pangea iPhone app: the back button, the chat's avatar and name in the middle (tap for
 * the person's profile or the group's info), the mute bell for Everyone Nearby, and "Find in conversation" under it.
 */
@Composable
internal fun IosChatHeader(
    title: String,
    contactKey: String,
    shortName: String,
    nodeColor: Color?,
    onBack: () -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onTitleClick: (() -> Unit)? = null,
    bellOn: Boolean? = null,
    onToggleBell: () -> Unit = {},
    menu: (@Composable () -> Unit)? = null,
) {
    val c = Ios.colors
    Column(modifier) {
        IosTopBar(
            title = title,
            showTitle = true,
            background = c.background,
            titleContent = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = if (onTitleClick != null) Modifier.iosPressable(onClick = onTitleClick) else Modifier,
                ) {
                    IosChatAvatar(
                        contactKey = contactKey,
                        shortName = shortName,
                        longName = title,
                        nodeColor = nodeColor,
                        size = 36.dp,
                    )
                    Text(
                        title,
                        style = Ios.type.headline,
                        color = c.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            leading = {
                IosGlassIconButton(
                    IosIcons.ChevronLeft,
                    contentDescription = stringResource(Res.string.navigate_back),
                    onClick = onBack,
                )
            },
            trailing = {
                if (bellOn != null) {
                    IosGlassIconButton(
                        icon = if (bellOn) IosIcons.Bell else IosIcons.BellSlash,
                        contentDescription = stringResource(Res.string.pangea_everyone_bell),
                        onClick = onToggleBell,
                        tint = c.tint,
                    )
                }
            },
        )
        // Android's extra thread options ride at the end of the search capsule, leaving the bar as iOS has it.
        IosSearchField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = stringResource(Res.string.pangea_find_in_conversation),
            modifier = Modifier.padding(horizontal = IosInset, vertical = 4.dp),
            trailing = menu,
        )
    }
}
