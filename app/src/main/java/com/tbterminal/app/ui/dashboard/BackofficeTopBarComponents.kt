package com.tbterminal.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.sync.BackendStatus
import com.tbterminal.app.data.sync.OfflineStatus
import com.tbterminal.app.ui.theme.TbAmber
import com.tbterminal.app.ui.theme.TbAmberLight
import com.tbterminal.app.ui.theme.TbBackground
import com.tbterminal.app.ui.theme.TbError
import com.tbterminal.app.ui.theme.TbGreenDark
import com.tbterminal.app.ui.theme.TbGreenLight
import com.tbterminal.app.ui.theme.TbSurfaceMuted
import com.tbterminal.app.ui.theme.TbText
import com.tbterminal.app.ui.theme.TbterminalappTheme

/**
 * TopBar ramping untuk layar anak (child destination: form, detail, audit).
 * Minimalis, tinggi minimal 48dp, target sentuh tombol kembali 48dp,
 * judul 16sp semibold satu baris, tanpa border kaku atau subtitle panjang.
 */
@Composable
fun CompactBackofficeTopBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        color = TbBackground,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("topbar-back-button")
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = TbSurfaceMuted,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "Kembali",
                                    tint = TbText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TbText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                content = actions
            )
        }
    }
}

/**
 * Banner status sinkronisasi global di shell.
 * Hanya tampil di tab utama (root) saat ada kondisi abnormal (offline, antrean, gagal sinkron).
 * Maksimal tinggi 32dp, dapat diketuk untuk membuka detail status/sheet akun.
 */
@Composable
fun BackofficeGlobalStatusBanner(
    offlineStatus: OfflineStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOnline = offlineStatus.isInternetOnline && offlineStatus.backendStatus == BackendStatus.CONNECTED
    val hasPending = offlineStatus.pendingSyncCount > 0
    val hasFailed = offlineStatus.failedSyncCount > 0
    val hasOtherUsersPending = offlineStatus.otherUsersPendingSyncCount > 0
    val hasIssue = !isOnline || hasPending || hasFailed || hasOtherUsersPending

    if (!hasIssue) return

    val otherUsersNote = if (hasOtherUsersPending) {
        " · ${offlineStatus.otherUsersPendingSyncCount} data kasir lain menunggu login akun terkait"
    } else {
        ""
    }
    val (bgColor, contentColor, icon, message) = when {
        !offlineStatus.isInternetOnline -> {
            BannerState(
                bgColor = TbAmberLight,
                contentColor = TbAmber,
                icon = Icons.Outlined.CloudOff,
                message = "Mode Offline — Periksa koneksi internet$otherUsersNote"
            )
        }
        offlineStatus.backendStatus != BackendStatus.CONNECTED -> {
            BannerState(
                bgColor = TbAmberLight,
                contentColor = TbAmber,
                icon = Icons.Outlined.CloudOff,
                message = "Server Terputus — Data tersimpan aman di lokal$otherUsersNote"
            )
        }
        hasFailed -> {
            BannerState(
                bgColor = TbError.copy(alpha = 0.12f),
                contentColor = TbError,
                icon = Icons.Outlined.SyncProblem,
                message = "${offlineStatus.failedSyncCount} transaksi gagal sinkron — Ketuk untuk periksa$otherUsersNote"
            )
        }
        hasPending -> {
            BannerState(
                bgColor = TbGreenLight,
                contentColor = TbGreenDark,
                icon = Icons.Outlined.Sync,
                message = "${offlineStatus.pendingSyncCount} data dalam antrean sinkronisasi$otherUsersNote"
            )
        }
        else -> {
            BannerState(
                bgColor = TbAmberLight,
                contentColor = TbAmber,
                icon = Icons.Outlined.Sync,
                message = "${offlineStatus.otherUsersPendingSyncCount} data kasir lain menunggu login akun terkait"
            )
        }
    }

    Surface(
        onClick = onClick,
        color = bgColor,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 32.dp)
            .testTag("backoffice-global-status-banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = message,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private data class BannerState(
    val bgColor: androidx.compose.ui.graphics.Color,
    val contentColor: androidx.compose.ui.graphics.Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val message: String
)

/**
 * Avatar ringkas untuk baris atas Dashboard (Admin & Owner).
 * Memiliki target sentuh minimal 48dp, lingkaran visual 36dp berlatar tonal,
 * menampilkan inisial pengguna, dan membuka sheet Akun saat diketuk.
 */
@Composable
fun BackofficeAvatarButton(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "backoffice-avatar-btn"
) {
    val initial = name.trim().take(1).uppercase().ifEmpty { "U" }
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .testTag(testTag)
    ) {
        Surface(
            shape = CircleShape,
            color = TbGreenLight,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initial,
                    color = TbGreenDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// PREVIEWS
// ═════════════════════════════════════════════════════════════════════════════

@Preview(name = "BackofficeAvatarButton", showBackground = true)
@Composable
private fun BackofficeAvatarButtonPreview() {
    TbterminalappTheme {
        BackofficeAvatarButton(
            name = "Admin",
            onClick = {}
        )
    }
}

@Preview(name = "CompactBackofficeTopBar - Child Screen Form", showBackground = true)
@Composable
private fun CompactBackofficeTopBarPreview() {
    TbterminalappTheme {
        CompactBackofficeTopBar(
            title = "Tambah Produk Baru",
            onBack = {},
            actions = {
                IconButton(
                    onClick = {},
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Save,
                        contentDescription = "Simpan",
                        tint = TbGreenDark
                    )
                }
            }
        )
    }
}

@Preview(name = "Global Status Banner - Offline", showBackground = true)
@Composable
private fun BackofficeGlobalStatusBannerOfflinePreview() {
    TbterminalappTheme {
        BackofficeGlobalStatusBanner(
            offlineStatus = OfflineStatus(
                isInternetOnline = false,
                backendStatus = BackendStatus.UNREACHABLE
            ),
            onClick = {}
        )
    }
}

@Preview(name = "Global Status Banner - Pending Queue", showBackground = true)
@Composable
private fun BackofficeGlobalStatusBannerPendingPreview() {
    TbterminalappTheme {
        BackofficeGlobalStatusBanner(
            offlineStatus = OfflineStatus(
                isInternetOnline = true,
                backendStatus = BackendStatus.CONNECTED,
                pendingSyncCount = 5
            ),
            onClick = {}
        )
    }
}
