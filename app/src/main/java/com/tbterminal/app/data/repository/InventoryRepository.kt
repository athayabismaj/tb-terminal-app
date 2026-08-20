package com.tbterminal.app.data.repository

import com.tbterminal.app.data.model.CreateProductCommand
import com.tbterminal.app.data.local.database.CategoryLocalDataSource
import com.tbterminal.app.data.local.database.ProductLocalDataSource
import com.tbterminal.app.data.local.database.UnitLocalDataSource
import com.tbterminal.app.data.local.entity.LocalProductEntity
import com.tbterminal.app.data.local.mapper.toLocalCategoryEntity
import com.tbterminal.app.data.local.mapper.toLocalProductEntity
import com.tbterminal.app.data.local.mapper.toLocalUnitEntity
import com.tbterminal.app.data.local.mapper.toProduct as toCachedProduct
import com.tbterminal.app.data.local.mapper.toProductCategory as toCachedProductCategory
import com.tbterminal.app.data.local.mapper.toProductUnit as toCachedProductUnit
import com.tbterminal.app.data.model.Product
import com.tbterminal.app.data.model.ProductCategory
import com.tbterminal.app.data.model.ProductCategoryPage
import com.tbterminal.app.data.model.ProductPage
import com.tbterminal.app.data.model.ProductDetail
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.model.ProductStockPage
import com.tbterminal.app.data.model.ProductUnit
import com.tbterminal.app.data.model.ProductUnitPage
import com.tbterminal.app.data.model.StockAdjustment
import com.tbterminal.app.data.model.StockAdjustmentPage
import com.tbterminal.app.data.model.StockCard
import com.tbterminal.app.data.model.StockMovement
import com.tbterminal.app.data.model.UpdateProductCommand
import com.tbterminal.app.data.model.ProductCsvImportResult
import com.tbterminal.app.data.model.ProductCsvPreview
import com.tbterminal.app.data.model.ProductCsvRowPreview
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
import com.tbterminal.app.data.remote.ProductCsvImportRequestDto
import com.tbterminal.app.data.remote.OpeningStockRequestDto
import com.tbterminal.app.data.remote.safeApiCall
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException

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
    suspend fun getStockCard(
        page: Int = 1,
        limit: Int = 20,
        productId: String? = null,
        search: String? = null,
        type: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ): RepositoryResult<StockCard>

    suspend fun getProduct(id: String): RepositoryResult<Product>

    suspend fun getProductDetail(id: String): RepositoryResult<ProductDetail>

    suspend fun getProductsPage(
        page: Int = 1,
        limit: Int = 20,
        search: String? = null
    ): RepositoryResult<ProductPage>


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

    suspend fun previewProductCsv(csv: String): RepositoryResult<ProductCsvPreview>
    suspend fun commitProductCsv(csv: String): RepositoryResult<ProductCsvImportResult>
    suspend fun createOpeningStock(
        productId: String,
        date: String,
        quantity: BigDecimal,
        note: String
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
    private val inventoryApi: InventoryApi,
    private val productLocalDataSource: ProductLocalDataSource? = null,
    private val categoryLocalDataSource: CategoryLocalDataSource? = null,
    private val unitLocalDataSource: UnitLocalDataSource? = null
) : InventoryRepository {
    override suspend fun getProductStocks(
        page: Int,
        limit: Int,
        search: String?
    ): RepositoryResult<ProductStockPage> {
        val result = safeApiCall {
            inventoryApi.getStockDetails(page = page, limit = limit, search = search?.takeIf(String::isNotBlank))
        }.toRepositoryResult { response ->
            val stockPage = response.data
            if (!response.success || stockPage == null) {
                RepositoryResult.Error(
                    code = response.code ?: "PRODUCT_STOCKS_FAILED",
                    message = response.message ?: response.error ?: "Daftar produk gagal dimuat."
                )
            } else {
                cacheProductStocks(stockPage.data)
                RepositoryResult.Success(stockPage.toProductStockPage())
            }
        }

        return when (result) {
            is RepositoryResult.Success -> result
            is RepositoryResult.Error,
            is RepositoryResult.Exception -> fallbackProductStockPage(page, limit, search, result)
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
        val result = safeApiCall { inventoryApi.getProduct(id) }
            .toRepositoryResult { response ->
                val product = response.data
                if (!response.success || product == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "PRODUCT_FAILED",
                        message = response.message ?: response.error ?: "Produk gagal dimuat."
                    )
                } else {
                    cacheProduct(product)
                    RepositoryResult.Success(product.toProduct())
                }
            }

        return when (result) {
            is RepositoryResult.Success -> result
            is RepositoryResult.Error,
            is RepositoryResult.Exception -> fallbackProduct(id, result)
        }
    }

    override suspend fun getStockCard(
        page: Int,
        limit: Int,
        productId: String?,
        search: String?,
        type: String?,
        startDate: String?,
        endDate: String?
    ): RepositoryResult<StockCard> {
        return safeApiCall {
            inventoryApi.getStockCard(page, limit, productId, search, type, startDate, endDate)
        }.toRepositoryResult { response ->
            val card = response.data
            if (!response.success || card == null) {
                RepositoryResult.Error(
                    response.code ?: "STOCK_CARD_FAILED",
                    response.message ?: response.error ?: "Kartu stok gagal dimuat."
                )
            } else RepositoryResult.Success(
                StockCard(
                    data = card.data.map { row ->
                        StockMovement(
                            row.id, row.productId, row.sku, row.productName, row.unitName, row.type,
                            row.balanceBefore, row.qtyIn, row.qtyOut, row.balanceAfter,
                            row.referenceType, row.referenceNumber, row.occurredAt
                        )
                    },
                    total = card.total,
                    page = card.page,
                    limit = card.limit,
                    totalPages = card.totalPages,
                    currentStock = card.currentStock,
                    ledgerBalance = card.ledgerBalance,
                    reconciled = card.reconciled
                )
            )
        }
    }


    override suspend fun getProductsPage(
        page: Int,
        limit: Int,
        search: String?
    ): RepositoryResult<ProductPage> {
        val result = safeApiCall {
            inventoryApi.getProducts(page = page, limit = limit, search = search?.takeIf(String::isNotBlank))
        }.toRepositoryResult { response ->
            val productPage = response.data
            if (!response.success || productPage == null) {
                RepositoryResult.Error(
                    code = response.code ?: "PRODUCTS_FAILED",
                    message = response.message ?: response.error ?: "Produk gagal dimuat."
                )
                } else {
                cacheProducts(productPage.data)
                RepositoryResult.Success(productPage.toProductPage())
            }
        }

        return when (result) {
            is RepositoryResult.Success -> result
            is RepositoryResult.Error,
            is RepositoryResult.Exception -> fallbackProductPage(page, limit, search, result)
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
                    cacheProduct(product)
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
                    cacheProduct(product)
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
                    cacheProduct(product)
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
        val result = safeApiCall { inventoryApi.getCategories() }
            .toRepositoryResult { response ->
                val categories = response.data
                if (!response.success || categories == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "CATEGORIES_FAILED",
                        message = response.message ?: response.error ?: "Kategori gagal dimuat."
                    )
                } else {
                    cacheCategories(categories)
                    RepositoryResult.Success(categories.map(CategoryResponseDto::toProductCategory))
                }
            }

        return when (result) {
            is RepositoryResult.Success -> result
            is RepositoryResult.Error,
            is RepositoryResult.Exception -> fallbackCategories(result)
        }
    }

    override suspend fun previewProductCsv(csv: String): RepositoryResult<ProductCsvPreview> {
        return safeApiCall { inventoryApi.previewProductCsv(ProductCsvImportRequestDto(csv)) }
            .toRepositoryResult { response ->
                val preview = response.data
                if (!response.success || preview == null) {
                    RepositoryResult.Error(
                        response.code ?: "CSV_PREVIEW_FAILED",
                        response.message ?: response.error ?: "Preview CSV gagal dibuat."
                    )
                } else {
                    RepositoryResult.Success(
                        ProductCsvPreview(
                            preview.totalRows,
                            preview.validRows,
                            preview.invalidRows,
                            preview.rows.map { row ->
                                ProductCsvRowPreview(row.rowNumber, row.sku, row.name, row.category, row.unit, row.errors)
                            }
                        )
                    )
                }
            }
    }

    override suspend fun commitProductCsv(csv: String): RepositoryResult<ProductCsvImportResult> {
        return safeApiCall { inventoryApi.commitProductCsv(ProductCsvImportRequestDto(csv)) }
            .toRepositoryResult { response ->
                val result = response.data
                if (!response.success || result == null) {
                    RepositoryResult.Error(
                        response.code ?: "CSV_IMPORT_FAILED",
                        response.message ?: response.error ?: "Impor CSV gagal disimpan."
                    )
                } else {
                    RepositoryResult.Success(ProductCsvImportResult(result.importedProducts, result.openingBalances))
                }
            }
    }

    override suspend fun createOpeningStock(
        productId: String,
        date: String,
        quantity: BigDecimal,
        note: String
    ): RepositoryResult<Unit> {
        return safeApiCall {
            inventoryApi.createOpeningStock(OpeningStockRequestDto(productId, date, quantity, note.trim()))
        }.toRepositoryResult { response ->
            if (!response.success || response.data == null) {
                RepositoryResult.Error(
                    response.code ?: "OPENING_STOCK_FAILED",
                    response.message ?: response.error ?: "Saldo awal gagal disimpan."
                )
            } else RepositoryResult.Success(Unit)
        }
    }

    override suspend fun getCategoryPage(
        page: Int,
        limit: Int,
        search: String?
    ): RepositoryResult<ProductCategoryPage> {
        val result = safeApiCall {
            inventoryApi.getCategoriesPage(page = page, limit = limit, search = search?.takeIf(String::isNotBlank))
        }.toRepositoryResult { response ->
            val categoryPage = response.data
            if (!response.success || categoryPage == null) {
                RepositoryResult.Error(
                    code = response.code ?: "CATEGORIES_FAILED",
                    message = response.message ?: response.error ?: "Kategori gagal dimuat."
                )
                } else {
                    cacheCategories(categoryPage.data)
                RepositoryResult.Success(categoryPage.toProductCategoryPage())
            }
        }

        return when (result) {
            is RepositoryResult.Success -> result
            is RepositoryResult.Error,
            is RepositoryResult.Exception -> fallbackCategoryPage(page, limit, search, result)
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
                    cacheCategory(category)
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
                    cacheCategory(category)
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
        val result = safeApiCall { inventoryApi.getUnits() }
            .toRepositoryResult { response ->
                val units = response.data
                if (!response.success || units == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "UNITS_FAILED",
                        message = response.message ?: response.error ?: "Satuan gagal dimuat."
                    )
                } else {
                    cacheUnits(units)
                    RepositoryResult.Success(units.map(UnitResponseDto::toProductUnit))
                }
            }

        return when (result) {
            is RepositoryResult.Success -> result
            is RepositoryResult.Error,
            is RepositoryResult.Exception -> fallbackUnits(result)
        }
    }

    override suspend fun getUnitPage(
        page: Int,
        limit: Int,
        search: String?
    ): RepositoryResult<ProductUnitPage> {
        val result = safeApiCall {
            inventoryApi.getUnitsPage(page = page, limit = limit, search = search?.takeIf(String::isNotBlank))
        }.toRepositoryResult { response ->
            val unitPage = response.data
            if (!response.success || unitPage == null) {
                RepositoryResult.Error(
                    code = response.code ?: "UNITS_FAILED",
                    message = response.message ?: response.error ?: "Satuan gagal dimuat."
                )
                } else {
                    cacheUnits(unitPage.data)
                RepositoryResult.Success(unitPage.toProductUnitPage())
            }
        }

        return when (result) {
            is RepositoryResult.Success -> result
            is RepositoryResult.Error,
            is RepositoryResult.Exception -> fallbackUnitPage(page, limit, search, result)
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
                    cacheUnit(unit)
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
                    cacheUnit(unit)
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

    private suspend fun cacheProduct(product: ProductResponseDto) {
        cacheSafely {
            productLocalDataSource?.cacheProduct(product.toLocalProductEntity())
        }
    }

    private suspend fun cacheProducts(products: List<ProductResponseDto>) {
        cacheSafely {
            productLocalDataSource?.cacheProducts(products.map(ProductResponseDto::toLocalProductEntity))
        }
    }

    private suspend fun cacheProductStocks(products: List<StockDetailResponseDto>) {
        cacheSafely {
            productLocalDataSource?.cacheProductStockSnapshots(
                products.map(StockDetailResponseDto::toLocalProductEntity)
            )
        }
    }

    private suspend fun cacheCategory(category: CategoryResponseDto) {
        cacheSafely {
            categoryLocalDataSource?.cacheCategory(category.toLocalCategoryEntity())
        }
    }

    private suspend fun cacheCategories(categories: List<CategoryResponseDto>) {
        cacheSafely {
            categoryLocalDataSource?.cacheCategories(categories.map(CategoryResponseDto::toLocalCategoryEntity))
        }
    }

    private suspend fun cacheUnit(unit: UnitResponseDto) {
        cacheSafely {
            unitLocalDataSource?.cacheUnit(unit.toLocalUnitEntity())
        }
    }

    private suspend fun cacheUnits(units: List<UnitResponseDto>) {
        cacheSafely {
            unitLocalDataSource?.cacheUnits(units.map(UnitResponseDto::toLocalUnitEntity))
        }
    }

    private suspend fun cacheSafely(block: suspend () -> Unit) {
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // Cache failure must not change the online-first repository behavior.
        }
    }

    private suspend fun fallbackProduct(
        id: String,
        original: RepositoryResult<Product>
    ): RepositoryResult<Product> {
        if (!original.shouldFallbackToCache()) return original
        return readCacheOrOriginal(original) {
            productLocalDataSource?.getCachedProductByServerId(id)?.toCachedProduct()
                ?.let { RepositoryResult.Success(it) }
        }
    }

    private suspend fun fallbackProductPage(
        page: Int,
        limit: Int,
        search: String?,
        original: RepositoryResult<ProductPage>
    ): RepositoryResult<ProductPage> {
        if (!original.shouldFallbackToCache()) return original
        return readCacheOrOriginal(original) {
            val cached = productLocalDataSource?.getCachedProducts(page, limit, search) ?: return@readCacheOrOriginal null
            if (cached.data.isEmpty()) return@readCacheOrOriginal null
            RepositoryResult.Success(
                ProductPage(
                    data = cached.data.map { it.toCachedProduct() },
                    total = cached.total,
                    page = cached.page,
                    limit = cached.limit,
                    totalPages = cached.totalPages
                )
            )
        }
    }

    private suspend fun fallbackProductStockPage(
        page: Int,
        limit: Int,
        search: String?,
        original: RepositoryResult<ProductStockPage>
    ): RepositoryResult<ProductStockPage> {
        if (!original.shouldFallbackToCache()) return original
        return readCacheOrOriginal(original) {
            val cached = productLocalDataSource?.getCachedProducts(page, limit, search)
                ?: return@readCacheOrOriginal null
            if (cached.data.isEmpty()) return@readCacheOrOriginal null
            RepositoryResult.Success(
                ProductStockPage(
                    data = cached.data.map { it.toCachedProductStock() },
                    total = cached.total,
                    page = cached.page,
                    limit = cached.limit,
                    totalPages = cached.totalPages
                )
            )
        }
    }

    private suspend fun fallbackCategories(
        original: RepositoryResult<List<ProductCategory>>
    ): RepositoryResult<List<ProductCategory>> {
        if (!original.shouldFallbackToCache()) return original
        return readCacheOrOriginal(original) {
            val cached = categoryLocalDataSource?.getCachedCategories().orEmpty()
            if (cached.isEmpty()) null else RepositoryResult.Success(cached.map { it.toCachedProductCategory() })
        }
    }

    private suspend fun fallbackCategoryPage(
        page: Int,
        limit: Int,
        search: String?,
        original: RepositoryResult<ProductCategoryPage>
    ): RepositoryResult<ProductCategoryPage> {
        if (!original.shouldFallbackToCache()) return original
        return readCacheOrOriginal(original) {
            val cached = categoryLocalDataSource?.getCachedCategoryPage(page, limit, search)
                ?: return@readCacheOrOriginal null
            if (cached.data.isEmpty()) return@readCacheOrOriginal null
            RepositoryResult.Success(
                ProductCategoryPage(
                    data = cached.data.map { it.toCachedProductCategory() },
                    total = cached.total,
                    page = cached.page,
                    limit = cached.limit,
                    totalPages = cached.totalPages
                )
            )
        }
    }

    private suspend fun fallbackUnits(
        original: RepositoryResult<List<ProductUnit>>
    ): RepositoryResult<List<ProductUnit>> {
        if (!original.shouldFallbackToCache()) return original
        return readCacheOrOriginal(original) {
            val cached = unitLocalDataSource?.getCachedUnits().orEmpty()
            if (cached.isEmpty()) null else RepositoryResult.Success(cached.map { it.toCachedProductUnit() })
        }
    }

    private suspend fun fallbackUnitPage(
        page: Int,
        limit: Int,
        search: String?,
        original: RepositoryResult<ProductUnitPage>
    ): RepositoryResult<ProductUnitPage> {
        if (!original.shouldFallbackToCache()) return original
        return readCacheOrOriginal(original) {
            val cached = unitLocalDataSource?.getCachedUnitPage(page, limit, search)
                ?: return@readCacheOrOriginal null
            if (cached.data.isEmpty()) return@readCacheOrOriginal null
            RepositoryResult.Success(
                ProductUnitPage(
                    data = cached.data.map { it.toCachedProductUnit() },
                    total = cached.total,
                    page = cached.page,
                    limit = cached.limit,
                    totalPages = cached.totalPages
                )
            )
        }
    }

    private suspend fun <T> readCacheOrOriginal(
        original: RepositoryResult<T>,
        block: suspend () -> RepositoryResult<T>?
    ): RepositoryResult<T> {
        return try {
            block() ?: original
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            original
        }
    }
}

private fun RepositoryResult<*>.shouldFallbackToCache(): Boolean {
    return when (this) {
        is RepositoryResult.Success -> false
        is RepositoryResult.Exception -> true
        is RepositoryResult.Error -> {
            val normalizedCode = code.uppercase()
            val normalizedMessage = message.uppercase()
            val isAuthError = normalizedCode.contains("401") ||
                normalizedCode.contains("403") ||
                normalizedCode.contains("UNAUTHORIZED") ||
                normalizedCode.contains("FORBIDDEN") ||
                normalizedMessage.contains("UNAUTHORIZED") ||
                normalizedMessage.contains("FORBIDDEN")
            !isAuthError && (
                normalizedCode.startsWith("HTTP_5") ||
                    normalizedCode == "EMPTY_BODY" ||
                    normalizedCode.contains("TIMEOUT") ||
                    normalizedCode.contains("NETWORK") ||
                    normalizedCode.contains("SERVER")
                )
        }
    }
}

private fun LocalProductEntity.toCachedProductStock(): ProductStock {
    return ProductStock(
        productId = serverId.orEmpty(),
        sku = sku.orEmpty(),
        productName = name,
        categoryName = "",
        unitName = "",
        quantity = stock.toBigDecimalSafe(),
        minStock = minimumStock.toBigDecimalSafe(),
        priceBuy = priceBuy.toBigDecimalSafe(),
        priceRetail = priceRetail.toBigDecimalSafe(),
        priceContractor = priceContractor.toBigDecimalSafe(),
        discount = discount.toBigDecimalSafe(),
        isActive = isActive
    )
}

private fun Double.toBigDecimalSafe(): BigDecimal {
    return runCatching { BigDecimal.valueOf(this) }.getOrDefault(BigDecimal.ZERO)
}
