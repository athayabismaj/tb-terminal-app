package com.tbterminal.app.ui.common

import com.tbterminal.app.ui.cash.components.applyPettyCashQuickChip
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IdrMoneyInputParserTest {
    @Test
    fun parseWholeRupiahInput_acceptsPlainThousandGroupedZeroAndCommaZeroInputs() {
        assertEquals(BigDecimal("20000"), parseWholeRupiahInput("20000"))
        assertEquals(BigDecimal("20000"), parseWholeRupiahInput("20.000"))
        assertEquals(BigDecimal("1250000"), parseWholeRupiahInput("1.250.000"))
        assertEquals(BigDecimal.ZERO, parseWholeRupiahInput("0"))
        assertEquals(BigDecimal("20000"), parseWholeRupiahInput("20000,00"))
        assertEquals(BigDecimal("20000"), parseWholeRupiahInput("20.000,0"))
        assertEquals(BigDecimal("-500"), parseWholeRupiahInput("-500"))
    }

    @Test
    fun parseWholeRupiahInput_rejectsNonZeroCommaDecimalsAndMalformedDotGroupings() {
        listOf(
            "",
            "   ",
            "abc",
            "1.500,50",
            "20.000,05",
            "20000,",
            "20000,0,0",
            "20.00",
            "99.99",
            "20.",
            ".500",
            "20..000",
            "1.25.000",
        ).forEach { invalid ->
            assertNull("Expected null for '$invalid'", parseWholeRupiahInput(invalid))
        }
    }

    @Test
    fun sanitizeWholeRupiahInput_preservesThousandsAndCommaForExplicitValidation() {
        assertEquals("20.000", sanitizeWholeRupiahInput("Rp 20.000"))
        assertEquals("1.250.000", sanitizeWholeRupiahInput("1.250.000"))
        assertEquals("20000,00", sanitizeWholeRupiahInput("20000,00"))
        assertEquals("20.000,0", sanitizeWholeRupiahInput("20.000,0"))
        assertEquals("1.500,50", sanitizeWholeRupiahInput("1.500,50"))
        assertEquals("123456789012", sanitizeWholeRupiahInput("123456789012345", maxIntegerDigits = 12))
    }

    @Test
    fun toWholeRupiahInputText_dropsZeroFractionsWithoutScientificNotation() {
        assertEquals("20000", BigDecimal("20000.00").toWholeRupiahInputText())
        assertEquals("0", BigDecimal("0.00").toWholeRupiahInputText())
        assertEquals("1250000", BigDecimal("1250000").toWholeRupiahInputText())
        assertEquals("1500,5", BigDecimal("1500.50").toWholeRupiahInputText())
    }

    @Test
    fun pettyCashQuickChip_plus20rb_formatsAndParsesCleanly() {
        assertEquals("20.000", applyPettyCashQuickChip("", 20_000L))
        assertEquals("20.000", applyPettyCashQuickChip("0", 20_000L))
        assertEquals("40.000", applyPettyCashQuickChip("20.000", 20_000L))
        assertEquals("40.000", applyPettyCashQuickChip("20000,00", 20_000L))
    }
}
