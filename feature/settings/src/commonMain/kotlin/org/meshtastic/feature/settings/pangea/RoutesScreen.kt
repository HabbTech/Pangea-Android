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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.model.pangea.Route
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.delete
import org.meshtastic.core.resources.okay
import org.meshtastic.core.resources.pangea_import_route
import org.meshtastic.core.resources.pangea_route_bad_file
import org.meshtastic.core.resources.pangea_route_bad_file_message
import org.meshtastic.core.resources.pangea_route_list
import org.meshtastic.core.resources.unknown
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosButton
import org.meshtastic.core.ui.ios.IosButtonStyle
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosPushedScreen
import org.meshtastic.core.ui.ios.IosSwipeAction
import org.meshtastic.core.ui.ios.IosSwipeActions
import org.meshtastic.core.ui.ios.iosPressable
import org.meshtastic.core.ui.util.rememberOpenFileLauncher

/**
 * The iPhone app's Route List (Settings → Routes): an Import Route capsule over the saved routes, each swiping to
 * Delete. Choosing one opens its editor in place, as iOS does; Back closes the editor before it leaves the page.
 */
@Composable
fun RoutesScreen(viewModel: RoutesViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val routes by viewModel.routes.collectAsStateWithLifecycle()
    val badFile by viewModel.badFile.collectAsStateWithLifecycle()
    var selectedId by rememberSaveable { mutableStateOf<Int?>(null) }
    val selected = routes.firstOrNull { it.id == selectedId }
    val unknown = stringResource(Res.string.unknown)
    // iOS asks for CSV files; Android's file managers label CSVs inconsistently, so any file is offered and its header
    // decides.
    val open = rememberOpenFileLauncher { uri -> uri?.let { viewModel.import(it, fallbackName = unknown) } }

    if (selected == null) {
        RouteList(
            routes = routes,
            onImport = { open("*/*") },
            onSelect = { selectedId = it.id },
            onDelete = viewModel::delete,
            onBack = onBack,
            modifier = modifier,
        )
    } else {
        NavigationBackHandler(
            state = rememberNavigationEventState(NavigationEventInfo.None),
            isBackEnabled = true,
            onBackCompleted = { selectedId = null },
        )
        RouteEditor(
            route = selected,
            onClose = { selectedId = null },
            onSave = {
                viewModel.save(it)
                selectedId = null
            },
            onExport = viewModel::export,
            modifier = modifier,
        )
    }

    if (badFile) {
        MeshtasticDialog(
            titleRes = Res.string.pangea_route_bad_file,
            messageRes = Res.string.pangea_route_bad_file_message,
            confirmTextRes = Res.string.okay,
            onConfirm = viewModel::dismissBadFile,
            // iOS's alert has OK alone; a tap outside or Back closes it too.
            onDismissRequest = viewModel::dismissBadFile,
        )
    }
}

@Composable
private fun RouteList(
    routes: List<Route>,
    onImport: () -> Unit,
    onSelect: (Route) -> Unit,
    onDelete: (Route) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val delete = stringResource(Res.string.delete)
    IosPushedScreen(
        title = stringResource(Res.string.pangea_route_list),
        onBack = onBack,
        grouped = false,
        modifier = modifier,
        showRadioChip = false,
    ) {
        item(key = "import") {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                IosButton(
                    text = stringResource(Res.string.pangea_import_route),
                    onClick = onImport,
                    style = IosButtonStyle.Tinted,
                    large = true,
                    fullWidth = false,
                )
            }
        }
        routes.forEachIndexed { index, route ->
            item(key = route.id) {
                IosSwipeActions(
                    actions = listOf(IosSwipeAction(delete, IosIcons.Trash, Ios.colors.systemRed) { onDelete(route) }),
                ) {
                    RouteRow(route = route, showSeparator = index > 0, onClick = { onSelect(route) })
                }
            }
        }
    }
}

/**
 * A route as iOS's plain list shows it: a 40 pt dot in its colour (checked when it is on the mesh map), its name, date
 * and notes, and its point count at the end.
 */
@Composable
private fun RouteRow(route: Route, showSeparator: Boolean, onClick: () -> Unit) {
    val c = Ios.colors
    val color = route.swatch()
    Column(Modifier.fillMaxWidth().background(c.background).iosPressable(onClick = onClick)) {
        if (showSeparator) {
            Box(Modifier.padding(start = TEXT_START).fillMaxWidth().height(0.5.dp).background(c.separator))
        }
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                if (route.enabled) {
                    Icon(
                        IosIcons.CheckCircleFill,
                        contentDescription = null,
                        tint = if (color.luminance() > LIGHT_LUMINANCE) Color.Black else Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f).padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(route.name, style = Ios.type.headline, color = c.label)
                Text(
                    DateFormatter.formatDateTimeNumeric(route.createdMillis),
                    style = Ios.type.callout,
                    color = c.systemGray,
                )
                if (route.notes.isNotEmpty()) Text(route.notes, style = Ios.type.callout, color = c.systemGray)
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                IosIcons.MapPinEllipse,
                contentDescription = null,
                tint = c.secondaryLabel,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(3.dp))
            Text(route.points.size.toString(), style = Ios.type.headline, color = c.secondaryLabel)
        }
    }
}

/** A route's colour, opaque. */
internal fun Route.swatch(): Color = Color(OPAQUE or (color and Route.COLOR_MASK))

/** Where a row's words start: the 20 pt inset, the 40 pt dot and the gap after it. */
private val TEXT_START = 76.dp

/** iOS's `isLight()`: above this, a dot takes a black checkmark. */
private const val LIGHT_LUMINANCE = 0.5f

private const val OPAQUE = -0x1000000
