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

import androidx.compose.animation.core.animate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Velocity
import kotlin.math.roundToInt

/**
 * How far a screen's large title has slid under its top bar, as on iOS: the title leaves with the first of the scroll,
 * the small title fades into the bar, and it only comes back once the list is at its top again.
 *
 * One of these belongs to each destination ([IosLargeTitleScope]). The bar reports how tall its large title is
 * ([collapsibleHeight]) and reads [offset]; the destination's scrolling content drives it through [connection].
 */
@Stable
class IosLargeTitleState(startCollapsed: Boolean = false) {
    /** Height in px of the part of the bar that folds away; 0 while the screen shows no large title. */
    var collapsibleHeight by mutableFloatStateOf(0f)
        private set

    /** Px of the large title hidden so far, from 0 (all showing) to [collapsibleHeight] (gone). */
    var offset by mutableFloatStateOf(0f)
        private set

    private var pendingCollapsed = startCollapsed

    /** 0 while the large title shows in full, 1 once it has gone under the bar. */
    val fraction: Float
        get() = if (collapsibleHeight > 0f) (offset / collapsibleHeight).coerceIn(0f, 1f) else 0f

    /** True once more than half of the large title is hidden: the state worth restoring. */
    val isCollapsed: Boolean
        get() = fraction > HALF

    /** The bar measured its large title at [height] px (0 when it has none). */
    fun onMeasured(height: Float) {
        if (height == collapsibleHeight) return
        collapsibleHeight = height
        offset =
            if (pendingCollapsed && height > 0f) {
                pendingCollapsed = false
                height
            } else {
                offset.coerceIn(0f, height)
            }
    }

    val connection: NestedScrollConnection =
        object : NestedScrollConnection {
            // Scrolling up folds the title away before the list itself moves, so both leave together as on iOS.
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y >= 0f || collapsibleHeight <= 0f) return Offset.Zero
                val target = (offset - available.y).coerceIn(0f, collapsibleHeight)
                val used = target - offset
                offset = target
                return Offset(0f, -used)
            }

            // Whatever a downward scroll has left once the list is at its top brings the title back.
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y <= 0f || collapsibleHeight <= 0f) return Offset.Zero
                val target = (offset - available.y).coerceIn(0f, collapsibleHeight)
                val used = offset - target
                offset = target
                return Offset(0f, used)
            }

            // iOS never rests with half a title showing: settle to whichever end is nearer.
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val height = collapsibleHeight
                if (height > 0f && offset > 0f && offset < height) {
                    val target = if (offset > height * HALF) height else 0f
                    animate(initialValue = offset, targetValue = target) { value, _ -> offset = value }
                }
                return Velocity.Zero
            }
        }

    companion object {
        private const val HALF = 0.5f

        /** Remembers only whether the title had gone, which is all a returning screen needs. */
        val Saver: Saver<IosLargeTitleState, Boolean> =
            Saver(save = { it.isCollapsed }, restore = { IosLargeTitleState(startCollapsed = it) })
    }
}

/** The large-title state of the destination on screen; null where nothing provides one (previews, the desktop). */
@Suppress("CompositionLocalAllowlist")
val LocalIosLargeTitleState = compositionLocalOf<IosLargeTitleState?> { null }

/**
 * Gives [content] (one destination) its own [IosLargeTitleState] and feeds every scroll inside it to that state, so a
 * bar that reads [LocalIosLargeTitleState] folds its large title away without the screen wiring anything up.
 */
@Composable
fun IosLargeTitleScope(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val state = rememberSaveable(saver = IosLargeTitleState.Saver) { IosLargeTitleState() }
    CompositionLocalProvider(LocalIosLargeTitleState provides state) {
        Box(modifier.fillMaxSize().nestedScroll(state.connection)) { content() }
    }
}

/**
 * Lays its content out [state]'s offset shorter, sliding it up and out of sight under whatever sits above: the part of
 * a bar that holds the large title. Reports the content's full height to [state]. A null [state] leaves it as it is.
 */
fun Modifier.iosLargeTitleCollapse(state: IosLargeTitleState?): Modifier = if (state == null) {
    this
} else {
    clipToBounds().layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        state.onMeasured(placeable.height.toFloat())
        val hidden = state.offset.roundToInt().coerceIn(0, placeable.height)
        layout(placeable.width, placeable.height - hidden) { placeable.place(0, -hidden) }
    }
}
