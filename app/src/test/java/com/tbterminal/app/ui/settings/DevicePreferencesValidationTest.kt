package com.tbterminal.app.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DevicePreferencesValidationTest {
    @Test
    fun acceptsSupportedPaperMoneyAndLockRange() {
        val grouped = validateDevicePreferences("80mm", "1.000", "30")
        assertNull(grouped.error)
        assertEquals("80mm", grouped.preferences?.paperSize)
        assertEquals("1000", grouped.preferences?.cashTolerance)
        assertEquals(30, grouped.preferences?.autoLockMinutes)

        val commaZero = validateDevicePreferences("58mm", "1000,00", "15")
        assertNull(commaZero.error)
        assertEquals("1000", commaZero.preferences?.cashTolerance)
    }

    @Test
    fun normalizeLegacyCashToleranceInput_roundsLegacyDecimalsHalfUp() {
        assertEquals("1001", normalizeLegacyCashToleranceInput("1000.50"))
        assertEquals("1250", normalizeLegacyCashToleranceInput("1250,49"))
        assertEquals("1000", normalizeLegacyCashToleranceInput("1.000"))
    }

    @Test
    fun rejectsUnsupportedPaperInvalidMoneyAndUnsafeLockRange() {
        assertNotNull(validateDevicePreferences("A4", "0", "15").error)
        assertNotNull(validateDevicePreferences("58mm", "1000,50", "15").error)
        assertNotNull(validateDevicePreferences("58mm", "1000.50", "15").error)
        assertNotNull(validateDevicePreferences("58mm", "0", "0").error)
        assertNotNull(validateDevicePreferences("58mm", "0", "121").error)
    }
}
