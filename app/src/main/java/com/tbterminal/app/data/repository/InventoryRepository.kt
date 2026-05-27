package com.tbterminal.app.data.repository

import com.tbterminal.app.data.model.CreateProductCommand
import com.tbterminal.app.data.model.Product
import com.tbterminal.app.data.model.ProductCategory
import com.tbterminal.app.data.model.ProductCategoryPage
import com.tbterminal.app.data.model.ProductDetail
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.model.ProductStockPage
import com.tbterminal.app.data.model.ProductUnit
import com.tbterminal.app.data.model.ProductUnitPage
import com.tbterminal.app.data.model.StockAdjustment
import com.tbterminal.app.data.model.StockAdjustmentPage
import com.tbterminal.app.data.model.UpdateProductCommand
import com.tbterminal.app.data.remote.CategoryRequestDto
import com.tbterminal.app.data.remote.CategoryResponseDto
import com.tbterminal.app.data.remote.InventoryApi
import com.tbterminal.app.data.remote.PaginatedResponse
import com.tbterminal.app.data.remote.ProductCreateRequestDto
import com.tbterminal.app.data.remote.ProductResponseDto
import com.tbterminal.app.data.remote.ProductUpdateRequestDto
import com.tbterminal.app.data.remote.StockAdjustmentResponseDto
import com.tbterminal.app.data.remote.StockDetailResponseDto
import com.tbterminal.app.data.remote.StockOpnameRequestDto
import com.tbterminal.app.data.remote.UnitRequestDto
import com.tbterminal.app.data.remote.UnitResponseDto
import com.tbterminal.app.data.remote.safeApiCall

interface InventoryRepository {
    suspend fun getProductStocks(
        page: Int = 1,
        limit: Int = 20,
        search: String? = null
    ): RepositoryResult<ProductStockPage>

    suspend fun getStockAdjustments(
        page: Int = 1,
        limit: Int = 20,
        search: String? = null,
        type: String? = null
    ): RepositoryResult<StockAdjustmentPage>

    suspend fun getProduct(id: String): RepositoryResult<Product>

    suspend fun getProductDetail(id: String): RepositoryResult<ProductDetail>

    suspend fun createProduct(command: CreateProductCommand): RepositoryResult<Product>

    suspend fun updateProduct(
        id: String,
        command: UpdateProductCommand
    ): RepositoryResult<Product>

    suspend fun deleteProduct(id: String): RepositoryResult<Unit>

    suspend fun activateProduct(id: String): RepositoryResult<Product>

    suspend fun executeStockOpname(
        productId: String,
        adjustmentType: String,
        actualQty: java.math.BigDecimal,
        notes: String?
    ): RepositoryResult<Unit>

    suspend fun getCategories(): RepositoryResult<List<ProductCategory>>

    suspend fun getCategoryPage(
        page: Int = 1,
        limit: Int = 10,
        search: String? = null
    ): RepositoryResult<ProductCategoryPage>

    suspend fun createCategory(name: String): RepositoryResult<ProductCategory>

    suspend fun updateCategory(
        id: String,
        name: String
    ): RepositoryResult<ProductCategory>

    suspend fun deleteCategory(id: String): RepositoryResult<Unit>

    suspend fun getUnits(): RepositoryResult<List<ProductUnit>>

    suspend fun getUnitPage(
        page: Int = 1,
        limit: Int = 10,
        search: String? = null
    ): RepositoryResult<ProductUnitPage>

    suspend fun createUnit(
        name: String,
        symbol: String
    ): RepositoryResult<ProductUnit>

    suspend fun updateUnit(
        id: String,
        name: String,
        symbol: String
    ): RepositoryResult<ProductUnit>

    suspend fun deleteUnit(id: String): RepositoryResult<Unit>
}

class RemoteInventoryRepository(
    private val inventoryApi: InventoryApi
) : InventoryRepository {
    override suspend fun getProductStocks(
        page: Int,
        limit: Int,
        search: String?
    ): RepositoryResult<ProductStockPage> {
        return safeApiCall {
            inventoryApi.getStockDetails(page = page, limit = limit, search = search?.takeIf(String::isNotBlank))
        }.toRepositoryResult { response ->
            val stockPage = response.data
            if (!response.success || stockPage == null) {
                RepositoryResult.Error(
                    code = response.code ?: "PRODUCT_STOCKS_FAILED",
                    message = response.message ?: response.error ?: "Daftar produk gagal dimuat."
                )
            } else {
                RepositoryResult.Success(stockPage.toProductStockPage())
            }
        }
    }

    override suspend fun getStockAdjustments(
        page: Int,
        limit: Int,
        search: String?,
        type: String?
    ): RepositoryResult<StockAdjustmentPage> {
        return safeApiCall {
            inventoryApi.getStockAdjustments(
                page = page,
                limit = limit,
                search = search?.takeIf(String::isNotBlank),
                type = type?.takeIf(String::isNotBlank)
            )
        }.toRepositoryResult { response ->
            val adjustmentPage = response.data
            if (!response.success || adjustmentPage == null) {
                RepositoryResult.Error(
                    code = response.code ?: "STOCK_ADJUSTMENTS_FAILED",
                    message = response.message ?: response.error ?: "Riwayat penyesuaian stok gagal dimuat."
                )
            } else {
                RepositoryResult.Success(adjustmentPage.toStockAdjustmentPage())
            }
        }
    }

    override suspend fun getProduct(id: String): RepositoryResult<Product> {
        return safeApiCall { inventoryApi.getProduct(id) }
            .toRepositoryResult { response ->
                val product = response.data
                if (!response.success || product == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "PRODUCT_FAILED",
                        message = response.message ?: response.error ?: "Produk gagal dimuat."
                    )
                } else {
                    RepositoryResult.Success(product.toProduct())
                }
            }
    }

    override suspend fun getProductDetail(id: String): RepositoryResult<ProductDetail> {
        return when (val productResult = getProduct(id)) {
            is RepositoryResult.Error -> productResult
            is RepositoryResult.Exception -> productResult
            is RepositoryResult.Success -> {
                val product = productResult.data
                val stock = when (
                    val stockResult = getProductStocks(page = 1, limit = 50, search = product.sku)
                ) {
                    is RepositoryResult.Success -> {
                        stockResult.data.data.firstOrNull { item -> item.productId == id }
                    }
                    is RepositoryResult.Error,
                    is RepositoryResult.Exception -> null
                }

                RepositoryResult.Success(
                    ProductDetail(
                        product = product,
                        stock = stock
                    )
                )
            }
        }
    }

    override suspend fun createProduct(command: CreateProductCommand): RepositoryResult<Product> {
        val request = ProductCreateRequestDto(
            categoryId = command.categoryId,
            baseUnitId = command.baseUnitId,
            sku = command.sku,
            name = command.name,
            priceBuy = command.priceBuy,
            priceRetail = command.priceRetail,
            priceContractor = command.priceContractor,
            discount = command.discount,
            minStock = command.minStock,
            photoFilename = command.photoFilename
        )

        return safeApiCall { inventoryApi.createProduct(request) }
            .toRepositoryResult { response ->
                val product = response.data
                if (!response.success || product == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "CREATE_PRODUCT_FAILED",
                        message = response.message ?: response.error ?: "Produk gagal disimpan."
                    )
                } else {
                    RepositoryResult.Success(product.toProduct())
                }
            }
    }

    override suspend fun updateProduct(
        id: String,
        command: UpdateProductCommand
    ): RepositoryResult<Product> {
        val request = ProductUpdateRequestDto(
            categoryId = command.categoryId,
            baseUnitId = command.baseUnitId,
            name = command.name,
            priceBuy = command.priceBuy,
            priceRetail = command.priceRetail,
            priceContractor = command.priceContractor,
            discount = command.discount,
            minStock = command.minStock,
            photoFilename = command.photoFilename
        )

        return safeApiCall { inventoryApi.updateProduct(id, request) }
            .toRepositoryResult { response ->
                val product = response.data
                if (!response.success || product == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "UPDATE_PRODUCT_FAILED",
                        message = response.message ?: response.error ?: "Produk gagal diperbarui."
                    )
                } else {
                    RepositoryResult.Success(product.toProduct())
                }
            }
    }

    override suspend fun deleteProduct(id: String): RepositoryResult<Unit> {
        return safeApiCall { inventoryApi.deleteProduct(id) }
            .toRepositoryResult { response ->
                if (!response.success) {
                    RepositoryResult.Error(
                        code = response.code ?: "DELETE_PRODUCT_FAILED",
                        message = response.message ?: response.error ?: "Produk gagal dinonaktifkan."
                    )
                } else {
                    RepositoryResult.Success(Unit)
                }
            }
    }

    override suspend fun activateProduct(id: String): RepositoryResult<Product> {
        return safeApiCall { inventoryApi.activateProduct(id) }
            .toRepositoryResult { response ->
                val product = response.data
                if (!response.success || product == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "ACTIVATE_PRODUCT_FAILED",
                        message = response.message ?: response.error ?: "Produk gagal diaktifkan kembali."
                    )
                } else {
                    RepositoryResult.Success(product.toProduct())
                }
            }
    }

    override suspend fun executeStockOpname(
        productId: String,
        adjustmentType: String,
        actualQty: java.math.BigDecimal,
        notes: String?
    ): RepositoryResult<Unit> {
        val request = StockOpnameRequestDto(
            productId = productId,
            adjustmentType = adjustmentType,
            actualQty = actualQty,
            notes = notes?.trim()?.takeIf(String::isNotBlank)
        )

        return safeApiCall { inventoryApi.executeStockOpname(request) }
            .toRepositoryResult { response ->
                if (!response.success) {
                    RepositoryResult.Error(
                        code = response.code ?: "STOCK_OPNAME_FAILED",
                        message = response.message ?: response.error ?: "Stok opname gagal disimpan."
                    )
                } else {
                    RepositoryResult.Success(Unit)
                }
            }
    }

    override suspend fun getCategories(): RepositoryResult<List<ProductCategory>> {
        return safeApiCall { inventoryApi.getCategories() }
            .toRepositoryResult { response ->
                val categories = response.data
                if (!response.success || categories == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "CATEGORIES_FAILED",
                        message = response.message ?: response.error ?: "Kategori gagal dimuat."
                    )
                } else {
                    RepositoryResult.Success(categories.map(CategoryResponseDto::toProductCategory))
                }
            }
    }

    override suspend fun getCategoryPage(
        page: Int,
        limit: Int,
        search: String?
    ): RepositoryResult<ProductCategoryPage> {
        return safeApiCall {
            inventoryApi.getCategoriesPage(page = page, limit = limit, search = search?.takeIf(String::isNotBlank))
        }.toRepositoryResult { response ->
            val categoryPage = response.data
            if (!response.success || categoryPage == null) {
                RepositoryResult.Error(
                    code = response.code ?: "CATEGORIES_FAILED",
                    message = response.message ?: response.error ?: "Kategori gagal dimuat."
                )
            } else {
                RepositoryResult.Success(categoryPage.toProductCategoryPage())
            }
        }
    }

    override suspend fun createCategory(name: String): RepositoryResult<ProductCategory> {
        return safeApiCall { inventoryApi.createCategory(CategoryRequestDto(name = name)) }
            .toRepositoryResult { response ->
                val category = response.data
                if (!response.success || category == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "CREATE_CATEGORY_FAILED",
                        message = response.message ?: response.error ?: "Kategori gagal disimpan."
                    )
                } else {
                    RepositoryResult.Success(category.toProductCategory())
                }
            }
    }

    override suspend fun updateCategory(
        id: String,
        name: String
    ): RepositoryResult<ProductCategory> {
        return safeApiCall { inventoryApi.updateCategory(id, CategoryRequestDto(name = name)) }
            .toRepositoryResult { response ->
                val category = response.data
                if (!response.success || category == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "UPDATE_CATEGORY_FAILED",
                        message = response.message ?: response.error ?: "Kategori gagal diperbarui."
                    )
                } else {
                    RepositoryResult.Success(category.toProductCategory())
                }
            }
    }

    override suspend fun deleteCategory(id: String): RepositoryResult<Unit> {
        return safeApiCall { inventoryApi.deleteCategory(id) }
            .toRepositoryResult { response ->
                if (!response.success) {
                    RepositoryResult.Error(
                        code = response.code ?: "DELETE_CATEGORY_FAILED",
                        message = response.message ?: response.error ?: "Kategori gagal dihapus."
                    )
                } else {
                    RepositoryResult.Success(Unit)
                }
            }
    }

    override suspend fun getUnits(): RepositoryResult<List<ProductUnit>> {
        return safeApiCall { inventoryApi.getUnits() }
            .toRepositoryResult { response ->
                val units = response.data
                if (!response.success || units == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "UNITS_FAILED",
                        message = response.message ?: response.error ?: "Satuan gagal dimuat."
                    )
                } else {
                    RepositoryResult.Success(units.map(UnitResponseDto::toProductUnit))
                }
            }
    }

    override suspend fun getUnitPage(
        page: Int,
        limit: Int,
        search: String?
    ): RepositoryResult<ProductUnitPage> {
        return safeApiCall {
            inventoryApi.getUnitsPage(page = page, limit = limit, search = search?.takeIf(String::isNotBlank))
        }.toRepositoryResult { response ->
            val unitPage = response.data
            if (!response.success || unitPage == null) {
                RepositoryResult.Error(
                    code = response.code ?: "UNITS_FAILED",
                    message = response.message ?: response.error ?: "Satuan gagal dimuat."
                )
            } else {
                RepositoryResult.Success(unitPage.toProductUnitPage())
            }
        }
    }

    override suspend fun createUnit(
        name: String,
        symbol: String
    ): RepositoryResult<ProductUnit> {
        return safeApiCall { inventoryApi.createUnit(UnitRequestDto(name = name, symbol = symbol)) }
            .toRepositoryResult { response ->
                val unit = response.data
                if (!response.success || unit == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "CREATE_UNIT_FAILED",
                        message = response.message ?: response.error ?: "Satuan gagal disimpan."
                    )
                } else {
                    RepositoryResult.Success(unit.toProductUnit())
                }
            }
    }

    override suspend fun updateUnit(
        id: String,
        name: String,
        symbol: String
    ): RepositoryResult<ProductUnit> {
        return safeApiCall { inventoryApi.updateUnit(id, UnitRequestDto(name = name, symbol = symbol)) }
            .toRepositoryResult { response ->
                val unit = response.data
                if (!response.success || unit == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "UPDATE_UNIT_FAILED",
                        message = response.message ?: response.error ?: "Satuan gagal diperbarui."
                    )
                } else {
                    RepositoryResult.Success(unit.toProductUnit())
                }
            }
    }

    override suspend fun deleteUnit(id: String): RepositoryResult<Unit> {
        return safeApiCall { inventoryApi.deleteUnit(id) }
            .toRepositoryResult { response ->
                if (!response.success) {
                    RepositoryResult.Error(
                        code = response.code ?: "DELETE_UNIT_FAILED",
                        message = response.message ?: response.error ?: "Satuan gagal dihapus."
                    )
                } else {
                    RepositoryResult.Success(Unit)
                }
            }
    }
}
