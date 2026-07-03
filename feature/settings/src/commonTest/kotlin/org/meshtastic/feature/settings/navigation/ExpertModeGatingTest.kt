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
package org.meshtastic.feature.settings.navigation

import org.meshtastic.core.resources.Res
import org.meshtastic.core.resources.device
import org.meshtastic.core.resources.device_configuration
import org.meshtastic.core.resources.module_settings
import org.meshtastic.core.resources.modules
import org.meshtastic.core.resources.radio
import org.meshtastic.core.resources.radio_configuration
import org.meshtastic.proto.Config
import org.meshtastic.proto.DeviceMetadata
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Exhaustive classification tests for Expert Mode gating. Every [ConfigRoute] and [ModuleRoute] entry is asserted in
 * both casual and expert mode, so adding a route without deciding its gating fails these tests (and the exhaustive
 * `when` behind `isExpertOnly` fails compilation).
 */
class ExpertModeGatingTest {

    private val casualConfigRoutes = setOf(ConfigRoute.USER, ConfigRoute.CHANNELS, ConfigRoute.DISPLAY)
    private val casualModuleRoutes = setOf(ModuleRoute.AMBIENT_LIGHTING)

    /** Metadata that supports every route, so expert gating is the only differentiator. */
    private val fullySupportedMetadata =
        DeviceMetadata(firmware_version = "2.8.0", hasBluetooth = true, hasWifi = true, hasEthernet = true)

    @Test
    fun `every config route is classified for expert mode`() {
        ConfigRoute.entries.forEach { route ->
            assertEquals(
                route !in casualConfigRoutes,
                route.isExpertOnly,
                "$route must be classified as casual or expert-only",
            )
        }
    }

    @Test
    fun `every module route is classified for expert mode`() {
        ModuleRoute.entries.forEach { route ->
            assertEquals(
                route !in casualModuleRoutes,
                route.isExpertOnly,
                "$route must be classified as casual or expert-only",
            )
        }
    }

    @Test
    fun `casual radio section shows only user and channels`() {
        assertEquals(listOf(ConfigRoute.USER, ConfigRoute.CHANNELS), ConfigRoute.radioConfigRoutes(false))
    }

    @Test
    fun `expert radio section shows all radio routes`() {
        assertEquals(ConfigRoute.radioConfigRoutes, ConfigRoute.radioConfigRoutes(true))
    }

    @Test
    fun `every config route is present or absent as expected in device section`() {
        val casual = ConfigRoute.deviceConfigRoutes(fullySupportedMetadata, false)
        val expert = ConfigRoute.deviceConfigRoutes(fullySupportedMetadata, true)

        ConfigRoute.entries.forEach { route ->
            val inRadioSection = route in ConfigRoute.radioConfigRoutes
            assertEquals(!inRadioSection && !route.isExpertOnly, route in casual, "casual device section: $route")
            assertEquals(!inRadioSection, route in expert, "expert device section: $route")
        }
    }

    @Test
    fun `every module route is present or absent as expected in module section`() {
        // TAK role makes the TAK module applicable, so the expert list covers every module.
        val role = Config.DeviceConfig.Role.TAK
        val casual = ModuleRoute.filterExcludedFrom(fullySupportedMetadata, role, false)
        val expert = ModuleRoute.filterExcludedFrom(fullySupportedMetadata, role, true)

        ModuleRoute.entries.forEach { route ->
            assertEquals(!route.isExpertOnly, route in casual, "casual module section: $route")
            assertTrue(route in expert, "expert module section: $route")
        }
    }

    @Test
    fun `bluetooth and network metadata exclusions apply in both modes`() {
        val noRadios =
            DeviceMetadata(firmware_version = "2.8.0", hasBluetooth = false, hasWifi = false, hasEthernet = false)

        listOf(false, true).forEach { expertMode ->
            val routes = ConfigRoute.deviceConfigRoutes(noRadios, expertMode)
            assertFalse(ConfigRoute.BLUETOOTH in routes, "BLUETOOTH must stay excluded (expertMode=$expertMode)")
            assertFalse(ConfigRoute.NETWORK in routes, "NETWORK must stay excluded (expertMode=$expertMode)")
        }
    }

    @Test
    fun `excluded modules bitfield applies in both modes`() {
        val metadata =
            DeviceMetadata(
                firmware_version = "2.8.0",
                excluded_modules = ModuleRoute.MQTT.bitfield or ModuleRoute.AMBIENT_LIGHTING.bitfield,
            )

        listOf(false, true).forEach { expertMode ->
            val modules = ModuleRoute.filterExcludedFrom(metadata, null, expertMode)
            assertFalse(ModuleRoute.MQTT in modules, "MQTT must stay excluded (expertMode=$expertMode)")
            assertFalse(
                ModuleRoute.AMBIENT_LIGHTING in modules,
                "AMBIENT_LIGHTING must stay excluded (expertMode=$expertMode)",
            )
        }
    }

    @Test
    fun `section titles switch between casual and expert`() {
        assertEquals(Res.string.radio, ConfigRoute.radioSectionTitle(false))
        assertEquals(Res.string.radio_configuration, ConfigRoute.radioSectionTitle(true))
        assertEquals(Res.string.device, ConfigRoute.deviceSectionTitle(false))
        assertEquals(Res.string.device_configuration, ConfigRoute.deviceSectionTitle(true))
        assertEquals(Res.string.modules, ModuleRoute.sectionTitle(false))
        assertEquals(Res.string.module_settings, ModuleRoute.sectionTitle(true))
    }
}
