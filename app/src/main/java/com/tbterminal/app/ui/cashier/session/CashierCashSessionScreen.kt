package com.tbterminal.app.ui.cashier.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.cash.CashReconciliationUiState
import com.tbterminal.app.ui.cashier.session.components.CashSessionHeader
import com.tbterminal.app.ui.cashier.session.components.CloseShiftSection
import com.tbterminal.app.ui.cashier.session.components.OpenShiftForm
import com.tbterminal.app.ui.cashier.session.components.SessionSummarySection
import com.tbterminal.app.ui.dashboard.DashboardBackground

@Composable
fun CashierCashSessionScreen(
    modifier: Modifier = Modifier,
    uiState: CashReconciliationUiState,
    onOpeningCashChanged: (String) -> Unit,
    onOpenSession: () -> Unit,
    onClosingCashChanged: (String) -> Unit,
    onClosingNotesChanged: (String) -> Unit,
    onCloseSession: () -> Unit,
    onShowExpenseDialog: () -> Unit,
    onTransactionHistoryClick: () -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(DashboardBackground)
    ) {
        val isDesktop = maxWidth > 900.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            CashSessionHeader()

            if (uiState.isLoading && !uiState.isSubmitting) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (!uiState.hasActiveSession) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OpenShiftForm(
                        uiState = uiState,
                        onOpeningCashChanged = onOpeningCashChanged,
                        onOpenSession = onOpenSession
                    )
                }
            } else {
                if (isDesktop) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SessionSummarySection(
                            modifier = Modifier.weight(7f),
                            uiState = uiState,
                            onShowExpenseDialog = onShowExpenseDialog,
                            onTransactionHistoryClick = onTransactionHistoryClick
                        )
                        CloseShiftSection(
                            modifier = Modifier.weight(5f),
                            cashAmount = uiState.closingCashInput,
                            notes = uiState.closingNotesInput,
                            onCashChange = onClosingCashChanged,
                            onNotesChange = onClosingNotesChanged,
                            onSubmit = onCloseSession,
                            isSubmitting = uiState.isSubmitting
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SessionSummarySection(
                            modifier = Modifier.fillMaxWidth(),
                            uiState = uiState,
                            onShowExpenseDialog = onShowExpenseDialog,
                            onTransactionHistoryClick = onTransactionHistoryClick
                        )
                        CloseShiftSection(
                            modifier = Modifier.fillMaxWidth(),
                            cashAmount = uiState.closingCashInput,
                            notes = uiState.closingNotesInput,
                            onCashChange = onClosingCashChanged,
                            onNotesChange = onClosingNotesChanged,
                            onSubmit = onCloseSession,
                            isSubmitting = uiState.isSubmitting
                        )
                    }
                }
            }
        }
    }
}
