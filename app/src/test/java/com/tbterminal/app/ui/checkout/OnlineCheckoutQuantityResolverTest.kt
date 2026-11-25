package com.tbterminal.app.ui.checkout

import com.tbterminal.app.data.model.CashExpense
import com.tbterminal.app.data.model.CashExpensePage
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.CashSessionPage
import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.data.model.CashTransactionPage
import com.tbterminal.app.data.model.CheckoutDiscount
import com.tbterminal.app.data.model.CheckoutPreview
import com.tbterminal.app.data.model.CheckoutPreviewCommand
import com.tbterminal.app.data.model.CheckoutPreviewItem
import com.tbterminal.app.data.model.CheckoutReceipt
import com.tbterminal.app.data.model.CheckoutSubmitCommand
import com.tbterminal.app.data.model.CreateProductCommand
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.model.CustomerCommand
import com.tbterminal.app.data.model.CustomerPage
import com.tbterminal.app.data.model.Product as DomainProduct
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
import com.tbterminal.app.data.model.RefundDisposition
import com.tbterminal.app.data.model.StockAdjustmentPage
import com.tbterminal.app.data.model.StockCard
import com.tbterminal.app.data.model.TransactionRefundResult
import com.tbterminal.app.data.model.TransactionVoidResult
import com.tbterminal.app.data.model.UpdateProductCommand
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.CheckoutRepository
import com.tbterminal.app.data.repository.CustomerRepository
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.data.repository.RepositoryResult
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnlineCheckoutQuantityResolverTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fractional secondary factor preserves exact decimal without truncation`() {
        val result = resolveOnlineCheckoutItems(
            listOf(
                CartItem(
                    productId = "prod-1",
                    productName = "Cat Kayu",
                    unitPrice = BigDecimal("25000"),
                    quantity = 3,
                    unitName = "Kaleng",
                    cartItemId = "prod-1__sec",
                    baseUnitPrice = BigDecimal("10000"),
                    baseUnitName = "Liter",
                    secondaryUnitId = "unit-kaleng",
                    secondaryUnitName = "Kaleng",
                    secondaryUnitFactor = BigDecimal("2.5"),
                    isSecondaryUnit = true,
                )
            )
        )

        assertTrue(result is OnlineCheckoutItemsResult.Success)
        val item = (result as OnlineCheckoutItemsResult.Success).items.single()
        assertEquals(0, BigDecimal("7.5").compareTo(item.quantity))
        assertEquals("7.5", item.quantity.toPlainString())
    }

    @Test
    fun `two decimal fractional result succeeds while more than two decimals is blocked`() {
        val validResult = resolveOnlineCheckoutItems(
            listOf(
                CartItem(
                    productId = "prod-2",
                    productName = "Besi",
                    unitPrice = BigDecimal("12500"),
                    quantity = 3,
                    unitName = "Batang",
                    cartItemId = "prod-2__sec",
                    secondaryUnitId = "unit-btg",
                    secondaryUnitFactor = BigDecimal("1.25"),
                    isSecondaryUnit = true,
                )
            )
        )
        assertTrue(validResult is OnlineCheckoutItemsResult.Success)
        assertEquals("3.75", (validResult as OnlineCheckoutItemsResult.Success).items.single().quantity.toPlainString())

        val invalidResult = resolveOnlineCheckoutItems(
            listOf(
                CartItem(
                    productId = "prod-3",
                    productName = "Kawat",
                    unitPrice = BigDecimal("21250"),
                    quantity = 1,
                    unitName = "Roll",
                    cartItemId = "prod-3__sec",
                    secondaryUnitId = "unit-roll",
                    secondaryUnitFactor = BigDecimal("2.125"),
                    isSecondaryUnit = true,
                )
            )
        )
        assertTrue(invalidResult is OnlineCheckoutItemsResult.Error)
        assertEquals(
            "Jumlah satuan dasar Kawat melebihi 2 angka desimal",
            (invalidResult as OnlineCheckoutItemsResult.Error).message,
        )
    }

    @Test
    fun `duplicate productId with base and secondary units is blocked`() {
        val result = resolveOnlineCheckoutItems(
            listOf(
                CartItem(
                    productId = "prod-1",
                    productName = "Paku 5 cm",
                    unitPrice = BigDecimal("10000"),
                    quantity = 2,
                    unitName = "Pcs",
                    cartItemId = "prod-1",
                    isSecondaryUnit = false,
                ),
                CartItem(
                    productId = "prod-1",
                    productName = "Paku 5 cm",
                    unitPrice = BigDecimal("25000"),
                    quantity = 1,
                    unitName = "Dus",
                    cartItemId = "prod-1__sec",
                    secondaryUnitId = "unit-dus",
                    secondaryUnitFactor = BigDecimal("2.5"),
                    isSecondaryUnit = true,
                ),
            )
        )

        assertTrue(result is OnlineCheckoutItemsResult.Error)
        assertEquals(
            "Produk yang sama tidak boleh muncul lebih dari sekali dalam keranjang",
            (result as OnlineCheckoutItemsResult.Error).message,
        )
    }

    @Test
    fun `checkoutViewModel sends exact 7_5 quantity for 3x2_5 in both preview and submit`() = runTest(testDispatcher) {
        val fakeRepo = RecordingCheckoutRepository()
        val vm = buildViewModel(
            checkoutRepository = fakeRepo,
            products = listOf(sampleStock(productId = "prod-1", factor = BigDecimal("2.5"))),
        )
        advanceUntilIdle()

        vm.addProduct(
            Product(
                productId = "prod-1",
                name = "Cat Kayu",
                unitPrice = BigDecimal("10000"),
                sku = "CAT-1",
                unitName = "Liter",
                stockQty = BigDecimal("50"),
                baseUnitId = "unit-liter",
                secondaryUnitId = "unit-kaleng",
                secondaryUnitName = "Kaleng",
                secondaryUnitFactor = BigDecimal("2.5"),
            ),
            isSecondary = true,
        )
        vm.updateCartQuantity("prod-1__sec", 3)

        vm.submitCheckout(PaymentMethod.TUNAI, BigDecimal("75000"))
        advanceUntilIdle()

        val previewCmd = fakeRepo.previewCommands.single()
        assertEquals("7.5", previewCmd.items.single().quantity.toPlainString())

        val submitCmd = fakeRepo.submitCommands.single()
        assertEquals("7.5", submitCmd.items.single().quantity.toPlainString())
        assertNull(vm.uiState.value.errorEvent)
    }

    @Test
    fun `checkoutViewModel blocks duplicate base and secondary product before hitting preview or submit`() = runTest(testDispatcher) {
        val fakeRepo = RecordingCheckoutRepository()
        val vm = buildViewModel(
            checkoutRepository = fakeRepo,
            products = listOf(sampleStock(productId = "prod-1", factor = BigDecimal("2.5"))),
        )
        advanceUntilIdle()

        val product = Product(
            productId = "prod-1",
            name = "Cat Kayu",
            unitPrice = BigDecimal("10000"),
            sku = "CAT-1",
            unitName = "Liter",
            stockQty = BigDecimal("50"),
            baseUnitId = "unit-liter",
            secondaryUnitId = "unit-kaleng",
            secondaryUnitName = "Kaleng",
            secondaryUnitFactor = BigDecimal("2.5"),
        )
        vm.addProduct(product, isSecondary = false)
        vm.addProduct(product, isSecondary = true)

        vm.submitCheckout(PaymentMethod.TUNAI, BigDecimal("35000"))
        advanceUntilIdle()

        assertTrue(fakeRepo.previewCommands.isEmpty())
        assertTrue(fakeRepo.submitCommands.isEmpty())
        val errorMsg = (vm.uiState.value.errorEvent as? com.tbterminal.app.ui.common.UiText.DynamicString)?.value
        assertEquals("Produk yang sama tidak boleh muncul lebih dari sekali dalam keranjang", errorMsg)
    }

    @Test
    fun `checkoutViewModel blocks over two decimals before calling previewCheckout`() = runTest(testDispatcher) {
        val fakeRepo = RecordingCheckoutRepository()
        val vm = buildViewModel(
            checkoutRepository = fakeRepo,
            products = listOf(sampleStock(productId = "prod-1", factor = BigDecimal("2.125"))),
        )
        advanceUntilIdle()

        vm.addProduct(
            Product(
                productId = "prod-1",
                name = "Kawat",
                unitPrice = BigDecimal("10000"),
                sku = "KWT-1",
                unitName = "Meter",
                stockQty = BigDecimal("50"),
                baseUnitId = "unit-m",
                secondaryUnitId = "unit-roll",
                secondaryUnitName = "Roll",
                secondaryUnitFactor = BigDecimal("2.125"),
            ),
            isSecondary = true,
        )

        vm.submitCheckout(PaymentMethod.TUNAI, BigDecimal("21250"))
        advanceUntilIdle()

        assertTrue(fakeRepo.previewCommands.isEmpty())
        assertTrue(fakeRepo.submitCommands.isEmpty())
        val errorMsg = (vm.uiState.value.errorEvent as? com.tbterminal.app.ui.common.UiText.DynamicString)?.value
        assertEquals("Jumlah satuan dasar Kawat melebihi 2 angka desimal", errorMsg)
    }

    @Test
    fun `idempotency fingerprint distinguishes factor changes while normalizing trailing zeros`() {
        val baseItem = CartItem(
            productId = "prod-1",
            productName = "Cat Kayu",
            unitPrice = BigDecimal("25000"),
            quantity = 3,
            unitName = "Kaleng",
            cartItemId = "prod-1__sec",
            secondaryUnitId = "unit-kaleng",
            secondaryUnitName = "Kaleng",
            secondaryUnitFactor = BigDecimal("2.5"),
            isSecondaryUnit = true,
        )

        fun fp(item: CartItem) = buildOnlineCheckoutIdempotencyFingerprint(
            items = listOf(item),
            paymentMethod = PaymentMethod.TUNAI,
            amountPaid = BigDecimal("75000"),
            paymentLines = null,
            isSplitDpMode = false,
            customerId = null,
            notes = null,
            transactionDiscount = null,
            checkoutAttemptId = null,
            managerApprovalId = null,
        )

        val fpFactor25 = fp(baseItem)
        val fpFactor250 = fp(baseItem.copy(secondaryUnitFactor = BigDecimal("2.50")))
        val fpFactor3 = fp(baseItem.copy(secondaryUnitFactor = BigDecimal("3")))

        assertEquals(fpFactor25, fpFactor250)
        assertNotEquals(fpFactor25, fpFactor3)
    }

    private fun sampleStock(productId: String, factor: BigDecimal) = ProductStock(
        productId = productId,
        sku = "SKU-1",
        productName = "Cat Kayu",
        categoryName = "Cat",
        unitName = "Liter",
        quantity = BigDecimal("50"),
        minStock = BigDecimal("5"),
        priceBuy = BigDecimal("8000"),
        priceRetail = BigDecimal("10000"),
        priceContractor = BigDecimal("9500"),
        discount = BigDecimal.ZERO,
        isActive = true,
        secondaryUnitId = "unit-kaleng",
        secondaryUnitName = "Kaleng",
        secondaryUnitFactor = factor,
        baseUnitId = "unit-liter",
    )

    private fun buildViewModel(
        checkoutRepository: CheckoutRepository,
        products: List<ProductStock>,
    ): CheckoutViewModel {
        return CheckoutViewModel(
            checkoutRepository = checkoutRepository,
            inventoryRepository = object : InventoryRepository {
                override suspend fun getProductStocks(page: Int, limit: Int, search: String?): RepositoryResult<ProductStockPage> =
                    RepositoryResult.Success(ProductStockPage(products, products.size.toLong(), page, limit, 1))
                override suspend fun getStockAdjustments(page: Int, limit: Int, search: String?, type: String?): RepositoryResult<StockAdjustmentPage> = error("unused")
                override suspend fun getStockCard(page: Int, limit: Int, productId: String?, search: String?, type: String?, startDate: String?, endDate: String?): RepositoryResult<StockCard> = error("unused")
                override suspend fun getProduct(id: String): RepositoryResult<DomainProduct> = error("unused")
                override suspend fun getProductDetail(id: String): RepositoryResult<ProductDetail> = error("unused")
                override suspend fun getProductsPage(page: Int, limit: Int, search: String?): RepositoryResult<ProductPage> = error("unused")
                override suspend fun createProduct(command: CreateProductCommand): RepositoryResult<DomainProduct> = error("unused")
                override suspend fun updateProduct(id: String, command: UpdateProductCommand): RepositoryResult<DomainProduct> = error("unused")
                override suspend fun deleteProduct(id: String): RepositoryResult<Unit> = error("unused")
                override suspend fun activateProduct(id: String): RepositoryResult<DomainProduct> = error("unused")
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
            },
            customerRepository = object : CustomerRepository {
                override suspend fun getCustomers(page: Int, limit: Int, search: String?, status: String?): RepositoryResult<CustomerPage> =
                    RepositoryResult.Success(CustomerPage(emptyList(), 0L, page, limit, 1))
                override suspend fun getCustomerById(id: String): RepositoryResult<Customer> = error("unused")
                override suspend fun createCustomer(command: CustomerCommand): RepositoryResult<Customer> = error("unused")
                override suspend fun updateCustomer(id: String, command: CustomerCommand): RepositoryResult<Customer> = error("unused")
                override suspend fun deactivateCustomer(id: String): RepositoryResult<Unit> = error("unused")
                override suspend fun activateCustomer(id: String): RepositoryResult<Customer> = error("unused")
            },
            cashReconciliationRepository = object : CashReconciliationRepository {
                override suspend fun getActiveSession(): RepositoryResult<CashSession?> = RepositoryResult.Success(
                    CashSession(
                        id = "sess-1",
                        userId = "kasir-1",
                        openedAt = "2026-11-25T08:00:00+07:00",
                        closedAt = null,
                        openingCash = BigDecimal("100000"),
                        closingCash = null,
                        systemCash = null,
                        difference = null,
                        totalExpenses = BigDecimal.ZERO,
                        notes = null,
                        status = "OPEN",
                    )
                )
                override suspend fun openSession(startingCash: BigDecimal): RepositoryResult<CashSession> = error("unused")
                override suspend fun getSessions(page: Int, limit: Int, status: String?, startDate: String?, endDate: String?): RepositoryResult<CashSessionPage> = error("unused")
                override suspend fun getSessionById(id: String): RepositoryResult<CashSession> = error("unused")
                override suspend fun closeSession(endingCashPhysical: BigDecimal, notes: String?): RepositoryResult<CashSession> = error("unused")
                override suspend fun forceCloseSession(sessionId: String, endingCashPhysical: BigDecimal, notes: String?): RepositoryResult<CashSession> = error("unused")
                override suspend fun getTransactions(page: Int, limit: Int, sessionId: String?, search: String?, receiptNumber: String?, cashierId: String?, customerId: String?, paymentMethod: String?, status: String?, startDate: String?, endDate: String?): RepositoryResult<CashTransactionPage> = error("unused")
                override suspend fun getTransactionById(id: String): RepositoryResult<CashTransactionDetail> = error("unused")
                override suspend fun voidTransaction(id: String, reason: String, idempotencyKey: String, managerApprovalId: String?): RepositoryResult<TransactionVoidResult> = error("unused")
                override suspend fun refundTransaction(id: String, reason: String, disposition: RefundDisposition, idempotencyKey: String, managerApprovalId: String?): RepositoryResult<TransactionRefundResult> = error("unused")
                override suspend fun addExpense(amount: BigDecimal, description: String): RepositoryResult<CashExpense> = error("unused")
                override suspend fun getExpenses(sessionId: String): RepositoryResult<List<CashExpense>> = error("unused")
                override suspend fun getExpenseHistory(page: Int, limit: Int, sessionId: String?, startDate: String?, endDate: String?): RepositoryResult<CashExpensePage> = error("unused")
                override suspend fun payTransactionDebt(transactionId: String, amount: BigDecimal, method: String, idempotencyKey: String?): RepositoryResult<CashTransactionDetail> = error("unused")
            },
        )
    }

    private class RecordingCheckoutRepository(
        private val failSubmit: Boolean = false,
    ) : CheckoutRepository {
        val previewCommands = mutableListOf<CheckoutPreviewCommand>()
        val submitCommands = mutableListOf<CheckoutSubmitCommand>()

        override suspend fun previewCheckout(command: CheckoutPreviewCommand): RepositoryResult<CheckoutPreview> {
            previewCommands += command
            val previewItems = command.items.map { item ->
                CheckoutPreviewItem(
                    productId = item.productId,
                    quantity = item.quantity,
                    unitPrice = BigDecimal("10000"),
                    grossLineTotal = BigDecimal("75000"),
                    discountType = null,
                    discountValue = BigDecimal.ZERO,
                    discountAmount = BigDecimal.ZERO,
                    netLineTotal = BigDecimal("75000"),
                )
            }
            return RepositoryResult.Success(
                CheckoutPreview(
                    checkoutAttemptId = "00000000-0000-0000-0000-000000000001",
                    discountFingerprint = "fp",
                    items = previewItems,
                    grossSubtotal = BigDecimal("75000"),
                    itemDiscountTotal = BigDecimal.ZERO,
                    transactionDiscountAmount = BigDecimal.ZERO,
                    totalDiscountAmount = BigDecimal.ZERO,
                    effectiveDiscountPercent = BigDecimal.ZERO,
                    netTotal = BigDecimal("75000"),
                    cashierDiscountLimitPercent = BigDecimal("10"),
                    approvalRequired = false,
                    expiresAt = "2099-01-01T00:00:00Z",
                )
            )
        }

        override suspend fun submitCheckout(command: CheckoutSubmitCommand): RepositoryResult<CheckoutReceipt> {
            submitCommands += command
            return if (failSubmit) {
                RepositoryResult.Error("VALIDATION_ERROR", "Simulasi gagal")
            } else {
                RepositoryResult.Success(
                    CheckoutReceipt(
                        receiptId = "TRX-0001",
                        transactionId = "trx-1",
                        amountTendered = command.amountPaid,
                        changeAmount = "0",
                        idempotentReplay = false,
                    )
                )
            }
        }
    }
}
