package com.tbterminal.app.ui.salestransactions

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

@Composable
internal fun TransactionHistoryContent(
    state: AdminTransactionHistoryUiState,
    onSearchChanged: (String) -> Unit,
    onStatusChanged: (String) -> Unit,
    onPaymentMethodChanged: (String) -> Unit,
    onDatePresetSelected: (String) -> Unit,
    onDateSelected: (String) -> Unit,
    onSyncClick: (Long) -> Unit,
    onSyncAllClick: () -> Unit,
    onRetry: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onReceiptClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showMobileFilters by remember { mutableStateOf(false) }
    var showSummarySheet by remember { mutableStateOf(false) }

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
            SalesHistoryFilters(
                compact = compact,
                state = state,
                onSearchChanged = onSearchChanged,
                onStatusChanged = onStatusChanged,
                onPaymentMethodChanged = onPaymentMethodChanged,
                onCalendarClick = { showDatePicker = true },
                onDatePresetSelected = onDatePresetSelected,
                onOpenMobileFilters = { showMobileFilters = true },
            )

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

            if (state.error != null && state.transactions.isNotEmpty()) {
                SalesHistoryInlineError(state.error, onRetry)
            }

            when {
                state.isLoading && state.transactions.isEmpty() -> SalesHistoryLoading(compact)
                state.error != null && state.transactions.isEmpty() -> SalesHistoryError(state.error, onRetry)
                state.transactions.isEmpty() -> SalesHistoryEmpty()
                else -> {
                    SalesHistorySectionHeader(compact, onSummaryClick = { showSummarySheet = true })
                    if (compact) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sales-transaction-grouped-list"),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
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

    if (showMobileFilters) {
        SalesFilterBottomSheet(
            state = state,
            onDismiss = { showMobileFilters = false },
            onPresetSelected = onDatePresetSelected,
            onCalendarClick = {
                showMobileFilters = false
                showDatePicker = true
            },
            onStatusChanged = onStatusChanged,
            onPaymentMethodChanged = onPaymentMethodChanged,
        )
    }

    if (showDatePicker) {
        HistoryDatePickerDialog(
            currentDate = state.endDate ?: state.selectedDate,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                onDateSelected(it)
                showDatePicker = false
            },
        )
    }

    if (showSummarySheet) {
        SalesSummaryBottomSheet(
            state = state,
            onDismiss = { showSummarySheet = false },
        )
    }
}

@Composable
private fun SalesHistoryFilters(
    compact: Boolean,
    state: AdminTransactionHistoryUiState,
    onSearchChanged: (String) -> Unit,
    onStatusChanged: (String) -> Unit,
    onPaymentMethodChanged: (String) -> Unit,
    onCalendarClick: () -> Unit,
    onDatePresetSelected: (String) -> Unit,
    onOpenMobileFilters: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("sales-history-filters"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (compact) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SalesSearchField(
                    value = state.query,
                    onValueChange = onSearchChanged,
                    modifier = Modifier.weight(1f),
                    compact = true,
                )
                val isFiltered = state.statusFilter != "Semua" || state.paymentMethodFilter != "Semua" || (state.selectedPreset != null && state.selectedPreset != "Hari ini")
                Surface(
                    onClick = onOpenMobileFilters,
                    modifier = Modifier
                        .size(42.dp)
                        .testTag("sales-history-open-filters"),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, if (isFiltered) Color(0xFF0F172A) else Color(0xFFCBD5E1)),
                    contentColor = Color(0xFF0F172A),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.Tune,
                            contentDescription = "Filter transaksi",
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF0F172A),
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
    onPresetSelected: (String) -> Unit,
    onCalendarClick: () -> Unit,
    onStatusChanged: (String) -> Unit,
    onPaymentMethodChanged: (String) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color(0xFFCBD5E1), CircleShape)
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sales-history-filter-sheet"),
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
                        text = "Filter Penjualan",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.3).sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Atur periode, status, dan metode pembayaran",
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
                            val presets = listOf(
                                "Hari ini" to "Hari",
                                "Minggu ini" to "Minggu",
                                "Bulan ini" to "Bulan",
                            )
                            presets.forEach { (key, label) ->
                                val isActive = state.selectedPreset == key
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(8.dp))
                                        .then(
                                            if (isActive) {
                                                Modifier
                                                    .shadow(elevation = 1.dp, shape = RoundedCornerShape(8.dp))
                                                    .background(Color.White)
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable { onPresetSelected(key) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isActive) Color(0xFF0F172A) else Color(0xFF64748B),
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
                                .clickable(onClick = onCalendarClick)
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
                                    text = state.historyShortDateLabel(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF256B57),
                                )
                            }
                        }
                    }
                }

                // Section: STATUS
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "STATUS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.8.sp,
                    )
                    FilterDropdownField(
                        selectedValue = state.statusFilter,
                        displayLabel = state.statusFilter.salesStatusLabel(),
                        options = salesStatusChoices,
                        onSelected = onStatusChanged,
                        contentDescription = "Filter status",
                    )
                }

                // Section: METODE PEMBAYARAN
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "METODE PEMBAYARAN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.8.sp,
                    )
                    FilterDropdownField(
                        selectedValue = state.paymentMethodFilter,
                        displayLabel = state.paymentMethodFilter.salesPaymentFilterLabel(),
                        options = salesPaymentChoices,
                        onSelected = onPaymentMethodChanged,
                        contentDescription = "Filter metode pembayaran",
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Action Button (Selesai)
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
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
                        text = "Selesai",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterDropdownField(
    selectedValue: String,
    displayLabel: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
    contentDescription: String,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF8FAF9))
                .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = displayLabel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1E293B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Outlined.ExpandMore,
                    contentDescription = contentDescription,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            containerColor = Color.White,
        ) {
            options.forEach { (value, label) ->
                val isSelected = selectedValue.equals(value, ignoreCase = true)
                DropdownMenuItem(
                    modifier = Modifier.testTag("sales-filter-option-$contentDescription-$value"),
                    text = {
                        Text(
                            text = label,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Color(0xFF256B57) else Color(0xFF1E293B),
                        )
                    },
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
    onSummaryClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Penjualan",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A),
        )
        if (compact) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .then(
                        if (onSummaryClick != null) Modifier.clickable(onClick = onSummaryClick)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center,
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

@Composable
private fun SalesTransactionMobileItem(
    transaction: CashTransaction,
    onReceiptClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onReceiptClick(transaction.id) }
            .testTag("sales-transaction-${transaction.id}")
            .padding(16.dp),
    ) {
        // Header: Icon, Details & Chevron
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
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
                        text = transaction.receiptNumber(),
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
                            text = transaction.customerName?.takeIf { it.isNotBlank() } ?: "Pelanggan umum",
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
                        val cashierName = transaction.cashierName?.takeIf { it.isNotBlank() } ?: "-"
                        Text(
                            text = "Kasir: $cashierName",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF64748B),
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
                            text = transaction.paymentMethods.salesPaymentMethodsLabel(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF475569),
                            maxLines = 1,
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

        // Details Grid: Baris 1 Total & Dibayar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "TOTAL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFF94A3B8),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = transaction.total.moneyText(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "DIBAYAR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.End,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = transaction.paidAmount.moneyText(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF15803D),
                    textAlign = TextAlign.End,
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        HorizontalDivider(color = Color(0xFFF8FAFC))
        Spacer(Modifier.height(6.dp))

        // Details Grid: Baris 2 Tanggal & Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${transaction.createdAt.displayDate()}, ${transaction.createdAt.displayTime()}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B),
            )
            val statusColor = when (transaction.status.lowercase()) {
                "lunas", "paid", "success", "completed" -> Color(0xFF15803D)
                "dp", "partial", "pending" -> Color(0xFFD97706)
                "voided", "void", "refunded", "cancelled" -> Color(0xFFE11D48)
                "hutang", "unpaid" -> Color(0xFFEA580C)
                else -> Color(0xFF15803D)
            }
            Text(
                text = transaction.status.salesStatusDisplay(),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = statusColor,
            )
        }
    }
}

@Composable
private fun SalesTransactionCard(transaction: CashTransaction, onReceiptClick: (String) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onReceiptClick(transaction.id) }
            .testTag("sales-transaction-${transaction.id}"),
        color = TbSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, TbOutline),
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        transaction.receiptNumber(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TbText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${transaction.createdAt.displayDate()} · ${transaction.createdAt.displayTime()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TbTextMuted,
                    )
                }
                SalesStatusBadge(transaction.status)
            }
            HorizontalDivider(color = TbOutline.copy(alpha = 0.72f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ReceiptLong, null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(19.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        transaction.customerName ?: "Pelanggan umum",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TbText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        buildSalesMeta(transaction),
                        style = MaterialTheme.typography.bodySmall,
                        color = TbTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total", style = MaterialTheme.typography.labelSmall, color = TbTextMuted)
                    Text(
                        transaction.total.moneyText(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TbText,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Dibayar ${transaction.paidAmount.moneyText()}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = TbTextMuted,
                )
                Text("Lihat struk", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, bottom = 4.dp)
                .testTag("sales-history-pagination"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            ) {
                Text(
                    text = "${state.total} data",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569),
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
                    val canPrev = state.page > 1 && !state.isLoading
                    val canNext = state.page < state.totalPages && !state.isLoading
                    IconButton(
                        onClick = onPreviousPage,
                        enabled = canPrev,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Halaman sebelumnya",
                            tint = if (canPrev) Color(0xFF475569) else Color(0xFFCBD5E1),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        text = "${state.page} / ${state.totalPages.coerceAtLeast(1)}",
                        modifier = Modifier.padding(horizontal = 8.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF334155),
                    )
                    IconButton(
                        onClick = onNextPage,
                        enabled = canNext,
                        modifier = Modifier.size(28.dp),
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Halaman berikutnya",
                            tint = if (canNext) Color(0xFF475569) else Color(0xFFCBD5E1),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    } else {
        TbPagination(
            currentPage = state.page,
            totalPages = state.totalPages,
            onPreviousPage = onPreviousPage,
            onNextPage = onNextPage,
            supportingText = "Menampilkan ${state.currentStart}–${state.currentEnd} dari ${state.total}",
            isLoading = state.isLoading,
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
