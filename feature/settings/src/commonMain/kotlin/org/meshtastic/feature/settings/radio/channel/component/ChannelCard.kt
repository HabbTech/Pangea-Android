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
package org.meshtastic.feature.settings.radio.channel.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.delete
import org.meshtastic.core.resources.pangea_primary_channel
import org.meshtastic.core.resources.pangea_secondary_channel
import org.meshtastic.core.ui.component.ChannelItem
import org.meshtastic.core.ui.component.SecurityIcon
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.proto.ChannelSettings
import org.meshtastic.proto.Config

@Composable
internal fun ChannelCard(
    index: Int,
    title: String,
    enabled: Boolean,
    channelSettings: ChannelSettings,
    loraConfig: Config.LoRaConfig,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    sharesLocation: Boolean,
    modifier: Modifier = Modifier,
) = ChannelItem(
    modifier = modifier,
    index = index,
    title = title,
    enabled = enabled,
    onClick = onEditClick,
    subtitle =
    stringResource(if (index == 0) Res.string.pangea_primary_channel else Res.string.pangea_secondary_channel),
    titleIcon = { SecurityIcon(channelSettings, loraConfig) },
) {
    // iOS marks the channel that carries your position with a green arrow.
    if (sharesLocation) {
        Icon(
            imageVector = IosIcons.LocationFill,
            contentDescription = stringResource(ChannelIcons.LOCATION.descriptionResId),
            tint = Ios.colors.systemGreen,
            modifier = Modifier.padding(horizontal = 5.dp).size(18.dp),
        )
    }
    if (channelSettings.uplink_enabled) {
        Icon(
            imageVector = vectorResource(ChannelIcons.UPLINK.icon),
            contentDescription = stringResource(ChannelIcons.UPLINK.descriptionResId),
            modifier = Modifier.wrapContentSize().padding(horizontal = 5.dp),
        )
    }
    if (channelSettings.downlink_enabled) {
        Icon(
            imageVector = vectorResource(ChannelIcons.DOWNLINK.icon),
            contentDescription = stringResource(ChannelIcons.DOWNLINK.descriptionResId),
            modifier = Modifier.wrapContentSize().padding(horizontal = 5.dp),
        )
    }
    Spacer(modifier = Modifier.width(6.dp))
    // Android keeps delete on the row (iOS deletes from the channel's own sheet).
    IconButton(onClick = { onDeleteClick() }) {
        Icon(
            imageVector = IosIcons.Xmark,
            contentDescription = stringResource(Res.string.delete),
            tint = Ios.colors.tertiaryLabel,
            modifier = Modifier.size(16.dp),
        )
    }
}
