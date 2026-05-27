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
        if (username.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Username dan password tidak boleh kosong")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            when (val result = authRepository.login(username, password)) {
                is RepositoryResult.Success -> {
                    val session = result.data
                    _authState.value = AuthState.Success(
                        token = session.token,
                        role = session.user.role,
                        name = session.user.name
                    )
                }

                is RepositoryResult.Error -> {
                    _authState.value = AuthState.Error(result.message)
                }

                is RepositoryResult.Exception -> {
                    _authState.value = AuthState.Error(
                        "Terjadi kesalahan jaringan: ${result.throwable.message ?: "tidak diketahui"}"
                    )
                }
            }
        }
    }

    fun unlock(pin: String) {
        if (pin.isBlank()) {
            _unlockState.value = UnlockState.Error("PIN tidak boleh kosong")
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
                    _unlockState.value = UnlockState.Error(
                        "Terjadi kesalahan jaringan: ${result.throwable.message ?: "tidak diketahui"}"
                    )
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

    companion object {
        fun factory(authRepository: AuthRepository): ViewModelProvider.Factory {
            return viewModelFactory {
                AuthViewModel(authRepository = authRepository)
            }
        }
    }
}
