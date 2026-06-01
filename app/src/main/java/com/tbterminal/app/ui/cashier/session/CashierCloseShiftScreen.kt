package com.tbterminal.app.ui.cashier.session

import androidx.compose.runtime.Composable
import com.tbterminal.app.data.repository.CashReconciliationRepository

@Composable
fun CashierCloseShiftScreen(
    name: String,
    role: String,
    cashReconciliationRepository: CashReconciliationRepository,
    onDashboardClick: () -> Unit,
    onPosClick: () -> Unit,
    onTransactionHistoryClick: () -> Unit = {},
    onStockCheckClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit
) {
    CashierCashSessionRoute(
        name = name,
        role = role,
        cashReconciliationRepository = cashReconciliationRepository,
        onDashboardClick = onDashboardClick,
        onPosClick = onPosClick,
        onTransactionHistoryClick = onTransactionHistoryClick,
        onStockCheckClick = onStockCheckClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    )
}
