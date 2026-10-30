package com.tbterminal.app.ui.salestransactions

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tbterminal.app.TbTerminalApplication
import com.tbterminal.app.data.model.CashTransactionDetail
import com.tbterminal.app.data.model.receiptNumber
import com.tbterminal.app.data.printer.DeliveryOrderFormatter
import com.tbterminal.app.data.printer.PrinterResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.RoundingMode

@Composable
fun DeliveryOrderDialog(
    transaction: CashTransactionDetail,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPrinting by remember { mutableStateOf(false) }
    var showWhatsAppConfirm by remember { mutableStateOf(false) }

    val deliveryInfo = remember(transaction.notes) {
        DeliveryOrderFormatter.parseDeliveryInfo(transaction.notes)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.LocalShipping,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Surat Jalan / Pengiriman",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Tutup",
                            tint = Color(0xFF64748B)
                        )
                    }
                }

                // Kertas Simulasi Slip Surat Jalan (Putih dengan Border Halus)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "TB TERMINAL",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "*** SURAT JALAN & PENGIRIMAN ***",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0284C7),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))

                        // Metadata
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("No. Nota", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(transaction.receiptNumber(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Penerima", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(transaction.customerName ?: "Pelanggan Umum", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Alamat Kirim", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(
                                text = deliveryInfo.address?.ifBlank { null } ?: "Alamat Belum Terisi",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color.Black,
                                maxLines = 2
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Armada / Supir", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(deliveryInfo.courier?.ifBlank { null } ?: "Supir Toko", fontSize = 11.sp, color = Color.Black)
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))

                        // Tabel Fisik Barang (Centang / Checkbox)
                        Text(
                            text = "DAFTAR BARANG YANG DIKIRIM:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )

                        transaction.items.forEach { item ->
                            val qtyStr = if (item.quantity.stripTrailingZeros().scale() <= 0) {
                                item.quantity.setScale(0, RoundingMode.HALF_UP).toPlainString()
                            } else {
                                item.quantity.stripTrailingZeros().toPlainString()
                            }
                            val unitStr = item.unitName?.takeIf { it.isNotBlank() } ?: "pcs"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "[ ]",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0284C7),
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = "$qtyStr $unitStr",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.width(60.dp)
                                )
                                Text(
                                    text = item.productName,
                                    fontSize = 11.sp,
                                    color = Color(0xFF1E293B)
                                )
                            }
                        }

                        if (deliveryInfo.deliveryFeeText != null) {
                            HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Biaya Pengiriman", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text(deliveryInfo.deliveryFeeText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 6.dp))

                        // Kolom Tanda Tangan
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Pengirim", fontSize = 10.sp, color = Color(0xFF64748B))
                                Spacer(Modifier.height(24.dp))
                                Text("( .......... )", fontSize = 10.sp, color = Color.Black)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Sopir/Armada", fontSize = 10.sp, color = Color(0xFF64748B))
                                Spacer(Modifier.height(24.dp))
                                Text("( .......... )", fontSize = 10.sp, color = Color.Black)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Penerima", fontSize = 10.sp, color = Color(0xFF64748B))
                                Spacer(Modifier.height(24.dp))
                                Text("( .......... )", fontSize = 10.sp, color = Color.Black)
                            }
                        }
                    }
                }

                // Tombol Aksi
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Cetak Thermal
                    Button(
                        onClick = {
                            if (isPrinting) return@Button
                            isPrinting = true
                            scope.launch {
                                val app = context.applicationContext as? TbTerminalApplication
                                val printerManager = app?.appContainer?.thermalPrinterManager
                                if (printerManager == null) {
                                    Toast.makeText(context, "Layanan printer belum siap", Toast.LENGTH_SHORT).show()
                                    isPrinting = false
                                    return@launch
                                }
                                val paperSize = withContext(Dispatchers.IO) { printerManager.getPaperSize() }
                                val bytes = DeliveryOrderFormatter.buildDeliveryOrderBytes(
                                    transaction = transaction,
                                    storeName = "TB TERMINAL",
                                    paperSize = paperSize
                                )
                                val result = printerManager.printCustomBytes(context, bytes)
                                isPrinting = false
                                when (result) {
                                    is PrinterResult.Success -> {
                                        Toast.makeText(context, "Surat Jalan berhasil dicetak!", Toast.LENGTH_SHORT).show()
                                    }
                                    is PrinterResult.Error -> {
                                        Toast.makeText(context, "Gagal mencetak: ${result.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        enabled = !isPrinting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        if (isPrinting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Mencetak Surat Jalan...", color = Color.White)
                        } else {
                            Icon(Icons.Outlined.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Cetak Surat Jalan (Thermal)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    // 2. Kirim via WhatsApp
                    Button(
                        onClick = { showWhatsAppConfirm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen)
                    ) {
                        Icon(Icons.Outlined.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Kirim Surat Jalan ke WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    if (showWhatsAppConfirm) {
        val waText = remember(transaction) {
            DeliveryOrderFormatter.buildDeliveryOrderWhatsAppText(transaction)
        }
        WhatsAppReceiptDialog(
            transaction = transaction,
            onDismissRequest = { showWhatsAppConfirm = false },
            onSend = { phone ->
                showWhatsAppConfirm = false
                val formattedPhone = WhatsAppReceiptHelper.formatPhoneNumber(phone)
                try {
                    val encodedText = java.net.URLEncoder.encode(waText, "UTF-8")
                    val uri = if (formattedPhone.isNotBlank()) {
                        android.net.Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedText")
                    } else {
                        android.net.Uri.parse("https://api.whatsapp.com/send?text=$encodedText")
                    }
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(context, "Aplikasi WhatsApp tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}
