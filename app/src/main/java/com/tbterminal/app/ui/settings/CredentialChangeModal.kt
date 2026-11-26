package com.tbterminal.app.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tbterminal.app.data.validation.CredentialValidator
import com.tbterminal.app.ui.auth.ForgotPasswordDialog
import com.tbterminal.app.ui.auth.MaxPinLength
import com.tbterminal.app.ui.auth.PinDots
import com.tbterminal.app.ui.auth.PinKeypad
import com.tbterminal.app.ui.dashboard.owner.DetailProfilScreen
import com.tbterminal.app.ui.dashboard.owner.OwnerIcons
import com.tbterminal.app.ui.dashboard.owner.OwnerVectorIcon
import com.tbterminal.app.ui.dashboard.owner.cssLineBoxHeight
import com.tbterminal.app.ui.dashboard.owner.ownerTextStyle
import com.tbterminal.app.ui.dashboard.owner.sampleReferenceProfileState
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.FieldErrorColor
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.OwnerBorder
import com.tbterminal.app.ui.theme.SurfaceWhite
import com.tbterminal.app.ui.theme.TbterminalappTheme

internal enum class CredentialDialog { PASSWORD, PIN }

private val MODAL_TABLET_BREAKPOINT = 840.dp
private val ModalScrimColor = Color(0x7A14201B)
private val ModalDragHandleColor = Color(0xFFD5DBD7)
private val MutedPlaceholderColor = Color(0xFF8A938E)

@Immutable
internal data class PasswordChecklistState(
    val hasMin8Chars: Boolean,
    val hasLetterAndDigit: Boolean,
    val isDifferentFromCurrent: Boolean,
    val withinMax72Chars: Boolean = true
) {
    val allSatisfied: Boolean
        get() = hasMin8Chars && withinMax72Chars && hasLetterAndDigit && isDifferentFromCurrent
}

internal fun evaluatePasswordChecklist(
    currentPassword: String,
    newPassword: String
): PasswordChecklistState = PasswordChecklistState(
    hasMin8Chars = newPassword.length >= CredentialValidator.PASSWORD_MIN_LENGTH,
    hasLetterAndDigit = newPassword.any(Char::isLetter) && newPassword.any(Char::isDigit),
    isDifferentFromCurrent = newPassword.isNotEmpty() && newPassword != currentPassword,
    withinMax72Chars = newPassword.length <= CredentialValidator.PASSWORD_MAX_LENGTH
)

internal fun isWeakPin(pin: String): Boolean {
    if (pin.length < 4 || !pin.all(Char::isDigit)) return true
    if (pin.toSet().size == 1) return true
    var ascending = true
    var descending = true
    for (i in 1 until pin.length) {
        val prev = pin[i - 1].digitToInt()
        val curr = pin[i].digitToInt()
        if (curr != prev + 1) ascending = false
        if (curr != prev - 1) descending = false
    }
    return ascending || descending
}

/**
 * Pop-up adaptif (bottom sheet di ponsel < 840dp, dialog tengah 480dp di tablet >= 840dp)
 * untuk Ubah kata sandi & Ubah PIN transaksi.
 */
@Composable
internal fun CredentialChangeDialog(
    type: CredentialDialog,
    isSaving: Boolean,
    errorMessage: String?,
    onInputChanged: () -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit,
    onVerifyCurrentPin: ((String, (PinVerifyOutcome) -> Unit) -> Unit)? = null,
    onResetPinWithPassword: ((String, String, String, () -> Unit) -> Unit)? = null,
    onForgotPasswordClick: (() -> Unit)? = null,
    onForceLogout: (() -> Unit)? = null,
    requireCurrentCredential: Boolean = true
) {
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showForgotPinResetModal by remember { mutableStateOf(false) }

    if (showForgotPinResetModal && onResetPinWithPassword != null) {
        ResetPinViaPasswordSheet(
            isSaving = isSaving,
            errorMessage = errorMessage,
            onInputChanged = onInputChanged,
            onDismiss = {
                if (!isSaving) {
                    showForgotPinResetModal = false
                    onInputChanged()
                }
            },
            onSubmit = { currentPassword, newPin, confirmPin ->
                onResetPinWithPassword(currentPassword, newPin, confirmPin) {
                    showForgotPinResetModal = false
                    onDismiss()
                }
            }
        )
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= MODAL_TABLET_BREAKPOINT
        if (type == CredentialDialog.PASSWORD) {
            AdaptiveSheet(
                title = "Ubah kata sandi",
                isTablet = isTablet,
                tabletHeight = if (requireCurrentCredential) 624.dp else 460.dp,
                dismissEnabled = !isSaving,
                onDismiss = onDismiss,
                content = {
                    PasswordModalContent(
                        isSaving = isSaving,
                        errorMessage = errorMessage,
                        onInputChanged = onInputChanged,
                        onForgotPasswordClick = {
                            if (onForgotPasswordClick != null) {
                                onForgotPasswordClick()
                            } else {
                                showForgotPasswordDialog = true
                            }
                        },
                        onSubmit = onSubmit,
                        requireCurrentCredential = requireCurrentCredential
                    )
                }
            )
        } else {
            PinFlowModalContainer(
                isTablet = isTablet,
                isSaving = isSaving,
                errorMessage = errorMessage,
                onInputChanged = onInputChanged,
                onDismiss = onDismiss,
                onVerifyCurrentPin = onVerifyCurrentPin,
                onSubmitPinChange = onSubmit,
                onForgotPinClick = { showForgotPinResetModal = true },
                onForceLogout = onForceLogout,
                requireCurrentCredential = requireCurrentCredential
            )
        }
    }

    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(onDismiss = { showForgotPasswordDialog = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdaptiveSheet(
    title: String,
    isTablet: Boolean,
    tabletHeight: Dp,
    dismissEnabled: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetTestTag: String = "credential-dialog",
    content: @Composable () -> Unit
) {
    if (isTablet) {
        Dialog(
            onDismissRequest = { if (dismissEnabled) onDismiss() },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ModalScrimColor)
                    .clickable(
                        enabled = dismissEnabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = modifier
                        .width(480.dp)
                        .heightIn(min = tabletHeight, max = 720.dp)
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = Color(0x3D14201B),
                            spotColor = Color(0x3D14201B)
                        )
                        .clip(RoundedCornerShape(28.dp))
                        .background(SurfaceWhite)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .imePadding()
                        .padding(start = 32.dp, top = 16.dp, end = 32.dp, bottom = 32.dp)
                        .testTag(sheetTestTag)
                ) {
                    SheetHeaderRow(
                        title = title,
                        dismissEnabled = dismissEnabled,
                        onDismiss = onDismiss
                    )
                    content()
                }
            }
        }
    } else {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { if (dismissEnabled) onDismiss() },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = SurfaceWhite,
            tonalElevation = 0.dp,
            scrimColor = ModalScrimColor,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .size(width = 32.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ModalDragHandleColor)
                )
            },
            modifier = modifier.testTag(sheetTestTag)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
            ) {
                SheetHeaderRow(
                    title = title,
                    dismissEnabled = dismissEnabled,
                    onDismiss = onDismiss
                )
                content()
            }
        }
    }
}

@Composable
private fun SheetHeaderRow(
    title: String,
    dismissEnabled: Boolean,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = ownerTextStyle(
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                letterSpacing = (-0.01).em
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = false)
                .cssLineBoxHeight(22.dp)
                .semantics { heading() }
        )

        Box(
            modifier = Modifier.size(width = 36.dp, height = 48.dp)
        ) {
            Box(
                modifier = Modifier
                    .wrapContentSize(align = Alignment.CenterStart, unbounded = true)
                    .size(48.dp)
                    .clickable(
                        enabled = dismissEnabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onDismiss
                    )
                    .semantics { contentDescription = "Tutup" }
                    .testTag("credential-close-btn"),
                contentAlignment = Alignment.Center
            ) {
                OwnerVectorIcon(
                    imageVector = OwnerIcons.CloseX,
                    tint = Muted,
                    size = 24.dp
                )
            }
        }
    }
}

@Composable
internal fun PasswordModalContent(
    isSaving: Boolean,
    errorMessage: String?,
    onInputChanged: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSubmit: (String, String, String) -> Unit,
    initialCurrent: String = "",
    initialNew: String = "",
    initialConfirm: String = "",
    initialNewFocused: Boolean = false,
    fillSpaceAboveButton: Boolean = false,
    requireCurrentCredential: Boolean = true
) {
    val focusManager = LocalFocusManager.current
    var currentValue by remember { mutableStateOf(initialCurrent) }
    var newValue by remember { mutableStateOf(initialNew) }
    var confirmValue by remember { mutableStateOf(initialConfirm) }
    var confirmTouched by remember { mutableStateOf(initialConfirm.isNotEmpty()) }

    val checklist = remember(currentValue, newValue) {
        evaluatePasswordChecklist(currentPassword = currentValue, newPassword = newValue)
    }

    val currentError = errorMessage?.takeIf {
        it.contains("saat ini", ignoreCase = true) ||
            it.contains("lama", ignoreCase = true) ||
            it.contains("salah", ignoreCase = true)
    }
    val mismatchError = if (confirmTouched && confirmValue.isNotEmpty() && confirmValue != newValue) {
        "Kata sandi tidak sama"
    } else null

    val isChecklistSatisfied = if (requireCurrentCredential) {
        checklist.allSatisfied
    } else {
        checklist.hasMin8Chars && checklist.withinMax72Chars && checklist.hasLetterAndDigit
    }
    val maxLengthError = if (!checklist.withinMax72Chars) {
        "Password maksimal ${CredentialValidator.PASSWORD_MAX_LENGTH} karakter"
    } else null
    val isReady = (!requireCurrentCredential || currentValue.isNotBlank()) &&
        isChecklistSatisfied &&
        confirmValue == newValue &&
        !isSaving

    fun trySubmit() {
        confirmTouched = true
        if (isReady) {
            onSubmit(currentValue, newValue, confirmValue)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fillSpaceAboveButton) Modifier.weight(1f) else Modifier)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            if (requireCurrentCredential) {
                PasswordField(
                    label = "Kata sandi saat ini",
                    value = currentValue,
                    onValueChange = {
                        currentValue = it
                        onInputChanged()
                    },
                    enabled = !isSaving,
                    errorText = currentError,
                    imeAction = ImeAction.Next,
                    onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                    testTag = "credential-current"
                )

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clickable(
                                enabled = !isSaving,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Button,
                                onClick = onForgotPasswordClick
                            )
                            .testTag("password-forgot-link"),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = "Lupa kata sandi?",
                            style = ownerTextStyle(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Medium,
                                color = Brand
                            ),
                            modifier = Modifier.cssLineBoxHeight(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            PasswordField(
                label = "Kata sandi baru",
                value = newValue,
                onValueChange = {
                    newValue = it
                    onInputChanged()
                },
                enabled = !isSaving,
                errorText = maxLengthError,
                forceFocusedBorder = initialNewFocused,
                imeAction = ImeAction.Next,
                onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                testTag = "credential-new"
            )

            Spacer(modifier = Modifier.height(10.dp))

            PasswordChecklist(
                state = checklist,
                showCurrentRule = requireCurrentCredential
            )

            Spacer(modifier = Modifier.height(20.dp))

            PasswordField(
                label = "Ulangi kata sandi baru",
                value = confirmValue,
                onValueChange = {
                    confirmTouched = true
                    confirmValue = it
                    onInputChanged()
                },
                enabled = !isSaving,
                errorText = mismatchError,
                imeAction = ImeAction.Done,
                onImeAction = ::trySubmit,
                testTag = "credential-confirmation"
            )

            if (errorMessage != null && currentError == null) {
                Spacer(modifier = Modifier.height(8.dp))
                FieldErrorRow(message = errorMessage)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isReady || isSaving) Brand else OwnerBorder)
                .clickable(
                    enabled = isReady,
                    role = Role.Button,
                    onClick = ::trySubmit
                )
                .testTag("credential-save"),
            contentAlignment = Alignment.Center
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = OnBrand,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Simpan kata sandi",
                    style = ownerTextStyle(
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isReady) OnBrand else MutedPlaceholderColor
                    ),
                    modifier = Modifier.cssLineBoxHeight(20.dp)
                )
            }
        }
    }
}

@Composable
internal fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    errorText: String? = null,
    forceFocusedBorder: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    testTag: String
) {
    var visible by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val isFocused = focused || forceFocusedBorder
    val isError = errorText != null

    val borderWidth = if (isFocused && !isError) 2.dp else 1.dp
    val borderColor = when {
        isError -> FieldErrorColor
        isFocused -> Brand
        else -> OwnerBorder
    }
    val startPadding = if (borderWidth == 2.dp) 15.dp else 16.dp

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = ownerTextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Ink
            ),
            modifier = Modifier
                .padding(bottom = 8.dp)
                .cssLineBoxHeight(20.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceWhite)
                .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
                .padding(start = startPadding, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        style = ownerTextStyle(
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            color = MutedPlaceholderColor
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    singleLine = true,
                    textStyle = ownerTextStyle(
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Ink,
                        letterSpacing = if (!visible && value.isNotEmpty()) 0.12.em else 0.em
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = imeAction
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { onImeAction() },
                        onDone = { onImeAction() }
                    ),
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    interactionSource = interactionSource,
                    cursorBrush = SolidColor(if (isError) FieldErrorColor else Brand),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(testTag)
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        enabled = enabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = { visible = !visible }
                    )
                    .semantics {
                        contentDescription = if (visible) "Sembunyikan kata sandi" else "Tampilkan kata sandi"
                    },
                contentAlignment = Alignment.Center
            ) {
                OwnerVectorIcon(
                    imageVector = if (visible) OwnerIcons.EyeOff else OwnerIcons.Eye,
                    tint = Muted,
                    size = 20.dp
                )
            }
        }

        if (errorText != null) {
            Spacer(modifier = Modifier.height(8.dp))
            FieldErrorRow(message = errorText)
        }
    }
}

@Composable
private fun FieldErrorRow(message: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OwnerVectorIcon(
            imageVector = OwnerIcons.ErrorCircle,
            tint = FieldErrorColor,
            size = 16.dp
        )
        Text(
            text = message,
            style = ownerTextStyle(
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
                color = FieldErrorColor
            ),
            modifier = Modifier.cssLineBoxHeight(18.dp)
        )
    }
}

@Composable
internal fun PasswordChecklist(
    state: PasswordChecklistState,
    modifier: Modifier = Modifier,
    showCurrentRule: Boolean = true
) {
    val rows = buildList {
        add(state.hasMin8Chars to "Minimal 8 karakter")
        add(state.hasLetterAndDigit to "Ada huruf dan angka")
        if (showCurrentRule) {
            add(state.isDifferentFromCurrent to "Berbeda dari kata sandi saat ini")
        }
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        rows.forEach { (satisfied, text) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OwnerVectorIcon(
                    imageVector = if (satisfied) OwnerIcons.CheckCircle else OwnerIcons.EmptyCircle,
                    tint = if (satisfied) Brand else MutedPlaceholderColor,
                    size = 16.dp
                )
                Text(
                    text = text,
                    style = ownerTextStyle(
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (satisfied) Ink else Muted
                    ),
                    modifier = Modifier.cssLineBoxHeight(18.dp)
                )
            }
        }
    }
}

@Composable
private fun PinFlowModalContainer(
    isTablet: Boolean,
    isSaving: Boolean,
    errorMessage: String?,
    onInputChanged: () -> Unit,
    onDismiss: () -> Unit,
    onVerifyCurrentPin: ((String, (PinVerifyOutcome) -> Unit) -> Unit)?,
    onSubmitPinChange: (String, String, String) -> Unit,
    onForgotPinClick: () -> Unit,
    onForceLogout: (() -> Unit)?,
    requireCurrentCredential: Boolean = true
) {
    var step by remember(requireCurrentCredential) { mutableIntStateOf(if (requireCurrentCredential) 1 else 2) }
    var currentPin by remember { mutableStateOf("") }
    var verifiedOldPin by remember { mutableStateOf("") }
    var candidateNewPin by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var shakeCount by remember { mutableIntStateOf(0) }

    fun clearSensitivePinState() {
        currentPin = ""
        verifiedOldPin = ""
        candidateNewPin = ""
    }

    DisposableEffect(Unit) {
        onDispose { clearSensitivePinState() }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            clearSensitivePinState()
            step = if (requireCurrentCredential) 1 else 2
        }
    }

    val handleDismiss: () -> Unit = {
        clearSensitivePinState()
        onDismiss()
    }

    val handleBackStep: () -> Unit = {
        if (!isSaving) {
            localError = null
            currentPin = ""
            when (step) {
                3 -> {
                    candidateNewPin = ""
                    step = 2
                }
                2 -> if (requireCurrentCredential) {
                    verifiedOldPin = ""
                    candidateNewPin = ""
                    step = 1
                } else {
                    handleDismiss()
                }
                else -> handleDismiss()
            }
        }
    }

    BackHandler(enabled = !isSaving, onBack = handleBackStep)

    fun triggerPinError(message: String) {
        localError = message
        shakeCount++
        currentPin = ""
    }

    fun handleDigitPress(digit: String) {
        if (isSaving || currentPin.length >= MaxPinLength) return
        localError = null
        onInputChanged()
        val updated = currentPin + digit
        currentPin = updated
        if (updated.length == MaxPinLength) {
            when (step) {
                1 -> {
                    if (onVerifyCurrentPin != null) {
                        onVerifyCurrentPin(updated) { outcome ->
                            when (outcome) {
                                PinVerifyOutcome.Verified -> {
                                    verifiedOldPin = updated
                                    currentPin = ""
                                    localError = null
                                    step = 2
                                }
                                is PinVerifyOutcome.Rejected -> {
                                    val msg = if (outcome.isLocked || outcome.attemptsLeft <= 0) {
                                        outcome.message
                                    } else {
                                        "PIN salah. Sisa ${outcome.attemptsLeft} percobaan."
                                    }
                                    triggerPinError(msg)
                                }
                                is PinVerifyOutcome.ForceLogout -> {
                                    handleDismiss()
                                    onForceLogout?.invoke()
                                }
                                is PinVerifyOutcome.Error -> {
                                    triggerPinError(outcome.message)
                                }
                            }
                        }
                    } else {
                        verifiedOldPin = updated
                        currentPin = ""
                        step = 2
                    }
                }
                2 -> {
                    when {
                        isWeakPin(updated) -> triggerPinError("PIN terlalu mudah ditebak")
                        requireCurrentCredential && updated == verifiedOldPin -> triggerPinError("PIN baru harus berbeda")
                        else -> {
                            candidateNewPin = updated
                            currentPin = ""
                            localError = null
                            step = 3
                        }
                    }
                }
                3 -> {
                    if (updated != candidateNewPin) {
                        candidateNewPin = ""
                        step = 2
                        triggerPinError("PIN tidak sama. Ulangi dari awal.")
                    } else {
                        val oldPinSnapshot = verifiedOldPin
                        val newPinSnapshot = candidateNewPin
                        clearSensitivePinState()
                        onSubmitPinChange(oldPinSnapshot, newPinSnapshot, updated)
                    }
                }
            }
        }
    }

    AdaptiveSheet(
        title = "Ubah PIN transaksi",
        isTablet = isTablet,
        tabletHeight = if (requireCurrentCredential) 600.dp else 544.dp,
        dismissEnabled = !isSaving,
        onDismiss = handleDismiss,
        content = {
            PinModalContent(
                step = step,
                enteredLength = currentPin.length,
                errorMessage = localError ?: errorMessage,
                shakeTrigger = shakeCount,
                enabled = !isSaving,
                onDigitClick = ::handleDigitPress,
                onBackspaceClick = {
                    if (!isSaving && currentPin.isNotEmpty()) {
                        currentPin = currentPin.dropLast(1)
                        localError = null
                    }
                },
                onForgotPinClick = {
                    clearSensitivePinState()
                    onForgotPinClick()
                },
                totalSteps = if (requireCurrentCredential) 3 else 2,
                stepProgressValue = if (requireCurrentCredential) step else (step - 1),
                showForgotPin = requireCurrentCredential
            )
        }
    )
}

@Composable
internal fun StepProgress(
    currentStep: Int,
    totalSteps: Int = 3,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(totalSteps) { index ->
                val active = index < currentStep
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (active) Brand else OwnerBorder)
                )
            }
        }

        Text(
            text = "Langkah $currentStep dari $totalSteps",
            style = ownerTextStyle(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Muted
            ),
            modifier = Modifier.cssLineBoxHeight(16.dp)
        )
    }
}

@Composable
internal fun PinModalContent(
    step: Int,
    enteredLength: Int,
    errorMessage: String?,
    shakeTrigger: Int,
    enabled: Boolean,
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onForgotPinClick: () -> Unit,
    fillSpaceAboveLink: Boolean = false,
    totalSteps: Int = 3,
    stepProgressValue: Int = step,
    showForgotPin: Boolean = true
) {
    val (stepTitle, stepSubtitle) = when (step) {
        1 -> "Masukkan PIN saat ini" to "Untuk memastikan ini kamu."
        2 -> "Buat PIN baru" to "6 digit angka. Hindari angka berurutan atau sama."
        else -> "Ulangi PIN baru" to "Masukkan lagi PIN yang sama."
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        StepProgress(currentStep = stepProgressValue, totalSteps = totalSteps)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stepTitle,
            style = ownerTextStyle(
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.cssLineBoxHeight(20.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stepSubtitle,
            style = ownerTextStyle(
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
                color = Muted
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.cssLineBoxHeight(18.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        PinDots(
            pinLength = enteredLength,
            errorMessage = errorMessage,
            totalDigits = MaxPinLength,
            shakeTrigger = shakeTrigger,
            errorFontSize = 13.sp,
            errorLineHeight = 18.sp,
            errorTopSpacing = 10.dp
        )

        Spacer(modifier = Modifier.height(12.dp))

        PinKeypad(
            enabled = enabled,
            onDigitClick = onDigitClick,
            onBackspaceClick = onBackspaceClick,
            buttonHeight = 52.dp,
            spacing = 10.dp,
            digitFontSize = 20.sp,
            backspaceIconSize = 22.dp,
            enableHaptics = true
        )

        if (showForgotPin) {
            if (fillSpaceAboveLink) {
                Spacer(modifier = Modifier.weight(1f))
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }

            Box(
                modifier = Modifier
                    .height(44.dp)
                    .clickable(
                        enabled = enabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onForgotPinClick
                    )
                    .padding(horizontal = 12.dp)
                    .testTag("pin-forgot-link"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Lupa PIN?",
                    style = ownerTextStyle(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Brand
                    ),
                    modifier = Modifier.cssLineBoxHeight(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ResetPinViaPasswordSheet(
    isSaving: Boolean,
    errorMessage: String?,
    onInputChanged: () -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var currentPassword by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    fun clearSensitiveInputs() {
        currentPassword = ""
        newPin = ""
        confirmPin = ""
    }

    DisposableEffect(Unit) {
        onDispose { clearSensitiveInputs() }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            clearSensitiveInputs()
        }
    }

    val pinValid = CredentialValidator.isValidPin(newPin) && !isWeakPin(newPin)
    val isReady = currentPassword.isNotBlank() && pinValid && confirmPin == newPin && !isSaving

    fun submitAndClear() {
        val passwordSnapshot = currentPassword
        val newPinSnapshot = newPin
        val confirmPinSnapshot = confirmPin
        clearSensitiveInputs()
        onSubmit(passwordSnapshot, newPinSnapshot, confirmPinSnapshot)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= MODAL_TABLET_BREAKPOINT
        AdaptiveSheet(
            title = "Atur ulang PIN",
            isTablet = isTablet,
            tabletHeight = 520.dp,
            dismissEnabled = !isSaving,
            onDismiss = {
                clearSensitiveInputs()
                onDismiss()
            },
            sheetTestTag = "reset-pin-dialog",
            content = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    PasswordField(
                        label = "Kata sandi saat ini",
                        value = currentPassword,
                        onValueChange = {
                            currentPassword = it
                            onInputChanged()
                        },
                        enabled = !isSaving,
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                        testTag = "reset-pin-password"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PasswordField(
                        label = "PIN baru (4–6 digit)",
                        value = newPin,
                        onValueChange = {
                            newPin = it.filter(Char::isDigit).take(MaxPinLength)
                            onInputChanged()
                        },
                        enabled = !isSaving,
                        errorText = if (newPin.length >= CredentialValidator.PIN_MIN_LENGTH && isWeakPin(newPin)) {
                            "PIN terlalu mudah ditebak"
                        } else null,
                        imeAction = ImeAction.Next,
                        onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
                        testTag = "reset-pin-new"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    PasswordField(
                        label = "Ulangi PIN baru",
                        value = confirmPin,
                        onValueChange = {
                            confirmPin = it.filter(Char::isDigit).take(MaxPinLength)
                            onInputChanged()
                        },
                        enabled = !isSaving,
                        errorText = if (confirmPin.isNotEmpty() && confirmPin != newPin) {
                            "PIN tidak sama"
                        } else errorMessage,
                        imeAction = ImeAction.Done,
                        onImeAction = { if (isReady) submitAndClear() },
                        testTag = "reset-pin-confirm"
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isReady || isSaving) Brand else OwnerBorder)
                            .clickable(enabled = isReady, role = Role.Button) {
                                submitAndClear()
                            }
                            .testTag("reset-pin-save"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = OnBrand,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Simpan PIN baru",
                                style = ownerTextStyle(
                                    fontSize = 16.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isReady) OnBrand else MutedPlaceholderColor
                                ),
                                modifier = Modifier.cssLineBoxHeight(20.dp)
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
internal fun PasswordPopupReferenceFrame(
    isTablet: Boolean,
    modifier: Modifier = Modifier
) {
    val bgProfileState = sampleReferenceProfileState().copy(role = "OWNER")
    Box(modifier = modifier.fillMaxSize()) {
        DetailProfilScreen(
            state = bgProfileState,
            onBack = {},
            onCopyUserId = {},
            onEditProfileClick = {},
            onChangePasswordClick = {},
            onChangePinClick = {},
            onLogoutClick = {},
            onNavSelect = {}
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ModalScrimColor)
        ) {
            if (isTablet) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(480.dp)
                        .height(624.dp)
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = Color(0x3D14201B),
                            spotColor = Color(0x3D14201B)
                        )
                        .clip(RoundedCornerShape(28.dp))
                        .background(SurfaceWhite)
                        .padding(start = 32.dp, top = 16.dp, end = 32.dp, bottom = 32.dp)
                ) {
                    SheetHeaderRow(title = "Ubah kata sandi", dismissEnabled = true, onDismiss = {})
                    PasswordModalContent(
                        isSaving = false,
                        errorMessage = null,
                        onInputChanged = {},
                        onForgotPasswordClick = {},
                        onSubmit = { _, _, _ -> },
                        initialCurrent = "kasirlama1",
                        initialNew = "barokah25",
                        initialConfirm = "barokah2",
                        initialNewFocused = true,
                        fillSpaceAboveButton = true
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(648.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(SurfaceWhite)
                        .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 12.dp, bottom = 8.dp)
                            .size(width = 32.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ModalDragHandleColor)
                    )
                    SheetHeaderRow(title = "Ubah kata sandi", dismissEnabled = true, onDismiss = {})
                    PasswordModalContent(
                        isSaving = false,
                        errorMessage = null,
                        onInputChanged = {},
                        onForgotPasswordClick = {},
                        onSubmit = { _, _, _ -> },
                        initialCurrent = "kasirlama1",
                        initialNew = "barokah25",
                        initialConfirm = "barokah2",
                        initialNewFocused = true,
                        fillSpaceAboveButton = true
                    )
                }
            }
        }
    }
}

@Composable
internal fun PinPopupReferenceFrame(
    isTablet: Boolean,
    modifier: Modifier = Modifier
) {
    val bgProfileState = sampleReferenceProfileState().copy(role = "OWNER")
    Box(modifier = modifier.fillMaxSize()) {
        DetailProfilScreen(
            state = bgProfileState,
            onBack = {},
            onCopyUserId = {},
            onEditProfileClick = {},
            onChangePasswordClick = {},
            onChangePinClick = {},
            onLogoutClick = {},
            onNavSelect = {}
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ModalScrimColor)
        ) {
            if (isTablet) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(480.dp)
                        .height(640.dp)
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(28.dp),
                            ambientColor = Color(0x3D14201B),
                            spotColor = Color(0x3D14201B)
                        )
                        .clip(RoundedCornerShape(28.dp))
                        .background(SurfaceWhite)
                        .padding(start = 32.dp, top = 16.dp, end = 32.dp, bottom = 32.dp)
                ) {
                    SheetHeaderRow(title = "Ubah PIN transaksi", dismissEnabled = true, onDismiss = {})
                    PinModalContent(
                        step = 2,
                        enteredLength = 3,
                        errorMessage = null,
                        shakeTrigger = 0,
                        enabled = true,
                        onDigitClick = {},
                        onBackspaceClick = {},
                        onForgotPinClick = {},
                        fillSpaceAboveLink = true
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(668.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(SurfaceWhite)
                        .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 12.dp, bottom = 8.dp)
                            .size(width = 32.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ModalDragHandleColor)
                    )
                    SheetHeaderRow(title = "Ubah PIN transaksi", dismissEnabled = true, onDismiss = {})
                    PinModalContent(
                        step = 2,
                        enteredLength = 3,
                        errorMessage = null,
                        shakeTrigger = 0,
                        enabled = true,
                        onDigitClick = {},
                        onBackspaceClick = {},
                        onForgotPinClick = {},
                        fillSpaceAboveLink = true
                    )
                }
            }
        }
    }
}

@Preview(name = "PasswordPopupPhone", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
internal fun PasswordPopupPhonePreview() {
    TbterminalappTheme { PasswordPopupReferenceFrame(isTablet = false) }
}

@Preview(name = "PasswordPopupTablet", showBackground = true, widthDp = 1024, heightDp = 768)
@Composable
internal fun PasswordPopupTabletPreview() {
    TbterminalappTheme { PasswordPopupReferenceFrame(isTablet = true) }
}

@Preview(name = "PinPopupPhone", showBackground = true, widthDp = 390, heightDp = 844)
@Composable
internal fun PinPopupPhonePreview() {
    TbterminalappTheme { PinPopupReferenceFrame(isTablet = false) }
}

@Preview(name = "PinPopupTablet", showBackground = true, widthDp = 1024, heightDp = 768)
@Composable
internal fun PinPopupTabletPreview() {
    TbterminalappTheme { PinPopupReferenceFrame(isTablet = true) }
}
