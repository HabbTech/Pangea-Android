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
package org.meshtastic.feature.settings.radio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.Node
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.clean_node_database_description
import org.meshtastic.core.resources.clean_node_database_title
import org.meshtastic.core.resources.clean_nodes_older_than
import org.meshtastic.core.resources.clean_now
import org.meshtastic.core.resources.clean_unknown_nodes
import org.meshtastic.core.resources.nodes_queued_for_deletion
import org.meshtastic.core.ui.component.NodeChip
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosAccessory
import org.meshtastic.core.ui.ios.IosInset
import org.meshtastic.core.ui.ios.IosPushedColumnScreen
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.ios.IosSlider
import org.meshtastic.core.ui.ios.iosPressable
import org.meshtastic.core.ui.ios.iosRowSeparator

/**
 * Composable screen for cleaning the node database. Allows users to specify criteria for deleting nodes. The list of
 * nodes to be deleted updates automatically as filter criteria change.
 *
 * Drawn as an iOS grouped form, as the iPhone app's own stale-node purge in App Settings: the age as a row with its
 * slider (the caution as the section's footer), the unknown-only switch, the nodes about to go, and Clean Now in red in
 * a card of its own.
 */
@Composable
fun CleanNodeDatabaseScreen(viewModel: CleanNodeDatabaseViewModel, onBack: () -> Unit) {
    val olderThanDays by viewModel.olderThanDays.collectAsStateWithLifecycle()
    val onlyUnknownNodes by viewModel.onlyUnknownNodes.collectAsStateWithLifecycle()
    val nodesToDelete by viewModel.nodesToDelete.collectAsStateWithLifecycle()

    SideEffect(olderThanDays, onlyUnknownNodes) { viewModel.getNodesToDelete() }

    IosPushedColumnScreen(
        title = stringResource(Res.string.clean_node_database_title),
        onBack = onBack,
        largeTitle = true,
    ) {
        Column(Modifier.fillMaxWidth()) {
            IosSection(footer = stringResource(Res.string.clean_node_database_description)) {
                DaysThresholdFilter(
                    olderThanDays = olderThanDays,
                    onlyUnknownNodes = onlyUnknownNodes,
                    onDaysChanged = viewModel::onOlderThanDaysChanged,
                )
                IosRow(
                    title = stringResource(Res.string.clean_unknown_nodes),
                    accessory =
                    IosAccessory.Switch(
                        checked = onlyUnknownNodes,
                        onCheckedChange = viewModel::onOnlyUnknownNodesChanged,
                    ),
                )
            }
            NodesDeletionPreview(nodesToDelete = nodesToDelete)
            CleanNowSection(
                enabled = nodesToDelete.isNotEmpty(),
                onClick = { if (nodesToDelete.isNotEmpty()) viewModel.requestCleanNodes() },
            )
        }
    }
}

private const val MIN_UNKNOWN_DAYS_THRESHOLD = 0f
private const val MIN_KNOWN_DAYS_THRESHOLD = 7f
private const val MAX_DAYS_THRESHOLD = 365f

/**
 * Composable for the "older than X days" filter. This filter is always active.
 *
 * @param olderThanDays The number of days for the filter.
 * @param onlyUnknownNodes Whether the "only unknown nodes" filter is enabled.
 * @param onDaysChanged Callback for when the number of days changes.
 */
@Composable
private fun DaysThresholdFilter(olderThanDays: Float, onlyUnknownNodes: Boolean, onDaysChanged: (Float) -> Unit) {
    val valueRange =
        if (onlyUnknownNodes) {
            MIN_UNKNOWN_DAYS_THRESHOLD..MAX_DAYS_THRESHOLD
        } else {
            MIN_KNOWN_DAYS_THRESHOLD..MAX_DAYS_THRESHOLD
        }
    // A day per stop, without iOS's stop dots: a year of them would be a solid line.
    Column(Modifier.iosRowSeparator().fillMaxWidth().padding(horizontal = IosInset, vertical = 11.dp)) {
        Text(
            text = stringResource(Res.string.clean_nodes_older_than, olderThanDays.toInt()),
            style = Ios.type.body,
            color = Ios.colors.label,
        )
        IosSlider(
            value = olderThanDays,
            onValueChange = { onDaysChanged(kotlin.math.round(it)) },
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Composable for displaying the list of nodes queued for deletion.
 *
 * @param nodesToDelete The list of nodes to be deleted.
 */
@Composable
private fun NodesDeletionPreview(nodesToDelete: List<Node>) {
    IosSection(header = stringResource(Res.string.nodes_queued_for_deletion, nodesToDelete.size).trimEnd(':')) {
        if (nodesToDelete.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = IosInset, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                nodesToDelete.forEach { node -> NodeChip(node = node) }
            }
        }
    }
}

/** iOS's destructive form button: red words centred in a card of their own, grey while there is nothing to clean. */
@Composable
private fun CleanNowSection(enabled: Boolean, onClick: () -> Unit) {
    IosSection {
        Box(
            Modifier.fillMaxWidth().heightIn(min = 52.dp).iosPressable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(Res.string.clean_now),
                style = Ios.type.body.copy(fontWeight = FontWeight.SemiBold),
                color = if (enabled) Ios.colors.systemRed else Ios.colors.tertiaryLabel,
            )
        }
    }
}
