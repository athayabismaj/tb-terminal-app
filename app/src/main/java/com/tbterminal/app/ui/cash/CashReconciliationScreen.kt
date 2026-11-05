package com.tbterminal.app.ui.cash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tbterminal.app.ui.components.TopFloatingCapsule

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
    onPettyCashTypeChanged: (com.tbterminal.app.data.local.cashexpense.CashMovementType) -> Unit = {},
    onPettyCashCategoryChanged: (String) -> Unit = {},
    onAddExpense: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CashBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            CashHeader()
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

        TopFloatingCapsule(
            message = uiState.message,
            error = uiState.errorMessage,
            onDismiss = onDismissMessage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                .zIndex(99f),
        )
    }

    if (uiState.isPettyCashDialogOpen || uiState.isExpenseDialogOpen) {
        com.tbterminal.app.ui.cash.components.PettyCashDialog(
            selectedType = uiState.pettyCashType,
            amountInput = uiState.pettyCashAmountInput.ifBlank { uiState.expenseAmountInput },
            categoryInput = uiState.pettyCashCategoryInput,
            descriptionInput = uiState.pettyCashDescriptionInput.ifBlank { uiState.expenseDescriptionInput },
            isSubmitting = uiState.isSubmitting,
            errorMessage = uiState.errorMessage,
            onTypeChanged = onPettyCashTypeChanged,
            onAmountChanged = onExpenseAmountChanged,
            onCategoryChanged = onPettyCashCategoryChanged,
            onDescriptionChanged = onExpenseDescriptionChanged,
            onDismiss = onHideExpenseDialog,
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
    onPettyCashTypeChanged: (com.tbterminal.app.data.local.cashexpense.CashMovementType) -> Unit = {},
    onPettyCashCategoryChanged: (String) -> Unit = {},
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
