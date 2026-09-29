package com.tbterminal.app.ui.backup

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.tbterminal.app.data.remote.DatabaseBackupJobDto
import com.tbterminal.app.data.repository.LocalBackupSummary
import com.tbterminal.app.ui.dashboard.BackofficeAdaptiveShell
import com.tbterminal.app.ui.dashboard.BackofficeSection
import com.tbterminal.app.ui.theme.TbterminalappTheme

@Preview(name = "Cadangan Data - Phone", widthDp = 412, heightDp = 915, showBackground = true)
@Preview(name = "Cadangan Data - Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun BackupRestorePreview() {
    TbterminalappTheme {
        BackofficeAdaptiveShell(
            userName = "Pemilik Toko",
            role = "OWNER",
            activeSection = BackofficeSection.MENU,
            onSectionSelected = {},
            onProfileClick = {},
            onLogout = {},
            pageTitle = "Cadangan data",
            onBack = {},
        ) { contentModifier ->
            BackupRestoreScreen(
                modifier = contentModifier,
                uiState = backupPreviewState(),
                onCreateBackup = {},
                onRestoreFileSelected = {},
                onConfirmRestore = {},
                onDismissRestore = {},
                onDismissMessage = {},
                canManageServerBackup = true,
                onCreateServerBackup = {},
                onDownloadServerBackup = { _, _ -> },
                onServerRestoreFileSelected = {},
                onServerRestorePhraseChanged = {},
                onServerRestoreAcknowledgedChanged = {},
                onConfirmServerRestore = {},
                onDismissServerRestore = {},
            )
        }
    }
}

private fun backupPreviewState() = BackupRestoreUiState(
    summary = LocalBackupSummary(
        pendingSyncCount = 2,
        failedSyncCount = 0,
        conflictCount = 0,
        openCashSessionCount = 1,
    ),
    serverJobs = listOf(
        DatabaseBackupJobDto(
            id = "backup-1",
            operation = "BACKUP",
            status = "SUCCEEDED",
            fileName = "tb-terminal-2026-09-09.dump",
            createdAt = "2026-09-09T08:30:00Z",
        ),
    ),
)
