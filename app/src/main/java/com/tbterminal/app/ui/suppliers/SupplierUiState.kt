package com.tbterminal.app.ui.suppliers

import com.tbterminal.app.data.model.Supplier

data class SupplierUiState(
    val suppliers: List<Supplier> = emptyList(),
    val searchQuery: String = "",
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
)
