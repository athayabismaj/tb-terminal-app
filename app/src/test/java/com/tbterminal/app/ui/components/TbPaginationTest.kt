package com.tbterminal.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class TbPaginationTest {

    @Test
    fun `tbPaginationSupportingText handles zero count`() {
        assertEquals("0 nota", tbPaginationSupportingText(visibleCount = 0, totalCount = 0L, unit = "nota"))
        assertEquals("0 produk", tbPaginationSupportingText(visibleCount = 0, totalCount = -1L, unit = "produk"))
    }

    @Test
    fun `tbPaginationSupportingText handles all visible items`() {
        assertEquals("15 nota", tbPaginationSupportingText(visibleCount = 15, totalCount = 15L, unit = "nota"))
        assertEquals("20 produk", tbPaginationSupportingText(visibleCount = 25, totalCount = 20L, unit = "produk"))
    }

    @Test
    fun `tbPaginationSupportingText handles partial visible count`() {
        assertEquals("10 dari 50 nota", tbPaginationSupportingText(visibleCount = 10, totalCount = 50L, unit = "nota"))
    }

    @Test
    fun `tbPaginationSupportingText handles range indices`() {
        assertEquals(
            "1–10 dari 45 hutang",
            tbPaginationSupportingText(
                visibleCount = 10,
                totalCount = 45L,
                unit = "hutang",
                startIndex = 1,
                endIndex = 10
            )
        )
    }
}
