package com.tbterminal.app.ui.suppliers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.data.repository.PurchasingRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.ui.common.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal object CreatedSupplierBridge {
    private var pendingCreatedSupplier: Supplier? = null

    fun setPendingCreated(supplier: Supplier) {
        pendingCreatedSupplier = supplier
    }

    fun consumePendingCreated(): Supplier? {
        val value = pendingCreatedSupplier
        pendingCreatedSupplier = null
        return value
    }
}

class SupplierViewModel(
    private val purchasingRepository: PurchasingRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SupplierUiState())
    val uiState: StateFlow<SupplierUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null
    private var loadJob: Job? = null

    init {
        loadSuppliers()
    }

    fun loadSuppliers(page: Int = _uiState.value.page) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val state = _uiState.value
            val safePage = page.coerceAtLeast(1)
            _uiState.update { it.copy(isLoading = true, page = safePage, message = null) }
            when (
                val result = purchasingRepository.getSuppliers(
                    page = safePage,
                    limit = state.pageSize,
                    search = state.searchQuery.trim().takeIf(String::isNotBlank),
                    status = state.statusFilter.apiValue
                )
            ) {
                is RepositoryResult.Success -> {
                    val pageData = result.data
                    _uiState.update {
                        it.copy(
                            suppliers = pageData.data,
                            page = pageData.page,
                            totalPages = pageData.totalPages.coerceAtLeast(1),
                            totalSuppliers = pageData.total,
                            isLoading = false
                        )
                    }
                }
                is RepositoryResult.Error -> setMessage(result.message, loading = false)
                is RepositoryResult.Exception -> setMessage("Supplier gagal dimuat karena koneksi bermasalah.", loading = false)
            }
        }
    }

    fun refresh() = loadSuppliers(page = 1)

    fun onSearchChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, page = 1, message = null) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            loadSuppliers(page = 1)
        }
    }

    fun onStatusFilterChanged(filter: SupplierStatusFilter) {
        if (_uiState.value.statusFilter == filter) return
        searchJob?.cancel()
        _uiState.update { it.copy(statusFilter = filter, page = 1, message = null) }
        loadSuppliers(page = 1)
    }

    fun onNameChanged(value: String) = updateInput { it.copy(nameInput = value) }
    fun onPhoneChanged(value: String) = updateInput { it.copy(phoneInput = value) }
    fun onAddressChanged(value: String) = updateInput { it.copy(addressInput = value) }
    fun onPaymentTermChanged(value: String) = updateInput { it.copy(paymentTermInput = value.filter(Char::isDigit)) }

    fun edit(supplier: Supplier) {
        _uiState.update {
            it.copy(
                editingSupplier = supplier,
                nameInput = supplier.name,
                phoneInput = supplier.phone.orEmpty(),
                addressInput = supplier.address.orEmpty(),
                paymentTermInput = supplier.paymentTermDays.toString(),
                message = null
            )
        }
    }

    fun cancelEdit() = _uiState.update { it.clearedForm(message = null) }

    fun resetSavedState() = _uiState.update { it.copy(isSaved = false) }

    fun save() {
        val state = _uiState.value
        val name = state.nameInput.trim()
        val paymentTerm = state.paymentTermInput.toIntOrNull()
        when {
            name.isBlank() -> setMessage("Nama supplier tidak boleh kosong.")
            paymentTerm == null || paymentTerm < 0 -> setMessage("Termin pembayaran harus berupa angka nol atau lebih.")
            else -> submitSupplier(state, name, paymentTerm)
        }
    }

    fun delete(supplier: Supplier) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }
            when (val result = purchasingRepository.deleteSupplier(supplier.id)) {
                is RepositoryResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, message = "Supplier berhasil dinonaktifkan.") }
                    loadSuppliers(page = 1)
                }
                is RepositoryResult.Error -> setMessage(result.message)
                is RepositoryResult.Exception -> setMessage("Supplier gagal dinonaktifkan karena koneksi bermasalah.")
            }
        }
    }

    fun previousPage() {
        val state = _uiState.value
        if (state.page > 1 && !state.isLoading) loadSuppliers(state.page - 1)
    }

    fun nextPage() {
        val state = _uiState.value
        if (state.page < state.totalPages && !state.isLoading) loadSuppliers(state.page + 1)
    }

    fun clearMessage() = _uiState.update { it.copy(message = null) }

    private fun submitSupplier(state: SupplierUiState, name: String, paymentTerm: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }
            val phone = state.phoneInput.trim().takeIf(String::isNotBlank)
            val address = state.addressInput.trim().takeIf(String::isNotBlank)
            val isCreating = state.editingSupplier == null
            val result = state.editingSupplier?.let {
                purchasingRepository.updateSupplier(it.id, name, phone, address, paymentTerm)
            } ?: purchasingRepository.createSupplier(name, phone, address, paymentTerm)
            if (isCreating && result is RepositoryResult.Success) {
                CreatedSupplierBridge.setPendingCreated(result.data)
            }
            handleSavedResult(result)
        }
    }

    private fun handleSavedResult(result: RepositoryResult<Supplier>) {
        when (result) {
            is RepositoryResult.Success -> {
                _uiState.update { it.clearedForm(message = "Supplier berhasil disimpan.").copy(isSaved = true) }
                loadSuppliers()
            }
            is RepositoryResult.Error -> setMessage(result.message)
            is RepositoryResult.Exception -> setMessage("Supplier gagal disimpan karena koneksi bermasalah.")
        }
    }

    private fun updateInput(update: (SupplierUiState) -> SupplierUiState) {
        _uiState.update { update(it).copy(message = null) }
    }

    private fun setMessage(message: String, loading: Boolean = _uiState.value.isLoading) {
        _uiState.update { it.copy(isSaving = false, isLoading = loading, message = message) }
    }

    private fun SupplierUiState.clearedForm(message: String?): SupplierUiState {
        return copy(
            editingSupplier = null,
            nameInput = "",
            phoneInput = "",
            addressInput = "",
            paymentTermInput = "30",
            isSaving = false,
            isSaved = false,
            message = message
        )
    }

    companion object {
        fun factory(purchasingRepository: PurchasingRepository): ViewModelProvider.Factory {
            return viewModelFactory { SupplierViewModel(purchasingRepository) }
        }
    }
}

class SupplierDetailViewModel(
    private val purchasingRepository: PurchasingRepository,
    private val supplierId: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(SupplierDetailUiState())
    val uiState: StateFlow<SupplierDetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
    }

    fun loadDetail() {
        if (supplierId.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "ID supplier tidak valid."
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = purchasingRepository.getSupplierById(supplierId, includeInactive = true)) {
                is RepositoryResult.Success -> {
                    _uiState.update { it.copy(supplier = result.data, isLoading = false) }
                }
                is RepositoryResult.Error -> setLoadError(result.message)
                is RepositoryResult.Exception -> setLoadError("Koneksi bermasalah. Detail supplier gagal dimuat.")
            }
        }
    }

    fun deactivate() = performDeactivate(stayOnDetail = false)

    fun undoActivate() = performDeactivate(stayOnDetail = true)

    fun activate() {
        if (_uiState.value.isMutating || supplierId.isBlank()) return
        _uiState.update {
            it.copy(
                isMutating = true,
                isReactivated = false,
                actionErrorMessage = null,
                isNotFoundError = false
            )
        }
        viewModelScope.launch {
            when (val result = purchasingRepository.activateSupplier(supplierId)) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            supplier = result.data,
                            isMutating = false,
                            isReactivated = true,
                            actionErrorMessage = null,
                            isNotFoundError = false
                        )
                    }
                }
                is RepositoryResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isMutating = false,
                            actionErrorMessage = result.message,
                            isNotFoundError = result.code == "HTTP_404"
                        )
                    }
                }
                is RepositoryResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isMutating = false,
                            actionErrorMessage = "Supplier gagal diaktifkan karena koneksi bermasalah.",
                            isNotFoundError = false
                        )
                    }
                }
            }
        }
    }

    private fun performDeactivate(stayOnDetail: Boolean) {
        if (_uiState.value.isMutating || supplierId.isBlank()) return
        _uiState.update {
            it.copy(
                isMutating = true,
                isReactivated = false,
                actionErrorMessage = null,
                isNotFoundError = false
            )
        }
        viewModelScope.launch {
            when (val result = purchasingRepository.deleteSupplier(supplierId)) {
                is RepositoryResult.Success -> {
                    _uiState.update { current ->
                        current.copy(
                            supplier = current.supplier?.copy(isActive = false),
                            isMutating = false,
                            isDeactivated = !stayOnDetail,
                            actionErrorMessage = null,
                            isNotFoundError = false
                        )
                    }
                }
                is RepositoryResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isMutating = false,
                            actionErrorMessage = result.message,
                            isNotFoundError = result.code == "HTTP_404"
                        )
                    }
                }
                is RepositoryResult.Exception -> {
                    val msg = if (stayOnDetail) {
                        "Supplier gagal dinonaktifkan kembali karena koneksi bermasalah."
                    } else {
                        "Supplier gagal dinonaktifkan karena koneksi bermasalah."
                    }
                    _uiState.update {
                        it.copy(
                            isMutating = false,
                            actionErrorMessage = msg,
                            isNotFoundError = false
                        )
                    }
                }
            }
        }
    }

    fun clearActionError() {
        _uiState.update { it.copy(actionErrorMessage = null, isNotFoundError = false) }
    }

    fun resetDeactivatedState() {
        _uiState.update { it.copy(isDeactivated = false) }
    }

    fun resetReactivatedState() {
        _uiState.update { it.copy(isReactivated = false) }
    }

    private fun setLoadError(message: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    companion object {
        fun factory(
            purchasingRepository: PurchasingRepository,
            supplierId: String
        ): ViewModelProvider.Factory {
            return viewModelFactory { SupplierDetailViewModel(purchasingRepository, supplierId) }
        }
    }
}
