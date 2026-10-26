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
package org.meshtastic.core.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import org.meshtastic.core.model.PowerMode
import org.meshtastic.core.ui.util.LocalPowerModeMenu
import org.meshtastic.core.ui.util.PowerModeMenuState
import kotlin.test.Test
import kotlin.test.assertEquals

/** P8: the top-bar Power Mode quick-access action (brief §3 test list). */
@OptIn(ExperimentalTestApi::class)
class PowerModeQuickAccessTest {

    private fun state(mode: PowerMode, onSelect: (PowerMode) -> Unit = {}) =
        PowerModeMenuState(currentMode = mode, onSelectMode = onSelect, onAboutClick = {})

    @Suppress("TestFunctionName")
    @Composable
    private fun BarUnderTest(menuState: PowerModeMenuState?) {
        CompositionLocalProvider(LocalPowerModeMenu provides menuState) {
            MainAppBar(
                title = "Test",
                ourNode = null,
                showNodeChip = false,
                canNavigateUp = false,
                onNavigateUp = {},
                actions = {},
                onClickChip = {},
            )
        }
    }

    @Test
    fun quickAccess_showsSelectedModeIcon_viaContentDescription() = runComposeUiTest {
        setContent { BarUnderTest(state(PowerMode.TRAIL)) }

        onNodeWithContentDescription("Power Mode: Trail").assertIsDisplayed()
    }

    @Test
    fun quickAccess_absent_whenNoMenuProvided() = runComposeUiTest {
        setContent { BarUnderTest(menuState = null) }

        onNodeWithContentDescription("Power Mode: Standard").assertDoesNotExist()
        onNodeWithContentDescription("Power Mode: Trail").assertDoesNotExist()
        onNodeWithContentDescription("Power Mode: Expedition").assertDoesNotExist()
    }

    @Test
    fun click_opensMenu_withAllModes() = runComposeUiTest {
        setContent { BarUnderTest(state(PowerMode.STANDARD)) }

        onNodeWithContentDescription("Power Mode: Standard").performClick()

        onNodeWithText("Standard").assertIsDisplayed()
        onNodeWithText("Trail").assertIsDisplayed()
        onNodeWithText("Expedition").assertIsDisplayed()
    }

    @Test
    fun selectingMode_callsOnSelectMode() = runComposeUiTest {
        var selected: PowerMode? = null
        setContent { BarUnderTest(state(PowerMode.STANDARD, onSelect = { selected = it })) }

        onNodeWithContentDescription("Power Mode: Standard").performClick()
        onNodeWithText("Expedition").performClick()

        assertEquals(PowerMode.EXPEDITION, selected)
    }

    @Test
    fun icon_updatesReactively_whenSelectionChanges() = runComposeUiTest {
        var mode by mutableStateOf(PowerMode.STANDARD)
        setContent { BarUnderTest(state(mode, onSelect = { mode = it })) }

        onNodeWithContentDescription("Power Mode: Standard").performClick()
        onNodeWithText("Expedition").performClick()

        onNodeWithContentDescription("Power Mode: Expedition").assertIsDisplayed()
    }
}
