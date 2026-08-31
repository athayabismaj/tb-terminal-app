package com.tbterminal.app.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.tbterminal.app.ui.theme.TbterminalappTheme

@Preview(name = "Login - Phone", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
fun PhoneLoginPreview() {
    AuthPreviewTheme { LoginPreviewContent() }
}

@Preview(name = "Login - Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
fun TabletLoginPreview() {
    AuthPreviewTheme { LoginPreviewContent() }
}

@Preview(name = "Login - Tablet Portrait", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
fun TabletPortraitLoginPreview() {
    AuthPreviewTheme { LoginPreviewContent() }
}

@Preview(name = "PIN - Phone", widthDp = 412, heightDp = 915, showBackground = true)
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
        username = "",
        password = "",
        authState = AuthState.Idle,
        onUsernameChange = {},
        onPasswordChange = {},
        onSubmit = {}
    )
}

@Composable
private fun PinPreviewContent() {
    PinContent(
        userName = "Pemilik Toko",
        pin = "12",
        unlockState = UnlockState.Idle,
        onDigitClick = {},
        onBackspaceClick = {},
        onSubmitClick = {},
        onBackToLogin = {}
    )
}
