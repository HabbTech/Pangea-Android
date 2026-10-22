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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.service.LockdownState
import org.meshtastic.core.repository.LockdownPassphraseStore
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.disconnect
import org.meshtastic.core.resources.lockdown_backoff
import org.meshtastic.core.resources.lockdown_enter_passphrase
import org.meshtastic.core.resources.lockdown_incorrect_passphrase
import org.meshtastic.core.resources.lockdown_lock_reason
import org.meshtastic.core.resources.lockdown_set_passphrase
import org.meshtastic.core.resources.lockdown_submit
import org.meshtastic.core.ui.ios.IosSheet
import org.meshtastic.core.ui.ios.IosSheetHeader

/**
 * Non-dismissable lockdown authentication sheet.
 *
 * Shown while the connected device requires passphrase input or retry. The sheet blocks app interaction while shown.
 * After a passphrase command is admitted to the active transport, [LockdownState.AwaitingResponse] hides it until
 * firmware reports the next status. A rejected dispatch leaves the retryable state and sheet in place; a locked,
 * provisioning, or failed response shows it again, while successful authentication leaves it dismissed. Swipes, Back
 * and taps outside are ignored whenever it is visible; Disconnect is the way out.
 *
 * Drawn as the iPhone app's lockdown sheet: the passphrase card (with the reason or the error above the field), the
 * session limits, and Submit in a card of its own.
 */
@Suppress("LongMethod")
@Composable
fun LockdownDialog(
    lockdownState: LockdownState,
    onSubmit: (passphrase: String, boots: Int, hours: Int, sessionMinutes: Int) -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shouldShow =
        when (lockdownState) {
            is LockdownState.Locked -> true
            is LockdownState.NeedsProvision -> true
            is LockdownState.UnlockFailed -> true
            is LockdownState.UnlockBackoff -> true
            else -> false
        }
    if (!shouldShow) return

    var passphrase by rememberSaveable { mutableStateOf("") }
    var confirmPassphrase by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var boots by rememberSaveable { mutableIntStateOf(LockdownPassphraseStore.DEFAULT_BOOTS) }
    var hours by rememberSaveable { mutableIntStateOf(0) }
    var sessionMinutes by rememberSaveable { mutableIntStateOf(0) }

    val isProvisioning = lockdownState is LockdownState.NeedsProvision
    val title =
        stringResource(if (isProvisioning) Res.string.lockdown_set_passphrase else Res.string.lockdown_enter_passphrase)
    val inBackoff = lockdownState is LockdownState.UnlockBackoff
    val passphraseValid = passphrase.isNotEmpty() && passphrase.encodeToByteArray().size <= MAX_PASSPHRASE_BYTES
    val confirmValid = !isProvisioning || passphrase == confirmPassphrase
    val isValid = passphraseValid && confirmValid && !inBackoff

    val note =
        when (lockdownState) {
            is LockdownState.UnlockFailed -> stringResource(Res.string.lockdown_incorrect_passphrase)

            is LockdownState.UnlockBackoff -> stringResource(Res.string.lockdown_backoff, lockdownState.backoffSeconds)

            is LockdownState.Locked ->
                lockdownState.lockReason
                    .takeIf { it.isNotEmpty() }
                    ?.let { stringResource(Res.string.lockdown_lock_reason, it) }

            else -> null
        }

    IosSheet(
        title = title,
        onDismissRequest = {},
        modifier = modifier,
        grouped = true,
        dismissable = false,
        header = {
            IosSheetHeader(title = title, cancelLabel = stringResource(Res.string.disconnect), onCancel = onDisconnect)
        },
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
                note = note,
                noteIsError = lockdownState is LockdownState.UnlockFailed || inBackoff,
                confirm = if (isProvisioning) confirmPassphrase else null,
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
            LockdownSubmitSection(
                label = stringResource(Res.string.lockdown_submit),
                enabled = isValid,
                onClick = { onSubmit(passphrase, boots, hours, sessionMinutes) },
            )
        }
    }
}
