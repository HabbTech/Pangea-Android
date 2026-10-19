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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.model.RegionInfo
import org.meshtastic.core.model.pangea.RadioAutoConfig
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.intro_expert_continue
import org.meshtastic.core.resources.pangea_display_name
import org.meshtastic.core.resources.pangea_pair_no_radio
import org.meshtastic.core.resources.pangea_radio_restarting_wait
import org.meshtastic.core.resources.pangea_region_choose
import org.meshtastic.core.resources.pangea_region_footer
import org.meshtastic.core.resources.pangea_region_label
import org.meshtastic.core.resources.pangea_save_to_radio_failed
import org.meshtastic.core.resources.pangea_saving_to_radio
import org.meshtastic.core.resources.pangea_setting_up
import org.meshtastic.core.resources.pangea_setup_name_desc
import org.meshtastic.core.resources.pangea_setup_name_title
import org.meshtastic.core.resources.skip
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosActivityIndicator
import org.meshtastic.core.ui.ios.IosButton
import org.meshtastic.core.ui.ios.IosButtonStyle
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosTextField
import org.meshtastic.core.ui.ios.iosPressable
import org.meshtastic.feature.intro.SetupProfileViewModel.Phase
import org.meshtastic.proto.Config.LoRaConfig.RegionCode

/**
 * Pair Your Radio, as on the iPhone app: the [pairRadio] content (from the connections feature) on a setup sheet, with
 * "I don't have my radio yet" at the bottom, which ends setup.
 */
@Composable
internal fun PairRadioScreen(
    pairRadio: @Composable (onPair: () -> Unit) -> Unit,
    onPair: () -> Unit,
    onNoRadio: () -> Unit,
) {
    IosSetupSheet(
        bottom = {
            IosButton(
                text = stringResource(Res.string.pangea_pair_no_radio),
                onClick = onNoRadio,
                style = IosButtonStyle.Plain,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp),
            )
        },
    ) {
        pairRadio(onPair)
    }
}

/**
 * "What should people call you?", as on the iPhone app (SetupProfileView): the name friends see, plus where the radio
 * will be used when it has no region yet. Saving restarts the radio; the page waits for it before moving on. Unlike
 * iOS, a Skip link appears if saving fails.
 */
@Suppress("LongMethod")
@Composable
internal fun SetupProfileScreen(onFinish: () -> Unit, viewModel: SetupProfileViewModel = koinViewModel()) {
    val c = Ios.colors
    val phase by viewModel.phase.collectAsStateWithLifecycle()
    val needsRegion by viewModel.needsRegion.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf(viewModel.startingName()) }
    var region by remember { mutableStateOf(viewModel.suggestedRegion) }
    val focus = remember { FocusRequester() }
    val busy = phase == Phase.SAVING || phase == Phase.RESTARTING
    val fill = c.secondaryFill

    val currentOnFinish by rememberUpdatedState(onFinish)
    LaunchedEffect(phase) { if (phase == Phase.DONE) currentOnFinish() }
    LaunchedEffect(Unit) { if (name.isEmpty()) focus.requestFocus() }

    IosSetupPage(
        title = stringResource(Res.string.pangea_setup_name_title),
        description = stringResource(Res.string.pangea_setup_name_desc),
        plainDescription = true,
        primaryLabel = stringResource(if (busy) Res.string.pangea_setting_up else Res.string.intro_expert_continue),
        primaryEnabled =
        RadioAutoConfig.clampedLongName(name).isNotEmpty() && (!needsRegion || region != null) && !busy,
        onPrimary = { viewModel.save(name, region.takeIf { needsRegion }) },
        // If the radio didn't take it (or is Demo Mode, which ignores settings), the name is already in the profile:
        // let the user carry on rather than strand them in setup ("every step can be skipped or done later").
        skipLabel = if (phase == Phase.FAILED) stringResource(Res.string.skip) else null,
        onSkip = onFinish,
        header = {
            Icon(
                IosIcons.PersonCircleFill,
                contentDescription = null,
                tint = c.tint,
                modifier = Modifier.padding(top = 16.dp).size(68.dp),
            )
        },
    ) {
        IosTextField(
            value = name,
            onValueChange = {
                name = if (it.encodeToByteArray().size > MAX_NAME_BYTES) RadioAutoConfig.clampedLongName(it) else it
            },
            placeholder = stringResource(Res.string.pangea_display_name),
            textStyle = Ios.type.title3,
            containerColor = fill,
            keyboardOptions =
            KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth().height(56.dp).focusRequester(focus),
        )
        if (needsRegion) {
            RegionCard(region = region, onRegion = { region = it }, fill = fill)
        }
        Status(phase)
    }
}

@Composable
private fun RegionCard(region: RegionCode?, onRegion: (RegionCode) -> Unit, fill: Color) {
    val c = Ios.colors
    var choosing by remember { mutableStateOf(false) }
    val regions = remember { RegionInfo.entries.filter { it.regionCode != RegionCode.UNSET } }
    Column(
        Modifier.fillMaxWidth().padding(top = 28.dp).clip(RoundedCornerShape(14.dp)).background(fill).padding(16.dp),
    ) {
        Box {
            Row(
                Modifier.fillMaxWidth().iosPressable { choosing = true },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(IosIcons.Globe, contentDescription = null, tint = c.tint, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(Res.string.pangea_region_label), style = Ios.type.body, color = c.label)
                Spacer(Modifier.weight(1f))
                Text(
                    regions.firstOrNull { it.regionCode == region }?.description
                        ?: stringResource(Res.string.pangea_region_choose),
                    style = Ios.type.body,
                    color = c.secondaryLabel,
                )
                Icon(
                    IosIcons.ChevronRight,
                    contentDescription = null,
                    tint = c.tertiaryLabel,
                    modifier = Modifier.padding(start = 6.dp).size(14.dp),
                )
            }
            DropdownMenu(expanded = choosing, onDismissRequest = { choosing = false }) {
                regions.forEach { info ->
                    DropdownMenuItem(
                        text = { Text(info.description) },
                        onClick = {
                            onRegion(info.regionCode)
                            choosing = false
                        },
                    )
                }
            }
        }
        Text(
            stringResource(Res.string.pangea_region_footer),
            style = Ios.type.footnote,
            color = c.secondaryLabel,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun Status(phase: Phase) {
    val c = Ios.colors
    Column(
        Modifier.fillMaxWidth().padding(top = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (phase) {
            Phase.SAVING -> {
                IosActivityIndicator(color = c.secondaryLabel)
                Text(stringResource(Res.string.pangea_saving_to_radio), style = Ios.type.body, color = c.secondaryLabel)
            }

            Phase.RESTARTING -> {
                IosActivityIndicator(color = c.secondaryLabel)
                Text(
                    stringResource(Res.string.pangea_radio_restarting_wait),
                    style = Ios.type.footnote,
                    color = c.secondaryLabel,
                    textAlign = TextAlign.Center,
                )
            }

            Phase.FAILED -> {
                val orange = c.systemOrange
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        IosIcons.WarningFill,
                        contentDescription = null,
                        tint = orange,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(Res.string.pangea_save_to_radio_failed),
                        style = Ios.type.footnote,
                        color = orange,
                    )
                }
            }

            Phase.EDITING,
            Phase.DONE,
            -> Unit
        }
    }
}

/** A Meshtastic long name holds 36 bytes of UTF-8 (Pangea-Apple RadioAutoConfig.clampedLongName). */
private const val MAX_NAME_BYTES = 36
