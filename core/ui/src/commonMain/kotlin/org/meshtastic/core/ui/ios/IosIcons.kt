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
 *
 * A catalogue of path data with no logic, so its size is not a design smell.
 */
@Suppress("LargeClass")
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

    /** `message`: incoming messages on the setup pages. */
    val MessageOutline: ImageVector by lazy {
        icon("ios.message") {
            stroked(width = 1.6f) {
                moveTo(12f, 4.2f)
                curveTo(16.9f, 4.2f, 20.8f, 7.4f, 20.8f, 11.4f)
                curveTo(20.8f, 15.4f, 16.9f, 18.6f, 12f, 18.6f)
                curveTo(10.9f, 18.6f, 9.9f, 18.45f, 8.95f, 18.15f)
                lineTo(4.6f, 20.2f)
                lineTo(5.7f, 16.4f)
                curveTo(4.1f, 15.1f, 3.2f, 13.35f, 3.2f, 11.4f)
                curveTo(3.2f, 7.4f, 7.1f, 4.2f, 12f, 4.2f)
                close()
            }
        }
    }

    /** `message.badge.waveform`: Welcome's "Message Without Cell Service". */
    val MessageWaveform: ImageVector by lazy {
        icon("ios.messageWaveform") {
            stroked(width = 1.5f) {
                moveTo(10f, 3.5f)
                curveTo(14.4f, 3.5f, 17.6f, 6.3f, 17.6f, 9.8f)
                curveTo(17.6f, 10.4f, 17.5f, 11f, 17.3f, 11.5f)
                moveTo(12.6f, 15.8f)
                curveTo(11.8f, 16f, 10.9f, 16.1f, 10f, 16.1f)
                curveTo(9.1f, 16.1f, 8.3f, 16f, 7.5f, 15.75f)
                lineTo(3.6f, 17.6f)
                lineTo(4.5f, 14.3f)
                curveTo(3.2f, 13.1f, 2.4f, 11.55f, 2.4f, 9.8f)
                curveTo(2.4f, 6.3f, 5.6f, 3.5f, 10f, 3.5f)
                moveTo(15.2f, 17f)
                lineTo(15.2f, 19.2f)
                moveTo(17.4f, 14.8f)
                lineTo(17.4f, 21.4f)
                moveTo(19.6f, 16f)
                lineTo(19.6f, 20.2f)
                moveTo(21.8f, 17.2f)
                lineTo(21.8f, 19f)
            }
        }
    }

    /** `lock.shield`: Welcome's "Private by Default". */
    val LockShield: ImageVector by lazy {
        icon("ios.lockShield") {
            stroked(width = 1.6f) {
                moveTo(12f, 2.8f)
                lineTo(19.5f, 5.6f)
                lineTo(19.5f, 11f)
                curveTo(19.5f, 15.8f, 16.3f, 19.4f, 12f, 21.2f)
                curveTo(7.7f, 19.4f, 4.5f, 15.8f, 4.5f, 11f)
                lineTo(4.5f, 5.6f)
                close()
                moveTo(10.2f, 11.2f)
                lineTo(10.2f, 9.6f)
                curveTo(10.2f, 8.4f, 11f, 7.6f, 12f, 7.6f)
                curveTo(13f, 7.6f, 13.8f, 8.4f, 13.8f, 9.6f)
                lineTo(13.8f, 11.2f)
            }
            filled { roundRect(9f, 11.2f, 15f, 16.2f, 1f) }
        }
    }

    /** `bolt.horizontal.circle`: Welcome's "Ready in Minutes". */
    val BoltCircle: ImageVector by lazy {
        icon("ios.boltCircle") {
            stroked(width = 1.6f) { circle(12f, 12f, 9.2f) }
            filled {
                moveTo(5.8f, 12.6f)
                lineTo(10.8f, 8.6f)
                lineTo(10.4f, 11.3f)
                lineTo(18.2f, 11.4f)
                lineTo(13.2f, 15.4f)
                lineTo(13.6f, 12.7f)
                close()
            }
        }
    }

    /** `car.fill`. */
    val CarFill: ImageVector by lazy {
        icon("ios.carFill") {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                moveTo(5.2f, 10f)
                lineTo(6.8f, 5.8f)
                curveTo(7.1f, 5f, 7.8f, 4.5f, 8.6f, 4.5f)
                lineTo(15.4f, 4.5f)
                curveTo(16.2f, 4.5f, 16.9f, 5f, 17.2f, 5.8f)
                lineTo(18.8f, 10f)
                curveTo(20f, 10.3f, 20.8f, 11.3f, 20.8f, 12.6f)
                lineTo(20.8f, 16.8f)
                lineTo(3.2f, 16.8f)
                lineTo(3.2f, 12.6f)
                curveTo(3.2f, 11.3f, 4f, 10.3f, 5.2f, 10f)
                close()
                moveTo(7.2f, 9.8f)
                lineTo(8.3f, 6.6f)
                lineTo(15.7f, 6.6f)
                lineTo(16.8f, 9.8f)
                close()
                circle(6.6f, 13.4f, 1.2f)
                circle(17.4f, 13.4f, 1.2f)
            }
            filled {
                roundRect(4.2f, 16.4f, 7.6f, 19.4f, 1.2f)
                roundRect(16.4f, 16.4f, 19.8f, 19.4f, 1.2f)
            }
        }
    }

    /** `battery.25percent`: Low Battery on the notifications page. */
    val Battery25: ImageVector by lazy {
        icon("ios.battery25") {
            stroked(width = 1.5f) { roundRect(2.5f, 7.5f, 19.5f, 16.5f, 2.8f) }
            filled {
                roundRect(20.5f, 10.3f, 22f, 13.7f, 0.7f)
                roundRect(4.5f, 9.5f, 8.5f, 14.5f, 1.2f)
            }
        }
    }

    /** `lines.measurement.horizontal`: Distance Measurements. */
    val Measurement: ImageVector by lazy {
        icon("ios.measurement") {
            stroked(width = 1.7f) {
                moveTo(3.5f, 8f)
                lineTo(3.5f, 16f)
                moveTo(7.75f, 10.5f)
                lineTo(7.75f, 13.5f)
                moveTo(12f, 5.5f)
                lineTo(12f, 18.5f)
                moveTo(16.25f, 10.5f)
                lineTo(16.25f, 13.5f)
                moveTo(20.5f, 8f)
                lineTo(20.5f, 16f)
            }
        }
    }

    /** `line.3.horizontal.decrease.circle`: Distance Filters. */
    val FilterCircle: ImageVector by lazy {
        icon("ios.filterCircle") {
            stroked(width = 1.5f) {
                circle(12f, 12f, 9.2f)
                moveTo(7.5f, 9f)
                lineTo(16.5f, 9f)
                moveTo(9f, 12f)
                lineTo(15f, 12f)
                moveTo(10.5f, 15f)
                lineTo(13.5f, 15f)
            }
        }
    }

    /** `line.3.horizontal.decrease.circle.fill`: filters that are on. */
    val FilterCircleFill: ImageVector by lazy {
        icon("ios.filterCircleFill") {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                circle(12f, 12f, 9.6f)
                capsule(7.5f, 9f, 16.5f, 9f, 0.8f)
                capsule(9f, 12f, 15f, 12f, 0.8f)
                capsule(10.5f, 15f, 13.5f, 15f, 0.8f)
            }
        }
    }

    /** `hammer`: Tools. */
    val Hammer: ImageVector by lazy {
        icon("ios.hammer") {
            stroked(width = 1.5f) {
                // The handle, then the head across its top.
                moveTo(4.8f, 19.6f)
                lineTo(12.6f, 11.8f)
                moveTo(10.2f, 9.4f)
                lineTo(14.4f, 5.2f)
                curveTo(15.4f, 4.2f, 17.0f, 4.2f, 18.0f, 5.2f)
                lineTo(19.2f, 6.4f)
                lineTo(15.0f, 10.6f)
                moveTo(10.2f, 9.4f)
                lineTo(13.8f, 13.0f)
                lineTo(15.0f, 10.6f)
            }
        }
    }

    /** `tag`: write a contact to an NFC tag. */
    val Tag: ImageVector by lazy {
        icon("ios.tag") {
            stroked(width = 1.5f) {
                moveTo(4.2f, 5.6f)
                curveTo(4.2f, 4.8f, 4.8f, 4.2f, 5.6f, 4.2f)
                lineTo(11.2f, 4.2f)
                lineTo(19.8f, 12.8f)
                lineTo(12.8f, 19.8f)
                lineTo(4.2f, 11.2f)
                close()
                circle(8.4f, 8.4f, 1.3f)
            }
        }
    }

    /** `wave.3.right.circle`: scan an NFC tag. */
    val WaveRightCircle: ImageVector by lazy {
        icon("ios.waveRightCircle") {
            stroked(width = 1.5f) {
                circle(12f, 12f, 9.2f)
                // Three arcs opening to the right of a point at (8, 12), at 45 degrees either side.
                listOf(2.4f, 4.6f, 6.8f).forEach { r ->
                    val d = r * 0.7071f
                    moveTo(7.4f + d, 12f - d)
                    arcTo(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 7.4f + d, y1 = 12f + d)
                }
            }
        }
    }

    /** `mappin`: Mesh Map Location. */
    val MapPin: ImageVector by lazy {
        icon("ios.mapPin") {
            filled { circle(12f, 7.2f, 3.6f) }
            stroked(width = 1.8f) {
                moveTo(12f, 10.6f)
                lineTo(12f, 21.2f)
            }
        }
    }

    /** The Bluetooth rune, drawn in the capsule iOS uses for its custom Bluetooth symbol. */
    val BluetoothRune: ImageVector by lazy {
        icon("ios.bluetoothRune") {
            stroked(width = 1.7f) {
                moveTo(7.4f, 8.2f)
                lineTo(16.4f, 15.7f)
                lineTo(12f, 19.6f)
                lineTo(12f, 4.4f)
                lineTo(16.4f, 8.3f)
                lineTo(7.4f, 15.8f)
            }
        }
    }

    /** `person.and.background.dotted`: Background Connections. */
    val PersonDotted: ImageVector by lazy {
        icon("ios.personDotted") {
            filled {
                circle(12f, 9.4f, 2.8f)
                moveTo(6.8f, 18.4f)
                curveTo(6.8f, 15.3f, 9.1f, 13.6f, 12f, 13.6f)
                curveTo(14.9f, 13.6f, 17.2f, 15.3f, 17.2f, 18.4f)
                close()
                for (x in listOf(3f, 6f, 9f, 12f, 15f, 18f, 21f)) {
                    circle(x, 3f, 0.75f)
                    circle(x, 21f, 0.75f)
                }
                for (y in listOf(6f, 9f, 12f, 15f, 18f)) {
                    circle(3f, y, 0.75f)
                    circle(21f, y, 0.75f)
                }
            }
        }
    }

    /** `wrench.and.screwdriver`: Expert Mode's "Advanced User". */
    val WrenchScrewdriver: ImageVector by lazy {
        icon("ios.wrenchScrewdriver") {
            stroked(width = 1.6f) {
                circle(6.2f, 6.2f, 2.6f)
                moveTo(8.1f, 8.1f)
                lineTo(19.2f, 19.2f)
                moveTo(13.4f, 10.6f)
                lineTo(5.2f, 18.8f)
                moveTo(4.4f, 19.6f)
                lineTo(5.2f, 18.8f)
            }
            stroked(width = 3.2f) {
                moveTo(15.4f, 8.6f)
                lineTo(19.2f, 4.8f)
            }
        }
    }

    /** `eye.slash`: Expert Mode's "Hidden Options". */
    val EyeSlash: ImageVector by lazy {
        icon("ios.eyeSlash") {
            stroked(width = 1.6f) {
                moveTo(2.5f, 12f)
                curveTo(4.8f, 7.8f, 8.2f, 5.8f, 12f, 5.8f)
                curveTo(15.8f, 5.8f, 19.2f, 7.8f, 21.5f, 12f)
                curveTo(19.2f, 16.2f, 15.8f, 18.2f, 12f, 18.2f)
                curveTo(8.2f, 18.2f, 4.8f, 16.2f, 2.5f, 12f)
                close()
                circle(12f, 12f, 3f)
                moveTo(4f, 3.5f)
                lineTo(20f, 20.5f)
            }
        }
    }

    /** `hand.tap`: Expert Mode's "Change Anytime". */
    val HandTap: ImageVector by lazy {
        icon("ios.handTap") {
            stroked(width = 1.5f) {
                moveTo(9.5f, 14.6f)
                lineTo(9.5f, 6f)
                curveTo(9.5f, 5.1f, 10.2f, 4.4f, 11f, 4.4f)
                curveTo(11.8f, 4.4f, 12.5f, 5.1f, 12.5f, 6f)
                lineTo(12.5f, 11.2f)
                curveTo(12.5f, 10.4f, 13.1f, 9.8f, 13.9f, 9.8f)
                curveTo(14.7f, 9.8f, 15.3f, 10.4f, 15.3f, 11.2f)
                lineTo(15.3f, 12f)
                curveTo(15.3f, 11.3f, 15.9f, 10.8f, 16.6f, 10.8f)
                curveTo(17.3f, 10.8f, 17.9f, 11.3f, 17.9f, 12f)
                lineTo(17.9f, 15.6f)
                curveTo(17.9f, 18.9f, 15.6f, 21.2f, 12.6f, 21.2f)
                curveTo(10.6f, 21.2f, 9.2f, 20.4f, 8f, 18.8f)
                lineTo(5.6f, 15.5f)
                curveTo(5.1f, 14.8f, 5.3f, 13.9f, 6f, 13.5f)
                curveTo(6.6f, 13.1f, 7.4f, 13.3f, 7.9f, 13.9f)
                lineTo(9.5f, 15.9f)
                moveTo(7.2f, 3.6f)
                lineTo(8.2f, 4.4f)
                moveTo(14.8f, 3.6f)
                lineTo(13.8f, 4.4f)
                moveTo(11f, 1.6f)
                lineTo(11f, 2.6f)
            }
        }
    }

    /** `antenna.radiowaves.left.and.right` as iOS draws it hierarchically: the waves lighter than the mast. */
    val AntennaTwoTone: ImageVector by lazy {
        icon("ios.antennaTwoTone") {
            filled { circle(12f, 10.5f, 1.7f) }
            stroked(width = 1.8f) {
                moveTo(12f, 12.2f)
                lineTo(12f, 20.5f)
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeAlpha = 0.35f,
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
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

    /** `doc.on.doc`: copy. */
    val DocOnDoc: ImageVector by lazy {
        icon("ios.docOnDoc") {
            stroked(width = 1.5f) {
                roundRect(8.5f, 7.5f, 19.5f, 21f, 2.2f)
                moveTo(15.5f, 5.3f)
                lineTo(15.5f, 4.7f)
                curveTo(15.5f, 3.5f, 14.5f, 2.5f, 13.3f, 2.5f)
                lineTo(6.7f, 2.5f)
                curveTo(5.5f, 2.5f, 4.5f, 3.5f, 4.5f, 4.7f)
                lineTo(4.5f, 14.8f)
                curveTo(4.5f, 16f, 5.5f, 17f, 6.7f, 17f)
                lineTo(6.6f, 17f)
            }
        }
    }

    /** `square.and.arrow.up`: share. */
    val ShareUp: ImageVector by lazy {
        icon("ios.shareUp") {
            stroked(width = 1.6f) {
                moveTo(8.5f, 9f)
                lineTo(6.8f, 9f)
                curveTo(5.8f, 9f, 5f, 9.8f, 5f, 10.8f)
                lineTo(5f, 19.2f)
                curveTo(5f, 20.2f, 5.8f, 21f, 6.8f, 21f)
                lineTo(17.2f, 21f)
                curveTo(18.2f, 21f, 19f, 20.2f, 19f, 19.2f)
                lineTo(19f, 10.8f)
                curveTo(19f, 9.8f, 18.2f, 9f, 17.2f, 9f)
                lineTo(15.5f, 9f)
                moveTo(12f, 14.5f)
                lineTo(12f, 2.8f)
                moveTo(8.6f, 6.2f)
                lineTo(12f, 2.8f)
                lineTo(15.4f, 6.2f)
            }
        }
    }

    /** `person.crop.circle.badge.plus`: Add to Contacts. */
    val PersonCirclePlus: ImageVector by lazy {
        icon("ios.personCirclePlus") {
            stroked(width = 1.5f) {
                moveTo(17.6f, 9.6f)
                curveTo(18f, 10.5f, 18.3f, 11.5f, 18.3f, 12.6f)
                curveTo(18.3f, 17f, 14.8f, 20.5f, 10.4f, 20.5f)
                curveTo(6f, 20.5f, 2.5f, 17f, 2.5f, 12.6f)
                curveTo(2.5f, 8.2f, 6f, 4.7f, 10.4f, 4.7f)
                curveTo(11.5f, 4.7f, 12.5f, 4.9f, 13.4f, 5.3f)
                circle(10.4f, 10.6f, 2.7f)
                moveTo(5.2f, 18f)
                curveTo(6.4f, 16.1f, 8.3f, 15.1f, 10.4f, 15.1f)
                curveTo(12.5f, 15.1f, 14.4f, 16.1f, 15.6f, 18f)
                moveTo(19.4f, 2.6f)
                lineTo(19.4f, 8.2f)
                moveTo(16.6f, 5.4f)
                lineTo(22.2f, 5.4f)
            }
        }
    }

    /** `qrcode`: Show QR. */
    val QrCode: ImageVector by lazy {
        icon("ios.qrcode") {
            stroked(width = 1.5f) {
                roundRect(3.2f, 3.2f, 9.8f, 9.8f, 1.3f)
                roundRect(14.2f, 3.2f, 20.8f, 9.8f, 1.3f)
                roundRect(3.2f, 14.2f, 9.8f, 20.8f, 1.3f)
            }
            filled {
                roundRect(5.4f, 5.4f, 7.6f, 7.6f, 0.4f)
                roundRect(16.4f, 5.4f, 18.6f, 7.6f, 0.4f)
                roundRect(5.4f, 16.4f, 7.6f, 18.6f, 0.4f)
                roundRect(14.2f, 14.2f, 16.4f, 16.4f, 0.4f)
                roundRect(18.6f, 14.2f, 20.8f, 16.4f, 0.4f)
                roundRect(16.4f, 16.4f, 18.6f, 18.6f, 0.4f)
                roundRect(14.2f, 18.6f, 16.4f, 20.8f, 0.4f)
                roundRect(18.6f, 18.6f, 20.8f, 20.8f, 0.4f)
            }
        }
    }

    /** `qrcode.viewfinder`: scan a QR code. */
    val QrViewfinder: ImageVector by lazy {
        icon("ios.qrViewfinder") {
            stroked(width = 1.6f) {
                moveTo(2.8f, 8f)
                lineTo(2.8f, 5f)
                curveTo(2.8f, 3.8f, 3.8f, 2.8f, 5f, 2.8f)
                lineTo(8f, 2.8f)
                moveTo(16f, 2.8f)
                lineTo(19f, 2.8f)
                curveTo(20.2f, 2.8f, 21.2f, 3.8f, 21.2f, 5f)
                lineTo(21.2f, 8f)
                moveTo(21.2f, 16f)
                lineTo(21.2f, 19f)
                curveTo(21.2f, 20.2f, 20.2f, 21.2f, 19f, 21.2f)
                lineTo(16f, 21.2f)
                moveTo(8f, 21.2f)
                lineTo(5f, 21.2f)
                curveTo(3.8f, 21.2f, 2.8f, 20.2f, 2.8f, 19f)
                lineTo(2.8f, 16f)
            }
            filled {
                roundRect(6.8f, 6.8f, 10.6f, 10.6f, 0.7f)
                roundRect(13.4f, 6.8f, 17.2f, 10.6f, 0.7f)
                roundRect(6.8f, 13.4f, 10.6f, 17.2f, 0.7f)
                roundRect(13.4f, 13.4f, 15.2f, 15.2f, 0.4f)
                roundRect(15.4f, 15.4f, 17.2f, 17.2f, 0.4f)
            }
        }
    }

    /** `arrow.up.arrow.down.square`: Firmware Updates. */
    val ArrowsUpDownSquare: ImageVector by lazy {
        icon("ios.arrowsUpDownSquare") {
            stroked(width = 1.5f) {
                roundRect(3.2f, 3.2f, 20.8f, 20.8f, 3.6f)
                moveTo(9f, 17.2f)
                lineTo(9f, 6.8f)
                moveTo(6.4f, 9.4f)
                lineTo(9f, 6.8f)
                lineTo(11.6f, 9.4f)
                moveTo(15f, 6.8f)
                lineTo(15f, 17.2f)
                moveTo(12.4f, 14.6f)
                lineTo(15f, 17.2f)
                lineTo(17.6f, 14.6f)
            }
        }
    }

    /** `person.crop.square.fill`: the User settings. */
    val PersonSquareFill: ImageVector by lazy {
        icon("ios.personSquareFill") {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                roundRect(2.8f, 3.8f, 21.2f, 20.2f, 3.4f)
                circle(12f, 10.2f, 3.1f)
                moveTo(6.4f, 20.2f)
                curveTo(6.9f, 16.9f, 9.2f, 14.9f, 12f, 14.9f)
                curveTo(14.8f, 14.9f, 17.1f, 16.9f, 17.6f, 20.2f)
                close()
            }
        }
    }

    /** `display`: the Display settings, the screen lightly filled as iOS draws it. */
    val DisplayMonitor: ImageVector by lazy {
        icon("ios.display") {
            path(fill = SolidColor(Color.Black), fillAlpha = 0.3f) { roundRect(2.6f, 4f, 21.4f, 16.6f, 2f) }
            stroked(width = 1.5f) {
                roundRect(2.6f, 4f, 21.4f, 16.6f, 2f)
                moveTo(9f, 20.4f)
                lineTo(15f, 20.4f)
                moveTo(12f, 16.6f)
                lineTo(12f, 20.4f)
            }
        }
    }

    /** `light.max` style rays: Ambient Lighting. */
    val Rays: ImageVector by lazy {
        icon("ios.rays") {
            stroked(width = 1.6f) {
                moveTo(12f, 6.5f)
                lineTo(12f, 9.5f)
                moveTo(5.6f, 9.1f)
                lineTo(7.6f, 11.1f)
                moveTo(18.4f, 9.1f)
                lineTo(16.4f, 11.1f)
                moveTo(2.5f, 15.5f)
                lineTo(5.5f, 15.5f)
                moveTo(18.5f, 15.5f)
                lineTo(21.5f, 15.5f)
                moveTo(8.5f, 15.5f)
                lineTo(15.5f, 15.5f)
            }
        }
    }

    /** `wand.and.stars`: Expert Mode. */
    val WandStars: ImageVector by lazy {
        icon("ios.wandStars") {
            stroked(width = 1.7f) {
                moveTo(4f, 20f)
                lineTo(14.4f, 9.6f)
            }
            filled {
                moveTo(17.5f, 2.5f)
                lineTo(18.3f, 4.7f)
                lineTo(20.5f, 5.5f)
                lineTo(18.3f, 6.3f)
                lineTo(17.5f, 8.5f)
                lineTo(16.7f, 6.3f)
                lineTo(14.5f, 5.5f)
                lineTo(16.7f, 4.7f)
                close()
                moveTo(9.5f, 3.5f)
                lineTo(10f, 4.9f)
                lineTo(11.4f, 5.4f)
                lineTo(10f, 5.9f)
                lineTo(9.5f, 7.3f)
                lineTo(9f, 5.9f)
                lineTo(7.6f, 5.4f)
                lineTo(9f, 4.9f)
                close()
                moveTo(19.5f, 11.5f)
                lineTo(20f, 12.9f)
                lineTo(21.4f, 13.4f)
                lineTo(20f, 13.9f)
                lineTo(19.5f, 15.3f)
                lineTo(19f, 13.9f)
                lineTo(17.6f, 13.4f)
                lineTo(19f, 12.9f)
                close()
            }
        }
    }

    /** `battery.100`: Power Mode. */
    val BatteryFull: ImageVector by lazy {
        icon("ios.batteryFull") {
            stroked(width = 1.5f) { roundRect(2.5f, 7.5f, 19.5f, 16.5f, 2.8f) }
            filled {
                roundRect(20.5f, 10.3f, 22f, 13.7f, 0.7f)
                roundRect(4.5f, 9.5f, 17.5f, 14.5f, 1.2f)
            }
        }
    }

    /** `clock`: 12 Hour Clock. */
    val Clock: ImageVector by lazy {
        icon("ios.clock") {
            stroked(width = 1.6f) {
                circle(12f, 12f, 9.2f)
                moveTo(12f, 6.8f)
                lineTo(12f, 12f)
                lineTo(8.6f, 14f)
            }
        }
    }

    /** `bold`: Bold Heading. */
    val BoldB: ImageVector by lazy {
        icon("ios.bold") {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                moveTo(6.5f, 3.8f)
                lineTo(13f, 3.8f)
                curveTo(16.2f, 3.8f, 18f, 5.4f, 18f, 7.9f)
                curveTo(18f, 9.6f, 17f, 10.9f, 15.5f, 11.4f)
                curveTo(17.5f, 11.8f, 18.8f, 13.3f, 18.8f, 15.5f)
                curveTo(18.8f, 18.4f, 16.6f, 20.2f, 13.2f, 20.2f)
                lineTo(6.5f, 20.2f)
                close()
                moveTo(9.9f, 6.6f)
                lineTo(9.9f, 10.4f)
                lineTo(12.4f, 10.4f)
                curveTo(13.8f, 10.4f, 14.6f, 9.7f, 14.6f, 8.5f)
                curveTo(14.6f, 7.3f, 13.8f, 6.6f, 12.4f, 6.6f)
                close()
                moveTo(9.9f, 13f)
                lineTo(9.9f, 17.4f)
                lineTo(12.7f, 17.4f)
                curveTo(14.4f, 17.4f, 15.3f, 16.6f, 15.3f, 15.2f)
                curveTo(15.3f, 13.8f, 14.4f, 13f, 12.7f, 13f)
                close()
            }
        }
    }

    /** `gyroscope`: Wake Screen on tap or motion. */
    val Gyroscope: ImageVector by lazy {
        icon("ios.gyroscope") {
            stroked(width = 1.5f) {
                circle(12f, 12f, 8.6f)
                moveTo(3.4f, 12f)
                curveTo(3.4f, 9.6f, 7.2f, 7.8f, 12f, 7.8f)
                curveTo(16.8f, 7.8f, 20.6f, 9.6f, 20.6f, 12f)
                curveTo(20.6f, 14.4f, 16.8f, 16.2f, 12f, 16.2f)
                curveTo(7.2f, 16.2f, 3.4f, 14.4f, 3.4f, 12f)
                moveTo(5.9f, 5.9f)
                lineTo(18.1f, 18.1f)
            }
            filled { circle(12f, 12f, 1.6f) }
        }
    }

    /** `pip.swap`: Flip Screen. */
    val FlipScreen: ImageVector by lazy {
        icon("ios.flipScreen") {
            stroked(width = 1.5f) {
                roundRect(2.6f, 3.6f, 15.8f, 13.4f, 1.8f)
                moveTo(5.2f, 6.2f)
                lineTo(8.4f, 9.4f)
                moveTo(5.2f, 8.8f)
                lineTo(5.2f, 6.2f)
                lineTo(7.8f, 6.2f)
            }
            filled { roundRect(9.4f, 10.4f, 21.4f, 20.4f, 1.8f) }
        }
    }

    /** `text.alignleft`: Long Name. */
    val TextAlignLeft: ImageVector by lazy {
        icon("ios.textAlignLeft") {
            stroked(width = 1.6f) {
                moveTo(3.5f, 5.5f)
                lineTo(20.5f, 5.5f)
                moveTo(3.5f, 10f)
                lineTo(14.5f, 10f)
                moveTo(3.5f, 14.5f)
                lineTo(20.5f, 14.5f)
                moveTo(3.5f, 19f)
                lineTo(12.5f, 19f)
            }
        }
    }

    /** `circle.fill`: Short Name. */
    val CircleFill: ImageVector by lazy { icon("ios.circleFill") { filled { circle(12f, 12f, 7.5f) } } }

    /** `iphone.slash`: Unmessageable. */
    val PhoneSlash: ImageVector by lazy {
        icon("ios.phoneSlash") {
            stroked(width = 1.5f) {
                moveTo(7f, 4.6f)
                curveTo(7.3f, 3.6f, 8.1f, 2.8f, 9.3f, 2.8f)
                lineTo(15.2f, 2.8f)
                curveTo(16.5f, 2.8f, 17.4f, 3.7f, 17.4f, 5f)
                lineTo(17.4f, 15.6f)
                moveTo(17.2f, 20.1f)
                curveTo(16.8f, 20.8f, 16.1f, 21.2f, 15.2f, 21.2f)
                lineTo(9.3f, 21.2f)
                curveTo(8f, 21.2f, 7.1f, 20.3f, 7.1f, 19f)
                lineTo(7.1f, 8.6f)
                moveTo(10.6f, 18.6f)
                lineTo(13.4f, 18.6f)
                moveTo(4f, 3.5f)
                lineTo(20f, 21f)
            }
        }
    }

    /** `person.text.rectangle`: Licensed Operator. */
    val PersonTextRect: ImageVector by lazy {
        icon("ios.personTextRect") {
            stroked(width = 1.5f) {
                roundRect(2.4f, 4.6f, 21.6f, 19.4f, 2.4f)
                moveTo(13.4f, 9.4f)
                lineTo(18.6f, 9.4f)
                moveTo(13.4f, 12f)
                lineTo(18.6f, 12f)
                moveTo(13.4f, 14.6f)
                lineTo(17f, 14.6f)
            }
            filled {
                circle(8.2f, 10.2f, 1.9f)
                moveTo(4.9f, 15.8f)
                curveTo(5.3f, 14.1f, 6.6f, 13.1f, 8.2f, 13.1f)
                curveTo(9.8f, 13.1f, 11.1f, 14.1f, 11.5f, 15.8f)
                close()
            }
        }
    }

    /** `lightbulb`: Ambient Lighting's LED State. */
    val Lightbulb: ImageVector by lazy {
        icon("ios.lightbulb") {
            stroked(width = 1.5f) {
                moveTo(9.2f, 16.2f)
                curveTo(9.2f, 14.6f, 6f, 12.9f, 6f, 9f)
                curveTo(6f, 5.6f, 8.7f, 2.9f, 12f, 2.9f)
                curveTo(15.3f, 2.9f, 18f, 5.6f, 18f, 9f)
                curveTo(18f, 12.9f, 14.8f, 14.6f, 14.8f, 16.2f)
                close()
                moveTo(9.4f, 18.6f)
                lineTo(14.6f, 18.6f)
                moveTo(10.2f, 21f)
                lineTo(13.8f, 21f)
            }
        }
    }

    /** `eyedropper`: a colour setting. */
    val Eyedropper: ImageVector by lazy {
        icon("ios.eyedropper") {
            stroked(width = 1.4f) { capsule(4.4f, 19.6f, 12.2f, 11.8f, 1.1f) }
            filled {
                capsule(10.9f, 9.1f, 14.9f, 13.1f, 1.2f)
                capsule(14.6f, 9.4f, 18.4f, 5.6f, 2.7f)
            }
        }
    }

    /** `plus`, as on a stepper. */
    val Plus: ImageVector by lazy {
        icon("ios.plus") {
            stroked(width = 2.4f) {
                moveTo(3.6f, 12f)
                lineTo(20.4f, 12f)
                moveTo(12f, 3.6f)
                lineTo(12f, 20.4f)
            }
        }
    }

    /** `minus`, as on a stepper. */
    val Minus: ImageVector by lazy {
        icon("ios.minus") {
            stroked(width = 2.4f) {
                moveTo(3.6f, 12f)
                lineTo(20.4f, 12f)
            }
        }
    }

    /** `xmark`: close a sheet. */
    val Xmark: ImageVector by lazy {
        icon("ios.xmark") {
            stroked(width = 2.2f) {
                moveTo(6f, 6f)
                lineTo(18f, 18f)
                moveTo(18f, 6f)
                lineTo(6f, 18f)
            }
        }
    }

    /** `arrow.triangle.2.circlepath`: refresh, and the empty Firmware Updates page. */
    val ArrowsCirclepath: ImageVector by lazy {
        icon("ios.arrowsCirclepath") {
            stroked(width = 1.6f) {
                moveTo(5.24f, 10.19f)
                arcTo(7f, 7f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 18.89f, y1 = 10.78f)
                moveTo(18.76f, 13.81f)
                arcTo(7f, 7f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 5.11f, y1 = 13.22f)
            }
            filled {
                moveTo(16.2f, 9.9f)
                lineTo(21.6f, 9.9f)
                lineTo(18.9f, 13.7f)
                close()
                moveTo(2.4f, 14.1f)
                lineTo(7.8f, 14.1f)
                lineTo(5.1f, 10.3f)
                close()
            }
        }
    }

    /** `dot.radiowaves.left.and.right` turned upright: LoRa. */
    val LoraWaves: ImageVector by lazy {
        icon("ios.loraWaves") {
            filled { circle(12f, 12f, 1.8f) }
            stroked(width = 1.6f) {
                moveTo(8.75f, 8.75f)
                arcTo(4.6f, 4.6f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 15.25f, y1 = 8.75f)
                moveTo(6.2f, 6.2f)
                arcTo(8.2f, 8.2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 17.8f, y1 = 6.2f)
                moveTo(15.25f, 15.25f)
                arcTo(4.6f, 4.6f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 8.75f, y1 = 15.25f)
                moveTo(17.8f, 17.8f)
                arcTo(8.2f, 8.2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 6.2f, y1 = 17.8f)
            }
        }
    }

    /** `dot.radiowaves.left.and.right`: Mesh Beacon. */
    val RadiowavesLeftRight: ImageVector by lazy {
        icon("ios.radiowavesLeftRight") {
            filled { circle(12f, 12f, 1.8f) }
            stroked(width = 1.6f) {
                moveTo(8.75f, 8.75f)
                arcTo(4.6f, 4.6f, 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 8.75f, y1 = 15.25f)
                moveTo(6.2f, 6.2f)
                arcTo(8.2f, 8.2f, 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 6.2f, y1 = 17.8f)
                moveTo(15.25f, 8.75f)
                arcTo(4.6f, 4.6f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 15.25f, y1 = 15.25f)
                moveTo(17.8f, 6.2f)
                arcTo(8.2f, 8.2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 17.8f, y1 = 17.8f)
            }
        }
    }

    /** `fibrechannel`: Channels. */
    val FibreChannel: ImageVector by lazy {
        icon("ios.fibreChannel") {
            stroked(width = 1.7f) {
                moveTo(21f, 7.5f)
                lineTo(9.5f, 7.5f)
                lineTo(5.8f, 11.6f)
                moveTo(3f, 16.5f)
                lineTo(14.5f, 16.5f)
                lineTo(18.2f, 12.4f)
            }
        }
    }

    /** `flipphone`: Device. */
    val FlipPhone: ImageVector by lazy {
        icon("ios.flipPhone") {
            stroked(width = 1.5f) {
                roundRect(6.5f, 2.5f, 17.5f, 21.5f, 3f)
                moveTo(6.5f, 14.2f)
                lineTo(17.5f, 14.2f)
            }
            filled { roundRect(8.6f, 5f, 15.4f, 11.8f, 1.2f) }
        }
    }

    /** `bolt.fill`: Power. */
    val BoltFill: ImageVector by lazy {
        icon("ios.boltFill") {
            filled {
                moveTo(14f, 1.8f)
                lineTo(4.8f, 13.6f)
                lineTo(11f, 13.6f)
                lineTo(9.8f, 22.2f)
                lineTo(19.2f, 10f)
                lineTo(13f, 10f)
                close()
            }
        }
    }

    /** `waveform`: Audio. */
    val Waveform: ImageVector by lazy {
        icon("ios.waveform") {
            stroked(width = 1.6f) {
                listOf(
                    3f to 2f,
                    6f to 4.5f,
                    9f to 8f,
                    12f to 10f,
                    15f to 6.5f,
                    18f to 4f,
                    21f to 1.5f,
                ).forEach { (x, h) ->
                    moveTo(x, 12f - h)
                    lineTo(x, 12f + h)
                }
            }
        }
    }

    /** `list.bullet.rectangle.fill`: Canned Messages. */
    val ListRectFill: ImageVector by lazy {
        icon("ios.listRectFill") {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                roundRect(2f, 4f, 22f, 20f, 3f)
                listOf(8.4f, 12f, 15.6f).forEach { y ->
                    circle(6.2f, y, 1.05f)
                    roundRect(8.8f, y - 0.8f, 18.6f, y + 0.8f, 0.8f)
                }
            }
        }
    }

    /** `sensor`: Detection Sensor. */
    val Sensor: ImageVector by lazy {
        icon("ios.sensor") {
            stroked(width = 1.5f) {
                roundRect(2.5f, 6f, 15.5f, 18f, 3f)
                listOf(6.5f, 9f, 11.5f).forEach { x ->
                    moveTo(x, 9.5f)
                    lineTo(x, 13f)
                }
                moveTo(18f, 9.2f)
                arcTo(4.2f, 4.2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 18f, y1 = 14.8f)
                moveTo(20.4f, 7.4f)
                arcTo(6.6f, 6.6f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 20.4f, y1 = 16.6f)
            }
        }
    }

    /** `megaphone`: External Notification. */
    val Megaphone: ImageVector by lazy {
        icon("ios.megaphone") {
            stroked(width = 1.5f) {
                moveTo(3f, 9.5f)
                lineTo(3f, 14.5f)
                lineTo(7f, 14.5f)
                lineTo(17.5f, 19.5f)
                lineTo(17.5f, 4.5f)
                lineTo(7f, 9.5f)
                close()
                moveTo(7f, 9.5f)
                lineTo(7f, 14.5f)
                moveTo(7.4f, 14.7f)
                lineTo(8.8f, 19.8f)
                lineTo(11.2f, 19.8f)
                lineTo(10.2f, 16f)
                moveTo(19.8f, 9.6f)
                arcTo(2.6f, 2.6f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 19.8f, y1 = 14.4f)
            }
        }
    }

    /** `point.3.connected.trianglepath.dotted`: Range Test and Trace Routes. */
    val PointsConnected: ImageVector by lazy {
        icon("ios.pointsConnected") {
            stroked(width = 1.5f) {
                circle(5f, 6f, 2.2f)
                circle(19f, 6f, 2.2f)
                circle(12f, 18.5f, 2.2f)
            }
            filled {
                listOf(9.2f, 12f, 14.8f).forEach { x -> circle(x, 6f, 0.75f) }
                listOf(0.3f, 0.52f, 0.74f).forEach { t ->
                    circle(5f + 7f * t, 6f + 12.5f * t, 0.75f)
                    circle(19f - 7f * t, 6f + 12.5f * t, 0.75f)
                }
            }
        }
    }

    /** `figure.walk.motion`: PAX Counter. */
    val FigureWalk: ImageVector by lazy {
        icon("ios.figureWalk") {
            filled { circle(15f, 3.8f, 1.9f) }
            stroked(width = 2f) {
                moveTo(14.2f, 7.2f)
                lineTo(12.6f, 13.2f)
                moveTo(10f, 11f)
                lineTo(13.8f, 7.6f)
                lineTo(17.2f, 11.2f)
                moveTo(12.6f, 13.2f)
                lineTo(10f, 17f)
                lineTo(8f, 21f)
                moveTo(12.6f, 13.2f)
                lineTo(15.2f, 17f)
                lineTo(16.6f, 21f)
            }
            stroked(width = 1.4f) {
                moveTo(2.5f, 8f)
                lineTo(7f, 8f)
                moveTo(3.5f, 11.5f)
                lineTo(7.5f, 11.5f)
                moveTo(2.5f, 15f)
                lineTo(6.5f, 15f)
            }
        }
    }

    /** `terminal`: Serial. */
    val Terminal: ImageVector by lazy {
        icon("ios.terminal") {
            stroked(width = 1.5f) {
                roundRect(2.5f, 4.5f, 21.5f, 19.5f, 2.5f)
                moveTo(6f, 8.5f)
                lineTo(8.8f, 10.8f)
                lineTo(6f, 13.1f)
                moveTo(10.4f, 13.1f)
                lineTo(14f, 13.1f)
            }
        }
    }

    /** `envelope.arrow.triangle.branch`: Store & Forward. */
    val EnvelopeBranch: ImageVector by lazy {
        icon("ios.envelopeBranch") {
            stroked(width = 1.5f) {
                roundRect(3f, 9.5f, 21f, 21f, 2f)
                moveTo(3.4f, 10.2f)
                lineTo(12f, 16f)
                lineTo(20.6f, 10.2f)
                moveTo(12f, 12f)
                lineTo(12f, 7f)
                lineTo(8f, 3f)
                moveTo(12f, 7f)
                lineTo(16f, 3f)
                moveTo(8f, 5.6f)
                lineTo(8f, 3f)
                lineTo(10.6f, 3f)
                moveTo(16f, 5.6f)
                lineTo(16f, 3f)
                lineTo(13.4f, 3f)
            }
        }
    }

    /** `target`: TAK Server. */
    val Target: ImageVector by lazy {
        icon("ios.target") {
            stroked(width = 1.5f) {
                circle(12f, 12f, 9f)
                circle(12f, 12f, 5.4f)
            }
            filled { circle(12f, 12f, 2.2f) }
        }
    }

    /** `chart.xyaxis.line`: Telemetry. */
    val ChartLine: ImageVector by lazy {
        icon("ios.chartLine") {
            stroked(width = 1.5f) {
                moveTo(3f, 3f)
                lineTo(3f, 21f)
                lineTo(21f, 21f)
                moveTo(5.5f, 16.5f)
                lineTo(9.5f, 11.5f)
                lineTo(13.5f, 15f)
                lineTo(19.5f, 8f)
            }
        }
    }

    /** `arrow.triangle.branch`: Traffic Management. */
    val ArrowBranch: ImageVector by lazy {
        icon("ios.arrowBranch") {
            stroked(width = 1.7f) {
                moveTo(12f, 21f)
                lineTo(12f, 12.5f)
                lineTo(6f, 4.5f)
                moveTo(12f, 12.5f)
                lineTo(18f, 4.5f)
                moveTo(6.1f, 8.4f)
                lineTo(6f, 4.5f)
                lineTo(9.7f, 5.4f)
                moveTo(17.9f, 8.4f)
                lineTo(18f, 4.5f)
                lineTo(14.3f, 5.4f)
            }
        }
    }

    /** `square.and.arrow.down`: Import Configuration. */
    val ShareDown: ImageVector by lazy {
        icon("ios.shareDown") {
            stroked(width = 1.6f) {
                moveTo(8.5f, 9f)
                lineTo(6.8f, 9f)
                curveTo(5.8f, 9f, 5f, 9.8f, 5f, 10.8f)
                lineTo(5f, 19.2f)
                curveTo(5f, 20.2f, 5.8f, 21f, 6.8f, 21f)
                lineTo(17.2f, 21f)
                curveTo(18.2f, 21f, 19f, 20.2f, 19f, 19.2f)
                lineTo(19f, 10.8f)
                curveTo(19f, 9.8f, 18.2f, 9f, 17.2f, 9f)
                lineTo(15.5f, 9f)
                moveTo(12f, 2.8f)
                lineTo(12f, 14.5f)
                moveTo(8.6f, 11.1f)
                lineTo(12f, 14.5f)
                lineTo(15.4f, 11.1f)
            }
        }
    }

    /** `scroll`: Logs. */
    val Scroll: ImageVector by lazy {
        icon("ios.scroll") {
            stroked(width = 1.5f) {
                moveTo(6f, 18f)
                lineTo(6f, 5.5f)
                curveTo(6f, 4.1f, 7.1f, 3f, 8.5f, 3f)
                lineTo(18.5f, 3f)
                curveTo(19.9f, 3f, 21f, 4.1f, 21f, 5.5f)
                curveTo(21f, 6.9f, 19.9f, 8f, 18.5f, 8f)
                lineTo(18f, 8f)
                moveTo(18f, 5.5f)
                lineTo(18f, 18.5f)
                curveTo(18f, 19.9f, 16.9f, 21f, 15.5f, 21f)
                lineTo(5.5f, 21f)
                curveTo(4.1f, 21f, 3f, 19.9f, 3f, 18.5f)
                lineTo(3f, 18f)
                lineTo(13f, 18f)
                lineTo(13f, 18.5f)
                curveTo(13f, 19.9f, 14.1f, 21f, 15.5f, 21f)
                moveTo(9f, 8f)
                lineTo(15f, 8f)
                moveTo(9f, 11.5f)
                lineTo(15f, 11.5f)
                moveTo(9f, 15f)
                lineTo(13f, 15f)
            }
        }
    }

    /** `trash`: Clean Node Database. */
    val Trash: ImageVector by lazy {
        icon("ios.trash") {
            stroked(width = 1.5f) {
                moveTo(3.5f, 6.5f)
                lineTo(20.5f, 6.5f)
                moveTo(9f, 6.5f)
                lineTo(9.4f, 3.8f)
                lineTo(14.6f, 3.8f)
                lineTo(15f, 6.5f)
                moveTo(5.6f, 6.5f)
                lineTo(6.8f, 20.6f)
                lineTo(17.2f, 20.6f)
                lineTo(18.4f, 6.5f)
                moveTo(10f, 10f)
                lineTo(10f, 17f)
                moveTo(14f, 10f)
                lineTo(14f, 17f)
            }
        }
    }

    /** `pin`: pin a chat to the top of the list. */
    val Pin: ImageVector by lazy {
        icon("ios.pin") {
            stroked(width = 1.5f) {
                moveTo(8.2f, 3.5f)
                lineTo(15.8f, 3.5f)
                moveTo(9.4f, 3.5f)
                lineTo(9.4f, 9.6f)
                lineTo(6.4f, 13.8f)
                lineTo(17.6f, 13.8f)
                lineTo(14.6f, 9.6f)
                lineTo(14.6f, 3.5f)
                moveTo(12f, 13.8f)
                lineTo(12f, 21f)
            }
        }
    }

    /** `cpu`: Remote Hardware. */
    val Cpu: ImageVector by lazy {
        icon("ios.cpu") {
            stroked(width = 1.5f) {
                roundRect(6f, 6f, 18f, 18f, 2f)
                roundRect(9.2f, 9.2f, 14.8f, 14.8f, 1f)
                listOf(9.5f, 12f, 14.5f).forEach { v ->
                    moveTo(v, 3f)
                    lineTo(v, 6f)
                    moveTo(v, 18f)
                    lineTo(v, 21f)
                    moveTo(3f, v)
                    lineTo(6f, v)
                    moveTo(18f, v)
                    lineTo(21f, v)
                }
            }
        }
    }

    /** `arrow.up`, small: the chip's send light. */
    val ArrowUpThin: ImageVector by lazy {
        icon("ios.arrowUpThin") {
            stroked(width = 2.2f) {
                moveTo(12f, 21f)
                lineTo(12f, 3.5f)
                moveTo(5.5f, 10f)
                lineTo(12f, 3.5f)
                lineTo(18.5f, 10f)
            }
        }
    }

    /** `arrow.down`, small: the chip's receive light. */
    val ArrowDownThin: ImageVector by lazy {
        icon("ios.arrowDownThin") {
            stroked(width = 2.2f) {
                moveTo(12f, 3f)
                lineTo(12f, 20.5f)
                moveTo(5.5f, 14f)
                lineTo(12f, 20.5f)
                lineTo(18.5f, 14f)
            }
        }
    }

    /** `questionmark.circle`: a screen's legend or help. */
    val QuestionCircle: ImageVector by lazy {
        icon("ios.questionCircle") {
            stroked(width = 1.6f) {
                circle(12f, 12f, 9.2f)
                moveTo(9.5f, 9.6f)
                curveTo(9.5f, 8.1f, 10.6f, 7.1f, 12f, 7.1f)
                curveTo(13.4f, 7.1f, 14.5f, 8.1f, 14.5f, 9.4f)
                curveTo(14.5f, 11.2f, 12f, 11.5f, 12f, 13.6f)
            }
            filled { circle(12f, 16.6f, 1.05f) }
        }
    }

    /** `map`, outlined: the map style button. */
    val MapOutline: ImageVector by lazy {
        icon("ios.mapOutline") {
            stroked(width = 1.5f) {
                moveTo(3f, 6.2f)
                lineTo(9f, 4f)
                lineTo(15f, 6.2f)
                lineTo(21f, 4f)
                lineTo(21f, 17.8f)
                lineTo(15f, 20f)
                lineTo(9f, 17.8f)
                lineTo(3f, 20f)
                close()
                moveTo(9f, 4f)
                lineTo(9f, 17.8f)
                moveTo(15f, 6.2f)
                lineTo(15f, 20f)
            }
        }
    }

    /** `square.3.layers.3d`: map layers. */
    val Layers: ImageVector by lazy {
        icon("ios.layers") {
            stroked(width = 1.5f) {
                moveTo(12f, 3f)
                lineTo(21f, 7.5f)
                lineTo(12f, 12f)
                lineTo(3f, 7.5f)
                close()
                moveTo(3f, 12f)
                lineTo(12f, 16.5f)
                lineTo(21f, 12f)
                moveTo(3f, 16.5f)
                lineTo(12f, 21f)
                lineTo(21f, 16.5f)
            }
        }
    }

    /** `plus.square`: add an item. */
    val PlusSquare: ImageVector by lazy {
        icon("ios.plusSquare") {
            stroked(width = 1.5f) {
                roundRect(3.5f, 3.5f, 20.5f, 20.5f, 3.6f)
                moveTo(12f, 7.6f)
                lineTo(12f, 16.4f)
                moveTo(7.6f, 12f)
                lineTo(16.4f, 12f)
            }
        }
    }

    /** `key`: Public Key. */
    val Key: ImageVector by lazy {
        icon("ios.key") {
            stroked(width = 1.5f) {
                circle(8.2f, 8.2f, 4.6f)
                moveTo(11.5f, 11.5f)
                lineTo(20.5f, 20.5f)
                moveTo(17.4f, 17.4f)
                lineTo(19.4f, 15.4f)
                moveTo(15f, 15f)
                lineTo(16.6f, 13.4f)
            }
            filled { circle(7.4f, 7.4f, 1.15f) }
        }
    }

    /** `key.fill`: Private Key. */
    val KeyFill: ImageVector by lazy {
        icon("ios.keyFill") {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                circle(8.2f, 8.2f, 5.2f)
                circle(7.4f, 7.4f, 1.3f)
            }
            stroked(width = 2.4f) {
                moveTo(11.5f, 11.5f)
                lineTo(20.2f, 20.2f)
                moveTo(17.4f, 17.4f)
                lineTo(19.2f, 15.6f)
                moveTo(15f, 15f)
                lineTo(16.4f, 13.6f)
            }
        }
    }

    /** `arrow.clockwise.circle`: Regenerate Private Key. */
    val ArrowClockwiseCircle: ImageVector by lazy {
        icon("ios.arrowClockwiseCircle") {
            stroked(width = 1.5f) {
                circle(12f, 12f, 9.2f)
                moveTo(16.2f, 12.6f)
                arcTo(4.3f, 4.3f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 12.6f, y1 = 7.8f)
            }
            filled {
                moveTo(12.4f, 5.6f)
                lineTo(15.2f, 7.9f)
                lineTo(12.2f, 9.9f)
                close()
            }
        }
    }

    /** `lock.rotation`: regenerate (the round button). */
    val LockRotation: ImageVector by lazy {
        icon("ios.lockRotation") {
            stroked(width = 1.5f) {
                moveTo(19.6f, 8.6f)
                arcTo(8.4f, 8.4f, 0f, isMoreThanHalf = true, isPositiveArc = false, x1 = 20.4f, y1 = 12.8f)
                moveTo(10.3f, 11.4f)
                lineTo(10.3f, 9.9f)
                arcTo(1.7f, 1.7f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 13.7f, y1 = 9.9f)
                lineTo(13.7f, 11.4f)
            }
            filled {
                roundRect(8.9f, 11.2f, 15.1f, 16.2f, 1.2f)
                moveTo(17.8f, 9.4f)
                lineTo(21.3f, 9.8f)
                lineTo(20.2f, 6.4f)
                close()
            }
        }
    }

    /** `cloud`: Key Backup. */
    val Cloud: ImageVector by lazy {
        icon("ios.cloud") {
            stroked(width = 1.5f) {
                moveTo(7f, 18.5f)
                curveTo(4.2f, 18.5f, 2.5f, 16.6f, 2.5f, 14.3f)
                curveTo(2.5f, 12f, 4.2f, 10.3f, 6.4f, 10.1f)
                curveTo(7f, 7.2f, 9.4f, 5.2f, 12.3f, 5.2f)
                curveTo(15.3f, 5.2f, 17.8f, 7.4f, 18.2f, 10.3f)
                curveTo(20.1f, 10.6f, 21.5f, 12.2f, 21.5f, 14.3f)
                curveTo(21.5f, 16.6f, 19.7f, 18.5f, 17.4f, 18.5f)
                close()
            }
        }
    }

    /** `icloud.and.arrow.up`: back up. */
    val CloudArrowUp: ImageVector by lazy {
        icon("ios.cloudArrowUp") {
            stroked(width = 1.5f) {
                moveTo(7f, 18.5f)
                curveTo(4.2f, 18.5f, 2.5f, 16.6f, 2.5f, 14.3f)
                curveTo(2.5f, 12f, 4.2f, 10.3f, 6.4f, 10.1f)
                curveTo(7f, 7.2f, 9.4f, 5.2f, 12.3f, 5.2f)
                curveTo(15.3f, 5.2f, 17.8f, 7.4f, 18.2f, 10.3f)
                curveTo(20.1f, 10.6f, 21.5f, 12.2f, 21.5f, 14.3f)
                curveTo(21.5f, 16.6f, 19.7f, 18.5f, 17.4f, 18.5f)
                close()
                moveTo(12f, 16.4f)
                lineTo(12f, 10.6f)
                moveTo(9.7f, 12.9f)
                lineTo(12f, 10.6f)
                lineTo(14.3f, 12.9f)
            }
        }
    }

    /** `key.icloud`: restore. */
    val CloudKey: ImageVector by lazy {
        icon("ios.cloudKey") {
            stroked(width = 1.5f) {
                moveTo(7f, 18.5f)
                curveTo(4.2f, 18.5f, 2.5f, 16.6f, 2.5f, 14.3f)
                curveTo(2.5f, 12f, 4.2f, 10.3f, 6.4f, 10.1f)
                curveTo(7f, 7.2f, 9.4f, 5.2f, 12.3f, 5.2f)
                curveTo(15.3f, 5.2f, 17.8f, 7.4f, 18.2f, 10.3f)
                curveTo(20.1f, 10.6f, 21.5f, 12.2f, 21.5f, 14.3f)
                curveTo(21.5f, 16.6f, 19.7f, 18.5f, 17.4f, 18.5f)
                close()
                circle(9.6f, 13.4f, 1.5f)
                moveTo(11.1f, 13.4f)
                lineTo(15.4f, 13.4f)
                moveTo(14.2f, 13.4f)
                lineTo(14.2f, 14.8f)
            }
        }
    }

    /** `key.viewfinder`: the admin keys. */
    val KeyViewfinder: ImageVector by lazy {
        icon("ios.keyViewfinder") {
            stroked(width = 1.6f) {
                moveTo(3f, 7.5f)
                lineTo(3f, 5f)
                curveTo(3f, 3.9f, 3.9f, 3f, 5f, 3f)
                lineTo(7.5f, 3f)
                moveTo(16.5f, 3f)
                lineTo(19f, 3f)
                curveTo(20.1f, 3f, 21f, 3.9f, 21f, 5f)
                lineTo(21f, 7.5f)
                moveTo(21f, 16.5f)
                lineTo(21f, 19f)
                curveTo(21f, 20.1f, 20.1f, 21f, 19f, 21f)
                lineTo(16.5f, 21f)
                moveTo(7.5f, 21f)
                lineTo(5f, 21f)
                curveTo(3.9f, 21f, 3f, 20.1f, 3f, 19f)
                lineTo(3f, 16.5f)
                circle(12f, 9.4f, 2.4f)
                moveTo(12f, 11.8f)
                lineTo(12f, 17f)
                moveTo(12f, 14.6f)
                lineTo(13.6f, 14.6f)
            }
        }
    }

    /** `ant.fill`: Debug Logs. */
    val AntFill: ImageVector by lazy {
        icon("ios.antFill") {
            filled {
                circle(12f, 4.9f, 2f)
                circle(12f, 10.2f, 2.5f)
                circle(12f, 16.8f, 3.9f)
            }
            stroked(width = 1.4f) {
                moveTo(10f, 9.4f)
                lineTo(6.5f, 7.6f)
                lineTo(4.6f, 8.6f)
                moveTo(14f, 9.4f)
                lineTo(17.5f, 7.6f)
                lineTo(19.4f, 8.6f)
                moveTo(9.8f, 11.4f)
                lineTo(5.6f, 12.6f)
                moveTo(14.2f, 11.4f)
                lineTo(18.4f, 12.6f)
                moveTo(8.6f, 15.4f)
                lineTo(5.4f, 17.4f)
                lineTo(5.4f, 20.2f)
                moveTo(15.4f, 15.4f)
                lineTo(18.6f, 17.4f)
                lineTo(18.6f, 20.2f)
                moveTo(11f, 3.2f)
                lineTo(9.6f, 1.6f)
                moveTo(13f, 3.2f)
                lineTo(14.4f, 1.6f)
            }
        }
    }

    /** `list.bullet.rectangle`: Use Preset. */
    val ListRect: ImageVector by lazy {
        icon("ios.listRect") {
            stroked(width = 1.5f) {
                roundRect(2.5f, 4.5f, 21.5f, 19.5f, 2.8f)
                listOf(8.6f, 12f, 15.4f).forEach { y ->
                    moveTo(9f, y)
                    lineTo(18.2f, y)
                }
            }
            filled { listOf(8.6f, 12f, 15.4f).forEach { y -> circle(6.2f, y, 0.95f) } }
        }
    }

    /** `server.rack`: Ignore MQTT. */
    val ServerRack: ImageVector by lazy {
        icon("ios.serverRack") {
            stroked(width = 1.5f) {
                roundRect(3f, 4.5f, 21f, 11.2f, 1.6f)
                roundRect(3f, 12.8f, 21f, 19.5f, 1.6f)
                moveTo(10.5f, 7.85f)
                lineTo(17.8f, 7.85f)
                moveTo(10.5f, 16.15f)
                lineTo(17.8f, 16.15f)
            }
            filled {
                circle(6.4f, 7.85f, 1f)
                circle(6.4f, 16.15f, 1f)
            }
        }
    }

    /** `waveform.badge.plus`: RX Boosted Gain. */
    val WaveformPlus: ImageVector by lazy {
        icon("ios.waveformPlus") {
            stroked(width = 1.6f) {
                listOf(2.5f to 1.6f, 5.3f to 4f, 8.1f to 7f, 10.9f to 9f, 13.7f to 5.6f).forEach { (x, h) ->
                    moveTo(x, 11f - h)
                    lineTo(x, 11f + h)
                }
            }
            filled { circle(18.5f, 17.5f, 4.5f) }
            path(fill = SolidColor(Color.White)) {
                roundRect(16.1f, 16.85f, 20.9f, 18.15f, 0.6f)
                roundRect(17.85f, 15.1f, 19.15f, 19.9f, 0.6f)
            }
        }
    }

    /** `wifi`. */
    val Wifi: ImageVector by lazy {
        icon("ios.wifi") {
            stroked(width = 1.8f) {
                moveTo(8.82f, 15.82f)
                arcTo(4.5f, 4.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 15.18f, y1 = 15.82f)
                moveTo(5.64f, 12.64f)
                arcTo(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 18.36f, y1 = 12.64f)
                moveTo(2.45f, 9.45f)
                arcTo(13.5f, 13.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 21.55f, y1 = 9.45f)
            }
            filled { circle(12f, 19f, 1.5f) }
        }
    }

    /** `button.horizontal`: a GPIO button pin. */
    val ButtonHorizontal: ImageVector by lazy {
        icon("ios.buttonHorizontal") { stroked(width = 1.5f) { roundRect(3f, 8f, 21f, 16f, 4f) } }
    }

    /** `repeat`: Echo. */
    val Repeat: ImageVector by lazy {
        icon("ios.repeat") {
            stroked(width = 1.6f) {
                moveTo(5f, 11.5f)
                lineTo(5f, 9f)
                curveTo(5f, 7.9f, 5.9f, 7f, 7f, 7f)
                lineTo(19f, 7f)
                moveTo(16.5f, 4.5f)
                lineTo(19f, 7f)
                lineTo(16.5f, 9.5f)
                moveTo(19f, 12.5f)
                lineTo(19f, 15f)
                curveTo(19f, 16.1f, 18.1f, 17f, 17f, 17f)
                lineTo(5f, 17f)
                moveTo(7.5f, 14.5f)
                lineTo(5f, 17f)
                lineTo(7.5f, 19.5f)
            }
        }
    }

    /** `waveform.path.ecg`: heartbeat. */
    val Ecg: ImageVector by lazy {
        icon("ios.ecg") {
            stroked(width = 1.6f) {
                moveTo(2f, 12f)
                lineTo(7f, 12f)
                lineTo(9f, 7f)
                lineTo(12f, 17f)
                lineTo(14.5f, 9f)
                lineTo(16f, 12f)
                lineTo(22f, 12f)
            }
        }
    }

    /** `thermometer`. */
    val Thermometer: ImageVector by lazy {
        icon("ios.thermometer") {
            stroked(width = 1.5f) { roundRect(9.8f, 2.8f, 14.2f, 15.4f, 2.2f) }
            filled {
                circle(12f, 17.9f, 3.3f)
                roundRect(11.2f, 8f, 12.8f, 16f, 0.8f)
            }
        }
    }

    /** `aqi.medium`: air quality. */
    val AqiMedium: ImageVector by lazy {
        icon("ios.aqiMedium") {
            filled {
                for (row in 0..4) {
                    for (col in 0..4) {
                        if ((row == 0 || row == 4) && (col == 0 || col == 4)) continue
                        circle(4f + col * 4f, 4f + row * 4f, if ((row + col) % 2 == 0) 1.1f else 0.8f)
                    }
                }
            }
        }
    }

    /** `dot.radiowaves.right`: a sensor or radio sending. */
    val DotRadiowavesRight: ImageVector by lazy {
        icon("ios.dotRadiowavesRight") {
            filled { circle(6f, 12f, 1.8f) }
            stroked(width = 1.6f) {
                moveTo(9.18f, 8.82f)
                arcTo(4.5f, 4.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 9.18f, y1 = 15.18f)
                moveTo(12.01f, 5.99f)
                arcTo(8.5f, 8.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 12.01f, y1 = 18.01f)
            }
        }
    }

    /** `signature`: a name. */
    val Signature: ImageVector by lazy {
        icon("ios.signature") {
            stroked(width = 1.5f) {
                moveTo(3f, 15f)
                curveTo(6f, 8f, 8f, 8f, 8f, 12f)
                curveTo(8f, 16f, 10f, 16f, 12f, 12f)
                curveTo(14f, 8f, 15f, 9f, 15f, 13f)
                lineTo(21f, 13f)
                moveTo(3f, 19f)
                lineTo(21f, 19f)
            }
        }
    }

    /** `arrow.up.to.line`: pull-up. */
    val ArrowUpToLine: ImageVector by lazy {
        icon("ios.arrowUpToLine") {
            stroked(width = 1.6f) {
                moveTo(5f, 4f)
                lineTo(19f, 4f)
                moveTo(12f, 20f)
                lineTo(12f, 8f)
                moveTo(7.5f, 12.5f)
                lineTo(12f, 8f)
                lineTo(16.5f, 12.5f)
            }
        }
    }

    /** `wallet.pass`: a password. */
    val WalletPass: ImageVector by lazy {
        icon("ios.walletPass") {
            stroked(width = 1.5f) {
                roundRect(4.5f, 3f, 19.5f, 21f, 2.5f)
                moveTo(4.5f, 9f)
                lineTo(19.5f, 9f)
                moveTo(8f, 14f)
                lineTo(16f, 14f)
            }
        }
    }

    /** `number`. */
    val Number: ImageVector by lazy {
        icon("ios.number") {
            stroked(width = 1.6f) {
                moveTo(9.5f, 4f)
                lineTo(8f, 20f)
                moveTo(16f, 4f)
                lineTo(14.5f, 20f)
                moveTo(4.5f, 9f)
                lineTo(19.5f, 9f)
                moveTo(4f, 15f)
                lineTo(19f, 15f)
            }
        }
    }

    /** `circle.grid.cross`: a subnet. */
    val CircleGridCross: ImageVector by lazy {
        icon("ios.circleGridCross") {
            stroked(width = 1.5f) {
                circle(12f, 12f, 9f)
                moveTo(12f, 3f)
                lineTo(12f, 21f)
                moveTo(3f, 12f)
                lineTo(21f, 12f)
            }
        }
    }

    /** `power`. */
    val Power: ImageVector by lazy {
        icon("ios.power") {
            stroked(width = 1.8f) {
                moveTo(7.4f, 6.6f)
                arcTo(7.6f, 7.6f, 0f, isMoreThanHalf = true, isPositiveArc = false, x1 = 16.6f, y1 = 6.6f)
                moveTo(12f, 3f)
                lineTo(12f, 11f)
            }
        }
    }

    /** `light.beacon.max.fill`: buzzer or light output. */
    val Beacon: ImageVector by lazy {
        icon("ios.beacon") {
            filled {
                moveTo(7.5f, 17f)
                lineTo(7.5f, 12.5f)
                arcTo(4.5f, 4.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 16.5f, y1 = 12.5f)
                lineTo(16.5f, 17f)
                close()
                roundRect(5f, 17.6f, 19f, 20.6f, 1f)
            }
            stroked(width = 1.5f) {
                moveTo(12f, 2.5f)
                lineTo(12f, 5f)
                moveTo(5.5f, 5f)
                lineTo(7.2f, 6.9f)
                moveTo(18.5f, 5f)
                lineTo(16.8f, 6.9f)
                moveTo(2.5f, 10.5f)
                lineTo(5f, 11.3f)
                moveTo(21.5f, 10.5f)
                lineTo(19f, 11.3f)
            }
        }
    }

    /** `dial.min`: a rotary encoder. */
    val Dial: ImageVector by lazy {
        icon("ios.dial") {
            stroked(width = 1.5f) {
                circle(12f, 12f, 5.6f)
                moveTo(12f, 12f)
                lineTo(9.4f, 9.4f)
                for (i in 0 until DIAL_TICKS) spoke(12f, 12f, 8.2f, 9.8f, i * 360f / DIAL_TICKS)
            }
        }
    }

    private const val DIAL_TICKS = 12

    /** `arrow.up.arrow.down`. */
    val ArrowsUpDown: ImageVector by lazy {
        icon("ios.arrowsUpDown") {
            stroked(width = 1.6f) {
                moveTo(8f, 20f)
                lineTo(8f, 4f)
                moveTo(4.5f, 7.5f)
                lineTo(8f, 4f)
                lineTo(11.5f, 7.5f)
                moveTo(16f, 4f)
                lineTo(16f, 20f)
                moveTo(12.5f, 16.5f)
                lineTo(16f, 20f)
                lineTo(19.5f, 16.5f)
            }
        }
    }

    /** `iphone.radiowaves.left.and.right`: the phone relays for the radio. */
    val PhoneWaves: ImageVector by lazy {
        icon("ios.phoneWaves") {
            stroked(width = 1.5f) {
                roundRect(9f, 4f, 15f, 20f, 2f)
                moveTo(6.5f, 8.5f)
                arcTo(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 6.5f, y1 = 15.5f)
                moveTo(4f, 6f)
                arcTo(8.5f, 8.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, x1 = 4f, y1 = 18f)
                moveTo(17.5f, 8.5f)
                arcTo(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 17.5f, y1 = 15.5f)
                moveTo(20f, 6f)
                arcTo(8.5f, 8.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 20f, y1 = 18f)
            }
        }
    }

    /** `lock.icloud`: encryption. */
    val CloudLock: ImageVector by lazy {
        icon("ios.cloudLock") {
            stroked(width = 1.5f) {
                moveTo(7f, 18.5f)
                curveTo(4.2f, 18.5f, 2.5f, 16.6f, 2.5f, 14.3f)
                curveTo(2.5f, 12f, 4.2f, 10.3f, 6.4f, 10.1f)
                curveTo(7f, 7.2f, 9.4f, 5.2f, 12.3f, 5.2f)
                curveTo(15.3f, 5.2f, 17.8f, 7.4f, 18.2f, 10.3f)
                curveTo(20.1f, 10.6f, 21.5f, 12.2f, 21.5f, 14.3f)
                curveTo(21.5f, 16.6f, 19.7f, 18.5f, 17.4f, 18.5f)
                close()
                moveTo(10.4f, 12.8f)
                lineTo(10.4f, 11.9f)
                arcTo(1.6f, 1.6f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 13.6f, y1 = 11.9f)
                lineTo(13.6f, 12.8f)
            }
            filled { roundRect(9.4f, 12.6f, 14.6f, 16.6f, 0.8f) }
        }
    }

    /** `tree`: the MQTT root topic. */
    val Tree: ImageVector by lazy {
        icon("ios.tree") {
            stroked(width = 1.5f) {
                // A cloud-shaped canopy, open where the trunk leaves it.
                moveTo(10.8f, 15.6f)
                lineTo(8f, 15.6f)
                curveTo(5.1f, 15.6f, 3f, 13.6f, 3.2f, 11.1f)
                curveTo(3.4f, 9.1f, 4.9f, 7.8f, 6.6f, 7.6f)
                curveTo(7.1f, 4.9f, 9.3f, 3f, 12f, 3f)
                curveTo(14.7f, 3f, 16.9f, 4.9f, 17.4f, 7.6f)
                curveTo(19.1f, 7.8f, 20.6f, 9.1f, 20.8f, 11.1f)
                curveTo(21f, 13.6f, 18.9f, 15.6f, 16f, 15.6f)
                lineTo(13.2f, 15.6f)
                moveTo(12f, 21f)
                lineTo(12f, 10.5f)
                moveTo(12f, 15f)
                lineTo(9.4f, 12.4f)
                moveTo(12f, 13.4f)
                lineTo(14.6f, 10.8f)
            }
        }
    }

    /** `checkmark.shield.fill`: TLS. */
    val ShieldCheck: ImageVector by lazy {
        icon("ios.shieldCheck") {
            filled {
                moveTo(12f, 2.5f)
                lineTo(19.5f, 5.5f)
                lineTo(19.5f, 11f)
                curveTo(19.5f, 16f, 16f, 19.5f, 12f, 21.5f)
                curveTo(8f, 19.5f, 4.5f, 16f, 4.5f, 11f)
                lineTo(4.5f, 5.5f)
                close()
            }
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(8.6f, 12f)
                lineTo(11f, 14.4f)
                lineTo(15.6f, 9.4f)
            }
        }
    }

    /** `brain`: Smart Position. */
    val Brain: ImageVector by lazy {
        icon("ios.brain") {
            stroked(width = 1.4f) {
                moveTo(12f, 4.5f)
                curveTo(9f, 3f, 5.5f, 5f, 6f, 8f)
                curveTo(3.5f, 9f, 3.5f, 13f, 5.5f, 14f)
                curveTo(5f, 17.5f, 8.5f, 19.5f, 12f, 18.5f)
                moveTo(12f, 4.5f)
                curveTo(15f, 3f, 18.5f, 5f, 18f, 8f)
                curveTo(20.5f, 9f, 20.5f, 13f, 18.5f, 14f)
                curveTo(19f, 17.5f, 15.5f, 19.5f, 12f, 18.5f)
                moveTo(12f, 4.5f)
                lineTo(12f, 18.5f)
                moveTo(8f, 9f)
                curveTo(9.5f, 9f, 10f, 10f, 10f, 11f)
                moveTo(16f, 9f)
                curveTo(14.5f, 9f, 14f, 10f, 14f, 11f)
                moveTo(7.5f, 14.5f)
                curveTo(8.5f, 13.6f, 9.8f, 13.6f, 10.4f, 14.6f)
                moveTo(16.5f, 14.5f)
                curveTo(15.5f, 13.6f, 14.2f, 13.6f, 13.6f, 14.6f)
            }
        }
    }

    /** `location.square.fill`: Fixed Position. */
    val LocationSquareFill: ImageVector by lazy {
        icon("ios.locationSquareFill") {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                roundRect(3f, 3f, 21f, 21f, 4f)
                moveTo(16.8f, 7.2f)
                lineTo(13.4f, 17f)
                lineTo(12f, 12f)
                lineTo(7f, 10.6f)
                close()
            }
        }
    }

    /** `paintpalette`: TAK team. */
    val PaintPalette: ImageVector by lazy {
        icon("ios.paintPalette") {
            stroked(width = 1.5f) {
                moveTo(12f, 3f)
                curveTo(6.5f, 3f, 3f, 7f, 3f, 11.5f)
                curveTo(3f, 16.5f, 7f, 20.5f, 11.5f, 20.5f)
                curveTo(13f, 20.5f, 13.5f, 19.5f, 13f, 18.3f)
                curveTo(12.5f, 17f, 13.4f, 16f, 14.8f, 16f)
                lineTo(16.5f, 16f)
                curveTo(19f, 16f, 21f, 14f, 21f, 11.5f)
                curveTo(21f, 6.8f, 17f, 3f, 12f, 3f)
                close()
            }
            filled {
                circle(7.5f, 11f, 1.2f)
                circle(9.5f, 7.3f, 1.2f)
                circle(14.3f, 6.8f, 1.2f)
                circle(17.5f, 9.8f, 1.2f)
            }
        }
    }

    /** `person.badge.shield.checkmark`: TAK role. */
    val PersonShield: ImageVector by lazy {
        icon("ios.personShield") {
            stroked(width = 1.5f) {
                circle(9f, 8f, 3f)
                moveTo(3.5f, 19f)
                curveTo(4f, 15f, 6.3f, 13.3f, 9f, 13.3f)
                curveTo(10.3f, 13.3f, 11.5f, 13.7f, 12.4f, 14.4f)
            }
            filled {
                moveTo(17f, 10.5f)
                lineTo(21f, 12f)
                lineTo(21f, 15f)
                curveTo(21f, 18f, 19f, 19.8f, 17f, 20.8f)
                curveTo(15f, 19.8f, 13f, 18f, 13f, 15f)
                lineTo(13f, 12f)
                close()
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

    /** `arrow.up`: the Altitude position flag. */
    val ArrowUp: ImageVector by lazy {
        icon("ios.arrowUp") {
            stroked(width = 1.6f) {
                moveTo(12f, 20.5f)
                lineTo(12f, 3.8f)
                moveTo(5.6f, 10.2f)
                lineTo(12f, 3.8f)
                lineTo(18.4f, 10.2f)
            }
        }
    }

    /** `skew`: a slanted frame with its corners marked; the satellites-in-view position flag. */
    val Skew: ImageVector by lazy {
        icon("ios.skew") {
            stroked(width = 1.5f) {
                moveTo(7.6f, 5f)
                lineTo(20.5f, 5f)
                lineTo(16.4f, 19f)
                lineTo(3.5f, 19f)
                close()
            }
            filled {
                roundRect(6.2f, 3.6f, 9f, 6.4f, 0.5f)
                roundRect(19.1f, 3.6f, 21.9f, 6.4f, 0.5f)
                roundRect(15f, 17.6f, 17.8f, 20.4f, 0.5f)
                roundRect(2.1f, 17.6f, 4.9f, 20.4f, 0.5f)
            }
        }
    }

    /** `speedometer`: a dial with its needle; the speed position flag. */
    val Speedometer: ImageVector by lazy {
        icon("ios.speedometer") {
            stroked(width = 1.6f) {
                moveTo(5.6f, 18.4f)
                arcTo(9f, 9f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 18.4f, y1 = 18.4f)
                moveTo(12f, 13f)
                lineTo(15.6f, 8.6f)
                moveTo(12f, 5.4f)
                lineTo(12f, 6.8f)
                moveTo(5.6f, 11.6f)
                lineTo(7f, 11.9f)
                moveTo(18.4f, 11.6f)
                lineTo(17f, 11.9f)
            }
            filled { circle(12f, 13f, 1.5f) }
        }
    }

    /** `location.circle`: a location arrow in a circle; the heading position flag. */
    val LocationCircle: ImageVector by lazy {
        icon("ios.locationCircle") {
            stroked(width = 1.5f) {
                circle(12f, 12f, 9.2f)
                moveTo(16.4f, 7.6f)
                lineTo(7f, 11.4f)
                lineTo(11.2f, 12.7f)
                lineTo(12.5f, 16.9f)
                close()
            }
        }
    }

    /** `exclamationmark.triangle`: a caution, outlined. */
    val Warning: ImageVector by lazy {
        icon("ios.warning") {
            stroked(width = 1.5f) {
                moveTo(10.5f, 4.2f)
                curveTo(11.2f, 3f, 12.8f, 3f, 13.5f, 4.2f)
                lineTo(21.2f, 17.7f)
                curveTo(21.9f, 18.9f, 21f, 20.4f, 19.6f, 20.4f)
                lineTo(4.4f, 20.4f)
                curveTo(3f, 20.4f, 2.1f, 18.9f, 2.8f, 17.7f)
                close()
                moveTo(12f, 9f)
                lineTo(12f, 13.8f)
            }
            filled { circle(12f, 16.9f, 1f) }
        }
    }

    /** `dot.radiowaves.up.forward`: a dot with waves rising from it; MQTT. */
    val DotRadiowavesUpForward: ImageVector by lazy {
        icon("ios.dotRadiowavesUpForward") {
            filled { circle(5.8f, 18.2f, 2f) }
            stroked(width = 1.7f) {
                moveTo(5f, 11.4f)
                arcTo(7.6f, 7.6f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 12.6f, y1 = 19f)
                moveTo(5f, 5.4f)
                arcTo(13.6f, 13.6f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 18.6f, y1 = 19f)
            }
        }
    }

    /** `star`: Add to favorites. */
    val Star: ImageVector by lazy {
        icon("ios.star") {
            stroked(width = 1.5f) {
                moveTo(12.00f, 3.00f)
                lineTo(14.47f, 9.20f)
                lineTo(21.13f, 9.63f)
                lineTo(15.99f, 13.90f)
                lineTo(17.64f, 20.37f)
                lineTo(12.00f, 16.80f)
                lineTo(6.36f, 20.37f)
                lineTo(8.01f, 13.90f)
                lineTo(2.87f, 9.63f)
                lineTo(9.53f, 9.20f)
                close()
            }
        }
    }

    /** `star.fill`: a favourite; Remove from favorites. */
    val StarFill: ImageVector by lazy {
        icon("ios.starFill") {
            filled {
                moveTo(12.00f, 3.00f)
                lineTo(14.47f, 9.20f)
                lineTo(21.13f, 9.63f)
                lineTo(15.99f, 13.90f)
                lineTo(17.64f, 20.37f)
                lineTo(12.00f, 16.80f)
                lineTo(6.36f, 20.37f)
                lineTo(8.01f, 13.90f)
                lineTo(2.87f, 9.63f)
                lineTo(9.53f, 9.20f)
                close()
            }
        }
    }

    /** `minus.circle`: Ignore. */
    val MinusCircle: ImageVector by lazy {
        icon("ios.minusCircle") {
            stroked(width = 1.5f) {
                circle(12f, 12f, 9.2f)
                moveTo(7.6f, 12f)
                lineTo(16.4f, 12f)
            }
        }
    }

    /** `chart.bar`: local stats. */
    val ChartBar: ImageVector by lazy {
        icon("ios.chartBar") {
            stroked(width = 1.5f) {
                roundRect(3.2f, 12.5f, 7.6f, 20f, 1f)
                roundRect(9.8f, 8f, 14.2f, 20f, 1f)
                roundRect(16.4f, 4f, 20.8f, 20f, 1f)
            }
        }
    }

    /** `signpost.right.and.left`: trace route. */
    val Signpost: ImageVector by lazy {
        icon("ios.signpost") {
            stroked(width = 1.5f) {
                moveTo(12f, 2.8f)
                lineTo(12f, 21.2f)
                moveTo(8f, 21.2f)
                lineTo(16f, 21.2f)
                moveTo(12f, 5f)
                lineTo(18.3f, 5f)
                lineTo(20.5f, 7.2f)
                lineTo(18.3f, 9.4f)
                lineTo(12f, 9.4f)
                moveTo(12f, 11.8f)
                lineTo(5.7f, 11.8f)
                lineTo(3.5f, 14f)
                lineTo(5.7f, 16.2f)
                lineTo(12f, 16.2f)
            }
        }
    }

    /** `person.2.badge.gearshape`: Exchange User Info. */
    val PeopleGear: ImageVector by lazy {
        icon("ios.peopleGear") {
            stroked(width = 1.5f) {
                circle(8.6f, 7.6f, 3.2f)
                moveTo(2.6f, 19.6f)
                curveTo(2.6f, 15.8f, 5.3f, 13.6f, 8.6f, 13.6f)
                curveTo(10.1f, 13.6f, 11.4f, 14f, 12.4f, 14.8f)
                moveTo(15.2f, 9.6f)
                curveTo(16.8f, 9.6f, 18f, 8.4f, 18f, 6.8f)
                curveTo(18f, 5.2f, 16.8f, 4f, 15.2f, 4f)
                gearOutline(17.6f, 17.6f, 3.6f, 2.6f, 6)
            }
            filled { circle(17.6f, 17.6f, 1f) }
        }
    }

    /** `mappin.and.ellipse`: Position Log. */
    val MapPinEllipse: ImageVector by lazy {
        icon("ios.mapPinEllipse") {
            stroked(width = 1.5f) {
                circle(12f, 7.4f, 3.6f)
                moveTo(12f, 11f)
                lineTo(12f, 18.4f)
                moveTo(8f, 15.6f)
                curveTo(4.9f, 16.1f, 3f, 17.1f, 3f, 18.3f)
                curveTo(3f, 19.9f, 7f, 21.2f, 12f, 21.2f)
                curveTo(17f, 21.2f, 21f, 19.9f, 21f, 18.3f)
                curveTo(21f, 17.1f, 19.1f, 16.1f, 16f, 15.6f)
            }
        }
    }

    /** `cloud.sun.rain`: Environment Metrics Log. */
    val CloudSunRain: ImageVector by lazy {
        icon("ios.cloudSunRain") {
            stroked(width = 1.5f) {
                moveTo(8.2f, 17f)
                lineTo(6.4f, 17f)
                curveTo(4.4f, 17f, 2.8f, 15.5f, 2.8f, 13.6f)
                curveTo(2.8f, 11.8f, 4.2f, 10.4f, 6f, 10.2f)
                curveTo(6.6f, 8.2f, 8.4f, 6.8f, 10.6f, 6.8f)
                curveTo(13f, 6.8f, 15f, 8.5f, 15.3f, 10.8f)
                curveTo(17f, 10.9f, 18.3f, 12.2f, 18.3f, 13.9f)
                curveTo(18.3f, 15.6f, 16.9f, 17f, 15.2f, 17f)
                lineTo(14.6f, 17f)
                moveTo(9.4f, 18.4f)
                lineTo(8.6f, 20.6f)
                moveTo(12.4f, 18.4f)
                lineTo(11.6f, 20.6f)
                moveTo(13.2f, 6.9f)
                curveTo(13.9f, 5.4f, 15.4f, 4.4f, 17.1f, 4.4f)
                curveTo(19.5f, 4.4f, 21.4f, 6.3f, 21.4f, 8.7f)
                curveTo(21.4f, 9.9f, 20.9f, 11f, 20.1f, 11.8f)
                moveTo(17.1f, 1.4f)
                lineTo(17.1f, 2.4f)
                moveTo(22.6f, 8.7f)
                lineTo(23.4f, 8.7f)
            }
        }
    }

    /** `bolt`: Power Metrics Log. */
    val Bolt: ImageVector by lazy {
        icon("ios.bolt") {
            stroked(width = 1.5f) {
                moveTo(13.6f, 2.6f)
                lineTo(5.4f, 13.6f)
                lineTo(11.4f, 13.6f)
                lineTo(10.4f, 21.4f)
                lineTo(18.6f, 10.4f)
                lineTo(12.6f, 10.4f)
                close()
            }
        }
    }

    /** `person.crop.circle`: a node's Name. */
    val PersonCropCircle: ImageVector by lazy {
        icon("ios.personCropCircle") {
            stroked(width = 1.5f) {
                circle(12f, 12f, 9.4f)
                circle(12f, 9.6f, 3.4f)
                moveTo(5.8f, 18.8f)
                curveTo(7.1f, 16.4f, 9.4f, 15.2f, 12f, 15.2f)
                curveTo(14.6f, 15.2f, 16.9f, 16.4f, 18.2f, 18.8f)
            }
        }
    }

    /** `clock.arrow.circlepath`: Last heard. */
    val ClockArrowCirclepath: ImageVector by lazy {
        icon("ios.clockArrowCirclepath") {
            stroked(width = 1.5f) {
                moveTo(4.2f, 9.4f)
                arcTo(8.6f, 8.6f, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = 3.6f, y1 = 13.6f)
                moveTo(2.2f, 6.8f)
                lineTo(4.2f, 9.4f)
                lineTo(6.9f, 7.7f)
                moveTo(12.2f, 7.4f)
                lineTo(12.2f, 12.4f)
                lineTo(15.4f, 14.4f)
            }
        }
    }

    /** `checkmark.circle.fill`: Uptime. The check is cut out of the disc. */
    val CheckCircleFill: ImageVector by lazy {
        icon("ios.checkCircleFill") {
            path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
                circle(12f, 12f, 9.6f)
                moveTo(7.2f, 12.2f)
                lineTo(8.3f, 11.1f)
                lineTo(10.6f, 13.4f)
                lineTo(15.7f, 8.3f)
                lineTo(16.8f, 9.4f)
                lineTo(10.6f, 15.6f)
                close()
            }
        }
    }

    /** `flag`: Getting Started. */
    val Flag: ImageVector by lazy {
        icon("ios.flag") {
            stroked(width = 1.5f) {
                moveTo(5f, 21f)
                lineTo(5f, 3.6f)
                moveTo(5f, 4.2f)
                curveTo(7.6f, 3f, 9.6f, 3.2f, 12f, 4.4f)
                curveTo(14.4f, 5.6f, 16.4f, 5.8f, 19f, 4.6f)
                lineTo(19f, 13.6f)
                curveTo(16.4f, 14.8f, 14.4f, 14.6f, 12f, 13.4f)
                curveTo(9.6f, 12.2f, 7.6f, 12f, 5f, 13.2f)
            }
        }
    }

    /** `bubble.left.and.bubble.right`: Messaging. */
    val BubblesTwo: ImageVector by lazy {
        icon("ios.bubblesTwo") {
            stroked(width = 1.5f) {
                moveTo(4.6f, 4.4f)
                lineTo(13.4f, 4.4f)
                curveTo(14.7f, 4.4f, 15.6f, 5.3f, 15.6f, 6.6f)
                lineTo(15.6f, 10.6f)
                curveTo(15.6f, 11.9f, 14.7f, 12.8f, 13.4f, 12.8f)
                lineTo(8.2f, 12.8f)
                lineTo(5f, 15.4f)
                lineTo(5f, 12.8f)
                lineTo(4.6f, 12.8f)
                curveTo(3.3f, 12.8f, 2.4f, 11.9f, 2.4f, 10.6f)
                lineTo(2.4f, 6.6f)
                curveTo(2.4f, 5.3f, 3.3f, 4.4f, 4.6f, 4.4f)
                close()
                moveTo(17.8f, 8.6f)
                lineTo(19.4f, 8.6f)
                curveTo(20.7f, 8.6f, 21.6f, 9.5f, 21.6f, 10.8f)
                lineTo(21.6f, 14.8f)
                curveTo(21.6f, 16.1f, 20.7f, 17f, 19.4f, 17f)
                lineTo(19f, 17f)
                lineTo(19f, 19.6f)
                lineTo(15.8f, 17f)
                lineTo(10.6f, 17f)
                curveTo(9.3f, 17f, 8.4f, 16.1f, 8.4f, 14.8f)
            }
        }
    }

    /** `arrow.up.forward`, small: a link that opens in the browser. */
    val ArrowUpForward: ImageVector by lazy {
        icon("ios.arrowUpForward") {
            stroked(width = 1.8f) {
                moveTo(6.5f, 17.5f)
                lineTo(17f, 7f)
                moveTo(8.5f, 7f)
                lineTo(17f, 7f)
                lineTo(17f, 15.5f)
            }
        }
    }

    /** `lock`, outlined: Privacy & Security. */
    val LockOutline: ImageVector by lazy {
        icon("ios.lockOutline") {
            stroked(width = 1.5f) {
                roundRect(5.4f, 10.4f, 18.6f, 21f, 2.2f)
                moveTo(8.2f, 10.4f)
                lineTo(8.2f, 7.4f)
                curveTo(8.2f, 5.2f, 9.9f, 3.4f, 12f, 3.4f)
                curveTo(14.1f, 3.4f, 15.8f, 5.2f, 15.8f, 7.4f)
                lineTo(15.8f, 10.4f)
            }
        }
    }

    /** `cellularbars`: Signal Meter. */
    val CellularBars: ImageVector by lazy {
        icon("ios.cellularBars") {
            filled {
                roundRect(2.6f, 14.6f, 6.2f, 20f, 0.8f)
                roundRect(7.8f, 11f, 11.4f, 20f, 0.8f)
                roundRect(13f, 7.4f, 16.6f, 20f, 0.8f)
                roundRect(18.2f, 3.8f, 21.8f, 20f, 0.8f)
            }
        }
    }

    /** `circle.dashed`: a geofence's radius. */
    val CircleDashed: ImageVector by lazy {
        icon("ios.circleDashed") {
            stroked(width = 1.6f) {
                val dashes = 12
                val r = 8.6f
                for (i in 0 until dashes) {
                    val a0 = i * 2 * PI.toFloat() / dashes
                    val a1 = a0 + PI.toFloat() / dashes
                    moveTo(12f + r * cos(a0), 12f + r * sin(a0))
                    arcTo(
                        r,
                        r,
                        0f,
                        isMoreThanHalf = false,
                        isPositiveArc = true,
                        x1 = 12f + r * cos(a1),
                        y1 = 12f + r * sin(a1),
                    )
                }
            }
        }
    }

    /** `rectangle.dashed`: a geofence's bounding box. */
    val RectangleDashed: ImageVector by lazy {
        icon("ios.rectangleDashed") {
            stroked(width = 1.6f) {
                dashed(3.2f, 5.6f, 20.8f, 5.6f)
                dashed(20.8f, 5.6f, 20.8f, 18.4f)
                dashed(20.8f, 18.4f, 3.2f, 18.4f)
                dashed(3.2f, 18.4f, 3.2f, 5.6f)
            }
        }
    }

    /** `x.circle`: Cancel in the waypoint editor. */
    val XmarkCircle: ImageVector by lazy {
        icon("ios.xmarkCircle") {
            stroked(width = 1.6f) {
                circle(12f, 12f, 9.2f)
                moveTo(9f, 9f)
                lineTo(15f, 15f)
                moveTo(15f, 9f)
                lineTo(9f, 15f)
            }
        }
    }

    /** `safari`: a link that opens in the browser (Device Links). */
    val Safari: ImageVector by lazy {
        icon("ios.safari") {
            stroked(width = 1.6f) { circle(12f, 12f, 9.2f) }
            filled {
                moveTo(15.9f, 8.1f)
                lineTo(13.4f, 13.4f)
                lineTo(8.1f, 15.9f)
                lineTo(10.6f, 10.6f)
                close()
            }
        }
    }

    /** A straight dashed stroke from ([x0], [y0]) to ([x1], [y1]), its dashes evenly spread with one at each end. */
    private fun PathBuilder.dashed(x0: Float, y0: Float, x1: Float, y1: Float, dash: Float = 2.2f) {
        val length = kotlin.math.hypot(x1 - x0, y1 - y0)
        val count = (length / (dash * 2)).toInt().coerceAtLeast(1)
        val step = (length - dash) / count
        for (i in 0..count) {
            val t0 = i * step / length
            val t1 = (i * step + dash) / length
            moveTo(x0 + (x1 - x0) * t0, y0 + (y1 - y0) * t0)
            lineTo(x0 + (x1 - x0) * t1, y0 + (y1 - y0) * t1)
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
