package com.tbterminal.app.ui.offline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.ui.dashboard.DashboardBrandGreenDark
import com.tbterminal.app.ui.dashboard.DashboardSurface
import com.tbterminal.app.ui.dashboard.DashboardTextPrimary
import com.tbterminal.app.ui.dashboard.DashboardTextSecondary
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val OfflineLine = Color(0xFFE2E8F0)
private val OfflineSoft = Color(0xFFF8FAFC)
private val OfflineWarning = Color(0xFFF59E0B)
private val OfflineDanger = Color(0xFFDC2626)

@Composable
fun LocalPendingTransactionsCard(
    transactions: List<LocalPendingTransactionUi>,
    message: String? = null,
    bulkProgressMessage: String? = null,
    isBulkSyncing: Boolean = false,
    onSyncClick: ((Long) -> Unit)? = null,
    onSyncAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val canBulkSync = transactions.any { it.syncStatus == SyncStatus.PENDING || it.syncStatus == SyncStatus.FAILED }
    val shouldShowBulkSync = canBulkSync || isBulkSyncing
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DashboardSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, OfflineLine)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Transaksi Offline Belum Tersinkron",
                        color = DashboardTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Data lokal belum dikirim ke server. Struk server belum tersedia.",
                        color = DashboardTextSecondary,
                        fontSize = 12.sp
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = OfflineWarning.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "${transactions.size} pending",
                            color = OfflineWarning,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    if (shouldShowBulkSync && onSyncAllClick != null) {
                        OutlinedButton(
                            onClick = onSyncAllClick,
                            enabled = !isBulkSyncing
                        ) {
                            Text(
                                text = if (isBulkSyncing) "Menyinkronkan..." else "Sinkronkan Semua",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (!bulkProgressMessage.isNullOrBlank()) {
                Text(
                    text = bulkProgressMessage,
                    color = Color(0xFF2563EB),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            if (!message.isNullOrBlank()) {
                Text(
                    text = message,
                    color = if (message.contains("berhasil", ignoreCase = true)) DashboardBrandGreenDark else OfflineDanger,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            HorizontalDivider(color = OfflineLine)

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OfflineSoft)
                        .padding(horizontal = 24.dp, vertical = 22.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "Tidak ada transaksi offline tertunda.",
                        color = DashboardTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                LocalPendingHeader()
                transactions.forEachIndexed { index, transaction ->
                    LocalPendingRow(
                        transaction = transaction,
                        onSyncClick = onSyncClick,
                        syncActionsEnabled = !isBulkSyncing
                    )
                    if (index < transactions.lastIndex) {
                        HorizontalDivider(color = OfflineLine.copy(alpha = 0.75f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalPendingHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OfflineSoft)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderText("TRANSAKSI LOKAL", Modifier.weight(1.6f))
        HeaderText("WAKTU", Modifier.weight(1.1f))
        HeaderText("TOTAL", Modifier.weight(1f), Alignment.End)
        HeaderText("DIBAYAR", Modifier.weight(1f), Alignment.End)
        HeaderText("SISA", Modifier.weight(1f), Alignment.End)
        HeaderText("SYNC", Modifier.weight(0.9f), Alignment.End)
        HeaderText("AKSI", Modifier.weight(1.1f), Alignment.End)
    }
}

@Composable
private fun LocalPendingRow(
    transaction: LocalPendingTransactionUi,
    onSyncClick: ((Long) -> Unit)?,
    syncActionsEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.6f)) {
            Text(
                text = transaction.transactionCode,
                color = DashboardBrandGreenDark,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Lokal #${transaction.localId}",
                color = DashboardTextSecondary,
                fontSize = 11.sp
            )
            if (
                transaction.syncStatus in listOf(SyncStatus.FAILED, SyncStatus.CONFLICT) &&
                !transaction.lastError.isNullOrBlank()
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = transaction.lastError,
                    color = OfflineDanger,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = transaction.occurredAt.formatLocalTime(),
            color = DashboardTextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1.1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        MoneyText(transaction.total, Modifier.weight(1f))
        MoneyText(transaction.paidAmount, Modifier.weight(1f))
        MoneyText(transaction.remainingAmount, Modifier.weight(1f), danger = transaction.remainingAmount > BigDecimal.ZERO)
        Box(modifier = Modifier.weight(0.9f), contentAlignment = Alignment.CenterEnd) {
            SyncStatusBadge(transaction.syncStatus)
        }
        Box(modifier = Modifier.weight(1.1f), contentAlignment = Alignment.CenterEnd) {
            if (onSyncClick != null) {
                OutlinedButton(
                    onClick = { onSyncClick(transaction.localId) },
                    enabled = syncActionsEnabled && transaction.syncStatus != SyncStatus.SYNCING
                ) {
                    Text(
                        text = if (transaction.syncStatus == SyncStatus.SYNCING) "Sync..." else "Sinkronkan",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderText(
    text: String,
    modifier: Modifier,
    align: Alignment.Horizontal = Alignment.Start
) {
    Column(modifier = modifier, horizontalAlignment = align) {
        Text(
            text = text,
            color = DashboardTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}

@Composable
private fun MoneyText(value: BigDecimal, modifier: Modifier, danger: Boolean = false) {
    Text(
        text = value.moneyText(),
        color = if (danger) OfflineDanger else DashboardTextPrimary,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        textAlign = TextAlign.End,
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun SyncStatusBadge(status: SyncStatus) {
    val color = when (status) {
        SyncStatus.PENDING -> OfflineWarning
        SyncStatus.SYNCING -> Color(0xFF2563EB)
        SyncStatus.FAILED -> OfflineDanger
        SyncStatus.CONFLICT -> Color(0xFF7C3AED)
        SyncStatus.SYNCED -> DashboardBrandGreenDark
    }
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = status.name,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

private fun BigDecimal.moneyText(): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).format(this)

private fun Long.formatLocalTime(): String = runCatching {
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd MMM HH:mm", Locale.forLanguageTag("id-ID")))
}.getOrDefault("-")
