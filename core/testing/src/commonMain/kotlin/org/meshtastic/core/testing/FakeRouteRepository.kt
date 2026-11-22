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
package org.meshtastic.core.testing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.meshtastic.core.model.pangea.Route
import org.meshtastic.core.repository.RouteRepository

/** Saved routes held in memory, in the order they were saved. */
class FakeRouteRepository(initial: List<Route> = emptyList()) : RouteRepository {
    override val routes = MutableStateFlow(initial)

    override suspend fun save(route: Route) {
        routes.update { list -> list.filterNot { it.id == route.id } + route }
    }

    override suspend fun delete(id: Int) {
        routes.update { list -> list.filterNot { it.id == id } }
    }
}
