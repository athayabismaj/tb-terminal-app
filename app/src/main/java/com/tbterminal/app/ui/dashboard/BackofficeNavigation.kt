package com.tbterminal.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import com.tbterminal.app.navigation.AppAccessPolicy
import com.tbterminal.app.navigation.AppCapability
import com.tbterminal.app.R

enum class BackofficeSection(@param:StringRes val labelRes: Int) {
    HOME(R.string.nav_home),
    TRANSACTIONS(R.string.nav_transactions),
    FINANCE(R.string.nav_finance),
    STOCK(R.string.nav_stock),
    MORE(R.string.nav_more),
    MENU(R.string.nav_menu)
}

internal enum class BackofficeNavigationMode { BOTTOM_BAR, RAIL, DRAWER }

internal val BackofficeRailWidth = 88.dp
internal val OwnerCompactSidebarWidth = 76.dp
internal val OwnerExpandedSidebarWidth = 204.dp

@Stable
internal class BackofficeSidebarState(initiallyCollapsed: Boolean = false) {
    var isCollapsed by mutableStateOf(initiallyCollapsed)
        private set

    fun toggle() {
        isCollapsed = !isCollapsed
    }

    companion object {
        val Saver = Saver<BackofficeSidebarState, Boolean>(
            save = { it.isCollapsed },
            restore = { BackofficeSidebarState(it) },
        )
    }
}

@Composable
internal fun rememberBackofficeSidebarState(): BackofficeSidebarState =
    rememberSaveable(saver = BackofficeSidebarState.Saver) { BackofficeSidebarState() }

internal val LocalBackofficeSidebarState = staticCompositionLocalOf { BackofficeSidebarState() }

internal fun backofficeNavigationMode(widthDp: Float, heightDp: Float): BackofficeNavigationMode = when {
    minOf(widthDp, heightDp) < 600f -> BackofficeNavigationMode.BOTTOM_BAR
    widthDp < 900f -> BackofficeNavigationMode.RAIL
    else -> BackofficeNavigationMode.DRAWER
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
    BackofficeNavigationItem(BackofficeSection.MORE, Icons.Outlined.GridView),
    BackofficeNavigationItem(BackofficeSection.MENU, Icons.Outlined.GridView)
)

/** Navigation persona is role based; individual entries remain permission based. */
internal fun isOwnerPersona(role: String?): Boolean = role?.trim()?.equals("owner", ignoreCase = true) == true

internal fun visibleBackofficeSections(role: String): List<BackofficeSection> =
    if (!AppAccessPolicy.can(role, AppCapability.BACKOFFICE)) emptyList()
    else listOf(BackofficeSection.HOME, BackofficeSection.TRANSACTIONS, BackofficeSection.FINANCE,
        BackofficeSection.STOCK, if (isOwnerPersona(role)) BackofficeSection.MENU else BackofficeSection.MORE)

internal data class BackofficePageNavigation(val title: String? = null, val onBack: (() -> Unit)? = null)
internal val LocalBackofficePageNavigation = staticCompositionLocalOf { BackofficePageNavigation() }

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
    showPageHeader: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit
) {
    val ownerNavigation = isOwnerPersona(role)
    val pageNavigation = LocalBackofficePageNavigation.current
    val sidebarState = LocalBackofficeSidebarState.current
    val effectiveTitle = pageTitle ?: pageNavigation.title ?: stringResource(activeSection.labelRes)
    val effectiveBack = onBack ?: pageNavigation.onBack
    val navigationDividerColor = MaterialTheme.colorScheme.outlineVariant
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val navigationMode = backofficeNavigationMode(maxWidth.value, maxHeight.value)
        val expandedNavigation = navigationMode == BackofficeNavigationMode.DRAWER
        val navigationItems = backofficeNavigationItems.filter { item ->
            item.section in visibleBackofficeSections(role)
        }
        if (navigationMode == BackofficeNavigationMode.BOTTOM_BAR) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    if (showPageHeader) {
                        BackofficeTopBar(
                            activeSection = activeSection,
                            pageTitle = effectiveTitle,
                            onBack = effectiveBack,
                            compact = true,
                            onProfileClick = onProfileClick,
                            showProfile = !ownerNavigation,
                        )
                    }
                },
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                val stroke = 1.dp.toPx()
                                drawLine(
                                    color = Color(0xFFE2E8F0), 
                                    start = Offset(0f, 0f), 
                                    end = Offset(size.width, 0f), 
                                    strokeWidth = stroke
                                )
                            },
                        containerColor = Color.White,
                        tonalElevation = 0.dp
                    ) {
                        navigationItems.forEach { item ->
                            val selected = item.section == activeSection
                            NavigationBarItem(
                                modifier = Modifier.testTag("nav-${item.section.name}"),
                                icon = { 
                                    Icon(
                                        item.icon, 
                                        contentDescription = null, 
                                        modifier = Modifier.size(24.dp)
                                    ) 
                                },
                                label = {
                                    Text(
                                        text = stringResource(item.section.labelRes),
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
                                    )
                                },
                                selected = selected,
                                onClick = { onSectionSelected(item.section) },
                                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF256B57),
                                    selectedTextColor = Color(0xFF256B57),
                                    indicatorColor = Color(0xFFE1EFEA),
                                    unselectedIconColor = Color(0xFF94A3B8),
                                    unselectedTextColor = Color(0xFF64748B)
                                )
                            )
                        }
                    }
                }
            ) { paddingValues ->
                content(
                    Modifier.fillMaxSize().padding(paddingValues)
                        .then(if (showPageHeader) Modifier else Modifier.statusBarsPadding())
                )
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                if (ownerNavigation) {
                    OwnerNavigationPanel(
                        expanded = expandedNavigation && !sidebarState.isCollapsed,
                        canToggle = expandedNavigation,
                        activeSection = activeSection,
                        onSectionSelected = onSectionSelected,
                        onToggle = sidebarState::toggle,
                    )
                } else {
                NavigationRail(
                    modifier = Modifier
                        .width(BackofficeRailWidth)
                        .fillMaxHeight()
                        .drawBehind {
                            val stroke = 0.75.dp.toPx()
                            drawLine(
                                color = navigationDividerColor,
                                start = Offset(size.width - stroke / 2f, 0f),
                                end = Offset(size.width - stroke / 2f, size.height),
                                strokeWidth = stroke,
                            )
                        },
                    containerColor = MaterialTheme.colorScheme.background,
                    header = { BackofficeRailBrand() }
                ) {
                    navigationItems.forEach { item ->
                        NavigationRailItem(
                            modifier = Modifier.testTag("nav-${item.section.name}"),
                            selected = item.section == activeSection,
                            onClick = { onSectionSelected(item.section) },
                            icon = { Icon(item.icon, contentDescription = stringResource(item.section.labelRes)) },
                            label = {
                                Text(
                                    stringResource(item.section.labelRes),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
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
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    if (showPageHeader) {
                        BackofficeTopBar(
                            activeSection = activeSection,
                            pageTitle = effectiveTitle,
                            onBack = effectiveBack,
                            compact = false,
                            onProfileClick = onProfileClick,
                            showProfile = !ownerNavigation,
                        )
                    }
                    content(
                        Modifier.weight(1f).fillMaxWidth()
                            .then(if (showPageHeader) Modifier else Modifier.statusBarsPadding())
                    )
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
    onProfileClick: () -> Unit,
    showProfile: Boolean = true,
) {
    com.tbterminal.app.ui.components.TBTopAppBar(
        title = pageTitle ?: stringResource(activeSection.labelRes),
        onBackClick = onBack,
        actions = {
            if (showProfile) {
                IconButton(onClick = onProfileClick) {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = "Buka profil",
                        tint = Color(0xFF1E293B) // Slate 800
                    )
                }
            }
        }
    )
}

@Composable
internal fun OwnerNavigationPanel(
    expanded: Boolean,
    canToggle: Boolean,
    activeSection: BackofficeSection,
    onSectionSelected: (BackofficeSection) -> Unit,
    onToggle: () -> Unit,
) {
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    val sections = listOf(
        BackofficeSection.HOME,
        BackofficeSection.TRANSACTIONS,
        BackofficeSection.FINANCE,
        BackofficeSection.STOCK,
        BackofficeSection.MENU,
    )
    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .width(if (expanded) OwnerExpandedSidebarWidth else OwnerCompactSidebarWidth)
                .fillMaxHeight()
                .testTag("owner-navigation-panel")
                .statusBarsPadding()
                .drawBehind {
                    val stroke = 0.75.dp.toPx()
                    drawLine(
                        color = dividerColor,
                        start = Offset(size.width - stroke / 2f, 0f),
                        end = Offset(size.width - stroke / 2f, size.height),
                        strokeWidth = stroke,
                    )
                }
                .padding(horizontal = if (expanded) 8.dp else 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OwnerSidebarBrand(
                expanded = expanded,
                canToggle = canToggle,
                onToggle = onToggle,
            )
            Spacer(Modifier.height(if (expanded) 16.dp else 12.dp))
            sections.forEach { section ->
                val item = backofficeNavigationItems.first { it.section == section }
                OwnerSidebarItem(
                    section = section,
                    icon = item.icon,
                    selected = activeSection == section,
                    expanded = expanded,
                    onClick = { onSectionSelected(section) },
                )
                Spacer(Modifier.height(4.dp))
            }
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun OwnerSidebarBrand(
    expanded: Boolean,
    canToggle: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = if (expanded) 6.dp else 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center,
    ) {
        if (!expanded && canToggle) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(44.dp).testTag("owner-sidebar-toggle"),
            ) {
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = "Perbesar sidebar",
                    modifier = Modifier.size(20.dp),
                )
            }
        } else {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(11.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Storefront,
                        contentDescription = "TB Terminal",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        if (expanded) {
            Spacer(Modifier.width(10.dp))
            Text(
                "TB Terminal",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            if (canToggle) {
                IconButton(
                    onClick = onToggle,
                    modifier = Modifier.size(40.dp).testTag("owner-sidebar-toggle"),
                ) {
                    Icon(
                        Icons.Outlined.ChevronLeft,
                        contentDescription = "Perkecil sidebar",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun OwnerSidebarItem(
    section: BackofficeSection,
    icon: ImageVector,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val label = stringResource(section.labelRes)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (expanded) 48.dp else 56.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.Tab)
            .testTag("nav-${section.name}"),
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(12.dp),
    ) {
        if (expanded) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    modifier = Modifier.size(20.dp),
                )
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


