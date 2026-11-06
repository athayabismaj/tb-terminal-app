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
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.PauseCircleOutline
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
// Aligned with Terminal Barokah Jaya Cashier Dashboard Specifications
// ═════════════════════════════════════════════════════════════════════════════
private val SurfaceBg = Color(0xFFF8FAF9)
private val PrimaryDark = Color(0xFF004528)
private val PrimaryBrand = Color(0xFF1B5E3C)
private val CardBorder = Color(0xFFE7ECE9)
private val StatusChipBg = Color(0xFFE8F6ED)
private val StatusChipBorder = Color(0xFFD1ECD9)

private val Slate900 = Color(0xFF111827)
private val Slate800 = Color(0xFF1E293B)
private val Slate700 = Color(0xFF334155)
private val Slate600 = Color(0xFF475569)
private val Slate500 = Color(0xFF64748B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate200 = Color(0xFFE2E8F0)
private val Slate100 = Color(0xFFF1F5F9)
private val Slate50 = Color(0xFFF8FAFC)

private val Emerald50 = Color(0xFFEDF7F1)
private val Emerald500 = Color(0xFF10B981)
private val Emerald600 = Color(0xFF059669)
private val Forest700 = Color(0xFF17583D)

private val AmberBg = Color(0xFFFFFBEB)
private val AmberBorder = Color(0xFFFDE68A)
private val AmberText = Color(0xFFD97706)

private val DangerRed = Color(0xFFDC2626)

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
            // ── 1. SAPAAN KASIR & STATUS SHIFT (OVERVIEW) ──
            CashierOperationalHeader(
                displayName = displayName,
                uiState = uiState
            )

            if (uiState.isLoading && uiState.activeSession == null && uiState.recentTransactions.isEmpty()) {
                SkeletonCard()
                SkeletonCard()
            } else {
                // ── 2. BANNER AKSI UTAMA POS REGISTER ──
                CashierHeroCtaCard(
                    isShiftActive = uiState.activeSession != null,
                    onPosClick = onPosClick,
                    onOpenSessionClick = onCashSessionClick
                )

                // ── 3. CARD RINGKASAN SHIFT KASIR ──
                CashierShiftMetricsCard(
                    uiState = uiState,
                    isPrivacyHidden = isPrivacyHidden,
                    onTogglePrivacy = { isPrivacyHidden = !isPrivacyHidden },
                    onCashSessionClick = onCashSessionClick
                )

                // ── 4. RINCIAN METODE PEMBAYARAN SHIFT ──
                CashierTenderBreakdownCard(
                    uiState = uiState,
                    isPrivacyHidden = isPrivacyHidden
                )

                // ── 5. SEKSI AKSI CEPAT OPERASIONAL (6 MENU) ──
                CashierQuickActionsGrid(
                    onPosClick = onPosClick,
                    onCashSessionClick = onCashSessionClick,
                    onHeldCartsClick = onPosClick,
                    onTransactionHistoryClick = onTransactionHistoryClick,
                    onStockCheckClick = onStockCheckClick,
                    onMaterialEstimatorClick = { showMaterialEstimator = true }
                )

                // ── 6. TRANSAKSI SHIFT TERKINI FEED ──
                CashierRecentTransactionsCard(
                    recentTransactions = uiState.recentTransactions,
                    onViewAllClick = onTransactionHistoryClick
                )
            }
        }
    }

    // Modal Kalkulator Estimasi Bahan Bangunan
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
// 1. SAPAAN KASIR & STATUS SHIFT
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun CashierOperationalHeader(
    displayName: String,
    uiState: CashierDashboardUiState
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Halo, [Name] + Live Pulsing Shift Duration Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Halo, $displayName",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    letterSpacing = (-0.3).sp
                )

                if (isShiftActive) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulseShift")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.35f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(900),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseAlpha"
                    )

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Emerald50,
                        border = BorderStroke(1.dp, StatusChipBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Emerald500.copy(alpha = pulseAlpha), CircleShape)
                            )
                            Text(
                                text = uiState.shiftDurationFormatted ?: "Shift Aktif",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Forest700
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
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(AmberText, CircleShape)
                            )
                            Text(
                                text = "Shift Ditutup",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AmberText
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            // Row 2: Status siap melayani + Waktu Buka
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(if (isShiftActive) Emerald500 else Slate400, CircleShape)
                    )
                    Text(
                        text = if (isShiftActive) "Kasir siap melayani transaksi" else "Shift kasir belum dibuka",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate600
                    )
                }

                Text(
                    text = if (isShiftActive && uiState.shiftOpenedTimeFormatted != null) {
                        "Buka ${uiState.shiftOpenedTimeFormatted} WIB"
                    } else if (isShiftActive) {
                        "Shift Berjalan"
                    } else {
                        "Belum Aktif"
                    },
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Normal,
                    color = Slate400
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 2. BANNER AKSI UTAMA POS REGISTER
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun CashierHeroCtaCard(
    isShiftActive: Boolean,
    onPosClick: () -> Unit,
    onOpenSessionClick: () -> Unit
) {
    Surface(
        onClick = if (isShiftActive) onPosClick else onOpenSessionClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Slate200),
        shadowElevation = 0.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isShiftActive) Slate100 else AmberBg)
                        .border(1.dp, if (isShiftActive) Slate200 else AmberBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isShiftActive) Icons.Outlined.PointOfSale else Icons.Outlined.LockOpen,
                        contentDescription = null,
                        tint = if (isShiftActive) PrimaryDark else AmberText,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (isShiftActive) "BUKA TRANSAKSI KASIR (POS)" else "BUKA SHIFT KASIR DAHULU",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        letterSpacing = 0.3.sp
                    )
                    Text(
                        text = if (isShiftActive) {
                            "Input item, scan barcode & terima pembayaran"
                        } else {
                            "Masukkan modal awal untuk mulai melayani transaksi"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = Slate500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Slate100)
                    .border(1.dp, Slate200, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = "Buka POS",
                    tint = Slate600,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 3. CARD RINGKASAN SHIFT KASIR
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
            // Header: RINGKASAN SHIFT KASIR + Toggle Eye + Badge Shift Aktif
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
                        color = Slate500,
                        letterSpacing = 0.8.sp
                    )

                    IconButton(
                        onClick = onTogglePrivacy,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isPrivacyHidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (isPrivacyHidden) "Tampilkan Nominal" else "Sembunyikan Nominal",
                            tint = Slate400,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isShiftActive) StatusChipBg else AmberBg,
                    border = BorderStroke(1.dp, if (isShiftActive) StatusChipBorder else AmberBorder)
                ) {
                    Text(
                        text = if (isShiftActive) "Shift Aktif" else "Shift Ditutup",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isShiftActive) Forest700 else AmberText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                }
            }

            // Main Shift Revenue Highlight
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Penjualan Shift Saya",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate500
                )

                Text(
                    text = if (isPrivacyHidden) "••••••••" else uiState.todaySalesRevenue.toRupiahText(),
                    fontSize = 30.sp,
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
                        tint = Emerald600,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (isShiftActive) {
                            "${uiState.todayTransactionsCount} transaksi berhasil pada shift ini"
                        } else {
                            "Belum ada transaksi shift yang tercatat"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate600
                    )
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            // Metrics Breakdown:
            // Row 1: Estimasi Fisik Laci
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Emerald50),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Payments,
                            contentDescription = null,
                            tint = PrimaryDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "Estimasi Fisik Laci",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        Text(
                            text = "Modal Awal + Tunai Shift",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate500
                        )
                    }
                }

                Text(
                    text = if (isPrivacyHidden) "••••••••" else uiState.systemCash.toRupiahText(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Slate900,
                    letterSpacing = (-0.3).sp
                )
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            // Row 2: Modal Awal & Kas Kecil Net (2 Kolom divided by vertical line)
            val netPettyCash = uiState.totalCashIn - uiState.totalExpenses
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column 1: Modal Awal
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Modal Awal",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate500
                        )
                        Text(
                            text = "Awal",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate400
                        )
                    }
                    Text(
                        text = if (isPrivacyHidden) "••••••" else uiState.openingCash.toRupiahText(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Kas laci masuk",
                        fontSize = 10.sp,
                        color = Slate400
                    )
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(44.dp)
                        .background(Slate100)
                )

                // Column 2: Kas Kecil Net
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kas Kecil Net",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate500
                        )
                        Text(
                            text = "Mutasi",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate400
                        )
                    }
                    Text(
                        text = if (isPrivacyHidden) "••••••" else netPettyCash.toRupiahText(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (netPettyCash >= java.math.BigDecimal.ZERO) Slate900 else DangerRed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Masuk - keluar",
                        fontSize = 10.sp,
                        color = Slate400
                    )
                }
            }

            // Action Button: Kelola Kas & Tutup Shift
            Surface(
                onClick = onCashSessionClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = PrimaryBrand
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Kelola Kas & Tutup Shift (Z-Report)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 4. RINCIAN METODE PEMBAYARAN SHIFT
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun CashierTenderBreakdownCard(
    uiState: CashierDashboardUiState,
    isPrivacyHidden: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RINCIAN METODE PEMBAYARAN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate500,
                letterSpacing = 0.8.sp
            )

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Slate100,
                border = BorderStroke(1.dp, Slate200)
            ) {
                Text(
                    text = "Shift Ini",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate500,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        // Unified 3-Column Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, CardBorder),
            shadowElevation = 0.5.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column 1: Tunai Laci
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Emerald50),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Payments,
                                contentDescription = null,
                                tint = PrimaryDark,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "Tunai Laci",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate700
                        )
                    }

                    Text(
                        text = if (isPrivacyHidden) "••••••" else uiState.cashRevenue.toRupiahText(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Divider 1
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(38.dp)
                        .background(Slate100)
                )

                // Column 2: QRIS / EDC
                val hasQris = uiState.qrisRevenue > java.math.BigDecimal.ZERO
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Slate50),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.QrCode2,
                                contentDescription = null,
                                tint = Slate600,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "QRIS / EDC",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate600
                        )
                    }

                    Text(
                        text = if (isPrivacyHidden) "••••••" else uiState.qrisRevenue.toRupiahText(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasQris) Slate800 else Slate400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Divider 2
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(38.dp)
                        .background(Slate100)
                )

                // Column 3: Kredit / Bon
                val hasReceivable = uiState.receivableRevenue > java.math.BigDecimal.ZERO
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Slate50),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                contentDescription = null,
                                tint = Slate600,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "Kredit / Bon",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Slate600
                        )
                    }

                    Text(
                        text = if (isPrivacyHidden) "••••••" else uiState.receivableRevenue.toRupiahText(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hasReceivable) Slate800 else Slate400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 5. SEKSI AKSI CEPAT OPERASIONAL (6 MENU)
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AKSI CEPAT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Slate500,
                letterSpacing = 0.8.sp
            )
            Text(
                text = "6 Menu",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate400
            )
        }

        // Row 1: Mulai POS (Primary emerald highlight), Kas Kecil, Parkir Nota
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CashierActionItem(
                title = "Mulai POS",
                subtitle = "Jual Barang",
                icon = Icons.Outlined.PointOfSale,
                iconTint = PrimaryDark,
                iconBg = Emerald50,
                borderColor = StatusChipBorder,
                titleColor = Slate900,
                onClick = onPosClick,
                modifier = Modifier.weight(1f)
            )

            CashierActionItem(
                title = "Kas Kecil",
                subtitle = "Masuk/Keluar",
                icon = Icons.Outlined.Payments,
                iconTint = Slate700,
                iconBg = Slate50,
                borderColor = CardBorder,
                titleColor = Slate800,
                onClick = onCashSessionClick,
                modifier = Modifier.weight(1f)
            )

            CashierActionItem(
                title = "Parkir Nota",
                subtitle = "Hold Cart",
                icon = Icons.Outlined.PauseCircleOutline,
                iconTint = Slate700,
                iconBg = Slate50,
                borderColor = CardBorder,
                titleColor = Slate800,
                onClick = onHeldCartsClick,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Riwayat, Cek Stok, Kalkulator Bangunan
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CashierActionItem(
                title = "Riwayat",
                subtitle = "Struk Shift",
                icon = Icons.Outlined.History,
                iconTint = Slate700,
                iconBg = Slate50,
                borderColor = CardBorder,
                titleColor = Slate800,
                onClick = onTransactionHistoryClick,
                modifier = Modifier.weight(1f)
            )

            CashierActionItem(
                title = "Cek Stok",
                subtitle = "Harga & Sisa",
                icon = Icons.Outlined.Inventory2,
                iconTint = Slate700,
                iconBg = Slate50,
                borderColor = CardBorder,
                titleColor = Slate800,
                onClick = onStockCheckClick,
                modifier = Modifier.weight(1f)
            )

            CashierActionItem(
                title = "Kalkulator",
                subtitle = "Bangunan",
                icon = Icons.Outlined.Calculate,
                iconTint = Slate700,
                iconBg = Slate50,
                borderColor = CardBorder,
                titleColor = Slate800,
                onClick = onMaterialEstimatorClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CashierActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    borderColor: Color,
    titleColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(94.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, borderColor),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Slate400,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 6. TRANSAKSI SHIFT TERKINI
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
    onViewAllClick: () -> Unit
) {
    val items = remember(recentTransactions) {
        recentTransactions.take(5).map { trx ->
            val receiptNo = if (trx.receiptId.isNotBlank()) {
                val code = trx.receiptId.takeLast(6).uppercase()
                "#TRX-$code"
            } else {
                "#TRX-${trx.id.takeLast(6).uppercase()}"
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
                timeMethod = "$timeStr WIB • $method",
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
            // Header: Green dot + Transaksi Shift Terkini + Lihat Semua Link
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
                        color = Slate800,
                        letterSpacing = (-0.2).sp
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
                        color = Slate600
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Slate400,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            HorizontalDivider(color = Slate100, thickness = 1.dp)

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Slate100),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(20.dp)
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
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
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
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Slate100),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                        contentDescription = null,
                                        tint = PrimaryBrand,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = item.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Slate900
                                    )
                                    Text(
                                        text = item.subtitle,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Slate600,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = item.timeMethod,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Slate400
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = item.amount,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Slate900
                                )
                                Text(
                                    text = item.status,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.isPaid) Emerald600 else AmberText
                                )
                            }
                        }

                        if (index < items.lastIndex) {
                            HorizontalDivider(color = Slate50, thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}
