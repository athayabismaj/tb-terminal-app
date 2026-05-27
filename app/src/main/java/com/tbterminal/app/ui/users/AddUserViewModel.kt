package com.tbterminal.app.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.CreateUserCommand
import com.tbterminal.app.data.model.ManagedUser
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

data class AddUserUiState(
    val fullName: String = "",
    val email: String = "",
    val username: String = "",
    val password: String = "",
    val pin: String = "",
    val roles: List<UserRole> = emptyList(),
    val selectedRoleId: String? = null,
    val isLoadingRoles: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

sealed interface AddUserEvent {
    data class UserCreated(val user: ManagedUser) : AddUserEvent
}

class AddUserViewModel(
    private val userRepository: UserRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddUserUiState(isLoadingRoles = true))
    val uiState: StateFlow<AddUserUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddUserEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<AddUserEvent> = _events.asSharedFlow()

    init {
        loadRoles()
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

    fun updateFullName(value: String) = updateForm { state -> state.copy(fullName = value) }

    fun updateEmail(value: String) = updateForm { state -> state.copy(email = value) }

    fun updateUsername(value: String) = updateForm { state -> state.copy(username = value) }

    fun updatePassword(value: String) = updateForm { state -> state.copy(password = value) }

    fun updatePin(value: String) = updateForm { state ->
        state.copy(pin = value.filter(Char::isDigit).take(PIN_LENGTH))
    }

    fun selectRole(roleId: String) = updateForm { state -> state.copy(selectedRoleId = roleId) }

    fun submit() {
        val state = _uiState.value
        val selectedRoleId = state.selectedRoleId

        when {
            state.isSubmitting -> return
            state.fullName.isBlank() -> setFormError("Nama lengkap tidak boleh kosong.")
            state.username.isBlank() -> setFormError("Username tidak boleh kosong.")
            state.password.length < MIN_PASSWORD_LENGTH -> {
                setFormError("Password minimal $MIN_PASSWORD_LENGTH karakter.")
            }
            state.pin.length != PIN_LENGTH -> {
                setFormError("PIN akses harus terdiri dari $PIN_LENGTH digit.")
            }
            selectedRoleId == null -> setFormError("Pilih role pengguna terlebih dahulu.")
            else -> createUser(state = state, roleId = selectedRoleId)
        }
    }

    private fun createUser(
        state: AddUserUiState,
        roleId: String
    ) {
        viewModelScope.launch {
            _uiState.update { current ->
                current.copy(isSubmitting = true, errorMessage = null)
            }

            val command = CreateUserCommand(
                name = state.fullName.trim(),
                username = state.username.trim(),
                password = state.password,
                pin = state.pin,
                email = state.email.trim().takeIf(String::isNotBlank),
                roleId = roleId
            )

            when (val result = userRepository.createUser(command)) {
                is RepositoryResult.Success -> handleCreatedUser(result.data)
                is RepositoryResult.Error -> setError(result.message)
                is RepositoryResult.Exception -> {
                    setError("User gagal disimpan karena koneksi ke server bermasalah.")
                }
            }
        }
    }

    private fun handleRoles(roles: List<UserRole>) {
        val assignableRoles = roles
            .filterNot { role -> role.name.equals("owner", ignoreCase = true) }

        if (assignableRoles.isEmpty()) {
            setError("Role pengguna tidak tersedia.", rolesFinished = true)
            return
        }

        val preferredRoleId = assignableRoles
            .firstOrNull { role -> role.name.equals("kasir", ignoreCase = true) }
            ?.id
            ?: assignableRoles.first().id

        _uiState.update { state ->
            state.copy(
                roles = assignableRoles,
                selectedRoleId = state.selectedRoleId ?: preferredRoleId,
                isLoadingRoles = false,
                errorMessage = null
            )
        }
    }

    private fun handleCreatedUser(user: ManagedUser) {
        _uiState.update { state ->
            state.copy(isSubmitting = false, errorMessage = null)
        }
        _events.tryEmit(AddUserEvent.UserCreated(user))
    }

    private fun updateForm(transform: (AddUserUiState) -> AddUserUiState) {
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
        rolesFinished: Boolean = false
    ) {
        _uiState.update { state ->
            state.copy(
                isSubmitting = false,
                isLoadingRoles = if (rolesFinished) false else state.isLoadingRoles,
                errorMessage = message
            )
        }
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 6
        private const val PIN_LENGTH = 6

        fun factory(userRepository: UserRepository): ViewModelProvider.Factory {
            return viewModelFactory {
                AddUserViewModel(userRepository = userRepository)
            }
        }
    }
}
