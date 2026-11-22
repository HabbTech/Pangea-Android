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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.pangea_all_set_desc
import org.meshtastic.core.resources.pangea_all_set_title
import org.meshtastic.core.resources.pangea_start_messaging
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosIcons

/**
 * The last setup page, as on the Pangea iPhone app (SetupDoneView): a green check, "You're All Set" and a line of grey
 * text in the middle of the page, and **Start Messaging** at the bottom.
 */
@Composable
internal fun AllSetScreen(onStart: () -> Unit) {
    val c = Ios.colors
    IosSetupSheet(
        centered = true,
        bottom = {
            IntroBottomBar(
                onSkip = {},
                onConfigure = onStart,
                skipButtonText = "",
                configureButtonText = stringResource(Res.string.pangea_start_messaging),
                showSkipButton = false,
            )
        },
    ) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(c.systemGreen), contentAlignment = Alignment.Center) {
            Icon(IosIcons.Checkmark, contentDescription = null, tint = Color.White, modifier = Modifier.size(42.dp))
        }
        Text(
            stringResource(Res.string.pangea_all_set_title),
            style = Ios.type.largeTitle,
            color = c.label,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            stringResource(Res.string.pangea_all_set_desc),
            style = Ios.type.body,
            color = c.secondaryLabel,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}
