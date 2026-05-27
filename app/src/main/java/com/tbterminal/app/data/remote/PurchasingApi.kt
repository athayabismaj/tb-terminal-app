package com.tbterminal.app.data.remote

import java.math.BigDecimal
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query

interface PurchasingApi {
    @GET("/api/purchasing/suppliers")
    suspend fun getSuppliers(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("search") search: String? = null
    ): Response<ApiResponse<PaginatedResponse<SupplierResponseDto>>>

    @POST("/api/purchasing/suppliers")
    suspend fun createSupplier(
        @Body request: SupplierRequestDto
    ): Response<ApiResponse<SupplierResponseDto>>

    @POST("/api/purchasing/purchases")
    suspend fun createPurchase(
        @Body request: PurchaseRequestDto
    ): Response<ApiResponse<PurchaseResponseDto>>

    @GET("/api/purchasing/payables")
    suspend fun getPayables(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("supplierId") supplierId: String? = null,
        @Query("status") status: String? = null
    ): Response<ApiResponse<PaginatedResponse<PayableResponseDto>>>

    @GET("/api/purchasing/payables/{id}")
    suspend fun getPayableById(
        @Path("id") id: String
    ): Response<ApiResponse<PayableResponseDto>>

    @POST("/api/purchasing/payables/payments")
    suspend fun createSupplierPayment(
        @Body request: SupplierPaymentRequestDto
    ): Response<ApiResponse<SupplierPaymentResponseDto>>
}

@Serializable
data class SupplierRequestDto(
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    val paymentTermDays: Int = 30
)

@Serializable
data class SupplierResponseDto(
    val id: String,
    val name: String,
    val phone: String?,
    val address: String?,
    val paymentTermDays: Int,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class PurchaseItemRequestDto(
    val productId: String,
    @Serializable(with = BigDecimalStringSerializer::class) val qty: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val price: BigDecimal
)

@Serializable
data class PurchaseRequestDto(
    val supplierId: String,
    val invoiceNo: String? = null,
    val paymentMethod: String,
    @Serializable(with = BigDecimalStringSerializer::class) val amountPaid: BigDecimal,
    val notes: String? = null,
    val dueDays: Int = 30,
    val items: List<PurchaseItemRequestDto>
)

@Serializable
data class PurchaseResponseDto(
    val id: String,
    val supplierId: String,
    val supplierName: String,
    val userId: String,
    val invoiceNo: String?,
    @Serializable(with = BigDecimalStringSerializer::class) val total: BigDecimal,
    val notes: String?,
    val receivedAt: String,
    val createdAt: String,
    val items: List<PurchaseItemResponseDto>
)

@Serializable
data class PurchaseItemResponseDto(
    val productId: String,
    val productName: String,
    val unitId: String,
    @Serializable(with = BigDecimalStringSerializer::class) val quantity: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val priceAtTransaction: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val cogsAtTransaction: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val subtotal: BigDecimal
)

@Serializable
data class PayableResponseDto(
    val id: String,
    val supplierId: String,
    val supplierName: String,
    val purchaseId: String,
    @Serializable(with = BigDecimalStringSerializer::class) val amount: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val paidAmount: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val remainingAmount: BigDecimal,
    val dueDate: String,
    val status: String,
    val createdAt: String
)

@Serializable
data class SupplierPaymentRequestDto(
    val payableId: String,
    @Serializable(with = BigDecimalStringSerializer::class) val amount: BigDecimal,
    val method: String,
    val reference: String? = null,
    val notes: String? = null
)

@Serializable
data class SupplierPaymentResponseDto(
    val id: String,
    val payableId: String,
    @Serializable(with = BigDecimalStringSerializer::class) val amount: BigDecimal,
    val method: String,
    val reference: String?,
    val notes: String?,
    val paidAt: String,
    val payableStatus: String,
    @Serializable(with = BigDecimalStringSerializer::class) val payableRemainingAmount: BigDecimal
)
