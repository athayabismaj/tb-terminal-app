package com.tbterminal.app.ui.cashier.sync

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.CashierSyncRepository
import com.tbterminal.app.data.sync.BackendHealthMonitor
import com.tbterminal.app.data.sync.OfflineSyncScheduler
import com.tbterminal.app.data.sync.SyncManager
import com.tbterminal.app.ui.dashboard.cashier.CashierDashboardShell
import com.tbterminal.app.ui.dashboard.cashier.CashierDestination

@Composable
fun CashierSyncRoute(
    name: String,
    role: String,
    cashierSyncRepository: CashierSyncRepository,
    offlineSyncScheduler: OfflineSyncScheduler,
    syncManager: SyncManager,
    backendHealthMonitor: BackendHealthMonitor,
    onBack: () -> Unit,
    onDashboardClick: () -> Unit,
    onPosClick: () -> Unit,
    onCashSessionClick: () -> Unit,
    onTransactionHistoryClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogout: () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val syncRunner = remember(context, offlineSyncScheduler, syncManager, backendHealthMonitor) {
        WorkManagerCashierSyncRunner(
            appContext = context,
            offlineSyncScheduler = offlineSyncScheduler,
            syncManager = syncManager,
            backendHealthMonitor = backendHealthMonitor,
        )
    }
    val viewModel: CashierSyncViewModel = viewModel(
        factory = CashierSyncViewModel.factory(
            repository = cashierSyncRepository,
            runner = syncRunner,
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CashierDashboardShell(
        userName = name,
        role = role,
        activeDestination = CashierDestination.SyncData,
        pageTitle = "Sinkron Data",
        showBottomBar = false,
        onBackClick = onBack,
        onDashboardClick = onDashboardClick,
        onPosClick = onPosClick,
        onCashSessionClick = onCashSessionClick,
        onTransactionHistoryClick = onTransactionHistoryClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onSyncDataClick = {},
        onLogout = onLogout,
    ) { contentModifier ->
        CashierSyncScreen(
            uiState = uiState,
            onSyncNowClick = viewModel::onSyncNowClick,
            onRefreshServerStatus = viewModel::refreshServerStatus,
            onDismissFeedback = viewModel::clearFeedback,
            modifier = contentModifier,
        )
    }
}
