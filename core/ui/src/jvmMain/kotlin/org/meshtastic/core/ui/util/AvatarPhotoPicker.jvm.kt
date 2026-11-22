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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Color
import java.awt.FileDialog
import java.awt.Frame
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

@Composable
actual fun rememberAvatarPhotoPicker(onPick: (ByteArray?) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    return {
        val dialog = FileDialog(null as Frame?, "", FileDialog.LOAD)
        dialog.isVisible = true
        val file = dialog.file?.let { File(dialog.directory, it) }
        if (file != null) {
            scope.launch { onPick(withContext(Dispatchers.IO) { runCatching { avatarFrom(file) }.getOrNull() }) }
        }
    }
}

@Suppress("ReturnCount")
private fun avatarFrom(file: File): ByteArray? {
    val source = ImageIO.read(file) ?: return null
    val side = minOf(source.width, source.height)
    if (side <= 0) return null
    val left = (source.width - side) / 2
    val top = (source.height - side) / 2
    for (pixels in AVATAR_SIZES) {
        val square = BufferedImage(pixels, pixels, BufferedImage.TYPE_INT_RGB)
        val graphics = square.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        graphics.color = Color.WHITE
        graphics.fillRect(0, 0, pixels, pixels)
        graphics.drawImage(source, 0, 0, pixels, pixels, left, top, left + side, top + side, null)
        graphics.dispose()
        val fitted =
            AVATAR_QUALITIES.firstNotNullOfOrNull { quality ->
                jpeg(square, quality / PERCENT).takeIf { it.size <= AVATAR_MAX_BYTES }
            }
        if (fitted != null) return fitted
    }
    return null
}

private fun jpeg(image: BufferedImage, quality: Float): ByteArray {
    val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
    val out = ByteArrayOutputStream()
    ImageIO.createImageOutputStream(out).use { stream ->
        writer.output = stream
        val params =
            writer.defaultWriteParam.apply {
                compressionMode = ImageWriteParam.MODE_EXPLICIT
                compressionQuality = quality
            }
        writer.write(null, IIOImage(image, null, null), params)
        writer.dispose()
    }
    return out.toByteArray()
}

private const val PERCENT = 100f
