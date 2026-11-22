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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.iosPressable
import org.meshtastic.core.ui.theme.AppTheme

/**
 * A channel as the iPhone app lists it: the slot number in a tinted circle, the channel's name with an optional
 * [titleIcon] before it (its lock) and [subtitle] under it ("Primary channel"), then [content] at the end.
 */
@Composable
fun ChannelItem(
    index: Int,
    title: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    subtitle: String? = null,
    titleIcon: (@Composable () -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val c = Ios.colors
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .iosPressable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(c.tint), contentAlignment = Alignment.Center) {
            Text(text = "$index", style = Ios.type.title2, color = c.onTint)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (titleIcon != null) {
                    // The lock is a tappable explanation; keep its touch target without letting it push the name.
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                        Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) { titleIcon() }
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = title,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = Ios.type.headline,
                    color = c.label,
                )
            }
            if (subtitle != null) {
                Text(text = subtitle, style = Ios.type.footnote, color = c.secondaryLabel)
            }
        }
        content()
    }
}

@Preview
@Composable
fun ChannelItemPreview() {
    AppTheme { ChannelItem(index = 0, title = "Medium Fast", enabled = true) {} }
}
