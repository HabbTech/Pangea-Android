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
package org.meshtastic.feature.map.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.close
import org.meshtastic.core.resources.nodes_at_this_location
import org.meshtastic.core.ui.component.NodeChip
import org.meshtastic.core.ui.ios.IosAccessory
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.ios.IosSheet

/**
 * Lists the nodes sharing one spot, when zooming in further will not separate them: an iOS sheet at the medium detent
 * with one row per node.
 *
 * Renders nothing when [members] is empty, so callers can hand it state directly without a guard.
 */
@Composable
fun ClusterMembersDialog(
    members: List<ClusterMemberEntry>,
    onMemberClick: (Int) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (members.isEmpty()) return

    IosSheet(
        title = stringResource(Res.string.nodes_at_this_location),
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        actionLabel = stringResource(Res.string.close),
        onAction = onDismissRequest,
        grouped = true,
        heightFraction = MEDIUM_DETENT,
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            IosSection {
                // Deduped by node number: a node can reach the map from two sources, or arrive as a `num = 0`
                // placeholder.
                val rows = members.distinctBy { it.nodeNum }
                rows.forEach { member ->
                    IosRow(
                        title = member.title,
                        subtitle = member.subtitle.takeIf { it.isNotBlank() },
                        leading = member.node?.let { { NodeChip(node = it) } },
                        leadingWidth = if (member.node != null) CHIP_COLUMN.dp else null,
                        accessory = IosAccessory.Chevron,
                        onClick = { onMemberClick(member.nodeNum) },
                    )
                }
            }
        }
    }
}

/** iOS's medium detent: half the screen, the map still in view above. */
private const val MEDIUM_DETENT = 0.5f

/** Room for a node's short-name chip ahead of its name. */
private const val CHIP_COLUMN = 84
