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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (compact) 16.dp else 28.dp,
                    vertical = if (compact) 14.dp else 20.dp,
                )
                .testTag("purchase-history-content"),
            verticalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 16.dp),
        ) {
            if (compact) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(40.dp).background(Color(0xFFF1F5F9), CircleShape)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Kembali", tint = Color(0xFF0F172A), modifier = Modifier.size(24.dp))
                    }
                    Text("Riwayat Pembelian", color = Color(0xFF0F172A), fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                }
            }

            PurchaseToolbar(
                uiState = uiState,
                compact = compact,
                onSupplierSelected = onSupplierSelected,
                onSearchChanged = onSearchChanged,
                onOpenMobileFilters = { showMobileFilters = true },
            )
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
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (compact) 16.dp else 28.dp,
                    vertical = if (compact) 14.dp else 20.dp,
                )
                .testTag("purchase-history-content"),
            verticalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 16.dp),
        ) {
            if (compact) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(40.dp).background(Color(0xFFF1F5F9), CircleShape)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Kembali", tint = Color(0xFF0F172A), modifier = Modifier.size(24.dp))
                    }
                    Text("Riwayat Pembelian", color = Color(0xFF0F172A), fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                }
            }

            PurchaseToolbar(
                uiState = uiState,
                compact = compact,
                onSupplierSelected = onSupplierSelected,
                onSearchChanged = onSearchChanged,
                onOpenMobileFilters = { showMobileFilters = true },
            ) -> Unit,
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


@Composable
private fun PurchaseCardMobile(purchase: PurchaseSummary, onShowDetail: (String) -> Unit) {
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
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
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