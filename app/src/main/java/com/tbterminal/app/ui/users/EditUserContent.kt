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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.UserRole
import com.tbterminal.app.ui.components.TbMobileControlSheet
import com.tbterminal.app.ui.components.TbMobileSheetDoneButton
import com.tbterminal.app.ui.theme.TbBackground
import com.tbterminal.app.ui.theme.TbError
import com.tbterminal.app.ui.theme.TbGreen
import com.tbterminal.app.ui.theme.TbGreenDark
import com.tbterminal.app.ui.theme.TbGreenLight
import com.tbterminal.app.ui.theme.TbOutline
import com.tbterminal.app.ui.theme.TbSurfaceMuted
import com.tbterminal.app.ui.theme.TbText
import com.tbterminal.app.ui.theme.TbTextMuted

private val EditSurface = TbBackground
private val EditSurfaceLow = TbSurfaceMuted
private val EditOnSurface = TbText
private val EditPrimary = TbGreen
private val EditPrimaryDark = TbGreenDark
private val EditEmerald50 = TbGreenLight
private val EditSlate200 = TbOutline
private val EditSlate400 = Color(0xFF94A3B8)
private val EditSlate500 = TbTextMuted
private val EditSlate600 = TbTextMuted
private val EditBlue100 = Color(0xFFDBEAFE)
private val EditBlue600 = Color(0xFF2563EB)
private val EditPurple100 = Color(0xFFF3E8FF)
private val EditPurple600 = Color(0xFF9333EA)
private val EditError = TbError
private val EditErrorContainer = Color(0xFFFEE2E2)

@Composable
fun EditUserContent(
    modifier: Modifier = Modifier,
    uiState: EditUserUiState,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onSubmit: () -> Unit
) {
    val compact = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 700
    var showAccessDetails by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditSurface)
            .padding(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 16.dp else 28.dp)
            .verticalScroll(rememberScrollState())
    ) {
        when {
            uiState.isLoadingUser -> EditLoadingCard(message = "Memuat data user...")
            uiState.errorMessage != null && uiState.fullName.isBlank() -> {
                EditErrorCard(message = uiState.errorMessage, onRetry = onRetry)
            }
            else -> Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                EditUserFormCard(
                    uiState = uiState,
                    onFullNameChange = onFullNameChange,
                    onEmailChange = onEmailChange,
                    onUsernameChange = onUsernameChange,
                    onRoleChange = onRoleChange,
                    onActiveChange = onActiveChange,
                    onShowAccessDetails = { showAccessDetails = true },
                    onCancel = onCancel,
                    onSubmit = onSubmit,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp),
                    compact = compact,
                )
            }
        }
    }

    if (showAccessDetails) {
        EditUserAccessSheet(
            selectedRole = uiState.selectedRole,
            rolesAreLoading = uiState.isLoadingRoles,
            accountIsActive = uiState.isActive,
            onDismiss = { showAccessDetails = false },
        )
    }
}

@Composable
private fun EditUserFormCard(
    uiState: EditUserUiState,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onShowAccessDetails: () -> Unit,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, EditSlate200)
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 16.dp else 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (uiState.errorMessage != null) {
                EditUserErrorBanner(message = uiState.errorMessage)
            }

            EditUserSectionTitle("Informasi pengguna")
            if (compact) Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EditUserInputField("Nama lengkap", uiState.fullName, onFullNameChange, Icons.Outlined.Person, "Nama karyawan", Modifier.fillMaxWidth(), enabled = !uiState.isSubmitting)
                EditUserInputField("Alamat email", uiState.email, onEmailChange, Icons.Outlined.Email, "contoh@email.com", Modifier.fillMaxWidth(), keyboardType = KeyboardType.Email, enabled = !uiState.isSubmitting)
            } else Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                EditUserInputField(
                    modifier = Modifier.weight(1f),
                    label = "Nama lengkap",
                    value = uiState.fullName,
                    onValueChange = onFullNameChange,
                    icon = Icons.Outlined.Person,
                    placeholder = "Nama karyawan",
                    enabled = !uiState.isSubmitting
                )
                EditUserInputField(
                    modifier = Modifier.weight(1f),
                    label = "Alamat email",
                    value = uiState.email,
                    onValueChange = onEmailChange,
                    icon = Icons.Outlined.Email,
                    placeholder = "contoh@email.com",
                    keyboardType = KeyboardType.Email,
                    enabled = !uiState.isSubmitting
                )
            }

            EditUserSectionTitle("Akun")
            if (compact) Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EditUserInputField("Username", uiState.username, onUsernameChange, Icons.Outlined.AlternateEmail, "username", Modifier.fillMaxWidth(), enabled = !uiState.isSubmitting)
                EditUserStatusCard(uiState.isActive, !uiState.isSubmitting, onActiveChange, Modifier.fillMaxWidth())
            } else Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                EditUserInputField(
                    modifier = Modifier.weight(1f),
                    label = "Username",
                    value = uiState.username,
                    onValueChange = onUsernameChange,
                    icon = Icons.Outlined.AlternateEmail,
                    placeholder = "username",
                    enabled = !uiState.isSubmitting
                )
                EditUserStatusCard(
                    modifier = Modifier.weight(1f),
                    checked = uiState.isActive,
                    enabled = !uiState.isSubmitting,
                    onCheckedChange = onActiveChange
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EditUserSectionTitle("Role dan hak akses")
                EditUserRoleSelector(
                    roles = uiState.roles,
                    selectedRoleId = uiState.selectedRoleId,
                    isLoading = uiState.isLoadingRoles,
                    enabled = !uiState.isSubmitting,
                    onRoleChange = onRoleChange,
                    compact = compact
                )
                TextButton(
                    onClick = onShowAccessDetails,
                    enabled = !uiState.isLoadingRoles,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Outlined.Security, contentDescription = null, modifier = Modifier.size(18.dp), tint = EditPrimaryDark)
                    Spacer(Modifier.width(7.dp))
                    Text("Lihat rincian hak akses", color = EditPrimaryDark, fontWeight = FontWeight.SemiBold)
                }
            }

            HorizontalDivider(color = EditSlate200)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !uiState.isSubmitting,
                    modifier = if (compact) Modifier.weight(1f).height(52.dp) else Modifier.height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, EditSlate200),
                ) {
                    Text(text = "Batal", color = EditSlate600, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onSubmit,
                    enabled = !uiState.isSubmitting && !uiState.isLoadingUser && !uiState.isLoadingRoles,
                    modifier = if (compact) Modifier.weight(1f).height(52.dp) else Modifier.height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EditPrimary),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 28.dp)
                ) {
                    if (uiState.isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        text = if (uiState.isSubmitting) "Menyimpan..." else if (compact) "Simpan" else "Simpan perubahan",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun EditUserSectionTitle(title: String) {
    Text(
        text = title,
        color = EditOnSurface,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun EditUserInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = EditSlate600,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            placeholder = { Text(text = placeholder, color = EditSlate400) },
            leadingIcon = { Icon(imageVector = icon, contentDescription = null, tint = EditSlate400) },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = EditSurfaceLow,
                focusedContainerColor = EditSurfaceLow,
                disabledContainerColor = EditSurfaceLow,
                unfocusedBorderColor = EditSlate200,
                focusedBorderColor = EditPrimary
            ),
            singleLine = true
        )
    }
}

@Composable
private fun EditUserStatusCard(
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Status akun",
            color = EditSlate600,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(EditSurfaceLow)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (checked) EditEmerald50 else EditSlate200),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Badge,
                        contentDescription = null,
                        tint = if (checked) EditPrimary else EditSlate400,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = if (checked) "Aktif" else "Nonaktif",
                        color = if (checked) EditPrimaryDark else EditSlate500,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (checked) "Bisa login ke terminal" else "Akses login ditutup",
                        color = EditSlate500,
                        fontSize = 11.sp
                    )
                }
            }
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun EditUserRoleSelector(
    roles: List<UserRole>,
    selectedRoleId: String?,
    isLoading: Boolean,
    enabled: Boolean,
    onRoleChange: (String) -> Unit,
    compact: Boolean
) {
    when {
        isLoading -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(color = EditPrimary, modifier = Modifier.size(24.dp))
            Text(text = "Memuat role karyawan...", color = EditSlate500, fontSize = 13.sp)
        }
        roles.isEmpty() -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(EditSurfaceLow)
                .padding(16.dp)
        ) {
            Text(
                text = "Role karyawan belum tersedia. Role Owner disembunyikan dari halaman ini.",
                color = EditSlate500,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        else -> if (compact) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            roles.forEach { role ->
                val visual = role.editRoleVisual()
                EditUserRoleCard(role, selectedRoleId == role.id, enabled, visual.icon, visual.iconBackground, visual.iconTint, { onRoleChange(role.id) }, Modifier.fillMaxWidth())
            }
        } else Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            roles.forEach { role ->
                val visual = role.editRoleVisual()
                EditUserRoleCard(
                    role = role,
                    selected = selectedRoleId == role.id,
                    enabled = enabled,
                    icon = visual.icon,
                    iconBackground = visual.iconBackground,
                    iconTint = visual.iconTint,
                    onClick = { onRoleChange(role.id) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun EditUserRoleCard(
    role: UserRole,
    selected: Boolean,
    enabled: Boolean,
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (selected) EditEmerald50 else Color.White),
        border = BorderStroke(1.dp, if (selected) EditPrimary else EditSlate200),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Column {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = role.editDisplayTitle(),
                    color = EditOnSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = role.editDisplayDescription(),
                    color = EditSlate500,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EditPrimary,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }
    }
}

@Composable
private fun EditUserAccessSheet(
    selectedRole: UserRole?,
    rolesAreLoading: Boolean,
    accountIsActive: Boolean,
    onDismiss: () -> Unit,
) {
    TbMobileControlSheet(
        title = "Rincian hak akses",
        subtitle = "Status akun dan akses berdasarkan role",
        onDismiss = onDismiss,
        testTag = "edit-user-access-sheet",
    ) {
        AccountStatusSummary(isActive = accountIsActive)
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                rolesAreLoading -> EditAccessPanelFeedback(
                    message = "Menyiapkan role dari server...",
                    showProgress = true
                )
                selectedRole == null -> EditAccessPanelFeedback(
                    message = "Pilih role terlebih dahulu untuk melihat hak akses."
                )
                else -> EditSelectedRoleAccessSummary(role = selectedRole)
            }
        }
        TbMobileSheetDoneButton(
            onClick = onDismiss,
            label = "Tutup",
            testTag = "edit-user-access-done",
        )
    }
}

@Composable
private fun AccountStatusSummary(
    isActive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) EditEmerald50 else EditSurfaceLow)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.Lock,
                contentDescription = null,
                tint = if (isActive) EditPrimary else EditSlate400,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Text(
                text = if (isActive) "Akun aktif" else "Akun nonaktif",
                color = if (isActive) EditPrimaryDark else EditSlate600,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isActive) "Karyawan dapat login." else "Karyawan tidak dapat login.",
                color = EditSlate500,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun EditSelectedRoleAccessSummary(
    role: UserRole
) {
    val summary = role.editAccessSummary()
    val visual = role.editRoleVisual()

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(EditSurfaceLow)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(visual.iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = visual.icon,
                    contentDescription = null,
                    tint = visual.iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = summary.title,
                    color = EditOnSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = summary.description,
                    color = EditSlate500,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            summary.permissions.forEach { permission ->
                EditPermissionItem(permission = permission)
            }
        }
    }
}

@Composable
private fun EditPermissionItem(
    permission: EditRolePermission
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (permission.granted) EditEmerald50 else EditSurfaceLow)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (permission.granted) Icons.Default.Check else Icons.Default.Lock,
                contentDescription = null,
                tint = if (permission.granted) EditPrimary else EditSlate400,
                modifier = Modifier.size(14.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = permission.title,
                    color = if (permission.granted) EditOnSurface else EditSlate600,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = permission.level,
                    color = if (permission.granted) EditPrimary else EditSlate400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = permission.description,
                color = EditSlate500,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun EditAccessPanelFeedback(
    message: String,
    showProgress: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(EditSurfaceLow)
            .padding(horizontal = 18.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showProgress) {
            CircularProgressIndicator(color = EditPrimary, modifier = Modifier.size(26.dp))
        }
        Text(
            text = message,
            color = EditSlate500,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EditLoadingCard(
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, EditSlate200)
    ) {
        Row(
            modifier = Modifier.padding(28.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(color = EditPrimary, modifier = Modifier.size(26.dp))
            Text(text = message, color = EditSlate500, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun EditErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, EditSlate200)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            EditUserErrorBanner(message = message)
            OutlinedButton(onClick = onRetry) {
                Text(text = "Muat ulang")
            }
        }
    }
}

@Composable
private fun EditUserErrorBanner(
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(EditErrorContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = message,
            color = EditError,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private data class EditRoleVisual(
    val icon: ImageVector,
    val iconBackground: Color,
    val iconTint: Color
)

private data class EditRoleAccessSummary(
    val title: String,
    val description: String,
    val permissions: List<EditRolePermission>
)

private data class EditRolePermission(
    val title: String,
    val description: String,
    val level: String,
    val granted: Boolean
)

private fun UserRole.editRoleVisual(): EditRoleVisual {
    return when (name.trim().uppercase()) {
        "ADMIN" -> EditRoleVisual(
            icon = Icons.Outlined.AdminPanelSettings,
            iconBackground = EditPurple100,
            iconTint = EditPurple600
        )
        else -> EditRoleVisual(
            icon = Icons.Outlined.PointOfSale,
            iconBackground = EditBlue100,
            iconTint = EditBlue600
        )
    }
}

private fun UserRole.editDisplayTitle(): String {
    return name.lowercase().replaceFirstChar { first -> first.titlecase() }
}

private fun UserRole.editDisplayDescription(): String {
    return when (name.trim().uppercase()) {
        "ADMIN" -> "Manajemen inventory dan laporan dasar."
        "KASIR" -> "Transaksi POS dan akses operasional."
        else -> "Hak akses mengikuti konfigurasi backend."
    }
}

private fun UserRole.editAccessSummary(): EditRoleAccessSummary {
    return when (name.trim().uppercase()) {
        "ADMIN" -> EditRoleAccessSummary(
            title = "Admin",
            description = "Karyawan pengelola operasional toko di bawah owner.",
            permissions = listOf(
                EditRolePermission("Transaksi penjualan", "Membuat dan membaca transaksi POS.", "AKSES", true),
                EditRolePermission("Stok dan produk", "Membuat, mengubah, dan melihat data inventory.", "KELOLA", true),
                EditRolePermission("Pelanggan dan piutang", "Kelola pelanggan serta pembayaran piutang.", "KELOLA", true),
                EditRolePermission("Pembelian dan supplier", "Kelola pembelian dan kewajiban supplier.", "KELOLA", true),
                EditRolePermission("Laporan operasional", "Membaca analitik dan ringkasan toko.", "LIHAT", true),
                EditRolePermission("Manajemen pengguna", "Buat dan perbarui akun karyawan.", "TERBATAS", true)
            )
        )
        "KASIR" -> EditRoleAccessSummary(
            title = "Kasir",
            description = "Karyawan terminal depan untuk transaksi dan layanan pelanggan.",
            permissions = listOf(
                EditRolePermission("Transaksi penjualan", "Menjalankan POS dan membaca transaksi penjualan.", "AKSES", true),
                EditRolePermission("Stok dan produk", "Melihat katalog, satuan, dan ketersediaan stok.", "LIHAT", true),
                EditRolePermission("Pelanggan dan piutang", "Melihat pelanggan dan menerima pembayaran piutang.", "OPERASIONAL", true),
                EditRolePermission("Pembelian dan supplier", "Tidak dapat mengelola alur pembelian.", "TERKUNCI", false),
                EditRolePermission("Laporan operasional", "Tidak dapat membuka analitik manajemen.", "TERKUNCI", false),
                EditRolePermission("Manajemen pengguna", "Tidak dapat membuat atau mengubah akun staf.", "TERKUNCI", false)
            )
        )
        else -> EditRoleAccessSummary(
            title = editDisplayTitle(),
            description = "Role ini mengikuti aturan akses backend yang terpasang.",
            permissions = listOf(
                EditRolePermission("Akses role", "Izin rinci belum memiliki ringkasan frontend.", "BACKEND", true)
            )
        )
    }
}
