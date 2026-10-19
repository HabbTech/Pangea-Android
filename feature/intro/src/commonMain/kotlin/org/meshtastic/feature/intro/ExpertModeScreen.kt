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

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
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
 * Informational final step of the intro flow describing Expert Mode: who it is for, what it hides, and where to toggle
 * it later. Deliberately has no inline toggle - advanced users opt in from Settings.
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

    IosSetupPage(
        title = stringResource(Res.string.intro_expert_mode_title),
        primaryLabel = stringResource(Res.string.intro_expert_continue),
        onPrimary = onContinue,
    ) {
        rows.forEach { row -> FeatureRow(feature = row) }
    }
}

@Suppress("PreviewPublic")
@PreviewLightDark
@Composable
fun ExpertModeScreenPreview() {
    AppTheme { Surface { ExpertModeScreen(onContinue = {}) } }
}
