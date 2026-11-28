package com.tbterminal.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tbterminal.app.data.repository.AuthRepository
import com.tbterminal.app.data.repository.PinUnlockResult
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.sync.defaultElapsedRealtimeMillis
import com.tbterminal.app.data.validation.CredentialValidator
import com.tbterminal.app.ui.common.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val token: String, val role: String, val name: String) : AuthState()
    data class Error(
        val message: String,
        val usernameError: String? = null,
        val passwordError: String? = null,
        val isUsernameHighlighted: Boolean = false,
        val isPasswordHighlighted: Boolean = false
    ) : AuthState()
}

sealed interface UnlockState {
    data object Idle : UnlockState
    data object Loading : UnlockState
    data object Success : UnlockState
    data class WrongPin(val message: String, val attemptsLeft: Int) : UnlockState
    data class LockedOut(
        val message: String,
        val lockedUntilEpochMillis: Long,
        val remainingSeconds: Long
    ) : UnlockState
    data class ForceLogout(val message: String) : UnlockState
    data class Error(val message: String) : UnlockState
}

sealed interface ResetPinState {
    data object Idle : ResetPinState
    data object Loading : ResetPinState
    data object Success : ResetPinState
    data class Error(val message: String) : ResetPinState
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val clockMillis: () -> Long = { System.currentTimeMillis() },
    private val tickIntervalMillis: Long = 1_000L,
    private val elapsedRealtimeMillis: () -> Long = { defaultElapsedRealtimeMillis() },
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _unlockState = MutableStateFlow<UnlockState>(UnlockState.Idle)
    val unlockState: StateFlow<UnlockState> = _unlockState.asStateFlow()

    private val _resetPinState = MutableStateFlow<ResetPinState>(ResetPinState.Idle)
    val resetPinState: StateFlow<ResetPinState> = _resetPinState.asStateFlow()

    private val _pendingForgotPinReset = MutableStateFlow(false)
    val pendingForgotPinReset: StateFlow<Boolean> = _pendingForgotPinReset.asStateFlow()

    private val _loginNotice = MutableStateFlow<String?>(null)
    val loginNotice: StateFlow<String?> = _loginNotice.asStateFlow()

    private var verifiedPasswordForPinReset: String? = null
    private var lockoutCountdownJob: Job? = null

    val hasVerifiedPasswordForPinReset: Boolean
        get() = !verifiedPasswordForPinReset.isNullOrBlank()

    fun login(username: String, password: String) {
        _loginNotice.value = null
        val normalizedUsername = username.trim()
        val isUsernameBlank = normalizedUsername.isBlank()
        val isPasswordBlank = password.isBlank()
        if (isUsernameBlank || isPasswordBlank) {
            val usernameMsg = if (isUsernameBlank) "Nama pengguna wajib diisi" else null
            val passwordMsg = if (isPasswordBlank) "Kata sandi wajib diisi" else null
            val summary = when {
                isUsernameBlank && isPasswordBlank -> "Nama pengguna dan kata sandi wajib diisi"
                isUsernameBlank -> "Nama pengguna wajib diisi"
                else -> "Kata sandi wajib diisi"
            }
            _authState.value = AuthState.Error(
                message = summary,
                usernameError = usernameMsg,
                passwordError = passwordMsg,
                isUsernameHighlighted = isUsernameBlank,
                isPasswordHighlighted = isPasswordBlank
            )
            return
        }
        if (normalizedUsername.length > 50) {
            _authState.value = AuthState.Error(
                message = "Nama pengguna maksimal 50 karakter",
                usernameError = "Nama pengguna maksimal 50 karakter",
                isUsernameHighlighted = true
            )
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error(
                message = "Kata sandi minimal 6 karakter",
                passwordError = "Kata sandi minimal 6 karakter",
                isPasswordHighlighted = true
            )
            return
        }
        if (password.length > CredentialValidator.PASSWORD_MAX_LENGTH) {
            val maxMsg = "Kata sandi maksimal ${CredentialValidator.PASSWORD_MAX_LENGTH} karakter"
            _authState.value = AuthState.Error(
                message = maxMsg,
                passwordError = maxMsg,
                isPasswordHighlighted = true
            )
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            when (val result = authRepository.login(normalizedUsername, password)) {
                is RepositoryResult.Success -> {
                    if (_pendingForgotPinReset.value) {
                        verifiedPasswordForPinReset = password
                    }
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
                    val isNetwork = lower.contains("koneksi") || lower.contains("internet") ||
                        lower.contains("network") || lower.contains("timeout") ||
                        lower.contains("jaringan") || lower.contains("server tidak dapat") ||
                        lower.contains("server sedang")
                    val isCredentialFailure = result.code == "401" ||
                        result.code.contains("401") ||
                        result.code == "LOGIN_FAILED" ||
                        result.code == "INVALID_CREDENTIALS" ||
                        result.code == "UNAUTHORIZED" ||
                        lower.contains("username atau password") ||
                        lower.contains("nama pengguna atau kata sandi") ||
                        lower.contains("kredensial") ||
                        lower.contains("sesi anda telah habis") ||
                        lower.contains("unauthorized") ||
                        lower.contains("login gagal") ||
                        lower.contains("nama pengguna tidak") ||
                        lower.contains("user tidak") ||
                        lower.contains("user not found") ||
                        lower.contains("sandi") ||
                        lower.contains("password")

                    val errorState = when {
                        lower.contains("akun tidak aktif") -> AuthState.Error(
                            message = "Akun tidak aktif. Hubungi admin toko.",
                            isUsernameHighlighted = true,
                            isPasswordHighlighted = true,
                            passwordError = "Akun tidak aktif. Hubungi admin toko."
                        )
                        lower.contains("kredensial default") -> AuthState.Error(
                            message = "Kredensial default tidak diizinkan pada production.",
                            isUsernameHighlighted = true,
                            isPasswordHighlighted = true,
                            passwordError = "Kredensial default tidak diizinkan pada production."
                        )
                        isNetwork -> AuthState.Error(message = rawMessage)
                        isCredentialFailure -> AuthState.Error(
                            message = "Nama pengguna atau kata sandi salah",
                            usernameError = null,
                            passwordError = "Nama pengguna atau kata sandi salah",
                            isUsernameHighlighted = true,
                            isPasswordHighlighted = true
                        )
                        else -> AuthState.Error(
                            message = rawMessage,
                            isUsernameHighlighted = true,
                            isPasswordHighlighted = true,
                            passwordError = rawMessage
                        )
                    }
                    _authState.value = errorState
                }

                is RepositoryResult.Exception -> {
                    _authState.value = AuthState.Error("Terjadi kesalahan tak terduga. Silakan coba lagi.")
                }
            }
        }
    }

    fun onLoginInputChanged(isUsername: Boolean) {
        val current = _authState.value as? AuthState.Error ?: return
        if (current.isUsernameHighlighted && current.isPasswordHighlighted && current.usernameError == null) {
            // Credential error: clear both highlights cleanly once user edits either input
            _authState.value = AuthState.Idle
            return
        }
        val nextUsernameError = if (isUsername) null else current.usernameError
        val nextPasswordError = if (!isUsername) null else current.passwordError
        val nextUsernameHighlight = if (isUsername) false else current.isUsernameHighlighted
        val nextPasswordHighlight = if (!isUsername) false else current.isPasswordHighlighted
        if (nextUsernameError == null && nextPasswordError == null && !nextUsernameHighlight && !nextPasswordHighlight) {
            _authState.value = AuthState.Idle
        } else {
            _authState.value = current.copy(
                usernameError = nextUsernameError,
                passwordError = nextPasswordError,
                isUsernameHighlighted = nextUsernameHighlight,
                isPasswordHighlighted = nextPasswordHighlight
            )
        }
    }

    fun unlock(pin: String) {
        val currentUnlock = _unlockState.value
        if (currentUnlock is UnlockState.LockedOut && currentUnlock.remainingSeconds > 0L) {
            return
        }
        if (!CredentialValidator.isValidPin(pin)) {
            _unlockState.value = UnlockState.Error(
                "PIN harus terdiri dari ${CredentialValidator.PIN_MIN_LENGTH} sampai ${CredentialValidator.PIN_MAX_LENGTH} digit angka"
            )
            return
        }

        lockoutCountdownJob?.cancel()
        _unlockState.value = UnlockState.Loading
        viewModelScope.launch {
            when (val result = authRepository.unlock(pin)) {
                is RepositoryResult.Success -> handleUnlockResult(result.data)

                is RepositoryResult.Error -> {
                    _unlockState.value = UnlockState.Error(result.message)
                }

                is RepositoryResult.Exception -> {
                    _unlockState.value = UnlockState.Error("Terjadi kesalahan tak terduga. Silakan coba lagi.")
                }
            }
        }
    }

    private fun handleUnlockResult(result: PinUnlockResult) {
        when (result) {
            PinUnlockResult.Unlocked -> {
                lockoutCountdownJob?.cancel()
                _unlockState.value = UnlockState.Success
            }

            is PinUnlockResult.ForceLogout -> {
                lockoutCountdownJob?.cancel()
                verifiedPasswordForPinReset = null
                _pendingForgotPinReset.value = false
                _authState.value = AuthState.Error(result.message)
                _unlockState.value = UnlockState.ForceLogout(result.message)
            }

            is PinUnlockResult.Failed -> {
                if (result.isLocked) {
                    val nowWall = clockMillis()
                    val initialRemainingMillis = result.remainingLockMillis?.takeIf { it > 0L }
                        ?: result.lockedUntilEpochMillis?.let { (it - nowWall).takeIf { diff -> diff > 0L } }
                        ?: DEFAULT_LOCKOUT_FALLBACK_MILLIS
                    val deadlineElapsedMillis = elapsedRealtimeMillis() + initialRemainingMillis
                    val initialRemainingSeconds = remainingSecondsFromMillis(initialRemainingMillis)
                    val displayLockedUntil = result.lockedUntilEpochMillis?.takeIf { it > nowWall } ?: 0L
                    startLockoutCountdown(
                        message = result.message,
                        displayLockedUntilEpochMillis = displayLockedUntil,
                        deadlineElapsedMillis = deadlineElapsedMillis,
                        initialRemainingSeconds = initialRemainingSeconds,
                    )
                    return
                }
                _unlockState.value = UnlockState.WrongPin(
                    message = result.message,
                    attemptsLeft = result.attemptsLeft
                )
            }
        }
    }

    private fun startLockoutCountdown(
        message: String,
        displayLockedUntilEpochMillis: Long,
        deadlineElapsedMillis: Long,
        initialRemainingSeconds: Long
    ) {
        lockoutCountdownJob?.cancel()
        _unlockState.value = UnlockState.LockedOut(
            message = message,
            lockedUntilEpochMillis = displayLockedUntilEpochMillis,
            remainingSeconds = initialRemainingSeconds
        )
        lockoutCountdownJob = viewModelScope.launch {
            var remaining = initialRemainingSeconds
            while (remaining > 0L) {
                delay(tickIntervalMillis)
                remaining = remainingSecondsUntilElapsed(deadlineElapsedMillis)
                if (remaining > 0L) {
                    _unlockState.value = UnlockState.LockedOut(
                        message = message,
                        lockedUntilEpochMillis = displayLockedUntilEpochMillis,
                        remainingSeconds = remaining
                    )
                }
            }
            _unlockState.value = UnlockState.Idle
        }
    }

    private fun remainingSecondsUntilElapsed(deadlineElapsedMillis: Long): Long {
        return remainingSecondsFromMillis(deadlineElapsedMillis - elapsedRealtimeMillis())
    }

    private fun remainingSecondsFromMillis(diffMillis: Long): Long {
        if (diffMillis <= 0L) return 0L
        return (diffMillis + 999L) / 1000L
    }

    fun startForgotPinFlow() {
        lockoutCountdownJob?.cancel()
        _unlockState.value = UnlockState.Idle
        _resetPinState.value = ResetPinState.Idle
        _authState.value = AuthState.Idle
        verifiedPasswordForPinReset = null
        _pendingForgotPinReset.value = true
    }

    fun cancelForgotPinFlow() {
        verifiedPasswordForPinReset = null
        _pendingForgotPinReset.value = false
        _resetPinState.value = ResetPinState.Idle
    }

    fun resetPinAfterLogin(
        newPin: String,
        confirmPin: String,
        currentPasswordInput: String? = null
    ) {
        val resolvedPassword = currentPasswordInput?.takeIf(String::isNotBlank)
            ?: verifiedPasswordForPinReset
        if (resolvedPassword.isNullOrBlank()) {
            _resetPinState.value = ResetPinState.Error("Kata sandi saat ini wajib diisi")
            return
        }
        if (!CredentialValidator.isValidPin(newPin)) {
            _resetPinState.value = ResetPinState.Error(
                "PIN baru harus terdiri dari ${CredentialValidator.PIN_MIN_LENGTH} sampai ${CredentialValidator.PIN_MAX_LENGTH} digit angka"
            )
            return
        }
        if (newPin != confirmPin) {
            _resetPinState.value = ResetPinState.Error("Konfirmasi PIN tidak cocok")
            return
        }

        _resetPinState.value = ResetPinState.Loading
        viewModelScope.launch {
            when (val result = authRepository.resetPinWithPassword(resolvedPassword, newPin)) {
                is RepositoryResult.Success -> {
                    verifiedPasswordForPinReset = null
                    _pendingForgotPinReset.value = false
                    _resetPinState.value = ResetPinState.Success
                }
                is RepositoryResult.Error -> {
                    _resetPinState.value = ResetPinState.Error(result.message)
                }
                is RepositoryResult.Exception -> {
                    _resetPinState.value = ResetPinState.Error("Terjadi kesalahan tak terduga. Silakan coba lagi.")
                }
            }
        }
    }

    fun resetResetPinState() {
        _resetPinState.value = ResetPinState.Idle
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun resetUnlockState() {
        if (_unlockState.value is UnlockState.LockedOut) return
        _unlockState.value = UnlockState.Idle
    }

    fun forceClearUnlockState() {
        lockoutCountdownJob?.cancel()
        _unlockState.value = UnlockState.Idle
    }

    fun clearLoginNotice() {
        _loginNotice.value = null
    }

    fun notifyPasswordChanged() {
        lockoutCountdownJob?.cancel()
        verifiedPasswordForPinReset = null
        _pendingForgotPinReset.value = false
        _unlockState.value = UnlockState.Idle
        _authState.value = AuthState.Idle
        _loginNotice.value = PASSWORD_CHANGED_LOGIN_MESSAGE
    }

    fun notifySessionExpired(reason: String? = null) {
        lockoutCountdownJob?.cancel()
        verifiedPasswordForPinReset = null
        _pendingForgotPinReset.value = false
        _loginNotice.value = null
        _unlockState.value = UnlockState.Idle
        _authState.value = AuthState.Error(
            reason?.takeIf(String::isNotBlank) ?: "Sesi telah berakhir. Silakan login kembali."
        )
    }

    override fun onCleared() {
        lockoutCountdownJob?.cancel()
        verifiedPasswordForPinReset = null
        super.onCleared()
    }

    companion object {
        const val PASSWORD_CHANGED_LOGIN_MESSAGE = "Password diubah, silakan login kembali"
        private const val DEFAULT_LOCKOUT_FALLBACK_MILLIS = 30_000L

        fun factory(authRepository: AuthRepository): ViewModelProvider.Factory {
            return viewModelFactory {
                AuthViewModel(
                    authRepository = authRepository,
                    elapsedRealtimeMillis = { defaultElapsedRealtimeMillis() },
                )
            }
        }
    }
}
