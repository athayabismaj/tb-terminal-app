package com.tbterminal.app.ui.offline

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.TbTerminalApplication
import com.tbterminal.app.data.sync.BackendStatus
import com.tbterminal.app.data.sync.DataSourceStatus
import com.tbterminal.app.data.sync.OfflineConnectionStatus
import com.tbterminal.app.data.sync.OfflineStatus

/**
 * Host banner indikator koneksi dan sinkronisasi.
 *
 * Standar POS & Industrial UX (Notification by Exception):
 * Saat kondisi NORMAL (server tersambung, internet aktif, tidak ada pending/gagal sync),
 * banner besar ini OTOMATIS TERSEMBUNYI agar tidak memakan ruang layar kasir.
 * Banner HANYA MUNCUL ketika:
 * 1. Offline (tidak ada internet)
 * 2. Server tidak tersambung (unreachable)
 * 3. Sedang sinkronisasi data (syncing)
 * 4. Sinkronisasi gagal atau ada antrean pending
 * 5. Mode data lokal (LOCAL_ONLY)
 */
@Composable
fun OfflineStatusIndicatorHost(
    modifier: Modifier = Modifier,
    alwaysShow: Boolean = false
) {
    val app = LocalContext.current.applicationContext as? TbTerminalApplication ?: return
    val statusFlow = remember(app) { app.appContainer.offlineStatusRepository.status }
    val status by statusFlow.collectAsState(initial = OfflineStatus())

    val isNormalConnected = status.isInternetOnline &&
        status.backendStatus == BackendStatus.CONNECTED &&
        status.dataSourceStatus != DataSourceStatus.LOCAL_ONLY &&
        status.connectionStatus == OfflineConnectionStatus.ONLINE &&
        status.pendingSyncCount == 0 &&
        status.failedSyncCount == 0

    val shouldShow = alwaysShow || !isNormalConnected

    AnimatedVisibility(
        visible = shouldShow,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        OfflineStatusIndicator(
            status = status,
            modifier = modifier
        )
    }
}

/**
 * Micro indicator status koneksi (titik mini + label) yang hemat ruang,
 * cocok disematkan di top bar atau header kasir tanpa memakan tempat.
 */
@Composable
fun OfflineStatusMiniDot(
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as? TbTerminalApplication ?: return
    val statusFlow = remember(app) { app.appContainer.offlineStatusRepository.status }
    val status by statusFlow.collectAsState(initial = OfflineStatus())

    val isNormal = status.isInternetOnline &&
        status.backendStatus == BackendStatus.CONNECTED &&
        status.dataSourceStatus != DataSourceStatus.LOCAL_ONLY &&
        status.connectionStatus == OfflineConnectionStatus.ONLINE &&
        status.pendingSyncCount == 0 &&
        status.failedSyncCount == 0

    val isWarning = !status.isInternetOnline || status.backendStatus == BackendStatus.UNREACHABLE
    val isSyncing = status.connectionStatus == OfflineConnectionStatus.SYNCING || status.pendingSyncCount > 0

    val dotColor = when {
        isWarning -> Color(0xFFD97706) // Amber
        isSyncing -> Color(0xFF2563EB) // Blue
        isNormal -> Color(0xFF1B5E3C) // Brand Green
        else -> Color(0xFF64748B) // Slate
    }

    val labelText = when {
        !status.isInternetOnline -> "Offline"
        status.backendStatus == BackendStatus.UNREACHABLE -> "Server Terputus"
        isSyncing -> "Sinkronisasi (${status.pendingSyncCount})"
        isNormal -> ""
        else -> ""
    }

    if (!isNormal || labelText.isNotEmpty()) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            if (labelText.isNotEmpty()) {
                Text(
                    text = labelText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = dotColor
                )
            }
        }
    }
}

@Composable
private fun OfflineStatusIndicator(
    status: OfflineStatus,
    modifier: Modifier = Modifier
) {
    val style = offlineStatusStyle(status)
    val detailText = offlineStatusDetail(status)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, style.borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(style.iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = style.icon,
                    contentDescription = style.title,
                    tint = style.contentColor,
                    modifier = Modifier.size(17.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = style.title,
                    color = style.contentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = detailText,
                    color = Color(0xFF6B7378),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private data class OfflineStatusStyle(
    val title: String,
    val icon: ImageVector,
    val contentColor: Color,
    val iconBackground: Color,
    val borderColor: Color
)

private fun offlineStatusStyle(status: OfflineStatus): OfflineStatusStyle {
    return when {
        status.dataSourceStatus == DataSourceStatus.LOCAL_ONLY -> OfflineStatusStyle(
            title = "Mode lokal",
            icon = Icons.Outlined.CloudOff,
            contentColor = Color(0xFFB45309),
            iconBackground = Color(0xFFFFF7ED),
            borderColor = Color(0xFFFDE7C7)
        )

        status.connectionStatus == OfflineConnectionStatus.SYNCING -> OfflineStatusStyle(
            title = "Sinkronisasi",
            icon = Icons.Outlined.Sync,
            contentColor = Color(0xFF2563EB),
            iconBackground = Color(0xFFEFF6FF),
            borderColor = Color(0xFFD7E4FF)
        )

        status.connectionStatus == OfflineConnectionStatus.SYNC_FAILED -> OfflineStatusStyle(
            title = "Sync gagal",
            icon = Icons.Outlined.SyncProblem,
            contentColor = Color(0xFFDC2626),
            iconBackground = Color(0xFFFFF1F2),
            borderColor = Color(0xFFFECACA)
        )

        !status.isInternetOnline -> OfflineStatusStyle(
            title = "Offline",
            icon = Icons.Outlined.CloudOff,
            contentColor = Color(0xFFB45309),
            iconBackground = Color(0xFFFFF7ED),
            borderColor = Color(0xFFFDE7C7)
        )

        status.backendStatus == BackendStatus.CONNECTED -> OfflineStatusStyle(
            title = "Server tersambung",
            icon = Icons.Outlined.CloudDone,
            contentColor = Color(0xFF008C86),
            iconBackground = Color(0xFFE6F8F2),
            borderColor = Color(0xFFD7E7E3)
        )

        status.backendStatus == BackendStatus.UNREACHABLE -> OfflineStatusStyle(
            title = "Server tidak tersambung",
            icon = Icons.Outlined.CloudOff,
            contentColor = Color(0xFFB45309),
            iconBackground = Color(0xFFFFF7ED),
            borderColor = Color(0xFFFDE7C7)
        )

        else -> OfflineStatusStyle(
            title = "Jaringan aktif",
            icon = Icons.Outlined.CloudDone,
            contentColor = Color(0xFF475569),
            iconBackground = Color(0xFFF1F5F9),
            borderColor = Color(0xFFE2E8F0)
        )
    }
}

private fun offlineStatusDetail(status: OfflineStatus): String {
    return when {
        status.dataSourceStatus == DataSourceStatus.LOCAL_ONLY -> "Data lokal digunakan"
        status.connectionStatus == OfflineConnectionStatus.SYNCING -> "${status.pendingSyncCount} antrean diproses"
        status.connectionStatus == OfflineConnectionStatus.SYNC_FAILED -> {
            if (status.failedSyncCount > 0) {
                "${status.failedSyncCount} antrean gagal"
            } else {
                "Perlu dicoba ulang"
            }
        }

        !status.isInternetOnline -> "Data lokal digunakan jika tersedia"
        status.backendStatus == BackendStatus.CONNECTED -> {
            if (status.pendingSyncCount > 0) {
                "${status.pendingSyncCount} antrean belum sync"
            } else {
                "Server aktif"
            }
        }

        status.backendStatus == BackendStatus.UNREACHABLE -> "Data lokal digunakan jika tersedia"
        else -> "Memeriksa server"
    }
}
