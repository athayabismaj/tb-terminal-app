package com.tbterminal.app.ui.stockreport

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.model.StockMovement
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private val StockReportBackground = Color.White
private val StockReportBorder = Color(0xFFE2E8F0)
private val StockReportText = Color(0xFF0F172A)
private val StockReportMuted = Color(0xFF64748B)
private val StockReportPrimary = Color(0xFF059669)
private val StockReportSoft = Color(0xFFF1F5F9)

@Composable
internal fun StockReportScreen(
    modifier: Modifier,
    uiState: StockReportUiState,
    onSearchChanged: (String) -> Unit,
    onCategoryFilterChanged: (String?) -> Unit,
    onProductSelected: (String) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StockReportBackground)
            .verticalScroll(rememberScrollState())
            .padding(40.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Text("Laporan Stok", fontSize = 28.sp, fontWeight = FontWeight.Medium, color = StockReportText)
        StockSummary(uiState)
        StockReportToolbar(uiState, onSearchChanged, onCategoryFilterChanged)
        StockTableCard(uiState, onPreviousPage, onNextPage, onProductSelected)
        StockCardLedger(uiState)
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
private fun StockReportToolbar(
    uiState: StockReportUiState,
    onSearchChanged: (String) -> Unit,
    onCategoryFilterChanged: (String?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChanged,
            trailingIcon = { Icon(Icons.Outlined.Search, "Cari produk", tint = StockReportMuted) },
            placeholder = { Text("Cari SKU atau nama produk...", color = StockReportMuted) },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = StockReportText,
                unfocusedTextColor = StockReportText,
                cursorColor = StockReportPrimary,
                focusedBorderColor = StockReportPrimary,
                unfocusedBorderColor = StockReportBorder,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )
        StockCategoryDropdown(uiState.categoryFilter, uiState.categoryOptions, onCategoryFilterChanged)
    }
}

@Composable
private fun StockCategoryDropdown(
    selectedCategory: String?,
    categories: List<String>,
    onCategoryFilterChanged: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .width(220.dp)
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, StockReportBorder)
        ) {
            Text(
                text = selectedCategory ?: "Semua kategori",
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = StockReportText,
                fontWeight = FontWeight.Medium
            )
            Icon(Icons.Outlined.ExpandMore, "Pilih kategori", tint = StockReportMuted, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(220.dp)
                .heightIn(max = 320.dp)
                .background(Color.White)
        ) {
            DropdownMenuItem(text = { Text("Semua kategori") }, onClick = {
                expanded = false
                onCategoryFilterChanged(null)
            })
            categories.forEach { category ->
                DropdownMenuItem(text = { Text(category) }, onClick = {
                    expanded = false
                    onCategoryFilterChanged(category)
                })
            }
        }
    }
}

@Composable
private fun StockTableCard(
    uiState: StockReportUiState,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onProductSelected: (String) -> Unit
) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(Color.White), border = BorderStroke(1.dp, StockReportBorder), modifier = Modifier.fillMaxWidth()) {
        Column {
            StockTableHeader()
            when {
                uiState.isLoading -> Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                uiState.errorMessage != null -> StockError(uiState.errorMessage)
                uiState.visibleStocks.isEmpty() -> Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { Text("Belum ada data stok.", color = StockReportMuted) }
                else -> uiState.visibleStocks.forEachIndexed { index, stock -> StockRow(stock, index % 2 != 0, onProductSelected) }
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
private fun StockRow(stock: ProductStock, useAlternateBackground: Boolean, onProductSelected: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onProductSelected(stock.productId) }
            .background(if (useAlternateBackground) StockReportSoft.copy(alpha = 0.76f) else Color.White)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
private fun StockCardLedger(uiState: StockReportUiState) {
    val selected = uiState.visibleStocks.firstOrNull { it.productId == uiState.selectedProductId }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(Color.White),
        border = BorderStroke(1.dp, StockReportBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Kartu Stok", color = StockReportText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                selected?.let { "${it.sku} · ${it.productName}" } ?: "Pilih produk pada tabel untuk melihat seluruh mutasi.",
                color = StockReportMuted
            )
            when {
                uiState.isCardLoading -> CircularProgressIndicator()
                uiState.cardErrorMessage != null -> Text(uiState.cardErrorMessage, color = Color(0xFFDC2626))
                selected == null -> Unit
                uiState.stockMovements.isEmpty() -> Text("Belum ada mutasi stok.", color = StockReportMuted)
                else -> {
                    Text(
                        if (uiState.cardReconciled == true) "Saldo ledger sesuai stok produk" else "PERINGATAN: saldo ledger tidak sesuai stok produk",
                        color = if (uiState.cardReconciled == true) StockReportPrimary else Color(0xFFDC2626),
                        fontWeight = FontWeight.Bold
                    )
                    StockMovementHeader()
                    uiState.stockMovements.forEach { movement -> StockMovementRow(movement) }
                }
            }
        }
    }
}

@Composable
private fun StockMovementHeader() {
    Row(Modifier.fillMaxWidth().background(StockReportSoft).padding(10.dp)) {
        StockLabel("WAKTU / REFERENSI", Modifier.weight(2f))
        StockLabel("JENIS", Modifier.weight(1f))
        StockLabel("SALDO AWAL", Modifier.weight(1f))
        StockLabel("MASUK", Modifier.weight(0.8f))
        StockLabel("KELUAR", Modifier.weight(0.8f))
        StockLabel("SALDO AKHIR", Modifier.weight(1f))
    }
}

@Composable
private fun StockMovementRow(row: StockMovement) {
    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(2f)) {
            Text(row.occurredAt.take(19).replace('T', ' '), color = StockReportText, fontSize = 12.sp)
            Text(row.referenceNumber ?: row.referenceType, color = StockReportMuted, fontSize = 11.sp)
        }
        Text(row.type.replace('_', ' '), Modifier.weight(1f), color = StockReportText, fontSize = 11.sp)
        Text(formatQuantity(row.balanceBefore), Modifier.weight(1f), color = StockReportText)
        Text(formatQuantity(row.qtyIn), Modifier.weight(0.8f), color = StockReportPrimary)
        Text(formatQuantity(row.qtyOut), Modifier.weight(0.8f), color = Color(0xFFDC2626))
        Text(formatQuantity(row.balanceAfter), Modifier.weight(1f), color = StockReportText, fontWeight = FontWeight.Bold)
    }
    HorizontalDivider(color = StockReportBorder)
}

@Composable
private fun StockError(message: String) {
    Column(Modifier.fillMaxWidth().height(180.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(message, color = Color(0xFFDC2626))
    }
}

@Composable
private fun StockPagination(uiState: StockReportUiState, onPreviousPage: () -> Unit, onNextPage: () -> Unit) {
    val safePage = uiState.page.coerceAtLeast(1)
    val safeTotalPages = uiState.totalPages.coerceAtLeast(1)
    val startItem = if (uiState.totalProducts == 0L) 0L else ((safePage - 1) * uiState.pageSize + 1L)
    val endItem = if (uiState.totalProducts == 0L) 0L else (startItem + uiState.visibleStocks.size - 1L).coerceAtMost(uiState.totalProducts)
    val rangeText = if (uiState.categoryFilter == null) {
        "Menampilkan $startItem-$endItem dari ${uiState.totalProducts} produk"
    } else {
        "Menampilkan ${uiState.visibleStocks.size} produk kategori pada halaman ini"
    }

    Row(
        Modifier
            .fillMaxWidth()
            .background(StockReportSoft.copy(alpha = 0.72f))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                rangeText,
                color = StockReportText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "Maksimal ${uiState.pageSize} produk per halaman",
                color = StockReportMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            StockPageButton(Icons.Default.ChevronLeft, safePage > 1, onPreviousPage)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(StockReportPrimary, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("$safePage", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Text("/ $safeTotalPages", color = StockReportMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            StockPageButton(Icons.Default.ChevronRight, safePage < safeTotalPages, onNextPage)
        }
    }
}

@Composable
private fun StockPageButton(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, StockReportBorder),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(34.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) StockReportText else StockReportMuted.copy(alpha = 0.35f))
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
