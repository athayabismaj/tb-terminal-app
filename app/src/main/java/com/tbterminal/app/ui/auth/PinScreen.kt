package com.tbterminal.app.ui.auth

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.BuildConfig
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.ChipBg
import com.tbterminal.app.ui.theme.FieldBorder
import com.tbterminal.app.ui.theme.FieldErrorColor
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.KawanSans
import com.tbterminal.app.ui.theme.KeyPressedBg
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.RadiusControl
import com.tbterminal.app.ui.theme.SurfaceWhite
import com.tbterminal.app.ui.theme.TbterminalappTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

internal const val MaxPinLength = 6

internal fun canSubmitPin(pin: String, isVerifying: Boolean): Boolean =
    pin.length == MaxPinLength && pin.all(Char::isDigit) && !isVerifying

internal fun buildPinUserInitials(userName: String): String {
    val parts = userName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val computed = when {
        parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}"
        parts.size == 1 && parts[0].length >= 2 -> parts[0].take(2)
        parts.size == 1 -> parts[0].take(1)
        else -> "PT"
    }
    return computed.uppercase()
}

@Immutable
data class PinUiState(
    val userName: String = "Pemilik Toko",
    val initials: String = "PT",
    val pin: String = "",
    val error: String? = null,
    val attemptsLeft: Int? = null,
    val isLockedOut: Boolean = false,
    val lockoutSecondsRemaining: Long = 0L,
    val isLoading: Boolean = false,
    val alert: LoginAlertState? = null
)

@Composable
fun PinScreen(
    viewModel: AuthViewModel,
    userName: String,
    onUnlockSuccess: () -> Unit,
    onBackToLogin: () -> Unit,
    onForgotPin: () -> Unit = {}
) {
    val unlockState by viewModel.unlockState.collectAsState()
    var pin by rememberSaveable { mutableStateOf("") }
    var lastSubmittedPin by rememberSaveable { mutableStateOf("") }
    var alertDismissed by remember { mutableStateOf(false) }

    val isVerifying = unlockState is UnlockState.Loading
    val lockedState = unlockState as? UnlockState.LockedOut
    val wrongPinState = unlockState as? UnlockState.WrongPin
    val isLockedOut = lockedState != null && lockedState.remainingSeconds > 0L
    val rawError = (unlockState as? UnlockState.Error)?.message
    val lowerError = rawError?.lowercase().orEmpty()
    val isNetworkError = rawError != null && (
        lowerError.contains("koneksi") || lowerError.contains("internet") ||
            lowerError.contains("network") || lowerError.contains("timeout") ||
            lowerError.contains("server tidak dapat dihubungi")
        )

    LaunchedEffect(unlockState) {
        if (unlockState !is UnlockState.Error) {
            alertDismissed = false
        }
        when (unlockState) {
            UnlockState.Success -> {
                onUnlockSuccess()
                viewModel.forceClearUnlockState()
            }
            is UnlockState.ForceLogout -> {
                pin = ""
                onBackToLogin()
            }
            is UnlockState.WrongPin -> {
                pin = ""
                delay(3000L)
                viewModel.resetUnlockState()
            }
            is UnlockState.Error -> {
                pin = ""
                if (!isNetworkError) {
                    delay(3000L)
                    viewModel.resetUnlockState()
                }
            }
            is UnlockState.LockedOut -> {
                if (pin.isNotEmpty()) {
                    pin = ""
                }
            }
            else -> Unit
        }
    }

    val displayName = userName.ifBlank { "Pemilik Toko" }
    val networkAlert = if (!alertDismissed && isNetworkError) {
        LoginAlertState(
            type = AlertType.Error,
            title = "Tidak dapat terhubung",
            message = "Periksa koneksi internet.",
            actionLabel = "Coba lagi"
        )
    } else null

    val pinFieldError = when {
        lockedState != null && isLockedOut ->
            formatPinLockoutMessage(lockedState.lockedUntilEpochMillis, lockedState.remainingSeconds)
        wrongPinState != null ->
            "PIN salah. Silakan coba lagi."
        rawError != null && !isNetworkError ->
            if (
                lowerError.contains("input tidak valid") ||
                (lowerError.contains("pin") && (
                    lowerError.contains("salah") ||
                        lowerError.contains("invalid") ||
                        lowerError.contains("tidak sesuai") ||
                        lowerError.contains("tidak valid")
                    ))
            ) {
                "PIN salah. Silakan coba lagi."
            } else {
                rawError
            }
        else -> null
    }

    PinScreen(
        state = PinUiState(
            userName = displayName,
            initials = buildPinUserInitials(displayName),
            pin = pin,
            error = pinFieldError,
            attemptsLeft = wrongPinState?.attemptsLeft,
            isLockedOut = isLockedOut,
            lockoutSecondsRemaining = lockedState?.remainingSeconds ?: 0L,
            isLoading = isVerifying,
            alert = networkAlert
        ),
        onDigit = { digit ->
            if (!isVerifying && !isLockedOut && pin.length < MaxPinLength) {
                if (unlockState is UnlockState.Error || unlockState is UnlockState.WrongPin) {
                    viewModel.resetUnlockState()
                }
                pin += digit
            }
        },
        onBackspace = {
            if (!isVerifying && !isLockedOut && pin.isNotEmpty()) {
                if (unlockState is UnlockState.Error || unlockState is UnlockState.WrongPin) {
                    viewModel.resetUnlockState()
                }
                pin = pin.dropLast(1)
            }
        },
        onComplete = { completedPin ->
            if (!isLockedOut && canSubmitPin(completedPin, isVerifying)) {
                lastSubmittedPin = completedPin
                viewModel.unlock(completedPin)
            }
        },
        onSwitchAccount = {
            viewModel.forceClearUnlockState()
            onBackToLogin()
        },
        onForgotPin = {
            viewModel.forceClearUnlockState()
            onForgotPin()
        },
        onRetry = {
            alertDismissed = false
            if (!isLockedOut && canSubmitPin(lastSubmittedPin, isVerifying)) {
                viewModel.unlock(lastSubmittedPin)
            } else {
                viewModel.resetUnlockState()
            }
        },
        onAlertDismiss = { alertDismissed = true }
    )
}

internal fun formatPinLockoutMessage(
    lockedUntilEpochMillis: Long,
    remainingSeconds: Long,
    zoneId: ZoneId = ZoneId.of("Asia/Jakarta")
): String {
    val countdown = formatPinLockoutCountdown(remainingSeconds)
    if (lockedUntilEpochMillis <= 0L) {
        return "Terlalu banyak percobaan · $countdown"
    }
    val timeText = Instant.ofEpochMilli(lockedUntilEpochMillis)
        .atZone(zoneId)
        .toLocalTime()
        .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    return "PIN terkunci hingga $timeText · $countdown"
}

internal fun formatPinLockoutCountdown(seconds: Long): String {
    val safeSeconds = seconds.coerceAtLeast(0L)
    val mins = safeSeconds / 60L
    val secs = safeSeconds % 60L
    return String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
}

@Composable
fun PinScreen(
    state: PinUiState,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onComplete: (String) -> Unit,
    onSwitchAccount: () -> Unit,
    onRetry: () -> Unit = {},
    onAlertDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
    onForgotPin: (() -> Unit)? = {}
) {
    val context = LocalContext.current
    if (!BuildConfig.DEBUG) {
        DisposableEffect(context) {
            val window = (context as? Activity)?.window
            window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            onDispose {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    val haptic = LocalHapticFeedback.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Brand)
    ) {
        val isTablet = maxWidth >= 600.dp

        if (isTablet) {
            TabletPinLayout(
                maxWidth = maxWidth,
                state = state,
                haptic = haptic,
                onDigit = onDigit,
                onBackspace = onBackspace,
                onComplete = onComplete,
                onSwitchAccount = onSwitchAccount,
                onForgotPin = onForgotPin
            )
        } else {
            PhonePinLayout(
                maxHeight = maxHeight,
                state = state,
                haptic = haptic,
                onDigit = onDigit,
                onBackspace = onBackspace,
                onComplete = onComplete,
                onSwitchAccount = onSwitchAccount,
                onForgotPin = onForgotPin
            )
        }

        // Gagal jaringan: AppAlert melayang di atas konten
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
                .widthIn(max = 390.dp)
                .padding(start = 16.dp, end = 16.dp, bottom = 72.dp)
        )
    }
}

@Composable
private fun PhonePinLayout(
    maxHeight: Dp,
    state: PinUiState,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onComplete: (String) -> Unit,
    onSwitchAccount: () -> Unit,
    onForgotPin: (() -> Unit)?
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val heroHeight = 188.dp
        val sheetMinHeight = (maxHeight - heroHeight).coerceAtLeast(0.dp)
        val sheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)

        Column(
            modifier = Modifier
                .widthIn(max = 390.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Area atas hijau: motif dekoratif + baris tombol kembali (<) & chip pengguna
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight)
                    .clipToBounds()
            ) {
                RingsPattern(
                    radii = listOf(48.dp, 92.dp, 136.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .wrapContentSize(align = Alignment.TopEnd, unbounded = true)
                        .offset(x = 80.dp, y = (-80).dp)
                        .size(280.dp)
                )

                DotsPattern(
                    dotRadius = 1.5.dp,
                    step = 20.dp,
                    maskAtBottomRight = true,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(width = 160.dp, height = 90.dp)
                )

                PinTopActionBar(
                    initials = state.initials,
                    userName = state.userName,
                    enabled = !state.isLoading,
                    onBackClick = onSwitchAccount,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(start = 20.dp, top = 16.dp, end = 20.dp)
                )
            }

            // Kontainer putih menyelubungi bulatan PIN, pesan error, Lupa PIN?, dan keypad
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = sheetMinHeight)
                    .clip(sheetShape)
                    .background(SurfaceWhite)
                    .navigationBarsPadding()
                    .padding(start = 28.dp, top = 32.dp, end = 28.dp, bottom = 44.dp)
                    .testTag("pin-keypad-sheet"),
                propagateMinConstraints = true
            ) {
                PinInputPanel(
                    state = state,
                    haptic = haptic,
                    onDigit = onDigit,
                    onBackspace = onBackspace,
                    onComplete = onComplete,
                    onForgotPin = onForgotPin
                )
            }
        }
    }
}

@Composable
private fun TabletPinLayout(
    maxWidth: Dp,
    state: PinUiState,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onComplete: (String) -> Unit,
    onSwitchAccount: () -> Unit,
    onForgotPin: (() -> Unit)?
) {
    val heroWidth = if (maxWidth >= 920.dp) 520.dp else (maxWidth * 0.45f)
    val sheetShape = RoundedCornerShape(topStart = 40.dp, bottomStart = 40.dp)

    Box(modifier = Modifier.fillMaxSize()) {
        // Panel hero hijau di kiri (selaras dengan TabletLoginLayout)
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

            PinTopActionBar(
                initials = state.initials,
                userName = state.userName,
                enabled = !state.isLoading,
                onBackClick = onSwitchAccount,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 28.dp, top = 24.dp, end = 64.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 64.dp, end = 76.dp),
                verticalArrangement = Arrangement.Center
            ) {
                BrandLogoMark(
                    containerSize = 72.dp,
                    cornerRadius = 20.dp,
                    iconSize = 36.dp
                )
                Spacer(modifier = Modifier.height(28.dp))
                HeroTitle(
                    text = "Barokah Jaya",
                    fontSize = 44.sp,
                    lineBoxHeight = (44f * 1.1f).dp
                )
                Spacer(modifier = Modifier.height(10.dp))
                HeroSubtitle(
                    text = "Sistem Kasir & Inventaris",
                    fontSize = 17.sp,
                    lineBoxHeight = (17f * 1.21f).dp
                )
            }
        }

        // Panel putih di kanan: margin kiri overlap 40dp, radius 40dp kiri atas & bawah
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = heroWidth - 40.dp)
                .clip(sheetShape)
                .background(SurfaceWhite)
                .navigationBarsPadding()
                .padding(start = 40.dp)
                .testTag("pin-keypad-sheet")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                PinInputPanel(
                    state = state,
                    haptic = haptic,
                    onDigit = onDigit,
                    onBackspace = onBackspace,
                    onComplete = onComplete,
                    onForgotPin = onForgotPin,
                    modifier = Modifier
                        .padding(horizontal = 28.dp, vertical = 40.dp)
                        .widthIn(max = 334.dp)
                )
            }
        }
    }
}

@Composable
private fun PinTopActionBar(
    initials: String,
    userName: String,
    enabled: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PinBackButton(
            enabled = enabled,
            onClick = onBackClick,
            modifier = Modifier.testTag("pin-back-button")
        )

        Spacer(modifier = Modifier.width(12.dp))

        PinUserChip(
            initials = initials,
            userName = userName,
            modifier = Modifier
                .weight(1f, fill = false)
                .testTag("pin-user-chip")
        )
    }
}

@Composable
private fun PinInputPanel(
    state: PinUiState,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onComplete: (String) -> Unit,
    onForgotPin: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PinDotsSection(
            pinLength = state.pin.length,
            errorMessage = state.error
        )

        if (onForgotPin != null) {
            Spacer(modifier = Modifier.height(6.dp))
            ForgotPinLink(
                enabled = !state.isLoading,
                onClick = onForgotPin
            )
            Spacer(modifier = Modifier.height(24.dp))
        } else {
            Spacer(modifier = Modifier.height(32.dp))
        }

        val keypadEnabled = !state.isLoading && !state.isLockedOut
        PinKeypadGrid(
            enabled = keypadEnabled,
            onDigitClick = { digit ->
                if (!keypadEnabled) return@PinKeypadGrid
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                val nextPin = if (state.pin.length < MaxPinLength) {
                    state.pin + digit
                } else {
                    state.pin
                }
                onDigit(digit)
                if (state.pin.length < MaxPinLength && nextPin.length == MaxPinLength) {
                    onComplete(nextPin)
                }
            },
            onBackspaceClick = {
                if (!keypadEnabled) return@PinKeypadGrid
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onBackspace()
            }
        )
    }
}

@Composable
private fun PinUserChip(
    initials: String,
    userName: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .padding(start = 6.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(SurfaceWhite),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                fontFamily = KawanSans,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = (12f * 1.21f).sp,
                color = Brand,
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.None
                    )
                )
            )
        }
        Text(
            text = userName,
            fontFamily = KawanSans,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = (14f * 1.21f).sp,
            color = OnBrand,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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

@Composable
private fun PinDotsSection(
    pinLength: Int,
    errorMessage: String?
) {
    PinDots(
        pinLength = pinLength,
        errorMessage = errorMessage
    )
}

@Composable
internal fun PinDots(
    pinLength: Int,
    errorMessage: String?,
    modifier: Modifier = Modifier,
    totalDigits: Int = MaxPinLength,
    shakeTrigger: Int = 0,
    errorFontSize: TextUnit = 12.sp,
    errorLineHeight: TextUnit = 16.sp,
    errorTopSpacing: Dp = 8.dp
) {
    val hasError = !errorMessage.isNullOrBlank()
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(errorMessage, shakeTrigger) {
        if (!errorMessage.isNullOrBlank() || shakeTrigger > 0) {
            shakeOffset.snapTo(0f)
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 300
                    0f at 0
                    (-8f) at 60
                    8f at 120
                    (-6f) at 180
                    6f at 240
                    0f at 300
                }
            )
        } else {
            shakeOffset.snapTo(0f)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .offset { IntOffset(shakeOffset.value.dp.roundToPx(), 0) }
                .semantics {
                    contentDescription = "$pinLength dari $totalDigits digit terisi"
                    liveRegion = LiveRegionMode.Polite
                }
                .testTag("pin-dots-row"),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(totalDigits) { index ->
                val filled = index < pinLength
                val dotModifier = when {
                    hasError -> Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(FieldErrorColor)
                    filled -> Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Brand)
                    else -> Modifier
                        .size(16.dp)
                        .border(width = 1.5.dp, color = FieldBorder, shape = CircleShape)
                }
                Box(modifier = dotModifier.testTag("pin-dot-$index"))
            }
        }

        Spacer(modifier = Modifier.height(errorTopSpacing))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            if (hasError) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { liveRegion = LiveRegionMode.Assertive }
                        .testTag("pin-error-text"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage!!,
                        fontFamily = KawanSans,
                        fontSize = errorFontSize,
                        fontWeight = FontWeight.Normal,
                        lineHeight = errorLineHeight,
                        color = FieldErrorColor,
                        textAlign = TextAlign.Center,
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
}

@Composable
private fun PinKeypadGrid(
    enabled: Boolean,
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit
) {
    PinKeypad(
        enabled = enabled,
        onDigitClick = onDigitClick,
        onBackspaceClick = onBackspaceClick,
        buttonHeight = 64.dp,
        spacing = 12.dp,
        enableHaptics = false
    )
}

@Composable
internal fun PinKeypad(
    enabled: Boolean,
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonHeight: Dp = 64.dp,
    spacing: Dp = 12.dp,
    digitFontSize: TextUnit = 24.sp,
    backspaceIconSize: Dp = 26.dp,
    enableHaptics: Boolean = false
) {
    val hapticFeedback = LocalHapticFeedback.current
    fun triggerDigit(digit: String) {
        if (enableHaptics) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        onDigitClick(digit)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        ).forEach { rowDigits ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                rowDigits.forEach { digit ->
                    PinKeyButton(
                        enabled = enabled,
                        buttonHeight = buttonHeight,
                        onClick = { triggerDigit(digit) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pin-key-$digit")
                    ) {
                        Text(
                            text = digit,
                            fontFamily = KawanSans,
                            fontSize = digitFontSize,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = (digitFontSize.value * 1.21f).sp,
                            color = Ink,
                            style = TextStyle(
                                fontFeatureSettings = "tnum",
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

        // Baris bawah: slot kosong, 0, hapus
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            PinKeyButton(
                enabled = enabled,
                buttonHeight = buttonHeight,
                onClick = { triggerDigit("0") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("pin-key-0")
            ) {
                Text(
                    text = "0",
                    fontFamily = KawanSans,
                    fontSize = digitFontSize,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = (digitFontSize.value * 1.21f).sp,
                    color = Ink,
                    style = TextStyle(
                        fontFeatureSettings = "tnum",
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.None
                        )
                    )
                )
            }
            PinKeyButton(
                enabled = enabled,
                buttonHeight = buttonHeight,
                onClick = {
                    if (enableHaptics) {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    onBackspaceClick()
                },
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Hapus digit" }
                    .testTag("pin-key-backspace")
            ) {
                PinBackspaceIcon(modifier = Modifier.size(backspaceIconSize))
            }
        }
    }
}

@Composable
private fun PinKeyButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonHeight: Dp = 64.dp,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val containerColor = if (isPressed) KeyPressedBg else ChipBg

    Box(
        modifier = modifier
            .height(buttonHeight)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(RadiusControl))
            .background(containerColor)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun PinBackButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(RadiusControl))
            .background(Color.White.copy(alpha = 0.14f))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { contentDescription = "Kembali" },
        contentAlignment = Alignment.Center
    ) {
        PinBackArrowIcon(modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun ForgotPinLink(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(RadiusControl))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 12.dp)
            .testTag("pin-forgot-pin"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Lupa PIN?",
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
            )
        )
    }
}

@Composable
private fun PinBackArrowIcon(modifier: Modifier = Modifier) {
    val chevronPath = remember { PathParser().parsePathString("M15 18l-6-6 6-6").toPath() }

    Canvas(modifier = modifier) {
        val scale = size.width / 24f
        val stroke = Stroke(
            width = 2.2f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        withTransform({ scale(scale, scale, Offset.Zero) }) {
            drawPath(path = chevronPath, color = OnBrand, style = stroke)
        }
    }
}

@Composable
private fun PinBackspaceIcon(modifier: Modifier = Modifier) {
    val bodyPath = remember {
        PathParser().parsePathString("M9 5h11a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H9l-6-7z").toPath()
    }
    val cross1Path = remember { PathParser().parsePathString("M12.5 9.5l5 5").toPath() }
    val cross2Path = remember { PathParser().parsePathString("M17.5 9.5l-5 5").toPath() }

    Canvas(modifier = modifier) {
        val scale = size.width / 24f
        val stroke = Stroke(
            width = 1.8f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        withTransform({ scale(scale, scale, Offset.Zero) }) {
            drawPath(path = bodyPath, color = Muted, style = stroke)
            drawPath(path = cross1Path, color = Muted, style = stroke)
            drawPath(path = cross2Path, color = Muted, style = stroke)
        }
    }
}

@Preview(name = "Barokah Jaya PIN - Kosong (390x844)", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun PinEmptyDesignPreview() {
    TbterminalappTheme {
        PinScreen(
            state = PinUiState(),
            onDigit = {},
            onBackspace = {},
            onComplete = {},
            onSwitchAccount = {}
        )
    }
}

@Preview(name = "Barokah Jaya PIN - 3 Digit Terisi (390x844)", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun PinFilled3DesignPreview() {
    TbterminalappTheme {
        PinScreen(
            state = PinUiState(pin = "123"),
            onDigit = {},
            onBackspace = {},
            onComplete = {},
            onSwitchAccount = {}
        )
    }
}

@Preview(name = "Barokah Jaya PIN - Tablet (1024x768)", widthDp = 1024, heightDp = 768, showBackground = true)
@Composable
fun PinTabletDesignPreview() {
    TbterminalappTheme {
        PinScreen(
            state = PinUiState(pin = "123"),
            onDigit = {},
            onBackspace = {},
            onComplete = {},
            onSwitchAccount = {}
        )
    }
}

@Preview(name = "Barokah Jaya PIN - Error PIN Salah (390x844)", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun PinErrorDesignPreview() {
    TbterminalappTheme {
        PinScreen(
            state = PinUiState(error = "PIN salah. Silakan coba lagi."),
            onDigit = {},
            onBackspace = {},
            onComplete = {},
            onSwitchAccount = {}
        )
    }
}

@Preview(name = "Barokah Jaya PIN - AppAlert Error (390x844)", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun PinAlertErrorDesignPreview() {
    TbterminalappTheme {
        PinScreen(
            state = PinUiState(
                alert = LoginAlertState(
                    type = AlertType.Error,
                    title = "Tidak dapat terhubung",
                    message = "Periksa koneksi internet.",
                    actionLabel = "Coba lagi"
                )
            ),
            onDigit = {},
            onBackspace = {},
            onComplete = {},
            onSwitchAccount = {}
        )
    }
}
