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
fun OwnerEditUserScreen(
    name: String,
    role: String,
    userId: String,
    onDashboardClick: () -> Unit,
    onUserManagementClick: () -> Unit,
    onSecurityLogClick: () -> Unit,
    onChangePasswordClick: (String) -> Unit,
    onChangePinClick: (String) -> Unit,
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

    OwnerDashboardShell(
        userName = name,
        role = role,
        activeDestination = OwnerDestination.UserManagement,
        onDashboardClick = onDashboardClick,
        onUserManagementClick = onUserManagementClick,
        onSecurityLogClick = onSecurityLogClick,
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
            onChangePasswordClick = { onChangePasswordClick(userId) },
            onChangePinClick = { onChangePinClick(userId) },
            onRetry = { viewModel.loadUser(userId) },
            onCancel = onUserManagementClick,
            onSubmit = viewModel::submit
        )
    }
}
