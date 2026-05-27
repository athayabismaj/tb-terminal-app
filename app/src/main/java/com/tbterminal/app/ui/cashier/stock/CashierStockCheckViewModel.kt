package com.tbterminal.app.ui.cashier.stock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.data.repository.RepositoryResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CashierStockCheckUiState(
    val query: String = "",
    val products: List<ProductStock> = emptyList(),
    val page: Int = 1,
    val limit: Int = STOCK_PAGE_SIZE,
    val total: Long = 0,
    val totalPages: Int = 1,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val currentStart: Long
        get() = if (total == 0L) 0 else ((page - 1) * limit + 1).toLong()

    val currentEnd: Long
        get() = minOf(page.toLong() * limit.toLong(), total)
}

private const val STOCK_PAGE_SIZE = 10

class CashierStockCheckViewModel(
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CashierStockCheckUiState())
    val uiState: StateFlow<CashierStockCheckUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadProducts()
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(query = newQuery, page = 1) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            loadProducts(page = 1, searchQuery = newQuery)
        }
    }

    fun previousPage() {
        val previous = (_uiState.value.page - 1).coerceAtLeast(1)
        if (previous != _uiState.value.page) loadProducts(page = previous)
    }

    fun nextPage() {
        val state = _uiState.value
        val next = (state.page + 1).coerceAtMost(state.totalPages.coerceAtLeast(1))
        if (next != state.page) loadProducts(page = next)
    }

    fun refresh() {
        loadProducts(page = _uiState.value.page)
    }

    private fun loadProducts(
        page: Int = _uiState.value.page,
        searchQuery: String = _uiState.value.query
    ) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (
                val result = inventoryRepository.getProductStocks(
                    page = page,
                    limit = STOCK_PAGE_SIZE,
                    search = searchQuery
                )
            ) {
                is RepositoryResult.Success -> {
                    val productPage = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            products = productPage.data,
                            page = productPage.page,
                            limit = productPage.limit,
                            total = productPage.total,
                            totalPages = productPage.totalPages.coerceAtLeast(1)
                        )
                    }
                }
                is RepositoryResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
                is RepositoryResult.Exception -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Terjadi kesalahan jaringan."
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun factory(
            inventoryRepository: InventoryRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CashierStockCheckViewModel(inventoryRepository) as T
            }
        }
    }
}
