package com.tbterminal.app.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.ManagedUser
import com.tbterminal.app.data.model.UpdateUserCommand
import com.tbterminal.app.data.model.UserPage
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

enum class UserCredentialMode {
    Password,
    Pin
}

data class UserCredentialUiState(
    val targetUser: ManagedUser? = null,
    val credential: String = "",
    val confirmation: String = "",
    val isLoadingUser: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

sealed interface UserCredentialEvent {
    data class CredentialUpdated(val user: ManagedUser) : UserCredentialEvent
}

class UserCredentialViewModel(
    private val userRepository: UserRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(UserCredentialUiState())
    val uiState: StateFlow<UserCredentialUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UserCredentialEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<UserCredentialEvent> = _events.asSharedFlow()

    private var loadedUserId: String? = null

    fun load(userId: String) {
        if (loadedUserId == userId && _uiState.value.targetUser != null) return

        loadedUserId = userId
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isLoadingUser = true, errorMessage = null)
            }

            when (val result = userRepository.getUsers(limit = USER_PAGE_LIMIT)) {
                is RepositoryResult.Success -> handleUsers(userId = userId, page = result.data)
                is RepositoryResult.Error -> setError(result.message, loadingFinished = true)
                is RepositoryResult.Exception -> {
                    setError("Data user gagal dimuat karena koneksi ke server bermasalah.", loadingFinished = true)
                }
            }
        }
    }

    fun updateCredential(value: String) {
        _uiState.update { state ->
            state.copy(credential = value, errorMessage = null)
        }
    }

    fun updateConfirmation(value: String) {
        _uiState.update { state ->
            state.copy(confirmation = value, errorMessage = null)
        }
    }

    fun submit(mode: UserCredentialMode) {
        val state = _uiState.value
        val user = state.targetUser
        val credential = state.credential
        val confirmation = state.confirmation

        when {
            state.isSubmitting -> return
            user == null -> setFormError("User belum siap diperbarui.")
            credential.isBlank() -> setFormError(mode.emptyMessage())
            mode == UserCredentialMode.Password && credential.length < MIN_PASSWORD_LENGTH -> {
                setFormError("Password baru minimal $MIN_PASSWORD_LENGTH karakter.")
            }
            mode == UserCredentialMode.Pin && !credential.all(Char::isDigit) -> {
                setFormError("PIN hanya boleh berisi angka.")
            }
            mode == UserCredentialMode.Pin && credential.length != PIN_LENGTH -> {
                setFormError("PIN baru harus terdiri dari $PIN_LENGTH digit.")
            }
            credential != confirmation -> setFormError(mode.mismatchMessage())
            else -> updateCredentialOnServer(user = user, mode = mode, credential = credential)
        }
    }

    private fun updateCredentialOnServer(
        user: ManagedUser,
        mode: UserCredentialMode,
        credential: String
    ) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(isSubmitting = true, errorMessage = null)
            }

            val command = UpdateUserCommand(
                name = user.name,
                username = user.username,
                isActive = user.isActive,
                roleId = user.roleId,
                email = user.email?.takeIf(String::isNotBlank),
                newPassword = credential.takeIf { mode == UserCredentialMode.Password },
                newPin = credential.takeIf { mode == UserCredentialMode.Pin }
            )

            when (val result = userRepository.updateUser(user.id, command)) {
                is RepositoryResult.Success -> handleUpdatedUser(result.data)
                is RepositoryResult.Error -> setError(result.message)
                is RepositoryResult.Exception -> {
                    setError(mode.connectionErrorMessage())
                }
            }
        }
    }

    private fun handleUsers(
        userId: String,
        page: UserPage
    ) {
        val user = page.data.firstOrNull { candidate -> candidate.id == userId }
        when {
            user == null -> setError("User tidak ditemukan.", loadingFinished = true)
            user.roleName.equals(OWNER_ROLE_NAME, ignoreCase = true) -> {
                setError("Akun owner tidak ditampilkan di manajemen karyawan.", loadingFinished = true)
            }
            else -> {
                _uiState.update { state ->
                    state.copy(
                        targetUser = user,
                        credential = "",
                        confirmation = "",
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
        _events.tryEmit(UserCredentialEvent.CredentialUpdated(user))
    }

    private fun setFormError(message: String) {
        _uiState.update { state ->
            state.copy(errorMessage = message)
        }
    }

    private fun setError(
        message: String,
        loadingFinished: Boolean = false
    ) {
        _uiState.update { state ->
            state.copy(
                isLoadingUser = if (loadingFinished) false else state.isLoadingUser,
                isSubmitting = false,
                errorMessage = message
            )
        }
    }

    private fun UserCredentialMode.emptyMessage(): String {
        return when (this) {
            UserCredentialMode.Password -> "Password baru wajib diisi."
            UserCredentialMode.Pin -> "PIN baru wajib diisi."
        }
    }

    private fun UserCredentialMode.mismatchMessage(): String {
        return when (this) {
            UserCredentialMode.Password -> "Konfirmasi password tidak sama."
            UserCredentialMode.Pin -> "Konfirmasi PIN tidak sama."
        }
    }

    private fun UserCredentialMode.connectionErrorMessage(): String {
        return when (this) {
            UserCredentialMode.Password -> "Password gagal diperbarui karena koneksi ke server bermasalah."
            UserCredentialMode.Pin -> "PIN gagal diperbarui karena koneksi ke server bermasalah."
        }
    }

    companion object {
        private const val USER_PAGE_LIMIT = 200
        private const val MIN_PASSWORD_LENGTH = 6
        private const val PIN_LENGTH = 6
        private const val OWNER_ROLE_NAME = "OWNER"

        fun factory(userRepository: UserRepository): ViewModelProvider.Factory {
            return viewModelFactory {
                UserCredentialViewModel(userRepository = userRepository)
            }
        }
    }
}
