package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.dashboard.DashboardBackground
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator

@Composable
fun OwnerDashboardShell(
    userName: String,
    role: String,
    activeDestination: OwnerDestination,
    onDashboardClick: () -> Unit,
    onReportsClick: () -> Unit = {},
    onLocalReportsClick: () -> Unit = onReportsClick,
    onSyncCenterClick: () -> Unit = {},
    onBackupRestoreClick: () -> Unit = onSyncCenterClick,
    onStockReportClick: () -> Unit = {},
    onReceivablesClick: () -> Unit = {},
    onSupplierDebtsClick: () -> Unit = {},
    onCashReconciliationClick: () -> Unit = {},
    onOperationalAuditClick: () -> Unit = {},
    onUserManagementClick: () -> Unit,
    onSecurityLogClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val destinationNavigator = LocalAdminDestinationNavigator.current
    fun navigateOrFallback(destination: AdminDestination, fallback: () -> Unit): () -> Unit = {
        destinationNavigator?.invoke(destination) ?: fallback()
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(DashboardBackground)
    ) {
        OwnerDashboardSidebar(
            activeDestination = activeDestination,
            onDashboardClick = onDashboardClick,
            onReportsClick = onReportsClick,
            onLocalReportsClick = onLocalReportsClick,
            onSyncCenterClick = onSyncCenterClick,
            onBackupRestoreClick = navigateOrFallback(AdminDestination.BackupRestore, onBackupRestoreClick),
            onStockReportClick = onStockReportClick,
            onReceivablesClick = onReceivablesClick,
            onSupplierDebtsClick = onSupplierDebtsClick,
            onCashReconciliationClick = onCashReconciliationClick,
            onOperationalAuditClick = onOperationalAuditClick,
            onUserManagementClick = onUserManagementClick,
            onSecurityLogClick = onSecurityLogClick,
            onSettingsClick = onSettingsClick,
            onLogout = onLogout,
            userName = userName,
            role = role,
            modifier = Modifier.width(260.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
            .fillMaxHeight()
        ) {
            OwnerDashboardHeader(userName = userName, role = role)
            content(Modifier.weight(1f))
        }
    }
}
