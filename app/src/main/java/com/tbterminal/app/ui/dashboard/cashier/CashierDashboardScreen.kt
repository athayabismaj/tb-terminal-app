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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PointOfSale
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
import com.tbterminal.app.ui.common.toDisplayTime
import com.tbterminal.app.ui.common.toRupiahText
import com.tbterminal.app.ui.components.RefreshableContent
import com.tbterminal.app.ui.components.SkeletonCard
import com.tbterminal.app.ui.components.TopFloatingCapsule

// Design tokens harmonized with HTML mockup & Industrial Standard
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

private val BrandGreen = Color(0xFF1B5E3C)
private val BrandGreenDark = Color(0xFF13462C)
private val BrandGreenLight = Color(0xFFF4F9F5)
private val BrandGreenTint = Color(0xFFE8F3EC)
private val BrandGreenBorder = Color(0xFFD1FAE5)
private val Emerald500 = Color(0xFF10B981)
private val Emerald700 = Color(0xFF047857)

private val CardBorder = Color(0xFFE2E8F0)

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
    modifier: Modifier = Modifier
) {
    var isPrivacyHidden by rememberSaveable { mutableStateOf(false) }

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
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── 1. GREETING HEADER ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Halo, $displayName",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = if (uiState.activeSession != null) {
                        "Shift kasir aktif · Siap melayani transaksi kasir"
                    } else {
                        "Shift kasir belum dibuka · Buka sesi untuk memulai"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate500
                )
            }

            if (uiState.isLoading && uiState.activeSession == null && uiState.recentTransactions.isEmpty()) {
                SkeletonCard()
                SkeletonCard()
            } else {
                // ── 2. SHIFT SUMMARY CARD (RINGKASAN SHIFT KASIR) ──
                CashierHeroShiftCard(
                    uiState = uiState,
                    isPrivacyHidden = isPrivacyHidden,
                    onTogglePrivacy = { isPrivacyHidden = !isPrivacyHidden },
                    onCashSessionClick = onCashSessionClick
                )

                // ── 3. QUICK ACTIONS GRID (AKSI CEPAT 4 KOLOM) ──
                CashierQuickActionsGrid(
                    onPosClick = onPosClick,
                    onCashSessionClick = onCashSessionClick,
                    onTransactionHistoryClick = onTransactionHistoryClick,
                    onStockCheckClick = onStockCheckClick
                )

                // ── 4. RECENT TRANSACTIONS SECTION (TRANSAKSI SHIFT TERKINI) ──
                CashierRecentTransactionsCard(
                    recentTransactions = uiState.recentTransactions,
                    onViewAllClick = onTransactionHistoryClick
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 2. HERO SHIFT SUMMARY CARD
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun CashierHeroShiftCard(
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

                if (isShiftActive) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
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
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Emerald500.copy(alpha = alpha))
                            )
                            Text(
                                text = "Shift Aktif",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandGreen
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
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                fontWeight = FontWeight.SemiBold,
                                color = AmberText
                            )
                        }
                    }
                }
            }

            // Main Metric: Penjualan Shift Saya
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "Penjualan Shift Saya",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate500
                )

                Text(
                    text = if (isPrivacyHidden) "••••••••" else uiState.todaySalesRevenue.toRupiahText(),
                    fontSize = 26.sp,
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
                            "${uiState.todayTransactionsCount} transaksi berhasil pada shift ini"
                        } else {
                            "Belum ada transaksi shift yang tercatat"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Emerald700
                    )
                }
            }

            // Sub Metrics: Estimasi Kas di Laci & Modal Awal Shift (Grid 2 Kolom)
            HorizontalDivider(color = Slate100, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Estimasi Kas Laci",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate400
                    )
                    Text(
                        text = if (isPrivacyHidden) "••••••••" else uiState.systemCash.toRupiahText(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Modal Awal Shift",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate400
                    )
                    Text(
                        text = if (isPrivacyHidden) "••••••••" else uiState.openingCash.toRupiahText(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action Button: Kelola Kas & Tutup Shift atau Buka Sesi Kas Baru
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
                        tint = BrandGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Kelola Kas & Tutup Shift",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = onCashSessionClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandGreen,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Buka Sesi Kas Baru",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 3. QUICK ACTIONS GRID
// ═════════════════════════════════════════════════════════════════════════════
@Composable
private fun CashierQuickActionsGrid(
    onPosClick: () -> Unit,
    onCashSessionClick: () -> Unit,
    onTransactionHistoryClick: () -> Unit,
    onStockCheckClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "AKSI CEPAT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Slate400,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CashierQuickActionItem(
                title = "Mulai POS",
                icon = Icons.Outlined.PointOfSale,
                onClick = onPosClick,
                modifier = Modifier.weight(1f)
            )

            CashierQuickActionItem(
                title = "Kas Kecil",
                icon = Icons.Outlined.Payments,
                onClick = onCashSessionClick,
                modifier = Modifier.weight(1f)
            )

            CashierQuickActionItem(
                title = "Riwayat",
                icon = Icons.Outlined.History,
                onClick = onTransactionHistoryClick,
                modifier = Modifier.weight(1f)
            )

            CashierQuickActionItem(
                title = "Cek Stok",
                icon = Icons.Outlined.Inventory2,
                onClick = onStockCheckClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CashierQuickActionItem(
    title: String,
    icon: ImageVector,
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
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BrandGreenLight)
                    .border(1.dp, BrandGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = BrandGreen,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate700,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 4. RECENT TRANSACTIONS SECTION
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
                hasRemaining || trx.status.equals("PARTIAL", ignoreCase = true) || trx.status.equals("UNPAID", ignoreCase = true) || trx.status.equals("HUTANG", ignoreCase = true) -> "Belum Lunas"
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
                        color = BrandGreen
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = Slate300,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Belum ada transaksi pada shift ini",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Slate500
                        )
                        Text(
                            text = "Transaksi POS yang berhasil dicetak akan muncul di sini.",
                            fontSize = 11.sp,
                            color = Slate400,
                            textAlign = TextAlign.Center
                        )
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
                                        .border(1.dp, BrandGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                        contentDescription = null,
                                        tint = BrandGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    Text(
                                        text = item.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
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
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = item.amount,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate900
                                )

                                Text(
                                    text = item.status,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (item.isPaid) Emerald700 else DangerRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
