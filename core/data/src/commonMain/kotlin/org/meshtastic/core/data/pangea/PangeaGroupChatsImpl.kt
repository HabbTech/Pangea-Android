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
package org.meshtastic.core.data.pangea

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path
import org.koin.core.annotation.Single
import org.meshtastic.core.common.di.ServiceScope
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.database.getDatabaseDirectory
import org.meshtastic.core.database.getFileSystem
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.pangea.GroupChatError
import org.meshtastic.core.model.pangea.GroupChatException
import org.meshtastic.core.model.pangea.GroupInvite
import org.meshtastic.core.model.pangea.PangeaCode
import org.meshtastic.core.repository.AdminController
import org.meshtastic.core.repository.CommandSender
import org.meshtastic.core.repository.ConnectionStateProvider
import org.meshtastic.core.repository.NodeManager
import org.meshtastic.core.repository.Notification
import org.meshtastic.core.repository.NotificationManager
import org.meshtastic.core.repository.PangeaGroup
import org.meshtastic.core.repository.PangeaGroupChats
import org.meshtastic.core.repository.PendingGroupInvite
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.getStringSuspend
import org.meshtastic.core.resources.pangea_group_invite_body
import org.meshtastic.core.resources.pangea_group_invite_title
import org.meshtastic.core.resources.pangea_group_invites_more_body
import org.meshtastic.core.resources.pangea_group_invites_more_title
import org.meshtastic.proto.Channel
import org.meshtastic.proto.ChannelSettings
import org.meshtastic.proto.MeshPacket
import org.meshtastic.proto.PortNum
import kotlin.random.Random

/** Port of Pangea-Apple GroupChats.swift and GroupInviteStore. */
@Suppress("TooManyFunctions", "LongParameterList", "ReturnCount")
@Single(binds = [PangeaGroupChats::class])
class PangeaGroupChatsImpl(
    private val radioConfigRepository: RadioConfigRepository,
    private val adminController: AdminController,
    private val commandSender: CommandSender,
    private val nodeManager: NodeManager,
    private val connection: ConnectionStateProvider,
    private val notifications: NotificationManager,
    private val scope: ServiceScope,
) : PangeaGroupChats {
    private val fileSystem: FileSystem = getFileSystem()
    private val file: Path = getDatabaseDirectory().parent!! / "PangeaProfiles" / "invites.json"
    private val json = Json { ignoreUnknownKeys = true }

    override val groups: StateFlow<List<PangeaGroup>> =
        radioConfigRepository.channelSetFlow
            .map { set ->
                set.settings.mapIndexedNotNull { index, settings ->
                    if (index == 0 || !isChat(settings)) return@mapIndexedNotNull null
                    PangeaGroup(index, settings.name, settings.psk, GroupInvite.isPrivateKey(settings.psk))
                }
            }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _pendingInvites = MutableStateFlow(load())
    override val pendingInvites: StateFlow<List<PendingGroupInvite>> = _pendingInvites.asStateFlow()

    /** Monotonic times of the last few invite notifications, so a flood can't bury the lock screen. */
    private val notified = ArrayDeque<Long>()

    private fun isChat(settings: ChannelSettings): Boolean =
        (settings.name.isNotEmpty() || settings.psk.size > 0) && !GroupInvite.isReserved(settings.name)

    private suspend fun requireConnected(): Int {
        if (connection.connectionState.value != ConnectionState.Connected) {
            throw GroupChatException(GroupChatError.NOT_CONNECTED)
        }
        return nodeManager.myNodeNum.value ?: throw GroupChatException(GroupChatError.NOT_CONNECTED)
    }

    /** Matched on key and the name's exact bytes: the radio hears a channel by a hash of both. */
    private fun existing(invite: GroupInvite, settings: List<ChannelSettings>): Int? =
        settings.indices.firstOrNull { index ->
            index > 0 && settings[index].psk == invite.psk && settings[index].name == invite.name
        }

    /** The first secondary slot the radio isn't using. Without a primary channel, the list is still loading. */
    private fun freeSlot(settings: List<ChannelSettings>): Int {
        if (settings.isEmpty()) throw GroupChatException(GroupChatError.CHANNELS_NOT_LOADED)
        return (1..MAX_GROUPS).firstOrNull { index -> settings.getOrNull(index)?.let { !isUsed(it) } ?: true }
            ?: throw GroupChatException(GroupChatError.FULL)
    }

    private fun isUsed(settings: ChannelSettings): Boolean = settings.name.isNotEmpty() || settings.psk.size > 0

    override suspend fun create(name: String): Result<Int> = runCatching {
        val groupName = GroupInvite.nameForNewGroup(name).getOrThrow()
        val key = Random.nextBytes(KEY_BYTES).toByteString()
        join(GroupInvite(groupName, key)).getOrThrow()
    }

    override suspend fun join(invite: GroupInvite): Result<Int> = runCatching {
        if (!GroupInvite.isPrivateKey(invite.psk)) {
            removeInvite(invite.id)
            throw GroupChatException(GroupChatError.CANT_SHARE_KEY)
        }
        requireConnected()
        val settings = radioConfigRepository.channelSetFlow.first().settings
        existing(invite, settings)?.let {
            removeInvite(invite.id)
            return@runCatching it
        }
        val index = freeSlot(settings)
        val channel =
            Channel.Builder()
                .also {
                    it.index = index
                    it.role = Channel.Role.SECONDARY
                    it.settings = invite.settings
                }
                .build()
        adminController.setLocalChannel(channel)
        Logger.i { "👥 [Groups] Joined group in slot $index" }
        removeInvite(invite.id)
        index
    }

    override suspend fun leave(index: Int): Result<Unit> = runCatching {
        if (index <= 0) return@runCatching
        requireConnected()
        val disabled =
            Channel.Builder()
                .also {
                    it.index = index
                    it.role = Channel.Role.DISABLED
                }
                .build()
        adminController.setLocalChannel(disabled)
        // A disabled slot isn't written to the local channel list, so clear it here or the group would linger until
        // a reconnect (the "needs an app restart" bug the field tester hit).
        val settings = radioConfigRepository.channelSetFlow.first().settings.toMutableList()
        if (index < settings.size) {
            settings[index] = ChannelSettings.Builder().build()
            while (settings.size > 1 && !isUsed(settings.last())) {
                settings.removeAt(settings.lastIndex)
            }
            radioConfigRepository.replaceAllSettings(settings)
        }
    }

    override suspend fun sendInvite(invite: GroupInvite, toNum: Long): Result<Unit> = runCatching {
        // Their app would refuse it without a word, so don't let it look sent.
        GroupInvite.shareable(invite.name, invite.psk).getOrThrow()
        requireConnected()
        val node = nodeManager.nodeDBbyNodeNum[toNum.toInt()]
        val key = node?.let { it.publicKey ?: it.user.public_key }
        if (key == null || key.size == 0) throw GroupChatException(GroupChatError.NO_KEY)
        commandSender.sendData(
            DataPacket(
                to = NodeAddress.numToDefaultId(toNum.toInt()),
                bytes = invite.invitePayload,
                dataType = PortNum.PRIVATE_APP.value,
                channel = NodeAddress.PKC_CHANNEL_INDEX,
                wantAck = true,
            ),
        )
        Logger.i { "👥 [Groups] Sent group invite" }
    }

    override fun declineInvite(id: String) = removeInvite(id)

    /** Only PKI-encrypted DMs count, so an invite can't be broadcast to everyone on a public channel. */
    override fun handle(packet: MeshPacket, myNodeNum: Int): Boolean {
        val data = packet.decoded ?: return false
        if (data.portnum != PortNum.PRIVATE_APP || !data.payload.startsWith(GroupInvite.MAGIC)) return false
        if (packet.pki_encrypted != true || (packet.to.toLong() and UINT32) == UINT32) return true
        val invite = GroupInvite.fromInvitePayload(data.payload)
        if (invite == null) {
            Logger.i { "👥 [Groups] Ignored an invite whose name or key can't be joined" }
            return true
        }
        scope.launch {
            val settings = radioConfigRepository.channelSetFlow.first().settings
            if (existing(invite, settings) != null) return@launch
            add(invite, packet.from.toLong() and UINT32)
        }
        return true
    }

    // MARK: Pending invites

    private fun add(invite: GroupInvite, from: Long) {
        val fresh = _pendingInvites.value.filter { nowMillis - it.receivedEpochMillis <= MAX_AGE_MILLIS }
        val fromThem = fresh.count { it.fromNum == from }
        if (fresh.any { it.invite == invite } || fromThem >= MAX_PER_SENDER) return
        var list = fresh
        if (list.size >= MAX_PENDING) {
            // Full: anyone new takes the place of the oldest invite from whoever has the most waiting.
            if (fromThem != 0) return
            val counts = list.groupingBy { it.fromNum }.eachCount()
            val most = counts.values.maxOrNull() ?: return
            if (most <= 1) return
            val oldest = list.indexOfLast { counts[it.fromNum] == most }
            list = list.filterIndexed { i, _ -> i != oldest }
        }
        _pendingInvites.value = listOf(PendingGroupInvite(invite, from, nowMillis)) + list
        save()
        scope.launch { notify(invite, from) }
    }

    private suspend fun notify(invite: GroupInvite, from: Long) {
        val now = nowMillis
        while (notified.isNotEmpty() && now - notified.first() > HOUR_MILLIS) notified.removeFirst()
        if (notified.size >= MAX_NOTIFICATIONS_PER_HOUR) {
            notifications.dispatch(
                Notification(
                    title = getStringSuspend(Res.string.pangea_group_invites_more_title),
                    message = getStringSuspend(Res.string.pangea_group_invites_more_body),
                    id = MORE_NOTIFICATION_ID,
                ),
            )
            return
        }
        notified.addLast(now)
        val sender =
            nodeManager.nodeDBbyNodeNum[from.toInt()]?.user?.long_name?.takeIf { it.isNotEmpty() }
                ?: PangeaCode.code(from)
        notifications.dispatch(
            Notification(
                title = getStringSuspend(Res.string.pangea_group_invite_title),
                message = getStringSuspend(Res.string.pangea_group_invite_body, sender, invite.name),
                id = invite.id.hashCode(),
            ),
        )
    }

    private fun removeInvite(id: String) {
        if (_pendingInvites.value.none { it.id == id }) return
        _pendingInvites.value = _pendingInvites.value.filterNot { it.id == id }
        save()
    }

    @Serializable
    private data class SavedInvite(val name: String, val psk: String, val fromNum: Long, val received: Long)

    private fun load(): List<PendingGroupInvite> {
        val text =
            runCatching { if (fileSystem.exists(file)) fileSystem.read(file) { readUtf8() } else null }.getOrNull()
                ?: return emptyList()
        val saved =
            runCatching { json.decodeFromString<List<SavedInvite>>(text) }
                .onFailure { Logger.e(it) { "👥 [Groups] Couldn't read saved invites" } }
                .getOrNull() ?: return emptyList()
        return saved.mapNotNull { item ->
            val psk = item.psk.decodeBase64() ?: return@mapNotNull null
            val invite = GroupInvite(item.name, psk).takeIf(GroupInvite::isJoinable) ?: return@mapNotNull null
            PendingGroupInvite(invite, item.fromNum, item.received)
        }
    }

    private fun save() {
        val saved =
            _pendingInvites.value.map {
                SavedInvite(it.invite.name, it.invite.psk.base64(), it.fromNum, it.receivedEpochMillis)
            }
        runCatching {
            fileSystem.createDirectories(file.parent!!)
            fileSystem.write(file) { writeUtf8(json.encodeToString(saved)) }
        }
            .onFailure { Logger.e(it) { "👥 [Groups] Couldn't save invites" } }
    }

    companion object {
        const val MAX_GROUPS = 7
        private const val KEY_BYTES = 32
        private const val UINT32 = 0xFFFFFFFFL
        private const val MAX_PER_SENDER = 3
        private const val MAX_PENDING = 20
        private const val MAX_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000
        private const val HOUR_MILLIS = 60L * 60 * 1000
        private const val MAX_NOTIFICATIONS_PER_HOUR = 3
        private const val MORE_NOTIFICATION_ID = 0x50474931 // "PGI1"
    }
}
