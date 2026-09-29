package com.tbterminal.app.ui.purchasehistory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable

import androidx.compose.material.icons.outlined.SignalCellularAlt

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

import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
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
            // PurchaseSummaryCards moved
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
            PurchaseMetric("Total nota", uiState.totalPurchases.toString(), "Sesuai filter", Icons.Outlined.ReceiptLong, PurchasePrimary, Modifier.weight(1f), true)
            PurchaseMetric("Nilai halaman", uiState.pageTotal.asCurrency(), "Halaman ini", Icons.Outlined.MonetizationOn, Color(0xFF2563EB), Modifier.weight(1f), true)
        }
        PurchaseMetric("Supplier aktif", uiState.suppliers.size.toString(), "Tersedia pada filter", Icons.Outlined.Business, Color(0xFFF59E0B), Modifier.fillMaxWidth(), true)
    } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        PurchaseMetric("TOTAL NOTA", uiState.totalPurchases.toString(), "Sesuai filter aktif", Icons.Outlined.ReceiptLong, PurchasePrimary, Modifier.weight(1f))
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
    var showSummaryPopup by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        if (compact) {
            // Header for List
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pembelian", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        .clickable { showSummaryPopup = true }
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.SignalCellularAlt, contentDescription = "Ringkasan", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                }
            }
            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFDFE5E1)),
                shadowElevation = 2.dp
            ) {
                when {
                    uiState.isLoading && uiState.purchases.isEmpty() -> LoadingBox()
                    uiState.errorMessage != null -> PurchaseError(uiState.errorMessage, onRefresh)
                    uiState.visiblePurchases.isEmpty() -> androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFDFE5E1)),
                    shadowElevation = 2.dp
                ) {
                    EmptyBox(uiState.searchQuery)
                }
                    else -> {
                        Column {
                            uiState.visiblePurchases.forEachIndexed { index, purchase ->
                                PurchaseMobileRow(purchase, onShowDetail)
                                if (index < uiState.visiblePurchases.lastIndex) {
                                    HorizontalDivider(color = Color(0xFFEEF2F0), modifier = Modifier.padding(horizontal = 16.dp))
                                }
                            }
                        }
                    }
                }
            }
            
            if (showSummaryPopup) {
                com.tbterminal.app.ui.components.TbMobileControlSheet(
                    title = "Ringkasan Pembelian",
                    subtitle = "Informasi total nota dan nilai pembelian",
                    onDismiss = { showSummaryPopup = false }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        PurchaseSummarySheetMetric(
                            title = "TOTAL NOTA",
                            value = uiState.totalPurchases.toString(),
                            note = "Sesuai filter aktif",
                            icon = Icons.Outlined.ReceiptLong,
                            iconColor = PurchasePrimary,
                            iconBgColor = Color(0xFFE1EFEA)
                        )
                        PurchaseSummarySheetMetric(
                            title = "NILAI HALAMAN INI",
                            value = uiState.pageTotal.asCurrency(),
                            note = "Maksimal ${uiState.pageSize} nota",
                            icon = Icons.Outlined.MonetizationOn,
                            iconColor = PurchasePrimary,
                            iconBgColor = Color(0xFFE1EFEA),
                            isHighlighted = true
                        )
                        PurchaseSummarySheetMetric(
                            title = "SUPPLIER AKTIF",
                            value = uiState.suppliers.size.toString(),
                            note = "Tersedia pada filter",
                            icon = Icons.Outlined.Business,
                            iconColor = Color(0xFFD97706),
                            iconBgColor = Color(0xFFFEF3C7)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    com.tbterminal.app.ui.components.TbMobileSheetDoneButton(
                        onClick = { showSummaryPopup = false }
                    )
                }
            }
        } else {
            PurchaseTableHeader()
            when {
                uiState.isLoading && uiState.purchases.isEmpty() -> LoadingBox()
                uiState.errorMessage != null -> PurchaseError(uiState.errorMessage, onRefresh)
                uiState.visiblePurchases.isEmpty() -> androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFDFE5E1)),
                    shadowElevation = 2.dp
                ) {
                    EmptyBox(uiState.searchQuery)
                }
                else -> {
                    Column {
                        uiState.visiblePurchases.forEachIndexed { index, purchase ->
                            PurchaseRow(purchase, index, onShowDetail)
                        }
                    }
                }
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
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = uiState.searchQuery, onValueChange = onSearchChanged,
            placeholder = { Text("Cari nomor nota atau supplier", color = PurchaseMuted, fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Outlined.Search, "Cari nota", tint = PurchaseMuted) },
            singleLine = true,
            modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = PurchaseText, unfocusedTextColor = PurchaseText, cursorColor = PurchasePrimary, focusedBorderColor = PurchaseBorder, unfocusedBorderColor = PurchaseBorder, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
        )
        SupplierFilterDropdown(uiState, onSupplierSelected)
    }
}

@Composable
private fun SupplierFilterDropdown(uiState: PurchaseHistoryUiState, onSupplierSelected: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFFC3DFD5), RoundedCornerShape(16.dp))
                .background(Color.White)
                .clickable { expanded = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Tune, contentDescription = "Filter", tint = PurchasePrimary, modifier = Modifier.size(20.dp))
            
            if (uiState.selectedSupplierId != null) {
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(8.dp).background(Color(0xFFF59E0B), RoundedCornerShape(50)))
            }
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
                DropdownMenuItem(
                    text = { Text(supplier.name, fontWeight = if (uiState.selectedSupplierId == supplier.id) FontWeight.Bold else FontWeight.Normal) }, 
                    onClick = {
                        onSupplierSelected(supplier.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PurchaseMobileRow(purchase: PurchaseSummary, onShowDetail: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onShowDetail(purchase.id) }
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(40.dp).background(Color(0xFFE1EFEA), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = Color(0xFF1B4D3E), modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    if (purchase.invoiceNo != null) {
                        Text(
                            text = purchase.invoiceNo,
                            color = Color(0xFF0F172A),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "Tanpa nomor nota",
                                color = Color(0xFF1E293B),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.width(6.dp))
                            Box(modifier = Modifier.background(Color(0xFFF1F5F9), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("Nota Manual", color = Color(0xFF64748B), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp).padding(top = 2.dp))
                }
                Text(
                    text = purchase.supplierName,
                    color = Color(0xFF64748B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp).drawBehind {
                drawLine(
                    color = Color(0xFFF1F5F9),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }.padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("TANGGAL MASUK", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
                Text(purchase.receivedAt.asDisplayDate(), color = Color(0xFF334155), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("TOTAL", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
                Text(purchase.total.asCurrency(), color = Color(0xFF1B4D3E), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
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
private fun PurchasePagination(
    uiState: PurchaseHistoryUiState,
    compact: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    if (compact) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.background(Color.White, RoundedCornerShape(50)).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("${uiState.visiblePurchases.size} dari ${uiState.totalPurchases} nota", color = Color(0xFF475569), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(
                modifier = Modifier.background(Color.White, RoundedCornerShape(50)).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Sebelumnya",
                    tint = if (uiState.page > 1) Color(0xFF0F172A) else Color(0xFFCBD5E1),
                    modifier = Modifier.size(16.dp).clickable(enabled = uiState.page > 1, onClick = onPreviousPage)
                )
                Text("${uiState.page} / ${uiState.totalPages.coerceAtLeast(1)}", color = Color(0xFF1E293B), fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Berikutnya",
                    tint = if (uiState.page < uiState.totalPages) Color(0xFF0F172A) else Color(0xFFCBD5E1),
                    modifier = Modifier.size(16.dp).clickable(enabled = uiState.page < uiState.totalPages, onClick = onNextPage)
                )
            }
        }
    } else {
        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Menampilkan ${uiState.visiblePurchases.size} dari ${uiState.totalPurchases} nota", color = PurchaseMuted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreviousPage, enabled = uiState.page > 1) { Icon(Icons.Default.ChevronLeft, "Sebelumnya") }
                Text("${uiState.page} / ${uiState.totalPages.coerceAtLeast(1)}", fontWeight = FontWeight.Bold)
                IconButton(onClick = onNextPage, enabled = uiState.page < uiState.totalPages) { Icon(Icons.Default.ChevronRight, "Berikutnya") }
            }
        }
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
private fun LoadingBox() = com.tbterminal.app.ui.components.SkeletonList(
    modifier = Modifier.fillMaxWidth().height(180.dp),
    itemCount = 4,
)

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

@Composable
private fun PurchaseSummarySheetMetric(
    title: String,
    value: String,
    note: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: androidx.compose.ui.graphics.Color,
    iconBgColor: androidx.compose.ui.graphics.Color,
    isHighlighted: Boolean = false
) {
    val bgColor = if (isHighlighted) androidx.compose.ui.graphics.Color(0xFFF2F8F5) else androidx.compose.ui.graphics.Color.White
    val borderColor = if (isHighlighted) androidx.compose.ui.graphics.Color(0xFFC3DFD5) else androidx.compose.ui.graphics.Color(0xFFE2E8F0)
    val titleColor = if (isHighlighted) androidx.compose.ui.graphics.Color(0xFF1E5847) else androidx.compose.ui.graphics.Color(0xFF94A3B8)
    val valueColor = if (isHighlighted) androidx.compose.ui.graphics.Color(0xFF256B57) else androidx.compose.ui.graphics.Color(0xFF0F172A)

    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        shadowElevation = if (isHighlighted) 2.dp else 1.dp
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .background(iconBgColor),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                androidx.compose.foundation.layout.Column {
                    androidx.compose.material3.Text(
                        title,
                        color = titleColor,
                        fontSize = 11.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    androidx.compose.material3.Text(
                        value,
                        color = valueColor,
                        fontSize = 18.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            androidx.compose.material3.Text(
                note,
                color = androidx.compose.ui.graphics.Color(0xFF64748B),
                fontSize = 12.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Right
            )
        }
    }
}
