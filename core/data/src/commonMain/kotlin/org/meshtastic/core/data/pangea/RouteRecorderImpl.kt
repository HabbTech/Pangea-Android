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
package org.meshtastic.core.data.pangea

import co.touchlab.kermit.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import org.koin.core.annotation.Single
import org.meshtastic.core.common.di.ApplicationCoroutineScope
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.model.pangea.GpsFix
import org.meshtastic.core.model.pangea.Route
import org.meshtastic.core.model.pangea.RouteRecording
import org.meshtastic.core.repository.LocationRepository
import org.meshtastic.core.repository.RouteRecorder
import org.meshtastic.core.repository.RouteRepository
import org.meshtastic.core.repository.toGpsFix
import kotlin.random.Random
import kotlin.random.nextInt

/**
 * The app's route recorder. It listens to the phone's location only while recording (and while a screen collects
 * [fixes]), adds each fix to the recording, and saves the route every few seconds, so a recording cut short by the app
 * being closed keeps what it had. Saves go one at a time, newest wins, so an older save can't land after the last.
 */
@Single(binds = [RouteRecorder::class])
class RouteRecorderImpl(
    private val locationRepository: LocationRepository,
    private val routeRepository: RouteRepository,
    private val applicationScope: ApplicationCoroutineScope,
) : RouteRecorder {
    private val _recording = MutableStateFlow<RouteRecording?>(null)
    override val recording: StateFlow<RouteRecording?> = _recording.asStateFlow()

    private val saves = Channel<Route>(Channel.CONFLATED)
    private var listening: Job? = null
    private var lastSavedMillis = 0L

    init {
        applicationScope.launch { for (route in saves) routeRepository.save(route) }
    }

    override fun fixes(): Flow<GpsFix> = locationRepository
        .getLocations(FIX_INTERVAL_MS)
        .mapNotNull { it.toGpsFix() }
        .catch { Logger.w(it) { "🗺️ [Routes] No location updates" } }

    override fun start(name: String, color: Int) {
        if (_recording.value != null) return
        val route =
            Route(
                id = Random.nextInt(Route.ID_RANGE),
                name = name,
                createdMillis = nowMillis,
                color = color and Route.COLOR_MASK,
            )
        _recording.value = RouteRecording(route)
        save(route)
        listening = applicationScope.launch { fixes().collect(::heard) }
    }

    override fun pause() {
        _recording.updateAndGet { it?.copy(paused = true) }?.let { save(it.route) }
    }

    override fun resume() {
        _recording.update { current -> current?.takeIf { it.paused }?.copy(paused = false, resumed = true) ?: current }
    }

    override fun finish() {
        val done = _recording.value ?: return
        listening?.cancel()
        listening = null
        _recording.value = null
        save(
            done.route.copy(
                enabled = true,
                endMillis = nowMillis,
                distanceMeters = done.distanceMeters,
                elevationGainMeters = done.elevationGainMeters,
            ),
        )
    }

    private fun heard(fix: GpsFix) {
        val before = _recording.value?.route?.points?.size
        val after = _recording.updateAndGet { it?.adding(fix) } ?: return
        if (after.route.points.size != before && nowMillis - lastSavedMillis >= SAVE_INTERVAL_MS) save(after.route)
    }

    private fun save(route: Route) {
        lastSavedMillis = nowMillis
        saves.trySend(route)
    }

    private companion object {
        const val FIX_INTERVAL_MS = 1_000L
        const val SAVE_INTERVAL_MS = 10_000L
    }
}
