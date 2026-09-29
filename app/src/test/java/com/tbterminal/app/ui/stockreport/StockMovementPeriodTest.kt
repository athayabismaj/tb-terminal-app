package com.tbterminal.app.ui.stockreport

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StockMovementPeriodTest {
    private val today = LocalDate.of(2026, 9, 24)

    @Test
    fun stockCardStartsWithTodayPeriod() {
        assertEquals(StockMovementPeriod.DAY, StockReportUiState().movementPeriod)
    }

    @Test
    fun allKeepsEntireHistory() {
        assertEquals(null to null, stockMovementDateRange(StockMovementPeriod.ALL, null, today))
    }

    @Test
    fun dayUsesCurrentStoreDate() {
        assertEquals("2026-09-24" to "2026-09-24", stockMovementDateRange(StockMovementPeriod.DAY, null, today))
    }

    @Test
    fun weekStartsOnMondayAndEndsToday() {
        assertEquals("2026-09-21" to "2026-09-24", stockMovementDateRange(StockMovementPeriod.WEEK, null, today))
    }

    @Test
    fun monthStartsOnFirstDayAndEndsToday() {
        assertEquals("2026-09-01" to "2026-09-24", stockMovementDateRange(StockMovementPeriod.MONTH, null, today))
    }

    @Test
    fun calendarSelectionFiltersOneDay() {
        assertEquals("2026-01-02" to "2026-01-02", stockMovementDateRange(StockMovementPeriod.DATE, "2026-01-02", today))
    }
}
