package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.runtime.Immutable
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

enum class KeuanganActionId {
    DAILY_CASH,
    CASH_EXPENSES,
    RECEIVABLES,
    RECEIVABLE_PAYMENTS,
    SUPPLIER_DEBTS,
    FINANCE_REPORTS
}

@Immutable
data class KeuanganUiState(
    val userName: String = "Pemilik Toko",
    val synced: Boolean = true,
    val lastCashBalance: Double = 0.0,
    val hasActiveSession: Boolean = false,
    val todayCashInflow: Double = 0.0,
    val activeReceivables: Double = 0.0,
    val receivableNotesCount: Int = 0,
    val duePayables: Double = 0.0,
    val payableNotesCount: Int = 0,
    val todayExpensesTotal: Double = 0.0,
    val balanceHidden: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    val selectedNav: DashboardNavDestination = DashboardNavDestination.KEUANGAN
)

private val keuanganDecimalSymbols = DecimalFormatSymbols(Locale.forLanguageTag("id-ID")).apply {
    groupingSeparator = '.'
    decimalSeparator = ','
}

internal fun formatKeuanganHeroMetric(amount: Double): String {
    val absVal = abs(amount)
    val sign = if (amount < 0) "-" else ""
    return when {
        absVal >= 1_000_000_000.0 -> {
            val fmt = DecimalFormat("#,##0.##", keuanganDecimalSymbols)
            "${sign}Rp ${fmt.format(absVal / 1_000_000_000.0)} M"
        }
        absVal >= 1_000_000.0 -> {
            val fmt = DecimalFormat("#,##0.##", keuanganDecimalSymbols)
            "${sign}Rp ${fmt.format(absVal / 1_000_000.0)} jt"
        }
        else -> formatRupiahSpaced(amount)
    }
}

internal fun formatKeuanganCashDelta(amount: Double): String {
    val formatted = formatKeuanganHeroMetric(abs(amount))
    return if (amount >= 0.0) "+$formatted" else "-$formatted"
}

internal fun sampleReferenceKeuanganState(): KeuanganUiState = KeuanganUiState(
    lastCashBalance = 3_250_000.0,
    hasActiveSession = false,
    todayCashInflow = 480_000.0,
    activeReceivables = 380_000.0,
    receivableNotesCount = 3,
    duePayables = 1_250_000.0,
    payableNotesCount = 2,
    todayExpensesTotal = 120_000.0,
    balanceHidden = false,
    loading = false,
    error = null,
    selectedNav = DashboardNavDestination.KEUANGAN
)
