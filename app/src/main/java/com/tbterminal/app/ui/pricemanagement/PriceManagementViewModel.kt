package com.tbterminal.app.ui.pricemanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.Product
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.model.UpdateProductCommand
import com.tbterminal.app.data.repository.InventoryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class PriceManagementUiState(
    val products: List<ProductStock> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null,
    val searchQuery: String = "",
    val currentPage: Int = 1,
    val hasMorePages: Boolean = false,
    val totalProducts: Long = 0,
    
    // Dialog State
    val selectedProductStock: ProductStock? = null,
    val selectedProductDetail: Product? = null,
    val isDetailLoading: Boolean = false
)

class PriceManagementViewModel(
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PriceManagementUiState())
    val uiState: StateFlow<PriceManagementUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadProducts()
    }

    fun loadProducts(page: Int = 1) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val result = inventoryRepository.getProductStocks(
                page = page,
                limit = 20,
                search = _uiState.value.searchQuery.takeIf { it.isNotBlank() }
            )
            
            when (result) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            products = result.data.data,
                            currentPage = result.data.page,
                            hasMorePages = result.data.page < result.data.totalPages,
                            totalProducts = result.data.total,
                            isLoading = false
                        )
                    }
                }
                is RepositoryResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                is RepositoryResult.Exception -> {
                    _uiState.update { it.copy(isLoading = false, error = "Koneksi ke server gagal.") }
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500) // Debounce
            loadProducts(page = 1)
        }
    }

    fun nextPage() {
        if (_uiState.value.hasMorePages) {
            loadProducts(_uiState.value.currentPage + 1)
        }
    }

    fun previousPage() {
        if (_uiState.value.currentPage > 1) {
            loadProducts(_uiState.value.currentPage - 1)
        }
    }
    
    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }

    fun openPriceDialog(productStock: ProductStock) {
        _uiState.update { 
            it.copy(
                selectedProductStock = productStock,
                selectedProductDetail = null,
                isDetailLoading = true,
                error = null
            ) 
        }
        
        viewModelScope.launch {
            when (val result = inventoryRepository.getProduct(productStock.productId)) {
                is RepositoryResult.Success -> {
                    _uiState.update {
                        it.copy(
                            selectedProductDetail = result.data,
                            isDetailLoading = false
                        )
                    }
                }
                else -> {
                    _uiState.update {
                        it.copy(
                            isDetailLoading = false,
                            selectedProductStock = null,
                            actionMessage = "Gagal memuat detail produk."
                        )
                    }
                }
            }
        }
    }

    fun closePriceDialog() {
        _uiState.update { 
            it.copy(
                selectedProductStock = null, 
                selectedProductDetail = null,
                isDetailLoading = false 
            ) 
        }
    }

    fun updatePrice(
        priceBuy: BigDecimal,
        priceRetail: BigDecimal,
        priceContractor: BigDecimal,
        discount: BigDecimal
    ) {
        val detail = _uiState.value.selectedProductDetail ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            
            val command = UpdateProductCommand(
                categoryId = detail.categoryId,
                baseUnitId = detail.baseUnitId,
                name = detail.name,
                priceBuy = priceBuy,
                priceRetail = priceRetail,
                priceContractor = priceContractor,
                discount = discount,
                minStock = detail.minStock,
                photoFilename = detail.photoFilename
            )
            
            when (val result = inventoryRepository.updateProduct(detail.id, command)) {
                is RepositoryResult.Success -> {
                    _uiState.update { 
                        it.copy(
                            isSaving = false,
                            selectedProductStock = null,
                            selectedProductDetail = null,
                            actionMessage = "Harga untuk ${detail.name} berhasil diperbarui!"
                        ) 
                    }
                    loadProducts(_uiState.value.currentPage)
                }
                is RepositoryResult.Error -> {
                    _uiState.update { 
                        it.copy(
                            isSaving = false,
                            error = result.message
                        ) 
                    }
                }
                is RepositoryResult.Exception -> {
                    _uiState.update { 
                        it.copy(
                            isSaving = false,
                            error = "Koneksi bermasalah saat menyimpan harga."
                        ) 
                    }
                }
            }
        }
    }

    companion object {
        fun factory(inventoryRepository: InventoryRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return PriceManagementViewModel(inventoryRepository) as T
                }
            }
        }
    }
}
