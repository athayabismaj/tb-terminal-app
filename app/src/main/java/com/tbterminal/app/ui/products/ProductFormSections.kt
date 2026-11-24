package com.tbterminal.app.ui.products

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.tbterminal.app.data.media.ImageSize
import com.tbterminal.app.ui.products.components.ProductPhotoThumbnail
import java.io.File

@Composable
internal fun ProductFormBody(
    uiState: ProductFormUiState,
    onInputChanged: (ProductFormInput) -> Unit,
    onPhotoPicked: (Uri, File?) -> Unit = { _, _ -> },
    onPhotoSelectionError: (String) -> Unit = {},
    onRemovePhoto: () -> Unit = {},
    onRetryPhotoUpload: () -> Unit = {},
    compact: Boolean,
) {
    when {
        uiState.isLoading -> ProductLoadingState(modifier = Modifier.height(260.dp))
        else -> ProductLoadedForm(
            uiState = uiState,
            onInputChanged = onInputChanged,
            onPhotoPicked = onPhotoPicked,
            onPhotoSelectionError = onPhotoSelectionError,
            onRemovePhoto = onRemovePhoto,
            onRetryPhotoUpload = onRetryPhotoUpload,
            compact = compact
        )
    }
}

@Composable
private fun ProductLoadedForm(
    uiState: ProductFormUiState,
    onInputChanged: (ProductFormInput) -> Unit,
    onPhotoPicked: (Uri, File?) -> Unit,
    onPhotoSelectionError: (String) -> Unit,
    onRemovePhoto: () -> Unit,
    onRetryPhotoUpload: () -> Unit,
    compact: Boolean,
) {
    val input = uiState.input

    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp)) {
        if (compact) {
            BasicInfoSection(uiState = uiState, input = input, onInputChanged = onInputChanged, compact = true)
            PhotoSection(
                uiState = uiState,
                onPhotoPicked = onPhotoPicked,
                onPhotoSelectionError = onPhotoSelectionError,
                onRemovePhoto = onRemovePhoto,
                onRetryPhotoUpload = onRetryPhotoUpload,
                compact = true
            )
            UnitSection(uiState = uiState, input = input, onInputChanged = onInputChanged, compact = true)
            PriceSection(input = input, onInputChanged = onInputChanged, compact = true)
            StockSection(input = input, onInputChanged = onInputChanged, compact = true)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    BasicInfoSection(uiState, input, onInputChanged, compact = false)
                    PhotoSection(
                        uiState = uiState,
                        onPhotoPicked = onPhotoPicked,
                        onPhotoSelectionError = onPhotoSelectionError,
                        onRemovePhoto = onRemovePhoto,
                        onRetryPhotoUpload = onRetryPhotoUpload,
                        compact = false
                    )
                    UnitSection(uiState, input, onInputChanged, compact = false)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PriceSection(input, onInputChanged, compact = false)
                    StockSection(input, onInputChanged, compact = false)
                }
            }
        }
    }
}

@Composable
internal fun PhotoSection(
    uiState: ProductFormUiState,
    onPhotoPicked: (Uri, File?) -> Unit,
    onPhotoSelectionError: (String) -> Unit,
    onRemovePhoto: () -> Unit,
    onRetryPhotoUpload: () -> Unit,
    compact: Boolean
) {
    val context = LocalContext.current
    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCameraFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    var showRemoveConfirm by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPhotoPicked(uri, null)
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { captured ->
        val uriText = pendingCameraUri
        val filePath = pendingCameraFilePath
        pendingCameraUri = null
        pendingCameraFilePath = null
        val file = filePath?.let(::File)
        if (captured && uriText != null) {
            onPhotoPicked(Uri.parse(uriText), file)
        } else {
            file?.delete()
        }
    }

    val launchCamera: () -> Unit = {
        runCatching {
            val cameraDir = File(context.cacheDir, "camera").apply { mkdirs() }
            val tempFile = File.createTempFile("product_photo_", ".jpg", cameraDir)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
            pendingCameraFilePath = tempFile.absolutePath
            pendingCameraUri = uri.toString()
            takePictureLauncher.launch(uri)
        }.onFailure { err ->
            onPhotoSelectionError(err.message ?: "Gagal membuka kamera")
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else {
            onPhotoSelectionError("Izin kamera diperlukan untuk mengambil foto produk")
        }
    }

    val activePreview = when {
        uiState.pendingPhoto != null -> uiState.pendingPhoto.previewUri
        uiState.removePhotoRequested -> null
        else -> uiState.photoUrl
    }
    val hasActivePhoto = !activePreview.isNullOrBlank()
    val isBusy = uiState.isSaving || uiState.photoStatus is ProductPhotoUploadStatus.Uploading
    val canPick = uiState.isOnline && !isBusy

    ProductFormSection(
        title = "Foto produk",
        sectionTag = "photo",
        icon = Icons.Outlined.Image,
        compact = compact
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ProductSoft)
                .border(1.dp, ProductLine.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                .testTag("product-photo-preview"),
            contentAlignment = Alignment.Center
        ) {
            ProductPhotoThumbnail(
                photoUrl = activePreview,
                imageSize = ImageSize.FULL,
                contentDescription = "Preview foto produk",
                modifier = Modifier.fillMaxSize(),
                fallback = {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = null,
                            tint = ProductMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("Belum ada foto produk", color = ProductMuted, fontSize = 13.sp)
                    }
                }
            )

            if (uiState.photoStatus is ProductPhotoUploadStatus.Uploading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                }
            }
        }

        if (!uiState.isOnline) {
            Text(
                text = "Foto bisa ditambahkan saat online",
                color = ProductMuted,
                fontSize = 12.sp,
                modifier = Modifier.testTag("product-photo-offline-hint")
            )
        }

        when (val status = uiState.photoStatus) {
            is ProductPhotoUploadStatus.Error -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ProductDanger.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = status.message,
                        color = ProductDanger,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (uiState.createdProductId != null || uiState.isEditMode) {
                        TextButton(
                            onClick = onRetryPhotoUpload,
                            enabled = !isBusy,
                            modifier = Modifier.testTag("product-photo-retry")
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Coba lagi")
                        }
                    }
                }
            }
            else -> Unit
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = canPick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("product-photo-gallery-btn")
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pilih dari galeri")
                }

                OutlinedButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            launchCamera()
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    enabled = canPick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).testTag("product-photo-camera-btn")
                ) {
                    Icon(Icons.Outlined.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ambil foto")
                }
            }

            if (hasActivePhoto) {
                TextButton(
                    onClick = {
                        if (uiState.isEditMode && uiState.pendingPhoto == null && !uiState.photoUrl.isNullOrBlank()) {
                            showRemoveConfirm = true
                        } else {
                            onRemovePhoto()
                        }
                    },
                    enabled = !isBusy && uiState.isOnline,
                    colors = ButtonDefaults.textButtonColors(contentColor = ProductDanger),
                    modifier = Modifier.testTag("product-photo-remove-btn")
                ) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Hapus foto")
                }
            }
        }
    }

    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            title = { Text("Hapus foto produk?") },
            text = { Text("Foto akan dihapus permanen saat perubahan produk disimpan.") },
            confirmButton = {
                Button(
                    onClick = {
                        showRemoveConfirm = false
                        onRemovePhoto()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ProductDanger)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun BasicInfoSection(
    uiState: ProductFormUiState,
    input: ProductFormInput,
    onInputChanged: (ProductFormInput) -> Unit,
    compact: Boolean,
) {
    ProductFormSection(title = "Informasi dasar", sectionTag = "basic", icon = Icons.Outlined.Info, compact = compact) {
        ProductFormInputField(
            label = "Nama produk *",
            value = input.name,
            placeholder = "Contoh: Semen Tiga Roda 50kg",
            onValueChange = { value -> onInputChanged(input.copy(name = value)) }
        )

        SkuField(
            modifier = Modifier.fillMaxWidth(),
            input = input,
            isEditMode = uiState.isEditMode,
            onInputChanged = onInputChanged,
        )
        CategoryField(
            modifier = Modifier.fillMaxWidth(),
            categories = uiState.categories,
            selectedCategoryId = input.categoryId,
            onSelect = { category -> onInputChanged(input.copy(categoryId = category.id)) },
        )
    }
}

@Composable
private fun UnitSection(
    uiState: ProductFormUiState,
    input: ProductFormInput,
    onInputChanged: (ProductFormInput) -> Unit,
    compact: Boolean,
) {
    ProductFormSection(
        title = "Satuan & konversi",
        sectionTag = "units",
        icon = Icons.Outlined.Straighten,
        compact = compact,
        headerAction = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Satuan kedua", color = ProductMuted, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = input.usesSecondaryUnit,
                    onCheckedChange = { enabled ->
                        onInputChanged(
                            input.copy(
                                usesSecondaryUnit = enabled,
                                secondaryUnitId = if (enabled) input.secondaryUnitId else "",
                                secondaryUnitFactor = if (enabled) input.secondaryUnitFactor else ""
                            )
                        )
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ProductPrimary
                    )
                )
            }
        }
    ) {
        ProductFormSelectField(
            modifier = Modifier.fillMaxWidth(),
            label = "Satuan utama *",
            selectedText = uiState.units.selectedUnitLabel(input.baseUnitId),
            options = uiState.units,
            optionText = { unit -> "${unit.name} (${unit.symbol})" },
            onSelect = { unit ->
                onInputChanged(
                    input.copy(
                        baseUnitId = unit.id,
                        secondaryUnitId = input.secondaryUnitId.takeUnless { it == unit.id }.orEmpty()
                    )
                )
            }
        )

        if (input.usesSecondaryUnit) {
            ConversionFields(
                modifier = Modifier.fillMaxWidth(),
                units = uiState.units,
                input = input,
                onInputChanged = onInputChanged,
                compact = compact,
            )
        }
    }
}

@Composable
private fun PriceSection(
    input: ProductFormInput,
    onInputChanged: (ProductFormInput) -> Unit,
    compact: Boolean,
) {
    ProductFormSection(title = "Harga & margin", sectionTag = "prices", icon = Icons.Outlined.Payments, compact = compact) {
        ProductFormInputField(
            modifier = Modifier.fillMaxWidth(),
            label = "Harga beli (modal)",
            value = input.priceBuy,
            placeholder = "0",
            prefix = "Rp",
            isNumber = true,
            onValueChange = { value -> onInputChanged(input.copy(priceBuy = value.numericInput())) },
        )
        PriceInputWithMargin(
            label = "Harga jual retail",
            value = input.priceRetail,
            margin = input.priceRetail.marginText(input.priceBuy),
            onValueChange = { value -> onInputChanged(input.copy(priceRetail = value.numericInput())) },
        )
        PriceInputWithMargin(
            label = "Harga jual kontraktor",
            value = input.priceContractor,
            margin = input.priceContractor.marginText(input.priceBuy),
            onValueChange = { value -> onInputChanged(input.copy(priceContractor = value.numericInput())) },
        )
    }
}

@Composable
private fun StockSection(
    input: ProductFormInput,
    onInputChanged: (ProductFormInput) -> Unit,
    compact: Boolean,
) {
    ProductFormSection(title = "Stok", sectionTag = "stock", icon = Icons.Outlined.Inventory2, compact = compact) {
        ReadOnlyInfoField(
            modifier = Modifier.fillMaxWidth(),
            label = "Stok awal",
            value = "0",
            helper = "Dicatat melalui Barang Masuk atau Stok Opname.",
        )
        ProductFormInputField(
            modifier = Modifier.fillMaxWidth(),
            label = "Stok minimum",
            value = input.minStock,
            placeholder = "0",
            isNumber = true,
            onValueChange = { value -> onInputChanged(input.copy(minStock = value.numericInput())) },
        )
    }
}

@Composable
internal fun ProductFormSection(
    title: String,
    sectionTag: String,
    icon: ImageVector,
    compact: Boolean,
    headerAction: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("product-form-section-$sectionTag"),
        colors = CardDefaults.cardColors(containerColor = ProductSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ProductLine.copy(alpha = 0.72f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(if (compact) 16.dp else 20.dp)) {
            ProductFormSectionHeader(title = title, icon = icon, headerAction = headerAction, compact = compact)
            Column(
                modifier = Modifier.padding(top = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun ProductFormSectionHeader(
    title: String,
    icon: ImageVector,
    headerAction: @Composable (() -> Unit)?,
    compact: Boolean,
) {
    val titleContent: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(ProductPrimary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = ProductPrimaryDark, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(title, color = ProductText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    if (compact && headerAction != null) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            titleContent()
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                headerAction()
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            titleContent()
            headerAction?.invoke()
        }
    }
}
