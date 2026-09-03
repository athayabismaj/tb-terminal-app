package com.tbterminal.app.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class AppRouteAccessPolicyTest {
    @Test
    fun cashierOnlyOpensOperationalRoutesAssignedToCashier() {
        assertFalse(AppRouteAccessPolicy.isAllowed(AppRoute.Products.route, "kasir"))
        assertFalse(AppRouteAccessPolicy.isAllowed(AppRoute.UserManagement.route, "kasir"))
        assertFalse(AppRouteAccessPolicy.isAllowed(AppRoute.AddProduct.route, "kasir"))
        assertFalse(AppRouteAccessPolicy.isAllowed(AppRoute.EditProduct.createRoute("product-1"), "kasir"))
        assertFalse(AppRouteAccessPolicy.isAllowed(AppRoute.StockOpnameForm.createRoute("product-1"), "kasir"))
        assertTrue(AppRouteAccessPolicy.isAllowed(AppRoute.CashierPos.route, "kasir"))
        assertTrue(AppRouteAccessPolicy.isAllowed(AppRoute.Customers.route, "kasir"))
        assertTrue(AppRouteAccessPolicy.isAllowed(AppRoute.Receivables.route, "kasir"))
        assertTrue(AppRouteAccessPolicy.isAllowed(AppRoute.ReceivablePayments.route, "kasir"))
        assertFalse(AppRouteAccessPolicy.isAllowed(AppRoute.CashierStockCheck.route, "kasir"))
    }

    @Test
    fun adminCannotOpenOwnerOnlyRoutes() {
        assertTrue(AppRouteAccessPolicy.isAllowed(AppRoute.Products.route, "admin"))
        assertFalse(AppRouteAccessPolicy.isAllowed(AppRoute.UserManagement.route, "admin"))
        assertFalse(AppRouteAccessPolicy.isAllowed(AppRoute.SecurityLog.route, "admin"))
        assertFalse(AppRouteAccessPolicy.isAllowed(AppRoute.BackupRestore.route, "admin"))
    }

    @Test
    fun ownerHasOwnerCapabilitiesButCannotEnterCashierPos() {
        assertTrue(AppRouteAccessPolicy.isAllowed(AppRoute.UserManagement.route, "owner"))
        assertTrue(AppRouteAccessPolicy.isAllowed(AppRoute.BackupRestore.route, "owner"))
        assertFalse(AppRouteAccessPolicy.isAllowed(AppRoute.CashierPos.route, "owner"))
        assertFalse(AppRouteAccessPolicy.isAllowed(AppRoute.Dashboard.route, "guest"))
    }

    @Test
    fun unknownRouteIsDeniedForEveryRole() {
        assertFalse(AppRouteAccessPolicy.isAllowed("admin/not-registered", "owner"))
        assertFalse(AppRouteAccessPolicy.isAllowed("cashier/not-registered", "kasir"))
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

    @Test
    fun ownerMenuContainsFullControlFeatures() {
        assertEquals(
            setOf(
                AppMenuFeature.DASHBOARD,
                AppMenuFeature.PRODUCTS,
                AppMenuFeature.STOCK,
                AppMenuFeature.SUPPLIERS,
                AppMenuFeature.PURCHASES,
                AppMenuFeature.PAYABLES,
                AppMenuFeature.CUSTOMERS,
                AppMenuFeature.RECEIVABLES,
                AppMenuFeature.RECEIVABLE_PAYMENTS,
                AppMenuFeature.TRANSACTIONS,
                AppMenuFeature.CASH_SESSIONS,
                AppMenuFeature.REPORTS,
                AppMenuFeature.USER_MANAGEMENT,
                AppMenuFeature.AUDIT,
                AppMenuFeature.SERVER_BACKUP,
                AppMenuFeature.STORE_SETTINGS,
                AppMenuFeature.SECURITY_SETTINGS,
                AppMenuFeature.DEVICE_SETTINGS,
                AppMenuFeature.ACCOUNT,
            ),
            AppAccessPolicy.visibleMenuFeatures("OWNER").toSet(),
        )
    }

    @Test
    fun adminMenuExcludesOwnerSecurityAndServerAdministration() {
        val features = AppAccessPolicy.visibleMenuFeatures("ADMIN")

        assertFalse(AppMenuFeature.USER_MANAGEMENT in features)
        assertFalse(AppMenuFeature.SERVER_BACKUP in features)
        assertFalse(AppMenuFeature.SECURITY_SETTINGS in features)
        assertTrue(AppMenuFeature.AUDIT in features)
        assertTrue(AppMenuFeature.STORE_SETTINGS in features)
    }

    @Test
    fun cashierMenuContainsOnlyDailyOperationalFeatures() {
        assertEquals(
            setOf(
                AppMenuFeature.DASHBOARD,
                AppMenuFeature.CUSTOMERS,
                AppMenuFeature.RECEIVABLES,
                AppMenuFeature.RECEIVABLE_PAYMENTS,
                AppMenuFeature.OWN_TRANSACTIONS,
                AppMenuFeature.POS,
                AppMenuFeature.CASH_SESSIONS,
                AppMenuFeature.DEVICE_SETTINGS,
                AppMenuFeature.ACCOUNT,
            ),
            AppAccessPolicy.visibleMenuFeatures("KASIR").toSet(),
        )
    }

    @Test
    fun sharedActionsUseCentralCapabilities() {
        assertFalse(AppAccessPolicy.can("KASIR", AppCapability.MANAGE_CUSTOMERS))
        assertFalse(AppAccessPolicy.can("KASIR", AppCapability.MANAGE_INVENTORY))
        assertFalse(AppAccessPolicy.can("KASIR", AppCapability.ADJUST_RECEIVABLES))
        assertFalse(AppAccessPolicy.can("KASIR", AppCapability.REVERSE_RECEIVABLE_PAYMENTS))
        assertTrue(AppAccessPolicy.can("OWNER", AppCapability.MANAGE_INVENTORY))
        assertTrue(AppAccessPolicy.can("ADMIN", AppCapability.MANAGE_INVENTORY))
    }
}
