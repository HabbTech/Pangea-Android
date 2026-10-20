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
package org.meshtastic.feature.discovery.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.schemaLabelRes
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.mesh_beacon_preset_indicator
import org.meshtastic.core.resources.pangea_discovery_beacon_channels
import org.meshtastic.core.resources.pangea_discovery_beacon_footer
import org.meshtastic.core.ui.icon.Lock
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosAccessory
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.feature.discovery.BeaconChannel

/**
 * Scan-setup section listing distinct custom channels that beacons advertised (Apple 014-mesh-beacons FR-007), drawn as
 * the iPhone app's: a lock, the channel's name over its preset, the beacon symbol, and a check while chosen. Selecting
 * a row adds a custom-channel scan target that tunes the radio's primary channel to that name during its dwell.
 */
@Composable
fun BeaconChannelsCard(
    channels: List<BeaconChannel>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val c = Ios.colors
    val beaconDesc = stringResource(Res.string.mesh_beacon_preset_indicator)
    Column(modifier.fillMaxWidth()) {
        IosSection(
            footer = stringResource(Res.string.pangea_discovery_beacon_footer),
            headerContent = {
                DiscoverySectionHeader(
                    IosIcons.RadiowavesLeftRight,
                    stringResource(Res.string.pangea_discovery_beacon_channels),
                    inset = 12.dp,
                )
            },
        ) {
            channels.forEach { channel ->
                val selected = channel.id in selectedIds
                IosRow(
                    title = channel.name,
                    subtitle =
                    channel.preset?.let { preset ->
                        preset.modemPreset.schemaLabelRes()?.let { stringResource(it) } ?: preset.displayName()
                    },
                    leading = {
                        Icon(
                            MeshtasticIcons.Lock,
                            contentDescription = null,
                            tint = c.secondaryLabel,
                            modifier = Modifier.size(14.dp),
                        )
                    },
                    leadingWidth = 40.dp,
                    accessory =
                    IosAccessory.Custom {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                IosIcons.RadiowavesLeftRight,
                                contentDescription = beaconDesc,
                                tint = c.systemBlue,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(16.dp))
                            Icon(
                                IosIcons.Checkmark,
                                contentDescription = null,
                                tint = if (selected) c.systemBlue else c.systemBlue.copy(alpha = 0f),
                                modifier = Modifier.padding(end = 4.dp).size(20.dp),
                            )
                        }
                    },
                    onClick = if (enabled) ({ onToggle(channel.id) }) else null,
                )
            }
        }
    }
}
