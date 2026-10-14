package com.tbterminal.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.repository.AuthRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.ui.common.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val token: String, val role: String, val name: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed interface UnlockState {
    data object Idle : UnlockState
    data object Loading : UnlockState
    data object Success : UnlockState
    data class Error(val message: String) : UnlockState
}

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _unlockState = MutableStateFlow<UnlockState>(UnlockState.Idle)
    val unlockState: StateFlow<UnlockState> = _unlockState.asStateFlow()

    fun login(username: String, password: String) {
        val normalizedUsername = username.trim()
        if (normalizedUsername.isBlank()) {
            _authState.value = AuthState.Error("Nama pengguna wajib diisi")
            return
        }
        if (password.isBlank()) {
            _authState.value = AuthState.Error("Kata sandi wajib diisi")
            return
        }
        if (normalizedUsername.length > 50) {
            _authState.value = AuthState.Error("Nama pengguna maksimal 50 karakter")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("Kata sandi minimal 6 karakter")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            when (val result = authRepository.login(normalizedUsername, password)) {
                is RepositoryResult.Success -> {
                    val session = result.data
                    _authState.value = AuthState.Success(
                        token = session.token,
                        role = session.user.role,
                        name = session.user.name
                    )
                }

                is RepositoryResult.Error -> {
                    val rawMessage = result.message.trim()
                    val lower = rawMessage.lowercase()
                    val message = when {
                        lower.contains("akun tidak aktif") -> {
                            "Akun tidak aktif. Hubungi admin toko."
                        }
                        lower.contains("kredensial default") -> {
                            "Kredensial default tidak diizinkan pada production."
                        }
                        result.code == "401" ||
                        result.code.contains("401") ||
                        result.code == "LOGIN_FAILED" ||
                        result.code == "INVALID_CREDENTIALS" ||
                        result.code == "UNAUTHORIZED" ||
                        lower.contains("username atau password") ||
                        lower.contains("nama pengguna atau kata sandi") ||
                        lower.contains("kredensial") ||
                        lower.contains("sesi anda telah habis") ||
                        lower.contains("unauthorized") ||
                        lower.contains("login gagal") -> {
                            "Nama pengguna atau kata sandi salah"
                        }
                        lower.contains("nama pengguna tidak") || lower.contains("user tidak") || lower.contains("user not found") -> {
                            "Nama pengguna tidak ditemukan"
                        }
                        (lower.contains("sandi") || lower.contains("password")) &&
                        !lower.contains("username") && !lower.contains("nama pengguna") -> {
                            "Kata sandi yang Anda masukkan salah"
                        }
                        else -> rawMessage
                    }
                    _authState.value = AuthState.Error(message)
                }

                is RepositoryResult.Exception -> {
                    _authState.value = AuthState.Error("Terjadi kesalahan tak terduga. Silakan coba lagi.")
                }
            }
        }
    }

    fun unlock(pin: String) {
        if (!pin.matches(Regex("\\d{4,6}"))) {
            _unlockState.value = UnlockState.Error("PIN harus terdiri dari 4 sampai 6 digit")
            return
        }

        _unlockState.value = UnlockState.Loading
        viewModelScope.launch {
            when (val result = authRepository.unlock(pin)) {
                is RepositoryResult.Success -> {
                    _unlockState.value = UnlockState.Success
                }

                is RepositoryResult.Error -> {
                    _unlockState.value = UnlockState.Error(result.message)
                }

                is RepositoryResult.Exception -> {
                    _unlockState.value = UnlockState.Error("Terjadi kesalahan tak terduga. Silakan coba lagi.")
                }
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun resetUnlockState() {
        _unlockState.value = UnlockState.Idle
    }

    fun notifySessionExpired() {
        _authState.value = AuthState.Error("Sesi telah berakhir. Silakan login kembali.")
    }

    companion object {
        fun factory(authRepository: AuthRepository): ViewModelProvider.Factory {
            return viewModelFactory {
                AuthViewModel(authRepository = authRepository)
            }
        }
    }
}
