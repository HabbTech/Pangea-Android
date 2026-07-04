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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.intro_expert_advanced_user
import org.meshtastic.core.resources.intro_expert_advanced_user_desc
import org.meshtastic.core.resources.intro_expert_change_anytime
import org.meshtastic.core.resources.intro_expert_change_anytime_desc
import org.meshtastic.core.resources.intro_expert_continue
import org.meshtastic.core.resources.intro_expert_hidden_options
import org.meshtastic.core.resources.intro_expert_hidden_options_desc
import org.meshtastic.core.resources.intro_expert_mode_title
import org.meshtastic.core.resources.intro_expert_standard_user
import org.meshtastic.core.resources.intro_expert_standard_user_desc
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.Person
import org.meshtastic.core.ui.icon.Settings
import org.meshtastic.core.ui.icon.Tune
import org.meshtastic.core.ui.icon.VisibilityOff
import org.meshtastic.core.ui.theme.AppTheme

/**
 * Informational final step of the intro flow describing Expert Mode: who it is for, what it hides, and where to
 * toggle it later. Deliberately has no inline toggle - advanced users opt in from Settings.
 *
 * @param onContinue Callback invoked when the user finishes the intro flow from this screen.
 */
@Composable
internal fun ExpertModeScreen(onContinue: () -> Unit) {
    val rows =
        listOf(
            FeatureUIData(
                icon = MeshtasticIcons.Person,
                titleRes = Res.string.intro_expert_standard_user,
                subtitleRes = Res.string.intro_expert_standard_user_desc,
            ),
            FeatureUIData(
                icon = MeshtasticIcons.Tune,
                titleRes = Res.string.intro_expert_advanced_user,
                subtitleRes = Res.string.intro_expert_advanced_user_desc,
            ),
            FeatureUIData(
                icon = MeshtasticIcons.VisibilityOff,
                titleRes = Res.string.intro_expert_hidden_options,
                subtitleRes = Res.string.intro_expert_hidden_options_desc,
            ),
            FeatureUIData(
                icon = MeshtasticIcons.Settings,
                titleRes = Res.string.intro_expert_change_anytime,
                subtitleRes = Res.string.intro_expert_change_anytime_desc,
            ),
        )

    Scaffold(
        bottomBar = {
            IntroBottomBar(
                onSkip = {},
                onConfigure = onContinue,
                skipButtonText = "",
                configureButtonText = stringResource(Res.string.intro_expert_continue),
                showSkipButton = false,
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
            Modifier.fillMaxSize().padding(innerPadding).padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.intro_expert_mode_title),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(32.dp))
            rows.forEach { row ->
                FeatureRow(feature = row)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Suppress("PreviewPublic")
@PreviewLightDark
@Composable
fun ExpertModeScreenPreview() {
    AppTheme { Surface { ExpertModeScreen(onContinue = {}) } }
}
