package com.tbterminal.app.ui.incominggoods

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.model.Supplier
import java.text.NumberFormat
import java.util.Locale
import java.math.BigDecimal

import com.tbterminal.app.ui.components.TbMobileControlSheet

private val Brand50 = Color(0xFFF0F7F4)
private val Brand100 = Color(0xFFE1EFE9)
private val Brand600 = Color(0xFF256B57)
private val Brand700 = Color(0xFF1D5545)
private val Brand200 = Color(0xFFC5DFD6)

@Composable
internal fun IncomingGoodsFormBody(
    uiState: IncomingGoodsUiState,
    compact: Boolean,
    onSelectProduct: (ProductStock) -> Unit,
    onSelectSupplier: (Supplier) -> Unit,
    onSupplierNameChanged: (String) -> Unit,
    onCreateSupplier: () -> Unit,
    onInvoiceChanged: (String) -> Unit,
    onGenerateInvoiceNumber: () -> Unit,
    onQuantityChanged: (String) -> Unit,
    onBuyPriceChanged: (String) -> Unit,
    onAmountPaidChanged: (String) -> Unit,
    onDueDaysChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(if (compact) 16.dp else 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ProductSupplierCard(
                uiState = uiState,
                onSelectProduct = onSelectProduct,
                onSelectSupplier = onSelectSupplier,
                onSupplierNameChanged = onSupplierNameChanged,
                onCreateSupplier = onCreateSupplier
            )
        }
        item {
            PurchaseDetailsCard(
                uiState = uiState,
                onInvoiceChanged = onInvoiceChanged,
                onGenerateInvoiceNumber = onGenerateInvoiceNumber,
                onQuantityChanged = onQuantityChanged,
                onBuyPriceChanged = onBuyPriceChanged,
                onNotesChanged = onNotesChanged
            )
        }
        item {
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun ProductSupplierCard(
    uiState: IncomingGoodsUiState,
    onSelectProduct: (ProductStock) -> Unit,
    onSelectSupplier: (Supplier) -> Unit,
    onSupplierNameChanged: (String) -> Unit,
    onCreateSupplier: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 14.dp)) {
                Box(
                    modifier = Modifier.size(28.dp).background(Brand50, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = Brand600, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("Produk & Supplier", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            }
            
            // Product Selection
            Text("Produk", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B), modifier = Modifier.padding(bottom = 6.dp))
            ProductSelectionDropdown(uiState, onSelectProduct)
            
            if (uiState.selectedProduct != null) {
                Spacer(Modifier.height(12.dp))
                ProductInfoPill(uiState.selectedProduct)
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Supplier Selection
            Text("Supplier", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B), modifier = Modifier.padding(bottom = 6.dp))
            SupplierSection(uiState, onSelectSupplier, onSupplierNameChanged, onCreateSupplier)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductSelectionDropdown(
    uiState: IncomingGoodsUiState,
    onSelectProduct: (ProductStock) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = uiState.selectedProduct?.productName ?: "Pilih Produk..."
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedBorderColor = Brand600
            ),
            shape = RoundedCornerShape(12.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            uiState.products.forEach { product ->
                DropdownMenuItem(
                    text = { Text(product.productName, fontSize = 13.sp) },
                    onClick = {
                        onSelectProduct(product)
                        expanded = false
                    }
                )
            }
            if (uiState.products.isEmpty()) {
                DropdownMenuItem(text = { Text("Tidak ada produk", color = Color.Gray) }, onClick = { expanded = false })
            }
        }
    }
}

@Composable
private fun ProductInfoPill(product: ProductStock) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text(product.productName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text("${product.sku ?: "-"} • ${product.categoryName ?: "Umum"}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                }
                Box(modifier = Modifier.background(Brand50, RoundedCornerShape(12.dp)).border(1.dp, Brand100, RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("Tersedia", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Brand700)
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFE2E8F0).copy(alpha = 0.6f))
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f).background(Color.White, RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(8.dp)).padding(8.dp)) {
                    Text("Stok sekarang", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    Text("${product.quantity} ${product.unitName ?: "Pcs"}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                }
                Column(Modifier.weight(1f).background(Color.White, RoundedCornerShape(8.dp)).border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(8.dp)).padding(8.dp)) {
                    Text("Harga beli terakhir", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    Text(product.priceBuy.asCurrency(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SupplierSection(
    uiState: IncomingGoodsUiState,
    onSelectSupplier: (Supplier) -> Unit,
    onSupplierNameChanged: (String) -> Unit,
    onCreateSupplier: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = uiState.selectedSupplier?.name ?: "Pilih Supplier..."
    var showQuickAdd by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedBorderColor = Brand600
            ),
            shape = RoundedCornerShape(12.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            uiState.suppliers.forEach { sup ->
                DropdownMenuItem(
                    text = { Text(sup.name, fontSize = 13.sp) },
                    onClick = {
                        onSelectSupplier(sup)
                        expanded = false
                    }
                )
            }
        }
    }
    
    Spacer(Modifier.height(8.dp))
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { showQuickAdd = !showQuickAdd }.padding(vertical = 4.dp)
    ) {
        Icon(if(showQuickAdd) Icons.Outlined.Remove else Icons.Outlined.Add, contentDescription = null, tint = Brand600, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(if(showQuickAdd) "Batal tambah" else "Tambah supplier baru", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Brand600)
    }
    
    if (showQuickAdd) {
        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp)).border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)).padding(10.dp)) {
            OutlinedTextField(
                value = uiState.newSupplierNameInput,
                onValueChange = onSupplierNameChanged,
                placeholder = { Text("Nama supplier baru", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedBorderColor = Brand600
                ),
                shape = RoundedCornerShape(8.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color(0xFF1E293B)),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { 
                    onCreateSupplier()
                    showQuickAdd = false 
                },
                modifier = Modifier.fillMaxWidth().height(36.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Brand600),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Simpan supplier", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PurchaseDetailsCard(
    uiState: IncomingGoodsUiState,
    onInvoiceChanged: (String) -> Unit,
    onGenerateInvoiceNumber: () -> Unit,
    onQuantityChanged: (String) -> Unit,
    onBuyPriceChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 14.dp)) {
                Box(
                    modifier = Modifier.size(28.dp).background(Brand50, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = Brand600, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("Rincian Pembelian", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            }
            
            // Nomor Nota
            Text("Nomor nota", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B), modifier = Modifier.padding(bottom = 6.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.invoiceNoInput,
                    onValueChange = onInvoiceChanged,
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedBorderColor = Brand600
                    ),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B)),
                    singleLine = true
                )
                Button(
                    onClick = onGenerateInvoiceNumber,
                    modifier = Modifier.height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand50, contentColor = Brand700),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Brand100),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("Buat nomor", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text("Nomor nota dibuat otomatis dan tetap dapat diubah.", fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
            
            // Jumlah & Harga
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Jumlah masuk", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B), modifier = Modifier.padding(bottom = 6.dp))
                    OutlinedTextField(
                        value = uiState.quantityInput,
                        onValueChange = onQuantityChanged,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedBorderColor = Brand600
                        ),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B)),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        trailingIcon = { Text("Pcs", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium, modifier = Modifier.padding(end = 12.dp)) }
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text("Harga satuan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B), modifier = Modifier.padding(bottom = 6.dp))
                    OutlinedTextField(
                        value = uiState.buyPriceInput,
                        onValueChange = onBuyPriceChanged,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedBorderColor = Brand600
                        ),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B)),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        leadingIcon = { Text("Rp", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 12.dp)) }
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Ringkasan 
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brand50.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    .border(1.dp, Brand100, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Total pembelian", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569))
                        Text((uiState.total?.toDouble() ?: 0.0).asCurrency(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Brand700, letterSpacing = (-0.5).sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = Brand100.copy(alpha = 0.7f))
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Estimasi utang", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
                        Text("-", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF94A3B8))
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Catatan
            Text("Catatan (opsional)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B), modifier = Modifier.padding(bottom = 6.dp))
            OutlinedTextField(
                value = uiState.notesInput,
                onValueChange = onNotesChanged,
                placeholder = { Text("Contoh: restok gudang utama dari nota supplier", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                modifier = Modifier.fillMaxWidth().height(80.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedBorderColor = Brand600
                ),
                shape = RoundedCornerShape(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Color(0xFF1E293B)),
                maxLines = 3
            )
        }
    }
}

@Composable
internal fun IncomingGoodsSubmitButton(
    uiState: IncomingGoodsUiState,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onSubmit,
        modifier = modifier.height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Brand600),
        shape = RoundedCornerShape(12.dp),
        enabled = uiState.selectedProduct != null && uiState.selectedSupplier != null
    ) {
        Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Pilih Pembayaran", fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun IncomingPaymentBottomSheet(
    uiState: IncomingGoodsUiState,
    onPaymentMethodChanged: (IncomingPaymentMethod) -> Unit,
    onAmountPaidChanged: (String) -> Unit,
    onDueDaysChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    TbMobileControlSheet(
        title = "Konfirmasi Pembayaran",
        subtitle = "Pilih metode penyelesaian nota barang masuk",
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            
            // Summary Total Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brand100.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .border(1.dp, Brand200, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("TOTAL PEMBELIAN NOTA", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Brand700.copy(alpha = 0.7f), letterSpacing = 0.5.sp)
                        Text((uiState.total?.toDouble() ?: 0.0).asCurrency(), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Brand700, letterSpacing = (-0.5).sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Storefront, contentDescription = null, tint = Brand700, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${uiState.quantityInput} Pcs ${uiState.selectedProduct?.productName ?: "-"} • ${uiState.selectedSupplier?.name ?: "-"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF475569),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            // Payment Method Grid
            Column {
                Text("METODE BAYAR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155), modifier = Modifier.padding(bottom = 10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PaymentOptionCard(
                        method = IncomingPaymentMethod.CASH,
                        selected = uiState.paymentMethod == IncomingPaymentMethod.CASH,
                        icon = Icons.Outlined.Payments,
                        title = "Tunai (Kas Toko)",
                        subtitle = "Lunas langsung",
                        modifier = Modifier.weight(1f),
                        onClick = { onPaymentMethodChanged(IncomingPaymentMethod.CASH) }
                    )
                    PaymentOptionCard(
                        method = IncomingPaymentMethod.TRANSFER,
                        selected = uiState.paymentMethod == IncomingPaymentMethod.TRANSFER,
                        icon = Icons.Outlined.AccountBalance,
                        title = "Transfer Bank",
                        subtitle = "BCA / Mandiri",
                        modifier = Modifier.weight(1f),
                        onClick = { onPaymentMethodChanged(IncomingPaymentMethod.TRANSFER) }
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PaymentOptionCard(
                        method = IncomingPaymentMethod.DEBT,
                        selected = uiState.paymentMethod == IncomingPaymentMethod.DEBT,
                        icon = Icons.Outlined.EventNote,
                        title = "Tempo / Utang",
                        subtitle = "Jatuh tempo",
                        modifier = Modifier.weight(1f),
                        onClick = { onPaymentMethodChanged(IncomingPaymentMethod.DEBT) }
                    )
                    PaymentOptionCard(
                        method = IncomingPaymentMethod.DOWN_PAYMENT,
                        selected = uiState.paymentMethod == IncomingPaymentMethod.DOWN_PAYMENT,
                        icon = Icons.Outlined.MonetizationOn,
                        title = "DP / Uang Muka",
                        subtitle = "Bayar sebagian",
                        modifier = Modifier.weight(1f),
                        onClick = { onPaymentMethodChanged(IncomingPaymentMethod.DOWN_PAYMENT) }
                    )
                }
            }
            
            // Conditional Inputs
            if (uiState.paymentMethod == IncomingPaymentMethod.DOWN_PAYMENT) {
                OutlinedTextField(
                    value = uiState.amountPaidInput,
                    onValueChange = onAmountPaidChanged,
                    label = { Text("Nominal DP (Rp)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Brand600
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            } else if (uiState.paymentMethod == IncomingPaymentMethod.DEBT) {
                OutlinedTextField(
                    value = uiState.dueDaysInput,
                    onValueChange = onDueDaysChanged,
                    label = { Text("Jatuh Tempo (Hari)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Brand600
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Cash Source Detail
            if (uiState.paymentMethod == IncomingPaymentMethod.CASH || uiState.paymentMethod == IncomingPaymentMethod.TRANSFER) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFE2E8F0).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(24.dp).background(Color(0xFFD1FAE5), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF047857), modifier = Modifier.size(14.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Sumber Kas Keluar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF64748B))
                            Text("Kas Utama", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Action Button
            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Brand700),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.SaveAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Konfirmasi & Simpan Barang Masuk", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PaymentOptionCard(
    method: IncomingPaymentMethod,
    selected: Boolean,
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val bgColor = if (selected) Brand50 else Color.White
    val borderColor = if (selected) Brand700 else Color(0xFFE2E8F0)
    val iconBgColor = if (selected) Brand700 else Color(0xFFF1F5F9)
    val iconColor = if (selected) Color.White else Color(0xFF475569)

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        if (selected) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd).size(20.dp).background(Brand700, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
        Column {
            Box(
                modifier = Modifier.size(32.dp).background(iconBgColor, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selected) Color(0xFF0F172A) else Color(0xFF1E293B))
            Text(subtitle, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = if (selected) Brand700 else Color(0xFF94A3B8))
        }
    }
}

private fun Double.asCurrency(): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).format(this)
private fun BigDecimal.asCurrency(): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).format(this)
