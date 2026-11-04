package com.tbterminal.app.ui.cashhistory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import java.time.LocalDate

@Composable
fun AdminCashSessionHistoryScreen(
    name: String,
    role: String,
    cashRepository: CashReconciliationRepository,
    onDashboardClick: () -> Unit,
    onProductsClick: () -> Unit,
    onStockOpnameClick: () -> Unit,
    onIncomingGoodsClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onReportsClick: () -> Unit,
    onOperationalAuditClick: () -> Unit,
    onShowDetail: (String) -> Unit,
    onBack: () -> Unit = {},
    onLogout: () -> Unit,
    activeDestination: AdminDestination = AdminDestination.CashSessionHistory,
    title: String = "Riwayat Kas Harian",
    viewModel: CashSessionHistoryViewModel = viewModel(
        factory = CashSessionHistoryViewModel.factory(cashRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var showFilterPopup by remember { mutableStateOf(false) }
    var showSummaryPopup by rememberSaveable { mutableStateOf(false) }

    val todayStr = remember { LocalDate.now().toString() }
    val isPeriodFiltered = (uiState.selectedPreset != null && uiState.selectedPreset != "Hari ini") ||
        (uiState.selectedPreset == null && uiState.startDate != null && (uiState.startDate != todayStr || uiState.endDate != todayStr))
    val isStatusFiltered = uiState.statusFilter != "Semua"
    val isFiltered = isPeriodFiltered || isStatusFiltered

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = activeDestination,
        pageTitle = title,
        showPageHeader = true,
        topBarActions = {
            // Button 1: Search [ 🔍 ]
            IconButton(
                onClick = { isSearchExpanded = !isSearchExpanded },
                modifier = Modifier.size(36.dp).testTag("cash-session-toggle-search")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Cari sesi kas",
                    tint = if (isSearchExpanded || uiState.searchQuery.isNotEmpty()) Color(0xFF256B57) else Color(0xFF475569),
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
                    modifier = Modifier.size(36.dp).testTag("cash-session-open-filters")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = "Filter sesi kas",
                        tint = if (isFiltered) Color(0xFF256B57) else Color(0xFF475569),
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

            // Button 3: Ringkasan [ 📑 ]
            IconButton(
                onClick = { showSummaryPopup = true },
                modifier = Modifier.size(36.dp).testTag("cash-session-summary")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = "Tampilkan ringkasan kas",
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        onDashboardClick = onDashboardClick,
        onProductsClick = onProductsClick,
        onStockOpnameClick = onStockOpnameClick,
        onIncomingGoodsClick = onIncomingGoodsClick,
        onSupplierDebtsClick = onSupplierDebtsClick,
        onReceivablesClick = onReceivablesClick,
        onCustomersClick = onCustomersClick,
        onReportsClick = onReportsClick,
        onOperationalAuditClick = onOperationalAuditClick,
        onBack = onBack,
        onLogout = onLogout
    ) { modifier ->
        RefreshableContent(
            isRefreshing = uiState.isLoading && uiState.sessions.isNotEmpty(),
            onRefresh = viewModel::refresh,
            modifier = modifier,
        ) {
            CashSessionHistoryScreen(
                modifier = androidx.compose.ui.Modifier,
                uiState = uiState,
                isSearchExpanded = isSearchExpanded,
                showFilterPopup = showFilterPopup,
                showSummaryPopup = showSummaryPopup,
                onDismissFilter = { showFilterPopup = false },
                onDismissSummary = { showSummaryPopup = false },
                onOpenFilter = { showFilterPopup = true },
                onOpenSummary = { showSummaryPopup = true },
                onSearchChanged = viewModel::setSearchQuery,
                onStatusFilterChanged = viewModel::setStatusFilter,
                onRefresh = viewModel::refresh,
                onDateChanged = viewModel::setDate,
                onDatePresetSelected = viewModel::setDatePreset,
                onPreviousDate = viewModel::previousDate,
                onNextDate = viewModel::nextDate,
                onShowDetail = onShowDetail,
                onPreviousPage = viewModel::previousPage,
                onNextPage = viewModel::nextPage,
                onDateRangeChanged = viewModel::setDateRange,
                onApplyFilter = viewModel::applyFilter,
                onResetAllFilters = viewModel::resetAllFilters
            )
        }
    }
}
