package com.tbterminal.app.ui.receivables

import com.tbterminal.app.ui.common.parseWholeRupiahOrExactRemainingInput
import com.tbterminal.app.ui.common.toWholeRupiahInputText

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.Receivable
import java.math.BigDecimal

private val BrandGreen = Color(0xFF10533C)
private val BrandGreenLight = Color(0xFFE8F5E9)
private val TextDark = Color(0xFF0F172A)
private val TextMuted = Color(0xFF64748B)
private val BorderColor = Color(0xFFE2E8F0)
private val ErrorRed = Color(0xFFEF4444)

@Composable
internal fun ReceivablePaymentDialog(
    receivable: Receivable,
    uiState: ReceivableUiState,
    onAmountChanged: (String) -> Unit,
    onMethodChanged: (ReceivablePaymentMethod) -> Unit,
    onReferenceChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    ReceivablePaymentDialog(
        receivable = receivable,
        paymentAmountInput = uiState.paymentAmountInput,
        paymentMethod = uiState.paymentMethod,
        referenceInput = uiState.referenceInput,
        notesInput = uiState.notesInput,
        isSubmittingPayment = uiState.isSubmittingPayment,
        errorMessage = uiState.errorMessage,
        onAmountChanged = onAmountChanged,
        onMethodChanged = onMethodChanged,
        onReferenceChanged = onReferenceChanged,
        onNotesChanged = onNotesChanged,
        onDismiss = onDismiss,
        onSubmit = onSubmit
    )
}

@Composable
internal fun ReceivablePaymentDialog(
    receivable: Receivable,
    paymentAmountInput: String,
    paymentMethod: ReceivablePaymentMethod,
    referenceInput: String,
    notesInput: String,
    isSubmittingPayment: Boolean,
    errorMessage: String?,
    onAmountChanged: (String) -> Unit,
    onMethodChanged: (ReceivablePaymentMethod) -> Unit,
    onReferenceChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    ReceivablePaymentDialog(
        targetCustomerName = receivable.customerName,
        referenceLabel = "Nota: ${receivable.mobileReference()}",
        totalRemaining = receivable.remainingAmount,
        isPayAll = false,
        invoiceCount = 1,
        paymentAmountInput = paymentAmountInput,
        paymentMethod = paymentMethod,
        referenceInput = referenceInput,
        notesInput = notesInput,
        isSubmittingPayment = isSubmittingPayment,
        errorMessage = errorMessage,
        onAmountChanged = onAmountChanged,
        onMethodChanged = onMethodChanged,
        onReferenceChanged = onReferenceChanged,
        onNotesChanged = onNotesChanged,
        onDismiss = onDismiss,
        onSubmit = onSubmit
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReceivablePayAllDialog(
    customerName: String,
    unpaidReceivables: List<Receivable>,
    selectedInvoiceIds: Set<String>,
    paymentAmountInput: String,
    paymentMethod: ReceivablePaymentMethod,
    referenceInput: String,
    notesInput: String,
    isSubmittingPayment: Boolean,
    errorMessage: String?,
    onToggleInvoice: (String) -> Unit,
    onSelectAllInvoices: (Boolean) -> Unit,
    onAmountChanged: (String) -> Unit,
    onMethodChanged: (ReceivablePaymentMethod) -> Unit,
    onReferenceChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    val selectedInvoices = remember(unpaidReceivables, selectedInvoiceIds) {
        unpaidReceivables.filter { it.id in selectedInvoiceIds }
    }
    val selectedTotalRemaining = remember(selectedInvoices) {
        selectedInvoices.fold(BigDecimal.ZERO) { sum, it -> sum + it.remainingAmount }
    }
    val payAmount = parseWholeRupiahOrExactRemainingInput(paymentAmountInput, selectedTotalRemaining)
        ?: BigDecimal.ZERO
    val allocations = remember(unpaidReceivables, selectedInvoiceIds, payAmount) {
        calculateFifoAllocations(unpaidReceivables, selectedInvoiceIds, payAmount)
    }
    val paymentsToMakeCount = remember(allocations) {
        allocations.count { it.isSelected && it.allocatedAmount > BigDecimal.ZERO }
    }
    val isAllSelected = unpaidReceivables.isNotEmpty() && selectedInvoiceIds.size >= unpaidReceivables.size

    ModalBottomSheet(
        onDismissRequest = { if (!isSubmittingPayment) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        scrimColor = Color(0xFF0F172A).copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("receivable-pay-all-sheet")
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar (Standard Industri POS)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Bayar Semua Piutang",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = "Pelunasan multi-nota piutang pelanggan",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    enabled = !isSubmittingPayment
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Tutup",
                        tint = TextMuted
                    )
                }
            }

            HorizontalDivider(color = BorderColor, thickness = 1.dp)

            // Context Card: Ringkasan Pelanggan & Seleksi
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = customerName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${selectedInvoices.size} dari ${unpaidReceivables.size} nota dipilih",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total Dipilih",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = selectedTotalRemaining.currencyText(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (selectedTotalRemaining > BigDecimal.ZERO) Color(0xFFDC2626) else BrandGreen
                        )
                    }
                }
            }

            // Error Alert Box
            if (!errorMessage.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFEF2F2))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = errorMessage,
                        color = ErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Section: Daftar Rincian Nota Piutang
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rincian Nota Piutang (${unpaidReceivables.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )

                    Surface(
                        onClick = { onSelectAllInvoices(!isAllSelected) },
                        shape = RoundedCornerShape(6.dp),
                        color = if (isAllSelected) Color(0xFFF1F5F9) else BrandGreenLight
                    ) {
                        Text(
                            text = if (isAllSelected) "Batal Pilih Semua" else "Pilih Semua (${unpaidReceivables.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isAllSelected) TextMuted else BrandGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        allocations.forEachIndexed { index, allocation ->
                            val invoice = allocation.receivable
                            val isChecked = allocation.isSelected

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isChecked) Color.Transparent else Color(0xFFF8FAFC).copy(alpha = 0.5f))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { onToggleInvoice(invoice.id) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = BrandGreen,
                                        uncheckedColor = TextMuted.copy(alpha = 0.6f)
                                    ),
                                    modifier = Modifier.size(24.dp)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = invoice.mobileReference(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isChecked) TextDark else TextMuted
                                        )

                                        Text(
                                            text = invoice.remainingAmount.currencyText(),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (isChecked) TextDark else TextMuted
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Jatuh tempo: ${invoice.dueDate.simpleDate()}",
                                            fontSize = 11.5.sp,
                                            color = TextMuted
                                        )

                                        // Status Allocation Badge
                                        when {
                                            !allocation.isSelected -> {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFF1F5F9)
                                                ) {
                                                    Text(
                                                        text = "TIDAK DIPILIH",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = TextMuted,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            allocation.isFullyPaid -> {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFDCFCE7)
                                                ) {
                                                    Text(
                                                        text = "✓ LUNAS",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF166534),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            allocation.isPartiallyPaid -> {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFFEF3C7)
                                                ) {
                                                    Text(
                                                        text = "SEBAGIAN (${allocation.allocatedAmount.currencyText()})",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF92400E),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            else -> {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFF8FAFC),
                                                    border = BorderStroke(1.dp, BorderColor)
                                                ) {
                                                    Text(
                                                        text = "BELUM TERALOKASI",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = TextMuted,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (index < allocations.lastIndex) {
                                HorizontalDivider(
                                    color = BorderColor.copy(alpha = 0.5f),
                                    thickness = 1.dp
                                )
                            }
                        }
                    }
                }
            }

            // Field Nominal Pembayaran
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nominal Pembayaran *",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )

                    Surface(
                        onClick = { onAmountChanged(selectedTotalRemaining.toInputText()) },
                        shape = RoundedCornerShape(8.dp),
                        color = BrandGreenLight,
                        border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Bayar Penuh",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandGreen
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = paymentAmountInput,
                    onValueChange = onAmountChanged,
                    prefix = {
                        Text(
                            text = "Rp",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextDark
                    ),
                    placeholder = { Text("0", fontSize = 16.sp, fontFamily = FontFamily.Monospace, color = TextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandGreen,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    )
                )

                if (payAmount > BigDecimal.ZERO && payAmount < selectedTotalRemaining) {
                    val sisa = selectedTotalRemaining - payAmount
                    Text(
                        text = "Dialokasikan ke nota terlama dahulu (FIFO). Sisa belum dibayar: ${sisa.currencyText()}",
                        fontSize = 11.5.sp,
                        color = Color(0xFFD97706),
                        modifier = Modifier.padding(start = 2.dp)
                    )
                } else if (payAmount >= selectedTotalRemaining && selectedTotalRemaining > BigDecimal.ZERO) {
                    Text(
                        text = "✓ Seluruh ${selectedInvoices.size} nota terpilih akan lunas penuh",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = BrandGreen,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }

            // Field Metode Pembayaran
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Metode Pembayaran",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )

                PaymentMethodRow(
                    selected = paymentMethod,
                    onSelect = onMethodChanged
                )
            }

            // Field No. Referensi / Bukti Transfer (Opsional jika Transfer / QRIS)
            if (paymentMethod != ReceivablePaymentMethod.Cash) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "No. Referensi Transfer / QRIS (Opsional)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                    OutlinedTextField(
                        value = referenceInput,
                        onValueChange = onReferenceChanged,
                        placeholder = { Text("Contoh: Trx ID #987654", color = TextMuted, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Receipt, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandGreen,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )
                }
            }

            // Field Catatan Pembayaran (Opsional)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Catatan Pembayaran (Opsional)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                OutlinedTextField(
                    value = notesInput,
                    onValueChange = onNotesChanged,
                    placeholder = {
                        Text(
                            text = "Keterangan pembayaran multi-nota / catatan kasir",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.EditNote, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandGreen,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Button(
                onClick = onSubmit,
                enabled = !isSubmittingPayment && payAmount > BigDecimal.ZERO && paymentsToMakeCount > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
            ) {
                if (isSubmittingPayment) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("MEMPROSES PEMBAYARAN...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        Icons.Outlined.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val actualPayAmount = payAmount.min(selectedTotalRemaining)
                    Text(
                        text = "BAYAR $paymentsToMakeCount NOTA (${actualPayAmount.currencyText()})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            ) {
                Text(
                    text = "Batal",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReceivablePaymentDialog(
    targetCustomerName: String,
    referenceLabel: String,
    totalRemaining: BigDecimal,
    isPayAll: Boolean = false,
    invoiceCount: Int = 1,
    paymentAmountInput: String,
    paymentMethod: ReceivablePaymentMethod,
    referenceInput: String,
    notesInput: String,
    isSubmittingPayment: Boolean,
    errorMessage: String?,
    onAmountChanged: (String) -> Unit,
    onMethodChanged: (ReceivablePaymentMethod) -> Unit,
    onReferenceChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    val payAmount = parseWholeRupiahOrExactRemainingInput(paymentAmountInput, totalRemaining) ?: BigDecimal.ZERO

    ModalBottomSheet(
        onDismissRequest = { if (!isSubmittingPayment) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        scrimColor = Color(0xFF0F172A).copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("receivable-payment-sheet")
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar (Matching QuickAddCustomerSheet)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isPayAll) "Bayar Semua Piutang" else "Bayar Piutang",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = if (isPayAll) "Pelunasan seluruh nota piutang pelanggan" else "Catat penerimaan pembayaran nota tagihan",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    enabled = !isSubmittingPayment
                ) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Tutup",
                        tint = TextMuted
                    )
                }
            }

            HorizontalDivider(color = BorderColor, thickness = 1.dp)

            // Context Card: Ringkas & Bersih (Standard Industri POS)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = targetCustomerName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = referenceLabel,
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isPayAll) "Total Tagihan" else "Sisa Tagihan",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = totalRemaining.currencyText(),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }

            // Error Alert Box (Matching QuickAddCustomerSheet)
            if (!errorMessage.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFEF2F2))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = errorMessage,
                        color = ErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Field Nominal Pembayaran
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nominal Pembayaran *",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )

                    Surface(
                        onClick = { onAmountChanged(totalRemaining.toInputText()) },
                        shape = RoundedCornerShape(8.dp),
                        color = BrandGreenLight,
                        border = BorderStroke(1.dp, BrandGreen.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = BrandGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Bayar Penuh",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandGreen
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = paymentAmountInput,
                    onValueChange = onAmountChanged,
                    prefix = {
                        Text(
                            text = "Rp",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextDark
                    ),
                    placeholder = { Text("0", fontSize = 16.sp, fontFamily = FontFamily.Monospace, color = TextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandGreen,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    )
                )

                if (payAmount > BigDecimal.ZERO && payAmount < totalRemaining) {
                    val sisa = totalRemaining - payAmount
                    Text(
                        text = if (isPayAll) {
                            "Dialokasikan ke nota terlama dahulu (FIFO). Sisa total: ${sisa.currencyText()}"
                        } else {
                            "Sisa tagihan setelah pembayaran: ${sisa.currencyText()}"
                        },
                        fontSize = 11.5.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                } else if (payAmount >= totalRemaining && totalRemaining > BigDecimal.ZERO) {
                    Text(
                        text = if (isPayAll) {
                            "✓ Semua nota ($invoiceCount nota) akan lunas penuh"
                        } else {
                            "✓ Tagihan akan lunas penuh"
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = BrandGreen,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }

            // Field Metode Pembayaran
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Metode Pembayaran",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )

                PaymentMethodRow(
                    selected = paymentMethod,
                    onSelect = onMethodChanged
                )
            }

            // Field No. Referensi / Bukti Transfer (Opsional jika Transfer / QRIS)
            if (paymentMethod != ReceivablePaymentMethod.Cash) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "No. Referensi Transfer / QRIS (Opsional)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                    OutlinedTextField(
                        value = referenceInput,
                        onValueChange = onReferenceChanged,
                        placeholder = { Text("Contoh: Trx ID #987654", color = TextMuted, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Receipt, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandGreen,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )
                }
            }

            // Field Catatan Pembayaran (Opsional)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Catatan Pembayaran (Opsional)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                OutlinedTextField(
                    value = notesInput,
                    onValueChange = onNotesChanged,
                    placeholder = {
                        Text(
                            text = if (isPayAll) "Keterangan pembayaran borongan / catatan kasir" else "Keterangan pembayaran / catatan kasir",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.EditNote, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandGreen,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons (Matching QuickAddCustomerSheet)
            Button(
                onClick = onSubmit,
                enabled = !isSubmittingPayment && payAmount > BigDecimal.ZERO,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
            ) {
                if (isSubmittingPayment) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("MEMPROSES PEMBAYARAN...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        Icons.Outlined.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPayAll) "BAYAR SEMUA NOTA ($invoiceCount)" else "SIMPAN PEMBAYARAN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            ) {
                Text(
                    text = "Batal",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun PaymentMethodRow(
    selected: ReceivablePaymentMethod,
    onSelect: (ReceivablePaymentMethod) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReceivablePaymentMethod.entries.forEach { method ->
            val isSelected = method == selected
            val icon: ImageVector = when (method) {
                ReceivablePaymentMethod.Cash -> Icons.Outlined.Payments
                ReceivablePaymentMethod.Transfer -> Icons.Outlined.AccountBalance
                ReceivablePaymentMethod.Qris -> Icons.Outlined.QrCode
            }

            Surface(
                onClick = { onSelect(method) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) BrandGreenLight else Color(0xFFF8FAFC),
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) BrandGreen else BorderColor
                ),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 11.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) BrandGreen else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = method.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) BrandGreen else TextDark
                    )
                }
            }
        }
    }
}

private fun BigDecimal.toInputText(): String = toWholeRupiahInputText()
