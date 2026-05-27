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
}
