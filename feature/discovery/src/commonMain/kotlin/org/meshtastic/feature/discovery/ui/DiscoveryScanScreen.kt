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
@file:Suppress("TooManyFunctions", "MagicNumber")

package org.meshtastic.feature.discovery.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.util.beaconJoinOption
import org.meshtastic.core.model.util.toJoinChannelSet
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.discovery_analysing_results
import org.meshtastic.core.resources.discovery_cancelling_scan
import org.meshtastic.core.resources.discovery_connection_warning
import org.meshtastic.core.resources.discovery_dwell_time_description
import org.meshtastic.core.resources.discovery_keep_screen_awake
import org.meshtastic.core.resources.discovery_keep_screen_awake_description
import org.meshtastic.core.resources.discovery_local_mesh
import org.meshtastic.core.resources.discovery_not_connected
import org.meshtastic.core.resources.discovery_not_connected_description
import org.meshtastic.core.resources.discovery_preparing
import org.meshtastic.core.resources.discovery_reconnecting
import org.meshtastic.core.resources.discovery_restoring_preset
import org.meshtastic.core.resources.discovery_scan_failed
import org.meshtastic.core.resources.discovery_scan_history
import org.meshtastic.core.resources.discovery_scan_progress
import org.meshtastic.core.resources.discovery_shifting_to
import org.meshtastic.core.resources.discovery_start_scan
import org.meshtastic.core.resources.discovery_start_scan_disabled
import org.meshtastic.core.resources.discovery_start_scan_reason_24ghz_unsupported
import org.meshtastic.core.resources.discovery_start_scan_reason_no_presets
import org.meshtastic.core.resources.discovery_start_scan_reason_not_connected
import org.meshtastic.core.resources.discovery_stop_scan
import org.meshtastic.core.resources.mesh_beacon_invitations_title
import org.meshtastic.core.resources.pangea_discovery_dwell_duration
import org.meshtastic.core.resources.pangea_discovery_dwell_header
import org.meshtastic.core.resources.pangea_discovery_hero_text
import org.meshtastic.core.resources.pangea_discovery_hero_title
import org.meshtastic.core.ui.component.DropDownPreference
import org.meshtastic.core.ui.component.SwitchPreference
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosButton
import org.meshtastic.core.ui.ios.IosButtonStyle
import org.meshtastic.core.ui.ios.IosGlassIconButton
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosInset
import org.meshtastic.core.ui.ios.IosPushedScreen
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.util.KeepScreenOn
import org.meshtastic.feature.discovery.DiscoveryScanState
import org.meshtastic.feature.discovery.DiscoveryViewModel
import org.meshtastic.feature.discovery.ui.component.BeaconChannelsCard
import org.meshtastic.feature.discovery.ui.component.DiscoverySectionHeader
import org.meshtastic.feature.discovery.ui.component.DwellProgressIndicator
import org.meshtastic.feature.discovery.ui.component.MeshBeaconInvitationCard
import org.meshtastic.feature.discovery.ui.component.PresetPickerCard
import org.meshtastic.proto.ChannelSet

private val DWELL_OPTIONS = listOf(1, 5, 15, 30, 45, 60, 90, 120, 180)

/**
 * Main scan screen for the Local Mesh Discovery feature, laid out as the iPhone app's: a large title with the history
 * button in the bar, a hero line, the preset grid, beacon channels, the dwell time, and the scan button at the end.
 */
@Suppress("LongMethod")
@Composable
fun DiscoveryScanScreen(
    viewModel: DiscoveryViewModel,
    onNavigateUp: () -> Unit,
    onNavigateToSummary: (sessionId: Long) -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier,
    onJoinOffer: (ChannelSet) -> Unit = {},
) {
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val selectedPresets by viewModel.selectedPresets.collectAsStateWithLifecycle()
    val beaconOffers by viewModel.beaconOffers.collectAsStateWithLifecycle()
    val beaconPresets by viewModel.beaconPresets.collectAsStateWithLifecycle()
    val beaconChannels by viewModel.beaconChannels.collectAsStateWithLifecycle()
    val selectedBeaconChannels by viewModel.selectedBeaconChannels.collectAsStateWithLifecycle()
    val currentLora by viewModel.currentLora.collectAsStateWithLifecycle()
    val currentChannels by viewModel.currentChannels.collectAsStateWithLifecycle()
    val dwellMinutes by viewModel.dwellDurationMinutes.collectAsStateWithLifecycle()
    val isConnected by viewModel.isConnected.collectAsStateWithLifecycle()
    val is24GhzBlocked by viewModel.is24GhzBlocked.collectAsStateWithLifecycle()
    val isLora24Region by viewModel.isLora24Region.collectAsStateWithLifecycle()
    val currentSession by viewModel.currentSession.collectAsStateWithLifecycle()
    val homePreset by viewModel.homePreset.collectAsStateWithLifecycle()

    var keepScreenAwake by rememberSaveable { mutableStateOf(true) }
    val isScanning = scanState !is DiscoveryScanState.Idle

    // Keep screen awake while a scan is in progress
    KeepScreenOn(isScanning && keepScreenAwake)

    // Navigate to summary when scan completes
    LaunchedEffect(scanState, onNavigateToSummary) {
        if (scanState is DiscoveryScanState.Complete) {
            currentSession?.id?.let { sessionId ->
                viewModel.reset()
                onNavigateToSummary(sessionId)
            }
        }
    }

    IosPushedScreen(
        title = stringResource(Res.string.discovery_local_mesh),
        onBack = onNavigateUp,
        modifier = modifier,
        largeTitle = true,
        trailing = {
            IosGlassIconButton(
                IosIcons.ClockArrowCirclepath,
                contentDescription = stringResource(Res.string.discovery_scan_history),
                onClick = onNavigateToHistory,
                tint = Ios.colors.tint,
            )
        },
    ) {
        item(key = "hero") { DiscoveryHero() }

        // Connection warning
        if (!isConnected) {
            item(key = "connection_warning") { ConnectionWarningCard() }
        }

        if (!isScanning) {
            // Received Mesh Beacon invitations from other meshes
            if (beaconOffers.isNotEmpty()) {
                item(key = "invitations_header") {
                    Text(
                        text = stringResource(Res.string.mesh_beacon_invitations_title),
                        style = Ios.type.headline,
                        color = Ios.colors.secondaryLabel,
                        modifier =
                        Modifier.padding(start = 32.dp, end = 32.dp, top = 20.dp, bottom = 8.dp).semantics {
                            heading()
                        },
                    )
                }
                items(beaconOffers.size, key = { "invitation_${beaconOffers[it].key}" }) { index ->
                    val offer = beaconOffers[index]
                    val joinOption =
                        remember(offer, currentLora, currentChannels) {
                            offer.beacon.beaconJoinOption(currentLora, currentChannels)
                        }
                    MeshBeaconInvitationCard(
                        offer = offer,
                        joinOption = joinOption,
                        onJoin = { offer.beacon.toJoinChannelSet(joinOption, currentLora)?.let(onJoinOffer) },
                        onDiscover = { viewModel.discoverOffer(offer) },
                        onDismiss = { viewModel.dismissOffer(offer) },
                        modifier = Modifier.padding(horizontal = IosInset, vertical = 4.dp),
                    )
                }
            }

            // Preset picker
            item(key = "preset_picker") {
                PresetPickerCard(
                    selectedPresets = selectedPresets,
                    homePreset = homePreset,
                    onTogglePreset = viewModel::togglePreset,
                    enabled = true,
                    beaconPresets = beaconPresets,
                )
            }

            // Beacon channels — custom channels advertised by beacons (hidden when none recorded)
            if (beaconChannels.isNotEmpty()) {
                item(key = "beacon_channels") {
                    BeaconChannelsCard(
                        channels = beaconChannels,
                        selectedIds = selectedBeaconChannels,
                        onToggle = viewModel::toggleBeaconChannel,
                        enabled = true,
                    )
                }
            }

            // Dwell time picker
            item(key = "dwell_picker") {
                DwellTimePicker(selectedMinutes = dwellMinutes, onMinuteSelect = viewModel::setDwellDuration)
            }

            // Keep awake toggle
            item(key = "keep_awake_toggle") {
                IosSection {
                    SwitchPreference(
                        title = stringResource(Res.string.discovery_keep_screen_awake),
                        summary = stringResource(Res.string.discovery_keep_screen_awake_description),
                        checked = keepScreenAwake,
                        enabled = true,
                        onCheckedChange = { keepScreenAwake = it },
                    )
                }
            }
        }

        // Scan progress section
        if (isScanning) {
            item(key = "scan_progress") { ScanProgressSection(scanState = scanState) }
        }

        item(key = "scan_button") {
            ScanButton(
                scanState = scanState,
                isConnected = isConnected,
                hasPresetsSelected = selectedPresets.isNotEmpty() || selectedBeaconChannels.isNotEmpty(),
                is24GhzUnsupported = isLora24Region && is24GhzBlocked,
                onStart = viewModel::startScan,
                onStop = viewModel::stopScan,
                modifier = Modifier.padding(start = IosInset, end = IosInset, top = 24.dp),
            )
        }
    }
}

/** The page's opening, as on iOS: the radio-waves symbol, what the screen is for, and how. */
@Composable
private fun DiscoveryHero() {
    val c = Ios.colors
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(IosIcons.RadiowavesLeftRight, contentDescription = null, tint = c.tint, modifier = Modifier.size(64.dp))
        Text(
            stringResource(Res.string.pangea_discovery_hero_title),
            style = Ios.type.title3.copy(fontWeight = FontWeight.SemiBold),
            color = c.label,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(Res.string.pangea_discovery_hero_text),
            style = Ios.type.subheadline,
            color = c.secondaryLabel,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ConnectionWarningCard(modifier: Modifier = Modifier) {
    val warningDescription = stringResource(Res.string.discovery_connection_warning)
    IosSection(
        modifier =
        modifier.semantics(mergeDescendants = true) {
            contentDescription = warningDescription
            liveRegion = LiveRegionMode.Polite
        },
    ) {
        IosRow(
            title = stringResource(Res.string.discovery_not_connected),
            subtitle = stringResource(Res.string.discovery_not_connected_description),
            icon = IosIcons.WarningFill,
            iconTint = Ios.colors.systemOrange,
            titleColor = Ios.colors.systemOrange,
        )
    }
}

/** "Dwell Time Per Preset": how long each preset is listened to, as a menu row. */
@Composable
private fun DwellTimePicker(selectedMinutes: Int, onMinuteSelect: (Int) -> Unit) {
    IosSection(
        headerContent = {
            DiscoverySectionHeader(
                IosIcons.Clock,
                stringResource(Res.string.pangea_discovery_dwell_header),
                inset = 12.dp,
            )
        },
    ) {
        DropDownPreference(
            title = stringResource(Res.string.pangea_discovery_dwell_duration),
            summary = stringResource(Res.string.discovery_dwell_time_description),
            enabled = true,
            items = DWELL_OPTIONS.map { it to "$it min" },
            selectedItem = selectedMinutes,
            onItemSelected = onMinuteSelect,
        )
    }
}

@Composable
private fun ScanButton(
    scanState: DiscoveryScanState,
    isConnected: Boolean,
    hasPresetsSelected: Boolean,
    is24GhzUnsupported: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isScanning = scanState !is DiscoveryScanState.Idle
    if (isScanning) {
        IosButton(
            text = stringResource(Res.string.discovery_stop_scan),
            onClick = onStop,
            style = IosButtonStyle.Tinted,
            large = true,
            icon = IosIcons.Xmark,
            modifier = modifier.fillMaxWidth(),
        )
    } else {
        val isEnabled = isConnected && hasPresetsSelected && !is24GhzUnsupported
        val disabledReason =
            when {
                !isConnected -> stringResource(Res.string.discovery_start_scan_reason_not_connected)
                is24GhzUnsupported -> stringResource(Res.string.discovery_start_scan_reason_24ghz_unsupported)
                !hasPresetsSelected -> stringResource(Res.string.discovery_start_scan_reason_no_presets)
                else -> ""
            }
        val disabledDescription = stringResource(Res.string.discovery_start_scan_disabled, disabledReason)
        val buttonModifier =
            if (!isEnabled) {
                Modifier.fillMaxWidth().semantics { contentDescription = disabledDescription }
            } else {
                Modifier.fillMaxWidth()
            }
        Column(modifier = modifier) {
            IosButton(
                text = stringResource(Res.string.discovery_start_scan),
                onClick = onStart,
                enabled = isEnabled,
                large = true,
                modifier = buttonModifier,
            )
            if (!isEnabled && disabledReason.isNotEmpty()) {
                Text(
                    text = disabledReason,
                    style = Ios.type.footnote,
                    color = Ios.colors.secondaryLabel,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ScanProgressSection(scanState: DiscoveryScanState, modifier: Modifier = Modifier) {
    IosSection(header = stringResource(Res.string.discovery_scan_progress), modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            when (scanState) {
                is DiscoveryScanState.Dwell -> {
                    DwellProgressIndicator(
                        presetName = scanState.presetName,
                        remainingSeconds = scanState.remainingSeconds,
                        totalSeconds = scanState.totalSeconds,
                    )
                }

                is DiscoveryScanState.Complete,
                is DiscoveryScanState.Idle,
                -> Unit

                else -> ScanStateLine(scanState)
            }
        }
    }
}

/** A step of the scan that is a line of text rather than a countdown. */
@Composable
private fun ScanStateLine(scanState: DiscoveryScanState) {
    val c = Ios.colors
    val (text, color) =
        when (scanState) {
            is DiscoveryScanState.Preparing -> stringResource(Res.string.discovery_preparing) to c.label

            is DiscoveryScanState.Shifting ->
                stringResource(Res.string.discovery_shifting_to, scanState.presetName) to c.label

            is DiscoveryScanState.Reconnecting ->
                stringResource(Res.string.discovery_reconnecting, scanState.presetName) to c.label

            is DiscoveryScanState.Analysis -> stringResource(Res.string.discovery_analysing_results) to c.label

            is DiscoveryScanState.Restoring -> stringResource(Res.string.discovery_restoring_preset) to c.label

            is DiscoveryScanState.Cancelling -> stringResource(Res.string.discovery_cancelling_scan) to c.label

            is DiscoveryScanState.Failed ->
                stringResource(Res.string.discovery_scan_failed, scanState.reason) to c.systemRed

            else -> return
        }
    Text(text = text, style = Ios.type.body, color = color)
}
