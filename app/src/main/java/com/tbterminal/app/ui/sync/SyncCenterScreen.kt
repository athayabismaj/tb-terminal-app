package com.tbterminal.app.ui.sync

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.repository.SyncQueueUiModel
import com.tbterminal.app.data.sync.SyncErrorCategory
import com.tbterminal.app.ui.theme.TbAmber
import com.tbterminal.app.ui.theme.TbBackground
import com.tbterminal.app.ui.theme.TbError
import com.tbterminal.app.ui.theme.TbGreen
import com.tbterminal.app.ui.theme.TbGreenDark
import com.tbterminal.app.ui.theme.TbGreenLight
import com.tbterminal.app.ui.theme.TbOutline
import com.tbterminal.app.ui.theme.TbSurface
import com.tbterminal.app.ui.theme.TbSurfaceMuted
import com.tbterminal.app.ui.theme.TbText
import com.tbterminal.app.ui.theme.TbTextMuted
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.components.SkeletonList
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SyncCenterScreen(
    uiState: SyncCenterUiState,
    onRetryItem: (Long) -> Unit,
    onMarkConflictReviewed: (Long) -> Unit,
    onRetryAllPending: () -> Unit,
    onRetryAllFailed: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val conflictItems = uiState.queueItems.filter { it.syncStatus == SyncStatus.CONFLICT }
    val groupedNonConflictItems = uiState.groupedItems
        .mapValues { (_, items) -> items.filter { it.syncStatus != SyncStatus.CONFLICT } }
        .filterValues { it.isNotEmpty() }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(TbBackground)) {
        val compact = maxWidth < 700.dp
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (compact) 16.dp else 32.dp,
                    vertical = if (compact) 14.dp else 24.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 18.dp),
        ) {
            if (uiState.isInitialLoading) {
                item { SyncOverviewSkeleton(compact) }
                item { SkeletonList(modifier = Modifier.fillMaxWidth(), itemCount = 3) }
            } else {
                item {
                    SyncOverviewCard(
                        uiState = uiState,
                        compact = compact,
                        onRetryAllPending = onRetryAllPending,
                        onRetryAllFailed = onRetryAllFailed,
                    )
                }
                uiState.errorMessage?.let { message ->
                    item { MessageCard(message, isError = true, onDismissMessage) }
                }
                uiState.message?.let { message ->
                    item { MessageCard(message, isError = false, onDismissMessage) }
                }

                if (uiState.queueItems.isEmpty()) {
                    item { EmptyState() }
                } else {
                    if (conflictItems.isNotEmpty()) {
                        item { QueueSectionTitle("Perlu ditinjau", conflictItems.size) }
                        items(conflictItems, key = { it.localId }) { item ->
                            SyncQueueCard(
                                item = item,
                                compact = compact,
                                canRetry = uiState.canRetry,
                                isRetrying = uiState.isRetrying,
                                onRetry = { onRetryItem(item.localId) },
                                onMarkReviewed = { onMarkConflictReviewed(item.localId) },
                            )
                        }
                    }

                    entityOrder(groupedNonConflictItems.keys).forEach { entityType ->
                        val entries = groupedNonConflictItems[entityType].orEmpty()
                        if (entries.isNotEmpty()) {
                            item { QueueSectionTitle(entityType.sectionTitle(), entries.size) }
                            items(entries, key = { it.localId }) { item ->
                                SyncQueueCard(
                                    item = item,
                                    compact = compact,
                                    canRetry = uiState.canRetry,
                                    isRetrying = uiState.isRetrying,
                                    onRetry = { onRetryItem(item.localId) },
                                    onMarkReviewed = { onMarkConflictReviewed(item.localId) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncOverviewSkeleton(compact: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("sync-initial-skeleton"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = TbSurface),
        border = BorderStroke(1.dp, TbOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(if (compact) 16.dp else 18.dp)) {
            SkeletonBox(Modifier.fillMaxWidth(if (compact) 0.54f else 0.24f).height(18.dp))
            Spacer(Modifier.height(8.dp))
            SkeletonBox(Modifier.fillMaxWidth(if (compact) 0.78f else 0.38f).height(12.dp))
            Spacer(Modifier.height(16.dp))
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SkeletonBox(Modifier.weight(1f).height(60.dp))
                            SkeletonBox(Modifier.weight(1f).height(60.dp))
                        }
                    }
                    SkeletonBox(Modifier.fillMaxWidth().height(60.dp))
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(5) { SkeletonBox(Modifier.weight(1f).height(60.dp)) }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBox(Modifier.weight(1f).height(48.dp))
                SkeletonBox(Modifier.weight(1f).height(48.dp))
            }
        }
    }
}

@Composable
private fun SyncOverviewCard(
    uiState: SyncCenterUiState,
    compact: Boolean,
    onRetryAllPending: () -> Unit,
    onRetryAllFailed: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = TbSurface),
        border = BorderStroke(1.dp, TbOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(if (compact) 16.dp else 18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        text = "Status sinkronisasi",
                        color = TbText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Pantau antrean data perangkat ke server.",
                        color = TbTextMuted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            SyncMetrics(uiState, compact)
            if (uiState.canRetry) {
                Spacer(Modifier.height(14.dp))
                SyncBatchActions(
                    compact = compact,
                    enabled = !uiState.isRefreshing && !uiState.isRetrying,
                    hasFailed = uiState.totalFailed > 0,
                    hasPending = uiState.totalPending > 0,
                    onRetryFailed = onRetryAllFailed,
                    onSyncPending = onRetryAllPending,
                )
            }
        }
    }
}

@Composable
private fun SyncMetrics(uiState: SyncCenterUiState, compact: Boolean) {
    if (compact) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SyncMetric("Menunggu", uiState.totalPending, TbAmber, Modifier.weight(1f))
                SyncMetric("Diproses", uiState.totalSyncing, TbGreenDark, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SyncMetric("Gagal", uiState.totalFailed, TbError, Modifier.weight(1f))
                SyncMetric("Konflik", uiState.totalConflict, TbError, Modifier.weight(1f))
            }
            SyncMetric("Berhasil hari ini", uiState.totalSyncedToday, TbGreenDark, Modifier.fillMaxWidth())
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SyncMetric("Menunggu", uiState.totalPending, TbAmber, Modifier.weight(1f))
            SyncMetric("Diproses", uiState.totalSyncing, TbGreenDark, Modifier.weight(1f))
            SyncMetric("Gagal", uiState.totalFailed, TbError, Modifier.weight(1f))
            SyncMetric("Konflik", uiState.totalConflict, TbError, Modifier.weight(1f))
            SyncMetric("Berhasil hari ini", uiState.totalSyncedToday, TbGreenDark, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SyncMetric(label: String, value: Int, accent: Color, modifier: Modifier) {
    Surface(modifier = modifier, color = TbSurfaceMuted, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = value.toString(),
                color = if (value == 0) TbTextMuted else accent,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                color = TbTextMuted,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SyncBatchActions(
    compact: Boolean,
    enabled: Boolean,
    hasFailed: Boolean,
    hasPending: Boolean,
    onRetryFailed: () -> Unit,
    onSyncPending: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val buttonModifier = if (compact) Modifier.weight(1f) else Modifier.width(210.dp)
        OutlinedButton(
            onClick = onRetryFailed,
            enabled = enabled && hasFailed,
            modifier = buttonModifier.height(48.dp),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 10.dp),
        ) {
            Text(if (compact) "Coba gagal" else "Coba lagi yang gagal", maxLines = 1)
        }
        Button(
            onClick = onSyncPending,
            enabled = enabled && hasPending,
            modifier = buttonModifier.height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TbGreen),
            contentPadding = PaddingValues(horizontal = 10.dp),
        ) {
            Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (compact) "Sinkronkan" else "Sinkronkan data", maxLines = 1)
        }
    }
}

@Composable
private fun QueueSectionTitle(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = TbText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Surface(color = TbSurfaceMuted, shape = RoundedCornerShape(999.dp)) {
            Text(
                text = count.toString(),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                color = TbTextMuted,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SyncQueueCard(
    item: SyncQueueUiModel,
    compact: Boolean,
    canRetry: Boolean,
    isRetrying: Boolean,
    onRetry: () -> Unit,
    onMarkReviewed: () -> Unit,
) {
    var expanded by rememberSaveable(item.localId) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TbSurface),
        border = BorderStroke(1.dp, TbOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (compact) CompactQueueHeader(item) else ExpandedQueueHeader(item)

            if (item.lastError != null) {
                Spacer(Modifier.height(10.dp))
                ErrorDisclosure(
                    expanded = expanded,
                    isConflict = item.syncStatus == SyncStatus.CONFLICT,
                    onClick = { expanded = !expanded },
                )
                if (expanded) ErrorDetails(item)
            }

            if (canRetry && item.syncStatus != SyncStatus.SYNCED) {
                Spacer(Modifier.height(12.dp))
                QueueActions(
                    compact = compact,
                    item = item,
                    enabled = !isRetrying && item.syncStatus != SyncStatus.SYNCING,
                    onRetry = onRetry,
                    onMarkReviewed = onMarkReviewed,
                )
            }
        }
    }
}

@Composable
private fun CompactQueueHeader(item: SyncQueueUiModel) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        QueueIcon()
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                text = item.entityType.displayName(),
                color = TbText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text("Data lokal #${item.entityId}", color = TbTextMuted, style = MaterialTheme.typography.bodySmall)
        }
        StatusBadge(item.syncStatus)
    }
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = item.updatedAt.formatDateTime(),
            modifier = Modifier.weight(1f),
            color = TbTextMuted,
            style = MaterialTheme.typography.labelMedium,
        )
        Text("Percobaan ${item.retryCount}", color = TbTextMuted, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ExpandedQueueHeader(item: SyncQueueUiModel) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        QueueIcon()
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                text = item.entityType.displayName(),
                color = TbText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text("Data lokal #${item.entityId}", color = TbTextMuted, style = MaterialTheme.typography.bodySmall)
        }
        StatusBadge(item.syncStatus)
        Column(Modifier.padding(start = 24.dp), horizontalAlignment = Alignment.End) {
            Text(item.updatedAt.formatDateTime(), color = TbTextMuted, style = MaterialTheme.typography.bodySmall)
            Text("Percobaan ${item.retryCount}", color = TbTextMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun QueueIcon() {
    Box(
        modifier = Modifier.size(42.dp).background(TbGreenLight, RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.Sync, contentDescription = null, tint = TbGreenDark, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun ErrorDisclosure(expanded: Boolean, isConflict: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = if (isConflict) TbError.copy(alpha = 0.07f) else TbSurfaceMuted,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = if (isConflict) TbError else TbTextMuted,
                modifier = Modifier.size(19.dp),
            )
            Text(
                text = if (isConflict) "Periksa konflik data" else "Lihat kendala sinkronisasi",
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                color = if (isConflict) TbError else TbText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
            IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Tutup detail" else "Buka detail",
                    tint = TbTextMuted,
                )
            }
        }
    }
}

@Composable
private fun ErrorDetails(item: SyncQueueUiModel) {
    Spacer(Modifier.height(10.dp))
    HorizontalDivider(color = TbOutline)
    Column(Modifier.padding(top = 10.dp)) {
        Text(
            text = item.lastError.orEmpty(),
            color = TbText,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
        if (item.syncStatus == SyncStatus.CONFLICT) {
            Text(
                text = "Jenis kendala: ${item.errorCategory.displayName()}",
                color = TbText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = item.recommendation,
                color = TbTextMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun QueueActions(
    compact: Boolean,
    item: SyncQueueUiModel,
    enabled: Boolean,
    onRetry: () -> Unit,
    onMarkReviewed: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
    ) {
        val actionModifier = if (compact) Modifier.weight(1f) else Modifier.width(170.dp)
        OutlinedButton(
            onClick = onRetry,
            enabled = enabled,
            modifier = actionModifier.height(48.dp),
            shape = RoundedCornerShape(13.dp),
            contentPadding = PaddingValues(horizontal = 10.dp),
        ) {
            Text("Coba lagi", maxLines = 1)
        }
        if (item.syncStatus == SyncStatus.CONFLICT) {
            TextButton(
                onClick = onMarkReviewed,
                enabled = enabled,
                modifier = actionModifier.height(48.dp),
                contentPadding = PaddingValues(horizontal = 10.dp),
            ) {
                Text("Tandai ditinjau", maxLines = 1)
            }
        }
    }
}

@Composable
private fun StatusBadge(status: SyncStatus) {
    val (background, foreground) = when (status) {
        SyncStatus.PENDING -> TbAmber.copy(alpha = 0.12f) to TbAmber
        SyncStatus.SYNCING -> TbGreenLight to TbGreenDark
        SyncStatus.FAILED, SyncStatus.CONFLICT -> TbError.copy(alpha = 0.10f) to TbError
        SyncStatus.SYNCED -> TbGreenLight to TbGreenDark
    }
    Surface(color = background, shape = RoundedCornerShape(999.dp)) {
        Text(
            text = status.displayName(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = foreground,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun MessageCard(message: String, isError: Boolean, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isError) TbError.copy(alpha = 0.08f) else TbGreenLight,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (isError) TbError.copy(alpha = 0.22f) else TbOutline),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f).padding(vertical = 6.dp),
                color = if (isError) TbError else TbGreenDark,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    }
}

@Composable
private fun EmptyState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = TbSurface),
        border = BorderStroke(1.dp, TbOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(TbGreenLight, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.CheckCircleOutline,
                    contentDescription = null,
                    tint = TbGreenDark,
                    modifier = Modifier.size(26.dp),
                )
            }
            Text(
                text = "Semua data sudah tersinkron",
                color = TbText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                text = "Antrean data offline akan tampil di halaman ini.",
                color = TbTextMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

private fun entityOrder(keys: Set<SyncEntityType>): List<SyncEntityType> {
    val preferred = listOf(
        SyncEntityType.TRANSACTION,
        SyncEntityType.CASH_SESSION,
        SyncEntityType.CASH_EXPENSE,
    )
    return preferred.filter { it in keys } + keys.filterNot { it in preferred }.sortedBy { it.name }
}

private fun SyncEntityType.sectionTitle(): String = when (this) {
    SyncEntityType.TRANSACTION -> "Transaksi"
    SyncEntityType.CASH_SESSION -> "Sesi kas"
    SyncEntityType.CASH_EXPENSE -> "Pengeluaran kas"
    SyncEntityType.UNSUPPORTED -> "Data lama tidak didukung"
}

private fun SyncEntityType.displayName(): String = sectionTitle()

private fun SyncStatus.displayName(): String = when (this) {
    SyncStatus.PENDING -> "Menunggu"
    SyncStatus.SYNCING -> "Diproses"
    SyncStatus.FAILED -> "Gagal"
    SyncStatus.CONFLICT -> "Konflik"
    SyncStatus.SYNCED -> "Berhasil"
}

private fun SyncErrorCategory.displayName(): String = when (this) {
    SyncErrorCategory.NETWORK -> "Jaringan"
    SyncErrorCategory.AUTH -> "Sesi masuk"
    SyncErrorCategory.VALIDATION -> "Validasi"
    SyncErrorCategory.CONFLICT -> "Konflik"
    SyncErrorCategory.UNKNOWN -> "Tidak diketahui"
}

private fun Long.formatDateTime(): String = Instant.ofEpochMilli(this)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
