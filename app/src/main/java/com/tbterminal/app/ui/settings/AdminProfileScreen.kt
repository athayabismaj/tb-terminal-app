package com.tbterminal.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.AuthRepository
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator
import com.tbterminal.app.ui.dashboard.owner.DashboardNavDestination
import com.tbterminal.app.ui.dashboard.owner.OwnerProfileDetailRoute

@Suppress("UNUSED_PARAMETER")
@Composable
fun AdminProfileScreen(
    name: String,
    role: String,
    authRepository: AuthRepository,
    onDashboardClick: () -> Unit = {},
    onProductsClick: () -> Unit = {},
    onAddProductClick: () -> Unit = {},
    onProductCategoriesClick: () -> Unit = {},
    onProductUnitsClick: () -> Unit = {},
    onCashReconciliationClick: () -> Unit = {},
    onSalesTransactionsClick: () -> Unit = {},
    onReportsClick: () -> Unit = {},
    onPriceManagementClick: () -> Unit = {},
    onStockOpnameClick: () -> Unit = {},
    onStockOpnameFormClick: () -> Unit = {},
    onIncomingGoodsClick: () -> Unit = {},
    onIncomingGoodsFormClick: () -> Unit = {},
    onSupplierDebtsClick: () -> Unit = {},
    onReceivablesClick: () -> Unit = {},
    onCustomersClick: () -> Unit = {},
    onOperationalAuditClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onBackToPrevious: (() -> Unit)? = null,
    onEditProfile: (() -> Unit)? = null,
    editMode: Boolean = false,
    onProfileSaved: () -> Unit = {},
    onLogout: () -> Unit = {},
    profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.factory(authRepository))
) {
    val uiState = profileViewModel.uiState.collectAsStateWithLifecycle().value
    if (!editMode) {
        OwnerProfileDetailRoute(
            fallbackName = name,
            fallbackRole = role,
            profileViewModel = profileViewModel,
            onBack = { onBackToPrevious?.invoke() },
            onEditProfile = onEditProfile,
            onDashboardClick = onDashboardClick,
            onLogout = onLogout
        )
        return
    }

    val destinationNavigator = LocalAdminDestinationNavigator.current
    val handleBack = onBackToPrevious ?: onProfileClick
    SharedEditProfileScreen(
        uiState = uiState,
        onSave = profileViewModel::updateProfile,
        onBack = handleBack,
        onSaved = {
            profileViewModel.consumeProfileUpdated()
            onProfileSaved()
        },
        onClearMessage = profileViewModel::clearMessage,
        fallbackName = name,
        fallbackRole = role,
        onNavSelect = { dest ->
            when (dest) {
                DashboardNavDestination.BERANDA ->
                    destinationNavigator?.invoke(AdminDestination.Dashboard) ?: onDashboardClick()
                DashboardNavDestination.TRANSAKSI ->
                    destinationNavigator?.invoke(AdminDestination.TransactionsHub)
                DashboardNavDestination.KEUANGAN ->
                    destinationNavigator?.invoke(AdminDestination.FinanceHub)
                DashboardNavDestination.STOK ->
                    destinationNavigator?.invoke(AdminDestination.StockHub)
                DashboardNavDestination.MENU -> handleBack()
            }
        }
    )
}
