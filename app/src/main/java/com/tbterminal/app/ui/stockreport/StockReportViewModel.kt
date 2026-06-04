package com.tbterminal.app.ui.stockreport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.ui.common.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StockReportViewModel(
    private val inventoryRepository: InventoryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(StockReportUiState())
    val uiState: StateFlow<StockReportUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    init {
        loadStocks()
    }

    fun loadStocks(page: Int = _uiState.value.page) {
        viewModelScope.launch {
            val state = _uiState.value
            val safePage = page.coerceAtLeast(1)
            _uiState.update { it.copy(isLoading = true, page = safePage, errorMessage = null) }
            when (
                val result = inventoryRepository.getProductStocks(
                    page = safePage,
                    limit = state.pageSize,
                    search = state.searchQuery.trim().takeIf(String::isNotBlank)
                )
            ) {
                is RepositoryResult.Success -> {
                    val pageData = result.data
                    _uiState.update {
                        it.copy(
                            stocks = pageData.data,
                            categoryFilter = it.categoryFilter
                                ?.takeIf { category -> pageData.data.any { stock -> stock.categoryName == category } },
                            page = pageData.page,
                            totalPages = pageData.totalPages.coerceAtLeast(1),
                            totalProducts = pageData.total,
                            isLoading = false
                        )
                    }
                }
                is RepositoryResult.Error -> setError(result.message)
                is RepositoryResult.Exception -> setError("Laporan stok gagal dimuat karena koneksi bermasalah.")
            }
        }
    }

    fun onSearchChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query, page = 1, errorMessage = null) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            loadStocks(page = 1)
        }
    }

    fun onCategoryFilterChanged(categoryName: String?) {
        _uiState.update { it.copy(categoryFilter = categoryName) }
    }

    fun previousPage() {
        val state = _uiState.value
        if (state.page > 1 && !state.isLoading) loadStocks(state.page - 1)
    }

    fun nextPage() {
        val state = _uiState.value
        if (state.page < state.totalPages && !state.isLoading) loadStocks(state.page + 1)
    }

    private fun setError(message: String) {
        _uiState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    companion object {
        fun factory(inventoryRepository: InventoryRepository): ViewModelProvider.Factory {
            return viewModelFactory { StockReportViewModel(inventoryRepository) }
        }
    }
}
