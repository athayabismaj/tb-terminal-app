package com.tbterminal.app.ui.offline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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

@Composable
fun OfflineStatusIndicatorHost(
    modifier: Modifier = Modifier
) {
    val app = LocalContext.current.applicationContext as? TbTerminalApplication ?: return
    val statusFlow = remember(app) { app.appContainer.offlineStatusRepository.status }
    val status by statusFlow.collectAsState(initial = OfflineStatus())

    OfflineStatusIndicator(
        status = status,
        modifier = modifier
    )
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
            androidx.compose.foundation.layout.Box(
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
