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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.schemaLabelRes
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.iosPressable
import org.meshtastic.core.ui.theme.AppTheme
import kotlin.jvm.JvmName

@Composable
fun <T : Enum<T>> DropDownPreference(
    title: String,
    enabled: Boolean,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    itemIcon: @Composable ((T) -> ImageVector)? = null,
    itemColor: @Composable ((T) -> Color)? = null,
    itemLabel: @Composable ((T) -> String)? = null,
) {
    // A deprecated value the radio currently holds stays on the list: dropping it leaves the field showing nothing and
    // writes a different value on the next save.
    val enumConstants =
        remember(selectedItem) {
            enumEntriesOf(selectedItem).filter {
                it.name != "UNRECOGNIZED" && (it == selectedItem || !it.isDeprecatedEnumEntry())
            }
        }

    val items =
        enumConstants.map {
            val label = itemLabel?.invoke(it) ?: it.schemaLabel()
            val icon = itemIcon?.invoke(it)
            val color = itemColor?.invoke(it)
            DropDownItem(it, label, icon, color)
        }

    DropDownPreference(
        title = title,
        enabled = enabled,
        items = items,
        selectedItem = selectedItem,
        onItemSelected = onItemSelected,
        modifier = modifier,
        summary = summary,
    )
}

data class DropDownItem<T>(
    val value: T,
    val label: String,
    val icon: ImageVector? = null,
    val color: Color? = null,
    /** When false, the item is shown greyed-out and cannot be selected. */
    val enabled: Boolean = true,
    /** Optional stable semantics tag for automated tests. */
    val testTag: String? = null,
)

@JvmName("DropDownPreferencePairs")
@Composable
fun <T> DropDownPreference(
    title: String,
    enabled: Boolean,
    items: List<Pair<T, String>>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    DropDownPreference(
        title = title,
        enabled = enabled,
        items = items.map { DropDownItem(it.first, it.second) },
        selectedItem = selectedItem,
        onItemSelected = onItemSelected,
        modifier = modifier,
        summary = summary,
    )
}

@Composable
@Suppress("LongMethod")
fun <T> DropDownPreference(
    title: String,
    enabled: Boolean,
    items: List<DropDownItem<T>>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val c = Ios.colors
    val currentItem = items.firstOrNull { it.value == selectedItem }

    // iOS's menu picker: the value in grey with up/down chevrons at the end of the row; tapping opens the choices.
    PreferenceRow(
        title = title,
        summary = summary,
        enabled = enabled,
        modifier = modifier.iosPressable(enabled = enabled) { expanded = true },
        trailing = {
            Box {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.widthIn(max = 220.dp)) {
                    if (currentItem?.icon != null) {
                        Icon(
                            imageVector = currentItem.icon,
                            contentDescription = null,
                            tint = c.secondaryLabel,
                            modifier = Modifier.padding(end = 6.dp).size(20.dp),
                        )
                    } else if (currentItem?.color != null) {
                        Icon(
                            painter = ColorPainter(currentItem.color),
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp).size(16.dp).clip(CircleShape),
                            tint = Color.Unspecified,
                        )
                    }
                    Text(
                        text = currentItem?.label.orEmpty(),
                        style = Ios.type.body,
                        color = c.secondaryLabel,
                        textAlign = TextAlign.End,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Icon(
                        IosIcons.ChevronUpDown,
                        contentDescription = null,
                        tint = c.secondaryLabel,
                        modifier = Modifier.padding(start = 4.dp).size(16.dp),
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    items.forEach { selectionOption ->
                        DropdownMenuItem(
                            modifier = selectionOption.testTag?.let { Modifier.testTag(it) } ?: Modifier,
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (selectionOption.icon != null) {
                                        Icon(
                                            imageVector = selectionOption.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    } else if (selectionOption.color != null) {
                                        Icon(
                                            painter = ColorPainter(selectionOption.color),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = Color.Unspecified,
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }
                                    Text(selectionOption.label)
                                }
                            },
                            trailingIcon =
                            if (selectionOption.value == selectedItem) {
                                { Icon(IosIcons.Checkmark, contentDescription = null, tint = c.tint) }
                            } else {
                                null
                            },
                            enabled = selectionOption.enabled,
                            onClick = {
                                onItemSelected(selectionOption.value)
                                expanded = false
                            },
                        )
                    }
                }
            }
        },
    )
}

internal expect fun <T : Enum<T>> enumEntriesOf(selectedItem: T): List<T>

internal expect fun Enum<*>.isDeprecatedEnumEntry(): Boolean

/** The label the schema gives this value, falling back to the constant's name where the schema does not name it. */
@Composable private fun Enum<*>.schemaLabel(): String = schemaLabelRes()?.let { stringResource(it) } ?: name

@Preview(showBackground = true)
@Composable
fun DropDownPreferencePreview() {
    AppTheme {
        DropDownPreference(
            title = "Settings",
            summary = "Lorem ipsum dolor sit amet",
            enabled = true,
            items = listOf(DropDownItem("TEST1", "text1"), DropDownItem("TEST2", "text2")),
            selectedItem = "TEST2",
            onItemSelected = {},
        )
    }
}
