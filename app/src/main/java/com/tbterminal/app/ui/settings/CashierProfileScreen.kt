package com.tbterminal.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.AuthRepository

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
    onEditProfile: (() -> Unit)? = null,
    editMode: Boolean = false,
    onBack: (() -> Unit)? = null,
    onProfileSaved: () -> Unit = {},
    onLogout: () -> Unit,
    profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(authRepository))
) {
    val uiState = profileViewModel.uiState.collectAsStateWithLifecycle().value
    val handleBack: () -> Unit = {
        if (onBack != null) {
            onBack()
        } else {
            onDashboardClick()
        }
    }

    if (editMode) {
        SharedEditProfileScreen(
            uiState = uiState,
            onSave = profileViewModel::updateProfile,
            onBack = handleBack,
            onSaved = {
                profileViewModel.consumeProfileUpdated()
                onProfileSaved()
            },
            onClearMessage = profileViewModel::clearMessage,
            fallbackName = userName,
            fallbackRole = role,
        )
    } else {
        SharedProfileScreen(
            uiState = uiState,
            onReload = profileViewModel::loadProfile,
            onChangePassword = profileViewModel::changePassword,
            onChangePin = profileViewModel::changePin,
            onClearMessage = profileViewModel::clearMessage,
            showHeader = true,
            pageTitle = "Profil Kasir",
            onBack = handleBack,
            onLogout = onLogout,
            onEditProfile = onEditProfile,
        )
    }
}
