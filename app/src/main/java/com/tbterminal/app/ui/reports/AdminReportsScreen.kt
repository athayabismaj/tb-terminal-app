package com.tbterminal.app.ui.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.R
import com.tbterminal.app.data.repository.ReportCsvType
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.components.TbMobileControlSheet
import com.tbterminal.app.ui.components.TopFloatingCapsule
import androidx.compose.ui.zIndex
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.reports.components.CompactBusinessSummary
import com.tbterminal.app.ui.reports.components.DailySalesReportCard
import com.tbterminal.app.ui.reports.components.LowStockWarningCard
import com.tbterminal.app.ui.reports.components.ReportColors
import com.tbterminal.app.ui.reports.components.ReportsDateRangeFilter
import com.tbterminal.app.ui.reports.components.ReportsErrorState
import com.tbterminal.app.ui.reports.components.ReportsExportMenu
import com.tbterminal.app.ui.reports.components.ReportsLoadingState
import com.tbterminal.app.ui.reports.components.ReportsTransactionTable
import com.tbterminal.app.ui.reports.components.SalesReportAggregateSection
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
    onLogout: () -> Unit,
    onDismissFeedback: () -> Unit = {},
) {
    val todayStr = LocalDate.now().toString()
    val isFiltered = uiState.startDate != todayStr || uiState.endDate != todayStr
    var showFilterPopup by rememberSaveable { mutableStateOf(false) }
    var showSummaryPopup by rememberSaveable { mutableStateOf(false) }

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.Reports,
        pageTitle = stringResource(R.string.owner_menu_reports),
        onBack = onBackToPrevious,
        showPageHeader = true,
        topBarActions = {
            // Button 1: Filter [ 🎛️ ]
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { showFilterPopup = true },
                    modifier = Modifier.size(36.dp).testTag("reports-open-filter")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = "Filter laporan",
                        tint = if (isFiltered) Color(0xFF256B57) else Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (isFiltered) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 6.dp)
                            .size(8.dp)
                            .background(Color(0xFFF59E0B), CircleShape)
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                }
            }

            // Button 2: Ringkasan [ 📑 ]
            IconButton(
                onClick = { showSummaryPopup = true },
                modifier = Modifier.size(36.dp).testTag("reports-open-summary")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = "Ringkasan laporan",
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(20.dp)
                )
            }
        },
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
        onLogout = onLogout,
    ) { contentModifier ->
        Box(modifier = contentModifier.fillMaxSize()) {
            RefreshableContent(
                isRefreshing = uiState.isLoading && uiState.dashboardMetrics != null,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                AdminReportsContent(
                    uiState = uiState,
                    showFilterPopup = showFilterPopup,
                    onDismissFilter = { showFilterPopup = false },
                    showSummaryPopup = showSummaryPopup,
                    onDismissSummary = { showSummaryPopup = false },
                    onDateRangeChanged = onDateRangeChanged,
                    onRetry = onRetry,
                    onSalesReportRetry = onSalesReportRetry,
                    onTransactionsRetry = onTransactionsRetry,
                    onPreviousTransactionPage = onPreviousTransactionPage,
                    onNextTransactionPage = onNextTransactionPage,
                    onExportCsv = onExportCsv,
                    modifier = Modifier,
                )
            }
            TopFloatingCapsule(
                message = uiState.exportMessage,
                error = if (uiState.dashboardMetrics != null) uiState.error else null,
                onDismiss = onDismissFeedback,
                modifier = Modifier.align(Alignment.TopCenter).zIndex(99f)
            )
        }
    }
}

@Composable
fun AdminReportsContent(
    uiState: AdminReportsUiState,
    showFilterPopup: Boolean = false,
    onDismissFilter: () -> Unit = {},
    showSummaryPopup: Boolean = false,
    onDismissSummary: () -> Unit = {},
    onDateRangeChanged: (LocalDate, LocalDate) -> Unit,
    onRetry: () -> Unit,
    onSalesReportRetry: () -> Unit,
    onTransactionsRetry: () -> Unit,
    onPreviousTransactionPage: () -> Unit,
    onNextTransactionPage: () -> Unit,
    onExportCsv: (ReportCsvType) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        val compact = maxWidth < 700.dp
        val stacked = maxWidth < 1100.dp
        var detailsExpanded by rememberSaveable { mutableStateOf(false) }
        var showMobileControls by rememberSaveable { mutableStateOf(false) }
        var showMobileSummary by rememberSaveable { mutableStateOf(false) }

        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxSize()
                .widthIn(max = 1280.dp)
                .padding(
                    horizontal = if (compact) 16.dp else 28.dp,
                    vertical = if (compact) 12.dp else 20.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (compact) {
                        val todayStr = LocalDate.now().toString()
                        val isFiltered = uiState.startDate != todayStr || uiState.endDate != todayStr
                        // Active filter chips row on compact screen
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Filter:",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEAF3EF),
                                border = BorderStroke(1.dp, Color(0xFF256B57).copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = reportsCompactRangeLabel(uiState.startDate, uiState.endDate),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF256B57)
                                    )
                                }
                            }

                            if (isFiltered) {
                                Text(
                                    text = "Reset",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            val now = LocalDate.now()
                                            onDateRangeChanged(now, now)
                                        }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ReportsDateRangeFilter(
                                startDate = uiState.startDate,
                                endDate = uiState.endDate,
                                onDateRangeChanged = onDateRangeChanged,
                            )
                            ReportsExportMenu(
                                isExporting = uiState.isExporting,
                                onExport = onExportCsv,
                                compact = false,
                            )
                        }
                    }

                    uiState.exportMessage?.let { message ->
                        Surface(
                            color = ReportColors.PrimarySoft,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ReportColors.Primary.copy(alpha = 0.2f)),
                        ) {
                            Text(
                                text = message,
                                color = ReportColors.Primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
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
                            onRetry = onRetry,
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
                                modifier = tableModifier,
                            )
                        }
                        val insights: @Composable (Modifier) -> Unit = { insightModifier ->
                            Column(
                                modifier = insightModifier,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
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

                        if (stacked) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                table(Modifier.fillMaxWidth())
                                if (detailsExpanded && !compact) insights(Modifier.fillMaxWidth())
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                            ) {
                                table(Modifier.weight(if (detailsExpanded) 2f else 1f))
                                if (detailsExpanded) insights(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        if (compact && (showMobileControls || showFilterPopup)) {
            TbMobileControlSheet(
                title = "Filter laporan",
                subtitle = "Atur periode dan ekspor data",
                onDismiss = {
                    showMobileControls = false
                    onDismissFilter()
                },
                testTag = "reports-control-sheet",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "PERIODE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.8.sp,
                    )
                    ReportsDateRangeFilter(
                        startDate = uiState.startDate,
                        endDate = uiState.endDate,
                        onDateRangeChanged = onDateRangeChanged,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                HorizontalDivider(color = ReportColors.Slate100, thickness = 1.dp)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "EKSPOR DATA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.8.sp,
                    )
                    ReportsExportMenu(
                        isExporting = uiState.isExporting,
                        onExport = {
                            showMobileControls = false
                            onDismissFilter()
                            onExportCsv(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        fillWidth = true,
                        compact = true,
                    )
                }

                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = {
                        showMobileControls = false
                        onDismissFilter()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("reports-control-done"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ReportColors.Primary,
                        contentColor = Color.White,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                ) {
                    Text(
                        text = "Selesai",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp,
                    )
                }
            }
        }

        if (compact && (showMobileSummary || showSummaryPopup)) {
            TbMobileControlSheet(
                title = "Ringkasan bisnis",
                subtitle = reportsCompactRangeLabel(uiState.startDate, uiState.endDate),
                onDismiss = {
                    detailsExpanded = false
                    showMobileSummary = false
                    onDismissSummary()
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
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = {
                        detailsExpanded = false
                        showMobileSummary = false
                        onDismissSummary()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("reports-summary-done"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ReportColors.Primary,
                        contentColor = Color.White,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                ) {
                    Text(
                        text = "Selesai",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun BusinessReportContentHeader(
    onOpenSummary: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Daftar Transaksi",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = ReportColors.OnSurface,
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, ReportColors.OutlineSoft, RoundedCornerShape(12.dp))
                .background(Color.White)
                .clickable(onClick = onOpenSummary)
                .testTag("reports-open-summary"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.FactCheck,
                contentDescription = "Ringkasan",
                tint = Color(0xFF334155),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private fun reportsCompactRangeLabel(startDate: String, endDate: String): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMM", Locale.forLanguageTag("id-ID"))
    val start = runCatching { LocalDate.parse(startDate).format(formatter) }.getOrDefault(startDate)
    val end = runCatching { LocalDate.parse(endDate).format(formatter) }.getOrDefault(endDate)
    return if (startDate == endDate) end else "$start – $end"
}
