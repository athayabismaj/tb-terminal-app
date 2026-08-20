package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.offline.OfflineStatusIndicatorHost

private val SidebarBackground = Color(0xFFF4F8FA)
private val SidebarTextPrimary = Color(0xFF111111)
private val SidebarTextSecondary = Color(0xFF6B7378)
private val SidebarLine = Color(0xFFD8E0E4)
private val SidebarTeal = Color(0xFF008C86)
private val SidebarSelected = Color(0xFFE8F3F2)

@Composable
fun OwnerDashboardSidebar(
    onLogout: () -> Unit,
    activeDestination: OwnerDestination = OwnerDestination.Dashboard,
    onDashboardClick: () -> Unit = {},
    onReportsClick: () -> Unit = {},
    onLocalReportsClick: () -> Unit = onReportsClick,
    onSyncCenterClick: () -> Unit = {},
    onBackupRestoreClick: () -> Unit = {},
    onStockReportClick: () -> Unit = {},
    onReceivablesClick: () -> Unit = {},
    onSupplierDebtsClick: () -> Unit = {},
    onCashReconciliationClick: () -> Unit = {},
    onOperationalAuditClick: () -> Unit = {},
    onUserManagementClick: () -> Unit = {},
    onSecurityLogClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    userName: String = "owner",
    role: String = "owner",
    modifier: Modifier = Modifier
) {
    val isReportsActive = activeDestination in OwnerDestination.reportsGroup
    val isFinanceActive = activeDestination in OwnerDestination.financeGroup
    val isSecurityActive = activeDestination in OwnerDestination.securityGroup
    val isSystemActive = activeDestination in OwnerDestination.systemGroup
    var reportsExpanded by rememberSaveable { mutableStateOf(isReportsActive) }
    var financeExpanded by rememberSaveable { mutableStateOf(isFinanceActive) }
    var securityExpanded by rememberSaveable { mutableStateOf(isSecurityActive) }
    var systemExpanded by rememberSaveable { mutableStateOf(isSystemActive) }

    LaunchedEffect(activeDestination) {
        reportsExpanded = isReportsActive
        financeExpanded = isFinanceActive
        securityExpanded = isSecurityActive
        systemExpanded = isSystemActive
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(SidebarBackground)
    ) {
        OwnerSidebarBrand()
        Text(
            text = "PUSAT MANAJEMEN",
            color = SidebarTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 27.dp, top = 30.dp, bottom = 14.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
        ) {
            OwnerSidebarItem(
                title = "Dashboard",
                icon = Icons.Outlined.Home,
                selected = activeDestination == OwnerDestination.Dashboard,
                onClick = onDashboardClick
            )
            OwnerSidebarExpandableItem(
                title = "Laporan",
                icon = Icons.Outlined.Analytics,
                selected = isReportsActive,
                expanded = reportsExpanded,
                onClick = { reportsExpanded = !reportsExpanded }
            )
            if (reportsExpanded) {
                OwnerSidebarSubMenu(
                    items = listOf(
                        OwnerSidebarEntry("Laporan Analitik", Icons.Outlined.GridView, activeDestination == OwnerDestination.Reports, onReportsClick),
                        OwnerSidebarEntry("Laporan Lokal", Icons.Outlined.Assessment, activeDestination == OwnerDestination.LocalReports, onLocalReportsClick),
                        OwnerSidebarEntry("Sinkronisasi & Konflik", Icons.Outlined.Sync, activeDestination == OwnerDestination.SyncCenter, onSyncCenterClick),
                        OwnerSidebarEntry("Laporan Stok", Icons.Outlined.Inventory2, activeDestination == OwnerDestination.StockReport, onStockReportClick)
                    )
                )
            }

            OwnerSidebarExpandableItem(
                title = "Keuangan",
                icon = Icons.Outlined.AccountBalanceWallet,
                selected = isFinanceActive,
                expanded = financeExpanded,
                onClick = { financeExpanded = !financeExpanded }
            )
            if (financeExpanded) {
                OwnerSidebarSubMenu(
                    items = listOf(
                        OwnerSidebarEntry("Piutang Pelanggan", Icons.Outlined.Payments, activeDestination == OwnerDestination.Receivables, onReceivablesClick),
                        OwnerSidebarEntry("Utang Supplier", Icons.AutoMirrored.Outlined.ReceiptLong, activeDestination == OwnerDestination.SupplierDebts, onSupplierDebtsClick),
                        OwnerSidebarEntry("Kas Harian", Icons.Outlined.AccountBalanceWallet, activeDestination == OwnerDestination.CashReconciliation, onCashReconciliationClick)
                    )
                )
            }

            OwnerSidebarExpandableItem(
                title = "Audit & Keamanan",
                icon = Icons.Outlined.Security,
                selected = isSecurityActive,
                expanded = securityExpanded,
                onClick = { securityExpanded = !securityExpanded }
            )
            if (securityExpanded) {
                OwnerSidebarSubMenu(
                    items = listOf(
                        OwnerSidebarEntry("Audit Operasional", Icons.Outlined.AssignmentTurnedIn, activeDestination == OwnerDestination.OperationalAudit, onOperationalAuditClick),
                        OwnerSidebarEntry("Log Keamanan", Icons.Outlined.Security, activeDestination == OwnerDestination.SecurityLog, onSecurityLogClick)
                    )
                )
            }

            OwnerSidebarExpandableItem(
                title = "Sistem",
                icon = Icons.Outlined.Settings,
                selected = isSystemActive,
                expanded = systemExpanded,
                onClick = { systemExpanded = !systemExpanded }
            )
            if (systemExpanded) {
                OwnerSidebarSubMenu(
                    items = listOf(
                        OwnerSidebarEntry("Manajemen Pengguna", Icons.Outlined.Group, activeDestination == OwnerDestination.UserManagement, onUserManagementClick),
                        OwnerSidebarEntry("Backup & Restore", Icons.Outlined.Backup, activeDestination == OwnerDestination.BackupRestore, onBackupRestoreClick),
                        OwnerSidebarEntry("Pengaturan Sistem", Icons.Outlined.Settings, activeDestination == OwnerDestination.Settings, onSettingsClick)
                    )
                )
            }
        }

        OfflineStatusIndicatorHost(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
        HorizontalDivider(color = SidebarLine)
        OwnerSidebarProfileSection(
            userName = userName,
            role = role,
            onSettingsClick = onSettingsClick,
            onLogout = onLogout
        )
    }
}

enum class OwnerDestination {
    Dashboard,
    Reports,
    LocalReports,
    SyncCenter,
    BackupRestore,
    StockReport,
    Receivables,
    SupplierDebts,
    CashReconciliation,
    OperationalAudit,
    UserManagement,
    SecurityLog,
    Settings;

    companion object {
        val reportsGroup = setOf(Reports, LocalReports, SyncCenter, StockReport)
        val financeGroup = setOf(Receivables, SupplierDebts, CashReconciliation)
        val securityGroup = setOf(OperationalAudit, SecurityLog)
        val systemGroup = setOf(UserManagement, BackupRestore, Settings)
    }
}

private data class OwnerSidebarEntry(
    val title: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit
)

@Composable
private fun OwnerSidebarBrand() {
    Row(
        modifier = Modifier.padding(start = 27.dp, top = 28.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xFFE6ECEF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "TB",
                color = SidebarTextSecondary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(11.dp))
        Text(
            text = "Terminal",
            color = SidebarTextPrimary,
            fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun OwnerSidebarItem(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (selected) SidebarTeal else SidebarTextPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) SidebarSelected else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(13.dp))
        Text(title, color = contentColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun OwnerSidebarExpandableItem(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val contentColor = if (selected) SidebarTeal else SidebarTextPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) SidebarSelected else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(13.dp))
        Text(
            text = title,
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = null,
            tint = SidebarTextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun OwnerSidebarSubMenu(items: List<OwnerSidebarEntry>) {
    Row(modifier = Modifier.padding(start = 22.dp)) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp, bottom = 4.dp)
                .width(1.dp)
                .height((items.size * 40).dp)
                .background(SidebarLine)
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (item.selected) SidebarSelected else Color.Transparent)
                        .clickable(onClick = item.onClick)
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = if (item.selected) SidebarTeal else SidebarTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = item.title,
                        color = if (item.selected) SidebarTeal else SidebarTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun OwnerSidebarProfileSection(
    userName: String,
    role: String,
    onSettingsClick: () -> Unit,
    onLogout: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
            ) {
                OwnerSidebarProfileAction(Icons.Outlined.Settings, "Pengaturan", SidebarTextPrimary) {
                    expanded = false
                    onSettingsClick()
                }
                HorizontalDivider(color = SidebarLine)
                OwnerSidebarProfileAction(Icons.AutoMirrored.Outlined.Logout, "Keluar", Color(0xFFDC2626)) {
                    expanded = false
                    onLogout()
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE6ECEF)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.firstOrNull()?.uppercase() ?: "O",
                    color = SidebarTeal,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userName,
                    color = SidebarTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = role.uppercase(),
                    color = SidebarTextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = SidebarTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun OwnerSidebarProfileAction(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
