package com.tbterminal.app.ui.dashboard

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.data.repository.InventoryRepository
import com.tbterminal.app.data.repository.PurchasingRepository
import com.tbterminal.app.data.repository.ReceivableRepository
import com.tbterminal.app.data.repository.AnalyticsRepository
import com.tbterminal.app.data.repository.SyncMonitoringRepository
import com.tbterminal.app.data.repository.RepositoryResult
import java.math.BigDecimal
import com.tbterminal.app.ui.barcode.CameraBarcodeScannerDialog
import com.tbterminal.app.ui.components.SkeletonBox
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tbterminal.app.R
import com.tbterminal.app.ui.components.TbLayoutInfo
import com.tbterminal.app.ui.components.TbPageSurface
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator

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
    val navigate: (AdminDestination) -> Unit = onNavigate ?: { destination -> globalNavigator?.invoke(destination) }
    var showStockBarcodeScanner by remember { mutableStateOf(false) }
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
            onBarcodeScanned = { _ ->
                showStockBarcodeScanner = false
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
                        contentDescription = if (isOwnerPersona(role)) "Analitik & Laporan" else "Audit & Laporan Cepat",
                        onClick = { navigate(AdminDestination.Reports) },
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
                        layout = layout,
                        role = role,
                        analyticsRepository = analyticsRepository,
                        cashReconciliationRepository = cashReconciliationRepository,
                        purchasingRepository = purchasingRepository,
                        onNavigate = navigate,
                    )
                    BackofficeSection.FINANCE -> FinanceHub(
                        layout = layout,
                        content = financeHubContent(role),
                        cashReconciliationRepository = cashReconciliationRepository,
                        receivableRepository = receivableRepository,
                        purchasingRepository = purchasingRepository,
                        analyticsRepository = analyticsRepository,
                        onNavigate = navigate,
                    )
                    BackofficeSection.STOCK -> StockHub(
                        layout = layout,
                        content = stockHubContent(role),
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
                    BackofficeSection.HOME, BackofficeSection.MENU -> Unit
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ── COLOR PALETTE: REFINED INDUSTRIAL STANDARD (TAILWIND SPEC) ──
private val StockSlate900 = Color(0xFF0F172A)
private val StockSlate800 = Color(0xFF1E293B)
private val StockSlate700 = Color(0xFF334155)
private val StockSlate500 = Color(0xFF64748B)
private val StockSlate400 = Color(0xFF94A3B8)
private val StockSlate300 = Color(0xFFCBD5E1)
private val StockSlate200 = Color(0xFFE2E8F0)
private val StockSlate100 = Color(0xFFF1F5F9)

private val FinanceBrand50 = Color(0xFFEEF7F2)
private val FinanceBrand100 = Color(0xFFD1FAE5)
private val FinanceBrand200 = Color(0xFFA7F3D0)
private val FinanceBrand600 = Color(0xFF1B5E3C)
private val FinanceBrand700 = Color(0xFF15803D)
private val FinanceBrand800 = Color(0xFF1B5E3C)
private val FinanceBrand900 = Color(0xFF14532D)

private fun formatShortCurrency(amount: BigDecimal): String {
    val value = amount.toDouble()
    return when {
        value >= 1_000_000_000 -> String.format(java.util.Locale.US, "Rp%.1f M", value / 1_000_000_000).replace(".", ",")
        value >= 1_000_000 -> String.format(java.util.Locale.US, "Rp%.1f Jt", value / 1_000_000).replace(".", ",")
        value >= 1_000 -> String.format(java.util.Locale.US, "Rp%.0f Rb", value / 1_000)
        else -> "Rp" + value.toLong().toString()
    }
}

@Composable
private fun FinanceHub(
    layout: TbLayoutInfo,
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
    var hasActiveSession by remember { mutableStateOf(false) }
    var isCashHealthy by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(cashReconciliationRepository, receivableRepository, purchasingRepository, analyticsRepository) {
        isLoading = true
        var loadedBalance = BigDecimal.ZERO
        var loadedInflow = BigDecimal.ZERO
        var loadedReceivables = BigDecimal.ZERO
        var loadedReceivablesCount = 0
        var loadedPayables = BigDecimal.ZERO
        var loadedPayablesCount = 0

        // 1. Fetch Analytics for today's summary & receivables
        if (analyticsRepository != null) {
            runCatching {
                val metrics = analyticsRepository.getDashboardMetrics()
                loadedInflow = BigDecimal.valueOf(metrics.totalRevenueToday)
                loadedReceivables = BigDecimal.valueOf(metrics.totalActiveReceivables)
                loadedReceivablesCount = metrics.activeReceivableCount.toInt()
            }
        }

        // 2. Fetch Cash Reconciliation for active session or last session cash
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
                            // If no active session, fetch latest closed session cash balance
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
        }

        // 3. Receivables detailed list
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

        // 4. Payables detailed list
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
        isCashHealthy = loadedBalance >= loadedPayables
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("finance-hub-content")
            .padding(top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // BEGIN: HeroFinancialCard
        HeroFinancialCard(
            totalCashBalance = totalCashBalance,
            todayCashInflow = todayCashInflow,
            totalReceivable = totalReceivables,
            totalPayable = totalPayables,
            isCashHealthy = isCashHealthy,
            isLoading = isLoading,
            onReceivablesClick = { onNavigate(AdminDestination.Receivables) },
            onPayablesClick = { onNavigate(AdminDestination.SupplierDebts) },
        )
        // END: HeroFinancialCard

        // BEGIN: CoreTransactionsSection
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "MENU TRANSAKSI & BUKU KAS",
                color = StockSlate400,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, StockSlate100),
                shadowElevation = 1.dp
            ) {
                Column {
                    FinanceHubItem(
                        icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                        title = "Piutang pelanggan",
                        subtitle = "Tagihan & pembayaran pelanggan",
                        badgeText = "$receivableNotesCount Nota · ${formatShortCurrency(totalReceivables)}",
                        badgeDotColor = Color(0xFF10B981),
                        onClick = { onNavigate(AdminDestination.Receivables) },
                    )
                    HorizontalDivider(color = StockSlate100, thickness = 1.dp)
                    FinanceHubItem(
                        icon = Icons.Outlined.AccountBalanceWallet,
                        title = "Hutang supplier",
                        subtitle = "Kewajiban dan pembayaran supplier",
                        badgeText = "$payableNotesCount Nota · ${formatShortCurrency(totalPayables)}",
                        badgeDotColor = Color(0xFFF59E0B),
                        onClick = { onNavigate(AdminDestination.SupplierDebts) },
                    )
                    HorizontalDivider(color = StockSlate100, thickness = 1.dp)
                    FinanceHubItem(
                        icon = Icons.Outlined.AssignmentTurnedIn,
                        title = "Kas harian",
                        subtitle = "Sesi kas dan rekonsiliasi harian",
                        badgeText = if (hasActiveSession) "1 Sesi Terbuka" else "Sesi Ditutup",
                        badgeDotColor = if (hasActiveSession) Color(0xFF10B981) else StockSlate400,
                        onClick = { onNavigate(AdminDestination.CashReconciliation) },
                    )
                }
            }
        }
        // END: CoreTransactionsSection

        // BEGIN: ReportsAndLedgerSection
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "LAPORAN & CATATAN",
                color = StockSlate400,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, StockSlate100),
                shadowElevation = 1.dp
            ) {
                Column {
                    FinanceHubItem(
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                        title = "Buku kas umum",
                        subtitle = "Arus kas masuk & keluar toko",
                        badgeText = "Bulan Ini",
                        badgeDotColor = Color(0xFF10B981),
                        onClick = { onNavigate(AdminDestination.CashSessionHistory) },
                    )
                    HorizontalDivider(color = StockSlate100, thickness = 1.dp)
                    FinanceHubItem(
                        icon = Icons.AutoMirrored.Outlined.TrendingUp,
                        title = "Laporan laba rugi",
                        subtitle = "Estimasi laba bersih & operasional",
                        badgeText = "Periode Ini",
                        badgeDotColor = Color(0xFF10B981),
                        onClick = { onNavigate(AdminDestination.Reports) },
                    )
                }
            }
        }
        // END: ReportsAndLedgerSection
    }
}

@Composable
private fun HeroFinancialCard(
    totalCashBalance: BigDecimal,
    todayCashInflow: BigDecimal,
    totalReceivable: BigDecimal,
    totalPayable: BigDecimal,
    isCashHealthy: Boolean,
    isLoading: Boolean,
    onReceivablesClick: () -> Unit,
    onPayablesClick: () -> Unit,
) {
    var isBalanceVisible by remember { mutableStateOf(true) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, StockSlate100),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Category Title & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RINGKASAN KAS & SALDO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StockSlate500,
                    letterSpacing = 0.5.sp
                )

                Surface(
                    shape = RoundedCornerShape(50),
                    color = FinanceBrand50,
                    border = BorderStroke(1.dp, FinanceBrand100.copy(alpha = 0.8f))
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
                                .background(FinanceBrand600)
                        )
                        Text(
                            text = if (isCashHealthy) "Arus Kas Sehat" else "Perlu Pantauan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = FinanceBrand700
                        )
                    }
                }
            }

            // Main Balance Section
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Saldo Kas Toko",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = StockSlate400
                    )
                    IconButton(
                        onClick = { isBalanceVisible = !isBalanceVisible },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isBalanceVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = if (isBalanceVisible) "Sembunyikan Saldo" else "Tampilkan Saldo",
                            tint = StockSlate400,
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
                    val formattedBalance = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("id-ID")).apply {
                        maximumFractionDigits = 0
                    }.format(totalCashBalance).replace(" ", "")

                    Text(
                        text = if (isBalanceVisible) formattedBalance else "Rp ••••••••",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = StockSlate900,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            HorizontalDivider(color = StockSlate100, thickness = 1.dp)

            // 3-Column Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Col 1: Kas Hari Ini
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
                        color = StockSlate400
                    )
                    val inflowStr = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("id-ID")).apply {
                        maximumFractionDigits = 0
                    }.format(todayCashInflow).replace(" ", "")

                    Text(
                        text = if (isLoading) "-" else "+$inflowStr",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = FinanceBrand700,
                        modifier = Modifier.padding(top = 3.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(StockSlate100)
                )

                // Col 2: Piutang Aktif
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
                        color = StockSlate400
                    )
                    val recStr = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("id-ID")).apply {
                        maximumFractionDigits = 0
                    }.format(totalReceivable).replace(" ", "")

                    Text(
                        text = if (isLoading) "-" else recStr,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = StockSlate800,
                        modifier = Modifier.padding(top = 3.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(StockSlate100)
                )

                // Col 3: Hutang Tempo
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
                        color = StockSlate400
                    )
                    val payStr = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("id-ID")).apply {
                        maximumFractionDigits = 0
                    }.format(totalPayable).replace(" ", "")

                    Text(
                        text = if (isLoading) "-" else payStr,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = StockSlate800,
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
private fun FinanceHubItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    badgeDotColor: Color? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(FinanceBrand50),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = FinanceBrand800,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(
                modifier = Modifier.padding(end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    color = StockSlate800,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = StockSlate400,
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
            if (badgeText != null) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (badgeDotColor != null) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(badgeDotColor)
                            )
                        }
                        Text(
                            text = badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = StockSlate700
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = StockSlate300,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private val StockBrand50 = Color(0xFFEEF7F2)
private val StockBrand100 = Color(0xFFDCFCE7)
private val StockBrand500 = Color(0xFF16A34A)
private val StockBrand600 = Color(0xFF1B5E3C)
private val StockBrand700 = Color(0xFF1B5E3C)
private val StockBrand800 = Color(0xFF14532D)

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
    layout: TbLayoutInfo,
    content: BackofficeHubContent,
    inventoryRepository: InventoryRepository? = null,
    syncMonitoringRepository: SyncMonitoringRepository? = null,
    onNavigate: (AdminDestination) -> Unit,
) {
    // Load inventory valuation and stats
    var valuationSummary by remember { mutableStateOf<com.tbterminal.app.data.costing.InventoryValuationSummary?>(null) }
    var lowStockCount by remember { mutableIntStateOf(0) }
    var totalProductsCount by remember { mutableIntStateOf(0) }
    var totalCategoriesCount by remember { mutableIntStateOf(0) }
    var totalUnitsCount by remember { mutableIntStateOf(0) }
    var syncStatusText by remember { mutableStateOf("Tersinkron") }
    var isSyncHealthy by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(inventoryRepository != null) }

    LaunchedEffect(inventoryRepository) {
        if (inventoryRepository != null) {
            isLoading = true
            runCatching {
                val result = inventoryRepository.getProductStocks(page = 1, limit = 100)
                if (result is RepositoryResult.Success) {
                    val products = result.data.data
                    valuationSummary = com.tbterminal.app.data.costing.MovingAverageCalculator.calculateInventoryValuation(products)
                    lowStockCount = products.count { it.isActive && it.quantity <= it.minStock }
                    totalProductsCount = result.data.total.toInt().coerceAtLeast(products.size)
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
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // BEGIN: ValuationDashboardCard
        StockValuationDashboardCard(
            valuationSummary = valuationSummary,
            totalProductsCount = totalProductsCount,
            lowStockCount = lowStockCount,
            syncStatusText = syncStatusText,
            isSyncHealthy = isSyncHealthy,
            isLoading = isLoading,
            onLowStockClick = { onNavigate(AdminDestination.Products) }
        )
        // END: ValuationDashboardCard

        // BEGIN: SectionCatalogPricing
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "KATALOG & HARGA",
                color = StockSlate500,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, StockSlate200.copy(alpha = 0.8f)),
            ) {
                Column {
                    StockHubItem(
                        icon = Icons.Outlined.Inventory2,
                        title = "Daftar Produk",
                        subtitle = if (totalProductsCount > 0) "$totalProductsCount produk terdaftar" else "Katalog seluruh produk, stok fisik toko, & status aktif",
                        onClick = { onNavigate(AdminDestination.Products) },
                    )
                    HorizontalDivider(color = StockSlate100, thickness = 1.dp)
                    StockHubItem(
                        icon = Icons.Outlined.LocalOffer,
                        title = "Harga & Margin",
                        subtitle = "Kelola margin keuntungan & HPP",
                        onClick = { onNavigate(AdminDestination.PriceManagement) },
                    )
                }
            }
        }
        // END: SectionCatalogPricing

        // BEGIN: SectionMutationAudit
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "MUTASI & AUDIT STOK",
                color = StockSlate500,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, StockSlate200.copy(alpha = 0.8f)),
            ) {
                Column {
                    StockHubItem(
                        icon = Icons.AutoMirrored.Outlined.ListAlt,
                        title = "Kartu Stok",
                        subtitle = "Riwayat & log arus keluar-masuk",
                        onClick = { onNavigate(AdminDestination.StockReport) },
                    )
                    HorizontalDivider(color = StockSlate100, thickness = 1.dp)
                    StockHubItem(
                        icon = Icons.Outlined.Tune,
                        title = "Stok Opname",
                        subtitle = "Pencocokan & selisih fisik",
                        onClick = { onNavigate(AdminDestination.StockOpname) },
                    )
                }
            }
        }
        // END: SectionMutationAudit

        // BEGIN: SectionMasterData
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "MASTER DATA",
                color = StockSlate500,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, StockSlate200.copy(alpha = 0.8f)),
            ) {
                Column {
                    StockHubItem(
                        icon = Icons.Outlined.Category,
                        title = "Kategori Produk",
                        subtitle = if (totalCategoriesCount > 0) "$totalCategoriesCount kategori terdaftar" else "Kelompok & rak etalase",
                        onClick = { onNavigate(AdminDestination.ProductCategories) },
                    )
                    HorizontalDivider(color = StockSlate100, thickness = 1.dp)
                    StockHubItem(
                        icon = Icons.Outlined.Straighten,
                        title = "Satuan Ukuran",
                        subtitle = if (totalUnitsCount > 0) "$totalUnitsCount satuan ukuran terdaftar" else "Pcs, sak, batang, dus, meter",
                        onClick = { onNavigate(AdminDestination.ProductUnits) },
                    )
                }
            }
        }
        // END: SectionMasterData
    }
}

@Composable
private fun StockValuationDashboardCard(
    valuationSummary: com.tbterminal.app.data.costing.InventoryValuationSummary?,
    totalProductsCount: Int,
    lowStockCount: Int,
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
        border = BorderStroke(1.dp, StockSlate200.copy(alpha = 0.8f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: "VALUASI STOK TOKO" and Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VALUASI STOK TOKO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StockSlate400,
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
                        color = StockBrand50,
                        border = BorderStroke(1.dp, StockBrand100)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(StockBrand500))
                            Text(
                                text = "Stok Aman",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = StockBrand700
                            )
                        }
                    }
                }
            }

            // Asset Value Row
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Nilai Aset (HPP)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = StockSlate500
                    )
                    IconButton(
                        onClick = { isValuationVisible = !isValuationVisible },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isValuationVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = if (isValuationVisible) "Sembunyikan nilai aset" else "Tampilkan nilai aset",
                            tint = StockSlate400,
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
                    val formattedValuation = valuationSummary?.totalValuation?.let {
                        java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("id-ID")).apply {
                            maximumFractionDigits = 0
                        }.format(it).replace(" ", "")
                    } ?: "Rp0"

                    Text(
                        text = if (isValuationVisible) formattedValuation else "Rp ••••••••",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = StockSlate900,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            HorizontalDivider(color = StockSlate100, thickness = 1.dp)

            // 3-Column Split Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column 1: Katalog Aktif
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Katalog Aktif",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = StockSlate400
                    )
                    Text(
                        text = if (isLoading) "-" else "$totalProductsCount Item",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StockSlate800,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(StockSlate100)
                )

                // Column 2: Perlu Restok
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
                        color = StockSlate400
                    )
                    Text(
                        text = if (isLoading) "-" else "$lowStockCount Item",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (lowStockCount > 0) Color(0xFFB45309) else StockSlate800,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(StockSlate100)
                )

                // Column 3: Status Sync
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Status Sync",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = StockSlate400
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
                                .background(if (isSyncHealthy) StockBrand500 else Color(0xFFD97706))
                        )
                        Text(
                            text = syncStatusText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StockSlate800,
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
private fun StockHubItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
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
                    .background(StockBrand50),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = StockBrand700,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    color = StockSlate800,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    color = StockSlate500,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = StockSlate400,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun TransactionsHub(
    layout: TbLayoutInfo,
    role: String,
    analyticsRepository: AnalyticsRepository? = null,
    cashReconciliationRepository: CashReconciliationRepository? = null,
    purchasingRepository: PurchasingRepository? = null,
    onNavigate: (AdminDestination) -> Unit,
) {
    var mode by remember { mutableStateOf("penjualan") }
    var todayRevenue by remember { mutableStateOf(BigDecimal.ZERO) }
    var todayTrxCount by remember { mutableIntStateOf(0) }
    var hasActiveSession by remember { mutableStateOf(false) }

    var monthlyPurchases by remember { mutableStateOf(BigDecimal.ZERO) }
    var monthlyPurchasesCount by remember { mutableIntStateOf(0) }

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
                    is RepositoryResult.Success -> {
                        hasActiveSession = sessionRes.data != null
                    }
                    else -> Unit
                }
            }
            runCatching {
                val todayStr = java.time.LocalDate.now().toString()
                when (val trxRes = cashReconciliationRepository.getTransactions(page = 1, limit = 50, sessionId = null, startDate = todayStr, endDate = todayStr)) {
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
                val now = java.time.LocalDate.now()
                val startOfMonth = now.withDayOfMonth(1).toString()
                when (val purchasesRes = purchasingRepository.getPurchases(page = 1, limit = 50, startDate = startOfMonth)) {
                    is RepositoryResult.Success -> {
                        monthlyPurchasesCount = purchasesRes.data.total.toInt().coerceAtLeast(purchasesRes.data.data.size)
                        monthlyPurchases = purchasesRes.data.data.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.total) }
                    }
                    else -> {
                        when (val payRes = purchasingRepository.getPayables(page = 1, limit = 50)) {
                            is RepositoryResult.Success -> {
                                monthlyPurchasesCount = payRes.data.total.toInt().coerceAtLeast(payRes.data.data.size)
                                monthlyPurchases = payRes.data.data.fold(BigDecimal.ZERO) { acc, p -> acc.add(p.amount) }
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    val formattedSalesToday = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("id-ID")).apply {
        maximumFractionDigits = 0
    }.format(todayRevenue).replace(" ", "")

    val formattedPurchasesMonth = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("id-ID")).apply {
        maximumFractionDigits = 0
    }.format(monthlyPurchases).replace(" ", "")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction-hub-content")
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Segmented Control
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab Penjualan
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { mode = "penjualan" },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 10.dp)) {
                        Text(
                            text = "Penjualan",
                            color = if (mode == "penjualan") Color(0xFF1B5E3C) else Color(0xFF64748B),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                    if (mode == "penjualan") {
                        Box(modifier = Modifier.align(Alignment.BottomCenter).width(50.dp).height(3.dp).background(Color(0xFF1B5E3C), RoundedCornerShape(50)))
                    }
                }
                // Tab Pembelian
                Box(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { mode = "pembelian" },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 10.dp)) {
                        Text(
                            text = "Pembelian",
                            color = if (mode == "pembelian") Color(0xFF1B5E3C) else Color(0xFF64748B),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                    if (mode == "pembelian") {
                        Box(modifier = Modifier.align(Alignment.BottomCenter).width(50.dp).height(3.dp).background(Color(0xFF1B5E3C), RoundedCornerShape(50)))
                    }
                }
            }
        }
        
        // Content Section
        if (mode == "penjualan") {
            // Penjualan Stats (White Background, Green Border)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFF1B5E3C)),
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
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFFEEF7F2),
                            border = BorderStroke(1.dp, Color(0xFFD1FAE5))
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
                                        .background(Color(0xFF1B5E3C))
                                )
                                Text(
                                    text = if (hasActiveSession) "Kasir Aktif" else "Hari Ini",
                                    color = Color(0xFF1B5E3C),
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
                                color = Color(0xFF0F172A),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = if (todayTrxCount > 0) "$todayTrxCount Transaksi Berhasil" else "Belum ada transaksi hari ini",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEEF7F2),
                            border = BorderStroke(1.dp, Color(0xFFD1FAE5))
                        ) {
                            Text(
                                text = if (hasActiveSession) "Shift Aktif" else "Total Penjualan",
                                color = Color(0xFF1B5E3C),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            
            Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    shadowElevation = 1.dp
                ) {
                    Column {
                        // Riwayat Penjualan
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onNavigate(AdminDestination.SalesTransactions) }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).background(Color(0xFFEEF7F2), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null, tint = Color(0xFF1B5E3C), modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Riwayat penjualan", color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Cari dan periksa transaksi penjualan", color = Color(0xFF64748B), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        } else {
            // Pembelian Stats (White Background, Green Border)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFF1B5E3C)),
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
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFFEEF7F2),
                            border = BorderStroke(1.dp, Color(0xFFD1FAE5))
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
                                        .background(Color(0xFF1B5E3C))
                                )
                                Text(
                                    text = "Bulan Berjalan",
                                    color = Color(0xFF1B5E3C),
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
                                text = formattedPurchasesMonth,
                                color = Color(0xFF0F172A),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = if (monthlyPurchasesCount > 0) "$monthlyPurchasesCount Faktur dari Supplier" else "Belum ada pembelian bulan ini",
                                color = Color(0xFF64748B),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEEF7F2),
                            border = BorderStroke(1.dp, Color(0xFFD1FAE5))
                        ) {
                            Text(
                                text = "Pengadaan",
                                color = Color(0xFF1B5E3C),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            
            Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    shadowElevation = 1.dp
                ) {
                    Column {
                        // Barang Masuk
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onNavigate(AdminDestination.IncomingGoods) }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).background(Color(0xFFEEF7F2), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.LocalShipping, contentDescription = null, tint = Color(0xFF1B5E3C), modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Barang masuk / PO", color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Proses barang dari supplier", color = Color(0xFF64748B), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        
                        // Riwayat Pembelian
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onNavigate(AdminDestination.PurchaseHistory) }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).background(Color(0xFFEEF7F2), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.ListAlt, contentDescription = null, tint = Color(0xFF1B5E3C), modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Riwayat pembelian", color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Cari dan periksa transaksi pembelian", color = Color(0xFF64748B), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun BackofficeHubAction.icon(): ImageVector = when (this) {
    BackofficeHubAction.SALES_HISTORY -> Icons.AutoMirrored.Outlined.ReceiptLong
    BackofficeHubAction.NEW_SALE -> Icons.Outlined.PointOfSale
    BackofficeHubAction.PURCHASE_HISTORY -> Icons.AutoMirrored.Outlined.ListAlt
    BackofficeHubAction.INCOMING_GOODS -> Icons.Outlined.LocalShipping
    BackofficeHubAction.RECEIVABLES -> Icons.Outlined.Payments
    BackofficeHubAction.SUPPLIER_DEBTS -> Icons.Outlined.AccountBalanceWallet
    BackofficeHubAction.DAILY_CASH -> Icons.Outlined.AssignmentTurnedIn
    BackofficeHubAction.PRODUCTS_STOCK -> Icons.Outlined.Inventory2
    BackofficeHubAction.ADJUST_STOCK -> Icons.Outlined.Tune
    BackofficeHubAction.STOCK_CARD -> Icons.AutoMirrored.Outlined.ListAlt
    BackofficeHubAction.PRODUCT_PRICES -> Icons.Outlined.Payments
    BackofficeHubAction.CATEGORIES -> Icons.Outlined.Category
    BackofficeHubAction.UNITS -> Icons.Outlined.Straighten
}

@StringRes
private fun BackofficeHubAction.financeDescriptionRes(): Int = when (this) {
    BackofficeHubAction.RECEIVABLES -> R.string.hub_receivables_description
    BackofficeHubAction.SUPPLIER_DEBTS -> R.string.hub_supplier_debts_description
    BackofficeHubAction.DAILY_CASH -> R.string.hub_daily_cash_description
    else -> error("Aksi $name bukan bagian halaman keuangan")
}

@StringRes
private fun BackofficeHubAction.transactionDescriptionRes(): Int = when (this) {
    BackofficeHubAction.SALES_HISTORY -> R.string.hub_sales_history_description
    BackofficeHubAction.NEW_SALE -> R.string.hub_new_sale_description
    BackofficeHubAction.PURCHASE_HISTORY -> R.string.hub_purchase_history_description
    BackofficeHubAction.INCOMING_GOODS -> R.string.hub_incoming_goods_description
    else -> error("Aksi $name bukan bagian halaman transaksi")
}






