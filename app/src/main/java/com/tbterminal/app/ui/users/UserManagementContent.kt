package com.tbterminal.app.ui.users

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val UserSlate50 = Color(0xFFF8FAFC)
private val UserSlate100 = Color(0xFFF1F5F9)
private val UserSlate200 = Color(0xFFE2E8F0)
private val UserSlate300 = Color(0xFFCBD5E1)
private val UserSlate400 = Color(0xFF94A3B8)
private val UserSlate500 = Color(0xFF64748B)
private val UserSlate600 = Color(0xFF475569)
private val UserOnSurface = Color(0xFF0F172A)
private val UserPrimary = Color(0xFF10B981)
private val UserPrimaryDark = Color(0xFF059669)
private val UserAmber100 = Color(0xFFFEF3C7)
private val UserAmber500 = Color(0xFFF59E0B)
private val UserAmber700 = Color(0xFFB45309)
private val UserBlue100 = Color(0xFFDBEAFE)
private val UserBlue700 = Color(0xFF1D4ED8)
private val UserError = Color(0xFFEF4444)

@Composable
fun UserManagementContent(
    modifier: Modifier = Modifier,
    uiState: UserManagementUiState,
    onAddUserClick: () -> Unit = {},
    onRetry: () -> Unit = {},
    onEditUserClick: (StaffMember) -> Unit = {},
    onChangePasswordClick: (StaffMember) -> Unit = {},
    onChangePinClick: (StaffMember) -> Unit = {},
    onDeactivateUserClick: (StaffMember) -> Unit = {},
    onActivateUserClick: (StaffMember) -> Unit = {},
    onSecurityLogClick: () -> Unit = {},
    onDismissActionMessage: () -> Unit = {}
) {
    val compact = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 700
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(UserSlate50)
            .padding(if (compact) 16.dp else 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        val pageTitle: @Composable () -> Unit = {
            Column {
                Text(
                    text = "Pengguna",
                    color = UserOnSurface,
                    fontSize = if (compact) 24.sp else 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                if (!compact) Text(
                    text = "Kelola akses staf dan izin untuk Terminal Toko.",
                    color = UserSlate500,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        val addButton: @Composable (Modifier) -> Unit = { buttonModifier ->
            Button(
                onClick = onAddUserClick,
                modifier = buttonModifier,
                colors = ButtonDefaults.buttonColors(containerColor = UserPrimary),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Tambah Pengguna", fontWeight = FontWeight.Bold)
            }
        }
        if (compact) Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            pageTitle()
            addButton(Modifier.fillMaxWidth().height(48.dp))
        } else Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            pageTitle()
            addButton(Modifier)
        }

        val statCards: @Composable () -> Unit = {
            UserStatCard(
                modifier = if (compact) Modifier.fillMaxWidth() else Modifier.weight(1f), icon = Icons.Default.Group, title = "TOTAL STAF",
                value = if (uiState.isLoading) "Memuat" else "${uiState.totalStaff} Orang",
                iconBackground = UserSlate50, iconTint = UserSlate400, valueColor = UserOnSurface
            )
            UserStatCard(
                modifier = if (compact) Modifier.fillMaxWidth() else Modifier.weight(1f), icon = Icons.Default.Verified, title = "STAF AKTIF",
                value = if (uiState.isLoading) "Memuat" else "${uiState.activeStaff} Orang",
                iconBackground = UserPrimary.copy(alpha = 0.1f), iconTint = UserPrimary, valueColor = UserPrimaryDark
            )
            UserStatCard(
                modifier = if (compact) Modifier.fillMaxWidth() else Modifier.weight(1f), icon = Icons.Default.Update, title = "LOGIN TERAKHIR",
                value = if (uiState.isLoading) "Memuat" else uiState.latestLogin,
                iconBackground = UserAmber500.copy(alpha = 0.1f), iconTint = UserAmber500, valueColor = UserOnSurface
            )
        }
        if (compact) Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            statCards()
        } else Row(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) { statCards() }

        if (uiState.actionErrorMessage != null) {
            UserActionBanner(
                message = uiState.actionErrorMessage,
                isError = true,
                onDismiss = onDismissActionMessage
            )
            Spacer(modifier = Modifier.height(18.dp))
        } else if (uiState.actionSuccessMessage != null) {
            UserActionBanner(
                message = uiState.actionSuccessMessage,
                isError = false,
                onDismiss = onDismissActionMessage
            )
            Spacer(modifier = Modifier.height(18.dp))
        }

        UserTableCard(
            staffMembers = uiState.staffMembers,
            isLoading = uiState.isLoading,
            errorMessage = uiState.errorMessage,
            isMutating = uiState.isMutating,
            onRetry = onRetry,
            onEditUserClick = onEditUserClick,
            onChangePasswordClick = onChangePasswordClick,
            onChangePinClick = onChangePinClick,
            onDeactivateUserClick = onDeactivateUserClick,
            onActivateUserClick = onActivateUserClick,
            compact = compact
        )
        Spacer(modifier = Modifier.height(20.dp))
        UserSecurityNotice(onSecurityLogClick = onSecurityLogClick)
    }
}

@Composable
private fun UserTableCard(
    staffMembers: List<StaffMember>,
    isLoading: Boolean,
    errorMessage: String?,
    isMutating: Boolean,
    onRetry: () -> Unit,
    onEditUserClick: (StaffMember) -> Unit,
    onChangePasswordClick: (StaffMember) -> Unit,
    onChangePinClick: (StaffMember) -> Unit,
    onDeactivateUserClick: (StaffMember) -> Unit,
    onActivateUserClick: (StaffMember) -> Unit,
    compact: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, UserSlate200)
    ) {
        Column {
            if (!compact) Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(24.dp)
                            .clip(CircleShape)
                            .background(UserPrimary)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Daftar Staf",
                        color = UserOnSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = UserSlate400
                    )
                }
            }

            HorizontalDivider(color = UserSlate100)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(UserSlate50.copy(alpha = 0.5f))
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                UserTableHeaderText(text = "NAMA", modifier = Modifier.weight(1.9f))
                UserTableHeaderText(text = "USERNAME", modifier = Modifier.weight(1.35f))
                UserTableHeaderText(text = "ROLE", modifier = Modifier.weight(1f))
                UserTableHeaderText(text = "STATUS", modifier = Modifier.weight(1f), alignment = Alignment.CenterHorizontally)
                UserTableHeaderText(text = "TERAKHIR LOGIN", modifier = Modifier.weight(1.35f))
                UserTableHeaderText(text = "AKSI", modifier = Modifier.weight(1.4f), alignment = Alignment.End)
            }

            HorizontalDivider(color = UserSlate100)

            when {
                isLoading -> UserTableFeedback(
                    message = "Memuat daftar pengguna...",
                    showProgress = true
                )
                errorMessage != null -> UserTableFeedback(
                    message = errorMessage,
                    actionLabel = "Muat ulang",
                    onAction = onRetry
                )
                staffMembers.isEmpty() -> UserTableFeedback(
                    message = "Belum ada pengguna yang tersedia."
                )
                else -> {
                    staffMembers.forEachIndexed { index, staff ->
                        if (compact) StaffMobileCard(
                            staff = staff,
                            actionsEnabled = !isMutating,
                            onEditUserClick = onEditUserClick,
                            onChangePasswordClick = onChangePasswordClick,
                            onChangePinClick = onChangePinClick,
                            onDeactivateUserClick = onDeactivateUserClick,
                            onActivateUserClick = onActivateUserClick
                        ) else StaffRow(
                            staff = staff,
                            actionsEnabled = !isMutating,
                            onEditUserClick = onEditUserClick,
                            onChangePasswordClick = onChangePasswordClick,
                            onChangePinClick = onChangePinClick,
                            onDeactivateUserClick = onDeactivateUserClick,
                            onActivateUserClick = onActivateUserClick
                        )
                        if (index < staffMembers.lastIndex) {
                            HorizontalDivider(color = UserSlate100)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserTableFeedback(
    message: String,
    showProgress: Boolean = false,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showProgress) {
            CircularProgressIndicator(
                color = UserPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = message,
            color = UserSlate500,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        if (actionLabel != null) {
            TextButton(onClick = onAction) {
                Text(text = actionLabel, color = UserPrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun UserActionBanner(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    val background = if (isError) UserError.copy(alpha = 0.14f) else UserPrimary.copy(alpha = 0.12f)
    val contentColor = if (isError) UserError else UserPrimaryDark

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            color = contentColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        TextButton(onClick = onDismiss) {
            Text(
                text = "Tutup",
                color = contentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun UserStatCard(
    icon: ImageVector,
    title: String,
    value: String,
    iconBackground: Color,
    iconTint: Color,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, UserSlate100),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    color = UserSlate400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = value,
                    color = valueColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun StaffDeactivateDialog(
    staff: StaffMember,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Text(
                text = "Nonaktifkan pengguna?",
                color = UserOnSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Akun ${staff.name} akan dinonaktifkan dan tidak bisa digunakan untuk login sampai diaktifkan kembali.",
                color = UserSlate600,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = UserError),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(text = if (isSaving) "Memproses..." else "Nonaktifkan")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text(text = "Batal", color = UserSlate600, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
private fun UserTableHeaderText(
    text: String,
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.Start
) {
    Column(
        modifier = modifier,
        horizontalAlignment = alignment
    ) {
        Text(
            text = text,
            color = UserSlate400,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun StaffMobileCard(
    staff: StaffMember,
    actionsEnabled: Boolean,
    onEditUserClick: (StaffMember) -> Unit,
    onChangePasswordClick: (StaffMember) -> Unit,
    onChangePinClick: (StaffMember) -> Unit,
    onDeactivateUserClick: (StaffMember) -> Unit,
    onActivateUserClick: (StaffMember) -> Unit
) {
    val inactive = staff.status == StaffStatus.Inactive
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (inactive) UserError.copy(alpha = 0.04f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (inactive) UserSlate200 else UserPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(staff.initials, color = if (inactive) UserSlate400 else UserPrimary, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(staff.name, color = UserOnSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("@${staff.username} · ${staff.lastLogin}", color = UserSlate500, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            StaffRoleBadge(role = staff.role, inactive = inactive)
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(if (inactive) UserSlate400 else UserPrimary))
            Spacer(Modifier.width(6.dp))
            Text(if (inactive) "Nonaktif" else "Aktif", color = if (inactive) UserSlate500 else UserPrimaryDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            StaffActionIconButton(Icons.Default.Edit, "Edit pengguna", UserSlate500, actionsEnabled) { onEditUserClick(staff) }
            if (inactive) {
                StaffActionIconButton(Icons.Default.CheckCircle, "Aktifkan pengguna", UserPrimary, actionsEnabled) { onActivateUserClick(staff) }
            } else {
                StaffActionIconButton(Icons.Default.Password, "Ganti password", UserSlate500, actionsEnabled) { onChangePasswordClick(staff) }
                StaffActionIconButton(Icons.Default.CreditCard, "Ganti PIN", UserSlate500, actionsEnabled) { onChangePinClick(staff) }
                StaffActionIconButton(Icons.Default.Block, "Nonaktifkan pengguna", UserError, actionsEnabled) { onDeactivateUserClick(staff) }
            }
        }
    }
}

@Composable
private fun StaffRow(
    staff: StaffMember,
    actionsEnabled: Boolean,
    onEditUserClick: (StaffMember) -> Unit,
    onChangePasswordClick: (StaffMember) -> Unit,
    onChangePinClick: (StaffMember) -> Unit,
    onDeactivateUserClick: (StaffMember) -> Unit,
    onActivateUserClick: (StaffMember) -> Unit
) {
    val inactive = staff.status == StaffStatus.Inactive
    val rowAlpha = if (inactive) 0.7f else 1f
    val rowBackground = if (inactive) UserError.copy(alpha = 0.05f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground)
            .clickable {}
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .alpha(rowAlpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1.9f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (inactive) UserSlate200 else UserPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = staff.initials,
                    color = if (inactive) UserSlate400 else UserPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = staff.name,
                    color = if (inactive) UserSlate500 else UserOnSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = staff.email,
                    color = UserSlate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Text(
            text = staff.username,
            modifier = Modifier.weight(1.35f),
            color = UserSlate600,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )

        StaffRoleBadge(
            role = staff.role,
            inactive = inactive,
            modifier = Modifier.weight(1f)
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (inactive) UserSlate400 else UserPrimary)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (inactive) "NONAKTIF" else "AKTIF",
                color = if (inactive) UserSlate400 else UserPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = staff.lastLogin,
            modifier = Modifier.weight(1.35f),
            color = if (inactive) UserSlate400 else UserSlate500,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier.weight(1.4f),
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (inactive) {
                StaffActionIconButton(
                    icon = Icons.Default.Edit,
                    contentDescription = "Edit pengguna",
                    tint = UserSlate500,
                    onClick = { onEditUserClick(staff) },
                    enabled = actionsEnabled
                )
                StaffActionIconButton(
                    icon = Icons.Default.CheckCircle,
                    contentDescription = "Aktifkan pengguna",
                    tint = UserPrimary,
                    onClick = { onActivateUserClick(staff) },
                    enabled = actionsEnabled
                )
            } else {
                StaffActionIconButton(
                    icon = Icons.Default.Edit,
                    contentDescription = "Edit pengguna",
                    tint = UserSlate500,
                    onClick = { onEditUserClick(staff) },
                    enabled = actionsEnabled
                )
                StaffActionIconButton(
                    icon = Icons.Default.Password,
                    contentDescription = "Ganti password",
                    tint = UserSlate500,
                    onClick = { onChangePasswordClick(staff) },
                    enabled = actionsEnabled
                )
                StaffActionIconButton(
                    icon = Icons.Default.CreditCard,
                    contentDescription = "Ganti PIN",
                    tint = UserSlate500,
                    onClick = { onChangePinClick(staff) },
                    enabled = actionsEnabled
                )
                StaffActionIconButton(
                    icon = Icons.Default.Block,
                    contentDescription = "Nonaktifkan pengguna",
                    tint = UserError.copy(alpha = 0.7f),
                    onClick = { onDeactivateUserClick(staff) },
                    enabled = actionsEnabled
                )
            }
        }
    }
}

@Composable
private fun StaffActionIconButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else UserSlate300,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun StaffRoleBadge(
    role: StaffRole,
    inactive: Boolean,
    modifier: Modifier = Modifier
) {
    val background = when (role) {
        StaffRole.Owner -> UserAmber100
        StaffRole.Admin -> UserPrimary.copy(alpha = 0.2f)
        StaffRole.Kasir -> if (inactive) UserSlate200 else UserBlue100
        StaffRole.Other -> UserSlate200
    }
    val contentColor = when (role) {
        StaffRole.Owner -> UserAmber700
        StaffRole.Admin -> UserPrimaryDark
        StaffRole.Kasir -> if (inactive) UserSlate500 else UserBlue700
        StaffRole.Other -> UserSlate600
    }
    val label = when (role) {
        StaffRole.Owner -> "OWNER"
        StaffRole.Admin -> "ADMIN"
        StaffRole.Kasir -> "KASIR"
        StaffRole.Other -> "LAINNYA"
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(background)
                .padding(horizontal = 10.dp, vertical = 2.dp)
        ) {
            Text(
                text = label,
                color = contentColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun UserSecurityNotice(
    onSecurityLogClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = UserPrimaryDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Security,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.1f),
                modifier = Modifier
                    .size(84.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 18.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Kepatuhan Keamanan Staf",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Staf diwajibkan mengganti PIN akses setiap 90 hari sesuai standar keamanan operasional. Audit terakhir menunjukkan kepatuhan 100% pada seluruh terminal aktif.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = onSecurityLogClick,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp)
                ) {
                    Text(text = "Lihat Log Keamanan", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
