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
@file:Suppress("MagicNumber")

package org.meshtastic.core.ui.ios

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.inter_regular
import org.meshtastic.core.resources.inter_semibold

/**
 * Pangea's iOS look: the colours, type and shapes of the Pangea iPhone app, so Android matches it pixel for pixel
 * (design/PANGEA-BRAND.md v2). Values are Apple's published system colours and the iOS default ("Large") type scale.
 */
@Immutable
data class IosColors(
    /** The app tint (`pangeaPrimary`). */
    val tint: Color,
    val onTint: Color,
    /** Soft fill for secondary buttons (`pangeaPrimaryContainer`) and the text on it. */
    val tintContainer: Color,
    val onTintContainer: Color,
    val expertCaption: Color,
    /** Plain screens (Chats, a chat) — `systemBackground`. */
    val background: Color,
    /** Settings-style screens behind the cards — `systemGroupedBackground`. */
    val groupedBackground: Color,
    /** Cards and rows on a grouped screen — `secondarySystemGroupedBackground`. */
    val groupedRow: Color,
    /** Fields inside a card, and sheets' fields — `tertiarySystemFill` over the row. */
    val fieldFill: Color,
    /** `.tertiarySystemFill`: search fields, translucent so they darken whatever they sit on. */
    val searchFill: Color,
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val separator: Color,
    /** Glass circle buttons and the floating tab bar. */
    val chrome: Color,
    val chromeSelected: Color,
    val chromeShadow: Color,
    /** Their messages. */
    val incomingBubble: Color,
    val systemGray: Color,
    val systemGray4: Color,
    val systemRed: Color,
    val systemGreen: Color,
    val systemBlue: Color,
    /** The "settings" links on the setup pages. */
    val link: Color,
    /** `.orange`: warnings that aren't errors. */
    val systemOrange: Color,
    /** `.yellow`: favourites' stars and an open lock. */
    val systemYellow: Color,
    /** `.background.secondary` inside a sheet: cards and fields on the setup pages. */
    val secondaryFill: Color,
    /** The globe circle for Everyone Nearby. */
    val everyoneNearby: Color,
    val isDark: Boolean,
)

val LightIosColors =
    IosColors(
        tint = Color(0xFFF05511),
        onTint = Color.White,
        tintContainer = Color(0xFFFFDBCB),
        onTintContainer = Color(0xFF3B0900),
        expertCaption = Color(0xFF2855A8),
        background = Color.White,
        groupedBackground = Color(0xFFF2F2F7),
        groupedRow = Color.White,
        fieldFill = Color(0xFFEEEEEF),
        searchFill = Color(0x24767680),
        label = Color.Black,
        secondaryLabel = Color(0x993C3C43),
        tertiaryLabel = Color(0x4D3C3C43),
        separator = Color(0xFFC6C6C8),
        chrome = Color(0xF7FFFFFF),
        chromeSelected = Color(0xFFE9E9EB),
        chromeShadow = Color(0x26000000),
        incomingBubble = Color(0xFFE9E9EB),
        systemGray = Color(0xFF8E8E93),
        systemGray4 = Color(0xFFD1D1D6),
        systemRed = Color(0xFFFF3B30),
        systemGreen = Color(0xFF34C759),
        systemBlue = Color(0xFF0088FF),
        link = Color(0xFF5C6BC0),
        systemOrange = Color(0xFFFF9500),
        systemYellow = Color(0xFFFFCC00),
        secondaryFill = Color(0xFFF2F2F7),
        everyoneNearby = Color(0xFF3355A6),
        isDark = false,
    )

val DarkIosColors =
    IosColors(
        tint = Color(0xFFF05511),
        onTint = Color.White,
        tintContainer = Color(0xFF7A2E04),
        onTintContainer = Color(0xFFFFDBCB),
        expertCaption = Color(0xFFB0BFF0),
        background = Color.Black,
        groupedBackground = Color.Black,
        groupedRow = Color(0xFF1C1C1E),
        fieldFill = Color(0xFF2C2C2E),
        searchFill = Color(0x3D767680),
        label = Color.White,
        secondaryLabel = Color(0x99EBEBF5),
        tertiaryLabel = Color(0x4DEBEBF5),
        separator = Color(0xFF38383A),
        chrome = Color(0xF21C1C1E),
        chromeSelected = Color(0xFF3A3A3C),
        chromeShadow = Color(0x66000000),
        incomingBubble = Color(0xFF26252A),
        systemGray = Color(0xFF8E8E93),
        systemGray4 = Color(0xFF3A3A3C),
        systemRed = Color(0xFFFF453A),
        systemGreen = Color(0xFF30D158),
        systemBlue = Color(0xFF0091FF),
        link = Color(0xFF5C6BC0),
        systemOrange = Color(0xFFFF9F0A),
        systemYellow = Color(0xFFFFD60A),
        secondaryFill = Color(0xFF2C2C2E),
        everyoneNearby = Color(0xFF3355A6),
        isDark = true,
    )

/** The iOS text styles (Dynamic Type "Large"), set in Inter because San Francisco is licensed for Apple platforms. */
@Immutable
data class IosTypography(
    val largeTitle: TextStyle,
    val title1: TextStyle,
    val title2: TextStyle,
    val title3: TextStyle,
    val headline: TextStyle,
    val body: TextStyle,
    val callout: TextStyle,
    val subheadline: TextStyle,
    val footnote: TextStyle,
    val caption1: TextStyle,
    val caption2: TextStyle,
    /** Personal codes, like `@0500-000V`: monospaced so they read character by character. */
    val code: TextStyle,
)

/**
 * Inter runs a little wider than San Francisco at the same size, so every style tracks slightly tighter than Apple's
 * published tracking to keep line breaks where iOS puts them.
 */
private const val INTER_TIGHTEN = -0.012f

fun iosTypography(family: FontFamily): IosTypography {
    fun style(size: Int, line: Int, tracking: Float, weight: FontWeight = FontWeight.Normal) = TextStyle(
        fontFamily = family,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = (tracking / size + INTER_TIGHTEN).em,
    )
    return IosTypography(
        largeTitle = style(34, 41, 0.40f, FontWeight.Bold),
        title1 = style(28, 34, 0.38f),
        title2 = style(22, 28, -0.26f),
        title3 = style(20, 25, -0.45f),
        headline = style(17, 22, -0.43f, FontWeight.SemiBold),
        body = style(17, 22, -0.43f),
        callout = style(16, 21, -0.31f),
        subheadline = style(15, 20, -0.23f),
        footnote = style(13, 18, -0.08f),
        caption1 = style(12, 16, 0f),
        caption2 = style(11, 13, 0.06f),
        code = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 17.sp, lineHeight = 22.sp),
    )
}

/** Inter Regular and SemiBold (SIL Open Font License 1.1; the notice is in the font files). Bold renders SemiBold. */
@Composable
fun interFontFamily(): FontFamily =
    FontFamily(Font(Res.font.inter_regular, FontWeight.Normal), Font(Res.font.inter_semibold, FontWeight.SemiBold))

@Suppress("CompositionLocalAllowlist")
val LocalIosColors = staticCompositionLocalOf { LightIosColors }

@Suppress("CompositionLocalAllowlist")
val LocalIosTypography = staticCompositionLocalOf { iosTypography(FontFamily.Default) }

/** `Ios.colors.tint`, `Ios.type.headline`, … — the iOS look, read from the theme. */
object Ios {
    val colors: IosColors
        @Composable @ReadOnlyComposable
        get() = LocalIosColors.current

    val type: IosTypography
        @Composable @ReadOnlyComposable
        get() = LocalIosTypography.current
}
