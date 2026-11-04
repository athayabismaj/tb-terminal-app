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
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tbterminal.app.data.model.Customer
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
fun AdminOpeningReceivableScreen(
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

    LaunchedEffect(customerId) {
        viewModel.initOpeningBalanceForCustomer(customerId)
    }

    LaunchedEffect(uiState.isOpeningBalanceSaved) {
        if (uiState.isOpeningBalanceSaved) {
            viewModel.resetOpeningBalanceSaved()
            onBackToReceivables()
        }
    }

    val selectedCustomer = uiState.customers.firstOrNull { it.id == uiState.openingCustomerId }
    val customerSummary = uiState.customerSummaries.firstOrNull { it.customerId == uiState.openingCustomerId }

    val debtDate = uiState.openingDebtDateInput
    val dueDate = uiState.openingDueDateInput
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
            title = "Pilih Tanggal Catat",
            subtitle = "Tentukan tanggal pencatatan saldo piutang lama",
            onDismiss = { showDebtDatePicker = false },
            onConfirm = { picked ->
                viewModel.onOpeningDebtDateChanged(picked)
                showDebtDatePicker = false
            }
        )
    }

    if (showDueDatePicker) {
        AppDatePickerDialog(
            initialDate = dueDate,
            title = "Pilih Tanggal Jatuh Tempo",
            subtitle = "Tentukan batas waktu pelunasan saldo piutang",
            onDismiss = { showDueDatePicker = false },
            onConfirm = { picked ->
                viewModel.onOpeningDueDateChanged(picked)
                showDueDatePicker = false
            }
        )
    }

    if (showCustomerPickerDialog) {
        CustomerSelectionDialog(
            customerRepository = customerRepository,
            selectedCustomerId = uiState.openingCustomerId,
            onCustomerSelected = { customer ->
                viewModel.onOpeningCustomerSelected(customer)
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
        activeDestination = AdminDestination.OpeningReceivableForm,
        pageTitle = "Isi Saldo Awal Piutang",
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (isWideScreen) 24.dp else 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Banner Informasi Bersih & Jelas
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
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = Brand600, modifier = Modifier.size(22.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pencatatan Saldo Awal Buku Piutang",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Gunakan formulir ini untuk memigrasi saldo utang lama pelanggan dari buku manual atau nota fisik terdahulu.",
                                fontSize = 12.sp,
                                color = Slate500,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1.3f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CardSection(title = "1. INFORMASI PELANGGAN", required = true) {
                                CustomerCardSelector(
                                    selectedCustomer = selectedCustomer,
                                    onOpenPicker = { showCustomerPickerDialog = true },
                                    onClearCustomer = { viewModel.onOpeningCustomerChanged("") }
                                )
                            }

                            CardSection(title = "2. NOMINAL & NOMOR NOTA LAMA", required = true) {
                                AmountAndLegacyInvoiceInputs(
                                    amountInput = uiState.openingAmountInput,
                                    legacyInvoiceInput = uiState.openingLegacyInvoiceInput,
                                    onAmountChanged = viewModel::onOpeningAmountChanged,
                                    onLegacyInvoiceChanged = viewModel::onOpeningLegacyInvoiceChanged
                                )
                            }

                            CardSection(title = "3. JADWAL & JATUH TEMPO", required = true) {
                                TimelineInputs(
                                    debtDate = debtDate,
                                    dueDate = dueDate,
                                    tenorDays = tenorDays,
                                    onPickDebtDate = { showDebtDatePicker = true },
                                    onPickDueDate = { showDueDatePicker = true },
                                    onDueDateChanged = viewModel::onOpeningDueDateChanged
                                )
                            }

                            CardSection(title = "4. CATATAN & KETERANGAN (OPSIONAL)") {
                                NotesInput(
                                    notes = uiState.openingNotesInput,
                                    onNotesChanged = viewModel::onOpeningNotesChanged
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(0.7f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OpeningBalanceAccountingSummary(
                                selectedCustomer = selectedCustomer,
                                currentRemaining = customerSummary?.totalRemaining ?: BigDecimal.ZERO,
                                inputAmountStr = uiState.openingAmountInput,
                                dueDate = dueDate,
                                tenorDays = tenorDays,
                                legacyInvoice = uiState.openingLegacyInvoiceInput
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CardSection(title = "1. INFORMASI PELANGGAN", required = true) {
                            CustomerCardSelector(
                                selectedCustomer = selectedCustomer,
                                onOpenPicker = { showCustomerPickerDialog = true },
                                onClearCustomer = { viewModel.onOpeningCustomerChanged("") }
                            )
                        }

                        CardSection(title = "2. NOMINAL & NOMOR NOTA LAMA", required = true) {
                            AmountAndLegacyInvoiceInputs(
                                amountInput = uiState.openingAmountInput,
                                legacyInvoiceInput = uiState.openingLegacyInvoiceInput,
                                onAmountChanged = viewModel::onOpeningAmountChanged,
                                onLegacyInvoiceChanged = viewModel::onOpeningLegacyInvoiceChanged
                            )
                        }

                        CardSection(title = "3. JADWAL & JATUH TEMPO", required = true) {
                            TimelineInputs(
                                debtDate = debtDate,
                                dueDate = dueDate,
                                tenorDays = tenorDays,
                                onPickDebtDate = { showDebtDatePicker = true },
                                onPickDueDate = { showDueDatePicker = true },
                                onDueDateChanged = viewModel::onOpeningDueDateChanged
                            )
                        }

                        CardSection(title = "4. CATATAN & KETERANGAN (OPSIONAL)") {
                            NotesInput(
                                notes = uiState.openingNotesInput,
                                onNotesChanged = viewModel::onOpeningNotesChanged
                            )
                        }

                        OpeningBalanceAccountingSummary(
                            selectedCustomer = selectedCustomer,
                            currentRemaining = customerSummary?.totalRemaining ?: BigDecimal.ZERO,
                            inputAmountStr = uiState.openingAmountInput,
                            dueDate = dueDate,
                            tenorDays = tenorDays,
                            legacyInvoice = uiState.openingLegacyInvoiceInput
                        )
                    }
                }
            }

            // Sticky Bottom Actions (Transparent Background)
            Surface(
                color = Color.Transparent,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (isWideScreen) 24.dp else 16.dp, vertical = 14.dp),
                    horizontalArrangement = if (isWideScreen) Arrangement.End else Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onBackToReceivables,
                        modifier = if (isWideScreen) Modifier.width(120.dp).height(48.dp) else Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Slate300),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Slate700
                        )
                    ) {
                        Text("Batal", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                    }

                    val canSubmit = uiState.openingCustomerId.isNotBlank() &&
                        uiState.openingAmountInput.isNotBlank() &&
                        !uiState.isSubmittingOpeningBalance

                    Button(
                        onClick = viewModel::submitOpeningBalance,
                        enabled = canSubmit,
                        modifier = if (isWideScreen) Modifier.width(180.dp).height(48.dp) else Modifier.weight(1.5f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Brand600,
                            disabledContainerColor = Slate200
                        )
                    ) {
                        if (uiState.isSubmittingOpeningBalance) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
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
                    Text("Cari pelanggan untuk pencatatan saldo awal", fontSize = 11.5.sp, color = Slate400)
                }
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Slate400, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun AmountAndLegacyInvoiceInputs(
    amountInput: String,
    legacyInvoiceInput: String,
    onAmountChanged: (String) -> Unit,
    onLegacyInvoiceChanged: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Nominal Box
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "NOMINAL SALDO AWAL PIUTANG", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate600)
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

        // Quick Amount Chips (Clear & Light like Filter Piutang)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "PILIHAN NOMINAL CEPAT", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = Slate400)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val chips = listOf(
                    "500 rb" to "500000",
                    "1 jt" to "1000000",
                    "2.5 jt" to "2500000",
                    "5 jt" to "5000000",
                    "10 jt" to "10000000"
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

        // Nomor Nota Fisik Lama
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = "NOMOR NOTA / FAKTUR FISIK LAMA (OPSIONAL)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate600)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp))
                BasicTextField(
                    value = legacyInvoiceInput,
                    onValueChange = onLegacyInvoiceChanged,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Slate900),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (legacyInvoiceInput.isEmpty()) {
                            Text("Contoh: NOTA-MANUAL-2024-089", fontSize = 13.sp, color = Slate400)
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
}

@Composable
private fun TimelineInputs(
    debtDate: String,
    dueDate: String,
    tenorDays: Long,
    onPickDebtDate: () -> Unit,
    onPickDueDate: () -> Unit,
    onDueDateChanged: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Dua Kartu Tanggal Bersih & Light
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
                        Text("TGL AWAL PIUTANG", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate500)
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
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = Brand600, modifier = Modifier.size(18.dp))
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
private fun NotesInput(
    notes: String,
    onNotesChanged: (String) -> Unit
) {
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
            value = notes,
            onValueChange = onNotesChanged,
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
            textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Slate900, lineHeight = 18.sp),
            decorationBox = { innerTextField ->
                if (notes.isEmpty()) {
                    Text("Tambahkan catatan riwayat bon manual, nama proyek, atau keterangan lainnya...", fontSize = 13.sp, color = Slate400, lineHeight = 18.sp)
                }
                innerTextField()
            }
        )
    }
}

@Composable
private fun OpeningBalanceAccountingSummary(
    selectedCustomer: Customer?,
    currentRemaining: BigDecimal,
    inputAmountStr: String,
    dueDate: String,
    tenorDays: Long,
    legacyInvoice: String
) {
    val inputAmount = inputAmountStr.toBigDecimalOrNull() ?: BigDecimal.ZERO
    val projectedTotal = currentRemaining + inputAmount

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
                    modifier = Modifier
                        .size(8.dp)
                        .background(Brand600, CircleShape)
                )
                Text(
                    text = "SIMULASI BUKU PIUTANG",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate500,
                    letterSpacing = 0.8.sp
                )
            }

            HorizontalDivider(color = Slate100)

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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Saldo Piutang Saat Ini:", fontSize = 12.5.sp, color = Slate500)
                Text(formatRupiah(currentRemaining), fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Saldo Awal Tambahan:", fontSize = 12.5.sp, color = Brand700, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "(+) ${formatRupiah(inputAmount)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Brand600
                )
            }

            HorizontalDivider(color = Slate100)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Piutang Baru", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate900)
                    Text("Setelah saldo awal dicatat", fontSize = 10.5.sp, color = Slate400)
                }
                Text(
                    text = formatRupiah(projectedTotal),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Brand600
                )
            }

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
                    if (legacyInvoice.isNotBlank()) {
                        Text(
                            text = "Referensi Nota: $legacyInvoice",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }
            }
        }
    }
}
