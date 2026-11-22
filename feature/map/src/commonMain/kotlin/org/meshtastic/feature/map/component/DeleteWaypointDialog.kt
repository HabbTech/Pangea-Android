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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.delete_for_everyone
import org.meshtastic.core.resources.delete_for_me
import org.meshtastic.core.resources.waypoint_delete
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosButton
import org.meshtastic.core.ui.ios.IosButtonStyle

/**
 * Waypoint removal confirmation, shared by both map flavors. Dropping our local copy needs no mesh-wide permission, so
 * it stays available for a waypoint locked to another node. Broadcasting the removal to the mesh is offered only when
 * [canDeleteForEveryone] (unlocked or locked to us, and connected).
 *
 * As on iOS, the ways to delete are the choices themselves, each a red capsule, stacked over Cancel.
 */
@Composable
fun DeleteWaypointDialog(
    canDeleteForEveryone: Boolean,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MeshtasticDialog(
        modifier = modifier,
        titleRes = Res.string.waypoint_delete,
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Keyed on eligibility at composition, so a mid-dialog disconnect takes the broadcast choice away.
                if (canDeleteForEveryone) {
                    DeleteChoice(stringResource(Res.string.delete_for_everyone), onDeleteForEveryone)
                }
                DeleteChoice(stringResource(Res.string.delete_for_me), onDeleteForMe)
            }
        },
        dismissTextRes = Res.string.cancel,
        onDismiss = onDismissRequest,
    )
}

@Composable
private fun DeleteChoice(label: String, onClick: () -> Unit) {
    IosButton(
        text = label,
        onClick = onClick,
        style = IosButtonStyle.Destructive,
        textStyle = Ios.type.body.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier.fillMaxWidth(),
    )
}
