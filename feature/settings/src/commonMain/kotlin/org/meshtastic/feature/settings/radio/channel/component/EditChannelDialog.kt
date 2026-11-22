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
package org.meshtastic.feature.settings.radio.channel.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.Channel
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.channel_name
import org.meshtastic.core.resources.default_
import org.meshtastic.core.resources.downlink_enabled
import org.meshtastic.core.resources.mqtt
import org.meshtastic.core.resources.pangea_channel_details
import org.meshtastic.core.resources.position
import org.meshtastic.core.resources.psk
import org.meshtastic.core.resources.save
import org.meshtastic.core.resources.uplink_enabled
import org.meshtastic.core.ui.component.EditBase64Preference
import org.meshtastic.core.ui.component.EditTextPreference
import org.meshtastic.core.ui.component.PositionPrecisionPreference
import org.meshtastic.core.ui.component.PreferenceIcon
import org.meshtastic.core.ui.component.SwitchPreference
import org.meshtastic.core.ui.ios.IosBottomSheet
import org.meshtastic.core.ui.ios.IosButton
import org.meshtastic.core.ui.ios.IosButtonStyle
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.ios.iosRowSeparator
import org.meshtastic.proto.ChannelSettings
import org.meshtastic.proto.ModuleSettings

/**
 * The channel editor as the iPhone app's: a full-height sheet of three form sections (Channel Details, Position, MQTT)
 * and a tinted Save capsule under them. Swiping the sheet away, or Back, cancels.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun EditChannelDialog(
    channelSettings: ChannelSettings,
    onAddClick: (ChannelSettings, ChannelPskEditState) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    modemPresetName: String = stringResource(Res.string.default_),
    initialPskEditState: ChannelPskEditState = ChannelPskEditState(),
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    var channelInput by remember(channelSettings) { mutableStateOf(channelSettings) }
    var pskEditState by remember(channelSettings, initialPskEditState) { mutableStateOf(initialPskEditState) }

    IosBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        grouped = true,
    ) {
        // Full height, as iOS's `.large` sheet: the form scrolls, Save stays at the foot.
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(SHEET_HEIGHT_FRACTION).padding(bottom = 24.dp)) {
            Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                IosSection(header = stringResource(Res.string.pangea_channel_details)) {
                    EditTextPreference(
                        title = stringResource(Res.string.channel_name),
                        value =
                        if (isFocused) {
                            channelInput.name
                        } else {
                            channelInput.name.ifEmpty { modemPresetName }
                        },
                        maxSize = 11, // name max_size:12
                        enabled = true,
                        isError = false,
                        keyboardOptions =
                        KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        onValueChanged = {
                            val update = channelInput.applyChannelNameEdit(it, pskEditState)
                            channelInput = update.settings
                            pskEditState = update.pskEditState
                        },
                        onFocusChanged = { isFocused = it.isFocused },
                    )

                    EditBase64Preference(
                        title = stringResource(Res.string.psk),
                        value = channelInput.psk,
                        enabled = true,
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        onValueChange = {
                            val fullPsk = Channel(ChannelSettings.Builder().also { wb -> wb.psk = it }.build()).psk
                            if (fullPsk.size in setOf(0, 16, 32)) {
                                pskEditState =
                                    pskEditState.copy(generatedPskForName = false, pskExplicitlyEdited = true)
                                channelInput = channelInput.newBuilder().also { wb -> wb.psk = it }.build()
                            }
                        },
                        onGenerateKey = {
                            pskEditState = pskEditState.copy(generatedPskForName = false, pskExplicitlyEdited = true)
                            channelInput =
                                channelInput.newBuilder().also { wb -> wb.psk = Channel.getRandomKey() }.build()
                        },
                        modifier = Modifier.iosRowSeparator(),
                    )
                }

                IosSection(header = stringResource(Res.string.position)) {
                    val moduleSettings = channelInput.module_settings ?: ModuleSettings.Builder().build()
                    PositionPrecisionPreference(
                        enabled = true,
                        value = moduleSettings.position_precision,
                        onValueChanged = {
                            val updatedModule =
                                moduleSettings.newBuilder().also { wb -> wb.position_precision = it }.build()
                            channelInput =
                                channelInput.newBuilder().also { wb -> wb.module_settings = updatedModule }.build()
                        },
                    )
                }

                IosSection(header = stringResource(Res.string.mqtt)) {
                    PreferenceIcon(IosIcons.ArrowUpThin) {
                        SwitchPreference(
                            title = stringResource(Res.string.uplink_enabled),
                            checked = channelInput.uplink_enabled,
                            enabled = true,
                            onCheckedChange = {
                                channelInput = channelInput.newBuilder().also { wb -> wb.uplink_enabled = it }.build()
                            },
                        )
                    }
                    PreferenceIcon(IosIcons.ArrowDownThin) {
                        SwitchPreference(
                            title = stringResource(Res.string.downlink_enabled),
                            checked = channelInput.downlink_enabled,
                            enabled = true,
                            onCheckedChange = {
                                channelInput = channelInput.newBuilder().also { wb -> wb.downlink_enabled = it }.build()
                            },
                            modifier = Modifier.iosRowSeparator(start = ICON_ROW_TEXT_START),
                        )
                    }
                }
            }

            IosButton(
                text = stringResource(Res.string.save),
                onClick = { onAddClick(channelInput, pskEditState) },
                style = IosButtonStyle.Tinted,
                icon = IosIcons.ShareDown,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp),
            )
        }
    }
}

/** iOS's `.large` sheet, the top of the page still showing above it. */
private const val SHEET_HEIGHT_FRACTION = 0.93f

/** Where an icon row's text starts (16 dp margin, 26 dp symbol, 14 dp gap), so its separator starts there too. */
private val ICON_ROW_TEXT_START = 56.dp

data class ChannelPskEditState(
    val canGeneratePskForName: Boolean = false,
    val generatedPskForName: Boolean = false,
    val pskExplicitlyEdited: Boolean = false,
)

internal data class ChannelNameUpdate(val settings: ChannelSettings, val pskEditState: ChannelPskEditState)

internal fun ChannelSettings.applyChannelNameEdit(
    nameInput: String,
    pskEditState: ChannelPskEditState,
): ChannelNameUpdate {
    val name = nameInput.trim()
    val defaultPsk = Channel.default.settings.psk
    val clearGeneratedPsk = name.isBlank() && pskEditState.generatedPskForName && !pskEditState.pskExplicitlyEdited
    val generateNamedPsk =
        name.isNotBlank() &&
            psk == defaultPsk &&
            pskEditState.canGeneratePskForName &&
            !pskEditState.pskExplicitlyEdited
    val nextPsk =
        when {
            clearGeneratedPsk -> defaultPsk
            generateNamedPsk -> Channel.getRandomKey()
            else -> psk
        }
    val nextGeneratedPskForName =
        when {
            clearGeneratedPsk -> false
            generateNamedPsk -> true
            else -> pskEditState.generatedPskForName
        }
    return ChannelNameUpdate(
        settings =
        this.newBuilder()
            .also { wb ->
                wb.name = name
                wb.psk = nextPsk
            }
            .build(),
        pskEditState = pskEditState.copy(generatedPskForName = nextGeneratedPskForName),
    )
}
