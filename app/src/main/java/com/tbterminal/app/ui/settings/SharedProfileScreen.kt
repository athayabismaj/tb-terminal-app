package com.tbterminal.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.border

import androidx.compose.ui.res.stringResource
import com.tbterminal.app.R
import com.tbterminal.app.data.model.UserProfile
import com.tbterminal.app.ui.components.SkeletonBox
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
    onLogout: (() -> Unit)? = null,
    onEditProfile: (() -> Unit)? = null,
) {
    var dialog by remember { mutableStateOf<CredentialDialog?>(null) }
    var confirmLogout by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.message) {
        if (uiState.message != null) dialog = null
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val compact = maxWidth < 720.dp
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxSize()
                .padding(
                    horizontal = if (compact) 16.dp else 32.dp,
                    vertical = if (compact) 12.dp else 28.dp,
                )
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 20.dp)
        ) {
            if (showHeader) ProfileScreenHeader()

            uiState.error?.let {
                MessageCard(it, isError = true, onClearMessage)
                Button(onClick = onReload, enabled = !uiState.isLoading) { Text("Coba lagi") }
            }
            uiState.message?.let { MessageCard(it, isError = false, onClearMessage) }

            when {
                uiState.isLoading && uiState.profile == null -> ProfileLoadingState(compact)
                uiState.profile != null -> ProfileContent(
                    profile = uiState.profile,
                    isSaving = uiState.isSaving,
                    compact = compact,
                    onPassword = { dialog = CredentialDialog.PASSWORD },
                    onPin = { dialog = CredentialDialog.PIN },
                    onLogout = onLogout?.let { { confirmLogout = true } },
                    onEditProfile = onEditProfile,
                )
                else -> Text(
                    "Profil tidak tersedia.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            // Ending a session remains reachable even if GET /auth/me fails.
            if (uiState.profile == null && onLogout != null) {
                AccountLogoutButton(!uiState.isSaving) { confirmLogout = true }
            }
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
) {
    Text(
        text = stringResource(R.string.profile_title),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun ProfileLoadingState(compact: Boolean) {
    val identity: @Composable () -> Unit = {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonBox(Modifier.size(if (compact) 58.dp else 72.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SkeletonBox(Modifier.fillMaxWidth(0.58f).height(18.dp))
                    SkeletonBox(Modifier.fillMaxWidth(0.38f).height(13.dp))
                    SkeletonBox(Modifier.fillMaxWidth(0.68f).height(12.dp))
                    SkeletonBox(Modifier.fillMaxWidth(0.46f).height(24.dp))
                }
            }
        }
    }
    val details: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonBox(Modifier.fillMaxWidth(0.24f).height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    repeat(2) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            SkeletonBox(Modifier.size(38.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                SkeletonBox(Modifier.fillMaxWidth(0.32f).height(11.dp))
                                SkeletonBox(Modifier.fillMaxWidth(0.62f).height(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (compact) {
        Column(
            modifier = Modifier.fillMaxWidth().testTag("profile-skeleton"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            identity()
            details()
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().testTag("profile-skeleton"),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(0.42f)) { identity() }
            Column(Modifier.weight(0.58f)) { details() }
        }
    }
}

@Composable
private fun ProfileContent(
    profile: UserProfile,
    isSaving: Boolean,
    compact: Boolean,
    onPassword: () -> Unit,
    onPin: () -> Unit,
    onLogout: (() -> Unit)? = null,
    onEditProfile: (() -> Unit)? = null,
) {
    if (compact) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ProfileIdentityCard(profile, compact = true, onEditProfile = onEditProfile)
            ProfileSecurityCard(isSaving, onPassword, onPin)
            if (onLogout != null) AccountLogoutButton(!isSaving, onLogout)
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(0.42f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ProfileIdentityCard(profile, compact = false, onEditProfile = onEditProfile)
                if (onLogout != null) AccountLogoutButton(!isSaving, onLogout)
            }
            Column(
                modifier = Modifier.weight(0.58f),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                ProfileSecurityCard(isSaving, onPassword, onPin)
            }
        }
    }
}

@Composable
internal fun AccountLogoutButton(enabled: Boolean, onClick: () -> Unit) {
    androidx.compose.material3.Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("account-logout"),
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFEF2F2), // red-50
            contentColor = Color(0xFFDC2626), // red-600
            disabledContainerColor = Color(0xFFFEF2F2).copy(alpha = 0.5f),
            disabledContentColor = Color(0xFFDC2626).copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, Color(0xFFFEE2E2).copy(alpha = 0.8f)) // red-100/80
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color(0xFFDC2626)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Keluar",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
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
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("profile-identity"),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, com.tbterminal.app.ui.theme.TbOutline.copy(alpha = 0.7f)),
        shadowElevation = 0.dp
    ) {
        if (compact) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    ProfileAvatar(profile, size = 56.dp)
                    Column(
                        modifier = Modifier.weight(1f).padding(top = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = profile.name,
                            color = com.tbterminal.app.ui.theme.TbText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                        )
                        Text(
                            text = "@${profile.username}",
                            color = com.tbterminal.app.ui.theme.TbTextMuted,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        ProfileBadge(profile.role.toProfileRoleLabel(), isError = false)
                        if (!profile.isActive) ProfileBadge("Tidak aktif", isError = true)
                    }
                }
                
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = com.tbterminal.app.ui.theme.TbOutline.copy(alpha = 0.6f)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFF9FAFB), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MailOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = com.tbterminal.app.ui.theme.TbTextMuted,
                        )
                    }
                    Text(
                        text = profile.email?.takeIf(String::isNotBlank) ?: "Email belum tersedia",
                        modifier = Modifier.weight(1f),
                        color = com.tbterminal.app.ui.theme.TbText,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    if (onEditProfile != null) {
                        IconButton(
                            onClick = onEditProfile,
                            modifier = Modifier.size(36.dp).testTag("profile-edit"),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit profil",
                                modifier = Modifier.size(16.dp),
                                tint = com.tbterminal.app.ui.theme.TbTextMuted,
                            )
                        }
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (onEditProfile != null) {
                    IconButton(
                        onClick = onEditProfile,
                        modifier = Modifier.align(Alignment.TopEnd).size(48.dp).testTag("profile-edit"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit profil",
                            modifier = Modifier.size(20.dp),
                            tint = com.tbterminal.app.ui.theme.TbGreenDark,
                        )
                    }
                }
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    ProfileAvatar(profile, size = 72.dp)
                    ProfileIdentityDetails(
                        profile = profile,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(profile: UserProfile, size: androidx.compose.ui.unit.Dp) {
    Box(modifier = Modifier.size(size)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(com.tbterminal.app.ui.theme.TbGreenLight, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                profile.name.take(1).uppercase(Locale.ROOT),
                color = com.tbterminal.app.ui.theme.TbGreenDark,
                style = if (size < 64.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Surface(
            modifier = Modifier.align(Alignment.BottomEnd).size(14.dp),
            shape = CircleShape,
            color = if (profile.isActive) com.tbterminal.app.ui.theme.TbGreenDark else MaterialTheme.colorScheme.error,
            border = BorderStroke(2.dp, Color.White),
            shadowElevation = 0.dp
        ) {}
    }
}

@Composable
private fun ProfileIdentityDetails(
    profile: UserProfile,
    horizontalAlignment: Alignment.Horizontal,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            profile.name,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
        )
        Text(
            "@${profile.username}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.MailOutline,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = profile.email?.takeIf(String::isNotBlank) ?: "Email belum tersedia",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileBadge(profile.role.toProfileRoleLabel(), isError = false)
            if (!profile.isActive) ProfileBadge("Tidak aktif", isError = true)
        }
    }
}

@Composable
private fun ProfileSecurityCard(
    isSaving: Boolean,
    onPassword: () -> Unit,
    onPin: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().testTag("profile-security")) {
        Text(
            text = "KEAMANAN AKUN",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6B7280), // gray-500
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFF3F4F6)), // gray-100
            shadowElevation = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                ProfileActionRow(
                    label = "Ubah kata sandi",
                    icon = Icons.Outlined.Lock,
                    enabled = !isSaving,
                    onClick = onPassword,
                    modifier = Modifier.testTag("profile-action-password"),
                )
                androidx.compose.material3.HorizontalDivider(color = Color(0xFFF3F4F6)) // gray-100
                ProfileActionRow(
                    label = "Ubah PIN",
                    icon = Icons.Outlined.Password,
                    enabled = !isSaving,
                    onClick = onPin,
                    modifier = Modifier.testTag("profile-action-pin"),
                )
            }
        }
    }
}

@Composable
private fun ProfileActionRow(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFFEBF3EF), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFF256B57),
            )
        }
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1F2937), // gray-800
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color(0xFF9CA3AF), // gray-400
        )
    }
}

@Composable
private fun ProfileBadge(label: String, isError: Boolean) {
    val contentColor = if (isError) MaterialTheme.colorScheme.error else com.tbterminal.app.ui.theme.TbGreenDark
    val containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else com.tbterminal.app.ui.theme.TbGreenLight
    Box(
        modifier = Modifier
            .background(containerColor, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(label, color = contentColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ProfileDivider() {
    androidx.compose.material3.HorizontalDivider(
        color = com.tbterminal.app.ui.theme.TbOutline.copy(alpha = 0.6f),
    )
}

private fun String.toProfileRoleLabel(): String = when (lowercase(Locale.ROOT)) {
    "owner" -> "Pemilik"
    "admin" -> "Admin"
    "cashier", "kasir" -> "Kasir"
    else -> replaceFirstChar { it.titlecase(Locale.ROOT) }
}

@Composable
private fun MessageCard(message: String, isError: Boolean, onDismiss: () -> Unit) {
    val contentColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    Surface(color = containerColor, shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(message, color = contentColor, modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text("Tutup") }
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
    var oldValue by remember(type) { mutableStateOf("") }
    var newValue by remember(type) { mutableStateOf("") }
    var confirmation by remember(type) { mutableStateOf("") }
    val isPin = type == CredentialDialog.PIN
    val title = if (isPin) "Ubah PIN" else "Ubah kata sandi"
    val keyboard = if (isPin) KeyboardType.NumberPassword else KeyboardType.Password
    val currentLabel = if (isPin) "PIN saat ini" else "Kata sandi saat ini"
    val newLabel = if (isPin) "PIN baru" else "Kata sandi baru"
    val confirmationLabel = if (isPin) "Ulangi PIN" else "Ulangi kata sandi"
    val formReady = oldValue.isNotBlank() && newValue.isNotBlank() && confirmation.isNotBlank()

    

    

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .testTag("credential-dialog"),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(40.dp).background(Color(0xFFEBF3EF), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPin) Icons.Outlined.Password else Icons.Outlined.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF256B57)
                    )
                }
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                CredentialField(
                    placeholder = currentLabel,
                    value = oldValue,
                    onValueChange = {
                        oldValue = credentialInput(it, isPin)
                        onInputChanged()
                    },
                    keyboardType = keyboard,
                    enabled = !isSaving,
                    testTag = "credential-current",
                )
                CredentialField(
                    placeholder = newLabel,
                    value = newValue,
                    onValueChange = {
                        newValue = credentialInput(it, isPin)
                        onInputChanged()
                    },
                    keyboardType = keyboard,
                    enabled = !isSaving,
                    testTag = "credential-new",
                )
                CredentialField(
                    placeholder = confirmationLabel,
                    value = confirmation,
                    onValueChange = {
                        confirmation = credentialInput(it, isPin)
                        onInputChanged()
                    },
                    keyboardType = keyboard,
                    enabled = !isSaving,
                    testTag = "credential-confirmation",
                )
            }

            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onDismiss, 
                    enabled = !isSaving,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                ) {
                    Text("Batal", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF256B57))
                }
                Button(
                    onClick = { onSubmit(oldValue, newValue, confirmation) },
                    enabled = !isSaving && formReady,
                    modifier = Modifier.heightIn(min = 44.dp).testTag("credential-save"),
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = if (!isSaving && formReady) Color(0xFF256B57) else Color(0xFFE5E7EB),
                        contentColor = if (!isSaving && formReady) Color.White else Color(0xFF6B7280)
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF256B57),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    val btnColor = if (!isSaving && formReady) Color.White else Color(0xFF6B7280)
                    Text(if (isSaving) "Menyimpan" else "Simpan", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = btnColor)
                }
            }
        }
    }
}

@Composable
private fun CredentialField(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    enabled: Boolean,
    testTag: String,
) {
    var visible by remember { mutableStateOf(false) }
    
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    
    val bgColor = if (isFocused) Color.White else Color(0xFFFAFAFA)
    val borderColor = if (isFocused) Color(0xFF256B57) else Color(0xFFD1D5DB) // gray-300
    val borderWidth = if (isFocused) 1.5.dp else 1.dp
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .background(bgColor, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                modifier = Modifier.weight(1f).testTag(testTag),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 14.sp,
                    color = Color(0xFF1F2937) // gray-800
                ),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                interactionSource = interactionSource,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF256B57)),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 14.sp,
                            color = Color(0xFF6B7280) // gray-500
                        )
                    }
                    innerTextField()
                }
            )
            
            IconButton(
                onClick = { visible = !visible }, 
                enabled = enabled,
                modifier = Modifier.size(24.dp).padding(end = 4.dp)
            ) {
                Icon(
                    imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) "Sembunyikan" else "Tampilkan",
                    tint = Color(0xFF9CA3AF), // gray-400
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun credentialInput(value: String, pin: Boolean): String =
    if (pin) value.filter(Char::isDigit).take(6) else value








