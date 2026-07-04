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
package org.meshtastic.core.model

/**
 * Three-tier app-side power profile. Feature sites read the gate properties below instead of branching on the raw
 * enum, so the meaning of each tier stays defined in one place.
 *
 * Ordinal order encodes aggressiveness: [STANDARD] < [TRAIL] < [EXPEDITION].
 */
enum class PowerMode {
    /** All features on. Best when the phone is charged or near power. */
    STANDARD,

    /** Day-trip tuning: reduced background work without noticeable feature loss. */
    TRAIL,

    /** Multi-day off-grid tuning: trades convenience for battery. */
    EXPEDITION,
    ;

    /** Continuous phone-GPS sharing to the mesh is allowed. */
    val allowsContinuousLocation: Boolean
        get() = this != EXPEDITION

    /** Phone-side weather fetching (if any) is allowed. */
    val allowsWeather: Boolean
        get() = this != EXPEDITION

    /** Continuously self-refreshing UI surfaces (live cards, rich periodic notifications) may start. */
    val allowsLiveUpdates: Boolean
        get() = this == STANDARD

    /** Automatic (not user-initiated) reconnection to the preferred radio is allowed. */
    val allowsBackgroundAutoConnect: Boolean
        get() = this != EXPEDITION

    /** Pushing fresh data to home-screen widgets is allowed. */
    val allowsWidgetRefresh: Boolean
        get() = this != EXPEDITION

    /**
     * Minimum interval (seconds) between phone-position shares to the mesh, regardless of the user's configured
     * interval. [Int.MAX_VALUE] means phone-position sharing is disabled entirely.
     */
    val locationIntervalFloorSecs: Int
        get() =
            when (this) {
                STANDARD -> 0
                TRAIL -> TRAIL_LOCATION_FLOOR_SECS
                EXPEDITION -> Int.MAX_VALUE
            }

    companion object {
        private const val TRAIL_LOCATION_FLOOR_SECS = 300

        /** The more aggressive (battery-saving) of two modes. */
        fun mostAggressive(a: PowerMode, b: PowerMode): PowerMode = if (a.ordinal >= b.ordinal) a else b

        /** Parses a persisted [name], falling back to [STANDARD] for null/unknown values. */
        fun fromName(name: String?): PowerMode = entries.firstOrNull { it.name == name } ?: STANDARD
    }
}
