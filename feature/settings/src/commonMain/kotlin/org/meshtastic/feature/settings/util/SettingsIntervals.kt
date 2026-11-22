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
package org.meshtastic.feature.settings.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.pangea_unset

val gpioPins = (0..48).map { it to "Pin $it" }

/** [gpioPins] as the iPhone app lists them: pin 0 reads "Unset", since the firmware takes 0 to mean no pin. */
@Composable
fun rememberGpioPins(): List<Pair<Int, String>> {
    val unset = stringResource(Res.string.pangea_unset)
    return remember(unset) { gpioPins.map { (pin, label) -> pin to if (pin == 0) unset else label } }
}
