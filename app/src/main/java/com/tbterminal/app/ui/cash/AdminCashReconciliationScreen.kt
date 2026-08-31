package com.tbterminal.app.ui.cash

import androidx.compose.runtime.Composable
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.ui.cashhistory.AdminCashSessionHistoryScreen
import com.tbterminal.app.ui.dashboard.admin.AdminDestination

@Composable
fun AdminCashReconciliationScreen(
    name: String,
    role: String,
    cashReconciliationRepository: CashReconciliationRepository,
    onDashboardClick: () -> Unit,
    onProductsClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onProductCategoriesClick: () -> Unit,
    onProductUnitsClick: () -> Unit,
    onCashReconciliationClick: () -> Unit = {},
    onSalesTransactionsClick: () -> Unit = {},
    onReportsClick: () -> Unit = {},
    onPriceManagementClick: () -> Unit = {},
    onStockOpnameClick: () -> Unit,
    onStockOpnameFormClick: () -> Unit,
    onIncomingGoodsClick: () -> Unit,
    onIncomingGoodsFormClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onOperationalAuditClick: () -> Unit = {},
    onCashSessionDetailClick: (String) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onBack: () -> Unit = {},
    onLogout: () -> Unit
) {
    AdminCashSessionHistoryScreen(
        name = name,
        role = role,
        cashRepository = cashReconciliationRepository,
        onDashboardClick = onDashboardClick,
        onProductsClick = onProductsClick,
        onStockOpnameClick = onStockOpnameClick,
        onIncomingGoodsClick = onIncomingGoodsClick,
        onSupplierDebtsClick = onSupplierDebtsClick,
        onReceivablesClick = onReceivablesClick,
        onCustomersClick = onCustomersClick,
        onReportsClick = onReportsClick,
        onOperationalAuditClick = onOperationalAuditClick,
        onShowDetail = onCashSessionDetailClick,
        onBack = onBack,
        onLogout = onLogout,
        activeDestination = AdminDestination.CashReconciliation,
        title = "Kas Harian"
    )
}
