package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.dashboard.DashboardBackground
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator
import com.tbterminal.app.ui.components.TbCompactDashboardTopBar
import com.tbterminal.app.ui.dashboard.BackofficeAdaptiveShell
import com.tbterminal.app.ui.dashboard.BackofficeSection
import kotlinx.coroutines.launch

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

    fun selectSection(section: BackofficeSection) {
        when (section) {
            BackofficeSection.HOME -> onDashboardClick()
            BackofficeSection.TRANSACTIONS -> navigateOrFallback(AdminDestination.TransactionsHub, {})()
            BackofficeSection.FINANCE -> navigateOrFallback(AdminDestination.FinanceHub, onReceivablesClick)()
            BackofficeSection.STOCK -> navigateOrFallback(AdminDestination.StockHub, onStockReportClick)()
            BackofficeSection.MORE -> navigateOrFallback(AdminDestination.MoreHub, onSettingsClick)()
        }
    }

    BackofficeAdaptiveShell(
        userName = userName,
        role = role,
        activeSection = activeDestination.backofficeSection(),
        onSectionSelected = ::selectSection,
        onProfileClick = navigateOrFallback(AdminDestination.Profile, onSettingsClick),
        onLogout = onLogout
    ) { contentModifier ->
        content(contentModifier)
    }
}

private fun OwnerDestination.backofficeSection(): BackofficeSection = when (this) {
    OwnerDestination.Dashboard -> BackofficeSection.HOME
    OwnerDestination.Receivables,
    OwnerDestination.SupplierDebts,
    OwnerDestination.CashReconciliation -> BackofficeSection.FINANCE
    else -> BackofficeSection.MORE
}
