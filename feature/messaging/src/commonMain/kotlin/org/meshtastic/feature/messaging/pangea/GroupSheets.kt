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
package org.meshtastic.feature.messaging.pangea

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.pangea.GroupChatError
import org.meshtastic.core.model.pangea.GroupInvite
import org.meshtastic.core.model.pangea.PangeaCode
import org.meshtastic.core.model.pangea.clampedUtf8
import org.meshtastic.core.repository.PangeaGroup
import org.meshtastic.core.repository.PendingGroupInvite
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.done
import org.meshtastic.core.resources.pangea_create_group
import org.meshtastic.core.resources.pangea_decline
import org.meshtastic.core.resources.pangea_error_not_connected
import org.meshtastic.core.resources.pangea_group_error_blank
import org.meshtastic.core.resources.pangea_group_error_cant_share_key
import org.meshtastic.core.resources.pangea_group_error_cant_share_name
import org.meshtastic.core.resources.pangea_group_error_channels_not_loaded
import org.meshtastic.core.resources.pangea_group_error_full
import org.meshtastic.core.resources.pangea_group_error_no_key
import org.meshtastic.core.resources.pangea_group_error_reserved
import org.meshtastic.core.resources.pangea_group_error_too_long
import org.meshtastic.core.resources.pangea_group_invites
import org.meshtastic.core.resources.pangea_group_name
import org.meshtastic.core.resources.pangea_group_name_rules
import org.meshtastic.core.resources.pangea_invite_from
import org.meshtastic.core.resources.pangea_invite_people
import org.meshtastic.core.resources.pangea_invite_sent
import org.meshtastic.core.resources.pangea_join
import org.meshtastic.core.resources.pangea_join_group_prompt
import org.meshtastic.core.resources.pangea_leave_group
import org.meshtastic.core.resources.pangea_leave_group_confirm
import org.meshtastic.core.resources.pangea_new_group
import org.meshtastic.core.resources.pangea_share_group
import org.meshtastic.core.resources.pangea_share_group_subtitle
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.component.QrDialog
import org.meshtastic.core.ui.icon.Group
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosButton
import org.meshtastic.core.ui.ios.IosButtonStyle
import org.meshtastic.core.ui.ios.IosCard
import org.meshtastic.core.ui.ios.IosInset
import org.meshtastic.core.ui.ios.IosSheet
import org.meshtastic.core.ui.ios.IosTextField

/** Invites waiting in Chats, each with Join and Decline (Pangea-Apple ChatsView invite rows). */
@Suppress("ViewModelForwarding")
@Composable
fun GroupInvitesSection(viewModel: PangeaChatsViewModel, onJoin: (String) -> Unit, modifier: Modifier = Modifier) {
    val pending by viewModel.pendingInvites.collectAsStateWithLifecycle()
    if (pending.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(stringResource(Res.string.pangea_group_invites), style = Ios.type.subheadline)
        pending.forEach { item ->
            InviteCard(
                item = item,
                sender = viewModel.senderName(item.fromNum),
                onJoin = { viewModel.join(item.invite) { index -> onJoin(viewModel.groupContactKey(index)) } },
                onDecline = { viewModel.decline(item) },
            )
        }
    }
    GroupErrorDialog(viewModel)
}

@Composable
private fun InviteCard(item: PendingGroupInvite, sender: String, onJoin: () -> Unit, onDecline: () -> Unit) {
    IosCard(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.invite.name, style = Ios.type.headline)
            Text(
                stringResource(Res.string.pangea_invite_from, sender),
                style = Ios.type.footnote,
                color = Ios.colors.secondaryLabel,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                IosButton(text = stringResource(Res.string.pangea_join), onClick = onJoin)
                IosButton(
                    text = stringResource(Res.string.pangea_decline),
                    onClick = onDecline,
                    style = IosButtonStyle.Tinted,
                )
            }
        }
    }
}

/** Name a new group; it gets a fresh private key and the first free slot. */
@Suppress("ViewModelForwarding", "ModifierMissing")
@Composable
fun NewGroupDialog(viewModel: PangeaChatsViewModel, onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    val c = Ios.colors
    IosSheet(
        title = stringResource(Res.string.pangea_new_group),
        cancelLabel = stringResource(Res.string.cancel),
        onDismissRequest = onDismiss,
    ) {
        Column(
            Modifier.fillMaxWidth().weight(1f).padding(horizontal = IosInset),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                MeshtasticIcons.Group,
                contentDescription = null,
                tint = c.tint,
                modifier = Modifier.padding(top = 12.dp).size(64.dp),
            )
            IosTextField(
                value = name,
                onValueChange = { name = it.clampedUtf8(GroupInvite.MAX_NAME_BYTES) },
                placeholder = stringResource(Res.string.pangea_group_name),
                textAlign = TextAlign.Center,
                textStyle = Ios.type.title3,
                modifier = Modifier.height(56.dp),
            )
            Text(
                stringResource(Res.string.pangea_group_name_rules),
                style = Ios.type.footnote,
                color = c.secondaryLabel,
                textAlign = TextAlign.Center,
            )
        }
        IosButton(
            text = stringResource(Res.string.pangea_create_group),
            onClick = { viewModel.createGroup(name) { index -> onCreate(viewModel.groupContactKey(index)) } },
            enabled = name.isNotBlank(),
            large = true,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
        )
    }
    GroupErrorDialog(viewModel)
}

/** Share a group by link or QR, invite people privately, or leave it (Pangea-Apple GroupSheets). */
@Suppress("ViewModelForwarding", "LongMethod", "ModifierMissing")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoSheet(viewModel: PangeaChatsViewModel, group: PangeaGroup, onLeave: () -> Unit, onDismiss: () -> Unit) {
    val people by viewModel.people.collectAsStateWithLifecycle()
    var showQr by remember { mutableStateOf<GroupInvite?>(null) }
    var confirmLeave by remember { mutableStateOf(false) }
    val invited = remember { mutableStateOf(setOf<Long>()) }
    IosSheet(
        title = group.name,
        onDismissRequest = onDismiss,
        actionLabel = stringResource(Res.string.done),
        onAction = onDismiss,
    ) {
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    IosButton(
                        text = stringResource(Res.string.pangea_share_group),
                        onClick = {
                            viewModel
                                .shareable(group)
                                .onSuccess { showQr = it }
                                .onFailure { viewModel.reportShareError(it) }
                        },
                    )
                    IosButton(
                        text = stringResource(Res.string.pangea_leave_group),
                        onClick = { confirmLeave = true },
                        style = IosButtonStyle.Tinted,
                    )
                }
            }
            item { Text(stringResource(Res.string.pangea_invite_people), style = Ios.type.subheadline) }
            items(people.filter { it.canMessage }, key = { it.num }) { person ->
                val sent = person.num in invited.value
                ListItem(
                    headlineContent = { Text(person.name) },
                    trailingContent = {
                        if (sent) {
                            Text(stringResource(Res.string.pangea_invite_sent), style = Ios.type.caption1)
                        } else {
                            IosButton(
                                text = stringResource(Res.string.pangea_invite_people),
                                onClick = { viewModel.sendInvite(group, person.num) { invited.value += person.num } },
                                style = IosButtonStyle.Plain,
                            )
                        }
                    },
                )
            }
        }
    }
    showQr?.let { invite ->
        QrDialog(
            title = group.name,
            uriString = invite.link,
            subtitle = stringResource(Res.string.pangea_share_group_subtitle),
            onDismiss = { showQr = null },
        )
    }
    if (confirmLeave) {
        MeshtasticDialog(
            title = stringResource(Res.string.pangea_leave_group),
            message = stringResource(Res.string.pangea_leave_group_confirm, group.name),
            confirmText = stringResource(Res.string.pangea_leave_group),
            confirmDestructive = true,
            onConfirm = { viewModel.leave(group.index, onLeave) },
            onDismiss = { confirmLeave = false },
        )
    }
    GroupErrorDialog(viewModel)
}

@Composable
private fun GroupErrorDialog(viewModel: PangeaChatsViewModel) {
    val error by viewModel.groupError.collectAsStateWithLifecycle()
    val shown = error ?: return
    MeshtasticDialog(message = stringResource(shown.message), onConfirm = viewModel::clearGroupError)
}

val GroupChatError.message: StringResource
    get() =
        when (this) {
            GroupChatError.NOT_CONNECTED -> Res.string.pangea_error_not_connected
            GroupChatError.CHANNELS_NOT_LOADED -> Res.string.pangea_group_error_channels_not_loaded
            GroupChatError.FULL -> Res.string.pangea_group_error_full
            GroupChatError.NO_KEY -> Res.string.pangea_group_error_no_key
            GroupChatError.RESERVED_NAME -> Res.string.pangea_group_error_reserved
            GroupChatError.BLANK_NAME -> Res.string.pangea_group_error_blank
            GroupChatError.TOO_LONG -> Res.string.pangea_group_error_too_long
            GroupChatError.CANT_SHARE_NAME -> Res.string.pangea_group_error_cant_share_name
            GroupChatError.CANT_SHARE_KEY -> Res.string.pangea_group_error_cant_share_key
        }

/**
 * Acts on a pangea:// link opened from outside the app: a contact link starts New Message on that person, a group link
 * asks whether to join.
 */
@Composable
fun PendingPangeaLink(viewModel: PangeaChatsViewModel, onStartChat: () -> Unit, onJoin: (String) -> Unit) {
    val startChat by rememberUpdatedState(onStartChat)
    val link by viewModel.pendingLink.collectAsStateWithLifecycle()
    val current = link ?: return
    val group = remember(current) { GroupInvite.fromLink(current) }
    if (group == null) {
        LaunchedEffect(current) {
            viewModel.consumeLink()
            PangeaCode.invite(current)?.let {
                viewModel.resolve(it)
                startChat()
            }
        }
        return
    }
    MeshtasticDialog(
        title = group.name,
        message = stringResource(Res.string.pangea_join_group_prompt, group.name),
        confirmText = stringResource(Res.string.pangea_join),
        onConfirm = {
            viewModel.consumeLink()
            viewModel.join(group) { index -> onJoin(viewModel.groupContactKey(index)) }
        },
        onDismiss = viewModel::consumeLink,
    )
}
