package com.tbterminal.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.tbterminal.app.ui.components.TopFloatingCapsule
import kotlinx.coroutines.delay
import com.tbterminal.app.data.printer.PrinterConnectionType
import com.tbterminal.app.data.printer.PrinterDeviceItem
import com.tbterminal.app.navigation.AppAccessPolicy
import com.tbterminal.app.navigation.AppCapability
import com.tbterminal.app.ui.components.SkeletonCard
import com.tbterminal.app.ui.theme.TbterminalappTheme

private val Brand500 = Color(0xFF256B57)
private val Brand600 = Color(0xFF1E5847)
private val Brand100 = Color(0xFFE8F2EE)
private val Brand50 = Color(0xFFF2F7F5)
private val BackgroundColor = Color(0xFFF8FAF9)
private val SurfaceMuted = Color(0xFFF8FAFB)
private val Slate900 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate600 = Color(0xFF475569)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate300 = Color(0xFFCBD5E1)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate100 = Color(0xFFF1F5F9)

enum class SettingsPage(val key: String, val title: String, val subtitle: String) {
    STORE("store", "Identitas toko", "Nama dan informasi kontak toko"),
    RECEIPT("receipt", "Tampilan struk", "Teks pembuka dan penutup struk"),
    DEVICE("device", "Perangkat & Struk", "Printer thermal, laci kasir, dan desain nota"),
}

internal fun visibleSettingsPages(role: String): List<SettingsPage> = buildList {
    if (AppAccessPolicy.can(role, AppCapability.STORE_SETTINGS)) {
        add(SettingsPage.STORE)
    }
    if (AppAccessPolicy.can(role, AppCapability.DEVICE_SETTINGS) || AppAccessPolicy.can(role, AppCapability.STORE_SETTINGS)) {
        add(SettingsPage.DEVICE)
    }
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
    onPrinterConnectionTypeChanged: (PrinterConnectionType) -> Unit = {},
    onPrinterDeviceSelected: (PrinterDeviceItem) -> Unit = {},
    onAutoCutPaperChanged: (Boolean) -> Unit = {},
    onOpenCashDrawerChanged: (Boolean) -> Unit = {},
    onScanPrinters: () -> Unit = {},
    onTestPrint: () -> Unit = {},
    onTestCashDrawer: () -> Unit = {},
    onDismissMessage: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val pages = visibleSettingsPages(role)
    val currentPage = selectedPage?.takeIf { it in pages }
    BackHandler(enabled = currentPage != null && pages.size > 1, onBack = onPageBack)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .testTag("settings-screen"),
    ) {
        val wide = maxWidth >= 840.dp
        val pagePadding = if (wide) 28.dp else 20.dp

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 1180.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = pagePadding, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (uiState.isLoading) {
                SettingsLoading(wide)
            } else if (currentPage == null) {
                if (uiState.error != null && uiState.storeName.isBlank()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), contentAlignment = Alignment.Center) {
                        OutlinedButton(
                            onClick = onReload,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Brand500),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Brand500),
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Muat Ulang Pengaturan")
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    SettingsMenu(pages = pages, wide = wide, onPageSelected = onPageSelected)
                }
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
                            SettingsPage.RECEIPT, SettingsPage.DEVICE -> DeviceAndReceiptPanel(
                                role = role,
                                initialTab = if (currentPage == SettingsPage.RECEIPT) DeviceReceiptTab.RECEIPT else DeviceReceiptTab.DEVICE,
                                uiState = uiState,
                                wide = wide,
                                onReceiptHeaderChanged = onReceiptHeaderChanged,
                                onReceiptFooterChanged = onReceiptFooterChanged,
                                onPrinterSizeChanged = onPrinterSizeChanged,
                                onCashToleranceChanged = onCashToleranceChanged,
                                onAutoLockMinutesChanged = onAutoLockMinutesChanged,
                                onAutoPrintReceiptChanged = onAutoPrintReceiptChanged,
                                onPrinterConnectionTypeChanged = onPrinterConnectionTypeChanged,
                                onPrinterDeviceSelected = onPrinterDeviceSelected,
                                onAutoCutPaperChanged = onAutoCutPaperChanged,
                                onOpenCashDrawerChanged = onOpenCashDrawerChanged,
                                onScanPrinters = onScanPrinters,
                                onTestPrint = onTestPrint,
                                onTestCashDrawer = onTestCashDrawer,
                                onSelectPrinter = onSelectPrinter,
                                onSaveStoreSettings = onSaveStoreSettings,
                                onSaveLocalPreferences = onSaveLocalPreferences,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Top Floating Capsule Notification (Zero Layout Shift)
        TopFloatingCapsule(
            message = uiState.message,
            error = uiState.error,
            onDismiss = onDismissMessage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                .zIndex(99f),
        )
    }
}

@Composable
private fun SettingsMenu(
    pages: List<SettingsPage>,
    wide: Boolean,
    onPageSelected: (SettingsPage) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (wide) Modifier.widthIn(max = 720.dp) else Modifier)
            .testTag("settings-menu"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "Pilih pengaturan yang ingin diubah.",
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Slate500,
            modifier = Modifier.padding(horizontal = 2.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                pages.forEachIndexed { index, page ->
                    SettingsMenuItem(
                        page = page,
                        onPageSelected = onPageSelected,
                    )
                    if (index < pages.lastIndex) {
                        HorizontalDivider(
                            color = Slate100,
                            thickness = 1.dp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsMenuItem(
    page: SettingsPage,
    onPageSelected: (SettingsPage) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings-menu-${page.key}")
            .clickable { onPageSelected(page) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brand100),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = page.icon(),
                    contentDescription = null,
                    tint = Brand500,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = page.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate900,
                    lineHeight = 18.sp,
                )
                Text(
                    text = page.subtitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Slate500,
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = "Buka ${page.title}",
            tint = Slate400,
            modifier = Modifier.size(18.dp),
        )
    }
}

private fun SettingsPage.icon(): ImageVector = when (this) {
    SettingsPage.STORE -> Icons.Outlined.Storefront
    SettingsPage.RECEIPT -> Icons.AutoMirrored.Outlined.ReceiptLong
    SettingsPage.DEVICE -> Icons.Outlined.Print
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings-section-store"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header Store Profile Preview Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Avatar with active checkmark
                Box(modifier = Modifier.size(56.dp)) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brand100)
                            .border(1.dp, Brand500.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Storefront,
                            contentDescription = null,
                            tint = Brand500,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Brand500),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = uiState.storeName.ifBlank { "TB Terminal" },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Surface(
                            color = Brand100,
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                text = "Aktif",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Brand500,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(
                        text = "Profil utama & identitas struk kasir",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Slate500,
                    )
                }
            }
        }

        // Form Container
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Receipt Preview / Sync Hint Badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF4F9F6))
                        .border(1.dp, Color(0xFFDCEBE3), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                        contentDescription = null,
                        tint = Brand500,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Digunakan otomatis pada kop struk & faktur cetak",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Brand500,
                    )
                }

                // Fields
                val isStoreNameError = uiState.error?.contains("Nama toko", ignoreCase = true) == true
                if (wide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        StoreInputField(
                            label = "Nama Toko",
                            value = uiState.storeName,
                            onValueChange = onStoreNameChanged,
                            placeholder = "Masukkan nama toko",
                            leadingIcon = Icons.Outlined.Storefront,
                            isError = isStoreNameError,
                            supportingText = if (isStoreNameError) "Nama toko tidak boleh kosong" else null,
                            modifier = Modifier.weight(1f),
                        )
                        StoreInputField(
                            label = "Nomor Telepon",
                            value = uiState.phone,
                            onValueChange = onPhoneChanged,
                            placeholder = "Nomor WhatsApp / Kasir",
                            leadingIcon = Icons.Outlined.Phone,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    StoreInputField(
                        label = "Nama Toko",
                        value = uiState.storeName,
                        onValueChange = onStoreNameChanged,
                        placeholder = "Masukkan nama toko",
                        leadingIcon = Icons.Outlined.Storefront,
                        isError = isStoreNameError,
                        supportingText = if (isStoreNameError) "Nama toko tidak boleh kosong" else null,
                    )
                    StoreInputField(
                        label = "Nomor Telepon",
                        value = uiState.phone,
                        onValueChange = onPhoneChanged,
                        placeholder = "Nomor WhatsApp / Kasir",
                        leadingIcon = Icons.Outlined.Phone,
                    )
                }

                StoreInputField(
                    label = "Alamat Toko",
                    value = uiState.address,
                    onValueChange = onAddressChanged,
                    placeholder = "Alamat lengkap toko",
                    leadingIcon = Icons.Outlined.Place,
                    minLines = 2,
                )

                // Submit Action CTA
                Button(
                    onClick = onSave,
                    enabled = !uiState.isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Brand500,
                        contentColor = Color.White,
                        disabledContainerColor = Slate200,
                        disabledContentColor = Slate400,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("settings-store-save"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isSaving) "Menyimpan..." else "Simpan Perubahan",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun StoreInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isError) Color(0xFFDC2626) else Slate900,
        )

        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    fontSize = 14.sp,
                    color = Slate400,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (isError) Color(0xFFDC2626) else Slate400,
                    modifier = Modifier.size(18.dp),
                )
            },
            isError = isError,
            supportingText = supportingText?.let { text ->
                {
                    Text(
                        text = text,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFDC2626),
                    )
                }
            },
            minLines = minLines,
            singleLine = minLines == 1,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = if (isError) Color(0xFFFFF5F5) else Color.White,
                unfocusedContainerColor = if (isError) Color(0xFFFFF5F5) else Color(0xFFF8FAF9),
                disabledContainerColor = Color(0xFFF8FAF9),
                errorContainerColor = Color(0xFFFFF5F5),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                focusedTextColor = Slate900,
                unfocusedTextColor = Slate900,
                errorTextColor = Slate900,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    if (isError) Color(0xFFEF4444) else Slate200,
                    RoundedCornerShape(12.dp),
                ),
        )
    }
}

@Composable
private fun ReceiptHeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brand100),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                    contentDescription = null,
                    tint = Brand500,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Tampilan Struk Kasir",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                    )
                    Surface(
                        color = Brand100,
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            text = "Nota Thermal",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Brand500,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    text = "Atur teks informasi toko dan pesan yang tercetak pada nota penjualan.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Slate500,
                )
            }
        }
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings-section-receipt"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ReceiptHeaderCard()

        if (wide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1.1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ReceiptEditorCard(
                        uiState = uiState,
                        onReceiptHeaderChanged = onReceiptHeaderChanged,
                        onReceiptFooterChanged = onReceiptFooterChanged,
                    )
                    ReceiptSaveActionSection(
                        isSaving = uiState.isSaving,
                        onSave = onSave,
                    )
                }

                ReceiptPreview(
                    uiState = uiState,
                    modifier = Modifier.weight(0.9f),
                )
            }
        } else {
            ReceiptEditorCard(
                uiState = uiState,
                onReceiptHeaderChanged = onReceiptHeaderChanged,
                onReceiptFooterChanged = onReceiptFooterChanged,
            )
            ReceiptPreview(
                uiState = uiState,
            )
            ReceiptSaveActionSection(
                isSaving = uiState.isSaving,
                onSave = onSave,
            )
        }
    }
}

@Composable
private fun ReceiptEditorCard(
    uiState: SettingsUiState,
    onReceiptHeaderChanged: (String) -> Unit,
    onReceiptFooterChanged: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Teks atas (Kop struk)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate700,
                )
                TextField(
                    value = uiState.receiptHeader,
                    onValueChange = onReceiptHeaderChanged,
                    placeholder = {
                        Text(
                            text = "Contoh: TB Terminal",
                            fontSize = 14.sp,
                            color = Slate400,
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color(0xFFF8FAF9),
                        disabledContainerColor = Color(0xFFF8FAF9),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp)),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Teks bawah (Pesan penutup)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate700,
                )
                TextField(
                    value = uiState.receiptFooter,
                    onValueChange = onReceiptFooterChanged,
                    placeholder = {
                        Text(
                            text = "Contoh: Terima kasih atas kunjungan Anda",
                            fontSize = 14.sp,
                            color = Slate400,
                        )
                    },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color(0xFFF8FAF9),
                        disabledContainerColor = Color(0xFFF8FAF9),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedTextColor = Slate900,
                        unfocusedTextColor = Slate900,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp)),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Print,
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    text = "Lebar kertas struk: ${uiState.printerSize} (dapat disesuaikan di tab Perangkat)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = Slate500,
                )
            }
        }
    }
}

@Composable
private fun ReceiptPreview(
    uiState: SettingsUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.testTag("settings-receipt-preview"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Pratinjau Hasil Cetak",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate700,
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Brand100,
            ) {
                Text(
                    text = "KERTAS ${if (uiState.printerSize == "80mm") "80MM" else "58MM"}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Brand500,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF1F5F9))
                .border(1.dp, Slate200.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 2.dp, bottomEnd = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Slate200.copy(alpha = 0.8f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Header Nota
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = (if (uiState.receiptHeader.isNotBlank()) uiState.receiptHeader else uiState.storeName.ifBlank { "TB TERMINAL" }).uppercase(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            letterSpacing = 0.5.sp,
                        )
                        Text(
                            text = uiState.address.ifBlank { "Jl. Raya Bangunan No. 88" },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            color = Slate500,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = uiState.phone.ifBlank { "0812-3456-7890" }.let {
                                if (it.startsWith("Telp")) it else "Telp: $it"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            color = Slate400,
                            textAlign = TextAlign.Center,
                        )
                    }

                    DashedDivider(color = Slate300)

                    // Info Transaksi
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "24/10/2026 14:32",
                            fontSize = 10.sp,
                            color = Slate500,
                        )
                        Text(
                            text = "#TRX-0982",
                            fontSize = 10.sp,
                            color = Slate500,
                        )
                    }

                    // Baris Item Transaksi
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        ReceiptRowItem("1x Semen Merdeka", "Rp50.000")
                        ReceiptRowItem("2x Paku 5cm", "Rp20.000")
                    }

                    DashedDivider(color = Slate300)

                    // Rincian Pembayaran
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        ReceiptTotalItem("TOTAL", "Rp70.000", isBold = true)
                        ReceiptTotalItem("TUNAI", "Rp100.000", isBold = false)
                        ReceiptTotalItem("KEMBALI", "Rp30.000", isBold = false)
                    }

                    DashedDivider(color = Slate300)

                    // Pesan Penutup / Footer Nota
                    Text(
                        text = "\"${uiState.receiptFooter.ifBlank { "Terima kasih atas kunjungan Anda" }}\"",
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color = Slate500,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        lineHeight = 15.sp,
                    )

                    Spacer(Modifier.height(4.dp))
                    DashedDivider(color = Slate300, dashWidth = 2.dp, gapWidth = 2.dp)
                }
            }
        }
    }
}

@Composable
private fun ReceiptRowItem(name: String, price: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            fontSize = 11.sp,
            color = Slate800,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = price,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Slate900,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun ReceiptTotalItem(label: String, value: String, isBold: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = if (isBold) 11.sp else 10.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) Slate900 else Slate600,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = value,
            fontSize = if (isBold) 11.sp else 10.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) Slate900 else Slate600,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun DashedDivider(
    color: Color = Slate300,
    dashWidth: Dp = 4.dp,
    gapWidth: Dp = 4.dp,
    strokeWidth: Dp = 1.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(strokeWidth),
    ) {
        val dashPx = dashWidth.toPx()
        val gapPx = gapWidth.toPx()
        val strokePx = strokeWidth.toPx()
        var currentX = 0f
        while (currentX < size.width) {
            drawLine(
                color = color,
                start = Offset(currentX, size.height / 2),
                end = Offset(
                    (currentX + dashPx).coerceAtMost(size.width),
                    size.height / 2,
                ),
                strokeWidth = strokePx,
            )
            currentX += dashPx + gapPx
        }
    }
}

@Composable
private fun ReceiptSaveActionSection(
    isSaving: Boolean,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onSave,
            enabled = !isSaving,
            colors = ButtonDefaults.buttonColors(
                containerColor = Brand500,
                contentColor = Color.White,
                disabledContainerColor = Slate200,
                disabledContentColor = Slate400,
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("settings-receipt-save"),
        ) {
            Icon(
                imageVector = Icons.Outlined.Save,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isSaving) "Menyimpan..." else "Simpan tampilan struk",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
        Text(
            text = "Perubahan teks struk otomatis diterapkan pada printer Bluetooth saat cetak nota.",
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Slate400,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            lineHeight = 16.sp,
        )
    }
}

enum class DeviceReceiptTab {
    DEVICE,
    RECEIPT,
}

@Composable
private fun DeviceAndReceiptPanel(
    role: String,
    initialTab: DeviceReceiptTab,
    uiState: SettingsUiState,
    wide: Boolean,
    onReceiptHeaderChanged: (String) -> Unit,
    onReceiptFooterChanged: (String) -> Unit,
    onPrinterSizeChanged: (String) -> Unit,
    onCashToleranceChanged: (String) -> Unit,
    onAutoLockMinutesChanged: (String) -> Unit,
    onAutoPrintReceiptChanged: (Boolean) -> Unit,
    onPrinterConnectionTypeChanged: (PrinterConnectionType) -> Unit,
    onPrinterDeviceSelected: (PrinterDeviceItem) -> Unit,
    onAutoCutPaperChanged: (Boolean) -> Unit,
    onOpenCashDrawerChanged: (Boolean) -> Unit,
    onScanPrinters: () -> Unit,
    onTestPrint: () -> Unit,
    onTestCashDrawer: () -> Unit,
    onSelectPrinter: () -> Unit,
    onSaveStoreSettings: () -> Unit,
    onSaveLocalPreferences: () -> Unit,
) {
    val canEditReceipt = AppAccessPolicy.can(role, AppCapability.STORE_SETTINGS)
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (canEditReceipt) {
            DeviceReceiptSegmentedTab(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
            )
        }

        AnimatedContent(
            targetState = if (canEditReceipt) selectedTab else DeviceReceiptTab.DEVICE,
            transitionSpec = {
                if (targetState == DeviceReceiptTab.RECEIPT) {
                    (fadeIn(animationSpec = tween(220)) + slideInHorizontally(animationSpec = tween(220)) { width -> width / 12 })
                        .togetherWith(fadeOut(animationSpec = tween(150)) + slideOutHorizontally(animationSpec = tween(150)) { width -> -width / 12 })
                } else {
                    (fadeIn(animationSpec = tween(220)) + slideInHorizontally(animationSpec = tween(220)) { width -> -width / 12 })
                        .togetherWith(fadeOut(animationSpec = tween(150)) + slideOutHorizontally(animationSpec = tween(150)) { width -> width / 12 })
                }
            },
            label = "DeviceReceiptTabTransition",
        ) { currentTab ->
            if (currentTab == DeviceReceiptTab.RECEIPT && canEditReceipt) {
                ReceiptPanel(
                    uiState = uiState,
                    wide = wide,
                    onReceiptHeaderChanged = onReceiptHeaderChanged,
                    onReceiptFooterChanged = onReceiptFooterChanged,
                    onSave = onSaveStoreSettings,
                )
            } else {
                DevicePanel(
                    uiState = uiState,
                    wide = wide,
                    onPrinterSizeChanged = onPrinterSizeChanged,
                    onCashToleranceChanged = onCashToleranceChanged,
                    onAutoLockMinutesChanged = onAutoLockMinutesChanged,
                    onAutoPrintReceiptChanged = onAutoPrintReceiptChanged,
                    onPrinterConnectionTypeChanged = onPrinterConnectionTypeChanged,
                    onPrinterDeviceSelected = onPrinterDeviceSelected,
                    onAutoCutPaperChanged = onAutoCutPaperChanged,
                    onOpenCashDrawerChanged = onOpenCashDrawerChanged,
                    onScanPrinters = onScanPrinters,
                    onTestPrint = onTestPrint,
                    onTestCashDrawer = onTestCashDrawer,
                    onSelectPrinter = onSelectPrinter,
                    onSave = onSaveLocalPreferences,
                )
            }
        }
    }
}

@Composable
private fun DeviceReceiptSegmentedTab(
    selectedTab: DeviceReceiptTab,
    onTabSelected: (DeviceReceiptTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, Slate200.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val isDevice = selectedTab == DeviceReceiptTab.DEVICE
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .then(
                        if (isDevice) {
                            Modifier
                                .shadow(elevation = 1.dp, shape = RoundedCornerShape(9.dp))
                                .background(Color.White)
                        } else {
                            Modifier.background(Color.Transparent)
                        }
                    )
                    .clickable { onTabSelected(DeviceReceiptTab.DEVICE) }
                    .testTag("tab-device"),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Print,
                        contentDescription = null,
                        tint = if (isDevice) Brand500 else Slate400,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Perangkat",
                        fontSize = 13.sp,
                        fontWeight = if (isDevice) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isDevice) Slate900 else Slate500,
                    )
                }
            }

            val isReceipt = selectedTab == DeviceReceiptTab.RECEIPT
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .then(
                        if (isReceipt) {
                            Modifier
                                .shadow(elevation = 1.dp, shape = RoundedCornerShape(9.dp))
                                .background(Color.White)
                        } else {
                            Modifier.background(Color.Transparent)
                        }
                    )
                    .clickable { onTabSelected(DeviceReceiptTab.RECEIPT) }
                    .testTag("tab-receipt"),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                        contentDescription = null,
                        tint = if (isReceipt) Brand500 else Slate400,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Tampilan Struk",
                        fontSize = 13.sp,
                        fontWeight = if (isReceipt) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isReceipt) Slate900 else Slate500,
                    )
                }
            }
        }
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
    onPrinterConnectionTypeChanged: (PrinterConnectionType) -> Unit,
    onPrinterDeviceSelected: (PrinterDeviceItem) -> Unit,
    onAutoCutPaperChanged: (Boolean) -> Unit,
    onOpenCashDrawerChanged: (Boolean) -> Unit,
    onScanPrinters: () -> Unit,
    onTestPrint: () -> Unit,
    onTestCashDrawer: () -> Unit,
    onSelectPrinter: () -> Unit,
    onSave: () -> Unit,
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            onScanPrinters()
        }
    }

    val checkAndScanBluetooth: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasConnect = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            val hasScan = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED

            if (hasConnect && hasScan) {
                onScanPrinters()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                    )
                )
            }
        } else {
            onScanPrinters()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings-section-device"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DeviceHeaderCard()

        PrinterConnectionCard(
            uiState = uiState,
            onPrinterConnectionTypeChanged = onPrinterConnectionTypeChanged,
            onPrinterDeviceSelected = onPrinterDeviceSelected,
            onScanBluetooth = checkAndScanBluetooth,
            onScanUsb = onScanPrinters,
            onSelectSystemPrinter = onSelectPrinter,
            onPrinterSizeChanged = onPrinterSizeChanged,
        )

        HardwareOptionsCard(
            autoCutPaper = uiState.autoCutPaper,
            openCashDrawer = uiState.openCashDrawer,
            autoPrintReceipt = uiState.autoPrintReceipt,
            onAutoCutPaperChanged = onAutoCutPaperChanged,
            onOpenCashDrawerChanged = onOpenCashDrawerChanged,
            onAutoPrintReceiptChanged = onAutoPrintReceiptChanged,
        )

        HardwareTestCard(
            isPrintingTest = uiState.isPrintingTest,
            onTestPrint = onTestPrint,
            onTestCashDrawer = onTestCashDrawer,
        )

        val cashToleranceError = if (uiState.error?.contains("Toleransi", ignoreCase = true) == true) "Harus berupa angka positif" else null
        val autoLockError = if (uiState.error?.contains("kunci", ignoreCase = true) == true) "Harus berupa angka menit" else null

        TerminalPreferencesCard(
            cashTolerance = uiState.cashTolerance,
            autoLockMinutes = uiState.autoLockMinutes,
            cashToleranceError = cashToleranceError,
            autoLockError = autoLockError,
            onCashToleranceChanged = onCashToleranceChanged,
            onAutoLockMinutesChanged = onAutoLockMinutesChanged,
        )

        DeviceSaveButton(
            isSaving = uiState.isSaving,
            onSave = onSave,
        )
    }
}

@Composable
private fun DeviceHeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brand100),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Print,
                    contentDescription = null,
                    tint = Brand500,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Perangkat Kasir",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                    )
                    Surface(
                        color = Brand100,
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            text = "ESC/POS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Brand500,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    text = "Preferensi printer thermal dan terminal kasir.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Slate500,
                )
            }
        }
    }
}

@Composable
private fun PrinterConnectionCard(
    uiState: SettingsUiState,
    onPrinterConnectionTypeChanged: (PrinterConnectionType) -> Unit,
    onPrinterDeviceSelected: (PrinterDeviceItem) -> Unit,
    onScanBluetooth: () -> Unit,
    onScanUsb: () -> Unit,
    onSelectSystemPrinter: () -> Unit,
    onPrinterSizeChanged: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Tipe Koneksi
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "TIPE KONEKSI PRINTER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate700,
                    letterSpacing = 0.5.sp,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ConnectionTypeOption(
                        label = "Bluetooth",
                        isSelected = uiState.printerConnectionType == PrinterConnectionType.BLUETOOTH,
                        onClick = { onPrinterConnectionTypeChanged(PrinterConnectionType.BLUETOOTH) },
                        modifier = Modifier.weight(1f),
                    )
                    ConnectionTypeOption(
                        label = "USB OTG",
                        isSelected = uiState.printerConnectionType == PrinterConnectionType.USB,
                        onClick = { onPrinterConnectionTypeChanged(PrinterConnectionType.USB) },
                        modifier = Modifier.weight(1f),
                    )
                    ConnectionTypeOption(
                        label = "Spooler OS",
                        isSelected = uiState.printerConnectionType == PrinterConnectionType.SYSTEM,
                        onClick = { onPrinterConnectionTypeChanged(PrinterConnectionType.SYSTEM) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Selected Device Status Box
            when (uiState.printerConnectionType) {
                PrinterConnectionType.BLUETOOTH -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceMuted)
                            .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "PRINTER BLUETOOTH AKTIF",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Brand500,
                                    letterSpacing = 0.5.sp,
                                )
                                Text(
                                    text = uiState.printerBluetoothName.ifBlank { "Belum ada printer dipilih" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (uiState.printerBluetoothName.isNotBlank()) Slate900 else Slate600,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (uiState.printerBluetoothAddress.isNotBlank()) {
                                    Text(
                                        text = uiState.printerBluetoothAddress,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Slate400,
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Brand100)
                                    .border(1.dp, Brand500.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .clickable(enabled = !uiState.isScanningPrinters, onClick = onScanBluetooth)
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    if (uiState.isScanningPrinters) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(13.dp),
                                            strokeWidth = 2.dp,
                                            color = Brand500,
                                        )
                                        Text(
                                            text = "Memindai...",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Brand500,
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.Search,
                                            contentDescription = null,
                                            tint = Brand500,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Text(
                                            text = "Pindai",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Brand500,
                                        )
                                    }
                                }
                            }
                        }

                        if (uiState.availablePrinters.isNotEmpty()) {
                            HorizontalDivider(color = Slate200, thickness = 1.dp)
                            Text(
                                text = "Pilih printer Bluetooth yang tersedia:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate600,
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                uiState.availablePrinters.forEach { device ->
                                    val isSelected = device.address == uiState.printerBluetoothAddress
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) Brand100 else Color.White)
                                            .border(
                                                1.dp,
                                                if (isSelected) Brand500 else Slate200,
                                                RoundedCornerShape(10.dp),
                                            )
                                            .clickable { onPrinterDeviceSelected(device) }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = device.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Slate900,
                                            )
                                            Text(
                                                text = device.address,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = Slate500,
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = "Terpilih",
                                                tint = Brand500,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                PrinterConnectionType.USB -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceMuted)
                            .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "PRINTER USB OTG",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Brand500,
                                    letterSpacing = 0.5.sp,
                                )
                                Text(
                                    text = if (uiState.availablePrinters.isNotEmpty()) {
                                        uiState.availablePrinters.first().name
                                    } else {
                                        "Koneksikan kabel adaptor OTG USB"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate800,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "Hubungkan kabel data printer thermal kasir via adaptor OTG USB.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Slate500,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Brand100)
                                    .border(1.dp, Brand500.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .clickable(enabled = !uiState.isScanningPrinters, onClick = onScanUsb)
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    if (uiState.isScanningPrinters) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(13.dp),
                                            strokeWidth = 2.dp,
                                            color = Brand500,
                                        )
                                        Text(
                                            text = "Mendeteksi...",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Brand500,
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Outlined.Search,
                                            contentDescription = null,
                                            tint = Brand500,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Text(
                                            text = "Deteksi USB",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Brand500,
                                        )
                                    }
                                }
                            }
                        }

                        if (uiState.availablePrinters.isNotEmpty()) {
                            HorizontalDivider(color = Slate200, thickness = 1.dp)
                            Text(
                                text = "Daftar USB printer terdeteksi:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate600,
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                uiState.availablePrinters.forEach { device ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.White)
                                            .border(1.dp, Slate200, RoundedCornerShape(10.dp))
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column {
                                            Text(
                                                text = device.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Slate900,
                                            )
                                            Text(
                                                text = device.address,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Normal,
                                                color = Slate500,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                PrinterConnectionType.SYSTEM -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceMuted)
                            .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "ANDROID PRINT SPOOLER",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Brand500,
                                    letterSpacing = 0.5.sp,
                                )
                                Text(
                                    text = uiState.selectedPrinterName.ifBlank { "Android Print Service" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "Gunakan dialog cetak sistem bawaan Android.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Slate500,
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Brand100)
                                    .border(1.dp, Brand500.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .clickable(onClick = onSelectSystemPrinter)
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Print,
                                        contentDescription = null,
                                        tint = Brand500,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = "Pilih",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Brand500,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Ukuran Kertas Thermal
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HorizontalDivider(color = Slate100, thickness = 1.dp)
                Text(
                    text = "UKURAN KERTAS THERMAL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate700,
                    letterSpacing = 0.5.sp,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PaperSizeOption(
                        label = "58 mm (Standar)",
                        isSelected = uiState.printerSize == "58mm",
                        onClick = { onPrinterSizeChanged("58mm") },
                        modifier = Modifier.weight(1f),
                    )
                    PaperSizeOption(
                        label = "80 mm (Lebar)",
                        isSelected = uiState.printerSize == "80mm",
                        onClick = { onPrinterSizeChanged("80mm") },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionTypeOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Brand100 else SurfaceMuted)
            .border(
                1.dp,
                if (isSelected) Brand500 else Slate200,
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = Brand500,
                    modifier = Modifier.size(13.dp),
                )
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) Brand500 else Slate600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PaperSizeOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Brand100 else SurfaceMuted)
            .border(
                1.dp,
                if (isSelected) Brand500 else Slate200,
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = Brand500,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) Brand500 else Slate600,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun HardwareOptionsCard(
    autoCutPaper: Boolean,
    openCashDrawer: Boolean,
    autoPrintReceipt: Boolean,
    onAutoCutPaperChanged: (Boolean) -> Unit,
    onOpenCashDrawerChanged: (Boolean) -> Unit,
    onAutoPrintReceiptChanged: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "OPSI PERANGKAT KERAS KASIR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate700,
                letterSpacing = 0.5.sp,
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                HardwareSwitchItem(
                    title = "Potong kertas otomatis (Auto-cutter)",
                    subtitle = "Kirim sinyal potong kertas ESC/POS setelah struk selesai dicetak.",
                    checked = autoCutPaper,
                    onCheckedChange = onAutoCutPaperChanged,
                )
                HorizontalDivider(color = Slate100, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
                HardwareSwitchItem(
                    title = "Buka laci kasir otomatis (Cash Drawer)",
                    subtitle = "Kirim sinyal kick-out pulse (ESC p) ke RJ11 laci uang saat pembayaran tunai selesai.",
                    checked = openCashDrawer,
                    onCheckedChange = onOpenCashDrawerChanged,
                )
                HorizontalDivider(color = Slate100, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))
                HardwareSwitchItem(
                    title = "Cetak struk otomatis",
                    subtitle = "Kirim instruksi cetak seketika transaksi kasir berhasil diselesaikan.",
                    checked = autoPrintReceipt,
                    onCheckedChange = onAutoPrintReceiptChanged,
                )
            }
        }
    }
}

@Composable
private fun HardwareSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate800,
                lineHeight = 18.sp,
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Slate500,
                lineHeight = 16.sp,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Brand500,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Slate300,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun HardwareTestCard(
    isPrintingTest: Boolean,
    onTestPrint: () -> Unit,
    onTestCashDrawer: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "UJI COBA PERANGKAT KERAS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate700,
                letterSpacing = 0.5.sp,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Test Print
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceMuted)
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                        .clickable(enabled = !isPrintingTest, onClick = onTestPrint)
                        .padding(vertical = 11.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (isPrintingTest) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = Brand500,
                            )
                            Text(
                                text = "Mencetak...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate700,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Print,
                                contentDescription = null,
                                tint = Brand500,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "Uji Cetak Struk",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate700,
                            )
                        }
                    }
                }

                // Test Cash Drawer
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceMuted)
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                        .clickable(onClick = onTestCashDrawer)
                        .padding(vertical = 11.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PointOfSale,
                            contentDescription = null,
                            tint = Brand500,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Uji Buka Laci",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate700,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TerminalPreferencesCard(
    cashTolerance: String,
    autoLockMinutes: String,
    cashToleranceError: String? = null,
    autoLockError: String? = null,
    onCashToleranceChanged: (String) -> Unit,
    onAutoLockMinutesChanged: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "PREFERENSI TERMINAL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate700,
                letterSpacing = 0.5.sp,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Box 1: Toleransi selisih
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (cashToleranceError != null) Color(0xFFFEF2F2) else SurfaceMuted)
                            .border(
                                1.dp,
                                if (cashToleranceError != null) Color(0xFFEF4444) else Slate200,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Toleransi selisih",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (cashToleranceError != null) Color(0xFFDC2626) else Slate500,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Rp",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (cashToleranceError != null) Color(0xFFDC2626) else Slate400,
                            )
                            BasicTextField(
                                value = cashTolerance,
                                onValueChange = onCashToleranceChanged,
                                textStyle = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900,
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (cashToleranceError != null) {
                        Text(
                            text = cashToleranceError,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFDC2626),
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }

                // Box 2: Kunci otomatis
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (autoLockError != null) Color(0xFFFEF2F2) else SurfaceMuted)
                            .border(
                                1.dp,
                                if (autoLockError != null) Color(0xFFEF4444) else Slate200,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Kunci otomatis",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (autoLockError != null) Color(0xFFDC2626) else Slate500,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            BasicTextField(
                                value = autoLockMinutes,
                                onValueChange = onAutoLockMinutesChanged,
                                textStyle = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900,
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "menit",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (autoLockError != null) Color(0xFFDC2626) else Slate400,
                            )
                        }
                    }
                    if (autoLockError != null) {
                        Text(
                            text = autoLockError,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFDC2626),
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceSaveButton(
    isSaving: Boolean,
    onSave: () -> Unit,
) {
    Button(
        onClick = onSave,
        enabled = !isSaving,
        colors = ButtonDefaults.buttonColors(
            containerColor = Brand500,
            contentColor = Color.White,
            disabledContainerColor = Brand500.copy(alpha = 0.5f),
            disabledContentColor = Color.White.copy(alpha = 0.7f),
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("settings-device-save"),
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color.White,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Menyimpan...",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Simpan pengaturan perangkat",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun settingsCardColor(): Color =
    if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surface else Color.White

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
