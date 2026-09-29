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
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
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
        ProductFormLabel("SKU / kode barang")
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
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ProductSoft,
                    contentColor = ProductText
                ),
                contentPadding = PaddingValues(horizontal = 14.dp),
            ) {
                Text("Buat SKU", fontWeight = FontWeight.SemiBold, maxLines = 1)
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
        ProductFormSelectField(
            modifier = Modifier.fillMaxWidth(),
            label = "Kategori *",
            selectedText = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: "Pilih kategori",
            options = categories,
            optionText = ProductCategory::name,
            onSelect = onSelect,
        )
    }
}

@Composable
internal fun ConversionFields(
    modifier: Modifier,
    units: List<ProductUnit>,
    input: ProductFormInput,
    onInputChanged: (ProductFormInput) -> Unit,
    compact: Boolean,
) {
    val availableUnits = units.filterNot { it.id == input.baseUnitId }
    val baseSymbol = units.firstOrNull { it.id == input.baseUnitId }?.symbol ?: "satuan utama"
    val secondarySymbol = units.firstOrNull { it.id == input.secondaryUnitId }?.symbol ?: "satuan kedua"

    val fields: @Composable (Modifier, Modifier) -> Unit = { unitModifier, factorModifier ->
        ProductFormSelectField(
            modifier = unitModifier,
            label = "Satuan kedua *",
            selectedText = availableUnits.selectedUnitLabel(input.secondaryUnitId),
            options = availableUnits,
            optionText = { unit -> "${unit.name} (${unit.symbol})" },
            onSelect = { unit -> onInputChanged(input.copy(secondaryUnitId = unit.id)) }
        )
        ProductFormInputField(
            modifier = factorModifier,
            label = "Faktor konversi *",
            value = input.secondaryUnitFactor,
            placeholder = "Contoh: 12",
            isNumber = true,
            onValueChange = { value ->
                onInputChanged(input.copy(secondaryUnitFactor = value.numericInput()))
            }
        )
    }
    if (compact) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            fields(Modifier.fillMaxWidth(), Modifier.fillMaxWidth())
        }
    } else {
        Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            fields(Modifier.weight(1f), Modifier.weight(1f))
        }
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
    compact: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Surface(color = ProductSurface, shadowElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compact) 16.dp else 32.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onCancel,
                modifier = if (compact) Modifier.weight(0.45f).height(52.dp) else Modifier.height(52.dp),
            ) {
                Text("Batal", color = ProductText, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.width(if (compact) 8.dp else 12.dp))
            Button(
                onClick = onSave,
                enabled = !isSaving && !isLoading,
                modifier = (if (compact) Modifier.weight(1f) else Modifier).height(52.dp).testTag("product-form-save"),
                colors = ButtonDefaults.buttonColors(containerColor = ProductPrimaryDark),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 22.dp),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = ProductSurface,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isSaving) "Menyimpan…" else "Simpan produk", fontWeight = FontWeight.SemiBold)
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
        modifier = modifier.fillMaxWidth().testTag("product-form-field"),
        shape = RoundedCornerShape(14.dp),
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
                Text("Margin $margin", color = ProductPrimaryDark, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
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
        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                onClick = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("product-form-select"),
                shape = RoundedCornerShape(14.dp),
                color = ProductSoft,
                contentColor = ProductText,
                border = BorderStroke(1.dp, ProductLine.copy(alpha = 0.7f)),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        selectedText,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(
                        Icons.Outlined.ExpandMore,
                        contentDescription = "Buka pilihan",
                        modifier = Modifier.size(20.dp),
                        tint = ProductMuted,
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = RoundedCornerShape(14.dp),
                containerColor = ProductSurface,
            ) {
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
                .clip(RoundedCornerShape(14.dp))
                .background(ProductSoft)
                .border(BorderStroke(1.dp, ProductLine.copy(alpha = 0.7f)), RoundedCornerShape(14.dp))
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
            .clip(RoundedCornerShape(14.dp))
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
        modifier = modifier.padding(bottom = 6.dp),
        color = ProductMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun productFormTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ProductPrimaryDark,
    unfocusedBorderColor = ProductLine.copy(alpha = 0.7f),
    disabledBorderColor = ProductLine.copy(alpha = 0.55f),
    focusedContainerColor = ProductSoft,
    unfocusedContainerColor = ProductSoft,
    disabledContainerColor = ProductSoft
)
