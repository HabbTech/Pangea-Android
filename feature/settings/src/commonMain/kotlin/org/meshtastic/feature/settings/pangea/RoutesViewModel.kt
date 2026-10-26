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
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.common.util.CommonUri
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.model.pangea.Route
import org.meshtastic.core.model.pangea.RouteCsv
import org.meshtastic.core.repository.FileService
import org.meshtastic.core.repository.RouteRepository
import kotlin.random.Random
import kotlin.random.nextInt

/** Backs the Route List: the saved routes, CSV import and export, edits and deletion. */
@KoinViewModel
class RoutesViewModel(
    private val routeRepository: RouteRepository,
    private val fileService: FileService,
    private val dispatchers: CoroutineDispatchers,
) : ViewModel() {
    val routes: StateFlow<List<Route>> = routeRepository.routes

    private val _badFile = MutableStateFlow(false)

    /** True while iOS's "Not a valid route file" alert is up. */
    val badFile: StateFlow<Boolean> = _badFile.asStateFlow()

    fun dismissBadFile() {
        _badFile.value = false
    }

    /**
     * Imports the CSV at [uri] as iOS does: named after the file ([fallbackName] when its name can't be told), in a
     * random colour, and shown on the mesh map.
     */
    fun import(uri: CommonUri, fallbackName: String) {
        viewModelScope.launch {
            var text: String? = null
            fileService.read(uri) { text = RouteCsv.readText(it) }
            val points = text?.let(RouteCsv::read)
            if (points == null) {
                _badFile.value = true
                return@launch
            }
            val fileName = withContext(dispatchers.io) { pickedFileName(uri) }
            routeRepository.save(
                Route(
                    id = Random.nextInt(Route.ID_RANGE),
                    name = fileName?.let(RouteCsv::routeName)?.ifBlank { null } ?: fallbackName,
                    createdMillis = nowMillis,
                    color = Random.nextInt(Route.COLOR_MASK + 1),
                    enabled = true,
                    points = points,
                ),
            )
        }
    }

    fun save(route: Route) {
        viewModelScope.launch { routeRepository.save(route) }
    }

    fun delete(route: Route) {
        viewModelScope.launch { routeRepository.delete(route.id) }
    }

    /** Writes [route] to [destination], a document the user picked, as iOS's CSV. */
    fun export(route: Route, destination: CommonUri) {
        viewModelScope.launch { fileService.write(destination) { it.writeUtf8(RouteCsv.write(route.points)) } }
    }
}
