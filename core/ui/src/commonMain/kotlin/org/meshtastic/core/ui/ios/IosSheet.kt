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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** iOS 26 sheet corners. */
private val SheetCorner = 38.dp

/**
 * An iOS sheet at its large detent: rounded top, no drag handle, the [IosSheetHeader] (**Cancel** at the left when
 * [cancelLabel] is set, the [title], an optional emphasized [actionLabel] at the right), then [content]. [grouped]
 * gives the grey background used when the content is white cards (Profile); otherwise the sheet is white.
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
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Ios.colors
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState =
        rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
        shape = RoundedCornerShape(topStart = SheetCorner, topEnd = SheetCorner),
        containerColor = if (grouped) c.groupedBackground else c.background,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0) },
    ) {
        Column(modifier.fillMaxWidth().fillMaxHeight(0.94f).navigationBarsPadding().imePadding()) {
            IosSheetHeader(
                title = title,
                cancelLabel = cancelLabel,
                onCancel = onDismissRequest,
                actionLabel = actionLabel,
                actionEnabled = actionEnabled,
                onAction = onAction,
            )
            content()
        }
    }
}
