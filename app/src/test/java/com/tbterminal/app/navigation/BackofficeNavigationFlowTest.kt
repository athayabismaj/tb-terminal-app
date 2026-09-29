package com.tbterminal.app.navigation

import com.tbterminal.app.R
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
    }
}
