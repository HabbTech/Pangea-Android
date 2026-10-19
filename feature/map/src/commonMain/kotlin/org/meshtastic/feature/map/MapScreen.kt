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
package org.meshtastic.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.map
import org.meshtastic.core.resources.pangea_map_empty_body
import org.meshtastic.core.resources.pangea_map_empty_title
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.component.PangeaLogoButton
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosDrawsUnderTabBar
import org.meshtastic.core.ui.util.LocalMapPlainMode
import org.meshtastic.core.ui.util.LocalMapViewProvider
import org.meshtastic.core.ui.util.LocalNodeProfileSheet

@Composable
fun MapScreen(
    onClickNodeChip: (Int) -> Unit,
    navigateToNodeDetails: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SharedMapViewModel,
    waypointId: Int? = null,
    sitePlannerNodeNum: Int? = null,
) {
    val ourNodeInfo by viewModel.ourNodeInfo.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val expertMode by koinInject<UiPrefs>().expertModeEnabled.collectAsStateWithLifecycle()

    // Pangea standard mode (Pangea-Apple Map tab): the plain map, edge to edge under the status and tab bars, with the
    // logo button; tapping someone's radio opens their profile, and your own radio does nothing.
    if (!expertMode) {
        IosDrawsUnderTabBar()
        var profileNum by remember { mutableStateOf<Int?>(null) }
        val profileSheet = LocalNodeProfileSheet.current
        Box(modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalMapPlainMode provides true) {
                LocalMapViewProvider.current?.MapView(
                    modifier = Modifier.fillMaxSize(),
                    navigateToNodeDetails = { num -> if (num != ourNodeInfo?.num) profileNum = num },
                    waypointId = waypointId,
                    sitePlannerNodeNum = null,
                )
            }
            PangeaLogoButton(Modifier.statusBarsPadding().padding(start = 16.dp, top = 8.dp))
            val positioned by viewModel.nodesWithPosition.collectAsStateWithLifecycle()
            if (positioned.none { it.num != ourNodeInfo?.num }) NoLocationsCard()
        }
        profileNum?.let { num -> profileSheet?.invoke(num) { profileNum = null } }
        return
    }

    @Suppress("ViewModelForwarding")
    Scaffold(
        modifier = modifier,
        topBar = {
            MainAppBar(
                title = stringResource(Res.string.map),
                ourNode = ourNodeInfo,
                showNodeChip = ourNodeInfo != null && isConnected,
                canNavigateUp = false,
                onNavigateUp = {},
                actions = {},
                onClickChip = { onClickNodeChip(it.num) },
            )
        },
    ) { paddingValues ->
        LocalMapViewProvider.current?.MapView(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            navigateToNodeDetails = navigateToNodeDetails,
            waypointId = waypointId,
            sitePlannerNodeNum = sitePlannerNodeNum,
        )
    }
}

/** Shown over an empty plain map, as on iOS: why there's nobody on it yet. */
@Composable
private fun BoxScope.NoLocationsCard() {
    val c = Ios.colors
    Column(
        Modifier.align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 68.dp)
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(22.dp), ambientColor = c.chromeShadow, spotColor = c.chromeShadow)
            .clip(RoundedCornerShape(22.dp))
            .background(c.chrome)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(Res.string.pangea_map_empty_title), style = Ios.type.headline, color = c.label)
        Text(stringResource(Res.string.pangea_map_empty_body), style = Ios.type.subheadline, color = c.secondaryLabel)
    }
}
