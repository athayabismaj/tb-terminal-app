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
import com.tbterminal.app.ui.audit.AdminOperationalAuditRoute
import com.tbterminal.app.ui.audit.AdminOperationalAuditViewModel
import com.tbterminal.app.ui.auth.AuthViewModel
import com.tbterminal.app.ui.auth.LoginScreen
import com.tbterminal.app.ui.auth.PinScreen
import com.tbterminal.app.ui.backup.BackupRestoreRoute
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
import com.tbterminal.app.ui.offlinereports.OfflineReportRoute
import com.tbterminal.app.ui.payables.AdminSupplierDebtScreen
import com.tbterminal.app.ui.pricemanagement.AdminPriceManagementScreen
import com.tbterminal.app.ui.products.AdminProductCategoriesScreen
import com.tbterminal.app.ui.products.AdminProductDetailScreen
import com.tbterminal.app.ui.products.AdminProductFormScreen
import com.tbterminal.app.ui.products.AdminProductListScreen
import com.tbterminal.app.ui.products.AdminProductUnitsScreen
import com.tbterminal.app.ui.receivables.AdminReceivableScreen
import com.tbterminal.app.ui.reports.AdminReportsRoute
import com.tbterminal.app.ui.salestransactions.AdminReceiptDetailScreen
import com.tbterminal.app.ui.salestransactions.AdminTransactionHistoryScreen
import com.tbterminal.app.ui.security.OwnerSecurityLogScreen
import com.tbterminal.app.ui.settings.CashierProfileScreen
import com.tbterminal.app.ui.settings.CashierSettingsScreen
import com.tbterminal.app.ui.stockopname.AdminStockOpnameFormScreen
import com.tbterminal.app.ui.stockopname.AdminStockOpnameScreen
import com.tbterminal.app.ui.sync.SyncCenterRoute
import com.tbterminal.app.ui.users.OwnerAddUserScreen
import com.tbterminal.app.ui.users.OwnerEditUserScreen
import com.tbterminal.app.ui.users.OwnerUserCredentialScreen
import com.tbterminal.app.ui.users.OwnerUserManagementScreen
import com.tbterminal.app.ui.users.UserCredentialMode
internal fun NavGraphBuilder.auditGraph(navController: NavHostController, sessionManager: SessionManager, appContainer: AppContainer) {
        composable(AppRoute.Reports.route) {
            AdminReportsNavRoute(
                sessionManager = sessionManager,
                navController = navController,
                appContainer = appContainer
            )
        }

        composable(AppRoute.OfflineReports.route) {
            OfflineReportsNavRoute(
                sessionManager = sessionManager,
                navController = navController,
                appContainer = appContainer
            )
        }

                                composable(AppRoute.PriceManagement.route) {
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

                AdminPriceManagementScreen(
                    name = sessionUser.name,
                    role = sessionUser.role,
                    inventoryRepository = appContainer.inventoryRepository,
                    onDashboardClick = {
                        navController.navigate(AppRoute.Dashboard.route) { launchSingleTop = true }
                    },
                    onProductsClick = {
                        navController.navigate(AppRoute.Products.route) { launchSingleTop = true }
                    },
                    onAddProductClick = {
                        navController.navigate(AppRoute.AddProduct.route) { launchSingleTop = true }
                    },
                    onProductCategoriesClick = {
                        navController.navigate(AppRoute.ProductCategories.route) { launchSingleTop = true }
                    },
                    onProductUnitsClick = {
                        navController.navigate(AppRoute.ProductUnits.route) { launchSingleTop = true }
                    },
                    onCashReconciliationClick = {
                        navController.navigate(AppRoute.CashReconciliation.route) { launchSingleTop = true }
                    },
                    onSalesTransactionsClick = {
                        navController.navigate(AppRoute.SalesTransactions.route) { launchSingleTop = true }
                    },
                    onReportsClick = {
                        navController.navigate(AppRoute.Reports.route) { launchSingleTop = true }
                    },
                    onPriceManagementClick = {
                        navController.navigate(AppRoute.PriceManagement.route) { launchSingleTop = true }
                    },
                    onStockOpnameClick = {
                        navController.navigate(AppRoute.StockOpname.route) { launchSingleTop = true }
                    },
                    onStockOpnameFormClick = {
                        navController.navigate(AppRoute.StockOpnameForm.createRoute()) { launchSingleTop = true }
                    },
                    onIncomingGoodsClick = {
                        navController.navigate(AppRoute.IncomingGoods.route) { launchSingleTop = true }
                    },
                    onIncomingGoodsFormClick = {
                        navController.navigate(AppRoute.IncomingGoodsForm.createRoute()) { launchSingleTop = true }
                    },
                    onSupplierDebtsClick = {
                        navController.navigate(AppRoute.SupplierDebts.route) { launchSingleTop = true }
                    },
                    onReceivablesClick = {
                        navController.navigate(AppRoute.Receivables.route) { launchSingleTop = true }
                    },
                    onCustomersClick = {
                        navController.navigate(AppRoute.Customers.route) { launchSingleTop = true }
                    },
                    onOperationalAuditClick = {
                        navController.navigate(AppRoute.OperationalAudit.route) { launchSingleTop = true }
                    },
                    onProfileClick = {
                        navController.navigate(AppRoute.AdminProfile.route) { launchSingleTop = true }
                    },
                    onSettingsClick = {
                        navController.navigate(AppRoute.AdminSettings.route) { launchSingleTop = true }
                    },
                    onLogout = logout
                )
            }
        }

        composable(AppRoute.OperationalAudit.route) {
            AdminOperationalAuditNavRoute(
                sessionManager = sessionManager,
                navController = navController,
                appContainer = appContainer
            )
        }

        composable(AppRoute.SyncCenter.route) {
            SyncCenterNavRoute(
                sessionManager = sessionManager,
                navController = navController,
                appContainer = appContainer
            )
        }

        composable(AppRoute.BackupRestore.route) {
            BackupRestoreNavRoute(
                sessionManager = sessionManager,
                navController = navController,
                appContainer = appContainer
            )
        }

}

@Composable
private fun OfflineReportsNavRoute(
    sessionManager: SessionManager,
    navController: NavHostController,
    appContainer: AppContainer
) {
    val sessionUser = sessionManager.readSessionUser()

    if (sessionUser == null) {
        LaunchedEffect(Unit) {
            sessionManager.logout()
            navController.navigate(AppRoute.Login.route) {
                popUpTo(0)
                launchSingleTop = true
            }
        }
        return
    }

    if (sessionUser.role.equals("kasir", ignoreCase = true)) {
        LaunchedEffect(Unit) {
            navController.navigate(AppRoute.Dashboard.route) {
                launchSingleTop = true
            }
        }
        return
    }

    OfflineReportRoute(
        name = sessionUser.name,
        role = sessionUser.role,
        offlineReportRepository = appContainer.offlineReportRepository,
        onDashboardClick = { navController.navigateAdminSingleTop(AppRoute.Dashboard.route) },
        onProductsClick = { navController.navigateAdminSingleTop(AppRoute.Products.route) },
        onAddProductClick = { navController.navigateAdminSingleTop(AppRoute.AddProduct.route) },
        onProductCategoriesClick = { navController.navigateAdminSingleTop(AppRoute.ProductCategories.route) },
        onProductUnitsClick = { navController.navigateAdminSingleTop(AppRoute.ProductUnits.route) },
        onCashReconciliationClick = { navController.navigateAdminSingleTop(AppRoute.CashReconciliation.route) },
        onSalesTransactionsClick = { navController.navigateAdminSingleTop(AppRoute.SalesTransactions.route) },
        onReportsClick = { navController.navigateAdminSingleTop(AppRoute.Reports.route) },
        onLocalReportsClick = { navController.navigateAdminSingleTop(AppRoute.OfflineReports.route) },
        onSyncCenterClick = { navController.navigateAdminSingleTop(AppRoute.SyncCenter.route) },
        onPriceManagementClick = { navController.navigateAdminSingleTop(AppRoute.PriceManagement.route) },
        onStockOpnameClick = { navController.navigateAdminSingleTop(AppRoute.StockOpname.route) },
        onStockOpnameFormClick = { navController.navigateAdminSingleTop(AppRoute.StockOpnameForm.createRoute()) },
        onIncomingGoodsClick = { navController.navigateAdminSingleTop(AppRoute.IncomingGoods.route) },
        onIncomingGoodsFormClick = { navController.navigateAdminSingleTop(AppRoute.IncomingGoodsForm.createRoute()) },
        onSupplierDebtsClick = { navController.navigateAdminSingleTop(AppRoute.SupplierDebts.route) },
        onReceivablesClick = { navController.navigateAdminSingleTop(AppRoute.Receivables.route) },
        onCustomersClick = { navController.navigateAdminSingleTop(AppRoute.Customers.route) },
        onOperationalAuditClick = { navController.navigateAdminSingleTop(AppRoute.OperationalAudit.route) },
        onUserManagementClick = { navController.navigateAdminSingleTop(AppRoute.UserManagement.route) },
        onSecurityLogClick = { navController.navigateAdminSingleTop(AppRoute.SecurityLog.route) },
        onProfileClick = { navController.navigateAdminSingleTop(AppRoute.AdminProfile.route) },
        onSettingsClick = { navController.navigateAdminSingleTop(AppRoute.AdminSettings.route) },
        onLogout = {
            sessionManager.logout()
            navController.navigate(AppRoute.Login.route) {
                popUpTo(0)
                launchSingleTop = true
            }
        }
    )
}

@Composable
private fun AdminReportsNavRoute(
    sessionManager: SessionManager,
    navController: NavHostController,
    appContainer: AppContainer
) {
    val sessionUser = sessionManager.readSessionUser()

    if (sessionUser == null) {
        LaunchedEffect(Unit) {
            sessionManager.logout()
            navController.navigate(AppRoute.Login.route) {
                popUpTo(0)
                launchSingleTop = true
            }
        }
        return
    }

    AdminReportsRoute(
        name = sessionUser.name,
        role = sessionUser.role,
        analyticsRepository = appContainer.analyticsRepository,
        cashReconciliationRepository = appContainer.cashReconciliationRepository,
        onDashboardClick = { navController.navigateAdminSingleTop(AppRoute.Dashboard.route) },
        onProductsClick = { navController.navigateAdminSingleTop(AppRoute.Products.route) },
        onAddProductClick = { navController.navigateAdminSingleTop(AppRoute.AddProduct.route) },
        onProductCategoriesClick = { navController.navigateAdminSingleTop(AppRoute.ProductCategories.route) },
        onProductUnitsClick = { navController.navigateAdminSingleTop(AppRoute.ProductUnits.route) },
        onCashReconciliationClick = { navController.navigateAdminSingleTop(AppRoute.CashReconciliation.route) },
        onSalesTransactionsClick = { navController.navigateAdminSingleTop(AppRoute.SalesTransactions.route) },
        onReportsClick = { navController.navigateAdminSingleTop(AppRoute.Reports.route) },
        onPriceManagementClick = { navController.navigateAdminSingleTop(AppRoute.PriceManagement.route) },
        onStockOpnameClick = { navController.navigateAdminSingleTop(AppRoute.StockOpname.route) },
        onStockOpnameFormClick = { navController.navigateAdminSingleTop(AppRoute.StockOpnameForm.createRoute()) },
        onIncomingGoodsClick = { navController.navigateAdminSingleTop(AppRoute.IncomingGoods.route) },
        onIncomingGoodsFormClick = { navController.navigateAdminSingleTop(AppRoute.IncomingGoodsForm.createRoute()) },
        onSupplierDebtsClick = { navController.navigateAdminSingleTop(AppRoute.SupplierDebts.route) },
        onReceivablesClick = { navController.navigateAdminSingleTop(AppRoute.Receivables.route) },
        onCustomersClick = { navController.navigateAdminSingleTop(AppRoute.Customers.route) },
        onOperationalAuditClick = { navController.navigateAdminSingleTop(AppRoute.OperationalAudit.route) },
        onUserManagementClick = { navController.navigateAdminSingleTop(AppRoute.UserManagement.route) },
        onSecurityLogClick = { navController.navigateAdminSingleTop(AppRoute.SecurityLog.route) },
        onProfileClick = { navController.navigateAdminSingleTop(AppRoute.AdminProfile.route) },
        onSettingsClick = { navController.navigateAdminSingleTop(AppRoute.AdminSettings.route) },
        onLogout = {
            sessionManager.logout()
            navController.navigate(AppRoute.Login.route) {
                popUpTo(0)
                launchSingleTop = true
            }
        }
    )
}

@Composable
private fun AdminOperationalAuditNavRoute(
    sessionManager: SessionManager,
    navController: NavHostController,
    appContainer: AppContainer
) {
    val sessionUser = sessionManager.readSessionUser()

    if (sessionUser == null) {
        LaunchedEffect(Unit) {
            sessionManager.logout()
            navController.navigate(AppRoute.Login.route) {
                popUpTo(0)
                launchSingleTop = true
            }
        }
        return
    }

    val auditViewModel: AdminOperationalAuditViewModel = viewModel(
        factory = AdminOperationalAuditViewModel.factory(appContainer.systemRepository)
    )

    AdminOperationalAuditRoute(
        name = sessionUser.name,
        role = sessionUser.role,
        viewModel = auditViewModel,
        onDashboardClick = { navController.navigateAdminSingleTop(AppRoute.Dashboard.route) },
        onProductsClick = { navController.navigateAdminSingleTop(AppRoute.Products.route) },
        onAddProductClick = { navController.navigateAdminSingleTop(AppRoute.AddProduct.route) },
        onProductCategoriesClick = { navController.navigateAdminSingleTop(AppRoute.ProductCategories.route) },
        onProductUnitsClick = { navController.navigateAdminSingleTop(AppRoute.ProductUnits.route) },
        onCashReconciliationClick = { navController.navigateAdminSingleTop(AppRoute.CashReconciliation.route) },
        onSalesTransactionsClick = { navController.navigateAdminSingleTop(AppRoute.SalesTransactions.route) },
        onReportsClick = { navController.navigateAdminSingleTop(AppRoute.Reports.route) },
        onPriceManagementClick = { navController.navigateAdminSingleTop(AppRoute.PriceManagement.route) },
        onStockOpnameClick = { navController.navigateAdminSingleTop(AppRoute.StockOpname.route) },
        onStockOpnameFormClick = { navController.navigateAdminSingleTop(AppRoute.StockOpnameForm.createRoute()) },
        onIncomingGoodsClick = { navController.navigateAdminSingleTop(AppRoute.IncomingGoods.route) },
        onIncomingGoodsFormClick = { navController.navigateAdminSingleTop(AppRoute.IncomingGoodsForm.createRoute()) },
        onSupplierDebtsClick = { navController.navigateAdminSingleTop(AppRoute.SupplierDebts.route) },
        onReceivablesClick = { navController.navigateAdminSingleTop(AppRoute.Receivables.route) },
        onCustomersClick = { navController.navigateAdminSingleTop(AppRoute.Customers.route) },
        onOperationalAuditClick = { navController.navigateAdminSingleTop(AppRoute.OperationalAudit.route) },
        onUserManagementClick = { navController.navigateAdminSingleTop(AppRoute.UserManagement.route) },
        onSecurityLogClick = { navController.navigateAdminSingleTop(AppRoute.SecurityLog.route) },
        onProfileClick = { navController.navigateAdminSingleTop(AppRoute.AdminProfile.route) },
        onSettingsClick = { navController.navigateAdminSingleTop(AppRoute.AdminSettings.route) },
        onLogout = {
            sessionManager.logout()
            navController.navigate(AppRoute.Login.route) {
                popUpTo(0)
                launchSingleTop = true
            }
        }
    )
}

private fun NavHostController.navigateAdminSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}

@Composable
private fun BackupRestoreNavRoute(
    sessionManager: SessionManager,
    navController: NavHostController,
    appContainer: AppContainer
) {
    val sessionUser = sessionManager.readSessionUser()

    if (sessionUser == null) {
        LaunchedEffect(Unit) {
            sessionManager.logout()
            navController.navigate(AppRoute.Login.route) {
                popUpTo(0)
                launchSingleTop = true
            }
        }
        return
    }

    if (sessionUser.role.equals("kasir", ignoreCase = true)) {
        LaunchedEffect(Unit) {
            navController.navigate(AppRoute.Dashboard.route) {
                launchSingleTop = true
            }
        }
        return
    }

    BackupRestoreRoute(
        name = sessionUser.name,
        role = sessionUser.role,
        localBackupRepository = appContainer.localBackupRepository,
        serverBackupRepository = appContainer.serverBackupRepository,
        onDashboardClick = { navController.navigateAdminSingleTop(AppRoute.Dashboard.route) },
        onProductsClick = { navController.navigateAdminSingleTop(AppRoute.Products.route) },
        onAddProductClick = { navController.navigateAdminSingleTop(AppRoute.AddProduct.route) },
        onProductCategoriesClick = { navController.navigateAdminSingleTop(AppRoute.ProductCategories.route) },
        onProductUnitsClick = { navController.navigateAdminSingleTop(AppRoute.ProductUnits.route) },
        onCashReconciliationClick = { navController.navigateAdminSingleTop(AppRoute.CashReconciliation.route) },
        onSalesTransactionsClick = { navController.navigateAdminSingleTop(AppRoute.SalesTransactions.route) },
        onReportsClick = { navController.navigateAdminSingleTop(AppRoute.Reports.route) },
        onLocalReportsClick = { navController.navigateAdminSingleTop(AppRoute.OfflineReports.route) },
        onSyncCenterClick = { navController.navigateAdminSingleTop(AppRoute.SyncCenter.route) },
        onBackupRestoreClick = { navController.navigateAdminSingleTop(AppRoute.BackupRestore.route) },
        onPriceManagementClick = { navController.navigateAdminSingleTop(AppRoute.PriceManagement.route) },
        onStockOpnameClick = { navController.navigateAdminSingleTop(AppRoute.StockOpname.route) },
        onStockOpnameFormClick = { navController.navigateAdminSingleTop(AppRoute.StockOpnameForm.createRoute()) },
        onIncomingGoodsClick = { navController.navigateAdminSingleTop(AppRoute.IncomingGoods.route) },
        onIncomingGoodsFormClick = { navController.navigateAdminSingleTop(AppRoute.IncomingGoodsForm.createRoute()) },
        onSupplierDebtsClick = { navController.navigateAdminSingleTop(AppRoute.SupplierDebts.route) },
        onReceivablesClick = { navController.navigateAdminSingleTop(AppRoute.Receivables.route) },
        onCustomersClick = { navController.navigateAdminSingleTop(AppRoute.Customers.route) },
        onOperationalAuditClick = { navController.navigateAdminSingleTop(AppRoute.OperationalAudit.route) },
        onUserManagementClick = { navController.navigateAdminSingleTop(AppRoute.UserManagement.route) },
        onSecurityLogClick = { navController.navigateAdminSingleTop(AppRoute.SecurityLog.route) },
        onProfileClick = { navController.navigateAdminSingleTop(AppRoute.AdminProfile.route) },
        onSettingsClick = { navController.navigateAdminSingleTop(AppRoute.AdminSettings.route) },
        onLogout = {
            sessionManager.logout()
            navController.navigate(AppRoute.Login.route) {
                popUpTo(0)
                launchSingleTop = true
            }
        }
    )
}

@Composable
private fun SyncCenterNavRoute(
    sessionManager: SessionManager,
    navController: NavHostController,
    appContainer: AppContainer
) {
    val sessionUser = sessionManager.readSessionUser()

    if (sessionUser == null) {
        LaunchedEffect(Unit) {
            sessionManager.logout()
            navController.navigate(AppRoute.Login.route) {
                popUpTo(0)
                launchSingleTop = true
            }
        }
        return
    }

    if (sessionUser.role.equals("kasir", ignoreCase = true)) {
        LaunchedEffect(Unit) {
            navController.navigate(AppRoute.Dashboard.route) {
                launchSingleTop = true
            }
        }
        return
    }

    SyncCenterRoute(
        name = sessionUser.name,
        role = sessionUser.role,
        syncMonitoringRepository = appContainer.syncMonitoringRepository,
        onDashboardClick = { navController.navigateAdminSingleTop(AppRoute.Dashboard.route) },
        onProductsClick = { navController.navigateAdminSingleTop(AppRoute.Products.route) },
        onAddProductClick = { navController.navigateAdminSingleTop(AppRoute.AddProduct.route) },
        onProductCategoriesClick = { navController.navigateAdminSingleTop(AppRoute.ProductCategories.route) },
        onProductUnitsClick = { navController.navigateAdminSingleTop(AppRoute.ProductUnits.route) },
        onCashReconciliationClick = { navController.navigateAdminSingleTop(AppRoute.CashReconciliation.route) },
        onSalesTransactionsClick = { navController.navigateAdminSingleTop(AppRoute.SalesTransactions.route) },
        onReportsClick = { navController.navigateAdminSingleTop(AppRoute.Reports.route) },
        onSyncCenterClick = { navController.navigateAdminSingleTop(AppRoute.SyncCenter.route) },
        onPriceManagementClick = { navController.navigateAdminSingleTop(AppRoute.PriceManagement.route) },
        onStockOpnameClick = { navController.navigateAdminSingleTop(AppRoute.StockOpname.route) },
        onStockOpnameFormClick = { navController.navigateAdminSingleTop(AppRoute.StockOpnameForm.createRoute()) },
        onIncomingGoodsClick = { navController.navigateAdminSingleTop(AppRoute.IncomingGoods.route) },
        onIncomingGoodsFormClick = { navController.navigateAdminSingleTop(AppRoute.IncomingGoodsForm.createRoute()) },
        onSupplierDebtsClick = { navController.navigateAdminSingleTop(AppRoute.SupplierDebts.route) },
        onReceivablesClick = { navController.navigateAdminSingleTop(AppRoute.Receivables.route) },
        onCustomersClick = { navController.navigateAdminSingleTop(AppRoute.Customers.route) },
        onOperationalAuditClick = { navController.navigateAdminSingleTop(AppRoute.OperationalAudit.route) },
        onUserManagementClick = { navController.navigateAdminSingleTop(AppRoute.UserManagement.route) },
        onSecurityLogClick = { navController.navigateAdminSingleTop(AppRoute.SecurityLog.route) },
        onProfileClick = { navController.navigateAdminSingleTop(AppRoute.AdminProfile.route) },
        onSettingsClick = { navController.navigateAdminSingleTop(AppRoute.AdminSettings.route) },
        onLogout = {
            sessionManager.logout()
            navController.navigate(AppRoute.Login.route) {
                popUpTo(0)
                launchSingleTop = true
            }
        }
    )
}
