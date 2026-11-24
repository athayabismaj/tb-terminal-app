package com.tbterminal.app.ui.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.FieldBorder
import com.tbterminal.app.ui.theme.FieldErrorColor
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.KawanSans
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.Placeholder
import com.tbterminal.app.ui.theme.RadiusControl
import com.tbterminal.app.ui.theme.SurfaceWhite
import com.tbterminal.app.ui.theme.TbterminalappTheme

@Immutable
data class LoginAlertState(
    val type: AlertType,
    val title: String,
    val message: String,
    val actionLabel: String? = null
)

@Immutable
data class LoginUiState(
    val initialUsername: String = "",
    val initialPassword: String = "",
    val isLoading: Boolean = false,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val isUsernameError: Boolean = !usernameError.isNullOrBlank(),
    val isPasswordError: Boolean = !passwordError.isNullOrBlank(),
    val alert: LoginAlertState? = null
)

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: (role: String, name: String) -> Unit
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var lastSubmittedUsername by rememberSaveable { mutableStateOf("") }
    var lastSubmittedPassword by rememberSaveable { mutableStateOf("") }
    var alertDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        alertDismissed = false
        if (authState is AuthState.Success) {
            val state = authState as AuthState.Success
            onLoginSuccess(state.role, state.name)
            viewModel.resetState()
        }
    }

    val errorState = authState as? AuthState.Error
    val errorMsg = errorState?.message
    val lower = errorMsg?.lowercase().orEmpty()
    val isNetworkError = errorMsg != null && (
        lower.contains("koneksi") || lower.contains("internet") ||
            lower.contains("network") || lower.contains("timeout") ||
            lower.contains("jaringan") || lower.contains("server sedang") ||
            lower.contains("server tidak dapat dihubungi")
        )

    val usernameErrorText = when {
        errorState == null || isNetworkError -> null
        errorState.usernameError != null || errorState.isUsernameHighlighted -> errorState.usernameError
        else -> null
    }
    val passwordErrorText = when {
        errorState == null || isNetworkError -> null
        errorState.passwordError != null || errorState.isPasswordHighlighted -> errorState.passwordError
        else -> errorMsg
    }
    val highlightUsername = !isNetworkError && errorState != null && (
        errorState.isUsernameHighlighted || !errorState.usernameError.isNullOrBlank()
        )
    val highlightPassword = !isNetworkError && errorState != null && (
        errorState.isPasswordHighlighted || !passwordErrorText.isNullOrBlank()
        )

    val mappedAlert = when {
        alertDismissed -> null
        isNetworkError -> LoginAlertState(
            type = AlertType.Error,
            title = "Tidak dapat terhubung",
            message = "Periksa koneksi internet.",
            actionLabel = "Coba lagi"
        )
        else -> null
    }

    LoginScreen(
        state = LoginUiState(
            isLoading = authState is AuthState.Loading,
            usernameError = usernameErrorText,
            passwordError = passwordErrorText,
            isUsernameError = highlightUsername,
            isPasswordError = highlightPassword,
            alert = mappedAlert
        ),
        onLogin = { username, password ->
            alertDismissed = false
            lastSubmittedUsername = username.trim()
            lastSubmittedPassword = password
            viewModel.login(lastSubmittedUsername, lastSubmittedPassword)
        },
        onForgot = { showForgotPasswordDialog = true },
        onRetry = {
            alertDismissed = false
            if (lastSubmittedUsername.isNotEmpty() || lastSubmittedPassword.isNotEmpty()) {
                viewModel.login(lastSubmittedUsername, lastSubmittedPassword)
            } else {
                viewModel.resetState()
            }
        },
        onAlertDismiss = { alertDismissed = true },
        onUsernameChanged = { viewModel.onLoginInputChanged(isUsername = true) },
        onPasswordChanged = { viewModel.onLoginInputChanged(isUsername = false) }
    )

    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            onDismiss = { showForgotPasswordDialog = false },
            onContactAdmin = { openAdminWhatsApp(context) }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    state: LoginUiState,
    onLogin: (username: String, password: String) -> Unit,
    onForgot: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onAlertDismiss: () -> Unit = {},
    onUsernameChanged: (String) -> Unit = {},
    onPasswordChanged: (String) -> Unit = {}
) {
    val imeVisible = WindowInsets.isImeVisible
    LoginContent(
        state = state,
        imeVisible = imeVisible,
        onLogin = onLogin,
        onForgot = onForgot,
        onRetry = onRetry,
        onAlertDismiss = onAlertDismiss,
        modifier = modifier,
        onUsernameChanged = onUsernameChanged,
        onPasswordChanged = onPasswordChanged
    )
}

@Composable
internal fun LoginContent(
    state: LoginUiState,
    imeVisible: Boolean,
    onLogin: (username: String, password: String) -> Unit,
    onForgot: () -> Unit,
    onRetry: () -> Unit,
    onAlertDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
    onUsernameChanged: (String) -> Unit = {},
    onPasswordChanged: (String) -> Unit = {}
) {
    var username by rememberSaveable(state.initialUsername) { mutableStateOf(state.initialUsername) }
    var password by rememberSaveable(state.initialPassword) { mutableStateOf(state.initialPassword) }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val passwordFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val submitLogin = {
        focusManager.clearFocus()
        if (!state.isLoading) {
            onLogin(username, password)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Brand)
            .imePadding()
    ) {
        val isTablet = maxWidth >= 600.dp

        if (isTablet) {
            TabletLoginLayout(
                maxWidth = maxWidth,
                username = username,
                onUsernameChange = {
                    username = it
                    onUsernameChanged(it)
                },
                password = password,
                onPasswordChange = {
                    password = it
                    onPasswordChanged(it)
                },
                passwordVisible = passwordVisible,
                onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                isLoading = state.isLoading,
                usernameError = state.usernameError,
                passwordError = state.passwordError,
                isUsernameError = state.isUsernameError,
                isPasswordError = state.isPasswordError,
                passwordFocusRequester = passwordFocusRequester,
                onSubmit = submitLogin,
                onForgot = onForgot
            )
        } else {
            PhoneLoginLayout(
                screenHeight = maxHeight,
                imeVisible = imeVisible,
                username = username,
                onUsernameChange = {
                    username = it
                    onUsernameChanged(it)
                },
                password = password,
                onPasswordChange = {
                    password = it
                    onPasswordChanged(it)
                },
                passwordVisible = passwordVisible,
                onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                isLoading = state.isLoading,
                usernameError = state.usernameError,
                passwordError = state.passwordError,
                isUsernameError = state.isUsernameError,
                isPasswordError = state.isPasswordError,
                passwordFocusRequester = passwordFocusRequester,
                onSubmit = submitLogin,
                onForgot = onForgot
            )
        }

        // STATE A: Alert melayang di atas konten, tidak menggeser formulir
        val alert = state.alert
        AppAlert(
            type = alert?.type ?: AlertType.Error,
            title = alert?.title.orEmpty(),
            message = alert?.message.orEmpty(),
            actionLabel = alert?.actionLabel,
            onAction = onRetry,
            onDismiss = onAlertDismiss,
            visible = alert != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 72.dp)
        )
    }
}

@Composable
private fun PhoneLoginLayout(
    screenHeight: Dp,
    imeVisible: Boolean,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    isLoading: Boolean,
    usernameError: String?,
    passwordError: String?,
    isUsernameError: Boolean,
    isPasswordError: Boolean,
    passwordFocusRequester: FocusRequester,
    onSubmit: () -> Unit,
    onForgot: () -> Unit
) {
    val heroHeight = if (imeVisible) 112.dp else 392.dp
    val sheetMinHeight = (screenHeight - heroHeight).coerceAtLeast(0.dp)
    val sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // .hero
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight)
                .clipToBounds()
        ) {
            // .rings tetap ada pada state normal maupun keyboard terbuka
            RingsPattern(
                radii = listOf(50.dp, 95.dp, 140.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .wrapContentSize(align = Alignment.TopEnd, unbounded = true)
                    .offset(x = 80.dp, y = (-80).dp)
                    .size(300.dp)
            )

            if (!imeVisible) {
                DotsPattern(
                    dotRadius = 1.5.dp,
                    step = 20.dp,
                    maskAtBottomRight = true,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(width = 190.dp, height = 170.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 28.dp, top = 136.dp, end = 28.dp)
                ) {
                    BrandLogoMark(
                        containerSize = 56.dp,
                        cornerRadius = 16.dp,
                        iconSize = 28.dp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    HeroTitle(text = "Barokah Jaya", fontSize = 34.sp, lineBoxHeight = (34f * 1.1f).dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    HeroSubtitle(text = "Sistem Kasir & Inventaris", fontSize = 15.sp, lineBoxHeight = (15f * 1.21f).dp)
                }
            } else {
                // Header ringkas saat keyboard terbuka: tinggi 112, padding 24/28/0, satu baris rata tengah vertikal jarak 12
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 28.dp, top = 24.dp, end = 28.dp, bottom = 0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BrandLogoMark(
                        containerSize = 40.dp,
                        cornerRadius = 12.dp,
                        iconSize = 20.dp
                    )
                    HeroTitle(text = "Barokah Jaya", fontSize = 24.sp, lineBoxHeight = (24f * 1.1f).dp)
                }
            }
        }

        // .sheet
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = sheetMinHeight)
                .clip(sheetShape)
                .background(SurfaceWhite)
                .navigationBarsPadding()
                .padding(
                    start = 28.dp,
                    top = if (imeVisible) 28.dp else 40.dp,
                    end = 28.dp,
                    bottom = if (imeVisible) 16.dp else 28.dp
                ),
            propagateMinConstraints = true
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Top
            ) {
                LoginFormContent(
                    isTablet = false,
                    username = username,
                    onUsernameChange = onUsernameChange,
                    password = password,
                    onPasswordChange = onPasswordChange,
                    passwordVisible = passwordVisible,
                    onTogglePasswordVisibility = onTogglePasswordVisibility,
                    isLoading = isLoading,
                    usernameError = usernameError,
                    passwordError = passwordError,
                    isUsernameError = isUsernameError,
                    isPasswordError = isPasswordError,
                    passwordFocusRequester = passwordFocusRequester,
                    onSubmit = onSubmit,
                    onForgot = onForgot,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun TabletLoginLayout(
    maxWidth: Dp,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    isLoading: Boolean,
    usernameError: String?,
    passwordError: String?,
    isUsernameError: Boolean,
    isPasswordError: Boolean,
    passwordFocusRequester: FocusRequester,
    onSubmit: () -> Unit,
    onForgot: () -> Unit
) {
    val heroWidth = if (maxWidth >= 920.dp) 520.dp else (maxWidth * 0.45f)
    val sheetShape = RoundedCornerShape(topStart = 40.dp, bottomStart = 40.dp)

    Box(modifier = Modifier.fillMaxSize()) {
        // .hero
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(heroWidth)
                .fillMaxHeight()
                .clipToBounds()
        ) {
            RingsPattern(
                radii = listOf(70.dp, 130.dp, 190.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .wrapContentSize(align = Alignment.TopStart, unbounded = true)
                    .offset(x = (-140).dp, y = (-140).dp)
                    .size(440.dp)
            )

            DotsPattern(
                dotRadius = 1.6.dp,
                step = 22.dp,
                maskAtBottomRight = false,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(width = 300.dp, height = 240.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 72.dp),
                verticalArrangement = Arrangement.Center
            ) {
                BrandLogoMark(
                    containerSize = 72.dp,
                    cornerRadius = 20.dp,
                    iconSize = 36.dp
                )
                Spacer(modifier = Modifier.height(32.dp))
                HeroTitle(text = "Barokah Jaya", fontSize = 48.sp, lineBoxHeight = (48f * 1.1f).dp)
                Spacer(modifier = Modifier.height(12.dp))
                HeroSubtitle(text = "Sistem Kasir & Inventaris", fontSize = 18.sp, lineBoxHeight = (18f * 1.21f).dp)
            }
        }

        // .sheet: margin-left: -40px; padding-left: 40px; border-radius: 40px 0 0 40px;
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = heroWidth - 40.dp)
                .clip(sheetShape)
                .background(SurfaceWhite)
                .navigationBarsPadding()
                .padding(start = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                LoginFormContent(
                    isTablet = true,
                    username = username,
                    onUsernameChange = onUsernameChange,
                    password = password,
                    onPasswordChange = onPasswordChange,
                    passwordVisible = passwordVisible,
                    onTogglePasswordVisibility = onTogglePasswordVisibility,
                    isLoading = isLoading,
                    usernameError = usernameError,
                    passwordError = passwordError,
                    isUsernameError = isUsernameError,
                    isPasswordError = isPasswordError,
                    passwordFocusRequester = passwordFocusRequester,
                    onSubmit = onSubmit,
                    onForgot = onForgot,
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 64.dp)
                        .widthIn(max = 400.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LoginFormContent(
    isTablet: Boolean,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    isLoading: Boolean,
    usernameError: String?,
    passwordError: String?,
    isUsernameError: Boolean,
    isPasswordError: Boolean,
    passwordFocusRequester: FocusRequester,
    onSubmit: () -> Unit,
    onForgot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inputHeight = if (isTablet) 60.dp else 56.dp
    val inputFontSize = if (isTablet) 17.sp else 16.sp
    val startPadding = if (isTablet) 20.dp else 18.dp
    val endPadding = if (isTablet) 60.dp else 56.dp
    val submitHeight = if (isTablet) 60.dp else 56.dp
    val submitFontSize = if (isTablet) 17.sp else 16.sp

    Column(modifier = modifier) {
        CustomLabeledField(
            label = "Nama pengguna",
            value = username,
            onValueChange = onUsernameChange,
            placeholder = "Contoh: admin.toko",
            errorMessage = usernameError,
            isError = isUsernameError,
            inputHeight = inputHeight,
            inputFontSize = inputFontSize,
            startPadding = startPadding,
            endPadding = endPadding,
            isPassword = false,
            passwordVisible = false,
            onTogglePasswordVisibility = null,
            toggleTouchSize = 0.dp,
            toggleOffset = 0.dp,
            toggleIconSize = 0.dp,
            enabled = !isLoading,
            contentType = ContentType.Username,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { passwordFocusRequester.requestFocus() }
            ),
            testTag = "login-username-input"
        )

        Spacer(modifier = Modifier.height(28.dp))

        CustomLabeledField(
            label = "Kata sandi",
            value = password,
            onValueChange = onPasswordChange,
            placeholder = "",
            errorMessage = passwordError,
            isError = isPasswordError,
            inputHeight = inputHeight,
            inputFontSize = inputFontSize,
            startPadding = startPadding,
            endPadding = endPadding,
            isPassword = true,
            passwordVisible = passwordVisible,
            onTogglePasswordVisibility = onTogglePasswordVisibility,
            toggleTouchSize = if (isTablet) 48.dp else 44.dp,
            toggleOffset = if (isTablet) 6.dp else 4.dp,
            toggleIconSize = if (isTablet) 24.dp else 22.dp,
            enabled = !isLoading,
            contentType = ContentType.Password,
            focusRequester = passwordFocusRequester,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onSubmit() }
            ),
            testTag = "login-password-input"
        )

        // .forgot-row { display: flex; justify-content: flex-end; margin-bottom: 4px; }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "Lupa kata sandi?",
                fontFamily = KawanSans,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = (14f * 1.21f).sp,
                color = Brand,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.None
                    )
                ),
                modifier = Modifier
                    .clickable(
                        enabled = !isLoading,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onForgot
                    )
                    .padding(start = 12.dp, top = 14.dp, end = 0.dp, bottom = 14.dp)
                    .testTag("login-forgot-password-link")
            )
        }

        // .submit
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(submitHeight)
                .clip(RoundedCornerShape(RadiusControl))
                .background(Brand)
                .clickable(
                    enabled = !isLoading,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onSubmit
                )
                .testTag("login-submit-button"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Masuk",
                fontFamily = KawanSans,
                fontSize = submitFontSize,
                fontWeight = FontWeight.SemiBold,
                lineHeight = (submitFontSize.value * 1.21f).sp,
                color = OnBrand,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.None
                    )
                )
            )
        }
    }
}

@Composable
private fun CustomLabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    errorMessage: String?,
    isError: Boolean = !errorMessage.isNullOrBlank(),
    inputHeight: Dp,
    inputFontSize: TextUnit,
    startPadding: Dp,
    endPadding: Dp,
    isPassword: Boolean,
    passwordVisible: Boolean,
    onTogglePasswordVisibility: (() -> Unit)?,
    toggleTouchSize: Dp,
    toggleOffset: Dp,
    toggleIconSize: Dp,
    enabled: Boolean,
    contentType: ContentType,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    focusRequester: FocusRequester? = null,
    testTag: String
) {
    var isFocused by remember { mutableStateOf(false) }
    val localFocusRequester = remember { FocusRequester() }
    val effectiveFocusRequester = focusRequester ?: localFocusRequester
    val hasError = isError || !errorMessage.isNullOrBlank()

    val fieldHeight = inputHeight + 3.dp
    val activeColor = when {
        hasError -> FieldErrorColor
        isFocused -> Brand
        else -> FieldBorder
    }
    val labelColor = when {
        hasError -> FieldErrorColor
        isFocused -> Brand
        else -> Muted
    }
    val hasOuterRing = hasError || isFocused

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(fieldHeight)
                .drawBehind {
                    val radiusPx = RadiusControl.toPx()
                    val strokePx = 1.5.dp.toPx()
                    val halfStroke = strokePx / 2f
                    // Border 1.5dp selalu di tempatnya agar tidak menggeser tata letak
                    drawRoundRect(
                        color = activeColor,
                        topLeft = Offset(halfStroke, halfStroke),
                        size = Size(
                            width = size.width - strokePx,
                            height = size.height - strokePx
                        ),
                        cornerRadius = CornerRadius(
                            x = (radiusPx - halfStroke).coerceAtLeast(0f),
                            y = (radiusPx - halfStroke).coerceAtLeast(0f)
                        ),
                        style = Stroke(width = strokePx)
                    )
                    // Saat fokus atau error: cincin 1dp di LUAR border ([-1dp .. 0dp])
                    if (hasOuterRing) {
                        val ringPx = 1.dp.toPx()
                        val halfRing = ringPx / 2f
                        drawRoundRect(
                            color = activeColor,
                            topLeft = Offset(-halfRing, -halfRing),
                            size = Size(
                                width = size.width + ringPx,
                                height = size.height + ringPx
                            ),
                            cornerRadius = CornerRadius(
                                x = radiusPx + halfRing,
                                y = radiusPx + halfRing
                            ),
                            style = Stroke(width = ringPx)
                        )
                    }
                }
        ) {
            // Area dalam setelah border 1.5dp
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(1.5.dp)
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    enabled = enabled,
                    visualTransformation = if (isPassword && !passwordVisible) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    textStyle = TextStyle(
                        fontFamily = KawanSans,
                        fontSize = inputFontSize,
                        fontWeight = FontWeight.Normal,
                        lineHeight = (inputFontSize.value * 1.21f).sp,
                        color = Ink,
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.None
                        )
                    ),
                    cursorBrush = SolidColor(if (hasError) FieldErrorColor else Ink),
                    modifier = Modifier
                        .fillMaxSize()
                        .focusRequester(effectiveFocusRequester)
                        .semantics {
                            this.contentType = contentType
                            if (hasError) {
                                error(errorMessage ?: "Periksa kembali input")
                            }
                        }
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                        }
                        .testTag(testTag),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = startPadding, end = endPadding),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (value.isEmpty() && placeholder.isNotEmpty()) {
                                Text(
                                    text = placeholder,
                                    fontFamily = KawanSans,
                                    fontSize = inputFontSize,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = (inputFontSize.value * 1.21f).sp,
                                    color = Placeholder,
                                    style = TextStyle(
                                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                                        lineHeightStyle = LineHeightStyle(
                                            alignment = LineHeightStyle.Alignment.Center,
                                            trim = LineHeightStyle.Trim.None
                                        )
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                if (isPassword && onTogglePasswordVisibility != null) {
                    val toggleDescription = if (passwordVisible) {
                        "Sembunyikan kata sandi"
                    } else {
                        "Tampilkan kata sandi"
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = toggleOffset, end = toggleOffset)
                            .size(toggleTouchSize)
                            .clickable(
                                enabled = enabled,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Button,
                                onClick = onTogglePasswordVisibility
                            )
                            .semantics { contentDescription = toggleDescription }
                            .testTag("login-password-toggle"),
                        contentAlignment = Alignment.Center
                    ) {
                        EyeToggleIcon(
                            visible = passwordVisible,
                            iconSize = toggleIconSize
                        )
                    }
                }
            }

            // Label selalu duduk di garis border atas: offset kiri 14dp, padding horizontal 6dp, latar putih
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 14.dp + 1.5.dp, y = (-10).dp + 1.5.dp)
                    .background(SurfaceWhite)
                    .padding(horizontal = 6.dp)
                    .height(18.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = label,
                    fontFamily = KawanSans,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp,
                    color = labelColor,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.None
                        )
                    )
                )
            }
        }

        if (!errorMessage.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .padding(start = 18.dp, top = 8.dp, end = 0.dp, bottom = 0.dp)
                    .heightIn(min = 16.dp)
                    .testTag("${testTag}-error"),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = errorMessage,
                    fontFamily = KawanSans,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 16.sp,
                    color = FieldErrorColor,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.None
                        )
                    )
                )
            }
        }
    }
}

@Composable
private fun EyeToggleIcon(
    visible: Boolean,
    iconSize: Dp
) {
    val slashPath = remember { PathParser().parsePathString("M3 3l18 18").toPath() }
    val hideTopPath = remember {
        PathParser().parsePathString("M10.6 6.1A9.8 9.8 0 0 1 12 6c5 0 8.5 4 9.5 6a12 12 0 0 1-2.2 3").toPath()
    }
    val hideBottomPath = remember {
        PathParser().parsePathString("M6.6 6.7C4.5 8 3.1 10.2 2.5 12c1 2 4.5 6 9.5 6 1.4 0 2.6-.3 3.7-.8").toPath()
    }
    val hidePupilPath = remember {
        PathParser().parsePathString("M9.9 9.9a3 3 0 0 0 4.2 4.2").toPath()
    }
    val showOutlinePath = remember {
        PathParser().parsePathString("M2.5 12S6 6 12 6s9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6z").toPath()
    }

    Canvas(modifier = Modifier.size(iconSize)) {
        val scale = size.width / 24f
        val stroke = Stroke(
            width = 1.8f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        withTransform({ scale(scale, scale, Offset.Zero) }) {
            if (visible) {
                drawPath(path = showOutlinePath, color = Muted, style = stroke)
                drawCircle(color = Muted, radius = 3f, center = Offset(12f, 12f), style = stroke)
            } else {
                drawPath(path = slashPath, color = Muted, style = stroke)
                drawPath(path = hideTopPath, color = Muted, style = stroke)
                drawPath(path = hideBottomPath, color = Muted, style = stroke)
                drawPath(path = hidePupilPath, color = Muted, style = stroke)
            }
        }
    }
}

private fun openAdminWhatsApp(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(
                "https://wa.me/?text=Halo%20Admin%20Terminal%20Barokah%20Jaya,%20saya%20membutuhkan%20bantuan%20reset%20kata%20sandi%20kasir."
            )
        }
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}

@Preview(name = "Barokah Jaya Login - Ponsel Default (390x844)", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun LoginPhoneDesignPreview() {
    TbterminalappTheme {
        LoginContent(
            state = LoginUiState(initialPassword = "katasandi"),
            imeVisible = false,
            onLogin = { _, _ -> },
            onForgot = {},
            onRetry = {}
        )
    }
}

@Preview(name = "Barokah Jaya Login - Tablet (1024x768)", widthDp = 1024, heightDp = 768, showBackground = true)
@Composable
fun LoginTabletDesignPreview() {
    TbterminalappTheme {
        LoginContent(
            state = LoginUiState(initialPassword = "katasandi"),
            imeVisible = false,
            onLogin = { _, _ -> },
            onForgot = {},
            onRetry = {}
        )
    }
}

@Preview(name = "Barokah Jaya Login - Ponsel + Alert Error (390x844)", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun LoginPhoneAlertErrorPreview() {
    TbterminalappTheme {
        LoginContent(
            state = LoginUiState(
                initialUsername = "owner",
                initialPassword = "katasandi",
                alert = LoginAlertState(
                    type = AlertType.Error,
                    title = "Tidak dapat terhubung",
                    message = "Periksa koneksi internet.",
                    actionLabel = "Coba lagi"
                )
            ),
            imeVisible = false,
            onLogin = { _, _ -> },
            onForgot = {},
            onRetry = {}
        )
    }
}

@Preview(name = "Barokah Jaya Login - Ponsel + Keyboard + Error (390x844)", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun LoginPhoneKeyboardErrorPreview() {
    TbterminalappTheme {
        LoginContent(
            state = LoginUiState(
                initialUsername = "owner",
                initialPassword = "123",
                passwordError = "Kata sandi minimal 6 karakter"
            ),
            imeVisible = true,
            onLogin = { _, _ -> },
            onForgot = {},
            onRetry = {}
        )
    }
}
