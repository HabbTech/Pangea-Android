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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.DeviceAddress
import org.meshtastic.core.model.DeviceType
import org.meshtastic.core.model.MeshActivity
import org.meshtastic.core.navigation.ContactsRoute
import org.meshtastic.core.navigation.MultiBackstack
import org.meshtastic.core.navigation.NodesRoute
import org.meshtastic.core.navigation.TopLevelDestination
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.pangea_status_connecting
import org.meshtastic.core.resources.pangea_status_disconnected
import org.meshtastic.core.resources.pangea_status_no_radio
import org.meshtastic.core.resources.pangea_status_pair_radio
import org.meshtastic.core.resources.pangea_status_reconnect
import org.meshtastic.core.resources.pangea_tab_chats
import org.meshtastic.core.resources.pangea_tab_map
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosStatusPill
import org.meshtastic.core.ui.ios.IosTab
import org.meshtastic.core.ui.ios.IosTabBar
import org.meshtastic.core.ui.ios.LocalIosBottomInset
import org.meshtastic.core.ui.ios.LocalIosUnderTabBar
import org.meshtastic.core.ui.navigation.icon
import org.meshtastic.core.ui.viewmodel.UIViewModel

/**
 * The app shell, drawn like the Pangea iPhone app (design/PANGEA-BRAND.md v2): screens fill the window and the iOS 26
 * floating tab bar sits over the bottom, with the radio status pill above it on Chats in standard mode.
 *
 * Uses the [MultiBackstack] state holder for independent histories per tab, as Navigation 3 recommends.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun MeshtasticNavigationSuite(
    multiBackstack: MultiBackstack,
    uiViewModel: UIViewModel,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val connectionState by uiViewModel.connectionState.collectAsStateWithLifecycle()
    val nodeRestartExpected by uiViewModel.nodeRestartExpected.collectAsStateWithLifecycle()
    val watchdogReconnectInFlight by uiViewModel.watchdogReconnectInFlight.collectAsStateWithLifecycle()
    val unreadMessageCount by uiViewModel.unreadMessageCount.collectAsStateWithLifecycle()
    val selectedDevice by uiViewModel.currentDeviceAddressFlow.collectAsStateWithLifecycle()
    val expertModeEnabled by uiViewModel.expertModeEnabled.collectAsStateWithLifecycle()
    val hasRadio = DeviceAddress.parse(selectedDevice) != null
    val destinations = visibleDestinations(expertModeEnabled)

    val currentTabRoute = multiBackstack.currentTabRoute
    val topLevelDestination = TopLevelDestination.fromNavKey(currentTabRoute)

    // Pangea: a tab hidden by standard mode lands on Chats, except Connect, which pairing opens.
    LaunchedEffect(topLevelDestination, destinations) {
        val current = topLevelDestination ?: return@LaunchedEffect
        if (current !in destinations && current != TopLevelDestination.Connect) {
            multiBackstack.navigateTopLevel(TopLevelDestination.Messages.route)
        }
    }

    // iOS shows the radio's state above the tab bar on Chats until the radio is ready; Expert Mode shows it on the
    // Connect tab instead.
    val restarting = nodeRestartExpected && connectionState != ConnectionState.Connected
    val pill: RadioPill? =
        when {
            expertModeEnabled || topLevelDestination != TopLevelDestination.Messages -> null

            !hasRadio -> RadioPill.NotPaired

            connectionState == ConnectionState.Connected -> null

            connectionState == ConnectionState.Disconnected && !restarting && !watchdogReconnectInFlight ->
                RadioPill.Disconnected

            else -> RadioPill.Connecting
        }

    val chatsLabel = stringResource(Res.string.pangea_tab_chats)
    val mapLabel = stringResource(Res.string.pangea_tab_map)
    val tabs =
        destinations.map { destination ->
            // iOS names the tabs plainly: Chats rather than Messages in standard mode, and Map rather than Mesh Map.
            val label =
                when {
                    destination == TopLevelDestination.Messages && !expertModeEnabled -> chatsLabel
                    destination == TopLevelDestination.Map -> mapLabel
                    else -> stringResource(destination.label)
                }
            IosTab(
                key = destination,
                label = label,
                badge = if (destination == TopLevelDestination.Messages) unreadMessageCount else 0,
            ) { tint ->
                TabIcon(
                    destination = destination,
                    tint = tint,
                    connectionState = connectionState,
                    restarting = restarting,
                    reconnecting = watchdogReconnectInFlight && !restarting,
                    selectedDevice = selectedDevice,
                    meshActivityFlow = uiViewModel.meshActivity,
                )
            }
        }

    val density = LocalDensity.current
    var chromeHeight by remember { mutableStateOf(0.dp) }
    val underTabBar = remember { mutableIntStateOf(0) }
    Box(modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalIosBottomInset provides chromeHeight, LocalIosUnderTabBar provides underTabBar) {
            // Older screens stop above the bar; iOS-style screens scroll under it (IosDrawsUnderTabBar).
            Box(Modifier.fillMaxSize().padding(bottom = if (underTabBar.intValue > 0) 0.dp else chromeHeight)) {
                content()
            }
        }
        Column(
            modifier =
            Modifier.align(Alignment.BottomCenter)
                .onSizeChanged { chromeHeight = with(density) { it.height.toDp() } }
                .navigationBarsPadding()
                .padding(bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (pill != null) {
                RadioStatusPill(pill) { multiBackstack.navigateTopLevel(TopLevelDestination.Connect.route) }
                Spacer(Modifier.height(10.dp))
            }
            IosTabBar(
                tabs = tabs,
                selected = topLevelDestination,
                onSelect = { tab ->
                    handleNavigation(tab.key as TopLevelDestination, topLevelDestination, multiBackstack, uiViewModel)
                },
            )
        }
    }
}

private enum class RadioPill {
    NotPaired,
    Disconnected,
    Connecting,
}

@Composable
private fun RadioStatusPill(pill: RadioPill, onPair: () -> Unit) {
    when (pill) {
        RadioPill.NotPaired ->
            IosStatusPill(
                text = stringResource(Res.string.pangea_status_no_radio),
                leading = IosIcons.Antenna,
                actionLabel = stringResource(Res.string.pangea_status_pair_radio),
                onAction = onPair,
            )

        RadioPill.Disconnected ->
            IosStatusPill(
                text = stringResource(Res.string.pangea_status_disconnected),
                leading = IosIcons.Antenna,
                actionLabel = stringResource(Res.string.pangea_status_reconnect),
                onAction = onPair,
            )

        RadioPill.Connecting -> IosStatusPill(text = stringResource(Res.string.pangea_status_connecting), busy = true)
    }
}

/** The tab icons: iOS-style Map, Chats and Settings; Nodes and Connect keep their own (Connect animates). */
@Composable
private fun TabIcon(
    destination: TopLevelDestination,
    tint: Color,
    connectionState: ConnectionState,
    restarting: Boolean,
    reconnecting: Boolean,
    selectedDevice: String?,
    meshActivityFlow: Flow<MeshActivity>,
) {
    val iconModifier = Modifier.size(26.dp)
    when (destination) {
        TopLevelDestination.Map -> Icon(IosIcons.Map, contentDescription = null, tint = tint, modifier = iconModifier)

        TopLevelDestination.Messages ->
            Icon(IosIcons.Bubble, contentDescription = null, tint = tint, modifier = iconModifier)

        TopLevelDestination.Settings ->
            Icon(IosIcons.Gear, contentDescription = null, tint = tint, modifier = iconModifier)

        TopLevelDestination.Connect -> {
            // An expected restart or a watchdog recovery shows as "connecting", not a scary disconnect.
            val presented =
                if ((restarting || reconnecting) && connectionState == ConnectionState.Disconnected) {
                    ConnectionState.Connecting
                } else {
                    connectionState
                }
            CompositionLocalProvider(LocalContentColor provides tint) {
                AnimatedConnectionsNavIcon(
                    connectionState = presented,
                    deviceType = DeviceType.fromAddress(selectedDevice ?: "NoDevice"),
                    meshActivityFlow = meshActivityFlow,
                )
            }
        }

        TopLevelDestination.Nodes ->
            Icon(vectorResource(destination.icon), contentDescription = null, tint = tint, modifier = iconModifier)
    }
}

private fun handleNavigation(
    destination: TopLevelDestination,
    topLevelDestination: TopLevelDestination?,
    multiBackstack: MultiBackstack,
    uiViewModel: UIViewModel,
) {
    val isRepress = destination == topLevelDestination
    if (isRepress) {
        val currentKey = multiBackstack.activeBackStack.lastOrNull()
        when (destination) {
            TopLevelDestination.Nodes -> {
                val onNodesList = currentKey is NodesRoute.Nodes
                if (!onNodesList) {
                    multiBackstack.navigateTopLevel(destination.route)
                } else {
                    uiViewModel.emitScrollToTopEvent(ScrollToTopEvent.NodesTabPressed)
                }
            }

            TopLevelDestination.Messages -> {
                val onConversationsList = currentKey is ContactsRoute.Contacts
                if (!onConversationsList) {
                    multiBackstack.navigateTopLevel(destination.route)
                } else {
                    uiViewModel.emitScrollToTopEvent(ScrollToTopEvent.ConversationsTabPressed)
                }
            }

            else -> {
                if (currentKey != destination.route) {
                    multiBackstack.navigateTopLevel(destination.route)
                }
            }
        }
    } else {
        multiBackstack.navigateTopLevel(destination.route)
    }
}

/**
 * Pangea standard mode (Pangea-Apple ContentView): Map, Chats and Settings, in that order. Pairing a radio goes through
 * the status pill or Settings → My Radio, which open Connect. Expert Mode shows every tab.
 */
internal fun visibleDestinations(expertMode: Boolean): List<TopLevelDestination> = if (expertMode) {
    TopLevelDestination.entries
} else {
    listOf(TopLevelDestination.Map, TopLevelDestination.Messages, TopLevelDestination.Settings)
}
