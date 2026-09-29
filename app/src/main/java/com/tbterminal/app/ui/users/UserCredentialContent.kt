package com.tbterminal.app.ui.users

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

private val CredentialSurface = TbBackground
private val CredentialSurfaceLow = TbSurfaceMuted
private val CredentialOnSurface = TbText
private val CredentialPrimary = TbGreen
private val CredentialPrimaryDark = TbGreenDark
private val CredentialEmerald50 = TbGreenLight
private val CredentialSlate200 = TbOutline
private val CredentialSlate400 = Color(0xFF94A3B8)
private val CredentialSlate500 = TbTextMuted
private val CredentialSlate600 = TbTextMuted
private val CredentialError = TbError
private val CredentialErrorContainer = Color(0xFFFEE2E2)

@Composable
fun UserCredentialContent(
    mode: UserCredentialMode,
    uiState: UserCredentialUiState,
    onCredentialChange: (String) -> Unit,
    onConfirmationChange: (String) -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val compact = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 700
    var showPolicy by remember(mode) { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CredentialSurface)
            .padding(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 16.dp else 28.dp)
            .verticalScroll(rememberScrollState())
    ) {
        when {
            uiState.isLoadingUser -> CredentialLoadingCard(message = "Memuat data user...")
            uiState.errorMessage != null && uiState.targetUser == null -> {
                CredentialErrorCard(message = uiState.errorMessage, onRetry = onRetry)
            }
            else -> Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                CredentialFormCard(
                    mode = mode,
                    uiState = uiState,
                    onCredentialChange = onCredentialChange,
                    onConfirmationChange = onConfirmationChange,
                    onCancel = onCancel,
                    onSubmit = onSubmit,
                    onShowPolicy = { showPolicy = true },
                    modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
                    compact = compact,
                )
            }
        }
    }

    if (showPolicy) {
        CredentialPolicySheet(
            mode = mode,
            uiState = uiState,
            onDismiss = { showPolicy = false },
        )
    }
}

@Composable
private fun CredentialFormCard(
    mode: UserCredentialMode,
    uiState: UserCredentialUiState,
    onCredentialChange: (String) -> Unit,
    onConfirmationChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
    onShowPolicy: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean
) {
    var showCredential by remember(mode) { mutableStateOf(false) }
    val fieldIcon = if (mode == UserCredentialMode.Password) Icons.Default.Password else Icons.Default.CreditCard

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, CredentialSlate200)
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 16.dp else 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (uiState.errorMessage != null) {
                CredentialErrorBanner(message = uiState.errorMessage)
            }

            CredentialTargetSummary(uiState = uiState)

            HorizontalDivider(color = CredentialSlate200)

            CredentialInputField(
                label = mode.inputLabel(),
                value = uiState.credential,
                onValueChange = onCredentialChange,
                icon = fieldIcon,
                placeholder = mode.inputPlaceholder(),
                showValue = showCredential,
                onShowValueChange = { showCredential = it },
                keyboardType = mode.keyboardType(),
                enabled = !uiState.isSubmitting
            )

            CredentialInputField(
                label = mode.confirmationLabel(),
                value = uiState.confirmation,
                onValueChange = onConfirmationChange,
                icon = fieldIcon,
                placeholder = mode.confirmationPlaceholder(),
                showValue = showCredential,
                onShowValueChange = { showCredential = it },
                keyboardType = mode.keyboardType(),
                enabled = !uiState.isSubmitting
            )

            TextButton(
                onClick = onShowPolicy,
                enabled = !uiState.isSubmitting,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = CredentialPrimaryDark,
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = mode.policyButtonLabel(),
                    color = CredentialPrimaryDark,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            HorizontalDivider(color = CredentialSlate200)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onCancel,
                    enabled = !uiState.isSubmitting
                ) {
                    Text(text = "Batal", color = CredentialSlate600, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = onSubmit,
                    enabled = !uiState.isSubmitting && uiState.targetUser != null,
                    colors = ButtonDefaults.buttonColors(containerColor = CredentialPrimary),
                    shape = RoundedCornerShape(14.dp),
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
                        text = if (uiState.isSubmitting) "Menyimpan..." else mode.submitLabel(),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CredentialTargetSummary(
    uiState: UserCredentialUiState
) {
    val user = uiState.targetUser

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CredentialSurfaceLow)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(CredentialEmerald50),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Badge,
                contentDescription = null,
                tint = CredentialPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user?.name ?: "User belum dimuat",
                color = CredentialOnSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.AlternateEmail,
                    contentDescription = null,
                    tint = CredentialSlate400,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = user?.username ?: "-",
                    color = CredentialSlate500,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        Text(
            text = user?.roleName?.uppercase().orEmpty(),
            color = CredentialPrimaryDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun CredentialInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    placeholder: String,
    showValue: Boolean,
    onShowValueChange: (Boolean) -> Unit,
    keyboardType: KeyboardType,
    enabled: Boolean
) {
    Column {
        Text(
            text = label,
            color = CredentialSlate600,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            placeholder = { Text(text = placeholder, color = CredentialSlate400) },
            leadingIcon = {
                Icon(imageVector = icon, contentDescription = null, tint = CredentialSlate400)
            },
            trailingIcon = {
                IconButton(onClick = { onShowValueChange(!showValue) }) {
                    Icon(
                        imageVector = if (showValue) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showValue) "Sembunyikan" else "Tampilkan",
                        tint = CredentialSlate500
                    )
                }
            },
            visualTransformation = if (showValue) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = CredentialSurfaceLow,
                focusedContainerColor = CredentialSurfaceLow,
                disabledContainerColor = CredentialSurfaceLow,
                unfocusedBorderColor = CredentialSlate200,
                focusedBorderColor = CredentialPrimary
            ),
            singleLine = true
        )
    }
}

@Composable
private fun CredentialPolicySheet(
    mode: UserCredentialMode,
    uiState: UserCredentialUiState,
    onDismiss: () -> Unit,
) {
    TbMobileControlSheet(
        title = mode.policyTitle(),
        subtitle = "Periksa ketentuan sebelum menyimpan perubahan",
        onDismiss = onDismiss,
        testTag = "credential-policy-sheet",
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CredentialEmerald50)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Security,
                contentDescription = null,
                tint = CredentialPrimaryDark,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "Perubahan berlaku saat login atau membuka terminal berikutnya.",
                modifier = Modifier.weight(1f),
                color = CredentialPrimaryDark,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        CredentialPolicyItem(
            title = mode.policyPrimaryTitle(),
            description = mode.policyPrimaryDescription(),
        )
        CredentialPolicyItem(
            title = "Konfirmasi harus sama",
            description = "Isi kedua kolom dengan nilai yang sama untuk mencegah kesalahan input.",
        )
        CredentialPolicyItem(
            title = "Akun yang diubah",
            description = uiState.targetUser?.let { user ->
                "${user.name} · ${user.username}"
            } ?: "Pengguna belum dimuat.",
        )
        TbMobileSheetDoneButton(
            onClick = onDismiss,
            label = "Mengerti",
            testTag = "credential-policy-done",
        )
    }
}

@Composable
private fun CredentialPolicyItem(
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CredentialSurfaceLow)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = CredentialPrimary,
            modifier = Modifier.size(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = CredentialOnSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                color = CredentialSlate500,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun CredentialLoadingCard(
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, CredentialSlate200)
    ) {
        Row(
            modifier = Modifier.padding(28.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(color = CredentialPrimary, modifier = Modifier.size(26.dp))
            Text(text = message, color = CredentialSlate500, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun CredentialErrorCard(
    message: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, CredentialSlate200)
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CredentialErrorBanner(message = message)
            OutlinedButton(onClick = onRetry) {
                Text(text = "Muat ulang")
            }
        }
    }
}

@Composable
private fun CredentialErrorBanner(
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CredentialErrorContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = message,
            color = CredentialError,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

internal fun UserCredentialMode.pageTitle(): String {
    return when (this) {
        UserCredentialMode.Password -> "Ubah Password"
        UserCredentialMode.Pin -> "Ubah PIN"
    }
}

private fun UserCredentialMode.inputLabel(): String {
    return when (this) {
        UserCredentialMode.Password -> "PASSWORD BARU"
        UserCredentialMode.Pin -> "PIN BARU"
    }
}

private fun UserCredentialMode.confirmationLabel(): String {
    return when (this) {
        UserCredentialMode.Password -> "KONFIRMASI PASSWORD"
        UserCredentialMode.Pin -> "KONFIRMASI PIN"
    }
}

private fun UserCredentialMode.inputPlaceholder(): String {
    return when (this) {
        UserCredentialMode.Password -> "Masukkan password baru"
        UserCredentialMode.Pin -> "Masukkan 6 digit PIN"
    }
}

private fun UserCredentialMode.confirmationPlaceholder(): String {
    return when (this) {
        UserCredentialMode.Password -> "Ulangi password baru"
        UserCredentialMode.Pin -> "Ulangi 6 digit PIN"
    }
}

private fun UserCredentialMode.submitLabel(): String {
    return when (this) {
        UserCredentialMode.Password -> "Simpan Password"
        UserCredentialMode.Pin -> "Simpan PIN"
    }
}

private fun UserCredentialMode.policyButtonLabel(): String {
    return when (this) {
        UserCredentialMode.Password -> "Lihat ketentuan password"
        UserCredentialMode.Pin -> "Lihat ketentuan PIN"
    }
}

private fun UserCredentialMode.keyboardType(): KeyboardType {
    return when (this) {
        UserCredentialMode.Password -> KeyboardType.Password
        UserCredentialMode.Pin -> KeyboardType.NumberPassword
    }
}

private fun UserCredentialMode.policyTitle(): String {
    return when (this) {
        UserCredentialMode.Password -> "Kebijakan Password"
        UserCredentialMode.Pin -> "Kebijakan PIN"
    }
}

private fun UserCredentialMode.policyPrimaryTitle(): String {
    return when (this) {
        UserCredentialMode.Password -> "Minimal 6 karakter"
        UserCredentialMode.Pin -> "Tepat 6 digit angka"
    }
}

private fun UserCredentialMode.policyPrimaryDescription(): String {
    return when (this) {
        UserCredentialMode.Password -> "Hindari password terlalu mudah seperti nama toko atau urutan angka sederhana."
        UserCredentialMode.Pin -> "PIN harus mudah diingat karyawan tetapi tidak boleh memakai pola yang mudah ditebak."
    }
}
