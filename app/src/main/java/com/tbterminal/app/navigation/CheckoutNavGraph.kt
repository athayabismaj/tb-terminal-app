package com.tbterminal.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.tbterminal.app.data.di.AppContainer
import com.tbterminal.app.data.session.SessionManager
import com.tbterminal.app.ui.admin.AdminBackofficePlaceholderScreen
import com.tbterminal.app.ui.audit.AdminOperationalAuditScreen
import com.tbterminal.app.ui.audit.AdminOperationalAuditViewModel
import com.tbterminal.app.ui.auth.AuthViewModel
import com.tbterminal.app.ui.auth.LoginScreen
import com.tbterminal.app.ui.auth.PinScreen
import com.tbterminal.app.ui.cash.AdminCashReconciliationScreen
import com.tbterminal.app.ui.cashier.session.CashierCloseShiftScreen
import com.tbterminal.app.ui.cashier.stock.CashierStockCheckScreen
import com.tbterminal.app.ui.cashier.transactions.CashierReceiptDetailScreen
import com.tbterminal.app.ui.cashier.transactions.CashierTransactionHistoryScreen
import com.tbterminal.app.ui.checkout.CashierCartRoute
import com.tbterminal.app.ui.checkout.CashierPosRoute
import com.tbterminal.app.ui.checkout.CheckoutViewModel
import com.tbterminal.app.ui.customers.AdminCustomerDetailScreen
import com.tbterminal.app.ui.customers.AdminCustomerFormScreen
import com.tbterminal.app.ui.customers.AdminCustomerListScreen
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardScreen
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.cashier.CashierDashboardScreen
import com.tbterminal.app.ui.dashboard.owner.OwnerDashboardScreen
import com.tbterminal.app.ui.incominggoods.AdminIncomingGoodsFormScreen
import com.tbterminal.app.ui.incominggoods.AdminIncomingGoodsScreen
import com.tbterminal.app.ui.payables.AdminSupplierDebtScreen
import com.tbterminal.app.ui.pricemanagement.AdminPriceManagementScreen
import com.tbterminal.app.ui.products.AdminProductCategoriesScreen
import com.tbterminal.app.ui.products.AdminProductDetailScreen
import com.tbterminal.app.ui.products.AdminProductFormScreen
import com.tbterminal.app.ui.products.AdminProductListScreen
import com.tbterminal.app.ui.products.AdminProductUnitsScreen
import com.tbterminal.app.ui.receivables.AdminReceivableScreen
import com.tbterminal.app.ui.reports.AdminReportsScreen
import com.tbterminal.app.ui.reports.AdminReportsViewModel
import com.tbterminal.app.ui.salestransactions.AdminReceiptDetailScreen
import com.tbterminal.app.ui.salestransactions.AdminTransactionHistoryScreen
import com.tbterminal.app.ui.security.OwnerSecurityLogScreen
import com.tbterminal.app.ui.settings.CashierProfileScreen
import com.tbterminal.app.ui.settings.CashierSettingsScreen
import com.tbterminal.app.ui.stockopname.AdminStockOpnameFormScreen
import com.tbterminal.app.ui.stockopname.AdminStockOpnameScreen
import com.tbterminal.app.ui.users.OwnerAddUserScreen
import com.tbterminal.app.ui.users.OwnerEditUserScreen
import com.tbterminal.app.ui.users.OwnerUserCredentialScreen
import com.tbterminal.app.ui.users.OwnerUserManagementScreen
import com.tbterminal.app.ui.users.UserCredentialMode
internal fun NavGraphBuilder.checkoutGraph(navController: NavHostController, sessionManager: SessionManager, appContainer: AppContainer) {
        composable(AppRoute.CashierPos.route) {
            val sessionUser = sessionManager.readSessionUser()

            if (sessionUser == null) {
                LaunchedEffect(Unit) {
                    sessionManager.logout()
                    navController.navigate(AppRoute.Login.route) {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                }
            } else {
                val logout: () -> Unit = {
                    sessionManager.logout()
                    navController.navigate(AppRoute.Login.route) {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                }
                val checkoutViewModel = cashierCheckoutViewModel(
                    navController = navController,
                    appContainer = appContainer
                )

                CashierPosRoute(
                    name = sessionUser.name,
                    role = sessionUser.role,
                    checkoutRepository = appContainer.checkoutRepository,
                    inventoryRepository = appContainer.inventoryRepository,
                    customerRepository = appContainer.customerRepository,
                    cashReconciliationRepository = appContainer.cashReconciliationRepository,
                    managerApprovalRepository = appContainer.managerApprovalRepository,
                    onDashboardClick = {
                        navController.navigate(AppRoute.Dashboard.route) {
                            launchSingleTop = true
                        }
                    },
                    onPosClick = {},
                    onNavigateToCart = {
                        navController.navigate(AppRoute.CashierCart.route) {
                            launchSingleTop = true
                        }
                    },
                    onCashSessionClick = {
                        navController.navigate(AppRoute.CashierCashSession.route) {
                            launchSingleTop = true
                        }
                    },
                    onTransactionHistoryClick = {
                        navController.navigate(AppRoute.CashierTransactionHistory.route) {
                            launchSingleTop = true
                        }
                    },
                    onStockCheckClick = {
                        navController.navigate(AppRoute.CashierStockCheck.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToReceipt = { receiptId ->
                        navController.navigate(AppRoute.CashierReceiptDetail.createRoute(receiptId)) {
                            launchSingleTop = true
                        }
                    },
                    onProfileClick = {
                        navController.navigate(AppRoute.CashierProfile.route) {
                            launchSingleTop = true
                        }
                    },
                    onSettingsClick = {
                        navController.navigate(AppRoute.CashierSettings.route) {
                            launchSingleTop = true
                        }
                    },
                    onLogout = logout,
                    viewModel = checkoutViewModel
                )
            }
        }

        composable(AppRoute.CashierCart.route) {
            val sessionUser = sessionManager.readSessionUser()

            if (sessionUser == null) {
                LaunchedEffect(Unit) {
                    sessionManager.logout()
                    navController.navigate(AppRoute.Login.route) {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                }
            } else {
                val logout: () -> Unit = {
                    sessionManager.logout()
                    navController.navigate(AppRoute.Login.route) {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                }

                CashierCartRoute(
                    name = sessionUser.name,
                    role = sessionUser.role,
                    checkoutRepository = appContainer.checkoutRepository,
                    inventoryRepository = appContainer.inventoryRepository,
                    customerRepository = appContainer.customerRepository,
                    cashReconciliationRepository = appContainer.cashReconciliationRepository,
                    managerApprovalRepository = appContainer.managerApprovalRepository,
                    viewModel = cashierCheckoutViewModel(
                        navController = navController,
                        appContainer = appContainer
                    ),
                    onBackToPos = {
                        navController.popBackStack()
                    },
                    onDashboardClick = {
                        navController.navigate(AppRoute.Dashboard.route) {
                            launchSingleTop = true
                        }
                    },
                    onPosClick = {
                        navController.popBackStack()
                    },
                    onCashSessionClick = {
                        navController.navigate(AppRoute.CashierCashSession.route) {
                            launchSingleTop = true
                        }
                    },
                    onTransactionHistoryClick = {
                        navController.navigate(AppRoute.CashierTransactionHistory.route) {
                            launchSingleTop = true
                        }
                    },
                    onStockCheckClick = {
                        navController.navigate(AppRoute.CashierStockCheck.route) {
                            launchSingleTop = true
                        }
                    },
                    onProfileClick = {
                        navController.navigate(AppRoute.CashierProfile.route) {
                            launchSingleTop = true
                        }
                    },
                    onSettingsClick = {
                        navController.navigate(AppRoute.CashierSettings.route) {
                            launchSingleTop = true
                        }
                    },
                    onLogout = logout,
                    onNavigateToReceipt = { receiptId ->
                        navController.navigate(AppRoute.CashierReceiptDetail.createRoute(receiptId)) {
                            popUpTo(AppRoute.CashierPos.route) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

}

@Composable
private fun cashierCheckoutViewModel(
    navController: NavHostController,
    appContainer: AppContainer
): CheckoutViewModel {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val cashierOwner = remember(currentBackStackEntry) {
        navController.getBackStackEntry(AppRoute.CashierPos.route)
    }

    return viewModel(
        viewModelStoreOwner = cashierOwner,
        factory = CheckoutViewModel.factory(
            checkoutRepository = appContainer.checkoutRepository,
            inventoryRepository = appContainer.inventoryRepository,
            customerRepository = appContainer.customerRepository,
            cashReconciliationRepository = appContainer.cashReconciliationRepository,
            cashSessionLocalDataSource = appContainer.cashSessionLocalDataSource,
            localCheckoutLookupService = appContainer.localCheckoutLookupService,
            localCheckoutService = appContainer.localCheckoutService,
            offlineStatusRepository = appContainer.offlineStatusRepository,
            offlineSyncScheduler = appContainer.offlineSyncScheduler
        )
    )
}
