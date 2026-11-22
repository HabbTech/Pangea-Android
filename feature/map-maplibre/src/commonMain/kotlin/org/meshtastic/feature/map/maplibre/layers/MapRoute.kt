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
package org.meshtastic.feature.map.maplibre.layers

import androidx.compose.runtime.Immutable
import org.maplibre.spatialk.geojson.Position
import org.meshtastic.core.model.pangea.RoutePoint

/** A saved route as a map draws it: its points and its colour (0xRRGGBB). */
@Immutable data class MapRoute(val id: Int, val points: List<RoutePoint>, val color: Int)

/** A route point where MapLibre wants it. */
fun RoutePoint.toGeoPosition(): Position = Position(longitude = longitude, latitude = latitude)
