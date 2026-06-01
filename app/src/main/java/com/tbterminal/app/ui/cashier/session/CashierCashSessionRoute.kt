package com.tbterminal.app.ui.cashier.session

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.ui.cash.CashExpenseDialog
import com.tbterminal.app.ui.cash.CashReconciliationViewModel
import com.tbterminal.app.ui.cashier.session.components.CashSessionErrorOverlay
import com.tbterminal.app.ui.cashier.session.components.SuccessModalOverlay
import com.tbterminal.app.ui.dashboard.cashier.CashierDashboardShell
import com.tbterminal.app.ui.dashboard.cashier.CashierDestination

@Composable
fun CashierCashSessionRoute(
    name: String,
    role: String,
    cashReconciliationRepository: CashReconciliationRepository,
    onDashboardClick: () -> Unit,
    onPosClick: () -> Unit,
    onTransactionHistoryClick: () -> Unit = {},
    onStockCheckClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: CashReconciliationViewModel = viewModel(
        factory = CashReconciliationViewModel.factory(cashReconciliationRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSuccessModal by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.message) {
        if (uiState.message != null && uiState.errorMessage == null && !uiState.hasActiveSession) {
            showSuccessModal = true
            viewModel.clearMessage()
        }
    }

    CashierDashboardShell(
        userName = name,
        role = role,
        activeDestination = CashierDestination.CashSession,
        onDashboardClick = onDashboardClick,
        onPosClick = onPosClick,
        onCashSessionClick = {},
        onTransactionHistoryClick = onTransactionHistoryClick,
        onStockCheckClick = onStockCheckClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    ) { contentModifier ->
        CashierCashSessionScreen(
            modifier = contentModifier,
            uiState = uiState,
            onOpeningCashChanged = viewModel::onOpeningCashChanged,
            onOpenSession = viewModel::openSession,
            onClosingCashChanged = viewModel::onClosingCashChanged,
            onClosingNotesChanged = viewModel::onClosingNotesChanged,
            onCloseSession = viewModel::closeSession,
            onShowExpenseDialog = viewModel::showExpenseDialog,
            onTransactionHistoryClick = onTransactionHistoryClick
        )
    }

    if (uiState.errorMessage != null) {
        CashSessionErrorOverlay(
            message = uiState.errorMessage.orEmpty(),
            onDismiss = viewModel::clearMessage
        )
    }

    if (showSuccessModal) {
        SuccessModalOverlay(
            onDismiss = { showSuccessModal = false },
            onPrint = { /* Logika Cetak Ulang Struk Ktor/Printer Bluetooth */ }
        )
    }

    if (uiState.isExpenseDialogOpen) {
        CashExpenseDialog(
            uiState = uiState,
            onDismiss = viewModel::hideExpenseDialog,
            onAmountChanged = viewModel::onExpenseAmountChanged,
            onDescriptionChanged = viewModel::onExpenseDescriptionChanged,
            onConfirm = viewModel::addExpense
        )
    }
}
