package com.tbterminal.app.ui.dashboard.owner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.focus.FocusDirection
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
import com.tbterminal.app.data.model.ProductUnit
import com.tbterminal.app.ui.auth.AlertType
import com.tbterminal.app.ui.auth.AppAlert
import com.tbterminal.app.ui.products.ProductUnitUiState
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.OwnerDivider
import com.tbterminal.app.ui.theme.TbterminalappTheme

private const val MAX_SATUAN_NAME_LENGTH = 50
private const val MAX_SATUAN_SYMBOL_LENGTH = 20

internal fun validateOwnerSatuanName(rawName: String): String? {
    val trimmed = rawName.trim()
    return when {
        trimmed.isEmpty() -> "Nama satuan wajib diisi."
        trimmed.length > MAX_SATUAN_NAME_LENGTH -> "Nama satuan maksimal $MAX_SATUAN_NAME_LENGTH karakter."
        else -> null
    }
}

internal fun validateOwnerSatuanSymbol(rawSymbol: String): String? {
    val trimmed = rawSymbol.trim()
    return when {
        trimmed.isEmpty() -> "Singkatan satuan wajib diisi."
        trimmed.length > MAX_SATUAN_SYMBOL_LENGTH -> "Simbol satuan maksimal $MAX_SATUAN_SYMBOL_LENGTH karakter."
        else -> null
    }
}

/**
 * Route Owner untuk halaman Tambah & Edit Satuan produk, menghubungkan [ProductUnitUiState]
 * ke form Owner beserta konfirmasi buang perubahan dan hapus satuan pada mode edit.
 */
@Composable
internal fun OwnerTambahSatuanRoute(
    uiState: ProductUnitUiState,
    onNameChanged: (String) -> Unit,
    onSymbolChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: (ProductUnit) -> Unit,
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
    val editingUnit = uiState.editingUnit
    val isEditMode = editingUnit != null
    val editKey = editingUnit?.id ?: "new"

    val initialName by rememberSaveable(editKey) { mutableStateOf(uiState.nameInput) }
    val initialSymbol by rememberSaveable(editKey) { mutableStateOf(uiState.symbolInput) }

    var nameErrorLocal by remember(editKey) { mutableStateOf<String?>(null) }
    var symbolErrorLocal by remember(editKey) { mutableStateOf<String?>(null) }
    var nameBlurValidated by remember(editKey) { mutableStateOf(false) }
    var symbolBlurValidated by remember(editKey) { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingNavAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var dismissedToast by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.message) {
        dismissedToast = false
    }

    val hasChanges = if (isEditMode) {
        uiState.nameInput != initialName || uiState.symbolInput != initialSymbol
    } else {
        uiState.nameInput.isNotEmpty() || uiState.symbolInput.isNotEmpty()
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
        val nameErr = validateOwnerSatuanName(uiState.nameInput)
        val symbolErr = validateOwnerSatuanSymbol(uiState.symbolInput)
        nameErrorLocal = nameErr
        symbolErrorLocal = symbolErr
        if (nameErr != null || symbolErr != null) return
        onSave()
    }

    val serverTarget = classifyCatalogSaveError(uiState.message)
    val effectiveNameError = nameErrorLocal
        ?: if (serverTarget == CatalogSaveErrorTarget.NAME) uiState.message else null
    val effectiveSymbolError = symbolErrorLocal
        ?: if (serverTarget == CatalogSaveErrorTarget.SYMBOL) uiState.message else null

    Box(modifier = modifier.fillMaxSize()) {
        OwnerTambahSatuanScreen(
            name = uiState.nameInput,
            symbol = uiState.symbolInput,
            nameError = effectiveNameError,
            symbolError = effectiveSymbolError,
            isSaving = uiState.isSaving,
            isEditMode = isEditMode,
            hasChanges = hasChanges,
            onNameChange = { value ->
                val clipped = value.take(MAX_SATUAN_NAME_LENGTH)
                onNameChanged(clipped)
                if (nameBlurValidated || nameErrorLocal != null) {
                    nameErrorLocal = validateOwnerSatuanName(clipped)
                }
            },
            onNameBlur = {
                nameBlurValidated = true
                if (uiState.nameInput.isNotEmpty() || nameErrorLocal != null) {
                    nameErrorLocal = validateOwnerSatuanName(uiState.nameInput)
                }
            },
            onSymbolChange = { value ->
                val clipped = value.take(MAX_SATUAN_SYMBOL_LENGTH)
                onSymbolChanged(clipped)
                if (symbolBlurValidated || symbolErrorLocal != null) {
                    symbolErrorLocal = validateOwnerSatuanSymbol(clipped)
                }
            },
            onSymbolBlur = {
                symbolBlurValidated = true
                if (uiState.symbolInput.isNotEmpty() || symbolErrorLocal != null) {
                    symbolErrorLocal = validateOwnerSatuanSymbol(uiState.symbolInput)
                }
            },
            onBack = { requestLeave(onCancel) },
            onSubmit = ::submitForm,
            onDeleteClick = if (editingUnit != null) {
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
                title = if (showNetworkToast) "Tidak dapat terhubung" else "Gagal memproses satuan",
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
                    .testTag("tambah-satuan-error-toast")
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
            title = if (isEditMode) "Buang perubahan?" else "Buang data satuan?",
            message = if (isEditMode) {
                "Perubahan yang belum disimpan akan hilang."
            } else {
                "Data yang sudah diisi akan hilang."
            },
            dialogTestTag = "discard-satuan-dialog",
            continueTestTag = "discard-satuan-continue",
            discardTestTag = "discard-satuan-confirm"
        )
    }

    if (showDeleteDialog && editingUnit != null) {
        CatalogDeleteConfirmDialog(
            title = "Hapus satuan?",
            message = "Satuan \"${editingUnit.name}\" (${editingUnit.symbol}) akan dihapus permanen dari daftar satuan.",
            isBusy = uiState.isSaving,
            dialogTestTag = "delete-satuan-dialog",
            cancelTestTag = "delete-satuan-cancel",
            confirmTestTag = "delete-satuan-confirm",
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                onDelete(editingUnit)
            }
        )
    }
}

@Composable
internal fun OwnerTambahSatuanScreen(
    name: String,
    symbol: String,
    nameError: String? = null,
    symbolError: String? = null,
    isSaving: Boolean = false,
    isEditMode: Boolean = false,
    hasChanges: Boolean = true,
    onNameChange: (String) -> Unit,
    onNameBlur: () -> Unit = {},
    onSymbolChange: (String) -> Unit,
    onSymbolBlur: () -> Unit = {},
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    onNavSelect: (DashboardNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    activeNavDestination: DashboardNavDestination = DashboardNavDestination.STOK,
    forceNameFocusBorderForPreview: Boolean = false
) {
    val pageTitle = if (isEditMode) "Edit satuan" else "Tambah satuan"
    val buttonLabel = if (isEditMode) "Simpan perubahan" else "Simpan satuan"
    val isSaveEnabled = name.trim().isNotEmpty() &&
        symbol.trim().isNotEmpty() &&
        !isSaving &&
        (!isEditMode || hasChanges)

    val heroTitle = if (isEditMode) "Edit satuan" else "Satuan baru"
    val heroSubtitle = if (isEditMode) {
        "Perbarui nama, singkatan, atau hapus satuan yang tidak lagi digunakan."
    } else {
        "Digunakan sebagai takaran produk di katalog, transaksi kasir, dan nota pembelian."
    }
    val heroChecks = if (isEditMode) {
        listOf(
            "Nama dan singkatan satuan wajib terisi",
            "Perubahan langsung berlaku di katalog produk",
            "Satuan yang belum dipakai dapat dihapus"
        )
    } else {
        listOf(
            "Nama dan singkatan satuan wajib diisi",
            "Singkatan 1\u20135 huruf tampil utuh di daftar",
            "Langsung tersedia di form produk & kasir"
        )
    }

    OwnerCatalogFormScaffold(
        pageTitle = pageTitle,
        buttonLabel = buttonLabel,
        heroIcon = OwnerIcons.RulerUnits,
        heroTitle = heroTitle,
        heroSubtitle = heroSubtitle,
        heroChecks = heroChecks,
        isSaveEnabled = isSaveEnabled,
        isSaving = isSaving,
        backTestTag = "tambah-satuan-back-btn",
        saveButtonTestTag = "unit-save",
        loadingTestTag = "tambah-satuan-loading",
        onBack = onBack,
        onSubmit = onSubmit,
        onNavSelect = onNavSelect,
        modifier = modifier,
        activeNavDestination = activeNavDestination
    ) { isTablet ->
        val focusManager = LocalFocusManager.current

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Data satuan",
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

            val nameField: @Composable (Modifier) -> Unit = { fieldModifier ->
                FormField(
                    label = "Nama satuan",
                    value = name,
                    onValueChange = onNameChange,
                    placeholder = "Contoh: Kilogram",
                    errorText = nameError,
                    enabled = !isSaving,
                    forceFocusedBorder = forceNameFocusBorderForPreview,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            focusManager.moveFocus(
                                if (isTablet) FocusDirection.Right else FocusDirection.Down
                            )
                        }
                    ),
                    onBlur = onNameBlur,
                    modifier = fieldModifier,
                    testTag = "unit-name"
                )
            }

            val symbolField: @Composable (Modifier) -> Unit = { fieldModifier ->
                FormField(
                    label = "Singkatan satuan",
                    value = symbol,
                    onValueChange = onSymbolChange,
                    placeholder = "Contoh: kg",
                    helperText = "Maksimal 5 karakter agar tampil utuh di daftar (mis. kg, btg, galon).",
                    errorText = symbolError,
                    enabled = !isSaving,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (name.trim().isNotEmpty() && symbol.trim().isNotEmpty()) {
                                onSubmit()
                            }
                        }
                    ),
                    onBlur = onSymbolBlur,
                    trailingContent = if (symbol.trim().isNotEmpty()) {
                        {
                            Spacer(modifier = Modifier.width(8.dp))
                            AbbreviationBadge(symbol = symbol)
                        }
                    } else null,
                    modifier = fieldModifier,
                    testTag = "unit-symbol"
                )
            }

            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    nameField(Modifier.weight(1f))
                    symbolField(Modifier.weight(1f))
                }
            } else {
                nameField(Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(20.dp))
                symbolField(Modifier.fillMaxWidth())
            }

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
                    label = "Hapus satuan",
                    busyLabel = "Menghapus...",
                    isBusy = isSaving,
                    testTag = "unit-delete-btn",
                    onClick = onDeleteClick
                )
            }
        }
    }
}

@Preview(
    name = "OwnerTambahSatuanPhone",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
internal fun OwnerTambahSatuanPhonePreview() {
    TbterminalappTheme {
        OwnerTambahSatuanScreen(
            name = "",
            symbol = "",
            onNameChange = {},
            onSymbolChange = {},
            onBack = {},
            onSubmit = {},
            onNavSelect = {},
            forceNameFocusBorderForPreview = true
        )
    }
}

@Preview(
    name = "OwnerEditSatuanPhone",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
internal fun OwnerEditSatuanPhonePreview() {
    TbterminalappTheme {
        OwnerTambahSatuanScreen(
            name = "Kilogram",
            symbol = "kg",
            isEditMode = true,
            hasChanges = false,
            onNameChange = {},
            onSymbolChange = {},
            onBack = {},
            onSubmit = {},
            onDeleteClick = {},
            onNavSelect = {}
        )
    }
}

@Preview(
    name = "OwnerTambahSatuanTablet",
    showBackground = true,
    widthDp = 1024,
    heightDp = 768
)
@Composable
internal fun OwnerTambahSatuanTabletPreview() {
    TbterminalappTheme {
        OwnerTambahSatuanScreen(
            name = "",
            symbol = "",
            onNameChange = {},
            onSymbolChange = {},
            onBack = {},
            onSubmit = {},
            onNavSelect = {},
            forceNameFocusBorderForPreview = true
        )
    }
}

@Preview(
    name = "OwnerEditSatuanTablet",
    showBackground = true,
    widthDp = 1024,
    heightDp = 768
)
@Composable
internal fun OwnerEditSatuanTabletPreview() {
    TbterminalappTheme {
        OwnerTambahSatuanScreen(
            name = "Kilogram",
            symbol = "kg",
            isEditMode = true,
            hasChanges = false,
            onNameChange = {},
            onSymbolChange = {},
            onBack = {},
            onSubmit = {},
            onDeleteClick = {},
            onNavSelect = {}
        )
    }
}
