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
package org.meshtastic.feature.map.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.add_custom_tile_source
import org.meshtastic.core.resources.add_local_mbtiles_file
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.delete_custom_tile_source
import org.meshtastic.core.resources.edit_custom_tile_source
import org.meshtastic.core.resources.local_mbtiles_file
import org.meshtastic.core.resources.manage_custom_tile_sources
import org.meshtastic.core.resources.name
import org.meshtastic.core.resources.name_cannot_be_empty
import org.meshtastic.core.resources.no_custom_tile_sources_found
import org.meshtastic.core.resources.provider_name_exists
import org.meshtastic.core.resources.save
import org.meshtastic.core.resources.url_cannot_be_empty
import org.meshtastic.core.resources.url_must_contain_placeholders
import org.meshtastic.core.resources.url_template
import org.meshtastic.core.resources.url_template_hint
import org.meshtastic.core.ui.component.MeshtasticDialog
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosAccessory
import org.meshtastic.core.ui.ios.IosButton
import org.meshtastic.core.ui.ios.IosButtonStyle
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosRow
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.ios.IosTextField
import org.meshtastic.feature.map.tiles.CustomTileProviderConfig
import org.meshtastic.feature.map.tiles.isValidTileUrlTemplate

@Suppress("LongMethod", "LongParameterList")
@Composable
fun CustomTileProviderManager(
    providers: List<CustomTileProviderConfig>,
    onAdd: (CustomTileProviderConfig) -> Unit,
    onUpdate: (CustomTileProviderConfig) -> Unit,
    onDelete: (String) -> Unit,
    onAddLocalMbTiles: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var editingConfig by remember { mutableStateOf<CustomTileProviderConfig?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    if (showEditDialog) {
        AddEditCustomTileProviderDialog(
            config = editingConfig,
            providers = providers,
            onDismiss = { showEditDialog = false },
            onSave = { config ->
                if (editingConfig == null) onAdd(config) else onUpdate(config)
                showEditDialog = false
            },
        )
    }

    // iOS's sheet layout: a centred title, the sources in one card, then the add buttons.
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Text(
                text = stringResource(Res.string.manage_custom_tile_sources),
                style = Ios.type.headline,
                color = Ios.colors.label,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
            )
        }

        item {
            IosSection {
                if (providers.isEmpty()) {
                    IosRow(
                        title = stringResource(Res.string.no_custom_tile_sources_found),
                        titleColor = Ios.colors.secondaryLabel,
                    )
                }
                providers.forEach { config ->
                    IosRow(
                        title = config.name,
                        subtitle =
                        if (config.isLocal) stringResource(Res.string.local_mbtiles_file) else config.urlTemplate,
                        accessory =
                        IosAccessory.Custom {
                            Row {
                                if (!config.isLocal) {
                                    IconButton(
                                        onClick = {
                                            editingConfig = config
                                            showEditDialog = true
                                        },
                                    ) {
                                        Icon(
                                            IosIcons.Compose,
                                            contentDescription = stringResource(Res.string.edit_custom_tile_source),
                                            tint = Ios.colors.tint,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                                IconButton(onClick = { onDelete(config.id) }) {
                                    Icon(
                                        IosIcons.Trash,
                                        contentDescription = stringResource(Res.string.delete_custom_tile_source),
                                        tint = Ios.colors.systemRed,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IosButton(
                    text = stringResource(Res.string.add_custom_tile_source),
                    onClick = {
                        editingConfig = null
                        showEditDialog = true
                    },
                    style = IosButtonStyle.Tinted,
                    large = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                onAddLocalMbTiles?.let { onAddLocal ->
                    IosButton(
                        text = stringResource(Res.string.add_local_mbtiles_file),
                        onClick = onAddLocal,
                        style = IosButtonStyle.Tinted,
                        large = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun AddEditCustomTileProviderDialog(
    config: CustomTileProviderConfig?,
    providers: List<CustomTileProviderConfig>,
    onDismiss: () -> Unit,
    onSave: (CustomTileProviderConfig) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(config?.name ?: "") }
    var url by rememberSaveable { mutableStateOf(config?.urlTemplate ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var urlError by remember { mutableStateOf<String?>(null) }

    val emptyNameError = stringResource(Res.string.name_cannot_be_empty)
    val providerNameExistsError = stringResource(Res.string.provider_name_exists)
    val urlCannotBeEmptyError = stringResource(Res.string.url_cannot_be_empty)
    val urlMustContainPlaceholdersError = stringResource(Res.string.url_must_contain_placeholders)

    fun validateAndSave() {
        nameError = validateName(name, providers, config?.id, emptyNameError, providerNameExistsError)
        urlError = validateUrl(url, urlCannotBeEmptyError, urlMustContainPlaceholdersError)
        if (nameError == null && urlError == null) {
            onSave(
                (config ?: CustomTileProviderConfig(name = name, urlTemplate = url))
                    .copy(name = name, urlTemplate = url)
                    .normalized(),
            )
        }
    }

    MeshtasticDialog(
        onDismiss = onDismiss,
        title =
        stringResource(
            if (config == null) Res.string.add_custom_tile_source else Res.string.edit_custom_tile_source,
        ),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                IosTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    placeholder = stringResource(Res.string.name),
                )
                nameError?.let { Text(it, style = Ios.type.footnote, color = Ios.colors.systemRed) }
                IosTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        urlError = null
                    },
                    placeholder = stringResource(Res.string.url_template),
                )
                Text(
                    text = urlError ?: stringResource(Res.string.url_template_hint),
                    style = Ios.type.footnote,
                    color = if (urlError != null) Ios.colors.systemRed else Ios.colors.secondaryLabel,
                )
            }
        },
        onConfirm = ::validateAndSave,
        confirmTextRes = Res.string.save,
        dismissTextRes = Res.string.cancel,
    )
}

private fun validateName(
    name: String,
    providers: List<CustomTileProviderConfig>,
    currentId: String?,
    emptyNameError: String,
    nameExistsError: String,
): String? = when {
    name.isBlank() -> emptyNameError
    providers.any { it.id != currentId && it.name.trim().equals(name.trim(), ignoreCase = true) } -> nameExistsError
    else -> null
}

private fun validateUrl(url: String, emptyUrlError: String, missingPlaceholdersError: String): String? = when {
    url.isBlank() -> emptyUrlError
    !url.isValidTileUrlTemplate(requireHttps = false) -> missingPlaceholdersError
    else -> null
}
