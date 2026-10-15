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
@file:Suppress("ReturnCount")

package org.meshtastic.core.model.pangea

import okio.ByteString
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import org.meshtastic.proto.ChannelSet
import org.meshtastic.proto.ChannelSettings

/** Why a group couldn't be created, joined, shared or invited to. */
enum class GroupChatError {
    NOT_CONNECTED,
    CHANNELS_NOT_LOADED,
    FULL,
    NO_KEY,
    RESERVED_NAME,
    BLANK_NAME,
    TOO_LONG,
    CANT_SHARE_NAME,
    CANT_SHARE_KEY,
}

/**
 * A group chat is a private channel with a random key, living in a secondary slot (1–7); the primary channel and LoRa
 * settings are never touched. Port of Pangea-Apple `GroupInvite` (GroupChats.swift); links and DM invites must stay
 * byte-for-byte compatible.
 *
 * Equality is byte-for-byte on name and key, as the radio tells channels apart.
 */
class GroupInvite(val name: String, val psk: ByteString) {
    /** The name's bytes plus a hash of the key, so the key itself never ends up in identifiers. */
    val id: String
        get() = name.encodeUtf8().base64() + "." + psk.sha256().substring(0, ID_HASH_BYTES).hex()

    val settings: ChannelSettings
        get() =
            ChannelSettings.Builder()
                .also {
                    it.name = name
                    it.psk = psk
                    it.uplink_enabled = false
                    it.downlink_enabled = false
                }
                .build()

    /** pangea://group#<ChannelSet with just this channel and no LoRa config> */
    val link: String
        get() {
            val set = ChannelSet.Builder().also { it.settings = listOf(settings) }.build()
            val data = ChannelSet.ADAPTER.encode(set).toByteString().base64Url().trimEnd('=')
            return "${PangeaCode.SCHEME}://$HOST#$data"
        }

    /** "PGI1" + ChannelSettings, sent privately (PKI) on PRIVATE_APP. */
    val invitePayload: ByteString
        get() = PangeaWire.concat(MAGIC, ChannelSettings.ADAPTER.encode(settings).toByteString())

    override fun equals(other: Any?): Boolean =
        other is GroupInvite && name.encodeUtf8() == other.name.encodeUtf8() && psk == other.psk

    override fun hashCode(): Int = 31 * name.encodeUtf8().hashCode() + psk.hashCode()

    override fun toString(): String = "GroupInvite(name=$name)"

    @Suppress("MagicNumber")
    companion object {
        /** Firmware limit for channel names. */
        const val MAX_NAME_BYTES = 11
        const val HOST = "group"
        private const val ID_HASH_BYTES = 8
        private const val SHORT_KEY = 16
        private const val LONG_KEY = 32

        /** Prefix so we never mistake another app's PRIVATE_APP payload for an invite. */
        val MAGIC: ByteString = "PGI1".encodeUtf8()

        /** Channel names firmware modules and upstream treat as special; never a group chat. */
        val reservedNames = setOf("gpio", "mqtt", "serial", "admin")

        /** The firmware's default channel key, but for its last byte. */
        private val defaultKeyPrefix =
            byteArrayOf(
                0xd4.toByte(),
                0xf1.toByte(),
                0xbb.toByte(),
                0x3a,
                0x20,
                0x29,
                0x07,
                0x59,
                0xf0.toByte(),
                0xbc.toByte(),
                0xff.toByte(),
                0xab.toByte(),
                0xcf.toByte(),
                0x4e,
                0x69,
            )
                .toByteString()

        /** Also true for names with hidden characters: the firmware reads names as C strings. */
        fun isReserved(name: String): Boolean =
            name.hasHiddenCharacters || name.cleanedName.lowercase() in reservedNames

        /** For the name being typed into New Group only: trimmed and cut to the firmware limit in whole characters. */
        fun clampedName(name: String): String = name.trim().clampedUtf8(MAX_NAME_BYTES).trim()

        /** A received name is shown only if it's exactly what it looks like. */
        fun isAcceptableName(name: String): Boolean =
            name.isNotEmpty() && name.cleanedName == name && !name.isBlankName && !isReserved(name)

        /** The name a new group gets from what was typed, or why it can't be one. */
        fun nameForNewGroup(typed: String): Result<String> {
            val (name, tooLong) = typed.cutName(MAX_NAME_BYTES)
            if (name.isEmpty()) {
                return Result.failure(
                    GroupChatException(if (tooLong) GroupChatError.TOO_LONG else GroupChatError.BLANK_NAME),
                )
            }
            return if (isAcceptableName(name)) {
                Result.success(name)
            } else {
                Result.failure(GroupChatException(GroupChatError.RESERVED_NAME))
            }
        }

        /**
         * A key only the group has: 16 or 32 random bytes. No key, the 1-byte default ones, the published default key
         * written out in full (or any of its variants), or all zeros are public.
         */
        fun isPrivateKey(psk: ByteString?): Boolean {
            if (psk == null || (psk.size != SHORT_KEY && psk.size != LONG_KEY)) return false
            if ((0 until psk.size).none { psk[it] != 0.toByte() }) return false
            return !(psk.size == SHORT_KEY && psk.startsWith(defaultKeyPrefix))
        }

        /** The invite for a group I'm in, or why a friend's app couldn't join from one. */
        fun shareable(name: String, psk: ByteString): Result<GroupInvite> {
            if (!isPrivateKey(psk)) return Result.failure(GroupChatException(GroupChatError.CANT_SHARE_KEY))
            val invite =
                fromSettings(GroupInvite(name, psk).settings)
                    ?: return Result.failure(GroupChatException(GroupChatError.CANT_SHARE_NAME))
            return Result.success(invite)
        }

        /** Received from someone else (link, QR or DM). The name is taken byte for byte; anything off is refused. */
        fun fromSettings(settings: ChannelSettings): GroupInvite? {
            val name = settings.name
            if (name.encodeUtf8().size > MAX_NAME_BYTES || !isAcceptableName(name) || !isPrivateKey(settings.psk)) {
                return null
            }
            return GroupInvite(name, settings.psk)
        }

        fun isJoinable(invite: GroupInvite): Boolean = fromSettings(invite.settings) != null

        fun fromLink(url: String): GroupInvite? {
            val parsed = SimpleUrl.parse(url.trim()) ?: return null
            if (!parsed.scheme.equals(PangeaCode.SCHEME, true) || !parsed.host.equals(HOST, true)) return null
            val data = parsed.fragment?.base64UrlToBase64()?.decodeBase64() ?: return null
            val set = runCatching { ChannelSet.ADAPTER.decode(data) }.getOrNull() ?: return null
            return set.settings.firstOrNull()?.let(::fromSettings)
        }

        fun fromInvitePayload(payload: ByteString): GroupInvite? {
            if (!payload.startsWith(MAGIC)) return null
            val settings =
                runCatching { ChannelSettings.ADAPTER.decode(payload.substring(MAGIC.size)) }.getOrNull() ?: return null
            return fromSettings(settings)
        }
    }
}

class GroupChatException(val error: GroupChatError) : Exception(error.name)
