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
@file:Suppress("MagicNumber")

package org.meshtastic.core.ui.ios

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Icons drawn to read like the SF Symbols the Pangea iPhone app uses. SF Symbols themselves are licensed for Apple
 * platforms only, so these are our own drawings on a 24-unit grid. Tint them with Icon(tint = …).
 */
object IosIcons {
    /** `map.fill`: three folded panels. */
    val Map: ImageVector by lazy {
        icon("ios.map") {
            filled {
                moveTo(2.5f, 6.4f)
                lineTo(7.9f, 4.0f)
                lineTo(7.9f, 17.6f)
                lineTo(2.5f, 20.0f)
                close()
                moveTo(9.3f, 4.0f)
                lineTo(14.7f, 6.4f)
                lineTo(14.7f, 20.0f)
                lineTo(9.3f, 17.6f)
                close()
                moveTo(16.1f, 6.4f)
                lineTo(21.5f, 4.0f)
                lineTo(21.5f, 17.6f)
                lineTo(16.1f, 20.0f)
                close()
            }
        }
    }

    /** `bubble.left.fill`: a speech bubble with its tail at the bottom left. */
    val Bubble: ImageVector by lazy {
        icon("ios.bubble") {
            filled {
                moveTo(12f, 3.2f)
                curveTo(17.5f, 3.2f, 21.6f, 6.7f, 21.6f, 11.1f)
                curveTo(21.6f, 15.5f, 17.5f, 19.0f, 12f, 19.0f)
                curveTo(10.9f, 19.0f, 9.85f, 18.86f, 8.86f, 18.6f)
                curveTo(7.6f, 19.85f, 5.7f, 20.75f, 3.6f, 20.85f)
                curveTo(4.65f, 19.7f, 5.3f, 18.35f, 5.42f, 16.9f)
                curveTo(3.6f, 15.45f, 2.4f, 13.4f, 2.4f, 11.1f)
                curveTo(2.4f, 6.7f, 6.5f, 3.2f, 12f, 3.2f)
                close()
            }
        }
    }

    /** `gearshape` as drawn in the iOS 26 tab bar: a toothed ring around a hub with three spokes. */
    val Gear: ImageVector by lazy {
        icon("ios.gear") {
            stroked(width = 1.7f) { gearOutline(cx = 12f, cy = 12f, outer = 10.2f, inner = 8.4f, teeth = 12) }
            stroked(width = 1.6f) {
                circle(12f, 12f, 2.2f)
                spoke(12f, 12f, 2.2f, 6.9f, -90f)
                spoke(12f, 12f, 2.2f, 6.9f, 30f)
                spoke(12f, 12f, 2.2f, 6.9f, 150f)
            }
        }
    }

    /** `chevron.right`, for list rows. */
    val ChevronRight: ImageVector by lazy {
        icon("ios.chevron.right", size = 14f, viewport = 14f) {
            stroked(width = 2.0f) {
                moveTo(5f, 2f)
                lineTo(10f, 7f)
                lineTo(5f, 12f)
            }
        }
    }

    /** `chevron.up.chevron.down`: the end of a menu picker row. */
    val ChevronUpDown: ImageVector by lazy {
        icon("ios.chevron.updown", size = 16f, viewport = 16f) {
            stroked(width = 1.6f) {
                moveTo(4.8f, 6.2f)
                lineTo(8f, 3f)
                lineTo(11.2f, 6.2f)
                moveTo(4.8f, 9.8f)
                lineTo(8f, 13f)
                lineTo(11.2f, 9.8f)
            }
        }
    }

    /** `person`: Direct Messages in Expert Mode. */
    val Person: ImageVector by lazy {
        icon("ios.person") {
            stroked(width = 1.6f) {
                circle(12f, 7.8f, 3.6f)
                moveTo(5f, 20f)
                curveTo(5f, 16.1f, 8.1f, 13.6f, 12f, 13.6f)
                curveTo(15.9f, 13.6f, 19f, 16.1f, 19f, 20f)
                close()
            }
        }
    }

    /** `person.2`: Channels in Expert Mode; the person behind is drawn lighter, as the hierarchical symbol is. */
    val People: ImageVector by lazy {
        icon("ios.people") {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeAlpha = 0.5f,
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                circle(7.6f, 8.4f, 2.8f)
                moveTo(2f, 18.5f)
                curveTo(2f, 15.4f, 4.4f, 13.4f, 7.6f, 13.4f)
                curveTo(8.6f, 13.4f, 9.5f, 13.6f, 10.2f, 14f)
            }
            stroked(width = 1.6f) {
                circle(15.2f, 7.6f, 3.3f)
                moveTo(8.8f, 19.5f)
                curveTo(8.8f, 15.9f, 11.6f, 13.6f, 15.2f, 13.6f)
                curveTo(18.8f, 13.6f, 21.6f, 15.9f, 21.6f, 19.5f)
                close()
            }
        }
    }

    /** `ellipsis`: a top bar's More menu. */
    val Ellipsis: ImageVector by lazy {
        icon("ios.ellipsis") {
            filled {
                circle(5.5f, 12f, 1.7f)
                circle(12f, 12f, 1.7f)
                circle(18.5f, 12f, 1.7f)
            }
        }
    }

    /** Pangea-Apple's `custom.mesh.radio`: the Nodes tab, a handheld radio with its screen and keypad dots. */
    val Radio: ImageVector by lazy {
        icon("ios.radio") {
            filled {
                roundRect(7.4f, 1.6f, 9.6f, 4.6f, 0.8f)
                roundRect(14.4f, 1.6f, 16.6f, 4.6f, 0.8f)
                roundRect(7.6f, 6.3f, 16.4f, 12.6f, 1.2f)
                circle(8.9f, 17.6f, 0.85f)
                circle(11.1f, 17.6f, 0.85f)
                circle(13.3f, 17.6f, 0.85f)
                circle(15.5f, 17.6f, 0.85f)
            }
            stroked(width = 1.8f) { roundRect(5f, 3.8f, 19f, 21.6f, 3f) }
        }
    }

    /** `link`: the Connect tab, two chain links. */
    val Link: ImageVector by lazy {
        icon("ios.link") {
            stroked(width = 2f) {
                capsule(6.3f, 17.7f, 10.6f, 13.4f, 3f)
                capsule(13.4f, 10.6f, 17.7f, 6.3f, 3f)
            }
        }
    }

    /** `questionmark.app`: About Pangea. */
    val QuestionSquare: ImageVector by lazy {
        icon("ios.questionSquare") {
            stroked(width = 1.6f) {
                roundRect(3.5f, 3.5f, 20.5f, 20.5f, 4.5f)
                moveTo(9.4f, 9.6f)
                curveTo(9.4f, 8f, 10.6f, 7f, 12f, 7f)
                curveTo(13.5f, 7f, 14.6f, 8f, 14.6f, 9.4f)
                curveTo(14.6f, 11.4f, 12f, 11.6f, 12f, 13.8f)
            }
            filled { circle(12f, 16.8f, 1.05f) }
        }
    }

    /** `doc.text`: Help & Documentation. */
    val DocText: ImageVector by lazy {
        icon("ios.docText") {
            stroked(width = 1.6f) {
                roundRect(5f, 2.8f, 19f, 21.2f, 2.6f)
                moveTo(8.5f, 8f)
                lineTo(15.5f, 8f)
                moveTo(8.5f, 11.5f)
                lineTo(15.5f, 11.5f)
                moveTo(8.5f, 15f)
                lineTo(13f, 15f)
            }
        }
    }

    /** `checkmark.circle`: a radio heard recently. */
    val CheckCircle: ImageVector by lazy {
        icon("ios.checkCircle") {
            stroked(width = 1.6f) {
                circle(12f, 12f, 9f)
                moveTo(8f, 12.4f)
                lineTo(10.8f, 15f)
                lineTo(16f, 9.2f)
            }
        }
    }

    /** `chevron.left`, for the back button. */
    val ChevronLeft: ImageVector by lazy {
        icon("ios.chevron.left") {
            stroked(width = 2.4f) {
                moveTo(15f, 4.5f)
                lineTo(7.5f, 12f)
                lineTo(15f, 19.5f)
            }
        }
    }

    /** `square.and.pencil`, the compose button on Chats. */
    val Compose: ImageVector by lazy {
        icon("ios.compose") {
            stroked(width = 1.8f) {
                moveTo(11f, 4.5f)
                lineTo(6f, 4.5f)
                curveTo(4.6f, 4.5f, 3.5f, 5.6f, 3.5f, 7f)
                lineTo(3.5f, 18f)
                curveTo(3.5f, 19.4f, 4.6f, 20.5f, 6f, 20.5f)
                lineTo(17f, 20.5f)
                curveTo(18.4f, 20.5f, 19.5f, 19.4f, 19.5f, 18f)
                lineTo(19.5f, 13f)
                moveTo(9.5f, 14.5f)
                lineTo(10.2f, 11.4f)
                lineTo(18.6f, 3.0f)
                curveTo(19.2f, 2.4f, 20.2f, 2.4f, 20.8f, 3.0f)
                curveTo(21.4f, 3.6f, 21.4f, 4.6f, 20.8f, 5.2f)
                lineTo(12.4f, 13.6f)
                close()
            }
        }
    }

    /** `checkmark`, for the selected row in a picker list. */
    val Checkmark: ImageVector by lazy {
        icon("ios.checkmark") {
            stroked(width = 2.2f) {
                moveTo(4.5f, 12.5f)
                lineTo(9.5f, 17.5f)
                lineTo(19.5f, 6.5f)
            }
        }
    }

    /** `magnifyingglass`. */
    val Search: ImageVector by lazy {
        icon("ios.search") {
            stroked(width = 2.0f) {
                circle(10.5f, 10.5f, 6.5f)
                moveTo(15.3f, 15.3f)
                lineTo(20.5f, 20.5f)
            }
        }
    }

    /** `globe`, Everyone Nearby's icon. */
    val Globe: ImageVector by lazy {
        icon("ios.globe") {
            stroked(width = 1.6f) {
                circle(12f, 12f, 9f)
                moveTo(3f, 12f)
                lineTo(21f, 12f)
                moveTo(12f, 3f)
                curveTo(9.2f, 5.5f, 9.2f, 18.5f, 12f, 21f)
                moveTo(12f, 3f)
                curveTo(14.8f, 5.5f, 14.8f, 18.5f, 12f, 21f)
                moveTo(4.6f, 7.5f)
                curveTo(9f, 8.9f, 15f, 8.9f, 19.4f, 7.5f)
                moveTo(4.6f, 16.5f)
                curveTo(9f, 15.1f, 15f, 15.1f, 19.4f, 16.5f)
            }
        }
    }

    /** `bell`: notifications on (Everyone Nearby's bell). */
    val Bell: ImageVector by lazy { icon("ios.bell") { stroked(width = 1.8f) { bellOutline() } } }

    /** `bell.slash`: notifications off. */
    val BellSlash: ImageVector by lazy {
        icon("ios.bell.slash") {
            stroked(width = 1.8f) {
                bellOutline()
                moveTo(3.5f, 3.5f)
                lineTo(20.5f, 20.5f)
            }
        }
    }

    /** `arrow.up.circle.fill`: send. A white arrow cut out of a filled circle. */
    val Send: ImageVector by lazy {
        icon("ios.send") {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                circle(12f, 12f, 11f)
                moveTo(12f, 5.6f)
                lineTo(17.2f, 10.8f)
                lineTo(15.8f, 12.2f)
                lineTo(13f, 9.4f)
                lineTo(13f, 18f)
                lineTo(11f, 18f)
                lineTo(11f, 9.4f)
                lineTo(8.2f, 12.2f)
                lineTo(6.8f, 10.8f)
                close()
            }
        }
    }

    /** `location`: the map's "show me" button (outline while not following). */
    val Location: ImageVector by lazy {
        icon("ios.location") {
            stroked(width = 1.8f) {
                moveTo(20.5f, 3.5f)
                lineTo(3.5f, 10.6f)
                lineTo(11.2f, 12.8f)
                lineTo(13.4f, 20.5f)
                close()
            }
        }
    }

    /** `location.fill`: the map is following you. */
    val LocationFill: ImageVector by lazy {
        icon("ios.location.fill") {
            filled {
                moveTo(20.5f, 3.5f)
                lineTo(3.5f, 10.6f)
                lineTo(11.2f, 12.8f)
                lineTo(13.4f, 20.5f)
                close()
            }
        }
    }

    /** `antenna.radiowaves.left.and.right`: My Radio, and the status pill. */
    val Antenna: ImageVector by lazy {
        icon("ios.antenna") {
            filled { circle(12f, 10.5f, 1.7f) }
            stroked(width = 1.8f) {
                moveTo(12f, 12.2f)
                lineTo(12f, 20.5f)
                moveTo(8.6f, 7.1f)
                curveTo(6.8f, 8.9f, 6.8f, 12.1f, 8.6f, 13.9f)
                moveTo(15.4f, 7.1f)
                curveTo(17.2f, 8.9f, 17.2f, 12.1f, 15.4f, 13.9f)
                moveTo(5.6f, 4.3f)
                curveTo(2.3f, 7.6f, 2.3f, 13.4f, 5.6f, 16.7f)
                moveTo(18.4f, 4.3f)
                curveTo(21.7f, 7.6f, 21.7f, 13.4f, 18.4f, 16.7f)
            }
        }
    }

    /** `person.crop.circle.fill`: the setup name page. The figure is cut out of the disc, as SF Symbols draws it. */
    val PersonCircleFill: ImageVector by lazy {
        icon("ios.personCircleFill") {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                circle(12f, 12f, 10f)
                circle(12f, 9.3f, 3.7f)
                moveTo(5.6f, 18.1f)
                curveTo(7.1f, 15.8f, 9.4f, 14.7f, 12f, 14.7f)
                curveTo(14.6f, 14.7f, 16.9f, 15.8f, 18.4f, 18.1f)
                curveTo(16.8f, 19.9f, 14.5f, 20.9f, 12f, 20.9f)
                curveTo(9.5f, 20.9f, 7.2f, 19.9f, 5.6f, 18.1f)
                close()
            }
        }
    }

    /** `checkmark.seal.fill`: a Pangea radio in the pairing list. */
    val CheckSeal: ImageVector by lazy {
        icon("ios.checkSeal") {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                val points = 16
                for (i in 0 until points) {
                    val angle = PI * 2 * i / points - PI / 2
                    val r = if (i % 2 == 0) 10.6 else 8.9
                    val x = (12 + r * cos(angle)).toFloat()
                    val y = (12 + r * sin(angle)).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
                moveTo(7.3f, 12.4f)
                lineTo(8.8f, 10.9f)
                lineTo(10.8f, 12.9f)
                lineTo(15.2f, 8.5f)
                lineTo(16.7f, 10f)
                lineTo(10.8f, 15.9f)
                close()
            }
        }
    }

    /** `dot.radiowaves.up.forward`: any other radio in the pairing list. */
    val RadioWaves: ImageVector by lazy {
        icon("ios.radioWaves") {
            filled { circle(6f, 18f, 2.2f) }
            stroked(width = 2f) {
                moveTo(6f, 12f)
                arcTo(6f, 6f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 12f, y1 = 18f)
                moveTo(6f, 8f)
                arcTo(10f, 10f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 16f, y1 = 18f)
                moveTo(6f, 4f)
                arcTo(14f, 14f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 20f, y1 = 18f)
            }
        }
    }

    /** `exclamationmark.triangle.fill`: a pairing or saving problem. */
    val WarningFill: ImageVector by lazy {
        icon("ios.warningFill") {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                moveTo(10.3f, 3.9f)
                curveTo(11.1f, 2.6f, 12.9f, 2.6f, 13.7f, 3.9f)
                lineTo(21.7f, 17.9f)
                curveTo(22.5f, 19.2f, 21.5f, 20.9f, 20f, 20.9f)
                lineTo(4f, 20.9f)
                curveTo(2.5f, 20.9f, 1.5f, 19.2f, 2.3f, 17.9f)
                close()
                moveTo(11f, 8.5f)
                lineTo(13f, 8.5f)
                lineTo(12.7f, 14.5f)
                lineTo(11.3f, 14.5f)
                close()
                circle(12f, 17.3f, 1.2f)
            }
        }
    }

    private fun icon(name: String, size: Float = 24f, viewport: Float = 24f, block: ImageVector.Builder.() -> Unit) =
        ImageVector.Builder(name, size.dp, size.dp, viewport, viewport).apply(block).build()

    private fun ImageVector.Builder.filled(build: PathBuilder.() -> Unit) =
        path(fill = SolidColor(Color.Black), pathFillType = PathFillType.NonZero, pathBuilder = build)

    private fun ImageVector.Builder.stroked(width: Float, build: PathBuilder.() -> Unit) = path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = width,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = build,
    )

    private fun PathBuilder.roundRect(left: Float, top: Float, right: Float, bottom: Float, r: Float) {
        moveTo(left + r, top)
        lineTo(right - r, top)
        arcTo(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = right, y1 = top + r)
        lineTo(right, bottom - r)
        arcTo(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = right - r, y1 = bottom)
        lineTo(left + r, bottom)
        arcTo(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = left, y1 = bottom - r)
        lineTo(left, top + r)
        arcTo(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = left + r, y1 = top)
        close()
    }

    /** A rounded bar of radius [r] around the line from ([x1], [y1]) to ([x2], [y2]). */
    private fun PathBuilder.capsule(x1: Float, y1: Float, x2: Float, y2: Float, r: Float) {
        val length = kotlin.math.hypot(x2 - x1, y2 - y1)
        val nx = -(y2 - y1) / length * r
        val ny = (x2 - x1) / length * r
        moveTo(x1 + nx, y1 + ny)
        lineTo(x2 + nx, y2 + ny)
        arcTo(r, r, 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = x2 - nx, y1 = y2 - ny)
        lineTo(x1 - nx, y1 - ny)
        arcTo(r, r, 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = x1 + nx, y1 = y1 + ny)
        close()
    }

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx + r, cy)
        arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx - r, y1 = cy)
        arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx + r, y1 = cy)
        close()
    }

    private fun PathBuilder.bellOutline() {
        moveTo(12f, 3.2f)
        curveTo(8.7f, 3.2f, 6.4f, 5.7f, 6.4f, 9.2f)
        lineTo(6.4f, 13.6f)
        lineTo(4.4f, 16.6f)
        lineTo(19.6f, 16.6f)
        lineTo(17.6f, 13.6f)
        lineTo(17.6f, 9.2f)
        curveTo(17.6f, 5.7f, 15.3f, 3.2f, 12f, 3.2f)
        close()
        moveTo(9.8f, 19.2f)
        curveTo(10.3f, 20.3f, 11.1f, 20.8f, 12f, 20.8f)
        curveTo(12.9f, 20.8f, 13.7f, 20.3f, 14.2f, 19.2f)
    }

    private fun PathBuilder.spoke(cx: Float, cy: Float, from: Float, to: Float, degrees: Float) {
        val a = degrees * PI.toFloat() / 180f
        moveTo(cx + from * cos(a), cy + from * sin(a))
        lineTo(cx + to * cos(a), cy + to * sin(a))
    }

    /** A gear rim: [teeth] flat-topped teeth between radius [inner] and [outer]. */
    private fun PathBuilder.gearOutline(cx: Float, cy: Float, outer: Float, inner: Float, teeth: Int) {
        val step = 2 * PI.toFloat() / teeth
        for (i in 0 until teeth) {
            val a0 = i * step
            val pts =
                listOf(a0 to inner, a0 + step * 0.18f to outer, a0 + step * 0.5f to outer, a0 + step * 0.68f to inner)
            pts.forEachIndexed { k, (a, r) ->
                val x = cx + r * cos(a)
                val y = cy + r * sin(a)
                if (i == 0 && k == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        close()
    }
}
