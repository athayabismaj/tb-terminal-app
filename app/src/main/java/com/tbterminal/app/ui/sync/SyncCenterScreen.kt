package com.tbterminal.app.ui.sync

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.repository.SyncQueueUiModel
import com.tbterminal.app.data.sync.SyncErrorCategory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val PageBackground = Color(0xFFF4F8FA)
private val CardBorder = Color(0xFFDDE6EC)
private val TextPrimary = Color(0xFF111827)
private val TextSecondary = Color(0xFF64748B)
private val Teal = Color(0xFF009B72)

@Composable
fun SyncCenterScreen(
    uiState: SyncCenterUiState,
    onRefresh: () -> Unit,
    onRetryItem: (Long) -> Unit,
    onMarkConflictReviewed: (Long) -> Unit,
    onRetryAllPending: () -> Unit,
    onRetryAllFailed: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val compact = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 700
    val conflictItems = uiState.queueItems.filter { it.syncStatus == SyncStatus.CONFLICT }
    val groupedNonConflictItems = uiState.groupedItems
        .mapValues { (_, items) -> items.filter { it.syncStatus != SyncStatus.CONFLICT } }
        .filterValues { it.isNotEmpty() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(horizontal = if (compact) 16.dp else 30.dp, vertical = if (compact) 16.dp else 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            SyncCenterHeader(
                isRefreshing = uiState.isRefreshing,
                isRetrying = uiState.isRetrying,
                canRetry = uiState.canRetry,
                onRefresh = onRefresh,
                onRetryAllPending = onRetryAllPending,
                onRetryAllFailed = onRetryAllFailed,
                compact = compact
            )
        }

        item {
            if (compact) Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryCard("Menunggu", uiState.totalPending.toString(), Color(0xFFFF9800), Modifier.fillMaxWidth())
                SummaryCard("Sedang Diproses", uiState.totalSyncing.toString(), Color(0xFF2563EB), Modifier.fillMaxWidth())
                SummaryCard("Gagal", uiState.totalFailed.toString(), Color(0xFFDC2626), Modifier.fillMaxWidth())
                SummaryCard("Konflik", uiState.totalConflict.toString(), Color(0xFF7C3AED), Modifier.fillMaxWidth())
                SummaryCard("Berhasil Hari Ini", uiState.totalSyncedToday.toString(), Teal, Modifier.fillMaxWidth())
            } else Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SummaryCard("Pending Sync", uiState.totalPending.toString(), Color(0xFFFF9800), Modifier.weight(1f))
                SummaryCard("Sedang Sinkronisasi", uiState.totalSyncing.toString(), Color(0xFF2563EB), Modifier.weight(1f))
                SummaryCard("Gagal Sinkronisasi", uiState.totalFailed.toString(), Color(0xFFDC2626), Modifier.weight(1f))
                SummaryCard("Conflict", uiState.totalConflict.toString(), Color(0xFF7C3AED), Modifier.weight(1f))
                SummaryCard("Berhasil Hari Ini", uiState.totalSyncedToday.toString(), Teal, Modifier.weight(1f))
            }
        }

        uiState.errorMessage?.let { message ->
            item {
                MessageCard(
                    message = message,
                    isError = true,
                    onDismiss = onDismissMessage
                )
            }
        }

        uiState.message?.let { message ->
            item {
                MessageCard(
                    message = message,
                    isError = false,
                    onDismiss = onDismissMessage
                )
            }
        }

        if (uiState.queueItems.isEmpty()) {
            item {
                EmptyState()
            }
        } else {
            if (conflictItems.isNotEmpty()) {
                item {
                    Text(
                        text = "Konflik Data",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(
                    items = conflictItems,
                    key = { item -> item.localId }
                ) { item ->
                    SyncQueueRow(
                        item = item,
                        canRetry = uiState.canRetry,
                        isRetrying = uiState.isRetrying,
                        onRetry = { onRetryItem(item.localId) },
                        onMarkReviewed = { onMarkConflictReviewed(item.localId) }
                    )
                }
            }

            entityOrder(groupedNonConflictItems.keys).forEach { entityType ->
                val itemsForType = groupedNonConflictItems[entityType].orEmpty()
                if (itemsForType.isNotEmpty()) {
                    item {
                        Text(
                            text = entityType.sectionTitle(),
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    items(
                        items = itemsForType,
                        key = { item -> item.localId }
                    ) { item ->
                        SyncQueueRow(
                            item = item,
                            canRetry = uiState.canRetry,
                            isRetrying = uiState.isRetrying,
                            onRetry = { onRetryItem(item.localId) },
                            onMarkReviewed = { onMarkConflictReviewed(item.localId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncCenterHeader(
    isRefreshing: Boolean,
    isRetrying: Boolean,
    canRetry: Boolean,
    onRefresh: () -> Unit,
    onRetryAllPending: () -> Unit,
    onRetryAllFailed: () -> Unit,
    compact: Boolean
) {
    if (compact) Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Sinkronisasi", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        if (canRetry) {
            OutlinedButton(enabled = !isRefreshing && !isRetrying, onClick = onRetryAllFailed, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) { Text("Coba Lagi yang Gagal") }
            Button(enabled = !isRefreshing && !isRetrying, onClick = onRetryAllPending, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Teal)) {
                Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Sinkronkan Data")
            }
        }
    } else Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Sinkronisasi & Konflik",
                color = TextPrimary,
                fontSize = 31.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Pantau data lokal yang belum tersinkron ke server.",
                color = TextSecondary,
                fontSize = 15.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (canRetry) {
            Spacer(Modifier.width(10.dp))
            OutlinedButton(
                enabled = !isRefreshing && !isRetrying,
                onClick = onRetryAllFailed,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Retry Failed")
            }
            Spacer(Modifier.width(10.dp))
            Button(
                enabled = !isRefreshing && !isRetrying,
                onClick = onRetryAllPending,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal)
            ) {
                Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sync Pending")
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(116.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(accent, RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title.uppercase(),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value,
                color = accent,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SyncQueueRow(
    item: SyncQueueUiModel,
    canRetry: Boolean,
    isRetrying: Boolean,
    onRetry: () -> Unit,
    onMarkReviewed: () -> Unit
) {
    var expanded by rememberSaveable(item.localId) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1.4f)) {
                    Text(
                        text = item.entityType.displayName(),
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Local #${item.entityId}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                StatusBadge(item.syncStatus)
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Retry ${item.retryCount}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = item.updatedAt.formatDateTime(),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (item.lastError != null) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = "Lihat error",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { expanded = !expanded }
                    )
                }
                if (canRetry && item.syncStatus != SyncStatus.SYNCED) {
                    Spacer(Modifier.width(10.dp))
                    OutlinedButton(
                        enabled = !isRetrying && item.syncStatus != SyncStatus.SYNCING,
                        onClick = onRetry,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Retry")
                    }
                }
                if (canRetry && item.syncStatus == SyncStatus.CONFLICT) {
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        enabled = !isRetrying,
                        onClick = onMarkReviewed,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Tandai Ditinjau")
                    }
                }
            }
            if (expanded && item.lastError != null) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = CardBorder)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = item.lastError,
                            color = Color(0xFF991B1B),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (item.syncStatus == SyncStatus.CONFLICT) {
                            Text(
                                text = "Kategori: ${item.errorCategory.displayName()}",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(
                                text = "Rekomendasi: ${item.recommendation}",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Text(
                            text = "Retry count ${item.retryCount} | Update ${item.updatedAt.formatDateTime()}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: SyncStatus) {
    val (bg, fg) = when (status) {
        SyncStatus.PENDING -> Color(0xFFFFF4DE) to Color(0xFFB45309)
        SyncStatus.SYNCING -> Color(0xFFEFF6FF) to Color(0xFF1D4ED8)
        SyncStatus.FAILED -> Color(0xFFFFEDEE) to Color(0xFFDC2626)
        SyncStatus.CONFLICT -> Color(0xFFF3E8FF) to Color(0xFF7C3AED)
        SyncStatus.SYNCED -> Color(0xFFE6F8EF) to Color(0xFF00875A)
    }
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bg
    ) {
        Text(
            text = status.name,
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun MessageCard(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    val bg = if (isError) Color(0xFFFFF1F2) else Color(0xFFE8F7F1)
    val fg = if (isError) Color(0xFFDC2626) else Teal
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(12.dp))
            .clickable(onClick = onDismiss)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(message, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EmptyState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tidak ada data sinkronisasi.",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Data offline akan muncul di sini saat ada antrian sync.",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

private fun entityOrder(keys: Set<SyncEntityType>): List<SyncEntityType> {
    val preferred = listOf(
        SyncEntityType.TRANSACTION,
        SyncEntityType.CASH_SESSION,
        SyncEntityType.CASH_EXPENSE
    )
    return preferred.filter { it in keys } + keys.filterNot { it in preferred }.sortedBy { it.name }
}

private fun SyncEntityType.sectionTitle(): String {
    return when (this) {
        SyncEntityType.TRANSACTION -> "Transaksi"
        SyncEntityType.CASH_SESSION -> "Sesi Kas"
        SyncEntityType.CASH_EXPENSE -> "Pengeluaran Kas"
        SyncEntityType.UNSUPPORTED -> "Data Lama Tidak Didukung"
    }
}

private fun SyncEntityType.displayName(): String {
    return when (this) {
        SyncEntityType.TRANSACTION -> "Transaksi"
        SyncEntityType.CASH_SESSION -> "Sesi Kas"
        SyncEntityType.CASH_EXPENSE -> "Pengeluaran Kas"
        SyncEntityType.UNSUPPORTED -> "Data Lama Tidak Didukung"
    }
}

private fun SyncErrorCategory.displayName(): String {
    return when (this) {
        SyncErrorCategory.NETWORK -> "Network"
        SyncErrorCategory.AUTH -> "Auth"
        SyncErrorCategory.VALIDATION -> "Validasi"
        SyncErrorCategory.CONFLICT -> "Conflict"
        SyncErrorCategory.UNKNOWN -> "Unknown"
    }
}

private fun Long.formatDateTime(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
}
