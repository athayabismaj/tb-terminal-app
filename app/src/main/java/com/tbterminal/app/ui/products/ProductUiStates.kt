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
import com.tbterminal.app.data.model.ProductCsvPreview
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

data class ProductListUiState(
    val products: List<ProductStock> = emptyList(),
    val categories: List<ProductCategory> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = ALL_PRODUCT_CATEGORIES,
    val page: Int = 1,
    val totalPages: Int = 1,
    val totalProducts: Long = 0,
    val pageSize: Int = 10,
    val isLoading: Boolean = false,
    val isMutating: Boolean = false,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
    val importCsv: String? = null,
    val importPreview: ProductCsvPreview? = null,
    val isImporting: Boolean = false,
    val showImportDialog: Boolean = false
) {
    val visibleProducts: List<ProductStock>
        get() = if (selectedCategory == ALL_PRODUCT_CATEGORIES) {
            products
        } else {
            products.filter { product -> product.categoryName == selectedCategory }
        }

    val availableCategories: List<String>
        get() = listOf(ALL_PRODUCT_CATEGORIES) + categories
            .map(ProductCategory::name)
            .filter(String::isNotBlank)
            .distinct()
            .sorted()

    val lowStockCount: Int
        get() = products.count { product -> product.quantity <= product.minStock }

    val activeCount: Int
        get() = products.count(ProductStock::isActive)
}

internal const val ALL_PRODUCT_CATEGORIES = "Semua Kategori"


data class ProductFormInput(
    val sku: String = "",
    val name: String = "",
    val categoryId: String = "",
    val baseUnitId: String = "",
    val priceBuy: String = "",
    val priceRetail: String = "",
    val priceContractor: String = "",
    val minStock: String = ""
)

data class ProductFormUiState(
    val input: ProductFormInput = ProductFormInput(),
    val categories: List<ProductCategory> = emptyList(),
    val units: List<ProductUnit> = emptyList(),
    val isEditMode: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)


data class ProductDetailUiState(
    val detail: ProductDetail? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

data class ProductCategoryUiState(
    val categories: List<ProductCategory> = emptyList(),
    val searchQuery: String = "",
    val page: Int = 1,
    val totalPages: Int = 1,
    val totalCategories: Long = 0,
    val pageSize: Int = 10,
    val nameInput: String = "",
    val editingCategory: ProductCategory? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val message: String? = null
)

data class ProductUnitUiState(
    val units: List<ProductUnit> = emptyList(),
    val searchQuery: String = "",
    val page: Int = 1,
    val totalPages: Int = 1,
    val totalUnits: Long = 0,
    val pageSize: Int = 10,
    val nameInput: String = "",
    val symbolInput: String = "",
    val editingUnit: ProductUnit? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val message: String? = null
)

