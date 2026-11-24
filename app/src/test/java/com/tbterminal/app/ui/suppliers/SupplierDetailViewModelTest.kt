package com.tbterminal.app.ui.suppliers

import com.tbterminal.app.data.model.CreatePurchaseCommand
import com.tbterminal.app.data.model.CreateSupplierPaymentCommand
import com.tbterminal.app.data.model.PurchaseDetail
import com.tbterminal.app.data.model.PurchaseReceipt
import com.tbterminal.app.data.model.PurchaseSummaryPage
import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.data.model.SupplierPage
import com.tbterminal.app.data.model.SupplierPayable
import com.tbterminal.app.data.model.SupplierPayablePage
import com.tbterminal.app.data.model.SupplierPaymentReceipt
import com.tbterminal.app.data.repository.PurchasingRepository
import com.tbterminal.app.data.repository.RepositoryResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SupplierDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadDetail_fetchesSupplierIncludingInactive() = runTest {
        val repo = FakeDetailPurchasingRepository(
            supplier = sampleSupplier(isActive = false)
        )

        val viewModel = SupplierDetailViewModel(repo, "sup-01")
        advanceUntilIdle()

        assertTrue(repo.lastIncludeInactive == true)
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals("PT Semen Merdeka", state.supplier?.name)
        assertEquals(false, state.supplier?.isActive)
    }

    @Test
    fun loadDetail_withBlankId_setsErrorWithoutCallingRepo() = runTest {
        val repo = FakeDetailPurchasingRepository(supplier = sampleSupplier(isActive = true))

        val viewModel = SupplierDetailViewModel(repo, " ")
        advanceUntilIdle()

        assertEquals(0, repo.getByIdCalls)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("ID supplier tidak valid.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun deactivate_marksInactiveAndSetsDeactivatedFlag() = runTest {
        val repo = FakeDetailPurchasingRepository(supplier = sampleSupplier(isActive = true))
        val viewModel = SupplierDetailViewModel(repo, "sup-01")
        advanceUntilIdle()

        viewModel.deactivate()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isDeactivated)
        assertEquals(false, state.supplier?.isActive)
    }

    @Test
    fun activateAndUndo_updatesStatusAndFlags() = runTest {
        val repo = FakeDetailPurchasingRepository(supplier = sampleSupplier(isActive = false))
        val viewModel = SupplierDetailViewModel(repo, "sup-01")
        advanceUntilIdle()

        viewModel.activate()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isReactivated)
        assertEquals(true, viewModel.uiState.value.supplier?.isActive)

        viewModel.undoActivate()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isReactivated)
        assertFalse(viewModel.uiState.value.isDeactivated)
        assertEquals(false, viewModel.uiState.value.supplier?.isActive)
    }

    @Test
    fun activate_whenNotFound_setsIsNotFoundError() = runTest {
        val repo = FakeDetailPurchasingRepository(
            supplier = sampleSupplier(isActive = false),
            activateResult = RepositoryResult.Error(code = "HTTP_404", message = "Supplier tidak ditemukan.")
        )
        val viewModel = SupplierDetailViewModel(repo, "sup-01")
        advanceUntilIdle()

        viewModel.activate()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isNotFoundError)
        assertEquals("Supplier tidak ditemukan.", state.actionErrorMessage)
    }

    private fun sampleSupplier(isActive: Boolean) = Supplier(
        id = "sup-01",
        name = "PT Semen Merdeka",
        phone = "081234567890",
        address = "Jl. Industri No. 12",
        paymentTermDays = 30,
        isActive = isActive,
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z"
    )

    private class FakeDetailPurchasingRepository(
        private var supplier: Supplier,
        private val activateResult: RepositoryResult<Supplier>? = null
    ) : PurchasingRepository {
        var getByIdCalls = 0
        var lastIncludeInactive: Boolean? = null

        override suspend fun getSuppliers(
            page: Int,
            limit: Int,
            search: String?,
            status: String?
        ): RepositoryResult<SupplierPage> =
            RepositoryResult.Success(SupplierPage(listOf(supplier), 1, 1, 10, 1))

        override suspend fun getSupplierById(
            id: String,
            includeInactive: Boolean
        ): RepositoryResult<Supplier> {
            getByIdCalls++
            lastIncludeInactive = includeInactive
            return RepositoryResult.Success(supplier)
        }

        override suspend fun activateSupplier(id: String): RepositoryResult<Supplier> {
            activateResult?.let { return it }
            supplier = supplier.copy(isActive = true)
            return RepositoryResult.Success(supplier)
        }

        override suspend fun createSupplier(
            name: String,
            phone: String?,
            address: String?,
            paymentTermDays: Int
        ): RepositoryResult<Supplier> = RepositoryResult.Success(supplier)

        override suspend fun updateSupplier(
            id: String,
            name: String,
            phone: String?,
            address: String?,
            paymentTermDays: Int
        ): RepositoryResult<Supplier> = RepositoryResult.Success(supplier)

        override suspend fun deleteSupplier(id: String): RepositoryResult<Unit> {
            supplier = supplier.copy(isActive = false)
            return RepositoryResult.Success(Unit)
        }

        override suspend fun createPurchase(command: CreatePurchaseCommand): RepositoryResult<PurchaseReceipt> =
            RepositoryResult.Error(code = "ERR", message = "Not implemented")

        override suspend fun getPurchases(
            page: Int,
            limit: Int,
            supplierId: String?,
            startDate: String?,
            endDate: String?
        ): RepositoryResult<PurchaseSummaryPage> =
            RepositoryResult.Success(PurchaseSummaryPage(emptyList(), 0, 1, 10, 1))

        override suspend fun getPurchaseById(id: String): RepositoryResult<PurchaseDetail> =
            RepositoryResult.Error(code = "ERR", message = "Not implemented")

        override suspend fun getPayables(
            page: Int,
            limit: Int,
            supplierId: String?,
            status: String?
        ): RepositoryResult<SupplierPayablePage> =
            RepositoryResult.Success(SupplierPayablePage(emptyList(), 0, 1, 10, 1))

        override suspend fun getPayableById(id: String): RepositoryResult<SupplierPayable> =
            RepositoryResult.Error(code = "ERR", message = "Not implemented")

        override suspend fun createSupplierPayment(command: CreateSupplierPaymentCommand): RepositoryResult<SupplierPaymentReceipt> =
            RepositoryResult.Error(code = "ERR", message = "Not implemented")

        override suspend fun createOpeningBalance(
            command: com.tbterminal.app.data.model.CreateOpeningPayableCommand
        ): RepositoryResult<SupplierPayable> =
            RepositoryResult.Error(code = "ERR", message = "Not implemented")

        override suspend fun createAdjustment(
            command: com.tbterminal.app.data.model.CreatePayableAdjustmentCommand
        ): RepositoryResult<SupplierPayable> =
            RepositoryResult.Error(code = "ERR", message = "Not implemented")
    }
}
