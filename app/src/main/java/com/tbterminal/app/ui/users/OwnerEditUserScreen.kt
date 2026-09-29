package com.tbterminal.app.ui.users

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.UserRepository
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination

@Composable
fun OwnerEditUserScreen(
    name: String,
    role: String,
    userId: String,
    onDashboardClick: () -> Unit,
    onUserManagementClick: () -> Unit,
    onReportsClick: () -> Unit = {},
    onSyncCenterClick: () -> Unit = {},
    onStockReportClick: () -> Unit = {},
    onReceivablesClick: () -> Unit = {},
    onSupplierDebtsClick: () -> Unit = {},
    onCashReconciliationClick: () -> Unit = {},
    onOperationalAuditClick: () -> Unit = {},
    onSecurityLogClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    userRepository: UserRepository,
    viewModel: EditUserViewModel = viewModel(
        factory = EditUserViewModel.factory(userRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(userId) {
        viewModel.load(userId)
    }

    LaunchedEffect(viewModel.events, onUserManagementClick) {
        viewModel.events.collect { event ->
            when (event) {
                is EditUserEvent.UserUpdated -> onUserManagementClick()
            }
        }
    }

    AdminDashboardShell(
        onProductsClick = {},

        userName = name,
        role = role,
        activeDestination = AdminDestination.UserManagement,
        onDashboardClick = onDashboardClick,
        onReportsClick = onReportsClick,
        onSyncCenterClick = onSyncCenterClick,
        onStockReportClick = onStockReportClick,
        onReceivablesClick = onReceivablesClick,
        onSupplierDebtsClick = onSupplierDebtsClick,
        onCashReconciliationClick = onCashReconciliationClick,
        onOperationalAuditClick = onOperationalAuditClick,
        onUserManagementClick = onUserManagementClick,
        onSecurityLogClick = onSecurityLogClick,
        onSettingsClick = onSettingsClick,
        pageTitle = "Edit pengguna",
        onBack = onUserManagementClick,
        showPageHeader = true,
        onLogout = onLogout
    ) { contentModifier ->
        EditUserContent(
            modifier = contentModifier,
            uiState = uiState,
            onFullNameChange = viewModel::updateFullName,
            onEmailChange = viewModel::updateEmail,
            onUsernameChange = viewModel::updateUsername,
            onRoleChange = viewModel::selectRole,
            onActiveChange = viewModel::updateActive,
            onRetry = { viewModel.loadUser(userId) },
            onCancel = onUserManagementClick,
            onSubmit = viewModel::submit
        )
    }
}
