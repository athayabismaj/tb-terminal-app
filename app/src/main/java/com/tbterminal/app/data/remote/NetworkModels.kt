package com.tbterminal.app.data.remote

import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class ApiErrorResponse(
    val code: String,
    val message: String,
    val details: String? = null
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class UnlockRequest(
    val pin: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val user: UserDto
)

@Serializable
data class UserDto(
    val id: String,
    val username: String,
    val name: String,
    val role: String,
    val isActive: Boolean,
    val joinedAt: String,
    val lastLoginAt: String? = null
)

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val error: String? = null,
    val code: String? = null
)

@Serializable
data class StockLowSummaryDto(
    val productId: String,
    val sku: String,
    val productName: String,
    val quantity: Double,
    val minStock: Double
)

@Serializable
data class DashboardMetricsDto(
    val totalRevenueToday: Double,
    val totalRevenueThisMonth: Double,
    val totalActiveReceivables: Double,
    val activeReceivableCount: Long,
    val lowStockCount: Long,
    val lowStockItems: List<StockLowSummaryDto>
)

@Serializable
data class DailySalesSummaryDto(
    val date: String,
    val transactionCount: Long,
    val totalRevenue: Double,
    val totalDp: Double
)

@Serializable
data class SalesReportResponseDto(
    val range: SalesReportRangeDto,
    val totals: SalesReportTotalsDto,
    val paymentMethods: List<PaymentMethodSummaryDto> = emptyList(),
    val transactionStatuses: List<TransactionStatusSummaryDto> = emptyList(),
    val topProducts: List<TopProductSalesDto> = emptyList(),
    val cashiers: List<CashierSalesSummaryDto> = emptyList(),
    val receivables: SalesReceivableSummaryDto
)

@Serializable
data class SalesReportRangeDto(
    val startDate: String,
    val endDate: String
)

@Serializable
data class SalesReportTotalsDto(
    val transactionCount: Long,
    @Serializable(with = BigDecimalStringSerializer::class) val grossRevenue: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val paidAmount: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val outstandingAmount: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val grossProfit: BigDecimal
)

@Serializable
data class PaymentMethodSummaryDto(
    val method: String,
    val paymentCount: Long,
    @Serializable(with = BigDecimalStringSerializer::class) val amount: BigDecimal
)

@Serializable
data class TransactionStatusSummaryDto(
    val status: String,
    val transactionCount: Long,
    @Serializable(with = BigDecimalStringSerializer::class) val revenue: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val paidAmount: BigDecimal
)

@Serializable
data class TopProductSalesDto(
    val productId: String,
    val productName: String,
    @Serializable(with = BigDecimalStringSerializer::class) val qtySold: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val revenue: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val grossProfit: BigDecimal
)

@Serializable
data class CashierSalesSummaryDto(
    val userId: String,
    val cashierName: String,
    val transactionCount: Long,
    @Serializable(with = BigDecimalStringSerializer::class) val revenue: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val grossProfit: BigDecimal
)

@Serializable
data class SalesReceivableSummaryDto(
    @Serializable(with = BigDecimalStringSerializer::class) val createdReceivableAmount: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val paidAmount: BigDecimal,
    @Serializable(with = BigDecimalStringSerializer::class) val remainingAmount: BigDecimal,
    val receivableCount: Long
)
