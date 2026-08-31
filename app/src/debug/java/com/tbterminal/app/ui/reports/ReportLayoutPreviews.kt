package com.tbterminal.app.ui.reports

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tbterminal.app.ui.offlinereports.OfflineReportScreen
import com.tbterminal.app.ui.offlinereports.OfflineReportUiState
import com.tbterminal.app.ui.stockreport.StockReportScreen
import com.tbterminal.app.ui.stockreport.StockReportUiState
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.time.LocalDate

@Preview(name = "Penjualan & Keuangan - Phone", widthDp = 412, heightDp = 915, showBackground = true)
@Preview(name = "Penjualan & Keuangan - Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
fun ReportSalesLayoutPreview() {
    TbterminalappTheme {
        AdminReportsContent(
            uiState = AdminReportsUiState(),
            onDateRangeChanged = { _, _ -> },
            onRefresh = {},
            onRetry = {},
            onSalesReportRetry = {},
            onTransactionsRetry = {},
            onPreviousTransactionPage = {},
            onNextTransactionPage = {},
            onExportCsv = {}
        )
    }
}

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
            onPreviousPage = {},
            onNextPage = {}
        )
    }
}

@Preview(name = "Laporan Lokal - Phone", widthDp = 412, heightDp = 915, showBackground = true)
@Preview(name = "Laporan Lokal - Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
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
