package com.tbterminal.app.data.remote

import retrofit2.Response
import retrofit2.http.GET

interface AnalyticsApi {
    @GET("api/analytics/dashboard")
    suspend fun getDashboardMetrics(): Response<ApiResponse<DashboardMetricsDto>>

    @GET("api/analytics/sales")
    suspend fun getDailySales(
        @retrofit2.http.Query("startDate") startDate: String?,
        @retrofit2.http.Query("endDate") endDate: String?
    ): Response<ApiResponse<List<DailySalesSummaryDto>>>

    @GET("api/analytics/sales/report")
    suspend fun getSalesReport(
        @retrofit2.http.Query("startDate") startDate: String?,
        @retrofit2.http.Query("endDate") endDate: String?,
        @retrofit2.http.Query("cashierId") cashierId: String? = null,
        @retrofit2.http.Query("sessionId") sessionId: String? = null,
        @retrofit2.http.Query("topProductsLimit") topProductsLimit: Int = 10
    ): Response<ApiResponse<SalesReportResponseDto>>
}
