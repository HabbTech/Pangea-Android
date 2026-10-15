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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import okio.ByteString
import okio.FileSystem
import okio.Path
import org.koin.core.annotation.Single
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.database.getDatabaseDirectory
import org.meshtastic.core.database.getFileSystem
import org.meshtastic.core.model.pangea.CardSource
import org.meshtastic.core.model.pangea.ContactProfile
import org.meshtastic.core.model.pangea.GroupSend
import org.meshtastic.core.model.pangea.MyProfile
import org.meshtastic.core.model.pangea.PangeaCode
import org.meshtastic.core.model.pangea.PangeaWire
import org.meshtastic.core.model.pangea.ProfileCard
import org.meshtastic.core.model.pangea.ProfileLedger
import org.meshtastic.core.model.pangea.SavedProfiles
import org.meshtastic.core.model.pangea.cleanedName
import org.meshtastic.core.repository.PangeaProfileStore
import org.meshtastic.core.repository.PhotoChange
import kotlin.concurrent.Volatile

/**
 * Profiles live in their own folder, separate from the Meshtastic database (Pangea-Apple ProfileStore.swift). A file
 * that can't be read is set aside, never written over: nicknames can't be fetched again.
 */
@Single(binds = [PangeaProfileStore::class, PangeaProfileFiles::class])
class PangeaProfileStoreImpl : PangeaProfileFiles(getFileSystem(), defaultFolder())

/** The store itself, on any file system and folder (tests use their own). */
@Suppress("TooManyFunctions", "ReturnCount")
open class PangeaProfileFiles(private val fileSystem: FileSystem, private val folder: Path) : PangeaProfileStore {
    private val lock = Any()
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    private val _me = MutableStateFlow(MyProfile())
    override val me: StateFlow<MyProfile> = _me.asStateFlow()

    private val _contacts = MutableStateFlow<Map<Long, ContactProfile>>(emptyMap())
    override val contacts: StateFlow<Map<Long, ContactProfile>> = _contacts.asStateFlow()

    private val _downloads = MutableStateFlow<Map<Long, Double>>(emptyMap())
    override val downloads: StateFlow<Map<Long, Double>> = _downloads.asStateFlow()

    private var groupSends: Map<String, GroupSend> = emptyMap()
    private var ledger = ProfileLedger()

    @Volatile private var myPhotoCache: ByteString? = null

    init {
        runCatching { fileSystem.createDirectories(folder) }
        load()
    }

    private val file: Path
        get() = folder / FILE_NAME

    private fun photoPath(name: String): Path = folder / "$name.jpg"

    private fun load() {
        if (!fileSystem.exists(file)) return
        val text = runCatching { fileSystem.read(file) { readUtf8() } }.getOrNull() ?: return
        val saved = runCatching { json.decodeFromString(SavedProfiles.serializer(), text) }.getOrNull()
        if (saved == null) {
            val aside = setAsidePath()
            Logger.e { "👤 [Profiles] Couldn't read $FILE_NAME, set aside as ${aside.name}" }
            runCatching { fileSystem.atomicMove(file, aside) }
            return
        }
        _me.value = saved.me.copy(emoji = ProfileCard.cleanEmoji(saved.me.emoji))
        _contacts.value = saved.contacts.mapValues { (_, profile) -> profile.copy(card = profile.card?.cleaned) }
        groupSends = saved.groupSends
        ledger = saved.ledger
    }

    private fun setAsidePath(): Path {
        val first = folder / "profiles.corrupt.json"
        return if (!fileSystem.exists(first)) first else folder / "profiles.corrupt-$nowMillis.json"
    }

    private fun save() {
        val saved = SavedProfiles(_me.value, _contacts.value, groupSends, ledger)
        val text = json.encodeToString(SavedProfiles.serializer(), saved)
        val temp = folder / "$FILE_NAME.tmp"
        runCatching {
            fileSystem.write(temp) { writeUtf8(text) }
            fileSystem.atomicMove(temp, file)
        }
            .onFailure { Logger.e(it) { "👤 [Profiles] Save failed" } }
    }

    private fun mutateContact(num: Long, block: (ContactProfile) -> ContactProfile) {
        _contacts.update { it + (num to block(it[num] ?: ContactProfile())) }
    }

    // MARK: Me

    override fun myPhotoData(): ByteString? {
        if (_me.value.photoVersion == 0L) return null
        myPhotoCache?.let {
            return it
        }
        return readBytes(photoPath(ME)).also { myPhotoCache = it }
    }

    override fun updateMe(displayName: String, emoji: String, color: Int, photo: PhotoChange?) = synchronized(lock) {
        var profile =
            _me.value.copy(
                displayName = displayName.cleanedName(ProfileCard.MAX_NAME_BYTES),
                emoji = ProfileCard.cleanEmoji(emoji),
                color = color and BYTE_MASK,
            )
        when (photo) {
            is PhotoChange.Set -> {
                writeBytes(photoPath(ME), photo.jpeg)
                profile = profile.copy(photoVersion = PangeaWire.hash(photo.jpeg).toLong())
                myPhotoCache = photo.jpeg
            }

            PhotoChange.Remove -> {
                runCatching { fileSystem.delete(photoPath(ME)) }
                profile = profile.copy(photoVersion = 0)
                myPhotoCache = null
            }

            null -> Unit
        }
        _me.value = profile
        save()
    }

    // MARK: Other people

    override fun photoData(num: Long): ByteString? {
        val profile = _contacts.value[num] ?: return null
        if (profile.savedPhotoVersion == 0L) return null
        return readBytes(photoPath(num.toString()))
    }

    /**
     * [channel] is the group it was heard in, or null for a private (PKI) card. Returns false when it was ignored:
     * anyone in a group can send a card in someone else's name, so a group card never replaces a private one.
     */
    fun receivedCard(card: ProfileCard, from: Long, channel: Int?, groupKey: String? = null): Boolean =
        synchronized(lock) {
            val existing = _contacts.value[from] ?: ContactProfile()
            if (channel != null && existing.cardSource == CardSource.DIRECT) return false
            var profile =
                existing.copy(card = card, cardSource = if (channel == null) CardSource.DIRECT else CardSource.GROUP)
            if (channel != null) profile = profile.copy(lastChannel = channel, lastGroupKey = groupKey)
            if (card.photoVersion == 0L && profile.savedPhotoVersion != 0L) {
                // They removed their photo.
                profile = profile.copy(savedPhotoVersion = 0)
                runCatching { fileSystem.delete(photoPath(from.toString())) }
            }
            _contacts.update { it + (from to profile) }
            save()
            true
        }

    fun savePhoto(data: ByteString, version: Long, num: Long) = synchronized(lock) {
        writeBytes(photoPath(num.toString()), data)
        mutateContact(num) { it.copy(savedPhotoVersion = version) }
        setDownload(num, null)
        save()
    }

    fun setDownload(num: Long, progress: Double?) {
        _downloads.update { if (progress == null) it - num else it + (num to progress) }
    }

    override fun declinePhoto(num: Long) = synchronized(lock) {
        val version = _contacts.value[num]?.card?.photoVersion ?: return
        mutateContact(num) { it.copy(declinedPhotoVersion = version) }
        save()
    }

    override fun setNickname(nickname: String, num: Long) = synchronized(lock) {
        val trimmed = nickname.trim()
        mutateContact(num) { it.copy(nickname = trimmed.ifEmpty { null }) }
        save()
    }

    // MARK: Contacts

    override fun setContact(saved: Boolean, num: Long, radioName: String?) = synchronized(lock) {
        mutateContact(num) { profile ->
            if (saved) {
                profile.copy(
                    isContact = true,
                    contactName = radioName?.takeIf { it.isNotEmpty() } ?: profile.contactName,
                )
            } else {
                profile.copy(isContact = false, contactName = null)
            }
        }
        save()
    }

    override fun contactNums(radioName: (Long) -> String?): List<Long> = _contacts.value
        .filter { it.value.isContact }
        .map { (num, profile) -> num to name(num, radioName(num) ?: profile.contactName) }
        .sortedWith(
            compareBy<Pair<Long, String>, String>(String.CASE_INSENSITIVE_ORDER) { it.second }.thenBy { it.first },
        )
        .map { it.first }

    fun markCardSent(version: Long, num: Long) = synchronized(lock) {
        mutateContact(num) { it.copy(sentCardVersion = version) }
        save()
    }

    /** Due when the card changed, a day after the last send, or when the clock went back past the last send. */
    fun needsGroupSend(key: String, version: Long, intervalMillis: Long = DAY_MILLIS, now: Long = nowMillis): Boolean {
        val last = groupSends[key] ?: return true
        val since = now - last.epochMillis
        return last.version != version || since > intervalMillis || since < 0
    }

    fun markGroupSent(key: String, version: Long) = synchronized(lock) {
        groupSends = groupSends + (key to GroupSend(version, nowMillis))
        save()
    }

    /** Switching or forgetting a radio: they'll need my card again since it came from a different radio. */
    fun resetSentCards() = synchronized(lock) {
        _contacts.update { contacts -> contacts.mapValues { it.value.copy(sentCardVersion = 0) } }
        groupSends = emptyMap()
        save()
    }

    // MARK: Ledger

    val currentLedger: ProfileLedger
        get() = ledger

    fun recordMessaged(num: Long, byMe: Boolean) = synchronized(lock) {
        ledger =
            if (byMe) {
                ledger.copy(messagedByMe = ledger.messagedByMe + (num to nowMillis))
            } else {
                ledger.copy(messagedMe = ledger.messagedMe + (num to nowMillis))
            }
        save()
    }

    fun recordGroupPost(groupKey: String, num: Long) = synchronized(lock) {
        val posters = ledger.groupPosters[groupKey].orEmpty()
        if (num in posters) return
        ledger = ledger.copy(groupPosters = ledger.groupPosters + (groupKey to posters + num))
        save()
    }

    override fun eraseAll() = synchronized(lock) {
        _me.value = MyProfile()
        _contacts.value = emptyMap()
        _downloads.value = emptyMap()
        groupSends = emptyMap()
        ledger = ProfileLedger()
        myPhotoCache = null
        runCatching { fileSystem.list(folder).filter { it.name != FILE_NAME }.forEach { fileSystem.delete(it) } }
        save()
    }

    // MARK: Names

    override fun name(num: Long, radioName: String?): String {
        val profile = _contacts.value[num]
        profile?.nickname?.let {
            return it
        }
        val display = profile?.card?.displayName?.let(ProfileCard::cleanName).orEmpty()
        if (display.isNotEmpty()) return display
        if (!radioName.isNullOrEmpty()) return radioName
        return PangeaCode.code(num)
    }

    private fun readBytes(path: Path): ByteString? =
        runCatching { if (fileSystem.exists(path)) fileSystem.read(path) { readByteString() } else null }.getOrNull()

    private fun writeBytes(path: Path, data: ByteString) {
        val temp = path.parent!! / "${path.name}.tmp"
        runCatching {
            fileSystem.write(temp) { write(data) }
            fileSystem.atomicMove(temp, path)
        }
            .onFailure { Logger.e(it) { "👤 [Profiles] Couldn't save ${path.name}" } }
    }

    companion object {
        private const val FILE_NAME = "profiles.json"
        private const val ME = "me"
        private const val BYTE_MASK = 0xFF
        private const val DAY_MILLIS = 24L * 60 * 60 * 1000
    }
}

private fun defaultFolder(): Path = getDatabaseDirectory().parent!! / "PangeaProfiles"
