package com.tbterminal.app.ui.stockreport

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class StockMovementPeriod {
    ALL,
    DAY,
    WEEK,
    MONTH,
    DATE,
}

/** The API accepts inclusive YYYY-MM-DD dates in the store's Asia/Jakarta timezone. */
internal fun stockMovementDateRange(
    period: StockMovementPeriod,
    selectedDate: String?,
    today: LocalDate = LocalDate.now(java.time.ZoneId.of("Asia/Jakarta")),
): Pair<String?, String?> = when (period) {
    StockMovementPeriod.ALL -> null to null
    StockMovementPeriod.DAY -> today.toString() to today.toString()
    StockMovementPeriod.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString() to today.toString()
    StockMovementPeriod.MONTH -> today.withDayOfMonth(1).toString() to today.toString()
    StockMovementPeriod.DATE -> selectedDate?.let { it to it } ?: (null to null)
}
