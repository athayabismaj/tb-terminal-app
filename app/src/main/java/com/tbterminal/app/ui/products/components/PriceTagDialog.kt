package com.tbterminal.app.ui.products.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.tbterminal.app.data.printer.BluetoothPrinterDriver
import com.tbterminal.app.data.printer.PriceTagLabelFormatter
import com.tbterminal.app.data.printer.PrinterResult
import com.tbterminal.app.ui.common.toRupiahText
import java.math.BigDecimal
import kotlinx.coroutines.launch

private val BrandGreen = Color(0xFF256B57)
private val TagYellow = Color(0xFFFEF9C3)
private val TagYellowBorder = Color(0xFFFDE047)

/**
 * Dialog pratinjau dan pencetak stiker label harga rak toko (Shelf Price Tag)
 * dengan grafik barcode 1D / QR code dan format ESC/POS printer thermal.
 */
@Composable
fun PriceTagDialog(
    productName: String,
    sku: String,
    barcode: String? = null,
    price: BigDecimal,
    unitName: String = "Pcs",
    storeName: String = "TB TERMINAL",
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var copies by remember { mutableIntStateOf(1) }
    var isPrinting by remember { mutableStateOf(false) }

    val resolvedCode = sku.ifBlank { barcode.orEmpty() }.ifBlank { "000000" }

    // Generate Barcode 1D (Code 128) Bitmap
    val barcodeBitmap = remember(resolvedCode) {
        generateCode128Bitmap(resolvedCode)
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .padding(16.dp),
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Dialog
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cetak Label Rak & Barcode",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
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

                // Kartu Pratinjau Stiker Rak (Shelf Price Tag)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TagYellow,
                    border = BorderStroke(1.5.dp, TagYellowBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = storeName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF854D0E),
                            letterSpacing = 1.sp
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = productName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(8.dp))

                        // Tampilan Barcode 1D
                        if (barcodeBitmap != null) {
                            Image(
                                bitmap = barcodeBitmap.asImageBitmap(),
                                contentDescription = "Barcode",
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(48.dp)
                            )
                        }

                        Text(
                            text = resolvedCode,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155),
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        )

                        HorizontalDivider(color = TagYellowBorder, thickness = 1.dp)

                        Spacer(Modifier.height(8.dp))

                        // Kotak Harga Menonjol
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = price.toRupiahText(),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = BrandGreen
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "/ $unitName",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64748B),
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                    }
                }

                // Pengatur Jumlah Salinan (Copies)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Jumlah Lembar:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF334155)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = { if (copies > 1) copies-- },
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Outlined.Remove, contentDescription = "Kurang", modifier = Modifier.size(16.dp))
                        }

                        Text(
                            text = "$copies",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        IconButton(
                            onClick = { if (copies < 20) copies++ },
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Tombol Cetak ke Printer Thermal
                Button(
                    onClick = {
                        val pairedPrinters = BluetoothPrinterDriver.getPairedDevices(context)
                        if (pairedPrinters.isEmpty()) {
                            Toast.makeText(context, "Tidak ada printer Bluetooth yang terhubung. Silakan pasangkan printer di Pengaturan.", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        val printer = pairedPrinters.first()
                        isPrinting = true
                        coroutineScope.launch {
                            val bytes = PriceTagLabelFormatter.buildPriceTagBytes(
                                productName = productName,
                                sku = sku,
                                barcode = barcode,
                                price = price,
                                unitName = unitName,
                                storeName = storeName,
                                copies = copies
                            )

                            when (val result = BluetoothPrinterDriver.print(context, printer.address, bytes)) {
                                is PrinterResult.Success -> {
                                    Toast.makeText(context, "Label rak ($copies lembar) berhasil dicetak ke ${printer.name}.", Toast.LENGTH_SHORT).show()
                                    onDismissRequest()
                                }
                                is PrinterResult.Error -> {
                                    Toast.makeText(context, "Gagal mencetak: ${result.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                            isPrinting = false
                        }
                    },
                    enabled = !isPrinting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isPrinting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Print,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Cetak $copies Label Rak",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Text("Tutup", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun generateCode128Bitmap(content: String, widthPx: Int = 400, heightPx: Int = 100): Bitmap? {
    return runCatching {
        val clean = content.filter { it.code in 32..126 }.ifBlank { "000000" }
        val bitMatrix = MultiFormatWriter().encode(clean, BarcodeFormat.CODE_128, widthPx, heightPx)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE
            }
        }
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    }.getOrNull()
}
