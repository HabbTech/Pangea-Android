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
package org.meshtastic.core.service

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.Single
import org.meshtastic.core.di.CoroutineDispatchers
import org.meshtastic.core.repository.SystemPowerSaveSource

/** Observes Android Battery Saver via [PowerManager.ACTION_POWER_SAVE_MODE_CHANGED]. */
@Single
class AndroidSystemPowerSaveSource(context: Application, dispatchers: CoroutineDispatchers) : SystemPowerSaveSource {
    private val scope = CoroutineScope(SupervisorJob() + dispatchers.default)
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    override val isPowerSaveMode: StateFlow<Boolean> =
        callbackFlow {
            trySend(powerManager.isPowerSaveMode)
            val receiver =
                object : BroadcastReceiver() {
                    override fun onReceive(receiverContext: Context?, intent: Intent?) {
                        trySend(powerManager.isPowerSaveMode)
                    }
                }
            context.registerReceiver(receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
            awaitClose { context.unregisterReceiver(receiver) }
        }
            .stateIn(scope, SharingStarted.Eagerly, powerManager.isPowerSaveMode)
}
