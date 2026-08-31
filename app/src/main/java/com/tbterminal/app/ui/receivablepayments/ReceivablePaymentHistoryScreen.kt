package com.tbterminal.app.ui.receivablepayments

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MonetizationOn
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tbterminal.app.data.model.ReceivablePaymentHistory
import com.tbterminal.app.data.model.ReceivablePaymentReceipt
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PaymentSurface = Color.White
private val PaymentSoft = Color(0xFFF1F5F9)
private val PaymentBorder = Color(0xFFE2E8F0)
private val PaymentText = Color(0xFF0F172A)
private val PaymentMuted = Color(0xFF64748B)
private val PaymentPrimary = Color(0xFF059669)

@Composable
internal fun ReceivablePaymentHistoryScreen(
    modifier: Modifier,
    uiState: ReceivablePaymentHistoryUiState,
    onSearchChanged: (String) -> Unit,
    onReceiverSearchChanged: (String) -> Unit,
    onReceivableIdChanged: (String) -> Unit,
    onDateFromChanged: (String) -> Unit,
    onDateToChanged: (String) -> Unit,
    onMethodFilterChanged: (ReceivablePaymentMethodFilter) -> Unit,
    onStatusFilterChanged: (ReceivablePaymentStatusFilter) -> Unit,
    onApplyFilters: () -> Unit,
    onShowDetail: (ReceivablePaymentHistory) -> Unit,
    onDismissDetail: () -> Unit,
    canReverse: Boolean,
    onOpenReversal: (ReceivablePaymentHistory) -> Unit,
    onReversalReasonChanged: (String) -> Unit,
    onDismissReversal: () -> Unit,
    onSubmitReversal: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    BoxWithConstraints(modifier.fillMaxSize().background(PaymentSurface)) {
        val compact = maxWidth < 700.dp
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = if (compact) 16.dp else 32.dp, vertical = if (compact) 14.dp else 24.dp), verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 22.dp)) {
        PaymentTable(
            uiState, onSearchChanged, onReceiverSearchChanged, onReceivableIdChanged, onDateFromChanged, onDateToChanged,
            onMethodFilterChanged, onStatusFilterChanged, onApplyFilters, onShowDetail,
            onPreviousPage, onNextPage, compact
        )
        }
    }
    uiState.selectedPayment?.let {
        PaymentDetailDialog(it, onDismissDetail, canReverse, onOpenReversal)
    }
    uiState.paymentToReverse?.let { payment ->
        ReversalDialog(
            payment = payment,
            reason = uiState.reversalReason,
            isSubmitting = uiState.isReversing,
            onReasonChanged = onReversalReasonChanged,
            onDismiss = onDismissReversal,
            onSubmit = onSubmitReversal
        )
    }
}

@Composable
private fun PaymentHistoryHeader() {
    Text("Riwayat Pembayaran Piutang", color = PaymentText, fontSize = 28.sp, fontWeight = FontWeight.Medium)
}

@Composable
private fun PaymentSummaryCards(uiState: ReceivablePaymentHistoryUiState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        PaymentMetric(
            "TOTAL PEMBAYARAN",
            uiState.totalPayments.toString(),
            "Sesuai data tersimpan",
            Icons.AutoMirrored.Outlined.ReceiptLong,
            PaymentPrimary,
            Modifier.weight(1f)
        )
        PaymentMetric(
            "NILAI HALAMAN INI",
            uiState.pageTotal.asCurrency(),
            "Maksimal ${uiState.pageSize} pembayaran",
            Icons.Outlined.MonetizationOn,
            Color(0xFF2563EB),
            Modifier.weight(1f)
        )
        PaymentMetric(
            "PIUTANG LUNAS",
            uiState.paidReceivablesOnPage.toString(),
            "Pada pembayaran yang tampil",
            Icons.Outlined.CheckCircle,
            Color(0xFF7C3AED),
            Modifier.weight(1f)
        )
    }
}

@Composable
private fun PaymentMetric(
    title: String,
    value: String,
    note: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier
) {
    Card(modifier, colors = CardDefaults.cardColors(PaymentSurface), border = BorderStroke(1.dp, PaymentBorder), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(value, color = PaymentText, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text(note, color = PaymentMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun PaymentTable(
    uiState: ReceivablePaymentHistoryUiState,
    onSearchChanged: (String) -> Unit,
    onReceiverSearchChanged: (String) -> Unit,
    onReceivableIdChanged: (String) -> Unit,
    onDateFromChanged: (String) -> Unit,
    onDateToChanged: (String) -> Unit,
    onMethodFilterChanged: (ReceivablePaymentMethodFilter) -> Unit,
    onStatusFilterChanged: (ReceivablePaymentStatusFilter) -> Unit,
    onApplyFilters: () -> Unit,
    onShowDetail: (ReceivablePaymentHistory) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    compact: Boolean
) {
    Column(Modifier.fillMaxWidth()) {
        PaymentToolbar(
            uiState, onSearchChanged, onReceiverSearchChanged, onReceivableIdChanged, onDateFromChanged, onDateToChanged,
            onMethodFilterChanged, onStatusFilterChanged, onApplyFilters, compact
        )
        Spacer(Modifier.height(if (compact) 12.dp else 28.dp))
        if (!compact) PaymentTableHeader()
        when {
            uiState.isLoading -> LoadingBox()
            uiState.errorMessage != null -> PaymentError(uiState.errorMessage)
            uiState.filteredPayments.isEmpty() -> EmptyBox()
            else -> uiState.filteredPayments.forEachIndexed { index, payment ->
                if (compact) PaymentMobileRow(payment, onShowDetail) else PaymentTableRow(payment, useAlternateBackground = index % 2 != 0, onShowDetail = onShowDetail)
            }
        }
        PaymentPagination(uiState, compact, onPreviousPage, onNextPage)
    }
}

@Composable
private fun PaymentToolbar(
    uiState: ReceivablePaymentHistoryUiState,
    onSearchChanged: (String) -> Unit,
    onReceiverSearchChanged: (String) -> Unit,
    onReceivableIdChanged: (String) -> Unit,
    onDateFromChanged: (String) -> Unit,
    onDateToChanged: (String) -> Unit,
    onMethodFilterChanged: (ReceivablePaymentMethodFilter) -> Unit,
    onStatusFilterChanged: (ReceivablePaymentStatusFilter) -> Unit,
    onApplyFilters: () -> Unit,
    compact: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (compact) Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PaymentFilterField(uiState.searchQuery, onSearchChanged, "Nama pelanggan", Modifier.fillMaxWidth())
            PaymentFilterField(uiState.receiverSearch, onReceiverSearchChanged, "Kasir atau penerima", Modifier.fillMaxWidth())
            PaymentMethodFilterDropdown(uiState.methodFilter, onMethodFilterChanged, Modifier.fillMaxWidth())
            PaymentStatusFilterDropdown(uiState.statusFilter, onStatusFilterChanged, Modifier.fillMaxWidth())
            PaymentFilterField(uiState.receivableIdFilter, onReceivableIdChanged, "ID piutang", Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PaymentFilterField(uiState.dateFrom, onDateFromChanged, "Tanggal awal", Modifier.weight(1f))
                PaymentFilterField(uiState.dateTo, onDateToChanged, "Tanggal akhir", Modifier.weight(1f))
            }
            Button(onClick = onApplyFilters, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("Terapkan") }
        } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            PaymentFilterField(uiState.searchQuery, onSearchChanged, "Nama pelanggan", Modifier.weight(1f))
            PaymentFilterField(uiState.receiverSearch, onReceiverSearchChanged, "Nama kasir/penerima", Modifier.weight(1f))
            PaymentMethodFilterDropdown(uiState.methodFilter, onMethodFilterChanged, Modifier.width(220.dp))
            PaymentStatusFilterDropdown(uiState.statusFilter, onStatusFilterChanged, Modifier.width(190.dp))
        }
        if (!compact) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            PaymentFilterField(uiState.receivableIdFilter, onReceivableIdChanged, "ID piutang", Modifier.weight(1f))
            PaymentFilterField(uiState.dateFrom, onDateFromChanged, "Dari (yyyy-MM-dd)", Modifier.weight(1f))
            PaymentFilterField(uiState.dateTo, onDateToChanged, "Sampai (yyyy-MM-dd)", Modifier.weight(1f))
            Button(onClick = onApplyFilters, modifier = Modifier.height(56.dp)) { Text("Terapkan filter") }
        }
    }
}

@Composable
private fun PaymentFilterField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = PaymentMuted) },
        trailingIcon = { Icon(Icons.Outlined.Search, null, tint = PaymentMuted) },
        singleLine = true,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PaymentPrimary,
            unfocusedBorderColor = PaymentBorder,
            focusedContainerColor = PaymentSurface,
            unfocusedContainerColor = PaymentSurface
        )
    )
}

@Composable
private fun PaymentStatusFilterDropdown(
    selected: ReceivablePaymentStatusFilter,
    onSelect: (ReceivablePaymentStatusFilter) -> Unit,
    modifier: Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = modifier.height(56.dp), shape = RoundedCornerShape(16.dp)) {
            Text(selected.label, modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.ExpandMore, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ReceivablePaymentStatusFilter.entries.forEach { filter ->
                DropdownMenuItem(text = { Text(filter.label) }, onClick = {
                    expanded = false
                    onSelect(filter)
                })
            }
        }
    }
}

@Composable
private fun PaymentMethodFilterDropdown(
    selected: ReceivablePaymentMethodFilter,
    onSelect: (ReceivablePaymentMethodFilter) -> Unit,
    modifier: Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = modifier.height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, PaymentBorder)
        ) {
            Text(selected.label, modifier = Modifier.weight(1f), color = PaymentText, fontWeight = FontWeight.Medium)
            Icon(Icons.Outlined.ExpandMore, "Pilih metode pembayaran", tint = PaymentMuted, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(220.dp).heightIn(max = 280.dp).background(PaymentSurface)
        ) {
            ReceivablePaymentMethodFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = { Text(filter.label) },
                    onClick = {
                        expanded = false
                        onSelect(filter)
                    }
                )
            }
        }
    }
}

@Composable
private fun PaymentTableHeader() {
    Row(Modifier.fillMaxWidth().background(PaymentSoft).padding(horizontal = 20.dp, vertical = 12.dp)) {
        TableLabel("PELANGGAN", Modifier.weight(1.7f))
        TableLabel("NOMOR / PIUTANG", Modifier.weight(1.6f))
        TableLabel("TANGGAL BAYAR", Modifier.weight(1.4f))
        TableLabel("METODE", Modifier.weight(0.9f))
        TableLabel("NOMINAL", Modifier.weight(1.1f))
        TableLabel("STATUS", Modifier.weight(0.9f))
        TableLabel("AKSI", Modifier.weight(0.45f))
    }
}

@Composable
private fun PaymentTableRow(
    payment: ReceivablePaymentHistory,
    useAlternateBackground: Boolean,
    onShowDetail: (ReceivablePaymentHistory) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (useAlternateBackground) PaymentSoft.copy(alpha = 0.76f) else PaymentSurface)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(payment.customerName, Modifier.weight(1.7f), color = PaymentText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Column(Modifier.weight(1.6f)) {
            Text(payment.paymentNumber, color = PaymentText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(payment.receivableId.shortId(), color = PaymentMuted, fontSize = 10.sp)
        }
        Text(payment.paidAt.asDisplayDate(), Modifier.weight(1.4f), color = PaymentMuted, fontSize = 13.sp)
        Text(payment.method.paymentMethodLabel(), Modifier.weight(0.9f), color = PaymentMuted, fontSize = 13.sp)
        Text(payment.amount.asCurrency(), Modifier.weight(1.1f), color = PaymentText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Box(Modifier.weight(0.9f)) { ReceivableStatusBadge(payment.receivableStatus) }
        Box(Modifier.weight(0.45f)) {
            IconButton(onClick = { onShowDetail(payment) }) {
                Icon(Icons.Outlined.Visibility, "Lihat detail", tint = PaymentPrimary)
            }
        }
    }
    HorizontalDivider(color = PaymentBorder)
}

@Composable
private fun PaymentMobileRow(payment: ReceivablePaymentHistory, onShowDetail: (ReceivablePaymentHistory) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(payment.customerName, color = PaymentText, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(payment.paymentNumber, color = PaymentMuted, fontSize = 11.sp)
            }
            ReceivableStatusBadge(payment.receivableStatus)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text("Tanggal", color = PaymentMuted, fontSize = 10.sp); Text(payment.paidAt.asDisplayDate(), color = PaymentText, fontSize = 12.sp) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Metode", color = PaymentMuted, fontSize = 10.sp); Text(payment.method.paymentMethodLabel(), color = PaymentText, fontSize = 12.sp) }
            Column(horizontalAlignment = Alignment.End) { Text("Nominal", color = PaymentMuted, fontSize = 10.sp); Text(payment.amount.asCurrency(), color = PaymentText, fontWeight = FontWeight.Bold) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Piutang ${payment.receivableId.shortId()}", color = PaymentMuted, fontSize = 11.sp)
            IconButton(onClick = { onShowDetail(payment) }) { Icon(Icons.Outlined.Visibility, "Lihat detail", tint = PaymentPrimary) }
        }
    }
    HorizontalDivider(color = PaymentBorder)
}

@Composable
private fun ReceivableStatusBadge(status: String) {
    val isPaid = status.equals("PAID", ignoreCase = true) || status.equals("lunas", ignoreCase = true)
    val tint = if (isPaid) PaymentPrimary else Color(0xFFF59E0B)
    Surface(color = tint.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp)) {
        Text(status.replace('_', ' ').uppercase(), Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = tint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PaymentPagination(
    uiState: ReceivablePaymentHistoryUiState,
    compact: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    val controls: @Composable () -> Unit = { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        PaymentPageButton(Icons.Default.ChevronLeft, uiState.page > 1, onPreviousPage)
        Text("${uiState.page} / ${uiState.totalPages}", color = PaymentText, fontWeight = FontWeight.Bold)
        PaymentPageButton(Icons.Default.ChevronRight, uiState.page < uiState.totalPages, onNextPage)
    } }
    if (compact) Column(Modifier.fillMaxWidth().background(PaymentSoft).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("${uiState.totalPayments} pembayaran", color = PaymentMuted, fontSize = 12.sp)
        controls()
    } else Row(Modifier.fillMaxWidth().background(PaymentSoft.copy(alpha = 0.7f)).padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(
                if (uiState.hasLocalFilter) {
                    "Menampilkan ${uiState.filteredPayments.size} pembayaran pada halaman ini"
                } else {
                    "Menampilkan ${uiState.currentStart}-${uiState.currentEnd} dari ${uiState.totalPayments} pembayaran"
                },
                color = PaymentText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text("Maksimal ${uiState.pageSize} pembayaran per halaman", color = PaymentMuted, fontSize = 11.sp)
        }
        controls()
    }
}

@Composable
private fun PaymentPageButton(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(34.dp)
    ) {
        Icon(icon, contentDescription = null)
    }
}

@Composable
private fun PaymentDetailDialog(
    payment: ReceivablePaymentHistory,
    onDismiss: () -> Unit,
    canReverse: Boolean,
    onOpenReversal: (ReceivablePaymentHistory) -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Card(Modifier.widthIn(max = 560.dp), colors = CardDefaults.cardColors(PaymentSurface), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(24.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Detail Pembayaran Piutang", color = PaymentText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(payment.paymentNumber, color = PaymentPrimary, fontSize = 12.sp)
                    }
                    TextButton(onClick = onDismiss) { Text("Tutup") }
                }
                Spacer(Modifier.height(16.dp))
                DetailInfo("Pelanggan", payment.customerName)
                DetailInfo("Transaksi", payment.transactionId?.shortId() ?: payment.source)
                DetailInfo("Tanggal bayar", payment.paidAt.asDisplayDate())
                DetailInfo("Metode", payment.method.paymentMethodLabel())
                DetailInfo("Jenis entri", payment.entryType)
                DetailInfo("Nominal", payment.amount.asCurrency())
                DetailInfo("Penerima", payment.receivedByName)
                DetailInfo("Saldo sebelum", payment.balanceBefore.asCurrency())
                DetailInfo("Saldo sesudah", payment.balanceAfter.asCurrency())
                DetailInfo("Status piutang", payment.receivableStatus.replace('_', ' ').uppercase())
                DetailInfo("Referensi", payment.reference ?: "-")
                DetailInfo("Catatan", payment.notes ?: "-")
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { printReceivablePaymentReceipt(context, payment.toReceipt()) }) {
                        Text("Cetak bukti")
                    }
                    if (canReverse && payment.entryType == "PAYMENT" && !payment.isReversed) {
                        OutlinedButton(onClick = { onDismiss(); onOpenReversal(payment) }) {
                            Text("Reversal")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReversalDialog(
    payment: ReceivablePaymentHistory,
    reason: String,
    isSubmitting: Boolean,
    onReasonChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reversal ${payment.paymentNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Reversal akan mengembalikan saldo piutang sebesar ${payment.amount.asCurrency()}.")
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChanged,
                    label = { Text("Alasan koreksi") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onSubmit, enabled = !isSubmitting) {
                Text(if (isSubmitting) "Memproses..." else "Catat reversal")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun DetailInfo(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = PaymentMuted, fontSize = 13.sp)
        Text(value, color = PaymentText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LoadingBox() = Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

@Composable
private fun EmptyBox() = Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) { Text("Belum ada pembayaran piutang.", color = PaymentMuted) }

@Composable
private fun PaymentError(message: String) {
    Column(Modifier.fillMaxWidth().height(180.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(message, color = Color(0xFFDC2626))
    }
}

@Composable
private fun TableLabel(text: String, modifier: Modifier) {
    Text(text, modifier, color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
}

private fun BigDecimal.asCurrency(): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).format(this)

private fun String.paymentMethodLabel(): String = when (lowercase()) {
    "tunai" -> "Tunai"
    "transfer" -> "Transfer"
    "qris" -> "QRIS"
    else -> replaceFirstChar(Char::uppercase)
}

private fun String.shortId(): String = takeLast(8).uppercase()

private fun String.asDisplayDate(): String {
    return runCatching {
        OffsetDateTime.parse(this).format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm", Locale.forLanguageTag("id-ID")))
    }.getOrDefault(this)
}

private fun ReceivablePaymentHistory.toReceipt() = ReceivablePaymentReceipt(
    id = id,
    paymentNumber = paymentNumber,
    receivableId = receivableId,
    customerId = customerId,
    customerName = customerName,
    amount = amount,
    method = method,
    reference = reference,
    notes = notes,
    paidAt = paidAt,
    paymentDate = paymentDate,
    entryType = entryType,
    reversedPaymentId = reversedPaymentId,
    receivedBy = receivedBy,
    receivedByName = receivedByName,
    balanceBefore = balanceBefore,
    balanceAfter = balanceAfter,
    receivableStatus = receivableStatus,
    receivableRemainingAmount = receivableRemainingAmount,
    idempotentReplay = false
)
