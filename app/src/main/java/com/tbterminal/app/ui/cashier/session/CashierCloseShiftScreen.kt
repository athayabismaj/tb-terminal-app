package com.tbterminal.app.ui.cashier.session

import androidx.compose.runtime.Composable
import com.tbterminal.app.data.local.cashexpense.LocalCashExpenseService
import com.tbterminal.app.data.local.cashsession.LocalCashSessionService
import com.tbterminal.app.data.local.database.CashSessionLocalDataSource
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.sync.OfflineStatusRepository

@Composable
fun CashierCloseShiftScreen(
    name: String,
    role: String,
    cashReconciliationRepository: CashReconciliationRepository,
    cashSessionLocalDataSource: CashSessionLocalDataSource? = null,
    localCashSessionService: LocalCashSessionService? = null,
    localCashExpenseService: LocalCashExpenseService? = null,
    offlineStatusRepository: OfflineStatusRepository? = null,
    cashierUserId: String? = null,
    cashierNameFallback: String? = null,
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
        cashSessionLocalDataSource = cashSessionLocalDataSource,
        localCashSessionService = localCashSessionService,
        localCashExpenseService = localCashExpenseService,
        offlineStatusRepository = offlineStatusRepository,
        cashierUserId = cashierUserId,
        cashierNameFallback = cashierNameFallback,
        onDashboardClick = onDashboardClick,
        onPosClick = onPosClick,
        onTransactionHistoryClick = onTransactionHistoryClick,
        onStockCheckClick = onStockCheckClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    )
}
