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

import kotlinx.coroutines.flow.StateFlow
import org.meshtastic.core.model.pangea.Route

/** The phone's saved routes (Pangea-Apple `RouteEntity`), kept apart from any radio's database. */
interface RouteRepository {
    /** Every route, by name and then newest first, as iOS's Route List sorts them. Empty until first read. */
    val routes: StateFlow<List<Route>>

    /** Adds [route], or replaces the saved route with its id. */
    suspend fun save(route: Route)

    suspend fun delete(id: Int)
}
