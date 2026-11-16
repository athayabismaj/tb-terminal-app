package com.tbterminal.app.ui.receivables

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.R
import com.tbterminal.app.data.model.Receivable
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    val today = remember { LocalDate.now() }

    val customersWithDebt = remember(uiState.receivables, uiState.customers) {
        val unpaidGroup = uiState.receivables
            .filter { it.remainingAmount > BigDecimal.ZERO }
            .groupBy { it.customerId }

        unpaidGroup.map { (custId, list) ->
            val totalRemaining = list.sumOf { it.remainingAmount }
            val customerObj = uiState.customers.firstOrNull { it.id == custId }
            val name = (customerObj?.name ?: list.firstOrNull()?.customerName ?: "Pelanggan").toTitleCase()
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

    QuickBillBottomSheetContent(
        customersWithDebt = customersWithDebt,
        initialCustomerId = initialCustomerId,
        onDismiss = onDismiss
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuickBillCustomerBottomSheet(
    customerId: String,
    customerName: String,
    customerPhone: String? = null,
    receivables: List<Receivable>,
    onDismiss: () -> Unit
) {
    val today = remember { LocalDate.now() }
    val unpaidList = remember(receivables) {
        receivables.filter { it.remainingAmount > BigDecimal.ZERO }
    }
    val nearestDue = remember(unpaidList) {
        unpaidList.mapNotNull {
            runCatching { LocalDate.parse(it.dueDate.take(10)) }.getOrNull()
        }.minOrNull()?.toString()
    }
    val overdueCount = remember(unpaidList) {
        unpaidList.count {
            val d = runCatching { LocalDate.parse(it.dueDate.take(10)) }.getOrNull()
            d != null && d.isBefore(today)
        }
    }
    val customerItem = remember(customerId, customerName, customerPhone, unpaidList, nearestDue, overdueCount) {
        QuickBillCustomerItem(
            id = customerId,
            name = customerName.toTitleCase(),
            phone = customerPhone,
            totalRemaining = unpaidList.sumOf { it.remainingAmount },
            unpaidInvoices = unpaidList,
            overdueCount = overdueCount,
            nearestDue = nearestDue
        )
    }

    QuickBillBottomSheetContent(
        customersWithDebt = listOf(customerItem),
        initialCustomerId = customerId,
        onDismiss = onDismiss
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickBillBottomSheetContent(
    customersWithDebt: List<QuickBillCustomerItem>,
    initialCustomerId: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedCustomerId by remember(initialCustomerId, customersWithDebt) {
        mutableStateOf(initialCustomerId ?: customersWithDebt.firstOrNull()?.id)
    }

    val selectedCustomer = customersWithDebt.firstOrNull { it.id == selectedCustomerId }
        ?: customersWithDebt.firstOrNull()

    // Default mode format
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
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(5.dp)
                        .background(Color(0xFFCBD5E1), CircleShape)
                )
            }
        },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Tagih via WhatsApp",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Kirim rincian tagihan langsung ke nomor pelanggan",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Tutup",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            if (customersWithDebt.isEmpty()) {
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
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
            } else if (selectedCustomer != null) {
                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Selection chip if > 1 customer
                    if (customersWithDebt.size > 1) {
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
                                    color = if (isSelected) Color(0xFF10533C) else Color(0xFFF8FAFC),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFF10533C) else Color(0xFFE2E8F0)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
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
                                            color = if (isSelected) Color(0xFFD1FAE5) else Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section Customer Card
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "PILIH PELANGGAN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.8.sp
                        )

                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shadowElevation = 0.5.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                // Top Row: Avatar + Name + Phone and Total Piutang
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(Color(0xFF10533C).copy(alpha = 0.10f), CircleShape)
                                                .border(
                                                    BorderStroke(1.dp, Color(0xFF10533C).copy(alpha = 0.20f)),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = getInitials(selectedCustomer.name),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF10533C)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = selectedCustomer.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A),
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = selectedCustomer.phone ?: "No. WhatsApp belum diisi",
                                                fontSize = 12.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Total Piutang",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF64748B)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = selectedCustomer.totalRemaining.currencyText(),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10533C)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                                Spacer(modifier = Modifier.height(10.dp))

                                // Bottom Row: Unpaid Invoices count + Due Date
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                            contentDescription = null,
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "${selectedCustomer.unpaidInvoices.size} Nota Belum Lunas",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF334155)
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Jatuh Tempo:",
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                        val formattedDue = formatDateIndo(selectedCustomer.nearestDue)
                                        val isOverdue = selectedCustomer.overdueCount > 0
                                        Text(
                                            text = formattedDue,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isOverdue) Color(0xFFE11D48) else Color(0xFF334155)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section Format Pesan
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "FORMAT PESAN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.8.sp
                        )

                        Box(modifier = Modifier.fillMaxWidth()) {
                            HorizontalDivider(
                                color = Color(0xFFE2E8F0),
                                thickness = 1.dp,
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )

                            Row(modifier = Modifier.fillMaxWidth()) {
                                QuickBillFormatMode.entries.forEach { mode ->
                                    val isSelected = formatMode == mode
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                if (formatMode != mode) {
                                                    formatMode = mode
                                                    editedMessageText = buildWhatsAppReminderText(
                                                        customerName = selectedCustomer.name,
                                                        totalRemainingText = selectedCustomer.totalRemaining.currencyText(),
                                                        unpaidInvoices = selectedCustomer.unpaidInvoices,
                                                        overdueCount = selectedCustomer.overdueCount,
                                                        nearestDueDate = selectedCustomer.nearestDue,
                                                        mode = mode
                                                    )
                                                }
                                            },
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = mode.label,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                            color = if (isSelected) Color(0xFF10533C) else Color(0xFF64748B),
                                            modifier = Modifier.padding(bottom = 10.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(2.dp)
                                                .background(if (isSelected) Color(0xFF10533C) else Color.Transparent)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section Pratinjau Pesan
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pratinjau Pesan",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Tombol Reset
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            editedMessageText = buildWhatsAppReminderText(
                                                customerName = selectedCustomer.name,
                                                totalRemainingText = selectedCustomer.totalRemaining.currencyText(),
                                                unpaidInvoices = selectedCustomer.unpaidInvoices,
                                                overdueCount = selectedCustomer.overdueCount,
                                                nearestDueDate = selectedCustomer.nearestDue,
                                                mode = formatMode
                                            )
                                            Toast.makeText(context, "Format pesan dipulihkan", Toast.LENGTH_SHORT).show()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Refresh,
                                        contentDescription = "Pulihkan format awal",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Tombol Copy
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            copyTextToClipboard(context, "Pesan Tagihan", editedMessageText)
                                            Toast.makeText(context, "Teks pesan berhasil disalin!", Toast.LENGTH_SHORT).show()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = "Salin teks",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Message Text Box
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BasicTextField(
                                value = editedMessageText,
                                onValueChange = { editedMessageText = it },
                                textStyle = TextStyle(
                                    fontSize = 12.sp,
                                    color = Color(0xFF1E293B),
                                    lineHeight = 18.sp,
                                    fontFamily = FontFamily.Default
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                                    .heightIn(min = 160.dp, max = 280.dp),
                                cursorBrush = SolidColor(Color(0xFF10533C))
                            )
                        }
                    }
                }

                // Sticky Bottom CTA (Footer)
                Surface(
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Button(
                            onClick = {
                                shareReminderViaWhatsApp(
                                    context = context,
                                    message = editedMessageText,
                                    phoneNumber = selectedCustomer.phone
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10533C),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_whatsapp),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Kirim ke WhatsApp",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Format teks pengingat WhatsApp sesuai format mockup resmi:
 * - Summary: Rangkuman ringkas dan ramah.
 * - Detailed: Rincian nota terstruktur dengan nomor nota, tanggal, jatuh tempo, dan nominal.
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
    val formattedNearestDue = formatDateIndo(nearestDueDate)

    return when (mode) {
        QuickBillFormatMode.Summary -> {
            buildString {
                appendLine("Halo Bapak/Ibu $customerName,")
                appendLine()
                appendLine("Kami menginformasikan rekap tagihan piutang Anda di $storeName:")
                appendLine()
                appendLine("• Total Sisa Tagihan: $totalRemainingText")
                appendLine("• Jumlah Nota: ${unpaidInvoices.size} Nota Belum Lunas")
                appendLine("• Jatuh Tempo Terdekat: $formattedNearestDue")
                appendLine()
                appendLine("Pembayaran dapat dilakukan langsung di kasir toko atau transfer rekening.")
                appendLine()
                appendLine("Terima kasih atas kerja sama dan kepercayaannya.")
                append(storeName)
            }
        }
        QuickBillFormatMode.Detailed -> {
            val sortedInvoices = unpaidInvoices.sortedBy { it.dueDate }
            buildString {
                appendLine("*PENGINGAT TAGIHAN PIUTANG*")
                appendLine("*$storeName*")
                appendLine("───────────────────────────────")
                appendLine()
                appendLine("Yth. *Bapak/Ibu $customerName*,")
                appendLine()
                appendLine("Semoga Bapak/Ibu senantiasa dalam keadaan sehat dan lancar rezeki.")
                appendLine()
                appendLine("Kami menginformasikan rincian nota piutang yang tercatat di toko kami:")
                appendLine()
                appendLine("📋 *RINCIAN NOTA:*")
                if (sortedInvoices.size <= 1) {
                    val inv = sortedInvoices.firstOrNull()
                    val ref = inv?.legacyInvoiceNumber?.takeIf(String::isNotBlank)
                        ?: inv?.transactionId?.takeIf(String::isNotBlank)?.let {
                            if (it.length > 12) "INV-${it.take(8).uppercase()}" else it
                        }
                        ?: "INV-${inv?.id?.take(8)?.uppercase() ?: "0000"}"
                    val invNo = if (ref.startsWith("#")) ref else "#$ref"
                    val date = formatDateIndo(inv?.debtDate?.ifBlank { inv.createdAt })
                    val due = formatDateIndo(inv?.dueDate)
                    val amount = inv?.remainingAmount?.currencyText() ?: totalRemainingText

                    appendLine("• No. Nota : *$invNo*")
                    appendLine("• Tanggal  : $date")
                    appendLine("• Jth. Tempo: *$due*")
                    appendLine("• Nominal  : *$amount*")
                } else {
                    sortedInvoices.forEachIndexed { idx, inv ->
                        val ref = inv.legacyInvoiceNumber?.takeIf(String::isNotBlank)
                            ?: inv.transactionId?.takeIf(String::isNotBlank)?.let {
                                if (it.length > 12) "INV-${it.take(8).uppercase()}" else it
                            }
                            ?: "INV-${inv.id.take(8).uppercase()}"
                        val invNo = if (ref.startsWith("#")) ref else "#$ref"
                        val date = formatDateIndo(inv.debtDate.ifBlank { inv.createdAt })
                        val due = formatDateIndo(inv.dueDate)
                        val amount = inv.remainingAmount.currencyText()

                        appendLine("${idx + 1}. No. Nota : *$invNo*")
                        appendLine("   • Tanggal  : $date")
                        appendLine("   • Jth. Tempo: *$due*")
                        appendLine("   • Nominal  : *$amount*")
                        if (idx < sortedInvoices.lastIndex) appendLine()
                    }
                }
                appendLine()
                appendLine("💰 *TOTAL TAGIHAN : $totalRemainingText*")
                appendLine()
                appendLine("💳 *Metode Pembayaran:*")
                appendLine("1. Langsung di kasir toko *$storeName*")
                appendLine("2. Transfer via Bank/QRIS (hubungi kami untuk rekening tujuan)")
                appendLine()
                appendLine("_Apabila telah melakukan pembayaran, mohon konfirmasi bukti transfer atau abaikan pesan ini._")
                appendLine()
                appendLine("Terima kasih atas kepercayaan dan kerja samanya.")
                appendLine()
                appendLine("Salam hangat,")
                append("*Kasir $storeName*")
            }
        }
    }
}

private fun String.toTitleCase(): String {
    return split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}

private fun getInitials(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "PL"
    val parts = trimmed.split("\\s+".toRegex()).filter { it.isNotEmpty() }
    return when {
        parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
        parts.isNotEmpty() -> parts[0].take(2).uppercase()
        else -> "PL"
    }
}

private fun formatDateIndo(dateStr: String?): String {
    if (dateStr.isNullOrBlank()) return "-"
    return runCatching {
        val parsed = LocalDate.parse(dateStr.take(10))
        val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id-ID"))
        parsed.format(formatter)
    }.getOrDefault(dateStr.take(10))
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
