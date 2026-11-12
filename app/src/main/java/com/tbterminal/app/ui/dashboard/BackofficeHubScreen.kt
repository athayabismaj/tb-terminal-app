package com.tbterminal.app.ui.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.PriceChange
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.data.costing.InventoryValuationSummary
import com.tbterminal.app.data.costing.MovingAverageCalculator
import com.tbterminal.app.data.repository.AnalyticsRepository
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.data.repository.PurchasingRepository
import com.tbterminal.app.data.repository.ReceivableRepository
import com.tbterminal.app.data.repository.RepositoryResult
import com.tbterminal.app.data.repository.SyncMonitoringRepository
import com.tbterminal.app.navigation.AppAccessPolicy
import com.tbterminal.app.navigation.AppCapability
import com.tbterminal.app.navigation.LocalPendingProductSearch
import com.tbterminal.app.ui.barcode.CameraBarcodeScannerDialog
import com.tbterminal.app.ui.components.SkeletonBox
import com.tbterminal.app.ui.components.TbAdaptiveBox
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BackofficeHubScreen(
    name: String,
    role: String,
    section: BackofficeSection,
    inventoryRepository: InventoryRepository? = null,
    cashReconciliationRepository: CashReconciliationRepository? = null,
    receivableRepository: ReceivableRepository? = null,
    purchasingRepository: PurchasingRepository? = null,
    analyticsRepository: AnalyticsRepository? = null,
    syncMonitoringRepository: SyncMonitoringRepository? = null,
    onNavigate: ((AdminDestination) -> Unit)? = null,
    onLogout: () -> Unit,
    onUserManagementClick: () -> Unit = {},
    onSecurityLogClick: () -> Unit = {},
) {
    val globalNavigator = LocalAdminDestinationNavigator.current
    val pendingProductSearch = LocalPendingProductSearch.current
    val navigate: (AdminDestination) -> Unit = onNavigate ?: { destination -> globalNavigator?.invoke(destination) }
    var showStockBarcodeScanner by remember { mutableStateOf(false) }
    val canViewReportsAnalytics = AppAccessPolicy.can(role, AppCapability.FINANCIAL_ANALYTICS)
    val activeDestination = when (section) {
        BackofficeSection.TRANSACTIONS -> AdminDestination.TransactionsHub
        BackofficeSection.FINANCE -> AdminDestination.FinanceHub
        BackofficeSection.STOCK -> AdminDestination.StockHub
        BackofficeSection.MORE, BackofficeSection.MENU -> AdminDestination.MoreHub
        BackofficeSection.HOME -> AdminDestination.Dashboard
    }

    if (showStockBarcodeScanner) {
        CameraBarcodeScannerDialog(
            onDismissRequest = { showStockBarcodeScanner = false },
            onBarcodeScanned = { scannedCode ->
                showStockBarcodeScanner = false
                pendingProductSearch.value = scannedCode.trim().takeIf(String::isNotBlank)
                navigate(AdminDestination.Products)
            }
        )
    }

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = activeDestination,
        onDashboardClick = { navigate(AdminDestination.Dashboard) },
        onProductsClick = { navigate(AdminDestination.Products) },
        onProfileClick = { navigate(AdminDestination.Profile) },
        onSettingsClick = { navigate(AdminDestination.Settings) },
        topBarActions = {
            when (section) {
                BackofficeSection.FINANCE -> {
                    HubTopBarIconButton(
                        icon = Icons.Outlined.Search,
                        contentDescription = "Cari transaksi",
                        onClick = { navigate(AdminDestination.SalesTransactions) },
                        testTag = "hub-finance-search",
                    )
                    if (canViewReportsAnalytics) {
                        HubTopBarIconButton(
                            icon = Icons.Outlined.Assessment,
                            contentDescription = "Analitik & Laporan",
                            onClick = { navigate(AdminDestination.Reports) },
                            testTag = "hub-finance-reports",
                        )
                    }
                }
                BackofficeSection.STOCK -> {
                    HubTopBarIconButton(
                        icon = Icons.Outlined.Search,
                        contentDescription = "Cari produk",
                        onClick = { navigate(AdminDestination.Products) },
                        testTag = "hub-stock-search",
                    )
                    HubTopBarIconButton(
                        icon = Icons.Outlined.QrCodeScanner,
                        contentDescription = "Scan barcode",
                        onClick = { showStockBarcodeScanner = true },
                        testTag = "hub-stock-scan",
                    )
                }
                else -> Unit
            }
        },
        onLogout = onLogout,
    ) { contentModifier ->
        TbAdaptiveBox(
            modifier = contentModifier
                .fillMaxSize()
                .background(HubCanvasBg),
        ) { layout ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                val containerModifier = if (layout.isCompact) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = 1120.dp)
                        .padding(horizontal = layout.horizontalPadding, vertical = layout.verticalSpacing)
                }
                Column(
                    modifier = containerModifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(if (layout.isCompact) 16.dp else 20.dp),
                ) {
                    when (section) {
                        BackofficeSection.TRANSACTIONS -> TransactionsHub(
                            role = role,
                            compact = layout.isCompact,
                            analyticsRepository = analyticsRepository,
                            cashReconciliationRepository = cashReconciliationRepository,
                            purchasingRepository = purchasingRepository,
                            onNavigate = navigate,
                        )
                        BackofficeSection.FINANCE -> FinanceHub(
                            role = role,
                            content = financeHubContent(role),
                            compact = layout.isCompact,
                            cashReconciliationRepository = cashReconciliationRepository,
                            receivableRepository = receivableRepository,
                            purchasingRepository = purchasingRepository,
                            analyticsRepository = analyticsRepository,
                            onNavigate = navigate,
                        )
                        BackofficeSection.STOCK -> StockHub(
                            role = role,
                            content = stockHubContent(role),
                            compact = layout.isCompact,
                            inventoryRepository = inventoryRepository,
                            syncMonitoringRepository = syncMonitoringRepository,
                            onNavigate = navigate,
                        )
                        BackofficeSection.MORE -> BackofficeMoreContent(
                            layout = layout,
                            role = role,
                            onNavigate = navigate,
                            onUserManagementClick = onUserManagementClick,
                            onSecurityLogClick = onSecurityLogClick,
                            onLogout = onLogout,
                        )
                        else -> Unit
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// ── COLOR PALETTE: REFINED INDUSTRIAL STANDARD ──
private val HubCanvasBg = Color(0xFFF2F4F7)
private val HubSlate900 = Color(0xFF0F172A)
private val HubSlate800 = Color(0xFF1E293B)
private val HubSlate500 = Color(0xFF64748B)
private val HubSlate400 = Color(0xFF94A3B8)
private val HubSlate100 = Color(0xFFF1F5F9)
private val HubCardBorder = Color(0xFFE2E8F0)
private val HubAmberBg = Color(0xFFFEF3C7)
private val HubAmberText = Color(0xFFB45309)

private val HubKasiraBrand = Color(0xFF10533C)
private val HubEmerald50 = Color(0xFFECFDF5)
private val HubEmerald600 = Color(0xFF059669)
private val HubEmerald700 = Color(0xFF047857)
private val HubAmberSoftBg = Color(0xFFFFFBEB)
private val HubSlate600 = Color(0xFF475569)
private val HubSlate700 = Color(0xFF334155)
private val HubPillTrackBg = Color(0xFFE5E9F0)
private val HubMotionEaseOut = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

private const val TX_STEP_TAB = 0
private const val TX_STEP_SUMMARY = 1
private const val TX_STEP_CTA = 2
private const val TX_STEP_LIST = 3

internal class StaggeredEntranceState(
    private val steps: List<Animatable<Float, *>>,
) {
    fun progress(index: Int): Float = steps.getOrNull(index)?.value ?: 1f
}

@Composable
internal fun rememberStaggeredEntrance(stepCount: Int): StaggeredEntranceState {
    val steps = remember(stepCount) { List(stepCount.coerceAtLeast(1)) { Animatable(0f) } }
    LaunchedEffect(steps) {
        coroutineScope {
            steps.forEachIndexed { index, anim ->
                launch {
                    delay(index * 45L)
                    anim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 320,
                            easing = HubMotionEaseOut,
                        ),
                    )
                }
            }
        }
    }
    return remember(steps) { StaggeredEntranceState(steps) }
}

internal fun Modifier.staggeredEntrance(
    slideDistancePx: Float,
    progress: () -> Float,
): Modifier = graphicsLayer {
    val current = progress()
    alpha = current
    translationY = (1f - current) * slideDistancePx
}

@Composable
private fun AnimatedNominalValue(
    text: String,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            (
                fadeIn(tween(durationMillis = 180)) +
                    slideInVertically(tween(durationMillis = 180)) { it / 4 }
                ) togetherWith (
                fadeOut(tween(durationMillis = 120)) +
                    slideOutVertically(tween(durationMillis = 120)) { -it / 4 }
                )
        },
        modifier = modifier,
        label = "HubNominalDisplay",
    ) { displayValue ->
        Text(
            text = displayValue,
            color = HubSlate900,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class HubActionBadge(
    val text: String,
    val dotColor: Color? = null,
    val bgColor: Color? = null,
    val textColor: Color? = null,
)

private fun formatIdCurrency(amount: BigDecimal): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).apply {
        maximumFractionDigits = 0
    }.format(amount).replace(" ", "")

private fun formatShortCurrency(amount: BigDecimal): String {
    val value = amount.toDouble()
    return when {
        value >= 1_000_000_000 -> String.format(Locale.US, "Rp%.1f M", value / 1_000_000_000).replace(".", ",")
        value >= 1_000_000 -> String.format(Locale.US, "Rp%.1f Jt", value / 1_000_000).replace(".", ",")
        value >= 1_000 -> String.format(Locale.US, "Rp%.0f Rb", value / 1_000)
        else -> "Rp" + value.toLong().toString()
    }
}

@Composable
private fun FinanceHub(
    role: String,
    content: BackofficeHubContent,
    compact: Boolean,
    cashReconciliationRepository: CashReconciliationRepository? = null,
    receivableRepository: ReceivableRepository? = null,
    purchasingRepository: PurchasingRepository? = null,
    analyticsRepository: AnalyticsRepository? = null,
    onNavigate: (AdminDestination) -> Unit,
) {
    var isNominalHidden by rememberSaveable { mutableStateOf(false) }
    var totalCashBalance by remember { mutableStateOf(BigDecimal.ZERO) }
    var todayCashInflow by remember { mutableStateOf(BigDecimal.ZERO) }
    var totalReceivables by remember { mutableStateOf(BigDecimal.ZERO) }
    var receivableNotesCount by remember { mutableIntStateOf(0) }
    var totalPayables by remember { mutableStateOf(BigDecimal.ZERO) }
    var payableNotesCount by remember { mutableIntStateOf(0) }
    var todayExpensesTotal by remember { mutableStateOf(BigDecimal.ZERO) }
    var todayExpensesCount by remember { mutableIntStateOf(0) }
    var hasActiveSession by remember { mutableStateOf(false) }
    val hasAnyRepo = cashReconciliationRepository != null ||
        receivableRepository != null ||
        purchasingRepository != null ||
        analyticsRepository != null
    var isLoading by remember { mutableStateOf(hasAnyRepo) }

    val entrance = rememberStaggeredEntrance(stepCount = 1 + content.groups.size)

    LaunchedEffect(cashReconciliationRepository, receivableRepository, purchasingRepository, analyticsRepository) {
        isLoading = hasAnyRepo
        try {
            var loadedBalance = BigDecimal.ZERO
            var loadedInflow = BigDecimal.ZERO
            var loadedReceivables = BigDecimal.ZERO
            var loadedReceivablesCount = 0
            var loadedPayables = BigDecimal.ZERO
            var loadedPayablesCount = 0
            var loadedExpensesTotal = BigDecimal.ZERO
            var loadedExpensesCount = 0

            if (analyticsRepository != null) {
                runCatching {
                    val metrics = analyticsRepository.getDashboardMetrics()
                    loadedInflow = BigDecimal.valueOf(metrics.totalRevenueToday)
                    loadedReceivables = BigDecimal.valueOf(metrics.totalActiveReceivables)
                    loadedReceivablesCount = metrics.activeReceivableCount.toInt()
                }
            }

            if (cashReconciliationRepository != null) {
                runCatching {
                    when (val sessionRes = cashReconciliationRepository.getActiveSession()) {
                        is RepositoryResult.Success -> {
                            val activeSession = sessionRes.data
                            hasActiveSession = activeSession != null
                            if (activeSession != null) {
                                loadedBalance = activeSession.systemCash ?: activeSession.openingCash
                                val sessionInflow = activeSession.systemCash?.subtract(activeSession.openingCash)?.max(BigDecimal.ZERO)
                                if (sessionInflow != null && sessionInflow > BigDecimal.ZERO) {
                                    loadedInflow = sessionInflow
                                }
                            } else {
                                when (val lastSessions = cashReconciliationRepository.getSessions(page = 1, limit = 1)) {
                                    is RepositoryResult.Success -> {
                                        val last = lastSessions.data.data.firstOrNull()
                                        if (last != null) {
                                            loadedBalance = last.closingCash ?: last.systemCash ?: last.openingCash
                                        }
                                    }
                                    else -> Unit
                                }
                            }
                        }
                        else -> Unit
                    }
                }

                runCatching {
                    val todayStr = LocalDate.now().toString()
                    when (
                        val expRes = cashReconciliationRepository.getExpenseHistory(
                            page = 1,
                            limit = 50,
                            startDate = todayStr,
                            endDate = todayStr,
                        )
                    ) {
                        is RepositoryResult.Success -> {
                            val page = expRes.data
                            loadedExpensesCount = page.total.toInt().coerceAtLeast(page.data.size)
                            loadedExpensesTotal = page.data.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.amount) }
                        }
                        else -> Unit
                    }
                }
            }

            if (receivableRepository != null) {
                runCatching {
                    when (val recRes = receivableRepository.getReceivables(page = 1, limit = 50, status = "unpaid")) {
                        is RepositoryResult.Success -> {
                            val page = recRes.data
                            loadedReceivablesCount = page.total.toInt().coerceAtLeast(page.data.size)
                            loadedReceivables = page.data.fold(BigDecimal.ZERO) { acc, r -> acc.add(r.remainingAmount) }
                        }
                        else -> Unit
                    }
                }
            }

            if (purchasingRepository != null) {
                runCatching {
                    when (val payRes = purchasingRepository.getPayables(page = 1, limit = 50, status = "unpaid")) {
                        is RepositoryResult.Success -> {
                            val page = payRes.data
                            loadedPayablesCount = page.total.toInt().coerceAtLeast(page.data.size)
                            loadedPayables = page.data.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.remainingAmount) }
                        }
                        else -> Unit
                    }
                }
            }

            totalCashBalance = loadedBalance
            todayCashInflow = loadedInflow
            totalReceivables = loadedReceivables
            receivableNotesCount = loadedReceivablesCount
            totalPayables = loadedPayables
            payableNotesCount = loadedPayablesCount
            todayExpensesTotal = loadedExpensesTotal
            todayExpensesCount = loadedExpensesCount
        } finally {
            isLoading = false
        }
    }

    val density = LocalDensity.current
    val heroSlidePx = remember(density) { with(density) { 14.dp.toPx() } }
    val groupBaseSlidePx = remember(density) { with(density) { 18.dp.toPx() } }
    val groupStepSlidePx = remember(density) { with(density) { 4.dp.toPx() } }
    val horizontalInsetModifier = if (compact) Modifier.padding(horizontal = 16.dp) else Modifier

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("finance-hub-content")
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 18.dp),
    ) {
        Box(
            modifier = if (compact) {
                Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
            } else {
                Modifier
            },
        ) {
            HeroFinancialCard(
                totalCashBalance = totalCashBalance,
                todayCashInflow = todayCashInflow,
                totalReceivable = totalReceivables,
                totalPayable = totalPayables,
                receivableNotesCount = receivableNotesCount,
                payableNotesCount = payableNotesCount,
                hasActiveSession = hasActiveSession,
                isNominalHidden = isNominalHidden,
                isLoading = isLoading,
                onToggleNominal = { isNominalHidden = !isNominalHidden },
                onReceivablesClick = { onNavigate(AdminDestination.Receivables) },
                onPayablesClick = { onNavigate(AdminDestination.SupplierDebts) },
                modifier = Modifier.staggeredEntrance(heroSlidePx) { entrance.progress(0) },
            )
        }

        content.groups.forEachIndexed { index, group ->
            HubActivityGroupSection(
                title = stringResource(group.titleRes).uppercase(Locale.ROOT),
                actions = group.actions,
                badgeProvider = { action ->
                    when (action) {
                        BackofficeHubAction.DAILY_CASH -> HubActionBadge(
                            text = if (hasActiveSession) "Sesi Aktif" else "Sesi Ditutup",
                            dotColor = if (hasActiveSession) HubEmerald600 else HubSlate400,
                            bgColor = if (hasActiveSession) HubEmerald50 else HubSlate100,
                            textColor = if (hasActiveSession) HubEmerald700 else HubSlate500,
                        )
                        BackofficeHubAction.CASH_EXPENSES -> HubActionBadge(
                            text = when {
                                isNominalHidden && todayExpensesCount > 0 -> "$todayExpensesCount Beban · Rp•••"
                                isNominalHidden -> "Tersembunyi"
                                todayExpensesCount > 0 -> "$todayExpensesCount Beban · ${formatShortCurrency(todayExpensesTotal)}"
                                else -> "Hari Ini Rp0"
                            },
                            dotColor = if (todayExpensesCount > 0) Color(0xFFD97706) else HubSlate400,
                            bgColor = if (todayExpensesCount > 0) HubAmberBg else HubSlate100,
                            textColor = if (todayExpensesCount > 0) HubAmberText else HubSlate500,
                        )
                        BackofficeHubAction.RECEIVABLES -> HubActionBadge(
                            text = "$receivableNotesCount Nota · ${if (isNominalHidden) "Rp•••" else formatShortCurrency(totalReceivables)}",
                            dotColor = if (receivableNotesCount > 0) HubEmerald600 else HubSlate400,
                            bgColor = if (receivableNotesCount > 0) HubEmerald50 else HubSlate100,
                            textColor = if (receivableNotesCount > 0) HubEmerald700 else HubSlate500,
                        )
                        BackofficeHubAction.SUPPLIER_DEBTS -> HubActionBadge(
                            text = "$payableNotesCount Nota · ${if (isNominalHidden) "Rp•••" else formatShortCurrency(totalPayables)}",
                            dotColor = if (payableNotesCount > 0) Color(0xFFD97706) else HubSlate400,
                            bgColor = if (payableNotesCount > 0) HubAmberBg else HubSlate100,
                            textColor = if (payableNotesCount > 0) HubAmberText else HubSlate500,
                        )
                        else -> null
                    }
                },
                iconColorsProvider = { action ->
                    when (action) {
                        BackofficeHubAction.CASH_EXPENSES,
                        BackofficeHubAction.SUPPLIER_DEBTS -> HubAmberSoftBg to HubAmberText
                        else -> HubEmerald50 to HubEmerald700
                    }
                },
                onActionClick = { action -> onNavigate(action.destination) },
                modifier = horizontalInsetModifier.staggeredEntrance(
                    groupBaseSlidePx + index * groupStepSlidePx,
                ) {
                    entrance.progress(1 + index)
                },
            )
        }
    }
}

@Composable
private fun HeroFinancialCard(
    totalCashBalance: BigDecimal,
    todayCashInflow: BigDecimal,
    totalReceivable: BigDecimal,
    totalPayable: BigDecimal,
    receivableNotesCount: Int,
    payableNotesCount: Int,
    hasActiveSession: Boolean,
    isNominalHidden: Boolean,
    isLoading: Boolean,
    onToggleNominal: () -> Unit,
    onReceivablesClick: () -> Unit,
    onPayablesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusText = when {
        hasActiveSession -> "Shift Aktif"
        payableNotesCount > 0 -> "$payableNotesCount Hutang"
        receivableNotesCount > 0 -> "$receivableNotesCount Piutang"
        else -> "Kas Siap"
    }
    val statusDotColor = if (!hasActiveSession && payableNotesCount > 0) {
        Color(0xFFD97706)
    } else {
        HubEmerald600
    }
    val statusTextColor = if (!hasActiveSession && payableNotesCount > 0) {
        HubAmberText
    } else {
        HubSlate500
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HubCardBorder.copy(alpha = 0.75f)),
        shadowElevation = 0.5.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                ) {
                    Text(
                        text = if (hasActiveSession) "Saldo Kas Laci (Shift Aktif)" else "Saldo Kas Sesi Terakhir",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HubSlate500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    IconButton(
                        onClick = onToggleNominal,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("finance-toggle-nominal"),
                    ) {
                        Icon(
                            imageVector = if (isNominalHidden) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = if (isNominalHidden) {
                                "Tampilkan nominal keuangan"
                            } else {
                                "Sembunyikan nominal keuangan"
                            },
                            tint = HubSlate500,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusDotColor),
                    )
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusTextColor,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (isLoading && !isNominalHidden) {
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(28.dp),
                )
            } else {
                AnimatedNominalValue(
                    text = if (isNominalHidden) "Rp••••••" else formatIdCurrency(totalCashBalance),
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = HubSlate100, thickness = 1.dp)
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeroFinanceMetricItem(
                    label = "Kas Hari Ini",
                    value = when {
                        isLoading && !isNominalHidden -> "—"
                        isNominalHidden -> "Rp••••"
                        else -> "+${formatIdCurrency(todayCashInflow)}"
                    },
                    valueColor = HubEmerald700,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp),
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(HubSlate100),
                )
                HeroFinanceMetricItem(
                    label = "Piutang Aktif",
                    value = when {
                        isLoading && !isNominalHidden -> "—"
                        isNominalHidden -> "Rp••••"
                        else -> formatIdCurrency(totalReceivable)
                    },
                    valueColor = HubSlate800,
                    onClick = onReceivablesClick,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(HubSlate100),
                )
                HeroFinanceMetricItem(
                    label = "Hutang Tempo",
                    value = when {
                        isLoading && !isNominalHidden -> "—"
                        isNominalHidden -> "Rp••••"
                        else -> formatIdCurrency(totalPayable)
                    },
                    valueColor = if (totalPayable > BigDecimal.ZERO) HubAmberText else HubSlate800,
                    onClick = onPayablesClick,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun HeroFinanceMetricItem(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(vertical = 2.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = HubSlate500,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun HubTopBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = Modifier
            .size(32.dp)
            .testTag(testTag),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color(0xFF475569),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun StockHub(
    role: String,
    content: BackofficeHubContent,
    compact: Boolean,
    inventoryRepository: InventoryRepository? = null,
    syncMonitoringRepository: SyncMonitoringRepository? = null,
    onNavigate: (AdminDestination) -> Unit,
) {
    val canViewFinancialValuation = AppAccessPolicy.can(role, AppCapability.FINANCIAL_ANALYTICS)
    var isNominalHidden by rememberSaveable { mutableStateOf(false) }
    var valuationSummary by remember { mutableStateOf<InventoryValuationSummary?>(null) }
    var lowStockCount by remember { mutableIntStateOf(0) }
    var totalProductsCount by remember { mutableIntStateOf(0) }
    var totalCategoriesCount by remember { mutableIntStateOf(0) }
    var totalUnitsCount by remember { mutableIntStateOf(0) }
    var isPartialStockLoad by remember { mutableStateOf(false) }
    var syncStatusText by remember { mutableStateOf("Tersinkron") }
    var isSyncHealthy by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(inventoryRepository != null) }

    val ctaOffset = if (content.primaryAction != null) 1 else 0
    val entrance = rememberStaggeredEntrance(stepCount = 1 + ctaOffset + content.groups.size)

    LaunchedEffect(inventoryRepository) {
        if (inventoryRepository != null) {
            isLoading = true
            try {
                runCatching {
                    when (val aggregateResult = loadAllProductStocks(inventoryRepository)) {
                        is RepositoryResult.Success -> {
                            val aggregate = aggregateResult.data
                            val products = aggregate.products
                            valuationSummary = MovingAverageCalculator.calculateInventoryValuation(products)
                            lowStockCount = products.count { it.isActive && it.quantity <= it.minStock }
                            totalProductsCount = aggregate.totalProducts
                            isPartialStockLoad = aggregate.isPartial
                        }
                        else -> Unit
                    }
                }
                runCatching {
                    when (val catRes = inventoryRepository.getCategories()) {
                        is RepositoryResult.Success -> totalCategoriesCount = catRes.data.size
                        else -> Unit
                    }
                }
                runCatching {
                    when (val unitRes = inventoryRepository.getUnits()) {
                        is RepositoryResult.Success -> totalUnitsCount = unitRes.data.size
                        else -> Unit
                    }
                }
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(syncMonitoringRepository) {
        syncMonitoringRepository?.let { repo ->
            repo.observeSnapshot().collect { snapshot ->
                when {
                    snapshot.totalFailed > 0 -> {
                        syncStatusText = "${snapshot.totalFailed} Gagal"
                        isSyncHealthy = false
                    }
                    snapshot.totalPending > 0 || snapshot.totalSyncing > 0 -> {
                        val pendingCount = snapshot.totalPending + snapshot.totalSyncing
                        syncStatusText = "$pendingCount Menunggu"
                        isSyncHealthy = false
                    }
                    else -> {
                        syncStatusText = "Tersinkron"
                        isSyncHealthy = true
                    }
                }
            }
        }
    }

    val density = LocalDensity.current
    val heroSlidePx = remember(density) { with(density) { 14.dp.toPx() } }
    val ctaSlidePx = remember(density) { with(density) { 18.dp.toPx() } }
    val groupBaseSlidePx = remember(density) { with(density) { 20.dp.toPx() } }
    val groupStepSlidePx = remember(density) { with(density) { 4.dp.toPx() } }
    val horizontalInsetModifier = if (compact) Modifier.padding(horizontal = 16.dp) else Modifier

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stock-hub-content")
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 18.dp),
    ) {
        Column(
            modifier = if (compact) {
                Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
            } else {
                Modifier
            },
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            StockValuationDashboardCard(
                showValuationFinancials = canViewFinancialValuation,
                valuationSummary = valuationSummary,
                totalProductsCount = totalProductsCount,
                lowStockCount = lowStockCount,
                isPartialStockLoad = isPartialStockLoad,
                syncStatusText = syncStatusText,
                isSyncHealthy = isSyncHealthy,
                isNominalHidden = isNominalHidden,
                isLoading = isLoading,
                onToggleNominal = { isNominalHidden = !isNominalHidden },
                onLowStockClick = { onNavigate(AdminDestination.Products) },
                modifier = Modifier.staggeredEntrance(heroSlidePx) { entrance.progress(0) },
            )

            content.primaryAction?.let { primaryAction ->
                StockQuickActionStrip(
                    primaryAction = primaryAction,
                    onActionClick = { action -> onNavigate(action.destination) },
                    modifier = Modifier.staggeredEntrance(ctaSlidePx) { entrance.progress(1) },
                )
            }
        }

        content.groups.forEachIndexed { index, group ->
            HubActivityGroupSection(
                title = stringResource(group.titleRes).uppercase(Locale.ROOT),
                actions = group.actions,
                titleProvider = { action ->
                    if (action == BackofficeHubAction.PRODUCT_PRICES && canViewFinancialValuation) {
                        "Harga & Margin"
                    } else {
                        stringResource(action.titleRes)
                    }
                },
                badgeProvider = { action ->
                    when (action) {
                        BackofficeHubAction.PRODUCTS_STOCK ->
                            if (totalProductsCount > 0) {
                                HubActionBadge(
                                    text = "$totalProductsCount item",
                                    dotColor = HubEmerald600,
                                    bgColor = HubEmerald50,
                                    textColor = HubEmerald700,
                                )
                            } else null
                        BackofficeHubAction.CATEGORIES ->
                            if (totalCategoriesCount > 0) {
                                HubActionBadge(
                                    text = "$totalCategoriesCount kategori",
                                    bgColor = HubSlate100,
                                    textColor = HubSlate600,
                                )
                            } else null
                        BackofficeHubAction.UNITS ->
                            if (totalUnitsCount > 0) {
                                HubActionBadge(
                                    text = "$totalUnitsCount satuan",
                                    bgColor = HubSlate100,
                                    textColor = HubSlate600,
                                )
                            } else null
                        else -> null
                    }
                },
                iconColorsProvider = { action ->
                    when (action) {
                        BackofficeHubAction.ADJUST_STOCK -> HubAmberSoftBg to HubAmberText
                        else -> HubEmerald50 to HubEmerald700
                    }
                },
                onActionClick = { action -> onNavigate(action.destination) },
                modifier = horizontalInsetModifier.staggeredEntrance(
                    groupBaseSlidePx + index * groupStepSlidePx,
                ) {
                    entrance.progress(1 + ctaOffset + index)
                },
            )
        }
    }
}

@Composable
private fun StockQuickActionStrip(
    primaryAction: BackofficeHubAction,
    onActionClick: (BackofficeHubAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = { onActionClick(primaryAction) },
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .testTag("stock-quick-primary"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = HubKasiraBrand,
            contentColor = Color.White,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        Icon(
            imageVector = primaryAction.icon(),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(primaryAction.titleRes),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StockValuationDashboardCard(
    showValuationFinancials: Boolean,
    valuationSummary: InventoryValuationSummary?,
    totalProductsCount: Int,
    lowStockCount: Int,
    isPartialStockLoad: Boolean,
    syncStatusText: String,
    isSyncHealthy: Boolean,
    isNominalHidden: Boolean,
    isLoading: Boolean,
    onToggleNominal: () -> Unit,
    onLowStockClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasLowStock = !isLoading && lowStockCount > 0
    val statusText = when {
        isLoading -> "Memuat"
        hasLowStock -> "$lowStockCount Perlu Restok"
        else -> "Stok Aman"
    }
    val statusDotColor = when {
        isLoading -> HubSlate400
        hasLowStock -> Color(0xFFD97706)
        else -> HubEmerald600
    }
    val statusTextColor = if (hasLowStock) HubAmberText else HubSlate500

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HubCardBorder.copy(alpha = 0.75f)),
        shadowElevation = 0.5.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                ) {
                    Text(
                        text = when {
                            !showValuationFinancials -> "Status Inventaris Toko"
                            isPartialStockLoad -> "Total Nilai Aset (Sebagian)"
                            else -> "Total Nilai Aset (HPP)"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HubSlate500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (showValuationFinancials) {
                        IconButton(
                            onClick = onToggleNominal,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("stock-toggle-nominal"),
                        ) {
                            Icon(
                                imageVector = if (isNominalHidden) {
                                    Icons.Outlined.VisibilityOff
                                } else {
                                    Icons.Outlined.Visibility
                                },
                                contentDescription = if (isNominalHidden) {
                                    "Tampilkan nilai aset"
                                } else {
                                    "Sembunyikan nilai aset"
                                },
                                tint = HubSlate500,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .then(
                            if (hasLowStock) {
                                Modifier.clickable(role = Role.Button, onClick = onLowStockClick)
                            } else {
                                Modifier
                            },
                        )
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusDotColor),
                    )
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusTextColor,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (showValuationFinancials) {
                if (isLoading && !isNominalHidden) {
                    SkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(28.dp),
                    )
                } else {
                    val formattedValuation = valuationSummary?.totalValuation?.let(::formatIdCurrency) ?: "Rp0"
                    AnimatedNominalValue(
                        text = if (isNominalHidden) "Rp••••••" else formattedValuation,
                    )
                }
            } else {
                if (isLoading) {
                    SkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(26.dp),
                    )
                } else {
                    Text(
                        text = if (lowStockCount > 0) {
                            "$lowStockCount SKU Perlu Restok Segera"
                        } else {
                            "$totalProductsCount SKU Siap Terpantau"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (lowStockCount > 0) HubAmberText else HubSlate900,
                        letterSpacing = (-0.3).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = HubSlate100, thickness = 1.dp)
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeroFinanceMetricItem(
                    label = "Katalog Aktif",
                    value = if (isLoading) "—" else "$totalProductsCount Item",
                    valueColor = HubSlate800,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp),
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(HubSlate100),
                )

                HeroFinanceMetricItem(
                    label = "Perlu Restok",
                    value = if (isLoading) "—" else "$lowStockCount Item",
                    valueColor = if (lowStockCount > 0) HubAmberText else HubSlate800,
                    onClick = if (lowStockCount > 0) onLowStockClick else null,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp),
                )

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(28.dp)
                        .background(HubSlate100),
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Text(
                        text = "Status Sync",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = HubSlate500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        modifier = Modifier.padding(top = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isSyncHealthy) HubEmerald600 else Color(0xFFD97706)),
                        )
                        Text(
                            text = syncStatusText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = HubSlate800,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionsHub(
    role: String,
    compact: Boolean,
    analyticsRepository: AnalyticsRepository? = null,
    cashReconciliationRepository: CashReconciliationRepository? = null,
    purchasingRepository: PurchasingRepository? = null,
    onNavigate: (AdminDestination) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(BackofficeTransactionTab.SALES) }
    var isNominalHidden by rememberSaveable { mutableStateOf(false) }

    var todayRevenue by remember { mutableStateOf(BigDecimal.ZERO) }
    var todayTrxCount by remember { mutableIntStateOf(0) }
    var hasActiveSession by remember { mutableStateOf(false) }

    var monthlyPurchases by remember { mutableStateOf(BigDecimal.ZERO) }
    var monthlyPurchasesCount by remember { mutableIntStateOf(0) }
    var purchaseLoadFailed by remember { mutableStateOf(false) }
    var isLoading by remember {
        mutableStateOf(
            analyticsRepository != null ||
                cashReconciliationRepository != null ||
                purchasingRepository != null,
        )
    }

    val entrance = rememberStaggeredEntrance(stepCount = 4)

    LaunchedEffect(analyticsRepository, cashReconciliationRepository, purchasingRepository) {
        isLoading = analyticsRepository != null ||
            cashReconciliationRepository != null ||
            purchasingRepository != null
        try {
            if (analyticsRepository != null) {
                runCatching {
                    val metrics = analyticsRepository.getDashboardMetrics()
                    todayRevenue = BigDecimal.valueOf(metrics.totalRevenueToday)
                }
            }
            if (cashReconciliationRepository != null) {
                runCatching {
                    when (val sessionRes = cashReconciliationRepository.getActiveSession()) {
                        is RepositoryResult.Success -> hasActiveSession = sessionRes.data != null
                        else -> Unit
                    }
                }
                runCatching {
                    val todayStr = LocalDate.now().toString()
                    when (
                        val trxRes = cashReconciliationRepository.getTransactions(
                            page = 1,
                            limit = 50,
                            sessionId = null,
                            startDate = todayStr,
                            endDate = todayStr,
                        )
                    ) {
                        is RepositoryResult.Success -> {
                            todayTrxCount = trxRes.data.total.toInt().coerceAtLeast(trxRes.data.data.size)
                            if (todayRevenue == BigDecimal.ZERO && trxRes.data.data.isNotEmpty()) {
                                todayRevenue = trxRes.data.data.fold(BigDecimal.ZERO) { acc, t -> acc.add(t.total) }
                            }
                        }
                        else -> Unit
                    }
                }
            }

            if (purchasingRepository != null) {
                runCatching {
                    val startOfMonth = LocalDate.now().withDayOfMonth(1).toString()
                    when (val purchasesRes = purchasingRepository.getPurchases(page = 1, limit = 50, startDate = startOfMonth)) {
                        is RepositoryResult.Success -> {
                            purchaseLoadFailed = false
                            monthlyPurchasesCount = purchasesRes.data.total.toInt().coerceAtLeast(purchasesRes.data.data.size)
                            monthlyPurchases = purchasesRes.data.data.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.total) }
                        }
                        else -> {
                            purchaseLoadFailed = true
                        }
                    }
                }.onFailure {
                    purchaseLoadFailed = true
                }
            }
        } finally {
            isLoading = false
        }
    }

    val formattedSalesToday = formatIdCurrency(todayRevenue)
    val formattedPurchasesMonth = formatIdCurrency(monthlyPurchases)
    val density = LocalDensity.current
    val tabSlidePx = remember(density) { with(density) { 10.dp.toPx() } }
    val summarySlidePx = remember(density) { with(density) { 16.dp.toPx() } }
    val ctaSlidePx = remember(density) { with(density) { 20.dp.toPx() } }
    val listSlidePx = remember(density) { with(density) { 24.dp.toPx() } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction-hub-content")
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 18.dp),
    ) {
        TransactionSegmentedTabStrip(
            selectedTab = selectedTab,
            compact = compact,
            onSelectTab = { selectedTab = it },
            modifier = Modifier.staggeredEntrance(tabSlidePx) { entrance.progress(TX_STEP_TAB) },
        )

        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                val movingForward = targetState.ordinal > initialState.ordinal
                val slideOffset: (Int) -> Int = { fullWidth ->
                    val delta = (fullWidth / 10).coerceAtLeast(24)
                    if (movingForward) delta else -delta
                }
                (
                    fadeIn(tween(durationMillis = 220, easing = HubMotionEaseOut)) +
                        slideInHorizontally(tween(durationMillis = 260, easing = HubMotionEaseOut), slideOffset)
                    ) togetherWith (
                    fadeOut(tween(durationMillis = 150)) +
                        slideOutHorizontally(tween(durationMillis = 180)) { -slideOffset(it) }
                    )
            },
            label = "TransactionsTabContent",
        ) { tab ->
            val tabContent = remember(role, tab) { transactionHubContent(role, tab) }
            val bodyModifier = if (compact) Modifier.padding(horizontal = 16.dp) else Modifier

            Column(
                modifier = bodyModifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (tab) {
                    BackofficeTransactionTab.SALES -> TransactionSummaryCard(
                        title = "Ringkasan Penjualan",
                        statusText = if (hasActiveSession) "Kasir Aktif" else "Hari Ini",
                        statusDotColor = HubEmerald600,
                        statusTextColor = HubSlate500,
                        nominalText = if (isNominalHidden) "Rp••••••" else formattedSalesToday,
                        isNominalHidden = isNominalHidden,
                        isLoading = isLoading,
                        onToggleNominal = { isNominalHidden = !isNominalHidden },
                        footerLabel = "Total Transaksi",
                        footerValue = "$todayTrxCount Struk",
                        footerValueColor = HubSlate700,
                        modifier = Modifier.staggeredEntrance(summarySlidePx) {
                            entrance.progress(TX_STEP_SUMMARY)
                        },
                    )
                    BackofficeTransactionTab.PURCHASES -> TransactionSummaryCard(
                        title = "Total Pembelian Bulan Ini",
                        statusText = if (purchaseLoadFailed) "Perlu Muat Ulang" else "Bulan Berjalan",
                        statusDotColor = if (purchaseLoadFailed) Color(0xFFD97706) else HubSlate400,
                        statusTextColor = if (purchaseLoadFailed) HubAmberText else HubSlate500,
                        nominalText = when {
                            isNominalHidden -> "Rp••••••"
                            purchaseLoadFailed -> "—"
                            else -> formattedPurchasesMonth
                        },
                        isNominalHidden = isNominalHidden,
                        isLoading = isLoading && !purchaseLoadFailed,
                        onToggleNominal = { isNominalHidden = !isNominalHidden },
                        footerLabel = "Total Faktur Masuk",
                        footerValue = if (purchaseLoadFailed) "Gagal memuat" else "$monthlyPurchasesCount Faktur",
                        footerValueColor = if (purchaseLoadFailed) HubAmberText else HubSlate700,
                        modifier = Modifier.staggeredEntrance(summarySlidePx) {
                            entrance.progress(TX_STEP_SUMMARY)
                        },
                    )
                }

                tabContent.primaryAction?.let { primary ->
                    TransactionPrimaryCtaButton(
                        action = primary,
                        onClick = { onNavigate(primary.destination) },
                        modifier = Modifier.staggeredEntrance(ctaSlidePx) {
                            entrance.progress(TX_STEP_CTA)
                        },
                    )
                }

                tabContent.groups.forEach { group ->
                    HubActivityGroupSection(
                        title = stringResource(group.titleRes).uppercase(Locale.ROOT),
                        actions = group.actions,
                        onActionClick = { action -> onNavigate(action.destination) },
                        modifier = Modifier.staggeredEntrance(listSlidePx) {
                            entrance.progress(TX_STEP_LIST)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionSegmentedTabStrip(
    selectedTab: BackofficeTransactionTab,
    compact: Boolean,
    onSelectTab: (BackofficeTransactionTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerPadding = if (compact) {
        Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
    } else {
        Modifier
    }
    Box(modifier = modifier.fillMaxWidth().then(containerPadding)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = HubPillTrackBg,
            border = BorderStroke(0.5.dp, HubCardBorder),
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(3.dp),
            ) {
                val tabCount = BackofficeTransactionTab.entries.size.coerceAtLeast(1)
                val tabWidth = maxWidth / tabCount
                val thumbOffset by animateDpAsState(
                    targetValue = tabWidth * selectedTab.ordinal,
                    animationSpec = tween(durationMillis = 250, easing = HubMotionEaseOut),
                    label = "TransactionsPillOffset",
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        modifier = Modifier
                            .offset(x = thumbOffset)
                            .width(tabWidth)
                            .height(38.dp),
                        shape = RoundedCornerShape(9.dp),
                        color = Color.White,
                        border = BorderStroke(0.5.dp, HubCardBorder.copy(alpha = 0.7f)),
                        shadowElevation = 1.5.dp,
                    ) {}

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BackofficeTransactionTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            val textColor by animateColorAsState(
                                targetValue = if (isSelected) HubKasiraBrand else HubSlate600,
                                animationSpec = tween(durationMillis = 200),
                                label = "TransactionsPillTextColor",
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .testTag("transactions-tab-${tab.name}")
                                    .clickable(role = Role.Tab) { onSelectTab(tab) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(tab.labelRes),
                                    color = textColor,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionSummaryCard(
    title: String,
    statusText: String,
    statusDotColor: Color,
    statusTextColor: Color,
    nominalText: String,
    isNominalHidden: Boolean,
    isLoading: Boolean,
    onToggleNominal: () -> Unit,
    footerLabel: String,
    footerValue: String,
    footerValueColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HubCardBorder.copy(alpha = 0.75f)),
        shadowElevation = 0.5.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title,
                        color = HubSlate500,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(
                        onClick = onToggleNominal,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("transactions-toggle-nominal"),
                    ) {
                        Icon(
                            imageVector = if (isNominalHidden) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = if (isNominalHidden) {
                                "Tampilkan nominal"
                            } else {
                                "Sembunyikan nominal"
                            },
                            tint = HubSlate500,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusDotColor),
                    )
                    Text(
                        text = statusText,
                        color = statusTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (isLoading && !isNominalHidden) {
                SkeletonBox(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(28.dp),
                )
            } else {
                AnimatedNominalValue(text = nominalText)
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = HubSlate100, thickness = 1.dp)
            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = footerLabel,
                    color = HubSlate500,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = footerValue,
                    color = footerValueColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun TransactionPrimaryCtaButton(
    action: BackofficeHubAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = if (action == BackofficeHubAction.INCOMING_GOODS) {
        "Tambah Barang Masuk"
    } else {
        stringResource(action.titleRes)
    }
    val icon = if (action == BackofficeHubAction.INCOMING_GOODS) {
        Icons.Default.Add
    } else {
        action.icon()
    }
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .testTag("transactions-primary-${action.name}"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = HubKasiraBrand,
            contentColor = Color.White,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun HubActivityGroupSection(
    title: String,
    actions: List<BackofficeHubAction>,
    onActionClick: (BackofficeHubAction) -> Unit,
    modifier: Modifier = Modifier,
    titleProvider: @Composable (BackofficeHubAction) -> String = { stringResource(it.titleRes) },
    badgeProvider: (BackofficeHubAction) -> HubActionBadge? = { null },
    iconColorsProvider: (BackofficeHubAction) -> Pair<Color, Color> = { HubEmerald50 to HubEmerald700 },
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            color = HubSlate500,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.7.sp,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, HubCardBorder.copy(alpha = 0.75f)),
            shadowElevation = 0.5.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
            ) {
                actions.forEachIndexed { index, action ->
                    val (iconBg, iconTint) = iconColorsProvider(action)
                    HubActivityRow(
                        action = action,
                        title = titleProvider(action),
                        badge = badgeProvider(action),
                        iconBgColor = iconBg,
                        iconTintColor = iconTint,
                        onClick = { onActionClick(action) },
                    )
                    if (index < actions.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 62.dp, end = 16.dp),
                            color = HubSlate100,
                            thickness = 1.dp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HubActivityRow(
    action: BackofficeHubAction,
    title: String,
    badge: HubActionBadge?,
    iconBgColor: Color,
    iconTintColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag("hub-action-${action.name}")
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBgColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = action.icon(),
                contentDescription = null,
                tint = iconTintColor,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = title,
            color = HubSlate800,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (badge != null) {
            Surface(
                shape = CircleShape,
                color = badge.bgColor ?: HubSlate100,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    if (badge.dotColor != null) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(badge.dotColor),
                        )
                    }
                    Text(
                        text = badge.text,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = badge.textColor ?: HubSlate600,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = HubSlate400,
            modifier = Modifier.size(18.dp),
        )
    }
}

private fun BackofficeHubAction.icon(): ImageVector = when (this) {
    BackofficeHubAction.SALES_HISTORY -> Icons.AutoMirrored.Outlined.ReceiptLong
    BackofficeHubAction.NEW_SALE -> Icons.Outlined.PointOfSale
    BackofficeHubAction.RECEIVABLE_PAYMENTS -> Icons.Outlined.Payments
    BackofficeHubAction.PURCHASE_HISTORY -> Icons.AutoMirrored.Outlined.ListAlt
    BackofficeHubAction.INCOMING_GOODS -> Icons.Outlined.LocalShipping
    BackofficeHubAction.RECEIVABLES -> Icons.Outlined.Payments
    BackofficeHubAction.SUPPLIER_DEBTS -> Icons.Outlined.AccountBalanceWallet
    BackofficeHubAction.DAILY_CASH -> Icons.Outlined.AssignmentTurnedIn
    BackofficeHubAction.CASH_EXPENSES -> Icons.Outlined.PriceChange
    BackofficeHubAction.FINANCE_REPORTS -> Icons.AutoMirrored.Outlined.TrendingUp
    BackofficeHubAction.ADD_PRODUCT -> Icons.Default.Add
    BackofficeHubAction.PRODUCTS_STOCK -> Icons.Outlined.Inventory2
    BackofficeHubAction.ADJUST_STOCK -> Icons.Outlined.Tune
    BackofficeHubAction.STOCK_CARD -> Icons.AutoMirrored.Outlined.ListAlt
    BackofficeHubAction.PRODUCT_PRICES -> Icons.Outlined.LocalOffer
    BackofficeHubAction.CATEGORIES -> Icons.Outlined.Category
    BackofficeHubAction.UNITS -> Icons.Outlined.Straighten
}
