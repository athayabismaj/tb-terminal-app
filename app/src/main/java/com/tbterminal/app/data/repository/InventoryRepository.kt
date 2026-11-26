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
import com.tbterminal.app.data.model.ProductPriceTier
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
import com.tbterminal.app.data.remote.UpdateProductPriceTiersRequestDto
import com.tbterminal.app.data.remote.StockAdjustmentResponseDto
import com.tbterminal.app.data.remote.StockDetailResponseDto
import com.tbterminal.app.data.remote.StockOpnameRequestDto
import com.tbterminal.app.data.remote.UnitRequestDto
import com.tbterminal.app.data.remote.UnitResponseDto
import com.tbterminal.app.data.remote.ProductCsvImportRequestDto
import com.tbterminal.app.data.remote.OpeningStockRequestDto
import com.tbterminal.app.data.remote.NetworkResult
import com.tbterminal.app.data.remote.safeApiCall
import com.tbterminal.app.data.remote.safeUnitApiCall
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
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

    suspend fun getProductPriceTiers(productId: String): RepositoryResult<List<ProductPriceTier>> =
        RepositoryResult.Success(emptyList())

    suspend fun updateProductPriceTiers(
        productId: String,
        tiers: List<ProductPriceTier>
    ): RepositoryResult<List<ProductPriceTier>> = RepositoryResult.Success(tiers)

    suspend fun uploadProductPhoto(
        productId: String,
        bytes: ByteArray,
        fileName: String = "product.jpg"
    ): RepositoryResult<String> = RepositoryResult.Error("UNSUPPORTED", "Upload foto produk tidak didukung.")

    suspend fun deleteProductPhoto(productId: String): RepositoryResult<Unit> = RepositoryResult.Success(Unit)

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
        val normalizedSearch = search?.takeIf(String::isNotBlank)
        val result = safeApiCall {
            inventoryApi.getStockDetails(page = page, limit = limit, search = normalizedSearch)
        }.toRepositoryResult { response ->
            val stockPage = response.data
            if (!response.success || stockPage == null) {
                RepositoryResult.Error(
                    code = response.code ?: "PRODUCT_STOCKS_FAILED",
                    message = response.message ?: response.error ?: "Daftar produk gagal dimuat."
                )
            } else {
                cacheProductStocks(stockPage.data)
                RepositoryResult.Success(
                    enrichProductStockPage(
                        stockPage = stockPage,
                        page = page,
                        limit = limit,
                        search = normalizedSearch
                    )
                )
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
            secondaryUnitId = command.secondaryUnitId,
            secondaryUnitFactor = command.secondaryUnitFactor?.stripTrailingZeros()?.toPlainString(),
            priceTiers = command.priceTiers?.map(ProductPriceTier::toDto)
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
            secondaryUnitId = command.secondaryUnitId,
            secondaryUnitFactor = command.secondaryUnitFactor?.stripTrailingZeros()?.toPlainString(),
            priceTiers = command.priceTiers?.map(ProductPriceTier::toDto)
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

    override suspend fun uploadProductPhoto(
        productId: String,
        bytes: ByteArray,
        fileName: String
    ): RepositoryResult<String> {
        val body = bytes.toRequestBody("image/jpeg".toMediaType())
        val part = MultipartBody.Part.createFormData("photo", fileName, body)
        return safeApiCall { inventoryApi.uploadProductPhoto(productId, part) }
            .toRepositoryResult { response ->
                val uploaded = response.data
                if (!response.success || uploaded == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "UPLOAD_PRODUCT_PHOTO_FAILED",
                        message = response.message ?: response.error ?: "Foto produk gagal diunggah."
                    )
                } else {
                    cacheSafely { productLocalDataSource?.updateProductPhoto(productId, uploaded.photoUrl) }
                    RepositoryResult.Success(uploaded.photoUrl)
                }
            }
    }

    override suspend fun deleteProductPhoto(productId: String): RepositoryResult<Unit> {
        return safeUnitApiCall { inventoryApi.deleteProductPhoto(productId) }
            .toRepositoryResult {
                cacheSafely { productLocalDataSource?.updateProductPhoto(productId, null) }
                RepositoryResult.Success(Unit)
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

    override suspend fun getProductPriceTiers(productId: String): RepositoryResult<List<ProductPriceTier>> {
        return safeApiCall { inventoryApi.getProductPriceTiers(productId) }
            .toRepositoryResult { response ->
                val data = response.data
                if (!response.success || data == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "GET_PRICE_TIERS_FAILED",
                        message = response.message ?: response.error ?: "Harga grosir gagal dimuat."
                    )
                } else {
                    RepositoryResult.Success(data.map { it.toProductPriceTier() })
                }
            }
    }

    override suspend fun updateProductPriceTiers(
        productId: String,
        tiers: List<ProductPriceTier>
    ): RepositoryResult<List<ProductPriceTier>> {
        val request = UpdateProductPriceTiersRequestDto(tiers = tiers.map(ProductPriceTier::toDto))
        return safeApiCall { inventoryApi.updateProductPriceTiers(productId, request) }
            .toRepositoryResult { response ->
                val data = response.data
                if (!response.success || data == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "UPDATE_PRICE_TIERS_FAILED",
                        message = response.message ?: response.error ?: "Harga grosir gagal disimpan."
                    )
                } else {
                    RepositoryResult.Success(data.map { it.toProductPriceTier() })
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

    private suspend fun enrichProductStockPage(
        stockPage: PaginatedResponse<StockDetailResponseDto>,
        page: Int,
        limit: Int,
        search: String?
    ): ProductStockPage {
        val basePage = stockPage.toProductStockPage()
        if (stockPage.data.isEmpty()) return basePage

        // Backend v1.0.7 omits baseUnitId on /stock; fall back to /products and /units.
        // Backend 5b includes baseUnitId authoritatively, so skip /products and treat null secondaryUnitId as none.
        val needsProductMetadata = stockPage.data.any { dto ->
            dto.baseUnitId.isNullOrBlank() ||
                (!dto.secondaryUnitId.isNullOrBlank() && dto.secondaryUnitFactor.isNullOrBlank())
        }
        val productsById = if (needsProductMetadata) {
            fetchAndCacheProductsSafely(page = page, limit = limit, search = search)
        } else {
            emptyMap()
        }
        val needsLegacyFallbackCache = stockPage.data.any { it.baseUnitId.isNullOrBlank() }
        val cachedById = if (needsLegacyFallbackCache) {
            loadCachedProductsByServerId(stockPage.data.map { it.productId })
        } else {
            emptyMap()
        }

        val unitIdsNeeded = buildSet {
            stockPage.data.forEach { dto ->
                val hasAuthoritativeUnits = !dto.baseUnitId.isNullOrBlank()
                if (hasAuthoritativeUnits) {
                    val secUnitId = dto.secondaryUnitId?.trim()?.takeIf(String::isNotBlank)
                    if (secUnitId != null && dto.secondaryUnitName.isNullOrBlank()) {
                        add(secUnitId)
                    }
                    val baseUnitId = dto.baseUnitId?.trim()?.takeIf(String::isNotBlank)
                    if (baseUnitId != null && dto.baseUnitName.isNullOrBlank() && dto.unitName.isBlank()) {
                        add(baseUnitId)
                    }
                } else {
                    val remote = productsById[dto.productId]
                    val cached = cachedById[dto.productId]
                    val secUnitId = dto.secondaryUnitId?.trim()?.takeIf(String::isNotBlank)
                        ?: if (remote != null) remote.secondaryUnitId?.trim()?.takeIf(String::isNotBlank)
                        else cached?.secondaryUnitServerId?.trim()?.takeIf(String::isNotBlank)
                    if (secUnitId != null && dto.secondaryUnitName.isNullOrBlank()) {
                        add(secUnitId)
                    }
                    val baseUnitId = dto.baseUnitId?.trim()?.takeIf(String::isNotBlank)
                        ?: remote?.baseUnitId?.trim()?.takeIf(String::isNotBlank)
                        ?: cached?.unitServerId?.trim()?.takeIf(String::isNotBlank)
                    if (baseUnitId != null && dto.unitName.isBlank()) {
                        add(baseUnitId)
                    }
                }
            }
        }
        val unitNameById = resolveUnitNamesSafely(unitIdsNeeded)
        if (unitNameById.isNotEmpty()) {
            cacheSafely {
                productLocalDataSource?.applyUnitNames(
                    unitNameByServerId = unitNameById,
                    productServerIds = stockPage.data.map { it.productId }
                )
            }
        }

        val enrichedItems = basePage.data.mapIndexed { index, item ->
            val dto = stockPage.data[index]
            val remote = productsById[item.productId]
            if (!dto.baseUnitId.isNullOrBlank()) {
                val resolvedBaseUnitId = item.baseUnitId
                val resolvedUnitName = item.unitName.takeIf(String::isNotBlank)
                    ?: resolvedBaseUnitId?.let(unitNameById::get)
                    ?: ""
                val resolvedSecondaryUnitId = item.secondaryUnitId
                val resolvedSecondaryFactor = if (resolvedSecondaryUnitId != null) {
                    item.secondaryUnitFactor
                        ?: remote?.secondaryUnitFactor?.trim()?.toBigDecimalOrNull()
                } else {
                    null
                }
                val resolvedSecondaryUnitName = if (resolvedSecondaryUnitId != null) {
                    item.secondaryUnitName ?: unitNameById[resolvedSecondaryUnitId]
                } else {
                    null
                }
                item.copy(
                    unitName = resolvedUnitName,
                    baseUnitId = resolvedBaseUnitId,
                    secondaryUnitId = resolvedSecondaryUnitId,
                    secondaryUnitName = resolvedSecondaryUnitName,
                    secondaryUnitFactor = resolvedSecondaryFactor
                )
            } else {
                val cached = cachedById[item.productId]
                val resolvedBaseUnitId = item.baseUnitId?.takeIf(String::isNotBlank)
                    ?: remote?.baseUnitId?.trim()?.takeIf(String::isNotBlank)
                    ?: cached?.unitServerId?.trim()?.takeIf(String::isNotBlank)
                val resolvedSecondaryUnitId = item.secondaryUnitId?.takeIf(String::isNotBlank)
                    ?: if (remote != null) remote.secondaryUnitId?.trim()?.takeIf(String::isNotBlank)
                    else cached?.secondaryUnitServerId?.trim()?.takeIf(String::isNotBlank)
                val resolvedSecondaryFactor = item.secondaryUnitFactor
                    ?: if (remote != null) remote.secondaryUnitFactor?.trim()?.toBigDecimalOrNull()
                    else cached?.secondaryUnitFactor
                val resolvedSecondaryUnitName = item.secondaryUnitName?.takeIf(String::isNotBlank)
                    ?: resolvedSecondaryUnitId?.let { unitId ->
                        unitNameById[unitId]
                            ?: cached?.takeIf { it.secondaryUnitServerId == unitId }?.secondaryUnitName
                    }
                item.copy(
                    baseUnitId = resolvedBaseUnitId,
                    secondaryUnitId = resolvedSecondaryUnitId,
                    secondaryUnitName = resolvedSecondaryUnitName,
                    secondaryUnitFactor = resolvedSecondaryFactor
                )
            }
        }
        return basePage.copy(data = enrichedItems)
    }

    private suspend fun fetchAndCacheProductsSafely(
        page: Int,
        limit: Int,
        search: String?
    ): Map<String, ProductResponseDto> {
        var productsById = emptyMap<String, ProductResponseDto>()
        cacheSafely {
            val result = safeApiCall {
                inventoryApi.getProducts(page = page, limit = limit, search = search)
            }
            if (result is NetworkResult.Success) {
                val productPage = result.data.data
                if (result.data.success && productPage != null) {
                    cacheProducts(productPage.data)
                    productsById = productPage.data.associateBy { it.id }
                }
            }
        }
        return productsById
    }

    private suspend fun loadCachedProductsByServerId(
        productIds: List<String>
    ): Map<String, LocalProductEntity> {
        val source = productLocalDataSource ?: return emptyMap()
        val result = mutableMapOf<String, LocalProductEntity>()
        cacheSafely {
            productIds.distinct().forEach { id ->
                source.getCachedProductByServerId(id)?.let { result[id] = it }
            }
        }
        return result
    }

    private suspend fun resolveUnitNamesSafely(unitIdsNeeded: Set<String>): Map<String, String> {
        if (unitIdsNeeded.isEmpty()) return emptyMap()
        val unitNames = loadCachedUnitNamesSafely().toMutableMap()
        if (unitIdsNeeded.any { it !in unitNames }) {
            cacheSafely {
                val result = safeApiCall { inventoryApi.getUnits() }
                if (result is NetworkResult.Success) {
                    val units = result.data.data
                    if (result.data.success && units != null) {
                        cacheUnits(units)
                        units.forEach { unit ->
                            val label = unit.name.trim().takeIf(String::isNotBlank)
                                ?: unit.symbol.trim().takeIf(String::isNotBlank)
                            if (label != null) {
                                unitNames[unit.id] = label
                            }
                        }
                    }
                }
            }
        }
        return unitNames
    }

    private suspend fun loadCachedUnitNamesSafely(): Map<String, String> {
        val source = unitLocalDataSource ?: return emptyMap()
        val map = mutableMapOf<String, String>()
        cacheSafely {
            source.getCachedUnits().forEach { unit ->
                val serverId = unit.serverId?.trim()?.takeIf(String::isNotBlank) ?: return@forEach
                val label = unit.name.trim().takeIf(String::isNotBlank)
                    ?: unit.symbol.trim().takeIf(String::isNotBlank)
                    ?: return@forEach
                map[serverId] = label
            }
        }
        return map
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
            val unitNameById = loadCachedUnitNamesSafely()
            RepositoryResult.Success(
                ProductStockPage(
                    data = cached.data.map { it.toCachedProductStock(unitNameById) },
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

private fun LocalProductEntity.toCachedProductStock(
    unitNameById: Map<String, String> = emptyMap()
): ProductStock {
    val resolvedBaseUnitId = unitServerId?.trim()?.takeIf(String::isNotBlank)
    val resolvedUnitName = unitName?.trim()?.takeIf(String::isNotBlank)
        ?: resolvedBaseUnitId?.let(unitNameById::get)
        ?: ""
    val resolvedSecondaryUnitId = secondaryUnitServerId?.trim()?.takeIf(String::isNotBlank)
    val resolvedSecondaryUnitName = secondaryUnitName?.trim()?.takeIf(String::isNotBlank)
        ?: resolvedSecondaryUnitId?.let(unitNameById::get)
    return ProductStock(
        productId = serverId.orEmpty(),
        sku = sku.orEmpty(),
        productName = name,
        categoryName = "",
        unitName = resolvedUnitName,
        quantity = stock.toBigDecimalSafe(),
        minStock = minimumStock.toBigDecimalSafe(),
        priceBuy = priceBuy.toBigDecimalSafe(),
        priceRetail = priceRetail.toBigDecimalSafe(),
        priceContractor = priceContractor.toBigDecimalSafe(),
        discount = discount.toBigDecimalSafe(),
        isActive = isActive,
        secondaryUnitId = resolvedSecondaryUnitId,
        secondaryUnitName = resolvedSecondaryUnitName,
        secondaryUnitFactor = secondaryUnitFactor,
        photoUrl = photoFilename,
        baseUnitId = resolvedBaseUnitId
    )
}

private fun Double.toBigDecimalSafe(): BigDecimal {
    return runCatching { BigDecimal.valueOf(this) }.getOrDefault(BigDecimal.ZERO)
}
