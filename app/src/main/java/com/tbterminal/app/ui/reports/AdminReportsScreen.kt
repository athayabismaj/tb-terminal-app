package com.tbterminal.app.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.reports.components.DailySalesReportCard
import com.tbterminal.app.ui.reports.components.LowStockWarningCard
import com.tbterminal.app.ui.reports.components.ReportsDateRangeFilter
import com.tbterminal.app.ui.reports.components.ReportsErrorState
import com.tbterminal.app.ui.reports.components.ReportsLoadingState
import com.tbterminal.app.ui.reports.components.ReportsTransactionTable
import com.tbterminal.app.ui.reports.components.ReportColors
import com.tbterminal.app.ui.reports.components.SalesReportAggregateSection
import com.tbterminal.app.data.repository.ReportCsvType
import com.tbterminal.app.ui.components.RefreshableContent
import java.time.LocalDate

@Composable
fun AdminReportsScreen(
    name: String,
    role: String,
    uiState: AdminReportsUiState,
    onDateRangeChanged: (LocalDate, LocalDate) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onSalesReportRetry: () -> Unit,
    onTransactionsRetry: () -> Unit,
    onPreviousTransactionPage: () -> Unit,
    onNextTransactionPage: () -> Unit,
    onExportCsv: (ReportCsvType) -> Unit,
    onDashboardClick: () -> Unit,
    onProductsClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onProductCategoriesClick: () -> Unit,
    onProductUnitsClick: () -> Unit,
    onCashReconciliationClick: () -> Unit,
    onSalesTransactionsClick: () -> Unit,
    onReportsClick: () -> Unit,
    onPriceManagementClick: () -> Unit,
    onStockOpnameClick: () -> Unit,
    onStockOpnameFormClick: () -> Unit,
    onIncomingGoodsClick: () -> Unit,
    onIncomingGoodsFormClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onOperationalAuditClick: () -> Unit,
    onUserManagementClick: () -> Unit,
    onSecurityLogClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogout: () -> Unit
) {
    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.Reports,
        pageTitle = "Penjualan & Keuangan",
        onDashboardClick = onDashboardClick,
        onProductsClick = onProductsClick,
        onAddProductClick = onAddProductClick,
        onProductCategoriesClick = onProductCategoriesClick,
        onProductUnitsClick = onProductUnitsClick,
        onCashReconciliationClick = onCashReconciliationClick,
        onSalesTransactionsClick = onSalesTransactionsClick,
        onReportsClick = onReportsClick,
        onPriceManagementClick = onPriceManagementClick,
        onStockOpnameClick = onStockOpnameClick,
        onStockOpnameFormClick = onStockOpnameFormClick,
        onIncomingGoodsClick = onIncomingGoodsClick,
        onIncomingGoodsFormClick = onIncomingGoodsFormClick,
        onSupplierDebtsClick = onSupplierDebtsClick,
        onReceivablesClick = onReceivablesClick,
        onCustomersClick = onCustomersClick,
        onOperationalAuditClick = onOperationalAuditClick,
        onUserManagementClick = onUserManagementClick,
        onSecurityLogClick = onSecurityLogClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    ) { contentModifier ->
        RefreshableContent(
            isRefreshing = uiState.isLoading && uiState.dashboardMetrics != null,
            onRefresh = onRefresh,
            modifier = contentModifier,
        ) {
            AdminReportsContent(
            uiState = uiState,
            onDateRangeChanged = onDateRangeChanged,
            onRefresh = onRefresh,
            onRetry = onRetry,
            onSalesReportRetry = onSalesReportRetry,
            onTransactionsRetry = onTransactionsRetry,
            onPreviousTransactionPage = onPreviousTransactionPage,
            onNextTransactionPage = onNextTransactionPage,
            onExportCsv = onExportCsv,
                modifier = Modifier
            )
        }
    }
}

@Composable
fun AdminReportsContent(
    uiState: AdminReportsUiState,
    onDateRangeChanged: (LocalDate, LocalDate) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onSalesReportRetry: () -> Unit,
    onTransactionsRetry: () -> Unit,
    onPreviousTransactionPage: () -> Unit,
    onNextTransactionPage: () -> Unit,
    onExportCsv: (ReportCsvType) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(ReportColors.Background)) {
        val compact = maxWidth < 720.dp
        val stacked = maxWidth < 1100.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 14.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 18.dp)
        ) {
        item {
            ReportsDateRangeFilter(uiState.startDate, uiState.endDate, onDateRangeChanged, onRefresh)
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Ekspor CSV", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportCsvType.entries.forEach { type ->
                        OutlinedButton(onClick = { onExportCsv(type) }, enabled = !uiState.isExporting) {
                            Text(if (uiState.isExporting) "Memproses…" else type.label)
                        }
                    }
                }
                uiState.exportMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }

        when {
            uiState.isLoading && uiState.dashboardMetrics == null -> {
                item { ReportsLoadingState() }
            }
            uiState.error != null && uiState.dashboardMetrics == null -> {
                item {
                    ReportsErrorState(
                        message = uiState.error,
                        onRetry = onRetry
                    )
                }
            }
            else -> {
                item {
                    SalesReportAggregateSection(
                        report = uiState.salesReport,
                        isLoading = uiState.isLoadingSalesReport,
                        error = uiState.salesReportError,
                        onRetry = onSalesReportRetry
                    )
                }
                item {
                    val table: @Composable (Modifier) -> Unit = { tableModifier ->
                        ReportsTransactionTable(
                            transactions = uiState.transactions,
                            isLoading = uiState.isTransactionsLoading,
                            error = uiState.transactionsError,
                            currentStart = uiState.transactionCurrentStart,
                            currentEnd = uiState.transactionCurrentEnd,
                            total = uiState.transactionTotal,
                            page = uiState.transactionPage,
                            totalPages = uiState.transactionTotalPages,
                            onRetry = onTransactionsRetry,
                            onPreviousPage = onPreviousTransactionPage,
                            onNextPage = onNextTransactionPage,
                            modifier = tableModifier
                        )
                    }
                    val insights: @Composable (Modifier) -> Unit = { insightModifier ->
                        Column(
                            modifier = insightModifier,
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            DailySalesReportCard(
                                startDate = uiState.startDate,
                                endDate = uiState.endDate,
                                dailySales = uiState.dailySales,
                                modifier = Modifier.fillMaxWidth()
                            )
                            LowStockWarningCard(
                                metrics = uiState.dashboardMetrics,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    if (stacked) {
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            table(Modifier.fillMaxWidth())
                            insights(Modifier.fillMaxWidth())
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            table(Modifier.weight(2f))
                            insights(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        }
    }
}
