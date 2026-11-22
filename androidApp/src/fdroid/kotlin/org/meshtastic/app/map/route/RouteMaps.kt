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
package org.meshtastic.app.map.route

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.meshtastic.app.map.androidCustomRasterBasemaps
import org.meshtastic.core.model.pangea.RoutePoint
import org.meshtastic.feature.map.maplibre.MapLibreRouteMap
import org.meshtastic.feature.map.maplibre.MapLibreRouteRecorderMap

/** Flavor-unified entry point for the route editor's map. MapLibre implementation. */
@Composable
fun RouteMap(points: List<RoutePoint>, color: Int, modifier: Modifier = Modifier) =
    MapLibreRouteMap(points, color, modifier, customBasemaps = { androidCustomRasterBasemaps() })

/** Flavor-unified entry point for the Route Recorder's map. MapLibre implementation. */
@Composable
fun RouteRecorderMap(points: List<RoutePoint>, color: Int, modifier: Modifier = Modifier) =
    MapLibreRouteRecorderMap(points, color, modifier, customBasemaps = { androidCustomRasterBasemaps() })
