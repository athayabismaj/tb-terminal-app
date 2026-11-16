package com.tbterminal.app.ui.dashboard

import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.isBackofficeRootDestination
import com.tbterminal.app.ui.dashboard.admin.isCashierPrimaryDestination
import com.tbterminal.app.ui.dashboard.cashier.CashierDestination
import com.tbterminal.app.ui.dashboard.cashier.isCashierRootDestination
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellProfileVisibilityTest {

    @Test
    fun `backoffice root destinations show profile icon only on 5 main tabs`() {
        val roots = listOf(
            AdminDestination.Dashboard,
            AdminDestination.TransactionsHub,
            AdminDestination.FinanceHub,
            AdminDestination.StockHub,
            AdminDestination.MoreHub,
        )
        roots.forEach { destination ->
            assertTrue("Expected root $destination", destination.isBackofficeRootDestination())
        }

        val secondary = AdminDestination.entries.filterNot { it in roots }
        secondary.forEach { destination ->
            assertFalse("Expected secondary $destination", destination.isBackofficeRootDestination())
        }
    }

    @Test
    fun `cashier root destinations show profile icon only on 5 main tabs`() {
        val roots = setOf(
            CashierDestination.Dashboard,
            CashierDestination.Pos,
            CashierDestination.CashSession,
            CashierDestination.TransactionHistory,
            CashierDestination.Receivables,
        )
        CashierDestination.entries.forEach { destination ->
            if (destination in roots) {
                assertTrue("Expected cashier root $destination", destination.isCashierRootDestination())
            } else {
                assertFalse("Expected cashier secondary $destination", destination.isCashierRootDestination())
            }
        }
    }

    @Test
    fun `admin destination mapping for cashier only enables profile icon on primary tabs`() {
        assertTrue(AdminDestination.Dashboard.isCashierPrimaryDestination())
        assertTrue(AdminDestination.NewTransaction.isCashierPrimaryDestination())
        assertTrue(AdminDestination.CashierCashSession.isCashierPrimaryDestination())
        assertTrue(AdminDestination.CashierTransactionHistory.isCashierPrimaryDestination())
        assertTrue(AdminDestination.Receivables.isCashierPrimaryDestination())

        assertFalse(AdminDestination.SalesTransactions.isCashierPrimaryDestination())
        assertFalse(AdminDestination.Products.isCashierPrimaryDestination())
        assertFalse(AdminDestination.Customers.isCashierPrimaryDestination())
        assertFalse(AdminDestination.ReceivablePayments.isCashierPrimaryDestination())
        assertFalse(AdminDestination.Profile.isCashierPrimaryDestination())
        assertFalse(AdminDestination.Settings.isCashierPrimaryDestination())
        assertFalse(AdminDestination.OpeningReceivableForm.isCashierPrimaryDestination())
    }
}
