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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.pangea_help_apple_watch
import org.meshtastic.core.resources.pangea_help_connecting_radios
import org.meshtastic.core.resources.pangea_help_contacts_profiles
import org.meshtastic.core.resources.pangea_help_expert_guides
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
import org.meshtastic.core.resources.pangea_help_settings
import org.meshtastic.core.resources.pangea_help_signal_meter
import org.meshtastic.core.resources.pangea_help_supported_hardware
import org.meshtastic.core.resources.pangea_help_tak
import org.meshtastic.core.resources.pangea_help_telemetry
import org.meshtastic.core.resources.pangea_help_troubleshooting
import org.meshtastic.core.resources.pangea_help_units_locale
import org.meshtastic.core.resources.pangea_help_whats_new

/** A guide in Pangea's documentation repository. */
private data class HelpGuide(val path: String, val title: StringResource)

/** Pangea's guides, as Pangea-Apple PangeaHelp lists them. */
object PangeaHelp {
    const val REPOSITORY = "https://github.com/HabbTech/Pangea-Documentation"
    const val REPORT_PROBLEM = "$REPOSITORY/issues/new/choose"

    fun page(path: String): String = "$REPOSITORY/blob/main/$path"
}

private val standardGuides =
    listOf(
        HelpGuide("docs/getting-started.md", Res.string.pangea_help_getting_started),
        HelpGuide("docs/messaging.md", Res.string.pangea_help_messaging),
        HelpGuide("docs/contacts-and-profiles.md", Res.string.pangea_help_contacts_profiles),
        HelpGuide("docs/privacy-and-security.md", Res.string.pangea_help_privacy_security),
        HelpGuide("docs/settings.md", Res.string.pangea_help_settings),
        HelpGuide("docs/supported-hardware.md", Res.string.pangea_help_supported_hardware),
        HelpGuide("docs/limits.md", Res.string.pangea_help_limits),
        HelpGuide("docs/troubleshooting.md", Res.string.pangea_help_troubleshooting),
        HelpGuide("docs/faq.md", Res.string.pangea_help_faq),
        HelpGuide("docs/glossary.md", Res.string.pangea_help_glossary),
    )

private val expertGuides =
    listOf(
        HelpGuide("docs/expert/connecting-radios.md", Res.string.pangea_help_connecting_radios),
        HelpGuide("docs/expert/nodes.md", Res.string.pangea_help_nodes),
        HelpGuide("docs/expert/map.md", Res.string.pangea_help_map),
        HelpGuide("docs/expert/signal-meter.md", Res.string.pangea_help_signal_meter),
        HelpGuide("docs/expert/telemetry.md", Res.string.pangea_help_telemetry),
        HelpGuide("docs/expert/local-mesh-discovery.md", Res.string.pangea_help_local_mesh_discovery),
        HelpGuide("docs/expert/firmware-updates.md", Res.string.pangea_help_firmware_updates),
        HelpGuide("docs/expert/mqtt.md", Res.string.pangea_help_mqtt),
        HelpGuide("docs/expert/tak.md", Res.string.pangea_help_tak),
        HelpGuide("docs/expert/apple-watch.md", Res.string.pangea_help_apple_watch),
        HelpGuide("docs/expert/units-and-locale.md", Res.string.pangea_help_units_locale),
        HelpGuide("docs/expert/logs-and-storage.md", Res.string.pangea_help_logs_storage),
    )

/** Help & Documentation: Pangea's guides, opened in the browser (Pangea-Apple "Point Help at Pangea's guides"). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PangeaHelpSheet(expertMode: Boolean, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            item { SectionTitle(Res.string.pangea_help_guides) }
            items(standardGuides, key = { it.path }) { guide ->
                HelpRow(stringResource(guide.title)) { uriHandler.openUri(PangeaHelp.page(guide.path)) }
            }
            if (expertMode) {
                item { SectionTitle(Res.string.pangea_help_expert_guides) }
                items(expertGuides, key = { it.path }) { guide ->
                    HelpRow(stringResource(guide.title)) { uriHandler.openUri(PangeaHelp.page(guide.path)) }
                }
            }
            item {
                HelpRow(stringResource(Res.string.pangea_help_whats_new)) {
                    uriHandler.openUri(PangeaHelp.page("CHANGELOG.md"))
                }
            }
            item {
                HelpRow(stringResource(Res.string.pangea_help_report_problem)) {
                    uriHandler.openUri(PangeaHelp.REPORT_PROBLEM)
                }
            }
            item {
                Text(
                    stringResource(Res.string.pangea_help_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: StringResource) {
    Text(
        stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun HelpRow(title: String, onClick: () -> Unit) {
    ListItem(headlineContent = { Text(title) }, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick))
}
