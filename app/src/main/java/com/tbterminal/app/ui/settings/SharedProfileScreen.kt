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








