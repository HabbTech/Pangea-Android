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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.meshtastic.core.common.util.LocaleUnitsProvider
import org.meshtastic.core.common.util.MeasurementSystem
import org.meshtastic.core.model.util.precisionRadiusMetersOrNull
import org.meshtastic.core.model.util.toDistanceString
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.i_agree
import org.meshtastic.core.resources.i_agree_to_share_my_location
import org.meshtastic.core.resources.map_reporting_consent_header
import org.meshtastic.core.resources.map_reporting_consent_text
import org.meshtastic.core.resources.schema_mapreportsettings_publish_interval_secs
import org.meshtastic.core.resources.schema_mapreportsettings_publish_interval_secs_description
import org.meshtastic.core.resources.schema_mqtt_map_reporting_enabled
import org.meshtastic.core.resources.schema_mqtt_map_reporting_enabled_description
import org.meshtastic.core.ui.component.DropDownPreference
import org.meshtastic.core.ui.component.PreferenceDivider
import org.meshtastic.core.ui.component.PreferenceIcon
import org.meshtastic.core.ui.component.SwitchPreference
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosSlider
import org.meshtastic.feature.settings.util.FixedUpdateIntervals
import org.meshtastic.feature.settings.util.IntervalConfiguration
import org.meshtastic.feature.settings.util.toDisplayString
import kotlin.math.roundToInt

private const val POSITION_PRECISION_MIN = 12
private const val POSITION_PRECISION_MAX = 15

@Suppress("LongMethod")
@Composable
fun MapReportingPreference(
    mapReportingEnabled: Boolean = false,
    onMapReportingEnabledChanged: (Boolean) -> Unit = {},
    shouldReportLocation: Boolean = false,
    onShouldReportLocationChanged: (Boolean) -> Unit = {},
    positionPrecision: Int = 14,
    onPositionPrecisionChanged: (Int) -> Unit = {},
    publishIntervalSecs: Int = 3600,
    onPublishIntervalSecsChanged: (Int) -> Unit = {},
    enabled: Boolean,
) {
    Column {
        // Tracks the switch locally so the consent rows can show before the config round-trips; re-keyed on
        // [mapReportingEnabled] so an externally-changed config wins over a stale local toggle.
        var showMapReportingWarning by rememberSaveable(mapReportingEnabled) { mutableStateOf(mapReportingEnabled) }
        PreferenceIcon(IosIcons.MapOutline) {
            SwitchPreference(
                title = stringResource(Res.string.schema_mqtt_map_reporting_enabled),
                summary = stringResource(Res.string.schema_mqtt_map_reporting_enabled_description),
                checked = showMapReportingWarning,
                enabled = enabled,
                onCheckedChange = { checked ->
                    showMapReportingWarning = checked
                    if (checked && shouldReportLocation) {
                        onMapReportingEnabledChanged(true)
                    } else if (!checked) {
                        onMapReportingEnabledChanged(false)
                    }
                },
            )
        }
        // The consent sits in the same section as rows of its own, as on the iPhone.
        AnimatedVisibility(showMapReportingWarning) {
            Column {
                PreferenceDivider()
                Text(
                    text = stringResource(Res.string.map_reporting_consent_header),
                    style = Ios.type.body,
                    color = Ios.colors.label,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 11.dp),
                )
                Text(
                    text = stringResource(Res.string.map_reporting_consent_text),
                    style = Ios.type.caption1,
                    color = Ios.colors.secondaryLabel,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 11.dp),
                )
                PreferenceDivider()
                SwitchPreference(
                    title = stringResource(Res.string.i_agree),
                    summary = stringResource(Res.string.i_agree_to_share_my_location),
                    checked = shouldReportLocation,
                    enabled = enabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            onMapReportingEnabledChanged(true)
                            onShouldReportLocationChanged(true)
                        } else {
                            onShouldReportLocationChanged(false)
                        }
                    },
                )
                if (shouldReportLocation && mapReportingEnabled) {
                    PreferenceDivider()
                    val publishItems = remember { IntervalConfiguration.BROADCAST_MEDIUM.allowedIntervals }
                    DropDownPreference(
                        title = stringResource(Res.string.schema_mapreportsettings_publish_interval_secs),
                        summary = stringResource(Res.string.schema_mapreportsettings_publish_interval_secs_description),
                        items = publishItems.map { it to it.toDisplayString() },
                        selectedItem =
                        FixedUpdateIntervals.fromValue(publishIntervalSecs.toLong()) ?: publishItems.first(),
                        enabled = enabled,
                        onItemSelected = { onPublishIntervalSecsChanged(it.value.toInt()) },
                    )
                    PreferenceDivider()
                    IosSlider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        value = positionPrecision.toFloat(),
                        onValueChange = { onPositionPrecisionChanged(it.roundToInt()) },
                        enabled = enabled,
                        valueRange = POSITION_PRECISION_MIN.toFloat()..POSITION_PRECISION_MAX.toFloat(),
                        steps = POSITION_PRECISION_MAX - POSITION_PRECISION_MIN - 1,
                    )
                    val precisionMeters = precisionRadiusMetersOrNull(positionPrecision)?.toInt()
                    val unit =
                        if (LocalInspectionMode.current) {
                            // Previews and screenshot tests render with no Koin application; the value itself is
                            // arbitrary there.
                            MeasurementSystem.METRIC
                        } else {
                            koinInject<LocaleUnitsProvider>().measurementSystem.collectAsStateWithLifecycle().value
                        }
                    Text(
                        text = "± ${precisionMeters?.toDistanceString(unit).orEmpty()}",
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 11.dp),
                        style = Ios.type.body,
                        color = Ios.colors.secondaryLabel,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun MapReportingPreview() {
    MapReportingPreference(
        mapReportingEnabled = true,
        onMapReportingEnabledChanged = {},
        shouldReportLocation = true,
        onShouldReportLocationChanged = {},
        positionPrecision = 5,
        onPositionPrecisionChanged = {},
        enabled = true,
    )
}
