package com.tbterminal.app.ui.payables

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tbterminal.app.data.model.SupplierPayable
import com.tbterminal.app.ui.dashboard.BackofficeAdaptiveShell
import com.tbterminal.app.ui.dashboard.BackofficeSection
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.math.BigDecimal

@Preview(name = "Hutang Supplier - Phone", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
fun PhoneSupplierDebtPreview() {
    TbterminalappTheme {
        BackofficeAdaptiveShell(
            userName = "Pemilik Toko",
            role = "owner",
            activeSection = BackofficeSection.FINANCE,
            onSectionSelected = {},
            onProfileClick = {},
            onLogout = {},
            pageTitle = "Hutang Supplier",
            onBack = {}
        ) { contentModifier ->
            SupplierDebtScreen(
            modifier = contentModifier,
            uiState = SupplierDebtUiState(
                payables = listOf(
                    SupplierPayable(
                        id = "payable-1",
                        supplierId = "supplier-1",
                        supplierName = "CV Sumber Pangan",
                        purchaseId = "PUR-20260825-001",
                        amount = BigDecimal("3200000"),
                        paidAmount = BigDecimal("1200000"),
                        remainingAmount = BigDecimal("2000000"),
                        dueDate = "2026-08-30",
                        status = "sebagian",
                        createdAt = "2026-08-20T08:30:00"
                    ),
                    SupplierPayable(
                        id = "payable-2",
                        supplierId = "supplier-2",
                        supplierName = "UD Makmur Bersama",
                        purchaseId = "PUR-20260822-009",
                        amount = BigDecimal("1450000"),
                        paidAmount = BigDecimal.ZERO,
                        remainingAmount = BigDecimal("1450000"),
                        dueDate = "2026-09-04",
                        status = "belum_lunas",
                        createdAt = "2026-08-22T11:15:00"
                    )
                ),
                total = 2,
                totalPages = 1
            ),
            onSearchChanged = {},
            onStatusFilterChanged = {},
            onRefresh = {},
            onPayClick = {},
            onPreviousPage = {},
            onNextPage = {},
            onDismissMessage = {}
            )
        }
    }
}

@Preview(name = "Hutang Supplier - Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
fun TabletSupplierDebtPreview() {
    TbterminalappTheme {
        BackofficeAdaptiveShell(
            userName = "Pemilik Toko",
            role = "owner",
            activeSection = BackofficeSection.FINANCE,
            onSectionSelected = {},
            onProfileClick = {},
            onLogout = {},
            pageTitle = "Hutang Supplier",
            onBack = {},
        ) { contentModifier ->
            SupplierDebtScreen(
                modifier = contentModifier,
                uiState = previewSupplierDebtState(),
                onSearchChanged = {},
                onStatusFilterChanged = {},
                onRefresh = {},
                onPayClick = {},
                onPreviousPage = {},
                onNextPage = {},
                onDismissMessage = {},
            )
        }
    }
}

private fun previewSupplierDebtState() = SupplierDebtUiState(
    payables = listOf(
        SupplierPayable(
            id = "payable-1",
            supplierId = "supplier-1",
            supplierName = "CV Sumber Pangan",
            purchaseId = "PUR-20260825-001",
            amount = BigDecimal("3200000"),
            paidAmount = BigDecimal("1200000"),
            remainingAmount = BigDecimal("2000000"),
            dueDate = "2026-08-30",
            status = "sebagian",
            createdAt = "2026-08-20T08:30:00",
        ),
        SupplierPayable(
            id = "payable-2",
            supplierId = "supplier-2",
            supplierName = "UD Makmur Bersama",
            purchaseId = "PUR-20260822-009",
            amount = BigDecimal("1450000"),
            paidAmount = BigDecimal.ZERO,
            remainingAmount = BigDecimal("1450000"),
            dueDate = "2026-09-20",
            status = "belum_lunas",
            createdAt = "2026-08-22T11:15:00",
        ),
    ),
    total = 2,
    totalPages = 1,
)
