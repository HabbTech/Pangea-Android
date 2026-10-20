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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.name
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.iosPressable

/** The name row: the waypoint's emoji in a grey circle (tap to change it), then the name, inline. */
@Composable
internal fun WaypointNameRow(name: String, emoji: String, onNameChange: (String) -> Unit, onPickEmoji: () -> Unit) {
    val c = Ios.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(c.fieldFill).iosPressable(onClick = onPickEmoji),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 22.sp)
        }
        Spacer(Modifier.width(12.dp))
        WaypointTextField(
            value = name,
            placeholder = stringResource(Res.string.name),
            onValueChange = onNameChange,
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
    }
}

/** A text row of the form: the value inline on the card, its placeholder in grey. */
@Composable
internal fun WaypointTextRow(value: String, placeholder: String, onValueChange: (String) -> Unit) {
    WaypointTextField(
        value = value,
        placeholder = placeholder,
        onValueChange = onValueChange,
        singleLine = false,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
    )
}

@Composable
private fun WaypointTextField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = Ios.colors
    Box(modifier) {
        if (value.isEmpty()) Text(placeholder, style = Ios.type.body, color = c.tertiaryLabel)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            maxLines = if (singleLine) 1 else WAYPOINT_DESCRIPTION_LINES,
            textStyle = Ios.type.body.copy(color = c.label),
            cursorBrush = SolidColor(c.tint),
            keyboardOptions =
            KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private const val WAYPOINT_DESCRIPTION_LINES = 3
