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
     * The next setup page, in the iPhone app's order: Welcome -> Notifications -> Location -> Bluetooth -> Expert Mode
     * -> Pair Your Radio -> Your Name -> All Set. Android adds Critical Alerts after Notifications when notifications
     * are allowed (it has to open the alert channel's settings, which iOS asks for with the permission).
     *
     * Every path passes through [ExpertMode], so no shortcut skips it; see porting/notes/onboarding-routing.md in the
     * project folder for the rationale.
     *
     * @param bluetoothRequiresLocation true below Android 12, where Bluetooth scanning is gated by ACCESS_FINE_LOCATION
     *   rather than the "Nearby devices" permissions. The Location page has just asked for it, so a Bluetooth page
     *   would ask for the same system permission twice in a row — and a user who declines both has spent both of
     *   Android's allowed denials, landing on USER_FIXED with no dialog available again. The Bluetooth page is skipped
     *   instead.
     * @param bluetoothSupported false on hardware with no Bluetooth LE, where the Bluetooth page is skipped.
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
        is Welcome -> Notifications
        is Notifications -> if (allPermissionsGranted) CriticalAlerts else Location
        is CriticalAlerts -> Location
        is Location -> if (bluetoothSupported && !bluetoothRequiresLocation) Bluetooth else ExpertMode
        is Bluetooth -> ExpertMode
        is ExpertMode -> if (canPairRadio) PairRadio else AllSet
        is PairRadio -> SetupProfile
        is SetupProfile -> AllSet
        is AllSet -> null
        else -> null
    }
}
