package com.tbterminal.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.PriceChange
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tbterminal.app.R
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
import com.tbterminal.app.ui.components.TbPageSurface
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

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
                BackofficeSection.TRANSACTIONS -> {
                    HubTopBarIconButton(
                        icon = Icons.Outlined.Search,
                        contentDescription = "Cari transaksi",
                        onClick = { navigate(AdminDestination.SalesTransactions) },
                        testTag = "hub-transactions-search",
                    )
                }
                BackofficeSection.FINANCE -> {
                    HubTopBarIconButton(
                        icon = Icons.Outlined.Search,
                        contentDescription = "Cari transaksi",
                        onClick = { navigate(AdminDestination.SalesTransactions) },
                        testTag = "hub-finance-search",
                    )
                    HubTopBarIconButton(
                        icon = Icons.Outlined.Assessment,
                        contentDescription = if (canViewReportsAnalytics) "Analitik & Laporan" else "Laporan Perangkat",
                        onClick = {
                            navigate(
                                if (canViewReportsAnalytics) AdminDestination.Reports else AdminDestination.LocalReports
                            )
                        },
                        testTag = "hub-finance-reports",
                    )
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
        TbPageSurface(modifier = contentModifier, maxContentWidth = 1120.dp) { layout, pageModifier ->
            Column(
                modifier = pageModifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(if (layout.isCompact) 20.dp else 24.dp),
            ) {
                when (section) {
                    BackofficeSection.TRANSACTIONS -> TransactionsHub(
                        role = role,
                        analyticsRepository = analyticsRepository,
                        cashReconciliationRepository = cashReconciliationRepository,
                        purchasingRepository = purchasingRepository,
                        onNavigate = navigate,
                    )
                    BackofficeSection.FINANCE -> FinanceHub(
                        role = role,
                        content = financeHubContent(role),
                        cashReconciliationRepository = cashReconciliationRepository,
                        receivableRepository = receivableRepository,
                        purchasingRepository = purchasingRepository,
                        analyticsRepository = analyticsRepository,
                        onNavigate = navigate,
                    )
                    BackofficeSection.STOCK -> StockHub(
                        role = role,
                        content = stockHubContent(role),
                        inventoryRepository = inventoryRepository,
                        syncMonitoringRepository = syncMonitoringRepository,
                        onNavigate = navigate,
                    )
                    BackofficeSection.MORE -> BackofficeMoreContent(
                        layout = layout,
                        userName = name,
                        role = role,
                        onNavigate = navigate,
                        onUserManagementClick = onUserManagementClick,
                        onSecurityLogClick = onSecurityLogClick,
                        onLogout = onLogout,
                    )
                    BackofficeSection.HOME, BackofficeSection.MENU -> Unit
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ── COLOR PALETTE: REFINED INDUSTRIAL STANDARD ──
private val HubSlate900 = Color(0xFF0F172A)
private val HubSlate800 = Color(0xFF1E293B)
private val HubSlate700 = Color(0xFF334155)
private val HubSlate500 = Color(0xFF64748B)
private val HubSlate400 = Color(0xFF94A3B8)
private val HubSlate300 = Color(0xFFCBD5E1)
private val HubSlate200 = Color(0xFFE2E8F0)
private val HubSlate100 = Color(0xFFF1F5F9)

private val HubBrand50 = Color(0xFFEEF7F2)
private val HubBrand100 = Color(0xFFD1FAE5)
private val HubBrand500 = Color(0xFF16A34A)
private val HubBrand600 = Color(0xFF1B5E3C)
private val HubBrand700 = Color(0xFF15803D)
private val HubBrand800 = Color(0xFF1B5E3C)

private data class HubActionBadge(
    val text: String,
    val dotColor: Color? = null,
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
    cashReconciliationRepository: CashReconciliationRepository? = null,
    receivableRepository: ReceivableRepository? = null,
    purchasingRepository: PurchasingRepository? = null,
    analyticsRepository: AnalyticsRepository? = null,
    onNavigate: (AdminDestination) -> Unit,
) {
    var totalCashBalance by remember { mutableStateOf(BigDecimal.ZERO) }
    var todayCashInflow by remember { mutableStateOf(BigDecimal.ZERO) }
    var totalReceivables by remember { mutableStateOf(BigDecimal.ZERO) }
    var receivableNotesCount by remember { mutableIntStateOf(0) }
    var totalPayables by remember { mutableStateOf(BigDecimal.ZERO) }
    var payableNotesCount by remember { mutableIntStateOf(0) }
    var todayExpensesTotal by remember { mutableStateOf(BigDecimal.ZERO) }
    var todayExpensesCount by remember { mutableIntStateOf(0) }
    var hasActiveSession by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(cashReconciliationRepository, receivableRepository, purchasingRepository, analyticsRepository) {
        isLoading = true
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
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("finance-hub-content")
            .padding(top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        HeroFinancialCard(
            totalCashBalance = totalCashBalance,
            todayCashInflow = todayCashInflow,
            totalReceivable = totalReceivables,
            totalPayable = totalPayables,
            receivableNotesCount = receivableNotesCount,
            payableNotesCount = payableNotesCount,
            hasActiveSession = hasActiveSession,
            isLoading = isLoading,
            onReceivablesClick = { onNavigate(AdminDestination.Receivables) },
            onPayablesClick = { onNavigate(AdminDestination.SupplierDebts) },
        )

        content.groups.forEach { group ->
            HubActionGroupCard(
                title = stringResource(group.titleRes).uppercase(Locale.ROOT),
                actions = group.actions,
                role = role,
                badgeProvider = { action ->
                    when (action) {
                        BackofficeHubAction.DAILY_CASH -> HubActionBadge(
                            text = if (hasActiveSession) "Sesi Aktif" else "Sesi Ditutup",
                            dotColor = if (hasActiveSession) Color(0xFF10B981) else HubSlate400,
                        )
                        BackofficeHubAction.CASH_EXPENSES -> HubActionBadge(
                            text = if (todayExpensesCount > 0) {
                                "$todayExpensesCount Beban · ${formatShortCurrency(todayExpensesTotal)}"
                            } else {
                                "Hari Ini Rp0"
                            },
                            dotColor = if (todayExpensesCount > 0) Color(0xFFF59E0B) else HubSlate400,
                        )
                        BackofficeHubAction.RECEIVABLES -> HubActionBadge(
                            text = "$receivableNotesCount Nota · ${formatShortCurrency(totalReceivables)}",
                            dotColor = if (receivableNotesCount > 0) Color(0xFF10B981) else HubSlate400,
                        )
                        BackofficeHubAction.SUPPLIER_DEBTS -> HubActionBadge(
                            text = "$payableNotesCount Nota · ${formatShortCurrency(totalPayables)}",
                            dotColor = if (payableNotesCount > 0) Color(0xFFF59E0B) else HubSlate400,
                        )
                        else -> null
                    }
                },
                onActionClick = { action -> onNavigate(action.destination) },
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
    isLoading: Boolean,
    onReceivablesClick: () -> Unit,
    onPayablesClick: () -> Unit,
) {
    var isBalanceVisible by remember { mutableStateOf(true) }

    val statusText = when {
        hasActiveSession -> "Shift Kas Aktif"
        payableNotesCount > 0 || receivableNotesCount > 0 -> "$receivableNotesCount Piutang · $payableNotesCount Hutang"
        else -> "Kas Siap"
    }
    val statusDotColor = when {
        hasActiveSession -> HubBrand600
        payableNotesCount > 0 -> Color(0xFFD97706)
        else -> HubBrand600
    }
    val statusBg = if (!hasActiveSession && payableNotesCount > 0) Color(0xFFFEF3C7) else HubBrand50
    val statusBorder = if (!hasActiveSession && payableNotesCount > 0) Color(0xFFFDE68A) else HubBrand100.copy(alpha = 0.8f)
    val statusTextColor = if (!hasActiveSession && payableNotesCount > 0) Color(0xFFB45309) else HubBrand700

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HubSlate100),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RINGKASAN KAS & SALDO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HubSlate500,
                    letterSpacing = 0.5.sp
                )

                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusBg,
                    border = BorderStroke(1.dp, statusBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusDotColor)
                        )
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = statusTextColor
                        )
                    }
                }
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (hasActiveSession) "Saldo Kas Laci (Shift Aktif)" else "Saldo Kas Sesi Terakhir",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = HubSlate400
                    )
                    IconButton(
                        onClick = { isBalanceVisible = !isBalanceVisible },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isBalanceVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = if (isBalanceVisible) "Sembunyikan Saldo" else "Tampilkan Saldo",
                            tint = HubSlate400,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (isLoading) {
                    SkeletonBox(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(32.dp)
                            .padding(top = 4.dp)
                    )
                } else {
                    Text(
                        text = if (isBalanceVisible) formatIdCurrency(totalCashBalance) else "Rp ••••••••",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = HubSlate900,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            HorizontalDivider(color = HubSlate100, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Kas Hari Ini",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = HubSlate400
                    )
                    Text(
                        text = if (isLoading) "-" else "+${formatIdCurrency(todayCashInflow)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = HubBrand700,
                        modifier = Modifier.padding(top = 3.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(HubSlate100)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onReceivablesClick)
                        .padding(horizontal = 6.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Piutang Aktif",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = HubSlate400
                    )
                    Text(
                        text = if (isLoading) "-" else formatIdCurrency(totalReceivable),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = HubSlate800,
                        modifier = Modifier.padding(top = 3.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(HubSlate100)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onPayablesClick)
                        .padding(start = 6.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Hutang Tempo",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = HubSlate400
                    )
                    Text(
                        text = if (isLoading) "-" else formatIdCurrency(totalPayable),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = HubSlate800,
                        modifier = Modifier.padding(top = 3.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
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
    inventoryRepository: InventoryRepository? = null,
    syncMonitoringRepository: SyncMonitoringRepository? = null,
    onNavigate: (AdminDestination) -> Unit,
) {
    val canViewFinancialValuation = AppAccessPolicy.can(role, AppCapability.FINANCIAL_ANALYTICS)
    var valuationSummary by remember { mutableStateOf<InventoryValuationSummary?>(null) }
    var lowStockCount by remember { mutableIntStateOf(0) }
    var totalProductsCount by remember { mutableIntStateOf(0) }
    var totalCategoriesCount by remember { mutableIntStateOf(0) }
    var totalUnitsCount by remember { mutableIntStateOf(0) }
    var isPartialStockLoad by remember { mutableStateOf(false) }
    var syncStatusText by remember { mutableStateOf("Tersinkron") }
    var isSyncHealthy by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(inventoryRepository != null) }

    LaunchedEffect(inventoryRepository) {
        if (inventoryRepository != null) {
            isLoading = true
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
            isLoading = false
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stock-hub-content")
            .padding(top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StockValuationDashboardCard(
            showValuationFinancials = canViewFinancialValuation,
            valuationSummary = valuationSummary,
            totalProductsCount = totalProductsCount,
            lowStockCount = lowStockCount,
            isPartialStockLoad = isPartialStockLoad,
            syncStatusText = syncStatusText,
            isSyncHealthy = isSyncHealthy,
            isLoading = isLoading,
            onLowStockClick = { onNavigate(AdminDestination.Products) },
        )

        val quickActions = listOfNotNull(content.primaryAction, content.secondaryQuickAction)
        if (quickActions.isNotEmpty()) {
            StockQuickActionStrip(
                primaryAction = content.primaryAction,
                secondaryAction = content.secondaryQuickAction,
                onActionClick = { action -> onNavigate(action.destination) },
            )
        }

        content.groups.forEach { group ->
            HubActionGroupCard(
                title = stringResource(group.titleRes).uppercase(Locale.ROOT),
                actions = group.actions,
                role = role,
                subtitleOverride = { action ->
                    when (action) {
                        BackofficeHubAction.PRODUCTS_STOCK ->
                            if (totalProductsCount > 0) "$totalProductsCount produk terdaftar" else null
                        BackofficeHubAction.CATEGORIES ->
                            if (totalCategoriesCount > 0) "$totalCategoriesCount kategori terdaftar" else null
                        BackofficeHubAction.UNITS ->
                            if (totalUnitsCount > 0) "$totalUnitsCount satuan ukuran terdaftar" else null
                        BackofficeHubAction.PRODUCT_PRICES ->
                            if (!canViewFinancialValuation) stringResource(R.string.hub_product_prices_admin_description) else null
                        else -> null
                    }
                },
                onActionClick = { action -> onNavigate(action.destination) },
            )
        }
    }
}

@Composable
private fun StockQuickActionStrip(
    primaryAction: BackofficeHubAction?,
    secondaryAction: BackofficeHubAction?,
    onActionClick: (BackofficeHubAction) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        primaryAction?.let { action ->
            Button(
                onClick = { onActionClick(action) },
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("stock-quick-primary"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HubBrand600,
                    contentColor = Color.White,
                ),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalShipping,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Barang Masuk",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        secondaryAction?.let { action ->
            OutlinedButton(
                onClick = { onActionClick(action) },
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("stock-quick-secondary"),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, HubBrand600.copy(alpha = 0.45f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = HubBrand600,
                ),
                contentPadding = PaddingValues(horizontal = 12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(action.titleRes),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
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
    isLoading: Boolean,
    onLowStockClick: () -> Unit,
) {
    var isValuationVisible by remember { mutableStateOf(true) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HubSlate200.copy(alpha = 0.8f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showValuationFinancials) "VALUASI STOK TOKO" else "STATUS INVENTARIS TOKO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HubSlate400,
                    letterSpacing = 0.5.sp
                )

                if (!isLoading && lowStockCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.clickable(onClick = onLowStockClick)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFD97706)))
                            Text(
                                text = "$lowStockCount Perlu Restok",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                } else if (!isLoading) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = HubBrand50,
                        border = BorderStroke(1.dp, HubBrand100)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(HubBrand500))
                            Text(
                                text = "Stok Aman",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = HubBrand700
                            )
                        }
                    }
                }
            }

            if (showValuationFinancials) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPartialStockLoad) "Total Nilai Aset (Sebagian Data)" else "Total Nilai Aset (HPP)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = HubSlate500
                        )
                        IconButton(
                            onClick = { isValuationVisible = !isValuationVisible },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isValuationVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                contentDescription = if (isValuationVisible) "Sembunyikan nilai aset" else "Tampilkan nilai aset",
                                tint = HubSlate400,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (isLoading) {
                        SkeletonBox(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .height(28.dp)
                                .padding(top = 4.dp)
                        )
                    } else {
                        val formattedValuation = valuationSummary?.totalValuation?.let(::formatIdCurrency) ?: "Rp0"
                        Text(
                            text = if (isValuationVisible) formattedValuation else "Rp ••••••••",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = HubSlate900,
                            letterSpacing = (-0.5).sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            } else {
                Column {
                    Text(
                        text = "Ringkasan Operasional Gudang",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = HubSlate500
                    )
                    if (isLoading) {
                        SkeletonBox(
                            modifier = Modifier
                                .fillMaxWidth(0.45f)
                                .height(26.dp)
                                .padding(top = 4.dp)
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
                            color = if (lowStockCount > 0) Color(0xFFB45309) else HubSlate900,
                            letterSpacing = (-0.3).sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = HubSlate100, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Katalog Aktif",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = HubSlate400
                    )
                    Text(
                        text = if (isLoading) "-" else "$totalProductsCount Item",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HubSlate800,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(HubSlate100)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = lowStockCount > 0, onClick = onLowStockClick),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Perlu Restok",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = HubSlate400
                    )
                    Text(
                        text = if (isLoading) "-" else "$lowStockCount Item",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (lowStockCount > 0) Color(0xFFB45309) else HubSlate800,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(HubSlate100)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Status Sync",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = HubSlate400
                    )
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isSyncHealthy) HubBrand500 else Color(0xFFD97706))
                        )
                        Text(
                            text = syncStatusText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HubSlate800,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
    analyticsRepository: AnalyticsRepository? = null,
    cashReconciliationRepository: CashReconciliationRepository? = null,
    purchasingRepository: PurchasingRepository? = null,
    onNavigate: (AdminDestination) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(BackofficeTransactionTab.SALES) }
    val content = remember(role, selectedTab) { transactionHubContent(role, selectedTab) }

    var todayRevenue by remember { mutableStateOf(BigDecimal.ZERO) }
    var todayTrxCount by remember { mutableIntStateOf(0) }
    var hasActiveSession by remember { mutableStateOf(false) }

    var monthlyPurchases by remember { mutableStateOf(BigDecimal.ZERO) }
    var monthlyPurchasesCount by remember { mutableIntStateOf(0) }
    var purchaseLoadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(analyticsRepository, cashReconciliationRepository, purchasingRepository) {
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
    }

    val formattedSalesToday = formatIdCurrency(todayRevenue)
    val formattedPurchasesMonth = formatIdCurrency(monthlyPurchases)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction-hub-content")
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Segmented Control
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, HubSlate200),
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackofficeTransactionTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("transactions-tab-${tab.name}")
                            .clickable { selectedTab = tab },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = stringResource(tab.labelRes),
                                color = if (isSelected) HubBrand600 else HubSlate500,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .width(50.dp)
                                    .height(3.dp)
                                    .background(HubBrand600, RoundedCornerShape(50))
                            )
                        }
                    }
                }
            }
        }

        if (selectedTab == BackofficeTransactionTab.SALES) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, HubBrand600),
                shadowElevation = 0.5.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ringkasan Hari Ini",
                            color = HubSlate500,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = HubBrand50,
                            border = BorderStroke(1.dp, HubBrand100)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(HubBrand600)
                                )
                                Text(
                                    text = if (hasActiveSession) "Kasir Aktif" else "Hari Ini",
                                    color = HubBrand600,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = formattedSalesToday,
                                color = HubSlate900,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = if (todayTrxCount > 0) "$todayTrxCount Transaksi Berhasil" else "Belum ada transaksi hari ini",
                                color = HubSlate500,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = HubBrand50,
                            border = BorderStroke(1.dp, HubBrand100)
                        ) {
                            Text(
                                text = if (hasActiveSession) "Shift Aktif" else "Total Penjualan",
                                color = HubBrand600,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, HubBrand600),
                shadowElevation = 0.5.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Pembelian Bulan Ini",
                            color = HubSlate500,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (purchaseLoadFailed) Color(0xFFFEF3C7) else HubBrand50,
                            border = BorderStroke(1.dp, if (purchaseLoadFailed) Color(0xFFFDE68A) else HubBrand100)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (purchaseLoadFailed) Color(0xFFD97706) else HubBrand600)
                                )
                                Text(
                                    text = if (purchaseLoadFailed) "Perlu Muat Ulang" else "Bulan Berjalan",
                                    color = if (purchaseLoadFailed) Color(0xFFB45309) else HubBrand600,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = if (purchaseLoadFailed) "—" else formattedPurchasesMonth,
                                color = HubSlate900,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = when {
                                    purchaseLoadFailed -> "Data pembelian belum tersedia, buka Riwayat pembelian"
                                    monthlyPurchasesCount > 0 -> "$monthlyPurchasesCount Faktur dari Supplier"
                                    else -> "Belum ada pembelian bulan ini"
                                },
                                color = HubSlate500,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = HubBrand50,
                            border = BorderStroke(1.dp, HubBrand100)
                        ) {
                            Text(
                                text = "Pengadaan",
                                color = HubBrand600,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        content.primaryAction?.let { primary ->
            Button(
                onClick = { onNavigate(primary.destination) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("transactions-primary-${primary.name}"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HubBrand600,
                    contentColor = Color.White,
                ),
            ) {
                Icon(
                    imageVector = primary.icon(),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(primary.titleRes),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        content.groups.forEach { group ->
            HubActionGroupCard(
                title = stringResource(group.titleRes).uppercase(Locale.ROOT),
                actions = group.actions,
                role = role,
                onActionClick = { action -> onNavigate(action.destination) },
            )
        }
    }
}

@Composable
private fun HubActionGroupCard(
    title: String,
    actions: List<BackofficeHubAction>,
    role: String,
    badgeProvider: (BackofficeHubAction) -> HubActionBadge? = { null },
    subtitleOverride: (BackofficeHubAction) -> String? = { null },
    onActionClick: (BackofficeHubAction) -> Unit,
) {
    val showFinancialLabel = AppAccessPolicy.can(role, AppCapability.FINANCIAL_ANALYTICS)
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = HubSlate500,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, HubSlate200.copy(alpha = 0.7f)),
            shadowElevation = 0.5.dp
        ) {
            Column {
                actions.forEachIndexed { index, action ->
                    val itemTitle = if (action == BackofficeHubAction.PRODUCT_PRICES && showFinancialLabel) {
                        "Harga & Margin"
                    } else {
                        stringResource(action.titleRes)
                    }
                    val itemSubtitle = subtitleOverride(action) ?: stringResource(action.descriptionRes)
                    HubActionRow(
                        icon = action.icon(),
                        title = itemTitle,
                        subtitle = itemSubtitle,
                        badge = badgeProvider(action),
                        testTag = "hub-action-${action.name}",
                        onClick = { onActionClick(action) },
                    )
                    if (index < actions.lastIndex) {
                        HorizontalDivider(color = HubSlate100, thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HubActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: HubActionBadge? = null,
    testTag: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(HubBrand50),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = HubBrand800,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(
                modifier = Modifier.padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    color = HubSlate800,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = HubSlate500,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, HubSlate200)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (badge.dotColor != null) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(badge.dotColor)
                            )
                        }
                        Text(
                            text = badge.text,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = HubSlate700,
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
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private fun BackofficeHubAction.icon(): ImageVector = when (this) {
    BackofficeHubAction.SALES_HISTORY -> Icons.AutoMirrored.Outlined.ReceiptLong
    BackofficeHubAction.NEW_SALE -> Icons.Outlined.PointOfSale
    BackofficeHubAction.RECEIVABLE_PAYMENTS -> Icons.Outlined.Payments
    BackofficeHubAction.OPERATIONAL_AUDIT -> Icons.Outlined.Security
    BackofficeHubAction.PURCHASE_HISTORY -> Icons.AutoMirrored.Outlined.ListAlt
    BackofficeHubAction.INCOMING_GOODS -> Icons.Outlined.LocalShipping
    BackofficeHubAction.SUPPLIERS -> Icons.Outlined.Group
    BackofficeHubAction.RECEIVABLES -> Icons.Outlined.Payments
    BackofficeHubAction.SUPPLIER_DEBTS -> Icons.Outlined.AccountBalanceWallet
    BackofficeHubAction.DAILY_CASH -> Icons.Outlined.AssignmentTurnedIn
    BackofficeHubAction.CASH_EXPENSES -> Icons.Outlined.PriceChange
    BackofficeHubAction.CASH_SESSION_HISTORY -> Icons.AutoMirrored.Outlined.MenuBook
    BackofficeHubAction.FINANCE_REPORTS -> Icons.AutoMirrored.Outlined.TrendingUp
    BackofficeHubAction.ADD_PRODUCT -> Icons.Default.Add
    BackofficeHubAction.PRODUCTS_STOCK -> Icons.Outlined.Inventory2
    BackofficeHubAction.ADJUST_STOCK -> Icons.Outlined.Tune
    BackofficeHubAction.STOCK_CARD -> Icons.AutoMirrored.Outlined.ListAlt
    BackofficeHubAction.PRODUCT_PRICES -> Icons.Outlined.LocalOffer
    BackofficeHubAction.CATEGORIES -> Icons.Outlined.Category
    BackofficeHubAction.UNITS -> Icons.Outlined.Straighten
}
