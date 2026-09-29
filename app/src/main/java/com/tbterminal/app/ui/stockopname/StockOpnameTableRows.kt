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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
                    if (index < uiState.tablePageProducts.lastIndex) {
                        HorizontalDivider(
                            modifier = if (compact) Modifier.padding(horizontal = 16.dp) else Modifier,
                            color = OpnameLine.copy(alpha = 0.58f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StockMobileRow(product: ProductStock, rowState: OpnameRowState, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .testTag("stock-adjustment-product-${product.productId}"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    product.productName,
                    color = OpnameText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${product.sku} · ${product.categoryName}",
                    color = OpnameMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                color = rowState.statusColor.copy(alpha = 0.11f),
                shape = RoundedCornerShape(999.dp),
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        rowState.statusLabel,
                        color = rowState.statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = "Sesuaikan stok ${product.productName}",
                        tint = rowState.statusColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OpnameMetric("Sistem", "${product.quantity.qtyText()} ${product.unitName}", Modifier.weight(1f))
            OpnameMetric(
                "Fisik",
                rowState.physicalQty.takeUnless { it == "-" }?.let { "$it ${product.unitName}" } ?: "-",
                Modifier.weight(1f),
            )
            OpnameMetric(
                "Selisih",
                rowState.diffText,
                Modifier.weight(1f),
                rowState.statusColor,
                Alignment.End
            )
        }
        if (rowState.reason != "-") {
            Text(
                "Alasan: ${rowState.reason}",
                color = OpnameMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun OpnameMetric(
    label: String,
    value: String,
    modifier: Modifier,
    valueColor: Color = OpnameText,
    alignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(modifier = modifier, horizontalAlignment = alignment, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = OpnameMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        Box(modifier = Modifier.weight(1.35f).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
            Surface(
                color = rowState.statusColor.copy(alpha = 0.11f),
                shape = RoundedCornerShape(999.dp),
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        rowState.statusLabel,
                        color = rowState.statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = "Sesuaikan stok ${product.productName}",
                        tint = rowState.statusColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
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
