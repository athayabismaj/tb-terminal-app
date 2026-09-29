package com.tbterminal.app.ui.dashboard

import com.tbterminal.app.R
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.backofficeSection
import com.tbterminal.app.ui.dashboard.admin.isBackofficeRootDestination
import com.tbterminal.app.ui.components.TbWindowWidthClass
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class BackofficeNavigationTest {
    @Test
    fun `phone keeps bottom navigation in portrait and landscape`() {
        assertEquals(BackofficeNavigationMode.BOTTOM_BAR, backofficeNavigationMode(411f, 914f))
        assertEquals(BackofficeNavigationMode.BOTTOM_BAR, backofficeNavigationMode(914f, 411f))
    }

    @Test
    fun `tablet uses rail or drawer according to available width`() {
        assertEquals(BackofficeNavigationMode.RAIL, backofficeNavigationMode(800f, 1280f))
        assertEquals(BackofficeNavigationMode.DRAWER, backofficeNavigationMode(1280f, 800f))
    }

    @Test
    fun `tablet navigation stays compact`() {
        assertEquals(88.dp, BackofficeRailWidth)
        assertEquals(76.dp, OwnerCompactSidebarWidth)
        assertEquals(204.dp, OwnerExpandedSidebarWidth)
    }

    @Test
    fun `owner sidebar can collapse and expand`() {
        val state = BackofficeSidebarState()
        assertEquals(false, state.isCollapsed)
        state.toggle()
        assertEquals(true, state.isCollapsed)
        state.toggle()
        assertEquals(false, state.isCollapsed)
    }

    @Test
    fun `navigation exposes exactly five daily work sections`() {
        assertEquals(
            listOf(R.string.nav_home, R.string.nav_transactions, R.string.nav_finance, R.string.nav_stock, R.string.nav_menu),
            visibleBackofficeSections("OWNER").map(BackofficeSection::labelRes)
        )
    }

    @Test
    fun `owner persona is explicit while admin keeps existing navigation`() {
        assertEquals(listOf(BackofficeSection.HOME, BackofficeSection.TRANSACTIONS, BackofficeSection.FINANCE,
            BackofficeSection.STOCK, BackofficeSection.MENU), visibleBackofficeSections("OWNER"))
        assertEquals(listOf(BackofficeSection.HOME, BackofficeSection.TRANSACTIONS, BackofficeSection.FINANCE,
            BackofficeSection.STOCK, BackofficeSection.MORE), visibleBackofficeSections("ADMIN"))
        assertEquals(visibleBackofficeSections("OWNER"), visibleBackofficeSections(" owner "))
        assertEquals(emptyList<BackofficeSection>(), visibleBackofficeSections("KASIR"))
        assertEquals(false, isOwnerPersona("ADMIN"))
        assertEquals(false, isOwnerPersona("KASIR"))
    }

    @Test
    fun `technical destinations stay highlighted under their work section`() {
        assertEquals(BackofficeSection.TRANSACTIONS, AdminDestination.PurchaseHistory.backofficeSection())
        assertEquals(BackofficeSection.FINANCE, AdminDestination.SupplierDebts.backofficeSection())
        assertEquals(BackofficeSection.FINANCE, AdminDestination.CashExpenses.backofficeSection())
        assertEquals(BackofficeSection.STOCK, AdminDestination.StockOpname.backofficeSection())
        assertEquals(BackofficeSection.STOCK, AdminDestination.ProductUnits.backofficeSection())
        assertEquals(BackofficeSection.MENU, AdminDestination.BackupRestore.backofficeSection("OWNER"))
        assertEquals(BackofficeSection.MENU, AdminDestination.Profile.backofficeSection("OWNER"))
        assertEquals(BackofficeSection.MORE, AdminDestination.Profile.backofficeSection("ADMIN"))
    }

    @Test
    fun `role labels use simple Indonesian terms`() {
        assertEquals("Pemilik", "owner".toBackofficeRoleLabel())
        assertEquals("Admin", "ADMIN".toBackofficeRoleLabel())
        assertEquals("Kasir", "cashier".toBackofficeRoleLabel())
    }

    @Test
    fun `only five navbar destinations are root pages without shell header`() {
        val roots = listOf(
            AdminDestination.Dashboard,
            AdminDestination.TransactionsHub,
            AdminDestination.FinanceHub,
            AdminDestination.StockHub,
            AdminDestination.MoreHub,
        )
        assertEquals(5, roots.count(AdminDestination::isBackofficeRootDestination))
        listOf(
            AdminDestination.Profile,
            AdminDestination.SalesTransactions,
            AdminDestination.Receivables,
            AdminDestination.Products,
        ).forEach { assertEquals(false, it.isBackofficeRootDestination()) }
    }

    @Test
    fun `home summaries stay compact and respect large text`() {
        assertEquals(2, dashboardSummaryColumnCount(TbWindowWidthClass.Compact, 1f))
        assertEquals(1, dashboardSummaryColumnCount(TbWindowWidthClass.Compact, 1.3f))
        assertEquals(2, dashboardSummaryColumnCount(TbWindowWidthClass.Medium, 1f))
        assertEquals(3, dashboardSummaryColumnCount(TbWindowWidthClass.Expanded, 1f))
    }

    @Test fun `all owner menu destinations share the menu section`() {
        listOf(
            AdminDestination.MoreHub, AdminDestination.Customers, AdminDestination.Suppliers,
            AdminDestination.SupplierForm,
            AdminDestination.UserManagement, AdminDestination.Reports, AdminDestination.LocalReports,
            AdminDestination.BackupRestore, AdminDestination.Settings, AdminDestination.SyncCenter,
            AdminDestination.OperationalAudit, AdminDestination.SecurityLog, AdminDestination.Profile,
        ).forEach { assertEquals(BackofficeSection.MENU, it.backofficeSection("OWNER")) }
    }
}
