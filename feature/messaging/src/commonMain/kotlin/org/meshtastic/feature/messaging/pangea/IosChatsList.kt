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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.isSensitiveData
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.model.Contact
import org.meshtastic.core.model.ContactKey
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.contact_draft_prefix
import org.meshtastic.core.resources.delete
import org.meshtastic.core.resources.pangea_message_someone
import org.meshtastic.core.resources.pangea_mute
import org.meshtastic.core.resources.pangea_new_message
import org.meshtastic.core.resources.pangea_no_chats_yet
import org.meshtastic.core.resources.pangea_pin
import org.meshtastic.core.resources.pangea_start_group
import org.meshtastic.core.resources.pangea_tab_chats
import org.meshtastic.core.resources.pangea_unpin
import org.meshtastic.core.resources.unmute
import org.meshtastic.core.ui.component.AvatarPalette
import org.meshtastic.core.ui.component.PangeaAvatar
import org.meshtastic.core.ui.component.PangeaLogoButton
import org.meshtastic.core.ui.component.rememberAvatarBitmap
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosButton
import org.meshtastic.core.ui.ios.IosButtonStyle
import org.meshtastic.core.ui.ios.IosGlassIconButton
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosInset
import org.meshtastic.core.ui.ios.IosLargeTitleScreen
import org.meshtastic.feature.messaging.ui.contact.SwipeableContactRow

/** Where a chat row's text starts: 16 margin + 46 avatar + 12 gap, as measured on the iPhone app. */
private val RowTextStart = 74.dp
private val AvatarSize = 46.dp

/**
 * Chats in standard mode, drawn like the Pangea iPhone app: large title, the logo and compose buttons, and a plain list
 * with Everyone Nearby first and every other chat by its latest message. Long-press for the chat's actions; swipe to
 * mute or delete as before.
 */
@Composable
internal fun IosChatsScreen(
    contacts: List<Contact>,
    listState: LazyListState,
    onOpen: (Contact) -> Unit,
    onCompose: () -> Unit,
    onNewGroup: () -> Unit,
    onToggleMute: (Contact) -> Unit,
    onTogglePin: (Contact) -> Unit,
    onDelete: (Contact) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {},
) {
    val ordered =
        remember(contacts) {
            val (nearby, rest) = contacts.partition { it.isEveryoneNearby() }
            nearby +
                rest.sortedWith(
                    compareByDescending<Contact> { it.isPinned }.thenByDescending { it.lastMessageTime ?: 0L },
                )
        }
    IosLargeTitleScreen(
        title = stringResource(Res.string.pangea_tab_chats),
        grouped = false,
        listState = listState,
        modifier = modifier,
        leading = { PangeaLogoButton() },
        trailing = {
            IosGlassIconButton(
                icon = IosIcons.Compose,
                contentDescription = stringResource(Res.string.pangea_new_message),
                onClick = onCompose,
                tint = Ios.colors.tint,
            )
        },
    ) {
        item(key = "chats-header") { header() }
        if (ordered.isEmpty()) {
            item(key = "chats-empty") { NoChatsYet(onCompose = onCompose, onNewGroup = onNewGroup) }
        } else {
            item(key = "chats-top-rule") { Rule(start = RowTextStart) }
            items(ordered, key = { it.contactKey }) { contact ->
                SwipeableContactRow(
                    contact = contact,
                    enabled = true,
                    onMute = { onToggleMute(contact) },
                    onDelete = { onDelete(contact) },
                ) {
                    IosChatRow(
                        contact = contact,
                        onClick = { onOpen(contact) },
                        onToggleMute = { onToggleMute(contact) },
                        onTogglePin = { onTogglePin(contact) },
                        onDelete = { onDelete(contact) },
                    )
                }
            }
            item(key = "chats-bottom-rule") { Rule(start = IosInset) }
        }
    }
}

@Composable
private fun Rule(start: androidx.compose.ui.unit.Dp) {
    Box(Modifier.fillMaxWidth().padding(start = start, end = IosInset).height(0.5.dp).background(Ios.colors.separator))
}

/** One chat: avatar, name and time, the latest message (two lines), unread count and a chevron. */
@Suppress("LongMethod")
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IosChatRow(
    contact: Contact,
    onClick: () -> Unit,
    onToggleMute: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = Ios.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    var menu by remember { mutableStateOf(false) }
    Box(Modifier.background(c.background)) {
        Column(
            Modifier.fillMaxWidth()
                .background(if (pressed) c.systemGray4 else Color.Transparent)
                .combinedClickable(
                    interactionSource = source,
                    indication = null,
                    onClick = onClick,
                    onLongClick = { menu = true },
                ),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = IosInset, end = IosInset, top = 17.dp, bottom = 17.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChatAvatar(contact)
                Spacer(Modifier.width(RowTextStart - IosInset - AvatarSize))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            contact.longName,
                            style = Ios.type.headline,
                            color = c.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            contact.lastMessageTime?.let { DateFormatter.formatShortDate(it) }.orEmpty(),
                            style = Ios.type.subheadline,
                            color = c.secondaryLabel,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val draft = contact.draft.isNotBlank()
                        Text(
                            if (draft) {
                                stringResource(Res.string.contact_draft_prefix, contact.draft)
                            } else {
                                contact.lastMessageText.orEmpty()
                            },
                            style = Ios.type.subheadline,
                            color = c.secondaryLabel,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).semantics { isSensitiveData = true },
                        )
                        if (contact.unreadCount > 0) {
                            Box(
                                Modifier.padding(start = 8.dp)
                                    .clip(CircleShape)
                                    .background(c.tint)
                                    .padding(horizontal = 7.dp, vertical = 1.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (contact.unreadCount > 99) "99+" else contact.unreadCount.toString(),
                                    style = Ios.type.subheadline,
                                    color = c.onTint,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
                Icon(
                    IosIcons.ChevronRight,
                    contentDescription = null,
                    tint = c.tertiaryLabel,
                    modifier = Modifier.padding(start = 10.dp).size(14.dp),
                )
            }
            Rule(start = RowTextStart)
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(if (contact.isMuted) Res.string.unmute else Res.string.pangea_mute)) },
                onClick = {
                    menu = false
                    onToggleMute()
                },
            )
            DropdownMenuItem(
                text = {
                    Text(stringResource(if (contact.isPinned) Res.string.pangea_unpin else Res.string.pangea_pin))
                },
                onClick = {
                    menu = false
                    onTogglePin()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.delete), color = c.systemRed) },
                onClick = {
                    menu = false
                    onDelete()
                },
            )
        }
    }
}

/**
 * The chat's picture: Everyone Nearby is a white globe on blue; a group or person shows their photo or emoji and colour
 * when set, otherwise their short name (a person) or first letter (a group) on their colour.
 */
@Composable
private fun ChatAvatar(contact: Contact) {
    val c = Ios.colors
    if (contact.isEveryoneNearby()) {
        Box(
            Modifier.size(AvatarSize).clip(CircleShape).background(c.everyoneNearby),
            contentAlignment = Alignment.Center,
        ) {
            Icon(IosIcons.Globe, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
        return
    }
    val custom = LocalPangeaAvatars.current(contact.contactKey)
    val isChannel = contact.contactKey.endsWith(NodeAddress.ID_BROADCAST)
    val fallback =
        contact.nodeColors?.let { Color(it.second) }
            ?: AvatarPalette.color((contact.longName.hashCode() and Int.MAX_VALUE) % AvatarPalette.colors.size)
    PangeaAvatar(
        photo = rememberAvatarBitmap(custom?.photo),
        emoji = custom?.emoji.orEmpty(),
        colorIndex = custom?.color,
        initials = if (isChannel) contact.longName.take(1).uppercase() else contact.shortName,
        fallbackColor = fallback,
        size = AvatarSize,
    )
}

/** Channel 0's group chat: a broadcast key (a direct chat on channel 0 has a node address instead). */
private fun Contact.isEveryoneNearby(): Boolean =
    !isRetired && contactKey.endsWith(NodeAddress.ID_BROADCAST) && ContactKey(contactKey).channelOrNull == 0

/** "No Chats Yet", with **Message Someone** (filled) and **Start a Group** (soft), as on iOS. */
@Composable
private fun NoChatsYet(onCompose: () -> Unit, onNewGroup: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(Res.string.pangea_no_chats_yet), style = Ios.type.title2, color = Ios.colors.label)
        Spacer(Modifier.height(8.dp))
        IosButton(stringResource(Res.string.pangea_message_someone), onClick = onCompose, large = true)
        IosButton(
            stringResource(Res.string.pangea_start_group),
            onClick = onNewGroup,
            style = IosButtonStyle.Tinted,
            large = true,
        )
    }
}
