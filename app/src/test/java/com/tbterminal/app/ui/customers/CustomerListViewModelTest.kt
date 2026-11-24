package com.tbterminal.app.ui.customers

import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.model.CustomerCommand
import com.tbterminal.app.data.model.CustomerPage
import com.tbterminal.app.data.repository.CustomerRepository
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerListViewModelTest {

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
    fun statusFilterChangesReloadPageOneWithMatchingStatusQuery() = runTest {
        val repo = FakeListCustomerRepository()
        val viewModel = CustomerListViewModel(repo)
        advanceUntilIdle()

        assertEquals(listOf(GetCustomersCall(page = 1, limit = 10, search = null, status = "all")), repo.calls)
        assertEquals(CustomerStatusFilter.ALL, viewModel.uiState.value.statusFilter)
        assertEquals(listOf("Aktif Satu", "Nonaktif Satu"), viewModel.uiState.value.customers.map { it.name })

        viewModel.onStatusFilterChanged(CustomerStatusFilter.ACTIVE)
        advanceUntilIdle()
        assertEquals(CustomerStatusFilter.ACTIVE, viewModel.uiState.value.statusFilter)
        assertEquals(listOf("Aktif Satu"), viewModel.uiState.value.customers.map { it.name })

        viewModel.onStatusFilterChanged(CustomerStatusFilter.INACTIVE)
        advanceUntilIdle()
        assertEquals(CustomerStatusFilter.INACTIVE, viewModel.uiState.value.statusFilter)
        assertEquals(listOf("Nonaktif Satu"), viewModel.uiState.value.customers.map { it.name })

        // Re-selecting same status does not fire redundant network request
        viewModel.onStatusFilterChanged(CustomerStatusFilter.INACTIVE)
        advanceUntilIdle()

        viewModel.onStatusFilterChanged(CustomerStatusFilter.ALL)
        advanceUntilIdle()
        assertEquals(CustomerStatusFilter.ALL, viewModel.uiState.value.statusFilter)

        assertEquals(
            listOf("all", "active", "inactive", "all"),
            repo.calls.map { it.status }
        )
    }

    @Test
    fun filterAppliedReloadsOnlyWhenStatusChanges() = runTest {
        val repo = FakeListCustomerRepository()
        val viewModel = CustomerListViewModel(repo)
        advanceUntilIdle()

        viewModel.onFilterApplied(CustomerCategoryFilter.CONTRACTOR, CustomerStatusFilter.ALL)
        advanceUntilIdle()
        assertEquals(1, repo.calls.size)
        assertEquals(CustomerCategoryFilter.CONTRACTOR, viewModel.uiState.value.categoryFilter)

        viewModel.onFilterApplied(CustomerCategoryFilter.CONTRACTOR, CustomerStatusFilter.INACTIVE)
        advanceUntilIdle()
        assertEquals(2, repo.calls.size)
        assertEquals("inactive", repo.calls.last().status)
        assertEquals(CustomerStatusFilter.INACTIVE, viewModel.uiState.value.statusFilter)
    }

    @Test
    fun refreshReloadsFromPageOneEvenAfterPaging() = runTest {
        val repo = FakeListCustomerRepository(totalPages = 2)
        val viewModel = CustomerListViewModel(repo)
        advanceUntilIdle()

        viewModel.nextPage()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.page)

        viewModel.refresh()
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.page)
        assertEquals(listOf(1, 2, 1), repo.calls.map { it.page })
    }
}

private data class GetCustomersCall(
    val page: Int,
    val limit: Int,
    val search: String?,
    val status: String?
)

private class FakeListCustomerRepository(
    private val totalPages: Int = 1
) : CustomerRepository {
    val calls = mutableListOf<GetCustomersCall>()

    private val allCustomers = listOf(
        customer(id = "c-active", name = "Aktif Satu", isActive = true),
        customer(id = "c-inactive", name = "Nonaktif Satu", isActive = false)
    )

    override suspend fun getCustomers(
        page: Int,
        limit: Int,
        search: String?,
        status: String?
    ): RepositoryResult<CustomerPage> {
        calls += GetCustomersCall(page = page, limit = limit, search = search, status = status)
        val filtered = when (status) {
            "active" -> allCustomers.filter { it.isActive }
            "inactive" -> allCustomers.filter { !it.isActive }
            else -> allCustomers
        }
        return RepositoryResult.Success(
            CustomerPage(
                data = filtered,
                total = filtered.size.toLong(),
                page = page,
                limit = limit,
                totalPages = totalPages
            )
        )
    }

    override suspend fun getCustomerById(id: String): RepositoryResult<Customer> =
        throw UnsupportedOperationException()

    override suspend fun createCustomer(command: CustomerCommand): RepositoryResult<Customer> =
        throw UnsupportedOperationException()

    override suspend fun updateCustomer(id: String, command: CustomerCommand): RepositoryResult<Customer> =
        throw UnsupportedOperationException()

    override suspend fun deactivateCustomer(id: String): RepositoryResult<Unit> =
        throw UnsupportedOperationException()

    override suspend fun activateCustomer(id: String): RepositoryResult<Customer> =
        throw UnsupportedOperationException()

    private fun customer(id: String, name: String, isActive: Boolean) = Customer(
        id = id,
        name = name,
        phone = "081234567890",
        address = null,
        isContractor = true,
        creditLimit = BigDecimal("1000000"),
        paymentTermDays = 14,
        isActive = isActive,
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z"
    )
}
