package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.ui.auth.DotsPattern
import com.tbterminal.app.ui.auth.RingsPattern
import com.tbterminal.app.ui.salestransactions.AdminTransactionHistoryUiState
import com.tbterminal.app.ui.salestransactions.SalesSortOrder
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.BrandSoftText
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.SurfaceWhite
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val salesHeroSymbols = DecimalFormatSymbols(Locale.forLanguageTag("id-ID")).apply {
    groupingSeparator = '.'
    decimalSeparator = ','
}

@Immutable
internal data class OwnerSalesSummaryUiState(
    val totalSales: BigDecimal = BigDecimal.ZERO,
    val totalTransactions: Long = 0L,
    val paidTotal: BigDecimal = BigDecimal.ZERO,
    val debtDoTotal: BigDecimal = BigDecimal.ZERO
)

/**
 * TODO: Ringkasan header saat ini dihitung dari transaksi halaman aktif (`uiState.transactions`)
 * karena `CashTransactionPage` belum mengembalikan agregat total penjualan, Lunas, dan Hutang/DO
 * lintas seluruh halaman. Selain itu, konfirmasikan apakah untuk transaksi `DP`/`partial` kolom
 * `Lunas` mengambil `paidAmount` atau hanya transaksi berstatus `Lunas`, serta apakah transaksi
 * `voided` dikecualikan agar rumus `Total penjualan = Lunas + Hutang / DO` selalu tepat.
 */
internal fun AdminTransactionHistoryUiState.toOwnerSalesSummary(): OwnerSalesSummaryUiState {
    val activeTransactions = transactions.filterNot { it.isVoidedSale() }
    var totalSales = BigDecimal.ZERO
    var paidTotal = BigDecimal.ZERO
    var debtDoTotal = BigDecimal.ZERO

    activeTransactions.forEach { tx ->
        val txTotal = tx.total.coerceAtLeast(BigDecimal.ZERO)
        totalSales = totalSales.add(txTotal)
        if (tx.isPaidSale()) {
            paidTotal = paidTotal.add(txTotal)
        } else {
            val paidPart = tx.paidAmount.coerceIn(BigDecimal.ZERO, txTotal)
            val debtPart = txTotal.subtract(paidPart).coerceAtLeast(BigDecimal.ZERO)
            paidTotal = paidTotal.add(paidPart)
            debtDoTotal = debtDoTotal.add(debtPart)
        }
    }

    return OwnerSalesSummaryUiState(
        totalSales = totalSales,
        totalTransactions = total.coerceAtLeast(0L),
        paidTotal = paidTotal,
        debtDoTotal = debtDoTotal
    )
}

private fun CashTransaction.isVoidedSale(): Boolean =
    status.trim().lowercase() in setOf("voided", "void", "dibatalkan", "cancelled", "refunded")

private fun CashTransaction.isPaidSale(): Boolean =
    status.trim().lowercase() in setOf("lunas", "paid", "success", "completed")

internal fun formatSalesHeroAmount(amount: BigDecimal): String {
    val formatter = DecimalFormat("#,##0", salesHeroSymbols)
    return formatter.format(amount.coerceAtLeast(BigDecimal.ZERO))
}

internal fun formatSalesColumnCompactRupiah(amount: BigDecimal): String =
    formatRupiahCompact(amount.coerceAtLeast(BigDecimal.ZERO).toDouble())

internal fun formatHistoryCountLabel(count: Long, unit: String): String {
    val formatter = DecimalFormat("#,##0", salesHeroSymbols)
    return "${formatter.format(count.coerceAtLeast(0L))} $unit"
}

internal fun formatSalesCountLabel(count: Long): String = formatHistoryCountLabel(count, "transaksi")

internal fun salesSortOrderShortLabel(order: SalesSortOrder): String = when (order) {
    SalesSortOrder.Newest -> "Urut: Terbaru"
    SalesSortOrder.HighestAmount -> "Urut: Terbesar"
    SalesSortOrder.ReceiptAsc -> "Urut: No. Struk"
}

/**
 * Baris top bar halaman sekunder Riwayat Penjualan: tinggi 48dp, margin kiri -12dp, jarak item 0dp,
 * tombol kembali 48x48dp, judul 20/26 satu baris, serta 3 ikon aksi kanan (Cari, Filter, Laporan)
 * masing-masing 44x48dp dengan margin kanan -10dp pada ikon terakhir.
 */
@Composable
internal fun SecondaryHeader(
    title: String,
    onBack: () -> Unit,
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
    onReportClick: () -> Unit,
    modifier: Modifier = Modifier,
    isTablet: Boolean = false,
    backTestTag: String = "sales-history-back-btn",
    searchTestTag: String = "sales-history-toggle-search",
    filterTestTag: String = "sales-history-open-filters",
    reportTestTag: String = "sales-history-report-btn",
    searchContentDescription: String = "Cari transaksi",
    filterContentDescription: String = "Filter transaksi",
    reportContentDescription: String = "Laporan penjualan"
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val actionWidth = if (isTablet) 48.dp else 44.dp
    val endOutsetDp = if (isTablet) 12.dp else 10.dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val startOutsetPx = 12.dp.roundToPx()
                val endOutsetPx = endOutsetDp.roundToPx()
                val expandedWidth = if (constraints.hasBoundedWidth) {
                    constraints.maxWidth + startOutsetPx + endOutsetPx
                } else {
                    constraints.maxWidth
                }
                val placeable = measurable.measure(
                    Constraints(
                        minWidth = if (constraints.hasFixedWidth) expandedWidth else 0,
                        maxWidth = expandedWidth,
                        minHeight = constraints.minHeight,
                        maxHeight = constraints.maxHeight
                    )
                )
                val layoutWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else placeable.width
                layout(layoutWidth, placeable.height) {
                    placeable.placeRelative(x = -startOutsetPx, y = 0)
                }
            }
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onBack
                )
                .semantics { contentDescription = "Kembali" }
                .testTag(backTestTag),
            contentAlignment = Alignment.Center
        ) {
            OwnerVectorIcon(
                imageVector = OwnerIcons.ChevronLeft,
                tint = OnBrand,
                size = 24.dp
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            var maxTitleWidthPx by remember { mutableIntStateOf(0) }
            val titleSize = remember(title, maxTitleWidthPx, density.fontScale) {
                if (maxTitleWidthPx <= 0) {
                    18.sp
                } else {
                    pickFittingSingleLineFontSize(
                        candidatesSp = listOf(18f, 17f, 16f, 15f, 14f, 13f),
                        maxWidthPx = maxTitleWidthPx
                    ) { candidate ->
                        textMeasurer.measure(
                            text = title,
                            style = ownerTextStyle(
                                fontSize = candidate,
                                lineHeight = if (candidate == 18.sp) 22.sp else (candidate.value * 1.22f).sp,
                                fontWeight = FontWeight.SemiBold,
                                color = OnBrand,
                                letterSpacing = (-0.01).em
                            ),
                            maxLines = 1,
                            softWrap = false
                        ).size.width
                    }
                }
            }
            Text(
                text = title,
                style = ownerTextStyle(
                    fontSize = titleSize,
                    lineHeight = if (titleSize == 18.sp) 22.sp else (titleSize.value * 1.22f).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnBrand,
                    letterSpacing = (-0.01).em
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { maxTitleWidthPx = it.width }
                    .semantics { heading() }
            )
        }

        HeaderActionIconButton(
            icon = OwnerIcons.Search,
            contentDescription = searchContentDescription,
            onClick = onSearchClick,
            testTag = searchTestTag,
            width = actionWidth
        )
        HeaderActionIconButton(
            icon = OwnerIcons.FilterSliders,
            contentDescription = filterContentDescription,
            onClick = onFilterClick,
            testTag = filterTestTag,
            width = actionWidth
        )
        HeaderActionIconButton(
            icon = OwnerIcons.DocumentReport,
            contentDescription = reportContentDescription,
            onClick = onReportClick,
            testTag = reportTestTag,
            width = actionWidth
        )
    }
}

@Composable
private fun HeaderActionIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String,
    width: Dp = 44.dp
) {
    Box(
        modifier = Modifier
            .size(width = width, height = 48.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { this.contentDescription = contentDescription }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        OwnerVectorIcon(
            imageVector = icon,
            tint = OnBrand,
            size = 24.dp
        )
    }
}

@Immutable
internal data class OwnerHistoryHeroMetric(
    val label: String,
    val value: String,
    val valueTestTag: String
)

/**
 * Ringkasan penjualan di header hijau (dipakai bersama keadaan kosong dan berisi data):
 * "Total penjualan", "Rp" + nominal penuh (mengecil otomatis hingga 28sp), garis 1dp putih 18%,
 * serta 3 kolom sama lebar (Total transaksi | Lunas | Hutang / DO).
 */
@Composable
internal fun SalesSummary(
    summary: OwnerSalesSummaryUiState,
    modifier: Modifier = Modifier,
    isTablet: Boolean = false
) {
    val countText = remember(summary.totalTransactions) { formatSalesCountLabel(summary.totalTransactions) }
    val paidText = remember(summary.paidTotal) { formatSalesColumnCompactRupiah(summary.paidTotal) }
    val debtText = remember(summary.debtDoTotal) { formatSalesColumnCompactRupiah(summary.debtDoTotal) }

    OwnerHistoryHeroSummary(
        totalLabel = "Total penjualan",
        totalAmount = summary.totalSales,
        totalTestTag = "sales-summary-total-amount",
        metrics = listOf(
            OwnerHistoryHeroMetric("Total transaksi", countText, "sales-summary-count"),
            OwnerHistoryHeroMetric("Lunas", paidText, "sales-summary-paid"),
            OwnerHistoryHeroMetric("Hutang / DO", debtText, "sales-summary-debt")
        ),
        modifier = modifier,
        isTablet = isTablet
    )
}

/**
 * Komponen ringkasan hero riwayat transaksi (Penjualan / Pembelian) di header hijau:
 * label total, "Rp" + nominal penuh (mengecil otomatis hingga 28sp), garis 1dp putih 18%,
 * serta kolom metrik sama lebar yang dipisahkan hairline vertikal 1dp putih 18%.
 */
@Composable
internal fun OwnerHistoryHeroSummary(
    totalLabel: String,
    totalAmount: BigDecimal,
    totalTestTag: String,
    metrics: List<OwnerHistoryHeroMetric>,
    modifier: Modifier = Modifier,
    isTablet: Boolean = false
) {
    val lineColor = Color.White.copy(alpha = 0.18f)
    val heroAmountText = remember(totalAmount) { formatSalesHeroAmount(totalAmount) }

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = totalLabel,
            style = ownerTextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                color = BrandSoftText
            ),
            maxLines = 1,
            modifier = Modifier.cssLineBoxHeight(20.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        var heroRowWidthPx by remember { mutableIntStateOf(0) }
        val rpWidthPx = remember(density.fontScale) {
            textMeasurer.measure(
                text = "Rp",
                style = ownerTextStyle(
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandSoftText
                ),
                maxLines = 1,
                softWrap = false
            ).size.width
        }
        val gapPx = with(density) { 6.dp.roundToPx() }
        val availableNumberWidthPx = (heroRowWidthPx - rpWidthPx - gapPx).coerceAtLeast(1)

        val baseHeroSp = if (isTablet) 48 else 40
        val heroFontSize = remember(heroAmountText, heroRowWidthPx, availableNumberWidthPx, density.fontScale, isTablet) {
            if (heroRowWidthPx <= 0) {
                baseHeroSp.sp
            } else {
                pickFittingSingleLineFontSize(
                    candidatesSp = (baseHeroSp downTo 28 step 2).map { it.toFloat() },
                    maxWidthPx = availableNumberWidthPx
                ) { candidate ->
                    textMeasurer.measure(
                        text = heroAmountText,
                        style = ownerTextStyle(
                            fontSize = candidate,
                            lineHeight = when (candidate) {
                                48.sp -> 54.sp
                                40.sp -> 46.sp
                                else -> (candidate.value * 1.15f).sp
                            },
                            fontWeight = FontWeight.Bold,
                            color = OnBrand,
                            letterSpacing = (-0.03).em,
                            tabularNumbers = true
                        ),
                        maxLines = 1,
                        softWrap = false
                    ).size.width
                }
            }
        }
        val heroLineHeight = when (heroFontSize) {
            48.sp -> 54.sp
            40.sp -> 46.sp
            else -> (heroFontSize.value * 1.15f).sp
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { heroRowWidthPx = it.width },
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "Rp",
                style = ownerTextStyle(
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandSoftText
                ),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.alignByBaseline()
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = heroAmountText,
                style = ownerTextStyle(
                    fontSize = heroFontSize,
                    lineHeight = heroLineHeight,
                    fontWeight = FontWeight.Bold,
                    color = OnBrand,
                    letterSpacing = (-0.03).em,
                    tabularNumbers = true
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .alignByBaseline()
                    .cssLineBoxHeight((heroLineHeight.value * density.fontScale).dp)
                    .testTag(totalTestTag)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

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
            metrics.forEachIndexed { index, metric ->
                SalesSummaryMetricColumn(
                    label = metric.label,
                    value = metric.value,
                    valueTestTag = metric.valueTestTag,
                    modifier = if (index == 0) {
                        Modifier.weight(1f)
                    } else {
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .ownerHeroLeftDivider(lineColor)
                    }
                )
            }
        }
    }
}

@Composable
private fun SalesSummaryMetricColumn(
    label: String,
    value: String,
    valueTestTag: String,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    Column(modifier = modifier) {
        Text(
            text = label,
            style = ownerTextStyle(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Normal,
                color = BrandSoftText
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.cssLineBoxHeight(16.dp)
        )

        Spacer(modifier = Modifier.height(2.dp))

        var maxValueWidthPx by remember { mutableIntStateOf(0) }
        val valueSize = remember(value, maxValueWidthPx, density.fontScale) {
            if (maxValueWidthPx <= 0) {
                16.sp
            } else {
                pickFittingSingleLineFontSize(
                    candidatesSp = listOf(16f, 14.5f, 13f, 12f),
                    maxWidthPx = maxValueWidthPx
                ) { candidate ->
                    textMeasurer.measure(
                        text = value,
                        style = ownerTextStyle(
                            fontSize = candidate,
                            lineHeight = if (candidate == 16.sp) 20.sp else (candidate.value * 1.25f).sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnBrand,
                            tabularNumbers = true
                        ),
                        maxLines = 1,
                        softWrap = false
                    ).size.width
                }
            }
        }
        val valueLineHeight = if (valueSize == 16.sp) 20.sp else (valueSize.value * 1.25f).sp
        Text(
            text = value,
            style = ownerTextStyle(
                fontSize = valueSize,
                lineHeight = valueLineHeight,
                fontWeight = FontWeight.SemiBold,
                color = OnBrand,
                tabularNumbers = true
            ),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .cssLineBoxHeight((valueLineHeight.value * density.fontScale).dp)
                .onSizeChanged { maxValueWidthPx = it.width }
                .testTag(valueTestTag)
        )
    }
}

/**
 * Keadaan hasil kosong Riwayat Penjualan: lingkaran putih 88dp polos (tanpa border/bayangan),
 * ikon pencarian-silang 40dp warna Brand, judul 18/600 (24dp di bawah ikon), penjelasan 14/20 Muted
 * (8dp di bawah judul, maks 280dp ponsel / 320dp tablet). Tanpa tombol apa pun.
 */
@Composable
internal fun EmptyResult(
    title: String,
    description: String?,
    descriptionMaxWidth: Dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(SurfaceWhite),
            contentAlignment = Alignment.Center
        ) {
            OwnerVectorIcon(
                imageVector = OwnerIcons.SearchNotFound,
                tint = Brand,
                size = 40.dp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = ownerTextStyle(
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )

        if (!description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = ownerTextStyle(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = Muted
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = descriptionMaxWidth)
            )
        }
    }
}

private val OWNER_HISTORY_TABLET_BREAKPOINT = 840.dp

@Composable
internal fun OwnerHistoryResponsiveLayout(
    title: String,
    panelTitle: String,
    sortLabel: String,
    sortLabelTestTag: String,
    searchPlaceholder: String,
    backTestTag: String,
    searchTestTag: String,
    filterTestTag: String,
    reportTestTag: String,
    searchBackTestTag: String,
    searchInputTestTag: String,
    searchClearTestTag: String,
    searchContentDescription: String,
    filterContentDescription: String,
    reportContentDescription: String,
    isSearchExpanded: Boolean,
    searchQuery: String,
    isLoading: Boolean,
    errorMessage: String?,
    isEmpty: Boolean,
    emptyTitle: String,
    emptyDescription: String?,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onReportClick: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    heroSummary: @Composable (isTablet: Boolean) -> Unit,
    loadingContent: @Composable (isTablet: Boolean) -> Unit,
    errorContent: @Composable (message: String) -> Unit,
    dataContent: @Composable (isTablet: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    autoFocusSearch: Boolean = true
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        val isTablet = maxWidth >= OWNER_HISTORY_TABLET_BREAKPOINT
        if (isTablet) {
            TabletOwnerHistoryLayout(
                title = title,
                panelTitle = panelTitle,
                sortLabel = sortLabel,
                sortLabelTestTag = sortLabelTestTag,
                searchPlaceholder = searchPlaceholder,
                backTestTag = backTestTag,
                searchTestTag = searchTestTag,
                filterTestTag = filterTestTag,
                reportTestTag = reportTestTag,
                searchBackTestTag = searchBackTestTag,
                searchInputTestTag = searchInputTestTag,
                searchClearTestTag = searchClearTestTag,
                searchContentDescription = searchContentDescription,
                filterContentDescription = filterContentDescription,
                reportContentDescription = reportContentDescription,
                isSearchExpanded = isSearchExpanded,
                searchQuery = searchQuery,
                isLoading = isLoading,
                errorMessage = errorMessage,
                isEmpty = isEmpty,
                emptyTitle = emptyTitle,
                emptyDescription = emptyDescription,
                onBack = onBack,
                onOpenSearch = onOpenSearch,
                onCloseSearch = onCloseSearch,
                onSearchChanged = onSearchChanged,
                onOpenFilter = onOpenFilter,
                onReportClick = onReportClick,
                onNavSelect = onNavSelect,
                autoFocusSearch = autoFocusSearch,
                heroSummary = { heroSummary(true) },
                loadingContent = { loadingContent(true) },
                errorContent = errorContent,
                dataContent = { dataContent(true) }
            )
        } else {
            PhoneOwnerHistoryLayout(
                title = title,
                panelTitle = panelTitle,
                sortLabel = sortLabel,
                sortLabelTestTag = sortLabelTestTag,
                searchPlaceholder = searchPlaceholder,
                backTestTag = backTestTag,
                searchTestTag = searchTestTag,
                filterTestTag = filterTestTag,
                reportTestTag = reportTestTag,
                searchBackTestTag = searchBackTestTag,
                searchInputTestTag = searchInputTestTag,
                searchClearTestTag = searchClearTestTag,
                searchContentDescription = searchContentDescription,
                filterContentDescription = filterContentDescription,
                reportContentDescription = reportContentDescription,
                isSearchExpanded = isSearchExpanded,
                searchQuery = searchQuery,
                isLoading = isLoading,
                errorMessage = errorMessage,
                isEmpty = isEmpty,
                emptyTitle = emptyTitle,
                emptyDescription = emptyDescription,
                onBack = onBack,
                onOpenSearch = onOpenSearch,
                onCloseSearch = onCloseSearch,
                onSearchChanged = onSearchChanged,
                onOpenFilter = onOpenFilter,
                onReportClick = onReportClick,
                autoFocusSearch = autoFocusSearch,
                heroSummary = { heroSummary(false) },
                loadingContent = { loadingContent(false) },
                errorContent = errorContent,
                dataContent = { dataContent(false) }
            )
        }
    }
}

@Composable
private fun PhoneOwnerHistoryLayout(
    title: String,
    panelTitle: String,
    sortLabel: String,
    sortLabelTestTag: String,
    searchPlaceholder: String,
    backTestTag: String,
    searchTestTag: String,
    filterTestTag: String,
    reportTestTag: String,
    searchBackTestTag: String,
    searchInputTestTag: String,
    searchClearTestTag: String,
    searchContentDescription: String,
    filterContentDescription: String,
    reportContentDescription: String,
    isSearchExpanded: Boolean,
    searchQuery: String,
    isLoading: Boolean,
    errorMessage: String?,
    isEmpty: Boolean,
    emptyTitle: String,
    emptyDescription: String?,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onReportClick: () -> Unit,
    autoFocusSearch: Boolean,
    heroSummary: @Composable () -> Unit,
    loadingContent: @Composable () -> Unit,
    errorContent: @Composable (message: String) -> Unit,
    dataContent: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        val viewportHeightPx = with(LocalDensity.current) { maxHeight.roundToPx() }
        val overlapPx = with(LocalDensity.current) { 28.dp.roundToPx() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Layout(
                content = {
                    HistoryPhoneGreenHeader(
                        title = title,
                        searchPlaceholder = searchPlaceholder,
                        backTestTag = backTestTag,
                        searchTestTag = searchTestTag,
                        filterTestTag = filterTestTag,
                        reportTestTag = reportTestTag,
                        searchBackTestTag = searchBackTestTag,
                        searchInputTestTag = searchInputTestTag,
                        searchClearTestTag = searchClearTestTag,
                        searchContentDescription = searchContentDescription,
                        filterContentDescription = filterContentDescription,
                        reportContentDescription = reportContentDescription,
                        isSearchExpanded = isSearchExpanded,
                        searchQuery = searchQuery,
                        onBack = onBack,
                        onOpenSearch = onOpenSearch,
                        onCloseSearch = onCloseSearch,
                        onSearchChanged = onSearchChanged,
                        onOpenFilter = onOpenFilter,
                        onReportClick = onReportClick,
                        autoFocusSearch = autoFocusSearch,
                        heroSummary = heroSummary
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                            .background(SurfaceWhite)
                            .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 32.dp)
                    ) {
                        HistoryPanelHeaderRow(
                            panelTitle = panelTitle,
                            sortLabel = sortLabel,
                            sortLabelTestTag = sortLabelTestTag
                        )

                        when {
                            isLoading -> {
                                Spacer(modifier = Modifier.height(16.dp))
                                loadingContent()
                            }
                            errorMessage != null -> {
                                Spacer(modifier = Modifier.height(24.dp))
                                errorContent(errorMessage)
                            }
                            isEmpty -> {
                                Spacer(modifier = Modifier.height(72.dp))
                                EmptyResult(
                                    title = emptyTitle,
                                    description = emptyDescription,
                                    descriptionMaxWidth = 280.dp
                                )
                            }
                            else -> {
                                Spacer(modifier = Modifier.height(16.dp))
                                dataContent()
                            }
                        }
                    }
                }
            ) { measurables, constraints ->
                val headerPlaceable = measurables[0].measure(
                    constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                )
                val minPanelHeight = (viewportHeightPx - (headerPlaceable.height - overlapPx)).coerceAtLeast(0)
                val panelPlaceable = measurables[1].measure(
                    constraints.copy(minHeight = minPanelHeight, maxHeight = Constraints.Infinity)
                )
                val totalHeight = (headerPlaceable.height - overlapPx + panelPlaceable.height)
                    .coerceAtLeast(headerPlaceable.height)

                layout(constraints.maxWidth, totalHeight) {
                    headerPlaceable.placeRelative(0, 0)
                    panelPlaceable.placeRelative(0, headerPlaceable.height - overlapPx)
                }
            }
        }
    }
}

@Composable
private fun TabletOwnerHistoryLayout(
    title: String,
    panelTitle: String,
    sortLabel: String,
    sortLabelTestTag: String,
    searchPlaceholder: String,
    backTestTag: String,
    searchTestTag: String,
    filterTestTag: String,
    reportTestTag: String,
    searchBackTestTag: String,
    searchInputTestTag: String,
    searchClearTestTag: String,
    searchContentDescription: String,
    filterContentDescription: String,
    reportContentDescription: String,
    isSearchExpanded: Boolean,
    searchQuery: String,
    isLoading: Boolean,
    errorMessage: String?,
    isEmpty: Boolean,
    emptyTitle: String,
    emptyDescription: String?,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onReportClick: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    autoFocusSearch: Boolean,
    heroSummary: @Composable () -> Unit,
    loadingContent: @Composable () -> Unit,
    errorContent: @Composable (message: String) -> Unit,
    dataContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        OwnerTabletNavRail(
            activeDestination = DashboardNavDestination.TRANSAKSI,
            onNavSelect = onNavSelect
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            HistoryTabletGreenPanel(
                title = title,
                searchPlaceholder = searchPlaceholder,
                backTestTag = backTestTag,
                searchTestTag = searchTestTag,
                filterTestTag = filterTestTag,
                reportTestTag = reportTestTag,
                searchBackTestTag = searchBackTestTag,
                searchInputTestTag = searchInputTestTag,
                searchClearTestTag = searchClearTestTag,
                searchContentDescription = searchContentDescription,
                filterContentDescription = filterContentDescription,
                reportContentDescription = reportContentDescription,
                isSearchExpanded = isSearchExpanded,
                searchQuery = searchQuery,
                onBack = onBack,
                onOpenSearch = onOpenSearch,
                onCloseSearch = onCloseSearch,
                onSearchChanged = onSearchChanged,
                onOpenFilter = onOpenFilter,
                onReportClick = onReportClick,
                autoFocusSearch = autoFocusSearch,
                heroSummary = heroSummary
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 400.dp)
                    .clip(RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp))
                    .background(SurfaceWhite)
            ) {
                val panelMinEmptyHeight = (maxHeight - 64.dp - 20.dp).coerceAtLeast(0.dp)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 64.dp, top = 40.dp, end = 32.dp, bottom = 40.dp)
                ) {
                    HistoryPanelHeaderRow(
                        panelTitle = panelTitle,
                        sortLabel = sortLabel,
                        sortLabelTestTag = sortLabelTestTag
                    )

                    when {
                        isLoading -> {
                            Spacer(modifier = Modifier.height(16.dp))
                            loadingContent()
                        }
                        errorMessage != null -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = panelMinEmptyHeight),
                                contentAlignment = Alignment.Center
                            ) {
                                errorContent(errorMessage)
                            }
                        }
                        isEmpty -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = panelMinEmptyHeight)
                                    .padding(bottom = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyResult(
                                    title = emptyTitle,
                                    description = emptyDescription,
                                    descriptionMaxWidth = 320.dp
                                )
                            }
                        }
                        else -> {
                            Spacer(modifier = Modifier.height(16.dp))
                            dataContent()
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun HistoryPhoneGreenHeader(
    title: String,
    searchPlaceholder: String,
    backTestTag: String,
    searchTestTag: String,
    filterTestTag: String,
    reportTestTag: String,
    searchBackTestTag: String,
    searchInputTestTag: String,
    searchClearTestTag: String,
    searchContentDescription: String,
    filterContentDescription: String,
    reportContentDescription: String,
    isSearchExpanded: Boolean,
    searchQuery: String,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onReportClick: () -> Unit,
    autoFocusSearch: Boolean,
    heroSummary: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brand)
            .clipToBounds()
    ) {
        Box(modifier = Modifier.matchParentSize()) {
            RingsPattern(
                radii = listOf(50.dp, 95.dp, 140.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .wrapContentSize(align = Alignment.TopEnd, unbounded = true)
                    .offset(x = 90.dp, y = (-90).dp)
                    .size(300.dp)
            )

            DotsPattern(
                dotRadius = 1.5.dp,
                step = 20.dp,
                maskAtBottomRight = true,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(width = 190.dp, height = 110.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 56.dp)
        ) {
            if (isSearchExpanded) {
                SearchTopBar(
                    query = searchQuery,
                    onQueryChanged = onSearchChanged,
                    onCloseSearch = onCloseSearch,
                    autoFocus = autoFocusSearch,
                    placeholder = searchPlaceholder,
                    backTestTag = searchBackTestTag,
                    inputTestTag = searchInputTestTag,
                    clearTestTag = searchClearTestTag
                )
            } else {
                SecondaryHeader(
                    title = title,
                    onBack = onBack,
                    onSearchClick = onOpenSearch,
                    onFilterClick = onOpenFilter,
                    onReportClick = onReportClick,
                    isTablet = false,
                    backTestTag = backTestTag,
                    searchTestTag = searchTestTag,
                    filterTestTag = filterTestTag,
                    reportTestTag = reportTestTag,
                    searchContentDescription = searchContentDescription,
                    filterContentDescription = filterContentDescription,
                    reportContentDescription = reportContentDescription
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            heroSummary()
        }
    }
}

@Composable
internal fun HistoryTabletGreenPanel(
    title: String,
    searchPlaceholder: String,
    backTestTag: String,
    searchTestTag: String,
    filterTestTag: String,
    reportTestTag: String,
    searchBackTestTag: String,
    searchInputTestTag: String,
    searchClearTestTag: String,
    searchContentDescription: String,
    filterContentDescription: String,
    reportContentDescription: String,
    isSearchExpanded: Boolean,
    searchQuery: String,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onReportClick: () -> Unit,
    autoFocusSearch: Boolean,
    heroSummary: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .width(432.dp)
            .fillMaxHeight()
            .background(Brand)
            .clipToBounds()
    ) {
        RingsPattern(
            radii = listOf(50.dp, 95.dp, 140.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .wrapContentSize(align = Alignment.TopEnd, unbounded = true)
                .offset(x = 90.dp, y = (-90).dp)
                .size(300.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, top = 40.dp, end = 56.dp, bottom = 40.dp)
        ) {
            if (isSearchExpanded) {
                SearchTopBar(
                    query = searchQuery,
                    onQueryChanged = onSearchChanged,
                    onCloseSearch = onCloseSearch,
                    autoFocus = autoFocusSearch,
                    placeholder = searchPlaceholder,
                    backTestTag = searchBackTestTag,
                    inputTestTag = searchInputTestTag,
                    clearTestTag = searchClearTestTag
                )
            } else {
                SecondaryHeader(
                    title = title,
                    onBack = onBack,
                    onSearchClick = onOpenSearch,
                    onFilterClick = onOpenFilter,
                    onReportClick = onReportClick,
                    isTablet = true,
                    backTestTag = backTestTag,
                    searchTestTag = searchTestTag,
                    filterTestTag = filterTestTag,
                    reportTestTag = reportTestTag,
                    searchContentDescription = searchContentDescription,
                    filterContentDescription = filterContentDescription,
                    reportContentDescription = reportContentDescription
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                heroSummary()
            }
        }
    }
}

@Composable
internal fun HistoryPanelHeaderRow(
    panelTitle: String,
    sortLabel: String,
    sortLabelTestTag: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = panelTitle,
            style = ownerTextStyle(
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink
            ),
            maxLines = 1,
            modifier = Modifier
                .cssLineBoxHeight(20.dp)
                .semantics { heading() }
        )

        Text(
            text = sortLabel,
            style = ownerTextStyle(
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
                color = Muted
            ),
            maxLines = 1,
            modifier = Modifier
                .cssLineBoxHeight(18.dp)
                .testTag(sortLabelTestTag)
        )
    }
}

private inline fun pickFittingSingleLineFontSize(
    candidatesSp: List<Float>,
    maxWidthPx: Int,
    measureWidthPx: (TextUnit) -> Int
): TextUnit {
    candidatesSp.forEach { sizeValue ->
        val candidate = sizeValue.sp
        if (measureWidthPx(candidate) <= maxWidthPx) {
            return candidate
        }
    }
    return candidatesSp.last().sp
}
