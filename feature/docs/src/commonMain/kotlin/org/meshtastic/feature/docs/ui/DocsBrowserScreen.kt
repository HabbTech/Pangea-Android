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
package org.meshtastic.feature.docs.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.doc_loading
import org.meshtastic.core.resources.doc_no_documentation
import org.meshtastic.core.resources.doc_no_results
import org.meshtastic.core.resources.doc_search_placeholder
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosAccessory
import org.meshtastic.core.ui.ios.IosActivityIndicator
import org.meshtastic.core.ui.ios.IosPushedScreen
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSearchField
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.feature.docs.model.AIDocAssistantSessionState
import org.meshtastic.feature.docs.model.DocPage
import org.meshtastic.feature.docs.model.DocSection
import org.meshtastic.feature.docs.model.ModelReadiness

/** Main documentation browser screen showing a grouped TOC. */
@Suppress("LongMethod", "LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocsBrowserScreen(
    pages: List<DocPage>,
    isLoading: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectPage: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isAiSupported: Boolean = false,
    modelReadiness: ModelReadiness = ModelReadiness.Checking,
    showFab: Boolean = false,
    showChirpy: Boolean = false,
    chirpyState: AIDocAssistantSessionState = AIDocAssistantSessionState(),
    onChirpyToggle: () -> Unit = {},
    onChirpyDismiss: () -> Unit = {},
    onChirpyDraftChange: (String) -> Unit = {},
    onChirpySubmit: () -> Unit = {},
    onChirpyNavigateToPage: (String) -> Unit = {},
) {
    // iOS's pushed page: the back button and title in the bar, the search field, then the guides as sections. Chirpy
    // floats at the bottom right as a glass button.
    Box(modifier = modifier.fillMaxSize()) {
        IosPushedScreen(title = "Help & Documentation", onBack = onBack, largeTitle = true) {
            item {
                IosSearchField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = stringResource(Res.string.doc_search_placeholder),
                    glass = false,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            docsResults(pages = pages, isLoading = isLoading, searchQuery = searchQuery, onSelectPage = onSelectPage)
        }
        if (isAiSupported && showFab) {
            ChirpyFab(
                modelReadiness = modelReadiness,
                onClick = onChirpyToggle,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 24.dp),
            )
        }

        if (showChirpy) {
            ChirpyAssistantSheet(
                state = chirpyState,
                modelReadiness = modelReadiness,
                onDraftChange = onChirpyDraftChange,
                onSubmit = onChirpySubmit,
                onDismiss = onChirpyDismiss,
                onNavigateToPage = onChirpyNavigateToPage,
            )
        }
    }
}

/** The loading / empty / results states under the search field. */
private fun LazyListScope.docsResults(
    pages: List<DocPage>,
    isLoading: Boolean,
    searchQuery: String,
    onSelectPage: (String) -> Unit,
) {
    when {
        isLoading ->
            item {
                Column(
                    modifier = Modifier.fillParentMaxHeight(PLACEHOLDER_HEIGHT_FRACTION).fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    IosActivityIndicator(color = Ios.colors.systemGray)
                    Text(
                        text = stringResource(Res.string.doc_loading),
                        style = Ios.type.subheadline,
                        color = Ios.colors.secondaryLabel,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }

        pages.isEmpty() ->
            item {
                Column(
                    modifier = Modifier.fillParentMaxHeight(PLACEHOLDER_HEIGHT_FRACTION).fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text =
                        if (searchQuery.isNotBlank()) {
                            stringResource(Res.string.doc_no_results)
                        } else {
                            stringResource(Res.string.doc_no_documentation)
                        },
                        style = Ios.type.body,
                        color = Ios.colors.secondaryLabel,
                    )
                }
            }

        else -> {
            val userGuidePages = pages.filter { it.section == DocSection.UserGuide }.sortedBy { it.navOrder }
            val devGuidePages = pages.filter { it.section == DocSection.DeveloperGuide }.sortedBy { it.navOrder }
            if (userGuidePages.isNotEmpty()) {
                item { DocGuideSection(title = "User Guide", pages = userGuidePages, onSelectPage = onSelectPage) }
            }
            if (devGuidePages.isNotEmpty()) {
                item { DocGuideSection(title = "Developer Guide", pages = devGuidePages, onSelectPage = onSelectPage) }
            }
        }
    }
}

/** One guide as an iOS section: a row per page with its symbol and a chevron. */
@Composable
private fun DocGuideSection(title: String, pages: List<DocPage>, onSelectPage: (String) -> Unit) {
    IosSection(
        headerContent = {
            Text(
                text = title,
                style = Ios.type.headline,
                color = Ios.colors.secondaryLabel,
                modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 8.dp).semantics { heading() },
            )
        },
    ) {
        pages.forEach { page ->
            IosRow(
                title = page.title,
                icon = page.resolveIcon(),
                accessory = IosAccessory.Chevron,
                onClick = { onSelectPage(page.id) },
                modifier = Modifier.semantics { contentDescription = "Open ${page.title}" },
            )
        }
    }
}

/** Leaves room for the search field above, so the placeholder sits in the middle of what's left. */
private const val PLACEHOLDER_HEIGHT_FRACTION = 0.7f
