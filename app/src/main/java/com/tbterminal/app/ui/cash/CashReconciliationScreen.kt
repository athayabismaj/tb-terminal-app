package com.tbterminal.app.ui.cash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun CashReconciliationScreen(
    modifier: Modifier,
    uiState: CashReconciliationUiState,
    onDismissMessage: () -> Unit,
    onOpeningCashChanged: (String) -> Unit,
    onOpenSession: () -> Unit,
    onClosingCashChanged: (String) -> Unit,
    onClosingNotesChanged: (String) -> Unit,
    onCloseSession: () -> Unit,
    onRefresh: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onShowExpenseDialog: () -> Unit,
    onHideExpenseDialog: () -> Unit,
    onExpenseAmountChanged: (String) -> Unit,
    onExpenseDescriptionChanged: (String) -> Unit,
    onAddExpense: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CashBackground)
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        CashHeader()
        CashMessage(uiState, onDismissMessage)
        CashMetrics(uiState)
        CashSessionPanel(
            uiState = uiState,
            onOpeningCashChanged = onOpeningCashChanged,
            onOpenSession = onOpenSession,
            onClosingCashChanged = onClosingCashChanged,
            onClosingNotesChanged = onClosingNotesChanged,
            onCloseSession = onCloseSession,
            onShowExpenseDialog = onShowExpenseDialog
        )
        CashTransactionsTable(
            uiState = uiState,
            onRefresh = onRefresh,
            onPreviousPage = onPreviousPage,
            onNextPage = onNextPage,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (uiState.isExpenseDialogOpen) {
        CashExpenseDialog(
            uiState = uiState,
            onDismiss = onHideExpenseDialog,
            onAmountChanged = onExpenseAmountChanged,
            onDescriptionChanged = onExpenseDescriptionChanged,
            onConfirm = onAddExpense
        )
    }
}

@Composable
internal fun CashReconciliationContent(
    modifier: Modifier,
    uiState: CashReconciliationUiState,
    onDismissMessage: () -> Unit,
    onOpeningCashChanged: (String) -> Unit,
    onOpenSession: () -> Unit,
    onClosingCashChanged: (String) -> Unit,
    onClosingNotesChanged: (String) -> Unit,
    onCloseSession: () -> Unit,
    onRefresh: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onShowExpenseDialog: () -> Unit,
    onHideExpenseDialog: () -> Unit,
    onExpenseAmountChanged: (String) -> Unit,
    onExpenseDescriptionChanged: (String) -> Unit,
    onAddExpense: () -> Unit
) {
    CashReconciliationScreen(
        modifier = modifier,
        uiState = uiState,
        onDismissMessage = onDismissMessage,
        onOpeningCashChanged = onOpeningCashChanged,
        onOpenSession = onOpenSession,
        onClosingCashChanged = onClosingCashChanged,
        onClosingNotesChanged = onClosingNotesChanged,
        onCloseSession = onCloseSession,
        onRefresh = onRefresh,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage,
        onShowExpenseDialog = onShowExpenseDialog,
        onHideExpenseDialog = onHideExpenseDialog,
        onExpenseAmountChanged = onExpenseAmountChanged,
        onExpenseDescriptionChanged = onExpenseDescriptionChanged,
        onAddExpense = onAddExpense
    )
}
