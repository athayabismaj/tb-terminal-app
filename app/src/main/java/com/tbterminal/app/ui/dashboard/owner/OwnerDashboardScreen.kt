package com.tbterminal.app.ui.dashboard.owner

import androidx.compose.foundation.background
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
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.data.remote.DailySalesSummaryDto
import com.tbterminal.app.data.repository.AnalyticsRepository
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.OfflineDashboardRepository
import com.tbterminal.app.ui.auth.AlertType
import com.tbterminal.app.ui.auth.AppAlert
import com.tbterminal.app.ui.auth.DotsPattern
import com.tbterminal.app.ui.auth.RingsPattern
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardUiState
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardViewModel
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator
import com.tbterminal.app.ui.dashboard.offline.OfflineDashboardViewModel
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.Ink
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.SurfaceWhite
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong

@Composable
fun OwnerDashboardScreen(
    name: String,
    role: String,
    analyticsRepository: AnalyticsRepository,
    cashReconciliationRepository: CashReconciliationRepository? = null,
    offlineDashboardRepository: OfflineDashboardRepository,
    onLogout: () -> Unit,
    onReportsClick: () -> Unit = {},
    onLocalReportsClick: () -> Unit = onReportsClick,
    onStockReportClick: () -> Unit = {},
    onReceivablesClick: () -> Unit = {},
    onSupplierDebtsClick: () -> Unit = {},
    onCashReconciliationClick: () -> Unit = {},
    onSalesTransactionsClick: () -> Unit = {},
    onNewTransactionClick: () -> Unit = onSalesTransactionsClick,
    onProductsClick: () -> Unit = onStockReportClick,
    onOperationalAuditClick: () -> Unit = {},
    onSyncCenterClick: () -> Unit = {},
    onBackupRestoreClick: () -> Unit = onSyncCenterClick,
    onUserManagementClick: () -> Unit,
    onSecurityLogClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    dashboardViewModel: AdminDashboardViewModel = viewModel(
        factory = AdminDashboardViewModel.factory(
            analyticsRepository = analyticsRepository,
            cashReconciliationRepository = cashReconciliationRepository,
            includeFinancialSummary = true,
        )
    ),
    offlineDashboardViewModel: OfflineDashboardViewModel = viewModel(
        factory = OfflineDashboardViewModel.factory(offlineDashboardRepository)
    )
) {
    val dashboardUiState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val offlineUiState by offlineDashboardViewModel.uiState.collectAsStateWithLifecycle()
    var balanceHidden by rememberSaveable { mutableStateOf(false) }
    var showAccountSheet by rememberSaveable { mutableStateOf(false) }

    val accountContext = rememberOwnerAccountContextState()
    val isSynced = remember(accountContext.isNetworkHealthy, offlineUiState) {
        accountContext.isNetworkHealthy && offlineUiState.failedSyncCount == 0
    }

    val mappedState = remember(name, dashboardUiState, balanceHidden, isSynced) {
        dashboardUiState.toDashboardUiState(
            rawName = name,
            balanceHidden = balanceHidden,
            synced = isSynced
        )
    }

    val destinationNavigator = LocalAdminDestinationNavigator.current
    fun navigateDestination(dest: AdminDestination, fallback: () -> Unit) {
        destinationNavigator?.invoke(dest) ?: fallback()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RefreshableContent(
            isRefreshing = dashboardUiState.isLoading && dashboardUiState.metrics != null,
            onRefresh = dashboardViewModel::refresh,
            modifier = Modifier.fillMaxSize()
        ) {
            DashboardScreen(
                state = mappedState,
                onPeriodChange = { period, customRange ->
                    when (period) {
                        Period.HARI -> dashboardViewModel.setPeriod(1)
                        Period.MINGGU -> dashboardViewModel.setPeriod(7)
                        Period.BULAN -> dashboardViewModel.setPeriod(30)
                        Period.KUSTOM -> {
                            if (customRange != null) {
                                dashboardViewModel.setCustomRange(customRange.first, customRange.second)
                            }
                        }
                    }
                },
                onToggleHidden = { balanceHidden = !balanceHidden },
                onQuickAction = { action ->
                    when (action) {
                        QuickActionType.CATAT_BELI -> onSupplierDebtsClick()
                        QuickActionType.CEK_STOK -> onProductsClick()
                        QuickActionType.LAPORAN -> onReportsClick()
                    }
                },
                onTileClick = { tile ->
                    when (tile) {
                        DashboardTileType.PIUTANG_AKTIF -> onReceivablesClick()
                        DashboardTileType.STOK_KRITIS -> onProductsClick()
                    }
                },
                onSeeAll = onSalesTransactionsClick,
                onNavSelect = { nav ->
                    when (nav) {
                        DashboardNavDestination.BERANDA -> Unit
                        DashboardNavDestination.TRANSAKSI ->
                            navigateDestination(AdminDestination.TransactionsHub, onSalesTransactionsClick)
                        DashboardNavDestination.KEUANGAN ->
                            navigateDestination(AdminDestination.FinanceHub, onReceivablesClick)
                        DashboardNavDestination.STOK ->
                            navigateDestination(AdminDestination.StockHub, onProductsClick)
                        DashboardNavDestination.MENU ->
                            navigateDestination(AdminDestination.MoreHub, onSettingsClick)
                    }
                },
                onProfile = { showAccountSheet = true },
                onDismissError = dashboardViewModel::clearError,
                onRetryError = dashboardViewModel::refresh
            )
        }

        OwnerAccountSheetHost(
            visible = showAccountSheet,
            userName = mappedState.userName,
            role = role,
            offlineStatus = accountContext.offlineStatus,
            devicePreferences = accountContext.devicePreferences,
            onDismiss = { showAccountSheet = false },
            onNavigate = ::navigateDestination,
            onSettingsClick = onSettingsClick,
            onSyncCenterClick = onSyncCenterClick,
            onReportsClick = onReportsClick,
            onLogout = onLogout
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onPeriodChange: (Period, Pair<Long, Long>?) -> Unit,
    onToggleHidden: () -> Unit,
    onQuickAction: (QuickActionType) -> Unit,
    onTileClick: (DashboardTileType) -> Unit,
    onSeeAll: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier,
    onDismissError: () -> Unit = {},
    onRetryError: () -> Unit = {}
) {
    DashboardScreenContent(
        state = state,
        onPeriodChange = onPeriodChange,
        onToggleHidden = onToggleHidden,
        onQuickAction = onQuickAction,
        onTileClick = onTileClick,
        onSeeAll = onSeeAll,
        onNavSelect = onNavSelect,
        onProfile = onProfile,
        modifier = modifier,
        onDismissError = onDismissError,
        onRetryError = onRetryError
    )
}

@Composable
internal fun DashboardScreenContent(
    state: DashboardUiState,
    onPeriodChange: (Period, Pair<Long, Long>?) -> Unit,
    onToggleHidden: () -> Unit,
    onQuickAction: (QuickActionType) -> Unit,
    onTileClick: (DashboardTileType) -> Unit,
    onSeeAll: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    onProfile: () -> Unit,
    modifier: Modifier = Modifier,
    onDismissError: () -> Unit = {},
    onRetryError: () -> Unit = {},
    initialPeriodSelectorExpanded: Boolean = false,
    renderPeriodSelectorInOverlay: Boolean = false
) {
    var showDateRangeDialog by remember { mutableStateOf(false) }
    var periodSelectorExpanded by rememberSaveable { mutableStateOf(initialPeriodSelectorExpanded) }
    var periodSelectorOpenedOnce by remember { mutableStateOf(false) }
    val periodDropdownFocusRequester = remember { FocusRequester() }

    LaunchedEffect(periodSelectorExpanded) {
        if (!periodSelectorExpanded && periodSelectorOpenedOnce) {
            periodSelectorOpenedOnce = false
            runCatching { periodDropdownFocusRequester.requestFocus() }
        }
    }

    val openPeriodSelector: () -> Unit = {
        periodSelectorOpenedOnce = true
        periodSelectorExpanded = true
    }

    val handlePeriodSelect: (Period) -> Unit = { period ->
        periodSelectorExpanded = false
        if (period == Period.KUSTOM) {
            showDateRangeDialog = true
        } else if (period != state.period) {
            onPeriodChange(period, null)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        val isTablet = maxWidth >= 600.dp

        if (isTablet) {
            TabletOwnerDashboardLayout(
                state = state,
                periodDropdownExpanded = periodSelectorExpanded,
                periodDropdownFocusRequester = periodDropdownFocusRequester,
                onOpenPeriodSelector = openPeriodSelector,
                onToggleHidden = onToggleHidden,
                onQuickAction = onQuickAction,
                onTileClick = onTileClick,
                onSeeAll = onSeeAll,
                onNavSelect = onNavSelect,
                onProfile = onProfile
            )
        } else {
            PhoneOwnerDashboardLayout(
                state = state,
                periodDropdownExpanded = periodSelectorExpanded,
                periodDropdownFocusRequester = periodDropdownFocusRequester,
                onOpenPeriodSelector = openPeriodSelector,
                onToggleHidden = onToggleHidden,
                onQuickAction = onQuickAction,
                onTileClick = onTileClick,
                onSeeAll = onSeeAll,
                onNavSelect = onNavSelect,
                onProfile = onProfile
            )
        }

        if (periodSelectorExpanded) {
            if (renderPeriodSelectorInOverlay) {
                OwnerPeriodSelectorFrame(
                    selectedPeriod = state.period,
                    customRange = state.customRange,
                    isTablet = isTablet,
                    onSelect = handlePeriodSelect,
                    onDismiss = { periodSelectorExpanded = false }
                )
            } else {
                OwnerPeriodSelector(
                    selectedPeriod = state.period,
                    customRange = state.customRange,
                    isTablet = isTablet,
                    onSelect = handlePeriodSelect,
                    onDismiss = { periodSelectorExpanded = false }
                )
            }
        }

        AppAlert(
            type = AlertType.Error,
            title = "Gagal memuat data",
            message = state.error.orEmpty(),
            actionLabel = "Coba lagi",
            onAction = onRetryError,
            onDismiss = onDismissError,
            visible = !state.error.isNullOrBlank(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = if (isTablet) 24.dp else 96.dp)
        )

        if (showDateRangeDialog) {
            OwnerCustomDateRangeDialog(
                initialRange = state.customRange,
                onDismiss = { showDateRangeDialog = false },
                onConfirm = { startMillis, endMillis ->
                    showDateRangeDialog = false
                    onPeriodChange(Period.KUSTOM, startMillis to endMillis)
                }
            )
        }
    }
}

@Composable
private fun PhoneOwnerDashboardLayout(
    state: DashboardUiState,
    periodDropdownExpanded: Boolean,
    periodDropdownFocusRequester: FocusRequester,
    onOpenPeriodSelector: () -> Unit,
    onToggleHidden: () -> Unit,
    onQuickAction: (QuickActionType) -> Unit,
    onTileClick: (DashboardTileType) -> Unit,
    onSeeAll: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    onProfile: () -> Unit
) {
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
            // 1. Header hijau (ikut scroll, menyatu dengan panel putih yang menumpuk 28dp ke atas)
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
                    OwnerGreetingRow(
                        userName = state.userName,
                        synced = state.synced,
                        selectedPeriod = state.period,
                        customRange = state.customRange,
                        periodDropdownExpanded = periodDropdownExpanded,
                        onOpenPeriodSelector = onOpenPeriodSelector,
                        periodDropdownFocusRequester = periodDropdownFocusRequester,
                        onProfileClick = onProfile
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OwnerNetSalesLabelRow(
                        balanceHidden = state.balanceHidden,
                        onToggleHidden = onToggleHidden
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (state.isLoading) {
                        Box(
                            modifier = Modifier
                                .width(220.dp)
                                .height(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.20f))
                        )
                    } else {
                        Text(
                            text = if (state.balanceHidden) "Rp ••••••" else formatRupiahSpaced(state.netSales),
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

                    if (!state.isLoading && state.deltaPercent != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OwnerDeltaChip(
                            deltaPercent = state.deltaPercent,
                            period = state.period
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    OwnerHeroSummaryStatsRow(
                        transactionCount = state.transactionCount,
                        estimatedProfit = state.estimatedProfit,
                        cashIn = state.cashIn,
                        balanceHidden = state.balanceHidden,
                        isLoading = state.isLoading,
                        isTablet = false
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
                OwnerQuickActionsRow(
                    isTablet = false,
                    onQuickAction = onQuickAction
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Perlu perhatian",
                    style = ownerTextStyle(
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OwnerAttentionTilesRow(
                    receivables = state.receivables,
                    stockStatus = state.stockStatus,
                    balanceHidden = state.balanceHidden,
                    isLoading = state.isLoading,
                    onTileClick = onTileClick
                )

                Spacer(modifier = Modifier.height(32.dp))

                OwnerSalesTrendChart(
                    chart = state.chart,
                    balanceHidden = state.balanceHidden,
                    isLoading = state.isLoading,
                    onGreenBackground = false
                )

                Spacer(modifier = Modifier.height(32.dp))

                OwnerRecentTransactionsSection(
                    recent = state.recent,
                    isTablet = false,
                    isLoading = state.isLoading,
                    balanceHidden = state.balanceHidden,
                    onSeeAll = onSeeAll
                )
            }
        }

        // 3. Menu bawah (80dp) ditempel ke dasar layar, tidak ikut scroll
        OwnerPhoneBottomNav(
            activeDestination = DashboardNavDestination.BERANDA,
            onNavSelect = onNavSelect,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun TabletOwnerDashboardLayout(
    state: DashboardUiState,
    periodDropdownExpanded: Boolean,
    periodDropdownFocusRequester: FocusRequester,
    onOpenPeriodSelector: () -> Unit,
    onToggleHidden: () -> Unit,
    onQuickAction: (QuickActionType) -> Unit,
    onTileClick: (DashboardTileType) -> Unit,
    onSeeAll: () -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    onProfile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        // 1. Menu samping (rail) lebar 88dp
        OwnerTabletNavRail(
            activeDestination = DashboardNavDestination.BERANDA,
            onNavSelect = onNavSelect
        )

        // 2 & 3. Panel hijau 432dp + Panel putih menumpuk 32dp ke kiri
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // 2. Panel hijau lebar 432dp (isi lebar 352dp: kiri 24dp, kanan 56dp)
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
                    OwnerGreetingRow(
                        userName = state.userName,
                        synced = state.synced,
                        selectedPeriod = state.period,
                        customRange = state.customRange,
                        periodDropdownExpanded = periodDropdownExpanded,
                        onOpenPeriodSelector = onOpenPeriodSelector,
                        periodDropdownFocusRequester = periodDropdownFocusRequester,
                        onProfileClick = onProfile
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OwnerNetSalesLabelRow(
                        balanceHidden = state.balanceHidden,
                        onToggleHidden = onToggleHidden
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (state.isLoading) {
                        Box(
                            modifier = Modifier
                                .width(240.dp)
                                .height(54.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.20f))
                        )
                    } else {
                        Text(
                            text = if (state.balanceHidden) "Rp ••••••" else formatRupiahSpaced(state.netSales),
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

                    if (!state.isLoading && state.deltaPercent != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OwnerDeltaChip(
                            deltaPercent = state.deltaPercent,
                            period = state.period
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    OwnerHeroSummaryStatsRow(
                        transactionCount = state.transactionCount,
                        estimatedProfit = state.estimatedProfit,
                        cashIn = state.cashIn,
                        balanceHidden = state.balanceHidden,
                        isLoading = state.isLoading,
                        isTablet = true
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    OwnerSalesTrendChart(
                        chart = state.chart,
                        balanceHidden = state.balanceHidden,
                        isLoading = state.isLoading,
                        onGreenBackground = true
                    )
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
                    .padding(start = 64.dp, top = 40.dp, end = 32.dp, bottom = 32.dp)
            ) {
                OwnerQuickActionsRow(
                    isTablet = true,
                    onQuickAction = onQuickAction
                )

                Spacer(modifier = Modifier.height(24.dp))

                OwnerAttentionTilesRow(
                    receivables = state.receivables,
                    stockStatus = state.stockStatus,
                    balanceHidden = state.balanceHidden,
                    isLoading = state.isLoading,
                    onTileClick = onTileClick
                )

                Spacer(modifier = Modifier.height(32.dp))

                OwnerRecentTransactionsSection(
                    recent = state.recent,
                    isTablet = true,
                    isLoading = state.isLoading,
                    balanceHidden = state.balanceHidden,
                    onSeeAll = onSeeAll
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OwnerCustomDateRangeDialog(
    initialRange: Pair<Long, Long>?,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit
) {
    val pickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialRange?.first,
        initialSelectedEndDateMillis = initialRange?.second
    )
    val canConfirm = pickerState.selectedStartDateMillis != null && pickerState.selectedEndDateMillis != null

    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = DatePickerDefaults.colors(containerColor = SurfaceWhite),
        confirmButton = {
            TextButton(
                enabled = canConfirm,
                onClick = {
                    val start = pickerState.selectedStartDateMillis
                    val end = pickerState.selectedEndDateMillis
                    if (start != null && end != null) {
                        onConfirm(start, end)
                    }
                }
            ) {
                Text(
                    text = "Terapkan",
                    color = if (canConfirm) Brand else Ink.copy(alpha = 0.38f),
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Batal", color = Ink)
            }
        }
    ) {
        DateRangePicker(
            state = pickerState,
            colors = DatePickerDefaults.colors(
                containerColor = SurfaceWhite,
                selectedDayContainerColor = Brand,
                todayDateBorderColor = Brand,
                dayInSelectionRangeContainerColor = com.tbterminal.app.ui.theme.BrandTint
            )
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// MAPPER FROM AdminDashboardUiState -> DashboardUiState
// ═════════════════════════════════════════════════════════════════════════════
internal fun AdminDashboardUiState.toDashboardUiState(
    rawName: String,
    balanceHidden: Boolean,
    synced: Boolean
): DashboardUiState {
    val displayName = normalizeOwnerDisplayName(rawName)

    val period = when {
        selectedPeriodDays == 0 && customStartDateMillis != null && customEndDateMillis != null -> Period.KUSTOM
        selectedPeriodDays == 7 -> Period.MINGGU
        selectedPeriodDays == 30 -> Period.BULAN
        else -> Period.HARI
    }
    val customRange = if (customStartDateMillis != null && customEndDateMillis != null) {
        customStartDateMillis to customEndDateMillis
    } else {
        null
    }

    val netSales = financialTotals?.netRevenue?.toDouble()
        ?: metrics?.totalRevenueToday
        ?: 0.0
    val prevSales = previousPeriodNetRevenue
    val deltaPercent = if (prevSales != null && prevSales > 0.0) {
        ((netSales - prevSales) / prevSales) * 100.0
    } else {
        null
    }

    val lowCount = metrics?.lowStockCount ?: 0L
    val stockStatus = if (lowCount > 0L) StockStatus.Critical(lowCount) else StockStatus.Safe

    val chartPoints = dailySalesTrend.toChartDayPoints()
    val recentUi = recentTransactions.map { it.toTransactionUi() }

    return DashboardUiState(
        userName = displayName,
        period = period,
        customRange = customRange,
        balanceHidden = balanceHidden,
        netSales = netSales,
        deltaPercent = deltaPercent,
        transactionCount = financialTotals?.transactionCount ?: 0L,
        estimatedProfit = financialTotals?.grossProfit?.toDouble() ?: 0.0,
        cashIn = financialTotals?.paidAmount?.toDouble() ?: 0.0,
        receivables = metrics?.totalActiveReceivables ?: 0.0,
        stockStatus = stockStatus,
        chart = chartPoints,
        recent = recentUi,
        synced = synced,
        isLoading = isLoading && metrics == null,
        error = error
    )
}

private fun List<DailySalesSummaryDto>.toChartDayPoints(): List<DayPoint> {
    if (isEmpty()) return emptyList()
    val lastFive = takeLast(5)
    return lastFive.mapIndexed { index, summary ->
        DayPoint(
            dateLabel = formatChartDateLabel(summary.date),
            revenue = summary.totalRevenue,
            isToday = index == lastFive.lastIndex
        )
    }
}

private fun formatChartDateLabel(isoDate: String): String {
    return runCatching {
        val date = LocalDate.parse(isoDate.take(10))
        "${date.dayOfMonth} ${indonesianShortMonth(date.monthValue)}"
    }.getOrElse { isoDate }
}

private fun CashTransaction.toTransactionUi(): TransactionUi {
    val shortNo = if (receiptId.isNotBlank()) {
        "#${receiptId.takeLast(4).uppercase()}"
    } else {
        "#${id.takeLast(4).uppercase()}"
    }
    val timeText = formatTrxTime(createdAt)
    val rawMethod = paymentMethods.firstOrNull().orEmpty().trim()
    val methodText = when (rawMethod.uppercase()) {
        "QRIS" -> "QRIS"
        "TRANSFER" -> "Transfer"
        "CASH", "TUNAI", "" -> "Tunai"
        else -> rawMethod.lowercase().replaceFirstChar { it.uppercase() }
    }
    return TransactionUi(
        id = id,
        title = "Penjualan $shortNo",
        subtitle = "$timeText · $methodText",
        amount = total.toDouble()
    )
}

private fun formatTrxTime(isoString: String): String {
    return runCatching {
        val instant = Instant.parse(isoString)
        instant.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrElse {
        isoString.substringAfter('T', "").take(5).ifEmpty { "00:00" }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// PREVIEWS (390x1320 PONSEL FULL HEIGHT & 1024x768 TABLET)
// ═════════════════════════════════════════════════════════════════════════════
@Preview(
    name = "Barokah Jaya Dashboard Owner - Ponsel (390x1320)",
    widthDp = 390,
    heightDp = 1320,
    showBackground = true
)
@Composable
fun OwnerDashboardPhoneReferencePreview() {
    val state = sampleReferenceDashboardState(recentCount = 3).copy(
        chart = listOf(
            DayPoint("16 Nov", 1_130_769.0, isToday = false),
            DayPoint("17 Nov", 1_696_154.0, isToday = false),
            DayPoint("18 Nov", 942_308.0, isToday = false),
            DayPoint("19 Nov", 2_073_077.0, isToday = false),
            DayPoint("20 Nov", 2_450_000.0, isToday = true)
        )
    )
    TbterminalappTheme {
        DashboardScreen(
            state = state,
            onPeriodChange = { _, _ -> },
            onToggleHidden = {},
            onQuickAction = {},
            onTileClick = {},
            onSeeAll = {},
            onNavSelect = {},
            onProfile = {}
        )
    }
}

@Preview(
    name = "Barokah Jaya Dashboard Owner - Tablet (1024x768)",
    widthDp = 1024,
    heightDp = 768,
    showBackground = true
)
@Composable
fun OwnerDashboardTabletReferencePreview() {
    val state = sampleReferenceDashboardState(recentCount = 5).copy(
        chart = listOf(
            DayPoint("16 Nov", 1_075_610.0, isToday = false),
            DayPoint("17 Nov", 1_613_415.0, isToday = false),
            DayPoint("18 Nov", 956_098.0, isToday = false),
            DayPoint("19 Nov", 2_031_707.0, isToday = false),
            DayPoint("20 Nov", 2_450_000.0, isToday = true)
        )
    )
    TbterminalappTheme {
        DashboardScreen(
            state = state,
            onPeriodChange = { _, _ -> },
            onToggleHidden = {},
            onQuickAction = {},
            onTileClick = {},
            onSeeAll = {},
            onNavSelect = {},
            onProfile = {}
        )
    }
}

@Composable
internal fun OwnerDashboardPeriodOpenReferenceFrame(isTablet: Boolean) {
    DashboardScreenContent(
        state = sampleReferenceDashboardState(recentCount = if (isTablet) 5 else 3),
        onPeriodChange = { _, _ -> },
        onToggleHidden = {},
        onQuickAction = {},
        onTileClick = {},
        onSeeAll = {},
        onNavSelect = {},
        onProfile = {},
        initialPeriodSelectorExpanded = true,
        renderPeriodSelectorInOverlay = true
    )
}

@Preview(
    name = "Barokah Jaya Dashboard Owner - Filter Periode Terbuka Ponsel (390x844)",
    widthDp = 390,
    heightDp = 844,
    showBackground = true
)
@Composable
fun OwnerDashboardPeriodOpenPhonePreview() {
    TbterminalappTheme {
        OwnerDashboardPeriodOpenReferenceFrame(isTablet = false)
    }
}

@Preview(
    name = "Barokah Jaya Dashboard Owner - Filter Periode Terbuka Tablet (1024x768)",
    widthDp = 1024,
    heightDp = 768,
    showBackground = true
)
@Composable
fun OwnerDashboardPeriodOpenTabletPreview() {
    TbterminalappTheme {
        OwnerDashboardPeriodOpenReferenceFrame(isTablet = true)
    }
}
