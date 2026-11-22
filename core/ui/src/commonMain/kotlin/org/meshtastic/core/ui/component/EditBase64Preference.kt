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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isSensitiveData
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import okio.ByteString
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.Channel
import org.meshtastic.core.model.util.base64ToByteString
import org.meshtastic.core.model.util.encodeToString
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.error
import org.meshtastic.core.resources.reset
import org.meshtastic.core.ui.icon.Close
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Refresh
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.theme.AppTheme

@Suppress("LongMethod", "CyclomaticComplexMethod", "MagicNumber")
@Composable
fun EditBase64Preference(
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null,
    value: ByteString,
    enabled: Boolean,
    readOnly: Boolean = false,
    placeholderText: String? = null,
    keyboardActions: KeyboardActions,
    onValueChange: (ByteString) -> Unit,
    onGenerateKey: (() -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val isMismatch = value.size == 32 && value.toByteArray().all { it == 0.toByte() }
    val errorString = stringResource(Res.string.error)
    var valueState by remember { mutableStateOf(if (isMismatch) errorString else value.encodeToString()) }
    val isError = value.encodeToString() != valueState || isMismatch

    // don't update values while the user is editing
    var isFocused by remember { mutableStateOf(false) }
    LaunchedEffect(value) {
        if (!isFocused) {
            valueState = if (isMismatch) errorString else value.encodeToString()
        }
    }

    val (icon, description) =
        when {
            isError -> MeshtasticIcons.Close to stringResource(Res.string.error)
            onGenerateKey != null && !isFocused -> MeshtasticIcons.Refresh to stringResource(Res.string.reset)
            else -> null to null
        }
    // iOS: the name on its own line (with its symbol and any buttons), the key under it in grey monospace, the help
    // text last. A field with nothing in it shows its name, as iOS's placeholder does.
    val c = Ios.colors
    val keyStyle = Ios.type.subheadline.copy(fontFamily = FontFamily.Monospace)
    PreferenceRow(
        title = title,
        modifier = modifier,
        enabled = enabled,
        trailing = {
            if (icon != null) {
                IconButton(
                    onClick = {
                        if (isError) {
                            valueState = value.encodeToString()
                            onValueChange(value)
                        } else if (onGenerateKey != null && !isFocused) {
                            onGenerateKey()
                        }
                    },
                    enabled = enabled,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = description,
                        tint = if (isError) c.systemRed else c.tint,
                    )
                }
            } else if (trailingIcon != null) {
                trailingIcon()
            }
        },
        below = {
            Box(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                if (valueState.isEmpty()) {
                    Text(text = placeholderText ?: title, style = keyStyle, color = c.tertiaryLabel)
                }
                BasicTextField(
                    value = valueState,
                    onValueChange = {
                        valueState = it
                        runCatching { it.base64ToByteString() }.onSuccess(onValueChange)
                    },
                    modifier =
                    Modifier.fillMaxWidth()
                        .onFocusChanged { focusState -> isFocused = focusState.isFocused }
                        .semantics {
                            isSensitiveData = true
                            contentDescription = title
                        },
                    enabled = enabled,
                    readOnly = readOnly,
                    textStyle = keyStyle.copy(color = if (isError) c.systemRed else c.secondaryLabel),
                    cursorBrush = SolidColor(c.tint),
                    keyboardOptions =
                    KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = keyboardActions,
                )
            }
            if (summary != null) {
                Text(
                    text = summary,
                    style = Ios.type.footnote,
                    color = c.secondaryLabel,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun EditBase64PreferencePreview() {
    AppTheme {
        EditBase64Preference(
            title = "Title",
            summary = "This is a summary",
            value = Channel.getRandomKey(),
            enabled = true,
            keyboardActions = KeyboardActions {},
            onValueChange = { _ -> },
            onGenerateKey = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
