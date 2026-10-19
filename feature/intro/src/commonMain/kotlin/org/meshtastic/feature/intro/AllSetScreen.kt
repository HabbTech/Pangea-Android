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
package org.meshtastic.feature.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.pangea_all_set_desc
import org.meshtastic.core.resources.pangea_all_set_title
import org.meshtastic.core.resources.pangea_start_messaging
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosIcons

/** The last setup page, as on the Pangea iPhone app: a green check, "You're All Set", and **Start Messaging**. */
@Composable
internal fun AllSetScreen(onStart: () -> Unit) {
    IosSetupPage(
        title = stringResource(Res.string.pangea_all_set_title),
        description = stringResource(Res.string.pangea_all_set_desc),
        primaryLabel = stringResource(Res.string.pangea_start_messaging),
        onPrimary = onStart,
        header = {
            Box(
                Modifier.padding(top = 120.dp, bottom = 24.dp)
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Ios.colors.systemGreen),
                contentAlignment = Alignment.Center,
            ) {
                Icon(IosIcons.Checkmark, contentDescription = null, tint = Color.White, modifier = Modifier.size(56.dp))
            }
        },
    )
}
