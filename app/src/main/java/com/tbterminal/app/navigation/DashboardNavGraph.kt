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
import com.tbterminal.app.ui.cashdetail.AdminCashReconciliationDetailScreen
import com.tbterminal.app.ui.cashexpenses.AdminCashExpenseHistoryScreen
import com.tbterminal.app.ui.cashhistory.AdminCashSessionHistoryScreen
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
import com.tbterminal.app.ui.purchasehistory.AdminPurchaseHistoryScreen
import com.tbterminal.app.ui.receivablepayments.AdminReceivablePaymentHistoryScreen
import com.tbterminal.app.ui.receivables.AdminReceivableScreen
import com.tbterminal.app.ui.reports.AdminReportsScreen
import com.tbterminal.app.ui.reports.AdminReportsViewModel
import com.tbterminal.app.ui.salestransactions.AdminReceiptDetailScreen
import com.tbterminal.app.ui.salestransactions.AdminTransactionHistoryScreen
import com.tbterminal.app.ui.security.OwnerSecurityLogScreen
import com.tbterminal.app.ui.settings.CashierProfileScreen
import com.tbterminal.app.ui.settings.CashierSettingsScreen
import com.tbterminal.app.ui.settings.AdminProfileScreen
import com.tbterminal.app.ui.settings.AdminSettingsScreen
import com.tbterminal.app.ui.stockopname.AdminStockOpnameFormScreen
import com.tbterminal.app.ui.stockopname.AdminStockOpnameScreen
import com.tbterminal.app.ui.stockreport.AdminStockReportScreen
import com.tbterminal.app.ui.suppliers.AdminSupplierScreen
import com.tbterminal.app.ui.users.OwnerAddUserScreen
import com.tbterminal.app.ui.users.OwnerEditUserScreen
import com.tbterminal.app.ui.users.OwnerUserCredentialScreen
import com.tbterminal.app.ui.users.OwnerUserManagementScreen
import com.tbterminal.app.ui.users.UserCredentialMode
internal fun NavGraphBuilder.dashboardGraph(navController: NavHostController, sessionManager: SessionManager, appContainer: AppContainer) {
        composable(AppRoute.Dashboard.route) {
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

                when (sessionUser.role.normalizedRole()) {
                    "owner" -> {
                        OwnerDashboardScreen(
                            name = sessionUser.name,
                            role = sessionUser.role,
                            offlineDashboardRepository = appContainer.offlineDashboardRepository,
                            onReportsClick = {
                                navController.navigate(AppRoute.Reports.route) {
                                    launchSingleTop = true
                                }
                            },
                            onLocalReportsClick = {
                                navController.navigate(AppRoute.OfflineReports.route) {
                                    launchSingleTop = true
                                }
                            },
                            onSyncCenterClick = {
                                navController.navigate(AppRoute.SyncCenter.route) {
                                    launchSingleTop = true
                                }
                            },
                            onBackupRestoreClick = {
                                navController.navigate(AppRoute.BackupRestore.route) {
                                    launchSingleTop = true
                                }
                            },
                            onStockReportClick = {
                                navController.navigate(AppRoute.StockReport.route) {
                                    launchSingleTop = true
                                }
                            },
                            onReceivablesClick = {
                                navController.navigate(AppRoute.Receivables.route) {
                                    launchSingleTop = true
                                }
                            },
                            onSupplierDebtsClick = {
                                navController.navigate(AppRoute.SupplierDebts.route) {
                                    launchSingleTop = true
                                }
                            },
                            onCashReconciliationClick = {
                                navController.navigate(AppRoute.CashReconciliation.route) {
                                    launchSingleTop = true
                                }
                            },
                            onOperationalAuditClick = {
                                navController.navigate(AppRoute.OperationalAudit.route) {
                                    launchSingleTop = true
                                }
                            },
                            onUserManagementClick = {
                                navController.navigate(AppRoute.UserManagement.route) {
                                    launchSingleTop = true
                                }
                            },
                            onSecurityLogClick = {
                                navController.navigate(AppRoute.SecurityLog.route) {
                                    launchSingleTop = true
                                }
                            },
                            onSettingsClick = {
                                navController.navigate(AppRoute.AdminSettings.route) {
                                    launchSingleTop = true
                                }
                            },
                            onLogout = logout
                        )
                    }

                    "admin" -> {
                        AdminDashboardScreen(
                            name = sessionUser.name,
                            role = sessionUser.role,
                            analyticsRepository = appContainer.analyticsRepository,
                            offlineDashboardRepository = appContainer.offlineDashboardRepository,
                            onProductsClick = {
                                navController.navigate(AppRoute.Products.route) {
                                    launchSingleTop = true
                                }
                            },
                            onAddProductClick = {
                                navController.navigate(AppRoute.AddProduct.route) {
                                    launchSingleTop = true
                                }
                            },
                            onProductCategoriesClick = {
                                navController.navigate(AppRoute.ProductCategories.route) {
                                    launchSingleTop = true
                                }
                            },
                            onProductUnitsClick = {
                                navController.navigate(AppRoute.ProductUnits.route) {
                                    launchSingleTop = true
                                }
                            },
                            onCashReconciliationClick = {
                                navController.navigate(AppRoute.CashReconciliation.route) {
                                    launchSingleTop = true
                                }
                            },
                            onSalesTransactionsClick = {
                                navController.navigate(AppRoute.SalesTransactions.route) {
                                    launchSingleTop = true
                                }
                            },
                            onReportsClick = {
                                navController.navigate(AppRoute.Reports.route) {
                                    launchSingleTop = true
                                }
                            },
                            onLocalReportsClick = {
                                navController.navigate(AppRoute.OfflineReports.route) {
                                    launchSingleTop = true
                                }
                            },
                            onSyncCenterClick = {
                                navController.navigate(AppRoute.SyncCenter.route) {
                                    launchSingleTop = true
                                }
                            },
                            onPriceManagementClick = {
                                navController.navigate(AppRoute.PriceManagement.route) {
                                    launchSingleTop = true
                                }
                            },
                            onStockOpnameClick = {
                                navController.navigate(AppRoute.StockOpname.route) {
                                    launchSingleTop = true
                                }
                            },
                            onStockOpnameFormClick = {
                                navController.navigate(AppRoute.StockOpnameForm.createRoute()) {
                                    launchSingleTop = true
                                }
                            },
                            onIncomingGoodsClick = {
                                navController.navigate(AppRoute.IncomingGoods.route) {
                                    launchSingleTop = true
                                }
                            },
                            onIncomingGoodsFormClick = {
                                navController.navigate(AppRoute.IncomingGoodsForm.createRoute()) {
                                    launchSingleTop = true
                                }
                            },
                            onSuppliersClick = {
                                navController.navigate(AppRoute.Suppliers.route) {
                                    launchSingleTop = true
                                }
                            },
                            onPurchaseHistoryClick = {
                                navController.navigate(AppRoute.PurchaseHistory.route) {
                                    launchSingleTop = true
                                }
                            },
                            onStockReportClick = {
                                navController.navigate(AppRoute.StockReport.route) {
                                    launchSingleTop = true
                                }
                            },
                            onSupplierDebtsClick = {
                                navController.navigate(AppRoute.SupplierDebts.route) {
                                    launchSingleTop = true
                                }
                            },
                            onCashSessionHistoryClick = {
                                navController.navigate(AppRoute.CashSessionHistory.route) {
                                    launchSingleTop = true
                                }
                            },
                            onCashReconciliationDetailClick = {
                                navController.navigate(AppRoute.CashReconciliationDetail.createRoute()) {
                                    launchSingleTop = true
                                }
                            },
                            onCashExpensesClick = {
                                navController.navigate(AppRoute.CashExpenses.route) {
                                    launchSingleTop = true
                                }
                            },
                            onReceivablesClick = {
                                navController.navigate(AppRoute.Receivables.route) {
                                    launchSingleTop = true
                                }
                            },
                            onReceivablePaymentsClick = {
                                navController.navigate(AppRoute.ReceivablePayments.route) {
                                    launchSingleTop = true
                                }
                            },
                            onCustomersClick = {
                                navController.navigate(AppRoute.Customers.route) {
                                    launchSingleTop = true
                                }
                            },
                            onOperationalAuditClick = {
                                navController.navigate(AppRoute.OperationalAudit.route) {
                                    launchSingleTop = true
                                }
                            },
                            onProfileClick = {
                        navController.navigate(AppRoute.AdminProfile.route) {
                            launchSingleTop = true
                        }
                    },
                    onSettingsClick = {
                                navController.navigate(AppRoute.AdminSettings.route) {
                                    launchSingleTop = true
                                }
                            },
                            onLogout = logout
                        )
                    }

                    "kasir" -> {
                        CashierDashboardScreen(
                            name = sessionUser.name,
                            role = sessionUser.role,
                            cashReconciliationRepository = appContainer.cashReconciliationRepository,
                            onDashboardClick = {},
                            onPosClick = {
                                navController.navigate(AppRoute.CashierPos.route) {
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
                            onLogout = logout
                        )
                    }

                    else -> {
                        LaunchedEffect(sessionUser.role) {
                            sessionManager.logout()
                            navController.navigate(AppRoute.Login.route) {
                                popUpTo(0)
                                launchSingleTop = true
                            }
                        }
                    }
                }
            }
        }

        composable(AppRoute.CashierProfile.route) {
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
                CashierProfileScreen(
                    userName = sessionUser.name,
                    role = sessionUser.role,
                    authRepository = appContainer.authRepository,
                    onDashboardClick = {
                        navController.navigate(AppRoute.Dashboard.route) {
                            popUpTo(AppRoute.Dashboard.route) { inclusive = true }
                        }
                    },
                    onPosClick = {
                        navController.navigate(AppRoute.CashierPos.route) {
                            popUpTo(AppRoute.Dashboard.route)
                        }
                    },
                    onCashSessionClick = {
                        navController.navigate(AppRoute.CashierCashSession.route) {
                            popUpTo(AppRoute.Dashboard.route)
                        }
                    },
                    onTransactionHistoryClick = {
                        navController.navigate(AppRoute.CashierTransactionHistory.route) {
                            popUpTo(AppRoute.Dashboard.route)
                        }
                    },
                    onStockCheckClick = {
                        navController.navigate(AppRoute.CashierStockCheck.route) {
                            popUpTo(AppRoute.Dashboard.route)
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

        composable(AppRoute.CashierSettings.route) {
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
                CashierSettingsScreen(
                    userName = sessionUser.name,
                    role = sessionUser.role,
                    systemRepository = appContainer.systemRepository,
                    localAppSettingsDataSource = appContainer.localAppSettingsDataSource,
                    onDashboardClick = {
                        navController.navigate(AppRoute.Dashboard.route) {
                            popUpTo(AppRoute.Dashboard.route) { inclusive = true }
                        }
                    },
                    onPosClick = {
                        navController.navigate(AppRoute.CashierPos.route) {
                            popUpTo(AppRoute.Dashboard.route)
                        }
                    },
                    onCashSessionClick = {
                        navController.navigate(AppRoute.CashierCashSession.route) {
                            popUpTo(AppRoute.Dashboard.route)
                        }
                    },
                    onTransactionHistoryClick = {
                        navController.navigate(AppRoute.CashierTransactionHistory.route) {
                            popUpTo(AppRoute.Dashboard.route)
                        }
                    },
                    onStockCheckClick = {
                        navController.navigate(AppRoute.CashierStockCheck.route) {
                            popUpTo(AppRoute.Dashboard.route)
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

        composable(AppRoute.AdminProfile.route) {
            val sessionUser = sessionManager.readSessionUser()
            if (sessionUser == null) {
                LaunchedEffect(Unit) { logout(sessionManager, navController) }
            } else {
                fun navigate(route: String) {
                    navController.navigate(route) { launchSingleTop = true }
                }
                AdminProfileScreen(
                    name = sessionUser.name,
                    role = sessionUser.role,
                    authRepository = appContainer.authRepository,
                    onDashboardClick = { navigate(AppRoute.Dashboard.route) },
                    onProductsClick = { navigate(AppRoute.Products.route) },
                    onAddProductClick = { navigate(AppRoute.AddProduct.route) },
                    onProductCategoriesClick = { navigate(AppRoute.ProductCategories.route) },
                    onProductUnitsClick = { navigate(AppRoute.ProductUnits.route) },
                    onCashReconciliationClick = { navigate(AppRoute.CashReconciliation.route) },
                    onSalesTransactionsClick = { navigate(AppRoute.SalesTransactions.route) },
                    onReportsClick = { navigate(AppRoute.Reports.route) },
                    onPriceManagementClick = { navigate(AppRoute.PriceManagement.route) },
                    onStockOpnameClick = { navigate(AppRoute.StockOpname.route) },
                    onStockOpnameFormClick = { navigate(AppRoute.StockOpnameForm.createRoute()) },
                    onIncomingGoodsClick = { navigate(AppRoute.IncomingGoods.route) },
                    onIncomingGoodsFormClick = { navigate(AppRoute.IncomingGoodsForm.createRoute()) },
                    onSupplierDebtsClick = { navigate(AppRoute.SupplierDebts.route) },
                    onReceivablesClick = { navigate(AppRoute.Receivables.route) },
                    onCustomersClick = { navigate(AppRoute.Customers.route) },
                    onOperationalAuditClick = { navigate(AppRoute.OperationalAudit.route) },
                    onProfileClick = { navigate(AppRoute.AdminProfile.route) },
                    onSettingsClick = { navigate(AppRoute.AdminSettings.route) },
                    onLogout = { logout(sessionManager, navController) }
                )
            }
        }

        composable(AppRoute.AdminSettings.route) {
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
                fun navigate(route: String) {
                    navController.navigate(route) {
                        launchSingleTop = true
                    }
                }

                AdminSettingsScreen(
                    name = sessionUser.name,
                    role = sessionUser.role,
                    systemRepository = appContainer.systemRepository,
                    localAppSettingsDataSource = appContainer.localAppSettingsDataSource,
                    onDashboardClick = { navigate(AppRoute.Dashboard.route) },
                    onProductsClick = { navigate(AppRoute.Products.route) },
                    onAddProductClick = { navigate(AppRoute.AddProduct.route) },
                    onProductCategoriesClick = { navigate(AppRoute.ProductCategories.route) },
                    onProductUnitsClick = { navigate(AppRoute.ProductUnits.route) },
                    onCashReconciliationClick = { navigate(AppRoute.CashReconciliation.route) },
                    onSalesTransactionsClick = { navigate(AppRoute.SalesTransactions.route) },
                    onReportsClick = { navigate(AppRoute.Reports.route) },
                    onPriceManagementClick = { navigate(AppRoute.PriceManagement.route) },
                    onStockOpnameClick = { navigate(AppRoute.StockOpname.route) },
                    onStockOpnameFormClick = { navigate(AppRoute.StockOpnameForm.createRoute()) },
                    onIncomingGoodsClick = { navigate(AppRoute.IncomingGoods.route) },
                    onIncomingGoodsFormClick = { navigate(AppRoute.IncomingGoodsForm.createRoute()) },
                    onSuppliersClick = { navigate(AppRoute.Suppliers.route) },
                    onPurchaseHistoryClick = { navigate(AppRoute.PurchaseHistory.route) },
                    onStockReportClick = { navigate(AppRoute.StockReport.route) },
                    onSupplierDebtsClick = { navigate(AppRoute.SupplierDebts.route) },
                    onCashSessionHistoryClick = { navigate(AppRoute.CashSessionHistory.route) },
                    onCashReconciliationDetailClick = { navigate(AppRoute.CashReconciliationDetail.createRoute()) },
                    onCashExpensesClick = { navigate(AppRoute.CashExpenses.route) },
                    onReceivablesClick = { navigate(AppRoute.Receivables.route) },
                    onReceivablePaymentsClick = { navigate(AppRoute.ReceivablePayments.route) },
                    onCustomersClick = { navigate(AppRoute.Customers.route) },
                    onOperationalAuditClick = { navigate(AppRoute.OperationalAudit.route) },
                    onUserManagementClick = { navigate(AppRoute.UserManagement.route) },
                    onSecurityLogClick = { navigate(AppRoute.SecurityLog.route) },
                    onProfileClick = { navigate(AppRoute.AdminProfile.route) },
                    onSettingsClick = { navigate(AppRoute.AdminSettings.route) },
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

        composable(AppRoute.Suppliers.route) {
            AdminSupplierRoute(
                sessionManager = sessionManager,
                navController = navController,
                appContainer = appContainer
            )
        }

        composable(AppRoute.PurchaseHistory.route) {
            AdminPurchaseHistoryRoute(
                sessionManager = sessionManager,
                navController = navController,
                appContainer = appContainer
            )
        }

        composable(AppRoute.StockReport.route) {
            AdminStockReportRoute(
                sessionManager = sessionManager,
                navController = navController,
                appContainer = appContainer
            )
        }

        composable(AppRoute.CashSessionHistory.route) {
            AdminCashSessionHistoryRoute(
                sessionManager = sessionManager,
                navController = navController,
                appContainer = appContainer
            )
        }

        composable(
            route = AppRoute.CashReconciliationDetail.route,
            arguments = listOf(
                navArgument(AppRoute.CashReconciliationDetail.SESSION_ID_ARG) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            AdminCashReconciliationDetailRoute(
                sessionManager = sessionManager,
                navController = navController,
                appContainer = appContainer,
                sessionId = backStackEntry.arguments
                    ?.getString(AppRoute.CashReconciliationDetail.SESSION_ID_ARG)
                    .orEmpty()
            )
        }

        composable(AppRoute.CashExpenses.route) {
            AdminCashExpenseHistoryRoute(
                sessionManager = sessionManager,
                navController = navController,
                appContainer = appContainer
            )
        }

        composable(AppRoute.ReceivablePayments.route) {
            AdminReceivablePaymentHistoryRoute(
                sessionManager = sessionManager,
                navController = navController,
                appContainer = appContainer
            )
        }

}

@Composable
private fun AdminSupplierRoute(
    sessionManager: SessionManager,
    navController: NavHostController,
    appContainer: AppContainer
) {
    val sessionUser = sessionManager.readSessionUser()
    if (sessionUser == null) {
        RedirectToLogin(sessionManager, navController)
        return
    }
    fun navigate(route: String) = navController.navigate(route) { launchSingleTop = true }
    AdminSupplierScreen(
        name = sessionUser.name,
        role = sessionUser.role,
        purchasingRepository = appContainer.purchasingRepository,
        onDashboardClick = { navigate(AppRoute.Dashboard.route) },
        onProductsClick = { navigate(AppRoute.Products.route) },
        onAddProductClick = { navigate(AppRoute.AddProduct.route) },
        onProductCategoriesClick = { navigate(AppRoute.ProductCategories.route) },
        onProductUnitsClick = { navigate(AppRoute.ProductUnits.route) },
        onStockOpnameClick = { navigate(AppRoute.StockOpname.route) },
        onStockOpnameFormClick = { navigate(AppRoute.StockOpnameForm.createRoute()) },
        onIncomingGoodsClick = { navigate(AppRoute.IncomingGoods.route) },
        onIncomingGoodsFormClick = { navigate(AppRoute.IncomingGoodsForm.createRoute()) },
        onSuppliersClick = {},
        onPurchaseHistoryClick = { navigate(AppRoute.PurchaseHistory.route) },
        onStockReportClick = { navigate(AppRoute.StockReport.route) },
        onSupplierDebtsClick = { navigate(AppRoute.SupplierDebts.route) },
        onReceivablesClick = { navigate(AppRoute.Receivables.route) },
        onCustomersClick = { navigate(AppRoute.Customers.route) },
        onReportsClick = { navigate(AppRoute.Reports.route) },
        onOperationalAuditClick = { navigate(AppRoute.OperationalAudit.route) },
        onLogout = { logout(sessionManager, navController) }
    )
}

@Composable
private fun AdminStockReportRoute(
    sessionManager: SessionManager,
    navController: NavHostController,
    appContainer: AppContainer
) {
    val sessionUser = sessionManager.readSessionUser()
    if (sessionUser == null) {
        RedirectToLogin(sessionManager, navController)
        return
    }
    fun navigate(route: String) = navController.navigate(route) { launchSingleTop = true }
    AdminStockReportScreen(
        name = sessionUser.name,
        role = sessionUser.role,
        inventoryRepository = appContainer.inventoryRepository,
        onDashboardClick = { navigate(AppRoute.Dashboard.route) },
        onProductsClick = { navigate(AppRoute.Products.route) },
        onAddProductClick = { navigate(AppRoute.AddProduct.route) },
        onProductCategoriesClick = { navigate(AppRoute.ProductCategories.route) },
        onProductUnitsClick = { navigate(AppRoute.ProductUnits.route) },
        onStockOpnameClick = { navigate(AppRoute.StockOpname.route) },
        onStockOpnameFormClick = { navigate(AppRoute.StockOpnameForm.createRoute()) },
        onIncomingGoodsClick = { navigate(AppRoute.IncomingGoods.route) },
        onIncomingGoodsFormClick = { navigate(AppRoute.IncomingGoodsForm.createRoute()) },
        onSuppliersClick = { navigate(AppRoute.Suppliers.route) },
        onPurchaseHistoryClick = { navigate(AppRoute.PurchaseHistory.route) },
        onStockReportClick = {},
        onSupplierDebtsClick = { navigate(AppRoute.SupplierDebts.route) },
        onReceivablesClick = { navigate(AppRoute.Receivables.route) },
        onCustomersClick = { navigate(AppRoute.Customers.route) },
        onReportsClick = { navigate(AppRoute.Reports.route) },
        onOperationalAuditClick = { navigate(AppRoute.OperationalAudit.route) },
        onLogout = { logout(sessionManager, navController) }
    )
}

@Composable
private fun AdminPurchaseHistoryRoute(
    sessionManager: SessionManager,
    navController: NavHostController,
    appContainer: AppContainer
) {
    val sessionUser = sessionManager.readSessionUser()
    if (sessionUser == null) {
        RedirectToLogin(sessionManager, navController)
        return
    }
    fun navigate(route: String) = navController.navigate(route) { launchSingleTop = true }
    AdminPurchaseHistoryScreen(
        name = sessionUser.name,
        role = sessionUser.role,
        purchasingRepository = appContainer.purchasingRepository,
        onDashboardClick = { navigate(AppRoute.Dashboard.route) },
        onProductsClick = { navigate(AppRoute.Products.route) },
        onAddProductClick = { navigate(AppRoute.AddProduct.route) },
        onProductCategoriesClick = { navigate(AppRoute.ProductCategories.route) },
        onProductUnitsClick = { navigate(AppRoute.ProductUnits.route) },
        onStockOpnameClick = { navigate(AppRoute.StockOpname.route) },
        onStockOpnameFormClick = { navigate(AppRoute.StockOpnameForm.createRoute()) },
        onIncomingGoodsClick = { navigate(AppRoute.IncomingGoods.route) },
        onIncomingGoodsFormClick = { navigate(AppRoute.IncomingGoodsForm.createRoute()) },
        onSuppliersClick = { navigate(AppRoute.Suppliers.route) },
        onPurchaseHistoryClick = {},
        onStockReportClick = { navigate(AppRoute.StockReport.route) },
        onSupplierDebtsClick = { navigate(AppRoute.SupplierDebts.route) },
        onReceivablesClick = { navigate(AppRoute.Receivables.route) },
        onCustomersClick = { navigate(AppRoute.Customers.route) },
        onReportsClick = { navigate(AppRoute.Reports.route) },
        onOperationalAuditClick = { navigate(AppRoute.OperationalAudit.route) },
        onLogout = { logout(sessionManager, navController) }
    )
}

@Composable
private fun AdminReceivablePaymentHistoryRoute(
    sessionManager: SessionManager,
    navController: NavHostController,
    appContainer: AppContainer
) {
    val sessionUser = sessionManager.readSessionUser()
    if (sessionUser == null) {
        RedirectToLogin(sessionManager, navController)
        return
    }
    fun navigate(route: String) = navController.navigate(route) { launchSingleTop = true }
    AdminReceivablePaymentHistoryScreen(
        name = sessionUser.name,
        role = sessionUser.role,
        receivableRepository = appContainer.receivableRepository,
        onDashboardClick = { navigate(AppRoute.Dashboard.route) },
        onProductsClick = { navigate(AppRoute.Products.route) },
        onStockOpnameClick = { navigate(AppRoute.StockOpname.route) },
        onIncomingGoodsClick = { navigate(AppRoute.IncomingGoods.route) },
        onSupplierDebtsClick = { navigate(AppRoute.SupplierDebts.route) },
        onReceivablesClick = { navigate(AppRoute.Receivables.route) },
        onCustomersClick = { navigate(AppRoute.Customers.route) },
        onReportsClick = { navigate(AppRoute.Reports.route) },
        onOperationalAuditClick = { navigate(AppRoute.OperationalAudit.route) },
        onLogout = { logout(sessionManager, navController) }
    )
}

@Composable
private fun AdminCashSessionHistoryRoute(
    sessionManager: SessionManager,
    navController: NavHostController,
    appContainer: AppContainer
) {
    val sessionUser = sessionManager.readSessionUser()
    if (sessionUser == null) {
        RedirectToLogin(sessionManager, navController)
        return
    }
    fun navigate(route: String) = navController.navigate(route) { launchSingleTop = true }
    AdminCashSessionHistoryScreen(
        name = sessionUser.name,
        role = sessionUser.role,
        cashRepository = appContainer.cashReconciliationRepository,
        onDashboardClick = { navigate(AppRoute.Dashboard.route) },
        onProductsClick = { navigate(AppRoute.Products.route) },
        onStockOpnameClick = { navigate(AppRoute.StockOpname.route) },
        onIncomingGoodsClick = { navigate(AppRoute.IncomingGoods.route) },
        onSupplierDebtsClick = { navigate(AppRoute.SupplierDebts.route) },
        onReceivablesClick = { navigate(AppRoute.Receivables.route) },
        onCustomersClick = { navigate(AppRoute.Customers.route) },
        onReportsClick = { navigate(AppRoute.Reports.route) },
        onOperationalAuditClick = { navigate(AppRoute.OperationalAudit.route) },
        onShowDetail = { navigate(AppRoute.CashReconciliationDetail.createRoute(it)) },
        onLogout = { logout(sessionManager, navController) }
    )
}

@Composable
private fun AdminCashReconciliationDetailRoute(
    sessionManager: SessionManager,
    navController: NavHostController,
    appContainer: AppContainer,
    sessionId: String
) {
    val sessionUser = sessionManager.readSessionUser()
    if (sessionUser == null) {
        RedirectToLogin(sessionManager, navController)
        return
    }
    fun navigate(route: String) = navController.navigate(route) { launchSingleTop = true }
    AdminCashReconciliationDetailScreen(
        name = sessionUser.name,
        role = sessionUser.role,
        sessionId = sessionId,
        cashRepository = appContainer.cashReconciliationRepository,
        onDashboardClick = { navigate(AppRoute.Dashboard.route) },
        onProductsClick = { navigate(AppRoute.Products.route) },
        onStockOpnameClick = { navigate(AppRoute.StockOpname.route) },
        onIncomingGoodsClick = { navigate(AppRoute.IncomingGoods.route) },
        onSupplierDebtsClick = { navigate(AppRoute.SupplierDebts.route) },
        onReceivablesClick = { navigate(AppRoute.Receivables.route) },
        onCustomersClick = { navigate(AppRoute.Customers.route) },
        onReportsClick = { navigate(AppRoute.Reports.route) },
        onOperationalAuditClick = { navigate(AppRoute.OperationalAudit.route) },
        onOpenHistory = { navigate(AppRoute.CashSessionHistory.route) },
        onLogout = { logout(sessionManager, navController) }
    )
}

@Composable
private fun AdminCashExpenseHistoryRoute(
    sessionManager: SessionManager,
    navController: NavHostController,
    appContainer: AppContainer
) {
    val sessionUser = sessionManager.readSessionUser()
    if (sessionUser == null) {
        RedirectToLogin(sessionManager, navController)
        return
    }
    fun navigate(route: String) = navController.navigate(route) { launchSingleTop = true }
    AdminCashExpenseHistoryScreen(
        name = sessionUser.name,
        role = sessionUser.role,
        cashRepository = appContainer.cashReconciliationRepository,
        onDashboardClick = { navigate(AppRoute.Dashboard.route) },
        onProductsClick = { navigate(AppRoute.Products.route) },
        onStockOpnameClick = { navigate(AppRoute.StockOpname.route) },
        onIncomingGoodsClick = { navigate(AppRoute.IncomingGoods.route) },
        onSupplierDebtsClick = { navigate(AppRoute.SupplierDebts.route) },
        onReceivablesClick = { navigate(AppRoute.Receivables.route) },
        onCustomersClick = { navigate(AppRoute.Customers.route) },
        onReportsClick = { navigate(AppRoute.Reports.route) },
        onOperationalAuditClick = { navigate(AppRoute.OperationalAudit.route) },
        onShowSessionDetail = { navigate(AppRoute.CashReconciliationDetail.createRoute(it)) },
        onLogout = { logout(sessionManager, navController) }
    )
}

@Composable
private fun RedirectToLogin(sessionManager: SessionManager, navController: NavHostController) {
    LaunchedEffect(Unit) {
        logout(sessionManager, navController)
    }
}

private fun logout(sessionManager: SessionManager, navController: NavHostController) {
    sessionManager.logout()
    navController.navigate(AppRoute.Login.route) {
        popUpTo(0)
        launchSingleTop = true
    }
}

@Composable
private fun AdminPlaceholderRoute(
    sessionManager: SessionManager,
    navController: NavHostController,
    destination: AdminDestination,
    title: String,
    subtitle: String,
    badgeText: String,
    focusItems: List<String>,
    integrationNotes: List<String>
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
    } else {
        fun navigate(route: String) {
            navController.navigate(route) {
                launchSingleTop = true
            }
        }

        AdminBackofficePlaceholderScreen(
            name = sessionUser.name,
            role = sessionUser.role,
            destination = destination,
            title = title,
            subtitle = subtitle,
            badgeText = badgeText,
            focusItems = focusItems,
            integrationNotes = integrationNotes,
            onDashboardClick = { navigate(AppRoute.Dashboard.route) },
            onProductsClick = { navigate(AppRoute.Products.route) },
            onAddProductClick = { navigate(AppRoute.AddProduct.route) },
            onProductCategoriesClick = { navigate(AppRoute.ProductCategories.route) },
            onProductUnitsClick = { navigate(AppRoute.ProductUnits.route) },
            onCashReconciliationClick = { navigate(AppRoute.CashReconciliation.route) },
            onSalesTransactionsClick = { navigate(AppRoute.SalesTransactions.route) },
            onReportsClick = { navigate(AppRoute.Reports.route) },
            onPriceManagementClick = { navigate(AppRoute.PriceManagement.route) },
            onStockOpnameClick = { navigate(AppRoute.StockOpname.route) },
            onStockOpnameFormClick = { navigate(AppRoute.StockOpnameForm.createRoute()) },
            onIncomingGoodsClick = { navigate(AppRoute.IncomingGoods.route) },
            onIncomingGoodsFormClick = { navigate(AppRoute.IncomingGoodsForm.createRoute()) },
            onSuppliersClick = { navigate(AppRoute.Suppliers.route) },
            onPurchaseHistoryClick = { navigate(AppRoute.PurchaseHistory.route) },
            onStockReportClick = { navigate(AppRoute.StockReport.route) },
            onSupplierDebtsClick = { navigate(AppRoute.SupplierDebts.route) },
            onCashSessionHistoryClick = { navigate(AppRoute.CashSessionHistory.route) },
            onCashReconciliationDetailClick = { navigate(AppRoute.CashReconciliationDetail.createRoute()) },
            onCashExpensesClick = { navigate(AppRoute.CashExpenses.route) },
            onReceivablesClick = { navigate(AppRoute.Receivables.route) },
            onReceivablePaymentsClick = { navigate(AppRoute.ReceivablePayments.route) },
            onCustomersClick = { navigate(AppRoute.Customers.route) },
            onOperationalAuditClick = { navigate(AppRoute.OperationalAudit.route) },
            onProfileClick = { navigate(AppRoute.AdminProfile.route) },
            onSettingsClick = { navigate(AppRoute.AdminSettings.route) },
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


private fun String.normalizedRole(): String {
    return trim().lowercase()
}
