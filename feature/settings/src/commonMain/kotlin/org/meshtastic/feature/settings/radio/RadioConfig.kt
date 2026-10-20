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
package org.meshtastic.feature.settings.radio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.navigation.ChannelsRoute
import org.meshtastic.core.navigation.FirmwareRoute
import org.meshtastic.core.navigation.Route
import org.meshtastic.core.navigation.SettingsRoute
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.administration
import org.meshtastic.core.resources.advanced_title
import org.meshtastic.core.resources.backup_restore
import org.meshtastic.core.resources.clean_node_database_title
import org.meshtastic.core.resources.device
import org.meshtastic.core.resources.expert_mode
import org.meshtastic.core.resources.factory_reset
import org.meshtastic.core.resources.firmware_update_title
import org.meshtastic.core.resources.ic_power_settings_new
import org.meshtastic.core.resources.ic_restart_alt
import org.meshtastic.core.resources.ic_restore
import org.meshtastic.core.resources.ic_schedule
import org.meshtastic.core.resources.ic_storage
import org.meshtastic.core.resources.logs
import org.meshtastic.core.resources.message_device_managed
import org.meshtastic.core.resources.modules
import org.meshtastic.core.resources.nodedb_reset
import org.meshtastic.core.resources.pangea_canned_messages
import org.meshtastic.core.resources.pangea_device_configuration
import org.meshtastic.core.resources.pangea_export_configuration
import org.meshtastic.core.resources.pangea_import_configuration
import org.meshtastic.core.resources.pangea_logging
import org.meshtastic.core.resources.pangea_module_configuration
import org.meshtastic.core.resources.pangea_pax_counter
import org.meshtastic.core.resources.pangea_radio_configuration
import org.meshtastic.core.resources.pangea_share_qr_code
import org.meshtastic.core.resources.radio
import org.meshtastic.core.resources.reboot
import org.meshtastic.core.resources.set_time
import org.meshtastic.core.resources.shutdown
import org.meshtastic.core.resources.tak_server
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.icon.AdminPanelSettings
import org.meshtastic.core.ui.icon.ChevronRight
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.feature.settings.component.ExpressiveSection
import org.meshtastic.feature.settings.component.expertModeCaptionColor
import org.meshtastic.feature.settings.navigation.ConfigRoute
import org.meshtastic.feature.settings.navigation.ModuleRoute

/**
 * The radio's settings as the Pangea iPhone app lists them: Radio, Device and Module Configuration with every setting
 * inline (an "Expert Mode" caption under the expert-only ones), then Backup & Restore, Logging, and Android's own Clean
 * Node Database under Advanced.
 */
@Composable
fun RadioConfigItemList(
    state: RadioConfigState,
    isManaged: Boolean,
    expertModeEnabled: Boolean,
    isOtaCapable: Boolean = false,
    onRouteClick: (Enum<*>) -> Unit = {},
    onImport: () -> Unit = {},
    onExport: () -> Unit = {},
    expertModeToggle: (@Composable () -> Unit)? = null,
    onNavigate: (Route) -> Unit,
) {
    val enabled = state.connected && !state.responseState.isWaiting() && !isManaged
    val modules =
        ModuleRoute.filterExcludedFrom(state.metadata, state.radioConfig.device?.role, expertModeEnabled).sortedBy {
            IosModuleOrder.indexOf(it)
        }

    // iOS's names: "Radio Configuration" and so on in Expert Mode, "Radio" in standard mode.
    val radioTitle = stringResource(if (expertModeEnabled) Res.string.pangea_radio_configuration else Res.string.radio)
    val deviceTitle =
        stringResource(if (expertModeEnabled) Res.string.pangea_device_configuration else Res.string.device)
    val moduleTitle =
        stringResource(if (expertModeEnabled) Res.string.pangea_module_configuration else Res.string.modules)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ExpressiveSection(title = radioTitle) {
            if (isManaged) {
                ManagedMessage()
            }
            (ConfigRoute.radioConfigRoutes(expertModeEnabled) - ConfigRoute.USER)
                .sortedBy { IosRadioOrder.indexOf(it) }
                .forEach { route -> RouteItem(route, enabled) { onRouteClick(route) } }
            if (state.isLocal) {
                ListItem(text = stringResource(Res.string.pangea_share_qr_code), leadingIcon = IosIcons.QrCode) {
                    onNavigate(ChannelsRoute.Channels)
                }
            }
        }
        ExpressiveSection(title = deviceTitle) {
            (listOf(ConfigRoute.USER) + ConfigRoute.deviceConfigRoutes(state.metadata, expertModeEnabled))
                .sortedBy { IosDeviceOrder.indexOf(it) }
                .forEach { route -> RouteItem(route, enabled) { onRouteClick(route) } }
            if (expertModeEnabled) {
                AdministrationContent(enabled, onNavigate)
            }
        }
        ExpressiveSection(title = moduleTitle) {
            // iOS lists the app's TAK Server among the modules, after Store & Forward.
            val (beforeTak, rest) = modules.partition { IosModuleOrder.indexOf(it) <= TAK_SERVER_AFTER }
            beforeTak.forEach { route -> ModuleItem(route, enabled) { onRouteClick(route) } }
            if (state.isLocal && expertModeEnabled) {
                ListItem(
                    text = stringResource(Res.string.tak_server),
                    supportingText = stringResource(Res.string.expert_mode),
                    supportingTextColor = expertModeCaptionColor(),
                    leadingIcon = IosIcons.Target,
                    enabled = enabled,
                    onClick = { onNavigate(SettingsRoute.TakServer) },
                )
            }
            rest.forEach { route -> ModuleItem(route, enabled) { onRouteClick(route) } }
        }

        if (state.isLocal) {
            BackupRestoreSection(isManaged, enabled, onImport, onExport)
            expertModeToggle?.invoke()
            if (expertModeEnabled) {
                LoggingSection(onNavigate)
            }
            AdvancedSection(isManaged, isOtaCapable, enabled, expertModeEnabled, onNavigate)
        }
    }
}

/** iOS's order within each section (Settings.swift). */
private val IosRadioOrder = listOf(ConfigRoute.LORA, ConfigRoute.CHANNELS, ConfigRoute.SECURITY)

private val IosDeviceOrder =
    listOf(
        ConfigRoute.USER,
        ConfigRoute.BLUETOOTH,
        ConfigRoute.DEVICE,
        ConfigRoute.DISPLAY,
        ConfigRoute.NETWORK,
        ConfigRoute.POSITION,
        ConfigRoute.POWER,
    )

private val IosModuleOrder =
    listOf(
        ModuleRoute.AMBIENT_LIGHTING,
        ModuleRoute.AUDIO,
        ModuleRoute.CANNED_MESSAGE,
        ModuleRoute.DETECTION_SENSOR,
        ModuleRoute.MESH_BEACON,
        ModuleRoute.EXT_NOTIFICATION,
        ModuleRoute.MQTT,
        ModuleRoute.NEIGHBOR_INFO,
        ModuleRoute.RANGE_TEST,
        ModuleRoute.PAXCOUNTER,
        ModuleRoute.SERIAL,
        ModuleRoute.STORE_FORWARD,
        ModuleRoute.TAK,
        ModuleRoute.TELEMETRY,
        // Android only, so last.
        ModuleRoute.REMOTE_HARDWARE,
    )

private val TAK_SERVER_AFTER = IosModuleOrder.indexOf(ModuleRoute.STORE_FORWARD)

/** The SF Symbol iOS shows for each setting, drawn in [IosIcons]. */
private val ConfigRoute.iosIcon: ImageVector
    get() =
        when (this) {
            ConfigRoute.USER -> IosIcons.PersonSquareFill
            ConfigRoute.CHANNELS -> IosIcons.FibreChannel
            ConfigRoute.DEVICE -> IosIcons.FlipPhone
            ConfigRoute.POSITION -> IosIcons.Location
            ConfigRoute.POWER -> IosIcons.BoltFill
            ConfigRoute.NETWORK -> IosIcons.Globe
            ConfigRoute.DISPLAY -> IosIcons.DisplayMonitor
            ConfigRoute.LORA -> IosIcons.LoraWaves
            ConfigRoute.BLUETOOTH -> IosIcons.Antenna
            ConfigRoute.SECURITY -> IosIcons.LockShield
        }

private val ModuleRoute.iosIcon: ImageVector
    get() =
        when (this) {
            ModuleRoute.MQTT -> IosIcons.RadioWaves
            ModuleRoute.SERIAL -> IosIcons.Terminal
            ModuleRoute.EXT_NOTIFICATION -> IosIcons.Megaphone
            ModuleRoute.STORE_FORWARD -> IosIcons.EnvelopeBranch
            ModuleRoute.RANGE_TEST -> IosIcons.PointsConnected
            ModuleRoute.TELEMETRY -> IosIcons.ChartLine
            ModuleRoute.CANNED_MESSAGE -> IosIcons.ListRectFill
            ModuleRoute.AUDIO -> IosIcons.Waveform
            ModuleRoute.REMOTE_HARDWARE -> IosIcons.Cpu
            ModuleRoute.NEIGHBOR_INFO -> IosIcons.Globe
            ModuleRoute.AMBIENT_LIGHTING -> IosIcons.Rays
            ModuleRoute.DETECTION_SENSOR -> IosIcons.Sensor
            ModuleRoute.PAXCOUNTER -> IosIcons.FigureWalk
            ModuleRoute.TAK -> IosIcons.Target
            ModuleRoute.MESH_BEACON -> IosIcons.RadiowavesLeftRight
        }

/** iOS's names where they differ from Android's ("Canned Messages", "PAX Counter"). */
private val ModuleRoute.iosTitle: StringResource
    get() =
        when (this) {
            ModuleRoute.CANNED_MESSAGE -> Res.string.pangea_canned_messages
            ModuleRoute.PAXCOUNTER -> Res.string.pangea_pax_counter
            else -> title
        }

@Composable
private fun ColumnScope.RouteItem(route: ConfigRoute, enabled: Boolean, onClick: () -> Unit) {
    ListItem(
        text = stringResource(route.title),
        supportingText = if (route.isExpertOnly) stringResource(Res.string.expert_mode) else null,
        supportingTextColor = expertModeCaptionColor(),
        leadingIcon = route.iosIcon,
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
private fun ColumnScope.ModuleItem(route: ModuleRoute, enabled: Boolean, onClick: () -> Unit) {
    ListItem(
        text = stringResource(route.iosTitle),
        supportingText = if (route.isExpertOnly) stringResource(Res.string.expert_mode) else null,
        supportingTextColor = expertModeCaptionColor(),
        leadingIcon = route.iosIcon,
        enabled = enabled,
        onClick = onClick,
    )
}

@Composable
private fun BackupRestoreSection(isManaged: Boolean, enabled: Boolean, onImport: () -> Unit, onExport: () -> Unit) {
    val tint = Ios.colors.tint
    ExpressiveSection(title = stringResource(Res.string.backup_restore)) {
        if (isManaged) {
            ManagedMessage()
        }
        // Actions, so iOS draws them in the tint with no chevron.
        ListItem(
            text = stringResource(Res.string.pangea_import_configuration),
            textColor = tint,
            leadingIcon = IosIcons.ShareDown,
            trailingIcon = null,
            enabled = enabled,
            onClick = onImport,
        )
        ListItem(
            text = stringResource(Res.string.pangea_export_configuration),
            textColor = tint,
            leadingIcon = IosIcons.ShareUp,
            trailingIcon = null,
            enabled = enabled,
            onClick = onExport,
        )
    }
}

@Composable
private fun LoggingSection(onNavigate: (Route) -> Unit) {
    ExpressiveSection(title = stringResource(Res.string.pangea_logging)) {
        // Always enabled: the logs are the app's own, so no radio connection or managed-mode restriction applies.
        ListItem(
            text = stringResource(Res.string.logs),
            supportingText = stringResource(Res.string.expert_mode),
            supportingTextColor = expertModeCaptionColor(),
            leadingIcon = IosIcons.Scroll,
            onClick = { onNavigate(SettingsRoute.DebugPanel) },
        )
    }
}

@Composable
private fun ColumnScope.AdministrationContent(enabled: Boolean, onNavigate: (Route) -> Unit) {
    ListItem(
        text = stringResource(Res.string.administration),
        supportingText = stringResource(Res.string.expert_mode),
        supportingTextColor = expertModeCaptionColor(),
        leadingIcon = MeshtasticIcons.AdminPanelSettings,
        trailingIcon = MeshtasticIcons.ChevronRight,
        leadingIconTint = MaterialTheme.colorScheme.error,
        textColor = MaterialTheme.colorScheme.error,
        trailingIconTint = MaterialTheme.colorScheme.error,
        enabled = enabled,
    ) {
        onNavigate(SettingsRoute.Administration)
    }
}

/**
 * Android's extras with no iOS row: Firmware Update where the top of Settings has none (desktop), and Clean Node DB.
 */
@Composable
private fun AdvancedSection(
    isManaged: Boolean,
    isOtaCapable: Boolean,
    enabled: Boolean,
    expertModeEnabled: Boolean,
    onNavigate: (Route) -> Unit,
) {
    if (!isOtaCapable && !expertModeEnabled) return
    ExpressiveSection(title = stringResource(Res.string.advanced_title)) {
        if (isManaged) {
            ManagedMessage()
        }

        if (isOtaCapable) {
            ListItem(
                text = stringResource(Res.string.firmware_update_title),
                leadingIcon = IosIcons.ArrowsUpDownSquare,
                enabled = enabled,
                onClick = { onNavigate(FirmwareRoute.FirmwareUpdate) },
            )
        }

        if (expertModeEnabled) {
            ListItem(
                text = stringResource(Res.string.clean_node_database_title),
                supportingText = stringResource(Res.string.expert_mode),
                supportingTextColor = expertModeCaptionColor(),
                leadingIcon = IosIcons.Trash,
                enabled = enabled,
                onClick = { onNavigate(SettingsRoute.CleanNodeDb) },
            )
        }
    }
}

enum class AdminRoute(val icon: DrawableResource, val title: StringResource) {
    SET_TIME(Res.drawable.ic_schedule, Res.string.set_time),
    REBOOT(Res.drawable.ic_restart_alt, Res.string.reboot),
    SHUTDOWN(Res.drawable.ic_power_settings_new, Res.string.shutdown),
    FACTORY_RESET(Res.drawable.ic_restore, Res.string.factory_reset),
    NODEDB_RESET(Res.drawable.ic_storage, Res.string.nodedb_reset),
}

@Composable
private fun ManagedMessage() {
    Text(
        text = stringResource(Res.string.message_device_managed),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.error,
    )
}
