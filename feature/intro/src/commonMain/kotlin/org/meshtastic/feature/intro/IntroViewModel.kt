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
     * Core Connection -> Shared Location -> Notifications -> Critical Alerts -> Expert Mode -> Pair Your Radio -> Your
     * Name -> All Set.
     *
     * Every path ends on [ExpertMode] (informational, no permission ask) so that no skip shortcut bypasses it; see
     * porting/notes/onboarding-routing.md in the project folder for the rationale.
     *
     * @param bluetoothRequiresLocation true on API < 31, where BLE scanning is gated by ACCESS_FINE_LOCATION rather
     *   than the Android 12 "Nearby devices" permissions. There the Bluetooth screen has *already asked for the
     *   location permission*, so following it with a Location screen asks the user for the same system permission twice
     *   in a row — and a user who declines both has spent both of Android's allowed denials before ever seeing the app,
     *   landing on USER_FIXED with no dialog available again. The Bluetooth screen covers both uses on those releases,
     *   so the second ask is dropped rather than duplicated.
     * @param bluetoothSupported false on hardware with no Bluetooth LE, where the Bluetooth screen is skipped.
     * @param canPairRadio true where the app supplies the Pair Your Radio page; then setup pairs a radio and asks for
     *   the user's name before "You're All Set", as on the iPhone app.
     */
    fun getNextKey(
        currentKey: NavKey,
        allPermissionsGranted: Boolean,
        bluetoothRequiresLocation: Boolean = false,
        bluetoothSupported: Boolean = true,
        canPairRadio: Boolean = false,
    ): NavKey? = when (currentKey) {
        is Welcome -> if (bluetoothSupported) Bluetooth else Location
        is Bluetooth -> if (bluetoothRequiresLocation) Notifications else Location
        is Location -> Notifications
        is Notifications -> if (allPermissionsGranted) CriticalAlerts else ExpertMode
        is CriticalAlerts -> ExpertMode
        is ExpertMode -> if (canPairRadio) PairRadio else AllSet
        is PairRadio -> SetupProfile
        is SetupProfile -> AllSet
        is AllSet -> null
        else -> null
    }
}
