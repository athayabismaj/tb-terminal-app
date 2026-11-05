package com.tbterminal.app.ui.dashboard.cashier

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.tbterminal.app.ui.offline.OfflineStatusMiniDot
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator
import com.tbterminal.app.ui.theme.TbBackground
import com.tbterminal.app.ui.theme.TbGreen
import com.tbterminal.app.ui.theme.TbGreenLight
import com.tbterminal.app.ui.theme.TbSurface
private val CashierShellBackground = TbBackground
private val CashierSidebarBackground = TbSurface
private val CashierSidebarTextPrimary = Color(0xFF111111)
private val CashierSidebarTextSecondary = Color(0xFF6B7378)
private val CashierSidebarLine = Color(0xFFD8E0E4)
private val CashierSidebarTeal = TbGreen
private val CashierSidebarSelected = TbGreenLight
private val CashierSidebarDanger = Color(0xFFDC2626)
private val CashierSidebarDangerSoft = Color(0xFFFFF1F2)
private val CashierSidebarActionIconBg = Color(0xFFEAF1F3)
private val CashierExpandedBreakpoint = 840.dp

enum class CashierDestination {
    Dashboard,
    Pos,
    CashSession,
    TransactionHistory,
    Customers,
    Receivables,
    ReceivablePayments,
    StockCheck,
    Profile,
    Settings
}

@Composable
fun CashierDashboardShell(
    userName: String,
    role: String,
    activeDestination: CashierDestination,
    onDashboardClick: () -> Unit,
    onPosClick: () -> Unit,
    onCashSessionClick: () -> Unit = {},
    onTransactionHistoryClick: () -> Unit = {},
    onStockCheckClick: () -> Unit = {},
    onReceivablesClick: (() -> Unit)? = null,
    onCustomersClick: (() -> Unit)? = null,
    onReceivablePaymentsClick: (() -> Unit)? = null,
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    showHeader: Boolean = true,
    content: @Composable (Modifier) -> Unit
) {
    val adminNavigator = LocalAdminDestinationNavigator.current
    val openReceivables: () -> Unit = onReceivablesClick ?: {
        adminNavigator?.invoke(AdminDestination.Receivables)
        Unit
    }
    val openCustomers: () -> Unit = onCustomersClick ?: {
        adminNavigator?.invoke(AdminDestination.Customers)
        Unit
    }
    val openReceivablePayments: () -> Unit = onReceivablePaymentsClick ?: {
        adminNavigator?.invoke(AdminDestination.ReceivablePayments)
        Unit
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(CashierShellBackground)
    ) {
        val isExpandedScreen = maxWidth >= CashierExpandedBreakpoint

        if (isExpandedScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                CashierShellSidebar(
                    userName = userName,
                    role = role,
                    activeDestination = activeDestination,
                    onDashboardClick = onDashboardClick,
                    onPosClick = onPosClick,
                    onCashSessionClick = onCashSessionClick,
                    onTransactionHistoryClick = onTransactionHistoryClick,
                    onCustomersClick = openCustomers,
                    onReceivablesClick = openReceivables,
                    onReceivablePaymentsClick = openReceivablePayments,
                    onProfileClick = onProfileClick,
                    onSettingsClick = onSettingsClick,
                    onLogout = onLogout,
                    modifier = Modifier.width(266.dp)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    content(Modifier.weight(1f))
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                if (showHeader) {
                    CashierCompactTopBar(
                        userName = userName,
                        role = role,
                        onProfileClick = onProfileClick,
                        onSettingsClick = onSettingsClick,
                        onLogout = onLogout
                    )
                }
                content(Modifier.weight(1f).then(if (showHeader) Modifier else Modifier.statusBarsPadding()))
                OfflineStatusIndicatorHost(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
                CashierBottomNavigation(
                    activeDestination = activeDestination,
                    onDashboardClick = onDashboardClick,
                    onPosClick = onPosClick,
                    onCashSessionClick = onCashSessionClick,
                    onTransactionHistoryClick = onTransactionHistoryClick,
                    onReceivablesClick = openReceivables
                )
            }
        }
    }
}

@Composable
private fun CashierCompactTopBar(
    userName: String,
    role: String,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogout: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val displayName = remember(userName) {
        val trimmed = userName.trim()
        if (trimmed.isEmpty() || trimmed.equals("kasir", ignoreCase = true)) {
            "Kasir"
        } else {
            trimmed
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFEEF7F2))
                    .border(1.dp, Color(0xFFD1FAE5), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "TB",
                    color = Color(0xFF1B5E3C),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Terminal Barokah Jaya",
                    color = Color(0xFF0F172A),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = (-0.3).sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "TERMINAL KASIR",
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    )
                    OfflineStatusMiniDot()
                }
            }

            Surface(
                onClick = { expanded = !expanded },
                shape = RoundedCornerShape(999.dp),
                color = Color.White,
                border = BorderStroke(1.dp, if (expanded) Color(0xFF1B5E3C) else Color(0xFFE2E8F0)),
                shadowElevation = 0.5.dp
            ) {
                Row(
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp, end = 10.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEEF7F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayName.firstOrNull()?.uppercase() ?: "K",
                            color = Color(0xFF1B5E3C),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = displayName.take(12),
                        color = Color(0xFF1E293B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = "Menu Profil",
                        tint = if (expanded) Color(0xFF1B5E3C) else Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        if (expanded) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEEF7F2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayName.firstOrNull()?.uppercase() ?: "K",
                                color = Color(0xFF1B5E3C),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayName,
                                color = Color(0xFF0F172A),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "KASIR · TERMINAL BAROKAH JAYA",
                                color = Color(0xFF64748B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    CashierSidebarProfileAction(
                        icon = Icons.Outlined.Person,
                        label = "Profil Saya",
                        color = Color(0xFF1E293B)
                    ) {
                        expanded = false
                        onProfileClick()
                    }
                    CashierSidebarProfileAction(
                        icon = Icons.Outlined.Settings,
                        label = "Pengaturan",
                        color = Color(0xFF1E293B)
                    ) {
                        expanded = false
                        onSettingsClick()
                    }
                    CashierSidebarProfileAction(
                        icon = Icons.AutoMirrored.Outlined.Logout,
                        label = "Keluar dari Akun",
                        color = Color(0xFFDC2626),
                        iconBackground = Color(0xFFFFF1F2)
                    ) {
                        expanded = false
                        onLogout()
                    }
                }
            }
        }
        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
    }
}
@Composable
private fun CashierBottomNavigation(
    activeDestination: CashierDestination,
    onDashboardClick: () -> Unit,
    onPosClick: () -> Unit,
    onCashSessionClick: () -> Unit,
    onTransactionHistoryClick: () -> Unit,
    onReceivablesClick: () -> Unit
) {
    val items = listOf(
        CashierBottomNavItem(
            destination = CashierDestination.Dashboard,
            label = "Dashboard",
            icon = Icons.Outlined.Home,
            onClick = onDashboardClick
        ),
        CashierBottomNavItem(
            destination = CashierDestination.Pos,
            label = "POS",
            icon = Icons.Outlined.PointOfSale,
            onClick = onPosClick
        ),
        CashierBottomNavItem(
            destination = CashierDestination.CashSession,
            label = "Kas",
            icon = Icons.Outlined.Payments,
            onClick = onCashSessionClick
        ),
        CashierBottomNavItem(
            destination = CashierDestination.TransactionHistory,
            label = "Riwayat",
            icon = Icons.Outlined.History,
            onClick = onTransactionHistoryClick
        ),
        CashierBottomNavItem(
            destination = CashierDestination.Receivables,
            label = "Piutang",
            icon = Icons.Outlined.AccountBalanceWallet,
            onClick = onReceivablesClick
        )
    )

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        items.forEach { item ->
            val selected = activeDestination == item.destination ||
                (item.destination == CashierDestination.Receivables && activeDestination in setOf(
                    CashierDestination.Customers,
                    CashierDestination.ReceivablePayments,
                ))
            NavigationBarItem(
                selected = selected,
                onClick = item.onClick,
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(21.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CashierSidebarTeal,
                    selectedTextColor = CashierSidebarTeal,
                    indicatorColor = CashierSidebarSelected,
                    unselectedIconColor = CashierSidebarTextSecondary,
                    unselectedTextColor = CashierSidebarTextSecondary
                )
            )
        }
    }
}

private data class CashierBottomNavItem(
    val destination: CashierDestination,
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun CashierShellSidebar(
    userName: String,
    role: String,
    activeDestination: CashierDestination,
    onDashboardClick: () -> Unit,
    onPosClick: () -> Unit,
    onCashSessionClick: () -> Unit,
    onTransactionHistoryClick: () -> Unit,
    onCustomersClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onReceivablePaymentsClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(CashierSidebarBackground)
    ) {
        CashierSidebarBrand()
        Text(
            text = "TERMINAL KASIR",
            color = CashierSidebarTextSecondary,
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
            CashierShellSidebarItem(
                icon = Icons.Outlined.Home,
                text = "Dashboard",
                isActive = activeDestination == CashierDestination.Dashboard,
                onClick = onDashboardClick
            )
            CashierShellSidebarItem(
                icon = Icons.Outlined.PointOfSale,
                text = "POS",
                isActive = activeDestination == CashierDestination.Pos,
                onClick = onPosClick
            )
            CashierShellSidebarItem(
                icon = Icons.Outlined.Payments,
                text = "Kas Harian",
                isActive = activeDestination == CashierDestination.CashSession,
                onClick = onCashSessionClick
            )
            CashierShellSidebarItem(
                icon = Icons.Outlined.History,
                text = "Transaksi Saya",
                isActive = activeDestination == CashierDestination.TransactionHistory,
                onClick = onTransactionHistoryClick
            )
            CashierShellSidebarItem(
                icon = Icons.Outlined.Person,
                text = "Pelanggan",
                isActive = activeDestination == CashierDestination.Customers,
                onClick = onCustomersClick
            )
            CashierShellSidebarItem(
                icon = Icons.Outlined.AccountBalanceWallet,
                text = "Piutang Pelanggan",
                isActive = activeDestination == CashierDestination.Receivables,
                onClick = onReceivablesClick
            )
            CashierShellSidebarItem(
                icon = Icons.Outlined.Payments,
                text = "Pembayaran Piutang",
                isActive = activeDestination == CashierDestination.ReceivablePayments,
                onClick = onReceivablePaymentsClick
            )
        }
        OfflineStatusIndicatorHost(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
        )
        HorizontalDivider(color = CashierSidebarLine)
        CashierSidebarProfileSection(
            userName = userName,
            role = role,
            onProfileClick = onProfileClick,
            onSettingsClick = onSettingsClick,
            onLogout = onLogout
        )
    }
}

@Composable
private fun CashierSidebarBrand() {
    Row(
        modifier = Modifier.padding(start = 22.dp, top = 24.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFEEF7F2))
                .border(1.dp, Color(0xFFD1FAE5), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "TB",
                color = Color(0xFF1B5E3C),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        Spacer(modifier = Modifier.width(11.dp))
        Column {
            Text(
                text = "Terminal Barokah Jaya",
                color = Color(0xFF0F172A),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "TERMINAL KASIR",
                color = Color(0xFF64748B),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
@Composable
private fun CashierShellSidebarItem(
    icon: ImageVector,
    text: String,
    isActive: Boolean = false,
    tint: Color = CashierSidebarTextPrimary,
    onClick: () -> Unit
) {
    val contentColor = if (isActive) CashierSidebarTeal else tint

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) CashierSidebarSelected else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = text, tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(13.dp))
        Text(
            text = text,
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CashierSidebarProfileSection(
    userName: String,
    role: String,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogout: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val displayName = remember(userName) {
        val trimmed = userName.trim()
        if (trimmed.isEmpty() || trimmed.equals("kasir", ignoreCase = true)) {
            "Kasir"
        } else {
            trimmed
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (expanded) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier.padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    CashierSidebarProfileAction(
                        Icons.Outlined.Person,
                        "Profil Saya",
                        Color(0xFF1E293B)
                    ) {
                        expanded = false
                        onProfileClick()
                    }
                    CashierSidebarProfileAction(
                        Icons.Outlined.Settings,
                        "Pengaturan",
                        Color(0xFF1E293B)
                    ) {
                        expanded = false
                        onSettingsClick()
                    }
                    CashierSidebarProfileAction(
                        icon = Icons.AutoMirrored.Outlined.Logout,
                        label = "Keluar dari Akun",
                        color = Color(0xFFDC2626),
                        iconBackground = Color(0xFFFFF1F2)
                    ) {
                        expanded = false
                        onLogout()
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEEF7F2))
                    .border(1.dp, Color(0xFFD1FAE5), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.firstOrNull()?.uppercase() ?: "K",
                    color = Color(0xFF1B5E3C),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    color = Color(0xFF0F172A),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "KASIR",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = if (expanded) Color(0xFF1B5E3C) else Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
@Composable
private fun CashierSidebarProfileAction(
    icon: ImageVector,
    label: String,
    color: Color,
    iconBackground: Color = CashierSidebarActionIconBg,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
