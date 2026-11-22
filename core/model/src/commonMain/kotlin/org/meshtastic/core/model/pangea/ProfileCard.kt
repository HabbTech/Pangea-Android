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
@file:Suppress("MagicNumber", "ReturnCount")

package org.meshtastic.core.model.pangea

import kotlinx.serialization.Serializable
import okio.Buffer
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString

// What Pangea users share about themselves, and the small binary messages that carry it (Pangea-Apple
// ProfileCard.swift). Everything rides on PRIVATE_APP, which other Meshtastic apps ignore.
//
//  Card      "PGP2" color(1) photoVersion(4) [pieces(1) partTag(8)×parts] emojiLen(1) emoji nameLen(1) name
//  Request   "PGQ3" photoVersion(4) [bitmap of wanted slots; none = all]
//  Manifest  "PGM1" photoVersion(4) part(1) pieceTag(8)×(up to 21)
//  Chunk     "PGC2" photoVersion(4) index(1) bytes
//  Card ask  "PGK1" (nothing after it): send me your card, privately
//
// A photo goes out as its manifest parts, then its pieces; together they're its "slots", numbered in that order.

/** A Pangea user's card: display name, emoji avatar, colour and (optionally) the layout of their photo. */
@Serializable
data class ProfileCard(
    val displayName: String,
    val emoji: String,
    val color: Int,
    /** 0 when there's no photo; otherwise a hash of the photo's bytes. */
    val photoVersion: Long = 0,
    /** How many pieces the photo comes in. 0 when there's no photo. */
    val photoPieces: Int = 0,
    /** The tag of each part of the photo's piece list, hex-encoded. Empty when there's no photo. */
    val photoPartTagsHex: List<String> = emptyList(),
) {
    val photoPartTags: List<ByteString>
        get() = photoPartTagsHex.map { it.decodeHexOrEmpty() }

    /** The same card with its name and emoji cleaned, for cards stored before the rules were what they are now. */
    val cleaned: ProfileCard
        get() = copy(displayName = cleanName(displayName), emoji = cleanEmoji(emoji))

    /** The card describes a photo that can be fetched and checked piece by piece. */
    val hasPhotoLayout: Boolean
        get() =
            photoVersion != 0L &&
                photoPieces > 0 &&
                photoPieces <= PhotoChunk.MAX_CHUNKS &&
                photoPartTags.size == PhotoManifest.parts(photoPieces) &&
                photoPartTags.all { it.size == PangeaWire.TAG_BYTES }

    /** Changes whenever anything on the card changes, so we know who has the latest one. */
    val version: UInt
        get() = PangeaWire.hash(payload)

    val payload: ByteString
        get() {
            val emojiBytes = emoji.clampedUtf8(MAX_EMOJI_BYTES).encodeUtf8()
            val nameBytes = displayName.clampedUtf8(MAX_NAME_BYTES).encodeUtf8()
            val buffer = Buffer()
            buffer.write(PangeaWire.cardMagic)
            buffer.writeByte(color and BYTE_MASK)
            // A photo that can't be described piece by piece isn't offered at all.
            if (hasPhotoLayout) {
                buffer.write(PangeaWire.bytes(photoVersion.toUInt()))
                buffer.writeByte(photoPieces)
                photoPartTags.forEach { buffer.write(it) }
            } else {
                buffer.write(PangeaWire.bytes(0u))
            }
            buffer.writeByte(emojiBytes.size)
            buffer.write(emojiBytes)
            buffer.writeByte(nameBytes.size)
            buffer.write(nameBytes)
            return buffer.readByteString()
        }

    companion object {
        const val MAX_NAME_BYTES = 64

        /** Room for skin-tone pairs like 🧑🏻‍🤝‍🧑🏿. */
        const val MAX_EMOJI_BYTES = 40

        private const val BYTE_MASK = 0xFF

        fun create(
            displayName: String,
            emoji: String,
            color: Int,
            photoVersion: UInt = 0u,
            photoPieces: Int = 0,
            photoPartTags: List<ByteString> = emptyList(),
        ): ProfileCard = ProfileCard(
            displayName = displayName,
            emoji = emoji,
            color = color and BYTE_MASK,
            photoVersion = photoVersion.toLong(),
            photoPieces = photoPieces,
            photoPartTagsHex = photoPartTags.map { it.hex() },
        )

        /** My card, describing this photo when it's the one the version names. */
        fun create(displayName: String, emoji: String, color: Int, photo: PhotoPieces?): ProfileCard = create(
            displayName = displayName,
            emoji = emoji,
            color = color,
            photoVersion = photo?.version ?: 0u,
            photoPieces = photo?.chunks?.size ?: 0,
            photoPartTags = photo?.partTags ?: emptyList(),
        )

        /** A name as shown: without invisible or direction-changing characters, spaces plain, trimmed. */
        fun cleanName(name: String): String {
            val cleaned = name.cleanedName
            return if (cleaned.isBlankName) "" else cleaned
        }

        /** One emoji the editor offers, or "" for anything else. */
        fun cleanEmoji(text: String): String = if (EmojiCatalog.contains(text)) text else ""

        @Suppress("ReturnCount")
        fun parse(payload: ByteString): ProfileCard? {
            if (!payload.startsWith(PangeaWire.cardMagic)) return null
            val reader = PangeaWire.Reader(payload.substring(PangeaWire.cardMagic.size))
            val color = reader.byte() ?: return null
            val photoVersion = reader.uint32() ?: return null
            var pieces = 0
            val tags = mutableListOf<ByteString>()
            if (photoVersion != 0u) {
                val count = reader.byte()?.toInt() ?: return null
                if (count <= 0 || count > PhotoChunk.MAX_CHUNKS) return null
                pieces = count
                repeat(PhotoManifest.parts(count)) { tags += reader.take(PangeaWire.TAG_BYTES) ?: return null }
            }
            val emojiLen = reader.byte()?.toInt() ?: return null
            val emoji = reader.take(emojiLen) ?: return null
            val nameLen = reader.byte()?.toInt() ?: return null
            val name = reader.take(nameLen) ?: return null
            if (emoji.size > MAX_EMOJI_BYTES || name.size > MAX_NAME_BYTES) return null
            // Bytes that aren't UTF-8 count as no name (or no emoji), not as replacement marks.
            return create(
                displayName = cleanName(name.utf8OrEmpty()),
                emoji = cleanEmoji(emoji.utf8OrEmpty()),
                color = color.toInt(),
                photoVersion = photoVersion,
                photoPieces = pieces,
                photoPartTags = tags,
            )
        }
    }
}

/** Ask for some (or all) of a photo's slots. */
data class PhotoRequest(val photoVersion: UInt, val wanted: Set<Int>? = null) {
    /** Bitmap of wanted slots after the version; no bitmap means everything. */
    val payload: ByteString
        get() {
            val buffer = Buffer().write(PangeaWire.requestMagic).write(PangeaWire.bytes(photoVersion))
            val highest = wanted?.maxOrNull()
            if (wanted != null && highest != null) {
                val bitmap = ByteArray(highest / BITS + 1)
                wanted.forEach { index ->
                    bitmap[index / BITS] = (bitmap[index / BITS].toInt() or (1 shl (index % BITS))).toByte()
                }
                buffer.write(bitmap)
            }
            return buffer.readByteString()
        }

    fun wants(slot: Int): Boolean = wanted?.contains(slot) ?: true

    companion object {
        private const val BITS = 8

        /** Most slots a photo can have: every manifest part, then every piece. */
        val MAX_SLOTS: Int = PhotoManifest.parts(PhotoChunk.MAX_CHUNKS) + PhotoChunk.MAX_CHUNKS

        fun parse(payload: ByteString): PhotoRequest? {
            if (!payload.startsWith(PangeaWire.requestMagic)) return null
            val reader = PangeaWire.Reader(payload.substring(PangeaWire.requestMagic.size))
            val version = reader.uint32() ?: return null
            val bitmap = reader.rest()
            if (bitmap.size > (MAX_SLOTS + BITS - 1) / BITS) return null
            var wanted: Set<Int>? = null
            if (bitmap.size > 0) {
                wanted = buildSet {
                    for (byteIndex in 0 until bitmap.size) {
                        val byte = bitmap[byteIndex].toInt()
                        for (bit in 0 until BITS) if (byte and (1 shl bit) != 0) add(byteIndex * BITS + bit)
                    }
                }
            }
            return PhotoRequest(version, wanted)
        }
    }
}

/** One part of a photo's piece list: the tag of each piece it covers. */
data class PhotoManifest(val photoVersion: UInt, val part: Int, val tags: List<ByteString>) {
    /** What the card carries for this part. */
    val tag: ByteString
        get() = PangeaWire.tag(PangeaWire.concat(*tags.toTypedArray()))

    val payload: ByteString
        get() =
            PangeaWire.concat(
                PangeaWire.manifestMagic,
                PangeaWire.bytes(photoVersion),
                byteArrayOf(part.toByte()).toByteString(),
                *tags.toTypedArray(),
            )

    companion object {
        /** 21 × 8 bytes, the same room a piece gets. */
        const val TAGS_PER_PART = 21

        fun parts(pieces: Int): Int = (pieces + TAGS_PER_PART - 1) / TAGS_PER_PART

        @Suppress("ReturnCount")
        fun parse(payload: ByteString): PhotoManifest? {
            if (!payload.startsWith(PangeaWire.manifestMagic)) return null
            val reader = PangeaWire.Reader(payload.substring(PangeaWire.manifestMagic.size))
            val version = reader.uint32() ?: return null
            val part = reader.byte()?.toInt() ?: return null
            if (part >= parts(PhotoChunk.MAX_CHUNKS)) return null
            val rest = reader.rest()
            val tagBytes = PangeaWire.TAG_BYTES
            if (rest.size == 0 || rest.size % tagBytes != 0 || rest.size / tagBytes > TAGS_PER_PART) return null
            val tags = (0 until rest.size step tagBytes).map { rest.substring(it, it + tagBytes) }
            return PhotoManifest(version, part, tags)
        }
    }
}

/** One piece of a photo. */
data class PhotoChunk(val photoVersion: UInt, val index: Int, val bytes: ByteString) {
    val payload: ByteString
        get() =
            PangeaWire.concat(
                PangeaWire.chunkMagic,
                PangeaWire.bytes(photoVersion),
                byteArrayOf(index.toByte()).toByteString(),
                bytes,
            )

    companion object {
        /** Leaves room for PKI overhead inside a single LoRa packet. */
        const val MAX_BYTES = 180

        /** 80 × 180 bytes ≈ 14 KB, enough for a 256 px photo. */
        const val MAX_CHUNKS = 80

        fun parse(payload: ByteString): PhotoChunk? {
            if (!payload.startsWith(PangeaWire.chunkMagic)) return null
            val reader = PangeaWire.Reader(payload.substring(PangeaWire.chunkMagic.size))
            val version = reader.uint32() ?: return null
            val index = reader.byte()?.toInt() ?: return null
            if (index >= MAX_CHUNKS) return null
            val bytes = reader.rest()
            if (bytes.size == 0 || bytes.size > MAX_BYTES) return null
            return PhotoChunk(version, index, bytes)
        }

        fun split(photo: ByteString): List<PhotoChunk> {
            val version = PangeaWire.hash(photo)
            val pieces =
                (0 until photo.size step MAX_BYTES).map { photo.substring(it, minOf(it + MAX_BYTES, photo.size)) }
            if (pieces.isEmpty() || pieces.size > MAX_CHUNKS) return emptyList()
            return pieces.mapIndexed { index, piece -> PhotoChunk(version, index, piece) }
        }

        /** Null until every piece is there and the result matches the version it claims. */
        fun assemble(chunks: Map<Int, ByteString>, count: Int, version: UInt): ByteString? {
            if (count <= 0) return null
            val buffer = Buffer()
            for (index in 0 until count) buffer.write(chunks[index] ?: return null)
            val photo = buffer.readByteString()
            return if (PangeaWire.hash(photo) == version) photo else null
        }
    }
}

/** My photo as it goes on the air: manifest parts first, then pieces. */
class PhotoPieces
private constructor(
    val version: UInt,
    val chunks: List<PhotoChunk>,
    val manifest: List<PhotoManifest>,
) {
    val partTags: List<ByteString>
        get() = manifest.map { it.tag }

    val slotCount: Int
        get() = manifest.size + chunks.size

    fun payload(slot: Int): ByteString? {
        if (slot < manifest.size) return manifest[slot].payload
        val index = slot - manifest.size
        return chunks.getOrNull(index)?.payload
    }

    companion object {
        fun from(photo: ByteString): PhotoPieces? {
            val chunks = PhotoChunk.split(photo)
            if (chunks.isEmpty()) return null
            val version = PangeaWire.hash(photo)
            val tags = chunks.map { PangeaWire.tag(it.bytes) }
            val manifest =
                tags.chunked(PhotoManifest.TAGS_PER_PART).mapIndexed { part, partTags ->
                    PhotoManifest(version, part, partTags)
                }
            return PhotoPieces(version, chunks, manifest)
        }
    }
}

private fun ByteString.utf8OrEmpty(): String {
    // okio decodes malformed input with replacement characters; a round trip tells them apart.
    val text = utf8()
    return if (text.encodeUtf8() == this) text else ""
}

private fun String.decodeHexOrEmpty(): ByteString =
    runCatching { ByteArray(length / 2) { i -> substring(i * 2, i * 2 + 2).toInt(16).toByte() }.toByteString() }
        .getOrDefault(ByteString.EMPTY)
