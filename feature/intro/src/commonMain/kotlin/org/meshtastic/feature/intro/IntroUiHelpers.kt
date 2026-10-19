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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.navigate_back
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosGlassIconButton
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosInset

/** The grey behind a light-mode iOS sheet. */
private val SheetBackdrop = Color(0xFFCFCFD4)

/** Goes back one setup page; null on the first page. Provided by the setup graph. */
@Suppress("CompositionLocalAllowlist")
internal val LocalIntroBack = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * One setup page, as on the Pangea iPhone app: a glass back button, a large centred title, an explanation, an icon-led
 * list of what the step is for, and one full-width orange button pinned at the bottom (with an optional Skip link under
 * it).
 */
@Composable
internal fun IosSetupPage(
    title: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    skipLabel: String? = null,
    onSkip: () -> Unit = {},
    header: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val c = Ios.colors
    val onBack = LocalIntroBack.current
    // Setup shows as a sheet over grey, as on iOS: rounded top corners a little below the status bar.
    val sheet = if (c.isDark) c.groupedRow else c.background
    val behind = if (c.isDark) Color.Black else SheetBackdrop
    Column(
        modifier
            .fillMaxSize()
            .background(behind)
            .statusBarsPadding()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp))
            .background(sheet),
    ) {
        Box(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = IosInset)) {
            if (onBack != null) {
                IosGlassIconButton(
                    IosIcons.ChevronLeft,
                    contentDescription = stringResource(Res.string.navigate_back),
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
            }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = IosInset),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            header?.invoke()
            Text(
                text = title,
                style = Ios.type.title1.copy(fontWeight = FontWeight.Bold),
                color = c.label,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (description != null) {
                Text(
                    text = description,
                    style = Ios.type.headline,
                    color = c.label,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            content()
        }
        IntroBottomBar(
            onSkip = onSkip,
            onConfigure = onPrimary,
            skipButtonText = skipLabel.orEmpty(),
            configureButtonText = primaryLabel,
            showSkipButton = skipLabel != null,
        )
    }
}

/**
 * One point in a setup page's list: an icon, a bold label and a grey explanation, as on the iPhone app.
 *
 * @param feature The [FeatureUIData] containing information for the row.
 */
@Composable
internal fun FeatureRow(feature: FeatureUIData) {
    val c = Ios.colors
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Box(Modifier.width(56.dp), contentAlignment = Alignment.CenterStart) {
            Icon(imageVector = feature.icon, contentDescription = null, modifier = Modifier.size(34.dp), tint = c.label)
        }
        Column {
            feature.titleRes?.let { titleRes ->
                Text(
                    text = stringResource(titleRes),
                    style = Ios.type.subheadline.copy(fontWeight = FontWeight.SemiBold),
                    color = c.label,
                )
            }
            Text(text = stringResource(feature.subtitleRes), style = Ios.type.subheadline, color = c.secondaryLabel)
        }
    }
}
