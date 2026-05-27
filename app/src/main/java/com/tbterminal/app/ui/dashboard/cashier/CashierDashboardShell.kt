package com.tbterminal.app.ui.dashboard.cashier

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.tbterminal.app.ui.dashboard.DashboardBackground
import com.tbterminal.app.ui.dashboard.DashboardBrandGreen
import com.tbterminal.app.ui.dashboard.DashboardBrandGreenDark
import com.tbterminal.app.ui.dashboard.DashboardSurface
import com.tbterminal.app.ui.dashboard.DashboardTextPrimary
import com.tbterminal.app.ui.dashboard.DashboardTextSecondary

enum class CashierDestination {
    Dashboard,
    Pos,
    CashSession,
    TransactionHistory,
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
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    showHeader: Boolean = true,
    content: @Composable (Modifier) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(DashboardBackground)
    ) {
        CashierShellSidebar(
            userName = userName,
            role = role,
            activeDestination = activeDestination,
            onDashboardClick = onDashboardClick,
            onPosClick = onPosClick,
            onCashSessionClick = onCashSessionClick,
            onTransactionHistoryClick = onTransactionHistoryClick,
            onStockCheckClick = onStockCheckClick,
            onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
            onLogout = onLogout,
            modifier = Modifier.width(260.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            content(Modifier.weight(1f))
        }
    }
}

@Composable
private fun CashierShellSidebar(
    userName: String,
    role: String,
    activeDestination: CashierDestination,
    onDashboardClick: () -> Unit,
    onPosClick: () -> Unit,
    onCashSessionClick: () -> Unit,
    onTransactionHistoryClick: () -> Unit,
    onStockCheckClick: () -> Unit,
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(DashboardSurface)
            .border(1.dp, Color.LightGray.copy(alpha = 0.3f))
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Text(
            text = "TB Terminal",
            color = DashboardBrandGreenDark,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 12.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "MENU UTAMA",
            color = DashboardTextSecondary.copy(alpha = 0.6f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        CashierShellSidebarItem(
            Icons.Outlined.GridView,
            "Dashboard",
            isActive = activeDestination == CashierDestination.Dashboard,
            onClick = onDashboardClick
        )
        CashierShellSidebarItem(
            Icons.Outlined.PointOfSale,
            "Mesin Kasir (POS)",
            isActive = activeDestination == CashierDestination.Pos,
            onClick = onPosClick
        )
        CashierShellSidebarItem(
            Icons.Outlined.Inventory2,
            "Cek Stok Barang",
            isActive = activeDestination == CashierDestination.StockCheck,
            onClick = onStockCheckClick
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "TRANSAKSI & KEUANGAN",
            color = DashboardTextSecondary.copy(alpha = 0.6f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        CashierShellSidebarItem(
            Icons.Outlined.Payments,
            "Kas Harian & Pengeluaran",
            isActive = activeDestination == CashierDestination.CashSession,
            onClick = onCashSessionClick
        )
        CashierShellSidebarItem(
            Icons.Outlined.History,
            "Riwayat & Pelunasan",
            isActive = activeDestination == CashierDestination.TransactionHistory,
            onClick = onTransactionHistoryClick
        )

        Spacer(modifier = Modifier.weight(1f))

        // ── Modern Profile Panel ──
        var isProfileExpanded by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxWidth()) {
            // Expandable menu (appears above the profile card)
            AnimatedVisibility(
                visible = isProfileExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                ) {
                    // Profile
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isProfileExpanded = false
                                onProfileClick()
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Person,
                            contentDescription = null,
                            tint = DashboardTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Profil",
                            color = DashboardTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    // Settings
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isProfileExpanded = false
                                onSettingsClick()
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = null,
                            tint = DashboardTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Pengaturan",
                            color = DashboardTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    // Logout
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isProfileExpanded = false
                                onLogout()
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Keluar",
                            color = Color(0xFFEF4444),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Profile Card (always visible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isProfileExpanded) DashboardBrandGreen.copy(alpha = 0.08f)
                        else Color.Transparent
                    )
                    .clickable { isProfileExpanded = !isProfileExpanded }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DashboardBrandGreen.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.firstOrNull()?.uppercase() ?: "K",
                        color = DashboardBrandGreenDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName,
                        color = DashboardTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = role,
                        color = DashboardTextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = if (isProfileExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    tint = DashboardTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun CashierShellSidebarItem(
    icon: ImageVector,
    text: String,
    isActive: Boolean = false,
    tint: Color = DashboardTextPrimary,
    onClick: () -> Unit
) {
    val contentColor = if (isActive) DashboardBrandGreenDark else tint
    val backgroundColor = if (isActive) DashboardBrandGreen.copy(alpha = 0.12f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = text, tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

