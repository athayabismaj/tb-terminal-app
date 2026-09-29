package com.tbterminal.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.tbterminal.app.data.model.UserProfile
import com.tbterminal.app.ui.dashboard.BackofficeAdaptiveShell
import com.tbterminal.app.ui.dashboard.BackofficeSection
import com.tbterminal.app.ui.theme.TbterminalappTheme

@Preview(name = "Profil - Phone", widthDp = 412, heightDp = 915, showBackground = true)
@Preview(name = "Profil - Phone Landscape", widthDp = 915, heightDp = 412, showBackground = true)
@Preview(name = "Profil - Font Besar", widthDp = 412, heightDp = 915, fontScale = 1.3f, showBackground = true)
@Composable
fun PhoneProfilePreview() {
    TbterminalappTheme {
        ProfilePreviewShell()
    }
}

@Preview(name = "Profil - Tablet Portrait", widthDp = 800, heightDp = 1280, showBackground = true)
@Preview(name = "Profil - Tablet", widthDp = 1024, heightDp = 768, showBackground = true)
@Composable
fun TabletProfilePreview() {
    TbterminalappTheme {
        ProfilePreviewShell()
    }
}

@Composable
private fun ProfilePreviewShell() {
    BackofficeAdaptiveShell(
        userName = "Pemilik Toko",
        role = "OWNER",
        activeSection = BackofficeSection.MENU,
        onSectionSelected = {},
        onProfileClick = {},
        onLogout = {},
        pageTitle = "Profil",
        onBack = {},
    ) { contentModifier ->
        SharedProfileScreen(
            uiState = ProfileUiState(profile = previewProfile()),
            onReload = {},
            onChangePassword = { _, _, _ -> },
            onChangePin = { _, _, _ -> },
            onClearMessage = {},
            onLogout = {},
            showHeader = false,
            modifier = contentModifier,
        )
    }
}

private fun previewProfile() = UserProfile(
    id = "owner-1",
    username = "pemilik",
    name = "Pemilik Toko",
    role = "OWNER",
    email = "pemilik@tbterminal.id",
    isActive = true,
    joinedAt = "2025-06-14T08:00:00",
    lastLoginAt = "2026-08-25T09:42:00",
)
