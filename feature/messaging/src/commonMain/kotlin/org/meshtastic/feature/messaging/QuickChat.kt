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
package org.meshtastic.feature.messaging

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.database.entity.QuickChatAction
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.add
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.delete
import org.meshtastic.core.resources.message
import org.meshtastic.core.resources.name
import org.meshtastic.core.resources.quick_chat
import org.meshtastic.core.resources.quick_chat_append
import org.meshtastic.core.resources.quick_chat_edit
import org.meshtastic.core.resources.quick_chat_instant
import org.meshtastic.core.resources.quick_chat_new
import org.meshtastic.core.resources.save
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.component.dragContainer
import org.meshtastic.core.ui.component.dragDropItemsIndexed
import org.meshtastic.core.ui.component.rememberDragDropState
import org.meshtastic.core.ui.icon.DragHandle
import org.meshtastic.core.ui.icon.FastForward
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosButton
import org.meshtastic.core.ui.ios.IosButtonStyle
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosInset
import org.meshtastic.core.ui.ios.IosSwitch
import org.meshtastic.core.ui.ios.IosTextField

/**
 * The quick chat buttons, drawn as an iOS inset list: one row per action (its short name, the message under it, edit
 * and the drag handle at the end), with "+" in the bar to add one.
 */
@Composable
fun QuickChatScreen(modifier: Modifier = Modifier, viewModel: QuickChatViewModel, onNavigateUp: () -> Unit) {
    val actions by viewModel.quickChatActions.collectAsStateWithLifecycle()
    var showActionDialog by remember { mutableStateOf<QuickChatAction?>(null) }

    val listState = rememberLazyListState()
    val dragDropState =
        rememberDragDropState(listState) { fromIndex, toIndex ->
            val list = actions.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
            viewModel.updateActionPositions(list)
        }

    Scaffold(
        containerColor = Ios.colors.groupedBackground,
        topBar = {
            MainAppBar(
                title = stringResource(Res.string.quick_chat),
                ourNode = null,
                showNodeChip = false,
                canNavigateUp = true,
                onNavigateUp = onNavigateUp,
                actions = {
                    // iOS adds from the bar, where Android had a floating button.
                    IconButton(onClick = { showActionDialog = QuickChatAction(position = actions.size) }) {
                        Icon(IosIcons.Plus, contentDescription = stringResource(Res.string.add), tint = Ios.colors.tint)
                    }
                },
                onClickChip = {},
            )
        },
    ) { innerPadding ->
        Box(modifier = modifier.fillMaxSize().padding(innerPadding)) {
            showActionDialog?.let {
                EditQuickChatDialog(
                    action = it,
                    onSave = viewModel::addQuickChatAction,
                    onDelete = viewModel::deleteQuickChatAction,
                ) {
                    showActionDialog = null
                }
            }

            LazyColumn(
                modifier = Modifier.dragContainer(dragDropState = dragDropState, haptics = LocalHapticFeedback.current),
                state = listState,
                contentPadding = PaddingValues(horizontal = IosInset, vertical = 20.dp),
            ) {
                dragDropItemsIndexed(items = actions, dragDropState = dragDropState, key = { _, item -> item.uuid }) {
                        index,
                        action,
                        _,
                    ->
                    QuickChatItem(
                        action = action,
                        onEdit = { showActionDialog = it },
                        isFirst = index == 0,
                        isLast = index == actions.lastIndex,
                    )
                }
            }
        }
    }
}

@Suppress("MagicNumber")
private fun getMessageName(message: String): String = if (message.length <= 3) {
    message.uppercase()
} else {
    buildString {
        append(message.first().uppercase())
        append(message[message.length / 2].uppercase())
        append(message.last().uppercase())
    }
}

/** Adding or editing a quick chat button: an iOS alert with the two fields, the instant switch, and Delete. */
@OptIn(ExperimentalLayoutApi::class)
@Suppress("LongMethod")
@Composable
internal fun EditQuickChatDialog(
    action: QuickChatAction,
    onSave: (QuickChatAction) -> Unit,
    onDelete: (QuickChatAction) -> Unit,
    onDismiss: () -> Unit,
) {
    var actionInput by remember { mutableStateOf(action) }
    val newQuickChat = remember { action.uuid == 0L }
    val isInstant = actionInput.mode == QuickChatAction.Mode.Instant
    val title = if (newQuickChat) Res.string.quick_chat_new else Res.string.quick_chat_edit

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        if (newQuickChat) {
            focusRequester.requestFocus()
        }
    }

    MeshtasticDialog(
        onDismiss = onDismiss,
        title = stringResource(title),
        confirmText = stringResource(Res.string.save),
        onConfirm = {
            onSave(actionInput)
            onDismiss()
        },
        dismissText = stringResource(Res.string.cancel),
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                FieldWithCounter(placeholder = stringResource(Res.string.name), value = actionInput.name, maxSize = 5) {
                    actionInput = actionInput.copy(name = it.uppercase())
                }

                FieldWithCounter(
                    placeholder = stringResource(Res.string.message),
                    value = actionInput.message,
                    maxSize = 200,
                    getSize = { it.encodeToByteArray().size + 1 },
                    modifier = Modifier.padding(top = 8.dp).focusRequester(focusRequester),
                ) {
                    actionInput = actionInput.copy(message = it)
                    if (newQuickChat) {
                        actionInput = actionInput.copy(name = getMessageName(it))
                    }
                }

                val text = if (isInstant) Res.string.quick_chat_instant else Res.string.quick_chat_append
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isInstant) {
                        Icon(
                            imageVector = MeshtasticIcons.FastForward,
                            contentDescription = null,
                            tint = Ios.colors.tint,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        text = stringResource(text),
                        style = Ios.type.body,
                        color = Ios.colors.label,
                        modifier = Modifier.weight(1f),
                    )
                    IosSwitch(
                        checked = isInstant,
                        onCheckedChange = { checked ->
                            actionInput =
                                actionInput.copy(
                                    mode = if (checked) QuickChatAction.Mode.Instant else QuickChatAction.Mode.Append,
                                )
                        },
                    )
                }

                if (!newQuickChat) {
                    IosButton(
                        text = stringResource(Res.string.delete),
                        onClick = {
                            onDelete(actionInput)
                            onDismiss()
                        },
                        style = IosButtonStyle.Destructive,
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    )
                }
            }
        },
    )
}

/** An iOS field with its byte or character count under it while it has focus. */
@Composable
private fun FieldWithCounter(
    placeholder: String,
    value: String,
    maxSize: Int,
    modifier: Modifier = Modifier,
    getSize: (String) -> Int = { it.length },
    onValueChange: (String) -> Unit = {},
) = Column(modifier) {
    var isFocused by remember { mutableStateOf(false) }
    IosTextField(
        value = value,
        onValueChange = {
            if (getSize(it) <= maxSize) {
                onValueChange(it)
            }
        },
        placeholder = placeholder,
        modifier = Modifier.onFocusEvent { isFocused = it.isFocused },
    )
    if (isFocused) {
        Text(
            text = "${getSize(value)}/$maxSize",
            style = Ios.type.caption1,
            color = Ios.colors.secondaryLabel,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, end = 6.dp),
        )
    }
}

/**
 * One quick chat button as a row of an iOS inset list. [isFirst] and [isLast] round the card's ends; rows in between
 * are joined by a hairline, as one section.
 */
@Composable
internal fun QuickChatItem(
    action: QuickChatAction,
    modifier: Modifier = Modifier,
    onEdit: (QuickChatAction) -> Unit = {},
    isFirst: Boolean = true,
    isLast: Boolean = true,
) {
    val c = Ios.colors
    val corner = 26.dp
    val shape =
        RoundedCornerShape(
            topStart = if (isFirst) corner else 0.dp,
            topEnd = if (isFirst) corner else 0.dp,
            bottomStart = if (isLast) corner else 0.dp,
            bottomEnd = if (isLast) corner else 0.dp,
        )
    Column(modifier.fillMaxWidth().clip(shape).background(c.groupedRow)) {
        if (!isFirst) {
            HorizontalDivider(thickness = 0.5.dp, color = c.separator, modifier = Modifier.padding(start = 56.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(56.dp), contentAlignment = Alignment.Center) {
                if (action.mode == QuickChatAction.Mode.Instant) {
                    Icon(
                        imageVector = MeshtasticIcons.FastForward,
                        contentDescription = stringResource(Res.string.quick_chat_instant),
                        tint = c.tint,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Column(Modifier.weight(1f).padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = action.name, style = Ios.type.body.copy(fontWeight = FontWeight.SemiBold), color = c.label)
                Text(text = action.message, style = Ios.type.subheadline, color = c.secondaryLabel)
            }
            IconButton(onClick = { onEdit(action) }) {
                Icon(
                    imageVector = IosIcons.Compose,
                    contentDescription = stringResource(Res.string.quick_chat_edit),
                    tint = c.tint,
                )
            }
            Icon(
                imageVector = MeshtasticIcons.DragHandle,
                contentDescription = stringResource(Res.string.quick_chat),
                tint = c.tertiaryLabel,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}
