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

import kotlin.time.Duration

/** How a profile packet reaches someone: privately to their key, or inside a group they're in. */
sealed interface ProfileRoute {
    data object Direct : ProfileRoute

    data class Group(val index: Int) : ProfileRoute
}

/** How a send went. Only [NO_RADIO] means nothing else can go out either. */
enum class ProfileSendResult {
    SENT,

    /** No radio connected. */
    NO_RADIO,

    /** This one didn't go (no key for them, or the radio refused it). */
    FAILED,
}

/** What came of asking for someone's photo. */
enum class PhotoAsk {
    /** Asked; it's downloading. */
    STARTED,

    /** Already downloading, this same version. */
    ALREADY_GOING,

    /** No radio to ask through. */
    NO_RADIO,

    /** [ProfileOutbox.MAX_TRANSFERS] are already going. */
    BUSY,

    /** No photo to ask for, or no way to reach them. */
    UNAVAILABLE,
}

/** Who gets profile traffic. Kept apart from the radio and the database so it can be tested (ProfileSync.swift). */
@Suppress("TooManyFunctions")
object ProfileRules {
    const val BROADCAST: Long = 0xFFFFFFFFL

    /**
     * How a profile packet arrived, or null when it has to be ignored: anything on the public channel, or on a channel
     * that isn't one of my Pangea groups.
     */
    fun route(pkiEncrypted: Boolean, to: Long, channel: Int, isGroupChat: (Int) -> Boolean): ProfileRoute? {
        if (pkiEncrypted && to != BROADCAST) return ProfileRoute.Direct
        if (channel <= 0 || !isGroupChat(channel)) return null
        return ProfileRoute.Group(channel)
    }

    /**
     * How to ask someone for their photo. Their app answers a private request only if they've messaged me (or think I'm
     * in their group); a request inside a group we share is always answered.
     */
    fun requestRoute(canReachDirectly: Boolean, theyMessagedMe: Boolean, sharedGroup: Int?): ProfileRoute? {
        if (canReachDirectly && theyMessagedMe) return ProfileRoute.Direct
        if (sharedGroup != null) return ProfileRoute.Group(sharedGroup)
        return if (canReachDirectly) ProfileRoute.Direct else null
    }

    /** My card and photo go to people I've messaged, people I saved to Contacts, and my groups. */
    fun mayShare(route: ProfileRoute, iMessagedThem: Boolean, inMyGroup: Boolean, savedContact: Boolean): Boolean =
        when (route) {
            is ProfileRoute.Group -> true
            ProfileRoute.Direct -> iMessagedThem || inMyGroup || savedContact
        }

    /** A group download belongs to the group it was asked in: its identity must still be what that slot holds now. */
    fun isSameGroup(askedIn: String?, now: String?): Boolean = askedIn != null && askedIn == now

    /** Whether a group card for someone whose card came privately is worth asking them about. */
    fun asksForCard(heard: ProfileCard, stored: ProfileCard?, canReachDirectly: Boolean): Boolean =
        canReachDirectly && heard != stored

    /** What's known when a queued packet comes up, for [stillNeeded]. */
    data class SendFacts(
        val myCardVersion: UInt? = null,
        val sentCardVersion: UInt? = null,
        val canReachDirectly: Boolean = false,
        val mayShare: Boolean = false,
        val sameGroup: Boolean = false,
        val needsGroupSend: Boolean = false,
        val storedCard: ProfileCard? = null,
        val transferExists: Boolean = false,
    )

    /** Whether a packet that waited in the outbox is still worth sending, as things are when it goes. */
    fun stillNeeded(entry: ProfileOutbox.Entry, facts: SendFacts): Boolean = when (val item = entry.item) {
        is ProfileOutbox.Item.Card ->
            when (item.target) {
                is CardTarget.Person -> {
                    val version = facts.myCardVersion
                    // An answer goes even if they have it: they're asking because they may not.
                    version != null &&
                        facts.canReachDirectly &&
                        facts.mayShare &&
                        (entry.force || facts.sentCardVersion != version)
                }

                is CardTarget.Group -> facts.myCardVersion != null && facts.sameGroup && facts.needsGroupSend
            }

        is ProfileOutbox.Item.CardAsk -> {
            // Their real card may have come while it waited.
            val hint = entry.hint
            hint != null && asksForCard(hint, facts.storedCard, facts.canReachDirectly)
        }

        is ProfileOutbox.Item.PhotoAsk -> facts.transferExists
    }

    /** How the lane books a send (see Pangea-Apple `ProfileRules.outcome`). */
    fun outcome(result: ProfileSendResult?, readyAfter: Boolean, sameLink: Boolean): ProfileOutbox.Outcome =
        when (result) {
            null -> ProfileOutbox.Outcome.NOT_NEEDED

            ProfileSendResult.SENT -> ProfileOutbox.Outcome.SENT

            ProfileSendResult.NO_RADIO -> ProfileOutbox.Outcome.NO_RADIO

            ProfileSendResult.FAILED ->
                if (readyAfter && sameLink) ProfileOutbox.Outcome.FAILED else ProfileOutbox.Outcome.NO_RADIO
        }

    /** After a send found no radio: null when it's gone (wait for it), else how long to back off. */
    fun afterNoRadio(linked: Boolean, lastBackoff: Duration?): Duration? {
        if (!linked) return null
        if (lastBackoff == null) return ProfileOutbox.FIRST_BACKOFF
        return minOf(lastBackoff * 2, ProfileOutbox.MAX_BACKOFF)
    }

    enum class PhotoRetry {
        WAIT,
        ASK_AGAIN,
        GIVE_UP,
    }

    /** What the photo watchdog does with a download. */
    fun photoRetry(
        quietFor: Duration,
        attempts: Int,
        askQueued: Boolean,
        stillAskedGroup: Boolean,
        radioAwayFor: Duration? = null,
        longestGap: Duration = Duration.ZERO,
    ): PhotoRetry {
        if (radioAwayFor != null) {
            return if (radioAwayFor > ProfileOutbox.MAX_RADIO_AWAY) PhotoRetry.GIVE_UP else PhotoRetry.WAIT
        }
        if (quietFor <= photoPatience(longestGap) || askQueued) return PhotoRetry.WAIT
        return if (attempts >= ProfileOutbox.MAX_PHOTO_ATTEMPTS || !stillAskedGroup) {
            PhotoRetry.GIVE_UP
        } else {
            PhotoRetry.ASK_AGAIN
        }
    }

    /** A minute, or five times the longest wait so far, up to three minutes. */
    @Suppress("MagicNumber")
    fun photoPatience(longestGap: Duration): Duration =
        minOf(maxOf(longestGap * 5, ProfileOutbox.MIN_PHOTO_PATIENCE), ProfileOutbox.MAX_PHOTO_PATIENCE)

    /** How long a sender took over a piece that just came, less the time my radio was away meanwhile. */
    fun pieceGap(now: Duration, lastActivity: Duration, awayInGap: Duration, awaySince: Duration?): Duration {
        val stillAway = awaySince?.let { now - maxOf(it, lastActivity) } ?: Duration.ZERO
        return maxOf(Duration.ZERO, now - lastActivity - awayInGap - stillAway)
    }

    /** What saving my profile does next. */
    enum class SaveAction {
        APPLY_NAME,
        PUSH,
    }

    fun afterSave(nameChanged: Boolean, meChanged: Boolean, radioReady: Boolean): List<SaveAction> = buildList {
        if (nameChanged && radioReady) add(SaveAction.APPLY_NAME)
        if (meChanged) add(SaveAction.PUSH)
    }

    /** The line shown with a photo offer or download, when there's something to say. */
    enum class PhotoLine {
        WAITING_FOR_RADIO,
        CONNECT_RADIO,
        BUSY,
    }

    fun photoLine(downloading: Boolean, radioAway: Boolean, full: Boolean, lastAsk: PhotoAsk?): PhotoLine? {
        if (downloading) return if (radioAway) PhotoLine.WAITING_FOR_RADIO else null
        if (radioAway || lastAsk == PhotoAsk.NO_RADIO) return PhotoLine.CONNECT_RADIO
        if (full || lastAsk == PhotoAsk.BUSY) return PhotoLine.BUSY
        return null
    }

    /** Whether a photo part that arrived over [arrival] belongs to a download asked for over [requested]. */
    fun photoPartFits(arrival: ProfileRoute, requested: ProfileRoute): Boolean = when (requested) {
        ProfileRoute.Direct -> arrival == ProfileRoute.Direct
        is ProfileRoute.Group -> arrival == requested || arrival == ProfileRoute.Direct
    }
}

/** When to ask again for a photo coming in, or give up on it. */
class PhotoWatchdog(private val clocks: Map<Long, Clock>) {
    data class Clock(
        /** The last ask or piece. */
        var lastActivity: Duration,
        var attempts: Int = 1,
        /** The longest wait for a piece so far. */
        var longestGap: Duration = Duration.ZERO,
        /** Time the radio was away since [lastActivity], from outages already over. */
        var awayInGap: Duration = Duration.ZERO,
    ) {
        /** A piece came: still making progress, so earlier retries don't count against it. */
        fun pieceCame(now: Duration, awaySince: Duration?) {
            longestGap = maxOf(longestGap, ProfileRules.pieceGap(now, lastActivity, awayInGap, awaySince))
            awayInGap = Duration.ZERO
            attempts = 1
            lastActivity = now
        }

        /** Asked (again): a new gap starts. */
        fun asked(now: Duration) {
            lastActivity = now
            awayInGap = Duration.ZERO
        }

        /** The radio's ready again, after being away since [awaySince]. */
        fun radioBack(awaySince: Duration, now: Duration) {
            awayInGap += maxOf(Duration.ZERO, now - maxOf(awaySince, lastActivity))
        }
    }

    /** Every download's step, as of [now]. [radioAwaySince] null means the radio is ready. */
    fun steps(
        now: Duration,
        radioReady: Boolean,
        radioAwaySince: Duration?,
        askQueued: (Long) -> Boolean,
        stillAskedGroup: (Long) -> Boolean,
    ): Map<Long, ProfileRules.PhotoRetry> {
        val away: Duration? = if (radioReady) null else radioAwaySince?.let { now - it } ?: Duration.ZERO
        return clocks.mapValues { (num, clock) ->
            ProfileRules.photoRetry(
                quietFor = now - clock.lastActivity,
                attempts = clock.attempts,
                askQueued = askQueued(num),
                stillAskedGroup = away != null || stillAskedGroup(num),
                radioAwayFor = away,
                longestGap = clock.longestGap,
            )
        }
    }
}
