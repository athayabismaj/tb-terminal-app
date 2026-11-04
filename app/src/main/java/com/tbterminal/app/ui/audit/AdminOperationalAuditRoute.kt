package com.tbterminal.app.ui.audit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.components.TopFloatingCapsule

@Composable
fun AdminOperationalAuditRoute(
    name: String,
    role: String,
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
    viewModel: AdminOperationalAuditViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isFiltered = uiState.selectedAction != null || uiState.selectedPreset != "today"
    var showFilterPopup by rememberSaveable { mutableStateOf(false) }
    var showSummaryPopup by rememberSaveable { mutableStateOf(false) }

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.OperationalAudit,
        pageTitle = "Riwayat aktivitas",
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
                    modifier = Modifier.size(36.dp).testTag("audit-open-filters")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = "Filter riwayat aktivitas",
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
                modifier = Modifier.size(36.dp).testTag("audit-open-summary")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = "Ringkasan aktivitas",
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
        onLogout = onLogout
    ) { contentModifier ->
        Box(modifier = contentModifier.fillMaxSize()) {
            RefreshableContent(
                isRefreshing = uiState.isLoading && uiState.logs.isNotEmpty(),
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                AdminOperationalAuditScreen(
                    uiState = uiState,
                    showFilterPopup = showFilterPopup,
                    onDismissFilter = { showFilterPopup = false },
                    showSummaryPopup = showSummaryPopup,
                    onDismissSummary = { showSummaryPopup = false },
                    onActionFilterChanged = viewModel::setActionFilter,
                    onDateChanged = viewModel::setDate,
                    onDatePresetSelected = viewModel::setDatePreset,
                    onRetry = viewModel::refresh,
                    onPreviousPage = { viewModel.loadLogs(page = uiState.currentPage - 1) },
                    onNextPage = { viewModel.loadLogs(page = uiState.currentPage + 1) },
                )
            }
            if (uiState.logs.isNotEmpty()) {
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
