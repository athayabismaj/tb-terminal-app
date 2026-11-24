package com.tbterminal.app.ui.suppliers

import com.tbterminal.app.data.model.Supplier

enum class SupplierStatusFilter(
    val label: String,
    val chipLabel: String,
    val apiValue: String
) {
    ALL(label = "Semua status", chipLabel = "Semua", apiValue = "all"),
    ACTIVE(label = "Aktif", chipLabel = "Aktif", apiValue = "active"),
    INACTIVE(label = "Nonaktif", chipLabel = "Nonaktif", apiValue = "inactive"),
}

internal enum class SupplierSortOrder(val label: String) {
    NAME_ASC("Nama A\u2013Z"),
    NAME_DESC("Nama Z\u2013A"),
    TERM_DESC("Termin terlama"),
}

data class SupplierUiState(
    val suppliers: List<Supplier> = emptyList(),
    val searchQuery: String = "",
    val statusFilter: SupplierStatusFilter = SupplierStatusFilter.ALL,
    val page: Int = 1,
    val totalPages: Int = 1,
    val totalSuppliers: Long = 0,
    val pageSize: Int = 10,
    val nameInput: String = "",
    val phoneInput: String = "",
    val addressInput: String = "",
    val paymentTermInput: String = "30",
    val editingSupplier: Supplier? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val message: String? = null
) {
    val visibleSuppliers: List<Supplier>
        get() = when (statusFilter) {
            SupplierStatusFilter.ALL -> suppliers
            SupplierStatusFilter.ACTIVE -> suppliers.filter { it.isActive }
            SupplierStatusFilter.INACTIVE -> suppliers.filter { !it.isActive }
        }
}

data class SupplierDetailUiState(
    val supplier: Supplier? = null,
    val isLoading: Boolean = true,
    val isMutating: Boolean = false,
    val errorMessage: String? = null,
    val actionErrorMessage: String? = null,
    val isDeactivated: Boolean = false,
    val isReactivated: Boolean = false,
    val isNotFoundError: Boolean = false
)
