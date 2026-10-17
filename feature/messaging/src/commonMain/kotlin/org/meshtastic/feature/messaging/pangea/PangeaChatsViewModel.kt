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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.pangea.ContactProfile
import org.meshtastic.core.model.pangea.GroupChatError
import org.meshtastic.core.model.pangea.GroupChatException
import org.meshtastic.core.model.pangea.GroupInvite
import org.meshtastic.core.model.pangea.PangeaCode
import org.meshtastic.core.model.pangea.PhotoAsk
import org.meshtastic.core.repository.ConnectionStateProvider
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.PangeaGroup
import org.meshtastic.core.repository.PangeaGroupChats
import org.meshtastic.core.repository.PangeaLinkInbox
import org.meshtastic.core.repository.PangeaProfileStore
import org.meshtastic.core.repository.PangeaProfileSync
import org.meshtastic.core.repository.PendingGroupInvite
import org.meshtastic.core.repository.RadioController
import org.meshtastic.core.repository.UiPrefs
import kotlin.time.Duration.Companion.seconds

/** Someone to start a private chat with, as the New Message and invite lists show them. */
data class PangeaPerson(val num: Long, val name: String, val code: String, val canMessage: Boolean)

/**
 * Turns an @code into someone you can message privately (Pangea-Apple ContactResolver.swift). If we already know them,
 * done. If the invite carried their key (link / QR), add them straight to the radio. Otherwise say hello over the mesh
 * and wait for their key.
 */
sealed interface ResolveState {
    data object Idle : ResolveState

    data class Reaching(val code: String) : ResolveState

    /** Ready: open this DM. */
    data class Ready(val contactKey: String) : ResolveState

    data class Unreachable(val code: String) : ResolveState

    data class Failed(val reason: ResolveFailure) : ResolveState
}

enum class ResolveFailure {
    NOT_CONNECTED,
    OWN_CODE,
    SEND_FAILED,
    INVALID,
}

@Suppress("TooManyFunctions")
@KoinViewModel
class PangeaChatsViewModel(
    private val groupChats: PangeaGroupChats,
    private val profiles: PangeaProfileStore,
    private val profileSync: PangeaProfileSync,
    private val nodeRepository: NodeRepository,
    private val radioController: RadioController,
    private val linkInbox: PangeaLinkInbox,
    connectionStateProvider: ConnectionStateProvider,
    private val uiPrefs: UiPrefs,
) : ViewModel() {
    /** The bell in Everyone Nearby (standard mode): off until turned on. */
    val everyoneNearbyNotifications: StateFlow<Boolean> = uiPrefs.everyoneNearbyNotifications

    fun toggleEveryoneNearbyNotifications() =
        uiPrefs.setEveryoneNearbyNotifications(!uiPrefs.everyoneNearbyNotifications.value)

    /** Pangea standard mode hides mesh detail; Expert Mode shows it. */
    val expertModeEnabled: StateFlow<Boolean> = uiPrefs.expertModeEnabled

    /** A pangea:// link opened from outside the app. */
    val pendingLink: StateFlow<String?> = linkInbox.pending

    fun consumeLink() = linkInbox.consume()

    val connectionState: StateFlow<ConnectionState> = connectionStateProvider.connectionState

    val pendingInvites: StateFlow<List<PendingGroupInvite>> = groupChats.pendingInvites

    val groups: StateFlow<List<PangeaGroup>> = groupChats.groups

    private val _resolve = MutableStateFlow<ResolveState>(ResolveState.Idle)
    val resolve: StateFlow<ResolveState> = _resolve.asStateFlow()

    private val _groupError = MutableStateFlow<GroupChatError?>(null)
    val groupError: StateFlow<GroupChatError?> = _groupError.asStateFlow()

    private var resolveJob: Job? = null

    /** My @code and pangea:// link, once my radio's node is known. */
    val myCode: StateFlow<Pair<String, String>?> =
        nodeRepository.ourNodeInfo
            .map { node ->
                node ?: return@map null
                val num = node.num.toLong() and UINT32
                PangeaCode.code(num) to PangeaCode.link(num, node.user)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), null)

    /** Saved contacts first, then everyone I can message privately, by name. */
    val people: StateFlow<List<PangeaPerson>> =
        combine(nodeRepository.nodeDBbyNum, profiles.contacts, nodeRepository.ourNodeInfo) { nodes, _, me ->
            val myNum = me?.num
            val saved = profiles.contactNums { num -> nodes[num.toInt()]?.user?.long_name }
            val savedPeople =
                saved.map { num ->
                    person(num, nodes[num.toInt()]?.user?.long_name, nodes[num.toInt()]?.hasPKC == true)
                }
            val others =
                nodes.values
                    .filter { it.num != myNum && it.hasPKC && (it.num.toLong() and UINT32) !in saved }
                    .map { person(it.num.toLong() and UINT32, it.user.long_name, true) }
                    .sortedBy { it.name.lowercase() }
            savedPeople + others
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    private fun person(num: Long, radioName: String?, canMessage: Boolean) =
        PangeaPerson(num, profiles.name(num, radioName), PangeaCode.code(num), canMessage)

    /** The private-chat conversation key for [num]. */
    fun dmContactKey(num: Long): String = "${NodeAddress.PKC_CHANNEL_INDEX}${NodeAddress.numToDefaultId(num.toInt())}"

    /** What the user typed, pasted or scanned: an @code, node ID, pangea:// link or Meshtastic contact link. */
    fun resolve(input: String) {
        val invite = PangeaCode.invite(input)
        if (invite == null) {
            _resolve.value = ResolveState.Failed(ResolveFailure.INVALID)
            return
        }
        resolve(invite)
    }

    fun resolve(invite: PangeaCode.Invite) {
        resolveJob?.cancel()
        resolveJob = viewModelScope.launch { _resolve.value = run(invite) }
    }

    fun cancelResolve() {
        resolveJob?.cancel()
        _resolve.value = ResolveState.Idle
    }

    @Suppress("ReturnCount")
    private suspend fun run(invite: PangeaCode.Invite): ResolveState {
        val me = nodeRepository.ourNodeInfo.value
        if (connectionState.value != ConnectionState.Connected || me == null) {
            return ResolveState.Failed(ResolveFailure.NOT_CONNECTED)
        }
        if (invite.nodeNum == (me.num.toLong() and UINT32)) return ResolveState.Failed(ResolveFailure.OWN_CODE)
        val ready = ResolveState.Ready(dmContactKey(invite.nodeNum))
        if (hasKey(invite.nodeNum)) return ready

        // Link or QR code: we already have their name and key.
        val contact = invite.contact
        if (contact != null && (contact.user?.public_key?.size ?: 0) > 0) {
            val added = runCatching { radioController.importContact(contact) }.isSuccess
            if (added) return ready
        }

        // Typed code: say hello over the mesh and wait for their reply.
        _resolve.value = ResolveState.Reaching(invite.code)
        repeat(ATTEMPTS) {
            val sent = runCatching { radioController.requestUserInfo(invite.nodeNum.toInt()) }.isSuccess
            if (!sent) return ResolveState.Failed(ResolveFailure.SEND_FAILED)
            repeat(REQUEST_INTERVAL_SECONDS) {
                delay(1.seconds)
                if (hasKey(invite.nodeNum)) return ready
            }
        }
        return ResolveState.Unreachable(invite.code)
    }

    /** A DM to them goes privately (PKI). Without their key it would go out on the public channel. */
    private fun hasKey(num: Long): Boolean = nodeRepository.nodeDBbyNum.value[num.toInt()]?.hasPKC == true

    // MARK: Groups

    fun clearGroupError() {
        _groupError.value = null
    }

    fun createGroup(name: String, onCreated: (Int) -> Unit) =
        viewModelScope.launch { groupChats.create(name).onSuccess(onCreated).onFailure(::reportGroupError) }

    fun join(invite: GroupInvite, onJoined: (Int) -> Unit) =
        viewModelScope.launch { groupChats.join(invite).onSuccess(onJoined).onFailure(::reportGroupError) }

    fun decline(pending: PendingGroupInvite) = groupChats.declineInvite(pending.id)

    fun leave(index: Int, onLeft: () -> Unit) =
        viewModelScope.launch { groupChats.leave(index).onSuccess { onLeft() }.onFailure(::reportGroupError) }

    fun sendInvite(group: PangeaGroup, toNum: Long, onSent: () -> Unit) = viewModelScope.launch {
        groupChats
            .sendInvite(GroupInvite(group.name, group.psk), toNum)
            .onSuccess { onSent() }
            .onFailure(::reportGroupError)
    }

    /** The invite link for a group I'm in, or why it can't be shared. */
    fun shareable(group: PangeaGroup): Result<GroupInvite> = GroupInvite.shareable(group.name, group.psk)

    fun senderName(num: Long): String =
        profiles.name(num, nodeRepository.nodeDBbyNum.value[num.toInt()]?.user?.long_name)

    fun reportShareError(error: Throwable) = reportGroupError(error)

    // MARK: Profiles

    val profileContacts: StateFlow<Map<Long, ContactProfile>> = profiles.contacts

    val downloads: StateFlow<Map<Long, Double>> = profiles.downloads

    private val _lastPhotoAsk = MutableStateFlow<PhotoAsk?>(null)
    val lastPhotoAsk: StateFlow<PhotoAsk?> = _lastPhotoAsk.asStateFlow()

    fun photo(num: Long): ByteArray? = profiles.photoData(num)?.toByteArray()

    fun displayName(num: Long, radioName: String?): String = profiles.name(num, radioName)

    fun setNickname(num: Long, nickname: String) = profiles.setNickname(nickname, num)

    fun setContact(num: Long, saved: Boolean, radioName: String?) = profiles.setContact(saved, num, radioName)

    fun declinePhoto(num: Long) = profiles.declinePhoto(num)

    fun requestPhoto(num: Long) = viewModelScope.launch { _lastPhotoAsk.value = profileSync.requestPhoto(num) }

    fun canRequestPhoto(num: Long): Boolean = profileSync.canRequestPhoto(num)

    private fun reportGroupError(error: Throwable) {
        _groupError.value = (error as? GroupChatException)?.error ?: GroupChatError.NOT_CONNECTED
    }

    /** The conversation key for a group chat in [index]. */
    fun groupContactKey(index: Int): String = "$index${NodeAddress.ID_BROADCAST}"

    private companion object {
        const val UINT32 = 0xFFFFFFFFL
        const val ATTEMPTS = 3
        const val REQUEST_INTERVAL_SECONDS = 20
        const val STOP_TIMEOUT = 5_000L
    }
}
