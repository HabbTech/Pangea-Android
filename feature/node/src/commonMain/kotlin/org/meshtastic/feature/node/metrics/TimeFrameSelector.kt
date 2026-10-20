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
package org.meshtastic.feature.node.metrics

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosSegmentedControl
import org.meshtastic.feature.node.model.TimeFrame

@Suppress("LambdaParameterEventTrailing")
@Composable
fun TimeFrameSelector(
    selectedTimeFrame: TimeFrame,
    availableTimeFrames: List<TimeFrame>,
    onTimeFrameSelected: (TimeFrame) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (availableTimeFrames.size <= 1) return

    // iOS's segmented control, as the iPhone app's metric logs pick a time range.
    val labels = availableTimeFrames.map { stringResource(it.strRes) }
    IosSegmentedControl(
        options = labels,
        selectedIndex = availableTimeFrames.indexOf(selectedTimeFrame).coerceAtLeast(0),
        onSelect = { onTimeFrameSelected(availableTimeFrames[it]) },
        modifier = modifier.height(32.dp),
        textStyle = Ios.type.subheadline,
    )
}
