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

package org.meshtastic.core.ui.ios

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Press feedback the iOS way: no ripple; the control dims while held. */
@Composable
fun Modifier.iosPressable(enabled: Boolean = true, role: Role = Role.Button, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    return this.alpha(if (pressed) 0.55f else 1f)
        .clickable(interactionSource = source, indication = null, enabled = enabled, role = role, onClick = onClick)
}

enum class IosButtonStyle {
    /** `.borderedProminent`: tint fill, white text. The one main action on a screen. */
    Filled,

    /** `.bordered` with the tint: soft fill, tint text. The secondary action. */
    Tinted,

    /** Plain tinted text: an escape hatch like "I don't have my radio yet". */
    Plain,

    /** `.bordered` without a tint: grey fill, label-coloured words (Copy, Backup, Restore). */
    Gray,

    /** A destructive action (`role: .destructive`): grey fill, red words (Delete, Forget This Radio). */
    Destructive,
}

/**
 * An iOS button. [large] is `.controlSize(.large)`: full width, 50 dp tall, as the setup pages' bottom button. The
 * disabled look is iOS's grey fill with grey text.
 */
@Composable
fun IosButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: IosButtonStyle = IosButtonStyle.Filled,
    enabled: Boolean = true,
    large: Boolean = false,
    icon: ImageVector? = null,
    textStyle: TextStyle = Ios.type.body,
) {
    val c = Ios.colors
    val container =
        when {
            style == IosButtonStyle.Plain -> Color.Transparent
            !enabled || style == IosButtonStyle.Gray || style == IosButtonStyle.Destructive -> c.fieldFill
            style == IosButtonStyle.Filled -> c.tint
            else -> c.tint.copy(alpha = if (c.isDark) 0.24f else 0.15f)
        }
    val content =
        when {
            !enabled -> c.tertiaryLabel
            style == IosButtonStyle.Filled -> c.onTint
            style == IosButtonStyle.Gray -> c.label
            style == IosButtonStyle.Destructive -> c.systemRed
            else -> c.tint
        }
    Row(
        modifier =
        modifier
            .then(if (large) Modifier.fillMaxWidth().height(50.dp) else Modifier.defaultMinSize(minHeight = 34.dp))
            .clip(CircleShape)
            .background(container)
            .iosPressable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (large) 20.dp else 14.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            // The symbol scales with the words, as SF Symbols do.
            val iconSize = with(LocalDensity.current) { (textStyle.fontSize * ICON_TO_TEXT).toDp() }
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(iconSize))
            Spacer(Modifier.width(iconSize / 3))
        }
        Text(text, style = textStyle, color = content, textAlign = TextAlign.Center)
    }
}

/**
 * The round "glass" buttons at the top of iOS 26 screens (back, compose, the logo, the bell): a 44 dp circle of
 * translucent white (dark grey in dark mode) with a soft shadow.
 */
@Composable
fun IosGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    val c = Ios.colors
    Box(
        modifier =
        modifier
            .size(44.dp)
            .shadow(12.dp, CircleShape, ambientColor = c.chromeShadow, spotColor = c.chromeShadow)
            .clip(CircleShape)
            .background(c.chrome)
            .iosPressable(onClick = onClick)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** A glass button showing [icon] in [tint] (the label colour by default). */
@Composable
fun IosGlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Ios.colors.label,
) {
    IosGlassButton(onClick = onClick, modifier = modifier, contentDescription = contentDescription) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** A glass capsule with a word in it: a sheet's **Cancel** or **Done**. */
@Composable
fun IosGlassTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    enabled: Boolean = true,
) {
    val c = Ios.colors
    Box(
        modifier =
        modifier
            .height(40.dp)
            .shadow(10.dp, CircleShape, ambientColor = c.chromeShadow, spotColor = c.chromeShadow)
            .clip(CircleShape)
            .background(c.chrome)
            .iosPressable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = if (emphasized) Ios.type.headline else Ios.type.body,
            color = if (enabled) c.tint else c.tertiaryLabel,
        )
    }
}

/** An iOS switch: 51×31 track, white 27 dp thumb, the tint when on. */
@Composable
fun IosSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = Ios.colors
    val track by
        animateColorAsState(
            if (checked) {
                c.tint
            } else if (c.isDark) {
                Color(0xFF39393D)
            } else {
                Color(0xFFE9E9EA)
            },
            label = "iosSwitchTrack",
        )
    val thumbX by animateDpAsState(if (checked) 22.dp else 2.dp, label = "iosSwitchThumb")
    Box(
        modifier =
        modifier
            .size(width = 51.dp, height = 31.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(CircleShape)
            .background(track)
            .then(
                if (onCheckedChange != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = enabled,
                        role = Role.Switch,
                    ) {
                        onCheckedChange(!checked)
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        Box(
            Modifier.offset(x = thumbX, y = 2.dp)
                .size(27.dp)
                .shadow(3.dp, CircleShape, ambientColor = Color(0x40000000), spotColor = Color(0x40000000))
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

/** The capsule search field (`.searchable`), like "Find in conversation". */
@Composable
fun IosSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    glass: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = Ios.colors
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(
                if (glass) {
                    Modifier.shadow(10.dp, CircleShape, ambientColor = c.chromeShadow, spotColor = c.chromeShadow)
                } else {
                    Modifier
                },
            )
            .clip(CircleShape)
            .background(if (glass) c.chrome else c.searchFill)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(IosIcons.Search, contentDescription = null, tint = c.label, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, style = Ios.type.body, color = c.secondaryLabel)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = Ios.type.body.copy(color = c.label),
                cursorBrush = SolidColor(c.tint),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        trailing?.invoke()
    }
}

/** A filled iOS text field (`tertiarySystemFill`, 12 dp corners), as in the sheets and setup. */
@Composable
fun IosTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = Ios.type.body,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textAlign: TextAlign = TextAlign.Start,
    containerColor: Color = Ios.colors.fieldFill,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val c = Ios.colors
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    style = textStyle,
                    color = c.tertiaryLabel,
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = textStyle.copy(color = c.label, textAlign = textAlign),
                cursorBrush = SolidColor(c.tint),
                keyboardOptions = keyboardOptions,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        trailing?.invoke(this)
    }
}

/** The iOS activity indicator: eight fading spokes turning in steps. */
@Composable
fun IosActivityIndicator(modifier: Modifier = Modifier, color: Color = Ios.colors.tint, size: Dp = 20.dp) {
    val turn by
        rememberInfiniteTransition(label = "iosSpinner")
            .animateFloat(
                initialValue = 0f,
                targetValue = 8f,
                animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Restart),
                label = "iosSpinnerTurn",
            )
    Canvas(modifier.size(size).rotate((turn.toInt() % 8) * 45f)) {
        val r = this.size.minDimension / 2
        for (i in 0 until 8) {
            val a = (i * 45f - 90f) * (kotlin.math.PI.toFloat() / 180f)
            val dir = Offset(kotlin.math.cos(a), kotlin.math.sin(a))
            drawLine(
                color = color.copy(alpha = 1f - i * 0.11f),
                start = center + dir * (r * 0.45f),
                end = center + dir * (r * 0.95f),
                strokeWidth = r * 0.2f,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * The radio status pill above the tab bar in standard mode ("Connecting to your radio…" with a spinner, or "No radio
 * paired" with **Pair Radio**).
 */
@Composable
fun IosStatusPill(
    text: String,
    modifier: Modifier = Modifier,
    busy: Boolean = false,
    leading: ImageVector? = null,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val c = Ios.colors
    // iOS's banner is `.regularMaterial` in a capsule: a soft grey, no shadow. With a button it spans the screen less
    // 16 dp each side and pushes the button to the end; without one it hugs its text.
    val material = if (c.isDark) Color(0xF2252527) else Color(0xF2F5F5F5)
    Row(
        modifier =
        modifier
            .then(if (actionLabel != null) Modifier.fillMaxWidth().padding(horizontal = 16.dp) else Modifier)
            .clip(CircleShape)
            .background(material)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .defaultMinSize(minHeight = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            busy -> IosActivityIndicator(size = 18.dp)
            leading != null -> Icon(leading, null, tint = c.secondaryLabel, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = Ios.type.subheadline,
            color = c.label,
            modifier = Modifier.weight(1f, fill = actionLabel != null),
        )
        if (actionLabel != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.height(27.dp)
                    .clip(CircleShape)
                    .background(c.tint)
                    .iosPressable(onClick = onAction)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(actionLabel, style = Ios.type.subheadline.copy(fontWeight = FontWeight.Bold), color = c.onTint)
            }
        }
    }
}

/** The red count badge on a tab, and the tint count badge on a chat row. */
@Composable
fun IosBadge(count: Int, modifier: Modifier = Modifier, color: Color = Ios.colors.systemRed) {
    Box(
        modifier
            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
            .clip(CircleShape)
            .background(color)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (count > 99) "99+" else count.toString(),
            style = Ios.type.footnote.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
        )
    }
}

/** A button symbol's size against its text, about 19 dp beside 17 sp body text. */
private const val ICON_TO_TEXT = 1.12f
