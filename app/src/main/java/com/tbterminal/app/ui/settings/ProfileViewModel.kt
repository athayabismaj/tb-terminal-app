package com.tbterminal.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.UserProfile
import com.tbterminal.app.data.repository.AuthRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val profile: UserProfile? = null,
    val error: String? = null,
    val message: String? = null
)

class ProfileViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, message = null) }
            when (val result = authRepository.getProfile()) {
                is RepositoryResult.Success -> _uiState.update {
                    it.copy(isLoading = false, profile = result.data)
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is RepositoryResult.Exception -> _uiState.update {
                    it.copy(isLoading = false, error = "Profil gagal dimuat karena koneksi bermasalah.")
                }
            }
        }
    }

    fun changePassword(oldPassword: String, newPassword: String, confirmation: String) {
        validatePasswordChange(oldPassword, newPassword, confirmation)?.let { validationError ->
            _uiState.update { it.copy(error = validationError, message = null) }
            return
        }
        submitCredentialChange {
            authRepository.changeMyPassword(oldPassword, newPassword)
        }
    }

    fun changePin(oldPin: String, newPin: String, confirmation: String) {
        validatePinChange(oldPin, newPin, confirmation)?.let { validationError ->
            _uiState.update { it.copy(error = validationError, message = null) }
            return
        }
        submitCredentialChange {
            authRepository.changeMyPin(oldPin, newPin)
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(error = null, message = null) }
    }

    private fun submitCredentialChange(request: suspend () -> RepositoryResult<Unit>) {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, message = null) }
            when (val result = request()) {
                is RepositoryResult.Success -> _uiState.update {
                    it.copy(isSaving = false, message = "Credential berhasil diperbarui.")
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isSaving = false, error = result.message)
                }
                is RepositoryResult.Exception -> _uiState.update {
                    it.copy(isSaving = false, error = "Perubahan gagal karena koneksi bermasalah.")
                }
            }
        }
    }

    companion object {
        fun factory(authRepository: AuthRepository): ViewModelProvider.Factory = viewModelFactory {
            ProfileViewModel(authRepository)
        }
    }
}

internal fun validatePasswordChange(
    oldPassword: String,
    newPassword: String,
    confirmation: String
): String? = when {
    oldPassword.isBlank() -> "Password lama wajib diisi."
    newPassword.length < 6 -> "Password baru minimal 6 karakter."
    oldPassword == newPassword -> "Password baru harus berbeda dari password lama."
    newPassword != confirmation -> "Konfirmasi password tidak sama."
    else -> null
}

internal fun validatePinChange(oldPin: String, newPin: String, confirmation: String): String? {
    val pinPattern = Regex("\\d{4,6}")
    return when {
        !oldPin.matches(pinPattern) -> "PIN lama harus terdiri dari 4 sampai 6 digit."
        !newPin.matches(pinPattern) -> "PIN baru harus terdiri dari 4 sampai 6 digit."
        oldPin == newPin -> "PIN baru harus berbeda dari PIN lama."
        newPin != confirmation -> "Konfirmasi PIN tidak sama."
        else -> null
    }
}
