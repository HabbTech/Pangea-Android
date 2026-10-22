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
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@file:Suppress("TooManyFunctions") // small, single-purpose section composables mirroring the planner's panels

package org.meshtastic.feature.map.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.NumberFormatter
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.latitude
import org.meshtastic.core.resources.longitude
import org.meshtastic.core.resources.name
import org.meshtastic.core.resources.site_planner
import org.meshtastic.core.resources.site_planner_antenna_gain_dbi
import org.meshtastic.core.resources.site_planner_antenna_height_meters
import org.meshtastic.core.resources.site_planner_color_scale
import org.meshtastic.core.resources.site_planner_estimate
import org.meshtastic.core.resources.site_planner_frequency_mhz
import org.meshtastic.core.resources.site_planner_high_resolution
import org.meshtastic.core.resources.site_planner_invalid_latitude
import org.meshtastic.core.resources.site_planner_invalid_longitude
import org.meshtastic.core.resources.site_planner_invalid_positive
import org.meshtastic.core.resources.site_planner_invalid_rx_sensitivity
import org.meshtastic.core.resources.site_planner_max_range_km
import org.meshtastic.core.resources.site_planner_rx_height_meters
import org.meshtastic.core.resources.site_planner_rx_sensitivity_dbm
import org.meshtastic.core.resources.site_planner_section_display
import org.meshtastic.core.resources.site_planner_section_receiver
import org.meshtastic.core.resources.site_planner_section_simulation
import org.meshtastic.core.resources.site_planner_section_transmitter
import org.meshtastic.core.resources.site_planner_subtitle
import org.meshtastic.core.resources.site_planner_tx_power_watts
import org.meshtastic.core.resources.site_planner_use_current_location
import org.meshtastic.core.resources.site_planner_use_map_center
import org.meshtastic.core.resources.site_planner_use_node_location
import org.meshtastic.core.ui.component.EditTextPreference
import org.meshtastic.core.ui.icon.Map
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosAccessory
import org.meshtastic.core.ui.ios.IosBottomSheet
import org.meshtastic.core.ui.ios.IosButton
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosMenu
import org.meshtastic.core.ui.ios.IosMenuItem
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.ios.iosRowSeparator

private const val LAT_LIMIT = 90.0
private const val LON_LIMIT = 180.0
private val SWATCH_WIDTH = 40.dp
private val SWATCH_HEIGHT = 16.dp

/** Editable form state; string-backed so partial/invalid input is preserved while typing. */
@Stable
private class SiteFormState(initial: SitePlannerParams) {
    var name by mutableStateOf(initial.name)
    var lat by mutableStateOf(initial.latitude.toString())
    var lon by mutableStateOf(initial.longitude.toString())
    var power by mutableStateOf(initial.txPowerWatts.toString())
    var freq by mutableStateOf(initial.txFreqMhz.toString())
    var height by mutableStateOf(initial.txHeightMeters.toString())
    var gain by mutableStateOf(initial.txGainDbi.toString())
    var colorScale by mutableStateOf(initial.colorScale)

    // Advanced (coverage-affecting) fields.
    var rxSensitivity by mutableStateOf(initial.rxSensitivityDbm.toString())
    var rxHeight by mutableStateOf(initial.rxHeightMeters.toString())
    var maxRange by mutableStateOf(initial.maxRangeKm.toString())
    var highResolution by mutableStateOf(initial.highResolution)

    // Validation — computed from the (observable) string fields, so callers just read the booleans.
    private val latValue
        get() = NumberFormatter.parseDecimalOrNull(lat)

    private val lonValue
        get() = NumberFormatter.parseDecimalOrNull(lon)

    val latBad
        get() = latValue.let { it == null || it !in -LAT_LIMIT..LAT_LIMIT }

    val lonBad
        get() = lonValue.let { it == null || it !in -LON_LIMIT..LON_LIMIT }

    val powerBad
        get() = (NumberFormatter.parseDecimalOrNull(power) ?: 0.0) <= 0.0

    val freqBad
        get() = (NumberFormatter.parseDecimalOrNull(freq) ?: 0.0) <= 0.0

    val rxSensBad
        get() =
            NumberFormatter.parseDecimalOrNull(rxSensitivity)?.let {
                it !in SitePlannerParams.MIN_RX_SENSITIVITY_DBM..SitePlannerParams.MAX_RX_SENSITIVITY_DBM
            } ?: false

    // Guard the null-island (0,0) case so an empty ocean run can't be submitted.
    val canSubmit
        get() = !latBad && !lonBad && !powerBad && !freqBad && !rxSensBad && (latValue != 0.0 || lonValue != 0.0)
}

/**
 * Bottom-sheet form for a Site Planner coverage estimate, pre-filled with [initial]. [onSubmit] receives the edited
 * params once they validate. Location shortcut chips ([onUseCurrentLocation]/[onUseNodeLocation]/[onUseMapCenter])
 * re-seed the coordinate fields when provided, preserving edits to the other fields.
 *
 * [note] is shown above the submit button. A host whose planner runs somewhere this app cannot read the result back
 * from uses it to say so, and to say what the user should do instead.
 */
@Composable
fun SitePlannerSheet(
    initial: SitePlannerParams,
    onSubmit: (SitePlannerParams) -> Unit,
    onDismiss: () -> Unit,
    onUseCurrentLocation: (() -> Unit)? = null,
    onUseNodeLocation: (() -> Unit)? = null,
    onUseMapCenter: (() -> Unit)? = null,
    note: String? = null,
) {
    val sheetState =
        rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        )
    val state = remember { SiteFormState(initial) }
    // Re-seed only the coordinates when a shortcut changes them; other fields keep the user's edits.
    LaunchedEffect(initial.latitude, initial.longitude) {
        state.lat = initial.latitude.toString()
        state.lon = initial.longitude.toString()
    }
    IosBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, grouped = true) {
        SiteFormContent(state, initial, onSubmit, onUseCurrentLocation, onUseNodeLocation, onUseMapCenter, note)
    }
}

/**
 * The planner's form as an iOS grouped form: a centred title over a footnote, then a section per panel of the planner
 * (Transmitter, Receiver, Simulation, Display) with each value as a titled row, and the estimate button at the foot.
 */
@Composable
private fun SiteFormContent(
    state: SiteFormState,
    initial: SitePlannerParams,
    onSubmit: (SitePlannerParams) -> Unit,
    onUseCurrentLocation: (() -> Unit)?,
    onUseNodeLocation: (() -> Unit)?,
    onUseMapCenter: (() -> Unit)?,
    note: String?,
) {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Text(
            text = stringResource(Res.string.site_planner),
            style = Ios.type.headline,
            color = Ios.colors.label,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).semantics { heading() },
        )
        Text(
            text = stringResource(Res.string.site_planner_subtitle),
            style = Ios.type.footnote,
            color = Ios.colors.secondaryLabel,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 6.dp),
        )
        // Sections in the planner's own panel order: Site / Transmitter → Receiver → Simulation Options → Display.
        TransmitterSection(state, onUseCurrentLocation, onUseNodeLocation, onUseMapCenter)
        ReceiverSection(state)
        SimulationSection(state)
        DisplaySection(state)
        note?.let {
            Text(
                text = it,
                style = Ios.type.footnote,
                color = Ios.colors.secondaryLabel,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
            )
        }
        IosButton(
            text = stringResource(Res.string.site_planner_estimate),
            onClick = { onSubmit(buildSubmitParams(state, initial)) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
            large = true,
            enabled = state.canSubmit,
        )
    }
}

@Composable
private fun TransmitterSection(
    state: SiteFormState,
    onUseCurrentLocation: (() -> Unit)?,
    onUseNodeLocation: (() -> Unit)?,
    onUseMapCenter: (() -> Unit)?,
) {
    val posMsg = stringResource(Res.string.site_planner_invalid_positive)
    IosSection(header = stringResource(Res.string.site_planner_section_transmitter)) {
        SiteField(state.name, { state.name = it }, Res.string.name, keyboardType = KeyboardType.Text)
        // Southern/western hemispheres and lossy antennas are negative, so these need a sign key.
        SiteField(
            state.lat,
            { state.lat = it },
            Res.string.latitude,
            keyboardType = KeyboardType.DecimalSigned,
            error = if (state.latBad) stringResource(Res.string.site_planner_invalid_latitude) else null,
        )
        SiteField(
            state.lon,
            { state.lon = it },
            Res.string.longitude,
            keyboardType = KeyboardType.DecimalSigned,
            error = if (state.lonBad) stringResource(Res.string.site_planner_invalid_longitude) else null,
        )
        SiteField(
            state.power,
            { state.power = it },
            Res.string.site_planner_tx_power_watts,
            error = if (state.powerBad) posMsg else null,
        )
        SiteField(
            state.freq,
            { state.freq = it },
            Res.string.site_planner_frequency_mhz,
            error = if (state.freqBad) posMsg else null,
        )
        SiteField(state.height, { state.height = it }, Res.string.site_planner_antenna_height_meters)
        SiteField(
            state.gain,
            { state.gain = it },
            Res.string.site_planner_antenna_gain_dbi,
            keyboardType = KeyboardType.DecimalSigned,
        )
    }
    LocationShortcuts(onUseCurrentLocation, onUseNodeLocation, onUseMapCenter)
}

@Composable
private fun ReceiverSection(state: SiteFormState) {
    IosSection(header = stringResource(Res.string.site_planner_section_receiver)) {
        // The whole valid range is negative (-150..-30 dBm), so an unsigned keypad reaches no valid value.
        SiteField(
            state.rxSensitivity,
            { state.rxSensitivity = it },
            Res.string.site_planner_rx_sensitivity_dbm,
            keyboardType = KeyboardType.DecimalSigned,
            error = if (state.rxSensBad) stringResource(Res.string.site_planner_invalid_rx_sensitivity) else null,
        )
        SiteField(state.rxHeight, { state.rxHeight = it }, Res.string.site_planner_rx_height_meters)
    }
}

@Composable
private fun SimulationSection(state: SiteFormState) {
    IosSection(header = stringResource(Res.string.site_planner_section_simulation)) {
        SiteField(state.maxRange, { state.maxRange = it }, Res.string.site_planner_max_range_km)
        IosRow(
            title = stringResource(Res.string.site_planner_high_resolution),
            accessory =
            IosAccessory.Switch(checked = state.highResolution, onCheckedChange = { state.highResolution = it }),
        )
    }
}

@Composable
private fun DisplaySection(state: SiteFormState) {
    IosSection(header = stringResource(Res.string.site_planner_section_display)) {
        PalettePicker(state.colorScale) { state.colorScale = it }
    }
}

/** Shortcuts that re-seed the coordinates from the device, this node, or the map center: tinted rows, as iOS's. */
@Composable
private fun LocationShortcuts(
    onUseCurrentLocation: (() -> Unit)?,
    onUseNodeLocation: (() -> Unit)?,
    onUseMapCenter: (() -> Unit)?,
) {
    if (onUseCurrentLocation == null && onUseNodeLocation == null && onUseMapCenter == null) return
    IosSection {
        onUseCurrentLocation?.let {
            IosRow(
                title = stringResource(Res.string.site_planner_use_current_location),
                icon = IosIcons.LocationFill,
                titleColor = Ios.colors.tint,
                onClick = it,
            )
        }
        onUseNodeLocation?.let {
            IosRow(
                title = stringResource(Res.string.site_planner_use_node_location),
                icon = IosIcons.MapPin,
                titleColor = Ios.colors.tint,
                onClick = it,
            )
        }
        onUseMapCenter?.let {
            IosRow(
                title = stringResource(Res.string.site_planner_use_map_center),
                icon = IosIcons.Map,
                titleColor = Ios.colors.tint,
                onClick = it,
            )
        }
    }
}

/** A titled value row of the form, the value typed at its end, as an iOS form's `TextField`. */
@Composable
private fun SiteField(
    value: String,
    onValueChange: (String) -> Unit,
    label: StringResource,
    modifier: Modifier = Modifier,
    // Every numeric field here is fractional (MHz, watts, dBi, km), so Decimal — not Number — is the floor.
    keyboardType: KeyboardType = KeyboardType.Decimal,
    error: String? = null,
) {
    EditTextPreference(
        title = stringResource(label),
        value = value,
        enabled = true,
        isError = error != null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        keyboardActions = KeyboardActions(),
        onValueChanged = onValueChange,
        summary = error,
        modifier = modifier.iosRowSeparator(),
    )
}

/** Color-scale picker: a row with the palette's swatch and name, choosing from a menu of every palette. */
@Composable
private fun PalettePicker(value: String, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IosRow(
        title = stringResource(Res.string.site_planner_color_scale),
        value = SitePlannerParams.COLOR_SCALES.firstOrNull { it.first == value }?.second ?: value,
        onClick = { expanded = true },
        accessory =
        IosAccessory.Custom {
            Box {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(8.dp))
                    PaletteSwatch(value)
                    Icon(
                        IosIcons.ChevronUpDown,
                        contentDescription = null,
                        tint = Ios.colors.secondaryLabel,
                        modifier = Modifier.padding(start = 6.dp).size(14.dp),
                    )
                }
                IosMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    SitePlannerParams.COLOR_SCALES.forEach { (key, label) ->
                        IosMenuItem(
                            text = label,
                            checked = key == value,
                            checkable = true,
                            onClick = {
                                onChange(key)
                                expanded = false
                            },
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun PaletteSwatch(name: String) {
    Box(
        Modifier.width(SWATCH_WIDTH)
            .height(SWATCH_HEIGHT)
            .clip(RoundedCornerShape(4.dp))
            .background(Brush.horizontalGradient(paletteStops(name))),
    )
}

/** Representative gradient stops per planner palette (approximations of the matplotlib LUTs for a preview swatch). */
@Suppress("MagicNumber")
private fun paletteStops(name: String): List<Color> {
    val argb =
        when (name) {
            "viridis" -> listOf(0xFF440154, 0xFF31688E, 0xFF35B779, 0xFFFDE725)
            "CMRmap" -> listOf(0xFF000000, 0xFF3B0F70, 0xFF8C2981, 0xFFDE4968, 0xFFFE9F6D, 0xFFFFFFFF)
            "cool" -> listOf(0xFF00FFFF, 0xFFFF00FF)
            "turbo" -> listOf(0xFF30123B, 0xFF4585F9, 0xFF1BD0D5, 0xFFA4FC3B, 0xFFFABA39, 0xFFE5460A, 0xFF7A0403)
            "jet" -> listOf(0xFF000080, 0xFF0000FF, 0xFF00FFFF, 0xFFFFFF00, 0xFFFF0000, 0xFF800000)
            else -> listOf(0xFF0D0887, 0xFF7E03A8, 0xFFCC4778, 0xFFF89540, 0xFFF0F921)
        }
    return argb.map { Color(it) }
}

/** Build params from the (string) form fields, falling back to the matching [initial] value when a field is invalid. */
private fun buildSubmitParams(state: SiteFormState, initial: SitePlannerParams): SitePlannerParams = SitePlannerParams(
    name = state.name.ifBlank { initial.name },
    latitude = NumberFormatter.parseDecimalOrNull(state.lat) ?: initial.latitude,
    longitude = NumberFormatter.parseDecimalOrNull(state.lon) ?: initial.longitude,
    txPowerWatts = NumberFormatter.parseDecimalOrNull(state.power) ?: initial.txPowerWatts,
    txFreqMhz = NumberFormatter.parseDecimalOrNull(state.freq) ?: initial.txFreqMhz,
    txHeightMeters = NumberFormatter.parseDecimalOrNull(state.height) ?: initial.txHeightMeters,
    txGainDbi = NumberFormatter.parseDecimalOrNull(state.gain) ?: initial.txGainDbi,
    colorScale = state.colorScale,
    rxSensitivityDbm = NumberFormatter.parseDecimalOrNull(state.rxSensitivity) ?: initial.rxSensitivityDbm,
    rxHeightMeters = NumberFormatter.parseDecimalOrNull(state.rxHeight) ?: initial.rxHeightMeters,
    maxRangeKm = NumberFormatter.parseDecimalOrNull(state.maxRange) ?: initial.maxRangeKm,
    highResolution = state.highResolution,
    minDbm = initial.minDbm,
    maxDbm = initial.maxDbm,
    overlayTransparency = initial.overlayTransparency,
)
