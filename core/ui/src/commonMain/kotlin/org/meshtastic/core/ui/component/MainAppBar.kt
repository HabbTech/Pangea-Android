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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.PowerMode
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.about_pangea
import org.meshtastic.core.resources.event_info
import org.meshtastic.core.resources.ic_meshtastic
import org.meshtastic.core.resources.navigate_back
import org.meshtastic.core.ui.icon.ArrowBack
import org.meshtastic.core.ui.icon.CalendarMonth
import org.meshtastic.core.ui.icon.Check
import org.meshtastic.core.ui.icon.Info
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.theme.LocalEventTheme
import org.meshtastic.core.ui.util.EventBrandingIcon
import org.meshtastic.core.ui.util.LocalEventBranding
import org.meshtastic.core.ui.util.LocalPowerModeMenu
import org.meshtastic.core.ui.util.PowerModeMenuState
import org.meshtastic.core.ui.util.icon
import org.meshtastic.core.ui.util.labelRes

/** Alpha for the ambient event accent wash over the app bar — subtle enough to keep title text legible. */
private const val EVENT_ACCENT_ALPHA = 0.12f

/** Height of the event brand rule under the app bar. A hairline: brand presence without stealing vertical space. */
private val EVENT_BRAND_RULE_HEIGHT = 3.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainAppBar(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    ourNode: Node?,
    showNodeChip: Boolean,
    canNavigateUp: Boolean,
    onNavigateUp: () -> Unit,
    actions: @Composable () -> Unit,
    onClickChip: (Node) -> Unit,
    brandingContent: @Composable () -> Unit = { EventAwareBranding() },
) {
    // Ambient event theming: when connected to event firmware (and not opted out), tint the bar with a faint wash of
    // the edition's accent color. Gated with the app-wide fonts via LocalEventTheme / the "Use event theme" toggle.
    val eventTheme = LocalEventTheme.current
    val accent = eventTheme?.accent
    val colors =
        if (accent != null) {
            TopAppBarDefaults.topAppBarColors(
                containerColor =
                accent.copy(alpha = EVENT_ACCENT_ALPHA).compositeOver(MaterialTheme.colorScheme.surface),
            )
        } else {
            TopAppBarDefaults.topAppBarColors()
        }
    Column(modifier = modifier) {
        TopAppBar(
            colors = colors,
            title = {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLargeEmphasized,
                )
            },
            subtitle = {
                subtitle?.let {
                    Text(
                        text = it,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            navigationIcon =
            if (canNavigateUp) {
                {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = MeshtasticIcons.ArrowBack,
                            contentDescription = stringResource(Res.string.navigate_back),
                        )
                    }
                }
            } else {
                { brandingContent() }
            },
            actions = {
                TopBarActions(
                    ourNode = ourNode,
                    showNodeChip = showNodeChip,
                    actions = actions,
                    onClickChip = onClickChip,
                )
            },
        )
        EventPaletteStrip(palette = eventTheme?.palette.orEmpty(), height = EVENT_BRAND_RULE_HEIGHT)
    }
}

/**
 * Nav-bar branding slot. When [LocalPowerModeMenu] is provided, the logo opens the Power Mode menu (with an About
 * entry, and an Event info entry while event branding is active). Without it, falls back to the original behavior:
 * event editions are tappable for their info sheet, otherwise a plain logo.
 */
@Composable
private fun EventAwareBranding() {
    val eventEdition = LocalEventBranding.current
    val powerModeMenu = LocalPowerModeMenu.current
    var showSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    if (powerModeMenu == null && eventEdition == null) {
        Icon(imageVector = vectorResource(Res.drawable.ic_meshtastic), contentDescription = null)
        return
    }
    // Every event edition is tappable for its info sheet. The icon prefers the hosted iconUrl, then a bundled
    // drawable, then the Meshtastic logo — see EventBrandingIcon. When a Power Mode menu is provided, the logo
    // opens that menu instead and the event sheet moves behind its About item.
    val brandingModifier =
        Modifier.size(32.dp).clip(CircleShape).clickable(role = Role.Button) {
            if (powerModeMenu != null) showMenu = true else showSheet = true
        }
    Box {
        // With Power Mode the logo is shown even outside an event, so the edition can be null here —
        // EventBrandingIcon requires one, so fall back to the plain mark.
        if (eventEdition != null) {
            EventBrandingIcon(edition = eventEdition, modifier = brandingModifier)
        } else {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_meshtastic),
                contentDescription = null,
                modifier = brandingModifier,
            )
        }
        if (powerModeMenu != null) {
            PowerModeMenu(
                expanded = showMenu,
                state = powerModeMenu,
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

/** Dropdown anchored to the nav-bar logo: Power Mode picker, About entry, and optional Event info entry. */
@Composable
private fun PowerModeMenu(
    expanded: Boolean,
    state: PowerModeMenuState,
    onDismiss: () -> Unit,
    onShowEventInfo: (() -> Unit)?,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        PowerMode.entries.forEach { mode ->
            DropdownMenuItem(
                text = { Text(stringResource(mode.labelRes)) },
                leadingIcon = { Icon(imageVector = mode.icon, contentDescription = null) },
                trailingIcon =
                if (mode == state.currentMode) {
                    { Icon(imageVector = MeshtasticIcons.Check, contentDescription = null) }
                } else {
                    null
                },
                onClick = {
                    state.onSelectMode(mode)
                    onDismiss()
                },
            )
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.about_pangea)) },
            leadingIcon = { Icon(imageVector = MeshtasticIcons.Info, contentDescription = null) },
            onClick = {
                state.onAboutClick()
                onDismiss()
            },
        )
        // EVENT-INFO ENTRY — delete this block (and the onShowEventInfo parameter plumbing above) to remove the
        // Event info menu item entirely; the event info sheet remains reachable nowhere else once removed.
        if (onShowEventInfo != null) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.event_info)) },
                leadingIcon = { Icon(imageVector = MeshtasticIcons.CalendarMonth, contentDescription = null) },
                onClick = {
                    onShowEventInfo()
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun TopBarActions(
    ourNode: Node?,
    showNodeChip: Boolean,
    actions: @Composable () -> Unit,
    onClickChip: (Node) -> Unit,
) {
    AnimatedVisibility(visible = showNodeChip, enter = fadeIn(), exit = fadeOut()) {
        ourNode?.let { node ->
            NodeChip(modifier = Modifier.padding(horizontal = 16.dp), node = node, onClick = onClickChip)
        }
    }

    actions()
}
