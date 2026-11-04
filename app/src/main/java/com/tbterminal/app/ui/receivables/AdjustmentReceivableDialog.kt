package com.tbterminal.app.ui.receivables

import com.tbterminal.app.ui.components.AppDatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.data.repository.CustomerRepository
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val Brand = Color(0xFF256B57)
private val Brand50 = Color(0xFFF0F6F4)
private val Brand100 = Color(0xFFE8F0EC)
private val Brand200 = Color(0xFFC7DCD4)
private val Gray100 = Color(0xFFF3F4F6)
private val Gray200 = Color(0xFFE5E7EB)
private val Gray300 = Color(0xFFD1D5DB)
private val Gray400 = Color(0xFF9CA3AF)
private val Gray500 = Color(0xFF6B7280)
private val Gray700 = Color(0xFF374151)
private val Gray800 = Color(0xFF1F2937)
private val Gray900 = Color(0xFF111827)

@Composable
private fun FieldLabel(text: String, required: Boolean = false, optional: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = text.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Gray700,
            letterSpacing = 0.5.sp
        )
        if (required) {
            Text("*", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
        }
        if (optional) {
            Text("(opsional)", fontSize = 11.sp, color = Gray400, fontWeight = FontWeight.Normal)
        }
    }
}

private fun formatIndonesianDate(dateStr: String): String {
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
internal fun AdjustmentReceivableDialog(
    customerRepository: CustomerRepository,
    uiState: ReceivableUiState,
    onCustomerChanged: (String) -> Unit,
    onCustomerSelected: ((Customer) -> Unit)? = null,
    onAmountChanged: (String) -> Unit,
    onDebtDateChanged: (String) -> Unit,
    onDueDateChanged: (String) -> Unit,
    onReferenceChanged: (String) -> Unit,
    onReasonChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(SheetCustomerStep.FORM) }
    var localSelectedCustomer by remember { mutableStateOf<Customer?>(null) }
    val effectiveCustomer = localSelectedCustomer ?: uiState.customers.firstOrNull { it.id == uiState.adjustmentCustomerId }

    BackHandler(enabled = currentStep != SheetCustomerStep.FORM) {
        currentStep = when (currentStep) {
            SheetCustomerStep.ADD_CUSTOMER -> SheetCustomerStep.SELECT_CUSTOMER
            SheetCustomerStep.SELECT_CUSTOMER -> SheetCustomerStep.FORM
            SheetCustomerStep.FORM -> SheetCustomerStep.FORM
        }
    }

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

    if (showDebtDatePicker) {
        AppDatePickerDialog(
            initialDate = debtDate,
            title = "Pilih Tanggal Penyesuaian",
            subtitle = "Tentukan tanggal pencatatan penyesuaian piutang",
            onDismiss = { showDebtDatePicker = false },
            onConfirm = { picked ->
                onDebtDateChanged(picked)
                showDebtDatePicker = false
            }
        )
    }

    if (showDueDatePicker) {
        AppDatePickerDialog(
            initialDate = dueDate,
            title = "Pilih Tanggal Jatuh Tempo",
            subtitle = "Tentukan batas waktu pelunasan piutang",
            onDismiss = { showDueDatePicker = false },
            onConfirm = { picked ->
                onDueDateChanged(picked)
                showDueDatePicker = false
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = { if (!uiState.isSubmittingAdjustment) onDismiss() },
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState.ordinal > initialState.ordinal) {
                    (slideInHorizontally(animationSpec = tween(280)) { width -> width } + fadeIn(animationSpec = tween(280)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(280)) { width -> -width / 4 } + fadeOut(animationSpec = tween(200)))
                } else {
                    (slideInHorizontally(animationSpec = tween(280)) { width -> -width / 4 } + fadeIn(animationSpec = tween(280)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(280)) { width -> width } + fadeOut(animationSpec = tween(200)))
                }
            },
            label = "AdjustmentSheetStepTransition"
        ) { step ->
            when (step) {
                SheetCustomerStep.FORM -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
            // Sheet Header with Mode Switcher & Close Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .background(Gray100, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Gray500,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Penyesuaian Piutang",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Gray900,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Sesuaikan nilai piutang pelanggan untuk koreksi atau diskon.",
                    fontSize = 12.sp,
                    color = Gray500,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            HorizontalDivider(color = Gray100)

            // Scrollable Form Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Pelanggan (In-Sheet Selection Flow - Option 2)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Pelanggan", required = true)
                    CustomerCardSelector(
                        selectedCustomer = effectiveCustomer,
                        onOpenPicker = { currentStep = SheetCustomerStep.SELECT_CUSTOMER },
                        onClearCustomer = {
                            localSelectedCustomer = null
                            onCustomerChanged("")
                        }
                    )
                }

                // 2. Nominal Piutang
                val isAmountError = uiState.errorMessage != null
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Nominal Piutang", required = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isAmountError) MaterialTheme.colorScheme.error.copy(alpha = 0.05f) else Color.White, RoundedCornerShape(12.dp))
                            .border(1.5.dp, if (isAmountError) MaterialTheme.colorScheme.error else Brand.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Rp",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = if (isAmountError) MaterialTheme.colorScheme.error else Brand
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        BasicTextField(
                            value = uiState.adjustmentAmountInput,
                            onValueChange = onAmountChanged,
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Gray900,
                                letterSpacing = (-0.5).sp
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (uiState.adjustmentAmountInput.isEmpty()) {
                                        Text("0", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Gray300)
                                    }
                                    innerTextField()
                                }
                            }
                        )
                        if (uiState.adjustmentAmountInput.isNotEmpty()) {
                            IconButton(
                                onClick = { onAmountChanged("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Gray400, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    if (isAmountError) {
                        Text(
                            text = uiState.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }

                // 3. Smart Timeline / Dual Date Picker Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brand50.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                        .border(1.dp, Brand100.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header with Tenor Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = null, tint = Brand, modifier = Modifier.size(16.dp))
                            Text(
                                text = "JADWAL & TENOR PEMBAYARAN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Gray800,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "Tenor $tenorDays Hari",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Brand
                        )
                    }

                    // Dual Date Cards with Connector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Card 1: TGL CATAT
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .border(1.dp, Brand.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .clickable { showDebtDatePicker = true }
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("TGL CATAT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Gray500, letterSpacing = 0.5.sp)
                                Icon(Icons.Default.DateRange, contentDescription = null, tint = Brand, modifier = Modifier.size(14.dp))
                            }
                            Text(
                                text = formatIndonesianDate(debtDate),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Gray900
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Hari ini",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Brand
                                )
                                Text("Ubah", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Gray400)
                            }
                        }

                        // Flow Indicator Arrow
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Color.White, CircleShape)
                                .border(1.dp, Brand200, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Brand, modifier = Modifier.size(12.dp))
                        }

                        // Card 2: JATUH TEMPO
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Brand50.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .border(1.5.dp, Brand.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable { showDueDatePicker = true }
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("JATUH TEMPO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Brand, letterSpacing = 0.5.sp)
                                Icon(Icons.Default.Notifications, contentDescription = null, tint = Brand, modifier = Modifier.size(14.dp))
                            }
                            Text(
                                text = formatIndonesianDate(dueDate),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Brand
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Sisa $tenorDays hari", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Gray700)
                                Text("Kalender", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Brand, textDecoration = TextDecoration.Underline)
                            }
                        }
                    }

                    // Quick Chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Pilihan cepat jatuh tempo:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Gray700)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Chip 1: +7 Hari
                            val is7 = tenorDays == 7L
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (is7) Brand else Color.White)
                                    .border(1.dp, if (is7) Brand else Gray200, RoundedCornerShape(8.dp))
                                    .clickable {
                                        val base = try { LocalDate.parse(debtDate) } catch (e: Exception) { LocalDate.now() }
                                        onDueDateChanged(base.plusDays(7).toString())
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+7 Hari",
                                    fontSize = 11.sp,
                                    fontWeight = if (is7) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (is7) Color.White else Gray700
                                )
                            }

                            // Chip 2: +14 Hari
                            val is14 = tenorDays == 14L
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (is14) Brand else Color.White)
                                    .border(1.dp, if (is14) Brand else Gray200, RoundedCornerShape(8.dp))
                                    .clickable {
                                        val base = try { LocalDate.parse(debtDate) } catch (e: Exception) { LocalDate.now() }
                                        onDueDateChanged(base.plusDays(14).toString())
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+14 Hari",
                                    fontSize = 11.sp,
                                    fontWeight = if (is14) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (is14) Color.White else Gray700
                                )
                            }

                            // Chip 3: +30 Hari
                            val is30 = tenorDays == 30L
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (is30) Brand else Color.White)
                                    .border(1.dp, if (is30) Brand else Gray200, RoundedCornerShape(8.dp))
                                    .clickable {
                                        val base = try { LocalDate.parse(debtDate) } catch (e: Exception) { LocalDate.now() }
                                        onDueDateChanged(base.plusDays(30).toString())
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+30 Hari",
                                    fontSize = 11.sp,
                                    fontWeight = if (is30) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (is30) Color.White else Gray700
                                )
                            }

                            // Chip 4: Akhir Bulan
                            val isEom = remember(dueDate) {
                                try {
                                    val d = LocalDate.parse(dueDate)
                                    d.dayOfMonth == d.lengthOfMonth()
                                } catch (e: Exception) {
                                    false
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isEom) Brand else Color.White)
                                    .border(1.dp, if (isEom) Brand else Gray200, RoundedCornerShape(8.dp))
                                    .clickable {
                                        val base = try { LocalDate.parse(debtDate) } catch (e: Exception) { LocalDate.now() }
                                        onDueDateChanged(base.withDayOfMonth(base.lengthOfMonth()).toString())
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Akhir Bulan",
                                    fontSize = 11.sp,
                                    fontWeight = if (isEom) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isEom) Color.White else Gray700
                                )
                            }
                        }
                    }
                }

                // 4. Referensi
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Referensi", optional = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(1.dp, Gray200, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null, tint = Gray400, modifier = Modifier.size(18.dp))
                        BasicTextField(
                            value = uiState.adjustmentReferenceInput,
                            onValueChange = onReferenceChanged,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Gray900
                            ),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (uiState.adjustmentReferenceInput.isEmpty()) {
                                        Text(
                                            text = "Contoh: ADJ-2024-001",
                                            color = Gray400,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }

                // 5. Alasan
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Alasan", optional = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .border(1.dp, Gray200, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.List,
                            contentDescription = null,
                            tint = Gray400,
                            modifier = Modifier.padding(top = 1.dp).size(18.dp)
                        )
                        BasicTextField(
                            value = uiState.adjustmentReasonInput,
                            onValueChange = onReasonChanged,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Gray900,
                                lineHeight = 18.sp
                            ),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.TopStart
                                ) {
                                    if (uiState.adjustmentReasonInput.isEmpty()) {
                                        Text(
                                            text = "Tambahkan keterangan alasan penyesuaian piutang...",
                                            color = Gray400,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            lineHeight = 18.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }
            }

            // Sticky Footer Actions
            HorizontalDivider(color = Gray100)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, Gray300, RoundedCornerShape(12.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "BATAL",
                        color = Gray700,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Button(
                    onClick = onSubmit,
                    modifier = Modifier
                        .weight(2f)
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand),
                    enabled = !uiState.isSubmittingAdjustment
                ) {
                    if (uiState.isSubmittingAdjustment) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(
                                text = "SIMPAN PENYESUAIAN",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
    SheetCustomerStep.SELECT_CUSTOMER -> {
        CustomerPickerSheetView(
            customerRepository = customerRepository,
            selectedCustomerId = uiState.adjustmentCustomerId,
            onBackToForm = { currentStep = SheetCustomerStep.FORM },
            onClose = onDismiss,
            onNavigateToAddCustomer = { currentStep = SheetCustomerStep.ADD_CUSTOMER },
            onCustomerSelected = { customer ->
                localSelectedCustomer = customer
                onCustomerChanged(customer.id)
                onCustomerSelected?.invoke(customer)
                currentStep = SheetCustomerStep.FORM
            }
        )
    }
    SheetCustomerStep.ADD_CUSTOMER -> {
        AddCustomerSheetView(
            customerRepository = customerRepository,
            onBackToPicker = { currentStep = SheetCustomerStep.SELECT_CUSTOMER },
            onClose = onDismiss,
            onCustomerCreated = { newCustomer ->
                localSelectedCustomer = newCustomer
                onCustomerChanged(newCustomer.id)
                onCustomerSelected?.invoke(newCustomer)
                currentStep = SheetCustomerStep.FORM
            }
        )
    }
}
}
}
}

