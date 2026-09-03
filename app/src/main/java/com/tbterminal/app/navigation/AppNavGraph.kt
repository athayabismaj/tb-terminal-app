package com.tbterminal.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import com.tbterminal.app.data.di.AppContainer
import com.tbterminal.app.data.session.SessionManager
import com.tbterminal.app.ui.auth.AuthViewModel
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String,
    authViewModel: AuthViewModel,
    sessionManager: SessionManager,
    appContainer: AppContainer
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val sessionRole = sessionManager.readSessionUser()?.role

    LaunchedEffect(currentRoute, sessionRole) {
        if (currentRoute != null && !AppRouteAccessPolicy.isAllowed(currentRoute, sessionRole)) {
            val fallback = if (sessionManager.hasAccessToken() && sessionRole != null) {
                AppRoute.Dashboard.route
            } else {
                AppRoute.Login.route
            }
            navController.navigate(fallback) {
                popUpTo(currentRoute) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    CompositionLocalProvider(
        LocalAdminDestinationNavigator provides { destination ->
            destination.adminRouteOrNull(sessionRole)?.let { route ->
                val destinationRoute = if (AppRouteAccessPolicy.isAllowed(route, sessionRole)) {
                    route
                } else {
                    AppRoute.Dashboard.route
                }
                navController.navigate(destinationRoute) {
                    launchSingleTop = true
                    if (destinationRoute != AppRoute.Dashboard.route) {
                        popUpTo(AppRoute.Dashboard.route) {
                            saveState = false
                        }
                    }
                }
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            authGraph(navController, authViewModel, sessionManager)
            dashboardGraph(navController, sessionManager, appContainer)
            checkoutGraph(navController, sessionManager, appContainer)
            inventoryGraph(navController, sessionManager, appContainer)
            salesGraph(navController, sessionManager, appContainer)
            purchasingGraph(navController, sessionManager, appContainer)
            receivableGraph(navController, sessionManager, appContainer)
            auditGraph(navController, sessionManager, appContainer)
            userGraph(navController, sessionManager, appContainer)
        }
    }
}

private fun AdminDestination.adminRouteOrNull(role: String?): String? {
    return when (this) {
        AdminDestination.Dashboard -> AppRoute.Dashboard.route
        AdminDestination.TransactionsHub -> AppRoute.BackofficeTransactions.route
        AdminDestination.FinanceHub -> AppRoute.BackofficeFinance.route
        AdminDestination.StockHub -> AppRoute.BackofficeStock.route
        AdminDestination.MoreHub -> AppRoute.BackofficeMore.route
        AdminDestination.NewTransaction -> AppRoute.CashierPos.route
        AdminDestination.CashierCashSession -> AppRoute.CashierCashSession.route
        AdminDestination.CashierTransactionHistory -> AppRoute.CashierTransactionHistory.route
        AdminDestination.Products -> AppRoute.Products.route
        AdminDestination.ProductCategories -> AppRoute.ProductCategories.route
        AdminDestination.ProductUnits -> AppRoute.ProductUnits.route
        AdminDestination.PriceManagement -> AppRoute.PriceManagement.route
        AdminDestination.StockOpname -> AppRoute.StockOpname.route
        AdminDestination.IncomingGoods -> AppRoute.IncomingGoods.route
        AdminDestination.Suppliers -> AppRoute.Suppliers.route
        AdminDestination.PurchaseHistory -> AppRoute.PurchaseHistory.route
        AdminDestination.StockReport -> AppRoute.StockReport.route
        AdminDestination.Customers -> AppRoute.Customers.route
        AdminDestination.Receivables -> AppRoute.Receivables.route
        AdminDestination.CashReconciliation -> AppRoute.CashReconciliation.route
        AdminDestination.SalesTransactions -> AppRoute.SalesTransactions.route
        AdminDestination.SupplierDebts -> AppRoute.SupplierDebts.route
        AdminDestination.CashSessionHistory -> AppRoute.CashSessionHistory.route
        AdminDestination.CashReconciliationDetail -> AppRoute.CashReconciliationDetail.createRoute()
        AdminDestination.CashExpenses -> AppRoute.CashExpenses.route
        AdminDestination.ReceivablePayments -> AppRoute.ReceivablePayments.route
        AdminDestination.Reports -> AppRoute.Reports.route
        AdminDestination.LocalReports -> AppRoute.OfflineReports.route
        AdminDestination.OperationalAudit -> AppRoute.OperationalAudit.route
        AdminDestination.SyncCenter -> AppRoute.SyncCenter.route
        AdminDestination.BackupRestore -> AppRoute.BackupRestore.route
        AdminDestination.Profile -> if (AppAccessPolicy.can(role, AppCapability.POS)) AppRoute.CashierProfile.route else AppRoute.AdminProfile.route
        AdminDestination.Settings -> if (AppAccessPolicy.can(role, AppCapability.POS)) AppRoute.CashierSettings.route else AppRoute.AdminSettings.route
        else -> null
    }
}
