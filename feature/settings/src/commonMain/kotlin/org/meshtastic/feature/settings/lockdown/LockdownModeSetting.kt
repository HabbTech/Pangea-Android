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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.service.LockdownState
import org.meshtastic.core.model.service.LockdownTokenInfo
import org.meshtastic.core.repository.LockdownPassphraseStore
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.lockdown_disable
import org.meshtastic.core.resources.lockdown_disable_message
import org.meshtastic.core.resources.lockdown_enable
import org.meshtastic.core.resources.lockdown_enable_ack
import org.meshtastic.core.resources.lockdown_enable_warning
import org.meshtastic.core.resources.lockdown_lock_now
import org.meshtastic.core.resources.lockdown_mode
import org.meshtastic.core.resources.lockdown_mode_setting_up
import org.meshtastic.core.resources.lockdown_mode_summary_locked
import org.meshtastic.core.resources.lockdown_mode_summary_off
import org.meshtastic.core.resources.lockdown_mode_summary_unlocked
import org.meshtastic.core.resources.lockdown_passphrase
import org.meshtastic.core.resources.lockdown_set_passphrase
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.component.SwitchPreference
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosAccessory
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.ios.IosSheet
import org.meshtastic.core.ui.ios.IosTextField
import org.meshtastic.feature.settings.radio.component.NodeActionButton

/**
 * Runtime lockdown-mode toggle for the security settings screen.
 *
 * The switch and its dialogs are driven entirely by the latest [lockdownState]:
 * - [LockdownState.Disabled] / [LockdownState.NeedsProvision] → OFF; turning ON opens the set-passphrase dialog with
 *   the one-time irreversible warning.
 * - [LockdownState.Locked] → ON (locked); authentication is handled by the global lockdown dialog, so the switch is
 *   read-only here.
 * - [LockdownState.AwaitingResponse] → temporarily non-actionable after transport admission while firmware processes a
 *   lockdown command.
 * - [LockdownState.Unlocked] → ON; turning OFF opens the disable dialog, plus a "Lock now" affordance and session info.
 *
 * Visibility is gated on [supported] — the firmware-version capability from `Capabilities.supportsLockdown` (lockdown
 * ships in firmware v2.8.0). [lockdownState] drives the switch position once a `LockdownStatus` arrives.
 */
@Composable
fun ColumnScope.LockdownModeSetting(
    supported: Boolean,
    lockdownState: LockdownState,
    tokenInfo: LockdownTokenInfo?,
    connected: Boolean,
    containerColor: Color,
    onEnable: (passphrase: String, boots: Int, hours: Int, sessionMinutes: Int) -> Unit,
    onDisable: (passphrase: String) -> Unit,
    onLockNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!supported) return

    var showEnableDialog by rememberSaveable { mutableStateOf(false) }
    var showDisableDialog by rememberSaveable { mutableStateOf(false) }

    val lockdownOn = lockdownState is LockdownState.Locked || lockdownState is LockdownState.Unlocked
    val unlocked = lockdownState is LockdownState.Unlocked
    // Only DISABLED/NEEDS_PROVISION (turn on) and UNLOCKED (turn off) are actionable from here; LOCKED auth is driven
    // by the blocking global dialog.
    val toggleEnabled =
        connected &&
            (lockdownState is LockdownState.Disabled || lockdownState is LockdownState.NeedsProvision || unlocked)

    val summary =
        when (lockdownState) {
            is LockdownState.Unlocked -> stringResource(Res.string.lockdown_mode_summary_unlocked)
            is LockdownState.Locked -> stringResource(Res.string.lockdown_mode_summary_locked)
            is LockdownState.NeedsProvision -> stringResource(Res.string.lockdown_mode_setting_up)
            else -> stringResource(Res.string.lockdown_mode_summary_off)
        }

    SwitchPreference(
        modifier = modifier,
        title = stringResource(Res.string.lockdown_mode),
        summary = summary,
        checked = lockdownOn,
        enabled = toggleEnabled,
        onCheckedChange = { turnOn -> if (turnOn) showEnableDialog = true else showDisableDialog = true },
        containerColor = containerColor,
    )

    if (unlocked) {
        LockdownSessionStatus(tokenInfo = tokenInfo)
        NodeActionButton(
            modifier = Modifier.padding(horizontal = SPACING_DP.dp),
            title = stringResource(Res.string.lockdown_lock_now),
            enabled = connected,
            onClick = onLockNow,
        )
    }

    if (showEnableDialog) {
        EnableLockdownDialog(
            onConfirm = { passphrase, boots, hours, sessionMinutes ->
                showEnableDialog = false
                onEnable(passphrase, boots, hours, sessionMinutes)
            },
            onDismiss = { showEnableDialog = false },
        )
    }

    if (showDisableDialog) {
        DisableLockdownDialog(
            onConfirm = { passphrase ->
                showDisableDialog = false
                onDisable(passphrase)
            },
            onDismiss = { showDisableDialog = false },
        )
    }
}

/**
 * Turning lockdown on: the iPhone app's set-passphrase sheet. The one-time warning heads the passphrase card, then the
 * session limits, an "I understand" switch, and Enable in a card of its own.
 */
@Suppress("LongMethod")
@Composable
private fun EnableLockdownDialog(
    onConfirm: (passphrase: String, boots: Int, hours: Int, sessionMinutes: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var passphrase by rememberSaveable { mutableStateOf("") }
    var confirmPassphrase by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var boots by rememberSaveable { mutableIntStateOf(LockdownPassphraseStore.DEFAULT_BOOTS) }
    var hours by rememberSaveable { mutableIntStateOf(0) }
    var sessionMinutes by rememberSaveable { mutableIntStateOf(0) }
    var acknowledged by rememberSaveable { mutableStateOf(false) }

    val passphraseValid = passphrase.isNotEmpty() && passphrase.encodeToByteArray().size <= MAX_PASSPHRASE_BYTES
    val matches = passphrase == confirmPassphrase
    val isValid = passphraseValid && matches && acknowledged

    IosSheet(
        title = stringResource(Res.string.lockdown_set_passphrase),
        onDismissRequest = onDismiss,
        cancelLabel = stringResource(Res.string.cancel),
        grouped = true,
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            LockdownPassphraseSection(
                passphrase = passphrase,
                onPassphraseChange = { passphrase = it },
                visible = passwordVisible,
                onToggleVisible = { passwordVisible = !passwordVisible },
                note = stringResource(Res.string.lockdown_enable_warning),
                confirm = confirmPassphrase,
                onConfirmChange = { confirmPassphrase = it },
            )
            LockdownSessionSection(
                boots = boots,
                onBootsChange = { boots = it },
                hours = hours,
                onHoursChange = { hours = it },
                minutes = sessionMinutes,
                onMinutesChange = { sessionMinutes = it },
            )
            IosSection {
                IosRow(
                    title = stringResource(Res.string.lockdown_enable_ack),
                    accessory = IosAccessory.Switch(checked = acknowledged, onCheckedChange = { acknowledged = it }),
                )
            }
            LockdownSubmitSection(
                label = stringResource(Res.string.lockdown_enable),
                enabled = isValid,
                onClick = { onConfirm(passphrase, boots, hours, sessionMinutes) },
            )
        }
    }
}

/** Turning lockdown off: an iOS alert with a secure field, its action red because the device reboots. */
@Composable
private fun DisableLockdownDialog(onConfirm: (passphrase: String) -> Unit, onDismiss: () -> Unit) {
    var passphrase by rememberSaveable { mutableStateOf("") }
    val isValid = passphrase.isNotEmpty() && passphrase.encodeToByteArray().size <= MAX_PASSPHRASE_BYTES

    MeshtasticDialog(
        titleRes = Res.string.lockdown_disable,
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(Res.string.lockdown_disable_message),
                    style = Ios.type.footnote,
                    color = Ios.colors.label,
                    textAlign = TextAlign.Center,
                )
                IosTextField(
                    value = passphrase,
                    onValueChange = { if (it.encodeToByteArray().size <= MAX_PASSPHRASE_BYTES) passphrase = it },
                    placeholder = stringResource(Res.string.lockdown_passphrase),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        },
        confirmTextRes = Res.string.lockdown_disable,
        onConfirm = { onConfirm(passphrase) },
        confirmDestructive = true,
        confirmEnabled = isValid,
        dismissTextRes = Res.string.cancel,
        onDismiss = onDismiss,
    )
}

private const val SPACING_DP = 8
