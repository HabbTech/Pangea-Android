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
package org.meshtastic.core.ui.pangea

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okio.ByteString.Companion.toByteString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinViewModel
import org.meshtastic.core.model.ConnectionState
import org.meshtastic.core.model.pangea.EmojiCatalog
import org.meshtastic.core.model.pangea.MyProfile
import org.meshtastic.core.model.pangea.PangeaCode
import org.meshtastic.core.model.pangea.ProfileCard
import org.meshtastic.core.model.pangea.ProfileRules
import org.meshtastic.core.model.pangea.RadioAutoConfig
import org.meshtastic.core.repository.ConnectionStateProvider
import org.meshtastic.core.repository.NodeRepository
import org.meshtastic.core.repository.PangeaProfileStore
import org.meshtastic.core.repository.PangeaProfileSync
import org.meshtastic.core.repository.PangeaRadioSetup
import org.meshtastic.core.repository.PhotoChange
import org.meshtastic.core.repository.RadioController
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.resources.name
import org.meshtastic.core.resources.pangea_all_emoji
import org.meshtastic.core.resources.pangea_choose_photo
import org.meshtastic.core.resources.pangea_color
import org.meshtastic.core.resources.pangea_display_name
import org.meshtastic.core.resources.pangea_edit_profile
import org.meshtastic.core.resources.pangea_my_profile
import org.meshtastic.core.resources.pangea_profile
import org.meshtastic.core.resources.pangea_profile_emoji
import org.meshtastic.core.resources.pangea_profile_emoji_footer
import org.meshtastic.core.resources.pangea_profile_name_footer
import org.meshtastic.core.resources.pangea_profile_save_restarts_radio
import org.meshtastic.core.resources.pangea_profile_shared_with
import org.meshtastic.core.resources.pangea_remove_photo
import org.meshtastic.core.resources.pangea_setup_profile
import org.meshtastic.core.resources.pangea_setup_profile_hint
import org.meshtastic.core.resources.save
import org.meshtastic.core.ui.component.AvatarPalette
import org.meshtastic.core.ui.component.PangeaAvatar
import org.meshtastic.core.ui.component.PangeaEmojiPicker
import org.meshtastic.core.ui.component.rememberAvatarBitmap
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosButton
import org.meshtastic.core.ui.ios.IosButtonStyle
import org.meshtastic.core.ui.ios.IosCard
import org.meshtastic.core.ui.ios.IosSection
import org.meshtastic.core.ui.ios.IosSheet
import org.meshtastic.core.ui.util.rememberAvatarPhotoPicker

/** What the profile screens show about me. */
data class MyProfileState(
    val profile: MyProfile = MyProfile(),
    val photo: ByteArray? = null,
    val radioName: String = "",
    val code: String? = null,
)

@KoinViewModel
class MyProfileViewModel(
    private val store: PangeaProfileStore,
    private val sync: PangeaProfileSync,
    private val radioSetup: PangeaRadioSetup,
    private val radioController: RadioController,
    nodeRepository: NodeRepository,
    connectionStateProvider: ConnectionStateProvider,
) : ViewModel() {
    val connectionState: StateFlow<ConnectionState> = connectionStateProvider.connectionState

    /**
     * Forget This Radio: disconnects and forgets the radio. Its chats stay in its own database for when it's paired
     * again; my card goes out fresh to whoever I talk to through the next one.
     */
    fun forgetRadio() = viewModelScope.launch {
        radioController.setDeviceAddress(NO_DEVICE)
        sync.reset()
    }

    val state: StateFlow<MyProfileState> =
        combine(store.me, nodeRepository.ourNodeInfo) { me, node ->
            MyProfileState(
                profile = me,
                photo = store.myPhotoData()?.toByteArray(),
                radioName = node?.user?.long_name.orEmpty(),
                code = node?.let { PangeaCode.code(it.num.toLong() and UINT32) },
            )
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), MyProfileState())

    /**
     * Saved here first, so nothing typed is lost; then the name goes to the radio and my card to the people and groups
     * I talk to (Pangea-Apple "Save profile locally before renaming the radio").
     */
    fun save(name: String, emoji: String, color: Int, photo: PhotoChange?) {
        val before = store.me.value
        val oldName = before.displayName.ifEmpty { state.value.radioName }
        store.updateMe(name, emoji, color, photo)
        val after = store.me.value
        val nameChanged = after.displayName.isNotEmpty() && after.displayName != oldName
        val actions =
            ProfileRules.afterSave(nameChanged, meChanged = after != before || photo != null, radioReady = true)
        viewModelScope.launch {
            if (ProfileRules.SaveAction.APPLY_NAME in actions) radioSetup.applyName(after.displayName)
            if (ProfileRules.SaveAction.PUSH in actions) sync.pushUpdatedCard()
        }
    }

    private companion object {
        const val NO_DEVICE = "n"
        const val UINT32 = 0xFFFFFFFFL
        const val STOP_TIMEOUT = 5_000L
    }
}

/** My avatar, name and @code at the top of Settings, with Edit Profile. */
@Composable
fun MyProfileSection(modifier: Modifier = Modifier, viewModel: MyProfileViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf(false) }
    val name = state.profile.displayName.ifEmpty { state.radioName }
    IosCard(modifier = modifier.fillMaxWidth().clickable { editing = true }) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PangeaAvatar(
                photo = rememberAvatarBitmap(state.photo),
                emoji = state.profile.emoji,
                colorIndex = state.profile.color,
                initials = RadioAutoConfig.shortName(name),
                fallbackColor = AvatarPalette.color(state.profile.color),
                size = 56.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.pangea_my_profile), style = Ios.type.caption1)
                Text(name, style = Ios.type.headline)
                state.code?.let { Text(it, fontFamily = FontFamily.Monospace, style = Ios.type.footnote) }
            }
            IosButton(
                text = stringResource(Res.string.pangea_edit_profile),
                onClick = { editing = true },
                style = IosButtonStyle.Plain,
            )
        }
    }
    if (editing) ProfileEditorSheet(state, onSave = viewModel::save, onDismiss = { editing = false })
}

/** Name, emoji, colour and photo, with a live preview (Pangea-Apple ProfileEditorSheet). */
@Suppress("LongMethod", "ModifierMissing")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorSheet(
    state: MyProfileState,
    onSave: (String, String, Int, PhotoChange?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(state.profile.displayName.ifEmpty { state.radioName }) }
    var emoji by rememberSaveable { mutableStateOf(state.profile.emoji) }
    var color by rememberSaveable { mutableStateOf(state.profile.color) }
    var photoChange by remember { mutableStateOf<PhotoChange?>(null) }
    var showAllEmoji by remember { mutableStateOf(false) }
    val pickPhoto = rememberAvatarPhotoPicker { bytes ->
        bytes?.let { photoChange = PhotoChange.Set(it.toByteString()) }
    }
    val previewPhoto =
        when (val change = photoChange) {
            is PhotoChange.Set -> change.jpeg.toByteArray()
            PhotoChange.Remove -> null
            null -> state.photo
        }

    IosSheet(
        title = stringResource(Res.string.pangea_profile),
        onDismissRequest = onDismiss,
        grouped = true,
        cancelLabel = stringResource(Res.string.cancel),
        actionLabel = stringResource(Res.string.save),
        actionEnabled = ProfileCard.cleanName(name).isNotEmpty(),
        onAction = {
            onSave(name, emoji, color, photoChange)
            onDismiss()
        },
    ) {
        // iOS's ProfileEditorSheet is a form: the photo, then Name, Emoji and Color sections, then the privacy note.
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            IosSection {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PangeaAvatar(
                        photo = rememberAvatarBitmap(previewPhoto),
                        emoji = emoji,
                        colorIndex = color,
                        initials = RadioAutoConfig.shortName(name),
                        fallbackColor = AvatarPalette.color(color),
                        size = 96.dp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IosButton(
                            text = stringResource(Res.string.pangea_choose_photo),
                            onClick = pickPhoto,
                            style = IosButtonStyle.Gray,
                            textStyle = Ios.type.subheadline,
                        )
                        if (previewPhoto != null) {
                            IosButton(
                                text = stringResource(Res.string.pangea_remove_photo),
                                onClick = { photoChange = PhotoChange.Remove },
                                style = IosButtonStyle.Gray,
                                textStyle = Ios.type.subheadline,
                            )
                        }
                    }
                }
            }
            IosSection(
                header = stringResource(Res.string.name),
                footer = stringResource(Res.string.pangea_profile_name_footer),
            ) {
                ProfileNameField(name = name, onNameChange = { name = it })
            }
            IosSection(
                header = stringResource(Res.string.pangea_profile_emoji),
                footer = stringResource(Res.string.pangea_profile_emoji_footer),
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    EmojiQuickPicks(
                        selected = emoji,
                        onPick = { emoji = if (emoji == it) "" else it },
                        onAll = { showAllEmoji = true },
                    )
                }
            }
            IosSection(header = stringResource(Res.string.pangea_color)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    ColorDots(selected = color, onPick = { color = it })
                }
            }
            Text(
                stringResource(Res.string.pangea_profile_shared_with),
                style = Ios.type.footnote,
                color = Ios.colors.secondaryLabel,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp),
            )
            // A new name is written to the radio, which restarts to use it: say so before Save.
            val currentName = state.profile.displayName.ifEmpty { state.radioName }
            if (state.code != null && ProfileCard.cleanName(name) != currentName) {
                Text(
                    stringResource(Res.string.pangea_profile_save_restarts_radio),
                    style = Ios.type.footnote,
                    color = Ios.colors.systemRed,
                    modifier = Modifier.padding(horizontal = 32.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (showAllEmoji) {
        PangeaEmojiPicker(
            onPick = {
                emoji = it
                showAllEmoji = false
            },
            onDismiss = { showAllEmoji = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmojiQuickPicks(selected: String, onPick: (String) -> Unit, onAll: () -> Unit) {
    val picks =
        if (selected.isEmpty() || selected in EmojiCatalog.suggestedEmoji) {
            EmojiCatalog.suggestedEmoji
        } else {
            listOf(selected) + EmojiCatalog.suggestedEmoji.dropLast(1)
        }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Wraps to the screen width; a fixed eight per row ran off a phone's edge.
        FlowRow(horizontalArrangement = Arrangement.Center) {
            picks.forEach { item ->
                val shape = CircleShape
                Text(
                    item,
                    fontSize = 24.sp,
                    modifier =
                    Modifier.padding(4.dp)
                        .clip(shape)
                        .then(
                            if (item == selected) {
                                Modifier.border(2.dp, Ios.colors.tint, shape)
                            } else {
                                Modifier
                            },
                        )
                        .clickable { onPick(item) }
                        .padding(6.dp),
                )
            }
        }
        IosButton(text = stringResource(Res.string.pangea_all_emoji), onClick = onAll, style = IosButtonStyle.Plain)
    }
}

@Composable
private fun ColorDots(selected: Int, onPick: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AvatarPalette.colors.forEachIndexed { index, color ->
            val ring = if (index == selected) Ios.colors.label else Color.Transparent
            Box(
                modifier =
                Modifier.size(34.dp).clip(CircleShape).border(3.dp, ring, CircleShape).clickable { onPick(index) },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(color))
            }
        }
    }
}

/**
 * "Set Up Your Profile" at the top of Chats until I've named myself (Pangea-Apple's setup step after pairing), so
 * friends see a name rather than "Meshtastic 1a2b".
 */
@Composable
fun SetupProfilePrompt(modifier: Modifier = Modifier, viewModel: MyProfileViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf(false) }
    val needsName =
        state.code != null && state.profile.displayName.isEmpty() && RadioAutoConfig.hasDefaultName(state.radioName)
    if (needsName) {
        IosCard(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.pangea_setup_profile), style = Ios.type.headline)
                    Text(
                        stringResource(Res.string.pangea_setup_profile_hint),
                        style = Ios.type.footnote,
                        color = Ios.colors.secondaryLabel,
                    )
                }
                IosButton(text = stringResource(Res.string.pangea_edit_profile), onClick = { editing = true })
            }
        }
    }
    if (editing) ProfileEditorSheet(state, onSave = viewModel::save, onDismiss = { editing = false })
}

/** The Name section's field, as an iOS form's text field: the name inline on the card, its placeholder in grey. */
@Composable
private fun ProfileNameField(name: String, onNameChange: (String) -> Unit) {
    val c = Ios.colors
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp)) {
        if (name.isEmpty()) {
            Text(stringResource(Res.string.pangea_display_name), style = Ios.type.body, color = c.tertiaryLabel)
        }
        BasicTextField(
            value = name,
            onValueChange = onNameChange,
            singleLine = true,
            textStyle = Ios.type.body.copy(color = c.label),
            cursorBrush = SolidColor(c.tint),
            keyboardOptions =
            KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
