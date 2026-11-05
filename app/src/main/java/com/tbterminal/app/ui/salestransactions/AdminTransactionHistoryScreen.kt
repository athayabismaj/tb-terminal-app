package com.tbterminal.app.ui.salestransactions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.local.dao.TransactionDao
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.sync.OfflineCheckoutSyncService
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.components.TopFloatingCapsule
import com.tbterminal.app.ui.dashboard.DashboardBrandGreenDark
import com.tbterminal.app.ui.dashboard.DashboardTextSecondary
import com.tbterminal.app.ui.dashboard.DashboardWarningOrange
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AdminTransactionHistoryScreen(
    name: String,
    role: String,
    cashReconciliationRepository: CashReconciliationRepository,
    transactionDao: TransactionDao? = null,
    offlineCheckoutSyncService: OfflineCheckoutSyncService? = null,
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
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onReceiptClick: (String) -> Unit,
    onLogout: () -> Unit,
    onBack: (() -> Unit)? = null,
    onNewTransactionClick: () -> Unit = {},
    viewModel: AdminTransactionHistoryViewModel = viewModel(
        factory = AdminTransactionHistoryViewModel.factory(
            repository = cashReconciliationRepository,
            transactionDao = transactionDao,
            offlineCheckoutSyncService = offlineCheckoutSyncService,
        ),
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var showFilterPopup by remember { mutableStateOf(false) }
    var showSummaryPopup by rememberSaveable { mutableStateOf(false) }

    val todayStr = remember { LocalDate.now(WIB_ZONE).toString() }
    val isDateFiltered = uiState.selectedPreset != "Hari ini" ||
        ((uiState.startDate != null && uiState.startDate != todayStr) || (uiState.endDate != null && uiState.endDate != todayStr))
    val isStatusFiltered = uiState.statusFilter != "Semua"
    val isPaymentFiltered = uiState.paymentMethodFilter != "Semua"
    val isSortFiltered = uiState.sortOrder != SalesSortOrder.Newest
    val isFiltered = isDateFiltered || isStatusFiltered || isPaymentFiltered || isSortFiltered

    LaunchedEffect(Unit) { viewModel.loadTransactions(page = 1) }

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.SalesTransactions,
        pageTitle = "Riwayat Penjualan",
        showPageHeader = true,
        topBarActions = {
            // Button 1: Search [ 🔍 ]
            IconButton(
                onClick = { isSearchExpanded = !isSearchExpanded },
                modifier = Modifier.size(36.dp).testTag("sales-history-toggle-search")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Cari transaksi",
                    tint = if (isSearchExpanded || uiState.query.isNotEmpty()) Color(0xFF1B5E3C) else Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Button 2: Filter [ ⚙️ ]
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { showFilterPopup = true },
                    modifier = Modifier.size(36.dp).testTag("sales-history-open-filters")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = "Filter transaksi",
                        tint = if (isFiltered) Color(0xFF1B5E3C) else Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (isFiltered) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 4.dp)
                            .size(8.dp)
                            .background(Color(0xFFE5A93C), CircleShape)
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                }
            }

            // Button 3: Rekap / Ringkasan [ 📑 ]
            IconButton(
                onClick = { showSummaryPopup = true },
                modifier = Modifier.size(36.dp).testTag("sales-history-summary")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = "Tampilkan rekap transaksi",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        onBack = onBack,
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
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout,
    ) { contentModifier ->
        Box(modifier = contentModifier.fillMaxSize()) {
            RefreshableContent(
                isRefreshing = uiState.isLoading && uiState.transactions.isNotEmpty(),
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                TransactionHistoryContent(
                    state = uiState,
                    isSearchExpanded = isSearchExpanded,
                    onToggleSearch = { isSearchExpanded = !isSearchExpanded },
                    showFilterPopup = showFilterPopup,
                    onDismissFilter = { showFilterPopup = false },
                    onApplyFilter = { status, paymentMethod, start, end, preset, sortOrder ->
                        viewModel.applyFilters(status, paymentMethod, start, end, preset, sortOrder)
                    },
                    showSummaryPopup = showSummaryPopup,
                    onDismissSummary = { showSummaryPopup = false },
                    onResetAllFilters = {
                        val today = LocalDate.now(WIB_ZONE).toString()
                        viewModel.updateSearchQuery("")
                        viewModel.applyFilters(
                            status = "Semua",
                            paymentMethod = "Semua",
                            startDateStr = today,
                            endDateStr = today,
                            preset = "Hari ini",
                            sortOrder = SalesSortOrder.Newest
                        )
                    },
                    onSortOrderChanged = viewModel::updateSortOrder,
                    onSearchChanged = viewModel::updateSearchQuery,
                    onStatusChanged = viewModel::updateStatusFilter,
                    onPaymentMethodChanged = viewModel::updatePaymentMethodFilter,
                    onDatePresetSelected = viewModel::setDatePreset,
                    onDateSelected = viewModel::setDate,
                    onDateRangeSelected = viewModel::setDateRange,
                    onSyncClick = viewModel::syncLocalTransaction,
                    onSyncAllClick = viewModel::syncAllLocalTransactions,
                    onRetry = viewModel::refresh,
                    onPreviousPage = viewModel::previousPage,
                    onNextPage = viewModel::nextPage,
                    onReceiptClick = onReceiptClick,
                    modifier = Modifier,
                )
            }
            if (uiState.transactions.isNotEmpty()) {
                TopFloatingCapsule(
                    message = null,
                    error = uiState.error,
                    onDismiss = viewModel::clearError,
                    modifier = Modifier.align(Alignment.TopCenter).zIndex(99f)
                )
            }
        }
    }
}

internal fun CashTransaction.receiptNumber(): String = receiptId

internal fun BigDecimal.moneyText(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"))
    formatter.maximumFractionDigits = 0
    return formatter.format(this).replace(" ", "")
}

private val WIB_ZONE = ZoneId.of("Asia/Jakarta")

private fun parseToWib(raw: String): ZonedDateTime? {
    val trimmed = raw.trim()
    return runCatching { OffsetDateTime.parse(trimmed).atZoneSameInstant(WIB_ZONE) }.getOrNull()
        ?: runCatching { Instant.parse(trimmed).atZone(WIB_ZONE) }.getOrNull()
        ?: runCatching { LocalDateTime.parse(trimmed).atZone(WIB_ZONE) }.getOrNull()
        ?: runCatching { LocalDate.parse(trimmed.take(10)).atStartOfDay(WIB_ZONE) }.getOrNull()
}

internal fun String.displayDateTime(): String = runCatching {
    parseToWib(this)?.format(
        DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.forLanguageTag("id-ID")),
    ) ?: this
}.getOrDefault(this)

internal fun String.displayDate(): String = runCatching {
    parseToWib(this)?.format(
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID")),
    ) ?: "-"
}.getOrDefault("-")

internal fun String.displayTime(): String = runCatching {
    parseToWib(this)?.format(DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("id-ID"))) ?: "-"
}.getOrDefault("-")

internal fun String.statusColor(): Color = when (lowercase()) {
    "lunas", "paid", "success", "completed" -> DashboardBrandGreenDark
    "dp", "partial" -> DashboardWarningOrange
    "hutang", "unpaid" -> Color(0xFFEF4444)
    "voided" -> Color(0xFF7F1D1D)
    else -> DashboardTextSecondary
}

internal fun String.formatDisplayDate(): String = runCatching {
    LocalDate.parse(this).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}.getOrDefault(this)
