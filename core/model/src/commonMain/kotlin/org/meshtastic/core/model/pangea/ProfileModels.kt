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
package org.meshtastic.core.model.pangea

import kotlinx.serialization.Serializable

/** What I share about myself. Port of Pangea-Apple `MyProfile`. */
@Serializable
data class MyProfile(
    val displayName: String = "",
    val emoji: String = "",
    val color: Int = 0,
    /** 0 when I have no photo; otherwise [PangeaWire.hash] of the photo's bytes. */
    val photoVersion: Long = 0,
) {
    /** My card. The photo is described only when [photo] is the one saved as mine. */
    fun card(fallbackName: String, photo: PhotoPieces? = null): ProfileCard = ProfileCard.create(
        displayName = displayName.ifEmpty { fallbackName },
        emoji = emoji,
        color = color,
        photo = photo?.takeIf { photoVersion != 0L && it.version.toLong() == photoVersion },
    )
}

/** How a card arrived: privately from their key, or heard in a group (where anyone could have sent it). */
@Serializable
enum class CardSource {
    DIRECT,
    GROUP,
}

/** Everything this device knows about another Pangea user. Nicknames never leave this device. */
@Serializable
data class ContactProfile(
    val card: ProfileCard? = null,
    /** Version of the photo saved on this device. */
    val savedPhotoVersion: Long = 0,
    /** Photo version the user tapped "Not Now" on. */
    val declinedPhotoVersion: Long = 0,
    val nickname: String? = null,
    /** Version of my card this person has been sent. */
    val sentCardVersion: Long = 0,
    /** Group channel we last heard their card on, for people without a DM key. */
    val lastChannel: Int? = null,
    /** That group's identity ("index:name:key hash") when the card arrived. */
    val lastGroupKey: String? = null,
    val cardSource: CardSource? = null,
    /** Saved to Contacts. Kept here, not in the node list, so they stay after the radio forgets them. */
    val isContact: Boolean = false,
    /** Their radio name when they were saved, to show while the radio doesn't know them. */
    val contactName: String? = null,
) {
    /** A photo is waiting that the user hasn't downloaded or dismissed. */
    val hasPhotoOffer: Boolean
        get() {
            val version = card?.photoVersion ?: 0L
            return version != 0L && version != savedPhotoVersion && version != declinedPhotoVersion
        }

    /** A newer photo exists than the one on this device, dismissed or not. */
    val canUpdatePhoto: Boolean
        get() {
            val version = card?.photoVersion ?: 0L
            return version != 0L && version != savedPhotoVersion
        }
}

/** My card version per group, and when it was last sent there. */
@Serializable data class GroupSend(val version: Long, val epochMillis: Long)

/**
 * Who the sharing rules may send my card and photo to. iOS reads these from its message database; Android keeps its own
 * small ledger so the rules don't depend on how the packet database is shaped.
 */
@Serializable
data class ProfileLedger(
    /** People I've sent a private message (or reaction) to, with when I last did. */
    val messagedByMe: Map<Long, Long> = emptyMap(),
    /** People who've sent me a private message, with when they last did. */
    val messagedMe: Map<Long, Long> = emptyMap(),
    /** Per group identity ("index:name:key hash"): who has posted there. */
    val groupPosters: Map<String, Set<Long>> = emptyMap(),
)

/** Everything the profile store keeps in profiles.json. */
@Serializable
data class SavedProfiles(
    val me: MyProfile = MyProfile(),
    val contacts: Map<Long, ContactProfile> = emptyMap(),
    val groupSends: Map<String, GroupSend> = emptyMap(),
    val ledger: ProfileLedger = ProfileLedger(),
)
