package com.tbterminal.app.ui.customers

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.ui.dashboard.BackofficeAdaptiveShell
import com.tbterminal.app.ui.dashboard.BackofficeSection
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.math.BigDecimal

@Preview(name = "Pelanggan - Phone", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Pelanggan - Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun CustomerListPreview() {
    TbterminalappTheme {
        BackofficeAdaptiveShell(
            userName = "Pemilik Toko",
            role = "OWNER",
            activeSection = BackofficeSection.MENU,
            onSectionSelected = {},
            onProfileClick = {},
            onLogout = {},
            pageTitle = "Pelanggan",
            onBack = {},
        ) { contentModifier ->
            CustomerListContent(
                modifier = contentModifier,
                uiState = customerPreviewState(),
                canManage = true,
                onSearchChanged = {},
                onCategoryFilterChanged = {},
                onAddCustomerClick = {},
                onEditCustomerClick = {},
                onCustomerDetailClick = {},
                onDeactivateClick = {},
                onPreviousPage = {},
                onNextPage = {},
                onDismissMessage = {},
            )
        }
    }
}

private fun customerPreviewState() = CustomerListUiState(
    customers = listOf(
        previewCustomer("1", "Toko Berkah", "0812 3456 7890", "Jl. Merdeka No. 12", false, "2000000", 14),
        previewCustomer("2", "CV Sumber Makmur", "0813 9876 5432", "Pasar Baru Blok C2", true, "10000000", 30),
        previewCustomer("3", "Warung Ibu Sari", null, null, false, "0", 0),
    ),
    page = 1,
    totalPages = 3,
    totalCustomers = 24,
    isLoading = false,
)

private fun previewCustomer(
    id: String,
    name: String,
    phone: String?,
    address: String?,
    contractor: Boolean,
    creditLimit: String,
    terms: Int,
) = Customer(
    id = id,
    name = name,
    phone = phone,
    address = address,
    isContractor = contractor,
    creditLimit = BigDecimal(creditLimit),
    paymentTermDays = terms,
    isActive = true,
    createdAt = "2026-01-01T08:00:00",
    updatedAt = "2026-01-01T08:00:00",
)
