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
package org.meshtastic.core.ui.qr

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.model.Channel
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.accept
import org.meshtastic.core.resources.add
import org.meshtastic.core.resources.add_channels_description
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.new_channel_rcvd
import org.meshtastic.core.resources.replace
import org.meshtastic.core.resources.replace_channels_and_settings_description
import org.meshtastic.core.ui.component.ChannelSelection
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.ios.IosSegmentedControl
import org.meshtastic.core.ui.ios.IosSheet
import org.meshtastic.core.ui.theme.AppTheme
import org.meshtastic.core.ui.util.getChannelPreviewForAdd
import org.meshtastic.proto.ChannelSet

@Composable
fun ScannedQrCodeDialog(
    incoming: ChannelSet,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScannedQrCodeViewModel = koinViewModel(),
) {
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val maxChannels by viewModel.maxChannels.collectAsStateWithLifecycle()

    ScannedQrCodeDialog(
        channels = channels,
        incoming = incoming,
        onDismiss = onDismiss,
        modifier = modifier,
        maxChannels = maxChannels,
        onConfirm = viewModel::setChannels,
    )
}

/** Enables the user to select which channels to accept after scanning a QR code. */
@OptIn(ExperimentalLayoutApi::class)
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
fun ScannedQrCodeDialog(
    channels: ChannelSet,
    incoming: ChannelSet,
    onDismiss: () -> Unit,
    onConfirm: (ChannelSet) -> Unit,
    modifier: Modifier = Modifier,
    maxChannels: Int = DEFAULT_MAX_CHANNELS,
) {
    var shouldReplace by rememberSaveable { mutableStateOf(incoming.lora_config != null) }

    // Freeze the channel limit for the dialog's lifetime so a late myNodeInfo emission (which updates
    // viewModel.maxChannels from the DEFAULT_MAX_CHANNELS seed to the real device value) cannot rebuild
    // addPreview/channelSelections and silently wipe the user's checkbox toggles.
    val effectiveMaxChannels = rememberSaveable { maxChannels }

    // Filtered ADD-mode preview: existing channels plus only the unique incoming channels. Duplicate incoming channels
    // are omitted entirely. REPLACE mode ignores this and uses the raw incoming set.
    val addPreview =
        remember(channels.settings, incoming.settings, channels.lora_config, effectiveMaxChannels) {
            getChannelPreviewForAdd(
                existing = channels.settings,
                incoming = incoming.settings,
                loraConfig = channels.lora_config ?: Channel.default.loraConfig,
                maxChannels = effectiveMaxChannels,
            )
        }

    val channelSet =
        remember(shouldReplace, channels, incoming, addPreview.settings) {
            if (shouldReplace) {
                // When replacing, apply the incoming LoRa configuration but preserve certain
                // locally safe fields such as MQTT flags and TX power. This prevents QR codes
                // from unintentionally overriding device-specific power limits (e.g. E22 caps).
                incoming
                    .newBuilder()
                    .also { wb ->
                        wb.lora_config =
                            incoming.lora_config
                                ?.newBuilder()
                                ?.also { wb ->
                                    wb.config_ok_to_mqtt = channels.lora_config?.config_ok_to_mqtt ?: false
                                    wb.tx_power = channels.lora_config?.tx_power ?: 0
                                }
                                ?.build()
                    }
                    .build()
            } else {
                channels.newBuilder().also { wb -> wb.settings = addPreview.settings }.build()
            }
        }

    val modemPresetName = Channel(loraConfig = channelSet.lora_config ?: Channel.default.loraConfig).name

    /* Holds selections made by the user */
    val channelSelections =
        remember(shouldReplace, channelSet.settings, addPreview.selections) {
            val defaults =
                if (shouldReplace) {
                    List(channelSet.settings.size) { true }
                } else {
                    addPreview.selections
                }
            mutableStateListOf<Boolean>().apply { addAll(defaults) }
        }

    val selectedChannelSet =
        if (shouldReplace) {
            channelSet
                .newBuilder()
                .also { wb ->
                    wb.settings = channelSet.settings.filterIndexed { i, _ -> channelSelections.getOrNull(i) == true }
                }
                .build()
        } else {
            channelSet
                .newBuilder()
                .also { wb ->
                    wb.settings =
                        channelSet.settings.filterIndexed { i, _ ->
                            // Primary (index 0) is always kept; existing secondaries can be dropped to free a slot for
                            // the
                            // incoming channel when the radio is full (Apple FR-017 "replace a secondary, never the
                            // primary").
                            i == 0 || channelSelections.getOrNull(i) == true
                        }
                }
                .build()
        }

    // Compute LoRa configuration changes when in replace mode
    val loraChanges =
        remember(shouldReplace, channels, incoming) {
            if (shouldReplace && incoming.lora_config != null) {
                val current = channels.lora_config
                val new = incoming.lora_config
                val changes = mutableListOf<String>()

                if (current?.hop_limit != new?.hop_limit) {
                    changes.add("Hop Limit: ${current?.hop_limit} -> ${new?.hop_limit}")
                }
                if (current?.region != new?.region) {
                    val currentRegionDesc = current?.region?.name ?: "Unknown"
                    val newRegionDesc = new?.region?.name ?: "Unknown"
                    changes.add("Region: $currentRegionDesc -> $newRegionDesc")
                }
                if (current?.modem_preset != new?.modem_preset) {
                    val currentPresetDesc = current?.modem_preset?.name ?: "Unknown"
                    val newPresetDesc = new?.modem_preset?.name ?: "Unknown"
                    changes.add("Modem Preset: $currentPresetDesc -> $newPresetDesc")
                }
                if (current?.use_preset != new?.use_preset) {
                    changes.add("Use Preset: ${current?.use_preset} -> ${new?.use_preset}")
                }

                changes
            } else {
                emptyList()
            }
        }

    // iOS's sheet for a scanned channel code: Cancel and Accept in the bar, Add or Replace as a segmented control, the
    // channels in a card, and what Replace would change to the LoRa settings under them.
    IosSheet(
        title = stringResource(Res.string.new_channel_rcvd),
        onDismissRequest = onDismiss,
        modifier = modifier,
        cancelLabel = stringResource(Res.string.cancel),
        actionLabel = stringResource(Res.string.accept),
        actionEnabled = selectedChannelSet.settings.size in 1..effectiveMaxChannels,
        onAction = {
            onDismiss()
            onConfirm(selectedChannelSet)
        },
        grouped = true,
    ) {
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                // Replace needs the code's LoRa settings; without them only Add is possible.
                IosSegmentedControl(
                    options = listOf(stringResource(Res.string.add), stringResource(Res.string.replace)),
                    selectedIndex = if (shouldReplace) 1 else 0,
                    onSelect = { index -> shouldReplace = index == 1 && incoming.lora_config != null },
                    enabled = incoming.lora_config != null || shouldReplace,
                    textStyle = Ios.type.subheadline,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(32.dp),
                )
                Text(
                    text =
                    stringResource(
                        if (shouldReplace) {
                            Res.string.replace_channels_and_settings_description
                        } else {
                            Res.string.add_channels_description
                        },
                    ),
                    style = Ios.type.footnote,
                    color = Ios.colors.secondaryLabel,
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 10.dp),
                )
            }

            item {
                IosSection {
                    channelSet.settings.forEachIndexed { index, channel ->
                        val isPrimary = index == 0
                        val channelObj = Channel(channel, channelSet.lora_config ?: Channel.default.loraConfig)
                        ChannelSelection(
                            index = index,
                            title = channel.name.ifEmpty { modemPresetName },
                            // Primary is always kept. In ADD mode existing secondaries become deselectable so the user
                            // can drop one to make room for the incoming channel on a full radio; REPLACE keeps its
                            // prior all-selectable behavior.
                            enabled = if (shouldReplace) true else !isPrimary,
                            isSelected = if (!shouldReplace && isPrimary) true else channelSelections[index],
                            onSelected = {
                                if (it || selectedChannelSet.settings.size > 1) {
                                    channelSelections[index] = it
                                }
                            },
                            channel = channelObj,
                        )
                    }
                }
            }

            // Display LoRa configuration changes when in replace mode
            if (shouldReplace && loraChanges.isNotEmpty()) {
                item {
                    IosSection(header = "LoRa Configuration Changes") {
                        loraChanges.forEach { change -> IosRow(title = change, titleStyle = Ios.type.subheadline) }
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ScannedQrCodeDialogPreview() {
    AppTheme {
        ScannedQrCodeDialog(
            channels =
            ChannelSet.Builder()
                .also { wb ->
                    wb.settings = listOf(Channel.default.settings)
                    wb.lora_config = Channel.default.loraConfig
                }
                .build(),
            incoming =
            ChannelSet.Builder()
                .also { wb ->
                    wb.settings = listOf(Channel.default.settings)
                    wb.lora_config = Channel.default.loraConfig
                }
                .build(),
            onDismiss = {},
            onConfirm = {},
        )
    }
}
