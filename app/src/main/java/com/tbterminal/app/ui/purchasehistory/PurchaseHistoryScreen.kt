@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.tbterminal.app.ui.purchasehistory

import androidx.compose.foundation.BorderStroke
import com.tbterminal.app.ui.components.SkeletonBox
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable

import androidx.compose.material.icons.outlined.FactCheck

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button

import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tbterminal.app.data.model.PurchaseDetail
import com.tbterminal.app.data.model.PurchaseSummary
import com.tbterminal.app.ui.components.HistoryDatePickerDialog
import com.tbterminal.app.ui.theme.TbBackground
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PurchaseBackground = TbBackground
private val PurchaseBorder = Color(0xFFE2E8F0)
private val PurchaseText = Color(0xFF0F172A)
private val PurchaseMuted = Color(0xFF64748B)
private val PurchasePrimary = Color(0xFF059669)
private val PurchaseSoft = Color(0xFFF8FAFC)

@Composable
internal fun PurchaseHistoryScreen(
    modifier: Modifier,
    uiState: PurchaseHistoryUiState,
    onFilterApplied: (String?, String?, String?, String) -> Unit,
    onSearchChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onShowDetail: (String) -> Unit,
    onDismissDetail: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    var showFilterPopup by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier.fillMaxSize().background(PurchaseBackground)) {
        val compact = maxWidth < 700.dp
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = if (compact) 16.dp else 28.dp, vertical = if (compact) 12.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp)
        ) {
            // PurchaseSummaryCards moved
            PurchaseToolbar(uiState, compact, { showFilterPopup = true }, onSearchChanged)
            PurchaseTable(uiState, compact, onRefresh, onShowDetail, onPreviousPage, onNextPage)
        }
    }
    
    if (showFilterPopup) {
        PurchaseFilterBottomSheet(
            uiState = uiState,
            onDismiss = { showFilterPopup = false },
            onApplyFilter = { supplierId, start, end, periodType ->
                onFilterApplied(supplierId, start, end, periodType)
            }
        )
    }
    
    if (uiState.isLoadingDetail || uiState.selectedPurchase != null) {
        PurchaseDetailDialog(
            detail = uiState.selectedPurchase,
            isLoading = uiState.isLoadingDetail,
            onDismiss = onDismissDetail
        )
    }
}

@Composable
private fun PurchaseHeader() {
    Text("Nota Pembelian", fontSize = 28.sp, fontWeight = FontWeight.Medium, color = PurchaseText)
}

@Composable
private fun PurchaseSummaryCards(uiState: PurchaseHistoryUiState, compact: Boolean) {
    if (compact) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PurchaseMetric("Total nota", uiState.effectiveTotalPurchases.toString(), "Sesuai filter", Icons.Outlined.ReceiptLong, PurchasePrimary, Modifier.weight(1f), true)
            PurchaseMetric("Nilai halaman", uiState.pageTotal.asCurrency(), "Halaman ini", Icons.Outlined.MonetizationOn, Color(0xFF2563EB), Modifier.weight(1f), true)
        }
        PurchaseMetric("Supplier aktif", uiState.suppliers.size.toString(), "Tersedia pada filter", Icons.Outlined.Business, Color(0xFFF59E0B), Modifier.fillMaxWidth(), true)
    } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        PurchaseMetric("TOTAL NOTA", uiState.effectiveTotalPurchases.toString(), "Sesuai filter aktif", Icons.Outlined.ReceiptLong, PurchasePrimary, Modifier.weight(1f))
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.isLoading && uiState.purchases.isEmpty()) {
                    SkeletonBox(Modifier.width(110.dp).height(20.dp), shape = RoundedCornerShape(4.dp))
                    SkeletonBox(Modifier.size(32.dp), shape = RoundedCornerShape(12.dp))
                } else {
                    Text("Pembelian", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    if (uiState.visiblePurchases.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .clickable { showSummaryPopup = true }
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.FactCheck,
                                contentDescription = "Ringkasan",
                                tint = Color(0xFF334155),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            ) {
                when {
                    uiState.isLoading && uiState.purchases.isEmpty() -> PurchaseMobileSkeleton()
                    uiState.errorMessage != null -> PurchaseError(uiState.errorMessage, onRefresh)
                    uiState.visiblePurchases.isEmpty() -> EmptyBox(uiState.searchQuery)
                    else -> {
                        Column {
                            uiState.visiblePurchases.forEachIndexed { index, purchase ->
                                PurchaseMobileRow(purchase, onShowDetail)
                                if (index < uiState.visiblePurchases.lastIndex) {
                                    HorizontalDivider(color = Color(0xFFF1F5F9))
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
                            Box(modifier = Modifier.width(40.dp).height(4.dp).background(Color(0xFFE2E8F0), CircleShape))
                        }
                    },
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 28.dp)
                    ) {
                        // Modal Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ringkasan Pembelian",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                letterSpacing = (-0.3).sp
                            )
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable { showSummaryPopup = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "Tutup ringkasan modal",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        // Key-Value Metric Rows
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Total Nota Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.Description,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = "Total nota",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF475569)
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = uiState.effectiveTotalPurchases.toString(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Nota",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.padding(bottom = 1.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                            // Nilai Halaman Ini Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF4F9F7))
                                    .padding(horizontal = 24.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Color(0xFFEBF4F1), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.Paid,
                                            contentDescription = null,
                                            tint = Color(0xFF256B57),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Nilai halaman ini",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF334155),
                                            lineHeight = 18.sp
                                        )
                                        Text(
                                            text = "Maksimal ${uiState.pageSize} nota",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                                Text(
                                    text = uiState.pageTotal.asCurrencyNoDecimals(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF256B57),
                                    letterSpacing = (-0.3).sp
                                )
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                            // Supplier Aktif Row
                            val activeSupplierCount = when {
                                uiState.selectedSupplierId != null -> 1
                                uiState.purchases.isNotEmpty() -> uiState.purchases.map { it.supplierName }.distinct().size
                                else -> uiState.suppliers.size
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.Store,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = "Supplier aktif",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF475569)
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = activeSupplierCount.toString(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Supplier",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.padding(bottom = 1.dp)
                                    )
                                }
                            }
                        }

                        // Action Button
                        Spacer(Modifier.height(24.dp))
                        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                            Button(
                                onClick = { showSummaryPopup = false },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF256B57),
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                            ) {
                                Text(
                                    text = "Selesai",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            PurchaseTableHeader()
            when {
                uiState.isLoading && uiState.purchases.isEmpty() -> PurchaseTableSkeleton()
                uiState.errorMessage != null -> PurchaseError(uiState.errorMessage, onRefresh)
                uiState.visiblePurchases.isEmpty() -> androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
        if (uiState.visiblePurchases.isNotEmpty()) {
            PurchasePagination(uiState, compact, onPreviousPage, onNextPage)
        }
    }
}

@Composable
private fun PurchaseToolbar(
    uiState: PurchaseHistoryUiState,
    compact: Boolean,
    onFilterClick: () -> Unit,
    onSearchChanged: (String) -> Unit
) {
    if (uiState.isLoading && uiState.purchases.isEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonBox(
                modifier = Modifier.weight(1f).height(if (compact) 42.dp else 48.dp),
                shape = RoundedCornerShape(if (compact) 12.dp else 14.dp),
            )
            SkeletonBox(
                modifier = Modifier.size(if (compact) 42.dp else 48.dp),
                shape = RoundedCornerShape(if (compact) 12.dp else 14.dp),
            )
        }
        return
    }

    val isFiltered = uiState.selectedSupplierId != null || (uiState.startDate != null && uiState.selectedPeriodType != "Hari")
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PurchaseSearchField(
            value = uiState.searchQuery,
            onValueChange = onSearchChanged,
            modifier = Modifier.weight(1f),
            compact = compact,
        )
        Surface(
            onClick = onFilterClick,
            modifier = Modifier
                .size(if (compact) 42.dp else 48.dp)
                .testTag("purchase-history-open-filters"),
            shape = RoundedCornerShape(if (compact) 12.dp else 14.dp),
            color = Color.White,
            border = BorderStroke(1.dp, if (isFiltered) Color(0xFF0F172A) else Color(0xFFCBD5E1)),
            contentColor = Color(0xFF0F172A),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.Tune,
                    contentDescription = "Filter nota pembelian",
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF0F172A),
                )
                if (isFiltered) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(8.dp)
                            .background(Color(0xFFF59E0B), CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun PurchaseSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    compact: Boolean = false,
) {
    Surface(
        modifier = modifier
            .height(if (compact) 42.dp else 48.dp)
            .testTag("purchase-history-search"),
        color = Color.White,
        shape = RoundedCornerShape(if (compact) 12.dp else 14.dp),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        "Cari nomor nota atau supplier",
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color(0xFF0F172A),
                        fontSize = 14.sp,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Hapus pencarian",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseFilterBottomSheet(
    uiState: PurchaseHistoryUiState,
    onDismiss: () -> Unit,
    onApplyFilter: (String?, String?, String?, String) -> Unit
) {
    val today = remember { LocalDate.now(ZoneId.of("Asia/Jakarta")) }
    var selectedSupplierId by remember { mutableStateOf(uiState.selectedSupplierId) }
    var selectedPeriod by remember { mutableStateOf(uiState.selectedPeriodType.ifBlank { "Hari" }) }

    val initialStart = remember(uiState.startDate) {
        uiState.startDate?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
    }
    val initialEnd = remember(uiState.endDate) {
        uiState.endDate?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
    }

    var startDate by remember { mutableStateOf(initialStart) }
    var endDate by remember { mutableStateOf(initialEnd) }
    var showDatePicker by remember { mutableStateOf(false) }

    val displayDate = when (selectedPeriod) {
        "Hari" -> startDate.format(DateTimeFormatter.ofPattern("dd/MM"))
        "Minggu" -> "${startDate.format(DateTimeFormatter.ofPattern("dd/MM"))} - ${endDate.format(DateTimeFormatter.ofPattern("dd/MM"))}"
        "Bulan" -> "${startDate.format(DateTimeFormatter.ofPattern("dd/MM"))} - ${endDate.format(DateTimeFormatter.ofPattern("dd/MM"))}"
        else -> if (startDate == endDate) startDate.format(DateTimeFormatter.ofPattern("dd/MM")) else "${startDate.format(DateTimeFormatter.ofPattern("dd/MM"))} - ${endDate.format(DateTimeFormatter.ofPattern("dd/MM"))}"
    }

    var supplierExpanded by remember { mutableStateOf(false) }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.width(40.dp).height(4.dp).background(Color(0xFFE2E8F0), CircleShape))
            }
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Filter Pembelian",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.3).sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Atur periode waktu dan supplier",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Tutup filter",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Section: PERIODE
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "PERIODE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.8.sp,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Segmented control
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            listOf(
                                "Hari" to "Hari",
                                "Minggu" to "Minggu",
                                "Bulan" to "Bulan",
                            ).forEach { (key, label) ->
                                val isSelected = selectedPeriod == key
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(8.dp))
                                        .then(
                                            if (isSelected) {
                                                Modifier
                                                    .shadow(elevation = 1.dp, shape = RoundedCornerShape(8.dp))
                                                    .background(Color.White)
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable {
                                            selectedPeriod = key
                                            when (key) {
                                                "Hari" -> {
                                                    startDate = today
                                                    endDate = today
                                                }
                                                "Minggu" -> {
                                                    startDate = today.with(DayOfWeek.MONDAY)
                                                    endDate = today.with(DayOfWeek.SUNDAY)
                                                }
                                                "Bulan" -> {
                                                    startDate = today.withDayOfMonth(1)
                                                    endDate = today.withDayOfMonth(today.lengthOfMonth())
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFF0F172A) else Color(0xFF64748B),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }

                        // Date button
                        Box(
                            modifier = Modifier
                                .height(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE8F2EE))
                                .border(
                                    BorderStroke(1.dp, Color(0xFFB8D8CD).copy(alpha = 0.6f)),
                                    RoundedCornerShape(12.dp),
                                )
                                .clickable(onClick = { showDatePicker = true })
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.CalendarMonth,
                                    contentDescription = "Pilih tanggal",
                                    tint = Color(0xFF256B57),
                                    modifier = Modifier.size(15.dp),
                                )
                                Text(
                                    text = displayDate,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF256B57),
                                )
                            }
                        }
                    }
                }

                // Section: SUPPLIER
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SUPPLIER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.8.sp,
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8FAF9))
                                .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(12.dp))
                                .clickable { supplierExpanded = true }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = uiState.suppliers.firstOrNull { it.id == selectedSupplierId }?.name ?: "Semua Supplier",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF0F172A),
                                )
                                Icon(
                                    Icons.Outlined.ExpandMore,
                                    contentDescription = "Pilih supplier",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = supplierExpanded,
                            onDismissRequest = { supplierExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f).background(Color.White),
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Semua Supplier",
                                        fontSize = 13.sp,
                                        fontWeight = if (selectedSupplierId == null) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedSupplierId == null) Color(0xFF256B57) else Color(0xFF0F172A),
                                    )
                                },
                                onClick = {
                                    selectedSupplierId = null
                                    supplierExpanded = false
                                }
                            )
                            uiState.suppliers.forEach { supplier ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            supplier.name,
                                            fontSize = 13.sp,
                                            fontWeight = if (selectedSupplierId == supplier.id) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedSupplierId == supplier.id) Color(0xFF256B57) else Color(0xFF0F172A),
                                        )
                                    },
                                    onClick = {
                                        selectedSupplierId = supplier.id
                                        supplierExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Action Button (Selesai)
                Button(
                    onClick = {
                        val startIso = "${startDate}T00:00:00Z"
                        val endIso = "${endDate}T23:59:59Z"
                        onApplyFilter(selectedSupplierId, startIso, endIso, selectedPeriod)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("purchase-history-filter-done"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF256B57),
                        contentColor = Color.White,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                ) {
                    Text(
                        text = "Selesai",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp,
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        HistoryDatePickerDialog(
            currentDate = startDate.toString(),
            initialMode = selectedPeriod,
            onDismiss = { showDatePicker = false },
            onConfirmRange = { newStartStr, newEndStr, mode ->
                startDate = LocalDate.parse(newStartStr)
                endDate = LocalDate.parse(newEndStr)
                selectedPeriod = mode
                showDatePicker = false
            },
            onConfirm = { dateStr ->
                val date = LocalDate.parse(dateStr)
                when (selectedPeriod) {
                    "Hari" -> {
                        startDate = date
                        endDate = date
                    }
                    "Minggu" -> {
                        startDate = date.with(DayOfWeek.MONDAY)
                        endDate = date.with(DayOfWeek.SUNDAY)
                    }
                    "Bulan" -> {
                        startDate = date.withDayOfMonth(1)
                        endDate = date.withDayOfMonth(date.lengthOfMonth())
                    }
                }
                showDatePicker = false
            }
        )
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Description,
                            contentDescription = null,
                            tint = Color(0xFF1E293B),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = purchase.invoiceNo ?: "Tanpa nomor nota",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = purchase.supplierName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Text(
                            text = " • ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFFCBD5E1),
                        )
                        Text(
                            text = if (purchase.invoiceNo != null) "Pembelian" else "Nota Manual",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp),
            )
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = Color(0xFFF1F5F9))
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "TANGGAL MASUK",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFF94A3B8),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = purchase.receivedAt.asDisplayDate(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "TOTAL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.End,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = purchase.total.asCurrency(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF256B57),
                    textAlign = TextAlign.End,
                )
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
            .clickable { onShowDetail(purchase.id) }
            .background(if (index % 2 == 0) Color.White else PurchaseSoft.copy(alpha = 0.7f))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(purchase.invoiceNo ?: "Tanpa nomor nota", Modifier.weight(1.6f), color = PurchaseText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(purchase.supplierName, Modifier.weight(1.8f), color = PurchaseMuted, fontSize = 13.sp)
        Text(purchase.receivedAt.asDisplayDate(), Modifier.weight(1.4f), color = PurchaseMuted, fontSize = 13.sp)
        Text(purchase.total.asCurrency(), Modifier.weight(1.2f), color = PurchaseText, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Box(Modifier.weight(0.5f)) {
            IconButton(onClick = { onShowDetail(purchase.id) }) {
                Icon(
                    Icons.Outlined.Description,
                    contentDescription = "Lihat rincian",
                    tint = Color(0xFF1E293B),
                )
            }
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val countText = if (uiState.effectiveTotalPurchases == 0L) {
                "0 nota"
            } else if (uiState.visiblePurchases.size.toLong() >= uiState.effectiveTotalPurchases) {
                "${uiState.effectiveTotalPurchases} nota"
            } else {
                "${uiState.visiblePurchases.size} dari ${uiState.effectiveTotalPurchases} nota"
            }
            Surface(
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            ) {
                Text(
                    text = countText,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569)
                )
            }
            Surface(
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val canPrev = uiState.page > 1 && !uiState.isLoading
                    val canNext = uiState.page < uiState.totalPages && !uiState.isLoading
                    IconButton(
                        onClick = onPreviousPage,
                        enabled = canPrev,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Halaman sebelumnya",
                            tint = if (canPrev) Color(0xFF475569) else Color(0xFFCBD5E1),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "${uiState.page} / ${uiState.totalPages.coerceAtLeast(1)}",
                        modifier = Modifier.padding(horizontal = 8.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF334155)
                    )
                    IconButton(
                        onClick = onNextPage,
                        enabled = canNext,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Halaman berikutnya",
                            tint = if (canNext) Color(0xFF475569) else Color(0xFFCBD5E1),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    } else {
        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val desktopText = if (uiState.effectiveTotalPurchases == 0L) {
                "Menampilkan 0 nota"
            } else {
                "Menampilkan ${uiState.pageStartIndex}-${uiState.pageEndIndex} dari ${uiState.effectiveTotalPurchases} nota"
            }
            Text(desktopText, color = PurchaseMuted, fontSize = 12.sp)
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseDetailDialog(detail: PurchaseDetail?, isLoading: Boolean = false, onDismiss: () -> Unit) {
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
        if (detail == null) {
            PurchaseDetailSkeletonContent(onDismiss = onDismiss)
        } else {
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
private fun PurchaseMobileSkeleton() {
    Column {
        repeat(3) { index ->
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SkeletonBox(Modifier.size(40.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            SkeletonBox(Modifier.fillMaxWidth(0.5f).height(14.dp))
                            Spacer(Modifier.height(4.dp))
                            SkeletonBox(Modifier.fillMaxWidth(0.35f).height(12.dp))
                        }
                    }
                    SkeletonBox(Modifier.size(16.dp))
                }
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        SkeletonBox(Modifier.width(80.dp).height(10.dp))
                        Spacer(Modifier.height(4.dp))
                        SkeletonBox(Modifier.width(100.dp).height(14.dp))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        SkeletonBox(Modifier.width(50.dp).height(10.dp))
                        Spacer(Modifier.height(4.dp))
                        SkeletonBox(Modifier.width(80.dp).height(16.dp))
                    }
                }
            }
            if (index < 2) {
                HorizontalDivider(color = Color(0xFFF1F5F9))
            }
        }
    }
}

@Composable
private fun PurchaseTableSkeleton() {
    Column {
        repeat(5) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (index % 2 == 0) Color.White else PurchaseSoft.copy(alpha = 0.7f))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonBox(Modifier.weight(1.6f).height(16.dp).padding(end = 12.dp))
                SkeletonBox(Modifier.weight(1.8f).height(16.dp).padding(end = 12.dp))
                SkeletonBox(Modifier.weight(1.4f).height(16.dp).padding(end = 12.dp))
                SkeletonBox(Modifier.weight(1.2f).height(16.dp).padding(end = 12.dp))
                SkeletonBox(Modifier.size(24.dp))
            }
            HorizontalDivider(color = PurchaseBorder)
        }
    }
}

@Composable
private fun PurchaseDetailSkeletonContent(onDismiss: () -> Unit) {
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
                SkeletonBox(Modifier.width(180.dp).height(18.dp))
                Spacer(Modifier.height(6.dp))
                SkeletonBox(Modifier.width(120.dp).height(12.dp))
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SkeletonBox(Modifier.width(80.dp).height(14.dp))
                    SkeletonBox(Modifier.width(120.dp).height(14.dp))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SkeletonBox(Modifier.width(90.dp).height(14.dp))
                    SkeletonBox(Modifier.width(140.dp).height(14.dp))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SkeletonBox(Modifier.width(60.dp).height(14.dp))
                    SkeletonBox(Modifier.width(100.dp).height(14.dp))
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SkeletonBox(Modifier.width(110.dp).height(12.dp))
                    SkeletonBox(Modifier.width(60.dp).height(12.dp))
                }
                repeat(2) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SkeletonBox(Modifier.size(44.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            SkeletonBox(Modifier.fillMaxWidth(0.6f).height(14.dp))
                            Spacer(Modifier.height(4.dp))
                            SkeletonBox(Modifier.width(90.dp).height(12.dp))
                        }
                        SkeletonBox(Modifier.width(70.dp).height(14.dp))
                    }
                }
            }

            SkeletonBox(Modifier.fillMaxWidth().height(88.dp))
        }

        HorizontalDivider(color = Color(0xFFF1F5F9))
        Box(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            SkeletonBox(Modifier.fillMaxWidth().height(52.dp))
        }
    }
}

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

private fun BigDecimal.asCurrencyNoDecimals(): String {
    val symbols = java.text.DecimalFormatSymbols(Locale.forLanguageTag("id-ID")).apply {
        groupingSeparator = '.'
    }
    return "Rp" + java.text.DecimalFormat("#,##0", symbols).format(this)
}

private fun String.asDisplayDate(): String {
    return runCatching {
        OffsetDateTime.parse(this).format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.forLanguageTag("id-ID")))
    }.getOrDefault(this)
}
