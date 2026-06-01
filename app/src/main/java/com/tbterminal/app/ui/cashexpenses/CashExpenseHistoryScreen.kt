package com.tbterminal.app.ui.cashexpenses

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.CashExpense
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ExpenseBackground = Color(0xFFF4FAFD)
private val ExpenseBorder = Color(0xFFE2E8F0)
private val ExpenseText = Color(0xFF0F172A)
private val ExpenseMuted = Color(0xFF64748B)
private val ExpensePrimary = Color(0xFF059669)
private val ExpenseDanger = Color(0xFFDC2626)

@Composable
internal fun CashExpenseHistoryScreen(
    modifier: Modifier,
    uiState: CashExpenseHistoryUiState,
    onRefresh: () -> Unit,
    onShowSessionDetail: (String) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Column(
        modifier.fillMaxSize().background(ExpenseBackground)
            .verticalScroll(rememberScrollState()).padding(32.dp)
    ) {
        Text("Pengeluaran Kas", color = ExpenseText, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        Text("Pantau kas keluar operasional yang dicatat pada setiap sesi kasir.", color = ExpenseMuted, fontSize = 14.sp)
        Spacer(Modifier.height(22.dp))
        ExpenseMetrics(uiState)
        Spacer(Modifier.height(18.dp))
        ExpenseTable(uiState, onRefresh, onShowSessionDetail, onPreviousPage, onNextPage)
    }
}

@Composable
private fun ExpenseMetrics(uiState: CashExpenseHistoryUiState) {
    val totalValue = uiState.expenses.fold(BigDecimal.ZERO) { total, item -> total + item.amount }
    val sessionCount = uiState.expenses.map(CashExpense::sessionId).distinct().size
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ExpenseMetric("TOTAL CATATAN", uiState.totalExpenses.toString(), "Sesuai data tersimpan", Icons.AutoMirrored.Outlined.ReceiptLong, ExpensePrimary, Modifier.weight(1f))
        ExpenseMetric("NILAI HALAMAN INI", totalValue.asCurrency(), "Maksimal 10 pengeluaran", Icons.Outlined.Payments, ExpenseDanger, Modifier.weight(1f))
        ExpenseMetric("SESI TERKAIT", sessionCount.toString(), "Pada halaman yang tampil", Icons.AutoMirrored.Outlined.ReceiptLong, Color(0xFF2563EB), Modifier.weight(1f))
    }
}

@Composable
private fun ExpenseMetric(title: String, value: String, note: String, icon: ImageVector, tint: Color, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(Color.White), border = BorderStroke(1.dp, ExpenseBorder), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(value, color = ExpenseText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(note, color = ExpenseMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ExpenseTable(
    uiState: CashExpenseHistoryUiState,
    onRefresh: () -> Unit,
    onShowSessionDetail: (String) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), border = BorderStroke(1.dp, ExpenseBorder), shape = RoundedCornerShape(12.dp)) {
        Column {
            ExpenseToolbar(onRefresh)
            ExpenseHeader()
            when {
                uiState.isLoading -> LoadingBox()
                uiState.errorMessage != null -> ErrorBox(uiState.errorMessage, onRefresh)
                uiState.expenses.isEmpty() -> EmptyBox()
                else -> uiState.expenses.forEach { ExpenseRow(it, onShowSessionDetail) }
            }
            ExpensePagination(uiState, onPreviousPage, onNextPage)
        }
    }
}

@Composable
private fun ExpenseToolbar(onRefresh: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("Daftar Pengeluaran", color = ExpenseText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Data kas keluar berasal dari cash session tersimpan.", color = ExpenseMuted, fontSize = 12.sp)
        }
        OutlinedButton(onClick = onRefresh) {
            Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Muat Ulang")
        }
    }
}

@Composable
private fun ExpenseHeader() {
    Row(Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(horizontal = 18.dp, vertical = 12.dp)) {
        Label("KASIR", Modifier.weight(1.2f))
        Label("SESI", Modifier.weight(1.25f))
        Label("WAKTU", Modifier.weight(1.55f))
        Label("DESKRIPSI", Modifier.weight(2f))
        Label("NOMINAL", Modifier.weight(1.1f))
        Label("AKSI", Modifier.weight(0.45f))
    }
}

@Composable
private fun ExpenseRow(item: CashExpense, onShowSessionDetail: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1.2f)) {
            Text(item.userName ?: "Kasir", color = ExpenseText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(item.userId.take(8), color = ExpenseMuted, fontSize = 10.sp)
        }
        Text(item.sessionId.take(8), Modifier.weight(1.25f), color = ExpenseMuted, fontSize = 12.sp)
        Text(item.createdAt.asDateTime(), Modifier.weight(1.55f), color = ExpenseMuted, fontSize = 12.sp)
        Text(item.description, Modifier.weight(2f), color = ExpenseText, fontSize = 12.sp)
        Text(item.amount.asCurrency(), Modifier.weight(1.1f), color = ExpenseDanger, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Box(Modifier.weight(0.45f)) {
            IconButton(onClick = { onShowSessionDetail(item.sessionId) }) {
                Icon(Icons.Outlined.Visibility, "Lihat detail sesi", tint = ExpensePrimary)
            }
        }
    }
    HorizontalDivider(color = ExpenseBorder)
}

@Composable
private fun ExpensePagination(uiState: CashExpenseHistoryUiState, onPreviousPage: () -> Unit, onNextPage: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Halaman ${uiState.page} dari ${uiState.totalPages}, total ${uiState.totalExpenses} pengeluaran", color = ExpenseMuted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPreviousPage, enabled = uiState.page > 1) { Text("Sebelumnya") }
            OutlinedButton(onClick = onNextPage, enabled = uiState.page < uiState.totalPages) { Text("Berikutnya") }
        }
    }
}

@Composable
private fun LoadingBox() = Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

@Composable
private fun EmptyBox() = Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { Text("Belum ada pengeluaran kas.", color = ExpenseMuted) }

@Composable
private fun ErrorBox(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().height(180.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(message, color = ExpenseDanger)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRetry) { Text("Coba Lagi") }
    }
}

@Composable
private fun Label(text: String, modifier: Modifier) = Text(text, modifier, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)

private fun BigDecimal.asCurrency(): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).format(this)

private fun String.asDateTime(): String = runCatching {
    OffsetDateTime.parse(this).atZoneSameInstant(ZoneId.of("Asia/Jakarta"))
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.forLanguageTag("id-ID")))
}.getOrDefault(this)
