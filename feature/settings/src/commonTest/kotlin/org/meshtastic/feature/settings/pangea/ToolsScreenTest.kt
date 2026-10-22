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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import okio.ByteString.Companion.toByteString
import org.meshtastic.core.common.util.CommonUri
import org.meshtastic.core.model.Node
import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.getString
import org.meshtastic.core.resources.pangea_nfc_node_name
import org.meshtastic.core.resources.pangea_nfc_not_pangea
import org.meshtastic.core.resources.pangea_nfc_scan_tag
import org.meshtastic.core.resources.pangea_nfc_write_contact
import org.meshtastic.core.ui.util.LocalNfcScannerProvider
import org.meshtastic.proto.User
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ToolsScreenTest {

    private val ownNode =
        Node(
            num = 0x42424242,
            user =
            User.Builder()
                .apply {
                    id = "!42424242"
                    long_name = "Demo Handset"
                    short_name = "DEMO"
                    public_key = ByteArray(32) { 7 }.toByteString()
                }
                .build(),
        )

    /** A scanner that reads [contents] off a tag the moment it is armed. */
    private fun scannerReading(
        contents: String?,
    ): @Composable (onResult: (String?) -> Unit, onNfcDisabled: () -> Unit) -> Unit = { onResult, _ ->
        LaunchedEffect(Unit) { onResult(contents) }
    }

    @Test
    fun showsTheNodeAndBothActions() = runComposeUiTest {
        setContent { ToolsScreen(ownNode = ownNode, onHandleDeepLink = { _, _ -> }, onBack = {}) }

        onNodeWithText(getString(Res.string.pangea_nfc_node_name, "Demo Handset")).assertIsDisplayed()
        onNodeWithText(getString(Res.string.pangea_nfc_write_contact)).assertIsDisplayed()
        onNodeWithText(getString(Res.string.pangea_nfc_scan_tag)).assertIsDisplayed()
    }

    @Test
    fun withoutARadioOnlyScanningIsOffered() = runComposeUiTest {
        setContent { ToolsScreen(ownNode = null, onHandleDeepLink = { _, _ -> }, onBack = {}) }

        onNodeWithText(getString(Res.string.pangea_nfc_write_contact)).assertDoesNotExist()
        onNodeWithText(getString(Res.string.pangea_nfc_scan_tag)).assertIsDisplayed()
    }

    @Test
    fun aScannedLinkGoesToTheDeepLinkHandler() = runComposeUiTest {
        val link = "https://meshtastic.org/e/#CgMSAQESBggBQANIAQ"
        val handled = mutableListOf<String>()
        setContent {
            CompositionLocalProvider(LocalNfcScannerProvider provides scannerReading(link)) {
                ToolsScreen(
                    ownNode = ownNode,
                    onHandleDeepLink = { uri: CommonUri, _ -> handled += uri.toString() },
                    onBack = {},
                )
            }
        }

        onNodeWithText(getString(Res.string.pangea_nfc_scan_tag)).performClick()
        runOnIdle { assertEquals(listOf(link), handled) }
    }

    @Test
    fun aLinkTheAppRejectsSaysTheTagIsNotPangeas() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalNfcScannerProvider provides scannerReading("https://example.com/x")) {
                ToolsScreen(ownNode = ownNode, onHandleDeepLink = { _, onInvalid -> onInvalid() }, onBack = {})
            }
        }

        onNodeWithText(getString(Res.string.pangea_nfc_scan_tag)).performClick()
        onNodeWithText(getString(Res.string.pangea_nfc_not_pangea)).assertIsDisplayed()
    }
}
