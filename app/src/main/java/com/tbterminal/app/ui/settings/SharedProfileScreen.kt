package com.tbterminal.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.UserProfile
import java.util.Locale

private val ProfilePrimary = Color(0xFF00694C)
private val ProfileText = Color(0xFF0F172A)
private val ProfileMuted = Color(0xFF64748B)

@Composable
fun SharedProfileScreen(
    uiState: ProfileUiState,
    onReload: () -> Unit,
    onChangePassword: (String, String, String) -> Unit,
    onChangePin: (String, String, String) -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dialog by remember { mutableStateOf<CredentialDialog?>(null) }
    LaunchedEffect(uiState.message) {
        if (uiState.message != null) dialog = null
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(Color(0xFFF7F9F8))) {
        val compact = maxWidth < 720.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (compact) 16.dp else 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(if (compact) 16.dp else 22.dp)
        ) {
            ProfileScreenHeader(compact = compact, isLoading = uiState.isLoading, onReload = onReload)

            uiState.error?.let { MessageCard(it, Color(0xFFB91C1C), onClearMessage) }
            uiState.message?.let { MessageCard(it, ProfilePrimary, onClearMessage) }

            when {
                uiState.isLoading && uiState.profile == null -> Box(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = ProfilePrimary) }
                uiState.profile != null -> ProfileContent(
                    profile = uiState.profile,
                    isSaving = uiState.isSaving,
                    compact = compact,
                    onPassword = { dialog = CredentialDialog.PASSWORD },
                    onPin = { dialog = CredentialDialog.PIN }
                )
                else -> Text("Profil tidak tersedia.", color = ProfileMuted)
            }
        }
    }

    dialog?.let { type ->
        CredentialChangeDialog(
            type = type,
            isSaving = uiState.isSaving,
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
    compact: Boolean,
    isLoading: Boolean,
    onReload: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Profil",
                color = ProfileText,
                fontSize = if (compact) 25.sp else 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                if (compact) "Kelola identitas dan keamanan akun."
                else "Identitas akun tersinkron dari server dan tidak dapat diedit dari terminal.",
                color = ProfileMuted,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
        OutlinedButton(
            onClick = onReload,
            enabled = !isLoading,
            modifier = Modifier.height(44.dp),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = if (compact) 12.dp else 16.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Muat ulang profil", modifier = Modifier.size(20.dp))
            if (!compact) {
                Spacer(Modifier.width(8.dp))
                Text("Muat ulang")
            }
        }
    }
}

@Composable
private fun ProfileContent(
    profile: UserProfile,
    isSaving: Boolean,
    compact: Boolean,
    onPassword: () -> Unit,
    onPin: () -> Unit
) {
    if (compact) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ProfileIdentityCard(profile)
            ProfileInformationCard(profile)
            ProfileSecurityCard(isSaving, onPassword, onPin)
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1.15f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                ProfileIdentityCard(profile)
                ProfileInformationCard(profile)
            }
            Box(modifier = Modifier.weight(0.85f)) {
                ProfileSecurityCard(isSaving, onPassword, onPin)
            }
        }
    }
}

@Composable
private fun ProfileIdentityCard(profile: UserProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(68.dp).background(ProfilePrimary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    profile.name.take(1).uppercase(Locale.ROOT),
                    color = ProfilePrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(profile.name, color = ProfileText, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text("@${profile.username}", color = ProfileMuted, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProfileBadge(profile.role.toProfileRoleLabel(), ProfilePrimary)
                    ProfileBadge(
                        if (profile.isActive) "Aktif" else "Tidak aktif",
                        if (profile.isActive) ProfilePrimary else Color(0xFFB91C1C)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileInformationCard(profile: UserProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Informasi akun", color = ProfileText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            ProfileDetailRow("Email", profile.email?.takeIf(String::isNotBlank) ?: "-")
            HorizontalDivider(color = Color(0xFFE8EEEB))
            ProfileDetailRow("Bergabung", formatDateTimeToDate(profile.joinedAt))
            HorizontalDivider(color = Color(0xFFE8EEEB))
            ProfileDetailRow("Login terakhir", profile.lastLoginAt?.let(::formatDateTimeToTime) ?: "-")
        }
    }
}

@Composable
private fun ProfileSecurityCard(
    isSaving: Boolean,
    onPassword: () -> Unit,
    onPin: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Keamanan akun", color = ProfileText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Perbarui akses untuk akun yang sedang digunakan.", color = ProfileMuted, fontSize = 13.sp)
            }
            Button(
                onClick = onPassword,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Ubah password", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onPin,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Pin, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Ubah PIN", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProfileDetailRow(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, color = ProfileMuted, fontSize = 12.sp)
        Text(
            value,
            color = ProfileText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ProfileBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private fun String.toProfileRoleLabel(): String = when (lowercase(Locale.ROOT)) {
    "owner" -> "Pemilik"
    "admin" -> "Admin"
    "cashier", "kasir" -> "Kasir"
    else -> replaceFirstChar { it.titlecase(Locale.ROOT) }
}

@Composable
private fun MessageCard(message: String, color: Color, onDismiss: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(message, color = color, modifier = Modifier.weight(1f))
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    }
}

private enum class CredentialDialog { PASSWORD, PIN }

@Composable
private fun CredentialChangeDialog(
    type: CredentialDialog,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var oldValue by remember(type) { mutableStateOf("") }
    var newValue by remember(type) { mutableStateOf("") }
    var confirmation by remember(type) { mutableStateOf("") }
    val isPin = type == CredentialDialog.PIN
    val label = if (isPin) "PIN" else "Password"
    val keyboard = if (isPin) KeyboardType.NumberPassword else KeyboardType.Password

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ubah $label") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CredentialField("$label lama", oldValue, { oldValue = credentialInput(it, isPin) }, keyboard, !isSaving)
                CredentialField("$label baru", newValue, { newValue = credentialInput(it, isPin) }, keyboard, !isSaving)
                CredentialField("Konfirmasi $label baru", confirmation, { confirmation = credentialInput(it, isPin) }, keyboard, !isSaving)
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(oldValue, newValue, confirmation) }, enabled = !isSaving) {
                Text(if (isSaving) "Menyimpan..." else "Simpan")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Batal") } },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun CredentialField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    enabled: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        enabled = enabled,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    )
}

private fun credentialInput(value: String, pin: Boolean): String =
    if (pin) value.filter(Char::isDigit).take(6) else value

fun formatDateTimeToDate(isoString: String): String =
    isoString.takeIf { it.length >= 10 }?.substring(0, 10) ?: isoString

fun formatDateTimeToTime(isoString: String): String =
    isoString.takeIf { it.length >= 16 }?.let { "${it.substring(0, 10)} ${it.substring(11, 16)}" } ?: isoString
