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
fun OwnerUserCredentialScreen(
    name: String,
    role: String,
    userId: String,
    mode: UserCredentialMode,
    onDashboardClick: () -> Unit,
    onEditUserClick: (String) -> Unit,
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
    viewModel: UserCredentialViewModel = viewModel(
        factory = UserCredentialViewModel.factory(userRepository)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(userId) {
        viewModel.load(userId)
    }

    LaunchedEffect(viewModel.events, userId) {
        viewModel.events.collect { event ->
            when (event) {
                is UserCredentialEvent.CredentialUpdated -> onUserManagementClick()
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
        pageTitle = mode.pageTitle(),
        onBack = onUserManagementClick,
        showPageHeader = true,
        onLogout = onLogout
    ) { contentModifier ->
        UserCredentialContent(
            modifier = contentModifier,
            mode = mode,
            uiState = uiState,
            onCredentialChange = { value ->
                viewModel.updateCredential(
                    if (mode == UserCredentialMode.Pin) value.filter(Char::isDigit).take(PIN_LENGTH) else value
                )
            },
            onConfirmationChange = { value ->
                viewModel.updateConfirmation(
                    if (mode == UserCredentialMode.Pin) value.filter(Char::isDigit).take(PIN_LENGTH) else value
                )
            },
            onRetry = { viewModel.load(userId) },
            onCancel = onUserManagementClick,
            onSubmit = { viewModel.submit(mode) }
        )
    }
}

private const val PIN_LENGTH = 6
