package com.tbterminal.app.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.repository.LocalBackupSummary
import com.tbterminal.app.data.repository.LocalRestorePreview
import com.tbterminal.app.data.remote.DatabaseBackupJobDto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val PageBackground = Color(0xFFF4F8FA)
private val CardBorder = Color(0xFFDDE6EC)
private val TextPrimary = Color(0xFF111827)
private val TextSecondary = Color(0xFF64748B)
private val Teal = Color(0xFF009B72)
private val Orange = Color(0xFFF59E0B)
private val Red = Color(0xFFDC2626)

@Composable
fun BackupRestoreScreen(
    uiState: BackupRestoreUiState,
    onCreateBackup: (android.net.Uri?) -> Unit,
    onRestoreFileSelected: (android.net.Uri?) -> Unit,
    onConfirmRestore: () -> Unit,
    onDismissRestore: () -> Unit,
    onDismissMessage: () -> Unit,
    canManageServerBackup: Boolean,
    onRefreshServerBackups: () -> Unit,
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PageBackground)
            .padding(horizontal = 30.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Backup & Restore",
                        color = TextPrimary,
                        fontSize = 31.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Backup lokal perangkat dipisahkan dari backup PostgreSQL server.",
                        color = TextSecondary,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (uiState.isBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), color = Teal)
                }
            }
        }

        uiState.message?.let { message ->
            item { MessageCard(message = message, isError = false, onDismiss = onDismissMessage) }
        }
        uiState.errorMessage?.let { message ->
            item { MessageCard(message = message, isError = true, onDismiss = onDismissMessage) }
        }

        item {
            Text("Backup Lokal Perangkat", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        item {
            BackupWarningCard(summary = uiState.summary)
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryCard(
                    title = "Pending Sync",
                    value = uiState.summary.pendingSyncCount.toString(),
                    accent = Orange,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Failed Sync",
                    value = uiState.summary.failedSyncCount.toString(),
                    accent = Red,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Conflict",
                    value = uiState.summary.conflictCount.toString(),
                    accent = Color(0xFF7C3AED),
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Sesi Open",
                    value = uiState.summary.openCashSessionCount.toString(),
                    accent = Teal,
                    modifier = Modifier.weight(1f)
                )
            }
        }


        if (canManageServerBackup) {
            item { HorizontalDivider(color = CardBorder) }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Backup Database Server", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Job PostgreSQL berjalan asinkron; status dipantau tanpa mengulang restore.", color = TextSecondary, fontSize = 13.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(enabled = !uiState.isServerBusy, onClick = onCreateServerBackup, colors = ButtonDefaults.buttonColors(containerColor = Teal)) { Text("Buat Backup Server") }
                        OutlinedButton(
                            enabled = !uiState.isServerBusy,
                            onClick = { serverRestoreLauncher.launch(arrayOf("application/octet-stream", "application/x-pgdump", "*/*")) }
                        ) { Text("Validasi File Restore") }
                    }
                }
            }
            if (uiState.isServerLoading || uiState.isServerBusy) {
                item {
                    com.tbterminal.app.ui.components.SkeletonList(
                        modifier = Modifier.fillMaxWidth(),
                        itemCount = 3,
                    )
                }
            }
            if (uiState.serverJobs.isEmpty() && !uiState.isServerLoading) {
                item { Text("Belum ada metadata backup/restore server.", color = TextSecondary) }
            }
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

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Backup,
                    title = "Buat Backup",
                    description = "Backup berisi database Room dan metadata status sync. User memilih lokasi simpan file.",
                    buttonText = "Buat Backup",
                    enabled = !uiState.isBusy,
                    onClick = { createBackupLauncher.launch(uiState.summary.suggestedFileName) }
                )
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Restore,
                    title = "Restore dari File",
                    description = "Pilih file backup lokal. Restore disiapkan dan diterapkan saat aplikasi direstart.",
                    buttonText = "Pilih File",
                    enabled = !uiState.isBusy,
                    onClick = {
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
        }
    }

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

@Composable
private fun ServerBackupJobCard(
    job: DatabaseBackupJobDto,
    busy: Boolean,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${job.operation} • ${job.status}", color = if (job.status == "FAILED") Red else TextPrimary, fontWeight = FontWeight.Bold)
                Text(job.fileName, color = TextSecondary, fontSize = 13.sp)
                Text(job.createdAt.formatInstant(), color = TextSecondary, fontSize = 12.sp)
                job.errorMessage?.let { Text(it, color = Red, fontSize = 12.sp) }
            }
            if (job.operation == "BACKUP" && job.status == "SUCCEEDED" && job.removedAt == null) {
                OutlinedButton(enabled = !busy, onClick = onDownload) { Text("Download") }
            }
        }
    }
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
        title = { Text("Konfirmasi Restore Database Server", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Restore hanya boleh dijalankan saat maintenance pada database local/test terisolasi.", color = Red, fontWeight = FontWeight.Bold)
                Text("Ketik persis: ${prompt.confirmationPhrase}", color = TextPrimary)
                Text("Token kedaluwarsa: ${prompt.expiresAt.formatInstant()}", color = TextSecondary)
                androidx.compose.material3.OutlinedTextField(
                    value = phraseInput,
                    onValueChange = onPhraseChanged,
                    label = { Text("Frasa konfirmasi") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = acknowledged, onCheckedChange = onAcknowledgedChanged)
                    Text("Saya memahami downtime dan data database akan ditimpa.", color = TextPrimary)
                }
                Text("Jika request timeout, aplikasi hanya memeriksa status dan tidak mengirim confirm ulang.", color = TextSecondary, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy && acknowledged && phraseInput == prompt.confirmationPhrase,
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Red)
            ) { Text("Jalankan Restore") }
        },
        dismissButton = { OutlinedButton(enabled = !isBusy, onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun BackupWarningCard(summary: LocalBackupSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = if (summary.pendingSyncCount + summary.failedSyncCount + summary.conflictCount > 0) Orange else Teal,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = "Backup mencakup data yang belum tersinkron.",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pastikan file backup disimpan di tempat aman. Restore tidak mengubah data server dan tidak menjalankan sync otomatis.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 5.dp)
                )
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
        modifier = modifier.height(112.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title.uppercase(), color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(value, color = accent, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    buttonText: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(220.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Icon(icon, contentDescription = null, tint = Teal, modifier = Modifier.size(30.dp))
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 6.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(
                enabled = enabled,
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal)
            ) {
                Text(buttonText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RestoreConfirmDialog(
    preview: LocalRestorePreview,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        title = { Text("Konfirmasi Restore", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Data lokal saat ini akan diganti setelah aplikasi direstart.", color = TextPrimary)
                Text("Backup dibuat: ${preview.metadata.createdAt.formatInstant()}", color = TextSecondary)
                Text("App version: ${preview.metadata.appVersion}", color = TextSecondary)
                Text("Pending: ${preview.metadata.pendingSyncCount}, Failed: ${preview.metadata.failedSyncCount}, Conflict: ${preview.metadata.conflictCount}", color = TextSecondary)
                if (preview.openCashSessionCount > 0) {
                    Text(
                        text = "Peringatan: masih ada ${preview.openCashSessionCount} sesi kas OPEN di data lokal saat ini.",
                        color = Red,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (preview.syncingCount > 0) {
                    Text(
                        text = "Restore tidak akan berjalan saat ada sync aktif.",
                        color = Red,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Lakukan restore saat tidak ada transaksi berjalan. Setelah restart, buka Sync Center untuk mengecek status data.",
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy,
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Red)
            ) {
                Text("Konfirmasi Restore")
            }
        },
        dismissButton = {
            OutlinedButton(enabled = !isBusy, onClick = onDismiss) {
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
    val bg = if (isError) Color(0xFFFFF1F2) else Color(0xFFE8F7F1)
    val fg = if (isError) Red else Teal
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(message, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        OutlinedButton(onClick = onDismiss) {
            Text("Tutup")
        }
    }
}

private fun String.formatInstant(): String {
    return runCatching {
        Instant.parse(this)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
    }.getOrDefault(this)
}
