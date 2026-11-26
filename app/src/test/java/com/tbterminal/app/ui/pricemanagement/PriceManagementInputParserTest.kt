package com.tbterminal.app.ui.pricemanagement

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PriceManagementInputParserTest {
    @Test
    fun parseCatalogBuyPriceInput_allowsIdrThousandsAndUpToTwoCommaDecimals() {
        assertEquals(BigDecimal("15000"), parseCatalogBuyPriceInput("15.000"))
        assertEquals(BigDecimal("1333.33"), parseCatalogBuyPriceInput("1.333,33"))
        assertEquals(BigDecimal.ZERO, parseCatalogBuyPriceInput("0"))

        assertNull(parseCatalogBuyPriceInput("1333.33"))
        assertNull(parseCatalogBuyPriceInput("1.333,333"))
        assertNull(parseCatalogBuyPriceInput(""))
    }

    @Test
    fun parseCatalogWholeRupiahInput_requiresWholeRupiahWithoutCents() {
        assertEquals(BigDecimal("20000"), parseCatalogWholeRupiahInput("20.000"))
        assertEquals(BigDecimal("20000"), parseCatalogWholeRupiahInput("20000,00"))
        assertEquals(BigDecimal.ZERO, parseCatalogWholeRupiahInput("0"))

        assertNull(parseCatalogWholeRupiahInput("20000,5"))
        assertNull(parseCatalogWholeRupiahInput("20000.50"))
        assertNull(parseCatalogWholeRupiahInput(""))
    }
}
