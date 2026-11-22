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
package org.meshtastic.feature.intro

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.meshtastic.core.ui.util.PermissionStatus
import org.meshtastic.core.ui.util.PermissionUiState

/** Navigation graph for the application introduction / onboarding flow, in the iPhone app's order. */
@Suppress("LongMethod")
internal fun EntryProviderScope<NavKey>.introGraph(
    backStack: NavBackStack<NavKey>,
    viewModel: IntroViewModel,
    onDone: () -> Unit,
    pairRadio: (@Composable (onPair: () -> Unit) -> Unit)? = null,
) {
    fun navigateToNext(
        current: NavKey,
        permissionsGranted: Boolean = true,
        bluetoothRequiresLocation: Boolean = false,
        bluetoothSupported: Boolean = true,
    ) {
        val next =
            viewModel.getNextKey(
                current,
                permissionsGranted,
                bluetoothRequiresLocation,
                bluetoothSupported,
                canPairRadio = pairRadio != null,
            )
        if (next != null) {
            backStack.add(next)
        } else {
            onDone()
        }
    }

    entry<Welcome> { WelcomeScreen(onGetStarted = { navigateToNext(Welcome) }) }

    entry<Notifications> {
        WithBack(backStack) {
            val notification = LocalIntroPermissions.current.notification
            // Null means the platform doesn't gate notifications at runtime, so there is nothing to ask for.
            NotificationsScreen(
                status = notification?.status ?: PermissionStatus.GRANTED,
                onPrimaryAction =
                rememberAskThenContinue(notification) { granted ->
                    navigateToNext(Notifications, permissionsGranted = granted)
                },
                onOpenSettings = { notification?.openAppSettings?.invoke() },
            )
        }
    }

    entry<CriticalAlerts> {
        WithBack(backStack) {
            val settingsNavigator = LocalIntroSettingsNavigator.current
            CriticalAlertsScreen(
                onConfigure = {
                    settingsNavigator.openCriticalAlertsSettings()
                    navigateToNext(CriticalAlerts)
                },
            )
        }
    }

    entry<Location> {
        WithBack(backStack) {
            val permissions = LocalIntroPermissions.current
            LocationScreen(
                status = permissions.location.status,
                onPrimaryAction =
                rememberAskThenContinue(permissions.location) {
                    navigateToNext(
                        Location,
                        bluetoothRequiresLocation = permissions.bluetoothRequiresLocation,
                        bluetoothSupported = permissions.bluetoothSupported,
                    )
                },
                onOpenSettings = permissions.location.openAppSettings,
            )
        }
    }

    entry<Bluetooth> {
        WithBack(backStack) {
            val permissions = LocalIntroPermissions.current
            BluetoothScreen(
                status = permissions.bluetooth.status,
                onPrimaryAction = rememberAskThenContinue(permissions.bluetooth) { navigateToNext(Bluetooth) },
                onOpenSettings = permissions.bluetooth.openAppSettings,
            )
        }
    }

    entry<ExpertMode> { WithBack(backStack) { ExpertModeScreen(onContinue = { navigateToNext(ExpertMode) }) } }

    // As on iOS, the last three pages have no back button: pairing and naming the radio aren't undone by going back.
    entry<PairRadio> {
        pairRadio?.let { PairRadioScreen(pairRadio = it, onPair = { navigateToNext(PairRadio) }, onNoRadio = onDone) }
    }

    entry<SetupProfile> { SetupProfileScreen(onFinish = { navigateToNext(SetupProfile) }) }

    entry<AllSet> { AllSetScreen(onStart = onDone) }
}

/**
 * A setup page's permission button, as on the iPhone app: it asks once, then moves on whatever the answer — once the
 * system dialog closes (the activity pauses and resumes) or the permission is granted. With nothing to ask (already
 * granted, not gated on this platform, or Android will no longer show the dialog) it moves straight on. [onContinue] is
 * told whether the permission is now held.
 */
@Composable
private fun rememberAskThenContinue(state: PermissionUiState?, onContinue: (granted: Boolean) -> Unit): () -> Unit {
    val currentState by rememberUpdatedState(state)
    val currentOnContinue by rememberUpdatedState(onContinue)
    var asking by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var answered by remember { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (asking && event == Lifecycle.Event.ON_PAUSE) paused = true
            if (asking && paused && event == Lifecycle.Event.ON_RESUME) answered = true
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val granted = state == null || state.status == PermissionStatus.GRANTED
    // Runs after the composition that saw the answer, so the permission's new status is the one passed on.
    LaunchedEffect(answered, granted) {
        if (asking && (answered || granted)) {
            asking = false
            paused = false
            answered = false
            currentOnContinue(granted)
        }
    }
    return {
        val current = currentState
        if (current != null && introPermissionAction(current.status) == IntroPermissionAction.REQUEST) {
            asking = true
            current.request()
        } else {
            currentOnContinue(current == null || current.status == PermissionStatus.GRANTED)
        }
    }
}

/** Gives a setup page its back button (every page after Welcome). */
@Composable
private fun WithBack(backStack: NavBackStack<NavKey>, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalIntroBack provides { backStack.removeLastOrNull() }, content = content)
}
