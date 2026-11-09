package com.tbterminal.app.ui.receivables

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.Receivable
import java.math.BigDecimal
import java.time.LocalDate

internal enum class QuickBillFormatMode(val label: String) {
    Summary("Rangkuman Rapi"),
    Detailed("Rincian Nota")
}

internal data class QuickBillCustomerItem(
    val id: String,
    val name: String,
    val phone: String?,
    val totalRemaining: BigDecimal,
    val unpaidInvoices: List<Receivable>,
    val overdueCount: Int,
    val nearestDue: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuickBillBottomSheet(
    uiState: ReceivableUiState,
    initialCustomerId: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }

    val customersWithDebt = remember(uiState.receivables, uiState.customers) {
        val unpaidGroup = uiState.receivables
            .filter { it.remainingAmount > BigDecimal.ZERO }
            .groupBy { it.customerId }

        unpaidGroup.map { (custId, list) ->
            val totalRemaining = list.sumOf { it.remainingAmount }
            val customerObj = uiState.customers.firstOrNull { it.id == custId }
            val name = customerObj?.name ?: list.firstOrNull()?.customerName ?: "Pelanggan"
            val phone = customerObj?.phone
            val nearestDue = list.mapNotNull {
                runCatching { LocalDate.parse(it.dueDate.take(10)) }.getOrNull()
            }.minOrNull()?.toString()

            val overdueCount = list.count {
                val d = runCatching { LocalDate.parse(it.dueDate.take(10)) }.getOrNull()
                d != null && d.isBefore(today)
            }

            QuickBillCustomerItem(
                id = custId,
                name = name,
                phone = phone,
                totalRemaining = totalRemaining,
                unpaidInvoices = list,
                overdueCount = overdueCount,
                nearestDue = nearestDue
            )
        }.sortedByDescending { it.totalRemaining }
    }

    var selectedCustomerId by remember(initialCustomerId, customersWithDebt) {
        mutableStateOf(initialCustomerId ?: customersWithDebt.firstOrNull()?.id)
    }

    val selectedCustomer = customersWithDebt.firstOrNull { it.id == selectedCustomerId }
        ?: customersWithDebt.firstOrNull()

    // Mode format: jika pelanggan memiliki banyak nota (>3), default-kan ke Summary agar tidak menumpuk
    var formatMode by remember(selectedCustomer?.id) {
        val defaultMode = if ((selectedCustomer?.unpaidInvoices?.size ?: 0) > 3) {
            QuickBillFormatMode.Summary
        } else {
            QuickBillFormatMode.Detailed
        }
        mutableStateOf(defaultMode)
    }

    // State teks pesan yang dapat diedit bebas oleh kasir
    var editedMessageText by remember(selectedCustomer?.id, formatMode) {
        val initialText = if (selectedCustomer != null) {
            buildWhatsAppReminderText(
                customerName = selectedCustomer.name,
                totalRemainingText = selectedCustomer.totalRemaining.currencyText(),
                unpaidInvoices = selectedCustomer.unpaidInvoices,
                overdueCount = selectedCustomer.overdueCount,
                nearestDueDate = selectedCustomer.nearestDue,
                mode = formatMode
            )
        } else ""
        mutableStateOf(initialText)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color(0xFFE2E8F0), CircleShape)
                )
            }
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tagih Cepat via WhatsApp",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Teks dapat diedit sebelum dibagikan ke WhatsApp (tanpa API)",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Tutup",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            if (customersWithDebt.isEmpty()) {
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFFDCFCE7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "Semua Piutang Lunas",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Tidak ada tagihan piutang aktif yang perlu ditagih saat ini.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                // Pemilih Pelanggan (jika lebih dari 1 pelanggan)
                if (customersWithDebt.size > 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Pilih Pelanggan:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            customersWithDebt.forEach { item ->
                                val isSelected = item.id == selectedCustomerId
                                Surface(
                                    onClick = { selectedCustomerId = item.id },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFF15803D) else Color(0xFFF8FAFC),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFF15803D) else Color(0xFFE2E8F0)
                                    ),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = item.name,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = item.totalRemaining.currencyText(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = if (isSelected) Color(0xFFDCFCE7) else Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Info Pelanggan Terpilih
                if (selectedCustomer != null) {
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFFE2E8F0), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.Person,
                                            contentDescription = null,
                                            tint = Color(0xFF475569),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = selectedCustomer.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.Phone,
                                                contentDescription = null,
                                                tint = Color(0xFF94A3B8),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = selectedCustomer.phone ?: "No. WhatsApp belum diisi",
                                                fontSize = 11.sp,
                                                color = if (selectedCustomer.phone != null) Color(0xFF475569) else Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Total Piutang",
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = selectedCustomer.totalRemaining.currencyText(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.ReceiptLong,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "${selectedCustomer.unpaidInvoices.size} Nota Belum Lunas",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                if (selectedCustomer.overdueCount > 0) {
                                    Text(
                                        text = "${selectedCustomer.overdueCount} Jatuh Tempo",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFE11D48)
                                    )
                                } else if (!selectedCustomer.nearestDue.isNullOrBlank()) {
                                    Text(
                                        text = "Tempo: ${selectedCustomer.nearestDue}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            // Banner info jika pelanggan memiliki nota banyak
                            if (selectedCustomer.unpaidInvoices.size > 3) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFEFF6FF),
                                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Info,
                                            contentDescription = null,
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Pelanggan memiliki ${selectedCustomer.unpaidInvoices.size} nota. Format Rangkuman membuat pesan lebih rapi dan ringkas.",
                                            fontSize = 11.sp,
                                            color = Color(0xFF1E40AF),
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Pilihan Format Pesan (Tab Ringkasan vs Rincian)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Format Pesan WhatsApp:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickBillFormatMode.entries.forEach { mode ->
                                val isSelected = formatMode == mode
                                Surface(
                                    onClick = { formatMode = mode },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                    modifier = Modifier.weight(1f).height(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = mode.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Color(0xFF475569)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Editable Message Text Field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = null,
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Edit Teks Pesan:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF475569)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Tombol Reset ke Default Template
                                Surface(
                                    onClick = {
                                        editedMessageText = buildWhatsAppReminderText(
                                            customerName = selectedCustomer.name,
                                            totalRemainingText = selectedCustomer.totalRemaining.currencyText(),
                                            unpaidInvoices = selectedCustomer.unpaidInvoices,
                                            overdueCount = selectedCustomer.overdueCount,
                                            nearestDueDate = selectedCustomer.nearestDue,
                                            mode = formatMode
                                        )
                                        Toast.makeText(context, "Teks di-reset ke template awal", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.Refresh,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Reset",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                // Tombol Salin Teks
                                Surface(
                                    onClick = {
                                        copyTextToClipboard(context, "Pesan Tagihan", editedMessageText)
                                        Toast.makeText(context, "Teks pesan berhasil disalin!", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.ContentCopy,
                                            contentDescription = null,
                                            tint = Color(0xFF15803D),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Salin",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF15803D)
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = editedMessageText,
                            onValueChange = { editedMessageText = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(
                                fontSize = 11.sp,
                                color = Color(0xFF14532D),
                                lineHeight = 16.sp,
                                fontFamily = FontFamily.Default
                            ),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 7,
                            maxLines = 14,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF0FDF4),
                                unfocusedContainerColor = Color(0xFFF0FDF4),
                                focusedBorderColor = Color(0xFF15803D),
                                unfocusedBorderColor = Color(0xFFDCFCE7),
                                cursorColor = Color(0xFF15803D)
                            )
                        )
                        Text(
                            text = "💡 Tip: Anda dapat menambahkan nomor rekening bank, catatan khusus, atau salam sebelum mengirim.",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 14.sp
                        )
                    }

                    // Tombol Aksi Utama (Bagikan ke WhatsApp)
                    Surface(
                        onClick = {
                            shareReminderViaWhatsApp(
                                context = context,
                                message = editedMessageText,
                                phoneNumber = selectedCustomer.phone
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF15803D),
                        contentColor = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Share,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Bagikan ke WhatsApp",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Format pesan WhatsApp yang rapi, profesional, dan nyaman dibaca.
 * Menangani pelanggan dengan banyak nota secara cerdas:
 * - Mode Summary: Rangkuman total + status nota (sangat cocok untuk banyak nota).
 * - Mode Detailed: Rincian nota prioritas (top 4-5 nota tertua/jatuh tempo) + ringkasan sisa nota.
 */
private fun buildWhatsAppReminderText(
    customerName: String,
    totalRemainingText: String,
    unpaidInvoices: List<Receivable>,
    overdueCount: Int,
    nearestDueDate: String?,
    mode: QuickBillFormatMode,
    storeName: String = "Terminal Barokah Jaya"
): String {
    val sb = StringBuilder()
    sb.appendLine("*PENGINGAT TAGIHAN PIUTANG*")
    sb.appendLine("*$storeName*")
    sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
    sb.appendLine("Yth. Bapak/Ibu *$customerName*,")
    sb.appendLine()
    sb.appendLine("Semoga Bapak/Ibu senantiasa dalam keadaan sehat.")
    sb.appendLine("Kami menginformasikan rekap tagihan piutang Anda di *$storeName*:")
    sb.appendLine()
    sb.appendLine("💰 *Total Sisa Tagihan:* *$totalRemainingText*")
    
    val totalNota = unpaidInvoices.size
    val notaStatusDesc = when {
        overdueCount > 0 -> "$totalNota Nota ($overdueCount Jatuh Tempo)"
        else -> "$totalNota Nota"
    }
    sb.appendLine("📑 *Jumlah Tagihan:* $notaStatusDesc")

    if (!nearestDueDate.isNullOrBlank()) {
        sb.appendLine("⏰ *Jatuh Tempo Terdekat:* $nearestDueDate")
    }

    if (mode == QuickBillFormatMode.Detailed && unpaidInvoices.isNotEmpty()) {
        sb.appendLine()
        sb.appendLine("📋 *Rincian Nota:*")

        // Urutkan nota: yang jatuh tempo duluan / tanggal terlama di atas
        val sortedList = unpaidInvoices.sortedBy { it.dueDate }
        val maxShow = 5
        val displayItems = sortedList.take(maxShow)
        val remainingCount = sortedList.size - displayItems.size

        displayItems.forEachIndexed { idx, item ->
            val ref = item.legacyInvoiceNumber?.takeIf(String::isNotBlank)
                ?: item.transactionId?.takeIf(String::isNotBlank)
                ?: item.id.take(8).uppercase()
            val due = item.dueDate.take(10)
            val isOverdue = runCatching { LocalDate.parse(due).isBefore(LocalDate.now()) }.getOrDefault(false)
            val dueStatus = if (isOverdue) " _(Jatuh tempo)_" else " _(Tempo: $due)_"
            sb.appendLine("${idx + 1}. Nota #$ref • ${item.remainingAmount.currencyText()}$dueStatus")
        }

        if (remainingCount > 0) {
            val remainingSum = sortedList.drop(maxShow).sumOf { it.remainingAmount }
            sb.appendLine("_(...dan $remainingCount nota lainnya senilai ${remainingSum.currencyText()})_")
        }
    }

    sb.appendLine()
    sb.appendLine("Pembayaran dapat diselesaikan langsung di kasir toko atau transfer rekening.")
    sb.appendLine("Apabila ada pertanyaan mengenai rincian nota atau sudah melakukan pembayaran, silakan hubungi kami.")
    sb.appendLine()
    sb.appendLine("Terima kasih atas kerja sama dan kepercayaannya. 🙏")
    sb.appendLine()
    sb.appendLine("*$storeName*")
    return sb.toString().trimEnd()
}

internal fun shareReminderViaWhatsApp(
    context: Context,
    message: String,
    phoneNumber: String?
) {
    val cleanPhone = phoneNumber?.filter { it.isDigit() }?.let { raw ->
        when {
            raw.startsWith("0") -> "62" + raw.drop(1)
            raw.startsWith("62") -> raw
            raw.startsWith("8") -> "62$raw"
            else -> raw
        }
    }

    // 1. Jika no. HP terdaftar, buka chat WhatsApp nomor tersebut dengan pesan yang sudah terisi
    if (!cleanPhone.isNullOrBlank() && cleanPhone.length >= 8) {
        try {
            val encodedMsg = java.net.URLEncoder.encode(message, "UTF-8")
            val directUri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg")
            val directIntent = Intent(Intent.ACTION_VIEW, directUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(directIntent)
            return
        } catch (_: Exception) {
            // Fall back to native ACTION_SEND jika web intent gagal
        }
    }

    // 2. Fitur Share Bawaan Android (Intent.ACTION_SEND)
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Pengingat Tagihan Piutang")
        putExtra(Intent.EXTRA_TEXT, message)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    try {
        val waIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_TEXT, message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(waIntent)
    } catch (_: Exception) {
        val chooser = Intent.createChooser(sendIntent, "Bagikan Pengingat Tagihan via WhatsApp").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(chooser)
    }
}

private fun copyTextToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText(label, text)
    clipboard?.setPrimaryClip(clip)
}
