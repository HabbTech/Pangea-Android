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

package org.meshtastic.feature.settings.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eygraber.uri.toKmpUri
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.qualifier.named
import org.meshtastic.core.common.di.GOOGLE_SERVICES_AVAILABLE
import org.meshtastic.core.common.util.UnitsOverride
import org.meshtastic.core.navigation.Route
import org.meshtastic.core.navigation.SettingsRoute
import org.meshtastic.core.navigation.WifiProvisionRoute
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.app_functions_settings
import org.meshtastic.core.resources.app_functions_settings_summary
import org.meshtastic.core.resources.app_settings
import org.meshtastic.core.resources.device_links
import org.meshtastic.core.resources.filter_settings
import org.meshtastic.core.resources.node_layout_section_title
import org.meshtastic.core.resources.pangea_app_files
import org.meshtastic.core.resources.pangea_developers
import org.meshtastic.core.resources.pangea_tools
import org.meshtastic.core.resources.preferences_language
import org.meshtastic.core.resources.wifi_devices
import org.meshtastic.core.ui.component.ListItem
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.icon.Device
import org.meshtastic.core.ui.icon.FilterList
import org.meshtastic.core.ui.icon.List
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.SettingsRemote
import org.meshtastic.core.ui.icon.Wifi
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.util.LocalNfcScannerSupported
import org.meshtastic.core.ui.util.isBluetoothSupported
import org.meshtastic.feature.settings.SettingsViewModel
import org.meshtastic.feature.settings.component.AppInfoSection
import org.meshtastic.feature.settings.component.AppearanceSettingsContent
import org.meshtastic.feature.settings.component.ExpressiveSection
import org.meshtastic.feature.settings.component.MessageSignalDetailsSetting
import org.meshtastic.feature.settings.component.PermissionsSettingsContent
import org.meshtastic.feature.settings.component.PersistenceSettingsContent
import org.meshtastic.feature.settings.component.PrivacySettingsContent
import org.meshtastic.feature.settings.component.UnitsOption
import org.meshtastic.feature.settings.component.UnitsPickerDialog
import org.meshtastic.feature.settings.radio.RadioConfigViewModel
import org.meshtastic.feature.settings.util.LanguageUtils
import org.meshtastic.feature.settings.util.LanguageUtils.languageMap

/**
 * Android's own app settings, which the iPhone app has no rows for: permissions, privacy, language and units, the node
 * database and exports, node layout, Wi-Fi provisioning, filters, and the version. They sit under App Settings in
 * Expert Mode, below iOS's sections, rather than at the foot of Settings.
 */
@Suppress("LongMethod")
@Composable
actual fun AppSettingsExtras(onNavigate: (Route) -> Unit, modifier: Modifier) {
    val settingsViewModel: SettingsViewModel = koinViewModel()
    val viewModel: RadioConfigViewModel = koinViewModel()
    val appFunctionsAvailable: Boolean = koinInject(qualifier = named(GOOGLE_SERVICES_AVAILABLE))
    val hiddenFeaturesUnlocked by settingsViewModel.hiddenFeaturesUnlocked.collectAsStateWithLifecycle()
    val showFullMessageTimestamps by settingsViewModel.showFullMessageTimestamps.collectAsStateWithLifecycle()
    val showSignalDetails by settingsViewModel.showMessageSignalDetails.collectAsStateWithLifecycle()
    val ourNode by settingsViewModel.ourNodeInfo.collectAsStateWithLifecycle()

    var showLanguagePickerDialog by rememberSaveable { mutableStateOf(false) }
    if (showLanguagePickerDialog) {
        LanguagePickerDialog(
            onDismiss = { showLanguagePickerDialog = false },
            onSelect = { languageTag -> settingsViewModel.setLocale(languageTag) },
        )
    }
    var showUnitsPickerDialog by rememberSaveable { mutableStateOf(false) }
    val unitsOverride = UnitsOverride.fromValue(settingsViewModel.unitsOverride.collectAsStateWithLifecycle().value)
    if (showUnitsPickerDialog) {
        UnitsPickerDialog(
            current = unitsOverride,
            onClickUnits = { settingsViewModel.setUnitsOverride(it) },
            onDismiss = { showUnitsPickerDialog = false },
        )
    }

    Column(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Onboarding runs once, so this is the only place a user who skipped or declined a permission can find
        // their way back to it.
        PermissionsSettingsContent()

        ExpressiveSection(title = stringResource(Res.string.app_settings)) {
            PrivacySettingsContent(
                analyticsAvailable = appFunctionsAvailable,
                analyticsEnabled = viewModel.analyticsAllowedFlow.collectAsStateWithLifecycle(true).value,
                onToggleAnalytics = { viewModel.toggleAnalyticsAllowed() },
                provideLocation = settingsViewModel.provideLocation.collectAsStateWithLifecycle().value,
                onToggleLocation = { settingsViewModel.setProvideLocation(it) },
                homoglyphEnabled = viewModel.homoglyphEncodingEnabledFlow.collectAsStateWithLifecycle(false).value,
                onToggleHomoglyph = { viewModel.toggleHomoglyphCharactersEncodingEnabled() },
                startProvideLocation = { settingsViewModel.startProvidingLocation() },
                stopProvideLocation = { settingsViewModel.stopProvidingLocation() },
            )
            // The theme is iOS's Appearance section above.
            AppearanceSettingsContent(
                showFullMessageTimestamps = showFullMessageTimestamps,
                onShowFullMessageTimestampsChange = settingsViewModel::setShowFullMessageTimestamps,
                onShowLanguagePicker = { showLanguagePickerDialog = true },
                onShowThemePicker = null,
                unitsSummary = stringResource(UnitsOption.entries.first { it.override == unitsOverride }.label),
                onShowUnitsPicker = { showUnitsPickerDialog = true },
            )
            MessageSignalDetailsSetting(
                checked = showSignalDetails,
                onCheckedChange = settingsViewModel::setShowMessageSignalDetails,
            )
            PersistenceSettingsContent(
                cacheLimit = settingsViewModel.dbCacheLimit.collectAsStateWithLifecycle().value,
                onCheckCacheLimitEvictionCount = { settingsViewModel.cachedDeviceCountExceeding(it) },
                onSetCacheLimit = { settingsViewModel.setDbCacheLimit(it) },
                nodeShortName = ourNode?.user?.short_name ?: "",
                onExportData = { settingsViewModel.saveDataCsv(it.toKmpUri()) },
                onExportNodeDb = { settingsViewModel.saveNodeDbJson(it) },
            )
            ListItem(text = stringResource(Res.string.node_layout_section_title), leadingIcon = MeshtasticIcons.List) {
                onNavigate(SettingsRoute.NodeList)
            }
            // Wi-Fi provisioning reaches the device over BLE.
            if (isBluetoothSupported()) {
                ListItem(text = stringResource(Res.string.wifi_devices), leadingIcon = MeshtasticIcons.Wifi) {
                    onNavigate(WifiProvisionRoute.WifiProvision())
                }
            }
            ListItem(text = stringResource(Res.string.filter_settings), leadingIcon = MeshtasticIcons.FilterList) {
                onNavigate(SettingsRoute.FilterSettings)
            }
            if (appFunctionsAvailable) {
                ListItem(
                    text = stringResource(Res.string.app_functions_settings),
                    supportingText = stringResource(Res.string.app_functions_settings_summary),
                    leadingIcon = MeshtasticIcons.SettingsRemote,
                ) {
                    onNavigate(SettingsRoute.AppFunctionsSettings)
                }
            }
        }

        AppInfoSection(
            appVersionName = settingsViewModel.appVersionName,
            hiddenFeaturesUnlocked = hiddenFeaturesUnlocked,
            onUnlockHiddenFeatures = { settingsViewModel.unlockHiddenFeatures() },
            onShowAppIntro = { settingsViewModel.showAppIntro() },
            onNavigateToAbout = { onNavigate(SettingsRoute.About) },
        )

        ExpressiveSection(title = stringResource(Res.string.pangea_developers)) {
            ListItem(text = stringResource(Res.string.device_links), leadingIcon = MeshtasticIcons.Device) {
                onNavigate(SettingsRoute.DeviceLinks)
            }
            ListItem(text = stringResource(Res.string.pangea_app_files), leadingIcon = IosIcons.Folder) {
                onNavigate(SettingsRoute.AppFiles)
            }
            // iOS lists Tools only where its NFC reader is available.
            if (LocalNfcScannerSupported.current) {
                ListItem(text = stringResource(Res.string.pangea_tools), leadingIcon = IosIcons.Hammer) {
                    onNavigate(SettingsRoute.Tools)
                }
            }
        }
    }
}

@Composable
private fun LanguagePickerDialog(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    MeshtasticDialog(
        title = stringResource(Res.string.preferences_language),
        onDismiss = onDismiss,
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                languageMap().forEach { (languageTag, languageName) ->
                    ListItem(text = languageName, trailingIcon = null) {
                        LanguageUtils.setAppLocale(languageTag)
                        onSelect(languageTag)
                        onDismiss()
                    }
                }
            }
        },
    )
}
