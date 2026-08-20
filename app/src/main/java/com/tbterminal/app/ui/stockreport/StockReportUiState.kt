package com.tbterminal.app.ui.stockreport

import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.model.StockMovement
import java.math.BigDecimal

data class StockReportUiState(
    val stocks: List<ProductStock> = emptyList(),
    val searchQuery: String = "",
    val categoryFilter: String? = null,
    val page: Int = 1,
    val totalPages: Int = 1,
    val totalProducts: Long = 0,
    val pageSize: Int = 10,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val selectedProductId: String? = null,
    val stockMovements: List<StockMovement> = emptyList(),
    val isCardLoading: Boolean = false,
    val cardErrorMessage: String? = null,
    val cardReconciled: Boolean? = null
) {
    val categoryOptions: List<String>
        get() = stocks.map(ProductStock::categoryName).distinct().sorted()

    val visibleStocks: List<ProductStock>
        get() = stocks.filter { stock ->
            categoryFilter == null || stock.categoryName == categoryFilter
        }

    val pageStockValue: BigDecimal
        get() = visibleStocks.fold(BigDecimal.ZERO) { total, stock ->
            total.add(stock.quantity.multiply(stock.priceBuy))
        }

    val pageLowStockCount: Int
        get() = visibleStocks.count { it.quantity > BigDecimal.ZERO && it.quantity <= it.minStock }

    val pageOutOfStockCount: Int
        get() = visibleStocks.count { it.quantity <= BigDecimal.ZERO }
}
