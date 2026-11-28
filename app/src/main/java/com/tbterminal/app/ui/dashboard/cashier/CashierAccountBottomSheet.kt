package com.tbterminal.app.ui.dashboard.cashier

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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.local.database.DevicePreferences
import com.tbterminal.app.data.sync.BackendStatus
import com.tbterminal.app.data.sync.OfflineStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashierAccountBottomSheet(
    userName: String,
    role: String = "Kasir",
    terminalId: String = "TRM-01",
    offlineStatus: OfflineStatus,
    devicePreferences: DevicePreferences,
    hasActiveShift: Boolean,
    onDismiss: () -> Unit,
    onLockScreen: () -> Unit,
    onSwitchCashier: () -> Unit,
    onChangePin: () -> Unit,
    onSettingsClick: () -> Unit,
    onSyncDataClick: () -> Unit = {},
    syncBadgeCount: Int = offlineStatus.totalSyncBadgeCount,
    onLogout: () -> Unit
) {
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

    val displayName = remember(userName) {
        val trimmed = userName.trim()
        when {
            trimmed.isEmpty() || trimmed.equals("kasir", ignoreCase = true) -> "Kasir"
            trimmed.equals("Budi", ignoreCase = true) -> "Budi Santoso"
            else -> trimmed
        }
    }

    val userInitial = remember(displayName) {
        displayName.firstOrNull()?.uppercase() ?: "K"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE2E8F0))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 1. HEADER IDENTITAS KASIR ──
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
                            .background(Color(0xFFE8F3EC))
                            .border(1.5.dp, Color(0xFF1B5E3C).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userInitial,
                            color = Color(0xFF1B5E3C),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text(
                            text = displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "KASIR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E3C)
                            )
                            Text(
                                text = "· $terminalId (Barokah Jaya)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // ── 2. STATUS CHIPS ROW ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status Shift
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (hasActiveShift) Color(0xFFECFDF5) else Color(0xFFFFF7ED),
                    border = BorderStroke(1.dp, if (hasActiveShift) Color(0xFFA7F3D0) else Color(0xFFFFEDD5)),
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
                                .background(if (hasActiveShift) Color(0xFF10B981) else Color(0xFFEA580C))
                        )
                        Column {
                            Text(
                                text = "STATUS SHIFT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (hasActiveShift) "Shift Aktif" else "Shift Tertutup",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasActiveShift) Color(0xFF047857) else Color(0xFFC2410C)
                            )
                        }
                    }
                }

                // Status Cloud / Sync
                val isOnline = offlineStatus.isInternetOnline && offlineStatus.backendStatus == BackendStatus.CONNECTED
                Surface(
                    onClick = {
                        onDismiss()
                        onSyncDataClick()
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isOnline) Color(0xFFF0FDF4) else Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, if (isOnline) Color(0xFFBBF7D0) else Color(0xFFFDE68A)),
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
                                .background(if (isOnline) Color(0xFF16A34A) else Color(0xFFD97706))
                        )
                        Column {
                            Text(
                                text = "SINKRONISASI",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (syncBadgeCount > 0) {
                                    "$syncBadgeCount antrean"
                                } else if (isOnline) {
                                    "Tersinkron"
                                } else {
                                    "Mode Offline"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOnline && syncBadgeCount == 0) Color(0xFF15803D) else Color(0xFFB45309)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // ── 3. SEKSI KEAMANAN AKUN (Kunci Layar, Ganti Kasir, Ganti PIN) ──
            Text(
                text = "KEAMANAN & AKSES KASIR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 0.6.sp
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AccountActionRow(
                    icon = Icons.Outlined.Lock,
                    title = "Kunci Layar (PIN)",
                    subtitle = "Kunci layar instan dengan 1 ketukan",
                    badge = "Cepat",
                    onClick = {
                        onDismiss()
                        onLockScreen()
                    }
                )
                AccountActionRow(
                    icon = Icons.Outlined.Person,
                    title = "Ganti Kasir",
                    subtitle = "Alihkan giliran ke kasir lain",
                    onClick = {
                        onDismiss()
                        onSwitchCashier()
                    }
                )
                AccountActionRow(
                    icon = Icons.Outlined.Password,
                    title = "Ganti PIN Kasir",
                    subtitle = "Perbarui 6 digit PIN keamanan akun kasir",
                    onClick = {
                        onDismiss()
                        onChangePin()
                    }
                )
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // ── 4. SEKSI PENGATURAN PERANGKAT ──
            Text(
                text = "PERANGKAT & HARDWARE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 0.6.sp
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AccountActionRow(
                    icon = Icons.Outlined.Sync,
                    title = "Sinkron Data",
                    subtitle = if (syncBadgeCount > 0) {
                        "$syncBadgeCount data menunggu dikirim ke server"
                    } else {
                        "Pantau status & kirim data offline"
                    },
                    badge = if (syncBadgeCount > 0) syncBadgeCount.toString() else null,
                    badgeColor = Color(0xFFD97706),
                    onClick = {
                        onDismiss()
                        onSyncDataClick()
                    }
                )
                val isPrinterConfigured = devicePreferences.printerBluetoothAddress.isNotBlank()
                AccountActionRow(
                    icon = Icons.Outlined.Print,
                    title = "Printer Struk (${devicePreferences.paperSize})",
                    subtitle = if (isPrinterConfigured) {
                        "Tersambung: ${devicePreferences.printerBluetoothName.ifBlank { "Printer Bluetooth" }}"
                    } else {
                        "Belum dikonfigurasi (ketuk untuk atur)"
                    },
                    badge = if (isPrinterConfigured) "Siap" else "Atur",
                    badgeColor = if (isPrinterConfigured) Color(0xFF10B981) else Color(0xFFEA580C),
                    onClick = {
                        onDismiss()
                        onSettingsClick()
                    }
                )
                AccountActionRow(
                    icon = Icons.Outlined.Settings,
                    title = "Pengaturan Perangkat",
                    subtitle = "Suara scan barcode, ukuran struk, & tema",
                    onClick = {
                        onDismiss()
                        onSettingsClick()
                    }
                )
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // ── 5. SEKSI INFORMASI APLIKASI ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TB Terminal POS v1.0.0 (Build 1)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = "Terminal Barokah Jaya",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )
            }

            // ── 6. TOMBOL KELUAR ──
            OutlinedButton(
                onClick = { showLogoutConfirmDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFE11D48)
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFFE11D48)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Keluar Akun",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE11D48)
                )
            }
        }
    }

    // ── DIALOG KONFIRMASI KELUAR (DENGAN PERINGATAN SHIFT AKTIF / UN-SYNCED) ──
    if (showLogoutConfirmDialog) {
        val hasWarnings = hasActiveShift || offlineStatus.pendingSyncCount > 0

        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = if (hasWarnings) Icons.Outlined.Warning else Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = null,
                    tint = if (hasWarnings) Color(0xFFDC2626) else Color(0xFF334155),
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = if (hasWarnings) "Peringatan Sebelum Keluar" else "Konfirmasi Keluar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (hasActiveShift) {
                        Text(
                            text = "• Shift kasir saat ini masih AKTIF. Pastikan kasir pengganti mengetahui atau lakukan Tutup Shift terlebih dahulu.",
                            fontSize = 13.sp,
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (offlineStatus.pendingSyncCount > 0) {
                        Text(
                            text = "• Terdapat ${offlineStatus.pendingSyncCount} transaksi yang belum tersinkron ke cloud server.",
                            fontSize = 13.sp,
                            color = Color(0xFFD97706),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = if (hasWarnings) {
                            "Apakah Anda yakin tetap ingin keluar dari akun sekarang?"
                        } else {
                            "Apakah Anda yakin ingin keluar dari akun kasir?"
                        },
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasWarnings) Color(0xFFDC2626) else Color(0xFF1B5E3C),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (hasWarnings) "Tetap Keluar" else "Keluar",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutConfirmDialog = false }
                ) {
                    Text(
                        text = "Batal",
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}

@Composable
private fun AccountActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null,
    badgeColor: Color = Color(0xFF10B981),
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFF334155),
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    if (badge != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(badgeColor.copy(alpha = 0.12f))
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = badge,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
