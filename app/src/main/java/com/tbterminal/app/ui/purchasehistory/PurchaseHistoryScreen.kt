package com.tbterminal.app.ui.purchasehistory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

private val PurchaseBackground = Color.White
private val PurchaseBorder = Color(0xFFE2E8F0)
private val PurchaseText = Color(0xFF0F172A)
private val PurchaseMuted = Color(0xFF64748B)
private val PurchasePrimary = Color(0xFF059669)
private val PurchaseSoft = Color(0xFFF8FAFC)

@Composable
internal fun PurchaseHistoryScreen(
    modifier: Modifier,
    uiState: PurchaseHistoryUiState,
    onSupplierSelected: (String?) -> Unit,
    onSearchChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onShowDetail: (String) -> Unit,
    onDismissDetail: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    BoxWithConstraints(modifier.fillMaxSize().background(PurchaseBackground)) {
        val compact = maxWidth < 700.dp
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 14.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 22.dp)
        ) {
            PurchaseSummaryCards(uiState, compact)
            PurchaseToolbar(uiState, compact, onSupplierSelected, onSearchChanged)
            PurchaseTable(uiState, compact, onRefresh, onShowDetail, onPreviousPage, onNextPage)
        }
    }
    uiState.selectedPurchase?.let { PurchaseDetailDialog(it, onDismissDetail) }
}

@Composable
private fun PurchaseHeader() {
    Text("Nota Pembelian", fontSize = 28.sp, fontWeight = FontWeight.Medium, color = PurchaseText)
}

@Composable
private fun PurchaseSummaryCards(uiState: PurchaseHistoryUiState, compact: Boolean) {
    if (compact) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PurchaseMetric("Total nota", uiState.totalPurchases.toString(), "Sesuai filter", Icons.AutoMirrored.Outlined.ReceiptLong, PurchasePrimary, Modifier.weight(1f), true)
            PurchaseMetric("Nilai halaman", uiState.pageTotal.asCurrency(), "Halaman ini", Icons.Outlined.MonetizationOn, Color(0xFF2563EB), Modifier.weight(1f), true)
        }
        PurchaseMetric("Supplier aktif", uiState.suppliers.size.toString(), "Tersedia pada filter", Icons.Outlined.Business, Color(0xFFF59E0B), Modifier.fillMaxWidth(), true)
    } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        PurchaseMetric("TOTAL NOTA", uiState.totalPurchases.toString(), "Sesuai filter aktif", Icons.AutoMirrored.Outlined.ReceiptLong, PurchasePrimary, Modifier.weight(1f))
        PurchaseMetric("NILAI HALAMAN INI", uiState.pageTotal.asCurrency(), "Maksimal ${uiState.pageSize} nota", Icons.Outlined.MonetizationOn, Color(0xFF2563EB), Modifier.weight(1f))
        PurchaseMetric("SUPPLIER AKTIF", uiState.suppliers.size.toString(), "Tersedia pada filter", Icons.Outlined.Business, Color(0xFFF59E0B), Modifier.weight(1f))
    }
}

@Composable
private fun PurchaseMetric(title: String, value: String, note: String, icon: ImageVector, tint: Color, modifier: Modifier, compact: Boolean = false) {
    Card(modifier, colors = CardDefaults.cardColors(Color.White), border = BorderStroke(1.dp, PurchaseBorder), shape = RoundedCornerShape(if (compact) 18.dp else 12.dp)) {
        Column(Modifier.padding(if (compact) 14.dp else 18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, fontSize = 10.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(value, fontSize = if (compact) 18.sp else 22.sp, color = PurchaseText, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(note, fontSize = 11.sp, color = PurchaseMuted)
        }
    }
}

@Composable
private fun PurchaseTable(
    uiState: PurchaseHistoryUiState,
    compact: Boolean,
    onRefresh: () -> Unit,
    onShowDetail: (String) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        if (!compact) PurchaseTableHeader()
        when {
            uiState.isLoading -> LoadingBox()
            uiState.errorMessage != null -> PurchaseError(uiState.errorMessage, onRefresh)
            uiState.visiblePurchases.isEmpty() -> EmptyBox(uiState.searchQuery)
            else -> uiState.visiblePurchases.forEachIndexed { index, purchase ->
                if (compact) PurchaseMobileRow(purchase, onShowDetail) else PurchaseRow(purchase, index, onShowDetail)
            }
        }
        PurchasePagination(uiState, compact, onPreviousPage, onNextPage)
    }
}

@Composable
private fun PurchaseToolbar(
    uiState: PurchaseHistoryUiState,
    compact: Boolean,
    onSupplierSelected: (String?) -> Unit,
    onSearchChanged: (String) -> Unit
) {
    val search: @Composable (Modifier) -> Unit = { fieldModifier ->
        OutlinedTextField(
            value = uiState.searchQuery, onValueChange = onSearchChanged,
            placeholder = { Text("Cari nota atau supplier", color = PurchaseMuted) },
            trailingIcon = { Icon(Icons.Outlined.Search, "Cari nota", tint = PurchaseMuted) }, singleLine = true,
            modifier = fieldModifier.height(56.dp), shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PurchaseText, unfocusedTextColor = PurchaseText, cursorColor = PurchasePrimary, focusedBorderColor = PurchasePrimary, unfocusedBorderColor = PurchaseBorder, focusedContainerColor = PurchaseBackground, unfocusedContainerColor = PurchaseBackground)
        )
    }
    if (compact) Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        search(Modifier.fillMaxWidth())
        SupplierFilterDropdown(uiState, onSupplierSelected, Modifier.fillMaxWidth())
    } else Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        search(Modifier.weight(1f))
        SupplierFilterDropdown(uiState, onSupplierSelected, Modifier.width(220.dp))
    }
}

@Composable
private fun SupplierFilterDropdown(uiState: PurchaseHistoryUiState, onSupplierSelected: (String?) -> Unit, modifier: Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val supplierName = uiState.suppliers.firstOrNull { it.id == uiState.selectedSupplierId }?.name ?: "Semua supplier"
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = modifier.height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, PurchaseBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = PurchaseBackground,
                contentColor = PurchaseText
            )
        ) {
            Text(
                text = supplierName,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
            Icon(Icons.Outlined.ExpandMore, contentDescription = "Pilih supplier", tint = PurchaseMuted, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(220.dp)
                .heightIn(max = 320.dp)
                .background(PurchaseBackground)
        ) {
            DropdownMenuItem(text = { Text("Semua supplier") }, onClick = {
                onSupplierSelected(null)
                expanded = false
            })
            uiState.suppliers.forEach { supplier ->
                DropdownMenuItem(text = { Text(supplier.name) }, onClick = {
                    onSupplierSelected(supplier.id)
                    expanded = false
                })
            }
        }
    }
}

@Composable
private fun PurchaseMobileRow(purchase: PurchaseSummary, onShowDetail: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(purchase.invoiceNo ?: "Tanpa nomor nota", color = PurchaseText, fontWeight = FontWeight.Bold)
                Text(purchase.supplierName, color = PurchaseMuted, fontSize = 12.sp)
            }
            IconButton(onClick = { onShowDetail(purchase.id) }) { Icon(Icons.Outlined.Visibility, "Lihat detail", tint = PurchasePrimary) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("Tanggal masuk", color = PurchaseMuted, fontSize = 11.sp); Text(purchase.receivedAt.asDisplayDate(), color = PurchaseText, fontSize = 12.sp) }
            Column(horizontalAlignment = Alignment.End) { Text("Total", color = PurchaseMuted, fontSize = 11.sp); Text(purchase.total.asCurrency(), color = PurchaseText, fontWeight = FontWeight.Bold) }
        }
    }
    HorizontalDivider(color = PurchaseBorder)
}

@Composable
private fun PurchaseTableHeader() {
    Row(Modifier.fillMaxWidth().background(PurchaseSoft).padding(horizontal = 20.dp, vertical = 14.dp)) {
        TableLabel("NO. NOTA", Modifier.weight(1.6f))
        TableLabel("SUPPLIER", Modifier.weight(1.8f))
        TableLabel("TANGGAL MASUK", Modifier.weight(1.4f))
        TableLabel("TOTAL", Modifier.weight(1.2f))
        TableLabel("AKSI", Modifier.weight(0.5f))
    }
}

@Composable
private fun PurchaseRow(purchase: PurchaseSummary, index: Int, onShowDetail: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (index % 2 == 0) PurchaseBackground else PurchaseSoft.copy(alpha = 0.7f))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
private fun PurchasePagination(uiState: PurchaseHistoryUiState, compact: Boolean, onPreviousPage: () -> Unit, onNextPage: () -> Unit) {
    val controls: @Composable () -> Unit = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PurchasePageButton(uiState.page > 1 && !uiState.isLoading, onPreviousPage, Icons.Default.ChevronLeft)
            Text("${uiState.page} / ${uiState.totalPages}", color = PurchaseText, fontWeight = FontWeight.Bold)
            PurchasePageButton(uiState.page < uiState.totalPages && !uiState.isLoading, onNextPage, Icons.Default.ChevronRight)
        }
    }
    if (compact) Column(Modifier.fillMaxWidth().background(PurchaseSoft).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (uiState.searchQuery.isBlank()) "${uiState.totalPurchases} nota pembelian" else "${uiState.visiblePurchases.size} hasil", color = PurchaseMuted, fontSize = 12.sp)
        controls()
    } else Row(
        Modifier
            .fillMaxWidth()
            .background(PurchaseSoft.copy(alpha = 0.7f))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                if (uiState.searchQuery.isBlank()) {
                    "Menampilkan ${uiState.pageStartIndex}-${uiState.pageEndIndex} dari ${uiState.totalPurchases} nota"
                } else {
                    "Menampilkan ${uiState.visiblePurchases.size} nota cocok pada halaman ini"
                },
                color = PurchaseText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text("Maksimal ${uiState.pageSize} nota per halaman", color = PurchaseMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
        controls()
    }
}

@Composable
private fun PurchasePageButton(enabled: Boolean, onClick: () -> Unit, icon: ImageVector) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(34.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = PurchaseText)
    ) {
        Icon(icon, contentDescription = null)
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
private fun EmptyBox(searchQuery: String) = Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
    Text(if (searchQuery.isBlank()) "Belum ada nota pembelian." else "Nota pembelian tidak ditemukan.", color = PurchaseMuted)
}

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
