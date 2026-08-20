package com.tbterminal.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
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
    CompositionLocalProvider(
        LocalAdminDestinationNavigator provides { destination ->
            destination.adminRouteOrNull()?.let { route ->
                navController.navigate(route) {
                    launchSingleTop = true
                    popUpTo(AppRoute.Dashboard.route) {
                        saveState = false
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

private fun AdminDestination.adminRouteOrNull(): String? {
    return when (this) {
        AdminDestination.Dashboard -> AppRoute.Dashboard.route
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
        AdminDestination.Profile -> AppRoute.AdminProfile.route
        AdminDestination.Settings -> AppRoute.AdminSettings.route
        else -> null
    }
}
