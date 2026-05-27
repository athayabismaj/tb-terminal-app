package com.tbterminal.app.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.ManagedUser
import com.tbterminal.app.data.model.UpdateUserCommand
import com.tbterminal.app.data.model.UserPage
import com.tbterminal.app.data.model.UserRole
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.repository.UserRepository
import com.tbterminal.app.ui.common.viewModelFactory
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditUserUiState(
    val userId: String? = null,
    val fullName: String = "",
    val email: String = "",
    val username: String = "",
    val selectedRoleId: String? = null,
    val isActive: Boolean = true,
    val roles: List<UserRole> = emptyList(),
    val isLoadingUser: Boolean = false,
    val isLoadingRoles: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
) {
    val selectedRole: UserRole?
        get() = roles.firstOrNull { role -> role.id == selectedRoleId }
}

sealed interface EditUserEvent {
    data class UserUpdated(val user: ManagedUser) : EditUserEvent
}

class EditUserViewModel(
    private val userRepository: UserRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditUserUiState())
    val uiState: StateFlow<EditUserUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EditUserEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<EditUserEvent> = _events.asSharedFlow()

    private var loadedUserId: String? = null

    fun load(userId: String) {
        if (loadedUserId == userId) return

        loadedUserId = userId
        loadRoles()
        loadUser(userId)
    }

    fun loadRoles() {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isLoadingRoles = true, errorMessage = null)
            }

            when (val result = userRepository.getRoles()) {
                is RepositoryResult.Success -> handleRoles(result.data)
                is RepositoryResult.Error -> setError(result.message, rolesFinished = true)
                is RepositoryResult.Exception -> {
                    setError("Role gagal dimuat karena koneksi ke server bermasalah.", rolesFinished = true)
                }
            }
        }
    }

    fun loadUser(userId: String) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    userId = userId,
                    isLoadingUser = true,
                    errorMessage = null
                )
            }

            when (val result = userRepository.getUsers(limit = USER_PAGE_LIMIT)) {
                is RepositoryResult.Success -> handleUsers(userId = userId, page = result.data)
                is RepositoryResult.Error -> setError(result.message, userFinished = true)
                is RepositoryResult.Exception -> {
                    setError("Data user gagal dimuat karena koneksi ke server bermasalah.", userFinished = true)
                }
            }
        }
    }

    fun updateFullName(value: String) = updateForm { state -> state.copy(fullName = value) }

    fun updateEmail(value: String) = updateForm { state -> state.copy(email = value) }

    fun updateUsername(value: String) = updateForm { state -> state.copy(username = value) }

    fun selectRole(roleId: String) = updateForm { state -> state.copy(selectedRoleId = roleId) }

    fun updateActive(value: Boolean) = updateForm { state -> state.copy(isActive = value) }

    fun submit() {
        val state = _uiState.value
        val userId = state.userId
        val roleId = state.selectedRoleId

        when {
            state.isSubmitting -> return
            userId.isNullOrBlank() -> setFormError("User belum siap diperbarui.")
            state.fullName.isBlank() -> setFormError("Nama lengkap tidak boleh kosong.")
            state.username.isBlank() -> setFormError("Username tidak boleh kosong.")
            roleId.isNullOrBlank() -> setFormError("Pilih role pengguna terlebih dahulu.")
            else -> updateUser(userId = userId, state = state, roleId = roleId)
        }
    }

    private fun updateUser(
        userId: String,
        state: EditUserUiState,
        roleId: String
    ) {
        viewModelScope.launch {
            _uiState.update { current ->
                current.copy(isSubmitting = true, errorMessage = null)
            }

            val command = UpdateUserCommand(
                name = state.fullName.trim(),
                username = state.username.trim(),
                isActive = state.isActive,
                roleId = roleId,
                email = state.email.trim().takeIf(String::isNotBlank)
            )

            when (val result = userRepository.updateUser(userId, command)) {
                is RepositoryResult.Success -> handleUpdatedUser(result.data)
                is RepositoryResult.Error -> setError(result.message)
                is RepositoryResult.Exception -> {
                    setError("User gagal diperbarui karena koneksi ke server bermasalah.")
                }
            }
        }
    }

    private fun handleRoles(roles: List<UserRole>) {
        val employeeRoles = roles
            .filterNot { role -> role.name.equals(OWNER_ROLE_NAME, ignoreCase = true) }

        if (employeeRoles.isEmpty()) {
            setError("Role karyawan tidak tersedia.", rolesFinished = true)
            return
        }

        _uiState.update { state ->
            state.copy(
                roles = employeeRoles,
                selectedRoleId = state.selectedRoleId?.takeIf { selected ->
                    employeeRoles.any { role -> role.id == selected }
                } ?: employeeRoles.first().id,
                isLoadingRoles = false,
                errorMessage = null
            )
        }
    }

    private fun handleUsers(
        userId: String,
        page: UserPage
    ) {
        val user = page.data.firstOrNull { candidate -> candidate.id == userId }
        when {
            user == null -> {
                setError("User tidak ditemukan.", userFinished = true)
            }
            user.roleName.equals(OWNER_ROLE_NAME, ignoreCase = true) -> {
                setError("Akun owner tidak ditampilkan di manajemen karyawan.", userFinished = true)
            }
            else -> {
                _uiState.update { state ->
                    state.copy(
                        userId = user.id,
                        fullName = user.name,
                        email = user.email.orEmpty(),
                        username = user.username,
                        selectedRoleId = user.roleId,
                        isActive = user.isActive,
                        isLoadingUser = false,
                        errorMessage = null
                    )
                }
            }
        }
    }

    private fun handleUpdatedUser(user: ManagedUser) {
        _uiState.update { state ->
            state.copy(isSubmitting = false, errorMessage = null)
        }
        _events.tryEmit(EditUserEvent.UserUpdated(user))
    }

    private fun updateForm(transform: (EditUserUiState) -> EditUserUiState) {
        _uiState.update { state ->
            transform(state).copy(errorMessage = null)
        }
    }

    private fun setFormError(message: String) {
        _uiState.update { state ->
            state.copy(errorMessage = message)
        }
    }

    private fun setError(
        message: String,
        userFinished: Boolean = false,
        rolesFinished: Boolean = false
    ) {
        _uiState.update { state ->
            state.copy(
                isLoadingUser = if (userFinished) false else state.isLoadingUser,
                isLoadingRoles = if (rolesFinished) false else state.isLoadingRoles,
                isSubmitting = false,
                errorMessage = message
            )
        }
    }

    companion object {
        private const val USER_PAGE_LIMIT = 200
        private const val OWNER_ROLE_NAME = "OWNER"

        fun factory(userRepository: UserRepository): ViewModelProvider.Factory {
            return viewModelFactory {
                EditUserViewModel(userRepository = userRepository)
            }
        }
    }
}
