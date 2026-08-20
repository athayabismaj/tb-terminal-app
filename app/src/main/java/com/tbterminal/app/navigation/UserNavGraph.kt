package com.tbterminal.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
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
import com.tbterminal.app.ui.checkout.CashierCartScreen
import com.tbterminal.app.ui.checkout.CashierPosScreen
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
internal fun NavGraphBuilder.userGraph(navController: NavHostController, sessionManager: SessionManager, appContainer: AppContainer) {
        composable(AppRoute.UserManagement.route) {
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
                OwnerUserManagementScreen(
                    name = sessionUser.name,
                    role = sessionUser.role,
                    userRepository = appContainer.userRepository,
                    onDashboardClick = {
                        navController.navigate(AppRoute.Dashboard.route) {
                            popUpTo(AppRoute.Dashboard.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onAddUserClick = {
                        navController.navigate(AppRoute.AddUser.route) {
                            launchSingleTop = true
                        }
                    },
                    onEditUserClick = { userId ->
                        navController.navigate(AppRoute.EditUser.createRoute(userId)) {
                            launchSingleTop = true
                        }
                    },
                    onChangePasswordClick = { userId ->
                        navController.navigate(AppRoute.ChangeUserPassword.createRoute(userId)) {
                            launchSingleTop = true
                        }
                    },
                    onChangePinClick = { userId ->
                        navController.navigate(AppRoute.ChangeUserPin.createRoute(userId)) {
                            launchSingleTop = true
                        }
                    },
                    onSecurityLogClick = {
                        navController.navigate(AppRoute.SecurityLog.route) {
                            launchSingleTop = true
                        }
                    },
                    onReportsClick = { navController.navigateOwnerModule(AppRoute.Reports.route) },
                    onSyncCenterClick = { navController.navigateOwnerModule(AppRoute.SyncCenter.route) },
                    onStockReportClick = { navController.navigateOwnerModule(AppRoute.StockReport.route) },
                    onReceivablesClick = { navController.navigateOwnerModule(AppRoute.Receivables.route) },
                    onSupplierDebtsClick = { navController.navigateOwnerModule(AppRoute.SupplierDebts.route) },
                    onCashReconciliationClick = { navController.navigateOwnerModule(AppRoute.CashReconciliation.route) },
                    onOperationalAuditClick = { navController.navigateOwnerModule(AppRoute.OperationalAudit.route) },
                    onSettingsClick = { navController.navigateOwnerModule(AppRoute.AdminSettings.route) },
                    onLogout = {
                        sessionManager.logout()
                        navController.navigate(AppRoute.Login.route) {
                            popUpTo(0)
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        composable(
            route = AppRoute.EditUser.route,
            arguments = listOf(
                navArgument(AppRoute.EditUser.USER_ID_ARG) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val sessionUser = sessionManager.readSessionUser()
            val userId = backStackEntry.arguments?.getString(AppRoute.EditUser.USER_ID_ARG)

            if (sessionUser == null || userId.isNullOrBlank()) {
                LaunchedEffect(Unit) {
                    sessionManager.logout()
                    navController.navigate(AppRoute.Login.route) {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                }
            } else {
                OwnerEditUserScreen(
                    name = sessionUser.name,
                    role = sessionUser.role,
                    userId = userId,
                    userRepository = appContainer.userRepository,
                    onDashboardClick = {
                        navController.navigate(AppRoute.Dashboard.route) {
                            popUpTo(AppRoute.Dashboard.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onUserManagementClick = {
                        navController.navigate(AppRoute.UserManagement.route) {
                            popUpTo(AppRoute.UserManagement.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onSecurityLogClick = {
                        navController.navigate(AppRoute.SecurityLog.route) {
                            launchSingleTop = true
                        }
                    },
                    onReportsClick = { navController.navigateOwnerModule(AppRoute.Reports.route) },
                    onSyncCenterClick = { navController.navigateOwnerModule(AppRoute.SyncCenter.route) },
                    onStockReportClick = { navController.navigateOwnerModule(AppRoute.StockReport.route) },
                    onReceivablesClick = { navController.navigateOwnerModule(AppRoute.Receivables.route) },
                    onSupplierDebtsClick = { navController.navigateOwnerModule(AppRoute.SupplierDebts.route) },
                    onCashReconciliationClick = { navController.navigateOwnerModule(AppRoute.CashReconciliation.route) },
                    onOperationalAuditClick = { navController.navigateOwnerModule(AppRoute.OperationalAudit.route) },
                    onSettingsClick = { navController.navigateOwnerModule(AppRoute.AdminSettings.route) },
                    onChangePasswordClick = { targetUserId ->
                        navController.navigate(AppRoute.ChangeUserPassword.createRoute(targetUserId)) {
                            launchSingleTop = true
                        }
                    },
                    onChangePinClick = { targetUserId ->
                        navController.navigate(AppRoute.ChangeUserPin.createRoute(targetUserId)) {
                            launchSingleTop = true
                        }
                    },
                    onLogout = {
                        sessionManager.logout()
                        navController.navigate(AppRoute.Login.route) {
                            popUpTo(0)
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        composable(
            route = AppRoute.ChangeUserPassword.route,
            arguments = listOf(
                navArgument(AppRoute.ChangeUserPassword.USER_ID_ARG) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val sessionUser = sessionManager.readSessionUser()
            val userId = backStackEntry.arguments?.getString(AppRoute.ChangeUserPassword.USER_ID_ARG)

            if (sessionUser == null || userId.isNullOrBlank()) {
                LaunchedEffect(Unit) {
                    sessionManager.logout()
                    navController.navigate(AppRoute.Login.route) {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                }
            } else {
                OwnerUserCredentialScreen(
                    name = sessionUser.name,
                    role = sessionUser.role,
                    userId = userId,
                    mode = UserCredentialMode.Password,
                    userRepository = appContainer.userRepository,
                    onDashboardClick = {
                        navController.navigate(AppRoute.Dashboard.route) {
                            popUpTo(AppRoute.Dashboard.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onEditUserClick = { targetUserId ->
                        navController.navigate(AppRoute.EditUser.createRoute(targetUserId)) {
                            popUpTo(AppRoute.UserManagement.route)
                            launchSingleTop = true
                        }
                    },
                    onUserManagementClick = {
                        navController.navigate(AppRoute.UserManagement.route) {
                            popUpTo(AppRoute.UserManagement.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onSecurityLogClick = {
                        navController.navigate(AppRoute.SecurityLog.route) {
                            launchSingleTop = true
                        }
                    },
                    onReportsClick = { navController.navigateOwnerModule(AppRoute.Reports.route) },
                    onSyncCenterClick = { navController.navigateOwnerModule(AppRoute.SyncCenter.route) },
                    onStockReportClick = { navController.navigateOwnerModule(AppRoute.StockReport.route) },
                    onReceivablesClick = { navController.navigateOwnerModule(AppRoute.Receivables.route) },
                    onSupplierDebtsClick = { navController.navigateOwnerModule(AppRoute.SupplierDebts.route) },
                    onCashReconciliationClick = { navController.navigateOwnerModule(AppRoute.CashReconciliation.route) },
                    onOperationalAuditClick = { navController.navigateOwnerModule(AppRoute.OperationalAudit.route) },
                    onSettingsClick = { navController.navigateOwnerModule(AppRoute.AdminSettings.route) },
                    onLogout = {
                        sessionManager.logout()
                        navController.navigate(AppRoute.Login.route) {
                            popUpTo(0)
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        composable(
            route = AppRoute.ChangeUserPin.route,
            arguments = listOf(
                navArgument(AppRoute.ChangeUserPin.USER_ID_ARG) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val sessionUser = sessionManager.readSessionUser()
            val userId = backStackEntry.arguments?.getString(AppRoute.ChangeUserPin.USER_ID_ARG)

            if (sessionUser == null || userId.isNullOrBlank()) {
                LaunchedEffect(Unit) {
                    sessionManager.logout()
                    navController.navigate(AppRoute.Login.route) {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                }
            } else {
                OwnerUserCredentialScreen(
                    name = sessionUser.name,
                    role = sessionUser.role,
                    userId = userId,
                    mode = UserCredentialMode.Pin,
                    userRepository = appContainer.userRepository,
                    onDashboardClick = {
                        navController.navigate(AppRoute.Dashboard.route) {
                            popUpTo(AppRoute.Dashboard.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onEditUserClick = { targetUserId ->
                        navController.navigate(AppRoute.EditUser.createRoute(targetUserId)) {
                            popUpTo(AppRoute.UserManagement.route)
                            launchSingleTop = true
                        }
                    },
                    onUserManagementClick = {
                        navController.navigate(AppRoute.UserManagement.route) {
                            popUpTo(AppRoute.UserManagement.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onSecurityLogClick = {
                        navController.navigate(AppRoute.SecurityLog.route) {
                            launchSingleTop = true
                        }
                    },
                    onReportsClick = { navController.navigateOwnerModule(AppRoute.Reports.route) },
                    onSyncCenterClick = { navController.navigateOwnerModule(AppRoute.SyncCenter.route) },
                    onStockReportClick = { navController.navigateOwnerModule(AppRoute.StockReport.route) },
                    onReceivablesClick = { navController.navigateOwnerModule(AppRoute.Receivables.route) },
                    onSupplierDebtsClick = { navController.navigateOwnerModule(AppRoute.SupplierDebts.route) },
                    onCashReconciliationClick = { navController.navigateOwnerModule(AppRoute.CashReconciliation.route) },
                    onOperationalAuditClick = { navController.navigateOwnerModule(AppRoute.OperationalAudit.route) },
                    onSettingsClick = { navController.navigateOwnerModule(AppRoute.AdminSettings.route) },
                    onLogout = {
                        sessionManager.logout()
                        navController.navigate(AppRoute.Login.route) {
                            popUpTo(0)
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        composable(AppRoute.SecurityLog.route) {
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
                OwnerSecurityLogScreen(
                    name = sessionUser.name,
                    role = sessionUser.role,
                    securityLogRepository = appContainer.securityLogRepository,
                    onDashboardClick = {
                        navController.navigate(AppRoute.Dashboard.route) {
                            popUpTo(AppRoute.Dashboard.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onUserManagementClick = {
                        navController.navigate(AppRoute.UserManagement.route) {
                            launchSingleTop = true
                        }
                    },
                    onReportsClick = { navController.navigateOwnerModule(AppRoute.Reports.route) },
                    onSyncCenterClick = { navController.navigateOwnerModule(AppRoute.SyncCenter.route) },
                    onStockReportClick = { navController.navigateOwnerModule(AppRoute.StockReport.route) },
                    onReceivablesClick = { navController.navigateOwnerModule(AppRoute.Receivables.route) },
                    onSupplierDebtsClick = { navController.navigateOwnerModule(AppRoute.SupplierDebts.route) },
                    onCashReconciliationClick = { navController.navigateOwnerModule(AppRoute.CashReconciliation.route) },
                    onOperationalAuditClick = { navController.navigateOwnerModule(AppRoute.OperationalAudit.route) },
                    onSettingsClick = { navController.navigateOwnerModule(AppRoute.AdminSettings.route) },
                    onLogout = {
                        sessionManager.logout()
                        navController.navigate(AppRoute.Login.route) {
                            popUpTo(0)
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        composable(AppRoute.AddUser.route) {
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
                OwnerAddUserScreen(
                    name = sessionUser.name,
                    role = sessionUser.role,
                    userRepository = appContainer.userRepository,
                    onDashboardClick = {
                        navController.navigate(AppRoute.Dashboard.route) {
                            popUpTo(AppRoute.Dashboard.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onUserManagementClick = {
                        navController.navigate(AppRoute.UserManagement.route) {
                            popUpTo(AppRoute.UserManagement.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onSecurityLogClick = {
                        navController.navigate(AppRoute.SecurityLog.route) {
                            launchSingleTop = true
                        }
                    },
                    onReportsClick = { navController.navigateOwnerModule(AppRoute.Reports.route) },
                    onSyncCenterClick = { navController.navigateOwnerModule(AppRoute.SyncCenter.route) },
                    onStockReportClick = { navController.navigateOwnerModule(AppRoute.StockReport.route) },
                    onReceivablesClick = { navController.navigateOwnerModule(AppRoute.Receivables.route) },
                    onSupplierDebtsClick = { navController.navigateOwnerModule(AppRoute.SupplierDebts.route) },
                    onCashReconciliationClick = { navController.navigateOwnerModule(AppRoute.CashReconciliation.route) },
                    onOperationalAuditClick = { navController.navigateOwnerModule(AppRoute.OperationalAudit.route) },
                    onSettingsClick = { navController.navigateOwnerModule(AppRoute.AdminSettings.route) },
                    onLogout = {
                        sessionManager.logout()
                        navController.navigate(AppRoute.Login.route) {
                            popUpTo(0)
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
}

private fun NavHostController.navigateOwnerModule(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}
