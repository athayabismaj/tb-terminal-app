@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.tbterminal.app.ui.purchasehistory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight

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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState

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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.tbterminal.app.ui.components.TbPagination
import com.tbterminal.app.ui.theme.TbBackground
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PurchaseBackground = Color(0xFFF6F8F7)
private val PurchaseBorder = Color(0xFFE2E8F0)
private val PurchaseText = Color(0xFF0F172A)
private val PurchaseMuted = Color(0xFF64748B)
private val PurchasePrimary = Color(0xFF059669)
private val PurchaseSoft = Color(0xFFF8FAFC)

@Composable
internal fun PurchaseHistoryScreen(
    modifier: Modifier,
    uiState: PurchaseHistoryUiState,
    isSearchExpanded: Boolean = false,
    onToggleSearch: () -> Unit = {},
    showFilterPopup: Boolean = false,
    onDismissFilter: () -> Unit = {},
    showSummaryPopup: Boolean = false,
    onDismissSummary: () -> Unit = {},
    onFilterApplied: (String?, String?, String?, String, PurchaseSortOrder) -> Unit,
    onResetAllFilters: () -> Unit = {},
    onSearchChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onShowDetail: (String) -> Unit,
    onDismissDetail: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onAddPurchase: () -> Unit = {}
) {
    BoxWithConstraints(modifier.fillMaxSize().background(PurchaseBackground)) {
        val compact = maxWidth < 700.dp
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = if (compact) 16.dp else 28.dp, vertical = if (compact) 12.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 16.dp)
        ) {
            PurchaseHeaderToolbar(
                uiState = uiState,
                compact = compact,
                isSearchExpanded = isSearchExpanded,
                onToggleSearch = onToggleSearch,
                onSearchChanged = onSearchChanged,
                onAddPurchase = onAddPurchase,
                onClearSupplierFilter = {
                    onFilterApplied(null, uiState.startDate, uiState.endDate, uiState.selectedPeriodType, uiState.sortOrder)
                },
                onClearDateFilter = {
                    val today = todayPurchaseDateString()
                    onFilterApplied(uiState.selectedSupplierId, today, today, "Hari Ini", uiState.sortOrder)
                },
                onClearSortFilter = {
                    onFilterApplied(uiState.selectedSupplierId, uiState.startDate, uiState.endDate, uiState.selectedPeriodType, PurchaseSortOrder.NEWEST)
                },
                onResetAllFilters = onResetAllFilters
            )
            PurchaseTable(uiState, compact, onRefresh, onShowDetail, onPreviousPage, onNextPage)
        }
    }
    
    if (showFilterPopup) {
        PurchaseFilterBottomSheet(
            uiState = uiState,
            onDismiss = onDismissFilter,
            onApplyFilter = onFilterApplied
        )
    }

    if (showSummaryPopup) {
        PurchaseSummaryBottomSheet(
            uiState = uiState,
            onDismiss = onDismissSummary
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
    Column(Modifier.fillMaxWidth()) {
        if (compact) {
            androidx.compose.material3.Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 1.dp
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
private fun formatPeriodDisplayDate(
    startDateStr: String?,
    endDateStr: String?,
    periodType: String
): String {
    if (periodType == "Semua") return "Semua"
    if (startDateStr == null) return periodType
    val start = runCatching { LocalDate.parse(startDateStr.take(10)) }.getOrNull()
    val end = runCatching { LocalDate.parse(endDateStr?.take(10).orEmpty()) }.getOrNull() ?: start
    if (start == null) return periodType

    val idLocale = Locale.forLanguageTag("id-ID")
    val dayMonth = DateTimeFormatter.ofPattern("d MMM", idLocale)
    val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", idLocale)
    val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", idLocale)
    val today = LocalDate.now(ZoneId.of("Asia/Jakarta"))

    return when (periodType) {
        "Hari", "Hari Ini" -> {
            if (start == today) {
                "Hari Ini (${start.format(dayMonth)})"
            } else {
                start.format(dayMonthYear)
            }
        }
        "Minggu", "Minggu Ini" -> {
            if (end != null && start != end) {
                if (start.month == end.month && start.year == end.year) {
                    "${start.dayOfMonth} - ${end.format(dayMonthYear)}"
                } else {
                    "${start.format(dayMonth)} - ${end.format(dayMonthYear)}"
                }
            } else {
                start.format(dayMonthYear)
            }
        }
        "Bulan", "Bulan Ini" -> {
            start.format(monthYear)
        }
        else -> {
            if (end != null && start != end) {
                if (start.month == end.month && start.year == end.year) {
                    "${start.dayOfMonth} - ${end.format(dayMonthYear)}"
                } else {
                    "${start.format(dayMonth)} - ${end.format(dayMonthYear)}"
                }
            } else {
                start.format(dayMonthYear)
            }
        }
    }
}

@Composable
private fun PurchaseHeaderToolbar(
    uiState: PurchaseHistoryUiState,
    compact: Boolean,
    isSearchExpanded: Boolean,
    onToggleSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onAddPurchase: () -> Unit,
    onClearSupplierFilter: () -> Unit,
    onClearDateFilter: () -> Unit,
    onClearSortFilter: () -> Unit,
    onResetAllFilters: () -> Unit,
) {
    val todayStr = remember { todayPurchaseDateString() }
    val hasSupplierFilter = uiState.selectedSupplierId != null
    val hasDateFilter = when (uiState.selectedPeriodType) {
        "Semua" -> true
        "Hari Ini", "Hari" -> uiState.startDate != todayStr || uiState.endDate != todayStr
        else -> true
    }
    val hasSortFilter = uiState.sortOrder != PurchaseSortOrder.NEWEST
    val isFiltered = uiState.isFiltered
    val isSearchActive = isSearchExpanded || uiState.searchQuery.isNotEmpty()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Expandable Search Bar (When Search is open or query is present)
        AnimatedVisibility(
            visible = isSearchActive,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF256B57).copy(alpha = 0.5f)),
                shadowElevation = 1.dp
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
                        tint = Color(0xFF256B57),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (uiState.searchQuery.isEmpty()) {
                            Text(
                                "Cari nomor nota atau supplier...",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        BasicTextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchChanged,
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color(0xFF0F172A),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("purchase-history-search"),
                        )
                    }
                    IconButton(
                        onClick = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                onSearchChanged("")
                            } else {
                                onToggleSearch()
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Tutup pencarian",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        // 1. Active Filters Row (jika filter aktif)
        if (isFiltered) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Filter:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
                if (hasSupplierFilter) {
                    val supplierName = uiState.suppliers.firstOrNull { it.id == uiState.selectedSupplierId }?.name ?: "Supplier"
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Outlined.Business, null, tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                            Text(
                                text = supplierName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF334155),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 120.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onClearSupplierFilter),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Close, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(11.dp))
                            }
                        }
                    }
                }
                if (hasDateFilter) {
                    val periodLabel = formatPeriodDisplayDate(uiState.startDate, uiState.endDate, uiState.selectedPeriodType)
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Outlined.CalendarMonth, null, tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                            Text(
                                text = periodLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF334155),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onClearDateFilter),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Close, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(11.dp))
                            }
                        }
                    }
                }
                if (hasSortFilter) {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Sort, null, tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                            Text(
                                text = uiState.sortOrder.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF334155),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onClearSortFilter),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Close, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(11.dp))
                            }
                        }
                    }
                }
                Text(
                    text = "Reset",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFEF4444),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onResetAllFilters)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }

        // 2. Title & Action Row (selalu tampil)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Daftar Pembelian",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            // Right: Tombol Catat (Sage Green Brand)
            Surface(
                onClick = onAddPurchase,
                modifier = Modifier
                    .height(36.dp)
                    .testTag("purchase-history-add"),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF256B57),
                contentColor = Color.White,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Catat",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun PurchaseSummaryBottomSheet(
    uiState: PurchaseHistoryUiState,
    onDismiss: () -> Unit
) {
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
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            // Modal Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ringkasan Pembelian",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Informasi ringkas transaksi periode berjalan",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Tutup ringkasan modal",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Content Metrics List (Clean & Minimalist Row Design)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
            ) {
                // Row 1: Total Nilai (Featured Highlight)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Total Nilai",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155)
                        )
                        Text(
                            text = "Maksimal ${uiState.pageSize} nota",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Text(
                        text = uiState.pageTotal.asCurrencyNoDecimals(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF256B57),
                        letterSpacing = (-0.5).sp
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                // Row 2: Total Nota
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Total nota",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF475569)
                        )
                        Text(
                            text = "Keseluruhan input",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
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

                // Row 3: Supplier Aktif
                val activeSupplierCount = when {
                    uiState.selectedSupplierId != null -> 1
                    uiState.purchases.isNotEmpty() -> uiState.purchases.map { it.supplierName }.distinct().size
                    else -> uiState.suppliers.size
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Supplier aktif",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF475569)
                        )
                        Text(
                            text = "Mitra terhubung",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
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

            // Action Button Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF256B57),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = "Selesai",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseFilterBottomSheet(
    uiState: PurchaseHistoryUiState,
    onDismiss: () -> Unit,
    onApplyFilter: (String?, String?, String?, String, PurchaseSortOrder) -> Unit
) {
    val today = remember { LocalDate.now(ZoneId.of("Asia/Jakarta")) }
    var selectedPeriod by remember { 
        mutableStateOf(
            when (uiState.selectedPeriodType) {
                "Hari" -> "Hari Ini"
                "Minggu" -> "Minggu Ini"
                "Bulan" -> "Bulan Ini"
                else -> uiState.selectedPeriodType.ifBlank { "Hari Ini" }
            }
        ) 
    }
    var selectedSupplierId by remember { mutableStateOf(uiState.selectedSupplierId) }
    var selectedSortOrder by remember { mutableStateOf(uiState.sortOrder) }

    val initialStart = remember(uiState.startDate) {
        uiState.startDate?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
    }
    val initialEnd = remember(uiState.endDate) {
        uiState.endDate?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
    }

    var startDate by remember { mutableStateOf(initialStart) }
    var endDate by remember { mutableStateOf(initialEnd) }
    var showDatePicker by remember { mutableStateOf(false) }
    var supplierSearchQuery by remember { mutableStateOf("") }
    var isSupplierSearchVisible by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(5.dp)
                    .background(Color(0xFFCBD5E1), CircleShape)
            )
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Filter Pembelian",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Atur periode waktu dan supplier pembelian",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 17.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Tutup filter",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // Section 1: Periode Waktu
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "PERIODE WAKTU",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.8.sp
                    )

                    // 2x2 Grid: [Semua, Hari Ini], [Minggu Ini, Bulan Ini]
                    val periods = listOf(
                        listOf("Semua" to "Semua", "Hari Ini" to "Hari Ini"),
                        listOf("Minggu Ini" to "Minggu Ini", "Bulan Ini" to "Bulan Ini")
                    )
                    periods.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { (key, label) ->
                                val isSelected = selectedPeriod == key
                                Surface(
                                    onClick = {
                                        selectedPeriod = key
                                        when (key) {
                                            "Semua" -> Unit
                                            "Hari Ini" -> {
                                                startDate = today
                                                endDate = today
                                            }
                                            "Minggu Ini" -> {
                                                startDate = today.with(DayOfWeek.MONDAY)
                                                endDate = today.with(DayOfWeek.SUNDAY)
                                            }
                                            "Bulan Ini" -> {
                                                startDate = today.withDayOfMonth(1)
                                                endDate = today.withDayOfMonth(today.lengthOfMonth())
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) Color(0xFF256B57) else Color(0xFFE2E8F0)
                                    ),
                                    shadowElevation = 0.dp
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                            color = if (isSelected) Color(0xFF256B57) else Color(0xFF334155)
                                        )
                                        if (isSelected) {
                                            Icon(
                                                Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF256B57),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Pilih Tanggal Kustom Button
                    val isCustom = selectedPeriod == "Kustom"
                    val customText = if (isCustom) {
                        val df = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("id-ID"))
                        if (startDate == endDate) startDate.format(df) else "${startDate.format(df)} - ${endDate.format(df)}"
                    } else {
                        "Pilih Tanggal Kustom"
                    }
                    Surface(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(
                            if (isCustom) 1.5.dp else 1.dp,
                            if (isCustom) Color(0xFF256B57) else Color(0xFFCBD5E1)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.CalendarMonth,
                                    contentDescription = null,
                                    tint = if (isCustom) Color(0xFF256B57) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = customText,
                                    fontSize = 13.5.sp,
                                    fontWeight = if (isCustom) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isCustom) Color(0xFF256B57) else Color(0xFF475569)
                                )
                            }
                            Text(
                                text = "Atur >",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF256B57)
                            )
                        }
                    }
                }

                // Section 2: Supplier
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SUPPLIER",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.8.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${uiState.suppliers.size} Terdaftar",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF94A3B8)
                            )
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (isSupplierSearchVisible || supplierSearchQuery.isNotEmpty()) Color(0xFFE2E8F0) else Color(0xFFF1F5F9))
                                    .clickable {
                                        isSupplierSearchVisible = !isSupplierSearchVisible
                                        if (!isSupplierSearchVisible) {
                                            supplierSearchQuery = ""
                                        }
                                    }
                                    .testTag("purchase-filter-toggle-supplier-search"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = "Cari supplier",
                                    tint = if (isSupplierSearchVisible || supplierSearchQuery.isNotEmpty()) Color(0xFF256B57) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Mini Search bar for supplier
                    AnimatedVisibility(
                        visible = isSupplierSearchVisible || supplierSearchQuery.isNotEmpty(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                    if (supplierSearchQuery.isEmpty()) {
                                        Text(
                                            "Cari supplier...",
                                            fontSize = 13.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                    BasicTextField(
                                        value = supplierSearchQuery,
                                        onValueChange = { supplierSearchQuery = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = Color(0xFF0F172A),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (supplierSearchQuery.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .clickable { supplierSearchQuery = "" },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.Close,
                                            contentDescription = "Hapus",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Scrollable supplier list
                    val filteredSuppliers = remember(uiState.suppliers, supplierSearchQuery) {
                        if (supplierSearchQuery.isBlank()) uiState.suppliers
                        else uiState.suppliers.filter { it.name.contains(supplierSearchQuery, ignoreCase = true) }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (supplierSearchQuery.isBlank() || "semua supplier".contains(supplierSearchQuery, ignoreCase = true)) {
                                val isAllSelected = selectedSupplierId == null
                                Surface(
                                    onClick = { selectedSupplierId = null },
                                    modifier = Modifier.fillMaxWidth().height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(
                                        if (isAllSelected) 1.5.dp else 1.dp,
                                        if (isAllSelected) Color(0xFF256B57) else Color(0xFFE2E8F0)
                                    ),
                                    shadowElevation = 0.dp
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Semua Supplier",
                                            fontSize = 13.5.sp,
                                            fontWeight = if (isAllSelected) FontWeight.SemiBold else FontWeight.Medium,
                                            color = if (isAllSelected) Color(0xFF256B57) else Color(0xFF334155)
                                        )
                                        if (isAllSelected) {
                                            Icon(
                                                Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF256B57),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            filteredSuppliers.forEach { supplier ->
                                val isSelected = selectedSupplierId == supplier.id
                                Surface(
                                    onClick = { selectedSupplierId = supplier.id },
                                    modifier = Modifier.fillMaxWidth().height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) Color(0xFF256B57) else Color(0xFFE2E8F0)
                                    ),
                                    shadowElevation = 0.dp
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = supplier.name,
                                            fontSize = 13.5.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                            color = if (isSelected) Color(0xFF256B57) else Color(0xFF334155),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (isSelected) {
                                            Icon(
                                                Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF256B57),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 3: Urutkan Berdasarkan
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "URUTKAN BERDASARKAN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.8.sp
                    )
                    val sortRows = listOf(
                        listOf(PurchaseSortOrder.NEWEST to Icons.Outlined.Schedule, PurchaseSortOrder.NAME_ASC to Icons.AutoMirrored.Outlined.Sort),
                        listOf(PurchaseSortOrder.HIGHEST_AMOUNT to Icons.Outlined.Paid, PurchaseSortOrder.DUE_NEAREST to Icons.Outlined.CalendarMonth)
                    )
                    sortRows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { (order, icon) ->
                                val isSelected = selectedSortOrder == order
                                Surface(
                                    onClick = { selectedSortOrder = order },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) Color(0xFF256B57) else Color(0xFFE2E8F0)
                                    ),
                                    shadowElevation = 0.dp
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Icon(
                                                icon,
                                                contentDescription = null,
                                                tint = if (isSelected) Color(0xFF256B57) else Color(0xFF64748B),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = order.label,
                                                fontSize = 13.5.sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                                color = if (isSelected) Color(0xFF256B57) else Color(0xFF334155),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = Color(0xFF256B57),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Sticky Footer Actions
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset Button (2/5 width)
                    Surface(
                        onClick = {
                            selectedPeriod = "Hari Ini"
                            startDate = today
                            endDate = today
                            selectedSupplierId = null
                            selectedSortOrder = PurchaseSortOrder.NEWEST
                            supplierSearchQuery = ""
                            isSupplierSearchVisible = false
                        },
                        modifier = Modifier
                            .weight(2f)
                            .height(48.dp)
                            .testTag("purchase-history-filter-reset"),
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.5.dp, Color(0xFF256B57))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Reset",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF256B57)
                            )
                        }
                    }

                    // Terapkan Button (3/5 width)
                    Button(
                        onClick = {
                            val (startIso, endIso) = when (selectedPeriod) {
                                "Semua" -> null to null
                                else -> "${startDate}T00:00:00Z" to "${endDate}T23:59:59Z"
                            }
                            onApplyFilter(selectedSupplierId, startIso, endIso, selectedPeriod, selectedSortOrder)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(3f)
                            .height(48.dp)
                            .testTag("purchase-history-filter-done"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF256B57),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                    ) {
                        Text(
                            text = "Terapkan",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // iOS home bar pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(128.dp)
                            .height(4.dp)
                            .background(Color(0xFFCBD5E1), CircleShape)
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        HistoryDatePickerDialog(
            currentDate = startDate.toString(),
            initialMode = if (selectedPeriod in listOf("Hari Ini", "Hari", "Minggu Ini", "Minggu", "Bulan Ini", "Bulan")) selectedPeriod else "Kustom",
            onDismiss = { showDatePicker = false },
            onConfirmRange = { newStartStr, newEndStr, mode ->
                startDate = LocalDate.parse(newStartStr)
                endDate = LocalDate.parse(newEndStr)
                selectedPeriod = "Kustom"
                showDatePicker = false
            },
            onConfirm = { dateStr ->
                val date = LocalDate.parse(dateStr)
                startDate = date
                endDate = date
                selectedPeriod = "Kustom"
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
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // 40x40 icon container with Sage Green receipt icon
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = Color(0xFF256B57),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = purchase.supplierName.ifBlank { "Tanpa Supplier" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = purchase.invoiceNo ?: "Tanpa nomor nota",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF475569),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Lunas",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF256B57)
                        )
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .background(Color(0xFFCBD5E1), CircleShape)
                        )
                        Text(
                            text = if (purchase.invoiceNo != null) "Pembelian" else "Nota Manual",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFCBD5E1),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        HorizontalDivider(
            color = Color(0xFFF1F5F9),
            thickness = 1.dp
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = purchase.receivedAt.asDisplayDate(),
                fontSize = 12.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = purchase.total.asCurrency(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
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
    val countText = if (uiState.effectiveTotalPurchases == 0L) {
        "0 nota"
    } else if (uiState.visiblePurchases.size.toLong() >= uiState.effectiveTotalPurchases) {
        "${uiState.effectiveTotalPurchases} nota"
    } else {
        "${uiState.visiblePurchases.size} dari ${uiState.effectiveTotalPurchases} nota"
    }
    TbPagination(
        currentPage = uiState.page,
        totalPages = uiState.totalPages,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage,
        supportingText = countText,
        isLoading = uiState.isLoading,
        compact = compact,
        testTag = "purchase-history-pagination"
    )
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
private fun EmptyBox(searchQuery: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFFF1F5F9), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (searchQuery.isNotBlank()) Icons.Outlined.Search else Icons.Outlined.ReceiptLong,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = if (searchQuery.isNotBlank()) "Nota tidak ditemukan" else "Belum ada nota pembelian",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF334155),
            textAlign = TextAlign.Center
        )
        Text(
            text = if (searchQuery.isNotBlank()) 
                "Coba cari nomor nota atau nama supplier yang berbeda." 
            else 
                "Gunakan tombol Catat di atas untuk mencatat nota pembelian baru.",
            fontSize = 13.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )
    }
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