package com.tbterminal.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Summarize
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tbterminal.app.ui.components.TbLayoutInfo
import com.tbterminal.app.ui.dashboard.admin.AdminDestination

@Composable
internal fun BackofficeMoreContent(
    layout: TbLayoutInfo,
    role: String,
    onNavigate: (AdminDestination) -> Unit,
    onUserManagementClick: () -> Unit,
    onSecurityLogClick: () -> Unit,
    onLogout: () -> Unit,
) {
    val sections = remember(role) { moreMenuSections(role) }
    val columns = remember(sections, layout.widthClass) { moreMenuColumns(sections, layout.widthClass) }
    val onSelect: (MoreMenuAction) -> Unit = { action ->
        when (action) {
            MoreMenuAction.USERS -> onUserManagementClick()
            MoreMenuAction.SECURITY_LOG -> onSecurityLogClick()
            MoreMenuAction.LOGOUT -> onLogout()
            else -> action.destination?.let(onNavigate)
        }
    }
    
    // Add header to Menu page
    Column(modifier = Modifier.fillMaxWidth().testTag("more-hub-content").padding(bottom = 24.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp)) {
            Text(
                "Menu lainnya",
                color = Color(0xFF0F172A),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                lineHeight = 28.sp
            )
            Text(
                "Pengaturan, laporan, dan preferensi akun",
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            columns.forEach { column ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    column.forEach { section -> MoreMenuGroup(section, onSelect) }
                }
            }
        }
    }
}

@Composable
private fun MoreMenuGroup(
    section: MoreMenuSection,
    onSelect: (MoreMenuAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = section.title.uppercase(),
            modifier = Modifier.padding(horizontal = 4.dp),
            color = Color(0xFF64748B),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
            shadowElevation = 1.dp
        ) {
            Column {
                section.actions.forEachIndexed { index, action ->
                    MoreMenuRow(action, onClick = { onSelect(action) })
                    if (index < section.actions.lastIndex) {
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreMenuRow(action: MoreMenuAction, onClick: () -> Unit) {
    val isLogout = action == MoreMenuAction.LOGOUT
    val iconBg = if (isLogout) Color(0xFFFFEEF0) else Color(0xFFE1EFEA)
    val iconTint = if (isLogout) Color(0xFFE11D48) else Color(0xFF1B4D3E)
    val textColor = if (isLogout) Color(0xFFE11D48) else Color(0xFF0F172A)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("more-action-")
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(iconBg, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(action.icon(), contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(action.title, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(action.subtitle, color = Color(0xFF64748B), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun MoreMenuAction.icon(): ImageVector = when (this) {
    MoreMenuAction.CUSTOMERS -> Icons.Outlined.Groups
    MoreMenuAction.SUPPLIERS -> Icons.Outlined.Group
    MoreMenuAction.REPORTS -> Icons.Outlined.Summarize
    MoreMenuAction.LOCAL_REPORTS -> Icons.AutoMirrored.Outlined.ReceiptLong
    MoreMenuAction.USERS, MoreMenuAction.PROFILE -> Icons.Outlined.Person
    MoreMenuAction.ACTIVITY -> Icons.Outlined.Security
    MoreMenuAction.SECURITY_LOG -> Icons.Outlined.Lock
    MoreMenuAction.BACKUP -> Icons.Outlined.Backup
    MoreMenuAction.SETTINGS -> Icons.Outlined.Settings
    MoreMenuAction.SYNC -> Icons.Outlined.Sync
    MoreMenuAction.LOGOUT -> Icons.AutoMirrored.Outlined.Logout
}
