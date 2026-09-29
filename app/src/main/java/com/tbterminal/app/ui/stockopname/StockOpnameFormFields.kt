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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.ProductStock

@Composable
internal fun SelectedProductSummary(product: ProductStock?) {
    val title = product?.productName ?: "Belum ada produk dipilih"
    val subtitle = product?.let { "${it.sku} · ${it.categoryName}" } ?: "Pilih produk untuk mulai menyesuaikan stok."

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(OpnameSoft.copy(alpha = 0.72f))
            .border(1.dp, OpnameLine.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Text(title, color = OpnameText, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(subtitle, color = OpnameMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
internal fun StockDifferenceSummary(uiState: StockOpnameUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        StockInfoTile(Modifier.weight(1f), "Stok sistem", uiState.selectedProduct?.quantity?.qtyText() ?: "-", uiState.selectedProduct?.unitName.orEmpty(), OpnameText)
        StockInfoTile(Modifier.weight(1f), "Selisih", uiState.difference?.signedQtyText() ?: "-", uiState.selectedProduct?.unitName.orEmpty(), uiState.difference.differenceColor())
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
            .background(OpnameSoft.copy(alpha = 0.72f))
            .border(1.dp, OpnameLine.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .padding(14.dp)
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
    onSelect: (StockAdjustmentType) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Jenis penyesuaian", color = OpnameMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StockAdjustmentType.entries.chunked(2).forEach { rowTypes ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowTypes.forEach { type ->
                        AdjustmentTypeCard(Modifier.weight(1f), type, selectedType == type) { onSelect(type) }
                    }
                    if (rowTypes.size == 1) Spacer(Modifier.weight(1f))
                }
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
            .height(88.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("stock-adjustment-type-${type.apiValue}")
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(type.label, color = titleColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(
            type.description,
            color = OpnameMuted,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun QuantityInput(value: String, onValueChanged: (String) -> Unit) {
    LabeledInput(
        label = "Stok fisik",
        value = value,
        onValueChanged = onValueChanged,
        placeholder = "Masukkan hasil hitung fisik",
        keyboardType = KeyboardType.Decimal
    )
}

@Composable
internal fun OpeningDateInput(value: String, onValueChanged: (String) -> Unit) {
    LabeledInput(
        label = "Tanggal saldo awal",
        value = value,
        onValueChanged = onValueChanged,
        placeholder = "YYYY-MM-DD"
    )
}

@Composable
internal fun NotesInput(value: String, onValueChanged: (String) -> Unit) {
    LabeledInput(
        label = "Alasan / catatan",
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
        Text(label, color = OpnameMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChanged,
            placeholder = { Text(placeholder) },
            singleLine = minLines == 1,
            minLines = minLines,
            maxLines = if (minLines > 1) 4 else 1,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OpnameTextFieldColors()
        )
    }
}
