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
package org.meshtastic.core.repository

import kotlinx.coroutines.flow.StateFlow
import okio.ByteString
import org.meshtastic.core.model.pangea.ContactProfile
import org.meshtastic.core.model.pangea.GroupInvite
import org.meshtastic.core.model.pangea.MyProfile
import org.meshtastic.core.model.pangea.PhotoAsk
import org.meshtastic.core.model.pangea.ProfileCard
import org.meshtastic.proto.MeshPacket

/**
 * Pangea profiles: my card (name, emoji, colour, photo) and what other Pangea users shared with me. Kept in their own
 * files, apart from the Meshtastic database, so upstream migrations never have to know about them.
 */
@Suppress("TooManyFunctions")
interface PangeaProfileStore {
    val me: StateFlow<MyProfile>
    val contacts: StateFlow<Map<Long, ContactProfile>>

    /** People whose photo is downloading right now, with progress 0..1. */
    val downloads: StateFlow<Map<Long, Double>>

    fun profile(num: Long): ContactProfile? = contacts.value[num]

    /** Nickname, then the name they chose in Pangea, then their radio name, then their @code. */
    fun name(num: Long, radioName: String?): String

    fun myPhotoData(): ByteString?

    fun photoData(num: Long): ByteString?

    /** [photo]: null leaves the photo as it is, [PhotoChange.Remove] removes it. */
    fun updateMe(displayName: String, emoji: String, color: Int, photo: PhotoChange? = null)

    fun setNickname(nickname: String, num: Long)

    fun declinePhoto(num: Long)

    fun isContact(num: Long): Boolean = contacts.value[num]?.isContact == true

    fun setContact(saved: Boolean, num: Long, radioName: String? = null)

    /** Saved contacts, sorted by the name each row shows. */
    fun contactNums(radioName: (Long) -> String? = { null }): List<Long>

    /** Settings → Clear App Data. */
    fun eraseAll()
}

sealed interface PhotoChange {
    data class Set(val jpeg: ByteString) : PhotoChange

    data object Remove : PhotoChange
}

/**
 * Sends my card to the people I talk to and fetches photos only when asked (Pangea-Apple ProfileSync). Everything rides
 * on PRIVATE_APP, which other Meshtastic apps ignore.
 */
interface PangeaProfileSync {
    /** Returns true when the packet was a Pangea profile message (handled or deliberately ignored). */
    fun handle(packet: MeshPacket, myNodeNum: Int): Boolean

    /** Hooked into every text message I send. [toNum] is 0xFFFFFFFF for a channel message. */
    fun didSendMessage(toNum: Long, channel: Int)

    /** Hooked into every text message I receive. */
    fun didReceiveMessage(fromNum: Long, toNum: Long, channel: Int, pkiEncrypted: Boolean)

    /** After editing my profile: refresh the people I've talked to lately. */
    fun pushUpdatedCard()

    /** The user tapped "Update" on someone's photo. */
    suspend fun requestPhoto(num: Long): PhotoAsk

    fun canRequestPhoto(num: Long): Boolean

    /** The radio this was all for is gone: nothing queued or downloading belongs to the next one. */
    fun reset()

    /** My card as it would go out now; null without a connected radio. */
    val myCard: ProfileCard?
}

/** A Pangea group chat: a private channel in a secondary slot (1–7). */
data class PangeaGroup(val index: Int, val name: String, val psk: ByteString, val isPrivate: Boolean)

/**
 * Group chats (Pangea-Apple GroupChats.swift). Creating, joining and leaving only ever touch secondary channel slots;
 * the primary channel and LoRa settings are never changed, so joining can't knock a radio off the mesh.
 */
interface PangeaGroupChats {
    val groups: StateFlow<List<PangeaGroup>>

    /** Invites received over a DM and waiting for a tap on Join, newest first. */
    val pendingInvites: StateFlow<List<PendingGroupInvite>>

    /** Creates a group with a fresh 32-byte key in the first free slot. Returns the slot. */
    suspend fun create(name: String): Result<Int>

    /** Adds the group to the first free slot. Already a member? Returns that slot. */
    suspend fun join(invite: GroupInvite): Result<Int>

    suspend fun leave(index: Int): Result<Unit>

    /** Sends the group's key privately (PKI) to someone I can message privately. */
    suspend fun sendInvite(invite: GroupInvite, toNum: Long): Result<Unit>

    fun declineInvite(id: String)

    /** Returns true when the packet was a Pangea group invite (kept or deliberately ignored). */
    fun handle(packet: MeshPacket, myNodeNum: Int): Boolean
}

data class PendingGroupInvite(val invite: GroupInvite, val fromNum: Long, val receivedEpochMillis: Long) {
    val id: String
        get() = invite.id
}

/** A pangea:// link opened from outside the app, waiting for the Chats screen to act on it. */
interface PangeaLinkInbox {
    val pending: StateFlow<String?>

    fun offer(link: String)

    fun consume()
}

/**
 * Applies what a freshly paired radio needs, without the user having to understand it (Pangea-Apple
 * RadioAutoConfig.apply). Also switches a radio to pairing without a code once it's paired (at most twice per radio).
 */
interface PangeaRadioSetup {
    /** Saves [name] as the radio's owner name, with initials as its short name. True when it changed anything. */
    suspend fun applyName(name: String): Result<Boolean>

    /** Sets the LoRa region if the radio has none or another one. True when a reboot is expected. */
    suspend fun applyRegion(region: org.meshtastic.proto.Config.LoRaConfig.RegionCode): Result<Boolean>
}
