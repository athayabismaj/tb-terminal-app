package com.tbterminal.app.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tbterminal.app.data.remote.DatabaseBackupJobDto
import com.tbterminal.app.data.repository.LocalBackupSummary
import com.tbterminal.app.data.repository.LocalRestorePreview
import com.tbterminal.app.ui.components.SkeletonCard
import com.tbterminal.app.ui.components.TopFloatingCapsule
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// ── COLOR PALETTE ACCORDING TO HTML DESIGN ──
private val BrandSage = Color(0xFF256B57)       // Primary Sage Brand
private val BrandSageLight = Color(0xFFF0F7F4)  // brand-50
private val BrandSageDark = Color(0xFF1E5646)   // brand-600

private val SurfaceBg = Color(0xFFF4F6F4)
private val SurfaceBorder = Color(0xFFE2E8F0)

private val Slate900 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate600 = Color(0xFF475569)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate100 = Color(0xFFF1F5F9)

private val AmberBg = Color(0xFFFFFBEB)
private val AmberBorder = Color(0xFFFDE68A)
private val AmberText = Color(0xFFD97706)

private val RoseBg = Color(0xFFFFF1F2)
private val RoseBorder = Color(0xFFFDA4AF)
private val RoseText = Color(0xFFE11D48)

private enum class BackupPageSection { DEVICE, SERVER }

@Composable
fun BackupRestoreScreen(
    uiState: BackupRestoreUiState,
    onCreateBackup: (android.net.Uri?) -> Unit,
    onRestoreFileSelected: (android.net.Uri?) -> Unit,
    onConfirmRestore: () -> Unit,
    onDismissRestore: () -> Unit,
    onDismissMessage: () -> Unit,
    canManageServerBackup: Boolean,
    onCreateServerBackup: () -> Unit,
    onDownloadServerBackup: (DatabaseBackupJobDto, android.net.Uri?) -> Unit,
    onServerRestoreFileSelected: (android.net.Uri?) -> Unit,
    onServerRestorePhraseChanged: (String) -> Unit,
    onServerRestoreAcknowledgedChanged: (Boolean) -> Unit,
    onConfirmServerRestore: () -> Unit,
    onDismissServerRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDownloadJob by remember { mutableStateOf<DatabaseBackupJobDto?>(null) }
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = onCreateBackup
    )
    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = onRestoreFileSelected
    )
    val serverRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = onServerRestoreFileSelected
    )
    val serverDownloadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        pendingDownloadJob?.let { onDownloadServerBackup(it, uri) }
        pendingDownloadJob = null
    }

    var selectedSection by rememberSaveable { mutableStateOf(BackupPageSection.DEVICE) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceBg),
        contentAlignment = Alignment.TopCenter
    ) {
        val isWide = maxWidth >= 680.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = if (isWide) 640.dp else 480.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── TAB SWITCHER (PERANGKAT vs SERVER) ──
            if (canManageServerBackup) {
                item {
                    TabSwitcher(
                        selected = selectedSection,
                        onSelected = { selectedSection = it }
                    )
                }
            }

            // ── TAB CONTENT ──
            if (selectedSection == BackupPageSection.DEVICE || !canManageServerBackup) {
                // "Cadangan perangkat" Card
                item {
                    LocalBackupCard(
                        summary = uiState.summary,
                        busy = uiState.isBusy,
                        onCreate = { createBackupLauncher.launch(uiState.summary.suggestedFileName) },
                        onRestore = {
                            restoreLauncher.launch(
                                arrayOf(
                                    "application/zip",
                                    "application/octet-stream",
                                    "application/x-zip-compressed",
                                    "*/*"
                                )
                            )
                        }
                    )
                }
            } else {
                // "Cadangan database server" Card
                item {
                    ServerBackupCard(
                        busy = uiState.isServerBusy,
                        onCreate = onCreateServerBackup,
                        onValidate = {
                            serverRestoreLauncher.launch(
                                arrayOf(
                                    "application/octet-stream",
                                    "application/x-pgdump",
                                    "*/*"
                                )
                            )
                        }
                    )
                }

                // Section: RIWAYAT SERVER Header
                item {
                    ServerHistoryHeader(jobCount = uiState.serverJobs.size)
                }

                // Server Backup Jobs / Empty State / Skeleton
                if (uiState.isServerLoading || uiState.isServerBusy) {
                    item { ServerBackupSkeleton() }
                } else if (uiState.serverJobs.isEmpty()) {
                    item { EmptyServerBackupState() }
                } else {
                    items(uiState.serverJobs, key = { it.id }) { job ->
                        ServerBackupJobCard(
                            job = job,
                            busy = uiState.isServerBusy,
                            onDownload = {
                                pendingDownloadJob = job
                                serverDownloadLauncher.launch(job.fileName)
                            }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
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

    // ── RESTORE CONFIRMATION DIALOGS ──
    uiState.selectedRestorePreview?.let { preview ->
        RestoreConfirmDialog(
            preview = preview,
            isBusy = uiState.isBusy,
            onDismiss = onDismissRestore,
            onConfirm = onConfirmRestore
        )
    }

    uiState.serverRestorePrompt?.let { prompt ->
        ServerRestoreConfirmDialog(
            prompt = prompt,
            phraseInput = uiState.serverRestorePhraseInput,
            acknowledged = uiState.serverRestoreAcknowledged,
            isBusy = uiState.isServerBusy,
            onPhraseChanged = onServerRestorePhraseChanged,
            onAcknowledgedChanged = onServerRestoreAcknowledgedChanged,
            onDismiss = onDismissServerRestore,
            onConfirm = onConfirmServerRestore
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// TAB SWITCHER (PERANGKAT vs SERVER)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun TabSwitcher(
    selected: BackupPageSection,
    onSelected: (BackupPageSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SurfaceBorder.copy(alpha = 0.8f)),
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabSwitcherItem(
                label = "Perangkat",
                icon = Icons.Outlined.Smartphone,
                selected = selected == BackupPageSection.DEVICE,
                onClick = { onSelected(BackupPageSection.DEVICE) },
                modifier = Modifier.weight(1f)
            )

            TabSwitcherItem(
                label = "Server",
                icon = Icons.Outlined.Cloud,
                selected = selected == BackupPageSection.SERVER,
                onClick = { onSelected(BackupPageSection.SERVER) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TabSwitcherItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) BrandSage else Slate500,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) BrandSage else Slate500
                )
            }

            if (selected) {
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(BrandSage)
                )
            } else {
                Spacer(Modifier.height(8.5.dp))
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 1. "CADANGAN PERANGKAT" CARD
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun LocalBackupCard(
    summary: LocalBackupSummary,
    busy: Boolean,
    onCreate: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SurfaceBorder.copy(alpha = 0.8f)),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Title & Subtitle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cadangan perangkat",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        letterSpacing = (-0.2).sp
                    )
                    Text(
                        text = "Data aplikasi pada perangkat ini.",
                        fontSize = 12.sp,
                        color = Slate500,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = BrandSage,
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Clean 2x2 Metric Grid (Open typography on white background)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Metrik 1: Menunggu
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = summary.pendingSyncCount.toString(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            lineHeight = 28.sp
                        )
                        Text(
                            text = "Menunggu",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate500,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Metrik 2: Gagal
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = summary.failedSyncCount.toString(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (summary.failedSyncCount > 0) RoseText else Slate900,
                            lineHeight = 28.sp
                        )
                        Text(
                            text = "Gagal",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate500,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Metrik 3: Konflik
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = summary.conflictCount.toString(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (summary.conflictCount > 0) AmberText else Slate900,
                            lineHeight = 28.sp
                        )
                        Text(
                            text = "Konflik",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate500,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Metrik 4: Sesi terbuka
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = summary.openCashSessionCount.toString(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (summary.openCashSessionCount > 0) AmberText else Slate900,
                            lineHeight = 28.sp
                        )
                        Text(
                            text = "Sesi terbuka",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate500,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Warning Notice if needed
            if (summary.hasBackupWarning()) {
                Spacer(Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AmberBg,
                    border = BorderStroke(1.dp, AmberBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = AmberText,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Periksa sinkronisasi dan sesi kas sebelum memulihkan data.",
                            fontSize = 12.sp,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Slate100, thickness = 1.dp)
            Spacer(Modifier.height(16.dp))

            // Action Buttons (Simpan & Pulihkan)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Button Simpan
                Button(
                    enabled = !busy,
                    onClick = onCreate,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandSage,
                        contentColor = Color.White,
                        disabledContainerColor = BrandSage.copy(alpha = 0.5f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Simpan",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Button Pulihkan
                OutlinedButton(
                    enabled = !busy,
                    onClick = onRestore,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Slate700
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Restore,
                        contentDescription = null,
                        tint = Slate700,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Pulihkan",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 2. "CADANGAN DATABASE SERVER" CARD
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun ServerBackupCard(
    busy: Boolean,
    onCreate: () -> Unit,
    onValidate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SurfaceBorder.copy(alpha = 0.8f)),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cadangan database server",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        letterSpacing = (-0.2).sp
                    )
                    Text(
                        text = "Dikelola aman oleh server.",
                        fontSize = 12.sp,
                        color = Slate500,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (busy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = BrandSage,
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Action Buttons (Buat cadangan & Validasi file)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Button Buat cadangan
                Button(
                    enabled = !busy,
                    onClick = onCreate,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandSage,
                        contentColor = Color.White,
                        disabledContainerColor = BrandSage.copy(alpha = 0.5f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Buat cadangan",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }

                // Button Validasi file
                OutlinedButton(
                    enabled = !busy,
                    onClick = onValidate,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Slate700
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Restore,
                        contentDescription = null,
                        tint = Slate600,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Validasi file",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 3. RIWAYAT SERVER SECTION & EMPTY STATE
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun ServerHistoryHeader(jobCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "RIWAYAT SERVER",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Slate500,
            letterSpacing = 0.6.sp
        )

        Surface(
            shape = RoundedCornerShape(999.dp),
            color = Slate100
        ) {
            Text(
                text = "$jobCount proses",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Slate600,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun EmptyServerBackupState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SurfaceBorder.copy(alpha = 0.8f)),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(BrandSageLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Cloud,
                    contentDescription = null,
                    tint = BrandSage,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Belum ada riwayat cadangan server",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Slate800
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Cadangan baru akan tampil di bagian ini.",
                fontSize = 12.sp,
                color = Slate400
            )
        }
    }
}

@Composable
private fun ServerBackupSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("server-backup-skeleton"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(3) {
            SkeletonCard(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ServerBackupJobCard(
    job: DatabaseBackupJobDto,
    busy: Boolean,
    onDownload: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, SurfaceBorder.copy(alpha = 0.8f)),
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BrandSageLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudDownload,
                    contentDescription = null,
                    tint = BrandSage,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = job.operation.toOperationLabel(),
                        color = Slate900,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    JobStatusBadge(job.status)
                }

                Text(
                    text = job.fileName.ifBlank { "File belum tersedia" },
                    color = Slate500,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = job.createdAt.formatInstant(),
                    color = Slate400,
                    fontSize = 11.sp
                )

                job.errorMessage?.let {
                    Text(text = it, color = RoseText, fontSize = 11.sp)
                }

                if (job.operation == "BACKUP" && job.status == "SUCCEEDED" && job.removedAt == null) {
                    TextButton(
                        enabled = !busy,
                        onClick = onDownload,
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CloudDownload,
                            contentDescription = null,
                            tint = BrandSage,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Unduh file",
                            color = BrandSage,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JobStatusBadge(status: String) {
    val failed = status == "FAILED"
    val completed = status == "SUCCEEDED"
    val background = when {
        failed -> RoseBg
        completed -> BrandSageLight
        else -> Slate100
    }
    val foreground = when {
        failed -> RoseText
        completed -> BrandSageDark
        else -> Slate600
    }
    Surface(
        color = background,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = status.toStatusLabel(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = foreground,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// DIALOGS & MESSAGES
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun RestoreConfirmDialog(
    preview: LocalRestorePreview,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        shape = RoundedCornerShape(16.dp),
        title = { Text("Konfirmasi pemulihan", fontWeight = FontWeight.Bold, color = Slate900) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Data lokal saat ini akan diganti setelah aplikasi direstart.", color = Slate900)
                Text("Cadangan dibuat: ${preview.metadata.createdAt.formatInstant()}", color = Slate500, fontSize = 12.sp)
                Text("Versi aplikasi: ${preview.metadata.appVersion}", color = Slate500, fontSize = 12.sp)
                Text(
                    text = "Menunggu: ${preview.metadata.pendingSyncCount}, gagal: ${preview.metadata.failedSyncCount}, konflik: ${preview.metadata.conflictCount}",
                    color = Slate500,
                    fontSize = 12.sp
                )
                if (preview.openCashSessionCount > 0) {
                    Text(
                        text = "Peringatan: masih ada ${preview.openCashSessionCount} sesi kas terbuka pada data lokal.",
                        color = RoseText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                if (preview.syncingCount > 0) {
                    Text(
                        text = "Pemulihan tidak dapat berjalan saat sinkronisasi aktif.",
                        color = RoseText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = "Lakukan pemulihan saat tidak ada transaksi berjalan. Setelah aplikasi dibuka ulang, periksa halaman Sinkronisasi.",
                    color = Slate500,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy,
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoseText)
            ) {
                Text("Pulihkan data")
            }
        },
        dismissButton = {
            OutlinedButton(
                enabled = !isBusy,
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun ServerRestoreConfirmDialog(
    prompt: ServerRestorePrompt,
    phraseInput: String,
    acknowledged: Boolean,
    isBusy: Boolean,
    onPhraseChanged: (String) -> Unit,
    onAcknowledgedChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        shape = RoundedCornerShape(16.dp),
        title = { Text("Konfirmasi pemulihan server", fontWeight = FontWeight.Bold, color = Slate900) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Pemulihan hanya boleh dijalankan saat maintenance pada database local/test terisolasi.",
                    color = RoseText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Text("Ketik persis: ${prompt.confirmationPhrase}", color = Slate900, fontWeight = FontWeight.Medium)
                Text("Token kedaluwarsa: ${prompt.expiresAt.formatInstant()}", color = Slate500, fontSize = 12.sp)
                OutlinedTextField(
                    value = phraseInput,
                    onValueChange = onPhraseChanged,
                    label = { Text("Frasa konfirmasi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = acknowledged, onCheckedChange = onAcknowledgedChanged)
                    Text("Saya memahami layanan berhenti sementara dan data akan ditimpa.", color = Slate800, fontSize = 12.sp)
                }
                Text("Jika koneksi timeout, aplikasi hanya memeriksa status tanpa mengirim konfirmasi ulang.", color = Slate400, fontSize = 11.sp)
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy && acknowledged && phraseInput == prompt.confirmationPhrase,
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoseText)
            ) {
                Text("Mulai pemulihan")
            }
        },
        dismissButton = {
            OutlinedButton(
                enabled = !isBusy,
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun MessageCard(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    val bg = if (isError) RoseBg else BrandSageLight
    val border = if (isError) RoseBorder else SurfaceBorder
    val fg = if (isError) RoseText else BrandSageDark

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                color = fg,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Tutup",
                    tint = fg,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun LocalBackupSummary.hasBackupWarning(): Boolean =
    pendingSyncCount + failedSyncCount + conflictCount + openCashSessionCount > 0

private fun String.formatInstant(): String {
    return runCatching {
        Instant.parse(this)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
    }.getOrDefault(this)
}

private fun String.toOperationLabel(): String = when (uppercase()) {
    "BACKUP" -> "Cadangan"
    "RESTORE" -> "Pemulihan"
    else -> replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}

private fun String.toStatusLabel(): String = when (uppercase()) {
    "QUEUED", "PENDING" -> "Menunggu"
    "RUNNING", "PROCESSING" -> "Diproses"
    "SUCCEEDED" -> "Selesai"
    "FAILED" -> "Gagal"
    else -> replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
}
