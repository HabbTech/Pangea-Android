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
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.mapNotNull
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
 * [fixes]), adds each fix to the recording, and saves the route once a minute and on pause and finish, so a recording
 * cut short by the app being closed keeps what it had without rewriting a long route's file every few seconds.
 *
 * Fixes arrive on a background thread while Start, Pause and Finish come from the screen, so each change to the
 * recording and the save it queues happen together under [lock]: a fix heard as Finish is pressed can't queue its older
 * copy of the route after the finished one. Saves go one at a time, newest wins.
 */
@Single(binds = [RouteRecorder::class])
class RouteRecorderImpl(
    private val locationRepository: LocationRepository,
    private val routeRepository: RouteRepository,
    private val applicationScope: ApplicationCoroutineScope,
) : RouteRecorder {
    private val _recording = MutableStateFlow<RouteRecording?>(null)
    override val recording: StateFlow<RouteRecording?> = _recording.asStateFlow()

    private val lock = SynchronizedObject()
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
        synchronized(lock) {
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
    }

    override fun pause() {
        synchronized(lock) {
            val paused = _recording.value?.copy(paused = true) ?: return
            _recording.value = paused
            save(paused.route)
        }
    }

    override fun resume() {
        synchronized(lock) {
            val current = _recording.value?.takeIf { it.paused } ?: return
            _recording.value = current.copy(paused = false, resumed = true)
        }
    }

    override fun finish() {
        synchronized(lock) {
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
    }

    private fun heard(fix: GpsFix) {
        synchronized(lock) {
            val before = _recording.value ?: return
            val after = before.adding(fix)
            _recording.value = after
            val recorded = after.route.points.size != before.route.points.size
            if (recorded && nowMillis - lastSavedMillis >= SAVE_INTERVAL_MS) save(after.route)
        }
    }

    /** Queues [route] to be written; only ever called holding [lock], so the queue's order is the recording's. */
    private fun save(route: Route) {
        lastSavedMillis = nowMillis
        saves.trySend(route)
    }

    private companion object {
        const val FIX_INTERVAL_MS = 1_000L

        /** A minute: at a fix a second, a long walk's file would otherwise be rewritten hundreds of times an hour. */
        const val SAVE_INTERVAL_MS = 60_000L
    }
}
