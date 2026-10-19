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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.pangea.PangeaCode
import org.meshtastic.core.model.pangea.PhotoAsk
import org.meshtastic.core.model.pangea.ProfileRules
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.done
import org.meshtastic.core.resources.pangea_add_nickname
import org.meshtastic.core.resources.pangea_add_to_contacts
import org.meshtastic.core.resources.pangea_get_photo
import org.meshtastic.core.resources.pangea_getting_photo
import org.meshtastic.core.resources.pangea_nickname
import org.meshtastic.core.resources.pangea_nickname_hint
import org.meshtastic.core.resources.pangea_photo_busy
import org.meshtastic.core.resources.pangea_photo_connect_radio
import org.meshtastic.core.resources.pangea_photo_offer
import org.meshtastic.core.resources.pangea_photo_waiting_radio
import org.meshtastic.core.resources.pangea_profile
import org.meshtastic.core.resources.pangea_remove_from_contacts
import org.meshtastic.core.resources.pangea_send_message
import org.meshtastic.core.ui.component.PangeaAvatar
import org.meshtastic.core.ui.component.rememberAvatarBitmap
import org.meshtastic.core.ui.icon.Close
import org.meshtastic.core.ui.icon.Copy
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.PersonAdd
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosInset
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.ios.IosSheet
import org.meshtastic.core.ui.ios.IosTextField
import org.meshtastic.core.ui.ios.iosPressable
import org.meshtastic.core.ui.util.createClipEntry

/**
 * Someone's profile: their photo (fetched only when asked), the name they chose, a private nickname, their @code, and
 * Save to Contacts (Pangea-Apple ContactProfileSheet).
 */
@Suppress("ViewModelForwarding", "ModifierMissing", "LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactProfileSheet(
    viewModel: PangeaChatsViewModel,
    num: Long,
    radioName: String?,
    shortName: String,
    nodeColor: Color,
    onDismiss: () -> Unit,
    onSendMessage: (() -> Unit)? = null,
) {
    val contacts by viewModel.profileContacts.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val profile = contacts[num]
    var nickname by rememberSaveable(num) { mutableStateOf(profile?.nickname.orEmpty()) }
    val photo = rememberAvatarBitmap(if ((profile?.savedPhotoVersion ?: 0L) != 0L) viewModel.photo(num) else null)
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val code = PangeaCode.code(num)
    val c = Ios.colors
    // Closing the sheet saves a changed nickname, as on iOS.
    val close = {
        if (nickname != profile?.nickname.orEmpty()) viewModel.setNickname(num, nickname)
        onDismiss()
    }

    IosSheet(
        title = stringResource(Res.string.pangea_profile),
        onDismissRequest = close,
        actionLabel = stringResource(Res.string.done),
        onAction = close,
        grouped = true,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PangeaAvatar(
                photo = photo,
                emoji = profile?.card?.emoji.orEmpty(),
                colorIndex = profile?.card?.color,
                initials = shortName,
                fallbackColor = nodeColor,
                size = 120.dp,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                viewModel.displayName(num, radioName),
                style = Ios.type.title2.copy(fontWeight = FontWeight.Bold),
                color = c.label,
                modifier = Modifier.padding(top = 12.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier =
                Modifier.padding(top = 6.dp).iosPressable {
                    scope.launch { clipboard.setClipEntry(createClipEntry(code, code)) }
                },
            ) {
                Icon(MeshtasticIcons.Copy, contentDescription = null, tint = c.tint, modifier = Modifier.size(20.dp))
                Text(code, style = Ios.type.body.copy(fontFamily = FontFamily.Monospace), color = c.tint)
            }
            Box(Modifier.padding(horizontal = IosInset, vertical = 8.dp)) {
                PhotoRow(viewModel, num, profile?.canUpdatePhoto == true, downloads[num])
            }
            IosSection(
                header = stringResource(Res.string.pangea_nickname),
                footer = stringResource(Res.string.pangea_nickname_hint),
            ) {
                IosTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    placeholder = stringResource(Res.string.pangea_add_nickname),
                    containerColor = c.groupedRow,
                )
            }
            IosSection {
                if (onSendMessage != null) {
                    IosRow(
                        title = stringResource(Res.string.pangea_send_message),
                        icon = IosIcons.Bubble,
                        titleColor = c.tint,
                        onClick = {
                            close()
                            onSendMessage()
                        },
                    )
                }
                val saved = profile?.isContact == true
                IosRow(
                    title =
                    stringResource(
                        if (saved) Res.string.pangea_remove_from_contacts else Res.string.pangea_add_to_contacts,
                    ),
                    icon = MeshtasticIcons.PersonAdd,
                    titleColor = if (saved) c.systemRed else c.tint,
                    iconTint = if (saved) c.systemRed else c.tint,
                    onClick = { viewModel.setContact(num, !saved, radioName) },
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
