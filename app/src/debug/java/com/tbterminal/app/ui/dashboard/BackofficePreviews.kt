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
import java.math.BigDecimal

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
            onLogout = {},
            showPageHeader = false,
        ) { modifier ->
            if (activeSection == BackofficeSection.HOME) {
                BackofficeDashboardContent(
                    role = "OWNER",
                    metrics = DashboardMetricsDto(
                        totalRevenueToday = BigDecimal("4850000"),
                        totalRevenueThisMonth = BigDecimal("86400000"),
                        totalActiveReceivables = BigDecimal("7250000"),
                        activeReceivableCount = 6,
                        lowStockCount = 3,
                        lowStockItems = listOf(
                            StockLowSummaryDto("preview-1", "BRG-001", "Beras Premium", BigDecimal("4"), BigDecimal("10")),
                            StockLowSummaryDto("preview-2", "BRG-002", "Minyak Goreng", BigDecimal("2"), BigDecimal("8"))
                        )
                    ),
                    isLoading = false,
                    error = null,
                    offlineUiState = OfflineDashboardUiState(
                        localRevenueToday = BigDecimal("1250000"),
                        localCollectedToday = BigDecimal("1000000"),
                        localReceivableOutstanding = BigDecimal("250000"),
                        localExpenseToday = BigDecimal("75000"),
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
        val label = androidx.compose.ui.res.stringResource(section.labelRes)
        Text(label, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Navbar aktif. Pada aplikasi normal, halaman ${label.lowercase()} akan dibuka.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
