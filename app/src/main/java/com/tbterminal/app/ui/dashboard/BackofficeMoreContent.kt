package com.tbterminal.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
private val MenuBrandTint = Color(0xFFEAF3ED)
private val MenuSoftSurface = Color(0xFFF6F9F7)
private val MenuSoftBorder = Color(0xFFE6ECE8)
private val MenuRowDivider = Color(0xFFF1F5F9)
private val MenuSectionDivider = Color(0xFFE2E8F0)
private val MenuTextPrimary = Color(0xFF0F172A)
private val MenuTextSecondary = Color(0xFF64748B)
private val MenuBadgeText = Color(0xFF475569)
private val MenuChevron = Color(0xFFCBD5E1)
private val MenuDestructiveRed = Color(0xFFE11D48)

internal data class RoleMenuItemSpec(
    val key: String,
    val icon: ImageVector,
    val title: String,
    val subtitle: String = "",
    val badgeText: String? = null,
    val badgeDotColor: Color? = null,
    val isDestructive: Boolean = false,
    val testTag: String,
    val onClick: () -> Unit,
)

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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("more-hub-content")
            .padding(top = if (layout.isCompact) 8.dp else 0.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(if (layout.isCompact) 8.dp else 20.dp),
    ) {
        if (layout.isCompact) {
            sections.forEachIndexed { index, section ->
                RoleMenuGroupCard(
                    title = section.title,
                    items = section.toMenuItemSpecs(onSelect),
                    compact = true,
                    showBottomDivider = index < sections.lastIndex,
                )
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                columns.forEach { column ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        column.forEachIndexed { index, section ->
                            RoleMenuGroupCard(
                                title = section.title,
                                items = section.toMenuItemSpecs(onSelect),
                                compact = false,
                                showBottomDivider = index < column.lastIndex,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun MoreMenuSection.toMenuItemSpecs(onSelect: (MoreMenuAction) -> Unit): List<RoleMenuItemSpec> =
    actions.map { action ->
        RoleMenuItemSpec(
            key = action.name,
            icon = action.icon(),
            title = action.title,
            subtitle = action.subtitle,
            isDestructive = action == MoreMenuAction.LOGOUT,
            testTag = "more-action-${action.name}",
            onClick = { onSelect(action) },
        )
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
        shape = RoundedCornerShape(14.dp),
        color = MenuSoftSurface,
        border = BorderStroke(1.dp, MenuSoftBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MenuBrandTint),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initial,
                    color = MenuBrandGreen,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = displayName,
                    color = MenuTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
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
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Buka profil",
                tint = MenuTextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
internal fun RoleMenuGroupCard(
    title: String,
    items: List<RoleMenuItemSpec>,
    modifier: Modifier = Modifier,
    compact: Boolean = true,
    showBottomDivider: Boolean = true,
) {
    val horizontalPad = if (compact) 16.dp else 4.dp
    val dividerStart = if (compact) 52.dp else 40.dp

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(Locale.ROOT),
            modifier = Modifier.padding(
                start = horizontalPad,
                end = horizontalPad,
                top = if (compact) 12.dp else 0.dp,
                bottom = 6.dp,
            ),
            color = MenuTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.7.sp,
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, item ->
                RoleMenuRow(item = item, horizontalPadding = horizontalPad)
                if (index < items.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = dividerStart, end = horizontalPad),
                        color = MenuRowDivider,
                        thickness = 1.dp,
                    )
                }
            }
            if (showBottomDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = horizontalPad, vertical = 4.dp),
                    color = MenuSectionDivider.copy(alpha = 0.75f),
                    thickness = 0.75.dp,
                )
            }
        }
    }
}

@Composable
private fun RoleMenuRow(
    item: RoleMenuItemSpec,
    horizontalPadding: androidx.compose.ui.unit.Dp,
) {
    val iconTint = if (item.isDestructive) MenuDestructiveRed else MenuBrandGreen
    val textColor = if (item.isDestructive) MenuDestructiveRed else MenuTextPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .testTag(item.testTag)
            .clickable(role = Role.Button, onClick = item.onClick)
            .padding(horizontal = horizontalPadding, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = item.title,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (item.badgeText != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (item.badgeDotColor != null) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(item.badgeDotColor),
                    )
                }
                Text(
                    text = item.badgeText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MenuBadgeText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MenuChevron,
            modifier = Modifier.size(18.dp),
        )
    }
}

private fun MoreMenuAction.icon(): ImageVector = when (this) {
    MoreMenuAction.CUSTOMERS -> Icons.Outlined.Groups
    MoreMenuAction.SUPPLIERS -> Icons.Outlined.Group
    MoreMenuAction.REPORTS -> Icons.Outlined.Summarize
    MoreMenuAction.LOCAL_REPORTS -> Icons.AutoMirrored.Outlined.ReceiptLong
    MoreMenuAction.USERS -> Icons.Outlined.Person
    MoreMenuAction.ACTIVITY -> Icons.Outlined.Security
    MoreMenuAction.SECURITY_LOG -> Icons.Outlined.Lock
    MoreMenuAction.BACKUP -> Icons.Outlined.Backup
    MoreMenuAction.STORE_SETTINGS -> Icons.Outlined.Storefront
    MoreMenuAction.DEVICE_SETTINGS -> Icons.Outlined.Print
    MoreMenuAction.SETTINGS -> Icons.Outlined.Settings
    MoreMenuAction.SYNC -> Icons.Outlined.Sync
    MoreMenuAction.LOGOUT -> Icons.AutoMirrored.Outlined.Logout
}
