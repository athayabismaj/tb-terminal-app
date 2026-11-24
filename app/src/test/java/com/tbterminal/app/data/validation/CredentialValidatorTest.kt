package com.tbterminal.app.data.validation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialValidatorTest {

    @Test
    fun passwordRequiresBetween8And72Characters() {
        assertFalse(CredentialValidator.isValidPasswordLength(""))
        assertFalse(CredentialValidator.isValidPasswordLength("1234567"))
        assertTrue(CredentialValidator.isValidPasswordLength("12345678"))
        assertTrue(CredentialValidator.isValidPasswordLength("a".repeat(72)))
        assertFalse(CredentialValidator.isValidPasswordLength("a".repeat(73)))

        assertNotNull(CredentialValidator.validatePassword(""))
        assertNotNull(CredentialValidator.validatePassword("1234567"))
        assertNull(CredentialValidator.validatePassword("12345678"))
        assertNull(CredentialValidator.validatePassword("a".repeat(72)))
        assertNotNull(CredentialValidator.validatePassword("a".repeat(73)))
    }

    @Test
    fun pinRequires4To6NumericDigitsOnly() {
        assertFalse(CredentialValidator.isValidPin(""))
        assertFalse(CredentialValidator.isValidPin("123"))
        assertTrue(CredentialValidator.isValidPin("1234"))
        assertTrue(CredentialValidator.isValidPin("12345"))
        assertTrue(CredentialValidator.isValidPin("123456"))
        assertFalse(CredentialValidator.isValidPin("1234567"))
        assertFalse(CredentialValidator.isValidPin("12ab"))
        assertFalse(CredentialValidator.isValidPin("12ab56"))
        assertFalse(CredentialValidator.isValidPin("12 34"))

        assertNotNull(CredentialValidator.validatePin(""))
        assertNotNull(CredentialValidator.validatePin("123"))
        assertNull(CredentialValidator.validatePin("1234"))
        assertNull(CredentialValidator.validatePin("12345"))
        assertNull(CredentialValidator.validatePin("123456"))
        assertNotNull(CredentialValidator.validatePin("1234567"))
        assertNotNull(CredentialValidator.validatePin("12a4"))
    }
}
