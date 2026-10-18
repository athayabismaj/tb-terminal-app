package com.tbterminal.app.ui.purchasehistory

import com.tbterminal.app.data.model.PurchaseDetail
import com.tbterminal.app.data.model.PurchaseSummary
import com.tbterminal.app.data.model.Supplier
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

private val WIB_ZONE = ZoneId.of("Asia/Jakarta")

fun todayPurchaseDateString(): String = LocalDate.now(WIB_ZONE).toString()

data class PurchaseHistoryUiState(
    val purchases: List<PurchaseSummary> = emptyList(),
    val suppliers: List<Supplier> = emptyList(),
    val selectedSupplierId: String? = null,
    val startDate: String? = todayPurchaseDateString(),
    val endDate: String? = todayPurchaseDateString(),
    val selectedPeriodType: String = "Hari",
    val searchQuery: String = "",
    val selectedPurchase: PurchaseDetail? = null,
    val page: Int = 1,
    val totalPages: Int = 1,
    val totalPurchases: Long = 0,
    val pageSize: Int = 10,
    val isLoading: Boolean = true,
    val isLoadingDetail: Boolean = false,
    val errorMessage: String? = null
) {
    val visiblePurchases: List<PurchaseSummary>
        get() {
            val query = searchQuery.trim()
            val filteredByQuery = if (query.isBlank()) purchases else {
                purchases.filter { purchase ->
                    purchase.invoiceNo.orEmpty().contains(query, ignoreCase = true) ||
                        purchase.supplierName.contains(query, ignoreCase = true)
                }
            }
            val start = startDate?.take(10)
            val end = endDate?.take(10)
            if (start == null && end == null) return filteredByQuery

            return filteredByQuery.filter { purchase ->
                val purchaseDate = purchase.receivedAt.take(10)
                val matchesStart = start == null || purchaseDate >= start
                val matchesEnd = end == null || purchaseDate <= end
                matchesStart && matchesEnd
            }
        }

    val effectiveTotalPurchases: Long
        get() {
            if (visiblePurchases.isEmpty() || purchases.isEmpty()) return 0L
            if (visiblePurchases.size < purchases.size) {
                return visiblePurchases.size.toLong()
            }
            return totalPurchases
        }

    val pageTotal: BigDecimal
        get() = visiblePurchases.fold(BigDecimal.ZERO) { total, purchase -> total.add(purchase.total) }

    val pageStartIndex: Long
        get() = if (visiblePurchases.isEmpty() || effectiveTotalPurchases == 0L) 0L else ((page - 1L) * pageSize) + 1L

    val pageEndIndex: Long
        get() = if (visiblePurchases.isEmpty() || effectiveTotalPurchases == 0L) 0L else minOf(page.toLong() * pageSize, effectiveTotalPurchases)
}
