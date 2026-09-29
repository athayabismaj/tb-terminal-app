package com.tbterminal.app.ui.settings

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileValidationTest {
    @Test
    fun passwordRequiresOldDistinctMinimumAndConfirmation() {
        assertNotNull(validatePasswordChange("", "new-pass", "new-pass"))
        assertNotNull(validatePasswordChange("old-pass", "short", "short"))
        assertNotNull(validatePasswordChange("same-pass", "same-pass", "same-pass"))
        assertNotNull(validatePasswordChange("old-pass", "new-pass", "different"))
        assertNull(validatePasswordChange("old-pass", "new-pass", "new-pass"))
    }

    @Test
    fun pinOnlyAcceptsFourToSixDigitsAndMatchingConfirmation() {
        assertNotNull(validatePinChange("12ab", "5678", "5678"))
        assertNotNull(validatePinChange("1234", "123", "123"))
        assertNotNull(validatePinChange("1234", "1234", "1234"))
        assertNotNull(validatePinChange("1234", "5678", "9999"))
        assertNull(validatePinChange("1234", "5678", "5678"))
    }

    @Test
    fun profileRequiresValidNameAndOptionalValidEmail() {
        assertNotNull(validateProfileForm("P", null))
        assertNotNull(validateProfileForm("Pemilik Toko", "email-tidak-valid"))
        assertNotNull(validateProfileForm("Pemilik Toko", "a".repeat(151)))
        assertNull(validateProfileForm("Pemilik Toko", null))
        assertNull(validateProfileForm("Pemilik Toko", "pemilik@tbterminal.id"))
    }
}
