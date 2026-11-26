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
import com.tbterminal.app.data.repository.AnalyticsRepository
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.PurchasingRepository
import com.tbterminal.app.data.repository.ReceivableRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.ui.auth.AlertType
import com.tbterminal.app.ui.auth.AppAlert
import com.tbterminal.app.ui.auth.DotsPattern
import com.tbterminal.app.ui.auth.RingsPattern
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator
import com.tbterminal.app.ui.theme.Brand
import com.tbterminal.app.ui.theme.OnBrand
import com.tbterminal.app.ui.theme.SurfaceWhite
import com.tbterminal.app.ui.theme.TbterminalappTheme
import java.math.BigDecimal
import java.time.LocalDate
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Composable
internal fun OwnerKeuanganRoute(
    analyticsRepository: AnalyticsRepository,
    cashReconciliationRepository: CashReconciliationRepository,
    receivableRepository: ReceivableRepository,
    purchasingRepository: PurchasingRepository,
    name: String = "Owner",
    role: String = "owner",
    onLogout: () -> Unit = {},
    onDailyCashClick: () -> Unit,
    onCashExpensesClick: () -> Unit,
    onReceivablesClick: () -> Unit,
    onReceivablePaymentsClick: () -> Unit,
    onSupplierDebtsClick: () -> Unit,
    onReportsClick: () -> Unit,
    onFallbackDashboard: () -> Unit,
    onFallbackTransactions: () -> Unit,
    onFallbackStock: () -> Unit,
    onFallbackMenu: () -> Unit
) {
    var balanceHidden by rememberSaveable { mutableStateOf(false) }
    var showAccountSheet by rememberSaveable { mutableStateOf(false) }
    var refreshTrigger by rememberSaveable { mutableIntStateOf(0) }
    var uiState by remember { mutableStateOf(KeuanganUiState(loading = true)) }
    val accountContext = rememberOwnerAccountContextState()
    val displayName = remember(name) { normalizeOwnerDisplayName(name) }

    LaunchedEffect(refreshTrigger) {
        uiState = uiState.copy(loading = true, error = null)
        val todayStr = LocalDate.now().toString()

        coroutineScope {
            val metricsDeferred = async { runCatching { analyticsRepository.getDashboardMetrics() } }
            val sessionDeferred = async { cashReconciliationRepository.getActiveSession() }
            val expensesDeferred = async {
                cashReconciliationRepository.getExpenseHistory(
                    page = 1,
                    limit = 50,
                    startDate = todayStr,
                    endDate = todayStr
                )
            }
            val receivablesUnpaidDeferred = async {
                receivableRepository.getReceivables(page = 1, limit = 50, status = "unpaid")
            }
            val receivablesPartialDeferred = async {
                receivableRepository.getReceivables(page = 1, limit = 50, status = "partial")
            }
            val payablesUnpaidDeferred = async {
                purchasingRepository.getPayables(page = 1, limit = 50, status = "belum_lunas")
            }
            val payablesPartialDeferred = async {
                purchasingRepository.getPayables(page = 1, limit = 50, status = "sebagian")
            }

            val metricsRes = metricsDeferred.await()
            val sessionRes = sessionDeferred.await()
            val expensesRes = expensesDeferred.await()
            val receivablesUnpaidRes = receivablesUnpaidDeferred.await()
            val receivablesPartialRes = receivablesPartialDeferred.await()
            val payablesUnpaidRes = payablesUnpaidDeferred.await()
            val payablesPartialRes = payablesPartialDeferred.await()

            var loadedBalance = 0.0
            var loadedInflow = 0.0
            var loadedReceivables = 0.0
            var loadedReceivablesCount = 0
            var loadedPayables = 0.0
            var loadedPayablesCount = 0
            var loadedExpenses = 0.0
            var activeSession = false

            metricsRes.getOrNull()?.let { metrics ->
                // Display-only conversion for UI summary state; never used for calculations, storage, or server submission (see docs/BACKLOG.md).
                loadedInflow = metrics.totalRevenueToday.toDouble()
                loadedReceivables = metrics.totalActiveReceivables.toDouble()
                loadedReceivablesCount = metrics.activeReceivableCount.toInt()
            }

            if (sessionRes is RepositoryResult.Success) {
                val active = sessionRes.data
                activeSession = active != null
                if (active != null) {
                    loadedBalance = (active.systemCash ?: active.openingCash).toDouble()
                    val sessionInflow = active.systemCash?.subtract(active.openingCash)?.max(BigDecimal.ZERO)
                    if (sessionInflow != null && sessionInflow > BigDecimal.ZERO) {
                        loadedInflow = sessionInflow.toDouble()
                    }
                } else {
                    when (val historyRes = cashReconciliationRepository.getSessions(page = 1, limit = 1)) {
                        is RepositoryResult.Success -> {
                            val last = historyRes.data.data.firstOrNull()
                            if (last != null) {
                                loadedBalance = (last.closingCash ?: last.systemCash ?: last.openingCash).toDouble()
                            }
                        }
                        else -> Unit
                    }
                }
            }

            if (expensesRes is RepositoryResult.Success) {
                loadedExpenses = expensesRes.data.data.fold(BigDecimal.ZERO) { acc, item ->
                    acc.add(item.amount)
                }.toDouble()
            }

            val receivablePages = listOfNotNull(
                (receivablesUnpaidRes as? RepositoryResult.Success)?.data,
                (receivablesPartialRes as? RepositoryResult.Success)?.data
            )
            if (receivablePages.isNotEmpty()) {
                loadedReceivablesCount = receivablePages.sumOf { it.total.toInt().coerceAtLeast(it.data.size) }
                loadedReceivables = receivablePages.flatMap { it.data }
                    .fold(BigDecimal.ZERO) { acc, item -> acc.add(item.remainingAmount) }
                    .toDouble()
            }

            val payablePages = listOfNotNull(
                (payablesUnpaidRes as? RepositoryResult.Success)?.data,
                (payablesPartialRes as? RepositoryResult.Success)?.data
            )
            if (payablePages.isNotEmpty()) {
                loadedPayablesCount = payablePages.sumOf { it.total.toInt().coerceAtLeast(it.data.size) }
                loadedPayables = payablePages.flatMap { it.data }
                    .fold(BigDecimal.ZERO) { acc, item -> acc.add(item.remainingAmount) }
                    .toDouble()
            }

            val repoErrors = listOf(
                sessionRes,
                expensesRes,
                receivablesUnpaidRes,
                receivablesPartialRes,
                payablesUnpaidRes,
                payablesPartialRes
            )
            val firstError = metricsRes.exceptionOrNull()?.message
                ?: repoErrors.filterIsInstance<RepositoryResult.Error>().firstOrNull()?.message
                ?: repoErrors.filterIsInstance<RepositoryResult.Exception>().firstOrNull()
                    ?.let { "Gagal memuat data keuangan." }

            uiState = uiState.copy(
                lastCashBalance = loadedBalance,
                hasActiveSession = activeSession,
                todayCashInflow = loadedInflow,
                activeReceivables = loadedReceivables,
                receivableNotesCount = loadedReceivablesCount,
                duePayables = loadedPayables,
                payableNotesCount = loadedPayablesCount,
                todayExpensesTotal = loadedExpenses,
                loading = false,
                error = firstError
            )
        }
    }

    val destinationNavigator = LocalAdminDestinationNavigator.current
    fun navigateDest(dest: AdminDestination, fallback: () -> Unit) {
        destinationNavigator?.invoke(dest) ?: fallback()
    }

    val handleAction: (KeuanganActionId) -> Unit = { action ->
        when (action) {
            KeuanganActionId.DAILY_CASH -> navigateDest(AdminDestination.CashReconciliation, onDailyCashClick)
            KeuanganActionId.CASH_EXPENSES -> navigateDest(AdminDestination.CashExpenses, onCashExpensesClick)
            KeuanganActionId.RECEIVABLES -> navigateDest(AdminDestination.Receivables, onReceivablesClick)
            KeuanganActionId.RECEIVABLE_PAYMENTS ->
                navigateDest(AdminDestination.ReceivablePayments, onReceivablePaymentsClick)
            KeuanganActionId.SUPPLIER_DEBTS -> navigateDest(AdminDestination.SupplierDebts, onSupplierDebtsClick)
            KeuanganActionId.FINANCE_REPORTS -> navigateDest(AdminDestination.Reports, onReportsClick)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        RefreshableContent(
            isRefreshing = uiState.loading,
            onRefresh = { refreshTrigger++ },
            modifier = Modifier.fillMaxSize()
        ) {
            KeuanganScreen(
                state = uiState.copy(
                    userName = displayName,
                    synced = accountContext.isSynced,
                    balanceHidden = balanceHidden
                ),
                onToggleHidden = { balanceHidden = !balanceHidden },
                onActionClick = handleAction,
                onNavSelect = { nav ->
                    when (nav) {
                        DashboardNavDestination.BERANDA -> navigateDest(AdminDestination.Dashboard, onFallbackDashboard)
                        DashboardNavDestination.TRANSAKSI ->
                            navigateDest(AdminDestination.TransactionsHub, onFallbackTransactions)
                        DashboardNavDestination.KEUANGAN -> Unit
                        DashboardNavDestination.STOK -> navigateDest(AdminDestination.StockHub, onFallbackStock)
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
            onReportsClick = onReportsClick,
            onLogout = onLogout
        )
    }
}

@Composable
fun KeuanganScreen(
    state: KeuanganUiState,
    onToggleHidden: () -> Unit,
    onActionClick: (KeuanganActionId) -> Unit,
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
            TabletKeuanganLayout(
                state = state,
                onToggleHidden = onToggleHidden,
                onActionClick = onActionClick,
                onNavSelect = onNavSelect,
                onProfile = onProfile
            )
        } else {
            PhoneKeuanganLayout(
                state = state,
                onToggleHidden = onToggleHidden,
                onActionClick = onActionClick,
                onNavSelect = onNavSelect,
                onProfile = onProfile
            )
        }

        AppAlert(
            type = AlertType.Error,
            title = "Gagal memuat keuangan",
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
private fun PhoneKeuanganLayout(
    state: KeuanganUiState,
    onToggleHidden: () -> Unit,
    onActionClick: (KeuanganActionId) -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    onProfile: () -> Unit
) {
    val groups = remember(
        state.hasActiveSession,
        state.todayExpensesTotal,
        state.receivableNotesCount,
        state.activeReceivables,
        state.payableNotesCount,
        state.duePayables,
        state.balanceHidden
    ) { buildKeuanganMenuGroups(state) }

    val balanceLabel = if (state.hasActiveSession) "Saldo kas aktif" else "Saldo kas sesi terakhir"
    val statusChipLabel = if (state.hasActiveSession) "Kas aktif" else "Kas siap"

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
                        title = "Keuangan",
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
                            label = balanceLabel,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        TransaksiPeriodStatusChip(label = statusChipLabel)
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
                            text = if (state.balanceHidden) "Rp ••••••" else formatRupiahSpaced(state.lastCashBalance),
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
                                .testTag("keuangan-hero-balance")
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    KeuanganHeroStatsRow(
                        state = state,
                        onReceivablesClick = { onActionClick(KeuanganActionId.RECEIVABLES) },
                        onPayablesClick = { onActionClick(KeuanganActionId.SUPPLIER_DEBTS) }
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
                groups.forEachIndexed { index, group ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                    KeuanganMenuSection(
                        group = group,
                        onActionClick = onActionClick
                    )
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
private fun TabletKeuanganLayout(
    state: KeuanganUiState,
    onToggleHidden: () -> Unit,
    onActionClick: (KeuanganActionId) -> Unit,
    onNavSelect: (DashboardNavDestination) -> Unit,
    onProfile: () -> Unit
) {
    val groups = remember(
        state.hasActiveSession,
        state.todayExpensesTotal,
        state.receivableNotesCount,
        state.activeReceivables,
        state.payableNotesCount,
        state.duePayables,
        state.balanceHidden
    ) { buildKeuanganMenuGroups(state) }

    val balanceLabel = if (state.hasActiveSession) "Saldo kas aktif" else "Saldo kas sesi terakhir"
    val statusChipLabel = if (state.hasActiveSession) "Kas aktif" else "Kas siap"

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
                        title = "Keuangan",
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
                            label = balanceLabel,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        TransaksiPeriodStatusChip(label = statusChipLabel)
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
                            text = if (state.balanceHidden) "Rp ••••••" else formatRupiahSpaced(state.lastCashBalance),
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
                                .testTag("keuangan-hero-balance")
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    KeuanganHeroStatsRow(
                        state = state,
                        onReceivablesClick = { onActionClick(KeuanganActionId.RECEIVABLES) },
                        onPayablesClick = { onActionClick(KeuanganActionId.SUPPLIER_DEBTS) }
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
                    KeuanganMenuSection(
                        group = group,
                        onActionClick = onActionClick
                    )
                }
            }
        }
    }
}

@Preview(
    name = "Barokah Jaya Keuangan - Ponsel (390x1030)",
    widthDp = 390,
    heightDp = 1030,
    showBackground = true
)
@Composable
fun OwnerKeuanganPhonePreview() {
    TbterminalappTheme {
        KeuanganScreen(
            state = sampleReferenceKeuanganState(),
            onToggleHidden = {},
            onActionClick = {},
            onNavSelect = {}
        )
    }
}

@Preview(
    name = "Barokah Jaya Keuangan - Tablet (1024x768)",
    widthDp = 1024,
    heightDp = 768,
    showBackground = true
)
@Composable
fun OwnerKeuanganTabletPreview() {
    TbterminalappTheme {
        KeuanganScreen(
            state = sampleReferenceKeuanganState(),
            onToggleHidden = {},
            onActionClick = {},
            onNavSelect = {}
        )
    }
}
