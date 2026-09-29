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
internal fun NavGraphBuilder.inventoryGraph(navController: NavHostController, sessionManager: SessionManager, appContainer: AppContainer) {
        composable(AppRoute.CashierStockCheck.route) {
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

                CashierStockCheckScreen(
                    name = sessionUser.name,
                    role = sessionUser.role,
                    inventoryRepository = appContainer.inventoryRepository,
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

        composable(AppRoute.Products.route) {
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
                AdminProductListScreen(
                    name = sessionUser.name,
                    role = sessionUser.role,
                    inventoryRepository = appContainer.inventoryRepository,
                    onDashboardClick = {
                        navController.navigate(AppRoute.Dashboard.route) {
                            launchSingleTop = true
                        }
                    },
                    onProductsClick = {},
                    onAddProductClick = {
                        navController.navigate(AppRoute.AddProduct.route) {
                            launchSingleTop = true
                        }
                    },
                    onEditProductClick = { productId ->
                        navController.navigate(AppRoute.EditProduct.createRoute(productId)) {
                            launchSingleTop = true
                        }
                    },
                    onProductDetailClick = { productId ->
                        navController.navigate(AppRoute.ProductDetail.createRoute(productId)) {
                            launchSingleTop = true
                        }
                    },
                    onCategoriesClick = {
                        navController.navigate(AppRoute.ProductCategories.route) {
                            launchSingleTop = true
                        }
                    },
                    onUnitsClick = {
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

        composable(AppRoute.AddProduct.route) {
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
                AdminProductFormScreen(
                    name = sessionUser.name,
                    role = sessionUser.role,
                    inventoryRepository = appContainer.inventoryRepository,
                    productId = null,
                    onDashboardClick = {
                        navController.navigate(AppRoute.Dashboard.route) {
                            launchSingleTop = true
                        }
                    },
                    onProductsClick = {
                        navController.navigate(AppRoute.Products.route) {
                            popUpTo(AppRoute.Products.route) { inclusive = true }
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
                    onProfileClick = {
                        navController.navigate(AppRoute.AdminProfile.route) {
                            launchSingleTop = true
                        }
                    },
                    onSettingsClick = {
                        navController.navigate(AppRoute.AdminSettings.route) {
                            launchSingleTop = true
                        }
                    },                                    onBackToProducts = {
                        navController.navigate(AppRoute.Products.route) {
                            popUpTo(AppRoute.Products.route) { inclusive = true }
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
            route = AppRoute.EditProduct.route,
            arguments = listOf(
                navArgument(AppRoute.EditProduct.PRODUCT_ID_ARG) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val sessionUser = sessionManager.readSessionUser()
            val productId = backStackEntry.arguments?.getString(AppRoute.EditProduct.PRODUCT_ID_ARG)

            if (sessionUser == null || productId.isNullOrBlank()) {
                LaunchedEffect(Unit) {
                    sessionManager.logout()
                    navController.navigate(AppRoute.Login.route) {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                }
            } else {
                AdminProductFormScreen(
                    name = sessionUser.name,
                    role = sessionUser.role,
                    inventoryRepository = appContainer.inventoryRepository,
                    productId = productId,
                    onDashboardClick = {
                        navController.navigate(AppRoute.Dashboard.route) {
                            launchSingleTop = true
                        }
                    },
                    onProductsClick = {
                        navController.navigate(AppRoute.Products.route) {
                            popUpTo(AppRoute.Products.route) { inclusive = true }
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
                    onProfileClick = {
                        navController.navigate(AppRoute.AdminProfile.route) {
                            launchSingleTop = true
                        }
                    },
                    onSettingsClick = {
                        navController.navigate(AppRoute.AdminSettings.route) {
                            launchSingleTop = true
                        }
                    },                                    onBackToProducts = {
                        navController.navigate(AppRoute.Products.route) {
                            popUpTo(AppRoute.Products.route) { inclusive = true }
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
            route = AppRoute.ProductDetail.route,
            arguments = listOf(
                navArgument(AppRoute.ProductDetail.PRODUCT_ID_ARG) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val sessionUser = sessionManager.readSessionUser()
            val productId = backStackEntry.arguments?.getString(AppRoute.ProductDetail.PRODUCT_ID_ARG)

            if (sessionUser == null || productId.isNullOrBlank()) {
                LaunchedEffect(Unit) {
                    sessionManager.logout()
                    navController.navigate(AppRoute.Login.route) {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                }
            } else {
                AdminProductDetailScreen(
                    name = sessionUser.name,
                    role = sessionUser.role,
                    inventoryRepository = appContainer.inventoryRepository,
                    productId = productId,
                    onDashboardClick = {
                        navController.navigate(AppRoute.Dashboard.route) {
                            launchSingleTop = true
                        }
                    },
                    onProductsClick = {
                        navController.navigate(AppRoute.Products.route) {
                            popUpTo(AppRoute.Products.route) { inclusive = true }
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
                    onProfileClick = {
                        navController.navigate(AppRoute.AdminProfile.route) {
                            launchSingleTop = true
                        }
                    },
                    onSettingsClick = {
                        navController.navigate(AppRoute.AdminSettings.route) {
                            launchSingleTop = true
                        }
                    },                                    onBackToProducts = {
                        navController.navigate(AppRoute.Products.route) {
                            popUpTo(AppRoute.Products.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onEditProductClick = { targetProductId ->
                        navController.navigate(AppRoute.EditProduct.createRoute(targetProductId)) {
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

        composable(AppRoute.ProductCategories.route) {
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
                AdminProductCategoriesScreen(
                    name = sessionUser.name,
                    role = sessionUser.role,
                    inventoryRepository = appContainer.inventoryRepository,
                    onStockClick = {
                        navController.navigate(AppRoute.BackofficeStock.route) {
                            launchSingleTop = true
                        }
                    },
                    onDashboardClick = {
                        navController.navigate(AppRoute.Dashboard.route) {
                            launchSingleTop = true
                        }
                    },
                    onProductsClick = {
                        navController.navigate(AppRoute.Products.route) {
                            popUpTo(AppRoute.Products.route) { inclusive = true }
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

        composable(AppRoute.ProductUnits.route) {
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
                AdminProductUnitsScreen(
                    name = sessionUser.name,
                    role = sessionUser.role,
                    inventoryRepository = appContainer.inventoryRepository,
                    onStockClick = {
                        navController.navigate(AppRoute.BackofficeStock.route) {
                            launchSingleTop = true
                        }
                    },
                    onDashboardClick = {
                        navController.navigate(AppRoute.Dashboard.route) {
                            launchSingleTop = true
                        }
                    },
                    onProductsClick = {
                        navController.navigate(AppRoute.Products.route) {
                            popUpTo(AppRoute.Products.route) { inclusive = true }
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

        composable(AppRoute.StockOpname.route) {
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
                AdminStockOpnameScreen(
                    name = sessionUser.name,
                    role = sessionUser.role,
                    inventoryRepository = appContainer.inventoryRepository,
                    onStockClick = {
                        navController.navigate(AppRoute.BackofficeStock.route) {
                            launchSingleTop = true
                        }
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
                    },                                    onStockOpnameClick = {},
                    onStockOpnameFormClick = { productId ->
                        navController.navigate(AppRoute.StockOpnameForm.createRoute(productId)) {
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
            route = AppRoute.StockOpnameForm.route,
            arguments = listOf(
                navArgument(AppRoute.StockOpnameForm.PRODUCT_ID_ARG) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val sessionUser = sessionManager.readSessionUser()
            val productId = backStackEntry.arguments
                ?.getString(AppRoute.StockOpnameForm.PRODUCT_ID_ARG)

            if (sessionUser == null) {
                LaunchedEffect(Unit) {
                    sessionManager.logout()
                    navController.navigate(AppRoute.Login.route) {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                }
            } else {
                AdminStockOpnameFormScreen(
                    name = sessionUser.name,
                    role = sessionUser.role,
                    inventoryRepository = appContainer.inventoryRepository,
                    productId = productId,
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
                    onStockOpnameFormClick = {},
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
