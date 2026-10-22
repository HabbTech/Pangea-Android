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
package org.meshtastic.feature.settings.pangea

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.CommonUri
import org.meshtastic.core.model.pangea.Route
import org.meshtastic.core.model.pangea.RouteCsv
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.enabled
import org.meshtastic.core.resources.name
import org.meshtastic.core.resources.navigate_back
import org.meshtastic.core.resources.notes
import org.meshtastic.core.resources.pangea_color
import org.meshtastic.core.resources.pangea_route_export
import org.meshtastic.core.resources.pangea_route_log
import org.meshtastic.core.resources.pangea_route_show_on_map
import org.meshtastic.core.resources.save
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosAccessory
import org.meshtastic.core.ui.ios.IosButton
import org.meshtastic.core.ui.ios.IosButtonStyle
import org.meshtastic.core.ui.ios.IosColorPickerSheet
import org.meshtastic.core.ui.ios.IosColorWell
import org.meshtastic.core.ui.ios.IosDrawsUnderTabBar
import org.meshtastic.core.ui.ios.IosGlassIconButton
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.ios.IosTopBar
import org.meshtastic.core.ui.ios.LocalIosBottomInset
import org.meshtastic.core.ui.ios.iosPressable
import org.meshtastic.core.ui.ios.iosRowSeparator
import org.meshtastic.core.ui.util.LocalRouteMapProvider
import org.meshtastic.core.ui.util.rememberSaveFileLauncher

/**
 * iOS's route editor, which takes the Route List's place when a route is chosen: Name, Enabled ("Show on the mesh
 * map."), Color and Notes, then Cancel and Save (until something changes, Save is greyed), and the route on a map that
 * fills the rest, with Export over its corner. The title is the name being typed.
 */
@Composable
internal fun RouteEditor(
    route: Route,
    onClose: () -> Unit,
    onSave: (Route) -> Unit,
    onExport: (Route, CommonUri) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Ios.colors
    var name by rememberSaveable(route.id) { mutableStateOf(route.name) }
    var notes by rememberSaveable(route.id) { mutableStateOf(route.notes) }
    var enabled by rememberSaveable(route.id) { mutableStateOf(route.enabled) }
    var color by rememberSaveable(route.id) { mutableIntStateOf(route.color) }
    var picking by remember { mutableStateOf(false) }
    val changed = name != route.name || notes != route.notes || enabled != route.enabled || color != route.color
    val exportName = RouteCsv.exportFileName(stringResource(Res.string.pangea_route_log, route.name))
    val export = rememberSaveFileLauncher { uri -> onExport(route, uri) }

    IosDrawsUnderTabBar()
    val bottom = LocalIosBottomInset.current
    Box(modifier.fillMaxSize().background(c.groupedBackground)) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.windowInsetsPadding(WindowInsets.statusBars).height(EDITOR_BAR_HEIGHT))
            RouteForm(
                name = name,
                onNameChange = { name = it.withinBytes(Route.MAX_NAME_BYTES) },
                notes = notes,
                onNotesChange = { notes = it },
                enabled = enabled,
                onEnabledChange = { enabled = it },
                color = route.copy(color = color).swatch(),
                onPickColor = { picking = true },
            )
            EditorButtons(
                changed = changed,
                onCancel = onClose,
                onSave = { onSave(route.copy(name = name, notes = notes, enabled = enabled, color = color)) },
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LocalRouteMapProvider.current(route.points, color, Modifier.fillMaxSize())
                ExportButton(
                    onClick = { export(exportName, CSV_MIME) },
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = bottom + 16.dp),
                )
            }
        }
        IosTopBar(
            title = name,
            showTitle = true,
            background = c.groupedBackground,
            leading = {
                IosGlassIconButton(
                    IosIcons.ChevronLeft,
                    contentDescription = stringResource(Res.string.navigate_back),
                    onClick = onClose,
                )
            },
        )
    }

    if (picking) {
        IosColorPickerSheet(
            color = route.copy(color = color).swatch(),
            onColorChange = { color = it.toArgb() and Route.COLOR_MASK },
            onDismissRequest = { picking = false },
        )
    }
}

/** iOS's Cancel and Save capsules, side by side; Save is greyed until something changes. */
@Composable
private fun EditorButtons(changed: Boolean, onCancel: () -> Unit, onSave: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        IosButton(
            text = stringResource(Res.string.cancel),
            onClick = onCancel,
            style = IosButtonStyle.Tinted,
            large = true,
            fullWidth = false,
        )
        IosButton(
            text = stringResource(Res.string.save),
            onClick = onSave,
            style = IosButtonStyle.Tinted,
            enabled = changed,
            large = true,
            fullWidth = false,
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun RouteForm(
    name: String,
    onNameChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    color: Color,
    onPickColor: () -> Unit,
) {
    IosSection {
        FormTextRow(value = name, onValueChange = onNameChange, placeholder = stringResource(Res.string.name))
        IosRow(
            title = stringResource(Res.string.enabled),
            icon = IosIcons.CurvePath,
            subtitle = stringResource(Res.string.pangea_route_show_on_map),
            accessory = IosAccessory.Switch(enabled, onEnabledChange),
        )
        IosRow(
            title = stringResource(Res.string.pangea_color),
            accessory = IosAccessory.Custom { IosColorWell(color = color, onClick = onPickColor) },
            onClick = onPickColor,
        )
        FormTextRow(
            value = notes,
            onValueChange = onNotesChange,
            placeholder = stringResource(Res.string.notes),
            minLines = NOTES_MIN_LINES,
            maxLines = NOTES_MAX_LINES,
        )
    }
}

/** iOS's untitled form field (`TextField(axis: .vertical)`): grey words that wrap, the placeholder when empty. */
@Composable
private fun FormTextRow(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    minLines: Int = 1,
    maxLines: Int = NAME_MAX_LINES,
) {
    val c = Ios.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = Ios.type.body.copy(color = c.systemGray),
        cursorBrush = SolidColor(c.tint),
        minLines = minLines,
        maxLines = maxLines,
        modifier = Modifier.iosRowSeparator().fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, style = Ios.type.body, color = c.tertiaryLabel)
                inner()
            }
        },
    )
}

/** iOS's glass Export capsule over the map. */
@Composable
private fun ExportButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ios.colors
    Row(
        modifier
            .height(50.dp)
            .shadow(10.dp, CircleShape, ambientColor = c.chromeShadow, spotColor = c.chromeShadow)
            .clip(CircleShape)
            .background(c.chrome)
            .iosPressable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(IosIcons.ShareDown, contentDescription = null, tint = c.tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(Res.string.pangea_route_export), style = Ios.type.body, color = c.tint)
    }
}

/** [this] cut to at most [maxBytes] of UTF-8, a character at a time from the end, as iOS trims a route's name. */
internal fun String.withinBytes(maxBytes: Int): String {
    var text = this
    while (text.encodeToByteArray().size > maxBytes) text = text.dropLast(1)
    return text
}

private val EDITOR_BAR_HEIGHT = 60.dp
private const val CSV_MIME = "text/csv"
private const val NAME_MAX_LINES = 4
private const val NOTES_MIN_LINES = 3
private const val NOTES_MAX_LINES = 5
