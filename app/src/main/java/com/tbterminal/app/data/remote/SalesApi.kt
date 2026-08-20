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
    @GET("/api/sales/sessions")
    suspend fun getSessions(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("status") status: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<ApiResponse<PaginatedResponse<CashSessionResponseDto>>>

    @GET("/api/sales/sessions/active")
    suspend fun getActiveSession(): Response<ApiResponse<CashSessionResponseDto?>>

    @GET("/api/sales/sessions/{sessionId}")
    suspend fun getSessionById(
        @Path("sessionId") sessionId: String
    ): Response<ApiResponse<CashSessionResponseDto>>

    @POST("/api/sales/sessions/open")
    suspend fun openSession(
        @Body request: OpenSessionRequestDto
    ): Response<ApiResponse<CashSessionResponseDto>>

    @POST("/api/sales/sessions/sync/open")
    suspend fun syncOpenCashSession(
        @Body request: OfflineCashSessionOpenSyncRequestDto
    ): Response<ApiResponse<OfflineCashSessionOpenSyncResponseDto>>

    @POST("/api/sales/sessions/sync/close")
    suspend fun syncCloseCashSession(
        @Body request: OfflineCashSessionCloseSyncRequestDto
    ): Response<ApiResponse<OfflineCashSessionCloseSyncResponseDto>>

    @POST("/api/sales/sessions/close")
    suspend fun closeSession(
        @Body request: CloseSessionRequestDto
    ): Response<ApiResponse<CashSessionResponseDto>>

    @POST("/api/sales/sessions/expenses")
    suspend fun addExpense(
        @Body request: CashExpenseRequestDto
    ): Response<ApiResponse<CashExpenseResponseDto>>

    @GET("/api/sales/sessions/expenses")
    suspend fun getExpenseHistory(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("sessionId") sessionId: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<ApiResponse<PaginatedResponse<CashExpenseResponseDto>>>

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
        @Query("receiptNumber") receiptNumber: String? = null,
        @Query("cashierId") cashierId: String? = null,
        @Query("customerId") customerId: String? = null,
        @Query("paymentMethod") paymentMethod: String? = null,
        @Query("status") status: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<ApiResponse<PaginatedResponse<SalesTransactionSummaryDto>>>

    @GET("/api/sales/transactions/{id}")
    suspend fun getTransactionById(
        @Path("id") id: String
    ): Response<ApiResponse<TransactionDetailDto>>

    @POST("/api/sales/transactions/{id}/void")
    suspend fun voidTransaction(
        @Path("id") id: String,
        @Body request: VoidTransactionRequestDto
    ): Response<ApiResponse<VoidTransactionResponseDto>>

    @POST("/api/sales/checkout/sync")
    suspend fun syncOfflineCheckout(
        @Body request: OfflineCheckoutSyncRequestDto
    ): Response<ApiResponse<OfflineCheckoutSyncResponseDto>>

    @POST("/api/sales/cash-expenses/sync")
    suspend fun syncCashExpense(
        @Body request: OfflineCashExpenseSyncRequestDto
    ): Response<ApiResponse<OfflineCashExpenseSyncResponseDto>>
}

@Serializable
data class OpenSessionRequestDto(
    @Serializable(with = BigDecimalStringSerializer::class) val startingCash: BigDecimal
)

@Serializable
data class OfflineCashSessionOpenSyncRequestDto(
    val clientGeneratedId: String,
    val deviceId: String,
    val cashierUserId: String,
    val openedAt: String,
    @Serializable(with = BigDecimalStringSerializer::class) val startingCash: BigDecimal,
    val openingNote: String? = null
)

@Serializable
data class OfflineCashSessionOpenSyncResponseDto(
    val syncStatus: String,
    val serverCashSessionId: String,
    val openedAt: String,
    val syncedAt: String
)

@Serializable
data class OfflineCashSessionCloseSyncRequestDto(
    val deviceId: String,
    val clientGeneratedId: String,
    val serverCashSessionId: String,
    val cashierUserId: String,
    val closedAt: String,
    @Serializable(with = BigDecimalStringSerializer::class) val actualCash: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val expectedCash: BigDecimal? = null,
    @Serializable(with = BigDecimalStringSerializer::class) val difference: BigDecimal? = null,
    val closingNote: String? = null
)

@Serializable
data class OfflineCashSessionCloseSyncResponseDto(
    val syncStatus: String,
    val serverCashSessionId: String,
    val closedAt: String,
    val syncedAt: String
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
    val userName: String? = null,
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
    val cashierId: String = "",
    val cashierName: String? = null,
    val paymentMethods: List<String> = emptyList(),
    val type: String,
    val status: String,
    @Serializable(with = BigDecimalStringSerializer::class) val total: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val paidAmount: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val remainingAmount: BigDecimal? = null,
    val createdAt: String,
    val voidedAt: String? = null,
    val voidReason: String? = null
)

@Serializable
data class TransactionDetailDto(
    val id: String,
    val receiptId: String? = null,
    val sessionId: String,
    val customerId: String?,
    val customerName: String? = null,
    val userId: String = "",
    val cashierName: String? = null,
    val paymentMethods: List<String> = emptyList(),
    val type: String,
    val status: String,
    @Serializable(with = BigDecimalStringSerializer::class) val total: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val paidAmount: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val amountTendered: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalStringSerializer::class) val changeAmount: BigDecimal = BigDecimal.ZERO,
    val createdAt: String,
    val voidedAt: String? = null,
    val voidedBy: String? = null,
    val voidedByName: String? = null,
    val voidReason: String? = null,
    val items: List<TransactionItemDto>
)

@Serializable
data class VoidTransactionRequestDto(val idempotencyKey: String, val reason: String)

@Serializable
data class VoidTransactionResponseDto(
    val voidId: String,
    val transactionId: String,
    val receiptId: String,
    val status: String,
    val reason: String,
    val voidedBy: String,
    val voidedByName: String? = null,
    val voidedAt: String,
    val idempotentReplay: Boolean = false
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
    val userName: String? = null,
    @Serializable(with = BigDecimalStringSerializer::class) val amount: BigDecimal,
    val description: String,
    val createdAt: String
)

@Serializable
data class OfflineCashExpenseSyncRequestDto(
    val clientGeneratedId: String,
    val deviceId: String,
    val cashierUserId: String,
    val serverCashSessionId: String,
    @Serializable(with = BigDecimalStringSerializer::class) val amount: BigDecimal,
    val category: String,
    val note: String,
    val occurredAt: String
)

@Serializable
data class OfflineCashExpenseSyncResponseDto(
    val syncStatus: String,
    val serverExpenseId: String,
    val syncedAt: String
)

@Serializable
data class PayDebtRequestDto(
    @Serializable(with = BigDecimalStringSerializer::class) val amount: BigDecimal,
    val method: String // "tunai", "transfer", "qris"
)

@Serializable
data class OfflineCheckoutSyncItemRequestDto(
    val productId: String,
    val productNameSnapshot: String,
    @Serializable(with = BigDecimalStringSerializer::class) val quantity: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val priceAtTransaction: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val cogsAtTransaction: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val discount: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalStringSerializer::class) val subtotal: BigDecimal
)

@Serializable
data class OfflineCheckoutSyncRequestDto(
    val clientGeneratedId: String,
    val deviceId: String,
    val localTransactionCode: String,
    val cashierUserId: String,
    val cashSessionId: String,
    val customerId: String? = null,
    val paymentMethod: String,
    @Serializable(with = BigDecimalStringSerializer::class) val subtotal: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val discount: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalStringSerializer::class) val total: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val paidAmount: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val remainingAmount: BigDecimal,
    val occurredAt: String,
    val note: String? = null,
    val dueDays: Int = 30,
    val items: List<OfflineCheckoutSyncItemRequestDto>
)

@Serializable
data class OfflineCheckoutSyncResponseDto(
    val syncStatus: String,
    val serverTransactionId: String,
    val receiptId: String,
    val serverPaymentIds: List<String> = emptyList(),
    val serverReceivableId: String? = null,
    val syncedAt: String
)
