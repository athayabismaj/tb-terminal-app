package com.tbterminal.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

// ==========================================
// TEMA & WARNA
// ==========================================
val SurfaceBg = Color.Transparent
val Slate50 = Color(0xFFF8FAFC)
val Slate100 = Color(0xFFF1F5F9)
val Slate200 = Color(0xFFE2E8F0)
val Slate300 = Color(0xFFCBD5E1)
val Slate400 = Color(0xFF94A3B8)
val Slate500 = Color(0xFF64748B)
val Slate900 = Color(0xFF0F172A)
val Primary = Color(0xFF00694C) // Sesuai desain TB Terminal
val PrimaryLight = Primary.copy(alpha = 0.05f)

// ==========================================
// KONTROLER UTAMA
// ==========================================
@Composable
fun SharedProfileScreen(
    userName: String,
    role: String,
    isActive: Boolean = true,
    joinedAt: String = "Mar 2024",
    lastLoginAt: String? = null,
    modifier: Modifier = Modifier
) {
    // State (Nantinya diikat ke ViewModel Ktor Anda)
    var fullName by remember { mutableStateOf(userName) }
    var username by remember { mutableStateOf(userName.lowercase(Locale.ROOT).replace(" ", "")) }
    var email by remember { mutableStateOf(userName.lowercase(Locale.ROOT).replace(" ", "") + "@tbterminal.com") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceBg)
            // Hilangkan padding statis 48.dp karena parent sudah memberi margin/padding (CashierProfileScreen)
            // Cukup berikan padding agar konten tidak mepet jika di-scroll ke mentok ujung
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Header Page
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Primary.copy(alpha = 0.12f))
                        .padding(14.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Account Profile", color = Slate900, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text(
                        text = "Manage your personal information and terminal preferences.",
                        color = Slate500,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Primary.copy(alpha = 0.14f))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Verified, contentDescription = null, tint = Primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(if (isActive) "Profil Aktif" else "Profil Inaktif", color = Primary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Layout Responsif: Membagi Kiri (Form) dan Kanan (Stats)
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isDesktop = maxWidth > 800.dp

            if (isDesktop) {
                Row(horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                    ProfileFormSection(modifier = Modifier.weight(2f), fullName, username, email, role, { fullName = it }, { username = it }, { email = it })
                    AccountStatsSection(modifier = Modifier.weight(1f), isActive = isActive, joinedAt = joinedAt, lastLoginAt = lastLoginAt)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(48.dp)) {
                    ProfileFormSection(modifier = Modifier.fillMaxWidth(), fullName, username, email, role, { fullName = it }, { username = it }, { email = it })
                    AccountStatsSection(modifier = Modifier.fillMaxWidth(), isActive = isActive, joinedAt = joinedAt, lastLoginAt = lastLoginAt)
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Footer Placeholder
        Box(modifier = Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) {
            Text("© 2025 TB TERMINAL V.1.0.0", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate400, letterSpacing = 1.sp)
        }
    }
}

// ==========================================
// SEGMEN KIRI: FORM PROFIL
// ==========================================
@Composable
fun ProfileFormSection(
    modifier: Modifier = Modifier,
    fullName: String, username: String, email: String, role: String,
    onNameChange: (String) -> Unit, onUserChange: (String) -> Unit, onEmailChange: (String) -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // Avatar Area
        Box(modifier = Modifier.padding(bottom = 16.dp), contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier.size(128.dp).clip(CircleShape).background(Slate50).border(1.dp, Slate100, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(fullName.take(1).uppercase(Locale.ROOT), fontSize = 48.sp, fontWeight = FontWeight.Black, color = Primary)
            }
            IconButton(
                onClick = { /* Ubah Foto */ },
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.White).border(1.dp, Slate100, CircleShape)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Avatar", tint = Slate500, modifier = Modifier.size(20.dp))
            }
        }
        
        Text(username, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Slate900)
        Text(role, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Primary, letterSpacing = 2.sp, modifier = Modifier.padding(top = 4.dp, bottom = 48.dp))

        // Form Fields (Max Width dibatasi agar tidak terlalu lebar di layar besar)
        Column(modifier = Modifier.widthIn(max = 500.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(32.dp)) {
            UnderlinedInputField("NAMA LENGKAP", fullName, onNameChange, icon = Icons.Default.Badge)
            UnderlinedInputField("USERNAME", username, onUserChange, icon = Icons.Default.Person)
            UnderlinedInputField("EMAIL", email, onEmailChange, icon = Icons.Default.AlternateEmail)
            UnderlinedInputField("ROLE", role, {}, isEnabled = false, icon = Icons.Default.AdminPanelSettings)

            Button(
                onClick = { /* Handle Ktor PUT Request */ },
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 16.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Simpan Perubahan", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ==========================================
// SEGMEN KANAN: STATISTIK AKUN
// ==========================================
@Composable
fun AccountStatsSection(
    modifier: Modifier = Modifier,
    isActive: Boolean,
    joinedAt: String,
    lastLoginAt: String?
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Slate100)
    ) {
        Column(modifier = Modifier.padding(32.dp)) {
            Text("ACCOUNT STATS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Slate900, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 32.dp))

            StatRow(icon = Icons.Default.CalendarToday, label = "Joined", value = formatDateTimeToDate(joinedAt))
            Spacer(modifier = Modifier.height(24.dp))
            
            // Status Active
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = Slate400, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Status", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate500)
                }
                Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(if (isActive) "ACTIVE" else "INACTIVE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Slate500, letterSpacing = 1.sp)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Slate200)
            
            Text("Last login: ${lastLoginAt?.let { formatDateTimeToTime(it) } ?: "-"}", fontSize = 10.sp, color = Slate400, style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
        }
    }
}

// Helper to format ISO DateTime to readable format (simplified)
fun formatDateTimeToDate(isoString: String): String {
    try {
        if (isoString.length >= 10) {
            val parts = isoString.substring(0, 10).split("-")
            if (parts.size == 3) {
                val months = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                val monthIndex = parts[1].toIntOrNull() ?: 1
                return "${months.getOrNull(monthIndex) ?: ""} ${parts[0]}"
            }
        }
    } catch (e: Exception) {}
    return isoString
}

fun formatDateTimeToTime(isoString: String): String {
    try {
        if (isoString.length >= 16) {
            val dateParts = isoString.substring(0, 10)
            val timeParts = isoString.substring(11, 16)
            return "$dateParts at $timeParts"
        }
    } catch (e: Exception) {}
    return isoString
}

// ==========================================
// KOMPONEN INPUT KUSTOM
// ==========================================
@Composable
fun UnderlinedInputField(label: String, value: String, onValueChange: (String) -> Unit, isEnabled: Boolean = true, icon: ImageVector? = null) {
    Column {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isEnabled) Slate400 else Slate300, letterSpacing = 1.sp)
        TextField(
            value = value,
            onValueChange = onValueChange,
            enabled = isEnabled,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = icon?.let { 
                { Icon(it, contentDescription = null, tint = if (isEnabled) Primary else Slate300) }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Primary,
                unfocusedIndicatorColor = Slate400,
                disabledIndicatorColor = Slate200,
                focusedTextColor = Slate900,
                unfocusedTextColor = Slate900,
                disabledTextColor = Slate500
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
fun StatRow(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Slate400, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Slate500)
        }
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate900)
    }
}
