package com.tbterminal.app.ui.dashboard.owner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.tbterminal.app.data.model.Supplier
import com.tbterminal.app.ui.auth.AlertType
import com.tbterminal.app.ui.auth.AppAlert
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.components.TopFloatingCapsule
import com.tbterminal.app.ui.dashboard.LocalBackofficePageNavigation
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator
import com.tbterminal.app.ui.suppliers.SupplierSortOrder
import com.tbterminal.app.ui.suppliers.SupplierStatusFilter
import com.tbterminal.app.ui.suppliers.SupplierUiState
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.FieldErrorColor
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.Muted
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.OwnerBorder
import com.tbterminal.app.ui.theme.OwnerDivider
import com.tbterminal.app.ui.theme.SurfaceWhite
import com.tbterminal.app.ui.theme.TbterminalappTheme

private val OWNER_SUPPLIER_DATA_TABLET_BREAKPOINT = 840.dp

internal fun filterAndSortOwnerSuppliers(
    suppliers: List<Supplier>,
    searchQuery: String,
    statusFilter: SupplierStatusFilter,
    sortOrder: SupplierSortOrder
): List<Supplier> {
    val trimmedQuery = searchQuery.trim()
    val queryDigits = trimmedQuery.filter(Char::isDigit)

    val filtered = suppliers
        .asSequence()
        .filter { supplier ->
            when (statusFilter) {
                SupplierStatusFilter.ALL -> true
                SupplierStatusFilter.ACTIVE -> supplier.isActive
                SupplierStatusFilter.INACTIVE -> !supplier.isActive
            }
        }
        .filter { supplier ->
            if (trimmedQuery.isEmpty()) {
                true
            } else {
                val matchesName = supplier.name.contains(trimmedQuery, ignoreCase = true)
                val phoneRaw = supplier.phone.orEmpty()
                val matchesPhone = phoneRaw.contains(trimmedQuery, ignoreCase = true) ||
                    (queryDigits.isNotEmpty() && phoneRaw.filter(Char::isDigit).contains(queryDigits))
                val matchesAddress = supplier.address.orEmpty().contains(trimmedQuery, ignoreCase = true)
                matchesName || matchesPhone || matchesAddress
            }
        }
        .toList()

    return when (sortOrder) {
        SupplierSortOrder.NAME_ASC -> filtered.sortedBy { it.name.lowercase() }
        SupplierSortOrder.NAME_DESC -> filtered.sortedByDescending { it.name.lowercase() }
        SupplierSortOrder.TERM_DESC -> filtered.sortedWith(
            compareByDescending<Supplier> { it.paymentTermDays }
                .thenBy { it.name.lowercase() }
        )
    }
}

@Composable
internal fun OwnerSupplierRoute(
    uiState: SupplierUiState,
    canManage: Boolean,
    isSearchExpanded: Boolean,
    sortOrder: SupplierSortOrder,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSearchChanged: (String) -> Unit,
    onOpenFilter: () -> Unit,
    onAddSupplierClick: () -> Unit,
    onSupplierDetailClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onLoadNextPage: () -> Unit,
    onRetryLoadMore: () -> Unit,
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

    var accumulatedSuppliers by remember { mutableStateOf<List<Supplier>>(emptyList()) }
    var lastLoadedPage by remember { mutableIntStateOf(0) }
    var lastLoadedStatusFilter by remember { mutableStateOf(uiState.statusFilter) }
    val hasError = isSupplierErrorMessage(uiState.message)

    LaunchedEffect(uiState.suppliers, uiState.page, uiState.isLoading, uiState.message, uiState.statusFilter) {
        if (uiState.statusFilter != lastLoadedStatusFilter) {
            lastLoadedStatusFilter = uiState.statusFilter
            lastLoadedPage = 0
        }
        if (!uiState.isLoading && !hasError) {
            if (uiState.page <= 1) {
                accumulatedSuppliers = uiState.suppliers
                lastLoadedPage = 1
            } else if (uiState.page > lastLoadedPage) {
                accumulatedSuppliers = (accumulatedSuppliers + uiState.suppliers).distinctBy { it.id }
                lastLoadedPage = uiState.page
            } else if (uiState.page == lastLoadedPage) {
                val updatedIds = uiState.suppliers.associateBy { it.id }
                accumulatedSuppliers = accumulatedSuppliers.map { existing ->
                    updatedIds[existing.id] ?: existing
                }
            }
        }
    }

    val isFiltered = uiState.statusFilter != SupplierStatusFilter.ALL
    val displayedSuppliers = remember(
        accumulatedSuppliers,
        uiState.searchQuery,
        uiState.statusFilter,
        sortOrder
    ) {
        filterAndSortOwnerSuppliers(
            suppliers = accumulatedSuppliers,
            searchQuery = uiState.searchQuery,
            statusFilter = uiState.statusFilter,
            sortOrder = sortOrder
        )
    }

    val summary = remember(accumulatedSuppliers, uiState.totalSuppliers) {
        SupplierEmptySummaryUiState(
            totalSuppliers = uiState.totalSuppliers,
            activeCount = accumulatedSuppliers.count { it.isActive },
            inactiveCount = accumulatedSuppliers.count { !it.isActive }
        )
    }

    val hasMorePages = uiState.page < uiState.totalPages
    val footerStatus = when {
        accumulatedSuppliers.isEmpty() -> PelangganFooterStatus.NONE
        uiState.isLoading -> PelangganFooterStatus.LOADING
        hasError -> PelangganFooterStatus.ERROR
        !hasMorePages -> PelangganFooterStatus.END_OF_LIST
        else -> PelangganFooterStatus.NONE
    }

    val isInitialLoading = uiState.isLoading && accumulatedSuppliers.isEmpty()
    val isInitialError = hasError && accumulatedSuppliers.isEmpty() && !uiState.isLoading
    val showEmptyState = !isInitialLoading && !isInitialError && displayedSuppliers.isEmpty()
    val emptySpec = supplierEmptyStateSpecFor(
        searchQuery = uiState.searchQuery,
        isFiltered = isFiltered
    )

    var pendingToastMessage by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        OwnerSupplierToastBridge.consumePendingSuccess()?.let {
            pendingToastMessage = it
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RefreshableContent(
            isRefreshing = uiState.isLoading && accumulatedSuppliers.isNotEmpty(),
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            if (isInitialLoading || isInitialError || showEmptyState) {
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
                            isInitialError -> Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = uiState.message ?: "Supplier gagal dimuat.",
                                    style = ownerTextStyle(
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Muted
                                    ),
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = onRefresh,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Brand,
                                        contentColor = OnBrand
                                    )
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
            } else {
                OwnerSupplierDataScreen(
                    summary = summary,
                    suppliers = displayedSuppliers,
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
                    onAddSupplierClick = onAddSupplierClick,
                    onSupplierClick = { onSupplierDetailClick(it.id) },
                    onLoadMore = onLoadNextPage,
                    onRetryLoadMore = onRetryLoadMore,
                    onNavSelect = ::handleNavSelect
                )
            }
        }

        TopFloatingCapsule(
            message = if (!hasError) uiState.message else null,
            error = if (hasError && !isInitialError && accumulatedSuppliers.isEmpty()) uiState.message else null,
            onDismiss = onDismissMessage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                .zIndex(99f)
        )

        AppAlert(
            type = AlertType.Success,
            title = pendingToastMessage.orEmpty(),
            message = "",
            visible = !pendingToastMessage.isNullOrBlank(),
            onDismiss = { pendingToastMessage = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .zIndex(99f)
        )
    }
}

@Composable
internal fun OwnerSupplierDataScreen(
    summary: SupplierEmptySummaryUiState,
    suppliers: List<Supplier>,
    sortLabel: String,
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
    onAddSupplierClick: () -> Unit,
    onSupplierClick: (Supplier) -> Unit,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    autoFocusSearch: Boolean = true,
    statusFilter: SupplierStatusFilter = SupplierStatusFilter.ALL
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState, suppliers.size, hasMorePages, footerStatus) {
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
        val isTablet = maxWidth >= OWNER_SUPPLIER_DATA_TABLET_BREAKPOINT
        if (isTablet) {
            TabletSupplierDataLayout(
                summary = summary,
                suppliers = suppliers,
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
                onAddSupplierClick = onAddSupplierClick,
                onSupplierClick = onSupplierClick,
                onRetryLoadMore = onRetryLoadMore,
                onNavSelect = onNavSelect,
                autoFocusSearch = autoFocusSearch
            )
        } else {
            PhoneSupplierDataLayout(
                summary = summary,
                suppliers = suppliers,
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
                onAddSupplierClick = onAddSupplierClick,
                onSupplierClick = onSupplierClick,
                onRetryLoadMore = onRetryLoadMore,
                autoFocusSearch = autoFocusSearch
            )
        }
    }
}

@Composable
private fun SupplierListHeaderSection(
    sortLabel: String,
    statusFilter: SupplierStatusFilter,
    canManage: Boolean,
    onAddSupplierClick: () -> Unit
) {
    val sectionTitle = when (statusFilter) {
        SupplierStatusFilter.ALL -> "Semua supplier"
        SupplierStatusFilter.ACTIVE -> "Supplier aktif"
        SupplierStatusFilter.INACTIVE -> "Supplier nonaktif"
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        if (canManage) {
            AddPelangganPrimaryButton(
                onClick = onAddSupplierClick,
                testTag = "supplier-add",
                label = "Tambah supplier"
            )
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
                    .testTag("supplier-sort-label")
            )
        }
    }
}

@Composable
private fun PhoneSupplierDataLayout(
    summary: SupplierEmptySummaryUiState,
    suppliers: List<Supplier>,
    sortLabel: String,
    statusFilter: SupplierStatusFilter,
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
    onAddSupplierClick: () -> Unit,
    onSupplierClick: (Supplier) -> Unit,
    onRetryLoadMore: () -> Unit,
    autoFocusSearch: Boolean
) {
    val overlapPx = with(LocalDensity.current) { 28.dp.roundToPx() }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .testTag("owner-supplier-lazy-list")
    ) {
        item(key = "supplier-header-cap") {
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
                            .padding(start = 20.dp, top = 24.dp, end = 20.dp)
                    ) {
                        SupplierListHeaderSection(
                            sortLabel = sortLabel,
                            statusFilter = statusFilter,
                            canManage = canManage,
                            onAddSupplierClick = onAddSupplierClick
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
            items = suppliers,
            key = { _, supplier -> supplier.id }
        ) { index, supplier ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite)
                    .padding(horizontal = 20.dp)
            ) {
                SupplierRow(
                    supplier = supplier,
                    onClick = { onSupplierClick(supplier) }
                )
                if (index < suppliers.lastIndex) {
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

        item(key = "supplier-footer") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceWhite)
                    .padding(top = 8.dp, bottom = 24.dp)
            ) {
                ListFooterState(
                    status = footerStatus,
                    onRetry = onRetryLoadMore,
                    loadingText = "Memuat supplier lainnya\u2026",
                    endText = "Semua supplier sudah ditampilkan",
                    footerTestTag = "supplier-list-footer",
                    retryTestTag = "supplier-footer-retry"
                )
            }
        }
    }
}

@Composable
private fun TabletSupplierDataLayout(
    summary: SupplierEmptySummaryUiState,
    suppliers: List<Supplier>,
    sortLabel: String,
    statusFilter: SupplierStatusFilter,
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
    onAddSupplierClick: () -> Unit,
    onSupplierClick: (Supplier) -> Unit,
    onRetryLoadMore: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    autoFocusSearch: Boolean
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
                        .testTag("owner-supplier-lazy-list"),
                    contentPadding = PaddingValues(start = 64.dp, top = 32.dp, end = 32.dp, bottom = 32.dp)
                ) {
                    item(key = "supplier-top-controls") {
                        SupplierListHeaderSection(
                            sortLabel = sortLabel,
                            statusFilter = statusFilter,
                            canManage = canManage,
                            onAddSupplierClick = onAddSupplierClick
                        )
                    }

                    itemsIndexed(
                        items = suppliers,
                        key = { _, supplier -> supplier.id }
                    ) { index, supplier ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SupplierRow(
                                supplier = supplier,
                                onClick = { onSupplierClick(supplier) }
                            )
                            if (index < suppliers.lastIndex) {
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

                    item(key = "supplier-footer") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            ListFooterState(
                                status = footerStatus,
                                onRetry = onRetryLoadMore,
                                loadingText = "Memuat supplier lainnya\u2026",
                                endText = "Semua supplier sudah ditampilkan",
                                footerTestTag = "supplier-list-footer",
                                retryTestTag = "supplier-footer-retry"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SupplierRow(
    supplier: Supplier,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val phoneLabel = formatCustomerPhoneDisplay(supplier.phone)
    val addressText = supplier.address?.trim()?.takeIf { it.isNotEmpty() }
    val subtitle = if (addressText != null) "$phoneLabel \u00B7 $addressText" else phoneLabel
    val termLabel = if (supplier.paymentTermDays > 0) "${supplier.paymentTermDays} hari" else "Tunai"

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
            .testTag("supplier-row-${supplier.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatusAvatar(
            name = supplier.name,
            isActive = supplier.isActive
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = supplier.name,
                style = ownerTextStyle(
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (supplier.isActive) Ink else Muted
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

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = termLabel,
                style = ownerTextStyle(
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    tabularNumbers = true
                ),
                maxLines = 1,
                modifier = Modifier.cssLineBoxHeight(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (supplier.isActive) "Termin" else "Nonaktif",
                style = ownerTextStyle(
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (supplier.isActive) Muted else FieldErrorColor
                ),
                maxLines = 1,
                modifier = Modifier.cssLineBoxHeight(16.dp)
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

internal fun sampleReferenceSuppliers(): List<Supplier> = listOf(
    Supplier(
        id = "s1",
        name = "PT Semen Nusantara Tbk",
        phone = "081234567890",
        address = "Jl. Industri Raya No. 12",
        paymentTermDays = 30,
        isActive = true,
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z"
    ),
    Supplier(
        id = "s2",
        name = "CV Besi Jaya",
        phone = null,
        address = null,
        paymentTermDays = 0,
        isActive = true,
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z"
    ),
    Supplier(
        id = "s3",
        name = "UD Sumber Keramik",
        phone = "08915454644",
        address = "Kawasan Pergudangan Block C",
        paymentTermDays = 14,
        isActive = false,
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z"
    )
)

@Preview(
    name = "OwnerSupplierDataPhone",
    showBackground = true,
    widthDp = 390,
    heightDp = 844
)
@Composable
internal fun OwnerSupplierDataPhonePreview() {
    TbterminalappTheme {
        val sample = sampleReferenceSuppliers()
        OwnerSupplierDataScreen(
            summary = SupplierEmptySummaryUiState(totalSuppliers = 3, activeCount = 2, inactiveCount = 1),
            suppliers = sample,
            sortLabel = SupplierSortOrder.NAME_ASC.label,
            footerStatus = PelangganFooterStatus.END_OF_LIST,
            hasMorePages = false,
            canManage = true,
            isSearchExpanded = false,
            searchQuery = "",
            isFiltered = false,
            onBack = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChanged = {},
            onOpenFilter = {},
            onAddSupplierClick = {},
            onSupplierClick = {},
            onLoadMore = {},
            onRetryLoadMore = {},
            onNavSelect = {}
        )
    }
}

@Preview(
    name = "OwnerSupplierDataTablet",
    showBackground = true,
    widthDp = 1024,
    heightDp = 768
)
@Composable
internal fun OwnerSupplierDataTabletPreview() {
    TbterminalappTheme {
        val sample = sampleReferenceSuppliers()
        OwnerSupplierDataScreen(
            summary = SupplierEmptySummaryUiState(totalSuppliers = 3, activeCount = 2, inactiveCount = 1),
            suppliers = sample,
            sortLabel = SupplierSortOrder.NAME_ASC.label,
            footerStatus = PelangganFooterStatus.END_OF_LIST,
            hasMorePages = false,
            canManage = true,
            isSearchExpanded = false,
            searchQuery = "",
            isFiltered = false,
            onBack = {},
            onOpenSearch = {},
            onCloseSearch = {},
            onSearchChanged = {},
            onOpenFilter = {},
            onAddSupplierClick = {},
            onSupplierClick = {},
            onLoadMore = {},
            onRetryLoadMore = {},
            onNavSelect = {}
        )
    }
}
