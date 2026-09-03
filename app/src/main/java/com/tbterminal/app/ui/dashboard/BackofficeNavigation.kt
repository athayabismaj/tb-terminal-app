package com.tbterminal.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tbterminal.app.navigation.AppAccessPolicy
import com.tbterminal.app.navigation.AppCapability

enum class BackofficeSection(val label: String) {
    HOME("Beranda"),
    TRANSACTIONS("Transaksi"),
    FINANCE("Keuangan"),
    STOCK("Stok"),
    MORE("Lainnya")
}

private data class BackofficeNavigationItem(
    val section: BackofficeSection,
    val icon: ImageVector
)

private val backofficeNavigationItems = listOf(
    BackofficeNavigationItem(BackofficeSection.HOME, Icons.Outlined.Home),
    BackofficeNavigationItem(BackofficeSection.TRANSACTIONS, Icons.AutoMirrored.Outlined.ReceiptLong),
    BackofficeNavigationItem(BackofficeSection.FINANCE, Icons.Outlined.AccountBalanceWallet),
    BackofficeNavigationItem(BackofficeSection.STOCK, Icons.Outlined.Inventory2),
    BackofficeNavigationItem(BackofficeSection.MORE, Icons.Outlined.GridView)
)

internal fun visibleBackofficeSections(role: String): List<BackofficeSection> =
    if (AppAccessPolicy.can(role, AppCapability.BACKOFFICE)) BackofficeSection.entries else emptyList()

@Composable
fun BackofficeAdaptiveShell(
    userName: String,
    role: String,
    activeSection: BackofficeSection,
    onSectionSelected: (BackofficeSection) -> Unit,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit,
    pageTitle: String? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val navigationItems = backofficeNavigationItems.filter { item ->
            item.section in visibleBackofficeSections(role)
        }
        if (maxWidth < 600.dp) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    BackofficeTopBar(
                        activeSection = activeSection,
                        pageTitle = pageTitle,
                        onBack = onBack,
                        compact = true,
                        onProfileClick = onProfileClick
                    )
                },
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                        navigationItems.forEach { item ->
                            NavigationBarItem(
                                selected = item.section == activeSection,
                                onClick = { onSectionSelected(item.section) },
                                icon = { Icon(item.icon, contentDescription = item.section.label) },
                                label = { Text(item.section.label, maxLines = 1) }
                            )
                        }
                    }
                }
            ) { paddingValues ->
                content(Modifier.fillMaxSize().padding(paddingValues))
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.width(112.dp).fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    header = { BackofficeRailBrand() }
                ) {
                    navigationItems.forEach { item ->
                        NavigationRailItem(
                            selected = item.section == activeSection,
                            onClick = { onSectionSelected(item.section) },
                            icon = { Icon(item.icon, contentDescription = item.section.label) },
                            label = { Text(item.section.label, maxLines = 1) }
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    NavigationRailItem(
                        selected = false,
                        onClick = onProfileClick,
                        icon = { Icon(Icons.Outlined.Person, contentDescription = "Profil") },
                        label = { Text("Profil") }
                    )
                    NavigationRailItem(
                        selected = false,
                        onClick = onLogout,
                        icon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Keluar") },
                        label = { Text("Keluar") }
                    )
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    BackofficeTopBar(
                        activeSection = activeSection,
                        pageTitle = pageTitle,
                        onBack = onBack,
                        compact = false,
                        onProfileClick = onProfileClick
                    )
                    content(Modifier.weight(1f).fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun BackofficeTopBar(
    activeSection: BackofficeSection,
    pageTitle: String?,
    onBack: (() -> Unit)?,
    compact: Boolean,
    onProfileClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = if (compact) 12.dp else 20.dp, vertical = 8.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(if (compact) 56.dp else 60.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(percent = 50),
            color = androidx.compose.ui.graphics.Color.Transparent,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(start = if (onBack == null) 18.dp else 2.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Kembali",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    pageTitle ?: activeSection.label,
                    modifier = Modifier.weight(1f),
                    style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onProfileClick, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = "Buka profil",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }
        }
    }
}

internal fun String.toBackofficeRoleLabel(): String = when (lowercase()) {
    "owner" -> "Pemilik"
    "admin" -> "Admin"
    "kasir", "cashier" -> "Kasir"
    else -> replaceFirstChar(Char::uppercase)
}

@Composable
private fun BackofficeRailBrand() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.shapes.medium
            ),
            contentAlignment = Alignment.Center
        ) {
            Text("TB", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
        }
        Text("Terminal", style = MaterialTheme.typography.labelSmall)
    }
}
