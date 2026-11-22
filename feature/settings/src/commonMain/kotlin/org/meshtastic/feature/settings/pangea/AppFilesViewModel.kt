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
import okio.Path.Companion.toPath
import okio.buffer
import okio.use
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.common.database.DatabaseManager
import org.meshtastic.core.common.util.CommonUri
import org.meshtastic.core.database.getDatabaseDirectory
import org.meshtastic.core.database.getFileSystem
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.repository.FileService

/** Backs App Files: reads the app's storage, and exports or deletes one of its files. */
@KoinViewModel
class AppFilesViewModel(
    private val databaseManager: DatabaseManager,
    private val fileService: FileService,
    private val dispatchers: CoroutineDispatchers,
) : ViewModel() {
    private val _storage = MutableStateFlow<AppStorage?>(null)
    val storage: StateFlow<AppStorage?> = _storage.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _storage.value =
                withContext(dispatchers.io) {
                    readAppStorage(
                        fs = getFileSystem(),
                        dbDir = getDatabaseDirectory(),
                        filesDir = appFilesDirectory(),
                        activeDb = databaseManager.activeDatabaseName(),
                    )
                }
        }
    }

    fun delete(file: StoredFile) {
        viewModelScope.launch {
            withContext(dispatchers.io) { getFileSystem().delete(file.path.toPath(), mustExist = false) }
            refresh()
        }
    }

    /** Copies [file] to [destination], a document the user picked to save it as. */
    fun export(file: StoredFile, destination: CommonUri) {
        viewModelScope.launch {
            fileService.write(destination) { sink ->
                getFileSystem().source(file.path.toPath()).buffer().use { source -> sink.writeAll(source) }
            }
        }
    }
}
