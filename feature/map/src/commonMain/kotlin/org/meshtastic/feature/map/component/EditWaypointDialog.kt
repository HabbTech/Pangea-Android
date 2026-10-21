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
package org.meshtastic.feature.map.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.DateFormatter
import org.meshtastic.core.common.util.MeasurementSystem
import org.meshtastic.core.common.util.systemTimeZone
import org.meshtastic.core.model.geofence.GeofenceRadiusPresets
import org.meshtastic.core.model.isLocked
import org.meshtastic.core.model.util.PUSHPIN_CODE_POINT
import org.meshtastic.core.model.util.firstCodePointOrNull
import org.meshtastic.core.model.util.toCodePointString
import org.meshtastic.core.model.util.toDistanceString
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.date
import org.meshtastic.core.resources.delete
import org.meshtastic.core.resources.description
import org.meshtastic.core.resources.expires
import org.meshtastic.core.resources.geofence
import org.meshtastic.core.resources.geofence_off
import org.meshtastic.core.resources.geofence_radius
import org.meshtastic.core.resources.locked
import org.meshtastic.core.resources.name
import org.meshtastic.core.resources.pangea_waypoint_create
import org.meshtastic.core.resources.pangea_waypoint_edit_box
import org.meshtastic.core.resources.pangea_waypoint_editing
import org.meshtastic.core.resources.pangea_waypoint_expire
import org.meshtastic.core.resources.pangea_waypoint_favorites_only
import org.meshtastic.core.resources.pangea_waypoint_icon
import org.meshtastic.core.resources.pangea_waypoint_notify_enter
import org.meshtastic.core.resources.pangea_waypoint_notify_exit
import org.meshtastic.core.resources.pangea_waypoint_options
import org.meshtastic.core.resources.pangea_waypoint_radius
import org.meshtastic.core.resources.pangea_waypoint_remove_box
import org.meshtastic.core.resources.pangea_waypoint_set_box
import org.meshtastic.core.resources.save
import org.meshtastic.core.resources.send
import org.meshtastic.core.resources.time
import org.meshtastic.core.ui.emoji.EmojiPickerDialog
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosAccessory
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.ios.IosSheet
import org.meshtastic.proto.Waypoint
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * Shared waypoint editor used by both the google and fdroid map flavors (DRY — replaces the two drifted per-flavor
 * copies). Map-engine-specific concerns stay outside: drawing the box overlay and the drag-to-define gesture are
 * triggered via [onBeginBoxAuthoring], which hands the current draft back to the flavor's map so the user can define
 * the bounding box there; the flavor re-opens this dialog with the drawn box applied.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Suppress("LongMethod", "CyclomaticComplexMethod", "MagicNumber")
@Composable
fun EditWaypointDialog(
    waypoint: Waypoint,
    displayUnits: MeasurementSystem,
    myNodeNum: Int?,
    onSend: (Waypoint) -> Unit,
    onDelete: (Waypoint) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onBeginBoxAuthoring: (Waypoint) -> Unit = {},
) {
    var waypointInput by remember { mutableStateOf(waypoint) }
    val title = if (waypoint.id == 0) Res.string.pangea_waypoint_create else Res.string.pangea_waypoint_editing
    val defaultEmoji = PUSHPIN_CODE_POINT
    val currentEmojiCodepoint = if (waypointInput.icon == 0) defaultEmoji else waypointInput.icon
    var showEmojiPickerView by remember { mutableStateOf(false) }

    val tz = systemTimeZone

    var selectedDateString by remember { mutableStateOf("") }
    var selectedTimeString by remember { mutableStateOf("") }
    var isExpiryEnabled by remember { mutableStateOf(waypointInput.expire.isExpirySet()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    LaunchedEffect(waypointInput.expire, isExpiryEnabled) {
        val expireValue = waypointInput.expire
        if (isExpiryEnabled) {
            val instant = expireValue.expiryInstantOrDefault()
            selectedDateString = DateFormatter.formatDate(instant.toEpochMilliseconds())
            selectedTimeString = DateFormatter.formatTime(instant.toEpochMilliseconds())
            if (!expireValue.isExpirySet()) {
                waypointInput =
                    waypointInput.newBuilder().also { wb -> wb.expire = instant.epochSeconds.toInt() }.build()
            }
        } else {
            selectedDateString = ""
            selectedTimeString = ""
        }
    }

    if (!showEmojiPickerView) {
        // iOS's waypoint form: a full-height sheet titled Create or Editing Waypoint, the Waypoint Options and
        // Geofence sections, and Send, Cancel and (for a saved waypoint) Delete as capsules beneath.
        IosSheet(
            title = stringResource(title),
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            grouped = true,
            grabber = true,
        ) {
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                IosSection(header = stringResource(Res.string.pangea_waypoint_options)) {
                    WaypointFieldRow(
                        label = stringResource(Res.string.name),
                        value = waypointInput.name,
                        onValueChange = {
                            waypointInput = waypointInput.newBuilder().also { wb -> wb.name = it.take(29) }.build()
                        },
                    )
                    WaypointFieldRow(
                        label = stringResource(Res.string.description),
                        value = waypointInput.description,
                        multiline = true,
                        onValueChange = {
                            waypointInput =
                                waypointInput.newBuilder().also { wb -> wb.description = it.take(99) }.build()
                        },
                    )
                    WaypointIconRow(
                        label = stringResource(Res.string.pangea_waypoint_icon),
                        emoji = currentEmojiCodepoint.toCodePointString(),
                        onPickEmoji = { showEmojiPickerView = true },
                    )
                    IosRow(
                        title = stringResource(Res.string.expires),
                        icon = IosIcons.Clock,
                        accessory =
                        IosAccessory.Switch(
                            checked = isExpiryEnabled,
                            onCheckedChange = { checked ->
                                isExpiryEnabled = checked
                                if (checked) {
                                    if (!waypointInput.expire.isExpirySet()) {
                                        val default = waypointInput.expire.expiryInstantOrDefault()
                                        waypointInput =
                                            waypointInput
                                                .newBuilder()
                                                .also { wb -> wb.expire = default.epochSeconds.toInt() }
                                                .build()
                                    }
                                } else {
                                    waypointInput =
                                        waypointInput.newBuilder().also { wb -> wb.expire = Int.MAX_VALUE }.build()
                                }
                            },
                        ),
                    )
                    if (isExpiryEnabled) {
                        WaypointExpireRow(
                            label = stringResource(Res.string.pangea_waypoint_expire),
                            date = selectedDateString,
                            time = selectedTimeString,
                            onPickDate = { showDatePicker = true },
                            onPickTime = { showTimePicker = true },
                        )
                    }
                    IosRow(
                        title = stringResource(Res.string.locked),
                        icon = IosIcons.LockOutline,
                        accessory =
                        IosAccessory.Switch(
                            checked = waypointInput.isLocked,
                            // Lock to our own node so we (and only we) can later edit or remove it; unlock clears
                            // the
                            // owner. locked_to must be a real node number - a placeholder locks the waypoint to a
                            // phantom node and blocks even the creator (see #6343). Disabled until our node number
                            // is
                            // known.
                            enabled = myNodeNum != null,
                            onCheckedChange = { locked ->
                                waypointInput =
                                    waypointInput
                                        .newBuilder()
                                        .also { wb -> wb.locked_to = if (locked) myNodeNum ?: 0 else 0 }
                                        .build()
                            },
                        ),
                    )
                }
                GeofenceSection(
                    waypoint = waypointInput,
                    displayUnits = displayUnits,
                    onWaypointChange = { waypointInput = it },
                    onBeginBoxAuthoring = { onBeginBoxAuthoring(waypointInput) },
                )
                Spacer(Modifier.height(16.dp))
            }
            WaypointFormActions(
                sendLabel = stringResource(Res.string.send),
                sendEnabled = waypointInput.name.isNotBlank(),
                onSend = { onSend(waypointInput) },
                cancelLabel = stringResource(Res.string.cancel),
                onCancel = onDismissRequest,
                deleteLabel = if (waypoint.id != 0) stringResource(Res.string.delete) else null,
                onDelete = { onDelete(waypointInput) },
            )
        }
        if (isExpiryEnabled) {
            val ldt = waypointInput.expire.expiryInstantOrDefault().toLocalDateTime(tz)
            // The pickers are Material 3's own, so this editor builds for desktop as well as Android.
            if (showDatePicker) {
                ExpiryDatePickerDialog(
                    initial = ldt,
                    onDismiss = { showDatePicker = false },
                    onPick = { picked ->
                        val newLdt =
                            picked.atTime(
                                hour = ldt.hour,
                                minute = ldt.minute,
                                second = ldt.second,
                                nanosecond = ldt.nanosecond,
                            )
                        waypointInput =
                            waypointInput
                                .newBuilder()
                                .also { wb -> wb.expire = newLdt.toInstant(tz).epochSeconds.toInt() }
                                .build()
                        showDatePicker = false
                    },
                )
            }

            if (showTimePicker) {
                ExpiryTimePickerDialog(
                    initial = ldt,
                    onDismiss = { showTimePicker = false },
                    onPick = { hour, minute ->
                        val newLdt =
                            LocalDate(year = ldt.year, month = ldt.month, day = ldt.day)
                                .atTime(hour = hour, minute = minute, second = ldt.second, nanosecond = ldt.nanosecond)
                        waypointInput =
                            waypointInput
                                .newBuilder()
                                .also { wb -> wb.expire = newLdt.toInstant(tz).epochSeconds.toInt() }
                                .build()
                        showTimePicker = false
                    },
                )
            }
        }
    } else {
        EmojiPickerDialog(onDismiss = { showEmojiPickerView = false }) { selectedEmoji ->
            showEmojiPickerView = false
            selectedEmoji.firstCodePointOrNull()?.let {
                waypointInput = waypointInput.newBuilder().also { wb -> wb.icon = it }.build()
            }
        }
    }
}

/**
 * Geofence authoring controls. Radius presets are locale-aware (wire value is always metres); the enter/exit toggles
 * appear only once a circle or box exists, and the favorites-only toggle only once enter or exit is on.
 */
@Composable
private fun GeofenceSection(
    waypoint: Waypoint,
    displayUnits: MeasurementSystem,
    onWaypointChange: (Waypoint) -> Unit,
    onBeginBoxAuthoring: () -> Unit,
) {
    val presets = remember(displayUnits) { GeofenceRadiusPresets.forUnits(displayUnits) }
    val hasBox = waypoint.bounding_box != null
    val hasRegion = waypoint.geofence_radius > 0 || hasBox
    // The closest preset reads as chosen, so a radius authored on another client (an imperial or non-preset value) is
    // still shown rather than appearing as no choice and being silently clobbered on edit.
    val selectedRadius = GeofenceRadiusPresets.nearest(waypoint.geofence_radius, displayUnits)
    val off = stringResource(Res.string.geofence_off)
    val c = Ios.colors

    IosSection(header = stringResource(Res.string.geofence)) {
        WaypointRadiusRow(
            label = stringResource(Res.string.pangea_waypoint_radius),
            choices =
            presets.map { meters -> meters to if (meters == 0) off else meters.toDistanceString(displayUnits) },
            selected = selectedRadius,
            onSelect = { meters ->
                onWaypointChange(
                    waypoint
                        .newBuilder()
                        .also { wb -> wb.geofence_radius = meters }
                        .build()
                        .normalizeGeofenceNotifications(),
                )
            },
        )
        IosRow(
            title =
            stringResource(if (hasBox) Res.string.pangea_waypoint_edit_box else Res.string.pangea_waypoint_set_box),
            icon = IosIcons.RectangleDashed,
            titleColor = c.tint,
            onClick = onBeginBoxAuthoring,
        )
        if (hasBox) {
            IosRow(
                title = stringResource(Res.string.pangea_waypoint_remove_box),
                icon = IosIcons.Trash,
                iconTint = c.systemRed,
                titleColor = c.systemRed,
                onClick = {
                    onWaypointChange(
                        waypoint
                            .newBuilder()
                            .also { wb -> wb.bounding_box = null }
                            .build()
                            .normalizeGeofenceNotifications(),
                    )
                },
            )
        }
        if (hasRegion) GeofenceNotificationRows(waypoint = waypoint, onWaypointChange = onWaypointChange)
    }
}

/** Enter/exit switches, and favourites-only once either is on. */
@Composable
private fun GeofenceNotificationRows(waypoint: Waypoint, onWaypointChange: (Waypoint) -> Unit) {
    GeofenceToggleRow(
        label = stringResource(Res.string.pangea_waypoint_notify_enter),
        icon = IosIcons.Bell,
        checked = waypoint.notify_on_enter,
        onCheckedChange = {
            onWaypointChange(
                waypoint.newBuilder().also { wb -> wb.notify_on_enter = it }.build().normalizeGeofenceNotifications(),
            )
        },
    )
    GeofenceToggleRow(
        label = stringResource(Res.string.pangea_waypoint_notify_exit),
        icon = IosIcons.BellSlash,
        checked = waypoint.notify_on_exit,
        onCheckedChange = {
            onWaypointChange(
                waypoint.newBuilder().also { wb -> wb.notify_on_exit = it }.build().normalizeGeofenceNotifications(),
            )
        },
    )
    if (waypoint.notify_on_enter || waypoint.notify_on_exit) {
        GeofenceToggleRow(
            label = stringResource(Res.string.pangea_waypoint_favorites_only),
            icon = IosIcons.Star,
            checked = waypoint.notify_favorites_only,
            onCheckedChange = {
                onWaypointChange(waypoint.newBuilder().also { wb -> wb.notify_favorites_only = it }.build())
            },
        )
    }
}

/** A switch row with its symbol; the whole row is the toggle, so its label names the switch to a screen reader. */
@Composable
private fun GeofenceToggleRow(label: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    IosRow(
        title = label,
        icon = icon,
        accessory = IosAccessory.Switch(checked = checked, onCheckedChange = onCheckedChange),
    )
}

/**
 * Clears geofence notification flags the UI can no longer expose, so stale values don't silently reappear when a region
 * is added again: drop all three when no region remains, and drop favorites-only when neither enter nor exit is on.
 */
private fun Waypoint.normalizeGeofenceNotifications(): Waypoint = when {
    geofence_radius <= 0 && bounding_box == null ->
        this.newBuilder()
            .also { wb ->
                wb.notify_on_enter = false
                wb.notify_on_exit = false
                wb.notify_favorites_only = false
            }
            .build()

    !notify_on_enter && !notify_on_exit -> this.newBuilder().also { wb -> wb.notify_favorites_only = false }.build()

    else -> this
}

/**
 * The expiry date, picked with Material 3's own dialog.
 *
 * The picker works in UTC-midnight milliseconds, so the value is converted through [TimeZone.UTC] on the way in and
 * out. Reading it back in the local zone would land on the previous day for anyone west of Greenwich.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpiryDatePickerDialog(initial: LocalDateTime, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state =
        rememberDatePickerState(
            initialSelectedDateMillis = initial.date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
        )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onPick(Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date)
                    } ?: onDismiss()
                },
            ) {
                Text(stringResource(Res.string.save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    ) {
        DatePicker(state = state)
    }
}

/**
 * The expiry time of day.
 *
 * `is24Hour` is left to [rememberTimePickerState], whose default resolves the platform's own clock preference — which
 * is what `android.text.format.DateFormat.is24HourFormat` used to supply here, and what made this Android-only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpiryTimePickerDialog(initial: LocalDateTime, onPick: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute)
    TimePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onPick(state.hour, state.minute) }) { Text(stringResource(Res.string.save)) }
        },
        title = { Text(stringResource(Res.string.time)) },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    ) {
        TimePicker(state = state)
    }
}

/**
 * Whether this `expire` value names a real expiry.
 *
 * The wire field carries two sentinels rather than being nullable: `0` for a waypoint that never had one set, and
 * `Int.MAX_VALUE` for one explicitly marked as never expiring. Neither is a timestamp, and treating either as one puts
 * the expiry in 1970 or in 2038.
 */
internal fun Int.isExpirySet(): Boolean = this != 0 && this != Int.MAX_VALUE

/**
 * This `expire` value as an instant, or the default the editor offers when there is none.
 *
 * Eight hours out, which is what the expiry switch and both pickers seed themselves with — they used to each spell out
 * the same fallback, so it could drift in one of the three.
 */
internal fun Int.expiryInstantOrDefault(): Instant =
    if (isExpirySet()) Instant.fromEpochSeconds(toLong()) else Clock.System.now() + DEFAULT_EXPIRY

/** How far ahead a newly enabled expiry is set. */
private val DEFAULT_EXPIRY = 8.hours
