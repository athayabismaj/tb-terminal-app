package com.tbterminal.app.ui.offlinereports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.local.dao.OfflineCashReportRow
import com.tbterminal.app.data.local.dao.OfflineExpenseReportRow
import com.tbterminal.app.data.local.dao.OfflineReceivableReportRow
import com.tbterminal.app.data.local.dao.OfflineSalesReportRow
import com.tbterminal.app.data.local.model.SyncStatus
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PageBackground = Color(0xFFF4F8FA)
private val CardBorder = Color(0xFFDDE6EC)
private val TextPrimary = Color(0xFF111827)
private val TextSecondary = Color(0xFF64748B)
private val Teal = Color(0xFF009B72)
private val Blue = Color(0xFF2563EB)
private val Orange = Color(0xFFFF9800)
private val Red = Color(0xFFDC2626)

@Composable
fun OfflineReportScreen(
    uiState: OfflineReportUiState,
    onPresetSelected: (OfflineReportPreset) -> Unit,
    onCustomStartChanged: (String) -> Unit,
    onCustomEndChanged: (String) -> Unit,
    onApplyCustomRange: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(PageBackground)) {
        val compact = maxWidth < 700.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = if (compact) 16.dp else 30.dp, vertical = if (compact) 14.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 18.dp)
        ) {
        item {
            OfflineReportHeader(
                isLoading = uiState.isLoading,
                lastRefresh = uiState.lastRefresh,
                onRefresh = onRefresh,
                compact = compact
            )
        }

        item {
            OfflineReportFilter(
                uiState = uiState,
                onPresetSelected = onPresetSelected,
                onCustomStartChanged = onCustomStartChanged,
                onCustomEndChanged = onCustomEndChanged,
                onApplyCustomRange = onApplyCustomRange,
                compact = compact
            )
        }

        uiState.errorMessage?.let { message ->
            item {
                MessageCard(message)
            }
        }

        if (uiState.isLoading) {
            item {
                LoadingCard()
            }
        }

        item {
            OfflineKpiGrid(uiState, compact)
        }

        item {
            OfflineSalesReportSection(
                transactions = uiState.transactions,
                startDate = uiState.startDate,
                endDate = uiState.endDate,
                compact = compact
            )
        }

        item {
            OfflineCashReportSection(cashSessions = uiState.cashSessions, compact = compact)
        }

        item {
            OfflineReceivableReportSection(receivables = uiState.receivables, compact = compact)
        }

        item {
            OfflineExpenseReportSection(expenses = uiState.expenses, compact = compact)
        }
        }
    }
}

@Composable
private fun OfflineKpiGrid(uiState: OfflineReportUiState, compact: Boolean) {
    val items = listOf(
        Triple("Omzet lokal", uiState.salesSummary.localRevenue.formatCurrency(), Teal),
        Triple("Uang diterima", uiState.salesSummary.collectedAmount.formatCurrency(), Blue),
        Triple("Sisa piutang", uiState.salesSummary.outstandingAmount.formatCurrency(), Red),
        Triple("Transaksi", uiState.salesSummary.totalTransactions.toString(), Orange)
    )
    val icons = listOf(Icons.Outlined.Assessment, Icons.Outlined.Payments, Icons.Outlined.CreditCard, Icons.AutoMirrored.Outlined.ReceiptLong)
    if (compact) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.chunked(2).forEachIndexed { rowIndex, rowItems ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowItems.forEachIndexed { itemIndex, item ->
                        KpiCard(item.first, item.second, icons[rowIndex * 2 + itemIndex], item.third, Modifier.weight(1f), true)
                    }
                }
            }
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items.forEachIndexed { index, item -> KpiCard(item.first, item.second, icons[index], item.third, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun OfflineReportHeader(
    isLoading: Boolean,
    lastRefresh: Long?,
    onRefresh: () -> Unit,
    compact: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Data tersimpan di perangkat", color = TextPrimary, fontSize = if (compact) 15.sp else 17.sp, fontWeight = FontWeight.Bold)
            if (lastRefresh != null) {
                Text(
                    text = "Terakhir diperbarui ${lastRefresh.formatDateTime()}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        OutlinedButton(
            enabled = !isLoading,
            onClick = onRefresh,
            shape = RoundedCornerShape(14.dp),
            contentPadding = if (compact) androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 10.dp) else androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            if (!compact) {
                Spacer(Modifier.width(8.dp))
                Text("Perbarui")
            }
        }
    }
}

@Composable
private fun OfflineReportFilter(
    uiState: OfflineReportUiState,
    onPresetSelected: (OfflineReportPreset) -> Unit,
    onCustomStartChanged: (String) -> Unit,
    onCustomEndChanged: (String) -> Unit,
    onApplyCustomRange: () -> Unit,
    compact: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = Teal,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Periode ${uiState.startDate.formatShortDate()} - ${uiState.endDate.formatShortDate()}",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    OfflineReportPreset.Today,
                    OfflineReportPreset.Last7Days,
                    OfflineReportPreset.ThisMonth
                ).forEach { preset ->
                    PresetButton(
                        text = preset.label,
                        selected = uiState.selectedPreset == preset,
                        onClick = { onPresetSelected(preset) }
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            if (compact) Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = uiState.customStartInput,
                    onValueChange = onCustomStartChanged,
                    label = { Text("Tanggal awal") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = uiState.customEndInput,
                    onValueChange = onCustomEndChanged,
                    label = { Text("Tanggal akhir") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                Button(
                    onClick = onApplyCustomRange,
                    colors = ButtonDefaults.buttonColors(containerColor = Teal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Terapkan")
                }
            } else Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(uiState.customStartInput, onCustomStartChanged, label = { Text("Tanggal awal") }, singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp))
                OutlinedTextField(uiState.customEndInput, onCustomEndChanged, label = { Text("Tanggal akhir") }, singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp))
                Button(onClick = onApplyCustomRange, colors = ButtonDefaults.buttonColors(containerColor = Teal), shape = RoundedCornerShape(14.dp), modifier = Modifier.height(56.dp)) { Text("Terapkan") }
            }
        }
    }
}

@Composable
private fun PresetButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) Teal else Color.White
    val fg = if (selected) Color.White else TextSecondary
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = bg, contentColor = fg),
        border = BorderStroke(1.dp, if (selected) Teal else CardBorder)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Card(
        modifier = modifier.height(if (compact) 108.dp else 124.dp),
        shape = RoundedCornerShape(if (compact) 18.dp else 14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 14.dp else 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title.uppercase(),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(accent.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                }
            }
            Text(
                text = value,
                color = accent,
                fontSize = if (compact) 18.sp else 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun OfflineSalesReportSection(
    transactions: List<OfflineSalesReportRow>,
    startDate: LocalDate,
    endDate: LocalDate,
    compact: Boolean = false
) {
    ReportCard(
        title = "Laporan Penjualan Lokal",
        subtitle = "${startDate.formatShortDate()} sampai ${endDate.formatShortDate()}"
    ) {
        if (transactions.isEmpty()) {
            EmptyRows("Belum ada transaksi lokal pada periode ini.")
        } else if (compact) {
            transactions.forEach { row ->
                OfflineMobileRow(
                    title = row.transactionCode,
                    subtitle = "${row.customerName} · ${row.occurredAt.formatDateTime()}",
                    values = listOf("Total" to row.total.formatCurrency(), "Dibayar" to row.paidAmount.formatCurrency(), "Sisa" to row.remainingAmount.formatCurrency()),
                    status = row.syncStatus
                )
            }
        } else {
            HeaderRow(listOf("Transaksi", "Customer", "Total", "Dibayar", "Sisa", "Sync"))
            transactions.forEach { row ->
                DataRow(
                    cells = listOf(
                        "${row.transactionCode}\n${row.occurredAt.formatDateTime()}",
                        row.customerName,
                        row.total.formatCurrency(),
                        row.paidAmount.formatCurrency(),
                        row.remainingAmount.formatCurrency()
                    ),
                    status = row.syncStatus
                )
            }
        }
    }
}

@Composable
fun OfflineCashReportSection(cashSessions: List<OfflineCashReportRow>, compact: Boolean = false) {
    ReportCard(
        title = "Laporan Kas Lokal",
        subtitle = "Sesi kas lokal dalam periode yang dipilih."
    ) {
        if (cashSessions.isEmpty()) {
            EmptyRows("Belum ada sesi kas lokal pada periode ini.")
        } else if (compact) {
            cashSessions.forEach { row ->
                OfflineMobileRow(
                    title = "Sesi ${row.status.lowercase().replaceFirstChar { it.uppercase() }}",
                    subtitle = "Kasir ${row.cashierUserId} · ${row.openedAt.formatDateTime()}",
                    values = listOf("Modal" to row.startingCash.formatCurrency(), "Penjualan" to row.totalCashSales.formatCurrency(), "Kas akhir" to row.endingCash.formatCurrency()),
                    status = row.syncStatus
                )
            }
        } else {
            HeaderRow(listOf("Kasir", "Modal", "Tunai", "Expense", "Kas Akhir", "Sync"))
            cashSessions.forEach { row ->
                DataRow(
                    cells = listOf(
                        "${row.cashierUserId}\n${row.status} - ${row.openedAt.formatDateTime()}",
                        row.startingCash.formatCurrency(),
                        row.totalCashSales.formatCurrency(),
                        row.totalExpense.formatCurrency(),
                        row.endingCash.formatCurrency()
                    ),
                    status = row.syncStatus
                )
            }
        }
    }
}

@Composable
fun OfflineReceivableReportSection(receivables: List<OfflineReceivableReportRow>, compact: Boolean = false) {
    ReportCard(
        title = "Laporan Piutang Lokal",
        subtitle = "Piutang dari transaksi lokal/offline."
    ) {
        if (receivables.isEmpty()) {
            EmptyRows("Belum ada piutang lokal pada periode ini.")
        } else if (compact) {
            receivables.forEach { row ->
                OfflineMobileRow(
                    title = row.customerName,
                    subtitle = "${row.transactionCode} · ${row.status}",
                    values = listOf("Total" to row.totalAmount.formatCurrency(), "Dibayar" to row.paidAmount.formatCurrency(), "Sisa" to row.remainingAmount.formatCurrency()),
                    status = row.syncStatus
                )
            }
        } else {
            HeaderRow(listOf("Customer", "Transaksi", "Total", "Dibayar", "Sisa", "Sync"))
            receivables.forEach { row ->
                DataRow(
                    cells = listOf(
                        "${row.customerName}\n${row.status}",
                        row.transactionCode,
                        row.totalAmount.formatCurrency(),
                        row.paidAmount.formatCurrency(),
                        row.remainingAmount.formatCurrency()
                    ),
                    status = row.syncStatus
                )
            }
        }
    }
}

@Composable
fun OfflineExpenseReportSection(expenses: List<OfflineExpenseReportRow>, compact: Boolean = false) {
    ReportCard(
        title = "Laporan Pengeluaran Lokal",
        subtitle = "Pengeluaran kas yang tersimpan di perangkat."
    ) {
        if (expenses.isEmpty()) {
            EmptyRows("Belum ada pengeluaran kas lokal pada periode ini.")
        } else if (compact) {
            expenses.forEach { row ->
                OfflineMobileRow(
                    title = row.category,
                    subtitle = "${row.occurredAt.formatDateTime()} · ${row.description.orEmpty().ifBlank { "Tanpa catatan" }}",
                    values = listOf("Nominal" to row.amount.formatCurrency()),
                    status = row.syncStatus
                )
            }
        } else {
            HeaderRow(listOf("Tanggal", "Kategori", "Nominal", "Catatan", "Sync"))
            expenses.forEach { row ->
                DataRow(
                    cells = listOf(
                        row.occurredAt.formatDateTime(),
                        row.category,
                        row.amount.formatCurrency(),
                        row.description.orEmpty().ifBlank { "-" }
                    ),
                    status = row.syncStatus
                )
            }
        }
    }
}

@Composable
private fun OfflineMobileRow(
    title: String,
    subtitle: String,
    values: List<Pair<String, String>>,
    status: SyncStatus
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = TextSecondary, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            SyncBadge(status)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            values.forEach { (label, value) ->
                Column(Modifier.weight(1f)) {
                    Text(label, color = TextSecondary, fontSize = 10.sp)
                    Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
    HorizontalDivider(color = CardBorder)
}

@Composable
private fun ReportCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun HeaderRow(titles: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        titles.forEach { title ->
            Text(
                text = title.uppercase(),
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DataRow(
    cells: List<String>,
    status: SyncStatus
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        cells.forEach { cell ->
            Text(
                text = cell,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            SyncBadge(status)
        }
    }
    HorizontalDivider(color = CardBorder)
}

@Composable
private fun SyncBadge(status: SyncStatus) {
    val (bg, fg) = when (status) {
        SyncStatus.PENDING -> Color(0xFFFFF4DE) to Color(0xFFB45309)
        SyncStatus.SYNCING -> Color(0xFFEFF6FF) to Color(0xFF1D4ED8)
        SyncStatus.FAILED -> Color(0xFFFFEDEE) to Red
        SyncStatus.CONFLICT -> Color(0xFFF3E8FF) to Color(0xFF7C3AED)
        SyncStatus.SYNCED -> Color(0xFFE6F8EF) to Teal
    }
    Surface(shape = RoundedCornerShape(999.dp), color = bg) {
        Text(
            text = status.name,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun EmptyRows(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MessageCard(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF4DE), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(message, color = Color(0xFFB45309), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Teal, strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
            Text("Memuat laporan lokal...", color = TextSecondary, fontSize = 14.sp)
        }
    }
}

private fun Double.formatCurrency(): String {
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).format(this)
}

private fun Long.formatDateTime(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
}

private fun LocalDate.formatShortDate(): String {
    return format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
}
