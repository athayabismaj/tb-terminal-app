package com.tbterminal.app.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tbterminal.app.navigation.AppAccessPolicy
import com.tbterminal.app.navigation.AppCapability
import com.tbterminal.app.ui.components.TbLayoutInfo
import com.tbterminal.app.ui.components.TbPageSurface
import com.tbterminal.app.ui.dashboard.admin.AdminDashboardShell
import com.tbterminal.app.ui.dashboard.admin.AdminDestination
import com.tbterminal.app.ui.dashboard.admin.LocalAdminDestinationNavigator

private data class HubAction(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val destination: AdminDestination? = null,
    val onClick: (() -> Unit)? = null
)

@Composable
fun BackofficeHubScreen(
    name: String,
    role: String,
    section: BackofficeSection,
    onNavigate: ((AdminDestination) -> Unit)? = null,
    onLogout: () -> Unit,
    onUserManagementClick: () -> Unit = {},
    onSecurityLogClick: () -> Unit = {}
) {
    val globalNavigator = LocalAdminDestinationNavigator.current
    val navigate: (AdminDestination) -> Unit = onNavigate ?: { destination -> globalNavigator?.invoke(destination) }
    val activeDestination = when (section) {
        BackofficeSection.TRANSACTIONS -> AdminDestination.TransactionsHub
        BackofficeSection.FINANCE -> AdminDestination.FinanceHub
        BackofficeSection.STOCK -> AdminDestination.StockHub
        BackofficeSection.MORE -> AdminDestination.MoreHub
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
        onLogout = onLogout
    ) { contentModifier ->
        TbPageSurface(modifier = contentModifier) { layout, pageModifier ->
            Column(
                modifier = pageModifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(layout.verticalSpacing)
            ) {
                when (section) {
                    BackofficeSection.TRANSACTIONS -> TransactionsHub(
                        layout,
                        navigate,
                        AppAccessPolicy.can(role, AppCapability.POS)
                    )
                    BackofficeSection.FINANCE -> FinanceHub(layout, navigate)
                    BackofficeSection.STOCK -> StockHub(layout, navigate)
                    BackofficeSection.MORE -> MoreHub(
                        layout = layout,
                        role = role,
                        onNavigate = navigate,
                        onUserManagementClick = onUserManagementClick,
                        onSecurityLogClick = onSecurityLogClick,
                        onLogout = onLogout
                    )
                    BackofficeSection.HOME -> Unit
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TransactionsHub(
    layout: TbLayoutInfo,
    onNavigate: (AdminDestination) -> Unit,
    canUsePos: Boolean
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    HubHeader(
        title = "Transaksi",
        subtitle = "Penjualan dan pembelian dalam satu tempat.",
        actionLabel = if (canUsePos) "+ Transaksi Baru" else null,
        onAction = if (canUsePos) ({ onNavigate(AdminDestination.NewTransaction) }) else null,
        showTitle = false
    )
    TabRow(selectedTabIndex = selectedTab) {
        listOf("Penjualan", "Pembelian").forEachIndexed { index, label ->
            Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(label) })
        }
    }
    val actions = if (selectedTab == 0) {
        buildList {
            add(HubAction("Daftar Penjualan", "Cari nomor transaksi, kasir, pelanggan, metode, dan status.", Icons.AutoMirrored.Outlined.ReceiptLong, AdminDestination.SalesTransactions))
            if (canUsePos) add(HubAction("Buat Penjualan", "Mulai transaksi POS baru.", Icons.Outlined.PointOfSale, AdminDestination.NewTransaction))
        }
    } else {
        buildList {
            add(HubAction("Daftar Pembelian", "Cari pembelian supplier dan status pembayarannya.", Icons.AutoMirrored.Outlined.ListAlt, AdminDestination.PurchaseHistory))
            add(HubAction("Barang Masuk", "Catat pembelian atau penerimaan barang.", Icons.Outlined.LocalShipping, AdminDestination.IncomingGoods))
        }
    }
    HubActionGrid(layout, actions, onNavigate)
}

@Composable
private fun FinanceHub(
    layout: TbLayoutInfo,
    onNavigate: (AdminDestination) -> Unit
) {
    FinanceCardGrid(
        layout = layout,
        actions = listOf(
            HubAction(
                title = "Piutang Pelanggan",
                description = "Tagihan, jatuh tempo, dan pembayaran pelanggan.",
                icon = Icons.Outlined.Payments,
                destination = AdminDestination.Receivables
            ),
            HubAction(
                title = "Hutang Supplier",
                description = "Kewajiban, jatuh tempo, dan pembayaran supplier.",
                icon = Icons.Outlined.AccountBalanceWallet,
                destination = AdminDestination.SupplierDebts
            ),
            HubAction(
                title = "Kas Harian",
                description = "Saldo awal, uang masuk, uang keluar, dan saldo sistem.",
                icon = Icons.Outlined.AssignmentTurnedIn,
                destination = AdminDestination.CashReconciliation
            )
        ),
        onNavigate = onNavigate
    )
}

@Composable
private fun FinanceCardGrid(
    layout: TbLayoutInfo,
    actions: List<HubAction>,
    onNavigate: (AdminDestination) -> Unit
) {
    if (layout.isCompact) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            actions.forEach { action ->
                Card(
                    onClick = { action.destination?.let(onNavigate) },
                    modifier = Modifier.fillMaxWidth().height(104.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder(),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(action.icon, contentDescription = null, modifier = Modifier.size(23.dp))
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(action.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                action.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
        return
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        actions.forEach { action ->
            Card(
                onClick = { action.destination?.let(onNavigate) },
                modifier = Modifier.weight(1f).height(164.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder(),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(action.icon, contentDescription = null, modifier = Modifier.size(23.dp))
                        }
                    }
                    Text(action.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        action.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun StockHub(
    layout: TbLayoutInfo,
    onNavigate: (AdminDestination) -> Unit
) {
    HubHeader(
        title = "Stok",
        subtitle = "Cek ketersediaan, harga, dan pergerakan barang.",
        actionLabel = "+ Barang Masuk",
        onAction = { onNavigate(AdminDestination.IncomingGoods) },
        showTitle = false
    )
    HubActionGrid(
        layout,
        listOf(
            HubAction("Daftar Produk & Stok", "Cari produk, kategori, harga, stok sekarang, dan batas minimum.", Icons.Outlined.Inventory2, AdminDestination.Products),
            HubAction("Barang Masuk", "Tambah stok dari pembelian atau penerimaan.", Icons.Outlined.LocalShipping, AdminDestination.IncomingGoods),
            HubAction("Sesuaikan Stok", "Koreksi stok melalui opname dengan alasan yang jelas.", Icons.Outlined.Tune, AdminDestination.StockOpname),
            HubAction("Kartu Stok", "Lihat saldo dan riwayat mutasi setiap produk.", Icons.AutoMirrored.Outlined.ListAlt, AdminDestination.StockReport)
        ),
        onNavigate
    )
    HorizontalDivider()
    Text("Pengaturan produk", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    HubActionGrid(
        layout,
        listOf(
            HubAction("Harga Produk", "Atur harga jual dan riwayat perubahan harga.", Icons.Outlined.Payments, AdminDestination.PriceManagement),
            HubAction("Kategori", "Kelola kelompok produk.", Icons.Outlined.Category, AdminDestination.ProductCategories),
            HubAction("Satuan", "Kelola satuan utama dan konversi produk.", Icons.Outlined.Straighten, AdminDestination.ProductUnits)
        ),
        onNavigate
    )
}

@Composable
private fun MoreHub(
    layout: TbLayoutInfo,
    role: String,
    onNavigate: (AdminDestination) -> Unit,
    onUserManagementClick: () -> Unit,
    onSecurityLogClick: () -> Unit,
    onLogout: () -> Unit
) {
    HubHeader("Lainnya", "Master data, laporan, administrasi, perangkat, dan akun.", showTitle = false)
    HubGroup("Master Data", layout, listOf(
        HubAction("Produk", "Daftar dan konfigurasi produk.", Icons.Outlined.Inventory2, AdminDestination.Products),
        HubAction("Pelanggan", "Data pelanggan dan batas kredit.", Icons.Outlined.Groups, AdminDestination.Customers),
        HubAction("Supplier", "Data supplier toko.", Icons.Outlined.Group, AdminDestination.Suppliers),
        HubAction("Kategori", "Pengelompokan produk.", Icons.Outlined.Category, AdminDestination.ProductCategories),
        HubAction("Satuan", "Satuan utama dan konversi produk.", Icons.Outlined.Straighten, AdminDestination.ProductUnits)
    ), onNavigate)
    HubGroup("Laporan", layout, listOf(
        HubAction("Penjualan & Keuangan", "Laporan periode, metode pembayaran, dan status.", Icons.Outlined.GridView, AdminDestination.Reports),
        HubAction("Stok", "Laporan stok dan kartu stok.", Icons.Outlined.Inventory2, AdminDestination.StockReport),
        HubAction("Laporan Lokal", "Ringkasan data yang tersimpan di perangkat.", Icons.AutoMirrored.Outlined.ListAlt, AdminDestination.LocalReports)
    ), onNavigate)

    val administration = buildList {
        if (AppAccessPolicy.can(role, AppCapability.USER_MANAGEMENT)) {
            add(HubAction("Pengguna", "Kelola akun admin dan kasir.", Icons.Outlined.Person, onClick = onUserManagementClick))
        }
        if (AppAccessPolicy.can(role, AppCapability.AUDIT)) {
            add(HubAction("Riwayat Aktivitas", "Audit perubahan data dan aktivitas penting.", Icons.Outlined.Security, AdminDestination.OperationalAudit))
        }
        if (AppAccessPolicy.can(role, AppCapability.SECURITY_SETTINGS)) {
            add(HubAction("Log Keamanan", "Periksa login dan kejadian keamanan.", Icons.Outlined.Lock, onClick = onSecurityLogClick))
        }
        if (AppAccessPolicy.can(role, AppCapability.SERVER_BACKUP)) {
            add(HubAction("Backup & Restore", "Kelola backup lokal dan database server.", Icons.Outlined.Backup, AdminDestination.BackupRestore))
        }
    }
    HubGroup("Administrasi", layout, administration, onNavigate)
    HubGroup("Perangkat", layout, listOf(
        HubAction("Printer", "Pilih ukuran kertas dan cetak melalui Android.", Icons.Outlined.Print, AdminDestination.Settings),
        HubAction("Sinkronisasi", "Pantau antrean offline dan status koneksi.", Icons.Outlined.Sync, AdminDestination.SyncCenter),
        HubAction(
            "Pengaturan",
            if (AppAccessPolicy.can(role, AppCapability.SECURITY_SETTINGS)) {
                "Atur operasional, keamanan, dan perangkat."
            } else {
                "Atur operasional dan perangkat."
            },
            Icons.Outlined.Settings,
            AdminDestination.Settings
        )
    ), onNavigate)
    HubGroup("Akun", layout, listOf(
        HubAction("Profil", "Lihat identitas akun yang sedang digunakan.", Icons.Outlined.Person, AdminDestination.Profile),
        HubAction("Password / PIN", "Perbarui kredensial akun sendiri.", Icons.Outlined.Lock, AdminDestination.Profile),
        HubAction("Logout", "Keluar dengan aman dari perangkat ini.", Icons.AutoMirrored.Outlined.Logout, onClick = onLogout)
    ), onNavigate)
}

@Composable
private fun HubGroup(
    title: String,
    layout: TbLayoutInfo,
    actions: List<HubAction>,
    onNavigate: (AdminDestination) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        HubActionGrid(layout, actions, onNavigate)
    }
}

@Composable
private fun HubHeader(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    showTitle: Boolean = true
) {
    if (!showTitle && (actionLabel == null || onAction == null)) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (showTitle) {
                Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction, modifier = Modifier.height(48.dp)) { Text(actionLabel) }
        }
    }
}

@Composable
private fun HubActionGrid(
    layout: TbLayoutInfo,
    actions: List<HubAction>,
    onNavigate: (AdminDestination) -> Unit
) {
    val columns = when {
        layout.isCompact -> 1
        layout.isExpanded -> 3
        else -> 2
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        actions.chunked(columns).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { action ->
                    Card(
                        onClick = { action.onClick?.invoke() ?: action.destination?.let(onNavigate) },
                        modifier = Modifier.weight(1f).height(112.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(action.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Column {
                                Text(action.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(action.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                repeat(columns - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
