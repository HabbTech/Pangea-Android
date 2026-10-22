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

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * A free-form card shaped like an inset grouped section: the section's corners and row colour, flat, with no Material
 * shadow. For content that is not a list of rows (those go in an [IosSection]).
 */
@Composable
fun IosCard(
    modifier: Modifier = Modifier,
    shape: Shape = IosCardShape,
    colors: CardColors =
        CardDefaults.cardColors(containerColor = Ios.colors.groupedRow, contentColor = Ios.colors.label),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = colors,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        content = content,
    )
}
