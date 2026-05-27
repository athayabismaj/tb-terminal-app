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

class ProductFormViewModel(
    private val inventoryRepository: InventoryRepository,
    private val productId: String? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ProductFormUiState(
            isEditMode = productId != null,
            isLoading = true
        )
    )
    val uiState: StateFlow<ProductFormUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    fun onInputChanged(input: ProductFormInput) {
        _uiState.update { state -> state.copy(input = input, errorMessage = null) }
    }

    fun save() {
        val state = _uiState.value
        val input = state.input

        val priceBuy = input.priceBuy.toProductDecimalOrNull()
        val priceRetail = input.priceRetail.toProductDecimalOrNull()
        val priceContractor = input.priceContractor.toProductDecimalOrNull()
        val minStock = input.minStock.toProductDecimalOrNull()

        when {
            state.isSaving -> return
            input.name.isBlank() -> setError("Nama produk tidak boleh kosong.")
            input.sku.isBlank() && !state.isEditMode -> setError("SKU produk tidak boleh kosong.")
            input.categoryId.isBlank() -> setError("Kategori wajib dipilih.")
            input.baseUnitId.isBlank() -> setError("Satuan wajib dipilih.")
            priceBuy == null -> setError("Harga beli harus berupa angka valid.")
            priceRetail == null -> setError("Harga retail harus berupa angka valid.")
            priceContractor == null -> setError("Harga kontraktor harus berupa angka valid.")
            minStock == null -> setError("Stok minimum harus berupa angka valid.")
            else -> submit(
                priceBuy = priceBuy,
                priceRetail = priceRetail,
                priceContractor = priceContractor,
                discount = java.math.BigDecimal.ZERO,
                minStock = minStock
            )
        }
    }

    fun resetSavedState() {
        _uiState.update { state -> state.copy(isSaved = false) }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { state -> state.copy(isLoading = true, errorMessage = null) }

            val categoriesResult = inventoryRepository.getCategories()
            val unitsResult = inventoryRepository.getUnits()
            val productResult = productId?.let { id -> inventoryRepository.getProduct(id) }

            val categories = when (categoriesResult) {
                is RepositoryResult.Success -> categoriesResult.data
                is RepositoryResult.Error -> return@launch setLoadError(categoriesResult.message)
                is RepositoryResult.Exception -> return@launch setLoadError("Kategori gagal dimuat karena koneksi bermasalah.")
            }
            val units = when (unitsResult) {
                is RepositoryResult.Success -> unitsResult.data
                is RepositoryResult.Error -> return@launch setLoadError(unitsResult.message)
                is RepositoryResult.Exception -> return@launch setLoadError("Satuan gagal dimuat karena koneksi bermasalah.")
            }
            val product = when (productResult) {
                null -> null
                is RepositoryResult.Success -> productResult.data
                is RepositoryResult.Error -> return@launch setLoadError(productResult.message)
                is RepositoryResult.Exception -> return@launch setLoadError("Produk gagal dimuat karena koneksi bermasalah.")
            }

            _uiState.update { state ->
                state.copy(
                    categories = categories,
                    units = units,
                    input = product?.toFormInput()
                        ?: state.input.copy(
                            categoryId = categories.firstOrNull()?.id.orEmpty(),
                            baseUnitId = units.firstOrNull()?.id.orEmpty()
                        ),
                    isLoading = false,
                    errorMessage = null
                )
            }
        }
    }

    private fun submit(
        priceBuy: BigDecimal,
        priceRetail: BigDecimal,
        priceContractor: BigDecimal,
        discount: BigDecimal,
        minStock: BigDecimal
    ) {
        viewModelScope.launch {
            val input = _uiState.value.input
            _uiState.update { state -> state.copy(isSaving = true, errorMessage = null) }

            val result = if (productId == null) {
                inventoryRepository.createProduct(
                    CreateProductCommand(
                        categoryId = input.categoryId,
                        baseUnitId = input.baseUnitId,
                        sku = input.sku.trim(),
                        name = input.name.trim(),
                        priceBuy = priceBuy,
                        priceRetail = priceRetail,
                        priceContractor = priceContractor,
                discount = java.math.BigDecimal.ZERO,
                        minStock = minStock
                    )
                )
            } else {
                inventoryRepository.updateProduct(
                    id = productId,
                    command = UpdateProductCommand(
                        categoryId = input.categoryId,
                        baseUnitId = input.baseUnitId,
                        name = input.name.trim(),
                        priceBuy = priceBuy,
                        priceRetail = priceRetail,
                        priceContractor = priceContractor,
                discount = java.math.BigDecimal.ZERO,
                        minStock = minStock
                    )
                )
            }

            when (result) {
                is RepositoryResult.Success -> {
                    _uiState.update { state ->
                        state.copy(isSaving = false, isSaved = true)
                    }
                }
                is RepositoryResult.Error -> setError(result.message)
                is RepositoryResult.Exception -> setError("Produk gagal disimpan karena koneksi bermasalah.")
            }
        }
    }

    private fun setLoadError(message: String) {
        _uiState.update { state ->
            state.copy(isLoading = false, errorMessage = message)
        }
    }

    private fun setError(message: String) {
        _uiState.update { state ->
            state.copy(isSaving = false, errorMessage = message)
        }
    }

    companion object {
        fun factory(
            inventoryRepository: InventoryRepository,
            productId: String? = null
        ): ViewModelProvider.Factory {
            return viewModelFactory {
                ProductFormViewModel(
                    inventoryRepository = inventoryRepository,
                    productId = productId
                )
            }
        }
    }
}


private fun Product.toFormInput(): ProductFormInput {
    return ProductFormInput(
        sku = sku,
        name = name,
        categoryId = categoryId,
        baseUnitId = baseUnitId,
        priceBuy = priceBuy.toPlainString(),
        priceRetail = priceRetail.toPlainString(),
        priceContractor = priceContractor.toPlainString(),
        minStock = minStock.toPlainString()
    )
}

private fun String.toProductDecimalOrNull(): BigDecimal? {
    return trim()
        .replace(",", ".")
        .takeIf(String::isNotBlank)
        ?.runCatching { toBigDecimal() }
        ?.getOrNull()
}


