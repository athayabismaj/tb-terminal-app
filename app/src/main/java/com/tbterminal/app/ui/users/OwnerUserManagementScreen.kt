package com.tbterminal.app.ui.users

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.UserRepository
import com.tbterminal.app.ui.dashboard.owner.OwnerDetailUserRoute
import com.tbterminal.app.ui.dashboard.owner.OwnerUserRoute

@Composable
fun OwnerUserManagementScreen(
    name: String,
    role: String,
    onDashboardClick: () -> Unit,
    onAddUserClick: () -> Unit,
    onEditUserClick: (String) -> Unit,
    onChangePasswordClick: (String) -> Unit,
    onChangePinClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onUserDetailClick: (String) -> Unit = {},
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
    var wasDisplayed by rememberSaveable { mutableStateOf(false) }

    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var showFilterPopup by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf(UserStatusFilter.All) }
    var roleFilter by rememberSaveable { mutableStateOf(UserRoleFilter.All) }
    var sortOrder by rememberSaveable { mutableStateOf(UserSortOrder.NAME_ASC) }

    LaunchedEffect(viewModel) {
        if (wasDisplayed) viewModel.refresh()
        wasDisplayed = true
    }

    OwnerUserRoute(
        uiState = uiState,
        isSearchExpanded = isSearchExpanded,
        searchQuery = searchQuery,
        statusFilter = statusFilter,
        roleFilter = roleFilter,
        sortOrder = sortOrder,
        onOpenSearch = { isSearchExpanded = true },
        onCloseSearch = {
            isSearchExpanded = false
            if (searchQuery.isNotEmpty()) {
                searchQuery = ""
            }
        },
        onSearchChanged = { searchQuery = it },
        onOpenFilter = { showFilterPopup = true },
        onAddUserClick = {
            viewModel.clearActionMessages()
            onAddUserClick()
        },
        onUserDetailClick = { userId ->
            viewModel.clearActionMessages()
            onUserDetailClick(userId)
        },
        onRefresh = viewModel::refresh,
        onBack = onBackClick,
        onDashboardClick = onDashboardClick,
        onDismissActionMessage = viewModel::clearActionMessages
    )

    if (showFilterPopup) {
        UserFilterSheet(
            roleFilter = roleFilter,
            statusFilter = statusFilter,
            onApplyFilter = { newRole, newStatus ->
                roleFilter = newRole
                statusFilter = newStatus
            },
            onDismiss = { showFilterPopup = false },
            sortOrder = sortOrder,
            onSortApply = { sortOrder = it }
        )
    }
}

@Composable
fun OwnerUserDetailRouteScreen(
    userId: String,
    userRepository: UserRepository,
    onDashboardClick: () -> Unit,
    onBackToUsers: () -> Unit,
    onEditUserClick: (String) -> Unit,
    viewModel: UserDetailViewModel = viewModel(
        factory = UserDetailViewModel.factory(userRepository, userId)
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var wasDisplayed by rememberSaveable(userId) { mutableStateOf(false) }

    LaunchedEffect(viewModel, userId) {
        if (wasDisplayed) viewModel.loadDetail()
        wasDisplayed = true
    }

    OwnerDetailUserRoute(
        uiState = uiState,
        onBack = onBackToUsers,
        onRetry = viewModel::loadDetail,
        onEditUserClick = onEditUserClick,
        onChangePassword = viewModel::changePassword,
        onChangePin = viewModel::changePin,
        onClearCredentialMessages = viewModel::clearCredentialMessages,
        onDeactivateConfirm = viewModel::deactivate,
        onClearActionError = viewModel::clearActionError,
        onDeactivatedReset = viewModel::resetDeactivatedState,
        onDashboardClick = onDashboardClick,
        onActivateClick = viewModel::activate,
        onUndoActivateClick = viewModel::undoActivate,
        onReactivatedReset = viewModel::resetReactivatedState
    )
}
