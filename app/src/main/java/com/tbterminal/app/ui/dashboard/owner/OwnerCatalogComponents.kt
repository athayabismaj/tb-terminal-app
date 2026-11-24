package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.tbterminal.app.ui.auth.DotsPattern
import com.tbterminal.app.ui.auth.RingsPattern
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.dashboard.LocalBackofficePageNavigation
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.BrandSoftText
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.OwnerBorder
import com.tbterminal.app.ui.theme.OwnerDivider
import com.tbterminal.app.ui.theme.SurfaceWhite
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

internal val OWNER_CATALOG_TABLET_BREAKPOINT = 840.dp
private val CatalogTabletCheckColor = Color(0xFFA9DCC4)
private val CatalogDangerColor = Color(0xFFB3123F)
private val CatalogDangerBorderColor = Color(0xFFF5C6D3)

internal data class CatalogBreakdownMetric(
    val label: String,
    val value: String
)

internal enum class CatalogSaveErrorTarget {
    NONE,
    NAME,
    SYMBOL,
    NETWORK_TOAST,
    GENERIC_TOAST
}

internal fun isCatalogErrorMessage(message: String?): Boolean {
    if (message.isNullOrBlank()) return false
    return message.contains("gagal", ignoreCase = true) ||
        message.contains("tidak", ignoreCase = true) ||
        message.contains("kosong", ignoreCase = true)
}

internal fun classifyCatalogSaveError(message: String?): CatalogSaveErrorTarget {
    if (message.isNullOrBlank()) return CatalogSaveErrorTarget.NONE
    val lower = message.lowercase()
    val isSuccess = "berhasil" in lower && ("disimpan" in lower || "dihapus" in lower)
    if (isSuccess) return CatalogSaveErrorTarget.NONE
    val isNetwork = listOf("koneksi", "jaringan", "terhubung", "timeout", "network", "offline")
        .any { it in lower }
    if (isNetwork) return CatalogSaveErrorTarget.NETWORK_TOAST
    val isSymbol = listOf("simbol", "singkatan", "symbol").any { it in lower }
    if (isSymbol) return CatalogSaveErrorTarget.SYMBOL
    val isName = listOf("nama", "name", "duplikat", "sudah ada", "terdaftar").any { it in lower }
    if (isName) return CatalogSaveErrorTarget.NAME
    return CatalogSaveErrorTarget.GENERIC_TOAST
}

internal fun formatCatalogCreatedDate(rawCreatedAt: String): String {
    val trimmed = rawCreatedAt.trim()
    if (trimmed.isEmpty()) return "-"
    val zone = ZoneId.systemDefault()
    val date = runCatching { OffsetDateTime.parse(trimmed).atZoneSameInstant(zone).toLocalDate() }
        .recoverCatching { Instant.parse(trimmed).atZone(zone).toLocalDate() }
        .recoverCatching { LocalDate.parse(trimmed.substringBefore("T")) }
        .getOrNull()
        ?: return trimmed.substringBefore("T").ifBlank { "-" }
    return "${date.dayOfMonth} ${indonesianShortMonth(date.monthValue)} ${date.year}"
}

/**
 * Mengakumulasi halaman item terpaginasi pada route daftar master catalog Owner (Satuan / Kategori)
 * sehingga mendukung infinite scroll tanpa mengubah ViewModel/Repository.
 */
@Composable
internal fun <T> rememberAccumulatedCatalogItems(
    pageItems: List<T>,
    page: Int,
    isLoading: Boolean,
    hasError: Boolean,
    searchQuery: String,
    keyOf: (T) -> String
): List<T> {
    var accumulated by remember { mutableStateOf<List<T>>(emptyList()) }
    var lastLoadedPage by remember { mutableIntStateOf(0) }
    var lastSearchQuery by remember { mutableStateOf(searchQuery) }

    LaunchedEffect(pageItems, page, isLoading, hasError, searchQuery) {
        if (searchQuery != lastSearchQuery) {
            lastSearchQuery = searchQuery
            lastLoadedPage = 0
        }
        if (!isLoading && !hasError) {
            if (page <= 1) {
                accumulated = pageItems
                lastLoadedPage = 1
            } else if (page > lastLoadedPage) {
                accumulated = (accumulated + pageItems).distinctBy(keyOf)
                lastLoadedPage = page
            } else if (page == lastLoadedPage) {
                val updatedById = pageItems.associateBy(keyOf)
                accumulated = accumulated.map { existing ->
                    updatedById[keyOf(existing)] ?: existing
                }
            }
        }
    }
    return accumulated
}

@Composable
internal fun rememberCatalogBackHandler(
    onBack: (() -> Unit)?,
    onStockClick: () -> Unit,
    onDashboardClick: () -> Unit,
    activeNavDestination: DashboardNavDestination
): Pair<() -> Unit, (DashboardNavDestination) -> Unit> {
    val destinationNavigator = LocalAdminDestinationNavigator.current
    val pageBack = LocalBackofficePageNavigation.current.onBack
    val navigateBack: () -> Unit = pageBack ?: onBack ?: {
        destinationNavigator?.invoke(AdminDestination.StockHub) ?: onStockClick()
    }

    val handleNavSelect: (DashboardNavDestination) -> Unit = { destination ->
        when (destination) {
            DashboardNavDestination.BERANDA ->
                destinationNavigator?.invoke(AdminDestination.Dashboard) ?: onDashboardClick()
            DashboardNavDestination.TRANSAKSI ->
                destinationNavigator?.invoke(AdminDestination.TransactionsHub)
            DashboardNavDestination.KEUANGAN ->
                destinationNavigator?.invoke(AdminDestination.FinanceHub)
            DashboardNavDestination.STOK ->
                if (activeNavDestination == DashboardNavDestination.STOK) navigateBack()
                else destinationNavigator?.invoke(AdminDestination.StockHub) ?: onStockClick()
            DashboardNavDestination.MENU ->
                if (activeNavDestination == DashboardNavDestination.MENU) navigateBack()
                else destinationNavigator?.invoke(AdminDestination.MoreHub)
        }
    }
    return navigateBack to handleNavSelect
}

@Composable
internal fun CatalogHeaderBar(
    title: String,
    isSearchExpanded: Boolean,
    searchQuery: String,
    searchPlaceholder: String,
    searchContentDescription: String,
    backTestTag: String,
    searchToggleTestTag: String,
    searchBackTestTag: String,
    searchInputTestTag: String,
    searchClearTestTag: String,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    autoFocusSearch: Boolean
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
            isFiltered = false,
            onBack = onBack,
            onSearchClick = onOpenSearch,
            onFilterClick = {},
            backTestTag = backTestTag,
            searchTestTag = searchToggleTestTag,
            searchContentDescription = searchContentDescription,
            showFilterAction = false
        )
    }
}

@Composable
internal fun CatalogSummaryStats(
    totalLabel: String,
    totalCount: Long,
    unitLabel: String,
    countTestTag: String,
    breakdownMetrics: List<CatalogBreakdownMetric> = emptyList(),
    modifier: Modifier = Modifier,
    isTablet: Boolean = false
) {
    val lineColor = Color.White.copy(alpha = 0.18f)
    val countFontSize = if (isTablet) 48.sp else 40.sp
    val countLineHeight = if (isTablet) 54.sp else 46.sp
    val countBoxHeight = if (isTablet) 54.dp else 46.dp

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
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.cssLineBoxHeight(20.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = totalCount.toString(),
                style = ownerTextStyle(
                    fontSize = countFontSize,
                    lineHeight = countLineHeight,
                    fontWeight = FontWeight.Bold,
                    color = OnBrand,
                    letterSpacing = (-0.03).em,
                    tabularNumbers = true
                ),
                maxLines = 1,
                modifier = Modifier
                    .cssLineBoxHeight(countBoxHeight)
                    .alignByBaseline()
                    .testTag(countTestTag)
            )

            Text(
                text = unitLabel,
                style = ownerTextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = BrandSoftText
                ),
                maxLines = 1,
                modifier = Modifier
                    .cssLineBoxHeight(20.dp)
                    .alignByBaseline()
            )
        }

        if (breakdownMetrics.size == 2) {
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
                PelangganStatColumn(
                    label = breakdownMetrics[0].label,
                    value = breakdownMetrics[0].value,
                    modifier = Modifier.weight(1f)
                )
                PelangganStatColumn(
                    label = breakdownMetrics[1].label,
                    value = breakdownMetrics[1].value,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .ownerHeroLeftDivider(lineColor)
                )
            }
        }
    }
}

@Composable
internal fun CatalogPhoneGreenHeader(
    headerBar: @Composable () -> Unit,
    summaryStats: @Composable () -> Unit
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
            headerBar()

            Spacer(modifier = Modifier.height(16.dp))

            summaryStats()
        }
    }
}

@Composable
internal fun CatalogTabletGreenPanel(
    headerBar: @Composable () -> Unit,
    summaryStats: @Composable () -> Unit
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
            headerBar()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                summaryStats()
            }
        }
    }
}

@Composable
internal fun CatalogListHeaderSection(
    addButtonLabel: String,
    addButtonTestTag: String,
    sectionTitle: String,
    sortLabel: String,
    sortTestTag: String,
    onAddClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        AddPelangganPrimaryButton(
            onClick = onAddClick,
            testTag = addButtonTestTag,
            label = addButtonLabel
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sectionTitle,
                style = ownerTextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                ),
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
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .cssLineBoxHeight(18.dp)
                    .testTag(sortTestTag)
            )
        }
    }
}

@Composable
internal fun CatalogIconCircleBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(SurfaceWhite)
            .border(1.dp, OwnerBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        OwnerVectorIcon(
            imageVector = icon,
            tint = Brand,
            size = 20.dp
        )
    }
}

@Composable
internal fun CatalogListRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(vertical = 16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        leading()

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = ownerTextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Ink
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.cssLineBoxHeight(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = ownerTextStyle(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = Muted,
                    tabularNumbers = true
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.cssLineBoxHeight(18.dp)
            )
        }

        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            OwnerVectorIcon(
                imageVector = OwnerIcons.ChevronRight,
                tint = Muted,
                size = 20.dp
            )
        }
    }
}

@Composable
internal fun CatalogListDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 52.dp)
            .height(1.dp)
            .background(OwnerDivider)
    )
}

@Composable
internal fun <T> OwnerCatalogDataScreen(
    items: List<T>,
    itemKey: (T) -> String,
    footerStatus: PelangganFooterStatus,
    hasMorePages: Boolean,
    listTestTag: String,
    footerLoadingText: String,
    footerEndText: String,
    footerTestTag: String,
    footerRetryTestTag: String,
    headerBar: @Composable () -> Unit,
    summaryStats: @Composable (isTablet: Boolean) -> Unit,
    listHeaderSection: @Composable () -> Unit,
    rowContent: @Composable (T) -> Unit,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    activeNavDestination: DashboardNavDestination = DashboardNavDestination.STOK
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState, items.size, hasMorePages, footerStatus) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisible >= (totalItems - 3).coerceAtLeast(0) && totalItems > 1
        }.collect { nearBottom ->
            if (nearBottom && hasMorePages && footerStatus == PelangganFooterStatus.NONE) {
                onLoadMore()
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        val isTablet = maxWidth >= OWNER_CATALOG_TABLET_BREAKPOINT
        if (isTablet) {
            TabletCatalogDataLayout(
                items = items,
                itemKey = itemKey,
                footerStatus = footerStatus,
                listState = listState,
                listTestTag = listTestTag,
                footerLoadingText = footerLoadingText,
                footerEndText = footerEndText,
                footerTestTag = footerTestTag,
                footerRetryTestTag = footerRetryTestTag,
                headerBar = headerBar,
                summaryStats = { summaryStats(true) },
                listHeaderSection = listHeaderSection,
                rowContent = rowContent,
                onRetryLoadMore = onRetryLoadMore,
                onNavSelect = onNavSelect,
                activeNavDestination = activeNavDestination
            )
        } else {
            PhoneCatalogDataLayout(
                items = items,
                itemKey = itemKey,
                footerStatus = footerStatus,
                listState = listState,
                listTestTag = listTestTag,
                footerLoadingText = footerLoadingText,
                footerEndText = footerEndText,
                footerTestTag = footerTestTag,
                footerRetryTestTag = footerRetryTestTag,
                headerBar = headerBar,
                summaryStats = { summaryStats(false) },
                listHeaderSection = listHeaderSection,
                rowContent = rowContent,
                onRetryLoadMore = onRetryLoadMore
            )
        }
    }
}

@Composable
private fun <T> PhoneCatalogDataLayout(
    items: List<T>,
    itemKey: (T) -> String,
    footerStatus: PelangganFooterStatus,
    listState: LazyListState,
    listTestTag: String,
    footerLoadingText: String,
    footerEndText: String,
    footerTestTag: String,
    footerRetryTestTag: String,
    headerBar: @Composable () -> Unit,
    summaryStats: @Composable () -> Unit,
    listHeaderSection: @Composable () -> Unit,
    rowContent: @Composable (T) -> Unit,
    onRetryLoadMore: () -> Unit
) {
    val overlapPx = with(LocalDensity.current) { 28.dp.roundToPx() }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .testTag(listTestTag)
    ) {
        item(key = "catalog-header-cap") {
            Layout(
                content = {
                    CatalogPhoneGreenHeader(
                        headerBar = headerBar,
                        summaryStats = summaryStats
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                            .background(SurfaceWhite)
                            .padding(start = 20.dp, top = 24.dp, end = 20.dp)
                    ) {
                        listHeaderSection()
                    }
                }
            ) { measurables, constraints ->
                val headerPlaceable = measurables[0].measure(
                    constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                )
                val capPlaceable = measurables[1].measure(
                    constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                )
                val totalHeight = (headerPlaceable.height - overlapPx + capPlaceable.height)
                    .coerceAtLeast(headerPlaceable.height)
                layout(constraints.maxWidth, totalHeight) {
                    headerPlaceable.placeRelative(0, 0)
                    capPlaceable.placeRelative(0, headerPlaceable.height - overlapPx)
                }
            }
        }

        itemsIndexed(
            items = items,
            key = { _, item -> itemKey(item) }
        ) { index, item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite)
                    .padding(horizontal = 20.dp)
            ) {
                rowContent(item)
                if (index < items.lastIndex) {
                    CatalogListDivider()
                }
            }
        }

        item(key = "catalog-footer") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite)
                    .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 32.dp)
            ) {
                ListFooterState(
                    status = footerStatus,
                    onRetry = onRetryLoadMore,
                    loadingText = footerLoadingText,
                    endText = footerEndText,
                    footerTestTag = footerTestTag,
                    retryTestTag = footerRetryTestTag
                )
            }
        }
    }
}

@Composable
private fun <T> TabletCatalogDataLayout(
    items: List<T>,
    itemKey: (T) -> String,
    footerStatus: PelangganFooterStatus,
    listState: LazyListState,
    listTestTag: String,
    footerLoadingText: String,
    footerEndText: String,
    footerTestTag: String,
    footerRetryTestTag: String,
    headerBar: @Composable () -> Unit,
    summaryStats: @Composable () -> Unit,
    listHeaderSection: @Composable () -> Unit,
    rowContent: @Composable (T) -> Unit,
    onRetryLoadMore: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    activeNavDestination: DashboardNavDestination
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        OwnerTabletNavRail(
            activeDestination = activeNavDestination,
            onNavSelect = onNavSelect
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            CatalogTabletGreenPanel(
                headerBar = headerBar,
                summaryStats = summaryStats
            )

            Box(
                modifier = Modifier
                    .padding(start = 400.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp))
                    .background(SurfaceWhite)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(listTestTag),
                    contentPadding = PaddingValues(start = 64.dp, top = 32.dp, end = 32.dp, bottom = 32.dp)
                ) {
                    item(key = "catalog-top-controls") {
                        listHeaderSection()
                    }

                    itemsIndexed(
                        items = items,
                        key = { _, item -> itemKey(item) }
                    ) { index, item ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            rowContent(item)
                            if (index < items.lastIndex) {
                                CatalogListDivider()
                            }
                        }
                    }

                    item(key = "catalog-footer") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            ListFooterState(
                                status = footerStatus,
                                onRetry = onRetryLoadMore,
                                loadingText = footerLoadingText,
                                endText = footerEndText,
                                footerTestTag = footerTestTag,
                                retryTestTag = footerRetryTestTag
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun OwnerCatalogEmptyScreen(
    headerBar: @Composable () -> Unit,
    summaryStats: @Composable (isTablet: Boolean) -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    centerPanelContent: Boolean,
    panelContent: @Composable (isTablet: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    activeNavDestination: DashboardNavDestination = DashboardNavDestination.STOK
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        val isTablet = maxWidth >= OWNER_CATALOG_TABLET_BREAKPOINT
        if (isTablet) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SurfaceWhite)
            ) {
                OwnerTabletNavRail(
                    activeDestination = activeNavDestination,
                    onNavSelect = onNavSelect
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    CatalogTabletGreenPanel(
                        headerBar = headerBar,
                        summaryStats = { summaryStats(true) }
                    )

                    BoxWithConstraints(
                        modifier = Modifier
                            .padding(start = 400.dp)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp))
                            .background(SurfaceWhite)
                    ) {
                        val panelViewportHeight = maxHeight
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = panelViewportHeight)
                                    .padding(start = 64.dp, top = 40.dp, end = 32.dp, bottom = 40.dp),
                                contentAlignment = if (centerPanelContent) Alignment.Center else Alignment.TopStart
                            ) {
                                panelContent(true)
                            }
                        }
                    }
                }
            }
        } else {
            val viewportHeightPx = with(LocalDensity.current) { maxHeight.roundToPx() }
            val overlapPx = with(LocalDensity.current) { 28.dp.roundToPx() }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Layout(
                    content = {
                        CatalogPhoneGreenHeader(
                            headerBar = headerBar,
                            summaryStats = { summaryStats(false) }
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                                .background(SurfaceWhite)
                                .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 32.dp),
                            contentAlignment = if (centerPanelContent) Alignment.Center else Alignment.TopStart
                        ) {
                            panelContent(false)
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
}

@Composable
internal fun CatalogLoadingSkeleton(
    compact: Boolean,
    testTag: String
) {
    val count = if (compact) 4 else 5
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonBox(modifier = Modifier.width(120.dp).height(18.dp), shape = RoundedCornerShape(6.dp))
            SkeletonBox(modifier = Modifier.width(96.dp).height(14.dp), shape = RoundedCornerShape(6.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        repeat(count) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SkeletonBox(modifier = Modifier.size(40.dp), shape = CircleShape)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SkeletonBox(modifier = Modifier.width(140.dp).height(16.dp), shape = RoundedCornerShape(6.dp))
                    SkeletonBox(modifier = Modifier.width(120.dp).height(12.dp), shape = RoundedCornerShape(6.dp))
                }
                SkeletonBox(modifier = Modifier.size(20.dp), shape = RoundedCornerShape(6.dp))
            }
            if (index < count - 1) {
                CatalogListDivider()
            }
        }
    }
}

@Composable
internal fun CatalogErrorPanel(
    message: String,
    retryTestTag: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = message,
            style = ownerTextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Muted
            ),
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Brand)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onRetry
                )
                .padding(horizontal = 20.dp)
                .testTag(retryTestTag),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Muat ulang",
                style = ownerTextStyle(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnBrand
                )
            )
        }
    }
}

@Composable
internal fun CatalogDeleteDangerButton(
    label: String,
    busyLabel: String,
    isBusy: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceWhite)
            .border(1.dp, CatalogDangerBorderColor, RoundedCornerShape(16.dp))
            .clickable(
                enabled = !isBusy,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .testTag(testTag),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OwnerVectorIcon(
            imageVector = OwnerIcons.Trash,
            tint = CatalogDangerColor,
            size = 20.dp
        )
        Text(
            text = if (isBusy) busyLabel else label,
            style = ownerTextStyle(
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = CatalogDangerColor
            ),
            maxLines = 1,
            modifier = Modifier.cssLineBoxHeight(20.dp)
        )
    }
}

@Composable
internal fun CatalogDeleteConfirmDialog(
    title: String,
    message: String,
    isBusy: Boolean,
    dialogTestTag: String,
    cancelTestTag: String,
    confirmTestTag: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = { if (!isBusy) onDismiss() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceWhite)
                .padding(24.dp)
                .testTag(dialogTestTag)
        ) {
            Text(
                text = title,
                style = ownerTextStyle(
                    fontSize = 18.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = ownerTextStyle(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Normal,
                    color = Muted
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            enabled = !isBusy,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = onDismiss
                        )
                        .padding(horizontal = 16.dp)
                        .testTag(cancelTestTag),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Batal",
                        style = ownerTextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Brand
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CatalogDangerColor)
                        .clickable(
                            enabled = !isBusy,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            onClick = onConfirm
                        )
                        .padding(horizontal = 20.dp)
                        .testTag(confirmTestTag),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isBusy) "Menghapus..." else "Hapus",
                        style = ownerTextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = OnBrand
                        )
                    )
                }
            }
        }
    }
}

@Composable
internal fun OwnerCatalogFormScaffold(
    pageTitle: String,
    buttonLabel: String,
    heroIcon: ImageVector,
    heroTitle: String,
    heroSubtitle: String,
    heroChecks: List<String>,
    isSaveEnabled: Boolean,
    isSaving: Boolean,
    backTestTag: String,
    saveButtonTestTag: String,
    loadingTestTag: String,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    activeNavDestination: DashboardNavDestination = DashboardNavDestination.STOK,
    formContent: @Composable (isTablet: Boolean) -> Unit
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        val isTablet = maxWidth >= OWNER_CATALOG_TABLET_BREAKPOINT
        if (isTablet) {
            TabletCatalogFormLayout(
                pageTitle = pageTitle,
                buttonLabel = buttonLabel,
                heroIcon = heroIcon,
                heroTitle = heroTitle,
                heroSubtitle = heroSubtitle,
                heroChecks = heroChecks,
                isSaveEnabled = isSaveEnabled,
                isSaving = isSaving,
                backTestTag = backTestTag,
                saveButtonTestTag = saveButtonTestTag,
                loadingTestTag = loadingTestTag,
                onBack = onBack,
                onSubmit = onSubmit,
                onNavSelect = onNavSelect,
                activeNavDestination = activeNavDestination,
                formContent = { formContent(true) }
            )
        } else {
            PhoneCatalogFormLayout(
                pageTitle = pageTitle,
                buttonLabel = buttonLabel,
                isSaveEnabled = isSaveEnabled,
                isSaving = isSaving,
                backTestTag = backTestTag,
                saveButtonTestTag = saveButtonTestTag,
                loadingTestTag = loadingTestTag,
                onBack = onBack,
                onSubmit = onSubmit,
                formContent = { formContent(false) }
            )
        }
    }
}

@Composable
private fun PhoneCatalogFormLayout(
    pageTitle: String,
    buttonLabel: String,
    isSaveEnabled: Boolean,
    isSaving: Boolean,
    backTestTag: String,
    saveButtonTestTag: String,
    loadingTestTag: String,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    formContent: @Composable () -> Unit
) {
    val overlapPx = with(LocalDensity.current) { 28.dp.roundToPx() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .imePadding()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val scrollViewportHeightPx = with(LocalDensity.current) { maxHeight.roundToPx() }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Layout(
                    content = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
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

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 20.dp, top = 56.dp, end = 20.dp)
                            ) {
                                TambahPelangganTopBar(
                                    title = pageTitle,
                                    onBack = onBack,
                                    backTestTag = backTestTag
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                                .background(SurfaceWhite)
                                .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 24.dp)
                        ) {
                            formContent()
                        }
                    }
                ) { measurables, constraints ->
                    val headerPlaceable = measurables[0].measure(
                        constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                    )
                    val minPanelHeight = (scrollViewportHeightPx - (headerPlaceable.height - overlapPx))
                        .coerceAtLeast(0)
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

        StickyActionBar(
            enabled = isSaveEnabled,
            isSaving = isSaving,
            onSaveClick = onSubmit,
            buttonLabel = buttonLabel,
            buttonTestTag = saveButtonTestTag,
            loadingTestTag = loadingTestTag
        )
    }
}

@Composable
private fun TabletCatalogFormLayout(
    pageTitle: String,
    buttonLabel: String,
    heroIcon: ImageVector,
    heroTitle: String,
    heroSubtitle: String,
    heroChecks: List<String>,
    isSaveEnabled: Boolean,
    isSaving: Boolean,
    backTestTag: String,
    saveButtonTestTag: String,
    loadingTestTag: String,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    activeNavDestination: DashboardNavDestination,
    formContent: @Composable () -> Unit
) {
    val density = LocalDensity.current

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .imePadding()
    ) {
        OwnerTabletNavRail(
            activeDestination = activeNavDestination,
            onNavSelect = onNavSelect
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(400.dp)
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
                    TambahPelangganTopBar(
                        title = pageTitle,
                        onBack = onBack,
                        backTestTag = backTestTag
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            OwnerVectorIcon(
                                imageVector = heroIcon,
                                tint = OnBrand,
                                size = 36.dp,
                                modifier = Modifier.offset(x = (-2).dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = heroTitle,
                                style = ownerTextStyle(
                                    fontSize = 24.sp,
                                    lineHeight = 30.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnBrand,
                                    letterSpacing = (-0.02).em
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = heroSubtitle,
                                style = ownerTextStyle(
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = BrandSoftText
                                )
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.White.copy(alpha = 0.18f))
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                heroChecks.forEach { check ->
                                    CatalogTabletHighlightCheckRow(check)
                                }
                            }
                        }
                    }
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .padding(start = 368.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp))
                    .background(SurfaceWhite)
            ) {
                val panelViewportHeightPx = with(density) { maxHeight.roundToPx() }
                val verticalPaddingPx = with(density) { 64.dp.roundToPx() }
                val minButtonGapPx = with(density) { 20.dp.roundToPx() }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 64.dp, top = 32.dp, end = 40.dp, bottom = 32.dp)
                ) {
                    Layout(
                        content = {
                            formContent()

                            SaveCustomerButton(
                                enabled = isSaveEnabled,
                                isSaving = isSaving,
                                onClick = onSubmit,
                                label = buttonLabel,
                                buttonTestTag = saveButtonTestTag,
                                loadingTestTag = loadingTestTag
                            )
                        }
                    ) { measurables, constraints ->
                        val formPlaceable = measurables[0].measure(
                            constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                        )
                        val buttonPlaceable = measurables[1].measure(
                            constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity)
                        )
                        val innerAvailable = (panelViewportHeightPx - verticalPaddingPx).coerceAtLeast(0)
                        val naturalTotal = formPlaceable.height + minButtonGapPx + buttonPlaceable.height
                        val totalHeight = maxOf(innerAvailable, naturalTotal)
                        val buttonY = totalHeight - buttonPlaceable.height

                        layout(constraints.maxWidth, totalHeight) {
                            formPlaceable.placeRelative(0, 0)
                            buttonPlaceable.placeRelative(0, buttonY)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogTabletHighlightCheckRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OwnerVectorIcon(
            imageVector = OwnerIcons.CheckPlain,
            tint = CatalogTabletCheckColor,
            size = 18.dp
        )
        Text(
            text = text,
            style = ownerTextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                color = OnBrand
            )
        )
    }
}
