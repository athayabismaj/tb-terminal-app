package com.tbterminal.app.ui.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveLayoutTest {
    @Test
    fun `phone widths are compact`() {
        assertEquals(TbWindowWidthClass.Compact, tbWindowWidthClass(599.dp))
    }

    @Test
    fun `tablet widths are medium`() {
        assertEquals(TbWindowWidthClass.Medium, tbWindowWidthClass(600.dp))
        assertEquals(TbWindowWidthClass.Medium, tbWindowWidthClass(899.dp))
    }

    @Test
    fun `large tablet widths are expanded`() {
        assertEquals(TbWindowWidthClass.Expanded, tbWindowWidthClass(900.dp))
    }
}
