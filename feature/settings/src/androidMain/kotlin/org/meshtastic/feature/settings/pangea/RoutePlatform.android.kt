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

import android.provider.OpenableColumns
import com.eygraber.uri.toAndroidUri
import org.meshtastic.core.common.ContextServices
import org.meshtastic.core.common.util.CommonUri

// A document provider names its files in the display-name column; the path is often an opaque id.
internal actual fun pickedFileName(uri: CommonUri): String? = runCatching {
    ContextServices.app.contentResolver
        .query(uri.toAndroidUri(), arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
}
    .getOrNull() ?: uri.lastPathSegment

internal actual val canRecordRoutes: Boolean = true
