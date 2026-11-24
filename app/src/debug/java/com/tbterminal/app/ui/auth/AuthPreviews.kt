package com.tbterminal.app.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.tbterminal.app.ui.theme.TbterminalappTheme

@Preview(name = "Login - Phone", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun PhoneLoginPreview() {
    AuthPreviewTheme { LoginPreviewContent() }
}

@Preview(name = "Login - Tablet", widthDp = 1024, heightDp = 768, showBackground = true)
@Composable
fun TabletLoginPreview() {
    AuthPreviewTheme { LoginPreviewContent() }
}

@Preview(name = "Login - Phone Alert Error", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun PhoneAlertLoginPreview() {
    AuthPreviewTheme {
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

@Preview(name = "Login - Phone Keyboard Open", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun PhoneKeyboardLoginPreview() {
    AuthPreviewTheme {
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

@Preview(name = "Login - Tablet Portrait", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun TabletPortraitLoginPreview() {
    AuthPreviewTheme { LoginPreviewContent() }
}

@Preview(name = "PIN - Phone", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun PhonePinPreview() {
    AuthPreviewTheme { PinPreviewContent() }
}

@Preview(name = "PIN - Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
fun TabletPinPreview() {
    AuthPreviewTheme { PinPreviewContent() }
}

@Preview(name = "PIN - Tablet Portrait", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun TabletPortraitPinPreview() {
    AuthPreviewTheme { PinPreviewContent() }
}

@Composable
private fun AuthPreviewTheme(content: @Composable () -> Unit) {
    TbterminalappTheme { content() }
}

@Composable
private fun LoginPreviewContent() {
    LoginContent(
        state = LoginUiState(initialPassword = "katasandi"),
        imeVisible = false,
        onLogin = { _, _ -> },
        onForgot = {},
        onRetry = {}
    )
}

@Composable
private fun PinPreviewContent() {
    PinScreen(
        state = PinUiState(pin = "123"),
        onDigit = {},
        onBackspace = {},
        onComplete = {},
        onSwitchAccount = {}
    )
}
