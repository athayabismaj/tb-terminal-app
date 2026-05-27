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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tbterminal.app.data.repository.CashReconciliationRepository
import com.tbterminal.app.ui.dashboard.DashboardBackground
import com.tbterminal.app.ui.dashboard.DashboardBrandGreen
import com.tbterminal.app.ui.dashboard.DashboardBrandGreenDark
import com.tbterminal.app.ui.dashboard.DashboardInfoBlue
import com.tbterminal.app.ui.dashboard.DashboardSurface
import com.tbterminal.app.ui.dashboard.DashboardTextPrimary
import com.tbterminal.app.ui.dashboard.DashboardTextSecondary
import com.tbterminal.app.ui.dashboard.DashboardWarningOrange

@Composable
fun CashierDashboardScreen(
    name: String,
    role: String,
    cashReconciliationRepository: CashReconciliationRepository,
    onDashboardClick: () -> Unit = {},
    onPosClick: () -> Unit = {},
    onCashSessionClick: () -> Unit = {},
    onTransactionHistoryClick: () -> Unit = {},
    onStockCheckClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: CashierDashboardViewModel = viewModel(
        factory = CashierDashboardViewModel.factory(cashReconciliationRepository)
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
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onLogout = onLogout
    ) { contentModifier ->
        CashierDashboardContent(
            uiState = uiState,
            onPosClick = onPosClick,
            onStockCheckClick = onStockCheckClick,
            modifier = contentModifier.padding(32.dp)
        )
    }
}

@Composable
private fun CashierSidebar(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(DashboardSurface)
            .border(1.dp, Color.LightGray.copy(alpha = 0.3f))
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        Text(
            text = "TB Terminal",
            color = DashboardBrandGreenDark,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
        )
        Text(
            text = "TERMINAL KASIR",
            color = DashboardTextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 12.dp, bottom = 48.dp)
        )

        CashierSidebarItem(Icons.Outlined.GridView, "Dashboard", isActive = true)
        CashierSidebarItem(Icons.Outlined.PointOfSale, "POS")
        CashierSidebarItem(Icons.Outlined.Payments, "Kas Harian")
        CashierSidebarItem(Icons.Outlined.History, "Riwayat Transaksi")
        CashierSidebarItem(Icons.Outlined.Inventory2, "Cek Stok")

        Spacer(modifier = Modifier.weight(1f))

        CashierSidebarItem(
            icon = Icons.AutoMirrored.Outlined.Logout,
            text = "Keluar",
            tint = DashboardTextSecondary,
            onClick = onLogout
        )
    }
}

@Composable
private fun CashierSidebarItem(
    icon: ImageVector,
    text: String,
    isActive: Boolean = false,
    tint: Color = DashboardTextPrimary,
    onClick: (() -> Unit)? = null
) {
    val contentColor = if (isActive) DashboardBrandGreenDark else tint
    val backgroundColor = if (isActive) DashboardBrandGreen.copy(alpha = 0.12f) else Color.Transparent
    val clickModifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .then(clickModifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = text, tint = contentColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            color = contentColor,
            fontSize = 14.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun CashierHeader(
    userName: String,
    role: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DashboardSurface)
            .padding(horizontal = 32.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = "",
            onValueChange = {},
            placeholder = {
                Text("Cari barang atau scan barcode...", color = DashboardTextSecondary, fontSize = 14.sp)
            },
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = DashboardTextSecondary)
            },
            trailingIcon = {
                Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Scan barcode", tint = DashboardTextSecondary)
            },
            modifier = Modifier
                .width(440.dp)
                .height(50.dp),
            shape = RoundedCornerShape(24.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = DashboardBrandGreenDark,
                unfocusedContainerColor = DashboardBackground,
                focusedContainerColor = DashboardBackground
            )
        )

        Spacer(modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(userName, color = DashboardTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("DASHBOARD ${role.uppercase()}", color = DashboardTextSecondary, fontSize = 10.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(DashboardBrandGreen.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = userName.firstOrNull()?.uppercase() ?: "K",
                color = DashboardBrandGreenDark,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CashierDashboardContent(
    uiState: CashierDashboardUiState,
    onPosClick: () -> Unit,
    onStockCheckClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Dashboard Kasir",
                    color = DashboardTextPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Fokus pada transaksi, kas harian, dan pengecekan stok cepat.",
                    color = DashboardTextSecondary,
                    fontSize = 14.sp
                )
            }
            Button(
                onClick = onPosClick,
                colors = ButtonDefaults.buttonColors(containerColor = DashboardBrandGreenDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.PointOfSale, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buka POS", fontWeight = FontWeight.Bold)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DashboardSurface),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.28f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Mulai Sesi", color = DashboardTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            if (uiState.activeSession != null) "Sesi sedang berjalan" else "Klik untuk membuka sesi kasir", 
                            color = DashboardTextSecondary, 
                            fontSize = 12.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(DashboardBrandGreenDark.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.ReceiptLong, contentDescription = null, tint = DashboardBrandGreenDark, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }

        val moneyFormat = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("id-ID"))
        val systemCashStr = moneyFormat.format(uiState.systemCash)

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            CashierMetricCard(
                title = "TRANSAKSI HARI INI",
                value = "${uiState.todayTransactionsCount}",
                subtitle = "Semua transaksi selesai",
                icon = Icons.Outlined.ReceiptLong,
                tint = DashboardBrandGreenDark,
                modifier = Modifier.weight(1f)
            )
            CashierMetricCard(
                title = "KAS SISTEM",
                value = systemCashStr,
                subtitle = "Total kas hari ini",
                icon = Icons.Outlined.AccountBalanceWallet,
                tint = DashboardInfoBlue,
                modifier = Modifier.weight(1f)
            )
            CashierMetricCard(
                title = "KERANJANG AKTIF",
                value = "0",
                subtitle = "Pesanan pending",
                icon = Icons.Outlined.ShoppingCart,
                tint = DashboardWarningOrange,
                modifier = Modifier.weight(1f)
            )
        }

        // Target Harian
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DashboardSurface),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.28f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Target Penjualan Harian", color = DashboardTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("75%", color = DashboardBrandGreenDark, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { 0.75f },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = DashboardBrandGreenDark,
                    trackColor = DashboardBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Rp 7.500.000 / Rp 10.000.000", color = DashboardTextSecondary, fontSize = 12.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Card(
                modifier = Modifier
                    .weight(1.35f)
                    .height(380.dp),
                colors = CardDefaults.cardColors(containerColor = DashboardSurface),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.28f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text("Aksi Cepat", color = DashboardTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    CashierQuickAction(
                        icon = Icons.Outlined.PointOfSale,
                        title = "Mulai transaksi baru",
                        description = "Buka layar POS untuk input pesanan.",
                        tint = DashboardBrandGreenDark,
                        onClick = onPosClick
                    )
                    CashierQuickAction(
                        icon = Icons.Outlined.Inventory2,
                        title = "Cek stok cepat",
                        description = "Lihat sisa stok tanpa membuka modul admin.",
                        tint = DashboardWarningOrange,
                        onClick = onStockCheckClick
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(380.dp),
                colors = CardDefaults.cardColors(containerColor = DashboardSurface),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.28f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Status Hardware & Sesi", color = DashboardTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    CashierStatusRow("Shift", if (uiState.activeSession != null) "Aktif" else "Tutup", uiState.activeSession != null)
                    CashierStatusRow("Printer Thermal", "Terhubung", true)

                    val durasiStr = if (uiState.activeSession != null) {
                        try {
                            val openedAt = java.time.OffsetDateTime.parse(uiState.activeSession.openedAt)
                            val now = java.time.OffsetDateTime.now()
                            val durasi = java.time.Duration.between(openedAt, now)
                            "${durasi.toHours()} jam ${durasi.toMinutesPart()} menit"
                        } catch (e: Exception) { "-" }
                    } else {
                        "-"
                    }
                    CashierStatusRow("Durasi Shift", durasiStr, false)
                    
                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DashboardInfoBlue.copy(alpha = 0.1f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.History, contentDescription = null, tint = DashboardInfoBlue, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Baru saja terjual", color = DashboardTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            if (uiState.recentTransactions.isEmpty()) {
                                Text("Belum ada transaksi", color = DashboardTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            } else {
                                uiState.recentTransactions.forEach { tx ->
                                    val totalStr = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("id-ID")).format(tx.total)
                                    Text("${tx.receiptId} - $totalStr", color = DashboardTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CashierMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(156.dp),
        colors = CardDefaults.cardColors(containerColor = DashboardSurface),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint)
            }
            Column {
                Text(title, color = DashboardTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(value, color = DashboardTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                Text(subtitle, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun CashierQuickAction(
    icon: ImageVector,
    title: String,
    description: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, color = DashboardTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(description, color = DashboardTextSecondary, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun CashierStatusRow(
    label: String,
    value: String,
    hasDot: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = DashboardTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (hasDot) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val alpha = infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(DashboardBrandGreenDark.copy(alpha = alpha.value))
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(value, color = DashboardTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
