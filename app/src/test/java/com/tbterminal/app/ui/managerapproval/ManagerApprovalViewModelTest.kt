package com.tbterminal.app.ui.managerapproval

import com.tbterminal.app.data.model.CreateManagerApprovalCommand
import com.tbterminal.app.data.model.ManagerApprovalAction
import com.tbterminal.app.data.model.ManagerApprovalContext
import com.tbterminal.app.data.model.ManagerApprovalGrant
import com.tbterminal.app.data.model.ManagerApprovalResourceType
import com.tbterminal.app.data.model.ManagerApprovalStatus
import com.tbterminal.app.data.remote.ApiResponse
import com.tbterminal.app.data.remote.NetworkResult
import com.tbterminal.app.data.repository.ManagerApprovalRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
class ManagerApprovalViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val context = ManagerApprovalContext(
        action = ManagerApprovalAction.VOID_TRANSACTION,
        resourceId = "11111111-2222-3333-4444-555555555555",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun adminApprovalReturnsScopedGrantAndClearsPin() = runTest(dispatcher) {
        assertSuccessfulApproval("admin01")
    }

    @Test
    fun ownerApprovalReturnsScopedGrantAndClearsPin() = runTest(dispatcher) {
        assertSuccessfulApproval("owner01")
    }

    @Test
    fun wrongPinUsesSafeMessageAndNeverKeepsPin() = runTest(dispatcher) {
        val repository = FakeManagerApprovalRepository(
            result = NetworkResult.Error("MANAGER_APPROVAL_INVALID", "invalid"),
        )
        val viewModel = configuredViewModel(repository, username = "admin01", pin = "000999")

        assertTrue(viewModel.submit())
        assertEquals("", viewModel.uiState.value.approverPin)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(ManagerApprovalUiPhase.ERROR, viewModel.uiState.value.phase)
        assertEquals("PIN atau akun manager tidak valid.", viewModel.uiState.value.errorMessage)
        assertEquals("", viewModel.uiState.value.approverPin)
    }

    @Test
    fun cashierApproverIsRejectedWithReadableMessage() = runTest(dispatcher) {
        val repository = FakeManagerApprovalRepository(
            result = NetworkResult.Error("MANAGER_APPROVER_FORBIDDEN", "forbidden"),
        )
        val viewModel = configuredViewModel(repository, username = "kasir01", pin = "000999")

        viewModel.submit()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "Akun tersebut tidak dapat memberikan persetujuan manager.",
            viewModel.uiState.value.errorMessage,
        )
    }

    @Test
    fun networkAndRateLimitErrorsAreMapped() {
        assertEquals(
            "Persetujuan gagal karena koneksi bermasalah.",
            managerApprovalErrorMessage("NETWORK_TIMEOUT", "timeout"),
        )
        assertEquals(
            "Terlalu banyak percobaan. Coba kembali beberapa saat lagi.",
            managerApprovalErrorMessage("HTTP_429", "rate limited"),
        )
        assertEquals(
            "Persetujuan manager sudah kedaluwarsa. Silakan minta persetujuan baru.",
            managerApprovalErrorMessage("MANAGER_APPROVAL_EXPIRED", "expired"),
        )
    }

    @Test
    fun doubleSubmitStartsOnlyOneRepositoryRequest() = runTest(dispatcher) {
        val release = CompletableDeferred<Unit>()
        val repository = BlockingManagerApprovalRepository(release)
        val viewModel = configuredViewModel(repository, username = "admin01", pin = "000999")

        assertTrue(viewModel.submit())
        assertFalse(viewModel.submit())
        runCurrent()
        assertEquals(1, repository.callCount)

        release.complete(Unit)
        advanceUntilIdle()
        assertEquals(ManagerApprovalUiPhase.SUCCESS, viewModel.uiState.value.phase)
    }

    @Test
    fun actionAndResourceAreSentExactlyAsContext() = runTest(dispatcher) {
        val discountContext = ManagerApprovalContext(
            action = ManagerApprovalAction.DISCOUNT_OVERRIDE,
            resourceId = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
        )
        val repository = FakeManagerApprovalRepository(
            result = NetworkResult.Success(ApiResponse(success = true, data = grant(discountContext))),
        )
        val viewModel = ManagerApprovalViewModel(discountContext, repository)
        viewModel.onUsernameChanged("owner01")
        viewModel.onPinChanged("123456")

        viewModel.submit()
        advanceUntilIdle()

        val command = repository.lastCommand!!
        assertEquals(ManagerApprovalAction.DISCOUNT_OVERRIDE, command.action)
        assertEquals(ManagerApprovalResourceType.TRANSACTION, command.resourceType)
        assertEquals(discountContext.resourceId, command.resourceId)
        assertEquals("owner01", command.approverUsername)
        assertEquals("123456", command.approverPin)
        assertEquals("", viewModel.uiState.value.approverPin)
    }

    @Test
    fun supportedActionsAreClosedAndTypeSafe() {
        assertEquals(
            setOf(
                ManagerApprovalAction.VOID_TRANSACTION,
                ManagerApprovalAction.REFUND_TRANSACTION,
                ManagerApprovalAction.DISCOUNT_OVERRIDE,
            ),
            ManagerApprovalAction.entries.toSet(),
        )
    }

    @Test
    fun commandStringNeverExposesPin() {
        val command = CreateManagerApprovalCommand(
            action = context.action,
            resourceType = context.resourceType,
            resourceId = context.resourceId,
            approverUsername = "owner01",
            approverPin = "987654",
        )

        assertFalse(command.toString().contains("987654"))
        assertTrue(command.toString().contains("<redacted>"))
    }

    @Test
    fun invalidInputDoesNotCallRepository() = runTest(dispatcher) {
        val repository = FakeManagerApprovalRepository(successResult(context))
        val viewModel = configuredViewModel(repository, username = "admin01", pin = "123")

        assertFalse(viewModel.submit())
        advanceUntilIdle()

        assertEquals(0, repository.callCount)
        assertNull(viewModel.uiState.value.grant)
        assertEquals("PIN manager harus terdiri dari 6 digit.", viewModel.uiState.value.errorMessage)
    }

    private suspend fun assertSuccessfulApproval(username: String) {
        val repository = FakeManagerApprovalRepository(successResult(context))
        val viewModel = configuredViewModel(repository, username, "000999")

        assertTrue(viewModel.submit())
        assertEquals("", viewModel.uiState.value.approverPin)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, repository.callCount)
        assertEquals(ManagerApprovalUiPhase.SUCCESS, viewModel.uiState.value.phase)
        assertEquals(context.action, viewModel.uiState.value.grant?.action)
        assertEquals(context.resourceId, viewModel.uiState.value.grant?.resourceId)
        assertEquals("", viewModel.uiState.value.approverPin)
    }

    private fun configuredViewModel(
        repository: ManagerApprovalRepository,
        username: String,
        pin: String,
    ) = ManagerApprovalViewModel(context, repository).apply {
        onUsernameChanged(username)
        onPinChanged(pin)
    }

    private fun successResult(context: ManagerApprovalContext) = NetworkResult.Success(
        ApiResponse(success = true, data = grant(context)),
    )

    private fun grant(context: ManagerApprovalContext) = ManagerApprovalGrant(
        approvalId = "ffffffff-1111-2222-3333-444444444444",
        action = context.action,
        resourceType = context.resourceType,
        resourceId = context.resourceId,
        status = ManagerApprovalStatus.APPROVED,
        createdAt = "2026-09-01T00:00:00Z",
        expiresAt = "2026-09-01T00:05:00Z",
    )

    private class FakeManagerApprovalRepository(
        private val result: NetworkResult<ApiResponse<ManagerApprovalGrant>>,
    ) : ManagerApprovalRepository {
        var callCount = 0
        var lastCommand: CreateManagerApprovalCommand? = null

        override suspend fun createApproval(
            command: CreateManagerApprovalCommand,
        ): NetworkResult<ApiResponse<ManagerApprovalGrant>> {
            callCount += 1
            lastCommand = command
            return result
        }
    }

    private inner class BlockingManagerApprovalRepository(
        private val release: CompletableDeferred<Unit>,
    ) : ManagerApprovalRepository {
        var callCount = 0

        override suspend fun createApproval(
            command: CreateManagerApprovalCommand,
        ): NetworkResult<ApiResponse<ManagerApprovalGrant>> {
            callCount += 1
            release.await()
            return successResult(context)
        }
    }
}
