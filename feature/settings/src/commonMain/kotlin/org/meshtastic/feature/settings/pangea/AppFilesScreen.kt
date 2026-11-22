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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.delete
import org.meshtastic.core.resources.download
import org.meshtastic.core.resources.pangea_active_database
import org.meshtastic.core.resources.pangea_app_files_section
import org.meshtastic.core.resources.pangea_db_missing
import org.meshtastic.core.resources.pangea_db_shm
import org.meshtastic.core.resources.pangea_db_store
import org.meshtastic.core.resources.pangea_db_total
import org.meshtastic.core.resources.pangea_db_wal
import org.meshtastic.core.resources.pangea_file_row
import org.meshtastic.core.resources.pangea_file_storage
import org.meshtastic.core.resources.pangea_radio_databases
import org.meshtastic.core.resources.pangea_radio_databases_footer
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosPushedScreen
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.ios.IosSwipeAction
import org.meshtastic.core.ui.ios.IosSwipeActions
import org.meshtastic.core.ui.ios.iosRowSeparator
import org.meshtastic.core.ui.util.rememberSaveFileLauncher

/** iOS's Phone GPS section: the phone's own fix, where the platform has one to show. */
@Composable expect fun PhoneGpsSection(modifier: Modifier = Modifier)

/**
 * The iPhone app's App Files page ("File Storage"): the phone's GPS, the active database's size by part, the other
 * radios' databases, and the app's own files, each of which swipes to Download (save a copy) or Delete.
 *
 * The databases are listed without actions: they are open or pooled while the app runs, so neither a copy nor a
 * deletion would be safe from here. Android keeps its map layers in the map's own options, so iOS's Map Data link has
 * no row here.
 */
@Composable
fun AppFilesScreen(viewModel: AppFilesViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val storage by viewModel.storage.collectAsStateWithLifecycle()
    var exporting by remember { mutableStateOf<StoredFile?>(null) }
    val save = rememberSaveFileLauncher { uri ->
        exporting?.let { viewModel.export(it, uri) }
        exporting = null
    }

    IosPushedScreen(title = stringResource(Res.string.pangea_file_storage), onBack = onBack, modifier = modifier) {
        item(key = "gps") { PhoneGpsSection() }
        storage?.let { s ->
            item(key = "active") { ActiveDatabaseSection(s) }
            if (s.otherDatabases.isNotEmpty()) {
                item(key = "others") {
                    IosSection(
                        header = stringResource(Res.string.pangea_radio_databases),
                        footer = stringResource(Res.string.pangea_radio_databases_footer),
                    ) {
                        s.otherDatabases.forEach { db ->
                            IosRow(title = db.name, icon = IosIcons.Cylinder, value = formatFileSize(db.sizeBytes))
                        }
                    }
                }
            }
            if (s.files.isNotEmpty()) {
                item(key = "files") {
                    FilesSection(
                        files = s.files,
                        onDownload = { file ->
                            exporting = file
                            save(file.name.substringAfterLast('/'), "application/octet-stream")
                        },
                        onDelete = viewModel::delete,
                    )
                }
            }
        }
    }
}

/** The active database by part, as iOS sizes it: Store, WAL, SHM and their total. */
@Composable
private fun ActiveDatabaseSection(storage: AppStorage) {
    val missing = stringResource(Res.string.pangea_db_missing)
    IosSection(header = stringResource(Res.string.pangea_active_database)) {
        IosRow(
            title = stringResource(Res.string.pangea_db_store),
            value = storage.activeStore?.let(::formatFileSize) ?: missing,
        )
        IosRow(
            title = stringResource(Res.string.pangea_db_wal),
            value = storage.activeWal?.let(::formatFileSize) ?: missing,
        )
        IosRow(
            title = stringResource(Res.string.pangea_db_shm),
            value = storage.activeShm?.let(::formatFileSize) ?: missing,
        )
        IosRow(title = stringResource(Res.string.pangea_db_total), value = formatFileSize(storage.activeTotal))
    }
}

/** The app's files, a row each ("name - date - size"), swiping to Download or Delete as iOS's do. */
@Composable
private fun FilesSection(files: List<StoredFile>, onDownload: (StoredFile) -> Unit, onDelete: (StoredFile) -> Unit) {
    val download = stringResource(Res.string.download)
    val delete = stringResource(Res.string.delete)
    IosSection(header = stringResource(Res.string.pangea_app_files_section)) {
        files.forEach { file ->
            IosSwipeActions(
                actions =
                listOf(
                    IosSwipeAction(download, IosIcons.ShareDown, Ios.colors.systemGray) { onDownload(file) },
                    IosSwipeAction(delete, IosIcons.Trash, Ios.colors.systemRed) { onDelete(file) },
                ),
                modifier = Modifier.iosRowSeparator(),
            ) {
                IosRow(
                    title =
                    stringResource(
                        Res.string.pangea_file_row,
                        file.name,
                        file.modifiedMillis?.let(DateFormatter::formatDateTimeShort).orEmpty(),
                        formatFileSize(file.sizeBytes),
                    ),
                    icon = IosIcons.DocText,
                    showSeparator = false,
                )
            }
        }
    }
}
