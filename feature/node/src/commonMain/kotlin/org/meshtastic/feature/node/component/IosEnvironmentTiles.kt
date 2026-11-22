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
@file:Suppress("MagicNumber", "TooManyFunctions")

package org.meshtastic.feature.node.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.common.util.MeasurementSystem
import org.meshtastic.core.common.util.NumberFormatter
import org.meshtastic.core.model.Node
import org.meshtastic.core.model.util.UnitConversions
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.distance
import org.meshtastic.core.resources.pangea_env_dew_point_now
import org.meshtastic.core.resources.pangea_env_gusts
import org.meshtastic.core.resources.pangea_env_humidity
import org.meshtastic.core.resources.pangea_env_pressure_high
import org.meshtastic.core.resources.pangea_env_pressure_low
import org.meshtastic.core.resources.pangea_env_rainfall_1h
import org.meshtastic.core.resources.pangea_env_rainfall_24h
import org.meshtastic.core.resources.pangea_env_soil_temp
import org.meshtastic.core.resources.pangea_env_temp
import org.meshtastic.core.resources.pressure
import org.meshtastic.core.resources.radiation
import org.meshtastic.core.resources.soil_moisture
import org.meshtastic.core.resources.weight
import org.meshtastic.core.resources.wind
import org.meshtastic.core.ui.icon.Humidity
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.icon.SoilMoisture
import org.meshtastic.core.ui.icon.SoilTemperature
import org.meshtastic.core.ui.icon.Weight
import org.meshtastic.core.ui.icon.WindDirection
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosIcons
import kotlin.math.roundToInt

/**
 * A node's weather readings as the iPhone app shows them (its `…CompactWidget` views in `NodeDetail`): a two-column
 * grid of grey tiles, each a small tinted heading over one large number. Readings iOS has no tile for stay in
 * [EnvironmentMetrics]'s cards underneath.
 */
@Suppress("CyclomaticComplexMethod", "LongMethod")
@Composable
internal fun IosEnvironmentTiles(
    node: Node,
    displayUnits: MeasurementSystem,
    isFahrenheit: Boolean,
    modifier: Modifier = Modifier,
) {
    val imperial = displayUnits == MeasurementSystem.IMPERIAL
    val tiles: List<@Composable (Modifier) -> Unit> = buildList {
        with(node.environmentMetrics) {
            val temp = temperature?.takeUnless { it.isNaN() }
            temp?.let { celsius -> add { m -> TemperatureTile(shortTemperature(celsius, isFahrenheit), m) } }
            relative_humidity
                ?.takeUnless { it.isNaN() }
                ?.let { rh ->
                    val dewPoint =
                        temp
                            ?.let { UnitConversions.calculateDewPoint(it, rh) }
                            ?.takeUnless { it.isNaN() }
                            ?.let { shortTemperature(it, isFahrenheit) }
                    add { m -> HumidityTile(rh.roundToInt(), dewPoint, m) }
                }
            barometric_pressure
                ?.takeUnless { it.isNaN() }
                ?.let { hPa ->
                    add { m ->
                        PressureTile(NumberFormatter.format(hPa, 2), low = hPa <= LOW_PRESSURE_HPA, modifier = m)
                    }
                }
            wind_speed
                ?.takeUnless { it.isNaN() }
                ?.let { speed ->
                    val gust = wind_gust?.takeUnless { it.isNaN() || it <= 0f }
                    add { m ->
                        WindTile(
                            speed = windSpeed(speed, imperial),
                            gust = gust?.let { windSpeed(it, imperial) },
                            direction = cardinal(wind_direction ?: 0),
                            modifier = m,
                        )
                    }
                }
            rainfall_1h
                ?.takeUnless { it.isNaN() }
                ?.let { mm ->
                    val (value, unit) = smallLength(mm, imperial)
                    add { m ->
                        ValueTile(
                            stringResource(Res.string.pangea_env_rainfall_1h),
                            IosIcons.CloudSunRain,
                            value,
                            unit,
                            m,
                        )
                    }
                }
            rainfall_24h
                ?.takeUnless { it.isNaN() }
                ?.let { mm ->
                    val (value, unit) = smallLength(mm, imperial)
                    add { m ->
                        ValueTile(stringResource(Res.string.pangea_env_rainfall_24h), IosIcons.Cloud, value, unit, m)
                    }
                }
            radiation
                ?.takeUnless { it.isNaN() }
                ?.let { r ->
                    add { m ->
                        ValueTile(
                            title = stringResource(Res.string.radiation),
                            icon = null,
                            value = NumberFormatter.format(r, 1),
                            unit = "µR/hr",
                            modifier = m,
                            smallerValueSize = 34,
                        )
                    }
                }
            weight
                ?.takeUnless { it.isNaN() }
                ?.let { kg ->
                    val value = NumberFormatter.format(if (imperial) kg * LBS_PER_KG else kg, 1)
                    add { m ->
                        ValueTile(
                            stringResource(Res.string.weight),
                            MeshtasticIcons.Weight,
                            value,
                            if (imperial) "lbs" else "kg",
                            m,
                        )
                    }
                }
            distance
                ?.takeUnless { it.isNaN() }
                ?.let { mm ->
                    val (value, unit) = smallLength(mm, imperial)
                    add { m -> ValueTile(stringResource(Res.string.distance), IosIcons.Measurement, value, unit, m) }
                }
            soil_temperature
                ?.takeUnless { it.isNaN() }
                ?.let { celsius ->
                    val shown = if (isFahrenheit) UnitConversions.celsiusToFahrenheit(celsius) else celsius
                    add { m ->
                        ValueTile(
                            stringResource(Res.string.pangea_env_soil_temp),
                            MeshtasticIcons.SoilTemperature,
                            shown.roundToInt().toString(),
                            if (isFahrenheit) "°F" else "°C",
                            m,
                        )
                    }
                }
            soil_moisture?.let { percent ->
                add { m ->
                    ValueTile(
                        stringResource(Res.string.soil_moisture),
                        MeshtasticIcons.SoilMoisture,
                        percent.toString(),
                        "%",
                        m,
                    )
                }
            }
        }
    }
    if (tiles.isEmpty()) return
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(TILE_GAP)) {
        tiles.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(TILE_GAP)) {
                pair.forEach { tile -> tile(Modifier.weight(1f)) }
                // A lone tile keeps to its own column, as in iOS's grid.
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** iOS's `WeatherConditionsCompactWidget`: the temperature at 72 pt, or 54 pt when it runs to four characters. */
@Composable
private fun TemperatureTile(temperature: String, modifier: Modifier = Modifier) {
    val heading = stringResource(Res.string.pangea_env_temp)
    Tile(modifier = modifier, description = "$heading $temperature") {
        TileHeading(heading, IosIcons.CloudSunRain, uppercase = false)
        Text(
            temperature,
            style = number(if (temperature.length < 4) 72 else 54),
            color = Ios.colors.label,
            maxLines = 1,
        )
    }
}

/** iOS's `HumidityCompactWidget`: the percentage as a large title, and the dew point spelled out under it. */
@Composable
private fun HumidityTile(humidity: Int, dewPoint: String?, modifier: Modifier = Modifier) {
    val heading = stringResource(Res.string.pangea_env_humidity)
    val dewLine = dewPoint?.let { stringResource(Res.string.pangea_env_dew_point_now, it) }
    Tile(modifier = modifier, description = listOfNotNull(heading, "$humidity%", dewLine).joinToString(" ")) {
        TileHeading(heading, MeshtasticIcons.Humidity)
        Text(
            "$humidity%",
            style = Ios.type.largeTitle.copy(fontWeight = FontWeight.Normal),
            color = Ios.colors.label,
            modifier = Modifier.padding(bottom = 5.dp),
        )
        if (dewLine != null) {
            Text(dewLine, style = Ios.type.caption2, color = Ios.colors.label, maxLines = 3)
        }
    }
}

/** iOS's `PressureCompactWidget`: the reading, whether it is low or high, and the unit on its own line. */
@Composable
private fun PressureTile(pressure: String, low: Boolean, modifier: Modifier = Modifier) {
    val heading = stringResource(Res.string.pressure)
    val level = stringResource(if (low) Res.string.pangea_env_pressure_low else Res.string.pangea_env_pressure_high)
    Tile(modifier = modifier, description = "$heading $pressure $PRESSURE_UNIT $level") {
        TileHeading(heading, IosIcons.Dial)
        Text(pressure, style = number(if (pressure.length < 7) 35 else 30), color = Ios.colors.label, maxLines = 1)
        Text(level, style = Ios.type.body, color = Ios.colors.label, modifier = Modifier.padding(bottom = 10.dp))
        Text(PRESSURE_UNIT, style = Ios.type.body, color = Ios.colors.label)
    }
}

/** iOS's `WindCompactWidget`: where the wind is from, its speed, and the gusts when there are any. */
@Composable
private fun WindTile(speed: String, gust: String?, direction: String, modifier: Modifier = Modifier) {
    val heading = stringResource(Res.string.wind)
    val gustLine = gust?.let { stringResource(Res.string.pangea_env_gusts, it) }
    Tile(modifier = modifier, description = listOfNotNull(heading, direction, speed, gustLine).joinToString(" ")) {
        TileHeading(heading, MeshtasticIcons.WindDirection, style = Ios.type.body)
        Text(
            direction,
            style = if (gust == null) Ios.type.callout else Ios.type.caption1,
            color = Ios.colors.label,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        Text(speed, style = number(35), color = Ios.colors.label, maxLines = 1)
        if (gustLine != null) Text(gustLine, style = Ios.type.body, color = Ios.colors.label)
    }
}

/**
 * iOS's tiles for one number and its unit (rainfall, radiation, weight, distance, soil): the value at 50 pt, or
 * [smallerValueSize] once it runs to four characters, with the unit small beside it.
 */
@Composable
private fun ValueTile(
    title: String,
    icon: ImageVector?,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    smallerValueSize: Int = 40,
) {
    Tile(modifier = modifier, description = "$title $value $unit") {
        if (icon != null) {
            TileHeading(title, icon, style = Ios.type.callout)
        } else {
            // Radiation's heading leads with the trefoil sign rather than a symbol.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("☢", style = number(30).copy(fontFamily = FontFamily.Monospace), color = Ios.colors.label)
                Text(title.uppercase(), style = Ios.type.callout, color = Ios.colors.label, maxLines = 1)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                value,
                style = number(if (value.length < 4) 50 else smallerValueSize),
                color = Ios.colors.label,
                maxLines = 1,
            )
            Text(unit, style = Ios.type.footnote.copy(fontSize = 14.sp), color = Ios.colors.label, maxLines = 1)
        }
    }
}

/**
 * A tile's heading: the symbol in the tint and the name beside it, in capitals unless it already is an abbreviation.
 */
@Composable
private fun TileHeading(
    title: String,
    icon: ImageVector,
    uppercase: Boolean = true,
    style: TextStyle = Ios.type.caption1,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(icon, contentDescription = null, tint = Ios.colors.tint, modifier = Modifier.size(18.dp))
        Text(
            if (uppercase) title.uppercase() else title,
            style = style,
            color = Ios.colors.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The tile itself: iOS's `MeshtasticTile` fill, 20 pt corners, 16 pt of padding, at least 152 pt tall, with its
 * contents centred as a left-aligned block. Read out as one item, [description].
 */
@Composable
private fun Tile(description: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val tile = if (Ios.colors.isDark) TILE_DARK else TILE_LIGHT
    Box(
        modifier
            .heightIn(min = TILE_MIN_HEIGHT)
            .clip(RoundedCornerShape(20.dp))
            .background(tile)
            .padding(16.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.clearAndSetSemantics {},
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content,
        )
    }
}

/** San Francisco's display sizes for the big numbers, set in the app's own face. */
@Composable
private fun number(size: Int): TextStyle =
    Ios.type.body.copy(fontSize = size.sp, lineHeight = (size * 1.2f).sp, letterSpacing = (-0.02f).em)

/** The temperature without its unit letter, as iOS's short format gives it ("60°"). */
private fun shortTemperature(celsius: Float, isFahrenheit: Boolean): String {
    val shown = if (isFahrenheit) UnitConversions.celsiusToFahrenheit(celsius) else celsius
    return "${shown.roundToInt()}°"
}

/** Wind as iOS writes it: whole numbers, mph or km/h. */
private fun windSpeed(metersPerSecond: Float, imperial: Boolean): String = if (imperial) {
    "${UnitConversions.metersPerSecondToMph(metersPerSecond).roundToInt()} mph"
} else {
    "${UnitConversions.metersPerSecondToKph(metersPerSecond).roundToInt()} km/h"
}

/** A length sent in millimetres: whole millimetres, or inches to one place. */
private fun smallLength(millimeters: Float, imperial: Boolean): Pair<String, String> = if (imperial) {
    NumberFormatter.format(millimeters / MM_PER_INCH, 1) to "in"
} else {
    millimeters.roundToInt().toString() to "mm"
}

/** The eight-point compass name for a bearing in degrees. */
private fun cardinal(degrees: Int): String {
    val points = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val normalized = ((degrees % 360) + 360) % 360
    return points[((normalized + 22.5f) / 45f).toInt() % points.size]
}

/** iOS calls a reading at or under this "LOW" (one standard atmosphere is 1013.25 hPa). */
private const val LOW_PRESSURE_HPA = 1009.144f

/** iOS writes the unit this way. */
private const val PRESSURE_UNIT = "hPA"
private const val LBS_PER_KG = 2.20462f
private const val MM_PER_INCH = 25.4f

private val TILE_GAP = 10.dp
private val TILE_MIN_HEIGHT = 152.dp

/** `Colors/MeshtasticTile`: a half-transparent grey-blue in Light Mode, a solid slate in Dark Mode. */
private val TILE_LIGHT = Color(0x80D1D3DE)
private val TILE_DARK = Color(0xFF2C2D3C)
