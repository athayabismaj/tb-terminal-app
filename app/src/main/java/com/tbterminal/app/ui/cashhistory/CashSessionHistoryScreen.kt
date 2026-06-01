package com.tbterminal.app.ui.cashhistory

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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.CashSession
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SessionBackground = Color(0xFFF4FAFD)
private val SessionBorder = Color(0xFFE2E8F0)
private val SessionText = Color(0xFF0F172A)
private val SessionMuted = Color(0xFF64748B)
private val SessionPrimary = Color(0xFF059669)

@Composable
internal fun CashSessionHistoryScreen(
    modifier: Modifier,
    uiState: CashSessionHistoryUiState,
    onStatusFilterChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onShowDetail: (String) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize().background(SessionBackground)
            .verticalScroll(rememberScrollState()).padding(32.dp)
    ) {
        Text("Riwayat Kas Harian", color = SessionText, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        Text("Pantau histori buka dan tutup shift kasir serta selisih rekonsiliasinya.", color = SessionMuted, fontSize = 14.sp)
        Spacer(Modifier.height(22.dp))
        SessionMetrics(uiState)
        Spacer(Modifier.height(18.dp))
        SessionTable(uiState, onStatusFilterChanged, onRefresh, onShowDetail, onPreviousPage, onNextPage)
    }
}

@Composable
private fun SessionMetrics(uiState: CashSessionHistoryUiState) {
    val openCount = uiState.sessions.count { it.status.equals("OPEN", true) }
    val difference = uiState.sessions.fold(BigDecimal.ZERO) { total, item -> total + (item.difference ?: BigDecimal.ZERO) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        SessionMetric("TOTAL SESI", uiState.totalSessions.toString(), "Sesuai histori tersimpan", Icons.AutoMirrored.Outlined.ReceiptLong, SessionPrimary, Modifier.weight(1f))
        SessionMetric("SESI TERBUKA", openCount.toString(), "Pada halaman yang tampil", Icons.Outlined.Schedule, Color(0xFF2563EB), Modifier.weight(1f))
        SessionMetric("SELISIH HALAMAN INI", difference.asCurrency(), "Akumulasi selisih kas", Icons.Outlined.Payments, Color(0xFFF59E0B), Modifier.weight(1f))
    }
}

@Composable
private fun SessionMetric(title: String, value: String, note: String, icon: ImageVector, tint: Color, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(Color.White), border = BorderStroke(1.dp, SessionBorder), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(value, color = SessionText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(note, color = SessionMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SessionTable(
    uiState: CashSessionHistoryUiState,
    onStatusFilterChanged: (String) -> Unit,
    onRefresh: () -> Unit,
    onShowDetail: (String) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), border = BorderStroke(1.dp, SessionBorder), shape = RoundedCornerShape(12.dp)) {
        Column {
            SessionToolbar(uiState.statusFilter, onStatusFilterChanged, onRefresh)
            SessionHeader()
            when {
                uiState.isLoading -> LoadingBox()
                uiState.errorMessage != null -> ErrorBox(uiState.errorMessage, onRefresh)
                uiState.sessions.isEmpty() -> EmptyBox()
                else -> uiState.sessions.forEach { SessionRow(it, onShowDetail) }
            }
            SessionPagination(uiState, onPreviousPage, onNextPage)
        }
    }
}

@Composable
private fun SessionToolbar(selected: String, onSelected: (String) -> Unit, onRefresh: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("Daftar Sesi Kas", color = SessionText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Maksimal 10 sesi per halaman.", color = SessionMuted, fontSize = 12.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf("Semua", "OPEN", "CLOSED").forEach { status ->
                FilterChip(selected = selected == status, onClick = { onSelected(status) }, label = { Text(status.statusLabel()) })
            }
            OutlinedButton(onClick = onRefresh) {
                Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Muat Ulang")
            }
        }
    }
}

@Composable
private fun SessionHeader() {
    Row(Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(horizontal = 18.dp, vertical = 12.dp)) {
        Label("KASIR", Modifier.weight(1.3f))
        Label("DIBUKA", Modifier.weight(1.5f))
        Label("DITUTUP", Modifier.weight(1.5f))
        Label("MODAL AWAL", Modifier.weight(1.1f))
        Label("KAS SISTEM", Modifier.weight(1.1f))
        Label("SELISIH", Modifier.weight(1f))
        Label("STATUS", Modifier.weight(0.9f))
        Label("AKSI", Modifier.weight(0.45f))
    }
}

@Composable
private fun SessionRow(session: CashSession, onShowDetail: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1.3f)) {
            Text(session.userName ?: "Kasir", color = SessionText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(session.userId.take(8), color = SessionMuted, fontSize = 10.sp)
        }
        Text(session.openedAt.asDateTime(), Modifier.weight(1.5f), color = SessionMuted, fontSize = 12.sp)
        Text(session.closedAt?.asDateTime() ?: "-", Modifier.weight(1.5f), color = SessionMuted, fontSize = 12.sp)
        Text(session.openingCash.asCurrency(), Modifier.weight(1.1f), color = SessionText, fontSize = 12.sp)
        Text((session.systemCash ?: session.openingCash).asCurrency(), Modifier.weight(1.1f), color = SessionText, fontSize = 12.sp)
        Text((session.difference ?: BigDecimal.ZERO).asCurrency(), Modifier.weight(1f), color = session.difference.differenceColor(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Box(Modifier.weight(0.9f)) { SessionStatusBadge(session.status) }
        Box(Modifier.weight(0.45f)) {
            IconButton(onClick = { onShowDetail(session.id) }) {
                Icon(Icons.Outlined.Visibility, "Lihat detail", tint = SessionPrimary)
            }
        }
    }
    HorizontalDivider(color = SessionBorder)
}

@Composable
private fun SessionStatusBadge(status: String) {
    val isOpen = status.equals("OPEN", true)
    val tint = if (isOpen) Color(0xFF2563EB) else SessionPrimary
    Surface(color = tint.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp)) {
        Text(status.statusLabel().uppercase(), Modifier.padding(horizontal = 9.dp, vertical = 4.dp), color = tint, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SessionPagination(uiState: CashSessionHistoryUiState, onPreviousPage: () -> Unit, onNextPage: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color(0xFFF8FAFC)).padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Halaman ${uiState.page} dari ${uiState.totalPages}, total ${uiState.totalSessions} sesi", color = SessionMuted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPreviousPage, enabled = uiState.page > 1) { Text("Sebelumnya") }
            OutlinedButton(onClick = onNextPage, enabled = uiState.page < uiState.totalPages) { Text("Berikutnya") }
        }
    }
}

@Composable
private fun LoadingBox() = Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

@Composable
private fun EmptyBox() = Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { Text("Belum ada sesi kas.", color = SessionMuted) }

@Composable
private fun ErrorBox(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().height(180.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(message, color = Color(0xFFDC2626))
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onRetry) { Text("Coba Lagi") }
    }
}

@Composable
private fun Label(text: String, modifier: Modifier) = Text(text, modifier, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)

private fun String.statusLabel(): String = when (this) {
    "OPEN" -> "Terbuka"
    "CLOSED" -> "Ditutup"
    else -> this
}

private fun BigDecimal?.differenceColor(): Color = when {
    this == null || compareTo(BigDecimal.ZERO) == 0 -> SessionMuted
    compareTo(BigDecimal.ZERO) < 0 -> Color(0xFFDC2626)
    else -> SessionPrimary
}

private fun BigDecimal.asCurrency(): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).format(this)

private fun String.asDateTime(): String = runCatching {
    OffsetDateTime.parse(this).atZoneSameInstant(ZoneId.of("Asia/Jakarta"))
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.forLanguageTag("id-ID")))
}.getOrDefault(this)
