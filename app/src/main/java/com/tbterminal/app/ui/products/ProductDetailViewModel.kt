package com.tbterminal.app.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductDetailViewModel(
    private val inventoryRepository: InventoryRepository,
    private val productId: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail()
    }

    fun loadDetail() {
        viewModelScope.launch {
            _uiState.update { state -> state.copy(isLoading = true, errorMessage = null) }

            when (val result = inventoryRepository.getProductDetail(productId)) {
                is RepositoryResult.Success -> {
                    _uiState.update { state ->
                        state.copy(detail = result.data, isLoading = false)
                    }
                }
                is RepositoryResult.Error -> setError(result.message)
                is RepositoryResult.Exception -> setError("Detail produk gagal dimuat karena koneksi bermasalah.")
            }
        }
    }

    private fun setError(message: String) {
        _uiState.update { state ->
            state.copy(isLoading = false, errorMessage = message)
        }
    }

    companion object {
        fun factory(
            inventoryRepository: InventoryRepository,
            productId: String
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                ProductDetailViewModel(
                    inventoryRepository = inventoryRepository,
                    productId = productId
                )
            }
        }
    }
}
