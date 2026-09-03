package com.tbterminal.app.ui.common

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class UiFormattersTest {
    @Test
    fun formatsRupiahConsistently() {
        assertEquals("Rp 100.000", BigDecimal("100000").toRupiahText())
        assertEquals("Rp 1.250.000,5", BigDecimal("1250000.50").toRupiahText())
    }

    @Test
    fun formatsIsoDateAndTimeForUi() {
        val value = "2026-09-01T14:30:00+07:00"
        assertEquals("01 Sep 2026", value.toDisplayDate())
        assertEquals("14:30", value.toDisplayTime())
        assertEquals("01 Sep 2026 · 14:30", value.toDisplayDateTime())
    }

    @Test
    fun invalidDateDoesNotLeakRawIsoText() {
        assertEquals("-", "invalid".toDisplayDateTime())
    }
}
