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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.navigate_back
import org.meshtastic.core.resources.pangea_settings_word
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosGlassIconButton
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosInset

/** The grey behind a light-mode iOS sheet. */
private val SheetBackdrop = Color(0xFFCFCFD4)

/** Goes back one setup page; null on the first page. Provided by the setup graph. */
@Suppress("CompositionLocalAllowlist")
internal val LocalIntroBack = staticCompositionLocalOf<(() -> Unit)?> { null }

/** True inside [PairRadioSheet]: the setup pages then draw no backdrop or rounded top of their own. */
@Suppress("CompositionLocalAllowlist")
internal val LocalSetupInSheet = staticCompositionLocalOf { false }

/**
 * The sheet every setup page sits on, as on the Pangea iPhone app: rounded top corners over grey, a glass back button
 * when [onBack] is set, scrolling [content] and a [bottom] pinned under it. With [centered] the content doesn't scroll
 * and sits in the middle of the page, as on "You're All Set".
 */
@Composable
internal fun IosSetupSheet(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = LocalIntroBack.current,
    bottom: @Composable () -> Unit = {},
    centered: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Ios.colors
    // Setup shows as a sheet over grey, as on iOS: rounded top corners a little below the status bar.
    val sheet = if (c.isDark) c.groupedRow else c.background
    val behind = if (c.isDark) Color.Black else SheetBackdrop
    Column(
        if (LocalSetupInSheet.current) {
            modifier.fillMaxSize().imePadding()
        } else {
            modifier
                .fillMaxSize()
                .background(behind)
                .statusBarsPadding()
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp))
                .background(sheet)
                .imePadding()
        },
    ) {
        // Pages without a back button hide the bar altogether, as iOS does (navigationBarBackButtonHidden).
        if (onBack != null) {
            Box(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = IosInset)) {
                IosGlassIconButton(
                    IosIcons.ChevronLeft,
                    contentDescription = stringResource(Res.string.navigate_back),
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
            }
        } else {
            Spacer(Modifier.height(16.dp))
        }
        Column(
            Modifier.weight(1f)
                .fillMaxWidth()
                .then(if (centered) Modifier else Modifier.verticalScroll(rememberScrollState()))
                .padding(horizontal = IosInset),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (centered) Arrangement.Center else Arrangement.Top,
            content = content,
        )
        bottom()
    }
}

/**
 * One setup page, as on the Pangea iPhone app: a glass back button, a large centred title, an explanation, an icon-led
 * list of what the step is for, and one full-width orange button pinned at the bottom (with an optional Skip link under
 * it). [plainDescription] draws the explanation in grey body text, as the pairing and name pages do.
 */
@Composable
internal fun IosSetupPage(
    title: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    plainDescription: Boolean = false,
    primaryEnabled: Boolean = true,
    skipLabel: String? = null,
    onSkip: () -> Unit = {},
    header: (@Composable () -> Unit)? = null,
    largeTitle: Boolean = false,
    onDescriptionLink: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    val c = Ios.colors
    IosSetupSheet(
        modifier = modifier,
        bottom = {
            IntroBottomBar(
                onSkip = onSkip,
                onConfigure = onPrimary,
                skipButtonText = skipLabel.orEmpty(),
                configureButtonText = primaryLabel,
                showSkipButton = skipLabel != null,
                configureEnabled = primaryEnabled,
            )
        },
    ) {
        header?.invoke()
        Text(
            text = title,
            style = if (largeTitle) Ios.type.largeTitle else Ios.type.title1.copy(fontWeight = FontWeight.Bold),
            color = c.label,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (description != null) {
            // iOS links the word "settings" to the app's system settings page.
            val settingsWord = stringResource(Res.string.pangea_settings_word)
            val linkColor = c.link
            val text =
                remember(description, settingsWord, linkColor, onDescriptionLink) {
                    buildAnnotatedString {
                        append(description)
                        val at = description.lastIndexOf(settingsWord)
                        if (onDescriptionLink != null && at >= 0) {
                            addLink(
                                LinkAnnotation.Clickable(
                                    tag = "settings",
                                    styles = TextLinkStyles(SpanStyle(color = linkColor)),
                                ) {
                                    onDescriptionLink()
                                },
                                at,
                                at + settingsWord.length,
                            )
                        }
                    }
                }
            Text(
                text = text,
                style = if (plainDescription) Ios.type.body else Ios.type.headline,
                color = if (plainDescription) c.secondaryLabel else c.label,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 22.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        content()
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
    // iOS makeRow: the symbol in a 72 × 60 frame (SF Symbols fill it more than these drawings do, hence the smaller
    // insets), the text 8 pt after it, and 16 pt between rows.
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Box(
            Modifier.size(width = 72.dp, height = 60.dp).padding(horizontal = 12.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (feature.capsule) {
                Box(
                    Modifier.size(width = 34.dp, height = 46.dp).clip(RoundedCornerShape(17.dp)).background(c.label),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = null,
                        tint = if (c.isDark) c.groupedRow else c.background,
                        modifier = Modifier.size(28.dp),
                    )
                }
            } else {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = null,
                    tint = feature.tint ?: c.label,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column {
            feature.titleRes?.let { titleRes ->
                Text(
                    text = stringResource(titleRes),
                    style = Ios.type.footnote.copy(fontWeight = FontWeight.SemiBold),
                    color = c.label,
                )
            }
            Text(text = stringResource(feature.subtitleRes), style = Ios.type.footnote, color = c.secondaryLabel)
        }
    }
}

/** A section title inside a setup page ("Send Notifications"), as iOS's bold title 2. */
@Composable
internal fun SetupSectionTitle(text: String) {
    Text(
        text = text,
        style = Ios.type.title2.copy(fontWeight = FontWeight.Bold),
        color = Ios.colors.label,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
}
