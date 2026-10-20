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
package org.meshtastic.feature.settings.pangea

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.pangea_forget_radio
import org.meshtastic.core.resources.pangea_forget_radio_message
import org.meshtastic.core.resources.pangea_forget_radio_title
import org.meshtastic.core.resources.pangea_my_radio
import org.meshtastic.core.resources.pangea_no_radio_paired
import org.meshtastic.core.resources.pangea_pair_different_radio
import org.meshtastic.core.resources.pangea_pair_radio
import org.meshtastic.core.resources.pangea_radio_connected
import org.meshtastic.core.resources.pangea_radio_connecting
import org.meshtastic.core.resources.pangea_radio_not_connected
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.pangea.MyProfileViewModel

/** My radio: its name and whether it's connected, with Pair and Forget (Pangea-Apple MyRadioSection). */
@Composable
fun MyRadioSection(
    onPairRadio: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val connection by viewModel.connectionState.collectAsStateWithLifecycle()
    var confirmForget by remember { mutableStateOf(false) }
    val paired = state.code != null
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.pangea_my_radio), style = MaterialTheme.typography.labelMedium)
            Text(
                state.radioName.ifEmpty { stringResource(Res.string.pangea_no_radio_paired) },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(
                    when (connection) {
                        ConnectionState.Connected -> Res.string.pangea_radio_connected
                        ConnectionState.Connecting -> Res.string.pangea_radio_connecting
                        else -> Res.string.pangea_radio_not_connected
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color =
                if (connection == ConnectionState.Connected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPairRadio) {
                    Text(
                        stringResource(
                            if (paired) Res.string.pangea_pair_different_radio else Res.string.pangea_pair_radio,
                        ),
                    )
                }
                if (paired) {
                    TextButton(onClick = { confirmForget = true }) {
                        Text(stringResource(Res.string.pangea_forget_radio))
                    }
                }
            }
        }
    }
    if (confirmForget) {
        MeshtasticDialog(
            titleRes = Res.string.pangea_forget_radio_title,
            messageRes = Res.string.pangea_forget_radio_message,
            confirmTextRes = Res.string.pangea_forget_radio,
            onConfirm = {
                confirmForget = false
                viewModel.forgetRadio()
            },
            confirmDestructive = true,
            dismissTextRes = Res.string.cancel,
            onDismiss = { confirmForget = false },
        )
    }
}
