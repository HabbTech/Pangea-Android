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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import org.meshtastic.core.model.pangea.RoutePoint
import org.meshtastic.core.ui.component.PlaceholderScreen

/**
 * The Route List editor's map: a saved route as a solid line in its [color] (0xRRGGBB), green start and black finish
 * markers, framed to fit, as iOS's route editor draws it. Embedded, with no chrome of its own. Hosts without a map
 * engine fall back to a [PlaceholderScreen].
 */
@Suppress("Wrapping", "CompositionLocalAllowlist")
val LocalRouteMapProvider =
    compositionLocalOf<@Composable (points: List<RoutePoint>, color: Int, modifier: Modifier) -> Unit> {
        { _, _, _ -> PlaceholderScreen("Route Map") }
    }

/**
 * The Route Recorder's map: full screen, showing and following the phone's position, with the recording so far as a
 * dashed line in its [color]. The caller draws its own buttons over it.
 */
@Suppress("Wrapping", "CompositionLocalAllowlist")
val LocalRouteRecorderMapProvider =
    compositionLocalOf<@Composable (points: List<RoutePoint>, color: Int, modifier: Modifier) -> Unit> {
        { _, _, _ -> PlaceholderScreen("Route Recorder Map") }
    }
