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
package org.meshtastic.feature.discovery.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.database.entity.DiscoverySessionEntity
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.back
import org.meshtastic.core.resources.delete
import org.meshtastic.core.resources.discovery_delete_session
import org.meshtastic.core.resources.discovery_delete_session_confirm
import org.meshtastic.core.resources.discovery_empty_history
import org.meshtastic.core.resources.discovery_history
import org.meshtastic.core.resources.discovery_scan_complete
import org.meshtastic.core.resources.discovery_scan_incomplete
import org.meshtastic.core.resources.discovery_unique_nodes
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.icon.CheckCircle
import org.meshtastic.core.ui.icon.Delete
import org.meshtastic.core.ui.icon.History
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Warning
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosCardShape
import org.meshtastic.core.ui.ios.IosGlassIconButton
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosTopBar
import org.meshtastic.feature.discovery.DiscoveryHistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryHistoryScreen(
    viewModel: DiscoveryHistoryViewModel,
    onNavigateUp: () -> Unit,
    onNavigateToDetail: (sessionId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        containerColor = Ios.colors.groupedBackground,
        topBar = {
            IosTopBar(
                title = stringResource(Res.string.discovery_history),
                showTitle = true,
                background = Ios.colors.groupedBackground,
                leading = {
                    IosGlassIconButton(
                        IosIcons.ChevronLeft,
                        contentDescription = stringResource(Res.string.back),
                        onClick = onNavigateUp,
                    )
                },
            )
        },
    ) { padding ->
        if (sessions.isEmpty()) {
            EmptyHistoryState(modifier = Modifier.padding(padding).fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            ) {
                items(sessions, key = { it.id }) { session ->
                    SessionListItem(
                        session = session,
                        onClick = { onNavigateToDetail(session.id) },
                        onDelete = { viewModel.deleteSession(session.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = MeshtasticIcons.History,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Ios.colors.secondaryLabel,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.discovery_empty_history),
                style = Ios.type.body,
                color = Ios.colors.secondaryLabel,
            )
        }
    }
}

@Composable
private fun SessionListItem(session: DiscoverySessionEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val sessionDescription =
        "${formatTimestamp(session.timestamp)}, ${session.presetsScanned}, " +
            "${session.totalUniqueNodes} unique nodes, " +
            if (session.completionStatus == "complete") "complete" else "incomplete"

    Card(
        shape = IosCardShape,
        colors = CardDefaults.cardColors(containerColor = Ios.colors.groupedRow),
        modifier =
        Modifier.fillMaxWidth().clickable(onClick = onClick).semantics(mergeDescendants = true) {
            contentDescription = sessionDescription
        },
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CompletionStatusIcon(session.completionStatus)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = formatTimestamp(session.timestamp), style = Ios.type.headline)
                Spacer(Modifier.height(4.dp))
                Text(text = session.presetsScanned, style = Ios.type.subheadline, color = Ios.colors.secondaryLabel)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(Res.string.discovery_unique_nodes, session.totalUniqueNodes),
                    style = Ios.type.footnote,
                    color = Ios.colors.secondaryLabel,
                )
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    imageVector = MeshtasticIcons.Delete,
                    contentDescription = stringResource(Res.string.discovery_delete_session),
                    tint = Ios.colors.systemRed,
                )
            }
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                onDelete()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Composable
private fun CompletionStatusIcon(status: String) {
    if (status == "complete") {
        Icon(
            imageVector = MeshtasticIcons.CheckCircle,
            contentDescription = stringResource(Res.string.discovery_scan_complete),
            tint = Ios.colors.tint,
            modifier = Modifier.size(24.dp),
        )
    } else {
        Icon(
            imageVector = MeshtasticIcons.Warning,
            contentDescription = stringResource(Res.string.discovery_scan_incomplete),
            tint = Ios.colors.systemRed,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun DeleteConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    MeshtasticDialog(
        titleRes = Res.string.discovery_delete_session,
        messageRes = Res.string.discovery_delete_session_confirm,
        confirmTextRes = Res.string.delete,
        confirmDestructive = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Suppress("MagicNumber")
internal fun formatTimestamp(epochMillis: Long): String = DateFormatter.formatDateTimeShort(epochMillis)
