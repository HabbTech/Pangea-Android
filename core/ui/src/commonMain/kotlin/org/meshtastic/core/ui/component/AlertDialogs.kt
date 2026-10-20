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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.okay
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosButton
import org.meshtastic.core.ui.ios.IosButtonStyle
import org.meshtastic.core.ui.ios.LocalIosColors
import org.meshtastic.core.ui.ios.elevated
import org.meshtastic.core.ui.util.annotatedStringFromHtml

/** An iOS 26 alert's corners, and how wide it gets. */
private val AlertCorner = 32.dp
private val AlertMaxWidth = 300.dp
private val AlertMaxHeight = 600.dp

/**
 * A comprehensive and flexible dialog component for the Meshtastic application, drawn as an iOS alert: a rounded panel
 * with the title and message centred, and its actions as capsules (Cancel in grey, the action in the tint).
 *
 * @param modifier Modifier for the dialog.
 * @param title The title text of the dialog.
 * @param titleRes The title string resource of the dialog.
 * @param message Optional plain text message.
 * @param messageRes Optional string resource message.
 * @param messageColor Optional color for the plain text message; [Color.Unspecified] uses the default text color.
 * @param html Optional HTML formatted message.
 * @param icon Optional leading icon.
 * @param text Optional custom composable content for the body. Scrolling long content is the caller's job.
 * @param confirmText Text for the confirmation button.
 * @param confirmTextRes String resource for the confirmation button.
 * @param onConfirm Callback for the confirmation button.
 * @param dismissText Text for the dismiss button.
 * @param dismissTextRes String resource for the dismiss button.
 * @param onDismiss Callback for when the dialog is dismissed or the dismiss button is clicked.
 * @param choices If provided, displays a list of buttons instead of the standard confirm/dismiss actions.
 * @param dismissable Whether the dialog can be dismissed by clicking outside or pressing back.
 * @param confirmDestructive Draws the confirm action as a destructive one (red), as iOS's `role: .destructive`.
 * @param confirmEnabled Whether the confirm action can be pressed yet.
 * @param onDismissRequest What a tap outside or Back does, when that differs from [onDismiss] (an alert whose grey
 *   button toggles its content rather than closing it).
 */
@Composable
@Suppress("LongMethod", "CyclomaticComplexMethod")
fun MeshtasticDialog(
    modifier: Modifier = Modifier,
    title: String? = null,
    titleRes: StringResource? = null,
    message: String? = null,
    messageRes: StringResource? = null,
    messageColor: Color = Color.Unspecified,
    html: String? = null,
    icon: ImageVector? = null,
    text: @Composable (() -> Unit)? = null,
    confirmText: String? = null,
    confirmTextRes: StringResource? = null,
    onConfirm: (() -> Unit)? = null,
    dismissText: String? = null,
    dismissTextRes: StringResource? = null,
    onDismiss: (() -> Unit)? = null,
    choices: Map<String, () -> Unit> = emptyMap(),
    dismissable: Boolean = true,
    confirmDestructive: Boolean = false,
    confirmEnabled: Boolean = true,
    onDismissRequest: (() -> Unit)? = null,
) {
    val titleText = title ?: titleRes?.let { stringResource(it) } ?: ""
    val messageText = message ?: messageRes?.let { stringResource(it) }
    val confirmButtonText = confirmText ?: confirmTextRes?.let { stringResource(it) }
    val dismissButtonText = dismissText ?: dismissTextRes?.let { stringResource(it) }
    // An alert floats over the screen, so in Dark Mode it takes iOS's elevated greys, as sheets do.
    val c = Ios.colors.elevated()

    val htmlAnnotated =
        html?.let {
            annotatedStringFromHtml(
                it,
                linkStyles =
                TextLinkStyles(style = SpanStyle(textDecoration = TextDecoration.Underline, color = c.tint)),
            )
        }

    Dialog(
        onDismissRequest = { if (dismissable) (onDismissRequest ?: onDismiss)?.invoke() },
        properties = DialogProperties(dismissOnBackPress = dismissable, dismissOnClickOutside = dismissable),
    ) {
        CompositionLocalProvider(LocalIosColors provides c) {
            Column(
                modifier =
                modifier
                    .widthIn(max = AlertMaxWidth)
                    .heightIn(max = AlertMaxHeight)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AlertCorner))
                    .background(if (c.isDark) c.groupedRow else Color.White)
                    .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                icon?.let { Icon(it, contentDescription = null, tint = c.tint, modifier = Modifier.size(28.dp)) }
                if (titleText.isNotEmpty()) {
                    Text(
                        text = titleText,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = Ios.type.headline,
                        color = c.label,
                    )
                }
                // The body takes what room is left, so a long one scrolls (its own scroll) rather than pushing the
                // buttons off the alert.
                Box(Modifier.weight(1f, fill = false).fillMaxWidth()) {
                    Column(
                        modifier =
                        if (choices.isNotEmpty()) Modifier.verticalScroll(rememberScrollState()) else Modifier,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        when {
                            text != null -> text()

                            htmlAnnotated != null ->
                                Text(
                                    text = htmlAnnotated,
                                    style = Ios.type.subheadline,
                                    color = c.label,
                                    textAlign = TextAlign.Center,
                                )

                            messageText != null ->
                                Text(
                                    text = messageText,
                                    style = Ios.type.subheadline,
                                    color = if (messageColor == Color.Unspecified) c.label else messageColor,
                                    textAlign = TextAlign.Center,
                                )
                        }
                        if (choices.isNotEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                choices.forEach { (choice, action) ->
                                    IosButton(
                                        text = choice,
                                        onClick = {
                                            action()
                                            onDismiss?.invoke()
                                        },
                                        style = IosButtonStyle.Gray,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }
                }
                if (choices.isEmpty() && (onDismiss != null || onConfirm != null)) {
                    AlertActions(
                        dismissLabel = onDismiss?.let { dismissButtonText ?: stringResource(Res.string.cancel) },
                        onDismiss = onDismiss,
                        confirmLabel = onConfirm?.let { confirmButtonText ?: stringResource(Res.string.okay) },
                        onConfirm = onConfirm,
                        confirmDestructive = confirmDestructive,
                        confirmEnabled = confirmEnabled,
                    )
                }
            }
        }
    }
}

/**
 * An alert's actions as iOS 26 draws them: capsules side by side (Cancel in grey, the action in the tint, or red when
 * it destroys something), stacked with the action on top when the words are too long to sit side by side.
 */
@Suppress("LongParameterList")
@Composable
private fun AlertActions(
    dismissLabel: String?,
    onDismiss: (() -> Unit)?,
    confirmLabel: String?,
    onConfirm: (() -> Unit)?,
    confirmDestructive: Boolean,
    confirmEnabled: Boolean,
) {
    val stacked = (dismissLabel?.length ?: 0) + (confirmLabel?.length ?: 0) > STACK_AT_CHARS
    val confirm: @Composable (Modifier) -> Unit = { mod ->
        if (onConfirm != null && confirmLabel != null) {
            IosButton(
                text = confirmLabel,
                onClick = onConfirm,
                style = if (confirmDestructive) IosButtonStyle.Destructive else IosButtonStyle.Filled,
                enabled = confirmEnabled,
                textStyle = Ios.type.body.copy(fontWeight = FontWeight.SemiBold),
                modifier = mod,
            )
        }
    }
    val dismiss: @Composable (Modifier) -> Unit = { mod ->
        if (onDismiss != null && dismissLabel != null) {
            IosButton(
                text = dismissLabel,
                onClick = onDismiss,
                style = IosButtonStyle.Gray,
                textStyle = Ios.type.body.copy(fontWeight = FontWeight.Medium),
                modifier = mod,
            )
        }
    }
    if (stacked) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            confirm(Modifier.fillMaxWidth())
            dismiss(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            dismiss(Modifier.weight(1f))
            confirm(Modifier.weight(1f))
        }
    }
}

/** Two labels longer than this together don't fit side by side in an alert's width, so iOS stacks them. */
private const val STACK_AT_CHARS = 20

/** A simplified [MeshtasticDialog] using only string resources. */
@Composable
fun MeshtasticResourceDialog(
    modifier: Modifier = Modifier,
    titleRes: StringResource,
    messageRes: StringResource,
    confirmTextRes: StringResource? = null,
    dismissTextRes: StringResource? = null,
    onConfirm: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    dismissable: Boolean = true,
) {
    MeshtasticDialog(
        modifier = modifier,
        titleRes = titleRes,
        messageRes = messageRes,
        confirmTextRes = confirmTextRes,
        dismissTextRes = dismissTextRes,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        dismissable = dismissable,
    )
}

/** A simplified [MeshtasticDialog] using a title resource and a plain text message. */
@Composable
fun MeshtasticTextDialog(
    modifier: Modifier = Modifier,
    titleRes: StringResource,
    message: String,
    confirmTextRes: StringResource? = null,
    dismissTextRes: StringResource? = null,
    onConfirm: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    dismissable: Boolean = true,
) {
    MeshtasticDialog(
        modifier = modifier,
        titleRes = titleRes,
        message = message,
        confirmTextRes = confirmTextRes,
        dismissTextRes = dismissTextRes,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        dismissable = dismissable,
    )
}
