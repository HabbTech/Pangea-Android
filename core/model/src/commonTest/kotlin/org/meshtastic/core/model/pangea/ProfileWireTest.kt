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
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Mirrors Pangea-Apple ProfileTests.swift ("Profile wire format"). */
class ProfileWireTest {
    private fun bytes(count: Int, mod: Int) = ByteArray(count) { (it % mod).toByte() }.toByteString()

    private fun of(vararg values: Int): ByteString = values.map { it.toByte() }.toByteArray().toByteString()

    @Test
    fun cardRoundTrips() {
        val photo = assertNotNull(PhotoPieces.from(bytes(5000, 253)))
        val card = ProfileCard.create("Sam Rivera", "🏕️", 3, photo)
        assertTrue(card.hasPhotoLayout)
        assertTrue(card.payload.size < 100)
        assertEquals(card, ProfileCard.parse(card.payload))
        val plain = ProfileCard.create("Sam", "", 0)
        assertEquals(plain, ProfileCard.parse(plain.payload))
    }

    @Test
    fun biggestCardFitsInOnePacket() {
        val photo = assertNotNull(PhotoPieces.from(bytes(PhotoChunk.MAX_BYTES * PhotoChunk.MAX_CHUNKS, 251)))
        assertEquals(4, photo.manifest.size)
        val card = ProfileCard.create("é".repeat(32), "🧑🏻‍🤝‍🧑🏿", 1, photo)
        assertTrue(card.payload.size <= PhotoChunk.MAX_BYTES + 10)
        assertTrue(photo.manifest.all { it.payload.size <= PhotoChunk.MAX_BYTES + 10 })
        assertEquals(card, ProfileCard.parse(card.payload))
    }

    @Test
    fun aPhotoWithoutItsPiecesIsntOffered() {
        val card = ProfileCard.create("Sam", "", 0, photoVersion = 0xDEADBEEFu)
        assertFalse(card.hasPhotoLayout)
        assertEquals(0L, ProfileCard.parse(card.payload)?.photoVersion)
    }

    @Test
    fun cardVersionChangesWithContent() {
        val card = ProfileCard.create("Sam", "", 0)
        assertNotEquals(card.version, card.copy(color = 1).version)
        assertEquals(card.version, ProfileCard.create("Sam", "", 0).version)
    }

    @Test
    fun longFieldsAreClampedToFit() {
        val card = ProfileCard.create("é".repeat(100), "🔥".repeat(10), 0)
        val decoded = assertNotNull(ProfileCard.parse(card.payload))
        assertTrue(decoded.displayName.encodeUtf8().size <= ProfileCard.MAX_NAME_BYTES)
        assertTrue(decoded.emoji.encodeUtf8().size <= ProfileCard.MAX_EMOJI_BYTES)
    }

    @Test
    fun rejectsOtherPayloads() {
        assertNull(ProfileCard.parse("PGI1hello".encodeUtf8()))
        assertNull(ProfileCard.parse(PangeaWire.concat(PangeaWire.cardMagic, of(1, 2))))
        assertNull(PhotoRequest.parse("nope".encodeUtf8()))
        assertNull(PhotoChunk.parse(PangeaWire.concat(PangeaWire.chunkMagic, of(0, 0, 0, 1, 80, 2))))
        assertNull(PhotoChunk.parse(PangeaWire.concat(PangeaWire.chunkMagic, of(0, 0, 0, 1, 5))))
        assertNull(PhotoManifest.parse(PangeaWire.concat(PangeaWire.manifestMagic, of(0, 0, 0, 1, 0, 1, 2, 3))))
        assertNull(
            PhotoManifest.parse(
                PangeaWire.concat(PangeaWire.manifestMagic, of(0, 0, 0, 1, 4), ByteArray(8).toByteString()),
            ),
        )
        assertNull(ProfileCard.parse(PangeaWire.concat("PGP1".encodeUtf8(), of(0, 0, 0, 0, 0, 0, 0))))
    }

    @Test
    fun requestRoundTrips() {
        val some = PhotoRequest(42u, setOf(1, 3, 9, 70))
        assertEquals(some, PhotoRequest.parse(some.payload))
        val all = PhotoRequest(42u)
        assertEquals(all, PhotoRequest.parse(all.payload))
        assertTrue(all.wants(77))
        assertFalse(some.wants(2))
    }

    @Test
    fun requestBitmapLayoutMatchesIos() {
        // Slot n is bit (n % 8) of byte (n / 8), least significant bit first.
        val payload = PhotoRequest(1u, setOf(0, 9)).payload
        assertEquals(PangeaWire.concat(PangeaWire.requestMagic, of(0, 0, 0, 1, 0x01, 0x02)), payload)
    }

    @Test
    fun photoSplitsAndReassembles() {
        val photo = bytes(1100, 251)
        val chunks = PhotoChunk.split(photo)
        assertEquals(7, chunks.size)
        assertTrue(chunks.all { it.payload.size <= PhotoChunk.MAX_BYTES + 9 })
        val received = mutableMapOf<Int, ByteString>()
        chunks.reversed().forEach { chunk ->
            val decoded = assertNotNull(PhotoChunk.parse(chunk.payload))
            received[decoded.index] = decoded.bytes
        }
        val version = PangeaWire.hash(photo)
        assertEquals(photo, PhotoChunk.assemble(received, chunks.size, version))
        received.remove(3)
        assertNull(PhotoChunk.assemble(received, chunks.size, version))
    }

    @Test
    fun corruptPhotoIsRejected() {
        val photo = ByteArray(300) { 7 }.toByteString()
        val received = PhotoChunk.split(photo).associate { it.index to it.bytes }.toMutableMap()
        received[0] = PangeaWire.concat(of(8), received.getValue(0).substring(1))
        assertNull(PhotoChunk.assemble(received, 2, PangeaWire.hash(photo)))
    }

    @Test
    fun versionIsNeverZeroAndBigEndian() {
        assertNotEquals(0u, PangeaWire.hash(ByteString.EMPTY))
        // SHA-256("") starts e3 b0 c4 42.
        assertEquals(0xE3B0C442u, PangeaWire.hash(ByteString.EMPTY))
    }

    @Test
    fun downloadChecksEveryPieceAgainstTheCard() {
        val photo = bytes(5000, 253)
        val pieces = assertNotNull(PhotoPieces.from(photo))
        val card = ProfileCard.create("Sam", "", 0, pieces)
        val download = assertNotNull(PhotoDownload.from(card))
        // A piece before its manifest part is held, then checked.
        assertEquals(PhotoDownload.Outcome.HELD, download.accept(pieces.chunks[0]))
        pieces.manifest.forEach { assertEquals(PhotoDownload.Outcome.ADDED, download.accept(it)) }
        // A forged piece is ignored.
        val forged = PhotoChunk(pieces.version, 1, of(1, 2, 3))
        assertEquals(PhotoDownload.Outcome.IGNORED, download.accept(forged))
        pieces.chunks.drop(1).forEach { assertEquals(PhotoDownload.Outcome.ADDED, download.accept(it)) }
        assertTrue(download.isComplete)
        assertEquals(photo, download.assemble())
    }

    @Test
    fun emojiCatalogHasTheFullSet() {
        assertEquals(9, EmojiCatalog.groups.size)
        assertTrue(EmojiCatalog.groups.sumOf { it.emoji.size } > 1800)
        assertTrue(EmojiCatalog.contains("🏳️‍🌈"))
        assertTrue(EmojiCatalog.contains("🏕️"))
        assertFalse(EmojiCatalog.contains("x"))
    }
}
