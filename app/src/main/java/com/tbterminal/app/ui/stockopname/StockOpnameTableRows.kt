package com.tbterminal.app.ui.stockopname

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.ProductStock
import com.tbterminal.app.data.model.StockAdjustment
import java.math.BigDecimal

@Composable
internal fun StockTableRows(
    modifier: Modifier,
    uiState: StockOpnameUiState,
    onSelectProduct: (ProductStock) -> Unit,
    compact: Boolean = false
) {
    Box(modifier = modifier.fillMaxWidth()) {
        when {
            uiState.isLoading -> Box(modifier = Modifier.fillMaxWidth().height(180.dp)) { LoadingState() }
            uiState.tableProducts.isEmpty() -> Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                EmptyState("Tidak ada produk aktif yang cocok.")
            }
            else -> Column(modifier = Modifier.fillMaxWidth()) {
                uiState.tablePageProducts.forEachIndexed { index, product ->
                    if (compact) StockMobileRow(product, product.toRowState(uiState)) { onSelectProduct(product) } else StockTableRow(
                        product = product,
                        rowState = product.toRowState(uiState),
                        useAlternateBackground = index % 2 != 0,
                        onClick = { onSelectProduct(product) }
                    )
                    HorizontalDivider(color = OpnameLine.copy(alpha = 0.65f))
                }
            }
        }
    }
}

@Composable
private fun StockMobileRow(product: ProductStock, rowState: OpnameRowState, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row {
            Column(Modifier.weight(1f)) {
                Text(product.productName, color = OpnameText, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${product.sku} · ${product.categoryName}", color = OpnameMuted, fontSize = 11.sp)
            }
            Text(rowState.statusLabel, color = rowState.statusColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Sistem ${product.quantity.qtyText()} ${product.unitName}", color = OpnameMuted, fontSize = 12.sp)
            Text("Fisik ${rowState.physicalQty}", color = OpnameText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("Selisih ${rowState.diffText}", color = rowState.statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        if (rowState.reason != "-") Text(rowState.reason, color = OpnameMuted, fontSize = 11.sp)
    }
}

@Composable
private fun StockTableRow(
    product: ProductStock,
    rowState: OpnameRowState,
    useAlternateBackground: Boolean,
    onClick: () -> Unit
) {
    val background = when {
        rowState.isSelected -> OpnamePrimary.copy(alpha = 0.08f)
        useAlternateBackground -> OpnameSoft.copy(alpha = 0.76f)
        else -> OpnameSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProductNameCell(product, Modifier.weight(2.45f).padding(end = 16.dp))
        NumberCell(product.quantity.qtyText(), product.unitName, Modifier.weight(1.15f).padding(horizontal = 8.dp), OpnameText)
        NumberCell(rowState.physicalQty, product.unitName, Modifier.weight(1.15f).padding(horizontal = 8.dp), rowState.statusColor)
        Text(
            rowState.diffText,
            modifier = Modifier.weight(1.05f).padding(horizontal = 8.dp),
            color = rowState.statusColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End
        )
        Text(
            rowState.statusLabel,
            modifier = Modifier.weight(1.35f).padding(horizontal = 10.dp),
            color = rowState.statusColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Text(
            rowState.reason,
            modifier = Modifier.weight(1.15f).padding(start = 10.dp),
            color = OpnameMuted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProductNameCell(product: ProductStock, modifier: Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(OpnamePrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(product.productName.firstOrNull()?.uppercase() ?: "P", color = OpnamePrimaryDark, fontWeight = FontWeight.Black)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                product.productName,
                color = OpnameText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                product.sku,
                color = OpnameMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NumberCell(value: String, unitName: String, modifier: Modifier, tint: Color) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.Bottom) {
        Text(value, color = tint, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
        if (value != "-") {
            Text(unitName, color = OpnameMuted, fontSize = 10.sp, modifier = Modifier.padding(start = 3.dp, bottom = 1.dp))
        }
    }
}

private data class OpnameRowState(
    val isSelected: Boolean,
    val physicalQty: String,
    val diffText: String,
    val statusLabel: String,
    val statusColor: Color,
    val reason: String
)

private fun ProductStock.toRowState(uiState: StockOpnameUiState): OpnameRowState {
    val isSelected = productId == uiState.selectedProduct?.productId
    val latestAdjustment = uiState.latestAdjustmentsByProductId[productId]
    val activePhysicalQty = if (isSelected) uiState.actualQty else null
    val activeDifference = if (isSelected) uiState.difference else null
    val physicalQty = activePhysicalQty ?: latestAdjustment?.qtyAfter
    val difference = activeDifference ?: latestAdjustment?.difference

    return OpnameRowState(
        isSelected = isSelected,
        physicalQty = physicalQty?.qtyText() ?: "-",
        diffText = difference?.signedQtyText() ?: "-",
        statusLabel = difference.toStatusLabel(physicalQty),
        statusColor = difference.toStatusColor(physicalQty),
        reason = resolveReason(isSelected, uiState.adjustmentType, latestAdjustment)
    )
}

private fun resolveReason(
    isSelected: Boolean,
    adjustmentType: StockAdjustmentType,
    latestAdjustment: StockAdjustment?
): String {
    if (isSelected) return adjustmentType.label
    if (latestAdjustment == null) return "-"
    return latestAdjustment.reason.ifBlank { latestAdjustment.adjustmentTypeLabel }
}

private fun BigDecimal?.toStatusLabel(physicalQty: BigDecimal?): String {
    if (physicalQty == null) return "Belum dihitung"
    if (this == null || compareTo(BigDecimal.ZERO) == 0) return "Sesuai"
    return if (signum() > 0) "Lebih" else "Kurang"
}

private fun BigDecimal?.toStatusColor(physicalQty: BigDecimal?): Color {
    if (physicalQty == null) return OpnameMuted
    if (this == null || compareTo(BigDecimal.ZERO) == 0) return OpnamePrimaryDark
    return if (signum() > 0) OpnameWarning else OpnameDanger
}
