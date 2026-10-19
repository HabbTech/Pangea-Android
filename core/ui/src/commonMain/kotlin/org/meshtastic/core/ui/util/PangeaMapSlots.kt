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
package org.meshtastic.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Pangea standard mode's plain map (Pangea-Apple, standard Map tab): no toolbar, zoom buttons, waypoints or planner,
 * just the radios and a "locate me" button. The map provider reads this; the map screen sets it.
 */
@Suppress("CompositionLocalAllowlist")
val LocalMapPlainMode = staticCompositionLocalOf { false }

/**
 * Shows a radio's Profile sheet (photo, name, code, nickname, Send Message, Add to Contacts). The app provides it,
 * since the sheet lives in the messaging feature and Send Message navigates to the chat; null where nobody provides it.
 */
@Suppress("CompositionLocalAllowlist")
val LocalNodeProfileSheet =
    staticCompositionLocalOf<(@Composable (nodeNum: Int, onDismiss: () -> Unit) -> Unit)?> { null }

/**
 * How far down the screen the map's own controls must start when it runs under floating header buttons (Pangea Expert
 * Mode's map): the status bar plus the logo row. Zero where the map has an ordinary top bar.
 */
@Suppress("CompositionLocalAllowlist")
val LocalMapTopInset = staticCompositionLocalOf { 0.dp }
