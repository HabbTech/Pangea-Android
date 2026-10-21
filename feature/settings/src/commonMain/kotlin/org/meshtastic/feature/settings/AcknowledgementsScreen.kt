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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.variant.LibraryBadges
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.acknowledgements
import org.meshtastic.core.resources.library_count
import org.meshtastic.core.resources.open_source_description
import org.meshtastic.core.resources.open_source_libraries
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.ios.Ios

/**
 * Shared Acknowledgements screen listing the open-source libraries the app is built on, using the multiplatform
 * [LibrariesContainer] composable and [produceLibraries] from the AboutLibraries KMP library.
 *
 * Leverages the full M3 [LibrariesContainer] API:
 * - **header**: app branding with descriptive text
 * - **divider**: [HorizontalDivider] between library items for clean visual separation
 * - **footer**: total library count summary
 * - **contentPadding**: proper LazyColumn padding (avoids clipping during scroll)
 * - **badges**: per-row metadata badges (author, version, description, license, funding) via [LibraryBadges]
 * - **license dialog**: built-in license dialog wired by default via the container's `licenseDialogBody`
 *
 * Each platform provides a [jsonProvider] lambda that loads the library definitions JSON
 *
 * @see <a href="https://github.com/mikepenz/AboutLibraries">AboutLibraries KMP</a>
 */
@Composable
fun AcknowledgementsScreen(onNavigateUp: () -> Unit, jsonProvider: suspend () -> String) {
    val libraries by produceLibraries(jsonProvider)

    Scaffold(
        containerColor = Ios.colors.background,
        topBar = {
            MainAppBar(
                title = stringResource(Res.string.acknowledgements),
                canNavigateUp = true,
                onNavigateUp = onNavigateUp,
                ourNode = null,
                showNodeChip = false,
                actions = {},
                onClickChip = {},
            )
        },
    ) { paddingValues ->
        // The library list draws itself from Material's colours; scoped to iOS's here, so its rows, badges and dialog
        // read as the rest of the app does.
        val c = Ios.colors
        MaterialTheme(
            colorScheme =
            MaterialTheme.colorScheme.copy(
                background = c.background,
                onBackground = c.label,
                surface = c.background,
                onSurface = c.label,
                onSurfaceVariant = c.secondaryLabel,
                primary = c.tint,
                primaryContainer = c.fieldFill,
                onPrimaryContainer = c.secondaryLabel,
                secondaryContainer = c.fieldFill,
                onSecondaryContainer = c.secondaryLabel,
                tertiaryContainer = c.fieldFill,
                onTertiaryContainer = c.secondaryLabel,
            ),
        ) {
            LibrariesContainer(
                libraries = libraries,
                modifier = Modifier.fillMaxSize(),
                contentPadding = paddingValues,
                badges =
                LibraryBadges(version = true, author = true, description = true, license = true, funding = true),
                header = {
                    item {
                        AcknowledgementsHeader()
                        HorizontalDivider(thickness = 0.5.dp, color = Ios.colors.separator)
                    }
                },
                divider = { HorizontalDivider(thickness = 0.5.dp, color = Ios.colors.separator) },
                footer = {
                    val count = libraries?.libraries?.size ?: 0
                    if (count > 0) {
                        item {
                            HorizontalDivider(thickness = 0.5.dp, color = Ios.colors.separator)
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(Res.string.library_count, count),
                                    style = Ios.type.footnote,
                                    color = Ios.colors.secondaryLabel,
                                )
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun AcknowledgementsHeader() {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = stringResource(Res.string.open_source_libraries),
            style = Ios.type.headline,
            color = Ios.colors.label,
        )
        Text(
            text = stringResource(Res.string.open_source_description),
            style = Ios.type.footnote,
            color = Ios.colors.secondaryLabel,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
