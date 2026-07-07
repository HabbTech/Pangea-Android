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
package org.meshtastic.core.ui.util

import androidx.compose.runtime.staticCompositionLocalOf
import org.meshtastic.core.model.PowerMode

/**
 * State and callbacks for the nav-bar logo's Power Mode menu. Provided at the app root (where DI and root
 * navigation are available); null hides the menu and the logo falls back to its pre-Power-Mode behavior.
 */
data class PowerModeMenuState(
    val currentMode: PowerMode,
    val onSelectMode: (PowerMode) -> Unit,
    val onAboutClick: () -> Unit,
)

val LocalPowerModeMenu = staticCompositionLocalOf<PowerModeMenuState?> { null }
