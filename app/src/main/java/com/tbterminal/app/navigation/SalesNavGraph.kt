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
internal fun NavGraphBuilder.salesGraph(navController: NavHostController, sessionManager: SessionManager, appContainer: AppContainer) {
        composable(AppRoute.CashierCashSession.route) {
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

                CashierCloseShiftScreen(
                    name = sessionUser.name,
                    role = sessionUser.role,
                    cashReconciliationRepository = appContainer.cashReconciliationRepository,
                    cashSessionLocalDataSource = appContainer.cashSessionLocalDataSource,
                    localCashSessionService = appContainer.localCashSessionService,
                    localCashExpenseService = appContainer.localCashExpenseService,
                    offlineStatusRepository = appContainer.offlineStatusRepository,
                    cashierUserId = sessionUser.userId ?: sessionUser.name,
                    cashierNameFallback = sessionUser.name,
                    onDashboardClick = {
                        navController.navigate(AppRoute.Dashboard.route) {
                            launchSingleTop = true
                        }
                    },
                    onPosClick = {
                        navController.navigate(AppRoute.CashierPos.route) {
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
        }

        composable(AppRoute.CashierTransactionHistory.route) {
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

                CashierTransactionHistoryScreen(
                    name = sessionUser.name,
                    role = sessionUser.role,
                    cashReconciliationRepository = appContainer.cashReconciliationRepository,
                    transactionDao = appContainer.localDatabase.transactionDao(),
                    offlineCheckoutSyncService = appContainer.offlineCheckoutSyncService,
                    onDashboardClick = {
                        navController.navigate(AppRoute.Dashboard.route) {
                            launchSingleTop = true
                        }
                    },
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
                    onTransactionHistoryClick = {},
                    onStockCheckClick = {
                        navController.navigate(AppRoute.CashierStockCheck.route) {
                            launchSingleTop = true
                        }
                    },
                    onReceiptClick = { transactionId ->
                        navController.navigate(AppRoute.CashierReceiptDetail.createRoute(transactionId)) {
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
        }

        composable(
            route = AppRoute.CashierReceiptDetail.route,
            arguments = listOf(
                navArgument(AppRoute.CashierReceiptDetail.TRANSACTION_ID_ARG) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val sessionUser = sessionManager.readSessionUser()
            val transactionId = backStackEntry.arguments
                ?.getString(AppRoute.CashierReceiptDetail.TRANSACTION_ID_ARG)
                .orEmpty()

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

                CashierReceiptDetailScreen(
                    name = sessionUser.name,
                    role = sessionUser.role,
                    transactionId = transactionId,
                    cashReconciliationRepository = appContainer.cashReconciliationRepository,
                    onBackClick = { navController.popBackStack() },
                    onDashboardClick = {
                        navController.navigate(AppRoute.Dashboard.route) {
                            launchSingleTop = true
                        }
                    },
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
        }

        composable(AppRoute.CashReconciliation.route) {
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

                AdminCashReconciliationScreen(
                    name = sessionUser.name,
                    role = sessionUser.role,
                    cashReconciliationRepository = appContainer.cashReconciliationRepository,
                    onDashboardClick = {
                        navController.navigate(AppRoute.Dashboard.route) {
                            launchSingleTop = true
                        }
                    },
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
                    onCashReconciliationClick = {},
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
                    onSupplierDebtsClick = {
                        navController.navigate(AppRoute.SupplierDebts.route) {
                            launchSingleTop = true
                        }
                    },
                    onReceivablesClick = {
                        navController.navigate(AppRoute.Receivables.route) {
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
                    onCashSessionDetailClick = { sessionId ->
                        navController.navigate(AppRoute.CashReconciliationDetail.createRoute(sessionId)) {
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
                    onBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(AppRoute.Dashboard.route) { launchSingleTop = true }
                        }
                    },
                    onLogout = logout
                )
            }
        }

        composable(AppRoute.SalesTransactions.route) {
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
                AdminTransactionHistoryScreen(
                    name = sessionUser.name,
                    role = sessionUser.role,
                    cashReconciliationRepository = appContainer.cashReconciliationRepository,
                    transactionDao = appContainer.localDatabase.transactionDao(),
                    offlineCheckoutSyncService = appContainer.offlineCheckoutSyncService,
                    onReceiptClick = { transactionId ->
                        navController.navigate(AppRoute.AdminReceiptDetail.createRoute(transactionId)) { launchSingleTop = true }
                    },
                    onDashboardClick = {
                        navController.navigate(AppRoute.Dashboard.route) {
                            launchSingleTop = true
                        }
                    },
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
                    onPriceManagementClick = {
                        navController.navigate(AppRoute.PriceManagement.route) {
                            launchSingleTop = true
                        }
                    },                                    onStockOpnameClick = {
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
                    onSupplierDebtsClick = {
                        navController.navigate(AppRoute.SupplierDebts.route) {
                            launchSingleTop = true
                        }
                    },
                    onReceivablesClick = {},
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
                    },                                    onLogout = {
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
            route = AppRoute.AdminReceiptDetail.route,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""
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
                AdminReceiptDetailScreen(
                    name = sessionUser.name,
                    role = sessionUser.role,
                    transactionId = transactionId,
                    cashReconciliationRepository = appContainer.cashReconciliationRepository,
                    onBackClick = { navController.popBackStack() },
                    onDashboardClick = {
                        navController.navigate(AppRoute.Dashboard.route) {
                            launchSingleTop = true
                        }
                    },
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
                    onPriceManagementClick = {
                        navController.navigate(AppRoute.PriceManagement.route) {
                            launchSingleTop = true
                        }
                    },                                    onStockOpnameClick = {
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
                    onSupplierDebtsClick = {
                        navController.navigate(AppRoute.SupplierDebts.route) {
                            launchSingleTop = true
                        }
                    },
                    onReceivablesClick = {},
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
                    },                                    onLogout = {
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
