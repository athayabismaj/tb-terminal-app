package com.tbterminal.app.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.repository.LocalBackupSummary
import com.tbterminal.app.data.repository.LocalRestorePreview
import com.tbterminal.app.data.remote.DatabaseBackupJobDto
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val PageBackground = TbBackground
private val CardBorder = TbOutline
private val TextPrimary = TbText
private val TextSecondary = TbTextMuted
private val Teal = TbGreen
private val Orange = TbAmber
private val Red = TbError

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

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(PageBackground)) {
        val compact = maxWidth < 700.dp
        var selectedSection by rememberSaveable { mutableStateOf(BackupPageSection.DEVICE) }
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 1240.dp)
                .fillMaxWidth()
                .padding(horizontal = if (compact) 16.dp else 24.dp, vertical = if (compact) 14.dp else 22.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 18.dp)
        ) {
            uiState.message?.let { message ->
                item { MessageCard(message = message, isError = false, onDismiss = onDismissMessage) }
            }
            uiState.errorMessage?.let { message ->
                item { MessageCard(message = message, isError = true, onDismiss = onDismissMessage) }
            }

            if (compact && canManageServerBackup) {
                item {
                    BackupSectionSelector(
                        selected = selectedSection,
                        onSelected = { selectedSection = it },
                    )
                }
            }

            if (!compact) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        LocalBackupPanel(
                            summary = uiState.summary,
                            compact = false,
                            busy = uiState.isBusy,
                            onCreate = { createBackupLauncher.launch(uiState.summary.suggestedFileName) },
                            onRestore = {
                                restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed", "*/*"))
                            },
                            modifier = Modifier.weight(if (canManageServerBackup) 1f else 2f),
                        )
                        if (canManageServerBackup) {
                            ServerBackupPanel(
                                busy = uiState.isServerBusy,
                                onCreate = onCreateServerBackup,
                                onValidate = {
                                    serverRestoreLauncher.launch(arrayOf("application/octet-stream", "application/x-pgdump", "*/*"))
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            } else if (selectedSection == BackupPageSection.DEVICE || !canManageServerBackup) {
                item {
                    LocalBackupPanel(
                        summary = uiState.summary,
                        compact = true,
                        busy = uiState.isBusy,
                        onCreate = { createBackupLauncher.launch(uiState.summary.suggestedFileName) },
                        onRestore = {
                            restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed", "*/*"))
                        },
                    )
                }
            } else {
                item {
                    ServerBackupPanel(
                        busy = uiState.isServerBusy,
                        onCreate = onCreateServerBackup,
                        onValidate = {
                            serverRestoreLauncher.launch(arrayOf("application/octet-stream", "application/x-pgdump", "*/*"))
                        },
                    )
                }
            }

            if (canManageServerBackup && (!compact || selectedSection == BackupPageSection.SERVER)) {
                item {
                    ServerHistoryHeader(jobCount = uiState.serverJobs.size)
                }
                if (uiState.isServerLoading || uiState.isServerBusy) {
                    item {
                        ServerBackupSkeleton()
                    }
                } else if (uiState.serverJobs.isEmpty()) {
                    item { EmptyServerBackupState() }
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
private fun LocalBackupPanel(
    summary: LocalBackupSummary,
    compact: Boolean,
    busy: Boolean,
    onCreate: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackupSectionCard(modifier) {
        SectionTitle("Cadangan perangkat", "Data aplikasi pada perangkat ini.", busy)
        Spacer(Modifier.height(16.dp))
        BackupMetrics(summary, compact)
        if (summary.hasBackupWarning()) {
            Spacer(Modifier.height(12.dp))
            BackupNotice(summary)
        }
        Spacer(Modifier.height(16.dp))
        LocalActionButtons(busy = busy, onCreate = onCreate, onRestore = onRestore)
    }
}

@Composable
private fun ServerBackupPanel(
    busy: Boolean,
    onCreate: () -> Unit,
    onValidate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackupSectionCard(modifier) {
        SectionTitle("Cadangan database server", "Dikelola aman oleh server.", busy)
        Spacer(Modifier.height(16.dp))
        ServerActionButtons(
            busy = busy,
            onCreate = onCreate,
            onValidate = onValidate,
        )
    }
}

@Composable
private fun ServerActionButtons(
    busy: Boolean,
    onCreate: () -> Unit,
    onValidate: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            enabled = !busy,
            onClick = onCreate,
            colors = ButtonDefaults.buttonColors(containerColor = Teal),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).height(48.dp),
            contentPadding = PaddingValues(horizontal = 10.dp),
        ) {
            Icon(Icons.Outlined.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text("Buat cadangan", maxLines = 1)
        }
        OutlinedButton(
            enabled = !busy,
            onClick = onValidate,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).height(48.dp),
            contentPadding = PaddingValues(horizontal = 10.dp),
        ) {
            Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text("Validasi file", maxLines = 1)
        }
    }
}

@Composable
private fun BackupSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = TbSurface,
        border = BorderStroke(1.dp, CardBorder),
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
private fun BackupSectionSelector(
    selected: BackupPageSection,
    onSelected: (BackupPageSection) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = TbSurfaceMuted.copy(alpha = 0.65f),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            BackupSectionSelectorItem(
                label = "Perangkat",
                selected = selected == BackupPageSection.DEVICE,
                onClick = { onSelected(BackupPageSection.DEVICE) },
                modifier = Modifier.weight(1f),
            )
            BackupSectionSelectorItem(
                label = "Server",
                selected = selected == BackupPageSection.SERVER,
                onClick = { onSelected(BackupPageSection.SERVER) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BackupSectionSelectorItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        color = if (selected) TbSurface else Color.Transparent,
        contentColor = if (selected) TbGreenDark else TextSecondary,
        shape = RoundedCornerShape(11.dp),
        tonalElevation = if (selected) 1.dp else 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
        }
    }
}

@Composable
private fun LocalActionButtons(
    busy: Boolean,
    onCreate: () -> Unit,
    onRestore: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            enabled = !busy,
            onClick = onCreate,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal),
            contentPadding = PaddingValues(horizontal = 10.dp),
        ) {
            Icon(Icons.Outlined.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Simpan", maxLines = 1)
        }
        OutlinedButton(
            enabled = !busy,
            onClick = onRestore,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 10.dp),
        ) {
            Icon(Icons.Outlined.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Pulihkan", maxLines = 1)
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String, busy: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        if (busy) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Teal, strokeWidth = 2.dp)
    }
}

@Composable
private fun BackupMetrics(summary: LocalBackupSummary, compact: Boolean) {
    val first: @Composable RowScope.() -> Unit = {
        BackupMetric("Menunggu", summary.pendingSyncCount, if (summary.pendingSyncCount > 0) Orange else TextSecondary, Modifier.weight(1f))
        BackupMetric("Gagal", summary.failedSyncCount, if (summary.failedSyncCount > 0) Red else TextSecondary, Modifier.weight(1f))
    }
    val second: @Composable RowScope.() -> Unit = {
        BackupMetric("Konflik", summary.conflictCount, if (summary.conflictCount > 0) Orange else TextSecondary, Modifier.weight(1f))
        BackupMetric("Sesi terbuka", summary.openCashSessionCount, if (summary.openCashSessionCount > 0) Orange else Teal, Modifier.weight(1f))
    }
    if (compact) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { first() }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { second() }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { first(); second() }
    }
}

@Composable
private fun BackupMetric(label: String, value: Int, color: Color, modifier: Modifier) {
    Surface(modifier = modifier, color = TbSurfaceMuted, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(value.toString(), color = color, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun BackupNotice(summary: LocalBackupSummary) {
    Surface(color = TbSurfaceMuted, shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.Warning, contentDescription = null, tint = Orange, modifier = Modifier.size(20.dp))
            Text(
                text = "Periksa sinkronisasi dan sesi kas sebelum memulihkan data.",
                modifier = Modifier.padding(start = 10.dp),
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun LocalBackupSummary.hasBackupWarning(): Boolean =
    pendingSyncCount + failedSyncCount + conflictCount + openCashSessionCount > 0

@Composable
private fun ServerHistoryHeader(jobCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Riwayat server",
            color = TextPrimary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "$jobCount proses",
            color = TextSecondary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun ServerBackupSkeleton() {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("server-backup-skeleton"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(3) {
            com.tbterminal.app.ui.components.SkeletonCard(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun EmptyServerBackupState() {
    Surface(modifier = Modifier.fillMaxWidth(), color = TbSurface, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, CardBorder)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.CloudDownload, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text("Belum ada riwayat cadangan server", color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("Cadangan baru akan tampil di bagian ini.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
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
        shape = RoundedCornerShape(18.dp),
        color = TbSurface,
        border = BorderStroke(1.dp, CardBorder),
        tonalElevation = 1.dp,
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(42.dp).background(TbGreenLight, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.CloudDownload, contentDescription = null, tint = TbGreenDark, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(job.operation.toOperationLabel(), modifier = Modifier.weight(1f, fill = false), color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    JobStatusBadge(job.status)
                }
                Text(job.fileName.ifBlank { "File belum tersedia" }, color = TextSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(job.createdAt.formatInstant(), color = TextSecondary, fontSize = 12.sp)
                job.errorMessage?.let { Text(it, color = Red, fontSize = 12.sp) }
                if (job.operation == "BACKUP" && job.status == "SUCCEEDED" && job.removedAt == null) {
                    TextButton(
                        enabled = !busy,
                        onClick = onDownload,
                        modifier = Modifier.height(40.dp),
                        contentPadding = PaddingValues(horizontal = 0.dp),
                    ) {
                        Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Unduh file")
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
        failed -> Red.copy(alpha = 0.10f)
        completed -> TbGreenLight
        else -> TbSurfaceMuted
    }
    val foreground = when {
        failed -> Red
        completed -> TbGreenDark
        else -> TextSecondary
    }
    Surface(color = background, shape = RoundedCornerShape(999.dp)) {
        Text(status.toStatusLabel(), modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp), color = foreground, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
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
        title = { Text("Konfirmasi pemulihan server", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Pemulihan hanya boleh dijalankan saat maintenance pada database local/test terisolasi.", color = Red, fontWeight = FontWeight.Bold)
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
                    Text("Saya memahami layanan berhenti sementara dan data akan ditimpa.", color = TextPrimary)
                }
                Text("Jika koneksi timeout, aplikasi hanya memeriksa status tanpa mengirim konfirmasi ulang.", color = TextSecondary, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy && acknowledged && phraseInput == prompt.confirmationPhrase,
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Red)
            ) { Text("Mulai pemulihan") }
        },
        dismissButton = { OutlinedButton(enabled = !isBusy, onClick = onDismiss) { Text("Batal") } }
    )
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
        title = { Text("Konfirmasi pemulihan", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Data lokal saat ini akan diganti setelah aplikasi direstart.", color = TextPrimary)
                Text("Cadangan dibuat: ${preview.metadata.createdAt.formatInstant()}", color = TextSecondary)
                Text("Versi aplikasi: ${preview.metadata.appVersion}", color = TextSecondary)
                Text("Menunggu: ${preview.metadata.pendingSyncCount}, gagal: ${preview.metadata.failedSyncCount}, konflik: ${preview.metadata.conflictCount}", color = TextSecondary)
                if (preview.openCashSessionCount > 0) {
                    Text(
                        text = "Peringatan: masih ada ${preview.openCashSessionCount} sesi kas terbuka pada data lokal.",
                        color = Red,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (preview.syncingCount > 0) {
                    Text(
                        text = "Pemulihan tidak dapat berjalan saat sinkronisasi aktif.",
                        color = Red,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Lakukan pemulihan saat tidak ada transaksi berjalan. Setelah aplikasi dibuka ulang, periksa halaman Sinkronisasi.",
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
                Text("Pulihkan data")
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
    val bg = if (isError) Red.copy(alpha = 0.08f) else TbGreenLight
    val fg = if (isError) Red else TbGreenDark
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(message, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onDismiss) {
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
