package com.tbterminal.app.ui.users

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.UserRepository
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.components.RefreshableContent

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
    val configuration = LocalConfiguration.current
    val isCompactScreen = configuration.screenWidthDp < 700

    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var showFilterPopup by rememberSaveable { mutableStateOf(false) }
    var showSummaryPopup by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf(UserStatusFilter.All) }
    var roleFilter by rememberSaveable { mutableStateOf(UserRoleFilter.All) }
    val isFiltered = statusFilter != UserStatusFilter.All || roleFilter != UserRoleFilter.All

    AdminDashboardShell(
        onProductsClick = {},
        userName = name,
        role = role,
        activeDestination = AdminDestination.UserManagement,
        topBarActions = {
            // Button 1: Search [ 🔍 ]
            IconButton(
                onClick = { isSearchExpanded = !isSearchExpanded },
                modifier = Modifier.size(36.dp).testTag("user-toggle-search")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Cari pengguna",
                    tint = if (isSearchExpanded || searchQuery.isNotEmpty()) Color(0xFF256B57) else Color(0xFF475569),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Button 2: Filter [ 🎛️ ]
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { showFilterPopup = true },
                    modifier = Modifier.size(36.dp).testTag("user-open-filter")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = "Filter pengguna",
                        tint = if (isFiltered) Color(0xFF256B57) else Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (isFiltered) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 6.dp)
                            .size(8.dp)
                            .background(Color(0xFFF59E0B), CircleShape)
                            .border(1.5.dp, Color.White, CircleShape)
                    )
                }
            }

            // Button 3: Ringkasan [ 📑 ]
            IconButton(
                onClick = { showSummaryPopup = true },
                modifier = Modifier.size(36.dp).testTag("user-open-summary")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = "Ringkasan pengguna",
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(20.dp)
                )
            }
        },
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
        pageTitle = "Pengguna & akses",
        onBack = onBackClick,
        showPageHeader = true,
        onLogout = onLogout
    ) { contentModifier ->
        RefreshableContent(
            isRefreshing = uiState.isLoading && uiState.staffMembers.isNotEmpty(),
            onRefresh = viewModel::refresh,
            modifier = contentModifier,
        ) {
            UserManagementContent(
                modifier = Modifier,
                uiState = uiState,
                searchQuery = searchQuery,
                onSearchQueryChanged = { searchQuery = it },
                statusFilter = statusFilter,
                onStatusFilterChanged = { statusFilter = it },
                roleFilter = roleFilter,
                onRoleFilterChanged = { roleFilter = it },
                isSearchExpanded = isSearchExpanded,
                onToggleSearch = { isSearchExpanded = !isSearchExpanded },
                showFilterPopup = showFilterPopup,
                onDismissFilter = { showFilterPopup = false },
                showSummaryPopup = showSummaryPopup,
                onDismissSummary = { showSummaryPopup = false },
                onAddUserClick = onAddUserClick,
                onRetry = viewModel::refresh,
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
                onDismissActionMessage = viewModel::clearActionMessages
            )
        }
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
