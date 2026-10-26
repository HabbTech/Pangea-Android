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
package org.meshtastic.feature.settings.pangea

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.MeasurementSystem
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.model.pangea.GpsFix
import org.meshtastic.core.model.pangea.RouteActivity
import org.meshtastic.core.model.pangea.RouteRecording
import org.meshtastic.core.model.pangea.TimeOfDay
import org.meshtastic.core.model.util.toDistanceString
import org.meshtastic.core.model.util.toElevationString
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.distance
import org.meshtastic.core.resources.grant_permission
import org.meshtastic.core.resources.pangea_activity
import org.meshtastic.core.resources.pangea_activity_biking
import org.meshtastic.core.resources.pangea_activity_driving
import org.meshtastic.core.resources.pangea_activity_hiking
import org.meshtastic.core.resources.pangea_activity_overlanding
import org.meshtastic.core.resources.pangea_activity_skiing
import org.meshtastic.core.resources.pangea_activity_walking
import org.meshtastic.core.resources.pangea_elev_gain
import org.meshtastic.core.resources.pangea_gps_waiting
import org.meshtastic.core.resources.pangea_recorded_route_name
import org.meshtastic.core.resources.pangea_recording_paused
import org.meshtastic.core.resources.pangea_recording_route
import org.meshtastic.core.resources.pangea_route_finish
import org.meshtastic.core.resources.pangea_route_location_needed
import org.meshtastic.core.resources.pangea_route_pause
import org.meshtastic.core.resources.pangea_route_resume
import org.meshtastic.core.resources.pangea_route_start
import org.meshtastic.core.resources.pangea_time_afternoon
import org.meshtastic.core.resources.pangea_time_evening
import org.meshtastic.core.resources.pangea_time_midday
import org.meshtastic.core.resources.pangea_time_morning
import org.meshtastic.core.resources.pangea_time_nighttime
import org.meshtastic.core.resources.time
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosButton
import org.meshtastic.core.ui.ios.IosButtonStyle
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosMenu
import org.meshtastic.core.ui.ios.IosMenuItem
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.ios.IosSheet
import org.meshtastic.core.ui.ios.iosPressable
import kotlin.time.Clock

/**
 * The Route Recorder's details sheet, as iOS lays it out: what is happening (recording with its point count, or
 * paused), Time / Distance / Elev. Gain, the phone's GPS, the Activity to name a new recording by, and the buttons for
 * the next step. Starting needs location access, which is asked for here.
 */
@Suppress("LongParameterList")
@Composable
internal fun RecorderSheet(
    recording: RouteRecording?,
    fix: GpsFix?,
    units: MeasurementSystem,
    locationGranted: Boolean,
    onGrantLocation: () -> Unit,
    activity: RouteActivity,
    onActivityChange: (RouteActivity) -> Unit,
    onStart: (name: String) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val routeName = recordingName(activity)
    IosSheet(title = "", onDismissRequest = onDismissRequest, heightFraction = SHEET_HEIGHT, header = {}) {
        if (recording != null) {
            RecordingStatus(recording)
            Divider()
            RecordingStats(recording, units)
        }
        Divider()
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            IosSection {
                when {
                    !locationGranted -> {
                        IosRow(
                            title = stringResource(Res.string.pangea_route_location_needed),
                            titleColor = Ios.colors.secondaryLabel,
                        )
                        IosRow(
                            title = stringResource(Res.string.grant_permission),
                            titleColor = Ios.colors.tint,
                            onClick = onGrantLocation,
                        )
                    }

                    fix == null ->
                        IosRow(
                            title = stringResource(Res.string.pangea_gps_waiting),
                            titleColor = Ios.colors.secondaryLabel,
                        )

                    else -> GpsFixRows(fix)
                }
            }
        }
        if (recording == null) ActivityPicker(activity, onActivityChange)
        RecorderButtons(
            recording = recording,
            canStart = locationGranted,
            onStart = { onStart(routeName) },
            onPause = onPause,
            onResume = onResume,
            onFinish = onFinish,
        )
    }
}

@Composable
private fun RecordingStatus(recording: RouteRecording) {
    val red = RecorderRed
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (recording.paused) {
            Icon(IosIcons.PlayPause, contentDescription = null, tint = red, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(Res.string.pangea_recording_paused),
                style = Ios.type.title1,
                color = Ios.colors.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Icon(IosIcons.RecordCircleFill, contentDescription = null, tint = red, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(Res.string.pangea_recording_route),
                style = Ios.type.title1,
                color = Ios.colors.label,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(IosIcons.MapPinEllipse, contentDescription = null, tint = red, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(4.dp))
            Text(recording.route.points.size.toString(), style = Ios.type.title2, color = red)
        }
    }
}

/** Time since the recording began (pauses included, as iOS's timer counts), distance, and elevation gained. */
@Composable
private fun RecordingStats(recording: RouteRecording, units: MeasurementSystem) {
    val now by
        produceState(nowMillis) {
            while (true) {
                delay(TICK_MILLIS)
                value = nowMillis
            }
        }
    Row(
        Modifier.fillMaxWidth().height(STATS_HEIGHT),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Stat(elapsed(now - recording.route.createdMillis), Res.string.time)
        StatDivider()
        Stat(recording.distanceMeters.toInt().toDistanceString(units), Res.string.distance)
        StatDivider()
        Stat(recording.elevationGainMeters.toInt().toElevationString(units), Res.string.pangea_elev_gain)
    }
}

@Composable
private fun Stat(value: String, label: StringResource) {
    Column(Modifier.padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = Ios.type.title1, color = Ios.colors.label, maxLines = 1)
        Text(stringResource(label), style = Ios.type.callout, color = Ios.colors.label, maxLines = 1)
    }
}

@Composable
private fun StatDivider() {
    Box(Modifier.fillMaxHeight().padding(vertical = 8.dp).width(0.5.dp).background(Ios.colors.separator))
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(Ios.colors.separator))
}

/** iOS's Activity menu: the walker, "Activity", and the choice in the tint, opening a menu of the six. */
@Composable
private fun ActivityPicker(activity: RouteActivity, onActivityChange: (RouteActivity) -> Unit) {
    val c = Ios.colors
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(IosIcons.FigureWalk, contentDescription = null, tint = c.tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(Res.string.pangea_activity), style = Ios.type.body, color = c.label)
        Spacer(Modifier.width(10.dp))
        Box {
            Row(
                Modifier.iosPressable { open = true }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(activity.label), style = Ios.type.body, color = c.tint)
                Spacer(Modifier.width(4.dp))
                Icon(IosIcons.ChevronUpDown, contentDescription = null, tint = c.tint, modifier = Modifier.size(14.dp))
            }
            IosMenu(expanded = open, onDismissRequest = { open = false }) {
                RouteActivity.entries.forEach { option ->
                    IosMenuItem(
                        text = stringResource(option.label),
                        checkable = true,
                        checked = option == activity,
                        onClick = {
                            onActivityChange(option)
                            open = false
                        },
                    )
                }
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun RecorderButtons(
    recording: RouteRecording?,
    canStart: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
    ) {
        when {
            recording == null ->
                RecorderButton(Res.string.pangea_route_start, IosIcons.Play, onStart, enabled = canStart)

            recording.paused -> RecorderButton(Res.string.pangea_route_resume, IosIcons.PlayPause, onResume)

            else -> RecorderButton(Res.string.pangea_route_pause, IosIcons.Pause, onPause)
        }
        if (recording != null) RecorderButton(Res.string.pangea_route_finish, IosIcons.Flag, onFinish)
    }
}

@Composable
private fun RecorderButton(label: StringResource, icon: ImageVector, onClick: () -> Unit, enabled: Boolean = true) {
    IosButton(
        text = stringResource(label),
        onClick = onClick,
        style = IosButtonStyle.Tinted,
        enabled = enabled,
        large = true,
        fullWidth = false,
        icon = icon,
    )
}

/** iOS's name for a new recording: the part of the day and the activity ("Morning hiking"). */
@Composable
private fun recordingName(activity: RouteActivity): String {
    val hour = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
    return stringResource(
        Res.string.pangea_recorded_route_name,
        stringResource(TimeOfDay.of(hour).label),
        stringResource(activity.label).lowercase(),
    )
}

/** Elapsed time as SwiftUI's timer shows it: 0:05, 12:34, 1:02:03. */
internal fun elapsed(millis: Long): String {
    val seconds = (millis / MILLIS_PER_SECOND).coerceAtLeast(0)
    val h = seconds / SECONDS_PER_HOUR
    val m = (seconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val s = (seconds % SECONDS_PER_MINUTE).toString().padStart(2, '0')
    return if (h > 0) "$h:${m.toString().padStart(2, '0')}:$s" else "$m:$s"
}

private val RouteActivity.label: StringResource
    get() =
        when (this) {
            RouteActivity.WALKING -> Res.string.pangea_activity_walking
            RouteActivity.HIKING -> Res.string.pangea_activity_hiking
            RouteActivity.BIKING -> Res.string.pangea_activity_biking
            RouteActivity.DRIVING -> Res.string.pangea_activity_driving
            RouteActivity.OVERLANDING -> Res.string.pangea_activity_overlanding
            RouteActivity.SKIING -> Res.string.pangea_activity_skiing
        }

private val TimeOfDay.label: StringResource
    get() =
        when (this) {
            TimeOfDay.MORNING -> Res.string.pangea_time_morning
            TimeOfDay.MIDDAY -> Res.string.pangea_time_midday
            TimeOfDay.AFTERNOON -> Res.string.pangea_time_afternoon
            TimeOfDay.EVENING -> Res.string.pangea_time_evening
            TimeOfDay.NIGHTTIME -> Res.string.pangea_time_nighttime
        }

/** iOS opens the sheet at its 45% detent. */
private const val SHEET_HEIGHT = 0.45f
private val STATS_HEIGHT = 90.dp
private const val TICK_MILLIS = 1_000L
private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L
