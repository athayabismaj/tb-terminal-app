package com.tbterminal.app.ui.dashboard.owner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tbterminal.app.data.model.UserRole
import com.tbterminal.app.ui.auth.AlertType
import com.tbterminal.app.ui.auth.AppAlert
import com.tbterminal.app.ui.auth.RingsPattern
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.BrandSoftText
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.SurfaceWhite
import com.tbterminal.app.ui.theme.TbterminalappTheme
import com.tbterminal.app.ui.users.AddUserUiState
import com.tbterminal.app.ui.users.EditUserUiState

private val OWNER_TAMBAH_USER_TABLET_BREAKPOINT = 840.dp
private val EMAIL_PATTERN = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

internal enum class TambahUserServerTarget {
    NONE,
    FULL_NAME,
    USERNAME,
    EMAIL,
    PASSWORD,
    PIN,
    NETWORK_TOAST,
    GENERIC_TOAST
}

internal fun classifyUserSaveError(message: String?): TambahUserServerTarget {
    if (message.isNullOrBlank()) return TambahUserServerTarget.NONE
    val lower = message.lowercase()
    val isNetwork = listOf("koneksi", "jaringan", "terhubung", "timeout", "network", "offline")
        .any { it in lower }
    if (isNetwork) return TambahUserServerTarget.NETWORK_TOAST
    if ("username" in lower) return TambahUserServerTarget.USERNAME
    if ("email" in lower) return TambahUserServerTarget.EMAIL
    if ("password" in lower || "kata sandi" in lower) return TambahUserServerTarget.PASSWORD
    if ("pin" in lower) return TambahUserServerTarget.PIN
    val isName = listOf("nama", "name", "duplikat", "sudah ada").any { it in lower }
    if (isName) return TambahUserServerTarget.FULL_NAME
    return TambahUserServerTarget.GENERIC_TOAST
}

internal fun validateOwnerUserFullName(raw: String): String? {
    val trimmed = raw.trim()
    return when {
        trimmed.isEmpty() -> "Nama lengkap wajib diisi"
        trimmed.length < 2 -> "Minimal 2 karakter"
        else -> null
    }
}

internal fun validateOwnerUsername(raw: String): String? {
    val trimmed = raw.trim()
    return when {
        trimmed.isEmpty() -> "Username wajib diisi"
        trimmed.any(Char::isWhitespace) -> "Username tidak boleh mengandung spasi"
        trimmed.length < 3 -> "Minimal 3 karakter"
        else -> null
    }
}

internal fun validateOwnerUserEmail(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    return if (!EMAIL_PATTERN.matches(trimmed)) "Format email tidak valid" else null
}

internal fun validateOwnerUserPassword(raw: String): String? {
    return when {
        raw.isEmpty() -> "Password wajib diisi"
        raw.length < 6 -> "Password minimal 6 karakter"
        else -> null
    }
}

internal fun validateOwnerUserPin(raw: String): String? {
    return when {
        raw.isEmpty() -> "PIN akses wajib diisi"
        raw.length != 6 || !raw.all(Char::isDigit) -> "PIN harus 6 digit angka"
        else -> null
    }
}

private fun List<UserRole>.toRoleRadioOptions(): List<RadioCardOption<String>> {
    return map { role ->
        val normalized = role.name.trim().uppercase()
        val (title, subtitle) = when (normalized) {
            "ADMIN" -> "Admin" to "Kelola stok, pembelian & kas"
            "KASIR" -> "Kasir" to "Transaksi kasir & cek stok"
            else -> role.name.lowercase().replaceFirstChar(Char::uppercase) to "Hak akses ${role.name.lowercase()}"
        }
        RadioCardOption(
            value = role.id,
            title = title,
            subtitle = subtitle,
            testTag = "user-role-${role.name.lowercase()}"
        )
    }
}

@Composable
internal fun OwnerTambahUserRoute(
    uiState: AddUserUiState,
    onFullNameChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onPinChanged: (String) -> Unit,
    onRoleSelected: (String) -> Unit,
    onSave: () -> Unit,
    onRetryRoles: () -> Unit,
    onBack: () -> Unit,
    onDashboardClick: () -> Unit
) {
    val destinationNavigator = LocalAdminDestinationNavigator.current
    val initialRoleId by rememberSaveable { mutableStateOf(uiState.selectedRoleId) }

    var nameErrorLocal by remember { mutableStateOf<String?>(null) }
    var usernameErrorLocal by remember { mutableStateOf<String?>(null) }
    var emailErrorLocal by remember { mutableStateOf<String?>(null) }
    var passwordErrorLocal by remember { mutableStateOf<String?>(null) }
    var pinErrorLocal by remember { mutableStateOf<String?>(null) }
    var nameBlurValidated by remember { mutableStateOf(false) }
    var usernameBlurValidated by remember { mutableStateOf(false) }
    var emailBlurValidated by remember { mutableStateOf(false) }
    var passwordBlurValidated by remember { mutableStateOf(false) }
    var pinBlurValidated by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var pendingNavAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var dismissedToast by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        dismissedToast = false
    }

    val hasChanges = uiState.fullName.isNotEmpty() ||
        uiState.username.isNotEmpty() ||
        uiState.email.isNotEmpty() ||
        uiState.password.isNotEmpty() ||
        uiState.pin.isNotEmpty() ||
        (initialRoleId != null && uiState.selectedRoleId != initialRoleId)

    fun requestLeave(target: () -> Unit) {
        if (uiState.isSubmitting) return
        if (hasChanges) {
            pendingNavAction = target
            showDiscardDialog = true
        } else {
            target()
        }
    }

    BackHandler(enabled = !uiState.isSubmitting) {
        requestLeave(onBack)
    }

    fun handleNavSelect(destination: DashboardNavDestination) {
        val target: () -> Unit = {
            when (destination) {
                DashboardNavDestination.BERANDA ->
                    destinationNavigator?.invoke(AdminDestination.Dashboard) ?: onDashboardClick()
                DashboardNavDestination.TRANSAKSI ->
                    destinationNavigator?.invoke(AdminDestination.TransactionsHub)
                DashboardNavDestination.KEUANGAN ->
                    destinationNavigator?.invoke(AdminDestination.FinanceHub)
                DashboardNavDestination.STOK ->
                    destinationNavigator?.invoke(AdminDestination.StockHub)
                DashboardNavDestination.MENU -> onBack()
            }
        }
        requestLeave(target)
    }

    fun submitForm() {
        if (uiState.isSubmitting) return
        val nameErr = validateOwnerUserFullName(uiState.fullName)
        val userErr = validateOwnerUsername(uiState.username)
        val mailErr = validateOwnerUserEmail(uiState.email)
        val passErr = validateOwnerUserPassword(uiState.password)
        val pinErr = validateOwnerUserPin(uiState.pin)
        nameErrorLocal = nameErr
        usernameErrorLocal = userErr
        emailErrorLocal = mailErr
        passwordErrorLocal = passErr
        pinErrorLocal = pinErr
        if (nameErr != null || userErr != null || mailErr != null || passErr != null || pinErr != null) {
            return
        }
        onSave()
    }

    val isInitialRoleError = uiState.roles.isEmpty() && !uiState.isLoadingRoles && uiState.errorMessage != null
    val serverTarget = if (uiState.roles.isNotEmpty()) classifyUserSaveError(uiState.errorMessage) else TambahUserServerTarget.NONE

    Box(modifier = Modifier.fillMaxSize()) {
        OwnerUserFormScreen(
            isEditMode = false,
            isLoadingInitial = uiState.isLoadingRoles && uiState.roles.isEmpty(),
            initialLoadError = if (isInitialRoleError) uiState.errorMessage else null,
            roles = uiState.roles,
            selectedRoleId = uiState.selectedRoleId,
            fullName = uiState.fullName,
            username = uiState.username,
            email = uiState.email,
            password = uiState.password,
            pin = uiState.pin,
            fullNameError = nameErrorLocal ?: if (serverTarget == TambahUserServerTarget.FULL_NAME) uiState.errorMessage else null,
            usernameError = usernameErrorLocal ?: if (serverTarget == TambahUserServerTarget.USERNAME) uiState.errorMessage else null,
            emailError = emailErrorLocal ?: if (serverTarget == TambahUserServerTarget.EMAIL) uiState.errorMessage else null,
            passwordError = passwordErrorLocal ?: if (serverTarget == TambahUserServerTarget.PASSWORD) uiState.errorMessage else null,
            pinError = pinErrorLocal ?: if (serverTarget == TambahUserServerTarget.PIN) uiState.errorMessage else null,
            isSaving = uiState.isSubmitting,
            hasChanges = hasChanges,
            onRoleSelected = onRoleSelected,
            onFullNameChange = { value ->
                val clipped = value.take(120)
                onFullNameChanged(clipped)
                if (nameBlurValidated || nameErrorLocal != null) {
                    nameErrorLocal = validateOwnerUserFullName(clipped)
                }
            },
            onFullNameBlur = {
                nameBlurValidated = true
                if (uiState.fullName.isNotEmpty() || nameErrorLocal != null) {
                    nameErrorLocal = validateOwnerUserFullName(uiState.fullName)
                }
            },
            onUsernameChange = { value ->
                val clipped = value.take(60)
                onUsernameChanged(clipped)
                if (usernameBlurValidated || usernameErrorLocal != null) {
                    usernameErrorLocal = validateOwnerUsername(clipped)
                }
            },
            onUsernameBlur = {
                usernameBlurValidated = true
                if (uiState.username.isNotEmpty() || usernameErrorLocal != null) {
                    usernameErrorLocal = validateOwnerUsername(uiState.username)
                }
            },
            onEmailChange = { value ->
                val clipped = value.take(120)
                onEmailChanged(clipped)
                if (emailBlurValidated || emailErrorLocal != null) {
                    emailErrorLocal = validateOwnerUserEmail(clipped)
                }
            },
            onEmailBlur = {
                emailBlurValidated = true
                emailErrorLocal = validateOwnerUserEmail(uiState.email)
            },
            onPasswordChange = { value ->
                val clipped = value.take(100)
                onPasswordChanged(clipped)
                if (passwordBlurValidated || passwordErrorLocal != null) {
                    passwordErrorLocal = validateOwnerUserPassword(clipped)
                }
            },
            onPasswordBlur = {
                passwordBlurValidated = true
                if (uiState.password.isNotEmpty() || passwordErrorLocal != null) {
                    passwordErrorLocal = validateOwnerUserPassword(uiState.password)
                }
            },
            onPinChange = { value ->
                val digits = value.filter(Char::isDigit).take(6)
                onPinChanged(digits)
                if (pinBlurValidated || pinErrorLocal != null) {
                    pinErrorLocal = validateOwnerUserPin(digits)
                }
            },
            onPinBlur = {
                pinBlurValidated = true
                if (uiState.pin.isNotEmpty() || pinErrorLocal != null) {
                    pinErrorLocal = validateOwnerUserPin(uiState.pin)
                }
            },
            onRetryInitialLoad = onRetryRoles,
            onBack = { requestLeave(onBack) },
            onSubmit = ::submitForm,
            onNavSelect = ::handleNavSelect
        )

        val showNetworkToast = serverTarget == TambahUserServerTarget.NETWORK_TOAST && !dismissedToast
        val showGenericToast = serverTarget == TambahUserServerTarget.GENERIC_TOAST && !dismissedToast
        if (showNetworkToast || showGenericToast) {
            AppAlert(
                type = AlertType.Error,
                title = if (showNetworkToast) "Tidak dapat terhubung" else "Gagal menyimpan pengguna",
                message = uiState.errorMessage.orEmpty(),
                actionLabel = if (showNetworkToast) "Coba lagi" else null,
                onAction = { submitForm() },
                visible = true,
                onDismiss = { dismissedToast = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .zIndex(99f)
                    .testTag("tambah-user-error-toast")
            )
        }
    }

    if (showDiscardDialog) {
        DiscardCustomerDialog(
            onContinueEditing = {
                showDiscardDialog = false
                pendingNavAction = null
            },
            onDiscard = {
                showDiscardDialog = false
                val target = pendingNavAction ?: onBack
                pendingNavAction = null
                target()
            },
            title = "Buang data pengguna?",
            message = "Data yang sudah diisi akan hilang.",
            dialogTestTag = "discard-user-dialog",
            continueTestTag = "discard-user-continue",
            discardTestTag = "discard-user-confirm"
        )
    }
}

@Composable
internal fun OwnerEditUserRoute(
    uiState: EditUserUiState,
    onFullNameChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onRoleSelected: (String) -> Unit,
    onSave: () -> Unit,
    onRetryLoad: () -> Unit,
    onBack: () -> Unit,
    onDashboardClick: () -> Unit
) {
    val destinationNavigator = LocalAdminDestinationNavigator.current
    val preloaded = !uiState.isLoadingUser &&
        !uiState.isLoadingRoles &&
        uiState.errorMessage == null &&
        uiState.fullName.isNotEmpty()

    var hasLoadedInitial by rememberSaveable { mutableStateOf(preloaded) }
    var initialRoleId by rememberSaveable { mutableStateOf(if (preloaded) uiState.selectedRoleId else null) }
    var initialFullName by rememberSaveable { mutableStateOf(if (preloaded) uiState.fullName else "") }
    var initialUsername by rememberSaveable { mutableStateOf(if (preloaded) uiState.username else "") }
    var initialEmail by rememberSaveable { mutableStateOf(if (preloaded) uiState.email else "") }

    LaunchedEffect(uiState.isLoadingUser, uiState.isLoadingRoles, uiState.fullName, uiState.selectedRoleId) {
        if (!hasLoadedInitial && !uiState.isLoadingUser && !uiState.isLoadingRoles && uiState.fullName.isNotEmpty()) {
            initialRoleId = uiState.selectedRoleId
            initialFullName = uiState.fullName
            initialUsername = uiState.username
            initialEmail = uiState.email
            hasLoadedInitial = true
        }
    }

    var nameErrorLocal by remember { mutableStateOf<String?>(null) }
    var usernameErrorLocal by remember { mutableStateOf<String?>(null) }
    var emailErrorLocal by remember { mutableStateOf<String?>(null) }
    var nameBlurValidated by remember { mutableStateOf(false) }
    var usernameBlurValidated by remember { mutableStateOf(false) }
    var emailBlurValidated by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var pendingNavAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var dismissedToast by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.errorMessage) {
        dismissedToast = false
    }

    val hasChanges = hasLoadedInitial && (
        uiState.selectedRoleId != initialRoleId ||
            uiState.fullName != initialFullName ||
            uiState.username != initialUsername ||
            uiState.email != initialEmail
        )

    fun requestLeave(target: () -> Unit) {
        if (uiState.isSubmitting) return
        if (hasChanges) {
            pendingNavAction = target
            showDiscardDialog = true
        } else {
            target()
        }
    }

    BackHandler(enabled = !uiState.isSubmitting) {
        requestLeave(onBack)
    }

    fun handleNavSelect(destination: DashboardNavDestination) {
        val target: () -> Unit = {
            when (destination) {
                DashboardNavDestination.BERANDA ->
                    destinationNavigator?.invoke(AdminDestination.Dashboard) ?: onDashboardClick()
                DashboardNavDestination.TRANSAKSI ->
                    destinationNavigator?.invoke(AdminDestination.TransactionsHub)
                DashboardNavDestination.KEUANGAN ->
                    destinationNavigator?.invoke(AdminDestination.FinanceHub)
                DashboardNavDestination.STOK ->
                    destinationNavigator?.invoke(AdminDestination.StockHub)
                DashboardNavDestination.MENU -> onBack()
            }
        }
        requestLeave(target)
    }

    fun submitForm() {
        if (uiState.isSubmitting) return
        val nameErr = validateOwnerUserFullName(uiState.fullName)
        val userErr = validateOwnerUsername(uiState.username)
        val mailErr = validateOwnerUserEmail(uiState.email)
        nameErrorLocal = nameErr
        usernameErrorLocal = userErr
        emailErrorLocal = mailErr
        if (nameErr != null || userErr != null || mailErr != null) {
            return
        }
        onSave()
    }

    val isLoadingInitial = uiState.isLoadingUser || uiState.isLoadingRoles
    val initialLoadError = if (!hasLoadedInitial && !isLoadingInitial && uiState.errorMessage != null) {
        uiState.errorMessage
    } else {
        null
    }
    val serverTarget = if (hasLoadedInitial) classifyUserSaveError(uiState.errorMessage) else TambahUserServerTarget.NONE

    Box(modifier = Modifier.fillMaxSize()) {
        OwnerUserFormScreen(
            isEditMode = true,
            isLoadingInitial = isLoadingInitial && !hasLoadedInitial,
            initialLoadError = initialLoadError,
            roles = uiState.roles,
            selectedRoleId = uiState.selectedRoleId,
            fullName = uiState.fullName,
            username = uiState.username,
            email = uiState.email,
            password = "",
            pin = "",
            fullNameError = nameErrorLocal ?: if (serverTarget == TambahUserServerTarget.FULL_NAME) uiState.errorMessage else null,
            usernameError = usernameErrorLocal ?: if (serverTarget == TambahUserServerTarget.USERNAME) uiState.errorMessage else null,
            emailError = emailErrorLocal ?: if (serverTarget == TambahUserServerTarget.EMAIL) uiState.errorMessage else null,
            passwordError = null,
            pinError = null,
            isSaving = uiState.isSubmitting,
            hasChanges = hasChanges,
            onRoleSelected = onRoleSelected,
            onFullNameChange = { value ->
                val clipped = value.take(120)
                onFullNameChanged(clipped)
                if (nameBlurValidated || nameErrorLocal != null) {
                    nameErrorLocal = validateOwnerUserFullName(clipped)
                }
            },
            onFullNameBlur = {
                nameBlurValidated = true
                if (uiState.fullName.isNotEmpty() || nameErrorLocal != null) {
                    nameErrorLocal = validateOwnerUserFullName(uiState.fullName)
                }
            },
            onUsernameChange = { value ->
                val clipped = value.take(60)
                onUsernameChanged(clipped)
                if (usernameBlurValidated || usernameErrorLocal != null) {
                    usernameErrorLocal = validateOwnerUsername(clipped)
                }
            },
            onUsernameBlur = {
                usernameBlurValidated = true
                if (uiState.username.isNotEmpty() || usernameErrorLocal != null) {
                    usernameErrorLocal = validateOwnerUsername(uiState.username)
                }
            },
            onEmailChange = { value ->
                val clipped = value.take(120)
                onEmailChanged(clipped)
                if (emailBlurValidated || emailErrorLocal != null) {
                    emailErrorLocal = validateOwnerUserEmail(clipped)
                }
            },
            onEmailBlur = {
                emailBlurValidated = true
                emailErrorLocal = validateOwnerUserEmail(uiState.email)
            },
            onPasswordChange = {},
            onPinChange = {},
            onRetryInitialLoad = onRetryLoad,
            onBack = { requestLeave(onBack) },
            onSubmit = ::submitForm,
            onNavSelect = ::handleNavSelect
        )

        val showNetworkToast = serverTarget == TambahUserServerTarget.NETWORK_TOAST && !dismissedToast
        val showGenericToast = serverTarget == TambahUserServerTarget.GENERIC_TOAST && !dismissedToast
        if (showNetworkToast || showGenericToast) {
            AppAlert(
                type = AlertType.Error,
                title = if (showNetworkToast) "Tidak dapat terhubung" else "Gagal menyimpan perubahan",
                message = uiState.errorMessage.orEmpty(),
                actionLabel = if (showNetworkToast) "Coba lagi" else null,
                onAction = { submitForm() },
                visible = true,
                onDismiss = { dismissedToast = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .zIndex(99f)
                    .testTag("tambah-user-error-toast")
            )
        }
    }

    if (showDiscardDialog) {
        DiscardCustomerDialog(
            onContinueEditing = {
                showDiscardDialog = false
                pendingNavAction = null
            },
            onDiscard = {
                showDiscardDialog = false
                val target = pendingNavAction ?: onBack
                pendingNavAction = null
                target()
            },
            title = "Buang perubahan?",
            message = "Perubahan yang belum disimpan akan hilang.",
            dialogTestTag = "discard-user-dialog",
            continueTestTag = "discard-user-continue",
            discardTestTag = "discard-user-confirm"
        )
    }
}

@Composable
internal fun OwnerUserFormScreen(
    isEditMode: Boolean,
    isLoadingInitial: Boolean = false,
    initialLoadError: String? = null,
    roles: List<UserRole>,
    selectedRoleId: String?,
    fullName: String,
    username: String,
    email: String,
    password: String,
    pin: String,
    fullNameError: String? = null,
    usernameError: String? = null,
    emailError: String? = null,
    passwordError: String? = null,
    pinError: String? = null,
    isSaving: Boolean = false,
    hasChanges: Boolean = true,
    onRoleSelected: (String) -> Unit,
    onFullNameChange: (String) -> Unit,
    onFullNameBlur: () -> Unit = {},
    onUsernameChange: (String) -> Unit,
    onUsernameBlur: () -> Unit = {},
    onEmailChange: (String) -> Unit,
    onEmailBlur: () -> Unit = {},
    onPasswordChange: (String) -> Unit,
    onPasswordBlur: () -> Unit = {},
    onPinChange: (String) -> Unit,
    onPinBlur: () -> Unit = {},
    onRetryInitialLoad: () -> Unit = {},
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val pageTitle = if (isEditMode) "Edit pengguna" else "Tambah pengguna"
    val buttonLabel = if (isEditMode) "Simpan perubahan" else "Simpan pengguna"
    val isSaveEnabled = !isLoadingInitial &&
        initialLoadError == null &&
        fullName.trim().length >= 2 &&
        username.trim().length >= 3 &&
        selectedRoleId != null &&
        (isEditMode || (password.length >= 6 && pin.length == 6)) &&
        !isSaving &&
        (!isEditMode || hasChanges)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        val isTablet = maxWidth >= OWNER_TAMBAH_USER_TABLET_BREAKPOINT
        if (isTablet) {
            TabletOwnerUserFormLayout(
                isEditMode = isEditMode,
                pageTitle = pageTitle,
                buttonLabel = buttonLabel,
                isLoadingInitial = isLoadingInitial,
                initialLoadError = initialLoadError,
                roles = roles,
                selectedRoleId = selectedRoleId,
                fullName = fullName,
                username = username,
                email = email,
                password = password,
                pin = pin,
                fullNameError = fullNameError,
                usernameError = usernameError,
                emailError = emailError,
                passwordError = passwordError,
                pinError = pinError,
                isSaveEnabled = isSaveEnabled,
                isSaving = isSaving,
                onRoleSelected = onRoleSelected,
                onFullNameChange = onFullNameChange,
                onFullNameBlur = onFullNameBlur,
                onUsernameChange = onUsernameChange,
                onUsernameBlur = onUsernameBlur,
                onEmailChange = onEmailChange,
                onEmailBlur = onEmailBlur,
                onPasswordChange = onPasswordChange,
                onPasswordBlur = onPasswordBlur,
                onPinChange = onPinChange,
                onPinBlur = onPinBlur,
                onRetryInitialLoad = onRetryInitialLoad,
                onBack = onBack,
                onSubmit = onSubmit,
                onNavSelect = onNavSelect
            )
        } else {
            PhoneOwnerUserFormLayout(
                isEditMode = isEditMode,
                pageTitle = pageTitle,
                buttonLabel = buttonLabel,
                isLoadingInitial = isLoadingInitial,
                initialLoadError = initialLoadError,
                roles = roles,
                selectedRoleId = selectedRoleId,
                fullName = fullName,
                username = username,
                email = email,
                password = password,
                pin = pin,
                fullNameError = fullNameError,
                usernameError = usernameError,
                emailError = emailError,
                passwordError = passwordError,
                pinError = pinError,
                isSaveEnabled = isSaveEnabled,
                isSaving = isSaving,
                onRoleSelected = onRoleSelected,
                onFullNameChange = onFullNameChange,
                onFullNameBlur = onFullNameBlur,
                onUsernameChange = onUsernameChange,
                onUsernameBlur = onUsernameBlur,
                onEmailChange = onEmailChange,
                onEmailBlur = onEmailBlur,
                onPasswordChange = onPasswordChange,
                onPasswordBlur = onPasswordBlur,
                onPinChange = onPinChange,
                onPinBlur = onPinBlur,
                onRetryInitialLoad = onRetryInitialLoad,
                onBack = onBack,
                onSubmit = onSubmit
            )
        }
    }
}

@Composable
private fun UserFormFieldsColumn(
    isEditMode: Boolean,
    isLoadingInitial: Boolean,
    initialLoadError: String?,
    roles: List<UserRole>,
    selectedRoleId: String?,
    fullName: String,
    username: String,
    email: String,
    password: String,
    pin: String,
    fullNameError: String?,
    usernameError: String?,
    emailError: String?,
    passwordError: String?,
    pinError: String?,
    isSaving: Boolean,
    onRoleSelected: (String) -> Unit,
    onFullNameChange: (String) -> Unit,
    onFullNameBlur: () -> Unit,
    onUsernameChange: (String) -> Unit,
    onUsernameBlur: () -> Unit,
    onEmailChange: (String) -> Unit,
    onEmailBlur: () -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordBlur: () -> Unit,
    onPinChange: (String) -> Unit,
    onPinBlur: () -> Unit,
    onRetryInitialLoad: () -> Unit,
    isTablet: Boolean
) {
    when {
        isLoadingInitial -> CustomerFormSkeleton(modifier = Modifier.testTag("tambah-user-loading-skeleton"))
        initialLoadError != null -> UserEmptyCenteredError(
            message = initialLoadError,
            onRetry = onRetryInitialLoad
        )
        else -> {
            val focusManager = LocalFocusManager.current
            var showPassword by remember { mutableStateOf(false) }
            var showPin by remember { mutableStateOf(false) }
            val roleOptions = remember(roles) { roles.toRoleRadioOptions() }

            Text(
                text = "Peran pengguna",
                style = ownerTextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                ),
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .cssLineBoxHeight(20.dp)
                    .semantics { heading() }
            )

            RadioCardGroup(
                options = roleOptions,
                selected = selectedRoleId.orEmpty(),
                onSelect = { if (!isSaving) onRoleSelected(it) },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Data pengguna",
                style = ownerTextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                ),
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .cssLineBoxHeight(20.dp)
                    .semantics { heading() }
            )

            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    FormField(
                        label = "Nama lengkap",
                        value = fullName,
                        onValueChange = onFullNameChange,
                        placeholder = "cth. Budi Santoso",
                        errorText = fullNameError,
                        enabled = !isSaving,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Right) }
                        ),
                        onBlur = onFullNameBlur,
                        modifier = Modifier.weight(1f),
                        testTag = "tambah-user-fullname"
                    )

                    FormField(
                        label = "Username",
                        value = username,
                        onValueChange = onUsernameChange,
                        placeholder = "cth. budi.kasir",
                        errorText = usernameError,
                        enabled = !isSaving,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        onBlur = onUsernameBlur,
                        modifier = Modifier.weight(1f),
                        testTag = "tambah-user-username"
                    )
                }
            } else {
                FormField(
                    label = "Nama lengkap",
                    value = fullName,
                    onValueChange = onFullNameChange,
                    placeholder = "cth. Budi Santoso",
                    errorText = fullNameError,
                    enabled = !isSaving,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    onBlur = onFullNameBlur,
                    testTag = "tambah-user-fullname"
                )

                Spacer(modifier = Modifier.height(16.dp))

                FormField(
                    label = "Username",
                    value = username,
                    onValueChange = onUsernameChange,
                    placeholder = "cth. budi.kasir",
                    errorText = usernameError,
                    enabled = !isSaving,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    onBlur = onUsernameBlur,
                    testTag = "tambah-user-username"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            FormField(
                label = "Email",
                optional = true,
                value = email,
                onValueChange = onEmailChange,
                placeholder = "cth. budi@toko.id",
                errorText = emailError,
                enabled = !isSaving,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = if (isEditMode) ImeAction.Done else ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    onDone = { focusManager.clearFocus() }
                ),
                onBlur = onEmailBlur,
                testTag = "tambah-user-email"
            )

            if (!isEditMode) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Keamanan akun",
                    style = ownerTextStyle(
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink
                    ),
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .cssLineBoxHeight(20.dp)
                        .semantics { heading() }
                )

                if (isTablet) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        PasswordFormField(
                            label = "Password",
                            value = password,
                            onValueChange = onPasswordChange,
                            placeholder = "Minimal 6 karakter",
                            visible = showPassword,
                            onToggleVisibility = { showPassword = !showPassword },
                            errorText = passwordError,
                            enabled = !isSaving,
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next,
                            onNext = { focusManager.moveFocus(FocusDirection.Right) },
                            onBlur = onPasswordBlur,
                            modifier = Modifier.weight(1f),
                            testTag = "tambah-user-password",
                            toggleContentDescription = "Tampilkan password"
                        )

                        PasswordFormField(
                            label = "PIN akses (6 digit)",
                            value = pin,
                            onValueChange = onPinChange,
                            placeholder = "6 angka PIN",
                            visible = showPin,
                            onToggleVisibility = { showPin = !showPin },
                            errorText = pinError,
                            helperText = "Untuk buka layar kasir cepat.",
                            enabled = !isSaving,
                            tabularNumbers = true,
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done,
                            onDone = { focusManager.clearFocus() },
                            onBlur = onPinBlur,
                            modifier = Modifier.weight(1f),
                            testTag = "tambah-user-pin",
                            toggleContentDescription = "Tampilkan PIN"
                        )
                    }
                } else {
                    PasswordFormField(
                        label = "Password",
                        value = password,
                        onValueChange = onPasswordChange,
                        placeholder = "Minimal 6 karakter",
                        visible = showPassword,
                        onToggleVisibility = { showPassword = !showPassword },
                        errorText = passwordError,
                        enabled = !isSaving,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        onBlur = onPasswordBlur,
                        testTag = "tambah-user-password",
                        toggleContentDescription = "Tampilkan password"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PasswordFormField(
                        label = "PIN akses (6 digit)",
                        value = pin,
                        onValueChange = onPinChange,
                        placeholder = "6 angka PIN",
                        visible = showPin,
                        onToggleVisibility = { showPin = !showPin },
                        errorText = pinError,
                        helperText = "Digunakan untuk membuka layar kasir dengan cepat.",
                        enabled = !isSaving,
                        tabularNumbers = true,
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done,
                        onDone = { focusManager.clearFocus() },
                        onBlur = onPinBlur,
                        testTag = "tambah-user-pin",
                        toggleContentDescription = "Tampilkan PIN"
                    )
                }
            }
        }
    }
}

@Composable
private fun PasswordFormField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    visible: Boolean,
    onToggleVisibility: () -> Unit,
    errorText: String?,
    enabled: Boolean,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    testTag: String,
    toggleContentDescription: String,
    modifier: Modifier = Modifier,
    helperText: String? = null,
    tabularNumbers: Boolean = false,
    onNext: (() -> Unit)? = null,
    onDone: (() -> Unit)? = null,
    onBlur: () -> Unit
) {
    FormField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        helperText = helperText,
        errorText = errorText,
        enabled = enabled,
        tabularNumbers = tabularNumbers,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { onNext?.invoke() },
            onDone = { onDone?.invoke() }
        ),
        onBlur = onBlur,
        trailingContent = {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onToggleVisibility
                    )
                    .semantics { contentDescription = toggleContentDescription },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = null,
                    tint = Muted,
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        modifier = modifier,
        testTag = testTag
    )
}

@Composable
private fun PhoneOwnerUserFormLayout(
    isEditMode: Boolean,
    pageTitle: String,
    buttonLabel: String,
    isLoadingInitial: Boolean,
    initialLoadError: String?,
    roles: List<UserRole>,
    selectedRoleId: String?,
    fullName: String,
    username: String,
    email: String,
    password: String,
    pin: String,
    fullNameError: String?,
    usernameError: String?,
    emailError: String?,
    passwordError: String?,
    pinError: String?,
    isSaveEnabled: Boolean,
    isSaving: Boolean,
    onRoleSelected: (String) -> Unit,
    onFullNameChange: (String) -> Unit,
    onFullNameBlur: () -> Unit,
    onUsernameChange: (String) -> Unit,
    onUsernameBlur: () -> Unit,
    onEmailChange: (String) -> Unit,
    onEmailBlur: () -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordBlur: () -> Unit,
    onPinChange: (String) -> Unit,
    onPinBlur: () -> Unit,
    onRetryInitialLoad: () -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    val overlapPx = with(LocalDensity.current) { 28.dp.roundToPx() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .imePadding()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val scrollViewportHeightPx = with(LocalDensity.current) { maxHeight.roundToPx() }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Layout(
                    content = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(Brand)
                                .clipToBounds()
                        ) {
                            RingsPattern(
                                radii = listOf(50.dp, 95.dp, 140.dp),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .wrapContentSize(align = Alignment.TopEnd, unbounded = true)
                                    .offset(x = 90.dp, y = (-90).dp)
                                    .size(300.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 20.dp, top = 56.dp, end = 20.dp)
                            ) {
                                TambahPelangganTopBar(
                                    title = pageTitle,
                                    onBack = onBack,
                                    backTestTag = "tambah-user-back-btn"
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                                .background(SurfaceWhite)
                                .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 24.dp)
                        ) {
                            UserFormFieldsColumn(
                                isEditMode = isEditMode,
                                isLoadingInitial = isLoadingInitial,
                                initialLoadError = initialLoadError,
                                roles = roles,
                                selectedRoleId = selectedRoleId,
                                fullName = fullName,
                                username = username,
                                email = email,
                                password = password,
                                pin = pin,
                                fullNameError = fullNameError,
                                usernameError = usernameError,
                                emailError = emailError,
                                passwordError = passwordError,
                                pinError = pinError,
                                isSaving = isSaving,
                                onRoleSelected = onRoleSelected,
                                onFullNameChange = onFullNameChange,
                                onFullNameBlur = onFullNameBlur,
                                onUsernameChange = onUsernameChange,
                                onUsernameBlur = onUsernameBlur,
                                onEmailChange = onEmailChange,
                                onEmailBlur = onEmailBlur,
                                onPasswordChange = onPasswordChange,
                                onPasswordBlur = onPasswordBlur,
                                onPinChange = onPinChange,
                                onPinBlur = onPinBlur,
                                onRetryInitialLoad = onRetryInitialLoad,
                                isTablet = false
                            )
                        }
                    }
                ) { measurables, constraints ->
                    val headerPlaceable = measurables[0].measure(
                        constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                    )
                    val minPanelHeight = (scrollViewportHeightPx - (headerPlaceable.height - overlapPx))
                        .coerceAtLeast(0)
                    val panelPlaceable = measurables[1].measure(
                        constraints.copy(minHeight = minPanelHeight, maxHeight = Constraints.Infinity)
                    )
                    val totalHeight = (headerPlaceable.height - overlapPx + panelPlaceable.height)
                        .coerceAtLeast(headerPlaceable.height)

                    layout(constraints.maxWidth, totalHeight) {
                        headerPlaceable.placeRelative(0, 0)
                        panelPlaceable.placeRelative(0, headerPlaceable.height - overlapPx)
                    }
                }
            }
        }

        StickyActionBar(
            enabled = isSaveEnabled,
            isSaving = isSaving,
            onSaveClick = onSubmit,
            buttonLabel = buttonLabel,
            buttonTestTag = "tambah-user-save",
            loadingTestTag = "tambah-user-loading"
        )
    }
}

@Composable
private fun TabletOwnerUserFormLayout(
    isEditMode: Boolean,
    pageTitle: String,
    buttonLabel: String,
    isLoadingInitial: Boolean,
    initialLoadError: String?,
    roles: List<UserRole>,
    selectedRoleId: String?,
    fullName: String,
    username: String,
    email: String,
    password: String,
    pin: String,
    fullNameError: String?,
    usernameError: String?,
    emailError: String?,
    passwordError: String?,
    pinError: String?,
    isSaveEnabled: Boolean,
    isSaving: Boolean,
    onRoleSelected: (String) -> Unit,
    onFullNameChange: (String) -> Unit,
    onFullNameBlur: () -> Unit,
    onUsernameChange: (String) -> Unit,
    onUsernameBlur: () -> Unit,
    onEmailChange: (String) -> Unit,
    onEmailBlur: () -> Unit,
    onPasswordChange: (String) -> Unit,
    onPasswordBlur: () -> Unit,
    onPinChange: (String) -> Unit,
    onPinBlur: () -> Unit,
    onRetryInitialLoad: () -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit
) {
    val density = LocalDensity.current
    val heroTitle = if (isEditMode) "Edit pengguna" else "Pengguna baru"
    val heroSubtitle = if (isEditMode) {
        "Perbarui nama, username, email, atau peran akses akun staf."
    } else {
        "Tambahkan akun admin atau kasir dengan hak akses dan PIN kasir."
    }
    val heroChecks = if (isEditMode) {
        listOf(
            "Perubahan peran langsung berlaku",
            "Password dan PIN bisa diubah dari halaman detail",
            "Akun bisa dinonaktifkan kapan saja"
        )
    } else {
        listOf(
            "Pilih peran Admin atau Kasir",
            "Password minimal 6 karakter",
            "PIN 6 digit dipakai untuk buka layar kasir"
        )
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .imePadding()
    ) {
        OwnerTabletNavRail(
            activeDestination = DashboardNavDestination.MENU,
            onNavSelect = onNavSelect
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(432.dp)
                    .fillMaxHeight()
                    .background(Brand)
                    .clipToBounds()
            ) {
                RingsPattern(
                    radii = listOf(50.dp, 95.dp, 140.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .wrapContentSize(align = Alignment.TopEnd, unbounded = true)
                        .offset(x = 90.dp, y = (-90).dp)
                        .size(300.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 24.dp, top = 40.dp, end = 56.dp, bottom = 40.dp)
                ) {
                    TambahPelangganTopBar(
                        title = pageTitle,
                        onBack = onBack,
                        backTestTag = "tambah-user-back-btn"
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = heroTitle,
                        style = ownerTextStyle(
                            fontSize = 28.sp,
                            lineHeight = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnBrand,
                            letterSpacing = (-0.02).em
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = heroSubtitle,
                        style = ownerTextStyle(
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Normal,
                            color = BrandSoftText
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        heroChecks.forEach { checkText ->
                            TabletHighlightCheckRow(checkText)
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .padding(start = 400.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp))
                    .background(SurfaceWhite)
            ) {
                val panelViewportHeightPx = with(density) { maxHeight.roundToPx() }
                val verticalPaddingPx = with(density) { 80.dp.roundToPx() }
                val minButtonGapPx = with(density) { 32.dp.roundToPx() }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 64.dp, top = 40.dp, end = 32.dp, bottom = 40.dp)
                ) {
                    Layout(
                        content = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                UserFormFieldsColumn(
                                    isEditMode = isEditMode,
                                    isLoadingInitial = isLoadingInitial,
                                    initialLoadError = initialLoadError,
                                    roles = roles,
                                    selectedRoleId = selectedRoleId,
                                    fullName = fullName,
                                    username = username,
                                    email = email,
                                    password = password,
                                    pin = pin,
                                    fullNameError = fullNameError,
                                    usernameError = usernameError,
                                    emailError = emailError,
                                    passwordError = passwordError,
                                    pinError = pinError,
                                    isSaving = isSaving,
                                    onRoleSelected = onRoleSelected,
                                    onFullNameChange = onFullNameChange,
                                    onFullNameBlur = onFullNameBlur,
                                    onUsernameChange = onUsernameChange,
                                    onUsernameBlur = onUsernameBlur,
                                    onEmailChange = onEmailChange,
                                    onEmailBlur = onEmailBlur,
                                    onPasswordChange = onPasswordChange,
                                    onPasswordBlur = onPasswordBlur,
                                    onPinChange = onPinChange,
                                    onPinBlur = onPinBlur,
                                    onRetryInitialLoad = onRetryInitialLoad,
                                    isTablet = true
                                )
                            }

                            SaveCustomerButton(
                                enabled = isSaveEnabled,
                                isSaving = isSaving,
                                onClick = onSubmit,
                                label = buttonLabel,
                                buttonTestTag = "tambah-user-save",
                                loadingTestTag = "tambah-user-loading"
                            )
                        }
                    ) { measurables, constraints ->
                        val formPlaceable = measurables[0].measure(
                            constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                        )
                        val buttonPlaceable = measurables[1].measure(
                            constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                        )
                        val innerAvailable = (panelViewportHeightPx - verticalPaddingPx).coerceAtLeast(0)
                        val naturalTotal = formPlaceable.height + minButtonGapPx + buttonPlaceable.height
                        val totalHeight = maxOf(innerAvailable, naturalTotal)
                        val buttonY = totalHeight - buttonPlaceable.height

                        layout(constraints.maxWidth, totalHeight) {
                            formPlaceable.placeRelative(0, 0)
                            buttonPlaceable.placeRelative(0, buttonY)
                        }
                    }
                }
            }
        }
    }
}

private fun sampleUserRoles(): List<UserRole> = listOf(
    UserRole(id = "r-admin", name = "ADMIN"),
    UserRole(id = "r-kasir", name = "KASIR")
)

@Preview(
    name = "OwnerTambahUserPhone",
    showBackground = true,
    widthDp = 390,
    heightDp = 920
)
@Composable
internal fun OwnerTambahUserPhonePreview() {
    TbterminalappTheme {
        OwnerUserFormScreen(
            isEditMode = false,
            roles = sampleUserRoles(),
            selectedRoleId = "r-kasir",
            fullName = "",
            username = "",
            email = "",
            password = "",
            pin = "",
            onRoleSelected = {},
            onFullNameChange = {},
            onUsernameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onPinChange = {},
            onBack = {},
            onSubmit = {},
            onNavSelect = {}
        )
    }
}

@Preview(
    name = "OwnerTambahUserTablet",
    showBackground = true,
    widthDp = 1024,
    heightDp = 768
)
@Composable
internal fun OwnerTambahUserTabletPreview() {
    TbterminalappTheme {
        OwnerUserFormScreen(
            isEditMode = false,
            roles = sampleUserRoles(),
            selectedRoleId = "r-admin",
            fullName = "Andi Pratama",
            username = "andi.admin",
            email = "andi@toko.id",
            password = "",
            pin = "",
            onRoleSelected = {},
            onFullNameChange = {},
            onUsernameChange = {},
            onEmailChange = {},
            onPasswordChange = {},
            onPinChange = {},
            onBack = {},
            onSubmit = {},
            onNavSelect = {}
        )
    }
}
