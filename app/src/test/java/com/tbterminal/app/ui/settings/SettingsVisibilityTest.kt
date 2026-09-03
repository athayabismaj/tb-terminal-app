package com.tbterminal.app.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsVisibilityTest {
    @Test
    fun ownerSeesStoreSecurityAndDeviceSettings() {
        assertEquals(
            listOf("store", "security", "device", "sync"),
            visibleSettingsTabKeys("OWNER"),
        )
    }

    @Test
    fun adminCannotSeeOwnerSecuritySettings() {
        assertEquals(
            listOf("store", "device", "sync"),
            visibleSettingsTabKeys("admin"),
        )
    }

    @Test
    fun cashierOnlySeesLocalDeviceSettings() {
        assertEquals(listOf("device"), visibleSettingsTabKeys("kasir"))
    }
}
