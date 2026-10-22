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
package org.meshtastic.feature.settings.radio.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.altitude
import org.meshtastic.core.resources.pangea_position_flag_altitude_msl
import org.meshtastic.core.resources.pangea_position_flag_dop
import org.meshtastic.core.resources.pangea_position_flag_geoidal_separation
import org.meshtastic.core.resources.pangea_position_flag_heading
import org.meshtastic.core.resources.pangea_position_flag_hvdop
import org.meshtastic.core.resources.pangea_position_flag_satellites
import org.meshtastic.core.resources.pangea_position_flag_sequence
import org.meshtastic.core.resources.pangea_position_flag_speed
import org.meshtastic.core.resources.pangea_position_flag_timestamp
import org.meshtastic.core.resources.schema_position_position_flags_description
import org.meshtastic.core.ui.component.PreferenceDivider
import org.meshtastic.core.ui.component.PreferenceIcon
import org.meshtastic.core.ui.component.SwitchPreference
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.proto.Config.PositionConfig.PositionFlags

/** One position flag's row: its label, its symbol (if iOS gives it one), and the flag it waits on, if any. */
private class PositionFlagRow(
    val flag: PositionFlags,
    val label: StringResource,
    val icon: ImageVector?,
    val shownWith: PositionFlags? = null,
)

private val flagRows by lazy {
    listOf(
        PositionFlagRow(PositionFlags.ALTITUDE, Res.string.altitude, IosIcons.ArrowUp),
        PositionFlagRow(
            PositionFlags.ALTITUDE_MSL,
            Res.string.pangea_position_flag_altitude_msl,
            IosIcons.ArrowUpToLine,
            shownWith = PositionFlags.ALTITUDE,
        ),
        PositionFlagRow(
            PositionFlags.GEOIDAL_SEPARATION,
            Res.string.pangea_position_flag_geoidal_separation,
            IosIcons.Globe,
            shownWith = PositionFlags.ALTITUDE,
        ),
        PositionFlagRow(PositionFlags.SATINVIEW, Res.string.pangea_position_flag_satellites, IosIcons.Skew),
        PositionFlagRow(PositionFlags.SEQ_NO, Res.string.pangea_position_flag_sequence, IosIcons.Number),
        PositionFlagRow(PositionFlags.TIMESTAMP, Res.string.pangea_position_flag_timestamp, IosIcons.Clock),
        PositionFlagRow(PositionFlags.SPEED, Res.string.pangea_position_flag_speed, IosIcons.Speedometer),
        PositionFlagRow(PositionFlags.HEADING, Res.string.pangea_position_flag_heading, IosIcons.LocationCircle),
        PositionFlagRow(PositionFlags.DOP, Res.string.pangea_position_flag_dop, null),
        PositionFlagRow(
            PositionFlags.HVDOP,
            Res.string.pangea_position_flag_hvdop,
            null,
            shownWith = PositionFlags.DOP,
        ),
    )
}

/**
 * The position flags as the iPhone app lays them out: the help text, then a switch per flag rather than one
 * multi-select list. The altitude and DOP details show once their parent flag is on; hiding them keeps their value.
 */
@Composable
internal fun PositionFlagRows(flags: Int, enabled: Boolean, onFlagsChange: (Int) -> Unit) {
    Text(
        text = stringResource(Res.string.schema_position_position_flags_description),
        style = Ios.type.body,
        color = Ios.colors.secondaryLabel,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    )
    flagRows.forEach { row ->
        val parent = row.shownWith
        if (parent == null || flags and parent.value != 0) {
            PreferenceDivider()
            val switch: @Composable () -> Unit = {
                SwitchPreference(
                    title = stringResource(row.label),
                    checked = flags and row.flag.value != 0,
                    enabled = enabled,
                    onCheckedChange = { on ->
                        onFlagsChange(if (on) flags or row.flag.value else flags and row.flag.value.inv())
                    },
                )
            }
            val icon = row.icon
            if (icon != null) PreferenceIcon(icon, content = switch) else switch()
        }
    }
}
