package com.tbterminal.app.ui.stockopname

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Inventory2
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
import com.tbterminal.app.ui.components.SkeletonBox
import java.math.BigDecimal

private val BrandGreen = Color(0xFF256B57)
private val TextPrimary = Color(0xFF0F172A)
private val TextSecondary = Color(0xFF64748B)
private val TextMuted = Color(0xFF94A3B8)
private val BorderSlate200 = Color(0xFFE2E8F0)

@Composable
internal fun StockTableRows(
    modifier: Modifier,
    uiState: StockOpnameUiState,
    onSelectProduct: (ProductStock) -> Unit,
    compact: Boolean = false
) {
    Box(modifier = modifier.fillMaxWidth()) {
        when {
            uiState.isLoading -> StockOpnameSkeleton(compact = compact)
            uiState.tableProducts.isEmpty() -> StockOpnameEmptyState()
            else -> Column(modifier = Modifier.fillMaxWidth()) {
                uiState.tablePageProducts.forEachIndexed { index, product ->
                    if (compact) {
                        StockMobileRow(product, product.toRowState(uiState)) { onSelectProduct(product) }
                    } else {
                        StockTableRow(
                            product = product,
                            rowState = product.toRowState(uiState),
                            onClick = { onSelectProduct(product) }
                        )
                    }
                    if (index < uiState.tablePageProducts.lastIndex) {
                        HorizontalDivider(
                            modifier = if (compact) Modifier.padding(horizontal = 16.dp) else Modifier,
                            color = Color(0xFFF1F5F9),
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
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("stock-adjustment-product-${product.productId}"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = product.productName,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${product.sku} · ${product.categoryName}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OpnameStatusBadge(rowState.statusLabel)
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Sesuaikan stok ${product.productName}",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OpnameMetric("Sistem", "${product.quantity.qtyText()} ${product.unitName}", Modifier.weight(1f))
            OpnameMetric(
                "Fisik",
                rowState.physicalQty.takeUnless { it == "-" }?.let { "$it ${product.unitName}" } ?: "-",
                Modifier.weight(1f),
                alignment = Alignment.CenterHorizontally
            )
            OpnameMetric(
                "Selisih",
                rowState.diffText,
                Modifier.weight(1f),
                valueColor = opnameDiffColor(rowState.diffText),
                alignment = Alignment.End
            )
        }
        if (rowState.reason != "-") {
            Text(
                text = "Alasan: ${rowState.reason}",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun OpnameStatusBadge(label: String) {
    val (bgColor, textColor) = when (label.lowercase()) {
        "sesuai" -> Color(0xFFEAF3EF) to Color(0xFF256B57)
        "lebih" -> Color(0xFFFEF3C7) to Color(0xFFB45309)
        "kurang" -> Color(0xFFFEE2E2) to Color(0xFFB91C1C)
        else -> Color(0xFFF1F5F9) to Color(0xFF64748B)
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

private fun opnameDiffColor(diffText: String): Color {
    return when {
        diffText.startsWith("+") -> Color(0xFFB45309)
        diffText.startsWith("-") -> Color(0xFFB91C1C)
        diffText == "0" -> Color(0xFF256B57)
        else -> TextPrimary
    }
}

@Composable
private fun OpnameMetric(
    label: String,
    value: String,
    modifier: Modifier,
    valueColor: Color = TextPrimary,
    alignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(modifier = modifier, horizontalAlignment = alignment, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StockTableRow(
    product: ProductStock,
    rowState: OpnameRowState,
    onClick: () -> Unit
) {
    val background = if (rowState.isSelected) BrandGreen.copy(alpha = 0.06f) else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProductNameCell(product, Modifier.weight(2.45f).padding(end = 16.dp))
        NumberCell(product.quantity.qtyText(), product.unitName, Modifier.weight(1.15f).padding(horizontal = 8.dp), TextPrimary)
        NumberCell(rowState.physicalQty, product.unitName, Modifier.weight(1.15f).padding(horizontal = 8.dp), rowState.statusColor)
        Text(
            text = rowState.diffText,
            modifier = Modifier.weight(1.05f).padding(horizontal = 8.dp),
            color = opnameDiffColor(rowState.diffText),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End
        )
        Box(modifier = Modifier.weight(1.35f).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
            OpnameStatusBadge(rowState.statusLabel)
        }
        Text(
            text = rowState.reason,
            modifier = Modifier.weight(1.15f).padding(start = 10.dp),
            color = TextSecondary,
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
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BrandGreen.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = product.productName.firstOrNull()?.uppercase() ?: "P",
                color = BrandGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = product.productName,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = product.sku,
                color = TextSecondary,
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
        Text(value, color = tint, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        if (value != "-") {
            Text(unitName, color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(start = 3.dp, bottom = 1.dp))
        }
    }
}

@Composable
private fun StockOpnameSkeleton(compact: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        repeat(if (compact) 4 else 5) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (compact) 16.dp else 24.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SkeletonBox(Modifier.size(36.dp), shape = RoundedCornerShape(10.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(if (compact) 1f else 2.45f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SkeletonBox(Modifier.fillMaxWidth(if (compact) 0.7f else 0.5f).height(14.dp))
                    SkeletonBox(Modifier.width(70.dp).height(11.dp))
                }
                if (!compact) {
                    SkeletonBox(Modifier.weight(1.15f).height(13.dp))
                    SkeletonBox(Modifier.weight(1.15f).height(13.dp))
                    SkeletonBox(Modifier.weight(1.05f).height(13.dp))
                    SkeletonBox(Modifier.weight(1.35f).height(24.dp), shape = RoundedCornerShape(999.dp))
                    SkeletonBox(Modifier.weight(1.15f).height(12.dp))
                } else {
                    SkeletonBox(Modifier.width(60.dp).height(22.dp), shape = RoundedCornerShape(999.dp))
                }
            }
            if (index < (if (compact) 3 else 4)) {
                HorizontalDivider(color = Color(0xFFF1F5F9))
            }
        }
    }
}

@Composable
private fun StockOpnameEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Inventory2,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Belum ada produk aktif yang cocok",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
        )
        Text(
            text = "Coba ubah kata kunci pencarian atau filter kategori Anda.",
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
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
    if (physicalQty == null) return TextMuted
    if (this == null || compareTo(BigDecimal.ZERO) == 0) return BrandGreen
    return if (signum() > 0) Color(0xFFD97706) else Color(0xFFE11D48)
}
