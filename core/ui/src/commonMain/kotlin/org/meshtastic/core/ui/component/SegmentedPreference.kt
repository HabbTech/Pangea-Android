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
package org.meshtastic.core.ui.component

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.schemaLabelRes
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosSegmentedControl

/**
 * A setting with a few choices, as the iPhone app's forms draw one: a segmented control across the whole row, one
 * segment per value of [selectedItem]'s enum, labelled as the schema names it (or by [itemLabel]).
 */
@Composable
fun <T : Enum<T>> SegmentedPreference(
    selectedItem: T,
    enabled: Boolean,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    itemLabel: @Composable ((T) -> String)? = null,
) {
    val entries =
        remember(selectedItem) {
            enumEntriesOf(selectedItem).filter {
                it.name != "UNRECOGNIZED" && (it == selectedItem || !it.isDeprecatedEnumEntry())
            }
        }
    val labels =
        entries.map { entry ->
            itemLabel?.invoke(entry) ?: entry.schemaLabelRes()?.let { stringResource(it) } ?: entry.name
        }
    IosSegmentedControl(
        options = labels,
        selectedIndex = entries.indexOf(selectedItem),
        onSelect = { onSelect(entries[it]) },
        modifier = modifier.padding(horizontal = 16.dp, vertical = 13.dp).height(32.dp),
        textStyle = Ios.type.subheadline,
        enabled = enabled,
    )
}
