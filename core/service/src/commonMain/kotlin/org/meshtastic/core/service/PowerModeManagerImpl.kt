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
package org.meshtastic.core.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.Single
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.model.PowerMode
import org.meshtastic.core.repository.PowerModeManager
import org.meshtastic.core.repository.SystemPowerSaveSource
import org.meshtastic.core.repository.UiPrefs

/** Default [PowerModeManager]: user selection from [UiPrefs] clamped by the platform battery-saver state. */
@Single
class PowerModeManagerImpl(
    private val uiPrefs: UiPrefs,
    powerSaveSource: SystemPowerSaveSource,
    dispatchers: CoroutineDispatchers,
) : PowerModeManager {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)

    override val selectedMode: StateFlow<PowerMode> = uiPrefs.powerMode

    override val systemPowerSave: StateFlow<Boolean> = powerSaveSource.isPowerSaveMode

    override val effectiveMode: StateFlow<PowerMode> =
        combine(selectedMode, systemPowerSave) { selected, powerSave ->
            if (powerSave) PowerMode.mostAggressive(selected, PowerMode.EXPEDITION) else selected
        }
            .stateIn(scope, SharingStarted.Eagerly, selectedMode.value)

    override fun setSelectedMode(mode: PowerMode) {
        uiPrefs.setPowerMode(mode)
    }
}
