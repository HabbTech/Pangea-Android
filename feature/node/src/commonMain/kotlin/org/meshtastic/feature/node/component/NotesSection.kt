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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.Node
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.add_a_note
import org.meshtastic.core.resources.notes
import org.meshtastic.core.resources.save
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.ios.IosTextField

@Composable
fun NotesSection(node: Node, onSaveNotes: (Int, String) -> Unit, modifier: Modifier = Modifier) {
    // An iOS section: "Notes" as its header and the note as a field in its row, saved by Done or the tinted Save.
    if (node.isFavorite) {
        val originalNotes = node.notes
        var notes by remember(node.notes) { mutableStateOf(node.notes) }
        val edited = notes.trim() != originalNotes.trim()
        val keyboardController = LocalSoftwareKeyboardController.current
        val save = {
            onSaveNotes(node.num, notes.trim())
            keyboardController?.hide()
        }
        IosSection(header = stringResource(Res.string.notes), modifier = modifier) {
            IosTextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = stringResource(Res.string.add_a_note),
                containerColor = Color.Transparent,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
                trailing = {
                    Text(
                        text = stringResource(Res.string.save),
                        style = Ios.type.body,
                        color = if (edited) Ios.colors.tint else Ios.colors.tertiaryLabel,
                        modifier =
                        Modifier.clickable(enabled = edited, role = Role.Button) { save() }
                            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
                    )
                },
            )
        }
    }
}
