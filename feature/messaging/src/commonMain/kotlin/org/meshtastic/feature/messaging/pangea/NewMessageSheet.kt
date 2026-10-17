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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
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
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.pangea.PangeaCode
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.pangea_contacts
import org.meshtastic.core.resources.pangea_error_invalid
import org.meshtastic.core.resources.pangea_error_not_connected
import org.meshtastic.core.resources.pangea_error_own_code
import org.meshtastic.core.resources.pangea_error_send_failed
import org.meshtastic.core.resources.pangea_new_group
import org.meshtastic.core.resources.pangea_new_message
import org.meshtastic.core.resources.pangea_new_message_hint
import org.meshtastic.core.resources.pangea_open_chat
import org.meshtastic.core.resources.pangea_paste
import org.meshtastic.core.resources.pangea_reaching
import org.meshtastic.core.resources.pangea_show_qr
import org.meshtastic.core.resources.pangea_unreachable
import org.meshtastic.core.resources.pangea_your_code
import org.meshtastic.core.resources.pangea_your_code_subtitle
import org.meshtastic.core.ui.component.QrDialog
import org.meshtastic.core.ui.icon.Group
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.QrCode
import org.meshtastic.core.ui.icon.QrCodeScanner
import org.meshtastic.core.ui.icon.Share
import org.meshtastic.core.ui.util.LocalBarcodeScannerProvider
import org.meshtastic.core.ui.util.LocalBarcodeScannerSupported
import org.meshtastic.core.ui.util.readText
import org.meshtastic.core.ui.util.rememberShareText

/**
 * Start a private chat by @code, link or QR code, or pick someone you already know (Pangea-Apple NewMessageSheet). Your
 * own code sits at the bottom with Share and QR, so it's one tap to give it to a friend.
 */
@Suppress("ViewModelForwarding")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMessageSheet(
    viewModel: PangeaChatsViewModel,
    onOpenChat: (String) -> Unit,
    onNewGroup: () -> Unit,
    onDismiss: () -> Unit,
) {
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

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.cancelResolve()
            onDismiss()
        },
        sheetState =
        rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
    ) {
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            item { CodeEntry(resolve = resolve, onSubmit = viewModel::resolve) }
            item {
                TextButton(onClick = onNewGroup, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(MeshtasticIcons.Group, contentDescription = null)
                    Text(stringResource(Res.string.pangea_new_group), modifier = Modifier.padding(start = 8.dp))
                }
            }
            if (people.isNotEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.pangea_contacts),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                    )
                }
                items(people, key = { it.num }) { person ->
                    ListItem(
                        headlineContent = { Text(person.name) },
                        supportingContent = { Text(person.code, fontFamily = FontFamily.Monospace) },
                        trailingContent = {
                            if (person.canMessage) {
                                TextButton(onClick = { onOpenChat(viewModel.dmContactKey(person.num)) }) {
                                    Text(stringResource(Res.string.pangea_open_chat))
                                }
                            }
                        },
                    )
                }
            }
            myCode?.let { (code, link) -> item { MyCodeCard(code, link) } }
        }
    }
}

@Composable
private fun CodeEntry(resolve: ResolveState, onSubmit: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val scanner = LocalBarcodeScannerProvider.current { contents -> contents?.let { onSubmit(it) } }
    val canScan = LocalBarcodeScannerSupported.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.pangea_new_message), style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(Res.string.pangea_new_message_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            isError = resolve is ResolveState.Failed && PangeaCode.looksComplete(text),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { onSubmit(text) }, enabled = text.isNotBlank() && resolve !is ResolveState.Reaching) {
                Text(stringResource(Res.string.pangea_open_chat))
            }
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val pasted = clipboard.getClipEntry()?.readText().orEmpty()
                        if (pasted.isNotBlank()) {
                            text = pasted
                            onSubmit(pasted)
                        }
                    }
                },
            ) {
                Text(stringResource(Res.string.pangea_paste))
            }
            if (canScan) {
                IconButton(onClick = scanner::startScan) {
                    Icon(MeshtasticIcons.QrCodeScanner, contentDescription = null)
                }
            }
        }
        ResolveStatus(resolve)
    }
}

@Composable
private fun ResolveStatus(resolve: ResolveState) {
    when (resolve) {
        is ResolveState.Reaching ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                Text(stringResource(Res.string.pangea_reaching, resolve.code))
            }

        is ResolveState.Unreachable ->
            Text(stringResource(Res.string.pangea_unreachable, resolve.code), color = MaterialTheme.colorScheme.error)

        is ResolveState.Failed -> Text(stringResource(resolve.reason.message), color = MaterialTheme.colorScheme.error)

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

/** My @code, with Share and a QR code that carries my name and key so scanning it connects at once. */
@Composable
fun MyCodeCard(code: String, link: String, modifier: Modifier = Modifier) {
    var showQr by remember { mutableStateOf(false) }
    val share = rememberShareText()
    val title = stringResource(Res.string.pangea_your_code)
    Card(modifier = modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(code, style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Monospace)
            Text(
                stringResource(Res.string.pangea_your_code_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { share("$code\n$link", title) }) {
                    Icon(MeshtasticIcons.Share, contentDescription = null)
                    Text(title, modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedButton(onClick = { showQr = true }) {
                    Icon(MeshtasticIcons.QrCode, contentDescription = null)
                    Text(stringResource(Res.string.pangea_show_qr), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
    if (showQr) QrDialog(title = title, uriString = link, onDismiss = { showQr = false }, subtitle = code)
}
