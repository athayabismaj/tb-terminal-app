package com.tbterminal.app.ui.cash

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
import androidx.compose.material.icons.outlined.Payments
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
import com.tbterminal.app.data.model.CashSession
import com.tbterminal.app.data.printer.CashSessionSummaryFormatter
import com.tbterminal.app.data.printer.PrinterResult
import com.tbterminal.app.ui.salestransactions.WhatsAppGreen
import com.tbterminal.app.ui.salestransactions.WhatsAppReceiptHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@Composable
fun ShiftSettlementDialog(
    session: CashSession,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPrinting by remember { mutableStateOf(false) }

    fun formatMoney(amount: BigDecimal?): String {
        val safeAmount = amount ?: BigDecimal.ZERO
        val symbols = DecimalFormatSymbols(Locale("id", "ID")).apply { groupingSeparator = '.' }
        val formatter = DecimalFormat("#,##0", symbols)
        return "Rp " + formatter.format(safeAmount)
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
                            imageVector = Icons.Outlined.Payments,
                            contentDescription = null,
                            tint = Color(0xFF0D9488),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Rekapitulasi Shift Kasir",
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

                // Slip Visual
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
                            text = "*** REKAPITULASI PENUTUPAN KAS ***",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0D9488),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Kasir", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(session.userName ?: session.userId, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Status Sesi", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(session.status.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (session.status.equals("closed", true)) Color(0xFF047857) else Color(0xFFD97706))
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Modal Kas Awal", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(formatMoney(session.openingCash), fontSize = 11.sp, color = Color.Black)
                        }
                        val systemCash = session.systemCash ?: session.openingCash
                        val cashSales = systemCash.subtract(session.openingCash).coerceAtLeast(BigDecimal.ZERO)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Penjualan Tunai", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(formatMoney(cashSales), fontSize = 11.sp, color = Color(0xFF047857))
                        }
                        if (session.totalExpenses > BigDecimal.ZERO) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Pengeluaran Kas", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text("- " + formatMoney(session.totalExpenses), fontSize = 11.sp, color = Color(0xFFDC2626))
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Kas Diharapkan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                            Text(formatMoney(systemCash), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Kas Fisik Nyata", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                            Text(formatMoney(session.closingCash ?: systemCash), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                        }

                        val difference = session.difference ?: BigDecimal.ZERO
                        val diffColor = when {
                            difference.compareTo(BigDecimal.ZERO) > 0 -> Color(0xFF047857)
                            difference.compareTo(BigDecimal.ZERO) < 0 -> Color(0xFFDC2626)
                            else -> Color(0xFF475569)
                        }
                        val diffText = when {
                            difference.compareTo(BigDecimal.ZERO) > 0 -> "Lebih: +" + formatMoney(difference.abs())
                            difference.compareTo(BigDecimal.ZERO) < 0 -> "Kurang: -" + formatMoney(difference.abs())
                            else -> "Pas (Rp 0)"
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Selisih Kas", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = diffColor)
                            Text(diffText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = diffColor)
                        }

                        if (!session.notes.isNullOrBlank()) {
                            HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))
                            Text("Catatan: ${session.notes}", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }
                }

                // Tombol Aksi
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Cetak Slip Shift
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
                                val bytes = CashSessionSummaryFormatter.buildShiftSettlementBytes(
                                    session = session,
                                    storeName = "TB TERMINAL",
                                    paperSize = paperSize
                                )
                                val result = printerManager.printCustomBytes(context, bytes)
                                isPrinting = false
                                when (result) {
                                    is PrinterResult.Success -> {
                                        Toast.makeText(context, "Slip Shift berhasil dicetak!", Toast.LENGTH_SHORT).show()
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
                    ) {
                        if (isPrinting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Mencetak Slip Shift...", color = Color.White)
                        } else {
                            Icon(Icons.Outlined.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Cetak Slip Shift (Thermal)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    // 2. Kirim WhatsApp ke Owner
                    Button(
                        onClick = {
                            val text = CashSessionSummaryFormatter.buildShiftSettlementWhatsAppText(session)
                            try {
                                val encoded = java.net.URLEncoder.encode(text, "UTF-8")
                                val uri = android.net.Uri.parse("https://api.whatsapp.com/send?text=$encoded")
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri).apply {
                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "WhatsApp tidak terpasang di perangkat", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen)
                    ) {
                        Icon(Icons.Outlined.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Kirim Laporan Shift ke WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
