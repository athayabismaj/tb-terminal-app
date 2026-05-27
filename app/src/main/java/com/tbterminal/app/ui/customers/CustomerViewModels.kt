package com.tbterminal.app.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.model.CustomerCommand
import com.tbterminal.app.data.repository.CustomerRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.ui.common.viewModelFactory
import java.math.BigDecimal
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CustomerListViewModel(
    private val customerRepository: CustomerRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CustomerListUiState())
    val uiState: StateFlow<CustomerListUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    init {
        loadCustomers()
    }

    fun loadCustomers(page: Int = _uiState.value.page) {
        viewModelScope.launch {
            val search = _uiState.value.searchQuery.trim().takeIf(String::isNotBlank)
            _uiState.update { it.copy(isLoading = true, errorMessage = null, page = page) }

            when (
                val result = customerRepository.getCustomers(
                    page = page,
                    limit = CUSTOMER_PAGE_SIZE,
                    search = search
                )
            ) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            customers = result.data.data,
                            page = result.data.page,
                            totalPages = result.data.totalPages.coerceAtLeast(1),
                            totalCustomers = result.data.total,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                is RepositoryResult.Error -> setLoadError(result.message)
                is RepositoryResult.Exception -> setLoadError("Koneksi bermasalah. Pelanggan gagal dimuat.")
            }
        }
    }

    fun onSearchChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            loadCustomers(page = 1)
        }
    }

    fun previousPage() {
        val previous = (_uiState.value.page - 1).coerceAtLeast(1)
        if (previous != _uiState.value.page) loadCustomers(previous)
    }

    fun nextPage() {
        val next = (_uiState.value.page + 1).coerceAtMost(_uiState.value.totalPages)
        if (next != _uiState.value.page) loadCustomers(next)
    }

    fun deactivate(customer: Customer) {
        if (_uiState.value.isMutating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true, message = null, errorMessage = null) }
            when (val result = customerRepository.deactivateCustomer(customer.id)) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isMutating = false,
                            message = "Pelanggan ${customer.name} berhasil dinonaktifkan."
                        )
                    }
                    loadCustomers()
                }
                is RepositoryResult.Error -> setActionError(result.message)
                is RepositoryResult.Exception -> setActionError("Pelanggan gagal dinonaktifkan karena koneksi bermasalah.")
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, errorMessage = null) }
    }

    private fun setLoadError(message: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    private fun setActionError(message: String) {
        _uiState.update { it.copy(isMutating = false, errorMessage = message) }
    }

    companion object {
        fun factory(customerRepository: CustomerRepository): ViewModelProvider.Factory {
            return viewModelFactory { CustomerListViewModel(customerRepository) }
        }
    }
}

class CustomerFormViewModel(
    private val customerRepository: CustomerRepository,
    private val customerId: String?
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        CustomerFormUiState(isEditMode = customerId != null, isLoading = customerId != null)
    )
    val uiState: StateFlow<CustomerFormUiState> = _uiState.asStateFlow()

    init {
        if (customerId != null) loadCustomer(customerId)
    }

    fun onInputChanged(input: CustomerFormInput) {
        _uiState.update { it.copy(input = input, errorMessage = null) }
    }

    fun save() {
        val state = _uiState.value
        val command = state.input.toCommandOrError { message ->
            _uiState.update { it.copy(errorMessage = message) }
        } ?: return

        if (state.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val result = if (customerId == null) {
                customerRepository.createCustomer(command)
            } else {
                customerRepository.updateCustomer(customerId, command)
            }

            when (result) {
                is RepositoryResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                }
                is RepositoryResult.Error -> setSaveError(result.message)
                is RepositoryResult.Exception -> setSaveError("Koneksi bermasalah. Pelanggan gagal disimpan.")
            }
        }
    }

    fun resetSavedState() {
        _uiState.update { it.copy(isSaved = false) }
    }

    private fun loadCustomer(id: String) {
        viewModelScope.launch {
            when (val result = customerRepository.getCustomerById(id)) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            input = result.data.toFormInput(),
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                is RepositoryResult.Error -> setLoadError(result.message)
                is RepositoryResult.Exception -> setLoadError("Koneksi bermasalah. Detail pelanggan gagal dimuat.")
            }
        }
    }

    private fun setLoadError(message: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    private fun setSaveError(message: String) {
        _uiState.update { it.copy(isSaving = false, errorMessage = message) }
    }

    companion object {
        fun factory(
            customerRepository: CustomerRepository,
            customerId: String?
        ): ViewModelProvider.Factory {
            return viewModelFactory { CustomerFormViewModel(customerRepository, customerId) }
        }
    }
}

class CustomerDetailViewModel(
    private val customerRepository: CustomerRepository,
    private val customerId: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(CustomerDetailUiState())
    val uiState: StateFlow<CustomerDetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
    }

    fun loadDetail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = customerRepository.getCustomerById(customerId)) {
                is RepositoryResult.Success -> {
                    _uiState.update { it.copy(customer = result.data, isLoading = false) }
                }
                is RepositoryResult.Error -> setLoadError(result.message)
                is RepositoryResult.Exception -> setLoadError("Koneksi bermasalah. Detail pelanggan gagal dimuat.")
            }
        }
    }

    private fun setLoadError(message: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    companion object {
        fun factory(
            customerRepository: CustomerRepository,
            customerId: String
        ): ViewModelProvider.Factory {
            return viewModelFactory { CustomerDetailViewModel(customerRepository, customerId) }
        }
    }
}

private fun Customer.toFormInput(): CustomerFormInput {
    return CustomerFormInput(
        name = name,
        phone = phone.orEmpty(),
        address = address.orEmpty(),
        isContractor = isContractor,
        creditLimit = creditLimit.stripTrailingZeros().toPlainString(),
        paymentTermDays = paymentTermDays.toString()
    )
}

private fun CustomerFormInput.toCommandOrError(onError: (String) -> Unit): CustomerCommand? {
    val cleanName = name.trim()
    if (cleanName.isBlank()) {
        onError("Nama pelanggan wajib diisi.")
        return null
    }

    val limit = creditLimit.numericInput().toBigDecimalOrNull()
    if (limit == null || limit < BigDecimal.ZERO) {
        onError("Limit kredit wajib berupa angka nol atau lebih.")
        return null
    }

    val term = paymentTermDays.filter(Char::isDigit).toIntOrNull()
    if (term == null || term < 0) {
        onError("Termin bayar wajib berupa angka nol atau lebih.")
        return null
    }

    return CustomerCommand(
        name = cleanName,
        phone = phone.trim().takeIf(String::isNotBlank),
        address = address.trim().takeIf(String::isNotBlank),
        isContractor = isContractor,
        creditLimit = limit,
        paymentTermDays = term
    )
}

internal fun String.numericInput(): String {
    return filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')
}
