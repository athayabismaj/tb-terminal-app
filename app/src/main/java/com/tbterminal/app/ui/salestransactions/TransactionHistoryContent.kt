package com.tbterminal.app.ui.salestransactions

import androidx.compose.foundation.BorderStroke
import com.tbterminal.app.ui.components.TbPagination
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.tbterminal.app.ui.components.TbMobileControlSheet
import com.tbterminal.app.ui.components.TbMobileFilterButton
import com.tbterminal.app.ui.components.TbMobileSheetDoneButton
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
                    vertical = if (compact) 14.dp else 20.dp,
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
                    SalesHistorySectionHeader()
                    if (compact) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            state.transactions.forEach { transaction ->
                                SalesTransactionCard(transaction, onReceiptClick)
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
        TbMobileControlSheet(
            title = "Filter penjualan",
            subtitle = "Atur periode, status, dan metode pembayaran",
            onDismiss = { showMobileFilters = false },
            testTag = "sales-history-filter-sheet",
        ) {
            SalesPeriodSelector(
                selected = state.selectedPreset,
                dateLabel = state.historyShortDateLabel(),
                onPresetSelected = onDatePresetSelected,
                onCalendarClick = {
                    showMobileFilters = false
                    showDatePicker = true
                },
                modifier = Modifier.fillMaxWidth(),
            )
            SalesFilterDropdown(
                label = state.statusFilter.salesStatusLabel(),
                choices = salesStatusChoices,
                onSelected = onStatusChanged,
                contentDescription = "Filter status",
                modifier = Modifier.fillMaxWidth(),
            )
            SalesFilterDropdown(
                label = state.paymentMethodFilter.salesPaymentLabel(),
                choices = salesPaymentChoices,
                onSelected = onPaymentMethodChanged,
                contentDescription = "Filter metode pembayaran",
                modifier = Modifier.fillMaxWidth(),
            )
            TbMobileSheetDoneButton(
                onClick = { showMobileFilters = false },
                testTag = "sales-history-filter-done",
            )
        }
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SalesSearchField(state.query, onSearchChanged, Modifier.weight(1f))
                TbMobileFilterButton(
                    onClick = onOpenMobileFilters,
                    active = state.statusFilter != "Semua" || state.paymentMethodFilter != "Semua",
                    testTag = "sales-history-open-filters",
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SalesSearchField(state.query, onSearchChanged, Modifier.weight(1f))
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
                    state.paymentMethodFilter.salesPaymentLabel(), salesPaymentChoices,
                    onPaymentMethodChanged, "Filter metode pembayaran", Modifier.width(190.dp),
                )
            }
        }
    }
}

@Composable
private fun SalesSearchField(value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier.height(48.dp).testTag("sales-history-search"),
        color = TbSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, TbOutline),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = TbTextMuted, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        "Cari transaksi, pelanggan, atau kasir",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TbTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = TbText,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = "Hapus pencarian", modifier = Modifier.size(18.dp))
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
    "tunai" to "Tunai",
    "transfer" to "Transfer",
    "qris" to "QRIS",
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

@Composable
private fun SalesHistorySectionHeader() {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Penjualan",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TbText,
        )
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
            transaction.paymentMethods.salesPaymentMethodsLabel(), Modifier.weight(1.1f),
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
    Column(
        modifier = Modifier.fillMaxWidth().testTag("sales-history-skeleton"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(if (compact) 4 else 5) {
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
    TbPagination(
        currentPage = state.page,
        totalPages = state.totalPages,
        onPreviousPage = onPreviousPage,
        onNextPage = onNextPage,
        supportingText = if (compact) "${state.total} data"
        else "Menampilkan ${state.currentStart}–${state.currentEnd} dari ${state.total}",
        isLoading = state.isLoading,
        testTag = "sales-history-pagination",
    )
}

private fun buildSalesMeta(transaction: CashTransaction): String = buildList {
    transaction.cashierName?.takeIf { it.isNotBlank() }?.let { add(it) }
    add(transaction.paymentMethods.salesPaymentMethodsLabel())
}.joinToString(" · ")

private fun List<String>.salesPaymentMethodsLabel(): String =
    if (isEmpty()) "Metode tidak tersedia" else joinToString(" + ") { it.salesPaymentLabel() }

private fun String.salesPaymentLabel(): String = when (lowercase()) {
    "semua" -> "Semua metode"
    "tunai", "cash" -> "Tunai"
    "transfer", "bank_transfer" -> "Transfer"
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
    val date = endDate ?: selectedDate ?: return "Tanggal"
    return date.formatDisplayDate().take(5)
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
