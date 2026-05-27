package com.tbterminal.app.data.remote

import java.math.BigDecimal
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface InventoryApi {
    @GET("/api/inventory/categories")
    suspend fun getCategories(): Response<ApiResponse<List<CategoryResponseDto>>>

    @GET("/api/inventory/categories")
    suspend fun getCategoriesPage(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("search") search: String? = null
    ): Response<ApiResponse<PaginatedResponse<CategoryResponseDto>>>

    @POST("/api/inventory/categories")
    suspend fun createCategory(
        @Body request: CategoryRequestDto
    ): Response<ApiResponse<CategoryResponseDto>>

    @PUT("/api/inventory/categories/{id}")
    suspend fun updateCategory(
        @Path("id") id: String,
        @Body request: CategoryRequestDto
    ): Response<ApiResponse<CategoryResponseDto>>

    @DELETE("/api/inventory/categories/{id}")
    suspend fun deleteCategory(
        @Path("id") id: String
    ): Response<ApiResponse<Unit>>

    @GET("/api/inventory/units")
    suspend fun getUnits(): Response<ApiResponse<List<UnitResponseDto>>>

    @GET("/api/inventory/units")
    suspend fun getUnitsPage(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("search") search: String? = null
    ): Response<ApiResponse<PaginatedResponse<UnitResponseDto>>>

    @POST("/api/inventory/units")
    suspend fun createUnit(
        @Body request: UnitRequestDto
    ): Response<ApiResponse<UnitResponseDto>>

    @PUT("/api/inventory/units/{id}")
    suspend fun updateUnit(
        @Path("id") id: String,
        @Body request: UnitRequestDto
    ): Response<ApiResponse<UnitResponseDto>>

    @DELETE("/api/inventory/units/{id}")
    suspend fun deleteUnit(
        @Path("id") id: String
    ): Response<ApiResponse<Unit>>

    @GET("/api/inventory/products")
    suspend fun getProducts(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("search") search: String? = null
    ): Response<ApiResponse<PaginatedResponse<ProductResponseDto>>>

    @GET("/api/inventory/products/{id}")
    suspend fun getProduct(
        @Path("id") id: String
    ): Response<ApiResponse<ProductResponseDto>>

    @POST("/api/inventory/products")
    suspend fun createProduct(
        @Body request: ProductCreateRequestDto
    ): Response<ApiResponse<ProductResponseDto>>

    @PUT("/api/inventory/products/{id}")
    suspend fun updateProduct(
        @Path("id") id: String,
        @Body request: ProductUpdateRequestDto
    ): Response<ApiResponse<ProductResponseDto>>

    @DELETE("/api/inventory/products/{id}")
    suspend fun deleteProduct(
        @Path("id") id: String
    ): Response<ApiResponse<Unit>>

    @PATCH("/api/inventory/products/{id}/activate")
    suspend fun activateProduct(
        @Path("id") id: String
    ): Response<ApiResponse<ProductResponseDto>>

    @GET("/api/inventory/stock")
    suspend fun getStockDetails(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("search") search: String? = null
    ): Response<ApiResponse<PaginatedResponse<StockDetailResponseDto>>>

    @GET("/api/inventory/stock/adjustments")
    suspend fun getStockAdjustments(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("search") search: String? = null,
        @Query("type") type: String? = null
    ): Response<ApiResponse<PaginatedResponse<StockAdjustmentResponseDto>>>

    @POST("/api/inventory/stock/opname")
    suspend fun executeStockOpname(
        @Body request: StockOpnameRequestDto
    ): Response<ApiResponse<Unit>>
}

@Serializable
data class CategoryResponseDto(
    val id: String,
    val name: String,
    val createdAt: String
)

@Serializable
data class CategoryRequestDto(
    val name: String
)

@Serializable
data class UnitResponseDto(
    val id: String,
    val name: String,
    val symbol: String,
    val createdAt: String
)

@Serializable
data class UnitRequestDto(
    val name: String,
    val symbol: String
)

@Serializable
data class ProductResponseDto(
    val id: String,
    val categoryId: String,
    val baseUnitId: String,
    val sku: String,
    val name: String,
    @Serializable(with = BigDecimalStringSerializer::class) val priceBuy: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val priceRetail: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val priceContractor: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val discount: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalStringSerializer::class) val minStock: BigDecimal,
    val photoFilename: String? = null,
    val isActive: Boolean
)

@Serializable
data class ProductCreateRequestDto(
    val categoryId: String,
    val baseUnitId: String,
    val sku: String,
    val name: String,
    @Serializable(with = BigDecimalStringSerializer::class) val priceBuy: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val priceRetail: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val priceContractor: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val discount: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalStringSerializer::class) val minStock: BigDecimal,
    val photoFilename: String? = null
)

@Serializable
data class ProductUpdateRequestDto(
    val categoryId: String,
    val baseUnitId: String,
    val name: String,
    @Serializable(with = BigDecimalStringSerializer::class) val priceBuy: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val priceRetail: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val priceContractor: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val discount: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalStringSerializer::class) val minStock: BigDecimal,
    val photoFilename: String? = null
)

@Serializable
data class StockDetailResponseDto(
    val productId: String,
    val sku: String,
    val productName: String,
    val categoryName: String,
    val unitName: String,
    @Serializable(with = BigDecimalStringSerializer::class) val quantity: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val minStock: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val priceBuy: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val priceRetail: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val priceContractor: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val discount: BigDecimal = BigDecimal.ZERO,
    val isActive: Boolean
)

@Serializable
data class StockAdjustmentResponseDto(
    val id: String,
    val productId: String,
    val sku: String,
    val productName: String,
    val categoryName: String,
    val unitName: String,
    val adjustmentType: String,
    val adjustmentTypeLabel: String,
    @Serializable(with = BigDecimalStringSerializer::class) val qtyBefore: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val qtyAfter: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val difference: BigDecimal,
    val reason: String,
    val userId: String,
    val createdAt: String
)

@Serializable
data class StockOpnameRequestDto(
    val productId: String,
    val adjustmentType: String,
    @Serializable(with = BigDecimalStringSerializer::class) val actualQty: BigDecimal,
    val notes: String? = null
)

object BigDecimalStringSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.math.BigDecimal", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BigDecimal {
        return decoder.decodeString().toBigDecimal()
    }

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.toPlainString())
    }
}
