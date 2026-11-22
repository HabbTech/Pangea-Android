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
package org.meshtastic.feature.settings.pangea

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.NumberFormatter
import org.meshtastic.core.model.pangea.GpsFix
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.pangea_gps_accuracy
import org.meshtastic.core.resources.pangea_gps_altitude
import org.meshtastic.core.resources.pangea_gps_coordinate
import org.meshtastic.core.resources.pangea_gps_heading
import org.meshtastic.core.resources.pangea_gps_speed
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosRow

/**
 * iOS's GPS status rows (`GPSStatus`): the phone's fix as the phone reports it, a row for each of coordinate, accuracy,
 * altitude, heading and speed it has. For an [org.meshtastic.core.ui.ios.IosSection].
 */
@Composable
internal fun GpsFixRows(fix: GpsFix) {
    IosRow(
        title =
        stringResource(
            Res.string.pangea_gps_coordinate,
            "${NumberFormatter.format(fix.latitude, COORD_PLACES)}, " +
                NumberFormatter.format(fix.longitude, COORD_PLACES),
        ),
        icon = IosIcons.MapPin,
    )
    fix.horizontalAccuracy?.let {
        IosRow(title = stringResource(Res.string.pangea_gps_accuracy, metres(it)), icon = IosIcons.Target)
    }
    fix.altitude?.let {
        IosRow(title = stringResource(Res.string.pangea_gps_altitude, metres(it.toFloat())), icon = IosIcons.Mountain)
    }
    fix.bearing?.let {
        IosRow(title = stringResource(Res.string.pangea_gps_heading, "${it.toInt()}°"), icon = IosIcons.Safari)
    }
    fix.speed?.let {
        IosRow(
            title = stringResource(Res.string.pangea_gps_speed, "${NumberFormatter.format(it * KMH_PER_MS, 0)} km/h"),
            icon = IosIcons.Speedometer,
        )
    }
}

private fun metres(value: Float): String = "${NumberFormatter.format(value, 0)} m"

private const val COORD_PLACES = 5
private const val KMH_PER_MS = 3.6f
