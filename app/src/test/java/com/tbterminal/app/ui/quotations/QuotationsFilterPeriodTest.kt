package com.tbterminal.app.ui.quotations

import com.tbterminal.app.data.model.CreateQuotationCommand
import com.tbterminal.app.data.model.QuotationDetail
import com.tbterminal.app.data.model.QuotationPage
import com.tbterminal.app.data.model.QuotationStatus
import com.tbterminal.app.data.model.QuotationSummary
import com.tbterminal.app.data.repository.QuotationRepository
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class QuotationsFilterPeriodTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeQuotationRepository(
        val items: List<QuotationSummary> = emptyList(),
    ) : QuotationRepository {
        var lastStatus: String? = null
        var lastSearch: String? = null
        var lastStartDate: String? = null
        var lastEndDate: String? = null

        override suspend fun getQuotations(
            page: Int,
            limit: Int,
            status: String?,
            search: String?,
            startDate: String?,
            endDate: String?,
        ): RepositoryResult<QuotationPage> {
            lastStatus = status
            lastSearch = search
            lastStartDate = startDate
            lastEndDate = endDate

            return RepositoryResult.Success(
                QuotationPage(
                    data = items,
                    total = items.size.toLong(),
                    page = page,
                    limit = limit,
                    totalPages = 1,
                )
            )
        }

        override suspend fun createQuotation(command: CreateQuotationCommand): RepositoryResult<QuotationDetail> =
            error("Not needed")

        override suspend fun getQuotationById(id: String): RepositoryResult<QuotationDetail> =
            error("Not needed")

        override suspend fun updateQuotationStatus(
            id: String,
            status: QuotationStatus,
            convertedTransactionId: String?,
        ): RepositoryResult<QuotationDetail> =
            error("Not needed")
    }

    private fun summary(id: String, date: String, status: QuotationStatus = QuotationStatus.DRAFT) = QuotationSummary(
        id = id,
        quotationNumber = "RAB-$id",
        userId = "u-1",
        status = status,
        total = BigDecimal("100000"),
        itemCount = 1,
        createdAt = "${date}T10:00:00Z",
    )

    @Test
    fun `default state has Semua preset with null dates`() = runTest {
        val repo = FakeQuotationRepository()
        val vm = QuotationsViewModel(repo)
        advanceUntilIdle()

        assertEquals("Semua", vm.uiState.value.selectedPeriodPreset)
        assertNull(vm.uiState.value.startDate)
        assertNull(vm.uiState.value.endDate)
        assertNull(repo.lastStartDate)
        assertNull(repo.lastEndDate)
    }

    @Test
    fun `applyFilters updates preset, date range, status and calls repo`() = runTest {
        val today = LocalDate.of(2026, 11, 16)
        val repo = FakeQuotationRepository()
        val vm = QuotationsViewModel(repo)
        advanceUntilIdle()

        vm.applyFilters(
            status = QuotationStatus.SENT.name,
            preset = "Hari ini",
            start = today,
            end = today,
        )
        advanceUntilIdle()

        assertEquals("Hari ini", vm.uiState.value.selectedPeriodPreset)
        assertEquals(today, vm.uiState.value.startDate)
        assertEquals(today, vm.uiState.value.endDate)
        assertEquals(QuotationStatus.SENT.name, vm.uiState.value.selectedStatus)
        assertEquals("2026-11-16", repo.lastStartDate)
        assertEquals("2026-11-16", repo.lastEndDate)
        assertEquals(QuotationStatus.SENT.name, repo.lastStatus)
    }

    @Test
    fun `resetFilters resets preset, dates, and status to default`() = runTest {
        val repo = FakeQuotationRepository()
        val vm = QuotationsViewModel(repo)
        advanceUntilIdle()

        vm.applyFilters(
            status = QuotationStatus.DRAFT.name,
            preset = "Kustom",
            start = LocalDate.of(2026, 11, 1),
            end = LocalDate.of(2026, 11, 10),
        )
        advanceUntilIdle()

        vm.resetFilters()
        advanceUntilIdle()

        assertEquals("Semua", vm.uiState.value.selectedPeriodPreset)
        assertNull(vm.uiState.value.startDate)
        assertNull(vm.uiState.value.endDate)
        assertEquals(ALL_QUOTATION_STATUSES, vm.uiState.value.selectedStatus)
    }

    @Test
    fun `client-side safety net filters out records outside date range`() = runTest {
        val q1 = summary("1", "2026-11-10")
        val q2 = summary("2", "2026-11-15")
        val q3 = summary("3", "2026-11-20")

        val repo = FakeQuotationRepository(items = listOf(q1, q2, q3))
        val vm = QuotationsViewModel(repo)
        advanceUntilIdle()
        assertEquals(3, vm.uiState.value.quotations.size)

        vm.applyFilters(
            status = ALL_QUOTATION_STATUSES,
            preset = "Kustom",
            start = LocalDate.of(2026, 11, 12),
            end = LocalDate.of(2026, 11, 16),
        )
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.quotations.size)
        assertEquals("2", vm.uiState.value.quotations[0].id)
    }
}
