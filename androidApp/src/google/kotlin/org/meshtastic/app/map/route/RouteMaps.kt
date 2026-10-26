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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.RoundCap
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.meshtastic.core.model.pangea.Route
import org.meshtastic.core.model.pangea.RoutePoint
import org.meshtastic.core.repository.RouteRecorder
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.util.RouteRecorderMapControls
import org.meshtastic.core.ui.util.rememberLocationPermissionState

/** The route editor's map on Google Maps: the route, its start and finish, framed once the map has loaded. */
@Composable
fun RouteMap(points: List<RoutePoint>, color: Int, modifier: Modifier = Modifier) {
    val line = remember(points) { points.map { LatLng(it.latitude, it.longitude) } }
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(line.firstOrNull() ?: LatLng(0.0, 0.0), FOLLOW_ZOOM)
    }
    var loaded by remember { mutableStateOf(false) }
    val padding = with(LocalDensity.current) { FIT_PADDING.roundToPx() }
    LaunchedEffect(line, loaded) {
        if (loaded && line.size > 1) {
            val bounds = LatLngBounds.builder().apply { line.forEach(::include) }.build()
            camera.move(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        }
    }
    GoogleMap(
        modifier = modifier,
        cameraPositionState = camera,
        mapColorScheme = if (Ios.colors.isDark) ComposeMapColorScheme.DARK else ComposeMapColorScheme.LIGHT,
        uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false),
        onMapLoaded = { loaded = true },
    ) {
        RouteOverlay(line = line, color = color.opaque(), startColor = Color.Green)
    }
}

/**
 * The Route Recorder's map on Google Maps: the phone's blue dot, followed (in from the world view to iOS's framing on
 * the first fix), and the recording as a dashed line. iOS turns the map to the phone's compass heading; Google Maps
 * offers no heading here, so the map turns to the direction of travel while the phone is moving. iOS's 3D toggle and
 * compass sit down the right; the compass turns the map north up and stops it turning.
 */
@Composable
@Suppress("LongMethod")
fun RouteRecorderMap(points: List<RoutePoint>, color: Int, modifier: Modifier = Modifier) {
    val line = remember(points) { points.map { LatLng(it.latitude, it.longitude) } }
    val camera = rememberCameraPositionState {
        position =
            line.lastOrNull()?.let { CameraPosition.fromLatLngZoom(it, FOLLOW_ZOOM) }
                ?: CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), WORLD_ZOOM)
    }
    val permission = rememberLocationPermissionState()
    val recorder: RouteRecorder = koinInject()
    var followHeading by remember { mutableStateOf(true) }
    val currentFollowHeading by rememberUpdatedState(followHeading)
    val scope = rememberCoroutineScope()
    LaunchedEffect(permission.isGranted) {
        if (!permission.isGranted) return@LaunchedEffect
        recorder.fixes().collect { fix ->
            val now = camera.position
            val next = CameraPosition.Builder(now).target(LatLng(fix.latitude, fix.longitude))
            if (now.zoom < FOLLOW_ZOOM - 1) next.zoom(FOLLOW_ZOOM)
            val course = fix.bearing
            if (currentFollowHeading && course != null && (fix.speed ?: 0f) > MOVING_SPEED) next.bearing(course)
            camera.animate(CameraUpdateFactory.newCameraPosition(next.build()))
        }
    }
    Box(modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = camera,
            mapColorScheme = if (Ios.colors.isDark) ComposeMapColorScheme.DARK else ComposeMapColorScheme.LIGHT,
            properties = MapProperties(isMyLocationEnabled = permission.isGranted),
            uiSettings =
            MapUiSettings(
                zoomControlsEnabled = false,
                mapToolbarEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = false,
            ),
        ) {
            RouteOverlay(line = line, color = color.opaque(), dashed = true, endpoints = false)
        }
        val pitched = camera.position.tilt > 1f
        RouteRecorderMapControls(
            pitched = pitched,
            bearing = camera.position.bearing,
            onTogglePitch = {
                val tilted = CameraPosition.Builder(camera.position).tilt(if (pitched) 0f else PITCH_3D).build()
                scope.launch { camera.animate(CameraUpdateFactory.newCameraPosition(tilted)) }
            },
            onNorthUp = {
                followHeading = false
                val northUp = CameraPosition.Builder(camera.position).bearing(0f).build()
                scope.launch { camera.animate(CameraUpdateFactory.newCameraPosition(northUp)) }
            },
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}

/** A saved route on the mesh map, in its own colour with SwiftUI's green start. */
@Composable
@GoogleMapComposable
fun SavedRouteOverlay(route: Route) {
    val line = remember(route.points) { route.points.map { LatLng(it.latitude, it.longitude) } }
    RouteOverlay(line = line, color = route.color.opaque())
}

/**
 * A route as iOS draws one: a 3 pt round-capped line, dashed while recording, with a [startColor] dot at its start and
 * a black one at its finish, each ringed in white. Needs two points.
 */
@Composable
@GoogleMapComposable
fun RouteOverlay(
    line: List<LatLng>,
    color: Color,
    dashed: Boolean = false,
    endpoints: Boolean = true,
    startColor: Color = RouteStartGreen,
) {
    if (line.size < 2) return
    val density = LocalDensity.current
    val width = with(density) { LINE_WIDTH.toPx() }
    val dash = with(density) { DASH.toPx() }
    Polyline(
        points = line,
        color = color,
        width = width,
        startCap = RoundCap(),
        endCap = RoundCap(),
        jointType = JointType.ROUND,
        pattern = if (dashed) listOf(Dash(dash), Gap(dash)) else null,
    )
    if (endpoints) {
        EndpointMarker(line.first(), startColor)
        EndpointMarker(line.last(), Color.Black)
    }
}

/** iOS's `RouteEndpointMarker`: a 15 pt dot with a 3 pt white ring. */
@Composable
@GoogleMapComposable
private fun EndpointMarker(at: LatLng, color: Color) {
    MarkerComposable(color, state = rememberUpdatedMarkerState(position = at), anchor = CENTRE) {
        Box(Modifier.size(15.dp).background(color, CircleShape).border(3.dp, Color.White, CircleShape))
    }
}

/** SwiftUI's `.green`, which the mesh map's start markers use; the route editor's are UIKit's pure green. */
val RouteStartGreen = Color(0xFF34C759)

/** A marker hung from its middle. */
private val CENTRE = Offset(0.5f, 0.5f)

private fun Int.opaque(): Color = Color(OPAQUE or (this and RGB_MASK))

private const val OPAQUE = -0x1000000
private const val RGB_MASK = 0xFFFFFF

/** iOS's user-location camera, about 1.5 km across a phone. */
private const val FOLLOW_ZOOM = 14.7f

/** How far 3D tilts the map, as the mesh map's 3D button does. */
private const val PITCH_3D = 60f

/** Above walking-pace jitter (m/s): the course is only a direction once the phone is moving. */
private const val MOVING_SPEED = 0.5f
private const val WORLD_ZOOM = 2f
private val FIT_PADDING = 48.dp
private val LINE_WIDTH = 3.dp
private val DASH = 10.dp
