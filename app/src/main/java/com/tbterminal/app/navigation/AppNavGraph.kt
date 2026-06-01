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
                navController.navigate(route) { launchSingleTop = true }
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
        AdminDestination.Suppliers -> AppRoute.Suppliers.route
        AdminDestination.PurchaseHistory -> AppRoute.PurchaseHistory.route
        AdminDestination.StockReport -> AppRoute.StockReport.route
        AdminDestination.CashSessionHistory -> AppRoute.CashSessionHistory.route
        AdminDestination.CashReconciliationDetail -> AppRoute.CashReconciliationDetail.createRoute()
        AdminDestination.CashExpenses -> AppRoute.CashExpenses.route
        AdminDestination.ReceivablePayments -> AppRoute.ReceivablePayments.route
        else -> null
    }
}
