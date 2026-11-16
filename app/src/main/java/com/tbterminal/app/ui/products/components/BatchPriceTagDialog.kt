package com.tbterminal.app.ui.products.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.printer.PriceTagItem
import com.tbterminal.app.data.printer.PriceTagPrintHelper
import com.tbterminal.app.data.printer.PrinterResult
import com.tbterminal.app.data.printer.ThermalPrinterManager
import com.tbterminal.app.ui.common.toRupiahText
import kotlinx.coroutines.launch

private val BatchGreen = Color(0xFF256B57)
private val BatchTagBg = Color(0xFFFEF9C3)
private val BatchTagBorder = Color(0xFFFDE047)

internal data class BatchPriceTagEntry(
    val product: ProductStock,
    val copies: Int = 1,
)

internal fun toggleProductInPrintQueue(
    queue: Map<String, BatchPriceTagEntry>,
    product: ProductStock,
): Map<String, BatchPriceTagEntry> {
    return if (product.productId in queue) {
        queue - product.productId
    } else {
        queue + (product.productId to BatchPriceTagEntry(product = product, copies = 1))
    }
}

internal fun updatePrintQueueCopies(
    queue: Map<String, BatchPriceTagEntry>,
    productId: String,
    copies: Int,
): Map<String, BatchPriceTagEntry> {
    val existing = queue[productId] ?: return queue
    val clamped = copies.coerceIn(1, 50)
    return queue + (productId to existing.copy(copies = clamped))
}

internal fun selectAllProductsForPrintQueue(
    queue: Map<String, BatchPriceTagEntry>,
    products: List<ProductStock>,
): Map<String, BatchPriceTagEntry> {
    val next = queue.toMutableMap()
    products.filter { it.isActive }.forEach { product ->
        if (product.productId !in next) {
            next[product.productId] = BatchPriceTagEntry(product = product, copies = 1)
        }
    }
    return next
}

internal fun totalPrintQueueLabels(queue: Map<String, BatchPriceTagEntry>): Int =
    queue.values.sumOf { it.copies.coerceIn(1, 50) }

internal fun toPriceTagItems(queue: Map<String, BatchPriceTagEntry>): List<PriceTagItem> =
    queue.values.map { entry ->
        PriceTagItem(
            productId = entry.product.productId,
            productName = entry.product.productName,
            sku = entry.product.sku,
            barcode = entry.product.sku,
            price = entry.product.priceRetail,
            unitName = entry.product.unitName.ifBlank { "Pcs" },
            copies = entry.copies.coerceIn(1, 50),
        )
    }

@Composable
fun BatchPriceTagDialog(
    products: List<ProductStock>,
    storeName: String = "TB TERMINAL",
    thermalPrinterManager: ThermalPrinterManager? = null,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var filterQuery by remember { mutableStateOf("") }
    var queue by remember { mutableStateOf<Map<String, BatchPriceTagEntry>>(emptyMap()) }
    var isPrinting by remember { mutableStateOf(false) }

    val filteredProducts = remember(products, filterQuery) {
        val q = filterQuery.trim()
        val base = products.filter { it.isActive }
        if (q.isEmpty()) base
        else base.filter {
            it.productName.contains(q, ignoreCase = true) ||
                it.sku.contains(q, ignoreCase = true) ||
                it.categoryName.contains(q, ignoreCase = true)
        }
    }
    val totalLabels = totalPrintQueueLabels(queue)
    val firstSelected = queue.values.firstOrNull()?.product
    val previewBitmap = remember(firstSelected?.sku) {
        firstSelected?.sku?.let { PriceTagPrintHelper.generateCode128Bitmap(it, widthPx = 280, heightPx = 56) }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .padding(8.dp),
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Antrean Cetak Barcode Massal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "${queue.size} produk dipilih • $totalLabels lembar label",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    IconButton(onClick = onDismissRequest, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Close, contentDescription = "Tutup", tint = Color(0xFF64748B))
                    }
                }

                OutlinedTextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    placeholder = { Text("Cari nama produk atau SKU...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { queue = selectAllProductsForPrintQueue(queue, filteredProducts) },
                        enabled = filteredProducts.isNotEmpty()
                    ) {
                        Text("Pilih Semua (${filteredProducts.size})", color = BatchGreen, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(
                        onClick = { queue = emptyMap() },
                        enabled = queue.isNotEmpty()
                    ) {
                        Text("Bersihkan", color = Color(0xFFDC2626), fontWeight = FontWeight.SemiBold)
                    }
                }

                if (firstSelected != null && previewBitmap != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BatchTagBg,
                        border = BorderStroke(1.dp, BatchTagBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Pratinjau: ${firstSelected.productName}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${firstSelected.priceRetail.toRupiahText()} / ${firstSelected.unitName.ifBlank { "Pcs" }} • SKU ${firstSelected.sku}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF475569)
                                )
                            }
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = "Pratinjau Barcode",
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(28.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                if (filteredProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Tidak ada produk aktif yang cocok.", color = Color(0xFF64748B), fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp, max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredProducts, key = { it.productId }) { product ->
                            val entry = queue[product.productId]
                            val selected = entry != null
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (selected) BatchGreen.copy(alpha = 0.4f) else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { queue = toggleProductInPrintQueue(queue, product) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Checkbox(
                                        checked = selected,
                                        onCheckedChange = { queue = toggleProductInPrintQueue(queue, product) },
                                        colors = CheckboxDefaults.colors(checkedColor = BatchGreen)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product.productName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "SKU: ${product.sku} • ${product.priceRetail.toRupiahText()}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (entry != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            IconButton(
                                                onClick = { queue = updatePrintQueueCopies(queue, product.productId, entry.copies - 1) },
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(Color.White, RoundedCornerShape(6.dp))
                                            ) {
                                                Icon(Icons.Outlined.Remove, contentDescription = "Kurangi lembar", modifier = Modifier.size(14.dp))
                                            }
                                            Text(
                                                text = "${entry.copies}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A),
                                                modifier = Modifier.padding(horizontal = 4.dp)
                                            )
                                            IconButton(
                                                onClick = { queue = updatePrintQueueCopies(queue, product.productId, entry.copies + 1) },
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(Color.White, RoundedCornerShape(6.dp))
                                            ) {
                                                Icon(Icons.Outlined.Add, contentDescription = "Tambah lembar", modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val tagItems = toPriceTagItems(queue)
                            PriceTagPrintHelper.printPriceTagSheetPdf(
                                context = context,
                                items = tagItems,
                                storeName = storeName,
                            )
                        },
                        enabled = queue.isNotEmpty() && !isPrinting,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BatchGreen.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, tint = BatchGreen, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("PDF A4", color = BatchGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            val tagItems = toPriceTagItems(queue)
                            isPrinting = true
                            coroutineScope.launch {
                                when (
                                    val result = PriceTagPrintHelper.printThermalLabels(
                                        context = context,
                                        items = tagItems,
                                        storeName = storeName,
                                        thermalPrinterManager = thermalPrinterManager,
                                    )
                                ) {
                                    is PrinterResult.Success -> {
                                        Toast.makeText(
                                            context,
                                            "$totalLabels label barcode berhasil dikirim ke printer thermal.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onDismissRequest()
                                    }
                                    is PrinterResult.Error -> {
                                        Toast.makeText(context, "Gagal mencetak: ${result.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                                isPrinting = false
                            }
                        },
                        enabled = queue.isNotEmpty() && !isPrinting,
                        modifier = Modifier
                            .weight(1.25f)
                            .height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BatchGreen),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        if (isPrinting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Outlined.Print, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Thermal ($totalLabels)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
