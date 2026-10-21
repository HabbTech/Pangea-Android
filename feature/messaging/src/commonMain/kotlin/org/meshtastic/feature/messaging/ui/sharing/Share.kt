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
package org.meshtastic.feature.messaging.ui.sharing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.Contact
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.share
import org.meshtastic.core.resources.share_to
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosAccessory
import org.meshtastic.core.ui.ios.IosButton
import org.meshtastic.core.ui.ios.IosButtonStyle
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosInset
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.ios.LocalIosBottomInset
import org.meshtastic.feature.messaging.pangea.IosChatAvatar
import org.meshtastic.feature.messaging.ui.contact.ContactsViewModel

@Composable
fun ShareScreen(viewModel: ContactsViewModel, onConfirm: (String) -> Unit, onNavigateUp: () -> Unit) {
    val contactList by viewModel.contactList.collectAsStateWithLifecycle()
    // A retired channel has left the radio, so it can receive nothing; offering it here would fail silently.
    val shareable = remember(contactList) { contactList.filterNot { it.isRetired } }

    ShareScreen(contacts = shareable, onConfirm = onConfirm, onNavigateUp = onNavigateUp)
}

/**
 * Picking the conversation to share into, drawn as an iOS inset list: each chat with its avatar and last message, a
 * checkmark on the chosen one, and Share as the large button under the list.
 */
@Composable
fun ShareScreen(contacts: List<Contact>, onConfirm: (String) -> Unit, onNavigateUp: () -> Unit) {
    var selectedContact by remember { mutableStateOf("") }
    // A conversation can leave the list while the sheet is open, most obviously when its channel is archived mid
    // share. Drop the selection with it rather than leaving the button armed on a key that can no longer receive.
    val selectionIsSendable = contacts.any { it.contactKey == selectedContact }
    LaunchedEffect(contacts) {
        if (selectedContact.isNotEmpty() && contacts.none { it.contactKey == selectedContact }) selectedContact = ""
    }

    Scaffold(
        containerColor = Ios.colors.groupedBackground,
        topBar = {
            MainAppBar(
                title = stringResource(Res.string.share_to),
                ourNode = null,
                showNodeChip = false,
                canNavigateUp = true,
                onNavigateUp = onNavigateUp,
                actions = {},
                onClickChip = {},
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
            Modifier.padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = LocalIosBottomInset.current + 24.dp),
        ) {
            IosSection {
                contacts.forEach { contact ->
                    IosRow(
                        title = contact.longName,
                        subtitle = contact.lastMessageText?.takeIf { it.isNotBlank() },
                        leading = {
                            IosChatAvatar(
                                contactKey = contact.contactKey,
                                shortName = contact.shortName,
                                longName = contact.longName,
                                nodeColor = contact.nodeColors?.let { Color(it.second) },
                                size = AVATAR_SIZE.dp,
                            )
                        },
                        leadingWidth = AVATAR_COLUMN.dp,
                        accessory =
                        if (contact.contactKey == selectedContact) IosAccessory.Checkmark else IosAccessory.None,
                        onClick = { selectedContact = contact.contactKey },
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            IosButton(
                text = stringResource(Res.string.share),
                onClick = { if (selectionIsSendable) onConfirm(selectedContact) },
                style = IosButtonStyle.Filled,
                enabled = selectionIsSendable,
                large = true,
                icon = IosIcons.Send,
                modifier = Modifier.fillMaxWidth().padding(horizontal = IosInset),
            )
        }
    }
}

private const val AVATAR_SIZE = 40

/** Room for the avatar ahead of the chat's name, as the chat list lays it out. */
private const val AVATAR_COLUMN = 68

// Preview kept out of commonMain to avoid platform tooling dependencies.
