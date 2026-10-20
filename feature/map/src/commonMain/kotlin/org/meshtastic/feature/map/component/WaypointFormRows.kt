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
package org.meshtastic.feature.map.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosAccessory
import org.meshtastic.core.ui.ios.IosButton
import org.meshtastic.core.ui.ios.IosButtonStyle
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosInset
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.iosPressable
import org.meshtastic.core.ui.ios.iosRowSeparator

/**
 * A labelled text row of iOS's form: the [label], then the field taking the rest of the row, its words in grey as iOS's
 * `.foregroundColor(.gray)` draws them. A [multiline] field grows downward, as `axis: .vertical` does.
 */
@Composable
internal fun WaypointFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    multiline: Boolean = false,
) {
    val c = Ios.colors
    Row(
        Modifier.iosRowSeparator()
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = IosInset, vertical = 13.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(label, style = Ios.type.body, color = c.label)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(label, style = Ios.type.body, color = c.tertiaryLabel)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = !multiline,
                maxLines = if (multiline) DESCRIPTION_LINES else 1,
                textStyle = Ios.type.body.copy(color = c.secondaryLabel),
                cursorBrush = SolidColor(c.tint),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** The Icon row: its label, then the emoji at iOS's 34 pt; tapping it opens the picker. */
@Composable
internal fun WaypointIconRow(label: String, emoji: String, onPickEmoji: () -> Unit) {
    Row(
        Modifier.iosRowSeparator()
            .fillMaxWidth()
            .iosPressable(onClick = onPickEmoji)
            .padding(horizontal = IosInset, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = Ios.type.body, color = Ios.colors.label)
        Spacer(Modifier.width(8.dp))
        Text(emoji, fontSize = EMOJI_SIZE.sp)
    }
}

/** iOS's compact date picker: the label, then the date and the time each in a grey chip that opens its picker. */
@Composable
internal fun WaypointExpireRow(
    label: String,
    date: String,
    time: String,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
) {
    IosRow(
        title = label,
        titleStyle = Ios.type.callout,
        accessory =
        IosAccessory.Custom {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DateChip(date, onPickDate)
                DateChip(time, onPickTime)
            }
        },
    )
}

/**
 * iOS's menu picker for the geofence radius: the label with its symbol, the choice in grey with up/down chevrons, and
 * the choices in a menu from there.
 */
@Composable
internal fun <T> WaypointRadiusRow(label: String, choices: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    IosRow(
        title = label,
        icon = IosIcons.CircleDashed,
        value = choices.firstOrNull { it.first == selected }?.second,
        onClick = { open = true },
        accessory =
        IosAccessory.Custom {
            Box {
                Icon(
                    IosIcons.ChevronUpDown,
                    contentDescription = null,
                    tint = Ios.colors.secondaryLabel,
                    modifier = Modifier.padding(start = 6.dp).size(14.dp),
                )
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    choices.forEach { (value, text) ->
                        DropdownMenuItem(
                            text = { Text(text) },
                            trailingIcon =
                            if (value == selected) {
                                { Icon(IosIcons.Checkmark, contentDescription = null, tint = Ios.colors.tint) }
                            } else {
                                null
                            },
                            onClick = {
                                open = false
                                onSelect(value)
                            },
                        )
                    }
                }
            }
        },
    )
}

/**
 * The buttons under iOS's waypoint form: Send, Cancel and, for a saved waypoint, Delete, as bordered capsules in a
 * centred row.
 */
@Suppress("LongParameterList")
@Composable
internal fun WaypointFormActions(
    sendLabel: String,
    sendEnabled: Boolean,
    onSend: () -> Unit,
    cancelLabel: String,
    onCancel: () -> Unit,
    deleteLabel: String?,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        IosButton(
            text = sendLabel,
            onClick = onSend,
            style = IosButtonStyle.Tinted,
            enabled = sendEnabled,
            icon = IosIcons.ArrowUp,
        )
        IosButton(text = cancelLabel, onClick = onCancel, style = IosButtonStyle.Tinted, icon = IosIcons.XmarkCircle)
        if (deleteLabel != null) {
            IosButton(text = deleteLabel, onClick = onDelete, style = IosButtonStyle.Destructive, icon = IosIcons.Trash)
        }
    }
}

@Composable
private fun DateChip(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = Ios.type.callout,
        color = Ios.colors.label,
        modifier =
        Modifier.clip(RoundedCornerShape(8.dp))
            .background(Ios.colors.fieldFill)
            .iosPressable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
    )
}

private const val DESCRIPTION_LINES = 3

/** iOS draws the Icon field's emoji at 34 pt. */
private const val EMOJI_SIZE = 34
