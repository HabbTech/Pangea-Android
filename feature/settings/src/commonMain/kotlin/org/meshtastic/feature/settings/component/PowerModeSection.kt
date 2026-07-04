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
package org.meshtastic.feature.settings.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.model.PowerMode
import org.meshtastic.core.repository.PowerModeManager
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.power_mode
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.icon.Check
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.util.icon
import org.meshtastic.core.ui.util.labelRes
import org.meshtastic.core.ui.util.summaryRes

/**
 * Power Mode picker: three battery-profile tiers with icon + summary. Selection persists immediately via
 * [PowerModeManager]; the OS battery saver may clamp the effective mode above the selection.
 */
@Composable
fun PowerModeSection() {
    val powerModeManager: PowerModeManager = koinInject()
    val selectedMode by powerModeManager.selectedMode.collectAsState()

    ExpressiveSection(title = stringResource(Res.string.power_mode)) {
        PowerMode.entries.forEach { mode ->
            ListItem(
                text = stringResource(mode.labelRes),
                supportingText = stringResource(mode.summaryRes),
                leadingIcon = mode.icon,
                trailingIcon = if (mode == selectedMode) MeshtasticIcons.Check else null,
            ) {
                powerModeManager.setSelectedMode(mode)
            }
        }
    }
}
