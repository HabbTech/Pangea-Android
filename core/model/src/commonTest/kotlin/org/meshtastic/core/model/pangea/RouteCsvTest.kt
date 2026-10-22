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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RouteCsvTest {

    @Test
    fun exportsAsIosWritesIt() {
        val points =
            listOf(
                RoutePoint(37.7749295, -122.4194155, altitude = 16, speed = 1, heading = 270),
                RoutePoint(37.775, -122.0, altitude = -3),
            )

        assertEquals(
            "Id, Latitude, Longitude, Altitude, Speed, Heading\n" +
                "1, 37.7749295, -122.4194155, 16, 1, 270\n" +
                "2, 37.775, -122.0, -3, 0, 0",
            RouteCsv.write(points),
        )
    }

    @Test
    fun anEmptyRouteExportsTheHeaderAlone() {
        assertEquals(RouteCsv.HEADER, RouteCsv.write(emptyList()))
    }

    @Test
    fun importsIosExportsWithEveryColumn() {
        val text = "Id, Latitude, Longitude, Altitude, Speed, Heading\n1, 37.7749295, -122.4194155, 16, 1, 270\n"

        assertEquals(
            listOf(RoutePoint(37.7749295, -122.4194155, altitude = 16, speed = 1, heading = 270)),
            RouteCsv.read(text),
        )
    }

    @Test
    fun columnsCanComeInAnyOrderWithOtherColumnsBetween() {
        val text = "name,longitude,elevation,latitude\r\nA,-1.5,12,51.25\r\nB,-1.6,13,51.26\r\n"

        assertEquals(listOf(RoutePoint(51.25, -1.5), RoutePoint(51.26, -1.6)), RouteCsv.read(text))
    }

    @Test
    fun aFileWithoutBothCoordinateColumnsIsNotARoute() {
        assertNull(RouteCsv.read("Id, Latitude, Altitude\n1, 37.7, 16"))
        assertNull(RouteCsv.read(""))
    }

    @Test
    fun aFileWithNoUsableRowsIsNotARoute() {
        assertNull(RouteCsv.read("Latitude,Longitude\n"))
        assertNull(RouteCsv.read("Latitude,Longitude\nnorth,west"))
    }

    @Test
    fun badRowsAreSkippedAndCoordinatesHeldInRange() {
        val text = "${0xFEFF.toChar()}Latitude,Longitude\n95,200\nbad,1\n1\n\"10.5\",\"20.25\"\nNaN,1"

        assertEquals(listOf(RoutePoint(90.0, 180.0), RoutePoint(10.5, 20.25)), RouteCsv.read(text))
    }

    @Test
    fun theFileNameNamesTheRoute() {
        assertEquals("Morning hike Log", RouteCsv.routeName("Morning hike Log.csv"))
        assertEquals("trail.v2", RouteCsv.routeName("trail.v2.txt"))
        assertEquals("loop", RouteCsv.routeName("loop"))
    }

    @Test
    fun exportsAreNamedAsIosNamesThem() {
        assertEquals("Morning hike Log.csv", RouteCsv.exportFileName("Morning hike Log"))
        assertEquals("a-b-c Log.csv", RouteCsv.exportFileName("a/b:c Log"))
        assertEquals("trail.CSV", RouteCsv.exportFileName("trail.CSV"))
    }

    @Test
    fun degreesKeepSevenPlaces() {
        assertEquals("0.0", RouteCsv.formatDegrees(0.0))
        assertEquals("-0.0001", RouteCsv.formatDegrees(-0.0001))
        assertEquals("51.5", RouteCsv.formatDegrees(51.500000004))
    }
}
