package com.tbterminal.app.ui.users

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.UserRepository
import com.tbterminal.app.ui.dashboard.owner.OwnerDashboardShell
import com.tbterminal.app.ui.dashboard.owner.OwnerDestination

@Composable
fun OwnerUserManagementScreen(
    name: String,
    role: String,
    onDashboardClick: () -> Unit,
    onAddUserClick: () -> Unit,
    onEditUserClick: (String) -> Unit,
    onChangePasswordClick: (String) -> Unit,
    onChangePinClick: (String) -> Unit,
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
    viewModel: UserManagementViewModel = viewModel(
        factory = UserManagementViewModel.factory(userRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var deactivatingStaff by remember { mutableStateOf<StaffMember?>(null) }

    OwnerDashboardShell(
        userName = name,
        role = role,
        activeDestination = OwnerDestination.UserManagement,
        onDashboardClick = onDashboardClick,
        onReportsClick = onReportsClick,
        onSyncCenterClick = onSyncCenterClick,
        onStockReportClick = onStockReportClick,
        onReceivablesClick = onReceivablesClick,
        onSupplierDebtsClick = onSupplierDebtsClick,
        onCashReconciliationClick = onCashReconciliationClick,
        onOperationalAuditClick = onOperationalAuditClick,
        onUserManagementClick = {},
        onSecurityLogClick = onSecurityLogClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    ) { contentModifier ->
        UserManagementContent(
            modifier = contentModifier,
            uiState = uiState,
            onAddUserClick = onAddUserClick,
            onRetry = viewModel::loadUsers,
            onEditUserClick = { staff ->
                viewModel.clearActionMessages()
                onEditUserClick(staff.id)
            },
            onChangePasswordClick = { staff ->
                viewModel.clearActionMessages()
                onChangePasswordClick(staff.id)
            },
            onChangePinClick = { staff ->
                viewModel.clearActionMessages()
                onChangePinClick(staff.id)
            },
            onDeactivateUserClick = { staff ->
                viewModel.clearActionMessages()
                deactivatingStaff = staff
            },
            onActivateUserClick = { staff ->
                viewModel.clearActionMessages()
                viewModel.activateUser(staff)
            },
            onSecurityLogClick = onSecurityLogClick,
            onDismissActionMessage = viewModel::clearActionMessages
        )
    }

    deactivatingStaff?.let { staff ->
        StaffDeactivateDialog(
            staff = staff,
            isSaving = uiState.isMutating,
            onDismiss = { deactivatingStaff = null },
            onConfirm = {
                viewModel.deactivateUser(staff)
                deactivatingStaff = null
            }
        )
    }
}
