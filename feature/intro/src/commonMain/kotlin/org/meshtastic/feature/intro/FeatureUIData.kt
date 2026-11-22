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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource

/**
 * One row of a setup page, as the iPhone app's `makeRow`: an icon, an optional bold title and a grey explanation.
 *
 * @param icon The row's symbol.
 * @param titleRes Optional bold title.
 * @param subtitleRes The explanation.
 * @param tint The symbol's colour where iOS draws it in colour (location blue, map-pin red, …); null is the text
 *   colour.
 * @param capsule Draws the symbol on a dark capsule, as iOS's custom Bluetooth symbol.
 */
internal data class FeatureUIData(
    val icon: ImageVector,
    val titleRes: StringResource? = null,
    val subtitleRes: StringResource,
    val tint: Color? = null,
    val capsule: Boolean = false,
)
