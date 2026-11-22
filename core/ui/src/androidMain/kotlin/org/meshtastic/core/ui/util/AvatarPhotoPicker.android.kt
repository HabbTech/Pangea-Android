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
package org.meshtastic.core.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Composable
actual fun rememberAvatarPhotoPicker(onPick: (ByteArray?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                scope.launch {
                    val bytes = withContext(Dispatchers.IO) { runCatching { avatarFrom(context, uri) }.getOrNull() }
                    onPick(bytes)
                }
            }
        }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}

/** Upright, square, on white (transparent PNGs would turn black), then the largest size and quality that fits. */
@Suppress("ReturnCount")
private fun avatarFrom(context: Context, uri: Uri): ByteArray? {
    val source = decodeUpright(context, uri) ?: return null
    val side = minOf(source.width, source.height)
    if (side <= 0) return null
    val left = (source.width - side) / 2
    val top = (source.height - side) / 2
    for (pixels in AVATAR_SIZES) {
        val square = Bitmap.createBitmap(pixels, pixels, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(square)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(
            source,
            Rect(left, top, left + side, top + side),
            Rect(0, 0, pixels, pixels),
            Paint(Paint.FILTER_BITMAP_FLAG),
        )
        val fitted =
            AVATAR_QUALITIES.firstNotNullOfOrNull { quality ->
                val out = ByteArrayOutputStream()
                square.compress(Bitmap.CompressFormat.JPEG, quality, out)
                out.toByteArray().takeIf { it.size <= AVATAR_MAX_BYTES }
            }
        if (fitted != null) return fitted
    }
    Logger.w { "👤 [Profiles] Couldn't fit the picked photo into an avatar" }
    return null
}

/** Decoded no bigger than needed, with the camera's rotation applied where the platform can. */
private fun decodeUpright(context: Context, uri: Uri): Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
        val longest = maxOf(info.size.width, info.size.height)
        val scale = (longest / DECODE_TARGET).coerceAtLeast(1)
        decoder.setTargetSize(info.size.width / scale, info.size.height / scale)
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }
} else {
    context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply { inSampleSize = SAMPLE_SIZE })
    }
}

private const val DECODE_TARGET = 1024
private const val SAMPLE_SIZE = 4
