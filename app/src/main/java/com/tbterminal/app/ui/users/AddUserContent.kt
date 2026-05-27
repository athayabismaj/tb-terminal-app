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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.AlternateEmail
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.UserRole

private val AddUserSurface = Color(0xFFF4FAFD)
private val AddUserSurfaceContainerLow = Color(0xFFEEF5F7)
private val AddUserOnSurface = Color(0xFF161D1F)
private val AddUserPrimary = Color(0xFF1D9E75)
private val AddUserEmerald50 = Color(0xFFECFDF5)
private val AddUserEmerald100 = Color(0xFFD1FAE5)
private val AddUserEmerald900 = Color(0xFF064E3B)
private val AddUserSlate200 = Color(0xFFE2E8F0)
private val AddUserSlate400 = Color(0xFF94A3B8)
private val AddUserSlate500 = Color(0xFF64748B)
private val AddUserSlate600 = Color(0xFF475569)
private val AddUserBlue100 = Color(0xFFDBEAFE)
private val AddUserBlue600 = Color(0xFF2563EB)
private val AddUserPurple100 = Color(0xFFF3E8FF)
private val AddUserPurple600 = Color(0xFF9333EA)
private val AddUserError = Color(0xFFB91C1C)
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AddUserSurface)
            .padding(horizontal = 32.dp, vertical = 28.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Column(modifier = Modifier.padding(bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Manajemen Pengguna", color = AddUserSlate500, fontSize = 14.sp)
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = AddUserSlate500,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Tambah User Baru",
                    color = AddUserPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Informasi Akun",
                color = AddUserOnSurface,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Lengkapi data di bawah ini untuk menambahkan akses baru ke sistem TB Terminal.",
                color = AddUserSlate500,
                fontSize = 14.sp
            )
        }

        val selectedRole = uiState.roles.firstOrNull { role ->
            role.id == uiState.selectedRoleId
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.Top
        ) {
            AddUserFormCard(
                uiState = uiState,
                onFullNameChange = onFullNameChange,
                onEmailChange = onEmailChange,
                onUsernameChange = onUsernameChange,
                onPasswordChange = onPasswordChange,
                onPinChange = onPinChange,
                onRoleChange = onRoleChange,
                onRetryRoles = onRetryRoles,
                onCancel = onCancel,
                onSubmit = onSubmit,
                modifier = Modifier.weight(1.2f)
            )
            AddUserAccessPanel(
                selectedRole = selectedRole,
                rolesAreLoading = uiState.isLoadingRoles,
                modifier = Modifier.weight(0.8f)
            )
        }
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AddUserSlate200)
    ) {
        Column(modifier = Modifier.padding(28.dp)) {
            if (uiState.errorMessage != null) {
                AddUserErrorBanner(message = uiState.errorMessage)
                Spacer(modifier = Modifier.height(20.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                AddUserInputField(
                    modifier = Modifier.weight(1f),
                    label = "NAMA LENGKAP",
                    value = uiState.fullName,
                    onValueChange = onFullNameChange,
                    icon = Icons.Outlined.Person,
                    placeholder = "Contoh: Budi Santoso"
                )
                AddUserInputField(
                    modifier = Modifier.weight(1f),
                    label = "ALAMAT EMAIL",
                    value = uiState.email,
                    onValueChange = onEmailChange,
                    icon = Icons.Outlined.Email,
                    placeholder = "contoh@email.com",
                    keyboardType = KeyboardType.Email
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                AddUserInputField(
                    modifier = Modifier.weight(1f),
                    label = "USERNAME",
                    value = uiState.username,
                    onValueChange = onUsernameChange,
                    icon = Icons.Outlined.AlternateEmail,
                    placeholder = "budisan88"
                )
                Column(modifier = Modifier.weight(1f)) {
                    AddUserInputField(
                        label = "PASSWORD",
                        value = uiState.password,
                        onValueChange = onPasswordChange,
                        icon = Icons.Outlined.Lock,
                        placeholder = "Masukkan password",
                        isPassword = true
                    )
                    Text(
                        text = "Minimal 6 karakter angka atau huruf.",
                        color = AddUserSlate400,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                AddUserInputField(
                    modifier = Modifier.weight(1f),
                    label = "PIN AKSES",
                    value = uiState.pin,
                    onValueChange = onPinChange,
                    icon = Icons.Outlined.Lock,
                    placeholder = "6 digit PIN",
                    isPassword = true,
                    keyboardType = KeyboardType.NumberPassword
                )
                AccountSecurityNote(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "ROLE / HAK AKSES",
                color = AddUserSlate600,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            AddUserRoleSelector(
                roles = uiState.roles,
                selectedRoleId = uiState.selectedRoleId,
                isLoading = uiState.isLoadingRoles,
                onRoleChange = onRoleChange,
                onRetry = onRetryRoles
            )

            Spacer(modifier = Modifier.height(28.dp))
            HorizontalDivider(color = AddUserSlate200)
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text(text = "Batal", color = AddUserSlate600, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = onSubmit,
                    enabled = !uiState.isSubmitting && !uiState.isLoadingRoles,
                    colors = ButtonDefaults.buttonColors(containerColor = AddUserPrimary),
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
                        text = if (uiState.isSubmitting) "Menyimpan..." else "Simpan User",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
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
            shape = RoundedCornerShape(8.dp),
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
    onRetry: () -> Unit
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
        else -> Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) AddUserEmerald50 else Color.White
        ),
        border = BorderStroke(2.dp, if (isSelected) AddUserPrimary else AddUserSlate200)
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
private fun AccountSecurityNote(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "KEAMANAN SESI",
            color = AddUserSlate600,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AddUserSurfaceContainerLow)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(AddUserEmerald50),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = AddUserPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Kredensial sesi",
                    color = AddUserOnSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "PIN diminta saat terminal dibuka kembali.",
                    color = AddUserSlate500,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun AddUserAccessPanel(
    selectedRole: UserRole?,
    rolesAreLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AddUserSlate200)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Ringkasan Hak Akses",
                    color = AddUserOnSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Akses akan mengikuti role yang dipilih untuk akun ini.",
                    color = AddUserSlate500,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

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
