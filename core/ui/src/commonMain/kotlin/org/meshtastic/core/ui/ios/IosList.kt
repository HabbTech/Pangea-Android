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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** iOS 26 inset-grouped card corners. */
val IosCardShape = RoundedCornerShape(26.dp)

/** Side margin of an inset-grouped card, and the padding inside it. */
val IosInset = 16.dp

/** Where a row's text starts when it has a leading icon (the separator starts here too). */
private val IconTextStart = 56.dp

/** Marks a section child with where its top separator starts; [IosRow] sets it for you. */
data class IosSeparatorInset(val start: Dp)

/** For custom children of an [IosSection]: draw the separator above this child from [start]. */
fun Modifier.iosRowSeparator(start: Dp = IosInset): Modifier = layoutId(IosSeparatorInset(start))

/**
 * An inset-grouped section (`List` with `.insetGrouped`): an optional grey [header], a white card of [content] rows
 * with hairline separators between them (from each row's text start to the card's inner edge, never above the first
 * row), and an optional [footer] explanation.
 */
@Composable
fun IosSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    content: @Composable () -> Unit,
) {
    val separator = Ios.colors.separator
    // Written during layout and read during draw in the same frame; not snapshot state, so it can't loop.
    val lines = remember { ArrayList<Pair<Float, Float>>() }
    Column(modifier.fillMaxWidth().padding(horizontal = IosInset)) {
        if (header != null) {
            Text(
                header,
                style = Ios.type.headline,
                color = Ios.colors.secondaryLabel,
                modifier = Modifier.padding(start = IosInset, end = IosInset, top = 18.dp, bottom = 8.dp),
            )
        } else {
            Spacer(Modifier.height(20.dp))
        }
        Layout(
            content = content,
            modifier =
            Modifier.fillMaxWidth().clip(IosCardShape).background(Ios.colors.groupedRow).drawWithContent {
                drawContent()
                val hair = 0.5.dp.toPx()
                val end = size.width - IosInset.toPx()
                lines.forEach { (y, start) ->
                    if (end > start) drawRect(separator, Offset(start, y), Size(end - start, hair))
                }
            },
        ) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
            lines.clear()
            var y = 0
            placeables.forEachIndexed { i, p ->
                val inset = measurables[i].layoutId as? IosSeparatorInset
                if (i > 0 && inset != null) lines.add(y.toFloat() to inset.start.toPx())
                y += p.height
            }
            layout(constraints.maxWidth, y) {
                var top = 0
                placeables.forEach {
                    it.placeRelative(0, top)
                    top += it.height
                }
            }
        }
        if (footer != null) {
            Text(
                footer,
                style = Ios.type.footnote,
                color = Ios.colors.secondaryLabel,
                modifier = Modifier.padding(start = IosInset, end = IosInset, top = 8.dp),
            )
        }
    }
}

/** What sits at the trailing edge of an [IosRow]. */
sealed interface IosAccessory {
    data object None : IosAccessory

    /** Pushes to another screen. */
    data object Chevron : IosAccessory

    /** The selected option in a picker list. */
    data object Checkmark : IosAccessory

    data class Switch(val checked: Boolean, val onCheckedChange: (Boolean) -> Unit, val enabled: Boolean = true) :
        IosAccessory

    data class Custom(val content: @Composable () -> Unit) : IosAccessory
}

/**
 * A row in an [IosSection]: optional SF-style [icon] in the tint (or any [leading] content), [title], optional
 * [subtitle] under it, optional grey [value] on the right, and an [accessory]. [titleColor] lets action rows ("Pair a
 * Radio", "Clear App Data") use the tint or red. The section draws the separator above it.
 */
@Composable
fun IosRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = Ios.colors.tint,
    leading: (@Composable () -> Unit)? = null,
    subtitle: String? = null,
    value: String? = null,
    titleColor: Color = Ios.colors.label,
    accessory: IosAccessory = IosAccessory.None,
    showSeparator: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val c = Ios.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val textStart: Dp = if (icon != null || leading != null) IconTextStart else IosInset
    val clickModifier =
        when {
            accessory is IosAccessory.Switch ->
                Modifier.clickable(source, indication = null, enabled = accessory.enabled, role = Role.Switch) {
                    accessory.onCheckedChange(!accessory.checked)
                }

            onClick != null -> Modifier.clickable(source, indication = null, role = Role.Button, onClick = onClick)

            else -> Modifier
        }
    Row(
        Modifier.then(if (showSeparator) Modifier.iosRowSeparator(textStart) else Modifier)
            .fillMaxWidth()
            .background(if (pressed) c.systemGray4 else Color.Transparent)
            .then(clickModifier)
            .then(modifier)
            .defaultMinSize(minHeight = 52.dp)
            .padding(end = IosInset),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(textStart), contentAlignment = Alignment.Center) {
            when {
                leading != null -> leading()
                icon != null -> Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
        }
        Column(Modifier.weight(1f).padding(vertical = 11.dp)) {
            Text(title, style = Ios.type.body, color = titleColor)
            if (subtitle != null) {
                Text(subtitle, style = Ios.type.subheadline, color = c.secondaryLabel)
            }
        }
        if (value != null) {
            Text(
                value,
                style = Ios.type.body,
                color = c.secondaryLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        when (accessory) {
            IosAccessory.None -> Unit

            IosAccessory.Chevron ->
                Icon(
                    IosIcons.ChevronRight,
                    contentDescription = null,
                    tint = c.tertiaryLabel,
                    modifier = Modifier.padding(start = 8.dp).size(14.dp),
                )

            IosAccessory.Checkmark ->
                Icon(IosIcons.Checkmark, contentDescription = null, tint = c.tint, modifier = Modifier.size(20.dp))

            is IosAccessory.Switch ->
                IosSwitch(
                    accessory.checked,
                    onCheckedChange = null,
                    enabled = accessory.enabled,
                    modifier = Modifier.padding(start = 8.dp),
                )

            is IosAccessory.Custom -> accessory.content()
        }
    }
}
