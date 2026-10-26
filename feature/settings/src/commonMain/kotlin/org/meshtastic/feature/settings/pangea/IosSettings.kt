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
@file:Suppress("MagicNumber")

package org.meshtastic.feature.settings.pangea

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.PowerMode
import org.meshtastic.core.model.pangea.RadioAutoConfig
import org.meshtastic.core.navigation.ConnectionsRoute
import org.meshtastic.core.navigation.FirmwareRoute
import org.meshtastic.core.navigation.Route
import org.meshtastic.core.navigation.SettingsRoute
import org.meshtastic.core.repository.PowerModeManager
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.about_pangea
import org.meshtastic.core.resources.ambient_lighting
import org.meshtastic.core.resources.bottom_nav_settings
import org.meshtastic.core.resources.display
import org.meshtastic.core.resources.expert_mode
import org.meshtastic.core.resources.expert_mode_summary
import org.meshtastic.core.resources.modules
import org.meshtastic.core.resources.pangea_app_settings
import org.meshtastic.core.resources.pangea_appearance
import org.meshtastic.core.resources.pangea_appearance_footer
import org.meshtastic.core.resources.pangea_battery
import org.meshtastic.core.resources.pangea_configure
import org.meshtastic.core.resources.pangea_connected_node
import org.meshtastic.core.resources.pangea_device_section
import org.meshtastic.core.resources.pangea_expert_mode_footer
import org.meshtastic.core.resources.pangea_firmware_updates
import org.meshtastic.core.resources.pangea_forget_radio
import org.meshtastic.core.resources.pangea_forget_radio_message
import org.meshtastic.core.resources.pangea_forget_radio_title
import org.meshtastic.core.resources.pangea_match_device
import org.meshtastic.core.resources.pangea_my_radio
import org.meshtastic.core.resources.pangea_no_radio_paired
import org.meshtastic.core.resources.pangea_open_settings
import org.meshtastic.core.resources.pangea_pair_different_radio
import org.meshtastic.core.resources.pangea_pair_radio
import org.meshtastic.core.resources.pangea_profile_row_subtitle
import org.meshtastic.core.resources.pangea_radio_connected
import org.meshtastic.core.resources.pangea_radio_connecting
import org.meshtastic.core.resources.pangea_radio_not_connected
import org.meshtastic.core.resources.pangea_route_recorder
import org.meshtastic.core.resources.pangea_routes
import org.meshtastic.core.resources.pangea_setup_profile
import org.meshtastic.core.resources.pangea_you
import org.meshtastic.core.resources.pangea_you_footer
import org.meshtastic.core.resources.power_mode
import org.meshtastic.core.resources.theme_dark
import org.meshtastic.core.resources.theme_light
import org.meshtastic.core.resources.user
import org.meshtastic.core.ui.component.AvatarPalette
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.component.PangeaAvatar
import org.meshtastic.core.ui.component.PangeaLogoButton
import org.meshtastic.core.ui.component.rememberAvatarBitmap
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosAccessory
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosLargeTitleScreen
import org.meshtastic.core.ui.ios.IosMenu
import org.meshtastic.core.ui.ios.IosMenuItem
import org.meshtastic.core.ui.ios.IosPushedScreen
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.pangea.MyProfileViewModel
import org.meshtastic.core.ui.pangea.ProfileEditorSheet
import org.meshtastic.core.ui.theme.SemanticColors
import org.meshtastic.core.ui.util.LocalOpenPairRadio
import org.meshtastic.core.ui.util.icon
import org.meshtastic.core.ui.util.labelRes
import org.meshtastic.core.ui.util.rememberOpenAppSettings
import org.meshtastic.core.ui.util.summaryRes
import org.meshtastic.feature.settings.component.ThemeOption

/**
 * Settings in standard mode, drawn like the Pangea iPhone app: **You** (your profile), **My Radio**, About Pangea, App
 * Settings and Firmware Updates, **Configure** (the connected radio's User and Display screens, and Ambient Lighting
 * when it has LEDs), and the **Expert Mode** switch at the bottom.
 */
@Suppress("LongMethod")
@Composable
fun IosSettingsScreen(
    connectedNodeName: String?,
    showAmbientLighting: Boolean,
    expertModeEnabled: Boolean,
    onExpertModeChange: (Boolean) -> Unit,
    onNavigate: (Route) -> Unit,
    modifier: Modifier = Modifier,
    profileViewModel: MyProfileViewModel = koinViewModel(),
) {
    val state by profileViewModel.state.collectAsStateWithLifecycle()
    val connection by profileViewModel.connectionState.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf(false) }
    var confirmForget by remember { mutableStateOf(false) }
    val name = state.profile.displayName.ifEmpty { state.radioName }
    val paired = state.code != null
    val c = Ios.colors
    val openPairRadio = LocalOpenPairRadio.current

    IosLargeTitleScreen(
        title = stringResource(Res.string.bottom_nav_settings),
        modifier = modifier,
        leading = { PangeaLogoButton() },
    ) {
        item(key = "you") {
            IosSection(
                header = stringResource(Res.string.pangea_you),
                footer = stringResource(Res.string.pangea_you_footer),
            ) {
                IosRow(
                    title = name.ifEmpty { stringResource(Res.string.pangea_setup_profile) },
                    subtitle = stringResource(Res.string.pangea_profile_row_subtitle),
                    subtitleColor = c.tint.copy(alpha = 0.7f),
                    titleStyle = Ios.type.title3,
                    leading = {
                        PangeaAvatar(
                            photo = rememberAvatarBitmap(state.photo),
                            emoji = state.profile.emoji,
                            colorIndex = state.profile.color,
                            initials = RadioAutoConfig.shortName(name),
                            fallbackColor = AvatarPalette.color(state.profile.color),
                            size = 54.dp,
                            modifier = Modifier.padding(start = 2.dp),
                        )
                    },
                    leadingWidth = 82.dp,
                    accessory = IosAccessory.Chevron,
                    onClick = { editing = true },
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        }
        item(key = "radio") {
            IosSection(header = stringResource(Res.string.pangea_my_radio)) {
                IosRow(
                    title = state.radioName.ifEmpty { stringResource(Res.string.pangea_no_radio_paired) },
                    subtitle =
                    stringResource(
                        when (connection) {
                            ConnectionState.Connected -> Res.string.pangea_radio_connected
                            ConnectionState.Connecting -> Res.string.pangea_radio_connecting
                            else -> Res.string.pangea_radio_not_connected
                        },
                    ),
                    icon = IosIcons.Antenna,
                )
                IosRow(
                    title =
                    stringResource(
                        if (paired) Res.string.pangea_pair_different_radio else Res.string.pangea_pair_radio,
                    ),
                    titleColor = c.tint,
                    onClick = { openPairRadio?.invoke() ?: onNavigate(ConnectionsRoute.Connections()) },
                )
                if (paired) {
                    IosRow(
                        title = stringResource(Res.string.pangea_forget_radio),
                        titleColor = c.systemRed,
                        onClick = { confirmForget = true },
                    )
                }
            }
        }
        item(key = "app") {
            IosSection {
                IosRow(
                    title = stringResource(Res.string.about_pangea),
                    icon = IosIcons.QuestionSquare,
                    accessory = IosAccessory.Chevron,
                    onClick = { onNavigate(SettingsRoute.About) },
                )
                IosRow(
                    title = stringResource(Res.string.pangea_app_settings),
                    icon = IosIcons.Gear,
                    accessory = IosAccessory.Chevron,
                    onClick = { onNavigate(SettingsRoute.AppSettings) },
                )
                IosRow(
                    title = stringResource(Res.string.pangea_routes),
                    icon = IosIcons.RoadLanes,
                    accessory = IosAccessory.Chevron,
                    onClick = { onNavigate(SettingsRoute.Routes) },
                )
                if (canRecordRoutes) {
                    IosRow(
                        title = stringResource(Res.string.pangea_route_recorder),
                        icon = IosIcons.RecordCircle,
                        // The iPhone app's red here is Pangea's Error 500, not systemRed.
                        iconTint = SemanticColors.Error,
                        accessory = IosAccessory.Chevron,
                        onClick = { onNavigate(SettingsRoute.RouteRecorder) },
                    )
                }
                IosRow(
                    title = stringResource(Res.string.pangea_firmware_updates),
                    icon = IosIcons.ArrowsUpDownSquare,
                    accessory = IosAccessory.Chevron,
                    onClick = { onNavigate(FirmwareRoute.FirmwareGraph) },
                )
            }
        }
        if (connectedNodeName != null) {
            item(key = "configure") {
                IosSection(header = stringResource(Res.string.pangea_configure)) {
                    IosRow(title = stringResource(Res.string.pangea_connected_node, connectedNodeName))
                }
                IosSection(header = stringResource(Res.string.pangea_device_section)) {
                    IosRow(
                        title = stringResource(Res.string.user),
                        icon = IosIcons.PersonSquareFill,
                        accessory = IosAccessory.Chevron,
                        onClick = { onNavigate(SettingsRoute.User) },
                    )
                    IosRow(
                        title = stringResource(Res.string.display),
                        icon = IosIcons.DisplayMonitor,
                        accessory = IosAccessory.Chevron,
                        onClick = { onNavigate(SettingsRoute.Display) },
                    )
                }
                if (showAmbientLighting) {
                    IosSection(header = stringResource(Res.string.modules)) {
                        IosRow(
                            title = stringResource(Res.string.ambient_lighting),
                            icon = IosIcons.Rays,
                            accessory = IosAccessory.Chevron,
                            onClick = { onNavigate(SettingsRoute.AmbientLighting) },
                        )
                    }
                }
            }
        }
        item(key = "expert") {
            IosSection(footer = stringResource(Res.string.pangea_expert_mode_footer)) {
                IosRow(
                    title = stringResource(Res.string.expert_mode),
                    icon = IosIcons.WandStars,
                    accessory = IosAccessory.Switch(expertModeEnabled, onExpertModeChange),
                )
            }
        }
    }

    if (editing) ProfileEditorSheet(state, onSave = profileViewModel::save, onDismiss = { editing = false })
    if (confirmForget) {
        MeshtasticDialog(
            titleRes = Res.string.pangea_forget_radio_title,
            messageRes = Res.string.pangea_forget_radio_message,
            confirmTextRes = Res.string.pangea_forget_radio,
            confirmDestructive = true,
            onConfirm = {
                confirmForget = false
                profileViewModel.forgetRadio()
            },
            onDismiss = { confirmForget = false },
        )
    }
}

/**
 * App Settings, as on the Pangea iPhone app: **Appearance** (Match Device, Light or Dark), the **Expert Mode** switch,
 * and **Battery** (Power Mode).
 */
@Suppress("LongMethod")
@Composable
fun IosAppSettingsScreen(
    theme: Int,
    onThemeChange: (Int) -> Unit,
    expertModeEnabled: Boolean,
    onExpertModeChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    extras: (@Composable () -> Unit)? = null,
) {
    val powerModeManager: PowerModeManager = koinInject()
    val powerMode by powerModeManager.selectedMode.collectAsState()
    val c = Ios.colors
    IosPushedScreen(
        title = stringResource(Res.string.pangea_app_settings),
        onBack = onBack,
        largeTitle = true,
        modifier = modifier,
    ) {
        item(key = "appearance") {
            IosSection(
                header = stringResource(Res.string.pangea_appearance),
                footer = stringResource(Res.string.pangea_appearance_footer),
            ) {
                listOf(
                    ThemeOption.SYSTEM to Res.string.pangea_match_device,
                    ThemeOption.LIGHT to Res.string.theme_light,
                    ThemeOption.DARK to Res.string.theme_dark,
                )
                    .forEach { (option, label) ->
                        IosRow(
                            title = stringResource(label),
                            accessory = if (theme == option.mode) IosAccessory.Checkmark else IosAccessory.None,
                            onClick = { onThemeChange(option.mode) },
                        )
                    }
            }
        }
        item(key = "app") {
            IosSection(
                header = stringResource(Res.string.pangea_app_settings),
                footer = stringResource(Res.string.expert_mode_summary),
            ) {
                // iOS: Open Settings, then Expert Mode. (Its PKI Administration switch has no Android counterpart:
                // remote administration is always available in Expert Mode.)
                val openAppSettings = rememberOpenAppSettings()
                IosRow(
                    title = stringResource(Res.string.pangea_open_settings),
                    icon = IosIcons.Gear,
                    titleColor = c.tint,
                    onClick = openAppSettings,
                )
                IosRow(
                    title = stringResource(Res.string.expert_mode),
                    icon = IosIcons.WandStars,
                    accessory = IosAccessory.Switch(expertModeEnabled, onExpertModeChange),
                )
            }
        }
        item(key = "battery") {
            IosSection(
                header = stringResource(Res.string.pangea_battery),
                footer = stringResource(powerMode.summaryRes),
            ) {
                var menu by remember { mutableStateOf(false) }
                IosRow(
                    title = stringResource(Res.string.power_mode),
                    icon = IosIcons.BatteryFull,
                    accessory =
                    IosAccessory.Custom {
                        Box {
                            // iOS's menu picker: the value in grey with up/down chevrons.
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    stringResource(powerMode.labelRes),
                                    style = Ios.type.body,
                                    color = c.secondaryLabel,
                                )
                                Icon(
                                    IosIcons.ChevronUpDown,
                                    contentDescription = null,
                                    tint = c.secondaryLabel,
                                    modifier = Modifier.padding(start = 4.dp).size(16.dp),
                                )
                            }
                            IosMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                PowerMode.entries.forEach { mode ->
                                    IosMenuItem(
                                        text = stringResource(mode.labelRes),
                                        icon = mode.icon,
                                        onClick = {
                                            menu = false
                                            powerModeManager.setSelectedMode(mode)
                                        },
                                    )
                                }
                            }
                        }
                    },
                    onClick = { menu = true },
                )
            }
        }
        if (extras != null) {
            item(key = "platform") { extras() }
        }
    }
}
