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

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.DeviceType
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.bluetooth
import org.meshtastic.core.resources.network
import org.meshtastic.core.resources.usb
import org.meshtastic.core.ui.ios.IosSegmentedControl

/**
 * Single-choice transport selector rendered below the connection card. A Material 3 [SingleChoiceSegmentedButtonRow]
 * makes the mutually-exclusive choice explicit: the segments read as one grouped control and the selected transport
 * shows a check, rather than three independent chips whose filled state was read as "enabled/available" instead of
 * "selected".
 *
 * @param showBluetooth false on hardware with no Bluetooth LE, where the BLE segment could never find anything.
 * @param showUsb false on hardware with no USB host, unless Demo Mode needs the USB pane.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportSelector(
    activeTransport: DeviceType,
    onSelectTransport: (DeviceType) -> Unit,
    modifier: Modifier = Modifier,
    showBluetooth: Boolean = true,
    showUsb: Boolean = true,
) {
    val transports =
        DeviceType.entries.filter { (it != DeviceType.BLE || showBluetooth) && (it != DeviceType.USB || showUsb) }
    // Pangea: the iOS segmented control, as on the iPhone app's Connect screen.
    IosSegmentedControl(
        options = transports.map { stringResource(it.label) },
        selectedIndex = transports.indexOf(activeTransport).coerceAtLeast(0),
        onSelect = { onSelectTransport(transports[it]) },
        modifier = modifier,
    )
}

private val DeviceType.label: StringResource
    get() =
        when (this) {
            DeviceType.BLE -> Res.string.bluetooth
            DeviceType.TCP -> Res.string.network
            DeviceType.USB -> Res.string.usb
        }
