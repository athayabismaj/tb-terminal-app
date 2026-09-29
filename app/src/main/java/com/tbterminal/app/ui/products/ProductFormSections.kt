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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ProductFormBody(
    uiState: ProductFormUiState,
    onInputChanged: (ProductFormInput) -> Unit,
    compact: Boolean,
) {
    when {
        uiState.isLoading -> ProductLoadingState(modifier = Modifier.height(260.dp))
        else -> ProductLoadedForm(uiState = uiState, onInputChanged = onInputChanged, compact = compact)
    }
}

@Composable
private fun ProductLoadedForm(
    uiState: ProductFormUiState,
    onInputChanged: (ProductFormInput) -> Unit,
    compact: Boolean,
) {
    val input = uiState.input

    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp)) {
        if (uiState.errorMessage != null) {
            ProductFormError(message = uiState.errorMessage)
        }

        if (compact) {
            BasicInfoSection(uiState = uiState, input = input, onInputChanged = onInputChanged, compact = true)
            UnitSection(uiState = uiState, input = input, onInputChanged = onInputChanged, compact = true)
            PriceSection(input = input, onInputChanged = onInputChanged, compact = true)
            StockSection(input = input, onInputChanged = onInputChanged, compact = true)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    BasicInfoSection(uiState, input, onInputChanged, compact = false)
                    UnitSection(uiState, input, onInputChanged, compact = false)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PriceSection(input, onInputChanged, compact = false)
                    StockSection(input, onInputChanged, compact = false)
                }
            }
        }
    }
}

@Composable
private fun BasicInfoSection(
    uiState: ProductFormUiState,
    input: ProductFormInput,
    onInputChanged: (ProductFormInput) -> Unit,
    compact: Boolean,
) {
    ProductFormSection(title = "Informasi dasar", sectionTag = "basic", icon = Icons.Outlined.Info, compact = compact) {
        ProductFormInputField(
            label = "Nama produk *",
            value = input.name,
            placeholder = "Contoh: Semen Tiga Roda 50kg",
            onValueChange = { value -> onInputChanged(input.copy(name = value)) }
        )

        SkuField(
            modifier = Modifier.fillMaxWidth(),
            input = input,
            isEditMode = uiState.isEditMode,
            onInputChanged = onInputChanged,
        )
        CategoryField(
            modifier = Modifier.fillMaxWidth(),
            categories = uiState.categories,
            selectedCategoryId = input.categoryId,
            onSelect = { category -> onInputChanged(input.copy(categoryId = category.id)) },
        )
    }
}

@Composable
private fun UnitSection(
    uiState: ProductFormUiState,
    input: ProductFormInput,
    onInputChanged: (ProductFormInput) -> Unit,
    compact: Boolean,
) {
    ProductFormSection(
        title = "Satuan & konversi",
        sectionTag = "units",
        icon = Icons.Outlined.Straighten,
        compact = compact,
        headerAction = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Satuan kedua", color = ProductMuted, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(8.dp))
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
            label = "Satuan utama *",
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
                onInputChanged = onInputChanged,
                compact = compact,
            )
        }
    }
}

@Composable
private fun PriceSection(
    input: ProductFormInput,
    onInputChanged: (ProductFormInput) -> Unit,
    compact: Boolean,
) {
    ProductFormSection(title = "Harga & margin", sectionTag = "prices", icon = Icons.Outlined.Payments, compact = compact) {
        ProductFormInputField(
            modifier = Modifier.fillMaxWidth(),
            label = "Harga beli (modal)",
            value = input.priceBuy,
            placeholder = "0",
            prefix = "Rp",
            isNumber = true,
            onValueChange = { value -> onInputChanged(input.copy(priceBuy = value.numericInput())) },
        )
        PriceInputWithMargin(
            label = "Harga jual retail",
            value = input.priceRetail,
            margin = input.priceRetail.marginText(input.priceBuy),
            onValueChange = { value -> onInputChanged(input.copy(priceRetail = value.numericInput())) },
        )
        PriceInputWithMargin(
            label = "Harga jual kontraktor",
            value = input.priceContractor,
            margin = input.priceContractor.marginText(input.priceBuy),
            onValueChange = { value -> onInputChanged(input.copy(priceContractor = value.numericInput())) },
        )
    }
}

@Composable
private fun StockSection(
    input: ProductFormInput,
    onInputChanged: (ProductFormInput) -> Unit,
    compact: Boolean,
) {
    ProductFormSection(title = "Stok", sectionTag = "stock", icon = Icons.Outlined.Inventory2, compact = compact) {
        ReadOnlyInfoField(
            modifier = Modifier.fillMaxWidth(),
            label = "Stok awal",
            value = "0",
            helper = "Dicatat melalui Barang Masuk atau Stok Opname.",
        )
        ProductFormInputField(
            modifier = Modifier.fillMaxWidth(),
            label = "Stok minimum",
            value = input.minStock,
            placeholder = "0",
            isNumber = true,
            onValueChange = { value -> onInputChanged(input.copy(minStock = value.numericInput())) },
        )
    }
}

@Composable
internal fun ProductFormSection(
    title: String,
    sectionTag: String,
    icon: ImageVector,
    compact: Boolean,
    headerAction: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("product-form-section-$sectionTag"),
        colors = CardDefaults.cardColors(containerColor = ProductSurface),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ProductLine.copy(alpha = 0.72f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(if (compact) 16.dp else 20.dp)) {
            ProductFormSectionHeader(title = title, icon = icon, headerAction = headerAction, compact = compact)
            Column(
                modifier = Modifier.padding(top = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun ProductFormSectionHeader(
    title: String,
    icon: ImageVector,
    headerAction: @Composable (() -> Unit)?,
    compact: Boolean,
) {
    val titleContent: @Composable () -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(ProductPrimary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = ProductPrimaryDark, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(title, color = ProductText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    if (compact && headerAction != null) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            titleContent()
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                headerAction()
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            titleContent()
            headerAction?.invoke()
        }
    }
}
