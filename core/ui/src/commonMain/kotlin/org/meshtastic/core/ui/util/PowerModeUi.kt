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
package org.meshtastic.core.ui.util

import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import org.meshtastic.core.model.PowerMode
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.power_mode_expedition
import org.meshtastic.core.resources.power_mode_expedition_summary
import org.meshtastic.core.resources.power_mode_standard
import org.meshtastic.core.resources.power_mode_standard_summary
import org.meshtastic.core.resources.power_mode_trail
import org.meshtastic.core.resources.power_mode_trail_summary
import org.meshtastic.core.ui.icon.ElectricPower
import org.meshtastic.core.ui.icon.Elevation
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Route

/** Display label for a [PowerMode]. */
val PowerMode.labelRes: StringResource
    get() =
        when (this) {
            PowerMode.STANDARD -> Res.string.power_mode_standard
            PowerMode.TRAIL -> Res.string.power_mode_trail
            PowerMode.EXPEDITION -> Res.string.power_mode_expedition
        }

/** One-paragraph summary of what a [PowerMode] trades off. */
val PowerMode.summaryRes: StringResource
    get() =
        when (this) {
            PowerMode.STANDARD -> Res.string.power_mode_standard_summary
            PowerMode.TRAIL -> Res.string.power_mode_trail_summary
            PowerMode.EXPEDITION -> Res.string.power_mode_expedition_summary
        }

/** Icon for a [PowerMode] (nearest Material equivalents of the iOS bolt/hiking/mountain symbols). */
val PowerMode.icon: ImageVector
    get() =
        when (this) {
            PowerMode.STANDARD -> MeshtasticIcons.ElectricPower
            PowerMode.TRAIL -> MeshtasticIcons.Route
            PowerMode.EXPEDITION -> MeshtasticIcons.Elevation
        }
