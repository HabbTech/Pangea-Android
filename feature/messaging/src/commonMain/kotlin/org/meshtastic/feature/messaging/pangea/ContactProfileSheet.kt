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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.pangea.PangeaCode
import org.meshtastic.core.model.pangea.PhotoAsk
import org.meshtastic.core.model.pangea.ProfileRules
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.pangea_get_photo
import org.meshtastic.core.resources.pangea_getting_photo
import org.meshtastic.core.resources.pangea_nickname
import org.meshtastic.core.resources.pangea_nickname_hint
import org.meshtastic.core.resources.pangea_photo_busy
import org.meshtastic.core.resources.pangea_photo_connect_radio
import org.meshtastic.core.resources.pangea_photo_offer
import org.meshtastic.core.resources.pangea_photo_waiting_radio
import org.meshtastic.core.resources.pangea_radio_name
import org.meshtastic.core.resources.pangea_save_contact
import org.meshtastic.core.resources.save
import org.meshtastic.core.ui.component.PangeaAvatar
import org.meshtastic.core.ui.component.rememberAvatarBitmap
import org.meshtastic.core.ui.icon.Close
import org.meshtastic.core.ui.icon.MeshtasticIcons

/**
 * Someone's profile: their photo (fetched only when asked), the name they chose, a private nickname, their @code, and
 * Save to Contacts (Pangea-Apple ContactProfileSheet).
 */
@Suppress("ViewModelForwarding")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactProfileSheet(
    viewModel: PangeaChatsViewModel,
    num: Long,
    radioName: String?,
    shortName: String,
    nodeColor: Color,
    onDismiss: () -> Unit,
) {
    val contacts by viewModel.profileContacts.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val profile = contacts[num]
    var nickname by rememberSaveable(num) { mutableStateOf(profile?.nickname.orEmpty()) }
    val photo = rememberAvatarBitmap(if ((profile?.savedPhotoVersion ?: 0L) != 0L) viewModel.photo(num) else null)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PangeaAvatar(
                photo = photo,
                emoji = profile?.card?.emoji.orEmpty(),
                colorIndex = profile?.card?.color,
                initials = shortName,
                fallbackColor = nodeColor,
                size = 96.dp,
            )
            Text(viewModel.displayName(num, radioName), style = MaterialTheme.typography.headlineSmall)
            Text(PangeaCode.code(num), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
            if (!radioName.isNullOrEmpty()) {
                Text(
                    stringResource(Res.string.pangea_radio_name, radioName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PhotoRow(viewModel, num, profile?.canUpdatePhoto == true, downloads[num])
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text(stringResource(Res.string.pangea_nickname)) },
                supportingText = { Text(stringResource(Res.string.pangea_nickname_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    if (nickname != profile?.nickname.orEmpty()) {
                        Button(
                            onClick = { viewModel.setNickname(num, nickname) },
                            modifier = Modifier.padding(end = 4.dp),
                        ) {
                            Text(stringResource(Res.string.save))
                        }
                    }
                },
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.pangea_save_contact), modifier = Modifier.weight(1f))
                Switch(
                    checked = profile?.isContact == true,
                    onCheckedChange = { viewModel.setContact(num, it, radioName) },
                )
            }
        }
    }
}

/** "Sam shared a profile photo · Get Photo", or the download as it goes. */
@Composable
private fun PhotoRow(viewModel: PangeaChatsViewModel, num: Long, canUpdate: Boolean, progress: Double?) {
    val lastAsk by viewModel.lastPhotoAsk.collectAsStateWithLifecycle()
    val connected =
        viewModel.connectionState.collectAsStateWithLifecycle().value ==
            org.meshtastic.core.model.ConnectionState.Connected
    if (progress == null && !canUpdate) return
    val line =
        ProfileRules.photoLine(
            downloading = progress != null,
            radioAway = !connected,
            full = false,
            lastAsk = lastAsk?.takeIf { it == PhotoAsk.BUSY || it == PhotoAsk.NO_RADIO },
        )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (progress != null) {
                Text(stringResource(Res.string.pangea_getting_photo, (progress * PERCENT).toInt()))
                LinearProgressIndicator(progress = { progress.toFloat() }, modifier = Modifier.fillMaxWidth())
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.pangea_photo_offer), modifier = Modifier.weight(1f))
                    Button(onClick = { viewModel.requestPhoto(num) }, enabled = viewModel.canRequestPhoto(num)) {
                        Text(stringResource(Res.string.pangea_get_photo))
                    }
                    IconButton(onClick = { viewModel.declinePhoto(num) }) {
                        Icon(MeshtasticIcons.Close, contentDescription = null)
                    }
                }
            }
            line?.let {
                Text(
                    stringResource(
                        when (it) {
                            ProfileRules.PhotoLine.WAITING_FOR_RADIO -> Res.string.pangea_photo_waiting_radio
                            ProfileRules.PhotoLine.CONNECT_RADIO -> Res.string.pangea_photo_connect_radio
                            ProfileRules.PhotoLine.BUSY -> Res.string.pangea_photo_busy
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private const val PERCENT = 100
