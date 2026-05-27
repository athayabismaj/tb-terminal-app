package com.tbterminal.app.data.remote

import kotlinx.serialization.Serializable

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
