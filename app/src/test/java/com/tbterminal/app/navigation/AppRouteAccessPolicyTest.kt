package com.tbterminal.app.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRouteAccessPolicyTest {
    @Test
    fun cashierCannotOpenAdminOrOwnerRoutes() {
        assertFalse(AppRouteAccessPolicy.isAllowed(AppRoute.Products.route, "kasir"))
        assertFalse(AppRouteAccessPolicy.isAllowed(AppRoute.UserManagement.route, "kasir"))
        assertTrue(AppRouteAccessPolicy.isAllowed(AppRoute.CashierPos.route, "kasir"))
    }

    @Test
    fun adminCannotOpenOwnerOnlyRoutes() {
        assertTrue(AppRouteAccessPolicy.isAllowed(AppRoute.Products.route, "admin"))
        assertFalse(AppRouteAccessPolicy.isAllowed(AppRoute.UserManagement.route, "admin"))
        assertFalse(AppRouteAccessPolicy.isAllowed(AppRoute.SecurityLog.route, "admin"))
    }

    @Test
    fun ownerHasFullAccessAndUnknownRoleIsDenied() {
        assertTrue(AppRouteAccessPolicy.isAllowed(AppRoute.UserManagement.route, "owner"))
        assertTrue(AppRouteAccessPolicy.isAllowed(AppRoute.CashierPos.route, "owner"))
        assertFalse(AppRouteAccessPolicy.isAllowed(AppRoute.Dashboard.route, "guest"))
    }

    @Test
    fun backofficeWorkAreasAreSharedByOwnerAndAdminButHiddenFromCashier() {
        listOf(
            AppRoute.BackofficeTransactions.route,
            AppRoute.BackofficeFinance.route,
            AppRoute.BackofficeStock.route,
            AppRoute.BackofficeMore.route
        ).forEach { route ->
            assertTrue(AppRouteAccessPolicy.isAllowed(route, "owner"))
            assertTrue(AppRouteAccessPolicy.isAllowed(route, "admin"))
            assertFalse(AppRouteAccessPolicy.isAllowed(route, "kasir"))
        }
    }
}
