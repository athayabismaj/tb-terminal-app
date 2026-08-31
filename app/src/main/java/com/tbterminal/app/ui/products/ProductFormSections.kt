package com.tbterminal.app.ui.products

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ProductFormBody(
    uiState: ProductFormUiState,
    onInputChanged: (ProductFormInput) -> Unit
) {
    when {
        uiState.isLoading -> ProductLoadingState(modifier = Modifier.height(260.dp))
        else -> ProductLoadedForm(uiState = uiState, onInputChanged = onInputChanged)
    }
}

@Composable
private fun ProductLoadedForm(
    uiState: ProductFormUiState,
    onInputChanged: (ProductFormInput) -> Unit
) {
    val input = uiState.input

    if (uiState.errorMessage != null) {
        ProductFormError(message = uiState.errorMessage)
    }

    BasicInfoSection(uiState = uiState, input = input, onInputChanged = onInputChanged)
    UnitSection(uiState = uiState, input = input, onInputChanged = onInputChanged)
    PriceSection(input = input, onInputChanged = onInputChanged)
    StockSection(input = input, onInputChanged = onInputChanged)
}

@Composable
private fun BasicInfoSection(
    uiState: ProductFormUiState,
    input: ProductFormInput,
    onInputChanged: (ProductFormInput) -> Unit
) {
    ProductFormSection(title = "Informasi Dasar", icon = Icons.Outlined.Info) {
        ProductFormInputField(
            label = "NAMA PRODUK *",
            value = input.name,
            placeholder = "Contoh: Semen Tiga Roda 50kg",
            onValueChange = { value -> onInputChanged(input.copy(name = value)) }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            SkuField(
                modifier = Modifier.weight(1f),
                input = input,
                isEditMode = uiState.isEditMode,
                onInputChanged = onInputChanged
            )
            CategoryField(
                modifier = Modifier.weight(1f),
                categories = uiState.categories,
                selectedCategoryId = input.categoryId,
                onSelect = { category -> onInputChanged(input.copy(categoryId = category.id)) }
            )
        }
    }
}

@Composable
private fun UnitSection(
    uiState: ProductFormUiState,
    input: ProductFormInput,
    onInputChanged: (ProductFormInput) -> Unit
) {
    ProductFormSection(
        title = "Satuan & Konversi",
        icon = Icons.Outlined.Straighten,
        headerAction = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Satuan Kedua", color = ProductMuted, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = input.usesSecondaryUnit,
                    onCheckedChange = { enabled ->
                        onInputChanged(
                            input.copy(
                                usesSecondaryUnit = enabled,
                                secondaryUnitId = if (enabled) input.secondaryUnitId else "",
                                secondaryUnitFactor = if (enabled) input.secondaryUnitFactor else ""
                            )
                        )
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ProductPrimary
                    )
                )
            }
        }
    ) {
        ProductFormSelectField(
            modifier = Modifier.fillMaxWidth(),
            label = "SATUAN UTAMA *",
            selectedText = uiState.units.selectedUnitLabel(input.baseUnitId),
            options = uiState.units,
            optionText = { unit -> "${unit.name} (${unit.symbol})" },
            onSelect = { unit ->
                onInputChanged(
                    input.copy(
                        baseUnitId = unit.id,
                        secondaryUnitId = input.secondaryUnitId.takeUnless { it == unit.id }.orEmpty()
                    )
                )
            }
        )

        if (input.usesSecondaryUnit) {
            ConversionFields(
                modifier = Modifier.fillMaxWidth(),
                units = uiState.units,
                input = input,
                onInputChanged = onInputChanged
            )
        }
    }
}

@Composable
private fun PriceSection(
    input: ProductFormInput,
    onInputChanged: (ProductFormInput) -> Unit
) {
    ProductFormSection(title = "Harga & Margin", icon = Icons.Outlined.Payments) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ProductFormInputField(
                modifier = Modifier.weight(1f),
                label = "HARGA BELI (MODAL)",
                value = input.priceBuy,
                placeholder = "0",
                prefix = "Rp",
                isNumber = true,
                onValueChange = { value -> onInputChanged(input.copy(priceBuy = value.numericInput())) }
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PriceInputWithMargin(
                    label = "HARGA JUAL (RETAIL)",
                    value = input.priceRetail,
                    margin = input.priceRetail.marginText(input.priceBuy),
                    onValueChange = { value -> onInputChanged(input.copy(priceRetail = value.numericInput())) }
                )
                PriceInputWithMargin(
                    label = "HARGA JUAL (KONTRAKTOR)",
                    value = input.priceContractor,
                    margin = input.priceContractor.marginText(input.priceBuy),
                    onValueChange = { value -> onInputChanged(input.copy(priceContractor = value.numericInput())) }
                )
            }
        }
    }
}

@Composable
private fun StockSection(
    input: ProductFormInput,
    onInputChanged: (ProductFormInput) -> Unit
) {
    ProductFormSection(title = "Stok", icon = Icons.Outlined.Inventory2) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ReadOnlyInfoField(
                modifier = Modifier.weight(1f),
                label = "STOK AWAL",
                value = "0",
                helper = "Stok awal masuk lewat Barang Masuk atau Stok Opname."
            )
            ProductFormInputField(
                modifier = Modifier.weight(1f),
                label = "STOK MINIMUM (ALERT)",
                value = input.minStock,
                placeholder = "0",
                isNumber = true,
                onValueChange = { value -> onInputChanged(input.copy(minStock = value.numericInput())) }
            )
        }
    }
}

@Composable
internal fun ProductFormSection(
    title: String,
    icon: ImageVector,
    headerAction: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ProductSurface),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, ProductLine)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            ProductFormSectionHeader(title = title, icon = icon, headerAction = headerAction)
            HorizontalDivider(color = ProductLine, modifier = Modifier.padding(bottom = 16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
        }
    }
}

@Composable
private fun ProductFormSectionHeader(
    title: String,
    icon: ImageVector,
    headerAction: @Composable (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(ProductPrimary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = ProductPrimaryDark, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, color = ProductText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        headerAction?.invoke()
    }
}
