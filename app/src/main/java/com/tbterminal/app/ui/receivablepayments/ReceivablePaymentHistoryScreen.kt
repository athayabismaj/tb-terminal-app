package com.tbterminal.app.ui.receivablepayments

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tbterminal.app.data.model.ReceivablePaymentHistory
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
    onMethodFilterChanged: (ReceivablePaymentMethodFilter) -> Unit,
    onShowDetail: (ReceivablePaymentHistory) -> Unit,
    onDismissDetail: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PaymentSurface)
            .verticalScroll(rememberScrollState())
            .padding(40.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        PaymentHistoryHeader()
        PaymentTable(uiState, onSearchChanged, onMethodFilterChanged, onShowDetail, onPreviousPage, onNextPage)
    }
    uiState.selectedPayment?.let { PaymentDetailDialog(it, onDismissDetail) }
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
    onMethodFilterChanged: (ReceivablePaymentMethodFilter) -> Unit,
    onShowDetail: (ReceivablePaymentHistory) -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        PaymentToolbar(uiState, onSearchChanged, onMethodFilterChanged)
        Spacer(Modifier.height(28.dp))
        PaymentTableHeader()
        when {
            uiState.isLoading -> LoadingBox()
            uiState.errorMessage != null -> PaymentError(uiState.errorMessage)
            uiState.filteredPayments.isEmpty() -> EmptyBox()
            else -> uiState.filteredPayments.forEachIndexed { index, payment ->
                PaymentTableRow(payment, useAlternateBackground = index % 2 != 0, onShowDetail = onShowDetail)
            }
        }
        PaymentPagination(uiState, onPreviousPage, onNextPage)
    }
}

@Composable
private fun PaymentToolbar(
    uiState: ReceivablePaymentHistoryUiState,
    onSearchChanged: (String) -> Unit,
    onMethodFilterChanged: (ReceivablePaymentMethodFilter) -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChanged,
            placeholder = { Text("Cari pelanggan atau transaksi...", color = PaymentMuted) },
            trailingIcon = { Icon(Icons.Outlined.Search, "Cari pembayaran", tint = PaymentMuted) },
            singleLine = true,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PaymentPrimary,
                unfocusedBorderColor = PaymentBorder,
                focusedContainerColor = PaymentSurface,
                unfocusedContainerColor = PaymentSurface
            )
        )
        PaymentMethodFilterDropdown(uiState.methodFilter, onMethodFilterChanged)
    }
}

@Composable
private fun PaymentMethodFilterDropdown(
    selected: ReceivablePaymentMethodFilter,
    onSelect: (ReceivablePaymentMethodFilter) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.width(220.dp).height(56.dp),
            shape = RoundedCornerShape(8.dp),
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
        TableLabel("TRANSAKSI", Modifier.weight(1.6f))
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
        Text(payment.transactionId.shortId(), Modifier.weight(1.6f), color = PaymentMuted, fontSize = 13.sp)
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
private fun ReceivableStatusBadge(status: String) {
    val isPaid = status.equals("lunas", ignoreCase = true)
    val tint = if (isPaid) PaymentPrimary else Color(0xFFF59E0B)
    Surface(color = tint.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp)) {
        Text(status.replace('_', ' ').uppercase(), Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = tint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PaymentPagination(
    uiState: ReceivablePaymentHistoryUiState,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Row(Modifier.fillMaxWidth().background(PaymentSoft.copy(alpha = 0.7f)).padding(horizontal = 20.dp, vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PaymentPageButton(Icons.Default.ChevronLeft, enabled = uiState.page > 1, onClick = onPreviousPage)
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(PaymentPrimary), contentAlignment = Alignment.Center) {
                Text(uiState.page.toString(), color = PaymentSurface, fontWeight = FontWeight.Bold)
            }
            Text("/ ${uiState.totalPages}", color = PaymentMuted, fontWeight = FontWeight.SemiBold)
            PaymentPageButton(Icons.Default.ChevronRight, enabled = uiState.page < uiState.totalPages, onClick = onNextPage)
        }
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
private fun PaymentDetailDialog(payment: ReceivablePaymentHistory, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(Modifier.widthIn(max = 560.dp), colors = CardDefaults.cardColors(PaymentSurface), shape = RoundedCornerShape(12.dp)) {
            Column(Modifier.padding(24.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Detail Pembayaran Piutang", color = PaymentText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(payment.id.shortId(), color = PaymentPrimary, fontSize = 12.sp)
                    }
                    TextButton(onClick = onDismiss) { Text("Tutup") }
                }
                Spacer(Modifier.height(16.dp))
                DetailInfo("Pelanggan", payment.customerName)
                DetailInfo("Transaksi", payment.transactionId.shortId())
                DetailInfo("Tanggal bayar", payment.paidAt.asDisplayDate())
                DetailInfo("Metode", payment.method.paymentMethodLabel())
                DetailInfo("Nominal masuk", payment.amount.asCurrency())
                DetailInfo("Sisa piutang saat ini", payment.receivableRemainingAmount.asCurrency())
                DetailInfo("Status piutang", payment.receivableStatus.replace('_', ' ').uppercase())
                DetailInfo("Referensi", payment.reference ?: "-")
                DetailInfo("Catatan", payment.notes ?: "-")
            }
        }
    }
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
