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
package org.meshtastic.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.koinInject
import org.meshtastic.core.navigation.MultiBackstack
import org.meshtastic.core.navigation.SettingsRoute
import org.meshtastic.core.navigation.TopLevelDestination
import org.meshtastic.core.repository.PowerModeManager
import org.meshtastic.core.ui.util.PowerModeMenuState

/** Builds the nav-bar Power Mode menu state: current selection, selection handler, and About navigation. */
@Composable
internal fun rememberPowerModeMenu(multiBackstack: MultiBackstack): PowerModeMenuState {
    val powerModeManager = koinInject<PowerModeManager>()
    val selectedPowerMode by powerModeManager.selectedMode.collectAsState()
    return PowerModeMenuState(
        currentMode = selectedPowerMode,
        onSelectMode = powerModeManager::setSelectedMode,
        onAboutClick = {
            multiBackstack.navigateTopLevel(TopLevelDestination.Settings.route)
            multiBackstack.activeBackStack.add(SettingsRoute.About)
        },
    )
}
