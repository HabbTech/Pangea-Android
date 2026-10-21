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
package org.meshtastic.core.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.meshtastic.core.model.pangea.EmojiCatalog
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosBottomSheet

/**
 * Every emoji a Pangea profile or chat icon can carry, grouped as on iOS. Tapping an emoji picks it; holding one with
 * skin tones offers them (Pangea-Apple "Tapping an emoji picks it; hold for skin tones").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PangeaEmojiPicker(onPick: (String) -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    IosBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState =
        rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = CELL),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        ) {
            EmojiCatalog.groups.forEach { group ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        group.name,
                        style = Ios.type.subheadline,
                        modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
                    )
                }
                items(group.emoji, key = { it.emoji }) { item -> EmojiCell(item, onPick) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EmojiCell(item: EmojiCatalog.Emoji, onPick: (String) -> Unit) {
    var showTones by remember { mutableStateOf(false) }
    Box(
        contentAlignment = Alignment.Center,
        modifier =
        Modifier.size(CELL)
            .combinedClickable(
                onClick = { onPick(item.emoji) },
                onLongClick = { if (item.variants.isNotEmpty()) showTones = true },
            ),
    ) {
        Text(item.emoji, fontSize = EMOJI_SIZE.sp)
        DropdownMenu(expanded = showTones, onDismissRequest = { showTones = false }) {
            Row {
                (listOf(item.emoji) + item.variants).forEach { variant ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                        Modifier.size(CELL).clickable {
                            showTones = false
                            onPick(variant)
                        },
                    ) {
                        Text(variant, fontSize = EMOJI_SIZE.sp)
                    }
                }
            }
        }
    }
}

private val CELL = 44.dp
private const val EMOJI_SIZE = 26
