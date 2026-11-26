package com.tbterminal.app.ui.checkout

import com.tbterminal.app.data.model.CashExpense
import com.tbterminal.app.data.model.CashExpensePage
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.model.CashSessionPage
import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.data.model.CashTransactionPage
import com.tbterminal.app.data.model.CheckoutPreview
import com.tbterminal.app.data.model.CheckoutPreviewCommand
import com.tbterminal.app.data.model.CheckoutReceipt
import com.tbterminal.app.data.model.CheckoutSubmitCommand
import com.tbterminal.app.data.model.CreateProductCommand
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.model.CustomerCommand
import com.tbterminal.app.data.model.CustomerPage
import com.tbterminal.app.data.model.Product
import com.tbterminal.app.data.model.ProductCategory
import com.tbterminal.app.data.model.ProductCategoryPage
import com.tbterminal.app.data.model.ProductCsvImportResult
import com.tbterminal.app.data.model.ProductCsvPreview
import com.tbterminal.app.data.model.ProductDetail
import com.tbterminal.app.data.model.ProductPage
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
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckoutDeliveryFeeInputTest {
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
    fun updateDeliveryFee_usesWholeRupiahIdrRulesAndDoesNotConcatenateCents() = runTest(testDispatcher) {
        val vm = buildViewModel()

        vm.updateDeliveryFee("15.000")
        assertEquals("15.000", vm.uiState.value.deliveryFeeInput)
        assertEquals(0, BigDecimal("15000").compareTo(vm.uiState.value.deliveryFee))

        vm.updateDeliveryFee("20000,00")
        assertEquals("20000,00", vm.uiState.value.deliveryFeeInput)
        assertEquals(0, BigDecimal("20000").compareTo(vm.uiState.value.deliveryFee))

        vm.updateDeliveryFee("1.500,50")
        assertEquals("1.500,50", vm.uiState.value.deliveryFeeInput)
        assertEquals(0, BigDecimal.ZERO.compareTo(vm.uiState.value.deliveryFee))
    }

    private fun buildViewModel(): CheckoutViewModel {
        return CheckoutViewModel(
            checkoutRepository = object : CheckoutRepository {
                override suspend fun previewCheckout(command: CheckoutPreviewCommand): RepositoryResult<CheckoutPreview> = error("unused")
                override suspend fun submitCheckout(command: CheckoutSubmitCommand): RepositoryResult<CheckoutReceipt> = error("unused")
            },
            inventoryRepository = object : InventoryRepository {
                override suspend fun getProductStocks(page: Int, limit: Int, search: String?): RepositoryResult<ProductStockPage> =
                    RepositoryResult.Success(ProductStockPage(emptyList(), 0L, page, limit, 1))
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
                override suspend fun getActiveSession(): RepositoryResult<CashSession?> = RepositoryResult.Success(null)
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
}
