package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.tbterminal.app.navigation.AppAccessPolicy
import com.tbterminal.app.navigation.AppCapability
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.BrandSoftText
import com.tbterminal.app.ui.theme.FieldBorder
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.OwnerSyncDot
import java.util.Locale

@Immutable
internal data class OwnerMenuUiState(
    val userName: String = "Pemilik Toko",
    val role: String = "OWNER",
    val storeName: String = "Terminal Barokah Jaya",
    // TODO: Muat foto profil melingkar 40dp saat UserProfile menyediakan URL foto.
    val photoUrl: String? = null,
    val synced: Boolean = true,
    val syncSubtitle: String = "Terakhir 2 menit lalu"
) {
    val roleLabel: String
        get() = formatMenuRoleLabel(role)

    val initial: String
        get() = userName.trim().firstOrNull()?.uppercase() ?: "P"
}

internal fun sampleReferenceMenuState(
    role: String = "OWNER",
    userName: String = "Pemilik Toko"
): OwnerMenuUiState = OwnerMenuUiState(
    userName = userName,
    role = role,
    storeName = "Terminal Barokah Jaya",
    photoUrl = null,
    synced = true,
    syncSubtitle = "Terakhir 2 menit lalu"
)

internal fun formatMenuRoleLabel(role: String?): String {
    return when (role?.trim()?.lowercase(Locale.ROOT)) {
        "owner" -> "Owner"
        "admin" -> "Admin"
        "kasir" -> "Kasir"
        else -> {
            val cleaned = role?.trim().orEmpty()
            if (cleaned.isEmpty()) "Owner"
            else cleaned.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }
        }
    }
}

internal fun buildOwnerMenuGroups(
    role: String?,
    syncSubtitle: String,
    onDestinationClick: (AdminDestination) -> Unit
): List<OwnerPlainMenuGroup> {
    val storeItems = buildList {
        if (AppAccessPolicy.can(role, AppCapability.CUSTOMERS)) {
            add(
                OwnerPlainMenuItem(
                    tag = "owner-menu-item-customers",
                    title = "Pelanggan",
                    subtitle = "Data & riwayat belanja pelanggan",
                    icon = OwnerIcons.Customers,
                    iconTint = Brand,
                    onClick = { onDestinationClick(AdminDestination.Customers) }
                )
            )
        }
        if (AppAccessPolicy.can(role, AppCapability.SUPPLIERS)) {
            add(
                OwnerPlainMenuItem(
                    tag = "owner-menu-item-suppliers",
                    title = "Supplier",
                    subtitle = "Kontak & riwayat pembelian",
                    icon = OwnerIcons.SupplierTruck,
                    iconTint = Brand,
                    onClick = { onDestinationClick(AdminDestination.Suppliers) }
                )
            )
        }
        if (AppAccessPolicy.can(role, AppCapability.USER_MANAGEMENT)) {
            add(
                OwnerPlainMenuItem(
                    tag = "owner-menu-item-users",
                    title = "Pengguna & akses",
                    subtitle = "Kasir, peran, dan PIN",
                    icon = OwnerIcons.UserSingle,
                    iconTint = Brand,
                    onClick = { onDestinationClick(AdminDestination.UserManagement) }
                )
            )
        }
    }

    val systemItems = buildList {
        if (AppAccessPolicy.can(role, AppCapability.SYNC)) {
            add(
                OwnerPlainMenuItem(
                    tag = "owner-menu-item-sync",
                    title = "Sinkronisasi",
                    subtitle = syncSubtitle,
                    icon = OwnerIcons.SyncArrows,
                    iconTint = Brand,
                    onClick = { onDestinationClick(AdminDestination.SyncCenter) }
                )
            )
        }
        if (AppAccessPolicy.can(role, AppCapability.SERVER_BACKUP)) {
            add(
                OwnerPlainMenuItem(
                    tag = "owner-menu-item-backup",
                    title = "Cadangan data",
                    subtitle = "Cadangkan & pulihkan data",
                    icon = OwnerIcons.BackupCloud,
                    iconTint = Brand,
                    onClick = { onDestinationClick(AdminDestination.BackupRestore) }
                )
            )
        }
        if (AppAccessPolicy.can(role, AppCapability.AUDIT)) {
            add(
                OwnerPlainMenuItem(
                    tag = "owner-menu-item-activity",
                    title = "Riwayat aktivitas",
                    subtitle = "Catatan perubahan data",
                    icon = OwnerIcons.ActivityClock,
                    iconTint = Brand,
                    onClick = { onDestinationClick(AdminDestination.OperationalAudit) }
                )
            )
        }
        if (AppAccessPolicy.can(role, AppCapability.SECURITY_SETTINGS)) {
            add(
                OwnerPlainMenuItem(
                    tag = "owner-menu-item-security",
                    title = "Log keamanan",
                    subtitle = "Login & percobaan PIN",
                    icon = OwnerIcons.SecurityShieldCheck,
                    iconTint = Brand,
                    onClick = { onDestinationClick(AdminDestination.SecurityLog) }
                )
            )
        }
    }

    return buildList {
        if (storeItems.isNotEmpty()) {
            add(OwnerPlainMenuGroup(title = "Kelola toko", items = storeItems))
        }
        if (systemItems.isNotEmpty()) {
            add(OwnerPlainMenuGroup(title = "Sistem & keamanan", items = systemItems))
        }
    }
}

@Composable
internal fun OwnerMenuTopBar(
    userName: String,
    initial: String,
    synced: Boolean,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val syncLabel = if (synced) "tersinkron" else "offline"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Menu",
            style = ownerTextStyle(
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnBrand,
                letterSpacing = (-0.01).em
            ),
            maxLines = 1,
            modifier = Modifier.semantics { heading() }
        )

        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 48.dp)
        ) {
            Box(
                modifier = Modifier
                    .wrapContentSize(align = Alignment.CenterStart, unbounded = true)
                    .size(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onProfileClick
                    )
                    .semantics { contentDescription = "Akun $userName, $syncLabel" }
                    .testTag("owner-menu-avatar-btn"),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(40.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            style = ownerTextStyle(
                                fontSize = 16.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnBrand
                            ),
                            modifier = Modifier.cssLineBoxHeight(20.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (synced) OwnerSyncDot else FieldBorder)
                            .border(width = 2.dp, color = Brand, shape = CircleShape)
                            .testTag("owner-menu-sync-dot")
                    )
                }
            }
        }
    }
}

@Composable
internal fun OwnerMenuProfileHeaderBlock(
    state: OwnerMenuUiState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = state.userName,
            style = ownerTextStyle(
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
                color = OnBrand,
                letterSpacing = (-0.01).em
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .cssLineBoxHeight(26.dp)
                .testTag("owner-menu-profile-name")
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OwnerRoleBadgePill(
                roleLabel = state.roleLabel,
                modifier = Modifier.testTag("owner-menu-role-badge")
            )

            Text(
                text = "· ${state.storeName}",
                style = ownerTextStyle(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = BrandSoftText
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .cssLineBoxHeight(20.dp)
                    .testTag("owner-menu-store-name")
            )
        }
    }
}

@Composable
internal fun OwnerRoleBadgePill(
    roleLabel: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(24.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OwnerVectorIcon(
            imageVector = OwnerIcons.Shield,
            tint = OnBrand,
            size = 14.dp
        )
        Text(
            text = roleLabel,
            style = ownerTextStyle(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnBrand
            ),
            maxLines = 1,
            modifier = Modifier.cssLineBoxHeight(16.dp)
        )
    }
}
