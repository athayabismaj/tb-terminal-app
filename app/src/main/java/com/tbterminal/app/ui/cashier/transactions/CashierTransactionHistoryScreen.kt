package com.tbterminal.app.ui.cashier.transactions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.local.dao.TransactionDao
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.sync.OfflineCheckoutSyncService
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.components.TopFloatingCapsule
import com.tbterminal.app.ui.dashboard.cashier.CashierDashboardShell
import com.tbterminal.app.ui.dashboard.cashier.CashierDestination
import com.tbterminal.app.ui.salestransactions.AdminTransactionHistoryUiState
import com.tbterminal.app.ui.salestransactions.SalesSortOrder
import com.tbterminal.app.ui.salestransactions.TransactionHistoryContent
import java.time.LocalDate
import java.time.ZoneId

private val WIB_ZONE = ZoneId.of("Asia/Jakarta")

@Composable
fun CashierTransactionHistoryScreen(
    name: String,
    role: String,
    cashReconciliationRepository: CashReconciliationRepository,
    transactionDao: TransactionDao? = null,
    offlineCheckoutSyncService: OfflineCheckoutSyncService? = null,
    onDashboardClick: () -> Unit,
    onPosClick: () -> Unit,
    onCashSessionClick: () -> Unit = {},
    onTransactionHistoryClick: () -> Unit = {},
    onStockCheckClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onReceiptClick: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: CashierTransactionHistoryViewModel = viewModel(
        factory = CashierTransactionHistoryViewModel.factory(
            repository = cashReconciliationRepository,
            transactionDao = transactionDao,
            offlineCheckoutSyncService = offlineCheckoutSyncService
        )
    )
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

    val adminUiState = remember(uiState) {
        AdminTransactionHistoryUiState(
            transactions = uiState.transactions,
            localPendingTransactions = uiState.localPendingTransactions,
            isLoading = uiState.isLoading,
            error = uiState.errorMessage,
            query = uiState.query,
            statusFilter = uiState.statusFilter,
            paymentMethodFilter = uiState.paymentMethodFilter,
            sortOrder = uiState.sortOrder,
            selectedDate = uiState.selectedDate,
            startDate = uiState.startDate,
            endDate = uiState.endDate,
            selectedPreset = uiState.selectedPreset,
            currentStart = uiState.currentStart.toInt(),
            currentEnd = uiState.currentEnd.toInt(),
            total = uiState.total,
            page = uiState.page,
            totalPages = uiState.totalPages,
            hasMorePages = uiState.page < uiState.totalPages,
            localSyncMessage = uiState.localSyncMessage,
            isBulkSyncing = uiState.isBulkSyncing,
            bulkSyncProgressMessage = uiState.bulkSyncProgressMessage
        )
    }

    LaunchedEffect(Unit) {
        viewModel.loadTransactions(page = 1)
    }

    CashierDashboardShell(
        userName = name,
        role = role,
        activeDestination = CashierDestination.TransactionHistory,
        topBarActions = {
            val isSearchActive = isSearchExpanded || uiState.query.isNotEmpty()

            // Button 1: Search [ 🔍 ]
            Surface(
                onClick = { isSearchExpanded = !isSearchExpanded },
                shape = CircleShape,
                color = Color.Transparent,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("cashier-history-toggle-search")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Cari transaksi",
                        tint = if (isSearchActive) Color(0xFF004528) else Color(0xFF475569),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Button 2: Filter [ ⚙️ ]
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    onClick = { showFilterPopup = true },
                    shape = CircleShape,
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("cashier-history-open-filters")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = "Filter transaksi",
                            tint = if (isFiltered) Color(0xFF004528) else Color(0xFF475569),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (isFiltered) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 2.dp, end = 2.dp)
                            .size(7.dp)
                            .background(Color(0xFFE5A93C), CircleShape)
                            .border(1.dp, Color.White, CircleShape)
                    )
                }
            }

            // Button 3: Rekap / Ringkasan Shift Kasir [ 📑 ]
            Surface(
                onClick = { showSummaryPopup = true },
                shape = CircleShape,
                color = Color.Transparent,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("cashier-history-summary")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = "Tampilkan rekap transaksi",
                        tint = if (showSummaryPopup) Color(0xFF004528) else Color(0xFF475569),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        onDashboardClick = onDashboardClick,
        onPosClick = onPosClick,
        onCashSessionClick = onCashSessionClick,
        onTransactionHistoryClick = onTransactionHistoryClick,
        onStockCheckClick = onStockCheckClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    ) { contentModifier ->
        Box(modifier = contentModifier.fillMaxSize()) {
            RefreshableContent(
                isRefreshing = uiState.isLoading && uiState.transactions.isNotEmpty(),
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                TransactionHistoryContent(
                    state = adminUiState,
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
                    isSearchExpanded = isSearchExpanded,
                    onToggleSearch = { isSearchExpanded = !isSearchExpanded },
                    showFilterPopup = showFilterPopup,
                    onDismissFilter = { showFilterPopup = false },
                    onApplyFilter = { status, payment, start, end, preset, sortOrder ->
                        viewModel.applyFilters(status, payment, start, end, preset, sortOrder)
                    },
                    showSummaryPopup = showSummaryPopup,
                    onDismissSummary = { showSummaryPopup = false },
                    onResetAllFilters = viewModel::resetFilters,
                    onSortOrderChanged = viewModel::updateSortOrder,
                    modifier = Modifier.fillMaxSize()
                )
            }
            TopFloatingCapsule(
                message = uiState.receiptMessage,
                error = uiState.errorMessage,
                onDismiss = viewModel::clearFeedback,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(99f)
            )
        }
    }
}
