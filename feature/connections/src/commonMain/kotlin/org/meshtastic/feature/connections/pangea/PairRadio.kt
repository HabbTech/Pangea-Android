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
package org.meshtastic.feature.connections.pangea

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.pangea.RadioAutoConfig
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.bluetooth
import org.meshtastic.core.resources.network
import org.meshtastic.core.resources.pangea_pair_code_hint
import org.meshtastic.core.resources.pangea_pair_connecting
import org.meshtastic.core.resources.pangea_pair_desc
import org.meshtastic.core.resources.pangea_pair_failed
import org.meshtastic.core.resources.pangea_pair_failed_body
import org.meshtastic.core.resources.pangea_pair_first_time
import org.meshtastic.core.resources.pangea_pair_looking
import org.meshtastic.core.resources.pangea_pair_title
import org.meshtastic.core.resources.pangea_pair_try_again
import org.meshtastic.core.resources.pangea_radio
import org.meshtastic.core.resources.usb
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosActivityIndicator
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.iosPressable
import org.meshtastic.core.ui.viewmodel.ConnectionsViewModel
import org.meshtastic.feature.connections.ScannerViewModel
import org.meshtastic.feature.connections.model.DeviceListEntry
import kotlin.time.Duration.Companion.seconds

/**
 * Pair Your Radio, as on the Pangea iPhone app (PairRadioView): looks for radios nearby, connects to the one tapped and
 * waits until it has sent its settings, then calls [onPair]. Pangea radios are listed first. With
 * [finishIfAlreadyConnected] (first-run setup), a radio that is already connected moves straight on.
 *
 * Unlike iOS there is no "Switch to this radio?" warning: Android keeps each radio's chats in its own database, so
 * switching doesn't remove any.
 */
@Suppress("LongMethod")
@Composable
fun PairRadioContent(
    onPair: () -> Unit,
    modifier: Modifier = Modifier,
    finishIfAlreadyConnected: Boolean = true,
    scanModel: ScannerViewModel = koinViewModel(),
    connectionsViewModel: ConnectionsViewModel = koinViewModel(),
) {
    val c = Ios.colors
    val bleDevices by scanModel.bleDevicesForUi.collectAsStateWithLifecycle()
    val networkDevices by scanModel.discoveredTcpDevicesForUi.collectAsStateWithLifecycle()
    val usbDevices by scanModel.usbDevicesForUi.collectAsStateWithLifecycle()
    // Demo Mode, last: only offered in debug builds or with hidden features unlocked, as on the Connect screen.
    val demoDevices by scanModel.virtualDevicesForUi.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<DeviceListEntry?>(null) }
    var failed by remember { mutableStateOf(false) }
    var finished by remember { mutableStateOf(false) }

    val currentOnPair by rememberUpdatedState(onPair)

    fun finish() {
        if (!finished) {
            finished = true
            currentOnPair()
        }
    }

    LaunchedEffect(Unit) {
        if (finishIfAlreadyConnected && connectionsViewModel.connectionState.value is ConnectionState.Connected) {
            finish()
        }
    }
    DisposableEffect(Unit) {
        scanModel.startBleScan()
        scanModel.startNetworkScan()
        onDispose {
            scanModel.stopBleScan()
            scanModel.stopNetworkScan()
        }
    }
    // Ready once the picked radio has finished its handshake (settings and radios nearby downloaded).
    LaunchedEffect(selected) {
        if (selected == null) return@LaunchedEffect
        val ready =
            withTimeoutOrNull(CONNECT_TIMEOUT) {
                connectionsViewModel.connectionState
                    .dropWhile { it is ConnectionState.Connected }
                    .first { it is ConnectionState.Connected }
            }
        if (ready != null) {
            finish()
        } else {
            failed = true
            selected = null
            scanModel.startBleScan()
        }
    }

    val radios =
        (bleDevices + networkDevices + usbDevices)
            .distinctBy { it.fullAddress }
            .sortedByDescending { RadioAutoConfig.isPangeaRadio(it.name) } + demoDevices
    val working = selected != null && !finished

    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            IosIcons.Antenna,
            contentDescription = null,
            tint = c.tint,
            modifier = Modifier.padding(top = 24.dp).size(56.dp),
        )
        Text(
            stringResource(Res.string.pangea_pair_title),
            style = Ios.type.largeTitle,
            color = c.label,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            stringResource(Res.string.pangea_pair_desc),
            style = Ios.type.body,
            color = c.secondaryLabel,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Spacer(Modifier.size(24.dp))
        when {
            working -> Progress()

            else -> {
                if (failed) ErrorCard()
                if (radios.isEmpty()) {
                    Looking()
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        radios.forEach { entry ->
                            RadioRow(entry) {
                                failed = false
                                // An unpaired radio asks for Android's pairing dialog first, then connects by itself.
                                scanModel.onSelected(entry)
                                selected = entry
                            }
                        }
                    }
                    Text(
                        stringResource(Res.string.pangea_pair_code_hint),
                        style = Ios.type.footnote,
                        color = c.secondaryLabel,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Looking() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IosActivityIndicator(color = Ios.colors.secondaryLabel)
        Text(stringResource(Res.string.pangea_pair_looking), style = Ios.type.body, color = Ios.colors.secondaryLabel)
    }
}

@Composable
private fun Progress() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IosActivityIndicator(color = Ios.colors.secondaryLabel, size = 32.dp)
        Text(stringResource(Res.string.pangea_pair_connecting), style = Ios.type.headline, color = Ios.colors.label)
        Text(
            stringResource(Res.string.pangea_pair_first_time),
            style = Ios.type.footnote,
            color = Ios.colors.secondaryLabel,
        )
    }
}

@Composable
private fun ErrorCard() {
    val c = Ios.colors
    val orange = c.systemOrange
    Column(
        Modifier.fillMaxWidth()
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(orange.copy(alpha = 0.12f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(IosIcons.WarningFill, contentDescription = null, tint = orange, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.pangea_pair_failed), style = Ios.type.headline, color = orange)
        }
        Text(stringResource(Res.string.pangea_pair_failed_body), style = Ios.type.subheadline, color = c.secondaryLabel)
        Text(stringResource(Res.string.pangea_pair_try_again), style = Ios.type.subheadline, color = c.label)
    }
}

@Composable
private fun RadioRow(entry: DeviceListEntry, onClick: () -> Unit) {
    val c = Ios.colors
    val serial = RadioAutoConfig.pangeaSerial(entry.name)
    val transport =
        when (entry) {
            is DeviceListEntry.Ble -> stringResource(Res.string.bluetooth)
            is DeviceListEntry.Usb -> stringResource(Res.string.usb)
            else -> stringResource(Res.string.network)
        }
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(c.secondaryFill)
            .iosPressable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (serial != null) IosIcons.CheckSeal else IosIcons.RadioWaves,
            contentDescription = null,
            tint = if (serial != null) c.tint else c.secondaryLabel,
            modifier = Modifier.width(36.dp).size(28.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (serial != null) stringResource(Res.string.pangea_radio) else entry.name,
                style = Ios.type.headline.copy(fontWeight = FontWeight.SemiBold),
                color = c.label,
            )
            Text(serial ?: transport, style = Ios.type.subheadline, color = c.secondaryLabel)
        }
        Icon(IosIcons.ChevronRight, contentDescription = null, tint = c.tertiaryLabel, modifier = Modifier.size(16.dp))
    }
}

/** How long to wait for a picked radio to connect and send its settings before offering the list again. */
private val CONNECT_TIMEOUT = 90.seconds
