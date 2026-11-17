package com.tbterminal.app.ui.cashier.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.ui.cash.CashReconciliationUiState
import com.tbterminal.app.ui.cashier.session.components.ShiftPaymentBreakdownBottomSheet
import com.tbterminal.app.ui.cashier.session.components.ShiftRecentTransactionsSection
import com.tbterminal.app.ui.cashier.session.components.ShiftReprintChooserDialog
import com.tbterminal.app.ui.cashier.session.components.ShiftSingleLineSummaryCard
import com.tbterminal.app.ui.cashier.session.components.ShiftUtilityRow
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.math.BigDecimal

private val sampleActiveSession = CashSession(
    id = "SESS-20261117-001",
    userId = "USR-001",
    userName = "Ahmad Kasir",
    openedAt = "2026-11-17T08:00:00Z",
    closedAt = null,
    openingCash = BigDecimal("200000"),
    closingCash = null,
    systemCash = BigDecimal("1650000"),
    difference = null,
    totalExpenses = BigDecimal("50000"),
    totalCashIn = BigDecimal("100000"),
    notes = null,
    status = "OPEN"
)

private val sampleTransactions = listOf(
    CashTransaction(
        id = "TX-01",
        receiptId = "TRX-20261117-001",
        sessionId = "SESS-20261117-001",
        customerId = "CUST-01",
        customerName = "Budi Santoso",
        paymentMethods = listOf("TUNAI"),
        type = "SALE",
        status = "COMPLETED",
        total = BigDecimal("158000"),
        paidAmount = BigDecimal("158000"),
        createdAt = "2026-11-17T08:30:00Z"
    ),
    CashTransaction(
        id = "TX-02",
        receiptId = "TRX-20261117-002",
        sessionId = "SESS-20261117-001",
        customerId = null,
        customerName = "Pelanggan Umum",
        paymentMethods = listOf("QRIS"),
        type = "SALE",
        status = "COMPLETED",
        total = BigDecimal("75000"),
        paidAmount = BigDecimal("75000"),
        createdAt = "2026-11-17T09:15:00Z"
    ),
    CashTransaction(
        id = "TX-03",
        receiptId = "TRX-20261117-003",
        sessionId = "SESS-20261117-001",
        customerId = "CUST-02",
        customerName = "PT Maju Konstruksi",
        paymentMethods = listOf("TRANSFER"),
        type = "SALE",
        status = "COMPLETED",
        total = BigDecimal("850000"),
        paidAmount = BigDecimal("850000"),
        createdAt = "2026-11-17T10:00:00Z"
    ),
    CashTransaction(
        id = "TX-04",
        receiptId = "TRX-20261117-004",
        sessionId = "SESS-20261117-001",
        customerId = "CUST-03",
        customerName = "Pak Haji Slamet",
        paymentMethods = listOf("HUTANG"),
        type = "SALE",
        status = "COMPLETED",
        total = BigDecimal("320000"),
        paidAmount = BigDecimal("0"),
        createdAt = "2026-11-17T10:45:00Z"
    )
)

private val sampleActiveUiState = CashReconciliationUiState(
    activeSession = sampleActiveSession,
    transactions = sampleTransactions,
    total = 4,
    totalCashIn = BigDecimal("100000"),
    totalCashOut = BigDecimal("50000"),
    isLoading = false
)

@Preview(name = "Fase 4 - Tab Kas Shift Belum Dibuka", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun PreviewCashierSessionShiftNotOpened() {
    TbterminalappTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF8FAF9)
        ) {
            CashierCashSessionScreen(
                uiState = CashReconciliationUiState(activeSession = null, isLoading = false),
                onOpeningCashChanged = {},
                onOpenSession = {},
                onClosingCashChanged = {},
                onClosingNotesChanged = {},
                onCloseSession = {},
                onShowExpenseDialog = {},
                onTransactionHistoryClick = {}
            )
        }
    }
}

@Preview(name = "Fase 4 - Tab Kas Shift Aktif (Single-Line & Utility Row)", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun PreviewCashierSessionActive() {
    TbterminalappTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF8FAF9)
        ) {
            CashierCashSessionScreen(
                uiState = sampleActiveUiState,
                onOpeningCashChanged = {},
                onOpenSession = {},
                onClosingCashChanged = {},
                onClosingNotesChanged = {},
                onCloseSession = {},
                onShowExpenseDialog = {},
                onTransactionHistoryClick = {}
            )
        }
    }
}

@Preview(name = "Fase 4 - Shift Single-Line Summary Card", showBackground = true, widthDp = 390)
@Composable
fun PreviewShiftSingleLineSummaryCard() {
    TbterminalappTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8FAF9))
                .padding(16.dp)
        ) {
            ShiftSingleLineSummaryCard(
                omzet = BigDecimal("1403000"),
                transactionCount = 4,
                expectedCash = BigDecimal("1650000"),
                isPrivacyHidden = false,
                onTogglePrivacy = {},
                onClickDetail = {}
            )
        }
    }
}

@Preview(name = "Fase 4 - Shift Utility Row (48dp Touch Targets)", showBackground = true, widthDp = 390)
@Composable
fun PreviewShiftUtilityRow() {
    TbterminalappTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8FAF9))
                .padding(16.dp)
        ) {
            ShiftUtilityRow(
                onKasMasukKeluarClick = {},
                onCetakUlangClick = {},
                onTutupShiftClick = {}
            )
        }
    }
}

@Preview(name = "Fase 4 - Shift Recent Transactions", showBackground = true, widthDp = 390)
@Composable
fun PreviewShiftRecentTransactions() {
    TbterminalappTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8FAF9))
                .padding(16.dp)
        ) {
            ShiftRecentTransactionsSection(
                transactions = sampleTransactions,
                totalTransactions = 4,
                onTransactionHistoryClick = {},
                onReprintTransaction = {}
            )
        }
    }
}

@Preview(name = "Fase 4 - Shift Reprint Chooser Dialog", showBackground = true, widthDp = 390)
@Composable
fun PreviewShiftReprintDialog() {
    TbterminalappTheme {
        ShiftReprintChooserDialog(
            hasActiveShift = true,
            latestTransaction = sampleTransactions.first(),
            onPrintShiftSlip = {},
            onOpenTransactionHistory = {},
            onDismiss = {}
        )
    }
}
