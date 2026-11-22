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
package org.meshtastic.core.testing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okio.ByteString
import org.meshtastic.core.model.pangea.ChatIcon
import org.meshtastic.core.model.pangea.ContactProfile
import org.meshtastic.core.model.pangea.MyProfile
import org.meshtastic.core.model.pangea.PangeaCode
import org.meshtastic.core.repository.PangeaProfileStore
import org.meshtastic.core.repository.PhotoChange

/** In-memory [PangeaProfileStore] for tests: no files, same naming rules. */
class FakePangeaProfileStore : PangeaProfileStore {
    override val me = MutableStateFlow(MyProfile())
    override val contacts = MutableStateFlow<Map<Long, ContactProfile>>(emptyMap())
    override val downloads: StateFlow<Map<Long, Double>> = MutableStateFlow(emptyMap())

    override fun name(num: Long, radioName: String?): String = contacts.value[num]?.nickname
        ?: contacts.value[num]?.card?.displayName?.takeIf { it.isNotEmpty() }
        ?: radioName?.takeIf { it.isNotEmpty() }
        ?: PangeaCode.code(num)

    override fun myPhotoData(): ByteString? = null

    override fun photoData(num: Long): ByteString? = null

    override fun updateMe(displayName: String, emoji: String, color: Int, photo: PhotoChange?) {
        me.value = me.value.copy(displayName = displayName, emoji = emoji, color = color)
    }

    override fun setNickname(nickname: String, num: Long) {
        contacts.value += num to (contacts.value[num] ?: ContactProfile()).copy(nickname = nickname.ifBlank { null })
    }

    override fun declinePhoto(num: Long) = Unit

    override fun setContact(saved: Boolean, num: Long, radioName: String?) {
        contacts.value +=
            num to (contacts.value[num] ?: ContactProfile()).copy(isContact = saved, contactName = radioName)
    }

    override fun contactNums(radioName: (Long) -> String?): List<Long> =
        contacts.value.filter { it.value.isContact }.keys.sorted()

    override val chatIcons = MutableStateFlow<Map<String, ChatIcon>>(emptyMap())

    override fun setChatIcon(key: String, icon: ChatIcon?) {
        chatIcons.value = if (icon == null) chatIcons.value - key else chatIcons.value + (key to icon)
    }

    override fun eraseAll() {
        me.value = MyProfile()
        contacts.value = emptyMap()
    }
}
