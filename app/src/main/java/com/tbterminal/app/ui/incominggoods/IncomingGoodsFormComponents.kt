package com.tbterminal.app.ui.incominggoods

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBusiness
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.model.Supplier

@Composable
internal fun IncomingGoodsFormBody(
    uiState: IncomingGoodsUiState,
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
    onNotesChanged: (String) -> Unit,
    onPaymentMethodChanged: (IncomingPaymentMethod) -> Unit,
    onSubmit: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { FormTitle() }
        item { ProductSelectionDropdown(uiState, onSelectProduct) }
        item { SelectedProductSummary(uiState.selectedProduct) }
        item { SupplierSection(uiState, onSelectSupplier, onSupplierNameChanged, onCreateSupplier) }
        item { PurchaseInputs(uiState, onInvoiceChanged, onGenerateInvoiceNumber, onQuantityChanged, onBuyPriceChanged) }
        item { TotalSummary(uiState) }
        item { PaymentMethodSection(uiState.paymentMethod, onPaymentMethodChanged) }
        item { PaymentInputs(uiState, onAmountPaidChanged, onDueDaysChanged) }
        item { NotesInput(uiState.notesInput, onNotesChanged) }
        item { SubmitButton(uiState, onSubmit) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductSelectionDropdown(
    uiState: IncomingGoodsUiState,
    onSelectProduct: (ProductStock) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("PRODUK", color = IncomingMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = uiState.selectedProduct?.productName ?: "Pilih produk",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = IncomingTextFieldColors()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                uiState.products.forEach { product ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(product.productName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(product.sku, color = IncomingMuted, fontSize = 12.sp)
                            }
                        },
                        onClick = {
                            onSelectProduct(product)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FormTitle() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(IncomingPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = IncomingPrimaryDark)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text("Form Barang Masuk", color = IncomingText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Simpan restok berdasarkan nota supplier.", color = IncomingMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SelectedProductSummary(product: ProductStock?) {
    val title = product?.productName ?: "Belum ada produk dipilih"
    val subtitle = product?.let { "${it.sku} - ${it.categoryName}" } ?: "Pilih produk dari daftar di sebelah kiri."
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(IncomingSoft)
            .padding(16.dp)
    ) {
        Text(title, color = IncomingText, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(subtitle, color = IncomingMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        if (product != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProductInfoPill(
                    label = "STOK SAAT INI",
                    value = "${product.quantity.qtyText()} ${product.unitName}",
                    modifier = Modifier.weight(1f)
                )
                ProductInfoPill(
                    label = "HARGA BELI",
                    value = product.priceBuy.currencyText(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ProductInfoPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(IncomingSurface)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, color = IncomingMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = IncomingText, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("SUPPLIER", color = IncomingMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = uiState.selectedSupplier?.name ?: "Pilih supplier",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = IncomingTextFieldColors()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                uiState.suppliers.forEach { supplier ->
                    DropdownMenuItem(
                        text = { Text(supplier.name) },
                        onClick = {
                            onSelectSupplier(supplier)
                            expanded = false
                        }
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = uiState.newSupplierNameInput,
                onValueChange = onSupplierNameChanged,
                placeholder = { Text("Tambah supplier cepat") },
                leadingIcon = { Icon(Icons.Outlined.AddBusiness, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = IncomingTextFieldColors()
            )
            Button(
                onClick = onCreateSupplier,
                enabled = !uiState.isSavingSupplier,
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IncomingPrimary)
            ) {
                if (uiState.isSavingSupplier) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Simpan", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PurchaseInputs(
    uiState: IncomingGoodsUiState,
    onInvoiceChanged: (String) -> Unit,
    onGenerateInvoiceNumber: () -> Unit,
    onQuantityChanged: (String) -> Unit,
    onBuyPriceChanged: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            LabeledTextField(
                label = "NO. NOTA",
                value = uiState.invoiceNoInput,
                onValueChanged = onInvoiceChanged,
                placeholder = "BM-20260524-001",
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = onGenerateInvoiceNumber,
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IncomingSoft,
                    contentColor = IncomingPrimaryDark
                ),
                contentPadding = PaddingValues(horizontal = 18.dp)
            ) {
                Text("Generate", fontWeight = FontWeight.Bold)
            }
        }
        Text(
            text = "Format otomatis: BM-tahunbulantanggal-nomor urut, contoh BM-20260524-001.",
            color = IncomingMuted,
            fontSize = 11.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LabeledTextField("JUMLAH MASUK", uiState.quantityInput, onQuantityChanged, "0", Modifier.weight(1f), KeyboardType.Decimal)
            LabeledTextField("HARGA BELI", uiState.buyPriceInput, onBuyPriceChanged, "0", Modifier.weight(1f), KeyboardType.Decimal)
        }
    }
}

@Composable
private fun TotalSummary(uiState: IncomingGoodsUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        SummaryTile("TOTAL", uiState.total.currencyText(), Icons.Outlined.Payments, Modifier.weight(1f))
        SummaryTile("ESTIMASI UTANG", uiState.payableAmount.currencyText(), Icons.Outlined.LocalShipping, Modifier.weight(1f))
    }
}

@Composable
private fun PaymentMethodSection(
    selected: IncomingPaymentMethod,
    onSelect: (IncomingPaymentMethod) -> Unit
) {
    Column {
        Text("METODE PEMBAYARAN", color = IncomingMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            IncomingPaymentMethod.entries.chunked(3).forEach { methods ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    methods.forEach { method ->
                        PaymentChip(method, method == selected, { onSelect(method) }, Modifier.weight(1f))
                    }
                    repeat(3 - methods.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun PaymentChip(
    method: IncomingPaymentMethod,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) IncomingPrimary.copy(alpha = 0.12f) else IncomingSoft)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(method.label, color = if (isSelected) IncomingPrimaryDark else IncomingText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(method.description, color = IncomingMuted, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun PaymentInputs(
    uiState: IncomingGoodsUiState,
    onAmountPaidChanged: (String) -> Unit,
    onDueDaysChanged: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (uiState.paymentMethod == IncomingPaymentMethod.DOWN_PAYMENT) {
            LabeledTextField("JUMLAH DP", uiState.amountPaidInput, onAmountPaidChanged, "0", Modifier.weight(1f), KeyboardType.Decimal)
        }
        if (uiState.paymentMethod == IncomingPaymentMethod.DEBT || uiState.paymentMethod == IncomingPaymentMethod.DOWN_PAYMENT) {
            LabeledTextField("TERMIN HARI", uiState.dueDaysInput, onDueDaysChanged, "30", Modifier.weight(1f), KeyboardType.Number)
        }
    }
}

@Composable
private fun NotesInput(
    value: String,
    onValueChanged: (String) -> Unit
) {
    LabeledTextField(
        label = "CATATAN",
        value = value,
        onValueChanged = onValueChanged,
        placeholder = "Contoh: restok gudang utama dari nota supplier",
        minLines = 3
    )
}

@Composable
private fun SubmitButton(
    uiState: IncomingGoodsUiState,
    onSubmit: () -> Unit
) {
    Button(
        onClick = onSubmit,
        enabled = uiState.selectedProduct != null &&
            uiState.selectedSupplier != null &&
            uiState.quantity != null &&
            uiState.buyPrice != null &&
            !uiState.isSubmitting,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = IncomingPrimary),
        contentPadding = PaddingValues(horizontal = 18.dp)
    ) {
        if (uiState.isSubmitting) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Simpan Barang Masuk", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LabeledTextField(
    label: String,
    value: String,
    onValueChanged: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1
) {
    Column(modifier = modifier) {
        Text(label, color = IncomingMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChanged,
            placeholder = { Text(placeholder) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            minLines = minLines,
            maxLines = if (minLines > 1) 4 else 1,
            singleLine = minLines == 1,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = IncomingTextFieldColors()
        )
    }
}

@Composable
private fun SummaryTile(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(IncomingSoft)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = IncomingPrimaryDark, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(label, color = IncomingMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = IncomingText, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
        }
    }
}
