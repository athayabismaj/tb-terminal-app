package com.tbterminal.app.ui.receivables

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tbterminal.app.data.model.CustomerReceivableSummary
import com.tbterminal.app.data.model.Receivable
import com.tbterminal.app.ui.dashboard.BackofficeAdaptiveShell
import com.tbterminal.app.ui.dashboard.BackofficeSection
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.math.BigDecimal

@Preview(name = "Piutang - Phone", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
fun PhoneReceivablePreview() {
    TbterminalappTheme {
        BackofficeAdaptiveShell(
            userName = "Pemilik Toko",
            role = "owner",
            activeSection = BackofficeSection.FINANCE,
            onSectionSelected = {},
            onProfileClick = {},
            onLogout = {},
            pageTitle = "Piutang Pelanggan",
            onBack = {}
        ) { contentModifier ->
            ReceivableScreen(
            modifier = contentModifier,
            uiState = ReceivableUiState(
                receivables = previewReceivables,
                customerSummaries = listOf(
                    CustomerReceivableSummary(
                        customerId = "customer-1",
                        customerName = "Toko Sumber Rejeki",
                        totalAmount = BigDecimal("1850000"),
                        totalPaid = BigDecimal("500000"),
                        totalRemaining = BigDecimal("1350000"),
                        unpaidCount = 2,
                        overdueCount = 1,
                        nearestDueDate = "2026-08-28"
                    )
                ),
                total = 2,
                totalPages = 1
            ),
            onSearchChanged = {},
            onStatusFilterChanged = {},
            onDueFilterChanged = {},
            canAdjust = true,
            onAddOpeningBalance = {},
            onAddAdjustment = {},
            onPayClick = {},
            onPreviousPage = {},
            onNextPage = {},
            onDismissMessage = {}
            )
        }
    }
}

private val previewReceivables = listOf(
    Receivable(
        id = "receivable-1",
        customerId = "customer-1",
        customerName = "Toko Sumber Rejeki",
        transactionId = "TRX-20260825-001",
        source = "SALE",
        amount = BigDecimal("1250000"),
        paidAmount = BigDecimal("500000"),
        remainingAmount = BigDecimal("750000"),
        debtDate = "2026-08-18",
        dueDate = "2026-08-28",
        status = "PARTIAL",
        legacyInvoiceNumber = "INV-1842",
        notes = null,
        createdBy = "owner-1",
        isActive = true,
        createdAt = "2026-08-18T10:30:00"
    ),
    Receivable(
        id = "receivable-2",
        customerId = "customer-2",
        customerName = "Warung Makmur",
        transactionId = "TRX-20260820-006",
        source = "OPENING_BALANCE",
        amount = BigDecimal("600000"),
        paidAmount = BigDecimal.ZERO,
        remainingAmount = BigDecimal("600000"),
        debtDate = "2026-08-20",
        dueDate = "2026-09-05",
        status = "UNPAID",
        legacyInvoiceNumber = "NOTA-099",
        notes = null,
        createdBy = "owner-1",
        isActive = true,
        createdAt = "2026-08-20T09:15:00"
    )
)
