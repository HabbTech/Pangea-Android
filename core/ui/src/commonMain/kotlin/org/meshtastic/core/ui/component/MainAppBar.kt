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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.PowerMode
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.about_pangea
import org.meshtastic.core.resources.event_info
import org.meshtastic.core.resources.navigate_back
import org.meshtastic.core.resources.pangea_logo
import org.meshtastic.core.resources.power_mode_quick_access
import org.meshtastic.core.ui.icon.CalendarMonth
import org.meshtastic.core.ui.icon.Info
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosActionCapsule
import org.meshtastic.core.ui.ios.IosBarLayout
import org.meshtastic.core.ui.ios.IosGlassButton
import org.meshtastic.core.ui.ios.IosGlassIconButton
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosMenu
import org.meshtastic.core.ui.ios.IosMenuDivider
import org.meshtastic.core.ui.ios.IosMenuItem
import org.meshtastic.core.ui.ios.IosRadioChip
import org.meshtastic.core.ui.ios.LocalIosRadioChip
import org.meshtastic.core.ui.theme.LocalEventTheme
import org.meshtastic.core.ui.util.LocalEventBranding
import org.meshtastic.core.ui.util.LocalPowerModeMenu
import org.meshtastic.core.ui.util.PowerModeMenuState
import org.meshtastic.core.ui.util.icon
import org.meshtastic.core.ui.util.labelRes

/** Alpha for the ambient event accent wash over the app bar — subtle enough to keep title text legible. */
private const val EVENT_ACCENT_ALPHA = 0.12f

/** Height of the event brand rule under the app bar. A hairline: brand presence without stealing vertical space. */
private val EVENT_BRAND_RULE_HEIGHT = 3.dp

/**
 * The top of a screen, drawn like the Pangea iPhone app (design/PANGEA-BRAND.md v2): a glass back button on pushed
 * screens or the round logo button (Power Mode menu) on a tab, the screen's actions grouped in a glass capsule at the
 * right (with our radio's chip), then the large title and an optional subtitle.
 */
@Composable
fun MainAppBar(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    ourNode: Node?,
    showNodeChip: Boolean,
    canNavigateUp: Boolean,
    onNavigateUp: () -> Unit,
    onClickChip: (Node) -> Unit,
    // The screen's own background: white over iOS's plain lists, grey (the default) over grouped ones.
    background: Color = MaterialTheme.colorScheme.background,
    // iOS's inline title (navigationBarTitleDisplayMode(.inline)): the title small and centred in the top row.
    inlineTitle: Boolean = false,
    // Expert Mode's radio chip, on pushed screens and the tab roots that ask for it ([showNodeChip]); false where iOS
    // leaves it out (node detail).
    radioChip: Boolean = true,
    // Trailing slot: a @Composable content lambda, not an event handler (detekt LambdaParameterEventTrailing).
    actions: @Composable () -> Unit,
) {
    // Ambient event theming: when connected to event firmware (and not opted out), tint the bar with a faint wash of
    // the edition's accent color. Gated with the app-wide fonts via LocalEventTheme / the "Use event theme" toggle.
    val eventTheme = LocalEventTheme.current
    val accent = eventTheme?.accent
    val c = Ios.colors
    Column(
        modifier =
        modifier
            .fillMaxWidth()
            .background(accent?.copy(alpha = EVENT_ACCENT_ALPHA)?.compositeOver(background) ?: background)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        IosBarLayout(
            modifier = Modifier.fillMaxWidth().height(TOP_ROW_HEIGHT).padding(horizontal = 16.dp),
            leading = {
                if (canNavigateUp) {
                    IosGlassIconButton(
                        IosIcons.ChevronLeft,
                        contentDescription = stringResource(Res.string.navigate_back),
                        onClick = onNavigateUp,
                    )
                } else {
                    PangeaLogoButton()
                }
            },
            title = {
                if (inlineTitle) {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = Ios.type.headline,
                        color = c.label,
                    )
                }
            },
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IosActionCapsule { CompositionLocalProvider(LocalContentColor provides c.label) { actions() } }
                    // iOS's radio chip (Expert Mode only): on the tab roots that ask for it, and on every pushed
                    // screen.
                    val chip = LocalIosRadioChip.current
                    if (chip != null && radioChip && (showNodeChip || canNavigateUp)) {
                        Spacer(Modifier.width(10.dp))
                        IosRadioChip(chip, onClick = ourNode?.let { node -> { onClickChip(node) } })
                    }
                }
            },
        )
        if (!inlineTitle) LargeTitle(title, subtitle)
        Spacer(Modifier.height(8.dp))
        EventPaletteStrip(palette = eventTheme?.palette.orEmpty(), height = EVENT_BRAND_RULE_HEIGHT)
    }
}

/** The large title under the top row, with an optional [subtitle]. */
@Composable
private fun LargeTitle(title: String, subtitle: String?) {
    val c = Ios.colors
    Column {
        Text(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = Ios.type.largeTitle,
            color = c.label,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = Ios.type.subheadline,
                color = c.secondaryLabel,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

private val TOP_ROW_HEIGHT = 60.dp

/**
 * The round logo button at the top left of Chats and Settings, as on the Pangea iPhone app: the full-colour logo in a
 * glass circle. Tapping it opens the Power Mode menu (P8), where the Material app bar shows a lightning icon.
 */
@Composable
fun PangeaLogoButton(modifier: Modifier = Modifier) {
    val state = LocalPowerModeMenu.current
    val eventEdition = LocalEventBranding.current
    var showMenu by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }
    Box(modifier) {
        IosGlassButton(
            onClick = { if (state != null) showMenu = true },
            contentDescription =
            state?.let {
                stringResource(Res.string.power_mode_quick_access, stringResource(it.currentMode.labelRes))
            },
        ) {
            Image(
                painter = painterResource(Res.drawable.pangea_logo),
                contentDescription = null,
                modifier = Modifier.size(30.dp).clip(CircleShape),
            )
        }
        if (state != null) {
            PowerModeMenu(
                expanded = showMenu,
                state = state,
                onDismiss = { showMenu = false },
                onShowEventInfo =
                if (eventEdition != null) {
                    fun() {
                        showSheet = true
                    }
                } else {
                    null
                },
            )
        }
    }
    if (showSheet && eventEdition != null) {
        EventInfoSheet(edition = eventEdition, onDismiss = { showSheet = false })
    }
}

/** Dropdown anchored to the top-bar quick-access action: Power Mode picker, About entry, optional Event info entry. */
@Composable
private fun PowerModeMenu(
    expanded: Boolean,
    state: PowerModeMenuState,
    onDismiss: () -> Unit,
    onShowEventInfo: (() -> Unit)?,
) {
    IosMenu(expanded = expanded, onDismissRequest = onDismiss) {
        PowerMode.entries.forEach { mode ->
            IosMenuItem(
                text = stringResource(mode.labelRes),
                icon = mode.icon,
                checked = mode == state.currentMode,
                onClick = {
                    state.onSelectMode(mode)
                    onDismiss()
                },
            )
        }
        IosMenuDivider()
        IosMenuItem(
            text = stringResource(Res.string.about_pangea),
            icon = MeshtasticIcons.Info,
            onClick = {
                state.onAboutClick()
                onDismiss()
            },
        )
        // EVENT-INFO ENTRY — delete this block (and the onShowEventInfo parameter plumbing above) to remove the
        // Event info menu item entirely; the event info sheet remains reachable nowhere else once removed.
        if (onShowEventInfo != null) {
            IosMenuItem(
                text = stringResource(Res.string.event_info),
                icon = MeshtasticIcons.CalendarMonth,
                onClick = {
                    onShowEventInfo()
                    onDismiss()
                },
            )
        }
    }
}
