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
package org.meshtastic.feature.messaging.ui.contact

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.common.util.CommonUri
import org.meshtastic.core.common.util.NumberFormatter
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.Contact
import org.meshtastic.core.model.ContactKey
import org.meshtastic.core.model.ContactSettings
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.pangea.ChatIcon
import org.meshtastic.core.model.util.TimeConstants
import org.meshtastic.core.model.util.formatMuteRemainingTime
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.are_you_sure
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.channel_invalid
import org.meshtastic.core.resources.channels
import org.meshtastic.core.resources.contact_muted_snackbar
import org.meshtastic.core.resources.currently
import org.meshtastic.core.resources.delete
import org.meshtastic.core.resources.delete_messages
import org.meshtastic.core.resources.mute_1_week
import org.meshtastic.core.resources.mute_8_hours
import org.meshtastic.core.resources.mute_always
import org.meshtastic.core.resources.mute_notifications
import org.meshtastic.core.resources.mute_status_always
import org.meshtastic.core.resources.mute_status_muted_for_days
import org.meshtastic.core.resources.mute_status_muted_for_hours
import org.meshtastic.core.resources.mute_status_unmuted
import org.meshtastic.core.resources.okay
import org.meshtastic.core.resources.pangea_contacts_count
import org.meshtastic.core.resources.pangea_everyone_nearby
import org.meshtastic.core.resources.pangea_find_contact
import org.meshtastic.core.resources.swipe_action_delete
import org.meshtastic.core.resources.swipe_action_mute
import org.meshtastic.core.resources.swipe_action_unmute
import org.meshtastic.core.resources.undo
import org.meshtastic.core.resources.unmute
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.component.MeshtasticTextDialog
import org.meshtastic.core.ui.component.ScrollToTopEvent
import org.meshtastic.core.ui.component.smartScrollToTop
import org.meshtastic.core.ui.icon.Delete
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.VolumeMute
import org.meshtastic.core.ui.icon.VolumeUp
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.pangea.SetupProfilePrompt
import org.meshtastic.core.ui.util.parseDeepLinkOrInvalid
import org.meshtastic.core.ui.util.rememberShowToastResource
import org.meshtastic.feature.messaging.pangea.AvatarVisual
import org.meshtastic.feature.messaging.pangea.ExpertChatSection
import org.meshtastic.feature.messaging.pangea.GroupInvitesSection
import org.meshtastic.feature.messaging.pangea.IosChatsScreen
import org.meshtastic.feature.messaging.pangea.IosExpertMessagesScreen
import org.meshtastic.feature.messaging.pangea.LocalPangeaAvatars
import org.meshtastic.feature.messaging.pangea.NewGroupDialog
import org.meshtastic.feature.messaging.pangea.NewMessageSheet
import org.meshtastic.feature.messaging.pangea.PangeaChatsViewModel
import org.meshtastic.feature.messaging.pangea.PendingPangeaLink
import org.meshtastic.feature.messaging.pangea.dmNodeNum
import kotlin.time.Duration.Companion.days

@Suppress("LongMethod", "CyclomaticComplexMethod", "LongParameterList", "ViewModelForwarding", "ModifierMissing")
@Composable
fun ContactsScreen(
    onNavigateToShare: () -> Unit,
    onHandleDeepLink: (CommonUri, onInvalid: () -> Unit) -> Unit,
    viewModel: ContactsViewModel,
    onNavigateToMessages: (String) -> Unit,
    onNavigateToNodeDetails: (Int) -> Unit,
    onNavigateToFilterSettings: () -> Unit,
    scrollToTopEvents: Flow<ScrollToTopEvent>?,
    pangeaViewModel: PangeaChatsViewModel = koinViewModel(),
    section: ExpertChatSection? = null,
    onOpenSection: (ExpertChatSection) -> Unit = {},
    onBack: (() -> Unit)? = null,
) {
    val showToast = rememberShowToastResource()
    viewModel.everyoneNearby = stringResource(Res.string.pangea_everyone_nearby)
    // Pangea: New Message (by @code, link or QR) and New Group, as on iOS.
    var showNewMessage by rememberSaveable { mutableStateOf(false) }
    var showNewGroup by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val ourNode by viewModel.ourNodeInfo.collectAsStateWithLifecycle()
    var showMuteDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    // State for managing selected contacts
    val selectedContactKeys = remember { mutableStateListOf<String>() }
    val isSelectionModeActive by remember { derivedStateOf { selectedContactKeys.isNotEmpty() } }

    // State for contacts list. Channel placeholders (empty broadcast channels) are already merged in by the VM.
    val contacts by viewModel.contactList.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()

    val contactsListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(scrollToTopEvents) {
        scrollToTopEvents?.collectLatest { event ->
            if (event == ScrollToTopEvent.ConversationsTabPressed) {
                contactsListState.smartScrollToTop(coroutineScope)
            }
        }
    }

    // Derived state for selected contacts and count
    val selectedContacts =
        remember(contacts, selectedContactKeys) { contacts.filter { it.contactKey in selectedContactKeys } }
    // Get message count directly from repository for selected contacts
    var selectedCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(selectedContactKeys.size, selectedContactKeys.joinToString(",")) {
        selectedCount = viewModel.getTotalMessageCount(selectedContactKeys.toList())
    }
    var pendingSwipeDelete by remember { mutableStateOf<Contact?>(null) }
    LaunchedEffect(pendingSwipeDelete) {
        val contact = pendingSwipeDelete ?: return@LaunchedEffect
        selectedContactKeys.clear()
        selectedContactKeys.add(contact.contactKey)
        selectedCount = viewModel.getTotalMessageCount(listOf(contact.contactKey))
        showDeleteDialog = true
        pendingSwipeDelete = null
    }

    val isAllMuted = remember(selectedContacts) { selectedContacts.all { it.isMuted } }
    val isAllPinned =
        remember(selectedContacts) { selectedContacts.isNotEmpty() && selectedContacts.all { it.isPinned } }

    // Callback functions for item interaction
    val onContactClick: (Contact) -> Unit = { contact ->
        if (isSelectionModeActive) {
            // If in selection mode, toggle selection
            if (selectedContactKeys.contains(contact.contactKey)) {
                selectedContactKeys.remove(contact.contactKey)
            } else {
                selectedContactKeys.add(contact.contactKey)
            }
        } else {
            // If not in selection mode, navigate to messages
            onNavigateToMessages(contact.contactKey)
        }
    }

    val onNodeChipClick: (Contact) -> Unit = { contact ->
        if (contact.contactKey.contains("!")) {
            // if it's a node, look up the nodeNum including the !
            val nodeKey = ContactKey(contact.contactKey).addressString
            val node = viewModel.getNode(nodeKey)
            onNavigateToNodeDetails(node.num)
        } else {
            // Channels
        }
    }

    val onContactLongClick: (Contact) -> Unit = { contact ->
        // Enter selection mode and select the item on long press
        if (!isSelectionModeActive) {
            selectedContactKeys.add(contact.contactKey)
        } else {
            // If already in selection mode, toggle selection
            if (selectedContactKeys.contains(contact.contactKey)) {
                selectedContactKeys.remove(contact.contactKey)
            } else {
                selectedContactKeys.add(contact.contactKey)
            }
        }
    }

    val pangeaProfiles by pangeaViewModel.profileContacts.collectAsStateWithLifecycle()
    val chatIcons by pangeaViewModel.chatIcons.collectAsStateWithLifecycle()
    val pangeaGroups by pangeaViewModel.groups.collectAsStateWithLifecycle()
    val pangeaAvatars: (String) -> AvatarVisual? =
        remember(pangeaProfiles, chatIcons, pangeaGroups) {
            { key ->
                channelIconOf(key, pangeaViewModel, chatIcons)
                    ?: dmNodeNum(key)?.let { num ->
                        pangeaProfiles[num]?.let { profile ->
                            val photo = if (profile.savedPhotoVersion != 0L) pangeaViewModel.photo(num) else null
                            AvatarVisual(photo, profile.card?.emoji.orEmpty(), profile.card?.color)
                        }
                    }
            }
        }

    val pangeaExpert by pangeaViewModel.expertModeEnabled.collectAsStateWithLifecycle()
    val mutedLabel = stringResource(Res.string.contact_muted_snackbar)
    val undoLabel = stringResource(Res.string.undo)
    val toggleMute: (Contact) -> Unit = { contact ->
        val wasMuted = contact.isMuted
        viewModel.setMuteUntil(listOf(contact.contactKey), if (wasMuted) 0L else Long.MAX_VALUE)
        if (!wasMuted) {
            viewModel.showSnackbar(
                message = mutedLabel.replace("%1\$s", contact.longName),
                actionLabel = undoLabel,
                onAction = { viewModel.setMuteUntil(listOf(contact.contactKey), 0L) },
            )
        }
    }

    CompositionLocalProvider(LocalPangeaAvatars provides pangeaAvatars) {
        // Pangea standard mode: Chats as on the iPhone app. Expert Mode keeps the full mesh list below.
        if (!pangeaExpert) {
            IosChatsScreen(
                contacts = contacts,
                listState = contactsListState,
                onOpen = { onNavigateToMessages(it.contactKey) },
                onCompose = { showNewMessage = true },
                onComposeGroup = { showNewGroup = true },
                onNewGroup = { showNewGroup = true },
                onToggleMute = toggleMute,
                onTogglePin = { viewModel.setPinned(listOf(it.contactKey), !it.isPinned) },
                // Deleting drops the conversation's packets, so this only nominates it; the dialog confirms.
                onDelete = { pendingSwipeDelete = it },
                header = {
                    SetupProfilePrompt()
                    GroupInvitesSection(viewModel = pangeaViewModel, onJoin = onNavigateToMessages)
                },
            )
        } else {
            // Pangea Expert Mode: Messages -> Channels / Direct Messages, as on the iPhone app.
            val (channelChats, directChats) =
                remember(contacts) { contacts.partition { it.contactKey.endsWith(NodeAddress.ID_BROADCAST) } }
            val unreadCountTotal by viewModel.unreadCountTotal.collectAsStateWithLifecycle(0)
            when (section) {
                null ->
                    IosExpertMessagesScreen(
                        channelCount = channelChats.size,
                        unreadCount = unreadCountTotal,
                        connected = connectionState is ConnectionState.Connected,
                        onOpen = onOpenSection,
                        onCompose = { showNewMessage = true },
                        onMarkAllRead = viewModel::markAllAsRead,
                        onFilterSettings = onNavigateToFilterSettings,
                        onImport = { uriString ->
                            val onInvalid: () -> Unit = { scope.launch { showToast(Res.string.channel_invalid) } }
                            parseDeepLinkOrInvalid(uriString, onHandleDeepLink, onInvalid)
                        },
                        onShareChannels = onNavigateToShare,
                        header = {
                            SetupProfilePrompt()
                            GroupInvitesSection(viewModel = pangeaViewModel, onJoin = onNavigateToMessages)
                        },
                    )

                ExpertChatSection.CHANNELS,
                ExpertChatSection.DIRECT,
                -> {
                    val isChannels = section == ExpertChatSection.CHANNELS
                    IosChatsScreen(
                        contacts = if (isChannels) channelChats else directChats,
                        listState = contactsListState,
                        onOpen = { onNavigateToMessages(it.contactKey) },
                        onCompose = { showNewMessage = true },
                        onComposeGroup = { showNewGroup = true },
                        onNewGroup = { showNewGroup = true },
                        onToggleMute = toggleMute,
                        onTogglePin = { viewModel.setPinned(listOf(it.contactKey), !it.isPinned) },
                        onDelete = { pendingSwipeDelete = it },
                        title =
                        if (isChannels) {
                            stringResource(Res.string.channels)
                        } else {
                            stringResource(Res.string.pangea_contacts_count, directChats.size)
                        },
                        onBack = onBack,
                        showCompose = false,
                        expert = true,
                        channels = channels,
                        searchPlaceholder = if (isChannels) null else stringResource(Res.string.pangea_find_contact),
                    )
                }
            }
        }
    }

    PendingPangeaLink(
        viewModel = pangeaViewModel,
        onStartChat = { showNewMessage = true },
        onJoin = onNavigateToMessages,
    )

    if (showNewMessage) {
        NewMessageSheet(
            viewModel = pangeaViewModel,
            onOpenChat = { key ->
                showNewMessage = false
                onNavigateToMessages(key)
            },
            onDismiss = { showNewMessage = false },
        )
    }
    if (showNewGroup) {
        NewGroupDialog(
            viewModel = pangeaViewModel,
            onCreate = { key ->
                showNewGroup = false
                onNavigateToMessages(key)
            },
            onDismiss = { showNewGroup = false },
        )
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            selectedCount = selectedCount,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteContacts(selectedContactKeys.toList())
                selectedContactKeys.clear()
            },
        )
    }

    // Get contact settings for the dialog
    val contactSettings by viewModel.getContactSettings().collectAsStateWithLifecycle(initialValue = emptyMap())

    if (showMuteDialog) {
        MuteNotificationsDialog(
            selectedContactKeys = selectedContactKeys.toList(),
            contactSettings = contactSettings,
            onDismiss = { showMuteDialog = false },
            onConfirm = { muteUntil ->
                showMuteDialog = false
                viewModel.setMuteUntil(selectedContactKeys.toList(), muteUntil)
                selectedContactKeys.clear()
            },
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun MuteNotificationsDialog(
    selectedContactKeys: List<String>,
    contactSettings: Map<String, ContactSettings>,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit, // Lambda to handle the confirmed mute duration
) {
    // Options for mute duration
    val muteOptions = remember {
        listOf(
            Res.string.unmute to 0L,
            Res.string.mute_8_hours to TimeConstants.EIGHT_HOURS.inWholeMilliseconds,
            Res.string.mute_1_week to 7.days.inWholeMilliseconds,
            Res.string.mute_always to Long.MAX_VALUE,
        )
    }

    // State to hold the selected mute duration index
    var selectedOptionIndex by remember { mutableIntStateOf(2) } // Default to "Always"

    MeshtasticDialog(
        onDismiss = onDismiss, // Dismiss the dialog when clicked outside
        titleRes = Res.string.mute_notifications,
        confirmTextRes = Res.string.okay,
        onConfirm = {
            val selectedMuteDuration = muteOptions[selectedOptionIndex].second
            onConfirm(selectedMuteDuration)
            onDismiss() // Dismiss the dialog after confirming
        },
        dismissTextRes = Res.string.cancel,
        text = {
            Column {
                // Show current mute status
                selectedContactKeys.forEach { contactKey ->
                    contactSettings[contactKey]?.let { settings ->
                        val now = nowMillis
                        val statusText =
                            when {
                                settings.muteUntil > 0 && settings.muteUntil != Long.MAX_VALUE -> {
                                    val remaining = settings.muteUntil - now
                                    if (remaining > 0) {
                                        val (days, hours) = formatMuteRemainingTime(remaining)
                                        val hoursFormatted = NumberFormatter.format(hours, 1)
                                        if (days >= 1) {
                                            stringResource(Res.string.mute_status_muted_for_days, days, hoursFormatted)
                                        } else {
                                            stringResource(Res.string.mute_status_muted_for_hours, hoursFormatted)
                                        }
                                    } else {
                                        stringResource(Res.string.mute_status_unmuted)
                                    }
                                }

                                settings.muteUntil == Long.MAX_VALUE -> stringResource(Res.string.mute_status_always)

                                else -> stringResource(Res.string.mute_status_unmuted)
                            }
                        Text(
                            text = stringResource(Res.string.currently) + " " + statusText,
                            style = Ios.type.footnote,
                            color = Ios.colors.secondaryLabel,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }

                muteOptions.forEachIndexed { index, (stringRes, _) ->
                    val isSelected = index == selectedOptionIndex
                    val text = stringResource(stringRes)
                    Row(
                        modifier =
                        Modifier.fillMaxWidth()
                            .selectable(selected = isSelected, onClick = { selectedOptionIndex = index })
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = text,
                            style = Ios.type.body,
                            color = Ios.colors.label,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            Icon(
                                IosIcons.Checkmark,
                                contentDescription = null,
                                tint = Ios.colors.tint,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun DeleteConfirmationDialog(
    selectedCount: Int, // Number of items to be deleted
    onDismiss: () -> Unit,
    onConfirm: () -> Unit, // Lambda to handle the delete action
) {
    val deleteMessage =
        pluralStringResource(
            Res.plurals.delete_messages,
            selectedCount,
            selectedCount, // Pass the count as a format argument
        )

    MeshtasticTextDialog(
        titleRes = Res.string.are_you_sure,
        message = deleteMessage,
        confirmTextRes = Res.string.delete,
        onConfirm = {
            onConfirm()
            onDismiss() // Dismiss the dialog after confirming
        },
        onDismiss = onDismiss,
    )
}

/**
 * One-handed row actions: swipe toward the reading direction to mute, away from it to delete.
 *
 * The action fires from `onDismiss`, which the box raises once per settle. It must not fire from `confirmValueChange`:
 * that runs from the fling's `calculateSnapOffset`, so it is called once per frame — a single swipe fired mute nine
 * times, leaving the result decided by frame-count parity and stacking a snackbar per call. It is also deprecated in
 * `AnchoredDraggable`, which directs callers at the anchor set instead.
 *
 * `SwipeToDismissBox` disables its own gestures while the row is settled in a dismissed direction, so animating home is
 * what re-arms the next swipe rather than mere decoration.
 *
 * Mute is instant and undoable from a snackbar; delete is not, because [ContactsViewModel.deleteContacts] hard deletes
 * the conversation's packets with nothing to restore from, so it routes into the same confirmation dialog the selection
 * toolbar uses.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SwipeableContactRow(
    contact: Contact,
    enabled: Boolean,
    onMute: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    // These outlive a recomposition, so without them the callbacks would stay the ones captured the first time — and
    // they read the contact's mute state, which is exactly what a swipe changes.
    val currentOnMute by rememberUpdatedState(onMute)
    val currentOnDelete by rememberUpdatedState(onDelete)
    val dismissState = rememberSwipeToDismissBoxState()
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(dismissState) {
        // A tick the moment the swipe becomes eligible, so the row can be committed or dragged back without
        // watching it. `snapshotFlow` is distinct-until-changed, so re-crossing the threshold ticks again.
        snapshotFlow { dismissState.targetValue }
            .collect { target ->
                if (target != SwipeToDismissBoxValue.Settled) {
                    haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                }
            }
    }
    // Kept stable on purpose: the box raises `onDismiss` from a `LaunchedEffect` keyed on the lambda, so a fresh one
    // each recomposition would re-fire the action for a row that is still settled open.
    val onDismiss = remember {
        { direction: SwipeToDismissBoxValue ->
            when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> currentOnMute()
                SwipeToDismissBoxValue.EndToStart -> currentOnDelete()
                SwipeToDismissBoxValue.Settled -> Unit
            }
        }
    }
    LaunchedEffect(dismissState) {
        snapshotFlow { dismissState.settledValue }
            .collect { settled ->
                if (settled == SwipeToDismissBoxValue.Settled) return@collect
                // `AnchoredDraggableState` serialises mutations, so the first attempt can lose to the gesture's own
                // settle still holding the mutex; retrying wins uncontended rather than leaving the row stranded.
                while (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                    try {
                        dismissState.reset()
                    } catch (interrupted: CancellationException) {
                        // Ours to retry unless the effect itself is going away.
                        if (!currentCoroutineContext().isActive) throw interrupted
                    }
                }
            }
    }
    // The same two actions without the gesture: a swipe is unusable under a screen reader, and Material asks for a
    // non-swipe route to anything a swipe can reach.
    val muteLabel =
        stringResource(if (contact.isMuted) Res.string.swipe_action_unmute else Res.string.swipe_action_mute)
    val deleteLabel = stringResource(Res.string.swipe_action_delete)
    val rowActions =
        remember(muteLabel, deleteLabel) {
            listOf(
                CustomAccessibilityAction(muteLabel) {
                    currentOnMute()
                    true
                },
                CustomAccessibilityAction(deleteLabel) {
                    currentOnDelete()
                    true
                },
            )
        }
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeBackground(dismissState.dismissDirection, contact.isMuted) },
        modifier = Modifier.semantics { customActions = rowActions },
        gesturesEnabled = enabled,
        onDismiss = onDismiss,
        content = { content() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeBackground(direction: SwipeToDismissBoxValue, isMuted: Boolean) {
    val muting = direction == SwipeToDismissBoxValue.StartToEnd
    val color =
        when (direction) {
            SwipeToDismissBoxValue.StartToEnd -> Ios.colors.fieldFill
            SwipeToDismissBoxValue.EndToStart -> Ios.colors.systemRed.copy(alpha = 0.12f)
            SwipeToDismissBoxValue.Settled -> Color.Transparent
        }
    val contentColor =
        when (direction) {
            SwipeToDismissBoxValue.StartToEnd -> Ios.colors.label
            SwipeToDismissBoxValue.EndToStart -> Ios.colors.systemRed
            SwipeToDismissBoxValue.Settled -> Color.Transparent
        }
    val label =
        when {
            direction == SwipeToDismissBoxValue.EndToStart -> stringResource(Res.string.swipe_action_delete)
            isMuted -> stringResource(Res.string.swipe_action_unmute)
            else -> stringResource(Res.string.swipe_action_mute)
        }
    Row(
        modifier = Modifier.fillMaxSize().background(color).padding(horizontal = 20.dp),
        horizontalArrangement = if (muting) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (direction == SwipeToDismissBoxValue.Settled) return@Row
        Icon(
            imageVector =
            when {
                direction == SwipeToDismissBoxValue.EndToStart -> MeshtasticIcons.Delete
                isMuted -> MeshtasticIcons.VolumeUp
                else -> MeshtasticIcons.VolumeMute
            },
            contentDescription = label,
            tint = contentColor,
        )
    }
}

/** A group chat's or Everyone Nearby's own icon, from its conversation key ("2^all"). */
@Suppress("ReturnCount")
private fun channelIconOf(key: String, viewModel: PangeaChatsViewModel, icons: Map<String, ChatIcon>): AvatarVisual? {
    if (!key.endsWith(NodeAddress.ID_BROADCAST)) return null
    val index = key.removeSuffix(NodeAddress.ID_BROADCAST).toIntOrNull() ?: return null
    val icon = viewModel.chatIconKey(index)?.let(icons::get) ?: return null
    return AvatarVisual(photo = null, emoji = icon.emoji, color = icon.color)
}
