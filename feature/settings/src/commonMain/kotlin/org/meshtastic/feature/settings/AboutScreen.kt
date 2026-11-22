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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.about
import org.meshtastic.core.resources.acknowledgements
import org.meshtastic.core.resources.apps
import org.meshtastic.core.resources.copyright_notice
import org.meshtastic.core.resources.documentation
import org.meshtastic.core.resources.github_repository
import org.meshtastic.core.resources.license
import org.meshtastic.core.resources.license_notice
import org.meshtastic.core.resources.pangea_buy_radios
import org.meshtastic.core.resources.pangea_buy_radios_description
import org.meshtastic.core.resources.pangea_copyright_notice
import org.meshtastic.core.resources.pangea_solar_node
import org.meshtastic.core.resources.pangea_version_line
import org.meshtastic.core.resources.pangea_what_is
import org.meshtastic.core.resources.pangea_what_is_description
import org.meshtastic.core.resources.project_information
import org.meshtastic.core.resources.website
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosAccessory
import org.meshtastic.core.ui.ios.IosInset
import org.meshtastic.core.ui.ios.IosPushedColumnScreen
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.ios.iosPressable
import org.meshtastic.core.ui.ios.iosRowSeparator
import org.meshtastic.core.ui.theme.AppTheme

// Pangea (Pangea-Apple About.swift): complete radios from AlloNode, habb.tech, and Pangea's own guides. The source
// link stays, pointed at this fork, as the GPL notice below asks.
private const val HARDWARE_URL = "https://www.etsy.com/shop/AlloNode"
private const val GITHUB_REPO_URL = "https://github.com/HabbTech/Pangea-Android"
private const val WEBSITE_URL = "https://habb.tech/"
private const val DOCS_URL = "https://github.com/HabbTech/Pangea-Documentation/tree/main"
private const val LICENSE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"

/**
 * About, drawn like the Pangea iPhone app: what Pangea is, the AlloNode radios, the version, and the project links.
 * Android keeps its source link, acknowledgements and licence rows and the copyright notices, as the GPL asks.
 */
@Composable
fun AboutScreen(
    appVersionName: String,
    onNavigateUp: () -> Unit,
    onNavigateToAcknowledgements: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val c = Ios.colors
    IosPushedColumnScreen(title = stringResource(Res.string.about), onBack = onNavigateUp, modifier = modifier) {
        IosSection(header = stringResource(Res.string.pangea_what_is)) {
            Text(
                stringResource(Res.string.pangea_what_is_description),
                style = Ios.type.title3,
                color = c.label,
                modifier = Modifier.padding(IosInset),
            )
        }
        IosSection(header = stringResource(Res.string.apps)) {
            BuyRadiosRow(onClick = { uriHandler.openUri(HARDWARE_URL) })
            // iOS's rows here (Sponsor, Review) are title3; its version line is plain body text.
            IosRow(
                title = stringResource(Res.string.github_repository),
                titleStyle = Ios.type.title3,
                accessory = IosAccessory.Chevron,
                onClick = { uriHandler.openUri(GITHUB_REPO_URL) },
            )
            IosRow(
                title = stringResource(Res.string.acknowledgements),
                titleStyle = Ios.type.title3,
                accessory = IosAccessory.Chevron,
                onClick = onNavigateToAcknowledgements,
            )
            IosRow(title = stringResource(Res.string.pangea_version_line, appVersionName))
        }
        IosSection(header = stringResource(Res.string.project_information)) {
            listOf(
                Res.string.website to WEBSITE_URL,
                Res.string.documentation to DOCS_URL,
                Res.string.license to LICENSE_URL,
            )
                .forEach { (label, url) ->
                    IosRow(
                        title = stringResource(label),
                        titleColor = c.tint,
                        titleStyle = Ios.type.title2,
                        onClick = { uriHandler.openUri(url) },
                    )
                }
        }
        CopyrightFooter()
    }
}

/** The AlloNode solar router with "Buy Complete Radios", as the iPhone app shows it. */
@Composable
private fun BuyRadiosRow(onClick: () -> Unit) {
    val c = Ios.colors
    Row(
        Modifier.iosRowSeparator()
            .fillMaxWidth()
            .iosPressable(onClick = onClick)
            .padding(horizontal = IosInset, vertical = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            painter = painterResource(Res.drawable.pangea_solar_node),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.width(75.dp).clip(RoundedCornerShape(5.dp)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(Res.string.pangea_buy_radios), style = Ios.type.title2, color = c.tint)
            Text(
                stringResource(Res.string.pangea_buy_radios_description),
                style = Ios.type.callout,
                color = c.tint.copy(alpha = 0.6f),
            )
        }
    }
}

// GPL section 5(d): this screen shows the copyright notice, the no-warranty statement and where the
// license is, so a modified version has to keep showing them too. Keep all three together here.
@Composable
private fun CopyrightFooter(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(Res.string.pangea_copyright_notice, Res.string.copyright_notice, Res.string.license_notice).forEach {
            Text(
                text = stringResource(it),
                modifier = Modifier.fillMaxWidth(),
                style = Ios.type.caption1,
                color = Ios.colors.secondaryLabel,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview
@Composable
private fun AboutScreenPreview() {
    AppTheme { AboutScreen(appVersionName = "2.5.0", onNavigateUp = {}, onNavigateToAcknowledgements = {}) }
}
