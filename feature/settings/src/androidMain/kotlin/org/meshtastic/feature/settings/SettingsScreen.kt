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
package org.meshtastic.feature.settings

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eygraber.uri.toKmpUri
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.common.util.nowMillis
import org.meshtastic.core.navigation.DiscoveryRoute
import org.meshtastic.core.navigation.FirmwareRoute
import org.meshtastic.core.navigation.Route
import org.meshtastic.core.navigation.SettingsRoute
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.about_pangea
import org.meshtastic.core.resources.bottom_nav_settings
import org.meshtastic.core.resources.discovery_local_mesh
import org.meshtastic.core.resources.expert_mode
import org.meshtastic.core.resources.export_configuration
import org.meshtastic.core.resources.help_and_documentation
import org.meshtastic.core.resources.import_configuration
import org.meshtastic.core.resources.pangea_app_settings
import org.meshtastic.core.resources.pangea_configure
import org.meshtastic.core.resources.pangea_connected_node
import org.meshtastic.core.resources.pangea_firmware_updates
import org.meshtastic.core.resources.remotely_administrating
import org.meshtastic.core.ui.component.BasicListItem
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.feature.settings.component.ExpressiveSection
import org.meshtastic.feature.settings.component.expertModeCaptionColor
import org.meshtastic.feature.settings.navigation.ConfigRoute
import org.meshtastic.feature.settings.navigation.ModuleRoute
import org.meshtastic.feature.settings.pangea.IosSettingsScreen
import org.meshtastic.feature.settings.pangea.PangeaHelpSheet
import org.meshtastic.feature.settings.radio.RadioConfigItemList
import org.meshtastic.feature.settings.radio.RadioConfigViewModel
import org.meshtastic.feature.settings.radio.component.EditDeviceProfileDialog
import org.meshtastic.feature.settings.search.SettingsSearchBar
import org.meshtastic.feature.settings.search.SettingsSearchViewModel
import org.meshtastic.feature.settings.util.deviceProfileExportFileName
import org.meshtastic.proto.DeviceProfile
import kotlin.time.Instant.Companion.fromEpochMilliseconds

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    viewModel: RadioConfigViewModel,
    onClickNodeChip: (Int) -> Unit = {},
    onNavigate: (Route) -> Unit = {},
    onBack: (() -> Unit)? = null,
) {
    val expertModeEnabled by settingsViewModel.expertModeEnabled.collectAsStateWithLifecycle()
    // Pangea: Help & Documentation opens Pangea's own guides (Pangea-Apple PangeaHelp).
    var showPangeaHelp by rememberSaveable { mutableStateOf(false) }
    if (showPangeaHelp) PangeaHelpSheet(expertMode = expertModeEnabled, onDismiss = { showPangeaHelp = false })
    val localConfig by settingsViewModel.localConfig.collectAsStateWithLifecycle()
    val ourNode by settingsViewModel.ourNodeInfo.collectAsStateWithLifecycle()
    val isConnected by settingsViewModel.isConnected.collectAsStateWithLifecycle(false)
    val destNode by viewModel.destNode.collectAsStateWithLifecycle()
    val state by viewModel.radioConfigState.collectAsStateWithLifecycle()

    var deviceProfile by remember { mutableStateOf<DeviceProfile?>(null) }
    var showEditDeviceProfileDialog by remember { mutableStateOf(false) }

    val importConfigLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                showEditDeviceProfileDialog = true
                it.data?.data?.let { uri ->
                    viewModel.importProfile(uri.toKmpUri()) { profile -> deviceProfile = profile }
                }
            }
        }

    val exportConfigLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.data?.let { uri -> viewModel.exportProfile(uri.toKmpUri(), deviceProfile!!) }
            }
        }

    if (showEditDeviceProfileDialog) {
        EditDeviceProfileDialog(
            title =
            if (deviceProfile != null) {
                stringResource(Res.string.import_configuration)
            } else {
                stringResource(Res.string.export_configuration)
            },
            deviceProfile = deviceProfile ?: viewModel.currentDeviceProfile,
            onConfirm = {
                showEditDeviceProfileDialog = false
                if (deviceProfile != null) {
                    viewModel.installProfile(it)
                } else {
                    deviceProfile = it
                    val dateStr =
                        fromEpochMilliseconds(nowMillis)
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .format(
                                LocalDateTime.Format {
                                    year()
                                    monthNumber()
                                    day()
                                },
                            )
                    val fileName = deviceProfileExportFileName(it.long_name, it.short_name, dateStr)
                    val intent =
                        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "application/*"
                            putExtra(Intent.EXTRA_TITLE, fileName)
                        }
                    exportConfigLauncher.launch(intent)
                }
            },
            onDismiss = {
                showEditDeviceProfileDialog = false
                deviceProfile = null
            },
        )
    }

    // Pangea standard mode: Settings as on the iPhone app. Expert Mode (and remote admin) keeps the full screen.
    if (!expertModeEnabled && state.isLocal) {
        IosSettingsScreen(
            connectedNodeName = ourNode?.user?.long_name?.takeIf { isConnected },
            showAmbientLighting =
            ModuleRoute.filterExcludedFrom(
                state.metadata,
                state.radioConfig.device?.role,
                expertModeEnabled = false,
            )
                .contains(ModuleRoute.AMBIENT_LIGHTING),
            expertModeEnabled = expertModeEnabled,
            onExpertModeChange = settingsViewModel::setExpertModeEnabled,
            onNavigate = onNavigate,
        )
        return
    }

    Scaffold(
        topBar = {
            // Show back arrow when remotely administering (caller supplies onBack and we're not on the local node).
            val showBack = onBack != null && !state.isLocal
            MainAppBar(
                title = stringResource(Res.string.bottom_nav_settings),
                // As on iOS, the local radio is named under Configure rather than under the title.
                subtitle =
                if (state.isLocal) {
                    null
                } else {
                    val remoteName = destNode?.user?.long_name ?: ""
                    stringResource(Res.string.remotely_administrating, remoteName)
                },
                ourNode = ourNode,
                // iOS's Settings has no radio chip at its root.
                showNodeChip = false,
                canNavigateUp = showBack,
                onNavigateUp = { onBack?.invoke() },
                actions = {},
                onClickChip = { node -> onClickNodeChip(node.num) },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsSearchBar(
                viewModel = koinViewModel<SettingsSearchViewModel>(),
                onNavigate = onNavigate,
                // This phone's own settings are hidden below while administering another node; search hides them too.
                includeAppLocal = state.isLocal,
            )

            // Pangea Expert Mode opens as the iPhone app's does: About, Help, App Settings and Firmware Updates, then
            // the
            // connected radio under Configure. My Profile and My Radio are standard mode's (Connect does pairing here).
            if (state.isLocal) {
                ExpressiveSection(title = "") {
                    ListItem(text = stringResource(Res.string.about_pangea), leadingIcon = IosIcons.QuestionSquare) {
                        onNavigate(SettingsRoute.About)
                    }
                    ListItem(
                        text = stringResource(Res.string.help_and_documentation),
                        supportingText = stringResource(Res.string.expert_mode),
                        supportingTextColor = expertModeCaptionColor(),
                        leadingIcon = IosIcons.DocText,
                    ) {
                        showPangeaHelp = true
                    }
                    ListItem(text = stringResource(Res.string.pangea_app_settings), leadingIcon = IosIcons.Gear) {
                        onNavigate(SettingsRoute.AppSettings)
                    }
                    ListItem(
                        text = stringResource(Res.string.discovery_local_mesh),
                        supportingText = stringResource(Res.string.expert_mode),
                        supportingTextColor = expertModeCaptionColor(),
                        leadingIcon = IosIcons.Antenna,
                    ) {
                        onNavigate(DiscoveryRoute.DiscoveryGraph)
                    }
                    ListItem(
                        text = stringResource(Res.string.pangea_firmware_updates),
                        leadingIcon = IosIcons.ArrowsUpDownSquare,
                    ) {
                        onNavigate(FirmwareRoute.FirmwareGraph)
                    }
                }
                ourNode
                    ?.user
                    ?.long_name
                    ?.takeIf { isConnected }
                    ?.let { name ->
                        ExpressiveSection(title = stringResource(Res.string.pangea_configure)) {
                            BasicListItem(text = stringResource(Res.string.pangea_connected_node, name))
                        }
                    }
            }

            RadioConfigItemList(
                state = state,
                isManaged = localConfig.security?.is_managed ?: false,
                expertModeEnabled = expertModeEnabled,
                // Firmware Updates sits at the top of Settings, as on iOS, so the list doesn't repeat it.
                onRouteClick = { route ->
                    val navRoute =
                        when (route) {
                            is ConfigRoute -> route.route
                            is ModuleRoute -> route.route
                            else -> null
                        }
                    navRoute?.let { onNavigate(it) }
                },
                onImport = {
                    viewModel.clearPacketResponse()
                    deviceProfile = null
                    val intent =
                        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "application/*"
                        }
                    importConfigLauncher.launch(intent)
                },
                onExport = {
                    viewModel.clearPacketResponse()
                    deviceProfile = null
                    showEditDeviceProfileDialog = true
                },
                onNavigate = onNavigate,
            )
        }
    }
}
