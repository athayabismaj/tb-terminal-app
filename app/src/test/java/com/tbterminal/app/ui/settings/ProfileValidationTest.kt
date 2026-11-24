package com.tbterminal.app.ui.settings

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileValidationTest {
    @Test
    fun passwordRequiresOldDistinctMinimumAndConfirmation() {
        assertNotNull(validatePasswordChange("", "new-pass", "new-pass"))
        assertNotNull(validatePasswordChange("old-pass", "short", "short"))
        assertNotNull(validatePasswordChange("old-pass", "1234567", "1234567"))
        assertNotNull(validatePasswordChange("old-pass", "a".repeat(73), "a".repeat(73)))
        assertNotNull(validatePasswordChange("same-pass", "same-pass", "same-pass"))
        assertNotNull(validatePasswordChange("old-pass", "new-pass", "different"))
        assertNull(validatePasswordChange("old-pass", "new-pass", "new-pass"))
        assertNull(validatePasswordChange("old-pass", "a".repeat(72), "a".repeat(72)))
    }

    @Test
    fun pinOnlyAcceptsFourToSixDigitsAndMatchingConfirmation() {
        assertNotNull(validatePinChange("12ab", "5678", "5678"))
        assertNotNull(validatePinChange("1234", "123", "123"))
        assertNotNull(validatePinChange("1234", "1234567", "1234567"))
        assertNotNull(validatePinChange("1234", "1234", "1234"))
        assertNotNull(validatePinChange("1234", "5678", "9999"))
        assertNull(validatePinChange("1234", "5678", "5678"))
        assertNull(validatePinChange("1234", "56789", "56789"))
        assertNull(validatePinChange("1234", "567890", "567890"))
    }

    @Test
    fun profileRequiresValidNameUsernameAndOptionalValidEmail() {
        assertNotNull(validateProfileForm("P", null))
        assertNotNull(validateProfileForm("Pemilik Toko", "email-tidak-valid"))
        assertNotNull(validateProfileForm("Pemilik Toko", "a".repeat(151)))
        assertNull(validateProfileForm("Pemilik Toko", null))
        assertNull(validateProfileForm("Pemilik Toko", "pemilik@tbterminal.id"))

        assertNotNull(validateProfileForm("Pemilik Toko", "ab", "pemilik@tbterminal.id"))
        assertNotNull(validateProfileForm("Pemilik Toko", "user spasi", "pemilik@tbterminal.id"))
        assertNull(validateProfileForm("Pemilik Toko", "owner.barokah", "pemilik@tbterminal.id"))
    }
}
