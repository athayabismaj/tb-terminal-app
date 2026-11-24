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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.tbterminal.app.data.repository.AnalyticsRepository
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.PurchasingRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator
import com.tbterminal.app.ui.auth.AlertType
import com.tbterminal.app.ui.auth.AppAlert
import com.tbterminal.app.ui.auth.DotsPattern
import com.tbterminal.app.ui.auth.RingsPattern
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.SurfaceWhite
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Composable
internal fun OwnerTransaksiRoute(
    analyticsRepository: AnalyticsRepository,
    cashReconciliationRepository: CashReconciliationRepository,
    purchasingRepository: PurchasingRepository,
    name: String = "Owner",
    role: String = "owner",
    onLogout: () -> Unit = {},
    onNewSaleClick: () -> Unit,
    onNewPurchaseClick: () -> Unit,
    onSalesHistoryClick: () -> Unit,
    onPurchasesHistoryClick: () -> Unit,
    onSaleItemClick: (String) -> Unit,
    onPurchaseItemClick: (String) -> Unit,
    onFallbackDashboard: () -> Unit,
    onFallbackFinance: () -> Unit,
    onFallbackStock: () -> Unit,
    onFallbackMenu: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(TransaksiTab.PENJUALAN) }
    var balanceHidden by rememberSaveable { mutableStateOf(false) }
    var showAccountSheet by rememberSaveable { mutableStateOf(false) }
    var refreshTrigger by rememberSaveable { mutableIntStateOf(0) }
    var uiState by remember { mutableStateOf(TransaksiUiState(loading = true)) }
    val accountContext = rememberOwnerAccountContextState()
    val displayName = remember(name) { normalizeOwnerDisplayName(name) }

    LaunchedEffect(refreshTrigger) {
        uiState = uiState.copy(loading = true, error = null)
        val today = LocalDate.now()
        val monthStart = today.withDayOfMonth(1).toString()
        val todayIso = today.toString()

        coroutineScope {
            val metricsDeferred = async { runCatching { analyticsRepository.getDashboardMetrics() } }
            val dailyDeferred = async { runCatching { analyticsRepository.getDailySales(todayIso, todayIso) } }
            val salesDeferred = async {
                cashReconciliationRepository.getTransactions(page = 1, limit = 8, sessionId = null)
            }
            val purchasesDeferred = async {
                purchasingRepository.getPurchases(
                    page = 1,
                    limit = 20,
                    startDate = monthStart,
                    endDate = todayIso
                )
            }
            val unpaidDeferred = async {
                purchasingRepository.getPayables(page = 1, limit = 50, status = "belum_lunas")
            }
            val partialDeferred = async {
                purchasingRepository.getPayables(page = 1, limit = 50, status = "sebagian")
            }

            val metricsResult = metricsDeferred.await()
            val dailyResult = dailyDeferred.await()
            val salesResult = salesDeferred.await()
            val purchasesResult = purchasesDeferred.await()
            val unpaidResult = unpaidDeferred.await()
            val partialResult = partialDeferred.await()

            val unpaidPurchaseIds = buildSet {
                if (unpaidResult is RepositoryResult.Success) {
                    addAll(unpaidResult.data.data.mapNotNull { it.purchaseId })
                }
                if (partialResult is RepositoryResult.Success) {
                    addAll(partialResult.data.data.mapNotNull { it.purchaseId })
                }
            }

            val firstError = metricsResult.exceptionOrNull()?.message
                ?: listOf(salesResult, purchasesResult)
                    .filterIsInstance<RepositoryResult.Error>()
                    .firstOrNull()
                    ?.message
                ?: listOf(salesResult, purchasesResult)
                    .filterIsInstance<RepositoryResult.Exception>()
                    .firstOrNull()
                    ?.let { "Gagal memuat data transaksi." }

            val todayTxCount = dailyResult.getOrNull()?.sumOf { it.transactionCount }
                ?: (salesResult as? RepositoryResult.Success)?.data?.total
                ?: 0L

            val salesSummary = metricsResult.getOrNull()?.let { metrics ->
                TransaksiSummary(
                    label = "Ringkasan penjualan",
                    periodChip = "Hari ini",
                    amount = metrics.totalRevenueToday.roundToLong(),
                    footerLabel = "Total transaksi",
                    footerValue = "$todayTxCount struk"
                )
            } ?: uiState.sales

            val mappedSales = when (salesResult) {
                is RepositoryResult.Success -> salesResult.data.data.map { tx ->
                    val shortCode = tx.receiptId.takeLast(4).padStart(4, '0')
                    val timePart = formatIsoTimeHm(tx.createdAt)
                    val method = tx.paymentMethods.firstOrNull()
                        ?.let(::formatPaymentMethodLabel)
                        ?: "Tunai"
                    val subtitle = if (timePart.isNotEmpty()) "$timePart · $method" else method
                    TransaksiItemUi(
                        id = tx.id,
                        title = "Penjualan #$shortCode",
                        subtitle = subtitle,
                        amount = tx.total.toLongAmount(),
                        kind = TransaksiItemKind.SALE,
                        unpaid = false
                    )
                }
                else -> uiState.salesItems
            }

            val (purchasesSummary, mappedPurchases) = when (purchasesResult) {
                is RepositoryResult.Success -> {
                    val page = purchasesResult.data
                    val monthTotal = page.data.fold(0L) { acc, item -> acc + item.total.toLongAmount() }
                    val summary = TransaksiSummary(
                        label = "Total pembelian bulan ini",
                        periodChip = "Bulan berjalan",
                        amount = monthTotal,
                        footerLabel = "Total faktur masuk",
                        footerValue = "${page.total} faktur"
                    )
                    val items = page.data.take(6).map { p ->
                        val invNo = p.invoiceNo?.takeIf { it.isNotBlank() }
                            ?: "#${p.id.takeLast(4).uppercase()}"
                        val title = if (invNo.startsWith("Faktur", ignoreCase = true)) invNo else "Faktur $invNo"
                        val isUnpaid = p.id in unpaidPurchaseIds
                        val dateLabel = formatIsoDayMonth(p.receivedAt.ifBlank { p.createdAt })
                        val statusLabel = if (isUnpaid) "Belum lunas" else "Lunas"
                        val subtitle = if (dateLabel.isNotEmpty()) "$dateLabel · $statusLabel" else statusLabel
                        TransaksiItemUi(
                            id = p.id,
                            title = title,
                            subtitle = subtitle,
                            amount = p.total.toLongAmount(),
                            kind = TransaksiItemKind.PURCHASE,
                            unpaid = isUnpaid
                        )
                    }
                    summary to items
                }
                else -> uiState.purchases to uiState.purchaseItems
            }

            uiState = uiState.copy(
                sales = salesSummary,
                purchases = purchasesSummary,
                salesItems = mappedSales,
                purchaseItems = mappedPurchases,
                loading = false,
                error = firstError
            )
        }
    }

    val destinationNavigator = LocalAdminDestinationNavigator.current
    fun navigateDest(dest: AdminDestination, fallback: () -> Unit) {
        destinationNavigator?.invoke(dest) ?: fallback()
    }

    var barangMasukSuccessToast by remember { mutableStateOf<String?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                OwnerBarangMasukToastBridge.consumePendingSuccess()?.let { msg ->
                    barangMasukSuccessToast = msg
                    refreshTrigger++
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RefreshableContent(
            isRefreshing = uiState.loading,
            onRefresh = { refreshTrigger++ },
            modifier = Modifier.fillMaxSize()
        ) {
            TransaksiScreen(
                state = uiState.copy(
                    userName = displayName,
                    synced = accountContext.isSynced,
                    tab = selectedTab,
                    balanceHidden = balanceHidden
                ),
                onTabChange = { selectedTab = it },
                onToggleHidden = { balanceHidden = !balanceHidden },
                onPrimaryAction = { tab ->
                    if (tab == TransaksiTab.PENJUALAN) onNewSaleClick() else onNewPurchaseClick()
                },
                onSeeHistory = { tab ->
                    if (tab == TransaksiTab.PENJUALAN) onSalesHistoryClick() else onPurchasesHistoryClick()
                },
                onItemClick = { id ->
                    if (selectedTab == TransaksiTab.PENJUALAN) onSaleItemClick(id) else onPurchaseItemClick(id)
                },
                onNavSelect = { nav ->
                    when (nav) {
                        DashboardNavDestination.BERANDA -> navigateDest(AdminDestination.Dashboard, onFallbackDashboard)
                        DashboardNavDestination.TRANSAKSI -> Unit
                        DashboardNavDestination.KEUANGAN -> navigateDest(AdminDestination.FinanceHub, onFallbackFinance)
                        DashboardNavDestination.STOK -> navigateDest(AdminDestination.StockHub, onFallbackStock)
                        DashboardNavDestination.MENU -> navigateDest(AdminDestination.MoreHub, onFallbackMenu)
                    }
                },
                onProfile = { showAccountSheet = true },
                onDismissError = { uiState = uiState.copy(error = null) },
                onRetryError = { refreshTrigger++ }
            )
        }

        AppAlert(
            type = AlertType.Success,
            title = barangMasukSuccessToast.orEmpty(),
            message = "",
            onDismiss = { barangMasukSuccessToast = null },
            visible = barangMasukSuccessToast != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                .zIndex(99f)
        )

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
fun TransaksiScreen(
    state: TransaksiUiState,
    onTabChange: (TransaksiTab) -> Unit,
    onToggleHidden: () -> Unit,
    onPrimaryAction: (TransaksiTab) -> Unit,
    onSeeHistory: (TransaksiTab) -> Unit,
    onItemClick: (String) -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    onProfile: () -> Unit = {},
    onDismissError: () -> Unit = {},
    onRetryError: () -> Unit = {}
) {
    TransaksiScreenContent(
        state = state,
        onTabChange = onTabChange,
        onToggleHidden = onToggleHidden,
        onPrimaryAction = onPrimaryAction,
        onSeeHistory = onSeeHistory,
        onItemClick = onItemClick,
        onNavSelect = onNavSelect,
        modifier = modifier,
        onProfile = onProfile,
        onDismissError = onDismissError,
        onRetryError = onRetryError
    )
}

@Composable
internal fun TransaksiScreenContent(
    state: TransaksiUiState,
    onTabChange: (TransaksiTab) -> Unit,
    onToggleHidden: () -> Unit,
    onPrimaryAction: (TransaksiTab) -> Unit,
    onSeeHistory: (TransaksiTab) -> Unit,
    onItemClick: (String) -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    onProfile: () -> Unit = {},
    onDismissError: () -> Unit = {},
    onRetryError: () -> Unit = {},
    initialSelectorExpanded: Boolean = false,
    renderSelectorInOverlay: Boolean = false
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        val isTablet = maxWidth >= 600.dp
        var selectorExpanded by rememberSaveable { mutableStateOf(initialSelectorExpanded) }
        var selectorOpenedOnce by remember { mutableStateOf(false) }
        val dropdownFocusRequester = remember { FocusRequester() }

        LaunchedEffect(selectorExpanded) {
            if (!selectorExpanded && selectorOpenedOnce) {
                selectorOpenedOnce = false
                runCatching { dropdownFocusRequester.requestFocus() }
            }
        }

        val openSelector: () -> Unit = {
            selectorOpenedOnce = true
            selectorExpanded = true
        }

        if (isTablet) {
            TabletTransaksiLayout(
                state = state,
                dropdownExpanded = selectorExpanded,
                dropdownFocusRequester = dropdownFocusRequester,
                onOpenSelector = openSelector,
                onToggleHidden = onToggleHidden,
                onPrimaryAction = onPrimaryAction,
                onSeeHistory = onSeeHistory,
                onItemClick = onItemClick,
                onNavSelect = onNavSelect,
                onProfile = onProfile
            )
        } else {
            PhoneTransaksiLayout(
                state = state,
                dropdownExpanded = selectorExpanded,
                dropdownFocusRequester = dropdownFocusRequester,
                onOpenSelector = openSelector,
                onToggleHidden = onToggleHidden,
                onPrimaryAction = onPrimaryAction,
                onSeeHistory = onSeeHistory,
                onItemClick = onItemClick,
                onNavSelect = onNavSelect,
                onProfile = onProfile
            )
        }

        if (selectorExpanded) {
            val selectAction: (TransaksiTab) -> Unit = { selected ->
                if (selected != state.tab) {
                    onTabChange(selected)
                }
                selectorExpanded = false
            }
            if (renderSelectorInOverlay) {
                TransactionTypeSelectorFrame(
                    selectedTab = state.tab,
                    isTablet = isTablet,
                    onSelect = selectAction,
                    onDismiss = { selectorExpanded = false }
                )
            } else {
                TransactionTypeSelector(
                    selectedTab = state.tab,
                    isTablet = isTablet,
                    onSelect = selectAction,
                    onDismiss = { selectorExpanded = false }
                )
            }
        }

        AppAlert(
            type = AlertType.Error,
            title = "Gagal memuat transaksi",
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
private fun PhoneTransaksiLayout(
    state: TransaksiUiState,
    dropdownExpanded: Boolean,
    dropdownFocusRequester: FocusRequester,
    onOpenSelector: () -> Unit,
    onToggleHidden: () -> Unit,
    onPrimaryAction: (TransaksiTab) -> Unit,
    onSeeHistory: (TransaksiTab) -> Unit,
    onItemClick: (String) -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    onProfile: () -> Unit
) {
    val activeSummary = if (state.tab == TransaksiTab.PENJUALAN) state.sales else state.purchases
    val activeItems = if (state.tab == TransaksiTab.PENJUALAN) state.salesItems else state.purchaseItems

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
            // 1. Header hijau (ikut scroll bersama isi)
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
                    TransaksiDropdownHeaderRow(
                        activeType = state.tab,
                        expanded = dropdownExpanded,
                        userName = state.userName,
                        synced = state.synced,
                        onDropdownClick = onOpenSelector,
                        onProfileClick = onProfile,
                        dropdownFocusRequester = dropdownFocusRequester
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
                            label = activeSummary.label
                        )

                        TransaksiPeriodStatusChip(label = activeSummary.periodChip)
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
                            text = if (state.balanceHidden) "Rp ••••••" else formatRupiahIdr(activeSummary.amount),
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
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    TransaksiSummaryFooterRow(
                        label = activeSummary.footerLabel,
                        value = if (state.loading) "—" else activeSummary.footerValue
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
                    .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 80.dp)
            ) {
                if (state.tab == TransaksiTab.PEMBELIAN) {
                    TransaksiPrimaryCtaButton(
                        tab = state.tab,
                        invertedOnGreen = false,
                        onClick = { onPrimaryAction(state.tab) }
                    )

                    Spacer(modifier = Modifier.height(28.dp))
                }

                TransaksiActivitySectionHeader(
                    tab = state.tab,
                    isTablet = false,
                    onSeeHistory = { onSeeHistory(state.tab) }
                )

                TransaksiActivityList(
                    tab = state.tab,
                    items = activeItems,
                    loading = state.loading,
                    isTablet = false,
                    onItemClick = onItemClick
                )
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
private fun TabletTransaksiLayout(
    state: TransaksiUiState,
    dropdownExpanded: Boolean,
    dropdownFocusRequester: FocusRequester,
    onOpenSelector: () -> Unit,
    onToggleHidden: () -> Unit,
    onPrimaryAction: (TransaksiTab) -> Unit,
    onSeeHistory: (TransaksiTab) -> Unit,
    onItemClick: (String) -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    onProfile: () -> Unit
) {
    val activeSummary = if (state.tab == TransaksiTab.PENJUALAN) state.sales else state.purchases
    val activeItems = if (state.tab == TransaksiTab.PENJUALAN) state.salesItems else state.purchaseItems

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        // 1. Menu samping tetap (88dp)
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
            // 2. Panel hijau tetap 432dp (isi 352dp: kiri 24dp, kanan 56dp), hanya lingkaran
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
                    TransaksiDropdownHeaderRow(
                        activeType = state.tab,
                        expanded = dropdownExpanded,
                        userName = state.userName,
                        synced = state.synced,
                        onDropdownClick = onOpenSelector,
                        onProfileClick = onProfile,
                        dropdownFocusRequester = dropdownFocusRequester
                    )

                    // Blok ringkasan dipusatkan vertikal di sisa ruang
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OwnerNetSalesLabelRow(
                                balanceHidden = state.balanceHidden,
                                onToggleHidden = onToggleHidden,
                                label = activeSummary.label
                            )

                            TransaksiPeriodStatusChip(label = activeSummary.periodChip)
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
                                text = if (state.balanceHidden) "Rp ••••••" else formatRupiahIdr(activeSummary.amount),
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
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        TransaksiSummaryFooterRow(
                            label = activeSummary.footerLabel,
                            value = if (state.loading) "—" else activeSummary.footerValue
                        )
                    }

                    if (state.tab == TransaksiTab.PEMBELIAN) {
                        // Tombol CTA tablet khusus Pembelian: latar putih, teks & ikon hijau
                        TransaksiPrimaryCtaButton(
                            tab = state.tab,
                            invertedOnGreen = true,
                            onClick = { onPrimaryAction(state.tab) }
                        )
                    }
                }
            }

            // 3. Panel putih menumpuk 32dp ke panel hijau (432 - 32 = 400dp), sudut kiri radius 32dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 400.dp)
                    .clip(RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp))
                    .background(SurfaceWhite)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 64.dp, top = 40.dp, end = 32.dp, bottom = 40.dp)
            ) {
                TransaksiActivitySectionHeader(
                    tab = state.tab,
                    isTablet = true,
                    onSeeHistory = { onSeeHistory(state.tab) }
                )

                TransaksiActivityList(
                    tab = state.tab,
                    items = activeItems,
                    loading = state.loading,
                    isTablet = true,
                    onItemClick = onItemClick
                )
            }
        }
    }
}

private fun BigDecimal.toLongAmount(): Long =
    runCatching { setScale(0, java.math.RoundingMode.HALF_UP).toLong() }.getOrDefault(toLong())

private fun formatPaymentMethodLabel(raw: String): String = when (raw.uppercase()) {
    "CASH", "TUNAI" -> "Tunai"
    "QRIS" -> "QRIS"
    "TRANSFER", "BANK_TRANSFER" -> "Transfer"
    "CREDIT", "PIUTANG" -> "Piutang"
    else -> raw.lowercase().replaceFirstChar { it.uppercase() }
}

private fun formatIsoTimeHm(iso: String): String {
    val instant = runCatching { Instant.parse(iso) }.getOrNull() ?: return ""
    val localTime = instant.atZone(ZoneId.systemDefault()).toLocalTime()
    return localTime.format(DateTimeFormatter.ofPattern("HH:mm"))
}

private fun formatIsoDayMonth(iso: String): String {
    val months = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
    val date = runCatching { Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDate() }.getOrNull()
        ?: runCatching { LocalDate.parse(iso.take(10)) }.getOrNull()
        ?: return ""
    return "${date.dayOfMonth} ${months[(date.monthValue - 1).coerceIn(0, 11)]}"
}

// ═════════════════════════════════════════════════════════════════════════════
// PREVIEWS (4 KONDISI: PONSEL 390x844 & TABLET 1024x768, PENJUALAN & PEMBELIAN)
// ═════════════════════════════════════════════════════════════════════════════
@Preview(
    name = "Barokah Jaya Transaksi - Penjualan Ponsel (390x844)",
    widthDp = 390,
    heightDp = 844,
    showBackground = true
)
@Composable
fun OwnerTransaksiSalesPhonePreview() {
    TbterminalappTheme {
        TransaksiScreen(
            state = sampleReferenceTransaksiState(tab = TransaksiTab.PENJUALAN, isTablet = false),
            onTabChange = {},
            onToggleHidden = {},
            onPrimaryAction = {},
            onSeeHistory = {},
            onItemClick = {},
            onNavSelect = {}
        )
    }
}

@Preview(
    name = "Barokah Jaya Transaksi - Pembelian Ponsel (390x844)",
    widthDp = 390,
    heightDp = 844,
    showBackground = true
)
@Composable
fun OwnerTransaksiPurchasePhonePreview() {
    TbterminalappTheme {
        TransaksiScreen(
            state = sampleReferenceTransaksiState(tab = TransaksiTab.PEMBELIAN, isTablet = false),
            onTabChange = {},
            onToggleHidden = {},
            onPrimaryAction = {},
            onSeeHistory = {},
            onItemClick = {},
            onNavSelect = {}
        )
    }
}

@Preview(
    name = "Barokah Jaya Transaksi - Penjualan Tablet (1024x768)",
    widthDp = 1024,
    heightDp = 768,
    showBackground = true
)
@Composable
fun OwnerTransaksiSalesTabletPreview() {
    TbterminalappTheme {
        TransaksiScreen(
            state = sampleReferenceTransaksiState(tab = TransaksiTab.PENJUALAN, isTablet = true),
            onTabChange = {},
            onToggleHidden = {},
            onPrimaryAction = {},
            onSeeHistory = {},
            onItemClick = {},
            onNavSelect = {}
        )
    }
}

@Preview(
    name = "Barokah Jaya Transaksi - Pembelian Tablet (1024x768)",
    widthDp = 1024,
    heightDp = 768,
    showBackground = true
)
@Composable
fun OwnerTransaksiPurchaseTabletPreview() {
    TbterminalappTheme {
        TransaksiScreen(
            state = sampleReferenceTransaksiState(tab = TransaksiTab.PEMBELIAN, isTablet = true),
            onTabChange = {},
            onToggleHidden = {},
            onPrimaryAction = {},
            onSeeHistory = {},
            onItemClick = {},
            onNavSelect = {}
        )
    }
}

@Composable
internal fun OwnerTransaksiDropdownOpenReferenceFrame(
    isTablet: Boolean,
    tab: TransaksiTab = TransaksiTab.PENJUALAN
) {
    TransaksiScreenContent(
        state = sampleReferenceTransaksiState(tab = tab, isTablet = isTablet),
        onTabChange = {},
        onToggleHidden = {},
        onPrimaryAction = {},
        onSeeHistory = {},
        onItemClick = {},
        onNavSelect = {},
        initialSelectorExpanded = true,
        renderSelectorInOverlay = true
    )
}

@Preview(
    name = "Barokah Jaya Transaksi - Dropdown Terbuka Ponsel (390x844)",
    widthDp = 390,
    heightDp = 844,
    showBackground = true
)
@Composable
fun OwnerTransaksiDropdownOpenPhonePreview() {
    TbterminalappTheme {
        OwnerTransaksiDropdownOpenReferenceFrame(isTablet = false)
    }
}

@Preview(
    name = "Barokah Jaya Transaksi - Dropdown Terbuka Tablet (1024x768)",
    widthDp = 1024,
    heightDp = 768,
    showBackground = true
)
@Composable
fun OwnerTransaksiDropdownOpenTabletPreview() {
    TbterminalappTheme {
        OwnerTransaksiDropdownOpenReferenceFrame(isTablet = true)
    }
}
