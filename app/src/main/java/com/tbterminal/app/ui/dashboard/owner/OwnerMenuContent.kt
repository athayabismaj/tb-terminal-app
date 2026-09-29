package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tbterminal.app.R
import com.tbterminal.app.data.model.UserProfile
import com.tbterminal.app.ui.components.TbLayoutInfo
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import java.util.Locale

@Composable
internal fun OwnerMenuContent(
    layout: TbLayoutInfo,
    sessionName: String,
    role: String,
    profile: UserProfile?,
    onProfileClick: () -> Unit,
    onNavigate: (AdminDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups = remember(role) { ownerMenuGroups(role) }
    val columns = remember(groups, layout.isCompact) { ownerMenuColumns(groups, expanded = !layout.isCompact) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (layout.isCompact) 20.dp else 24.dp),
    ) {
        OwnerMenuProfileCard(
            sessionName = sessionName,
            profile = profile,
            onClick = onProfileClick,
        )
        if (layout.isCompact) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                groups.forEach { group -> OwnerMenuGroupCard(group, onNavigate) }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                columns.forEach { column ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        column.forEach { group -> OwnerMenuGroupCard(group, onNavigate) }
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnerMenuProfileCard(
    sessionName: String,
    profile: UserProfile?,
    onClick: () -> Unit,
) {
    val displayName = profile?.name?.takeIf(String::isNotBlank) ?: sessionName
    val username = profile?.username?.takeIf(String::isNotBlank)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 84.dp)
            .testTag("owner-menu-profile")
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        displayName.take(1).uppercase(Locale.ROOT),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (username != null) {
                    Text(
                        "@$username",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = "Buka profil",
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun OwnerMenuGroupCard(group: OwnerMenuGroup, onNavigate: (AdminDestination) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(group.titleRes),
            modifier = Modifier.padding(horizontal = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        ) {
            Column {
                group.actions.forEachIndexed { index, action ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                        )
                    }
                    OwnerMenuRow(action = action, onClick = { onNavigate(action.destination) })
                }
            }
        }
    }
}

@Composable
private fun OwnerMenuRow(action: OwnerMenuAction, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .testTag("owner-menu-${action.name}")
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(action.icon(), contentDescription = null, modifier = Modifier.size(23.dp), tint = MaterialTheme.colorScheme.primary)
        Text(
            stringResource(action.titleRes),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun OwnerMenuAction.icon(): ImageVector = when (this) {
    OwnerMenuAction.CUSTOMERS -> Icons.Outlined.Groups
    OwnerMenuAction.SUPPLIERS -> Icons.Outlined.Group
    OwnerMenuAction.USERS -> Icons.Outlined.Person
    OwnerMenuAction.REPORTS -> Icons.Outlined.Summarize
    OwnerMenuAction.DEVICE_REPORTS -> Icons.AutoMirrored.Outlined.ReceiptLong
    OwnerMenuAction.BACKUP -> Icons.Outlined.Backup
    OwnerMenuAction.SETTINGS -> Icons.Outlined.Settings
    OwnerMenuAction.SYNC -> Icons.Outlined.Sync
    OwnerMenuAction.ACTIVITY -> Icons.Outlined.Security
    OwnerMenuAction.SECURITY_LOG -> Icons.Outlined.Lock
}
