package com.tbterminal.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tbterminal.app.data.model.UserProfile
import com.tbterminal.app.ui.theme.TbterminalappTheme

@Preview(name = "Profil - Phone", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
fun PhoneProfilePreview() {
    TbterminalappTheme {
        SharedProfileScreen(
            uiState = ProfileUiState(
                isLoading = false,
                profile = UserProfile(
                    id = "owner-1",
                    username = "pemilik",
                    name = "Pemilik Toko",
                    role = "OWNER",
                    email = "pemilik@tbterminal.id",
                    isActive = true,
                    joinedAt = "2025-06-14T08:00:00",
                    lastLoginAt = "2026-08-25T09:42:00"
                )
            ),
            onReload = {},
            onChangePassword = { _, _, _ -> },
            onChangePin = { _, _, _ -> },
            onClearMessage = {},
            modifier = Modifier
        )
    }
}
