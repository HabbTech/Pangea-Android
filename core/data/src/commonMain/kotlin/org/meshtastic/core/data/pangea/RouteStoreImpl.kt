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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import org.koin.core.annotation.Single
import org.meshtastic.core.common.di.ApplicationCoroutineScope
import org.meshtastic.core.database.getFileSystem
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.model.pangea.Route
import org.meshtastic.core.repository.RouteRepository

/** Where routes are saved: a `routes` folder among the app's own files, which App Files lists. */
internal expect fun routesDirectory(): Path

/** The phone's routes, read from disk once, in the background, when first asked for. */
@Single(binds = [RouteRepository::class])
class RouteStoreImpl(dispatchers: CoroutineDispatchers, applicationScope: ApplicationCoroutineScope) :
    RouteFiles(getFileSystem(), routesDirectory(), dispatchers.io) {
    init {
        applicationScope.launch { load() }
    }
}

/**
 * The store itself, on any file system and folder (tests use their own). Each route is a JSON file named by its id,
 * written whole and swapped in, so a crash mid-save leaves the last good copy. A file that can't be read is skipped and
 * left where it is.
 */
open class RouteFiles(
    private val fileSystem: FileSystem,
    private val folder: Path,
    private val io: CoroutineDispatcher,
) : RouteRepository {
    private val mutex = Mutex()
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val _routes = MutableStateFlow<List<Route>>(emptyList())
    override val routes: StateFlow<List<Route>> = _routes.asStateFlow()

    suspend fun load() = withContext(io) {
        mutex.withLock {
            val files = fileSystem.listOrNull(folder).orEmpty().filter { it.name.endsWith(EXTENSION) }
            val loaded =
                files.mapNotNull { path ->
                    runCatching { json.decodeFromString(Route.serializer(), fileSystem.read(path) { readUtf8() }) }
                        .onFailure { Logger.w { "🗺️ [Routes] Couldn't read ${path.name}" } }
                        .getOrNull()
                }
            // A route saved before the read finished is newer than its file's copy.
            _routes.update { current -> (current + loaded).distinctBy { it.id }.sortedWith(ORDER) }
        }
    }

    override suspend fun save(route: Route) = withContext(io) {
        mutex.withLock {
            val temp = folder / ".${route.id}$EXTENSION.tmp"
            runCatching {
                fileSystem.createDirectories(folder)
                fileSystem.write(temp) { writeUtf8(json.encodeToString(Route.serializer(), route)) }
                fileSystem.atomicMove(temp, file(route.id))
            }
                .onFailure { Logger.e(it) { "🗺️ [Routes] Save failed" } }
            _routes.update { list -> (list.filterNot { it.id == route.id } + route).sortedWith(ORDER) }
        }
    }

    override suspend fun delete(id: Int) = withContext(io) {
        mutex.withLock {
            runCatching { fileSystem.delete(file(id), mustExist = false) }
                .onFailure { Logger.e(it) { "🗺️ [Routes] Delete failed" } }
            _routes.update { list -> list.filterNot { it.id == id } }
        }
    }

    private fun file(id: Int): Path = folder / "$id$EXTENSION"

    private companion object {
        const val EXTENSION = ".json"

        /** iOS's Route List order: by name, then newest first. */
        val ORDER = compareBy<Route> { it.name.lowercase() }.thenByDescending { it.createdMillis }
    }
}
