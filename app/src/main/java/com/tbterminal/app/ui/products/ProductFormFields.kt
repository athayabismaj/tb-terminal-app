package com.tbterminal.app.ui.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.ProductCategory
import com.tbterminal.app.data.model.ProductUnit

@Composable
internal fun SkuField(
    modifier: Modifier,
    input: ProductFormInput,
    isEditMode: Boolean,
    onInputChanged: (ProductFormInput) -> Unit
) {
    Column(modifier = modifier) {
        ProductFormLabel("SKU / KODE BARANG")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProductFormInputFieldBox(
                modifier = Modifier.weight(1f),
                value = input.sku,
                placeholder = "SKU-XXXXXX",
                enabled = !isEditMode,
                onValueChange = { value -> onInputChanged(input.copy(sku = value.uppercase())) }
            )
            Button(
                onClick = { onInputChanged(input.copy(sku = generateSku(input.name))) },
                enabled = !isEditMode,
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ProductLine,
                    contentColor = ProductText
                )
            ) {
                Text("Generate", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun CategoryField(
    modifier: Modifier,
    categories: List<ProductCategory>,
    selectedCategoryId: String,
    onSelect: (ProductCategory) -> Unit
) {
    Column(modifier = modifier) {
        ProductFormLabel("KATEGORI *")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProductFormSelectField(
                modifier = Modifier.weight(1f),
                label = null,
                selectedText = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Pilih kategori",
                options = categories,
                optionText = ProductCategory::name,
                onSelect = onSelect
            )
            IconButton(
                onClick = { },
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ProductLine)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah kategori", tint = ProductText)
            }
        }
    }
}

@Composable
internal fun ConversionFields(
    modifier: Modifier,
    units: List<ProductUnit>,
    input: ProductFormInput,
    onInputChanged: (ProductFormInput) -> Unit
) {
    val availableUnits = units.filterNot { it.id == input.baseUnitId }
    val baseSymbol = units.firstOrNull { it.id == input.baseUnitId }?.symbol ?: "satuan utama"
    val secondarySymbol = units.firstOrNull { it.id == input.secondaryUnitId }?.symbol ?: "satuan kedua"

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        ProductFormSelectField(
            modifier = Modifier.weight(1f),
            label = "SATUAN KEDUA *",
            selectedText = availableUnits.selectedUnitLabel(input.secondaryUnitId),
            options = availableUnits,
            optionText = { unit -> "${unit.name} (${unit.symbol})" },
            onSelect = { unit -> onInputChanged(input.copy(secondaryUnitId = unit.id)) }
        )
        ProductFormInputField(
            modifier = Modifier.weight(1f),
            label = "FAKTOR KONVERSI *",
            value = input.secondaryUnitFactor,
            placeholder = "Contoh: 12",
            isNumber = true,
            onValueChange = { value ->
                onInputChanged(input.copy(secondaryUnitFactor = value.numericInput()))
            }
        )
    }
    Text(
        text = "1 $secondarySymbol = ${input.secondaryUnitFactor.ifBlank { "..." }} $baseSymbol. Stok tetap dicatat dalam satuan utama.",
        color = ProductMuted,
        fontSize = 11.sp,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
internal fun ProductFormBottomBar(
    isSaving: Boolean,
    isLoading: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Surface(color = ProductSurface, shadowElevation = 8.dp, border = BorderStroke(1.dp, ProductLine)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("Batal", color = ProductText, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = onSave,
                enabled = !isSaving && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = ProductPrimaryDark),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isSaving) "Menyimpan..." else "Simpan Produk", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun ProductFormInputField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    prefix: String? = null,
    isNumber: Boolean = false
) {
    Column(modifier = modifier) {
        ProductFormLabel(label)
        ProductFormInputFieldBox(
            value = value,
            placeholder = placeholder,
            prefix = prefix,
            isNumber = isNumber,
            onValueChange = onValueChange
        )
    }
}

@Composable
internal fun ProductFormInputFieldBox(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    prefix: String? = null,
    isNumber: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        placeholder = { Text(placeholder, color = ProductMuted.copy(alpha = 0.65f)) },
        leadingIcon = prefix?.let {
            { Text(it, color = ProductMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isNumber) KeyboardType.Number else KeyboardType.Text
        ),
        singleLine = true,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = productFormTextFieldColors()
    )
}

@Composable
internal fun PriceInputWithMargin(
    label: String,
    value: String,
    margin: String,
    onValueChange: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductFormLabel(label, modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(ProductPrimary.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("MARGIN: $margin", color = ProductPrimaryDark, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        ProductFormInputFieldBox(
            value = value,
            placeholder = "0",
            prefix = "Rp",
            isNumber = true,
            onValueChange = onValueChange
        )
    }
}

@Composable
internal fun <T> ProductFormSelectField(
    selectedText: String,
    options: List<T>,
    optionText: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        if (label != null) ProductFormLabel(label)
        Box {
            Button(
                onClick = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ProductSoft,
                    contentColor = ProductText
                ),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text(
                    selectedText,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionText(option)) },
                        onClick = {
                            expanded = false
                            onSelect(option)
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun ReadOnlyInfoField(
    label: String,
    value: String,
    helper: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        ProductFormLabel(label)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ProductSoft)
                .border(BorderStroke(1.dp, ProductLine), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(value, color = ProductMuted, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(helper, color = ProductMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
internal fun ProductFormError(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ProductDanger.copy(alpha = 0.12f))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(message, color = ProductDanger, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun ProductFormLabel(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label,
        modifier = modifier.padding(bottom = 4.dp),
        color = ProductMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun productFormTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ProductPrimary,
    unfocusedBorderColor = ProductLine,
    disabledBorderColor = ProductLine,
    focusedContainerColor = ProductSoft,
    unfocusedContainerColor = ProductSoft,
    disabledContainerColor = ProductSoft
)
