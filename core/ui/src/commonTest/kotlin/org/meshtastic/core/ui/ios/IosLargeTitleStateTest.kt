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
package org.meshtastic.core.ui.ios

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosLargeTitleStateTest {

    private val drag = NestedScrollSource.UserInput

    private fun measured(height: Float = 100f) = IosLargeTitleState().apply { onMeasured(height) }

    @Test
    fun scrollingUpFoldsTheTitleBeforeTheListMoves() {
        val state = measured()

        val first = state.connection.onPreScroll(Offset(0f, -60f), drag)
        assertEquals(-60f, first.y)
        assertEquals(60f, state.offset)

        // Only what is left of the title is taken; the rest of the drag goes to the list.
        val second = state.connection.onPreScroll(Offset(0f, -60f), drag)
        assertEquals(-40f, second.y)
        assertEquals(100f, state.offset)
        assertEquals(1f, state.fraction)
        assertTrue(state.isCollapsed)
    }

    @Test
    fun scrollingDownBringsTheTitleBackOnlyOnceTheListIsAtItsTop() {
        val state = measured()
        state.connection.onPreScroll(Offset(0f, -100f), drag)

        // The list still has room to scroll, so it takes the drag and nothing is left over.
        assertEquals(Offset.Zero, state.connection.onPreScroll(Offset(0f, 50f), drag))
        assertEquals(Offset.Zero, state.connection.onPostScroll(Offset(0f, 50f), Offset.Zero, drag))
        assertEquals(100f, state.offset)

        // At the top, what the list could not use unfolds the title.
        val used = state.connection.onPostScroll(Offset.Zero, Offset(0f, 30f), drag)
        assertEquals(30f, used.y)
        assertEquals(70f, state.offset)
    }

    @Test
    fun aScreenWithNoLargeTitleTakesNothing() {
        val state = IosLargeTitleState()

        assertEquals(Offset.Zero, state.connection.onPreScroll(Offset(0f, -40f), drag))
        assertEquals(Offset.Zero, state.connection.onPostScroll(Offset.Zero, Offset(0f, 40f), drag))
        assertEquals(0f, state.fraction)
        assertFalse(state.isCollapsed)
    }

    @Test
    fun sidewaysScrollsAreLeftAlone() {
        val state = measured()

        assertEquals(Offset.Zero, state.connection.onPreScroll(Offset(-80f, 0f), drag))
        assertEquals(0f, state.offset)
    }

    @Test
    fun aRestoredCollapsedTitleStaysGoneOnceItIsMeasured() {
        val state = IosLargeTitleState(startCollapsed = true)

        state.onMeasured(80f)

        assertEquals(80f, state.offset)
        assertTrue(state.isCollapsed)
    }

    @Test
    fun aTitleThatGetsShorterKeepsTheOffsetInRange() {
        val state = measured()
        state.connection.onPreScroll(Offset(0f, -100f), drag)

        state.onMeasured(60f)

        assertEquals(60f, state.offset)
        assertEquals(1f, state.fraction)
    }
}
