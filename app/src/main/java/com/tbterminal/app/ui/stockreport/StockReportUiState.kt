package com.tbterminal.app.ui.stockreport

import com.tbterminal.app.data.model.ProductStock
import java.math.BigDecimal

data class StockReportUiState(
    val stocks: List<ProductStock> = emptyList(),
    val searchQuery: String = "",
    val page: Int = 1,
    val totalPages: Int = 1,
    val totalProducts: Long = 0,
    val pageSize: Int = 10,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val pageStockValue: BigDecimal
        get() = stocks.fold(BigDecimal.ZERO) { total, stock ->
            total.add(stock.quantity.multiply(stock.priceBuy))
        }

    val pageLowStockCount: Int
        get() = stocks.count { it.quantity > BigDecimal.ZERO && it.quantity <= it.minStock }

    val pageOutOfStockCount: Int
        get() = stocks.count { it.quantity <= BigDecimal.ZERO }
}
