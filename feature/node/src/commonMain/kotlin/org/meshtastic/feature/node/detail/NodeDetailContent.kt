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
package org.meshtastic.feature.node.detail

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.database.entity.FirmwareRelease
import org.meshtastic.core.model.Node
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.air_quality
import org.meshtastic.core.resources.environment
import org.meshtastic.core.resources.loading
import org.meshtastic.core.resources.position
import org.meshtastic.core.resources.power
import org.meshtastic.core.resources.soil_water_metrics
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosActivityIndicator
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.feature.node.component.AdministrationSection
import org.meshtastic.feature.node.component.AirQualityInfoCards
import org.meshtastic.feature.node.component.DeviceLinksSection
import org.meshtastic.feature.node.component.EnvironmentMetrics
import org.meshtastic.feature.node.component.NotesSection
import org.meshtastic.feature.node.component.PositionInlineContent
import org.meshtastic.feature.node.component.PowerMetrics
import org.meshtastic.feature.node.component.SoilWaterMetrics
import org.meshtastic.feature.node.model.NodeDetailAction

/**
 * Shared content composable for node details, usable from both Android and Desktop.
 *
 * Renders a [Crossfade] between a loading spinner and the full [NodeDetailList] when the node is present. This
 * composable contains no Android-specific APIs — overlays (compass, bottom sheets, permission launchers) are handled by
 * the platform-specific screen wrapper.
 */
@Composable
fun NodeDetailContent(
    uiState: NodeDetailUiState,
    onAction: (NodeDetailAction) -> Unit,
    onFirmwareSelect: (FirmwareRelease) -> Unit,
    onSaveNotes: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    Crossfade(targetState = uiState.node != null, label = "NodeDetailContent", modifier = modifier) { isNodePresent ->
        if (isNodePresent && uiState.node != null) {
            NodeDetailList(
                node = uiState.node,
                ourNode = uiState.ourNode,
                uiState = uiState,
                listState = listState,
                onAction = onAction,
                onFirmwareSelect = onFirmwareSelect,
                onSaveNotes = onSaveNotes,
            )
        } else {
            val loadingDescription = stringResource(Res.string.loading)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                IosActivityIndicator(
                    modifier = Modifier.semantics { contentDescription = loadingDescription },
                    color = Ios.colors.systemGray,
                )
            }
        }
    }
}

/**
 * Scrollable list of node detail sections in the iPhone app's order: the hardware, "I want one", the node itself, its
 * latest environment, air quality and power readings, its logs and the actions on it. Android's extras follow: the
 * position map, telemetry requests, notes and administration.
 */
@Suppress("LongMethod")
@Composable
fun NodeDetailList(
    node: Node,
    ourNode: Node?,
    uiState: NodeDetailUiState,
    listState: LazyListState,
    onAction: (NodeDetailAction) -> Unit,
    onFirmwareSelect: (FirmwareRelease) -> Unit,
    onSaveNotes: (Int, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val metrics = uiState.metricsState
    val isLocal = metrics.isLocal
    LazyColumn(modifier = modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 24.dp)) {
        metrics.deviceHardware?.let { hardware ->
            item { NodeHardwareSection(deviceHardware = hardware, reportedTarget = metrics.reportedTarget) }
        }
        if (metrics.deviceLinks.isNotEmpty()) {
            item { DeviceLinksSection(links = metrics.deviceLinks) }
        }
        item { NodeIdentitySection(node = node, isLocal = isLocal) }
        if (node.hasEnvironmentMetrics) {
            item {
                ReadingsSection(stringResource(Res.string.environment)) {
                    EnvironmentMetrics(node, metrics.displayUnits, metrics.isFahrenheit)
                }
            }
        }
        if (node.hasAirQualityMetrics) {
            item {
                ReadingsSection(stringResource(Res.string.air_quality)) {
                    AirQualityInfoCards(node, metrics.airQualityMetrics, metrics.isFahrenheit)
                }
            }
        }
        if (node.hasSoilWaterMetrics) {
            item { ReadingsSection(stringResource(Res.string.soil_water_metrics)) { SoilWaterMetrics(node) } }
        }
        if (node.hasPowerMetrics) {
            item { ReadingsSection(stringResource(Res.string.power)) { PowerMetrics(node) } }
        }
        item { NodeLogsSection(node = node, availableLogs = uiState.availableLogs, onAction = onAction) }
        item {
            NodeActionsSection(
                node = node,
                isLocal = isLocal,
                hasConversation = uiState.hasConversation,
                lastTracerouteTime = uiState.lastTracerouteTime,
                displayUnits = metrics.displayUnits,
                onAction = onAction,
            )
        }
        if (node.latitude != 0.0 || node.longitude != 0.0) {
            item {
                ReadingsSection(stringResource(Res.string.position)) {
                    PositionInlineContent(node, ourNode, metrics.displayUnits, onAction, showActions = false)
                }
            }
        }
        if (!isLocal) {
            item {
                NodeTelemetryRequestsSection(
                    node = node,
                    lastRequestNeighborsTime = uiState.lastRequestNeighborsTime,
                    onAction = onAction,
                )
            }
        }
        item { NotesSection(node = node, onSaveNotes = onSaveNotes) }
        if (!metrics.isManaged) {
            item {
                AdministrationSection(
                    node = node,
                    metricsState = metrics,
                    onAction = onAction,
                    onFirmwareSelect = onFirmwareSelect,
                    sessionStatus = uiState.sessionStatus,
                    isEnsuringSession = uiState.isEnsuringSession,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
                )
            }
        }
    }
}

/** A section holding a node's latest readings: Android's metric cards on the iPhone app's grouped card. */
@Composable
private fun ReadingsSection(title: String, content: @Composable () -> Unit) {
    IosSection(header = title) { Column(Modifier.fillMaxWidth().padding(16.dp)) { content() } }
}
