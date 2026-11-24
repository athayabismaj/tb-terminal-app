package com.tbterminal.app.ui.users

import com.tbterminal.app.data.model.CreateUserCommand
import com.tbterminal.app.data.model.ManagedUser
import com.tbterminal.app.data.model.UpdateUserCommand
import com.tbterminal.app.data.model.UserPage
import com.tbterminal.app.data.model.UserRole
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserCredentialAndAddUserValidationTest {

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
    fun userCredentialViewModel_enforces8To72PasswordAnd4To6DigitPin() = runTest {
        val repo = RecordingUserRepository()
        val vm = UserCredentialViewModel(repo)
        vm.load("u1")
        advanceUntilIdle()

        // 7-char password rejected
        vm.updateCredential("1234567")
        vm.updateConfirmation("1234567")
        vm.submit(UserCredentialMode.Password)
        assertNotNull(vm.uiState.value.errorMessage)
        assertEquals(0, repo.updateCallCount)

        // 73-char password rejected
        vm.updateCredential("a".repeat(73))
        vm.updateConfirmation("a".repeat(73))
        vm.submit(UserCredentialMode.Password)
        assertNotNull(vm.uiState.value.errorMessage)
        assertEquals(0, repo.updateCallCount)

        // 8-char password accepted
        vm.updateCredential("12345678")
        vm.updateConfirmation("12345678")
        vm.submit(UserCredentialMode.Password)
        advanceUntilIdle()
        assertEquals(1, repo.updateCallCount)
        assertEquals("12345678", repo.lastUpdateCommand?.newPassword)

        // 3-digit and non-digit PIN rejected
        vm.updateCredential("123")
        vm.updateConfirmation("123")
        vm.submit(UserCredentialMode.Pin)
        assertNotNull(vm.uiState.value.errorMessage)
        assertEquals(1, repo.updateCallCount)

        vm.updateCredential("12a4")
        vm.updateConfirmation("12a4")
        vm.submit(UserCredentialMode.Pin)
        assertNotNull(vm.uiState.value.errorMessage)
        assertEquals(1, repo.updateCallCount)

        // 4-digit PIN accepted
        vm.updateCredential("2580")
        vm.updateConfirmation("2580")
        vm.submit(UserCredentialMode.Pin)
        advanceUntilIdle()
        assertEquals(2, repo.updateCallCount)
        assertEquals("2580", repo.lastUpdateCommand?.newPin)
    }

    @Test
    fun addUserViewModel_enforces8To72PasswordAnd4To6NumericPin() = runTest {
        val repo = RecordingUserRepository()
        val vm = AddUserViewModel(repo)
        advanceUntilIdle()

        vm.updateFullName("Budi Santoso")
        vm.updateUsername("budi.kasir")

        // 7-char password rejected
        vm.updatePassword("1234567")
        vm.updatePin("2580")
        vm.submit()
        assertNotNull(vm.uiState.value.errorMessage)
        assertEquals(0, repo.createCallCount)

        // 73-char password rejected
        vm.updatePassword("b".repeat(73))
        vm.updatePin("2580")
        vm.submit()
        assertNotNull(vm.uiState.value.errorMessage)
        assertEquals(0, repo.createCallCount)

        // Non-digit PIN rejected instead of silently stripped
        vm.updatePassword("validPass8")
        vm.updatePin("12ab56")
        vm.submit()
        assertNotNull(vm.uiState.value.errorMessage)
        assertEquals(0, repo.createCallCount)

        // 3-digit PIN rejected
        vm.updatePin("123")
        vm.submit()
        assertNotNull(vm.uiState.value.errorMessage)
        assertEquals(0, repo.createCallCount)

        // 4-digit PIN + 8-char password accepted
        vm.updatePin("2580")
        vm.submit()
        advanceUntilIdle()
        assertNull(vm.uiState.value.errorMessage)
        assertEquals(1, repo.createCallCount)
        assertEquals("2580", repo.lastCreateCommand?.pin)
    }

    @Test
    fun userManagementViewModel_rejectsNonDigitPinAndInvalidPasswordLengths() = runTest {
        val repo = RecordingUserRepository()
        val vm = UserManagementViewModel(repo)
        advanceUntilIdle()

        val staff = vm.uiState.value.staffMembers.first()
        val baseInput = StaffEditInput(
            name = staff.name,
            username = staff.username,
            email = staff.rawEmail.orEmpty(),
            roleId = staff.roleId,
            isActive = true,
            newPassword = "",
            newPin = ""
        )

        // Previously .filter(Char::isDigit) would silently strip "ab" into "123456" — must reject now!
        vm.updateUser(staff, baseInput.copy(newPin = "12ab3456"))
        assertNotNull(vm.uiState.value.actionErrorMessage)
        assertEquals(0, repo.updateCallCount)

        // 6-char password rejected
        vm.updateUser(staff, baseInput.copy(newPassword = "abc123"))
        assertNotNull(vm.uiState.value.actionErrorMessage)
        assertEquals(0, repo.updateCallCount)

        // 73-char password rejected
        vm.updateUser(staff, baseInput.copy(newPassword = "c".repeat(73)))
        assertNotNull(vm.uiState.value.actionErrorMessage)
        assertEquals(0, repo.updateCallCount)

        // 4-digit PIN + 8-char password accepted
        vm.updateUser(staff, baseInput.copy(newPassword = "valid123", newPin = "4829"))
        advanceUntilIdle()
        assertNull(vm.uiState.value.actionErrorMessage)
        assertEquals(1, repo.updateCallCount)
        assertEquals("valid123", repo.lastUpdateCommand?.newPassword)
        assertEquals("4829", repo.lastUpdateCommand?.newPin)
    }

    private class RecordingUserRepository : UserRepository {
        var createCallCount = 0
        var updateCallCount = 0
        var lastCreateCommand: CreateUserCommand? = null
        var lastUpdateCommand: UpdateUserCommand? = null

        private val user = ManagedUser(
            id = "u1",
            roleId = "r-kasir",
            roleName = "KASIR",
            name = "Budi Kasir",
            username = "budi",
            email = null,
            isActive = true,
            lastLogin = null,
            createdAt = "2026-01-01T00:00:00Z"
        )

        override suspend fun getUsers(page: Int, limit: Int, search: String?): RepositoryResult<UserPage> =
            RepositoryResult.Success(UserPage(listOf(user), 1, 1, limit, 1))

        override suspend fun getRoles(): RepositoryResult<List<UserRole>> =
            RepositoryResult.Success(listOf(UserRole("r-kasir", "KASIR")))

        override suspend fun createUser(command: CreateUserCommand): RepositoryResult<ManagedUser> {
            createCallCount++
            lastCreateCommand = command
            return RepositoryResult.Success(user)
        }

        override suspend fun updateUser(userId: String, command: UpdateUserCommand): RepositoryResult<ManagedUser> {
            updateCallCount++
            lastUpdateCommand = command
            return RepositoryResult.Success(user)
        }

        override suspend fun deactivateUser(userId: String): RepositoryResult<Unit> =
            RepositoryResult.Success(Unit)
    }
}
