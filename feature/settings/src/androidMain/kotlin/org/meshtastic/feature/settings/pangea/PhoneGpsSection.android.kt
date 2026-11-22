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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.mapNotNull
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.repository.LocationRepository
import org.meshtastic.core.repository.toGpsFix
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.grant_permission
import org.meshtastic.core.resources.pangea_gps_permission
import org.meshtastic.core.resources.pangea_gps_waiting
import org.meshtastic.core.resources.pangea_phone_gps
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.util.PermissionStatus
import org.meshtastic.core.ui.util.rememberLocationPermissionState

/**
 * iOS's Phone GPS rows: the phone's own fix (coordinate, accuracy, altitude, heading, speed), as the phone reports it.
 * Location access is asked for here when it hasn't been given.
 */
@Composable
actual fun PhoneGpsSection(modifier: Modifier) {
    val permission = rememberLocationPermissionState()
    IosSection(header = stringResource(Res.string.pangea_phone_gps), modifier = modifier) {
        if (permission.status != PermissionStatus.GRANTED) {
            IosRow(title = stringResource(Res.string.pangea_gps_permission), titleColor = Ios.colors.secondaryLabel)
            IosRow(
                title = stringResource(Res.string.grant_permission),
                titleColor = Ios.colors.tint,
                onClick = permission.request,
            )
        } else {
            val repository: LocationRepository = koinInject()
            val fix by
                remember(repository) { repository.getLocations().mapNotNull { it.toGpsFix() } }
                    .collectAsStateWithLifecycle(null)
            val current = fix
            if (current == null) {
                IosRow(title = stringResource(Res.string.pangea_gps_waiting), titleColor = Ios.colors.secondaryLabel)
            } else {
                GpsFixRows(current)
            }
        }
    }
}
