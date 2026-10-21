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
package org.meshtastic.feature.node.list

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.NodeListDensity
import org.meshtastic.core.model.excludes
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.channel_invalid
import org.meshtastic.core.resources.hop_histogram_title
import org.meshtastic.core.resources.node_list_help_title
import org.meshtastic.core.resources.node_sort_button
import org.meshtastic.core.resources.nodes
import org.meshtastic.core.resources.nodes_empty_disconnected_hint
import org.meshtastic.core.resources.nodes_empty_disconnected_title
import org.meshtastic.core.resources.nodes_empty_searching_hint
import org.meshtastic.core.resources.nodes_empty_searching_title
import org.meshtastic.core.resources.nodes_unheard_banner
import org.meshtastic.core.resources.nodes_unheard_banner_one
import org.meshtastic.core.resources.nodes_unheard_keep
import org.meshtastic.core.resources.nodes_unheard_remove
import org.meshtastic.core.resources.pangea_nodes_count
import org.meshtastic.core.resources.set_up_connection
import org.meshtastic.core.ui.component.IosNodeRow
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.component.MeshtasticImportFAB
import org.meshtastic.core.ui.component.NodeItemCompact
import org.meshtastic.core.ui.component.ScrollToTopEvent
import org.meshtastic.core.ui.component.SharedContactDialog
import org.meshtastic.core.ui.component.smartScrollToTop
import org.meshtastic.core.ui.icon.BarChart
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.NoDevice
import org.meshtastic.core.ui.icon.Nodes
import org.meshtastic.core.ui.icon.SignalOff
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosButton
import org.meshtastic.core.ui.ios.IosButtonStyle
import org.meshtastic.core.ui.ios.IosGlassIconButton
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.util.parseDeepLinkOrInvalid
import org.meshtastic.feature.node.component.LocalNodeContextMenu
import org.meshtastic.feature.node.component.NodeContextMenu
import org.meshtastic.feature.node.component.NodeCountSummary
import org.meshtastic.feature.node.component.NodeFilterSearchBar
import org.meshtastic.feature.node.component.NodeFilterToggles
import org.meshtastic.feature.node.component.NodeHopHistogramSheet
import org.meshtastic.feature.node.component.NodeListHelp
import org.meshtastic.feature.node.component.NodeSortButton
import org.meshtastic.proto.ExcludedModules

/**
 * design#115: status message editing is offered on the connected local node only, and only where the firmware has the
 * module — absent, never disabled, everywhere else.
 */
internal fun canEditStatusMessage(node: Node, ourNode: Node?, connectionState: ConnectionState): Boolean =
    node.num == ourNode?.num &&
        connectionState == ConnectionState.Connected &&
        node.capabilities.supportsStatusMessage &&
        !node.metadata.excludes(ExcludedModules.STATUSMESSAGE_CONFIG)

@Suppress("LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NodeListScreen(
    navigateToNodeDetails: (Int) -> Unit,
    viewModel: NodeListViewModel,
    modifier: Modifier = Modifier,
    navigateToMessages: (String) -> Unit = {},
    scrollToTopEvents: Flow<ScrollToTopEvent>? = null,
    activeNodeId: Int? = null,
    onHandleDeepLink: (org.meshtastic.core.common.util.CommonUri, onInvalid: () -> Unit) -> Unit = { _, _ -> },
    onNavigateToConnections: () -> Unit = {},
    onEditStatusMessage: () -> Unit = {},
) {
    val showToast = org.meshtastic.core.ui.util.rememberShowToastResource()
    val scope = rememberCoroutineScope()
    val state by viewModel.nodesUiState.collectAsStateWithLifecycle()

    // Dedup by num: it's the LazyColumn key (must be unique) but the upstream flow can momentarily emit
    // two entries with the same num (a num=0 unknown-node placeholder, or a brief double-emission during
    // an active-DB switch). Dedup once here so the count in the app bar and the rendered rows agree.
    // Composite "num_index" keys would also fix the crash but break animateItem() reorder animations.
    val allNodes by viewModel.nodeList.collectAsStateWithLifecycle()
    val nodes = remember(allNodes) { allNodes.distinctBy { it.num } }
    val ourNode by viewModel.ourNodeInfo.collectAsStateWithLifecycle()
    val onlineNodeCount by viewModel.onlineNodeCount.collectAsStateWithLifecycle(0)
    val totalNodeCount by viewModel.totalNodeCount.collectAsStateWithLifecycle(0)
    val unfilteredNodes by viewModel.unfilteredNodeList.collectAsStateWithLifecycle()
    // Favorites and our own node are never offered for removal: a favorite is an explicit keep, and the connected
    // radio is not something the node list may delete. ourNode and unfilteredNodes come from independent flows, so
    // the list can already contain the local node while ourNode is still null. Offer nothing until it is known,
    // rather than risk removing the user's own node from the radio.
    val unheardNodes = remember(unfilteredNodes, ourNode) { selectRemovableUnheardNodes(unfilteredNodes, ourNode?.num) }
    val deviceImageUrls by viewModel.deviceImageUrls.collectAsStateWithLifecycle()
    val ignoredNodeCount = unfilteredNodes.count { it.isIgnored }

    // No cache window here: this pane renders inside ThreePaneScaffold's LookaheadScope, where a
    // prefetched item can reach the main placement pass before the lookahead pass has measured it.
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(scrollToTopEvents) {
        scrollToTopEvents?.collectLatest { event ->
            if (event == ScrollToTopEvent.NodesTabPressed) {
                listState.smartScrollToTop(coroutineScope)
            }
        }
    }

    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val deviceType by viewModel.deviceType.collectAsStateWithLifecycle()

    val density by viewModel.nodeListDensity.collectAsStateWithLifecycle()
    val showPower by viewModel.shouldShowPower.collectAsStateWithLifecycle()
    val showLastHeard by viewModel.shouldShowLastHeard.collectAsStateWithLifecycle()
    val lastHeardIsRelative by viewModel.lastHeardIsRelative.collectAsStateWithLifecycle()
    val showLocation by viewModel.shouldShowLocation.collectAsStateWithLifecycle()
    val showHops by viewModel.shouldShowHops.collectAsStateWithLifecycle()
    val showSignal by viewModel.shouldShowSignal.collectAsStateWithLifecycle()
    val showChannel by viewModel.shouldShowChannel.collectAsStateWithLifecycle()
    val showRole by viewModel.shouldShowRole.collectAsStateWithLifecycle()
    val showTelemetry by viewModel.shouldShowTelemetry.collectAsStateWithLifecycle()

    val isScrollInProgress by remember {
        derivedStateOf { listState.isScrollInProgress && (listState.canScrollForward || listState.canScrollBackward) }
    }

    var showHelpSheet by remember { mutableStateOf(false) }
    if (showHelpSheet) {
        NodeListHelp(onDismiss = { showHelpSheet = false })
    }

    var showHopHistogram by remember { mutableStateOf(false) }
    if (showHopHistogram) {
        NodeHopHistogramSheet(nodes = unfilteredNodes, onDismiss = { showHopHistogram = false })
    }

    var showShareContact by remember { mutableStateOf(false) }
    if (showShareContact) {
        SharedContactDialog(contact = ourNode, onDismiss = { showShareContact = false }, isOwnContact = true)
    }

    // One row renderer, used by the list itself and by the search bar's expanded results, so the two can never drift.
    // It stays a LazyItemScope lambda rather than a composable of its own because animateItem() needs that receiver.
    val nodeRow: @Composable LazyItemScope.(Node) -> Unit = { node ->
        var expanded by remember { mutableStateOf(false) }

        Box(
            modifier =
            if (density == NodeListDensity.COMPLETE) {
                Modifier
            } else {
                Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            },
        ) {
            val isThisNode = node.num == ourNode?.num
            val canEditStatus = canEditStatusMessage(node, ourNode, connectionState)
            // Our own node only earns a long press while it has something to offer, or it opens an empty menu.
            val longClick =
                if (!isThisNode || canEditStatus) {
                    { expanded = true }
                } else {
                    null
                }

            val isActive = remember(activeNodeId, node.num) { activeNodeId == node.num }

            when (density) {
                // Pangea: the iPhone app's node row (the compact layout stays an Android option).
                NodeListDensity.COMPLETE ->
                    IosNodeRow(
                        modifier = Modifier.animateItem(),
                        thisNode = ourNode,
                        thatNode = node,
                        distanceUnits = state.distanceUnits,
                        connectionState = connectionState,
                        onClick = { navigateToNodeDetails(node.num) },
                        onLongClick = longClick,
                    )

                NodeListDensity.COMPACT ->
                    NodeItemCompact(
                        modifier = Modifier.animateItem(),
                        thisNode = ourNode,
                        thatNode = node,
                        distanceUnits = state.distanceUnits,
                        onClick = { navigateToNodeDetails(node.num) },
                        onLongClick = longClick,
                        isActive = isActive,
                        showPower = showPower,
                        showLastHeard = showLastHeard,
                        lastHeardIsRelative = lastHeardIsRelative,
                        showLocation = showLocation,
                        showHops = showHops,
                        showSignal = showSignal,
                        showChannel = showChannel,
                        showRole = showRole,
                        showTelemetry = showTelemetry,
                        tempInFahrenheit = state.tempInFahrenheit,
                        deviceImageUrl = deviceImageUrls[node.user.hw_model.value],
                    )
            }
            if (canEditStatus) {
                LocalNodeContextMenu(
                    expanded = expanded,
                    onUpdateStatus = onEditStatusMessage,
                    onDismiss = { expanded = false },
                )
            } else if (!isThisNode) {
                NodeContextMenu(
                    expanded = expanded,
                    node = node,
                    onFavorite = { viewModel.favoriteNode(node) },
                    onMute = { viewModel.muteNode(node) },
                    onMessage = {
                        val route = viewModel.getDirectMessageRoute(node)
                        navigateToMessages(route)
                    },
                    onTraceRoute = { viewModel.traceRoute(node) },
                    onIgnore = { viewModel.ignoreNode(node) },
                    onRemove = { viewModel.removeNode(node) },
                    onDismiss = { expanded = false },
                )
            }
        }
    }

    val filterPrefs = viewModel.nodeFilterPreferences
    val filterToggles =
        NodeFilterToggles(
            includeUnknown = state.filter.includeUnknown,
            onToggleIncludeUnknown = filterPrefs::toggleIncludeUnknown,
            excludeInfrastructure = state.filter.excludeInfrastructure,
            onToggleExcludeInfrastructure = filterPrefs::toggleExcludeInfrastructure,
            onlyOnline = state.filter.onlyOnline,
            onToggleOnlyOnline = filterPrefs::toggleOnlyOnline,
            onlyDirect = state.filter.onlyDirect,
            onToggleOnlyDirect = filterPrefs::toggleOnlyDirect,
            showIgnored = state.filter.showIgnored,
            onToggleShowIgnored = filterPrefs::toggleShowIgnored,
            ignoredNodeCount = ignoredNodeCount,
            excludeUnheard = state.filter.excludeUnheard,
            onToggleExcludeUnheard = filterPrefs::toggleExcludeUnheard,
            excludeMqtt = state.filter.excludeMqtt,
            onToggleExcludeMqtt = filterPrefs::toggleExcludeMqtt,
            onlySigned = state.filter.onlySigned,
            onToggleOnlySigned = filterPrefs::toggleOnlySigned,
            onlyEncrypted = state.filter.onlyEncrypted,
            onToggleOnlyEncrypted = filterPrefs::toggleOnlyEncrypted,
        )

    Scaffold(
        modifier = modifier,
        containerColor = Ios.colors.background,
        topBar = {
            MainAppBar(
                // iOS puts the count in the title: "Nodes (144)".
                title = stringResource(Res.string.pangea_nodes_count, nodes.size),
                // Node counts deliberately live in the list header (NodeCountSummary), not here: the app bar's
                // title/subtitle slot competes for width with the node chip + action icons and is clipped to a
                // single line, which truncated the counts (issue #6268).
                ourNode = ourNode,
                showNodeChip = ourNode != null && connectionState is ConnectionState.Connected,
                canNavigateUp = false,
                onNavigateUp = {},
                background = Ios.colors.background,
                // iOS's bar holds only the radio chip: the legend and the filters are floating buttons at the foot.
                actions = {},
                onClickChip = { navigateToNodeDetails(it.num) },
            )
        },
        floatingActionButton = {
            val shareCapable = ourNode?.capabilities?.supportsQrCodeSharing ?: false
            // Android's import and share button, which iOS doesn't have: above the floating filter button.
            MeshtasticImportFAB(
                modifier =
                Modifier.padding(bottom = 68.dp)
                    .animateFloatingActionButton(
                        visible =
                        !isScrollInProgress && connectionState == ConnectionState.Connected && shareCapable,
                        alignment = androidx.compose.ui.Alignment.BottomEnd,
                    ),
                onImport = { uriString ->
                    val onInvalid: () -> Unit = { scope.launch { showToast(Res.string.channel_invalid) } }
                    parseDeepLinkOrInvalid(uriString, onHandleDeepLink, onInvalid)
                },
                onShareContact = { showShareContact = true },
                isContactContext = true,
            )
        },
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(contentPadding).focusable()) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                stickyHeader {
                    val animatedAlpha by
                        animateFloatAsState(targetValue = if (!isScrollInProgress) 1.0f else 0f, label = "alpha")
                    Column(
                        modifier =
                        Modifier.fillMaxWidth()
                            .graphicsLayer(alpha = animatedAlpha)
                            .background(Ios.colors.background)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        UnheardNodesBanner(
                            unheardNodes = unheardNodes,
                            onRemoveAll = { unheardNodes.forEach(viewModel::removeNode) },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        )
                        NodeFilterSearchBar(
                            filterText = state.filter.filterText,
                            onTextChange = { viewModel.nodeFilterText = it },
                            currentSortOption = state.sort,
                            onSortSelect = viewModel::setSortOption,
                            modifier = Modifier.fillMaxWidth(),
                            showSortButton = false,
                            searchResults = {
                                // The full-screen expanded bar hides the sticky header's counts.
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    item {
                                        NodeCountSummary(
                                            onlineCount = onlineNodeCount,
                                            shownCount = nodes.size,
                                            totalCount = totalNodeCount,
                                            modifier =
                                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                        )
                                    }
                                    items(nodes, key = { it.num }, itemContent = nodeRow)
                                }
                            },
                            toggles = filterToggles,
                        )
                    }
                }

                items(nodes, key = { it.num }, itemContent = nodeRow)
                if (nodes.isEmpty() && !state.filter.isActive) {
                    item {
                        NodeListEmptyState(
                            connectionState = connectionState,
                            onNavigateToConnections = onNavigateToConnections,
                            modifier = Modifier.fillParentMaxSize(),
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(88.dp)) }
            }
            // iOS's two floating glass buttons above the tab bar: the legend at the left, sort and filters at the
            // right. Android's hop histogram rides at the top of the filter menu.
            IosGlassIconButton(
                icon = IosIcons.QuestionCircle,
                contentDescription = stringResource(Res.string.node_list_help_title),
                onClick = { showHelpSheet = true },
                tint = Ios.colors.tint,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 22.dp),
            )
            NodeSortButton(
                currentSortOption = state.sort,
                onSortSelect = viewModel::setSortOption,
                toggles = filterToggles,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 22.dp),
                extraItems = { dismiss ->
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.hop_histogram_title)) },
                        leadingIcon = { Icon(MeshtasticIcons.BarChart, contentDescription = null) },
                        onClick = {
                            dismiss()
                            showHopHistogram = true
                        },
                    )
                },
                button = { open ->
                    IosGlassIconButton(
                        icon = IosIcons.FilterCircle,
                        contentDescription = stringResource(Res.string.node_sort_button),
                        onClick = open,
                        tint = Ios.colors.tint,
                    )
                },
            )
        }
    }
}

/**
 * Inline empty state for the Nodes screen. Material 3 inline empty-state guidance: a small muted icon, short title, and
 * supporting hint. When the user has no device selected (or is otherwise disconnected), an action button routes them to
 * the Connections tab; when connected with no nodes yet we show a passive "searching" state.
 */
@Composable
private fun NodeListEmptyState(
    connectionState: ConnectionState,
    onNavigateToConnections: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isConnected = connectionState == ConnectionState.Connected
    val (icon: ImageVector, title: String, hint: String) =
        if (isConnected) {
            Triple(
                MeshtasticIcons.Nodes,
                stringResource(Res.string.nodes_empty_searching_title),
                stringResource(Res.string.nodes_empty_searching_hint),
            )
        } else {
            Triple(
                MeshtasticIcons.NoDevice,
                stringResource(Res.string.nodes_empty_disconnected_title),
                stringResource(Res.string.nodes_empty_disconnected_hint),
            )
        }
    Column(
        modifier = modifier.padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (!isConnected) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onNavigateToConnections) { Text(stringResource(Res.string.set_up_connection)) }
        }
    }
}

/**
 * Offers to remove the nodes the radio has not heard since its LoRa settings changed.
 *
 * Shown as an offer, never an automatic removal: the app's node DB is deliberately a superset of the radio's, and a
 * node may simply be out of range rather than on another channel — it comes back on its own when next heard.
 *
 * Dismissal is per-composition rather than saved, and re-arms whenever the count rises, so a later config change offers
 * again instead of staying silent.
 */
@Composable
private fun UnheardNodesBanner(unheardNodes: List<Node>, onRemoveAll: () -> Unit, modifier: Modifier = Modifier) {
    var dismissed by remember { mutableStateOf(false) }
    var previousCount by remember { mutableIntStateOf(0) }
    val count = unheardNodes.size

    // Re-arm on any rise, not only above a historical maximum: dismissing at 3, dropping to 1 and rising to 2 is
    // still a new config change worth offering for.
    LaunchedEffect(count) {
        if (count > previousCount) dismissed = false
        previousCount = count
    }

    if (count == 0 || dismissed) return

    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(Ios.colors.secondaryFill).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                imageVector = MeshtasticIcons.SignalOff,
                contentDescription = null,
                tint = Ios.colors.systemOrange,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text =
                if (count == 1) {
                    stringResource(Res.string.nodes_unheard_banner_one)
                } else {
                    stringResource(Res.string.nodes_unheard_banner, count)
                },
                style = Ios.type.subheadline,
                color = Ios.colors.label,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.End)) {
            IosButton(
                text = stringResource(Res.string.nodes_unheard_keep),
                onClick = { dismissed = true },
                style = IosButtonStyle.Gray,
                textStyle = Ios.type.subheadline,
            )
            IosButton(
                text = stringResource(Res.string.nodes_unheard_remove),
                onClick = {
                    onRemoveAll()
                    dismissed = true
                },
                style = IosButtonStyle.Destructive,
                textStyle = Ios.type.subheadline,
            )
        }
    }
}
