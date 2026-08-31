package com.tbterminal.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.dashboard.DashboardBackground
import com.tbterminal.app.ui.dashboard.DashboardBrandGreen
import com.tbterminal.app.ui.dashboard.DashboardBrandGreenDark
import com.tbterminal.app.ui.dashboard.DashboardTextPrimary
import com.tbterminal.app.ui.dashboard.DashboardTextSecondary

private data class SettingsTab(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@Composable
fun SharedSettingsScreen(
    userName: String,
    role: String,
    uiState: SettingsUiState,
    onReload: () -> Unit,
    onSaveStoreSettings: () -> Unit,
    onSaveLocalPreferences: () -> Unit,
    onStoreNameChanged: (String) -> Unit,
    onAddressChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onReceiptHeaderChanged: (String) -> Unit,
    onReceiptFooterChanged: (String) -> Unit,
    onPrinterSizeChanged: (String) -> Unit,
    onDefaultCreditLimitChanged: (String) -> Unit,
    onDefaultTermDaysChanged: (String) -> Unit,
    onCashToleranceChanged: (String) -> Unit,
    onAutoLockMinutesChanged: (String) -> Unit,
    onAutoPrintReceiptChanged: (Boolean) -> Unit,
    onBarcodeScannerChanged: (Boolean) -> Unit,
    onOfflineCacheChanged: (Boolean) -> Unit,
    onSelectPrinter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isManagementRole = !role.equals("kasir", ignoreCase = true)
    val tabs = buildList {
        if (isManagementRole) {
            add(SettingsTab("store", "Toko & Struk", "Header, footer, ukuran printer", Icons.Outlined.Storefront))
            add(SettingsTab("operation", "Operasional", "Limit kredit, termin, kas", Icons.Outlined.Payments))
        }
        add(SettingsTab("security", "Keamanan", "Kebijakan akses akun", Icons.Outlined.Lock))
        add(SettingsTab("device", "Perangkat", "Printer dan scanner kasir", Icons.Outlined.Print))
        if (isManagementRole) {
            add(SettingsTab("sync", "Backup & Sync", "Status sinkronisasi data", Icons.Outlined.Backup))
        }
    }
    var selectedTab by rememberSaveable(role) { mutableStateOf(tabs.first().key) }

    LaunchedEffect(role) {
        if (tabs.none { it.key == selectedTab }) {
            selectedTab = tabs.first().key
        }
    }

    val content: @Composable (Modifier) -> Unit = { contentModifier ->
        SettingsContentCard(
            title = if (isManagementRole) "Pengaturan Sistem" else "Pengaturan Kasir",
            subtitle = if (isManagementRole) {
                "Kelola identitas toko, struk, perangkat, dan preferensi operasional."
            } else {
                "Kelola preferensi perangkat kasir yang dipakai pada terminal ini."
            },
            userName = userName,
            uiState = uiState,
            onReload = onReload,
            modifier = contentModifier
        ) {
            when (selectedTab) {
                "store" -> StoreSettingsContent(
                    uiState = uiState,
                    onStoreNameChanged = onStoreNameChanged,
                    onAddressChanged = onAddressChanged,
                    onPhoneChanged = onPhoneChanged,
                    onReceiptHeaderChanged = onReceiptHeaderChanged,
                    onReceiptFooterChanged = onReceiptFooterChanged,
                    onPrinterSizeChanged = onPrinterSizeChanged,
                    onSave = onSaveStoreSettings
                )
                "operation" -> OperationalSettingsContent(
                    uiState = uiState,
                    onDefaultCreditLimitChanged = onDefaultCreditLimitChanged,
                    onDefaultTermDaysChanged = onDefaultTermDaysChanged,
                    onCashToleranceChanged = onCashToleranceChanged,
                    onAutoLockMinutesChanged = onAutoLockMinutesChanged,
                    onSave = onSaveLocalPreferences
                )
                "security" -> SecuritySettingsContent(
                    isManagementRole = isManagementRole,
                    onSave = onSaveLocalPreferences
                )
                "device" -> DeviceSettingsContent(
                    uiState = uiState,
                    onAutoPrintReceiptChanged = onAutoPrintReceiptChanged,
                    onBarcodeScannerChanged = onBarcodeScannerChanged,
                    onOfflineCacheChanged = onOfflineCacheChanged,
                    onSelectPrinter = onSelectPrinter,
                    onSave = onSaveLocalPreferences
                )
                "sync" -> BackupSettingsContent(
                    uiState = uiState,
                    onSave = onSaveLocalPreferences
                )
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(DashboardBackground)) {
        val compact = maxWidth < 900.dp
        if (compact) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsCompactMenu(tabs, selectedTab) { selectedTab = it }
                content(Modifier.weight(1f).fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                SettingsSideMenu(
                    tabs = tabs,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    modifier = Modifier.width(286.dp).fillMaxHeight()
                )
                content(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SettingsCompactMenu(
    tabs: List<SettingsTab>,
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { tab ->
            val selected = selectedTab == tab.key
            Surface(
                onClick = { onTabSelected(tab.key) },
                color = if (selected) DashboardBrandGreenDark else Color.White,
                contentColor = if (selected) Color.White else DashboardTextPrimary,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, if (selected) DashboardBrandGreenDark else Color(0xFFE1E8EC))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(tab.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(tab.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SettingsSideMenu(
    tabs: List<SettingsTab>,
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFFE1E8EC))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Menu Pengaturan",
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DashboardTextPrimary,
                modifier = Modifier.padding(start = 6.dp, bottom = 18.dp)
            )
            tabs.forEach { tab ->
                SettingsMenuItem(
                    tab = tab,
                    selected = selectedTab == tab.key,
                    onClick = { onTabSelected(tab.key) }
                )
            }
        }
    }
}

@Composable
private fun SettingsMenuItem(
    tab: SettingsTab,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (selected) DashboardBrandGreen.copy(alpha = 0.14f) else Color.Transparent
    val iconBg = if (selected) DashboardBrandGreenDark else Color(0xFFF0F5F6)
    val textColor = if (selected) DashboardBrandGreenDark else DashboardTextPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = null,
                tint = if (selected) Color.White else DashboardTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tab.title,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = tab.subtitle,
                color = DashboardTextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingsContentCard(
    title: String,
    subtitle: String,
    userName: String,
    uiState: SettingsUiState,
    onReload: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFFE1E8EC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = DashboardTextPrimary)
                    Text(subtitle, color = DashboardTextSecondary, fontSize = 14.sp)
                    Text("Akun aktif: $userName", color = DashboardTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
                OutlinedButton(onClick = onReload, enabled = !uiState.isLoading && !uiState.isSaving) {
                    Text("Muat Ulang")
                }
            }

            if (uiState.isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Memuat pengaturan...", color = DashboardTextSecondary)
                }
            }
            uiState.error?.let { SettingsMessage(text = it, error = true) }
            uiState.message?.let { SettingsMessage(text = it, error = false) }

            HorizontalDivider(color = Color(0xFFE8EEF2))
            content()
        }
    }
}

@Composable
private fun SettingsMessage(text: String, error: Boolean) {
    Surface(
        color = if (error) Color(0xFFFFE4E6) else Color(0xFFDFF7ED),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            color = if (error) Color(0xFFB91C1C) else DashboardBrandGreenDark,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun StoreSettingsContent(
    uiState: SettingsUiState,
    onStoreNameChanged: (String) -> Unit,
    onAddressChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onReceiptHeaderChanged: (String) -> Unit,
    onReceiptFooterChanged: (String) -> Unit,
    onPrinterSizeChanged: (String) -> Unit,
    onSave: () -> Unit
) {
    SettingsSection("Identitas Toko", "Data ini dipakai di header struk dan dokumen transaksi.") {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            SettingsField("Nama toko", uiState.storeName, onStoreNameChanged, Modifier.weight(1f))
            SettingsField("Nomor telepon", uiState.phone, onPhoneChanged, Modifier.weight(1f))
        }
        SettingsField("Alamat toko", uiState.address, onAddressChanged, Modifier.fillMaxWidth())
    }

    SettingsSection("Format Struk", "Atur teks yang muncul saat struk dicetak.") {
        SettingsField("Header struk", uiState.receiptHeader, onReceiptHeaderChanged, Modifier.fillMaxWidth())
        SettingsField("Footer struk", uiState.receiptFooter, onReceiptFooterChanged, Modifier.fillMaxWidth(), minLines = 3)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrinterSizeChip("58mm", uiState.printerSize == "58mm", onPrinterSizeChanged)
            PrinterSizeChip("80mm", uiState.printerSize == "80mm", onPrinterSizeChanged)
        }
    }

    PrimarySettingsButton(
        text = if (uiState.isSaving) "Menyimpan..." else "Simpan Pengaturan Toko",
        enabled = !uiState.isSaving && !uiState.isLoading,
        onClick = onSave
    )
}

@Composable
private fun OperationalSettingsContent(
    uiState: SettingsUiState,
    onDefaultCreditLimitChanged: (String) -> Unit,
    onDefaultTermDaysChanged: (String) -> Unit,
    onCashToleranceChanged: (String) -> Unit,
    onAutoLockMinutesChanged: (String) -> Unit,
    onSave: () -> Unit
) {
    SettingsSection("Piutang & Termin", "Default untuk pelanggan baru dan transaksi non-tunai.") {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            SettingsField("Limit kredit default", uiState.defaultCreditLimit, onDefaultCreditLimitChanged, Modifier.weight(1f), prefix = "Rp")
            SettingsField("Termin default", uiState.defaultTermDays, onDefaultTermDaysChanged, Modifier.weight(1f), suffix = "hari")
        }
    }
    SettingsSection("Kas & Keamanan Sesi", "Preferensi operasional terminal saat shift aktif.") {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            SettingsField("Toleransi selisih kas", uiState.cashTolerance, onCashToleranceChanged, Modifier.weight(1f), prefix = "Rp")
            SettingsField("Auto lock kasir", uiState.autoLockMinutes, onAutoLockMinutesChanged, Modifier.weight(1f), suffix = "menit")
        }
    }
    PrimarySettingsButton("Simpan Preferensi Operasional", enabled = true, onClick = onSave)
}

@Composable
private fun SecuritySettingsContent(
    isManagementRole: Boolean,
    onSave: () -> Unit
) {
    SettingsSection("Kebijakan Akses", "Aturan keamanan yang perlu dijaga saat terminal dipakai.") {
        SecurityPolicyItem("PIN kasir wajib 6 digit", "Dipakai untuk unlock terminal setelah sesi terkunci.")
        SecurityPolicyItem("Password minimal 6 karakter", "Perubahan password dan PIN tetap dilakukan dari Manajemen Pengguna agar tercatat di audit.")
        SecurityPolicyItem("Role dibatasi per fungsi", if (isManagementRole) "Owner/admin bisa mengelola operasional sesuai hak akses." else "Kasir hanya mengakses POS, stok cek, riwayat, dan kas sendiri.")
    }
    PrimarySettingsButton("Simpan Preferensi Keamanan", enabled = true, onClick = onSave)
}

@Composable
private fun DeviceSettingsContent(
    uiState: SettingsUiState,
    onAutoPrintReceiptChanged: (Boolean) -> Unit,
    onBarcodeScannerChanged: (Boolean) -> Unit,
    onOfflineCacheChanged: (Boolean) -> Unit,
    onSelectPrinter: () -> Unit,
    onSave: () -> Unit
) {
    SettingsSection("Printer Struk", "Preferensi perangkat kasir pada terminal ini.") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF4F8FA), RoundedCornerShape(14.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(uiState.selectedPrinterName, fontWeight = FontWeight.Bold, color = DashboardTextPrimary)
                Text("Ukuran struk aktif ${uiState.printerSize}", color = DashboardTextSecondary, fontSize = 12.sp)
            }
            OutlinedButton(onClick = onSelectPrinter) {
                Icon(Icons.Outlined.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Uji / Pilih Printer")
            }
        }
        SettingsSwitchRow("Cetak struk otomatis", "Struk langsung dicetak setelah transaksi berhasil.", uiState.autoPrintReceipt, onAutoPrintReceiptChanged)
        SettingsSwitchRow("Scanner barcode aktif", "Fitur barcode lanjutan belum termasuk pada Batch 8A.", uiState.barcodeScannerEnabled, onBarcodeScannerChanged)
        SettingsSwitchRow("Cache offline terminal", "Offline sync hanya untuk transaksi, sesi kas, dan pengeluaran kas.", uiState.offlineCacheEnabled, onOfflineCacheChanged)
    }
    PrimarySettingsButton("Simpan Preferensi Perangkat", enabled = true, onClick = onSave)
}

@Composable
private fun BackupSettingsContent(
    uiState: SettingsUiState,
    onSave: () -> Unit
) {
    SettingsSection("Status Sinkronisasi", "Ringkasan kesiapan data operasional.") {
        InfoTile("Backend", "Terhubung melalui API utama", DashboardBrandGreenDark)
        InfoTile("Pengaturan toko", if (uiState.error == null) "Siap digunakan" else "Perlu dimuat ulang", if (uiState.error == null) DashboardBrandGreenDark else Color(0xFFB91C1C))
        InfoTile("Offline sync", "Transaksi, sesi kas, dan pengeluaran kas saja.", DashboardTextSecondary)
    }
    PrimarySettingsButton("Tandai Sudah Dicek", enabled = true, onClick = onSave)
}

@Composable
private fun SettingsSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Column {
            Text(title, color = DashboardTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text(subtitle, color = DashboardTextSecondary, fontSize = 13.sp)
        }
        content()
    }
}

@Composable
private fun SettingsField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    prefix: String? = null,
    suffix: String? = null,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = prefix?.let { { Text(it, fontWeight = FontWeight.Bold, color = DashboardBrandGreenDark) } },
        trailingIcon = suffix?.let { { Text(it, color = DashboardTextSecondary, fontSize = 12.sp) } },
        minLines = minLines,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
private fun PrinterSizeChip(
    label: String,
    selected: Boolean,
    onClick: (String) -> Unit
) {
    Surface(
        color = if (selected) DashboardBrandGreenDark else Color.White,
        contentColor = if (selected) Color.White else DashboardTextPrimary,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, if (selected) DashboardBrandGreenDark else Color(0xFFDCE5EA)),
        modifier = Modifier.clickable { onClick(label) }
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFB), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = DashboardTextPrimary, fontWeight = FontWeight.Bold)
            Text(subtitle, color = DashboardTextSecondary, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SecurityPolicyItem(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFB), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(DashboardBrandGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = DashboardBrandGreenDark, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, color = DashboardTextPrimary, fontWeight = FontWeight.Bold)
            Text(subtitle, color = DashboardTextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun InfoTile(title: String, subtitle: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFB), RoundedCornerShape(14.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = DashboardTextPrimary, fontWeight = FontWeight.Bold)
            Text(subtitle, color = DashboardTextSecondary, fontSize = 12.sp)
        }
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
private fun PrimarySettingsButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = DashboardBrandGreenDark),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.height(52.dp)
    ) {
        Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.Bold)
    }
}
