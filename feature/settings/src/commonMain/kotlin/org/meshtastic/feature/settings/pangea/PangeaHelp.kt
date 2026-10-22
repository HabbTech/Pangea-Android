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
package org.meshtastic.feature.settings.pangea

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.help_and_documentation
import org.meshtastic.core.resources.pangea_help_apple_watch
import org.meshtastic.core.resources.pangea_help_connecting_radios
import org.meshtastic.core.resources.pangea_help_contacts_profiles
import org.meshtastic.core.resources.pangea_help_expert_section
import org.meshtastic.core.resources.pangea_help_faq
import org.meshtastic.core.resources.pangea_help_firmware_updates
import org.meshtastic.core.resources.pangea_help_footer
import org.meshtastic.core.resources.pangea_help_getting_started
import org.meshtastic.core.resources.pangea_help_glossary
import org.meshtastic.core.resources.pangea_help_guides
import org.meshtastic.core.resources.pangea_help_limits
import org.meshtastic.core.resources.pangea_help_local_mesh_discovery
import org.meshtastic.core.resources.pangea_help_logs_storage
import org.meshtastic.core.resources.pangea_help_map
import org.meshtastic.core.resources.pangea_help_messaging
import org.meshtastic.core.resources.pangea_help_mqtt
import org.meshtastic.core.resources.pangea_help_nodes
import org.meshtastic.core.resources.pangea_help_privacy_security
import org.meshtastic.core.resources.pangea_help_report_problem
import org.meshtastic.core.resources.pangea_help_search
import org.meshtastic.core.resources.pangea_help_settings
import org.meshtastic.core.resources.pangea_help_signal_meter
import org.meshtastic.core.resources.pangea_help_supported_hardware
import org.meshtastic.core.resources.pangea_help_tak
import org.meshtastic.core.resources.pangea_help_telemetry
import org.meshtastic.core.resources.pangea_help_troubleshooting
import org.meshtastic.core.resources.pangea_help_units_locale
import org.meshtastic.core.resources.pangea_help_whats_new
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosAccessory
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosInset
import org.meshtastic.core.ui.ios.IosPushedScreen
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSearchField
import org.meshtastic.core.ui.ios.IosSection

/** A guide in Pangea's documentation repository, with the symbol the iPhone app gives it. */
private data class HelpGuide(val path: String, val title: StringResource, val icon: () -> ImageVector)

/** Pangea's guides, as Pangea-Apple PangeaHelp lists them. */
object PangeaHelp {
    const val REPOSITORY = "https://github.com/HabbTech/Pangea-Documentation"
    const val REPORT_PROBLEM = "$REPOSITORY/issues/new/choose"

    fun page(path: String): String = "$REPOSITORY/blob/main/$path"
}

private val standardGuides =
    listOf(
        HelpGuide("docs/getting-started.md", Res.string.pangea_help_getting_started) { IosIcons.Flag },
        HelpGuide("docs/messaging.md", Res.string.pangea_help_messaging) { IosIcons.BubblesTwo },
        HelpGuide("docs/contacts-and-profiles.md", Res.string.pangea_help_contacts_profiles) {
            IosIcons.PersonCropCircle
        },
        HelpGuide("docs/privacy-and-security.md", Res.string.pangea_help_privacy_security) { IosIcons.LockOutline },
        HelpGuide("docs/settings.md", Res.string.pangea_help_settings) { IosIcons.Gear },
        HelpGuide("docs/supported-hardware.md", Res.string.pangea_help_supported_hardware) { IosIcons.Antenna },
        HelpGuide("docs/limits.md", Res.string.pangea_help_limits) { IosIcons.Dial },
        HelpGuide("docs/troubleshooting.md", Res.string.pangea_help_troubleshooting) { IosIcons.WrenchScrewdriver },
        HelpGuide("docs/faq.md", Res.string.pangea_help_faq) { IosIcons.QuestionCircle },
        HelpGuide("docs/glossary.md", Res.string.pangea_help_glossary) { IosIcons.DocText },
    )

private val expertGuides =
    listOf(
        HelpGuide("docs/expert/connecting-radios.md", Res.string.pangea_help_connecting_radios) { IosIcons.Antenna },
        HelpGuide("docs/expert/nodes.md", Res.string.pangea_help_nodes) { IosIcons.PointsConnected },
        HelpGuide("docs/expert/map.md", Res.string.pangea_help_map) { IosIcons.MapOutline },
        HelpGuide("docs/expert/signal-meter.md", Res.string.pangea_help_signal_meter) { IosIcons.CellularBars },
        HelpGuide("docs/expert/telemetry.md", Res.string.pangea_help_telemetry) { IosIcons.ChartLine },
        HelpGuide("docs/expert/local-mesh-discovery.md", Res.string.pangea_help_local_mesh_discovery) {
            IosIcons.RadiowavesLeftRight
        },
        HelpGuide("docs/expert/firmware-updates.md", Res.string.pangea_help_firmware_updates) {
            IosIcons.ArrowsUpDownSquare
        },
        HelpGuide("docs/expert/mqtt.md", Res.string.pangea_help_mqtt) { IosIcons.DotRadiowavesUpForward },
        HelpGuide("docs/expert/tak.md", Res.string.pangea_help_tak) { IosIcons.Target },
        HelpGuide("docs/expert/apple-watch.md", Res.string.pangea_help_apple_watch) { IosIcons.Clock },
        HelpGuide("docs/expert/units-and-locale.md", Res.string.pangea_help_units_locale) { IosIcons.Measurement },
        HelpGuide("docs/expert/logs-and-storage.md", Res.string.pangea_help_logs_storage) { IosIcons.Scroll },
    )

/**
 * Help & Documentation as the iPhone app shows it: a pushed page with a search field, Pangea's guides with their
 * symbols (Expert Mode's own guides under them while it is on), then What's New and Report a Problem. Every row opens
 * the guide in the browser.
 */
@Composable
fun PangeaHelpScreen(expertMode: Boolean, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    var query by rememberSaveable { mutableStateOf("") }
    val guideTitles = (standardGuides + expertGuides).associate { it.path to stringResource(it.title) }
    fun matches(guide: HelpGuide) = query.isBlank() || guideTitles[guide.path].orEmpty().contains(query, true)
    val standard = standardGuides.filter(::matches)
    val expert = if (expertMode) expertGuides.filter(::matches) else emptyList()

    IosPushedScreen(
        title = stringResource(Res.string.help_and_documentation),
        onBack = onBack,
        modifier = modifier,
        largeTitle = true,
    ) {
        item(key = "search") {
            IosSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(Res.string.pangea_help_search),
                glass = false,
                modifier = Modifier.padding(horizontal = IosInset).padding(top = 4.dp),
            )
        }
        if (standard.isNotEmpty()) {
            item(key = "guides") {
                IosSection(header = stringResource(Res.string.pangea_help_guides)) {
                    standard.forEach { guide ->
                        HelpRow(stringResource(guide.title), guide.icon()) {
                            uriHandler.openUri(PangeaHelp.page(guide.path))
                        }
                    }
                }
            }
        }
        if (expert.isNotEmpty()) {
            item(key = "expert") {
                IosSection(header = stringResource(Res.string.pangea_help_expert_section)) {
                    expert.forEach { guide ->
                        HelpRow(stringResource(guide.title), guide.icon()) {
                            uriHandler.openUri(PangeaHelp.page(guide.path))
                        }
                    }
                }
            }
        }
        if (query.isBlank()) {
            item(key = "more") {
                IosSection(footer = stringResource(Res.string.pangea_help_footer)) {
                    HelpRow(stringResource(Res.string.pangea_help_whats_new), IosIcons.WandStars) {
                        uriHandler.openUri(PangeaHelp.page("CHANGELOG.md"))
                    }
                    HelpRow(stringResource(Res.string.pangea_help_report_problem), IosIcons.MessageOutline) {
                        uriHandler.openUri(PangeaHelp.REPORT_PROBLEM)
                    }
                }
            }
        }
    }
}

/** A guide's row: its symbol in the label colour, its title, and the arrow iOS puts on a link that leaves the app. */
@Composable
private fun HelpRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    val c = Ios.colors
    IosRow(
        title = title,
        icon = icon,
        iconTint = c.label,
        accessory =
        IosAccessory.Custom {
            Icon(
                IosIcons.ArrowUpForward,
                contentDescription = null,
                tint = c.tertiaryLabel,
                modifier = Modifier.size(14.dp),
            )
        },
        onClick = onClick,
    )
}
