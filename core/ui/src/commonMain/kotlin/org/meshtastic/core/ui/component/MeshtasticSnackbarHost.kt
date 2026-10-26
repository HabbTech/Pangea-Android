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
package org.meshtastic.core.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.close
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.util.SnackbarManager

/**
 * Shared composable that observes [SnackbarManager.events] and provides a global [SnackbarHostState].
 *
 * It renders a [SnackbarHost] using the provided [hostModifier] over the provided [content].
 */
@Composable
fun MeshtasticSnackbarProvider(
    snackbarManager: SnackbarManager,
    modifier: Modifier = Modifier,
    hostModifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarManager) {
        snackbarManager.events.collect { event ->
            val result =
                snackbarHostState.showSnackbar(
                    message = event.message,
                    actionLabel = event.actionLabel,
                    withDismissAction = event.withDismissAction,
                    duration = event.duration,
                )
            if (result == SnackbarResult.ActionPerformed) {
                event.onAction?.invoke()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        content()
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).safeDrawingPadding().then(hostModifier),
        ) { data ->
            IosSnackbar(data)
        }
    }
}

/**
 * A snackbar in the iPhone app's banner style (its photo-update banner): a rounded material card, the message in
 * subheadline type and the action in the tint.
 */
@Composable
private fun IosSnackbar(data: SnackbarData) {
    val c = Ios.colors
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = c.chrome.copy(alpha = 1f),
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp).heightIn(min = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = data.visuals.message,
                style = Ios.type.subheadline,
                color = c.label,
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
            )
            data.visuals.actionLabel?.let { label ->
                Text(
                    text = label,
                    style = Ios.type.subheadline.copy(fontWeight = FontWeight.SemiBold),
                    color = c.tint,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { data.performAction() }.padding(8.dp),
                )
            }
            if (data.visuals.withDismissAction) {
                Icon(
                    IosIcons.Xmark,
                    contentDescription = stringResource(Res.string.close),
                    tint = c.secondaryLabel,
                    modifier =
                    Modifier.clip(RoundedCornerShape(8.dp)).clickable { data.dismiss() }.padding(8.dp).size(16.dp),
                )
            }
        }
    }
}
