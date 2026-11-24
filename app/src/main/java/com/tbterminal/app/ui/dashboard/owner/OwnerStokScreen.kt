package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.costing.MovingAverageCalculator
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.repository.SyncMonitoringRepository
import com.tbterminal.app.ui.auth.AlertType
import com.tbterminal.app.ui.auth.AppAlert
import com.tbterminal.app.ui.auth.DotsPattern
import com.tbterminal.app.ui.auth.RingsPattern
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator
import com.tbterminal.app.ui.dashboard.loadAllProductStocks
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.SurfaceWhite
import com.tbterminal.app.ui.theme.TbterminalappTheme
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Composable
internal fun OwnerStokRoute(
    inventoryRepository: InventoryRepository,
    syncMonitoringRepository: SyncMonitoringRepository?,
    name: String = "Owner",
    role: String = "owner",
    onLogout: () -> Unit = {},
    onAddProductClick: () -> Unit,
    onProductsClick: () -> Unit,
    onPriceManagementClick: () -> Unit,
    onStockReportClick: () -> Unit,
    onStockOpnameClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    onUnitsClick: () -> Unit,
    onFallbackDashboard: () -> Unit,
    onFallbackTransactions: () -> Unit,
    onFallbackFinance: () -> Unit,
    onFallbackMenu: () -> Unit
) {
    var balanceHidden by rememberSaveable { mutableStateOf(false) }
    var showAccountSheet by rememberSaveable { mutableStateOf(false) }
    var refreshTrigger by rememberSaveable { mutableIntStateOf(0) }
    var uiState by remember { mutableStateOf(StokUiState(loading = true)) }
    val accountContext = rememberOwnerAccountContextState()
    val displayName = remember(name) { normalizeOwnerDisplayName(name) }

    LaunchedEffect(inventoryRepository, refreshTrigger) {
        uiState = uiState.copy(loading = true, error = null)
        coroutineScope {
            val stockDeferred = async { runCatching { loadAllProductStocks(inventoryRepository) } }
            val categoriesDeferred = async { runCatching { inventoryRepository.getCategories() } }
            val unitsDeferred = async { runCatching { inventoryRepository.getUnits() } }

            val stockRes = stockDeferred.await().getOrNull()
            val catRes = categoriesDeferred.await().getOrNull()
            val unitRes = unitsDeferred.await().getOrNull()

            var assetValuation = uiState.totalAssetValuation
            var totalProducts = uiState.totalProductsCount
            var lowStockCount = uiState.lowStockCount
            var totalCategories = uiState.totalCategoriesCount
            var totalUnits = uiState.totalUnitsCount

            if (stockRes is RepositoryResult.Success) {
                val aggregate = stockRes.data
                val products = aggregate.products
                val valuation = MovingAverageCalculator.calculateInventoryValuation(products)
                assetValuation = valuation.totalValuation.toDouble()
                lowStockCount = products.count { it.isActive && it.quantity <= it.minStock }
                totalProducts = aggregate.totalProducts
            }

            if (catRes is RepositoryResult.Success) {
                totalCategories = catRes.data.size
            }

            if (unitRes is RepositoryResult.Success) {
                totalUnits = unitRes.data.size
            }

            val repoResults = listOfNotNull(stockRes, catRes, unitRes)
            val firstError = repoResults.filterIsInstance<RepositoryResult.Error>().firstOrNull()?.message
                ?: repoResults.filterIsInstance<RepositoryResult.Exception>().firstOrNull()
                    ?.let { "Gagal memuat data stok." }

            uiState = uiState.copy(
                totalAssetValuation = assetValuation,
                totalProductsCount = totalProducts,
                lowStockCount = lowStockCount,
                totalCategoriesCount = totalCategories,
                totalUnitsCount = totalUnits,
                loading = false,
                error = firstError
            )
        }
    }

    LaunchedEffect(syncMonitoringRepository) {
        syncMonitoringRepository?.observeSnapshot()?.collect { snapshot ->
            val (statusText, healthy) = when {
                snapshot.totalFailed > 0 -> "${snapshot.totalFailed} Gagal" to false
                snapshot.totalPending > 0 || snapshot.totalSyncing > 0 ->
                    "${snapshot.totalPending + snapshot.totalSyncing} Menunggu" to false
                else -> "Tersinkron" to true
            }
            uiState = uiState.copy(syncStatusText = statusText, isSyncHealthy = healthy)
        }
    }

    val destinationNavigator = LocalAdminDestinationNavigator.current
    fun navigateDest(dest: AdminDestination, fallback: () -> Unit) {
        destinationNavigator?.invoke(dest) ?: fallback()
    }

    val handleAction: (StokActionId) -> Unit = { action ->
        when (action) {
            StokActionId.ADD_PRODUCT -> navigateDest(AdminDestination.AddProduct, onAddProductClick)
            StokActionId.PRODUCTS_STOCK -> navigateDest(AdminDestination.Products, onProductsClick)
            StokActionId.PRODUCT_PRICES -> navigateDest(AdminDestination.PriceManagement, onPriceManagementClick)
            StokActionId.STOCK_CARD -> navigateDest(AdminDestination.StockReport, onStockReportClick)
            StokActionId.ADJUST_STOCK -> navigateDest(AdminDestination.StockOpname, onStockOpnameClick)
            StokActionId.CATEGORIES -> navigateDest(AdminDestination.ProductCategories, onCategoriesClick)
            StokActionId.UNITS -> navigateDest(AdminDestination.ProductUnits, onUnitsClick)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RefreshableContent(
            isRefreshing = uiState.loading,
            onRefresh = { refreshTrigger++ },
            modifier = Modifier.fillMaxSize()
        ) {
            StokScreen(
                state = uiState.copy(
                    userName = displayName,
                    synced = accountContext.isSynced && uiState.isSyncHealthy,
                    balanceHidden = balanceHidden
                ),
                onToggleHidden = { balanceHidden = !balanceHidden },
                onActionClick = handleAction,
                onNavSelect = { nav ->
                    when (nav) {
                        DashboardNavDestination.BERANDA -> navigateDest(AdminDestination.Dashboard, onFallbackDashboard)
                        DashboardNavDestination.TRANSAKSI ->
                            navigateDest(AdminDestination.TransactionsHub, onFallbackTransactions)
                        DashboardNavDestination.KEUANGAN -> navigateDest(AdminDestination.FinanceHub, onFallbackFinance)
                        DashboardNavDestination.STOK -> Unit
                        DashboardNavDestination.MENU -> navigateDest(AdminDestination.MoreHub, onFallbackMenu)
                    }
                },
                onProfile = { showAccountSheet = true },
                onDismissError = { uiState = uiState.copy(error = null) },
                onRetryError = { refreshTrigger++ }
            )
        }

        OwnerAccountSheetHost(
            visible = showAccountSheet,
            userName = displayName,
            role = role,
            offlineStatus = accountContext.offlineStatus,
            devicePreferences = accountContext.devicePreferences,
            onDismiss = { showAccountSheet = false },
            onNavigate = ::navigateDest,
            onLogout = onLogout
        )
    }
}

@Composable
fun StokScreen(
    state: StokUiState,
    onToggleHidden: () -> Unit,
    onActionClick: (StokActionId) -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    onProfile: () -> Unit = {},
    onDismissError: () -> Unit = {},
    onRetryError: () -> Unit = {}
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        val isTablet = maxWidth >= 600.dp

        if (isTablet) {
            TabletStokLayout(
                state = state,
                onToggleHidden = onToggleHidden,
                onActionClick = onActionClick,
                onNavSelect = onNavSelect,
                onProfile = onProfile
            )
        } else {
            PhoneStokLayout(
                state = state,
                onToggleHidden = onToggleHidden,
                onActionClick = onActionClick,
                onNavSelect = onNavSelect,
                onProfile = onProfile
            )
        }

        AppAlert(
            type = AlertType.Error,
            title = "Gagal memuat stok",
            message = state.error.orEmpty(),
            actionLabel = "Coba lagi",
            onAction = onRetryError,
            onDismiss = onDismissError,
            visible = !state.error.isNullOrBlank(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = if (isTablet) 24.dp else 96.dp)
        )
    }
}

@Composable
private fun PhoneStokLayout(
    state: StokUiState,
    onToggleHidden: () -> Unit,
    onActionClick: (StokActionId) -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    onProfile: () -> Unit
) {
    val groups = remember(
        state.totalProductsCount,
        state.lowStockCount,
        state.totalCategoriesCount,
        state.totalUnitsCount,
        onActionClick
    ) { buildStokMenuGroups(state, onActionClick) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Header hijau (padding 56dp vertikal, 20dp horizontal)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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

                DotsPattern(
                    dotRadius = 1.5.dp,
                    step = 20.dp,
                    maskAtBottomRight = true,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(width = 190.dp, height = 110.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 56.dp)
                ) {
                    OwnerHeaderTitleRow(
                        title = "Stok",
                        userName = state.userName,
                        synced = state.synced,
                        onProfileClick = onProfile
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OwnerNetSalesLabelRow(
                            balanceHidden = state.balanceHidden,
                            onToggleHidden = onToggleHidden,
                            label = "Total nilai aset (HPP)",
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        StokHeroStatusChip(lowStockCount = state.lowStockCount)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (state.loading) {
                        Box(
                            modifier = Modifier
                                .width(220.dp)
                                .height(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.20f))
                        )
                    } else {
                        Text(
                            text = if (state.balanceHidden) "Rp ••••••" else formatRupiahSpaced(state.totalAssetValuation),
                            style = ownerTextStyle(
                                fontSize = 40.sp,
                                lineHeight = 46.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnBrand,
                                letterSpacing = (-0.03).em,
                                tabularNumbers = true
                            ),
                            maxLines = 1,
                            modifier = Modifier
                                .height(46.dp)
                                .wrapContentHeight(align = Alignment.CenterVertically, unbounded = true)
                                .testTag("stok-hero-valuation")
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    StokHeroStatsRow(
                        state = state,
                        onLowStockClick = { onActionClick(StokActionId.PRODUCTS_STOCK) }
                    )
                }
            }

            // 2. Panel putih menumpuk 28dp ke atas header hijau (sudut atas radius 32dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val overlapPx = 28.dp.roundToPx()
                        layout(placeable.width, (placeable.height - overlapPx).coerceAtLeast(0)) {
                            placeable.placeRelative(0, -overlapPx)
                        }
                    }
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(SurfaceWhite)
                    .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 92.dp)
            ) {
                StokAddProductButton(
                    invertedOnGreen = false,
                    onClick = { onActionClick(StokActionId.ADD_PRODUCT) }
                )

                Spacer(modifier = Modifier.height(32.dp))

                groups.forEachIndexed { index, group ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                    OwnerPlainMenuSection(group = group)
                }
            }
        }

        // 3. Menu bawah menempel di dasar layar (tinggi 80dp)
        OwnerPhoneBottomNav(
            activeDestination = state.selectedNav,
            onNavSelect = onNavSelect,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun TabletStokLayout(
    state: StokUiState,
    onToggleHidden: () -> Unit,
    onActionClick: (StokActionId) -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    onProfile: () -> Unit
) {
    val groups = remember(
        state.totalProductsCount,
        state.lowStockCount,
        state.totalCategoriesCount,
        state.totalUnitsCount,
        onActionClick
    ) { buildStokMenuGroups(state, onActionClick) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        // 1. Rel navigasi samping tetap (88dp)
        OwnerTabletNavRail(
            activeDestination = state.selectedNav,
            onNavSelect = onNavSelect
        )

        // 2 & 3. Panel hijau 432dp + Panel putih menumpuk 32dp ke kiri
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // 2. Panel hijau tetap 432dp (isi 352dp: kiri 24dp, kanan 56dp), hanya pola lingkaran kanan atas
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
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
                    OwnerHeaderTitleRow(
                        title = "Stok",
                        userName = state.userName,
                        synced = state.synced,
                        onProfileClick = onProfile
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OwnerNetSalesLabelRow(
                            balanceHidden = state.balanceHidden,
                            onToggleHidden = onToggleHidden,
                            label = "Total nilai aset (HPP)",
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        StokHeroStatusChip(lowStockCount = state.lowStockCount)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (state.loading) {
                        Box(
                            modifier = Modifier
                                .width(240.dp)
                                .height(54.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.20f))
                        )
                    } else {
                        Text(
                            text = if (state.balanceHidden) "Rp ••••••" else formatRupiahSpaced(state.totalAssetValuation),
                            style = ownerTextStyle(
                                fontSize = 48.sp,
                                lineHeight = 54.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnBrand,
                                letterSpacing = (-0.03).em,
                                tabularNumbers = true
                            ),
                            maxLines = 1,
                            modifier = Modifier
                                .height(54.dp)
                                .wrapContentHeight(align = Alignment.CenterVertically, unbounded = true)
                                .testTag("stok-hero-valuation")
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    StokHeroStatsRow(
                        state = state,
                        onLowStockClick = { onActionClick(StokActionId.PRODUCTS_STOCK) }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Tombol putih "Tambah produk" menempel di bawah panel hijau
                    StokAddProductButton(
                        invertedOnGreen = true,
                        onClick = { onActionClick(StokActionId.ADD_PRODUCT) }
                    )
                }
            }

            // 3. Panel putih menumpuk 32dp ke panel hijau (432 - 32 = 400dp), sudut kiri radius 32dp, bisa di-scroll
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 400.dp)
                    .clip(RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp))
                    .background(SurfaceWhite)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 64.dp, top = 40.dp, end = 32.dp, bottom = 32.dp)
            ) {
                groups.forEachIndexed { index, group ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                    OwnerPlainMenuSection(group = group)
                }
            }
        }
    }
}

@Preview(
    name = "Barokah Jaya Stok - Ponsel (390x1118)",
    widthDp = 390,
    heightDp = 1118,
    showBackground = true
)
@Composable
fun OwnerStokPhonePreview() {
    TbterminalappTheme {
        StokScreen(
            state = sampleReferenceStokState(),
            onToggleHidden = {},
            onActionClick = {},
            onNavSelect = {}
        )
    }
}

@Preview(
    name = "Barokah Jaya Stok - Tablet (1024x768)",
    widthDp = 1024,
    heightDp = 768,
    showBackground = true
)
@Composable
fun OwnerStokTabletPreview() {
    TbterminalappTheme {
        StokScreen(
            state = sampleReferenceStokState(),
            onToggleHidden = {},
            onActionClick = {},
            onNavSelect = {}
        )
    }
}
