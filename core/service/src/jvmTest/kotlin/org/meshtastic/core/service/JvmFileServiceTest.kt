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
package org.meshtastic.core.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.meshtastic.core.common.util.CommonUri
import org.meshtastic.core.di.CoroutineDispatchers
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JvmFileServiceTest {
    private val dir: File = Files.createTempDirectory("file-service").toFile()
    private val service =
        JvmFileService(CoroutineDispatchers(Dispatchers.Unconfined, Dispatchers.Unconfined, Dispatchers.Unconfined))

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    @Test
    fun aFileUriFromTheDesktopDialogsIsTheFileItNames() = runTest {
        val file = File(dir, "Morning hike Log.csv")
        val uri = CommonUri.parse(file.toURI().toString())

        assertTrue(service.write(uri) { it.writeUtf8("Latitude,Longitude") })
        var text: String? = null
        assertTrue(service.read(uri) { text = it.readUtf8() })

        assertEquals("Latitude,Longitude", file.readText())
        assertEquals("Latitude,Longitude", text)
    }

    @Test
    fun aBarePathStillWorks() = runTest {
        val file = File(dir, "plain.txt")

        assertTrue(service.write(CommonUri.parse(file.path)) { it.writeUtf8("ok") })

        assertEquals("ok", file.readText())
    }
}
