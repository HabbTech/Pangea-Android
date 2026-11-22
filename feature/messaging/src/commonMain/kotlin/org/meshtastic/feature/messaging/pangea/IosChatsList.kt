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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.isSensitiveData
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.model.Contact
import org.meshtastic.core.model.ContactKey
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.contact_draft_prefix
import org.meshtastic.core.resources.delete
import org.meshtastic.core.resources.navigate_back
import org.meshtastic.core.resources.pangea_message_someone
import org.meshtastic.core.resources.pangea_mute
import org.meshtastic.core.resources.pangea_new_group
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
import org.meshtastic.core.ui.component.SecurityIcon
import org.meshtastic.core.ui.component.rememberAvatarBitmap
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosButton
import org.meshtastic.core.ui.ios.IosButtonStyle
import org.meshtastic.core.ui.ios.IosContextMenuOffset
import org.meshtastic.core.ui.ios.IosGlassIconButton
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosInset
import org.meshtastic.core.ui.ios.IosLargeTitleScreen
import org.meshtastic.core.ui.ios.IosMenu
import org.meshtastic.core.ui.ios.IosMenuItem
import org.meshtastic.core.ui.ios.IosSearchField
import org.meshtastic.feature.messaging.ui.contact.SwipeableContactRow
import org.meshtastic.proto.ChannelSet

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
    onComposeGroup: () -> Unit,
    onNewGroup: () -> Unit,
    onToggleMute: (Contact) -> Unit,
    onTogglePin: (Contact) -> Unit,
    onDelete: (Contact) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    onBack: (() -> Unit)? = null,
    showCompose: Boolean = true,
    expert: Boolean = false,
    channels: ChannelSet? = null,
    searchPlaceholder: String? = null,
    header: @Composable () -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    val ordered = remember(contacts, expert, query) { orderChats(contacts, expert, query) }
    IosLargeTitleScreen(
        title = title ?: stringResource(Res.string.pangea_tab_chats),
        grouped = false,
        listState = listState,
        modifier = modifier,
        leading = {
            if (onBack != null) {
                IosGlassIconButton(
                    icon = IosIcons.ChevronLeft,
                    contentDescription = stringResource(Res.string.navigate_back),
                    onClick = onBack,
                )
            } else {
                PangeaLogoButton()
            }
        },
        trailing = if (showCompose) composeButton(onCompose, onComposeGroup) else null,
    ) {
        item(key = "chats-header") { header() }
        if (searchPlaceholder != null) {
            item(key = "chats-search") {
                IosSearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = searchPlaceholder,
                    glass = false,
                    modifier = Modifier.padding(start = IosInset, end = IosInset, bottom = 16.dp),
                )
            }
        }
        if (ordered.isEmpty()) {
            if (!expert) item(key = "chats-empty") { NoChatsYet(onCompose = onCompose, onNewGroup = onNewGroup) }
        } else {
            item(key = "chats-top-rule") { Rule(start = if (expert) IosInset else RowTextStart) }
            items(ordered, key = { it.contactKey }) { contact ->
                SwipeableContactRow(
                    contact = contact,
                    enabled = true,
                    onMute = { onToggleMute(contact) },
                    onDelete = { onDelete(contact) },
                ) {
                    IosChatRow(
                        contact = contact,
                        expert = expert,
                        channels = channels,
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

/** The compose button, which offers a new message or a new group, as on iOS. */
private fun composeButton(onCompose: () -> Unit, onComposeGroup: () -> Unit): @Composable RowScope.() -> Unit = {
    var composeMenu by remember { mutableStateOf(false) }
    Box {
        IosGlassIconButton(
            icon = IosIcons.Compose,
            contentDescription = stringResource(Res.string.pangea_new_message),
            onClick = { composeMenu = true },
            tint = Ios.colors.tint,
        )
        IosMenu(expanded = composeMenu, onDismissRequest = { composeMenu = false }) {
            IosMenuItem(
                text = stringResource(Res.string.pangea_new_message),
                icon = IosIcons.Compose,
                onClick = {
                    composeMenu = false
                    onCompose()
                },
            )
            IosMenuItem(
                text = stringResource(Res.string.pangea_new_group),
                icon = IosIcons.People,
                onClick = {
                    composeMenu = false
                    onComposeGroup()
                },
            )
        }
    }
}

/**
 * The chats to show, matching [query]: Everyone Nearby first, then pinned, then by latest message. Expert Mode's
 * Channels list keeps the radio's slot order instead, as iOS does.
 */
private fun orderChats(contacts: List<Contact>, expert: Boolean, query: String): List<Contact> {
    val matching =
        contacts.filter {
            query.isBlank() || it.longName.contains(query, ignoreCase = true) || it.shortName.contains(query, true)
        }
    if (expert && matching.all { it.contactKey.endsWith(NodeAddress.ID_BROADCAST) }) {
        return matching.sortedBy { ContactKey(it.contactKey).channelOrNull ?: Int.MAX_VALUE }
    }
    val (nearby, rest) = matching.partition { it.isEveryoneNearby() }
    return nearby +
        rest.sortedWith(compareByDescending<Contact> { it.isPinned }.thenByDescending { it.lastMessageTime ?: 0L })
}

@Composable
private fun Rule(start: Dp) {
    Box(Modifier.fillMaxWidth().padding(start = start, end = IosInset).height(0.5.dp).background(Ios.colors.separator))
}

/** One chat: avatar, name and time, the latest message (two lines), unread count and a chevron. */
@Suppress("LongMethod")
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun IosChatRow(
    contact: Contact,
    expert: Boolean,
    channels: ChannelSet?,
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
            val channelIndex = ContactKey(contact.contactKey).channelOrNull
            val isChannel = contact.contactKey.endsWith(NodeAddress.ID_BROADCAST)
            // Expert Mode shows a channel's key state before its name; a retired channel has none left.
            val lockChannels = channels?.takeIf { expert && isChannel && !contact.isRetired }
            Row(
                Modifier.fillMaxWidth()
                    .padding(start = if (expert) 4.dp else IosInset, end = IosInset, top = 17.dp, bottom = 17.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (expert) {
                    // Expert Mode marks unread chats with a dot before the avatar, as iOS does.
                    Box(Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                        if (contact.unreadCount > 0) Box(Modifier.size(10.dp).clip(CircleShape).background(c.tint))
                    }
                }
                if (expert && isChannel && channelIndex != null) {
                    ChannelNumberAvatar(channelIndex)
                } else {
                    ChatAvatar(contact)
                }
                Spacer(Modifier.width(RowTextStart - IosInset - AvatarSize))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (lockChannels != null && channelIndex != null) {
                            SecurityIcon(channels, channelIndex)
                            Spacer(Modifier.width(6.dp))
                        }
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
                            maxLines = if (expert) 1 else 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).semantics { isSensitiveData = true },
                        )
                        if (contact.unreadCount > 0 && !expert) {
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
            Rule(start = if (expert) IosInset else RowTextStart)
        }
        IosMenu(expanded = menu, onDismissRequest = { menu = false }, offset = IosContextMenuOffset) {
            IosMenuItem(
                text = stringResource(if (contact.isMuted) Res.string.unmute else Res.string.pangea_mute),
                icon = if (contact.isMuted) IosIcons.Bell else IosIcons.BellSlash,
                onClick = {
                    menu = false
                    onToggleMute()
                },
            )
            IosMenuItem(
                text = stringResource(if (contact.isPinned) Res.string.pangea_unpin else Res.string.pangea_pin),
                icon = IosIcons.Pin,
                onClick = {
                    menu = false
                    onTogglePin()
                },
            )
            IosMenuItem(
                text = stringResource(Res.string.delete),
                icon = IosIcons.Trash,
                destructive = true,
                onClick = {
                    menu = false
                    onDelete()
                },
            )
        }
    }
}

/**
 * A chat's picture: Everyone Nearby is a white globe on blue; a group or person shows their photo or emoji and colour
 * when set, otherwise their short name (a person) or first letter (a group) on their colour. Used by the chat list, the
 * chat header and the bubbles.
 */
@Composable
internal fun IosChatAvatar(
    contactKey: String,
    shortName: String,
    longName: String,
    nodeColor: Color?,
    modifier: Modifier = Modifier,
    isRetired: Boolean = false,
    size: Dp = AvatarSize,
) {
    if (isEveryoneNearbyKey(contactKey, isRetired)) {
        Box(
            modifier.size(size).clip(CircleShape).background(Ios.colors.everyoneNearby),
            contentAlignment = Alignment.Center,
        ) {
            Icon(IosIcons.Globe, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.56f))
        }
        return
    }
    val custom = LocalPangeaAvatars.current(contactKey)
    val isChannel = contactKey.endsWith(NodeAddress.ID_BROADCAST)
    val fallback = nodeColor ?: AvatarPalette.color((longName.hashCode() and Int.MAX_VALUE) % AvatarPalette.colors.size)
    PangeaAvatar(
        photo = rememberAvatarBitmap(custom?.photo),
        emoji = custom?.emoji.orEmpty(),
        colorIndex = custom?.color,
        initials = if (isChannel) longName.take(1).uppercase() else shortName,
        fallbackColor = fallback,
        size = size,
        modifier = modifier,
    )
}

/** A channel in Expert Mode: its slot number on an orange circle, as on the iPhone app. */
@Composable
private fun ChannelNumberAvatar(index: Int) {
    Box(
        Modifier.size(AvatarSize).clip(CircleShape).background(Ios.colors.tint.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(index.toString(), style = Ios.type.title1, color = Color.White)
    }
}

@Composable
private fun ChatAvatar(contact: Contact) {
    IosChatAvatar(
        contactKey = contact.contactKey,
        shortName = contact.shortName,
        longName = contact.longName,
        nodeColor = contact.nodeColors?.let { Color(it.second) },
        isRetired = contact.isRetired,
    )
}

/** Channel 0's group chat: a broadcast key (a direct chat on channel 0 has a node address instead). */
internal fun isEveryoneNearbyKey(contactKey: String, isRetired: Boolean = false): Boolean =
    !isRetired && contactKey.endsWith(NodeAddress.ID_BROADCAST) && ContactKey(contactKey).channelOrNull == 0

private fun Contact.isEveryoneNearby(): Boolean = isEveryoneNearbyKey(contactKey, isRetired)

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
