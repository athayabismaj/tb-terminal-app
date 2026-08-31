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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PointOfSale
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

private val EditSurface = Color(0xFFF4FAFD)
private val EditSurfaceLow = Color(0xFFEEF5F7)
private val EditOnSurface = Color(0xFF161D1F)
private val EditPrimary = Color(0xFF1D9E75)
private val EditPrimaryDark = Color(0xFF059669)
private val EditEmerald50 = Color(0xFFECFDF5)
private val EditSlate200 = Color(0xFFE2E8F0)
private val EditSlate400 = Color(0xFF94A3B8)
private val EditSlate500 = Color(0xFF64748B)
private val EditSlate600 = Color(0xFF475569)
private val EditBlue100 = Color(0xFFDBEAFE)
private val EditBlue600 = Color(0xFF2563EB)
private val EditPurple100 = Color(0xFFF3E8FF)
private val EditPurple600 = Color(0xFF9333EA)
private val EditError = Color(0xFFB91C1C)
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
    onChangePasswordClick: () -> Unit,
    onChangePinClick: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onSubmit: () -> Unit
) {
    val compact = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 700
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EditSurface)
            .padding(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 16.dp else 28.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Column(modifier = Modifier.padding(bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Manajemen Pengguna", color = EditSlate500, fontSize = 14.sp)
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = EditSlate500,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Edit User",
                    color = EditPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Edit Akun Karyawan",
                color = EditOnSurface,
                fontSize = if (compact) 24.sp else 32.sp,
                fontWeight = FontWeight.Bold
            )
            if (!compact) Text(
                text = "Perbarui data staf, status akun, kredensial opsional, dan hak akses karyawan.",
                color = EditSlate500,
                fontSize = 14.sp
            )
        }

        when {
            uiState.isLoadingUser -> EditLoadingCard(message = "Memuat data user...")
            uiState.errorMessage != null && uiState.fullName.isBlank() -> {
                EditErrorCard(message = uiState.errorMessage, onRetry = onRetry)
            }
            else -> {
                val editContent: @Composable (Modifier, Modifier) -> Unit = { formModifier, panelModifier ->
                    EditUserFormCard(
                        uiState, onFullNameChange, onEmailChange, onUsernameChange, onRoleChange, onActiveChange,
                        onCancel, onSubmit, formModifier, compact
                    )
                    EditUserAccessPanel(uiState.selectedRole, uiState.isLoadingRoles, uiState.isActive, onChangePasswordClick, onChangePinClick, panelModifier)
                }
                if (compact) Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    editContent(Modifier.fillMaxWidth(), Modifier.fillMaxWidth())
                } else Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    editContent(Modifier.weight(1.2f), Modifier.weight(0.8f))
                }
            }
        }
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
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, EditSlate200)
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 16.dp else 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (uiState.errorMessage != null) {
                EditUserErrorBanner(message = uiState.errorMessage)
            }

            if (compact) Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EditUserInputField("NAMA LENGKAP", uiState.fullName, onFullNameChange, Icons.Outlined.Person, "Nama karyawan", Modifier.fillMaxWidth(), enabled = !uiState.isSubmitting)
                EditUserInputField("ALAMAT EMAIL", uiState.email, onEmailChange, Icons.Outlined.Email, "contoh@email.com", Modifier.fillMaxWidth(), keyboardType = KeyboardType.Email, enabled = !uiState.isSubmitting)
            } else Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                EditUserInputField(
                    modifier = Modifier.weight(1f),
                    label = "NAMA LENGKAP",
                    value = uiState.fullName,
                    onValueChange = onFullNameChange,
                    icon = Icons.Outlined.Person,
                    placeholder = "Nama karyawan",
                    enabled = !uiState.isSubmitting
                )
                EditUserInputField(
                    modifier = Modifier.weight(1f),
                    label = "ALAMAT EMAIL",
                    value = uiState.email,
                    onValueChange = onEmailChange,
                    icon = Icons.Outlined.Email,
                    placeholder = "contoh@email.com",
                    keyboardType = KeyboardType.Email,
                    enabled = !uiState.isSubmitting
                )
            }

            if (compact) Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                EditUserInputField("USERNAME", uiState.username, onUsernameChange, Icons.Outlined.AlternateEmail, "username", Modifier.fillMaxWidth(), enabled = !uiState.isSubmitting)
                EditUserStatusCard(uiState.isActive, !uiState.isSubmitting, onActiveChange, Modifier.fillMaxWidth())
            } else Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                EditUserInputField(
                    modifier = Modifier.weight(1f),
                    label = "USERNAME",
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
                Text(
                    text = "ROLE / HAK AKSES",
                    color = EditSlate600,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                EditUserRoleSelector(
                    roles = uiState.roles,
                    selectedRoleId = uiState.selectedRoleId,
                    isLoading = uiState.isLoadingRoles,
                    enabled = !uiState.isSubmitting,
                    onRoleChange = onRoleChange,
                    compact = compact
                )
            }

            HorizontalDivider(color = EditSlate200)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onCancel,
                    enabled = !uiState.isSubmitting
                ) {
                    Text(text = "Batal", color = EditSlate600, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = onSubmit,
                    enabled = !uiState.isSubmitting && !uiState.isLoadingUser && !uiState.isLoadingRoles,
                    colors = ButtonDefaults.buttonColors(containerColor = EditPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
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
                        text = if (uiState.isSubmitting) "Menyimpan..." else "Simpan Perubahan",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
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
            shape = RoundedCornerShape(8.dp),
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
            text = "STATUS AKUN",
            color = EditSlate600,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(8.dp))
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
        border = BorderStroke(2.dp, if (selected) EditPrimary else EditSlate200),
        shape = RoundedCornerShape(12.dp)
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
private fun EditUserAccessPanel(
    selectedRole: UserRole?,
    rolesAreLoading: Boolean,
    accountIsActive: Boolean,
    onChangePasswordClick: () -> Unit,
    onChangePinClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, EditSlate200)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Ringkasan Hak Akses",
                    color = EditOnSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Role Owner sengaja tidak ditampilkan karena halaman ini khusus akun karyawan.",
                    color = EditSlate500,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            AccountStatusSummary(isActive = accountIsActive)

            CredentialActionSummary(
                onChangePasswordClick = onChangePasswordClick,
                onChangePinClick = onChangePinClick
            )

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
    }
}

@Composable
private fun CredentialActionSummary(
    onChangePasswordClick: () -> Unit,
    onChangePinClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Kredensial Akses",
            color = EditOnSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Password dan PIN dikelola di halaman terpisah agar audit perubahan lebih jelas.",
            color = EditSlate500,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
        CredentialActionButton(
            title = "Ubah Password",
            description = "Ganti kredensial login akun karyawan.",
            icon = Icons.Default.Password,
            onClick = onChangePasswordClick
        )
        CredentialActionButton(
            title = "Ubah PIN",
            description = "Ganti PIN 6 digit untuk membuka terminal.",
            icon = Icons.Default.CreditCard,
            onClick = onChangePinClick
        )
    }
}

@Composable
private fun CredentialActionButton(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(EditSurfaceLow)
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(EditEmerald50),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EditPrimary,
                modifier = Modifier.size(19.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = EditOnSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = EditSlate500,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = EditSlate400,
            modifier = Modifier.size(18.dp)
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
        shape = RoundedCornerShape(12.dp),
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
        shape = RoundedCornerShape(12.dp),
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
