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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import org.meshtastic.core.model.Node
import org.meshtastic.core.repository.UiPrefs
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.pangea_map_empty_body
import org.meshtastic.core.resources.pangea_map_empty_title
import org.meshtastic.core.ui.component.PangeaLogoButton
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosDrawsUnderTabBar
import org.meshtastic.core.ui.ios.IosRadioChip
import org.meshtastic.core.ui.ios.LocalIosRadioChip
import org.meshtastic.core.ui.util.LocalMapPlainMode
import org.meshtastic.core.ui.util.LocalMapTopInset
import org.meshtastic.core.ui.util.LocalMapViewProvider
import org.meshtastic.core.ui.util.LocalNodeProfileSheet

// Parameter order predates the detekt rule (it was baselined); the ViewModel is handed on to the plain map.
@Suppress("ViewModelForwarding", "ComposableParamOrder")
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

    if (!expertMode) {
        PlainMap(ourNodeNum = ourNodeInfo?.num, viewModel = viewModel, modifier = modifier, waypointId = waypointId)
    } else {
        ExpertMap(
            ourNodeInfo = ourNodeInfo,
            isConnected = isConnected,
            onClickNodeChip = onClickNodeChip,
            navigateToNodeDetails = navigateToNodeDetails,
            modifier = modifier,
            waypointId = waypointId,
            sitePlannerNodeNum = sitePlannerNodeNum,
        )
    }
}

/**
 * Pangea standard mode's map (Pangea-Apple Map tab): the plain map, edge to edge under the status and tab bars, with
 * the logo button; tapping someone's radio opens their profile, and your own radio does nothing.
 */
@Suppress("ViewModelForwarding")
@Composable
private fun PlainMap(
    ourNodeNum: Int?,
    viewModel: SharedMapViewModel,
    modifier: Modifier = Modifier,
    waypointId: Int? = null,
) {
    IosDrawsUnderTabBar()
    var profileNum by remember { mutableStateOf<Int?>(null) }
    val profileSheet = LocalNodeProfileSheet.current
    Box(modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalMapPlainMode provides true) {
            LocalMapViewProvider.current?.MapView(
                modifier = Modifier.fillMaxSize(),
                navigateToNodeDetails = { num -> if (num != ourNodeNum) profileNum = num },
                waypointId = waypointId,
                sitePlannerNodeNum = null,
            )
        }
        PangeaLogoButton(Modifier.statusBarsPadding().padding(start = 16.dp, top = 8.dp))
        val positioned by viewModel.nodesWithPosition.collectAsStateWithLifecycle()
        if (positioned.none { it.num != ourNodeNum }) NoLocationsCard()
    }
    profileNum?.let { num -> profileSheet?.invoke(num) { profileNum = null } }
}

/** Expert Mode's map, as on iOS: the full map edge to edge under the logo and the radio chip, its controls in glass. */
@Composable
private fun ExpertMap(
    ourNodeInfo: Node?,
    isConnected: Boolean,
    onClickNodeChip: (Int) -> Unit,
    navigateToNodeDetails: (Int) -> Unit,
    modifier: Modifier = Modifier,
    waypointId: Int? = null,
    sitePlannerNodeNum: Int? = null,
) {
    IosDrawsUnderTabBar()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + HEADER_ROW
    Box(modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalMapTopInset provides topInset) {
            LocalMapViewProvider.current?.MapView(
                modifier = Modifier.fillMaxSize(),
                navigateToNodeDetails = navigateToNodeDetails,
                waypointId = waypointId,
                sitePlannerNodeNum = sitePlannerNodeNum,
            )
        }
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PangeaLogoButton()
            Spacer(Modifier.weight(1f))
            // iOS's radio chip; it shows a broken link while no radio is connected.
            val chip = LocalIosRadioChip.current
            if (chip != null) {
                val node = ourNodeInfo?.takeIf { isConnected }
                IosRadioChip(chip, onClick = node?.let { { onClickNodeChip(it.num) } })
            }
        }
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

/** The floating logo row's height, which the map's own controls stay below. */
private val HEADER_ROW = 60.dp
