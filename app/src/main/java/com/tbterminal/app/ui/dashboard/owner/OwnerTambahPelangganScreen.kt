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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tbterminal.app.ui.auth.AlertType
import com.tbterminal.app.ui.auth.AppAlert
import com.tbterminal.app.ui.auth.RingsPattern
import com.tbterminal.app.ui.customers.CUSTOMER_FIELD_ADDRESS
import com.tbterminal.app.ui.customers.CUSTOMER_FIELD_CREDIT_LIMIT
import com.tbterminal.app.ui.customers.CUSTOMER_FIELD_NAME
import com.tbterminal.app.ui.customers.CUSTOMER_FIELD_PAYMENT_TERM
import com.tbterminal.app.ui.customers.CUSTOMER_FIELD_PHONE
import com.tbterminal.app.ui.customers.CustomerFormInput
import com.tbterminal.app.ui.customers.CustomerFormUiState
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.BrandSoftText
import com.tbterminal.app.ui.theme.FieldErrorColor
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.SurfaceWhite
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.math.BigDecimal

private val OWNER_TAMBAH_PELANGGAN_TABLET_BREAKPOINT = 840.dp
private val TabletCheckColor = Color(0xFFA9DCC4)

private val customerTypeOptions = listOf(
    RadioCardOption(
        value = false,
        title = "Umum",
        subtitle = "Harga eceran biasa",
        testTag = "customer-type-umum"
    ),
    RadioCardOption(
        value = true,
        title = "Kontraktor",
        subtitle = "Harga & limit kredit khusus",
        testTag = "customer-type-kontraktor"
    )
)

internal enum class TambahPelangganServerTarget {
    NONE,
    NAME,
    PHONE,
    NETWORK_TOAST,
    GENERIC_TOAST
}

internal fun classifyCustomerSaveError(message: String?): TambahPelangganServerTarget {
    if (message.isNullOrBlank()) return TambahPelangganServerTarget.NONE
    val lower = message.lowercase()
    val isNetwork = listOf("koneksi", "jaringan", "terhubung", "timeout", "network", "offline")
        .any { it in lower }
    if (isNetwork) return TambahPelangganServerTarget.NETWORK_TOAST
    val isPhone = listOf("hp", "telepon", "nomor", "phone").any { it in lower }
    if (isPhone) return TambahPelangganServerTarget.PHONE
    val isName = listOf("nama", "name", "duplikat", "sudah ada", "terdaftar").any { it in lower }
    if (isName) return TambahPelangganServerTarget.NAME
    return TambahPelangganServerTarget.GENERIC_TOAST
}

internal fun seedEditCreditDigits(raw: String): String {
    val bd = raw.trim().toBigDecimalOrNull() ?: return ""
    return if (bd > BigDecimal.ZERO) bd.toBigInteger().toString().trimStart('0') else ""
}

internal fun seedEditTermDigits(raw: String): String {
    val num = raw.filter(Char::isDigit).toIntOrNull() ?: 0
    return if (num > 0) num.coerceAtMost(3650).toString() else ""
}

internal fun seedEditPhone(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    val digits = trimmed.filter(Char::isDigit)
    return if (digits.length in 10..13 && (digits.startsWith("08") || digits.startsWith("62"))) {
        digits
    } else {
        trimmed
    }
}

@Composable
internal fun OwnerTambahPelangganRoute(
    uiState: CustomerFormUiState,
    onInputChanged: (CustomerFormInput) -> Unit,
    onSave: () -> Unit,
    onSavedReset: () -> Unit,
    onBack: () -> Unit,
    onDashboardClick: () -> Unit
) {
    val destinationNavigator = LocalAdminDestinationNavigator.current
    val isEditMode = uiState.isEditMode
    val preloaded = isEditMode && !uiState.isLoading && uiState.errorMessage == null

    var hasLoadedInitial by rememberSaveable(isEditMode) { mutableStateOf(!isEditMode || preloaded) }

    var initialIsContractor by rememberSaveable(isEditMode) {
        mutableStateOf(if (preloaded) uiState.input.isContractor else false)
    }
    var initialName by rememberSaveable(isEditMode) {
        mutableStateOf(if (preloaded) uiState.input.name else "")
    }
    var initialPhone by rememberSaveable(isEditMode) {
        mutableStateOf(if (preloaded) seedEditPhone(uiState.input.phone) else "")
    }
    var initialCreditDigits by rememberSaveable(isEditMode) {
        mutableStateOf(if (preloaded) seedEditCreditDigits(uiState.input.creditLimit) else "")
    }
    var initialTermDigits by rememberSaveable(isEditMode) {
        mutableStateOf(if (preloaded) seedEditTermDigits(uiState.input.paymentTermDays) else "")
    }
    var initialAddress by rememberSaveable(isEditMode) {
        mutableStateOf(if (preloaded) uiState.input.address else "")
    }

    var isContractor by rememberSaveable(isEditMode) { mutableStateOf(initialIsContractor) }
    var nameText by rememberSaveable(isEditMode) { mutableStateOf(initialName) }
    var phoneText by rememberSaveable(isEditMode) { mutableStateOf(initialPhone) }
    var creditDigits by rememberSaveable(isEditMode) { mutableStateOf(initialCreditDigits) }
    var termDigits by rememberSaveable(isEditMode) { mutableStateOf(initialTermDigits) }
    var addressText by rememberSaveable(isEditMode) { mutableStateOf(initialAddress) }

    LaunchedEffect(isEditMode, uiState.isLoading, uiState.errorMessage, uiState.input) {
        if (isEditMode && !hasLoadedInitial && !uiState.isLoading && uiState.errorMessage == null) {
            val loadedContractor = uiState.input.isContractor
            val loadedName = uiState.input.name
            val loadedPhone = seedEditPhone(uiState.input.phone)
            val loadedCredit = seedEditCreditDigits(uiState.input.creditLimit)
            val loadedTerm = seedEditTermDigits(uiState.input.paymentTermDays)
            val loadedAddress = uiState.input.address

            initialIsContractor = loadedContractor
            initialName = loadedName
            initialPhone = loadedPhone
            initialCreditDigits = loadedCredit
            initialTermDigits = loadedTerm
            initialAddress = loadedAddress

            isContractor = loadedContractor
            nameText = loadedName
            phoneText = loadedPhone
            creditDigits = loadedCredit
            termDigits = loadedTerm
            addressText = loadedAddress
            hasLoadedInitial = true
        }
    }

    var nameErrorLocal by rememberSaveable { mutableStateOf<String?>(null) }
    var phoneErrorLocal by rememberSaveable { mutableStateOf<String?>(null) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var dismissedToast by remember { mutableStateOf(false) }
    var pendingNavAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            OwnerCustomerToastBridge.setPendingSuccess(
                if (isEditMode) "Perubahan pelanggan disimpan" else "Pelanggan ditambahkan"
            )
            onSavedReset()
            onBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        dismissedToast = false
    }

    fun syncInput(
        contractor: Boolean = isContractor,
        name: String = nameText,
        phone: String = phoneText,
        credit: String = creditDigits,
        term: String = termDigits,
        address: String = addressText
    ) {
        onInputChanged(
            CustomerFormInput(
                name = name,
                phone = phone,
                address = address,
                isContractor = contractor,
                creditLimit = credit.ifEmpty { "0" },
                paymentTermDays = term.ifEmpty { "0" }
            )
        )
    }

    val hasUnsavedChanges = if (isEditMode) {
        hasLoadedInitial && (
            isContractor != initialIsContractor ||
                nameText != initialName ||
                phoneText != initialPhone ||
                creditDigits != initialCreditDigits ||
                termDigits != initialTermDigits ||
                addressText != initialAddress
            )
    } else {
        isContractor ||
            nameText.isNotEmpty() ||
            phoneText.isNotEmpty() ||
            creditDigits.isNotEmpty() ||
            termDigits.isNotEmpty() ||
            addressText.isNotEmpty()
    }

    fun requestLeave(target: () -> Unit) {
        if (uiState.isSaving) return
        if (hasUnsavedChanges) {
            pendingNavAction = target
            showDiscardDialog = true
        } else {
            target()
        }
    }

    BackHandler(enabled = !uiState.isSaving) {
        requestLeave(onBack)
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
                DashboardNavDestination.MENU -> onBack()
            }
        }
        requestLeave(target)
    }

    fun submitForm() {
        if (uiState.isSaving || (isEditMode && !hasLoadedInitial)) return
        val nameErr = validateOwnerCustomerName(nameText)
        val phoneErr = validateOwnerCustomerPhone(phoneText)
        nameErrorLocal = nameErr
        phoneErrorLocal = phoneErr
        if (nameErr != null || phoneErr != null) {
            return
        }
        syncInput()
        onSave()
    }

    val isLoadingInitial = isEditMode && uiState.isLoading && !hasLoadedInitial
    val initialLoadError = if (isEditMode && !hasLoadedInitial && !uiState.isLoading) {
        uiState.errorMessage
    } else {
        null
    }

    val serverTarget = if (initialLoadError == null) {
        classifyCustomerSaveError(uiState.errorMessage)
    } else {
        TambahPelangganServerTarget.NONE
    }
    val resolvedNameError = nameErrorLocal
        ?: uiState.fieldErrors[CUSTOMER_FIELD_NAME]
        ?: if (serverTarget == TambahPelangganServerTarget.NAME) uiState.errorMessage else null
    val resolvedPhoneError = phoneErrorLocal
        ?: uiState.fieldErrors[CUSTOMER_FIELD_PHONE]
        ?: if (serverTarget == TambahPelangganServerTarget.PHONE) uiState.errorMessage else null
    val resolvedCreditError = uiState.fieldErrors[CUSTOMER_FIELD_CREDIT_LIMIT]
    val resolvedTermError = uiState.fieldErrors[CUSTOMER_FIELD_PAYMENT_TERM]
    val resolvedAddressError = uiState.fieldErrors[CUSTOMER_FIELD_ADDRESS]

    Box(modifier = Modifier.fillMaxSize()) {
        OwnerTambahPelangganScreen(
            isEditMode = isEditMode,
            isLoadingInitial = isLoadingInitial,
            initialLoadError = initialLoadError,
            hasChanges = hasUnsavedChanges,
            isContractor = isContractor,
            name = nameText,
            phone = phoneText,
            creditLimitDigits = creditDigits,
            paymentTermDigits = termDigits,
            address = addressText,
            nameError = resolvedNameError,
            phoneError = resolvedPhoneError,
            creditLimitError = resolvedCreditError,
            paymentTermError = resolvedTermError,
            addressError = resolvedAddressError,
            isSaving = uiState.isSaving,
            onContractorChange = { value ->
                isContractor = value
                syncInput(contractor = value)
            },
            onNameChange = { value ->
                val clipped = value.take(120)
                nameText = clipped
                nameErrorLocal = null
                syncInput(name = clipped)
            },
            onNameBlur = {
                if (nameText.isNotEmpty()) {
                    nameErrorLocal = validateOwnerCustomerName(nameText)
                }
            },
            onPhoneChange = { value ->
                val clipped = value.take(24)
                phoneText = clipped
                phoneErrorLocal = null
                syncInput(phone = clipped)
            },
            onPhoneBlur = {
                phoneErrorLocal = validateOwnerCustomerPhone(phoneText)
            },
            onCreditLimitChange = { raw ->
                val digits = raw.filter(Char::isDigit).trimStart('0').take(12)
                creditDigits = digits
                syncInput(credit = digits)
            },
            onPaymentTermChange = { raw ->
                val digits = raw.filter(Char::isDigit).take(4)
                val clamped = digits.toIntOrNull()?.let { value ->
                    if (value > 3650) "3650" else value.toString()
                }.orEmpty()
                val normalized = if (digits.all { it == '0' } && digits.isNotEmpty()) "" else clamped
                termDigits = normalized
                syncInput(term = normalized)
            },
            onAddressChange = { value ->
                val clipped = value.take(500)
                addressText = clipped
                syncInput(address = clipped)
            },
            onBack = { requestLeave(onBack) },
            onSubmit = ::submitForm,
            onNavSelect = ::handleNavSelect
        )

        val showNetworkToast = serverTarget == TambahPelangganServerTarget.NETWORK_TOAST && !dismissedToast
        val showGenericToast = serverTarget == TambahPelangganServerTarget.GENERIC_TOAST && !dismissedToast
        if (showNetworkToast || showGenericToast) {
            AppAlert(
                type = AlertType.Error,
                title = if (showNetworkToast) "Tidak dapat terhubung" else "Gagal menyimpan pelanggan",
                message = uiState.errorMessage.orEmpty(),
                actionLabel = if (showNetworkToast) "Coba lagi" else null,
                onAction = { submitForm() },
                visible = true,
                onDismiss = { dismissedToast = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .zIndex(99f)
                    .testTag("tambah-pelanggan-error-toast")
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
                val target = pendingNavAction ?: onBack
                pendingNavAction = null
                target()
            },
            title = if (isEditMode) "Buang perubahan?" else "Buang data pelanggan?",
            message = if (isEditMode) {
                "Perubahan yang belum disimpan akan hilang."
            } else {
                "Data yang sudah diisi akan hilang."
            }
        )
    }
}

@Composable
internal fun OwnerTambahPelangganScreen(
    isContractor: Boolean,
    name: String,
    phone: String,
    creditLimitDigits: String,
    paymentTermDigits: String,
    address: String,
    nameError: String? = null,
    phoneError: String? = null,
    creditLimitError: String? = null,
    paymentTermError: String? = null,
    addressError: String? = null,
    isSaving: Boolean = false,
    onContractorChange: (Boolean) -> Unit,
    onNameChange: (String) -> Unit,
    onNameBlur: () -> Unit = {},
    onPhoneChange: (String) -> Unit,
    onPhoneBlur: () -> Unit = {},
    onCreditLimitChange: (String) -> Unit,
    onPaymentTermChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false,
    isLoadingInitial: Boolean = false,
    initialLoadError: String? = null,
    hasChanges: Boolean = true,
    forceNameFocusBorderForPreview: Boolean = false
) {
    val pageTitle = if (isEditMode) "Edit pelanggan" else "Tambah pelanggan"
    val buttonLabel = if (isEditMode) "Simpan perubahan" else "Simpan pelanggan"
    val isSaveEnabled = !isLoadingInitial &&
        initialLoadError == null &&
        name.trim().length >= 2 &&
        !isSaving &&
        (!isEditMode || hasChanges)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        val isTablet = maxWidth >= OWNER_TAMBAH_PELANGGAN_TABLET_BREAKPOINT
        if (isTablet) {
            TabletTambahPelangganLayout(
                isEditMode = isEditMode,
                pageTitle = pageTitle,
                buttonLabel = buttonLabel,
                isLoadingInitial = isLoadingInitial,
                initialLoadError = initialLoadError,
                isContractor = isContractor,
                name = name,
                phone = phone,
                creditLimitDigits = creditLimitDigits,
                paymentTermDigits = paymentTermDigits,
                address = address,
                nameError = nameError,
                phoneError = phoneError,
                creditLimitError = creditLimitError,
                paymentTermError = paymentTermError,
                addressError = addressError,
                isSaveEnabled = isSaveEnabled,
                isSaving = isSaving,
                onContractorChange = onContractorChange,
                onNameChange = onNameChange,
                onNameBlur = onNameBlur,
                onPhoneChange = onPhoneChange,
                onPhoneBlur = onPhoneBlur,
                onCreditLimitChange = onCreditLimitChange,
                onPaymentTermChange = onPaymentTermChange,
                onAddressChange = onAddressChange,
                onBack = onBack,
                onSubmit = onSubmit,
                onNavSelect = onNavSelect,
                forceNameFocusBorderForPreview = forceNameFocusBorderForPreview
            )
        } else {
            PhoneTambahPelangganLayout(
                pageTitle = pageTitle,
                buttonLabel = buttonLabel,
                isLoadingInitial = isLoadingInitial,
                initialLoadError = initialLoadError,
                isContractor = isContractor,
                name = name,
                phone = phone,
                creditLimitDigits = creditLimitDigits,
                paymentTermDigits = paymentTermDigits,
                address = address,
                nameError = nameError,
                phoneError = phoneError,
                creditLimitError = creditLimitError,
                paymentTermError = paymentTermError,
                addressError = addressError,
                isSaveEnabled = isSaveEnabled,
                isSaving = isSaving,
                onContractorChange = onContractorChange,
                onNameChange = onNameChange,
                onNameBlur = onNameBlur,
                onPhoneChange = onPhoneChange,
                onPhoneBlur = onPhoneBlur,
                onCreditLimitChange = onCreditLimitChange,
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
private fun InitialCustomerLoadErrorBlock(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
            .testTag("customer-form-load-error"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Gagal memuat data pelanggan",
            style = ownerTextStyle(
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = FieldErrorColor
            ),
            textAlign = TextAlign.Center
        )
        Text(
            text = message,
            style = ownerTextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                color = Muted
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PhoneTambahPelangganLayout(
    pageTitle: String,
    buttonLabel: String,
    isLoadingInitial: Boolean,
    initialLoadError: String?,
    isContractor: Boolean,
    name: String,
    phone: String,
    creditLimitDigits: String,
    paymentTermDigits: String,
    address: String,
    nameError: String?,
    phoneError: String?,
    creditLimitError: String?,
    paymentTermError: String?,
    addressError: String?,
    isSaveEnabled: Boolean,
    isSaving: Boolean,
    onContractorChange: (Boolean) -> Unit,
    onNameChange: (String) -> Unit,
    onNameBlur: () -> Unit,
    onPhoneChange: (String) -> Unit,
    onPhoneBlur: () -> Unit,
    onCreditLimitChange: (String) -> Unit,
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
                        // 1. Header hijau ringkas 160dp (top 56dp agar sejajar judul halaman utama)
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
                                    onBack = onBack
                                )
                            }
                        }

                        // 2. Panel putih menumpuk 28dp ke atas header hijau (sudut atas 32dp)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                                .background(SurfaceWhite)
                                .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 24.dp)
                        ) {
                            when {
                                isLoadingInitial -> CustomerFormSkeleton()
                                initialLoadError != null -> InitialCustomerLoadErrorBlock(initialLoadError)
                                else -> {
                                    Text(
                                        text = "Jenis pelanggan",
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

                                    RadioCardGroup(
                                        options = customerTypeOptions,
                                        selected = isContractor,
                                        onSelect = onContractorChange,
                                        enabled = !isSaving
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    FormField(
                                        label = "Nama pelanggan",
                                        value = name,
                                        onValueChange = onNameChange,
                                        placeholder = "Contoh: CV Perkasa Mulia",
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
                                        testTag = "tambah-pelanggan-name"
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    FormField(
                                        label = "Nomor HP",
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
                                        testTag = "tambah-pelanggan-phone"
                                    )

                                    Text(
                                        text = "Kredit",
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

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        FormField(
                                            label = "Limit kredit",
                                            value = creditLimitDigits,
                                            onValueChange = onCreditLimitChange,
                                            placeholder = "0",
                                            prefixText = "Rp",
                                            errorText = creditLimitError,
                                            enabled = !isSaving,
                                            tabularNumbers = true,
                                            visualTransformation = ThousandSeparatorVisualTransformation,
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Number,
                                                imeAction = ImeAction.Next
                                            ),
                                            keyboardActions = KeyboardActions(
                                                onNext = { focusManager.moveFocus(FocusDirection.Right) }
                                            ),
                                            modifier = Modifier.weight(1.6f),
                                            testTag = "tambah-pelanggan-credit-limit"
                                        )

                                        FormField(
                                            label = "Termin",
                                            value = paymentTermDigits,
                                            onValueChange = onPaymentTermChange,
                                            placeholder = "0",
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
                                            modifier = Modifier.weight(1f),
                                            testTag = "tambah-pelanggan-payment-term"
                                        )
                                    }

                                    Text(
                                        text = "Biarkan 0 jika pelanggan selalu bayar tunai.",
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
                                        testTag = "tambah-pelanggan-address"
                                    )
                                }
                            }
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

        // 3. Bar bawah menempel di dasar layar dan naik di atas keyboard
        StickyActionBar(
            enabled = isSaveEnabled,
            isSaving = isSaving,
            onSaveClick = onSubmit,
            buttonLabel = buttonLabel
        )
    }
}

@Composable
private fun TabletTambahPelangganLayout(
    isEditMode: Boolean,
    pageTitle: String,
    buttonLabel: String,
    isLoadingInitial: Boolean,
    initialLoadError: String?,
    isContractor: Boolean,
    name: String,
    phone: String,
    creditLimitDigits: String,
    paymentTermDigits: String,
    address: String,
    nameError: String?,
    phoneError: String?,
    creditLimitError: String?,
    paymentTermError: String?,
    addressError: String?,
    isSaveEnabled: Boolean,
    isSaving: Boolean,
    onContractorChange: (Boolean) -> Unit,
    onNameChange: (String) -> Unit,
    onNameBlur: () -> Unit,
    onPhoneChange: (String) -> Unit,
    onPhoneBlur: () -> Unit,
    onCreditLimitChange: (String) -> Unit,
    onPaymentTermChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    forceNameFocusBorderForPreview: Boolean
) {
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current

    val heroTitle = if (isEditMode) "Edit pelanggan" else "Pelanggan baru"
    val heroSubtitle = if (isEditMode) {
        "Perbarui informasi kontak, tipe pelanggan, atau batas kredit."
    } else {
        "Dipakai untuk mencatat piutang, limit kredit, dan nama di struk."
    }
    val heroChecks = if (isEditMode) {
        listOf(
            "Nama pelanggan wajib terisi",
            "Perubahan limit & termin langsung berlaku",
            "Tipe kontraktor mengatur harga khusus"
        )
    } else {
        listOf(
            "Hanya nama yang wajib diisi",
            "Limit dan termin bisa diubah nanti",
            "Kontraktor otomatis dapat harga khusus"
        )
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .imePadding()
    ) {
        // 1. Rel navigasi 88dp TETAP tampil, Menu aktif
        OwnerTabletNavRail(
            activeDestination = DashboardNavDestination.MENU,
            onNavSelect = onNavSelect
        )

        // 2 & 3. Panel hijau 400dp + Panel putih menumpuk 32dp ke kiri
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
                        onBack = onBack
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OwnerVectorIcon(
                                imageVector = OwnerIcons.CustomersEmptyGroup,
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
                                    TabletHighlightCheckRow(check)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Panel putih (menumpuk 32dp ke kiri di atas panel hijau, sudut kiri 32dp)
            BoxWithConstraints(
                modifier = Modifier
                    .padding(start = 368.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp))
                    .background(SurfaceWhite)
            ) {
                val panelViewportHeightPx = with(density) { maxHeight.roundToPx() }
                val verticalPaddingPx = with(density) { 64.dp.roundToPx() } // top 32 + bottom 32
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
                                when {
                                    isLoadingInitial -> CustomerFormSkeleton()
                                    initialLoadError != null -> InitialCustomerLoadErrorBlock(initialLoadError)
                                    else -> {
                                        Text(
                                            text = "Jenis pelanggan",
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

                                        RadioCardGroup(
                                            options = customerTypeOptions,
                                            selected = isContractor,
                                            onSelect = onContractorChange,
                                            enabled = !isSaving
                                        )

                                        Spacer(modifier = Modifier.height(28.dp))

                                        Text(
                                            text = "Data pelanggan",
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
                                                label = "Nama pelanggan",
                                                value = name,
                                                onValueChange = onNameChange,
                                                placeholder = "Contoh: CV Perkasa Mulia",
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
                                                testTag = "tambah-pelanggan-name"
                                            )

                                            FormField(
                                                label = "Nomor HP",
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
                                                testTag = "tambah-pelanggan-phone"
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(28.dp))

                                        Text(
                                            text = "Kredit",
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
                                                label = "Limit kredit",
                                                value = creditLimitDigits,
                                                onValueChange = onCreditLimitChange,
                                                placeholder = "0",
                                                prefixText = "Rp",
                                                errorText = creditLimitError,
                                                enabled = !isSaving,
                                                tabularNumbers = true,
                                                visualTransformation = ThousandSeparatorVisualTransformation,
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = KeyboardType.Number,
                                                    imeAction = ImeAction.Next
                                                ),
                                                keyboardActions = KeyboardActions(
                                                    onNext = { focusManager.moveFocus(FocusDirection.Right) }
                                                ),
                                                modifier = Modifier.weight(1.6f),
                                                testTag = "tambah-pelanggan-credit-limit"
                                            )

                                            FormField(
                                                label = "Termin",
                                                value = paymentTermDigits,
                                                onValueChange = onPaymentTermChange,
                                                placeholder = "0",
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
                                                modifier = Modifier.weight(1f),
                                                testTag = "tambah-pelanggan-payment-term"
                                            )
                                        }

                                        Text(
                                            text = "Biarkan 0 jika pelanggan selalu bayar tunai.",
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
                                            testTag = "tambah-pelanggan-address"
                                        )
                                    }
                                }
                            }

                            SaveCustomerButton(
                                enabled = isSaveEnabled,
                                isSaving = isSaving,
                                onClick = onSubmit,
                                label = buttonLabel
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
internal fun TabletHighlightCheckRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OwnerVectorIcon(
            imageVector = OwnerIcons.CheckPlain,
            tint = TabletCheckColor,
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
    name = "OwnerTambahPelangganPhone",
    showBackground = true,
    widthDp = 390,
    heightDp = 920
)
@Composable
internal fun OwnerTambahPelangganPhonePreview() {
    TbterminalappTheme {
        OwnerTambahPelangganScreen(
            isContractor = false,
            name = "",
            phone = "",
            creditLimitDigits = "",
            paymentTermDigits = "",
            address = "",
            onContractorChange = {},
            onNameChange = {},
            onPhoneChange = {},
            onCreditLimitChange = {},
            onPaymentTermChange = {},
            onAddressChange = {},
            onBack = {},
            onSubmit = {},
            onNavSelect = {},
            forceNameFocusBorderForPreview = true
        )
    }
}

@Preview(
    name = "OwnerTambahPelangganTablet",
    showBackground = true,
    widthDp = 1024,
    heightDp = 768
)
@Composable
internal fun OwnerTambahPelangganTabletPreview() {
    TbterminalappTheme {
        OwnerTambahPelangganScreen(
            isContractor = false,
            name = "",
            phone = "",
            creditLimitDigits = "",
            paymentTermDigits = "",
            address = "",
            onContractorChange = {},
            onNameChange = {},
            onPhoneChange = {},
            onCreditLimitChange = {},
            onPaymentTermChange = {},
            onAddressChange = {},
            onBack = {},
            onSubmit = {},
            onNavSelect = {},
            forceNameFocusBorderForPreview = true
        )
    }
}

@Preview(
    name = "OwnerTambahPelangganPhoneError",
    showBackground = true,
    widthDp = 390,
    heightDp = 920
)
@Composable
internal fun OwnerTambahPelangganPhoneErrorPreview() {
    TbterminalappTheme {
        OwnerTambahPelangganScreen(
            isContractor = true,
            name = "CV Sinar Jaya",
            phone = "08123",
            creditLimitDigits = "1500000",
            paymentTermDigits = "14",
            address = "Jl. Merdeka No. 12",
            phoneError = "Nomor HP tidak valid.",
            onContractorChange = {},
            onNameChange = {},
            onPhoneChange = {},
            onCreditLimitChange = {},
            onPaymentTermChange = {},
            onAddressChange = {},
            onBack = {},
            onSubmit = {},
            onNavSelect = {}
        )
    }
}

@Preview(
    name = "OwnerEditPelangganPhone",
    showBackground = true,
    widthDp = 390,
    heightDp = 920
)
@Composable
internal fun OwnerEditPelangganPhonePreview() {
    TbterminalappTheme {
        OwnerTambahPelangganScreen(
            isEditMode = true,
            hasChanges = true,
            isContractor = true,
            name = "Budi Santoso",
            phone = "081234567890",
            creditLimitDigits = "5000000",
            paymentTermDigits = "14",
            address = "Jl. Sudirman No. 45",
            onContractorChange = {},
            onNameChange = {},
            onPhoneChange = {},
            onCreditLimitChange = {},
            onPaymentTermChange = {},
            onAddressChange = {},
            onBack = {},
            onSubmit = {},
            onNavSelect = {}
        )
    }
}

@Preview(
    name = "OwnerEditPelangganTablet",
    showBackground = true,
    widthDp = 1024,
    heightDp = 768
)
@Composable
internal fun OwnerEditPelangganTabletPreview() {
    TbterminalappTheme {
        OwnerTambahPelangganScreen(
            isEditMode = true,
            hasChanges = true,
            isContractor = true,
            name = "Budi Santoso",
            phone = "081234567890",
            creditLimitDigits = "5000000",
            paymentTermDigits = "14",
            address = "Jl. Sudirman No. 45",
            onContractorChange = {},
            onNameChange = {},
            onPhoneChange = {},
            onCreditLimitChange = {},
            onPaymentTermChange = {},
            onAddressChange = {},
            onBack = {},
            onSubmit = {},
            onNavSelect = {}
        )
    }
}
