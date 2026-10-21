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
package org.meshtastic.feature.messaging.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isSensitiveData
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.model.Message
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.Reaction
import org.meshtastic.core.model.isAckProofForged
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.a11y_message_from
import org.meshtastic.core.resources.action_show_message_status
import org.meshtastic.core.resources.filter_message_label
import org.meshtastic.core.resources.message_translated_label
import org.meshtastic.core.resources.reply
import org.meshtastic.core.resources.security_signed_verified
import org.meshtastic.core.ui.component.AutoLinkText
import org.meshtastic.core.ui.component.HighlightedText
import org.meshtastic.core.ui.component.NODE_TINT_EMPHASIZED
import org.meshtastic.core.ui.component.Rssi
import org.meshtastic.core.ui.component.Snr
import org.meshtastic.core.ui.component.TransportIcon
import org.meshtastic.core.ui.component.nodeTintedContainer
import org.meshtastic.core.ui.emoji.EmojiPickerDialog
import org.meshtastic.core.ui.icon.FormatQuote
import org.meshtastic.core.ui.icon.HopCount
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Reply
import org.meshtastic.core.ui.icon.ShieldCheck
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosBottomSheet
import org.meshtastic.core.ui.theme.MessageItemColors
import org.meshtastic.core.ui.theme.StatusColors.StatusGreen
import org.meshtastic.core.ui.util.createClipEntry
import org.meshtastic.feature.messaging.pangea.IosChatAvatar
import org.meshtastic.feature.messaging.pangea.LocalPangeaPlainWording

internal const val MESSAGE_STATUS_LABEL_TEST_TAG = "message_status_label"

/** Selects the persisted compact label or formats the raw display instant with platform date/time conventions. */
internal fun formatMessageTimestamp(
    message: Message,
    showFullMessageTimestamp: Boolean,
    fullDateTimeFormatter: (Long) -> String = DateFormatter::formatDateTime,
): String = if (showFullMessageTimestamp) {
    fullDateTimeFormatter(message.displayTime)
} else {
    message.time
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun MessageItem(
    modifier: Modifier = Modifier,
    node: Node,
    ourNode: Node,
    message: Message,
    selected: Boolean,
    inSelectionMode: Boolean = false,
    onReply: () -> Unit = {},
    sendReaction: (String) -> Unit = {},
    onShowReactions: () -> Unit = {},
    showUserName: Boolean = true,
    showFullMessageTimestamp: Boolean = false,
    /** Android's signal, hops and route line under received bubbles; off by default, as the iPhone app has none. */
    showSignalDetails: Boolean = false,
    emojis: List<Reaction> = emptyList(),
    quickEmojis: List<String> = listOf("👍", "👎", "😂", "🔥", "❤️", "😮"),
    /** False for an archived conversation: existing reactions still render, but none can be added. */
    canReact: Boolean = true,
    /** False for an archived conversation: its channel is gone, so there is nothing to reply into. */
    canReply: Boolean = true,
    /**
     * Hoisted so the list can keep at most one bar open and close it on a tap anywhere else; owning it here would leave
     * every row's bar independent, and none of them able to see a tap outside their own bubble.
     */
    quickReactionsOpen: Boolean = false,
    onQuickReactionsOpenChange: (Boolean) -> Unit = {},
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onDoubleClick: () -> Unit = {},
    onSelect: () -> Unit = {},
    onDelete: () -> Unit = {},
    onClickChip: (Node) -> Unit = {},
    resolveMention: (String) -> Node? = { null },
    onNavigateToOriginalMessage: (Int) -> Unit = {},
    onStatusClick: () -> Unit = {},
    hasSamePrev: Boolean = false,
    hasSameNext: Boolean = false,
    searchQuery: String = "",
    translationAvailable: Boolean = false,
    isDirectMessage: Boolean = false,
    onTranslate: () -> Unit = {},
    onToggleTranslation: () -> Unit = {},
) = Column(
    modifier =
    modifier
        .fillMaxWidth()
        // Wider gap between sender groups, tight spacing within a group, so runs read as one unit.
        // The gap between groups is the iPhone app's.
        .padding(
            top =
            if (hasSamePrev) {
                4.dp
            } else {
                28.dp
            },
        ),
) {
    var activeSheet by remember { mutableStateOf<ActiveSheet?>(null) }
    val clipboardManager = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val sheetState =
        rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        )
    val isLocal = node.num == ourNode.num
    val timestamp = formatMessageTimestamp(message, showFullMessageTimestamp)
    val statusString = message.getStatusStringRes(isDirectMessage)
    val isDirectImplicitAck = message.status == MessageStatus.DELIVERED && isDirectMessage
    val isRetryableFailure = message.status == MessageStatus.ERROR && message.isStatusRetryable(isDirectMessage)
    val isForgedAck = isAckProofForged(message.ackProofStatus)
    // While searching, always show the original text — FTS matches and highlights apply to it, not the translation.
    val showsTranslation = message.showTranslated && message.translatedText != null && searchQuery.isEmpty()
    val bodyText = message.displayedText(searching = searchQuery.isNotEmpty())
    if (activeSheet != null) {
        IosBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = sheetState,
            grouped = activeSheet == ActiveSheet.Actions,
        ) {
            when (activeSheet) {
                ActiveSheet.Actions -> {
                    MessageActionsContent(
                        quickEmojis = if (canReact) quickEmojis else emptyList(),
                        canReply = canReply,
                        onReply = {
                            activeSheet = null
                            onReply()
                        },
                        onReact = { emoji ->
                            activeSheet = null
                            sendReaction(emoji)
                        },
                        onMoreReactions = { activeSheet = ActiveSheet.Emoji },
                        onCopy = {
                            activeSheet = null
                            coroutineScope.launch {
                                clipboardManager.setClipEntry(createClipEntry(bodyText, "message"))
                            }
                        },
                        onSelect = {
                            activeSheet = null
                            onSelect()
                        },
                        onDelete = {
                            activeSheet = null
                            onDelete()
                        },
                        statusString = statusString,
                        status =
                        if (isLocal) {
                            message.status
                        } else {
                            null
                        },
                        // Mesh time (rx_time), matching the bubble header. receivedTime is when this phone
                        // pulled the packet off the node, which is misleading after an offline backlog sync.
                        timestamp = timestamp,
                        xeddsaSigned = message.xeddsaSigned,
                        ackProofStatus = message.ackProofStatus,
                        onStatus = onStatusClick,
                        translationRowState = translationRowStateFor(message, translationAvailable),
                        onTranslate = {
                            activeSheet = null
                            onTranslate()
                        },
                        onToggleTranslation = {
                            activeSheet = null
                            onToggleTranslation()
                        },
                    )
                }

                ActiveSheet.Emoji -> {
                    EmojiPickerDialog(
                        onDismiss = { activeSheet = null },
                        onConfirm = { emoji ->
                            activeSheet = null
                            sendReaction(emoji)
                        },
                    )
                }

                null -> {}
            }
        }
    }

    val containsBel = message.text.contains('\u0007')

    // Pangea: bubbles as on the iPhone app — theirs grey on the left beside their avatar with their name above,
    // yours orange on the right with white text and the delivery line under it. Expert Mode adds the mesh details.
    val ios = Ios.colors
    val plainWording = LocalPangeaPlainWording.current
    val containerColor = if (message.fromLocal) ios.tint else ios.incomingBubble
    val contentColor = if (message.fromLocal) ios.onTint else ios.label
    val metadataStyle = Ios.type.caption1
    val metadataColor = if (message.fromLocal) ios.onTint.copy(alpha = 0.8f) else ios.secondaryLabel
    val messageShape = RoundedCornerShape(BUBBLE_CORNER)
    val cardBorder =
        when {
            selected -> BorderStroke(2.dp, ios.systemBlue)
            containsBel -> BorderStroke(2.dp, MessageItemColors.Red)
            else -> null
        }
    val dimmed = message.filtered || (inSelectionMode && !selected)
    val senderName = if (message.fromLocal) ourNode.user.long_name else node.user.long_name
    val messageA11yText = stringResource(Res.string.a11y_message_from, senderName, bodyText)
    if (showUserName && !message.fromLocal) {
        Row(
            modifier = Modifier.padding(start = BUBBLE_TEXT_START, end = 16.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Expert Mode names the sender with their node ID, as iOS does ("Madrona Tower (!0a014911)"). The time
            // shows only when "Show full message timestamps" asks for it.
            Text(
                text = if (plainWording) node.user.long_name else "${node.user.long_name} (${node.user.id})",
                modifier = Modifier.weight(1f, fill = false),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = Ios.type.footnote,
                color = ios.secondaryLabel,
            )
            if (!plainWording && showFullMessageTimestamp) {
                Text(text = timestamp, style = Ios.type.footnote, color = ios.secondaryLabel)
            }
        }
    } else if (showUserName && !plainWording && showFullMessageTimestamp) {
        Text(
            text = timestamp,
            modifier = Modifier.align(Alignment.End).padding(end = 16.dp, bottom = 3.dp),
            style = Ios.type.footnote,
            color = ios.secondaryLabel,
        )
    }
    // Reactions live on the bubble, where the eye already is, instead of behind a modal that covers the
    // conversation. Reached by long press or double tap; neither sends anything on its own, because a reaction is a
    // real packet on a duty-cycled radio. The overflow button keeps the full actions sheet one tap away.
    AnimatedVisibility(
        // Stays available on an archived conversation even though its emoji are gone: this bar carries the only
        // route to the actions sheet, and copy, select, delete and status all still apply to archived messages.
        visible = quickReactionsOpen && !inSelectionMode,
        modifier = Modifier.align(if (message.fromLocal) Alignment.End else Alignment.Start),
    ) {
        Surface(
            modifier =
            Modifier.padding(
                start = if (message.fromLocal) 16.dp else BUBBLE_TEXT_START,
                end = 16.dp,
                top = 2.dp,
                bottom = 2.dp,
            ),
            shape = RoundedCornerShape(QUICK_REACTION_BAR_CORNER),
            color = ios.groupedRow,
            shadowElevation = 4.dp,
        ) {
            QuickEmojiRow(
                quickEmojis = quickEmojis,
                onReact = { emoji ->
                    onQuickReactionsOpenChange(false)
                    sendReaction(emoji)
                },
                onMoreReactions = {
                    onQuickReactionsOpenChange(false)
                    activeSheet = ActiveSheet.Emoji
                },
                onMoreActions = {
                    onQuickReactionsOpenChange(false)
                    activeSheet = ActiveSheet.Actions
                },
            )
        }
    }

    val haptics = LocalHapticFeedback.current
    val swipeThresholdPx = with(LocalDensity.current) { REPLY_SWIPE_THRESHOLD.toPx() }
    // Drag toward the reading direction, so the gesture reads the same way under RTL.
    val swipeSign = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    val swipeOffset = remember(message.uuid) { Animatable(0f) }
    var swipeArmed by remember(message.uuid) { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        // An archived conversation cannot send, so the swipe neither renders nor arms. Left in place it would buzz
        // at the threshold and then drop the reply silently.
        if (canReply) {
            ReplySwipeAffordance(
                progress = (swipeOffset.value * swipeSign / swipeThresholdPx).coerceIn(0f, 1f),
                modifier = Modifier.align(if (message.fromLocal) Alignment.CenterEnd else Alignment.CenterStart),
            )
        }
        Row(
            modifier =
            Modifier.align(if (message.fromLocal) Alignment.CenterEnd else Alignment.CenterStart)
                .graphicsLayer {
                    translationX = swipeOffset.value
                    alpha = if (dimmed) DIMMED_ALPHA else 1f
                }
                .padding(
                    start = if (message.fromLocal) BUBBLE_FAR_SIDE else 16.dp,
                    end = if (message.fromLocal) 16.dp else BUBBLE_FAR_SIDE_INCOMING,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!message.fromLocal) {
                // The avatar sits beside the last bubble of a run, as on iOS; earlier bubbles keep its space.
                if (hasSameNext) {
                    Spacer(Modifier.size(AVATAR_SIZE))
                } else {
                    IosChatAvatar(
                        contactKey = "${NodeAddress.PKC_CHANNEL_INDEX}${node.user.id}",
                        shortName = node.user.short_name,
                        longName = node.user.long_name,
                        nodeColor = Color(node.colors.second),
                        size = AVATAR_SIZE,
                        modifier = Modifier.clickable { onClickChip(node) },
                    )
                }
                Spacer(Modifier.width(AVATAR_GAP))
            }
            Surface(
                modifier =
                Modifier.pointerInput(message.uuid, inSelectionMode, canReply) {
                    if (inSelectionMode || !canReply) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val reached = swipeOffset.value * swipeSign >= swipeThresholdPx
                            swipeArmed = false
                            // Fire before animating home: Animatable serializes mutations, so a fresh drag
                            // would
                            // cancel the settle coroutine and take the reply with it.
                            if (reached) onReply()
                            coroutineScope.launch { swipeOffset.animateTo(0f) }
                        },
                        onDragCancel = {
                            swipeArmed = false
                            coroutineScope.launch { swipeOffset.animateTo(0f) }
                        },
                    ) { change, dragAmount ->
                        // detectHorizontalDragGestures already applies horizontal touch slop, so this tests
                        // direction, not axis: claim the gesture only while dragging toward reply, so a
                        // back-swipe
                        // still reaches whatever is behind.
                        val next = (swipeOffset.value + dragAmount) * swipeSign
                        if (next > 0f) change.consume()
                        val clamped = next.coerceIn(0f, swipeThresholdPx * REPLY_SWIPE_OVERDRAG) * swipeSign
                        coroutineScope.launch { swipeOffset.snapTo(clamped) }
                        val reached = clamped * swipeSign >= swipeThresholdPx
                        if (reached != swipeArmed) {
                            swipeArmed = reached
                            if (reached) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                }
                    .widthIn(max = 480.dp)
                    .combinedClickable(
                        onClick = { if (quickReactionsOpen) onQuickReactionsOpenChange(false) else onClick() },
                        onLongClick = {
                            onLongClick()
                            if (!inSelectionMode) {
                                onQuickReactionsOpenChange(true)
                            }
                        },
                        onDoubleClick = {
                            onDoubleClick()
                            if (!inSelectionMode) onQuickReactionsOpenChange(!quickReactionsOpen)
                        },
                    )
                    .semantics(mergeDescendants = true) {
                        contentDescription = messageA11yText
                        role = Role.Button
                        isSensitiveData = true
                    },
                color = containerColor,
                contentColor = contentColor,
                shape = messageShape,
                border = cardBorder,
            ) {
                Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                    OriginalMessageSnippet(
                        modifier = Modifier.fillMaxWidth(),
                        message = message,
                        ourNode = ourNode,
                        onNavigateToOriginalMessage = onNavigateToOriginalMessage,
                    )

                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        if (searchQuery.isNotEmpty()) {
                            HighlightedText(
                                text = message.text,
                                query = searchQuery,
                                style = Ios.type.body,
                                color = contentColor,
                            )
                        } else {
                            val mentionDisplayName =
                                remember(resolveMention) {
                                    { id: String ->
                                        resolveMention(id)?.let { it.user.long_name.ifEmpty { it.user.short_name } }
                                    }
                                }
                            AutoLinkText(
                                text = bodyText,
                                style = Ios.type.body,
                                color = contentColor,
                                mentionName = mentionDisplayName,
                                onMentionClick = { id -> resolveMention(id)?.let(onClickChip) },
                            )
                        }

                        // iOS keeps a message's signal, hops and signature in its details; Android's line under the
                        // bubble is an Expert Mode option.
                        val showDiagnostics = !message.fromLocal && !plainWording && showSignalDetails
                        if (showDiagnostics || containsBel || message.filtered || showsTranslation) {
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                if (showDiagnostics) {
                                    // All mesh diagnostics (signature, signal or hops, transport) grouped in one
                                    // compact run.
                                    DiagnosticsRow {
                                        // XEdDSA is only set on verified broadcasts, never DMs.
                                        if (message.xeddsaSigned) {
                                            Icon(
                                                imageVector = MeshtasticIcons.ShieldCheck,
                                                contentDescription =
                                                stringResource(Res.string.security_signed_verified),
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.StatusGreen,
                                            )
                                        }
                                        TransportIcon(
                                            transport = message.transportMechanism,
                                            viaMqtt = message.viaMqtt,
                                            modifier = Modifier.size(14.dp),
                                            tint = metadataColor,
                                        )
                                        if (message.hopsAway == 0 && !message.viaMqtt) {
                                            Snr(message.snr)
                                            Rssi(message.rssi)
                                        } else {
                                            Icon(
                                                imageVector = MeshtasticIcons.HopCount,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = metadataColor,
                                            )
                                            Text(
                                                text =
                                                if (message.hopsAway >= 0) message.hopsAway.toString() else "?",
                                                style = metadataStyle,
                                                color = metadataColor,
                                            )
                                        }
                                    }
                                }
                                if (containsBel) {
                                    Text(text = "🔔")
                                }
                                if (message.filtered) {
                                    Text(
                                        text = stringResource(Res.string.filter_message_label),
                                        style = metadataStyle,
                                        color = metadataColor,
                                    )
                                }
                                if (showsTranslation) {
                                    Text(
                                        text = stringResource(Res.string.message_translated_label),
                                        style = metadataStyle,
                                        color = metadataColor,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (message.fromLocal) {
        // The delivery line sits under your bubble, in plain words, as on iOS.
        Box(Modifier.align(Alignment.End).padding(end = 16.dp, top = 4.dp)) {
            MessageStatusLabel(
                status = message.status ?: MessageStatus.UNKNOWN,
                text = stringResource(statusString.second),
                metadataStyle = Ios.type.footnote,
                isWarning = isDirectImplicitAck || isRetryableFailure || isForgedAck,
                onStatusClick = onStatusClick,
            )
        }
    }

    ReactionRow(
        modifier =
        Modifier.align(if (message.fromLocal) Alignment.End else Alignment.Start)
            .padding(
                top = 2.dp,
                start = if (!message.fromLocal) BUBBLE_TEXT_START else BUBBLE_FAR_SIDE,
                end = 16.dp,
            ),
        reactions = if (message.fromLocal) emojis.reversed() else emojis,
        myId = ourNode.user.id,
        onShowReactions = onShowReactions,
    )
}

/** iOS bubble geometry, measured on the Pangea iPhone app. */
private val BUBBLE_CORNER = 20.dp
private val AVATAR_SIZE = 46.dp
private val AVATAR_GAP = 8.dp

/** Where incoming bubbles (and the sender's name above them) start: margin + avatar + gap. */
private val BUBBLE_TEXT_START = 16.dp + AVATAR_SIZE + AVATAR_GAP

/** Space kept free on the far side of a bubble, so a long message never spans the screen. */
private val BUBBLE_FAR_SIDE = 56.dp
private val BUBBLE_FAR_SIDE_INCOMING = 40.dp

/** Filtered messages, and unselected ones while selecting, fade back. */
private const val DIMMED_ALPHA = 0.5f

/** Drag distance at which a horizontal swipe on a bubble commits to a reply. */
private val REPLY_SWIPE_THRESHOLD = 64.dp

/** How far past the threshold the bubble may still be dragged, so the gesture has some give. */
private const val REPLY_SWIPE_OVERDRAG = 1.5f

private val QUICK_REACTION_BAR_CORNER = 20.dp

/** Reply arrow revealed behind a bubble as it is dragged; [progress] runs 0..1 up to the commit threshold. */
@Composable
private fun ReplySwipeAffordance(progress: Float, modifier: Modifier = Modifier) {
    if (progress <= 0f) return
    Icon(
        imageVector = MeshtasticIcons.Reply,
        contentDescription = null,
        modifier = modifier.padding(horizontal = 14.dp).size(20.dp).alpha(progress),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private enum class ActiveSheet {
    Actions,
    Emoji,
}

internal val MessageStatusColorKey = SemanticsPropertyKey<Color>("MessageStatusColor")

/** Row grouping a received message's mesh diagnostics (signature, signal or hops, transport). */
@Composable
private fun DiagnosticsRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

@Composable
private fun MessageStatusLabel(
    status: MessageStatus,
    text: String,
    metadataStyle: TextStyle,
    isWarning: Boolean,
    onStatusClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusColor = messageStatusColor(status, isWarning = isWarning)
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .testTag(MESSAGE_STATUS_LABEL_TEST_TAG)
            .semantics { this[MessageStatusColorKey] = statusColor }
            .clickable(
                onClickLabel = stringResource(Res.string.action_show_message_status),
                role = Role.Button,
                onClick = onStatusClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        MessageStatusIcon(
            status = status,
            modifier = Modifier.size(14.dp),
            tint = statusColor,
            includeContentDescription = false,
        )
        Text(
            text = text,
            modifier = Modifier.weight(1f, fill = false),
            style = metadataStyle,
            color = statusColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun translationRowStateFor(message: Message, translationAvailable: Boolean): TranslationRowState? = when {
    // Toggling a persisted translation is just a DB flag flip — offer it even when
    // the translation engine is no longer available for the current locale.
    message.translatedText != null ->
        if (message.showTranslated) TranslationRowState.ShowOriginal else TranslationRowState.ShowTranslation

    !translationAvailable || message.text.isBlank() -> null

    else -> TranslationRowState.Translate
}

@Composable
private fun OriginalMessageSnippet(
    message: Message,
    ourNode: Node,
    onNavigateToOriginalMessage: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val originalMessage = message.originalMessage
    if (originalMessage != null && originalMessage.packetId != 0) {
        val originalMessageNode = if (originalMessage.fromLocal) ourNode else originalMessage.node
        // Same node-tinted treatment as the bubble (keeps onSurface text AA), but at the emphasized tint so the quoted
        // header still reads as distinct from the bubble body below it.
        val replyContainerColor =
            nodeTintedContainer(Color(originalMessageNode.colors.second), fraction = NODE_TINT_EMPHASIZED)
        val replyContentColor = MaterialTheme.colorScheme.onSurface
        // Rectangle shape — the outer message bubble's Surface clips to its
        // rounded corners, so the reply header inherits the correct top radii
        // automatically and stays square on the bottom where body text follows.
        Surface(
            modifier =
            modifier
                .fillMaxWidth()
                .clickable { onNavigateToOriginalMessage(originalMessage.packetId) }
                // clickable makes this its own a11y node, so the bubble's isSensitiveData doesn't reach it
                .semantics { isSensitiveData = true },
            contentColor = replyContentColor,
            color = replyContainerColor,
            shape = RectangleShape,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    MeshtasticIcons.FormatQuote,
                    contentDescription = stringResource(Res.string.reply),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = originalMessageNode.user.short_name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = originalMessage.text,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
