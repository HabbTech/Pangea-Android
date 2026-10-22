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

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RouteRecordingTest {

    private val start = RouteRecording(Route(id = 1000, name = "Morning hike", createdMillis = 0, color = 0xFF0000))

    private fun fix(lat: Double, altitude: Double? = null, accuracy: Float? = 5f, time: Long = 0) =
        GpsFix(latitude = lat, longitude = 0.0, altitude = altitude, horizontalAccuracy = accuracy, timeMillis = time)

    /** About 111.2 m: a thousandth of a degree of latitude. */
    private val step = 0.001
    private val stepMeters = 111.19

    private fun near(expected: Double, actual: Double) = assertTrue(abs(expected - actual) < 0.5, "$actual")

    @Test
    fun pointsAddUpDistanceAndOnlyTheClimbs() {
        val recording =
            start
                .adding(fix(0.0, altitude = 100.0, time = 1))
                .adding(fix(step, altitude = 110.5, time = 2))
                .adding(fix(2 * step, altitude = 104.0, time = 3))
                .adding(fix(3 * step, altitude = 109.0, time = 4))

        assertEquals(4, recording.route.points.size)
        near(3 * stepMeters, recording.distanceMeters)
        assertEquals(10.5 + 5.0, recording.elevationGainMeters)
        assertEquals(RoutePoint(3 * step, 0.0, altitude = 109, timeMillis = 4), recording.route.points.last())
    }

    @Test
    fun pausedAndVagueFixesMoveTheFixButAreNotRecorded() {
        val vague = start.adding(fix(0.0, accuracy = 80f))
        assertEquals(0, vague.route.points.size)
        assertEquals(80f, vague.lastFix?.horizontalAccuracy)

        val paused = start.adding(fix(0.0)).copy(paused = true).adding(fix(step))
        assertEquals(1, paused.route.points.size)
        assertEquals(step, paused.lastFix?.latitude)
    }

    @Test
    fun theGapWhilePausedIsNotDistanceTravelled() {
        val recording =
            start
                .adding(fix(0.0))
                .adding(fix(step))
                .copy(paused = true)
                .adding(fix(5 * step))
                .copy(paused = false, resumed = true)
                .adding(fix(5 * step))
                .adding(fix(6 * step))

        assertEquals(4, recording.route.points.size)
        near(2 * stepMeters, recording.distanceMeters)
    }

    @Test
    fun aFixWithoutAccuracyIsRecorded() {
        assertEquals(1, start.adding(fix(0.0, accuracy = null)).route.points.size)
    }

    @Test
    fun theTimeOfDayNamesARecordingAsIosDoes() {
        assertEquals(TimeOfDay.NIGHTTIME, TimeOfDay.of(5))
        assertEquals(TimeOfDay.MORNING, TimeOfDay.of(6))
        assertEquals(TimeOfDay.MIDDAY, TimeOfDay.of(12))
        assertEquals(TimeOfDay.AFTERNOON, TimeOfDay.of(16))
        assertEquals(TimeOfDay.EVENING, TimeOfDay.of(21))
        assertEquals(TimeOfDay.NIGHTTIME, TimeOfDay.of(22))
    }
}
