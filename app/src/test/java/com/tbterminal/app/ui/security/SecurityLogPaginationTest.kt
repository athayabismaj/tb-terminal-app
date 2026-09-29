package com.tbterminal.app.ui.security

import org.junit.Assert.assertEquals
import org.junit.Test

class SecurityLogPaginationTest {
    @Test
    fun `security log requests ten items per page by default`() {
        assertEquals(10, SecurityLogUiState().limit)
    }
}
