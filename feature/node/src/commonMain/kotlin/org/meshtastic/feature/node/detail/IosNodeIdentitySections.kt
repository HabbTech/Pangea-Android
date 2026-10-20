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
package org.meshtastic.feature.node.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.MetricFormatter
import org.meshtastic.core.model.DeviceHardware
import org.meshtastic.core.model.HardwareSupportTier
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeSecurityIndicator
import org.meshtastic.core.model.supportTier
import org.meshtastic.core.model.util.formatUptime
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.copy
import org.meshtastic.core.resources.name
import org.meshtastic.core.resources.node_number
import org.meshtastic.core.resources.node_sort_last_heard
import org.meshtastic.core.resources.pangea_community_hardware
import org.meshtastic.core.resources.pangea_connected_node
import org.meshtastic.core.resources.pangea_firmware_version
import org.meshtastic.core.resources.pangea_maker_hardware
import org.meshtastic.core.resources.pangea_messaging
import org.meshtastic.core.resources.pangea_no_public_key
import org.meshtastic.core.resources.pangea_no_public_key_text
import org.meshtastic.core.resources.pangea_node_section
import org.meshtastic.core.resources.pangea_public_key_mismatch
import org.meshtastic.core.resources.pangea_public_key_mismatch_text
import org.meshtastic.core.resources.pangea_role
import org.meshtastic.core.resources.pangea_signal_value
import org.meshtastic.core.resources.pangea_signed_node
import org.meshtastic.core.resources.pangea_supported_hardware
import org.meshtastic.core.resources.pangea_this_is_your_radio
import org.meshtastic.core.resources.pangea_unmonitored
import org.meshtastic.core.resources.pangea_user_id
import org.meshtastic.core.resources.pangea_verified_by_radio
import org.meshtastic.core.resources.pangea_verified_contact
import org.meshtastic.core.resources.pangea_verified_in_person
import org.meshtastic.core.resources.public_key
import org.meshtastic.core.resources.status_message
import org.meshtastic.core.resources.uptime
import org.meshtastic.core.ui.component.determineSignalQuality
import org.meshtastic.core.ui.component.label
import org.meshtastic.core.ui.icon.Lock
import org.meshtastic.core.ui.icon.LockOpen
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.role
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosAccessory
import org.meshtastic.core.ui.ios.IosBatteryGauge
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.ios.iosPressable
import org.meshtastic.core.ui.util.LocalModemPreset
import org.meshtastic.core.ui.util.LocalNoiseFloor
import org.meshtastic.core.ui.util.createClipEntry
import org.meshtastic.core.ui.util.formatAgo
import org.meshtastic.feature.node.component.DeviceHardwareImage
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * The radio's hardware as the iPhone app's node detail opens: a large picture with its support rosette and the model
 * name, under a header naming how well the hardware is supported.
 */
@Composable
internal fun NodeHardwareSection(deviceHardware: DeviceHardware, reportedTarget: String?) {
    val title =
        when (deviceHardware.supportTier) {
            HardwareSupportTier.SUPPORTED -> stringResource(Res.string.pangea_supported_hardware)
            HardwareSupportTier.MAKER -> stringResource(Res.string.pangea_maker_hardware)
            HardwareSupportTier.COMMUNITY -> stringResource(Res.string.pangea_community_hardware)
        }
    val supported = deviceHardware.supportTier == HardwareSupportTier.SUPPORTED
    IosSection(header = title) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(200.dp)) {
                DeviceHardwareImage(deviceHardware, Modifier.fillMaxWidth().height(200.dp))
                Icon(
                    if (supported) IosIcons.CheckSeal else IosIcons.Xmark,
                    contentDescription = null,
                    tint = if (supported) Ios.colors.systemGreen else Ios.colors.secondaryLabel,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).size(26.dp),
                )
            }
            Text(
                text = reportedTarget?.let { "${deviceHardware.displayName} ($it)" } ?: deviceHardware.displayName,
                style = Ios.type.headline,
                color = Ios.colors.label,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * The "Node" section: the node's colour badge with its signal (when heard directly) and battery, then a row each for
 * its identity, key, firmware, role, status and when it was heard, in the iPhone app's order.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
internal fun NodeIdentitySection(node: Node, isLocal: Boolean) {
    val c = Ios.colors
    val indicator = NodeSecurityIndicator.of(node, isOwnNode = isLocal)
    IosSection(header = stringResource(Res.string.pangea_node_section)) {
        NodeBadgeRow(node)
        if (indicator == NodeSecurityIndicator.KEY_MISMATCH) {
            IosRow(
                title = stringResource(Res.string.pangea_public_key_mismatch),
                titleColor = c.systemRed,
                subtitle = stringResource(Res.string.pangea_public_key_mismatch_text),
                icon = IosIcons.WarningFill,
                iconTint = c.systemRed,
                showSeparator = false,
            )
        }
        IosRow(
            title = stringResource(Res.string.name),
            icon = IosIcons.PersonCropCircle,
            value = node.user.long_name.ifEmpty { "—" },
            showSeparator = indicator == NodeSecurityIndicator.KEY_MISMATCH,
        )
        IosRow(
            title = stringResource(Res.string.node_number),
            icon = IosIcons.Number,
            value = node.num.toUInt().toString(),
        )
        IosRow(title = stringResource(Res.string.pangea_user_id), icon = IosIcons.Person, value = node.user.id)
        TrustRow(indicator, isLocal)
        PublicKeyRow(node)
        node.metadata?.let { metadata ->
            IosRow(
                title = stringResource(Res.string.pangea_firmware_version),
                icon = IosIcons.Cpu,
                value = metadata.firmware_version.ifEmpty { "—" },
            )
        }
        IosRow(
            title = stringResource(Res.string.pangea_role),
            icon = MeshtasticIcons.role(node.user.role),
            value = stringResource(node.user.role.label),
        )
        node.nodeStatus
            ?.takeIf { it.isNotBlank() }
            ?.let { status ->
                IosRow(
                    title = stringResource(Res.string.status_message),
                    icon = IosIcons.TextAlignLeft,
                    value = status,
                    valueLines = STATUS_LINES,
                )
            }
        if (node.user.is_unmessagable == true) {
            IosRow(
                title = stringResource(Res.string.pangea_messaging),
                icon = IosIcons.PhoneSlash,
                value = stringResource(Res.string.pangea_unmonitored),
            )
        }
        node.deviceMetrics.uptime_seconds
            ?.takeIf { it > 0 }
            ?.let { uptime ->
                IosRow(
                    title = stringResource(Res.string.uptime),
                    icon = IosIcons.CheckCircleFill,
                    iconTint = c.systemGreen,
                    value = formatUptime(uptime),
                )
            }
        if (node.lastHeard > 0) {
            IosRow(
                title = stringResource(Res.string.node_sort_last_heard),
                icon = IosIcons.ClockArrowCirclepath,
                value = formatAgo(node.lastHeard),
            )
        }
    }
}

/** The node's colour badge, centred, with its signal when heard directly and its battery gauge beside it. */
@Composable
private fun NodeBadgeRow(node: Node) {
    val snr = node.snrOrNull
    val showSignal = snr != null && snr != 0f && !node.viaMqtt && node.hopsAway == 0
    val level = node.batteryLevel
    Row(
        Modifier.fillMaxWidth().padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(75.dp).clip(CircleShape).background(Color(node.colors.second)),
            contentAlignment = Alignment.Center,
        ) {
            Text(node.user.short_name, style = Ios.type.title3, color = Color(node.colors.first))
        }
        if (showSignal && snr != null) {
            val quality =
                determineSignalQuality(snr, LocalModemPreset.current, node.rssiOrNull, LocalNoiseFloor.current)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(Res.string.pangea_signal_value, stringResource(quality.nameRes)),
                    style = Ios.type.footnote,
                    color = Ios.colors.label,
                )
                Text("SNR ${MetricFormatter.snr(snr)}", style = Ios.type.caption1, color = quality.color())
                node.rssiOrNull?.let { rssi ->
                    Text(
                        "RSSI ${MetricFormatter.rssi(rssi)}",
                        style = Ios.type.caption1,
                        color = Ios.colors.secondaryLabel,
                    )
                }
            }
        }
        if (level != null && level > 0) {
            IosBatteryGauge(
                level = level,
                voltage = node.voltage?.takeIf { it > 0f }?.let { MetricFormatter.voltage(it) },
            )
        }
    }
}

/** One trust row, the strongest that applies, as the iPhone app shows it; a node that has earned none gets no row. */
@Composable
private fun TrustRow(indicator: NodeSecurityIndicator, isLocal: Boolean) {
    val (title, detail) =
        when {
            isLocal -> Res.string.pangea_connected_node to Res.string.pangea_this_is_your_radio

            indicator == NodeSecurityIndicator.VERIFIED_CONTACT ->
                Res.string.pangea_verified_contact to Res.string.pangea_verified_in_person

            indicator == NodeSecurityIndicator.SIGNED_NODE ->
                Res.string.pangea_signed_node to Res.string.pangea_verified_by_radio

            else -> return
        }
    IosRow(
        title = stringResource(title),
        icon = IosIcons.CheckSeal,
        iconTint = Ios.colors.systemGreen,
        value = stringResource(detail),
    )
}

/** The key on file: a green lock with Copy, or a yellow open lock saying why messages to the node can't be sent. */
@OptIn(ExperimentalEncodingApi::class)
@Composable
private fun PublicKeyRow(node: Node) {
    val c = Ios.colors
    val key = node.publicKey ?: node.user.public_key
    if (key.size == 0) {
        IosRow(
            title = stringResource(Res.string.pangea_no_public_key),
            subtitle = stringResource(Res.string.pangea_no_public_key_text),
            icon = MeshtasticIcons.LockOpen,
            iconTint = c.systemYellow,
        )
        return
    }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val label = stringResource(Res.string.public_key)
    val encoded = Base64.Default.encode(key.toByteArray()).trim()
    IosRow(
        title = label,
        icon = MeshtasticIcons.Lock,
        iconTint = c.systemGreen,
        accessory =
        IosAccessory.Custom {
            Row(
                Modifier.iosPressable { scope.launch { clipboard.setClipEntry(createClipEntry(encoded, label)) } },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(IosIcons.Key, contentDescription = null, tint = c.tint, modifier = Modifier.size(18.dp))
                Text(
                    stringResource(Res.string.copy),
                    style = Ios.type.body,
                    color = c.tint,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        },
    )
}

/** A status message wraps over this many lines at most, as the iPhone app caps it. */
private const val STATUS_LINES = 6
