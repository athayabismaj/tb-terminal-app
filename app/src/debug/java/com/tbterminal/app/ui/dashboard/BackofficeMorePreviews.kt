package com.tbterminal.app.ui.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.tbterminal.app.ui.theme.TbterminalappTheme
import com.tbterminal.app.ui.settings.SharedProfileScreen
import com.tbterminal.app.ui.settings.ProfileUiState
import com.tbterminal.app.data.model.UserProfile
import com.tbterminal.app.ui.components.TbPageSurface
import com.tbterminal.app.ui.dashboard.owner.OwnerMenuContent

@Preview(name = "Owner Menu - Phone", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Owner Menu - Phone Large Text", widthDp = 360, heightDp = 800, fontScale = 1.5f, showBackground = true)
@Preview(name = "Owner Menu - Tablet Portrait", widthDp = 800, heightDp = 1280, showBackground = true)
@Preview(name = "Owner Menu - Tablet Landscape", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
fun OwnerMenuPreview() {
    TbterminalappTheme {
        BackofficeAdaptiveShell(
            userName = "Pemilik Toko", role = "OWNER", activeSection = BackofficeSection.MENU,
            onSectionSelected = {}, onProfileClick = {}, onLogout = {},
        ) { modifier ->
            TbPageSurface(modifier = modifier, maxContentWidth = 1120.dp) { layout, pageModifier ->
                OwnerMenuContent(
                    layout = layout, sessionName = "Pemilik Toko", role = "OWNER",
                    profile = previewProfile, onProfileClick = {}, onNavigate = {},
                    modifier = pageModifier.verticalScroll(rememberScrollState()),
                )
            }
        }
    }
}

@Preview(name = "Owner Profile - Phone", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Owner Profile - Phone Large Text", widthDp = 360, heightDp = 800, fontScale = 1.5f, showBackground = true)
@Preview(name = "Owner Profile - Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
fun OwnerAccountPreview() {
    TbterminalappTheme {
        BackofficeAdaptiveShell(
            userName = "Pemilik Toko", role = "OWNER", activeSection = BackofficeSection.MENU,
            onSectionSelected = {}, onProfileClick = {}, onLogout = {}, pageTitle = "Profil", onBack = {},
        ) { modifier ->
            SharedProfileScreen(
                uiState = ProfileUiState(isLoading = false, profile = previewProfile),
                onReload = {}, onChangePassword = { _, _, _ -> }, onChangePin = { _, _, _ -> },
                onClearMessage = {}, showHeader = false, onLogout = {}, modifier = modifier,
            )
        }
    }
}

private val previewProfile = UserProfile(
    "preview", "pemilik", "Pemilik Toko", "OWNER", null, true, "2026-01-01", null,
)
