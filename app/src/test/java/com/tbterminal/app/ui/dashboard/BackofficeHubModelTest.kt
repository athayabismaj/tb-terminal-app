package com.tbterminal.app.ui.dashboard

import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackofficeHubModelTest {
    @Test
    fun `owner sales keeps history without cashier POS action`() {
        val content = transactionHubContent("OWNER", BackofficeTransactionTab.SALES)

        assertNull(content.primaryAction)
        assertEquals(
            listOf(BackofficeHubAction.SALES_HISTORY),
            content.groups.single().actions,
        )
    }

    @Test
    fun `purchase flow has one primary action followed by history`() {
        val content = transactionHubContent("OWNER", BackofficeTransactionTab.PURCHASES)

        assertEquals(BackofficeHubAction.INCOMING_GOODS, content.primaryAction)
        assertEquals(
            listOf(BackofficeHubAction.PURCHASE_HISTORY),
            content.groups.single().actions,
        )
    }

    @Test
    fun `finance actions follow daily user workflow order`() {
        assertEquals(
            listOf(
                BackofficeHubAction.RECEIVABLES,
                BackofficeHubAction.SUPPLIER_DEBTS,
                BackofficeHubAction.DAILY_CASH,
            ),
            financeHubContent("OWNER").groups.single().actions,
        )
    }

    @Test
    fun `stock separates operations from product settings without duplicate destinations`() {
        val content = stockHubContent("OWNER")
        val actions = content.groups.flatMap { it.actions }

        assertEquals(BackofficeHubAction.INCOMING_GOODS, content.primaryAction)
        assertEquals(
            listOf(
                BackofficeHubAction.PRODUCTS_STOCK,
                BackofficeHubAction.ADJUST_STOCK,
                BackofficeHubAction.STOCK_CARD,
            ),
            content.groups[0].actions,
        )
        assertEquals(
            listOf(
                BackofficeHubAction.PRODUCT_PRICES,
                BackofficeHubAction.CATEGORIES,
                BackofficeHubAction.UNITS,
            ),
            content.groups[1].actions,
        )
        assertEquals(actions.size, actions.distinctBy { it.destination }.size)
        assertTrue(content.primaryAction?.destination !in actions.map { it.destination })
    }

    @Test
    fun `non backoffice persona receives no work hub actions`() {
        listOf("KASIR", "", "unknown").forEach { role ->
            assertEquals(BackofficeHubContent(), financeHubContent(role))
            assertEquals(BackofficeHubContent(), stockHubContent(role))
            assertEquals(
                BackofficeHubContent(),
                transactionHubContent(role, BackofficeTransactionTab.SALES),
            )
        }
    }

    @Test
    fun `every visible action still points to a real destination`() {
        val destinations = buildList {
            addAll(financeHubContent("OWNER").groups.flatMap { it.actions }.map { it.destination })
            addAll(stockHubContent("OWNER").groups.flatMap { it.actions }.map { it.destination })
        }
        assertTrue(AdminDestination.Receivables in destinations)
        assertTrue(AdminDestination.CashReconciliation in destinations)
        assertTrue(AdminDestination.Products in destinations)
        assertTrue(AdminDestination.StockReport in destinations)
    }
}
