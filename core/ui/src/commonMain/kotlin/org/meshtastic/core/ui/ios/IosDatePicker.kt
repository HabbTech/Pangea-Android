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
package org.meshtastic.core.ui.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.meshtastic.core.common.util.DateFormatter

/**
 * iOS's graphical date picker, as a compact `DatePicker` opens it: a card with the month and year over arrows to step
 * through months, the weekday initials, and the month's days, the chosen one in a tinted circle and today in the tint.
 * Tapping a day picks it and closes the card; tapping outside closes it unchanged.
 */
@Composable
fun IosCalendarDialog(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit, today: LocalDate) {
    var month by remember { mutableStateOf(LocalDate(initial.year, initial.month, 1)) }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.width(CARD_WIDTH)
                .clip(RoundedCornerShape(CARD_CORNER))
                .background(Ios.colors.elevated().groupedRow)
                .padding(horizontal = 12.dp, vertical = 14.dp),
        ) {
            MonthHeader(month = month, onStep = { month = month.plus(it, DateTimeUnit.MONTH) })
            Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp)) {
                DateFormatter.shortWeekdayNames().forEach { name ->
                    Text(
                        text = name.uppercase(),
                        style = Ios.type.caption1.copy(fontWeight = FontWeight.SemiBold),
                        color = Ios.colors.tertiaryLabel,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            MonthGrid(month = month, selected = initial, today = today, onPick = onPick)
        }
    }
}

@Composable
private fun MonthHeader(month: LocalDate, onStep: (Int) -> Unit) {
    val millis = month.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    Row(Modifier.fillMaxWidth().padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = DateFormatter.formatMonthYear(millis),
            style = Ios.type.headline,
            color = Ios.colors.label,
            modifier = Modifier.weight(1f),
        )
        Icon(
            IosIcons.ChevronLeft,
            contentDescription = null,
            tint = Ios.colors.tint,
            modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onStep(-1) }.padding(8.dp),
        )
        Icon(
            IosIcons.ChevronRight,
            contentDescription = null,
            tint = Ios.colors.tint,
            modifier = Modifier.size(36.dp).clip(CircleShape).clickable { onStep(1) }.padding(8.dp),
        )
    }
}

/** The month's days in weeks, Sunday first, blank cells before the 1st. */
@Composable
private fun MonthGrid(month: LocalDate, selected: LocalDate, today: LocalDate, onPick: (LocalDate) -> Unit) {
    val lead = month.dayOfWeek.isoDayNumber % DAYS_PER_WEEK
    val days = month.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).day
    val cells = lead + days
    val weeks = (cells + DAYS_PER_WEEK - 1) / DAYS_PER_WEEK
    Column {
        repeat(weeks) { week ->
            Row(Modifier.fillMaxWidth()) {
                repeat(DAYS_PER_WEEK) { col ->
                    val day = week * DAYS_PER_WEEK + col - lead + 1
                    Box(Modifier.weight(1f).height(DAY_SIZE), contentAlignment = Alignment.Center) {
                        if (day in 1..days) {
                            val date = LocalDate(month.year, month.month, day)
                            DayCell(day = day, isSelected = date == selected, isToday = date == today) { onPick(date) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: Int, isSelected: Boolean, isToday: Boolean, onClick: () -> Unit) {
    val c = Ios.colors
    Box(
        Modifier.size(DAY_SIZE - 4.dp)
            .clip(CircleShape)
            .background(if (isSelected) c.tint.copy(alpha = if (c.isDark) 0.3f else 0.15f) else c.groupedRow.copy(0f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.toString(),
            style = Ios.type.title3.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal),
            color = if (isSelected || isToday) c.tint else c.label,
        )
    }
}

/**
 * iOS's wheel time picker, as a compact time `DatePicker` opens it: an hours wheel and a minutes wheel turning past a
 * grey selection band. The time is reported when the card closes (a tap outside it), as iOS's popover does.
 */
@Composable
fun IosTimeWheelDialog(hour: Int, minute: Int, onPick: (hour: Int, minute: Int) -> Unit) {
    var h by remember { mutableIntStateOf(hour) }
    var m by remember { mutableIntStateOf(minute) }
    Dialog(onDismissRequest = { onPick(h, m) }) {
        Box(
            Modifier.width(WHEEL_CARD_WIDTH)
                .clip(RoundedCornerShape(CARD_CORNER))
                .background(Ios.colors.elevated().groupedRow)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.fillMaxWidth()
                    .height(WHEEL_ROW)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Ios.colors.fieldFill),
            )
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Wheel(count = HOURS, initial = hour, onSelect = { h = it }, label = { it.toString().padStart(2, '0') })
                Text(":", style = Ios.type.title3, color = Ios.colors.label)
                Wheel(
                    count = MINUTES,
                    initial = minute,
                    onSelect = { m = it },
                    label = { it.toString().padStart(2, '0') },
                )
            }
        }
    }
}

/** One wheel: the values in a snapping column, the one in the band in the label colour, the rest greyed. */
@Composable
private fun Wheel(count: Int, initial: Int, onSelect: (Int) -> Unit, label: (Int) -> String) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initial)
    // The row in the band: the list's content starts there, past the padding, so it is the first visible item.
    val centred by remember { derivedStateOf { state.firstVisibleItemIndex } }
    val select by rememberUpdatedState(onSelect)
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling -> if (!scrolling) select(state.firstVisibleItemIndex.coerceIn(0, count - 1)) }
    }
    LazyColumn(
        state = state,
        flingBehavior = rememberSnapFlingBehavior(lazyListState = state),
        contentPadding = PaddingValues(vertical = WHEEL_ROW * (WHEEL_VISIBLE / 2)),
        modifier = Modifier.width(WHEEL_WIDTH).height(WHEEL_ROW * WHEEL_VISIBLE),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(count) { i ->
            Box(Modifier.height(WHEEL_ROW).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = label(i),
                    style = Ios.type.title3,
                    color = if (i == centred) Ios.colors.label else Ios.colors.tertiaryLabel,
                )
            }
        }
    }
}

private const val DAYS_PER_WEEK = 7
private const val HOURS = 24
private const val MINUTES = 60
private const val WHEEL_VISIBLE = 5
private val CARD_WIDTH = 340.dp
private val WHEEL_CARD_WIDTH = 220.dp
private val CARD_CORNER = 22.dp
private val DAY_SIZE = 44.dp
private val WHEEL_ROW = 36.dp
private val WHEEL_WIDTH = 64.dp
