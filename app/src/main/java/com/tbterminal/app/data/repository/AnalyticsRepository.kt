package com.tbterminal.app.data.repository

import com.tbterminal.app.data.remote.AnalyticsApi
import com.tbterminal.app.data.remote.DashboardMetricsDto
import com.tbterminal.app.data.remote.SalesReportResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

data class AnalyticsFilter(
    val startDate: String,
    val endDate: String,
    val cashierId: String? = null,
    val customerId: String? = null,
    val productId: String? = null,
    val categoryId: String? = null,
    val paymentMethod: String? = null,
    val status: String? = null
)

enum class ReportCsvType(val path: String, val label: String) {
    TRANSACTIONS("transactions", "Transaksi"),
    SALES_DETAILS("sales-details", "Detail penjualan"),
    STOCK("stock", "Stok"),
    STOCK_CARD("stock-card", "Kartu stok"),
    RECEIVABLES("receivables", "Piutang"),
    PAYMENTS("payments", "Pembayaran")
}

interface AnalyticsRepository {
    suspend fun getDashboardMetrics(): DashboardMetricsDto
    suspend fun getDailySales(startDate: String?, endDate: String?): List<com.tbterminal.app.data.remote.DailySalesSummaryDto>
    suspend fun getSalesReport(
        startDate: String?,
        endDate: String?,
        cashierId: String? = null,
        sessionId: String? = null,
        customerId: String? = null,
        productId: String? = null,
        categoryId: String? = null,
        paymentMethod: String? = null,
        status: String? = null,
        topProductsLimit: Int = 10
    ): SalesReportResponseDto
    suspend fun exportCsv(type: ReportCsvType, filter: AnalyticsFilter, output: OutputStream)
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
        customerId: String?,
        productId: String?,
        categoryId: String?,
        paymentMethod: String?,
        status: String?,
        topProductsLimit: Int
    ): SalesReportResponseDto = withContext(Dispatchers.IO) {
        try {
            val response = api.getSalesReport(
                startDate = startDate,
                endDate = endDate,
                cashierId = cashierId,
                sessionId = sessionId,
                customerId = customerId,
                productId = productId,
                categoryId = categoryId,
                paymentMethod = paymentMethod,
                status = status,
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

    override suspend fun exportCsv(type: ReportCsvType, filter: AnalyticsFilter, output: OutputStream) = withContext(Dispatchers.IO) {
        val response = api.exportCsv(
            type = type.path,
            startDate = filter.startDate,
            endDate = filter.endDate,
            cashierId = filter.cashierId,
            customerId = filter.customerId,
            productId = filter.productId,
            categoryId = filter.categoryId,
            paymentMethod = filter.paymentMethod,
            status = filter.status
        )
        if (!response.isSuccessful) throw Exception("Ekspor gagal: server merespons ${response.code()}")
        val body = response.body() ?: throw Exception("Ekspor gagal: respons server kosong")
        body.byteStream().use { input -> output.use { input.copyTo(it) } }
        Unit
    }
}
