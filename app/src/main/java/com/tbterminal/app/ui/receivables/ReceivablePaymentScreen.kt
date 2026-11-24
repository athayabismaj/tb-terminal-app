package com.tbterminal.app.ui.receivables

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
private val BrandGreenLight = Color(0xFFF0FDF4)
private val Slate50 = Color(0xFFF8FAFC)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate400 = Color(0xFF94A3B8)
private val Slate500 = Color(0xFF64748B)
private val Slate600 = Color(0xFF475569)
private val Slate700 = Color(0xFF334155)
private val Slate800 = Color(0xFF1E293B)
private val Slate900 = Color(0xFF0F172A)

private val Emerald50 = Color(0xFFECFDF5)
private val Emerald100 = Color(0xFFD1FAE5)
private val Emerald600 = Color(0xFF059669)
private val Emerald700 = Color(0xFF047857)
private val Emerald800 = Color(0xFF065F46)
private val Amber700 = Color(0xFFB45309)
private val Rose500 = Color(0xFFF43F5E)

/**
 * Dedicated Full Screen Page untuk Bayar Piutang (Multi-Nota).
 * Mengikuti desain persis dari HTML Mockup Kasira POS:
 * - App Header dengan tombol Back & Judul "Bayar Piutang"
 * - Customer Summary Card (Avatar initial, nama pelanggan, counter nota, total tagihan)
 * - Rincian Nota Piutang (Daftar nota dengan checkbox, no nota monospace, jatuh tempo, sisa tagihan, badge status)
 * - Nominal Pembayaran (Tombol "Bayar Penuh", input prefix Rp, teks feedback real-time)
 * - Metode Pembayaran (Tunai, Transfer, QRIS + No Referensi jika non-tunai)
 * - Catatan Kasir (Opsional)
 * - Sticky Bottom Action Footer (Bayar X Nota • Rp [Total])
 */
@Composable
internal fun ReceivableDedicatedPaymentScreen(
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
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    val payAmount: BigDecimal = remember(paymentAmountInput) {
        paymentAmountInput.trim().replace(",", ".").toBigDecimalOrNull() ?: BigDecimal.ZERO
    }
    val selectedInvoices: List<Receivable> = remember(unpaidReceivables, selectedInvoiceIds) {
        unpaidReceivables.filter { it.id in selectedInvoiceIds }
    }
    val selectedTotalRemaining: BigDecimal = remember(selectedInvoices) {
        selectedInvoices.fold(BigDecimal.ZERO) { sum, it -> sum + it.remainingAmount }
    }
    val allocations: List<InvoicePaymentAllocation> = remember(unpaidReceivables, selectedInvoiceIds, payAmount) {
        calculateFifoAllocations(unpaidReceivables, selectedInvoiceIds, payAmount)
    }
    val paymentsToMakeCount: Int = remember(allocations) {
        allocations.count { it.isSelected && it.allocatedAmount > BigDecimal.ZERO }
    }
    val isAllSelected = unpaidReceivables.isNotEmpty() && selectedInvoiceIds.size >= unpaidReceivables.size

    BackHandler(enabled = !isSubmittingPayment, onBack = onBack)

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        containerColor = Color.White,
        topBar = {
            // App Header (Sticky Top Bar)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                border = BorderStroke(1.dp, Slate100)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = onBack,
                            enabled = !isSubmittingPayment,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Kembali",
                                tint = Slate700,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = "Bayar Piutang",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900,
                            letterSpacing = (-0.2).sp
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Sticky Bottom Action Footer
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                border = BorderStroke(1.dp, Slate100),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    val actualPayAmount = payAmount.min(selectedTotalRemaining)
                    val buttonEnabled = !isSubmittingPayment && payAmount > BigDecimal.ZERO && paymentsToMakeCount > 0

                    Button(
                        onClick = onSubmit,
                        enabled = buttonEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn-submit-dedicated-payment"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandGreen,
                            disabledContainerColor = Slate200
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        if (isSubmittingPayment) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "MEMPROSES PEMBAYARAN...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Payments,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (buttonEnabled) Color.White else Slate400
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (paymentsToMakeCount > 0 && actualPayAmount > BigDecimal.ZERO) {
                                    "BAYAR $paymentsToMakeCount NOTA • ${actualPayAmount.currencyText()}"
                                } else {
                                    "PILIH NOTA PIUTANG"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = if (buttonEnabled) Color.White else Slate400
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Customer Summary Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Slate50.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, Slate100),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Customer Avatar Initial
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Emerald50)
                                .border(1.dp, Emerald100, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = customerName.take(1).uppercase().ifBlank { "P" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandGreen
                            )
                        }

                        // Name & Selection Count
                        Column {
                            Text(
                                text = customerName.ifBlank { "Pelanggan" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${selectedInvoices.size}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandGreen
                                )
                                Text(
                                    text = " dari ${unpaidReceivables.size} nota dipilih",
                                    fontSize = 12.sp,
                                    color = Slate500
                                )
                            }
                        }
                    }

                    // Total Balance
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "TOTAL TAGIHAN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate400,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = selectedTotalRemaining.currencyText(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = BrandGreen
                        )
                    }
                }
            }

            // Error Message Banner (jika ada error)
            if (!errorMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            // 2. Invoices Section (Rincian Nota Piutang)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RINCIAN NOTA PIUTANG (${unpaidReceivables.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 0.5.sp
                    )

                    Text(
                        text = if (isAllSelected) "Batal Pilih Semua" else "Pilih Semua",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandGreen,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onSelectAllInvoices(!isAllSelected) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Slate100),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        for ((index, allocation) in allocations.withIndex()) {
                            val invoice = allocation.receivable
                            val isChecked = allocation.isSelected

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isChecked) Color.White else Slate50.copy(alpha = 0.6f))
                                    .clickable { onToggleInvoice(invoice.id) }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { onToggleInvoice(invoice.id) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = BrandGreen,
                                        uncheckedColor = Slate400
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
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (isChecked) Slate800 else Slate500
                                        )

                                        Text(
                                            text = invoice.remainingAmount.currencyText(),
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (isChecked) Slate900 else Slate500
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
                                            fontSize = 11.sp,
                                            color = Slate400
                                        )

                                        // Status Allocation Text
                                        when {
                                            !allocation.isSelected -> {
                                                Text(
                                                    text = "Tidak Dipilih",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Slate400
                                                )
                                            }
                                            allocation.isFullyPaid -> {
                                                Text(
                                                    text = "✓ Lunas",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Emerald700
                                                )
                                            }
                                            allocation.isPartiallyPaid -> {
                                                Text(
                                                    text = "Sebagian (${allocation.allocatedAmount.currencyText()})",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Amber700
                                                )
                                            }
                                            else -> {
                                                Text(
                                                    text = "Belum Teralokasi",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Slate400
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (index < allocations.lastIndex) {
                                HorizontalDivider(color = Slate100, thickness = 1.dp)
                            }
                        }
                    }
                }
            }

            // 3. Amount Input Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "NOMINAL PEMBAYARAN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate500,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = " *",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Rose500
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onAmountChanged(selectedTotalRemaining.toInputText()) }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
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
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandGreen
                        )
                    }
                }

                OutlinedTextField(
                    value = paymentAmountInput,
                    onValueChange = onAmountChanged,
                    prefix = {
                        Text(
                            text = "Rp",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate400,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = Slate900
                    ),
                    placeholder = {
                        Text(
                            text = "0",
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Slate400
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandGreen,
                        unfocusedBorderColor = Slate200,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Slate50.copy(alpha = 0.7f)
                    )
                )

                // Feedback Note
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    when {
                        selectedInvoices.isEmpty() -> {
                            Text(
                                text = "Tidak ada nota yang dipilih",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Slate500
                            )
                        }
                        payAmount >= selectedTotalRemaining && selectedTotalRemaining > BigDecimal.ZERO -> {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = Emerald600,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Seluruh ${selectedInvoices.size} nota terpilih akan lunas penuh",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Emerald800
                            )
                        }
                        payAmount > BigDecimal.ZERO && payAmount < selectedTotalRemaining -> {
                            val sisa = selectedTotalRemaining - payAmount
                            Text(
                                text = "Dialokasikan ke nota terlama dahulu (FIFO). Sisa belum dibayar: ${sisa.currencyText()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Amber700
                            )
                        }
                        else -> {
                            Text(
                                text = "Masukkan nominal yang dibayarkan pelanggan",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                color = Slate500
                            )
                        }
                    }
                }
            }

            // 4. Payment Method Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "METODE PEMBAYARAN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate500,
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (method in ReceivablePaymentMethod.entries) {
                        val isSelected = method == paymentMethod
                        val icon: ImageVector = when (method) {
                            ReceivablePaymentMethod.Cash -> Icons.Outlined.Payments
                            ReceivablePaymentMethod.Transfer -> Icons.Outlined.AccountBalance
                            ReceivablePaymentMethod.Qris -> Icons.Outlined.QrCode
                        }

                        Surface(
                            onClick = { onMethodChanged(method) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) BrandGreenLight else Color.White,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) BrandGreen else Slate200
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) BrandGreen else Slate500,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = method.label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) BrandGreen else Slate600
                                )
                            }
                        }
                    }
                }

                // Reference Field (Jika Transfer atau QRIS)
                if (paymentMethod != ReceivablePaymentMethod.Cash) {
                    OutlinedTextField(
                        value = referenceInput,
                        onValueChange = onReferenceChanged,
                        placeholder = { Text("Contoh: Trx ID #987654", color = Slate400, fontSize = 12.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Receipt, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp))
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandGreen,
                            unfocusedBorderColor = Slate200,
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Slate50.copy(alpha = 0.7f)
                        )
                    )
                }
            }

            // 5. Notes Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "CATATAN KASIR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = " (OPSIONAL)",
                        fontSize = 11.sp,
                        color = Slate400
                    )
                }

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = onNotesChanged,
                    placeholder = {
                        Text(
                            text = "Catatan pembayaran atau keterangan kasir",
                            color = Slate400,
                            fontSize = 12.5.sp
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.Description, contentDescription = null, tint = Slate400, modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandGreen,
                        unfocusedBorderColor = Slate200,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Slate50.copy(alpha = 0.7f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun BigDecimal.toInputText(): String =
    stripTrailingZeros().toPlainString()
