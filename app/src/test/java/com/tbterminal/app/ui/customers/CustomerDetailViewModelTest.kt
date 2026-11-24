package com.tbterminal.app.ui.customers

import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.model.CustomerCommand
import com.tbterminal.app.data.model.CustomerPage
import com.tbterminal.app.data.repository.CustomerRepository
import com.tbterminal.app.data.repository.RepositoryResult
import java.io.IOException
import java.math.BigDecimal
import kotlinx.coroutines.CompletableDeferred
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
class CustomerDetailViewModelTest {

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
    fun activateUpdatesCustomerStateSetsReactivatedFlagAndGuardsDoubleTap() = runTest {
        val deferred = CompletableDeferred<RepositoryResult<Customer>>()
        val repo = FakeDetailCustomerRepository(
            initialCustomer = sampleCustomer(isActive = false),
            onActivate = { deferred.await() }
        )
        val viewModel = CustomerDetailViewModel(repo, "cust-01")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.customer!!.isActive)

        // Rapid double-tap while request is in flight
        viewModel.activate()
        viewModel.activate()
        dispatcher.scheduler.runCurrent()
        assertEquals(1, repo.activateCount)

        deferred.complete(RepositoryResult.Success(sampleCustomer(isActive = true)))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isMutating)
        assertTrue(state.customer!!.isActive)
        assertTrue(state.isReactivated)
        assertFalse(state.isDeactivated)
        assertNull(state.actionErrorMessage)
    }

    @Test
    fun undoActivateDeactivatesWithoutLeavingDetailScreen() = runTest {
        val repo = FakeDetailCustomerRepository(
            initialCustomer = sampleCustomer(isActive = true)
        )
        val viewModel = CustomerDetailViewModel(repo, "cust-01")
        advanceUntilIdle()

        viewModel.undoActivate()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, repo.deactivateCount)
        assertFalse(state.customer!!.isActive)
        assertFalse(state.isDeactivated)
        assertFalse(state.isReactivated)
    }

    @Test
    fun activate404SetsNotFoundErrorAndOfflineSetsConnectionMessage() = runTest {
        val repo = FakeDetailCustomerRepository(
            initialCustomer = sampleCustomer(isActive = false),
            onActivate = { RepositoryResult.Error("HTTP_404", "Pelanggan sudah dihapus atau tidak ditemukan.") }
        )
        val viewModel = CustomerDetailViewModel(repo, "cust-01")
        advanceUntilIdle()

        viewModel.activate()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isNotFoundError)
        assertEquals("Pelanggan sudah dihapus atau tidak ditemukan.", viewModel.uiState.value.actionErrorMessage)

        viewModel.clearActionError()
        assertFalse(viewModel.uiState.value.isNotFoundError)
        assertNull(viewModel.uiState.value.actionErrorMessage)

        repo.onActivate = { RepositoryResult.Exception(IOException("offline")) }
        viewModel.activate()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isNotFoundError)
        assertEquals(
            "Pelanggan gagal diaktifkan karena koneksi bermasalah.",
            viewModel.uiState.value.actionErrorMessage
        )
    }

    private fun sampleCustomer(isActive: Boolean) = Customer(
        id = "cust-01",
        name = "Budi Santoso",
        phone = "081234567890",
        address = "Jl. Sudirman No. 45",
        isContractor = true,
        creditLimit = BigDecimal("5000000"),
        paymentTermDays = 14,
        isActive = isActive,
        createdAt = "2026-01-10T08:00:00Z",
        updatedAt = "2026-02-15T10:30:00Z"
    )
}

private class FakeDetailCustomerRepository(
    private val initialCustomer: Customer,
    var onActivate: suspend () -> RepositoryResult<Customer> = {
        RepositoryResult.Success(initialCustomer.copy(isActive = true))
    }
) : CustomerRepository {
    var activateCount = 0
    var deactivateCount = 0

    override suspend fun getCustomers(
        page: Int,
        limit: Int,
        search: String?,
        status: String?
    ): RepositoryResult<CustomerPage> =
        throw UnsupportedOperationException()

    override suspend fun getCustomerById(id: String): RepositoryResult<Customer> =
        RepositoryResult.Success(initialCustomer)

    override suspend fun createCustomer(command: CustomerCommand): RepositoryResult<Customer> =
        throw UnsupportedOperationException()

    override suspend fun updateCustomer(id: String, command: CustomerCommand): RepositoryResult<Customer> =
        throw UnsupportedOperationException()

    override suspend fun deactivateCustomer(id: String): RepositoryResult<Unit> {
        deactivateCount++
        return RepositoryResult.Success(Unit)
    }

    override suspend fun activateCustomer(id: String): RepositoryResult<Customer> {
        activateCount++
        return onActivate()
    }
}
