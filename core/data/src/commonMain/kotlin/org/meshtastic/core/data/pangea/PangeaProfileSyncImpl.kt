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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.ByteString
import org.koin.core.annotation.Single
import org.meshtastic.core.common.di.ServiceScope
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.DataPacket
import org.meshtastic.core.model.NodeAddress
import org.meshtastic.core.model.pangea.CardTarget
import org.meshtastic.core.model.pangea.GroupInvite
import org.meshtastic.core.model.pangea.PangeaWire
import org.meshtastic.core.model.pangea.PhotoAsk
import org.meshtastic.core.model.pangea.PhotoAudience
import org.meshtastic.core.model.pangea.PhotoChunk
import org.meshtastic.core.model.pangea.PhotoDownload
import org.meshtastic.core.model.pangea.PhotoManifest
import org.meshtastic.core.model.pangea.PhotoPieces
import org.meshtastic.core.model.pangea.PhotoRequest
import org.meshtastic.core.model.pangea.PhotoSendLimiter
import org.meshtastic.core.model.pangea.PhotoWatchdog
import org.meshtastic.core.model.pangea.ProfileCard
import org.meshtastic.core.model.pangea.ProfileOutbox
import org.meshtastic.core.model.pangea.ProfileRoute
import org.meshtastic.core.model.pangea.ProfileRules
import org.meshtastic.core.model.pangea.ProfileSendResult
import org.meshtastic.core.repository.CommandSender
import org.meshtastic.core.repository.ConnectionStateProvider
import org.meshtastic.core.repository.NodeManager
import org.meshtastic.core.repository.PangeaProfileSync
import org.meshtastic.core.repository.RadioConfigRepository
import org.meshtastic.proto.ChannelSet
import org.meshtastic.proto.MeshPacket
import org.meshtastic.proto.PortNum
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Sends my card to the people I talk to, and fetches photos only when asked (port of Pangea-Apple ProfileSync.swift):
 * - DMs: my card goes along, privately, after a message to someone who doesn't have its latest version.
 * - Groups: my card goes to the group after I post, at most once a day unless it changed.
 * - Photos: never pushed. Their app asks and I send it in pieces; every piece is checked against my card.
 * - Private replies only go to people I've messaged, saved contacts, or people in one of my groups.
 * - A card heard in a group never replaces one that came privately from their key.
 * - Everyone Nearby (the public channel), and any channel without a private key, never sees any of this.
 */
@Suppress("TooManyFunctions", "LongParameterList", "ReturnCount", "LargeClass", "ComplexCondition")
@Single(binds = [PangeaProfileSync::class, PangeaProfileSyncImpl::class])
class PangeaProfileSyncImpl(
    private val store: PangeaProfileFiles,
    private val nodeManager: NodeManager,
    private val commandSender: CommandSender,
    private val connection: ConnectionStateProvider,
    radioConfigRepository: RadioConfigRepository,
    private val scope: ServiceScope,
) : PangeaProfileSync {
    private val clockOrigin = TimeSource.Monotonic.markNow()

    private fun now(): Duration = clockOrigin.elapsedNow()

    private val channelSet: StateFlow<ChannelSet> =
        radioConfigRepository.channelSetFlow.stateIn(scope, SharingStarted.Eagerly, ChannelSet.Builder().build())

    private class Transfer(
        val download: PhotoDownload,
        val clock: PhotoWatchdog.Clock,
        val route: ProfileRoute,
        /** For a group download, that group's identity: the slot alone may hold another group by then. */
        val groupKey: String?,
    )

    /** Every bit of state below is only touched on this one thread, as iOS keeps it on the main actor. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val serial = Dispatchers.Default.limitedParallelism(1)

    private val transfers = mutableMapOf<Long, Transfer>()
    private var outbox = ProfileOutbox()
    private var photoLimiter = PhotoSendLimiter()
    private var lane: Job? = null
    private var napping: Job? = null
    private var watchdog: Job? = null
    private var waitingForRadio = false
    private var backoff: Duration? = null
    private var backoffUntil: Duration? = null
    private var readinessEdges = 0
    private var awaySince: Duration? = null
    private var outgoing: PhotoPieces? = null

    /** An edit came while the radio couldn't take a packet: my card goes to everyone once it's back. */
    private var pushWhenBack = false

    init {
        scope.launch(serial) {
            connection.connectionState.collect { state ->
                readinessEdges++
                if (state == ConnectionState.Connected) {
                    radioBecameReady()
                } else if (awaySince == null) {
                    awaySince = now()
                }
            }
        }
    }

    private val myNum: Long?
        get() = nodeManager.myNodeNum.value?.let { it.toLong() and UINT32 }

    private val radioReady: Boolean
        get() = connection.connectionState.value == ConnectionState.Connected && myNum != null

    private val radioLinked: Boolean
        get() = connection.connectionState.value == ConnectionState.Connected

    private fun radioBecameReady() {
        val away = awaySince
        awaySince = null
        waitingForRadio = false
        backoff = null
        backoffUntil = null
        if (away != null) {
            val nowAt = now()
            transfers.values.forEach { it.clock.radioBack(away, nowAt) }
        }
        startLane()
        if (pushWhenBack) {
            pushWhenBack = false
            pushCard()
        }
    }

    // MARK: Me

    private val myPhotoPieces: PhotoPieces?
        get() {
            val version = store.me.value.photoVersion
            if (version == 0L) return null
            outgoing?.let { if (it.version.toLong() == version) return it }
            return store.myPhotoData()?.let(PhotoPieces::from).also { outgoing = it }
        }

    override val myCard: ProfileCard?
        get() {
            val num = myNum ?: return null
            val radioName = nodeManager.nodeDBbyNodeNum[num.toInt()]?.user?.long_name.orEmpty()
            return store.me.value.card(radioName, myPhotoPieces)
        }

    // MARK: Who's who

    private fun publicKey(num: Long): ByteString? {
        val node = nodeManager.nodeDBbyNodeNum[num.toInt()] ?: return null
        return (node.publicKey ?: node.user.public_key).takeIf { it.size > 0 }
    }

    private fun canReachDirectly(num: Long): Boolean = publicKey(num) != null

    /** "index:name:key hash" for a Pangea group with a private key on my radio; null for anything else. */
    fun groupKey(index: Int): String? {
        if (index <= 0) return null
        val settings = channelSet.value.settings.getOrNull(index) ?: return null
        if (!isPrivateGroup(settings.name, settings.psk)) return null
        return "$index:${settings.name}:${PangeaWire.hash(settings.psk)}"
    }

    private fun isPrivateGroup(name: String, psk: ByteString): Boolean =
        name.isNotEmpty() && !GroupInvite.isReserved(name) && GroupInvite.isPrivateKey(psk)

    private fun myGroupIndexes(): List<Int> = channelSet.value.settings.indices.filter { groupKey(it) != null }

    private fun iHaveMessaged(num: Long) = num in store.currentLedger.messagedByMe

    private fun theyHaveMessaged(num: Long) = num in store.currentLedger.messagedMe

    /** The group I last heard their card on, if I'm still in that same group. */
    private fun currentGroup(num: Long): Int? {
        val profile = store.profile(num) ?: return null
        val channel = profile.lastChannel ?: return null
        val key = profile.lastGroupKey ?: return null
        return channel.takeIf { groupKey(it) == key }
    }

    /** They've posted, or shared their card, in a Pangea group I'm in now. */
    private fun isInMyGroups(num: Long): Boolean {
        if (currentGroup(num) != null) return true
        val keys = myGroupIndexes().mapNotNull(::groupKey)
        return keys.any { num in store.currentLedger.groupPosters[it].orEmpty() }
    }

    private fun mayShare(num: Long, route: ProfileRoute): Boolean {
        if (route is ProfileRoute.Group) return true
        return ProfileRules.mayShare(route, iHaveMessaged(num), isInMyGroups(num), store.isContact(num))
    }

    /** Whether they'd still get my card and photo with no private messages between us. */
    fun keepsSharingWithoutChat(num: Long): Boolean = store.isContact(num) || isInMyGroups(num)

    // MARK: Outgoing

    override fun didSendMessage(toNum: Long, channel: Int) {
        scope.launch(serial) { sentMessage(toNum, channel) }
    }

    private fun sentMessage(toNum: Long, channel: Int) {
        val me = myNum
        if (me != null) {
            if (toNum != ProfileRules.BROADCAST && toNum > 0) {
                store.recordMessaged(toNum, byMe = true)
            } else {
                groupKey(channel)?.let { store.recordGroupPost(it, me) }
            }
        }
        if (myCard == null) return
        if (toNum > 0 && toNum != ProfileRules.BROADCAST) {
            if (!canReachDirectly(toNum)) return
            // Let the message itself go first.
            schedule(
                ProfileOutbox.Item.Card(CardTarget.Person(toNum)),
                ProfileOutbox.Reason.MY_MESSAGE,
                ProfileOutbox.AFTER_MESSAGE,
            )
        } else {
            val key = groupKey(channel) ?: return
            schedule(
                ProfileOutbox.Item.Card(CardTarget.Group(channel, key)),
                ProfileOutbox.Reason.MY_MESSAGE,
                ProfileOutbox.AFTER_MESSAGE,
            )
        }
    }

    override fun didReceiveMessage(fromNum: Long, toNum: Long, channel: Int, pkiEncrypted: Boolean) {
        scope.launch(serial) { receivedMessage(fromNum, toNum, channel, pkiEncrypted) }
    }

    private fun receivedMessage(fromNum: Long, toNum: Long, channel: Int, pkiEncrypted: Boolean) {
        if (fromNum == myNum) return
        if (pkiEncrypted && toNum != ProfileRules.BROADCAST) {
            store.recordMessaged(fromNum, byMe = false)
        } else {
            groupKey(channel)?.let { store.recordGroupPost(it, fromNum) }
        }
    }

    override fun pushUpdatedCard() {
        scope.launch(serial) { pushCard() }
    }

    private fun pushCard() {
        // Who to send to can't be told without the radio (it may be restarting for a new name).
        if (!radioReady) {
            pushWhenBack = true
            return
        }
        if (myCard == null) return
        val me = myNum
        val since = nowMillis - RECENT.inWholeMilliseconds
        val ledger = store.currentLedger
        val recent =
            (ledger.messagedByMe.entries + ledger.messagedMe.entries)
                .filter { it.value > since }
                .sortedByDescending { it.value }
                .map { it.key }
                .distinct()
        val people =
            recent
                .filter { it != me && canReachDirectly(it) && mayShare(it, ProfileRoute.Direct) }
                .take(MAX_PUSH_PEOPLE)
                .toMutableList()
        // Saved contacts too, chat or no chat, while there's room.
        for (num in store.contactNums()) {
            if (people.size >= MAX_PUSH_PEOPLE) break
            if (num != me && num !in people && canReachDirectly(num)) people += num
        }
        people.forEach {
            schedule(
                ProfileOutbox.Item.Card(CardTarget.Person(it)),
                ProfileOutbox.Reason.MY_EDIT,
                ProfileOutbox.AFTER_EDIT,
            )
        }
        val postedGroups = myGroupIndexes().mapNotNull { index -> groupKey(index)?.let { index to it } }
        for ((index, key) in postedGroups.filter { !ledger.groupPosters[it.second].isNullOrEmpty() }) {
            schedule(
                ProfileOutbox.Item.Card(CardTarget.Group(index, key)),
                ProfileOutbox.Reason.MY_EDIT,
                ProfileOutbox.AFTER_EDIT,
            )
        }
    }

    private fun schedule(
        item: ProfileOutbox.Item,
        reason: ProfileOutbox.Reason,
        delay: ClosedFloatingPointRange<Double>,
        hint: ProfileCard? = null,
        isReask: Boolean = false,
    ): Boolean {
        if (!outbox.schedule(item, reason, delay, now(), hint, isReask)) {
            Logger.i { "👤 [Profiles] Outbox full for $reason; dropped a profile packet" }
            return false
        }
        startLane()
        return true
    }

    override fun reset() {
        scope.launch(serial) { resetNow() }
    }

    private fun resetNow() {
        lane?.cancel()
        lane = null
        waitingForRadio = false
        backoff = null
        backoffUntil = null
        pushWhenBack = false
        outbox = ProfileOutbox()
        photoLimiter = PhotoSendLimiter()
        outgoing = null
        transfers.keys.toList().forEach { endTransfer(it, saved = false) }
        store.resetSentCards()
    }

    override suspend fun requestPhoto(num: Long): PhotoAsk = withContext(serial) { askForPhoto(num) }

    private fun askForPhoto(num: Long): PhotoAsk {
        val profile = store.profile(num) ?: return PhotoAsk.UNAVAILABLE
        val download = profile.card?.let(PhotoDownload::from) ?: return PhotoAsk.UNAVAILABLE
        transfers[num]?.let { if (it.download.version == download.version) return PhotoAsk.ALREADY_GOING }
        if (!radioReady) return PhotoAsk.NO_RADIO
        val route = route(num) ?: return PhotoAsk.UNAVAILABLE
        var groupKey: String? = null
        if (route is ProfileRoute.Group) groupKey = groupKey(route.index) ?: return PhotoAsk.UNAVAILABLE
        val others = transfers.size - if (transfers[num] == null) 0 else 1
        if (others >= ProfileOutbox.MAX_TRANSFERS) return PhotoAsk.BUSY
        if (!schedule(ProfileOutbox.Item.PhotoAsk(num), ProfileOutbox.Reason.PHOTO_ASK, ProfileOutbox.NOW)) {
            return PhotoAsk.BUSY
        }
        store.setDownload(num, 0.0)
        transfers[num] = Transfer(download, PhotoWatchdog.Clock(lastActivity = now()), route, groupKey)
        startWatchdog()
        return PhotoAsk.STARTED
    }

    override fun canRequestPhoto(num: Long): Boolean {
        val profile = store.profile(num) ?: return false
        if (profile.card?.hasPhotoLayout != true) return false
        return route(num) != null
    }

    private fun route(num: Long): ProfileRoute? {
        val direct = canReachDirectly(num)
        return ProfileRules.requestRoute(direct, direct && theyHaveMessaged(num), currentGroup(num))
    }

    // MARK: Incoming

    override fun handle(packet: MeshPacket, myNodeNum: Int): Boolean {
        val data = packet.decoded ?: return false
        if (data.portnum != PortNum.PRIVATE_APP) return false
        val payload = data.payload
        val isCard = payload.startsWith(PangeaWire.cardMagic)
        val isRequest = payload.startsWith(PangeaWire.requestMagic)
        val isManifest = payload.startsWith(PangeaWire.manifestMagic)
        val isChunk = payload.startsWith(PangeaWire.chunkMagic)
        val isCardRequest = payload.startsWith(PangeaWire.cardRequestMagic)
        if (!(isCard || isRequest || isManifest || isChunk || isCardRequest)) return false

        val from = packet.from.toLong() and UINT32
        if (from == (myNodeNum.toLong() and UINT32)) return true
        val to = packet.to.toLong() and UINT32
        // Only private DMs, or a Pangea group I'm in. Never the public channel.
        val route =
            ProfileRules.route(packet.pki_encrypted == true, to, packet.channel) { groupKey(it) != null } ?: return true
        // A private packet must come from the key I already know for them.
        val known = publicKey(from)
        if (route == ProfileRoute.Direct && packet.public_key.size > 0 && known != null && known != packet.public_key) {
            Logger.e { "👤 [Profiles] Ignored a private packet signed with an unexpected key" }
            return true
        }
        scope.launch(serial) {
            when {
                isCard -> ProfileCard.parse(payload)?.let { received(it, from, route) }
                isRequest -> PhotoRequest.parse(payload)?.let { received(it, from, route) }
                isManifest -> PhotoManifest.parse(payload)?.let { m -> receivedPhotoPart(from, route) { it.accept(m) } }
                isChunk -> PhotoChunk.parse(payload)?.let { c -> receivedPhotoPart(from, route) { it.accept(c) } }
                isCardRequest -> receivedCardRequest(from, route)
            }
        }
        return true
    }

    private fun received(card: ProfileCard, num: Long, route: ProfileRoute) {
        val accepted =
            if (route is ProfileRoute.Group) {
                store.receivedCard(card, num, route.index, groupKey(route.index))
            } else {
                store.receivedCard(card, num, null)
            }
        if (!accepted) {
            // Their card came privately from their key, so this one can't replace it. Ask them, privately.
            if (ProfileRules.asksForCard(card, store.profile(num)?.card, canReachDirectly(num))) {
                Logger.i { "👤 [Profiles] Group card differs from their private one; asking them for it" }
                schedule(
                    ProfileOutbox.Item.CardAsk(num),
                    ProfileOutbox.Reason.ASK,
                    ProfileOutbox.ASK_DELAY,
                    hint = card,
                )
            }
            return
        }
        Logger.i { "👤 [Profiles] Got a card" }
        // Swap cards: if they don't have mine yet, send it back. Only to people I talk to.
        if (myCard == null || !mayShare(num, route)) return
        when (route) {
            ProfileRoute.Direct ->
                schedule(
                    ProfileOutbox.Item.Card(CardTarget.Person(num)),
                    ProfileOutbox.Reason.REPLY,
                    ProfileOutbox.CARD_BACK,
                )

            is ProfileRoute.Group -> {
                val key = groupKey(route.index) ?: return
                schedule(
                    ProfileOutbox.Item.Card(CardTarget.Group(route.index, key)),
                    ProfileOutbox.Reason.REPLY,
                    ProfileOutbox.CARD_BACK_TO_GROUP,
                )
            }
        }
    }

    private fun receivedCardRequest(num: Long, route: ProfileRoute) {
        if (route != ProfileRoute.Direct || !mayShare(num, ProfileRoute.Direct) || myCard == null) return
        schedule(
            ProfileOutbox.Item.Card(CardTarget.Person(num)),
            ProfileOutbox.Reason.ANSWER,
            ProfileOutbox.AFTER_MESSAGE,
        )
    }

    private fun received(request: PhotoRequest, num: Long, route: ProfileRoute) {
        val pieces = myPhotoPieces ?: return
        if (request.photoVersion != pieces.version || !mayShare(num, route)) return
        val audience =
            when (route) {
                ProfileRoute.Direct -> PhotoAudience.Person(num)
                is ProfileRoute.Group -> PhotoAudience.Group(route.index, groupKey(route.index) ?: return)
            }
        val key = PhotoSendLimiter.Key(audience, pieces.version)
        if (!photoLimiter.admit(request.wanted, pieces.slotCount, key, now())) {
            Logger.i { "👤 [Profiles] Skipped a photo request (sent enough lately, or too busy)" }
            return
        }
        startLane()
    }

    private fun receivedPhotoPart(num: Long, route: ProfileRoute, accept: (PhotoDownload) -> PhotoDownload.Outcome) {
        val transfer = transfers[num] ?: return
        if (!ProfileRules.photoPartFits(route, transfer.route)) return
        if (route is ProfileRoute.Group && !isStillAskedGroup(transfer)) {
            endTransfer(num, saved = false)
            return
        }
        when (accept(transfer.download)) {
            PhotoDownload.Outcome.IGNORED,
            PhotoDownload.Outcome.DUPLICATE,
            PhotoDownload.Outcome.HELD,
            -> return

            PhotoDownload.Outcome.ADDED -> transfer.clock.pieceCame(now(), awaySince)
        }
        if (transfer.download.isComplete) {
            val photo = transfer.download.assemble()
            if (photo != null && photo.size <= MAX_AVATAR_BYTES) {
                endTransfer(num, saved = true)
                store.savePhoto(photo, transfer.download.version.toLong(), num)
                Logger.i { "👤 [Profiles] Photo received" }
            } else {
                Logger.e { "👤 [Profiles] Received photo isn't a usable image; dropped" }
                endTransfer(num, saved = false)
            }
            return
        }
        store.setDownload(num, transfer.download.progress)
    }

    private fun isStillAskedGroup(transfer: Transfer): Boolean {
        val route = transfer.route as? ProfileRoute.Group ?: return true
        return ProfileRules.isSameGroup(transfer.groupKey, groupKey(route.index))
    }

    private fun endTransfer(num: Long, saved: Boolean) {
        transfers.remove(num)
        outbox.remove(ProfileOutbox.Item.PhotoAsk(num))
        if (!saved) store.setDownload(num, null)
    }

    // MARK: The lane

    /**
     * Sends what's waiting, one packet at a time and at least [ProfileOutbox.SPACING] apart so regular messages still
     * get through: my own cards first, then the rest of the outbox taking turns with photo pieces.
     */
    private fun startLane() {
        val mayGo = !waitingForRadio || radioReady
        if (lane?.isActive == true) {
            napping?.cancel()
            return
        }
        if (!mayGo) return
        waitingForRadio = false
        lane = scope.launch(serial) { runLane() }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod", "NestedBlockDepth", "LoopWithTooManyJumpStatements")
    private suspend fun runLane() {
        while (currentCoroutineContext().isActive) {
            val nowAt = now()
            backoffUntil?.let { until ->
                if (until > nowAt) {
                    nap(until - nowAt)
                    continue
                }
            }
            val ready = outbox.sendableAt(nowAt)
            if (ready > nowAt) {
                nap(ready - nowAt)
                continue
            }
            when (val pick = outbox.next(nowAt, piecesWaiting = !photoLimiter.isIdle)) {
                is ProfileOutbox.Pick.Send -> {
                    val edges = readinessEdges
                    val result = deliver(pick.entry)
                    val outcome = ProfileRules.outcome(result, radioReady, readinessEdges == edges)
                    val wait = outbox.finish(pick.entry, outcome, now())
                    if (!wait) {
                        if (outcome == ProfileOutbox.Outcome.SENT) sentOne()
                        continue
                    }
                    if (!waitOutNoRadio()) return
                }

                ProfileOutbox.Pick.Piece -> {
                    if (!radioReady) {
                        if (!waitOutNoRadio()) return
                        continue
                    }
                    val next = photoLimiter.next(nowAt) ?: continue
                    val pieces = myPhotoPieces
                    val payload = pieces?.takeIf { it.version == next.key.version }?.payload(next.slot)
                    val destination = destination(next.key.audience)
                    if (pieces == null || payload == null || destination == null) {
                        photoLimiter.cancel(next.key)
                        continue
                    }
                    val edges = readinessEdges
                    val result = send(payload, destination.first, destination.second)
                    when (ProfileRules.outcome(result, radioReady, readinessEdges == edges)) {
                        ProfileOutbox.Outcome.SENT -> {
                            photoLimiter.delivered(next.key)
                            outbox.sentPhotoPiece(now())
                            sentOne()
                        }

                        ProfileOutbox.Outcome.NO_RADIO -> {
                            photoLimiter.putBack(next)
                            if (!waitOutNoRadio()) return
                        }

                        ProfileOutbox.Outcome.FAILED,
                        ProfileOutbox.Outcome.NOT_NEEDED,
                        -> photoLimiter.cancel(next.key)
                    }
                }

                null -> {
                    val wake = outbox.nextWake(nowAt, piecesWaiting = !photoLimiter.isIdle) ?: return
                    nap(wake - nowAt)
                }
            }
        }
    }

    private suspend fun nap(duration: Duration) {
        val sleep = scope.launch(serial) { delay(duration) }
        napping = sleep
        sleep.join()
        if (napping == sleep) napping = null
    }

    /**
     * A send found no radio. Gone: the lane pauses until it's back (false). Still linked: try again after a backoff.
     */
    private fun waitOutNoRadio(): Boolean {
        val wait = ProfileRules.afterNoRadio(radioLinked, backoff)
        if (wait == null) {
            waitingForRadio = true
            Logger.i { "👤 [Profiles] Waiting for the radio to send profile packets" }
            return false
        }
        backoff = wait
        backoffUntil = now() + wait
        return true
    }

    private fun sentOne() {
        backoff = null
        backoffUntil = null
    }

    /** Sends one outbox packet, worked out now. Null when there's nothing to send any more. */
    private suspend fun deliver(entry: ProfileOutbox.Entry): ProfileSendResult? {
        if (!radioReady) return ProfileSendResult.NO_RADIO
        return when (val item = entry.item) {
            is ProfileOutbox.Item.Card -> {
                val card = myCard ?: return null
                val version = card.version
                when (val target = item.target) {
                    is CardTarget.Person -> {
                        val facts =
                            ProfileRules.SendFacts(
                                myCardVersion = version,
                                sentCardVersion = store.profile(target.num)?.sentCardVersion?.toUInt(),
                                canReachDirectly = canReachDirectly(target.num),
                                mayShare = mayShare(target.num, ProfileRoute.Direct),
                            )
                        if (!ProfileRules.stillNeeded(entry, facts)) return null
                        send(card.payload, target.num, ProfileRoute.Direct).also {
                            if (it == ProfileSendResult.SENT) store.markCardSent(version.toLong(), target.num)
                        }
                    }

                    is CardTarget.Group -> {
                        val facts =
                            ProfileRules.SendFacts(
                                myCardVersion = version,
                                sameGroup = groupKey(target.index) == target.key,
                                needsGroupSend = store.needsGroupSend(target.key, version.toLong()),
                            )
                        if (!ProfileRules.stillNeeded(entry, facts)) return null
                        send(card.payload, ProfileRules.BROADCAST, ProfileRoute.Group(target.index)).also {
                            if (it == ProfileSendResult.SENT) store.markGroupSent(target.key, version.toLong())
                        }
                    }
                }
            }

            is ProfileOutbox.Item.CardAsk -> {
                val facts =
                    ProfileRules.SendFacts(
                        canReachDirectly = canReachDirectly(item.num),
                        storedCard = store.profile(item.num)?.card,
                    )
                if (!ProfileRules.stillNeeded(entry, facts)) return null
                send(PangeaWire.cardRequestMagic, item.num, ProfileRoute.Direct)
            }

            is ProfileOutbox.Item.PhotoAsk -> {
                val transfer = transfers[item.num]
                if (!ProfileRules.stillNeeded(entry, ProfileRules.SendFacts(transferExists = transfer != null))) {
                    return null
                }
                transfer ?: return null
                if (!isStillAskedGroup(transfer)) {
                    endTransfer(item.num, saved = false)
                    return null
                }
                val wanted = if (entry.isReask) transfer.download.missingSlots else null
                transfer.clock.asked(now())
                send(PhotoRequest(transfer.download.version, wanted).payload, item.num, transfer.route)
            }
        }
    }

    private fun destination(audience: PhotoAudience): Pair<Long, ProfileRoute>? = when (audience) {
        is PhotoAudience.Person -> if (canReachDirectly(audience.num)) audience.num to ProfileRoute.Direct else null

        is PhotoAudience.Group ->
            if (groupKey(audience.channel) == audience.key) {
                ProfileRules.BROADCAST to ProfileRoute.Group(audience.channel)
            } else {
                null
            }
    }

    private suspend fun send(payload: ByteString, to: Long, route: ProfileRoute): ProfileSendResult {
        if (!radioReady) return ProfileSendResult.NO_RADIO
        val channel =
            when (route) {
                ProfileRoute.Direct -> {
                    if (!canReachDirectly(to)) return ProfileSendResult.FAILED
                    NodeAddress.PKC_CHANNEL_INDEX
                }

                is ProfileRoute.Group -> route.index
            }
        val packet =
            DataPacket(
                to =
                if (to == ProfileRules.BROADCAST) {
                    NodeAddress.ID_BROADCAST
                } else {
                    NodeAddress.numToDefaultId(to.toInt())
                },
                bytes = payload,
                dataType = PortNum.PRIVATE_APP.value,
                channel = channel,
                wantAck = to != ProfileRules.BROADCAST,
            )
        return runCatching { commandSender.sendData(packet) }
            .fold(
                onSuccess = { ProfileSendResult.SENT },
                onFailure = {
                    Logger.e(it) { "👤 [Profiles] Send failed" }
                    if (radioReady) ProfileSendResult.FAILED else ProfileSendResult.NO_RADIO
                },
            )
    }

    // MARK: Watchdog

    /** Re-asks for missing parts, and gives up after a few quiet tries or once its group is gone. */
    private fun startWatchdog() {
        if (watchdog?.isActive == true) return
        watchdog =
            scope.launch(serial) {
                while (transfers.isNotEmpty()) {
                    delay(WATCHDOG_TICK)
                    watchdogStep()
                }
            }
    }

    private fun watchdogStep() {
        val steps =
            PhotoWatchdog(transfers.mapValues { it.value.clock })
                .steps(
                    now = now(),
                    radioReady = radioReady,
                    radioAwaySince = awaySince,
                    askQueued = { outbox.contains(ProfileOutbox.Item.PhotoAsk(it)) },
                    stillAskedGroup = { num -> transfers[num]?.let(::isStillAskedGroup) ?: false },
                )
        for ((num, step) in steps) {
            when (step) {
                ProfileRules.PhotoRetry.WAIT -> Unit

                ProfileRules.PhotoRetry.GIVE_UP -> endTransfer(num, saved = false)

                ProfileRules.PhotoRetry.ASK_AGAIN -> {
                    val queued =
                        schedule(
                            ProfileOutbox.Item.PhotoAsk(num),
                            ProfileOutbox.Reason.PHOTO_ASK,
                            ProfileOutbox.REASK,
                            isReask = true,
                        )
                    if (queued) {
                        transfers[num]?.clock?.let {
                            it.attempts += 1
                            it.asked(now())
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val UINT32 = 0xFFFFFFFFL
        private const val MAX_PUSH_PEOPLE = 8
        private val RECENT = 14.days
        private val WATCHDOG_TICK = 10.seconds

        /** iOS keeps avatars under about 14 KB (80 pieces of 180 bytes). */
        private const val MAX_AVATAR_BYTES = PhotoChunk.MAX_BYTES * PhotoChunk.MAX_CHUNKS
    }
}
