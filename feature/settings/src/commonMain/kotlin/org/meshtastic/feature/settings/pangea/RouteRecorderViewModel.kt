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

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.common.util.LocaleUnitsProvider
import org.meshtastic.core.common.util.MeasurementSystem
import org.meshtastic.core.model.pangea.GpsFix
import org.meshtastic.core.model.pangea.Route
import org.meshtastic.core.model.pangea.RouteRecording
import org.meshtastic.core.repository.RouteRecorder
import kotlin.random.Random

/** Backs the Route Recorder: the app's recording, the phone's fix while nothing records, and the user's units. */
@KoinViewModel
class RouteRecorderViewModel(private val recorder: RouteRecorder, localeUnitsProvider: LocaleUnitsProvider) :
    ViewModel() {
    val recording: StateFlow<RouteRecording?> = recorder.recording

    val units: StateFlow<MeasurementSystem> = localeUnitsProvider.measurementSystem

    /** The phone's fixes, for the GPS status before a recording starts (a recording carries its own). */
    fun fixes(): Flow<GpsFix> = recorder.fixes()

    /** Starts a recording called [name] in a random colour, as iOS's recorder does. */
    fun start(name: String) = recorder.start(name, Random.nextInt(Route.COLOR_MASK + 1))

    fun pause() = recorder.pause()

    fun resume() = recorder.resume()

    fun finish() = recorder.finish()
}
