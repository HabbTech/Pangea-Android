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
import kotlin.math.roundToLong

/**
 * Routes as CSV files, the way Pangea-Apple imports and exports them (`Routes.swift`, `routeToCsvFile`), so a route
 * moves between the two apps as a file.
 */
object RouteCsv {
    /** iOS's export header, spaces and all. */
    const val HEADER = "Id, Latitude, Longitude, Altitude, Speed, Heading"

    /**
     * [points] as iOS writes them: [HEADER], then a line per point numbered from 1, no newline at the end. Coordinates
     * keep iOS's seven decimal places (it stores them as degrees × 10⁷).
     */
    fun write(points: List<RoutePoint>): String = buildString {
        append(HEADER)
        points.forEachIndexed { index, point ->
            append('\n')
            append(
                listOf(
                    (index + 1).toString(),
                    formatDegrees(point.latitude),
                    formatDegrees(point.longitude),
                    point.altitude.toString(),
                    point.speed.toString(),
                    point.heading.toString(),
                )
                    .joinToString(", "),
            )
        }
    }

    /**
     * The points in [text], or null when it isn't a route file: the header row must name `Latitude` and `Longitude`
     * columns, in any order. Altitude, Speed and Heading are read where present (iOS's own exports have them). Rows
     * without both coordinates as numbers are skipped; coordinates are held to the valid ranges, as iOS does.
     */
    fun read(text: String): List<RoutePoint>? {
        val lines = text.removePrefix(BYTE_ORDER_MARK).lines()
        val headers = lines.first().let(::cells).map { it.lowercase() }
        val lat = headers.indexOf("latitude")
        val lon = headers.indexOf("longitude")
        if (lat < 0 || lon < 0) return null
        val altitude = headers.indexOf("altitude")
        val speed = headers.indexOf("speed")
        val heading = headers.indexOf("heading")

        val points =
            lines.drop(1).mapNotNull { line ->
                val row = cells(line)
                val latitude = row.getOrNull(lat)?.toDoubleOrNull()?.takeIf { it.isFinite() }
                val longitude = row.getOrNull(lon)?.toDoubleOrNull()?.takeIf { it.isFinite() }
                if (latitude == null || longitude == null) return@mapNotNull null
                RoutePoint(
                    latitude = latitude.coerceIn(-MAX_LATITUDE, MAX_LATITUDE),
                    longitude = longitude.coerceIn(-MAX_LONGITUDE, MAX_LONGITUDE),
                    altitude = row.wholeNumber(altitude),
                    speed = row.wholeNumber(speed),
                    heading = row.wholeNumber(heading),
                )
            }
        return points.takeIf { it.isNotEmpty() }
    }

    /** A route's name from its file's: iOS drops the four-character `.csv`; any other extension goes too. */
    fun routeName(fileName: String): String {
        val name = fileName.substringAfterLast('/')
        return if ('.' in name) name.substringBeforeLast('.') else name
    }

    /** iOS's export file name (`CsvDocument.exportFilename`): no slashes or colons, ending in `.csv`. */
    fun exportFileName(baseName: String): String {
        val name = baseName.replace('/', '-').replace(':', '-')
        return if (name.lowercase().endsWith(".csv")) name else "$name.csv"
    }

    private fun cells(line: String): List<String> = line.split(',').map { it.trim().removeSurrounding("\"").trim() }

    private fun List<String>.wholeNumber(index: Int): Int =
        getOrNull(index)?.toDoubleOrNull()?.takeIf { it.isFinite() }?.toInt() ?: 0

    /**
     * Degrees to seven places with trailing zeros dropped, as Swift prints iOS's stored value ("37.7749295", "-122.0").
     */
    internal fun formatDegrees(degrees: Double): String {
        val scaled = (degrees * DEGREE_SCALE).roundToLong()
        val whole = abs(scaled) / DEGREE_SCALE.toLong()
        val fraction = (abs(scaled) % DEGREE_SCALE.toLong()).toString().padStart(DECIMALS, '0').trimEnd('0')
        val sign = if (scaled < 0) "-" else ""
        return "$sign$whole.${fraction.ifEmpty { "0" }}"
    }

    /** U+FEFF, which spreadsheet apps put at the start of a UTF-8 file. */
    private val BYTE_ORDER_MARK = 0xFEFF.toChar().toString()
    private const val DEGREE_SCALE = 1e7
    private const val DECIMALS = 7
    private const val MAX_LATITUDE = 90.0
    private const val MAX_LONGITUDE = 180.0
}
