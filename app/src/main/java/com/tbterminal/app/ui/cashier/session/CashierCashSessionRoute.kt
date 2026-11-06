package com.tbterminal.app.ui.cashier.session

import android.content.Intent
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.local.cashexpense.LocalCashExpenseService
import com.tbterminal.app.data.local.cashsession.LocalCashSessionService
import com.tbterminal.app.data.local.database.CashSessionLocalDataSource
import com.tbterminal.app.data.printer.CashSessionSummaryFormatter
import com.tbterminal.app.data.printer.PrinterResult
import com.tbterminal.app.data.printer.ThermalPrinterManager
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.sync.OfflineStatusRepository
import com.tbterminal.app.ui.cash.CashExpenseDialog
import com.tbterminal.app.ui.cash.CashReconciliationViewModel
import com.tbterminal.app.ui.cashier.session.components.SuccessModalOverlay
import com.tbterminal.app.ui.dashboard.cashier.CashierDashboardShell
import com.tbterminal.app.ui.dashboard.cashier.CashierDestination
import kotlinx.coroutines.launch

@Composable
fun CashierCashSessionRoute(
    name: String,
    role: String,
    cashReconciliationRepository: CashReconciliationRepository,
    cashSessionLocalDataSource: CashSessionLocalDataSource? = null,
    localCashSessionService: LocalCashSessionService? = null,
    localCashExpenseService: LocalCashExpenseService? = null,
    offlineStatusRepository: OfflineStatusRepository? = null,
    thermalPrinterManager: ThermalPrinterManager? = null,
    cashierUserId: String? = null,
    cashierNameFallback: String? = null,
    onDashboardClick: () -> Unit,
    onPosClick: () -> Unit,
    onTransactionHistoryClick: () -> Unit = {},
    onStockCheckClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: CashReconciliationViewModel = viewModel(
        factory = CashReconciliationViewModel.factory(
            repository = cashReconciliationRepository,
            cashSessionLocalDataSource = cashSessionLocalDataSource,
            localCashSessionService = localCashSessionService,
            localCashExpenseService = localCashExpenseService,
            offlineStatusRepository = offlineStatusRepository,
            cashierUserId = cashierUserId,
            cashierNameFallback = cashierNameFallback
        )
    )
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSuccessModal by remember { mutableStateOf(false) }
    var isPrinting by remember { mutableStateOf(false) }

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
            onTransactionHistoryClick = onTransactionHistoryClick,
            onClearMessage = viewModel::clearMessage
        )
    }

    if (showSuccessModal) {
        val targetSession = uiState.lastClosedSession ?: uiState.activeSession
        SuccessModalOverlay(
            session = targetSession,
            isPrinting = isPrinting,
            onDismiss = {
                showSuccessModal = false
                onDashboardClick()
            },
            onPrint = {
                if (targetSession != null && thermalPrinterManager != null) {
                    scope.launch {
                        isPrinting = true
                        try {
                            val paperSize = thermalPrinterManager.getPaperSize()
                            val bytes = CashSessionSummaryFormatter.buildShiftSettlementBytes(
                                session = targetSession,
                                paperSize = paperSize
                            )
                            when (val result = thermalPrinterManager.printCustomBytes(context, bytes)) {
                                is PrinterResult.Success -> {
                                    Toast.makeText(context, "Slip rekap shift berhasil dicetak", Toast.LENGTH_SHORT).show()
                                }
                                is PrinterResult.Error -> {
                                    Toast.makeText(context, "Gagal mencetak: ${result.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error cetak: ${e.message}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isPrinting = false
                        }
                    }
                } else if (thermalPrinterManager == null) {
                    Toast.makeText(context, "Printer belum dikonfigurasi.", Toast.LENGTH_SHORT).show()
                }
            },
            onShareWhatsApp = {
                if (targetSession != null) {
                    val messageText = CashSessionSummaryFormatter.buildShiftSettlementWhatsAppText(targetSession)
                    val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, messageText)
                        setPackage("com.whatsapp")
                    }
                    try {
                        context.startActivity(whatsappIntent)
                    } catch (_: Throwable) {
                        val chooser = Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, messageText)
                            },
                            "Bagikan Slip Tutup Shift"
                        )
                        context.startActivity(chooser)
                    }
                }
            }
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
            onTypeChanged = viewModel::onPettyCashTypeChanged,
            onAmountChanged = viewModel::onPettyCashAmountChanged,
            onCategoryChanged = viewModel::onPettyCashCategoryChanged,
            onDescriptionChanged = viewModel::onPettyCashDescriptionChanged,
            onDismiss = viewModel::hidePettyCashDialog,
            onConfirm = viewModel::addPettyCash
        )
    }
}
