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
@file:Suppress("MagicNumber", "MatchingDeclarationName")

package org.meshtastic.core.ui.ios

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One tab of the floating tab bar. [icon] draws in [tint]; [badge] shows a red count when above zero. */
class IosTab(val key: Any, val label: String, val badge: Int = 0, val icon: @Composable (tint: Color) -> Unit)

/** The floating tab bar's height (without the margin under it). */
val IosTabBarHeight = 62.dp

/**
 * The iOS 26 floating tab bar: a glass capsule of tabs, the selected one on a grey pill and drawn in the tint. Tapping
 * the selected tab again calls [onSelect] too, so screens can pop to the top as on iOS.
 */
@Composable
fun IosTabBar(tabs: List<IosTab>, selected: Any?, onSelect: (IosTab) -> Unit, modifier: Modifier = Modifier) {
    val c = Ios.colors
    Row(
        modifier
            .height(IosTabBarHeight)
            .shadow(18.dp, CircleShape, ambientColor = c.chromeShadow, spotColor = c.chromeShadow)
            .clip(CircleShape)
            .background(c.chrome)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val isSelected = tab.key == selected
            val tint by animateColorAsState(if (isSelected) c.tint else c.label, label = "iosTabTint")
            Box(
                Modifier.width(if (tabs.size > 3) 72.dp else 90.dp)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(if (isSelected) c.chromeSelected else Color.Transparent)
                    .iosPressable(role = Role.Tab) { onSelect(tab) }
                    .semantics {
                        this.selected = isSelected
                        contentDescription = tab.label
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box {
                        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) { tab.icon(tint) }
                        if (tab.badge > 0) {
                            IosBadge(tab.badge, Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-6).dp))
                        }
                    }
                    Text(
                        tab.label,
                        style = Ios.type.caption2.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                        color = tint,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * How many on-screen destinations draw under the floating tab bar themselves (iOS-style screens built from
 * [IosLargeTitleScreen] / [IosPushedScreen]). While none do, the shell keeps content above the bar so older screens'
 * bottom controls stay reachable.
 */
@Suppress("CompositionLocalAllowlist")
val LocalIosUnderTabBar = staticCompositionLocalOf<MutableIntState> { mutableIntStateOf(0) }

/** Call from a screen that pads its own content by [LocalIosBottomInset] and can scroll under the tab bar. */
@Composable
fun IosDrawsUnderTabBar() {
    val counter = LocalIosUnderTabBar.current
    DisposableEffect(counter) {
        counter.intValue += 1
        onDispose { counter.intValue -= 1 }
    }
}
