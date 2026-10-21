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
package org.meshtastic.core.ui.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.meshtastic.core.model.Channel
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosIcons

/** A channel to keep or drop, with iOS's selection circle (a tinted tick when chosen, an empty ring otherwise). */
@Composable
fun ChannelSelection(
    index: Int,
    title: String,
    enabled: Boolean,
    isSelected: Boolean,
    onSelected: (Boolean) -> Unit,
    channel: Channel,
) = ChannelItem(index = index, title = title, enabled = enabled, titleIcon = { SecurityIcon(channel) }) {
    Spacer(modifier = Modifier.width(10.dp))
    Box(
        modifier =
        Modifier.size(28.dp)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .toggleable(
                value = isSelected,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onSelected,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Icon(
                IosIcons.CheckCircleFill,
                contentDescription = null,
                tint = Ios.colors.tint,
                modifier = Modifier.size(24.dp),
            )
        } else {
            Box(Modifier.size(22.dp).border(1.5.dp, Ios.colors.tertiaryLabel, CircleShape))
        }
    }
}

private const val DISABLED_ALPHA = 0.4f
