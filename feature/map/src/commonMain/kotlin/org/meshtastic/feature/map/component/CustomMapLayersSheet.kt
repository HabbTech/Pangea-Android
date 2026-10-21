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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.add_layer
import org.meshtastic.core.resources.add_network_layer
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.hide_layer
import org.meshtastic.core.resources.layer_subtitle
import org.meshtastic.core.resources.layer_type_coverage
import org.meshtastic.core.resources.layer_type_geojson
import org.meshtastic.core.resources.layer_type_kml
import org.meshtastic.core.resources.layer_type_network
import org.meshtastic.core.resources.manage_map_layers
import org.meshtastic.core.resources.map_layer_formats
import org.meshtastic.core.resources.name
import org.meshtastic.core.resources.name_cannot_be_empty
import org.meshtastic.core.resources.network_layer_url_hint
import org.meshtastic.core.resources.no_map_layers_loaded
import org.meshtastic.core.resources.refresh
import org.meshtastic.core.resources.remove_layer
import org.meshtastic.core.resources.save
import org.meshtastic.core.resources.show_layer
import org.meshtastic.core.resources.url
import org.meshtastic.core.resources.url_cannot_be_empty
import org.meshtastic.core.resources.url_must_be_http
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.icon.Layers
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Visibility
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosAccessory
import org.meshtastic.core.ui.ios.IosActivityIndicator
import org.meshtastic.core.ui.ios.IosButton
import org.meshtastic.core.ui.ios.IosButtonStyle
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.ios.IosTextField
import org.meshtastic.feature.map.layers.LayerType
import org.meshtastic.feature.map.layers.MapLayerItem
import org.meshtastic.feature.map.layers.isValidNetworkLayerUrl
import org.meshtastic.feature.map.layers.opacityOf

@Suppress("LongMethod", "ParameterNaming") // onAddLayerClicked is the established callback name used by both flavors
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CustomMapLayersSheet(
    mapLayers: List<MapLayerItem>,
    onToggleVisibility: (String) -> Unit,
    onRemoveLayer: (String) -> Unit,
    onAddLayerClicked: () -> Unit,
    onRefreshLayer: (String) -> Unit,
    onAddNetworkLayer: (String, String) -> Unit,
    opacity: Map<String, Float>,
    onOpacityChange: (String, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAddNetworkLayerDialog by remember { mutableStateOf(false) }
    // An iOS section: the title as its header, the layers as rows, the formats as its footer, then the add buttons.
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            IosSection(
                header = stringResource(Res.string.manage_map_layers),
                footer = stringResource(Res.string.map_layer_formats),
            ) {
                if (mapLayers.isEmpty()) {
                    IosRow(
                        title = stringResource(Res.string.no_map_layers_loaded),
                        titleColor = Ios.colors.secondaryLabel,
                    )
                } else {
                    mapLayers.forEach { layer ->
                        MapLayerRow(
                            layer = layer,
                            onToggleVisibility = onToggleVisibility,
                            onRemoveLayer = onRemoveLayer,
                            onRefreshLayer = onRefreshLayer,
                            opacity = opacity,
                            onOpacityChange = onOpacityChange,
                        )
                    }
                }
            }
        }
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IosButton(
                    text = stringResource(Res.string.add_layer),
                    onClick = onAddLayerClicked,
                    style = IosButtonStyle.Tinted,
                    large = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                IosButton(
                    text = stringResource(Res.string.add_network_layer),
                    onClick = { showAddNetworkLayerDialog = true },
                    style = IosButtonStyle.Tinted,
                    large = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showAddNetworkLayerDialog) {
        AddNetworkLayerDialog(
            onDismiss = { showAddNetworkLayerDialog = false },
            onConfirm = { name, url ->
                onAddNetworkLayer(name, url)
                showAddNetworkLayerDialog = false
            },
        )
    }
}

/**
 * One row of the layers sheet. The leading icon and subtitle exist so a stack of saved coverage estimates is
 * distinguishable from each other and from imported KML/GeoJSON files — they otherwise share a default name.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MapLayerRow(
    layer: MapLayerItem,
    onToggleVisibility: (String) -> Unit,
    onRemoveLayer: (String) -> Unit,
    onRefreshLayer: (String) -> Unit,
    opacity: Map<String, Float>,
    onOpacityChange: (String, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val typeLabel =
        stringResource(
            when {
                layer.isNetwork -> Res.string.layer_type_network
                layer.layerType == LayerType.COVERAGE -> Res.string.layer_type_coverage
                layer.layerType == LayerType.KML -> Res.string.layer_type_kml
                else -> Res.string.layer_type_geojson
            },
        )
    Column(modifier = modifier) {
        IosRow(
            title = layer.name,
            subtitle =
            layer.createdAt?.let {
                stringResource(Res.string.layer_subtitle, typeLabel, DateFormatter.formatDateTimeShort(it))
            } ?: typeLabel,
            icon = if (layer.layerType == LayerType.COVERAGE) IosIcons.AntennaTwoTone else IosIcons.Layers,
            accessory =
            IosAccessory.Custom {
                MapLayerActions(
                    layer = layer,
                    onToggleVisibility = onToggleVisibility,
                    onRemoveLayer = onRemoveLayer,
                    onRefreshLayer = onRefreshLayer,
                )
            },
        )
        // Keyed by URI, not id: a file-backed layer is handed a fresh random id on every load, so an id key would
        // lose the setting at the next start. hiddenLayerUrls keys visibility the same way.
        val opacityKey = layer.uri
        if (layer.isVisible && opacityKey != null) {
            LayerOpacitySlider(
                layerKey = opacityKey,
                opacity = opacity.opacityOf(opacityKey),
                onOpacityChange = onOpacityChange,
            )
        }
    }
}

/** Per-row refresh (network layers only), visibility toggle, and delete controls. */
@Composable
private fun MapLayerActions(
    layer: MapLayerItem,
    onToggleVisibility: (String) -> Unit,
    onRemoveLayer: (String) -> Unit,
    onRefreshLayer: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (layer.isNetwork) {
            if (layer.isRefreshing) {
                IosActivityIndicator(modifier = Modifier.size(24.dp).padding(4.dp), color = Ios.colors.systemGray)
            } else {
                IconButton(onClick = { onRefreshLayer(layer.id) }) {
                    Icon(
                        imageVector = IosIcons.ArrowsCirclepath,
                        contentDescription = stringResource(Res.string.refresh),
                        tint = Ios.colors.tint,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        IconToggleButton(checked = layer.isVisible, onCheckedChange = { onToggleVisibility(layer.id) }) {
            Icon(
                imageVector = if (layer.isVisible) MeshtasticIcons.Visibility else IosIcons.EyeSlash,
                contentDescription =
                stringResource(if (layer.isVisible) Res.string.hide_layer else Res.string.show_layer),
                tint = Ios.colors.tint,
                modifier = Modifier.size(22.dp),
            )
        }
        IconButton(onClick = { onRemoveLayer(layer.id) }) {
            Icon(
                imageVector = IosIcons.Trash,
                contentDescription = stringResource(Res.string.remove_layer),
                tint = Ios.colors.systemRed,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Suppress("ModifierMissing") // wraps MeshtasticDialog, which owns its own layout; no meaningful modifier slot
@Composable
fun AddNetworkLayerDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var url by rememberSaveable { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var urlError by remember { mutableStateOf<String?>(null) }

    val emptyNameError = stringResource(Res.string.name_cannot_be_empty)
    val emptyUrlError = stringResource(Res.string.url_cannot_be_empty)
    val invalidUrlError = stringResource(Res.string.url_must_be_http)

    // Validated here, not just in the store: the store's error return is dropped by two of its three callers,
    // so this dialog is the one place the user can be told. Same rules as [isValidNetworkLayerUrl].
    fun validateAndConfirm() {
        nameError = emptyNameError.takeIf { name.isBlank() }
        urlError =
            when {
                url.isBlank() -> emptyUrlError
                !isValidNetworkLayerUrl(url.trim()) -> invalidUrlError
                else -> null
            }
        if (nameError == null && urlError == null) onConfirm(name.trim(), url.trim())
    }

    MeshtasticDialog(
        onDismiss = onDismiss,
        title = stringResource(Res.string.add_network_layer),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                IosTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    placeholder = stringResource(Res.string.name),
                )
                nameError?.let { Text(it, style = Ios.type.footnote, color = Ios.colors.systemRed) }
                IosTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        urlError = null
                    },
                    placeholder = stringResource(Res.string.network_layer_url_hint),
                )
                urlError?.let { Text(it, style = Ios.type.footnote, color = Ios.colors.systemRed) }
            }
        },
        onConfirm = ::validateAndConfirm,
        confirmTextRes = Res.string.save,
        dismissTextRes = Res.string.cancel,
    )
}
