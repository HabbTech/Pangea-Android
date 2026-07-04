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
package org.meshtastic.feature.intro

import androidx.lifecycle.ViewModel
import androidx.navigation3.runtime.NavKey
import org.koin.core.annotation.KoinViewModel

/** ViewModel for the app introduction flow. */
@KoinViewModel
class IntroViewModel : ViewModel() {

    /**
     * Determines the next navigation key based on the current key and the state of permissions. The flow hierarchy is:
     * Core Connection -> Shared Location -> Notifications -> Critical Alerts -> Expert Mode -> Done.
     *
     * Every path ends on [ExpertMode] (informational, no permission ask) so that no skip shortcut bypasses it; see
     * porting/notes/onboarding-routing.md in the project folder for the rationale.
     */
    fun getNextKey(currentKey: NavKey, allPermissionsGranted: Boolean): NavKey? = when (currentKey) {
        is Welcome -> Bluetooth
        is Bluetooth -> Location
        is Location -> Notifications
        is Notifications -> if (allPermissionsGranted) CriticalAlerts else ExpertMode
        is CriticalAlerts -> ExpertMode
        is ExpertMode -> null
        else -> null
    }
}
