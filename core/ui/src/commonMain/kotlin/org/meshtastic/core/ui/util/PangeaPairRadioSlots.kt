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
package org.meshtastic.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The Pair Radio sheet (Pangea-Apple PairRadioSheet): pairing outside first-run setup, then the name page if the radio
 * still needs a name or a region. The app provides it, since it joins the connections and setup features; null where
 * nobody does.
 */
@Suppress("CompositionLocalAllowlist")
val LocalPairRadioSheet = staticCompositionLocalOf<(@Composable (onDismiss: () -> Unit) -> Unit)?> { null }

/**
 * Opens the Pair Radio sheet, as the status pill's **Pair Radio** and Settings' **Pair a Different Radio** do on iOS.
 * The app shell provides it; null outside the shell.
 */
@Suppress("CompositionLocalAllowlist")
val LocalOpenPairRadio = staticCompositionLocalOf<(() -> Unit)?> { null }
