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
    fun testWelcomeNavigatesNextToBluetooth() {
        val next = viewModel.getNextKey(Welcome, allPermissionsGranted = false)
        assertEquals(Bluetooth, next)
    }

    @Test
    fun testBluetoothNavigatesToLocation() {
        val next = viewModel.getNextKey(Bluetooth, allPermissionsGranted = false)
        assertEquals(Location, next)
    }

    @Test
    fun testLocationNavigatesToNotifications() {
        val next = viewModel.getNextKey(Location, allPermissionsGranted = false)
        assertEquals(Notifications, next)
    }

    @Test
    fun testNotificationsWithPermissionNavigatesToCriticalAlerts() {
        val next = viewModel.getNextKey(Notifications, allPermissionsGranted = true)
        assertEquals(CriticalAlerts, next)
    }

    @Test
    fun testNotificationsWithoutPermissionNavigatesToExpertMode() {
        val next = viewModel.getNextKey(Notifications, allPermissionsGranted = false)
        assertEquals(ExpertMode, next, "Skipping notifications must still route through the Expert Mode step")
    }

    @Test
    fun testCriticalAlertsNavigatesToExpertMode() {
        val next = viewModel.getNextKey(CriticalAlerts, allPermissionsGranted = true)
        assertEquals(ExpertMode, next, "Critical Alerts must route through the Expert Mode step")
    }

    @Test
    fun testExpertModeIsTerminal() {
        val next = viewModel.getNextKey(ExpertMode, allPermissionsGranted = true)
        assertNull(next, "ExpertMode is the final step and should not navigate further")
    }

    @Test
    fun testNoPathBypassesExpertMode() {
        // Exhaustive: from every non-terminal step, walking the flow with either permission outcome ends at ExpertMode.
        for (granted in listOf(true, false)) {
            var key: androidx.navigation3.runtime.NavKey? = Welcome
            var last: androidx.navigation3.runtime.NavKey = Welcome
            while (key != null) {
                last = key
                key = viewModel.getNextKey(key, allPermissionsGranted = granted)
            }
            assertEquals(ExpertMode, last, "Flow with permissionsGranted=$granted must end on ExpertMode")
        }
    }
}
