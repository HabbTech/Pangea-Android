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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.emptyFlow
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.model.pangea.RouteActivity
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.navigate_back
import org.meshtastic.core.resources.pangea_route_record_details
import org.meshtastic.core.resources.pangea_route_recording_details
import org.meshtastic.core.ui.ios.Ios
import org.meshtastic.core.ui.ios.IosDrawsUnderTabBar
import org.meshtastic.core.ui.ios.IosGlassIconButton
import org.meshtastic.core.ui.ios.IosIcons
import org.meshtastic.core.ui.ios.IosTopBar
import org.meshtastic.core.ui.ios.LocalIosBottomInset
import org.meshtastic.core.ui.ios.iosPressable
import org.meshtastic.core.ui.util.KeepScreenOn
import org.meshtastic.core.ui.util.LocalRouteRecorderMapProvider
import org.meshtastic.core.ui.util.PermissionStatus
import org.meshtastic.core.ui.util.rememberLocationPermissionState

/**
 * The iPhone app's Route Recorder (Settings → Route Recorder): a full-screen map following the phone, the recording
 * drawn on it dashed, and a big record button that opens the details sheet (status, Time, Distance, Elev. Gain, the
 * phone's GPS, Activity, and Start / Pause / Resume / Finish). The recording belongs to the app, not this screen: it
 * carries on after leaving, and is waiting here on return.
 */
@Composable
fun RouteRecorderScreen(viewModel: RouteRecorderViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val recording by viewModel.recording.collectAsStateWithLifecycle()
    val units by viewModel.units.collectAsStateWithLifecycle()
    val permission = rememberLocationPermissionState()
    val granted = permission.status == PermissionStatus.GRANTED
    var showingDetails by rememberSaveable { mutableStateOf(false) }
    var activity by rememberSaveable { mutableStateOf(RouteActivity.HIKING) }
    val idle = recording == null
    // While nothing records, the sheet's GPS status listens for itself; a recording carries the latest fix.
    val liveFix by
        remember(granted, idle) { if (granted && idle) viewModel.fixes() else emptyFlow() }
            .collectAsStateWithLifecycle(null)

    IosDrawsUnderTabBar()
    val bottom = LocalIosBottomInset.current
    Box(modifier.fillMaxSize()) {
        val route = recording?.route
        LocalRouteRecorderMapProvider.current(
            route?.points.orEmpty(),
            route?.color ?: IDLE_LINE_COLOR,
            Modifier.fillMaxSize(),
        )
        IosTopBar(
            title = "",
            showTitle = false,
            background = Color.Transparent,
            showRadioChip = false,
            leading = {
                IosGlassIconButton(
                    IosIcons.ChevronLeft,
                    contentDescription = stringResource(Res.string.navigate_back),
                    onClick = onBack,
                )
            },
        )
        RecordButton(
            recording = recording?.paused == false,
            onClick = { showingDetails = true },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = bottom + 16.dp),
        )
    }

    if (showingDetails) {
        KeepScreenOn(enabled = true)
        RecorderSheet(
            recording = recording,
            fix = recording?.lastFix ?: liveFix,
            units = units,
            locationGranted = granted,
            onGrantLocation = permission.request,
            activity = activity,
            onActivityChange = { activity = it },
            onStart = viewModel::start,
            onPause = viewModel::pause,
            onResume = viewModel::resume,
            onFinish = {
                viewModel.finish()
                showingDetails = false
            },
            onDismissRequest = { showingDetails = false },
        )
    }
}

/** iOS's record button: `record.circle` at 72 pt in red on a glass disc, filled while recording. */
@Composable
private fun RecordButton(recording: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ios.colors
    val label =
        stringResource(
            if (recording) Res.string.pangea_route_recording_details else Res.string.pangea_route_record_details,
        )
    Box(
        modifier
            .size(96.dp)
            .shadow(12.dp, CircleShape, ambientColor = c.chromeShadow, spotColor = c.chromeShadow)
            .clip(CircleShape)
            .background(c.chrome)
            .iosPressable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (recording) IosIcons.RecordCircleFill else IosIcons.RecordCircle,
            contentDescription = null,
            tint = c.systemRed,
            modifier = Modifier.size(86.dp),
        )
    }
}

/** iOS's starting colour for the line, before a recording picks its own. */
private const val IDLE_LINE_COLOR = 0x007AFF
