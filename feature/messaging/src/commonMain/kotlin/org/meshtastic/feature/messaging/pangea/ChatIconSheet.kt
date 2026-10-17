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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.pangea.ChatIcon
import org.meshtastic.core.model.pangea.EmojiCatalog
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.pangea_all_emoji
import org.meshtastic.core.resources.pangea_chat_icon
import org.meshtastic.core.resources.pangea_chat_icon_hint
import org.meshtastic.core.resources.pangea_use_symbol
import org.meshtastic.core.resources.save
import org.meshtastic.core.ui.component.AvatarPalette
import org.meshtastic.core.ui.component.PangeaAvatar
import org.meshtastic.core.ui.component.PangeaEmojiPicker

/**
 * Pick the emoji and colour a group chat or Everyone Nearby shows, kept on this device (Pangea-Apple ChatIconSheet).
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatIconSheet(title: String, current: ChatIcon?, onSave: (ChatIcon?) -> Unit, onDismiss: () -> Unit) {
    var emoji by remember { mutableStateOf(current?.emoji.orEmpty()) }
    var color by remember { mutableStateOf(current?.color ?: 0) }
    var showAll by remember { mutableStateOf(false) }
    // Fully open, so Save is on screen rather than under a half-height sheet.
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState =
        rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
    ) {
        Column(
            modifier =
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.pangea_chat_icon), style = MaterialTheme.typography.titleLarge)
            PangeaAvatar(
                photo = null,
                emoji = emoji,
                colorIndex = color,
                initials = title.take(1),
                fallbackColor = AvatarPalette.color(color),
                size = 96.dp,
            )
            Text(
                stringResource(Res.string.pangea_chat_icon_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Wraps to the screen width; a fixed eight per row ran off a phone's edge.
            FlowRow(horizontalArrangement = Arrangement.Center) {
                EmojiCatalog.suggestedEmoji.forEach { item ->
                    val ring = if (item == emoji) MaterialTheme.colorScheme.primary else Color.Transparent
                    Text(
                        item,
                        fontSize = 24.sp,
                        modifier =
                        Modifier.padding(4.dp)
                            .clip(CircleShape)
                            .border(2.dp, ring, CircleShape)
                            .clickable { emoji = item }
                            .padding(6.dp),
                    )
                }
            }
            TextButton(onClick = { showAll = true }) { Text(stringResource(Res.string.pangea_all_emoji)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AvatarPalette.colors.forEachIndexed { index, swatch ->
                    val ring = if (index == color) MaterialTheme.colorScheme.onSurface else Color.Transparent
                    Box(
                        modifier =
                        Modifier.size(34.dp).clip(CircleShape).border(3.dp, ring, CircleShape).clickable {
                            color = index
                        },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(24.dp).clip(CircleShape).background(swatch))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (current != null) {
                    TextButton(onClick = { onSave(null) }) { Text(stringResource(Res.string.pangea_use_symbol)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
                Button(onClick = { onSave(ChatIcon(emoji, color)) }, enabled = emoji.isNotEmpty()) {
                    Text(stringResource(Res.string.save))
                }
            }
        }
    }
    if (showAll) {
        PangeaEmojiPicker(
            onPick = {
                emoji = it
                showAll = false
            },
            onDismiss = { showAll = false },
        )
    }
}
