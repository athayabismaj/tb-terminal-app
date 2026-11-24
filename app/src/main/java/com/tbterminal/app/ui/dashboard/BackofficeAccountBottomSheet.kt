package com.tbterminal.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.local.database.DevicePreferences
import com.tbterminal.app.data.sync.BackendStatus
import com.tbterminal.app.data.sync.OfflineStatus
import com.tbterminal.app.ui.theme.TbAmber
import com.tbterminal.app.ui.theme.TbAmberLight
import com.tbterminal.app.ui.theme.TbBackground
import com.tbterminal.app.ui.theme.TbError
import com.tbterminal.app.ui.theme.TbGreen
import com.tbterminal.app.ui.theme.TbGreenDark
import com.tbterminal.app.ui.theme.TbGreenLight
import com.tbterminal.app.ui.theme.TbSurface
import com.tbterminal.app.ui.theme.TbSurfaceMuted
import com.tbterminal.app.ui.theme.TbText
import com.tbterminal.app.ui.theme.TbTextMuted
import com.tbterminal.app.ui.theme.TbterminalappTheme

val LocalBackofficeAccountSheet = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * Bottom sheet akun terpadu untuk peran Admin dan Owner.
 * Menampilkan identitas, status sinkronisasi, pengaturan perangkat, menu profil,
 * dan tombol keluar dengan konfirmasi data belum tersinkron.
 * Tidak membocorkan fitur spesifik kasir (mis. Ganti Kasir).
 */
@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackofficeAccountBottomSheet(
    userName: String,
    role: String,
    offlineStatus: OfflineStatus,
    devicePreferences: DevicePreferences,
    onDismiss: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSyncCenterClick: (() -> Unit)? = null,
    onReportsClick: (() -> Unit)? = null,
    onStoreSettingsClick: (() -> Unit)? = null,
    onLockScreen: (() -> Unit)? = null,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    val isOwner = isOwnerPersona(role)

    val displayName = remember(userName, isOwner) {
        val trimmed = userName.trim()
        when {
            trimmed.isEmpty() -> if (isOwner) "Pemilik Toko" else "Admin"
            isOwner && (trimmed.equals("owner", ignoreCase = true) || trimmed.equals("pemilik", ignoreCase = true)) -> "Pemilik Toko"
            !isOwner && trimmed.equals("admin", ignoreCase = true) -> "Admin"
            else -> trimmed
        }
    }

    val userInitial = remember(displayName, isOwner) {
        displayName.firstOrNull()?.uppercase() ?: if (isOwner) "P" else "A"
    }

    val roleBadge = if (isOwner) "PEMILIK TOKO" else "ADMIN"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = TbSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TbSurfaceMuted)
            )
        },
        modifier = modifier.testTag("backoffice-account-bottom-sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 1. HEADER IDENTITAS AKUN ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(TbGreenLight)
                            .border(1.5.dp, TbGreen.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userInitial,
                            color = TbGreenDark,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(
                            text = displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TbText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = roleBadge,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TbGreenDark
                            )
                            Text(
                                text = "· Terminal Barokah Jaya",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TbTextMuted
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(TbSurfaceMuted)
                        .testTag("account-sheet-close-btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = TbTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // ── 2. STATUS SINKRONISASI ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isOnline = offlineStatus.isInternetOnline && offlineStatus.backendStatus == BackendStatus.CONNECTED
                val hasPending = offlineStatus.pendingSyncCount > 0

                // Card Konektivitas Cloud
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isOnline) TbGreenLight else TbAmberLight,
                    border = BorderStroke(1.dp, if (isOnline) TbGreen.copy(alpha = 0.2f) else TbAmber.copy(alpha = 0.2f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) TbGreen else TbAmber)
                        )
                        Column {
                            Text(
                                text = "KONEKSI CLOUD",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TbTextMuted,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (isOnline) "Terhubung" else "Mode Offline",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline) TbGreenDark else TbAmber
                            )
                        }
                    }
                }

                // Card Antrean Sinkronisasi
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (!hasPending) TbGreenLight else TbAmberLight,
                    border = BorderStroke(1.dp, if (!hasPending) TbGreen.copy(alpha = 0.2f) else TbAmber.copy(alpha = 0.2f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (!hasPending) TbGreen else TbAmber)
                        )
                        Column {
                            Text(
                                text = "DATA SINKRONISASI",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TbTextMuted,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (hasPending) "${offlineStatus.pendingSyncCount} Antrean" else "Semua Tersimpan",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!hasPending) TbGreenDark else TbAmber
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = TbSurfaceMuted, thickness = 1.dp)

            // ── 3. MENU AKUN & KEAMANAN ──
            Text(
                text = "AKUN & KEAMANAN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TbTextMuted,
                letterSpacing = 0.6.sp
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BackofficeAccountActionRow(
                    icon = Icons.Outlined.Person,
                    title = "Profil Saya",
                    subtitle = "Informasi akun, ganti PIN, dan kata sandi",
                    testTag = "account-row-profile",
                    onClick = {
                        onDismiss()
                        onProfileClick()
                    }
                )
                if (onLockScreen != null) {
                    BackofficeAccountActionRow(
                        icon = Icons.Outlined.Lock,
                        title = "Kunci Layar (PIN)",
                        subtitle = "Kunci sesi aplikasi saat meninggalkan layar",
                        testTag = "account-row-lock",
                        onClick = {
                            onDismiss()
                            onLockScreen()
                        }
                    )
                }
            }

            HorizontalDivider(color = TbSurfaceMuted, thickness = 1.dp)

            // ── 4. MENU PERANGKAT & NAVIGASI SPESIFIK PERAN ──
            Text(
                text = if (isOwner) "PENGATURAN & BISNIS" else "PERANGKAT & OPERASIONAL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TbTextMuted,
                letterSpacing = 0.6.sp
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val isPrinterConfigured = devicePreferences.printerBluetoothAddress.isNotBlank()
                BackofficeAccountActionRow(
                    icon = Icons.Outlined.Settings,
                    title = "Perangkat & Printer (${devicePreferences.paperSize})",
                    subtitle = if (isPrinterConfigured) {
                        "Printer: ${devicePreferences.printerBluetoothName.ifBlank { "Bluetooth" }} · nota & suara"
                    } else {
                        "Atur printer, format nota, suara, & tema"
                    },
                    badge = if (isPrinterConfigured) "Siap" else "Atur",
                    badgeColor = if (isPrinterConfigured) TbGreenDark else TbAmber,
                    testTag = "account-row-settings",
                    onClick = {
                        onDismiss()
                        onSettingsClick()
                    }
                )

                if (isOwner) {
                    if (onStoreSettingsClick != null) {
                        BackofficeAccountActionRow(
                            icon = Icons.Outlined.Storefront,
                            title = "Identitas & Pengaturan Toko",
                            subtitle = "Nama toko, alamat, dan nomor kontak",
                            testTag = "account-row-store-settings",
                            onClick = {
                                onDismiss()
                                onStoreSettingsClick()
                            }
                        )
                    }
                } else {
                    if (onSyncCenterClick != null) {
                        BackofficeAccountActionRow(
                            icon = Icons.Outlined.Sync,
                            title = "Pusat Sinkronisasi",
                            subtitle = "Monitoring antrean dan sinkronisasi manual",
                            badge = if (offlineStatus.pendingSyncCount > 0) "${offlineStatus.pendingSyncCount}" else null,
                            badgeColor = TbAmber,
                            testTag = "account-row-sync",
                            onClick = {
                                onDismiss()
                                onSyncCenterClick()
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = TbSurfaceMuted, thickness = 1.dp)

            // ── 5. INFORMASI APLIKASI ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TB Terminal v1.0.0 (Build 1)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TbTextMuted
                )
                Text(
                    text = "Terminal Barokah Jaya",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TbText
                )
            }

            // ── 6. TOMBOL KELUAR (LOGOUT) ──
            OutlinedButton(
                onClick = { showLogoutConfirmDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("account-sheet-logout-btn"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, TbError.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TbError
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Keluar dari Akun",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // ── DIALOG KONFIRMASI KELUAR ──
    if (showLogoutConfirmDialog) {
        val hasPending = offlineStatus.pendingSyncCount > 0

        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = {
                Text(
                    text = "Keluar dari Akun?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TbText
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (hasPending) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TbAmberLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Warning,
                                    contentDescription = null,
                                    tint = TbAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "PERINGATAN: Ada ${offlineStatus.pendingSyncCount} data belum tersinkron ke server. Data tetap tersimpan aman di perangkat ini, namun server belum menerima data terbaru.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TbAmber
                                )
                            }
                        }
                    }
                    Text(
                        text = "Apakah Anda yakin ingin mengakhiri sesi $displayName?",
                        fontSize = 13.sp,
                        color = TbTextMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirmDialog = false
                        onDismiss()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TbError)
                ) {
                    Text("Tetap Keluar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
internal fun BackofficeAccountActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
    badgeColor: Color = TbGreenDark,
    testTag: String = "account-action-row"
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TbSurfaceMuted),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = TbText,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TbText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = TbTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (badge != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.12f),
                    ) {
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = TbTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// PREVIEWS
// ═════════════════════════════════════════════════════════════════════════════

@Preview(name = "Sheet Akun - Admin Online", showBackground = true)
@Composable
private fun BackofficeAccountBottomSheetAdminPreview() {
    TbterminalappTheme {
        BackofficeAccountBottomSheet(
            userName = "Ahmad Admin",
            role = "ADMIN",
            offlineStatus = OfflineStatus(isInternetOnline = true, backendStatus = BackendStatus.CONNECTED),
            devicePreferences = DevicePreferences(paperSize = "58mm", printerBluetoothAddress = "00:11:22"),
            onDismiss = {},
            onProfileClick = {},
            onSettingsClick = {},
            onSyncCenterClick = {},
            onLogout = {}
        )
    }
}

@Preview(name = "Sheet Akun - Owner Offline Antrean", showBackground = true)
@Composable
private fun BackofficeAccountBottomSheetOwnerOfflinePreview() {
    TbterminalappTheme {
        BackofficeAccountBottomSheet(
            userName = "H. Barokah",
            role = "OWNER",
            offlineStatus = OfflineStatus(
                isInternetOnline = false,
                backendStatus = BackendStatus.UNREACHABLE,
                pendingSyncCount = 7
            ),
            devicePreferences = DevicePreferences(paperSize = "80mm"),
            onDismiss = {},
            onProfileClick = {},
            onSettingsClick = {},
            onReportsClick = {},
            onStoreSettingsClick = {},
            onLogout = {}
        )
    }
}
