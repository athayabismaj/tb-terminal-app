package com.tbterminal.app.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardRoleAlignmentTest {
    @Test
    fun ownerDashboardContainsFinancialControlSummary() {
        assertEquals(
            listOf(
                DashboardSummaryKey.SALES,
                DashboardSummaryKey.NET_REVENUE,
                DashboardSummaryKey.REFUND,
                DashboardSummaryKey.DISCOUNT,
                DashboardSummaryKey.RECEIVABLES,
                DashboardSummaryKey.PAYABLES,
                DashboardSummaryKey.STOCK,
            ),
            dashboardSummaryKeys("OWNER"),
        )
    }

    @Test
    fun adminDashboardFocusesOnOperationalSummary() {
        val keys = dashboardSummaryKeys("ADMIN")

        assertEquals(
            listOf(
                DashboardSummaryKey.SALES,
                DashboardSummaryKey.RECEIVABLES,
                DashboardSummaryKey.PAYABLES,
                DashboardSummaryKey.CASH,
                DashboardSummaryKey.STOCK,
            ),
            keys,
        )
        assertFalse(DashboardSummaryKey.NET_REVENUE in keys)
        assertFalse(DashboardSummaryKey.DISCOUNT in keys)
        assertFalse(DashboardSummaryKey.REFUND in keys)
    }

    @Test
    fun cashierDoesNotReceiveBackofficeDashboardSummaries() {
        assertTrue(dashboardSummaryKeys("KASIR").isEmpty())
    }
}
