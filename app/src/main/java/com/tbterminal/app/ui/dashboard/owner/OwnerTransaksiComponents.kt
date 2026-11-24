package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.BrandSoftText
import com.tbterminal.app.ui.theme.ChipBg
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.OwnerAmberInk
import com.tbterminal.app.ui.theme.OwnerDivider
import com.tbterminal.app.ui.theme.OwnerSyncDot
import com.tbterminal.app.ui.theme.SurfaceWhite

internal fun TransaksiTab.displayLabel(): String = when (this) {
    TransaksiTab.PENJUALAN -> "Penjualan"
    TransaksiTab.PEMBELIAN -> "Pembelian"
}

/**
 * Baris header halaman Transaksi: judul "Transaksi" (18/SemiBold #FFFFFF, selaras dengan
 * Beranda/Keuangan/Stok) di kiri, kapsul pemilih jenis transaksi, dan avatar profil 40dp di kanan.
 */
@Composable
internal fun TransaksiDropdownHeaderRow(
    activeType: TransaksiTab,
    expanded: Boolean,
    userName: String,
    synced: Boolean,
    onDropdownClick: () -> Unit,
    onProfileClick: () -> Unit,
    dropdownFocusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    OwnerHeaderTitleRow(
        title = "Transaksi",
        userName = userName,
        synced = synced,
        onProfileClick = onProfileClick,
        modifier = modifier,
        titleTestTag = "transaksi-header-label",
        trailingContent = {
            TransactionTypeDropdown(
                activeType = activeType,
                expanded = expanded,
                onClick = onDropdownClick,
                modifier = Modifier.focusRequester(dropdownFocusRequester)
            )
        }
    )
}

/**
 * Kapsul pemilih jenis transaksi di header: tinggi visual 36dp (area sentuh 48dp),
 * border putih 1dp (alpha 0.32 tertutup / 0.70 terbuka), tanpa latar maupun bayangan.
 */
@Composable
internal fun TransactionTypeDropdown(
    activeType: TransaksiTab,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeLabel = activeType.displayLabel()
    OwnerHeaderDropdownCapsule(
        label = activeLabel,
        expanded = expanded,
        contentDescription = "Jenis transaksi: $activeLabel",
        testTag = "transaksi-type-dropdown",
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
private fun transactionTypeSelectorOptions(): List<OwnerSelectorOptionSpec<TransaksiTab>> = listOf(
    OwnerSelectorOptionSpec(
        value = TransaksiTab.PENJUALAN,
        title = "Penjualan",
        description = "Catat jual dan riwayat penjualan",
        icon = OwnerIcons.Receipt,
        iconSize = 20.dp,
        unselectedIconTint = Ink,
        testTag = "transaksi-option-PENJUALAN"
    ),
    OwnerSelectorOptionSpec(
        value = TransaksiTab.PEMBELIAN,
        title = "Pembelian",
        description = "Barang masuk dan riwayat pembelian",
        icon = OwnerIcons.Truck,
        iconSize = 22.dp,
        unselectedIconTint = Brand,
        testTag = "transaksi-option-PEMBELIAN"
    )
)

/**
 * Pemilih jenis transaksi dalam jendela Dialog kustom:
 * bottom sheet di ponsel dan dialog tengah (480dp) di tablet, dengan scrim #14201B alpha 0.4.
 */
@Composable
internal fun TransactionTypeSelector(
    selectedTab: TransaksiTab,
    isTablet: Boolean,
    onSelect: (TransaksiTab) -> Unit,
    onDismiss: () -> Unit
) {
    OwnerSelectorDialog(
        title = "Jenis transaksi",
        options = transactionTypeSelectorOptions(),
        selectedValue = selectedTab,
        isTablet = isTablet,
        onSelect = onSelect,
        onDismiss = onDismiss,
        scrimTestTag = "transaksi-selector-scrim",
        sheetTestTag = "transaksi-selector-sheet",
        handleTestTag = "transaksi-selector-handle"
    )
}

@Composable
internal fun TransactionTypeSelectorFrame(
    selectedTab: TransaksiTab,
    isTablet: Boolean,
    onSelect: (TransaksiTab) -> Unit,
    onDismiss: () -> Unit,
    panelFocusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    OwnerSelectorFrame(
        title = "Jenis transaksi",
        options = transactionTypeSelectorOptions(),
        selectedValue = selectedTab,
        isTablet = isTablet,
        onSelect = onSelect,
        onDismiss = onDismiss,
        scrimTestTag = "transaksi-selector-scrim",
        sheetTestTag = "transaksi-selector-sheet",
        handleTestTag = "transaksi-selector-handle",
        panelFocusRequester = panelFocusRequester,
        modifier = modifier
    )
}

/**
 * Chip periode pada baris ringkasan: padding 4x10dp, radius penuh,
 * latar rgba(0,0,0,0.20), titik 8dp #5FD3A1, jarak 6dp.
 */
@Composable
internal fun TransaksiPeriodStatusChip(
    label: String,
    modifier: Modifier = Modifier
) {
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
                .background(OwnerSyncDot)
        )
        Text(
            text = label,
            style = ownerTextStyle(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                color = OnBrand
            ),
            maxLines = 1
        )
    }
}

/**
 * Baris footer pada header hijau: garis atas 1dp putih 18%, padding atas 16dp.
 */
@Composable
internal fun TransaksiSummaryFooterRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val strokePx = 1.dp.toPx()
                drawLine(
                    color = Color.White.copy(alpha = 0.18f),
                    start = Offset(0f, strokePx / 2f),
                    end = Offset(size.width, strokePx / 2f),
                    strokeWidth = strokePx
                )
            }
            .padding(top = 17.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = ownerTextStyle(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Normal,
                color = BrandSoftText
            ),
            maxLines = 1
        )
        Text(
            text = value,
            style = ownerTextStyle(
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnBrand,
                tabularNumbers = true
            ),
            maxLines = 1
        )
    }
}

/**
 * Tombol aksi utama (CTA): tinggi 48dp, radius 16dp, ikon 20dp, teks 14sp/SemiBold, jarak 8dp
 * (selaras dengan skala tombol dan ikon di Dashboard Owner).
 * Di ponsel berlatar hijau teks putih; di tablet berlatar putih teks hijau.
 */
@Composable
internal fun TransaksiPrimaryCtaButton(
    tab: TransaksiTab,
    invertedOnGreen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = if (tab == TransaksiTab.PENJUALAN) "Catat jual" else "Tambah barang masuk"
    val icon = if (tab == TransaksiTab.PENJUALAN) OwnerIcons.Cart else OwnerIcons.Plus
    val containerColor = if (invertedOnGreen) SurfaceWhite else Brand
    val contentColor = if (invertedOnGreen) Brand else OnBrand

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { contentDescription = label }
            .testTag("transaksi-primary-cta"),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OwnerVectorIcon(
            imageVector = icon,
            tint = contentColor,
            size = 20.dp
        )
        Text(
            text = label,
            style = ownerTextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            ),
            maxLines = 1
        )
    }
}

/**
 * Header seksi daftar aktivitas: judul di kiri, tautan "Lihat riwayat" (48dp sentuh) di kanan.
 * Ponsel: tinggi baris 20dp dengan jarak bawah 8dp; Tablet: tinggi min 48dp tanpa jarak bawah.
 */
@Composable
internal fun TransaksiActivitySectionHeader(
    tab: TransaksiTab,
    isTablet: Boolean,
    onSeeHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = if (tab == TransaksiTab.PENJUALAN) "Aktivitas penjualan" else "Aktivitas pembelian"
    val baseModifier = if (isTablet) {
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    } else {
        modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    }

    Row(
        modifier = baseModifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = ownerTextStyle(
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink
            )
        )

        Box(
            modifier = Modifier.height(20.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .requiredHeight(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                        onClick = onSeeHistory
                    )
                    .padding(start = 16.dp, top = 14.dp, bottom = 14.dp)
                    .semantics { contentDescription = "Lihat riwayat" }
                    .testTag("transaksi-see-history"),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "Lihat riwayat",
                    style = ownerTextStyle(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Brand
                    )
                )
            }
        }
    }
}

/**
 * Daftar aktivitas transaksi dengan tinggi baris 88dp, garis pemisah 1dp (#EDF0EE) mulai 52dp
 * hanya di antara baris, state loading #F3F5F3 tanpa animasi, serta state kosong per tab.
 */
@Composable
internal fun TransaksiActivityList(
    tab: TransaksiTab,
    items: List<TransaksiItemUi>,
    loading: Boolean,
    isTablet: Boolean,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        when {
            loading -> {
                val skeletonCount = if (isTablet) 6 else 4
                repeat(skeletonCount) { index ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 52.dp)
                                .height(1.dp)
                                .background(OwnerDivider)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .heightIn(min = 64.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ChipBg)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(128.dp)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ChipBg)
                            )
                            Box(
                                modifier = Modifier
                                    .width(88.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ChipBg)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(78.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(ChipBg)
                        )
                    }
                }
            }

            items.isEmpty() -> {
                val emptyIcon = if (tab == TransaksiTab.PENJUALAN) OwnerIcons.Receipt else OwnerIcons.Truck
                val emptyTitle = if (tab == TransaksiTab.PENJUALAN) {
                    "Belum ada penjualan"
                } else {
                    "Belum ada pembelian"
                }
                val emptySubtitle = if (tab == TransaksiTab.PENJUALAN) {
                    "Penjualan kasir yang selesai akan muncul otomatis di sini."
                } else {
                    "Faktur barang masuk akan muncul di sini setelah dicatat."
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OwnerVectorIcon(
                        imageVector = emptyIcon,
                        tint = Muted,
                        size = 24.dp
                    )
                    Text(
                        text = emptyTitle,
                        style = ownerTextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = Ink
                        ),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = emptySubtitle,
                        style = ownerTextStyle(
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = Muted
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            else -> {
                items.forEachIndexed { index, item ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 52.dp)
                                .height(1.dp)
                                .background(OwnerDivider)
                        )
                    }
                    val formattedAmount = formatRupiahIdr(item.amount)
                    OwnerTransactionRow(
                        item = TransactionUi(
                            id = item.id,
                            title = item.title,
                            subtitle = item.subtitle,
                            amount = item.amount.toDouble()
                        ),
                        avatarBackground = SurfaceWhite,
                        balanceHidden = false,
                        onClick = { onItemClick(item.id) },
                        icon = if (item.kind == TransaksiItemKind.SALE) {
                            OwnerIcons.Receipt
                        } else {
                            OwnerIcons.Truck
                        },
                        subtitleColor = if (item.unpaid) OwnerAmberInk else Muted,
                        formattedAmount = formattedAmount,
                        semanticsLabel = "${item.title}, ${item.subtitle}, $formattedAmount",
                        modifier = Modifier.testTag("transaksi-row-${item.id}")
                    )
                }
            }
        }
    }
}
