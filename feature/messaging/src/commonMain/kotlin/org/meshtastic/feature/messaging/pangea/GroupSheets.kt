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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import org.meshtastic.core.resources.okay
import org.meshtastic.core.resources.pangea_create
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
import org.meshtastic.core.ui.component.QrDialog

/** Invites waiting in Chats, each with Join and Decline (Pangea-Apple ChatsView invite rows). */
@Suppress("ViewModelForwarding")
@Composable
fun GroupInvitesSection(viewModel: PangeaChatsViewModel, onJoin: (String) -> Unit, modifier: Modifier = Modifier) {
    val pending by viewModel.pendingInvites.collectAsStateWithLifecycle()
    if (pending.isEmpty()) return
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(stringResource(Res.string.pangea_group_invites), style = MaterialTheme.typography.titleSmall)
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
    Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.invite.name, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(Res.string.pangea_invite_from, sender),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                Button(onClick = onJoin) { Text(stringResource(Res.string.pangea_join)) }
                OutlinedButton(onClick = onDecline) { Text(stringResource(Res.string.pangea_decline)) }
            }
        }
    }
}

/** Name a new group; it gets a fresh private key and the first free slot. */
@Suppress("ViewModelForwarding")
@Composable
fun NewGroupDialog(viewModel: PangeaChatsViewModel, onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.pangea_new_group)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.clampedUtf8(GroupInvite.MAX_NAME_BYTES) },
                label = { Text(stringResource(Res.string.pangea_group_name)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.createGroup(name) { index -> onCreate(viewModel.groupContactKey(index)) } },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(Res.string.pangea_create))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
    GroupErrorDialog(viewModel)
}

/** Share a group by link or QR, invite people privately, or leave it (Pangea-Apple GroupSheets). */
@Suppress("ViewModelForwarding", "LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoSheet(viewModel: PangeaChatsViewModel, group: PangeaGroup, onLeave: () -> Unit, onDismiss: () -> Unit) {
    val people by viewModel.people.collectAsStateWithLifecycle()
    var showQr by remember { mutableStateOf<GroupInvite?>(null) }
    var confirmLeave by remember { mutableStateOf(false) }
    val invited = remember { mutableStateOf(setOf<Long>()) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState =
        rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
    ) {
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            item { Text(group.name, style = MaterialTheme.typography.titleLarge) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    Button(
                        onClick = {
                            viewModel
                                .shareable(group)
                                .onSuccess { showQr = it }
                                .onFailure { viewModel.reportShareError(it) }
                        },
                    ) {
                        Text(stringResource(Res.string.pangea_share_group))
                    }
                    OutlinedButton(onClick = { confirmLeave = true }) {
                        Text(stringResource(Res.string.pangea_leave_group))
                    }
                }
            }
            item { Text(stringResource(Res.string.pangea_invite_people), style = MaterialTheme.typography.titleSmall) }
            items(people.filter { it.canMessage }, key = { it.num }) { person ->
                val sent = person.num in invited.value
                ListItem(
                    headlineContent = { Text(person.name) },
                    trailingContent = {
                        if (sent) {
                            Text(
                                stringResource(Res.string.pangea_invite_sent),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        } else {
                            TextButton(
                                onClick = { viewModel.sendInvite(group, person.num) { invited.value += person.num } },
                            ) {
                                Text(stringResource(Res.string.pangea_invite_people))
                            }
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
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text(stringResource(Res.string.pangea_leave_group)) },
            text = { Text(stringResource(Res.string.pangea_leave_group_confirm, group.name)) },
            confirmButton = {
                TextButton(onClick = { viewModel.leave(group.index, onLeave) }) {
                    Text(stringResource(Res.string.pangea_leave_group))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeave = false }) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }
    GroupErrorDialog(viewModel)
}

@Composable
private fun GroupErrorDialog(viewModel: PangeaChatsViewModel) {
    val error by viewModel.groupError.collectAsStateWithLifecycle()
    val shown = error ?: return
    AlertDialog(
        onDismissRequest = viewModel::clearGroupError,
        text = { Text(stringResource(shown.message)) },
        confirmButton = { TextButton(onClick = viewModel::clearGroupError) { Text(stringResource(Res.string.okay)) } },
    )
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
    AlertDialog(
        onDismissRequest = viewModel::consumeLink,
        title = { Text(group.name) },
        text = { Text(stringResource(Res.string.pangea_join_group_prompt, group.name)) },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.consumeLink()
                    viewModel.join(group) { index -> onJoin(viewModel.groupContactKey(index)) }
                },
            ) {
                Text(stringResource(Res.string.pangea_join))
            }
        },
        dismissButton = { TextButton(onClick = viewModel::consumeLink) { Text(stringResource(Res.string.cancel)) } },
    )
}
