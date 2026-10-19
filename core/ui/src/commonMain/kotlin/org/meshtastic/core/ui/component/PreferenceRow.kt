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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.meshtastic.core.ui.ios.Ios

/**
 * One row of a settings form, drawn like the Pangea iPhone app's inset-grouped `Form`: the [title] and its [trailing]
 * control on one line, the grey [summary] under both, then anything [below]. 16 dp in from the card's edges, so
 * [PreferenceDivider]s line up with the text. With [fillTitle] false the title takes only its own width and [trailing]
 * gets the rest (inline text fields).
 */
@Composable
internal fun PreferenceRow(
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
    Column(modifier.fillMaxWidth().alpha(alpha).padding(horizontal = 16.dp, vertical = 11.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 28.dp), verticalAlignment = Alignment.CenterVertically) {
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
        below()
    }
}

/** How a form row looks while it can't be changed (no radio connected, or another setting rules it out). */
private const val DISABLED_ALPHA = 0.45f
