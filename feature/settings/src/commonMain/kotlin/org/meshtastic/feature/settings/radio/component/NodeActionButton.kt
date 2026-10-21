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
package org.meshtastic.feature.settings.radio.component

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.meshtastic.core.ui.ios.IosButton

@Composable
fun NodeActionButton(
    modifier: Modifier = Modifier,
    title: String,
    enabled: Boolean,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    // iOS's large capsule; the symbol takes the words' colour, as iOS's does.
    IosButton(
        text = title,
        onClick = onClick,
        modifier = modifier.then(Modifier.padding(vertical = 4.dp)),
        enabled = enabled,
        large = true,
        icon = icon,
    )
}
