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
package org.meshtastic.feature.discovery.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.ChannelOption
import org.meshtastic.core.model.schemaLabelRes
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.discovery_preset_home_label
import org.meshtastic.core.resources.discovery_stat_selected
import org.meshtastic.core.resources.discovery_stat_unselected
import org.meshtastic.core.resources.mesh_beacon_preset_indicator
import org.meshtastic.core.resources.pangea_discovery_legend_beacon
import org.meshtastic.core.resources.pangea_discovery_legend_home
import org.meshtastic.core.resources.pangea_discovery_modem_presets
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosCardShape
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosInset
import org.meshtastic.core.ui.ios.iosPressable

/** Formats a [ChannelOption] enum name (e.g. "LONG_FAST") into a human-readable label (e.g. "Long Fast"). */
internal fun ChannelOption.displayName(): String =
    name.split("_").joinToString(" ") { word -> word.lowercase().replaceFirstChar { it.uppercase() } }

/** Deprecated modem presets that should not appear in the discovery picker. */
private val DEPRECATED_PRESETS = setOf(ChannelOption.VERY_LONG_SLOW, ChannelOption.LONG_SLOW)

private const val TILE_COLUMNS = 3
private val TILE_GAP = 8.dp
private val TILE_HEIGHT = 74.dp

/**
 * The presets to sweep, as the iPhone app lays them out: a "Modem Presets" header, then a grid of tiles named as the
 * schema names them ("Long Range - Fast"), filled with the tint when chosen. A star marks the home preset and a beacon
 * one a nearby beacon advertised; a legend under the grid says so.
 */
@Composable
fun PresetPickerCard(
    selectedPresets: Set<ChannelOption>,
    homePreset: ChannelOption,
    onTogglePreset: (ChannelOption) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    beaconPresets: Set<ChannelOption> = emptySet(),
) {
    val presets = ChannelOption.entries.filter { it !in DEPRECATED_PRESETS }
    Column(modifier.fillMaxWidth()) {
        DiscoverySectionHeader(IosIcons.Antenna, stringResource(Res.string.pangea_discovery_modem_presets))
        Column(
            Modifier.padding(horizontal = IosInset)
                .fillMaxWidth()
                .clip(IosCardShape)
                .background(Ios.colors.groupedRow)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(TILE_GAP),
        ) {
            presets.chunked(TILE_COLUMNS).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(TILE_GAP)) {
                    row.forEach { preset ->
                        PresetTile(
                            preset = preset,
                            selected = preset in selectedPresets,
                            isHome = preset == homePreset,
                            fromBeacon = preset in beaconPresets,
                            enabled = enabled,
                            onClick = { onTogglePreset(preset) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    repeat(TILE_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        LegendLine(IosIcons.StarFill, stringResource(Res.string.pangea_discovery_legend_home))
        LegendLine(IosIcons.RadiowavesLeftRight, stringResource(Res.string.pangea_discovery_legend_beacon))
    }
}

/** One preset: its badges over its name, on the tint while chosen and on grey otherwise. */
@Suppress("LongParameterList")
@Composable
private fun PresetTile(
    preset: ChannelOption,
    selected: Boolean,
    isHome: Boolean,
    fromBeacon: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Ios.colors
    val name = preset.modemPreset.schemaLabelRes()?.let { stringResource(it) } ?: preset.displayName()
    val spoken = if (isHome) stringResource(Res.string.discovery_preset_home_label, name) else name
    val beaconDesc = stringResource(Res.string.mesh_beacon_preset_indicator)
    val state =
        stringResource(if (selected) Res.string.discovery_stat_selected else Res.string.discovery_stat_unselected)
    val fill =
        if (selected) {
            c.tint
        } else if (c.isDark) {
            TileGrayDark
        } else {
            TileGray
        }
    val ink = if (selected) Color.White else c.secondaryLabel
    Column(
        modifier
            .height(TILE_HEIGHT)
            .clip(RoundedCornerShape(14.dp))
            .background(fill)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .iosPressable(enabled = enabled, role = Role.Checkbox, onClick = onClick)
            .semantics {
                contentDescription = if (fromBeacon) "$spoken, $beaconDesc" else spoken
                stateDescription = state
            }
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (isHome || fromBeacon || selected) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isHome) Badge(IosIcons.StarFill, ink)
                if (fromBeacon) Badge(IosIcons.RadiowavesLeftRight, ink)
                if (selected) Badge(IosIcons.Checkmark, ink)
            }
            Spacer(Modifier.height(4.dp))
        }
        Text(
            name,
            style = Ios.type.caption1.copy(fontWeight = FontWeight.Medium),
            color = ink,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun Badge(icon: ImageVector, tint: Color) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
}

/** A line of the legend under the grid: its symbol in grey and what it means. */
@Composable
private fun LegendLine(icon: ImageVector, text: String) {
    val c = Ios.colors
    Row(Modifier.fillMaxWidth().padding(start = 28.dp, end = 28.dp, top = 8.dp), verticalAlignment = Alignment.Top) {
        Icon(
            icon,
            contentDescription = null,
            tint = c.secondaryLabel,
            modifier = Modifier.padding(top = 2.dp).size(14.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(text, style = Ios.type.footnote, color = c.secondaryLabel)
    }
}

/** A discovery section's header as the iPhone app draws it: the symbol and the title, both in the tint. */
@Composable
internal fun DiscoverySectionHeader(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    inset: Dp = HEADER_INSET,
) {
    val c = Ios.colors
    Row(
        modifier.fillMaxWidth().padding(start = inset, end = inset, top = 24.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(30.dp)) {
            Icon(icon, contentDescription = null, tint = c.tint, modifier = Modifier.size(22.dp))
        }
        Text(title, style = Ios.type.title3.copy(fontWeight = FontWeight.SemiBold), color = c.tint)
    }
}

private const val DISABLED_ALPHA = 0.45f

/** Where a tinted header starts at the screen's edge; inside an IosSection, which is inset already, 12 dp. */
private val HEADER_INSET = 28.dp

/** An unchosen tile: iOS's systemGray5, light and dark. */
private val TileGray = Color(0xFFE3E3E8)
private val TileGrayDark = Color(0xFF3A3A3C)
