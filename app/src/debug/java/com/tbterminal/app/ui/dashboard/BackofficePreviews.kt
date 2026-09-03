package com.tbterminal.app.ui.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.remote.DashboardMetricsDto
import com.tbterminal.app.data.remote.StockLowSummaryDto
import com.tbterminal.app.ui.dashboard.offline.OfflineDashboardUiState
import com.tbterminal.app.ui.theme.TbterminalappTheme

@Preview(name = "Owner - Phone", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
fun PhoneOwnerDashboardPreview() {
    BackofficePreview()
}

@Preview(name = "Owner - Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
fun TabletOwnerDashboardPreview() {
    BackofficePreview()
}

@Preview(name = "Owner Finance - Phone", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
fun PhoneOwnerFinancePreview() {
    BackofficeFinancePreview()
}

@Preview(name = "Owner Finance - Tablet", widthDp = 1280, heightDp = 800, showBackground = true)
@Composable
fun TabletOwnerFinancePreview() {
    BackofficeFinancePreview()
}

@Composable
private fun BackofficeFinancePreview() {
    TbterminalappTheme {
        BackofficeHubScreen(
            name = "Pemilik Toko",
            role = "owner",
            section = BackofficeSection.FINANCE,
            onNavigate = {},
            onLogout = {}
        )
    }
}

@Composable
private fun BackofficePreview() {
    TbterminalappTheme {
        var activeSection by rememberSaveable { mutableStateOf(BackofficeSection.HOME) }
        BackofficeAdaptiveShell(
            userName = "Pemilik Toko",
            role = "owner",
            activeSection = activeSection,
            onSectionSelected = { activeSection = it },
            onProfileClick = {},
            onLogout = {}
        ) { modifier ->
            if (activeSection == BackofficeSection.HOME) {
                BackofficeDashboardContent(
                    role = "OWNER",
                    metrics = DashboardMetricsDto(
                        totalRevenueToday = 4_850_000.0,
                        totalRevenueThisMonth = 86_400_000.0,
                        totalActiveReceivables = 7_250_000.0,
                        activeReceivableCount = 6,
                        lowStockCount = 3,
                        lowStockItems = listOf(
                            StockLowSummaryDto("preview-1", "BRG-001", "Beras Premium", 4.0, 10.0),
                            StockLowSummaryDto("preview-2", "BRG-002", "Minyak Goreng", 2.0, 8.0)
                        )
                    ),
                    isLoading = false,
                    error = null,
                    offlineUiState = OfflineDashboardUiState(
                        localRevenueToday = 1_250_000.0,
                        localCollectedToday = 1_000_000.0,
                        localReceivableOutstanding = 250_000.0,
                        localExpenseToday = 75_000.0,
                        localTransactionCount = 12,
                        pendingSyncCount = 2,
                        failedSyncCount = 0,
                        isLoading = false
                    ),
                    onNewTransactionClick = {},
                    onReceivablesClick = {},
                    onSupplierDebtsClick = {},
                    onCashClick = {},
                    onStockClick = {},
                    onTransactionsClick = {},
                    onReportsClick = {},
                    onSyncCenterClick = {},
                    showNewTransactionAction = false,
                    showOfflineDeviceSummary = false,
                    modifier = modifier
                )
            } else {
                PreviewSectionContent(section = activeSection, modifier = modifier)
            }
        }
    }
}

@Composable
private fun PreviewSectionContent(section: BackofficeSection, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(section.label, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Navbar aktif. Pada aplikasi normal, halaman ${section.label.lowercase()} akan dibuka.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
