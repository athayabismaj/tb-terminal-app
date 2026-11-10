package com.tbterminal.app.ui.dashboard

import com.tbterminal.app.data.model.CreateProductCommand
import com.tbterminal.app.data.model.Product
import com.tbterminal.app.data.model.ProductCategory
import com.tbterminal.app.data.model.ProductCategoryPage
import com.tbterminal.app.data.model.ProductCsvImportResult
import com.tbterminal.app.data.model.ProductCsvPreview
import com.tbterminal.app.data.model.ProductDetail
import com.tbterminal.app.data.model.ProductPage
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.model.ProductStockPage
import com.tbterminal.app.data.model.ProductUnit
import com.tbterminal.app.data.model.ProductUnitPage
import com.tbterminal.app.data.model.StockAdjustmentPage
import com.tbterminal.app.data.model.StockCard
import com.tbterminal.app.data.model.UpdateProductCommand
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackofficeHubModelTest {
    @Test
    fun `owner sales keeps history and audit without cashier POS action`() {
        val content = transactionHubContent("OWNER", BackofficeTransactionTab.SALES)

        assertNull(content.primaryAction)
        assertEquals(
            listOf(
                BackofficeHubAction.SALES_HISTORY,
                BackofficeHubAction.RECEIVABLE_PAYMENTS,
                BackofficeHubAction.OPERATIONAL_AUDIT,
            ),
            content.groups.single().actions,
        )
    }

    @Test
    fun `admin sales includes cashier POS action and receivable payments`() {
        val content = transactionHubContent("ADMIN", BackofficeTransactionTab.SALES)

        assertEquals(BackofficeHubAction.NEW_SALE, content.primaryAction)
        assertEquals(
            listOf(
                BackofficeHubAction.SALES_HISTORY,
                BackofficeHubAction.RECEIVABLE_PAYMENTS,
            ),
            content.groups.single().actions,
        )
    }

    @Test
    fun `purchase flow has incoming goods primary action followed by history, suppliers, and debts`() {
        val content = transactionHubContent("OWNER", BackofficeTransactionTab.PURCHASES)

        assertEquals(BackofficeHubAction.INCOMING_GOODS, content.primaryAction)
        assertEquals(
            listOf(
                BackofficeHubAction.PURCHASE_HISTORY,
                BackofficeHubAction.SUPPLIERS,
                BackofficeHubAction.SUPPLIER_DEBTS,
            ),
            content.groups.single().actions,
        )
    }

    @Test
    fun `finance actions separate cash expenses, receivables, and owner analytics`() {
        val ownerContent = financeHubContent("OWNER")
        assertEquals(3, ownerContent.groups.size)
        assertEquals(
            listOf(
                BackofficeHubAction.DAILY_CASH,
                BackofficeHubAction.CASH_EXPENSES,
                BackofficeHubAction.CASH_SESSION_HISTORY,
            ),
            ownerContent.groups[0].actions,
        )
        assertEquals(
            listOf(
                BackofficeHubAction.RECEIVABLES,
                BackofficeHubAction.RECEIVABLE_PAYMENTS,
                BackofficeHubAction.SUPPLIER_DEBTS,
            ),
            ownerContent.groups[1].actions,
        )
        assertEquals(listOf(BackofficeHubAction.FINANCE_REPORTS), ownerContent.groups[2].actions)

        val adminContent = financeHubContent("ADMIN")
        assertEquals(2, adminContent.groups.size)
        assertFalse(BackofficeHubAction.FINANCE_REPORTS in adminContent.groups.flatMap { it.actions })
    }

    @Test
    fun `stock separates operations from product settings without duplicate destinations`() {
        val content = stockHubContent("OWNER")
        val actions = content.groups.flatMap { it.actions }

        assertEquals(BackofficeHubAction.INCOMING_GOODS, content.primaryAction)
        assertEquals(BackofficeHubAction.ADD_PRODUCT, content.secondaryQuickAction)
        assertEquals(
            listOf(
                BackofficeHubAction.PRODUCTS_STOCK,
                BackofficeHubAction.PRODUCT_PRICES,
            ),
            content.groups[0].actions,
        )
        assertEquals(
            listOf(
                BackofficeHubAction.STOCK_CARD,
                BackofficeHubAction.ADJUST_STOCK,
            ),
            content.groups[1].actions,
        )
        assertEquals(
            listOf(
                BackofficeHubAction.CATEGORIES,
                BackofficeHubAction.UNITS,
            ),
            content.groups[2].actions,
        )
        assertEquals(actions.size, actions.distinctBy { it.destination }.size)
        assertTrue(content.primaryAction?.destination !in actions.map { it.destination })
        assertTrue(content.secondaryQuickAction?.destination !in actions.map { it.destination })
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
        assertTrue(AdminDestination.CashExpenses in destinations)
        assertTrue(AdminDestination.ReceivablePayments in destinations)
        assertTrue(AdminDestination.CashReconciliation in destinations)
        assertTrue(AdminDestination.Products in destinations)
        assertTrue(AdminDestination.StockReport in destinations)
    }

    @Test
    fun `loadAllProductStocks fetches every page beyond first 100 items`() = runBlocking {
        val repo = MultiPageFakeInventoryRepository()
        val result = loadAllProductStocks(repo, pageSize = 2)

        assertTrue(result is RepositoryResult.Success)
        val aggregate = (result as RepositoryResult.Success).data
        assertEquals(3, aggregate.products.size)
        assertEquals(3, aggregate.totalProducts)
        assertFalse(aggregate.isPartial)
    }

    private class MultiPageFakeInventoryRepository : InventoryRepository {
        private val sampleItems = listOf(
            fakeStock("p-1"),
            fakeStock("p-2"),
            fakeStock("p-3"),
        )

        override suspend fun getProductStocks(
            page: Int,
            limit: Int,
            search: String?,
        ): RepositoryResult<ProductStockPage> {
            val fromIndex = (page - 1) * limit
            val slice = if (fromIndex in sampleItems.indices) {
                sampleItems.subList(fromIndex, (fromIndex + limit).coerceAtMost(sampleItems.size))
            } else {
                emptyList()
            }
            return RepositoryResult.Success(
                ProductStockPage(
                    data = slice,
                    total = sampleItems.size.toLong(),
                    page = page,
                    limit = limit,
                    totalPages = 2,
                )
            )
        }

        private fun fakeStock(id: String) = ProductStock(
            productId = id,
            sku = "SKU-$id",
            productName = "Produk $id",
            categoryName = "Material",
            unitName = "Sak",
            quantity = BigDecimal.TEN,
            minStock = BigDecimal.ONE,
            priceBuy = BigDecimal(50_000),
            priceRetail = BigDecimal(65_000),
            priceContractor = BigDecimal(60_000),
            discount = BigDecimal.ZERO,
            isActive = true,
        )

        override suspend fun getStockAdjustments(page: Int, limit: Int, search: String?, type: String?): RepositoryResult<StockAdjustmentPage> = error("unused")
        override suspend fun getStockCard(page: Int, limit: Int, productId: String?, search: String?, type: String?, startDate: String?, endDate: String?): RepositoryResult<StockCard> = error("unused")
        override suspend fun getProduct(id: String): RepositoryResult<Product> = error("unused")
        override suspend fun getProductDetail(id: String): RepositoryResult<ProductDetail> = error("unused")
        override suspend fun getProductsPage(page: Int, limit: Int, search: String?): RepositoryResult<ProductPage> = error("unused")
        override suspend fun createProduct(command: CreateProductCommand): RepositoryResult<Product> = error("unused")
        override suspend fun updateProduct(id: String, command: UpdateProductCommand): RepositoryResult<Product> = error("unused")
        override suspend fun deleteProduct(id: String): RepositoryResult<Unit> = error("unused")
        override suspend fun activateProduct(id: String): RepositoryResult<Product> = error("unused")
        override suspend fun executeStockOpname(productId: String, adjustmentType: String, actualQty: BigDecimal, notes: String?): RepositoryResult<Unit> = error("unused")
        override suspend fun previewProductCsv(csv: String): RepositoryResult<ProductCsvPreview> = error("unused")
        override suspend fun commitProductCsv(csv: String): RepositoryResult<ProductCsvImportResult> = error("unused")
        override suspend fun createOpeningStock(productId: String, date: String, quantity: BigDecimal, note: String): RepositoryResult<Unit> = error("unused")
        override suspend fun getCategories(): RepositoryResult<List<ProductCategory>> = error("unused")
        override suspend fun getCategoryPage(page: Int, limit: Int, search: String?): RepositoryResult<ProductCategoryPage> = error("unused")
        override suspend fun createCategory(name: String): RepositoryResult<ProductCategory> = error("unused")
        override suspend fun updateCategory(id: String, name: String): RepositoryResult<ProductCategory> = error("unused")
        override suspend fun deleteCategory(id: String): RepositoryResult<Unit> = error("unused")
        override suspend fun getUnits(): RepositoryResult<List<ProductUnit>> = error("unused")
        override suspend fun getUnitPage(page: Int, limit: Int, search: String?): RepositoryResult<ProductUnitPage> = error("unused")
        override suspend fun createUnit(name: String, symbol: String): RepositoryResult<ProductUnit> = error("unused")
        override suspend fun updateUnit(id: String, name: String, symbol: String): RepositoryResult<ProductUnit> = error("unused")
        override suspend fun deleteUnit(id: String): RepositoryResult<Unit> = error("unused")
    }
}
