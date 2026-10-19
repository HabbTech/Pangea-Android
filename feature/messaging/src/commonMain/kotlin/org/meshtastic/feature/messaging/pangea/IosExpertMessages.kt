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
@file:Suppress("MatchingDeclarationName")

package org.meshtastic.feature.messaging.pangea

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.channels
import org.meshtastic.core.resources.direct_messages
import org.meshtastic.core.resources.filter_settings
import org.meshtastic.core.resources.mark_as_read
import org.meshtastic.core.resources.messages
import org.meshtastic.core.resources.pangea_more
import org.meshtastic.core.resources.pangea_new_message
import org.meshtastic.core.ui.component.MeshtasticImportFAB
import org.meshtastic.core.ui.component.PangeaLogoButton
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosGlassIconButton
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosInset
import org.meshtastic.core.ui.ios.IosLargeTitleScreen
import org.meshtastic.core.ui.ios.LocalIosBottomInset
import org.meshtastic.core.ui.ios.iosPressable

/** The two lists under Messages in Expert Mode. */
enum class ExpertChatSection {
    CHANNELS,
    DIRECT,
}

/**
 * Messages in Expert Mode, as on the Pangea iPhone app: **Channels** (with how many) and **Direct Messages**, each
 * opening its own list. Android keeps three things iOS doesn't have here: mark all as read and the message filters in a
 * More menu, and the import button for channel links.
 */
@Suppress("LongParameterList")
@Composable
internal fun IosExpertMessagesScreen(
    channelCount: Int,
    unreadCount: Int,
    connected: Boolean,
    onOpen: (ExpertChatSection) -> Unit,
    onCompose: () -> Unit,
    onMarkAllRead: () -> Unit,
    onFilterSettings: () -> Unit,
    onImport: (String) -> Unit,
    onShareChannels: () -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {},
) {
    Box(modifier.fillMaxSize()) {
        IosLargeTitleScreen(
            title = stringResource(Res.string.messages),
            grouped = false,
            leading = { PangeaLogoButton() },
            trailing = {
                var more by remember { mutableStateOf(false) }
                Box {
                    IosGlassIconButton(
                        icon = IosIcons.Ellipsis,
                        contentDescription = stringResource(Res.string.pangea_more),
                        onClick = { more = true },
                        tint = Ios.colors.tint,
                    )
                    DropdownMenu(expanded = more, onDismissRequest = { more = false }) {
                        if (unreadCount > 0) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.mark_as_read)) },
                                onClick = {
                                    more = false
                                    onMarkAllRead()
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.filter_settings)) },
                            onClick = {
                                more = false
                                onFilterSettings()
                            },
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                IosGlassIconButton(
                    icon = IosIcons.Compose,
                    contentDescription = stringResource(Res.string.pangea_new_message),
                    onClick = onCompose,
                    tint = Ios.colors.tint,
                )
            },
        ) {
            item(key = "messages-header") { header() }
            item(key = "messages-rule") { SectionRule() }
            item(key = "messages-channels") {
                SectionRow(IosIcons.People, stringResource(Res.string.channels), channelCount.takeIf { it > 0 }) {
                    onOpen(ExpertChatSection.CHANNELS)
                }
            }
            item(key = "messages-direct") {
                SectionRow(IosIcons.Person, stringResource(Res.string.direct_messages), null) {
                    onOpen(ExpertChatSection.DIRECT)
                }
            }
        }
        if (connected) {
            MeshtasticImportFAB(
                onImport = onImport,
                onShareChannels = onShareChannels,
                isContactContext = false,
                modifier =
                Modifier.align(Alignment.BottomEnd)
                    .padding(end = IosInset, bottom = LocalIosBottomInset.current + 16.dp),
            )
        }
    }
}

@Composable
private fun SectionRule() {
    Box(Modifier.fillMaxWidth().padding(horizontal = IosInset).height(0.5.dp).background(Ios.colors.separator))
}

@Composable
private fun SectionRow(icon: ImageVector, title: String, count: Int?, onClick: () -> Unit) {
    Column {
        SectionRowContent(icon, title, count, onClick)
        SectionRule()
    }
}

@Composable
private fun SectionRowContent(icon: ImageVector, title: String, count: Int?, onClick: () -> Unit) {
    val c = Ios.colors
    Row(
        Modifier.fillMaxWidth().iosPressable(onClick = onClick).padding(horizontal = IosInset, vertical = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = c.tint, modifier = Modifier.size(38.dp))
        Spacer(Modifier.width(22.dp))
        Text(title, style = Ios.type.title2, color = c.label, modifier = Modifier.weight(1f))
        if (count != null) {
            Text(count.toString(), style = Ios.type.body, color = c.secondaryLabel)
            Spacer(Modifier.width(12.dp))
        }
        Icon(IosIcons.ChevronRight, contentDescription = null, tint = c.tertiaryLabel, modifier = Modifier.size(14.dp))
    }
}
