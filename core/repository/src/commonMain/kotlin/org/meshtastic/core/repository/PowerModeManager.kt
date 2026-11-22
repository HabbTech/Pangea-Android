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
package org.meshtastic.core.repository

import kotlinx.coroutines.flow.StateFlow
import org.meshtastic.core.model.PowerMode

/**
 * Single source of truth for the app's three-tier power profile.
 *
 * The user picks a mode (persisted via UiPrefs); the OS battery saver clamps the [effectiveMode] up to
 * [PowerMode.EXPEDITION] while active, then drops back to the user's choice. Battery-hungry feature sites read
 * [effectiveMode] and branch on its [PowerMode] gate properties rather than on the raw enum.
 */
interface PowerModeManager {
    /** The user's persisted selection. */
    val selectedMode: StateFlow<PowerMode>

    /** True while the OS battery saver is active. */
    val systemPowerSave: StateFlow<Boolean>

    /** [selectedMode], clamped up to [PowerMode.EXPEDITION] while the OS battery saver is on. */
    val effectiveMode: StateFlow<PowerMode>

    /** Persists the user's selection and updates [selectedMode]/[effectiveMode] reactively. */
    fun setSelectedMode(mode: PowerMode)
}
