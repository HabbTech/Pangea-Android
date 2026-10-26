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
package org.meshtastic.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.meshtastic.core.ui.ios.IosTypography

/**
 * Upstream's Material typescale. Pangea no longer themes with it (see [materialTypography]); it stays for the
 * event-font path and for merges.
 *
 * `bodyLarge` and `bodyMedium` enforce a minimum of 16.sp per design standards §5.
 */
val AppTypography =
    Typography(
        bodyLarge =
        TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
        ),
        bodyMedium =
        TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.25.sp,
        ),
    )

/**
 * Pangea (design/PANGEA-BRAND.md v2): the Material roles mapped onto the iOS text styles, so any screen still drawn
 * with Material components gets the iPhone app's type: Inter at iOS sizes (34 large title, 17 body, 13 footnote).
 */
fun materialTypography(ios: IosTypography): Typography = Typography(
    displayLarge = ios.largeTitle.copy(fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = ios.largeTitle.copy(fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = ios.largeTitle.copy(fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = ios.largeTitle,
    headlineMedium = ios.title1,
    headlineSmall = ios.title2,
    titleLarge = ios.title3,
    titleMedium = ios.headline,
    titleSmall = ios.subheadline.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = ios.body,
    bodyMedium = ios.callout,
    bodySmall = ios.footnote,
    labelLarge = ios.body,
    labelMedium = ios.footnote,
    labelSmall = ios.caption2,
)
