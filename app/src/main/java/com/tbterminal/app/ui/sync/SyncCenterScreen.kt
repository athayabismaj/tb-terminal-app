package com.tbterminal.app.ui.sync

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.repository.SyncQueueUiModel
import com.tbterminal.app.data.sync.SyncErrorCategory
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.components.TopFloatingCapsule
import com.tbterminal.app.ui.components.SkeletonList
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val BrandSage = Color(0xFF256B57)
private val BrandSageDark = Color(0xFF1D5444)
private val BrandSageLight = Color(0xFFE8F2EE)
private val BackgroundSurface = Color(0xFFF8FAFC)
private val Slate900 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate600 = Color(0xFF475569)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate100 = Color(0xFFF1F5F9)

@Composable
fun SyncCenterScreen(
    uiState: SyncCenterUiState,
    onRetryItem: (Long) -> Unit,
    onMarkConflictReviewed: (Long) -> Unit,
    onRetryAllPending: () -> Unit,
    onRetryAllFailed: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
    showStatusSheet: Boolean = false,
    onOpenStatusSheet: () -> Unit = {},
    onDismissStatusSheet: () -> Unit = {},
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(BackgroundSurface)) {
        val compact = maxWidth < 700.dp
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (compact) 16.dp else 32.dp,
                    end = if (compact) 16.dp else 32.dp,
                    top = if (compact) 12.dp else 24.dp,
                    bottom = if (compact) 16.dp else 24.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (uiState.isInitialLoading) {
                item { SyncOverviewSkeleton(compact) }
                item { SkeletonList(modifier = Modifier.fillMaxWidth(), itemCount = 3) }
            } else {
                // Section Header on the Main Screen with FactCheck icon (desktop only; mobile has header action)
                if (!compact) {
                    item {
                        SyncSectionHeader(
                            onOpenStatusSheet = onOpenStatusSheet,
                        )
                    }
                }

                item {
                    SyncStatusOverviewCard(
                        uiState = uiState,
                        onRetryAllPending = onRetryAllPending,
                        onRetryAllFailed = onRetryAllFailed,
                    )
                }

                item {
                    OfflineDataQueueCard(
                        uiState = uiState,
                        compact = compact,
                        onRetryItem = onRetryItem,
                        onMarkConflictReviewed = onMarkConflictReviewed,
                    )
                }
            }
        }

        if (showStatusSheet) {
            SyncStatusBottomSheet(
                uiState = uiState,
                onDismiss = onDismissStatusSheet,
                onRetryAllPending = onRetryAllPending,
                onRetryAllFailed = onRetryAllFailed,
            )
        }

        TopFloatingCapsule(
            message = uiState.message,
            error = uiState.errorMessage,
            onDismiss = onDismissMessage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                .zIndex(99f),
        )
    }
}

@Composable
private fun SyncSectionHeader(
    onOpenStatusSheet: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Status sinkronisasi",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Slate900,
        )

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                .background(Color.White)
                .clickable { onOpenStatusSheet() }
                .testTag("sync-open-status-sheet"),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.FactCheck,
                contentDescription = "Rincian status sinkronisasi",
                tint = Color(0xFF334155),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun SyncOverviewSkeleton(compact: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("sync-initial-skeleton"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
    ) {
        Column(Modifier.padding(if (compact) 16.dp else 20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonBox(Modifier.width(140.dp).height(18.dp))
                SkeletonBox(Modifier.width(70.dp).height(24.dp))
            }
            Spacer(Modifier.height(8.dp))
            SkeletonBox(Modifier.width(130.dp).height(12.dp))
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SkeletonBox(Modifier.weight(1f).height(40.dp))
                SkeletonBox(Modifier.weight(1f).height(40.dp))
            }
        }
    }
}

@Composable
private fun SyncStatusOverviewCard(
    uiState: SyncCenterUiState,
    onRetryAllPending: () -> Unit,
    onRetryAllFailed: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Sinkronisasi perangkat",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate900,
                        )
                        Surface(
                            color = Slate100,
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                text = "Aktif",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate700,
                            )
                        }
                    }
                    Text(
                        text = "Terakhir sinkron: Baru saja",
                        fontSize = 12.sp,
                        color = Slate500,
                    )
                }

                Text(
                    text = "${uiState.queueItems.size} antrean",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate600,
                )
            }

            // Buttons Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val canRetryFailed = !uiState.isRefreshing && !uiState.isRetrying && uiState.canRetry && uiState.totalFailed > 0
                val canSync = !uiState.isRefreshing && !uiState.isRetrying && uiState.canRetry && (uiState.totalPending > 0 || uiState.queueItems.isNotEmpty())

                OutlinedButton(
                    onClick = onRetryAllFailed,
                    enabled = canRetryFailed,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("sync-retry-failed"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (canRetryFailed) Slate200 else Slate200.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Slate700,
                        containerColor = Color.White,
                        disabledContentColor = Slate400,
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(
                        text = "Coba gagal",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                }

                Button(
                    onClick = onRetryAllPending,
                    enabled = canSync,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("sync-all-pending"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandSage,
                        disabledContainerColor = Slate200,
                        disabledContentColor = Slate400,
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Sinkronkan",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun OfflineDataQueueCard(
    uiState: SyncCenterUiState,
    compact: Boolean,
    onRetryItem: (Long) -> Unit,
    onMarkConflictReviewed: (Long) -> Unit,
) {
    val conflictItems = uiState.queueItems.filter { it.syncStatus == SyncStatus.CONFLICT }
    val groupedNonConflictItems = uiState.groupedItems
        .mapValues { (_, items) -> items.filter { it.syncStatus != SyncStatus.CONFLICT } }
        .filterValues { it.isNotEmpty() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Antrean data offline",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate900,
                )
                Surface(
                    color = Slate100,
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        text = "${uiState.queueItems.size} data",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate600,
                    )
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            if (uiState.queueItems.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Slate100, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = BrandSage,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(
                        text = "Semua data sudah tersinkron",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate800,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "Antrean data offline akan tampil di halaman ini saat transaksi diproses tanpa internet.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Slate500,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 280.dp),
                        lineHeight = 16.sp,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (conflictItems.isNotEmpty()) {
                        QueueSectionTitle("Perlu ditinjau", conflictItems.size)
                        conflictItems.forEach { item ->
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
                            QueueSectionTitle(entityType.sectionTitle(), entries.size)
                            entries.forEach { item ->
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncStatusBottomSheet(
    uiState: SyncCenterUiState,
    onDismiss: () -> Unit,
    onRetryAllPending: () -> Unit,
    onRetryAllFailed: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .background(Color(0xFFCBD5E1), RoundedCornerShape(999.dp)),
                )
            }
        },
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .testTag("sync-status-sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "Rincian Sinkronisasi",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        lineHeight = 24.sp,
                    )
                    Text(
                        text = "Pantau status dan antrean data perangkat",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Slate500,
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Slate100, CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup",
                        tint = Slate500,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            // Minimalist Clean Key-Value Rows (Polos, no AI slop)
            Column(modifier = Modifier.fillMaxWidth()) {
                SyncStatusMetricRow(
                    label = "Menunggu",
                    value = uiState.totalPending,
                    dotColor = Color(0xFFD97706),
                )
                HorizontalDivider(color = Slate100, thickness = 1.dp)

                SyncStatusMetricRow(
                    label = "Diproses",
                    value = uiState.totalSyncing,
                    dotColor = Color(0xFF0284C7),
                )
                HorizontalDivider(color = Slate100, thickness = 1.dp)

                SyncStatusMetricRow(
                    label = "Gagal",
                    value = uiState.totalFailed,
                    dotColor = Color(0xFFE11D48),
                )
                HorizontalDivider(color = Slate100, thickness = 1.dp)

                SyncStatusMetricRow(
                    label = "Konflik",
                    value = uiState.totalConflict,
                    dotColor = Color(0xFF9333EA),
                )
                HorizontalDivider(color = Slate100, thickness = 1.dp)

                SyncStatusMetricRow(
                    label = "Berhasil hari ini",
                    value = uiState.totalSyncedToday,
                    dotColor = BrandSage,
                )
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val canRetryFailed = !uiState.isRefreshing && !uiState.isRetrying && uiState.canRetry && uiState.totalFailed > 0
                val canSync = !uiState.isRefreshing && !uiState.isRetrying && uiState.canRetry && (uiState.totalPending > 0 || uiState.queueItems.isNotEmpty())

                OutlinedButton(
                    onClick = onRetryAllFailed,
                    enabled = canRetryFailed,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (canRetryFailed) Slate200 else Slate200.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Slate700,
                        containerColor = Color.White,
                        disabledContentColor = Slate400,
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(
                        text = "Coba gagal",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                }

                Button(
                    onClick = onRetryAllPending,
                    enabled = canSync,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandSage,
                        disabledContainerColor = Slate200,
                        disabledContentColor = Slate400,
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Sinkronkan",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncStatusMetricRow(
    label: String,
    value: Int,
    dotColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dotColor, CircleShape)
            )
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Slate600,
            )
        }
        Text(
            text = value.toString(),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Slate900,
        )
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
            color = Slate900,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Surface(color = Slate100, shape = RoundedCornerShape(6.dp)) {
            Text(
                text = count.toString(),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                color = Slate600,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
    ) {
        Column(Modifier.padding(14.dp)) {
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
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(
                text = item.entityType.displayName(),
                color = Slate900,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Data lokal #${item.entityId}",
                color = Slate500,
                fontSize = 11.sp,
            )
        }
        StatusBadge(item.syncStatus)
    }
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = item.updatedAt.formatDateTime(),
            modifier = Modifier.weight(1f),
            color = Slate500,
            fontSize = 11.sp,
        )
        Text(
            text = "Percobaan ${item.retryCount}",
            color = Slate500,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun ExpandedQueueHeader(item: SyncQueueUiModel) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        QueueIcon()
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                text = item.entityType.displayName(),
                color = Slate900,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Data lokal #${item.entityId}",
                color = Slate500,
                fontSize = 12.sp,
            )
        }
        StatusBadge(item.syncStatus)
        Column(Modifier.padding(start = 20.dp), horizontalAlignment = Alignment.End) {
            Text(item.updatedAt.formatDateTime(), color = Slate500, fontSize = 11.sp)
            Text("Percobaan ${item.retryCount}", color = Slate500, fontSize = 11.sp)
        }
    }
}

@Composable
private fun QueueIcon() {
    Box(
        modifier = Modifier.size(36.dp).background(Slate100, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.Sync, contentDescription = null, tint = Slate700, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ErrorDisclosure(expanded: Boolean, isConflict: Boolean, onClick: () -> Unit) {
    val errorColor = Color(0xFFEF4444)
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = if (isConflict) errorColor.copy(alpha = 0.06f) else Slate100,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = if (isConflict) errorColor else Slate500,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = if (isConflict) "Periksa konflik data" else "Lihat kendala sinkronisasi",
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                color = if (isConflict) errorColor else Slate700,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Tutup detail" else "Buka detail",
                    tint = Slate500,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ErrorDetails(item: SyncQueueUiModel) {
    val errorColor = Color(0xFFEF4444)
    Spacer(Modifier.height(8.dp))
    HorizontalDivider(color = Slate200)
    Column(Modifier.padding(top = 8.dp)) {
        Text(
            text = item.lastError.orEmpty(),
            color = Slate800,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
        if (item.syncStatus == SyncStatus.CONFLICT) {
            Text(
                text = "Jenis kendala: ${item.errorCategory.displayName()}",
                color = errorColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                text = item.recommendation,
                color = Slate500,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp),
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
        val actionModifier = if (compact) Modifier.weight(1f) else Modifier.width(160.dp)
        OutlinedButton(
            onClick = onRetry,
            enabled = enabled,
            modifier = actionModifier.height(40.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Slate200),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700),
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Text("Coba lagi", fontSize = 12.sp, maxLines = 1)
        }
        if (item.syncStatus == SyncStatus.CONFLICT) {
            TextButton(
                onClick = onMarkReviewed,
                enabled = enabled,
                modifier = actionModifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Text("Tandai ditinjau", fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun StatusBadge(status: SyncStatus) {
    val (background, foreground) = when (status) {
        SyncStatus.PENDING -> Color(0xFFFEF3C7) to Color(0xFFD97706)
        SyncStatus.SYNCING -> Slate100 to Slate700
        SyncStatus.FAILED, SyncStatus.CONFLICT -> Color(0xFFFFE4E6) to Color(0xFFE11D48)
        SyncStatus.SYNCED -> Color(0xFFECFDF5) to Color(0xFF047857)
    }
    Surface(color = background, shape = RoundedCornerShape(6.dp)) {
        Text(
            text = status.displayName(),
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            color = foreground,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun MessageCard(message: String, isError: Boolean, onDismiss: () -> Unit) {
    val errorColor = Color(0xFFEF4444)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isError) errorColor.copy(alpha = 0.06f) else Slate100,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isError) errorColor.copy(alpha = 0.2f) else Slate200),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                color = if (isError) errorColor else Slate800,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
            TextButton(onClick = onDismiss) { Text("Tutup", fontSize = 12.sp) }
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
