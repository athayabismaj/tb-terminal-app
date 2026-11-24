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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserDetailViewModelTest {

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
    fun loadDetail_findsNonOwnerUserIncludingInactive() = runTest {
        val repo = FakeDetailUserRepository(
            users = listOf(sampleManagedUser(id = "u1", isActive = false))
        )

        val viewModel = UserDetailViewModel(repo, "u1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals("Andi Pratama", state.staff?.name)
        assertEquals(StaffStatus.Inactive, state.staff?.status)
    }

    @Test
    fun loadDetail_whenUserIsOwnerOrMissing_setsNotFoundError() = runTest {
        val repo = FakeDetailUserRepository(
            users = listOf(sampleManagedUser(id = "owner-1", roleName = "OWNER"))
        )

        val viewModel = UserDetailViewModel(repo, "owner-1")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Pengguna tidak ditemukan.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun deactivateActivateAndUndo_updatesStatusCorrectly() = runTest {
        val repo = FakeDetailUserRepository(
            users = listOf(sampleManagedUser(id = "u1", isActive = true))
        )
        val viewModel = UserDetailViewModel(repo, "u1")
        advanceUntilIdle()

        viewModel.deactivate()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isDeactivated)
        assertEquals(StaffStatus.Inactive, viewModel.uiState.value.staff?.status)

        viewModel.resetDeactivatedState()
        viewModel.activate()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isReactivated)
        assertEquals(StaffStatus.Active, viewModel.uiState.value.staff?.status)

        viewModel.undoActivate()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isReactivated)
        assertFalse(viewModel.uiState.value.isDeactivated)
        assertEquals(StaffStatus.Inactive, viewModel.uiState.value.staff?.status)
    }

    @Test
    fun activate_whenHttp404_setsNotFoundFlag() = runTest {
        val repo = FakeDetailUserRepository(
            users = listOf(sampleManagedUser(id = "u1", isActive = false)),
            updateResult = RepositoryResult.Error(code = "HTTP_404", message = "Pengguna tidak ditemukan.")
        )
        val viewModel = UserDetailViewModel(repo, "u1")
        advanceUntilIdle()

        viewModel.activate()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isNotFoundError)
        assertEquals("Pengguna tidak ditemukan.", viewModel.uiState.value.actionErrorMessage)
    }

    @Test
    fun changePasswordAndPin_updatesStaffAndSetsSuccessMessage() = runTest {
        val repo = FakeDetailUserRepository(
            users = listOf(sampleManagedUser(id = "u1", isActive = true))
        )
        val viewModel = UserDetailViewModel(repo, "u1")
        advanceUntilIdle()

        viewModel.changePassword("barokah25", "barokah25")
        advanceUntilIdle()
        assertEquals("Kata sandi berhasil diperbarui", viewModel.uiState.value.credentialSuccessMessage)
        assertEquals("barokah25", repo.lastCommand?.newPassword)

        viewModel.clearCredentialMessages()
        viewModel.changePassword("1234567", "1234567")
        assertNotNull(viewModel.uiState.value.credentialErrorMessage)

        viewModel.clearCredentialMessages()
        viewModel.changePassword("a".repeat(73), "a".repeat(73))
        assertNotNull(viewModel.uiState.value.credentialErrorMessage)

        viewModel.clearCredentialMessages()
        viewModel.changePin("1234", "1234")
        advanceUntilIdle()
        assertEquals("PIN transaksi berhasil diperbarui", viewModel.uiState.value.credentialSuccessMessage)
        assertEquals("1234", repo.lastCommand?.newPin)

        viewModel.clearCredentialMessages()
        viewModel.changePin("357924", "357924")
        advanceUntilIdle()
        assertEquals("PIN transaksi berhasil diperbarui", viewModel.uiState.value.credentialSuccessMessage)
        assertEquals("357924", repo.lastCommand?.newPin)
    }

    private fun sampleManagedUser(
        id: String,
        roleName: String = "ADMIN",
        isActive: Boolean = true
    ) = ManagedUser(
        id = id,
        name = "Andi Pratama",
        email = "andi@toko.id",
        username = "andi.admin",
        roleId = "r-admin",
        roleName = roleName,
        isActive = isActive,
        lastLogin = null,
        createdAt = "2026-01-01T00:00:00Z"
    )

    private class FakeDetailUserRepository(
        private val users: List<ManagedUser>,
        private val updateResult: RepositoryResult<ManagedUser>? = null
    ) : UserRepository {
        var lastCommand: UpdateUserCommand? = null
            private set

        override suspend fun getUsers(
            page: Int,
            limit: Int,
            search: String?
        ): RepositoryResult<UserPage> =
            RepositoryResult.Success(UserPage(users, users.size.toLong(), 1, limit, 1))

        override suspend fun getRoles(): RepositoryResult<List<UserRole>> =
            RepositoryResult.Success(emptyList())

        override suspend fun createUser(command: CreateUserCommand): RepositoryResult<ManagedUser> =
            RepositoryResult.Error(code = "ERR", message = "Not implemented")

        override suspend fun updateUser(
            userId: String,
            command: UpdateUserCommand
        ): RepositoryResult<ManagedUser> {
            lastCommand = command
            updateResult?.let { return it }
            val existing = users.first { it.id == userId }
            return RepositoryResult.Success(
                existing.copy(
                    name = command.name,
                    username = command.username,
                    email = command.email,
                    roleId = command.roleId,
                    isActive = command.isActive
                )
            )
        }

        override suspend fun deactivateUser(userId: String): RepositoryResult<Unit> =
            RepositoryResult.Success(Unit)
    }
}
