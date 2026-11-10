package com.tbterminal.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Summarize
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.components.TbLayoutInfo
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import java.util.Locale

private val MenuBrandGreen = Color(0xFF1B5E3C)
private val MenuBrandTint = Color(0xFFEEF7F2)
private val MenuDivider = Color(0xFFF1F5F9)
private val MenuTextPrimary = Color(0xFF0F172A)
private val MenuTextSecondary = Color(0xFF64748B)
private val MenuChevron = Color(0xFF94A3B8)
private val MenuDestructiveTint = Color(0xFFFFEEF0)
private val MenuDestructiveRed = Color(0xFFE11D48)

internal data class RoleMenuItemSpec(
    val key: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val isDestructive: Boolean = false,
    val testTag: String,
    val onClick: () -> Unit,
)

@Composable
internal fun BackofficeMoreContent(
    layout: TbLayoutInfo,
    userName: String = "Admin",
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

    val displayName = remember(userName) {
        val trimmed = userName.trim()
        when {
            trimmed.isEmpty() || trimmed.equals("admin", ignoreCase = true) -> "Admin Operasional"
            else -> trimmed
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("more-hub-content")
            .padding(top = 6.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        RoleMenuProfileCard(
            displayName = displayName,
            roleLabel = "Admin · TB Barokah Jaya",
            onClick = { onNavigate(AdminDestination.Profile) },
            testTag = "more-menu-profile",
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            columns.forEach { column ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    column.forEach { section ->
                        RoleMenuGroupCard(
                            title = section.title,
                            items = section.actions.map { action ->
                                RoleMenuItemSpec(
                                    key = action.name,
                                    icon = action.icon(),
                                    title = action.title,
                                    subtitle = action.subtitle,
                                    isDestructive = action == MoreMenuAction.LOGOUT,
                                    testTag = "more-action-${action.name}",
                                    onClick = { onSelect(action) },
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun RoleMenuProfileCard(
    displayName: String,
    roleLabel: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    val initial = displayName.trim().firstOrNull()?.uppercase(Locale.ROOT) ?: "U"
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, MenuDivider),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MenuBrandTint),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initial,
                    color = MenuBrandGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = displayName,
                    color = MenuTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = roleLabel,
                    color = MenuTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MenuBrandTint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = "Buka profil",
                    tint = MenuBrandGreen,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
internal fun RoleMenuGroupCard(
    title: String,
    items: List<RoleMenuItemSpec>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title.uppercase(Locale.ROOT),
            modifier = Modifier.padding(horizontal = 4.dp),
            color = MenuTextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, MenuDivider),
            shadowElevation = 1.dp,
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    RoleMenuRow(item)
                    if (index < items.lastIndex) {
                        HorizontalDivider(color = MenuDivider)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleMenuRow(item: RoleMenuItemSpec) {
    val iconBg = if (item.isDestructive) MenuDestructiveTint else MenuBrandTint
    val iconTint = if (item.isDestructive) MenuDestructiveRed else MenuBrandGreen
    val textColor = if (item.isDestructive) MenuDestructiveRed else MenuTextPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(item.testTag)
            .clickable(role = Role.Button, onClick = item.onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBg, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.subtitle,
                color = MenuTextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MenuChevron,
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
    MoreMenuAction.STORE_SETTINGS -> Icons.Outlined.Storefront
    MoreMenuAction.DEVICE_SETTINGS -> Icons.Outlined.Print
    MoreMenuAction.SETTINGS -> Icons.Outlined.Settings
    MoreMenuAction.SYNC -> Icons.Outlined.Sync
    MoreMenuAction.LOGOUT -> Icons.AutoMirrored.Outlined.Logout
}
