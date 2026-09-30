package com.tbterminal.app.ui.purchasehistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.repository.PurchasingRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PurchaseHistoryViewModel(
    private val purchasingRepository: PurchasingRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(PurchaseHistoryUiState())
    val uiState: StateFlow<PurchaseHistoryUiState> = _uiState.asStateFlow()

    init {
        loadSuppliers()
        loadPurchases()
    }

    fun loadPurchases(page: Int = _uiState.value.page) {
        viewModelScope.launch {
            val state = _uiState.value
            val safePage = page.coerceAtLeast(1)
            _uiState.update { it.copy(isLoading = true, page = safePage, errorMessage = null) }
            when (
                val result = purchasingRepository.getPurchases(
                    page = safePage,
                    limit = state.pageSize,
                    supplierId = state.selectedSupplierId,
                    startDate = state.startDate,
                    endDate = state.endDate
                )
            ) {
                is RepositoryResult.Success -> {
                    val pageData = result.data
                    _uiState.update {
                        it.copy(
                            purchases = pageData.data,
                            page = pageData.page,
                            totalPages = pageData.totalPages.coerceAtLeast(1),
                            totalPurchases = pageData.total,
                            isLoading = false
                        )
                    }
                }
                is RepositoryResult.Error -> setError(result.message)
                is RepositoryResult.Exception -> setError("Nota pembelian gagal dimuat karena koneksi bermasalah.")
            }
        }
    }

    fun refresh() = loadPurchases(_uiState.value.page)

    fun onFilterApplied(supplierId: String?, startDate: String?, endDate: String?, periodType: String) {
        _uiState.update { 
            it.copy(
                selectedSupplierId = supplierId, 
                startDate = startDate, 
                endDate = endDate,
                selectedPeriodType = periodType,
                page = 1, 
                errorMessage = null
            ) 
        }
        loadPurchases(page = 1)
    }

    fun onSearchChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun showDetail(purchaseId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDetail = true, errorMessage = null) }
            when (val result = purchasingRepository.getPurchaseById(purchaseId)) {
                is RepositoryResult.Success -> {
                    _uiState.update { it.copy(selectedPurchase = result.data, isLoadingDetail = false) }
                }
                is RepositoryResult.Error -> setError(result.message, detailLoading = false)
                is RepositoryResult.Exception -> setError("Detail nota gagal dimuat karena koneksi bermasalah.", detailLoading = false)
            }
        }
    }

    fun dismissDetail() = _uiState.update { it.copy(selectedPurchase = null) }

    fun previousPage() {
        val state = _uiState.value
        if (state.page > 1 && !state.isLoading) loadPurchases(state.page - 1)
    }

    fun nextPage() {
        val state = _uiState.value
        if (state.page < state.totalPages && !state.isLoading) loadPurchases(state.page + 1)
    }

    private fun loadSuppliers() {
        viewModelScope.launch {
            when (val result = purchasingRepository.getSuppliers(page = 1, limit = 100)) {
                is RepositoryResult.Success -> _uiState.update { it.copy(suppliers = result.data.data) }
                else -> Unit
            }
        }
    }

    private fun setError(message: String, detailLoading: Boolean = _uiState.value.isLoadingDetail) {
        _uiState.update { it.copy(isLoading = false, isLoadingDetail = detailLoading, errorMessage = message) }
    }

    companion object {
        fun factory(purchasingRepository: PurchasingRepository): ViewModelProvider.Factory {
            return viewModelFactory { PurchaseHistoryViewModel(purchasingRepository) }
        }
    }
}
