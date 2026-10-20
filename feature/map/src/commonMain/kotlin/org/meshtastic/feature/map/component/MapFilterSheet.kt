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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.map_filter_all_roles
import org.meshtastic.core.resources.map_filter_display_title
import org.meshtastic.core.resources.map_filter_roles_title
import org.meshtastic.core.resources.map_filter_show_ignored
import org.meshtastic.core.resources.node_filter_include_unknown
import org.meshtastic.core.resources.node_filter_only_direct
import org.meshtastic.core.resources.pangea_encrypted_only
import org.meshtastic.core.resources.pangea_favorites
import org.meshtastic.core.resources.pangea_map_filters
import org.meshtastic.core.resources.pangea_online
import org.meshtastic.core.resources.pangea_signed
import org.meshtastic.core.resources.pangea_via_mqtt
import org.meshtastic.core.resources.pangea_waypoints
import org.meshtastic.core.resources.show_precision_circle
import org.meshtastic.core.ui.component.label
import org.meshtastic.core.ui.icon.Lock
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.role
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosAccessory
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.ios.IosSheet
import org.meshtastic.feature.map.BaseMapViewModel
import org.meshtastic.proto.Config

/** The tag the row for one role carries, so a test can pick out a single role. */
fun roleFilterChipTestTag(role: Config.DeviceConfig.Role): String = "role-filter-${role.name}"

/**
 * The main map's filters, as the iPhone app's "Map Filters" sheet: switches with their symbols, then what the map
 * draws, then a checked row per device role.
 *
 * State in, actions out: neither engine keeps its own copy, and the Google flavour mounts this same composable.
 */
@Composable
fun MapFilterSheet(
    onDismissRequest: () -> Unit,
    filterState: BaseMapViewModel.MapFilterState,
    actions: MapFilterActions,
) {
    IosSheet(
        title = stringResource(Res.string.pangea_map_filters),
        onDismissRequest = onDismissRequest,
        grouped = true,
        grabber = true,
    ) {
        MapFilterSheetContent(filterState = filterState, actions = actions)
    }
}

/** The sheet's body, separate from the sheet itself so it can be tested without a window manager to host a modal. */
@Composable
internal fun MapFilterSheetContent(filterState: BaseMapViewModel.MapFilterState, actions: MapFilterActions) {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        NodeFilterToggles(filterState = filterState, actions = actions)

        IosSection(header = stringResource(Res.string.map_filter_display_title)) {
            FilterToggle(
                label = stringResource(Res.string.pangea_waypoints),
                icon = IosIcons.Signpost,
                checked = filterState.showWaypoints,
                onToggle = actions.onToggleShowWaypoints,
            )
            FilterToggle(
                label = stringResource(Res.string.show_precision_circle),
                icon = IosIcons.Target,
                checked = filterState.showPrecisionCircle,
                onToggle = actions.onToggleShowPrecisionCircle,
            )
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                LastHeardSlider(selected = filterState.lastHeardFilter, onSelect = actions.onSelectLastHeard)
            }
        }

        IosSection(header = stringResource(Res.string.map_filter_roles_title)) {
            RoleFilterRows(
                excluded = filterState.excludedRoles,
                onToggle = actions.onToggleRoleExcluded,
                onClear = actions.onClearExcludedRoles,
            )
        }
    }
}

/**
 * The node filters, in the iPhone app's order and words where it has them (MQTT, online, signed, encrypted,
 * favourites), then Android's own (direct, ignored, unknown).
 */
@Composable
private fun NodeFilterToggles(filterState: BaseMapViewModel.MapFilterState, actions: MapFilterActions) = IosSection {
    // iOS asks which nodes to include, so "Via Mqtt" on is Android's "Exclude MQTT" off.
    FilterToggle(
        label = stringResource(Res.string.pangea_via_mqtt),
        icon = IosIcons.DotRadiowavesUpForward,
        checked = !filterState.excludeMqtt,
        onToggle = actions.onToggleExcludeMqtt,
    )
    FilterToggle(
        label = stringResource(Res.string.pangea_online),
        icon = IosIcons.CheckCircleFill,
        checked = filterState.onlyOnline,
        onToggle = actions.onToggleOnlyOnline,
    )
    FilterToggle(
        label = stringResource(Res.string.pangea_signed),
        icon = IosIcons.CheckSeal,
        checked = filterState.onlySigned,
        onToggle = actions.onToggleOnlySigned,
    )
    FilterToggle(
        label = stringResource(Res.string.pangea_encrypted_only),
        icon = MeshtasticIcons.Lock,
        checked = filterState.onlyEncrypted,
        onToggle = actions.onToggleOnlyEncrypted,
    )
    FilterToggle(
        label = stringResource(Res.string.pangea_favorites),
        icon = IosIcons.StarFill,
        checked = filterState.onlyFavorites,
        onToggle = actions.onToggleOnlyFavorites,
    )
    FilterToggle(
        label = stringResource(Res.string.node_filter_only_direct),
        icon = IosIcons.Antenna,
        checked = filterState.onlyDirect,
        onToggle = actions.onToggleOnlyDirect,
    )
    // The list's `node_filter_show_ignored` reads "Only show ignored Nodes", which is what the list does and the
    // opposite of what this does.
    FilterToggle(
        label = stringResource(Res.string.map_filter_show_ignored),
        icon = IosIcons.MinusCircle,
        checked = filterState.showIgnored,
        onToggle = actions.onToggleShowIgnored,
    )
    FilterToggle(
        label = stringResource(Res.string.node_filter_include_unknown),
        icon = IosIcons.QuestionCircle,
        checked = filterState.includeUnknown,
        onToggle = actions.onToggleIncludeUnknown,
    )
}

/**
 * "All", then a row per device role, checked while that role is shown.
 *
 * Checked means shown, which is the way round a user reads the list. The state behind it is the complement, a set of
 * *excluded* roles, so that a role introduced by later firmware appears instead of silently vanishing.
 */
@Composable
private fun RoleFilterRows(
    excluded: Set<Config.DeviceConfig.Role>,
    onToggle: (Config.DeviceConfig.Role) -> Unit,
    onClear: () -> Unit,
) {
    IosRow(
        title = stringResource(Res.string.map_filter_all_roles),
        accessory = if (excluded.isEmpty()) IosAccessory.Checkmark else IosAccessory.None,
        modifier = Modifier.semantics { selected = excluded.isEmpty() },
        onClick = onClear,
    )
    // Deprecated slots are still values on the wire, so a node can report one and has to be filterable.
    @Suppress("DEPRECATION")
    Config.DeviceConfig.Role.entries.forEach { role ->
        val shown = role !in excluded
        IosRow(
            title = stringResource(role.label),
            icon = MeshtasticIcons.role(role),
            accessory = if (shown) IosAccessory.Checkmark else IosAccessory.None,
            modifier = Modifier.testTag(roleFilterChipTestTag(role)).semantics { selected = shown },
            onClick = { onToggle(role) },
        )
    }
}

@Composable
private fun FilterToggle(label: String, icon: ImageVector, checked: Boolean, onToggle: () -> Unit) {
    IosRow(
        title = label,
        icon = icon,
        iconTint = Ios.colors.tint,
        accessory = IosAccessory.Switch(checked = checked, onCheckedChange = { onToggle() }),
    )
}
