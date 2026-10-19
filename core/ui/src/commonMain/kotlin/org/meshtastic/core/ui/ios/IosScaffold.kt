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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.navigate_back

/**
 * Room the floating tab bar (and the status pill above it) takes at the bottom of the screen. Scrolling screens pad
 * their last item by this, so it can scroll clear of the bar as on iOS, where content runs underneath.
 */
@Suppress("CompositionLocalAllowlist")
val LocalIosBottomInset = compositionLocalOf { 0.dp }

/** Height of the top bar row that holds the glass buttons (below the status bar). */
private val TopBarHeight = 60.dp

/**
 * A screen with an iOS large title, like Chats and Settings: glass buttons at the top ([leading], [trailing]), the 34
 * dp bold [title] under them, and a list. Scrolling the title away fades a 17 dp inline title into the top bar.
 * [header] sits under the large title and scrolls with it (a search field, a "Configuration for:" card).
 */
@Composable
fun IosLargeTitleScreen(
    title: String,
    modifier: Modifier = Modifier,
    grouped: Boolean = true,
    listState: LazyListState = rememberLazyListState(),
    leading: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: LazyListScope.() -> Unit,
) {
    val c = Ios.colors
    val background = if (grouped) c.groupedBackground else c.background
    val collapsed by
        remember(listState) {
            derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 90 }
        }
    IosDrawsUnderTabBar()
    val bottom = LocalIosBottomInset.current
    Box(modifier.fillMaxSize().background(background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding =
            PaddingValues(
                top = contentPadding.calculateTopPadding(),
                bottom = bottom + contentPadding.calculateBottomPadding() + 24.dp,
            ),
        ) {
            item(key = "ios-large-title") {
                Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                    Spacer(Modifier.height(TopBarHeight))
                    Text(
                        title,
                        style = Ios.type.largeTitle,
                        color = c.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = IosInset, end = IosInset, top = 2.dp, bottom = 6.dp),
                    )
                }
            }
            content()
        }
        IosTopBar(title = title, showTitle = collapsed, background = background, leading = leading, trailing = trailing)
    }
}

/**
 * A pushed screen: a glass back button, an inline [title] (or [titleContent], such as a chat's avatar and name), and
 * optional [trailing] glass buttons; [largeTitle] adds the large title under the bar as iOS settings pages do.
 */
@Composable
fun IosPushedScreen(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    largeTitle: Boolean = false,
    grouped: Boolean = true,
    listState: LazyListState = rememberLazyListState(),
    titleContent: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    backLabel: String = stringResource(Res.string.navigate_back),
    content: LazyListScope.() -> Unit,
) {
    val c = Ios.colors
    val background = if (grouped) c.groupedBackground else c.background
    val collapsed by
        remember(listState, largeTitle) {
            derivedStateOf {
                !largeTitle || listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 90
            }
        }
    IosDrawsUnderTabBar()
    val bottom = LocalIosBottomInset.current
    Box(modifier.fillMaxSize().background(background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottom + 24.dp),
        ) {
            item(key = "ios-pushed-top") {
                Column(Modifier.windowInsetsPadding(WindowInsets.statusBars)) {
                    Spacer(Modifier.height(TopBarHeight))
                    if (largeTitle) {
                        Text(
                            title,
                            style = Ios.type.largeTitle,
                            color = c.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = IosInset, end = IosInset, top = 2.dp, bottom = 6.dp),
                        )
                    }
                }
            }
            content()
        }
        IosTopBar(
            title = title,
            showTitle = collapsed,
            background = background,
            titleContent = titleContent,
            leading = { IosGlassIconButton(IosIcons.ChevronLeft, contentDescription = backLabel, onClick = onBack) },
            trailing = trailing,
        )
    }
}

/**
 * The top bar: glass buttons over a fade of the screen's background, and the inline title in the middle once
 * [showTitle] is true. Used by both scaffolds; use it directly for screens that aren't a list (a chat).
 */
@Composable
fun IosTopBar(
    title: String,
    showTitle: Boolean,
    background: Color,
    modifier: Modifier = Modifier,
    titleContent: (@Composable () -> Unit)? = null,
    leading: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val titleAlpha by animateFloatAsState(if (showTitle) 1f else 0f, label = "iosInlineTitle")
    Box(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(0f to background, 0.8f to background, 1f to background.copy(alpha = 0f)))
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(TopBarHeight)
            .padding(horizontal = IosInset),
    ) {
        Row(Modifier.align(Alignment.CenterStart), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            leading?.invoke(this)
        }
        Box(
            Modifier.align(Alignment.Center)
                .padding(horizontal = 64.dp)
                .alpha(if (titleContent != null) 1f else titleAlpha),
        ) {
            if (titleContent != null) {
                titleContent()
            } else {
                Text(
                    title,
                    style = Ios.type.headline,
                    color = Ios.colors.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(Modifier.align(Alignment.CenterEnd), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            trailing?.invoke(this)
        }
    }
}

/**
 * The top of an iOS sheet: **Cancel** at the left (when [cancelLabel] is set), the [title] centred, and an optional
 * emphasized action at the right ([actionLabel], e.g. **Done**).
 */
@Composable
fun IosSheetHeader(
    title: String,
    cancelLabel: String?,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionEnabled: Boolean = true,
    onAction: () -> Unit = {},
    horizontalPadding: Dp = IosInset,
) {
    Box(modifier.fillMaxWidth().height(64.dp).padding(horizontal = horizontalPadding)) {
        if (cancelLabel != null) {
            IosGlassTextButton(cancelLabel, onClick = onCancel, modifier = Modifier.align(Alignment.CenterStart))
        }
        Text(title, style = Ios.type.headline, color = Ios.colors.label, modifier = Modifier.align(Alignment.Center))
        if (actionLabel != null) {
            IosGlassTextButton(
                actionLabel,
                onClick = onAction,
                emphasized = true,
                enabled = actionEnabled,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}
