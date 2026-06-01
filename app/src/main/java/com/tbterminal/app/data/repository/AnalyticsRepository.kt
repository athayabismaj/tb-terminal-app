package com.tbterminal.app.data.repository

import com.tbterminal.app.data.remote.AnalyticsApi
import com.tbterminal.app.data.remote.DashboardMetricsDto
import com.tbterminal.app.data.remote.SalesReportResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface AnalyticsRepository {
    suspend fun getDashboardMetrics(): DashboardMetricsDto
    suspend fun getDailySales(startDate: String?, endDate: String?): List<com.tbterminal.app.data.remote.DailySalesSummaryDto>
    suspend fun getSalesReport(
        startDate: String?,
        endDate: String?,
        cashierId: String? = null,
        sessionId: String? = null,
        topProductsLimit: Int = 10
    ): SalesReportResponseDto
}

class RemoteAnalyticsRepository(
    private val api: AnalyticsApi
) : AnalyticsRepository {
    override suspend fun getDashboardMetrics(): DashboardMetricsDto = withContext(Dispatchers.IO) {
        try {
            val response = api.getDashboardMetrics()
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    return@withContext body.data
                }
                throw Exception(body?.message ?: "Gagal mengambil data dashboard")
            } else {
                throw Exception("Gagal terhubung ke server: ${response.code()}")
            }
        } catch (e: Exception) {
            throw Exception("Terjadi kesalahan: ${e.message}")
        }
    }

    override suspend fun getDailySales(startDate: String?, endDate: String?): List<com.tbterminal.app.data.remote.DailySalesSummaryDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.getDailySales(startDate, endDate)
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    return@withContext body.data
                }
                throw Exception(body?.message ?: "Gagal mengambil data sales")
            } else {
                throw Exception("Gagal terhubung ke server: ${response.code()}")
            }
        } catch (e: Exception) {
            throw Exception("Terjadi kesalahan: ${e.message}")
        }
    }

    override suspend fun getSalesReport(
        startDate: String?,
        endDate: String?,
        cashierId: String?,
        sessionId: String?,
        topProductsLimit: Int
    ): SalesReportResponseDto = withContext(Dispatchers.IO) {
        try {
            val response = api.getSalesReport(
                startDate = startDate,
                endDate = endDate,
                cashierId = cashierId,
                sessionId = sessionId,
                topProductsLimit = topProductsLimit
            )
            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.data != null) {
                    return@withContext body.data
                }
                throw Exception(body?.message ?: "Gagal mengambil laporan penjualan agregat")
            } else {
                throw Exception("Gagal terhubung ke server: ${response.code()}")
            }
        } catch (e: Exception) {
            throw Exception("Terjadi kesalahan: ${e.message}")
        }
    }
}
