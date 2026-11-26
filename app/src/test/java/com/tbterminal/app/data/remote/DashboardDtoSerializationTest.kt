package com.tbterminal.app.data.remote

import java.math.BigDecimal
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardDtoSerializationTest {
    private val networkJson = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun parsesDashboardMetricsAndStockLowStringsWithoutDoublePrecisionLoss() {
        val rawJson = """
            {
              "totalRevenueToday": "9999999999999999.99",
              "totalRevenueThisMonth": "12345678901234.56",
              "totalActiveReceivables": "1250000.75",
              "activeReceivableCount": 14,
              "lowStockCount": 2,
              "lowStockItems": [
                {
                  "productId": "prod-1",
                  "sku": "PASIR-01",
                  "productName": "Pasir Bangka",
                  "quantity": "2.50",
                  "minStock": "10.25"
                }
              ]
            }
        """.trimIndent()

        val dto = networkJson.decodeFromString<DashboardMetricsDto>(rawJson)

        assertEquals(0, BigDecimal("9999999999999999.99").compareTo(dto.totalRevenueToday))
        assertEquals("9999999999999999.99", dto.totalRevenueToday.toPlainString())
        assertEquals(0, BigDecimal("12345678901234.56").compareTo(dto.totalRevenueThisMonth))
        assertEquals("12345678901234.56", dto.totalRevenueThisMonth.toPlainString())
        assertEquals(0, BigDecimal("1250000.75").compareTo(dto.totalActiveReceivables))
        assertEquals("1250000.75", dto.totalActiveReceivables.toPlainString())

        val item = dto.lowStockItems.single()
        assertEquals(0, BigDecimal("2.50").compareTo(item.quantity))
        assertEquals("2.50", item.quantity.toPlainString())
        assertEquals(0, BigDecimal("10.25").compareTo(item.minStock))
        assertEquals("10.25", item.minStock.toPlainString())
    }

    @Test
    fun parsesDailySalesSummaryStringsWithExplicitAndOmittedVoidedAmount() {
        val explicitVoidJson = """
            {
              "date": "2026-11-26",
              "transactionCount": 19,
              "totalRevenue": "4567890.25",
              "totalDp": "350000.50",
              "voidedTransactionCount": 2,
              "voidedAmount": "150000.50"
            }
        """.trimIndent()

        val explicitDto = networkJson.decodeFromString<DailySalesSummaryDto>(explicitVoidJson)
        assertEquals(0, BigDecimal("4567890.25").compareTo(explicitDto.totalRevenue))
        assertEquals("4567890.25", explicitDto.totalRevenue.toPlainString())
        assertEquals(0, BigDecimal("350000.50").compareTo(explicitDto.totalDp))
        assertEquals("350000.50", explicitDto.totalDp.toPlainString())
        assertEquals(0, BigDecimal("150000.50").compareTo(explicitDto.voidedAmount))
        assertEquals("150000.50", explicitDto.voidedAmount.toPlainString())

        val omittedVoidJson = """
            {
              "date": "2026-11-25",
              "transactionCount": 8,
              "totalRevenue": "2100000",
              "totalDp": "0"
            }
        """.trimIndent()

        val omittedDto = networkJson.decodeFromString<DailySalesSummaryDto>(omittedVoidJson)
        assertEquals(0, BigDecimal("2100000").compareTo(omittedDto.totalRevenue))
        assertEquals("2100000", omittedDto.totalRevenue.toPlainString())
        assertEquals(0, BigDecimal.ZERO.compareTo(omittedDto.totalDp))
        assertEquals(0, BigDecimal.ZERO.compareTo(omittedDto.voidedAmount))
        assertEquals("0", omittedDto.voidedAmount.toPlainString())
    }
}
