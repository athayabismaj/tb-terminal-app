package com.tbterminal.app.ui.incominggoods

import com.tbterminal.app.data.model.CreateOpeningPayableCommand
import com.tbterminal.app.data.model.CreatePayableAdjustmentCommand
import com.tbterminal.app.data.model.CreatePurchaseCommand
import com.tbterminal.app.data.model.CreateProductCommand
import com.tbterminal.app.data.model.CreateSupplierPaymentCommand
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
import com.tbterminal.app.data.model.PurchaseDetail
import com.tbterminal.app.data.model.PurchaseReceipt
import com.tbterminal.app.data.model.PurchaseSummaryPage
import com.tbterminal.app.data.model.StockAdjustmentPage
import com.tbterminal.app.data.model.StockCard
import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.data.model.SupplierPage
import com.tbterminal.app.data.model.SupplierPayablePage
import com.tbterminal.app.data.model.SupplierPaymentReceipt
import com.tbterminal.app.data.model.UpdateProductCommand
import com.tbterminal.app.data.numbering.DocumentNumberGenerator
import com.tbterminal.app.data.numbering.DocumentNumberType
import com.tbterminal.app.data.remote.PurchaseItemRequestDto
import com.tbterminal.app.data.remote.PurchaseRequestDto
import com.tbterminal.app.data.remote.PurchaseResponseDto
import com.tbterminal.app.data.remote.SupplierPaymentRequestDto
import com.tbterminal.app.data.remote.SupplierPaymentResponseDto
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.data.repository.PurchasingRepository
import com.tbterminal.app.data.repository.RepositoryResult
import java.io.IOException
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IncomingGoodsIdempotencyViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private val networkJson = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun adminSubmitIncomingGoods_reusesKeyOnRetry_rotatesOnChangeAndSuccess_andHandlesReplay() = runTest(dispatcher) {
        val inventoryRepo = FakeIncomingInventoryRepository()
        val purchasingRepo = FakeIncomingPurchasingRepository()
        var seq = 0
        val vm = IncomingGoodsViewModel(
            inventoryRepository = inventoryRepo,
            purchasingRepository = purchasingRepo,
            documentNumberGenerator = FakeDocNumberGenerator(),
            idempotencyKeyGenerator = { "inc-${++seq}" }
        )
        advanceUntilIdle()

        vm.onQuantityChanged("10")
        vm.onBuyPriceChanged("45000")
        vm.onInvoiceChanged("BM-001")
        vm.onNotesChanged("Batch pagi")

        // 1. Timeout/IOException -> retry keeps exact key
        purchasingRepo.nextPurchaseResult = RepositoryResult.Exception(IOException("Timeout"))
        vm.submitIncomingGoods()
        advanceUntilIdle()

        assertEquals(1, purchasingRepo.recordedPurchases.size)
        val firstKey = purchasingRepo.recordedPurchases[0].idempotencyKey
        assertNotNull(firstKey)
        assertTrue(firstKey!!.startsWith("purchase-inc-"))

        purchasingRepo.nextPurchaseResult = RepositoryResult.Error("HTTP_503", "Gateway timeout")
        vm.submitIncomingGoods()
        advanceUntilIdle()

        assertEquals(2, purchasingRepo.recordedPurchases.size)
        assertEquals(firstKey, purchasingRepo.recordedPurchases[1].idempotencyKey)

        // 2. Edit payload (qty changed) -> must generate a new key
        vm.onQuantityChanged("12")
        purchasingRepo.nextPurchaseResult = RepositoryResult.Error("NETWORK_TIMEOUT", "Timeout")
        vm.submitIncomingGoods()
        advanceUntilIdle()

        assertEquals(3, purchasingRepo.recordedPurchases.size)
        val changedQtyKey = purchasingRepo.recordedPurchases[2].idempotencyKey
        assertNotEquals(firstKey, changedQtyKey)

        // 3. Edit notes / invoice -> rotates again; replay response treated as normal single success
        vm.onNotesChanged("Batch sore")
        purchasingRepo.nextPurchaseResult = RepositoryResult.Success(
            PurchaseReceipt(
                id = "pur-1",
                supplierId = "sup-1",
                supplierName = "PT Semen Jaya",
                invoiceNo = "BM-001",
                total = BigDecimal("540000"),
                receivedAt = "2026-11-26T10:00:00Z",
                idempotentReplay = true
            )
        )
        vm.submitIncomingGoods()
        advanceUntilIdle()

        assertEquals(4, purchasingRepo.recordedPurchases.size)
        val replayKey = purchasingRepo.recordedPurchases[3].idempotencyKey
        assertNotEquals(changedQtyKey, replayKey)
        assertEquals("Barang masuk berhasil dicatat untuk PT Semen Jaya.", vm.uiState.value.message)
        assertNull(vm.uiState.value.errorMessage)

        // 4. Subsequent purchase even with exact same values gets fresh key
        vm.onQuantityChanged("12")
        vm.onBuyPriceChanged("45000")
        vm.onInvoiceChanged("BM-001")
        vm.onNotesChanged("Batch sore")
        purchasingRepo.nextPurchaseResult = null
        vm.submitIncomingGoods()
        advanceUntilIdle()

        assertEquals(5, purchasingRepo.recordedPurchases.size)
        assertNotEquals(replayKey, purchasingRepo.recordedPurchases[4].idempotencyKey)
    }

    @Test
    fun ownerSubmitIncomingGoods_reusesKeyOnRetry_rotatesOnChange_andHandlesConflict() = runTest(dispatcher) {
        val inventoryRepo = FakeIncomingInventoryRepository()
        val purchasingRepo = FakeIncomingPurchasingRepository()
        var seq = 0
        val vm = IncomingGoodsViewModel(
            inventoryRepository = inventoryRepo,
            purchasingRepository = purchasingRepo,
            documentNumberGenerator = FakeDocNumberGenerator(),
            idempotencyKeyGenerator = { "own-${++seq}" }
        )
        advanceUntilIdle()

        vm.onQuantityChanged("5")
        vm.onBuyPriceChanged("50000")

        // 1. Ambiguous exception -> retry reuses exact key
        purchasingRepo.nextPurchaseResult = RepositoryResult.Error("NETWORK_TIMEOUT", "Timeout")
        var successCalls = 0
        vm.submitOwnerIncomingGoods(
            invoiceNo = "INV-100",
            paymentMethod = IncomingPaymentMethod.CASH,
            amountPaid = BigDecimal("250000"),
            dueDays = 30,
            notes = "Stok owner",
            onSuccess = { successCalls++ }
        )
        advanceUntilIdle()

        assertEquals(1, purchasingRepo.recordedPurchases.size)
        val ownerKey = purchasingRepo.recordedPurchases[0].idempotencyKey
        assertNotNull(ownerKey)

        purchasingRepo.nextPurchaseResult = RepositoryResult.Exception(IOException("Lost connection"))
        vm.submitOwnerIncomingGoods(
            invoiceNo = "  INV-100  ",
            paymentMethod = IncomingPaymentMethod.CASH,
            amountPaid = BigDecimal("250000.00"),
            dueDays = 30,
            notes = "  Stok owner ",
            onSuccess = { successCalls++ }
        )
        advanceUntilIdle()

        assertEquals(2, purchasingRepo.recordedPurchases.size)
        assertEquals(ownerKey, purchasingRepo.recordedPurchases[1].idempotencyKey)

        // 2. 400 Idempotency conflict clears key & stops without auto-retry
        purchasingRepo.nextPurchaseResult = RepositoryResult.Error(
            code = "VALIDATION_ERROR",
            message = "Idempotency key sudah digunakan untuk request pembelian yang berbeda"
        )
        vm.submitOwnerIncomingGoods(
            invoiceNo = "INV-101",
            paymentMethod = IncomingPaymentMethod.CASH,
            amountPaid = BigDecimal("250000"),
            dueDays = 30,
            notes = "Stok owner",
            onSuccess = { successCalls++ }
        )
        advanceUntilIdle()

        assertEquals(3, purchasingRepo.recordedPurchases.size)
        val conflictKey = purchasingRepo.recordedPurchases[2].idempotencyKey
        assertNotEquals(ownerKey, conflictKey)
        assertFalse(vm.uiState.value.isSubmitting)
        assertEquals(0, successCalls)
        assertEquals(
            "Idempotency key sudah digunakan untuk request pembelian yang berbeda",
            vm.uiState.value.errorMessage
        )

        // 3. After conflict, replay response succeeds cleanly and rotates away from conflicted key
        purchasingRepo.nextPurchaseResult = RepositoryResult.Success(
            PurchaseReceipt(
                id = "pur-own-1",
                supplierId = "sup-1",
                supplierName = "PT Semen Jaya",
                invoiceNo = "INV-101",
                total = BigDecimal("250000"),
                receivedAt = "2026-11-26T10:00:00Z",
                idempotentReplay = true
            )
        )
        vm.submitOwnerIncomingGoods(
            invoiceNo = "INV-101",
            paymentMethod = IncomingPaymentMethod.CASH,
            amountPaid = BigDecimal("250000"),
            dueDays = 30,
            notes = "Stok owner",
            onSuccess = { successCalls++ }
        )
        advanceUntilIdle()

        assertEquals(4, purchasingRepo.recordedPurchases.size)
        assertNotEquals(conflictKey, purchasingRepo.recordedPurchases[3].idempotencyKey)
        assertEquals(1, successCalls)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun purchasingDtos_serializeIdempotencyKey_andDeserializeIdempotentReplay() {
        val purchaseRequest = PurchaseRequestDto(
            supplierId = "sup-1",
            invoiceNo = "INV-1",
            paymentMethod = "tunai",
            amountPaid = BigDecimal("100000"),
            dueDays = 30,
            items = listOf(PurchaseItemRequestDto("prod-1", BigDecimal("2"), BigDecimal("50000"))),
            idempotencyKey = "purchase-abc-123"
        )
        val encodedPurchase = networkJson.encodeToString(purchaseRequest)
        assertTrue(encodedPurchase.contains("\"idempotencyKey\":\"purchase-abc-123\""))

        val paymentRequest = SupplierPaymentRequestDto(
            payableId = "pay-1",
            amount = BigDecimal("50000"),
            method = "tunai",
            idempotencyKey = "supplier-pay-xyz-9"
        )
        val encodedPayment = networkJson.encodeToString(paymentRequest)
        assertTrue(encodedPayment.contains("\"idempotencyKey\":\"supplier-pay-xyz-9\""))

        val purchaseReplayJson = """
            {
              "id": "pur-1",
              "supplierId": "sup-1",
              "supplierName": "PT Semen Jaya",
              "userId": "u-1",
              "invoiceNo": "INV-1",
              "total": "100000",
              "notes": null,
              "receivedAt": "2026-11-26T10:00:00Z",
              "createdAt": "2026-11-26T10:00:00Z",
              "items": [],
              "idempotentReplay": true
            }
        """.trimIndent()
        val parsedPurchase = networkJson.decodeFromString<PurchaseResponseDto>(purchaseReplayJson)
        assertTrue(parsedPurchase.idempotentReplay)

        val paymentDefaultJson = """
            {
              "id": "pmt-1",
              "payableId": "pay-1",
              "amount": "50000",
              "method": "tunai",
              "reference": null,
              "notes": null,
              "paidAt": "2026-11-26T10:00:00Z",
              "payableStatus": "sebagian",
              "payableRemainingAmount": "50000"
            }
        """.trimIndent()
        val parsedDefaultPayment = networkJson.decodeFromString<SupplierPaymentResponseDto>(paymentDefaultJson)
        assertFalse(parsedDefaultPayment.idempotentReplay)

        val paymentReplayJson = """
            {
              "id": "pmt-1",
              "payableId": "pay-1",
              "amount": "50000",
              "method": "tunai",
              "reference": null,
              "notes": null,
              "paidAt": "2026-11-26T10:00:00Z",
              "payableStatus": "sebagian",
              "payableRemainingAmount": "50000",
              "idempotentReplay": true
            }
        """.trimIndent()
        val parsedReplayPayment = networkJson.decodeFromString<SupplierPaymentResponseDto>(paymentReplayJson)
        assertTrue(parsedReplayPayment.idempotentReplay)
    }

    private class FakeDocNumberGenerator : DocumentNumberGenerator {
        override fun generate(type: DocumentNumberType, date: LocalDate): String = "BM-TEST-001"
    }

    private class FakeIncomingPurchasingRepository : PurchasingRepository {
        val recordedPurchases = mutableListOf<CreatePurchaseCommand>()
        var nextPurchaseResult: RepositoryResult<PurchaseReceipt>? = null

        override suspend fun getSuppliers(
            page: Int,
            limit: Int,
            search: String?,
            status: String?
        ): RepositoryResult<SupplierPage> = RepositoryResult.Success(
            SupplierPage(
                data = listOf(
                    Supplier(
                        id = "sup-1",
                        name = "PT Semen Jaya",
                        phone = "08123456789",
                        address = "Jl. Raya",
                        paymentTermDays = 30,
                        isActive = true,
                        createdAt = "2026-11-01T00:00:00Z",
                        updatedAt = "2026-11-01T00:00:00Z"
                    )
                ),
                total = 1,
                page = 1,
                limit = limit,
                totalPages = 1
            )
        )

        override suspend fun createPurchase(command: CreatePurchaseCommand): RepositoryResult<PurchaseReceipt> {
            recordedPurchases += command
            val override = nextPurchaseResult
            nextPurchaseResult = null
            if (override != null) return override
            val total = command.items.fold(BigDecimal.ZERO) { acc, item -> acc + item.qty.multiply(item.price) }
            return RepositoryResult.Success(
                PurchaseReceipt(
                    id = "pur-${recordedPurchases.size}",
                    supplierId = command.supplierId,
                    supplierName = "PT Semen Jaya",
                    invoiceNo = command.invoiceNo,
                    total = total,
                    receivedAt = "2026-11-26T10:00:00Z",
                    idempotentReplay = false
                )
            )
        }

        override suspend fun getSupplierById(id: String, includeInactive: Boolean) = error("unused")
        override suspend fun activateSupplier(id: String) = error("unused")
        override suspend fun createSupplier(name: String, phone: String?, address: String?, paymentTermDays: Int) = error("unused")
        override suspend fun updateSupplier(id: String, name: String, phone: String?, address: String?, paymentTermDays: Int) = error("unused")
        override suspend fun deleteSupplier(id: String) = error("unused")
        override suspend fun getPurchases(page: Int, limit: Int, supplierId: String?, startDate: String?, endDate: String?): RepositoryResult<PurchaseSummaryPage> = error("unused")
        override suspend fun getPurchaseById(id: String): RepositoryResult<PurchaseDetail> = error("unused")
        override suspend fun getPayables(page: Int, limit: Int, supplierId: String?, status: String?): RepositoryResult<SupplierPayablePage> = error("unused")
        override suspend fun getPayableById(id: String) = error("unused")
        override suspend fun createSupplierPayment(command: CreateSupplierPaymentCommand): RepositoryResult<SupplierPaymentReceipt> = error("unused")
        override suspend fun createOpeningBalance(command: CreateOpeningPayableCommand) = error("unused")
        override suspend fun createAdjustment(command: CreatePayableAdjustmentCommand) = error("unused")
    }

    private class FakeIncomingInventoryRepository : InventoryRepository {
        override suspend fun getProductStocks(
            page: Int,
            limit: Int,
            search: String?
        ): RepositoryResult<ProductStockPage> = RepositoryResult.Success(
            ProductStockPage(
                data = listOf(
                    ProductStock(
                        productId = "prod-1",
                        sku = "SMN-01",
                        productName = "Semen Tiga Roda",
                        categoryName = "Material",
                        unitName = "Sak",
                        quantity = BigDecimal("50"),
                        minStock = BigDecimal("10"),
                        priceBuy = BigDecimal("45000"),
                        priceRetail = BigDecimal("60000"),
                        priceContractor = BigDecimal("57000"),
                        discount = BigDecimal.ZERO,
                        isActive = true
                    )
                ),
                total = 1,
                page = 1,
                limit = limit,
                totalPages = 1
            )
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
