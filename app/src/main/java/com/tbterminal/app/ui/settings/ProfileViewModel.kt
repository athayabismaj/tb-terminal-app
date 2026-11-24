package com.tbterminal.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.model.UserProfile
import com.tbterminal.app.data.repository.AuthRepository
import com.tbterminal.app.data.repository.PinUnlockResult
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
    val isNetworkError: Boolean = false,
    val message: String? = null,
    val profileUpdated: Boolean = false
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
            _uiState.update { it.copy(error = validationError, isNetworkError = false, message = null) }
            return
        }
        submitCredentialChange(
            successMessage = "Kata sandi diperbarui",
            mapError = ::mapPasswordChangeError
        ) {
            authRepository.changeMyPassword(oldPassword, newPassword)
        }
    }

    fun changePin(oldPin: String, newPin: String, confirmation: String) {
        validatePinChange(oldPin, newPin, confirmation)?.let { validationError ->
            _uiState.update { it.copy(error = validationError, isNetworkError = false, message = null) }
            return
        }
        submitCredentialChange(
            successMessage = "PIN diperbarui",
            mapError = { _, msg -> msg }
        ) {
            authRepository.changeMyPin(oldPin, newPin)
        }
    }

    fun verifyCurrentPin(
        pin: String,
        onResult: (PinVerifyOutcome) -> Unit
    ) {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, isNetworkError = false, message = null) }
            when (val res = authRepository.unlock(pin)) {
                is RepositoryResult.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    when (val unlock = res.data) {
                        PinUnlockResult.Unlocked -> onResult(PinVerifyOutcome.Verified)
                        is PinUnlockResult.Failed -> onResult(
                            PinVerifyOutcome.Rejected(
                                attemptsLeft = unlock.attemptsLeft,
                                isLocked = unlock.isLocked,
                                message = unlock.message
                            )
                        )
                        is PinUnlockResult.ForceLogout -> onResult(PinVerifyOutcome.ForceLogout(unlock.message))
                    }
                }
                is RepositoryResult.Error -> {
                    val networkFail = isNetworkFailureCode(res.code)
                    _uiState.update { it.copy(isSaving = false) }
                    onResult(
                        PinVerifyOutcome.Error(
                            message = if (networkFail) "Tidak dapat terhubung" else res.message,
                            isNetworkError = networkFail
                        )
                    )
                }
                is RepositoryResult.Exception -> {
                    _uiState.update { it.copy(isSaving = false) }
                    onResult(PinVerifyOutcome.Error(message = "Tidak dapat terhubung", isNetworkError = true))
                }
            }
        }
    }

    fun resetPinWithPassword(
        currentPassword: String,
        newPin: String,
        confirmation: String,
        onSuccess: () -> Unit
    ) {
        if (currentPassword.isBlank()) {
            _uiState.update { it.copy(error = "Kata sandi saat ini wajib diisi", isNetworkError = false) }
            return
        }
        if (!newPin.matches(Regex("\\d{6}"))) {
            _uiState.update { it.copy(error = "PIN baru harus 6 digit angka.", isNetworkError = false) }
            return
        }
        if (newPin != confirmation) {
            _uiState.update { it.copy(error = "Konfirmasi PIN tidak sama.", isNetworkError = false) }
            return
        }
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, isNetworkError = false, message = null) }
            when (val result = authRepository.resetPinWithPassword(currentPassword, newPin)) {
                is RepositoryResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, message = "PIN diperbarui") }
                    onSuccess()
                }
                is RepositoryResult.Error -> {
                    val networkFail = isNetworkFailureCode(result.code)
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = if (networkFail) "Tidak dapat terhubung" else mapPasswordChangeError(result.code, result.message),
                            isNetworkError = networkFail
                        )
                    }
                }
                is RepositoryResult.Exception -> {
                    _uiState.update {
                        it.copy(isSaving = false, error = "Tidak dapat terhubung", isNetworkError = true)
                    }
                }
            }
        }
    }

    fun updateProfile(name: String, username: String, email: String?) {
        validateProfileForm(name, username, email)?.let { validationError ->
            _uiState.update { it.copy(error = validationError, message = null) }
            return
        }
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, message = null, profileUpdated = false) }
            when (
                val result = authRepository.updateProfile(
                    name = name.trim(),
                    username = username.trim(),
                    email = email?.trim()?.takeIf { it.isNotBlank() }
                )
            ) {
                is RepositoryResult.Success -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        profile = result.data,
                        message = "Profil berhasil diperbarui.",
                        profileUpdated = true
                    )
                }
                is RepositoryResult.Error -> _uiState.update {
                    it.copy(isSaving = false, error = result.message)
                }
                is RepositoryResult.Exception -> _uiState.update {
                    it.copy(isSaving = false, error = "Profil gagal diperbarui karena koneksi bermasalah.")
                }
            }
        }
    }

    fun consumeProfileUpdated() {
        _uiState.update { it.copy(profileUpdated = false) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(error = null, isNetworkError = false, message = null) }
    }

    private fun submitCredentialChange(
        successMessage: String,
        mapError: (String, String) -> String,
        request: suspend () -> RepositoryResult<Unit>
    ) {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, isNetworkError = false, message = null) }
            when (val result = request()) {
                is RepositoryResult.Success -> _uiState.update {
                    it.copy(isSaving = false, message = successMessage, isNetworkError = false)
                }
                is RepositoryResult.Error -> {
                    val networkFail = isNetworkFailureCode(result.code)
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            error = if (networkFail) "Tidak dapat terhubung" else mapError(result.code, result.message),
                            isNetworkError = networkFail
                        )
                    }
                }
                is RepositoryResult.Exception -> _uiState.update {
                    it.copy(isSaving = false, error = "Tidak dapat terhubung", isNetworkError = true)
                }
            }
        }
    }

    private fun isNetworkFailureCode(code: String): Boolean = code in NETWORK_FAILURE_CODES

    private fun mapPasswordChangeError(code: String, message: String): String {
        val lower = message.lowercase()
        val isWrongCurrent = code == "HTTP_401" ||
            code == "INVALID_CREDENTIALS" ||
            code == "INVALID_PASSWORD" ||
            code == "WRONG_PASSWORD" ||
            ((lower.contains("lama") || lower.contains("saat ini")) && lower.contains("salah")) ||
            lower.contains("incorrect") ||
            lower.contains("invalid current password")
        return if (isWrongCurrent) "Kata sandi saat ini salah" else message
    }

    companion object {
        private val NETWORK_FAILURE_CODES = setOf(
            "NETWORK_TIMEOUT",
            "NETWORK_UNAVAILABLE",
            "CONNECTION_FAILED",
            "CONNECTION_INTERRUPTED"
        )

        fun factory(authRepository: AuthRepository): ViewModelProvider.Factory = viewModelFactory {
            ProfileViewModel(authRepository)
        }
    }
}

sealed interface PinVerifyOutcome {
    data object Verified : PinVerifyOutcome
    data class Rejected(val attemptsLeft: Int, val isLocked: Boolean, val message: String) : PinVerifyOutcome
    data class ForceLogout(val message: String) : PinVerifyOutcome
    data class Error(val message: String, val isNetworkError: Boolean) : PinVerifyOutcome
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

internal fun validateProfileForm(name: String, email: String?): String? =
    validateProfileForm(name = name, username = "default_user", email = email)

internal fun validateProfileForm(
    name: String,
    username: String,
    email: String?
): String? = when {
    name.trim().length !in 2..100 -> "Nama harus terdiri dari 2 sampai 100 karakter."
    !username.trim().matches(Regex("^[a-zA-Z0-9._-]{3,50}$")) ->
        "Username minimal 3 karakter (huruf, angka, titik, garis bawah, atau hubung tanpa spasi)."
    email != null && email.trim().length > 150 -> "Email maksimal 150 karakter."
    email != null && email.isNotBlank() && !email.trim().matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) ->
        "Format email tidak valid."
    else -> null
}
