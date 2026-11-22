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
package org.meshtastic.feature.settings.pangea

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.CommonUri
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.util.getSharedContactUrl
import org.meshtastic.core.model.util.toSharedContact
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.okay
import org.meshtastic.core.resources.pangea_nfc_node_name
import org.meshtastic.core.resources.pangea_nfc_not_pangea
import org.meshtastic.core.resources.pangea_nfc_read_failed
import org.meshtastic.core.resources.pangea_nfc_scan_tag
import org.meshtastic.core.resources.pangea_nfc_scan_tag_hint
import org.meshtastic.core.resources.pangea_nfc_tags
import org.meshtastic.core.resources.pangea_nfc_write_contact
import org.meshtastic.core.resources.pangea_nfc_write_contact_hint
import org.meshtastic.core.resources.pangea_tools
import org.meshtastic.core.resources.scan_nfc
import org.meshtastic.core.resources.scan_nfc_text
import org.meshtastic.core.resources.write_nfc
import org.meshtastic.core.resources.write_nfc_failed
import org.meshtastic.core.resources.write_nfc_success
import org.meshtastic.core.resources.write_nfc_text
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.component.NfcDisabledDialog
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosPushedColumnScreen
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.ios.iosRowSeparator
import org.meshtastic.core.ui.util.LocalNfcScannerProvider
import org.meshtastic.core.ui.util.LocalNfcWriterProvider
import org.meshtastic.core.ui.util.parseDeepLinkOrInvalid

/**
 * The iPhone app's Tools page: its NFC Tags section. Write your own contact to a tag (anyone who taps it can add you),
 * or read a Pangea tag, whose contact or channel link opens the same import as a scanned QR code.
 *
 * Shown from Settings only where the phone has NFC, as iOS shows it only where its NFC reader is available.
 */
@Composable
fun ToolsScreen(
    ownNode: Node?,
    onHandleDeepLink: (CommonUri, onInvalid: () -> Unit) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var writing by rememberSaveable { mutableStateOf(false) }
    var scanning by rememberSaveable { mutableStateOf(false) }
    var writeSucceeded by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var nfcOff by rememberSaveable { mutableStateOf(false) }
    var readFailed by rememberSaveable { mutableStateOf(false) }

    val contactUrl = ownNode?.ownContactUrl()

    NfcFlows(
        contactUrl = contactUrl,
        writing = writing,
        scanning = scanning,
        onWriteResult = { success ->
            writing = false
            writeSucceeded = success
        },
        onScanResult = { contents ->
            scanning = false
            val url = contents?.trim()?.takeIf { it.isNotEmpty() }
            if (url == null) {
                readFailed = true
            } else {
                parseDeepLinkOrInvalid(url, onHandleDeepLink) { readFailed = true }
            }
        },
        onNfcOff = {
            writing = false
            scanning = false
            nfcOff = true
        },
    )
    ToolsDialogs(
        writing = writing,
        scanning = scanning,
        writeSucceeded = writeSucceeded,
        nfcOff = nfcOff,
        readFailed = readFailed,
        onCancel = {
            writing = false
            scanning = false
        },
        onClose = {
            writeSucceeded = null
            nfcOff = false
            readFailed = false
        },
    )

    IosPushedColumnScreen(title = stringResource(Res.string.pangea_tools), onBack = onBack, modifier = modifier) {
        IosSection(header = stringResource(Res.string.pangea_nfc_tags)) {
            if (ownNode != null) {
                IosRow(title = stringResource(Res.string.pangea_nfc_node_name, ownNode.user.long_name))
                IosRow(
                    title = stringResource(Res.string.pangea_nfc_write_contact),
                    icon = IosIcons.Tag,
                    titleColor = if (contactUrl != null) Ios.colors.tint else Ios.colors.tertiaryLabel,
                    iconTint = if (contactUrl != null) Ios.colors.tint else Ios.colors.tertiaryLabel,
                    onClick = if (contactUrl != null) ({ writing = true }) else null,
                )
                Hint(stringResource(Res.string.pangea_nfc_write_contact_hint))
            }
            IosRow(
                title = stringResource(Res.string.pangea_nfc_scan_tag),
                icon = IosIcons.WaveRightCircle,
                titleColor = Ios.colors.tint,
                onClick = { scanning = true },
            )
            Hint(stringResource(Res.string.pangea_nfc_scan_tag_hint))
        }
    }
}

/** Arms the platform's NFC writer or reader for as long as a write or a scan is under way. */
@Composable
private fun NfcFlows(
    contactUrl: String?,
    writing: Boolean,
    scanning: Boolean,
    onWriteResult: (Boolean) -> Unit,
    onScanResult: (String?) -> Unit,
    onNfcOff: () -> Unit,
) {
    val writer = LocalNfcWriterProvider.current
    val scanner = LocalNfcScannerProvider.current
    if (writing && contactUrl != null) writer(contactUrl, onWriteResult, onNfcOff)
    if (scanning) scanner(onScanResult, onNfcOff)
}

/** The alerts around a write or a scan: hold the tag, the write's result, NFC off, and an unreadable tag. */
@Composable
private fun ToolsDialogs(
    writing: Boolean,
    scanning: Boolean,
    writeSucceeded: Boolean?,
    nfcOff: Boolean,
    readFailed: Boolean,
    onCancel: () -> Unit,
    onClose: () -> Unit,
) {
    when {
        writing ->
            MeshtasticDialog(
                onDismiss = onCancel,
                titleRes = Res.string.write_nfc,
                messageRes = Res.string.write_nfc_text,
                dismissTextRes = Res.string.cancel,
            )

        scanning ->
            MeshtasticDialog(
                onDismiss = onCancel,
                titleRes = Res.string.scan_nfc,
                messageRes = Res.string.scan_nfc_text,
                dismissTextRes = Res.string.cancel,
            )

        writeSucceeded != null ->
            MeshtasticDialog(
                onDismiss = onClose,
                titleRes = Res.string.write_nfc,
                messageRes = if (writeSucceeded) Res.string.write_nfc_success else Res.string.write_nfc_failed,
                onConfirm = onClose,
                confirmTextRes = Res.string.okay,
            )

        nfcOff -> NfcDisabledDialog(titleRes = Res.string.scan_nfc, onDismiss = onClose)

        readFailed ->
            MeshtasticDialog(
                onDismiss = onClose,
                titleRes = Res.string.pangea_nfc_read_failed,
                messageRes = Res.string.pangea_nfc_not_pangea,
                onConfirm = onClose,
                confirmTextRes = Res.string.okay,
            )
    }
}

/** iOS's caption row under an action: secondary footnote text in its own row of the list. */
@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = Ios.type.caption1,
        color = Ios.colors.secondaryLabel,
        modifier = Modifier.iosRowSeparator().fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

/**
 * This node's contact link, marked verified (you hold your own radio's key), as iOS's Tools page shares it; null when
 * the contact can't be shared: no public key, or a node that takes no messages, as iOS's `canShareContact` requires.
 */
private fun Node.ownContactUrl(): String? = takeIf { user.public_key.size > 0 && user.is_unmessagable != true }
    ?.toSharedContact(isOwnContact = true)
    ?.getSharedContactUrl()
    ?.toString()
