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
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    onNavigate: ((AdminDestination) -> Unit)? = null,
    onLogout: () -> Unit,
    onUserManagementClick: () -> Unit = {},
    onSecurityLogClick: () -> Unit = {},
) {
    val globalNavigator = LocalAdminDestinationNavigator.current
    val navigate: (AdminDestination) -> Unit = onNavigate ?: { destination -> globalNavigator?.invoke(destination) }
    val activeDestination = when (section) {
        BackofficeSection.TRANSACTIONS -> AdminDestination.TransactionsHub
        BackofficeSection.FINANCE -> AdminDestination.FinanceHub
        BackofficeSection.STOCK -> AdminDestination.StockHub
        BackofficeSection.MORE, BackofficeSection.MENU -> AdminDestination.MoreHub
        BackofficeSection.HOME -> AdminDestination.Dashboard
    }

    AdminDashboardShell(
        userName = name,
        role = role,
        activeDestination = activeDestination,
        onDashboardClick = { navigate(AdminDestination.Dashboard) },
        onProductsClick = { navigate(AdminDestination.Products) },
        onProfileClick = { navigate(AdminDestination.Profile) },
        onSettingsClick = { navigate(AdminDestination.Settings) },
        onLogout = onLogout,
    ) { contentModifier ->
        TbPageSurface(modifier = contentModifier, maxContentWidth = 1120.dp) { layout, pageModifier ->
            Column(
                modifier = pageModifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(if (layout.isCompact) 20.dp else 24.dp),
            ) {
                when (section) {
                    BackofficeSection.TRANSACTIONS -> TransactionsHub(layout, role, navigate)
                    BackofficeSection.FINANCE -> FinanceHub(layout, financeHubContent(role), navigate)
                    BackofficeSection.STOCK -> StockHub(layout, stockHubContent(role), navigate)
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

@Composable
private fun FinanceHub(
    layout: TbLayoutInfo,
    content: BackofficeHubContent,
    onNavigate: (AdminDestination) -> Unit,
) {
    val sage100 = Color(0xFFE1EFEA)
    val sage800 = Color(0xFF1B4D3E)
    
    Column(modifier = Modifier.fillMaxWidth().testTag("finance-hub-content").padding(bottom = 24.dp)) {
        // Header
        Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 28.dp)) {
            Text(
                "Kelola keuangan",
                color = Color(0xFF0F172A),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                lineHeight = 28.sp
            )
            Text(
                "Kelola catatan dan arus kas usaha Anda",
                color = Color(0xFF64748B),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        // Menu List
        Column(modifier = Modifier.fillMaxWidth()) {

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                shadowElevation = 1.dp
            ) {
                Column {
                    // Piutang Pelanggan
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onNavigate(AdminDestination.Receivables) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).background(sage100, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null, tint = sage800, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                                Text("Piutang pelanggan", color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Tagihan dan pembayaran pelanggan", color = Color(0xFF64748B), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                    }
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    
                    // Hutang Supplier
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onNavigate(AdminDestination.SupplierDebts) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).background(sage100, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = sage800, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                                Text("Hutang supplier", color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Kewajiban dan pembayaran supplier", color = Color(0xFF64748B), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                    }
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    
                    // Kas Harian
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onNavigate(AdminDestination.CashReconciliation) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).background(sage100, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.AssignmentTurnedIn, contentDescription = null, tint = sage800, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                                Text("Kas harian", color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Sesi kas dan rekonsiliasi harian", color = Color(0xFF64748B), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StockHub(
    layout: TbLayoutInfo,
    content: BackofficeHubContent,
    onNavigate: (AdminDestination) -> Unit,
) {
    val sage100 = Color(0xFFE1EFEA)
    val sage800 = Color(0xFF1B4D3E)
    
    Column(modifier = Modifier.fillMaxWidth().testTag("stock-hub-content").padding(bottom = 24.dp)) {
        // Header
        Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp)) {
            Text(
                "Manajemen stok",
                color = Color(0xFF0F172A),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                lineHeight = 28.sp
            )
            Text(
                "Pantau pergerakan barang dan katalog produk toko",
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        // Operasional stok
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                shadowElevation = 1.dp
            ) {
                Column {
                    // Item 1: Daftar produk
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onNavigate(AdminDestination.Products) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).background(sage100, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = sage800, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Daftar produk", color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Kelola katalog barang dan stok", color = Color(0xFF64748B), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                    }
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    
                    // Item 2: Sesuaikan stok
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onNavigate(AdminDestination.StockOpname) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).background(sage100, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Tune, contentDescription = null, tint = sage800, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sesuaikan stok", color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Update atau koreksi jumlah stok", color = Color(0xFF64748B), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                    }
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    
                    // Item 3: Kartu stok
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onNavigate(AdminDestination.StockReport) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).background(sage100, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ListAlt, contentDescription = null, tint = sage800, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Kartu stok", color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Riwayat pergerakan barang", color = Color(0xFF64748B), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        
        // Data produk
        Column(modifier = Modifier.fillMaxWidth()) {
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                shadowElevation = 1.dp
            ) {
                Column {
                    // Item 1: Harga produk
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onNavigate(AdminDestination.PriceManagement) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).background(sage100, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.LocalOffer, contentDescription = null, tint = sage800, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Harga produk", color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Kelola harga jual dan modal", color = Color(0xFF64748B), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                    }
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    
                    // Item 2: Kategori produk
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onNavigate(AdminDestination.ProductCategories) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).background(sage100, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Category, contentDescription = null, tint = sage800, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Kategori produk", color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Kelompokkan produk toko", color = Color(0xFF64748B), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                    }
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    
                    // Item 3: Satuan produk
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onNavigate(AdminDestination.ProductUnits) }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).background(sage100, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Straighten, contentDescription = null, tint = sage800, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Satuan produk", color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text("Atur satuan jual beli", color = Color(0xFF64748B), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionsHub(
    layout: TbLayoutInfo,
    role: String,
    onNavigate: (AdminDestination) -> Unit,
) {
    var mode by remember { mutableStateOf("penjualan") }
    
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
                            color = if (mode == "penjualan") Color(0xFF256B57) else Color(0xFF64748B),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                    if (mode == "penjualan") {
                        Box(modifier = Modifier.align(Alignment.BottomCenter).width(50.dp).height(3.dp).background(Color(0xFF256B57), RoundedCornerShape(50)))
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
                            color = if (mode == "pembelian") Color(0xFF256B57) else Color(0xFF64748B),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                    if (mode == "pembelian") {
                        Box(modifier = Modifier.align(Alignment.BottomCenter).width(50.dp).height(3.dp).background(Color(0xFF256B57), RoundedCornerShape(50)))
                    }
                }
            }
        }
        
        // Content Section
        if (mode == "penjualan") {
            // Penjualan Stats
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(Color(0xFF256B57), Color(0xFF1E5545))
                    )
                ).padding(16.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Ringkasan Hari Ini", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Row(
                            modifier = Modifier.background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF6EE7B7), CircleShape))
                            Text("Kasir Aktif", color = Color(0xFFD1FAE5), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Column {
                            Text("Rp 3.850.000", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                            Text("18 Transaksi Berhasil", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        Box(modifier = Modifier.background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text("Shift #1 (Pagi)", color = Color(0xFFECFDF5), fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
                                modifier = Modifier.size(40.dp).background(Color(0xFFE1EFEA), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null, tint = Color(0xFF1B4D3E), modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Riwayat penjualan", color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Cari dan periksa transaksi penjualan", color = Color(0xFF64748B), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                        
                        // Kasir Baru
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onNavigate(AdminDestination.CashierCashSession) }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp).background(Color(0xFFE1EFEA), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.PointOfSale, contentDescription = null, tint = Color(0xFF1B4D3E), modifier = Modifier.size(20.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Buka kasir baru", color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("Mulai sesi penjualan baru", color = Color(0xFF64748B), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        } else {
            // Pembelian Stats
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                    )
                ).padding(16.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Total Pembelian Bulan Ini", color = Color(0xFFCBD5E1), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Box(modifier = Modifier.background(Color(0xFF334155).copy(alpha = 0.6f), RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text("Bulan Berjalan", color = Color(0xFFE2E8F0), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Column {
                            Text("Rp 14.280.000", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                            Text("6 Faktur dari Supplier", color = Color(0xFF94A3B8), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        Box(modifier = Modifier.border(1.dp, Color(0xFF475569).copy(alpha = 0.5f), RoundedCornerShape(8.dp)).background(Color(0xFF334155).copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text("1 Pending PO", color = Color(0xFF34D399), fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
                                modifier = Modifier.size(40.dp).background(Color(0xFFE1EFEA), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.LocalShipping, contentDescription = null, tint = Color(0xFF1B4D3E), modifier = Modifier.size(20.dp))
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
                                modifier = Modifier.size(40.dp).background(Color(0xFFE1EFEA), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Outlined.ListAlt, contentDescription = null, tint = Color(0xFF1B4D3E), modifier = Modifier.size(20.dp))
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






