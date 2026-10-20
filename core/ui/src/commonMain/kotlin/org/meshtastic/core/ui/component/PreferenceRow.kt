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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.iosPressable

/**
 * One row of a settings form, drawn like the Pangea iPhone app's inset-grouped `Form`: the [title] and its [trailing]
 * control on one line, the grey [summary] under both, then anything [below]. 16 dp in from the card's edges, so
 * [PreferenceDivider]s line up with the text. With [fillTitle] false the title takes only its own width and [trailing]
 * gets the rest (inline text fields).
 */
@Composable
fun PreferenceRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    fillTitle: Boolean = true,
    trailing: @Composable RowScope.() -> Unit = {},
    below: @Composable ColumnScope.() -> Unit = {},
) {
    val c = Ios.colors
    val alpha = if (enabled) 1f else DISABLED_ALPHA
    val icon = LocalPreferenceIcon.current
    Column(modifier.fillMaxWidth().alpha(alpha).padding(horizontal = 16.dp, vertical = 11.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 28.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon.icon, contentDescription = null, tint = icon.tint ?: c.tint, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(14.dp))
            }
            Text(
                text = title,
                style = Ios.type.body,
                color = c.label,
                modifier =
                (if (fillTitle) Modifier.weight(1f) else Modifier)
                    // The title names the row for tests and screen readers, so it carries the row's disabled state.
                    .then(if (enabled) Modifier else Modifier.semantics { disabled() }),
            )
            Spacer(Modifier.width(12.dp))
            trailing()
        }
        if (!summary.isNullOrEmpty()) {
            Text(
                text = summary,
                style = Ios.type.callout,
                color = c.secondaryLabel,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        // A symbol belongs to this row only, not to anything drawn inside it.
        CompositionLocalProvider(LocalPreferenceIcon provides null) { below() }
    }
}

/** The symbol iOS shows before a setting's title (a clock before "12 Hour Clock"). */
class PreferenceIconSpec(val icon: ImageVector, val tint: Color?)

/** Set by [PreferenceIcon] for the one setting inside it. */
@Suppress("CompositionLocalAllowlist")
internal val LocalPreferenceIcon = compositionLocalOf<PreferenceIconSpec?> { null }

/**
 * Gives the setting inside [content] the symbol iOS shows before its title, in the tint (or [tint]), with the help text
 * still starting at the card's edge, as on the iPhone app's forms.
 */
@Composable
fun PreferenceIcon(icon: ImageVector, tint: Color? = null, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPreferenceIcon provides PreferenceIconSpec(icon, tint), content = content)
}

/** How a form row looks while it can't be changed (no radio connected, or another setting rules it out). */
private const val DISABLED_ALPHA = 0.45f

/**
 * A setting that is an action, as the iPhone app draws Regenerate Private Key: the title (with its [PreferenceIcon])
 * and a round grey button with [actionIcon] at the end, the help text under both.
 */
@Composable
fun PreferenceActionRow(
    title: String,
    actionIcon: ImageVector,
    actionDescription: String,
    enabled: Boolean,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    val c = Ios.colors
    PreferenceRow(
        title = title,
        modifier = modifier,
        enabled = enabled,
        // iOS sets an action's explanation in footnote, smaller than a setting's.
        below = {
            if (summary != null) {
                Text(
                    text = summary,
                    style = Ios.type.footnote,
                    color = c.secondaryLabel,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        },
        trailing = {
            Box(
                Modifier.size(44.dp)
                    .clip(CircleShape)
                    .background(c.fieldFill)
                    .iosPressable(enabled = enabled, onClick = onAction)
                    .semantics { contentDescription = actionDescription },
                contentAlignment = Alignment.Center,
            ) {
                Icon(actionIcon, contentDescription = null, tint = c.secondaryLabel, modifier = Modifier.size(24.dp))
            }
        },
    )
}

/** A form row that is a button, as iOS draws one: the [title] alone, in the tint. */
@Composable
fun PreferenceButtonRow(title: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = Ios.type.body,
        color = Ios.colors.tint,
        modifier =
        modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .iosPressable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp)
            .heightIn(min = 28.dp)
            .wrapContentHeight(Alignment.CenterVertically),
    )
}
