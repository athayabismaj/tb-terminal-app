package com.tbterminal.app.data.repository

import com.tbterminal.app.data.model.CreateUserCommand
import com.tbterminal.app.data.model.ManagedUser
import com.tbterminal.app.data.model.UpdateUserCommand
import com.tbterminal.app.data.model.UserPage
import com.tbterminal.app.data.model.UserRole
import com.tbterminal.app.data.remote.PaginatedResponse
import com.tbterminal.app.data.remote.RoleResponseDto
import com.tbterminal.app.data.remote.UserApi
import com.tbterminal.app.data.remote.UserCreateRequest
import com.tbterminal.app.data.remote.UserResponseDto
import com.tbterminal.app.data.remote.UserUpdateRequest
import com.tbterminal.app.data.remote.safeApiCall

interface UserRepository {
    suspend fun getUsers(
        page: Int = 1,
        limit: Int = 200,
        search: String? = null
    ): RepositoryResult<UserPage>

    suspend fun getRoles(): RepositoryResult<List<UserRole>>

    suspend fun createUser(command: CreateUserCommand): RepositoryResult<ManagedUser>

    suspend fun updateUser(
        userId: String,
        command: UpdateUserCommand
    ): RepositoryResult<ManagedUser>

    suspend fun deactivateUser(userId: String): RepositoryResult<Unit>
}

class RemoteUserRepository(
    private val userApi: UserApi
) : UserRepository {
    override suspend fun getUsers(
        page: Int,
        limit: Int,
        search: String?
    ): RepositoryResult<UserPage> {
        return safeApiCall { userApi.getUsers(page = page, limit = limit) }
            .toRepositoryResult { response ->
                val userPage = response.data
                if (!response.success || userPage == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "USERS_FAILED",
                        message = response.message ?: response.error ?: "Daftar pengguna gagal dimuat."
                    )
                } else {
                    RepositoryResult.Success(userPage.toUserPage())
                }
            }
    }

    override suspend fun getRoles(): RepositoryResult<List<UserRole>> {
        return safeApiCall { userApi.getRoles() }
            .toRepositoryResult { response ->
                val roles = response.data
                if (!response.success || roles == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "ROLES_FAILED",
                        message = response.message ?: response.error ?: "Role pengguna gagal dimuat."
                    )
                } else {
                    RepositoryResult.Success(roles.map(RoleResponseDto::toUserRole))
                }
            }
    }

    override suspend fun createUser(command: CreateUserCommand): RepositoryResult<ManagedUser> {
        val request = UserCreateRequest(
            name = command.name,
            username = command.username,
            password = command.password,
            pin = command.pin,
            email = command.email,
            roleId = command.roleId
        )

        return safeApiCall { userApi.createUser(request) }
            .toRepositoryResult { response ->
                val user = response.data
                if (!response.success || user == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "CREATE_USER_FAILED",
                        message = response.message ?: response.error ?: "User gagal disimpan."
                    )
                } else {
                    RepositoryResult.Success(user.toManagedUser())
                }
            }
    }

    override suspend fun updateUser(
        userId: String,
        command: UpdateUserCommand
    ): RepositoryResult<ManagedUser> {
        val request = UserUpdateRequest(
            name = command.name,
            username = command.username,
            isActive = command.isActive,
            roleId = command.roleId,
            email = command.email,
            newPassword = command.newPassword,
            newPin = command.newPin
        )

        return safeApiCall { userApi.updateUser(userId, request) }
            .toRepositoryResult { response ->
                val user = response.data
                if (!response.success || user == null) {
                    RepositoryResult.Error(
                        code = response.code ?: "UPDATE_USER_FAILED",
                        message = response.message ?: response.error ?: "User gagal diperbarui."
                    )
                } else {
                    RepositoryResult.Success(user.toManagedUser())
                }
            }
    }

    override suspend fun deactivateUser(userId: String): RepositoryResult<Unit> {
        return safeApiCall { userApi.deactivateUser(userId) }
            .toRepositoryResult { response ->
                if (!response.success) {
                    RepositoryResult.Error(
                        code = response.code ?: "DEACTIVATE_USER_FAILED",
                        message = response.message ?: response.error ?: "User gagal dinonaktifkan."
                    )
                } else {
                    RepositoryResult.Success(Unit)
                }
            }
    }
}

private fun PaginatedResponse<UserResponseDto>.toUserPage(): UserPage {
    return UserPage(
        data = data.map(UserResponseDto::toManagedUser),
        total = total,
        page = page,
        limit = limit,
        totalPages = totalPages
    )
}

private fun UserResponseDto.toManagedUser(): ManagedUser {
    return ManagedUser(
        id = id,
        roleId = roleId,
        roleName = roleName,
        name = name,
        username = username,
        email = email,
        isActive = isActive,
        lastLogin = lastLogin,
        createdAt = createdAt
    )
}

private fun RoleResponseDto.toUserRole(): UserRole {
    return UserRole(
        id = id,
        name = name
    )
}
