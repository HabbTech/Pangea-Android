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
package org.meshtastic.feature.node.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.NodeSortOption
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.node_filter_exclude_infrastructure
import org.meshtastic.core.resources.node_filter_exclude_unheard
import org.meshtastic.core.resources.node_filter_ignored
import org.meshtastic.core.resources.node_filter_include_unknown
import org.meshtastic.core.resources.node_filter_only_direct
import org.meshtastic.core.resources.node_filter_show_ignored
import org.meshtastic.core.resources.node_sort_button
import org.meshtastic.core.resources.node_sort_title
import org.meshtastic.core.resources.pangea_encrypted_only
import org.meshtastic.core.resources.pangea_find_node
import org.meshtastic.core.resources.pangea_node_filters
import org.meshtastic.core.resources.pangea_online
import org.meshtastic.core.resources.pangea_signed
import org.meshtastic.core.resources.pangea_via_mqtt
import org.meshtastic.core.ui.component.MeshtasticSearchBar
import org.meshtastic.core.ui.icon.Lock
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Sort
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosAccessory
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.ios.IosSheet

/**
 * The node list's search field: a Material 3 [MeshtasticSearchBar] whose expanded surface shows the matching nodes,
 * with the sort and filter menu as its trailing action.
 *
 * The "showing ignored nodes" banner sits under the collapsed bar rather than inside it: it reports a filter that is
 * already applied to the list below, not a search result.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodeFilterSearchBar(
    filterText: String,
    onTextChange: (String) -> Unit,
    currentSortOption: NodeSortOption,
    onSortSelect: (NodeSortOption) -> Unit,
    toggles: NodeFilterToggles,
    modifier: Modifier = Modifier,
    searchResults: @Composable ColumnScope.() -> Unit = {},
    // False where the sort and filter menu has its own button (Pangea's floating one, as on iOS).
    showSortButton: Boolean = true,
) {
    Column(modifier = modifier.background(Ios.colors.background)) {
        MeshtasticSearchBar(
            query = filterText,
            onQueryChange = onTextChange,
            placeholder = stringResource(Res.string.pangea_find_node),
            inputFieldTag = NODE_FILTER_SEARCH_BAR_INPUT_FIELD_TAG,
            trailingActions = {
                if (showSortButton) {
                    NodeSortButton(
                        currentSortOption = currentSortOption,
                        onSortSelect = onSortSelect,
                        toggles = toggles,
                    )
                }
            },
            expandedContent = searchResults,
        )
        if (toggles.showIgnored) {
            Box(
                modifier =
                Modifier.fillMaxWidth()
                    .background(Ios.colors.fieldFill)
                    .clickable { toggles.onToggleShowIgnored() }
                    .padding(vertical = 12.dp, horizontal = 24.dp),
            ) {
                Text(
                    text = stringResource(Res.string.node_filter_ignored),
                    style = Ios.type.subheadline,
                    color = Ios.colors.tint,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Tag for the collapsed node search field, so a test can target it rather than the expanded overlay's copy. */
const val NODE_FILTER_SEARCH_BAR_INPUT_FIELD_TAG = "NodeFilterSearchBarInputField"

data class NodeFilterToggles(
    val includeUnknown: Boolean,
    val onToggleIncludeUnknown: () -> Unit,
    val excludeInfrastructure: Boolean,
    val onToggleExcludeInfrastructure: () -> Unit,
    val onlyOnline: Boolean,
    val onToggleOnlyOnline: () -> Unit,
    val onlyDirect: Boolean,
    val onToggleOnlyDirect: () -> Unit,
    val showIgnored: Boolean,
    val onToggleShowIgnored: () -> Unit,
    val ignoredNodeCount: Int,
    val excludeUnheard: Boolean,
    val onToggleExcludeUnheard: () -> Unit,
    val excludeMqtt: Boolean,
    val onToggleExcludeMqtt: () -> Unit,
    val onlySigned: Boolean,
    val onToggleOnlySigned: () -> Unit,
    val onlyEncrypted: Boolean,
    val onToggleOnlyEncrypted: () -> Unit,
)

/**
 * The sort and filters, as the iPhone app's "Node Filters" sheet opened from [button] (by default the sort icon in the
 * search field): switches with their symbols in iOS's order and words where it has them, then Android's own filters,
 * then how the list is sorted. [extraItems] adds Android's own rows in a card of their own, such as Pangea's hop
 * histogram now that the bar has no room for it.
 */
@Suppress("LongMethod")
@Composable
fun NodeSortButton(
    currentSortOption: NodeSortOption,
    onSortSelect: (NodeSortOption) -> Unit,
    toggles: NodeFilterToggles,
    modifier: Modifier = Modifier,
    extraItems: (@Composable (dismiss: () -> Unit) -> Unit)? = null,
    button: @Composable (open: () -> Unit) -> Unit = { open ->
        IconButton(onClick = open) {
            Icon(
                imageVector = MeshtasticIcons.Sort,
                contentDescription = stringResource(Res.string.node_sort_button),
                modifier = Modifier.heightIn(max = 48.dp),
                tint = Ios.colors.label,
            )
        }
    },
) = Box(modifier) {
    var expanded by remember { mutableStateOf(false) }

    button { expanded = true }

    if (expanded) {
        IosSheet(
            title = stringResource(Res.string.pangea_node_filters),
            onDismissRequest = { expanded = false },
            grouped = true,
            grabber = true,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            ) {
                IosSection {
                    // iOS asks which nodes to include, so "Via Mqtt" on is Android's "Exclude MQTT" off.
                    FilterSwitch(
                        label = stringResource(Res.string.pangea_via_mqtt),
                        icon = IosIcons.DotRadiowavesUpForward,
                        checked = !toggles.excludeMqtt,
                        onToggle = toggles.onToggleExcludeMqtt,
                    )
                    FilterSwitch(
                        label = stringResource(Res.string.pangea_online),
                        icon = IosIcons.CheckCircleFill,
                        checked = toggles.onlyOnline,
                        onToggle = toggles.onToggleOnlyOnline,
                    )
                    FilterSwitch(
                        label = stringResource(Res.string.node_filter_exclude_unheard),
                        icon = IosIcons.EyeSlash,
                        checked = toggles.excludeUnheard,
                        onToggle = toggles.onToggleExcludeUnheard,
                    )
                    FilterSwitch(
                        label = stringResource(Res.string.pangea_signed),
                        icon = IosIcons.CheckSeal,
                        checked = toggles.onlySigned,
                        onToggle = toggles.onToggleOnlySigned,
                    )
                    FilterSwitch(
                        label = stringResource(Res.string.pangea_encrypted_only),
                        icon = MeshtasticIcons.Lock,
                        checked = toggles.onlyEncrypted,
                        onToggle = toggles.onToggleOnlyEncrypted,
                    )
                    FilterSwitch(
                        label = stringResource(Res.string.node_filter_show_ignored),
                        icon = IosIcons.MinusCircle,
                        checked = toggles.showIgnored,
                        onToggle = toggles.onToggleShowIgnored,
                        value = toggles.ignoredNodeCount.takeIf { it > 0 }?.toString(),
                    )
                    FilterSwitch(
                        label = stringResource(Res.string.node_filter_only_direct),
                        icon = IosIcons.Antenna,
                        checked = toggles.onlyDirect,
                        onToggle = toggles.onToggleOnlyDirect,
                    )
                    FilterSwitch(
                        label = stringResource(Res.string.node_filter_include_unknown),
                        icon = IosIcons.QuestionCircle,
                        checked = toggles.includeUnknown,
                        onToggle = toggles.onToggleIncludeUnknown,
                    )
                    FilterSwitch(
                        label = stringResource(Res.string.node_filter_exclude_infrastructure),
                        icon = IosIcons.Repeat,
                        checked = toggles.excludeInfrastructure,
                        onToggle = toggles.onToggleExcludeInfrastructure,
                    )
                }

                IosSection(header = stringResource(Res.string.node_sort_title)) {
                    NodeSortOption.entries.forEach { sort ->
                        IosRow(
                            title = stringResource(sort.stringRes),
                            accessory =
                            if (sort == currentSortOption) IosAccessory.Checkmark else IosAccessory.None,
                            onClick = { onSortSelect(sort) },
                        )
                    }
                }

                if (extraItems != null) {
                    IosSection { extraItems { expanded = false } }
                }
            }
        }
    }
}

@Composable
private fun FilterSwitch(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    onToggle: () -> Unit,
    value: String? = null,
) {
    IosRow(
        title = label,
        icon = icon,
        value = value,
        accessory = IosAccessory.Switch(checked = checked, onCheckedChange = { onToggle() }),
    )
}
