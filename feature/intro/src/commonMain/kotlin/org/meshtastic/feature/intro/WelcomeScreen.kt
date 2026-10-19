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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.get_started
import org.meshtastic.core.resources.pangea_welcome_offgrid_desc
import org.meshtastic.core.resources.pangea_welcome_offgrid_title
import org.meshtastic.core.resources.pangea_welcome_private_desc
import org.meshtastic.core.resources.pangea_welcome_private_title
import org.meshtastic.core.resources.pangea_welcome_ready_desc
import org.meshtastic.core.resources.pangea_welcome_ready_title
import org.meshtastic.core.resources.pangea_welcome_title
import org.meshtastic.core.ui.icon.CheckCircle
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.ShieldCheck
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.core.ui.util.LocalAnalyticsIntroProvider

/**
 * The initial welcome screen for the app introduction flow. It displays a brief overview of the app's key features.
 *
 * @param onGetStarted Callback invoked when the user proceeds from the welcome screen.
 */
@Composable
internal fun WelcomeScreen(onGetStarted: () -> Unit) {
    val analyticsIntro = LocalAnalyticsIntroProvider.current
    val features =
        listOf(
            FeatureUIData(
                icon = IosIcons.Bubble,
                titleRes = Res.string.pangea_welcome_offgrid_title,
                subtitleRes = Res.string.pangea_welcome_offgrid_desc,
            ),
            FeatureUIData(
                icon = MeshtasticIcons.ShieldCheck,
                titleRes = Res.string.pangea_welcome_private_title,
                subtitleRes = Res.string.pangea_welcome_private_desc,
            ),
            FeatureUIData(
                icon = MeshtasticIcons.CheckCircle,
                titleRes = Res.string.pangea_welcome_ready_title,
                subtitleRes = Res.string.pangea_welcome_ready_desc,
            ),
        )
    IosSetupPage(
        title = stringResource(Res.string.pangea_welcome_title),
        primaryLabel = stringResource(Res.string.get_started),
        onPrimary = onGetStarted,
    ) {
        features.forEach { feature -> FeatureRow(feature = feature) }
        Spacer(modifier = Modifier.height(24.dp))
        analyticsIntro()
    }
}

@Suppress("PreviewPublic")
@PreviewLightDark
@Composable
fun WelcomeScreenPreview() {
    AppTheme { Surface { WelcomeScreen(onGetStarted = {}) } }
}
