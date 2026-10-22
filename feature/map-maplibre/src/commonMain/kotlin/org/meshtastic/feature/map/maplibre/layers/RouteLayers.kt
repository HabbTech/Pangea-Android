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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.convertToColor
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.value.LineCap
import org.maplibre.compose.expressions.value.LineJoin
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import org.meshtastic.core.model.pangea.RoutePoint
import org.meshtastic.feature.map.maplibre.geojson.rememberFeatureSource
import org.meshtastic.feature.map.maplibre.geojson.toCssHex

/**
 * Routes as iOS draws them: a 3 pt round-capped line in each route's colour, dashed while it is being recorded, and
 * (unless [endpoints] is off) a [startColor] dot where it starts and a black one where it ends, each ringed in white. A
 * route needs two points to draw.
 */
@Composable
fun RouteLayers(
    routes: List<MapRoute>,
    idPrefix: String = "routes",
    dashed: Boolean = false,
    endpoints: Boolean = true,
    startColor: Color = RouteStartGreen,
) {
    val drawable = remember(routes) { routes.filter { it.points.size > 1 } }
    val lines =
        rememberFeatureSource(drawable) {
            FeatureCollection(
                drawable.map { route ->
                    Feature<LineString, JsonObject?>(
                        geometry = LineString(route.points.map { it.toGeoPosition() }),
                        properties = buildJsonObject { put(COLOR_KEY, route.color.toCssHex()) },
                    )
                },
            )
        }
    val color = feature[COLOR_KEY].asString().convertToColor(const(Color.Blue))
    if (dashed) {
        LineLayer(
            id = "$idPrefix-line",
            source = lines,
            cap = const(LineCap.Round),
            join = const(LineJoin.Round),
            width = const(LINE_WIDTH_DP.dp),
            color = color,
            dasharray = const(listOf<Number>(DASH_WIDTHS, DASH_WIDTHS)),
        )
    } else {
        LineLayer(
            id = "$idPrefix-line",
            source = lines,
            cap = const(LineCap.Round),
            join = const(LineJoin.Round),
            width = const(LINE_WIDTH_DP.dp),
            color = color,
        )
    }
    if (endpoints) {
        EndpointLayer(id = "$idPrefix-start", points = drawable.map { it.points.first() }, color = startColor)
        EndpointLayer(id = "$idPrefix-finish", points = drawable.map { it.points.last() }, color = Color.Black)
    }
}

/** iOS's `RouteEndpointMarker`: a 15 pt dot with a 3 pt white ring. */
@Composable
private fun EndpointLayer(id: String, points: List<RoutePoint>, color: Color) {
    val source =
        rememberFeatureSource(points) {
            FeatureCollection(
                points.map { Feature<Point, JsonObject?>(geometry = Point(it.toGeoPosition()), properties = null) },
            )
        }
    CircleLayer(
        id = id,
        source = source,
        color = const(color),
        radius = const(ENDPOINT_RADIUS_DP.dp),
        strokeColor = const(Color.White),
        strokeWidth = const(ENDPOINT_RING_DP.dp),
    )
}

/** SwiftUI's `.green`, which the mesh map's start markers use; the route editor's are UIKit's pure green. */
val RouteStartGreen = Color(0xFF34C759)

private const val COLOR_KEY = "color"
private const val LINE_WIDTH_DP = 3

/** iOS dashes in 10 pt runs; MapLibre counts dashes in line widths. */
private const val DASH_WIDTHS = 10.0 / LINE_WIDTH_DP

private const val ENDPOINT_RADIUS_DP = 4.5f
private const val ENDPOINT_RING_DP = 3
