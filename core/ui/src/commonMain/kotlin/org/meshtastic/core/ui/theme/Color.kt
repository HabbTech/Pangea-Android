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

import androidx.compose.ui.graphics.Color

// ─── Meshtastic Design Standards v1.3 ───
// Primary: Pangea Fossil #F05511 in light and dark, with white text, matching the Pangea iPhone app
// Surfaces, text and separators: iOS system colours (systemGroupedBackground, secondaryLabel, separator...)
// Secondary: iOS systemGray #8E8E93
// Tertiary: Blue 700 #2855A8 (the Expert Mode caption, as on iOS)
// See: standards/meshtastic_design_standards_v1_3.md §8

// ─── Light Scheme (§8.2) ───
// PANGEA BRAND v2: Fossil tint as on iOS (Ben, 2026-10-18) — intentional divergence from Meshtastic
// green; see design/PANGEA-BRAND.md; canary: PangeaPaletteTest
val primaryLight = Color(0xFFF05511)
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = Color(0xFFFFDBCB)
val onPrimaryContainerLight = Color(0xFF3B0900)
val secondaryLight = Color(0xFF8E8E93)
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = Color(0xFFE5E5EA)
val onSecondaryContainerLight = Color(0xFF000000)
val tertiaryLight = Color(0xFF2855A8) // Blue 700
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = Color(0xFFE8EAF6) // Blue 50
val onTertiaryContainerLight = Color(0xFF001849) // Blue 950
val errorLight = Color(0xFFFF3B30)
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFFFE5E3)
val onErrorContainerLight = Color(0xFF410002)
val backgroundLight = Color(0xFFF2F2F7)
val onBackgroundLight = Color(0xFF000000)
val surfaceLight = Color(0xFFF2F2F7)
val onSurfaceLight = Color(0xFF000000)
val surfaceVariantLight = Color(0xFFE5E5EA)
val onSurfaceVariantLight = Color(0xFF8A8A8E)
val outlineLight = Color(0xFFC6C6C8)
val outlineVariantLight = Color(0xFFC6C6C8)
val scrimLight = Color(0xFF000000)
val inverseSurfaceLight = Color(0xFF1C1C1E)
val inverseOnSurfaceLight = Color(0xFFFFFFFF)
val inversePrimaryLight = Color(0xFFF05511)
val surfaceDimLight = Color(0xFFE5E5EA)
val surfaceBrightLight = Color(0xFFFFFFFF)
val surfaceContainerLowestLight = Color(0xFFFFFFFF)
val surfaceContainerLowLight = Color(0xFFFFFFFF)
val surfaceContainerLight = Color(0xFFFFFFFF)
val surfaceContainerHighLight = Color(0xFFFFFFFF)
val surfaceContainerHighestLight = Color(0xFFFFFFFF)

// ─── Dark Scheme (§8.3) ───
// PANGEA BRAND v2: Fossil tint as on iOS — see design/PANGEA-BRAND.md; canary: PangeaPaletteTest
val primaryDark = Color(0xFFF05511)
val onPrimaryDark = Color(0xFFFFFFFF)
val primaryContainerDark = Color(0xFF7A2E04)
val onPrimaryContainerDark = Color(0xFFFFDBCB)
val secondaryDark = Color(0xFF8E8E93)
val onSecondaryDark = Color(0xFF000000)
val secondaryContainerDark = Color(0xFF2C2C2E)
val onSecondaryContainerDark = Color(0xFFFFFFFF)
val tertiaryDark = Color(0xFFB0BFF0) // Blue 300
val onTertiaryDark = Color(0xFF001849) // Blue 950
val tertiaryContainerDark = Color(0xFF2855A8) // Blue 700
val onTertiaryContainerDark = Color(0xFFE8EAF6) // Blue 50
val errorDark = Color(0xFFFF453A)
val onErrorDark = Color(0xFFFFFFFF)
val errorContainerDark = Color(0xFF3B0B08)
val onErrorContainerDark = Color(0xFFFFDAD6)
val backgroundDark = Color(0xFF000000)
val onBackgroundDark = Color(0xFFFFFFFF)
val surfaceDark = Color(0xFF000000)
val onSurfaceDark = Color(0xFFFFFFFF)
val surfaceVariantDark = Color(0xFF2C2C2E)
val onSurfaceVariantDark = Color(0xFF8D8D93)
val outlineDark = Color(0xFF38383A)
val outlineVariantDark = Color(0xFF38383A)
val scrimDark = Color(0xFF000000)
val inverseSurfaceDark = Color(0xFFF2F2F7)
val inverseOnSurfaceDark = Color(0xFF000000)
val inversePrimaryDark = Color(0xFFF05511)
val surfaceDimDark = Color(0xFF000000)
val surfaceBrightDark = Color(0xFF2C2C2E)
val surfaceContainerLowestDark = Color(0xFF000000)
val surfaceContainerLowDark = Color(0xFF1C1C1E)
val surfaceContainerDark = Color(0xFF1C1C1E)
val surfaceContainerHighDark = Color(0xFF1C1C1E)
val surfaceContainerHighestDark = Color(0xFF1C1C1E)
