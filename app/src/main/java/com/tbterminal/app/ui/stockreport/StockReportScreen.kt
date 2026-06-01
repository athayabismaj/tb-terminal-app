package com.tbterminal.app.ui.stockreport

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.ProductStock
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private val StockReportBackground = Color(0xFFF4FAFD)
private val StockReportBorder = Color(0xFFE2E8F0)
private val StockReportText = Color(0xFF0F172A)
private val StockReportMuted = Color(0xFF64748B)
private val StockReportPrimary = Color(0xFF059669)

@Composable
internal fun StockReportScreen(
    modifier: Modifier,
    uiState: StockReportUiState,
    onSearchChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize().background(StockReportBackground).verticalScroll(rememberScrollState()).padding(32.dp)
    ) {
        Text("Laporan Stok", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = StockReportText)
        Text("Pantau posisi stok, nilai persediaan, dan produk yang perlu ditindaklanjuti.", color = StockReportMuted, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        StockSummary(uiState)
        Spacer(Modifier.height(20.dp))
        StockTableCard(uiState, onSearchChanged, onRefresh, onPreviousPage, onNextPage)
    }
}

@Composable
private fun StockSummary(uiState: StockReportUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
        SummaryCard("TOTAL PRODUK", uiState.totalProducts.toString(), "Semua data backend", Icons.Outlined.Inventory2, StockReportPrimary, Modifier.weight(1f))
        SummaryCard("NILAI STOK HALAMAN INI", formatCurrency(uiState.pageStockValue), "Berdasarkan harga beli", Icons.Outlined.MonetizationOn, Color(0xFF2563EB), Modifier.weight(1f))
        SummaryCard("STOK RENDAH HALAMAN INI", uiState.pageLowStockCount.toString(), "Perlu dipantau", Icons.Outlined.WarningAmber, Color(0xFFF59E0B), Modifier.weight(1f))
        SummaryCard("STOK HABIS HALAMAN INI", uiState.pageOutOfStockCount.toString(), "Perlu restok", Icons.Outlined.Assessment, Color(0xFFDC2626), Modifier.weight(1f))
    }
}

@Composable
private fun SummaryCard(title: String, value: String, note: String, icon: ImageVector, tint: Color, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(Color.White), border = BorderStroke(1.dp, StockReportBorder)) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(value, color = StockReportText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(note, color = StockReportMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun StockTableCard(
    uiState: StockReportUiState,
    onSearchChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(Color.White), border = BorderStroke(1.dp, StockReportBorder), modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChanged,
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    placeholder = { Text("Cari SKU atau nama produk...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onRefresh) { Icon(Icons.Outlined.Refresh, "Muat ulang", tint = StockReportPrimary) }
            }
            StockTableHeader()
            when {
                uiState.isLoading -> Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                uiState.errorMessage != null -> StockError(uiState.errorMessage, onRefresh)
                uiState.stocks.isEmpty() -> Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { Text("Belum ada data stok.", color = StockReportMuted) }
                else -> uiState.stocks.forEach { stock -> StockRow(stock) }
            }
            StockPagination(uiState, onPreviousPage, onNextPage)
        }
    }
}

@Composable
private fun StockTableHeader() {
    Row(Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(horizontal = 20.dp, vertical = 12.dp)) {
        StockLabel("PRODUK", Modifier.weight(2.4f))
        StockLabel("KATEGORI", Modifier.weight(1.3f))
        StockLabel("STOK", Modifier.weight(0.9f))
        StockLabel("MINIMUM", Modifier.weight(0.9f))
        StockLabel("NILAI STOK", Modifier.weight(1.2f))
        StockLabel("STATUS", Modifier.weight(0.9f))
    }
}

@Composable
private fun StockRow(stock: ProductStock) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(2.4f)) {
            Text(stock.productName, color = StockReportText, fontWeight = FontWeight.Bold)
            Text(stock.sku, color = StockReportMuted, fontSize = 12.sp)
        }
        Text(stock.categoryName, Modifier.weight(1.3f), color = StockReportMuted, fontSize = 13.sp)
        Text("${formatQuantity(stock.quantity)} ${stock.unitName}", Modifier.weight(0.9f), color = StockReportText, fontSize = 13.sp)
        Text(formatQuantity(stock.minStock), Modifier.weight(0.9f), color = StockReportMuted, fontSize = 13.sp)
        Text(formatCurrency(stock.quantity.multiply(stock.priceBuy)), Modifier.weight(1.2f), color = StockReportText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        val status = stockStatus(stock)
        Text(status.first, Modifier.weight(0.9f), color = status.second, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
    HorizontalDivider(color = StockReportBorder)
}

@Composable
private fun StockError(message: String, onRefresh: () -> Unit) {
    Column(Modifier.fillMaxWidth().height(180.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(message, color = Color(0xFFDC2626))
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRefresh) { Text("Coba Lagi") }
    }
}

@Composable
private fun StockPagination(uiState: StockReportUiState, onPreviousPage: () -> Unit, onNextPage: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Menampilkan halaman ${uiState.page} dari ${uiState.totalPages}, total ${uiState.totalProducts} produk", color = StockReportMuted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPreviousPage, enabled = uiState.page > 1) { Text("Sebelumnya") }
            OutlinedButton(onClick = onNextPage, enabled = uiState.page < uiState.totalPages) { Text("Berikutnya") }
        }
    }
}

@Composable
private fun StockLabel(text: String, modifier: Modifier) {
    Text(text, modifier, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
}

private fun stockStatus(stock: ProductStock): Pair<String, Color> {
    return when {
        stock.quantity <= BigDecimal.ZERO -> "HABIS" to Color(0xFFDC2626)
        stock.quantity <= stock.minStock -> "RENDAH" to Color(0xFFF59E0B)
        else -> "AMAN" to StockReportPrimary
    }
}

private fun formatQuantity(value: BigDecimal): String = value.stripTrailingZeros().toPlainString()

private fun formatCurrency(value: BigDecimal): String {
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).format(value)
}
