package com.tbterminal.app.ui.offlinereports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(horizontal = 30.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            OfflineReportHeader(
                isLoading = uiState.isLoading,
                lastRefresh = uiState.lastRefresh,
                onRefresh = onRefresh
            )
        }

        item {
            OfflineReportFilter(
                uiState = uiState,
                onPresetSelected = onPresetSelected,
                onCustomStartChanged = onCustomStartChanged,
                onCustomEndChanged = onCustomEndChanged,
                onApplyCustomRange = onApplyCustomRange
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                KpiCard(
                    title = "Omzet Lokal",
                    value = uiState.salesSummary.localRevenue.formatCurrency(),
                    icon = Icons.Outlined.Assessment,
                    accent = Teal,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Uang Diterima",
                    value = uiState.salesSummary.collectedAmount.formatCurrency(),
                    icon = Icons.Outlined.Payments,
                    accent = Blue,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Sisa Piutang",
                    value = uiState.salesSummary.outstandingAmount.formatCurrency(),
                    icon = Icons.Outlined.CreditCard,
                    accent = Red,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Transaksi",
                    value = uiState.salesSummary.totalTransactions.toString(),
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    accent = Orange,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            OfflineSalesReportSection(
                transactions = uiState.transactions,
                startDate = uiState.startDate,
                endDate = uiState.endDate
            )
        }

        item {
            OfflineCashReportSection(cashSessions = uiState.cashSessions)
        }

        item {
            OfflineReceivableReportSection(receivables = uiState.receivables)
        }

        item {
            OfflineExpenseReportSection(expenses = uiState.expenses)
        }
    }
}

@Composable
private fun OfflineReportHeader(
    isLoading: Boolean,
    lastRefresh: Long?,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Laporan Lokal",
                color = TextPrimary,
                fontSize = 31.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Laporan penjualan, kas, piutang, dan expense dari database lokal.",
                color = TextSecondary,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
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
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Refresh")
        }
    }
}

@Composable
private fun OfflineReportFilter(
    uiState: OfflineReportUiState,
    onPresetSelected: (OfflineReportPreset) -> Unit,
    onCustomStartChanged: (String) -> Unit,
    onCustomEndChanged: (String) -> Unit,
    onApplyCustomRange: () -> Unit
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
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.customStartInput,
                    onValueChange = onCustomStartChanged,
                    label = { Text("Tanggal awal") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = uiState.customEndInput,
                    onValueChange = onCustomEndChanged,
                    label = { Text("Tanggal akhir") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onApplyCustomRange,
                    colors = ButtonDefaults.buttonColors(containerColor = Teal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Text("Terapkan")
                }
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(124.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
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
                fontSize = 24.sp,
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
    endDate: LocalDate
) {
    ReportCard(
        title = "Laporan Penjualan Lokal",
        subtitle = "${startDate.formatShortDate()} sampai ${endDate.formatShortDate()}"
    ) {
        if (transactions.isEmpty()) {
            EmptyRows("Belum ada transaksi lokal pada periode ini.")
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
fun OfflineCashReportSection(cashSessions: List<OfflineCashReportRow>) {
    ReportCard(
        title = "Laporan Kas Lokal",
        subtitle = "Sesi kas lokal dalam periode yang dipilih."
    ) {
        if (cashSessions.isEmpty()) {
            EmptyRows("Belum ada sesi kas lokal pada periode ini.")
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
fun OfflineReceivableReportSection(receivables: List<OfflineReceivableReportRow>) {
    ReportCard(
        title = "Laporan Piutang Lokal",
        subtitle = "Piutang dari transaksi lokal/offline."
    ) {
        if (receivables.isEmpty()) {
            EmptyRows("Belum ada piutang lokal pada periode ini.")
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
fun OfflineExpenseReportSection(expenses: List<OfflineExpenseReportRow>) {
    ReportCard(
        title = "Laporan Expense Lokal",
        subtitle = "Pengeluaran kas yang tersimpan di database lokal."
    ) {
        if (expenses.isEmpty()) {
            EmptyRows("Belum ada pengeluaran kas lokal pada periode ini.")
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
private fun ReportCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
