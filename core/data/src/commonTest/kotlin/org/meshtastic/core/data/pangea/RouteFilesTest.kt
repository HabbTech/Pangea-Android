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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.meshtastic.core.model.pangea.Route
import org.meshtastic.core.model.pangea.RoutePoint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RouteFilesTest {
    private val folder = "/files/routes".toPath()

    private fun route(id: Int, name: String, created: Long = 0) =
        Route(id = id, name = name, createdMillis = created, color = 0x33C758, points = listOf(RoutePoint(1.5, 2.5)))

    @Test
    fun savedRoutesComeBackFromDiskInIosOrder() = runTest {
        val fs = FakeFileSystem()
        val store = RouteFiles(fs, folder, Dispatchers.Unconfined)
        store.save(route(1, "trail", created = 10))
        store.save(route(2, "Trail", created = 20))
        store.save(route(3, "Alps"))

        val reopened = RouteFiles(fs, folder, Dispatchers.Unconfined)
        reopened.load()

        assertEquals(listOf(3, 2, 1), reopened.routes.value.map { it.id })
        assertEquals(route(3, "Alps"), reopened.routes.value.first())
    }

    @Test
    fun savingAgainReplacesAndDeletingRemovesTheFile() = runTest {
        val fs = FakeFileSystem()
        val store = RouteFiles(fs, folder, Dispatchers.Unconfined)
        store.save(route(7, "loop"))
        store.save(route(7, "loop").copy(enabled = true, notes = "windy"))

        assertEquals(listOf("windy"), store.routes.value.map { it.notes })
        assertEquals(listOf("7.json"), fs.list(folder).map { it.name })

        store.delete(7)
        assertEquals(emptyList(), store.routes.value)
        assertFalse(fs.exists(folder / "7.json"))
    }

    @Test
    fun anUnreadableFileIsSkippedAndKept() = runTest {
        val fs = FakeFileSystem()
        fs.createDirectories(folder)
        fs.write(folder / "9.json") { writeUtf8("not json") }
        RouteFiles(fs, folder, Dispatchers.Unconfined).save(route(4, "ok"))

        val store = RouteFiles(fs, folder, Dispatchers.Unconfined)
        store.load()

        assertEquals(listOf(4), store.routes.value.map { it.id })
        assertEquals("not json", fs.read(folder / "9.json") { readUtf8() })
    }

    @Test
    fun aSaveBeforeTheReadFinishesWins() = runTest {
        val fs = FakeFileSystem()
        RouteFiles(fs, folder, Dispatchers.Unconfined).save(route(5, "old"))

        val store = RouteFiles(fs, folder, Dispatchers.Unconfined)
        store.save(route(5, "new"))
        store.load()

        assertEquals(listOf("new"), store.routes.value.map { it.name })
    }
}
