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

import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Where my card goes: one person privately, or a group ("index:name:key hash"). */
sealed interface CardTarget {
    data class Person(val num: Long) : CardTarget

    data class Group(val index: Int, val key: String) : CardTarget
}

/**
 * Every profile packet but photo pieces waits here before it goes on the air (pieces take turns with them, see [next]).
 * Whatever the packet, it's spread out (a random delay when queued, and a gap between any two packets), queued once
 * (queuing the same thing again merges), kept apart by why it goes ([Reason]: each has its own room, cap and turn),
 * charged only for airtime used, and kept while the radio is away. What a packet carries is worked out when it goes,
 * not here. Instants are monotonic offsets so it can be tested without a radio. Port of Pangea-Apple `ProfileOutbox`.
 */
@Suppress("TooManyFunctions")
class ProfileOutbox(private val random: Random = Random.Default) {
    sealed interface Item {
        /** My card. One that answers their ask is sent even if they have it (see [Entry.force]). */
        data class Card(val target: CardTarget) : Item

        /** Asking this person, privately, for their card. */
        data class CardAsk(val num: Long) : Item

        /** Asking this person for their photo, for the download in progress. */
        data class PhotoAsk(val num: Long) : Item
    }

    /** Why a packet goes. Declaration order is priority. */
    enum class Reason {
        /** My card, after I message someone or post in a group. */
        MY_MESSAGE,

        /** My card, after I edit my profile. */
        MY_EDIT,

        /** My card back to someone whose card just came. */
        REPLY,

        /** My card to someone who asked for it. */
        ANSWER,

        /** Asking someone for their photo. */
        PHOTO_ASK,

        /** Asking someone, privately, for their card. */
        ASK,
    }

    /** Where an entry waits its turn on the lane: [MINE] first, then the others and photo pieces in turn. */
    enum class LaneClass {
        MINE,
        EDIT,
        INBOUND,
        PHOTO_ASKS,
        ASKS,
        PIECES,
    }

    data class Entry(
        val item: Item,
        var due: Duration,
        val reasons: MutableSet<Reason>,
        /** For an ask: the group card that made me ask (the latest one heard). */
        var hint: ProfileCard? = null,
        /** For a photo ask: only the pieces still missing. */
        var isReask: Boolean = false,
        /** When it was first queued. */
        var queued: Duration,
        /** When it was first due with no reason letting it through; cleared once one does. */
        var blockedSince: Duration? = null,
        /** The reason it went under, set when [next] hands it out. */
        var admittedAs: Reason? = null,
        /** The reason whose room it takes: the one it was first queued for, kept through merges. */
        val roomReason: Reason? = null,
    ) {
        /** My card to someone who asked for it: sent even if they have its version. */
        val force: Boolean
            get() = Reason.ANSWER in reasons

        /** The reason it waits under on the lane. */
        val lead: Reason
            get() = reasons.minOrNull() ?: Reason.ASK

        val laneClass: LaneClass
            get() = laneClass(lead)
    }

    /** What the lane sends next. */
    sealed interface Pick {
        data class Send(val entry: Entry) : Pick

        /** A photo piece (see [PhotoSendLimiter.next]). */
        data object Piece : Pick
    }

    /** How a send [next] handed out went. */
    enum class Outcome {
        /** Not needed any more; nothing went. */
        NOT_NEEDED,
        SENT,

        /** It reached the radio, which refused it. */
        FAILED,

        /** No radio. */
        NO_RADIO,
    }

    private data class Sent(
        val item: Item,
        val reasons: Set<Reason>,
        val chargedTo: Reason,
        val hint: ProfileCard?,
        val at: Duration,
    )

    private val _entries = mutableListOf<Entry>()
    val entries: List<Entry>
        get() = _entries

    private val sent = mutableListOf<Sent>()

    /** When the last profile packet of any kind, photo pieces included, went to the radio. */
    private var lastSend: Duration? = null

    /** Where the lane is in [CYCLE]. */
    private var cursor = 0

    /** Other packets handed out in a row while photo pieces wait. */
    private var sinceLastPiece = 0

    /** The entry [next] handed out, until [finish]: it keeps its place in its room, since it may come back. */
    private var inFlight: Entry? = null

    /**
     * Queues [item] to go after a random delay in [delay] (seconds). If it's already waiting, the two become one: the
     * sooner time, both reasons, the newer hint, and all missing pieces if either asked for all. False when [reason]
     * has no room left.
     */
    fun schedule(
        item: Item,
        reason: Reason,
        delay: ClosedFloatingPointRange<Double>,
        now: Duration,
        hint: ProfileCard? = null,
        isReask: Boolean = false,
    ): Boolean {
        val seconds =
            if (delay.start >= delay.endInclusive) delay.start else random.nextDouble(delay.start, delay.endInclusive)
        val due = now + (seconds * MILLIS_PER_SECOND).toLong().milliseconds
        val existing = _entries.firstOrNull { it.item == item }
        if (existing != null) {
            existing.due = minOf(existing.due, due)
            existing.reasons += reason
            existing.hint = hint ?: existing.hint
            existing.isReask = existing.isReask && isReask
            return true
        }
        // One in flight for another item holds its place, unless something queued meanwhile for its item already
        // does. One for this same item doesn't: it would merge back into this.
        val flight = inFlight
        val held =
            flight != null &&
                (flight.roomReason ?: flight.lead) == reason &&
                flight.item != item &&
                !contains(flight.item)
        val inRoom = _entries.count { (it.roomReason ?: it.lead) == reason } + (if (held) 1 else 0)
        if (inRoom >= room(reason)) return false
        _entries += Entry(item, due, mutableSetOf(reason), hint, isReask, queued = now, roomReason = reason)
        return true
    }

    fun contains(item: Item): Boolean = _entries.any { it.item == item }

    /** Not wanted any more (a photo download given up). */
    fun remove(item: Item) {
        _entries.removeAll { it.item == item }
    }

    /** The earliest a packet of any kind may go: [SPACING] after the last one. */
    fun sendableAt(now: Duration): Duration = lastSend?.let { maxOf(now, it + SPACING) } ?: now

    /** Sends counted against [reason]'s cap right now. */
    fun charged(reason: Reason, now: Duration): Int = sent.count { it.chargedTo == reason && now - it.at < WINDOW }

    /** The earliest, from [now] on, that [reason] lets this entry through if nothing else goes meanwhile. */
    fun admitsAt(reason: Reason, entry: Entry, now: Duration): Duration {
        var at = now
        cap(reason)?.let { cap ->
            val times = sent.filter { it.chargedTo == reason && now - it.at < WINDOW }.map { it.at }.sorted()
            if (times.size >= cap) at = maxOf(at, times[times.size - cap] + WINDOW)
        }
        // Any send that carried this reason counts, whatever it was charged to.
        sent
            .lastOrNull { it.item == entry.item && reason in it.reasons }
            ?.let { last ->
                when (reason) {
                    Reason.MY_EDIT -> at = maxOf(at, last.at + EDIT_INTERVAL)

                    Reason.ANSWER -> at = maxOf(at, last.at + ANSWER_INTERVAL)

                    Reason.ASK -> at = maxOf(at, last.at + if (last.hint != entry.hint) ASK_REARM else ASK_INTERVAL)

                    Reason.MY_MESSAGE,
                    Reason.REPLY,
                    Reason.PHOTO_ASK,
                    -> Unit
                }
            }
        return at
    }

    /** The reason an entry may go under now, by priority; null while every one holds it back. */
    fun admittingReason(entry: Entry, now: Duration): Reason? =
        entry.reasons.sorted().firstOrNull { admitsAt(it, entry, now) <= now }

    /**
     * What to send now, or null while it's too soon after the last packet or nothing may go. My own cards go first.
     * Then the lane takes turns ([CYCLE]), skipping whatever has nothing to send. While pieces wait, one goes after at
     * most [MAX_BETWEEN_PIECES] others. An entry handed out is taken out of the outbox; give it to [finish] once it's
     * gone.
     */
    @Suppress("ReturnCount")
    fun next(now: Duration, piecesWaiting: Boolean): Pick? {
        if (sendableAt(now) > now) return null
        prune(now)
        for (entry in _entries) {
            if (entry.due > now) continue
            if (admittingReason(entry, now) == null) {
                if (entry.blockedSince == null) entry.blockedSince = now
            } else {
                entry.blockedSince = null
            }
        }
        _entries.removeAll { entry ->
            (entry.blockedSince?.let { now - it > MAX_BLOCKED_AGE } ?: false) || now - entry.queued > MAX_QUEUED_AGE
        }
        if (piecesWaiting && sinceLastPiece >= MAX_BETWEEN_PIECES) {
            sinceLastPiece = 0
            return Pick.Piece
        }
        earliest(LaneClass.MINE, now)?.let {
            return Pick.Send(handOut(it, now, piecesWaiting))
        }
        for (step in CYCLE.indices) {
            val turn = (cursor + step) % CYCLE.size
            val laneClass = CYCLE[turn]
            if (laneClass == LaneClass.PIECES) {
                if (!piecesWaiting) continue
                cursor = turn + 1
                sinceLastPiece = 0
                return Pick.Piece
            }
            val index = earliest(laneClass, now)
            if (index != null) {
                cursor = turn + 1
                return Pick.Send(handOut(index, now, piecesWaiting))
            }
        }
        return null
    }

    private fun handOut(index: Int, now: Duration, piecesWaiting: Boolean): Entry {
        sinceLastPiece = if (piecesWaiting) sinceLastPiece + 1 else 0
        val entry = _entries.removeAt(index)
        entry.admittedAs = admittingReason(entry, now)
        inFlight = entry
        return entry
    }

    /** When to look again, not before [SPACING] is over. Null when nothing waits. */
    fun nextWake(now: Duration, piecesWaiting: Boolean): Duration? {
        val times =
            _entries
                .map { entry -> maxOf(entry.due, entry.reasons.minOfOrNull { admitsAt(it, entry, now) } ?: now) }
                .toMutableList()
        if (piecesWaiting) times += now
        val soonest = times.minOrNull() ?: return null
        return maxOf(soonest, sendableAt(now))
    }

    /**
     * After [next] handed out [entry]. A send that reached the radio (sent, or refused by it) is charged. One that
     * found no radio goes back as it was, uncharged, and true says the lane should wait for the radio.
     */
    fun finish(entry: Entry, outcome: Outcome, now: Duration): Boolean {
        if (inFlight?.item == entry.item) inFlight = null
        return when (outcome) {
            Outcome.SENT,
            Outcome.FAILED,
            -> {
                prune(now)
                sent += Sent(entry.item, entry.reasons.toSet(), entry.admittedAs ?: entry.lead, entry.hint, now)
                lastSend = now
                false
            }

            Outcome.NO_RADIO -> {
                putBack(entry)
                true
            }

            Outcome.NOT_NEEDED -> false
        }
    }

    /** A photo piece went: the next packet of any kind waits [SPACING] after it. */
    fun sentPhotoPiece(now: Duration) {
        lastSend = now
    }

    private fun earliest(laneClass: LaneClass, now: Duration): Int? = _entries.indices
        .filter {
            _entries[it].laneClass == laneClass &&
                _entries[it].due <= now &&
                admittingReason(_entries[it], now) != null
        }
        .minByOrNull { _entries[it].due }

    /** Back as it was when taken; merged into the same item if it was queued again meanwhile. */
    private fun putBack(entry: Entry) {
        entry.admittedAs = null
        val existing = _entries.firstOrNull { it.item == entry.item }
        if (existing == null) {
            _entries += entry
            return
        }
        existing.due = minOf(existing.due, entry.due)
        existing.reasons += entry.reasons
        existing.hint = existing.hint ?: entry.hint
        existing.isReask = existing.isReask && entry.isReask
        existing.queued = minOf(existing.queued, entry.queued)
    }

    private fun prune(now: Duration) {
        val longest = maxOf(WINDOW, EDIT_INTERVAL, ANSWER_INTERVAL, ASK_INTERVAL)
        sent.removeAll { now - it.at >= longest }
    }

    companion object {
        private const val MILLIS_PER_SECOND = 1000.0

        // Delays, in seconds, between queuing and sending.

        /** My card after I message someone or post in a group, or as an answer: the message goes first. */
        val AFTER_MESSAGE = 2.0..5.0

        /** My card back to someone whose card just came privately. */
        val CARD_BACK = 2.0..5.0

        /** My card back to a group, spread out so a new member doesn't get every reply at once. */
        val CARD_BACK_TO_GROUP = 3.0..15.0

        /** After editing my profile, to each person and group. */
        val AFTER_EDIT = 1.0..10.0

        /** Asking for a card: everyone who heard the same group card doesn't ask at the same moment. */
        val ASK_DELAY = 5.0..60.0

        /** Asking again for missing photo pieces. */
        val REASK = 0.0..5.0

        val NOW = 0.0..0.0

        /** Photo downloads going at once. */
        const val MAX_TRANSFERS = 6

        /** Asks per download (the first and three more) before it's given up. */
        const val MAX_PHOTO_ATTEMPTS = 4

        val MIN_PHOTO_PATIENCE: Duration = 60.seconds
        val MAX_PHOTO_PATIENCE: Duration = 180.seconds

        /** A download is given up once the radio has been away this long. */
        val MAX_RADIO_AWAY: Duration = (10 * 60).seconds

        /** Linked but not taking packets: the lane tries again after this, doubling up to [MAX_BACKOFF]. */
        val FIRST_BACKOFF: Duration = 5.seconds
        val MAX_BACKOFF: Duration = 60.seconds

        /** Other packets, my own cards included, between two photo pieces while pieces wait. */
        const val MAX_BETWEEN_PIECES = 3

        val WINDOW: Duration = (10 * 60).seconds
        const val REPLIES_PER_WINDOW = 12
        const val ANSWERS_PER_WINDOW = 12
        const val PHOTO_ASKS_PER_WINDOW = MAX_TRANSFERS * MAX_PHOTO_ATTEMPTS
        const val ASKS_PER_WINDOW = 3
        val EDIT_INTERVAL: Duration = 60.seconds
        val ANSWER_INTERVAL: Duration = (10 * 60).seconds
        val ASK_INTERVAL: Duration = (30 * 60).seconds
        val ASK_REARM: Duration = (5 * 60).seconds
        val SPACING: Duration = PhotoSendLimiter.SPACING
        val MAX_BLOCKED_AGE: Duration = (60 * 60).seconds
        val MAX_QUEUED_AGE: Duration = (6 * 60 * 60).seconds

        /** The lane's turns after MINE: photo pieces every other one. */
        val CYCLE =
            listOf(
                LaneClass.EDIT,
                LaneClass.PIECES,
                LaneClass.INBOUND,
                LaneClass.PIECES,
                LaneClass.PHOTO_ASKS,
                LaneClass.PIECES,
                LaneClass.EDIT,
                LaneClass.PIECES,
                LaneClass.ASKS,
                LaneClass.PIECES,
            )

        fun laneClass(reason: Reason): LaneClass = when (reason) {
            Reason.MY_MESSAGE -> LaneClass.MINE

            Reason.MY_EDIT -> LaneClass.EDIT

            Reason.REPLY,
            Reason.ANSWER,
            -> LaneClass.INBOUND

            Reason.PHOTO_ASK -> LaneClass.PHOTO_ASKS

            Reason.ASK -> LaneClass.ASKS
        }

        /** Entries each reason may queue at once, counted by the reason each was first queued for. */
        @Suppress("MagicNumber")
        fun room(reason: Reason): Int = when (reason) {
            Reason.MY_MESSAGE,
            Reason.REPLY,
            -> 16

            Reason.MY_EDIT -> 32

            Reason.ANSWER -> 32

            Reason.PHOTO_ASK -> MAX_TRANSFERS

            Reason.ASK -> 8
        }

        /** Sends per [WINDOW] across everyone. My own cards aren't capped. */
        fun cap(reason: Reason): Int? = when (reason) {
            Reason.MY_MESSAGE,
            Reason.MY_EDIT,
            -> null

            Reason.REPLY -> REPLIES_PER_WINDOW

            Reason.ANSWER -> ANSWERS_PER_WINDOW

            Reason.PHOTO_ASK -> PHOTO_ASKS_PER_WINDOW

            Reason.ASK -> ASKS_PER_WINDOW
        }
    }
}
