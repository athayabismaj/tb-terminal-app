package com.tbterminal.app.navigation

import com.tbterminal.app.R
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.toCashierDestination
import com.tbterminal.app.ui.dashboard.cashier.CashierDestination
import org.junit.Assert.*
import org.junit.Test

class BackofficeNavigationFlowTest {
    @Test fun ownerHasFiveRootsAndManagementPreservesItsParent() {
        listOf(AppRoute.Dashboard, AppRoute.BackofficeTransactions, AppRoute.BackofficeFinance,
            AppRoute.BackofficeStock, AppRoute.BackofficeMore).forEach {
            assertTrue(isBackofficePrimaryRoute(it.route, "OWNER"))
        }
        listOf(AppRoute.AdminProfile, AppRoute.Customers, AppRoute.UserManagement,
            AppRoute.AdminSettings, AppRoute.SecurityLog).forEach {
            assertFalse(isBackofficePrimaryRoute(it.route, "OWNER"))
            assertEquals(AppRoute.BackofficeMore.route, ownerMenuParentRoute(it.route, "OWNER"))
        }
    }

    @Test fun adminAndCashierKeepTheirNavigationPolicy() {
        assertTrue(isBackofficePrimaryRoute(AppRoute.BackofficeMore.route, "ADMIN"))
        assertFalse(isBackofficePrimaryRoute(AppRoute.AdminProfile.route, "ADMIN"))
        assertFalse(isBackofficePrimaryRoute(AppRoute.Dashboard.route, "KASIR"))
        assertFalse(isBackofficePrimaryRoute(AppRoute.Dashboard.route, "unknown"))
    }

    @Test fun childHeadersDescribeActualPageAndNotOtherMenu() {
        assertNull(backofficeRouteTitle(null, "OWNER"))
        assertEquals("Pengguna & akses", backofficeRouteTitle(AppRoute.UserManagement.route, "OWNER"))
        assertEquals("Log keamanan", backofficeRouteTitle(AppRoute.SecurityLog.route, "OWNER"))
        assertEquals("Kartu stok", backofficeRouteTitle(AppRoute.StockReport.route, "OWNER"))
        assertEquals("Pelanggan", backofficeRouteTitle(AppRoute.Customers.route, "OWNER"))
        assertEquals("Profil", backofficeRouteTitle(AppRoute.AdminProfile.route, "OWNER"))
    }

    @Test fun `admin and cashier never receive owner menu parent behavior`() {
        assertNull(ownerMenuParentRoute(AppRoute.AdminProfile.route, "ADMIN"))
        assertNull(ownerMenuParentRoute(AppRoute.Customers.route, "KASIR"))
        assertNull(ownerMenuParentRoute(AppRoute.BackofficeMore.route, "OWNER"))
    }

    @Test fun `standard owner menu titles use shared string resources`() {
        assertEquals(R.string.profile_title, backofficeRouteTitleRes(AppRoute.AdminProfile.route, "OWNER"))
        assertEquals(R.string.owner_menu_users, backofficeRouteTitleRes(AppRoute.UserManagement.route, "OWNER"))
        assertEquals(R.string.owner_menu_reports, backofficeRouteTitleRes(AppRoute.Reports.route, "OWNER"))
        assertEquals(R.string.owner_menu_device_reports, backofficeRouteTitleRes(AppRoute.OfflineReports.route, "OWNER"))
        assertEquals(R.string.owner_menu_backup, backofficeRouteTitleRes(AppRoute.BackupRestore.route, "OWNER"))
        assertEquals(R.string.owner_menu_settings, backofficeRouteTitleRes(AppRoute.AdminSettings.route, "OWNER"))
        assertEquals(R.string.owner_menu_store_settings, backofficeRouteTitleRes(AppRoute.AdminStoreSettings.route, "OWNER"))
        assertEquals(R.string.owner_menu_device_settings, backofficeRouteTitleRes(AppRoute.AdminDeviceSettings.route, "OWNER"))
    }

    @Test fun `cashier route and navbar destination map cash session properly`() {
        assertEquals(AppRoute.CashierCashSession.route, AdminDestination.CashierCashSession.adminRouteOrNull("kasir"))
        assertEquals(AppRoute.CashierCashSession.route, AdminDestination.CashReconciliation.adminRouteOrNull("kasir"))
        assertEquals(AppRoute.CashierCashSession.route, AdminDestination.CashSessionHistory.adminRouteOrNull("kasir"))
        assertEquals(AppRoute.CashierTransactionHistory.route, AdminDestination.SalesTransactions.adminRouteOrNull("kasir"))
        assertEquals(AppRoute.CashReconciliation.route, AdminDestination.CashReconciliation.adminRouteOrNull("admin"))
        assertEquals(AppRoute.CashReconciliation.route, AdminDestination.CashReconciliation.adminRouteOrNull("owner"))

        assertEquals(CashierDestination.CashSession, AdminDestination.CashierCashSession.toCashierDestination())
        assertEquals(CashierDestination.CashSession, AdminDestination.CashReconciliation.toCashierDestination())
        assertEquals(CashierDestination.CashSession, AdminDestination.CashSessionHistory.toCashierDestination())
        assertEquals(CashierDestination.TransactionHistory, AdminDestination.CashierTransactionHistory.toCashierDestination())
        assertEquals(CashierDestination.Receivables, AdminDestination.Receivables.toCashierDestination())
    }
}
