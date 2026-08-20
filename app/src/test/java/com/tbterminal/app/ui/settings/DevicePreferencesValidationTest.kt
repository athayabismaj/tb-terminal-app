package com.tbterminal.app.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DevicePreferencesValidationTest {
    @Test
    fun acceptsSupportedPaperMoneyAndLockRange() {
        val result = validateDevicePreferences("80mm", "1000,50", "30")

        assertNull(result.error)
        assertEquals("80mm", result.preferences?.paperSize)
        assertEquals("1000.5", result.preferences?.cashTolerance)
        assertEquals(30, result.preferences?.autoLockMinutes)
    }

    @Test
    fun rejectsUnsupportedPaperInvalidMoneyAndUnsafeLockRange() {
        assertNotNull(validateDevicePreferences("A4", "0", "15").error)
        assertNotNull(validateDevicePreferences("58mm", "1.001", "15").error)
        assertNotNull(validateDevicePreferences("58mm", "0", "0").error)
        assertNotNull(validateDevicePreferences("58mm", "0", "121").error)
    }
}
