package com.tbterminal.app.ui.customers

import com.tbterminal.app.data.model.Customer
import java.math.BigDecimal

internal const val CUSTOMER_PAGE_SIZE = 10

enum class CustomerCategoryFilter(val label: String) {
    ALL("Semua kategori"),
    GENERAL("Pelanggan umum"),
    CONTRACTOR("Kontraktor")
}

data class CustomerListUiState(
    val customers: List<Customer> = emptyList(),
    val searchQuery: String = "",
    val categoryFilter: CustomerCategoryFilter = CustomerCategoryFilter.ALL,
    val page: Int = 1,
    val totalPages: Int = 1,
    val totalCustomers: Long = 0,
    val pageSize: Int = CUSTOMER_PAGE_SIZE,
    val isLoading: Boolean = true,
    val isMutating: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null
) {
    val visibleCustomers: List<Customer>
        get() = when (categoryFilter) {
            CustomerCategoryFilter.ALL -> customers
            CustomerCategoryFilter.GENERAL -> customers.filterNot(Customer::isContractor)
            CustomerCategoryFilter.CONTRACTOR -> customers.filter(Customer::isContractor)
        }

    val contractorCount: Int
        get() = customers.count(Customer::isContractor)

    val totalCreditLimit: BigDecimal
        get() = customers.fold(BigDecimal.ZERO) { total, customer -> total.add(customer.creditLimit) }

    val currentStart: Long
        get() = if (totalCustomers == 0L) 0 else ((page - 1L) * pageSize) + 1

    val currentEnd: Long
        get() = minOf(page.toLong() * pageSize, totalCustomers)
}

data class CustomerFormInput(
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val isContractor: Boolean = false,
    val creditLimit: String = "0",
    val paymentTermDays: String = "0"
)

data class CustomerFormUiState(
    val input: CustomerFormInput = CustomerFormInput(),
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val fieldErrors: Map<String, String> = emptyMap(),
    val errorMessage: String? = null
)

data class CustomerDetailUiState(
    val customer: Customer? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
