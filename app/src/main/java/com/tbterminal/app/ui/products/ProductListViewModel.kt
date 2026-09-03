package com.tbterminal.app.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.CreateProductCommand
import com.tbterminal.app.data.model.Product
import com.tbterminal.app.data.model.ProductCategory
import com.tbterminal.app.data.model.ProductDetail
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.model.ProductUnit
import com.tbterminal.app.data.model.UpdateProductCommand
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

class ProductListViewModel(
    private val inventoryRepository: InventoryRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductListUiState(isLoading = true))
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    init {
        loadCategories()
        loadProducts()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            when (val result = inventoryRepository.getCategories()) {
                is RepositoryResult.Success -> {
                    _uiState.update { state -> state.copy(categories = result.data) }
                }
                is RepositoryResult.Error,
                is RepositoryResult.Exception -> Unit
            }
        }
    }

    fun loadProducts(page: Int = _uiState.value.page) {
        viewModelScope.launch {
            val query = _uiState.value.searchQuery.trim().takeIf(String::isNotBlank)
            _uiState.update { state ->
                state.copy(isLoading = true, errorMessage = null, page = page)
            }

            when (
                val result = inventoryRepository.getProductStocks(
                    page = page,
                    limit = PRODUCT_PAGE_SIZE,
                    search = query
                )
            ) {
                is RepositoryResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            products = result.data.data,
                            totalProducts = result.data.total,
                            page = result.data.page,
                            totalPages = result.data.totalPages.coerceAtLeast(1),
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                is RepositoryResult.Error -> setLoadError(result.message)
                is RepositoryResult.Exception -> setLoadError("Koneksi ke server bermasalah. Produk gagal dimuat.")
            }
        }
    }

    fun onSearchChanged(query: String) {
        _uiState.update { state -> state.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(350)
            loadProducts(page = 1)
        }
    }

    fun nextPage() {
        val state = _uiState.value
        if (state.page < state.totalPages) loadProducts(state.page + 1)
    }

    fun previousPage() {
        val state = _uiState.value
        if (state.page > 1) loadProducts(state.page - 1)
    }

    fun toggleProductStatus(product: ProductStock) {
        if (_uiState.value.isMutating) return

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isMutating = true, actionMessage = null)
            }

            val result = if (product.isActive) {
                inventoryRepository.deleteProduct(product.productId)
            } else {
                inventoryRepository.activateProduct(product.productId)
            }

            when (result) {
                is RepositoryResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isMutating = false,
                            actionMessage = product.statusActionSuccessMessage()
                        )
                    }
                    loadProducts()
                }
                is RepositoryResult.Error -> setActionError(result.message)
                is RepositoryResult.Exception -> setActionError(product.statusActionConnectionErrorMessage())
            }
        }
    }

    fun refresh() = loadProducts(_uiState.value.page)

    fun onCategorySelected(category: String) {
        _uiState.update { state ->
            state.copy(selectedCategory = category)
        }
    }

    fun clearActionMessage() {
        _uiState.update { state -> state.copy(actionMessage = null) }
    }

    fun previewCsv(csv: String) {
        if (_uiState.value.isImporting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, showImportDialog = true, importCsv = csv, importPreview = null, actionMessage = null) }
            when (val result = inventoryRepository.previewProductCsv(csv)) {
                is RepositoryResult.Success -> _uiState.update { it.copy(isImporting = false, importPreview = result.data) }
                is RepositoryResult.Error -> _uiState.update { it.copy(isImporting = false, showImportDialog = false, actionMessage = result.message) }
                is RepositoryResult.Exception -> _uiState.update { it.copy(isImporting = false, showImportDialog = false, actionMessage = "Preview CSV gagal karena koneksi bermasalah.") }
            }
        }
    }

    fun commitCsv() {
        val state = _uiState.value
        val csv = state.importCsv ?: return
        if (state.isImporting || state.importPreview?.invalidRows != 0) return
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true) }
            when (val result = inventoryRepository.commitProductCsv(csv)) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            showImportDialog = false,
                            importCsv = null,
                            importPreview = null,
                            actionMessage = "${result.data.importedProducts} produk dan ${result.data.openingBalances} saldo awal berhasil diimpor."
                        )
                    }
                    loadProducts(page = 1)
                }
                is RepositoryResult.Error -> _uiState.update { it.copy(isImporting = false, actionMessage = result.message) }
                is RepositoryResult.Exception -> _uiState.update { it.copy(isImporting = false, actionMessage = "Impor CSV gagal karena koneksi bermasalah.") }
            }
        }
    }

    fun dismissImport() {
        if (_uiState.value.isImporting) return
        _uiState.update { it.copy(showImportDialog = false, importCsv = null, importPreview = null) }
    }

    private fun setLoadError(message: String) {
        _uiState.update { state ->
            state.copy(isLoading = false, errorMessage = message)
        }
    }

    private fun setActionError(message: String) {
        _uiState.update { state ->
            state.copy(isMutating = false, actionMessage = message)
        }
    }

    companion object {
        fun factory(inventoryRepository: InventoryRepository): ViewModelProvider.Factory {
            return viewModelFactory {
                ProductListViewModel(inventoryRepository = inventoryRepository)
            }
        }
    }
}


private const val PRODUCT_PAGE_SIZE = 10

private fun ProductStock.statusActionSuccessMessage(): String {
    return if (isActive) {
        "Produk $productName berhasil dinonaktifkan."
    } else {
        "Produk $productName berhasil diaktifkan kembali."
    }

}

private fun ProductStock.statusActionConnectionErrorMessage(): String {
    return if (isActive) {
        "Produk gagal dinonaktifkan karena koneksi bermasalah."
    } else {
        "Produk gagal diaktifkan kembali karena koneksi bermasalah."
    }
}
