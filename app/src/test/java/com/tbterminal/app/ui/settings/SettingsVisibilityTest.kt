package com.tbterminal.app.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsVisibilityTest {
    @Test
    fun ownerSeesStoreReceiptAndDeviceSettings() {
        assertEquals(
            listOf(SettingsPage.STORE, SettingsPage.RECEIPT, SettingsPage.DEVICE),
            visibleSettingsPages("OWNER"),
        )
    }

    @Test
    fun adminSeesStoreReceiptAndDeviceSettings() {
        assertEquals(
            listOf(SettingsPage.STORE, SettingsPage.RECEIPT, SettingsPage.DEVICE),
            visibleSettingsPages("admin"),
        )
    }

    @Test
    fun cashierOnlySeesLocalDeviceSettings() {
        assertEquals(listOf(SettingsPage.DEVICE), visibleSettingsPages("kasir"))
    }
}
