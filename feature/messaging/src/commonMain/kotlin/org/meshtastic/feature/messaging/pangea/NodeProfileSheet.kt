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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.navigation.TopLevelDestination
import org.meshtastic.core.navigation.MultiBackstack
import org.meshtastic.core.navigation.ContactsRoute

/**
 * A radio's Profile sheet opened by its node number — from the map, where a tap on a radio shows the person
 * (Pangea-Apple standard Map tab), as [org.meshtastic.core.ui.util.LocalNodeProfileSheet]. **Send Message** hands the private chat's key to [onOpenChat].
 */
@Suppress("ViewModelForwarding")
@Composable
fun NodeProfileSheet(
    nodeNum: Int,
    onDismiss: () -> Unit,
    onOpenChat: (String) -> Unit,
    viewModel: PangeaChatsViewModel = koinViewModel(),
) {
    val nodeRepository: NodeRepository = koinInject()
    val nodes by nodeRepository.nodeDBbyNum.collectAsStateWithLifecycle()
    val node = nodes[nodeNum] ?: return
    val num = nodeNum.toLong() and UINT32_MASK
    ContactProfileSheet(
        viewModel = viewModel,
        num = num,
        radioName = node.user.long_name,
        shortName = node.user.short_name,
        nodeColor = Color(node.colors.second),
        onDismiss = onDismiss,
        onSendMessage = { onOpenChat(viewModel.dmContactKey(num)) },
    )
}

/** The app's Profile sheet slot ([LocalNodeProfileSheet]): Send Message opens the chat on the Chats tab. */
fun nodeProfileSheetSlot(multiBackstack: MultiBackstack): @Composable (Int, () -> Unit) -> Unit {
    val openChat: (String) -> Unit = { key ->
        multiBackstack.navigateTopLevel(TopLevelDestination.Messages.route)
        multiBackstack.activeBackStack.add(ContactsRoute.Messages(key))
    }
    return { nodeNum, onDismiss ->
        NodeProfileSheet(
            nodeNum = nodeNum,
            onDismiss = onDismiss,
            onOpenChat = { key ->
                onDismiss()
                openChat(key)
            },
        )
    }
}

private const val UINT32_MASK = 0xFFFFFFFFL
