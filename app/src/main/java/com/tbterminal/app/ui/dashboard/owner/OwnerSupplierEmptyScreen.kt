package com.tbterminal.app.ui.dashboard.owner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.tbterminal.app.ui.auth.DotsPattern
import com.tbterminal.app.ui.auth.RingsPattern
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.components.TopFloatingCapsule
import com.tbterminal.app.ui.dashboard.LocalBackofficePageNavigation
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator
import com.tbterminal.app.ui.suppliers.SupplierStatusFilter
import com.tbterminal.app.ui.suppliers.SupplierUiState
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.BrandSoftText
import com.tbterminal.app.ui.theme.ChipBg
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.SurfaceWhite
import com.tbterminal.app.ui.theme.TbterminalappTheme

private val OWNER_SUPPLIER_TABLET_BREAKPOINT = 840.dp

@Immutable
internal data class SupplierEmptySummaryUiState(
    val totalSuppliers: Long = 0L,
    val activeCount: Int = 0,
    val inactiveCount: Int = 0
)

@Immutable
internal data class SupplierEmptySpec(
    val icon: ImageVector,
    val iconContainerColor: Color,
    val iconTint: Color,
    val title: String,
    val description: String
)

internal fun SupplierUiState.toSupplierEmptySummary(): SupplierEmptySummaryUiState {
    return SupplierEmptySummaryUiState(
        totalSuppliers = totalSuppliers,
        activeCount = suppliers.count { it.isActive },
        inactiveCount = suppliers.count { !it.isActive }
    )
}

internal fun supplierEmptyStateSpecFor(
    searchQuery: String,
    isFiltered: Boolean
): SupplierEmptySpec {
    val trimmedQuery = searchQuery.trim()
    return when {
        trimmedQuery.isNotEmpty() -> SupplierEmptySpec(
            icon = OwnerIcons.Search,
            iconContainerColor = ChipBg,
            iconTint = Muted,
            title = "\"$trimmedQuery\" tidak ditemukan",
            description = "Periksa ejaan atau cari dengan nomor HP."
        )
        isFiltered -> SupplierEmptySpec(
            icon = OwnerIcons.Search,
            iconContainerColor = ChipBg,
            iconTint = Muted,
            title = "Supplier tidak ditemukan",
            description = "Ubah pilihan filter atau tambahkan supplier baru."
        )
        else -> SupplierEmptySpec(
            icon = OwnerIcons.Truck,
            iconContainerColor = SurfaceWhite,
            iconTint = Brand,
            title = "Belum ada supplier",
            description = "Tambahkan supplier untuk pencatatan transaksi pembelian, termin bayar, dan restok barang."
        )
    }
}

internal fun isSupplierErrorMessage(message: String?): Boolean {
    return message?.let {
        it.contains("gagal", ignoreCase = true) ||
            it.contains("tidak", ignoreCase = true) ||
            it.contains("kosong", ignoreCase = true)
    } == true
}

@Composable
internal fun OwnerSupplierEmptyRoute(
    uiState: SupplierUiState,
    canManage: Boolean,
    isSearchExpanded: Boolean,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onAddSupplierClick: () -> Unit,
    onRefresh: () -> Unit,
    onBack: (() -> Unit)?,
    onDashboardClick: () -> Unit,
    onDismissMessage: () -> Unit,
    loadingContent: @Composable (isTablet: Boolean) -> Unit
) {
    val destinationNavigator = LocalAdminDestinationNavigator.current
    val pageBack = LocalBackofficePageNavigation.current.onBack
    val navigateBack: () -> Unit = onBack ?: pageBack ?: {
        destinationNavigator?.invoke(AdminDestination.MoreHub) ?: onDashboardClick()
    }

    BackHandler(enabled = isSearchExpanded) {
        onCloseSearch()
    }
    BackHandler(enabled = !isSearchExpanded) {
        navigateBack()
    }

    fun handleNavSelect(destination: DashboardNavDestination) {
        when (destination) {
            DashboardNavDestination.BERANDA ->
                destinationNavigator?.invoke(AdminDestination.Dashboard) ?: onDashboardClick()
            DashboardNavDestination.TRANSAKSI ->
                destinationNavigator?.invoke(AdminDestination.TransactionsHub)
            DashboardNavDestination.KEUANGAN ->
                destinationNavigator?.invoke(AdminDestination.FinanceHub)
            DashboardNavDestination.STOK ->
                destinationNavigator?.invoke(AdminDestination.StockHub)
            DashboardNavDestination.MENU -> navigateBack()
        }
    }

    val isFiltered = uiState.statusFilter != SupplierStatusFilter.ALL
    val summary = uiState.toSupplierEmptySummary()
    val hasError = isSupplierErrorMessage(uiState.message)
    val isInitialLoading = uiState.isLoading && uiState.suppliers.isEmpty()
    val isInitialError = hasError && uiState.suppliers.isEmpty() && !uiState.isLoading
    val emptySpec = supplierEmptyStateSpecFor(
        searchQuery = uiState.searchQuery,
        isFiltered = isFiltered
    )

    Box(modifier = Modifier.fillMaxSize()) {
        RefreshableContent(
            isRefreshing = uiState.isLoading && uiState.suppliers.isNotEmpty(),
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            OwnerSupplierEmptyScreen(
                summary = summary,
                isSearchExpanded = isSearchExpanded,
                searchQuery = uiState.searchQuery,
                isFiltered = isFiltered,
                onBack = navigateBack,
                onOpenSearch = onOpenSearch,
                onCloseSearch = onCloseSearch,
                onSearchChanged = onSearchChanged,
                onOpenFilter = onOpenFilter,
                onNavSelect = ::handleNavSelect,
                panelContent = { isTablet ->
                    when {
                        isInitialLoading -> loadingContent(isTablet)
                        isInitialError -> SupplierEmptyCenteredError(
                            message = uiState.message ?: "Supplier gagal dimuat.",
                            onRetry = onRefresh
                        )
                        else -> EmptyState(
                            icon = emptySpec.icon,
                            iconContainerColor = emptySpec.iconContainerColor,
                            iconTint = emptySpec.iconTint,
                            title = emptySpec.title,
                            description = emptySpec.description,
                            showAddButton = canManage,
                            onAddCustomerClick = onAddSupplierClick,
                            buttonTestTag = "supplier-add",
                            buttonLabel = "Tambah supplier",
                            modifier = if (isTablet) Modifier.widthIn(max = 360.dp) else Modifier
                        )
                    }
                }
            )
        }

        TopFloatingCapsule(
            message = if (!hasError) uiState.message else null,
            error = if (hasError && !isInitialError) uiState.message else null,
            onDismiss = onDismissMessage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
        )
    }
}

@Composable
internal fun OwnerSupplierEmptyScreen(
    summary: SupplierEmptySummaryUiState,
    isSearchExpanded: Boolean,
    searchQuery: String,
    isFiltered: Boolean,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    panelContent: @Composable (isTablet: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    autoFocusSearch: Boolean = true
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        val isTablet = maxWidth >= OWNER_SUPPLIER_TABLET_BREAKPOINT
        if (isTablet) {
            TabletSupplierEmptyLayout(
                summary = summary,
                isSearchExpanded = isSearchExpanded,
                searchQuery = searchQuery,
                isFiltered = isFiltered,
                onBack = onBack,
                onOpenSearch = onOpenSearch,
                onCloseSearch = onCloseSearch,
                onSearchChanged = onSearchChanged,
                onOpenFilter = onOpenFilter,
                onNavSelect = onNavSelect,
                autoFocusSearch = autoFocusSearch,
                panelContent = { panelContent(true) }
            )
        } else {
            PhoneSupplierEmptyLayout(
                summary = summary,
                isSearchExpanded = isSearchExpanded,
                searchQuery = searchQuery,
                isFiltered = isFiltered,
                onBack = onBack,
                onOpenSearch = onOpenSearch,
                onCloseSearch = onCloseSearch,
                onSearchChanged = onSearchChanged,
                onOpenFilter = onOpenFilter,
                autoFocusSearch = autoFocusSearch,
                panelContent = { panelContent(false) }
            )
        }
    }
}

@Composable
internal fun SupplierPhoneGreenHeader(
    summary: SupplierEmptySummaryUiState,
    isSearchExpanded: Boolean,
    searchQuery: String,
    isFiltered: Boolean,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    autoFocusSearch: Boolean
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
                    placeholder = "Cari nama atau nomor HP...",
                    backTestTag = "supplier-search-back",
                    inputTestTag = "supplier-search-input",
                    clearTestTag = "supplier-search-clear"
                )
            } else {
                SecondaryHeader(
                    title = "Supplier",
                    isFiltered = isFiltered,
                    onBack = onBack,
                    onSearchClick = onOpenSearch,
                    onFilterClick = onOpenFilter,
                    backTestTag = "supplier-back-btn",
                    searchTestTag = "supplier-toggle-search",
                    filterTestTag = "supplier-open-filter",
                    searchContentDescription = "Cari supplier",
                    filterContentDescription = "Filter supplier"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SupplierSummaryStats(summary = summary)
        }
    }
}

@Composable
internal fun SupplierTabletGreenPanel(
    summary: SupplierEmptySummaryUiState,
    isSearchExpanded: Boolean,
    searchQuery: String,
    isFiltered: Boolean,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    autoFocusSearch: Boolean
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
                    placeholder = "Cari nama atau nomor HP...",
                    backTestTag = "supplier-search-back",
                    inputTestTag = "supplier-search-input",
                    clearTestTag = "supplier-search-clear"
                )
            } else {
                SecondaryHeader(
                    title = "Supplier",
                    isFiltered = isFiltered,
                    onBack = onBack,
                    onSearchClick = onOpenSearch,
                    onFilterClick = onOpenFilter,
                    backTestTag = "supplier-back-btn",
                    searchTestTag = "supplier-toggle-search",
                    filterTestTag = "supplier-open-filter",
                    searchContentDescription = "Cari supplier",
                    filterContentDescription = "Filter supplier"
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                SupplierSummaryStats(summary = summary, isTablet = true)
            }
        }
    }
}

@Composable
internal fun SupplierSummaryStats(
    summary: SupplierEmptySummaryUiState,
    modifier: Modifier = Modifier,
    isTablet: Boolean = false
) {
    val lineColor = Color.White.copy(alpha = 0.18f)
    val countFontSize = if (isTablet) 48.sp else 40.sp
    val countLineHeight = if (isTablet) 54.sp else 46.sp
    val countBoxHeight = if (isTablet) 54.dp else 46.dp

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Total supplier",
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
                text = summary.totalSuppliers.toString(),
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
                    .testTag("supplier-total-count")
            )

            Text(
                text = "supplier",
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
                label = "Aktif",
                value = "${summary.activeCount} supplier",
                modifier = Modifier.weight(1f)
            )
            PelangganStatColumn(
                label = "Nonaktif",
                value = "${summary.inactiveCount} supplier",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .ownerHeroLeftDivider(lineColor)
            )
        }
    }
}

@Composable
private fun PhoneSupplierEmptyLayout(
    summary: SupplierEmptySummaryUiState,
    isSearchExpanded: Boolean,
    searchQuery: String,
    isFiltered: Boolean,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    autoFocusSearch: Boolean,
    panelContent: @Composable () -> Unit
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
                    SupplierPhoneGreenHeader(
                        summary = summary,
                        isSearchExpanded = isSearchExpanded,
                        searchQuery = searchQuery,
                        isFiltered = isFiltered,
                        onBack = onBack,
                        onOpenSearch = onOpenSearch,
                        onCloseSearch = onCloseSearch,
                        onSearchChanged = onSearchChanged,
                        onOpenFilter = onOpenFilter,
                        autoFocusSearch = autoFocusSearch
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                            .background(SurfaceWhite)
                            .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        panelContent()
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
private fun TabletSupplierEmptyLayout(
    summary: SupplierEmptySummaryUiState,
    isSearchExpanded: Boolean,
    searchQuery: String,
    isFiltered: Boolean,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    autoFocusSearch: Boolean,
    panelContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        OwnerTabletNavRail(
            activeDestination = DashboardNavDestination.MENU,
            onNavSelect = onNavSelect
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            SupplierTabletGreenPanel(
                summary = summary,
                isSearchExpanded = isSearchExpanded,
                searchQuery = searchQuery,
                isFiltered = isFiltered,
                onBack = onBack,
                onOpenSearch = onOpenSearch,
                onCloseSearch = onCloseSearch,
                onSearchChanged = onSearchChanged,
                onOpenFilter = onOpenFilter,
                autoFocusSearch = autoFocusSearch
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
                        contentAlignment = Alignment.Center
                    ) {
                        panelContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun SupplierEmptyCenteredError(
    message: String,
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
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Brand, contentColor = OnBrand)
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

@Preview(
    name = "OwnerSupplierEmptyPhone",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
internal fun OwnerSupplierEmptyPhonePreview() {
    TbterminalappTheme {
        val spec = supplierEmptyStateSpecFor(searchQuery = "", isFiltered = false)
        OwnerSupplierEmptyScreen(
            summary = SupplierEmptySummaryUiState(),
            isSearchExpanded = false,
            searchQuery = "",
            isFiltered = false,
            onBack = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChanged = {},
            onOpenFilter = {},
            onNavSelect = {},
            panelContent = { isTablet ->
                EmptyState(
                    icon = spec.icon,
                    iconContainerColor = spec.iconContainerColor,
                    iconTint = spec.iconTint,
                    title = spec.title,
                    description = spec.description,
                    showAddButton = true,
                    onAddCustomerClick = {},
                    buttonTestTag = "supplier-add",
                    buttonLabel = "Tambah supplier",
                    modifier = if (isTablet) Modifier.widthIn(max = 360.dp) else Modifier
                )
            }
        )
    }
}

@Preview(
    name = "OwnerSupplierEmptyTablet",
    showBackground = true,
    widthDp = 1024,
    heightDp = 768
)
@Composable
internal fun OwnerSupplierEmptyTabletPreview() {
    TbterminalappTheme {
        val spec = supplierEmptyStateSpecFor(searchQuery = "", isFiltered = false)
        OwnerSupplierEmptyScreen(
            summary = SupplierEmptySummaryUiState(),
            isSearchExpanded = false,
            searchQuery = "",
            isFiltered = false,
            onBack = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChanged = {},
            onOpenFilter = {},
            onNavSelect = {},
            panelContent = { isTablet ->
                EmptyState(
                    icon = spec.icon,
                    iconContainerColor = spec.iconContainerColor,
                    iconTint = spec.iconTint,
                    title = spec.title,
                    description = spec.description,
                    showAddButton = true,
                    onAddCustomerClick = {},
                    buttonTestTag = "supplier-add",
                    buttonLabel = "Tambah supplier",
                    modifier = if (isTablet) Modifier.widthIn(max = 360.dp) else Modifier
                )
            }
        )
    }
}

@Preview(
    name = "OwnerSupplierSearchNotFoundPhone",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
internal fun OwnerSupplierSearchNotFoundPhonePreview() {
    TbterminalappTheme {
        val query = "PT Maju"
        val spec = supplierEmptyStateSpecFor(searchQuery = query, isFiltered = false)
        OwnerSupplierEmptyScreen(
            summary = SupplierEmptySummaryUiState(),
            isSearchExpanded = true,
            searchQuery = query,
            isFiltered = false,
            onBack = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChanged = {},
            onOpenFilter = {},
            onNavSelect = {},
            autoFocusSearch = false,
            panelContent = { isTablet ->
                EmptyState(
                    icon = spec.icon,
                    iconContainerColor = spec.iconContainerColor,
                    iconTint = spec.iconTint,
                    title = spec.title,
                    description = spec.description,
                    showAddButton = true,
                    onAddCustomerClick = {},
                    buttonTestTag = "supplier-add",
                    buttonLabel = "Tambah supplier",
                    modifier = if (isTablet) Modifier.widthIn(max = 360.dp) else Modifier
                )
            }
        )
    }
}
