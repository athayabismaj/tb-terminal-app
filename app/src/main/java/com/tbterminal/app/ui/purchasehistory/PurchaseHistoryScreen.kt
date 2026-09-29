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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button

import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Store
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
                @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
                androidx.compose.material3.ModalBottomSheet(
                    onDismissRequest = { showSummaryPopup = false },
                    containerColor = Color.White,
                    dragHandle = {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(modifier = Modifier.width(48.dp).height(6.dp).background(Color(0xFFCBD5E1), RoundedCornerShape(50)))
                        }
                    },
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text("Ringkasan Pembelian", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), letterSpacing = (-0.5).sp)
                                Text("Informasi total nota dan nilai pembelian", color = Color(0xFF64748B), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                            Box(
                                modifier = Modifier.size(32.dp).clickable { showSummaryPopup = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                            }
                        }

                        // Content
                        Column(
                            modifier = Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Total Nota
                            PurchaseSummarySheetMetric(
                                title = "TOTAL NOTA",
                                value = uiState.totalPurchases.toString(),
                                note = "Sesuai filter aktif",
                                icon = Icons.Outlined.ReceiptLong,
                                iconColor = Color(0xFF256B57),
                                iconBgColor = Color(0xFFEBF3F0),
                                isHighlighted = false
                            )
                            
                            // Nilai Halaman Ini
                            PurchaseSummarySheetMetric(
                                title = "NILAI HALAMAN INI",
                                value = uiState.pageTotal.asCurrency(),
                                note = "Maksimal ${uiState.pageSize} nota",
                                icon = Icons.Outlined.Paid,
                                iconColor = Color(0xFF256B57),
                                iconBgColor = Color(0xFFEBF3F0),
                                isHighlighted = true
                            )
                            
                            // Supplier Aktif
                            PurchaseSummarySheetMetric(
                                title = "SUPPLIER AKTIF",
                                value = uiState.suppliers.size.toString(),
                                note = "Tersedia pada filter",
                                icon = Icons.Outlined.Store,
                                iconColor = Color(0xFFD97706),
                                iconBgColor = Color(0xFFFFFBEB),
                                isHighlighted = false
                            )
                        }

                        // Footer
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Box(modifier = Modifier.fillMaxWidth().padding(20.dp).padding(top = 4.dp)) {
                            Button(
                                onClick = { showSummaryPopup = false },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF256B57))
                            ) {
                                Text("Selesai", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
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
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun PurchaseDetailDialog(detail: PurchaseDetail, onDismiss: () -> Unit) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.width(48.dp).height(6.dp).background(Color(0xFFE2E8F0), RoundedCornerShape(50))
                )
            }
        },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Detail Nota Pembelian",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.5).sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFF256B57), RoundedCornerShape(50)))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = detail.invoiceNo ?: "Tanpa nomor nota",
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(50))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Content Scroll
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Metadata section
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetailInfoMobile("Supplier", detail.supplierName, false)
                    DetailInfoMobile("Tanggal Masuk", detail.receivedAt.asDisplayDate(), false)
                    DetailInfoMobile("Catatan", detail.notes ?: "Tidak ada catatan", true)
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Item Pembelian
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ITEM PEMBELIAN",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 1.sp
                        )
                        Box(
                            modifier = Modifier.background(Color(0xFFF1F5F9), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${detail.items.size} Macam",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    detail.items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFFEBF3F0), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Inventory2,
                                    contentDescription = null,
                                    tint = Color(0xFF256B57),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.productName,
                                    color = Color(0xFF0F172A),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${item.quantity.stripTrailingZeros().toPlainString()} sak",
                                        color = Color(0xFF334155),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = " x ",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = item.priceAtTransaction.asCurrency(),
                                        color = Color(0xFF64748B),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Text(
                                text = item.subtotal.asCurrency(),
                                color = Color(0xFF0F172A),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Payment Summary
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .background(Color(0xFFF9FBFA), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF256B57).copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal Produk", color = Color(0xFF64748B), fontSize = 12.sp)
                        Text(detail.total.asCurrency(), color = Color(0xFF334155), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Pajak & Biaya Lain", color = Color(0xFF64748B), fontSize = 12.sp)
                        Text("Rp0,00", color = Color(0xFF94A3B8), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(bottom = 12.dp).drawBehind {
                            drawLine(
                                color = Color(0xFFE2E8F0),
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                            )
                        },
                        color = Color.Transparent
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("TOTAL PEMBELIAN", color = Color(0xFF475569), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                            Text("Lunas via Kasir", color = Color(0xFF94A3B8), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        Text(
                            text = detail.total.asCurrency(),
                            color = Color(0xFF256B57),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                    }
                }
            }

            // Footer Button
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF256B57))
                ) {
                    Text("Selesai", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DetailInfoMobile(label: String, value: String, isNote: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color(0xFF64748B),
            fontSize = 14.sp
        )
        if (isNote && (value == "Tidak ada catatan" || value == "-")) {
            Box(
                modifier = Modifier
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Tidak ada catatan",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        } else {
            Text(
                text = value,
                color = Color(0xFF0F172A),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
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
    val bgColor = if (isHighlighted) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color.White
    val borderColor = if (isHighlighted) androidx.compose.ui.graphics.Color(0xFFD2E3DC) else androidx.compose.ui.graphics.Color(0xFFE2E8F0).copy(alpha = 0.9f)
    val titleColor = if (isHighlighted) androidx.compose.ui.graphics.Color(0xFF1E5646) else androidx.compose.ui.graphics.Color(0xFF94A3B8)
    val valueColor = if (isHighlighted) androidx.compose.ui.graphics.Color(0xFF256B57) else androidx.compose.ui.graphics.Color(0xFF0F172A)

    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        shadowElevation = if (isHighlighted) 4.dp else 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = title,
                    color = titleColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = value,
                    color = valueColor,
                    fontSize = if (isHighlighted) 26.sp else 24.sp,
                    fontWeight = if (isHighlighted) FontWeight.ExtraBold else FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = note,
                    color = androidx.compose.ui.graphics.Color(0xFF64748B),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconBgColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
