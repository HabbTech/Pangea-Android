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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A photo coming in. The card it was asked for says how many pieces there are and the tag of each manifest part; each
 * manifest part gives the tag of each piece. So nothing from the air is taken on trust: a part or piece that doesn't
 * match is never stored and never undoes progress, and the piece count never comes from a piece.
 */
class PhotoDownload private constructor(val version: UInt, val pieceCount: Int, val partTags: List<ByteString>) {
    enum class Outcome {
        /** Checked and kept. */
        ADDED,

        /** Already have it. */
        DUPLICATE,

        /** Kept aside until its manifest part arrives to check it. */
        HELD,

        /** Not part of this photo. */
        IGNORED,
    }

    private val tags = mutableMapOf<Int, List<ByteString>>()
    private val pieces = mutableMapOf<Int, ByteString>()

    /** Pieces that came before their manifest part: a few different copies each, at most. */
    private val unchecked = mutableMapOf<Int, MutableList<ByteString>>()

    val slotCount: Int
        get() = partTags.size + pieceCount

    /** Slots still needed (manifest parts first, then pieces). A held piece isn't asked for until it's checked. */
    val missingSlots: Set<Int>
        get() = buildSet {
            for (part in partTags.indices) if (tags[part] == null) add(part)
            for (index in 0 until pieceCount) {
                if (pieces[index] == null && unchecked[index] == null) add(partTags.size + index)
            }
        }

    val isComplete: Boolean
        get() = pieces.size == pieceCount

    val progress: Double
        get() = if (slotCount == 0) 0.0 else (tags.size + pieces.size).toDouble() / slotCount

    fun accept(manifest: PhotoManifest): Outcome {
        val part = manifest.part
        if (manifest.photoVersion != version || part >= partTags.size) return Outcome.IGNORED
        if (tags[part] != null) return Outcome.DUPLICATE
        val covered = minOf(PhotoManifest.TAGS_PER_PART, pieceCount - part * PhotoManifest.TAGS_PER_PART)
        if (manifest.tags.size != covered || manifest.tag != partTags[part]) return Outcome.IGNORED
        tags[part] = manifest.tags
        // Check the pieces that came early; the ones that don't match are asked for again.
        for (offset in 0 until covered) {
            val index = part * PhotoManifest.TAGS_PER_PART + offset
            val copies = unchecked.remove(index) ?: continue
            copies.firstOrNull { PangeaWire.tag(it) == manifest.tags[offset] }?.let { pieces[index] = it }
        }
        return Outcome.ADDED
    }

    fun accept(chunk: PhotoChunk): Outcome {
        if (chunk.photoVersion != version || chunk.index >= pieceCount) return Outcome.IGNORED
        if (pieces[chunk.index] != null) return Outcome.DUPLICATE
        val part = chunk.index / PhotoManifest.TAGS_PER_PART
        val pieceTags = tags[part]
        if (pieceTags == null) {
            val copies = unchecked.getOrPut(chunk.index) { mutableListOf() }
            if (chunk.bytes in copies || copies.size >= MAX_UNCHECKED_COPIES) {
                if (copies.isEmpty()) unchecked.remove(chunk.index)
                return Outcome.IGNORED
            }
            copies += chunk.bytes
            return Outcome.HELD
        }
        if (PangeaWire.tag(chunk.bytes) != pieceTags[chunk.index % PhotoManifest.TAGS_PER_PART]) return Outcome.IGNORED
        pieces[chunk.index] = chunk.bytes
        return Outcome.ADDED
    }

    /** The photo, once every piece is in and it's the version the card named. */
    fun assemble(): ByteString? = PhotoChunk.assemble(pieces, pieceCount, version)

    companion object {
        const val MAX_UNCHECKED_COPIES = 3

        fun from(card: ProfileCard): PhotoDownload? {
            if (!card.hasPhotoLayout) return null
            return PhotoDownload(card.photoVersion.toUInt(), card.photoPieces, card.photoPartTags)
        }
    }
}

/** Who a photo send is for: a group channel (one broadcast every member there takes), or one person, privately. */
sealed interface PhotoAudience {
    data class Group(val channel: Int, val key: String) : PhotoAudience

    data class Person(val num: Long) : PhotoAudience

    val isGroup: Boolean
        get() = this is Group
}

/**
 * Decides which photo pieces go on the air, and in what order. Budgets are per group and per person, for one photo
 * version, with groups and people kept apart:
 * - Anyone in a group can claim to be anyone, so group sends are keyed by the group, never by who asked.
 * - Private requests come from a key the radio checked, so they're budgeted per person.
 * - A request for pieces already queued for the same group or person adds nothing.
 * - A new photo version gets a new allowance.
 *
 * Instants are monotonic offsets ([Duration] since an arbitrary origin) so changing the phone's clock can't stretch a
 * window.
 */
class PhotoSendLimiter {
    data class Key(val audience: PhotoAudience, val version: UInt)

    data class Handout(val key: Key, val slot: Int)

    private data class Job(val key: Key, val remaining: MutableSet<Int>, var lastProgress: Duration)

    private data class SentPiece(val key: Key, val at: Duration)

    private val jobs = mutableListOf<Job>()
    private val sent = mutableListOf<SentPiece>()
    private var turn = 0

    /** Whose piece [next] handed out, until it's delivered, put back or cancelled. */
    private var inFlight: Key? = null

    val isIdle: Boolean
        get() = jobs.isEmpty()

    val queuedCount: Int
        get() = jobs.size

    fun charged(key: Key, now: Duration): Int = sent.count { it.key == key && now - it.at < WINDOW }

    fun charged(groups: Boolean, now: Duration): Int =
        sent.count { it.key.audience.isGroup == groups && now - it.at < WINDOW }

    fun queued(key: Key): Set<Int> = jobs.firstOrNull { it.key == key }?.remaining?.toSet() ?: emptySet()

    /** Queues the wanted slots (null: all of them). False when the request is ignored. */
    @Suppress("ReturnCount")
    fun admit(wanted: Set<Int>?, slotCount: Int, key: Key, now: Duration): Boolean {
        if (slotCount <= 0 || slotCount > MAX_SLOT_INDEX) return false
        val all = (0 until slotCount).toSet()
        val asked = wanted?.intersect(all) ?: all
        if (asked.isEmpty()) return false
        sent.removeAll { now - it.at >= WINDOW }

        val existing = jobs.indexOfFirst { it.key == key }.takeIf { it >= 0 }
        val new = asked - (existing?.let { jobs[it].remaining } ?: emptySet())
        // Already on its way to them. Asked for again: they're still waiting, so it's progress.
        if (new.isEmpty()) {
            existing?.let { jobs[it].lastProgress = now }
            return true
        }
        // The piece handed out holds its job's place while it's out, unless that job is queued.
        val held = inFlight?.let { flight -> flight != key && jobs.none { it.key == flight } } ?: false
        if (existing == null && jobs.size + (if (held) 1 else 0) >= MAX_QUEUED) return false

        val forKey = sent.count { it.key == key } + (existing?.let { jobs[it].remaining.size } ?: 0)
        if (forKey + new.size > slotCount * PHOTOS_PER_KEY) return false
        val isGroup = key.audience.isGroup
        val forClass =
            sent.count { it.key.audience.isGroup == isGroup } +
                jobs.filter { it.key.audience.isGroup == isGroup }.sumOf { it.remaining.size }
        if (forClass + new.size > CLASS_SLOTS) return false

        if (existing != null) {
            jobs[existing].remaining += new
            jobs[existing].lastProgress = now
        } else {
            jobs += Job(key, new.toMutableSet(), now)
        }
        return true
    }

    /**
     * The next slot to send, taking turns between groups and people. Counted as sent now; [putBack] if it doesn't go.
     */
    fun next(now: Duration): Handout? {
        while (true) {
            // One held up that long (no radio) is skipped: they've given up waiting, and ask again for what they miss.
            jobs.removeAll { now - it.lastProgress > MAX_WAIT }
            if (jobs.isEmpty()) return null
            val index = turn % jobs.size
            val slot = jobs[index].remaining.minOrNull()
            if (slot == null) {
                jobs.removeAt(index)
                continue
            }
            val key = jobs[index].key
            jobs[index].remaining.remove(slot)
            jobs[index].lastProgress = now
            sent += SentPiece(key, now)
            inFlight = key
            if (jobs[index].remaining.isEmpty()) {
                jobs.removeAt(index)
                turn = index
            } else {
                turn = index + 1
            }
            return Handout(key, slot)
        }
    }

    /** Nobody wants the rest of this one. The slot handed out by [next] didn't go. */
    fun cancel(key: Key) {
        if (inFlight == key) inFlight = null
        refund(key)
        jobs.removeAll { it.key == key }
    }

    /** The piece [next] handed out for [key] went. */
    fun delivered(key: Key) {
        if (inFlight == key) inFlight = null
    }

    /** A piece [next] handed out couldn't go (no radio): queued again, uncharged, for when the radio's back. */
    fun putBack(unsent: Handout) {
        if (inFlight == unsent.key) inFlight = null
        val handedOut = refund(unsent.key) ?: return
        val index = jobs.indexOfFirst { it.key == unsent.key }
        if (index >= 0) {
            jobs[index].remaining += unsent.slot
        } else {
            // It was that job's last piece; admit kept its place while it was out.
            jobs += Job(unsent.key, mutableSetOf(unsent.slot), handedOut)
        }
    }

    /** Takes back the last piece sent to [key]; returns when it was handed out. */
    private fun refund(key: Key): Duration? {
        val last = sent.indexOfLast { it.key == key }
        if (last < 0) return null
        return sent.removeAt(last).at
    }

    companion object {
        val WINDOW: Duration = (15 * 60).seconds

        /** Each group, and each person, can get the whole photo twice per window, so one lost send can be redone. */
        const val PHOTOS_PER_KEY = 2

        /** Across all groups, and separately across all people. */
        const val PHOTOS_PER_CLASS = 3

        val CLASS_SLOTS: Int = PhotoRequest.MAX_SLOTS * PHOTOS_PER_CLASS

        /** Groups and people waiting at once. */
        const val MAX_QUEUED = 4

        /** Between pieces, so regular messages still get through. */
        val SPACING: Duration = 3.seconds

        /** Their app gives up after about four quiet minutes. */
        val MAX_WAIT: Duration = 240.seconds

        private const val MAX_SLOT_INDEX = 255
    }
}
