package com.tbterminal.app.ui.dashboard.offline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.runtime.Composable

private val LocalSurface = Color.White
private val LocalBorder = Color(0xFFDDE6EC)
private val TextPrimary = Color(0xFF111827)
private val TextSecondary = Color(0xFF64748B)
private val Teal = Color(0xFF009B72)
private val Blue = Color(0xFF2563EB)
private val Orange = Color(0xFFF59E0B)
private val Red = Color(0xFFDC2626)

@Composable
fun OfflineDashboardSection(
    uiState: OfflineDashboardUiState,
    onSyncCenterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val compact = maxWidth < 600.dp
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OfflineDashboardHeader(
                compact = compact,
                lastRefresh = uiState.lastRefresh,
                onSyncCenterClick = onSyncCenterClick
            )

            SyncWarningBanner(
                pendingSyncCount = uiState.pendingSyncCount,
                failedSyncCount = uiState.failedSyncCount
            )

            OfflineKpiGrid(uiState = uiState, compact = compact)
            OfflineSyncCountGrid(uiState = uiState, compact = compact)
        }
    }
}

@Composable
private fun OfflineDashboardHeader(
    compact: Boolean,
    lastRefresh: Long,
    onSyncCenterClick: () -> Unit
) {
    val title: @Composable () -> Unit = {
        Column {
            Text("Ringkasan Perangkat", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Data yang tersimpan di perangkat ini.", color = TextSecondary, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
    val action: @Composable () -> Unit = {
        OutlinedButton(onClick = onSyncCenterClick, shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Lihat Sinkronisasi")
        }
    }

    if (compact) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            title()
            Text("Diperbarui ${lastRefresh.formatRefreshTime()}", color = TextSecondary, fontSize = 12.sp)
            action()
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) { title() }
            Text("Diperbarui ${lastRefresh.formatRefreshTime()}", color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.width(10.dp))
            action()
        }
    }
}

@Composable
private fun OfflineKpiGrid(uiState: OfflineDashboardUiState, compact: Boolean) {
    val items = listOf(
        OfflineKpi("Omzet Lokal", uiState.localRevenueToday.asCurrency(), "Transaksi lokal hari ini", Icons.AutoMirrored.Outlined.TrendingUp, Teal),
        OfflineKpi("Uang Diterima", uiState.localCollectedToday.asCurrency(), "Pembayaran lokal hari ini", Icons.Outlined.Payments, Blue),
        OfflineKpi("Piutang Lokal", uiState.localReceivableOutstanding.asCurrency(), "Sisa piutang di perangkat", Icons.Outlined.AccountBalanceWallet, Orange),
        OfflineKpi("Pengeluaran Hari Ini", uiState.localExpenseToday.asCurrency(), "Pengeluaran lokal", Icons.AutoMirrored.Outlined.ReceiptLong, Red)
    )
    val columns = if (compact) 2 else 4
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items.chunked(columns).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                rowItems.forEach { item ->
                    LocalKpiCard(
                        title = item.title,
                        value = item.value,
                        subtitle = item.subtitle,
                        icon = item.icon,
                        tint = item.tint,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun OfflineSyncCountGrid(uiState: OfflineDashboardUiState, compact: Boolean) {
    val items = listOf(
        Triple("Transaksi Lokal", uiState.localTransactionCount.toString(), Teal),
        Triple("Menunggu Sinkron", uiState.pendingSyncCount.toString(), Orange),
        Triple("Gagal Sinkron", uiState.failedSyncCount.toString(), Red)
    )
    val columns = if (compact) 1 else 3
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items.chunked(columns).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                rowItems.forEach { item ->
                    LocalCompactCard(
                        title = item.first,
                        value = item.second,
                        tint = item.third,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private data class OfflineKpi(
    val title: String,
    val value: String,
    val subtitle: String,
    val icon: ImageVector,
    val tint: Color
)

@Composable
private fun SyncWarningBanner(
    pendingSyncCount: Int,
    failedSyncCount: Int
) {
    if (pendingSyncCount <= 0 && failedSyncCount <= 0) return
    val isFailed = failedSyncCount > 0
    val background = if (isFailed) Color(0xFFFFF1F2) else Color(0xFFFFF7E6)
    val foreground = if (isFailed) Red else Color(0xFFB45309)
    val message = when {
        failedSyncCount > 0 && pendingSyncCount > 0 ->
            "Ada $pendingSyncCount data belum tersinkron dan $failedSyncCount data gagal sinkronisasi."
        failedSyncCount > 0 ->
            "Ada $failedSyncCount data gagal sinkronisasi."
        else ->
            "Ada $pendingSyncCount data yang belum tersinkron."
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isFailed) Icons.Outlined.ErrorOutline else Icons.Outlined.CloudSync,
            contentDescription = null,
            tint = foreground,
            modifier = Modifier.size(19.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = message,
            color = foreground,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun LocalKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(144.dp),
        colors = CardDefaults.cardColors(containerColor = LocalSurface),
        border = BorderStroke(1.dp, LocalBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(tint.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title.uppercase(),
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column {
                Text(
                    text = value,
                    color = tint,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun LocalCompactCard(
    title: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(78.dp),
        colors = CardDefaults.cardColors(containerColor = LocalSurface),
        border = BorderStroke(1.dp, LocalBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(tint, RoundedCornerShape(9.dp))
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(value, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun Double.asCurrency(): String {
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).format(this)
}

private fun Long.formatRefreshTime(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("HH:mm"))
}
