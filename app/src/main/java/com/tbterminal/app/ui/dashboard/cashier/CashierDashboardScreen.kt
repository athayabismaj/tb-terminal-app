package com.tbterminal.app.ui.dashboard.cashier

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PauseCircleOutline
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Storefront
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.local.cashexpense.LocalCashExpenseService
import com.tbterminal.app.data.local.database.CashSessionLocalDataSource
import com.tbterminal.app.data.model.CashTransaction
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.ui.checkout.components.MaterialEstimatorDialog
import com.tbterminal.app.ui.common.toDisplayTime
import com.tbterminal.app.ui.common.toRupiahText
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.components.SkeletonCard
import com.tbterminal.app.ui.components.TopFloatingCapsule

// ═════════════════════════════════════════════════════════════════════════════
// INDUSTRIAL POS DESIGN TOKENS
// Harmonized with Terminal Barokah Jaya Design System & Global POS Standards
// (Moka POS, Toast POS, Square POS, Clover)
// ═════════════════════════════════════════════════════════════════════════════
private val SurfaceBg = Color(0xFFF8FAFC)
private val Slate900 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate600 = Color(0xFF475569)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate300 = Color(0xFFCBD5E1)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate50 = Color(0xFFF8FAFC)

private val BrandForest = Color(0xFF10533C)
private val BrandForestDark = Color(0xFF0B3828)
private val BrandGreenLight = Color(0xFFF0FDF4)
private val BrandGreenBorder = Color(0xFFBBF7D0)
private val Emerald500 = Color(0xFF10B981)
private val Emerald700 = Color(0xFF047857)

private val CardBorder = Color(0xFFE2E8F0)

private val AmberBg = Color(0xFFFFFBEB)
private val AmberBorder = Color(0xFFFDE68A)
private val AmberText = Color(0xFFD97706)

private val IndigoBg = Color(0xFFEEF2FF)
private val IndigoBorder = Color(0xFFC7D2FE)
private val IndigoText = Color(0xFF4338CA)

private val TealBg = Color(0xFFF0FDFA)
private val TealBorder = Color(0xFF99F6E4)
private val TealText = Color(0xFF0D9488)

private val DangerRed = Color(0xFFDC2626)
private val DangerBg = Color(0xFFFEF2F2)
private val DangerBorder = Color(0xFFFECACA)

@Composable
fun CashierDashboardScreen(
    name: String,
    role: String,
    cashReconciliationRepository: CashReconciliationRepository,
    cashSessionLocalDataSource: CashSessionLocalDataSource? = null,
    localCashExpenseService: LocalCashExpenseService? = null,
    onDashboardClick: () -> Unit = {},
    onPosClick: () -> Unit = {},
    onCashSessionClick: () -> Unit = {},
    onTransactionHistoryClick: () -> Unit = {},
    onStockCheckClick: () -> Unit = {},
    onReceivablesClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: CashierDashboardViewModel = viewModel(
        factory = CashierDashboardViewModel.factory(
            repository = cashReconciliationRepository,
            cashSessionLocalDataSource = cashSessionLocalDataSource,
            localCashExpenseService = localCashExpenseService
        )
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CashierDashboardShell(
        userName = name,
        role = role,
        activeDestination = CashierDestination.Dashboard,
        onDashboardClick = onDashboardClick,
        onPosClick = onPosClick,
        onCashSessionClick = onCashSessionClick,
        onTransactionHistoryClick = onTransactionHistoryClick,
        onStockCheckClick = onStockCheckClick,
        onReceivablesClick = onReceivablesClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    ) { contentModifier ->
        Box(modifier = contentModifier.fillMaxSize()) {
            TopFloatingCapsule(
                message = uiState.errorMessage,
                isError = true,
                onDismiss = viewModel::clearErrorMessage
            )

            RefreshableContent(
                isRefreshing = uiState.isLoading && (uiState.activeSession != null || uiState.recentTransactions.isNotEmpty()),
                onRefresh = viewModel::loadDashboardData,
                modifier = Modifier.fillMaxSize(),
            ) {
                CashierDashboardContent(
                    cashierName = name,
                    uiState = uiState,
                    onPosClick = onPosClick,
                    onCashSessionClick = onCashSessionClick,
                    onTransactionHistoryClick = onTransactionHistoryClick,
                    onStockCheckClick = onStockCheckClick,
                    onRefresh = viewModel::loadDashboardData,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun CashierDashboardContent(
    cashierName: String,
    uiState: CashierDashboardUiState,
    onPosClick: () -> Unit,
    onCashSessionClick: () -> Unit,
    onTransactionHistoryClick: () -> Unit,
    onStockCheckClick: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPrivacyHidden by rememberSaveable { mutableStateOf(false) }
    var showMaterialEstimator by remember { mutableStateOf(false) }

    val displayName = remember(cashierName) {
        val trimmed = cashierName.trim()
        if (trimmed.isEmpty() || trimmed.equals("kasir", ignoreCase = true)) {
            "Kasir"
        } else {
            trimmed
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceBg),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── 1. OPERATIONAL CONTEXT HEADER ──
            CashierOperationalHeader(
                displayName = displayName,
                uiState = uiState,
                onRefresh = onRefresh
            )

            if (uiState.isLoading && uiState.activeSession == null && uiState.recentTransactions.isEmpty()) {
                SkeletonCard()
                SkeletonCard()
            } else {
                // ── 2. PRIMARY POS CTA HERO CARD (STANDAR INDUSTRI KASIR) ──
                CashierHeroCtaCard(
                    isShiftActive = uiState.activeSession != null,
                    onPosClick = onPosClick,
                    onOpenSessionClick = onCashSessionClick
                )

                // ── 3. SHIFT METRICS & CASH DRAWER CARD ──
                CashierShiftMetricsCard(
                    uiState = uiState,
                    isPrivacyHidden = isPrivacyHidden,
                    onTogglePrivacy = { isPrivacyHidden = !isPrivacyHidden },
                    onCashSessionClick = onCashSessionClick
                )

                // ── 4. SHIFT TENDER BREAKDOWN CARD (METODE PEMBAYARAN) ──
                CashierTenderBreakdownCard(
                    uiState = uiState,
                    isPrivacyHidden = isPrivacyHidden
                )

                // ── 5. OPERATIONAL LAUNCHPAD (AKSI CEPAT 6 GRID) ──
                CashierQuickActionsGrid(
                    onPosClick = onPosClick,
                    onCashSessionClick = onCashSessionClick,
                    onHeldCartsClick = onPosClick,
                    onTransactionHistoryClick = onTransactionHistoryClick,
                    onStockCheckClick = onStockCheckClick,
                    onMaterialEstimatorClick = { showMaterialEstimator = true }
                )

                // ── 6. LIVE RECENT TRANSACTIONS FEED ──
                CashierRecentTransactionsCard(
                    recentTransactions = uiState.recentTransactions,
                    onViewAllClick = onTransactionHistoryClick,
                    onStartFirstTransactionClick = onPosClick
                )
            }
        }
    }

    // Material Estimator Dialog Modal for Building Materials
    if (showMaterialEstimator) {
        MaterialEstimatorDialog(
            onDismiss = { showMaterialEstimator = false },
            onApplySearchQuery = {
                showMaterialEstimator = false
                onPosClick()
            }
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 1. OPERATIONAL CONTEXT HEADER
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun CashierOperationalHeader(
    displayName: String,
    uiState: CashierDashboardUiState,
    onRefresh: () -> Unit
) {
    val isShiftActive = uiState.activeSession != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Halo, $displayName",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    letterSpacing = (-0.4).sp
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Slate100,
                    border = BorderStroke(1.dp, Slate200)
                ) {
                    Text(
                        text = "POS-01",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate600,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Storefront,
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = "Terminal Barokah Jaya",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate500
                )
                if (isShiftActive && uiState.shiftOpenedTimeFormatted != null) {
                    Text(
                        text = "• Buka ${uiState.shiftOpenedTimeFormatted}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500
                    )
                }
            }
        }

        // Live Shift Status Pill + Refresh Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isShiftActive) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.35f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = BrandGreenLight,
                    border = BorderStroke(1.dp, BrandGreenBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Emerald500.copy(alpha = alpha))
                        )
                        Text(
                            text = if (uiState.shiftDurationFormatted != null) {
                                "Shift ${uiState.shiftDurationFormatted}"
                            } else {
                                "Shift Aktif"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandForest
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = AmberBg,
                    border = BorderStroke(1.dp, AmberBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(AmberText)
                        )
                        Text(
                            text = "Shift Ditutup",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberText
                        )
                    }
                }
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Slate200, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Muat Ulang",
                    tint = Slate600,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 2. PRIMARY POS CTA HERO CARD
// Primary industry focus: Quick one-tap entry to point of sale screen
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun CashierHeroCtaCard(
    isShiftActive: Boolean,
    onPosClick: () -> Unit,
    onOpenSessionClick: () -> Unit
) {
    if (isShiftActive) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onPosClick),
            shape = RoundedCornerShape(16.dp),
            color = BrandForest,
            border = BorderStroke(1.dp, BrandForestDark),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PointOfSale,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "BUKA MESIN KASIR (POS)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            text = "Input item, scan barcode & terima pembayaran",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = "Buka POS",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    } else {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenSessionClick),
            shape = RoundedCornerShape(16.dp),
            color = AmberBg,
            border = BorderStroke(1.dp, AmberBorder),
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .border(1.dp, AmberBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LockOpen,
                            contentDescription = null,
                            tint = AmberText,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Shift Belum Dibuka",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate900
                        )
                        Text(
                            text = "Buka sesi & masukkan modal awal untuk mulai melayani",
                            fontSize = 11.sp,
                            color = Slate600,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Button(
                    onClick = onOpenSessionClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandForest,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text(
                        text = "Buka Shift",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 3. SHIFT METRICS & CASH DRAWER CARD
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun CashierShiftMetricsCard(
    uiState: CashierDashboardUiState,
    isPrivacyHidden: Boolean,
    onTogglePrivacy: () -> Unit,
    onCashSessionClick: () -> Unit
) {
    val isShiftActive = uiState.activeSession != null

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row: Label Ringkasan + Privacy Eye Button + Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "RINGKASAN SHIFT KASIR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        letterSpacing = 0.8.sp
                    )

                    IconButton(
                        onClick = onTogglePrivacy,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = if (isPrivacyHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (isPrivacyHidden) "Tampilkan Nominal" else "Sembunyikan Nominal",
                            tint = Slate400,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Text(
                    text = if (isShiftActive) "Sesi Berjalan" else "Sesi Ditutup",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isShiftActive) Emerald700 else Slate400
                )
            }

            // Main Metric: Penjualan Shift Saya
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "Total Penjualan Shift Saya",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate500
                )

                Text(
                    text = if (isPrivacyHidden) "••••••••" else uiState.todaySalesRevenue.toRupiahText(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900,
                    letterSpacing = (-0.5).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = Emerald700,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isShiftActive) {
                            "${uiState.todayTransactionsCount} transaksi · Rata-rata ${if (isPrivacyHidden) "••••" else uiState.averageTicket.toRupiahText()}"
                        } else {
                            "Belum ada transaksi shift yang tercatat"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Emerald700
                    )
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            // Sub Metrics 3-Grid: Estimasi Kas di Laci, Modal Awal Shift, & Kas Kecil Net
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Item 1: Kas di Laci (Physical Drawer Cash)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Slate50)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Kas Fisik Laci",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500
                    )
                    Text(
                        text = if (isPrivacyHidden) "••••••" else uiState.systemCash.toRupiahText(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Modal + Tunai",
                        fontSize = 9.sp,
                        color = Slate400
                    )
                }

                // Item 2: Modal Awal (Starting Float)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Slate50)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Modal Awal",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500
                    )
                    Text(
                        text = if (isPrivacyHidden) "••••••" else uiState.openingCash.toRupiahText(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Uang awal laci",
                        fontSize = 9.sp,
                        color = Slate400
                    )
                }

                // Item 3: Kas Kecil Net (Petty Cash Flow)
                val netPettyCash = uiState.totalCashIn - uiState.totalExpenses
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Slate50)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Kas Kecil Net",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500
                    )
                    Text(
                        text = if (isPrivacyHidden) "••••••" else netPettyCash.toRupiahText(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (netPettyCash >= java.math.BigDecimal.ZERO) Slate800 else DangerRed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Masuk - Keluar",
                        fontSize = 9.sp,
                        color = Slate400
                    )
                }
            }

            // Action Button: Kelola Kas & Tutup Shift
            if (isShiftActive) {
                OutlinedButton(
                    onClick = onCashSessionClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CardBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Slate50,
                        contentColor = Slate800
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = BrandForest
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Kelola Kas & Tutup Shift (Z-Report)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 4. SHIFT TENDER BREAKDOWN CARD (METODE PEMBAYARAN)
// Industry standard: Cashiers must know tender split (Cash vs Digital vs Credit)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun CashierTenderBreakdownCard(
    uiState: CashierDashboardUiState,
    isPrivacyHidden: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "RINCIAN METODE PEMBAYARAN SHIFT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 0.8.sp
                )

                Text(
                    text = "Tersinkron POS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate400
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Tunai di Laci (Cash)
                TenderItem(
                    label = "Tunai (Laci)",
                    amount = if (isPrivacyHidden) "••••••" else uiState.cashRevenue.toRupiahText(),
                    badge = "Fisik Laci",
                    icon = Icons.Outlined.AccountBalanceWallet,
                    iconTint = BrandForest,
                    badgeBg = BrandGreenLight,
                    badgeBorder = BrandGreenBorder,
                    badgeText = BrandForest,
                    modifier = Modifier.weight(1f)
                )

                // 2. QRIS & Transfer Bank
                TenderItem(
                    label = "QRIS / Transfer",
                    amount = if (isPrivacyHidden) "••••••" else uiState.qrisRevenue.toRupiahText(),
                    badge = "Bank / EDC",
                    icon = Icons.Outlined.QrCode2,
                    iconTint = IndigoText,
                    badgeBg = IndigoBg,
                    badgeBorder = IndigoBorder,
                    badgeText = IndigoText,
                    modifier = Modifier.weight(1f)
                )

                // 3. Nota Tempo / Piutang Pelanggan
                TenderItem(
                    label = "Nota Tempo",
                    amount = if (isPrivacyHidden) "••••••" else uiState.receivableRevenue.toRupiahText(),
                    badge = "Tempo Proyek",
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    iconTint = AmberText,
                    badgeBg = AmberBg,
                    badgeBorder = AmberBorder,
                    badgeText = AmberText,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TenderItem(
    label: String,
    amount: String,
    badge: String,
    icon: ImageVector,
    iconTint: Color,
    badgeBg: Color,
    badgeBorder: Color,
    badgeText: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Slate50,
        border = BorderStroke(1.dp, Slate200)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeBg,
                    border = BorderStroke(1.dp, badgeBorder)
                ) {
                    Text(
                        text = badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeText,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate500
                )
                Text(
                    text = amount,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 5. OPERATIONAL LAUNCHPAD (AKSI CEPAT 6 GRID)
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun CashierQuickActionsGrid(
    onPosClick: () -> Unit,
    onCashSessionClick: () -> Unit,
    onHeldCartsClick: () -> Unit,
    onTransactionHistoryClick: () -> Unit,
    onStockCheckClick: () -> Unit,
    onMaterialEstimatorClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "AKSI CEPAT OPERASIONAL",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Slate400,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        // Row 1: Mulai POS, Kas Kecil, Keranjang Ditahan (Hold/Parkir)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CashierLaunchpadItem(
                title = "Mulai POS",
                subtitle = "Jual Barang",
                icon = Icons.Outlined.PointOfSale,
                iconTint = BrandForest,
                bgIcon = BrandGreenLight,
                onClick = onPosClick,
                modifier = Modifier.weight(1f)
            )

            CashierLaunchpadItem(
                title = "Kas Kecil",
                subtitle = "Masuk/Keluar",
                icon = Icons.Outlined.Payments,
                iconTint = Emerald700,
                bgIcon = BrandGreenLight,
                onClick = onCashSessionClick,
                modifier = Modifier.weight(1f)
            )

            CashierLaunchpadItem(
                title = "Hold Cart",
                subtitle = "Parkir Nota",
                icon = Icons.Outlined.PauseCircleOutline,
                iconTint = IndigoText,
                bgIcon = IndigoBg,
                onClick = onHeldCartsClick,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Riwayat Shift, Cek Stok, Kalkulator Material Bangunan
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CashierLaunchpadItem(
                title = "Riwayat",
                subtitle = "Struk Shift",
                icon = Icons.Outlined.History,
                iconTint = Slate700,
                bgIcon = Slate100,
                onClick = onTransactionHistoryClick,
                modifier = Modifier.weight(1f)
            )

            CashierLaunchpadItem(
                title = "Cek Stok",
                subtitle = "Harga & Sisa",
                icon = Icons.Outlined.Inventory2,
                iconTint = AmberText,
                bgIcon = AmberBg,
                onClick = onStockCheckClick,
                modifier = Modifier.weight(1f)
            )

            CashierLaunchpadItem(
                title = "Kalkulator",
                subtitle = "Bahan Material",
                icon = Icons.Outlined.Calculate,
                iconTint = TealText,
                bgIcon = TealBg,
                onClick = onMaterialEstimatorClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CashierLaunchpadItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    bgIcon: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgIcon),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(19.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subtitle,
                    fontSize = 9.sp,
                    color = Slate400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 6. RECENT TRANSACTIONS FEED
// ═════════════════════════════════════════════════════════════════════════════
private data class DisplayTransactionItem(
    val title: String,
    val subtitle: String,
    val timeMethod: String,
    val amount: String,
    val status: String,
    val isPaid: Boolean
)

@Composable
private fun CashierRecentTransactionsCard(
    recentTransactions: List<CashTransaction>,
    onViewAllClick: () -> Unit,
    onStartFirstTransactionClick: () -> Unit
) {
    val items = remember(recentTransactions) {
        recentTransactions.take(5).map { trx ->
            val receiptNo = if (trx.receiptId.isNotBlank()) {
                val code = trx.receiptId.takeLast(6).uppercase()
                "#TRX-$code"
            } else {
                "#TRX-${trx.id.takeLast(4).uppercase()}"
            }
            val customer = if (!trx.customerName.isNullOrBlank()) trx.customerName else "Pelanggan Umum"
            val method = trx.paymentMethods.firstOrNull()?.uppercase() ?: "TUNAI"
            val timeStr = trx.createdAt.toDisplayTime()
            val hasRemaining = trx.remainingAmount != null && trx.remainingAmount > java.math.BigDecimal.ZERO
            val isLunas = !hasRemaining && (
                trx.status.equals("COMPLETED", ignoreCase = true) ||
                trx.status.equals("PAID", ignoreCase = true) ||
                trx.status.equals("LUNAS", ignoreCase = true)
            )
            val isBatal = trx.status.equals("BATAL", ignoreCase = true) || trx.status.equals("VOIDED", ignoreCase = true)

            val statusText = when {
                isBatal -> "Batal"
                isLunas -> "Lunas"
                hasRemaining || trx.status.equals("PARTIAL", ignoreCase = true) || trx.status.equals("UNPAID", ignoreCase = true) || trx.status.equals("HUTANG", ignoreCase = true) -> "Tempo"
                else -> trx.status
            }

            DisplayTransactionItem(
                title = receiptNo,
                subtitle = customer,
                timeMethod = "$timeStr WIB · $method",
                amount = trx.total.toRupiahText(),
                status = statusText,
                isPaid = isLunas
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: Dot + Transaksi Shift Terkini + Lihat Semua Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Emerald500, CircleShape)
                    )
                    Text(
                        text = "Transaksi Shift Terkini",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onViewAllClick)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Lihat Semua",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandForest
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = BrandForest,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Slate100),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "Belum ada transaksi pada shift ini",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate700
                        )
                        Text(
                            text = "Penjualan dari mesin POS akan tampil live di sini secara otomatis.",
                            fontSize = 11.sp,
                            color = Slate400,
                            textAlign = TextAlign.Center
                        )
                        OutlinedButton(
                            onClick = onStartFirstTransactionClick,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, CardBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandForest),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PointOfSale,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Mulai Transaksi Pertama",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEachIndexed { index, item ->
                        if (index > 0) {
                            HorizontalDivider(color = Slate100, thickness = 1.dp)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable(onClick = onViewAllClick)
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(BrandGreenLight)
                                        .border(1.dp, BrandGreenBorder, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                        contentDescription = null,
                                        tint = BrandForest,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    Text(
                                        text = item.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Slate900,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.subtitle,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Slate600,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.timeMethod,
                                        fontSize = 10.sp,
                                        color = Slate400,
                                        maxLines = 1
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = item.amount,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (item.isPaid) BrandGreenLight else AmberBg,
                                    border = BorderStroke(1.dp, if (item.isPaid) BrandGreenBorder else AmberBorder)
                                ) {
                                    Text(
                                        text = item.status,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.isPaid) Emerald700 else AmberText,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
