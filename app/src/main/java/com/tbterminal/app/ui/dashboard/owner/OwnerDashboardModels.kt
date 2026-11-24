package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.runtime.Immutable
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

enum class Period {
    HARI,
    MINGGU,
    BULAN,
    KUSTOM
}

enum class QuickActionType {
    CATAT_BELI,
    CEK_STOK,
    LAPORAN
}

enum class DashboardTileType {
    PIUTANG_AKTIF,
    STOK_KRITIS
}

enum class DashboardNavDestination {
    BERANDA,
    TRANSAKSI,
    KEUANGAN,
    STOK,
    MENU
}

@Immutable
data class DayPoint(
    val dateLabel: String,
    val revenue: Double,
    val isToday: Boolean = false
)

@Immutable
data class TransactionUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: Double
)

sealed interface StockStatus {
    data object Safe : StockStatus
    data class Critical(val count: Long) : StockStatus
}

@Immutable
data class DashboardUiState(
    val userName: String = "Pemilik Toko",
    val period: Period = Period.HARI,
    val customRange: Pair<Long, Long>? = null,
    val balanceHidden: Boolean = false,
    val netSales: Double = 0.0,
    val deltaPercent: Double? = null,
    val transactionCount: Long = 0L,
    val estimatedProfit: Double = 0.0,
    val cashIn: Double = 0.0,
    val receivables: Double = 0.0,
    val stockStatus: StockStatus = StockStatus.Safe,
    val chart: List<DayPoint> = emptyList(),
    val recent: List<TransactionUi> = emptyList(),
    val synced: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null
)

private val indonesianSymbols = DecimalFormatSymbols(Locale.forLanguageTag("id-ID")).apply {
    groupingSeparator = '.'
    decimalSeparator = ','
}

internal fun formatRupiahSpaced(amount: Double): String {
    val rounded = amount.roundToLong()
    val sign = if (rounded < 0) "-" else ""
    val formatter = DecimalFormat("#,##0", indonesianSymbols)
    return "${sign}Rp ${formatter.format(abs(rounded))}"
}

internal fun formatRupiahCompact(amount: Double): String {
    val absVal = abs(amount)
    val sign = if (amount < 0) "-" else ""
    return when {
        absVal >= 1_000_000_000.0 -> "${sign}Rp ${formatShortDecimal(absVal / 1_000_000_000.0, maxDecimals = 1)} M"
        absVal >= 1_000_000.0 -> "${sign}Rp ${formatShortDecimal(absVal / 1_000_000.0, maxDecimals = 1)} jt"
        absVal >= 1_000.0 -> "${sign}Rp ${formatShortDecimal(absVal / 1_000.0, maxDecimals = 0)} rb"
        else -> formatRupiahSpaced(amount)
    }
}

internal fun formatChartBarCompact(amount: Double): String {
    val absVal = abs(amount)
    return when {
        absVal >= 1_000_000_000.0 -> "${formatShortDecimal(absVal / 1_000_000_000.0, maxDecimals = 2)} M"
        absVal >= 1_000_000.0 -> "${formatShortDecimal(absVal / 1_000_000.0, maxDecimals = 2)} jt"
        absVal >= 1_000.0 -> "${formatShortDecimal(absVal / 1_000.0, maxDecimals = 0)} rb"
        else -> "${absVal.roundToLong()}"
    }
}

private fun formatShortDecimal(value: Double, maxDecimals: Int): String {
    val pattern = if (maxDecimals <= 0) "#,##0" else "#,##0.${"#".repeat(maxDecimals)}"
    val formatter = DecimalFormat(pattern, indonesianSymbols)
    return formatter.format(value)
}

internal fun formatCustomRangeChipLabel(range: Pair<Long, Long>?): String {
    if (range == null) return "Kustom"
    val start = Instant.ofEpochMilli(range.first).atZone(ZoneOffset.UTC).toLocalDate()
    val end = Instant.ofEpochMilli(range.second).atZone(ZoneOffset.UTC).toLocalDate()
    val startMonth = indonesianShortMonth(start.monthValue)
    val endMonth = indonesianShortMonth(end.monthValue)
    return if (start.year == end.year && start.monthValue == end.monthValue) {
        if (start.dayOfMonth == end.dayOfMonth) {
            "${start.dayOfMonth} $startMonth"
        } else {
            "${start.dayOfMonth}\u2013${end.dayOfMonth} $endMonth"
        }
    } else {
        "${start.dayOfMonth} $startMonth\u2013${end.dayOfMonth} $endMonth"
    }
}

internal fun periodComparisonLabel(period: Period): String = when (period) {
    Period.HARI -> "dari kemarin"
    Period.MINGGU -> "dari minggu lalu"
    Period.BULAN -> "dari bulan lalu"
    Period.KUSTOM -> "dari periode sebelumnya"
}

internal fun indonesianShortMonth(month: Int): String = when (month) {
    1 -> "Jan"
    2 -> "Feb"
    3 -> "Mar"
    4 -> "Apr"
    5 -> "Mei"
    6 -> "Jun"
    7 -> "Jul"
    8 -> "Agt"
    9 -> "Sep"
    10 -> "Okt"
    11 -> "Nov"
    12 -> "Des"
    else -> month.toString()
}

internal fun sampleReferenceDashboardState(recentCount: Int = 5): DashboardUiState {
    val allTransactions = listOf(
        TransactionUi("trx-0124", "Penjualan #0124", "10:42 · Tunai", 185_000.0),
        TransactionUi("trx-0123", "Penjualan #0123", "09:58 · QRIS", 62_500.0),
        TransactionUi("trx-0122", "Penjualan #0122", "09:15 · Tunai", 340_000.0),
        TransactionUi("trx-0121", "Penjualan #0121", "08:47 · Tunai", 128_000.0),
        TransactionUi("trx-0120", "Penjualan #0120", "08:21 · QRIS", 95_500.0)
    )
    // Sample 5-day chart matching HTML heights (48, 72, 40, 88, 104) summing to 9.8 jt with today = 2.45 jt
    val chartPoints = listOf(
        DayPoint("16 Nov", 1_550_000.0, isToday = false),
        DayPoint("17 Nov", 2_300_000.0, isToday = false),
        DayPoint("18 Nov", 1_200_000.0, isToday = false),
        DayPoint("19 Nov", 2_300_000.0, isToday = false),
        DayPoint("20 Nov", 2_450_000.0, isToday = true)
    )
    return DashboardUiState(
        userName = "Pemilik Toko",
        period = Period.HARI,
        balanceHidden = false,
        netSales = 2_450_000.0,
        deltaPercent = 12.0,
        transactionCount = 24L,
        estimatedProfit = 612_000.0,
        cashIn = 2_100_000.0,
        receivables = 380_000.0,
        stockStatus = StockStatus.Safe,
        chart = chartPoints,
        recent = allTransactions.take(recentCount),
        synced = true,
        isLoading = false,
        error = null
    )
}
