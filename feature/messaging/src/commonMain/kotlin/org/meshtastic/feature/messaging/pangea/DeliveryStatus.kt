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
package org.meshtastic.feature.messaging.pangea

import androidx.compose.runtime.staticCompositionLocalOf
import org.jetbrains.compose.resources.StringResource
import org.meshtastic.core.model.MessageStatus
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.pangea_status_couldnt_send_privately
import org.meshtastic.core.resources.pangea_status_delivered
import org.meshtastic.core.resources.pangea_status_duty_cycle
import org.meshtastic.core.resources.pangea_status_no_channel
import org.meshtastic.core.resources.pangea_status_no_interface
import org.meshtastic.core.resources.pangea_status_no_route
import org.meshtastic.core.resources.pangea_status_not_delivered
import org.meshtastic.core.resources.pangea_status_out_of_range
import org.meshtastic.core.resources.pangea_status_out_of_range_group
import org.meshtastic.core.resources.pangea_status_rate_limited
import org.meshtastic.core.resources.pangea_status_relayed_only
import org.meshtastic.core.resources.pangea_status_sending
import org.meshtastic.core.resources.pangea_status_sent
import org.meshtastic.core.resources.pangea_status_too_large
import org.meshtastic.proto.Routing

/**
 * True outside Expert Mode: a sent message's status reads in plain words instead of mesh terms (Pangea-Apple
 * DeliveryStatus). Provided by the message screen.
 */
@Suppress("CompositionLocalAllowlist")
val LocalPangeaPlainWording = staticCompositionLocalOf { false }

/** What happened to a message I sent, in plain words. [isDirect] is a private chat rather than a group. */
@Suppress("CyclomaticComplexMethod")
fun plainDeliveryStatus(status: MessageStatus?, routingError: Int, isDirect: Boolean): StringResource = when (status) {
    MessageStatus.RECEIVED -> Res.string.pangea_status_delivered

    MessageStatus.DELIVERED,
    MessageStatus.SFPP_CONFIRMED,
    ->
        if (isDirect) Res.string.pangea_status_relayed_only else Res.string.pangea_status_sent

    MessageStatus.ERROR ->
        when (Routing.Error.fromValue(routingError)) {
            Routing.Error.NONE ->
                if (isDirect) Res.string.pangea_status_delivered else Res.string.pangea_status_sent

            Routing.Error.MAX_RETRANSMIT,
            Routing.Error.TIMEOUT,
            Routing.Error.NO_RESPONSE,
            ->
                if (isDirect) Res.string.pangea_status_out_of_range else Res.string.pangea_status_out_of_range_group

            Routing.Error.NO_ROUTE -> Res.string.pangea_status_no_route

            Routing.Error.NO_INTERFACE -> Res.string.pangea_status_no_interface

            Routing.Error.NO_CHANNEL -> Res.string.pangea_status_no_channel

            Routing.Error.TOO_LARGE -> Res.string.pangea_status_too_large

            Routing.Error.DUTY_CYCLE_LIMIT -> Res.string.pangea_status_duty_cycle

            Routing.Error.RATE_LIMIT_EXCEEDED -> Res.string.pangea_status_rate_limited

            Routing.Error.PKI_FAILED,
            Routing.Error.PKI_UNKNOWN_PUBKEY,
            Routing.Error.PKI_SEND_FAIL_PUBLIC_KEY,
            -> Res.string.pangea_status_couldnt_send_privately

            else -> Res.string.pangea_status_not_delivered
        }

    MessageStatus.QUEUED,
    MessageStatus.ENROUTE,
    MessageStatus.SFPP_ROUTING,
    MessageStatus.UNKNOWN,
    null,
    -> Res.string.pangea_status_sending
}
