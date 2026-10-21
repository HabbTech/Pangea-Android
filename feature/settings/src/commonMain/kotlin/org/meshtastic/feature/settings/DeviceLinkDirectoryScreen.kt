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
package org.meshtastic.feature.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.device_links
import org.meshtastic.core.resources.device_links_affiliate_disclosure
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosAccessory
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosPushedScreen
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection

/** Directory of every imported msh.to short code. Tapping a row opens `msh.to/{shortCode}` in the browser. */
@Composable
fun DeviceLinkDirectoryScreen(
    viewModel: DeviceLinkDirectoryViewModel,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val links by viewModel.links.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    // iOS's Device Links page: one card, the affiliate note first, then each link in the tint with a compass.
    IosPushedScreen(
        title = stringResource(Res.string.device_links),
        onBack = onNavigateUp,
        modifier = modifier,
        largeTitle = true,
    ) {
        item {
            IosSection {
                if (links.isNotEmpty()) {
                    Text(
                        text = stringResource(Res.string.device_links_affiliate_disclosure),
                        style = Ios.type.subheadline,
                        color = Ios.colors.secondaryLabel,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp),
                    )
                }
                links.forEach { link ->
                    IosRow(
                        title = link.description ?: link.shortCode,
                        titleColor = Ios.colors.tint,
                        subtitle = "msh.to/${link.shortCode}",
                        subtitleColor = Ios.colors.tint.copy(alpha = LINK_ADDRESS_ALPHA),
                        accessory =
                        IosAccessory.Custom {
                            Icon(
                                IosIcons.Safari,
                                contentDescription = null,
                                tint = Ios.colors.tint,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        onClick = { uriHandler.openUri(link.url) },
                    )
                }
            }
        }
    }
}

/** iOS draws the link address in a lighter tint than its name. */
private const val LINK_ADDRESS_ALPHA = 0.6f
