package com.tbterminal.app.ui.cashier.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.ManagerApprovalRepository
import com.tbterminal.app.ui.components.TBTopAppBar
import com.tbterminal.app.ui.components.TopFloatingCapsule
import androidx.compose.ui.zIndex
import com.tbterminal.app.ui.dashboard.cashier.CashierDashboardShell
import com.tbterminal.app.ui.dashboard.cashier.CashierDestination
import com.tbterminal.app.ui.salestransactions.CetakStrukPreviewDialog
import com.tbterminal.app.ui.salestransactions.ReceiptActions
import com.tbterminal.app.ui.salestransactions.ReceiptLoadingCard
import com.tbterminal.app.ui.salestransactions.ReceiptMessageCard
import com.tbterminal.app.ui.salestransactions.ReceiptSummaryCard
import com.tbterminal.app.ui.salestransactions.printTransactionReceiptPdf
import com.tbterminal.app.ui.salestransactions.shareReceiptText
import com.tbterminal.app.ui.salestransactions.WhatsAppReceiptDialog
import com.tbterminal.app.ui.salestransactions.WhatsAppReceiptHelper
import com.tbterminal.app.ui.salestransactions.DeliveryOrderDialog

private val SurfaceBackground = Color(0xFFF8F9FA)

@Composable
fun CashierReceiptDetailScreen(
    name: String,
    role: String,
    transactionId: String,
    cashReconciliationRepository: CashReconciliationRepository,
    managerApprovalRepository: ManagerApprovalRepository,
    onBackClick: () -> Unit,
    onDashboardClick: () -> Unit,
    onPosClick: () -> Unit,
    onCashSessionClick: () -> Unit = {},
    onTransactionHistoryClick: () -> Unit = {},
    onStockCheckClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: CashierTransactionHistoryViewModel = viewModel(
        factory = CashierTransactionHistoryViewModel.factory(
            repository = cashReconciliationRepository,
            actorRole = role,
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(transactionId) {
        viewModel.loadReceipt(transactionId)
    }

    CashierDashboardShell(
        userName = name,
        role = role,
        activeDestination = CashierDestination.TransactionHistory,
        showHeader = false,
        onDashboardClick = onDashboardClick,
        onPosClick = onPosClick,
        onCashSessionClick = onCashSessionClick,
        onTransactionHistoryClick = onTransactionHistoryClick,
        onStockCheckClick = onStockCheckClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    ) { contentModifier ->
        Box(modifier = contentModifier.fillMaxSize()) {
            ReceiptDetailContent(
                state = uiState,
                transactionId = transactionId,
                role = role,
                onBackClick = onBackClick,
                onShowPayDebt = viewModel::showPayDebtDialog,
                onHidePayDebt = viewModel::hidePayDebtDialog,
                onPayDebtAmountChanged = viewModel::onPayDebtAmountChanged,
                onPayDebtMethodChanged = viewModel::onPayDebtMethodChanged,
                onPayDebt = viewModel::payDebt,
                onShowVoid = viewModel::showVoidDialog,
                onShowRefund = viewModel::showRefundDialog,
                modifier = Modifier.fillMaxSize()
            )
            TopFloatingCapsule(
                message = uiState.receiptMessage,
                error = if (uiState.selectedTransaction != null) uiState.errorMessage else null,
                onDismiss = viewModel::clearFeedback,
                modifier = Modifier.align(Alignment.TopCenter).zIndex(99f)
            )
        }
    }
    TransactionActionDialogs(
        state = uiState,
        managerApprovalRepository = managerApprovalRepository,
        onDismissVoid = viewModel::hideVoidDialog,
        onVoidReasonChanged = viewModel::onVoidReasonChanged,
        onConfirmVoid = viewModel::submitVoid,
        onDismissRefund = viewModel::hideRefundDialog,
        onRefundReasonChanged = viewModel::onRefundReasonChanged,
        onRefundDispositionChanged = viewModel::onRefundDispositionChanged,
        onConfirmRefund = viewModel::submitRefund,
        onApprovalGranted = viewModel::onManagerApprovalGranted,
        onApprovalDismissed = viewModel::cancelManagerApproval,
    )
}

@Composable
private fun ReceiptDetailTopBar(
    onBackClick: () -> Unit
) {
    TBTopAppBar(
        title = "Detail Struk",
        onBackClick = onBackClick,
        windowInsets = WindowInsets(0, 0, 0, 0)
    )
}

@Composable
private fun ReceiptDetailContent(
    state: CashierTransactionHistoryUiState,
    transactionId: String,
    role: String,
    onBackClick: () -> Unit,
    onShowPayDebt: () -> Unit,
    onHidePayDebt: () -> Unit,
    onPayDebtAmountChanged: (String) -> Unit,
    onPayDebtMethodChanged: (String) -> Unit,
    onPayDebt: () -> Unit,
    onShowVoid: () -> Unit,
    onShowRefund: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPrintDialog by remember { mutableStateOf(false) }
    var showWhatsAppDialog by remember { mutableStateOf(false) }
    var showDeliveryOrderDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceBackground)
    ) {
        ReceiptDetailTopBar(onBackClick = onBackClick)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 440.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when {
                    state.isReceiptLoading -> ReceiptLoadingCard()
                    state.errorMessage != null -> ReceiptMessageCard(state.errorMessage)
                    state.selectedTransaction == null -> ReceiptMessageCard("Struk $transactionId belum dapat dimuat.")
                    else -> {
                        ReceiptSummaryCard(
                            transaction = state.selectedTransaction,
                            refundResult = state.refundResult
                        )
                        ReceiptActions(
                            transaction = state.selectedTransaction,
                            role = role,
                            onPrintClick = { showPrintDialog = true },
                            onPrintPdfClick = { printTransactionReceiptPdf(context, state.selectedTransaction) },
                            onShareClick = { shareReceiptText(context, state.selectedTransaction) },
                            onWhatsAppClick = { showWhatsAppDialog = true },
                            onDeliveryOrderClick = { showDeliveryOrderDialog = true },
                            onShowPayDebt = onShowPayDebt,
                            onShowVoid = onShowVoid,
                            onShowRefund = onShowRefund
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showPrintDialog && state.selectedTransaction != null) {
        CetakStrukPreviewDialog(
            transaction = state.selectedTransaction,
            onDismiss = { showPrintDialog = false }
        )
    }

    if (showWhatsAppDialog && state.selectedTransaction != null) {
        WhatsAppReceiptDialog(
            transaction = state.selectedTransaction,
            onDismissRequest = { showWhatsAppDialog = false },
            onSend = { phone ->
                showWhatsAppDialog = false
                WhatsAppReceiptHelper.sendViaWhatsApp(context, state.selectedTransaction, phone)
            }
        )
    }

    if (showDeliveryOrderDialog && state.selectedTransaction != null) {
        DeliveryOrderDialog(
            transaction = state.selectedTransaction,
            onDismiss = { showDeliveryOrderDialog = false }
        )
    }

    if (state.isPayDebtDialogOpen) {
        CashierPayDebtDialog(
            state = state,
            onDismiss = onHidePayDebt,
            onAmountChanged = onPayDebtAmountChanged,
            onMethodChanged = onPayDebtMethodChanged,
            onConfirm = onPayDebt
        )
    }
}
