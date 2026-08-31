package com.tbterminal.app.ui.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthLayoutTest {
    @Test
    fun `phone uses compact auth layout and tablet uses wide layout`() {
        assertFalse(usesWideAuthLayout(411))
        assertTrue(usesWideAuthLayout(720))
        assertTrue(usesWideAuthLayout(1280))
    }

    @Test
    fun `pin submit requires exactly six digits and idle request`() {
        assertFalse(canSubmitPin("12345", false))
        assertTrue(canSubmitPin("123456", false))
        assertFalse(canSubmitPin("123456", true))
        assertFalse(canSubmitPin("12345a", false))
    }
}
