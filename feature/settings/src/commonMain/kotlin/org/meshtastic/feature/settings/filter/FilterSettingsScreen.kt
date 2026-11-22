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
package org.meshtastic.feature.settings.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.add
import org.meshtastic.core.resources.delete
import org.meshtastic.core.resources.filter_add_placeholder
import org.meshtastic.core.resources.filter_enable
import org.meshtastic.core.resources.filter_enable_summary
import org.meshtastic.core.resources.filter_no_words
import org.meshtastic.core.resources.filter_regex_pattern
import org.meshtastic.core.resources.filter_settings
import org.meshtastic.core.resources.filter_whole_word
import org.meshtastic.core.resources.filter_words
import org.meshtastic.core.resources.filter_words_summary
import org.meshtastic.core.ui.component.MainAppBar
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosCardShape
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosSwitch
import org.meshtastic.core.ui.ios.IosTextField

@Composable
fun FilterSettingsScreen(viewModel: FilterSettingsViewModel, onBack: () -> Unit) {
    val filterEnabled by viewModel.filterEnabled.collectAsStateWithLifecycle()
    val storedWords by viewModel.filterWords.collectAsStateWithLifecycle()
    val filterWords = remember(storedWords) { storedWords.sorted() }
    var newWord by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Ios.colors.groupedBackground,
        topBar = {
            MainAppBar(
                title = stringResource(Res.string.filter_settings),
                canNavigateUp = true,
                onNavigateUp = onBack,
                ourNode = null,
                showNodeChip = false,
                actions = {},
                onClickChip = {},
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { FilterEnableCard(filterEnabled) { viewModel.setFilterEnabled(it) } }
            item {
                FilterWordsInputCard(
                    newWord = newWord,
                    onNewWordChange = { newWord = it },
                    onAddWord = {
                        viewModel.addFilterWord(newWord)
                        newWord = ""
                    },
                )
            }
            if (filterWords.isEmpty()) {
                item {
                    Text(
                        stringResource(Res.string.filter_no_words),
                        style = Ios.type.footnote,
                        color = Ios.colors.secondaryLabel,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
            items(filterWords, key = { it }) { word ->
                FilterWordItem(word = word, onRemove = { viewModel.removeFilterWord(word) })
            }
        }
    }
}

@Composable
private fun FilterEnableCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clip(IosCardShape).background(Ios.colors.groupedRow)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.filter_enable), style = Ios.type.headline)
                Text(
                    stringResource(Res.string.filter_enable_summary),
                    style = Ios.type.footnote,
                    color = Ios.colors.secondaryLabel,
                )
            }
            IosSwitch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun FilterWordsInputCard(newWord: String, onNewWordChange: (String) -> Unit, onAddWord: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clip(IosCardShape).background(Ios.colors.groupedRow)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(Res.string.filter_words), style = Ios.type.headline)
            Text(
                stringResource(Res.string.filter_words_summary),
                style = Ios.type.footnote,
                color = Ios.colors.secondaryLabel,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IosTextField(
                    value = newWord,
                    onValueChange = onNewWordChange,
                    placeholder = stringResource(Res.string.filter_add_placeholder),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onAddWord) {
                    Icon(IosIcons.Plus, contentDescription = stringResource(Res.string.add), tint = Ios.colors.tint)
                }
            }
        }
    }
}

@Composable
private fun FilterWordItem(word: String, onRemove: () -> Unit) {
    val isRegex = word.startsWith("regex:")
    val displayText = if (isRegex) word.removePrefix("regex:") else word

    Column(modifier = Modifier.fillMaxWidth().clip(IosCardShape).background(Ios.colors.groupedRow)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = displayText, style = Ios.type.body, color = Ios.colors.label)
                Text(
                    text =
                    stringResource(if (isRegex) Res.string.filter_regex_pattern else Res.string.filter_whole_word),
                    style = Ios.type.footnote,
                    color = Ios.colors.secondaryLabel,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    IosIcons.Trash,
                    contentDescription = stringResource(Res.string.delete),
                    tint = Ios.colors.systemRed,
                )
            }
        }
    }
}
