package com.tbterminal.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Profil Akun", color = ProfileText, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text("Identitas akun berasal dari server dan tidak dapat diedit dari terminal.", color = ProfileMuted)
            }
            OutlinedButton(onClick = onReload, enabled = !uiState.isLoading) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Muat Ulang")
            }
        }

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
                onPassword = { dialog = CredentialDialog.PASSWORD },
                onPin = { dialog = CredentialDialog.PIN }
            )
            else -> Text("Profil tidak tersedia.", color = ProfileMuted)
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
private fun ProfileContent(
    profile: UserProfile,
    isSaving: Boolean,
    onPassword: () -> Unit,
    onPin: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier.size(88.dp).background(ProfilePrimary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    profile.name.take(1).uppercase(Locale.ROOT),
                    color = ProfilePrimary,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ProfileValue("Nama", profile.name)
                ProfileValue("Username", profile.username)
                profile.email?.let { ProfileValue("Email", it) }
                ProfileValue("Role", profile.role)
                ProfileValue("Status", if (profile.isActive) "Aktif" else "Tidak aktif")
                ProfileValue("Bergabung", formatDateTimeToDate(profile.joinedAt))
                ProfileValue("Login terakhir", profile.lastLoginAt?.let(::formatDateTimeToTime) ?: "-")
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Keamanan Akun", color = ProfileText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text("Perubahan hanya berlaku untuk akun yang sedang login dan dicatat oleh backend.", color = ProfileMuted)
            HorizontalDivider()
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onPassword, enabled = !isSaving) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ubah Password")
                }
                OutlinedButton(onClick = onPin, enabled = !isSaving) {
                    Icon(Icons.Default.Pin, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ubah PIN")
                }
            }
        }
    }
}

@Composable
private fun ProfileValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label.uppercase(Locale.ROOT), color = ProfileMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = ProfileText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
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
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Batal") } }
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
        singleLine = true
    )
}

private fun credentialInput(value: String, pin: Boolean): String =
    if (pin) value.filter(Char::isDigit).take(6) else value

fun formatDateTimeToDate(isoString: String): String =
    isoString.takeIf { it.length >= 10 }?.substring(0, 10) ?: isoString

fun formatDateTimeToTime(isoString: String): String =
    isoString.takeIf { it.length >= 16 }?.let { "${it.substring(0, 10)} ${it.substring(11, 16)}" } ?: isoString
