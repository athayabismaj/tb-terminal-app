package com.tbterminal.app.data.remote

import java.math.BigDecimal
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ReceivableApi {
    @GET("/api/receivable/customers")
    suspend fun getCustomers(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("search") search: String? = null
    ): Response<ApiResponse<PaginatedResponse<CustomerResponseDto>>>

    @GET("/api/receivable/customers/{id}")
    suspend fun getCustomerById(
        @Path("id") id: String
    ): Response<ApiResponse<CustomerResponseDto>>

    @POST("/api/receivable/customers")
    suspend fun createCustomer(
        @Body request: CustomerRequestDto
    ): Response<ApiResponse<CustomerResponseDto>>

    @PUT("/api/receivable/customers/{id}")
    suspend fun updateCustomer(
        @Path("id") id: String,
        @Body request: CustomerRequestDto
    ): Response<ApiResponse<CustomerResponseDto>>

    @DELETE("/api/receivable/customers/{id}")
    suspend fun deactivateCustomer(
        @Path("id") id: String
    ): Response<ApiResponse<Unit>>

    @GET("/api/receivable/receivables")
    suspend fun getReceivables(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("customerId") customerId: String? = null,
        @Query("status") status: String? = null,
        @Query("dueFilter") dueFilter: String? = null,
        @Query("dueFrom") dueFrom: String? = null,
        @Query("dueTo") dueTo: String? = null
    ): Response<ApiResponse<PaginatedResponse<ReceivableResponseDto>>>

    @POST("/api/receivable/receivables/opening-balance")
    suspend fun createOpeningBalance(
        @Body request: CreateStandaloneReceivableRequestDto
    ): Response<ApiResponse<ReceivableResponseDto>>

    @POST("/api/receivable/receivables/adjustment")
    suspend fun createAdjustment(
        @Body request: CreateStandaloneReceivableRequestDto
    ): Response<ApiResponse<ReceivableResponseDto>>

    @GET("/api/receivable/receivables/summary/customers")
    suspend fun getCustomerReceivableSummaries(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("dueFilter") dueFilter: String? = null
    ): Response<ApiResponse<PaginatedResponse<CustomerReceivableSummaryResponseDto>>>

    @GET("/api/receivable/receivables/{id}")
    suspend fun getReceivableById(
        @Path("id") id: String
    ): Response<ApiResponse<ReceivableResponseDto>>

    @POST("/api/receivable/payments")
    suspend fun createReceivablePayment(
        @Body request: ReceivablePaymentRequestDto
    ): Response<ApiResponse<ReceivablePaymentResponseDto>>

    @GET("/api/receivable/payments")
    suspend fun getReceivablePayments(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("receivableId") receivableId: String? = null,
        @Query("customerId") customerId: String? = null,
        @Query("method") method: String? = null,
        @Query("userId") userId: String? = null,
        @Query("customerSearch") customerSearch: String? = null,
        @Query("receiverSearch") receiverSearch: String? = null,
        @Query("status") status: String? = null,
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null
    ): Response<ApiResponse<PaginatedResponse<ReceivablePaymentHistoryResponseDto>>>

    @GET("/api/receivable/payments/{id}/receipt")
    suspend fun getReceivablePaymentReceipt(
        @Path("id") id: String
    ): Response<ApiResponse<ReceivablePaymentHistoryResponseDto>>

    @POST("/api/receivable/payments/{id}/reversal")
    suspend fun reverseReceivablePayment(
        @Path("id") id: String,
        @Body request: ReverseReceivablePaymentRequestDto
    ): Response<ApiResponse<ReceivablePaymentResponseDto>>
}

@Serializable
data class CustomerRequestDto(
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    val isContractor: Boolean = false,
    @Serializable(with = BigDecimalStringSerializer::class) val creditLimit: BigDecimal = BigDecimal.ZERO,
    val paymentTermDays: Int = 0
)

@Serializable
data class CustomerResponseDto(
    val id: String,
    val name: String,
    val phone: String? = null,
    val address: String? = null,
    val isContractor: Boolean,
    @Serializable(with = BigDecimalStringSerializer::class) val creditLimit: BigDecimal,
    val paymentTermDays: Int,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ReceivableResponseDto(
    val id: String,
    val customerId: String,
    val customerName: String,
    val transactionId: String? = null,
    val source: String = "SALE",
    @Serializable(with = BigDecimalStringSerializer::class) val amount: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val paidAmount: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val remainingAmount: BigDecimal,
    val debtDate: String,
    val dueDate: String,
    val status: String,
    val legacyInvoiceNumber: String? = null,
    val notes: String? = null,
    val createdBy: String,
    val isActive: Boolean = true,
    val createdAt: String
)

@Serializable
data class CreateStandaloneReceivableRequestDto(
    val customerId: String,
    @Serializable(with = BigDecimalStringSerializer::class) val amount: BigDecimal,
    val debtDate: String,
    val dueDate: String,
    val legacyInvoiceNumber: String? = null,
    val source: String = "OPENING_BALANCE",
    val notes: String? = null
)

@Serializable
data class CustomerReceivableSummaryResponseDto(
    val customerId: String,
    val customerName: String,
    @Serializable(with = BigDecimalStringSerializer::class) val totalAmount: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val totalPaid: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val totalRemaining: BigDecimal,
    val unpaidCount: Long,
    val overdueCount: Long,
    val nearestDueDate: String? = null
)

@Serializable
data class ReceivablePaymentRequestDto(
    val receivableId: String,
    @Serializable(with = BigDecimalStringSerializer::class) val amount: BigDecimal,
    val method: String,
    val reference: String? = null,
    val notes: String? = null,
    val idempotencyKey: String
)

@Serializable
data class ReverseReceivablePaymentRequestDto(
    val idempotencyKey: String,
    val reason: String
)

@Serializable
data class ReceivablePaymentResponseDto(
    val id: String,
    val paymentNumber: String,
    val receivableId: String,
    val customerId: String,
    val customerName: String,
    @Serializable(with = BigDecimalStringSerializer::class) val amount: BigDecimal,
    val method: String,
    val reference: String?,
    val notes: String?,
    val paidAt: String,
    val paymentDate: String,
    val entryType: String,
    val reversedPaymentId: String? = null,
    val receivedBy: String,
    val receivedByName: String,
    @Serializable(with = BigDecimalStringSerializer::class) val balanceBefore: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val balanceAfter: BigDecimal,
    val receivableStatus: String,
    @Serializable(with = BigDecimalStringSerializer::class) val receivableRemainingAmount: BigDecimal,
    val idempotentReplay: Boolean = false
)

@Serializable
data class ReceivablePaymentHistoryResponseDto(
    val id: String,
    val paymentNumber: String,
    val receivableId: String,
    val customerId: String,
    val customerName: String,
    val transactionId: String? = null,
    val source: String = "SALE",
    @Serializable(with = BigDecimalStringSerializer::class) val amount: BigDecimal,
    val method: String,
    val reference: String?,
    val notes: String?,
    val paidAt: String,
    val paymentDate: String,
    val entryType: String,
    val reversedPaymentId: String? = null,
    val receivedBy: String,
    val receivedByName: String,
    @Serializable(with = BigDecimalStringSerializer::class) val balanceBefore: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val balanceAfter: BigDecimal,
    val isReversed: Boolean = false,
    val receivableStatus: String,
    @Serializable(with = BigDecimalStringSerializer::class) val receivableRemainingAmount: BigDecimal
)
