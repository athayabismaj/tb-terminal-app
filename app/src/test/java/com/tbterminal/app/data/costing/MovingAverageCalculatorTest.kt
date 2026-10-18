package com.tbterminal.app.data.costing

import com.tbterminal.app.data.model.ProductStock
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class MovingAverageCalculatorTest {

    @Test
    fun calculateNewCogs_whenExistingStockZero_returnsIncomingPrice() {
        val newCogs = MovingAverageCalculator.calculateNewCogs(
            existingStock = BigDecimal.ZERO,
            existingCogs = BigDecimal.ZERO,
            incomingQuantity = BigDecimal("50"),
            incomingPrice = BigDecimal("25000"),
            scale = 0
        )
        assertEquals(BigDecimal("25000"), newCogs)
    }

    @Test
    fun calculateNewCogs_whenExistingStockNegative_returnsIncomingPrice() {
        val newCogs = MovingAverageCalculator.calculateNewCogs(
            existingStock = BigDecimal("-5"),
            existingCogs = BigDecimal("20000"),
            incomingQuantity = BigDecimal("20"),
            incomingPrice = BigDecimal("22000"),
            scale = 0
        )
        assertEquals(BigDecimal("22000"), newCogs)
    }

    @Test
    fun calculateNewCogs_whenIncomingQuantityZero_returnsExistingCogs() {
        val newCogs = MovingAverageCalculator.calculateNewCogs(
            existingStock = BigDecimal("100"),
            existingCogs = BigDecimal("15000"),
            incomingQuantity = BigDecimal.ZERO,
            incomingPrice = BigDecimal("20000"),
            scale = 0
        )
        assertEquals(BigDecimal("15000"), newCogs)
    }

    @Test
    fun calculateNewCogs_withPriceIncrease_calculatesCorrectWeightedAverage() {
        // Stok lama: 100 @ Rp 10.000 = Rp 1.000.000
        // Restok: 50 @ Rp 13.000 = Rp 650.000
        // Total: 150 unit = Rp 1.650.000 -> HPP baru = Rp 11.000
        val newCogs = MovingAverageCalculator.calculateNewCogs(
            existingStock = BigDecimal("100"),
            existingCogs = BigDecimal("10000"),
            incomingQuantity = BigDecimal("50"),
            incomingPrice = BigDecimal("13000"),
            scale = 0
        )
        assertEquals(BigDecimal("11000"), newCogs)
    }

    @Test
    fun calculateNewCogs_withPriceDecrease_calculatesCorrectWeightedAverage() {
        // Stok lama: 40 @ Rp 15.000 = Rp 600.000
        // Restok: 60 @ Rp 10.000 = Rp 600.000
        // Total: 100 unit = Rp 1.200.000 -> HPP baru = Rp 12.000
        val newCogs = MovingAverageCalculator.calculateNewCogs(
            existingStock = BigDecimal("40"),
            existingCogs = BigDecimal("15000"),
            incomingQuantity = BigDecimal("60"),
            incomingPrice = BigDecimal("10000"),
            scale = 0
        )
        assertEquals(BigDecimal("12000"), newCogs)
    }

    @Test
    fun calculateDetails_returnsCompleteMetricsAndValuations() {
        val result = MovingAverageCalculator.calculateDetails(
            existingStock = BigDecimal("100"),
            existingCogs = BigDecimal("10000"),
            incomingQuantity = BigDecimal("50"),
            incomingPrice = BigDecimal("13000"),
            scale = 0
        )

        assertEquals(BigDecimal("150"), result.newStock)
        assertEquals(BigDecimal("11000"), result.newCogs)
        assertEquals(BigDecimal("1000"), result.cogsDifference)
        assertEquals(BigDecimal("10.00"), result.percentChange)
        assertEquals(BigDecimal("1000000"), result.existingValuation)
        assertEquals(BigDecimal("650000"), result.incomingValuation)
        assertEquals(BigDecimal("1650000"), result.newValuation)
    }

    @Test
    fun calculateInventoryValuation_aggregatesOnlyActiveProducts() {
        val products = listOf(
            ProductStock(
                productId = "prod-1",
                sku = "SKU-1",
                productName = "Semen Tiga Roda",
                categoryName = "Semen",
                unitName = "Sak",
                quantity = BigDecimal("10"),
                minStock = BigDecimal("2"),
                priceBuy = BigDecimal("50000"),
                priceRetail = BigDecimal("65000"),
                priceContractor = BigDecimal("60000"),
                discount = BigDecimal.ZERO,
                isActive = true
            ),
            ProductStock(
                productId = "prod-2",
                sku = "SKU-2",
                productName = "Pasir Beton",
                categoryName = "Pasir",
                unitName = "M3",
                quantity = BigDecimal("20"),
                minStock = BigDecimal("5"),
                priceBuy = BigDecimal("150000"),
                priceRetail = BigDecimal("200000"),
                priceContractor = BigDecimal("180000"),
                discount = BigDecimal.ZERO,
                isActive = true
            ),
            ProductStock(
                productId = "prod-3",
                sku = "SKU-3",
                productName = "Bata Ringan (Nonaktif)",
                categoryName = "Bata",
                unitName = "Pcs",
                quantity = BigDecimal("1000"),
                minStock = BigDecimal("100"),
                priceBuy = BigDecimal("7000"),
                priceRetail = BigDecimal("9000"),
                priceContractor = BigDecimal("8500"),
                discount = BigDecimal.ZERO,
                isActive = false
            )
        )

        val valuation = MovingAverageCalculator.calculateInventoryValuation(products)

        // 10 * 50.000 = 500.000
        // 20 * 150.000 = 3.000.000
        // Total valuation = 3.500.000
        // Total stock units = 30
        // Active count = 2
        // Average valuation per product = 1.750.000
        assertEquals(2, valuation.totalActiveProducts)
        assertEquals(BigDecimal("30"), valuation.totalStockUnits)
        assertEquals(BigDecimal("3500000"), valuation.totalValuation)
        assertEquals(BigDecimal("1750000"), valuation.averageValuationPerProduct)
    }

    @Test
    fun calculateGrossProfitAndMargin_computesAccurateResults() {
        val subtotal = BigDecimal("120000")
        val cogs = BigDecimal("10000")
        val quantity = BigDecimal("10")

        val profit = MovingAverageCalculator.calculateGrossProfit(
            subtotal = subtotal,
            cogsAtTransaction = cogs,
            quantity = quantity
        )
        // Profit = 120.000 - (10.000 * 10) = 20.000
        assertEquals(BigDecimal("20000"), profit)

        val margin = MovingAverageCalculator.calculateGrossMarginPercent(
            grossProfit = profit,
            subtotal = subtotal
        )
        // Margin = (20.000 / 120.000) * 100 = 16.67%
        assertEquals(BigDecimal("16.67"), margin)
    }
}
