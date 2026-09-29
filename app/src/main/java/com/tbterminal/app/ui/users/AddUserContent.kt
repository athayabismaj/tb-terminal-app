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

private val AddUserSurface = TbBackground
private val AddUserSurfaceContainerLow = TbSurfaceMuted
private val AddUserOnSurface = TbText
private val AddUserPrimary = TbGreen
private val AddUserEmerald50 = TbGreenLight
private val AddUserEmerald100 = TbGreenLight
private val AddUserEmerald900 = TbGreenDark
private val AddUserSlate200 = TbOutline
private val AddUserSlate400 = Color(0xFF94A3B8)
private val AddUserSlate500 = TbTextMuted
private val AddUserSlate600 = TbTextMuted
private val AddUserBlue100 = Color(0xFFDBEAFE)
private val AddUserBlue600 = Color(0xFF2563EB)
private val AddUserPurple100 = Color(0xFFF3E8FF)
private val AddUserPurple600 = Color(0xFF9333EA)
private val AddUserError = TbError
private val AddUserErrorContainer = Color(0xFFFEE2E2)

@Composable
fun AddUserContent(
    modifier: Modifier = Modifier,
    uiState: AddUserUiState,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPinChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    onRetryRoles: () -> Unit,
    onCancel: () -> Unit = {},
    onSubmit: () -> Unit
) {
    val compact = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 700
    var showAccessDetails by remember { mutableStateOf(false) }
    val selectedRole = uiState.roles.firstOrNull { role -> role.id == uiState.selectedRoleId }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AddUserSurface)
            .padding(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 16.dp else 28.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            AddUserFormCard(
                uiState = uiState, onFullNameChange = onFullNameChange, onEmailChange = onEmailChange,
                onUsernameChange = onUsernameChange, onPasswordChange = onPasswordChange, onPinChange = onPinChange,
                onRoleChange = onRoleChange, onRetryRoles = onRetryRoles, onCancel = onCancel, onSubmit = onSubmit,
                onShowAccessDetails = { showAccessDetails = true },
                modifier = Modifier.fillMaxWidth().widthIn(max = 860.dp),
                compact = compact,
            )
        }
    }

    if (showAccessDetails) {
        AddUserAccessSheet(
            selectedRole = selectedRole,
            rolesAreLoading = uiState.isLoadingRoles,
            onDismiss = { showAccessDetails = false },
        )
    }
}

@Composable
private fun AddUserFormCard(
    uiState: AddUserUiState,
    onFullNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onPinChange: (String) -> Unit,
    onRoleChange: (String) -> Unit,
    onRetryRoles: () -> Unit,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
    onShowAccessDetails: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, AddUserSlate200)
    ) {
        Column(modifier = Modifier.padding(if (compact) 16.dp else 28.dp)) {
            if (uiState.errorMessage != null) {
                AddUserErrorBanner(message = uiState.errorMessage)
                Spacer(modifier = Modifier.height(20.dp))
            }
            AddUserSectionTitle("Informasi pengguna")
            Spacer(modifier = Modifier.height(14.dp))
            if (compact) Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AddUserInputField(label = "Nama lengkap", value = uiState.fullName, onValueChange = onFullNameChange, icon = Icons.Outlined.Person, placeholder = "Contoh: Budi Santoso", modifier = Modifier.fillMaxWidth())
                AddUserInputField(label = "Alamat email", value = uiState.email, onValueChange = onEmailChange, icon = Icons.Outlined.Email, placeholder = "contoh@email.com", modifier = Modifier.fillMaxWidth(), keyboardType = KeyboardType.Email)
            } else Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                AddUserInputField(
                    modifier = Modifier.weight(1f),
                    label = "Nama lengkap",
                    value = uiState.fullName,
                    onValueChange = onFullNameChange,
                    icon = Icons.Outlined.Person,
                    placeholder = "Contoh: Budi Santoso"
                )
                AddUserInputField(
                    modifier = Modifier.weight(1f),
                    label = "Alamat email",
                    value = uiState.email,
                    onValueChange = onEmailChange,
                    icon = Icons.Outlined.Email,
                    placeholder = "contoh@email.com",
                    keyboardType = KeyboardType.Email
                )
            }
            Spacer(modifier = Modifier.height(26.dp))
            AddUserSectionTitle("Akses masuk")
            Spacer(modifier = Modifier.height(14.dp))
            if (compact) Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AddUserInputField(label = "Username", value = uiState.username, onValueChange = onUsernameChange, icon = Icons.Outlined.AlternateEmail, placeholder = "budisan88", modifier = Modifier.fillMaxWidth())
                AddUserInputField(label = "Password", value = uiState.password, onValueChange = onPasswordChange, icon = Icons.Outlined.Lock, placeholder = "Minimal 6 karakter", modifier = Modifier.fillMaxWidth(), isPassword = true)
                AddUserInputField(label = "PIN akses", value = uiState.pin, onValueChange = onPinChange, icon = Icons.Outlined.Lock, placeholder = "6 digit PIN", modifier = Modifier.fillMaxWidth(), isPassword = true, keyboardType = KeyboardType.NumberPassword)
            } else Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                AddUserInputField(
                    modifier = Modifier.weight(1f),
                    label = "Username",
                    value = uiState.username,
                    onValueChange = onUsernameChange,
                    icon = Icons.Outlined.AlternateEmail,
                    placeholder = "budisan88"
                )
                AddUserInputField(
                    modifier = Modifier.weight(1f),
                    label = "Password",
                    value = uiState.password,
                    onValueChange = onPasswordChange,
                    icon = Icons.Outlined.Lock,
                    placeholder = "Minimal 6 karakter",
                    isPassword = true,
                )
            }
            if (!compact) {
                Spacer(modifier = Modifier.height(16.dp))
                AddUserInputField(
                    modifier = Modifier.fillMaxWidth(0.5f),
                    label = "PIN akses",
                    value = uiState.pin,
                    onValueChange = onPinChange,
                    icon = Icons.Outlined.Lock,
                    placeholder = "6 digit PIN",
                    isPassword = true,
                    keyboardType = KeyboardType.NumberPassword,
                )
            }

            Spacer(modifier = Modifier.height(26.dp))
            AddUserSectionTitle("Role dan hak akses")
            Spacer(modifier = Modifier.height(14.dp))
            AddUserRoleSelector(
                roles = uiState.roles,
                selectedRoleId = uiState.selectedRoleId,
                isLoading = uiState.isLoadingRoles,
                onRoleChange = onRoleChange,
                onRetry = onRetryRoles,
                compact = compact
            )
            TextButton(
                onClick = onShowAccessDetails,
                enabled = !uiState.isLoadingRoles,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            ) {
                Icon(Icons.Outlined.Security, contentDescription = null, modifier = Modifier.size(18.dp), tint = AddUserEmerald900)
                Spacer(Modifier.width(7.dp))
                Text("Lihat rincian hak akses", color = AddUserEmerald900, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(28.dp))
            HorizontalDivider(color = AddUserSlate200)
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = if (compact) Modifier.weight(1f).height(52.dp) else Modifier.height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, AddUserSlate200),
                ) {
                    Text(text = "Batal", color = AddUserSlate600, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onSubmit,
                    enabled = !uiState.isSubmitting && !uiState.isLoadingRoles,
                    modifier = if (compact) Modifier.weight(1f).height(52.dp) else Modifier.height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AddUserPrimary),
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
                        text = if (uiState.isSubmitting) "Menyimpan..." else if (compact) "Simpan" else "Simpan pengguna",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AddUserSectionTitle(title: String) {
    Text(
        text = title,
        color = AddUserOnSurface,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun AddUserInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = AddUserSlate600,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(text = placeholder, color = AddUserSlate400) },
            leadingIcon = { Icon(imageVector = icon, contentDescription = null, tint = AddUserSlate400) },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = AddUserSurfaceContainerLow,
                focusedContainerColor = AddUserSurfaceContainerLow,
                unfocusedBorderColor = AddUserSlate200,
                focusedBorderColor = AddUserPrimary
            ),
            singleLine = true
        )
    }
}

@Composable
private fun AddUserErrorBanner(
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AddUserErrorContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = message,
            color = AddUserError,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AddUserRoleSelector(
    roles: List<UserRole>,
    selectedRoleId: String?,
    isLoading: Boolean,
    onRoleChange: (String) -> Unit,
    onRetry: () -> Unit,
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
            CircularProgressIndicator(
                color = AddUserPrimary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "Memuat role dari server...",
                color = AddUserSlate500,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        roles.isEmpty() -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AddUserSurfaceContainerLow)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Role pengguna belum tersedia.",
                color = AddUserSlate600,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedButton(onClick = onRetry) {
                Text(text = "Muat ulang role")
            }
        }
        else -> if (compact) Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            roles.forEach { role ->
                val visual = role.visual()
                AddUserRoleCard(role, selectedRoleId == role.id, visual.icon, visual.iconBackground, visual.iconTint, { onRoleChange(role.id) }, Modifier.fillMaxWidth())
            }
        } else Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            roles.forEach { role ->
                val visual = role.visual()
                AddUserRoleCard(
                    role = role,
                    isSelected = selectedRoleId == role.id,
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
private fun AddUserRoleCard(
    role: UserRole,
    isSelected: Boolean,
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) AddUserEmerald50 else Color.White
        ),
        border = BorderStroke(1.dp, if (isSelected) AddUserPrimary else AddUserSlate200)
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
                    text = role.displayTitle(),
                    color = AddUserOnSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = role.displayDescription(),
                    color = AddUserSlate500,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = AddUserPrimary,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
        }
    }
}

private data class AddUserRoleVisual(
    val icon: ImageVector,
    val iconBackground: Color,
    val iconTint: Color
)

private fun UserRole.visual(): AddUserRoleVisual {
    return when (name.trim().uppercase()) {
        "ADMIN" -> AddUserRoleVisual(
            icon = Icons.Outlined.AdminPanelSettings,
            iconBackground = AddUserPurple100,
            iconTint = AddUserPurple600
        )
        else -> AddUserRoleVisual(
            icon = Icons.Outlined.PointOfSale,
            iconBackground = AddUserBlue100,
            iconTint = AddUserBlue600
        )
    }
}

private fun UserRole.displayTitle(): String {
    return name.lowercase().replaceFirstChar { first ->
        first.titlecase()
    }
}

private fun UserRole.displayDescription(): String {
    return when (name.trim().uppercase()) {
        "ADMIN" -> "Manajemen inventory dan laporan dasar."
        "KASIR" -> "Akses transaksi penjualan dan stok harian."
        else -> "Akses mengikuti role yang dipilih."
    }
}

@Composable
private fun AddUserAccessSheet(
    selectedRole: UserRole?,
    rolesAreLoading: Boolean,
    onDismiss: () -> Unit,
) {
    TbMobileControlSheet(
        title = "Rincian hak akses",
        subtitle = "Akses mengikuti role yang dipilih",
        onDismiss = onDismiss,
        testTag = "add-user-access-sheet",
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                rolesAreLoading -> AccessPanelFeedback(
                    message = "Menyiapkan role dari server...",
                    showProgress = true
                )
                selectedRole == null -> AccessPanelFeedback(
                    message = "Pilih role terlebih dahulu untuk melihat hak akses."
                )
                else -> SelectedRoleAccessSummary(role = selectedRole)
            }
        }
        TbMobileSheetDoneButton(
            onClick = onDismiss,
            label = "Tutup",
            testTag = "add-user-access-done",
        )
    }
}

@Composable
private fun SelectedRoleAccessSummary(
    role: UserRole
) {
    val summary = role.accessSummary()
    val visual = role.visual()

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AddUserSurfaceContainerLow)
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
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = summary.title,
                    color = AddUserOnSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = summary.description,
                    color = AddUserSlate500,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            summary.permissions.forEach { permission ->
                PermissionItem(permission = permission)
            }
        }
    }
}

@Composable
private fun AccessPanelFeedback(
    message: String,
    showProgress: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AddUserSurfaceContainerLow)
            .padding(horizontal = 18.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showProgress) {
            CircularProgressIndicator(
                color = AddUserPrimary,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = message,
            color = AddUserSlate500,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PermissionItem(
    permission: RolePermission
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (permission.granted) AddUserEmerald50 else AddUserSurfaceContainerLow)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (permission.granted) Color.White else AddUserSlate200),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (permission.granted) Icons.Default.Check else Icons.Default.Lock,
                contentDescription = null,
                tint = if (permission.granted) AddUserPrimary else AddUserSlate400,
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
                    color = if (permission.granted) AddUserOnSurface else AddUserSlate600,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = permission.level,
                    color = if (permission.granted) AddUserPrimary else AddUserSlate400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = permission.description,
                color = AddUserSlate500,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

private data class RoleAccessSummary(
    val title: String,
    val description: String,
    val permissions: List<RolePermission>
)

private data class RolePermission(
    val title: String,
    val description: String,
    val level: String,
    val granted: Boolean
)

private fun UserRole.accessSummary(): RoleAccessSummary {
    return when (name.trim().uppercase()) {
        "ADMIN" -> RoleAccessSummary(
            title = "Admin",
            description = "Role pengelola operasional toko di bawah owner.",
            permissions = listOf(
                RolePermission("Transaksi penjualan", "Membuat dan membaca transaksi POS.", "AKSES", true),
                RolePermission("Stok dan produk", "Membuat, mengubah, dan melihat data inventory.", "KELOLA", true),
                RolePermission("Pelanggan dan piutang", "Kelola pelanggan serta pembayaran piutang.", "KELOLA", true),
                RolePermission("Pembelian dan supplier", "Kelola pembelian dan kewajiban supplier.", "KELOLA", true),
                RolePermission("Laporan operasional", "Membaca analitik dan ringkasan toko.", "LIHAT", true),
                RolePermission("Manajemen pengguna", "Buat dan perbarui akun. Hapus user tetap khusus owner.", "TERBATAS", true)
            )
        )
        "KASIR" -> RoleAccessSummary(
            title = "Kasir",
            description = "Role terminal depan untuk transaksi dan layanan pelanggan.",
            permissions = listOf(
                RolePermission("Transaksi penjualan", "Menjalankan POS dan membaca transaksi penjualan.", "AKSES", true),
                RolePermission("Stok dan produk", "Melihat katalog, satuan, dan ketersediaan stok.", "LIHAT", true),
                RolePermission("Pelanggan dan piutang", "Melihat pelanggan dan menerima pembayaran piutang.", "OPERASIONAL", true),
                RolePermission("Pembelian dan supplier", "Tidak dapat mengelola alur pembelian.", "TERKUNCI", false),
                RolePermission("Laporan operasional", "Tidak dapat membuka analitik manajemen.", "TERKUNCI", false),
                RolePermission("Manajemen pengguna", "Tidak dapat membuat atau mengubah akun staf.", "TERKUNCI", false)
            )
        )
        else -> RoleAccessSummary(
            title = displayTitle(),
            description = "Role ini mengikuti aturan akses backend yang terpasang.",
            permissions = listOf(
                RolePermission("Akses role", "Izin rinci belum memiliki ringkasan frontend.", "BACKEND", true)
            )
        )
    }
}
