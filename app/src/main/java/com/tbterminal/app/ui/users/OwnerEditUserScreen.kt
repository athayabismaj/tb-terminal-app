package com.tbterminal.app.ui.users

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.UserRepository
import com.tbterminal.app.ui.dashboard.owner.OwnerEditUserRoute
import com.tbterminal.app.ui.dashboard.owner.OwnerUserToastBridge

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
                is EditUserEvent.UserUpdated -> {
                    OwnerUserToastBridge.setPendingSuccess("Perubahan pengguna disimpan")
                    onUserManagementClick()
                }
            }
        }
    }

    OwnerEditUserRoute(
        uiState = uiState,
        onFullNameChanged = viewModel::updateFullName,
        onUsernameChanged = viewModel::updateUsername,
        onEmailChanged = viewModel::updateEmail,
        onRoleSelected = viewModel::selectRole,
        onSave = viewModel::submit,
        onRetryLoad = {
            viewModel.loadRoles()
            viewModel.loadUser(userId)
        },
        onBack = onUserManagementClick,
        onDashboardClick = onDashboardClick
    )
}
