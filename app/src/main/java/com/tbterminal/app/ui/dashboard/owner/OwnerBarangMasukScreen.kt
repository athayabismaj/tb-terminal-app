package com.tbterminal.app.ui.dashboard.owner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.ui.auth.AlertType
import com.tbterminal.app.ui.auth.AppAlert
import com.tbterminal.app.ui.auth.RingsPattern
import com.tbterminal.app.ui.components.AppDatePickerDialog
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator
import com.tbterminal.app.ui.incominggoods.IncomingGoodsUiState
import com.tbterminal.app.ui.incominggoods.IncomingGoodsViewModel
import com.tbterminal.app.ui.incominggoods.IncomingPaymentMethod
import com.tbterminal.app.ui.incominggoods.toInputText
import com.tbterminal.app.ui.suppliers.CreatedSupplierBridge
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.BrandSoftText
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.OwnerBorder
import com.tbterminal.app.ui.theme.OwnerDivider
import com.tbterminal.app.ui.theme.SurfaceWhite
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

private val OWNER_BARANG_MASUK_TABLET_BREAKPOINT = 840.dp
private val BARANG_MASUK_WIB = ZoneId.of("Asia/Jakarta")

internal object OwnerBarangMasukToastBridge {
    private var pendingMessage: String? = null

    fun setPendingSuccess(message: String) {
        pendingMessage = message
    }

    fun consumePendingSuccess(): String? {
        val value = pendingMessage
        pendingMessage = null
        return value
    }
}

@Composable
internal fun OwnerBarangMasukRoute(
    uiState: IncomingGoodsUiState,
    viewModel: IncomingGoodsViewModel,
    onAddSupplierClick: () -> Unit,
    onBack: () -> Unit,
    onDashboardClick: () -> Unit,
    onProductsClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onSalesTransactionsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val destinationNavigator = LocalAdminDestinationNavigator.current
    val today = remember { LocalDate.now(BARANG_MASUK_WIB) }

    var paymentMethod by rememberSaveable { mutableStateOf(IncomingPaymentMethod.CASH) }
    var amountPaidDigits by rememberSaveable { mutableStateOf("") }
    var hasManualAmountPaidEdit by rememberSaveable { mutableStateOf(false) }
    var dueDateIso by rememberSaveable { mutableStateOf<String?>(null) }
    var lastSyncedSupplierId by rememberSaveable { mutableStateOf<String?>(null) }

    var initialBaselineCaptured by rememberSaveable { mutableStateOf(false) }
    var initialProductId by rememberSaveable { mutableStateOf<String?>(null) }
    var initialSupplierId by rememberSaveable { mutableStateOf<String?>(null) }
    var initialBuyPriceDigits by rememberSaveable { mutableStateOf("") }

    var showProductPicker by remember { mutableStateOf(false) }
    var showSupplierPicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var pendingNavAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    val currentViewModel by rememberUpdatedState(viewModel)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                CreatedSupplierBridge.consumePendingCreated()?.let { created ->
                    currentViewModel.onSupplierCreatedExternally(created)
                    lastSyncedSupplierId = created.id
                    dueDateIso = initialDueDateForSupplier(created.paymentTermDays, today)?.toString()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.selectedProduct?.productId, uiState.selectedSupplier?.id, uiState.buyPriceInput) {
        if (!initialBaselineCaptured && (!uiState.isLoadingProducts || uiState.selectedProduct != null)) {
            initialBaselineCaptured = true
            initialProductId = uiState.selectedProduct?.productId
            initialSupplierId = uiState.selectedSupplier?.id
            initialBuyPriceDigits = uiState.buyPriceInput.filter(Char::isDigit)
        }
    }

    LaunchedEffect(uiState.selectedSupplier?.id) {
        val supplier = uiState.selectedSupplier
        if (supplier != null && supplier.id != lastSyncedSupplierId) {
            lastSyncedSupplierId = supplier.id
            dueDateIso = initialDueDateForSupplier(supplier.paymentTermDays, today)?.toString()
        }
    }

    val quantityDigits = uiState.quantityInput.filter(Char::isDigit)
    val priceDigits = uiState.buyPriceInput.filter(Char::isDigit)

    val previewCalculation = remember(uiState.selectedProduct, quantityDigits, priceDigits, amountPaidDigits) {
        calculateOwnerBarangMasuk(
            selectedProduct = uiState.selectedProduct,
            quantityDigits = quantityDigits,
            unitPriceDigits = priceDigits,
            amountPaidDigits = amountPaidDigits
        )
    }

    LaunchedEffect(previewCalculation.total, paymentMethod, hasManualAmountPaidEdit) {
        if (!hasManualAmountPaidEdit) {
            amountPaidDigits = initialAmountPaidDigitsForMethod(paymentMethod, previewCalculation.total)
        }
    }

    val finalCalculation = remember(uiState.selectedProduct, quantityDigits, priceDigits, amountPaidDigits) {
        calculateOwnerBarangMasuk(
            selectedProduct = uiState.selectedProduct,
            quantityDigits = quantityDigits,
            unitPriceDigits = priceDigits,
            amountPaidDigits = amountPaidDigits
        )
    }

    val dueDate = remember(dueDateIso) {
        dueDateIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    }

    val formValid = remember(uiState.selectedProduct, uiState.selectedSupplier, finalCalculation, dueDate) {
        isOwnerBarangMasukFormValid(
            selectedProduct = uiState.selectedProduct,
            selectedSupplier = uiState.selectedSupplier,
            calculation = finalCalculation,
            dueDate = dueDate
        )
    }

    val hasUnsavedChanges = remember(
        uiState.invoiceNoInput,
        quantityDigits,
        priceDigits,
        uiState.notesInput,
        hasManualAmountPaidEdit,
        paymentMethod,
        uiState.selectedProduct?.productId,
        uiState.selectedSupplier?.id,
        initialBaselineCaptured,
        initialProductId,
        initialSupplierId,
        initialBuyPriceDigits
    ) {
        val productChanged = initialBaselineCaptured && uiState.selectedProduct?.productId != initialProductId
        val supplierChanged = initialBaselineCaptured && uiState.selectedSupplier?.id != initialSupplierId
        val priceChanged = initialBaselineCaptured && priceDigits != initialBuyPriceDigits
        uiState.invoiceNoInput.isNotEmpty() ||
            quantityDigits.isNotEmpty() ||
            uiState.notesInput.isNotEmpty() ||
            hasManualAmountPaidEdit ||
            paymentMethod != IncomingPaymentMethod.CASH ||
            productChanged ||
            supplierChanged ||
            priceChanged
    }

    fun requestLeave(target: () -> Unit) {
        if (uiState.isSubmitting) return
        if (hasUnsavedChanges) {
            pendingNavAction = target
            showDiscardDialog = true
        } else {
            target()
        }
    }

    fun handleNavSelect(destination: DashboardNavDestination) {
        val action: () -> Unit = when (destination) {
            DashboardNavDestination.BERANDA -> {
                { destinationNavigator?.invoke(AdminDestination.Dashboard) ?: onDashboardClick() }
            }
            DashboardNavDestination.TRANSAKSI -> {
                { destinationNavigator?.invoke(AdminDestination.TransactionsHub) ?: onSalesTransactionsClick() }
            }
            DashboardNavDestination.KEUANGAN -> {
                { destinationNavigator?.invoke(AdminDestination.FinanceHub) ?: onReceivablesClick() }
            }
            DashboardNavDestination.STOK -> {
                { destinationNavigator?.invoke(AdminDestination.StockHub) ?: onProductsClick() }
            }
            DashboardNavDestination.MENU -> {
                { destinationNavigator?.invoke(AdminDestination.MoreHub) ?: onSettingsClick() }
            }
        }
        requestLeave(action)
    }

    BackHandler(enabled = !showProductPicker && !showSupplierPicker && !showDueDatePicker) {
        requestLeave(onBack)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OwnerBarangMasukScreen(
            selectedProduct = uiState.selectedProduct,
            selectedSupplier = uiState.selectedSupplier,
            invoiceNo = uiState.invoiceNoInput,
            quantityDigits = quantityDigits,
            unitPriceDigits = priceDigits,
            paymentMethod = paymentMethod,
            amountPaidDigits = amountPaidDigits,
            dueDate = dueDate,
            notes = uiState.notesInput,
            calculation = finalCalculation,
            submitEnabled = formValid,
            isSubmitting = uiState.isSubmitting,
            onBack = { requestLeave(onBack) },
            onProductFieldClick = { showProductPicker = true },
            onSupplierFieldClick = { showSupplierPicker = true },
            onAddSupplierClick = onAddSupplierClick,
            onInvoiceNoChange = viewModel::onInvoiceChanged,
            onGenerateInvoiceNo = viewModel::generateInvoiceNumber,
            onQuantityChange = { digits -> viewModel.onQuantityChanged(digits) },
            onUnitPriceChange = { digits -> viewModel.onBuyPriceChanged(digits) },
            onPaymentMethodSelect = { method ->
                paymentMethod = method
                hasManualAmountPaidEdit = false
                amountPaidDigits = initialAmountPaidDigitsForMethod(method, finalCalculation.total)
            },
            onAmountPaidChange = { digits ->
                hasManualAmountPaidEdit = true
                amountPaidDigits = digits
            },
            onPayFullClick = {
                hasManualAmountPaidEdit = false
                amountPaidDigits = initialAmountPaidDigitsForMethod(IncomingPaymentMethod.CASH, finalCalculation.total)
            },
            onDueDateFieldClick = { showDueDatePicker = true },
            onNotesChange = viewModel::onNotesChanged,
            onSubmit = {
                // TODO: CreatePurchaseCommand.dueDays saat ini Int non-null di model & API; saat sisa utang = 0 dikirim dueDays = 0 agar tidak mengubah repository/endpoint.
                val dueDays = resolveDueDaysForSubmit(
                    remainingDebt = finalCalculation.remainingDebt,
                    dueDate = dueDate,
                    today = today
                )
                viewModel.submitOwnerIncomingGoods(
                    invoiceNo = uiState.invoiceNoInput,
                    paymentMethod = paymentMethod,
                    amountPaid = finalCalculation.amountPaid,
                    dueDays = dueDays,
                    notes = uiState.notesInput
                ) {
                    OwnerBarangMasukToastBridge.setPendingSuccess("Barang masuk tersimpan")
                    onBack()
                }
            },
            onNavSelect = ::handleNavSelect
        )

        AppAlert(
            type = AlertType.Error,
            title = "Gagal menyimpan barang masuk",
            message = uiState.errorMessage.orEmpty(),
            onDismiss = viewModel::clearMessage,
            visible = !uiState.errorMessage.isNullOrBlank(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                .zIndex(99f)
                .testTag("barang-masuk-error-toast")
        )
    }

    if (showProductPicker) {
        ProductPickerModal(
            products = uiState.products,
            selectedProductId = uiState.selectedProduct?.productId,
            onSelect = { product ->
                viewModel.selectProduct(product)
                // Always replace unitPrice with the newly selected product's buy price when switching products
                viewModel.onBuyPriceChanged(product.priceBuy.toInputText())
                showProductPicker = false
            },
            onDismiss = { showProductPicker = false }
        )
    }

    if (showSupplierPicker) {
        SupplierPickerModal(
            suppliers = uiState.suppliers,
            selectedSupplierId = uiState.selectedSupplier?.id,
            onSelect = { supplier ->
                viewModel.selectSupplier(supplier)
                lastSyncedSupplierId = supplier.id
                dueDateIso = initialDueDateForSupplier(supplier.paymentTermDays, today)?.toString()
                showSupplierPicker = false
            },
            onDismiss = { showSupplierPicker = false }
        )
    }

    if (showDueDatePicker) {
        AppDatePickerDialog(
            initialDate = dueDateIso ?: today.toString(),
            title = "Pilih Jatuh Tempo",
            subtitle = "Tentukan tanggal jatuh tempo pembayaran supplier",
            initialMode = "Hari",
            onDismiss = { showDueDatePicker = false },
            onConfirm = { pickedIso ->
                dueDateIso = pickedIso
                showDueDatePicker = false
            },
            onConfirmRange = { startIso, _, _ ->
                dueDateIso = startIso
                showDueDatePicker = false
            }
        )
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
            title = "Buang data barang masuk?",
            message = "Isian yang belum disimpan akan hilang.",
            dialogTestTag = "discard-barang-masuk-dialog",
            continueTestTag = "discard-barang-masuk-continue",
            discardTestTag = "discard-barang-masuk-confirm"
        )
    }
}

@Composable
internal fun OwnerBarangMasukScreen(
    selectedProduct: ProductStock?,
    selectedSupplier: Supplier?,
    invoiceNo: String,
    quantityDigits: String,
    unitPriceDigits: String,
    paymentMethod: IncomingPaymentMethod,
    amountPaidDigits: String,
    dueDate: LocalDate?,
    notes: String,
    calculation: OwnerBarangMasukCalculation,
    submitEnabled: Boolean,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onProductFieldClick: () -> Unit,
    onSupplierFieldClick: () -> Unit,
    onAddSupplierClick: () -> Unit,
    onInvoiceNoChange: (String) -> Unit,
    onGenerateInvoiceNo: () -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitPriceChange: (String) -> Unit,
    onPaymentMethodSelect: (IncomingPaymentMethod) -> Unit,
    onAmountPaidChange: (String) -> Unit,
    onPayFullClick: () -> Unit,
    onDueDateFieldClick: () -> Unit,
    onNotesChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        val isTablet = maxWidth >= OWNER_BARANG_MASUK_TABLET_BREAKPOINT
        if (isTablet) {
            TabletOwnerBarangMasukLayout(
                selectedProduct = selectedProduct,
                selectedSupplier = selectedSupplier,
                invoiceNo = invoiceNo,
                quantityDigits = quantityDigits,
                unitPriceDigits = unitPriceDigits,
                paymentMethod = paymentMethod,
                amountPaidDigits = amountPaidDigits,
                dueDate = dueDate,
                notes = notes,
                calculation = calculation,
                submitEnabled = submitEnabled,
                isSubmitting = isSubmitting,
                onBack = onBack,
                onProductFieldClick = onProductFieldClick,
                onSupplierFieldClick = onSupplierFieldClick,
                onAddSupplierClick = onAddSupplierClick,
                onInvoiceNoChange = onInvoiceNoChange,
                onGenerateInvoiceNo = onGenerateInvoiceNo,
                onQuantityChange = onQuantityChange,
                onUnitPriceChange = onUnitPriceChange,
                onPaymentMethodSelect = onPaymentMethodSelect,
                onAmountPaidChange = onAmountPaidChange,
                onPayFullClick = onPayFullClick,
                onDueDateFieldClick = onDueDateFieldClick,
                onNotesChange = onNotesChange,
                onSubmit = onSubmit,
                onNavSelect = onNavSelect
            )
        } else {
            PhoneOwnerBarangMasukLayout(
                selectedProduct = selectedProduct,
                selectedSupplier = selectedSupplier,
                invoiceNo = invoiceNo,
                quantityDigits = quantityDigits,
                unitPriceDigits = unitPriceDigits,
                paymentMethod = paymentMethod,
                amountPaidDigits = amountPaidDigits,
                dueDate = dueDate,
                notes = notes,
                calculation = calculation,
                submitEnabled = submitEnabled,
                isSubmitting = isSubmitting,
                onBack = onBack,
                onProductFieldClick = onProductFieldClick,
                onSupplierFieldClick = onSupplierFieldClick,
                onAddSupplierClick = onAddSupplierClick,
                onInvoiceNoChange = onInvoiceNoChange,
                onGenerateInvoiceNo = onGenerateInvoiceNo,
                onQuantityChange = onQuantityChange,
                onUnitPriceChange = onUnitPriceChange,
                onPaymentMethodSelect = onPaymentMethodSelect,
                onAmountPaidChange = onAmountPaidChange,
                onPayFullClick = onPayFullClick,
                onDueDateFieldClick = onDueDateFieldClick,
                onNotesChange = onNotesChange,
                onSubmit = onSubmit
            )
        }
    }
}

@Composable
private fun PhoneOwnerBarangMasukLayout(
    selectedProduct: ProductStock?,
    selectedSupplier: Supplier?,
    invoiceNo: String,
    quantityDigits: String,
    unitPriceDigits: String,
    paymentMethod: IncomingPaymentMethod,
    amountPaidDigits: String,
    dueDate: LocalDate?,
    notes: String,
    calculation: OwnerBarangMasukCalculation,
    submitEnabled: Boolean,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onProductFieldClick: () -> Unit,
    onSupplierFieldClick: () -> Unit,
    onAddSupplierClick: () -> Unit,
    onInvoiceNoChange: (String) -> Unit,
    onGenerateInvoiceNo: () -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitPriceChange: (String) -> Unit,
    onPaymentMethodSelect: (IncomingPaymentMethod) -> Unit,
    onAmountPaidChange: (String) -> Unit,
    onPayFullClick: () -> Unit,
    onDueDateFieldClick: () -> Unit,
    onNotesChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val unitLabel = selectedProduct?.unitName?.takeIf { it.isNotBlank() } ?: "Pieces"
    val overlapPx = with(LocalDensity.current) { 28.dp.roundToPx() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Layout(
                content = {
                    BarangMasukPhoneGreenHeader(onBack = onBack)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                            .background(SurfaceWhite)
                            .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 24.dp)
                    ) {
                        ProductAndSupplierSection(
                            selectedProduct = selectedProduct,
                            selectedSupplier = selectedSupplier,
                            isTablet = false,
                            onProductFieldClick = onProductFieldClick,
                            onSupplierFieldClick = onSupplierFieldClick,
                            onAddSupplierClick = onAddSupplierClick
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        PurchaseDetailsSection(
                            invoiceNo = invoiceNo,
                            quantityDigits = quantityDigits,
                            unitPriceDigits = unitPriceDigits,
                            unitLabel = unitLabel,
                            calculation = calculation,
                            isTablet = false,
                            onInvoiceNoChange = onInvoiceNoChange,
                            onGenerateInvoiceNo = onGenerateInvoiceNo,
                            onQuantityChange = onQuantityChange,
                            onUnitPriceChange = onUnitPriceChange
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        PaymentSection(
                            calculation = calculation,
                            paymentMethod = paymentMethod,
                            amountPaidDigits = amountPaidDigits,
                            selectedSupplier = selectedSupplier,
                            dueDate = dueDate,
                            isTablet = false,
                            onPaymentMethodSelect = onPaymentMethodSelect,
                            onAmountPaidChange = onAmountPaidChange,
                            onPayFullClick = onPayFullClick,
                            onDueDateFieldClick = onDueDateFieldClick
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        NotesSection(notes = notes, onNotesChange = onNotesChange)
                    }
                }
            ) { measurables, constraints ->
                val headerPlaceable = measurables[0].measure(
                    constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                )
                val panelPlaceable = measurables[1].measure(
                    constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                )
                val totalHeight = (headerPlaceable.height - overlapPx + panelPlaceable.height)
                    .coerceAtLeast(headerPlaceable.height)

                layout(constraints.maxWidth, totalHeight) {
                    headerPlaceable.placeRelative(0, 0)
                    panelPlaceable.placeRelative(0, headerPlaceable.height - overlapPx)
                }
            }
        }

        FormFooter(
            isTablet = false,
            enabled = submitEnabled,
            isSubmitting = isSubmitting,
            onSaveClick = onSubmit
        )
    }
}

@Composable
private fun TabletOwnerBarangMasukLayout(
    selectedProduct: ProductStock?,
    selectedSupplier: Supplier?,
    invoiceNo: String,
    quantityDigits: String,
    unitPriceDigits: String,
    paymentMethod: IncomingPaymentMethod,
    amountPaidDigits: String,
    dueDate: LocalDate?,
    notes: String,
    calculation: OwnerBarangMasukCalculation,
    submitEnabled: Boolean,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onProductFieldClick: () -> Unit,
    onSupplierFieldClick: () -> Unit,
    onAddSupplierClick: () -> Unit,
    onInvoiceNoChange: (String) -> Unit,
    onGenerateInvoiceNo: () -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitPriceChange: (String) -> Unit,
    onPaymentMethodSelect: (IncomingPaymentMethod) -> Unit,
    onAmountPaidChange: (String) -> Unit,
    onPayFullClick: () -> Unit,
    onDueDateFieldClick: () -> Unit,
    onNotesChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit
) {
    val unitLabel = selectedProduct?.unitName?.takeIf { it.isNotBlank() } ?: "Pieces"

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .imePadding()
    ) {
        OwnerTabletNavRail(
            activeDestination = DashboardNavDestination.STOK,
            onNavSelect = onNavSelect
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            BarangMasukTabletGreenPanel(
                calculation = calculation,
                unitLabel = unitLabel,
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 288.dp)
                    .clip(RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp))
                    .background(SurfaceWhite)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 64.dp, top = 32.dp, end = 32.dp, bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            ProductAndSupplierSection(
                                selectedProduct = selectedProduct,
                                selectedSupplier = selectedSupplier,
                                isTablet = true,
                                onProductFieldClick = onProductFieldClick,
                                onSupplierFieldClick = onSupplierFieldClick,
                                onAddSupplierClick = onAddSupplierClick
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            PurchaseDetailsSection(
                                invoiceNo = invoiceNo,
                                quantityDigits = quantityDigits,
                                unitPriceDigits = unitPriceDigits,
                                unitLabel = unitLabel,
                                calculation = calculation,
                                isTablet = true,
                                onInvoiceNoChange = onInvoiceNoChange,
                                onGenerateInvoiceNo = onGenerateInvoiceNo,
                                onQuantityChange = onQuantityChange,
                                onUnitPriceChange = onUnitPriceChange
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            PaymentSection(
                                calculation = calculation,
                                paymentMethod = paymentMethod,
                                amountPaidDigits = amountPaidDigits,
                                selectedSupplier = selectedSupplier,
                                dueDate = dueDate,
                                isTablet = true,
                                onPaymentMethodSelect = onPaymentMethodSelect,
                                onAmountPaidChange = onAmountPaidChange,
                                onPayFullClick = onPayFullClick,
                                onDueDateFieldClick = onDueDateFieldClick
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            NotesSection(notes = notes, onNotesChange = onNotesChange)
                        }
                    }
                }

                FormFooter(
                    isTablet = true,
                    enabled = submitEnabled,
                    isSubmitting = isSubmitting,
                    onSaveClick = onSubmit
                )
            }
        }
    }
}

@Composable
private fun BarangMasukPhoneGreenHeader(
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
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

        BarangMasukHeaderTitleRow(
            onBack = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 44.dp, end = 20.dp)
        )
    }
}

@Composable
private fun BarangMasukTabletGreenPanel(
    calculation: OwnerBarangMasukCalculation,
    unitLabel: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(320.dp)
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
            BarangMasukHeaderTitleRow(
                onBack = onBack,
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                BarangMasukTabletLiveSummary(
                    calculation = calculation,
                    unitLabel = unitLabel
                )
            }
        }
    }
}

@Composable
private fun BarangMasukHeaderTitleRow(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .layout { measurable, constraints ->
                val startOutsetPx = 12.dp.roundToPx()
                val expandedWidth = if (constraints.hasBoundedWidth) {
                    constraints.maxWidth + startOutsetPx
                } else {
                    constraints.maxWidth
                }
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = if (constraints.hasFixedWidth) expandedWidth else 0,
                        maxWidth = expandedWidth,
                        minHeight = constraints.minHeight,
                        maxHeight = constraints.maxHeight
                    )
                )
                val layoutWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else placeable.width
                layout(layoutWidth, placeable.height) {
                    placeable.placeRelative(x = -startOutsetPx, y = 0)
                }
            }
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onBack
                )
                .semantics { contentDescription = "Kembali" }
                .testTag("barang-masuk-back-btn"),
            contentAlignment = Alignment.Center
        ) {
            OwnerVectorIcon(
                imageVector = OwnerIcons.ChevronLeft,
                tint = OnBrand,
                size = 24.dp
            )
        }

        Text(
            text = "Barang masuk",
            style = ownerTextStyle(
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnBrand
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .cssLineBoxHeight(26.dp)
                .semantics { heading() }
        )
    }
}

@Composable
private fun BarangMasukTabletLiveSummary(
    calculation: OwnerBarangMasukCalculation,
    unitLabel: String
) {
    val lineColor = Color.White.copy(alpha = 0.18f)
    val heroAmount = remember(calculation.total) { formatBareWholeNumber(calculation.total) }
    val paidCompact = remember(calculation.amountPaid) {
        formatRupiahCompact(calculation.amountPaid.coerceAtLeast(BigDecimal.ZERO).toDouble())
    }
    val remainingCompact = remember(calculation.remainingDebt) {
        formatRupiahCompact(calculation.remainingDebt.coerceAtLeast(BigDecimal.ZERO).toDouble())
    }

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    var heroRowWidthPx by remember { mutableIntStateOf(0) }
    val rpWidthPx = remember(density.fontScale) {
        textMeasurer.measure(
            text = "Rp",
            style = ownerTextStyle(
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandSoftText
            ),
            maxLines = 1,
            softWrap = false
        ).size.width
    }
    val gapPx = with(density) { 6.dp.roundToPx() }
    val availableNumberPx = (heroRowWidthPx - rpWidthPx - gapPx).coerceAtLeast(1)
    val heroFontSize = remember(heroAmount, heroRowWidthPx, availableNumberPx, density.fontScale) {
        if (heroRowWidthPx <= 0) {
            36.sp
        } else {
            (36 downTo 22 step 2).map { it.toFloat() }.firstOrNull { sizeSp ->
                textMeasurer.measure(
                    text = heroAmount,
                    style = ownerTextStyle(
                        fontSize = sizeSp.sp,
                        lineHeight = if (sizeSp == 36f) 42.sp else (sizeSp * 1.17f).sp,
                        fontWeight = FontWeight.Bold,
                        color = OnBrand,
                        letterSpacing = (-0.03).em,
                        tabularNumbers = true
                    ),
                    maxLines = 1,
                    softWrap = false
                ).size.width <= availableNumberPx
            }?.sp ?: 22.sp
        }
    }
    val heroLineHeight = if (heroFontSize == 36.sp) 42.sp else (heroFontSize.value * 1.17f).sp

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Total pembelian",
            style = ownerTextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                color = BrandSoftText
            ),
            maxLines = 1,
            modifier = Modifier.cssLineBoxHeight(20.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { heroRowWidthPx = it.width },
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "Rp",
                style = ownerTextStyle(
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandSoftText
                ),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.alignByBaseline()
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = heroAmount,
                style = ownerTextStyle(
                    fontSize = heroFontSize,
                    lineHeight = heroLineHeight,
                    fontWeight = FontWeight.Bold,
                    color = OnBrand,
                    letterSpacing = (-0.03).em,
                    tabularNumbers = true
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .alignByBaseline()
                    .cssLineBoxHeight((heroLineHeight.value * density.fontScale).dp)
                    .testTag("barang-masuk-tablet-total")
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(lineColor)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Dibayar",
                    style = ownerTextStyle(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = BrandSoftText
                    ),
                    maxLines = 1,
                    modifier = Modifier.cssLineBoxHeight(16.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = paidCompact,
                    style = ownerTextStyle(
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnBrand,
                        tabularNumbers = true
                    ),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .cssLineBoxHeight(20.dp)
                        .testTag("barang-masuk-tablet-paid")
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .ownerHeroLeftDivider(lineColor)
            ) {
                Text(
                    text = "Sisa utang",
                    style = ownerTextStyle(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = BrandSoftText
                    ),
                    maxLines = 1,
                    modifier = Modifier.cssLineBoxHeight(16.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = remainingCompact,
                    style = ownerTextStyle(
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnBrand,
                        tabularNumbers = true
                    ),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .cssLineBoxHeight(20.dp)
                        .testTag("barang-masuk-tablet-remaining")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        StockImpact(
            calculation = calculation,
            unitLabel = unitLabel,
            onGreenBackground = true
        )
    }
}

@Composable
private fun ProductAndSupplierSection(
    selectedProduct: ProductStock?,
    selectedSupplier: Supplier?,
    isTablet: Boolean,
    onProductFieldClick: () -> Unit,
    onSupplierFieldClick: () -> Unit,
    onAddSupplierClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(text = "Produk & supplier")

        SelectField(
            label = "Produk",
            value = selectedProduct?.productName,
            placeholder = "Pilih produk",
            onClick = onProductFieldClick,
            testTag = "barang-masuk-product-field"
        )

        if (selectedProduct != null) {
            Spacer(modifier = Modifier.height(12.dp))
            ProductMetaInfoBlock(
                product = selectedProduct,
                showPhoneMetrics = !isTablet
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        SelectField(
            label = "Supplier",
            value = selectedSupplier?.name,
            placeholder = "Pilih supplier",
            onClick = onSupplierFieldClick,
            testTag = "barang-masuk-supplier-field"
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val startOutsetPx = 4.dp.roundToPx()
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.placeRelative(x = -startOutsetPx, y = 0)
                    }
                }
                .height(40.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onAddSupplierClick
                )
                .padding(horizontal = 4.dp)
                .testTag("barang-masuk-add-supplier"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OwnerVectorIcon(
                imageVector = OwnerIcons.Plus,
                tint = Brand,
                size = 20.dp
            )
            Text(
                text = "Tambah supplier baru",
                style = ownerTextStyle(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Brand
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PurchaseDetailsSection(
    invoiceNo: String,
    quantityDigits: String,
    unitPriceDigits: String,
    unitLabel: String,
    calculation: OwnerBarangMasukCalculation,
    isTablet: Boolean,
    onInvoiceNoChange: (String) -> Unit,
    onGenerateInvoiceNo: () -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitPriceChange: (String) -> Unit
) {
    val autoButtonWidth = if (isTablet) 100.dp else 112.dp
    val invoiceInteraction = remember { MutableInteractionSource() }
    val invoiceFocused by invoiceInteraction.collectIsFocusedAsState()
    val invoiceBorderWidth = if (invoiceFocused) 1.5.dp else 1.dp
    val invoiceBorderColor = if (invoiceFocused) Brand else OwnerBorder
    val invoiceHorizontalPad = if (invoiceFocused) 15.5.dp else 16.dp

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(text = "Rincian pembelian")

        Text(
            text = "Nomor nota",
            style = ownerTextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Ink
            ),
            maxLines = 1,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .cssLineBoxHeight(20.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = invoiceNo,
                onValueChange = onInvoiceNoChange,
                singleLine = true,
                textStyle = ownerTextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = Ink,
                    tabularNumbers = true
                ),
                interactionSource = invoiceInteraction,
                cursorBrush = SolidColor(Brand),
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("barang-masuk-invoice-input"),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceWhite)
                            .border(invoiceBorderWidth, invoiceBorderColor, RoundedCornerShape(12.dp))
                            .padding(horizontal = invoiceHorizontalPad),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (invoiceNo.isEmpty()) {
                            Text(
                                text = "Nomor nota",
                                style = ownerTextStyle(
                                    fontSize = 16.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = FormPlaceholderColor
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                }
            )

            val autoButtonMeasurer = rememberTextMeasurer()
            val autoButtonDensity = LocalDensity.current
            var autoButtonWidthPx by remember { mutableIntStateOf(0) }
            val autoButtonAvailablePx = (autoButtonWidthPx - with(autoButtonDensity) { 16.dp.toPx().toInt() }).coerceAtLeast(1)
            val autoButtonTextSize = remember(autoButtonWidthPx, autoButtonDensity.fontScale) {
                if (autoButtonWidthPx <= 0) {
                    14.sp
                } else {
                    listOf(14f, 13f, 12f, 11f).firstOrNull { sizeSp ->
                        autoButtonMeasurer.measure(
                            text = "Buat nomor",
                            style = ownerTextStyle(
                                fontSize = sizeSp.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Brand
                            ),
                            maxLines = 1,
                            softWrap = false
                        ).size.width <= autoButtonAvailablePx
                    }?.sp ?: 11.sp
                }
            }
            Box(
                modifier = Modifier
                    .width(autoButtonWidth)
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceWhite)
                    .border(1.dp, Brand, RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onGenerateInvoiceNo
                    )
                    .onSizeChanged { autoButtonWidthPx = it.width }
                    .padding(horizontal = 8.dp)
                    .testTag("barang-masuk-generate-invoice"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Buat nomor",
                    style = ownerTextStyle(
                        fontSize = autoButtonTextSize,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Brand
                    ),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (!isTablet) {
            Text(
                text = "Nomor nota dibuat otomatis dan tetap dapat diubah.",
                style = ownerTextStyle(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Muted
                ),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            FormField(
                label = "Jumlah masuk",
                value = quantityDigits,
                onValueChange = { raw -> onQuantityChange(raw.filter(Char::isDigit)) },
                placeholder = "0",
                suffixText = unitLabel,
                tabularNumbers = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                testTag = "barang-masuk-qty-input",
                modifier = Modifier.weight(1f)
            )

            MoneyField(
                label = "Harga satuan",
                digits = unitPriceDigits,
                onDigitsChange = onUnitPriceChange,
                placeholder = "0",
                testTag = "barang-masuk-price-input",
                modifier = Modifier.weight(1f)
            )
        }

        if (!isTablet) {
            Spacer(modifier = Modifier.height(20.dp))
            StockImpact(
                calculation = calculation,
                unitLabel = unitLabel,
                onGreenBackground = false
            )
        }
    }
}

@Composable
private fun PaymentSection(
    calculation: OwnerBarangMasukCalculation,
    paymentMethod: IncomingPaymentMethod,
    amountPaidDigits: String,
    selectedSupplier: Supplier?,
    dueDate: LocalDate?,
    isTablet: Boolean,
    onPaymentMethodSelect: (IncomingPaymentMethod) -> Unit,
    onAmountPaidChange: (String) -> Unit,
    onPayFullClick: () -> Unit,
    onDueDateFieldClick: () -> Unit
) {
    val showDebtFields = calculation.remainingDebt > BigDecimal.ZERO
    val amountError = if (calculation.amountPaidExceedsTotal) {
        "Tidak boleh melebihi total pembelian"
    } else {
        null
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(text = "Pembayaran")

        if (!isTablet) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "Total pembelian",
                        style = ownerTextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Normal,
                            color = Muted
                        ),
                        maxLines = 1,
                        modifier = Modifier.alignByBaseline()
                    )
                    Text(
                        text = formatRupiahWhole(calculation.total),
                        style = ownerTextStyle(
                            fontSize = 20.sp,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Brand,
                            tabularNumbers = true
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .alignByBaseline()
                            .testTag("barang-masuk-phone-total")
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(OwnerDivider)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        PaymentMethodChips(
            selected = paymentMethod,
            isTablet = isTablet,
            onSelect = onPaymentMethodSelect
        )

        Spacer(modifier = Modifier.height(20.dp))

        MoneyField(
            label = "Jumlah dibayar",
            digits = amountPaidDigits,
            onDigitsChange = onAmountPaidChange,
            placeholder = "0",
            errorText = amountError,
            labelEndAction = {
                Text(
                    text = "Bayar penuh",
                    style = ownerTextStyle(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Brand
                    ),
                    maxLines = 1,
                    modifier = Modifier
                        .cssLineBoxHeight(20.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = onPayFullClick
                        )
                        .testTag("barang-masuk-pay-full")
                )
            },
            testTag = "barang-masuk-amount-paid-input"
        )

        if (showDebtFields) {
            Spacer(modifier = Modifier.height(12.dp))
            LabelValueRow(
                label = "Sisa utang ke supplier",
                value = formatRupiahWhole(calculation.remainingDebt),
                valueColor = BarangMasukAmberInk,
                valueTestTag = "barang-masuk-sisa-utang"
            )

            Spacer(modifier = Modifier.height(20.dp))

            val termDays = selectedSupplier?.paymentTermDays ?: 0
            val dueDateError = if (dueDate == null) "Jatuh tempo wajib dipilih" else null
            val dueHelper = if (termDays > 0) {
                "Mengikuti termin supplier, $termDays hari."
            } else {
                "Supplier belum memiliki termin. Pilih jatuh tempo."
            }

            DateReadOnlyField(
                label = "Jatuh tempo",
                displayValue = dueDate?.let(::formatIndonesianDateShort),
                placeholder = "Pilih tanggal jatuh tempo",
                helperText = dueHelper,
                errorText = dueDateError,
                onClick = onDueDateFieldClick,
                testTag = "barang-masuk-due-date"
            )
        }
    }
}

@Composable
private fun NotesSection(
    notes: String,
    onNotesChange: (String) -> Unit
) {
    FormField(
        label = "Catatan",
        optional = true,
        value = notes,
        onValueChange = onNotesChange,
        placeholder = "Contoh: restok gudang utama dari nota supplier",
        singleLine = false,
        fieldHeight = 88.dp,
        multilineTopPadding = 16.dp,
        testTag = "barang-masuk-notes-input"
    )
}

internal fun sampleReferenceBarangMasukProduct(): ProductStock = ProductStock(
    productId = "p-1",
    sku = "SEM-655815",
    productName = "Semen Merdeka",
    categoryName = "Semen",
    unitName = "Pieces",
    quantity = BigDecimal.ZERO,
    minStock = BigDecimal("10"),
    priceBuy = BigDecimal("38000"),
    priceRetail = BigDecimal("48000"),
    priceContractor = BigDecimal("45000"),
    discount = BigDecimal.ZERO,
    isActive = true
)

internal fun sampleReferenceBarangMasukSupplier(): Supplier = Supplier(
    id = "sup-1",
    name = "Supri",
    phone = "08123456789",
    address = "Jl. Raya No. 1",
    paymentTermDays = 30,
    isActive = true,
    createdAt = "2026-01-01T00:00:00Z",
    updatedAt = "2026-01-01T00:00:00Z"
)

@Preview(
    name = "Barokah Jaya Barang Masuk - Ponsel (390x1605)",
    widthDp = 390,
    heightDp = 1605,
    showBackground = true
)
@Composable
internal fun OwnerBarangMasukPhonePreview() {
    TbterminalappTheme {
        val product = sampleReferenceBarangMasukProduct()
        val supplier = sampleReferenceBarangMasukSupplier()
        val calc = calculateOwnerBarangMasuk(
            selectedProduct = product,
            quantityDigits = "200",
            unitPriceDigits = "38000",
            amountPaidDigits = "5000000"
        )
        OwnerBarangMasukScreen(
            selectedProduct = product,
            selectedSupplier = supplier,
            invoiceNo = "",
            quantityDigits = "200",
            unitPriceDigits = "38000",
            paymentMethod = IncomingPaymentMethod.TRANSFER,
            amountPaidDigits = "5000000",
            dueDate = LocalDate.of(2026, 12, 23),
            notes = "",
            calculation = calc,
            submitEnabled = true,
            isSubmitting = false,
            onBack = {},
            onProductFieldClick = {},
            onSupplierFieldClick = {},
            onAddSupplierClick = {},
            onInvoiceNoChange = {},
            onGenerateInvoiceNo = {},
            onQuantityChange = {},
            onUnitPriceChange = {},
            onPaymentMethodSelect = {},
            onAmountPaidChange = {},
            onPayFullClick = {},
            onDueDateFieldClick = {},
            onNotesChange = {},
            onSubmit = {},
            onNavSelect = {}
        )
    }
}

@Preview(
    name = "Barokah Jaya Barang Masuk - Tablet (1024x768)",
    widthDp = 1024,
    heightDp = 768,
    showBackground = true
)
@Composable
internal fun OwnerBarangMasukTabletPreview() {
    TbterminalappTheme {
        val product = sampleReferenceBarangMasukProduct()
        val supplier = sampleReferenceBarangMasukSupplier()
        val calc = calculateOwnerBarangMasuk(
            selectedProduct = product,
            quantityDigits = "200",
            unitPriceDigits = "38000",
            amountPaidDigits = "5000000"
        )
        OwnerBarangMasukScreen(
            selectedProduct = product,
            selectedSupplier = supplier,
            invoiceNo = "",
            quantityDigits = "200",
            unitPriceDigits = "38000",
            paymentMethod = IncomingPaymentMethod.TRANSFER,
            amountPaidDigits = "5000000",
            dueDate = LocalDate.of(2026, 12, 23),
            notes = "",
            calculation = calc,
            submitEnabled = true,
            isSubmitting = false,
            onBack = {},
            onProductFieldClick = {},
            onSupplierFieldClick = {},
            onAddSupplierClick = {},
            onInvoiceNoChange = {},
            onGenerateInvoiceNo = {},
            onQuantityChange = {},
            onUnitPriceChange = {},
            onPaymentMethodSelect = {},
            onAmountPaidChange = {},
            onPayFullClick = {},
            onDueDateFieldClick = {},
            onNotesChange = {},
            onSubmit = {},
            onNavSelect = {}
        )
    }
}
