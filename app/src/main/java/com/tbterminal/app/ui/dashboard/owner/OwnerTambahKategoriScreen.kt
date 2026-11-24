package com.tbterminal.app.ui.dashboard.owner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tbterminal.app.data.model.ProductCategory
import com.tbterminal.app.ui.auth.AlertType
import com.tbterminal.app.ui.auth.AppAlert
import com.tbterminal.app.ui.products.ProductCategoryUiState
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.OwnerDivider
import com.tbterminal.app.ui.theme.TbterminalappTheme

private const val MAX_KATEGORI_NAME_LENGTH = 100

internal fun validateOwnerKategoriName(rawName: String): String? {
    val trimmed = rawName.trim()
    return when {
        trimmed.isEmpty() -> "Nama kategori wajib diisi."
        trimmed.length > MAX_KATEGORI_NAME_LENGTH -> "Nama kategori maksimal $MAX_KATEGORI_NAME_LENGTH karakter."
        else -> null
    }
}

/**
 * Route Owner untuk halaman Tambah & Edit Kategori produk, menghubungkan [ProductCategoryUiState]
 * ke form Owner beserta konfirmasi buang perubahan dan hapus kategori pada mode edit.
 */
@Composable
internal fun OwnerTambahKategoriRoute(
    uiState: ProductCategoryUiState,
    onNameChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: (ProductCategory) -> Unit,
    onCancel: () -> Unit,
    onStockClick: () -> Unit,
    onDashboardClick: () -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
    activeNavDestination: DashboardNavDestination = DashboardNavDestination.STOK
) {
    val (_, handleNavToRoot) = rememberCatalogBackHandler(
        onBack = onCancel,
        onStockClick = onStockClick,
        onDashboardClick = onDashboardClick,
        activeNavDestination = activeNavDestination
    )
    val editingCategory = uiState.editingCategory
    val isEditMode = editingCategory != null
    val editKey = editingCategory?.id ?: "new"

    val initialName by rememberSaveable(editKey) { mutableStateOf(uiState.nameInput) }

    var nameErrorLocal by remember(editKey) { mutableStateOf<String?>(null) }
    var nameBlurValidated by remember(editKey) { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingNavAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var dismissedToast by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.message) {
        dismissedToast = false
    }

    val hasChanges = if (isEditMode) {
        uiState.nameInput != initialName
    } else {
        uiState.nameInput.isNotEmpty()
    }

    fun requestLeave(target: () -> Unit) {
        if (uiState.isSaving) return
        if (hasChanges) {
            pendingNavAction = target
            showDiscardDialog = true
        } else {
            onCancel()
            if (target !== onCancel) target()
        }
    }

    BackHandler(enabled = !uiState.isSaving) {
        requestLeave(onCancel)
    }

    fun submitForm() {
        if (uiState.isSaving) return
        val nameErr = validateOwnerKategoriName(uiState.nameInput)
        nameErrorLocal = nameErr
        if (nameErr != null) return
        onSave()
    }

    val serverTarget = classifyCatalogSaveError(uiState.message)
    val effectiveNameError = nameErrorLocal
        ?: if (serverTarget == CatalogSaveErrorTarget.NAME) uiState.message else null

    Box(modifier = modifier.fillMaxSize()) {
        OwnerTambahKategoriScreen(
            name = uiState.nameInput,
            nameError = effectiveNameError,
            isSaving = uiState.isSaving,
            isEditMode = isEditMode,
            hasChanges = hasChanges,
            onNameChange = { value ->
                val clipped = value.take(MAX_KATEGORI_NAME_LENGTH)
                onNameChanged(clipped)
                if (nameBlurValidated || nameErrorLocal != null) {
                    nameErrorLocal = validateOwnerKategoriName(clipped)
                }
            },
            onNameBlur = {
                nameBlurValidated = true
                if (uiState.nameInput.isNotEmpty() || nameErrorLocal != null) {
                    nameErrorLocal = validateOwnerKategoriName(uiState.nameInput)
                }
            },
            onBack = { requestLeave(onCancel) },
            onSubmit = ::submitForm,
            onDeleteClick = if (editingCategory != null) {
                { showDeleteDialog = true }
            } else null,
            onNavSelect = { dest ->
                requestLeave {
                    if (dest == activeNavDestination) onCancel() else handleNavToRoot(dest)
                }
            },
            activeNavDestination = activeNavDestination
        )

        val showNetworkToast = serverTarget == CatalogSaveErrorTarget.NETWORK_TOAST && !dismissedToast
        val showGenericToast = serverTarget == CatalogSaveErrorTarget.GENERIC_TOAST && !dismissedToast
        if (showNetworkToast || showGenericToast) {
            AppAlert(
                type = AlertType.Error,
                title = if (showNetworkToast) "Tidak dapat terhubung" else "Gagal memproses kategori",
                message = uiState.message.orEmpty(),
                actionLabel = if (showNetworkToast) "Coba lagi" else null,
                onAction = { submitForm() },
                visible = true,
                onDismiss = {
                    dismissedToast = true
                    onDismissMessage()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .zIndex(99f)
                    .testTag("tambah-kategori-error-toast")
            )
        }
    }

    if (showDiscardDialog) {
        DiscardCustomerDialog(
            onContinueEditing = {
                showDiscardDialog = false
                pendingNavAction = null
            },
            onDiscard = {
                showDiscardDialog = false
                val target = pendingNavAction ?: onCancel
                pendingNavAction = null
                onCancel()
                if (target !== onCancel) target()
            },
            title = if (isEditMode) "Buang perubahan?" else "Buang data kategori?",
            message = if (isEditMode) {
                "Perubahan yang belum disimpan akan hilang."
            } else {
                "Data yang sudah diisi akan hilang."
            },
            dialogTestTag = "discard-kategori-dialog",
            continueTestTag = "discard-kategori-continue",
            discardTestTag = "discard-kategori-confirm"
        )
    }

    if (showDeleteDialog && editingCategory != null) {
        CatalogDeleteConfirmDialog(
            title = "Hapus kategori?",
            message = "Kategori \"${editingCategory.name}\" akan dihapus permanen dari daftar kategori.",
            isBusy = uiState.isSaving,
            dialogTestTag = "delete-kategori-dialog",
            cancelTestTag = "delete-kategori-cancel",
            confirmTestTag = "delete-kategori-confirm",
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                onDelete(editingCategory)
            }
        )
    }
}

@Composable
internal fun OwnerTambahKategoriScreen(
    name: String,
    nameError: String? = null,
    isSaving: Boolean = false,
    isEditMode: Boolean = false,
    hasChanges: Boolean = true,
    onNameChange: (String) -> Unit,
    onNameBlur: () -> Unit = {},
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    onNavSelect: (DashboardNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    activeNavDestination: DashboardNavDestination = DashboardNavDestination.STOK,
    forceNameFocusBorderForPreview: Boolean = false
) {
    val pageTitle = if (isEditMode) "Edit kategori" else "Tambah kategori"
    val buttonLabel = if (isEditMode) "Simpan perubahan" else "Simpan kategori"
    val isSaveEnabled = name.trim().isNotEmpty() &&
        !isSaving &&
        (!isEditMode || hasChanges)

    val heroTitle = if (isEditMode) "Edit kategori" else "Kategori baru"
    val heroSubtitle = if (isEditMode) {
        "Perbarui nama kategori atau hapus kategori yang tidak lagi digunakan."
    } else {
        "Digunakan untuk mengelompokkan produk di katalog, laporan stok, dan kasir."
    }
    val heroChecks = if (isEditMode) {
        listOf(
            "Nama kategori wajib terisi",
            "Perubahan langsung berlaku di katalog produk",
            "Kategori yang belum dipakai dapat dihapus"
        )
    } else {
        listOf(
            "Nama kategori wajib diisi",
            "Nama singkat memudahkan pencarian barang",
            "Langsung tersedia saat menambah produk"
        )
    }

    OwnerCatalogFormScaffold(
        pageTitle = pageTitle,
        buttonLabel = buttonLabel,
        heroIcon = OwnerIcons.CategoryShapes,
        heroTitle = heroTitle,
        heroSubtitle = heroSubtitle,
        heroChecks = heroChecks,
        isSaveEnabled = isSaveEnabled,
        isSaving = isSaving,
        backTestTag = "tambah-kategori-back-btn",
        saveButtonTestTag = "category-save",
        loadingTestTag = "tambah-kategori-loading",
        onBack = onBack,
        onSubmit = onSubmit,
        onNavSelect = onNavSelect,
        modifier = modifier,
        activeNavDestination = activeNavDestination
    ) { _ ->
        val focusManager = LocalFocusManager.current

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Data kategori",
                style = ownerTextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                ),
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .cssLineBoxHeight(20.dp)
                    .semantics { heading() }
            )

            FormField(
                label = "Nama kategori",
                value = name,
                onValueChange = onNameChange,
                placeholder = "Contoh: Cat dan Pelapis",
                helperText = "Nama yang singkat dan jelas memudahkan pencarian produk di katalog.",
                errorText = nameError,
                enabled = !isSaving,
                forceFocusedBorder = forceNameFocusBorderForPreview,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (name.trim().isNotEmpty()) {
                            onSubmit()
                        }
                    }
                ),
                onBlur = onNameBlur,
                modifier = Modifier.fillMaxWidth(),
                testTag = "category-name"
            )

            if (isEditMode && onDeleteClick != null) {
                Spacer(modifier = Modifier.height(32.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(OwnerDivider)
                )
                Spacer(modifier = Modifier.height(24.dp))
                CatalogDeleteDangerButton(
                    label = "Hapus kategori",
                    busyLabel = "Menghapus...",
                    isBusy = isSaving,
                    testTag = "category-delete-btn",
                    onClick = onDeleteClick
                )
            }
        }
    }
}

@Preview(
    name = "OwnerTambahKategoriPhone",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
internal fun OwnerTambahKategoriPhonePreview() {
    TbterminalappTheme {
        OwnerTambahKategoriScreen(
            name = "",
            onNameChange = {},
            onBack = {},
            onSubmit = {},
            onNavSelect = {},
            forceNameFocusBorderForPreview = true
        )
    }
}

@Preview(
    name = "OwnerEditKategoriPhone",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
internal fun OwnerEditKategoriPhonePreview() {
    TbterminalappTheme {
        OwnerTambahKategoriScreen(
            name = "Cat & Pelapis",
            isEditMode = true,
            hasChanges = false,
            onNameChange = {},
            onBack = {},
            onSubmit = {},
            onDeleteClick = {},
            onNavSelect = {}
        )
    }
}

@Preview(
    name = "OwnerTambahKategoriTablet",
    showBackground = true,
    widthDp = 1024,
    heightDp = 768
)
@Composable
internal fun OwnerTambahKategoriTabletPreview() {
    TbterminalappTheme {
        OwnerTambahKategoriScreen(
            name = "",
            onNameChange = {},
            onBack = {},
            onSubmit = {},
            onNavSelect = {},
            forceNameFocusBorderForPreview = true
        )
    }
}

@Preview(
    name = "OwnerEditKategoriTablet",
    showBackground = true,
    widthDp = 1024,
    heightDp = 768
)
@Composable
internal fun OwnerEditKategoriTabletPreview() {
    TbterminalappTheme {
        OwnerTambahKategoriScreen(
            name = "Cat & Pelapis",
            isEditMode = true,
            hasChanges = false,
            onNameChange = {},
            onBack = {},
            onSubmit = {},
            onDeleteClick = {},
            onNavSelect = {}
        )
    }
}
