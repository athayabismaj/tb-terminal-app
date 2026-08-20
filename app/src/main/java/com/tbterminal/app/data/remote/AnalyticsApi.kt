package com.tbterminal.app.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import okhttp3.ResponseBody

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
        @retrofit2.http.Query("customerId") customerId: String? = null,
        @retrofit2.http.Query("productId") productId: String? = null,
        @retrofit2.http.Query("categoryId") categoryId: String? = null,
        @retrofit2.http.Query("paymentMethod") paymentMethod: String? = null,
        @retrofit2.http.Query("status") status: String? = null,
        @retrofit2.http.Query("topProductsLimit") topProductsLimit: Int = 10
    ): Response<ApiResponse<SalesReportResponseDto>>

    @Streaming
    @GET("api/analytics/exports/{type}.csv")
    suspend fun exportCsv(
        @Path("type") type: String,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("cashierId") cashierId: String? = null,
        @Query("customerId") customerId: String? = null,
        @Query("productId") productId: String? = null,
        @Query("categoryId") categoryId: String? = null,
        @Query("paymentMethod") paymentMethod: String? = null,
        @Query("status") status: String? = null
    ): Response<ResponseBody>
}
