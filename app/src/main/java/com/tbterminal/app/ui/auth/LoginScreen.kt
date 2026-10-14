package com.tbterminal.app.ui.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.hardware.fingerprint.FingerprintManagerCompat

private const val PREFS_NAME = "login_prefs"
private const val KEY_REMEMBER_USERNAME = "remember_username"
private const val KEY_SAVED_USERNAME = "saved_username"
private const val KEY_HAS_LOGGED_IN_BEFORE = "has_logged_in_before"

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: (role: String, name: String) -> Unit,
    onBiometricAuthRequest: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()

    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val initialRemember = remember { prefs.getBoolean(KEY_REMEMBER_USERNAME, false) }
    val initialUsername = remember { if (initialRemember) prefs.getString(KEY_SAVED_USERNAME, "") ?: "" else "" }
    val hasLoggedInBefore = remember { prefs.getBoolean(KEY_HAS_LOGGED_IN_BEFORE, false) }

    var username by remember { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf("") }
    var rememberUsername by remember { mutableStateOf(initialRemember) }

    val isBiometricAvailable = remember {
        hasLoggedInBefore && checkDeviceBiometricSupport(context)
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            val state = authState as AuthState.Success
            prefs.edit().apply {
                putBoolean(KEY_HAS_LOGGED_IN_BEFORE, true)
                putBoolean(KEY_REMEMBER_USERNAME, rememberUsername)
                if (rememberUsername) {
                    putString(KEY_SAVED_USERNAME, username.trim())
                } else {
                    remove(KEY_SAVED_USERNAME)
                }
                apply()
            }
            onLoginSuccess(state.role, state.name)
            viewModel.resetState()
        }
    }

    LoginContent(
        username = username,
        password = password,
        authState = authState,
        onUsernameChange = {
            username = it
            if (authState is AuthState.Error) viewModel.resetState()
        },
        onPasswordChange = {
            password = it
            if (authState is AuthState.Error) viewModel.resetState()
        },
        onSubmit = {
            viewModel.login(username.trim(), password)
        },
        rememberUsername = rememberUsername,
        isBiometricSupported = isBiometricAvailable,
        onRememberUsernameChange = { rememberUsername = it },
        onBiometricClick = {
            if (onBiometricAuthRequest != null) {
                onBiometricAuthRequest()
            } else {
                viewModel.login(username.trim(), password)
            }
        }
    )
}

@Composable
internal fun LoginContent(
    username: String,
    password: String,
    authState: AuthState,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    rememberUsername: Boolean = false,
    isBiometricSupported: Boolean = false,
    onRememberUsernameChange: (Boolean) -> Unit = {},
    onBiometricClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    val passwordFocusRequester = remember { FocusRequester() }

    var usernameError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val isLoading = authState is AuthState.Loading
    val canSubmit = username.isNotBlank() && password.isNotBlank()

    fun handleUsernameChange(newValue: String) {
        usernameError = null
        onUsernameChange(newValue)
    }

    fun handlePasswordChange(newValue: String) {
        passwordError = null
        onPasswordChange(newValue)
    }

    fun executeSubmit() {
        var hasValidationError = false
        if (username.isBlank()) {
            usernameError = "Nama pengguna wajib diisi"
            hasValidationError = true
        }
        if (password.isBlank()) {
            passwordError = "Kata sandi wajib diisi"
            hasValidationError = true
        }
        if (!hasValidationError && !isLoading) {
            onSubmit()
        }
    }

    val isNetworkError = remember(authState) {
        if (authState is AuthState.Error) {
            val lower = authState.message.lowercase()
            lower.contains("koneksi") || lower.contains("internet") || lower.contains("network") ||
                lower.contains("timeout") || lower.contains("jaringan") || lower.contains("server tidak dapat dihubungi")
        } else false
    }

    val isLockedError = remember(authState) {
        if (authState is AuthState.Error) {
            val lower = authState.message.lowercase()
            lower.contains("terkunci") || lower.contains("locked") || lower.contains("terblokir")
        } else false
    }

    val isServerError = remember(authState) {
        if (authState is AuthState.Error) {
            val lower = authState.message.lowercase()
            lower.contains("gangguan") || lower.contains("server sedang") ||
                lower.contains("500") || lower.contains("502") || lower.contains("503")
        } else false
    }

    val isSessionExpired = remember(authState) {
        if (authState is AuthState.Error) {
            authState.message.contains("Sesi telah berakhir", ignoreCase = true)
        } else false
    }

    val isBannerError = remember(isNetworkError, isLockedError, isServerError, isSessionExpired) {
        isNetworkError || isLockedError || isServerError || isSessionExpired
    }

    val serverBannerErrorMessage = remember(authState, isBannerError) {
        when {
            authState is AuthState.Error && isBannerError -> sanitizeSecurityErrorMessage(authState.message)
            else -> null
        }
    }

    val authErrorMessage = (authState as? AuthState.Error)?.message

    val isUsernameSpecificError = remember(authErrorMessage) {
        if (authErrorMessage != null) {
            val lower = authErrorMessage.lowercase()
            (lower.contains("nama pengguna") || lower.contains("username")) &&
                !lower.contains("sandi") && !lower.contains("password")
        } else false
    }

    val isPasswordSpecificError = remember(authErrorMessage) {
        if (authErrorMessage != null) {
            val lower = authErrorMessage.lowercase()
            (lower.contains("sandi") || lower.contains("password")) &&
                !lower.contains("nama pengguna") && !lower.contains("username")
        } else false
    }

    val isGeneralCredentialError = remember(authErrorMessage, isBannerError, isUsernameSpecificError, isPasswordSpecificError) {
        authErrorMessage != null && !isBannerError && !isUsernameSpecificError && !isPasswordSpecificError
    }

    val effectiveUsernameError = usernameError ?: if (isUsernameSpecificError) authErrorMessage else null
    val isUsernameHighlightError = usernameError != null || isUsernameSpecificError || isGeneralCredentialError

    val effectivePasswordError = passwordError ?: when {
        isPasswordSpecificError -> authErrorMessage
        isGeneralCredentialError -> authErrorMessage ?: "Nama pengguna atau kata sandi salah"
        else -> null
    }
    val isPasswordHighlightError = passwordError != null || isPasswordSpecificError || isGeneralCredentialError

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .systemBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header Merek Barokah Jaya (Ikon Toko #EDF7F2 + Judul #0F172A + Subjudul #64748B)
                LoginBrandHeader()

                // Field 1: Nama Pengguna (Notched badge "Nama pengguna" #64748B)
                AuthInputField(
                    label = "Nama pengguna",
                    value = username,
                    onValueChange = ::handleUsernameChange,
                    placeholder = "Contoh: admin.toko",
                    leadingIcon = Icons.Outlined.Person,
                    errorMessage = effectiveUsernameError,
                    isError = isUsernameHighlightError,
                    enabled = !isLoading,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { passwordFocusRequester.requestFocus() }
                    ),
                    testTag = "login-username-input"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Field 2: Kata Sandi (Notched badge "Kata sandi" #64748B)
                AuthInputField(
                    label = "Kata sandi",
                    value = password,
                    onValueChange = ::handlePasswordChange,
                    placeholder = "••••••••",
                    leadingIcon = Icons.Outlined.Lock,
                    isPassword = true,
                    passwordVisible = passwordVisible,
                    onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                    errorMessage = effectivePasswordError,
                    isError = isPasswordHighlightError,
                    enabled = !isLoading,
                    focusRequester = passwordFocusRequester,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { executeSubmit() }
                    ),
                    testTag = "login-password-input"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Baris Opsi: Checkbox Ingat nama pengguna & Tautan Lupa kata sandi
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            enabled = !isLoading,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onRememberUsernameChange(!rememberUsername)
                        }
                    ) {
                        Checkbox(
                            checked = rememberUsername,
                            onCheckedChange = onRememberUsernameChange,
                            enabled = !isLoading,
                            colors = CheckboxDefaults.colors(
                                checkedColor = LoginThemeTokens.BrandGreen,
                                checkmarkColor = Color.White,
                                uncheckedColor = LoginThemeTokens.Slate300
                            ),
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("login-remember-checkbox")
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ingat nama pengguna",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = LoginThemeTokens.Slate600
                        )
                    }

                    Text(
                        text = "Lupa kata sandi?",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LoginThemeTokens.BrandGreen,
                        modifier = Modifier
                            .clickable(enabled = !isLoading) {
                                showForgotPasswordDialog = true
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                            .testTag("login-forgot-password-link")
                    )
                }

                // Banner Error Server jika ada kesalahan non-kredensial (misal jaringan/akun terkunci)
                if (!serverBannerErrorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LoginErrorBanner(message = serverBannerErrorMessage)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Tombol Utama "Masuk" (Selalu aktif dengan warna #256B57 dan state ditekan #1C5343)
                LoginPrimaryButton(
                    text = "Masuk",
                    onClick = ::executeSubmit,
                    enabled = !isLoading,
                    isLoading = isLoading,
                    testTag = "login-submit-button"
                )

                // Tombol Sekunder Biometrik (Hanya muncul jika didukung & pernah login)
                if (isBiometricSupported) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LoginBiometricButton(
                        onClick = onBiometricClick,
                        enabled = !isLoading,
                        testTag = "login-biometric-button"
                    )
                }
            }

            // Footer Aplikasi: Terminal Barokah Jaya · v1.0
            LoginFooter(
                modifier = Modifier.padding(top = 32.dp, bottom = 8.dp)
            )
        }
    }

    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            onDismiss = { showForgotPasswordDialog = false },
            onContactAdmin = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("https://wa.me/?text=Halo%20Admin%20Terminal%20Barokah%20Jaya,%20saya%20membutuhkan%20bantuan%20reset%20kata%20sandi%20kasir.")
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        )
    }
}

private fun sanitizeSecurityErrorMessage(rawMessage: String): String {
    val lower = rawMessage.lowercase()
    return when {
        lower.contains("koneksi") || lower.contains("internet") || lower.contains("network") || lower.contains("timeout") -> {
            "Tidak ada koneksi internet. Periksa koneksi Anda."
        }
        lower.contains("user") || lower.contains("password") || lower.contains("sandi") || lower.contains("salah") || lower.contains("401") || lower.contains("invalid") -> {
            "Nama pengguna atau kata sandi salah. Coba lagi."
        }
        lower.contains("terkunci") || lower.contains("locked") || lower.contains("terblokir") -> {
            "Akun terkunci karena terlalu banyak percobaan gagal. Hubungi admin toko."
        }
        else -> rawMessage
    }
}

@Suppress("DEPRECATION")
private fun checkDeviceBiometricSupport(context: Context): Boolean {
    return try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val biometricManager = context.getSystemService(android.hardware.biometrics.BiometricManager::class.java)
            biometricManager?.canAuthenticate(android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK) == android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS
        } else {
            val fingerprintManager = FingerprintManagerCompat.from(context)
            fingerprintManager.isHardwareDetected && fingerprintManager.hasEnrolledFingerprints()
        }
    } catch (_: Throwable) {
        false
    }
}
