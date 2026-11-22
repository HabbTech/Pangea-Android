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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.meshtastic.core.model.pangea.GpsFix

/** Platform-independent location object for KMP. */
expect class Location

/** This location as a [GpsFix], or null where the platform's location carries none. */
expect fun Location.toGpsFix(): GpsFix?

interface LocationRepository {
    /** Status of whether the app is actively subscribed to location changes. */
    val receivingLocationUpdates: StateFlow<Boolean>

    /** Observable flow for location updates */
    fun getLocations(): Flow<Location>

    /** Location updates about every [intervalMs], for a caller that needs more than the default rate (a recording). */
    fun getLocations(intervalMs: Long): Flow<Location> = getLocations()
}
