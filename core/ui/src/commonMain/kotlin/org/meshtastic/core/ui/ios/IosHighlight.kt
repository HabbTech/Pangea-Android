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

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import kotlinx.coroutines.launch

/**
 * iOS's touch feedback: while a finger is down the control dims under a faint wash, and it clears on release. No ripple
 * spreads from the touch point, as Material's does. The app's theme makes this the default indication, so every
 * clickable that doesn't draw its own (an [IosRow], an [IosButton]) takes it.
 */
class IosHighlight(private val dark: Boolean) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        IosHighlightNode(interactionSource, if (dark) DARK_WASH else LIGHT_WASH)

    override fun equals(other: Any?): Boolean = other is IosHighlight && other.dark == dark

    override fun hashCode(): Int = dark.hashCode()
}

private class IosHighlightNode(private val source: InteractionSource, private val wash: Color) :
    Modifier.Node(),
    DrawModifierNode {
    private var presses = 0

    override fun onAttach() {
        coroutineScope.launch {
            source.interactions.collect { interaction ->
                val before = presses > 0
                when (interaction) {
                    is PressInteraction.Press -> presses++

                    is PressInteraction.Release,
                    is PressInteraction.Cancel,
                    -> presses = (presses - 1).coerceAtLeast(0)
                }
                if (before != presses > 0) invalidateDraw()
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (presses > 0) drawRect(wash)
    }
}

/** iOS's highlight: a dark wash on light surfaces, a light one on dark. */
private val LIGHT_WASH = Color.Black.copy(alpha = 0.08f)
private val DARK_WASH = Color.White.copy(alpha = 0.12f)
