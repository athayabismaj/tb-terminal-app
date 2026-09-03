package com.tbterminal.app.ui.cashdetail

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.CashExpense
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.CashTransaction
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DetailBackground = Color(0xFFF4FAFD)
private val DetailBorder = Color(0xFFE2E8F0)
private val DetailText = Color(0xFF0F172A)
private val DetailMuted = Color(0xFF64748B)
private val DetailPrimary = Color(0xFF059669)

@Composable
internal fun CashReconciliationDetailScreen(
    modifier: Modifier,
    uiState: CashReconciliationDetailUiState,
    onOpenHistory: () -> Unit,
    onRetry: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    val compact = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 700
    Column(
        modifier = modifier.fillMaxSize().background(DetailBackground)
            .verticalScroll(rememberScrollState()).padding(if (compact) 16.dp else 32.dp)
    ) {
        DetailHeader(onOpenHistory, compact)
        when {
            uiState.sessionId.isBlank() -> SelectSessionPrompt(onOpenHistory)
            uiState.isLoading -> LoadingBox()
            uiState.errorMessage != null -> ErrorBox(uiState.errorMessage, onRetry)
            uiState.session != null -> DetailContent(uiState, onPreviousPage, onNextPage, compact)
        }
    }
}

@Composable
private fun DetailHeader(onOpenHistory: () -> Unit, compact: Boolean) {
    if (compact) Column(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Detail Kas", color = DetailText, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        OutlinedButton(onClick = onOpenHistory, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Buka Riwayat Kas") }
    } else Row(Modifier.fillMaxWidth().padding(bottom = 22.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("Detail Rekonsiliasi Kas", color = DetailText, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text("Audit kas sistem, kas fisik, pengeluaran, dan transaksi pada satu shift.", color = DetailMuted, fontSize = 14.sp)
        }
        OutlinedButton(onClick = onOpenHistory) { Text("Riwayat Kas Harian") }
    }
}

@Composable
private fun DetailContent(uiState: CashReconciliationDetailUiState, onPreviousPage: () -> Unit, onNextPage: () -> Unit, compact: Boolean) {
    val session = requireNotNull(uiState.session)
    SessionSummary(session, compact)
    Spacer(Modifier.height(16.dp))
    TransactionSection(uiState, onPreviousPage, onNextPage, compact)
    Spacer(Modifier.height(16.dp))
    ExpenseSection(uiState.expenses, compact)
}

@Composable
private fun SessionSummary(session: CashSession, compact: Boolean) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), border = BorderStroke(1.dp, DetailBorder), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(session.userName ?: "Kasir", color = DetailText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("${session.openedAt.asDateTime()} - ${session.closedAt?.asDateTime() ?: "Sesi masih aktif"}", color = DetailMuted, fontSize = 12.sp)
                }
                StatusBadge(session.status)
            }
            Spacer(Modifier.height(18.dp))
            if (compact) Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryValue("MODAL AWAL", session.openingCash.asCurrency(), Modifier.fillMaxWidth())
                SummaryValue("KAS SISTEM", (session.systemCash ?: session.openingCash).asCurrency(), Modifier.fillMaxWidth())
                SummaryValue("KAS FISIK", session.closingCash?.asCurrency() ?: "-", Modifier.fillMaxWidth())
                SummaryValue("PENGELUARAN", session.totalExpenses.asCurrency(), Modifier.fillMaxWidth())
                SummaryValue("SELISIH", session.difference?.asCurrency() ?: "-", Modifier.fillMaxWidth())
            } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryValue("MODAL AWAL", session.openingCash.asCurrency(), Modifier.weight(1f))
                SummaryValue("KAS SISTEM", (session.systemCash ?: session.openingCash).asCurrency(), Modifier.weight(1f))
                SummaryValue("KAS FISIK", session.closingCash?.asCurrency() ?: "-", Modifier.weight(1f))
                SummaryValue("PENGELUARAN", session.totalExpenses.asCurrency(), Modifier.weight(1f))
                SummaryValue("SELISIH", session.difference?.asCurrency() ?: "-", Modifier.weight(1f))
            }
            if (!session.notes.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                Text("Catatan: ${session.notes}", color = DetailMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SummaryValue(title: String, value: String, modifier: Modifier) {
    Box(modifier.background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp)).padding(12.dp)) {
        Column {
            Text(title, color = Color(0xFF94A3B8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(value, color = DetailText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TransactionSection(uiState: CashReconciliationDetailUiState, onPreviousPage: () -> Unit, onNextPage: () -> Unit, compact: Boolean) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), border = BorderStroke(1.dp, DetailBorder), shape = RoundedCornerShape(12.dp)) {
        Column {
            SectionTitle("Transaksi Dalam Sesi", "${uiState.totalTransactions} transaksi")
            if (!compact) TransactionHeader()
            if (uiState.transactions.isEmpty()) {
                EmptyRow("Belum ada transaksi pada sesi ini.")
            } else {
                uiState.transactions.forEach { if (compact) TransactionMobileRow(it) else TransactionRow(it) }
            }
            Row(Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Halaman ${uiState.page} dari ${uiState.totalPages}", color = DetailMuted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onPreviousPage, enabled = uiState.page > 1) { Text(if (compact) "‹" else "Sebelumnya") }
                    OutlinedButton(onClick = onNextPage, enabled = uiState.page < uiState.totalPages) { Text(if (compact) "›" else "Berikutnya") }
                }
            }
        }
    }
}

@Composable
private fun TransactionHeader() {
    Row(Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(horizontal = 18.dp, vertical = 11.dp)) {
        Label("NO TRANSAKSI", Modifier.weight(1.5f))
        Label("CUSTOMER", Modifier.weight(1.4f))
        Label("TANGGAL", Modifier.weight(1.5f))
        Label("TOTAL", Modifier.weight(1f))
        Label("STATUS", Modifier.weight(0.8f))
    }
}

@Composable
private fun TransactionRow(item: CashTransaction) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(item.receiptId, Modifier.weight(1.5f), color = DetailPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(item.customerName ?: "Umum", Modifier.weight(1.4f), color = DetailText, fontSize = 12.sp)
        Text(item.createdAt.asDateTime(), Modifier.weight(1.5f), color = DetailMuted, fontSize = 12.sp)
        Text(item.total.asCurrency(), Modifier.weight(1f), color = DetailText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Box(Modifier.weight(0.8f)) { StatusBadge(item.status) }
    }
    HorizontalDivider(color = DetailBorder)
}

@Composable
private fun TransactionMobileRow(item: CashTransaction) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(item.receiptId, color = DetailPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            StatusBadge(item.status)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(item.customerName ?: "Pelanggan umum", color = DetailText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(item.createdAt.asDateTime(), color = DetailMuted, fontSize = 11.sp)
            }
            Text(item.total.asCurrency(), color = DetailText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
    HorizontalDivider(color = DetailBorder)
}

@Composable
private fun ExpenseSection(expenses: List<CashExpense>, compact: Boolean) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), border = BorderStroke(1.dp, DetailBorder), shape = RoundedCornerShape(12.dp)) {
        Column {
            SectionTitle("Pengeluaran Kas", "${expenses.size} catatan")
            if (!compact) Row(Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(horizontal = 18.dp, vertical = 11.dp)) {
                Label("DESKRIPSI", Modifier.weight(2f))
                Label("WAKTU", Modifier.weight(1.4f))
                Label("NOMINAL", Modifier.weight(1f))
            }
            if (expenses.isEmpty()) {
                EmptyRow("Tidak ada pengeluaran pada sesi ini.")
            } else {
                expenses.forEach { if (compact) ExpenseMobileRow(it) else ExpenseRow(it) }
            }
        }
    }
}

@Composable
private fun ExpenseRow(item: CashExpense) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp)) {
        Text(item.description, Modifier.weight(2f), color = DetailText, fontSize = 12.sp)
        Text(item.createdAt.asDateTime(), Modifier.weight(1.4f), color = DetailMuted, fontSize = 12.sp)
        Text(item.amount.asCurrency(), Modifier.weight(1f), color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
    HorizontalDivider(color = DetailBorder)
}

@Composable
private fun ExpenseMobileRow(item: CashExpense) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.description, color = DetailText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(item.createdAt.asDateTime(), color = DetailMuted, fontSize = 11.sp)
        }
        Text(item.amount.asCurrency(), color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
    HorizontalDivider(color = DetailBorder)
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, color = DetailText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = DetailMuted, fontSize = 12.sp)
    }
}

@Composable
private fun StatusBadge(status: String) {
    val isClosedOrPaid = status.equals("CLOSED", true) || status.equals("lunas", true)
    val tint = if (isClosedOrPaid) DetailPrimary else Color(0xFFF59E0B)
    Surface(color = tint.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp)) {
        Text(status.replace('_', ' ').uppercase(), Modifier.padding(horizontal = 9.dp, vertical = 4.dp), color = tint, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SelectSessionPrompt(onOpenHistory: () -> Unit) {
    Column(Modifier.fillMaxWidth().height(240.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("Pilih sesi kas dari halaman Riwayat Kas Harian.", color = DetailMuted)
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onOpenHistory) { Text("Buka Riwayat Kas") }
    }
}

@Composable
private fun LoadingBox() = com.tbterminal.app.ui.components.SkeletonList(
    modifier = Modifier.fillMaxWidth().height(240.dp),
    itemCount = 5,
)

@Composable
private fun ErrorBox(message: String, onRetry: () -> Unit) {
    com.tbterminal.app.ui.components.AppErrorState(
        message = message,
        onRetry = onRetry,
        modifier = Modifier.height(240.dp),
    )
}

@Composable
private fun EmptyRow(message: String) = Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { Text(message, color = DetailMuted) }

@Composable
private fun Label(text: String, modifier: Modifier) = Text(text, modifier, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)

private fun BigDecimal.asCurrency(): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).format(this)

private fun String.asDateTime(): String = runCatching {
    OffsetDateTime.parse(this).atZoneSameInstant(ZoneId.of("Asia/Jakarta"))
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.forLanguageTag("id-ID")))
}.getOrDefault(this)
