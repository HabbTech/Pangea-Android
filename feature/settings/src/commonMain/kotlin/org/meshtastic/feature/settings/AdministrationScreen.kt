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
package org.meshtastic.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.Node
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.administration
import org.meshtastic.core.resources.preserve_favorites
import org.meshtastic.core.resources.remotely_administrating
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosPushedColumnScreen
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.ios.IosSwitch
import org.meshtastic.feature.settings.radio.AdminRoute
import org.meshtastic.feature.settings.radio.RadioConfigState
import org.meshtastic.feature.settings.radio.RadioConfigViewModel
import org.meshtastic.feature.settings.radio.ResponseState
import org.meshtastic.feature.settings.radio.component.LoadingOverlay
import org.meshtastic.feature.settings.radio.component.PacketResponseStateDialog
import org.meshtastic.feature.settings.radio.component.ShutdownConfirmationDialog
import org.meshtastic.feature.settings.radio.component.WarningDialog

@Composable
fun AdministrationScreen(viewModel: RadioConfigViewModel, onBack: () -> Unit) {
    val state by viewModel.radioConfigState.collectAsStateWithLifecycle()
    val destNode by viewModel.destNode.collectAsStateWithLifecycle()
    val enabled = state.connected && !state.responseState.isWaiting()

    Box(modifier = Modifier.fillMaxSize()) {
        // iOS keeps these commands as red buttons in a section of their own, each asking before it acts.
        IosPushedColumnScreen(title = stringResource(Res.string.administration), onBack = onBack, largeTitle = true) {
            IosSection(
                header =
                if (state.isLocal) {
                    destNode?.user?.long_name
                } else {
                    stringResource(Res.string.remotely_administrating, destNode?.user?.long_name ?: "")
                },
            ) {
                AdminRouteItems(viewModel = viewModel, enabled = enabled, state = state, destNode = destNode)
            }
        }

        LoadingOverlay(state = state.responseState)

        if (state.responseState is ResponseState.Success || state.responseState is ResponseState.Error) {
            PacketResponseStateDialog(
                state = state.responseState,
                onDismiss = { viewModel.clearPacketResponse() },
                onComplete = {
                    viewModel.clearPacketResponse()
                    onBack()
                },
            )
        }
    }
}

@Composable
private fun AdminRouteItems(
    viewModel: RadioConfigViewModel,
    enabled: Boolean,
    state: RadioConfigState,
    destNode: Node?,
) {
    AdminRoute.entries.forEach { route ->
        var showDialog by remember { mutableStateOf(false) }
        if (showDialog) {
            AdminActionDialog(
                route = route,
                destNode = destNode,
                enabled = enabled,
                state = state,
                onDismiss = { showDialog = false },
                onConfirm = { viewModel.setResponseStateLoading(route) },
                onPreserveFavoritesChange = { viewModel.setPreserveFavorites(it) },
            )
        }

        IosRow(
            title = stringResource(route.title),
            // Setting the clock changes nothing on the radio for good, so it is the one command not in red.
            titleColor =
            when {
                !enabled -> Ios.colors.tertiaryLabel
                route == AdminRoute.SET_TIME -> Ios.colors.tint
                else -> Ios.colors.systemRed
            },
            onClick = if (enabled) ({ showDialog = true }) else null,
        )
    }
}

@Composable
private fun AdminActionDialog(
    route: AdminRoute,
    destNode: Node?,
    enabled: Boolean,
    state: RadioConfigState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onPreserveFavoritesChange: (Boolean) -> Unit,
) {
    if (route == AdminRoute.SHUTDOWN || route == AdminRoute.REBOOT) {
        ShutdownConfirmationDialog(
            title = "${stringResource(route.title)}?",
            node = destNode,
            onDismiss = onDismiss,
            isShutdown = route == AdminRoute.SHUTDOWN,
            onConfirm = onConfirm,
        )
    } else {
        WarningDialog(
            title = "${stringResource(route.title)}?",
            destructive = route != AdminRoute.SET_TIME,
            text = {
                if (route == AdminRoute.NODEDB_RESET) {
                    Row(
                        modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(Res.string.preserve_favorites),
                            style = Ios.type.body,
                            color = Ios.colors.label,
                        )
                        IosSwitch(
                            checked = state.nodeDbResetPreserveFavorites,
                            onCheckedChange = onPreserveFavoritesChange,
                            enabled = enabled,
                        )
                    }
                }
            },
            onDismiss = onDismiss,
            onConfirm = onConfirm,
        )
    }
}
