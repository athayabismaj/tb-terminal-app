package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tbterminal.app.data.model.UserProfile
import com.tbterminal.app.ui.components.TbLayoutInfo
import com.tbterminal.app.ui.dashboard.RoleMenuGroupCard
import com.tbterminal.app.ui.dashboard.RoleMenuItemSpec
import com.tbterminal.app.ui.dashboard.RoleMenuProfileCard
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.rememberStaggeredEntrance
import com.tbterminal.app.ui.dashboard.staggeredEntrance

@Composable
internal fun OwnerMenuContent(
    layout: TbLayoutInfo,
    sessionName: String,
    role: String,
    profile: UserProfile?,
    onProfileClick: () -> Unit,
    onNavigate: (AdminDestination) -> Unit,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val groups = remember(role) { ownerMenuGroups(role) }
    val columns = remember(groups, layout.isCompact) { ownerMenuColumns(groups, expanded = !layout.isCompact) }
    val entrance = rememberStaggeredEntrance(stepCount = 1 + groups.size)
    val density = LocalDensity.current
    val profileSlidePx = remember(density) { with(density) { 14.dp.toPx() } }
    val groupBaseSlidePx = remember(density) { with(density) { 18.dp.toPx() } }
    val groupStepSlidePx = remember(density) { with(density) { 4.dp.toPx() } }
    val displayName = profile?.name?.takeIf(String::isNotBlank) ?: sessionName
    val username = profile?.username?.takeIf(String::isNotBlank)
    val roleLabel = if (username != null) "@$username · Pemilik Toko" else "Pemilik · TB Barokah Jaya"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = if (layout.isCompact) 12.dp else 0.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(if (layout.isCompact) 14.dp else 18.dp),
    ) {
        RoleMenuProfileCard(
            displayName = displayName,
            roleLabel = roleLabel,
            onClick = onProfileClick,
            testTag = "owner-menu-profile",
            modifier = (if (layout.isCompact) Modifier.padding(horizontal = 16.dp) else Modifier)
                .staggeredEntrance(profileSlidePx) { entrance.progress(0) },
        )
        if (layout.isCompact) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                groups.forEachIndexed { index, group ->
                    OwnerMenuGroupSection(
                        group = group,
                        compact = true,
                        onNavigate = onNavigate,
                        onLogout = onLogout,
                        modifier = Modifier.staggeredEntrance(groupBaseSlidePx + index * groupStepSlidePx) {
                            entrance.progress(1 + index)
                        },
                    )
                }
            }
        } else {
            val groupIndices = remember(groups) {
                groups.mapIndexed { idx, group -> group.titleRes to idx }.toMap()
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                columns.forEach { column ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        column.forEach { group ->
                            val index = groupIndices[group.titleRes] ?: 0
                            OwnerMenuGroupSection(
                                group = group,
                                compact = false,
                                onNavigate = onNavigate,
                                onLogout = onLogout,
                                modifier = Modifier.staggeredEntrance(groupBaseSlidePx + index * groupStepSlidePx) {
                                    entrance.progress(1 + index)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerMenuGroupSection(
    group: OwnerMenuGroup,
    compact: Boolean,
    onNavigate: (AdminDestination) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val groupTitle = stringResource(group.titleRes)
    val items = group.actions.map { action ->
        RoleMenuItemSpec(
            key = action.name,
            icon = action.icon(),
            title = stringResource(action.titleRes),
            subtitle = action.subtitle,
            isDestructive = action == OwnerMenuAction.LOGOUT,
            testTag = "owner-menu-${action.name}",
            onClick = {
                if (action == OwnerMenuAction.LOGOUT) {
                    onLogout()
                } else {
                    action.destination?.let(onNavigate)
                }
            },
        )
    }
    RoleMenuGroupCard(
        title = groupTitle,
        items = items,
        modifier = modifier,
        compact = compact,
    )
}

private fun OwnerMenuAction.icon(): ImageVector = when (this) {
    OwnerMenuAction.CUSTOMERS -> Icons.Outlined.Groups
    OwnerMenuAction.SUPPLIERS -> Icons.Outlined.Group
    OwnerMenuAction.USERS -> Icons.Outlined.Person
    OwnerMenuAction.BACKUP -> Icons.Outlined.Backup
    OwnerMenuAction.STORE_SETTINGS -> Icons.Outlined.Storefront
    OwnerMenuAction.DEVICE_SETTINGS -> Icons.Outlined.Print
    OwnerMenuAction.SETTINGS -> Icons.Outlined.Settings
    OwnerMenuAction.SYNC -> Icons.Outlined.Sync
    OwnerMenuAction.ACTIVITY -> Icons.Outlined.Security
    OwnerMenuAction.SECURITY_LOG -> Icons.Outlined.Lock
    OwnerMenuAction.LOGOUT -> Icons.AutoMirrored.Outlined.Logout
}
