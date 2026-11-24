package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.OwnerAmberInk
import com.tbterminal.app.ui.theme.OwnerSyncDot
import com.tbterminal.app.ui.theme.SurfaceWhite

private val StokWarningDot = Color(0xFFF2C14E)
private val StokWarningText = Color(0xFFF7D78A)

internal fun buildStokMenuGroups(
    state: StokUiState,
    onActionClick: (StokActionId) -> Unit
): List<OwnerPlainMenuGroup> {
    val hasLowStock = state.lowStockCount > 0
    return listOf(
        OwnerPlainMenuGroup(
            title = "Katalog & nilai jual",
            items = listOf(
                OwnerPlainMenuItem(
                    tag = "stok-menu-PRODUCTS_STOCK",
                    title = "Daftar produk",
                    subtitle = "${state.totalProductsCount} produk aktif",
                    icon = OwnerIcons.Box,
                    iconTint = Brand,
                    onClick = { onActionClick(StokActionId.PRODUCTS_STOCK) }
                ),
                OwnerPlainMenuItem(
                    tag = "stok-menu-PRODUCT_PRICES",
                    title = "Harga & margin",
                    subtitle = "Harga jual, HPP, dan margin",
                    icon = OwnerIcons.PriceTag,
                    iconTint = Brand,
                    onClick = { onActionClick(StokActionId.PRODUCT_PRICES) }
                )
            )
        ),
        OwnerPlainMenuGroup(
            title = "Audit & kontrol stok",
            items = listOf(
                OwnerPlainMenuItem(
                    tag = "stok-menu-STOCK_CARD",
                    title = "Kartu stok",
                    subtitle = if (hasLowStock) "${state.lowStockCount} produk di bawah stok minimum" else null,
                    icon = OwnerIcons.StockCard,
                    iconTint = if (hasLowStock) OwnerAmberInk else Brand,
                    onClick = { onActionClick(StokActionId.STOCK_CARD) }
                ),
                OwnerPlainMenuItem(
                    tag = "stok-menu-ADJUST_STOCK",
                    title = "Sesuaikan stok",
                    subtitle = "Opname, koreksi, retur, saldo awal",
                    icon = OwnerIcons.SlidersAdjust,
                    iconTint = Brand,
                    onClick = { onActionClick(StokActionId.ADJUST_STOCK) }
                )
            )
        ),
        OwnerPlainMenuGroup(
            title = "Data produk",
            items = listOf(
                OwnerPlainMenuItem(
                    tag = "stok-menu-CATEGORIES",
                    title = "Kategori produk",
                    subtitle = "${state.totalCategoriesCount} kategori",
                    icon = OwnerIcons.CategoryShapes,
                    iconTint = Brand,
                    onClick = { onActionClick(StokActionId.CATEGORIES) }
                ),
                OwnerPlainMenuItem(
                    tag = "stok-menu-UNITS",
                    title = "Satuan produk",
                    subtitle = "${state.totalUnitsCount} satuan",
                    icon = OwnerIcons.RulerUnits,
                    iconTint = Brand,
                    onClick = { onActionClick(StokActionId.UNITS) }
                )
            )
        )
    )
}

/**
 * Chip status stok di sisi kanan baris label header hijau:
 * - Ketika ada produk di bawah stok minimum: titik #F2C14E + "N perlu restok"
 * - Ketika tidak ada produk perlu restok: titik #5FD3A1 (--sync-dot) + "Stok aman"
 */
@Composable
internal fun StokHeroStatusChip(
    lowStockCount: Int,
    modifier: Modifier = Modifier
) {
    val hasLowStock = lowStockCount > 0
    val dotColor = if (hasLowStock) StokWarningDot else OwnerSyncDot
    val label = if (hasLowStock) "$lowStockCount perlu restok" else "Stok aman"

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.20f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = label,
            style = ownerTextStyle(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                color = OnBrand,
                tabularNumbers = true
            ),
            maxLines = 1
        )
    }
}

/**
 * Baris 3 kolom metrik di header hijau Stok sesuai CSS `.st` dan `.st+.st`.
 */
@Composable
internal fun StokHeroStatsRow(
    state: StokUiState,
    onLowStockClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lineColor = Color.White.copy(alpha = 0.18f)
    val hasLowStock = state.lowStockCount > 0

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(lineColor)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Katalog aktif",
                    style = ownerTextStyle(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = KeuanganHeroSoftText
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.cssLineBoxHeight(16.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (state.loading) "—" else "${state.totalProductsCount} item",
                    style = ownerTextStyle(
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = OnBrand,
                        tabularNumbers = true
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.cssLineBoxHeight(20.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .ownerHeroLeftDivider(lineColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onLowStockClick
                    )
                    .testTag("stok-stat-low-stock")
            ) {
                Text(
                    text = "Perlu restok",
                    style = ownerTextStyle(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = KeuanganHeroSoftText
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.cssLineBoxHeight(16.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (state.loading) "—" else "${state.lowStockCount} item",
                    style = ownerTextStyle(
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (!state.loading && hasLowStock) StokWarningText else OnBrand,
                        tabularNumbers = true
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.cssLineBoxHeight(20.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .ownerHeroLeftDivider(lineColor)
            ) {
                Text(
                    text = "Status sync",
                    style = ownerTextStyle(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = KeuanganHeroSoftText
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.cssLineBoxHeight(16.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.height(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (state.isSyncHealthy) OwnerSyncDot else StokWarningDot)
                    )
                    Text(
                        text = state.syncStatusText,
                        style = ownerTextStyle(
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnBrand,
                            tabularNumbers = true
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Tombol CTA "Tambah produk": tinggi 56dp, radius 16dp, ikon plus 22dp, gap 8dp, teks 16sp SemiBold.
 * Ponsel: latar hijau brand (#1F6B52), teks putih; Tablet: latar putih (#FFFFFF), teks hijau.
 */
@Composable
internal fun StokAddProductButton(
    invertedOnGreen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (invertedOnGreen) SurfaceWhite else Brand
    val contentColor = if (invertedOnGreen) Brand else OnBrand

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { contentDescription = "Tambah produk" }
            .testTag("stok-primary-add-product"),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OwnerVectorIcon(
            imageVector = OwnerIcons.Plus,
            tint = contentColor,
            size = 22.dp
        )
        Text(
            text = "Tambah produk",
            style = ownerTextStyle(
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            ),
            maxLines = 1
        )
    }
}
