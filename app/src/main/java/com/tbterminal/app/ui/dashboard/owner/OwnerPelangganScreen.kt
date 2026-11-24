package com.tbterminal.app.ui.dashboard.owner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tbterminal.app.data.model.Customer
import com.tbterminal.app.ui.auth.AlertType
import com.tbterminal.app.ui.auth.AppAlert
import com.tbterminal.app.ui.auth.DotsPattern
import com.tbterminal.app.ui.auth.RingsPattern
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.components.TopFloatingCapsule
import com.tbterminal.app.ui.customers.CustomerCategoryFilter
import com.tbterminal.app.ui.customers.CustomerListUiState
import com.tbterminal.app.ui.customers.CustomerStatusFilter
import com.tbterminal.app.ui.dashboard.LocalBackofficePageNavigation
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.OwnerDivider
import com.tbterminal.app.ui.theme.SurfaceWhite
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.math.BigDecimal

private val OWNER_PELANGGAN_TABLET_BREAKPOINT = 840.dp

internal fun filterAndSortOwnerCustomers(
    customers: List<Customer>,
    searchQuery: String,
    categoryFilter: CustomerCategoryFilter,
    statusFilter: CustomerStatusFilter,
    sortOrder: CustomerSortOrder
): List<Customer> {
    val trimmedQuery = searchQuery.trim()
    val queryDigits = trimmedQuery.filter(Char::isDigit)

    val filtered = customers
        .asSequence()
        .filter { customer ->
            when (categoryFilter) {
                CustomerCategoryFilter.ALL -> true
                CustomerCategoryFilter.GENERAL -> !customer.isContractor
                CustomerCategoryFilter.CONTRACTOR -> customer.isContractor
            }
        }
        .filter { customer ->
            when (statusFilter) {
                CustomerStatusFilter.ALL -> true
                CustomerStatusFilter.ACTIVE -> customer.isActive
                CustomerStatusFilter.INACTIVE -> !customer.isActive
            }
        }
        .filter { customer ->
            if (trimmedQuery.isEmpty()) {
                true
            } else {
                val matchesName = customer.name.contains(trimmedQuery, ignoreCase = true)
                val phoneRaw = customer.phone.orEmpty()
                val matchesPhone = phoneRaw.contains(trimmedQuery, ignoreCase = true) ||
                    (queryDigits.isNotEmpty() && phoneRaw.filter(Char::isDigit).contains(queryDigits))
                matchesName || matchesPhone
            }
        }
        .toList()

    return when (sortOrder) {
        CustomerSortOrder.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
        CustomerSortOrder.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
        CustomerSortOrder.CREDIT_DESC -> filtered.sortedWith(
            compareByDescending<Customer> { it.creditLimit }.thenBy { it.name.lowercase() }
        )
    }
}

@Composable
internal fun OwnerPelangganRoute(
    uiState: CustomerListUiState,
    canManage: Boolean,
    isSearchExpanded: Boolean,
    sortOrder: CustomerSortOrder,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onAddCustomerClick: () -> Unit,
    onCustomerDetailClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadNextPage: () -> Unit,
    onRetryLoadMore: (Int) -> Unit,
    onBack: (() -> Unit)?,
    onDashboardClick: () -> Unit,
    onDismissMessage: () -> Unit,
    loadingContent: @Composable (isTablet: Boolean) -> Unit,
    errorContent: @Composable () -> Unit
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

    // Akumulasi halaman dari CustomerListUiState agar mendukung infinite scroll tanpa mengubah ViewModel/Repository
    var accumulatedCustomers by remember { mutableStateOf<List<Customer>>(emptyList()) }
    var lastLoadedPage by remember { mutableIntStateOf(0) }
    var lastLoadedStatusFilter by remember { mutableStateOf(uiState.statusFilter) }

    LaunchedEffect(uiState.customers, uiState.page, uiState.isLoading, uiState.errorMessage, uiState.statusFilter) {
        if (uiState.statusFilter != lastLoadedStatusFilter) {
            accumulatedCustomers = emptyList()
            lastLoadedPage = 0
            lastLoadedStatusFilter = uiState.statusFilter
        }
        if (!uiState.isLoading && uiState.errorMessage == null) {
            if (uiState.page <= 1) {
                accumulatedCustomers = uiState.customers
                lastLoadedPage = 1
            } else if (uiState.page > lastLoadedPage) {
                accumulatedCustomers = (accumulatedCustomers + uiState.customers).distinctBy { it.id }
                lastLoadedPage = uiState.page
            } else if (uiState.page == lastLoadedPage) {
                // Refresh halaman terakhir yang termuat
                val updatedIds = uiState.customers.associateBy { it.id }
                accumulatedCustomers = accumulatedCustomers.map { existing ->
                    updatedIds[existing.id] ?: existing
                }
            }
        }
    }

    val isFiltered = uiState.categoryFilter != CustomerCategoryFilter.ALL ||
        uiState.statusFilter != CustomerStatusFilter.ALL
    val displayedCustomers = remember(
        accumulatedCustomers,
        uiState.searchQuery,
        uiState.categoryFilter,
        uiState.statusFilter,
        sortOrder
    ) {
        filterAndSortOwnerCustomers(
            customers = accumulatedCustomers,
            searchQuery = uiState.searchQuery,
            categoryFilter = uiState.categoryFilter,
            statusFilter = uiState.statusFilter,
            sortOrder = sortOrder
        )
    }

    // TODO: Gunakan angka ringkasan agregat seluruh halaman saat CustomerRepository/CustomerListUiState menyediakannya (saat ini dihitung dari item termuat).
    val summary = remember(accumulatedCustomers, uiState.totalCustomers) {
        PelangganSummaryUiState(
            totalCustomers = uiState.totalCustomers,
            generalCount = accumulatedCustomers.count { !it.isContractor },
            contractorCount = accumulatedCustomers.count { it.isContractor }
        )
    }

    val hasMorePages = uiState.page < uiState.totalPages
    val footerStatus = when {
        accumulatedCustomers.isEmpty() -> PelangganFooterStatus.NONE
        uiState.isLoading -> PelangganFooterStatus.LOADING
        uiState.errorMessage != null -> PelangganFooterStatus.ERROR
        !hasMorePages -> PelangganFooterStatus.END_OF_LIST
        else -> PelangganFooterStatus.NONE
    }

    val isInitialLoading = uiState.isLoading && accumulatedCustomers.isEmpty()
    val isInitialError = uiState.errorMessage != null && accumulatedCustomers.isEmpty()
    val showEmptyState = !isInitialLoading && !isInitialError && displayedCustomers.isEmpty()
    val emptySpec = emptyStateSpecFor(
        searchQuery = uiState.searchQuery,
        isFiltered = isFiltered
    )

    var addedToastMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        OwnerCustomerToastBridge.consumePendingSuccess()?.let {
            addedToastMessage = it
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RefreshableContent(
            isRefreshing = uiState.isLoading && accumulatedCustomers.isNotEmpty(),
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            if (isInitialLoading || isInitialError || showEmptyState) {
                OwnerPelangganScreen(
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
                    centerPanelContent = showEmptyState || isInitialError,
                    panelContent = { isTablet ->
                        when {
                            isInitialLoading -> loadingContent(isTablet)
                            isInitialError -> errorContent()
                            else -> EmptyState(
                                icon = emptySpec.icon,
                                iconContainerColor = emptySpec.iconContainerColor,
                                iconTint = emptySpec.iconTint,
                                title = emptySpec.title,
                                description = emptySpec.description,
                                showAddButton = canManage,
                                onAddCustomerClick = onAddCustomerClick,
                                modifier = if (isTablet) Modifier.widthIn(max = 360.dp) else Modifier
                            )
                        }
                    }
                )
            } else {
                OwnerPelangganDataScreen(
                    summary = summary,
                    customers = displayedCustomers,
                    sortLabel = sortOrder.label,
                    statusFilter = uiState.statusFilter,
                    footerStatus = footerStatus,
                    hasMorePages = hasMorePages,
                    canManage = canManage,
                    isSearchExpanded = isSearchExpanded,
                    searchQuery = uiState.searchQuery,
                    isFiltered = isFiltered,
                    onBack = navigateBack,
                    onOpenSearch = onOpenSearch,
                    onCloseSearch = onCloseSearch,
                    onSearchChanged = onSearchChanged,
                    onOpenFilter = onOpenFilter,
                    onAddCustomerClick = onAddCustomerClick,
                    onCustomerClick = onCustomerDetailClick,
                    onLoadMore = onLoadNextPage,
                    onRetryLoadMore = { onRetryLoadMore((lastLoadedPage + 1).coerceAtLeast(1)) },
                    onNavSelect = ::handleNavSelect
                )
            }
        }

        TopFloatingCapsule(
            message = uiState.message,
            error = if (accumulatedCustomers.isEmpty() && !isInitialError) uiState.errorMessage else null,
            onDismiss = onDismissMessage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                .zIndex(99f)
        )

        AppAlert(
            type = AlertType.Success,
            title = addedToastMessage.orEmpty(),
            message = "",
            visible = !addedToastMessage.isNullOrBlank(),
            onDismiss = { addedToastMessage = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .zIndex(99f)
        )
    }
}

@Composable
internal fun OwnerPelangganScreen(
    summary: PelangganSummaryUiState,
    isSearchExpanded: Boolean,
    searchQuery: String,
    isFiltered: Boolean,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    centerPanelContent: Boolean,
    panelContent: @Composable (isTablet: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    autoFocusSearch: Boolean = true
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        val isTablet = maxWidth >= OWNER_PELANGGAN_TABLET_BREAKPOINT
        if (isTablet) {
            TabletPelangganLayout(
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
                centerPanelContent = centerPanelContent,
                autoFocusSearch = autoFocusSearch,
                panelContent = { panelContent(true) }
            )
        } else {
            PhonePelangganLayout(
                summary = summary,
                isSearchExpanded = isSearchExpanded,
                searchQuery = searchQuery,
                isFiltered = isFiltered,
                onBack = onBack,
                onOpenSearch = onOpenSearch,
                onCloseSearch = onCloseSearch,
                onSearchChanged = onSearchChanged,
                onOpenFilter = onOpenFilter,
                centerPanelContent = centerPanelContent,
                autoFocusSearch = autoFocusSearch,
                panelContent = { panelContent(false) }
            )
        }
    }
}

@Composable
internal fun OwnerPelangganDataScreen(
    summary: PelangganSummaryUiState,
    customers: List<Customer>,
    sortLabel: String,
    statusFilter: CustomerStatusFilter = CustomerStatusFilter.ALL,
    footerStatus: PelangganFooterStatus,
    hasMorePages: Boolean,
    canManage: Boolean,
    isSearchExpanded: Boolean,
    searchQuery: String,
    isFiltered: Boolean,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onAddCustomerClick: () -> Unit,
    onCustomerClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    autoFocusSearch: Boolean = true
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState, customers.size, hasMorePages, footerStatus) {
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
        val isTablet = maxWidth >= OWNER_PELANGGAN_TABLET_BREAKPOINT
        if (isTablet) {
            TabletPelangganDataLayout(
                summary = summary,
                customers = customers,
                sortLabel = sortLabel,
                statusFilter = statusFilter,
                footerStatus = footerStatus,
                canManage = canManage,
                isSearchExpanded = isSearchExpanded,
                searchQuery = searchQuery,
                isFiltered = isFiltered,
                listState = listState,
                onBack = onBack,
                onOpenSearch = onOpenSearch,
                onCloseSearch = onCloseSearch,
                onSearchChanged = onSearchChanged,
                onOpenFilter = onOpenFilter,
                onAddCustomerClick = onAddCustomerClick,
                onCustomerClick = onCustomerClick,
                onRetryLoadMore = onRetryLoadMore,
                onNavSelect = onNavSelect,
                autoFocusSearch = autoFocusSearch
            )
        } else {
            PhonePelangganDataLayout(
                summary = summary,
                customers = customers,
                sortLabel = sortLabel,
                statusFilter = statusFilter,
                footerStatus = footerStatus,
                canManage = canManage,
                isSearchExpanded = isSearchExpanded,
                searchQuery = searchQuery,
                isFiltered = isFiltered,
                listState = listState,
                onBack = onBack,
                onOpenSearch = onOpenSearch,
                onCloseSearch = onCloseSearch,
                onSearchChanged = onSearchChanged,
                onOpenFilter = onOpenFilter,
                onAddCustomerClick = onAddCustomerClick,
                onCustomerClick = onCustomerClick,
                onRetryLoadMore = onRetryLoadMore,
                autoFocusSearch = autoFocusSearch
            )
        }
    }
}

@Composable
private fun PelangganPhoneGreenHeader(
    summary: PelangganSummaryUiState,
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
                    autoFocus = autoFocusSearch
                )
            } else {
                SecondaryHeader(
                    title = "Pelanggan",
                    isFiltered = isFiltered,
                    onBack = onBack,
                    onSearchClick = onOpenSearch,
                    onFilterClick = onOpenFilter
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SummaryStats(summary = summary)
        }
    }
}

@Composable
private fun PelangganTabletGreenPanel(
    summary: PelangganSummaryUiState,
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
                    autoFocus = autoFocusSearch
                )
            } else {
                SecondaryHeader(
                    title = "Pelanggan",
                    isFiltered = isFiltered,
                    onBack = onBack,
                    onSearchClick = onOpenSearch,
                    onFilterClick = onOpenFilter
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                SummaryStats(summary = summary, isTablet = true)
            }
        }
    }
}

@Composable
private fun PelangganListHeaderSection(
    sortLabel: String,
    statusFilter: CustomerStatusFilter,
    canManage: Boolean,
    onAddCustomerClick: () -> Unit
) {
    val sectionTitle = when (statusFilter) {
        CustomerStatusFilter.ALL -> "Semua pelanggan"
        CustomerStatusFilter.ACTIVE -> "Pelanggan aktif"
        CustomerStatusFilter.INACTIVE -> "Pelanggan nonaktif"
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        if (canManage) {
            AddPelangganPrimaryButton(onClick = onAddCustomerClick)
            Spacer(modifier = Modifier.height(24.dp))
        }

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
                text = "Urut: $sortLabel",
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
                    .testTag("pelanggan-sort-label")
            )
        }
    }
}

@Composable
private fun PhonePelangganDataLayout(
    summary: PelangganSummaryUiState,
    customers: List<Customer>,
    sortLabel: String,
    statusFilter: CustomerStatusFilter = CustomerStatusFilter.ALL,
    footerStatus: PelangganFooterStatus,
    canManage: Boolean,
    isSearchExpanded: Boolean,
    searchQuery: String,
    isFiltered: Boolean,
    listState: LazyListState,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onAddCustomerClick: () -> Unit,
    onCustomerClick: (String) -> Unit,
    onRetryLoadMore: () -> Unit,
    autoFocusSearch: Boolean
) {
    val overlapPx = with(LocalDensity.current) { 28.dp.roundToPx() }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .testTag("owner-pelanggan-lazy-list")
    ) {
        item(key = "pelanggan-header-cap") {
            Layout(
                content = {
                    PelangganPhoneGreenHeader(
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
                            .padding(start = 20.dp, top = 24.dp, end = 20.dp)
                    ) {
                        PelangganListHeaderSection(
                            sortLabel = sortLabel,
                            statusFilter = statusFilter,
                            canManage = canManage,
                            onAddCustomerClick = onAddCustomerClick
                        )
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
            items = customers,
            key = { _, customer -> customer.id }
        ) { index, customer ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite)
                    .padding(horizontal = 20.dp)
            ) {
                CustomerRow(
                    customer = customer,
                    onClick = { onCustomerClick(customer.id) }
                )
                if (index < customers.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 52.dp)
                            .height(1.dp)
                            .background(OwnerDivider)
                    )
                }
            }
        }

        item(key = "pelanggan-footer") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite)
                    .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 32.dp)
            ) {
                ListFooterState(
                    status = footerStatus,
                    onRetry = onRetryLoadMore
                )
            }
        }
    }
}

@Composable
private fun TabletPelangganDataLayout(
    summary: PelangganSummaryUiState,
    customers: List<Customer>,
    sortLabel: String,
    statusFilter: CustomerStatusFilter = CustomerStatusFilter.ALL,
    footerStatus: PelangganFooterStatus,
    canManage: Boolean,
    isSearchExpanded: Boolean,
    searchQuery: String,
    isFiltered: Boolean,
    listState: LazyListState,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onAddCustomerClick: () -> Unit,
    onCustomerClick: (String) -> Unit,
    onRetryLoadMore: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    autoFocusSearch: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        // 1. Rel navigasi 88dp TETAP tampil, Menu aktif
        OwnerTabletNavRail(
            activeDestination = DashboardNavDestination.MENU,
            onNavSelect = onNavSelect
        )

        // 2 & 3. Panel hijau 432dp + Panel kanan putih scroll independen
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            PelangganTabletGreenPanel(
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
                    .padding(start = 400.dp)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp))
                    .background(SurfaceWhite)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("owner-pelanggan-lazy-list"),
                    contentPadding = PaddingValues(start = 64.dp, top = 32.dp, end = 32.dp, bottom = 32.dp)
                ) {
                    item(key = "pelanggan-top-controls") {
                        PelangganListHeaderSection(
                            sortLabel = sortLabel,
                            statusFilter = statusFilter,
                            canManage = canManage,
                            onAddCustomerClick = onAddCustomerClick
                        )
                    }

                    itemsIndexed(
                        items = customers,
                        key = { _, customer -> customer.id }
                    ) { index, customer ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            CustomerRow(
                                customer = customer,
                                onClick = { onCustomerClick(customer.id) }
                            )
                            if (index < customers.lastIndex) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 52.dp)
                                        .height(1.dp)
                                        .background(OwnerDivider)
                                )
                            }
                        }
                    }

                    item(key = "pelanggan-footer") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            ListFooterState(
                                status = footerStatus,
                                onRetry = onRetryLoadMore
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhonePelangganLayout(
    summary: PelangganSummaryUiState,
    isSearchExpanded: Boolean,
    searchQuery: String,
    isFiltered: Boolean,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    centerPanelContent: Boolean,
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
                    PelangganPhoneGreenHeader(
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
                        contentAlignment = if (centerPanelContent) Alignment.Center else Alignment.TopStart
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
private fun TabletPelangganLayout(
    summary: PelangganSummaryUiState,
    isSearchExpanded: Boolean,
    searchQuery: String,
    isFiltered: Boolean,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    centerPanelContent: Boolean,
    autoFocusSearch: Boolean,
    panelContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        // 1. Rel navigasi 88dp TETAP tampil, Menu aktif
        OwnerTabletNavRail(
            activeDestination = DashboardNavDestination.MENU,
            onNavSelect = onNavSelect
        )

        // 2 & 3. Panel hijau 432dp + Panel putih menumpuk 32dp ke kiri
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            PelangganTabletGreenPanel(
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
                        contentAlignment = if (centerPanelContent) Alignment.Center else Alignment.TopStart
                    ) {
                        panelContent()
                    }
                }
            }
        }
    }
}

internal fun sampleReferenceCustomers(): List<Customer> = listOf(
    Customer(
        id = "c1",
        name = "Budi Santoso",
        phone = "081234567890",
        address = null,
        isContractor = true,
        creditLimit = BigDecimal("5000000"),
        paymentTermDays = 14,
        isActive = true,
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z"
    ),
    Customer(
        id = "c2",
        name = "CV Maju Bangun",
        phone = "082155667788",
        address = null,
        isContractor = true,
        creditLimit = BigDecimal("10000000"),
        paymentTermDays = 30,
        isActive = true,
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z"
    ),
    Customer(
        id = "c3",
        name = "Dewi Wulandari",
        phone = "083822114455",
        address = null,
        isContractor = false,
        creditLimit = BigDecimal.ZERO,
        paymentTermDays = 0,
        isActive = false,
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z"
    ),
    Customer(
        id = "c4",
        name = "Hendra Kurniawan",
        phone = null,
        address = null,
        isContractor = false,
        creditLimit = BigDecimal.ZERO,
        paymentTermDays = 0,
        isActive = true,
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z"
    ),
    Customer(
        id = "c5",
        name = "Jagad",
        phone = "08915454644",
        address = null,
        isContractor = false,
        creditLimit = BigDecimal.ZERO,
        paymentTermDays = 0,
        isActive = true,
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z"
    ),
    Customer(
        id = "c6",
        name = "Siti Aminah",
        phone = "085711223344",
        address = null,
        isContractor = false,
        creditLimit = BigDecimal.ZERO,
        paymentTermDays = 0,
        isActive = true,
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z"
    )
)

@Preview(
    name = "OwnerPelangganEmptyPhone",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
internal fun OwnerPelangganEmptyPhonePreview() {
    TbterminalappTheme {
        val spec = emptyStateSpecFor(searchQuery = "", isFiltered = false)
        OwnerPelangganScreen(
            summary = PelangganSummaryUiState(),
            isSearchExpanded = false,
            searchQuery = "",
            isFiltered = false,
            onBack = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChanged = {},
            onOpenFilter = {},
            onNavSelect = {},
            centerPanelContent = true,
            panelContent = { isTablet ->
                EmptyState(
                    icon = spec.icon,
                    iconContainerColor = spec.iconContainerColor,
                    iconTint = spec.iconTint,
                    title = spec.title,
                    description = spec.description,
                    showAddButton = true,
                    onAddCustomerClick = {},
                    modifier = if (isTablet) Modifier.widthIn(max = 360.dp) else Modifier
                )
            }
        )
    }
}

@Preview(
    name = "OwnerPelangganEmptyTablet",
    showBackground = true,
    widthDp = 1024,
    heightDp = 768
)
@Composable
internal fun OwnerPelangganEmptyTabletPreview() {
    TbterminalappTheme {
        val spec = emptyStateSpecFor(searchQuery = "", isFiltered = false)
        OwnerPelangganScreen(
            summary = PelangganSummaryUiState(),
            isSearchExpanded = false,
            searchQuery = "",
            isFiltered = false,
            onBack = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChanged = {},
            onOpenFilter = {},
            onNavSelect = {},
            centerPanelContent = true,
            panelContent = { isTablet ->
                EmptyState(
                    icon = spec.icon,
                    iconContainerColor = spec.iconContainerColor,
                    iconTint = spec.iconTint,
                    title = spec.title,
                    description = spec.description,
                    showAddButton = true,
                    onAddCustomerClick = {},
                    modifier = if (isTablet) Modifier.widthIn(max = 360.dp) else Modifier
                )
            }
        )
    }
}

@Preview(
    name = "OwnerPelangganSearchNotFoundPhone",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
internal fun OwnerPelangganSearchNotFoundPhonePreview() {
    TbterminalappTheme {
        val query = "Pak Budi"
        val spec = emptyStateSpecFor(searchQuery = query, isFiltered = false)
        OwnerPelangganScreen(
            summary = PelangganSummaryUiState(),
            isSearchExpanded = true,
            searchQuery = query,
            isFiltered = false,
            onBack = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChanged = {},
            onOpenFilter = {},
            onNavSelect = {},
            centerPanelContent = true,
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
                    modifier = if (isTablet) Modifier.widthIn(max = 360.dp) else Modifier
                )
            }
        )
    }
}

@Preview(
    name = "OwnerPelangganDataPhone",
    showBackground = true,
    widthDp = 390,
    heightDp = 1064
)
@Composable
internal fun OwnerPelangganDataPhonePreview() {
    TbterminalappTheme {
        OwnerPelangganDataScreen(
            summary = PelangganSummaryUiState(
                totalCustomers = 6L,
                generalCount = 4,
                contractorCount = 2
            ),
            customers = sampleReferenceCustomers(),
            sortLabel = CustomerSortOrder.NAME_ASC.label,
            footerStatus = PelangganFooterStatus.LOADING,
            hasMorePages = true,
            canManage = true,
            isSearchExpanded = false,
            searchQuery = "",
            isFiltered = false,
            onBack = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChanged = {},
            onOpenFilter = {},
            onAddCustomerClick = {},
            onCustomerClick = {},
            onLoadMore = {},
            onRetryLoadMore = {},
            onNavSelect = {}
        )
    }
}

@Preview(
    name = "OwnerPelangganDataTablet",
    showBackground = true,
    widthDp = 1024,
    heightDp = 768
)
@Composable
internal fun OwnerPelangganDataTabletPreview() {
    TbterminalappTheme {
        OwnerPelangganDataScreen(
            summary = PelangganSummaryUiState(
                totalCustomers = 6L,
                generalCount = 4,
                contractorCount = 2
            ),
            customers = sampleReferenceCustomers(),
            sortLabel = CustomerSortOrder.NAME_ASC.label,
            footerStatus = PelangganFooterStatus.LOADING,
            hasMorePages = true,
            canManage = true,
            isSearchExpanded = false,
            searchQuery = "",
            isFiltered = false,
            onBack = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChanged = {},
            onOpenFilter = {},
            onAddCustomerClick = {},
            onCustomerClick = {},
            onLoadMore = {},
            onRetryLoadMore = {},
            onNavSelect = {}
        )
    }
}
