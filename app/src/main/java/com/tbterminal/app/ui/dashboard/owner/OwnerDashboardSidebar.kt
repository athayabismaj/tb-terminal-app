package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.dashboard.DashboardBrandGreen
import com.tbterminal.app.ui.dashboard.DashboardBrandGreenDark
import com.tbterminal.app.ui.dashboard.DashboardSurface
import com.tbterminal.app.ui.dashboard.DashboardTextPrimary
import com.tbterminal.app.ui.dashboard.DashboardTextSecondary

@Composable
fun OwnerDashboardSidebar(
    onLogout: () -> Unit,
    activeDestination: OwnerDestination = OwnerDestination.Dashboard,
    onDashboardClick: () -> Unit = {},
    onUserManagementClick: () -> Unit = {},
    onSecurityLogClick: () -> Unit = {},
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
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
        )
        Text(
            text = "PUSAT MANAJEMEN",
            color = DashboardTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 12.dp, bottom = 48.dp)
        )

        OwnerSidebarItem(
            icon = Icons.Outlined.GridView,
            text = "Dashboard",
            isActive = activeDestination == OwnerDestination.Dashboard,
            onClick = onDashboardClick
        )
        OwnerSidebarItem(icon = Icons.Outlined.BarChart, text = "Laporan Penjualan")
        OwnerSidebarItem(icon = Icons.Outlined.Inventory2, text = "Laporan Stok")
        OwnerSidebarItem(icon = Icons.Outlined.Payments, text = "Kas Harian")
        OwnerSidebarItem(
            icon = Icons.Outlined.Group,
            text = "Manajemen Pengguna",
            isActive = activeDestination == OwnerDestination.UserManagement,
            onClick = onUserManagementClick
        )
        OwnerSidebarItem(
            icon = Icons.Outlined.Security,
            text = "Log Keamanan",
            isActive = activeDestination == OwnerDestination.SecurityLog,
            onClick = onSecurityLogClick
        )
        OwnerSidebarItem(icon = Icons.Outlined.Settings, text = "Pengaturan")

        Spacer(modifier = Modifier.weight(1f))

        OwnerSidebarItem(
            icon = Icons.AutoMirrored.Outlined.Logout,
            text = "Keluar",
            tint = DashboardTextSecondary,
            onClick = onLogout
        )
    }
}

enum class OwnerDestination {
    Dashboard,
    UserManagement,
    SecurityLog
}

@Composable
private fun OwnerSidebarItem(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    tint: Color = DashboardTextPrimary,
    onClick: (() -> Unit)? = null
) {
    val contentColor = if (isActive) DashboardBrandGreenDark else tint
    val backgroundColor = if (isActive) DashboardBrandGreen.copy(alpha = 0.12f) else Color.Transparent
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(clickModifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = text,
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}
