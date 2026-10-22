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
package org.meshtastic.feature.settings.radio.channel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.Capabilities
import org.meshtastic.core.model.Channel
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.add
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.channels
import org.meshtastic.core.resources.pangea_add_channel
import org.meshtastic.core.resources.pangea_channel_legend
import org.meshtastic.core.resources.send
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.component.PreferenceDivider
import org.meshtastic.core.ui.component.PreferenceFooter
import org.meshtastic.core.ui.component.dragContainer
import org.meshtastic.core.ui.component.dragDropItemsIndexed
import org.meshtastic.core.ui.component.rememberDragDropState
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosButton
import org.meshtastic.core.ui.ios.IosButtonStyle
import org.meshtastic.core.ui.ios.IosGlassIconButton
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.feature.settings.radio.RadioConfigViewModel
import org.meshtastic.feature.settings.radio.RebootBehavior
import org.meshtastic.feature.settings.radio.ResponseState
import org.meshtastic.feature.settings.radio.channel.component.ChannelCard
import org.meshtastic.feature.settings.radio.channel.component.ChannelLegendDialog
import org.meshtastic.feature.settings.radio.channel.component.ChannelPskEditState
import org.meshtastic.feature.settings.radio.channel.component.EditChannelDialog
import org.meshtastic.feature.settings.radio.component.LoadingOverlay
import org.meshtastic.feature.settings.radio.component.PacketResponseStateDialog
import org.meshtastic.proto.ChannelSettings
import org.meshtastic.proto.Config

@Composable
fun ChannelConfigScreen(viewModel: RadioConfigViewModel, onBack: () -> Unit) {
    val state by viewModel.radioConfigState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        ChannelConfigScreen(
            title = stringResource(Res.string.channels),
            onBack = onBack,
            settingsList = state.channelList,
            loraConfig = state.radioConfig.lora ?: Config.LoRaConfig.Builder().build(),
            maxChannels = viewModel.maxChannels,
            firmwareVersion = state.metadata?.firmware_version ?: "0.0.0",
            enabled = state.connected,
            isFetching = state.responseState is ResponseState.Loading,
            onPositiveClicked = { channelListInput -> viewModel.updateChannels(channelListInput, state.channelList) },
        )

        LoadingOverlay(state = state.responseState)

        if (state.responseState is ResponseState.Success || state.responseState is ResponseState.Error) {
            PacketResponseStateDialog(
                state = state.responseState,
                onDismiss = viewModel::clearPacketResponse,
                rebootBehavior = RebootBehavior.NEVER,
            )
        }
    }
}

@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
private fun ChannelConfigScreen(
    title: String,
    onBack: () -> Unit,
    settingsList: List<ChannelSettings>,
    loraConfig: Config.LoRaConfig,
    maxChannels: Int = 8,
    firmwareVersion: String,
    enabled: Boolean,
    isFetching: Boolean = false,
    onPositiveClicked: (List<ChannelSettings>) -> Unit,
) {
    val primarySettings = settingsList.getOrNull(0) ?: return
    val modemPresetName = remember(loraConfig) { Channel(loraConfig = loraConfig).name }
    val primaryChannel = remember(loraConfig) { Channel(primarySettings, loraConfig) }
    val capabilities = remember(firmwareVersion) { Capabilities(firmwareVersion) }

    val focusManager = LocalFocusManager.current
    val settingsListInput =
        rememberSaveable(saver = listSaver(save = { it.toList() }, restore = { it.toMutableStateList() })) {
            settingsList.toMutableStateList()
        }
    val pskEditStatesInput =
        rememberSaveable(saver = channelPskEditStatesSaver) {
            List(settingsListInput.size) { ChannelPskEditState() }.toMutableStateList()
        }

    // A remote channel fetch streams channels in one response at a time AFTER the editor first composes
    // (composition starts as soon as channel 0 lands), so the one-shot seed above goes stale and the list
    // would only show the channels present at seed time (#6317 — the footer Cancel's replaceWith was the
    // accidental workaround). Adopt the authoritative list while the fetch is in flight: the loading
    // overlay blocks input during the fetch, so there are no user edits to clobber.
    LaunchedEffect(settingsList, isFetching) {
        if (isFetching) {
            settingsListInput.replaceWith(settingsList)
            pskEditStatesInput.replaceAll(settingsList.size, ChannelPskEditState())
        }
    }

    val listState = rememberLazyListState()
    val dragDropState =
        rememberDragDropState(listState) { fromIndex, toIndex ->
            if (toIndex in settingsListInput.indices && fromIndex in settingsListInput.indices) {
                settingsListInput.move(fromIndex, toIndex)
                pskEditStatesInput.move(fromIndex, toIndex)
            }
        }

    val isEditing: Boolean =
        settingsList.size != settingsListInput.size ||
            settingsList.zip(settingsListInput).any { (item1, item2) -> item1 != item2 }

    var showEditChannelDialog: Int? by rememberSaveable { mutableStateOf(null) }
    var showChannelLegendDialog by rememberSaveable { mutableStateOf(false) }

    if (showEditChannelDialog != null) {
        val index = showEditChannelDialog ?: return
        EditChannelDialog(
            channelSettings = settingsListInput.getOrNull(index) ?: ChannelSettings.Builder().build(),
            modemPresetName = modemPresetName,
            initialPskEditState = pskEditStatesInput.getOrElse(index) { ChannelPskEditState() },
            onAddClick = { settings, pskEditState ->
                if (settingsListInput.size > index) {
                    settingsListInput[index] = settings
                    pskEditStatesInput[index] = pskEditState
                } else {
                    settingsListInput.add(settings)
                    pskEditStatesInput.add(pskEditState)
                }
                showEditChannelDialog = null
            },
            onDismissRequest = { showEditChannelDialog = null },
        )
    }

    if (showChannelLegendDialog) {
        ChannelLegendDialog(capabilities) { showChannelLegendDialog = false }
    }

    Scaffold(
        topBar = {
            MainAppBar(
                title = title,
                canNavigateUp = true,
                onNavigateUp = onBack,
                ourNode = null,
                showNodeChip = false,
                actions = {},
                onClickChip = {},
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column {
                val locationChannel = determineLocationSharingChannel(capabilities, settingsListInput.toList())

                LazyColumn(
                    modifier =
                    Modifier.dragContainer(dragDropState = dragDropState, haptics = LocalHapticFeedback.current),
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                ) {
                    // One white card, as on iOS: the channels (still drag-to-reorder, Android's own), then Add Channel.
                    dragDropItemsIndexed(items = settingsListInput, dragDropState = dragDropState) {
                            index,
                            channel,
                            isDragging,
                        ->
                        val shape =
                            if (index == 0) {
                                RoundedCornerShape(topStart = CARD_CORNER, topEnd = CARD_CORNER)
                            } else {
                                RectangleShape
                            }
                        Column(Modifier.clip(shape).background(Ios.colors.groupedRow)) {
                            if (index > 0) PreferenceDivider(startIndent = 74.dp)
                            ChannelCard(
                                index = index,
                                title = channel.name.ifEmpty { modemPresetName },
                                enabled = enabled,
                                channelSettings = channel,
                                loraConfig = loraConfig,
                                onEditClick = { showEditChannelDialog = index },
                                onDeleteClick = {
                                    settingsListInput.removeAt(index)
                                    pskEditStatesInput.removeAt(index)
                                },
                                sharesLocation = locationChannel == index,
                            )
                        }
                    }
                    item {
                        Column(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(bottomStart = CARD_CORNER, bottomEnd = CARD_CORNER))
                                .background(Ios.colors.groupedRow)
                                .padding(top = 6.dp, bottom = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            PreferenceDivider()
                            Spacer(Modifier.height(18.dp))
                            IosButton(
                                text = stringResource(Res.string.pangea_add_channel),
                                icon = IosIcons.PlusSquare,
                                style = IosButtonStyle.Tinted,
                                textStyle = Ios.type.title3,
                                // iOS's large control: 50 dp tall.
                                modifier = Modifier.height(54.dp).widthIn(min = 178.dp),
                                enabled = enabled && maxChannels > settingsListInput.size,
                                onClick = {
                                    settingsListInput.add(Channel.default.settings)
                                    pskEditStatesInput.add(ChannelPskEditState(canGeneratePskForName = true))
                                    showEditChannelDialog = settingsListInput.lastIndex
                                },
                            )
                        }
                    }
                    // Android sends channel edits together: Cancel and Send appear once something has changed.
                    if (isEditing) {
                        item {
                            PreferenceFooter(
                                enabled = enabled && isEditing,
                                negativeText = stringResource(Res.string.cancel),
                                onNegativeClicked = {
                                    focusManager.clearFocus()
                                    settingsListInput.replaceWith(settingsList)
                                    pskEditStatesInput.replaceAll(settingsList.size, ChannelPskEditState())
                                },
                                positiveText = stringResource(Res.string.send),
                                onPositiveClicked = {
                                    focusManager.clearFocus()
                                    val committedSettings = settingsListInput.toList()
                                    onPositiveClicked(committedSettings)
                                    settingsListInput.replaceWith(committedSettings)
                                    pskEditStatesInput.replaceAll(committedSettings.size, ChannelPskEditState())
                                },
                            )
                        }
                    }
                }
            }

            // iOS's legend button, floating at the bottom left.
            IosGlassIconButton(
                icon = IosIcons.QuestionCircle,
                contentDescription = stringResource(Res.string.pangea_channel_legend),
                onClick = { showChannelLegendDialog = true },
                tint = Ios.colors.tint,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 22.dp),
            )
            AnimatedVisibility(
                visible = maxChannels > settingsListInput.size,
                modifier = Modifier.align(Alignment.BottomEnd),
                enter =
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                ),
                exit =
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                ),
            ) {}
        }
    }
}

private val channelPskEditStatesSaver =
    listSaver<SnapshotStateList<ChannelPskEditState>, Int>(
        save = { states -> states.map { it.toSaveableFlags() } },
        restore = { flags -> flags.map { it.toChannelPskEditState() }.toMutableStateList() },
    )

internal fun <T> MutableList<T>.move(fromIndex: Int, toIndex: Int) {
    add(toIndex, removeAt(fromIndex))
}

internal fun <T> MutableList<T>.replaceWith(values: List<T>) {
    clear()
    addAll(values)
}

internal fun MutableList<ChannelPskEditState>.replaceAll(size: Int, value: ChannelPskEditState) {
    clear()
    repeat(size) { add(value) }
}

internal fun ChannelPskEditState.toSaveableFlags(): Int {
    var flags = 0
    if (canGeneratePskForName) flags = flags or CAN_GENERATE_PSK_FOR_NAME_FLAG
    if (generatedPskForName) flags = flags or GENERATED_PSK_FOR_NAME_FLAG
    if (pskExplicitlyEdited) flags = flags or PSK_EXPLICITLY_EDITED_FLAG
    return flags
}

internal fun Int.toChannelPskEditState(): ChannelPskEditState = ChannelPskEditState(
    canGeneratePskForName = this and CAN_GENERATE_PSK_FOR_NAME_FLAG != 0,
    generatedPskForName = this and GENERATED_PSK_FOR_NAME_FLAG != 0,
    pskExplicitlyEdited = this and PSK_EXPLICITLY_EDITED_FLAG != 0,
)

private const val CAN_GENERATE_PSK_FOR_NAME_FLAG = 1
private const val GENERATED_PSK_FOR_NAME_FLAG = 1 shl 1
private const val PSK_EXPLICITLY_EDITED_FLAG = 1 shl 2

/**
 * Determines what [Channel] if any is enabled to conduct automatic location sharing.
 *
 * @param capabilities of the connected node.
 * @param settingsList Current list of channels on the node.
 * @return the index of the channel within `settingsList`.
 */
private fun determineLocationSharingChannel(capabilities: Capabilities, settingsList: List<ChannelSettings>): Int {
    var output = -1
    if (capabilities.supportsSecondaryChannelLocation) {
        /* Essentially the first index with the setting enabled */
        for ((i, settings) in settingsList.withIndex()) {
            if ((settings.module_settings?.position_precision ?: 0) > 0) {
                output = i
                break
            }
        }
    } else {
        /* Only the primary channel at index 0 can share locations automatically */
        val primary = settingsList[0]
        if ((primary.module_settings?.position_precision ?: 0) > 0) {
            output = 0
        }
    }
    return output
}

/** iOS's inset-grouped card corners. */
private val CARD_CORNER = 26.dp
