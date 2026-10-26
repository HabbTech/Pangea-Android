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
package org.meshtastic.feature.settings.lockdown

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.lockdown_boots_remaining
import org.meshtastic.core.resources.lockdown_confirm_passphrase
import org.meshtastic.core.resources.lockdown_hide_passphrase
import org.meshtastic.core.resources.lockdown_hours_until_expiry
import org.meshtastic.core.resources.lockdown_passphrase
import org.meshtastic.core.resources.lockdown_passphrases_do_not_match
import org.meshtastic.core.resources.lockdown_session_minutes
import org.meshtastic.core.resources.lockdown_session_minutes_help
import org.meshtastic.core.resources.lockdown_show_passphrase
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Visibility
import org.meshtastic.core.ui.icon.VisibilityOff
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosAccessory
import org.meshtastic.core.ui.ios.IosInset
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.ios.IosTextField
import org.meshtastic.core.ui.ios.iosRowSeparator

/**
 * The passphrase card of the iPhone app's lockdown sheet: an optional [note] (red when [noteIsError]) above a secure
 * field with Show/Hide, then, when [confirm] is non-null, the confirmation field and its mismatch warning.
 */
@Suppress("LongParameterList")
@Composable
internal fun LockdownPassphraseSection(
    passphrase: String,
    onPassphraseChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    modifier: Modifier = Modifier,
    note: String? = null,
    noteIsError: Boolean = false,
    confirm: String? = null,
    onConfirmChange: (String) -> Unit = {},
) {
    val mismatch = confirm != null && confirm.isNotEmpty() && confirm != passphrase
    IosSection(
        modifier = modifier,
        footer = if (mismatch) stringResource(Res.string.lockdown_passphrases_do_not_match) else null,
    ) {
        if (note != null) {
            Text(
                note,
                style = Ios.type.footnote,
                color = if (noteIsError) Ios.colors.systemRed else Ios.colors.secondaryLabel,
                modifier = Modifier.fillMaxWidth().padding(horizontal = IosInset, vertical = 11.dp),
            )
        }
        LockdownField(
            value = passphrase,
            onValueChange = onPassphraseChange,
            placeholder = stringResource(Res.string.lockdown_passphrase),
            hidden = !visible,
            onToggleVisible = onToggleVisible,
        )
        if (confirm != null) {
            LockdownField(
                value = confirm,
                onValueChange = onConfirmChange,
                placeholder = stringResource(Res.string.lockdown_confirm_passphrase),
                hidden = true,
            )
        }
    }
}

/**
 * The session limits: reboots and hours before the unlock lapses, and the per-boot cap, each a row with its number at
 * the end.
 */
@Suppress("LongParameterList")
@Composable
internal fun LockdownSessionSection(
    boots: Int,
    onBootsChange: (Int) -> Unit,
    hours: Int,
    onHoursChange: (Int) -> Unit,
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    IosSection(modifier = modifier, footer = stringResource(Res.string.lockdown_session_minutes_help)) {
        NumberRow(stringResource(Res.string.lockdown_boots_remaining), boots) {
            onBootsChange(it.coerceIn(1, MAX_BOOTS))
        }
        NumberRow(stringResource(Res.string.lockdown_hours_until_expiry), hours) { onHoursChange(it.coerceAtLeast(0)) }
        NumberRow(stringResource(Res.string.lockdown_session_minutes), minutes) { onMinutesChange(it.coerceAtLeast(0)) }
    }
}

/** A form's one button: centred bold tinted text in its own card, grey while it can't be pressed. */
@Composable
internal fun LockdownSubmitSection(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IosSection(modifier = modifier) {
        Box(
            Modifier.fillMaxWidth()
                .heightIn(min = 52.dp)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                style = Ios.type.body.copy(fontWeight = FontWeight.SemiBold),
                color = if (enabled) Ios.colors.tint else Ios.colors.tertiaryLabel,
            )
        }
    }
}

/** A secure field drawn as a form row, with Show/Hide at its end when [onToggleVisible] is set. */
@Composable
private fun LockdownField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    hidden: Boolean,
    onToggleVisible: (() -> Unit)? = null,
) {
    IosTextField(
        value = value,
        onValueChange = { if (it.encodeToByteArray().size <= MAX_PASSPHRASE_BYTES) onValueChange(it) },
        placeholder = placeholder,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None,
        containerColor = Color.Transparent,
        modifier = Modifier.iosRowSeparator().padding(start = 2.dp, end = 2.dp),
        trailing =
        onToggleVisible?.let { toggle ->
            {
                Icon(
                    if (hidden) MeshtasticIcons.Visibility else MeshtasticIcons.VisibilityOff,
                    contentDescription =
                    stringResource(
                        if (hidden) Res.string.lockdown_show_passphrase else Res.string.lockdown_hide_passphrase,
                    ),
                    tint = Ios.colors.secondaryLabel,
                    modifier = Modifier.size(22.dp).clickable(role = Role.Button, onClick = toggle),
                )
            }
        },
    )
}

/** A row whose value is a number typed in place at its end, as iOS's TextField rows. */
@Composable
private fun NumberRow(title: String, value: Int, onValueChange: (Int) -> Unit) {
    IosRow(
        title = title,
        accessory =
        IosAccessory.Custom {
            IosTextField(
                value = value.toString(),
                onValueChange = { text -> text.toIntOrNull()?.let(onValueChange) },
                placeholder = "0",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textAlign = TextAlign.End,
                containerColor = Color.Transparent,
                modifier = Modifier.width(NUMBER_FIELD_WIDTH.dp),
            )
        },
    )
}

/** Firmware maximum: AdminMessage.lockdown_auth.passphrase is limited to 64 bytes. */
internal const val MAX_PASSPHRASE_BYTES = 64

private const val MAX_BOOTS = 255

private const val NUMBER_FIELD_WIDTH = 96
