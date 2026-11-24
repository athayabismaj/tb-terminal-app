package com.tbterminal.app.ui.users

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.UserRepository
import com.tbterminal.app.ui.dashboard.owner.OwnerTambahUserRoute
import com.tbterminal.app.ui.dashboard.owner.OwnerUserToastBridge

@Composable
fun OwnerAddUserScreen(
    name: String,
    role: String,
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
    viewModel: AddUserViewModel = viewModel(
        factory = AddUserViewModel.factory(userRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel.events, onUserManagementClick) {
        viewModel.events.collect { event ->
            when (event) {
                is AddUserEvent.UserCreated -> {
                    OwnerUserToastBridge.setPendingSuccess("Pengguna ditambahkan")
                    onUserManagementClick()
                }
            }
        }
    }

    OwnerTambahUserRoute(
        uiState = uiState,
        onFullNameChanged = viewModel::updateFullName,
        onUsernameChanged = viewModel::updateUsername,
        onEmailChanged = viewModel::updateEmail,
        onPasswordChanged = viewModel::updatePassword,
        onPinChanged = viewModel::updatePin,
        onRoleSelected = viewModel::selectRole,
        onSave = viewModel::submit,
        onRetryRoles = viewModel::loadRoles,
        onBack = onUserManagementClick,
        onDashboardClick = onDashboardClick
    )
}
