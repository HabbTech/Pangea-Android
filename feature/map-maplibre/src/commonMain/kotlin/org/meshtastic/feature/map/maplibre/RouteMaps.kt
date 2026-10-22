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
package org.meshtastic.feature.map.maplibre

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.location.BearingUpdate
import org.maplibre.compose.location.LocationPuck
import org.maplibre.compose.location.LocationTrackingEffect
import org.maplibre.compose.location.rememberDefaultHeadingProvider
import org.maplibre.compose.location.rememberDefaultLocationProvider
import org.maplibre.compose.location.rememberLocationState
import org.maplibre.compose.location.updateCamera
import org.maplibre.compose.material3.LocationPuckDefaults
import org.maplibre.spatialk.geojson.Position
import org.meshtastic.core.model.pangea.RoutePoint
import org.meshtastic.feature.map.maplibre.component.MeshMapOrnaments
import org.meshtastic.feature.map.maplibre.component.customRasterBasemaps
import org.meshtastic.feature.map.maplibre.component.rememberBasemapSelection
import org.meshtastic.feature.map.maplibre.layers.MapRoute
import org.meshtastic.feature.map.maplibre.layers.RouteLayers
import org.meshtastic.feature.map.maplibre.layers.toGeoPosition
import org.meshtastic.feature.map.maplibre.style.Basemap

/**
 * The route editor's map (iOS's Route List): the route in its colour with its start and finish, framed to fit. No
 * controls of its own: the editor puts its Export button over it.
 *
 * @param customBasemaps The host's own tile sources; see [MapLibreNodeTrackMap].
 */
@Composable
fun MapLibreRouteMap(
    points: List<RoutePoint>,
    color: Int,
    modifier: Modifier = Modifier,
    customBasemaps: @Composable () -> List<Basemap.Raster> = { customRasterBasemaps() },
) {
    val basemaps = rememberBasemapSelection(customBasemaps()) ?: return
    val routes = remember(points, color) { listOf(MapRoute(id = 0, points = points, color = color)) }
    val mapState =
        rememberSecondaryMapState(basemaps) {
            RouteLayers(routes = routes, idPrefix = "route-preview", startColor = Color.Green)
        }
    FitBoundsOnceVisible(mapState = mapState, key = points) { positionsBoundingBox(points.map { it.toGeoPosition() }) }
    Box(modifier) { SecondaryMapSurface(mapState = mapState, basemaps = basemaps, ornaments = MeshMapOrnaments) }
}

/**
 * The Route Recorder's map: the phone's position, followed, and the recording so far as a dashed line. Location access
 * is the recorder screen's to ask for; until it is given, the map stays where it opened.
 */
@Composable
fun MapLibreRouteRecorderMap(
    points: List<RoutePoint>,
    color: Int,
    modifier: Modifier = Modifier,
    customBasemaps: @Composable () -> List<Basemap.Raster> = { customRasterBasemaps() },
) {
    val basemaps = rememberBasemapSelection(customBasemaps()) ?: return
    val location =
        rememberLocationState(
            enabled = true,
            provider = rememberDefaultLocationProvider(),
            headingProvider = rememberDefaultHeadingProvider(),
        )
    val routes = remember(points, color) { listOf(MapRoute(id = 0, points = points, color = color)) }
    val start = remember {
        points.lastOrNull()?.let { CameraPosition(target = it.toGeoPosition(), zoom = FOLLOW_ZOOM) }
            ?: CameraPosition(target = Position(longitude = 0.0, latitude = 0.0), zoom = WORLD_ZOOM)
    }
    val mapState =
        rememberSecondaryMapState(basemaps, initialCameraPosition = start) {
            RouteLayers(routes = routes, idPrefix = "route-recording", dashed = true, endpoints = false)
            LocationPuck(
                idPrefix = "recorder-location",
                locationState = location,
                colors = LocationPuckDefaults.colors(),
            )
        }
    LocationTrackingEffect(locationState = location, enabled = true, trackBearing = false) {
        val camera = mapState.cameraPosition
        if (previousLocation == null && camera.zoom < FOLLOW_ZOOM) {
            // The first fix: come in from wherever the map opened to street level, as iOS's user-location camera does.
            mapState.animateCameraPosition(camera.copy(target = currentLocation.position, zoom = FOLLOW_ZOOM))
        } else {
            updateCamera(mapState = mapState, updateBearing = BearingUpdate.IGNORE)
        }
    }
    SecondaryMapSurface(mapState = mapState, basemaps = basemaps, modifier = modifier, ornaments = MeshMapOrnaments)
}

private const val FOLLOW_ZOOM = 16.0
private const val WORLD_ZOOM = 2.0
