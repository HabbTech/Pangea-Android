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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.ui.icon.AppSettingsAlt
import org.meshtastic.core.ui.icon.Info
import org.meshtastic.core.ui.icon.MeshtasticIcons
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.util.PermissionStatus

/**
 * A setup page that asks for one permission, as on the iPhone app: a large title, an optional explanation whose word
 * "settings" opens the app's settings, the rows saying what the permission is for, and one button.
 *
 * The button keeps its "Configure …" label whatever the status, as iOS's does: [onPrimaryAction] asks once and then
 * moves on whatever the answer (see `rememberAskThenContinue` in the setup graph). After a denial the page also names
 * what stays disabled, the educational UI Android's permissions guidance asks for; the notice is a polite live region
 * so a screen reader announces it when the user comes back from the dialog.
 *
 * @param headlineRes The page title.
 * @param descriptionRes The explanation under the title, or null for none (iOS's notifications page has none).
 * @param features The rows, drawn with [FeatureRow].
 * @param status The live permission status, for the notice.
 * @param deniedNoticeRes Notice shown after a single denial: what stays disabled if the user declines again.
 * @param blockedNoticeRes Notice shown once the system stops prompting: what stays disabled and where to turn it on.
 * @param configureButtonTextRes The button's label.
 * @param onPrimaryAction The button.
 * @param onOpenSettings The "settings" link in the explanation.
 * @param header Shown above the rows (a section title).
 * @param additionalContent Shown below the rows.
 */
@Suppress("LongParameterList")
@Composable
internal fun PermissionScreenLayout(
    headlineRes: StringResource,
    descriptionRes: StringResource?,
    features: List<FeatureUIData>,
    status: PermissionStatus,
    deniedNoticeRes: StringResource,
    blockedNoticeRes: StringResource,
    configureButtonTextRes: StringResource,
    onPrimaryAction: () -> Unit,
    onOpenSettings: () -> Unit = {},
    header: (@Composable () -> Unit)? = null,
    additionalContent: (@Composable () -> Unit)? = null,
) {
    IosSetupPage(
        title = stringResource(headlineRes),
        description = descriptionRes?.let { stringResource(it) },
        largeTitle = true,
        onDescriptionLink = onOpenSettings,
        primaryLabel = stringResource(configureButtonTextRes),
        onPrimary = onPrimaryAction,
    ) {
        // Directly under the description, above the rows, so a user who has just declined sees what they lose. It
        // renders only after a denial, so the happy path pays nothing.
        PermissionNotice(status = status, deniedNoticeRes = deniedNoticeRes, blockedNoticeRes = blockedNoticeRes)
        header?.invoke()
        features.forEach { feature -> FeatureRow(feature = feature) }
        additionalContent?.invoke()
    }
}

/**
 * Inline explanation of what the user loses by declining, shown only once they actually have declined.
 *
 * Deliberately quieter than [org.meshtastic.core.ui.component.RecoveryCard]: onboarding is not an error state, and an
 * `errorContainer` wash on a screen the user is allowed to skip would read as a scolding.
 */
@Composable
private fun PermissionNotice(
    status: PermissionStatus,
    deniedNoticeRes: StringResource,
    blockedNoticeRes: StringResource,
) {
    val noticeRes =
        when (status) {
            PermissionStatus.DENIED_CAN_RETRY -> deniedNoticeRes

            PermissionStatus.PERMANENTLY_DENIED -> blockedNoticeRes

            PermissionStatus.NOT_REQUESTED,
            PermissionStatus.GRANTED,
            -> return
        }
    val icon =
        if (status == PermissionStatus.PERMANENTLY_DENIED) MeshtasticIcons.AppSettingsAlt else MeshtasticIcons.Info

    // Column rather than a bare Surface plus Spacer: the notice now sits mid-list, so it owns the gap below it, and a
    // composable may only emit from one source at its top level.
    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = Ios.colors.fieldFill,
            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Ios.colors.secondaryLabel)
                Text(
                    text = stringResource(noticeRes),
                    style = Ios.type.footnote,
                    color = Ios.colors.secondaryLabel,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
