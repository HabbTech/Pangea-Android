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

import okio.ByteString
import okio.ByteString.Companion.toByteString
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class GroupAndOutboxTest {
    private val key: ByteString = ByteArray(32) { (it + 1).toByte() }.toByteString()

    @Test
    fun groupLinkAndDmInviteRoundTrip() {
        val invite = GroupInvite("Trail Crew", key)
        assertTrue(invite.link.startsWith("pangea://group#"))
        assertEquals(invite, GroupInvite.fromLink(invite.link))
        assertEquals(invite, GroupInvite.fromInvitePayload(invite.invitePayload))
    }

    @Test
    fun publicKeysAreNeverGroups() {
        assertFalse(GroupInvite.isPrivateKey(ByteString.EMPTY))
        assertFalse(GroupInvite.isPrivateKey(ByteArray(1) { 1 }.toByteString()))
        assertFalse(GroupInvite.isPrivateKey(ByteArray(32).toByteString()))
        val defaultKey =
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
                0x01,
            )
        assertFalse(GroupInvite.isPrivateKey(defaultKey.toByteString()))
        assertTrue(GroupInvite.isPrivateKey(key))
        assertNull(GroupInvite.shareable("Crew", ByteArray(1) { 1 }.toByteString()).getOrNull())
    }

    @Test
    fun namesFollowTheFirmwareAndReservedRules() {
        assertEquals("Trail Crew", GroupInvite.nameForNewGroup("  Trail   Crew ").getOrNull())
        assertNull(GroupInvite.nameForNewGroup("admin").getOrNull())
        assertNull(GroupInvite.nameForNewGroup("   ").getOrNull())
        assertNull(GroupInvite.fromSettings(GroupInvite("gpio", key).settings))
        // Too long for the firmware's 11 bytes: refused, never cut, on receipt.
        assertNull(GroupInvite.fromSettings(GroupInvite("Twelve chars", key).settings))
        // A name with a zero-width space would look like another group.
        assertNull(GroupInvite.fromSettings(GroupInvite("Crew​", key).settings))
    }

    @Test
    fun outboxMergesAndPutsMyCardsFirst() {
        val outbox = ProfileOutbox(Random(1))
        val now = 100.seconds
        val ask = ProfileOutbox.Item.CardAsk(5)
        val mine = ProfileOutbox.Item.Card(CardTarget.Person(7))
        assertTrue(
            outbox.schedule(
                ask,
                ProfileOutbox.Reason.ASK,
                ProfileOutbox.NOW,
                now,
                hint = ProfileCard.create("A", "", 0),
            ),
        )
        assertTrue(outbox.schedule(mine, ProfileOutbox.Reason.MY_MESSAGE, ProfileOutbox.NOW, now))
        assertTrue(outbox.schedule(mine, ProfileOutbox.Reason.REPLY, ProfileOutbox.NOW, now))
        assertEquals(2, outbox.entries.size)
        val first = assertIs<ProfileOutbox.Pick.Send>(outbox.next(now, piecesWaiting = false))
        assertEquals(mine, first.entry.item)
        outbox.finish(first.entry, ProfileOutbox.Outcome.SENT, now)
        // Spacing: nothing else goes for three seconds.
        assertNull(outbox.next(now + 1.seconds, piecesWaiting = false))
        val second = assertIs<ProfileOutbox.Pick.Send>(outbox.next(now + 4.seconds, piecesWaiting = false))
        assertEquals(ask, second.entry.item)
    }

    @Test
    fun noRadioPutsTheEntryBackUncharged() {
        val outbox = ProfileOutbox(Random(1))
        val now = 10.seconds
        val item = ProfileOutbox.Item.Card(CardTarget.Person(3))
        outbox.schedule(item, ProfileOutbox.Reason.REPLY, ProfileOutbox.NOW, now)
        val pick = assertIs<ProfileOutbox.Pick.Send>(outbox.next(now, piecesWaiting = false))
        assertTrue(outbox.finish(pick.entry, ProfileOutbox.Outcome.NO_RADIO, now))
        assertTrue(outbox.contains(item))
        assertEquals(0, outbox.charged(ProfileOutbox.Reason.REPLY, now))
    }

    @Test
    fun photoLimiterKeysGroupsApartFromPeople() {
        val limiter = PhotoSendLimiter()
        val now = 0.seconds
        val group = PhotoSendLimiter.Key(PhotoAudience.Group(2, "2:Crew:1"), 9u)
        val person = PhotoSendLimiter.Key(PhotoAudience.Person(5), 9u)
        assertTrue(limiter.admit(null, 3, group, now))
        assertTrue(limiter.admit(setOf(1), 3, person, now))
        val handouts = generateSequence { limiter.next(now) }.take(10).toList()
        assertEquals(4, handouts.size)
        assertEquals(listOf(0, 1, 2), handouts.filter { it.key == group }.map { it.slot }.sorted())
        assertEquals(listOf(1), handouts.filter { it.key == person }.map { it.slot })
        // Each audience may have the whole photo twice per window, no more.
        assertTrue(limiter.admit(null, 3, group, now))
        assertEquals(3, generateSequence { limiter.next(now) }.count())
        assertFalse(limiter.admit(null, 3, group, now + 1.seconds))
    }
}
