package com.tbterminal.app.ui.audit

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.tbterminal.app.data.model.AuditLogItem
import com.tbterminal.app.ui.dashboard.BackofficeAdaptiveShell
import com.tbterminal.app.ui.dashboard.BackofficeSection
import com.tbterminal.app.ui.theme.TbterminalappTheme

@Preview(name = "Riwayat Aktivitas - Phone", widthDp = 412, heightDp = 915, showBackground = true)
@Preview(name = "Riwayat Aktivitas - Phone Landscape", widthDp = 915, heightDp = 412, showBackground = true)
@Preview(name = "Riwayat Aktivitas - Font Besar", widthDp = 412, heightDp = 915, fontScale = 1.3f, showBackground = true)
@Preview(name = "Riwayat Aktivitas - Tablet Portrait", widthDp = 800, heightDp = 1280, showBackground = true)
@Preview(name = "Riwayat Aktivitas - Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun OperationalAuditPreview() {
    TbterminalappTheme {
        BackofficeAdaptiveShell(
            userName = "Pemilik Toko",
            role = "OWNER",
            activeSection = BackofficeSection.MENU,
            onSectionSelected = {},
            onProfileClick = {},
            onLogout = {},
            pageTitle = "Riwayat aktivitas",
            onBack = {},
        ) { contentModifier ->
            AdminOperationalAuditScreen(
                modifier = contentModifier,
                uiState = auditPreviewState(),
                onActionFilterChanged = {},
                onDateChanged = {},
                onDatePresetSelected = {},
                onRetry = {},
                onPreviousPage = {},
                onNextPage = {},
            )
        }
    }
}

private fun auditPreviewState() = AdminOperationalAuditUiState(
    logs = listOf(
        auditPreviewLog("1", "INSERT", "products", "Produk Semen ditambahkan"),
        auditPreviewLog("2", "UPDATE", "stock_adjustments", "Stok disesuaikan"),
    ),
    totalPages = 2,
)

private fun auditPreviewLog(id: String, action: String, table: String, label: String) = AuditLogItem(
    id = id,
    actorUserId = "owner-1",
    actorName = "Pemilik Toko",
    actorRole = "OWNER",
    action = action,
    schemaName = "inventory",
    tableName = table,
    recordId = "record-$id",
    ipAddress = null,
    oldData = if (action == "UPDATE") "{\"stock\":10}" else null,
    newData = "{\"stock\":15}",
    activityLabel = label,
    createdAt = "2026-09-09T08:30:00+07:00",
)
