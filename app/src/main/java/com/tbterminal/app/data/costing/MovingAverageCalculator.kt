package com.tbterminal.app.data.costing

import com.tbterminal.app.data.model.ProductStock
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Utility dan kalkulator penetapan nilai persediaan menggunakan metode
 * Weighted Moving Average Cost (MAC / HPP Rata-rata Bergerak).
 *
 * Standar Akuntansi Persediaan (Inventory Valuation):
 * HPP Baru = ((Stok Lama * HPP Lama) + (Qty Masuk * Harga Beli Masuk)) / (Stok Lama + Qty Masuk)
 * Jika Stok Lama <= 0: HPP Baru = Harga Beli Masuk
 */
object MovingAverageCalculator {

    /**
     * Menghitung HPP Rata-Rata Bergerak baru.
     */
    fun calculateNewCogs(
        existingStock: BigDecimal,
        existingCogs: BigDecimal,
        incomingQuantity: BigDecimal,
        incomingPrice: BigDecimal,
        scale: Int = 2
    ): BigDecimal {
        if (incomingQuantity <= BigDecimal.ZERO) {
            return existingCogs.setScale(scale, RoundingMode.HALF_UP)
        }

        // Jika stok lama kosong atau negatif (akibat overselling sebelum restok tercatat),
        // maka HPP baru sepenuhnya ditentukan oleh batch pembelian baru.
        if (existingStock <= BigDecimal.ZERO) {
            return incomingPrice.setScale(scale, RoundingMode.HALF_UP)
        }

        val existingValuation = existingStock.multiply(existingCogs)
        val incomingValuation = incomingQuantity.multiply(incomingPrice)
        val totalValuation = existingValuation.add(incomingValuation)
        val totalQuantity = existingStock.add(incomingQuantity)

        return totalValuation.divide(totalQuantity, scale, RoundingMode.HALF_UP)
    }

    /**
     * Menghitung kalkulasi lengkap HPP Bergerak beserta selisih, persentase,
     * dan valuasi sebelum vs sesudah restok.
     */
    fun calculateDetails(
        existingStock: BigDecimal,
        existingCogs: BigDecimal,
        incomingQuantity: BigDecimal,
        incomingPrice: BigDecimal,
        scale: Int = 0
    ): MovingAverageResult {
        val safeStock = existingStock.coerceAtLeast(BigDecimal.ZERO)
        val newStock = safeStock.add(incomingQuantity.coerceAtLeast(BigDecimal.ZERO))
        val newCogs = calculateNewCogs(
            existingStock = existingStock,
            existingCogs = existingCogs,
            incomingQuantity = incomingQuantity,
            incomingPrice = incomingPrice,
            scale = scale
        )

        val cogsDifference = newCogs.subtract(existingCogs)
        val percentChange = if (existingCogs > BigDecimal.ZERO) {
            cogsDifference.multiply(BigDecimal(100)).divide(existingCogs, 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        val existingValuation = safeStock.multiply(existingCogs)
        val incomingValuation = incomingQuantity.coerceAtLeast(BigDecimal.ZERO).multiply(incomingPrice)
        val newValuation = newStock.multiply(newCogs)

        return MovingAverageResult(
            existingStock = existingStock,
            existingCogs = existingCogs,
            incomingQuantity = incomingQuantity,
            incomingPrice = incomingPrice,
            newStock = newStock,
            newCogs = newCogs,
            cogsDifference = cogsDifference,
            percentChange = percentChange,
            existingValuation = existingValuation,
            incomingValuation = incomingValuation,
            newValuation = newValuation
        )
    }

    /**
     * Menghitung total valuasi persediaan dari daftar produk aktif.
     */
    fun calculateInventoryValuation(products: List<ProductStock>): InventoryValuationSummary {
        var totalStockUnits = BigDecimal.ZERO
        var totalValuation = BigDecimal.ZERO
        var activeCount = 0

        for (product in products) {
            if (product.isActive) {
                val stock = product.quantity.coerceAtLeast(BigDecimal.ZERO)
                totalStockUnits = totalStockUnits.add(stock)
                totalValuation = totalValuation.add(stock.multiply(product.priceBuy))
                activeCount++
            }
        }

        val averageValuation = if (activeCount > 0) {
            totalValuation.divide(BigDecimal(activeCount), 0, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        return InventoryValuationSummary(
            totalActiveProducts = activeCount,
            totalStockUnits = totalStockUnits,
            totalValuation = totalValuation,
            averageValuationPerProduct = averageValuation
        )
    }

    /**
     * Menghitung laba kotor transaksi per item.
     * Laba Kotor = Subtotal Net - (HPP * Kuantitas)
     */
    fun calculateGrossProfit(
        subtotal: BigDecimal,
        cogsAtTransaction: BigDecimal,
        quantity: BigDecimal
    ): BigDecimal {
        val totalCost = cogsAtTransaction.multiply(quantity)
        return subtotal.subtract(totalCost)
    }

    /**
     * Menghitung persentase gross margin.
     * Margin % = (Laba Kotor / Subtotal Net) * 100
     */
    fun calculateGrossMarginPercent(
        grossProfit: BigDecimal,
        subtotal: BigDecimal
    ): BigDecimal {
        if (subtotal <= BigDecimal.ZERO) return BigDecimal.ZERO
        return grossProfit.multiply(BigDecimal(100)).divide(subtotal, 2, RoundingMode.HALF_UP)
    }
}

/**
 * Data class hasil perhitungan Moving Average Cost.
 */
data class MovingAverageResult(
    val existingStock: BigDecimal,
    val existingCogs: BigDecimal,
    val incomingQuantity: BigDecimal,
    val incomingPrice: BigDecimal,
    val newStock: BigDecimal,
    val newCogs: BigDecimal,
    val cogsDifference: BigDecimal,
    val percentChange: BigDecimal,
    val existingValuation: BigDecimal,
    val incomingValuation: BigDecimal,
    val newValuation: BigDecimal
)

/**
 * Ringkasan valuasi total persediaan toko.
 */
data class InventoryValuationSummary(
    val totalActiveProducts: Int,
    val totalStockUnits: BigDecimal,
    val totalValuation: BigDecimal,
    val averageValuationPerProduct: BigDecimal
)
