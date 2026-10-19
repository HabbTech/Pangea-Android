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
package org.meshtastic.feature.connections.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withTimeout
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.ble.BleConnectionState
import org.meshtastic.core.model.DeviceType
import org.meshtastic.core.model.Node
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.disconnect
import org.meshtastic.core.resources.pangea_connection_name
import org.meshtastic.core.resources.rssi
import org.meshtastic.core.resources.unknown
import org.meshtastic.core.ui.component.MaterialBatteryInfo
import org.meshtastic.core.ui.component.Rssi
import org.meshtastic.core.ui.icon.Bluetooth
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Usb
import org.meshtastic.core.ui.icon.Wifi
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.iosPressable
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.feature.connections.model.DeviceListEntry
import org.meshtastic.proto.EnvironmentMetrics
import org.meshtastic.proto.Paxcount
import org.meshtastic.proto.User
import kotlin.time.Duration.Companion.seconds

private const val RSSI_DELAY = 3
private const val RSSI_TIMEOUT = 1

/**
 * Plain text resolved outside the animated connected-device subtree.
 *
 * TODO(CMP-6615): Remove this bundle and the caller-provided component labels after upgrading from Compose
 *   Multiplatform 1.11.1 to a release containing the Android resource-loading fix.
 */
@Immutable
data class CurrentlyConnectedText(
    val unknownLabel: String,
    val rssiLabel: String,
    val disconnectLabel: String,
    val firmwareVersion: String?,
)

@Suppress("LoopWithTooManyJumpStatements", "TooGenericExceptionCaught")
@Composable
fun CurrentlyConnectedInfo(
    node: Node,
    text: CurrentlyConnectedText,
    onNavigateToNodeDetails: (Int) -> Unit,
    onClickDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
    bleDevice: DeviceListEntry.Ble? = null,
    transport: DeviceType? = null,
) {
    // Null until the first successful read: 0 dBm is the strongest value on this scale, not "unknown".
    var rssi by remember(bleDevice?.device?.address) { mutableStateOf<Int?>(null) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(bleDevice, lifecycle) {
        if (bleDevice == null) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            bleDevice.device.state.collectLatest { state ->
                if (state != BleConnectionState.Connected) {
                    if (rssi != null) rssi = null
                    return@collectLatest
                }
                while (true) {
                    try {
                        val latestRssi = withTimeout(RSSI_TIMEOUT.seconds) { bleDevice.device.readRssi() }
                        if (latestRssi != rssi) rssi = latestRssi
                    } catch (_: TimeoutCancellationException) {
                        Logger.d { "RSSI read timed out" }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Logger.d(e) { "Failed to read RSSI" }
                    }
                    delay(RSSI_DELAY.seconds)
                }
            }
        }
    }
    // Pangea: the iPhone app's connected-radio card (its avatar, "Connection Name" and the transport). Android keeps
    // the battery, signal and firmware under them, and a visible Disconnect where iOS uses a swipe.
    val c = Ios.colors
    Column(modifier = modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(96.dp).clip(CircleShape).background(Color(node.colors.second)).clickable {
                    onNavigateToNodeDetails(node.num)
                },
                contentAlignment = Alignment.Center,
            ) {
                Text(node.user.short_name, style = Ios.type.title1, color = Color(node.colors.first))
            }
            Spacer(Modifier.width(20.dp))
            ConnectionFacts(
                node = node,
                text = text,
                transport = transport,
                rssi = rssi,
                showRssi = bleDevice != null,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = text.disconnectLabel,
            style = Ios.type.body,
            color = c.systemRed,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().iosPressable(onClick = onClickDisconnect).padding(vertical = 8.dp),
        )
    }
}

/** What's known about the connected radio: its name, how it's connected, its battery and signal, and its firmware. */
@Composable
private fun ConnectionFacts(
    node: Node,
    text: CurrentlyConnectedText,
    transport: DeviceType?,
    rssi: Int?,
    showRssi: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = Ios.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(Res.string.pangea_connection_name, node.user.long_name),
            style = Ios.type.body,
            color = c.secondaryLabel,
        )
        transport?.let { type ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(type.icon, contentDescription = null, tint = c.tint, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(6.dp))
                Text(type.name, style = Ios.type.title2, color = c.secondaryLabel)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            MaterialBatteryInfo(level = node.batteryLevel, voltage = node.voltage, unknownLabel = text.unknownLabel)
            if (showRssi) {
                Rssi(rssi = rssi, label = text.rssiLabel)
            }
        }
        text.firmwareVersion?.let { firmwareVersion ->
            Text(
                text = firmwareVersion,
                style = Ios.type.footnote,
                color = c.secondaryLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private val DeviceType.icon: ImageVector
    @Composable
    get() =
        when (this) {
            DeviceType.BLE -> MeshtasticIcons.Bluetooth
            DeviceType.TCP -> MeshtasticIcons.Wifi
            DeviceType.USB -> MeshtasticIcons.Usb
        }

@Suppress("MagicNumber", "UnusedPrivateMember")
@Composable
private fun CurrentlyConnectedInfoPreview() {
    AppTheme {
        CurrentlyConnectedInfo(
            node =
            Node(
                num = 13444,
                user =
                User.Builder()
                    .also { wb ->
                        wb.short_name = "\uD83E\uDEE0"
                        wb.long_name = "John Doe"
                    }
                    .build(),
                isIgnored = false,
                paxcounter =
                Paxcount.Builder()
                    .also { wb ->
                        wb.ble = 10
                        wb.wifi = 5
                    }
                    .build(),
                environmentMetrics =
                EnvironmentMetrics.Builder()
                    .also { wb ->
                        wb.temperature = 25f
                        wb.relative_humidity = 60f
                    }
                    .build(),
            ),
            text =
            CurrentlyConnectedText(
                unknownLabel = stringResource(Res.string.unknown),
                rssiLabel = stringResource(Res.string.rssi),
                disconnectLabel = stringResource(Res.string.disconnect),
                firmwareVersion = null,
            ),
            onNavigateToNodeDetails = {},
            onClickDisconnect = {},
        )
    }
}
