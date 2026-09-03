package com.tbterminal.app.ui.dashboard

import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.backofficeSection
import org.junit.Assert.assertEquals
import org.junit.Test

class BackofficeNavigationTest {
    @Test
    fun `navigation exposes exactly five daily work sections`() {
        assertEquals(
            listOf("Beranda", "Transaksi", "Keuangan", "Stok", "Lainnya"),
            BackofficeSection.entries.map(BackofficeSection::label)
        )
    }

    @Test
    fun `owner and admin share the five work sections`() {
        assertEquals(BackofficeSection.entries, visibleBackofficeSections("OWNER"))
        assertEquals(BackofficeSection.entries, visibleBackofficeSections("ADMIN"))
        assertEquals(emptyList<BackofficeSection>(), visibleBackofficeSections("KASIR"))
    }

    @Test
    fun `technical destinations stay highlighted under their work section`() {
        assertEquals(BackofficeSection.TRANSACTIONS, AdminDestination.PurchaseHistory.backofficeSection())
        assertEquals(BackofficeSection.FINANCE, AdminDestination.SupplierDebts.backofficeSection())
        assertEquals(BackofficeSection.FINANCE, AdminDestination.CashExpenses.backofficeSection())
        assertEquals(BackofficeSection.STOCK, AdminDestination.StockOpname.backofficeSection())
        assertEquals(BackofficeSection.STOCK, AdminDestination.ProductUnits.backofficeSection())
        assertEquals(BackofficeSection.MORE, AdminDestination.BackupRestore.backofficeSection())
    }

    @Test
    fun `role labels use simple Indonesian terms`() {
        assertEquals("Pemilik", "owner".toBackofficeRoleLabel())
        assertEquals("Admin", "ADMIN".toBackofficeRoleLabel())
        assertEquals("Kasir", "cashier".toBackofficeRoleLabel())
    }
}
