package com.tbterminal.app.ui.security

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.TabletAndroid
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.tbterminal.app.ui.dashboard.BackofficeAdaptiveShell
import com.tbterminal.app.ui.dashboard.BackofficeSection
import com.tbterminal.app.ui.theme.TbterminalappTheme

@Preview(name = "Log Keamanan - Phone", widthDp = 412, heightDp = 915, showBackground = true)
@Preview(name = "Log Keamanan - Phone Landscape", widthDp = 915, heightDp = 412, showBackground = true)
@Preview(name = "Log Keamanan - Font Besar", widthDp = 412, heightDp = 915, fontScale = 1.3f, showBackground = true)
@Preview(name = "Log Keamanan - Tablet Portrait", widthDp = 800, heightDp = 1280, showBackground = true)
@Preview(name = "Log Keamanan - Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
private fun SecurityLogPreview() {
    TbterminalappTheme {
        BackofficeAdaptiveShell(
            userName = "Pemilik Toko",
            role = "OWNER",
            activeSection = BackofficeSection.MENU,
            onSectionSelected = {},
            onProfileClick = {},
            onLogout = {},
            showPageHeader = false,
        ) { contentModifier ->
            SecurityLogContent(
                modifier = contentModifier,
                uiState = securityPreviewState(),
                onSearchQueryChange = {},
                onDateFilterChange = {},
                onActivityFilterChange = {},
                onRefresh = {},
                onPreviousPage = {},
                onNextPage = {},
                onPageClick = {},
                onViewLogDetail = {},
                onDismissLogDetail = {},
                onBack = {},
            )
        }
    }
}

private fun securityPreviewState() = SecurityLogUiState(
    logs = securityPreviewLogs(),
    visibleLogs = securityPreviewLogs(),
    totalLogs = 24,
    page = 1,
    totalPages = 3,
)

private fun securityPreviewLogs() = listOf(
    SecurityLogItem(
        id = "security-1",
        userName = "Pemilik Toko",
        userRole = "OWNER",
        type = SecurityLogType.Update,
        activityLabel = "Password pengguna diperbarui",
        ipAddress = "192.168.1.10",
        deviceIcon = Icons.Outlined.TabletAndroid,
        deviceName = "Client POS",
        time = "09:42",
        dateLabel = "09 Sep 2026",
        relativeTime = "5 menit lalu",
        compactRelativeTime = "5m lalu",
    ),
    SecurityLogItem(
        id = "security-2",
        userName = "Admin Toko",
        userRole = "ADMIN",
        type = SecurityLogType.Insert,
        activityLabel = "Pengguna baru ditambahkan",
        ipAddress = "192.168.1.12",
        deviceIcon = Icons.Outlined.TabletAndroid,
        deviceName = "Client POS",
        time = "09:30",
        dateLabel = "09 Sep 2026",
        relativeTime = "17 menit lalu",
        compactRelativeTime = "17m lalu",
    ),
)
