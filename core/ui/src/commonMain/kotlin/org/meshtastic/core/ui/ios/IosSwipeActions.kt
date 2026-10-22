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

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * iOS's trailing swipe actions (`.swipeActions`): drag the row left to reveal [actions] behind its end, the first one
 * at the trailing edge, as SwiftUI lays them out. A tap on one runs it and closes the row; a swipe back closes it too.
 * Screen readers get the actions as the row's custom actions, so they need no swipe.
 */
@Composable
fun IosSwipeActions(actions: List<IosSwipeAction>, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val revealPx = with(LocalDensity.current) { ACTION_WIDTH.toPx() } * actions.size
    fun close() = scope.launch { offset.animateTo(0f) }

    Box(
        modifier.fillMaxWidth().height(IntrinsicSize.Min).clipToBounds().semantics {
            customActions =
                actions.map { action ->
                    CustomAccessibilityAction(action.label) {
                        action.onClick()
                        true
                    }
                }
        },
    ) {
        Row(Modifier.align(Alignment.CenterEnd).fillMaxHeight()) {
            // SwiftUI puts the first action at the trailing edge.
            actions.asReversed().forEach { action ->
                Column(
                    Modifier.width(ACTION_WIDTH).fillMaxHeight().background(action.color).clickable {
                        close()
                        action.onClick()
                    },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(action.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text(action.label, style = Ios.type.caption1, color = Color.White, maxLines = 1)
                }
            }
        }
        Box(
            Modifier.offset { IntOffset(offset.value.roundToInt(), 0) }
                .background(Ios.colors.groupedRow)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state =
                    rememberDraggableState { delta ->
                        scope.launch { offset.snapTo((offset.value + delta).coerceIn(-revealPx, 0f)) }
                    },
                    onDragStopped = { velocity ->
                        val open = offset.value < -revealPx / 2 || velocity < -FLING_VELOCITY
                        offset.animateTo(if (open) -revealPx else 0f)
                    },
                ),
        ) {
            content()
        }
    }
}

private val ACTION_WIDTH = 76.dp

/** A leftward fling faster than this opens the row however far it was dragged (px/s). */
private const val FLING_VELOCITY = 800f
