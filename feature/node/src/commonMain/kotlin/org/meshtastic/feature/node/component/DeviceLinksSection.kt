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
package org.meshtastic.feature.node.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.DeviceLink
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.collapsed
import org.meshtastic.core.resources.device_links_affiliate_disclosure
import org.meshtastic.core.resources.device_links_i_want_one
import org.meshtastic.core.resources.device_links_open_in_browser
import org.meshtastic.core.resources.expanded
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosAccessory
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection

/**
 * Collapsible "I want one" section listing msh.to vendor/variant and marketplace links for the viewed device, drawn as
 * the iPhone app's own: a tinted row with a chevron that opens the links. Renders nothing when there are no matching
 * links. Ported from the Meshtastic-Apple `DeviceLinksSection`.
 */
@Composable
fun DeviceLinksSection(links: List<DeviceLink>, modifier: Modifier = Modifier) {
    if (links.isEmpty()) return

    var expanded by rememberSaveable { mutableStateOf(false) }
    val title = stringResource(Res.string.device_links_i_want_one)
    val expandStateDescription = stringResource(if (expanded) Res.string.expanded else Res.string.collapsed)
    val c = Ios.colors

    IosSection(modifier = modifier) {
        IosRow(
            title = title,
            titleColor = c.tint,
            titleStyle = Ios.type.headline,
            modifier =
            Modifier.semantics {
                stateDescription = expandStateDescription
                heading()
            },
            accessory =
            IosAccessory.Custom {
                Icon(
                    IosIcons.ChevronRight,
                    contentDescription = null,
                    tint = c.tint,
                    modifier = Modifier.size(13.dp).rotate(if (expanded) CHEVRON_UP else CHEVRON_DOWN),
                )
            },
            onClick = { expanded = !expanded },
        )
        if (expanded) {
            // The disclosure sits above the links it covers.
            Text(
                text = stringResource(Res.string.device_links_affiliate_disclosure),
                style = Ios.type.footnote,
                color = c.secondaryLabel,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            )
            links.forEach { DeviceLinkRow(it) }
        }
    }
}

@Composable
private fun DeviceLinkRow(link: DeviceLink) {
    val uriHandler = LocalUriHandler.current
    // Vendor and product-variant links are emphasized; marketplace links (region-tagged) are quieter.
    val prominent = link.isVendor || link.regions == null
    val openLabel = stringResource(Res.string.device_links_open_in_browser)
    val label = link.description ?: link.shortCode
    val c = Ios.colors

    IosRow(
        title = label,
        titleStyle = if (prominent) Ios.type.body.copy(fontWeight = FontWeight.SemiBold) else Ios.type.subheadline,
        modifier = Modifier.semantics { contentDescription = "$openLabel: $label" },
        accessory =
        IosAccessory.Custom {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(IosIcons.Globe, contentDescription = null, tint = c.tint, modifier = Modifier.size(20.dp))
            }
        },
        onClick = { uriHandler.openUri(link.url) },
    )
}

/** The disclosure chevron is the right chevron turned to point down, or up while open. */
private const val CHEVRON_DOWN = 90f
private const val CHEVRON_UP = -90f
