package com.tbterminal.app.data.remote

import java.math.BigDecimal
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SalesApi {
    @GET("/api/sales/sessions/active")
    suspend fun getActiveSession(): Response<ApiResponse<CashSessionResponseDto?>>

    @POST("/api/sales/sessions/open")
    suspend fun openSession(
        @Body request: OpenSessionRequestDto
    ): Response<ApiResponse<CashSessionResponseDto>>

    @POST("/api/sales/sessions/close")
    suspend fun closeSession(
        @Body request: CloseSessionRequestDto
    ): Response<ApiResponse<CashSessionResponseDto>>

    @POST("/api/sales/sessions/expenses")
    suspend fun addExpense(
        @Body request: CashExpenseRequestDto
    ): Response<ApiResponse<CashExpenseResponseDto>>

    @GET("/api/sales/sessions/{sessionId}/expenses")
    suspend fun getExpenses(
        @Path("sessionId") sessionId: String
    ): Response<ApiResponse<List<CashExpenseResponseDto>>>

    @POST("/api/sales/transactions/{id}/pay")
    suspend fun payTransactionDebt(
        @Path("id") id: String,
        @Body request: PayDebtRequestDto
    ): Response<ApiResponse<TransactionDetailDto>>

    @GET("/api/sales/transactions")
    suspend fun getTransactions(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("sessionId") sessionId: String? = null,
        @Query("search") search: String? = null,
        @Query("status") status: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<ApiResponse<PaginatedResponse<SalesTransactionSummaryDto>>>

    @GET("/api/sales/transactions/{id}")
    suspend fun getTransactionById(
        @Path("id") id: String
    ): Response<ApiResponse<TransactionDetailDto>>
}

@Serializable
data class OpenSessionRequestDto(
    @Serializable(with = BigDecimalStringSerializer::class) val startingCash: BigDecimal
)

@Serializable
data class CloseSessionRequestDto(
    @Serializable(with = BigDecimalStringSerializer::class) val endingCashPhysical: BigDecimal,
    val notes: String? = null
)

@Serializable
data class CashSessionResponseDto(
    val id: String,
    val userId: String,
    val openedAt: String,
    val closedAt: String?,
    @Serializable(with = BigDecimalStringSerializer::class) val openingCash: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val closingCash: BigDecimal?,
    @Serializable(with = BigDecimalStringSerializer::class) val systemCash: BigDecimal?,
    @Serializable(with = BigDecimalStringSerializer::class) val difference: BigDecimal?,
    @Serializable(with = BigDecimalStringSerializer::class) val totalExpenses: BigDecimal = BigDecimal.ZERO,
    val notes: String?,
    val status: String
)

@Serializable
data class SalesTransactionSummaryDto(
    val id: String,
    val receiptId: String? = null,
    val sessionId: String,
    val customerId: String?,
    val customerName: String? = null,
    val type: String,
    val status: String,
    @Serializable(with = BigDecimalStringSerializer::class) val total: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val paidAmount: BigDecimal,
    val createdAt: String
)

@Serializable
data class TransactionDetailDto(
    val id: String,
    val receiptId: String? = null,
    val sessionId: String,
    val customerId: String?,
    val customerName: String? = null,
    val type: String,
    val status: String,
    @Serializable(with = BigDecimalStringSerializer::class) val total: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val paidAmount: BigDecimal,
    val createdAt: String,
    val items: List<TransactionItemDto>
)

@Serializable
data class TransactionItemDto(
    val productId: String,
    val productName: String,
    val unitId: String,
    @Serializable(with = BigDecimalStringSerializer::class) val quantity: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val priceAtTransaction: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val subtotal: BigDecimal
)

@Serializable
data class CashExpenseRequestDto(
    @Serializable(with = BigDecimalStringSerializer::class) val amount: BigDecimal,
    val description: String
)

@Serializable
data class CashExpenseResponseDto(
    val id: String,
    val sessionId: String,
    val userId: String,
    @Serializable(with = BigDecimalStringSerializer::class) val amount: BigDecimal,
    val description: String,
    val createdAt: String
)

@Serializable
data class PayDebtRequestDto(
    @Serializable(with = BigDecimalStringSerializer::class) val amount: BigDecimal,
    val method: String // "tunai", "transfer", "qris"
)
