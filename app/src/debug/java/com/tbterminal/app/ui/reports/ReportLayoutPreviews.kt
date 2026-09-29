package com.tbterminal.app.ui.reports

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tbterminal.app.ui.offlinereports.OfflineReportScreen
import com.tbterminal.app.ui.offlinereports.OfflineReportUiState
import com.tbterminal.app.ui.stockreport.StockReportScreen
import com.tbterminal.app.ui.stockreport.StockReportUiState
import com.tbterminal.app.ui.theme.TbterminalappTheme
import com.tbterminal.app.data.remote.DashboardMetricsDto
import com.tbterminal.app.data.remote.PaymentMethodSummaryDto
import com.tbterminal.app.data.remote.SalesReceivableSummaryDto
import com.tbterminal.app.data.remote.SalesReportRangeDto
import com.tbterminal.app.data.remote.SalesReportResponseDto
import com.tbterminal.app.data.remote.SalesReportTotalsDto
import com.tbterminal.app.data.remote.TransactionStatusSummaryDto
import java.math.BigDecimal
import java.time.LocalDate

@Preview(name = "Penjualan & Keuangan - Phone", widthDp = 412, heightDp = 915, showBackground = true)
@Preview(name = "Penjualan & Keuangan - Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
fun ReportSalesLayoutPreview() {
    TbterminalappTheme {
        AdminReportsContent(
            uiState = businessReportPreviewState(),
            onDateRangeChanged = { _, _ -> },
            onRetry = {},
            onSalesReportRetry = {},
            onTransactionsRetry = {},
            onPreviousTransactionPage = {},
            onNextTransactionPage = {},
            onExportCsv = {}
        )
    }
}

private fun businessReportPreviewState() = AdminReportsUiState(
    dashboardMetrics = DashboardMetricsDto(
        totalRevenueToday = 2_450_000.0,
        totalRevenueThisMonth = 31_800_000.0,
        totalActiveReceivables = 4_250_000.0,
        activeReceivableCount = 8,
        lowStockCount = 3,
        lowStockItems = emptyList(),
    ),
    salesReport = SalesReportResponseDto(
        range = SalesReportRangeDto("2026-09-02", "2026-09-08"),
        totals = SalesReportTotalsDto(
            transactionCount = 42,
            grossRevenue = BigDecimal("12450000"),
            paidAmount = BigDecimal("10900000"),
            outstandingAmount = BigDecimal("1550000"),
            grossProfit = BigDecimal("3210000"),
        ),
        paymentMethods = listOf(
            PaymentMethodSummaryDto("CASH", 30, BigDecimal("7200000")),
            PaymentMethodSummaryDto("QRIS", 12, BigDecimal("3700000")),
        ),
        transactionStatuses = listOf(
            TransactionStatusSummaryDto("PAID", 36, BigDecimal("10100000"), BigDecimal("10100000")),
            TransactionStatusSummaryDto("DEBT", 6, BigDecimal("2350000"), BigDecimal("800000")),
        ),
        receivables = SalesReceivableSummaryDto(
            createdReceivableAmount = BigDecimal("2350000"),
            paidAmount = BigDecimal("800000"),
            remainingAmount = BigDecimal("1550000"),
            receivableCount = 6,
        ),
    ),
    isLoading = false,
)

@Preview(name = "Stok - Phone", widthDp = 412, heightDp = 915, showBackground = true)
@Preview(name = "Stok - Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
fun StockReportLayoutPreview() {
    TbterminalappTheme {
        StockReportScreen(
            modifier = Modifier,
            uiState = StockReportUiState(isLoading = false),
            onSearchChanged = {},
            onCategoryFilterChanged = {},
            onProductSelected = {},
            onMovementPeriodSelected = {},
            onMovementDateSelected = {},
            onPreviousPage = {},
            onNextPage = {}
        )
    }
}

@Preview(name = "Laporan Perangkat - Phone", widthDp = 412, heightDp = 915, showBackground = true)
@Preview(name = "Laporan Perangkat - Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
fun OfflineReportLayoutPreview() {
    val today = LocalDate.now()
    TbterminalappTheme {
        OfflineReportScreen(
            uiState = OfflineReportUiState(
                startDate = today,
                endDate = today,
                customStartInput = today.toString(),
                customEndInput = today.toString(),
                isLoading = false
            ),
            onPresetSelected = {},
            onCustomStartChanged = {},
            onCustomEndChanged = {},
            onApplyCustomRange = {},
            onRefresh = {}
        )
    }
}
