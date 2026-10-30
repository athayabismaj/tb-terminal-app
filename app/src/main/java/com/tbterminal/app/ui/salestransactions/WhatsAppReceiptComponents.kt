package com.tbterminal.app.ui.salestransactions

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.data.model.receiptNumber
import com.tbterminal.app.data.model.remainingAmount
import com.tbterminal.app.ui.common.toDisplayDateTime
import com.tbterminal.app.ui.common.toRupiahText
import java.math.BigDecimal
import java.net.URLEncoder

val WhatsAppGreen = Color(0xFF25D366)
val WhatsAppDarkGreen = Color(0xFF128C7E)

/**
 * Utilitas helper untuk membuat teks struk WhatsApp yang rapi dan memicu Intent pengiriman.
 */
object WhatsAppReceiptHelper {

    fun formatPhoneNumber(input: String): String {
        val digits = input.filter(Char::isDigit)
        return when {
            digits.startsWith("0") -> "62" + digits.substring(1)
            digits.startsWith("62") -> digits
            digits.isNotBlank() -> "62$digits"
            else -> ""
        }
    }

    fun buildWhatsAppReceiptText(
        transaction: CashTransactionDetail,
        storeName: String = "TB TERMINAL"
    ): String {
        val isVoided = transaction.status.equals("voided", true)
        val remaining = transaction.remainingAmount()
        val isSettled = !isVoided && remaining <= BigDecimal.ZERO
        val statusText = when {
            isVoided -> "BATAL / VOID"
            isSettled -> "LUNAS"
            transaction.paymentMethods.any { it.lowercase() in setOf("dp", "partial") } -> "DP / UANG MUKA"
            transaction.paymentMethods.any { it.lowercase() in setOf("hutang", "credit") } -> "HUTANG"
            else -> transaction.status.uppercase()
        }

        val sb = StringBuilder()
        sb.appendLine("*$storeName*")
        sb.appendLine("Nota Transaksi Pembelian Digital")
        sb.appendLine("================================")
        sb.appendLine("No. Struk : ${transaction.receiptNumber()}")
        sb.appendLine("Waktu     : ${transaction.createdAt.toDisplayDateTime()}")
        sb.appendLine("Kasir     : ${transaction.cashierName ?: "-"}")
        sb.appendLine("Pelanggan : ${transaction.customerName ?: "Pelanggan Umum"}")
        sb.appendLine("Status    : $statusText")
        sb.appendLine("--------------------------------")
        sb.appendLine("*RINCIAN BELANJA:*")
        transaction.items.forEach { item ->
            val qtyText = if (item.quantity.stripTrailingZeros().scale() <= 0) {
                item.quantity.setScale(0).toPlainString()
            } else {
                item.quantity.stripTrailingZeros().toPlainString()
            }
            sb.appendLine("• ${item.productName.ifBlank { "Barang" }}")
            sb.appendLine("  $qtyText x ${item.priceAtTransaction.toRupiahText()} = ${item.subtotal.toRupiahText()}")
        }
        sb.appendLine("--------------------------------")
        sb.appendLine("*TOTAL BELANJA : ${transaction.total.toRupiahText()}*")

        val methods = transaction.paymentMethods.joinToString(" + ") { m ->
            when (m.lowercase()) {
                "tunai", "cash" -> "Tunai"
                "qris" -> "QRIS"
                "transfer", "bank_transfer" -> "Transfer"
                "hutang", "credit" -> "Hutang"
                "dp", "down_payment" -> "DP"
                else -> m.replaceFirstChar { it.uppercase() }
            }
        }.ifBlank { "Tunai" }

        sb.appendLine("Metode Bayar  : $methods")
        sb.appendLine("Uang Diterima : ${transaction.paidAmount.toRupiahText()}")
        if (transaction.changeAmount > BigDecimal.ZERO) {
            sb.appendLine("Kembalian     : ${transaction.changeAmount.toRupiahText()}")
        }
        if (remaining > BigDecimal.ZERO) {
            sb.appendLine("Sisa Tagihan  : ${remaining.toRupiahText()}")
        }
        sb.appendLine("================================")
        sb.appendLine("Terima kasih telah berbelanja di *$storeName*!")
        sb.appendLine("Simpan pesan ini sebagai bukti transaksi sah.")
        return sb.toString()
    }

    fun sendViaWhatsApp(
        context: Context,
        transaction: CashTransactionDetail,
        targetPhoneNumber: String? = null,
        storeName: String = "TB TERMINAL"
    ) {
        val text = buildWhatsAppReceiptText(transaction, storeName)
        val formattedPhone = targetPhoneNumber?.let { formatPhoneNumber(it) }.orEmpty()

        try {
            val url = if (formattedPhone.isNotBlank()) {
                "https://api.whatsapp.com/send?phone=$formattedPhone&text=${URLEncoder.encode(text, "UTF-8")}"
            } else {
                "https://api.whatsapp.com/send?text=${URLEncoder.encode(text, "UTF-8")}"
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage("com.whatsapp")
            }

            // Jika WhatsApp terpasang, buka langsung
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Fallback jika com.whatsapp tidak langsung match (misal WhatsApp Business / browser)
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(fallbackIntent)
            }
        } catch (_: Exception) {
            // Fallback kedua: Android share chooser
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            val chooser = Intent.createChooser(sendIntent, "Kirim Struk via")
            context.startActivity(chooser)
        }
    }
}

/**
 * Dialog input nomor WhatsApp pelanggan sebelum mengirim struk digital.
 */
@Composable
fun WhatsAppReceiptDialog(
    transaction: CashTransactionDetail,
    defaultPhoneNumber: String = "",
    storeName: String = "TB TERMINAL",
    onDismissRequest: () -> Unit,
    onSend: (String) -> Unit
) {
    var phoneInput by remember { mutableStateOf(defaultPhoneNumber) }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = WhatsAppGreen.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Phone,
                                    contentDescription = null,
                                    tint = WhatsAppDarkGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Kirim Struk WhatsApp",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Tutup",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFE2E8F0))

                Text(
                    text = "Nota untuk transaksi ${transaction.receiptNumber()} sebesar ${transaction.total.toRupiahText()} akan dikirim langsung ke WhatsApp pembeli.",
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' || ch == '-' } },
                    label = { Text("Nomor WhatsApp Pelanggan") },
                    placeholder = { Text("Contoh: 08123456789") },
                    leadingIcon = {
                        Text(
                            "+62",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = WhatsAppDarkGreen,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsAppGreen,
                        focusedLabelColor = WhatsAppDarkGreen
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onSend(phoneInput) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (phoneInput.isBlank()) "Kirim ke Kontak WhatsApp" else "Kirim Pesan WhatsApp",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Text("Batal", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
