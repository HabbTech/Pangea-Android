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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosStepper

/**
 * A whole-number setting as the iPhone app's forms draw one with a small range: the title, the value in grey, and a −/+
 * stepper that keeps it inside [range].
 */
@Composable
fun StepperPreference(
    title: String,
    value: Int,
    range: IntRange,
    enabled: Boolean,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    PreferenceRow(
        title = title,
        modifier = modifier,
        summary = summary,
        enabled = enabled,
        trailing = {
            Text(value.toString(), style = Ios.type.body, color = Ios.colors.secondaryLabel)
            Spacer(Modifier.width(10.dp))
            IosStepper(
                value = value.coerceIn(range),
                onValueChange = { if (it in range) onValueChange(it) },
                range = range,
                enabled = enabled,
            )
        },
    )
}
