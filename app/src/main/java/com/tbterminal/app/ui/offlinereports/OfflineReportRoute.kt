package com.tbterminal.app.ui.offlinereports

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tbterminal.app.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.OfflineReportRepository
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination

@Composable
fun OfflineReportRoute(
    name: String,
    role: String,
    offlineReportRepository: OfflineReportRepository,
    onDashboardClick: () -> Unit,
    onProductsClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onProductCategoriesClick: () -> Unit,
    onProductUnitsClick: () -> Unit,
    onCashReconciliationClick: () -> Unit = {},
    onSalesTransactionsClick: () -> Unit = {},
    onReportsClick: () -> Unit = {},
    onLocalReportsClick: () -> Unit = {},
    onSyncCenterClick: () -> Unit = {},
    onPriceManagementClick: () -> Unit = {},
    onStockOpnameClick: () -> Unit,
    onStockOpnameFormClick: () -> Unit,
    onIncomingGoodsClick: () -> Unit,
    onIncomingGoodsFormClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onOperationalAuditClick: () -> Unit = {},
    onUserManagementClick: () -> Unit = {},
    onSecurityLogClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onBackToPrevious: (() -> Unit)? = null,
    onLogout: () -> Unit,
    viewModel: OfflineReportViewModel = viewModel(
        factory = OfflineReportViewModel.factory(offlineReportRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isFiltered = uiState.selectedPreset != OfflineReportPreset.Today
    var showFilterPopup by rememberSaveable { mutableStateOf(false) }
    var showSummaryPopup by rememberSaveable { mutableStateOf(false) }

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.LocalReports,
        pageTitle = stringResource(R.string.owner_menu_device_reports),
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
                    modifier = Modifier.size(36.dp).testTag("offline-report-open-filter")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = "Filter laporan lokal",
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
                modifier = Modifier.size(36.dp).testTag("offline-report-open-summary")
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
        onLocalReportsClick = onLocalReportsClick,
        onSyncCenterClick = onSyncCenterClick,
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
        OfflineReportScreen(
            modifier = contentModifier,
            uiState = uiState,
            showFilterPopup = showFilterPopup,
            onDismissFilter = { showFilterPopup = false },
            showSummaryPopup = showSummaryPopup,
            onDismissSummary = { showSummaryPopup = false },
            onPresetSelected = viewModel::selectPreset,
            onCustomStartChanged = viewModel::updateCustomStart,
            onCustomEndChanged = viewModel::updateCustomEnd,
            onApplyCustomRange = viewModel::applyCustomRange,
            onRefresh = viewModel::refresh
        )
    }
}
