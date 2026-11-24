package com.tbterminal.app.ui.dashboard.owner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tbterminal.app.ui.auth.AlertType
import com.tbterminal.app.ui.auth.AppAlert
import com.tbterminal.app.ui.auth.RingsPattern
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator
import com.tbterminal.app.ui.suppliers.SupplierUiState
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.BrandSoftText
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.SurfaceWhite
import com.tbterminal.app.ui.theme.TbterminalappTheme

private val OWNER_TAMBAH_SUPPLIER_TABLET_BREAKPOINT = 840.dp
private val SupplierTabletCheckColor = Color(0xFFA9DCC4)

internal enum class TambahSupplierServerTarget {
    NONE,
    NAME,
    PHONE,
    PAYMENT_TERM,
    NETWORK_TOAST,
    GENERIC_TOAST
}

internal fun validateOwnerSupplierName(rawName: String): String? {
    val trimmed = rawName.trim()
    return when {
        trimmed.isEmpty() -> "Nama supplier wajib diisi."
        trimmed.length < 2 -> "Nama supplier minimal 2 karakter."
        trimmed.length > 120 -> "Nama supplier maksimal 120 karakter."
        else -> null
    }
}

internal fun validateOwnerSupplierPhone(rawPhone: String): String? {
    val trimmed = rawPhone.trim()
    if (trimmed.isEmpty()) return null
    val digits = trimmed.filter(Char::isDigit)
    return if (digits.length in 7..15) null else "Nomor telepon tidak valid."
}

internal fun classifySupplierSaveError(message: String?): TambahSupplierServerTarget {
    if (message.isNullOrBlank()) return TambahSupplierServerTarget.NONE
    val lower = message.lowercase()
    val isSuccess = listOf("berhasil", "disimpan").all { it in lower }
    if (isSuccess) return TambahSupplierServerTarget.NONE
    val isNetwork = listOf("koneksi", "jaringan", "terhubung", "timeout", "network", "offline")
        .any { it in lower }
    if (isNetwork) return TambahSupplierServerTarget.NETWORK_TOAST
    val isPhone = listOf("hp", "telepon", "nomor", "phone").any { it in lower }
    if (isPhone) return TambahSupplierServerTarget.PHONE
    val isTerm = listOf("termin", "payment").any { it in lower }
    if (isTerm) return TambahSupplierServerTarget.PAYMENT_TERM
    val isName = listOf("nama", "name", "duplikat", "sudah ada", "terdaftar").any { it in lower }
    if (isName) return TambahSupplierServerTarget.NAME
    return TambahSupplierServerTarget.GENERIC_TOAST
}

@Composable
internal fun OwnerTambahSupplierRoute(
    uiState: SupplierUiState,
    onNameChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onAddressChanged: (String) -> Unit,
    onPaymentTermChanged: (String) -> Unit,
    onSave: () -> Unit,
    onSavedReset: () -> Unit,
    onCancel: () -> Unit,
    onDashboardClick: () -> Unit,
    onDismissMessage: () -> Unit
) {
    val destinationNavigator = LocalAdminDestinationNavigator.current
    val navigateBack: () -> Unit = onCancel
    val isEditMode = uiState.editingSupplier != null
    val editKey = uiState.editingSupplier?.id ?: "new"

    val initialName by rememberSaveable(editKey) { mutableStateOf(uiState.nameInput) }
    val initialPhone by rememberSaveable(editKey) { mutableStateOf(uiState.phoneInput) }
    val initialTerm by rememberSaveable(editKey) { mutableStateOf(uiState.paymentTermInput) }
    val initialAddress by rememberSaveable(editKey) { mutableStateOf(uiState.addressInput) }

    var nameErrorLocal by remember(editKey) { mutableStateOf<String?>(null) }
    var phoneErrorLocal by remember(editKey) { mutableStateOf<String?>(null) }
    var termErrorLocal by remember(editKey) { mutableStateOf<String?>(null) }
    var nameBlurValidated by remember(editKey) { mutableStateOf(false) }
    var phoneBlurValidated by remember(editKey) { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var pendingNavAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var dismissedToast by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            OwnerSupplierToastBridge.setPendingSuccess(
                if (isEditMode) "Perubahan supplier disimpan" else "Supplier ditambahkan"
            )
            onSavedReset()
            onCancel()
        }
    }

    LaunchedEffect(uiState.message) {
        dismissedToast = false
    }

    val hasChanges = if (isEditMode) {
        uiState.nameInput != initialName ||
            uiState.phoneInput != initialPhone ||
            uiState.paymentTermInput != initialTerm ||
            uiState.addressInput != initialAddress
    } else {
        uiState.nameInput.isNotEmpty() ||
            uiState.phoneInput.isNotEmpty() ||
            uiState.addressInput.isNotEmpty() ||
            (uiState.paymentTermInput.isNotEmpty() && uiState.paymentTermInput != "30")
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
        requestLeave(navigateBack)
    }

    fun handleNavSelect(destination: DashboardNavDestination) {
        val target: () -> Unit = {
            when (destination) {
                DashboardNavDestination.BERANDA ->
                    destinationNavigator?.invoke(AdminDestination.Dashboard) ?: onDashboardClick()
                DashboardNavDestination.TRANSAKSI ->
                    destinationNavigator?.invoke(AdminDestination.TransactionsHub)
                DashboardNavDestination.KEUANGAN ->
                    destinationNavigator?.invoke(AdminDestination.FinanceHub)
                DashboardNavDestination.STOK ->
                    destinationNavigator?.invoke(AdminDestination.StockHub)
                DashboardNavDestination.MENU -> navigateBack()
            }
        }
        requestLeave(target)
    }

    fun submitForm() {
        if (uiState.isSaving) return
        val nameErr = validateOwnerSupplierName(uiState.nameInput)
        val phoneErr = validateOwnerSupplierPhone(uiState.phoneInput)
        nameErrorLocal = nameErr
        phoneErrorLocal = phoneErr
        termErrorLocal = null
        if (nameErr != null || phoneErr != null) {
            return
        }
        if (uiState.paymentTermInput.isBlank()) {
            onPaymentTermChanged("0")
        }
        onSave()
    }

    val serverTarget = classifySupplierSaveError(uiState.message)
    val effectiveNameError = nameErrorLocal
        ?: if (serverTarget == TambahSupplierServerTarget.NAME) uiState.message else null
    val effectivePhoneError = phoneErrorLocal
        ?: if (serverTarget == TambahSupplierServerTarget.PHONE) uiState.message else null
    val effectiveTermError = termErrorLocal
        ?: if (serverTarget == TambahSupplierServerTarget.PAYMENT_TERM) uiState.message else null

    Box(modifier = Modifier.fillMaxSize()) {
        OwnerTambahSupplierScreen(
            name = uiState.nameInput,
            phone = uiState.phoneInput,
            paymentTermDigits = uiState.paymentTermInput,
            address = uiState.addressInput,
            nameError = effectiveNameError,
            phoneError = effectivePhoneError,
            paymentTermError = effectiveTermError,
            isSaving = uiState.isSaving,
            isEditMode = isEditMode,
            hasChanges = hasChanges,
            onNameChange = { value ->
                val clipped = value.take(120)
                onNameChanged(clipped)
                if (nameBlurValidated || nameErrorLocal != null) {
                    nameErrorLocal = validateOwnerSupplierName(clipped)
                }
            },
            onNameBlur = {
                nameBlurValidated = true
                if (uiState.nameInput.isNotEmpty() || nameErrorLocal != null) {
                    nameErrorLocal = validateOwnerSupplierName(uiState.nameInput)
                }
            },
            onPhoneChange = { value ->
                val filtered = value.filter { it.isDigit() || it in setOf('+', '-', ' ', '(', ')') }.take(20)
                onPhoneChanged(filtered)
                if (phoneBlurValidated || phoneErrorLocal != null) {
                    phoneErrorLocal = validateOwnerSupplierPhone(filtered)
                }
            },
            onPhoneBlur = {
                phoneBlurValidated = true
                phoneErrorLocal = validateOwnerSupplierPhone(uiState.phoneInput)
            },
            onPaymentTermChange = { raw ->
                val digits = raw.filter(Char::isDigit).take(4)
                val clamped = digits.toIntOrNull()?.let { value ->
                    if (value > 3650) "3650" else value.toString()
                }.orEmpty()
                termErrorLocal = null
                onPaymentTermChanged(clamped)
            },
            onAddressChange = { value ->
                onAddressChanged(value.take(500))
            },
            onBack = { requestLeave(navigateBack) },
            onSubmit = ::submitForm,
            onNavSelect = ::handleNavSelect
        )

        val showNetworkToast = serverTarget == TambahSupplierServerTarget.NETWORK_TOAST && !dismissedToast
        val showGenericToast = serverTarget == TambahSupplierServerTarget.GENERIC_TOAST && !dismissedToast
        if (showNetworkToast || showGenericToast) {
            AppAlert(
                type = AlertType.Error,
                title = if (showNetworkToast) "Tidak dapat terhubung" else "Gagal menyimpan supplier",
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
                    .testTag("tambah-supplier-error-toast")
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
                val target = pendingNavAction ?: navigateBack
                pendingNavAction = null
                onCancel()
                if (target !== onCancel) target()
            },
            title = if (isEditMode) "Buang perubahan?" else "Buang data supplier?",
            message = if (isEditMode) {
                "Perubahan yang belum disimpan akan hilang."
            } else {
                "Data yang sudah diisi akan hilang."
            },
            dialogTestTag = "discard-supplier-dialog",
            continueTestTag = "discard-supplier-continue",
            discardTestTag = "discard-supplier-confirm"
        )
    }
}

@Composable
internal fun OwnerTambahSupplierScreen(
    name: String,
    phone: String,
    paymentTermDigits: String,
    address: String,
    nameError: String? = null,
    phoneError: String? = null,
    paymentTermError: String? = null,
    addressError: String? = null,
    isSaving: Boolean = false,
    onNameChange: (String) -> Unit,
    onNameBlur: () -> Unit = {},
    onPhoneChange: (String) -> Unit,
    onPhoneBlur: () -> Unit = {},
    onPaymentTermChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false,
    hasChanges: Boolean = true,
    forceNameFocusBorderForPreview: Boolean = false
) {
    val pageTitle = if (isEditMode) "Edit supplier" else "Tambah supplier"
    val buttonLabel = if (isEditMode) "Simpan perubahan" else "Simpan supplier"
    val isSaveEnabled = name.trim().length >= 2 &&
        !isSaving &&
        (!isEditMode || hasChanges)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        val isTablet = maxWidth >= OWNER_TAMBAH_SUPPLIER_TABLET_BREAKPOINT
        if (isTablet) {
            TabletTambahSupplierLayout(
                isEditMode = isEditMode,
                pageTitle = pageTitle,
                buttonLabel = buttonLabel,
                name = name,
                phone = phone,
                paymentTermDigits = paymentTermDigits,
                address = address,
                nameError = nameError,
                phoneError = phoneError,
                paymentTermError = paymentTermError,
                addressError = addressError,
                isSaveEnabled = isSaveEnabled,
                isSaving = isSaving,
                onNameChange = onNameChange,
                onNameBlur = onNameBlur,
                onPhoneChange = onPhoneChange,
                onPhoneBlur = onPhoneBlur,
                onPaymentTermChange = onPaymentTermChange,
                onAddressChange = onAddressChange,
                onBack = onBack,
                onSubmit = onSubmit,
                onNavSelect = onNavSelect,
                forceNameFocusBorderForPreview = forceNameFocusBorderForPreview
            )
        } else {
            PhoneTambahSupplierLayout(
                pageTitle = pageTitle,
                buttonLabel = buttonLabel,
                name = name,
                phone = phone,
                paymentTermDigits = paymentTermDigits,
                address = address,
                nameError = nameError,
                phoneError = phoneError,
                paymentTermError = paymentTermError,
                addressError = addressError,
                isSaveEnabled = isSaveEnabled,
                isSaving = isSaving,
                onNameChange = onNameChange,
                onNameBlur = onNameBlur,
                onPhoneChange = onPhoneChange,
                onPhoneBlur = onPhoneBlur,
                onPaymentTermChange = onPaymentTermChange,
                onAddressChange = onAddressChange,
                onBack = onBack,
                onSubmit = onSubmit,
                forceNameFocusBorderForPreview = forceNameFocusBorderForPreview
            )
        }
    }
}

@Composable
private fun PhoneTambahSupplierLayout(
    pageTitle: String,
    buttonLabel: String,
    name: String,
    phone: String,
    paymentTermDigits: String,
    address: String,
    nameError: String?,
    phoneError: String?,
    paymentTermError: String?,
    addressError: String?,
    isSaveEnabled: Boolean,
    isSaving: Boolean,
    onNameChange: (String) -> Unit,
    onNameBlur: () -> Unit,
    onPhoneChange: (String) -> Unit,
    onPhoneBlur: () -> Unit,
    onPaymentTermChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    forceNameFocusBorderForPreview: Boolean
) {
    val focusManager = LocalFocusManager.current
    val overlapPx = with(LocalDensity.current) { 28.dp.roundToPx() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .imePadding()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val scrollViewportHeightPx = with(LocalDensity.current) { maxHeight.roundToPx() }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Layout(
                    content = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .background(Brand)
                                .clipToBounds()
                        ) {
                            RingsPattern(
                                radii = listOf(50.dp, 95.dp, 140.dp),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .wrapContentSize(align = Alignment.TopEnd, unbounded = true)
                                    .offset(x = 90.dp, y = (-90).dp)
                                    .size(300.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 20.dp, top = 56.dp, end = 20.dp)
                            ) {
                                TambahPelangganTopBar(
                                    title = pageTitle,
                                    onBack = onBack,
                                    backTestTag = "tambah-supplier-back-btn"
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                                .background(SurfaceWhite)
                                .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 24.dp)
                        ) {
                            Text(
                                text = "Data supplier",
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
                                label = "Nama supplier",
                                value = name,
                                onValueChange = onNameChange,
                                placeholder = "Contoh: PT Sumber Bangunan",
                                errorText = nameError,
                                enabled = !isSaving,
                                forceFocusedBorder = forceNameFocusBorderForPreview,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                onBlur = onNameBlur,
                                testTag = "tambah-supplier-name"
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            FormField(
                                label = "Nomor telepon",
                                optional = true,
                                value = phone,
                                onValueChange = onPhoneChange,
                                placeholder = "0812 3456 7890",
                                errorText = phoneError,
                                enabled = !isSaving,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                onBlur = onPhoneBlur,
                                testTag = "tambah-supplier-phone"
                            )

                            Text(
                                text = "Pembayaran",
                                style = ownerTextStyle(
                                    fontSize = 16.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Ink
                                ),
                                modifier = Modifier
                                    .padding(top = 32.dp, bottom = 12.dp)
                                    .cssLineBoxHeight(20.dp)
                                    .semantics { heading() }
                            )

                            FormField(
                                label = "Termin pembayaran",
                                value = paymentTermDigits,
                                onValueChange = onPaymentTermChange,
                                placeholder = "30",
                                suffixText = "hari",
                                errorText = paymentTermError,
                                enabled = !isSaving,
                                tabularNumbers = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                testTag = "tambah-supplier-payment-term"
                            )

                            Text(
                                text = "Biarkan 0 jika pembelian ke supplier selalu bayar tunai.",
                                style = ownerTextStyle(
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Muted
                                ),
                                modifier = Modifier.padding(top = 6.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            FormField(
                                label = "Alamat",
                                optional = true,
                                value = address,
                                onValueChange = onAddressChange,
                                placeholder = "Jalan, nomor, kecamatan",
                                errorText = addressError,
                                enabled = !isSaving,
                                singleLine = false,
                                fieldHeight = 96.dp,
                                multilineTopPadding = 16.dp,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Default
                                ),
                                testTag = "tambah-supplier-address"
                            )
                        }
                    }
                ) { measurables, constraints ->
                    val headerPlaceable = measurables[0].measure(
                        constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                    )
                    val minPanelHeight = (scrollViewportHeightPx - (headerPlaceable.height - overlapPx))
                        .coerceAtLeast(0)
                    val panelPlaceable = measurables[1].measure(
                        constraints.copy(minHeight = minPanelHeight, maxHeight = Constraints.Infinity)
                    )
                    val totalHeight = (headerPlaceable.height - overlapPx + panelPlaceable.height)
                        .coerceAtLeast(headerPlaceable.height)

                    layout(constraints.maxWidth, totalHeight) {
                        headerPlaceable.placeRelative(0, 0)
                        panelPlaceable.placeRelative(0, headerPlaceable.height - overlapPx)
                    }
                }
            }
        }

        StickyActionBar(
            enabled = isSaveEnabled,
            isSaving = isSaving,
            onSaveClick = onSubmit,
            buttonLabel = buttonLabel,
            buttonTestTag = "tambah-supplier-save",
            loadingTestTag = "tambah-supplier-loading"
        )
    }
}

@Composable
private fun TabletTambahSupplierLayout(
    isEditMode: Boolean,
    pageTitle: String,
    buttonLabel: String,
    name: String,
    phone: String,
    paymentTermDigits: String,
    address: String,
    nameError: String?,
    phoneError: String?,
    paymentTermError: String?,
    addressError: String?,
    isSaveEnabled: Boolean,
    isSaving: Boolean,
    onNameChange: (String) -> Unit,
    onNameBlur: () -> Unit,
    onPhoneChange: (String) -> Unit,
    onPhoneBlur: () -> Unit,
    onPaymentTermChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    forceNameFocusBorderForPreview: Boolean
) {
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current

    val heroTitle = if (isEditMode) "Edit supplier" else "Supplier baru"
    val heroSubtitle = if (isEditMode) {
        "Perbarui informasi kontak, termin bayar, atau alamat supplier."
    } else {
        "Dipakai untuk mencatat pembelian, termin bayar, dan stok masuk."
    }
    val heroChecks = if (isEditMode) {
        listOf(
            "Nama supplier wajib terisi",
            "Perubahan termin bayar langsung berlaku",
            "Data kontak terhubung ke riwayat pembelian"
        )
    } else {
        listOf(
            "Hanya nama supplier yang wajib diisi",
            "Termin pembayaran bisa diubah nanti",
            "Terhubung ke stok masuk & hutang supplier"
        )
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .imePadding()
    ) {
        OwnerTabletNavRail(
            activeDestination = DashboardNavDestination.MENU,
            onNavSelect = onNavSelect
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(400.dp)
                    .fillMaxHeight()
                    .background(Brand)
                    .clipToBounds()
            ) {
                RingsPattern(
                    radii = listOf(50.dp, 95.dp, 140.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .wrapContentSize(align = Alignment.TopEnd, unbounded = true)
                        .offset(x = 90.dp, y = (-90).dp)
                        .size(300.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 24.dp, top = 40.dp, end = 56.dp, bottom = 40.dp)
                ) {
                    TambahPelangganTopBar(
                        title = pageTitle,
                        onBack = onBack,
                        backTestTag = "tambah-supplier-back-btn"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OwnerVectorIcon(
                                imageVector = OwnerIcons.Truck,
                                tint = OnBrand,
                                size = 36.dp,
                                modifier = Modifier.offset(x = (-2).dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = heroTitle,
                                style = ownerTextStyle(
                                    fontSize = 24.sp,
                                    lineHeight = 30.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnBrand,
                                    letterSpacing = (-0.02).em
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = heroSubtitle,
                                style = ownerTextStyle(
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = BrandSoftText
                                )
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.18f))
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                heroChecks.forEach { check ->
                                    SupplierTabletHighlightCheckRow(check)
                                }
                            }
                        }
                    }
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .padding(start = 368.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp))
                    .background(SurfaceWhite)
            ) {
                val panelViewportHeightPx = with(density) { maxHeight.roundToPx() }
                val verticalPaddingPx = with(density) { 64.dp.roundToPx() }
                val minButtonGapPx = with(density) { 20.dp.roundToPx() }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 64.dp, top = 32.dp, end = 40.dp, bottom = 32.dp)
                ) {
                    Layout(
                        content = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Data supplier",
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

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    FormField(
                                        label = "Nama supplier",
                                        value = name,
                                        onValueChange = onNameChange,
                                        placeholder = "Contoh: PT Sumber Bangunan",
                                        errorText = nameError,
                                        enabled = !isSaving,
                                        forceFocusedBorder = forceNameFocusBorderForPreview,
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = KeyboardCapitalization.Words,
                                            imeAction = ImeAction.Next
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onNext = { focusManager.moveFocus(FocusDirection.Right) }
                                        ),
                                        onBlur = onNameBlur,
                                        modifier = Modifier.weight(1f),
                                        testTag = "tambah-supplier-name"
                                    )

                                    FormField(
                                        label = "Nomor telepon",
                                        optional = true,
                                        value = phone,
                                        onValueChange = onPhoneChange,
                                        placeholder = "0812 3456 7890",
                                        errorText = phoneError,
                                        enabled = !isSaving,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Phone,
                                            imeAction = ImeAction.Next
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                        ),
                                        onBlur = onPhoneBlur,
                                        modifier = Modifier.weight(1f),
                                        testTag = "tambah-supplier-phone"
                                    )
                                }

                                Text(
                                    text = "Pembayaran",
                                    style = ownerTextStyle(
                                        fontSize = 16.sp,
                                        lineHeight = 20.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Ink
                                    ),
                                    modifier = Modifier
                                        .padding(top = 28.dp, bottom = 12.dp)
                                        .cssLineBoxHeight(20.dp)
                                        .semantics { heading() }
                                )

                                FormField(
                                    label = "Termin pembayaran",
                                    value = paymentTermDigits,
                                    onValueChange = onPaymentTermChange,
                                    placeholder = "30",
                                    suffixText = "hari",
                                    errorText = paymentTermError,
                                    enabled = !isSaving,
                                    tabularNumbers = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                    ),
                                    modifier = Modifier.fillMaxWidth(0.5f),
                                    testTag = "tambah-supplier-payment-term"
                                )

                                Text(
                                    text = "Biarkan 0 jika pembelian ke supplier selalu bayar tunai.",
                                    style = ownerTextStyle(
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Muted
                                    ),
                                    modifier = Modifier.padding(top = 6.dp)
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                FormField(
                                    label = "Alamat",
                                    optional = true,
                                    value = address,
                                    onValueChange = onAddressChange,
                                    placeholder = "Jalan, nomor, kecamatan",
                                    errorText = addressError,
                                    enabled = !isSaving,
                                    singleLine = false,
                                    fieldHeight = 72.dp,
                                    multilineTopPadding = 14.dp,
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Sentences,
                                        imeAction = ImeAction.Default
                                    ),
                                    testTag = "tambah-supplier-address"
                                )
                            }

                            SaveCustomerButton(
                                enabled = isSaveEnabled,
                                isSaving = isSaving,
                                onClick = onSubmit,
                                label = buttonLabel,
                                buttonTestTag = "tambah-supplier-save",
                                loadingTestTag = "tambah-supplier-loading"
                            )
                        }
                    ) { measurables, constraints ->
                        val formPlaceable = measurables[0].measure(
                            constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                        )
                        val buttonPlaceable = measurables[1].measure(
                            constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                        )
                        val innerAvailable = (panelViewportHeightPx - verticalPaddingPx).coerceAtLeast(0)
                        val naturalTotal = formPlaceable.height + minButtonGapPx + buttonPlaceable.height
                        val totalHeight = maxOf(innerAvailable, naturalTotal)
                        val buttonY = totalHeight - buttonPlaceable.height

                        layout(constraints.maxWidth, totalHeight) {
                            formPlaceable.placeRelative(0, 0)
                            buttonPlaceable.placeRelative(0, buttonY)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SupplierTabletHighlightCheckRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OwnerVectorIcon(
            imageVector = OwnerIcons.CheckPlain,
            tint = SupplierTabletCheckColor,
            size = 18.dp
        )
        Text(
            text = text,
            style = ownerTextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                color = OnBrand
            )
        )
    }
}

@Preview(
    name = "OwnerTambahSupplierPhone",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
internal fun OwnerTambahSupplierPhonePreview() {
    TbterminalappTheme {
        OwnerTambahSupplierScreen(
            name = "",
            phone = "",
            paymentTermDigits = "30",
            address = "",
            onNameChange = {},
            onPhoneChange = {},
            onPaymentTermChange = {},
            onAddressChange = {},
            onBack = {},
            onSubmit = {},
            onNavSelect = {}
        )
    }
}

@Preview(
    name = "OwnerTambahSupplierTablet",
    showBackground = true,
    widthDp = 1024,
    heightDp = 768
)
@Composable
internal fun OwnerTambahSupplierTabletPreview() {
    TbterminalappTheme {
        OwnerTambahSupplierScreen(
            name = "",
            phone = "",
            paymentTermDigits = "30",
            address = "",
            onNameChange = {},
            onPhoneChange = {},
            onPaymentTermChange = {},
            onAddressChange = {},
            onBack = {},
            onSubmit = {},
            onNavSelect = {}
        )
    }
}
