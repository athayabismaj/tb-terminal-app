package com.tbterminal.app.ui.cashier.sync.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.repository.CashierClockWarning
import com.tbterminal.app.data.repository.CashierSyncItemKind
import com.tbterminal.app.data.repository.CashierSyncItemStatus
import com.tbterminal.app.data.repository.CashierSyncItemUi
import com.tbterminal.app.ui.cashier.sync.CashierSyncUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BrandGreen = Color(0xFF1B5E3C)
private val Slate900 = Color(0xFF0F172A)
private val Slate700 = Color(0xFF334155)
private val Slate600 = Color(0xFF475569)
private val Slate500 = Color(0xFF64748B)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate50 = Color(0xFFF8FAFC)

@Composable
internal fun CashierClockWarningBanner(
    warning: CashierClockWarning,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cashier-sync-clock-warning"),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFFBEB),
        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                tint = Color(0xFFB45309),
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(16.dp),
            )
            Text(
                text = warning.message,
                fontSize = 12.sp,
                color = Color(0xFF92400E),
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
internal fun CashierSyncStatusCard(
    uiState: CashierSyncUiState,
    onSyncNowClick: () -> Unit,
    onRefreshServerStatus: () -> Unit,
    onDismissFeedback: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Slate200),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (uiState.isServerOnline) Color(0xFFECFDF5) else Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (uiState.isServerOnline) Icons.Outlined.CloudDone else Icons.Outlined.CloudOff,
                            contentDescription = null,
                            tint = if (uiState.isServerOnline) Color(0xFF047857) else Color(0xFFB45309),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Status Server",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                            )
                            ServerStatusBadge(isOnline = uiState.isServerOnline)
                        }
                        Text(
                            text = formatLastSyncText(
                                timestampMillis = uiState.lastSuccessfulSyncAtMillis,
                                blockedCount = uiState.blockedCount,
                            ),
                            fontSize = 12.sp,
                            color = Slate500,
                        )
                    }
                }

                IconButton(
                    onClick = onRefreshServerStatus,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("cashier-sync-refresh-health"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Cek status server",
                        tint = Slate600,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            if (!uiState.isServerOnline) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = if (!uiState.isInternetOnline) {
                            "Perangkat tidak terhubung internet. Data tetap aman tersimpan di perangkat."
                        } else {
                            "Server belum terjangkau saat ini. Data akan dikirim otomatis begitu server kembali online."
                        },
                        fontSize = 12.sp,
                        color = Color(0xFF92400E),
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }

            Button(
                onClick = onSyncNowClick,
                enabled = !uiState.isSyncing,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandGreen,
                    contentColor = Color.White,
                    disabledContainerColor = Slate200,
                    disabledContentColor = Slate600,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("cashier-sync-now-button"),
            ) {
                if (uiState.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Slate600,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Sedang sinkron…",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Sinkronkan sekarang",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            val feedback = uiState.feedbackMessage
            if (!feedback.isNullOrBlank()) {
                val feedbackBg = when {
                    uiState.isFeedbackError -> Color(0xFFFFF1F2)
                    uiState.isSyncing -> Color(0xFFEFF6FF)
                    else -> Color(0xFFECFDF5)
                }
                val feedbackBorder = when {
                    uiState.isFeedbackError -> Color(0xFFFECDD3)
                    uiState.isSyncing -> Color(0xFFBFDBFE)
                    else -> Color(0xFFA7F3D0)
                }
                val feedbackFg = when {
                    uiState.isFeedbackError -> Color(0xFFBE123C)
                    uiState.isSyncing -> Color(0xFF1D4ED8)
                    else -> Color(0xFF047857)
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = feedbackBg,
                    border = BorderStroke(1.dp, feedbackBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cashier-sync-feedback"),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = feedback,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = feedbackFg,
                            lineHeight = 16.sp,
                            modifier = Modifier.weight(1f),
                        )
                        if (!uiState.isSyncing) {
                            IconButton(
                                onClick = onDismissFeedback,
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Tutup pesan",
                                    tint = feedbackFg,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.otherUsersPendingSyncCount > 0) {
                Text(
                    text = "${uiState.otherUsersPendingSyncCount} data milik kasir lain di perangkat ini menunggu login akun terkait.",
                    fontSize = 11.sp,
                    color = Slate500,
                    lineHeight = 15.sp,
                )
            }
        }
    }
}

@Composable
private fun ServerStatusBadge(isOnline: Boolean) {
    val bg = if (isOnline) Color(0xFFECFDF5) else Color(0xFFFEF3C7)
    val border = if (isOnline) Color(0xFFA7F3D0) else Color(0xFFFDE68A)
    val fg = if (isOnline) Color(0xFF047857) else Color(0xFFB45309)
    val dot = if (isOnline) Color(0xFF10B981) else Color(0xFFD97706)
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dot),
            )
            Text(
                text = if (isOnline) "Online" else "Offline",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = fg,
            )
        }
    }
}

@Composable
internal fun CashierSyncSummarySection(
    pendingCount: Int,
    sendingCount: Int,
    failedCount: Int,
    needsCheckCount: Int,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val cards = listOf(
        SummaryMetric("Menunggu", pendingCount, Color(0xFFD97706), Color(0xFFFFFBEB)),
        SummaryMetric("Sedang dikirim", sendingCount, Color(0xFF2563EB), Color(0xFFEFF6FF)),
        SummaryMetric("Gagal", failedCount, Color(0xFFDC2626), Color(0xFFFFF1F2)),
        SummaryMetric("Perlu dicek", needsCheckCount, Color(0xFF9333EA), Color(0xFFFAF5FF)),
    )

    if (compact) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            cards.chunked(2).forEach { rowCards ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowCards.forEach { metric ->
                        SummaryMetricCard(metric = metric, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            cards.forEach { metric ->
                SummaryMetricCard(metric = metric, modifier = Modifier.weight(1f))
            }
        }
    }
}

private data class SummaryMetric(
    val label: String,
    val count: Int,
    val accentColor: Color,
    val badgeBg: Color,
)

@Composable
private fun SummaryMetricCard(
    metric: SummaryMetric,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Slate200),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(metric.accentColor),
                )
                Text(
                    text = metric.label,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = metric.count.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900,
            )
        }
    }
}

@Composable
internal fun CashierSyncItemCard(
    item: CashierSyncItemUi,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Slate200),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Slate100),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = item.kind.icon(),
                        contentDescription = null,
                        tint = Slate700,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = item.title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = item.summary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate600,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                CashierSyncStatusBadge(status = item.status)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = Slate500,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = formatTimestamp(item.createdAtMillis),
                    fontSize = 11.sp,
                    color = Slate500,
                )
            }

            if (item.blockingCount > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                ) {
                    Text(
                        text = "Menahan ${item.blockingCount} data lain",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFB45309),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            if (!item.blockedByLabel.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate100,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Tertahan oleh: ${item.blockedByLabel}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate700,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            if (item.isConflict || !item.failureReason.isNullOrBlank()) {
                val isAttention = item.status == CashierSyncItemStatus.FAILED ||
                    item.status == CashierSyncItemStatus.NEEDS_CHECK
                val boxBg = when (item.status) {
                    CashierSyncItemStatus.FAILED -> Color(0xFFFFF1F2)
                    CashierSyncItemStatus.NEEDS_CHECK -> Color(0xFFFAF5FF)
                    else -> Slate50
                }
                val textColor = when (item.status) {
                    CashierSyncItemStatus.FAILED -> Color(0xFFB91C1C)
                    CashierSyncItemStatus.NEEDS_CHECK -> Color(0xFF7E22CE)
                    else -> Slate600
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = boxBg,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = if (isAttention) textColor else Slate500,
                            modifier = Modifier
                                .padding(top = 1.dp)
                                .size(15.dp),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (item.isConflict) {
                                Text(
                                    text = "Perlu dicek — hubungi admin/owner",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor,
                                    lineHeight = 16.sp,
                                )
                            }
                            if (!item.failureReason.isNullOrBlank()) {
                                Text(
                                    text = item.failureReason,
                                    fontSize = 11.5.sp,
                                    color = textColor,
                                    lineHeight = 16.sp,
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
private fun CashierSyncStatusBadge(status: CashierSyncItemStatus) {
    val (label, bg, fg) = when (status) {
        CashierSyncItemStatus.PENDING -> Triple("Menunggu", Color(0xFFFEF3C7), Color(0xFFB45309))
        CashierSyncItemStatus.SENDING -> Triple("Sedang dikirim", Color(0xFFEFF6FF), Color(0xFF1D4ED8))
        CashierSyncItemStatus.FAILED -> Triple("Gagal", Color(0xFFFFE4E6), Color(0xFFBE123C))
        CashierSyncItemStatus.NEEDS_CHECK -> Triple("Perlu dicek", Color(0xFFF3E8FF), Color(0xFF7E22CE))
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
internal fun CashierSyncEmptyCard(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Slate200),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFECFDF5)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = BrandGreen,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = "Semua data sudah tersinkron",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Slate900,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Transaksi offline, buka/tutup shift, dan mutasi kas yang menunggu pengiriman akan muncul di sini.",
                fontSize = 12.sp,
                color = Slate500,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp,
                modifier = Modifier.widthIn(max = 300.dp),
            )
        }
    }
}

private fun CashierSyncItemKind.icon(): ImageVector = when (this) {
    CashierSyncItemKind.TRANSACTION,
    CashierSyncItemKind.ONLINE_CHECKOUT -> Icons.Outlined.PointOfSale
    CashierSyncItemKind.OPEN_CASH_SESSION,
    CashierSyncItemKind.CLOSE_CASH_SESSION -> Icons.Outlined.LockOpen
    CashierSyncItemKind.CASH_EXPENSE,
    CashierSyncItemKind.CASH_IN -> Icons.Outlined.Payments
    CashierSyncItemKind.PAY_DEBT,
    CashierSyncItemKind.RECEIVABLE_PAYMENT -> Icons.Outlined.AccountBalanceWallet
    CashierSyncItemKind.VOID_TRANSACTION,
    CashierSyncItemKind.REFUND_TRANSACTION -> Icons.AutoMirrored.Outlined.ReceiptLong
}

private fun formatLastSyncText(
    timestampMillis: Long?,
    blockedCount: Int = 0,
): String {
    val base = if (timestampMillis == null || timestampMillis <= 0L) {
        "Belum pernah sinkron"
    } else {
        "Sinkron terakhir: ${formatTimestamp(timestampMillis)}"
    }
    return if (blockedCount > 0) {
        "$base · $blockedCount data tertahan"
    } else {
        base
    }
}

private fun formatTimestamp(timestampMillis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))
    return Instant.ofEpochMilli(timestampMillis)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}
