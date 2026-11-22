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
package org.meshtastic.feature.settings.radio.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.ambient_lighting
import org.meshtastic.core.resources.options
import org.meshtastic.core.resources.pangea_color
import org.meshtastic.core.resources.schema_ambientlighting_current
import org.meshtastic.core.resources.schema_ambientlighting_current_description
import org.meshtastic.core.resources.schema_ambientlighting_led_state
import org.meshtastic.core.resources.schema_ambientlighting_led_state_description
import org.meshtastic.core.ui.component.PreferenceDivider
import org.meshtastic.core.ui.component.PreferenceIcon
import org.meshtastic.core.ui.component.SwitchPreference
import org.meshtastic.core.ui.component.TitledCard
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosColorPickerSheet
import org.meshtastic.core.ui.ios.IosColorWell
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.feature.settings.radio.RadioConfigViewModel
import org.meshtastic.feature.settings.radio.RebootBehavior
import org.meshtastic.proto.ModuleConfig
import org.meshtastic.proto.current

@Composable
fun AmbientLightingConfigScreen(viewModel: RadioConfigViewModel, onBack: () -> Unit) {
    val state by viewModel.radioConfigState.collectAsStateWithLifecycle()
    val ambientLightingConfig =
        state.moduleConfig.ambient_lighting ?: ModuleConfig.AmbientLightingConfig.Builder().build()
    val formState = rememberConfigState(initialValue = ambientLightingConfig)
    val focusManager = LocalFocusManager.current

    RadioConfigScreenList(
        rebootBehavior = RebootBehavior.ALWAYS,
        title = stringResource(Res.string.ambient_lighting),
        configFor = state.userConfig.long_name.ifEmpty { state.destNodeName },
        onBack = onBack,
        configState = formState,
        enabled = state.connected,
        responseState = state.responseState,
        onDismissPacketResponse = viewModel::clearPacketResponse,
        onSave = {
            val config = ModuleConfig.Builder().also { wb -> wb.ambient_lighting = it }.build()
            viewModel.setModuleConfig(config)
        },
    ) {
        item {
            // iOS: one Options section. The LED colour is one colour well over the red, green and blue channels, and
            // the drive current a stepper, as the registry bounds it to a small range.
            TitledCard(title = stringResource(Res.string.options)) {
                PreferenceIcon(IosIcons.Lightbulb) {
                    SwitchPreference(
                        title = stringResource(Res.string.schema_ambientlighting_led_state),
                        summary = stringResource(Res.string.schema_ambientlighting_led_state_description),
                        checked = formState.value.led_state,
                        enabled = state.connected,
                        onCheckedChange = {
                            formState.value = formState.value.newBuilder().also { wb -> wb.led_state = it }.build()
                        },
                        containerColor = CardDefaults.cardColors().containerColor,
                    )
                }
                PreferenceDivider()
                LedColorRow(
                    config = formState.value,
                    enabled = state.connected,
                    onConfigChange = { formState.value = it },
                )
                PreferenceDivider(startIndent = ColorRowTextStart)
                BoundedIntEditTextPreference(
                    title = stringResource(Res.string.schema_ambientlighting_current),
                    summary = stringResource(Res.string.schema_ambientlighting_current_description),
                    value = formState.value.current,
                    metadata = ModuleConfig.AmbientLightingConfig.current,
                    enabled = state.connected,
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    onValueChange = {
                        formState.value = formState.value.newBuilder().also { wb -> wb.current = it }.build()
                    },
                )
            }
        }
    }
}

/** Where the Color row's title starts: after the eyedropper, as on iOS. */
private val ColorRowTextStart = 46.dp

/** The LED colour as one row: the eyedropper, "Color", and a colour well that opens the picker. */
@Composable
private fun LedColorRow(
    config: ModuleConfig.AmbientLightingConfig,
    enabled: Boolean,
    onConfigChange: (ModuleConfig.AmbientLightingConfig) -> Unit,
) {
    val c = Ios.colors
    var picking by remember { mutableStateOf(false) }
    val color =
        Color(
            red = config.red.coerceIn(0, CHANNEL_MAX),
            green = config.green.coerceIn(0, CHANNEL_MAX),
            blue = config.blue.coerceIn(0, CHANNEL_MAX),
        )
    Row(
        Modifier.fillMaxWidth()
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(IosIcons.Eyedropper, contentDescription = null, tint = c.tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(ColorRowTextStart - 16.dp - 22.dp))
        Text(
            stringResource(Res.string.pangea_color),
            style = Ios.type.body,
            color = c.label,
            modifier = Modifier.weight(1f),
        )
        IosColorWell(color = color, onClick = { picking = true }, enabled = enabled)
    }
    if (picking) {
        IosColorPickerSheet(
            color = color,
            onColorChange = { picked ->
                val argb = picked.toArgb()
                onConfigChange(
                    config
                        .newBuilder()
                        .also { wb ->
                            wb.red = (argb shr RED_SHIFT) and CHANNEL_MAX
                            wb.green = (argb shr GREEN_SHIFT) and CHANNEL_MAX
                            wb.blue = argb and CHANNEL_MAX
                        }
                        .build(),
                )
            },
            onDismissRequest = { picking = false },
        )
    }
}

private const val CHANNEL_MAX = 0xFF
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val DISABLED_ALPHA = 0.45f
