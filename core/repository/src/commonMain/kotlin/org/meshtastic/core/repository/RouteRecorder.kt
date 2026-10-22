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
import org.meshtastic.core.model.pangea.RouteRecording

/**
 * Records the phone's track as a route (Pangea-Apple `RouteRecorder` with `LocationsHandler`). One recording at a time,
 * held by the app rather than a screen, so it carries on while the user is elsewhere in the app and for as long as the
 * mesh service keeps the app running.
 */
interface RouteRecorder {
    /** The recording in progress, or null when there is none. */
    val recording: StateFlow<RouteRecording?>

    /** The phone's fixes, about once a second, while collected: the recorder screen's GPS status and map. */
    fun fixes(): Flow<GpsFix>

    /** Starts recording a new route called [name] in [color] (0xRRGGBB), saved straight away. */
    fun start(name: String, color: Int)

    fun pause()

    fun resume()

    /** Ends the recording and saves it as an enabled route with its distance and elevation gain. */
    fun finish()
}
