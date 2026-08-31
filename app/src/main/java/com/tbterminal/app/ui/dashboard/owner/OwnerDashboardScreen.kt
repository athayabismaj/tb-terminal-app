package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.OfflineDashboardRepository
import com.tbterminal.app.data.repository.AnalyticsRepository
import com.tbterminal.app.ui.dashboard.BackofficeDashboardContent
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardViewModel
import com.tbterminal.app.ui.dashboard.DashboardBrandGreenDark
import com.tbterminal.app.ui.dashboard.DashboardDangerRed
import com.tbterminal.app.ui.dashboard.DashboardInfoBlue
import com.tbterminal.app.ui.dashboard.DashboardSurface
import com.tbterminal.app.ui.dashboard.DashboardTextPrimary
import com.tbterminal.app.ui.dashboard.DashboardTextSecondary
import com.tbterminal.app.ui.dashboard.DashboardWarningOrange
import com.tbterminal.app.ui.dashboard.offline.OfflineDashboardSection
import com.tbterminal.app.ui.dashboard.offline.OfflineDashboardViewModel

@Composable
fun OwnerDashboardScreen(
    name: String,
    role: String,
    analyticsRepository: AnalyticsRepository,
    offlineDashboardRepository: OfflineDashboardRepository,
    onLogout: () -> Unit,
    onReportsClick: () -> Unit = {},
    onLocalReportsClick: () -> Unit = onReportsClick,
    onStockReportClick: () -> Unit = {},
    onReceivablesClick: () -> Unit = {},
    onSupplierDebtsClick: () -> Unit = {},
    onCashReconciliationClick: () -> Unit = {},
    onSalesTransactionsClick: () -> Unit = {},
    onNewTransactionClick: () -> Unit = onSalesTransactionsClick,
    onProductsClick: () -> Unit = onStockReportClick,
    onOperationalAuditClick: () -> Unit = {},
    onSyncCenterClick: () -> Unit = {},
    onBackupRestoreClick: () -> Unit = onSyncCenterClick,
    onUserManagementClick: () -> Unit,
    onSecurityLogClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    dashboardViewModel: AdminDashboardViewModel = viewModel(
        factory = AdminDashboardViewModel.factory(analyticsRepository)
    ),
    offlineDashboardViewModel: OfflineDashboardViewModel = viewModel(
        factory = OfflineDashboardViewModel.factory(offlineDashboardRepository)
    )
) {
    val dashboardUiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val offlineUiState by offlineDashboardViewModel.uiState.collectAsStateWithLifecycle()

    OwnerDashboardShell(
        userName = name,
        role = role,
        activeDestination = OwnerDestination.Dashboard,
        onDashboardClick = {},
        onReportsClick = onReportsClick,
        onLocalReportsClick = onLocalReportsClick,
        onStockReportClick = onStockReportClick,
        onReceivablesClick = onReceivablesClick,
        onSupplierDebtsClick = onSupplierDebtsClick,
        onCashReconciliationClick = onCashReconciliationClick,
        onOperationalAuditClick = onOperationalAuditClick,
        onSyncCenterClick = onSyncCenterClick,
        onBackupRestoreClick = onBackupRestoreClick,
        onUserManagementClick = onUserManagementClick,
        onSecurityLogClick = onSecurityLogClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    ) { contentModifier ->
        BackofficeDashboardContent(
            metrics = dashboardUiState.metrics,
            isLoading = dashboardUiState.isLoading,
            error = dashboardUiState.error,
            offlineUiState = offlineUiState,
            onNewTransactionClick = onNewTransactionClick,
            onReceivablesClick = onReceivablesClick,
            onSupplierDebtsClick = onSupplierDebtsClick,
            onCashClick = onCashReconciliationClick,
            onStockClick = onProductsClick,
            onTransactionsClick = onSalesTransactionsClick,
            onSyncCenterClick = onSyncCenterClick,
            showNewTransactionAction = false,
            showOfflineDeviceSummary = false,
            modifier = contentModifier
        )
    }
}

@Composable
private fun OwnerDashboardContent(
    modifier: Modifier = Modifier,
    offlineUiState: com.tbterminal.app.ui.dashboard.offline.OfflineDashboardUiState,
    onSyncCenterClick: () -> Unit
) {
    Column(
        modifier = modifier
            .padding(32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Ringkasan TB Terminal",
                    color = DashboardTextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pantau kondisi utama operasional toko bahan bangunan Anda.",
                    color = DashboardTextSecondary,
                    fontSize = 14.sp
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(DashboardSurface)
                    .padding(2.dp)
            ) {
                DashboardFilterChip(text = "Hari Ini", isSelected = true)
                DashboardFilterChip(text = "Minggu Ini", isSelected = false)
                DashboardFilterChip(text = "Bulan Ini", isSelected = false)
            }
        }

        OfflineDashboardSection(
            uiState = offlineUiState,
            onSyncCenterClick = onSyncCenterClick,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        LazyRow(
            modifier = Modifier.padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DashboardMetricCard(
                    title = "PENDAPATAN HARIAN",
                    value = "Rp 14.5M",
                    subtitle = "+12.5%",
                    icon = Icons.Outlined.AccountBalanceWallet,
                    tint = DashboardBrandGreenDark
                )
            }
            item {
                DashboardMetricCard(
                    title = "PENDAPATAN BULANAN",
                    value = "Rp 412.8M",
                    subtitle = "TARGET 82%",
                    icon = Icons.Outlined.Assessment,
                    tint = DashboardInfoBlue
                )
            }
            item {
                DashboardMetricCard(
                    title = "PIUTANG AKTIF",
                    value = "Rp 89.2M",
                    subtitle = "12 Pelanggan Utama",
                    icon = Icons.Outlined.Group,
                    tint = Color.Gray
                )
            }
            item {
                DashboardMetricCard(
                    title = "UTANG PEMASOK",
                    value = "Rp 124.5M",
                    subtitle = "3 Invoice Lewat Jatuh Tempo",
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    tint = DashboardDangerRed
                )
            }
            item {
                DashboardMetricCard(
                    title = "STOK KRITIS",
                    value = "8 SKUs",
                    subtitle = "Perlu Tindakan",
                    icon = Icons.Outlined.Inventory,
                    tint = DashboardWarningOrange
                )
            }
            item {
                DashboardMetricCard(
                    title = "ARUS KAS HARIAN",
                    value = "Rp 5.2M",
                    subtitle = "Saldo Seimbang",
                    icon = Icons.Outlined.Payments,
                    tint = DashboardBrandGreenDark,
                    isAccent = true
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(2f)
                    .height(450.dp),
                colors = CardDefaults.cardColors(containerColor = DashboardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Performa Penjualan 7 Hari",
                        color = DashboardTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Analisis pendapatan dan perubahan pertumbuhan mingguan",
                        color = DashboardTextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Grafik akan ditampilkan di sini", color = DashboardTextSecondary)
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                PriorityAlertsCard()
                Spacer(modifier = Modifier.height(24.dp))
                SystemHealthCard()
            }
        }
    }
}

@Composable
private fun DashboardFilterChip(
    text: String,
    isSelected: Boolean
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) DashboardBrandGreenDark else Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else DashboardTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DashboardMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    isAccent: Boolean = false
) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .height(200.dp),
        colors = CardDefaults.cardColors(containerColor = DashboardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isAccent) 4.dp else 1.dp),
        border = if (isAccent) BorderStroke(2.dp, tint.copy(alpha = 0.5f)) else null,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint)
            }
            Column {
                Text(
                    text = title,
                    color = DashboardTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = value,
                    color = DashboardTextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (subtitle.contains("+")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = subtitle,
                        color = tint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun PriorityAlertsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DashboardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Peringatan Prioritas",
                    color = DashboardTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Badge(containerColor = DashboardDangerRed) {
                    Text(text = "5 BARU", color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            DashboardAlertItem(
                icon = Icons.Outlined.Warning,
                title = "Utang Pemasok Jatuh Tempo Hari Ini",
                description = "PT Sinar Bangunan: Rp 45.2M perlu segera dibayarkan.",
                tint = DashboardDangerRed
            )
            Spacer(modifier = Modifier.height(12.dp))
            DashboardAlertItem(
                icon = Icons.Outlined.Schedule,
                title = "Piutang Lewat Jatuh Tempo",
                description = "CV Perkasa Mulia: Rp 12.8M terlambat 3 hari kerja.",
                tint = DashboardInfoBlue
            )
        }
    }
}

@Composable
private fun SystemHealthCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DashboardBrandGreenDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "STATUS KESEHATAN SISTEM",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Operasional Optimal",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            LinearProgressIndicator(
                progress = { 0.88f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f)
            )
            Text(
                text = "Toko beroperasi pada efisiensi 88%. Logistik, penjualan, dan rantai pasok berada dalam kondisi baik.",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun DashboardAlertItem(
    icon: ImageVector,
    title: String,
    description: String,
    tint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.05f))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = DashboardTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = DashboardTextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
            Text(
                text = "TANDAI LUNAS",
                color = tint,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
