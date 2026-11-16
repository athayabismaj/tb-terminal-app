package com.tbterminal.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.components.TopFloatingCapsule

@Composable
fun SharedEditProfileScreen(
    uiState: ProfileUiState,
    onSave: (String, String?) -> Unit,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
) {
    val profile = uiState.profile
    var name by rememberSaveable(profile?.id) { mutableStateOf(profile?.name.orEmpty()) }
    var email by rememberSaveable(profile?.id) { mutableStateOf(profile?.email.orEmpty()) }
    val validationError: String? = remember(name, email) { validateProfileForm(name, email) }

    val initials = remember(profile?.name, name) {
        val src = name.ifBlank { profile?.name.orEmpty() }.trim()
        val parts = src.split("\\s+".toRegex()).filter { it.isNotBlank() }
        when {
            parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
            parts.isNotEmpty() -> parts[0].take(2).uppercase()
            else -> "K"
        }
    }

    val cashierId = remember(profile?.id) {
        val digits = profile?.id?.filter { it.isDigit() }.orEmpty()
        val num = digits.takeLast(3).padStart(3, '0').ifEmpty { "001" }
        "KSR-$num"
    }

    LaunchedEffect(uiState.profileUpdated) {
        if (uiState.profileUpdated) onSaved()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        if (showHeader) {
            EditProfileHeader(onBack = onBack)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 440.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (uiState.isLoading && profile == null) {
                    EditProfileSkeleton()
                } else if (profile != null) {
                    // Hero Avatar Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .background(Color(0xFFDCFCE7), CircleShape)
                                        .border(2.dp, Color(0xFF86EFAC), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF166534)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color(0xFF10533C), CircleShape)
                                        .border(2.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = name.ifBlank { profile.name },
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = Color(0xFFF0FDF4),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                                    ) {
                                        Text(
                                            text = "Kasir",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF166534),
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "•",
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Text(
                                        text = "ID: $cashierId",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }

                    // Form Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Color(0x08000000))
                            .testTag("edit-profile-form"),
                        color = Color.White,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 4.dp, height = 16.dp)
                                            .background(Color(0xFF10533C), RoundedCornerShape(2.dp))
                                    )
                                    Text(
                                        text = "INFORMASI IDENTITAS",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Text(
                                    text = "Nama dan email digunakan sebagai tanda pengenal pada struk penjualan.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    lineHeight = 17.sp
                                )
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                            val isNameError = (name.isNotBlank() && name.trim().length < 2) || uiState.error?.contains("nama", ignoreCase = true) == true
                            val nameSupporting = if (name.isNotBlank() && name.trim().length < 2) {
                                "Nama minimal 2 karakter"
                            } else if (uiState.error?.contains("nama", ignoreCase = true) == true) {
                                uiState.error
                            } else null

                            val isEmailError = (email.isNotBlank() && !email.trim().matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) || uiState.error?.contains("email", ignoreCase = true) == true
                            val emailSupporting = if (email.isNotBlank() && !email.trim().matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
                                "Format email tidak valid"
                            } else if (uiState.error?.contains("email", ignoreCase = true) == true) {
                                uiState.error
                            } else null

                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                ProfileInputField(
                                    label = "Nama Lengkap",
                                    value = name,
                                    onValueChange = { name = it.take(100); onClearMessage() },
                                    enabled = !uiState.isSaving,
                                    placeholder = "Masukkan nama lengkap kasir",
                                    leadingIcon = Icons.Outlined.Person,
                                    testTag = "edit-profile-name",
                                    isError = isNameError,
                                    supportingText = nameSupporting,
                                    helperText = if (!isNameError) "Nama yang akan tercetak pada struk kasir" else null,
                                )

                                ProfileInputField(
                                    label = "Alamat Email (Opsional)",
                                    value = email,
                                    onValueChange = { email = it.take(150); onClearMessage() },
                                    enabled = !uiState.isSaving,
                                    placeholder = "budi@gmail.com",
                                    leadingIcon = Icons.Outlined.MailOutline,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    testTag = "edit-profile-email",
                                    isError = isEmailError,
                                    supportingText = emailSupporting,
                                    helperText = if (!isEmailError) "Digunakan untuk pengiriman rekap shift dan notifikasi" else null,
                                )

                                ProfileReadonlyField(
                                    label = "Username Sistem",
                                    value = profile.username,
                                    prefix = "@",
                                    leadingIcon = Icons.Outlined.AlternateEmail
                                )
                            }
                        }
                    }

                    // Action Buttons Row
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onSave(name.trim(), email.trim().ifBlank { null }) },
                            enabled = !uiState.isSaving && validationError == null &&
                                (name.trim() != profile.name || email.trim().ifBlank { null } != profile.email),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .shadow(2.dp, RoundedCornerShape(14.dp), spotColor = Color(0x10000000))
                                .testTag("edit-profile-save"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10533C),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFFE2E8F0),
                                disabledContentColor = Color(0xFF94A3B8)
                            )
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Menyimpan...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Simpan Perubahan", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = onBack,
                            enabled = !uiState.isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF475569)
                            )
                        ) {
                            Text("Batal & Kembali", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Home indicator
                    Box(
                        modifier = Modifier
                            .padding(top = 16.dp, bottom = 8.dp)
                            .size(width = 134.dp, height = 5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFCBD5E1))
                    )
                } else {
                    Text("Profil tidak tersedia.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
}

@Composable
private fun ProfileInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    testTag: String,
    placeholder: String = "",
    leadingIcon: ImageVector = Icons.Outlined.Person,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false,
    supportingText: String? = null,
    helperText: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor = when {
        isError -> Color(0xFFEF4444)
        isFocused -> Color(0xFF10533C)
        else -> Color(0xFFE2E8F0)
    }
    val bgColor = when {
        isError -> Color(0xFFFFF5F5)
        isFocused -> Color.White
        else -> Color(0xFFF8FAFC)
    }
    val borderWidth = if (isFocused || isError) 1.5.dp else 1.dp

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isError) Color(0xFFDC2626) else Color(0xFF64748B),
            letterSpacing = 0.5.sp
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
                .background(bgColor, RoundedCornerShape(12.dp)),
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

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(testTag),
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0F172A)
                    ),
                    keyboardOptions = keyboardOptions,
                    singleLine = true,
                    interactionSource = interactionSource,
                    cursorBrush = SolidColor(if (isError) Color(0xFFDC2626) else Color(0xFF10533C)),
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

@Composable
private fun ProfileReadonlyField(
    label: String,
    value: String,
    prefix: String,
    leadingIcon: ImageVector = Icons.Outlined.AlternateEmail
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            letterSpacing = 0.5.sp
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp)),
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
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )

                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = prefix,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(end = 2.dp)
                    )
                    Text(
                        text = value,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155)
                    )
                }

                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Terkunci",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }

        Text(
            text = "Username ditentukan oleh admin toko dan tidak dapat diubah",
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun EditProfileHeader(onBack: () -> Unit) {
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(34.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Kembali",
                        tint = Color(0xFF334155),
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = "Edit Profil Kasir",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    letterSpacing = (-0.3).sp,
                )
            }
            HorizontalDivider(color = Color(0xFFE2E8F0).copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun EditProfileSkeleton() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("edit-profile-skeleton"),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SkeletonBox(Modifier.fillMaxWidth(0.35f).heightIn(min = 20.dp))
            SkeletonBox(Modifier.fillMaxWidth().heightIn(min = 52.dp))
            SkeletonBox(Modifier.fillMaxWidth().heightIn(min = 52.dp))
            SkeletonBox(Modifier.fillMaxWidth(0.7f).heightIn(min = 18.dp))
        }
    }
}
