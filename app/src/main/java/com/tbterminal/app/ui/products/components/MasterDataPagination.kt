package com.tbterminal.app.ui.products.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.products.ProductLine
import com.tbterminal.app.ui.products.ProductMuted
import com.tbterminal.app.ui.products.ProductPrimaryDark
import com.tbterminal.app.ui.products.ProductSoft
import com.tbterminal.app.ui.products.ProductText

@Composable
internal fun MasterDataPagination(
    itemLabel: String,
    total: Long,
    page: Int,
    totalPages: Int,
    pageSize: Int,
    isLoading: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    val safePage = page.coerceAtLeast(1)
    val safeTotalPages = totalPages.coerceAtLeast(1)
    val firstItem = if (total == 0L) 0L else ((safePage - 1) * pageSize + 1L)
    val lastItem = if (total == 0L) 0L else (safePage * pageSize).toLong().coerceAtMost(total)

    HorizontalDivider(color = ProductLine)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ProductSoft.copy(alpha = 0.72f))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Menampilkan $firstItem-$lastItem dari $total $itemLabel",
                color = ProductText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Maksimal $pageSize $itemLabel per halaman",
                color = ProductMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            MasterDataPageButton(
                icon = Icons.Default.ChevronLeft,
                enabled = safePage > 1 && !isLoading,
                onClick = onPreviousPage
            )
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(ProductPrimaryDark, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(safePage.toString(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Text("/ $safeTotalPages", color = ProductMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            MasterDataPageButton(
                icon = Icons.Default.ChevronRight,
                enabled = safePage < safeTotalPages && !isLoading,
                onClick = onNextPage
            )
        }
    }
}

@Composable
private fun MasterDataPageButton(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ProductLine),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(34.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) ProductText else ProductMuted.copy(alpha = 0.35f)
        )
    }
}
