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
package org.meshtastic.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.acknowledgements
import org.meshtastic.core.resources.library_count
import org.meshtastic.core.resources.open_source_description
import org.meshtastic.core.resources.open_source_libraries
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosAccessory
import org.meshtastic.core.ui.ios.IosCardShape
import org.meshtastic.core.ui.ios.IosInset
import org.meshtastic.core.ui.ios.IosPushedScreen
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSheet

/**
 * The open-source libraries the app is built on, as an iOS settings page: a large title, a note on what this is, then
 * one inset list with a row per library (its name, who made it, its version) and the count under it. A row opens the
 * library's licence in a sheet.
 *
 * The libraries come from AboutLibraries ([produceLibraries]); each platform provides a [jsonProvider] that loads the
 * library definitions JSON.
 *
 * @see <a href="https://github.com/mikepenz/AboutLibraries">AboutLibraries KMP</a>
 */
@Composable
fun AcknowledgementsScreen(onNavigateUp: () -> Unit, jsonProvider: suspend () -> String) {
    val libraries by produceLibraries(jsonProvider)
    val list = libraries?.libraries.orEmpty()
    var shown by remember { mutableStateOf<Library?>(null) }

    IosPushedScreen(title = stringResource(Res.string.acknowledgements), onBack = onNavigateUp, largeTitle = true) {
        item(key = "ack-header") {
            Column(modifier = Modifier.padding(horizontal = IosInset * 2, vertical = 8.dp)) {
                Text(
                    text = stringResource(Res.string.open_source_libraries),
                    style = Ios.type.headline,
                    color = Ios.colors.secondaryLabel,
                )
                Text(
                    text = stringResource(Res.string.open_source_description),
                    style = Ios.type.footnote,
                    color = Ios.colors.secondaryLabel,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        // One inset list, drawn row by row so a long list stays lazy: the first and last rows carry the card's corners.
        itemsIndexed(list, key = { _, lib -> lib.uniqueId }) { index, library ->
            val shape =
                when {
                    list.size == 1 -> IosCardShape
                    index == 0 -> RoundedCornerShape(topStart = CORNER, topEnd = CORNER)
                    index == list.lastIndex -> RoundedCornerShape(bottomStart = CORNER, bottomEnd = CORNER)
                    else -> RoundedCornerShape(0.dp)
                }
            Box(Modifier.padding(horizontal = IosInset).clip(shape).background(Ios.colors.groupedRow)) {
                IosRow(
                    title = library.name,
                    subtitle = library.developers.firstOrNull()?.name ?: library.organization?.name,
                    value = library.artifactVersion,
                    accessory = IosAccessory.Chevron,
                    onClick = { shown = library },
                )
                if (index > 0) {
                    Box(
                        Modifier.padding(start = IosInset)
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(Ios.colors.separator),
                    )
                }
            }
        }
        if (list.isNotEmpty()) {
            item(key = "ack-count") {
                Text(
                    text = stringResource(Res.string.library_count, list.size),
                    style = Ios.type.footnote,
                    color = Ios.colors.secondaryLabel,
                    modifier = Modifier.padding(horizontal = IosInset * 2, vertical = 8.dp),
                )
            }
        }
    }

    shown?.let { library -> LicenseSheet(library = library, onDismiss = { shown = null }) }
}

/** A library's licence, as iOS shows an acknowledgement: its name as the title, the licence text below. */
@Composable
private fun LicenseSheet(library: Library, onDismiss: () -> Unit) {
    IosSheet(title = library.name, onDismissRequest = onDismiss, grouped = true, grabber = true) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            library.licenses.forEach { license ->
                Text(
                    text = license.name,
                    style = Ios.type.headline,
                    color = Ios.colors.secondaryLabel,
                    modifier = Modifier.padding(start = IosInset * 2, end = IosInset * 2, top = 18.dp, bottom = 8.dp),
                )
                Box(
                    Modifier.padding(horizontal = IosInset)
                        .fillMaxWidth()
                        .clip(IosCardShape)
                        .background(Ios.colors.groupedRow)
                        .padding(IosInset),
                ) {
                    SelectionContainer {
                        Text(
                            text = license.licenseContent?.takeIf { it.isNotBlank() } ?: license.url.orEmpty(),
                            style = Ios.type.footnote,
                            color = Ios.colors.label,
                        )
                    }
                }
            }
        }
    }
}

/** The inset list's corner, iOS 26's. */
private val CORNER = 26.dp
