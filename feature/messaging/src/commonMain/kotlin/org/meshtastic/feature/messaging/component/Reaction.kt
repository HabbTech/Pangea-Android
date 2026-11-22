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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.Reaction
import org.meshtastic.core.model.getMessageStatusDetailRes
import org.meshtastic.core.model.getMessageStatusStringRes
import org.meshtastic.core.model.isAckProofForged
import org.meshtastic.core.model.isMessageStatusRetryable
import org.meshtastic.core.model.util.getShortDateTime
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.security_signed_verified
import org.meshtastic.core.resources.you
import org.meshtastic.core.ui.component.Rssi
import org.meshtastic.core.ui.component.Snr
import org.meshtastic.core.ui.icon.HopCount
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.ShieldCheck
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosBottomSheet
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.feature.messaging.DeliveryInfo

/**
 * One reaction as the iPhone app draws it under a message: the emoji over its sender's short name, at a fixed width so
 * the pill around a row of them can be sized without measuring.
 */
@Composable
internal fun ReactionItem(
    emoji: String,
    modifier: Modifier = Modifier,
    shortName: String = "",
    status: MessageStatus = MessageStatus.UNKNOWN,
    isWarning: Boolean = false,
) {
    val isSending = status == MessageStatus.QUEUED || status == MessageStatus.ENROUTE
    val nameColor =
        when {
            status == MessageStatus.ERROR -> Ios.colors.systemRed
            isWarning -> Ios.colors.systemOrange
            else -> Ios.colors.systemGray
        }
    Column(
        modifier =
        modifier.width(REACTION_ITEM_WIDTH).then(if (isSending) Modifier.graphicsLayer(alpha = 0.5f) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(text = emoji, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Clip)
        Text(text = shortName.ifEmpty { "?" }, style = Ios.type.caption2, color = nameColor, maxLines = 1)
    }
}

/**
 * The reactions under a message, as the iPhone app's pill: every reaction with who sent it, one row for a handful and
 * two once there are several, scrolling sideways rather than overflowing the column. A tap or long press opens who
 * reacted with what; reacting is in the message's actions, as on iOS.
 */
@Composable
internal fun ReactionRow(
    modifier: Modifier = Modifier,
    reactions: List<Reaction> = emptyList(),
    myId: String? = null,
    onShowReactions: () -> Unit = {},
) {
    AnimatedVisibility(reactions.isNotEmpty(), modifier = modifier) {
        val rows = if (reactions.size > TWO_ROW_THRESHOLD) 2 else 1
        val columns = (reactions.size + rows - 1) / rows
        val contentWidth =
            REACTION_ITEM_WIDTH * columns + REACTION_ITEM_SPACING * (columns - 1).coerceAtLeast(0) + PILL_PADDING * 2
        val pillHeight = REACTION_ROW_HEIGHT * rows + REACTION_ROW_SPACING * (rows - 1) + PILL_PADDING * 2
        LazyHorizontalGrid(
            rows = GridCells.Fixed(rows),
            modifier =
            Modifier.width(contentWidth)
                .height(pillHeight)
                .clip(PillShape)
                .border(1.dp, Ios.colors.systemGray, PillShape)
                .combinedClickable(onClick = onShowReactions, onLongClick = onShowReactions),
            contentPadding = PaddingValues(PILL_PADDING),
            horizontalArrangement = Arrangement.spacedBy(REACTION_ITEM_SPACING),
            verticalArrangement = Arrangement.spacedBy(REACTION_ROW_SPACING),
        ) {
            items(reactions, key = { "${it.user.id}:${it.emoji}:${it.timestamp}" }) { reaction ->
                val isLocal = reaction.user.id == NodeAddress.ID_LOCAL || reaction.user.id == myId
                ReactionItem(
                    emoji = reaction.emoji,
                    shortName = reaction.user.short_name,
                    status = if (isLocal) reaction.status else MessageStatus.RECEIVED,
                    isWarning = isLocal && isAckProofForged(reaction.ackProofStatus),
                    modifier = Modifier.height(REACTION_ROW_HEIGHT),
                )
            }
        }
    }
}

/**
 * Who reacted with what, as an iOS sheet: the emoji to filter by along the top, then a card with a row per reaction
 * (name, how it arrived and when, and the emoji at the end).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "CyclomaticComplexity", "CyclomaticComplexMethod")
@Composable
internal fun ReactionDialog(
    reactions: List<Reaction>,
    onDismiss: () -> Unit = {},
    myId: String? = null,
    /** False for an archived conversation: a failed reaction cannot be retried there. */
    canReact: Boolean = true,
    onResend: (Reaction) -> Unit = {},
) = IosBottomSheet(
    onDismissRequest = onDismiss,
    sheetState =
    rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
    ),
    grouped = true,
) {
    val groupedEmojis = reactions.groupBy { it.emoji }
    var selectedEmoji by remember { mutableStateOf<String?>(null) }
    val filteredReactions = selectedEmoji?.let { groupedEmojis[it] ?: emptyList() } ?: reactions

    var showStatusDialog by remember { mutableStateOf<Reaction?>(null) }
    showStatusDialog?.let { reaction ->
        val isDirectMessage = NodeAddress.fromString(reaction.to) !is NodeAddress.Broadcast
        val (title, text) =
            getMessageStatusStringRes(
                reaction.status,
                reaction.routingError,
                isDirectMessage,
                reaction.ackProofStatus,
            )

        DeliveryInfo(
            title = title,
            text = text,
            detail = getMessageStatusDetailRes(reaction.status, reaction.routingError),
            resendOption =
            canReact && isMessageStatusRetryable(reaction.status, reaction.routingError, isDirectMessage),
            onConfirm = {
                onResend(reaction)
                showStatusDialog = null
            },
            onDismiss = { showStatusDialog = null },
            relays = reaction.relays,
        )
    }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
    ) {
        items(groupedEmojis.entries.toList(), key = { it.key }) { entry ->
            val emoji = entry.key
            val emojiReactions = entry.value
            val localReaction = emojiReactions.find { it.user.id == NodeAddress.ID_LOCAL || it.user.id == myId }
            val isSending =
                localReaction?.status == MessageStatus.QUEUED || localReaction?.status == MessageStatus.ENROUTE
            Text(
                text = "$emoji ${emojiReactions.size}",
                modifier =
                Modifier.clip(CircleShape)
                    .background(if (selectedEmoji == emoji) Ios.colors.fieldFill else Ios.colors.groupedRow)
                    .then(if (isSending) Modifier.graphicsLayer(alpha = 0.5f) else Modifier)
                    .clickable { selectedEmoji = if (selectedEmoji == emoji) null else emoji }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                style = Ios.type.subheadline,
                color = Ios.colors.label,
            )
        }
    }

    LazyColumn(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            IosSection {
                filteredReactions.forEach { reaction ->
                    ReactionDetailRow(reaction, myId) { showStatusDialog = it }
                }
            }
        }
    }
}

@Composable
private fun ReactionDetailRow(reaction: Reaction, myId: String?, onStatusClick: (Reaction) -> Unit) {
    val isLocal = reaction.user.id == myId || reaction.user.id == NodeAddress.ID_LOCAL
    val displayName =
        if (isLocal) {
            "${reaction.user.long_name} (${stringResource(Res.string.you)})"
        } else {
            reaction.user.long_name
        }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = displayName,
                    modifier = Modifier.weight(1f, fill = false),
                    style = Ios.type.body,
                    color = Ios.colors.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Set only on verified broadcasts, so a DM reaction never shows it.
                if (!isLocal && reaction.xeddsaSigned) {
                    Icon(
                        imageVector = MeshtasticIcons.ShieldCheck,
                        contentDescription = stringResource(Res.string.security_signed_verified),
                        modifier = Modifier.size(14.dp),
                        tint = Ios.colors.systemGreen,
                    )
                }
            }
            ReactionArrival(reaction)
        }
        if (isLocal) {
            MessageStatusButton(
                status = reaction.status,
                fromLocal = true,
                isWarning = isAckProofForged(reaction.ackProofStatus),
                onStatusClick = { onStatusClick(reaction) },
            )
        }
        Text(text = reaction.emoji, fontSize = 26.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

/** How a reaction arrived (signal when heard directly, else its hop count) and when. */
@Composable
private fun ReactionArrival(reaction: Reaction) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 2.dp),
    ) {
        // Local reactions now carry a null rssi; pre-schema-51 rows stored 0, so a legacy
        // 0 dBm reading stays indistinguishable from "no reading" and remains hidden.
        val isLocalOrPreDbUpdateReaction = reaction.rssi == null || reaction.rssi == 0
        if (!isLocalOrPreDbUpdateReaction) {
            if (reaction.hopsAway == 0) {
                Snr(reaction.snr)
                Rssi(reaction.rssi)
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(
                        imageVector = MeshtasticIcons.HopCount,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Ios.colors.secondaryLabel,
                    )
                    Text(
                        text = reaction.hopsAway.toString(),
                        style = Ios.type.footnote,
                        color = Ios.colors.secondaryLabel,
                    )
                }
            }
        }
        Text(text = getShortDateTime(reaction.timestamp), style = Ios.type.footnote, color = Ios.colors.secondaryLabel)
    }
}

/** iOS's reaction pill geometry: fixed-width items, so the pill's width is arithmetic. */
private val REACTION_ITEM_WIDTH = 40.dp
private val REACTION_ITEM_SPACING = 12.dp
private val REACTION_ROW_HEIGHT = 38.dp
private val REACTION_ROW_SPACING = 4.dp
private val PILL_PADDING = 10.dp
private val PillShape = RoundedCornerShape(18.dp)

/** More reactions than this wrap onto a second row. */
private const val TWO_ROW_THRESHOLD = 6
