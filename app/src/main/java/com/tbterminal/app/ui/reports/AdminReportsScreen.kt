package com.tbterminal.app.ui.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.tbterminal.app.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.reports.components.DailySalesReportCard
import com.tbterminal.app.ui.reports.components.CompactBusinessSummary
import com.tbterminal.app.ui.reports.components.LowStockWarningCard
import com.tbterminal.app.ui.reports.components.ReportsDateRangeFilter
import com.tbterminal.app.ui.reports.components.ReportsErrorState
import com.tbterminal.app.ui.reports.components.ReportsExportMenu
import com.tbterminal.app.ui.reports.components.ReportsLoadingState
import com.tbterminal.app.ui.reports.components.ReportsTransactionTable
import com.tbterminal.app.ui.reports.components.ReportColors
import com.tbterminal.app.ui.reports.components.SalesReportAggregateSection
import com.tbterminal.app.data.repository.ReportCsvType
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.components.TbMobileControlSheet
import com.tbterminal.app.ui.components.TbMobileFilterButton
import com.tbterminal.app.ui.components.TbMobileSheetDoneButton
import com.tbterminal.app.ui.components.TbMobileSummaryButton
import com.tbterminal.app.ui.theme.TbGreenDark
import com.tbterminal.app.ui.theme.TbBackground
import com.tbterminal.app.ui.theme.TbOutline
import com.tbterminal.app.ui.theme.TbSurface
import com.tbterminal.app.ui.theme.TbText
import com.tbterminal.app.ui.theme.TbTextMuted
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    onBackToPrevious: (() -> Unit)? = null,
    onLogout: () -> Unit
) {
    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.Reports,
        pageTitle = stringResource(R.string.owner_menu_reports),
        onBack = onBackToPrevious,
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
    onRetry: () -> Unit,
    onSalesReportRetry: () -> Unit,
    onTransactionsRetry: () -> Unit,
    onPreviousTransactionPage: () -> Unit,
    onNextTransactionPage: () -> Unit,
    onExportCsv: (ReportCsvType) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(TbBackground)) {
        val compact = maxWidth < 720.dp
        val stacked = maxWidth < 1100.dp
        var detailsExpanded by rememberSaveable { mutableStateOf(false) }
        var showMobileControls by rememberSaveable { mutableStateOf(false) }
        var showMobileSummary by rememberSaveable { mutableStateOf(false) }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 14.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 18.dp)
        ) {
        item {
            androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (compact) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f).height(50.dp),
                            color = TbSurface,
                            shape = RoundedCornerShape(15.dp),
                            border = BorderStroke(1.dp, TbOutline),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = TbGreenDark, modifier = Modifier.size(20.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Transaksi", style = MaterialTheme.typography.labelMedium, color = TbText, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                                    Text(
                                        reportsCompactRangeLabel(uiState.startDate, uiState.endDate),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TbTextMuted,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                        TbMobileFilterButton(
                            onClick = { showMobileControls = true },
                            testTag = "reports-open-controls",
                            contentDescription = "Atur periode dan ekspor",
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ReportsDateRangeFilter(
                            startDate = uiState.startDate,
                            endDate = uiState.endDate,
                            onDateRangeChanged = onDateRangeChanged,
                            modifier = Modifier.widthIn(min = 280.dp, max = 360.dp),
                        )
                        ReportsExportMenu(
                            isExporting = uiState.isExporting,
                            onExport = onExportCsv,
                        )
                    }
                }
                uiState.exportMessage?.let { message ->
                    Surface(
                        color = ReportColors.PrimarySoft,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = message,
                            color = ReportColors.PrimaryDark,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                }
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
                if (!compact) {
                    item {
                        SalesReportAggregateSection(
                            report = uiState.salesReport,
                            isLoading = uiState.isLoadingSalesReport,
                            error = uiState.salesReportError,
                            detailsExpanded = detailsExpanded,
                            onDetailsExpandedChange = { detailsExpanded = it },
                            onRetry = onSalesReportRetry,
                        )
                    }
                } else {
                    item {
                        BusinessReportContentHeader(
                            onOpenSummary = {
                                detailsExpanded = false
                                showMobileSummary = true
                            },
                        )
                    }
                }
                item {
                    val table: @Composable (Modifier) -> Unit = { tableModifier ->
                        ReportsTransactionTable(
                            transactions = uiState.transactions,
                            isLoading = uiState.isTransactionsLoading,
                            error = uiState.transactionsError,
                            page = uiState.transactionPage,
                            totalPages = uiState.transactionTotalPages,
                            totalItems = uiState.transactionTotal,
                            onRetry = onTransactionsRetry,
                            onPreviousPage = onPreviousTransactionPage,
                            onNextPage = onNextTransactionPage,
                            showHeader = !compact,
                            modifier = tableModifier
                        )
                    }
                    val insights: @Composable (Modifier) -> Unit = { insightModifier ->
                        androidx.compose.foundation.layout.Column(
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
                            if (detailsExpanded && !compact) insights(Modifier.fillMaxWidth())
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            table(Modifier.weight(if (detailsExpanded) 2f else 1f))
                            if (detailsExpanded) insights(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        }

        if (showMobileControls) {
            TbMobileControlSheet(
                title = "Filter laporan",
                subtitle = "Atur periode dan ekspor data",
                onDismiss = { showMobileControls = false },
                testTag = "reports-control-sheet",
            ) {
                ReportsDateRangeFilter(
                    startDate = uiState.startDate,
                    endDate = uiState.endDate,
                    onDateRangeChanged = onDateRangeChanged,
                    modifier = Modifier.fillMaxWidth(),
                )
                ReportsExportMenu(
                    isExporting = uiState.isExporting,
                    onExport = {
                        showMobileControls = false
                        onExportCsv(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    fillWidth = true,
                )
                TbMobileSheetDoneButton(
                    onClick = { showMobileControls = false },
                    testTag = "reports-control-done",
                )
            }
        }

        if (compact && showMobileSummary) {
            TbMobileControlSheet(
                title = "Ringkasan bisnis",
                subtitle = reportsCompactRangeLabel(uiState.startDate, uiState.endDate),
                onDismiss = {
                    detailsExpanded = false
                    showMobileSummary = false
                },
                testTag = "reports-summary-sheet",
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 560.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CompactBusinessSummary(
                        report = uiState.salesReport,
                        isLoading = uiState.isLoadingSalesReport,
                        error = uiState.salesReportError,
                        onRetry = onSalesReportRetry,
                    )
                    if (uiState.salesReport != null) {
                        SalesReportAggregateSection(
                            report = uiState.salesReport,
                            isLoading = false,
                            error = null,
                            detailsExpanded = detailsExpanded,
                            onDetailsExpandedChange = { detailsExpanded = it },
                            onRetry = onSalesReportRetry,
                            showPrimaryKpis = false,
                        )
                    }
                    if (detailsExpanded && uiState.salesReport != null) {
                        DailySalesReportCard(
                            startDate = uiState.startDate,
                            endDate = uiState.endDate,
                            dailySales = uiState.dailySales,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        LowStockWarningCard(
                            metrics = uiState.dashboardMetrics,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                TbMobileSheetDoneButton(
                    onClick = {
                        detailsExpanded = false
                        showMobileSummary = false
                    },
                    testTag = "reports-summary-done",
                )
            }
        }
    }
}

@Composable
private fun BusinessReportContentHeader(
    onOpenSummary: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Daftar transaksi",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = TbText,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        )
        TbMobileSummaryButton(
            onClick = onOpenSummary,
            testTag = "reports-open-summary",
        )
    }
}

private fun reportsCompactRangeLabel(startDate: String, endDate: String): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMM", Locale.forLanguageTag("id-ID"))
    val start = runCatching { LocalDate.parse(startDate).format(formatter) }.getOrDefault(startDate)
    val end = runCatching { LocalDate.parse(endDate).format(formatter) }.getOrDefault(endDate)
    return if (startDate == endDate) end else "$start – $end"
}
