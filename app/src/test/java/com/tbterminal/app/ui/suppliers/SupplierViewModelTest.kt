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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SupplierViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val fakeRepo = FakePurchasingRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun defaultFilter_isAll() = runTest {
        val viewModel = SupplierViewModel(fakeRepo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SupplierStatusFilter.ALL, state.statusFilter)
        assertEquals(2, state.visibleSuppliers.size)
    }

    @Test
    fun statusFilterChanged_filtersActiveSuppliers() = runTest {
        val viewModel = SupplierViewModel(fakeRepo)
        advanceUntilIdle()

        viewModel.onStatusFilterChanged(SupplierStatusFilter.ACTIVE)

        val state = viewModel.uiState.value
        assertEquals(SupplierStatusFilter.ACTIVE, state.statusFilter)
        assertEquals(1, state.visibleSuppliers.size)
        assertEquals("Supplier Aktif", state.visibleSuppliers.first().name)
    }

    @Test
    fun statusFilterChanged_filtersInactiveSuppliers() = runTest {
        val viewModel = SupplierViewModel(fakeRepo)
        advanceUntilIdle()

        viewModel.onStatusFilterChanged(SupplierStatusFilter.INACTIVE)

        val state = viewModel.uiState.value
        assertEquals(SupplierStatusFilter.INACTIVE, state.statusFilter)
        assertEquals(1, state.visibleSuppliers.size)
        assertEquals("Supplier Nonaktif", state.visibleSuppliers.first().name)
    }

    private class FakePurchasingRepository : PurchasingRepository {
        private val list = listOf(
            Supplier(
                id = "1",
                name = "Supplier Aktif",
                phone = "0812345",
                address = "Jl. Merdeka",
                paymentTermDays = 14,
                isActive = true,
                createdAt = "2026-01-01",
                updatedAt = "2026-01-01"
            ),
            Supplier(
                id = "2",
                name = "Supplier Nonaktif",
                phone = null,
                address = null,
                paymentTermDays = 30,
                isActive = false,
                createdAt = "2026-01-01",
                updatedAt = "2026-01-01"
            ),
        )

        override suspend fun getSuppliers(page: Int, limit: Int, search: String?): RepositoryResult<SupplierPage> {
            return RepositoryResult.Success(
                SupplierPage(
                    data = list,
                    total = list.size.toLong(),
                    page = 1,
                    limit = 10,
                    totalPages = 1
                )
            )
        }

        override suspend fun createSupplier(name: String, phone: String?, address: String?, paymentTermDays: Int): RepositoryResult<Supplier> {
            return RepositoryResult.Success(
                Supplier(
                    id = "3",
                    name = name,
                    phone = phone,
                    address = address,
                    paymentTermDays = paymentTermDays,
                    isActive = true,
                    createdAt = "2026-01-01",
                    updatedAt = "2026-01-01"
                )
            )
        }

        override suspend fun updateSupplier(id: String, name: String, phone: String?, address: String?, paymentTermDays: Int): RepositoryResult<Supplier> {
            return RepositoryResult.Success(
                Supplier(
                    id = id,
                    name = name,
                    phone = phone,
                    address = address,
                    paymentTermDays = paymentTermDays,
                    isActive = true,
                    createdAt = "2026-01-01",
                    updatedAt = "2026-01-01"
                )
            )
        }

        override suspend fun deleteSupplier(id: String): RepositoryResult<Unit> = RepositoryResult.Success(Unit)

        override suspend fun createPurchase(command: CreatePurchaseCommand): RepositoryResult<PurchaseReceipt> =
            RepositoryResult.Error(code = "ERR", message = "Not implemented")

        override suspend fun getPurchases(page: Int, limit: Int, supplierId: String?, startDate: String?, endDate: String?): RepositoryResult<PurchaseSummaryPage> =
            RepositoryResult.Success(PurchaseSummaryPage(emptyList(), 0, 1, 10, 1))

        override suspend fun getPurchaseById(id: String): RepositoryResult<PurchaseDetail> =
            RepositoryResult.Error(code = "ERR", message = "Not implemented")

        override suspend fun getPayables(page: Int, limit: Int, supplierId: String?, status: String?): RepositoryResult<SupplierPayablePage> =
            RepositoryResult.Success(SupplierPayablePage(emptyList(), 0, 1, 10, 1))

        override suspend fun getPayableById(id: String): RepositoryResult<SupplierPayable> =
            RepositoryResult.Error(code = "ERR", message = "Not implemented")

        override suspend fun createSupplierPayment(command: CreateSupplierPaymentCommand): RepositoryResult<SupplierPaymentReceipt> =
            RepositoryResult.Error(code = "ERR", message = "Not implemented")
    }
}
