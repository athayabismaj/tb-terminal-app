package com.tbterminal.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.AuthRepository
import com.tbterminal.app.ui.dashboard.cashier.CashierDashboardShell
import com.tbterminal.app.ui.dashboard.cashier.CashierDestination

@Composable
fun CashierProfileScreen(
    userName: String,
    role: String,
    authRepository: AuthRepository,
    onDashboardClick: () -> Unit = {},
    onPosClick: () -> Unit = {},
    onCashSessionClick: () -> Unit = {},
    onTransactionHistoryClick: () -> Unit = {},
    onStockCheckClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(authRepository))
) {
    val uiState = profileViewModel.uiState.collectAsStateWithLifecycle().value
    CashierDashboardShell(
        userName = userName,
        role = role,
        activeDestination = CashierDestination.Profile,
        onDashboardClick = onDashboardClick,
        onPosClick = onPosClick,
        onCashSessionClick = onCashSessionClick,
        onTransactionHistoryClick = onTransactionHistoryClick,
        onStockCheckClick = onStockCheckClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    ) { contentModifier ->
        SharedProfileScreen(
            uiState = uiState,
            onReload = profileViewModel::loadProfile,
            onChangePassword = profileViewModel::changePassword,
            onChangePin = profileViewModel::changePin,
            onClearMessage = profileViewModel::clearMessage,
            modifier = contentModifier
        )
    }
}
