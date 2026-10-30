package com.tbterminal.app.ui.security

import com.tbterminal.app.data.model.AuditLogPage
import com.tbterminal.app.data.remote.ApiResponse
import com.tbterminal.app.data.remote.NetworkResult
import com.tbterminal.app.data.repository.SecurityLogRepository
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class SecurityLogFilterTest {

    private val testDispatcher = StandardTestDispatcher()
    private val wibZone = ZoneId.of("Asia/Jakarta")

    private class FakeSecurityLogRepository : SecurityLogRepository {
        var lastPage: Int? = null
        var lastLimit: Int? = null
        var lastAction: String? = null
        var lastRange: String? = null
        var lastStartDate: String? = null
        var lastEndDate: String? = null

        override suspend fun getAuditLogs(
            page: Int,
            limit: Int,
            action: String?,
            range: String?,
            startDate: String?,
            endDate: String?
        ): NetworkResult<ApiResponse<AuditLogPage>> {
            lastPage = page
            lastLimit = limit
            lastAction = action
            lastRange = range
            lastStartDate = startDate
            lastEndDate = endDate
            return NetworkResult.Success(
                ApiResponse(
                    success = true,
                    data = AuditLogPage(
                        data = emptyList(),
                        total = 0,
                        page = page,
                        limit = limit,
                        totalPages = 1
                    )
                )
            )
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `security date filter labels match user specifications`() {
        assertEquals("Semua", SecurityDateFilter.All.label)
        assertEquals("Hari", SecurityDateFilter.Today.label)
        assertEquals("Minggu", SecurityDateFilter.Last7Days.label)
        assertEquals("Bulan", SecurityDateFilter.Last30Days.label)
    }

    @Test
    fun `initial ui state defaults to Hari with today date in WIB`() = runTest {
        val repo = FakeSecurityLogRepository()
        val viewModel = SecurityLogViewModel(repo)
        advanceUntilIdle()

        val today = LocalDate.now(wibZone).toString()
        val state = viewModel.uiState.value

        assertEquals("Hari", state.selectedPreset)
        assertEquals(today, state.startDate)
        assertEquals(today, state.endDate)
        assertEquals(SecurityDateFilter.Today, state.dateFilter)
        assertEquals("today", repo.lastRange)
        assertEquals(today, repo.lastStartDate)
        assertEquals(today, repo.lastEndDate)
    }

    @Test
    fun `setDatePreset Minggu sets start date to Monday and end date to today`() = runTest {
        val repo = FakeSecurityLogRepository()
        val viewModel = SecurityLogViewModel(repo)
        advanceUntilIdle()

        viewModel.setDatePreset("Minggu")
        advanceUntilIdle()

        val today = LocalDate.now(wibZone)
        val expectedMonday = today.with(DayOfWeek.MONDAY).toString()
        val state = viewModel.uiState.value

        assertEquals("Minggu", state.selectedPreset)
        assertEquals(expectedMonday, state.startDate)
        assertEquals(today.toString(), state.endDate)
        assertEquals(SecurityDateFilter.Last7Days, state.dateFilter)
        assertEquals("7d", repo.lastRange)
        assertEquals(expectedMonday, repo.lastStartDate)
        assertEquals(today.toString(), repo.lastEndDate)
    }

    @Test
    fun `setDatePreset Bulan sets start date to 1st of month and end date to today`() = runTest {
        val repo = FakeSecurityLogRepository()
        val viewModel = SecurityLogViewModel(repo)
        advanceUntilIdle()

        viewModel.setDatePreset("Bulan")
        advanceUntilIdle()

        val today = LocalDate.now(wibZone)
        val expectedFirstDay = today.withDayOfMonth(1).toString()
        val state = viewModel.uiState.value

        assertEquals("Bulan", state.selectedPreset)
        assertEquals(expectedFirstDay, state.startDate)
        assertEquals(today.toString(), state.endDate)
        assertEquals(SecurityDateFilter.Last30Days, state.dateFilter)
        assertEquals("30d", repo.lastRange)
        assertEquals(expectedFirstDay, repo.lastStartDate)
        assertEquals(today.toString(), repo.lastEndDate)
    }

    @Test
    fun `setDateRange sets custom range correctly`() = runTest {
        val repo = FakeSecurityLogRepository()
        val viewModel = SecurityLogViewModel(repo)
        advanceUntilIdle()

        viewModel.setDateRange("2026-10-01", "2026-10-15", null)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.selectedPreset)
        assertEquals("2026-10-01", state.startDate)
        assertEquals("2026-10-15", state.endDate)
        assertEquals(SecurityDateFilter.Custom, state.dateFilter)
        assertEquals("01/10 - 15/10", state.historyShortDateLabel())
        assertNull(repo.lastRange)
        assertEquals("2026-10-01", repo.lastStartDate)
        assertEquals("2026-10-15", repo.lastEndDate)
    }

    @Test
    fun `setDate with specific past date sends null range and exact start and end dates`() = runTest {
        val repo = FakeSecurityLogRepository()
        val viewModel = SecurityLogViewModel(repo)
        advanceUntilIdle()

        viewModel.setDate("2026-10-25")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.selectedPreset)
        assertEquals("2026-10-25", state.startDate)
        assertEquals("2026-10-25", state.endDate)
        assertEquals(SecurityDateFilter.Custom, state.dateFilter)
        assertNull(repo.lastRange)
        assertEquals("2026-10-25", repo.lastStartDate)
        assertEquals("2026-10-25", repo.lastEndDate)
    }

    @Test
    fun `applyFilters updates date range and activity atomically and queries api once`() = runTest {
        val repo = FakeSecurityLogRepository()
        val viewModel = SecurityLogViewModel(repo)
        advanceUntilIdle()

        viewModel.applyFilters(
            startDateStr = "2026-10-20",
            endDateStr = "2026-10-25",
            preset = null,
            activityFilter = SecurityActivityFilter.Insert
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.selectedPreset)
        assertEquals("2026-10-20", state.startDate)
        assertEquals("2026-10-25", state.endDate)
        assertEquals(SecurityDateFilter.Custom, state.dateFilter)
        assertEquals(SecurityActivityFilter.Insert, state.activityFilter)
        assertNull(repo.lastRange)
        assertEquals("2026-10-20", repo.lastStartDate)
        assertEquals("2026-10-25", repo.lastEndDate)
        assertEquals("INSERT", repo.lastAction)
    }

    @Test
    fun `audit logs with postgres offset timestamps are parsed and filtered correctly`() = runTest {
        val logItem = com.tbterminal.app.data.model.AuditLogItem(
            id = "log-1",
            actorUserId = "user-1",
            actorName = "Kasir Utama",
            actorRole = "KASIR",
            action = "INSERT",
            schemaName = "pos",
            tableName = "sales",
            recordId = "sale-1",
            ipAddress = "192.168.1.100",
            oldData = null,
            newData = null,
            activityLabel = "Tambah Penjualan",
            createdAt = "2026-10-25T14:30:00+07:00"
        )
        val repo = object : SecurityLogRepository {
            override suspend fun getAuditLogs(
                page: Int,
                limit: Int,
                action: String?,
                range: String?,
                startDate: String?,
                endDate: String?
            ): NetworkResult<ApiResponse<AuditLogPage>> {
                return NetworkResult.Success(
                    ApiResponse(
                        success = true,
                        data = AuditLogPage(
                            data = listOf(logItem),
                            total = 1,
                            page = 1,
                            limit = 10,
                            totalPages = 1
                        )
                    )
                )
            }
        }
        val viewModel = SecurityLogViewModel(repo)
        advanceUntilIdle()

        viewModel.setDate("2026-10-25")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.visibleLogs.size)
        assertEquals("2026-10-25", state.visibleLogs[0].rawDateIso)
        assertEquals("25 Okt 2026", state.visibleLogs[0].dateLabel)
        assertEquals("14:30", state.visibleLogs[0].time)
    }
}
