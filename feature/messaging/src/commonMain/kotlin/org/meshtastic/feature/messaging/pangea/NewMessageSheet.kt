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
package org.meshtastic.feature.messaging.pangea

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.pangea.PangeaCode
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.pangea_code_placeholder
import org.meshtastic.core.resources.pangea_contacts
import org.meshtastic.core.resources.pangea_contacts_empty
import org.meshtastic.core.resources.pangea_copy_code
import org.meshtastic.core.resources.pangea_error_invalid
import org.meshtastic.core.resources.pangea_error_not_connected
import org.meshtastic.core.resources.pangea_error_own_code
import org.meshtastic.core.resources.pangea_error_send_failed
import org.meshtastic.core.resources.pangea_message_someone_title
import org.meshtastic.core.resources.pangea_new_message
import org.meshtastic.core.resources.pangea_reaching
import org.meshtastic.core.resources.pangea_scan_qr
import org.meshtastic.core.resources.pangea_share
import org.meshtastic.core.resources.pangea_show_qr
import org.meshtastic.core.resources.pangea_start_chat
import org.meshtastic.core.resources.pangea_unreachable
import org.meshtastic.core.resources.pangea_your_code
import org.meshtastic.core.ui.component.QrDialog
import org.meshtastic.core.ui.icon.QrCode
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosActivityIndicator
import org.meshtastic.core.ui.ios.IosButton
import org.meshtastic.core.ui.ios.IosButtonStyle
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosInset
import org.meshtastic.core.ui.ios.IosSheet
import org.meshtastic.core.ui.ios.IosTextField
import org.meshtastic.core.ui.ios.iosPressable
import org.meshtastic.core.ui.util.LocalBarcodeScannerProvider
import org.meshtastic.core.ui.util.LocalBarcodeScannerSupported
import org.meshtastic.core.ui.util.createClipEntry
import org.meshtastic.core.ui.util.rememberShareText

/**
 * New Message, as on the Pangea iPhone app: "Message someone" with the @code field (scan a QR code from its button, or
 * paste a code or link into it) and **Start Chat**; your saved contacts; then your own code with **Share** and **Show
 * QR**, so it's one tap to give it to a friend.
 */
@Suppress("ModifierMissing")
@Composable
fun NewMessageSheet(viewModel: PangeaChatsViewModel, onOpenChat: (String) -> Unit, onDismiss: () -> Unit) {
    val resolve by viewModel.resolve.collectAsStateWithLifecycle()
    val people by viewModel.people.collectAsStateWithLifecycle()
    val myCode by viewModel.myCode.collectAsStateWithLifecycle()

    val openChat by rememberUpdatedState(onOpenChat)
    LaunchedEffect(resolve) {
        (resolve as? ResolveState.Ready)?.let {
            viewModel.cancelResolve()
            openChat(it.contactKey)
        }
    }

    IosSheet(
        title = stringResource(Res.string.pangea_new_message),
        cancelLabel = stringResource(Res.string.cancel),
        onDismissRequest = {
            viewModel.cancelResolve()
            onDismiss()
        },
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = IosInset),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CodeEntry(resolve = resolve, onSubmit = viewModel::resolve)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(Res.string.pangea_contacts), style = Ios.type.headline, color = Ios.colors.label)
            if (people.isEmpty()) {
                Text(
                    stringResource(Res.string.pangea_contacts_empty),
                    style = Ios.type.footnote,
                    color = Ios.colors.secondaryLabel,
                )
            } else {
                people.forEach { person ->
                    Row(
                        Modifier.fillMaxWidth()
                            .iosPressable(enabled = person.canMessage) {
                                onOpenChat(viewModel.dmContactKey(person.num))
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(person.name, style = Ios.type.body, color = Ios.colors.label)
                            Text(
                                person.code,
                                style = Ios.type.footnote,
                                fontFamily = FontFamily.Monospace,
                                color = Ios.colors.secondaryLabel,
                            )
                        }
                    }
                }
            }
            myCode?.let { (code, link) -> MyCodeCard(code, link, modifier = Modifier.padding(top = 8.dp)) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CodeEntry(resolve: ResolveState, onSubmit: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val scanner = LocalBarcodeScannerProvider.current { contents -> contents?.let { onSubmit(it) } }
    val canScan = LocalBarcodeScannerSupported.current
    val c = Ios.colors

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(Res.string.pangea_message_someone_title), style = Ios.type.headline, color = c.label)
        IosTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = stringResource(Res.string.pangea_code_placeholder),
            textStyle = Ios.type.body.copy(fontFamily = FontFamily.Monospace, fontSize = 19.sp),
            keyboardOptions =
            KeyboardOptions(capitalization = KeyboardCapitalization.Characters, keyboardType = KeyboardType.Ascii),
            trailing =
            if (canScan) {
                {
                    Icon(
                        IosIcons.QrViewfinder,
                        contentDescription = stringResource(Res.string.pangea_scan_qr),
                        tint = c.tint,
                        modifier = Modifier.size(26.dp).iosPressable(onClick = scanner::startScan),
                    )
                }
            } else {
                null
            },
        )
        IosButton(
            text = stringResource(Res.string.pangea_start_chat),
            onClick = { onSubmit(text) },
            enabled = text.isNotBlank() && resolve !is ResolveState.Reaching,
            large = true,
        )
        ResolveStatus(resolve, invalidVisible = PangeaCode.looksComplete(text))
    }
}

@Composable
private fun ResolveStatus(resolve: ResolveState, invalidVisible: Boolean) {
    val c = Ios.colors
    when (resolve) {
        is ResolveState.Reaching ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IosActivityIndicator(size = 16.dp)
                Text(
                    stringResource(Res.string.pangea_reaching, resolve.code),
                    style = Ios.type.footnote,
                    color = c.secondaryLabel,
                )
            }

        is ResolveState.Unreachable ->
            Text(
                stringResource(Res.string.pangea_unreachable, resolve.code),
                style = Ios.type.footnote,
                color = c.systemRed,
            )

        is ResolveState.Failed ->
            if (resolve.reason != ResolveFailure.INVALID || invalidVisible) {
                Text(stringResource(resolve.reason.message), style = Ios.type.footnote, color = c.systemRed)
            }

        ResolveState.Idle,
        is ResolveState.Ready,
        -> Unit
    }
}

private val ResolveFailure.message: StringResource
    get() =
        when (this) {
            ResolveFailure.NOT_CONNECTED -> Res.string.pangea_error_not_connected
            ResolveFailure.OWN_CODE -> Res.string.pangea_error_own_code
            ResolveFailure.SEND_FAILED -> Res.string.pangea_error_send_failed
            ResolveFailure.INVALID -> Res.string.pangea_error_invalid
        }

/**
 * "Your code": my @code in large monospaced type with a copy button, then **Share** (filled) and **Show QR** (soft).
 * The QR code carries my name and key, so scanning it connects at once.
 */
@Composable
fun MyCodeCard(code: String, link: String, modifier: Modifier = Modifier) {
    var showQr by remember { mutableStateOf(false) }
    val share = rememberShareText()
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val title = stringResource(Res.string.pangea_your_code)
    val c = Ios.colors
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.fieldFill).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, style = Ios.type.subheadline, color = c.secondaryLabel)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                code,
                style = Ios.type.title1.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold),
                color = c.label,
                modifier = Modifier.weight(1f),
            )
            Icon(
                IosIcons.DocOnDoc,
                contentDescription = stringResource(Res.string.pangea_copy_code),
                tint = c.tint,
                modifier =
                Modifier.size(24.dp).iosPressable {
                    scope.launch { clipboard.setClipEntry(createClipEntry(code, title)) }
                },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IosButton(
                text = stringResource(Res.string.pangea_share),
                onClick = { share("$code\n$link", title) },
                icon = IosIcons.ShareUp,
                modifier = Modifier.weight(1f).height(48.dp),
            )
            IosButton(
                text = stringResource(Res.string.pangea_show_qr),
                onClick = { showQr = true },
                style = IosButtonStyle.Tinted,
                icon = IosIcons.QrCode,
                modifier = Modifier.weight(1f).height(48.dp),
            )
        }
    }
    if (showQr) QrDialog(title = title, uriString = link, onDismiss = { showQr = false }, subtitle = code)
}
