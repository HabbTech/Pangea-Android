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
package org.meshtastic.feature.settings.radio.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.MetricFormatter
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.close
import org.meshtastic.core.resources.config_saved_restarting
import org.meshtastic.core.resources.delivery_confirmed
import org.meshtastic.core.resources.delivery_confirmed_reboot_warning
import org.meshtastic.core.resources.error
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.feature.settings.radio.RebootBehavior
import org.meshtastic.feature.settings.radio.ResponseState

private const val AUTO_DISMISS_DELAY_MS = 1500L

@Composable
fun <T> PacketResponseStateDialog(
    state: ResponseState<T>,
    onDismiss: () -> Unit = {},
    onComplete: () -> Unit = {},
    onBack: () -> Unit = {},
    rebootBehavior: RebootBehavior = RebootBehavior.MAY_RESTART,
) {
    LaunchedEffect(state) {
        if (state is ResponseState.Success) {
            delay(AUTO_DISMISS_DELAY_MS)
            onDismiss()
            onBack()
        }
    }

    MeshtasticDialog(
        onDismiss = if (state is ResponseState.Loading) onDismiss else null,
        title = null,
        icon = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (state) {
                    is ResponseState.Loading -> {
                        LoadingContent(state = state, onComplete = onComplete)
                    }

                    is ResponseState.Success -> {
                        SuccessContent(rebootBehavior)
                    }

                    is ResponseState.Error -> {
                        ErrorContent(state = state)
                    }

                    ResponseState.Empty -> {}
                }
            }
        },
        dismissable = false,
        onConfirm =
        if (state !is ResponseState.Loading) {
            {
                onDismiss()
                onBack()
            }
        } else {
            null
        },
        confirmText = stringResource(Res.string.close),
        dismissText = if (state is ResponseState.Loading) stringResource(Res.string.cancel) else null,
    )
}

@Composable
@Suppress("MagicNumber")
private fun LoadingContent(state: ResponseState.Loading, onComplete: () -> Unit) {
    val clampedProgress = (state.completed.toFloat() / state.total.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val progress by animateFloatAsState(targetValue = clampedProgress, label = "progress")
    val c = Ios.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = MetricFormatter.percent(progress * 100f, decimalPlaces = 0),
            style = Ios.type.title2,
            color = c.secondaryLabel,
        )
        // iOS's ProgressView bar: a thin grey track filled with the tint.
        Box(Modifier.fillMaxWidth().padding(top = 16.dp).height(4.dp).clip(CircleShape).background(c.secondaryFill)) {
            Box(Modifier.fillMaxWidth(progress).height(4.dp).clip(CircleShape).background(c.tint))
        }
        state.status?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = 12.dp),
                style = Ios.type.footnote,
                color = c.label,
                textAlign = TextAlign.Center,
            )
        }
    }
    if (state.completed >= state.total) onComplete()
}

/** An iOS alert's body: a small green tick, the headline, and what happens next in footnote type. */
@Composable
private fun SuccessContent(rebootBehavior: RebootBehavior) {
    Icon(
        imageVector = IosIcons.CheckCircleFill,
        contentDescription = null,
        modifier = Modifier.size(44.dp),
        tint = Ios.colors.systemGreen,
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(Res.string.delivery_confirmed),
            style = Ios.type.headline,
            color = Ios.colors.label,
            textAlign = TextAlign.Center,
        )
        // The firmware applies most config sections with a node reboot; say what is actually happening for the
        // section that was saved instead of a one-size-fits-all warning (and say nothing when nothing will happen).
        val rebootNotice =
            when (rebootBehavior) {
                RebootBehavior.ALWAYS -> stringResource(Res.string.config_saved_restarting)
                RebootBehavior.MAY_RESTART -> stringResource(Res.string.delivery_confirmed_reboot_warning)
                RebootBehavior.NEVER -> null
            }
        rebootNotice?.let {
            Text(text = it, style = Ios.type.footnote, textAlign = TextAlign.Center, color = Ios.colors.label)
        }
    }
}

/** An iOS alert's body for a failure: "Error" as the headline and the reason under it, no symbol. */
@Composable
private fun ErrorContent(state: ResponseState.Error) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(Res.string.error),
            style = Ios.type.headline,
            textAlign = TextAlign.Center,
            color = Ios.colors.label,
        )
        Text(
            text = "${state.error.asString()}.",
            style = Ios.type.footnote,
            textAlign = TextAlign.Center,
            color = Ios.colors.label,
        )
    }
}
