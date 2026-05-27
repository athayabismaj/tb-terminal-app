package com.tbterminal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tbterminal.app.data.di.AppContainer
import com.tbterminal.app.data.session.SessionEvent
import com.tbterminal.app.data.session.SessionManager
import com.tbterminal.app.ui.admin.AdminBackofficePlaceholderScreen
import com.tbterminal.app.ui.auth.AuthViewModel
import com.tbterminal.app.ui.auth.LoginScreen
import com.tbterminal.app.ui.auth.PinScreen
import com.tbterminal.app.ui.cash.AdminCashReconciliationScreen
import com.tbterminal.app.ui.cashier.session.CashierCloseShiftScreen
import com.tbterminal.app.ui.cashier.stock.CashierStockCheckScreen
import com.tbterminal.app.ui.cashier.transactions.CashierReceiptDetailScreen
import com.tbterminal.app.ui.cashier.transactions.CashierTransactionHistoryScreen
import com.tbterminal.app.ui.checkout.CashierPosScreen
import com.tbterminal.app.ui.checkout.CashierCartScreen
import com.tbterminal.app.ui.checkout.CheckoutViewModel
import com.tbterminal.app.ui.customers.AdminCustomerDetailScreen
import com.tbterminal.app.ui.customers.AdminCustomerFormScreen
import com.tbterminal.app.ui.customers.AdminCustomerListScreen
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardScreen
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.cashier.CashierDashboardScreen
import com.tbterminal.app.ui.settings.CashierProfileScreen
import com.tbterminal.app.ui.settings.CashierSettingsScreen
import com.tbterminal.app.ui.dashboard.owner.OwnerDashboardScreen
import com.tbterminal.app.ui.incominggoods.AdminIncomingGoodsFormScreen
import com.tbterminal.app.ui.incominggoods.AdminIncomingGoodsScreen
import com.tbterminal.app.ui.payables.AdminSupplierDebtScreen
import com.tbterminal.app.ui.products.AdminProductCategoriesScreen
import com.tbterminal.app.ui.products.AdminProductDetailScreen
import com.tbterminal.app.ui.products.AdminProductFormScreen
import com.tbterminal.app.ui.products.AdminProductListScreen
import com.tbterminal.app.ui.products.AdminProductUnitsScreen
import com.tbterminal.app.ui.pricemanagement.AdminPriceManagementScreen
import com.tbterminal.app.ui.users.OwnerAddUserScreen
import com.tbterminal.app.ui.users.OwnerEditUserScreen
import com.tbterminal.app.ui.users.OwnerUserCredentialScreen
import com.tbterminal.app.ui.users.OwnerUserManagementScreen
import com.tbterminal.app.ui.users.UserCredentialMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as TbTerminalApplication).appContainer
        setContent {
            TbterminalappTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val authViewModel: AuthViewModel = viewModel(
                        factory = AuthViewModel.factory(appContainer.authRepository)
                    )
                    val sessionManager = appContainer.sessionManager
                    val startDestination = remember {
                        sessionManager.resolveStartDestination()
                    }

                    LaunchedEffect(navController) {
                        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                            sessionManager.events.collect { event ->
                                when (event) {
                                    SessionEvent.Unauthorized -> {
                                        navController.navigate(AppRoute.Login.route) {
                                            popUpTo(0)
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            }
                        }
                    }

                    DisposableEffect(navController, sessionManager) {
                        val observer = LifecycleEventObserver { _, event ->
                            when (event) {
                                Lifecycle.Event.ON_STOP -> sessionManager.lockForResume()
                                Lifecycle.Event.ON_START -> {
                                    if (sessionManager.requiresPinUnlock()) {
                                        navController.navigate(AppRoute.Pin.route) {
                                            popUpTo(0)
                                            launchSingleTop = true
                                        }
                                    }
                                }

                                else -> Unit
                            }
                        }

                        lifecycle.addObserver(observer)

                        onDispose {
                            lifecycle.removeObserver(observer)
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        composable(AppRoute.Login.route) {
                            LoginScreen(
                                viewModel = authViewModel,
                                onLoginSuccess = { _, _ ->
                                    navController.navigate(AppRoute.Dashboard.route) {
                                        popUpTo(AppRoute.Login.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(AppRoute.Pin.route) {
                            val sessionUser = sessionManager.readSessionUser()

                            if (sessionUser == null) {
                                LaunchedEffect(Unit) {
                                    sessionManager.clearSession()
                                    navController.navigate(AppRoute.Login.route) {
                                        popUpTo(0)
                                        launchSingleTop = true
                                    }
                                }
                            } else {
                                PinScreen(
                                    viewModel = authViewModel,
                                    userName = sessionUser.name,
                                    onUnlockSuccess = {
                                        navController.navigate(AppRoute.Dashboard.route) {
                                            popUpTo(AppRoute.Pin.route) { inclusive = true }
                                        }
                                    },
                                    onBackToLogin = {
                                        sessionManager.clearSession()
                                        navController.navigate(AppRoute.Login.route) {
                                            popUpTo(0)
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        }

                        composable(AppRoute.Dashboard.route) {
                            val sessionUser = sessionManager.readSessionUser()

                            if (sessionUser == null) {
                                LaunchedEffect(Unit) {
                                    sessionManager.clearSession()
                                    navController.navigate(AppRoute.Login.route) {
                                        popUpTo(0)
                                        launchSingleTop = true
                                    }
                                }
                            } else {
                                val logout: () -> Unit = {
                                    sessionManager.clearSession()
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
                                            onLogout = logout
                                        )
                                    }

                                    "admin" -> {
                                        AdminDashboardScreen(
                                            name = sessionUser.name,
                                            role = sessionUser.role,
                                            analyticsRepository = appContainer.analyticsRepository,
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
                                            sessionManager.clearSession()
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
                                    sessionManager.clearSession()
                                    navController.navigate(AppRoute.Login.route) {
                                        popUpTo(0)
                                        launchSingleTop = true
                                    }
                                }
                            } else {
                                CashierProfileScreen(
                                    userName = sessionUser.name,
                                    role = sessionUser.role,
                                    isActive = sessionUser.isActive,
                                    joinedAt = sessionUser.joinedAt,
                                    lastLoginAt = sessionUser.lastLoginAt,
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
                                        sessionManager.clearSession()
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
                                    sessionManager.clearSession()
                                    navController.navigate(AppRoute.Login.route) {
                                        popUpTo(0)
                                        launchSingleTop = true
                                    }
                                }
                            } else {
                                CashierSettingsScreen(
                                    userName = sessionUser.name,
                                    role = sessionUser.role,
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
                                        sessionManager.clearSession()
                                        navController.navigate(AppRoute.Login.route) {
                                            popUpTo(0)
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        }

                        composable(AppRoute.CashierPos.route) {
                            val sessionUser = sessionManager.readSessionUser()

                            if (sessionUser == null) {
                                LaunchedEffect(Unit) {
                                    sessionManager.clearSession()
                                    navController.navigate(AppRoute.Login.route) {
                                        popUpTo(0)
                                        launchSingleTop = true
                                    }
                                }
                            } else {
                                val logout: () -> Unit = {
                                    sessionManager.clearSession()
                                    navController.navigate(AppRoute.Login.route) {
                                        popUpTo(0)
                                        launchSingleTop = true
                                    }
                                }
                                val checkoutViewModel = cashierCheckoutViewModel(
                                    navController = navController,
                                    appContainer = appContainer
                                )

                                CashierPosScreen(
                                    name = sessionUser.name,
                                    role = sessionUser.role,
                                    checkoutRepository = appContainer.checkoutRepository,
                                    inventoryRepository = appContainer.inventoryRepository,
                                    customerRepository = appContainer.customerRepository,
                                    cashReconciliationRepository = appContainer.cashReconciliationRepository,
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

                        composable(AppRoute.CashierCashSession.route) {
                            val sessionUser = sessionManager.readSessionUser()

                            if (sessionUser == null) {
                                LaunchedEffect(Unit) {
                                    sessionManager.clearSession()
                                    navController.navigate(AppRoute.Login.route) {
                                        popUpTo(0)
                                        launchSingleTop = true
                                    }
                                }
                            } else {
                                val logout: () -> Unit = {
                                    sessionManager.clearSession()
                                    navController.navigate(AppRoute.Login.route) {
                                        popUpTo(0)
                                        launchSingleTop = true
                                    }
                                }

                                CashierCloseShiftScreen(
                                    name = sessionUser.name,
                                    role = sessionUser.role,
                                    cashReconciliationRepository = appContainer.cashReconciliationRepository,
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

                        composable(AppRoute.CashierStockCheck.route) {
                            val sessionUser = sessionManager.readSessionUser()

                            if (sessionUser == null) {
                                LaunchedEffect(Unit) {
                                    sessionManager.clearSession()
                                    navController.navigate(AppRoute.Login.route) {
                                        popUpTo(0)
                                        launchSingleTop = true
                                    }
                                }
                            } else {
                                val logout: () -> Unit = {
                                    sessionManager.clearSession()
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

                        composable(AppRoute.CashierTransactionHistory.route) {
                            val sessionUser = sessionManager.readSessionUser()

                            if (sessionUser == null) {
                                LaunchedEffect(Unit) {
                                    sessionManager.clearSession()
                                    navController.navigate(AppRoute.Login.route) {
                                        popUpTo(0)
                                        launchSingleTop = true
                                    }
                                }
                            } else {
                                val logout: () -> Unit = {
                                    sessionManager.clearSession()
                                    navController.navigate(AppRoute.Login.route) {
                                        popUpTo(0)
                                        launchSingleTop = true
                                    }
                                }

                                CashierTransactionHistoryScreen(
                                    name = sessionUser.name,
                                    role = sessionUser.role,
                                    cashReconciliationRepository = appContainer.cashReconciliationRepository,
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

                        composable(AppRoute.CashierCart.route) {
                            val sessionUser = sessionManager.readSessionUser()

                            if (sessionUser == null) {
                                LaunchedEffect(Unit) {
                                    sessionManager.clearSession()
                                    navController.navigate(AppRoute.Login.route) {
                                        popUpTo(0)
                                        launchSingleTop = true
                                    }
                                }
                            } else {
                                val logout: () -> Unit = {
                                    sessionManager.clearSession()
                                    navController.navigate(AppRoute.Login.route) {
                                        popUpTo(0)
                                        launchSingleTop = true
                                    }
                                }

                                CashierCartScreen(
                                    name = sessionUser.name,
                                    role = sessionUser.role,
                                    checkoutRepository = appContainer.checkoutRepository,
                                    inventoryRepository = appContainer.inventoryRepository,
                                    customerRepository = appContainer.customerRepository,
                                    cashReconciliationRepository = appContainer.cashReconciliationRepository,
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
                                    sessionManager.clearSession()
                                    navController.navigate(AppRoute.Login.route) {
                                        popUpTo(0)
                                        launchSingleTop = true
                                    }
                                }
                            } else {
                                val logout: () -> Unit = {
                                    sessionManager.clearSession()
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

                        composable(AppRoute.Products.route) {
                            val sessionUser = sessionManager.readSessionUser()
                            if (sessionUser == null) {
                                navController.navigate(AppRoute.Login.route) { popUpTo(0) { inclusive = true } }
                            } else {
                                AdminProductListScreen(
                                    name = sessionUser.name,
                                    role = sessionUser.role,
                                    inventoryRepository = appContainer.inventoryRepository,
                                    onDashboardClick = { navController.navigate(AppRoute.Dashboard.route) { launchSingleTop = true } },
                                    onProductsClick = { },
                                    onAddProductClick = { navController.navigate(AppRoute.AddProduct.route) },
                                    onEditProductClick = { productId -> navController.navigate(AppRoute.EditProduct.createRoute(productId)) },
                                    onProductDetailClick = { productId -> navController.navigate(AppRoute.ProductDetail.createRoute(productId)) },
                                    onCategoriesClick = { navController.navigate(AppRoute.ProductCategories.route) },
                                    onUnitsClick = { navController.navigate(AppRoute.ProductUnits.route) },
                                    onStockOpnameClick = { navController.navigate(AppRoute.StockOpname.route) },
                                    onStockOpnameFormClick = { navController.navigate(AppRoute.StockOpnameForm.createRoute()) },
                                    onIncomingGoodsClick = { navController.navigate(AppRoute.IncomingGoods.route) },
                                    onIncomingGoodsFormClick = { navController.navigate(AppRoute.IncomingGoodsForm.createRoute()) },
                                    onSupplierDebtsClick = { navController.navigate(AppRoute.SupplierDebts.route) },
                                    onReceivablesClick = { navController.navigate(AppRoute.Receivables.route) },
                                    onCustomersClick = { navController.navigate(AppRoute.Customers.route) },
                                    onOperationalAuditClick = { navController.navigate(AppRoute.OperationalAudit.route) },
                                    onProfileClick = { navController.navigate(AppRoute.AdminProfile.route) },
                                    onSettingsClick = { navController.navigate(AppRoute.AdminSettings.route) },
                                    onLogout = {
                                        sessionManager.clearSession()
                                        navController.navigate(AppRoute.Login.route) { popUpTo(0) { inclusive = true } }
                                    }
                                )
                            }
                        }

                        composable(AppRoute.AddProduct.route) {
                            val sessionUser = sessionManager.readSessionUser()
                            if (sessionUser == null) {
                                navController.navigate(AppRoute.Login.route) { popUpTo(0) { inclusive = true } }
                            } else {
                                AdminProductFormScreen(
                                    name = sessionUser.name,
                                    role = sessionUser.role,
                                    inventoryRepository = appContainer.inventoryRepository,
                                    productId = null,
                                    onDashboardClick = { navController.navigate(AppRoute.Dashboard.route) { launchSingleTop = true } },
                                    onProductsClick = { navController.navigate(AppRoute.Products.route) { launchSingleTop = true } },
                                    onAddProductClick = { },
                                    onProductCategoriesClick = { navController.navigate(AppRoute.ProductCategories.route) },
                                    onProductUnitsClick = { navController.navigate(AppRoute.ProductUnits.route) },
                                    onStockOpnameClick = { navController.navigate(AppRoute.StockOpname.route) },
                                    onStockOpnameFormClick = { navController.navigate(AppRoute.StockOpnameForm.createRoute()) },
                                    onIncomingGoodsClick = { navController.navigate(AppRoute.IncomingGoods.route) },
                                    onIncomingGoodsFormClick = { navController.navigate(AppRoute.IncomingGoodsForm.createRoute()) },
                                    onBackToProducts = { navController.navigateUp() },
                                    onSettingsClick = { navController.navigate(AppRoute.AdminSettings.route) },
                                    onLogout = {
                                        sessionManager.clearSession()
                                        navController.navigate(AppRoute.Login.route) { popUpTo(0) { inclusive = true } }
                                    }
                                )
                            }
                        }

                        composable(AppRoute.EditProduct.route) { backStackEntry ->
                            val sessionUser = sessionManager.readSessionUser()
                            val productId = backStackEntry.arguments?.getString(AppRoute.EditProduct.PRODUCT_ID_ARG)
                            
                            if (sessionUser == null) {
                                navController.navigate(AppRoute.Login.route) { popUpTo(0) { inclusive = true } }
                            } else if (productId != null) {
                                AdminProductFormScreen(
                                    name = sessionUser.name,
                                    role = sessionUser.role,
                                    inventoryRepository = appContainer.inventoryRepository,
                                    productId = productId,
                                    onDashboardClick = { navController.navigate(AppRoute.Dashboard.route) { launchSingleTop = true } },
                                    onProductsClick = { navController.navigate(AppRoute.Products.route) { launchSingleTop = true } },
                                    onAddProductClick = { navController.navigate(AppRoute.AddProduct.route) },
                                    onProductCategoriesClick = { navController.navigate(AppRoute.ProductCategories.route) },
                                    onProductUnitsClick = { navController.navigate(AppRoute.ProductUnits.route) },
                                    onStockOpnameClick = { navController.navigate(AppRoute.StockOpname.route) },
                                    onStockOpnameFormClick = { navController.navigate(AppRoute.StockOpnameForm.createRoute()) },
                                    onIncomingGoodsClick = { navController.navigate(AppRoute.IncomingGoods.route) },
                                    onIncomingGoodsFormClick = { navController.navigate(AppRoute.IncomingGoodsForm.createRoute()) },
                                    onBackToProducts = { navController.navigateUp() },
                                    onSettingsClick = { navController.navigate(AppRoute.AdminSettings.route) },
                                    onLogout = {
                                        sessionManager.clearSession()
                                        navController.navigate(AppRoute.Login.route) { popUpTo(0) { inclusive = true } }
                                    }
                                )
                            }
                        }

                        composable(AppRoute.ProductDetail.route) { backStackEntry ->
                            val sessionUser = sessionManager.readSessionUser()
                            val productId = backStackEntry.arguments?.getString(AppRoute.ProductDetail.PRODUCT_ID_ARG)
                            
                            if (sessionUser == null) {
                                navController.navigate(AppRoute.Login.route) { popUpTo(0) { inclusive = true } }
                            } else if (productId != null) {
                                AdminProductDetailScreen(
                                    name = sessionUser.name,
                                    role = sessionUser.role,
                                    inventoryRepository = appContainer.inventoryRepository,
                                    productId = productId,
                                    onDashboardClick = { navController.navigate(AppRoute.Dashboard.route) { launchSingleTop = true } },
                                    onProductsClick = { navController.navigate(AppRoute.Products.route) { launchSingleTop = true } },
                                    onAddProductClick = { navController.navigate(AppRoute.AddProduct.route) },
                                    onProductCategoriesClick = { navController.navigate(AppRoute.ProductCategories.route) },
                                    onProductUnitsClick = { navController.navigate(AppRoute.ProductUnits.route) },
                                    onStockOpnameClick = { navController.navigate(AppRoute.StockOpname.route) },
                                    onStockOpnameFormClick = { navController.navigate(AppRoute.StockOpnameForm.createRoute()) },
                                    onIncomingGoodsClick = { navController.navigate(AppRoute.IncomingGoods.route) },
                                    onIncomingGoodsFormClick = { navController.navigate(AppRoute.IncomingGoodsForm.createRoute()) },
                                    onBackToProducts = { navController.navigateUp() },
                                    onEditProductClick = { pId -> navController.navigate(AppRoute.EditProduct.createRoute(pId)) },
                                    onSettingsClick = { navController.navigate(AppRoute.AdminSettings.route) },
                                    onLogout = {
                                        sessionManager.clearSession()
                                        navController.navigate(AppRoute.Login.route) { popUpTo(0) { inclusive = true } }
                                    }
                                )
                            }
                        }

                        composable(AppRoute.ProductCategories.route) {
                            val sessionUser = sessionManager.readSessionUser()
                            if (sessionUser == null) {
                                navController.navigate(AppRoute.Login.route) { popUpTo(0) { inclusive = true } }
                            } else {
                                AdminProductCategoriesScreen(
                                    name = sessionUser.name,
                                    role = sessionUser.role,
                                    inventoryRepository = appContainer.inventoryRepository,
                                    onDashboardClick = { navController.navigate(AppRoute.Dashboard.route) { launchSingleTop = true } },
                                    onProductsClick = { navController.navigate(AppRoute.Products.route) { launchSingleTop = true } },
                                    onAddProductClick = { navController.navigate(AppRoute.AddProduct.route) },
                                    onProductCategoriesClick = { },
                                    onProductUnitsClick = { navController.navigate(AppRoute.ProductUnits.route) },
                                    onStockOpnameClick = { navController.navigate(AppRoute.StockOpname.route) },
                                    onStockOpnameFormClick = { navController.navigate(AppRoute.StockOpnameForm.createRoute()) },
                                    onIncomingGoodsClick = { navController.navigate(AppRoute.IncomingGoods.route) },
                                    onIncomingGoodsFormClick = { navController.navigate(AppRoute.IncomingGoodsForm.createRoute()) },
                                    onSettingsClick = { navController.navigate(AppRoute.AdminSettings.route) },
                                    onLogout = {
                                        sessionManager.clearSession()
                                        navController.navigate(AppRoute.Login.route) { popUpTo(0) { inclusive = true } }
                                    }
                                )
                            }
                        }

                        composable(AppRoute.ProductUnits.route) {
                            val sessionUser = sessionManager.readSessionUser()
                            if (sessionUser == null) {
                                navController.navigate(AppRoute.Login.route) { popUpTo(0) { inclusive = true } }
                            } else {
                                AdminProductUnitsScreen(
                                    name = sessionUser.name,
                                    role = sessionUser.role,
                                    inventoryRepository = appContainer.inventoryRepository,
                                    onDashboardClick = { navController.navigate(AppRoute.Dashboard.route) { launchSingleTop = true } },
                                    onProductsClick = { navController.navigate(AppRoute.Products.route) { launchSingleTop = true } },
                                    onAddProductClick = { navController.navigate(AppRoute.AddProduct.route) },
                                    onProductCategoriesClick = { navController.navigate(AppRoute.ProductCategories.route) },
                                    onProductUnitsClick = { },
                                    onStockOpnameClick = { navController.navigate(AppRoute.StockOpname.route) },
                                    onStockOpnameFormClick = { navController.navigate(AppRoute.StockOpnameForm.createRoute()) },
                                    onIncomingGoodsClick = { navController.navigate(AppRoute.IncomingGoods.route) },
                                    onIncomingGoodsFormClick = { navController.navigate(AppRoute.IncomingGoodsForm.createRoute()) },
                                    onSettingsClick = { navController.navigate(AppRoute.AdminSettings.route) },
                                    onLogout = {
                                        sessionManager.clearSession()
                                        navController.navigate(AppRoute.Login.route) { popUpTo(0) { inclusive = true } }
                                    }
                                )
                            }
                        }
                        composable(AppRoute.CashReconciliation.route) {
                            val sessionUser = sessionManager.readSessionUser()

                            if (sessionUser == null) {
                                LaunchedEffect(Unit) {
                                    sessionManager.clearSession()
                                    navController.navigate(AppRoute.Login.route) {
                                        popUpTo(0)
                                        launchSingleTop = true
                                    }
                                }
                            } else {
                                val logout: () -> Unit = {
                                    sessionManager.clearSession()
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
                        }

                        composable(AppRoute.SalesTransactions.route) {
                            val sessionUser = sessionManager.readSessionUser()

                            if (sessionUser == null) {
                                LaunchedEffect(Unit) {
                                    sessionManager.clearSession()
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
                                        sessionManager.clearSession()
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
                                    sessionManager.clearSession()
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
                                        sessionManager.clearSession()
                                        navController.navigate(AppRoute.Login.route) {
                                            popUpTo(0)
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        }

                        composable(AppRoute.Reports.route) {
                            AdminPlaceholderRoute(
                                sessionManager = sessionManager,
                                navController = navController,
                                destination = AdminDestination.Reports,
                                title = "Laporan",
                                subtitle = "Konsolidasi penjualan, stok, laba kotor, hutang, dan piutang.",
                                badgeText = "Analitik toko",
                                focusItems = listOf(
                                    "Laporan penjualan harian, mingguan, dan bulanan.",
                                    "Laporan stok rendah dan pergerakan stok.",
                                    "Laba kotor berbasis harga modal transaksi.",
                                    "Daftar hutang dan piutang jatuh tempo."
                                ),
                                integrationNotes = listOf(
                                    "Butuh endpoint agregasi agar tablet tidak menghitung data mentah besar.",
                                    "Butuh filter periode dan ekspor ringkas.",
                                    "Butuh validasi role untuk laporan finansial sensitif."
                                )
                            )
                        }

                        composable(AppRoute.PriceManagement.route) {
                            AdminPlaceholderRoute(
                                sessionManager = sessionManager,
                                navController = navController,
                                destination = AdminDestination.PriceManagement,
                                title = "Manajemen Harga",
                                subtitle = "Kelola harga retail, kontraktor, dan riwayat perubahan harga.",
                                badgeText = "Kontrol harga",
                                focusItems = listOf(
                                    "Perubahan harga retail dan harga kontraktor.",
                                    "Histori harga per produk dan siapa yang mengubah.",
                                    "Validasi margin terhadap harga modal terakhir.",
                                    "Opsi approval owner untuk perubahan besar."
                                ),
                                integrationNotes = listOf(
                                    "Produk sudah punya harga dasar, tapi histori harga perlu endpoint khusus.",
                                    "Butuh audit log perubahan harga.",
                                    "Butuh aturan validasi margin minimum dari backend."
                                )
                            )
                        }

                        composable(AppRoute.OperationalAudit.route) {
                            AdminPlaceholderRoute(
                                sessionManager = sessionManager,
                                navController = navController,
                                destination = AdminDestination.OperationalAudit,
                                title = "Audit Operasional",
                                subtitle = "Pantau perubahan stok, harga, transaksi, pembayaran, dan master data.",
                                badgeText = "Jejak aktivitas",
                                focusItems = listOf(
                                    "Log perubahan stok opname, retur/rusak, dan restok.",
                                    "Log perubahan harga, produk, kategori, dan satuan.",
                                    "Log pembayaran hutang dan piutang.",
                                    "Filter user, modul, tanggal, dan jenis aktivitas."
                                ),
                                integrationNotes = listOf(
                                    "Log keamanan sudah ada; modul ini perlu audit operasional terpisah.",
                                    "Butuh endpoint paginated agar ringan di tablet.",
                                    "Butuh payload detail sebelum/sesudah untuk investigasi."
                                )
                            )
                        }

                        composable(AppRoute.AdminProfile.route) {
                            AdminPlaceholderRoute(
                                sessionManager = sessionManager,
                                navController = navController,
                                destination = AdminDestination.Profile,
                                title = "Profil Akun",
                                subtitle = "Lihat dan ubah informasi profil Anda.",
                                badgeText = "Profil",
                                focusItems = listOf("Informasi Nama, Role, Email", "Ubah Password"),
                                integrationNotes = listOf("Butuh endpoint update profil")
                            )
                        }

                        composable(AppRoute.AdminSettings.route) {
                            AdminPlaceholderRoute(
                                sessionManager = sessionManager,
                                navController = navController,
                                destination = AdminDestination.Settings,
                                title = "Pengaturan",
                                subtitle = "Atur profil toko, printer, struk, metode pembayaran, dan nomor dokumen.",
                                badgeText = "Konfigurasi toko",
                                focusItems = listOf(
                                    "Profil toko dan informasi yang muncul di struk.",
                                    "Printer struk dan opsi cetak otomatis.",
                                    "Metode pembayaran aktif untuk kasir.",
                                    "Format nomor nota POS, barang masuk, dan dokumen lain."
                                ),
                                integrationNotes = listOf(
                                    "Butuh endpoint konfigurasi toko.",
                                    "Butuh penyimpanan lokal untuk preferensi printer terminal.",
                                    "Butuh fail-closed default jika konfigurasi pembayaran belum valid."
                                )
                            )
                        }

                        composable(AppRoute.Products.route) {
                            val sessionUser = sessionManager.readSessionUser()

                            if (sessionUser == null) {
                                LaunchedEffect(Unit) {
                                    sessionManager.clearSession()
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
                                        sessionManager.clearSession()
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
                                    sessionManager.clearSession()
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
                                        sessionManager.clearSession()
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
                                    sessionManager.clearSession()
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
                                        sessionManager.clearSession()
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
                                    sessionManager.clearSession()
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
                                        sessionManager.clearSession()
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
                                    sessionManager.clearSession()
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
                                        sessionManager.clearSession()
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
                                    sessionManager.clearSession()
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
                                        sessionManager.clearSession()
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
                                    sessionManager.clearSession()
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
                                        sessionManager.clearSession()
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
                                    sessionManager.clearSession()
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
                                        sessionManager.clearSession()
                                        navController.navigate(AppRoute.Login.route) {
                                            popUpTo(0)
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        }

                        composable(AppRoute.IncomingGoods.route) {
                            val sessionUser = sessionManager.readSessionUser()

                            if (sessionUser == null) {
                                LaunchedEffect(Unit) {
                                    sessionManager.clearSession()
                                    navController.navigate(AppRoute.Login.route) {
                                        popUpTo(0)
                                        launchSingleTop = true
                                    }
                                }
                            } else {
                                AdminIncomingGoodsScreen(
                                    name = sessionUser.name,
                                    role = sessionUser.role,
                                    inventoryRepository = appContainer.inventoryRepository,
                                    purchasingRepository = appContainer.purchasingRepository,
                                    documentNumberGenerator = appContainer.documentNumberGenerator,
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
                                    onIncomingGoodsClick = {},
                                    onIncomingGoodsFormClick = { productId ->
                                        navController.navigate(AppRoute.IncomingGoodsForm.createRoute(productId)) {
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
                                        sessionManager.clearSession()
                                        navController.navigate(AppRoute.Login.route) {
                                            popUpTo(0)
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        }

                        composable(
                            route = AppRoute.IncomingGoodsForm.route,
                            arguments = listOf(
                                navArgument(AppRoute.IncomingGoodsForm.PRODUCT_ID_ARG) {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStackEntry ->
                            val sessionUser = sessionManager.readSessionUser()
                            val productId = backStackEntry.arguments
                                ?.getString(AppRoute.IncomingGoodsForm.PRODUCT_ID_ARG)

                            if (sessionUser == null) {
                                LaunchedEffect(Unit) {
                                    sessionManager.clearSession()
                                    navController.navigate(AppRoute.Login.route) {
                                        popUpTo(0)
                                        launchSingleTop = true
                                    }
                                }
                            } else {
                                AdminIncomingGoodsFormScreen(
                                    name = sessionUser.name,
                                    role = sessionUser.role,
                                    inventoryRepository = appContainer.inventoryRepository,
                                    purchasingRepository = appContainer.purchasingRepository,
                                    documentNumberGenerator = appContainer.documentNumberGenerator,
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
                                    onIncomingGoodsFormClick = {},
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
                                        sessionManager.clearSession()
                                        navController.navigate(AppRoute.Login.route) {
                                            popUpTo(0)
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        }

                        composable(AppRoute.SupplierDebts.route) {
                            val sessionUser = sessionManager.readSessionUser()

                            if (sessionUser == null) {
                                LaunchedEffect(Unit) {
                                    sessionManager.clearSession()
                                    navController.navigate(AppRoute.Login.route) {
                                        popUpTo(0)
                                        launchSingleTop = true
                                    }
                                }
                            } else {
                                AdminSupplierDebtScreen(
                                    name = sessionUser.name,
                                    role = sessionUser.role,
                                    purchasingRepository = appContainer.purchasingRepository,
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
                                    onSupplierDebtsClick = {},
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
                                        sessionManager.clearSession()
                                        navController.navigate(AppRoute.Login.route) {
                                            popUpTo(0)
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        }

                        composable(AppRoute.Receivables.route) {
                            val sessionUser = sessionManager.readSessionUser()

                            if (sessionUser == null) {
                                LaunchedEffect(Unit) {
                                    sessionManager.clearSession()
                                    navController.navigate(AppRoute.Login.route) {
                                        popUpTo(0)
                                        launchSingleTop = true
                                    }
                                }
                            } else {
                                AdminReceivableScreen(
                                    name = sessionUser.name,
                                    role = sessionUser.role,
                                    receivableRepository = appContainer.receivableRepository,
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
                                        sessionManager.clearSession()
                                        navController.navigate(AppRoute.Login.route) {
                                            popUpTo(0)
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        }

                        composable(AppRoute.Customers.route) {
                            val sessionUser = sessionManager.readSessionUser()

                            if (sessionUser == null) {
                                LaunchedEffect(Unit) {
                                    sessionManager.clearSession()
                                    navController.navigate(AppRoute.Login.route) {
                                        popUpTo(0)
                                        launchSingleTop = true
                                    }
                                }
                            } else {
                                AdminCustomerListScreen(
                                    name = sessionUser.name,
                                    role = sessionUser.role,
                                    customerRepository = appContainer.customerRepository,
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
                                    onReceivablesClick = {
                                        navController.navigate(AppRoute.Receivables.route) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onCustomersClick = {},
                                    onAddCustomerClick = {
                                        navController.navigate(AppRoute.AddCustomer.route) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onEditCustomerClick = { customerId ->
                                        navController.navigate(AppRoute.EditCustomer.createRoute(customerId)) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onCustomerDetailClick = { customerId ->
                                        navController.navigate(AppRoute.CustomerDetail.createRoute(customerId)) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onLogout = {
                                        sessionManager.clearSession()
                                        navController.navigate(AppRoute.Login.route) {
                                            popUpTo(0)
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        }

                        composable(AppRoute.AddCustomer.route) {
                            val sessionUser = sessionManager.readSessionUser()

                            if (sessionUser == null) {
                                LaunchedEffect(Unit) {
                                    sessionManager.clearSession()
                                    navController.navigate(AppRoute.Login.route) {
                                        popUpTo(0)
                                        launchSingleTop = true
                                    }
                                }
                            } else {
                                AdminCustomerFormScreen(
                                    name = sessionUser.name,
                                    role = sessionUser.role,
                                    customerId = null,
                                    customerRepository = appContainer.customerRepository,
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
                                    },                                    onBackToCustomers = {
                                        navController.navigate(AppRoute.Customers.route) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onLogout = {
                                        sessionManager.clearSession()
                                        navController.navigate(AppRoute.Login.route) {
                                            popUpTo(0)
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        }

                        composable(
                            route = AppRoute.EditCustomer.route,
                            arguments = listOf(
                                navArgument(AppRoute.EditCustomer.CUSTOMER_ID_ARG) {
                                    type = NavType.StringType
                                }
                            )
                        ) { backStackEntry ->
                            val sessionUser = sessionManager.readSessionUser()
                            val customerId = backStackEntry.arguments
                                ?.getString(AppRoute.EditCustomer.CUSTOMER_ID_ARG)

                            if (sessionUser == null || customerId.isNullOrBlank()) {
                                LaunchedEffect(Unit) {
                                    sessionManager.clearSession()
                                    navController.navigate(AppRoute.Login.route) {
                                        popUpTo(0)
                                        launchSingleTop = true
                                    }
                                }
                            } else {
                                AdminCustomerFormScreen(
                                    name = sessionUser.name,
                                    role = sessionUser.role,
                                    customerId = customerId,
                                    customerRepository = appContainer.customerRepository,
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
                                    },                                    onBackToCustomers = {
                                        navController.navigate(AppRoute.Customers.route) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onLogout = {
                                        sessionManager.clearSession()
                                        navController.navigate(AppRoute.Login.route) {
                                            popUpTo(0)
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        }

                        composable(
                            route = AppRoute.CustomerDetail.route,
                            arguments = listOf(
                                navArgument(AppRoute.CustomerDetail.CUSTOMER_ID_ARG) {
                                    type = NavType.StringType
                                }
                            )
                        ) { backStackEntry ->
                            val sessionUser = sessionManager.readSessionUser()
                            val customerId = backStackEntry.arguments
                                ?.getString(AppRoute.CustomerDetail.CUSTOMER_ID_ARG)

                            if (sessionUser == null || customerId.isNullOrBlank()) {
                                LaunchedEffect(Unit) {
                                    sessionManager.clearSession()
                                    navController.navigate(AppRoute.Login.route) {
                                        popUpTo(0)
                                        launchSingleTop = true
                                    }
                                }
                            } else {
                                AdminCustomerDetailScreen(
                                    name = sessionUser.name,
                                    role = sessionUser.role,
                                    customerId = customerId,
                                    customerRepository = appContainer.customerRepository,
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
                                    },                                    onEditCustomerClick = { targetCustomerId ->
                                        navController.navigate(AppRoute.EditCustomer.createRoute(targetCustomerId)) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onLogout = {
                                        sessionManager.clearSession()
                                        navController.navigate(AppRoute.Login.route) {
                                            popUpTo(0)
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        }

                        composable(AppRoute.UserManagement.route) {
                            val sessionUser = sessionManager.readSessionUser()

                            if (sessionUser == null) {
                                LaunchedEffect(Unit) {
                                    sessionManager.clearSession()
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
                                    onLogout = {
                                        sessionManager.clearSession()
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
                                    sessionManager.clearSession()
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
                                        sessionManager.clearSession()
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
                                    sessionManager.clearSession()
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
                                    onLogout = {
                                        sessionManager.clearSession()
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
                                    sessionManager.clearSession()
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
                                    onLogout = {
                                        sessionManager.clearSession()
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
                                    sessionManager.clearSession()
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
                                    onLogout = {
                                        sessionManager.clearSession()
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
                                    sessionManager.clearSession()
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
                                    onLogout = {
                                        sessionManager.clearSession()
                                        navController.navigate(AppRoute.Login.route) {
                                            popUpTo(0)
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun cashierCheckoutViewModel(
    navController: NavHostController,
    appContainer: AppContainer
): CheckoutViewModel {
    val cashierOwner = remember(navController) {
        navController.getBackStackEntry(AppRoute.CashierPos.route)
    }

    return viewModel(
        viewModelStoreOwner = cashierOwner,
        factory = CheckoutViewModel.factory(
            checkoutRepository = appContainer.checkoutRepository,
            inventoryRepository = appContainer.inventoryRepository,
            customerRepository = appContainer.customerRepository,
            cashReconciliationRepository = appContainer.cashReconciliationRepository
        )
    )
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
            sessionManager.clearSession()
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
            onSupplierDebtsClick = { navigate(AppRoute.SupplierDebts.route) },
            onReceivablesClick = { navigate(AppRoute.Receivables.route) },
            onCustomersClick = { navigate(AppRoute.Customers.route) },
            onOperationalAuditClick = { navigate(AppRoute.OperationalAudit.route) },
            onProfileClick = { navigate(AppRoute.AdminProfile.route) },
            onSettingsClick = { navigate(AppRoute.AdminSettings.route) },
            onLogout = {
                sessionManager.clearSession()
                navController.navigate(AppRoute.Login.route) {
                    popUpTo(0)
                    launchSingleTop = true
                }
            }
        )
    }
}

private sealed interface AppRoute {
    data object AdminReceiptDetail : AppRoute {
        override val route = "admin/sales/receipt/{transactionId}"
        fun createRoute(transactionId: String) = "admin/sales/receipt/$transactionId"
    }
    val route: String

    data object Login : AppRoute {
        override val route = "login"
    }

    data object Pin : AppRoute {
        override val route = "pin"
    }

    data object Dashboard : AppRoute {
        override val route = "dashboard"
    }

    data object CashReconciliation : AppRoute {
        override val route = "admin/cash-reconciliation"
    }

    data object SalesTransactions : AppRoute {
        override val route = "admin/sales-transactions"
    }

    data object Reports : AppRoute {
        override val route = "admin/reports"
    }

    data object PriceManagement : AppRoute {
        override val route = "admin/price-management"
    }

    data object OperationalAudit : AppRoute {
        override val route = "admin/operational-audit"
    }

    data object CashierProfile : AppRoute { override val route = "cashier_profile" }
    data object CashierSettings : AppRoute { override val route = "cashier_settings" }
    
    data object AdminProfile : AppRoute { override val route = "admin/profile" }
    data object AdminSettings : AppRoute {
        override val route = "admin/settings"
    }

    data object CashierPos : AppRoute {
        override val route = "cashier/pos"
    }

    data object CashierCashSession : AppRoute {
        override val route = "cashier/cash-session"
    }

    data object CashierCart : AppRoute {
        override val route = "cashier/cart"
    }

    data object CashierStockCheck : AppRoute {
        override val route = "cashier/stock-check"
    }

    data object CashierTransactionHistory : AppRoute {
        override val route = "cashier/transactions"
    }

    data object CashierReceiptDetail : AppRoute {
        const val TRANSACTION_ID_ARG = "transactionId"
        override val route = "cashier/transactions/{$TRANSACTION_ID_ARG}"

        fun createRoute(transactionId: String): String {
            return "cashier/transactions/$transactionId"
        }
    }

    data object UserManagement : AppRoute {
        override val route = "user-management"
    }

    data object SecurityLog : AppRoute {
        override val route = "security-log"
    }

    data object Products : AppRoute {
        override val route = "admin/products"
    }

    data object AddProduct : AppRoute {
        override val route = "admin/products/add"
    }

    data object EditProduct : AppRoute {
        const val PRODUCT_ID_ARG = "productId"
        override val route = "admin/products/edit/{$PRODUCT_ID_ARG}"

        fun createRoute(productId: String): String {
            return "admin/products/edit/$productId"
        }
    }

    data object ProductDetail : AppRoute {
        const val PRODUCT_ID_ARG = "productId"
        override val route = "admin/products/detail/{$PRODUCT_ID_ARG}"

        fun createRoute(productId: String): String {
            return "admin/products/detail/$productId"
        }
    }

    data object ProductCategories : AppRoute {
        override val route = "admin/products/categories"
    }

    data object ProductUnits : AppRoute {
        override val route = "admin/products/units"
    }

    data object StockOpname : AppRoute {
        override val route = "admin/stock-opname"
    }

    data object StockOpnameForm : AppRoute {
        const val PRODUCT_ID_ARG = "productId"
        override val route = "admin/stock-opname/form?$PRODUCT_ID_ARG={$PRODUCT_ID_ARG}"

        fun createRoute(productId: String? = null): String {
            return if (productId.isNullOrBlank()) {
                "admin/stock-opname/form"
            } else {
                "admin/stock-opname/form?$PRODUCT_ID_ARG=$productId"
            }
        }
    }

    data object IncomingGoods : AppRoute {
        override val route = "admin/incoming-goods"
    }

    data object IncomingGoodsForm : AppRoute {
        const val PRODUCT_ID_ARG = "productId"
        override val route = "admin/incoming-goods/form?$PRODUCT_ID_ARG={$PRODUCT_ID_ARG}"

        fun createRoute(productId: String? = null): String {
            return if (productId.isNullOrBlank()) {
                "admin/incoming-goods/form"
            } else {
                "admin/incoming-goods/form?$PRODUCT_ID_ARG=$productId"
            }
        }
    }

    data object SupplierDebts : AppRoute {
        override val route = "admin/supplier-debts"
    }

    data object Receivables : AppRoute {
        override val route = "admin/receivables"
    }

    data object Customers : AppRoute {
        override val route = "admin/customers"
    }

    data object AddCustomer : AppRoute {
        override val route = "admin/customers/add"
    }

    data object EditCustomer : AppRoute {
        const val CUSTOMER_ID_ARG = "customerId"
        override val route = "admin/customers/edit/{$CUSTOMER_ID_ARG}"

        fun createRoute(customerId: String): String {
            return "admin/customers/edit/$customerId"
        }
    }

    data object CustomerDetail : AppRoute {
        const val CUSTOMER_ID_ARG = "customerId"
        override val route = "admin/customers/detail/{$CUSTOMER_ID_ARG}"

        fun createRoute(customerId: String): String {
            return "admin/customers/detail/$customerId"
        }
    }

    data object AddUser : AppRoute {
        override val route = "user-management/add"
    }

    data object EditUser : AppRoute {
        const val USER_ID_ARG = "userId"
        override val route = "user-management/edit/{$USER_ID_ARG}"

        fun createRoute(userId: String): String {
            return "user-management/edit/$userId"
        }
    }

    data object ChangeUserPassword : AppRoute {
        const val USER_ID_ARG = "userId"
        override val route = "user-management/edit/{$USER_ID_ARG}/password"

        fun createRoute(userId: String): String {
            return "user-management/edit/$userId/password"
        }
    }

    data object ChangeUserPin : AppRoute {
        const val USER_ID_ARG = "userId"
        override val route = "user-management/edit/{$USER_ID_ARG}/pin"

        fun createRoute(userId: String): String {
            return "user-management/edit/$userId/pin"
        }
    }
}

private fun com.tbterminal.app.data.session.SessionManager.resolveStartDestination(): String {
    if (!hasAccessToken()) {
        return AppRoute.Login.route
    }

    if (readSessionUser() == null) {
        clearSession()
        return AppRoute.Login.route
    }

    return if (requiresPinUnlock()) {
        AppRoute.Pin.route
    } else {
        AppRoute.Dashboard.route
    }
}

private fun String.normalizedRole(): String {
    return trim().lowercase()
}


