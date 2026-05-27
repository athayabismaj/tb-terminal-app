package com.tbterminal.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Print
import /* androidx.compose.material.icons.outlined.Store */ androidx.compose.material.icons.outlined.Storefront
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
import com.tbterminal.app.ui.dashboard.DashboardBrandGreen
import com.tbterminal.app.ui.dashboard.DashboardBrandGreenDark
import com.tbterminal.app.ui.dashboard.DashboardTextPrimary
import com.tbterminal.app.ui.dashboard.DashboardTextSecondary

@Composable
fun SharedSettingsScreen(
    userName: String,
    role: String,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf("Toko") }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Left Column: Settings Menu
        Card(
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Pengaturan",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DashboardTextPrimary,
                    modifier = Modifier.padding(bottom = 24.dp, start = 8.dp, top = 8.dp)
                )

                
                
                
                SettingsMenuItem(
                    icon = Icons.Outlined.Lock,
                    title = "Keamanan",
                    subtitle = "Ubah password akun Anda",
                    isSelected = selectedTab == "Keamanan",
                    onClick = { selectedTab = "Keamanan" }
                )

                SettingsMenuItem(
                    icon = Icons.Outlined.Storefront,
                    title = "Toko & Struk",
                    subtitle = "Info toko di nota kasir",
                    isSelected = selectedTab == "Toko",
                    onClick = { selectedTab = "Toko" }
                )

                SettingsMenuItem(
                    icon = Icons.Outlined.Print,
                    title = "Printer",
                    subtitle = "Koneksi bluetooth printer",
                    isSelected = selectedTab == "Printer",
                    onClick = { selectedTab = "Printer" }
                )
            }
        }

        // Right Column: Settings Content
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                when (selectedTab) {
                    "Keamanan" -> SecurityContent()
                    "Toko" -> StoreContent()
                    "Printer" -> PrinterContent()
                }
            }
        }
    }
}

@Composable
private fun SettingsMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) DashboardBrandGreen.copy(alpha = 0.1f) else Color.Transparent
    val contentColor = if (isSelected) DashboardBrandGreenDark else DashboardTextPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isSelected) DashboardBrandGreenDark else Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else DashboardTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, color = contentColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, color = DashboardTextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SecurityContent() {
    Text("Keamanan Akun", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = DashboardTextPrimary)
    Text("Kelola password untuk keamanan akun Anda.", color = DashboardTextSecondary, fontSize = 14.sp)
    
    Spacer(modifier = Modifier.height(32.dp))

    OutlinedTextField(
        value = "",
        onValueChange = {},
        label = { Text("Password Lama") },
        modifier = Modifier.fillMaxWidth(0.6f),
        shape = RoundedCornerShape(12.dp)
    )
    
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = "",
        onValueChange = {},
        label = { Text("Password Baru") },
        modifier = Modifier.fillMaxWidth(0.6f),
        shape = RoundedCornerShape(12.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))
    
    OutlinedTextField(
        value = "",
        onValueChange = {},
        label = { Text("Konfirmasi Password Baru") },
        modifier = Modifier.fillMaxWidth(0.6f),
        shape = RoundedCornerShape(12.dp)
    )

    Spacer(modifier = Modifier.height(32.dp))
    
    Button(
        onClick = { /* TODO */ },
        colors = ButtonDefaults.buttonColors(containerColor = DashboardBrandGreenDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Perbarui Password", fontWeight = FontWeight.Bold)
    }
}


@Composable
private fun StoreContent() {
    Text("Informasi Toko", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = DashboardTextPrimary)
    Text("Informasi ini akan dicetak pada bagian header dan footer struk pembayaran.", color = DashboardTextSecondary, fontSize = 14.sp)
    
    Spacer(modifier = Modifier.height(32.dp))

    OutlinedTextField(
        value = "TB Terminal",
        onValueChange = {},
        label = { Text("Nama Toko") },
        modifier = Modifier.fillMaxWidth(0.6f),
        enabled = false
    )
    
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = "Jl. Contoh Alamat No. 123",
        onValueChange = {},
        label = { Text("Alamat") },
        modifier = Modifier.fillMaxWidth(0.6f),
        enabled = false
    )
    
    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = "Terima Kasih Telah Berbelanja\nBarang yang sudah dibeli tidak dapat ditukar.",
        onValueChange = {},
        label = { Text("Pesan Footer Struk") },
        modifier = Modifier.fillMaxWidth(0.6f),
        minLines = 3,
        enabled = false
    )
}

@Composable
private fun PrinterContent() {
    Text("Pengaturan Printer", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = DashboardTextPrimary)
    Text("Hubungkan perangkat dengan printer bluetooth kasir.", color = DashboardTextSecondary, fontSize = 14.sp)
    
    Spacer(modifier = Modifier.height(32.dp))

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Printer Belum Terhubung", fontWeight = FontWeight.Bold, color = DashboardTextPrimary)
                Text("Pastikan bluetooth aktif dan printer menyala.", color = DashboardTextSecondary, fontSize = 14.sp)
            }
            Button(
                onClick = { /* TODO */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = DashboardTextPrimary),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Text("Cari Printer", fontWeight = FontWeight.Bold)
            }
        }
    }
}
