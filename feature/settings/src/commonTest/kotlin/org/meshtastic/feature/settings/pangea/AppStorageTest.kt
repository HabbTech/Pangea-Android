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

import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppStorageTest {

    private val dbDir = "/data/databases".toPath()
    private val filesDir = "/data/files".toPath()

    private fun FakeFileSystem.file(path: String, bytes: Int) {
        val p = path.toPath()
        createDirectories(p.parent!!)
        write(p) { write(ByteArray(bytes)) }
    }

    @Test
    fun theActiveDatabaseIsSizedByPartAndTheOthersAddedUp() {
        val fs = FakeFileSystem()
        fs.file("/data/databases/meshtastic_database_aaa", 4096)
        fs.file("/data/databases/meshtastic_database_aaa-wal", 1000)
        fs.file("/data/databases/meshtastic_database_aaa-shm", 32768)
        fs.file("/data/databases/meshtastic_database_aaa.lck", 0)
        fs.file("/data/databases/meshtastic_database_bbb", 2000)
        fs.file("/data/databases/meshtastic_database_bbb-wal", 500)
        fs.file("/data/databases/unrelated.db", 9999)

        val storage = readAppStorage(fs, dbDir, filesDir = null, activeDb = "meshtastic_database_aaa")

        assertEquals(4096L, storage.activeStore)
        assertEquals(1000L, storage.activeWal)
        assertEquals(32768L, storage.activeShm)
        assertEquals(4096L + 1000 + 32768, storage.activeTotal)
        assertEquals(listOf(StoredDatabase("meshtastic_database_bbb", 2500)), storage.otherDatabases)
    }

    @Test
    fun desktopDatabasesWithADbExtensionAreReadToo() {
        val fs = FakeFileSystem()
        fs.file("/data/databases/meshtastic_database_default.db", 8192)
        fs.file("/data/databases/meshtastic_database_default.db-wal", 100)

        val storage = readAppStorage(fs, dbDir, filesDir = null, activeDb = "meshtastic_database_default")

        assertEquals(8192L, storage.activeStore)
        assertEquals(100L, storage.activeWal)
        assertNull(storage.activeShm)
    }

    @Test
    fun filesLeaveOutTheSettingsStoreAndTheProfileMarker() {
        val fs = FakeFileSystem()
        fs.createDirectories(dbDir)
        fs.file("/data/files/datastore/ui_ds.preferences_pb", 94)
        fs.file("/data/files/profileInstalled", 0)
        fs.file("/data/files/layers/trail.kml", 1234)
        fs.file("/data/files/export.csv", 10)

        val names = readAppStorage(fs, dbDir, filesDir, activeDb = null).files.map { it.name }

        assertEquals(listOf("export.csv", "layers/trail.kml"), names)
    }

    @Test
    fun sizesReadAsIosWritesThem() {
        assertEquals("512 bytes", formatFileSize(512))
        assertEquals("33 KB", formatFileSize(32_768))
        assertEquals("2.7 MB", formatFileSize(2_711_552))
        assertEquals("1.50 GB", formatFileSize(1_500_000_000))
    }

    @Test
    fun aDatabaseFileNameLosesItsEndings() {
        assertEquals("meshtastic_database_x", databaseBaseName("meshtastic_database_x-wal"))
        assertEquals("meshtastic_database_x", databaseBaseName("meshtastic_database_x.db-shm"))
        assertEquals("meshtastic_database_x", databaseBaseName("meshtastic_database_x.lck"))
    }
}
