package com.tbterminal.app.ui.receivables

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Info
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.model.Receivable
import com.tbterminal.app.data.repository.CustomerRepository
import com.tbterminal.app.ui.components.AppDatePickerDialog
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

private val Brand50 = Color(0xFFF0F7F5)
private val Brand100 = Color(0xFFE1EFEA)
private val Brand200 = Color(0xFFC4DFD6)
private val Brand600 = Color(0xFF256B57)
private val Brand700 = Color(0xFF1D5344)
private val Slate50 = Color(0xFFF8FAFC)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate300 = Color(0xFFCBD5E1)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate600 = Color(0xFF475569)
private val Slate700 = Color(0xFF334155)
private val Slate800 = Color(0xFF1E293B)
private val Slate900 = Color(0xFF0F172A)

private fun formatRupiah(amount: BigDecimal): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"))
    return formatter.format(amount).replace("Rp", "Rp ")
}

private fun formatIndoDate(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr)
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
        String.format("%02d %s %d", date.dayOfMonth, months[date.monthValue - 1], date.year)
    } catch (e: Exception) {
        dateStr
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAdjustmentReceivableScreen(
    name: String,
    role: String,
    customerId: String? = null,
    viewModel: ReceivableViewModel,
    customerRepository: CustomerRepository,
    onDashboardClick: () -> Unit,
    onProductsClick: () -> Unit,
    onAddProductClick: () -> Unit = onProductsClick,
    onProductCategoriesClick: () -> Unit = onProductsClick,
    onProductUnitsClick: () -> Unit = onProductsClick,
    onStockOpnameClick: () -> Unit = {},
    onStockOpnameFormClick: () -> Unit = onStockOpnameClick,
    onIncomingGoodsClick: () -> Unit = {},
    onIncomingGoodsFormClick: () -> Unit = onIncomingGoodsClick,
    onSupplierDebtsClick: () -> Unit = {},
    onReceivablesClick: () -> Unit = {},
    onCustomersClick: () -> Unit = {},
    onAddCustomerClick: () -> Unit = onCustomersClick,
    onReportsClick: () -> Unit = {},
    onOperationalAuditClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onBackToReceivables: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 840

    // Initialize with customerId if provided
    LaunchedEffect(customerId) {
        viewModel.initAdjustmentForCustomer(customerId)
    }

    // Auto navigate back on successful save
    LaunchedEffect(uiState.isAdjustmentSaved) {
        if (uiState.isAdjustmentSaved) {
            viewModel.resetAdjustmentSaved()
            onBackToReceivables()
        }
    }

    val selectedCustomer = uiState.customers.firstOrNull { it.id == uiState.adjustmentCustomerId }
    val customerSummary = uiState.customerSummaries.firstOrNull { it.customerId == uiState.adjustmentCustomerId }

    val debtDate = uiState.adjustmentDebtDateInput
    val dueDate = uiState.adjustmentDueDateInput
    val tenorDays = remember(debtDate, dueDate) {
        try {
            val start = LocalDate.parse(debtDate)
            val end = LocalDate.parse(dueDate)
            ChronoUnit.DAYS.between(start, end).coerceAtLeast(0)
        } catch (e: Exception) {
            30L
        }
    }

    var showDebtDatePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }
    var showCustomerPickerDialog by remember { mutableStateOf(false) }

    if (showDebtDatePicker) {
        AppDatePickerDialog(
            initialDate = debtDate,
            title = "Pilih Tanggal Penyesuaian",
            subtitle = "Tentukan tanggal pencatatan penyesuaian piutang",
            onDismiss = { showDebtDatePicker = false },
            onConfirm = { picked ->
                viewModel.onAdjustmentDebtDateChanged(picked)
                showDebtDatePicker = false
            }
        )
    }

    if (showDueDatePicker) {
        AppDatePickerDialog(
            initialDate = dueDate,
            title = "Pilih Tanggal Jatuh Tempo",
            subtitle = "Tentukan batas waktu pelunasan",
            onDismiss = { showDueDatePicker = false },
            onConfirm = { picked ->
                viewModel.onAdjustmentDueDateChanged(picked)
                showDueDatePicker = false
            }
        )
    }

    // Centered Dialog for Customer Selection
    if (showCustomerPickerDialog) {
        CustomerSelectionDialog(
            customerRepository = customerRepository,
            selectedCustomerId = uiState.adjustmentCustomerId,
            onCustomerSelected = { customer ->
                viewModel.onAdjustmentCustomerSelected(customer)
                showCustomerPickerDialog = false
            },
            onAddCustomerClick = {
                showCustomerPickerDialog = false
                onAddCustomerClick()
            },
            onDismiss = { showCustomerPickerDialog = false }
        )
    }

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = AdminDestination.AdjustmentReceivableForm,
        pageTitle = "Penyesuaian Piutang",
        showPageHeader = true,
        onBack = onBackToReceivables,
        onDashboardClick = onDashboardClick,
        onProductsClick = onProductsClick,
        onAddProductClick = onAddProductClick,
        onProductCategoriesClick = onProductCategoriesClick,
        onProductUnitsClick = onProductUnitsClick,
        onStockOpnameClick = onStockOpnameClick,
        onStockOpnameFormClick = onStockOpnameFormClick,
        onIncomingGoodsClick = onIncomingGoodsClick,
        onIncomingGoodsFormClick = onIncomingGoodsFormClick,
        onSupplierDebtsClick = onSupplierDebtsClick,
        onReceivablesClick = onReceivablesClick,
        onCustomersClick = onCustomersClick,
        onReportsClick = onReportsClick,
        onOperationalAuditClick = onOperationalAuditClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    ) { contentModifier ->
        Column(
            modifier = contentModifier
                .fillMaxSize()
                .background(Slate50)
        ) {
            // Main Scrollable Area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (isWideScreen) 24.dp else 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Banner Otoritas Admin / Owner (Clear & Light)
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brand50)
                                .border(1.dp, Brand100, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Shield, contentDescription = null, tint = Brand600, modifier = Modifier.size(22.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Otoritas Koreksi & Penyesuaian Piutang",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Hanya akun Owner dan Admin yang memiliki izin melakukan adjustment piutang. Setiap perubahan terekam dalam log audit pembukuan.",
                                fontSize = 12.sp,
                                color = Slate500,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Error Message Banner
                if (uiState.errorMessage != null) {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = uiState.errorMessage ?: "",
                                color = Color(0xFFDC2626),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (isWideScreen) {
                    // Layout 2 Kolom (Tablet & Desktop)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Kolom Kiri: Form Input Data
                        Column(
                            modifier = Modifier.weight(1.3f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CardSection(title = "1. PELANGGAN YANG DISESUAIKAN", required = true) {
                                CustomerCardSelector(
                                    selectedCustomer = selectedCustomer,
                                    onOpenPicker = { showCustomerPickerDialog = true },
                                    onClearCustomer = { viewModel.onAdjustmentCustomerChanged("") }
                                )
                            }

                            CardSection(title = "2. NOMINAL PENYESUAIAN", required = true) {
                                AdjustmentAmountInputs(
                                    amountInput = uiState.adjustmentAmountInput,
                                    onAmountChanged = viewModel::onAdjustmentAmountChanged
                                )
                            }

                            CardSection(title = "3. REFERENSI & ALASAN PENYESUAIAN", required = true) {
                                AdjustmentDocInputs(
                                    reference = uiState.adjustmentReferenceInput,
                                    reason = uiState.adjustmentReasonInput,
                                    selectedCustomer = selectedCustomer,
                                    activeInvoices = uiState.customerActiveInvoices,
                                    isLoadingInvoices = uiState.isLoadingCustomerInvoices,
                                    onSelectInvoice = viewModel::onSelectInvoiceForAdjustment,
                                    onClearSelectedInvoice = { viewModel.onAdjustmentReferenceChanged("") },
                                    onReferenceChanged = viewModel::onAdjustmentReferenceChanged,
                                    onReasonChanged = viewModel::onAdjustmentReasonChanged
                                )
                            }

                            CardSection(title = "4. TANGGAL EFEKTIF & JATUH TEMPO", required = true) {
                                AdjustmentTimelineInputs(
                                    debtDate = debtDate,
                                    dueDate = dueDate,
                                    tenorDays = tenorDays,
                                    onPickDebtDate = { showDebtDatePicker = true },
                                    onPickDueDate = { showDueDatePicker = true },
                                    onDueDateChanged = viewModel::onAdjustmentDueDateChanged
                                )
                            }
                        }

                        // Kolom Kanan: Preview Simulasi Pembukuan & Status Saldo
                        Column(
                            modifier = Modifier.weight(0.7f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            AdjustmentAccountingSummary(
                                selectedCustomer = selectedCustomer,
                                currentRemaining = customerSummary?.totalRemaining ?: BigDecimal.ZERO,
                                inputAmountStr = uiState.adjustmentAmountInput,
                                reference = uiState.adjustmentReferenceInput,
                                reason = uiState.adjustmentReasonInput,
                                dueDate = dueDate,
                                tenorDays = tenorDays
                            )
                        }
                    }
                } else {
                    // Layout 1 Kolom (Mobile Phone)
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CardSection(title = "1. PELANGGAN YANG DISESUAIKAN", required = true) {
                            CustomerCardSelector(
                                selectedCustomer = selectedCustomer,
                                onOpenPicker = { showCustomerPickerDialog = true },
                                onClearCustomer = { viewModel.onAdjustmentCustomerChanged("") }
                            )
                        }

                        CardSection(title = "2. NOMINAL PENYESUAIAN", required = true) {
                            AdjustmentAmountInputs(
                                amountInput = uiState.adjustmentAmountInput,
                                onAmountChanged = viewModel::onAdjustmentAmountChanged
                            )
                        }

                        CardSection(title = "3. REFERENSI & ALASAN PENYESUAIAN", required = true) {
                            AdjustmentDocInputs(
                                reference = uiState.adjustmentReferenceInput,
                                reason = uiState.adjustmentReasonInput,
                                selectedCustomer = selectedCustomer,
                                activeInvoices = uiState.customerActiveInvoices,
                                isLoadingInvoices = uiState.isLoadingCustomerInvoices,
                                onSelectInvoice = viewModel::onSelectInvoiceForAdjustment,
                                onClearSelectedInvoice = { viewModel.onAdjustmentReferenceChanged("") },
                                onReferenceChanged = viewModel::onAdjustmentReferenceChanged,
                                onReasonChanged = viewModel::onAdjustmentReasonChanged
                            )
                        }

                        CardSection(title = "4. TANGGAL EFEKTIF & JATUH TEMPO", required = true) {
                            AdjustmentTimelineInputs(
                                debtDate = debtDate,
                                dueDate = dueDate,
                                tenorDays = tenorDays,
                                onPickDebtDate = { showDebtDatePicker = true },
                                onPickDueDate = { showDueDatePicker = true },
                                onDueDateChanged = viewModel::onAdjustmentDueDateChanged
                            )
                        }

                        // Preview Simulasi Akuntansi di bawah form mobile
                        AdjustmentAccountingSummary(
                            selectedCustomer = selectedCustomer,
                            currentRemaining = customerSummary?.totalRemaining ?: BigDecimal.ZERO,
                            inputAmountStr = uiState.adjustmentAmountInput,
                            reference = uiState.adjustmentReferenceInput,
                            reason = uiState.adjustmentReasonInput,
                            dueDate = dueDate,
                            tenorDays = tenorDays
                        )
                    }
                }
            }

            // Sticky Bottom Action Bar (Transparent Background)
            Surface(
                color = Color.Transparent,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isWideScreen) 24.dp else 16.dp, vertical = 14.dp),
                    horizontalArrangement = if (isWideScreen) Arrangement.End else Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val canSubmit = uiState.adjustmentCustomerId.isNotBlank() &&
                        uiState.adjustmentAmountInput.isNotBlank() &&
                        !uiState.isSubmittingAdjustment

                    Button(
                        onClick = viewModel::submitAdjustment,
                        enabled = canSubmit,
                        modifier = if (isWideScreen) Modifier.width(220.dp).height(48.dp) else Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Brand600,
                            disabledContainerColor = Slate200
                        )
                    ) {
                        if (uiState.isSubmittingAdjustment) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Menyimpan...", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        } else {
                            Text("Simpan", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardSection(
    title: String,
    required: Boolean = false,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate500,
                    letterSpacing = 0.8.sp
                )
                if (required) {
                    Text("*", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                }
            }
            content()
        }
    }
}

@Composable
private fun CustomerCardSelector(
    selectedCustomer: Customer?,
    onOpenPicker: () -> Unit,
    onClearCustomer: () -> Unit
) {
    if (selectedCustomer != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .border(1.5.dp, Brand600, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brand50)
                        .border(1.dp, Brand100, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectedCustomer.name.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Brand700,
                        fontSize = 15.sp
                    )
                }
                Column {
                    Text(
                        text = selectedCustomer.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    Text(
                        text = selectedCustomer.phone ?: "Tanpa nomor telepon",
                        fontSize = 11.5.sp,
                        color = Slate500
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(
                    onClick = onOpenPicker,
                    colors = ButtonDefaults.textButtonColors(contentColor = Brand600)
                ) {
                    Text("Ganti", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(
                    onClick = onClearCustomer,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Hapus", tint = Slate400, modifier = Modifier.size(16.dp))
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenPicker)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Slate100),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = Slate500, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text("Pilih Pelanggan", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Slate800)
                    Text("Cari pelanggan yang akan disesuaikan piutangnya", fontSize = 11.5.sp, color = Slate400)
                }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun AdjustmentAmountInputs(
    amountInput: String,
    onAmountChanged: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Nominal Box
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "NOMINAL PENYESUAIAN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate600)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(1.dp, Slate300, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Rp", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Brand600)
                Spacer(modifier = Modifier.width(10.dp))
                BasicTextField(
                    value = amountInput,
                    onValueChange = onAmountChanged,
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate900),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (amountInput.isEmpty()) {
                            Text("0", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Slate300)
                        }
                        innerTextField()
                    }
                )
                if (amountInput.isNotEmpty()) {
                    IconButton(onClick = { onAmountChanged("") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Hapus", tint = Slate400, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Quick Amount Chips (Filter Piutang style: light & clean)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "PILIHAN NOMINAL CEPAT", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = Slate400)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val chips = listOf(
                    "50 rb" to "50000",
                    "100 rb" to "100000",
                    "250 rb" to "250000",
                    "500 rb" to "500000",
                    "1 jt" to "1000000"
                )
                chips.forEach { (label, value) ->
                    val isCurrent = amountInput == value
                    Surface(
                        onClick = {
                            val current = amountInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
                            val add = value.toBigDecimal()
                            onAmountChanged((current + add).toPlainString())
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        border = BorderStroke(if (isCurrent) 1.5.dp else 1.dp, if (isCurrent) Brand600 else Slate200),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "+$label",
                                fontSize = 11.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isCurrent) Brand600 else Slate700
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustmentDocInputs(
    reference: String,
    reason: String,
    selectedCustomer: Customer?,
    activeInvoices: List<Receivable>,
    isLoadingInvoices: Boolean,
    onSelectInvoice: (Receivable) -> Unit,
    onClearSelectedInvoice: () -> Unit,
    onReferenceChanged: (String) -> Unit,
    onReasonChanged: (String) -> Unit
) {
    var showInvoicePickerDialog by remember { mutableStateOf(false) }

    if (showInvoicePickerDialog && selectedCustomer != null) {
        ActiveInvoiceSelectionDialog(
            customerName = selectedCustomer.name,
            invoices = activeInvoices,
            selectedReference = reference,
            onSelectInvoice = onSelectInvoice,
            onDismiss = { showInvoicePickerDialog = false }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Pilihan Nota Piutang Aktif (Simpel & Compact)
        if (selectedCustomer != null) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "NOTA PIUTANG YANG DISESUAIKAN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate600
                    )
                    if (activeInvoices.isNotEmpty()) {
                        Text(
                            text = "${activeInvoices.size} Nota Berjalan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Brand600
                        )
                    }
                }

                if (isLoadingInvoices) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Brand600
                        )
                        Text(
                            text = "Memuat daftar nota piutang aktif...",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    }
                } else if (activeInvoices.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Info,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Tidak ada nota belum lunas. Anda dapat mengisi nomor referensi manual di bawah.",
                                fontSize = 11.5.sp,
                                color = Slate500,
                                lineHeight = 16.sp
                            )
                        }
                    }
                } else {
                    val selectedInvoice = activeInvoices.firstOrNull { inv ->
                        val invRef = inv.legacyInvoiceNumber?.takeIf(String::isNotBlank)
                            ?: inv.transactionId?.takeIf(String::isNotBlank)
                            ?: "NOTA-${inv.id.takeLast(6).uppercase()}"
                        invRef == reference
                    }

                    if (selectedInvoice == null) {
                        // Compact selector row (1 row, doesn't stretch down)
                        Surface(
                            onClick = { showInvoicePickerDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Brand50, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Outlined.ReceiptLong,
                                            contentDescription = null,
                                            tint = Brand600,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Pilih dari ${activeInvoices.size} Nota Berjalan...",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Slate800
                                        )
                                        Text(
                                            text = "Ketuk untuk memilih nota & isi nominal otomatis",
                                            fontSize = 11.sp,
                                            color = Slate400
                                        )
                                    }
                                }
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        // Sleek single selected card
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.5.dp, Brand600),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Brand50, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = Brand600,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = reference,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Brand700
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Brand50,
                                                border = BorderStroke(0.5.dp, Brand200)
                                            ) {
                                                Text(
                                                    text = "Dipilih",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Brand600,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Sisa: ${formatRupiah(selectedInvoice.remainingAmount)} • Jth Tempo: ${formatIndoDate(selectedInvoice.dueDate)}",
                                            fontSize = 11.sp,
                                            color = Slate500
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        onClick = { showInvoicePickerDialog = true },
                                        shape = RoundedCornerShape(6.dp),
                                        color = Slate100,
                                        modifier = Modifier.padding(end = 2.dp)
                                    ) {
                                        Text(
                                            text = "Ganti",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Slate700,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = onClearSelectedInvoice,
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Hapus pilihan",
                                            tint = Slate400,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dokumen Referensi (Dapat otomatis terisi atau diedit manual)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NOMOR REFERENSI / NOTA KOREKSI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate600
                )
                if (reference.isNotBlank()) {
                    Text(
                        text = "Dapat diedit manual",
                        fontSize = 10.5.sp,
                        color = Slate400
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Outlined.Description,
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(18.dp)
                )
                BasicTextField(
                    value = reference,
                    onValueChange = onReferenceChanged,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Slate900),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (reference.isEmpty()) {
                            Text("Pilih nota di atas atau ketik nomor referensi manual...", fontSize = 13.sp, color = Slate400)
                        }
                        innerTextField()
                    }
                )
            }
        }

        // Alasan Penyesuaian
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "ALASAN PENYESUAIAN (AUDIT TRAIL)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate600)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.AutoMirrored.Outlined.List, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp))
                BasicTextField(
                    value = reason,
                    onValueChange = onReasonChanged,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                    textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Slate900, lineHeight = 18.sp),
                    decorationBox = { innerTextField ->
                        if (reason.isEmpty()) {
                            Text("Jelaskan alasan penyesuaian (contoh: Diskon khusus disetujui owner, selisih pembulatan nota lama, retur barang)...", fontSize = 13.sp, color = Slate400, lineHeight = 18.sp)
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
}

@Composable
private fun ActiveInvoiceSelectionDialog(
    customerName: String,
    invoices: List<Receivable>,
    selectedReference: String,
    onSelectInvoice: (Receivable) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp)
                    .fillMaxHeight(0.75f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Slate200),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pilih Nota Piutang Aktif",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Text(
                                text = "Daftar tagihan berjalan milik $customerName",
                                fontSize = 12.sp,
                                color = Slate500
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Slate100)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Slate500, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Slate100)
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(invoices, key = { it.id }) { invoice ->
                            val invoiceRef = invoice.legacyInvoiceNumber?.takeIf(String::isNotBlank)
                                ?: invoice.transactionId?.takeIf(String::isNotBlank)
                                ?: "NOTA-${invoice.id.takeLast(6).uppercase()}"
                            val isSelected = selectedReference == invoiceRef

                            Surface(
                                onClick = {
                                    onSelectInvoice(invoice)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White,
                                border = if (isSelected) {
                                    BorderStroke(1.5.dp, Brand600)
                                } else {
                                    BorderStroke(1.dp, Slate200)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(if (isSelected) Brand50 else Slate100, RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.AutoMirrored.Outlined.ReceiptLong,
                                                contentDescription = null,
                                                tint = if (isSelected) Brand600 else Slate500,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = invoiceRef,
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Brand700 else Slate900
                                                )
                                                if (isSelected) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Brand50,
                                                        border = BorderStroke(0.5.dp, Brand200)
                                                    ) {
                                                        Text(
                                                            text = "Dipilih",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Brand600,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = "Tgl: ${formatIndoDate(invoice.debtDate)} • Jth Tempo: ${formatIndoDate(invoice.dueDate)}",
                                                fontSize = 11.5.sp,
                                                color = Slate500
                                            )
                                        }
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = formatRupiah(invoice.remainingAmount),
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Brand600 else Slate900
                                        )
                                        Text(
                                            text = "Sisa Piutang",
                                            fontSize = 10.5.sp,
                                            color = Slate400
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustmentTimelineInputs(
    debtDate: String,
    dueDate: String,
    tenorDays: Long,
    onPickDebtDate: () -> Unit,
    onPickDueDate: () -> Unit,
    onDueDateChanged: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Dua Kartu Tanggal Bersih & Light (Filter Piutang style)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPickDebtDate() },
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Slate200)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("TGL PENYESUAIAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500)
                        Text(formatIndoDate(debtDate), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
                    }
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp))
                }
            }

            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPickDueDate() },
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Slate200)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("JATUH TEMPO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500)
                        Text(formatIndoDate(dueDate), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Brand600)
                    }
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = Brand600, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Tenor chips (like in Filter Piutang)
        val baseDate = try { LocalDate.parse(debtDate) } catch (e: Exception) { LocalDate.now() }
        val lastDayOfCurrentMonth = baseDate.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth())
        val endOfMonthDate = if (baseDate.isEqual(lastDayOfCurrentMonth)) {
            baseDate.plusMonths(1).with(java.time.temporal.TemporalAdjusters.lastDayOfMonth())
        } else {
            lastDayOfCurrentMonth
        }

        val tenorOptions = listOf(
            "7 Hari" to baseDate.plusDays(7).toString(),
            "14 Hari" to baseDate.plusDays(14).toString(),
            "30 Hari" to baseDate.plusDays(30).toString(),
            "Akhir Bulan" to endOfMonthDate.toString()
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PILIHAN JANGKA WAKTU (TENOR)", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = Slate400)
                Text("Tenor: $tenorDays hari", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Brand600)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tenorOptions.forEach { (label, targetDate) ->
                    val isSelected = dueDate == targetDate
                    Surface(
                        onClick = { onDueDateChanged(targetDate) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) Brand600 else Slate200)
                    ) {
                        Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Brand600 else Slate700,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustmentAccountingSummary(
    selectedCustomer: Customer?,
    currentRemaining: BigDecimal,
    inputAmountStr: String,
    reference: String,
    reason: String,
    dueDate: String,
    tenorDays: Long
) {
    val inputAmount = inputAmountStr.toBigDecimalOrNull() ?: BigDecimal.ZERO

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Slate200)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(8.dp).background(Brand600, CircleShape)
                )
                Text(
                    text = "SIMULASI PENYESUAIAN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate500,
                    letterSpacing = 0.8.sp
                )
            }

            HorizontalDivider(color = Slate100)

            // Pelanggan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Pelanggan:", fontSize = 12.5.sp, color = Slate500)
                Text(
                    text = selectedCustomer?.name ?: "Belum dipilih",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedCustomer != null) Slate900 else Slate400
                )
            }

            // Saldo Saat Ini
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Saldo Aktif Saat Ini:", fontSize = 12.5.sp, color = Slate500)
                Text(formatRupiah(currentRemaining), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
            }

            // Nominal Penyesuaian
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Nominal Adjustment:", fontSize = 12.5.sp, color = Brand700, fontWeight = FontWeight.SemiBold)
                Text(
                    text = formatRupiah(inputAmount),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Brand600
                )
            }

            HorizontalDivider(color = Slate100)

            Surface(
                color = Slate50,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Slate200)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Jatuh Tempo: ${formatIndoDate(dueDate)} ($tenorDays hari)",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate700
                    )
                    if (reference.isNotBlank()) {
                        Text("No. Dokumen: $reference", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate800)
                    }
                    if (reason.isNotBlank()) {
                        Text("Alasan: $reason", fontSize = 11.sp, color = Slate500)
                    }
                }
            }
        }
    }
}
