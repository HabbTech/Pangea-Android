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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/** iOS 26 sheet corners. */
private val SheetCorner = 38.dp

/**
 * An iOS sheet at its large detent: rounded top, no drag handle, the [IosSheetHeader] (**Cancel** at the left when
 * [cancelLabel] is set, the [title], an optional emphasized [actionLabel] at the right), then [content]. [grouped]
 * gives the grey background used when the content is white cards (Profile); otherwise the sheet is white. A smaller
 * [heightFraction] is a sheet at iOS's medium detent; [header] replaces the standard one; [grabber] adds the drag
 * capsule.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IosSheet(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    cancelLabel: String? = null,
    actionLabel: String? = null,
    actionEnabled: Boolean = true,
    onAction: () -> Unit = {},
    grouped: Boolean = false,
    heightFraction: Float = 0.93f,
    header: (@Composable () -> Unit)? = null,
    // The grey capsule iOS draws at the top of a sheet that can be dragged between detents (the map's sheets).
    grabber: Boolean = false,
    // False for a sheet that must be answered (lockdown): swiping, Back and taps outside leave it open.
    dismissable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    // In Dark Mode a sheet is drawn in iOS's elevated greys, for its own background and everything on it.
    val c = Ios.colors.elevated()
    CompositionLocalProvider(LocalIosColors provides c) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState =
            rememberBottomSheetState(
                initialValue = SheetValue.Hidden,
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
                confirmValueChange = { dismissable || it != SheetValue.Hidden },
            ),
            properties =
            ModalBottomSheetProperties(
                shouldDismissOnBackPress = dismissable,
                shouldDismissOnClickOutside = dismissable,
            ),
            shape = RoundedCornerShape(topStart = SheetCorner, topEnd = SheetCorner),
            containerColor = if (grouped) c.groupedBackground else c.background,
            dragHandle = null,
            contentWindowInsets = { WindowInsets(0) },
        ) {
            Column(modifier.fillMaxWidth().fillMaxHeight(heightFraction).navigationBarsPadding().imePadding()) {
                if (grabber) {
                    Box(
                        Modifier.padding(top = 5.dp)
                            .align(Alignment.CenterHorizontally)
                            .size(width = 36.dp, height = 5.dp)
                            .clip(CircleShape)
                            .background(c.tertiaryLabel),
                    )
                }
                if (header != null) {
                    header()
                } else {
                    IosSheetHeader(
                        title = title,
                        cancelLabel = cancelLabel,
                        onCancel = onDismissRequest,
                        actionLabel = actionLabel,
                        actionEnabled = actionEnabled,
                        onAction = onAction,
                    )
                }
                Spacer(Modifier.height(12.dp))
                content()
            }
        }
    }
}

/**
 * A sheet that sizes to its content, drawn as iOS draws one: the grey grabber at the top, 38 dp corners, and iOS's
 * (elevated, in Dark Mode) background. A drop-in for Material's `ModalBottomSheet` where the content brings its own
 * heading; [IosSheet] is the one with iOS's Cancel / title / action bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IosBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    grouped: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Ios.colors.elevated()
    CompositionLocalProvider(LocalIosColors provides c) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = SheetCorner, topEnd = SheetCorner),
            containerColor = if (grouped) c.groupedBackground else c.background,
            contentColor = c.label,
            dragHandle = {
                Box(
                    Modifier.padding(top = 5.dp, bottom = 10.dp)
                        .size(width = 36.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(c.tertiaryLabel),
                )
            },
            content = content,
        )
    }
}
