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
package org.meshtastic.feature.intro

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.cancel
import org.meshtastic.core.ui.ios.IosInset
import org.meshtastic.core.ui.ios.IosSheet
import org.meshtastic.core.ui.ios.IosSheetHeader

/**
 * Pairing outside first-run setup, as the iPhone app's PairRadioSheet does it: [pairRadio] (from the connections
 * feature) in a sheet with **Cancel**, then the name page only if the radio still has its factory name or no region.
 */
@Composable
fun PairRadioSheet(
    pairRadio: @Composable (onPair: () -> Unit) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    profileViewModel: SetupProfileViewModel = koinViewModel(),
) {
    var showProfile by rememberSaveable { mutableStateOf(false) }
    val needsRegion by profileViewModel.needsRegion.collectAsStateWithLifecycle()
    IosSheet(
        title = "",
        onDismissRequest = onDismiss,
        modifier = modifier,
        header = {
            // iOS's Cancel belongs to the pairing page; the name page after it has none.
            if (showProfile) {
                Spacer(Modifier.height(20.dp))
            } else {
                IosSheetHeader(title = "", cancelLabel = stringResource(Res.string.cancel), onCancel = onDismiss)
            }
        },
    ) {
        CompositionLocalProvider(LocalSetupInSheet provides true) {
            if (showProfile) {
                // The same view model instance: koinViewModel hands back this screen's.
                SetupProfileScreen(onFinish = onDismiss)
            } else {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = IosInset)) {
                    pairRadio {
                        if (needsRegion || profileViewModel.radioHasDefaultName()) showProfile = true else onDismiss()
                    }
                }
            }
        }
    }
}
