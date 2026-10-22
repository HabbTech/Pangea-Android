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
package org.meshtastic.core.model.pangea

import kotlinx.serialization.Serializable
import org.meshtastic.core.common.util.latLongToMeter

/**
 * One point of a route, as Pangea-Apple's `LocationEntity` keeps it: whole metres, metres a second and degrees.
 * [timeMillis] is when it was recorded; imported points have none.
 */
@Serializable
data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Int = 0,
    val speed: Int = 0,
    val heading: Int = 0,
    val timeMillis: Long? = null,
)

/**
 * A saved route (Pangea-Apple `RouteEntity`): imported from a CSV or recorded by the Route Recorder. Routes are the
 * phone's, not a radio's. [color] is 0xRRGGBB; [enabled] routes are drawn on the mesh map.
 */
@Serializable
data class Route(
    val id: Int,
    val name: String,
    val notes: String = "",
    val createdMillis: Long,
    val endMillis: Long? = null,
    val color: Int,
    val enabled: Boolean = false,
    val distanceMeters: Double = 0.0,
    val elevationGainMeters: Double = 0.0,
    val points: List<RoutePoint> = emptyList(),
) {
    companion object {
        /** iOS keeps a route's name to 100 bytes of UTF-8. */
        const val MAX_NAME_BYTES = 100

        /** iOS's route ids: a random positive number above a byte's range. */
        val ID_RANGE = Byte.MAX_VALUE.toInt()..Int.MAX_VALUE

        const val COLOR_MASK = 0xFFFFFF
    }
}

/** The Route Recorder's activities, in iOS's order (`ActivityType`). */
enum class RouteActivity {
    WALKING,
    HIKING,
    BIKING,
    DRIVING,
    OVERLANDING,
    SKIING,
}

/** The part of the day a recording starts in, which begins its name ("Morning hike"), as iOS's `relativeTimeOfDay`. */
enum class TimeOfDay {
    MORNING,
    MIDDAY,
    AFTERNOON,
    EVENING,
    NIGHTTIME,
    ;

    companion object {
        @Suppress("MagicNumber")
        fun of(hour: Int): TimeOfDay = when (hour) {
            in 6..11 -> MORNING
            12 -> MIDDAY
            in 13..16 -> AFTERNOON
            in 17..21 -> EVENING
            else -> NIGHTTIME
        }
    }
}

/**
 * A fix from the phone's own location, as the Route Recorder shows and records it. Missing values are null; accuracies
 * are in metres, metres a second and degrees.
 */
data class GpsFix(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val horizontalAccuracy: Float? = null,
    val verticalAccuracy: Float? = null,
    val speed: Float? = null,
    val speedAccuracy: Float? = null,
    val bearing: Float? = null,
    val bearingAccuracy: Float? = null,
    val timeMillis: Long,
)

/**
 * A recording in progress: the route as saved so far, its running distance and elevation gain, and the phone's latest
 * fix. Points heard while [paused] move the fix but aren't recorded.
 */
data class RouteRecording(
    val route: Route,
    val paused: Boolean = false,
    val distanceMeters: Double = 0.0,
    val elevationGainMeters: Double = 0.0,
    val lastFix: GpsFix? = null,
    /** The last recorded point's altitude, which elevation gain climbs from. */
    val lastAltitude: Double? = null,
    /** True after a resume, until the next point: the gap while paused isn't distance travelled. */
    val resumed: Boolean = false,
) {
    /**
     * This recording with [fix] heard. Recorded unless paused or too inaccurate to draw: distance and elevation gain
     * add up from the last point, as iOS's `LocationsHandler` does.
     */
    fun adding(fix: GpsFix): RouteRecording {
        if (paused || (fix.horizontalAccuracy ?: 0f) > MAX_ACCURACY_METERS) return copy(lastFix = fix)
        val point =
            RoutePoint(
                latitude = fix.latitude,
                longitude = fix.longitude,
                altitude = fix.altitude?.toInt() ?: 0,
                speed = fix.speed?.toInt() ?: 0,
                heading = fix.bearing?.toInt() ?: 0,
                timeMillis = fix.timeMillis,
            )
        val last = route.points.lastOrNull()
        var distance = distanceMeters
        var gain = elevationGainMeters
        if (last != null && !resumed) {
            distance += latLongToMeter(last.latitude, last.longitude, point.latitude, point.longitude)
            if (fix.altitude != null && lastAltitude != null && fix.altitude > lastAltitude) {
                gain += fix.altitude - lastAltitude
            }
        }
        return copy(
            route = route.copy(points = route.points + point),
            distanceMeters = distance,
            elevationGainMeters = gain,
            lastFix = fix,
            lastAltitude = fix.altitude ?: lastAltitude,
            resumed = false,
        )
    }

    companion object {
        /** Fixes vaguer than this would scribble on the line; iOS's own filter is stricter still (5 m). */
        const val MAX_ACCURACY_METERS = 50f
    }
}
