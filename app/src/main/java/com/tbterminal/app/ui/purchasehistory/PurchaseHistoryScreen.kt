package com.tbterminal.app.ui.purchasehistory

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tbterminal.app.data.model.PurchaseDetail
import com.tbterminal.app.data.model.PurchaseSummary
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PurchaseBackground = Color(0xFFF4FAFD)
private val PurchaseBorder = Color(0xFFE2E8F0)
private val PurchaseText = Color(0xFF0F172A)
private val PurchaseMuted = Color(0xFF64748B)
private val PurchasePrimary = Color(0xFF059669)

@Composable
internal fun PurchaseHistoryScreen(
    modifier: Modifier,
    uiState: PurchaseHistoryUiState,
    onSupplierSelected: (String?) -> Unit,
    onRefresh: () -> Unit,
    onShowDetail: (String) -> Unit,
    onDismissDetail: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Column(modifier.fillMaxSize().background(PurchaseBackground).verticalScroll(rememberScrollState()).padding(32.dp)) {
        PurchaseHeader()
        PurchaseSummaryCards(uiState)
        Spacer(Modifier.height(20.dp))
        PurchaseTableCard(uiState, onSupplierSelected, onRefresh, onShowDetail, onPreviousPage, onNextPage)
    }
    uiState.selectedPurchase?.let { PurchaseDetailDialog(it, onDismissDetail) }
}

@Composable
private fun PurchaseHeader() {
    Column(Modifier.padding(bottom = 24.dp)) {
        Text("Nota Pembelian", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = PurchaseText)
        Text("Riwayat restok dan dokumen pembelian dari supplier.", fontSize = 14.sp, color = PurchaseMuted)
    }
}

@Composable
private fun PurchaseSummaryCards(uiState: PurchaseHistoryUiState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        PurchaseMetric("TOTAL NOTA", uiState.totalPurchases.toString(), "Sesuai filter aktif", Icons.AutoMirrored.Outlined.ReceiptLong, PurchasePrimary, Modifier.weight(1f))
        PurchaseMetric("NILAI HALAMAN INI", uiState.pageTotal.asCurrency(), "Maksimal ${uiState.pageSize} nota", Icons.Outlined.MonetizationOn, Color(0xFF2563EB), Modifier.weight(1f))
        PurchaseMetric("SUPPLIER AKTIF", uiState.suppliers.size.toString(), "Tersedia pada filter", Icons.Outlined.Business, Color(0xFFF59E0B), Modifier.weight(1f))
    }
}

@Composable
private fun PurchaseMetric(title: String, value: String, note: String, icon: ImageVector, tint: Color, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(Color.White), border = BorderStroke(1.dp, PurchaseBorder), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(value, fontSize = 22.sp, color = PurchaseText, fontWeight = FontWeight.ExtraBold)
            Text(note, fontSize = 11.sp, color = PurchaseMuted)
        }
    }
}

@Composable
private fun PurchaseTableCard(
    uiState: PurchaseHistoryUiState,
    onSupplierSelected: (String?) -> Unit,
    onRefresh: () -> Unit,
    onShowDetail: (String) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), border = BorderStroke(1.dp, PurchaseBorder), shape = RoundedCornerShape(12.dp)) {
        Column {
            PurchaseToolbar(uiState, onSupplierSelected, onRefresh)
            PurchaseTableHeader()
            when {
                uiState.isLoading -> LoadingBox()
                uiState.errorMessage != null -> PurchaseError(uiState.errorMessage, onRefresh)
                uiState.purchases.isEmpty() -> EmptyBox()
                else -> uiState.purchases.forEach { PurchaseRow(it, onShowDetail) }
            }
            PurchasePagination(uiState, onPreviousPage, onNextPage)
        }
    }
}

@Composable
private fun PurchaseToolbar(uiState: PurchaseHistoryUiState, onSupplierSelected: (String?) -> Unit, onRefresh: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val supplierName = uiState.suppliers.firstOrNull { it.id == uiState.selectedSupplierId }?.name ?: "Semua supplier"
    Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("Daftar Nota", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PurchaseText)
            Text("Status hutang dikelola pada menu Hutang Supplier.", fontSize = 12.sp, color = PurchaseMuted)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                OutlinedButton(onClick = { expanded = true }) { Text(supplierName) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Semua supplier") }, onClick = { expanded = false; onSupplierSelected(null) })
                    uiState.suppliers.forEach { supplier ->
                        DropdownMenuItem(text = { Text(supplier.name) }, onClick = { expanded = false; onSupplierSelected(supplier.id) })
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onRefresh) { Icon(Icons.Outlined.Refresh, "Muat ulang", tint = PurchasePrimary) }
        }
    }
}

@Composable
private fun PurchaseTableHeader() {
    Row(Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(horizontal = 20.dp, vertical = 12.dp)) {
        TableLabel("NO. NOTA", Modifier.weight(1.6f))
        TableLabel("SUPPLIER", Modifier.weight(1.8f))
        TableLabel("TANGGAL MASUK", Modifier.weight(1.4f))
        TableLabel("TOTAL", Modifier.weight(1.2f))
        TableLabel("AKSI", Modifier.weight(0.5f))
    }
}

@Composable
private fun PurchaseRow(purchase: PurchaseSummary, onShowDetail: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(purchase.invoiceNo ?: "Tanpa nomor nota", Modifier.weight(1.6f), color = PurchaseText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(purchase.supplierName, Modifier.weight(1.8f), color = PurchaseMuted, fontSize = 13.sp)
        Text(purchase.receivedAt.asDisplayDate(), Modifier.weight(1.4f), color = PurchaseMuted, fontSize = 13.sp)
        Text(purchase.total.asCurrency(), Modifier.weight(1.2f), color = PurchaseText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Box(Modifier.weight(0.5f)) {
            IconButton(onClick = { onShowDetail(purchase.id) }) { Icon(Icons.Outlined.Visibility, "Lihat detail", tint = PurchasePrimary) }
        }
    }
    HorizontalDivider(color = PurchaseBorder)
}

@Composable
private fun PurchasePagination(uiState: PurchaseHistoryUiState, onPreviousPage: () -> Unit, onNextPage: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Halaman ${uiState.page} dari ${uiState.totalPages}, total ${uiState.totalPurchases} nota", color = PurchaseMuted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPreviousPage, enabled = uiState.page > 1) { Text("Sebelumnya") }
            OutlinedButton(onClick = onNextPage, enabled = uiState.page < uiState.totalPages) { Text("Berikutnya") }
        }
    }
}

@Composable
private fun PurchaseDetailDialog(detail: PurchaseDetail, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(Modifier.widthIn(max = 760.dp).heightIn(max = 680.dp), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Detail Nota Pembelian", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PurchaseText)
                        Text(detail.invoiceNo ?: "Tanpa nomor nota", color = PurchasePrimary, fontSize = 13.sp)
                    }
                    TextButton(onClick = onDismiss) { Text("Tutup") }
                }
                Spacer(Modifier.height(16.dp))
                DetailInfo("Supplier", detail.supplierName)
                DetailInfo("Tanggal masuk", detail.receivedAt.asDisplayDate())
                DetailInfo("Catatan", detail.notes ?: "-")
                Spacer(Modifier.height(16.dp))
                Text("Item Pembelian", color = PurchaseText, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                detail.items.forEach { item ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Inventory2, null, tint = PurchasePrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.productName, color = PurchaseText, fontWeight = FontWeight.SemiBold)
                            Text("${item.quantity.stripTrailingZeros().toPlainString()} x ${item.priceAtTransaction.asCurrency()}", color = PurchaseMuted, fontSize = 12.sp)
                        }
                        Text(item.subtotal.asCurrency(), color = PurchaseText, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = PurchaseBorder)
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL", color = PurchaseMuted, fontWeight = FontWeight.Bold)
                    Text(detail.total.asCurrency(), color = PurchasePrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun DetailInfo(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = PurchaseMuted)
        Text(value, color = PurchaseText, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LoadingBox() = Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

@Composable
private fun EmptyBox() = Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { Text("Belum ada nota pembelian.", color = PurchaseMuted) }

@Composable
private fun PurchaseError(message: String, onRefresh: () -> Unit) {
    Column(Modifier.fillMaxWidth().height(180.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(message, color = Color(0xFFDC2626))
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRefresh) { Text("Coba Lagi") }
    }
}

@Composable
private fun TableLabel(text: String, modifier: Modifier) {
    Text(text, modifier, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
}

private fun BigDecimal.asCurrency(): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).format(this)

private fun String.asDisplayDate(): String {
    return runCatching {
        OffsetDateTime.parse(this).format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.forLanguageTag("id-ID")))
    }.getOrDefault(this)
}
