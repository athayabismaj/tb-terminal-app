package com.tbterminal.app.ui.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.BuildConfig

/**
 * Design tokens halaman login Terminal Barokah Jaya sesuai mockup modern minimalis.
 */
object LoginThemeTokens {
    val Background = Color(0xFFFFFFFF) // #FFFFFF bg-white Latar kanvas utama
    val CardBackground = Color(0xFFFFFFFF) // #FFFFFF

    val BrandGreen = Color(0xFF256B57) // #256B57 Sage Green Toko (CTA "Masuk" & Aksen Utama)
    val BrandGreenPressed = Color(0xFF1C5343) // #1C5343 Dark Sage (Hover / Pressed Tombol)
    val BrandGreenLight = Color(0xFFEDF7F2) // #EDF7F2 Tinted Sage Container (Kontainer Ikon Toko)
    val BrandGreenBorder = Color(0xFFEDF7F2) // #EDF7F2 Border Ikon Toko

    val Slate900 = Color(0xFF0F172A) // #0F172A Teks Utama (Judul "Barokah Jaya")
    val Slate700 = Color(0xFF334155) // #334155
    val Slate600 = Color(0xFF64748B) // #64748B Teks Sekunder & Label Input
    val Slate500 = Color(0xFF64748B) // #64748B Sub-teks "Sistem Kasir & Inventaris"
    val Slate400 = Color(0xFF94A3B8) // #94A3B8 Placeholder Input Text
    val Slate300 = Color(0xFFCBD5E1) // #CBD5E1 Border / Garis Tepi Input
    val Slate200 = Color(0xFFE2E8F0) // #E2E8F0

    val ErrorRed = Color(0xFFE11D48) // #E11D48 Rose-600 sesuai sample screenshot
    val ErrorBackground = Color(0xFFFFF1F2)
    val ErrorBorder = Color(0xFFFB91A0) // #FB91A0 Border halus saat error

    val RadiusCard = 16.dp
    val RadiusButton = 16.dp
    val RadiusBanner = 14.dp
}

/**
 * Header merek Barokah Jaya dengan lambang toko modern dan judul kasir & inventaris.
 */
@Composable
fun LoginBrandHeader(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = RoundedCornerShape(16.dp),
            color = LoginThemeTokens.BrandGreenLight, // #EDF7F2
            border = BorderStroke(1.dp, LoginThemeTokens.BrandGreenBorder)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Storefront,
                    contentDescription = null,
                    tint = LoginThemeTokens.BrandGreen, // #256B57
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Barokah Jaya",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = LoginThemeTokens.Slate900, // #0F172A
                lineHeight = 28.sp,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Sistem Kasir & Inventaris",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = LoginThemeTokens.Slate600 // #64748B
            )
        }
    }
}

/**
 * Input field bergaya notched outline dengan label melayang di atas garis border sesuai HTML mockup.
 */
@Composable
fun AuthInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordVisibilityToggle: (() -> Unit)? = null,
    errorMessage: String? = null,
    isError: Boolean = !errorMessage.isNullOrBlank(),
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    focusRequester: FocusRequester? = null,
    labelBackground: Color = LoginThemeTokens.Background,
    testTag: String? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    val localFocusRequester = remember { FocusRequester() }
    val effectiveFocusRequester = focusRequester ?: localFocusRequester
    val hasError = isError || !errorMessage.isNullOrBlank()

    val borderColor by animateColorAsState(
        targetValue = when {
            hasError -> LoginThemeTokens.ErrorBorder
            isFocused -> LoginThemeTokens.BrandGreen
            else -> LoginThemeTokens.Slate300
        },
        animationSpec = tween(durationMillis = 180),
        label = "field_border"
    )

    val labelColor by animateColorAsState(
        targetValue = when {
            hasError -> LoginThemeTokens.ErrorRed
            isFocused -> LoginThemeTokens.BrandGreen
            else -> LoginThemeTokens.Slate600
        },
        animationSpec = tween(durationMillis = 180),
        label = "field_label"
    )

    val iconColor by animateColorAsState(
        targetValue = when {
            hasError -> LoginThemeTokens.ErrorRed
            isFocused -> LoginThemeTokens.BrandGreen
            else -> LoginThemeTokens.Slate600 // #64748B Ikon dalam input
        },
        animationSpec = tween(durationMillis = 180),
        label = "field_icon"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 9.dp)
        ) {
            // Kontainer Kartu Input
            Surface(
                onClick = {
                    if (enabled) {
                        effectiveFocusRequester.requestFocus()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (testTag != null) Modifier.testTag("${testTag}-container") else Modifier),
                shape = RoundedCornerShape(LoginThemeTokens.RadiusCard),
                color = Color.White,
                border = BorderStroke(if (isFocused || hasError) 1.2.dp else 1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = LoginThemeTokens.Slate400 // #94A3B8 Placeholder
                            )
                        }
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
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = LoginThemeTokens.Slate900, // #0F172A
                                letterSpacing = if (isPassword && !passwordVisible && value.isNotEmpty()) 2.sp else 0.sp
                            ),
                            cursorBrush = SolidColor(if (hasError) LoginThemeTokens.ErrorRed else LoginThemeTokens.BrandGreen),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                                .focusRequester(effectiveFocusRequester)
                                .onFocusChanged { isFocused = it.isFocused }
                        )
                    }
                    if (isPassword && onPasswordVisibilityToggle != null) {
                        IconButton(
                            onClick = onPasswordVisibilityToggle,
                            enabled = enabled,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (passwordVisible) "Sembunyikan kata sandi" else "Tampilkan kata sandi",
                                tint = LoginThemeTokens.Slate400, // Tetap netral slate-400 sesuai screenshot
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Label melayang di atas garis border (notched badge)
            Box(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .align(Alignment.TopStart)
                    .offset(y = (-9).dp)
                    .background(labelBackground)
                    .padding(horizontal = 6.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = labelColor // #64748B default, #256B57 saat aktif, #E11D48 saat error
                )
            }
        }

        // Pesan Error Inline sesuai screenshot pengguna (Filled circle + teks merah)
        if (hasError && !errorMessage.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 2.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    modifier = Modifier.size(13.dp),
                    shape = CircleShape,
                    color = LoginThemeTokens.ErrorRed
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "!",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 9.sp
                        )
                    }
                }
                Text(
                    text = errorMessage,
                    fontSize = 12.sp,
                    color = LoginThemeTokens.ErrorRed,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Tombol utama Masuk dengan warna brand-600 #256B57 dan state ditekan #1C5343.
 */
@Composable
fun LoginPrimaryButton(
    text: String = "Masuk",
    onClick: () -> Unit,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
    testTag: String = "login-submit-button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.985f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "primary_button_press_scale"
    )

    val buttonColor by animateColorAsState(
        targetValue = if (isPressed) LoginThemeTokens.BrandGreenPressed else LoginThemeTokens.BrandGreen,
        animationSpec = tween(durationMillis = 150),
        label = "primary_button_color"
    )

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .testTag(testTag),
        shape = RoundedCornerShape(LoginThemeTokens.RadiusButton),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            contentColor = Color.White,
            disabledContainerColor = LoginThemeTokens.BrandGreen,
            disabledContentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 0.5.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                letterSpacing = 0.3.sp
            )
        }
    }
}

/**
 * Tombol sekunder sidik jari / biometrik.
 */
@Composable
fun LoginBiometricButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    testTag: String = "login-biometric-button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.985f else 1.0f,
        animationSpec = tween(durationMillis = 150),
        label = "biometric_button_press_scale"
    )

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .semantics { role = Role.Button }
            .testTag(testTag),
        shape = RoundedCornerShape(LoginThemeTokens.RadiusButton),
        color = Color.White,
        border = BorderStroke(1.dp, LoginThemeTokens.Slate300),
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Fingerprint,
                contentDescription = "Masuk dengan sidik jari",
                tint = LoginThemeTokens.BrandGreen,
                modifier = Modifier.size(21.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Masuk dengan sidik jari",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = LoginThemeTokens.Slate900
            )
        }
    }
}

/**
 * Banner pesan error dari server.
 */
@Composable
fun LoginErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
    testTag: String = "login-error-banner"
) {
    Surface(
        color = LoginThemeTokens.ErrorBackground,
        shape = RoundedCornerShape(LoginThemeTokens.RadiusBanner),
        border = BorderStroke(1.dp, LoginThemeTokens.ErrorBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = LoginThemeTokens.ErrorRed,
                modifier = Modifier.size(19.dp)
            )
            Text(
                text = message,
                fontSize = 13.sp,
                color = LoginThemeTokens.ErrorRed,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Dialog bantuan lupa kata sandi.
 */
@Composable
fun ForgotPasswordDialog(
    onDismiss: () -> Unit,
    onContactAdmin: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Lupa Kata Sandi?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = LoginThemeTokens.Slate900
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Untuk menjaga keamanan transaksi toko, pengaturan ulang kata sandi kasir dilakukan langsung oleh Owner atau Administrator toko Terminal Barokah Jaya.",
                    fontSize = 14.sp,
                    color = LoginThemeTokens.Slate600,
                    lineHeight = 20.sp
                )
                Text(
                    text = "Silakan hubungi admin toko untuk mendapatkan kredensial baru.",
                    fontSize = 13.sp,
                    color = LoginThemeTokens.Slate500,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            if (onContactAdmin != null) {
                Button(
                    onClick = {
                        onContactAdmin()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LoginThemeTokens.BrandGreen)
                ) {
                    Text("Hubungi Admin Toko", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Tutup", color = LoginThemeTokens.BrandGreen, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            if (onContactAdmin != null) {
                TextButton(onClick = onDismiss) {
                    Text("Tutup", color = LoginThemeTokens.Slate600)
                }
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

/**
 * Footer aplikasi minimalis sesuai HTML mockup.
 */
@Composable
fun LoginFooter(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Terminal Barokah Jaya",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = LoginThemeTokens.Slate600
        )
        Text(
            text = " · ",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = LoginThemeTokens.Slate400,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Text(
            text = "v${BuildConfig.VERSION_NAME}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = LoginThemeTokens.Slate600
        )
    }
}
