package com.tbterminal.app.ui.users

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.UserRepository
import com.tbterminal.app.ui.dashboard.owner.OwnerDashboardShell
import com.tbterminal.app.ui.dashboard.owner.OwnerDestination

@Composable
fun OwnerAddUserScreen(
    name: String,
    role: String,
    onDashboardClick: () -> Unit,
    onUserManagementClick: () -> Unit,
    onSecurityLogClick: () -> Unit,
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
                is AddUserEvent.UserCreated -> onUserManagementClick()
            }
        }
    }

    OwnerDashboardShell(
        userName = name,
        role = role,
        activeDestination = OwnerDestination.UserManagement,
        onDashboardClick = onDashboardClick,
        onUserManagementClick = onUserManagementClick,
        onSecurityLogClick = onSecurityLogClick,
        onLogout = onLogout
    ) { contentModifier ->
        AddUserContent(
            modifier = contentModifier,
            uiState = uiState,
            onFullNameChange = viewModel::updateFullName,
            onEmailChange = viewModel::updateEmail,
            onUsernameChange = viewModel::updateUsername,
            onPasswordChange = viewModel::updatePassword,
            onPinChange = viewModel::updatePin,
            onRoleChange = viewModel::selectRole,
            onRetryRoles = viewModel::loadRoles,
            onCancel = onUserManagementClick,
            onSubmit = viewModel::submit
        )
    }
}
