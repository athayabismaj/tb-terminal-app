package com.tbterminal.app.ui.salestransactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Payments
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material.icons.outlined.FactCheck
import com.tbterminal.app.ui.components.TbPagination
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.ui.components.HistoryDatePickerDialog
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.components.TbPeriodFilterRow
import com.tbterminal.app.ui.offline.LocalPendingTransactionsCard
import com.tbterminal.app.ui.theme.TbBackground
import com.tbterminal.app.ui.theme.TbError
import com.tbterminal.app.ui.theme.TbOutline
import com.tbterminal.app.ui.theme.TbSurface
import com.tbterminal.app.ui.theme.TbText
import com.tbterminal.app.ui.theme.TbTextMuted
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale


private val WIB_ZONE = ZoneId.of("Asia/Jakarta")

private fun formatSalesPeriodDisplayDate(
    startDateStr: String?,
    endDateStr: String?,
    preset: String?
): String {
    if (preset == "Semua") return "Semua"
    val start = startDateStr?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val end = endDateStr?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: start
    val idLocale = Locale.forLanguageTag("id-ID")
    val dayMonth = DateTimeFormatter.ofPattern("d MMM", idLocale)
    val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", idLocale)
    val today = LocalDate.now(WIB_ZONE)

    return when (preset) {
        "Hari ini", "Hari" -> {
            if (start == today) "Hari Ini (${start.format(dayMonth)})"
            else start?.format(dayMonthYear) ?: "Hari Ini"
        }
        "Minggu ini", "Minggu" -> {
            if (start != null && end != null) "Minggu Ini (${start.format(dayMonth)} - ${end.format(dayMonth)})"
            else "Minggu Ini"
        }
        "Bulan ini", "Bulan" -> {
            val monthYear = DateTimeFormatter.ofPattern("MMMM yyyy", idLocale)
            start?.format(monthYear)?.replaceFirstChar { it.uppercase() } ?: "Bulan Ini"
        }
        else -> {
            if (start != null && end != null) {
                if (start == end) start.format(dayMonthYear)
                else "${start.format(dayMonth)} - ${end.format(dayMonthYear)}"
            } else preset ?: "Hari Ini"
        }
    }
}

@Composable
internal fun TransactionHistoryContent(
    state: AdminTransactionHistoryUiState,
    onSearchChanged: (String) -> Unit,
    onStatusChanged: (String) -> Unit,
    onPaymentMethodChanged: (String) -> Unit,
    onDatePresetSelected: (String) -> Unit,
    onDateSelected: (String) -> Unit,
    onDateRangeSelected: (String, String, String?) -> Unit = { _, _, _ -> },
    onSyncClick: (Long) -> Unit,
    onSyncAllClick: () -> Unit,
    onRetry: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onReceiptClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isSearchExpanded: Boolean = false,
    onToggleSearch: () -> Unit = {},
    showFilterPopup: Boolean = false,
    onDismissFilter: () -> Unit = {},
    onApplyFilter: (String, String, String?, String?, String?, SalesSortOrder) -> Unit = { _, _, _, _, _, _ -> },
    showSummaryPopup: Boolean = false,
    onDismissSummary: () -> Unit = {},
    onResetAllFilters: () -> Unit = {},
    onSortOrderChanged: (SalesSortOrder) -> Unit = {},
) {
    var showDatePicker by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier.fillMaxSize().background(TbBackground)) {
        val compact = maxWidth < 700.dp
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxSize()
                .widthIn(max = 1280.dp)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (compact) 16.dp else 28.dp,
                    vertical = if (compact) 12.dp else 20.dp,
                )
                .testTag("sales-history-content"),
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
        ) {
            if (compact) {
                val isSearchActive = isSearchExpanded || state.query.isNotEmpty()
                val todayStr = remember { LocalDate.now(WIB_ZONE).toString() }
                val isDateFiltered = state.selectedPreset != "Hari ini" ||
                    ((state.startDate != null && state.startDate != todayStr) || (state.endDate != null && state.endDate != todayStr))
                val isStatusFiltered = state.statusFilter != "Semua"
                val isPaymentFiltered = state.paymentMethodFilter != "Semua"
                val isSortFiltered = state.sortOrder != SalesSortOrder.Newest
                val isFiltered = isDateFiltered || isStatusFiltered || isPaymentFiltered || isSortFiltered

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
                            shadowElevation = 0.dp
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
                                    if (state.query.isEmpty()) {
                                        Text(
                                            "Cari transaksi, pelanggan, atau kasir...",
                                            fontSize = 13.sp,
                                            color = Color(0xFF94A3B8),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    BasicTextField(
                                        value = state.query,
                                        onValueChange = onSearchChanged,
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = Color(0xFF0F172A),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("sales-history-search"),
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (state.query.isNotEmpty()) {
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

                    // Quick Filter Bar matching HTML
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp)
                            .testTag("sales-history-quick-filters"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Filter:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                            if (isFiltered) {
                                if (isDateFiltered) {
                                    val periodLabel = formatSalesPeriodDisplayDate(state.startDate, state.endDate, state.selectedPreset)
                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = Color(0xFFF1F5F9),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Outlined.CalendarMonth, null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                            Text(
                                                text = periodLabel,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF334155),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .clickable {
                                                        val today = LocalDate.now(WIB_ZONE).toString()
                                                        onApplyFilter(state.statusFilter, state.paymentMethodFilter, today, today, "Hari ini", state.sortOrder)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Outlined.Close, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                }
                                if (isStatusFiltered) {
                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = Color(0xFFF1F5F9),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Outlined.CheckCircle, null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                            Text(
                                                text = state.statusFilter.salesStatusLabel(),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF334155),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .clickable { onStatusChanged("Semua") },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Outlined.Close, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                }
                                if (isPaymentFiltered) {
                                    val paymentLabel = salesPaymentChoices.firstOrNull { it.first.equals(state.paymentMethodFilter, true) }?.second
                                        ?: state.paymentMethodFilter
                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = Color(0xFFF1F5F9),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Outlined.Payments, null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                            Text(
                                                text = paymentLabel,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF334155),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .clickable { onPaymentMethodChanged("Semua") },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Outlined.Close, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                }
                                if (isSortFiltered) {
                                    val sortLabel = when (state.sortOrder) {
                                        SalesSortOrder.HighestAmount -> "Nominal Terbesar"
                                        SalesSortOrder.ReceiptAsc -> "Nomor Nota"
                                        SalesSortOrder.Newest -> "Terbaru"
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = Color(0xFFF1F5F9),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Outlined.Sort, null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                            Text(
                                                text = sortLabel,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF334155),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .clickable { onSortOrderChanged(SalesSortOrder.Newest) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Outlined.Close, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Default "Semua" pill matching HTML
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Outlined.CalendarMonth, null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                        Text(
                                            text = "Semua",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF334155)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .clickable(onClick = onResetAllFilters),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Outlined.Close, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(12.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // Reset button
                        Text(
                            text = "Reset",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF43F5E),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable(onClick = onResetAllFilters)
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("sales-history-reset-filters")
                        )
                    }
                }
            } else {
                SalesHistoryFilters(
                    state = state,
                    onSearchChanged = onSearchChanged,
                    onStatusChanged = onStatusChanged,
                    onPaymentMethodChanged = onPaymentMethodChanged,
                    onCalendarClick = { showDatePicker = true },
                    onDatePresetSelected = onDatePresetSelected,
                )
            }

            if (
                state.localPendingTransactions.isNotEmpty() ||
                !state.localSyncMessage.isNullOrBlank() ||
                !state.bulkSyncProgressMessage.isNullOrBlank() ||
                state.isBulkSyncing
            ) {
                LocalPendingTransactionsCard(
                    transactions = state.localPendingTransactions,
                    message = state.localSyncMessage,
                    bulkProgressMessage = state.bulkSyncProgressMessage,
                    isBulkSyncing = state.isBulkSyncing,
                    onSyncClick = onSyncClick,
                    onSyncAllClick = onSyncAllClick,
                )
            }

            SalesHistorySectionHeader(
                compact = compact,
                isLoading = state.isLoading && state.transactions.isEmpty(),
            )

            when {
                state.isLoading && state.transactions.isEmpty() -> SalesHistoryLoading(compact)
                state.error != null && state.transactions.isEmpty() -> SalesHistoryError(state.error, onRetry)
                state.transactions.isEmpty() -> SalesHistoryEmpty()
                else -> {
                    if (compact) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sales-transaction-grouped-list"),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.8f)),
                            shadowElevation = 1.dp,
                        ) {
                            Column {
                                state.transactions.forEachIndexed { index, transaction ->
                                    SalesTransactionMobileItem(transaction, onReceiptClick)
                                    if (index < state.transactions.lastIndex) {
                                        HorizontalDivider(color = Color(0xFFF1F5F9))
                                    }
                                }
                            }
                        }
                    } else {
                        SalesTransactionTable(state.transactions, onReceiptClick)
                    }
                    SalesHistoryPagination(state, compact, onPreviousPage, onNextPage)
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }

    if (showFilterPopup) {
        SalesFilterBottomSheet(
            state = state,
            onDismiss = onDismissFilter,
            onApplyFilter = { status, payment, start, end, preset, sortOrder ->
                onApplyFilter(status, payment, start, end, preset, sortOrder)
                onDismissFilter()
            },
            onResetAllFilters = onResetAllFilters,
        )
    }

    if (showSummaryPopup) {
        SalesSummaryBottomSheet(
            state = state,
            onDismiss = onDismissSummary,
        )
    }

    if (showDatePicker) {
        HistoryDatePickerDialog(
            currentDate = state.startDate ?: state.selectedDate,
            initialMode = when (state.selectedPreset) {
                "Bulan ini", "Bulan" -> "Bulan"
                "Minggu ini", "Minggu" -> "Minggu"
                else -> "Hari"
            },
            onDismiss = { showDatePicker = false },
            onConfirmRange = { newStartStr, newEndStr, mode ->
                val preset = when (mode) {
                    "Bulan" -> "Bulan ini"
                    "Minggu" -> "Minggu ini"
                    else -> "Hari ini"
                }
                onDateRangeSelected(newStartStr, newEndStr, preset)
                showDatePicker = false
            },
            onConfirm = {
                onDateSelected(it)
                showDatePicker = false
            },
        )
    }
}
@Composable
private fun SalesHistoryFilters(
    state: AdminTransactionHistoryUiState,
    onSearchChanged: (String) -> Unit,
    onStatusChanged: (String) -> Unit,
    onPaymentMethodChanged: (String) -> Unit,
    onCalendarClick: () -> Unit,
    onDatePresetSelected: (String) -> Unit,
) {
    if (state.isLoading && state.transactions.isEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth().testTag("sales-history-filters"),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonBox(Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(14.dp))
            SkeletonBox(Modifier.width(430.dp).height(48.dp), shape = RoundedCornerShape(14.dp))
            SkeletonBox(Modifier.width(190.dp).height(48.dp), shape = RoundedCornerShape(14.dp))
            SkeletonBox(Modifier.width(190.dp).height(48.dp), shape = RoundedCornerShape(14.dp))
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth().testTag("sales-history-filters"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SalesSearchField(state.query, onSearchChanged, Modifier.weight(1f), compact = false)
        SalesPeriodSelector(
            selected = state.selectedPreset,
            dateLabel = state.historyShortDateLabel(),
            onPresetSelected = onDatePresetSelected,
            onCalendarClick = onCalendarClick,
            modifier = Modifier.width(430.dp),
        )
        SalesFilterDropdown(
            state.statusFilter.salesStatusLabel(), salesStatusChoices, onStatusChanged,
            "Filter status", Modifier.width(190.dp),
        )
        SalesFilterDropdown(
            state.paymentMethodFilter.salesPaymentFilterLabel(), salesPaymentChoices,
            onPaymentMethodChanged, "Filter metode pembayaran", Modifier.width(190.dp),
        )
    }
}


@Composable
private fun SalesSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    compact: Boolean = false,
) {
    Surface(
        modifier = modifier
            .height(if (compact) 42.dp else 48.dp)
            .testTag("sales-history-search"),
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
                        "Cari transaksi, pelanggan, atau kasir",
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

@Composable
private fun SalesPeriodSelector(
    selected: String?,
    dateLabel: String,
    onPresetSelected: (String) -> Unit,
    onCalendarClick: () -> Unit,
    modifier: Modifier,
) {
    TbPeriodFilterRow(
        selectedValue = selected,
        presets = listOf(
            "Hari ini" to "Hari",
            "Minggu ini" to "Minggu",
            "Bulan ini" to "Bulan",
        ),
        dateLabel = dateLabel,
        dateSelected = selected == null,
        onPresetSelected = onPresetSelected,
        onDateClick = onCalendarClick,
        modifier = modifier,
        testTag = "sales-history-date",
        dateTestTag = "sales-history-date-picker",
    )
}

private val salesStatusChoices = listOf(
    "Semua" to "Semua status",
    "Lunas" to "Lunas",
    "DP" to "DP",
    "Hutang" to "Hutang",
    "Voided" to "Dibatalkan",
)

private val salesPaymentChoices = listOf(
    "Semua" to "Semua metode",
    "tunai" to "Tunai / Cash",
    "qris" to "QRIS",
    "transfer" to "Transfer Bank",
    "hutang" to "Hutang",
    "dp" to "DP",
)

@Composable
private fun SalesFilterDropdown(
    label: String,
    choices: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
    contentDescription: String,
    modifier: Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
            contentColor = TbText,
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(Icons.Outlined.ExpandMore, contentDescription, modifier = Modifier.size(18.dp))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = TbSurface,
            shape = RoundedCornerShape(14.dp),
        ) {
            choices.forEach { (value, display) ->
                DropdownMenuItem(
                    modifier = Modifier.testTag("sales-filter-option-$contentDescription-$value"),
                    text = { Text(display) },
                    onClick = {
                        expanded = false
                        onSelected(value)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SalesFilterBottomSheet(
    state: AdminTransactionHistoryUiState,
    onDismiss: () -> Unit,
    onApplyFilter: (String, String, String?, String?, String?, SalesSortOrder) -> Unit,
    onResetAllFilters: () -> Unit = {},
) {
    val zone = remember { java.time.ZoneId.of("Asia/Jakarta") }
    val today: java.time.LocalDate = remember { java.time.LocalDate.now(zone) }
    var selectedPreset by remember { mutableStateOf(state.selectedPreset ?: "Hari ini") }
    val initialStart = remember(state.startDate) {
        state.startDate?.take(10)?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() } ?: today
    }
    val initialEnd = remember(state.endDate) {
        state.endDate?.take(10)?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() } ?: today
    }
    var startDate by remember { mutableStateOf<java.time.LocalDate?>(if (state.selectedPreset == "Semua") null else initialStart) }
    var endDate by remember { mutableStateOf<java.time.LocalDate?>(if (state.selectedPreset == "Semua") null else initialEnd) }
    var selectedStatus by remember { mutableStateOf(state.statusFilter) }
    var selectedPaymentMethod by remember { mutableStateOf(state.paymentMethodFilter) }
    var selectedSortOrder by remember { mutableStateOf(state.sortOrder) }
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sales-history-filter-sheet")
        ) {
            // Modal Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = "Filter Penjualan",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.3).sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Atur periode, status transaksi, dan metode pembayaran",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 17.sp,
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
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup Filter",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Scrollable Filter Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // BEGIN: FilterSection - Periode Waktu
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "PERIODE WAKTU",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.8.sp,
                    )

                    val periodOptions = listOf(
                        listOf("Semua" to "Semua", "Hari ini" to "Hari Ini"),
                        listOf("Minggu ini" to "Minggu Ini", "Bulan ini" to "Bulan Ini")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        periodOptions.forEach { rowPairs ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowPairs.forEach { (key, label) ->
                                    val isSelected = selectedPreset == key
                                    SalesFilterOptionCard(
                                        label = label,
                                        isSelected = isSelected,
                                        onClick = {
                                            selectedPreset = key
                                            when (key) {
                                                "Semua" -> {
                                                    startDate = null
                                                    endDate = null
                                                }
                                                "Hari ini" -> {
                                                    startDate = today
                                                    endDate = today
                                                }
                                                "Minggu ini" -> {
                                                    startDate = today.with(java.time.DayOfWeek.MONDAY)
                                                    endDate = today.with(java.time.DayOfWeek.SUNDAY)
                                                }
                                                "Bulan ini" -> {
                                                    startDate = today.withDayOfMonth(1)
                                                    endDate = today.withDayOfMonth(today.lengthOfMonth())
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        testTag = "sales-filter-period-$key"
                                    )
                                }
                            }
                        }
                    }

                    // Custom Date Range Button
                    Surface(
                        onClick = { showDatePicker = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("sales-history-custom-date-btn"),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarMonth,
                                    contentDescription = null,
                                    tint = if (selectedPreset == "Kustom") Color(0xFF256B57) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                                val customText = if (selectedPreset == "Kustom" && startDate != null && endDate != null) {
                                    if (startDate == endDate) {
                                        startDate!!.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                    } else {
                                        "${startDate!!.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"))} - ${endDate!!.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"))}"
                                    }
                                } else {
                                    "Pilih Tanggal Kustom"
                                }
                                Text(
                                    text = customText,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedPreset == "Kustom") FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (selectedPreset == "Kustom") Color(0xFF256B57) else Color(0xFF475569)
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

                // BEGIN: FilterSection - Status Transaksi
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "STATUS TRANSAKSI",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.8.sp,
                    )

                    val statusRows = listOf(
                        listOf("Semua" to "Semua Status", "Lunas" to "Lunas"),
                        listOf("DP" to "DP", "Hutang" to "Belum Lunas")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        statusRows.forEach { rowPairs ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowPairs.forEach { (key, label) ->
                                    val isSelected = selectedStatus.equals(key, ignoreCase = true)
                                    SalesFilterOptionCard(
                                        label = label,
                                        isSelected = isSelected,
                                        onClick = { selectedStatus = key },
                                        modifier = Modifier.weight(1f),
                                        testTag = "sales-filter-status-$key"
                                    )
                                }
                            }
                        }

                        // Full width extra status: Dibatalkan
                        val isVoided = selectedStatus.equals("Voided", ignoreCase = true) || selectedStatus.equals("Dibatalkan", ignoreCase = true)
                        SalesFilterOptionCard(
                            label = "Dibatalkan",
                            isSelected = isVoided,
                            onClick = { selectedStatus = "Voided" },
                            modifier = Modifier.fillMaxWidth(),
                            isCentered = true,
                            testTag = "sales-filter-status-voided"
                        )
                    }
                }

                // BEGIN: FilterSection - Metode Pembayaran
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "METODE PEMBAYARAN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.8.sp,
                    )

                    val paymentRows = listOf(
                        listOf("Semua" to "Semua Metode", "tunai" to "Cash"),
                        listOf("qris" to "QRIS", "transfer" to "Transfer")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        paymentRows.forEach { rowPairs ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowPairs.forEach { (key, label) ->
                                    val isSelected = selectedPaymentMethod.equals(key, ignoreCase = true)
                                    SalesFilterOptionCard(
                                        label = label,
                                        isSelected = isSelected,
                                        onClick = { selectedPaymentMethod = key },
                                        modifier = Modifier.weight(1f),
                                        testTag = "sales-filter-payment-$key"
                                    )
                                }
                            }
                        }

                        // Full width card: Hutang
                        val isHutang = selectedPaymentMethod.equals("hutang", ignoreCase = true)
                        SalesFilterOptionCard(
                            label = "Hutang",
                            isSelected = isHutang,
                            onClick = { selectedPaymentMethod = "hutang" },
                            modifier = Modifier.fillMaxWidth(),
                            isCentered = true,
                            testTag = "sales-filter-payment-hutang"
                        )
                    }
                }

                // BEGIN: FilterSection - Urutkan Berdasarkan
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "URUTKAN BERDASARKAN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.8.sp,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Waktu Terbaru
                        val isNewest = selectedSortOrder == SalesSortOrder.Newest
                        SalesFilterOptionCard(
                            label = "Waktu Terbaru",
                            isSelected = isNewest,
                            onClick = { selectedSortOrder = SalesSortOrder.Newest },
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Schedule,
                            isCentered = true,
                            testTag = "sales-filter-sort-newest"
                        )

                        // Nominal Terbesar
                        val isHighest = selectedSortOrder == SalesSortOrder.HighestAmount
                        SalesFilterOptionCard(
                            label = "Nominal Terbesar",
                            isSelected = isHighest,
                            onClick = { selectedSortOrder = SalesSortOrder.HighestAmount },
                            modifier = Modifier.weight(1f),
                            icon = Icons.AutoMirrored.Outlined.TrendingUp,
                            isCentered = true,
                            testTag = "sales-filter-sort-highest"
                        )
                    }

                    // Full width sort: No. Struk / Nota (A–Z)
                    val isReceiptAsc = selectedSortOrder == SalesSortOrder.ReceiptAsc
                    SalesFilterOptionCard(
                        label = "No. Struk / Nota (A–Z)",
                        isSelected = isReceiptAsc,
                        onClick = { selectedSortOrder = SalesSortOrder.ReceiptAsc },
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                        isCentered = true,
                        testTag = "sales-filter-sort-receipt"
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // BEGIN: ModalStickyFooter (Reset 2/5 + Terapkan 3/5)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Batal Button (Outline)
                Surface(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(2f)
                        .height(48.dp)
                        .testTag("sales-history-filter-cancel"),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.5.dp, Color(0xFFCBD5E1))
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Batal",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                    }
                }

                // Apply Button (Solid Dark Emerald)
                Button(
                    onClick = {
                        val startStr = if (selectedPreset == "Semua") null else startDate?.toString()
                        val endStr = if (selectedPreset == "Semua") null else endDate?.toString()
                        onApplyFilter(selectedStatus, selectedPaymentMethod, startStr, endStr, selectedPreset, selectedSortOrder)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(3f)
                        .height(48.dp)
                        .testTag("sales-history-filter-done"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF256B57),
                        contentColor = Color.White,
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                ) {
                    Text(
                        text = "Terapkan",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        HistoryDatePickerDialog(
            currentDate = (startDate ?: today).toString(),
            initialMode = when (selectedPreset) {
                "Bulan ini", "Bulan" -> "Bulan"
                "Minggu ini", "Minggu" -> "Minggu"
                else -> "Hari"
            },
            onDismiss = { showDatePicker = false },
            onConfirmRange = { newStartStr, newEndStr, _ ->
                startDate = java.time.LocalDate.parse(newStartStr)
                endDate = java.time.LocalDate.parse(newEndStr)
                selectedPreset = "Kustom"
                showDatePicker = false
            },
            onConfirm = { singleDateStr ->
                val date = java.time.LocalDate.parse(singleDateStr)
                startDate = date
                endDate = date
                selectedPreset = "Kustom"
                showDatePicker = false
            }
        )
    }
}

@Composable
private fun SalesFilterOptionCard(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    isCentered: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(46.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) Color(0xFF256B57) else Color(0xFFE2E8F0)
        ),
        shadowElevation = if (isSelected) 0.5.dp else 0.dp
    ) {
        if (isCentered) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color(0xFF256B57) else Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = label,
                    fontSize = 13.5.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) Color(0xFF256B57) else Color(0xFF334155),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) Color(0xFF256B57) else Color(0xFF334155),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Color(0xFF256B57),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SalesSummaryBottomSheet(
    state: AdminTransactionHistoryUiState,
    onDismiss: () -> Unit,
) {
    val totalTransactions = state.total
    val pageTotal = state.transactions.fold(BigDecimal.ZERO) { acc, t ->
        acc.add(t.total ?: BigDecimal.ZERO)
    }
    val lunasCount = state.transactions.count { it.status.equals("Lunas", ignoreCase = true) }
    val nonLunasCount = state.transactions.count { !it.status.equals("Lunas", ignoreCase = true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(48.dp)
                    .height(6.dp)
                    .background(Color(0xFFE2E8F0), CircleShape)
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sales-history-summary-sheet")
                .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 32.dp),
        ) {
            // Sheet Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Ringkasan Penjualan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    letterSpacing = (-0.3).sp,
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Tutup ringkasan",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Summary Metrics: Hero
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "TOTAL NILAI PENJUALAN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.5.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatSalesSummaryCurrency(pageTotal),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF256B57),
                    letterSpacing = (-0.5).sp,
                )
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Metrics List
            // 1. Total Transaksi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Total Transaksi",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B),
                )
                Text(
                    text = "$totalTransactions Transaksi",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                )
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // 2. Transaksi Lunas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Transaksi Lunas",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF059669), CircleShape)
                    )
                    Text(
                        text = "$lunasCount",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669),
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // 3. Transaksi Hutang / DP
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Transaksi Hutang / DP",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B),
                )
                Text(
                    text = "$nonLunasCount",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (nonLunasCount > 0) Color(0xFFD97706) else Color(0xFF94A3B8),
                )
            }

            Spacer(Modifier.height(16.dp))

            // Action Button Footer
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF256B57),
                    contentColor = Color.White,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
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

private fun formatSalesSummaryCurrency(amount: BigDecimal): String {
    val symbols = DecimalFormatSymbols(Locale.forLanguageTag("id-ID")).apply {
        groupingSeparator = '.'
    }
    val formatter = DecimalFormat("#,##0", symbols)
    return "Rp " + formatter.format(amount)
}

@Composable
private fun SalesHistorySectionHeader(
    compact: Boolean = false,
    isLoading: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isLoading) {
            SkeletonBox(Modifier.width(110.dp).height(20.dp), shape = RoundedCornerShape(4.dp))
        } else {
            Text(
                text = "Daftar Transaksi",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                letterSpacing = (-0.2).sp
            )
        }
    }
}


@Composable
private fun SalesTransactionMobileItem(
    transaction: CashTransaction,
    onReceiptClick: (String) -> Unit,
) {
    val statusClean = transaction.status.lowercase()
    val isPaid = statusClean in listOf("lunas", "paid", "success", "completed")
    val isPartial = statusClean in listOf("dp", "partial", "pending")
    val isVoided = statusClean in listOf("voided", "void", "refunded", "cancelled")
    val isUnpaid = statusClean in listOf("hutang", "unpaid")

    val dotColor = when {
        isPaid -> Color(0xFF10533C)
        isPartial -> Color(0xFFD97706)
        isUnpaid -> Color(0xFFDC2626)
        isVoided -> Color(0xFF64748B)
        else -> Color(0xFF10533C)
    }

    val customerName = transaction.customerName?.takeIf { it.isNotBlank() } ?: "Pelanggan Umum"
    val paymentLabel = transaction.paymentMethods.salesPaymentMethodsLabel(transaction.status)
    val remaining = transaction.remainingAmount ?: transaction.total.subtract(transaction.paidAmount).max(BigDecimal.ZERO)
    val subtitle = if (remaining > BigDecimal.ZERO && !isPaid) {
        "$customerName · $paymentLabel (Sisa ${remaining.moneyText()})"
    } else {
        "$customerName · $paymentLabel"
    }

    val dateTimeText = transaction.createdAt.displayShortDateTime()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onReceiptClick(transaction.id) }
            .testTag("sales-transaction-${transaction.id}")
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        // Line 1: Status dot + Receipt number on left, Total amount + Chevron on right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(dotColor, CircleShape)
                )
                Text(
                    text = transaction.receiptNumber(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B),
                    letterSpacing = (-0.3).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = transaction.total.moneyText(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    letterSpacing = (-0.3).sp,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFFCBD5E1),
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        // Line 2: Customer & Payment Method on left, Date & Time on right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, start = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = subtitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = dateTimeText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF94A3B8),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SalesTransactionCard(transaction: CashTransaction, onReceiptClick: (String) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sales-transaction-${transaction.id}"),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        shadowElevation = 0.5.dp,
    ) {
        SalesTransactionMobileItem(transaction, onReceiptClick)
    }
}

@Composable
private fun SalesTransactionTable(transactions: List<CashTransaction>, onReceiptClick: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("sales-history-table"),
        color = TbSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, TbOutline),
        tonalElevation = 1.dp,
    ) {
        Column {
            SalesTableHeader()
            transactions.forEachIndexed { index, transaction ->
                SalesTableRow(transaction, onReceiptClick)
                if (index < transactions.lastIndex) HorizontalDivider(color = TbOutline.copy(alpha = 0.72f))
            }
        }
    }
}

@Composable
private fun SalesTableHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SalesHeaderText("WAKTU", Modifier.weight(1.05f))
        SalesHeaderText("NO. TRANSAKSI", Modifier.weight(1.55f))
        SalesHeaderText("PELANGGAN", Modifier.weight(1.35f))
        SalesHeaderText("PEMBAYARAN", Modifier.weight(1.1f))
        SalesHeaderText("STATUS", Modifier.weight(1f))
        SalesHeaderText("TOTAL", Modifier.weight(1.2f), Alignment.End)
        Spacer(Modifier.width(40.dp))
    }
}

@Composable
private fun SalesTableRow(transaction: CashTransaction, onReceiptClick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onReceiptClick(transaction.id) }
            .testTag("sales-transaction-${transaction.id}")
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1.05f)) {
            Text(transaction.createdAt.displayTime(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(transaction.createdAt.displayDate(), style = MaterialTheme.typography.labelSmall, color = TbTextMuted)
        }
        Text(
            transaction.receiptNumber(), Modifier.weight(1.55f),
            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Column(modifier = Modifier.weight(1.35f)) {
            Text(
                transaction.customerName ?: "Pelanggan umum",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            transaction.cashierName?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = TbTextMuted, maxLines = 1)
            }
        }
        Text(
            transaction.paymentMethods.salesPaymentMethodsLabel(transaction.status), Modifier.weight(1.1f),
            style = MaterialTheme.typography.bodySmall, color = TbTextMuted,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Box(modifier = Modifier.weight(1f)) { SalesStatusBadge(transaction.status) }
        Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
            Text(transaction.total.moneyText(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("Bayar ${transaction.paidAmount.moneyText()}", style = MaterialTheme.typography.labelSmall, color = TbTextMuted)
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = "Lihat struk",
            tint = TbTextMuted,
            modifier = Modifier.padding(start = 12.dp).size(22.dp),
        )
    }
}

@Composable
private fun SalesHeaderText(text: String, modifier: Modifier, alignment: Alignment.Horizontal = Alignment.Start) {
    Column(modifier = modifier, horizontalAlignment = alignment) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = TbTextMuted, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SalesStatusBadge(status: String) {
    val color = status.statusColor()
    Surface(color = color.copy(alpha = 0.12f), contentColor = color, shape = RoundedCornerShape(999.dp)) {
        Text(
            status.salesStatusDisplay(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun SalesHistoryLoading(compact: Boolean) {
    if (compact) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sales-history-skeleton"),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        ) {
            Column {
                repeat(3) { index ->
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SkeletonBox(Modifier.size(40.dp))
                            Spacer(Modifier.width(12.dp))
                            SkeletonBox(Modifier.fillMaxWidth(0.55f).height(14.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            SkeletonBox(Modifier.width(80.dp).height(28.dp))
                            SkeletonBox(Modifier.width(80.dp).height(28.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFFF8FAFC))
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            SkeletonBox(Modifier.width(100.dp).height(14.dp))
                            SkeletonBox(Modifier.width(48.dp).height(14.dp))
                        }
                    }
                    if (index < 2) HorizontalDivider(color = Color(0xFFF1F5F9))
                }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth().testTag("sales-history-skeleton"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(5) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = TbSurface,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, TbOutline),
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        SkeletonBox(Modifier.size(40.dp))
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            SkeletonBox(Modifier.fillMaxWidth(0.42f).height(15.dp))
                            Spacer(Modifier.height(8.dp))
                            SkeletonBox(Modifier.fillMaxWidth(0.64f).height(12.dp))
                        }
                        SkeletonBox(Modifier.width(84.dp).height(28.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SalesHistoryEmpty() {
    SalesHistoryStateCard(
        icon = Icons.Outlined.History,
        title = "Belum ada penjualan",
        message = "Tidak ada transaksi yang cocok dengan filter saat ini.",
    )
}

@Composable
private fun SalesHistoryError(message: String, onRetry: () -> Unit) {
    SalesHistoryStateCard(
        icon = Icons.Outlined.History,
        title = "Riwayat gagal dimuat",
        message = message,
        action = { Button(onClick = onRetry) { Text("Coba lagi") } },
    )
}

@Composable
private fun SalesHistoryInlineError(message: String, onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onRetry) { Text("Coba lagi") }
        }
    }
}

@Composable
private fun SalesHistoryStateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    action: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = TbSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, TbOutline),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, null, tint = TbTextMuted, modifier = Modifier.size(32.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TbText)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = TbTextMuted, textAlign = TextAlign.Center)
            action?.invoke()
        }
    }
}

@Composable
private fun SalesHistoryPagination(
    state: AdminTransactionHistoryUiState,
    compact: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
) {
    if (compact) {
        val canPrev = state.page > 1 && !state.isLoading
        val canNext = state.page < state.totalPages && !state.isLoading

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("sales-history-pagination"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Pill 1: Total transactions
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFF10533C).copy(alpha = 0.3f)),
                shadowElevation = 0.5.dp,
            ) {
                Text(
                    text = "${state.total} transaksi",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF10533C),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            // Pill 2: Page controller
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFF10533C).copy(alpha = 0.3f)),
                shadowElevation = 0.5.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .clickable(enabled = canPrev, onClick = onPreviousPage),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChevronLeft,
                            contentDescription = "Halaman sebelumnya",
                            tint = if (canPrev) Color(0xFF10533C) else Color(0xFF94A3B8).copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "${state.page}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "/",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "${state.totalPages.coerceAtLeast(1)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .clickable(enabled = canNext, onClick = onNextPage),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = "Halaman selanjutnya",
                            tint = if (canNext) Color(0xFF10533C) else Color(0xFF94A3B8).copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    } else {
        val countText = if (state.total == 0L) {
            "0 transaksi"
        } else if (state.transactions.size.toLong() >= state.total) {
            "${state.total} transaksi"
        } else {
            "${state.transactions.size} dari ${state.total} transaksi"
        }
        TbPagination(
            currentPage = state.page,
            totalPages = state.totalPages,
            onPreviousPage = onPreviousPage,
            onNextPage = onNextPage,
            supportingText = countText,
            isLoading = state.isLoading,
            compact = compact,
            testTag = "sales-history-pagination",
        )
    }
}

private fun buildSalesMeta(transaction: CashTransaction): String = buildList {
    transaction.cashierName?.takeIf { it.isNotBlank() }?.let { add(it) }
    add(transaction.paymentMethods.salesPaymentMethodsLabel(transaction.status))
}.joinToString(" · ")

private fun List<String>.salesPaymentMethodsLabel(status: String = ""): String {
    val isSettled = status.lowercase() in setOf("lunas", "paid", "success", "completed")
    val filtered = if (isSettled) {
        filterNot { it.lowercase() in setOf("hutang", "credit", "dp", "down_payment") }
            .ifEmpty { listOf("tunai") }
    } else {
        this
    }
    return if (filtered.isEmpty()) "Metode tidak tersedia" else filtered.joinToString(" + ") { it.salesPaymentLabel() }
}

private fun String.salesPaymentLabel(): String = when (lowercase()) {
    "semua" -> "Semua metode"
    "tunai", "cash" -> "Tunai"
    "transfer", "bank_transfer" -> "Transfer"
    "qris" -> "QRIS"
    "hutang", "credit" -> "Hutang"
    "dp", "down_payment" -> "DP"
    else -> replaceFirstChar(Char::uppercase)
}

private fun String.salesPaymentFilterLabel(): String = when (lowercase()) {
    "semua" -> "Semua metode"
    "tunai", "cash" -> "Tunai / Cash"
    "transfer", "bank_transfer" -> "Transfer Bank"
    "qris" -> "QRIS"
    "hutang", "credit" -> "Hutang"
    "dp", "down_payment" -> "DP"
    else -> replaceFirstChar(Char::uppercase)
}

private fun String.salesStatusLabel(): String = when (lowercase()) {
    "semua" -> "Semua status"
    else -> salesStatusDisplay()
}

private fun String.salesStatusDisplay(): String = when (lowercase()) {
    "lunas", "paid", "success", "completed" -> "Lunas"
    "dp", "partial" -> "DP"
    "hutang", "unpaid" -> "Hutang"
    "voided" -> "Dibatalkan"
    "refunded" -> "Dikembalikan"
    else -> replaceFirstChar(Char::uppercase)
}

private fun AdminTransactionHistoryUiState.historyShortDateLabel(): String {
    val start = startDate?.take(10)?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
    val end = (endDate ?: selectedDate)?.take(10)?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }

    if (start != null && end != null) {
        return if (start == end) {
            start.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"))
        } else {
            "${start.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"))} - ${end.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"))}"
        }
    }

    val raw = endDate ?: selectedDate
    if (raw.isNullOrBlank()) {
        return runCatching {
            java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"))
        }.getOrDefault("Tanggal")
    }
    return runCatching {
        java.time.LocalDate.parse(raw).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"))
    }.getOrElse {
        raw.formatDisplayDate().take(5)
    }
}

@Preview(name = "Riwayat Penjualan - Phone", widthDp = 390, heightDp = 844, showBackground = true)
@Preview(name = "Riwayat Penjualan - Tablet", widthDp = 1180, heightDp = 800, showBackground = true)
@Composable
private fun TransactionHistoryPreview() {
    TbterminalappTheme {
        TransactionHistoryContent(
            state = AdminTransactionHistoryUiState(
                isLoading = false,
                transactions = previewTransactions,
                total = 2,
                currentStart = 1,
                currentEnd = 2,
            ),
            onSearchChanged = {}, onStatusChanged = {}, onPaymentMethodChanged = {},
            onDatePresetSelected = {},
            onDateSelected = {}, onSyncClick = {}, onSyncAllClick = {}, onRetry = {},
            onPreviousPage = {}, onNextPage = {}, onReceiptClick = {},
        )
    }
}

private val previewTransactions = listOf(
    CashTransaction(
        id = "sale-1", receiptId = "TRX-20260912-001", sessionId = "session-1",
        customerId = "customer-1", customerName = "Budi Santoso", cashierName = "Dian",
        paymentMethods = listOf("tunai"), type = "SALE", status = "Lunas",
        total = BigDecimal("125000"), paidAmount = BigDecimal("125000"),
        createdAt = "2026-09-12T09:15:00+07:00",
    ),
    CashTransaction(
        id = "sale-2", receiptId = "TRX-20260912-002", sessionId = "session-1",
        customerId = "customer-2", customerName = "CV Maju Jaya", cashierName = "Dian",
        paymentMethods = listOf("dp"), type = "SALE", status = "DP",
        total = BigDecimal("850000"), paidAmount = BigDecimal("350000"),
        createdAt = "2026-09-12T10:42:00+07:00",
    ),
)
