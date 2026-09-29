package com.tbterminal.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.components.SkeletonBox

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
    val validationError = remember(name, email) { validateProfileForm(name, email) }

    LaunchedEffect(uiState.profileUpdated) {
        if (uiState.profileUpdated) onSaved()
    }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (showHeader) EditProfileHeader(onBack)
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = maxWidth < 720.dp
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (compact) 16.dp else 28.dp, vertical = if (compact) 20.dp else 28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (uiState.isLoading && profile == null) {
                    EditProfileSkeleton()
                } else if (profile != null) {
                    // Profile Card matching HTML
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color(0x08000000))
                            .testTag("edit-profile-form"),
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.8f)),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                        ) {
                            Text(
                                "Informasi profil",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Nama dan email akan tampil sebagai identitas akun di aplikasi.",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                lineHeight = 18.sp
                            )
                            Spacer(Modifier.height(20.dp))
                            
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                ProfileInputField(
                                    label = "Nama lengkap",
                                    value = name,
                                    onValueChange = { name = it.take(100); onClearMessage() },
                                    enabled = !uiState.isSaving,
                                    testTag = "edit-profile-name"
                                )
                                ProfileInputField(
                                    label = "Email (opsional)",
                                    value = email,
                                    onValueChange = { email = it.take(150); onClearMessage() },
                                    enabled = !uiState.isSaving,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    testTag = "edit-profile-email"
                                )
                                ProfileReadonlyField(
                                    label = "Username",
                                    value = profile.username,
                                    prefix = "@"
                                )
                                // Role removed as requested by HTML "Tanpa Baris Role"
                            }
                        }
                    }

                    uiState.error?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.testTag("edit-profile-error"),
                        )
                    }
                    
                    // Action Button
                    Button(
                        onClick = { onSave(name, email.takeIf(String::isNotBlank)) },
                        enabled = !uiState.isSaving && validationError == null &&
                            (name.trim() != profile.name || email.trim().ifBlank { null } != profile.email),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .shadow(1.dp, RoundedCornerShape(12.dp), spotColor = Color(0x0D000000))
                            .testTag("edit-profile-save"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF256B57),
                            disabledContainerColor = Color(0xFF256B57).copy(alpha = 0.5f)
                        )
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Simpan perubahan", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    Text("Profil tidak tersedia.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
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
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor = if (isFocused) Color(0xFF256B57) else Color(0xFFCBD5E1)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(if (isFocused) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(Color.White, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(
                text = label.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B),
                letterSpacing = 0.5.sp
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp).testTag(testTag),
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0F172A)
                ),
                keyboardOptions = keyboardOptions,
                singleLine = true,
                interactionSource = interactionSource,
                cursorBrush = SolidColor(Color(0xFF256B57))
            )
        }
    }
}

@Composable
private fun ProfileReadonlyField(
    label: String,
    value: String,
    prefix: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
            .background(Color.White, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(
                text = label.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B),
                letterSpacing = 0.5.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = prefix,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0F172A)
                )
            }
        }
    }
}

@Composable
private fun EditProfileHeader(onBack: () -> Unit) {
    com.tbterminal.app.ui.components.TBTopAppBar(
        title = "Edit profil",
        onBackClick = onBack,
    )
}

@Composable
private fun EditProfileSkeleton() {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("edit-profile-skeleton"),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SkeletonBox(Modifier.fillMaxWidth(0.35f).heightIn(min = 20.dp))
            SkeletonBox(Modifier.fillMaxWidth().heightIn(min = 56.dp))
            SkeletonBox(Modifier.fillMaxWidth().heightIn(min = 56.dp))
            SkeletonBox(Modifier.fillMaxWidth(0.7f).heightIn(min = 18.dp))
        }
    }
}
