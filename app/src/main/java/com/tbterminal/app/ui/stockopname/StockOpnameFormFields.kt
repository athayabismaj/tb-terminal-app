package com.tbterminal.app.ui.stockopname

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.ProductStock

@Composable
internal fun FormTitle() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(OpnamePrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = OpnamePrimaryDark)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text("Form Penyesuaian", color = OpnameText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Input stok fisik untuk produk terpilih.", color = OpnameMuted, fontSize = 12.sp)
        }
    }
}

@Composable
internal fun SelectedProductSummary(product: ProductStock?) {
    val title = product?.productName ?: "Belum ada produk dipilih"
    val subtitle = product?.let { "${it.sku} - ${it.categoryName}" } ?: "Pilih produk dari tabel di sebelah kiri."

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(OpnameSoft)
            .padding(16.dp)
    ) {
        Text(title, color = OpnameText, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(subtitle, color = OpnameMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
internal fun StockDifferenceSummary(uiState: StockOpnameUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        StockInfoTile(Modifier.weight(1f), "STOK SISTEM", uiState.selectedProduct?.quantity?.qtyText() ?: "-", uiState.selectedProduct?.unitName.orEmpty(), OpnameText)
        StockInfoTile(Modifier.weight(1f), "SELISIH", uiState.difference?.signedQtyText() ?: "-", uiState.selectedProduct?.unitName.orEmpty(), uiState.difference.differenceColor())
    }
}

@Composable
private fun StockInfoTile(
    modifier: Modifier,
    label: String,
    value: String,
    suffix: String,
    tint: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(OpnameSoft)
            .padding(16.dp)
    ) {
        Text(label, color = OpnameMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = tint, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            if (suffix.isNotBlank()) {
                Text(suffix, color = OpnameMuted, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
            }
        }
    }
}

@Composable
internal fun AdjustmentTypeCards(
    selectedType: StockAdjustmentType,
    onSelect: (StockAdjustmentType) -> Unit
) {
    Column {
        Text("JENIS PENYESUAIAN", color = OpnameMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StockAdjustmentType.entries.forEach { type ->
                AdjustmentTypeCard(
                    modifier = Modifier.weight(1f),
                    type = type,
                    isSelected = selectedType == type,
                    onClick = { onSelect(type) }
                )
            }
        }
    }
}

@Composable
private fun AdjustmentTypeCard(
    modifier: Modifier,
    type: StockAdjustmentType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) OpnamePrimary else OpnameLine
    val background = if (isSelected) OpnamePrimary.copy(alpha = 0.12f) else OpnameSoft
    val titleColor = if (isSelected) OpnamePrimaryDark else OpnameText

    Column(
        modifier = modifier
            .height(108.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .border(2.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(type.label, color = titleColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(
            type.description,
            color = OpnameMuted,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            if (isSelected) "Dipilih" else "Pilih",
            color = if (isSelected) OpnamePrimaryDark else OpnameMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun QuantityInput(value: String, onValueChanged: (String) -> Unit) {
    LabeledInput(
        label = "PHYSICAL STOCK",
        value = value,
        onValueChanged = onValueChanged,
        placeholder = "Masukkan hasil hitung fisik",
        keyboardType = KeyboardType.Decimal
    )
}

@Composable
internal fun NotesInput(value: String, onValueChanged: (String) -> Unit) {
    LabeledInput(
        label = "ALASAN / CATATAN",
        value = value,
        onValueChanged = onValueChanged,
        placeholder = "Contoh: stok fisik gudang belakang sudah dihitung ulang",
        minLines = 3
    )
}

@Composable
private fun LabeledInput(
    label: String,
    value: String,
    onValueChanged: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1
) {
    Column {
        Text(label, color = OpnameMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChanged,
            placeholder = { Text(placeholder) },
            singleLine = minLines == 1,
            minLines = minLines,
            maxLines = if (minLines > 1) 4 else 1,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OpnameTextFieldColors()
        )
    }
}
