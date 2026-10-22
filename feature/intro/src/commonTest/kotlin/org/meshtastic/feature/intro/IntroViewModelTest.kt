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

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Bootstrap tests for IntroViewModel.
 *
 * Tests the intro navigation flow logic.
 */
class IntroViewModelTest {

    private lateinit var viewModel: IntroViewModel

    @BeforeTest
    fun setUp() {
        viewModel = IntroViewModel()
    }

    @Test
    fun testWelcomeNavigatesNextToNotifications() {
        assertEquals(Notifications, viewModel.getNextKey(Welcome, allPermissionsGranted = false))
    }

    @Test
    fun testNotificationsWithPermissionNavigatesToCriticalAlerts() {
        val next = viewModel.getNextKey(Notifications, allPermissionsGranted = true)
        assertEquals(CriticalAlerts, next)
    }

    @Test
    fun testNotificationsWithoutPermissionNavigatesToLocation() {
        assertEquals(Location, viewModel.getNextKey(Notifications, allPermissionsGranted = false))
    }

    @Test
    fun testCriticalAlertsNavigatesToLocation() {
        assertEquals(Location, viewModel.getNextKey(CriticalAlerts, allPermissionsGranted = true))
    }

    @Test
    fun testLocationNavigatesToBluetooth() {
        assertEquals(Bluetooth, viewModel.getNextKey(Location, allPermissionsGranted = false))
    }

    /**
     * Below Android 12 Bluetooth scanning is gated by ACCESS_FINE_LOCATION, which the Location page has just asked for.
     * A Bluetooth page would ask for the same system permission twice in a row — and a user who declines both is
     * USER_FIXED before reaching the app, with no dialog available again.
     */
    @Test
    fun testLocationSkipsBluetoothWhenItAlreadyAskedForIt() {
        val next = viewModel.getNextKey(Location, allPermissionsGranted = false, bluetoothRequiresLocation = true)
        assertEquals(ExpertMode, next)
    }

    @Test
    fun testLocationSkipsBluetoothOnHardwareWithoutIt() {
        val next = viewModel.getNextKey(Location, allPermissionsGranted = false, bluetoothSupported = false)
        assertEquals(ExpertMode, next)
    }

    @Test
    fun testBluetoothNavigatesToExpertMode() {
        assertEquals(ExpertMode, viewModel.getNextKey(Bluetooth, allPermissionsGranted = false))
    }

    @Test
    fun testExpertModeLeadsToAllSet() {
        assertEquals(AllSet, viewModel.getNextKey(ExpertMode, allPermissionsGranted = true))
        assertNull(viewModel.getNextKey(AllSet, allPermissionsGranted = true), "All Set is the final step")
    }

    @Test
    fun testNoPathBypassesExpertMode() {
        // Exhaustive: from every non-terminal step, walking the flow with either permission outcome ends at ExpertMode.
        for (granted in listOf(true, false)) {
            var key: androidx.navigation3.runtime.NavKey? = Welcome
            val seen = mutableListOf<androidx.navigation3.runtime.NavKey>()
            while (key != null) {
                seen += key
                key = viewModel.getNextKey(key, allPermissionsGranted = granted)
            }
            assertEquals(AllSet, seen.last(), "Flow with permissionsGranted=$granted must end on All Set")
            assertEquals(ExpertMode, seen[seen.size - 2], "Expert Mode must come just before All Set")
        }
    }

    @Test
    fun testWithPairingSetupPairsThenAsksForTheName() {
        assertEquals(PairRadio, viewModel.getNextKey(ExpertMode, allPermissionsGranted = true, canPairRadio = true))
        assertEquals(SetupProfile, viewModel.getNextKey(PairRadio, allPermissionsGranted = true, canPairRadio = true))
        assertEquals(AllSet, viewModel.getNextKey(SetupProfile, allPermissionsGranted = true, canPairRadio = true))
    }
}
