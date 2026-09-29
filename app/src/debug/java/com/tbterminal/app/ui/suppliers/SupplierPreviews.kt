package com.tbterminal.app.ui.suppliers

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.ui.dashboard.BackofficeAdaptiveShell
import com.tbterminal.app.ui.dashboard.BackofficeSection
import com.tbterminal.app.ui.theme.TbterminalappTheme

@Preview(name = "Supplier - Phone", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Supplier - Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun SupplierPreview() {
    TbterminalappTheme {
        BackofficeAdaptiveShell(
            userName = "Pemilik Toko",
            role = "OWNER",
            activeSection = BackofficeSection.MENU,
            onSectionSelected = {},
            onProfileClick = {},
            onLogout = {},
            pageTitle = "Supplier",
            onBack = {},
        ) { contentModifier ->
            SupplierScreen(
                modifier = contentModifier,
                uiState = supplierPreviewState(),
                onSearchChanged = {},
                onAdd = {},
                onEdit = {},
                onDelete = {},
                onPreviousPage = {},
                onNextPage = {},
                onDismissMessage = {},
            )
        }
    }
}

@Preview(name = "Tambah Supplier - Phone", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Tambah Supplier - Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun SupplierFormPreview() {
    TbterminalappTheme {
        BackofficeAdaptiveShell(
            userName = "Pemilik Toko",
            role = "OWNER",
            activeSection = BackofficeSection.MENU,
            onSectionSelected = {},
            onProfileClick = {},
            onLogout = {},
            pageTitle = "Tambah Supplier",
            onBack = {},
        ) { contentModifier ->
            SupplierFormScreen(
                modifier = contentModifier,
                uiState = supplierPreviewState(),
                onNameChanged = {}, onPhoneChanged = {}, onAddressChanged = {},
                onPaymentTermChanged = {}, onSave = {}, onCancel = {}, onDismissMessage = {},
            )
        }
    }
}

private fun supplierPreviewState() = SupplierUiState(
    suppliers = listOf(
        previewSupplier("supplier-1", "PT Sumber Bangunan", "0812 3456 7890", "Jl. Industri No. 18", 30),
        previewSupplier("supplier-2", "CV Makmur Jaya", "0813 9988 7766", "Pasar Baru Blok A2", 14),
    ),
    page = 1,
    totalPages = 3,
    totalSuppliers = 22,
    isLoading = false,
)

private fun previewSupplier(id: String, name: String, phone: String, address: String, term: Int) = Supplier(
    id = id,
    name = name,
    phone = phone,
    address = address,
    paymentTermDays = term,
    isActive = true,
    createdAt = "2026-01-01T08:00:00",
    updatedAt = "2026-01-01T08:00:00",
)
