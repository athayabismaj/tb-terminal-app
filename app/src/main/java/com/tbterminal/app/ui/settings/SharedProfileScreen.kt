package com.tbterminal.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.tbterminal.app.data.model.UserProfile
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.components.TopFloatingCapsule
import java.util.Locale

@Composable
fun SharedProfileScreen(
    uiState: ProfileUiState,
    onReload: () -> Unit,
    onChangePassword: (String, String, String) -> Unit,
    onChangePin: (String, String, String) -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    pageTitle: String = "Profil Kasir",
    onBack: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    onEditProfile: (() -> Unit)? = null,
) {
    var dialog by remember { mutableStateOf<CredentialDialog?>(null) }
    var confirmLogout by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.message) {
        if (uiState.message != null) dialog = null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        if (showHeader) {
            ProfileScreenHeader(
                title = pageTitle,
                onBack = onBack,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 440.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (uiState.error != null && uiState.profile == null && !uiState.isLoading) {
                    OutlinedButton(onClick = onReload) { Text("Coba lagi") }
                }

                val currentProfile = uiState.profile
                when {
                    uiState.isLoading && currentProfile == null -> ProfileLoadingState(compact = true)
                    currentProfile != null -> ProfileContent(
                        profile = currentProfile,
                        isSaving = uiState.isSaving,
                        onPassword = { dialog = CredentialDialog.PASSWORD },
                        onPin = { dialog = CredentialDialog.PIN },
                        onLogout = onLogout?.let { { confirmLogout = true } },
                        onEditProfile = onEditProfile,
                    )
                    else -> Text(
                        "Profil tidak tersedia.",
                        color = Color(0xFF64748B),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                if (uiState.profile == null && onLogout != null) {
                    AccountLogoutButton(!uiState.isSaving) { confirmLogout = true }
                }

                Spacer(modifier = Modifier.weight(1f, fill = false))

                // Home indicator bar at bottom
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(top = 8.dp, bottom = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 128.dp, height = 4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFCBD5E1)),
                    )
                }
            }

            TopFloatingCapsule(
                message = uiState.message,
                error = uiState.error,
                onDismiss = onClearMessage,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                    .zIndex(99f),
            )
        }
    }

    if (confirmLogout && onLogout != null) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Keluar dari akun?") },
            text = { Text("Anda perlu masuk kembali untuk menggunakan aplikasi.") },
            confirmButton = {
                TextButton(
                    onClick = { confirmLogout = false; onLogout() },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.testTag("account-confirm-logout"),
                ) { Text("Keluar") }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Batal") } },
        )
    }

    dialog?.let { type ->
        CredentialChangeDialog(
            type = type,
            isSaving = uiState.isSaving,
            errorMessage = uiState.error,
            onInputChanged = onClearMessage,
            onDismiss = { if (!uiState.isSaving) dialog = null },
            onSubmit = { oldValue, newValue, confirmation ->
                if (type == CredentialDialog.PASSWORD) {
                    onChangePassword(oldValue, newValue, confirmation)
                } else {
                    onChangePin(oldValue, newValue, confirmation)
                }
            }
        )
    }
}

@Composable
private fun ProfileScreenHeader(
    title: String = "Profil Kasir",
    onBack: (() -> Unit)? = null,
) {
    Surface(
        color = Color.White.copy(alpha = 0.95f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color(0xFF334155),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    letterSpacing = (-0.2).sp,
                )
            }
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
        }
    }
}

@Composable
private fun ProfileLoadingState(compact: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("profile-skeleton"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.8f)),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SkeletonBox(Modifier.size(48.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SkeletonBox(Modifier.fillMaxWidth(0.5f).height(18.dp))
                        SkeletonBox(Modifier.fillMaxWidth(0.7f).height(12.dp))
                    }
                }
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    SkeletonBox(Modifier.width(60.dp).height(12.dp))
                    SkeletonBox(Modifier.width(70.dp).height(14.dp))
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.8f)),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(3) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        SkeletonBox(Modifier.size(20.dp))
                        SkeletonBox(Modifier.weight(1f).height(14.dp))
                        SkeletonBox(Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(
    profile: UserProfile,
    isSaving: Boolean,
    onPassword: () -> Unit,
    onPin: () -> Unit,
    onLogout: (() -> Unit)? = null,
    onEditProfile: (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ProfileIdentityCard(profile = profile, onEditProfile = onEditProfile)
        ProfileSecurityCard(
            isSaving = isSaving,
            onPassword = onPassword,
            onPin = onPin,
            onEditProfile = onEditProfile,
        )
        if (onLogout != null) {
            AccountLogoutButton(!isSaving, onLogout)
        }
    }
}

@Composable
internal fun AccountLogoutButton(enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFFECDD3)), // rose-200
        shadowElevation = 0.5.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("account-logout"),
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                tint = Color(0xFFE11D48), // rose-600
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Keluar",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE11D48), // rose-600
            )
        }
    }
}

@Composable
internal fun ProfileIdentityCard(
    profile: UserProfile,
    compact: Boolean = true,
    onEditProfile: (() -> Unit)? = null,
) {
    val initials = remember(profile.name) {
        val parts = profile.name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (parts.size >= 2) {
            "${parts[0].take(1)}${parts[1].take(1)}".uppercase(Locale.ROOT)
        } else {
            profile.name.take(2).uppercase(Locale.ROOT).ifEmpty { "K" }
        }
    }

    val roleText = remember(profile.role) {
        val upper = profile.role.uppercase(Locale.ROOT)
        when {
            upper.contains("KASIR") -> "Kasir"
            upper.contains("PEMILIK") || upper.contains("OWNER") -> "Pemilik Toko"
            upper.contains("ADMIN") -> "Admin"
            else -> profile.role.replaceFirstChar { it.uppercase() }
        }
    }

    val emailText = remember(profile) {
        profile.email?.takeIf { it.isNotBlank() } ?: "${profile.username}@gmail.com"
    }

    val cashierId = remember(profile) {
        val digits = profile.id?.filter { it.isDigit() }.orEmpty()
        val num = digits.takeLast(3).padStart(3, '0').ifEmpty { "001" }
        "KSR-$num"
    }

    val idLabel = if (roleText.contains("Kasir", ignoreCase = true)) "ID Kasir" else "ID Pengguna"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile-identity"),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.8f)),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, Color(0xFFE2E8F0), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initials,
                        color = Color(0xFF334155),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = profile.name,
                        color = Color(0xFF0F172A),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = roleText,
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "•",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                        )
                        Text(
                            text = emailText,
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 16.dp, bottom = 14.dp),
                color = Color(0xFFF1F5F9),
                thickness = 1.dp,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = idLabel.uppercase(Locale.ROOT),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 1.sp,
                )
                Text(
                    text = cashierId,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E293B),
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

@Composable
private fun ProfileSecurityCard(
    isSaving: Boolean,
    onPassword: () -> Unit,
    onPin: () -> Unit,
    onEditProfile: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("profile-security"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "KEAMANAN & AKUN",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(start = 4.dp),
        )
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.8f)),
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                ProfileSecurityActionRow(
                    label = "Ubah kata sandi",
                    icon = Icons.Outlined.Lock,
                    enabled = !isSaving,
                    onClick = onPassword,
                    modifier = Modifier.testTag("profile-action-password"),
                )
                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                ProfileSecurityActionRow(
                    label = "Ubah PIN Transaksi",
                    icon = Icons.Outlined.Password,
                    enabled = !isSaving,
                    onClick = onPin,
                    modifier = Modifier.testTag("profile-action-pin"),
                )
                if (onEditProfile != null) {
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    ProfileSecurityActionRow(
                        label = "Ubah informasi kasir",
                        icon = Icons.Outlined.Person,
                        enabled = !isSaving,
                        onClick = onEditProfile,
                        modifier = Modifier.testTag("profile-action-edit"),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileSecurityActionRow(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1E293B),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private enum class CredentialDialog { PASSWORD, PIN }


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CredentialChangeDialog(
    type: CredentialDialog,
    isSaving: Boolean,
    errorMessage: String?,
    onInputChanged: () -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var oldValue by remember(type) { mutableStateOf("") }
    var newValue by remember(type) { mutableStateOf("") }
    var confirmation by remember(type) { mutableStateOf("") }
    val isPin = type == CredentialDialog.PIN
    val title = if (isPin) "Ubah PIN Transaksi" else "Ubah Kata Sandi"
    val subtitle = if (isPin) "Pastikan PIN baru mudah diingat namun sulit ditebak." else "Gunakan kata sandi yang kuat untuk melindungi akun Anda."
    val keyboard = if (isPin) KeyboardType.NumberPassword else KeyboardType.Password
    val currentLabel = if (isPin) "PIN Saat Ini" else "Kata Sandi Saat Ini"
    val newLabel = if (isPin) "PIN Baru" else "Kata Sandi Baru"
    val confirmationLabel = if (isPin) "Konfirmasi PIN Baru" else "Konfirmasi Kata Sandi Baru"
    val formReady = oldValue.isNotBlank() && newValue.isNotBlank() && confirmation.isNotBlank() &&
        (!isPin || (oldValue.length in 4..6 && newValue.length in 4..6 && confirmation.length in 4..6))

    val isOldValueError = errorMessage?.let {
        it.contains("lama", ignoreCase = true) || it.contains("saat ini", ignoreCase = true)
    } == true
    val isNewValueError = errorMessage?.let {
        (it.contains("baru", ignoreCase = true) && !it.contains("konfirmasi", ignoreCase = true) && !it.contains("sama", ignoreCase = true)) ||
        it.contains("minimal", ignoreCase = true) || it.contains("digit", ignoreCase = true) || it.contains("karakter", ignoreCase = true)
    } == true
    val isConfirmationError = errorMessage?.let {
        it.contains("konfirmasi", ignoreCase = true) || it.contains("tidak sama", ignoreCase = true) || it.contains("tidak cocok", ignoreCase = true)
    } == true
    val isGeneralError = errorMessage != null && !isOldValueError && !isNewValueError && !isConfirmationError

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = Color.White,
        scrimColor = Color(0xFF0F172A).copy(alpha = 0.6f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE2E8F0))
            )
        },
        modifier = Modifier.testTag("credential-dialog")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Modal Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        lineHeight = 20.sp
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    enabled = !isSaving,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Modal Body Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Policy / Info Card (matching ShiftClosure expectation card style)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC).copy(alpha = 0.7f))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isPin) "Kebijakan PIN Transaksi" else "Ketentuan Kata Sandi",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = if (isPin) "6 Digit Angka" else "Min. 6 Karakter",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        HorizontalDivider(
                            color = Color(0xFFE2E8F0).copy(alpha = 0.6f),
                            thickness = 1.dp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isPin) "Akses: Otentikasi & Tutup Shift" else "Akses: Login Akun Kasir",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = if (isPin) "Khusus Angka (0-9)" else "Kombinasi Karakter",
                                fontSize = 11.sp,
                                color = Color(0xFF10533C),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Input fields
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CredentialField(
                        label = currentLabel,
                        placeholder = "Masukkan $currentLabel",
                        value = oldValue,
                        onValueChange = {
                            oldValue = credentialInput(it, isPin)
                            onInputChanged()
                        },
                        keyboardType = keyboard,
                        enabled = !isSaving,
                        testTag = "credential-current",
                        isError = isOldValueError,
                        supportingText = if (isOldValueError) errorMessage else null,
                        leadingIcon = if (isPin) Icons.Outlined.Pin else Icons.Outlined.Lock
                    )

                    CredentialField(
                        label = newLabel,
                        placeholder = if (isPin) "Masukkan 6 digit PIN baru" else "Minimal 6 karakter baru",
                        value = newValue,
                        onValueChange = {
                            newValue = credentialInput(it, isPin)
                            onInputChanged()
                        },
                        keyboardType = keyboard,
                        enabled = !isSaving,
                        testTag = "credential-new",
                        isError = isNewValueError,
                        supportingText = if (isNewValueError) errorMessage else null,
                        helperText = if (!isNewValueError) {
                            if (isPin) "PIN harus terdiri dari 6 digit angka" else "Gunakan minimal 6 karakter kombinasi"
                        } else null,
                        leadingIcon = if (isPin) Icons.Outlined.Pin else Icons.Outlined.Lock
                    )

                    CredentialField(
                        label = confirmationLabel,
                        placeholder = "Ketik ulang $newLabel",
                        value = confirmation,
                        onValueChange = {
                            confirmation = credentialInput(it, isPin)
                            onInputChanged()
                        },
                        keyboardType = keyboard,
                        enabled = !isSaving,
                        testTag = "credential-confirmation",
                        isError = isConfirmationError,
                        supportingText = if (isConfirmationError) errorMessage else null,
                        leadingIcon = if (isPin) Icons.Outlined.Pin else Icons.Outlined.Lock
                    )
                }

                // General Error Card if any
                if (isGeneralError && errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEF2F2))
                            .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = errorMessage,
                                color = Color(0xFFB91C1C),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Action buttons: Primary Button + Kembali Text Button (identical to ShiftClosureBottomSheetModal)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSubmit(oldValue, newValue, confirmation) },
                        enabled = !isSaving && formReady,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10533C),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFE2E8F0),
                            disabledContentColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("credential-save")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isPin) "Simpan PIN Baru" else "Simpan Kata Sandi",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Surface(
                        onClick = onDismiss,
                        color = Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Kembali",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CredentialField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    enabled: Boolean,
    testTag: String,
    isError: Boolean = false,
    supportingText: String? = null,
    helperText: String? = null,
    leadingIcon: ImageVector = Icons.Outlined.Lock,
) {
    var visible by remember { mutableStateOf(false) }

    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val bgColor = if (isError) Color(0xFFFFF5F5) else Color.White
    val borderColor = when {
        isError -> Color(0xFFEF4444)
        isFocused -> Color(0xFF10533C)
        else -> Color(0xFFCBD5E1)
    }
    val borderWidth = if (isFocused || isError) 1.5.dp else 1.dp

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isError) Color(0xFFDC2626) else Color(0xFF334155),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .border(borderWidth, borderColor, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (isError) Color(0xFFDC2626) else if (isFocused) Color(0xFF10533C) else Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )

                androidx.compose.foundation.text.BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    modifier = Modifier.weight(1f).testTag(testTag),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0F172A)
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    interactionSource = interactionSource,
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(if (isError) Color(0xFFDC2626) else Color(0xFF10533C)),
                    decorationBox = { innerTextField ->
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                fontSize = 13.sp,
                                color = if (isError) Color(0xFFDC2626).copy(alpha = 0.7f) else Color(0xFF94A3B8)
                            )
                        }
                        innerTextField()
                    }
                )

                IconButton(
                    onClick = { visible = !visible },
                    enabled = enabled,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (visible) "Sembunyikan" else "Tampilkan",
                        tint = if (isError) Color(0xFFDC2626) else Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (supportingText != null) {
            Text(
                text = supportingText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFDC2626),
                modifier = Modifier.padding(start = 4.dp),
            )
        } else if (helperText != null) {
            Text(
                text = helperText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

private fun credentialInput(value: String, pin: Boolean): String =
    if (pin) value.filter(Char::isDigit).take(6) else value








