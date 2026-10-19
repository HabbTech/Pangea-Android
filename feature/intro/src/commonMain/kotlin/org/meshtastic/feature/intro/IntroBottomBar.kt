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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.meshtastic.core.ui.ios.IosButton
import org.meshtastic.core.ui.ios.IosButtonStyle

/**
 * The bottom of an introduction page, as on the Pangea iPhone app's setup pages: one full-width orange button for the
 * main action, and the skip as a plain text link under it.
 *
 * @param onSkip Callback for the skip action.
 * @param onConfigure Callback for the main configure/next action.
 * @param skipButtonText Text for the skip link.
 * @param configureButtonText Text for the main button.
 * @param showSkipButton Whether to show the skip link. Defaults to true.
 * @param configureEnabled Whether the main button can be pressed yet.
 */
@Composable
internal fun IntroBottomBar(
    onSkip: () -> Unit,
    onConfigure: () -> Unit,
    skipButtonText: String,
    configureButtonText: String,
    showSkipButton: Boolean = true,
    configureEnabled: Boolean = true,
) {
    Column(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IosButton(text = configureButtonText, onClick = onConfigure, large = true, enabled = configureEnabled)
        if (showSkipButton) {
            IosButton(
                text = skipButtonText,
                onClick = onSkip,
                style = IosButtonStyle.Plain,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
