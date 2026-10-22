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
package org.meshtastic.core.repository

import android.os.Build
import org.meshtastic.core.model.pangea.GpsFix

actual fun Location.toGpsFix(): GpsFix? = GpsFix(
    latitude = latitude,
    longitude = longitude,
    altitude =
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && hasMslAltitude() -> mslAltitudeMeters
        hasAltitude() -> altitude
        else -> null
    },
    horizontalAccuracy = if (hasAccuracy()) accuracy else null,
    verticalAccuracy = if (hasVerticalAccuracy()) verticalAccuracyMeters else null,
    speed = if (hasSpeed()) speed else null,
    speedAccuracy = if (hasSpeedAccuracy()) speedAccuracyMetersPerSecond else null,
    bearing = if (hasBearing()) bearing else null,
    bearingAccuracy = if (hasBearingAccuracy()) bearingAccuracyDegrees else null,
    timeMillis = time,
)
