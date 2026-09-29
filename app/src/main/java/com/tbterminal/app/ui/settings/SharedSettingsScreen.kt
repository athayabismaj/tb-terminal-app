package com.tbterminal.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tbterminal.app.navigation.AppAccessPolicy
import com.tbterminal.app.navigation.AppCapability
import com.tbterminal.app.ui.components.SkeletonCard
import com.tbterminal.app.ui.theme.TbterminalappTheme

enum class SettingsPage(val key: String, val title: String, val subtitle: String) {
    STORE("store", "Identitas toko", "Nama dan informasi kontak toko"),
    RECEIPT("receipt", "Tampilan struk", "Teks pembuka dan penutup struk"),
    DEVICE("device", "Perangkat", "Printer dan preferensi terminal"),
}

internal fun visibleSettingsPages(role: String): List<SettingsPage> = buildList {
    if (AppAccessPolicy.can(role, AppCapability.STORE_SETTINGS)) {
        add(SettingsPage.STORE)
        add(SettingsPage.RECEIPT)
    }
    if (AppAccessPolicy.can(role, AppCapability.DEVICE_SETTINGS)) add(SettingsPage.DEVICE)
}

@Composable
fun SharedSettingsScreen(
    role: String,
    selectedPage: SettingsPage?,
    onPageSelected: (SettingsPage) -> Unit,
    onPageBack: () -> Unit,
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
    onCashToleranceChanged: (String) -> Unit,
    onAutoLockMinutesChanged: (String) -> Unit,
    onAutoPrintReceiptChanged: (Boolean) -> Unit,
    onSelectPrinter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pages = visibleSettingsPages(role)
    val currentPage = selectedPage?.takeIf { it in pages }
    BackHandler(enabled = currentPage != null && pages.size > 1, onBack = onPageBack)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("settings-screen"),
    ) {
        val wide = maxWidth >= 840.dp
        val pagePadding = if (wide) 28.dp else 16.dp

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 1180.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = pagePadding, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            uiState.error?.let {
                SettingsMessage(text = it, error = true)
                OutlinedButton(onClick = onReload, enabled = !uiState.isLoading) { Text("Coba lagi") }
            }
            uiState.message?.let { SettingsMessage(text = it, error = false) }

            if (uiState.isLoading) {
                SettingsLoading(wide)
            } else if (currentPage == null) {
                SettingsMenu(pages = pages, wide = wide, onPageSelected = onPageSelected)
            } else {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = if (wide) 980.dp else 720.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        when (currentPage) {
                            SettingsPage.STORE -> StoreIdentityPanel(
                                uiState, wide, onStoreNameChanged, onAddressChanged, onPhoneChanged,
                                onSaveStoreSettings,
                            )
                            SettingsPage.RECEIPT -> ReceiptPanel(
                                uiState, wide, onReceiptHeaderChanged, onReceiptFooterChanged,
                                onSaveStoreSettings,
                            )
                            SettingsPage.DEVICE -> DevicePanel(
                                uiState, wide, onPrinterSizeChanged, onCashToleranceChanged,
                                onAutoLockMinutesChanged, onAutoPrintReceiptChanged,
                                onSelectPrinter, onSaveLocalPreferences,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SettingsMenu(
    pages: List<SettingsPage>,
    wide: Boolean,
    onPageSelected: (SettingsPage) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("settings-menu"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Pilih pengaturan yang ingin diubah.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (wide) {
            pages.chunked(2).forEach { rowPages ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowPages.forEach { page ->
                        SettingsMenuItem(page, onPageSelected, Modifier.weight(1f))
                    }
                    if (rowPages.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        } else {
            pages.forEach { page -> SettingsMenuItem(page, onPageSelected, Modifier.fillMaxWidth()) }
        }
    }
}

@Composable
private fun SettingsMenuItem(
    page: SettingsPage,
    onPageSelected: (SettingsPage) -> Unit,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier.testTag("settings-menu-${page.key}").clickable { onPageSelected(page) },
        color = settingsCardColor(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = page.icon(), contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(page.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    page.subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = "Buka ${page.title}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun SettingsPage.icon(): ImageVector = when (this) {
    SettingsPage.STORE -> Icons.Outlined.Storefront
    SettingsPage.RECEIPT, SettingsPage.DEVICE -> Icons.Outlined.Print
}

@Composable
private fun StoreIdentityPanel(
    uiState: SettingsUiState,
    wide: Boolean,
    onStoreNameChanged: (String) -> Unit,
    onAddressChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onSave: () -> Unit,
) {
    SettingsPanel(
        title = "Informasi toko",
        subtitle = "Digunakan pada struk dan dokumen toko.",
        icon = Icons.Outlined.Storefront,
        tag = "settings-section-store",
    ) {
        if (wide) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsField("Nama toko", uiState.storeName, onStoreNameChanged, Modifier.weight(1f))
                SettingsField("Nomor telepon", uiState.phone, onPhoneChanged, Modifier.weight(1f))
            }
        } else {
            SettingsField("Nama toko", uiState.storeName, onStoreNameChanged)
            SettingsField("Nomor telepon", uiState.phone, onPhoneChanged)
        }
        SettingsField("Alamat toko", uiState.address, onAddressChanged, minLines = 2)
        SettingsSaveButton(
            text = if (uiState.isSaving) "Menyimpan..." else "Simpan identitas toko",
            enabled = !uiState.isSaving,
            fullWidth = !wide,
            onClick = onSave,
        )
    }
}

@Composable
private fun ReceiptPanel(
    uiState: SettingsUiState,
    wide: Boolean,
    onReceiptHeaderChanged: (String) -> Unit,
    onReceiptFooterChanged: (String) -> Unit,
    onSave: () -> Unit,
) {
    SettingsPanel(
        title = "Teks struk",
        subtitle = "Sesuaikan pesan yang tercetak pada struk.",
        icon = Icons.Outlined.Print,
        tag = "settings-section-receipt",
    ) {
        if (wide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.Top,
            ) {
                ReceiptEditor(
                    uiState = uiState,
                    onReceiptHeaderChanged = onReceiptHeaderChanged,
                    onReceiptFooterChanged = onReceiptFooterChanged,
                    modifier = Modifier.weight(1.15f),
                )
                ReceiptPreview(uiState = uiState, modifier = Modifier.weight(0.85f))
            }
        } else {
            ReceiptEditor(uiState, onReceiptHeaderChanged, onReceiptFooterChanged)
            ReceiptPreview(uiState = uiState)
        }
        SettingsSaveButton(
            text = if (uiState.isSaving) "Menyimpan..." else "Simpan tampilan struk",
            enabled = !uiState.isSaving,
            fullWidth = !wide,
            onClick = onSave,
        )
    }
}

@Composable
private fun ReceiptEditor(
    uiState: SettingsUiState,
    onReceiptHeaderChanged: (String) -> Unit,
    onReceiptFooterChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsField("Teks atas", uiState.receiptHeader, onReceiptHeaderChanged)
        SettingsField("Teks bawah", uiState.receiptFooter, onReceiptFooterChanged, minLines = 3)
    }
}

@Composable
private fun ReceiptPreview(uiState: SettingsUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.testTag("settings-receipt-preview"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Pratinjau",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    uiState.storeName.ifBlank { "Nama toko" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (uiState.receiptHeader.isNotBlank()) {
                    Text(
                        uiState.receiptHeader,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                ReceiptPreviewRow("Contoh transaksi", "Rp100.000")
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    uiState.receiptFooter.ifBlank { "Terima kasih" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReceiptPreviewRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DevicePanel(
    uiState: SettingsUiState,
    wide: Boolean,
    onPrinterSizeChanged: (String) -> Unit,
    onCashToleranceChanged: (String) -> Unit,
    onAutoLockMinutesChanged: (String) -> Unit,
    onAutoPrintReceiptChanged: (Boolean) -> Unit,
    onSelectPrinter: () -> Unit,
    onSave: () -> Unit,
) {
    SettingsPanel(
        title = "Perangkat",
        subtitle = "Preferensi yang berlaku pada terminal ini.",
        icon = Icons.Outlined.Print,
        tag = "settings-section-device",
    ) {
        PrinterSelector(uiState.selectedPrinterName, uiState.printerSize, onSelectPrinter)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text("Ukuran kertas", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaperSizeChip("58 mm", uiState.printerSize == "58mm") { onPrinterSizeChanged("58mm") }
            PaperSizeChip("80 mm", uiState.printerSize == "80mm") { onPrinterSizeChanged("80mm") }
        }
        SettingsSwitchRow(
            "Cetak struk otomatis",
            "Buka layanan cetak setelah transaksi berhasil.",
            uiState.autoPrintReceipt,
            onAutoPrintReceiptChanged,
        )
        if (wide) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsField(
                    "Toleransi selisih kas", uiState.cashTolerance, onCashToleranceChanged,
                    Modifier.weight(1f), prefix = "Rp",
                )
                SettingsField(
                    "Kunci otomatis", uiState.autoLockMinutes, onAutoLockMinutesChanged,
                    Modifier.weight(1f), suffix = "menit",
                )
            }
        } else {
            SettingsField("Toleransi selisih kas", uiState.cashTolerance, onCashToleranceChanged, prefix = "Rp")
            SettingsField("Kunci otomatis", uiState.autoLockMinutes, onAutoLockMinutesChanged, suffix = "menit")
        }
        SettingsSaveButton(
            text = if (uiState.isSaving) "Menyimpan..." else "Simpan pengaturan perangkat",
            enabled = !uiState.isSaving,
            fullWidth = !wide,
            onClick = onSave,
        )
    }
}

@Composable
private fun SettingsPanel(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tag: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag(tag),
        color = settingsCardColor(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon, null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(21.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun PrinterSelector(printerName: String, paperSize: String, onSelectPrinter: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(printerName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                "Kertas $paperSize", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilledTonalButton(onClick = onSelectPrinter) {
            Icon(Icons.Outlined.Print, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Pilih")
        }
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
    minLines: Int = 1,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = prefix?.let { { Text(it, fontWeight = FontWeight.SemiBold) } },
        trailingIcon = suffix?.let { { Text(it, style = MaterialTheme.typography.labelMedium) } },
        minLines = minLines,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun PaperSizeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Outlined.CheckCircle, null, modifier = Modifier.size(18.dp)) }
        } else null,
    )
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsSaveButton(
    text: String,
    enabled: Boolean,
    fullWidth: Boolean,
    onClick: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
                .height(48.dp),
        ) {
            Icon(Icons.Outlined.Save, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text)
        }
    }
}

@Composable
private fun settingsCardColor(): Color =
    if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface else Color.White

@Composable
private fun SettingsMessage(text: String, error: Boolean) {
    Surface(
        color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun SettingsLoading(wide: Boolean) {
    val skeleton: @Composable (Modifier) -> Unit = { skeletonModifier ->
        Column(
            modifier = skeletonModifier.testTag("settings-skeleton"),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SkeletonCard()
            SkeletonCard()
        }
    }
    if (wide) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            skeleton(Modifier.weight(1f))
            skeleton(Modifier.weight(1f))
        }
    } else {
        skeleton(Modifier.fillMaxWidth())
    }
}

@Preview(name = "Pengaturan - Phone", widthDp = 390, heightDp = 844, showBackground = true)
@Preview(name = "Pengaturan - Tablet", widthDp = 1180, heightDp = 800, showBackground = true)
@Composable
private fun SharedSettingsScreenPreview() {
    TbterminalappTheme {
        SharedSettingsScreen(
            role = "OWNER",
            selectedPage = null,
            onPageSelected = {},
            onPageBack = {},
            uiState = SettingsUiState(storeName = "Toko Berkah", phone = "0812 3456 7890"),
            onReload = {}, onSaveStoreSettings = {}, onSaveLocalPreferences = {},
            onStoreNameChanged = {}, onAddressChanged = {}, onPhoneChanged = {},
            onReceiptHeaderChanged = {}, onReceiptFooterChanged = {}, onPrinterSizeChanged = {},
            onCashToleranceChanged = {}, onAutoLockMinutesChanged = {},
            onAutoPrintReceiptChanged = {}, onSelectPrinter = {},
        )
    }
}
