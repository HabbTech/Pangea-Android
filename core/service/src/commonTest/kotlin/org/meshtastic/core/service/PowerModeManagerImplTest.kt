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

import dev.mokkery.answering.calls
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.model.PowerMode
import org.meshtastic.core.repository.SystemPowerSaveSource
import org.meshtastic.core.repository.UiPrefs
import kotlin.test.Test
import kotlin.test.assertEquals

class PowerModeManagerImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val dispatchers = CoroutineDispatchers(io = testDispatcher, main = testDispatcher, default = testDispatcher)

    private val powerModeFlow = MutableStateFlow(PowerMode.STANDARD)
    private val powerSaveFlow = MutableStateFlow(false)

    private val uiPrefs =
        mock<UiPrefs> {
            every { powerMode } returns powerModeFlow
            every { setPowerMode(any()) } calls { (mode: PowerMode) -> powerModeFlow.value = mode }
        }
    private val powerSaveSource = mock<SystemPowerSaveSource> { every { isPowerSaveMode } returns powerSaveFlow }

    private fun createManager() = PowerModeManagerImpl(uiPrefs, powerSaveSource, dispatchers)

    @Test
    fun effectiveModeMatrixCoversAllSelectionsAndPowerSaveStates() = runTest(testDispatcher) {
        val manager = createManager()
        val expectations =
            listOf(
                // (selected, powerSave) -> effective
                Triple(PowerMode.STANDARD, false, PowerMode.STANDARD),
                Triple(PowerMode.TRAIL, false, PowerMode.TRAIL),
                Triple(PowerMode.EXPEDITION, false, PowerMode.EXPEDITION),
                Triple(PowerMode.STANDARD, true, PowerMode.EXPEDITION),
                Triple(PowerMode.TRAIL, true, PowerMode.EXPEDITION),
                Triple(PowerMode.EXPEDITION, true, PowerMode.EXPEDITION),
            )
        for ((selected, powerSave, effective) in expectations) {
            powerModeFlow.value = selected
            powerSaveFlow.value = powerSave
            assertEquals(effective, manager.effectiveMode.value, "selected=$selected powerSave=$powerSave")
        }
    }

    @Test
    fun effectiveModeDropsBackWhenPowerSaveEnds() = runTest(testDispatcher) {
        val manager = createManager()
        powerModeFlow.value = PowerMode.TRAIL
        powerSaveFlow.value = true
        assertEquals(PowerMode.EXPEDITION, manager.effectiveMode.value)
        powerSaveFlow.value = false
        assertEquals(PowerMode.TRAIL, manager.effectiveMode.value, "must revert to the user's selection")
    }

    @Test
    fun setSelectedModePersistsThroughPrefsAndUpdatesEffectiveMode() = runTest(testDispatcher) {
        val manager = createManager()
        manager.setSelectedMode(PowerMode.EXPEDITION)
        assertEquals(PowerMode.EXPEDITION, powerModeFlow.value, "must write through UiPrefs")
        assertEquals(PowerMode.EXPEDITION, manager.selectedMode.value)
        assertEquals(PowerMode.EXPEDITION, manager.effectiveMode.value)
    }
}
