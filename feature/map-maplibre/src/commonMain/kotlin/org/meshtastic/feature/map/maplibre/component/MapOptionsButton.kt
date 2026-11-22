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
package org.meshtastic.feature.map.maplibre.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.pangea_map_options
import org.meshtastic.core.resources.pangea_map_overlays
import org.meshtastic.core.resources.pangea_map_style
import org.meshtastic.core.resources.pangea_offline_maps
import org.meshtastic.core.ui.ios.IosAccessory
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.ios.IosSheet
import org.meshtastic.feature.map.component.LayerOpacitySlider
import org.meshtastic.feature.map.component.MapButton
import org.meshtastic.feature.map.layers.LayerOpacityStore
import org.meshtastic.feature.map.layers.opacityOf
import org.meshtastic.feature.map.maplibre.style.Basemap
import org.meshtastic.feature.map.maplibre.style.MapOverlay
import org.meshtastic.feature.map.maplibre.style.MapOverlays
import org.meshtastic.feature.map.tiles.MapTileCatalogue

/**
 * The map button and the "Map Options" sheet it opens, as on the iPhone app: the map style, the overlays drawn over it,
 * and offline maps, in one place. Android's own extras ride along: custom tile sources under the styles, and the
 * imported-layer manager at the end.
 *
 * Offline downloads are shown only where [offlineMapsSupported]. MapLibre's offline API compiles on every target, and
 * on desktop it will happily create a pack and report it — but the pack never downloads a tile, so the section would
 * offer a control that quietly does nothing. Better to not offer it.
 */
@Suppress("LongParameterList")
@Composable
internal fun MapOptionsButton(
    selection: BasemapSelection,
    overlays: List<MapOverlay>,
    onOverlaysChange: (List<MapOverlay>) -> Unit,
    offlineTarget: OfflineMapTarget,
    offlineMapsSupported: Boolean,
    basemapExtra: @Composable () -> Unit,
    layersExtra: @Composable () -> Unit,
) {
    var sheetVisible by remember { mutableStateOf(false) }

    MapButton(
        icon = IosIcons.MapOutline,
        contentDescription = stringResource(Res.string.pangea_map_options),
        onClick = { sheetVisible = true },
    )

    if (sheetVisible) {
        IosSheet(
            title = stringResource(Res.string.pangea_map_options),
            onDismissRequest = { sheetVisible = false },
            grouped = true,
            grabber = true,
        ) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                MapStyleSection(selection, basemapExtra)
                MapOverlaysSection(overlays, onOverlaysChange)
                OfflineSection(
                    offlineTarget = offlineTarget,
                    offlineMapsSupported = offlineMapsSupported,
                    onDismiss = { sheetVisible = false },
                )
                // The imported-layer manager is a lazy list of its own (a section and its add buttons), so it gets a
                // bounded height inside this scrolling sheet.
                Box(Modifier.fillMaxWidth().heightIn(max = LAYERS_MAX_HEIGHT)) { layersExtra() }
            }
        }
    }
}

/** Every basemap, ours then the user's own, with a check on the one in use; then the custom-source editor. */
@Composable
private fun MapStyleSection(selection: BasemapSelection, basemapExtra: @Composable () -> Unit) {
    IosSection(header = stringResource(Res.string.pangea_map_style)) {
        (selection.builtIns + selection.customs).forEach { basemap: Basemap ->
            IosRow(
                title = basemap.label,
                accessory = if (basemap.id == selection.selected.id) IosAccessory.Checkmark else IosAccessory.None,
                onClick = { selection.onSelect(basemap) },
            )
        }
        basemapExtra()
    }
}

/** The raster overlays as switches, each drawing one with its opacity slider under it. */
@Composable
private fun MapOverlaysSection(overlays: List<MapOverlay>, onOverlaysChange: (List<MapOverlay>) -> Unit) {
    val opacityStore: LayerOpacityStore = koinInject()
    val opacity by opacityStore.opacity.collectAsState()
    val enabledIds = overlays.mapTo(mutableSetOf()) { it.id }
    IosSection(header = stringResource(Res.string.pangea_map_overlays)) {
        MapTileCatalogue.overlays.forEach { overlay ->
            val on = overlay.id in enabledIds
            IosRow(
                title = stringResource(overlay.label),
                icon = IosIcons.Layers,
                accessory =
                IosAccessory.Switch(
                    checked = on,
                    onCheckedChange = {
                        MapOverlays.byId(overlay.id)?.let { onOverlaysChange(overlays.toggling(it)) }
                    },
                ),
            )
            // Only for an overlay that is actually drawing: a slider under an off switch controls nothing.
            if (on) {
                LayerOpacitySlider(
                    layerKey = overlay.id,
                    opacity = opacity.opacityOf(overlay.id),
                    onOpacityChange = opacityStore::setOpacity,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

/** Offline map downloads (where they work) and offline terrain, Android's own controls on the iOS card. */
@Composable
private fun OfflineSection(offlineTarget: OfflineMapTarget, offlineMapsSupported: Boolean, onDismiss: () -> Unit) {
    IosSection(header = stringResource(Res.string.pangea_offline_maps)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            if (offlineMapsSupported) {
                OfflineMapsSection(
                    target = offlineTarget,
                    onShowRegion = { bounds ->
                        onDismiss()
                        offlineTarget.showRegion(bounds)
                    },
                )
            }
            // Not offlineMapsSupported-gated — see OfflineTerrainSection's own doc comment for why terrain downloads
            // work on both Android and Desktop even though the base map's own offline packs don't.
            OfflineTerrainSection(
                target = offlineTarget,
                onShowRegion = { bounds ->
                    onDismiss()
                    offlineTarget.showRegion(bounds)
                },
            )
        }
    }
}

private fun List<MapOverlay>.toggling(overlay: MapOverlay): List<MapOverlay> =
    if (any { it.id == overlay.id }) filterNot { it.id == overlay.id } else this + overlay

private val LAYERS_MAX_HEIGHT = 420.dp
