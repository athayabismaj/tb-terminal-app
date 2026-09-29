package com.tbterminal.app.ui.sync

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.repository.SyncQueueUiModel
import com.tbterminal.app.data.sync.SyncErrorCategory
import com.tbterminal.app.ui.dashboard.BackofficeAdaptiveShell
import com.tbterminal.app.ui.dashboard.BackofficeSection
import com.tbterminal.app.ui.theme.TbterminalappTheme

@Preview(name = "Sinkronisasi - Phone", widthDp = 412, heightDp = 915, showBackground = true)
@Preview(name = "Sinkronisasi - Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun SyncCenterPreview() {
    TbterminalappTheme {
        BackofficeAdaptiveShell(
            userName = "Pemilik Toko",
            role = "OWNER",
            activeSection = BackofficeSection.MENU,
            onSectionSelected = {},
            onProfileClick = {},
            onLogout = {},
            pageTitle = "Sinkronisasi",
            onBack = {},
        ) { contentModifier ->
            SyncCenterScreen(
                modifier = contentModifier,
                uiState = previewState(),
                onRetryItem = {},
                onMarkConflictReviewed = {},
                onRetryAllPending = {},
                onRetryAllFailed = {},
                onDismissMessage = {},
            )
        }
    }
}

private fun previewState(): SyncCenterUiState {
    val transaction = previewItem(
        id = 1,
        type = SyncEntityType.TRANSACTION,
        status = SyncStatus.PENDING,
    )
    val conflict = previewItem(
        id = 2,
        type = SyncEntityType.CASH_SESSION,
        status = SyncStatus.CONFLICT,
        error = "Sesi kas pada server sudah mengalami perubahan.",
        category = SyncErrorCategory.CONFLICT,
    )
    return SyncCenterUiState(
        totalPending = 1,
        totalFailed = 1,
        totalConflict = 1,
        totalSyncedToday = 8,
        queueItems = listOf(conflict, transaction),
        groupedItems = mapOf(
            SyncEntityType.CASH_SESSION to listOf(conflict),
            SyncEntityType.TRANSACTION to listOf(transaction),
        ),
        isInitialLoading = false,
        canRetry = true,
    )
}

private fun previewItem(
    id: Long,
    type: SyncEntityType,
    status: SyncStatus,
    error: String? = null,
    category: SyncErrorCategory = SyncErrorCategory.UNKNOWN,
) = SyncQueueUiModel(
    localId = id,
    entityType = type,
    entityId = 1000 + id,
    syncStatus = status,
    retryCount = if (error == null) 0 else 2,
    lastError = error,
    errorCategory = category,
    recommendation = "Periksa data terakhir sebelum mencoba kembali.",
    createdAt = 1_788_912_000_000,
    updatedAt = 1_788_915_600_000,
)
