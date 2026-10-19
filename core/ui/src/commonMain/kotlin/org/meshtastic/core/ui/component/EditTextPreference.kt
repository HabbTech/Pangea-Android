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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.NumberFormatter
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.error
import org.meshtastic.core.ui.icon.Info
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.theme.AppTheme

@Composable
fun SignedIntegerEditTextPreference(
    title: String,
    value: Int,
    enabled: Boolean,
    keyboardActions: KeyboardActions,
    onValueChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    onFocusChanged: (FocusState) -> Unit = {},
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    var valueState by remember(value) { mutableStateOf(value.toString()) }

    EditTextPreference(
        title = title,
        value = valueState,
        enabled = enabled,
        summary = summary,
        isError = valueState.toIntOrNull() == null,
        // Signed by contract: RSSI thresholds and tx power are negative, and Number has no minus key.
        keyboardOptions =
        KeyboardOptions.Default.copy(keyboardType = KeyboardType.NumberSigned, imeAction = ImeAction.Done),
        keyboardActions = keyboardActions,
        onValueChanged = {
            valueState = it
            it.toIntOrNull()?.let { int -> onValueChanged(int) }
        },
        onFocusChanged = onFocusChanged,
        modifier = modifier,
        trailingIcon = trailingIcon,
    )
}

@Composable
fun EditTextPreference(
    title: String,
    value: Int,
    enabled: Boolean,
    isError: Boolean = false,
    keyboardActions: KeyboardActions,
    onValueChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    onFocusChanged: (FocusState) -> Unit = {},
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    var valueState by remember(value) { mutableStateOf(value.toUInt().toString()) }

    EditTextPreference(
        title = title,
        value = valueState,
        enabled = enabled,
        summary = summary,
        isError = value.toUInt().toString() != valueState || isError,
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = keyboardActions,
        onValueChanged = {
            if (it.isEmpty()) {
                valueState = it
            } else {
                it.toUIntOrNull()?.toInt()?.let { int ->
                    valueState = it
                    onValueChanged(int)
                }
            }
        },
        onFocusChanged = onFocusChanged,
        modifier = modifier,
        trailingIcon = trailingIcon,
    )
}

@Composable
fun EditTextPreference(
    title: String,
    value: Float,
    enabled: Boolean,
    keyboardActions: KeyboardActions,
    onValueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    onFocusChanged: (FocusState) -> Unit = {},
) {
    var valueState by remember(value) { mutableStateOf(value.toString()) }

    EditTextPreference(
        title = title,
        value = valueState,
        enabled = enabled,
        summary = summary,
        // Compare parsed values, not strings: a comma-locale "3,7" is correct input but never string-equals "3.7".
        isError = NumberFormatter.parseDecimalOrNull(valueState)?.toFloat() != value,
        keyboardOptions =
        KeyboardOptions.Default.copy(keyboardType = KeyboardType.DecimalSigned, imeAction = ImeAction.Done),
        keyboardActions = keyboardActions,
        onValueChanged = {
            val parsed = NumberFormatter.parseDecimalOrNull(it)?.toFloat()
            if (parsed != null) {
                valueState = it
                onValueChanged(parsed)
            } else if (NumberFormatter.isPartialDecimal(it)) {
                // Keep transient input ("", "-", "-.") so the field doesn't snap back mid-entry of "-1.5".
                valueState = it
            }
        },
        onFocusChanged = onFocusChanged,
        modifier = modifier,
    )
}

@Composable
fun EditTextPreference(
    title: String,
    value: Double,
    enabled: Boolean,
    keyboardActions: KeyboardActions,
    onValueChanged: (Double) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    onFocusChanged: (FocusState) -> Unit = {},
) {
    var valueState by remember(value) { mutableStateOf(value.toString()) }

    EditTextPreference(
        title = title,
        value = valueState,
        enabled = enabled,
        summary = summary,
        // Compare parsed values, not strings: a comma-locale "48,21" is correct input but never string-equals "48.21".
        isError = NumberFormatter.parseDecimalOrNull(valueState) != value,
        keyboardOptions =
        KeyboardOptions.Default.copy(keyboardType = KeyboardType.DecimalSigned, imeAction = ImeAction.Done),
        keyboardActions = keyboardActions,
        onValueChanged = {
            val parsed = NumberFormatter.parseDecimalOrNull(it)
            if (parsed != null) {
                valueState = it
                onValueChanged(parsed)
            } else if (NumberFormatter.isPartialDecimal(it)) {
                // Keep transient input ("", "-", "-.") so the field doesn't snap back mid-entry of "-1.5".
                valueState = it
            }
        },
        onFocusChanged = onFocusChanged,
        modifier = modifier,
    )
}

@Composable
fun EditTextPreference(
    title: String,
    value: String,
    enabled: Boolean,
    isError: Boolean,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    onValueChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    maxSize: Int = 0, // max_size - 1 (in bytes)
    // Set to show the byte counter once the value is within this much of [maxSize], the way the message composer
    // does, instead of only while the field has focus.
    counterVisibleWithinBytes: Int? = null,
    onFocusChanged: (FocusState) -> Unit = {},
    // [modifier] lands on the wrapping column, so a caller that needs the text field itself focused passes one here.
    focusRequester: FocusRequester? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    multiline: Boolean = false,
) {
    var isFocused by remember { mutableStateOf(false) }
    val c = Ios.colors

    val fieldModifier =
        Modifier.then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier).onFocusEvent {
            isFocused = it.isFocused
            onFocusChanged(it)
        }
    val onFieldChange: (String) -> Unit = {
        if (maxSize <= 0 || it.encodeToByteArray().size <= maxSize) onValueChanged(it)
    }

    PreferenceRow(
        title = title,
        summary = summary,
        enabled = enabled,
        fillTitle = multiline,
        modifier = modifier,
        trailing = {
            if (!multiline) {
                InlineTextField(
                    value = value,
                    onValueChange = onFieldChange,
                    placeholder = title,
                    enabled = enabled,
                    multiline = multiline,
                    isError = isError,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    visualTransformation = visualTransformation,
                    modifier = Modifier.weight(1f).then(fieldModifier),
                )
            }
            when {
                trailingIcon != null -> trailingIcon()

                isError ->
                    Icon(
                        imageVector = MeshtasticIcons.Info,
                        contentDescription = stringResource(Res.string.error),
                        tint = c.systemRed,
                        modifier = Modifier.padding(start = 6.dp).size(20.dp),
                    )
            }
        },
        below = {
            if (multiline) {
                InlineTextField(
                    value = value,
                    onValueChange = onFieldChange,
                    placeholder = title,
                    enabled = enabled,
                    multiline = multiline,
                    isError = isError,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    visualTransformation = visualTransformation,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp).then(fieldModifier),
                )
            }
            val valueBytes = value.encodeToByteArray().size
            if (showByteCounter(valueBytes, maxSize, counterVisibleWithinBytes, isFocused)) {
                ByteCounter(valueBytes = valueBytes, maxSize = maxSize, isError = isError)
            }
        },
    )
}

/** The text field inside an [EditTextPreference] row: plain text on the card, its [placeholder] in grey when empty. */
@Composable
private fun InlineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean,
    multiline: Boolean,
    isError: Boolean,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    visualTransformation: VisualTransformation,
    modifier: Modifier = Modifier,
) {
    val c = Ios.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        singleLine = !multiline,
        maxLines = if (multiline) MULTILINE_MAX_LINES else 1,
        textStyle = Ios.type.body.copy(color = if (isError) c.systemRed else c.label),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        cursorBrush = SolidColor(c.tint),
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth()) {
                // iOS shows the field's name as its placeholder; the row's title already names it for screen readers.
                if (value.isEmpty()) {
                    Text(
                        placeholder,
                        style = Ios.type.body,
                        color = c.tertiaryLabel,
                        maxLines = 1,
                        modifier = Modifier.clearAndSetSemantics {},
                    )
                }
                inner()
            }
        },
    )
}

/** A multi-line field grows to this many lines before it scrolls. */
private const val MULTILINE_MAX_LINES = 5

/** The `n/max` byte budget shown under a size-capped field. */
@Composable
private fun ByteCounter(valueBytes: Int, maxSize: Int, isError: Boolean) {
    Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$valueBytes/$maxSize",
            style = MaterialTheme.typography.bodySmall,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(end = 8.dp, bottom = 4.dp),
        )
    }
}

/**
 * Whether the byte counter earns its place: within [within] bytes of the limit when a field asks for the message
 * composer's rule, otherwise while the field has focus. A counter reading 0/80 before a character is typed is chrome.
 */
private fun showByteCounter(valueBytes: Int, maxSize: Int, within: Int?, isFocused: Boolean) = when {
    maxSize <= 0 -> false
    within == null -> isFocused
    else -> valueBytes >= maxSize - within
}

@Preview(showBackground = true)
@Composable
fun EditTextPreferencePreview() {
    AppTheme {
        Column {
            EditTextPreference(
                title = "String",
                value = "Meshtastic",
                summary = "This is a summary",
                maxSize = 39,
                enabled = true,
                isError = false,
                keyboardOptions = KeyboardOptions.Default,
                keyboardActions = KeyboardActions {},
                onValueChanged = {},
            )
            EditTextPreference(
                title = "Advanced Settings",
                value = UInt.MAX_VALUE.toInt(),
                enabled = true,
                keyboardActions = KeyboardActions {},
                onValueChanged = {},
            )
        }
    }
}
