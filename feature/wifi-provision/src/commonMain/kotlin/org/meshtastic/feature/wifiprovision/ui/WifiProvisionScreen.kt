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
@file:Suppress("TooManyFunctions", "LongMethod")

package org.meshtastic.feature.wifiprovision.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.apply
import org.meshtastic.core.resources.back
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.hide_password
import org.meshtastic.core.resources.img_mpwrd_logo
import org.meshtastic.core.resources.mpwrd_os
import org.meshtastic.core.resources.password
import org.meshtastic.core.resources.retry
import org.meshtastic.core.resources.show_password
import org.meshtastic.core.resources.wifi_provision_available_networks
import org.meshtastic.core.resources.wifi_provision_connect_failed
import org.meshtastic.core.resources.wifi_provision_description
import org.meshtastic.core.resources.wifi_provision_device_found
import org.meshtastic.core.resources.wifi_provision_device_found_detail
import org.meshtastic.core.resources.wifi_provision_hidden_network
import org.meshtastic.core.resources.wifi_provision_mpwrd_disclaimer
import org.meshtastic.core.resources.wifi_provision_no_networks
import org.meshtastic.core.resources.wifi_provision_scan_failed
import org.meshtastic.core.resources.wifi_provision_scan_networks
import org.meshtastic.core.resources.wifi_provision_scanning_ble
import org.meshtastic.core.resources.wifi_provision_scanning_wifi
import org.meshtastic.core.resources.wifi_provision_sending_credentials
import org.meshtastic.core.resources.wifi_provision_signal_strength
import org.meshtastic.core.resources.wifi_provision_ssid_label
import org.meshtastic.core.resources.wifi_provision_ssid_placeholder
import org.meshtastic.core.resources.wifi_provision_success_description
import org.meshtastic.core.resources.wifi_provision_success_device_connected
import org.meshtastic.core.resources.wifi_provision_success_done
import org.meshtastic.core.resources.wifi_provision_success_ip_address
import org.meshtastic.core.resources.wifi_provision_success_missing_ip
import org.meshtastic.core.resources.wifi_provision_success_open_ssh
import org.meshtastic.core.resources.wifi_provision_success_open_ssh_fallback
import org.meshtastic.core.resources.wifi_provision_success_password_value
import org.meshtastic.core.resources.wifi_provision_success_setup_description
import org.meshtastic.core.resources.wifi_provision_success_setup_title
import org.meshtastic.core.resources.wifi_provision_success_ssh_command
import org.meshtastic.core.resources.wifi_provision_success_ssh_label
import org.meshtastic.core.resources.wifi_provision_success_ssh_unavailable
import org.meshtastic.core.resources.wifi_provision_success_username
import org.meshtastic.core.resources.wifi_provision_success_username_value
import org.meshtastic.core.resources.wifi_provisioning
import org.meshtastic.core.ui.component.AutoLinkText
import org.meshtastic.core.ui.component.CopyIconButton
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Visibility
import org.meshtastic.core.ui.icon.VisibilityOff
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosAccessory
import org.meshtastic.core.ui.ios.IosActivityIndicator
import org.meshtastic.core.ui.ios.IosButton
import org.meshtastic.core.ui.ios.IosButtonStyle
import org.meshtastic.core.ui.ios.IosCardShape
import org.meshtastic.core.ui.ios.IosGlassIconButton
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosInset
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.ios.IosTextField
import org.meshtastic.core.ui.ios.IosTopBar
import org.meshtastic.core.ui.ios.LocalIosBottomInset
import org.meshtastic.core.ui.ios.iosRowSeparator
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.core.ui.util.rememberOpenUrl
import org.meshtastic.feature.wifiprovision.WifiProvisionError
import org.meshtastic.feature.wifiprovision.WifiProvisionUiState
import org.meshtastic.feature.wifiprovision.WifiProvisionUiState.Phase
import org.meshtastic.feature.wifiprovision.WifiProvisionUiState.ProvisionStatus
import org.meshtastic.feature.wifiprovision.WifiProvisionViewModel
import org.meshtastic.feature.wifiprovision.model.WifiNetwork

/**
 * Wi-Fi provisioning for an mPWRD-OS device, drawn as the iPhone app's Wi-Fi Provisioning page: an iOS bar on the
 * grouped background, the steps as centred status pages with capsule buttons, and the network form as grouped sections.
 */
@Suppress("LongMethod")
@Composable
fun WifiProvisionScreen(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    address: String? = null,
    viewModel: WifiProvisionViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val c = Ios.colors

    val errorMessage =
        uiState.error?.let { error ->
            when (error) {
                is WifiProvisionError.ConnectFailed ->
                    stringResource(Res.string.wifi_provision_connect_failed, error.detail)

                is WifiProvisionError.ScanFailed -> stringResource(Res.string.wifi_provision_scan_failed, error.detail)

                is WifiProvisionError.ProvisionFailed -> error.detail
            }
        }

    LaunchedEffect(uiState.error) { errorMessage?.let { snackbarHostState.showSnackbar(it) } }
    LaunchedEffect(Unit) { viewModel.connectToDevice(address) }

    Scaffold(
        modifier = modifier,
        containerColor = c.groupedBackground,
        topBar = {
            IosTopBar(
                title = stringResource(Res.string.wifi_provisioning),
                showTitle = true,
                background = c.groupedBackground,
                // iOS shows this as a sheet with no radio chip, and the title needs the room.
                showRadioChip = false,
                leading = {
                    IosGlassIconButton(
                        IosIcons.ChevronLeft,
                        contentDescription = stringResource(Res.string.back),
                        onClick = onNavigateUp,
                    )
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier =
            Modifier.padding(padding)
                .padding(bottom = LocalIosBottomInset.current)
                .fillMaxSize()
                .animateContentSize(),
        ) {
            MpwrdDisclaimerBanner()

            Crossfade(targetState = screenKey(uiState), label = "wifi_provision") { key ->
                when (key) {
                    ScreenKey.ConnectingBle ->
                        ScanningBleContent(
                            error = uiState.error as? WifiProvisionError.ConnectFailed,
                            onRetry = { viewModel.connectToDevice(address) },
                        )

                    ScreenKey.DeviceFound ->
                        DeviceFoundContent(
                            deviceName = uiState.deviceName,
                            onProceed = viewModel::scanNetworks,
                            onCancel = onNavigateUp,
                        )

                    ScreenKey.LoadingNetworks -> ScanningNetworksContent()

                    ScreenKey.Connected ->
                        ConnectedContent(
                            networks = uiState.networks,
                            provisionStatus = uiState.provisionStatus,
                            ipAddress = uiState.ipAddress,
                            isProvisioning = uiState.phase == Phase.Provisioning,
                            isScanning = uiState.phase == Phase.LoadingNetworks,
                            onScanNetworks = viewModel::scanNetworks,
                            onProvision = viewModel::provisionWifi,
                            onDisconnect = {
                                viewModel.disconnect()
                                onNavigateUp()
                            },
                        )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Screen-key helper for Crossfade
// ---------------------------------------------------------------------------

private enum class ScreenKey {
    ConnectingBle,
    DeviceFound,
    LoadingNetworks,
    Connected,
}

private fun screenKey(state: WifiProvisionUiState): ScreenKey = when (state.phase) {
    Phase.Idle,
    Phase.ConnectingBle,
    -> ScreenKey.ConnectingBle

    Phase.DeviceFound -> ScreenKey.DeviceFound

    Phase.LoadingNetworks -> if (state.networks.isEmpty()) ScreenKey.LoadingNetworks else ScreenKey.Connected

    Phase.Connected,
    Phase.Provisioning,
    -> ScreenKey.Connected
}

// ---------------------------------------------------------------------------
// Sub-composables
// ---------------------------------------------------------------------------

/** BLE scanning spinner — shown while searching for a device. */
@Composable
internal fun ScanningBleContent(error: WifiProvisionError.ConnectFailed? = null, onRetry: () -> Unit = {}) {
    CenteredStatusContent {
        if (error != null) {
            Icon(
                IosIcons.BluetoothRune,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Ios.colors.systemRed,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(Res.string.wifi_provision_connect_failed, error.detail),
                style = Ios.type.body,
                color = Ios.colors.systemRed,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            IosButton(
                text = stringResource(Res.string.retry),
                onClick = onRetry,
                style = IosButtonStyle.Tinted,
                icon = IosIcons.ArrowsCirclepath,
            )
        } else {
            IosActivityIndicator(size = 32.dp, color = Ios.colors.secondaryLabel)
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(Res.string.wifi_provision_scanning_ble),
                style = Ios.type.body,
                color = Ios.colors.secondaryLabel,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Confirmation step shown after BLE device discovery — the Android analog of the web flasher's native BLE pairing
 * prompt. Gives the user a clear "device found" moment before proceeding.
 */
@Composable
internal fun DeviceFoundContent(deviceName: String?, onProceed: () -> Unit, onCancel: () -> Unit) {
    CenteredStatusContent {
        Icon(IosIcons.BluetoothRune, contentDescription = null, modifier = Modifier.size(64.dp), tint = Ios.colors.tint)
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(Res.string.wifi_provision_device_found),
            style = Ios.type.title2,
            color = Ios.colors.label,
            textAlign = TextAlign.Center,
        )
        if (deviceName != null) {
            Spacer(Modifier.height(4.dp))
            Text(deviceName, style = Ios.type.body, color = Ios.colors.secondaryLabel, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(Res.string.wifi_provision_device_found_detail),
            style = Ios.type.subheadline,
            color = Ios.colors.secondaryLabel,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IosButton(text = stringResource(Res.string.cancel), onClick = onCancel, style = IosButtonStyle.Gray)
            IosButton(
                text = stringResource(Res.string.wifi_provision_scan_networks),
                onClick = onProceed,
                style = IosButtonStyle.Filled,
                icon = IosIcons.Wifi,
            )
        }
    }
}

/** Network scanning spinner — shown during the initial scan when no networks are loaded yet. */
@Composable
internal fun ScanningNetworksContent() {
    CenteredStatusContent {
        IosActivityIndicator(size = 32.dp, color = Ios.colors.secondaryLabel)
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(Res.string.wifi_provision_scanning_wifi),
            style = Ios.type.body,
            color = Ios.colors.secondaryLabel,
        )
    }
}

/**
 * Main configuration screen shown after BLE connection — mirrors the web flasher's connected state. All controls (scan
 * button, network list, SSID/password fields, Apply, status) are on one screen, as iOS grouped sections.
 */
@Suppress("LongMethod", "LongParameterList")
@Composable
internal fun ConnectedContent(
    networks: List<WifiNetwork>,
    provisionStatus: ProvisionStatus,
    ipAddress: String?,
    isProvisioning: Boolean,
    isScanning: Boolean,
    onScanNetworks: () -> Unit,
    onProvision: (ssid: String, password: String, hidden: Boolean) -> Unit,
    onDisconnect: () -> Unit,
) {
    if (provisionStatus == ProvisionStatus.Success) {
        ProvisionSuccessContent(ipAddress = ipAddress, onDone = onDisconnect)
        return
    }

    var ssid by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var hiddenNetwork by rememberSaveable { mutableStateOf(false) }
    val c = Ios.colors

    val haptic = LocalHapticFeedback.current
    LaunchedEffect(provisionStatus) {
        if (provisionStatus == ProvisionStatus.Success) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Text(
            stringResource(Res.string.wifi_provision_description),
            style = Ios.type.footnote,
            color = c.secondaryLabel,
            modifier = Modifier.padding(start = IosInset * 2, end = IosInset * 2, top = 8.dp),
        )

        IosSection {
            IosRow(
                title =
                stringResource(
                    if (isScanning) {
                        Res.string.wifi_provision_scanning_wifi
                    } else {
                        Res.string.wifi_provision_scan_networks
                    },
                ),
                icon = IosIcons.Wifi,
                titleColor = if (isScanning || isProvisioning) c.tertiaryLabel else c.tint,
                accessory =
                if (isScanning) IosAccessory.Custom { IosActivityIndicator(size = 18.dp) } else IosAccessory.None,
                onClick = if (!isScanning && !isProvisioning) onScanNetworks else null,
            )
        }

        // The networks found, the chosen one ticked, as iOS's nearby-device list.
        AnimatedVisibility(
            visible = networks.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            IosSection(header = stringResource(Res.string.wifi_provision_available_networks)) {
                networks.forEach { network ->
                    NetworkRow(network = network, isSelected = network.ssid == ssid, onClick = { ssid = network.ssid })
                }
            }
        }

        AnimatedVisibility(visible = networks.isEmpty() && !isScanning, enter = fadeIn(), exit = fadeOut()) {
            Text(
                stringResource(Res.string.wifi_provision_no_networks),
                style = Ios.type.footnote,
                color = c.secondaryLabel,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = IosInset * 2, vertical = 12.dp),
            )
        }

        IosSection(header = stringResource(Res.string.wifi_provision_ssid_label)) {
            IosTextField(
                value = ssid,
                onValueChange = { ssid = it },
                placeholder = stringResource(Res.string.wifi_provision_ssid_placeholder),
                containerColor = Color.Transparent,
                modifier = Modifier.iosRowSeparator().padding(horizontal = 2.dp),
            )
            IosTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = stringResource(Res.string.password),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onProvision(ssid, password, hiddenNetwork) }),
                visualTransformation =
                if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                containerColor = Color.Transparent,
                modifier = Modifier.iosRowSeparator().padding(horizontal = 2.dp),
                trailing = {
                    Icon(
                        if (passwordVisible) MeshtasticIcons.VisibilityOff else MeshtasticIcons.Visibility,
                        contentDescription =
                        stringResource(if (passwordVisible) Res.string.hide_password else Res.string.show_password),
                        tint = c.secondaryLabel,
                        modifier =
                        Modifier.size(22.dp).clickable(role = Role.Button) { passwordVisible = !passwordVisible },
                    )
                },
            )
            IosRow(
                title = stringResource(Res.string.wifi_provision_hidden_network),
                accessory = IosAccessory.Switch(checked = hiddenNetwork, onCheckedChange = { hiddenNetwork = it }),
            )
        }

        // Inline provision status (matches web flasher's status chip) — animated entrance
        AnimatedVisibility(
            visible = provisionStatus != ProvisionStatus.Idle || isProvisioning,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(Modifier.padding(horizontal = IosInset).padding(top = 20.dp)) {
                ProvisionStatusCard(provisionStatus = provisionStatus, isProvisioning = isProvisioning)
            }
        }

        // Cancel in grey, Apply in the tint, as iOS's capsule pair.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = IosInset).padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IosButton(text = stringResource(Res.string.cancel), onClick = onDisconnect, style = IosButtonStyle.Gray)
            IosButton(
                text =
                stringResource(
                    if (isProvisioning) Res.string.wifi_provision_sending_credentials else Res.string.apply,
                ),
                onClick = { onProvision(ssid, password, hiddenNetwork) },
                style = IosButtonStyle.Filled,
                enabled = ssid.isNotBlank() && !isProvisioning,
                icon = IosIcons.Wifi,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ProvisionSuccessContent(ipAddress: String?, onDone: () -> Unit) {
    val openUrl = rememberOpenUrl()
    val c = Ios.colors
    val defaultUsername = stringResource(Res.string.wifi_provision_success_username_value)
    val defaultPassword = stringResource(Res.string.wifi_provision_success_password_value)
    val resolvedIp = ipAddress ?: stringResource(Res.string.wifi_provision_success_missing_ip)
    val sshCommand =
        ipAddress?.let { stringResource(Res.string.wifi_provision_success_ssh_command, defaultUsername, it) }
            ?: stringResource(Res.string.wifi_provision_success_ssh_unavailable)
    val sshUri = ipAddress?.let { "ssh://$defaultUsername@$it" }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = IosIcons.CheckCircleFill,
            contentDescription = null,
            tint = c.systemGreen,
            modifier = Modifier.size(64.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.wifi_provision_success_device_connected),
            style = Ios.type.title2,
            color = c.label,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(Res.string.wifi_provision_success_description),
            style = Ios.type.subheadline,
            textAlign = TextAlign.Center,
            color = c.secondaryLabel,
            modifier = Modifier.padding(horizontal = IosInset * 2),
        )

        IosSection {
            ProvisionInfoItem(
                label = stringResource(Res.string.wifi_provision_success_ip_address),
                value = resolvedIp,
                copyEnabled = ipAddress != null,
            )
        }

        IosSection(
            header = stringResource(Res.string.wifi_provision_success_setup_title),
            footer = stringResource(Res.string.wifi_provision_success_open_ssh_fallback),
        ) {
            Text(
                text = stringResource(Res.string.wifi_provision_success_setup_description),
                style = Ios.type.subheadline,
                color = c.secondaryLabel,
                modifier = Modifier.fillMaxWidth().padding(horizontal = IosInset, vertical = 11.dp),
            )
            ProvisionInfoItem(
                label = stringResource(Res.string.wifi_provision_success_username),
                value = defaultUsername,
            )
            ProvisionInfoItem(label = stringResource(Res.string.password), value = defaultPassword)
            ProvisionInfoItem(
                label = stringResource(Res.string.wifi_provision_success_ssh_label),
                value = sshCommand,
                copyEnabled = ipAddress != null,
            )
            IosRow(
                title = stringResource(Res.string.wifi_provision_success_open_ssh),
                icon = IosIcons.Terminal,
                titleColor = if (sshUri != null) c.tint else c.tertiaryLabel,
                onClick = sshUri?.let { uri -> { openUrl(uri) } },
            )
        }

        IosButton(
            text = stringResource(Res.string.wifi_provision_success_done),
            onClick = onDone,
            style = IosButtonStyle.Filled,
            large = true,
            modifier = Modifier.padding(horizontal = IosInset).padding(top = 24.dp),
        )
    }
}

/** A credential row as iOS draws it: the label small and grey above the value, a copy button at the end. */
@Composable
private fun ProvisionInfoItem(label: String, value: String, copyEnabled: Boolean = true) {
    Row(
        modifier =
        Modifier.iosRowSeparator()
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(start = IosInset, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = label, style = Ios.type.footnote, color = Ios.colors.secondaryLabel)
            Text(text = value, style = Ios.type.body, color = Ios.colors.label)
        }
        if (copyEnabled) {
            CopyIconButton(valueToCopy = value)
        }
    }
}

/** One network found: its name, signal strength under it, a lock when it needs a password, a tick when chosen. */
@Composable
internal fun NetworkRow(network: WifiNetwork, isSelected: Boolean, onClick: () -> Unit) {
    val c = Ios.colors
    IosRow(
        title = network.ssid,
        subtitle = stringResource(Res.string.wifi_provision_signal_strength, network.signalStrength),
        icon = IosIcons.Wifi,
        iconTint = c.secondaryLabel,
        accessory =
        when {
            isSelected -> IosAccessory.Checkmark

            network.isProtected ->
                IosAccessory.Custom {
                    Icon(
                        IosIcons.LockOutline,
                        contentDescription = stringResource(Res.string.password),
                        tint = c.secondaryLabel,
                        modifier = Modifier.size(18.dp),
                    )
                }

            else -> IosAccessory.None
        },
        onClick = onClick,
    )
}

// ---------------------------------------------------------------------------
// mPWRD-OS disclaimer banner
// ---------------------------------------------------------------------------

private const val MPWRD_LOGO_SIZE_DP = 40

/** Branded disclaimer banner shown at the top of the provisioning screen, in an iOS card. */
@Composable
internal fun MpwrdDisclaimerBanner() {
    Row(
        modifier =
        Modifier.fillMaxWidth()
            .padding(horizontal = IosInset, vertical = 8.dp)
            .clip(IosCardShape)
            .background(Ios.colors.groupedRow)
            .padding(horizontal = IosInset, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.img_mpwrd_logo),
            contentDescription = stringResource(Res.string.mpwrd_os),
            modifier = Modifier.size(MPWRD_LOGO_SIZE_DP.dp).clip(RoundedCornerShape(8.dp)),
        )
        AutoLinkText(
            text = stringResource(Res.string.wifi_provision_mpwrd_disclaimer),
            style = Ios.type.footnote,
            color = Ios.colors.secondaryLabel,
            textAlign = TextAlign.Start,
        )
    }
}

// ---------------------------------------------------------------------------
// Shared layout wrapper for centered status screens
// ---------------------------------------------------------------------------

@Composable
private fun CenteredStatusContent(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = IosInset * 2),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@PreviewLightDark
@Composable
private fun ProvisionSuccessContentPreview() {
    AppTheme { ProvisionSuccessContent(ipAddress = "192.168.1.100", onDone = {}) }
}
