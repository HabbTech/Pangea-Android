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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.core.model.getAckProofStatusRes
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.action_more_message_actions
import org.meshtastic.core.resources.action_react_with_emoji
import org.meshtastic.core.resources.copy
import org.meshtastic.core.resources.delete
import org.meshtastic.core.resources.device_metrics_label_value
import org.meshtastic.core.resources.message_delivery_status
import org.meshtastic.core.resources.more_reactions
import org.meshtastic.core.resources.reply
import org.meshtastic.core.resources.security_signed_message_info
import org.meshtastic.core.resources.security_signed_verified
import org.meshtastic.core.resources.select
import org.meshtastic.core.resources.show_original
import org.meshtastic.core.resources.show_translation
import org.meshtastic.core.resources.timestamp
import org.meshtastic.core.resources.translate
import org.meshtastic.core.ui.icon.AddReaction
import org.meshtastic.core.ui.icon.Copy
import org.meshtastic.core.ui.icon.Dangerous
import org.meshtastic.core.ui.icon.Delete
import org.meshtastic.core.ui.icon.History
import org.meshtastic.core.ui.icon.KeyOff
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.More
import org.meshtastic.core.ui.icon.Reply
import org.meshtastic.core.ui.icon.SelectAll
import org.meshtastic.core.ui.icon.ShieldCheck
import org.meshtastic.core.ui.icon.Translate
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosAccessory
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.proto.MeshPacket

@Suppress("LongMethod")
@Composable
fun MessageActionsContent(
    quickEmojis: List<String>,
    onReply: () -> Unit,
    onReact: (String) -> Unit,
    onMoreReactions: () -> Unit,
    onCopy: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onStatus: () -> Unit,
    /** False for an archived conversation: there is no channel left to reply into. */
    canReply: Boolean = true,
    statusString: Pair<StringResource, StringResource>? = null,
    status: MessageStatus? = null,
    timestamp: String? = null,
    xeddsaSigned: Boolean = false,
    ackProofStatus: Int = 0,
    translationRowState: TranslationRowState? = null,
    onTranslate: () -> Unit = {},
    onToggleTranslation: () -> Unit = {},
) {
    val c = Ios.colors
    Column(Modifier.padding(bottom = 24.dp)) {
        // An archived conversation shows no reaction affordances: the send path refuses them, so offering the row
        // would be a control that silently does nothing.
        if (quickEmojis.isNotEmpty()) {
            QuickEmojiRow(quickEmojis = quickEmojis, onReact = onReact, onMoreReactions = onMoreReactions)
        }

        // What is known about the message, as one card.
        val hasInfo =
            xeddsaSigned || getAckProofStatusRes(ackProofStatus) != null || timestamp != null || status != null
        if (hasInfo) {
            IosSection {
                if (xeddsaSigned) {
                    IosRow(
                        title = stringResource(Res.string.security_signed_verified),
                        subtitle = stringResource(Res.string.security_signed_message_info),
                        icon = MeshtasticIcons.ShieldCheck,
                    )
                }
                AckProofRow(ackProofStatus)
                // The caller supplies the same compact or full timestamp shown in the conversation header.
                if (timestamp != null) {
                    IosRow(
                        title =
                        stringResource(
                            Res.string.device_metrics_label_value,
                            stringResource(Res.string.timestamp),
                            timestamp,
                        ),
                        icon = MeshtasticIcons.History,
                        iconTint = c.secondaryLabel,
                    )
                }
                if (status != null) {
                    val title =
                        statusString?.first?.let { stringResource(it) }
                            ?: stringResource(Res.string.message_delivery_status)
                    val statusText = statusString?.second?.let { stringResource(it) }
                    IosRow(
                        title = stringResource(Res.string.device_metrics_label_value, title, statusText.orEmpty()),
                        leading = { MessageStatusIcon(status = status) },
                        accessory = IosAccessory.Chevron,
                        onClick = onStatus,
                    )
                }
            }
        }

        // The actions, as iOS's menu lists them, Delete last and in red.
        IosSection {
            if (canReply) {
                IosRow(title = stringResource(Res.string.reply), icon = MeshtasticIcons.Reply, onClick = onReply)
            }
            IosRow(title = stringResource(Res.string.copy), icon = MeshtasticIcons.Copy, onClick = onCopy)
            if (translationRowState != null) {
                val headline =
                    when (translationRowState) {
                        TranslationRowState.Translate -> stringResource(Res.string.translate)
                        TranslationRowState.ShowOriginal -> stringResource(Res.string.show_original)
                        TranslationRowState.ShowTranslation -> stringResource(Res.string.show_translation)
                    }
                IosRow(
                    title = headline,
                    icon = MeshtasticIcons.Translate,
                    onClick =
                    if (translationRowState == TranslationRowState.Translate) onTranslate else onToggleTranslation,
                )
            }
            IosRow(title = stringResource(Res.string.select), icon = MeshtasticIcons.SelectAll, onClick = onSelect)
            IosRow(
                title = stringResource(Res.string.delete),
                icon = MeshtasticIcons.Delete,
                iconTint = c.systemRed,
                titleColor = c.systemRed,
                onClick = onDelete,
            )
        }
    }
}

/**
 * The radio's verdict on the ack that closed out this message. Silent when no proof was carried, which is every ack
 * from firmware predating [MeshPacket.AckProofStatus] and every ack that simply did not carry one.
 */
@Composable
private fun AckProofRow(ackProofStatus: Int) {
    val (headline, supporting) = getAckProofStatusRes(ackProofStatus) ?: return
    val proofStatus = MeshPacket.AckProofStatus.fromValue(ackProofStatus)
    val icon =
        when (proofStatus) {
            MeshPacket.AckProofStatus.ACK_PROOF_INVALID -> MeshtasticIcons.Dangerous
            MeshPacket.AckProofStatus.ACK_PROOF_NO_KEY -> MeshtasticIcons.KeyOff
            else -> MeshtasticIcons.ShieldCheck
        }
    val tint =
        when (proofStatus) {
            MeshPacket.AckProofStatus.ACK_PROOF_INVALID -> Ios.colors.systemRed
            MeshPacket.AckProofStatus.ACK_PROOF_NO_KEY -> Ios.colors.secondaryLabel
            else -> Ios.colors.tint
        }
    IosRow(title = stringResource(headline), subtitle = stringResource(supporting), icon = icon, iconTint = tint)
}

internal const val MAX_EMOJI_ROW_SIZE = 6

/**
 * Six one-tap reactions plus the full picker. [onMoreActions], when supplied, appends an overflow button — the inline
 * bar uses it to reach the actions sheet, which is where the row itself lives when opened the other way round.
 */
@Composable
internal fun QuickEmojiRow(
    quickEmojis: List<String>,
    onReact: (String) -> Unit,
    onMoreReactions: () -> Unit,
    onMoreActions: (() -> Unit)? = null,
) {
    Row(
        // Scrollable so seven 44dp touch targets never clip on narrow (320dp) sheets.
        modifier =
        Modifier.fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        quickEmojis.take(MAX_EMOJI_ROW_SIZE).forEach { emoji ->
            Box(
                modifier =
                Modifier.defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                    .clip(CircleShape)
                    .background(Ios.colors.fieldFill)
                    .clickable(
                        onClickLabel = stringResource(Res.string.action_react_with_emoji),
                        role = Role.Button,
                    ) {
                        onReact(emoji)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, style = Ios.type.headline)
            }
        }

        IconButton(
            onClick = onMoreReactions,
            modifier = Modifier.size(44.dp).background(Ios.colors.fieldFill, CircleShape),
        ) {
            Icon(
                MeshtasticIcons.AddReaction,
                contentDescription = stringResource(Res.string.more_reactions),
                modifier = Modifier.size(20.dp),
                tint = Ios.colors.secondaryLabel,
            )
        }

        if (onMoreActions != null) {
            IconButton(
                onClick = onMoreActions,
                modifier = Modifier.size(44.dp).background(Ios.colors.fieldFill, CircleShape),
            ) {
                Icon(
                    MeshtasticIcons.More,
                    contentDescription = stringResource(Res.string.action_more_message_actions),
                    modifier = Modifier.size(20.dp),
                    tint = Ios.colors.secondaryLabel,
                )
            }
        }
    }
}
