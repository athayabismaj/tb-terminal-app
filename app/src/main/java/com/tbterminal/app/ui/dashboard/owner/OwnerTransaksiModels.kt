package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.runtime.Immutable
import kotlin.math.abs

enum class TransaksiTab {
    PENJUALAN,
    PEMBELIAN
}

enum class TransaksiItemKind {
    SALE,
    PURCHASE
}

@Immutable
data class TransaksiSummary(
    val label: String,
    val periodChip: String,
    val amount: Long,
    val footerLabel: String,
    val footerValue: String
)

@Immutable
data class TransaksiItemUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: Long,
    val kind: TransaksiItemKind,
    val unpaid: Boolean = false
)

@Immutable
data class TransaksiUiState(
    val userName: String = "Pemilik Toko",
    val synced: Boolean = true,
    val tab: TransaksiTab = TransaksiTab.PENJUALAN,
    val balanceHidden: Boolean = false,
    val sales: TransaksiSummary = TransaksiSummary(
        label = "Ringkasan penjualan",
        periodChip = "Hari ini",
        amount = 0L,
        footerLabel = "Total transaksi",
        footerValue = "0 struk"
    ),
    val purchases: TransaksiSummary = TransaksiSummary(
        label = "Total pembelian bulan ini",
        periodChip = "Bulan berjalan",
        amount = 0L,
        footerLabel = "Total faktur masuk",
        footerValue = "0 faktur"
    ),
    val salesItems: List<TransaksiItemUi> = emptyList(),
    val purchaseItems: List<TransaksiItemUi> = emptyList(),
    val loading: Boolean = false,
    val selectedNav: DashboardNavDestination = DashboardNavDestination.TRANSAKSI,
    val error: String? = null
)

/**
 * Pemformat Rupiah buatan sendiri ("Rp 2.450.000") dengan pemisah ribuan titik
 * yang tidak bergantung pada Locale perangkat.
 */
internal fun formatRupiahIdr(amount: Long): String {
    val negative = amount < 0L
    val digits = abs(amount).toString()
    val grouped = buildString(digits.length + digits.length / 3) {
        val len = digits.length
        for (i in 0 until len) {
            append(digits[i])
            val remaining = len - 1 - i
            if (remaining > 0 && remaining % 3 == 0) {
                append('.')
            }
        }
    }
    return if (negative) "-Rp $grouped" else "Rp $grouped"
}

internal fun sampleReferenceTransaksiState(
    tab: TransaksiTab = TransaksiTab.PENJUALAN,
    isTablet: Boolean = false
): TransaksiUiState {
    val allSales = listOf(
        TransaksiItemUi("s1", "Penjualan #0124", "10:42 · Tunai", 185_000L, TransaksiItemKind.SALE),
        TransaksiItemUi("s2", "Penjualan #0123", "09:58 · QRIS", 62_500L, TransaksiItemKind.SALE),
        TransaksiItemUi("s3", "Penjualan #0122", "09:15 · Tunai", 340_000L, TransaksiItemKind.SALE),
        TransaksiItemUi("s4", "Penjualan #0121", "08:47 · Tunai", 128_000L, TransaksiItemKind.SALE),
        TransaksiItemUi("s5", "Penjualan #0120", "08:21 · QRIS", 95_500L, TransaksiItemKind.SALE),
        TransaksiItemUi("s6", "Penjualan #0119", "08:05 · Tunai", 47_000L, TransaksiItemKind.SALE),
        TransaksiItemUi("s7", "Penjualan #0118", "07:52 · Tunai", 210_000L, TransaksiItemKind.SALE),
        TransaksiItemUi("s8", "Penjualan #0117", "07:34 · QRIS", 76_500L, TransaksiItemKind.SALE)
    )
    val allPurchases = listOf(
        TransaksiItemUi("p1", "Faktur #0031", "18 Nov · Lunas", 1_250_000L, TransaksiItemKind.PURCHASE),
        TransaksiItemUi("p2", "Faktur #0030", "16 Nov · Lunas", 860_000L, TransaksiItemKind.PURCHASE),
        TransaksiItemUi(
            "p3",
            "Faktur #0029",
            "14 Nov · Belum lunas",
            2_100_000L,
            TransaksiItemKind.PURCHASE,
            unpaid = true
        ),
        TransaksiItemUi("p4", "Faktur #0028", "11 Nov · Lunas", 590_000L, TransaksiItemKind.PURCHASE),
        TransaksiItemUi("p5", "Faktur #0027", "8 Nov · Lunas", 1_480_000L, TransaksiItemKind.PURCHASE),
        TransaksiItemUi("p6", "Faktur #0026", "3 Nov · Lunas", 520_000L, TransaksiItemKind.PURCHASE)
    )
    return TransaksiUiState(
        tab = tab,
        balanceHidden = false,
        sales = TransaksiSummary(
            label = "Ringkasan penjualan",
            periodChip = "Hari ini",
            amount = 2_450_000L,
            footerLabel = "Total transaksi",
            footerValue = "24 struk"
        ),
        purchases = TransaksiSummary(
            label = "Total pembelian bulan ini",
            periodChip = "Bulan berjalan",
            amount = 6_800_000L,
            footerLabel = "Total faktur masuk",
            footerValue = "6 faktur"
        ),
        salesItems = if (isTablet) allSales else allSales.take(4),
        purchaseItems = if (isTablet) allPurchases else allPurchases.take(4),
        loading = false,
        selectedNav = DashboardNavDestination.TRANSAKSI
    )
}
