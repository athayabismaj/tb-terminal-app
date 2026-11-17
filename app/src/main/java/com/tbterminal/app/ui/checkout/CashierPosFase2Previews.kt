package com.tbterminal.app.ui.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PrintDisabled
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.checkout.components.ProductCardGrid
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.math.BigDecimal

private val sampleProducts = listOf(
    Product(
        productId = "P01",
        name = "Semen Gresik PPC 40kg",
        unitPrice = BigDecimal("65000"),
        unitName = "Sak",
        categoryName = "Semen",
        stockQty = BigDecimal("150")
    ),
    Product(
        productId = "P02",
        name = "Besi Beton Polos 10mm SNI",
        unitPrice = BigDecimal("72000"),
        unitName = "Btg",
        categoryName = "Besi",
        stockQty = BigDecimal("85")
    ),
    Product(
        productId = "P03",
        name = "Cat Tembok Avitex White 5kg",
        unitPrice = BigDecimal("115000"),
        unitName = "Pail",
        categoryName = "Cat",
        stockQty = BigDecimal("30")
    ),
    Product(
        productId = "P04",
        name = "Thinner Super High Gloss Cobra 1L",
        unitPrice = BigDecimal("32000"),
        unitName = "Klg",
        categoryName = "Cat",
        stockQty = BigDecimal("45")
    ),
    Product(
        productId = "P05",
        name = "Paku Kayu 5cm (2 Inci)",
        unitPrice = BigDecimal("18000"),
        unitName = "Kg",
        categoryName = "Alat",
        stockQty = BigDecimal("200")
    ),
    Product(
        productId = "P06",
        name = "Bata Ringan Hebel Standard 10cm",
        unitPrice = BigDecimal("12500"),
        unitName = "Pcs",
        categoryName = "Batu & Bata",
        stockQty = BigDecimal("0") // Habis
    )
)

private val sampleCartItems = listOf(
    CartItem(
        productId = "P01",
        productName = "Semen Gresik PPC 40kg",
        unitPrice = BigDecimal("65000"),
        quantity = 2,
        unitName = "Sak"
    ),
    CartItem(
        productId = "P04",
        productName = "Thinner Super High Gloss Cobra 1L",
        unitPrice = BigDecimal("32000"),
        quantity = 1,
        unitName = "Klg"
    )
)

@Preview(name = "Fase 2 - POS Keranjang Kosong (Normal Online)", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun PreviewPosScreenEmptyCart() {
    TbterminalappTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF8FAF9)
        ) {
            CashierPosScreen(
                state = CheckoutUiState(
                    products = sampleProducts,
                    cartItems = emptyList()
                ),
                onSearchChanged = {},
                onRefreshProducts = {},
                onProductClick = {},
                onCustomerSearchChanged = {},
                onSelectCustomer = {},
                onClearCustomer = {},
                onUseCustomerName = {},
                onRefreshCustomers = {},
                onUpdateQty = { _, _ -> },
                onSelectPayment = {},
                onAmountPaidChanged = {},
                onStartingCashChanged = {},
                onOpenCashSession = {},
                onRefreshCashSession = {},
                onCheckout = {},
                onNavigateToCart = {}
            )
        }
    }
}

@Preview(name = "Fase 2 - POS Keranjang Berisi (Floating Cart Dock 52dp)", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
fun PreviewPosScreenWithCart() {
    TbterminalappTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF8FAF9)
        ) {
            CashierPosScreen(
                state = CheckoutUiState(
                    products = sampleProducts,
                    cartItems = sampleCartItems
                ),
                onSearchChanged = {},
                onRefreshProducts = {},
                onProductClick = {},
                onCustomerSearchChanged = {},
                onSelectCustomer = {},
                onClearCustomer = {},
                onUseCustomerName = {},
                onRefreshCustomers = {},
                onUpdateQty = { _, _ -> },
                onSelectPayment = {},
                onAmountPaidChanged = {},
                onStartingCashChanged = {},
                onOpenCashSession = {},
                onRefreshCashSession = {},
                onCheckout = {},
                onNavigateToCart = {}
            )
        }
    }
}

@Preview(name = "Fase 2 - Banner Tipis Alert (Semua Varian)", showBackground = true, widthDp = 390)
@Composable
fun PreviewPosThinAlertBanners() {
    TbterminalappTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8FAF9))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Varian 1: Offline dengan antrean sync
            Surface(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    androidx.compose.material3.Text(
                        text = "Varian 1: Offline + 4 Belum Sinkron",
                        fontSize = 11.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                    // Render PosThinAlertBanner via CashierPosScreen preview helper
                    PosThinAlertBannerPreviewHelper(
                        isOnline = false,
                        pendingSyncCount = 4,
                        isPrinterReady = true
                    )
                }
            }

            // Varian 2: Online tapi ada transaksi antrean sync
            Surface(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    androidx.compose.material3.Text(
                        text = "Varian 2: Online + 2 Menunggu Sinkronisasi",
                        fontSize = 11.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                    PosThinAlertBannerPreviewHelper(
                        isOnline = true,
                        pendingSyncCount = 2,
                        isPrinterReady = true
                    )
                }
            }

            // Varian 3: Printer terputus / belum dikonfigurasi
            Surface(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    androidx.compose.material3.Text(
                        text = "Varian 3: Printer Struk Belum Terhubung",
                        fontSize = 11.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                    PosThinAlertBannerPreviewHelper(
                        isOnline = true,
                        pendingSyncCount = 0,
                        isPrinterReady = false
                    )
                }
            }
        }
    }
}

@Composable
private fun PosThinAlertBannerPreviewHelper(
    isOnline: Boolean,
    pendingSyncCount: Int,
    isPrinterReady: Boolean
) {
    val style = when {
        !isOnline -> {
            val pendingInfo = if (pendingSyncCount > 0) " • $pendingSyncCount belum sinkron" else ""
            AlertBannerStylePreview(
                bg = Color(0xFFFEF3C7),
                border = Color(0xFFFDE68A),
                text = Color(0xFFB45309),
                icon = Icons.Outlined.WifiOff,
                msg = "Mode Offline$pendingInfo"
            )
        }
        pendingSyncCount > 0 -> {
            AlertBannerStylePreview(
                bg = Color(0xFFEFF6FF),
                border = Color(0xFFBFDBFE),
                text = Color(0xFF1D4ED8),
                icon = Icons.Outlined.Sync,
                msg = "$pendingSyncCount transaksi menunggu sinkronisasi"
            )
        }
        else -> {
            AlertBannerStylePreview(
                bg = Color(0xFFFFF7ED),
                border = Color(0xFFFFEDD5),
                text = Color(0xFFC2410C),
                icon = Icons.Outlined.PrintDisabled,
                msg = "Printer struk belum terhubung"
            )
        }
    }

    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        color = style.bg,
        border = androidx.compose.foundation.BorderStroke(1.dp, style.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            androidx.compose.material3.Icon(
                imageVector = style.icon,
                contentDescription = null,
                tint = style.text,
                modifier = Modifier.padding(2.dp)
            )
            androidx.compose.material3.Text(
                text = style.msg,
                fontSize = 11.5.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = style.text,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

private data class AlertBannerStylePreview(
    val bg: Color,
    val border: Color,
    val text: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val msg: String
)
