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

import okio.FileSystem
import okio.Path
import org.meshtastic.core.common.util.NumberFormatter
import org.meshtastic.core.database.DatabaseConstants

/** A file the app keeps, as App Files lists it: [name] is its path under the files directory. */
data class StoredFile(val path: String, val name: String, val sizeBytes: Long, val modifiedMillis: Long?)

/** One radio's database: its file with its WAL, SHM and journal, added up. */
data class StoredDatabase(val name: String, val sizeBytes: Long)

/**
 * What App Files shows: the active database's parts (null where a part is missing), the other radios' databases, and
 * the app's own files.
 */
data class AppStorage(
    val activeStore: Long?,
    val activeWal: Long?,
    val activeShm: Long?,
    val otherDatabases: List<StoredDatabase>,
    val files: List<StoredFile>,
) {
    val activeTotal: Long
        get() = (activeStore ?: 0) + (activeWal ?: 0) + (activeShm ?: 0)
}

/** The app's own files directory, or null where the platform keeps none worth listing. */
expect fun appFilesDirectory(): Path?

/**
 * Reads the database directory [dbDir] and the files directory [filesDir] on [fs]. The databases are grouped by name
 * (`meshtastic_database_…`, with or without `.db`, and their `-wal`, `-shm` and `-journal` files); [activeDb] is the
 * one in use. The files directory leaves out the settings store (`datastore/`), which isn't the user's to delete, and
 * Android's empty `profileInstalled` marker.
 */
internal fun readAppStorage(fs: FileSystem, dbDir: Path, filesDir: Path?, activeDb: String?): AppStorage {
    val dbFiles = fs.listOrNull(dbDir).orEmpty().filter { it.name.startsWith(DatabaseConstants.DB_PREFIX) }
    val sizes = dbFiles.associate { it.name to (fs.metadataOrNull(it)?.size ?: 0L) }
    fun part(base: String, suffix: String): Long? = sizes["$base$suffix"] ?: sizes["$base.db$suffix"]

    val others =
        dbFiles
            .map { databaseBaseName(it.name) }
            .distinct()
            .filter { it != activeDb }
            .map { base -> StoredDatabase(base, DB_SUFFIXES.sumOf { part(base, it) ?: 0L }) }
            .sortedBy { it.name }

    val files =
        filesDir
            ?.let { dir -> if (fs.metadataOrNull(dir)?.isDirectory == true) fs.listRecursively(dir).toList() else null }
            .orEmpty()
            .filter { path ->
                val relative = path.relativeTo(filesDir!!).toString()
                val meta = fs.metadataOrNull(path)
                meta?.isRegularFile == true &&
                    !relative.startsWith(SETTINGS_STORE_DIR) &&
                    path.name != PROFILE_MARKER &&
                    !path.name.startsWith(".")
            }
            .map { path ->
                val meta = fs.metadataOrNull(path)
                StoredFile(
                    path = path.toString(),
                    name = path.relativeTo(filesDir!!).toString().replace('\\', '/'),
                    sizeBytes = meta?.size ?: 0L,
                    modifiedMillis = meta?.lastModifiedAtMillis,
                )
            }
            .sortedBy { it.name.lowercase() }

    return AppStorage(
        activeStore = activeDb?.let { part(it, "") },
        activeWal = activeDb?.let { part(it, "-wal") },
        activeShm = activeDb?.let { part(it, "-shm") },
        otherDatabases = others,
        files = files,
    )
}

/** A database file's name without its `.db`, `-wal`, `-shm`, `-journal` or `.lck` ending. */
internal fun databaseBaseName(fileName: String): String {
    var name = fileName
    for (suffix in listOf("-wal", "-shm", "-journal", ".lck")) name = name.removeSuffix(suffix)
    return name.removeSuffix(".db")
}

/** A size as iOS's file byte counter writes it: bytes, whole KB, then MB and GB to one and two places. */
internal fun formatFileSize(bytes: Long): String = when {
    bytes < KILO -> "$bytes bytes"
    bytes < KILO * KILO -> "${(bytes + KILO / 2) / KILO} KB"
    bytes < KILO * KILO * KILO -> "${NumberFormatter.format(bytes / (KILO * KILO).toDouble(), 1)} MB"
    else -> "${NumberFormatter.format(bytes / (KILO * KILO * KILO).toDouble(), 2)} GB"
}

private val DB_SUFFIXES = listOf("", "-wal", "-shm", "-journal")
private const val SETTINGS_STORE_DIR = "datastore"
private const val PROFILE_MARKER = "profileInstalled"

/** iOS counts file sizes in thousands, as storage makers do. */
private const val KILO = 1000L
