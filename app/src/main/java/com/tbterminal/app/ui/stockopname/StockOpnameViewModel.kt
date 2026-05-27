package com.tbterminal.app.ui.stockopname

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.repository.InventoryRepository
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

class StockOpnameViewModel(
    private val inventoryRepository: InventoryRepository,
    private val initialProductId: String? = null,
    private val autoSelectFirst: Boolean = true
) : ViewModel() {
    private val _uiState = MutableStateFlow(StockOpnameUiState(isLoading = true))
    val uiState: StateFlow<StockOpnameUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null
    private var initialSelectionApplied = false

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            val query = _uiState.value.searchQuery.trim().takeIf(String::isNotBlank)
            _uiState.update { state -> state.copy(isLoading = true, errorMessage = null) }

            when (
                val result = inventoryRepository.getProductStocks(
                    page = 1,
                    limit = STOCK_OPNAME_PAGE_SIZE,
                    search = query
                )
            ) {
                is RepositoryResult.Success -> {
                    applyProducts(result.data.data.filter(ProductStock::isActive))
                    loadLatestAdjustments(query)
                }
                is RepositoryResult.Error -> setLoadError(result.message)
                is RepositoryResult.Exception -> setLoadError("Koneksi ke server bermasalah. Data stok gagal dimuat.")
            }
        }
    }

    fun onSearchChanged(query: String) {
        _uiState.update { state -> state.copy(searchQuery = query, currentPage = 1) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            loadProducts()
        }
    }

    fun onCategoryFilterChanged(categoryName: String?) {
        _uiState.update { state -> state.copy(categoryFilter = categoryName, currentPage = 1) }
    }

    fun nextPage() {
        _uiState.update { state ->
            state.copy(currentPage = (state.tablePage + 1).coerceAtMost(state.totalTablePages))
        }
    }

    fun previousPage() {
        _uiState.update { state ->
            state.copy(currentPage = (state.tablePage - 1).coerceAtLeast(1))
        }
    }

    fun selectProduct(product: ProductStock) {
        _uiState.update { state ->
            state.copy(
                selectedProduct = product,
                actualQtyInput = "",
                notesInput = "",
                errorMessage = null,
                message = null
            )
        }
    }

    fun onActualQtyChanged(value: String) {
        _uiState.update { state -> state.copy(actualQtyInput = value.quantityInput()) }
    }

    fun onNotesChanged(value: String) {
        _uiState.update { state -> state.copy(notesInput = value) }
    }

    fun onAdjustmentTypeChanged(type: StockAdjustmentType) {
        _uiState.update { state -> state.copy(adjustmentType = type) }
    }

    fun submitOpname() {
        val state = _uiState.value
        val product = state.selectedProduct ?: return setActionError("Pilih produk terlebih dahulu.")
        val actualQty = state.actualQty ?: return setActionError("Stok fisik wajib diisi dengan angka valid.")
        if (actualQty < BigDecimal.ZERO) return setActionError("Stok fisik tidak boleh negatif.")
        if (state.isSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, message = null, errorMessage = null) }
            val result = inventoryRepository.executeStockOpname(
                productId = product.productId,
                adjustmentType = state.adjustmentType.apiValue,
                actualQty = actualQty,
                notes = state.notesInput
            )

            when (result) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            actualQtyInput = "",
                            notesInput = "",
                            message = "Stok ${product.productName} berhasil disesuaikan."
                        )
                    }
                    loadProducts()
                }
                is RepositoryResult.Error -> setActionError(result.message)
                is RepositoryResult.Exception -> setActionError("Stok opname gagal disimpan karena koneksi bermasalah.")
            }
        }
    }

    fun clearMessage() {
        _uiState.update { state -> state.copy(message = null, errorMessage = null) }
    }

    private fun applyProducts(products: List<ProductStock>) {
        _uiState.update { state ->
            val selected = resolveSelectedProduct(state, products)
            val categoryFilter = state.categoryFilter
                ?.takeIf { filter -> products.any { it.categoryName == filter } }

            state.copy(
                products = products,
                selectedProduct = selected,
                categoryFilter = categoryFilter,
                isLoading = false,
                errorMessage = null
            )
        }
        initialSelectionApplied = true
    }

    private suspend fun loadLatestAdjustments(query: String?) {
        when (
            val result = inventoryRepository.getStockAdjustments(
                page = 1,
                limit = STOCK_OPNAME_ADJUSTMENT_LOOKUP_LIMIT,
                search = query
            )
        ) {
            is RepositoryResult.Success -> {
                val latestByProduct = result.data.data
                    .distinctBy { adjustment -> adjustment.productId }
                    .associateBy { adjustment -> adjustment.productId }
                _uiState.update { state -> state.copy(latestAdjustmentsByProductId = latestByProduct) }
            }
            is RepositoryResult.Error,
            is RepositoryResult.Exception -> {
                _uiState.update { state -> state.copy(latestAdjustmentsByProductId = emptyMap()) }
            }
        }
    }

    private fun resolveSelectedProduct(
        state: StockOpnameUiState,
        products: List<ProductStock>
    ): ProductStock? {
        val current = state.selectedProduct
            ?.let { selected -> products.firstOrNull { it.productId == selected.productId } }
        if (current != null) return current

        val initial = if (!initialSelectionApplied) {
            initialProductId?.let { id -> products.firstOrNull { it.productId == id } }
        } else {
            null
        }

        return initial ?: products.firstOrNull().takeIf { autoSelectFirst }
    }

    private fun setLoadError(message: String) {
        _uiState.update { state -> state.copy(isLoading = false, errorMessage = message) }
    }

    private fun setActionError(message: String) {
        _uiState.update { state -> state.copy(isSubmitting = false, errorMessage = message) }
    }

    companion object {
        fun factory(
            inventoryRepository: InventoryRepository,
            initialProductId: String? = null,
            autoSelectFirst: Boolean = true
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                StockOpnameViewModel(
                    inventoryRepository = inventoryRepository,
                    initialProductId = initialProductId,
                    autoSelectFirst = autoSelectFirst
                )
            }
        }
    }
}
