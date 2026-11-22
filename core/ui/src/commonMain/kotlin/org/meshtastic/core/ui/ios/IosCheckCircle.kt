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
package org.meshtastic.core.ui.ios

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * iOS's selection circle, its stand-in for a checkbox (a list in edit mode, Mail's selection): a tinted tick when
 * [checked], an empty grey ring otherwise. Without [onCheckedChange] it only draws, for a row that toggles itself.
 */
@Composable
fun IosCheckCircle(
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    val toggle =
        if (onCheckedChange != null) {
            Modifier.toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange,
            )
        } else {
            Modifier
        }
    Box(
        modifier = modifier.size(28.dp).alpha(if (enabled) 1f else DISABLED_ALPHA).then(toggle),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
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
